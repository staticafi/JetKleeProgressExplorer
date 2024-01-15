package jetklee;

import java.awt.*;
import java.util.List;

/**
 * Displays source code.
 */
public abstract class SourceViewerBase extends TextViewerBase {
    protected static final int textFontSize = 14;
    private static final Color HIGHLIGHT_COLOR = new Color(184, 207, 229);
    protected int numLineColumnChars;

    public SourceViewerBase() {
        super();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, textFontSize));
    }

    /**
     * Fills the text area with numbered source code lines.
     */
    public void setSourceCodeLines() {
        int lineColumnSize = 1;
        for (int n = getSourceCodeLines().size(); n > 10; n /= 10)
            ++lineColumnSize;

        numLineColumnChars = lineColumnSize + 2;
        String lineColumnFormat = "%" + lineColumnSize + "s";
        StringBuilder stringBuilder = new StringBuilder();

        int lineCount = 1;
        for (String line : getSourceCodeLines()) {
            String numberedLine = String.format(lineColumnFormat, lineCount) + ": " + line + "\n";
            stringBuilder.append(numberedLine);
            ++lineCount;
        }

        String wholeText = stringBuilder.toString();

        textArea.setText(wholeText);
        textArea.setCaretPosition(0);
    }

    /**
     * Highlights and scrolls to given line in the source code.
     *
     * @param line to highlight
     */
    public void selectCodeLine(int line) {
        removeHighLight();
        setLine(line);
        highlightLine(line, HIGHLIGHT_COLOR);
    }

    public abstract List<String> getSourceCodeLines();
}
