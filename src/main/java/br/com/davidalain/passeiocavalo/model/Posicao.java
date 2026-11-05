package br.com.davidalain.passeiocavalo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Uma casa do tabuleiro, identificada por linha e coluna (começando em 0).
 */
public record Posicao(int linha, int coluna) implements Comparable<Posicao> {

    /** Os 8 deslocamentos possíveis do cavalo: (linha, coluna). */
    private static final int[][] DESLOCAMENTOS_CAVALO = {
            {-2, -1}, {-2, 1},  // dois para cima
            {2, -1}, {2, 1},    // dois para baixo
            {-1, -2}, {1, -2},  // dois para a esquerda
            {-1, 2}, {1, 2}     // dois para a direita
    };

    @Override
    public int compareTo(Posicao o) {
        int comp = Integer.compare(this.linha, o.linha);
        if (comp == 0) {
            return Integer.compare(this.coluna, o.coluna);
        }
        return comp;
    }

    /**
     * Retorna as casas que o cavalo alcança a partir desta, dentro de um tabuleiro de lado {@code dimensaoTabuleiro}.
     */
    public List<Posicao> movimentosPossiveisCavalo(int dimensaoTabuleiro) {
        List<Posicao> movimentos = new ArrayList<>();
        for (int[] deslocamento : DESLOCAMENTOS_CAVALO) {
            int novaLinha = this.linha + deslocamento[0];
            int novaColuna = this.coluna + deslocamento[1];
            if (posicaoValida(novaLinha, novaColuna, dimensaoTabuleiro)) {
                movimentos.add(new Posicao(novaLinha, novaColuna));
            }
        }
        return movimentos;
    }

    /**
     * Índice linear da casa (linha * dimensão + coluna), de 0 a dimensão² - 1.
     * É o número do bit que representa esta casa na máscara de casas visitadas.
     */
    public int indice(int dimensaoTabuleiro) {
        return this.linha * dimensaoTabuleiro + this.coluna;
    }

    /**
     * Retorna o "espelho original" desta casa: o representante dela entre as 8 simetrias do quadrado
     * (4 rotações e 4 reflexões).
     * <p>
     * Duas casas simétricas têm a mesma quantidade de passeios, porque cada passeio de uma pode ser girado/refletido
     * para virar um passeio da outra. Por isso basta calcular o representante e reaproveitar o resultado.
     * <p>
     * Como funciona:
     * <ol>
     *     <li>Reflete a linha para a metade de cima: linhas com índice &ge; ceil(N/2) viram {@code N - linha - 1};</li>
     *     <li>Reflete a coluna para a metade da esquerda, do mesmo jeito;</li>
     *     <li>Reflete na diagonal principal, garantindo {@code linha <= coluna}.</li>
     * </ol>
     * Exemplo para N=5 (linha -> resultado): 0->0, 1->1, 2->2, 3->1, 4->0.
     * Exemplo para N=6: 0->0, 1->1, 2->2, 3->2, 4->1, 5->0.
     *
     * @param dimensaoTabuleiro lado do tabuleiro (N)
     * @return a casa representante, sempre no triângulo {@code linha <= coluna} do quadrante superior esquerdo
     */
    public Posicao posicaoEspelhoOriginal(int dimensaoTabuleiro) {
        int indiceMax = (dimensaoTabuleiro + 1) / 2; // ceil(N/2)

        int linhaEspelho = this.linha >= indiceMax ? (dimensaoTabuleiro - this.linha - 1) : this.linha;
        int colunaEspelho = this.coluna >= indiceMax ? (dimensaoTabuleiro - this.coluna - 1) : this.coluna;

        if (linhaEspelho > colunaEspelho)
            return new Posicao(colunaEspelho, linhaEspelho);
        else
            return new Posicao(linhaEspelho, colunaEspelho);
    }

    public static boolean posicaoValida(int linha, int coluna, int dimensao) {
        return indiceValido(linha, dimensao) && indiceValido(coluna, dimensao);
    }

    private static boolean indiceValido(int indice, int dimensao) {
        return indice >= 0 && indice < dimensao;
    }

    @Override
    public String toString() {
        return "p[" + linha + "," + coluna + "]";
    }
}
