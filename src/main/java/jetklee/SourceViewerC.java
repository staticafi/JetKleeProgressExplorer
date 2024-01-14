package jetklee;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Displays C source code.
 */
public class SourceViewerC extends SourceViewerBase {
    private SourceLoader sourceLoader;
    private JLabel lineColumnLabel;

    /**
     * Initializes C source code panel and displays line and column label.
     * @param sourceLoader_ loads code from source.c file
     */

    public SourceViewerC(SourceLoader sourceLoader_) {
        super();
        sourceLoader = sourceLoader_;
        lineColumnLabel = new JLabel("Ln 1, Col 1");
        lineColumnLabel.setOpaque(true);
        lineColumnLabel.setFont(new Font("Monospaced", Font.PLAIN, textFontSize));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(lineColumnLabel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);

        MouseAdapter ma = new MouseAdapter() {
            private int lastIdx = -1;

            @Override
            public void mouseMoved(MouseEvent e) {
                int idx = textArea.viewToModel2D(e.getPoint());
                if (idx != -1 && idx != lastIdx) {
                    int line, column;
                    try {
                        line = textArea.getLineOfOffset(idx);
                        column = idx - textArea.getLineStartOffset(line);
                    } catch (BadLocationException ex) {
                        return;
                    }
                    if (column > numLineColumnChars)
                        lineColumnLabel.setText("Ln " + (line + 1) + ", Col " + (column - numLineColumnChars));
                    lastIdx = idx;
                }
            }
        };
        textArea.addMouseListener(ma);
        textArea.addMouseMotionListener(ma);
    }

    @Override
    public List<String> getSourceCodeLines() {
        return sourceLoader.sourceC;
    }

    public void setSourceCodeLines() {
        super.setSourceCodeLines();
    }
}
