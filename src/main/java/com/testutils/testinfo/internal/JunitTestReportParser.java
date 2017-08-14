package com.testutils.testinfo.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.markit.n6platform.util.basic.ExceptionUtils;
import com.markit.n6platform.util.basic.ReflectionUtils;
import com.testutils.testinfo.TestInfo;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class JunitTestReportParser {

    private final Pattern PARAMETERIZED_METHOD_PATTERN = Pattern.compile("(.*)\\[(.*)\\]");
    private final SAXParserFactory spf = SAXParserFactory.newInstance();

    public TestReportData parse(File xmlInputLocation) throws Exception {
        SAXParser parser = spf.newSAXParser();

        XMLReader reader = parser.getXMLReader();
        if (xmlInputLocation == null || !xmlInputLocation.exists()) {
            System.out.println("JUnit test results not found" + (xmlInputLocation != null ? " at " + xmlInputLocation.getAbsolutePath() : ""));
            return null;
        }

        TestReportData reportData = new TestReportData();
        for (File file : getTestXmlFiles(xmlInputLocation)) {
            parseFile(file, reader, reportData);
        }
        return reportData;
    }

    private List<File> getTestXmlFiles(File loc) throws IOException {
        return Files.walk(loc.toPath())
                .filter(fd -> Files.isRegularFile(fd))
                .filter(fd -> fd.toString().endsWith("xml"))
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    private void parseFile(File xmlFile, XMLReader reader, TestReportData reportData) throws IOException, SAXException {
        final InputSource inputSource = new InputSource(new BufferedInputStream(new FileInputStream(xmlFile)));
        DefaultHandler handler = new DefaultHandler() {

            private String testCase;
            private String className;
            private TestResult result;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes atts) {
                if ("testcase".equals(qName)) {
                    className = atts.getValue("classname");
                    testCase = atts.getValue("name");
                    result = TestResult.PASSED;
                } else if ("failure".equals(qName)) {
                    result = TestResult.FAILED;
                } else if ("skipped".equals(qName)) {
                    result = TestResult.SKIPPED;
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                if ("testcase".equals(qName)) {
                    Class<?> aClass = ReflectionUtils.forName(className);
                    try {

                        String methodName;
                        String methodNameInReport;
                        Matcher matcher = PARAMETERIZED_METHOD_PATTERN.matcher(testCase);
                        if (matcher.find()) {
                            methodName = matcher.group(1);
                            methodNameInReport = matcher.group(1) + "_" + matcher.group(2).replaceAll("[,\\s]+", "_");
                        } else {
                            methodName = methodNameInReport = testCase;
                        }

                        Method method = ReflectionUtils.getDeclaredMethod(aClass, methodName);
                        TestInfo testInfo = method == null ? null : method.getAnnotation(TestInfo.class);
                        reportData.addTestRunInfo(new TestRunInfo(className, methodNameInReport, testInfo, result, method));
                    } catch (Exception e) {
                        //exception occur when not able to resolve method name
                        Throwable rootCause = ExceptionUtils.getRootCause(e, 100);
                        String msg = rootCause == null ? e.getMessage() : rootCause.getMessage();
                        System.out.println("------------------Skipped test from report not able to find test method : " + msg);
                    }
                }
            }
        };
        reader.setContentHandler(handler);
        reader.parse(inputSource);
    }



}
