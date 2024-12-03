package jetklee;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ConstraintsViewer extends JPanel {
    private boolean showOriginal;
    private ArrayList<String> constraints;
    private JButton toggleButton;
    private JEditorPane editorPane;
    private static final String ORIGINAL_STRING = "Show Original";
    private static final String FORMATTED_STRING = "Show Formatted";

    public ConstraintsViewer() {
        super();
        this.setLayout(new BorderLayout());

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