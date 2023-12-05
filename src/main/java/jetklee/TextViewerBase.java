package jetklee;

import javax.swing.*;
import java.awt.*;

public abstract class TextViewerBase extends JPanel {
    protected JTextArea textArea;
    private JScrollPane scrollPane;
    public TextViewerBase() {
        super(new BorderLayout());
        textArea = new JTextArea();
        scrollPane = new JScrollPane(textArea);

        scrollPane.setWheelScrollingEnabled(true);
        textArea.setEditable(false);
        textArea.setText("");
        add(scrollPane, BorderLayout.CENTER);
    }
}
