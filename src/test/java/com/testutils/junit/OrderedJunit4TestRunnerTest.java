package com.testutils.junit;

import com.markit.n6platform.testframework.TestBase;
import com.testutils.junit.Order;
import com.testutils.junit.OrderedJunit4TestRunner;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(OrderedJunit4TestRunner.class)
public class OrderedJunit4TestRunnerTest extends TestBase {

    public static List<Integer> order = new ArrayList<>();

    @BeforeClass
    public static void setUp() {
        order.clear();
    }

    @AfterClass
    public static void tearDown() {
        Assert.assertEquals(Arrays.asList(3, 2, 1), order);
    }

    @Order(order = 3)
    @Test
    public void test1() {
        order.add(1);
    }

    @Order(order = 2)
    @Test
    public void test2() {
        order.add(2);
    }

    @Order(order = 1)
    @Test
    public void test3() {
        order.add(3);
    }
}