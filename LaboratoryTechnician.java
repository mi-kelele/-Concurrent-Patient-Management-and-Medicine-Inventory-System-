package Problem2;

import java.util.concurrent.TimeUnit;

/**
  Responsibilities (Task f):
  1. Pulls diagnostic requests from the shared BlockingQueue (Consumer pattern).
  2. Processes tests concurrently with other technicians.
  3. Records completed test findings back into the patient's record.
 */
public class LaboratoryTechnician implements Runnable {

    private final String name;
    private final HospitalSystem hospital;
    private volatile boolean keepRunning = true;
    private int testsProcessed = 0;

    public LaboratoryTechnician(String name, HospitalSystem hospital) {
        this.name = name;
        this.hospital = hospital;
    }

    public void stopWorker() {
        this.keepRunning = false;
    }

    @Override
    public void run() {
        System.out.println("[LAB TECH STARTED] " + name + " ready to process laboratory requests.");

        while (keepRunning || !hospital.getLabQueue().isEmpty()) {
            try {
                // Poll from queue. If empty, wait up to 500ms before checking loop condition
                LabRequest request = hospital.getLabQueue().poll(500, TimeUnit.MILLISECONDS); // 500. 

                if (request != null) {
                    // Simulate lab analysis time
                    Thread.sleep(40); 

                    String result = "Hemoglobin 14.2 g/dL, WBC 6,500/mcL (Normal)";
                    request.completeTest(name, result);

                    // Record result in patient record
                    Patient patient = hospital.getPatient(request.getPatientId());
                    if (patient != null) {
                        patient.addClinicalNote(name, "Lab Result for " + request.getTestType() + ": " + result);
                    }

                    testsProcessed++; // +1
                    System.out.println("[LAB PROCESSED] " + name + " finished test #"
                            + request.getRequestId() + " for Patient #" + request.getPatientId());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[LAB TECH FINISHED] " + name + " shift ended. Total tests analyzed: " + testsProcessed);
    }

    public int getTestsProcessed() { // getter. 
        return testsProcessed;
    }
}

