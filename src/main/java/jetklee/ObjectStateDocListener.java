package jetklee;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static jetklee.Styles.BACKGROUND_COLOR;
import static jetklee.Styles.DELETIONS_COLOR;

/**
 * A custom DocumentListener that filters a list based on user input in a JTextField.
 */
public class ObjectStateDocListener implements DocumentListener {
    private JTextField entryField;
    private JList<String> objectsList;
    private DefaultListModel<String> originalModel;

    public ObjectStateDocListener(JTextField entryField, JList<String> objectsList) {
        this.entryField = entryField;
        this.objectsList = objectsList;
        this.originalModel = (DefaultListModel<String>) objectsList.getModel();
    }

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

    /**
     * Filters the list based on the text entered in the JTextField.
     */
    private void filterList() {
        String input = entryField.getText().toLowerCase();

        if (input.isEmpty()) {
            objectsList.setModel(originalModel);
            objectsList.clearSelection();
            entryField.setBackground(BACKGROUND_COLOR);
            return;
        }

        // Find matching object names
        DefaultListModel<String> filteredModel = new DefaultListModel<>();
        for (int i = 0; i < originalModel.getSize(); i++) {
            String item = originalModel.getElementAt(i).toLowerCase();
            if (item.contains(input)) {
                filteredModel.addElement(originalModel.getElementAt(i));
            }
        }

        objectsList.setModel(filteredModel);

        if (filteredModel.isEmpty()) {
            entryField.setBackground(DELETIONS_COLOR);
        } else {
            entryField.setBackground(BACKGROUND_COLOR);
        }
    }
}
