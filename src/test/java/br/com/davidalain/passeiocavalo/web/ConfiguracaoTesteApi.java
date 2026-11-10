package br.com.davidalain.passeiocavalo.web;

import br.com.davidalain.passeiocavalo.servico.CalculadoraControlavel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Troca a calculadora real pela controlável, que consegue simular cálculos longos sem tabuleiros grandes. */
@TestConfiguration
class ConfiguracaoTesteApi {

    @Bean
    @Primary
    CalculadoraControlavel calculadoraControlavel() {
        return new CalculadoraControlavel();
    }
}
