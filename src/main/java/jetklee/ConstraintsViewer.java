package jetklee;

/**
 * Panel that displays constraints of selected execution state
 */
public class ConstraintsViewer extends TextViewerBase {
    public ConstraintsViewer() {
        super();
    }

    public void displayConstraints(ExecutionState executionState) {
        textArea.setText("");
        for (String constraint : executionState.constraints)
            textArea.append(constraint + "\n");
        textArea.setCaretPosition(0);
    }
}
