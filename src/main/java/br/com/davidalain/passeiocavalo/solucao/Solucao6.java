package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.model.Tabuleiro;

import java.util.List;
import java.util.stream.Stream;

/**
 * <h2>Solução 6 — Só contar: sem árvore e sem estado compartilhado</h2>
 *
 * <b>Objetivo:</b> contar todos os passeios a partir de uma casa, sem guardá-los.
 * <p>
 * <b>Lógica:</b> mesma busca da Solução 5 (máscara de bits, paralelismo só no topo), mas cada chamada
 * recursiva <i>retorna</i> quantos passeios existem abaixo dela:
 * <pre>
 *   contar(casa, visitados) = 1                                   se visitou as N² casas
 *                           = soma de contar(vizinho, visitados + vizinho)   para cada vizinho livre
 * </pre>
 * Nos níveis paralelos a soma é feita com {@code mapToLong(...).sum()}, que junta os resultados parciais
 * de cada thread sem nenhuma trava.
 * <p>
 * <b>Ganhos sobre a Solução 5:</b>
 * <ul>
 *     <li>Não cria um {@link br.com.davidalain.passeiocavalo.model.TreeNode} por casa visitada, e a memória
 *         fica constante: a árvore da Solução 5 guardava todos os passeios (milhões de nós no 6x6);</li>
 *     <li>As threads não disputam o mesmo mapa a cada passeio encontrado: cada uma soma no próprio contador local;</li>
 *     <li>A contagem é {@code long}, sem risco de estouro (no 7x7 há mais de 2³¹ passeios por casa).</li>
 * </ul>
 */
public record Solucao6(
        Tabuleiro tabuleiro
) {

    /**
     * Conta os passeios a partir de {@code posicaoInicial} e grava o total em
     * {@link Tabuleiro#mapaPosicaoInicialQuantidadeSolucoes()}.
     *
     * @return a quantidade de passeios que começam em {@code posicaoInicial}
     * @throws IllegalArgumentException se o tabuleiro for maior que 8x8
     */
    public long encontrarPasseioDoCavalo6(Posicao posicaoInicial) {
        tabuleiro.validarDimensaoBitmask();

        final long bitMaskVisitados = Tabuleiro.marcarPosicaoVisitada(0L, tabuleiro.dimensao(), posicaoInicial);
        final long quantidade = contarPasseios(posicaoInicial, bitMaskVisitados, 1);

        tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().put(posicaoInicial, quantidade);
        return quantidade;
    }

    private long contarPasseios(Posicao atual, long bitMaskVisitados, int movimentos) {
        // Caso base: visitou todas as casas -> este caminho é um passeio
        if (movimentos == tabuleiro.totalCasas()) {
            return 1;
        }

        final List<Posicao> proximosMovimentos = tabuleiro.grafoMovimentos().get(atual);

        // Paraleliza apenas no início da árvore
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
