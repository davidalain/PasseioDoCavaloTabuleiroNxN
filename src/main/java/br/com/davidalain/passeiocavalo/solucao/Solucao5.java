package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import br.com.davidalain.passeiocavalo.model.TreeNode;

import java.util.List;
import java.util.stream.Stream;

/**
 * <h2>Solução 5 — Máscara de bits no lugar da matriz de visitados</h2>
 *
 * <b>Objetivo:</b> o mesmo das Soluções 3 e 4 (contar todos os passeios e montar a árvore), gastando menos
 * memória e tempo por passo.
 * <p>
 * <b>Lógica:</b> igual à Solução 4, mas as casas visitadas ficam em um único {@code long}: o bit
 * {@code linha * N + coluna} ligado significa "casa visitada".
 * <ul>
 *     <li>Testar uma casa: {@code (mascara & (1L << indice)) != 0};</li>
 *     <li>Marcar uma casa: {@code mascara | (1L << indice)}, que gera um <i>novo</i> long.</li>
 * </ul>
 * Um {@code long} é passado por valor, então cada ramo recebe sua cópia de graça: some a cópia O(N²) da
 * matriz e o lixo que ela gerava para o coletor.
 * <p>
 * <b>Limite:</b> um long tem 64 bits, então o tabuleiro pode ter no máximo 8x8 casas.
 */
public record Solucao5(
        Tabuleiro tabuleiro
) {

    /**
     * Conta os passeios a partir de {@code posicaoInicial}, gravando o total em
     * {@link Tabuleiro#mapaPosicaoInicialQuantidadeSolucoes()}.
     *
     * @return a raiz da árvore com todos os passeios encontrados, ou {@code null} se não há nenhum
     * @throws IllegalArgumentException se o tabuleiro for maior que 8x8
     */
    public TreeNode<Posicao> encontrarPasseioDoCavalo5(Posicao posicaoInicial) {
        tabuleiro.validarDimensaoBitmask();

        final TreeNode<Posicao> raiz = new TreeNode<>(posicaoInicial, 1, null);
        final long bitMaskVisitados = Tabuleiro.marcarPosicaoVisitada(0L, tabuleiro.dimensao(), posicaoInicial);

        tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().put(posicaoInicial, 0L);
        encontrarPasseioRecursivo5(raiz, bitMaskVisitados, 1);

        return tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().get(posicaoInicial) > 0 ? raiz : null;
    }

    private void encontrarPasseioRecursivo5(TreeNode<Posicao> noAtual, long bitMaskVisitados, int movimentos) {
        // Caso base: visitou todas as casas -> é um passeio completo
        if (movimentos == tabuleiro.totalCasas()) {
            final Posicao primeiraPosicao = noAtual.getRoot().value;
            tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().merge(primeiraPosicao, 1L, Long::sum);
            return;
        }

        final List<Posicao> proximosMovimentos = tabuleiro.grafoMovimentos().get(noAtual.value);

        // Paraleliza apenas no início da árvore (como na Solução 4)
        final boolean usarParalelo = movimentos < tabuleiro.totalCasas() / 4;
        final Stream<Posicao> stream = usarParalelo ?
                proximosMovimentos.parallelStream() :
                proximosMovimentos.stream();

        stream.forEach(proximaPosicao -> {
            if (Tabuleiro.isPosicaoVisitada(bitMaskVisitados, tabuleiro.dimensao(), proximaPosicao)) return;

            final TreeNode<Posicao> filho = noAtual.addChild(proximaPosicao);
            final long bitMaskVisitadosNovo =
                    Tabuleiro.marcarPosicaoVisitada(bitMaskVisitados, tabuleiro.dimensao(), proximaPosicao);

            encontrarPasseioRecursivo5(filho, bitMaskVisitadosNovo, movimentos + 1);
        });

        // Beco sem saída (nenhum filho sobrou): remove o nó da árvore
        if (noAtual.childNodes.isEmpty() && noAtual.parentNode != null) {
            noAtual.parentNode.deleteChild(noAtual);
        }
    }
}
