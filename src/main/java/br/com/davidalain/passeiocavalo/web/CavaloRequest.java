package br.com.davidalain.passeiocavalo.web;

/**
 * Corpo da requisição de cálculo.
 *
 * @param dimensao lado do tabuleiro (N), obrigatório
 * @param token    token de acesso. Prefira o header {@code Authorization: Bearer <token>};
 *                 o campo no corpo continua aceito por compatibilidade
 */
public record CavaloRequest(
        Integer dimensao,
        String token
) {
}
