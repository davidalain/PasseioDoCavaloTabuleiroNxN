package br.com.davidalain.passeiocavalo.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testes do /health e da remoção do cálculo síncrono. */
class PasseioControllerTest extends TesteApiBase {

    @Test
    void health_responde_up_sem_token() throws Exception {
        mvc.perform(get("/api/passeiocavalo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void endpoint_sincrono_calcular_nao_existe_mais() throws Exception {
        mvc.perform(post("/api/passeiocavalo/calcular")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dimensao\": 5}"))
                .andExpect(status().isNotFound());
    }
}
