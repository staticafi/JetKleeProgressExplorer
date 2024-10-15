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
    private ExecutionState currentState = null;
    private boolean showAll = false;

    private JList<String> objectsList;
    private JScrollPane objectScrollPane;
    private JSplitPane objectStateSplitPane;
    private JSplitPane mainSplitPane;
    private PlanePanel segmentPanel;
    private PlanePanel offsetPanel;
    private JPanel objectInfoPanel;
    private JSplitPane planesSplitPane;
    private ArrayList<ExecutionState.ObjectState> objects = new ArrayList<>();

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
        objectsList.setCellRenderer(new CustomListCellRenderer(objects));

        objectScrollPane = new JScrollPane(objectsList);
        objectsPanel.add(objectScrollPane, BorderLayout.CENTER);

        // TODO Enttry field
//        entryField.addActionListener(e -> {
//            String input = entryField.getText();
//            try {
//                int number = Integer.parseInt(input);
//                objectsList.setSelectedIndex(number);
//            } catch (NumberFormatException ex) {
//                entryField.setText("Please enter a number");
//            }
//        });

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


    public void displayMemory(ExecutionState executionState) {
        currentState = executionState;

        objects = new ArrayList<>();
        objects.addAll(currentState.memory.additions());
        objects.addAll(currentState.memory.changes());

        ((CustomListCellRenderer) objectsList.getCellRenderer()).updateObjectList(objects);
        ((DefaultListModel<String>) objectsList.getModel()).clear();

        for (ExecutionState.ObjectState object : objects)
            ((DefaultListModel<String>) objectsList.getModel()).addElement(String.valueOf(object.objID()));

        for (ExecutionState.Deletion deletion : currentState.memory.deletions()) {
            ((DefaultListModel<String>) objectsList.getModel()).addElement(String.valueOf(deletion.objID()));
        }

        if (objects.isEmpty()) {
            segmentPanel.updateTables(null);
            offsetPanel.updateTables(null);
            return;
        }

        objectsList.setSelectedIndex(0);
        updatePlanes();
    }

    private void updatePlanes() {
        int selected = Integer.parseInt(objectsList.getSelectedValue());

        if (currentState.memory.deletions().contains(selected)) {
            segmentPanel.updateTables(null);
            offsetPanel.updateTables(null);
        }
        ExecutionState.ObjectState currentObjectState = objects.stream()
                .filter(obj -> obj.objID() == selected)
                .findFirst()
                .get();

        ExecutionState.Plane offsetPlane = currentObjectState.offsetPlane();
        ExecutionState.Plane segmentPlane = currentObjectState.segmentPlane();
        segmentPanel.updateTables(segmentPlane);
        offsetPanel.updateTables(offsetPlane);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() != objectsList) return;
        if (objectsList.getValueIsAdjusting()) return;
        if (objectsList.getSelectedIndex() < 0) return;

        updatePlanes();
    }
}
