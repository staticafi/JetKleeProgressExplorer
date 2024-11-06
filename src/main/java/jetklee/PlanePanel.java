package jetklee;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

import static jetklee.TreeViewer.*;

public class PlanePanel extends JPanel {
    private NodeMemory.Plane currentPlane;
    private JCheckBox sortByOffsetCheckBox;
    private JPanel bytePanel;
    private JPanel updatePanel;

    private final String[] byteColumns = {"offset", "value", "concrete", "knownSym", "unflushed"};

    public PlanePanel() {
        super(new BorderLayout());
        JTabbedPane offsetTabbedPane = new JTabbedPane(JTabbedPane.TOP);

        sortByOffsetCheckBox = new JCheckBox("Sort by Offset");
        sortByOffsetCheckBox.setSelected(false);
        sortByOffsetCheckBox.addActionListener(e -> updateTables(currentPlane));

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(sortByOffsetCheckBox, BorderLayout.NORTH);

        bytePanel = new JPanel(new BorderLayout());

        updatePanel = new JPanel(new BorderLayout());

        offsetTabbedPane.addTab("Bytes", bytePanel);
        offsetTabbedPane.addTab("Updates", updatePanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, controlPanel, offsetTabbedPane);
        this.add(splitPane, BorderLayout.CENTER);
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

    private void updateBytesTable(NodeMemory.Plane plane) {
        assert plane != null;

        if (plane.bytes().additions().isEmpty() && plane.bytes().deletions().isEmpty()) {
            clearTable(bytePanel);
            return;
        }

        ArrayList<NodeMemory.ByteGroup> byteGroups = new ArrayList<>();
        byteGroups.addAll(plane.bytes().additions());
        byteGroups.addAll(plane.bytes().deletions());

        ArrayList<Object[]> byteEntries = new ArrayList<>();
        ArrayList<Color> rowColors = new ArrayList<>();

        for (NodeMemory.ByteGroup bg : byteGroups) {
            for (int index : bg.indices()) {
                Object[] byteData = new Object[byteColumns.length];
                byteData[0] = index;
                byteData[1] = bg.value();
                byteData[2] = bg.concrete();
                byteData[3] = bg.knownSym();
                byteData[4] = bg.unflushed();

                byteEntries.add(byteData);
                rowColors.add(bg.isAddition() ? GREEN_COLOR : RED_COLOR);
            }
        }

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

        bytePanel.removeAll();
        bytePanel.add(new JScrollPane(byteTable));
        bytePanel.revalidate();
        bytePanel.repaint();
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
            numericUpdates.sort(Comparator.comparingInt(entry -> Integer.parseInt((String) entry[0]))
            );
        }

        updateEntries.addAll(numericUpdates);
        updateEntries.addAll(stringUpdates);

        Object[][] updateDataArray = new Object[updateEntries.size()][2];
        for (int i = 0; i < updateEntries.size(); i++) {
            updateDataArray[i] = updateEntries.get(i);
        }

        JTable updateTable = new JTable(updateDataArray, new String[]{"Offset", "Value"});

        updatePanel.removeAll();
        updatePanel.add(new JScrollPane(updateTable), BorderLayout.CENTER);
        updatePanel.revalidate();
        updatePanel.repaint();
    }

    public void updateTables(NodeMemory.Plane plane) {
        if (plane == null) {
            clearTable(updatePanel);
            clearTable(bytePanel);
            return;
        }

        this.currentPlane = plane;
        updateBytesTable(currentPlane);
        updateUpdatesTable(currentPlane);
    }
}
