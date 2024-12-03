package jetklee;

public class KQueryFormatter {
    private static final int INDENT_SIZE = 6;

    public static String formatConstraint(String constraint) {
        int indentLevel = 0;
        boolean inFunction = false;
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < constraint.length(); i++) {
            char c = constraint.charAt(i);

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
                    if (i != 0) {
                        formatted.append("<br>");
                    }
                    formatted
                            .append(indent(indentLevel))
                            .append(c);
                    indentLevel++;
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
                        formatted.append("</b> ");
                        inFunction = false;

                        // put the parameters of the function on a new line
                        if (i + 1 < constraint.length() && constraint.charAt(i + 1) != '(') {
                            formatted
                                    .append("<br>")
                                    .append(indent(indentLevel));
                        }
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
        return "&nbsp;".repeat(level * INDENT_SIZE);
    }
}
