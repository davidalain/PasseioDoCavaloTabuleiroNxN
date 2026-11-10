package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;

/**
 * Canal entre uma busca longa e quem a acompanha: permite cancelar a busca e receber o progresso.
 * <p>
 * Os métodos são chamados de várias threads ao mesmo tempo e devem ser rápidos:
 * {@link #cancelado()} é consultado a cada passo da busca.
 */
public interface ControleExecucao {

    /** Controle que nunca cancela e ignora o progresso. */
    ControleExecucao NENHUM = new ControleExecucao() {
    };

    /** Quando retorna {@code true}, a busca para lançando {@link java.util.concurrent.CancellationException}. */
    default boolean cancelado() {
        return false;
    }

    /** Chamado a cada passeio completo encontrado. */
    default void passeioEncontrado() {
    }

    /** Chamado quando termina a busca de uma casa inicial. */
    default void casaCalculada(Posicao casa, long quantidade) {
    }
}
