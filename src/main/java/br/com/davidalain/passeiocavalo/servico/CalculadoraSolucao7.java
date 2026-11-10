package br.com.davidalain.passeiocavalo.servico;

import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import br.com.davidalain.passeiocavalo.solucao.ControleExecucao;
import br.com.davidalain.passeiocavalo.solucao.Solucao7;
import org.springframework.stereotype.Component;

/** Calculadora usada pela API: a {@link Solucao7}. */
@Component
public class CalculadoraSolucao7 implements CalculadoraPasseios {

    @Override
    public long calcular(int dimensao, ControleExecucao controle) {
        return new Solucao7(new Tabuleiro(dimensao), controle).contarTodosOsPasseios();
    }
}
