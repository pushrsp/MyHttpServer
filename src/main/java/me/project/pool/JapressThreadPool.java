package me.project.pool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class JapressThreadPool implements Executor {

    private static final Runnable SHUTDOWN_TASK = () -> { };

    private final BlockingQueue<Runnable> bq = new LinkedTransferQueue<>();
    private final Thread[] threads;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public JapressThreadPool(int numThreads) {
        this.threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                while (true) {
                    try {
                        Runnable task = bq.take();
                        if(task == SHUTDOWN_TASK) {
                            break;
                        } else {
                            task.run();
                        }
                    } catch (Throwable t) {
                        if(!(t instanceof InterruptedException)) {
                            System.err.println("Unexpected exception: ");
                            t.printStackTrace();
                        }
                    }
                }

                System.err.println("Shutting thread " + Thread.currentThread().getName());
            });
        }
    }

    @Override
    public void execute(Runnable command) {
        if(started.compareAndSet(false, true)) {
            for (Thread thread : this.threads) {
                thread.start();
            }
        }

        if(this.shutdown.get()) {
            throw new RejectedExecutionException();
        }

        bq.add(command);

        if(shutdown.get()) {
            bq.remove(command);
        }
    }

    public void shutdown() {
        if(this.shutdown.compareAndSet(false, true)) {
            for (int i = 0; i < this.threads.length; i++) {
                bq.add(SHUTDOWN_TASK);
            }
        }

        for (Thread thread : this.threads) {
            do {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // do not propagate to prevent incomplete shutdown.
                }
            } while (thread.isAlive());
        }
    }
}
