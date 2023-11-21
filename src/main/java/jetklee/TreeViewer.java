package jetklee;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TreeViewer extends JPanel {
    public Tree tree;
    public int selectedRound;
    public float zoom;
    public static int borderSize = 100;
    public static int nodeSeparatorHorizontal = 25;
    public static int nodeSeparatorVertical = 200;
    public static int nodeSize = 75;
    private List<Dimension> areas;
    private static final float minZoom = 0.05f;
    private static final float maxZoom = 1.5f;
    public Rectangle viewRect;

    private static final int clickRadius = nodeSize/5;

    public TreeViewer(Tree tree_) {
        tree = tree_;
        zoom = 1.0f;

        selectedRound = 0;
        areas = new ArrayList<>();
        viewRect = null;

        setAutoscrolls(true);
        MouseAdapter ma = new MouseAdapter() {
            private Point origin = null;

            @Override
            public void mousePressed(MouseEvent e) {
                origin = new Point(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                origin = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (origin != null) {
                    Rectangle rect = new Rectangle(getVisibleRect());
                    rect.x += origin.x - e.getX();
                    rect.y += origin.y - e.getY();
                    scrollRectToVisible(rect);
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void onZoomChanged(int wheelRotations) {
        float newZoom = zoom + wheelRotations / 10.0f;
        if (newZoom < minZoom || newZoom > maxZoom) return;
        Point mouse = getMousePosition();
        viewRect = new Rectangle(getVisibleRect());
        if (mouse != null) {
            viewRect.x += Math.round(newZoom / zoom * mouse.x) - mouse.x;
            viewRect.y += Math.round(newZoom / zoom * mouse.y) - mouse.y;
        }
        zoom = newZoom;
        updateArea();
    }

    public boolean onMouseClicked(int clickX, int clickY) {
        for (Node node : tree.nodes.values()) {
            int rectX = Math.round(zoom * (node.viewProps.x - nodeSize / 2));
            int rectY = Math.round(zoom * (node.viewProps.y - nodeSize / 2));
            boolean isInNode = (rectX <= clickX && clickX <= rectX + zoom * nodeSize) && (rectY <= clickY && clickY <= rectY + zoom * nodeSize);
            if (isVisibleNode(node, selectedRound) && isInNode) {
                System.out.println("Node " + node.id + " clicked.");
                return true;
            }
        }
        System.out.println("Nothing clicked.");
        return false;
    }

    public void load() {
        if (tree.root == null) return;
        computeNodeLocations(tree.root, borderSize + nodeSize / 2, 0);

        for (int i = 0; i < tree.roundCounter; ++i) {
            Dimension area = new Dimension(0, 0);
            computeAreas(tree.root, i, area);
            area.width += borderSize;
            area.height += borderSize;
            areas.add(area);
        }
        updateArea();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (tree != null && tree.root != null) {
                    Rectangle rect = new Rectangle(getVisibleRect());
                    rect.x = tree.root.viewProps.x + nodeSize / 2 - (int) rect.getWidth() / 2;
                    rect.y = tree.root.viewProps.y + nodeSize / 2 - (int) rect.getHeight() / 2;
                    scrollRectToVisible(rect);
                }
            }
        });
    }

    private int computeNodeLocations(Node node, int minX, int depth) {
        if (node.left != null && node.right != null) {
            minX = Math.max(minX, computeNodeLocations(node.left, minX, depth + 1));
            minX = Math.max(minX, computeNodeLocations(node.right, minX + nodeSize + nodeSeparatorHorizontal, depth + 1));
            node.viewProps.x = (node.left.viewProps.x + node.right.viewProps.x) / 2;

            node.viewProps.subTreeMinX = node.left.viewProps.subTreeMinX;
            node.viewProps.subTreeMaxX = node.right.viewProps.subTreeMaxX;

//        } else if (node.left != null) {
//            minX = Math.max(minX, computeNodeLocations(node.left, minX, depth + 1));
//            node.viewProps.x = node.left.viewProps.x;
//        } else if (node.right != null) {
//            minX = Math.max(minX, computeNodeLocations(node.right, minX, depth + 1));
//            node.viewProps.x = node.right.viewProps.x;
        } else {
            node.viewProps.x = minX;
            node.viewProps.subTreeMinX = minX - nodeSize / 2 - nodeSeparatorHorizontal / 2;
            node.viewProps.subTreeMaxX = minX + nodeSize / 2 + nodeSeparatorHorizontal / 2;
        }

        node.viewProps.y = borderSize + depth * (nodeSize + nodeSeparatorVertical);
        return minX;
    }

    private void computeAreas(Node node, int round, Dimension area) {
        if (node == null || !isVisibleNode(node, round)) return;

        computeAreas(node.left, round, area);
        computeAreas(node.right, round, area);

        area.width = Math.max(area.width, node.viewProps.x + nodeSize / 2);
        area.height = Math.max(area.height, node.viewProps.y + nodeSize / 2);
    }

    public void updateArea() {
        Dimension area = areas.get(selectedRound);
        setPreferredSize(new Dimension(Math.round(zoom * area.width), Math.round(zoom * area.height)));
        revalidate();
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (viewRect != null) {
            scrollRectToVisible(viewRect);
            viewRect = null;
        }
        if (tree.root != null) drawSubTree(g, tree.root, getVisibleRect());
    }

    private boolean isVisibleNode(Node node, int round) {
//        return true;
        return node.startRound <= round && round < node.endRound;
    }

    private void drawChild(Graphics g, Node child) {
        g.drawLine(
                Math.round(zoom * child.parent.viewProps.x),
                Math.round(zoom * (child.parent.viewProps.y + nodeSize / 2.0f)),
                Math.round(zoom * child.viewProps.x),
                Math.round(zoom * (child.viewProps.y - nodeSize / 2.0f))
        );
        drawSubTree(g, child, getVisibleRect());
    }

    private void drawSubTree(Graphics g, Node node, Rectangle visibleRect) {
        if (!isVisibleNode(node, selectedRound)) return;
        // whole subtree on left or right side of the visible rectangle
        if (zoom * node.viewProps.subTreeMinX > visibleRect.x + visibleRect.width || zoom * node.viewProps.subTreeMaxX < visibleRect.x)
            return;
        // whole subtree under the visible rectangle
        if (zoom * (node.viewProps.y - nodeSize / 2.0f) > visibleRect.y + visibleRect.height)
            return;
        // node above the visible rectangle
        if (zoom * (node.viewProps.y + nodeSize + nodeSeparatorVertical) < visibleRect.y) {
            // decide for children
            if (node.left != null && isVisibleNode(node.left, selectedRound))
                drawSubTree(g, node.left, getVisibleRect());
            if (node.right != null && isVisibleNode(node.right, selectedRound))
                drawSubTree(g, node.right, getVisibleRect());
            return;
        }

        if (node.left != null && isVisibleNode(node.left, selectedRound)) drawChild(g, node.left);
        if (node.right != null && isVisibleNode(node.right, selectedRound)) drawChild(g, node.right);

//        System.out.println("Drawing: " + node.id);

        g.drawOval(
                Math.round(zoom * (node.viewProps.x - nodeSize / 2.0f)),
                Math.round(zoom * (node.viewProps.y - nodeSize / 2.0f)),
                Math.round(zoom * nodeSize),
                Math.round(zoom * nodeSize)
        );
        g.drawString(
                Integer.toString(node.id),
                Math.round(zoom * (node.viewProps.x)),
                Math.round(zoom * (node.viewProps.y))
        );
    }

}
