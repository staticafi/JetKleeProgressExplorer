package jetklee;

import java.awt.*;

public class TableRow {
    private Color color;
    private String index;
    private String value;
    private String mask;

    public TableRow(Color color, String index, String value, String mask, boolean isConcrete) {
        this.color = color;
        this.index = index;
        this.value = value;
        this.mask = getMaskFromConcrete(mask, isConcrete);
    }

    private String getMaskFromConcrete(String concreteMask, boolean isConcrete) {
        String mask = null;
        if (concreteMask != null) {
            mask = isConcrete ?
                    concreteMask.equals("0") ? "false" : "true"    // concrete
                    : concreteMask.equals("0") ? "true" : "false"; // symbolic
        }
        return mask;
    }

    public Color getColor() {
        return color;
    }

    public String getIndex() {
        return index;
    }

    public String getValue() {
        return value;
    }

    public String getMask() {
        return mask;
    }
}
