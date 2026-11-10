package br.com.davidalain.passeiocavalo.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

/** Verifica o token de acesso das requisições (variável de ambiente {@code API_TOKEN}). */
@Component
public class AutenticacaoToken {

    private static final Logger log = LoggerFactory.getLogger(AutenticacaoToken.class);
    private static final String PREFIXO_BEARER = "Bearer ";

    private final String tokenServidor;

    public AutenticacaoToken(@Value("${API_TOKEN:}") String tokenServidor) {
        this.tokenServidor = tokenServidor;
        if (tokenServidor.isBlank()) {
            log.warn("A variável API_TOKEN não foi definida: os endpoints de cálculo vão recusar todas as requisições.");
        }
    }

    /**
     * @param authorization valor do header {@code Authorization} (tem prioridade)
     * @param tokenCorpo    token enviado no corpo (compatibilidade com a v1.0.0), ou {@code null}
     * @return a resposta de erro, se a requisição não está autorizada; vazio se está
     */
    public Optional<ResponseEntity<Map<String, Object>>> verificar(String authorization, String tokenCorpo) {
        if (tokenServidor.isBlank()) {
            return Optional.of(Respostas.erro(HttpStatus.SERVICE_UNAVAILABLE,
                    "Servidor sem token configurado (variável API_TOKEN)."));
        }
        if (!tokenValido(extrairToken(authorization, tokenCorpo))) {
            return Optional.of(Respostas.erro(HttpStatus.UNAUTHORIZED, "Acesso negado. Token inválido ou ausente."));
        }
        return Optional.empty();
    }

    private static String extrairToken(String authorization, String tokenCorpo) {
        if (authorization != null && authorization.startsWith(PREFIXO_BEARER)) {
            return authorization.substring(PREFIXO_BEARER.length()).trim();
        }
        return tokenCorpo;
    }

    // Comparação em tempo constante, para não vazar o token pelo tempo de resposta
    private boolean tokenValido(String token) {
        if (token == null) return false;
        return MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                tokenServidor.getBytes(StandardCharsets.UTF_8));
    }
}
