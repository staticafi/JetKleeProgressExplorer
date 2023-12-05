package jetklee;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class SourceViewerC extends SourceViewerBase{
    private SourceMapping mapping;
    private JLabel lineColumnLabel;
    public SourceViewerC(SourceMapping sourceMapping_) {
        super();
        mapping = sourceMapping_;
        lineColumnLabel = new JLabel("Ln 1, Col 1");
        lineColumnLabel.setOpaque(true);
        lineColumnLabel.setFont( new Font("Monospaced", Font.PLAIN, textFontSize));

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
        return mapping.sourceC;
    }
    public void load() {
        super.load();
    }

    public void clear() {
        textArea.setText("");
        lineColumnLabel.setText("");
    }
}
