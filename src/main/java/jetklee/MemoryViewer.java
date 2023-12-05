package jetklee;

public class MemoryViewer extends TextViewerBase {
    public MemoryViewer() {
        super();
    }

    public void showMemory(ExecutionState executionState) {
        textArea.setText("");
        textArea.append("Objects:\n");
        for (int i = 0; i < executionState.objects.size(); i++) {
            textArea.append(executionState.objects.get(i).toString());
            textArea.append("\n");
        }

        textArea.append("\nObject States:\n");
        for (int i = 0; i < executionState.objectStates.size(); i++) {
            textArea.append(executionState.objectStates.get(i).toString());
            textArea.append("\n\n");
        }
        textArea.setCaretPosition(0);
    }
}
