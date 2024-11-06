package jetklee;

/**
 * Panel that displays context of selected execution state
 */
public class ContextViewer extends TextViewerBase {
    public ContextViewer() {
        super();
    }

    public void displayContext(NodeInfo info) {
        textArea.setText("");
        textArea.append(info.getContext().toString());
        textArea.setCaretPosition(0);
    }
}
