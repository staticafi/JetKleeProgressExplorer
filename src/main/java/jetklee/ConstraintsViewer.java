package jetklee;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ConstraintsViewer extends JPanel {

    private ArrayList<String> constraints;
    private JButton toggleButton;
    private JEditorPane editorPane;
    private boolean showRaw = false;
    private Set<String> labels;

    public ConstraintsViewer() {
        super();
        this.constraints = new ArrayList<>();
        this.labels = new HashSet<>();

        this.setLayout(new BorderLayout());

        editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        this.add(scrollPane, BorderLayout.CENTER);

        toggleButton = new JButton("Switch to Raw View");
        toggleButton.addActionListener(e -> toggleView());
        this.add(toggleButton, BorderLayout.NORTH);
    }

    public void displayConstraints(ArrayList<String> constraints) {
        this.constraints = constraints;
        StringBuilder content = new StringBuilder();
        labels.clear();

        for (String constraint : constraints) {
            if (showRaw) {
                content.append(constraint).append("<br><br>");
            } else {
                content.append(KQueryFormatter.formatConstraint(constraint)).append("<br>");
            }
        }

        editorPane.setText("<html><body>" + content + "</body></html>");
        editorPane.setCaretPosition(0);
    }

    private void toggleView() {
        showRaw = !showRaw;
        toggleButton.setText(showRaw ? "Switch to Formatted View" : "Switch to Raw View");
        displayConstraints(constraints);
    }
}