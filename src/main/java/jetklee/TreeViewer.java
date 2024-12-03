package jetklee;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static jetklee.Styles.*;

/**
 * Panel responsible for rendering and displaying the process tree.
 */

public class TreeViewer extends JPanel {

    private float zoom;
    private int selectedRound;
    private Node selectedNode;
    private Tree tree;
    private List<Dimension> areas;
    private Rectangle viewRect;
    private static Font font = makeFont(1.0f);
    private static final float MIN_ZOOM = 0.0f;
    private static final float MAX_ZOOM = 1.5f;
    private static final float TEXT_ZOOM_LIMIT = 0.2f;
    private static final int BORDER_SIZE = 100;
    private static final int NODE_SEPARATOR_HORIZONTAL = 25;
    private static final int NODE_SEPARATOR_VERTICAL = 200;
    private static final int NODE_WIDTH = 100;
    private static final int NODE_HEIGHT = 50;
    private static final float EDGE_THICKNESS = 1.0f;

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

        if (newZoom < MIN_ZOOM || newZoom > MAX_ZOOM){
            return;
        }
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
        for (Node node : tree.getNodes().values()) {
            int rectX = Math.round(zoom * (node.getViewProps().getX() - NODE_WIDTH / 2));
            int rectY = Math.round(zoom * (node.getViewProps().getY() - NODE_HEIGHT / 2));
            boolean isInNode = (rectX <= clickX && clickX <= rectX + zoom * NODE_WIDTH)
                    && (rectY <= clickY && clickY <= rectY + zoom * NODE_HEIGHT);

            if (isVisibleNode(node, selectedRound) && isInNode) {
                return node;
            }
        }
        return null;
    }

    /**
     * Computes node locations and size of panel needed in each round.
     * Scrolls the view to make the tree root visible.
     */
    public void load() {
        if (tree.getRoot() == null) return;
        computeNodeLocations(tree.getRoot(), BORDER_SIZE + NODE_WIDTH / 2, 0);

        for (int i = 0; i < tree.getRoundCounter(); ++i) {
            Dimension area = new Dimension(0, 0);
            computeAreas(tree.getRoot(), i, area);
            area.width += BORDER_SIZE;
            area.height += BORDER_SIZE;
            areas.add(area);
        }
        updateArea();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (tree != null && tree.getRoot() != null) {
                    Rectangle rect = new Rectangle(getVisibleRect());
                    rect.x = tree.getRoot().getViewProps().getX() + NODE_WIDTH / 2 - (int) rect.getWidth() / 2;
                    rect.y = tree.getRoot().getViewProps().getY() + NODE_HEIGHT / 2 - (int) rect.getHeight() / 2;
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
        if (tree.getRoot() != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setFont(font);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setStroke(new BasicStroke(EDGE_THICKNESS));
            drawSubTree(g2d, tree.getRoot(), getVisibleRect());
            if (selectedNode != null) {
                drawCross(g, selectedNode);
            }
        }
    }

    private void drawCross(Graphics g, Node node) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.GRAY);
        int nodeX = Math.round(zoom * node.getViewProps().getX());
        int nodeY = Math.round(zoom * node.getViewProps().getY());

        // Horizontal line
        g2d.drawLine(0, nodeY, getWidth(), nodeY);
        // Vertical line
        g2d.drawLine(nodeX, 0, nodeX, getHeight());
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
        if (node.getLeft() != null && node.getRight() != null) {
            minX = Math.max(minX, computeNodeLocations(node.getLeft(), minX, depth + 1));
            minX = Math.max(minX, computeNodeLocations(node.getRight(), minX + NODE_WIDTH + NODE_SEPARATOR_HORIZONTAL, depth + 1));
            node.getViewProps().setX((node.getLeft().getViewProps().getX() + node.getRight().getViewProps().getX()) / 2);

            node.getViewProps().setSubTreeMinX(node.getLeft().getViewProps().getSubTreeMinX());
            node.getViewProps().setSubTreeMaxX(node.getRight().getViewProps().getSubTreeMaxX());
        } else {
            node.getViewProps().setX(minX);
            node.getViewProps().setSubTreeMinX(minX - NODE_WIDTH / 2 - NODE_SEPARATOR_HORIZONTAL / 2);
            node.getViewProps().setSubTreeMaxX(minX + NODE_WIDTH / 2 + NODE_SEPARATOR_HORIZONTAL / 2);
        }

        node.getViewProps().setY(BORDER_SIZE + depth * (NODE_HEIGHT + NODE_SEPARATOR_VERTICAL));
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

        computeAreas(node.getLeft(), round, area);
        computeAreas(node.getRight(), round, area);

        area.width = Math.max(area.width, node.getViewProps().getX() + NODE_WIDTH / 2);
        area.height = Math.max(area.height, node.getViewProps().getY() + NODE_HEIGHT / 2);
    }

    private boolean isVisibleNode(Node node, int round) {
        return node.getStartRound() <= round && round < node.getEndRound();
    }

    private void drawChild(Graphics2D g2d, Node child) {
        g2d.drawLine(
                Math.round(zoom * child.getParent().getViewProps().getX()),
                Math.round(zoom * (child.getParent().getViewProps().getY() + NODE_HEIGHT / 2)),
                Math.round(zoom * child.getViewProps().getX()),
                Math.round(zoom * (child.getViewProps().getY() - NODE_HEIGHT / 2))
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
        if (zoom * node.getViewProps().getSubTreeMinX() > visibleRect.x + visibleRect.width ||
                zoom * node.getViewProps().getSubTreeMaxX() < visibleRect.x)
            return;
        // whole subtree under the visible rectangle
        if (zoom * (node.getViewProps().getY() - NODE_HEIGHT / 2) > visibleRect.y + visibleRect.height)
            return;
        // node above the visible rectangle
        if (zoom * (node.getViewProps().getY() + NODE_HEIGHT + NODE_SEPARATOR_VERTICAL) < visibleRect.y) {
            // decide for children
            if (node.getLeft() != null && isVisibleNode(node.getLeft(), selectedRound))
                drawSubTree(g2d, node.getLeft(), getVisibleRect());
            if (node.getRight() != null && isVisibleNode(node.getRight(), selectedRound))
                drawSubTree(g2d, node.getRight(), getVisibleRect());
            return;
        }

        if (node.getLeft() != null && isVisibleNode(node.getLeft(), selectedRound)) {
            g2d.setColor(RED_COLOR);
            drawChild(g2d, node.getLeft());
        }
        if (node.getRight() != null && isVisibleNode(node.getRight(), selectedRound)) {
            g2d.setColor(GREEN_COLOR);
            drawChild(g2d, node.getRight());
        }

        g2d.setColor(BLACK_COLOR);
        g2d.drawRect(
                Math.round(zoom * (node.getViewProps().getX() - NODE_WIDTH / 2)),
                Math.round(zoom * (node.getViewProps().getY() - NODE_HEIGHT / 2)),
                Math.round(zoom * NODE_WIDTH),
                Math.round(zoom * NODE_HEIGHT)
        );

        if (zoom >= TEXT_ZOOM_LIMIT) {
            g2d.drawString(
                    Integer.toString(node.getId()),
                    Math.round(zoom * (node.getViewProps().getX() - NODE_WIDTH / 2)),
                    Math.round(zoom * (node.getViewProps().getY() + NODE_HEIGHT / 4))
            );
        }
    }

    private static Font makeFont(float zoom) {
        return font = new Font(CODE_FONT, Font.PLAIN, Math.round((0.75f * NODE_HEIGHT) * zoom));
    }

    public void setSelectedRound(int selectedRound) {
        this.selectedRound = selectedRound;
    }

    public void setSelectedNode(Node selectedNode) {
        this.selectedNode = selectedNode;
    }
}
