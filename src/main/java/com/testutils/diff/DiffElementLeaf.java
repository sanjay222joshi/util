package com.testutils.diff;

import java.util.List;

public class DiffElementLeaf extends DiffElement {

    private final Object value1;
    private final Object value2;

    public DiffElementLeaf(String pathElement, Object value1, Object value2, DiffDelta delta) {
        super(pathElement);
        this.value1 = value1;
        this.value2 = value2;
        setDelta(delta);
    }

    @Override
    public void generateMessage(String path, StringBuilder output) {
        if (delta != DiffDelta.EQUAL && delta != DiffDelta.IGNORED) {
            String location = path.isEmpty() ? name : path + '.' + name;
            output.append(location).append(": ");
            if (delta == DiffDelta.REMOVE) {
                output.append("Removed ").append(value1);
            } else if (delta == DiffDelta.ADD) {
                output.append("Added ").append(value2);
            } else {
                output.append(value1).append(" != ").append(value2);
            }
            output.append('\n');
        }
    }

    @Override
    public void generateHtmlReport(StringBuilder output, String indent, boolean full) {
        if (delta == DiffDelta.CHANGED || delta == DiffDelta.IGNORED) {
            output.append("<table ");
            if (getDeltaColor() != null) {
                output.append(" style='").append(getDeltaColor()).append('\'');
            }
            output.append(">\n<tr>\n<td>input</td><td>");
            appendValueDif(output, value1);
            output.append("</td>\n</tr><tr>\n<td>reverse</td><td>").append(value2).append("</td></tr>\n</table>\n");
        } else {
            if (delta == DiffDelta.ADD) {
                appendValueDif(output, value2);
            } else {
                appendValueDif(output, value1);
            }
        }
    }

    private void appendValueDif(StringBuilder output, Object value) {
        appendValue(output, value);
    }

    private void appendValue(StringBuilder output, Object value) {
        if (value instanceof List) {
            for (Object element : (List<?>) value) {
                appendValue(output, element);
            }
        } else if (value instanceof N6Instance) {
            output.append("<table class='diffTable'>\n");
            N6Instance instance = (N6Instance) value;
            for (N6Field field : instance.getType().getFields()) {
                output.append("<tr><td>").append(field.getName()).append("</td><td>");
                appendValue(output, instance.getValue(field.getName()));
                output.append("</td></tr>\n");
            }
            output.append("</table>\n");
        } else {
            //noinspection NestedConditionalExpression
            output.append(value == null ? "" : "".equals(value) ? "&nbsp;" : String.valueOf(value));
        }
    }

    @Override
    public Integer getDeltaCount() {
        return delta == DiffDelta.EQUAL ? 0 : 1;
    }

    @Override
    public String getDeltaColor() {
        switch (delta) {
            case ADD:
                return COLOR_ADD;
            case REMOVE:
                return COLOR_REMOVE;
            case CHANGED:
                return COLOR_CHANGED;
            case IGNORED:
                return COLOR_IGNORE;
            default:
                return null;
        }
    }
}
