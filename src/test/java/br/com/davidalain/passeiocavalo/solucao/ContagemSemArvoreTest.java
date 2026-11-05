package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static br.com.davidalain.passeiocavalo.ValoresEsperados.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Testes das soluções que só contam os passeios (Soluções 6 e 7). */
class ContagemSemArvoreTest {

    // ---------------- Solução 6 ----------------

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void solucao6_conta_os_passeios_de_cada_casa(int n) {
        Tabuleiro t = new Tabuleiro(n);
        Solucao6 s = new Solucao6(t);
        for (int i = 0; i < t.totalCasas(); i++) {
            Posicao inicio = t.getPosicaoOrdenada(i);
            long esperado = passeiosPorCasa(n, inicio);
            assertEquals(esperado, s.encontrarPasseioDoCavalo6(inicio), "Casa " + inicio);
            assertEquals(esperado, t.mapaPosicaoInicialQuantidadeSolucoes().get(inicio));
        }
        assertEquals(TOTAL_POR_DIMENSAO.get(n), t.totalSolucoes());
    }

    // ---------------- Solução 7 ----------------

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void solucao7_conta_todos_os_passeios_do_tabuleiro(int n) {
        Tabuleiro t = new Tabuleiro(n);
        assertEquals(TOTAL_POR_DIMENSAO.get(n), new Solucao7(t).contarTodosOsPasseios());
        assertEquals(t.totalCasas(), t.mapaPosicaoInicialQuantidadeSolucoes().size(), "Todas as casas preenchidas");
        for (int i = 0; i < t.totalCasas(); i++) {
            Posicao p = t.getPosicaoOrdenada(i);
            assertEquals(passeiosPorCasa(n, p), t.mapaPosicaoInicialQuantidadeSolucoes().get(p), "Casa " + p);
        }
    }

    @Test
    void solucao7_uma_casa_isolada_calcula_pelo_espelho() {
        Tabuleiro t = new Tabuleiro(5);
        Posicao canto = new Posicao(4, 4);

        assertEquals(304, new Solucao7(t).encontrarPasseioDoCavalo7(canto));

        // Guardou a casa pedida e também o espelho original dela
        assertEquals(304, t.mapaPosicaoInicialQuantidadeSolucoes().get(canto));
        assertEquals(304, t.mapaPosicaoInicialQuantidadeSolucoes().get(new Posicao(0, 0)));
        assertEquals(2, t.mapaPosicaoInicialQuantidadeSolucoes().size());
    }

    @Test
    void solucao7_reaproveita_o_valor_do_espelho_ja_calculado() {
        Tabuleiro t = new Tabuleiro(5);
        // Um valor "falso" no espelho prova que a casa simétrica não refaz a busca
        t.mapaPosicaoInicialQuantidadeSolucoes().put(new Posicao(0, 0), 999L);
        assertEquals(999, new Solucao7(t).encontrarPasseioDoCavalo7(new Posicao(0, 4)));
    }

    @RepeatedTest(5)
    void solucao7_resultado_estavel_com_paralelismo() {
        assertEquals(1728, new Solucao7(new Tabuleiro(5)).contarTodosOsPasseios());
    }

    @Test
    void solucao6_e_solucao7_concordam_casa_a_casa() {
        Tabuleiro t6 = new Tabuleiro(5);
        Tabuleiro t7 = new Tabuleiro(5);
        Solucao6 s6 = new Solucao6(t6);
        Solucao7 s7 = new Solucao7(t7);
        for (int i = 0; i < 25; i++) {
            Posicao p = t6.getPosicaoOrdenada(i);
            assertEquals(s6.encontrarPasseioDoCavalo6(p), s7.encontrarPasseioDoCavalo7(p), "Casa " + p);
        }
    }
}
