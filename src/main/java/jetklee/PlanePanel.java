package jetklee;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.Map;

import static jetklee.Styles.*;

/**
 * Panel that displays memory plane of selected object state.
 */
public class PlanePanel extends JPanel {
    private NodeMemory.Plane currentPlane;
    private boolean showAll;
    private JCheckBox sortByOffsetCheckBox;
    private JPanel concretePanel;
    private JPanel symbolicPanel;
    private JPanel updatePanel;
    private final String[] CONCRETE_COLUMNS = {"index", "value", "isConcrete"};
    private final String[] SYMBOLIC_COLUMNS = {"index", "value", "isSymbolic"};
    private final String[] UPDATE_COLUMNS = {"index", "value"};

    public PlanePanel() {
        super(new BorderLayout());

        showAll = false;

        sortByOffsetCheckBox = new JCheckBox("Sort by Offset");
        sortByOffsetCheckBox.setSelected(true);
        sortByOffsetCheckBox.addActionListener(new ActionListener() {
            // TODO showall
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTables(currentPlane, showAll);
            }
        });

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(sortByOffsetCheckBox, BorderLayout.NORTH);

        concretePanel = new JPanel(new BorderLayout());
        symbolicPanel = new JPanel(new BorderLayout());
        updatePanel = new JPanel(new BorderLayout());

        JSplitPane bytesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, concretePanel, symbolicPanel);
        bytesSplitPane.setResizeWeight(0.5);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Bytes", bytesSplitPane);
        tabbedPane.addTab("Update", updatePanel);

        this.add(controlPanel, BorderLayout.NORTH);
        this.add(tabbedPane, BorderLayout.CENTER);
    }

    private void clearTable(JPanel panel) {
        panel.removeAll();
        JLabel emptyLabel = new JLabel("Empty");
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        panel.setLayout(new BorderLayout());
        panel.add(emptyLabel);
        panel.revalidate();
        panel.repaint();
    }

    private enum Column {
        INDEX, VALUE, MASK
    }

    // index can be either a number or a string (symbolic)
    private void sortByOffset(ArrayList<TableRow> rows) {
        rows.sort((row1, row2) -> {
            String index1 = row1.getIndex();
            String index2 = row2.getIndex();

            try {
                int num1 = Integer.parseInt(index1);
                int num2 = Integer.parseInt(index2);
                return Integer.compare(num1, num2);
            } catch (NumberFormatException e) {
                return index1.compareTo(index2);
            }
        });
    }


    private void updateBytesTable(JPanel bytePanel, String[] byteColumns, boolean isConcrete, boolean showAll) {
        NodeMemory.ByteMap additions = new NodeMemory.ByteMap();
        NodeMemory.ByteMap deletions = new NodeMemory.ByteMap();
        NodeMemory.ByteMap maskAdditions = new NodeMemory.ByteMap();
        NodeMemory.ByteMap maskDeletions = new NodeMemory.ByteMap();

        if (currentPlane != null) {
            maskAdditions = currentPlane.concreteMask().additions();
            maskDeletions = currentPlane.concreteMask().deletions();

            if (isConcrete) {
                additions = currentPlane.concreteStore().additions();
                deletions = currentPlane.concreteStore().deletions();
            } else {
                additions = currentPlane.knownSymbolics().additions();
                deletions = currentPlane.knownSymbolics().deletions();
            }
        }

        ArrayList<TableRow> byteRows = new ArrayList<>();
        byteRows.addAll(getByteRows(additions, showAll ? BACKGROUND_COLOR : ADDITIONS_COLOR, maskAdditions,
                isConcrete));
        byteRows.addAll(getByteRows(deletions, showAll ? BACKGROUND_COLOR : DELETIONS_COLOR, maskDeletions,
                isConcrete));

        if (sortByOffsetCheckBox.isSelected()) {
            sortByOffset(byteRows);
        }

        JTable byteTable = createBytesTable(byteRows, byteColumns, isConcrete);

        bytePanel.removeAll();
        bytePanel.add(new JScrollPane(byteTable));
        bytePanel.revalidate();
        bytePanel.repaint();
    }

    private JTable createBytesTable(ArrayList<TableRow> rows, String[] columns, boolean isConcrete) {
        Object[][] data = new Object[rows.size()][columns.length];

        for (int i = 0; i < rows.size(); ++i) {
            TableRow row = rows.get(i);
            data[i][Column.INDEX.ordinal()] = row.getIndex();
            data[i][Column.VALUE.ordinal()] = row.getValue();
            data[i][Column.MASK.ordinal()] = row.getMask();
        }
        return createTable(data, columns, isConcrete, rows);
    }

    private JTable createTable(Object[][] data, String[] columns, boolean isConcrete, ArrayList<TableRow> rows) {
        JTable table = new JTable(data, columns) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                Color rowColor = rows.get(row).getColor();
                component.setBackground(rowColor);
                return component;
            }
        };
        table.addMouseListener(createMouseAdapter(table, isConcrete));
        return table;
    }

    private JTable createUpdatesTable(ArrayList<TableRow> rows, String[] columns) {
        Object[][] data = new Object[rows.size()][columns.length];

        for (int i = 0; i < rows.size(); ++i) {
            TableRow row = rows.get(i);
            data[i][Column.INDEX.ordinal()] = row.getIndex();
            data[i][Column.VALUE.ordinal()] = row.getValue();
        }

        return createTable(data, columns, false, rows);
    }

    private MouseAdapter createMouseAdapter(JTable table, boolean isConcrete) {
        return new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());

                if (column == Column.VALUE.ordinal() && !isConcrete) {
                    Object value = table.getValueAt(row, column);
                    boolean isSymbolic = table.getValueAt(row, Column.MASK.ordinal()) == "true";
                    if (value != null && value != "" && isSymbolic) {
                        showPopup(value.toString());
                    }
                }
            }
        };
    }

    private ArrayList<TableRow> getByteRows(NodeMemory.ByteMap changes, Color color, NodeMemory.ByteMap mask,
                                            boolean isConcrete) {
        ArrayList<TableRow> byteRows = new ArrayList<>();

        for (String byteValue : changes.keySet()) {
            ArrayList<Integer> indices = changes.get(byteValue);
            for (int byteIndex : indices) {
                byteRows.add(new TableRow(color, Integer.toString(byteIndex), byteValue, findMask(byteIndex, mask), isConcrete));
            }
        }

        return byteRows;
    }

    private ArrayList<TableRow> getUpdateRows(NodeMemory.Updates updates) {
        ArrayList<TableRow> updateRows = new ArrayList<>();
        for (String key : updates.keySet()) {
            updateRows.add(new TableRow(BACKGROUND_COLOR, key, updates.get(key), null, true));
        }
        return updateRows;
    }

    private void updateUpdatesTable() {
        NodeMemory.Updates updates = currentPlane == null ? new NodeMemory.Updates() : currentPlane.updates();

        ArrayList<TableRow> updateRows = getUpdateRows(updates);

        if (sortByOffsetCheckBox.isSelected()) {
            sortByOffset(updateRows);
        }

        JTable updateTable = createUpdatesTable(updateRows, UPDATE_COLUMNS);

        updatePanel.removeAll();
        updatePanel.add(new JScrollPane(updateTable), BorderLayout.CENTER);
        updatePanel.revalidate();
        updatePanel.repaint();
    }

    public void updateTables(NodeMemory.Plane plane, boolean showAll) {
        this.showAll = showAll;
        this.currentPlane = plane;
        updateBytesTable(concretePanel, CONCRETE_COLUMNS, true, showAll);
        updateBytesTable(symbolicPanel, SYMBOLIC_COLUMNS, false, showAll);
        updateUpdatesTable();
    }

    private String findMask(int byteIndex, NodeMemory.ByteMap mask) {
        for (Map.Entry<String, ArrayList<Integer>> entry : mask.entrySet()) {
            if (entry.getValue().contains(byteIndex)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void showPopup(String value) {
        JFrame popup = new JFrame("Value");
        popup.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        popup.setSize(400, 300);
        popup.setResizable(true);

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText(KQueryFormatter.formatConstraint(value));
        editorPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        popup.add(scrollPane);

        popup.setLocationRelativeTo(this);
        popup.setVisible(true);
    }

}
