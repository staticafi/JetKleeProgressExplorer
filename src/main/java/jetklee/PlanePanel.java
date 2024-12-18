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
    private ExecutionState.Plane currentPlane;
    private boolean isColorful;
    private JCheckBox sortByOffsetCheckBox;
    private JPanel concretePanel;
    private JPanel symbolicPanel;
    private JPanel updatePanel;
    private final String[] CONCRETE_COLUMNS = {"index", "value", "isConcrete"};
    private final String[] SYMBOLIC_COLUMNS = {"index", "value"};

    public PlanePanel() {
        super(new BorderLayout());

        isColorful = false;

        sortByOffsetCheckBox = new JCheckBox("Sort by index");
        sortByOffsetCheckBox.setSelected(true);
        sortByOffsetCheckBox.addActionListener(new ActionListener() {
            // TODO showall
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTables(currentPlane, isColorful);
            }
        });

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(sortByOffsetCheckBox, BorderLayout.NORTH);

        concretePanel = new JPanel(new BorderLayout());
        symbolicPanel = new JPanel(new BorderLayout());
        updatePanel = new JPanel(new BorderLayout());

//        JSplitPane bytesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, concretePanel, symbolicPanel);
//        bytesSplitPane.setResizeWeight(0.5);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("ConcreteBytes", concretePanel);
        tabbedPane.addTab("SymbolicBytes", symbolicPanel);
        tabbedPane.addTab("Updates", updatePanel);

        this.add(controlPanel, BorderLayout.NORTH);
        this.add(tabbedPane, BorderLayout.CENTER);
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


    private void updateBytesTable(JPanel bytePanel, String[] byteColumns, boolean isConcrete, boolean isColorFul) {
        ExecutionState.ByteMap additions = new ExecutionState.ByteMap();
        ExecutionState.ByteMap deletions = new ExecutionState.ByteMap();
        ExecutionState.ByteMap maskAdditions = new ExecutionState.ByteMap();
        ExecutionState.ByteMap maskDeletions = new ExecutionState.ByteMap();

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
        byteRows.addAll(getByteRows(additions, isColorFul ? ADDITIONS_COLOR : BACKGROUND_COLOR, maskAdditions,
                isConcrete));
        byteRows.addAll(getByteRows(deletions, isColorFul ? DELETIONS_COLOR : BACKGROUND_COLOR, maskDeletions,
                isConcrete));

        if (sortByOffsetCheckBox.isSelected()) {
            sortByOffset(byteRows);
        }
        JTable byteTable = isConcrete ? createConcreteTable(byteRows, byteColumns) : createSymbolicTable(byteRows, byteColumns);

        bytePanel.removeAll();
        bytePanel.add(new JScrollPane(byteTable));
        bytePanel.revalidate();
        bytePanel.repaint();
    }

    private JTable createConcreteTable(ArrayList<TableRow> rows, String[] columns) {
        Object[][] data = new Object[rows.size()][columns.length];

        for (int i = 0; i < rows.size(); ++i) {
            TableRow row = rows.get(i);
            data[i][Column.INDEX.ordinal()] = row.getIndex();
            data[i][Column.VALUE.ordinal()] = row.getValue();
            data[i][Column.MASK.ordinal()] = row.getMask();
        }
        return createTable(data, columns, rows);
    }

    private JTable createTable(Object[][] data, String[] columns, ArrayList<TableRow> rows) {
        JTable table = new JTable(data, columns) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                Color rowColor = rows.get(row).getColor();
                component.setBackground(rowColor);
                return component;
            }
        };
        table.addMouseListener(createMouseAdapter(table));
        return table;
    }

    private JTable createSymbolicTable(ArrayList<TableRow> rows, String[] columns) {
        Object[][] data = new Object[rows.size()][columns.length];

        for (int i = 0; i < rows.size(); ++i) {
            TableRow row = rows.get(i);
            data[i][Column.INDEX.ordinal()] = row.getIndex();
            data[i][Column.VALUE.ordinal()] = row.getValue();
        }

        return createTable(data, columns, rows);
    }

    private MouseAdapter createMouseAdapter(JTable table) {
        return new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                Object value = table.getValueAt(row, column);
                showPopup(value.toString());
            }
        };
    }

    private ArrayList<TableRow> getByteRows(ExecutionState.ByteMap changes, Color color, ExecutionState.ByteMap mask,
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

    private ArrayList<TableRow> getUpdateRows(ExecutionState.Updates updates, Color color) {
        ArrayList<TableRow> updateRows = new ArrayList<>();
        for (Map.Entry<String, String> entry : updates) {
            updateRows.add(new TableRow(color, entry.getValue(), entry.getKey(),null, true));
        }
        return updateRows;
    }

    private void updateUpdatesTable(boolean isColorful) {
        ExecutionState.Updates updates = currentPlane == null ? new ExecutionState.Updates(): currentPlane.updates();

        ArrayList<TableRow> updateRows = getUpdateRows(updates, isColorful ? ADDITIONS_COLOR : BACKGROUND_COLOR);

        if (sortByOffsetCheckBox.isSelected()) {
            sortByOffset(updateRows);
        }

        JTable updateTable = createSymbolicTable(updateRows, SYMBOLIC_COLUMNS);

        updatePanel.removeAll();
        updatePanel.add(new JScrollPane(updateTable), BorderLayout.CENTER);
        updatePanel.revalidate();
        updatePanel.repaint();
    }

    public void updateTables(ExecutionState.Plane plane, boolean isColorful) {
        this.isColorful = isColorful;
        this.currentPlane = plane;
        updateBytesTable(concretePanel, CONCRETE_COLUMNS, true, isColorful);
        updateBytesTable(symbolicPanel, SYMBOLIC_COLUMNS, false, isColorful);
        updateUpdatesTable(isColorful);
    }

    private String findMask(int byteIndex, ExecutionState.ByteMap mask) {
        for (Map.Entry<String, ArrayList<Integer>> entry : mask.entrySet()) {
            if (entry.getValue().contains(byteIndex)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void showPopup(String value) {
        JFrame popup = new JFrame("Expression");
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
