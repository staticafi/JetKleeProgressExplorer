package jetklee;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

public class ProgressExplorer implements ListSelectionListener {
    public static final int ARGS_COUNT = 1;
    public Tree tree;
    private TreeViewer treeViewer;
    private JPanel rootPanel;
    private JList<String> roundsList;

    public ProgressExplorer() {
        tree = new Tree();
        treeViewer = new TreeViewer(tree);

        rootPanel = new JPanel(new BorderLayout());
//        ListSelectionModel model = roundsList.getSelectionModel();

    }
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() != roundsList)
            return;
        if (roundsList.getValueIsAdjusting())
            return;
        if (roundsList.getSelectedIndex() < 0)
            return;
        treeViewer.selectedRound = roundsList.getSelectedIndex();
        treeViewer.repaint();
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
                    explorer.tree.loadFiles(args[0]);
                } catch (Exception e) {
                    System.out.println("File load failed: " + e);
                    return;
                }
                explorer.treeViewer.load();

                JScrollPane treeScrollPane = new JScrollPane(explorer.treeViewer);

                String[] roundsArray = explorer.tree.rounds.toArray(new String[0]);
                explorer.roundsList = new JList<>(roundsArray);
                explorer.roundsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                explorer.roundsList.addListSelectionListener(explorer);

                JScrollPane roundScrollPane = new JScrollPane(explorer.roundsList);

                JPanel treePanel = new JPanel(new BorderLayout());
                treePanel.add(treeScrollPane, BorderLayout.CENTER);

                JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, roundScrollPane, treePanel);
                splitPane.setDividerLocation(100);

                explorer.rootPanel.add(splitPane, BorderLayout.CENTER);
                frame.setContentPane(explorer.rootPanel);

                frame.pack();
                frame.setVisible(true);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
    }
}
