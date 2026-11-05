package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import br.com.davidalain.passeiocavalo.model.TreeNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static br.com.davidalain.passeiocavalo.ValoresEsperados.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes das soluções que contam os passeios e devolvem a árvore com todos eles (Soluções 3, 4 e 5).
 */
class ContagemComArvoreTest {

    /** Executa a solução para uma casa e devolve a raiz da árvore. */
    private static final BiFunction<Tabuleiro, Posicao, TreeNode<Posicao>> SOLUCAO3 =
            (t, p) -> new Solucao3(t).encontrarPasseioDoCavalo3(p);
    private static final BiFunction<Tabuleiro, Posicao, TreeNode<Posicao>> SOLUCAO4 =
            (t, p) -> new Solucao4(t).encontrarPasseioDoCavalo4(p);
    private static final BiFunction<Tabuleiro, Posicao, TreeNode<Posicao>> SOLUCAO5 =
            (t, p) -> new Solucao5(t).encontrarPasseioDoCavalo5(p);

    static Stream<Arguments> solucoesEDimensoes() {
        return Stream.of(SOLUCAO3, SOLUCAO4, SOLUCAO5)
                .flatMap(s -> IntStream.rangeClosed(1, DIMENSAO_MAXIMA_TESTES)
                        .mapToObj(n -> Arguments.of(nome(s), s, n)));
    }

    private static String nome(Object s) {
        return s == SOLUCAO3 ? "Solucao3" : s == SOLUCAO4 ? "Solucao4" : "Solucao5";
    }

    @ParameterizedTest(name = "{0} N={2}")
    @MethodSource("solucoesEDimensoes")
    void conta_os_passeios_de_cada_casa_e_a_arvore_contem_exatamente_eles(
            String nome, BiFunction<Tabuleiro, Posicao, TreeNode<Posicao>> solucao, int n) {

        Tabuleiro t = new Tabuleiro(n);
        for (int i = 0; i < t.totalCasas(); i++) {
            Posicao inicio = t.getPosicaoOrdenada(i);
            long esperado = passeiosPorCasa(n, inicio);

            TreeNode<Posicao> raiz = solucao.apply(t, inicio);

            assertEquals(esperado, t.mapaPosicaoInicialQuantidadeSolucoes().get(inicio), nome + " em " + inicio);

            if (esperado == 0) {
                assertNull(raiz, "Sem passeios, a solução deve retornar null");
                continue;
            }

            // Cada folha da árvore é o fim de um passeio diferente
            assertNotNull(raiz);
            assertEquals(inicio, raiz.value);
            List<TreeNode<Posicao>> folhas = raiz.getLeaves();
            assertEquals(esperado, folhas.size(), "A árvore deve guardar só os passeios");
            for (TreeNode<Posicao> folha : folhas) {
                assertEquals(t.totalCasas(), folha.depth);
                assertPasseioValido(n, inicio, folha.getPathFromRoot());
            }
            assertEquals(esperado, folhas.stream().map(TreeNode::getPathFromRoot).distinct().count());
        }

        assertEquals(TOTAL_POR_DIMENSAO.get(n), t.totalSolucoes());
    }
}
