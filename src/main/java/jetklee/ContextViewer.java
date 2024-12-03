package jetklee;

import javax.swing.*;

import static jetklee.Styles.BLUE_STYLE;
import static jetklee.Styles.INFO_FONT;

/**
 * Panel that displays context of selected execution state.
 */
public class ContextViewer extends TextViewerBase {

    public ContextViewer() {
        super();
    }

    /**
     * Displays context of selected execution state in the Context panel.
     *
     * @param info to display.
     */
    public void displayContext(NodeInfo info) {
        NodeInfo.Context context = info.getContext();

        String html =
                "<html><body style=" + INFO_FONT + ">" +
                        createNodeInfoRow(context) +
                        createStateInfoRow(context) +
                        createLocationRow("location", context.location()) +
                        createLocationRow("nextLocation", context.nextLocation()) +
                        createStackRow(context) +
                        "</body></html>";

        displayInEditorPane(html);
    }

    private String createNodeInfoRow(NodeInfo.Context context) {
        return "<div><b><span style=" + BLUE_STYLE + ">nodeId:</span></b> " + context.nodeID() + "</div>" +
                "<div><span style=" + BLUE_STYLE + ">stateId:</span> " + context.stateID() +
                " <span style=" + BLUE_STYLE + ">parentId:</span> " + context.parentID() +
                " <span style=" + BLUE_STYLE + ">nextId:</span> " + context.nextID() +
                " <span style=" + BLUE_STYLE + ">depth:</span> " + context.depth() +
                "</div>";
    }

    private String createStateInfoRow(NodeInfo.Context context) {
        return "<div><span style=" + BLUE_STYLE + ">uniqueState:</span> " + context.uniqueState() +
                " <span style=" + BLUE_STYLE + ">coveredNew:</span> " + context.coveredNew() +
                " <span style=" + BLUE_STYLE + ">forkDisabled:</span> " + context.forkDisabled() +
                " <span style=" + BLUE_STYLE + ">instsSinceCovNew:</span> " + context.instsSinceCovNew() +
                " <span style=" + BLUE_STYLE + ">steppedInstructions:</span> " + context.steppedInstructions() +
                "</div><br>";
    }

    private String createLocationRow(String label, NodeInfo.Location location) {
        String formattedLocation =
                "&nbsp;&nbsp;- <span style=" + BLUE_STYLE + ">file:</span> " + location.file() + "<br>" +
                        "&nbsp;&nbsp;- <span style=" + BLUE_STYLE + ">line:</span> " + location.line() + "<br>" +
                        "&nbsp;&nbsp;- <span style=" + BLUE_STYLE + ">column:</span> " + location.column() + "<br>" +
                        "&nbsp;&nbsp;- <span style=" + BLUE_STYLE + ">assemblyLine:</span> " + location.assemblyLine() + "<br>";

        return "<div><b><span style=" + BLUE_STYLE + ">" + label + ":</span></b><br>" +
                formattedLocation + "</div><br>";
    }

    private String createStackRow(NodeInfo.Context context) {
        StringBuilder stackBuilder = new StringBuilder();
        stackBuilder.append("<div><b><span style=" + BLUE_STYLE + ">stack:</span></b><br>");

        for (NodeInfo.Location stackLocation : context.stack()) {
            stackBuilder.append("&nbsp;&nbsp;- <span style=" + BLUE_STYLE + ">file:</span> ")
                    .append(stackLocation.file())
                    .append(" <span style=" + BLUE_STYLE + ">line:</span> ").append(stackLocation.line())
                    .append(" <span style=" + BLUE_STYLE + ">column:</span> ").append(stackLocation.column())
                    .append("<br>");
        }
        stackBuilder.append("</div>");
        return stackBuilder.toString();
    }

    private void displayInEditorPane(String html) {
        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setEditable(false);
        this.removeAll();
        this.add(new JScrollPane(editorPane));
        this.revalidate();
    }
}
