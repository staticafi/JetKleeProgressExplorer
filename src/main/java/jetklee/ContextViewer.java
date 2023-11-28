package jetklee;

public class ContextViewer extends TextViewerBase {
    public ContextViewer() {
        super();
    }

    public void showContext(ExecutionState executionState) {
        textArea.setText("");
//        textArea.append(node.other.toString());
        textArea.append(executionState.context.toString());
    }
}
