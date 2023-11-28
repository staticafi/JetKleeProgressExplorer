package jetklee;

public class ConstraintsViewer extends TextViewerBase {
    public  ConstraintsViewer() {
        super();
    }

    public void showConstraints(ExecutionState executionState) {
        textArea.setText("");
        for (String constraint : executionState.constraints) {
            textArea.append(constraint + "\n");
        }
    }
}
