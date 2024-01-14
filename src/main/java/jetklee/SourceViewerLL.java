package jetklee;

import java.util.List;

/**
 * Displays LL source code.
 */
public class SourceViewerLL extends SourceViewerBase {
    private SourceLoader sourceLoader;

    public SourceViewerLL(SourceLoader sourceLoader_) {
        super();
        sourceLoader = sourceLoader_;
    }

    @Override
    public List<String> getSourceCodeLines() {
        return sourceLoader.sourceLL;
    }

    public void setSourceCodeLines() {
        super.setSourceCodeLines();
    }
}
