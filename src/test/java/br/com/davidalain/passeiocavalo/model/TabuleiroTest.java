package br.com.davidalain.passeiocavalo.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TabuleiroTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void dimensao_invalida_lanca_excecao(int dimensao) {
        assertThrows(IllegalArgumentException.class, () -> new Tabuleiro(dimensao));
    }

    @Test
    void cria_todas_as_casas() {
        Tabuleiro t = new Tabuleiro(5);
        assertEquals(5, t.dimensao());
        assertEquals(25, t.totalCasas());
        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 5; j++)
                assertEquals(new Posicao(i, j), t.tabuleiro()[i][j]);
    }

    @Test
    void posicao_ordenada_percorre_linha_a_linha() {
        Tabuleiro t = new Tabuleiro(5);
        assertEquals(new Posicao(0, 0), t.getPosicaoOrdenada(0));
        assertEquals(new Posicao(1, 2), t.getPosicaoOrdenada(7));
        assertEquals(new Posicao(4, 4), t.getPosicaoOrdenada(24));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 25})
    void posicao_ordenada_fora_do_tabuleiro_lanca_excecao(int numOrdem) {
        Tabuleiro t = new Tabuleiro(5);
        assertThrows(IllegalArgumentException.class, () -> t.getPosicaoOrdenada(numOrdem));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void grafo_tem_4_vezes_n_menos_1_vezes_n_menos_2_arestas(int n) {
        // Um tabuleiro NxN tem 4(N-1)(N-2) pares de casas ligadas por um salto de cavalo
        Tabuleiro t = new Tabuleiro(n);
        int somaGraus = t.grafoMovimentos().values().stream().mapToInt(List::size).sum();
        assertEquals(t.totalCasas(), t.grafoMovimentos().size());
        assertEquals(2 * 4 * (n - 1) * (n - 2), somaGraus);
    }

    @Test
    void grafo_e_simetrico() {
        Tabuleiro t = new Tabuleiro(5);
        t.grafoMovimentos().forEach((origem, destinos) ->
                destinos.forEach(destino -> assertTrue(t.grafoMovimentos().get(destino).contains(origem))));
    }

    @Test
    void grafo_e_imutavel() {
        Tabuleiro t = new Tabuleiro(5);
        List<Posicao> movimentos = t.grafoMovimentos().get(new Posicao(0, 0));
        assertThrows(UnsupportedOperationException.class, () -> movimentos.add(new Posicao(4, 4)));
        assertThrows(UnsupportedOperationException.class, () -> t.grafoMovimentos().clear());
    }

    @Test
    void mascara_de_bits_marca_e_consulta_casas() {
        Posicao canto = new Posicao(0, 0);
        Posicao ultima = new Posicao(4, 4);

        long mascara = Tabuleiro.marcarPosicaoVisitada(0L, 5, canto);
        assertTrue(Tabuleiro.isPosicaoVisitada(mascara, 5, canto));
        assertFalse(Tabuleiro.isPosicaoVisitada(mascara, 5, ultima));

        long mascara2 = Tabuleiro.marcarPosicaoVisitada(mascara, 5, ultima);
        assertTrue(Tabuleiro.isPosicaoVisitada(mascara2, 5, ultima));
        assertEquals(1L | (1L << 24), mascara2);
        assertFalse(Tabuleiro.isPosicaoVisitada(mascara, 5, ultima), "A máscara original não muda");
    }

    @Test
    void dimensoes_ate_5_cabem_na_mascara_de_bits() {
        for (int n = 1; n <= 5; n++) {
            Tabuleiro t = new Tabuleiro(n);
            assertDoesNotThrow(t::validarDimensaoBitmask);
        }
    }

    @Test
    void total_soma_o_mapa_de_solucoes() {
        Tabuleiro t = new Tabuleiro(5);
        assertEquals(0, t.totalSolucoes());
        t.mapaPosicaoInicialQuantidadeSolucoes().put(new Posicao(0, 0), 304L);
        t.mapaPosicaoInicialQuantidadeSolucoes().put(new Posicao(2, 2), 64L);
        assertEquals(368, t.totalSolucoes());
    }
}
