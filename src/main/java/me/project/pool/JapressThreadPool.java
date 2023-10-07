package me.project.pool;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class JapressThreadPool {

    private final ExecutorService executor;

    private final Duration shutdown;

    public JapressThreadPool(int numberOfThreads, String namePrefix, Duration shutdown) {
        AtomicInteger threadCount = new AtomicInteger(1);
        this.executor = Executors.newFixedThreadPool(numberOfThreads, runnable -> new Thread(runnable, namePrefix + " " + threadCount.getAndIncrement()));
        this.shutdown = shutdown;
    }

    public boolean shutdown() {
        executor.shutdownNow();

        try {
            return executor.awaitTermination(shutdown.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Ignore and exit
            return false;
        }
    }

    public Future<?> submit(Runnable runnable) {
        return this.executor.submit(runnable);
    }
}
