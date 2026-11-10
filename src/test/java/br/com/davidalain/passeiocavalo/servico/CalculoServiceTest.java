package br.com.davidalain.passeiocavalo.servico;

import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import br.com.davidalain.passeiocavalo.solucao.ControleExecucao;
import br.com.davidalain.passeiocavalo.solucao.Solucao7;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class CalculoServiceTest {

    private final CalculadoraControlavel calculadora = new CalculadoraControlavel();
    private CalculoService servico = novoServico(Duration.ZERO, 100);

    private CalculoService novoServico(Duration tempoMaximo, int historico) {
        return new CalculoService(calculadora, 2, tempoMaximo, historico);
    }

    @AfterEach
    void encerrar() {
        servico.encerrar();
    }

    private static void aguardarFim(Calculo c) {
        await().atMost(Duration.ofSeconds(10)).until(() -> c.status().finalizado());
    }

    @ParameterizedTest
    @CsvSource({"1,1,1", "4,0,3", "5,1728,6"})
    void calcula_em_segundo_plano(int dimensao, long total, int casas) {
        Calculo c = servico.iniciar(dimensao);
        assertEquals(dimensao, c.dimensao());

        Calculo fim = c.aguardar();
        assertSame(c, fim);
        assertEquals(StatusCalculo.CONCLUIDO, c.status());
        assertEquals(total, c.totalSolucoes());

        CalculoResposta r = c.resposta();
        assertEquals(c.id(), r.id());
        assertEquals(dimensao + "x" + dimensao, r.dimensaoTabuleiro());
        assertEquals(casas, r.casasTotal());
        assertEquals(casas, r.casasCalculadas());
        assertEquals(100.0, r.progressoPercentual());
        assertNotNull(r.finalizadoEm());
        assertNull(r.erro());
        assertSame(c, servico.buscar(c.id()).orElseThrow());
        assertTrue(servico.emAndamento().isEmpty(), "A vaga deve ser liberada no fim");
    }

    @Test
    void progresso_conta_os_passeios_das_casas_buscadas() {
        assertEquals(480, servico.iniciar(5).aguardar().resposta().passeiosEncontrados());
    }

    @Test
    void roda_no_pool_proprio_e_nao_no_pool_comum() {
        Set<String> threads = ConcurrentHashMap.newKeySet();
        CalculoService s = new CalculoService((d, controle) ->
                new Solucao7(new Tabuleiro(d), new ControleExecucao() {
                    @Override
                    public void passeioEncontrado() {
                        threads.add(Thread.currentThread().getName());
                    }
                }).contarTodosOsPasseios(), 3, Duration.ZERO, 10);
        try {
            assertEquals(1728, s.iniciar(5).aguardar().totalSolucoes());
            assertFalse(threads.isEmpty());
            assertTrue(threads.stream().allMatch(n -> n.startsWith("calculo-passeio-")),
                    "Threads usadas: " + threads);
        } finally {
            s.encerrar();
        }
    }

    @Test
    void segundo_calculo_e_recusado_enquanto_o_primeiro_roda() {
        calculadora.bloquear = true;
        Calculo primeiro = servico.iniciar(5);

        CalculoEmAndamentoException e = assertThrows(CalculoEmAndamentoException.class, () -> servico.iniciar(5));
        assertEquals(primeiro.id(), e.idEmAndamento());
        assertEquals(primeiro, servico.emAndamento().orElseThrow());
        assertEquals(StatusCalculo.EM_ANDAMENTO, primeiro.resposta().status());
        assertNull(primeiro.resposta().totalSolucoes());

        servico.cancelar(primeiro.id());
        aguardarFim(primeiro);
    }

    @Test
    void cancelar_para_o_calculo_e_libera_a_vaga() {
        calculadora.bloquear = true;
        Calculo c = servico.iniciar(5);

        assertSame(c, servico.cancelar(c.id()));
        aguardarFim(c);

        assertEquals(StatusCalculo.CANCELADO, c.status());
        assertEquals("Cálculo cancelado.", c.erro());
        assertNull(c.totalSolucoes());

        calculadora.bloquear = false;
        assertEquals(1728, servico.iniciar(5).aguardar().totalSolucoes(), "Depois do cancelamento, a vaga fica livre");
    }

    @Test
    void varios_pedidos_de_cancelamento_mantem_o_motivo_do_primeiro() {
        // Direto na classe Calculo, sem threads: o primeiro motivo vale até o cálculo terminar
        Calculo c = new Calculo(UUID.randomUUID(), 5, 6);
        assertTrue(c.solicitarCancelamento(StatusCalculo.TEMPO_ESGOTADO));
        assertTrue(c.solicitarCancelamento(StatusCalculo.CANCELADO), "Ainda não terminou, então aceita");
        assertTrue(c.cancelado());

        c.interromper();
        assertEquals(StatusCalculo.TEMPO_ESGOTADO, c.status());
        assertFalse(c.solicitarCancelamento(StatusCalculo.CANCELADO), "Depois do fim, recusa");
    }

    @Test
    void fim_do_calculo_so_e_registrado_uma_vez() {
        Calculo c = new Calculo(UUID.randomUUID(), 5, 6);
        c.concluir(1728);
        c.falhar(new IllegalStateException("depois do fim"));
        c.interromper();
        assertEquals(StatusCalculo.CONCLUIDO, c.status());
        assertEquals(1728, c.totalSolucoes());
        assertNull(c.erro());
    }

    @Test
    void cancelar_calculo_terminado_lanca_excecao() {
        Calculo c = servico.iniciar(5).aguardar();
        assertThrows(IllegalStateException.class, () -> servico.cancelar(c.id()));
        assertEquals(StatusCalculo.CONCLUIDO, c.status());
    }

    @Test
    void cancelar_id_inexistente_lanca_excecao() {
        assertThrows(NoSuchElementException.class, () -> servico.cancelar(UUID.randomUUID()));
        assertTrue(servico.buscar(UUID.randomUUID()).isEmpty());
    }

    @Test
    void tempo_maximo_interrompe_o_calculo() {
        servico.encerrar();
        servico = novoServico(Duration.ofMillis(200), 100);
        calculadora.bloquear = true;

        Calculo c = servico.iniciar(5);
        aguardarFim(c);

        assertEquals(StatusCalculo.TEMPO_ESGOTADO, c.status());
        assertTrue(c.erro().contains("tempo máximo"));
        assertTrue(c.tempoExecucaoMs() >= 200);
        assertTrue(servico.emAndamento().isEmpty());
    }

    @Test
    void tempo_maximo_nao_afeta_calculo_rapido() {
        servico.encerrar();
        servico = novoServico(Duration.ofSeconds(30), 100);
        assertEquals(StatusCalculo.CONCLUIDO, servico.iniciar(5).aguardar().status());
    }

    @Test
    void erro_na_busca_marca_falha_e_libera_a_vaga() {
        calculadora.erro = new IllegalStateException("falha simulada");
        Calculo c = servico.iniciar(5).aguardar();

        assertEquals(StatusCalculo.FALHOU, c.status());
        assertTrue(c.erro().contains("falha simulada"));
        assertTrue(servico.emAndamento().isEmpty());

        calculadora.erro = null;
        assertEquals(StatusCalculo.CONCLUIDO, servico.iniciar(5).aguardar().status());
    }

    @Test
    void historico_guarda_so_os_mais_recentes() {
        servico.encerrar();
        servico = novoServico(Duration.ZERO, 2);

        Calculo c1 = servico.iniciar(1).aguardar();
        Calculo c2 = servico.iniciar(1).aguardar();
        Calculo c3 = servico.iniciar(1).aguardar();
        Calculo c4 = servico.iniciar(1).aguardar();

        assertTrue(servico.buscar(c1.id()).isEmpty());
        assertTrue(servico.buscar(c2.id()).isEmpty());
        assertTrue(servico.buscar(c3.id()).isPresent());
        assertTrue(servico.buscar(c4.id()).isPresent());
    }

    @Test
    void desligar_o_servidor_cancela_o_calculo_em_andamento() {
        calculadora.bloquear = true;
        Calculo c = servico.iniciar(5);
        servico.encerrar();
        aguardarFim(c);
        assertEquals(StatusCalculo.CANCELADO, c.status());
    }

    @Test
    void depois_de_desligado_recusa_novos_calculos() {
        servico.encerrar();
        assertThrows(java.util.concurrent.RejectedExecutionException.class, () -> servico.iniciar(5));
        assertTrue(servico.emAndamento().isEmpty());
    }

    @Test
    void progresso_parcial_enquanto_roda() {
        calculadora.bloquear = true;
        Calculo c = servico.iniciar(5);
        CalculoResposta r = c.resposta();

        assertEquals(StatusCalculo.EM_ANDAMENTO, r.status());
        assertEquals(0, r.casasCalculadas());
        assertEquals(6, r.casasTotal());
        assertEquals(0.0, r.progressoPercentual());
        assertNull(r.finalizadoEm());

        c.casaCalculada(null, 0);
        c.casaCalculada(null, 0);
        assertEquals(33.3, c.resposta().progressoPercentual());

        servico.cancelar(c.id());
        aguardarFim(c);
    }
}
