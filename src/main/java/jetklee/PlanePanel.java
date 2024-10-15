package jetklee;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

import static jetklee.TreeViewer.*;

public class PlanePanel extends JPanel {
    private JPanel bytePanel;
    private JPanel updatePanel;

    private final String[] byteColumns = {"offset", "value", "concrete", "knownSym", "unflushed"};

    public PlanePanel() {
        super(new BorderLayout());
        JTabbedPane offsetTabbedPane = new JTabbedPane(JTabbedPane.TOP);

        bytePanel = new JPanel(new BorderLayout());
//        Object[][] byteData = {};
//        JTable byteTable = new JTable(byteData, byteColumns);
//        JScrollPane scrollPane = new JScrollPane(byteTable);
//        bytePanel.add(scrollPane);

        updatePanel = new JPanel(new BorderLayout());

        offsetTabbedPane.addTab("Bytes", bytePanel);
        offsetTabbedPane.addTab("Updates", updatePanel);
        this.add(offsetTabbedPane, BorderLayout.CENTER);
    }

    private void updateBytesTable(ExecutionState.Plane plane) {
        // TODO bytes in consecutive order and save if byte is added or deleted

        if (plane == null) {
            bytePanel.removeAll();
            bytePanel.revalidate();
            bytePanel.repaint();
            return;
        }

        ArrayList<ExecutionState.ByteGroup> bytes = new ArrayList<>();
        bytes.addAll(plane.bytes().additions());
        bytes.addAll(plane.bytes().deletions());

        int totalSize = 0;
        for (ExecutionState.ByteGroup bg : bytes) {
            totalSize += bg.indices().size();
        }

        Object[][] byteData = new Object[totalSize][byteColumns.length];
        Color[] rowColors = new Color[totalSize];

        int row = 0;
        for (ExecutionState.ByteGroup bg : bytes) {
            for (int index : bg.indices()) {
                byteData[row][0] = index;
                byteData[row][1] = bg.value();
                byteData[row][2] = bg.concrete();
                byteData[row][3] = bg.knownSym();
                byteData[row][4] = bg.unflushed();

                rowColors[row] = bg.isAddition() ? GREEN_COLOR : RED_COLOR;
                ++row;
            }
        }

        JTable byteTable = new JTable(byteData, byteColumns) {

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                c.setBackground(rowColors[row]);
                return c;
            }
        };
        bytePanel.removeAll();
        bytePanel.add(new JScrollPane(byteTable));
        bytePanel.revalidate();
        bytePanel.repaint();
    }

    private void updateUpdatesTable(ExecutionState.Plane plane) {
        if (plane == null) {
            updatePanel.removeAll();
            updatePanel.revalidate();
            updatePanel.repaint();
            return;
        }

        Object[][] updateData = new Object[plane.updates().size()][2];
        Color[] rowColors = new Color[plane.updates().size()];

        int row = 0;
        for (Map.Entry<String, String> update : plane.updates().entrySet()) {
            updateData[row][0] = update.getKey();
            updateData[row][1] = update.getValue();

            rowColors[row] = GREEN_COLOR;
            ++row;
        }

        JTable updateTable = new JTable(updateData, new String[]{"offset", "value"}) {

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                c.setBackground(rowColors[row]);
                return c;
            }
        };
        updatePanel.removeAll();
        updatePanel.add(new JScrollPane(updateTable));
        updatePanel.revalidate();
        updatePanel.repaint();
    }

    public void updateTables(ExecutionState.Plane plane) {
        updateBytesTable(plane);
        updateUpdatesTable(plane);
    }
}
