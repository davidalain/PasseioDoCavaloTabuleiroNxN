package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.model.Tabuleiro;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * <h2>Solução 7 — Contagem com reaproveitamento por simetria (usada pela API)</h2>
 *
 * <b>Objetivo:</b> contar todos os passeios de um tabuleiro NxN, de todas as casas iniciais, fazendo o
 * mínimo de trabalho.
 * <p>
 * <b>Lógica:</b> a busca é a da Solução 6 (máscara de bits, paralelismo no topo, retorno somado em
 * {@code long}). O ganho vem da <b>simetria</b>: o quadrado tem 8 simetrias (4 rotações e 4 reflexões), e
 * casas simétricas têm a mesma quantidade de passeios. Por isso:
 * <ol>
 *     <li>Cada casa é levada ao seu "espelho original" ({@link Posicao#posicaoEspelhoOriginal(int)}),
 *         que fica no triângulo {@code linha <= coluna} do quadrante superior esquerdo;</li>
 *     <li>A busca roda só para esses representantes (6 de 25 casas no 5x5, 6 de 36 no 6x6,
 *         10 de 49 no 7x7);</li>
 *     <li>As outras casas copiam o resultado do seu representante.</li>
 * </ol>
 * <b>Execução longa:</b> o {@link ControleExecucao} opcional permite acompanhar o progresso (passeios
 * encontrados e casas concluídas) e cancelar a busca. A cada passo, a busca consulta
 * {@link ControleExecucao#cancelado()} e, se for o caso, para lançando {@link CancellationException}.
 */
public record Solucao7(
        Tabuleiro tabuleiro,
        ControleExecucao controle
) {

    /** Solução sem acompanhamento nem cancelamento. */
    public Solucao7(Tabuleiro tabuleiro) {
        this(tabuleiro, ControleExecucao.NENHUM);
    }

    /**
     * Conta os passeios que começam em {@code posicaoInicial}. Se o espelho original dela já foi calculado,
     * reaproveita o resultado; senão, calcula para o espelho original e guarda os dois no mapa.
     *
     * @return a quantidade de passeios que começam em {@code posicaoInicial}
     * @throws IllegalArgumentException se o tabuleiro for maior que 8x8
     * @throws CancellationException    se o {@link ControleExecucao} pedir o cancelamento
     */
    public long encontrarPasseioDoCavalo7(Posicao posicaoInicial) {
        tabuleiro.validarDimensaoBitmask();

        final Posicao espelhoOriginal = posicaoInicial.posicaoEspelhoOriginal(tabuleiro.dimensao());

        // Se o espelho original já foi calculado, só reaproveita
        Long quantidade = tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().get(espelhoOriginal);
        if (quantidade == null) {
            final long bitMaskVisitados = Tabuleiro.marcarPosicaoVisitada(0L, tabuleiro.dimensao(), espelhoOriginal);
            quantidade = contarPasseios(espelhoOriginal, bitMaskVisitados, 1);
            tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().put(espelhoOriginal, quantidade);
            controle.casaCalculada(espelhoOriginal, quantidade);
        }

        tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().put(posicaoInicial, quantidade);
        return quantidade;
    }

    /**
     * Casas que exigem busca: os espelhos originais, sem repetição e em ordem.
     * O tamanho desta lista é o total usado para medir o progresso.
     */
    public List<Posicao> casasRepresentantes() {
        final int n = tabuleiro.dimensao();
        return IntStream.range(0, tabuleiro.totalCasas())
                .mapToObj(i -> tabuleiro.getPosicaoOrdenada(i).posicaoEspelhoOriginal(n))
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Conta os passeios de todas as casas do tabuleiro, em duas fases:
     * <ol>
     *     <li>Calcula, em paralelo, só as casas "espelho original" (as únicas que exigem busca);</li>
     *     <li>Preenche as demais casas copiando o valor do seu espelho.</li>
     * </ol>
     * Separar as fases evita que duas threads calculem o mesmo espelho ao mesmo tempo.
     * <p>
     * Os streams paralelos usam o ForkJoinPool da thread que chama este método: chamado de dentro de um
     * {@link java.util.concurrent.ForkJoinPool} próprio, o cálculo não ocupa o pool comum da JVM.
     *
     * @return o total de passeios do tabuleiro (soma de todas as casas iniciais)
     * @throws CancellationException se o {@link ControleExecucao} pedir o cancelamento
     */
    public long contarTodosOsPasseios() {
        tabuleiro.validarDimensaoBitmask();

        // Fase 1: só os representantes, em paralelo
        casasRepresentantes()
                .parallelStream()
                .forEach(this::encontrarPasseioDoCavalo7);

        // Fase 2: todas as casas; aqui só há leitura do mapa, nenhuma busca nova
        IntStream.range(0, tabuleiro.totalCasas())
                .mapToObj(tabuleiro::getPosicaoOrdenada)
                .forEach(this::encontrarPasseioDoCavalo7);

        return tabuleiro.totalSolucoes();
    }

    private long contarPasseios(Posicao atual, long bitMaskVisitados, int movimentos) {
        // Ponto de cancelamento: consultado a cada passo, para a busca parar logo depois do pedido
        if (controle.cancelado()) {
            throw new CancellationException("Cálculo cancelado");
        }

        // Caso base: visitou todas as casas -> este caminho é um passeio
        if (movimentos == tabuleiro.totalCasas()) {
            controle.passeioEncontrado();
            return 1;
        }

        final List<Posicao> proximosMovimentos = tabuleiro.grafoMovimentos().get(atual);

        // Paraleliza apenas no início da árvore, onde as subárvores são grandes
        final boolean usarParalelo = movimentos < tabuleiro.totalCasas() / 4;
        final Stream<Posicao> stream = usarParalelo ?
                proximosMovimentos.parallelStream() :
                proximosMovimentos.stream();

        return stream
                .filter(proxima -> !Tabuleiro.isPosicaoVisitada(bitMaskVisitados, tabuleiro.dimensao(), proxima))
                .mapToLong(proxima -> contarPasseios(
                        proxima,
                        Tabuleiro.marcarPosicaoVisitada(bitMaskVisitados, tabuleiro.dimensao(), proxima),
                        movimentos + 1))
                .sum();
    }
}
