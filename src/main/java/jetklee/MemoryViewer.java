package jetklee;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
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
    private Node currentState;
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
                showAll = !showAll;
                if (showAll) {
                    showAllButton.setText("Hide");
                    displayCompleteMemory(currentState);
                }
                else {
                    showAllButton.setText("Show All");
                    displayShortMemory(currentState);
                }
            }
        });

        this.add(showAllButton, BorderLayout.NORTH);

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
        objectStateSplitPane.setDividerLocation(0.9);
        objectStateSplitPane.setResizeWeight(0.9);

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, objectsPanel, objectStateSplitPane);
        mainSplitPane.setDividerLocation(0.2);
        mainSplitPane.setResizeWeight(0.0);

        this.add(mainSplitPane, BorderLayout.CENTER);
    }

    private void displayTables(NodeMemory.Memory memory) {
        segmentPanel.updateTables(null);
        offsetPanel.updateTables(null);

        objects = new ArrayList<>();
        objects.addAll(memory.additions());
        objects.addAll(memory.changes());

        ((CustomListCellRenderer) objectsList.getCellRenderer()).updateObjectList(objects, memory.deletions());
        ((DefaultListModel<String>) objectsList.getModel()).clear();

        for (NodeMemory.ObjectState object : objects)
            ((DefaultListModel<String>) objectsList.getModel()).addElement(String.valueOf(object.objID()));

        for (NodeMemory.Deletion deletion : memory.deletions()) {
            ((DefaultListModel<String>) objectsList.getModel()).addElement(String.valueOf(deletion.objID()));
        }

        if (objects.isEmpty()) {
            return;
        }

        objectsList.setSelectedIndex(0);
        updatePlanes();
    }

    private void appendKeyValue(StyledDocument doc, String key, String value) {
        SimpleAttributeSet keyStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(keyStyle, Color.BLUE);
        StyleConstants.setBold(keyStyle, true);

        SimpleAttributeSet valueStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(valueStyle, Color.BLACK);

        try {
            doc.insertString(doc.getLength(), key + ": ", keyStyle);
            doc.insertString(doc.getLength(), value + ", ", valueStyle);
        } catch (BadLocationException e) {
//            throw new RuntimeException(e);
        }
    }

    private void displayObjectInfo() {
        objectInfoPanel.removeAll();

        if (objectsList.getSelectedIndex() < 0) {
            return;
        }

        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        objectInfoPanel.add(scrollPane, BorderLayout.CENTER);

        StyledDocument doc = textPane.getStyledDocument();

        int selected = Integer.parseInt(objectsList.getSelectedValue());

        boolean isDeletion = currentMemory.deletions().stream()
                .anyMatch(deletion -> deletion.objID() == selected);

        if (isDeletion) {
            handleDeletionClick(selected);
            return;
        }

        NodeMemory.ObjectState currentObjectState = objects.stream()
                .filter(obj -> obj.objID() == selected)
                .findFirst()
                .orElse(null);

        if (currentObjectState == null) {
            return;
        }

        appendKeyValue(doc, "ID", String.valueOf(currentObjectState.objID()));
        appendKeyValue(doc, "Segment", String.valueOf(currentObjectState.segment()));
        appendKeyValue(doc, "Name", currentObjectState.name());
        appendKeyValue(doc, "Size", currentObjectState.size());
        appendKeyValue(doc, "Local", String.valueOf(currentObjectState.isLocal()));
        appendKeyValue(doc, "Global", String.valueOf(currentObjectState.isGlobal()));
        appendKeyValue(doc, "Fixed", String.valueOf(currentObjectState.isFixed()));
        appendKeyValue(doc, "User Spec", String.valueOf(currentObjectState.isUserSpec()));
        appendKeyValue(doc, "Lazy", String.valueOf(currentObjectState.isLazy()));
        appendKeyValue(doc, "Symbolic Address", currentObjectState.symAddress());
        appendKeyValue(doc, "Copy-On-Write Owner", String.valueOf(currentObjectState.copyOnWriteOwner()));
        appendKeyValue(doc, "Read-Only", String.valueOf(currentObjectState.readOnly()));

        if (currentObjectState.segmentPlane() != null) {
            appendPlaneInfo(doc, "Segment Plane", currentObjectState.segmentPlane());
        }
        if (currentObjectState.offsetPlane() != null) {
            appendPlaneInfo(doc, "Offset Plane", currentObjectState.offsetPlane());
        }
    }

    private void appendPlaneInfo(StyledDocument doc, String planeType, NodeMemory.Plane plane) {
        assert plane != null;

        SimpleAttributeSet headerStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(headerStyle, Color.BLUE);
        StyleConstants.setBold(headerStyle, true);
        try {
            doc.insertString(doc.getLength(), "\n\n" + planeType + ":\n", headerStyle);
        } catch (BadLocationException e) {
//            e.printStackTrace();
        }

        appendKeyValue(doc, "Root Object", plane.rootObject());
        appendKeyValue(doc, "Size Bound", String.valueOf(plane.sizeBound()));
        appendKeyValue(doc, "Initialized", String.valueOf(plane.initialized()));
        appendKeyValue(doc, "Symbolic", String.valueOf(plane.symbolic()));
        appendKeyValue(doc, "Initial Value", String.valueOf(plane.initialValue()));
    }

    private void getCompleteMemory(Node node, NodeMemory.Memory memory) {
        if (node == null) {
            return;
        }

        getCompleteMemory(node.getParent(), memory);
    }

    private void displayCompleteMemory(Node node) {
        NodeMemory.Memory m = new NodeMemory.Memory(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        getCompleteMemory(node, m);
        displayTables(m);
        displayObjectInfo();
    }

    private void displayShortMemory(Node node) {
        displayTables(currentMemory);
        displayObjectInfo();
    }

    public void displayMemory(Node node) {
        currentState = node;
        currentMemory = node.getMemory().getMemory();

        if (currentMemory == null) {
            currentMemory = new NodeMemory.Memory(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        if (showAll) {
            displayCompleteMemory(node);
        } else {
            displayShortMemory(node);
        }
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
            displayObjectInfo();
            updatePlanes();
        }
    }

    private void handleDeletionClick(int objID) {
        System.out.println("Deletion clicked: " + objID);
    }
}
