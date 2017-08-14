package com.testutils.testinfo.internal;

import net.rcarz.jiraclient.Issue;

import java.util.*;

public class EpicInfo {

    private final String key;
    private final String summary;
    private final String status;
    private final String dueDate;

    public EpicInfo(String key, String summary, String status, String dueDate) {
        this.key = key;
        this.summary = summary;
        this.status = status;
        this.dueDate = dueDate;
    }

    public String getKey() {
        return key;
    }

    public String getSummary() {
        return summary;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EpicInfo)) return false;

        EpicInfo epicInfo = (EpicInfo) o;

        return key != null ? key.equals(epicInfo.key) : epicInfo.key == null;

    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
}
