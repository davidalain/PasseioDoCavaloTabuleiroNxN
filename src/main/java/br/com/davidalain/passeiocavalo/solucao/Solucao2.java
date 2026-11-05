package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.model.Tabuleiro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <h2>Solução 2 — Backtracking (busca em profundidade) + Warnsdorff</h2>
 *
 * <b>Objetivo:</b> encontrar <i>um</i> passeio, com garantia: se existir, ele é encontrado.
 * <p>
 * <b>Lógica:</b> busca em profundidade recursiva.
 * <ol>
 *     <li>Marca a casa atual como visitada e a coloca no caminho;</li>
 *     <li>Se o caminho tem N² casas, achou um passeio;</li>
 *     <li>Senão, tenta cada vizinho livre, na ordem de Warnsdorff (menos saídas primeiro);</li>
 *     <li>Se nenhum vizinho leva a uma solução, desmarca a casa e volta (backtracking).</li>
 * </ol>
 * A ordem de Warnsdorff não muda o resultado, mas faz o primeiro passeio aparecer bem mais cedo.
 * <p>
 * <b>Diferença para a Solução 1:</b> quando fica sem saída, volta atrás e tenta outro caminho,
 * em vez de desistir. No pior caso o custo é exponencial, mas a resposta está garantida.
 */
public record Solucao2(
        Tabuleiro tabuleiro
) {

    /**
     * @param inicio casa onde o cavalo começa
     * @return a sequência de casas do primeiro passeio encontrado, ou {@code null} se não existe passeio
     */
    public List<Posicao> encontrarPasseioDoCavalo2(Posicao inicio) {
        List<Posicao> caminho = new ArrayList<>();
        boolean[][] visitado = new boolean[tabuleiro.dimensao()][tabuleiro.dimensao()];

        caminho.add(inicio);
        visitado[inicio.linha()][inicio.coluna()] = true;

        if (encontrarPasseioRecursivo(inicio, caminho, visitado)) {
            return caminho;
        }
        return null;
    }

    private boolean encontrarPasseioRecursivo(Posicao atual, List<Posicao> caminho, boolean[][] visitado) {
        // Caso base: todas as casas foram visitadas
        if (caminho.size() == tabuleiro.totalCasas()) {
            return true;
        }

        // Copia a lista antes de ordenar: o grafo é compartilhado e não pode ser alterado
        List<Posicao> proximosMovimentos = new ArrayList<>(tabuleiro.grafoMovimentos().get(atual));
        proximosMovimentos.sort(Comparator.comparingInt(p -> contarMovimentosDisponiveis(p, visitado)));

        for (Posicao proxima : proximosMovimentos) {
            if (visitado[proxima.linha()][proxima.coluna()]) continue;

            // Faz o movimento
            visitado[proxima.linha()][proxima.coluna()] = true;
            caminho.add(proxima);

            if (encontrarPasseioRecursivo(proxima, caminho, visitado)) {
                return true;
            }

            // Desfaz o movimento (backtracking)
            visitado[proxima.linha()][proxima.coluna()] = false;
            caminho.remove(caminho.size() - 1);
        }

        return false;
    }

    private int contarMovimentosDisponiveis(Posicao posicao, boolean[][] visitado) {
        int livres = 0;
        for (Posicao p : tabuleiro.grafoMovimentos().get(posicao)) {
            if (!visitado[p.linha()][p.coluna()]) livres++;
        }
        return livres;
    }
}
