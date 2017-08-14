package com.testutils.testinfo.internal;

public enum TestResult {
    PASSED, FAILED, SKIPPED;

    public String getImage() {
        return this == PASSED?"tick.png":this == FAILED?"cross.png":"arrow-skip.png";
    }

    public String toCss() {
        return toString().toLowerCase();
    }
}
