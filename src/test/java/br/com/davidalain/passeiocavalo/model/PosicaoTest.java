package br.com.davidalain.passeiocavalo.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PosicaoTest {

    @Test
    void canto_tem_dois_movimentos() {
        assertThat(new Posicao(0, 0).movimentosPossiveisCavalo(5))
                .containsExactlyInAnyOrder(new Posicao(1, 2), new Posicao(2, 1));
    }

    @Test
    void centro_do_5x5_tem_oito_movimentos() {
        assertThat(new Posicao(2, 2).movimentosPossiveisCavalo(5))
                .containsExactlyInAnyOrder(
                        new Posicao(0, 1), new Posicao(0, 3), new Posicao(4, 1), new Posicao(4, 3),
                        new Posicao(1, 0), new Posicao(3, 0), new Posicao(1, 4), new Posicao(3, 4));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void tabuleiros_pequenos_nao_tem_movimentos(int n) {
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                assertTrue(new Posicao(i, j).movimentosPossiveisCavalo(n).isEmpty());
    }

    @Test
    void compareTo_ordena_por_linha_e_depois_coluna() {
        assertTrue(new Posicao(0, 4).compareTo(new Posicao(1, 0)) < 0);
        assertTrue(new Posicao(1, 2).compareTo(new Posicao(1, 1)) > 0);
        assertEquals(0, new Posicao(3, 3).compareTo(new Posicao(3, 3)));
    }

    @Test
    void indice_linear() {
        assertEquals(0, new Posicao(0, 0).indice(5));
        assertEquals(7, new Posicao(1, 2).indice(5));
        assertEquals(24, new Posicao(4, 4).indice(5));
    }

    @Test
    void posicaoValida_verifica_limites() {
        assertTrue(Posicao.posicaoValida(0, 0, 5));
        assertTrue(Posicao.posicaoValida(4, 4, 5));
        assertFalse(Posicao.posicaoValida(-1, 0, 5));
        assertFalse(Posicao.posicaoValida(0, 5, 5));
    }

    @Test
    void toString_legivel() {
        assertEquals("p[1,2]", new Posicao(1, 2).toString());
    }

    @Test
    void espelho_original_exemplos_do_5x5() {
        assertEquals(new Posicao(0, 0), new Posicao(4, 4).posicaoEspelhoOriginal(5));
        assertEquals(new Posicao(0, 0), new Posicao(0, 4).posicaoEspelhoOriginal(5));
        assertEquals(new Posicao(0, 1), new Posicao(3, 0).posicaoEspelhoOriginal(5));
        assertEquals(new Posicao(2, 2), new Posicao(2, 2).posicaoEspelhoOriginal(5));
        assertEquals(new Posicao(1, 2), new Posicao(2, 3).posicaoEspelhoOriginal(5));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void espelho_original_fica_no_triangulo_e_e_idempotente(int n) {
        int metade = (n + 1) / 2;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Posicao espelho = new Posicao(i, j).posicaoEspelhoOriginal(n);
                assertTrue(espelho.linha() <= espelho.coluna());
                assertTrue(espelho.coluna() < metade);
                assertEquals(espelho, espelho.posicaoEspelhoOriginal(n));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void as_8_simetrias_tem_o_mesmo_espelho_original(int n) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Posicao p = new Posicao(i, j);
                Posicao espelho = p.posicaoEspelhoOriginal(n);
                int a = n - 1 - i, b = n - 1 - j;
                List<Posicao> simetricas = List.of(
                        new Posicao(i, j), new Posicao(j, i), new Posicao(a, j), new Posicao(i, b),
                        new Posicao(a, b), new Posicao(b, a), new Posicao(j, a), new Posicao(b, i));
                for (Posicao s : simetricas) {
                    assertEquals(espelho, s.posicaoEspelhoOriginal(n), "Simetria de " + p + ": " + s);
                }
            }
        }
    }

    @Test
    void quantidade_de_representantes() {
        assertEquals(1, representantes(1));
        assertEquals(3, representantes(3)); // (0,0), (0,1), (1,1)
        assertEquals(3, representantes(4));
        assertEquals(6, representantes(5));
    }

    private static int representantes(int n) {
        Set<Posicao> set = new HashSet<>();
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                set.add(new Posicao(i, j).posicaoEspelhoOriginal(n));
        return set.size();
    }
}
