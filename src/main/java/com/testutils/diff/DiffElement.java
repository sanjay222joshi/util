package com.testutils.diff;

public abstract class DiffElement {
    protected final String name;
    protected DiffDelta delta;

    public static final String COLOR_ADD = "#A3E18C";
    public static final String COLOR_REMOVE = "#F17464";
    public static final String COLOR_IGNORE = "#DEFCFC";
    public static final String COLOR_CHANGED = "#F1EB9B";

    protected DiffElement(String name) {
        this.name = name;
    }

    public void setDelta(DiffDelta delta) {
        this.delta = delta;
    }

    public abstract void generateMessage(String path, StringBuilder output);

    public abstract void generateHtmlReport(StringBuilder output, String indent, boolean full);

    public boolean isEqual() {
        return delta == DiffDelta.EQUAL || delta == DiffDelta.IGNORED;
    }

    public abstract Integer getDeltaCount();

    public String getName() {
        return name;
    }

    public abstract String getDeltaColor();
}
