package jetklee;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.io.File;
import java.util.prefs.Preferences;

//import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import static jetklee.Styles.DELETIONS_COLOR;

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
    private JTabbedPane nodeTabbedPane;
    private JSplitPane splitPane;
    private JScrollPane roundScrollPane;
    private JSplitPane mainSplitPane;
    private JPanel treePanel;
    private ConstraintsViewer constraintsViewer;
    private MemoryViewer memoryViewer;
    private ContextViewer contextViewer;
    private JPopupMenu rightClickMenu;
    private JMenuBar menuBar;

    public ProgressExplorer() {
        initializeSourceViewer();
        initliazeTreeViewer();
        initializeNodePanel();
        arrangePanels();
        createRightClickMenu();
        createOpenMenu();
    }

    private void createOpenMenu() {
        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });
        fileMenu.add(openMenuItem);
        menuBar.add(fileMenu);
    }

    private void createRightClickMenu() {
        rightClickMenu = new JPopupMenu();
        NodeAction[] nodeActions = new NodeAction[]{NodeAction.NODE_TO_C, NodeAction.NODE_TO_LL};
        for (NodeAction nodeAction : nodeActions) {
            JMenuItem newItem = new JMenuItem(nodeAction.value);
            newItem.addActionListener(this);
            newItem.setActionCommand(nodeAction.value);
            rightClickMenu.add(newItem);
        }
    }

    private void arrangePanels() {
        mainTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        mainTabbedPane.addTab("Tree", treePanel);
        mainTabbedPane.addTab("C", sourceC);
        mainTabbedPane.addTab("LL", sourceLL);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainTabbedPane, nodeTabbedPane);
        splitPane.setResizeWeight(0.1);
        splitPane.setDividerLocation(0.1);

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, roundScrollPane, splitPane);
        mainSplitPane.setResizeWeight(0.1);
        mainSplitPane.setDividerLocation(0.1);

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(mainSplitPane, BorderLayout.CENTER);
    }

    private void initializeSourceViewer() {
        sourceLoader = new SourceLoader();
        sourceC = new SourceViewerC(sourceLoader);
        sourceLL = new SourceViewerLL(sourceLoader);
    }

    private void initliazeTreeViewer() {
        tree = new Tree();
        treeViewer = new TreeViewer(tree);
        treeViewer.addMouseListener(this);

        JScrollPane treeScrollPane = new JScrollPane(treeViewer);
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
    }

    private void initializeNodePanel() {
        constraintsViewer = new ConstraintsViewer();
        memoryViewer = new MemoryViewer();
        contextViewer = new ContextViewer();

        nodeTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        nodeTabbedPane.addTab("Context", contextViewer);
        nodeTabbedPane.addTab("Constraints", constraintsViewer);
        nodeTabbedPane.addTab("Memory", memoryViewer);
        nodeTabbedPane.setVisible(false);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            /**
             * Initializes and displays the main application window. Loads recorded data from directory specified by
             * optional command line argument.
             */
            public void run() {
                String os = System.getProperty("os.name").toLowerCase();

                // LaF is causing problems on Linux
                if (os.contains("win")) {
                    FlatLightLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#62c1e5"));
                    try {
                        UIManager.setLookAndFeel(new FlatLightLaf());
                    } catch (Exception ex) {
                        System.err.println("Failed to initialize LaF");
                    }
                }

                JFrame frame = new JFrame("JetKlee Progress Explorer");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setPreferredSize(new Dimension(800, 600));

                ProgressExplorer explorer = new ProgressExplorer();
                if (args.length == ARGS_COUNT) {
                    explorer.load(Paths.get(args[0]).toAbsolutePath().toString());
                }
                if (args.length > ARGS_COUNT) {
                    throw new IllegalArgumentException("Invalid number of arguments. Expected " + ARGS_COUNT + " arguments.");
                }

                frame.setJMenuBar(explorer.menuBar);
                frame.setContentPane(explorer.rootPanel);
                try {
                    URL url = getClass().getResource("/icon.png");
                    frame.setIconImage(Toolkit.getDefaultToolkit().getImage(url));
                } catch (Exception e) {}

                frame.pack();
                frame.setVisible(true);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
    }

    private void openFile() {
        Preferences prefs = Preferences.userNodeForPackage(ProgressExplorer.class);
        String lastDirectory = prefs.get("lastDirectory", System.getProperty("user.home"));

        JFileChooser fileChooser = new JFileChooser(lastDirectory);
        fileChooser.setDialogTitle("Open Directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = fileChooser.showOpenDialog(rootPanel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            prefs.put("lastDirectory", selectedDir.getAbsolutePath());

            File recordingDir = new File(selectedDir, "__JetKleeProgressRecording__");
            load(recordingDir.getAbsolutePath());
        }
    }

    private enum TabbedPane {
        TREE_PANE, C_PANE, LL_PANE;
    }

    private enum NodeAction {
        NODE_INFO("Node Information"), NODE_TO_C("C"), NODE_TO_LL("LL");
        private final String value;

        NodeAction(String value) {
            this.value = value;
        }

        private static NodeAction parse(String actionStr) throws Exception {
            return switch (actionStr) {
                case "C" -> NODE_TO_C;
                case "LL" -> NODE_TO_LL;
                default -> throw new Exception("Unknown right click menu action: " + actionStr);
            };
        }
    }

    /**
     * Updates current tree view based on the round selected in the list menu.
     *
     * @param e the event that characterizes the change in the list menu.
     */
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() != roundsList || roundsList.getValueIsAdjusting() || roundsList.getSelectedIndex() < 0) {
            return;
        }
        treeViewer.setSelectedRound(roundsList.getSelectedIndex());
        treeViewer.updateArea();

        nodeTabbedPane.setVisible(treeViewer.isSelectedVisible());
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
            return;
        }
        nodeTabbedPane.setVisible(false);
        treeViewer.load();
        sourceC.setSourceCodeLines();
        sourceLL.setSourceCodeLines();

        DefaultListModel<String> model = (DefaultListModel<String>) roundsList.getModel();
        model.clear();

        for (int i = 0; i < tree.getRounds().size(); ++i) {
            model.addElement(tree.getRounds().get(i));
        }
        roundsList.setSelectedIndex(0);
        roundsList.ensureIndexIsVisible(0);
        roundsList.revalidate();
        roundsList.repaint();
    }

    /**
     * Jumps to given tab and line corresponding to the node's execution state.
     *
     * @param node to jump to.
     * @param pane to jump to.
     */
    private void selectCodeLine(Node node, TabbedPane pane) {
        mainTabbedPane.setSelectedIndex(pane.ordinal());
        sourceC.selectCodeLine(node.getExecutionState().getContext().insertContext().firstLocation().line());
        sourceC.highlightLine(node.getExecutionState().getContext().lastLocation().line(), DELETIONS_COLOR);
        sourceLL.selectCodeLine(node.getExecutionState().getContext().insertContext().firstLocation().assemblyLine());
        sourceLL.highlightLine(node.getExecutionState().getContext().lastLocation().assemblyLine(), DELETIONS_COLOR);
    }

    /**
     * Shows panel with information about node context, constraints and memory.
     * Highlights lines of code given by node's execution state.
     *
     * @param node the node for which information is displayed.
     */
    private void displayNodePane(Node node) {
        // Display node pane only if node has execution state
        // (if the execution was terminated early, the node may not have it)
        if (node.getExecutionState().getMemory() == null) {
            nodeTabbedPane.setVisible(false);
            return;
        }
        contextViewer.displayContext(node.getExecutionState().getContext());
        constraintsViewer.displayConstraints(node.getExecutionState().getConstraints());
        memoryViewer.setupAndDisplayMemory(node, sourceLL);

        nodeTabbedPane.setVisible(true);
        splitPane.setDividerLocation(0.5);
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
            case NODE_INFO -> displayNodePane(node);
            case NODE_TO_C -> selectCodeLine(node, TabbedPane.C_PANE);
            case NODE_TO_LL -> selectCodeLine(node, TabbedPane.LL_PANE);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Node node = treeViewer.onMouseClicked(e.getX(), e.getY());
        if (node == null && SwingUtilities.isLeftMouseButton(e)) {
            if (nodeTabbedPane.isVisible()) {
                nodeTabbedPane.setVisible(false);
                treeViewer.setSelectedNode(null);
            }
            sourceC.removeHighLight();
            sourceLL.removeHighLight();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Node node = treeViewer.onMouseClicked(e.getX(), e.getY());
        if (node != null) {
            if (SwingUtilities.isRightMouseButton(e)) {
                rightClickMenu.putClientProperty("node", node);
                rightClickMenu.show(e.getComponent(), e.getX(), e.getY());

                displayNodePane(node);
                treeViewer.setSelectedNode(node);
                treeViewer.repaint();
            }
            if (SwingUtilities.isLeftMouseButton(e)) {
                displayNodePane(node);
                treeViewer.setSelectedNode(node);
                treeViewer.repaint();
            }
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
