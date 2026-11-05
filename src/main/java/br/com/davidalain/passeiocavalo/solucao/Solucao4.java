package br.com.davidalain.passeiocavalo.solucao;

import br.com.davidalain.passeiocavalo.model.Posicao;
import br.com.davidalain.passeiocavalo.model.Tabuleiro;
import br.com.davidalain.passeiocavalo.model.TreeNode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * <h2>Solução 4 — Paralelismo só no topo da árvore de busca</h2>
 *
 * <b>Objetivo:</b> o mesmo da Solução 3 (contar todos os passeios e montar a árvore), com melhor uso das threads.
 * <p>
 * <b>Lógica:</b> igual à Solução 3, com uma mudança: só os primeiros níveis da busca
 * (menos de N²/4 movimentos) usam {@code parallelStream()}. Do meio para baixo, cada thread continua
 * sozinha com {@code stream()} sequencial.
 * <p>
 * <b>Por quê:</b> perto da raiz há poucos ramos, e cada um esconde uma subárvore enorme. Dividir ali gera
 * tarefas grandes, que compensam o custo de distribuí-las entre as threads. Perto das folhas as tarefas
 * são minúsculas, e paralelizá-las (como na Solução 3) só acrescenta custo de coordenação.
 * <p>
 * Ainda copia a matriz {@code boolean[][]} a cada passo; a Solução 5 troca isso por uma máscara de bits.
 */
public record Solucao4(
        Tabuleiro tabuleiro
) {

    /**
     * Conta os passeios a partir de {@code posicaoInicial}, gravando o total em
     * {@link Tabuleiro#mapaPosicaoInicialQuantidadeSolucoes()}.
     *
     * @return a raiz da árvore com todos os passeios encontrados, ou {@code null} se não há nenhum
     */
    public TreeNode<Posicao> encontrarPasseioDoCavalo4(Posicao posicaoInicial) {
        final TreeNode<Posicao> raiz = new TreeNode<>(posicaoInicial, 1, null);
        final boolean[][] visitados = new boolean[tabuleiro.dimensao()][tabuleiro.dimensao()];
        visitados[posicaoInicial.linha()][posicaoInicial.coluna()] = true;

        tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().put(posicaoInicial, 0L);
        encontrarPasseioRecursivo4(raiz, visitados, 1);

        return tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().get(posicaoInicial) > 0 ? raiz : null;
    }

    private void encontrarPasseioRecursivo4(TreeNode<Posicao> noAtual, boolean[][] visitados, int movimentos) {
        // Caso base: visitou todas as casas -> é um passeio completo
        if (movimentos == tabuleiro.totalCasas()) {
            final Posicao primeiraPosicao = noAtual.getRoot().value;
            tabuleiro.mapaPosicaoInicialQuantidadeSolucoes().merge(primeiraPosicao, 1L, Long::sum);
            return;
        }

        final List<Posicao> proximosMovimentos = tabuleiro.grafoMovimentos().get(noAtual.value);

        // Paraleliza apenas no início da árvore, onde as subárvores são grandes
        final boolean usarParalelo = movimentos < tabuleiro.totalCasas() / 4;
        final Stream<Posicao> stream = usarParalelo ?
                proximosMovimentos.parallelStream() :
                proximosMovimentos.stream();

        stream.forEach(proximaPosicao -> {
            if (visitados[proximaPosicao.linha()][proximaPosicao.coluna()]) return;

            final TreeNode<Posicao> filho = noAtual.addChild(proximaPosicao);
            final boolean[][] visitadosNovo = cloneVisitados(visitados);
            visitadosNovo[proximaPosicao.linha()][proximaPosicao.coluna()] = true;

            encontrarPasseioRecursivo4(filho, visitadosNovo, movimentos + 1);
        });

        // Chegando aqui, o nó é (1) intermediário de algum passeio ou (2) um beco sem saída.
        // No caso (2) ele não tem filhos: remove da árvore para liberar memória.
        if (noAtual.childNodes.isEmpty() && noAtual.parentNode != null) {
            noAtual.parentNode.deleteChild(noAtual);
        }
    }

    // Cópia independente da matriz de visitados, para o ramo poder rodar em outra thread
    private boolean[][] cloneVisitados(boolean[][] visitados) {
        boolean[][] visitadosNovo = new boolean[tabuleiro.dimensao()][];
        for (int i = 0; i < tabuleiro.dimensao(); i++) {
            visitadosNovo[i] = Arrays.copyOf(visitados[i], tabuleiro.dimensao());
        }
        return visitadosNovo;
    }
}
