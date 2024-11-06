package jetklee;

/**
 * Panel that displays constraints of selected execution state
 */
public class ConstraintsViewer extends TextViewerBase {
    public ConstraintsViewer() {
        super();
    }

    public void displayConstraints(NodeInfo nodeInfo) {
        textArea.setText("");
        for (String constraint : nodeInfo.getConstraints())
            textArea.append(constraint + "\n");
        textArea.setCaretPosition(0);
    }
}
