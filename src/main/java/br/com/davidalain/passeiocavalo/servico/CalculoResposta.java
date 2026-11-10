package br.com.davidalain.passeiocavalo.servico;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Situação de um cálculo, no formato devolvido pela API.
 *
 * @param casasCalculadas      casas iniciais já buscadas (só os espelhos originais exigem busca)
 * @param casasTotal           total de casas que exigem busca
 * @param progressoPercentual  casasCalculadas / casasTotal, de 0 a 100
 * @param passeiosEncontrados  passeios achados até agora nas casas buscadas. É uma medida de andamento:
 *                             o total final é maior, porque inclui as casas simétricas
 * @param totalSolucoes        total de passeios do tabuleiro; só aparece quando o status é CONCLUIDO
 * @param tempoExecucaoMs      tempo desde o início (ou duração total, se já terminou)
 * @param erro                 motivo, quando o status é CANCELADO, TEMPO_ESGOTADO ou FALHOU
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalculoResposta(
        UUID id,
        StatusCalculo status,
        String dimensaoTabuleiro,
        int casasCalculadas,
        int casasTotal,
        double progressoPercentual,
        long passeiosEncontrados,
        Long totalSolucoes,
        long tempoExecucaoMs,
        Instant iniciadoEm,
        Instant finalizadoEm,
        String erro
) {
}
