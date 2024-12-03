package jetklee;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ObjectInfoViewer {
    private final SourceViewerLL sourceLL;
    private final JPanel objectInfoPanel;

    public ObjectInfoViewer(SourceViewerLL sourceLL, JPanel objectInfoPanel) {
        this.sourceLL = sourceLL;
        this.objectInfoPanel = objectInfoPanel;
    }
    public void displayObjectInfo(JList<String> objectsList, NodeMemory.Memory memory, ArrayList<NodeMemory.ObjectState> objects) {
        objectInfoPanel.removeAll();

        if (objectsList.getSelectedIndex() < 0) {
            objectInfoPanel.revalidate();
            objectInfoPanel.repaint();
            return;
        }

        int selected = Integer.parseInt(objectsList.getSelectedValue().split(" ")[0]);

        boolean isDeletion = memory.deletions().stream()
                .anyMatch(deletion -> deletion.objID() == selected);

        if (isDeletion) {
            return;
        }

        NodeMemory.ObjectState currentObjectState = objects.stream()
                .filter(obj -> obj.objID() == selected)
                .findFirst()
                .orElse(null);

        if (currentObjectState == null) {
            return;
        }

        StringBuilder htmlContent = new StringBuilder("<html><body style='font-family:Arial;padding:10px;'>");

        // Row 1: ID (Key and Value are bold)
        htmlContent.append("<b style='color:blue;'>objId: </b>").append(currentObjectState.objID()).append("<br>");

        // Row 2: Segment, Name, Size, Copy-On-Write Owner, Symbolic Address
        appendKeyValueInlineNonBold(htmlContent, "segment", currentObjectState.segment());
        appendKeyValueInlineNonBold(htmlContent, "name", currentObjectState.name());
        appendKeyValueInlineNonBold(htmlContent, "size", currentObjectState.size());
        appendKeyValueInlineNonBold(htmlContent, "copyOnWriteOwner", currentObjectState.copyOnWriteOwner());
        appendKeyValueInlineNonBold(htmlContent, "symbolicAddress", currentObjectState.symAddress());
        htmlContent.append("<br>");

        // Row 3: Local, Global, Fixed, User Spec, Lazy, Read-Only
        appendKeyValueInlineNonBold(htmlContent, "local", currentObjectState.isLocal());
        appendKeyValueInlineNonBold(htmlContent, "global", currentObjectState.isGlobal());
        appendKeyValueInlineNonBold(htmlContent, "fixed", currentObjectState.isFixed());
        appendKeyValueInlineNonBold(htmlContent, "userSpec", currentObjectState.isUserSpec());
        appendKeyValueInlineNonBold(htmlContent, "lazy", currentObjectState.isLazy());
        appendKeyValueInlineNonBold(htmlContent, "readOnly", currentObjectState.readOnly());
        htmlContent.append("<br>");
        htmlContent.append("<br>");

        if (currentObjectState.allocSite() != null) {
            htmlContent.append("<b style='color:blue;'>allocSite:</b><br>");
            NodeMemory.AllocSite allocSite = currentObjectState.allocSite();
            String name = allocSite.name();
            String row = "";

            if (name != null && !name.isEmpty()) {
                row = String.valueOf(sourceLL.findDefinitionLine(name));
            }
            htmlContent.append(formatAllocSiteAsList(allocSite, row));
        }
        htmlContent.append("<br>");

        if (currentObjectState.offsetPlane() != null) {
            htmlContent.append("<b style='color:blue;'>offsetPlane</b>");
            htmlContent.append("<br>");
            appendPlaneDetailsHTML(htmlContent, currentObjectState.offsetPlane());
        }

        htmlContent.append("<br>");
        htmlContent.append("<br>");

        // Segment Plane
        if (currentObjectState.segmentPlane() != null) {
            htmlContent.append("<b style='color:blue;'>segmentPlane</b>");
            htmlContent.append("<br>");
            appendPlaneDetailsHTML(htmlContent, currentObjectState.segmentPlane());
        }

        htmlContent.append("</body></html>");

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText(htmlContent.toString());
        editorPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        objectInfoPanel.add(scrollPane, BorderLayout.CENTER);
        objectInfoPanel.revalidate();
        objectInfoPanel.repaint();
    }

    private String formatAllocSiteAsList(NodeMemory.AllocSite allocSite, String row) {

        return "&nbsp;&nbsp;- <span style='color:blue;'>scope:</span> " + allocSite.scope() + "<br>"
                + "&nbsp;&nbsp;- <span style='color:blue;'>name:</span> " + allocSite.name() + "<br>"
                + "&nbsp;&nbsp;- <span style='color:blue;'>row:</span> " + row + "<br>"
                + "&nbsp;&nbsp;- <span style='color:blue;'>code:</span> " + allocSite.code() + "<br>";
    }


    private void appendKeyValueInlineNonBold(StringBuilder html, String key, Object value) {
        html.append("<span style='color:blue;'>").append(key).append(":</span> <span style='color:black;'>").append(value).append("</span>&nbsp;&nbsp;&nbsp;");
    }

    private void appendPlaneDetailsHTML(StringBuilder html, NodeMemory.Plane plane) {
        appendKeyValueInlineNonBold(html, "rootObject", plane.rootObject());
        appendKeyValueInlineNonBold(html, "initialValue", plane.initialValue());
        appendKeyValueInlineNonBold(html, "sizeBound", plane.sizeBound());
        appendKeyValueInlineNonBold(html, "initialized", plane.initialized());
        appendKeyValueInlineNonBold(html, "symbolic", plane.symbolic());
    }
}
