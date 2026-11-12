package Problem2;

import java.util.concurrent.*;

public class ExecutorServiceMethodsDemo {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("=== EXECUTOR SERVICE METHODS PLAYGROUND ===\n");

        
        System.out.println("[Step 1] Creating a pool with 2 worker threads...");
        ExecutorService executor = Executors.newFixedThreadPool(2);

    
        System.out.println("\n[Step 2] Calling executor.execute()...");
        executor.execute(() -> {
            System.out.println("  -> (execute) Hello from " + Thread.currentThread().getName()
                    + "! I do work and return nothing.");
        });

        
        System.out.println("\n[Step 3] Calling executor.submit() to calculate 42 * 2...");
        Future<Integer> claimTicket = executor.submit(() -> {
            System.out.println("  -> (submit) Worker " + Thread.currentThread().getName()
                    + " is doing a heavy calculation for 1 second...");
            Thread.sleep(1000); 
            return 42 * 2;     
        });

        System.out.println("  -> Main thread is NOT blocked yet! Doing other work...");

        System.out.println("  -> Now main thread calls claimTicket.get() to get the result...");
        Integer result = claimTicket.get();
        System.out.println("  -> Result received from worker: " + result);

        
        System.out.println("\n[Step 4] Calling executor.shutdown()...");
        executor.shutdown();
        System.out.println("  -> Is executor shutdown? " + executor.isShutdown());

        
        System.out.println("\n[Step 5] Waiting for termination...");
        boolean allFinished = executor.awaitTermination(3, TimeUnit.SECONDS);
        System.out.println("  -> Did all workers finish in time? " + allFinished);
        System.out.println("  -> Is executor fully terminated? " + executor.isTerminated());

        System.out.println("\n=== DEMO FINISHED SUCCESSFULLY ===");
    }
}

