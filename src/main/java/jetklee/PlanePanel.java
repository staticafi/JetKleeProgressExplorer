package jetklee;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class PlanePanel extends JPanel {
    private NodeMemory.Plane currentPlane;
    private JCheckBox sortByOffsetCheckBox;
    private JPanel concretePanel;
    private JPanel symbolicPanel;
    private JPanel updatePanel;

    private static final Color RED_COLOR = new Color(255, 0, 0, 100);
    private static final Color GREEN_COLOR = new Color(34, 139, 34, 100);
    private final String[] concreteColumns = {"index", "value", "isConcrete"};
    private final String[] symbolicColumns = {"index", "value", "isSymbolic"};

    public PlanePanel() {
        super(new BorderLayout());

        sortByOffsetCheckBox = new JCheckBox("Sort by Offset");
        sortByOffsetCheckBox.setSelected(true);
        sortByOffsetCheckBox.addActionListener(e -> updateTables(currentPlane));

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

    private void updateBytesTable(NodeMemory.ByteMap additions, NodeMemory.ByteMap deletions, JPanel bytePanel, String[] byteColumns, boolean isConcrete) {
        if (additions.isEmpty() && deletions.isEmpty()) {
            clearTable(bytePanel);
            return;
        }

        ArrayList<Object[]> byteEntries = new ArrayList<>();
        ArrayList<Color> rowColors = new ArrayList<>();

        createByteRows(byteEntries, rowColors, additions, GREEN_COLOR, byteColumns, currentPlane.concreteMask().additions(), isConcrete);
        createByteRows(byteEntries, rowColors, deletions, RED_COLOR, byteColumns, currentPlane.concreteMask().additions(), isConcrete);

        if (sortByOffsetCheckBox.isSelected()) {
            ArrayList<Integer> indices = new ArrayList<>();
            for (Object[] entry : byteEntries) {
                indices.add((Integer) entry[0]);
            }

            ArrayList<Integer> sortedIndices = new ArrayList<>(indices);
            sortedIndices.sort(Integer::compareTo);

            ArrayList<Object[]> sortedByteEntries = new ArrayList<>();
            ArrayList<Color> sortedRowColors = new ArrayList<>();

            for (Integer sortedIndex : sortedIndices) {
                int originalPosition = indices.indexOf(sortedIndex);
                sortedByteEntries.add(byteEntries.get(originalPosition));
                sortedRowColors.add(rowColors.get(originalPosition));
                indices.set(originalPosition, null);
            }

            byteEntries = sortedByteEntries;
            rowColors = sortedRowColors;
        }

        Object[][] byteDataArray = new Object[byteEntries.size()][byteColumns.length];
        Color[] rowColorsArray = new Color[byteEntries.size()];

        for (int i = 0; i < byteEntries.size(); i++) {
            byteDataArray[i] = byteEntries.get(i);
            rowColorsArray[i] = rowColors.get(i);
        }

        JTable byteTable = new JTable(byteDataArray, byteColumns) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                c.setBackground(rowColorsArray[row]);
                return c;
            }

        };

        byteTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = byteTable.rowAtPoint(e.getPoint());
                int column = byteTable.columnAtPoint(e.getPoint());

                if (column == 1) {
                    Object value = byteTable.getValueAt(row, column);
                    boolean isSymbolic = byteTable.getValueAt(row, 2) == "false";
                    if (value != null && value != "" && isSymbolic) {
                        showPopup(value.toString(), "Value");
                    }
                }
            }
        });


        bytePanel.removeAll();
        bytePanel.add(new JScrollPane(byteTable));
        bytePanel.revalidate();
        bytePanel.repaint();
    }

    private void createByteRows(ArrayList<Object[]> byteEntries, ArrayList<Color> rowColors, NodeMemory.ByteMap deletions, Color redColor, String[] byteColumns, NodeMemory.ByteMap concreteMask, boolean isConcrete) {
        for (String key : deletions.keySet()) {
            ArrayList<Integer> indices = deletions.get(key);
            for (int index : indices) {
                Object[] byteData = new Object[byteColumns.length];
                byteData[0] = index;
                byteData[1] = key;
                String mask = findKeyForByteIndex(index, concreteMask);
                if (mask != null) {
                    if (isConcrete) {
                        byteData[2] = mask.equals("0") ? "false" : "true";
                    } else {
                        byteData[2] = mask.equals("0") ? "true" : "false";
                    }
                }
                byteEntries.add(byteData);
                rowColors.add(redColor);
            }
        }
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void updateUpdatesTable(NodeMemory.Plane plane) {
        assert plane != null;

        if (plane.updates().isEmpty()) {
            clearTable(updatePanel);
            return;
        }

        ArrayList<Object[]> updateEntries = new ArrayList<>();
        ArrayList<String> updates = new ArrayList<>(plane.updates().keySet());

        ArrayList<Object[]> numericUpdates = new ArrayList<>();
        ArrayList<Object[]> stringUpdates = new ArrayList<>();

        for (String key : updates) {
            Object[] updateEntry = new Object[2];
            updateEntry[0] = key;  // offset
            updateEntry[1] = plane.updates().get(key);  // value

            if (isNumeric(key)) {
                numericUpdates.add(updateEntry);
            } else {
                stringUpdates.add(updateEntry);
            }
        }

        if (sortByOffsetCheckBox.isSelected()) {
            numericUpdates.sort(Comparator.comparingInt(entry -> Integer.parseInt((String) entry[0])));
        }

        updateEntries.addAll(numericUpdates);
        updateEntries.addAll(stringUpdates);

        Object[][] updateDataArray = new Object[updateEntries.size()][2];
        for (int i = 0; i < updateEntries.size(); i++) {
            updateDataArray[i] = updateEntries.get(i);
        }

        JTable updateTable = getUpdateTable(updateDataArray);

        updatePanel.removeAll();
        updatePanel.add(new JScrollPane(updateTable), BorderLayout.CENTER);
        updatePanel.revalidate();
        updatePanel.repaint();
    }

    private JTable getUpdateTable(Object[][] updateDataArray) {
        JTable updateTable = new JTable(updateDataArray, new String[]{"Offset", "Value"});
        updateTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = updateTable.rowAtPoint(e.getPoint());
                int column = updateTable.columnAtPoint(e.getPoint());

                if (column == 0 || column == 1) {
                    Object value = updateTable.getValueAt(row, column);
                    if (value != null && value != "") {
                        String title = (column == 0) ? "Offset" : "Value";
                        showPopup(value.toString(), title);
                    }
                }
            }
        });

        return updateTable;
    }

    public void updateTables(NodeMemory.Plane plane) {
        if (plane == null) {
            clearTable(updatePanel);
            clearTable(concretePanel);
            clearTable(symbolicPanel);
            return;
        }

        this.currentPlane = plane;
        updateBytesTable(currentPlane.concreteStore().additions(), currentPlane.concreteStore().deletions(), concretePanel, concreteColumns, true);
        updateBytesTable(currentPlane.knownSymbolics().additions(), currentPlane.knownSymbolics().deletions(), symbolicPanel, symbolicColumns, false);
        updateUpdatesTable(currentPlane);
    }

    private String findKeyForByteIndex(int byteIndex, NodeMemory.ByteMap additions) {
        for (String key : additions.keySet()) {
            ArrayList<Integer> values = additions.get(key);

            if (values.contains(byteIndex)) {
                return key;
            }
        }
        return null;
    }

    private void showPopup(String value, String title) {
        JFrame popup = new JFrame(title);
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
