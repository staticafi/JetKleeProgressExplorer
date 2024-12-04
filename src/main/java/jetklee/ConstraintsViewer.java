package jetklee;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Panel that displays constraints of selected execution state.
 * Allows to switch between original and formatted view.
 */
public class ConstraintsViewer extends TextViewerBase {
    private boolean showOriginal;
    private ArrayList<String> constraints;
    private JButton toggleButton;
    private JEditorPane editorPane;
    private static final String ORIGINAL_STRING = "Show Original";
    private static final String FORMATTED_STRING = "Show Formatted";

    public ConstraintsViewer() {
        super();

        constraints = new ArrayList<>();
        showOriginal = false;

        editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        this.add(scrollPane, BorderLayout.CENTER);

        toggleButton = new JButton(ORIGINAL_STRING);
        toggleButton.addActionListener(e -> toggleView());
        this.add(toggleButton, BorderLayout.NORTH);
    }

    /**
     * Displays constraints (original or formatted) in the panel.
     *
     * @param constraints to display.
     */
    public void displayConstraints(ArrayList<String> constraints) {
        this.constraints = constraints;
        StringBuilder constraintsStr = new StringBuilder();

        for (String constraint : constraints) {
            if (showOriginal) {
                constraintsStr.append(constraint).append("<br><br>");
            } else {
                constraintsStr.append(KQueryFormatter.formatConstraint(constraint)).append("<br>");
            }
        }

        editorPane.setText("<html><body>" + constraintsStr + "</body></html>");
        editorPane.setCaretPosition(0);
    }

    private void toggleView() {
        showOriginal = !showOriginal;
        toggleButton.setText(showOriginal ? FORMATTED_STRING : ORIGINAL_STRING);
        displayConstraints(constraints);
    }
}