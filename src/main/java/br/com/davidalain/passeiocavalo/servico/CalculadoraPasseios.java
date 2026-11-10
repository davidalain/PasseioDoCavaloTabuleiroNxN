package br.com.davidalain.passeiocavalo.servico;

import br.com.davidalain.passeiocavalo.solucao.ControleExecucao;

/**
 * Faz a contagem de passeios de um tabuleiro NxN. Separada do {@link CalculoService} para os testes
 * poderem trocar a busca real por uma que demora o quanto for preciso, sem usar tabuleiros grandes.
 */
@FunctionalInterface
public interface CalculadoraPasseios {

    /**
     * @return o total de passeios do tabuleiro
     * @throws java.util.concurrent.CancellationException se {@code controle} pedir o cancelamento
     */
    long calcular(int dimensao, ControleExecucao controle);
}
