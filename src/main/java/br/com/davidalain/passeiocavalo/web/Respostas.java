package br.com.davidalain.passeiocavalo.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Respostas e validações comuns aos controllers. */
final class Respostas {

    private Respostas() {
    }

    static ResponseEntity<Map<String, Object>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }

    static ResponseEntity<Map<String, Object>> erro(HttpStatus status, String mensagem, String campo, Object valor) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("erro", mensagem);
        if (valor != null) corpo.put(campo, valor);
        return ResponseEntity.status(status).body(corpo);
    }

    /** Erro 400 se {@code dimensao} está ausente ou fora de 1..{@code maxima}; vazio se é válida. */
    static Optional<ResponseEntity<Map<String, Object>>> validarDimensao(Integer dimensao, int maxima) {
        if (dimensao == null) {
            return Optional.of(erro(HttpStatus.BAD_REQUEST, "O campo 'dimensao' é obrigatório."));
        }
        if (dimensao < 1 || dimensao > maxima) {
            return Optional.of(erro(HttpStatus.BAD_REQUEST,
                    "Dimensão inválida: use um valor entre 1 e " + maxima + "."));
        }
        return Optional.empty();
    }
}
