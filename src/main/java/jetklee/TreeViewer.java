package jetklee;

import javax.swing.*;
import java.awt.*;

public class TreeViewer extends JPanel {
    public Tree tree;
    public int selectedRound;
    public float zoom;
    public static int borderSize = 50;
    public static int nodeSeparatorHorizontal = 50;
    public static int nodeSeparatorVertical = 100;
    public static int nodeSize = 50;

    public TreeViewer(Tree tree_) {
        tree = tree_;
        zoom = 1.0f;

        selectedRound = 0;
        setPreferredSize(new Dimension(2000, 2000));
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (tree.root != null)
            drawSubTree(g, tree.root);
    }

    private boolean isVisibleNode(Node node) {
        return node.viewProps.startRound <= selectedRound && selectedRound < node.viewProps.endRound;
    }

    private void drawChild(Graphics g, Node child) {
        g.drawLine(child.parent.viewProps.x, child.parent.viewProps.y + nodeSize / 2, child.viewProps.x, child.viewProps.y - nodeSize / 2);
        drawSubTree(g, child);
    }

    private void drawSubTree(Graphics g, Node node) {
        if (!isVisibleNode(node))
            return;
        if (node.right != null && isVisibleNode(node.right))
            drawChild(g, node.right);
        if (node.left != null && isVisibleNode(node.left))
            drawChild(g, node.left);

        g.drawOval(node.viewProps.x - nodeSize / 2, node.viewProps.y - nodeSize / 2, nodeSize, nodeSize);
    }

    public void load() {
        if (tree.root != null)
            computeNodeLocations(tree.root, borderSize + nodeSize / 2, 0);
    }

    private int computeNodeLocations(Node node, int minX, int depth) {
        if (node.left != null && node.right != null) {
            minX = Math.max(minX, computeNodeLocations(node.left, minX, depth + 1));
            minX = Math.max(minX, computeNodeLocations(node.right, minX + nodeSize + nodeSeparatorHorizontal, depth + 1));
            node.viewProps.x = (node.left.viewProps.x + node.right.viewProps.x) / 2;
        } else if (node.left != null) {
            minX = Math.max(minX, computeNodeLocations(node.left, minX, depth + 1));
            node.viewProps.x = node.left.viewProps.x;
        } else if (node.right != null) {
            minX = Math.max(minX, computeNodeLocations(node.right, minX, depth + 1));
            node.viewProps.x = node.right.viewProps.x;
        } else {
            node.viewProps.x = minX;
        }

        node.viewProps.y = borderSize + depth * (nodeSize + nodeSeparatorVertical);
        return minX;
    }

}
