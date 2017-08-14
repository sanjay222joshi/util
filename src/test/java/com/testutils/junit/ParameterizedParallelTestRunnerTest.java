package com.testutils.junit;

import com.markit.n6platform.testframework.TestBase;
import com.testutils.junit.ParameterizedParallelTestRunner;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(ParameterizedParallelTestRunner.class)
public class ParameterizedParallelTestRunnerTest extends TestBase {

    private String param;

    public ParameterizedParallelTestRunnerTest(String param) {
        this.param = param;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<Object[]> objects = new ArrayList<>();
        objects.add(new Object[]{"test1"});
        objects.add(new Object[]{"test2"});
        objects.add(new Object[]{"test3"});
        return objects;
    }

    @Test
    public void test() {
        String thread = Thread.currentThread().getName();
        System.out.println("Test with " + param + " run by " + thread);
        Assert.assertThat(thread, StringContains.containsString("ParallelTestRunner-"));
    }
}