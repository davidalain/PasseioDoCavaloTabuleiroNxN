package br.com.davidalain.passeiocavalo.servico;

import java.util.UUID;

/** Lançada ao tentar iniciar um cálculo enquanto outro está rodando. */
public class CalculoEmAndamentoException extends RuntimeException {

    private final UUID idEmAndamento;

    public CalculoEmAndamentoException(UUID idEmAndamento) {
        super("Já existe um cálculo em andamento: " + idEmAndamento);
        this.idEmAndamento = idEmAndamento;
    }

    /** Id do cálculo que está rodando (pode ser {@code null} se ele terminou neste instante). */
    public UUID idEmAndamento() {
        return idEmAndamento;
    }
}
