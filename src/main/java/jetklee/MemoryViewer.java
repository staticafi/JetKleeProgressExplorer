package jetklee;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel that displays memory of selected execution state
 */
public class MemoryViewer extends TextViewerBase {
    public JButton showAllButton;
    private ExecutionState currentState = null;
    public MemoryViewer() {
        super();
//        showAllButton = new JButton("Show All");
//        showAllButton.setPreferredSize(new Dimension(75, 25));
//
//        showAllButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                showAllButton.setText("Hide");
//                System.out.println("Button was clicked!");
//
//                if (currentState != null){
//                    currentState.getCompleteMemory();
//                    displayMemory(currentState);
//                }
//            }
//        });
//
//        this.add(showAllButton, BorderLayout.NORTH);
    }

    public void displayMemory(ExecutionState executionState) {
        currentState = executionState;

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
