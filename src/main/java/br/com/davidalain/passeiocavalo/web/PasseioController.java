package br.com.davidalain.passeiocavalo.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Verificação de saúde do servidor.
 * <p>
 * O cálculo é feito só pelos endpoints assíncronos de {@link CalculoController}. O antigo
 * {@code POST /calcular} (síncrono) foi removido: ele mantinha a conexão HTTP aberta durante todo o cálculo,
 * e cálculos longos eram cortados pelos timeouts de proxies e clientes.
 */
@RestController
@RequestMapping("/api/passeiocavalo")
public class PasseioController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
