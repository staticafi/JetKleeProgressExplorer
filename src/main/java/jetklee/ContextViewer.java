package jetklee;

import javax.swing.*;

/**
 * Panel that displays context of selected execution state
 */
public class ContextViewer extends TextViewerBase {
    public ContextViewer() {
        super();
    }

    public void displayContext(NodeInfo info) {
        NodeInfo.Context context = info.getContext();

        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><body style='font-family:sans-serif;'>");

        // ROW 1: nodeID, stateID, parentID, nextID, depth
        htmlBuilder.append("<div><b style='color:blue;'>nodeId:</b> ")
                .append(context.nodeID()).append("<br>")
                .append("<span style='color:blue;'>stateId:</span> ").append(context.stateID()).append(" ")
                .append("<span style='color:blue;'>parentId:</span> ").append(context.parentID()).append(" ")
                .append("<span style='color:blue;'>nextId:</span> ").append(context.nextID()).append(" ")
                .append("<span style='color:blue;'>depth:</span> ").append(context.depth())
                .append("</div>");

        // ROW 2: uniqueState, coveredNew, forkDisabled, instsSinceCovNew, steppedInstructions
        htmlBuilder.append("<div><span style='color:blue;'>uniqueState:</span> ").append(context.uniqueState()).append(" ")
                .append("<span style='color:blue;'>coveredNew:</span> ").append(context.coveredNew()).append(" ")
                .append("<span style='color:blue;'>forkDisabled:</span> ").append(context.forkDisabled()).append(" ")
                .append("<span style='color:blue;'>instsSinceCovNew:</span> ").append(context.instsSinceCovNew()).append(" ")
                .append("<span style='color:blue;'>steppedInstructions:</span> ").append(context.steppedInstructions())
                .append("</div>")
                .append("<br>");

        // ROW 3: location
        htmlBuilder.append("<div><b style='color:blue;'>location:</b><br>")
                .append(formatLocationAsList(context.location()))
                .append("</div>")
                .append("<br>");

        // ROW 4: nextLocation
        htmlBuilder.append("<div><b style='color:blue;'>nextLocation:</b><br>")
                .append(formatLocationAsList(context.nextLocation()))
                .append("</div>")
                .append("<br>");

        // ROW 5: stack (Values normal)
        htmlBuilder.append("<div><b style='color:blue;'>stack:</b><br>");
        for (NodeInfo.Location stackLocation : context.stack()) {
            htmlBuilder.append("&nbsp;&nbsp;- <span style='color:blue;'>file:</span> ").append(stackLocation.file())
                    .append(" <span style='color:blue;'>line:</span> ").append(stackLocation.line())
                    .append(" <span style='color:blue;'>column:</span> ").append(stackLocation.column())
                    .append("<br>");
        }
        htmlBuilder.append("</div>");

        htmlBuilder.append("</body></html>");

        JEditorPane editorPane = new JEditorPane("text/html", htmlBuilder.toString());
        editorPane.setEditable(false);
        removeAll();
        add(new JScrollPane(editorPane));
        revalidate();
    }

    private String formatLocationAsList(NodeInfo.Location location) {
        return "&nbsp;&nbsp;- <span style='color:blue;'>file:</span> " + location.file() + "<br>"
                + "&nbsp;&nbsp;- <span style='color:blue;'>line:</span> " + location.line() + "<br>"
                + "&nbsp;&nbsp;- <span style='color:blue;'>column:</span> " + location.column() + "<br>"
                + "&nbsp;&nbsp;- <span style='color:blue;'>assemblyLine:</span> " + location.assemblyLine() + "<br>";
    }
}
