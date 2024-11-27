package jetklee;

public class KQueryFormatter {

    public static String formatConstraint(String input) {
        StringBuilder formatted = new StringBuilder();
        int indentLevel = 0;
        boolean inFunction = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (c) {
                case ('('):
                    if (i != 0) {
                        formatted.append("<br>");
                    }
                    formatted
                            .append(indent(indentLevel))
                            .append(c)
                            .append("<b style='color:blue;'>");
                    inFunction = true;
                    indentLevel++;
                    break;
                case ('['):
                    indentLevel++;
                    if (i != 0) {
                        formatted.append("<br>");
                    }
                    formatted
                            .append(indent(indentLevel))
                            .append(c);
                    break;

                case ')':
                    indentLevel--;
                    if (inFunction) {
                        inFunction = false;
                        formatted.append(c).append("<br>");
                    } else {
                        formatted
                                .append(c)
                                .append("<br>")
                                .append(indent(indentLevel));
                    }
                    break;

                case ']':
                    indentLevel--;
                    formatted.append(c);
                    break;

                case ' ':
                    if (inFunction) {
                        if (i + 1 < input.length() && input.charAt(i + 1) != '(') {
                            formatted.append("<br>");
                            formatted.append(indent(indentLevel));
                        }
                        formatted.append("</b> ");
                        inFunction = false;
                    }
                    formatted.append(c);
                    break;

                default:
                    formatted.append(c);
                    break;
            }
        }

        return formatted.toString().trim();
    }

    private static String indent(int level) {
        return "&nbsp;".repeat(level * 4);
    }
}
