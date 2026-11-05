package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.model.Tabuleiro;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <h2>Solução 1 — Busca gulosa com a heurística de Warnsdorff</h2>
 *
 * <b>Objetivo:</b> encontrar <i>um</i> passeio (não conta todos).
 * <p>
 * <b>Lógica:</b> a cada passo o cavalo vai para a casa vizinha não visitada que tem
 * <i>menos</i> saídas livres (regra de Warnsdorff, 1823). A ideia é visitar primeiro as casas
 * "difíceis" (cantos e bordas), antes que fiquem isoladas. Em caso de empate, vale a primeira
 * casa na ordem do grafo.
 * <p>
 * <b>Limitação:</b> não há retrocesso (backtracking). Se o cavalo ficar sem saída antes de visitar
 * todas as casas, o método desiste e retorna {@code null}, mesmo que exista um passeio a partir
 * daquela casa. É muito rápido (O(N²)), mas não garante a resposta.
 */
public record Solucao1(
        Tabuleiro tabuleiro
) {

    /**
     * @param inicio casa onde o cavalo começa
     * @return a sequência de casas do passeio, ou {@code null} se a busca gulosa ficou sem saída
     */
    public List<Posicao> encontrarPasseioDoCavalo(Posicao inicio) {
        final int totalPosicoes = tabuleiro.totalCasas();
        final List<Posicao> caminho = new ArrayList<>(totalPosicoes);
        final Set<Posicao> visitados = new HashSet<>();

        Posicao atual = inicio;
        caminho.add(atual);
        visitados.add(atual);

        while (caminho.size() < totalPosicoes) {
            // Escolhe o vizinho livre com o menor número de saídas livres
            Posicao proxima = null;
            int menorGrau = Integer.MAX_VALUE;
            for (Posicao vizinho : tabuleiro.grafoMovimentos().get(atual)) {
                if (visitados.contains(vizinho)) continue;

                int grau = contarSaidasLivres(vizinho, visitados);
                if (grau < menorGrau) {
                    menorGrau = grau;
                    proxima = vizinho;
                }
            }

            if (proxima == null) {
                return null; // Sem saída: a heurística falhou para esta casa inicial
            }

            atual = proxima;
            caminho.add(atual);
            visitados.add(atual);
        }

        return caminho;
    }

    private int contarSaidasLivres(Posicao posicao, Set<Posicao> visitados) {
        int livres = 0;
        for (Posicao p : tabuleiro.grafoMovimentos().get(posicao)) {
            if (!visitados.contains(p)) livres++;
        }
        return livres;
    }
}
