package jetklee;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;

/**
 * General panel with text.
 */
public abstract class TextViewerBase extends JPanel {
    protected JTextArea textArea;
    private JScrollPane scrollPane;
    public TextViewerBase() {
        super(new BorderLayout());
        textArea = new JTextArea();
        scrollPane = new JScrollPane(textArea);

//        Font customFont = new Font("Arial", Font.PLAIN, 30);
//        textArea.setFont(customFont);

        scrollPane.setWheelScrollingEnabled(true);
        textArea.setEditable(false);
        textArea.setText("");
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Jumps to given line in the text panel.
     *
     * @param line to jump to.
     */
    protected void setLine(int line) {
        try {
            int y = (int) textArea.modelToView2D(textArea.getLineStartOffset(line - 1)).getY();
            Rectangle viewRect = new Rectangle(textArea.getVisibleRect());
            viewRect.y = Math.max(0, y - viewRect.height / 2);
            textArea.scrollRectToVisible(viewRect);
        } catch (BadLocationException e) {
            // Nothing to do.
        }
    }

    protected void highlightLine(int line, Color color) {
        try {
            int startOffset = textArea.getLineStartOffset(line - 1);
            int endOffset = textArea.getLineEndOffset(line - 1);

            Highlighter highlighter = textArea.getHighlighter();
            Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(color);

            highlighter.addHighlight(startOffset, endOffset, painter);
        } catch (BadLocationException e) {
            // Nothing to do.
        }
    }

    public void removeHighLight() {
        textArea.getHighlighter().removeAllHighlights();
    }
}
