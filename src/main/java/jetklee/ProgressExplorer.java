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
    private SourceMapping sourceMapping;
    private SourceViewerC sourceC;
    private SourceViewerLL sourceLL;
    private JPanel rootPanel;
    private JList<String> roundsList;
    private JTabbedPane mainTabbedPane;
    private JTabbedPane leftTabbedPane;
    private JSplitPane splitPane;
    private JScrollPane roundScrollPane;
    private JSplitPane mainSplitPane;
    private JPanel treePanel;
    private JScrollPane treeScrollPane;
    private ConstraintsViewer constraintsViewer;
    private MemoryViewer memoryViewer;
    private ContextViewer contextViewer;
    private int divider;

    public ProgressExplorer() {
        tree = new Tree();
        treeViewer = new TreeViewer(tree);
        treeViewer.addMouseListener(this);

        sourceMapping = new SourceMapping();
        sourceC = new SourceViewerC(sourceMapping);
        sourceLL = new SourceViewerLL(sourceMapping);

        constraintsViewer = new ConstraintsViewer();
        memoryViewer = new MemoryViewer();
        contextViewer = new ContextViewer();
        divider = 1000;

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

        leftTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        leftTabbedPane.addTab("Context", contextViewer);
        leftTabbedPane.addTab("Constraints", constraintsViewer);
        leftTabbedPane.addTab("Memory", memoryViewer);
        leftTabbedPane.setVisible(false);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainTabbedPane, leftTabbedPane);
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, roundScrollPane, splitPane);
        mainSplitPane.setDividerLocation(50);

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(mainSplitPane, BorderLayout.CENTER);
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() != roundsList) return;
        if (roundsList.getValueIsAdjusting()) return;
        if (roundsList.getSelectedIndex() < 0) return;
        treeViewer.selectedRound = roundsList.getSelectedIndex();
        treeViewer.updateArea();
    }

    public void clear() {
        sourceMapping.clear();
        tree.clear();
        ((DefaultListModel<String>)roundsList.getModel()).clear();
        treeViewer.clear();
        sourceC.clear();
        sourceLL.clear();
    }

    private void load(String dir) {
        try{
            tree.load(Paths.get(dir));
            sourceMapping.load(dir);
        } catch(Exception e){
            JOptionPane.showMessageDialog(rootPanel, "Load has FAILED: " + e);
            clear();
            rootPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        }
        treeViewer.load();
        sourceC.load();
        sourceLL.load();

        for (int i = 0; i < tree.rounds.size(); ++i)
            ((DefaultListModel<String>)roundsList.getModel()).addElement(tree.rounds.get(i));
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("JetKlee: ProgressExplorer");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setPreferredSize(new Dimension(800, 600));

                ProgressExplorer explorer = new ProgressExplorer();
                if (args.length != ARGS_COUNT)
                    throw new IllegalArgumentException("Invalid number of arguments. Expected " + ARGS_COUNT + " arguments.");

                explorer.load(args[0]);

                frame.setContentPane(explorer.rootPanel);
                frame.pack();
                frame.setVisible(true);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
    }
    @Override
    public void mouseClicked(MouseEvent e) {
        Node node = treeViewer.onMouseClicked(e.getX(), e.getY());
        if (leftTabbedPane.isVisible())
            divider = splitPane.getDividerLocation();

        if (node != null){
            contextViewer.showContext(node.executionState);
            constraintsViewer.showConstraints(node.executionState);
            memoryViewer.showMemory(node.executionState);

            leftTabbedPane.setVisible(true);
            splitPane.setDividerLocation(divider);
        } else{
            leftTabbedPane.setVisible(false);
        }
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
}
