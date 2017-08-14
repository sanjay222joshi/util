package com.testutils.junit;

import org.junit.runners.Parameterized;

public class ParameterizedParallelTestRunner extends Parameterized {
    /**
     * Only called reflectively. Do not use programmatically.
     *
     * @param klass
     */
    public ParameterizedParallelTestRunner(Class<?> klass) throws Throwable {
        super(klass);
        setScheduler(new ParallelRunnerScheduler());
    }
}
