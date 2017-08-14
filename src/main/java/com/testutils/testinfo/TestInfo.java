package com.testutils.testinfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TestInfo {
    /**
     * Status of the test
     */
    TestState state();

    /**
     * JIRA key containing the requirement which is being tested
     */
    String devJira() default "";

    /**
     * JIRA key of the test task (optional)
     */
    @Deprecated
    String qaJira() default "";

    boolean integration() default false;
}
