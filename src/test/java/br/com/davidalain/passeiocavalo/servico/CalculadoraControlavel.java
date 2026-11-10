package br.com.davidalain.passeiocavalo.servico;

import br.com.davidalain.passeiocavalo.solucao.ControleExecucao;

import java.util.concurrent.CancellationException;

/**
 * Calculadora para testes. Por padrão usa a busca real (só chamada com N ≤ 5).
 * Com {@link #bloquear} ligado, simula um cálculo muito longo: fica rodando até ser cancelada,
 * sem usar CPU nem tabuleiros grandes.
 */
public class CalculadoraControlavel implements CalculadoraPasseios {

    private final CalculadoraPasseios real = new CalculadoraSolucao7();

    public volatile boolean bloquear;
    public volatile RuntimeException erro;
    public volatile String threadDoCalculo;

    @Override
    public long calcular(int dimensao, ControleExecucao controle) {
        threadDoCalculo = Thread.currentThread().getName();
        if (erro != null) throw erro;
        if (!bloquear) return real.calcular(dimensao, controle);

        while (!controle.cancelado()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Interrompido");
            }
        }
        throw new CancellationException("Cálculo cancelado");
    }

    public void reiniciar() {
        bloquear = false;
        erro = null;
        threadDoCalculo = null;
    }
}
