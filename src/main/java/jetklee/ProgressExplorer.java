package jetklee;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Paths;
import java.util.Collections;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Represents whole application. Initializes and updates UI components.
 */
public class ProgressExplorer implements ListSelectionListener, MouseWheelListener, MouseListener, ActionListener {
    private static final int ARGS_COUNT = 1;
    private Tree tree;
    private TreeViewer treeViewer;
    private SourceLoader sourceLoader;
    private SourceViewerC sourceC;
    private SourceViewerLL sourceLL;
    private JPanel rootPanel;
    private JList<String> roundsList;
    private JTabbedPane mainTabbedPane;
    private JTabbedPane nodeInfoTabbedPane;
    private JSplitPane splitPane;
    private JScrollPane roundScrollPane;
    private JSplitPane mainSplitPane;
    private JPanel treePanel;
    private JScrollPane treeScrollPane;
    private ConstraintsViewer constraintsViewer;
    private MemoryViewer memoryViewer;
    private ContextViewer contextViewer;
    private JPopupMenu rightClickMenu;
    private float divider;

    public ProgressExplorer() {
        tree = new Tree();
        treeViewer = new TreeViewer(tree);
        treeViewer.addMouseListener(this);

        sourceLoader = new SourceLoader();
        sourceC = new SourceViewerC(sourceLoader);
        sourceLL = new SourceViewerLL(sourceLoader);

        constraintsViewer = new ConstraintsViewer();
        memoryViewer = new MemoryViewer();
        contextViewer = new ContextViewer();
        divider = 0.5f;

        treeScrollPane = new JScrollPane(treeViewer);
        treeScrollPane.setWheelScrollingEnabled(false);
        treeScrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                treeViewer.onZoomChanged(-e.getWheelRotation());
            }
        });
        treePanel = new JPanel(new BorderLayout());
        treePanel.add(treeScrollPane, BorderLayout.CENTER);

        roundsList = new JList<>(new DefaultListModel<>());
        roundsList.addListSelectionListener(this);
        roundsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roundScrollPane = new JScrollPane(roundsList);

        mainTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        mainTabbedPane.addTab("Tree", treePanel);
        mainTabbedPane.addTab("C", sourceC);
        mainTabbedPane.addTab("LL", sourceLL);

        nodeInfoTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        nodeInfoTabbedPane.addTab("Context", contextViewer);
        nodeInfoTabbedPane.addTab("Constraints", constraintsViewer);
        nodeInfoTabbedPane.addTab("Memory", memoryViewer);
        nodeInfoTabbedPane.setVisible(false);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainTabbedPane, nodeInfoTabbedPane);
        splitPane.setResizeWeight(0.1);
        splitPane.setDividerLocation(0.1);

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, roundScrollPane, splitPane);
        mainSplitPane.setResizeWeight(0.1);
        mainSplitPane.setDividerLocation(0.1);

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(mainSplitPane, BorderLayout.CENTER);

        rightClickMenu = new JPopupMenu();
        NodeAction[] nodeActions = new NodeAction[]{NodeAction.NODE_INFO, NodeAction.NODE_TO_C, NodeAction.NODE_TO_LL};
        for (NodeAction nodeAction : nodeActions) {
            JMenuItem newItem = new JMenuItem(nodeAction.value);
            newItem.addActionListener(this);
            newItem.setActionCommand(nodeAction.value);
            rightClickMenu.add(newItem);
        }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            /**
             * Initializes and displays the main application window. Loads recorded data from directory specified by
             * command line argument.
             */
            public void run() {
                double startTime = System.currentTimeMillis();

                UIManager.put( "TabbedPane.selectedBackground", Color.white );
                FlatLightLaf.setGlobalExtraDefaults( Collections.singletonMap( "@accentColor", "#ADD8E6" ) );
                try {
                    UIManager.setLookAndFeel( new FlatLightLaf() );
                } catch( Exception ex ) {
                    System.err.println( "Failed to initialize LaF" );
                }

                JFrame frame = new JFrame("JetKlee: ProgressExplorer");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setPreferredSize(new Dimension(800, 600));

                ProgressExplorer explorer = new ProgressExplorer();
                if (args.length != ARGS_COUNT)
                    throw new IllegalArgumentException("Invalid number of arguments. Expected " + ARGS_COUNT + " arguments.");
                explorer.load(Paths.get(args[0]).toAbsolutePath().toString());

                frame.setContentPane(explorer.rootPanel);
                frame.pack();
                frame.setVisible(true);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

                double totalTime = (System.currentTimeMillis() - startTime) / 1000.0f;
                String formattedTime = String.format("%.2f", totalTime);
                System.out.println(formattedTime);
//                System.exit(0);
            }
        });
    }

    private enum TabbedPane {
        TREE_PANE, C_PANE, LL_PANE;
    }

    private enum NodeAction {
        NODE_INFO("Node Information"), NODE_TO_C("C"), NODE_TO_LL("LL");
        private final String value;

        NodeAction(String value_) {
            value = value_;
        }

        private static NodeAction parse(String actionStr) throws Exception {
            return switch (actionStr) {
                case "Node Information" -> NODE_INFO;
                case "C" -> NODE_TO_C;
                case "LL" -> NODE_TO_LL;
                default -> throw new Exception("Unknown right click menu action: " + actionStr);
            };
        }
    }

    /**
     * Updates current round based on the round selected in the list menu.
     *
     * @param e the event that characterizes the change in the list menu.
     */
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() != roundsList) return;
        if (roundsList.getValueIsAdjusting()) return;
        if (roundsList.getSelectedIndex() < 0) return;
        treeViewer.selectedRound = roundsList.getSelectedIndex();
        treeViewer.updateArea();
    }

    /**
     * Loads process tree, c source code and ll source code.
     *
     * @param dir directory with the recorded data.
     */
    private void load(String dir) {
        try {
            tree.load(dir);
            sourceLoader.load(dir);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(rootPanel, "Load has FAILED: " + e);
            rootPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        }
        treeViewer.load();
        sourceC.setSourceCodeLines();
        sourceLL.setSourceCodeLines();

        for (int i = 0; i < tree.rounds.size(); ++i)
            ((DefaultListModel<String>) roundsList.getModel()).addElement(tree.rounds.get(i));
        roundsList.setSelectedIndex(tree.roundCounter - 1);
        roundsList.ensureIndexIsVisible(tree.roundCounter - 1);
    }

    /**
     * Jumps to given tab and line corresponding to the node's execution state.
     *
     * @param node to jump to.
     * @param pane to jump to.
     */
    private void selectCodeLine(Node node, TabbedPane pane) {
        mainTabbedPane.setSelectedIndex(pane.ordinal());
        sourceC.selectCodeLine(node.getInfo().getContext().location().line());
        sourceLL.selectCodeLine(node.getInfo().getContext().location().assemblyLine());
    }

    /**
     * Shows panel with information about node context, constraints and memory.
     * Highlights lines of code given by node's execution state.
     *
     * @param node the node for which information is displayed.
     */
    private void displayNodeInfoPane(Node node) {
        contextViewer.displayContext(node.getInfo());
        constraintsViewer.displayConstraints(node.getInfo());
        memoryViewer.displayMemory(tree.memory.get(node.getMemoryId()));

        nodeInfoTabbedPane.setVisible(true);
        splitPane.setDividerLocation(0.7);
        selectCodeLine(node, TabbedPane.TREE_PANE);
    }

    /**
     * Performs action based on item selected in the right click popup menu.
     *
     * @param e the event to be processed.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        NodeAction action;
        try {
            action = NodeAction.parse(e.getActionCommand());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        Node node = (Node) rightClickMenu.getClientProperty("node");
        switch (action) {
            case NODE_INFO -> displayNodeInfoPane(node);
            case NODE_TO_C -> selectCodeLine(node, TabbedPane.C_PANE);
            case NODE_TO_LL -> selectCodeLine(node, TabbedPane.LL_PANE);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            if (nodeInfoTabbedPane.isVisible()) {
                nodeInfoTabbedPane.setVisible(false);
//                divider = splitPane.getDividerLocation();
            }
            sourceC.removeHighLight();
            sourceLL.removeHighLight();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Node node = treeViewer.onMouseClicked(e.getX(), e.getY());
        if (node != null && SwingUtilities.isRightMouseButton(e)) {
            rightClickMenu.putClientProperty("node", node);
            rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
