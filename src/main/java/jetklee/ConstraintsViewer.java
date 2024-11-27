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
                content.append(formatConstraint(constraint)).append("<br>");
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

    private String formatConstraint(String input) {
        StringBuilder formatted = new StringBuilder();
        int indentLevel = 0;
        boolean inFunction = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (c) {
                case ('('):
                    if (i != 0) {
                        formatted.append("<br>");
                    }
                    formatted
                            .append(indent(indentLevel))
                            .append(c)
                            .append("<b style='color:blue;'>");
                    inFunction = true;
                    indentLevel++;
                    break;
                case ('['):
                    indentLevel++;
                    if (i != 0) {
                        formatted.append("<br>");
                    }
                    formatted
                            .append(indent(indentLevel))
                            .append(c);
                    break;

                case ')':
                    indentLevel--;
                    if (inFunction) {
                        inFunction = false;
                        formatted.append(c).append("<br>");
                    } else {
                        formatted
                                .append(c)
                                .append("<br>")
                                .append(indent(indentLevel));
                    }
                    break;

                case ']':
                    indentLevel--;
                    formatted.append(c);
                    break;

                case ' ':
                    if (inFunction) {
                        if (i + 1 < input.length() && input.charAt(i + 1) != '(') {
                            formatted.append("<br>");
                            formatted.append(indent(indentLevel));
                        }
                        formatted.append("</b> ");
                        inFunction = false;
                    }
                    formatted.append(c);
                    break;

                default:
                    formatted.append(c);
                    break;
            }
        }

        return formatted.toString().trim();
    }

    private String indent(int level) {
        return "&nbsp;".repeat(level * 4);
    }
}