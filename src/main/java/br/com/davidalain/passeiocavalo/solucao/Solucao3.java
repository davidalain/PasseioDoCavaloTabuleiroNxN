package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import br.com.davidalain.passeiocavalo.model.TreeNode;

import java.util.Arrays;
import java.util.List;

/**
 * <h2>Solução 3 — Contagem de todos os passeios com paralelismo em todos os níveis</h2>
 *
 * <b>Objetivo:</b> contar <i>todos</i> os passeios que começam em uma casa e guardá-los em uma árvore.
 * <p>
 * <b>Lógica:</b> busca exaustiva em profundidade, sem parar no primeiro passeio.
 * <ul>
 *     <li>Cada caminho parcial vira um ramo de uma árvore ({@link TreeNode}); a raiz é a casa inicial;</li>
 *     <li>Os vizinhos de cada nó são explorados com {@code parallelStream()}, ou seja, em paralelo
 *         no ForkJoinPool comum;</li>
 *     <li>Como os ramos rodam em threads diferentes, cada ramo recebe a <i>sua própria cópia</i> da
 *         matriz de visitados (não há desfazer, como no backtracking da Solução 2);</li>
 *     <li>Ao chegar a N² casas, soma 1 na contagem da casa inicial (com {@code merge}, que é atômico);</li>
 *     <li>Ramos sem saída são removidos da árvore. No fim, sobram só os caminhos que são passeios.</li>
 * </ul>
 * <b>Problemas desta versão</b> (resolvidos nas próximas):
 * paralelizar até as folhas cria tarefas pequenas demais (o custo de coordenar as threads supera o ganho);
 * copiar a matriz {@code boolean[][]} a cada passo custa O(N²); e a árvore guarda todos os passeios na memória.
 */
public record Solucao3(
        Tabuleiro tabuleiro
) {

    /**
     * Conta os passeios a partir de {@code inicio}, gravando o total em
     * {@link Tabuleiro#mapaPosicaoInicialQuantidadeSolucoes()}.
     *
     * @return a raiz da árvore com todos os passeios encontrados, ou {@code null} se não há nenhum
     */
    public TreeNode<Posicao> encontrarPasseioDoCavalo3(Posicao inicio) {
        final TreeNode<Posicao> raiz = new TreeNode<>(inicio, 1, null);
        final boolean[][] visitados = new boolean[tabuleiro.dimensao()][tabuleiro.dimensao()];
        visitados[inicio.linha()][inicio.coluna()] = true;

        tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().put(inicio, 0L);
        encontrarPasseioRecursivo3(raiz, visitados, 1);

        return tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().get(inicio) > 0 ? raiz : null;
    }

    private void encontrarPasseioRecursivo3(TreeNode<Posicao> noAtual, boolean[][] visitados, int movimentos) {
        // Caso base: visitou todas as casas -> é um passeio completo
        if (movimentos == tabuleiro.totalCasas()) {
            final Posicao primeiraPosicao = noAtual.getRoot().value;
            tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().merge(primeiraPosicao, 1L, Long::sum);
            return;
        }

        final List<Posicao> proximosMovimentos = tabuleiro.grafoMovimentos().get(noAtual.value);

        // Explora todos os vizinhos em paralelo, em qualquer profundidade
        proximosMovimentos.parallelStream().forEach(proximaPosicao -> {
            if (visitados[proximaPosicao.linha()][proximaPosicao.coluna()]) return;

            final TreeNode<Posicao> filho = noAtual.addChild(proximaPosicao);

            // Cópia própria dos visitados para este ramo (os ramos rodam em paralelo)
            final boolean[][] visitadosNovo = new boolean[tabuleiro.dimensao()][];
            for (int i = 0; i < tabuleiro.dimensao(); i++) {
                visitadosNovo[i] = Arrays.copyOf(visitados[i], tabuleiro.dimensao());
            }
            visitadosNovo[proximaPosicao.linha()][proximaPosicao.coluna()] = true;

            encontrarPasseioRecursivo3(filho, visitadosNovo, movimentos + 1);
        });

        // Se nenhum filho levou a um passeio, este ramo é inútil: remove para liberar memória
        if (noAtual.childNodes.isEmpty() && noAtual.parentNode != null) {
            noAtual.parentNode.deleteChild(noAtual);
        }
    }
}
