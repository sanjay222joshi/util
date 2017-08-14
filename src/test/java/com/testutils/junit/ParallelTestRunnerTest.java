package com.testutils.junit;

import com.markit.n6platform.testframework.TestBase;
import com.testutils.junit.ParallelTestRunner;

import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ParallelTestRunner.class)
public class ParallelTestRunnerTest extends TestBase {

    @Test
    public void test1() throws InterruptedException {
        assertIt("test1");
    }


    @Test
    public void test2() throws InterruptedException {
        assertIt("test2");
    }


    @Test
    public void test3() throws InterruptedException {
        assertIt("test3");
    }

    private void assertIt(String methodName) {
        String thread = Thread.currentThread().getName();
        System.out.println(methodName + " run by " + thread);
        Assert.assertThat(thread, StringContains.containsString("ParallelTestRunner-"));
    }
}
