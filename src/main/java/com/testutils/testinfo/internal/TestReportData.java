package com.testutils.testinfo.internal;

import java.util.*;

public class TestReportData {

    private final Map<String, List<TestRunInfo>> linkedJiras = new HashMap<>();
    private final List<TestRunInfo> testInfo = new ArrayList<>();

    public void addTestRunInfo(TestRunInfo testRunInfo) {
        testInfo.add(testRunInfo);
        for (String key : testRunInfo.getLinkedJiras()) {
            List<TestRunInfo> testRunInfos = linkedJiras.computeIfAbsent(key, s -> new ArrayList<>());
            testRunInfos.add(testRunInfo);
        }
    }

    public Set<String> getLinkedJiras() {
        return linkedJiras.keySet();
    }

    public int getTestCount(String jiraKey) {
        List<TestRunInfo> testRunInfos = linkedJiras.get(jiraKey);
        return testRunInfos == null?0:testRunInfos.size();
    }

    public List<TestRunInfo> getTestRunsInfo() {
        return testInfo;
    }

    public List<TestRunInfo> getTestInfos(String key) {
        return linkedJiras.get(key);
    }
}