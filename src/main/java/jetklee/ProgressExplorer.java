package jetklee;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.nio.file.Paths;

public class ProgressExplorer implements ListSelectionListener, MouseWheelListener, MouseListener {
    private static final int ARGS_COUNT = 1;
    private Tree tree;
    private TreeViewer treeViewer;
    private JPanel rootPanel;
    private JList<String> roundsList;
    private ConstraintsViewer constraintsViewer;
    private MemoryViewer memoryViewer;
    private ContextViewer contextViewer;
    private int divider;

    public ProgressExplorer() {
        tree = new Tree();
        treeViewer = new TreeViewer(tree);
        rootPanel = new JPanel(new BorderLayout());
        constraintsViewer = new ConstraintsViewer();
        memoryViewer = new MemoryViewer();
        contextViewer = new ContextViewer();
        divider = 800;
//        ListSelectionModel model = roundsList.getSelectionModel();
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() != roundsList) return;
        if (roundsList.getValueIsAdjusting()) return;
        if (roundsList.getSelectedIndex() < 0) return;
        treeViewer.selectedRound = roundsList.getSelectedIndex();
        treeViewer.updateArea();
    }

    private void showNodeInformation(Node node) {
        contextViewer.showContext(node.executionState);
        constraintsViewer.showConstraints(node.executionState);
        memoryViewer.showMemory(node.executionState);

        constraintsViewer.textArea.setCaretPosition(0);
        memoryViewer.textArea.setCaretPosition(0);
        contextViewer.textArea.setCaretPosition(0);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("JetKlee: ProgressExplorer");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setPreferredSize(new Dimension(800, 600));

                ProgressExplorer explorer = new ProgressExplorer();
                if (args.length != ARGS_COUNT) {
                    throw new IllegalArgumentException("Invalid number of arguments. Expected " + ARGS_COUNT + " arguments.");
                }

                try {
                    explorer.tree.loadFiles(Paths.get(args[0]));
                } catch (Exception e) {
                    System.out.println("File load failed: " + e);
                    return;
                }
                explorer.treeViewer.load();

                JScrollPane treeScrollPane = new JScrollPane(explorer.treeViewer);
                treeScrollPane.setWheelScrollingEnabled(false);
                treeScrollPane.addMouseWheelListener(new MouseWheelListener() {
                    @Override
                    public void mouseWheelMoved(MouseWheelEvent e) {
                        explorer.treeViewer.onZoomChanged(-e.getWheelRotation());
                    }
                });

                String[] roundsArray = explorer.tree.rounds.toArray(new String[0]);
                explorer.roundsList = new JList<>(roundsArray);
                explorer.roundsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                explorer.roundsList.addListSelectionListener(explorer);

                JScrollPane roundScrollPane = new JScrollPane(explorer.roundsList);

                JPanel treePanel = new JPanel(new BorderLayout());
                treePanel.add(treeScrollPane, BorderLayout.CENTER);

                JPanel sourceC = new JPanel(new BorderLayout());
                JPanel sourceLL = new JPanel(new BorderLayout());

                JTabbedPane mainTabbedPane = new JTabbedPane(JTabbedPane.TOP);
                mainTabbedPane.addTab("Tree", treePanel);
                mainTabbedPane.addTab("C", sourceC);
                mainTabbedPane.addTab("LL", sourceLL);

                JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
                tabbedPane.addTab("Context", explorer.contextViewer);
                tabbedPane.addTab("Constraints", explorer.constraintsViewer);
                tabbedPane.addTab("Memory", explorer.memoryViewer);

                tabbedPane.setVisible(false);

                JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainTabbedPane, tabbedPane);

                JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, roundScrollPane, splitPane);
                mainSplitPane.setDividerLocation(100);

                explorer.rootPanel.add(mainSplitPane, BorderLayout.CENTER);
                explorer.treeViewer.addMouseListener(new MouseListener() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        Node node = explorer.treeViewer.onMouseClicked(e.getX(), e.getY());
                        if (tabbedPane.isVisible())
                            explorer.divider = splitPane.getDividerLocation();

                        if (node != null){
                            explorer.showNodeInformation(node);
                            tabbedPane.setVisible(true);
                            splitPane.setDividerLocation(explorer.divider);
                        } else{
                            tabbedPane.setVisible(false);
                        }
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {

                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {

                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {

                    }

                    @Override
                    public void mouseExited(MouseEvent e) {

                    }
                });

                frame.setContentPane(explorer.rootPanel);
                frame.pack();
                frame.setVisible(true);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
