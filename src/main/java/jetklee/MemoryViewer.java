package jetklee;

/**
 * Panel that displays memory of selected execution state
 */
public class MemoryViewer extends TextViewerBase {
    public MemoryViewer() {
        super();
    }

    public void displayMemory(ExecutionState executionState) {
        textArea.setText("");
        textArea.append("Objects:\n");
        textArea.append(executionState.objectsDiff.toString());
        textArea.append("\n");

        textArea.append("\nObject States:\n");
        for (int i = 0; i < executionState.objectStates.size(); i++) {
            textArea.append(executionState.objectStates.get(i).toString());
            textArea.append("\n\n");
        }
        textArea.setCaretPosition(0);
    }
}
