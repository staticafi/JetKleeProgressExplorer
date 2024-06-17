package jetklee;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel responsible for rendering and displaying the process tree.
 */

public class TreeViewer extends JPanel {
    public Tree tree;
    public int selectedRound;
    private List<Dimension> areas;
    private Rectangle viewRect;
    private float zoom;
    private static Font font = makeFont(1.0f);
    private static final float minZoom = 0.0f;
    private static final float maxZoom = 1.5f;
    private static final float textZoomLimit = 0.2f;
    private static final int borderSize = 100;
    private static final int nodeSeparatorHorizontal = 25;
    private static final int nodeSeparatorVertical = 200;
    private static final int nodeWidth = 100;
    private static final int nodeHeight = 50;
    private static final Color leftColor = Color.RED;
    private static final Color rightColor = new Color(34, 139, 34); // green
    private static final Color nodeColor = Color.BLACK;

    private static final float edgeThickness = 2.0f;

    /**
     * Enables mouse dragging to navigate through the process tree displayed in the tree panel.
     *
     * @param tree_ process tree to be displayed.
     */
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
                if (origin != null && SwingUtilities.isLeftMouseButton(e)) {
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

    /**
     * Adjusts the zoom level based on mouse wheel rotations, updates the view.
     *
     * @param wheelRotations number of mouse wheel rotations, change in zoom.
     */
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
        font = makeFont(zoom);
        updateArea();
    }

    /**
     * Checks which node was clicked.
     *
     * @param clickX x coordinate of the click on the JPanel.
     * @param clickY y coordinate of the click on the JPanel.
     * @return clicked node or null.
     */
    public Node onMouseClicked(int clickX, int clickY) {
        for (Node node : tree.nodes.values()) {
            int rectX = Math.round(zoom * (node.viewProps.x - nodeWidth / 2));
            int rectY = Math.round(zoom * (node.viewProps.y - nodeHeight / 2));
            boolean isInNode = (rectX <= clickX && clickX <= rectX + zoom * nodeWidth) && (rectY <= clickY && clickY <= rectY + zoom * nodeHeight);
            if (isVisibleNode(node, selectedRound) && isInNode) return node;
        }
        return null;
    }

    /**
     * Computes node locations and size of panel needed in each round.
     * Scrolls the view to make the tree root visible.
     */
    public void load() {
        if (tree.root == null) return;
        computeNodeLocations(tree.root, borderSize + nodeWidth / 2, 0);

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
                    rect.x = tree.root.viewProps.x + nodeWidth / 2 - (int) rect.getWidth() / 2;
                    rect.y = tree.root.viewProps.y + nodeHeight / 2 - (int) rect.getHeight() / 2;
                    scrollRectToVisible(rect);
                }
            }
        });
    }

    /**
     * Updates tree panel based on selected round and level of zoom.
     */
    public void updateArea() {
        Dimension area = areas.get(selectedRound);
        setPreferredSize(new Dimension(Math.round(zoom * area.width), Math.round(zoom * area.height)));
        revalidate();
        repaint();
    }

    /**
     * Customize rendering of the tree panel. Scrolls to visible rectangle and draws the tree.
     *
     * @param g the Graphics object used for painting.
     */
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (viewRect != null) {
            scrollRectToVisible(viewRect);
            viewRect = null;
        }
        if (tree.root != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setFont(font);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setStroke(new BasicStroke(edgeThickness));
            drawSubTree(g2d, tree.root, getVisibleRect());
        }
    }

    /**
     * Recursively computes coordinates of the tree node on the JPanel.
     *
     * @param node  root of the tree for which coordinates are computed.
     * @param minX  minimal x-coordinate of the node's subtree.
     * @param depth depth of the node in the tree.
     * @return the minimum x-coordinate in the subtree.
     */
    private int computeNodeLocations(Node node, int minX, int depth) {
        if (node.left != null && node.right != null) {
            minX = Math.max(minX, computeNodeLocations(node.left, minX, depth + 1));
            minX = Math.max(minX, computeNodeLocations(node.right, minX + nodeWidth + nodeSeparatorHorizontal, depth + 1));
            node.viewProps.x = (node.left.viewProps.x + node.right.viewProps.x) / 2;

            node.viewProps.subTreeMinX = node.left.viewProps.subTreeMinX;
            node.viewProps.subTreeMaxX = node.right.viewProps.subTreeMaxX;
        } else {
            node.viewProps.x = minX;
            node.viewProps.subTreeMinX = minX - nodeWidth / 2 - nodeSeparatorHorizontal / 2;
            node.viewProps.subTreeMaxX = minX + nodeWidth / 2 + nodeSeparatorHorizontal / 2;
        }

        node.viewProps.y = borderSize + depth * (nodeHeight + nodeSeparatorVertical);
        return minX;
    }

    /**
     * Recursively computes size of JPanel for given round.
     *
     * @param node  root of the tree for which the size is computed.
     * @param round round for which the size is computed.
     * @param area  stores the result of the computation.
     */
    private void computeAreas(Node node, int round, Dimension area) {
        if (node == null || !isVisibleNode(node, round)) return;

        computeAreas(node.left, round, area);
        computeAreas(node.right, round, area);

        area.width = Math.max(area.width, node.viewProps.x + nodeWidth / 2);
        area.height = Math.max(area.height, node.viewProps.y + nodeHeight / 2);
    }

    private boolean isVisibleNode(Node node, int round) {
        return node.startRound <= round && round < node.endRound;
    }

    private void drawChild(Graphics2D g2d, Node child) {
        g2d.drawLine(
                Math.round(zoom * child.parent.viewProps.x),
                Math.round(zoom * (child.parent.viewProps.y + nodeHeight / 2)),
                Math.round(zoom * child.viewProps.x),
                Math.round(zoom * (child.viewProps.y - nodeHeight / 2))
        );
        drawSubTree(g2d, child, getVisibleRect());
    }

    /**
     * Recursively draws visible parts of the process tree.
     *
     * @param g2d         graphics component on which the tree is drawn.
     * @param node        root of the tree which is drawn.
     * @param visibleRect part of the JPanel which is currently visible on the screen.
     */
    private void drawSubTree(Graphics2D g2d, Node node, Rectangle visibleRect) {
        if (!isVisibleNode(node, selectedRound)) return;
        // whole subtree on left or right side of the visible rectangle
        if (zoom * node.viewProps.subTreeMinX > visibleRect.x + visibleRect.width || zoom * node.viewProps.subTreeMaxX < visibleRect.x)
            return;
        // whole subtree under the visible rectangle
        if (zoom * (node.viewProps.y - nodeHeight / 2) > visibleRect.y + visibleRect.height)
            return;
        // node above the visible rectangle
        if (zoom * (node.viewProps.y + nodeHeight + nodeSeparatorVertical) < visibleRect.y) {
            // decide for children
            if (node.left != null && isVisibleNode(node.left, selectedRound))
                drawSubTree(g2d, node.left, getVisibleRect());
            if (node.right != null && isVisibleNode(node.right, selectedRound))
                drawSubTree(g2d, node.right, getVisibleRect());
            return;
        }

        if (node.left != null && isVisibleNode(node.left, selectedRound)) {
            g2d.setColor(leftColor);
            drawChild(g2d, node.left);
        }
        if (node.right != null && isVisibleNode(node.right, selectedRound)) {
            g2d.setColor(rightColor);
            drawChild(g2d, node.right);
        }

        g2d.setColor(nodeColor);
        g2d.drawRect(
                Math.round(zoom * (node.viewProps.x - nodeWidth / 2)),
                Math.round(zoom * (node.viewProps.y - nodeHeight / 2)),
                Math.round(zoom * nodeWidth),
                Math.round(zoom * nodeHeight)
        );

        if (zoom >= textZoomLimit) {
            g2d.drawString(
                    Integer.toString(node.id),
                    Math.round(zoom * (node.viewProps.x - nodeWidth / 2)),
                    Math.round(zoom * (node.viewProps.y + nodeHeight / 4))
            );
        }
    }

    private static Font makeFont(float zoom) {
        return font = new Font("Monospaced", Font.PLAIN, Math.round((0.75f * nodeHeight) * zoom));
    }
}
