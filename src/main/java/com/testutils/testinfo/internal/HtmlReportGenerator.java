package com.testutils.testinfo.internal;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Status;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.testutils.testinfo.ApplicationInfoProvider;
import com.testutils.testinfo.TestState;

import static com.testutils.testinfo.TestState.*;
import static com.testutils.testinfo.internal.ReportUtils.readableName;

public class HtmlReportGenerator {

    private static final String HTML_HEADER =
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n" +
                    "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
                    "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">\n" +
                    "<link type=\"text/css\" rel=\"stylesheet\" href=\"css/qaReport.css\"/>\n" +
                    "<link type=\"text/css\" rel=\"stylesheet\" href=\"css/tab.css\"/>\n" +
                    "<script type=\"text/javascript\" src=\"scripts/tab.js\"></script>\n" +
                    "<head>\n" +
                    "    <title>QA progress report</title>\n" +
                    "</head>\n";

    private static final String teamcityDownloadRepoPath = "http://teamcity.marketxs.com:8100/repository/download/";
    private static final String testPath = ":id/reports/tests/classes";
    private final String testBaseUrl;
    private final String buildId;
    private final String packagePrefix;
    private final String reportName;
    private final boolean tabReport;
    private final Predicate<TestRunInfo> integrationTestCond = testRunInfo -> testRunInfo.getTestInfo() != null && testRunInfo.getTestInfo().integration();

    public HtmlReportGenerator(String reportName, String buildTypeId, String buildId, String packagePrefix, boolean tabReport) {
        this.reportName = reportName;
        this.testBaseUrl = teamcityDownloadRepoPath + buildTypeId;
        this.buildId = buildId;
        this.packagePrefix = packagePrefix;
        this.tabReport = tabReport;
    }

    public void generate(File qaReportDir, TestReportData data, JiraQuery jiraQuery) throws IOException {
        File file = new File(qaReportDir, "qaReport.html");

        List<TestRunInfo> testRunsInfo = data.getTestRunsInfo();

        boolean hasIntegrationTest = testRunsInfo.stream().anyMatch(integrationTestCond);

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.append(HTML_HEADER);
            fileWriter.append("<body>\n");
            fileWriter.append("<h1>").append(reportName).append("</h1>\n");
            Collection<EpicInfo> epicInfo = jiraQuery.getEpicInfo();
            StringWriter writer = new StringWriter();
            OverallStats overallStats = new OverallStats();

            if (tabReport) {
                boolean first = true;
                writer.append("<ul class=\"tab\">");
                for (EpicInfo epic : epicInfo) {
                    if (first) {
                        writer.append("<li><a href=\"javascript:void(0)\" class=\"tablinks\" onclick=\"openTab(event, '")
                                .append(epic.getKey()).append("')").append("\"id=\"defaultOpen\">").append(epic.getSummary()).append("</a></li>");
                        first = false;
                    } else {
                        writer.append("<li><a href=\"javascript:void(0)\" class=\"tablinks\" onclick=\"openTab(event, '")
                                .append(epic.getKey()).append("')\">").append(epic.getSummary()).append("</a></li>");
                    }
                }
                writer.append("<li><a href=\"javascript:void(0)\" class=\"tablinks\" onclick=\"openTab(event, 'Regression')\">Regressions Test</a></li>\n");
                if (hasIntegrationTest) {
                    writer.append("<li><a href=\"javascript:void(0)\" class=\"tablinks\" onclick=\"openTab(event, 'Integration')\">Integrations Test</a></li>\n");
                }
                writer.append("</ul>");
            }

            for (EpicInfo epic : epicInfo) {
                writer.append("<div id=\"").append(epic.getKey());
                if (tabReport) {
                    writer.append("\" class=\"tabcontent\">\n");
                } else {
                    writer.append("\">\n");
                }
                generateEpicInfo(writer, jiraQuery, epic);
                generateStatusSummary(data, writer, jiraQuery, epic, overallStats);
                generateDetails(writer, getDDBC(epic) + " tests", jiraQuery, testRunsInfo.stream()
                        .filter(integrationTestCond.negate()).filter(testInEpic(jiraQuery, epic)).collect(Collectors.toList()), epic);
                writer.append("</div>");
            }


            fileWriter.append("<h2>").append("Overall Test Summary</h2>");
            fileWriter.append("<table>");

            List<TestRunInfo> testsNotInEpic = testsNotInEpic(jiraQuery, epicInfo, testRunsInfo);
            long regressionFailed = testsNotInEpic.stream().filter(integrationTestCond.negate()).filter(testRunInfo -> testRunInfo.getTestState() == Active && testRunInfo.getTestResult() == TestResult.FAILED).count();
            long integrationFailed = testRunsInfo.stream().filter(integrationTestCond).filter(testRunInfo -> testRunInfo.getTestState() == Active && testRunInfo.getTestResult() == TestResult.FAILED).count();

            writeSummary(fileWriter, overallStats.inEpic, overallStats.inEpicAndSpecification, overallStats.inEpicAndPreparation, overallStats.inEpicAndActive, overallStats.inEpicFailed,
                    overallStats.newFeatures, overallStats.newStory,overallStats.bugs, overallStats.coveredCount, regressionFailed, true, integrationFailed, hasIntegrationTest);

            fileWriter.append("</table>");

            fileWriter.append(writer.toString());


            if (tabReport) {
                fileWriter.append("<div id=\"Regression\" class=\"tabcontent\">\n");
            } else {
                fileWriter.append("<div id=\"Regression\">\n");
            }
            generateDetails(fileWriter, "Regression tests", jiraQuery, testsNotInEpic, null);
            fileWriter.append("</div>\n");


            if (hasIntegrationTest) {
                if (tabReport) {
                    fileWriter.append("<div id=\"Integration\" class=\"tabcontent\">\n");
                } else {
                    fileWriter.append("<div id=\"Integration\">\n");
                }


                List<TestRunInfo> integrationTest = testRunsInfo.stream().filter(integrationTestCond).collect(Collectors.toList());
                generateDetails(fileWriter, "Integrations test", jiraQuery, integrationTest, null);
                fileWriter.append("</div>\n");
            }

            generateApplicationInfo(fileWriter);
            fileWriter.append("</body></html>");
        }

        System.out.println("Report generated at: " + file.getAbsolutePath());
    }

    private List<TestRunInfo> testsNotInEpic(JiraQuery jiraQuery, Collection<EpicInfo> epicInfo, List<TestRunInfo> list) {

        Set<TestRunInfo> foundInEpics = new HashSet<>();
        for (EpicInfo info : epicInfo) {
            for (TestRunInfo testRunInfo : list) {
                if (!foundInEpics.contains(testRunInfo)) {
                    if (inEpic(jiraQuery, info, testRunInfo)) {
                        foundInEpics.add(testRunInfo);
                    }
                }
            }
        }

        List<TestRunInfo> notInEpics = new ArrayList<>();
        for (TestRunInfo testRunInfo : list) {
            if (!foundInEpics.contains(testRunInfo) && integrationTestCond.negate().test(testRunInfo)) {
                notInEpics.add(testRunInfo);
            }
        }
        return notInEpics;
    }


    private boolean ofTypeJira(Set<String> jira, Issue issue) {
        return jira.contains(issue.getKey().substring(0, issue.getKey().indexOf('-')));
    }

    private Predicate<TestRunInfo> testInEpic(JiraQuery jiraQuery, EpicInfo epicInfo) {
        return testRunInfo -> inEpic(jiraQuery, epicInfo, testRunInfo);
    }

    private void generateEpicInfo(Writer fileWriter, JiraQuery jiraQuery, EpicInfo epicInfo) throws IOException {
        fileWriter.append("<h2>Business case</h2>\n");
        fileWriter.append("<table>");
        addNameValue(fileWriter, "key", jiraLink(jiraQuery, epicInfo, epicInfo.getKey()));
        addNameValue(fileWriter, "summary", epicInfo.getSummary());
        addNameValue(fileWriter, "due date", epicInfo.getDueDate());
        fileWriter.append("</table>\n");
    }

    private void generateApplicationInfo(Writer writer) throws IOException {

        List<ApplicationInfo> applicationInfoList = ApplicationInfoProvider.getApplicationInfoList();
        if (applicationInfoList.isEmpty()) {
            return;
        }

        if (applicationInfoList.size() > 1) {
            writer.append("<h2>Environments</h2>");
        } else {
            writer.append("<h2>Environment</h2>");
        }

        writer.append("<table>");
        for (ApplicationInfo applicationInfo : applicationInfoList) {
            writer.append("<tr><td>");
            writer.append("<table style='border-collapse:collapse;border:none;width:100%'>");
            addNameValue(writer, "name", applicationInfo.getLogicalName());
            addNameValue(writer, "environment", applicationInfo.getEnvironment());
            addNameValue(writer, "build", "<a href='http://teamcity.marketxs.com:8100/viewLog.html?buildId=" + applicationInfo.getTeamCityBuildId() + "'>" + applicationInfo.getBuild() + "</a>");
            addNameValue(writer, "host", "<a href='http://" + applicationInfo.getHost() + ":" + applicationInfo.getSystemPort() + "'>" + applicationInfo.getHost() + "</a>");
            writer.append("</table>");
            writer.append("</td></tr>");
        }
        writer.append("</table>");
    }

    private void generateStatusSummary(TestReportData reportData, Writer writer, JiraQuery jiraQuery, EpicInfo epicInfo, OverallStats overallStats) throws IOException {

        List<TestRunInfo> testRunsInfo = reportData.getTestRunsInfo();
        writer.append("<h2>").append(getDDBC(epicInfo)).append(" Test Summary</h2>");
        writer.append("<table>");


        Iterator<TestRunInfo> testRunInfoStream = testRunsInfo.stream().filter(testInEpic(jiraQuery, epicInfo)).iterator();
        long inEpic = 0;
        long inEpicFailed = 0;
        long inEpicAndSpecification = 0;
        long inEpicAndPreparation = 0;
        long inEpicAndActive = 0;
        while (testRunInfoStream.hasNext()) {
            TestRunInfo testRunInfo = testRunInfoStream.next();
            if (testRunInfo.getTestState() == Specification) {
                inEpicAndSpecification++;
            } else if (testRunInfo.getTestState() == Preparation) {
                inEpicAndPreparation++;
            } else if (testRunInfo.getTestState() == Active) {
                inEpicAndActive++;
                if (testRunInfo.getTestResult() == TestResult.FAILED) {
                    inEpicFailed++;
                }
            }

            inEpic++;
        }

        long newFeatures = jiraQuery.getEpicFeatures(epicInfo).size();
        long newStory = jiraQuery.getEpicStory(epicInfo).size();
        long bugs = jiraQuery.getEpicBugs(epicInfo).size();
        Set<String> covered = testRunsInfo.stream().filter(testRunInfo -> inEpic(jiraQuery, epicInfo, testRunInfo)).collect(Collectors.toSet()).stream().map(TestRunInfo::getLinkedJiras).flatMap(Collection::stream).collect(Collectors.toSet());
        long coveredCount = covered.size();

        overallStats.inEpic += inEpic;
        overallStats.inEpicAndSpecification += inEpicAndSpecification;
        overallStats.inEpicAndPreparation += inEpicAndPreparation;
        overallStats.inEpicAndActive += inEpicAndActive;
        overallStats.inEpicFailed += inEpicFailed;
        overallStats.newFeatures += newFeatures;
        overallStats.newStory += newStory;
        overallStats.bugs += bugs;
        overallStats.coveredCount += coveredCount;

        writeSummary(writer, inEpic, inEpicAndSpecification, inEpicAndPreparation, inEpicAndActive, inEpicFailed, newFeatures, newStory, bugs, coveredCount, 0, false, 0, false);
        addJiraList(writer, jiraQuery, epicInfo, jiraQuery.getEpicFeatures(epicInfo).stream().collect(Collectors.toList()), "New Features", reportData);
        addJiraList(writer, jiraQuery, epicInfo, jiraQuery.getEpicStory(epicInfo).stream().collect(Collectors.toList()), "New Story", reportData);
        addJiraList(writer, jiraQuery, epicInfo, jiraQuery.getEpicBugs(epicInfo).stream().collect(Collectors.toList()), "Bug fixes", reportData);
        writer.append("</table>");
    }

    private void addJiraList(Writer writer, JiraQuery jiraQuery, EpicInfo epicInfo, List<Issue> issues, String identifier, TestReportData reportData) throws IOException {
        writer.append("<tr>");
        writer.append("<td class='area'>").append(identifier).append("</td>");
        Collections.sort(issues, (o1, o2) -> {
            String key1 = o1.getKey();
            String key2 = o2.getKey();
            String[] split1 = key1.split("-");
            String[] split2 = key2.split("-");
            int i = split1[0].compareTo(split2[0]);
            if (i == 0) {
                i = Integer.valueOf(split1[1]).compareTo(Integer.valueOf(split2[1]));
            }
            return i;
        });

        writer.append("<td colSpan='4'>");
        writer.append("<table style='border-collapse:collapse;border:none;width:100%'>");
        writer.append("<thead><th style='width:100px'>Key</th><th>Issue state</th><th>Summary</th><th style='width:60px;text-align:center;'>Tests</th>").append("</th></thead>");
        for (Issue issue : issues) {
            writer.append("<tr>");
            writer.append("<td>").append(jiraLink(jiraQuery, epicInfo, issue.getKey()));
            writer.append("</td><td>").append(issue.getStatus().getName()).append("</td>");
            writer.append("</td><td>").append(issue.getSummary()).append("</td>");

            List<TestRunInfo> testInfos = reportData.getTestInfos(issue.getKey());
            int count = testInfos == null ? 0 : testInfos.size();
            TestResult result = null;
            if (count > 0) {
                result = TestResult.SKIPPED;
                for (TestRunInfo testInfo : testInfos) {
                    if (testInfo.getTestState() == TestState.Active && result != TestResult.FAILED) {
                        if (testInfo.getTestResult() == TestResult.FAILED) {
                            result = TestResult.FAILED;
                        } else if (testInfo.getTestResult() == TestResult.PASSED) {
                            result = TestResult.PASSED;
                        }
                    }
                }
            }
            writer.append("<td style='text-align:center;' class='").append(result == null ? "" : result.toCss()).append("'> ").append(String.valueOf(reportData.getTestCount(issue.getKey()))).append("</td>");
            writer.append("</tr>");
        }
        writer.append("</table>");

        writer.append("</td></tr>");
    }

    private void writeSummary(Writer writer, long inEpic, long inEpicAndSpecification, long inEpicAndPreparation, long inEpicAndActive,
                              long inEpicFailed, long newFeatures, long newStory, long bugs, long coveredCount, long regressionFailed, boolean reportRegression,
                              long integrationFailed, boolean reportIntegration) throws IOException {

        addNameCountPercentage(writer, "New features", newFeatures, null, null, "Scope", 5);
        addNameCountPercentage(writer, "New story", newStory, null, null, null, 0);
        addNameCountPercentage(writer, "Bug fixes", bugs, null, null, null, 0);
        addNameCountPercentage(writer, "Total ", newFeatures + newStory + bugs, null, null, null, 0);
        addNameCountPercentage(writer, "Automated test coverage", coveredCount, null, (newFeatures + newStory + bugs) == 0 ? 0 : 100 * coveredCount / (newFeatures + newStory + bugs), null, 0);

        addNameCountPercentage(writer, "In specification", inEpicAndSpecification, null, null, "Tests", 4);
        addNameCountPercentage(writer, "In preparation", inEpicAndPreparation, null, null, null, 0);
        addNameCountPercentage(writer, "Active", inEpicAndActive, inEpicAndActive == inEpic ? "passed" : "skipped", inEpic == 0 ? 0 : 100 * inEpicAndActive / inEpic, null, 0);
        addNameCountPercentage(writer, "Total", inEpic, null, null, null, 0);

        addNameCountPercentage(writer, "Scope tests failed", inEpicFailed, inEpicFailed > 0 ? "failed" : "passed", null, "Execution", 4);

        boolean included = false;

        if (reportRegression) {
            addNameCountPercentage(writer, "Regressions", regressionFailed, regressionFailed > 0 ? "failed" : "passed", null, null, 0);
            included = true;
        }
        if (reportIntegration) {
            addNameCountPercentage(writer, "Integrations", integrationFailed, integrationFailed > 0 ? "failed" : "passed", null, null, 0);
            included = true;
        }

        if (!included) {
            writer.append("<tr><td></td></tr>");
        }

    }

    private String getDDBC(EpicInfo epicInfo) {
        return epicInfo.getSummary();
    }

    private void addNameValue(Writer fileWriter, String name, String value) throws IOException {
        fileWriter.append("<tr><td class='nameColumn'>").append(name).append("</td><td>").append(value).append("</td></tr>");
    }

    private void addNameCountPercentage(Writer fileWriter, String name, long value, String valueCss, Long percentage, String area, int areaRowSpan) throws IOException {
        fileWriter.append("<tr>");
        if (areaRowSpan > 0) {
            fileWriter.append("<td style='width:150px' class='area' rowSpan='").append(String.valueOf(areaRowSpan)).append("'>").append(area).append("</td>");
        }
        fileWriter.append("<td class='nameColumn'>").append(name).append("</td>");

        fileWriter.append("<td class='countColumn");
        if (valueCss != null) {
            fileWriter.append(" ").append(valueCss).append("'>");
        } else {
            fileWriter.append("'>");
        }
        fileWriter.append(String.valueOf(value)).append("</td><td class='percentage'>");
        if (percentage != null) {
            fileWriter.append(String.valueOf(percentage)).append(" %");
        }
        fileWriter.append("</td><td class='filler'/></tr>");
    }

    private void generateDetails(Writer writer, String title, JiraQuery jiraQuery, List<TestRunInfo> items, EpicInfo epicInfo) throws IOException {
        writer.append("<h2>").append(title).append("</h2>\n");
        writer.append("<table><thead><tr><th>Area</th><th>Test</th><th>Issues</th><th>State</th><th>Test Result</th></tr><thead><tbody>\n");
        String currentClass = null;
        Map<String, Integer> rowSpanMap = new HashMap<>();
        String lastClassName = null;
        int count = 0;
        for (TestRunInfo test : items) {
            String className = test.getClassName();
            if (lastClassName == null || className.equals(lastClassName)) {
                count++;
            } else {
                rowSpanMap.put(lastClassName, count);
                count = 1;
            }
            lastClassName = className;
        }
        rowSpanMap.put(lastClassName, count);
        for (TestRunInfo test : items) {
            writer.append("<tr>");
            if (!test.getClassName().equals(currentClass)) {
                Integer rowSpan = rowSpanMap.get(test.getClassName());
                if (rowSpan > 1) {
                    writer.append("<td class='areaColumn' rowspan='").append(String.valueOf(rowSpan)).append("'>");
                } else {
                    writer.append("<td class='areaColumn'>");
                }
                writer.append(test.getClassName().substring(getPrefixLength())).append("</td>");
            }
            currentClass = test.getClassName();
            writer.append("<td class='testColumn'><a href='").append(testUrl(test.getMethod())).append("' style=\"text-decoration:none\">").append(readableName(test.getName())).append("</a></td><td class='jiraColumn'>");
            writer.append(jiraLink(jiraQuery, epicInfo, test.getLinkedJiras())).append("</td>");
            boolean active = test.getTestState() == TestState.Active;
            writer.append("<td class='").append(active ? "" : "skipped").append("'>");
            writer.append(test.getTestState().toString()).append("</td><td style='text-align:center' class='").append(active ? test.getResultCss() : "").append("'>").append(getTestResultCell(test.getTestResult())).append("</td></tr>");
        }
        writer.append("</tbody></table>");
    }


    private int getPrefixLength() {
        return packagePrefix.length();
    }

    private String getTestResultCell(TestResult testResult) {
        return testResult.toString().toLowerCase();
    }

    private String jiraLink(JiraQuery jiraQuery, EpicInfo epicInfo, List<String> keys) throws IOException {
        StringBuilder jiras = new StringBuilder();
        for (String key : keys) {
            jiras.append(jiraLink(jiraQuery, epicInfo, key));
        }
        return jiras.toString();
    }

    private String jiraLink(JiraQuery jiraQuery, EpicInfo epicInfo, String key) throws IOException {
        Issue issue = jiraQuery.getIssue(epicInfo, key);
        if (issue != null) {
            Status status = issue.getStatus();
            String iconUrl = status.getIconUrl();
            boolean closed = status.getName().equals("Closed") || status.getName().equals("Canceled");
            return "<img src='" + iconUrl + "' title='" + status.getName() + "'/>" + "<a class='jira" + (closed ? " closed" : "") + "' href='https://jira.markit.com/browse/" + key + "'>" + key + "</a>";
        } else {
            return key == null ? "" : "<a class='jira' href='https://jira.markit.com/browse/" + key + "'>" + key + "</a>";
        }
    }

    private String testUrl(Method testMethod) {
        return testBaseUrl + "/" + buildId + testPath + "/" + testMethod.getDeclaringClass().getName() + ".html#" + testMethod.getName();
    }

    private boolean inEpic(JiraQuery jiraQuery, EpicInfo epicInfo, TestRunInfo testRunInfo) {
        return jiraQuery.isInEpic(epicInfo, testRunInfo.getLinkedJiras());
    }

    private class OverallStats {
        private long inEpic;
        private long inEpicAndActive;
        private long inEpicFailed;
        private long bugs;
        private long newFeatures;
        private long newStory;
        private long coveredCount;
        private long inEpicAndPreparation;
        private long inEpicAndSpecification;
    }
}
