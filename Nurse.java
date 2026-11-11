package Problem2;

import java.util.Random;

/**
 * ============================================================================
 * NURSE WORKER (Thread / Runnable)
 * ============================================================================
 * Responsibilities:
 * 1. Checks on patients and updates their vital signs.
 * 2. Submits laboratory test requests into the shared BlockingQueue.
 */
public class Nurse implements Runnable { // run() pool.submit(Nurse)

    private final String name;
    private final HospitalSystem hospital;
    private final int startPatientId;
    private final int endPatientId;
    private final Random random = new Random();

    public Nurse(String name, HospitalSystem hospital, int startPatientId, int endPatientId) {
        this.name = name;
        this.hospital = hospital;
        this.startPatientId = startPatientId;
        this.endPatientId = endPatientId;
    }

    @Override
    public void run() {
        System.out.println("[NURSE STARTED] " + name + " assigned to Patients #"
                + startPatientId + " through #" + endPatientId);

        for (int pId = startPatientId; pId <= endPatientId; pId++) {
            Patient patient = hospital.getPatient(pId);
            if (patient == null) continue;

            // 1. Take and update vitals
            String bp = (110 + random.nextInt(30)) + "/" + (70 + random.nextInt(20));
            double temp = 36.5 + (random.nextInt(25) / 10.0);
            int hr = 65 + random.nextInt(35);

            patient.updateVitals(name, bp, temp, hr);

            // 2. Submit a lab request for some patients into the BlockingQueue (Task f)
            if (pId % 3 == 0) {
                LabRequest request = new LabRequest(pId * 10, pId, "Complete Blood Count (CBC)", name);
                try {
                    // .put() blocks if the queue is full (Producer pattern)
                    hospital.getLabQueue().put(request);
                    System.out.println("[LAB SUBMITTED] " + name + " queued lab test for Patient #" + pId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Simulate small delay between patient checkups
            try {
                Thread.sleep(10 + random.nextInt(20)); // 10+0 - 10+19
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[NURSE FINISHED] " + name + " completed patient rounds.");
    }
}

