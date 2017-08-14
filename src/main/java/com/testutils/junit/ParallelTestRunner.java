package com.testutils.junit;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;


public class ParallelTestRunner extends BlockJUnit4ClassRunner {

    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     * @throws InitializationError if the test class is malformed.
     */
    public ParallelTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
        setScheduler(new ParallelRunnerScheduler());
    }
}
