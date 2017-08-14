package com.testutils.testinfo.internal;

import com.markit.n6platform.util.basic.StringUtils;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JiraQuery {

    private JiraInfoProvider jiraInfoProvider;

    private String normalize(String key) {
        return key.length() == 1 ? key.replace("-", "") : key;
    }

    public JiraQuery filter(String ddbc, String release,
                            String projects, String customQuery,
                            Set<String> linkedJiras) throws JiraException {


        if (!StringUtils.isBlank(normalize(ddbc))) {
            jiraInfoProvider = JiraInfoProvider.byDDBC(ddbc, normalize(projects), linkedJiras);
        } else if (!StringUtils.isBlank(normalize(release))) {
            jiraInfoProvider = JiraInfoProvider.byRelease(release, normalize(projects), linkedJiras);
        } else if (!StringUtils.isBlank(normalize(customQuery))) {
            jiraInfoProvider = JiraInfoProvider.byCustomQuery(customQuery, normalize(projects), linkedJiras);
        } else {
            throw new IllegalArgumentException("No valid jira filter");
        }
        return this;
    }

    public Issue getIssue(EpicInfo epicInfo, String key) {
        return jiraInfoProvider.getIssue(epicInfo, key);
    }

    public Collection<Issue> getEpicBugs(EpicInfo epicInfo) {
        return jiraInfoProvider.getEpicBugs(epicInfo);
    }

    public Collection<Issue> getEpicFeatures(EpicInfo epicInfo) {
        return jiraInfoProvider.getEpicFeatures(epicInfo);
    }

    public Collection<Issue> getEpicStory(EpicInfo epicInfo) {
        return jiraInfoProvider.getEpicStory(epicInfo);
    }

    public boolean isInEpic(EpicInfo epicInfo, List<String> jiras) {
        for (String jira : jiras) {
            if (jiraInfoProvider.isInEpic(epicInfo, jira)) {
                return true;
            }
        }
        return false;
    }

    public Collection<EpicInfo> getEpicInfo() {
        return jiraInfoProvider.getEpicInfo();
    }
}
