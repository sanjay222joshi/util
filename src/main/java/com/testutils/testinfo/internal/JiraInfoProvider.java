package com.testutils.testinfo.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.markit.n6platform.util.basic.StringUtils;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

public class JiraInfoProvider {

    private static final int MAX_RESULTS = 1000;

    static {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

    private final Set<String> ignoreJirasWithStatus = Sets.newHashSet("Cancelled");
    private JiraClient jira = new JiraClient("https://jira.markit.com", new BasicCredentials("teamcityagent", "Welcome@123"));

    private final Table<EpicInfo, String, Issue> epicBugMap = HashBasedTable.create();
    private final Table<EpicInfo, String, Issue> epicFeatureMap = HashBasedTable.create();
    private final Table<EpicInfo, String, Issue> epicStoryMap = HashBasedTable.create();

    private final Map<String, Issue> otherIssueMap = new HashMap<>();

    private JiraInfoProvider() {
    }

    public static JiraInfoProvider byDDBC(String ddbc, String projects, Set<String> linkedJiras) throws JiraException {
        JiraInfoProvider provider = new JiraInfoProvider();
        provider.ddbcJiras(ddbc, projects);
        provider.linkedJiras(linkedJiras);
        return provider;
    }

    public static JiraInfoProvider byRelease(String releaseVersion, String projects, Set<String> linkedJiras) throws JiraException {
        JiraInfoProvider provider = new JiraInfoProvider();
        provider.releaseJiras(releaseVersion, projects);
        provider.linkedJiras(linkedJiras);
        return provider;
    }

    public static JiraInfoProvider byCustomQuery(String customQuery, String projects, Set<String> linkedJiras) throws JiraException {
        JiraInfoProvider provider = new JiraInfoProvider();
        provider.query(customQuery, projects);
        provider.linkedJiras(linkedJiras);
        return provider;
    }


    private void linkedJiras(Set<String> linkedJiras) throws JiraException {
        List<String> otherIssues = linkedJiras.stream().filter(linkedJira -> !epicFeatureMap.containsColumn(linkedJira) && !epicStoryMap.containsColumn(linkedJira) && !epicBugMap.containsColumn(linkedJira)).collect(Collectors.toList());
        if (!otherIssues.isEmpty()) {
            Issue.SearchResult otherSearch = jira.searchIssues("id in (" + StringUtils.join(otherIssues, ',') + ")", MAX_RESULTS);
            for (Issue issue : otherSearch.issues) {
                otherIssueMap.put(issue.getKey(), issue);
            }
        }
    }

    private void ddbcJiras(String searchKey, String projects) throws JiraException {
        Issue epicIssue = jira.getIssue(searchKey);
        String dueDate = String.valueOf(epicIssue.getField("customfield_20228"));
        Issue.SearchResult epicSearch = jira.searchIssues("\"Epic Link\" = \"" + searchKey + "\"", MAX_RESULTS);
        EpicInfo epicInfo = new EpicInfo(searchKey, epicIssue.getSummary(), epicIssue.getStatus() == null ? null : epicIssue.getStatus().getName(), dueDate);

        Set<String> proj = new HashSet<>(Splitter.on(',').splitToList(projects));
        for (Issue issue : epicSearch.issues) {
            if (!ignoreJirasWithStatus.contains(issue.getStatus().getName())) {
                if (projects.isEmpty()
                        || proj.isEmpty()
                        || proj.contains(issue.getProject().getKey())) {
                    addIssue(issue, epicInfo);
                }
            }
        }
    }

    private void releaseJiras(String fixVersion, String projects) throws JiraException {
        String query = "fixVersion= '" + fixVersion + "'";
        query(query, projects);
    }


    private void query(String query, String projects) throws JiraException {
        if (!StringUtils.isBlank(projects)) {
            query += " and project in (" + projects + ")";
        }

        Issue.SearchResult searchResult;
        try {
            searchResult = jira.searchIssues(query, MAX_RESULTS);
        } catch (JiraException e) {
            e.printStackTrace();
            searchResult = new Issue.SearchResult();
            searchResult.issues = new ArrayList<>();
        }
        Multimap<String, Issue> issuesInRelease = ArrayListMultimap.create();
        for (Issue issue : searchResult.issues) {
            if (!ignoreJirasWithStatus.contains(issue.getStatus().getName())) {
                String epic = String.valueOf(issue.getField("customfield_14221"));
                if (epic == null || epic.equals("null")) {
                    otherIssueMap.put(issue.getKey(), issue);
                } else {
                    issuesInRelease.put(epic, issue);
                }
            }
        }

        for (String epic : issuesInRelease.keySet()) {
            Issue epicIssue = jira.getIssue(epic);
            Collection<Issue> issues = issuesInRelease.get(epic);
            Object fv = epicIssue.getField("customfield_20228");
            String dueDate = fv == null ? "NA" : String.valueOf(fv);
            EpicInfo epicInfo = new EpicInfo(epic, epicIssue.getSummary(), epicIssue.getStatus() == null ? null : epicIssue.getStatus().getName(), dueDate);
            for (Issue issue : issues) {
                addIssue(issue, epicInfo);
            }
        }
    }

    private void addIssue(Issue issue, EpicInfo epicInfo) {
        if (isBug(issue)) {
            epicBugMap.put(epicInfo, issue.getKey(), issue);
        }
        if (isNewFeature(issue)) {
            epicFeatureMap.put(epicInfo, issue.getKey(), issue);
        }

        if (isNewStory(issue)) {
            epicStoryMap.put(epicInfo, issue.getKey(), issue);
        }
    }

    public Collection<EpicInfo> getEpicInfo() {
        if(epicFeatureMap.rowKeySet().size() == 0){
            return epicStoryMap.rowKeySet();
        }
        return epicFeatureMap.rowKeySet();
    }

    Issue getIssue(EpicInfo epicInfo, String key) {
        if (epicInfo == null) {
            return otherIssueMap.get(key);
        }
        Issue epicIssue = epicFeatureMap.get(epicInfo, key);
        if (epicIssue == null) {
            epicIssue = epicBugMap.get(epicInfo, key);
        }

        if (epicIssue == null) {
            epicIssue = epicStoryMap.get(epicInfo, key);
        }
        return epicIssue;
    }

    boolean isInEpic(EpicInfo epicInfo, String jiraKey) {
        return getIssue(epicInfo, jiraKey) != null;
    }

    public Collection<Issue> getEpicFeatures(EpicInfo epicInfo) {
        return epicFeatureMap.row(epicInfo).values();
    }

    public Collection<Issue> getEpicBugs(EpicInfo epicInfo) {
        return epicBugMap.row(epicInfo).values();
    }

    public Collection<Issue> getEpicStory(EpicInfo epicInfo) {
        return epicStoryMap.row(epicInfo).values();
    }

    private boolean isBug(Issue issue) {
        return "Bug".equals(issue.getIssueType().getName());
    }

    private boolean isNewFeature(Issue issue) {
        return "New Feature".equals(issue.getIssueType().getName());
    }

    private boolean isNewStory(Issue issue) {
        return "Story".equals(issue.getIssueType().getName());
    }

    public Map<String, Issue> getOtherIssueMap() {
        return otherIssueMap;
    }
}
