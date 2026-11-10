package br.com.davidalain.passeiocavalo.web;

import br.com.davidalain.passeiocavalo.servico.CalculadoraControlavel;
import br.com.davidalain.passeiocavalo.servico.CalculoService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

/**
 * Base dos testes da API. Os limites são 5 aqui, para os testes nunca calcularem tabuleiros maiores.
 * Todas as classes de teste usam as mesmas propriedades, então compartilham o mesmo contexto Spring.
 */
@SpringBootTest(properties = {
        "API_TOKEN=token-teste",
        "passeio.dimensao-maxima=5",
        "passeio.dimensao-maxima-sincrona=5",
        "passeio.paralelismo=2"})
@AutoConfigureMockMvc
@Import(ConfiguracaoTesteApi.class)
abstract class TesteApiBase {

    static final String TOKEN = "Bearer token-teste";

    @Autowired
    MockMvc mvc;

    @Autowired
    CalculoService servico;

    @Autowired
    CalculadoraControlavel calculadora;

    /** Garante que nenhum teste deixa um cálculo rodando para o próximo. */
    @AfterEach
    void liberarVaga() {
        servico.emAndamento().ifPresent(c -> {
            try {
                servico.cancelar(c.id());
            } catch (IllegalStateException terminouNesteInstante) {
                // nada a fazer
            }
        });
        await().atMost(Duration.ofSeconds(10)).until(() -> servico.emAndamento().isEmpty());
        calculadora.reiniciar();
    }
}
