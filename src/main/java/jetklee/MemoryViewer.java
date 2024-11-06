package jetklee;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Panel that displays memory of selected execution state
 */
public class MemoryViewer extends JPanel implements ListSelectionListener {
    public JButton showAllButton;
    private NodeMemory.Memory currentMemory;
    private boolean showAll = false;

    private JList<String> objectsList;
    private JScrollPane objectScrollPane;
    private JSplitPane objectStateSplitPane;
    private JSplitPane mainSplitPane;
    private PlanePanel segmentPanel;
    private PlanePanel offsetPanel;
    private JPanel objectInfoPanel;
    private JSplitPane planesSplitPane;
    private ArrayList<NodeMemory.ObjectState> objects = new ArrayList<>();

    public MemoryViewer() {
        super(new BorderLayout());
        showAllButton = new JButton("Show All");
        showAllButton.setPreferredSize(new Dimension(75, 25));
        showAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                assert currentState != null;
//
//                showAll = !showAll;
//                if (showAll) {
//                    showAllButton.setText("Hide");
//                    displayCompleteMemory(currentState);
//                }
//                else {
//                    showAllButton.setText("Show All");
//                    displayShortMemory(currentState);
//                }
            }
        });

//        this.add(showAllButton, BorderLayout.NORTH);

        segmentPanel = new PlanePanel();
        segmentPanel.setBorder(new TitledBorder("Segment"));
        offsetPanel = new PlanePanel();
        offsetPanel.setBorder(new TitledBorder("Offset"));
        objectInfoPanel = new JPanel(new BorderLayout());
        objectInfoPanel.setBorder(new TitledBorder("Object Info"));

        JPanel objectsPanel = new JPanel(new BorderLayout());
        JTextField entryField = new JTextField();
        // TODO "Enter object ID" hint

        objectsList = new JList<>(new DefaultListModel<>());
        objectsList.setSelectedIndex(0);
        objectsList.setBorder(new TitledBorder("Objects"));
        objectsList.addListSelectionListener(this);
        objectsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objectsList.setCellRenderer(new CustomListCellRenderer(objects, new ArrayList<>()));

        objectScrollPane = new JScrollPane(objectsList);
        objectsPanel.add(objectScrollPane, BorderLayout.CENTER);

        entryField.addActionListener(e -> {
            String input = entryField.getText();
            try {
                int number = Integer.parseInt(input);
                objectsList.setSelectedIndex(number);
            } catch (NumberFormatException ex) {
//                entryField.setText("Please enter a number");
            }
        });

        objectsPanel.add(entryField, BorderLayout.NORTH);

        planesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, segmentPanel, offsetPanel);
        planesSplitPane.setDividerLocation(0.5);
        planesSplitPane.setResizeWeight(0.5);

        objectStateSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, planesSplitPane, objectInfoPanel);
        objectStateSplitPane.setDividerLocation(0.4);
        objectStateSplitPane.setResizeWeight(0.4);

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, objectsPanel, objectStateSplitPane);
        mainSplitPane.setDividerLocation(0.2);
        mainSplitPane.setResizeWeight(0.0);

        this.add(mainSplitPane, BorderLayout.CENTER);
    }


    public void displayMemory(NodeMemory.Memory memory) {
        segmentPanel.updateTables(null);
        offsetPanel.updateTables(null);

        currentMemory = memory;

        // TODO vypisat, ze nebol najdeny JSON pre tento node (moze sa stat pri timeoute)dd
        if (currentMemory == null) {
            currentMemory = new NodeMemory.Memory(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        objects = new ArrayList<>();
        objects.addAll(currentMemory.additions());
        objects.addAll(currentMemory.changes());

        ((CustomListCellRenderer) objectsList.getCellRenderer()).updateObjectList(objects, currentMemory.deletions());
        ((DefaultListModel<String>) objectsList.getModel()).clear();

        for (NodeMemory.ObjectState object : objects)
            ((DefaultListModel<String>) objectsList.getModel()).addElement(String.valueOf(object.objID()));

        for (NodeMemory.Deletion deletion : currentMemory.deletions()) {
            ((DefaultListModel<String>) objectsList.getModel()).addElement(String.valueOf(deletion.objID()));
        }

        if (objects.isEmpty()) {
            return;
        }

        objectsList.setSelectedIndex(0);
        updatePlanes();
    }

    private void updatePlanes() {
        if (objectsList.getModel().getSize() == 0) {
            segmentPanel.updateTables(null);
            offsetPanel.updateTables(null);
            return;
        }

        int selected = Integer.parseInt(objectsList.getSelectedValue());

        boolean isDeletion = currentMemory.deletions().stream()
                .anyMatch(deletion -> deletion.objID() == selected);

        if (isDeletion) {
            segmentPanel.updateTables(null);
            offsetPanel.updateTables(null);
            handleDeletionClick(selected);
            return;
        }

        NodeMemory.ObjectState currentObjectState = objects.stream()
                .filter(obj -> obj.objID() == selected)
                .findFirst()
                .orElse(null);

        if (currentObjectState != null) {
            NodeMemory.Plane offsetPlane = currentObjectState.offsetPlane();
            NodeMemory.Plane segmentPlane = currentObjectState.segmentPlane();
            segmentPanel.updateTables(segmentPlane);
            offsetPanel.updateTables(offsetPlane);
        } else {
            segmentPanel.updateTables(null);
            offsetPanel.updateTables(null);
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() != objectsList) return;
        if (objectsList.getValueIsAdjusting()) return;
        if (objectsList.getSelectedIndex() < 0) return;

        // remove table headers for empty tables
        segmentPanel.updateTables(null);
        offsetPanel.updateTables(null);

        int selectedID = Integer.parseInt(objectsList.getSelectedValue());

        boolean isDeletion = currentMemory.deletions().stream()
                .anyMatch(deletion -> deletion.objID() == selectedID);

        if (isDeletion) {
            handleDeletionClick(selectedID);
        } else {
            updatePlanes();
        }
    }

    private void handleDeletionClick(int objID) {
        System.out.println("Deletion clicked: " + objID);
    }
}
