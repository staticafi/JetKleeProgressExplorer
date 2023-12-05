package jetklee;

import java.util.List;

public class SourceViewerLL extends SourceViewerBase{
    private SourceMapping mapping;
    public SourceViewerLL(SourceMapping sourceMapping_) {
        super();
        mapping = sourceMapping_;
    }
    @Override
    public List<String> getSourceCodeLines() {
        return mapping.sourceLL;
    }
    public void load() {
        super.load();
    }
    public void clear(){
        textArea.setText("");
    }
}
