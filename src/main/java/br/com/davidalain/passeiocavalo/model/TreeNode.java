package br.com.davidalain.passeiocavalo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Nó de uma árvore genérica. Cada nó conhece o pai, a profundidade e os filhos.
 * <p>
 * A lista de filhos é sincronizada: as soluções paralelas adicionam e removem filhos do mesmo nó
 * a partir de threads diferentes.
 */
public class TreeNode<T extends Comparable<T>> {

    public final T value;
    public final int depth;
    public final TreeNode<T> parentNode;
    public final List<TreeNode<T>> childNodes;

    public TreeNode(T value, int depth, TreeNode<T> parentNode) {
        this.value = value;
        this.depth = depth;
        this.parentNode = parentNode;
        this.childNodes = Collections.synchronizedList(new ArrayList<>());
    }

    public void addChild(TreeNode<T> childNode) {
        this.childNodes.add(childNode);
    }

    /** Cria um filho com o valor dado (profundidade = a deste nó + 1) e o adiciona. */
    public TreeNode<T> addChild(T childNodeValue) {
        final TreeNode<T> childNode = new TreeNode<>(childNodeValue, depth + 1, this);
        this.childNodes.add(childNode);
        return childNode;
    }

    public boolean deleteChild(T childNodeValue) {
        return this.childNodes.removeIf(child -> child.value.equals(childNodeValue));
    }

    public void deleteChild(TreeNode<T> childNode) {
        this.childNodes.remove(childNode);
    }

    public T getValue() {
        return value;
    }

    public List<TreeNode<T>> getChildNodes() {
        return childNodes;
    }

    /** Raiz da árvore à qual este nó pertence. */
    public TreeNode<T> getRoot() {
        TreeNode<T> atual = this;
        while (atual.parentNode != null) {
            atual = atual.parentNode;
        }
        return atual;
    }

    /** Valores do caminho da raiz até este nó, nessa ordem. */
    public List<T> getPathFromRoot() {
        List<T> caminho = new ArrayList<>(depth);
        for (TreeNode<T> atual = this; atual != null; atual = atual.parentNode) {
            caminho.add(atual.value);
        }
        Collections.reverse(caminho);
        return caminho;
    }

    /** Todas as folhas da subárvore que começa neste nó. */
    public List<TreeNode<T>> getLeaves() {
        List<TreeNode<T>> folhas = new ArrayList<>();
        coletarFolhas(this, folhas);
        return folhas;
    }

    private static <K extends Comparable<K>> void coletarFolhas(TreeNode<K> no, List<TreeNode<K>> folhas) {
        List<TreeNode<K>> filhos = no.snapshotChildren();
        if (filhos.isEmpty()) {
            folhas.add(no);
            return;
        }
        for (TreeNode<K> filho : filhos) {
            coletarFolhas(filho, folhas);
        }
    }

    // Cópia da lista de filhos tirada com o lock, segura para percorrer.
    private List<TreeNode<T>> snapshotChildren() {
        synchronized (childNodes) {
            return new ArrayList<>(childNodes);
        }
    }

    public static <K extends Comparable<K>> Comparator<TreeNode<K>> comparingByValue() {
        return (Comparator<TreeNode<K>> & Serializable)
                (t1, t2) -> t1.value.compareTo(t2.value);
    }

    @Override
    public String toString() {
        final String spacing = "  ".repeat(depth);
        final List<TreeNode<T>> filhos = snapshotChildren();
        return "\r\n" + spacing + "TreeNode{" +
                "value=" + value +
                ", depth=" + depth +
                ", childNodes=" +
                (filhos.isEmpty() ?
                        "[EMPTY]" :
                        filhos.stream().map(Object::toString).collect(Collectors.joining(",")) + spacing) +
                "}\r\n";
    }
}
