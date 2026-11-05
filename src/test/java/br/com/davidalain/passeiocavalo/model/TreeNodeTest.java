package br.com.davidalain.passeiocavalo.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class TreeNodeTest {

    @Test
    void addChild_define_pai_e_profundidade() {
        TreeNode<Integer> raiz = new TreeNode<>(1, 1, null);
        TreeNode<Integer> filho = raiz.addChild(2);
        TreeNode<Integer> neto = filho.addChild(3);

        assertSame(raiz, filho.parentNode);
        assertEquals(2, filho.depth);
        assertEquals(3, neto.depth);
        assertEquals(List.of(filho), raiz.getChildNodes());
        assertEquals(2, filho.getValue());
    }

    @Test
    void addChild_com_no_pronto() {
        TreeNode<Integer> raiz = new TreeNode<>(1, 1, null);
        TreeNode<Integer> outro = new TreeNode<>(9, 2, raiz);
        raiz.addChild(outro);
        assertEquals(List.of(outro), raiz.getChildNodes());
    }

    @Test
    void deleteChild_por_valor_e_por_no() {
        TreeNode<Integer> raiz = new TreeNode<>(1, 1, null);
        TreeNode<Integer> a = raiz.addChild(2);
        raiz.addChild(3);

        assertTrue(raiz.deleteChild(3));
        assertFalse(raiz.deleteChild(42));
        assertEquals(List.of(a), raiz.getChildNodes());

        raiz.deleteChild(a);
        assertTrue(raiz.getChildNodes().isEmpty());
    }

    @Test
    void raiz_caminho_e_folhas() {
        TreeNode<Integer> raiz = new TreeNode<>(1, 1, null);
        TreeNode<Integer> a = raiz.addChild(2);
        TreeNode<Integer> a1 = a.addChild(3);
        TreeNode<Integer> b = raiz.addChild(4);

        assertSame(raiz, a1.getRoot());
        assertSame(raiz, raiz.getRoot());
        assertEquals(List.of(1, 2, 3), a1.getPathFromRoot());
        assertEquals(List.of(a1, b), raiz.getLeaves());

        TreeNode<Integer> sozinho = new TreeNode<>(7, 1, null);
        assertEquals(List.of(sozinho), sozinho.getLeaves());
    }

    @Test
    void comparingByValue_ordena_pelos_valores() {
        TreeNode<Integer> raiz = new TreeNode<>(0, 1, null);
        List<TreeNode<Integer>> nos = new ArrayList<>(List.of(raiz.addChild(3), raiz.addChild(1), raiz.addChild(2)));
        nos.sort(TreeNode.comparingByValue());
        assertEquals(List.of(1, 2, 3), nos.stream().map(TreeNode::getValue).toList());
    }

    @Test
    void addChild_e_seguro_com_varias_threads() {
        TreeNode<Integer> raiz = new TreeNode<>(0, 1, null);
        IntStream.range(0, 10_000).parallel().forEach(raiz::addChild);
        assertEquals(10_000, raiz.getChildNodes().size());

        IntStream.range(0, 10_000).filter(i -> i % 2 == 0).parallel().forEach(raiz::deleteChild);
        assertEquals(5_000, raiz.getChildNodes().size());
    }

    @Test
    void toString_mostra_a_arvore() {
        TreeNode<Integer> raiz = new TreeNode<>(1, 1, null);
        raiz.addChild(2);
        String texto = raiz.toString();
        assertTrue(texto.contains("value=1"));
        assertTrue(texto.contains("value=2"));
        assertTrue(texto.contains("[EMPTY]"));
    }
}
