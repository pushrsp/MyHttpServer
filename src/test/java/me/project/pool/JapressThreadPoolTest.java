package me.project.pool;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

class JapressThreadPoolTest {
    @Test
    public void submittedTasksAreExecuted() throws Exception {
        JapressThreadPool executor = new JapressThreadPool(2);
        int numTasks = 100;
        CountDownLatch latch = new CountDownLatch(numTasks);

        try {
            for (int i = 0; i < numTasks; i++) {
                final  int ii = i;
                executor.execute(() -> {
                    System.err.println("Thread: \\`" + Thread.currentThread().getName() + "\\` executes a task " + ii);

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    latch.countDown();
                });
            }

            //latch.await();
        } finally {
            executor.shutdown();
        }
    }
}
