package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static br.com.davidalain.passeiocavalo.ValoresEsperados.assertPasseioValido;
import static br.com.davidalain.passeiocavalo.ValoresEsperados.passeiosPorCasa;
import static org.junit.jupiter.api.Assertions.*;

/** Testes das soluções que procuram um único passeio (Solução 1 e Solução 2). */
class BuscaUmPasseioTest {

    // ---------------- Solução 1 (gulosa) ----------------

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void solucao1_so_retorna_passeios_validos_e_nunca_onde_nao_existe(int n) {
        Tabuleiro t = new Tabuleiro(n);
        Solucao1 s = new Solucao1(t);
        for (int i = 0; i < t.totalCasas(); i++) {
            Posicao inicio = t.getPosicaoOrdenada(i);
            List<Posicao> caminho = s.encontrarPasseioDoCavalo(inicio);
            if (passeiosPorCasa(n, inicio) == 0) {
                assertNull(caminho, "Não existe passeio a partir de " + inicio);
            } else if (caminho != null) {
                assertPasseioValido(n, inicio, caminho);
            }
        }
    }

    @Test
    void solucao1_encontra_passeio_a_partir_do_canto_do_5x5() {
        Posicao canto = new Posicao(0, 0);
        assertPasseioValido(5, canto, new Solucao1(new Tabuleiro(5)).encontrarPasseioDoCavalo(canto));
    }

    @Test
    void solucao1_pode_falhar_onde_existe_passeio() {
        // A heurística não volta atrás: em (1,1) do 5x5 existem 56 passeios, mas a busca gulosa fica sem saída
        Posicao inicio = new Posicao(1, 1);
        assertEquals(56, passeiosPorCasa(5, inicio));
        assertNull(new Solucao1(new Tabuleiro(5)).encontrarPasseioDoCavalo(inicio));
    }

    @Test
    void solucao1_tabuleiro_1x1() {
        assertEquals(List.of(new Posicao(0, 0)), new Solucao1(new Tabuleiro(1)).encontrarPasseioDoCavalo(new Posicao(0, 0)));
    }

    // ---------------- Solução 2 (backtracking) ----------------

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void solucao2_encontra_passeio_se_e_somente_se_existe(int n) {
        Tabuleiro t = new Tabuleiro(n);
        Solucao2 s = new Solucao2(t);
        for (int i = 0; i < t.totalCasas(); i++) {
            Posicao inicio = t.getPosicaoOrdenada(i);
            List<Posicao> caminho = s.encontrarPasseioDoCavalo2(inicio);
            if (passeiosPorCasa(n, inicio) > 0) {
                assertPasseioValido(n, inicio, caminho);
            } else {
                assertNull(caminho, "Não existe passeio a partir de " + inicio);
            }
        }
    }

    @Test
    void solucao2_nao_altera_o_grafo_compartilhado() {
        Tabuleiro t = new Tabuleiro(5);
        Map<Posicao, List<Posicao>> antes = new HashMap<>(t.grafoMovimentos());
        new Solucao2(t).encontrarPasseioDoCavalo2(new Posicao(0, 0));
        assertEquals(antes, t.grafoMovimentos());
    }
}
