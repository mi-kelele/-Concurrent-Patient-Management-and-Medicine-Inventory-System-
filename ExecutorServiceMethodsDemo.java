package Problem2;

import java.util.concurrent.*;

/**
 * ============================================================================
 * EXECUTOR SERVICE METHODS DEMO (DAY-1 BEGINNER PLAYGROUND)
 * ============================================================================
 * Run this file to see each method of ExecutorService in action with printouts:
 *
 * 1. Executors.newFixedThreadPool(2)
 * 2. execute(Runnable)           -> Fire and forget (no return value)
 * 3. submit(Callable)            -> Returns a Future (claim ticket for result)
 * 4. future.get()                -> Waits for worker to finish and gets answer
 * 5. shutdown()                  -> Polite stop: no new tasks accepted
 * 6. awaitTermination(sec, unit) -> Waits for all workers to leave building
 */
public class ExecutorServiceMethodsDemo {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("=== EXECUTOR SERVICE METHODS PLAYGROUND ===\n");

        // 1. FACTORY: Create a pool of 2 workers
        System.out.println("[Step 1] Creating a pool with 2 worker threads...");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 2. METHOD: execute(Runnable)
        // Use this when you do NOT need any answer back
        System.out.println("\n[Step 2] Calling executor.execute()...");
        executor.execute(() -> {
            System.out.println("  -> (execute) Hello from " + Thread.currentThread().getName()
                    + "! I do work and return nothing.");
        });

        // 3. METHOD: submit(Callable)
        // Use this when you WANT a return value back from the worker!
        System.out.println("\n[Step 3] Calling executor.submit() to calculate 42 * 2...");
        Future<Integer> claimTicket = executor.submit(() -> {
            System.out.println("  -> (submit) Worker " + Thread.currentThread().getName()
                    + " is doing a heavy calculation for 1 second...");
            Thread.sleep(1000); // simulate thinking time
            return 42 * 2;      // returning the answer
        });

        // While worker is calculating, the main program can do other things!
        System.out.println("  -> Main thread is NOT blocked yet! Doing other work...");

        // 4. METHOD: future.get()
        // This BLOCKS until the worker finishes and hands us the answer
        System.out.println("  -> Now main thread calls claimTicket.get() to get the result...");
        Integer result = claimTicket.get();
        System.out.println("  -> Result received from worker: " + result);

        // 5. METHOD: shutdown()
        // Tells the pool: "No more new tasks allowed, finish existing tasks."
        System.out.println("\n[Step 4] Calling executor.shutdown()...");
        executor.shutdown();
        System.out.println("  -> Is executor shutdown? " + executor.isShutdown());

        // 6. METHOD: awaitTermination()
        // Waits up to 3 seconds for all workers to wrap up
        System.out.println("\n[Step 5] Waiting for termination...");
        boolean allFinished = executor.awaitTermination(3, TimeUnit.SECONDS);
        System.out.println("  -> Did all workers finish in time? " + allFinished);
        System.out.println("  -> Is executor fully terminated? " + executor.isTerminated());

        System.out.println("\n=== DEMO FINISHED SUCCESSFULLY ===");
    }
}

