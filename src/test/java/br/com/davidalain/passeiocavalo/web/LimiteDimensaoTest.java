package br.com.davidalain.passeiocavalo.web;

import br.com.davidalain.passeiocavalo.servico.CalculadoraControlavel;
import br.com.davidalain.passeiocavalo.servico.CalculoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Limites de dimensão e configuração do token, montando o controller à mão.
 * <p>
 * Nenhuma busca roda com N > 5: a calculadora de teste fica bloqueada até ser cancelada,
 * então o 7x7 só é aceito e colocado em andamento.
 */
class LimiteDimensaoTest {

    private final CalculadoraControlavel calculadora = new CalculadoraControlavel();
    private final CalculoService servico = new CalculoService(calculadora, 1, Duration.ZERO, 10);

    private MockMvc mvc(String token, int dimensaoMaxima) {
        return MockMvcBuilders.standaloneSetup(
                new CalculoController(servico, new AutenticacaoToken(token), dimensaoMaxima)).build();
    }

    private static org.springframework.test.web.servlet.RequestBuilder iniciar(int dimensao) {
        return post("/api/passeiocavalo/calculos").header("Authorization", "Bearer t")
                .contentType(MediaType.APPLICATION_JSON).content("{\"dimensao\": " + dimensao + "}");
    }

    @AfterEach
    void encerrar() {
        servico.encerrar();
    }

    @Test
    void application_properties_usa_7_como_padrao() throws Exception {
        Properties p = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            p.load(in);
        }
        assertEquals("${PASSEIO_DIMENSAO_MAXIMA:7}", p.getProperty("passeio.dimensao-maxima"));
    }

    @Test
    void aceita_7x7_com_o_limite_padrao() throws Exception {
        calculadora.bloquear = true;

        mvc("t", 7).perform(iniciar(7))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.dimensaoTabuleiro").value("7x7"))
                .andExpect(jsonPath("$.casasTotal").value(10)); // 10 espelhos originais no 7x7

        servico.cancelar(servico.emAndamento().orElseThrow().id());
        await().atMost(Duration.ofSeconds(10)).until(() -> servico.emAndamento().isEmpty());
    }

    @Test
    void recusa_8x8_com_o_limite_padrao() throws Exception {
        mvc("t", 7).perform(iniciar(8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Dimensão inválida: use um valor entre 1 e 7."));
    }

    @Test
    void limite_configurado_acima_de_8_e_reduzido_para_8() throws Exception {
        // 9 é recusado antes de qualquer cálculo
        mvc("t", 50).perform(iniciar(9))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("entre 1 e 8")));
    }

    @Test
    void servidor_sem_token_configurado_retorna_503() throws Exception {
        mvc("", 7).perform(iniciar(5))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value(containsString("API_TOKEN")));
    }
}
