package br.com.davidalain.passeiocavalo.servico;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.solucao.ControleExecucao;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Um cálculo assíncrono: guarda o estado e o progresso, e serve de {@link ControleExecucao} para a busca.
 * <p>
 * A busca roda em várias threads e escreve aqui o progresso; as requisições HTTP leem ao mesmo tempo.
 * Por isso os contadores são atômicos e as mudanças de status são sincronizadas.
 */
public final class Calculo implements ControleExecucao {

    private final UUID id;
    private final int dimensao;
    private final int casasTotal;
    private final Instant iniciadoEm = Instant.now();
    private final long inicioNanos = System.nanoTime();

    // Progresso, atualizado pela busca (LongAdder aguenta muitas threads somando ao mesmo tempo)
    private final LongAdder passeiosEncontrados = new LongAdder();
    private final AtomicInteger casasCalculadas = new AtomicInteger();

    // Pedido de cancelamento, lido a cada passo da busca
    private volatile boolean cancelamentoSolicitado;
    private volatile StatusCalculo motivoCancelamento;

    private volatile StatusCalculo status = StatusCalculo.EM_ANDAMENTO;
    private volatile Long totalSolucoes;
    private volatile String erro;
    private volatile Instant finalizadoEm;
    private volatile long duracaoNanos = -1;

    private final CompletableFuture<Calculo> conclusao = new CompletableFuture<>();

    Calculo(UUID id, int dimensao, int casasTotal) {
        this.id = id;
        this.dimensao = dimensao;
        this.casasTotal = casasTotal;
    }

    public UUID id() {
        return id;
    }

    public int dimensao() {
        return dimensao;
    }

    public StatusCalculo status() {
        return status;
    }

    public Long totalSolucoes() {
        return totalSolucoes;
    }

    public String erro() {
        return erro;
    }

    public Instant finalizadoEm() {
        return finalizadoEm;
    }

    /** Duração até agora, ou a duração total se já terminou. */
    public long tempoExecucaoMs() {
        long nanos = duracaoNanos >= 0 ? duracaoNanos : System.nanoTime() - inicioNanos;
        return nanos / 1_000_000;
    }

    /** Bloqueia até o cálculo terminar (com sucesso ou não) e devolve este cálculo. */
    public Calculo aguardar() {
        return conclusao.join();
    }

    // ---------------- ControleExecucao (chamado pela busca) ----------------

    @Override
    public boolean cancelado() {
        return cancelamentoSolicitado;
    }

    @Override
    public void passeioEncontrado() {
        passeiosEncontrados.increment();
    }

    @Override
    public void casaCalculada(Posicao casa, long quantidade) {
        casasCalculadas.incrementAndGet();
    }

    // ---------------- Mudanças de estado (chamadas pelo CalculoService) ----------------

    /**
     * Pede para a busca parar. Só o primeiro pedido vale (define o motivo).
     *
     * @return {@code false} se o cálculo já tinha terminado
     */
    synchronized boolean solicitarCancelamento(StatusCalculo motivo) {
        if (status.finalizado()) return false;
        if (!cancelamentoSolicitado) {
            motivoCancelamento = motivo;
            cancelamentoSolicitado = true;
        }
        return true;
    }

    void concluir(long total) {
        finalizar(StatusCalculo.CONCLUIDO, total, null);
    }

    /** Encerra um cálculo que parou por pedido de cancelamento. */
    void interromper() {
        StatusCalculo motivo = motivoCancelamento != null ? motivoCancelamento : StatusCalculo.CANCELADO;
        String mensagem = motivo == StatusCalculo.TEMPO_ESGOTADO
                ? "Cálculo interrompido: passou do tempo máximo configurado."
                : "Cálculo cancelado.";
        finalizar(motivo, null, mensagem);
    }

    void falhar(Throwable causa) {
        finalizar(StatusCalculo.FALHOU, null, "Erro no cálculo: " + causa);
    }

    private synchronized void finalizar(StatusCalculo novoStatus, Long total, String mensagem) {
        if (status.finalizado()) return;
        duracaoNanos = System.nanoTime() - inicioNanos;
        finalizadoEm = Instant.now();
        totalSolucoes = total;
        erro = mensagem;
        status = novoStatus; // por último: quem lê o status já encontra os outros campos preenchidos
        conclusao.complete(this);
    }

    /** Fotografia do estado atual, para a resposta HTTP. */
    public CalculoResposta resposta() {
        final StatusCalculo statusAtual = status;
        final int calculadas = casasCalculadas.get();
        final double percentual = statusAtual == StatusCalculo.CONCLUIDO ? 100.0
                : casasTotal == 0 ? 0.0
                : Math.round(calculadas * 1000.0 / casasTotal) / 10.0;

        return new CalculoResposta(
                id,
                statusAtual,
                dimensao + "x" + dimensao,
                calculadas,
                casasTotal,
                percentual,
                passeiosEncontrados.sum(),
                totalSolucoes,
                tempoExecucaoMs(),
                iniciadoEm,
                finalizadoEm,
                erro);
    }
}
