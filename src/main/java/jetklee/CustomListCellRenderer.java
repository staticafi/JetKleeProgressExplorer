package jetklee;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

import static jetklee.TreeViewer.GREEN_COLOR;
import static jetklee.TreeViewer.RED_COLOR;

public class CustomListCellRenderer extends DefaultListCellRenderer {
    private final List<NodeMemory.ObjectState> objects;
    private final List<NodeMemory.Deletion> deletions;

    public CustomListCellRenderer(List<NodeMemory.ObjectState> objects, List<NodeMemory.Deletion> deletions) {
        this.objects = objects;
        this.deletions = deletions;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        int objID = Integer.parseInt(value.toString());
        NodeMemory.ObjectState objectState = getObjectStateById(objID);
        NodeMemory.Deletion deletionState = getDeletionById(objID);

        if (deletionState != null) {
            label.setBackground(RED_COLOR);
        } else if (objectState != null) {
            switch (objectState.type()) {
                case ADDITION:
                    label.setBackground(GREEN_COLOR);
                    break;
                case CHANGE:
                    label.setBackground(Color.ORANGE);
                    break;
                default:
                    label.setBackground(Color.WHITE);
            }
        } else {
            label.setBackground(Color.WHITE);
        }

        if (isSelected) {
            label.setBorder(new LineBorder(Color.BLACK, 2));
        } else {
            label.setBorder(null);
        }

        label.setOpaque(true);
        return label;
    }

    private NodeMemory.ObjectState getObjectStateById(int objID) {
        for (NodeMemory.ObjectState object : objects) {
            if (object.objID() == objID) {
                return object;
            }
        }
        return null;
    }

    private NodeMemory.Deletion getDeletionById(int objID) {
        for (NodeMemory.Deletion deletion : deletions) {
            if (deletion.objID() == objID) {
                return deletion;
            }
        }
        return null;
    }

    public void updateObjectList(List<NodeMemory.ObjectState> newObjects, List<NodeMemory.Deletion> newDeletions) {
        this.objects.clear();
        this.objects.addAll(newObjects);
        this.deletions.clear();
        this.deletions.addAll(newDeletions);
    }
}
