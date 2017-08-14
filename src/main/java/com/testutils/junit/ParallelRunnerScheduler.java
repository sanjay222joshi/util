package com.testutils.junit;

import org.junit.runners.model.RunnerScheduler;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class ParallelRunnerScheduler implements RunnerScheduler {

    private final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger i = new AtomicInteger();
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ParallelTestRunner-" + i.getAndIncrement());
        }
    });

    @Override
    public void schedule(Runnable childStatement) {
        executor.submit(childStatement);
    }

    @Override
    public void finished() {

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(20));
        boolean completed;
        executor.shutdown();
        try {
            completed = executor.awaitTermination(TimeUnit.DAYS.toMillis(1), MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            completed = false;
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeException(new InterruptedException("test runner shutdown was interrupted"));
        }

        if (!completed)
            throw new RuntimeException(new TimeoutException("test runner shutdown timed out before tests completed"));
    }
}