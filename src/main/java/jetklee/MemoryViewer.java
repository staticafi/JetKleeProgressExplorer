package jetklee;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import static jetklee.CompleteMemoryRetriever.getCompleteMemory;

/**
 * Panel that displays memory of selected execution state
 */
public class MemoryViewer extends JPanel implements ListSelectionListener {
    private boolean showAll;
    private int showAllSelection;
    private int shortSelection;
    private JButton showAllButton;
    private Node currentNode;
    private SourceViewerLL sourceLL;

    private ObjectInfoViewer objectInfoViewer;
    private JList<String> objectsList;
    private JScrollPane objectScrollPane;
    private PlanePanel segmentPanel;
    private PlanePanel offsetPanel;
    private JPanel objectInfoPanel;
    private ArrayList<NodeMemory.ObjectState> objects;

    public MemoryViewer() {
        super(new BorderLayout());

        showAll = false;
        shortSelection = 0;
        showAllSelection = 0;
        objects = new ArrayList<>();

        showAllButton = createShowAllButton();
        this.add(showAllButton, BorderLayout.NORTH);

        objectsList = createObjectsList();
        objectScrollPane = new JScrollPane(objectsList);

        JTextField entryField = createEntryField();

        JPanel objectsPanel = createObjectsPanel();
        objectsPanel.add(entryField, BorderLayout.NORTH);

        JSplitPane mainSplitPane = createMainSplitPane(objectsPanel);
        this.add(mainSplitPane, BorderLayout.CENTER);

        objectInfoViewer = new ObjectInfoViewer(sourceLL, objectInfoPanel);
    }

    private JTextField createEntryField() {
        JTextField entryField = new JTextField();
        entryField.getDocument().addDocumentListener(new ObjectStateDocListener(entryField, objectsList));
        entryField.addActionListener(createActionListener(entryField));
        return entryField;
    }

    private JSplitPane createMainSplitPane(JPanel objectsPanel) {
        JTabbedPane planesTabbedPane = createSegmentOffsetPane();

        JSplitPane objectStateSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, planesTabbedPane, objectInfoPanel);
        objectStateSplitPane.setDividerLocation(0.9);
        objectStateSplitPane.setResizeWeight(0.9);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, objectsPanel, objectStateSplitPane);
        mainSplitPane.setDividerLocation(0.2);
        mainSplitPane.setResizeWeight(0.0);
        return mainSplitPane;
    }

    private JPanel createObjectsPanel() {
        objectInfoPanel = new JPanel(new BorderLayout());
        objectInfoPanel.setBorder(new TitledBorder("Object Info"));
        JPanel objectsPanel = new JPanel(new BorderLayout());
        objectsPanel.add(objectScrollPane, BorderLayout.CENTER);
        return objectsPanel;
    }

    private JList<String> createObjectsList() {
        JList<String> objectsList = new JList<>(new DefaultListModel<>());
        objectsList.setSelectedIndex(0);
        objectsList.setBorder(new TitledBorder("Objects"));
        objectsList.addListSelectionListener(this);
        objectsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objectsList.setCellRenderer(new CustomListCellRenderer(objects, new ArrayList<>(), showAll));

        return objectsList;
    }

    private ActionListener createActionListener(final JTextField entryField) {
        // Select the first item in the list when the user presses Enter
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String input = entryField.getText().toLowerCase();
                if (!input.isEmpty()) {
                    DefaultListModel<String> currentModel = (DefaultListModel<String>) objectsList.getModel();
                    if (!currentModel.isEmpty()) {
                        objectsList.setSelectedIndex(0);
                        objectsList.ensureIndexIsVisible(0);
                        objectsList.requestFocus();
                    }
                }
            }
        };
    }

    private JTabbedPane createSegmentOffsetPane() {
        JTabbedPane planesTabbedPane = new JTabbedPane(JTabbedPane.TOP);

        JPanel segmentTab = new JPanel(new BorderLayout());
        JPanel offsetTab = new JPanel(new BorderLayout());
        segmentPanel = new PlanePanel();
        offsetPanel = new PlanePanel();

        segmentTab.add(segmentPanel, BorderLayout.CENTER);
        offsetTab.add(offsetPanel, BorderLayout.CENTER);

        planesTabbedPane.addTab("Offset", offsetTab);
        planesTabbedPane.addTab("Segment", segmentTab);

        return planesTabbedPane;
    }

    private JButton createShowAllButton() {
        JButton showAllButton = new JButton("Show All");
        showAllButton.setPreferredSize(new Dimension(75, 25));
        showAllButton.addActionListener(createShowAllListener());
        return showAllButton;
    }

    private ActionListener createShowAllListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAll = !showAll;
                NodeMemory.Memory memory;
                if (showAll) {
                    showAllButton.setText("Hide");
                    shortSelection = objectsList.getSelectedIndex();
                    memory = getCompleteMemory(currentNode);
//                    objectsList.setSelectedIndex(showAllSelection);
                } else {
                    showAllButton.setText("Show All");
                    showAllSelection = objectsList.getSelectedIndex();
                    displayMemory(currentNode.getMemory().getMemory());
                    memory = currentNode.getMemory().getMemory();
//                    objectsList.setSelectedIndex(shortSelection);
                }
                displayMemory(memory);
            }
        };
    }

    /**
     * Sets up the memory viewer to display the memory of the given node.
     *
     * @param node     the node whose memory should be displayed
     * @param sourceLL the source viewer for the corresponding LLVM code
     */
    public void setupAndDisplayMemory(Node node, SourceViewerLL sourceLL) {
        this.sourceLL = sourceLL;
        currentNode = node;
//        objectsList.setSelectedIndex(0);

        NodeMemory.Memory memory = showAll ? getCompleteMemory(node) : node.getMemory().getMemory();
        displayMemory(memory);
    }

    private void displayMemory(NodeMemory.Memory memory) {
        displayTables(memory);
        objectInfoViewer.displayObjectInfo(objectsList, memory, objects);
    }

    private void displayTables(NodeMemory.Memory memory) {
        segmentPanel.updateTables(null, showAll);
        offsetPanel.updateTables(null, showAll);

        objects = new ArrayList<>();
        objects.addAll(memory.additions());
        objects.addAll(memory.changes());

        ArrayList<NodeMemory.ObjectState> deletions = new ArrayList<>();
        for (NodeMemory.Deletion deletion : memory.deletions()) {
            deletions.add(getDeletedObjectState(deletion.objID()));
        }

        objects.addAll(deletions);

        ((CustomListCellRenderer) objectsList.getCellRenderer()).updateObjectList(objects, memory.deletions(), showAll);
        ((DefaultListModel<String>) objectsList.getModel()).clear();

        int maxDigits = 0;
        for (NodeMemory.ObjectState object : objects) {
            int length = String.valueOf(object.objID()).length();
            if (length > maxDigits) {
                maxDigits = length;
            }
        }

        for (NodeMemory.ObjectState object : objects) {
            String objectName = String.format("%-" + maxDigits + "d", object.objID()); // Left-align with padding

            if (object.segmentPlane() != null) {
                objectName += " " + object.segmentPlane().rootObject();
            } else if (object.offsetPlane() != null) {
                objectName += " " + object.offsetPlane().rootObject();
            }

            ((DefaultListModel<String>) objectsList.getModel()).addElement(objectName);
        }

//        objectsList.setSelectedIndex(0);

//        if (showAll) {
//            objectsList.setSelectedIndex(showAllSelection);
//        } else {
//            objectsList.setSelectedIndex(shortSelection);
//        }
//        updatePlanes();
//        displayObjectInfo();
    }




    private void updatePlanes() {
        if (objectsList.getModel().getSize() == 0) {
            segmentPanel.updateTables(null, showAll);
            offsetPanel.updateTables(null, showAll);
            return;
        }

        int selected = Integer.parseInt(objectsList.getSelectedValue().split(" ")[0]);

        NodeMemory.ObjectState currentObjectState;
        currentObjectState = objects.stream()
                .filter(obj -> obj.objID() == selected)
                .findFirst()
                .orElse(null);

        if (currentObjectState != null) {
            NodeMemory.Plane offsetPlane = currentObjectState.offsetPlane();
            NodeMemory.Plane segmentPlane = currentObjectState.segmentPlane();
            segmentPanel.updateTables(segmentPlane, showAll);
            offsetPanel.updateTables(offsetPlane, showAll);
        } else {
            segmentPanel.updateTables(null, showAll);
            offsetPanel.updateTables(null, showAll);
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() != objectsList) return;
        if (objectsList.getValueIsAdjusting()) return;
        if (objectsList.getSelectedIndex() < 0) return;

        // remove table headers for empty tables
        segmentPanel.updateTables(null, showAll);
        offsetPanel.updateTables(null, showAll);

        updatePlanes();
        objectInfoViewer.displayObjectInfo(objectsList, currentNode.getMemory().getMemory(), objects);
    }

    private NodeMemory.ObjectState getDeletedObjectState(int objID) {
        Node current = currentNode;

        // search for the memory of the deleted object
        while (true) {
            NodeMemory.Memory memory = current.getMemory().getMemory();
            for (NodeMemory.ObjectState addition : memory.additions()) {
                if (addition.objID() == objID) {
                    return addition;
                }
            }
            for (NodeMemory.ObjectState change : memory.changes()) {
                if (change.objID() == objID) {
                    return change;
                }
            }
            assert current.getParent() != null; // the node which was deleted must have been created before
            current = current.getParent();
        }
    }
}
