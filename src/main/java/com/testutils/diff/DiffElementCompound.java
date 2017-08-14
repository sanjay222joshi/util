package com.testutils.diff;

import java.util.ArrayList;
import java.util.List;

public class DiffElementCompound extends DiffElement {
    private final List<DiffElement> children;
    private final boolean list;

    public DiffElementCompound(String pathElement, boolean list) {
        super(pathElement);
        this.list = list;
        children = new ArrayList<>();
    }

    @Override
    public void generateMessage(String path, StringBuilder output) {
        String childPath = path.isEmpty() || name.isEmpty() ? name : path + '.' + name;
        for (DiffElement child : children) {
            child.generateMessage(list ? path : childPath, output);
        }
    }

    @Override
    public void generateHtmlReport(StringBuilder output, String indent, boolean full) {
        output.append(indent).append("<table class='diffTable'>\n");
        indent += " ";
        if (list) {
            output.append("<td>");
            for (DiffElement child : children) {
                if (full || !child.isEqual()) {
                    output.append(indent).append("<table>\n<tr>");
                    output.append("<td");
                    String color = child.getDeltaColor();
                    if (color != null) {
                        output.append(" style='background-color:").append(child.getDeltaColor()).append('\'');
                    }
                    output.append('>');
                    child.generateHtmlReport(output, indent, full);
                    output.append(indent).append("</td></tr></table>");
                }
            }
            output.append("</td></tr>\n");
        } else {
            for (DiffElement child : children) {
                if (full || !child.isEqual()) {
                    output.append("<tr><td>").append(child.getName()).append("</td>");
                    output.append("<td");
                    String color = child.getDeltaColor();
                    if (color != null) {
                        output.append(" style='background-color:").append(child.getDeltaColor()).append('\'');
                    }
                    output.append('>');
                    child.generateHtmlReport(output, indent, full);
                    output.append("</td></tr>\n");
                }
            }
        }
        output.append("</table>\n");
    }

    @Override
    public Integer getDeltaCount() {
        int deltaCount = 0;
        for (DiffElement child : children) {
            if (!child.isEqual()) {
                deltaCount++;
            }
        }
        return deltaCount;
    }

    @Override
    public String getDeltaColor() {
        switch (delta) {
            case ADD:
                return COLOR_ADD;
            case REMOVE:
                return COLOR_REMOVE;
            default:
                return null;
        }
    }

    public void addChild(DiffElement diffElement) {
        children.add(diffElement);
    }

    public List<DiffElement> getChildren() {
        return children;
    }
}
