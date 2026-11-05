package br.com.davidalain.passeiocavalo;

import br.com.davidalain.passeiocavalo.model.Posicao;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valores conhecidos usados pelos testes. Os testes rodam só até N=5 para serem rápidos.
 */
public final class ValoresEsperados {

    public static final int DIMENSAO_MAXIMA_TESTES = 5;

    /** Total de passeios por dimensão (OEIS A165134). */
    public static final Map<Integer, Long> TOTAL_POR_DIMENSAO = Map.of(
            1, 1L,
            2, 0L,
            3, 0L,
            4, 0L,
            5, 1728L
    );

    /** Passeios por casa inicial no 5x5 (soma = 1728). */
    public static final long[][] PASSEIOS_POR_CASA_5X5 = {
            {304, 0, 56, 0, 304},
            {0, 56, 0, 56, 0},
            {56, 0, 64, 0, 56},
            {0, 56, 0, 56, 0},
            {304, 0, 56, 0, 304},
    };

    private ValoresEsperados() {
    }

    /** Passeios esperados a partir de {@code inicio} em um tabuleiro de lado {@code n} (n de 1 a 5). */
    public static long passeiosPorCasa(int n, Posicao inicio) {
        if (n == 1) return 1;
        if (n == 5) return PASSEIOS_POR_CASA_5X5[inicio.linha()][inicio.coluna()];
        return 0; // 2x2, 3x3 e 4x4 não têm passeio
    }

    /** Verifica que {@code caminho} é um passeio válido: começa em {@code inicio}, visita as N² casas uma vez e só faz saltos de cavalo. */
    public static void assertPasseioValido(int n, Posicao inicio, List<Posicao> caminho) {
        assertNotNull(caminho, "Esperava um passeio a partir de " + inicio);
        assertEquals(n * n, caminho.size(), "O passeio deve visitar todas as casas");
        assertEquals(inicio, caminho.get(0), "O passeio deve começar na casa inicial");
        assertEquals(n * n, caminho.stream().distinct().count(), "Nenhuma casa pode se repetir");

        for (int i = 1; i < caminho.size(); i++) {
            Posicao a = caminho.get(i - 1);
            Posicao b = caminho.get(i);
            assertTrue(Posicao.posicaoValida(b.linha(), b.coluna(), n), "Casa fora do tabuleiro: " + b);
            int dl = Math.abs(a.linha() - b.linha());
            int dc = Math.abs(a.coluna() - b.coluna());
            assertTrue((dl == 1 && dc == 2) || (dl == 2 && dc == 1), "Salto inválido de " + a + " para " + b);
        }
    }
}
