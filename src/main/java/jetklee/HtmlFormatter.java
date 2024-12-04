package jetklee;

import static jetklee.Styles.KEY_COLOR;

public class HtmlFormatter {
    public static void appendKeyValueInlineNonBold(StringBuilder html, String key, Object value) {
        html.append("<span style='color:" + KEY_COLOR + ";'>").append(key).append(":</span>").append(value).append("&nbsp;&nbsp;&nbsp;");
    }
}
