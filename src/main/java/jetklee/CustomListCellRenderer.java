package jetklee;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

import static jetklee.TreeViewer.GREEN_COLOR;
import static jetklee.TreeViewer.RED_COLOR;

public class CustomListCellRenderer extends DefaultListCellRenderer {
    private final List<ExecutionState.ObjectState> objects;

    public CustomListCellRenderer(List<ExecutionState.ObjectState> objects) {
        this.objects = objects;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        int objID = Integer.parseInt(value.toString());
        ExecutionState.ObjectState objectState = getObjectStateById(objID);

        if (objectState != null) {
            switch (objectState.type()) {
                case ADDITION:
                    label.setBackground(GREEN_COLOR);
                    break;
                case DELETION:
                    label.setBackground(RED_COLOR);
                    break;
                case CHANGE:
                    label.setBackground(Color.ORANGE);
                    break;
                default:
                    label.setBackground(Color.WHITE);
            }
        }

        if (isSelected) {
            label.setBorder(new LineBorder(Color.BLACK, 2));
        } else {
            label.setBorder(null);
        }

        label.setOpaque(true);
        return label;
    }

    private ExecutionState.ObjectState getObjectStateById(int objID) {
        for (ExecutionState.ObjectState object : objects) {
            if (object.objID() == objID) {
                return object;
            }
        }
        return null;
    }

    public void updateObjectList(List<ExecutionState.ObjectState> newObjects) {
        this.objects.clear();
        this.objects.addAll(newObjects);
    }
}