package br.com.davidalain.passeiocavalo.servico;

import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import br.com.davidalain.passeiocavalo.solucao.Solucao7;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executa os cálculos em segundo plano, fora da thread da requisição HTTP.
 * <ul>
 *     <li>Um cálculo por vez: cada um já usa todos os núcleos reservados para ele;</li>
 *     <li>O cálculo roda em um {@link ForkJoinPool} próprio, então não ocupa o pool comum da JVM;</li>
 *     <li>Cancelamento cooperativo: a busca consulta o pedido a cada passo e para logo depois dele;</li>
 *     <li>Tempo máximo opcional: ao passar do limite, o cálculo é interrompido com status TEMPO_ESGOTADO;</li>
 *     <li>Os cálculos terminados ficam na memória para consulta, até o limite do histórico.</li>
 * </ul>
 */
@Service
public class CalculoService {

    private static final Logger log = LoggerFactory.getLogger(CalculoService.class);

    private final CalculadoraPasseios calculadora;
    private final Duration tempoMaximo;
    private final int historicoMaximo;

    private final ForkJoinPool pool;
    private final ScheduledExecutorService agendador;

    private final Map<UUID, Calculo> calculos = new ConcurrentHashMap<>();
    private final AtomicReference<Calculo> emAndamento = new AtomicReference<>();

    /**
     * @param paralelismo     threads do cálculo; 0 = uma por núcleo da máquina
     * @param tempoMaximo     duração máxima de um cálculo; zero = sem limite
     * @param historicoMaximo quantos cálculos terminados manter para consulta
     */
    public CalculoService(
            CalculadoraPasseios calculadora,
            @Value("${passeio.paralelismo:0}") int paralelismo,
            @Value("${passeio.tempo-maximo:0s}") Duration tempoMaximo,
            @Value("${passeio.historico-maximo:100}") int historicoMaximo) {

        this.calculadora = calculadora;
        this.tempoMaximo = tempoMaximo;
        this.historicoMaximo = Math.max(1, historicoMaximo);

        final int threads = paralelismo > 0 ? paralelismo : Runtime.getRuntime().availableProcessors();
        this.pool = new ForkJoinPool(threads, CalculoService::novaThreadDoCalculo, null, false);

        this.agendador = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "calculo-passeio-tempo-maximo");
            t.setDaemon(true);
            return t;
        });

        log.info("Cálculos com {} threads; tempo máximo: {}", threads,
                tempoMaximo.isZero() || tempoMaximo.isNegative() ? "sem limite" : tempoMaximo);
    }

    private static ForkJoinWorkerThread novaThreadDoCalculo(ForkJoinPool pool) {
        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        thread.setName("calculo-passeio-" + thread.getPoolIndex());
        return thread;
    }

    /**
     * Inicia o cálculo em segundo plano e retorna na hora.
     *
     * @throws CalculoEmAndamentoException se já há um cálculo rodando
     */
    public Calculo iniciar(int dimensao) {
        final int casasTotal = new Solucao7(new Tabuleiro(dimensao)).casasRepresentantes().size();
        final Calculo calculo = new Calculo(UUID.randomUUID(), dimensao, casasTotal);

        if (!emAndamento.compareAndSet(null, calculo)) {
            Calculo atual = emAndamento.get();
            throw new CalculoEmAndamentoException(atual != null ? atual.id() : null);
        }

        calculos.put(calculo.id(), calculo);
        limparHistorico();

        final ScheduledFuture<?> limite = agendarTempoMaximo(calculo);
        try {
            pool.execute(() -> executar(calculo, limite));
        } catch (RejectedExecutionException e) {
            // O pool só recusa tarefas quando o servidor está desligando
            if (limite != null) limite.cancel(false);
            emAndamento.compareAndSet(calculo, null);
            calculo.falhar(e);
            throw e;
        }

        log.info("Cálculo {} iniciado ({}x{})", calculo.id(), dimensao, dimensao);
        return calculo;
    }

    private ScheduledFuture<?> agendarTempoMaximo(Calculo calculo) {
        if (tempoMaximo.isZero() || tempoMaximo.isNegative()) return null;
        return agendador.schedule(
                () -> calculo.solicitarCancelamento(StatusCalculo.TEMPO_ESGOTADO),
                tempoMaximo.toMillis(), TimeUnit.MILLISECONDS);
    }

    // Roda dentro do pool próprio: os streams paralelos da busca usam este mesmo pool
    private void executar(Calculo calculo, ScheduledFuture<?> limite) {
        Runnable registrarFim;
        try {
            final long total = calculadora.calcular(calculo.dimensao(), calculo);
            registrarFim = calculo.cancelado() ? calculo::interromper : () -> calculo.concluir(total);
        } catch (Throwable erro) {
            if (calculo.cancelado()) {
                registrarFim = calculo::interromper;
            } else {
                log.error("Cálculo {} falhou", calculo.id(), erro);
                registrarFim = () -> calculo.falhar(erro);
            }
        } finally {
            if (limite != null) limite.cancel(false);
            // Libera a vaga antes de registrar o fim: quem esperava pelo resultado já pode iniciar outro
            emAndamento.compareAndSet(calculo, null);
        }
        registrarFim.run();
        log.info("Cálculo {} terminou: {} em {} ms", calculo.id(), calculo.status(), calculo.tempoExecucaoMs());
    }

    public Optional<Calculo> buscar(UUID id) {
        return Optional.ofNullable(calculos.get(id));
    }

    /** Cálculo rodando agora, se houver. */
    public Optional<Calculo> emAndamento() {
        return Optional.ofNullable(emAndamento.get());
    }

    /**
     * Pede o cancelamento. A busca para logo depois; o status muda para CANCELADO quando ela parar.
     *
     * @throws NoSuchElementException se o id não existe
     * @throws IllegalStateException  se o cálculo já terminou
     */
    public Calculo cancelar(UUID id) {
        final Calculo calculo = buscar(id).orElseThrow(() -> new NoSuchElementException("Cálculo não encontrado: " + id));
        if (!calculo.solicitarCancelamento(StatusCalculo.CANCELADO)) {
            throw new IllegalStateException("O cálculo já terminou com status " + calculo.status());
        }
        log.info("Cancelamento pedido para o cálculo {}", id);
        return calculo;
    }

    // Remove os cálculos terminados mais antigos quando o histórico passa do limite
    private void limparHistorico() {
        final int excesso = calculos.size() - historicoMaximo;
        if (excesso <= 0) return;

        calculos.values().stream()
                .filter(c -> c.status().finalizado())
                .sorted(Comparator.comparing(Calculo::finalizadoEm))
                .limit(excesso)
                .forEach(c -> calculos.remove(c.id()));
    }

    /** Ao desligar o servidor, cancela o cálculo em andamento e encerra as threads. */
    @PreDestroy
    public void encerrar() {
        emAndamento().ifPresent(c -> c.solicitarCancelamento(StatusCalculo.CANCELADO));
        agendador.shutdownNow();
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
