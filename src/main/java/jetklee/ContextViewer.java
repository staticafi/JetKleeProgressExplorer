package jetklee;

/**
 * Panel that displays context of selected execution state
 */
public class ContextViewer extends TextViewerBase {
    public ContextViewer() {
        super();
    }

    public void displayContext(ExecutionState executionState) {
        textArea.setText("");
        textArea.append(executionState.context.toString());
        textArea.setCaretPosition(0);
    }
}
