package br.com.davidalain.passeiocavalo.web;

import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import br.com.davidalain.passeiocavalo.servico.Calculo;
import br.com.davidalain.passeiocavalo.servico.CalculoEmAndamentoException;
import br.com.davidalain.passeiocavalo.servico.CalculoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Cálculo assíncrono: a requisição só inicia o cálculo e retorna na hora; o andamento e o resultado são
 * consultados depois pelo id. Assim nenhuma conexão HTTP fica aberta durante horas, sujeita aos timeouts de
 * proxies, balanceadores e clientes.
 * <pre>
 *   POST   /api/passeiocavalo/calculos        -> 202 + Location: .../calculos/{id}
 *   GET    /api/passeiocavalo/calculos/{id}   -> 200 com status, progresso e (no fim) o total
 *   DELETE /api/passeiocavalo/calculos/{id}   -> 202, pede o cancelamento
 * </pre>
 */
@RestController
@RequestMapping("/api/passeiocavalo/calculos")
public class CalculoController {

    private final CalculoService servico;
    private final AutenticacaoToken autenticacao;
    private final int dimensaoMaxima;

    public CalculoController(
            CalculoService servico,
            AutenticacaoToken autenticacao,
            @Value("${passeio.dimensao-maxima:7}") int dimensaoMaxima) {
        this.servico = servico;
        this.autenticacao = autenticacao;
        this.dimensaoMaxima = Math.min(dimensaoMaxima, Tabuleiro.DIMENSAO_MAXIMA_BITMASK);
    }

    @PostMapping
    public ResponseEntity<?> iniciar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) CavaloRequest request) {

        var naoAutorizado = autenticacao.verificar(authorization, request != null ? request.token() : null);
        if (naoAutorizado.isPresent()) return naoAutorizado.get();

        var invalida = Respostas.validarDimensao(request != null ? request.dimensao() : null, dimensaoMaxima);
        if (invalida.isPresent()) return invalida.get();

        try {
            Calculo calculo = servico.iniciar(request.dimensao());
            URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}").buildAndExpand(calculo.id()).toUri();
            return ResponseEntity.accepted().location(local).body(calculo.resposta());
        } catch (CalculoEmAndamentoException e) {
            return Respostas.erro(HttpStatus.TOO_MANY_REQUESTS,
                    "Já existe um cálculo em andamento. Acompanhe-o ou cancele-o antes de iniciar outro.",
                    "calculoEmAndamento", e.idEmAndamento());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> consultar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String id) {

        var naoAutorizado = autenticacao.verificar(authorization, null);
        if (naoAutorizado.isPresent()) return naoAutorizado.get();

        return converterId(id)
                .flatMap(servico::buscar)
                .<ResponseEntity<?>>map(c -> ResponseEntity.ok(c.resposta()))
                .orElseGet(() -> naoEncontrado(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String id) {

        var naoAutorizado = autenticacao.verificar(authorization, null);
        if (naoAutorizado.isPresent()) return naoAutorizado.get();

        Optional<UUID> uuid = converterId(id);
        if (uuid.isEmpty()) return naoEncontrado(id);

        try {
            return ResponseEntity.accepted().body(servico.cancelar(uuid.get()).resposta());
        } catch (NoSuchElementException e) {
            return naoEncontrado(id);
        } catch (IllegalStateException e) {
            return Respostas.erro(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    private static Optional<UUID> converterId(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static ResponseEntity<?> naoEncontrado(String id) {
        return Respostas.erro(HttpStatus.NOT_FOUND, "Cálculo não encontrado: " + id);
    }
}
