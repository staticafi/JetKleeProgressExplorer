package jetklee;

import java.awt.*;
import java.util.List;


public abstract class SourceViewerBase extends TextViewerBase{
    protected static final int textFontSize = 14;
    protected int numLineColumnChars;
    public SourceViewerBase() {
        super();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, textFontSize));
    }
    public void load() {
        int lineColumnSize = 1;
        for (int n = getSourceCodeLines().size(); n > 10; n /= 10)
            ++lineColumnSize;

        numLineColumnChars = lineColumnSize + 2;
        String lineColumnFormat =  "%" + lineColumnSize + "s";
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
    public abstract List<String> getSourceCodeLines();
}
