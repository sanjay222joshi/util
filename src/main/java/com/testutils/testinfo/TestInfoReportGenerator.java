package com.testutils.testinfo;

import com.google.common.base.Joiner;
import com.testutils.testinfo.internal.HtmlReportGenerator;
import com.testutils.testinfo.internal.JiraQuery;
import com.testutils.testinfo.internal.JunitTestReportParser;
import com.testutils.testinfo.internal.TestReportData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;


public class TestInfoReportGenerator {


    private final File xmlInputLocation;
    private final String ddbc;
    private final String release;
    private final String projects;
    private final String customQuery;

    private final JunitTestReportParser parser = new JunitTestReportParser();
    private final HtmlReportGenerator htmlReportGenerator;

    public TestInfoReportGenerator(String reportName, String buildTypeId, String buildId,
                                   String packagePrefix, File xmlInputLocation,
                                   String ddbc, String release, String projects, String customQuery, boolean tabReport) {
        this.xmlInputLocation = xmlInputLocation;
        this.ddbc = ddbc;
        this.release = release;
        this.projects = projects;
        this.customQuery = customQuery;
        this.htmlReportGenerator = new HtmlReportGenerator(reportName, buildTypeId, buildId, packagePrefix, tabReport);
    }

    public static void main(final String[] args) throws Exception {
        if (args.length < 10) {
            System.out.println("Must specify reportName, buildTypeId, buildId, test packagePrefix, report dir, ddbc, fixVersion, project, customJiraFilter, tabReport");
            return;
        }
        String reportName = args[0];
        String buildTypeId = args[1];
        String buildId = args[2];
        String testPackagePrefix = args[3];
        File reportDir = getFile(args[4]);
        String ddbc = args[5];
        String fixVersion = args[6];
        String project = args[7];
        String customJiraFilter = args[8];
        boolean tabReport = Boolean.valueOf(args[9]);

        new TestInfoReportGenerator(reportName, buildTypeId, buildId, testPackagePrefix,
                reportDir, ddbc, fixVersion, project, customJiraFilter, tabReport).generateReport();
    }

    public void generateReport() throws Exception {
        TestReportData data = parser.parse(xmlInputLocation);
        if (data != null) {
            File qaReportDir = createQaReportDir();
            JiraQuery queryJiraWith = new JiraQuery().filter(ddbc, release, projects, customQuery, data.getLinkedJiras());
            htmlReportGenerator.generate(qaReportDir, data, queryJiraWith);
        } else {
            System.out.println("No report found at :" + xmlInputLocation);
        }
    }

    private File createQaReportDir() throws IOException {
        File qaReportDir = new File(new File(xmlInputLocation.getParentFile(), "reports"), "qa");
        qaReportDir.mkdir();
        copyResourceTo(qaReportDir, "css", "qaReport.css", "tab.css");
        copyResourceTo(qaReportDir, "scripts", "tab.js");
        copyResourceTo(qaReportDir, "images", "check.png", "cross.png");
        return qaReportDir;
    }


    private void copyResourceTo(File qaReportDir, String dir, String... filesNames) throws IOException {
        File directory = new File(qaReportDir, dir);
        directory.mkdir();
        for (String fileName : filesNames) {
            File cssFile = new File(directory, fileName);
            InputStream src = TestInfo.class.getResourceAsStream(Joiner.on('/').join(dir, fileName));
            Files.copy(src, cssFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static File getFile(String arg) {
        final File file = new File(arg);
        if (!file.exists()) {
            System.out.println("directory root does not exist " + file.getAbsolutePath());
            return null;
        }
        return file;
    }


}

