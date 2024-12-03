package jetklee;

import javax.swing.*;

import static jetklee.Styles.KEY_COLOR;
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
                "<html><body style='font-family:" + INFO_FONT + ";'>" +
                        createNodeInfoRow(context) +
                        createStateInfoRow(context) +
                        createLocationRow("location", context.location()) +
                        createLocationRow("nextLocation", context.nextLocation()) +
                        createStackRow(context) +
                        "</body></html>";

        displayInEditorPane(html);
    }

    private String createNodeInfoRow(NodeInfo.Context context) {
        return "<div><b><span style=" + KEY_COLOR + ">nodeId:</span></b> " + context.nodeID() + "</div>" +
                "<div><span style=" + KEY_COLOR + ">stateId:</span> " + context.stateID() +
                " <span style=" + KEY_COLOR + ">parentId:</span> " + context.parentID() +
                " <span style=" + KEY_COLOR + ">nextId:</span> " + context.nextID() +
                " <span style=" + KEY_COLOR + ">depth:</span> " + context.depth() +
                "</div>";
    }

    private String createStateInfoRow(NodeInfo.Context context) {
        return "<div><span style=" + KEY_COLOR + ">uniqueState:</span> " + context.uniqueState() +
                " <span style=" + KEY_COLOR + ">coveredNew:</span> " + context.coveredNew() +
                " <span style=" + KEY_COLOR + ">forkDisabled:</span> " + context.forkDisabled() +
                " <span style=" + KEY_COLOR + ">instsSinceCovNew:</span> " + context.instsSinceCovNew() +
                " <span style=" + KEY_COLOR + ">steppedInstructions:</span> " + context.steppedInstructions() +
                "</div><br>";
    }

    private String createLocationRow(String label, NodeInfo.Location location) {
        String formattedLocation =
                "&nbsp;&nbsp;- <span style=" + KEY_COLOR + ">file:</span> " + location.file() + "<br>" +
                        "&nbsp;&nbsp;- <span style=" + KEY_COLOR + ">line:</span> " + location.line() + "<br>" +
                        "&nbsp;&nbsp;- <span style=" + KEY_COLOR + ">column:</span> " + location.column() + "<br>" +
                        "&nbsp;&nbsp;- <span style=" + KEY_COLOR + ">assemblyLine:</span> " + location.assemblyLine() + "<br>";

        return "<div><b><span style=" + KEY_COLOR + ">" + label + ":</span></b><br>" +
                formattedLocation + "</div><br>";
    }

    private String createStackRow(NodeInfo.Context context) {
        StringBuilder stackBuilder = new StringBuilder();
        stackBuilder.append("<div><b><span style=" + KEY_COLOR + ">stack:</span></b><br>");

        for (NodeInfo.Location stackLocation : context.stack()) {
            stackBuilder.append("&nbsp;&nbsp;- <span style=" + KEY_COLOR + ">file:</span> ")
                    .append(stackLocation.file())
                    .append(" <span style=" + KEY_COLOR + ">line:</span> ").append(stackLocation.line())
                    .append(" <span style=" + KEY_COLOR + ">column:</span> ").append(stackLocation.column())
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
