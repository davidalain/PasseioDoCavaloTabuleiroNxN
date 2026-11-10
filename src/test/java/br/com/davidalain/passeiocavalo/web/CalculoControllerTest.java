package br.com.davidalain.passeiocavalo.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Testes do cálculo assíncrono (POST/GET/DELETE /api/passeiocavalo/calculos). */
class CalculoControllerTest extends TesteApiBase {

    private static final String URL = "/api/passeiocavalo/calculos";

    @Autowired
    private ObjectMapper json;

    private ResultActions iniciar(String corpo) throws Exception {
        return mvc.perform(post(URL).header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private String iniciarERetornarId(int dimensao) throws Exception {
        MvcResult r = iniciar("{\"dimensao\": " + dimensao + "}").andExpect(status().isAccepted()).andReturn();
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("id").asText();
    }

    private JsonNode consultar(String id) throws Exception {
        MvcResult r = mvc.perform(get(URL + "/" + id).header("Authorization", TOKEN))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode aguardarStatus(String id, String status) {
        JsonNode[] ultimo = new JsonNode[1];
        await().atMost(Duration.ofSeconds(10)).until(() -> {
            ultimo[0] = consultar(id);
            return status.equals(ultimo[0].get("status").asText());
        });
        return ultimo[0];
    }

    // ---------------- Fluxo principal ----------------

    @Test
    void post_retorna_202_com_location_e_estado_inicial() throws Exception {
        calculadora.bloquear = true;
        iniciar("{\"dimensao\": 5}")
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", matchesPattern(".*/api/passeiocavalo/calculos/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.dimensaoTabuleiro").value("5x5"))
                .andExpect(jsonPath("$.casasTotal").value(6))
                .andExpect(jsonPath("$.casasCalculadas").value(0))
                .andExpect(jsonPath("$.progressoPercentual").value(0.0))
                .andExpect(jsonPath("$.iniciadoEm").isString())
                .andExpect(jsonPath("$.totalSolucoes").doesNotExist())
                .andExpect(jsonPath("$.finalizadoEm").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,0", "3,0", "4,0", "5,1728"})
    void get_mostra_o_resultado_quando_termina(int dimensao, long total) throws Exception {
        String id = iniciarERetornarId(dimensao);
        JsonNode fim = aguardarStatus(id, "CONCLUIDO");

        assertEquals(id, fim.get("id").asText());
        assertEquals(total, fim.get("totalSolucoes").asLong());
        assertEquals(dimensao + "x" + dimensao, fim.get("dimensaoTabuleiro").asText());
        assertEquals(100.0, fim.get("progressoPercentual").asDouble());
        assertEquals(fim.get("casasTotal").asInt(), fim.get("casasCalculadas").asInt());
        assertEquals(true, fim.has("finalizadoEm"));
        assertEquals(false, fim.has("erro"));
    }

    @Test
    void location_aponta_para_a_consulta() throws Exception {
        String location = iniciar("{\"dimensao\": 5}").andReturn().getResponse().getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);
        assertEquals(1728, aguardarStatus(id, "CONCLUIDO").get("totalSolucoes").asLong());
    }

    @Test
    void token_no_corpo_tambem_inicia() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dimensao\": 5, \"token\": \"token-teste\"}"))
                .andExpect(status().isAccepted());
    }

    // ---------------- Cancelamento ----------------

    @Test
    void delete_cancela_o_calculo_em_andamento() throws Exception {
        calculadora.bloquear = true;
        String id = iniciarERetornarId(5);

        mvc.perform(delete(URL + "/" + id).header("Authorization", TOKEN))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(id));

        JsonNode fim = aguardarStatus(id, "CANCELADO");
        assertEquals("Cálculo cancelado.", fim.get("erro").asText());
        assertEquals(false, fim.has("totalSolucoes"));

        // A vaga foi liberada: um novo cálculo começa
        calculadora.bloquear = false;
        iniciar("{\"dimensao\": 5}").andExpect(status().isAccepted());
    }

    @Test
    void delete_de_calculo_terminado_retorna_409() throws Exception {
        String id = iniciarERetornarId(5);
        aguardarStatus(id, "CONCLUIDO");

        mvc.perform(delete(URL + "/" + id).header("Authorization", TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(containsString("CONCLUIDO")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000-0000-0000-0000-000000000000", "nao-e-um-uuid"})
    void delete_de_id_inexistente_retorna_404(String id) throws Exception {
        mvc.perform(delete(URL + "/" + id).header("Authorization", TOKEN))
                .andExpect(status().isNotFound());
    }

    // ---------------- Um cálculo por vez ----------------

    @Test
    void segundo_post_retorna_429_com_o_id_do_que_esta_rodando() throws Exception {
        calculadora.bloquear = true;
        String id = iniciarERetornarId(5);

        iniciar("{\"dimensao\": 3}")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.calculoEmAndamento").value(id));
    }

    @Test
    void sem_corpo_e_sem_token_retorna_401() throws Exception {
        mvc.perform(post(URL)).andExpect(status().isUnauthorized());
    }

    // ---------------- Consulta ----------------

    @ParameterizedTest
    @ValueSource(strings = {"00000000-0000-0000-0000-000000000000", "nao-e-um-uuid"})
    void get_de_id_inexistente_retorna_404(String id) throws Exception {
        mvc.perform(get(URL + "/" + id).header("Authorization", TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(containsString("não encontrado")));
    }

    @Test
    void falha_na_busca_aparece_como_falhou() throws Exception {
        calculadora.erro = new IllegalStateException("falha simulada");
        String id = iniciarERetornarId(5);
        JsonNode fim = aguardarStatus(id, "FALHOU");
        assertEquals(true, fim.get("erro").asText().contains("falha simulada"));
    }

    @Test
    void calculo_roda_fora_das_threads_http() throws Exception {
        String id = iniciarERetornarId(5);
        aguardarStatus(id, "CONCLUIDO");
        assertEquals(true, calculadora.threadDoCalculo.startsWith("calculo-passeio-"),
                "Thread: " + calculadora.threadDoCalculo);
    }

    // ---------------- Validação e autenticação ----------------

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"dimensao\": 0}", "{\"dimensao\": -1}", "{\"dimensao\": 6}"})
    void dimensao_invalida_retorna_400(String corpo) throws Exception {
        // 6 é recusado pelo limite (5) antes de qualquer cálculo
        iniciar(corpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").isString());
    }

    @Test
    void json_invalido_retorna_400() throws Exception {
        iniciar("{dimensao: ").andExpect(status().isBadRequest());
    }

    @Test
    void sem_token_retorna_401_em_todos_os_endpoints() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"dimensao\": 5}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(URL + "/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(delete(URL + "/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void token_errado_retorna_401_em_todos_os_endpoints() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(post(URL).header("Authorization", "Bearer errado")
                .contentType(MediaType.APPLICATION_JSON).content("{\"dimensao\": 5}")).andExpect(status().isUnauthorized());
        mvc.perform(get(URL + "/" + id).header("Authorization", "Bearer errado")).andExpect(status().isUnauthorized());
        mvc.perform(delete(URL + "/" + id).header("Authorization", "Bearer errado")).andExpect(status().isUnauthorized());
    }
}
