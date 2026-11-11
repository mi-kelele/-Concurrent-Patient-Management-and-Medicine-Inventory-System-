package Problem2;

/**
 * ============================================================================
 * LAB REQUEST CLASS
 * ============================================================================
 * Represents a laboratory diagnostic request.
 *
 * Flow (Task f):
 * 1. A Nurse submits this request into the hospital's BlockingQueue.
 * 2. A Laboratory Technician picks it up from the queue and processes it.
 * 3. A Doctor retrieves the finished result and adds it to the patient record.
 */
public class LabRequest {

    private final int requestId;
    private final int patientId;
    private final String testType;
    private final String requestingNurse;

    // Filled in by the lab technician
    private String processingTech;
    private String testResult;
    private volatile boolean completed = false;

    public LabRequest(int requestId, int patientId, String testType, String requestingNurse) {
        this.requestId = requestId;
        this.patientId = patientId;
        this.testType = testType;
        this.requestingNurse = requestingNurse;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getPatientId() {
        return patientId;
    }

    public String getTestType() {
        return testType;
    }

    public String getRequestingNurse() {
        return requestingNurse;
    }

    // Called by the Lab Technician when the test is analyzed
    public synchronized void completeTest(String techName, String result) {
        this.processingTech = techName;
        this.testResult = result;
        this.completed = true;
        notifyAll(); // Wake up any thread waiting for this specific result!
    }

    // Called by Doctor to wait for and retrieve the result
    public synchronized String awaitResult(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!completed) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return "Pending (Test still in laboratory processing)";
            }
            wait(remaining);
        }
        return testResult + " (Processed by " + processingTech + ")";
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getTestResult() {
        return testResult;
    }

    public String getProcessingTech() {
        return processingTech;
    }
}

