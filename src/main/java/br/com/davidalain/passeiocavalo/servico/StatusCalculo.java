package br.com.davidalain.passeiocavalo.servico;

/** Situação de um cálculo assíncrono. */
public enum StatusCalculo {
    /** A busca está rodando. */
    EM_ANDAMENTO,
    /** Terminou; o total de passeios está disponível. */
    CONCLUIDO,
    /** Foi cancelado a pedido (DELETE) ou porque o servidor está desligando. */
    CANCELADO,
    /** Foi interrompido por passar do tempo máximo configurado. */
    TEMPO_ESGOTADO,
    /** Terminou com erro inesperado. */
    FALHOU;

    public boolean finalizado() {
        return this != EM_ANDAMENTO;
    }
}
