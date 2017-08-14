package com.testutils.testinfo.internal;

import com.markit.n6platform.util.basic.StringUtils;
import com.testutils.testinfo.TestInfo;
import com.testutils.testinfo.TestState;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class TestRunInfo {
    private final String name;
    private final String className;
    private final TestInfo testInfo;
    private final TestResult testResult;
    private final Method method;

    public TestRunInfo(String className, String name, TestInfo testInfo, TestResult testResult, Method method) {
        this.className = className;
        this.name = name;
        this.testInfo = testInfo;
        this.testResult = testResult;
        this.method = method;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public TestInfo getTestInfo() {
        return testInfo;
    }

    public TestResult getTestResult() {
        return testResult;
    }

    public TestState getTestState() {
        return testInfo == null? TestState.Active :testInfo.state();
    }

    public List<String> getLinkedJiras() {
        return testInfo == null? Collections.EMPTY_LIST : StringUtils.split(testInfo.devJira(),',');
    }

    public String getResultCss() {
        return testResult.toCss();
    }

    public Method getMethod() {
        return method;
    }

    public String getQualifiedName() {
        return className + "," + name;
    }
}
