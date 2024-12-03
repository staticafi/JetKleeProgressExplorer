package jetklee;

import java.util.List;

/**
 * Displays LL source code.
 */
public class SourceViewerLL extends SourceViewerBase {
    private SourceLoader sourceLoader;

    public SourceViewerLL(SourceLoader sourceLoader) {
        super();
        this.sourceLoader = sourceLoader;
    }

    @Override
    public List<String> getSourceCodeLines() {
        return sourceLoader.sourceLL;
    }

    public void setSourceCodeLines() {
        super.setSourceCodeLines();
    }

    /**
     * Finds the line number of a function or global variable in the .ll file with an exact name match.
     *
     * @param name The exact name of the function or global variable.
     * @return The line number where the function or global variable is defined, or -1 if not found.
     */
    public int findDefinitionLine(String name) {
        for (int i = 0; i < sourceLoader.sourceLL.size(); i++) {
            String line = sourceLoader.sourceLL.get(i).trim();

            if (line.startsWith("@") && line.contains(name)) {
                String[] parts = line.split(" ");
                if (parts.length > 0 && parts[0].equals("@" + name)) {
                    return i + 1;
                }
            }

            if ((line.startsWith("define") || line.startsWith("declare")) && line.contains(name)) {
                String functionName = extractFunctionName(line);
                if (functionName.equals("@" + name)) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private String extractFunctionName(String line) {
        int startIdx = line.indexOf("@");
        int endIdx = line.indexOf("(");

        if (startIdx != -1 && endIdx != -1) {
            return line.substring(startIdx, endIdx);
        }
        return "";
    }
}
