package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/** Progresso e cancelamento da Solução 7 (tabuleiros até 5x5). */
class Solucao7ControleExecucaoTest {

    @Test
    void casas_representantes_do_5x5() {
        assertEquals(
                List.of(new Posicao(0, 0), new Posicao(0, 1), new Posicao(0, 2),
                        new Posicao(1, 1), new Posicao(1, 2), new Posicao(2, 2)),
                new Solucao7(new Tabuleiro(5)).casasRepresentantes());
    }

    @Test
    void informa_o_progresso_de_cada_casa_e_cada_passeio() {
        AtomicLong passeios = new AtomicLong();
        Map<Posicao, Long> casas = new ConcurrentHashMap<>();
        ControleExecucao controle = new ControleExecucao() {
            @Override
            public void passeioEncontrado() {
                passeios.incrementAndGet();
            }

            @Override
            public void casaCalculada(Posicao casa, long quantidade) {
                assertNull(casas.put(casa, quantidade), "Cada casa é informada uma vez só");
            }
        };

        assertEquals(1728, new Solucao7(new Tabuleiro(5), controle).contarTodosOsPasseios());

        // Só os representantes são buscados: 304 + 0 + 56 + 56 + 0 + 64
        assertEquals(480, passeios.get());
        assertEquals(Map.of(
                new Posicao(0, 0), 304L, new Posicao(0, 1), 0L, new Posicao(0, 2), 56L,
                new Posicao(1, 1), 56L, new Posicao(1, 2), 0L, new Posicao(2, 2), 64L), casas);
    }

    @Test
    void cancelado_desde_o_inicio_nao_busca_nada() {
        AtomicLong passeios = new AtomicLong();
        ControleExecucao cancelado = new ControleExecucao() {
            @Override
            public boolean cancelado() {
                return true;
            }

            @Override
            public void passeioEncontrado() {
                passeios.incrementAndGet();
            }
        };

        assertThrows(CancellationException.class,
                () -> new Solucao7(new Tabuleiro(5), cancelado).contarTodosOsPasseios());
        assertEquals(0, passeios.get());
    }

    @Test
    void cancelamento_no_meio_para_a_busca() {
        AtomicBoolean cancelar = new AtomicBoolean();
        AtomicLong passeios = new AtomicLong();
        ControleExecucao controle = new ControleExecucao() {
            @Override
            public boolean cancelado() {
                return cancelar.get();
            }

            @Override
            public void passeioEncontrado() {
                if (passeios.incrementAndGet() == 10) cancelar.set(true); // cancela depois do 10º passeio
            }
        };

        Tabuleiro t = new Tabuleiro(5);
        assertThrows(CancellationException.class, () -> new Solucao7(t, controle).contarTodosOsPasseios());
        assertTrue(passeios.get() < 480, "A busca deve parar antes do fim, mas achou " + passeios.get());
        assertTrue(t.mapaPosicaoInicialQuantidadeSolucoes().size() < 25);
    }

    @Test
    void sem_controle_continua_funcionando() {
        assertEquals(1728, new Solucao7(new Tabuleiro(5)).contarTodosOsPasseios());
        assertSame(ControleExecucao.NENHUM, new Solucao7(new Tabuleiro(5)).controle());
    }
}
