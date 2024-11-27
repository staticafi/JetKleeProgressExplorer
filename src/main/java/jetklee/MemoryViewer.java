package jetklee;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import java.util.HashMap;

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
                } else {
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

        entryField.getDocument().addDocumentListener(new DocumentListener() {
            DefaultListModel<String> originalModel = (DefaultListModel<String>) objectsList.getModel();

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterList();
            }

            private void filterList() {
                String input = entryField.getText().toLowerCase();

                if (input.isEmpty()) {
                    objectsList.setModel(originalModel);
                    objectsList.clearSelection();
                    entryField.setBackground(Color.WHITE);
                    return;
                }

                DefaultListModel<String> filteredModel = new DefaultListModel<>();
                for (int i = 0; i < originalModel.getSize(); i++) {
                    String item = originalModel.getElementAt(i).toLowerCase();

                    if (item.contains(input)) {
                        filteredModel.addElement(originalModel.getElementAt(i));
                    }
                }

                objectsList.setModel(filteredModel);

                if (filteredModel.isEmpty()) {
                    entryField.setBackground(Color.PINK);
                } else {
                    entryField.setBackground(Color.WHITE);
                }
            }
        });

        entryField.addActionListener(e -> {
            String input = entryField.getText().toLowerCase();
            if (!input.isEmpty()) {
                DefaultListModel<String> currentModel = (DefaultListModel<String>) objectsList.getModel();
                if (!currentModel.isEmpty()) {
                    objectsList.setSelectedIndex(0);
                    objectsList.ensureIndexIsVisible(0);
                    objectsList.requestFocus();
                }
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

        int maxDigits = 0;
        for (NodeMemory.ObjectState object : objects) {
            int length = String.valueOf(object.objID()).length();
            if (length > maxDigits) {
                maxDigits = length;
            }
        }
        for (NodeMemory.Deletion deletion : memory.deletions()) {
            int length = String.valueOf(deletion.objID()).length();
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

        for (NodeMemory.Deletion deletion : memory.deletions()) {
            String deletionName = String.format("%-" + maxDigits + "d", deletion.objID());
            ((DefaultListModel<String>) objectsList.getModel()).addElement(deletionName);
        }

        if (objects.isEmpty()) {
            return;
        }

//        objectsList.setSelectedIndex(0);
//        updatePlanes();
//        displayObjectInfo();
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
            revalidate();
            repaint();
            return;
        }

        int selected = Integer.parseInt(objectsList.getSelectedValue().split(" ")[0]);

        boolean isDeletion = currentMemory.deletions().stream()
                .anyMatch(deletion -> deletion.objID() == selected);

        if (isDeletion) {
            return;
        }

        NodeMemory.ObjectState currentObjectState = objects.stream()
                .filter(obj -> obj.objID() == selected)
                .findFirst()
                .orElse(null);

        if (currentObjectState == null) {
            return;
        }

        StringBuilder htmlContent = new StringBuilder("<html><body style='font-family:Arial;padding:10px;'>");

        // Row 1: ID (Key and Value are bold)
        htmlContent.append("<b style='color:blue;'>objId:</b>").append(currentObjectState.objID()).append("<br>");

        // Row 2: Segment, Name, Size, Copy-On-Write Owner, Symbolic Address
        appendKeyValueInlineNonBold(htmlContent, "segment", currentObjectState.segment());
        appendKeyValueInlineNonBold(htmlContent, "name", currentObjectState.name());
        appendKeyValueInlineNonBold(htmlContent, "size", currentObjectState.size());
        appendKeyValueInlineNonBold(htmlContent, "copyOnWriteOwner", currentObjectState.copyOnWriteOwner());
        appendKeyValueInlineNonBold(htmlContent, "symbolicAddress", currentObjectState.symAddress());
        htmlContent.append("<br>");

        // Row 3: Local, Global, Fixed, User Spec, Lazy, Read-Only
        appendKeyValueInlineNonBold(htmlContent, "local", currentObjectState.isLocal());
        appendKeyValueInlineNonBold(htmlContent, "global", currentObjectState.isGlobal());
        appendKeyValueInlineNonBold(htmlContent, "fixed", currentObjectState.isFixed());
        appendKeyValueInlineNonBold(htmlContent, "userSpec", currentObjectState.isUserSpec());
        appendKeyValueInlineNonBold(htmlContent, "lazy", currentObjectState.isLazy());
        appendKeyValueInlineNonBold(htmlContent, "readOnly", currentObjectState.readOnly());
        htmlContent.append("<br>");
        htmlContent.append("<br>");

        if (currentObjectState.allocSite() != null) {
            htmlContent.append("<b style='color:blue;'>allocSite:</b><br>");
            NodeMemory.AllocSite allocSite = currentObjectState.allocSite();
            htmlContent.append(formatAllocSiteAsList(allocSite));
        }
        htmlContent.append("<br>");

        if (currentObjectState.offsetPlane() != null) {
            htmlContent.append("<b style='color:blue;'>offsetPlane</b>");
            htmlContent.append("<br>");
            appendPlaneDetailsHTML(htmlContent, currentObjectState.offsetPlane());
        }

        htmlContent.append("<br>");
        htmlContent.append("<br>");

        // Segment Plane
        if (currentObjectState.segmentPlane() != null) {
            htmlContent.append("<b style='color:blue;'>segmentPlane</b>");
            htmlContent.append("<br>");
            appendPlaneDetailsHTML(htmlContent, currentObjectState.segmentPlane());
        }

        htmlContent.append("</body></html>");

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText(htmlContent.toString());
        editorPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        objectInfoPanel.add(scrollPane, BorderLayout.CENTER);
        objectInfoPanel.revalidate();
        objectInfoPanel.repaint();
    }

    private String formatAllocSiteAsList(NodeMemory.AllocSite allocSite) {
        return "&nbsp;&nbsp;- <span style='color:blue;'>scope:</span> " + allocSite.scope() + "<br>"
                + "&nbsp;&nbsp;- <span style='color:blue;'>name:</span> " + allocSite.name() + "<br>"
                + "&nbsp;&nbsp;- <span style='color:blue;'>code:</span> " + allocSite.code() + "<br>";
    }


    private void appendKeyValueInlineNonBold(StringBuilder html, String key, Object value) {
        html.append("<span style='color:blue;'>").append(key).append(":</span> <span style='color:black;'>").append(value).append("</span>&nbsp;&nbsp;&nbsp;");
    }

    private void appendPlaneDetailsHTML(StringBuilder html, NodeMemory.Plane plane) {
        appendKeyValueInlineNonBold(html, "rootObject", plane.rootObject());
        appendKeyValueInlineNonBold(html, "initialValue", plane.initialValue());
        appendKeyValueInlineNonBold(html, "sizeBound", plane.sizeBound());
        appendKeyValueInlineNonBold(html, "initialized", plane.initialized());
        appendKeyValueInlineNonBold(html, "symbolic", plane.symbolic());
    }

    private static NodeMemory.ObjectState mergeObjectState(NodeMemory.ObjectState a, NodeMemory.ObjectState b) {
        NodeMemory.Plane mergedSegmentPlane = mergePlane(a.segmentPlane(), b.segmentPlane());
        NodeMemory.Plane mergedOffsetPlane = mergePlane(a.offsetPlane(), b.offsetPlane());

        return new NodeMemory.ObjectState(
                a.objID(), a.type(), a.segment(), a.name(), a.size(), a.isLocal(), a.isGlobal(),
                a.isFixed(), a.isUserSpec(), a.isLazy(), a.symAddress(), a.copyOnWriteOwner(),
                a.readOnly(), a.allocSite(), mergedSegmentPlane, mergedOffsetPlane);
    }

    private static NodeMemory.Plane mergePlane(NodeMemory.Plane a, NodeMemory.Plane b) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return new NodeMemory.Plane(
                a.type(), a.memoryObjectID(), a.rootObject(), a.sizeBound(), a.initialized(),
                a.symbolic(), a.initialValue(),
                mergeDiff(a.concreteStore(), b.concreteStore()),
                mergeDiff(a.concreteMask(), b.concreteMask()),
                mergeDiff(a.knownSymbolics(), b.knownSymbolics()),
                mergeUpdates(a.updates(), b.updates()));
    }

    private static NodeMemory.Updates mergeUpdates(NodeMemory.Updates a, NodeMemory.Updates b) {
        NodeMemory.Updates mergedUpdates = new NodeMemory.Updates();

        if (a != null) {
            mergedUpdates.putAll(a);
        }

        if (b != null) {
            mergedUpdates.putAll(b);
        }
        return mergedUpdates;
    }

    public static NodeMemory.Diff mergeDiff(NodeMemory.Diff a, NodeMemory.Diff b) {
        NodeMemory.ByteMap mergedByteMap = new NodeMemory.ByteMap();

        // Copy all entries from 'a' into 'mergedByteMap'
        a.additions().forEach((key, indices) -> {
            mergedByteMap.put(key, new ArrayList<>(indices));
        });

        // Delete all entries from 'b's deletions
        b.deletions().forEach((key, indices) -> {
            mergedByteMap.get(key).removeAll(indices);
        });

        // Remove all empty entries from 'mergedByteMap'
        mergedByteMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // Iterate over each addition in 'b' and merge it into 'mergedByteMap'
        b.additions().forEach((key, indices) -> {
            // If the key exists in 'mergedByteMap', merge the indices by adding all elements of 'indices' to 'mergedByteMap'
            mergedByteMap.merge(key, indices, (v1, v2) -> {
                v1.addAll(v2);
                return v1;
            });
        });

        return new NodeMemory.Diff(
                mergedByteMap,
                // The complete state does not contain deletions
                new NodeMemory.ByteMap());
    }

    private void displayCompleteMemory(Node node) {
        HashMap<Integer, NodeMemory.ObjectState> complete_memory = new HashMap<>();
        ArrayList<Node> nodes = new ArrayList<>();

        nodes.add(node);
        while (node.getParent() != null) {
            node = node.getParent();
            nodes.add(node);
        }
        // Now we have the nodes in order from leaf to root, traverse them in reverse order
        for (int i = nodes.size() - 1; i >= 0; i--) {
            NodeMemory.Memory node_memory = nodes.get(i).getMemory().getMemory();

            // Add newly added objects
            for (NodeMemory.ObjectState addition : node_memory.additions()) {
                complete_memory.put(addition.objID(), addition);
            }

            // Apply changes to changed objects
            for (NodeMemory.ObjectState change : node_memory.changes()) {
                NodeMemory.ObjectState oldObjectState = complete_memory.get(change.objID());
                complete_memory.put(change.objID(), mergeObjectState(oldObjectState, change));
            }

            // Remove deleted objects
            for (NodeMemory.Deletion deletion : node_memory.deletions()) {
                complete_memory.remove(deletion.objID());
            }
        }

        NodeMemory.Memory memory = new NodeMemory.Memory(new ArrayList<>(complete_memory.values()), new ArrayList<>(), new ArrayList<>());
        displayTables(memory);
        displayObjectInfo();
    }

    private void displayShortMemory(Node node) {
        displayTables(currentMemory);
        displayObjectInfo();
    }

    public void displayMemory(Node node) {
        showAllButton.setText("Show All");
        showAll = false;
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

        int selected = Integer.parseInt(objectsList.getSelectedValue().split(" ")[0]);

        boolean isDeletion = currentMemory.deletions().stream()
                .anyMatch(deletion -> deletion.objID() == selected);

        NodeMemory.ObjectState currentObjectState;
        if (isDeletion) {
            currentObjectState = getDeletedObjectState(selected);
        } else {
            currentObjectState = objects.stream()
                    .filter(obj -> obj.objID() == selected)
                    .findFirst()
                    .orElse(null);
        }

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

        updatePlanes();
        displayObjectInfo();
    }

    private NodeMemory.ObjectState getDeletedObjectState(int objID) {
        Node current = currentState;

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
