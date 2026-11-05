package br.com.davidalain.passeiocavalo.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes da API. O limite de dimensão é 5 aqui, para os testes nunca calcularem tabuleiros maiores.
 */
@SpringBootTest(properties = {"API_TOKEN=token-teste", "passeio.dimensao-maxima=5"})
@AutoConfigureMockMvc
class PasseioControllerTest {

    private static final String URL = "/api/passeiocavalo/calcular";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasseioController controller;

    @Test
    void health_responde_up() throws Exception {
        mvc.perform(get("/api/passeiocavalo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,0", "3,0", "4,0", "5,1728"})
    void calcula_com_token_no_header(int dimensao, long total) throws Exception {
        mvc.perform(post(URL)
                        .header("Authorization", "Bearer token-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dimensao\": " + dimensao + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Sucesso"))
                .andExpect(jsonPath("$.totalSolucoes").value(total))
                .andExpect(jsonPath("$.dimensaoTabuleiro").value(dimensao + "x" + dimensao))
                .andExpect(jsonPath("$.tempoExecucaoMs").isNumber());
    }

    @Test
    void calcula_com_token_no_corpo_por_compatibilidade() throws Exception {
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dimensao\": 5, \"token\": \"token-teste\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSolucoes").value(1728));
    }

    @Test
    void sem_token_retorna_401() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"dimensao\": 5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value(containsString("Token")));
    }

    @Test
    void sem_corpo_e_sem_token_retorna_401() throws Exception {
        mvc.perform(post(URL)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer errado", "Bearer token-test", "Bearer token-teste-extra", "token-teste"})
    void token_errado_retorna_401(String authorization) throws Exception {
        mvc.perform(post(URL)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dimensao\": 5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sem_dimensao_retorna_400() throws Exception {
        mvc.perform(post(URL)
                        .header("Authorization", "Bearer token-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("dimensao")));
    }

    @Test
    void token_no_corpo_sem_dimensao_retorna_400() throws Exception {
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"token-teste\"}"))
                .andExpect(status().isBadRequest());
    }

    // 6 é recusado pelo limite (5) antes de qualquer cálculo
    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6})
    void dimensao_fora_do_limite_retorna_400(int dimensao) throws Exception {
        mvc.perform(post(URL)
                        .header("Authorization", "Bearer token-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dimensao\": " + dimensao + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("entre 1 e 5")));
    }

    @Test
    void json_invalido_retorna_400() throws Exception {
        mvc.perform(post(URL)
                        .header("Authorization", "Bearer token-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{dimensao: "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void segundo_calculo_simultaneo_retorna_429() throws Exception {
        // Simula um cálculo em andamento ocupando a única vaga
        controller.calculoEmAndamento.acquire();
        try {
            mvc.perform(post(URL)
                            .header("Authorization", "Bearer token-teste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"dimensao\": 5}"))
                    .andExpect(status().isTooManyRequests());
        } finally {
            controller.calculoEmAndamento.release();
        }
        assertEquals(1, controller.calculoEmAndamento.availablePermits(), "A vaga deve ser liberada");
    }

    @Test
    void servidor_sem_token_configurado_retorna_503() {
        PasseioController semToken = new PasseioController("", 5);
        var resposta = semToken.calcularPasseio("Bearer ", new CavaloRequest(5, ""));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resposta.getStatusCode());
    }

    @Test
    void limite_configurado_acima_de_8_e_reduzido_para_8() {
        // Só verifica a mensagem de erro: nenhuma busca é executada
        PasseioController controller = new PasseioController("t", 50);
        var resposta = controller.calcularPasseio("Bearer t", new CavaloRequest(0, null));
        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("Dimensão inválida: use um valor entre 1 e 8.", resposta.getBody().get("erro"));
    }
}
