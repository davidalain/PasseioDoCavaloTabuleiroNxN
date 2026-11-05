package br.com.davidalain.passeiocavalo.web;

import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import br.com.davidalain.passeiocavalo.solucao.Solucao7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

@RestController
@RequestMapping("/api/passeiocavalo")
public class PasseioController {

    private static final Logger log = LoggerFactory.getLogger(PasseioController.class);
    private static final String PREFIXO_BEARER = "Bearer ";

    private final String tokenServidor;
    private final int dimensaoMaxima;

    /** Só um cálculo por vez: cada um já usa todos os núcleos da máquina. */
    final Semaphore calculoEmAndamento = new Semaphore(1);

    public PasseioController(
            @Value("${API_TOKEN:}") String tokenServidor,
            @Value("${passeio.dimensao-maxima:6}") int dimensaoMaxima) {
        this.tokenServidor = tokenServidor;
        this.dimensaoMaxima = Math.min(dimensaoMaxima, Tabuleiro.DIMENSAO_MAXIMA_BITMASK);

        if (tokenServidor.isBlank()) {
            log.warn("A variável API_TOKEN não foi definida: o endpoint /calcular vai recusar todas as requisições.");
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @PostMapping("/calcular")
    public ResponseEntity<Map<String, Object>> calcularPasseio(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) CavaloRequest request) {

        if (tokenServidor.isBlank()) {
            return erro(HttpStatus.SERVICE_UNAVAILABLE, "Servidor sem token configurado (variável API_TOKEN).");
        }

        if (!tokenValido(extrairToken(authorization, request))) {
            return erro(HttpStatus.UNAUTHORIZED, "Acesso negado. Token inválido ou ausente.");
        }

        if (request == null || request.dimensao() == null) {
            return erro(HttpStatus.BAD_REQUEST, "O campo 'dimensao' é obrigatório.");
        }

        final int dimensao = request.dimensao();
        if (dimensao < 1 || dimensao > dimensaoMaxima) {
            return erro(HttpStatus.BAD_REQUEST, "Dimensão inválida: use um valor entre 1 e " + dimensaoMaxima + ".");
        }

        if (!calculoEmAndamento.tryAcquire()) {
            return erro(HttpStatus.TOO_MANY_REQUESTS, "Já existe um cálculo em andamento. Tente novamente mais tarde.");
        }

        try {
            final long inicio = System.nanoTime();
            final long totalSolucoes = new Solucao7(new Tabuleiro(dimensao)).contarTodosOsPasseios();
            final long tempoMs = (System.nanoTime() - inicio) / 1_000_000;

            final Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("status", "Sucesso");
            corpo.put("totalSolucoes", totalSolucoes);
            corpo.put("dimensaoTabuleiro", dimensao + "x" + dimensao);
            corpo.put("tempoExecucaoMs", tempoMs);
            return ResponseEntity.ok(corpo);
        } finally {
            calculoEmAndamento.release();
        }
    }

    // O header Authorization tem prioridade; o campo "token" do corpo é aceito por compatibilidade
    private static String extrairToken(String authorization, CavaloRequest request) {
        if (authorization != null && authorization.startsWith(PREFIXO_BEARER)) {
            return authorization.substring(PREFIXO_BEARER.length()).trim();
        }
        return request != null ? request.token() : null;
    }

    // Comparação em tempo constante, para não vazar o token pelo tempo de resposta
    private boolean tokenValido(String token) {
        if (token == null) return false;
        return MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                tokenServidor.getBytes(StandardCharsets.UTF_8));
    }

    private static ResponseEntity<Map<String, Object>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }
}
