package br.com.davidalain.passeiocavalo.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tabuleiro NxN com o grafo de movimentos do cavalo pré-calculado.
 * <p>
 * O grafo ({@link #grafoMovimentos()}) é imutável e pode ser lido por várias threads ao mesmo tempo.
 * O mapa de resultados ({@link #mapaPosicaoInicialQuantidadeSolucoes()}) é um {@link ConcurrentHashMap},
 * onde as soluções guardam quantos passeios existem a partir de cada casa inicial.
 */
public final class Tabuleiro {

    /**
     * Maior dimensão suportada pelas soluções que usam máscara de bits:
     * um {@code long} tem 64 bits, um para cada casa de um tabuleiro 8x8.
     */
    public static final int DIMENSAO_MAXIMA_BITMASK = 8;

    private final int dimensao;
    private final Posicao[][] tabuleiro;
    private final Map<Posicao, List<Posicao>> grafoMovimentos;
    private final Map<Posicao, Long> mapaPosicaoInicialQuantidadeSolucoes = new ConcurrentHashMap<>();

    public Tabuleiro(int dimensao) {
        if (dimensao <= 0) {
            throw new IllegalArgumentException("Dimensão deve ser maior que zero");
        }
        this.dimensao = dimensao;
        this.tabuleiro = gerarTabuleiro(dimensao);
        this.grafoMovimentos = criarGrafoMovimentosCavalo(dimensao, this.tabuleiro);
    }

    public int dimensao() {
        return dimensao;
    }

    /** Quantidade de casas do tabuleiro (N²). */
    public int totalCasas() {
        return dimensao * dimensao;
    }

    public Posicao[][] tabuleiro() {
        return tabuleiro;
    }

    /** Para cada casa, a lista (imutável) de casas alcançáveis com um salto do cavalo. */
    public Map<Posicao, List<Posicao>> grafoMovimentos() {
        return grafoMovimentos;
    }

    /** Quantidade de passeios encontrados a partir de cada casa inicial. */
    public Map<Posicao, Long> mapaPosicaoInicialQuantidadeSolucoes() {
        return mapaPosicaoInicialQuantidadeSolucoes;
    }

    /** Soma dos passeios de todas as casas iniciais já calculadas. */
    public long totalSolucoes() {
        return mapaPosicaoInicialQuantidadeSolucoes.values().stream().mapToLong(Long::longValue).sum();
    }

    private static Posicao[][] gerarTabuleiro(int dimensao) {
        Posicao[][] tabuleiro = new Posicao[dimensao][dimensao];
        for (int i = 0; i < dimensao; i++) {
            for (int j = 0; j < dimensao; j++) {
                tabuleiro[i][j] = new Posicao(i, j);
            }
        }
        return tabuleiro;
    }

    /**
     * Retorna a casa de número {@code numOrdem}, contando da esquerda para a direita e de cima para baixo
     * (0 = canto superior esquerdo, N² - 1 = canto inferior direito).
     */
    public Posicao getPosicaoOrdenada(int numOrdem) {
        if (numOrdem < 0 || numOrdem >= totalCasas())
            throw new IllegalArgumentException("Posição inválida: " + numOrdem);

        return tabuleiro[numOrdem / dimensao][numOrdem % dimensao];
    }

    /** Imprime as coordenadas de todas as casas (útil para depuração). */
    public void imprimirTabuleiro() {
        for (int i = 0; i < dimensao; i++) {
            StringBuilder linha = new StringBuilder();
            for (int j = 0; j < dimensao; j++) {
                linha.append(tabuleiro[i][j]).append(' ');
            }
            System.out.println(linha);
        }
    }

    // Monta o grafo: cada casa aponta para as casas que o cavalo alcança a partir dela.
    private static Map<Posicao, List<Posicao>> criarGrafoMovimentosCavalo(int dimensao, Posicao[][] tabuleiro) {
        Map<Posicao, List<Posicao>> grafo = new HashMap<>();
        for (int i = 0; i < dimensao; i++) {
            for (int j = 0; j < dimensao; j++) {
                Posicao posicaoAtual = tabuleiro[i][j];
                grafo.put(posicaoAtual, List.copyOf(posicaoAtual.movimentosPossiveisCavalo(dimensao)));
            }
        }
        return Collections.unmodifiableMap(grafo);
    }

    // ---- Máscara de bits de casas visitadas: o bit i do long representa a casa de índice i ----

    /** Lança exceção se o tabuleiro não cabe em uma máscara de 64 bits. */
    public void validarDimensaoBitmask() {
        if (dimensao > DIMENSAO_MAXIMA_BITMASK) {
            throw new IllegalArgumentException("Esta implementação suporta no máximo " + DIMENSAO_MAXIMA_BITMASK + "x"
                    + DIMENSAO_MAXIMA_BITMASK + " (64 casas), mas o tabuleiro tem " + totalCasas() + " casas.");
        }
    }

    /** Indica se o bit da casa {@code posicao} está ligado na máscara. */
    public static boolean isPosicaoVisitada(long bitMaskVisitados, int dimensao, Posicao posicao) {
        return (bitMaskVisitados & (1L << posicao.indice(dimensao))) != 0;
    }

    /** Retorna uma nova máscara com o bit da casa {@code posicao} ligado. */
    public static long marcarPosicaoVisitada(long bitMaskVisitados, int dimensao, Posicao posicao) {
        return bitMaskVisitados | (1L << posicao.indice(dimensao));
    }
}
