package jetklee;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.RTextScrollPane;

import static jetklee.Styles.CODE_FONT;
import static jetklee.Styles.CODE_FONT_SIZE;

/**
 * Displays C source code.
 */
public class SourceViewerC extends SourceViewerBase {
    private SourceLoader sourceLoader;
    private JLabel lineColumnLabel;

    /**
     * Initializes C source code panel and displays line and column label.
     *
     * @param sourceLoader loads code from source.c file
     */
    public SourceViewerC(SourceLoader sourceLoader) {
        super();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
        this.sourceLoader = sourceLoader;
        lineColumnLabel = new JLabel("Ln 1, Col 1");
        lineColumnLabel.setOpaque(true);
        lineColumnLabel.setFont(new Font(CODE_FONT, Font.PLAIN, CODE_FONT_SIZE));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(lineColumnLabel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);

        MouseAdapter ma = createMouseAdapter();
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

    private MouseAdapter createMouseAdapter() {
        return new MouseAdapter() {
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
                    if (column > numLineColumnChars) {
                        lineColumnLabel.setText("Ln " + (line + 1) + ", Col " + (column - numLineColumnChars));
                    }
                    lastIdx = idx;
                }
            }
        };
    }
}
