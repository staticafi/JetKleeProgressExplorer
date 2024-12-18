package jetklee;

import javax.swing.*;

import java.util.ArrayList;

import static jetklee.HtmlFormatter.appendKeyValueInlineNonBold;
import static jetklee.Styles.*;

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
    public void displayContext(ExecutionState.Context context) {
        if (context == null) {
            displayInEditorPane("");
            return;
        }
        String html = "<html><body style='font-family:" + INFO_FONT + "; font-size:" + INFO_FONT_SIZE +
                ";padding:5px;'>" +
                createNodeInfoRow(context) +
                createStateInfoRow(context) +
                createLocationRow("firstLocation", context.insertContext().firstLocation()) +
                createLocationRow("lastLocation", context.lastLocation()) +
                createStackRow(context.insertContext().stack()) +
                "</body></html>";

        displayInEditorPane(html);
    }

    private String createNodeInfoRow(ExecutionState.Context context) {
        StringBuilder row = new StringBuilder();
        row.append("<b style='color:" + KEY_COLOR + ";'>nodeId: </b>").append(context.insertContext().nodeID()).append("<br>");

        appendKeyValueInlineNonBold(row, "stateId", context.insertContext().stateID());
        appendKeyValueInlineNonBold(row, "parentId", context.parentID());
        appendKeyValueInlineNonBold(row, "depth", context.insertContext().depth());
        row.append("<br>");
        return row.toString();
    }

    private String createStateInfoRow(ExecutionState.Context context) {
        StringBuilder row = new StringBuilder();
        appendKeyValueInlineNonBold(row, "uniqueState", context.insertContext().uniqueState());
        appendKeyValueInlineNonBold(row, "coveredNew", context.coveredNew());
        appendKeyValueInlineNonBold(row, "forkDisabled", context.forkDisabled());
        appendKeyValueInlineNonBold(row, "instsSinceCovNew", context.instsSinceCovNew());
        appendKeyValueInlineNonBold(row, "steppedInstructions", context.steppedInstructions());
        row.append("<br><br>");
        return row.toString();
    }

    private String createLocationRow(String label, ExecutionState.Location location) {
        return "<div><b><span style='color:" + KEY_COLOR + ";'>" + label + ":</span></b><br>" +
                "&nbsp;&nbsp;- <span style='color:" + KEY_COLOR + ";'>file:</span>" + location.file() + "<br>" +
                "&nbsp;&nbsp;- <span style='color:" + KEY_COLOR + ";'>line:</span>" + location.line() + "<br>" +
                "&nbsp;&nbsp;- <span style='color:" + KEY_COLOR + ";'>column:</span>" + location.column() + "<br>" +
                "&nbsp;&nbsp;- <span style='color:" + KEY_COLOR + ";'>assemblyLine:</span>" + location.assemblyLine() + "<br>" +
                "</div><br>";
    }

    private String createStackRow(ArrayList<ExecutionState.Location> stack) {
        StringBuilder stackBuilder = new StringBuilder();
        stackBuilder.append("<div><b><span style='color:").append(KEY_COLOR).append(";'>stack:</span></b><br>");

        for (ExecutionState.Location stackLocation : stack) {
            stackBuilder.append("&nbsp;&nbsp;- <span style='color:").append(KEY_COLOR).append(";'>file:</span> ")
                    .append(stackLocation.file())
                    .append(" <span style='color:").append(KEY_COLOR).append(";'>line:</span> ").append(stackLocation.line())
                    .append(" <span style='color:").append(KEY_COLOR).append(";'>column:</span> ").append(stackLocation.column())
                    .append(" <span style='color:").append(KEY_COLOR).append(";'>assemblyLine:</span> ").append(stackLocation.assemblyLine())
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
        this.repaint();
    }
}
