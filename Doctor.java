package Problem2;

import java.util.Random;
import java.util.concurrent.Semaphore;


public class Doctor implements Runnable { 

    private final String name;
    private final HospitalSystem hospital;
    private final int startPatientId; // 1
    private final int endPatientId; //20
    private final Random random = new Random(); 

    public Doctor(String name, HospitalSystem hospital, int startPatientId, int endPatientId) {
        this.name = name;
        this.hospital = hospital;
        this.startPatientId = startPatientId;
        this.endPatientId = endPatientId;
    }

    @Override
    public void run() {
        System.out.println("[DOCTOR STARTED] " + name + " beginning patient rounds for Patients #"
                + startPatientId + " through #" + endPatientId);

        Semaphore mriSuite = hospital.getSpecializedEquipmentPermits(); // shared resources. 

        for (int pId = startPatientId; pId <= endPatientId; pId++) {
            Patient patient = hospital.getPatient(pId);
            if (patient == null) continue;

            // Scenario: Certain patients require the specialized MRI / Diagnostic Scanner
            if (pId % 5 == 0) { 
                System.out.println("[SEMAPHORE WAITING] " + name + " requesting access to Specialized MRI Suite for Patient #" + pId);
                try {
                    // Acquire permit from Semaphore (blocks if both suites are currently occupied!)
                    mriSuite.acquire();
                    System.out.println("[SEMAPHORE ACQUIRED] " + name + " ENTERED MRI Suite with Patient #"
                            + pId + " (Available permits left: " + mriSuite.availablePermits() + ")");

                    // Simulate MRI scanning time
                    Thread.sleep(60);

                    patient.addClinicalNote(name, "MRI Scan completed: No acute abnormalities detected.");

                } catch (InterruptedException e) { 
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    // ALWAYS release the permit in finally block so waiting doctors can enter!
                    mriSuite.release();
                    System.out.println("[SEMAPHORE RELEASED] " + name + " LEFT MRI Suite. (Permits now: "
                            + mriSuite.availablePermits() + ")");
                }
            } else {
                // Regular clinical consultation
                patient.addClinicalNote(name, "Routine consultation: Patient responding well to treatment.");
            }

            try {
                Thread.sleep(15 + random.nextInt(25)); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[DOCTOR FINISHED] " + name + " finished all consultations.");
    }
}

