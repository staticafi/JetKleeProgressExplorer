package jetklee;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import static jetklee.CompleteMemoryRetriever.getCompleteMemory;
import static jetklee.CompleteMemoryRetriever.getDeletedObjectState;
import static jetklee.ObjectInfoViewer.displayObjectInfo;

/**
 * Panel that displays memory of selected execution state
 */
public class MemoryViewer extends JPanel implements ListSelectionListener {
    private boolean showAll;
    private int showAllSelection;
    private int shortSelection;
    private JButton showAllButton;
    private Node currentNode;
    private ExecutionState.Memory currentMemory;
    private SourceViewerLL sourceLL;
    private ObjectInfoViewer objectInfoViewer;
    private JList<String> objectsList;
    private JScrollPane objectScrollPane;
    private PlanePanel segmentPanel;
    private PlanePanel offsetPanel;
    private JPanel objectInfoPanel;
    private ArrayList<ExecutionState.ObjectState> objects;

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
        objectStateSplitPane.setDividerLocation(0.6);
        objectStateSplitPane.setResizeWeight(0.6);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, objectsPanel, objectStateSplitPane);
        mainSplitPane.setDividerLocation(0.6);
        mainSplitPane.setResizeWeight(0.1);

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
                if (showAll) {
                    showAllButton.setText("Hide");
                    shortSelection = objectsList.getSelectedIndex();
                    if (shortSelection < 0 || shortSelection >= objects.size()) {
                        shortSelection = 0;
                    }
                    currentMemory = getCompleteMemory(currentNode);
                    objectsList.setSelectedIndex(showAllSelection);
                } else {
                    showAllButton.setText("Show All");
                    showAllSelection = objectsList.getSelectedIndex();
                    if (showAllSelection < 0 || showAllSelection >= objects.size()) {
                        showAllSelection = 0;
                    }
                    currentMemory = currentNode.getExecutionState().getMemory();
                    objectsList.setSelectedIndex(shortSelection);
                }
                displayMemory(currentMemory);
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

        currentMemory = node.getExecutionState().getMemory();

        showAll = false;
        showAllButton.setText("Show All");

        displayMemory(currentMemory);
        objectsList.setSelectedIndex(0);
    }

    private void displayMemory(ExecutionState.Memory memory) {
        displayTables(memory);
        displayObjectInfo(objectsList, memory, objects, sourceLL, objectInfoPanel, currentNode);
    }

    private void displayTables(ExecutionState.Memory memory) {
        segmentPanel.updateTables(null, false);
        offsetPanel.updateTables(null, false);

        objects = new ArrayList<>(memory.additions());
        objects.addAll(memory.changes());

        for (ExecutionState.Deletion deletion : memory.deletions()) {
            objects.add(getDeletedObjectState(currentNode, deletion.objID()));
        }

//        int maxDigits = 0;
//        for (NodeMemory.ObjectState object : objects) {
//            int length = String.valueOf(object.objID()).length();
//            if (length > maxDigits) {
//                maxDigits = length;
//            }
//        }

        ArrayList<String> objectNames = new ArrayList<>();
        for (ExecutionState.ObjectState object : objects) {
            String objectName = Integer.toString(object.objID()); // Left-align with padding

            if (object.segmentPlane() != null) {
                objectName += " " + object.segmentPlane().rootObject();
            } else if (object.offsetPlane() != null) {
                objectName += " " + object.offsetPlane().rootObject();
            }
            objectNames.add(objectName);
        }
        DefaultListModel<String> model = (DefaultListModel<String>) objectsList.getModel();
        model.clear();
        model.addAll(objectNames);
        ((CustomListCellRenderer) objectsList.getCellRenderer()).updateObjectList(objects, memory.deletions(), showAll);


        if (showAll) {
            objectsList.setSelectedIndex(showAllSelection);
        } else {
            objectsList.setSelectedIndex(shortSelection);
        }
    }

    private void updatePlanes() {
        if (objectsList.getModel().getSize() == 0) {
            segmentPanel.updateTables(null, false);
            offsetPanel.updateTables(null, false);
            return;
        }

        int selected = Integer.parseInt(objectsList.getSelectedValue().split(" ")[0]);

        ExecutionState.ObjectState currentObjectState;
        currentObjectState = objects.stream()
                .filter(obj -> obj.objID() == selected)
                .findFirst()
                .orElse(null);

        boolean isDeletion = currentMemory.deletions().stream()
                .anyMatch(deletion -> deletion.objID() == selected);

        if (currentObjectState != null && !isDeletion) {
            ExecutionState.Plane offsetPlane = currentObjectState.offsetPlane();
            ExecutionState.Plane segmentPlane = currentObjectState.segmentPlane();

            segmentPanel.updateTables(segmentPlane, !showAll);
            offsetPanel.updateTables(offsetPlane, !showAll);
        } else {
            segmentPanel.updateTables(null, false);
            offsetPanel.updateTables(null, false);
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() != objectsList || objectsList.getValueIsAdjusting() || objectsList.getSelectedIndex() < 0 ) {
            return;
        }

        updatePlanes();
        displayObjectInfo(objectsList, currentNode.getExecutionState().getMemory(), objects, sourceLL, objectInfoPanel, currentNode);

    }
}
