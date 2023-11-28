package jetklee;

import javax.swing.*;
import java.awt.*;

public abstract class TextViewerBase extends JPanel {
    protected JTextArea textArea;
    protected JScrollPane scrollPane;
    public TextViewerBase() {
        super(new BorderLayout());
        textArea = new JTextArea();
        scrollPane = new JScrollPane(textArea);

        scrollPane.setWheelScrollingEnabled(true);
        textArea.setEditable(false);
        add(scrollPane, BorderLayout.CENTER);
    }
}
