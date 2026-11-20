package Problem2;

import java.util.Random;
/*
Responsibilities (Task d):
  1. Receives medication orders for patients.
  2. Dispenses doses from the shared MedicineInventory.
  3. Safely handles inventory limits without letting stock become negative,
     even when multiple pharmacists process orders at the exact same moment.
*/

public class Pharmacist implements Runnable { // .sub 

    private final String name;
    private final HospitalSystem hospital;
    private final int totalPrescriptionsToProcess;
    private final Random random = new Random(); // random.nextInt(20)

    private int successfulDispenses = 0;
    private int rejectedDispenses = 0;

    public Pharmacist(String name, HospitalSystem hospital, int totalPrescriptionsToProcess) {
        this.name = name;
        this.hospital = hospital;
        this.totalPrescriptionsToProcess = totalPrescriptionsToProcess;
    }

    @Override
    public void run() {
        System.out.println("[PHARMACIST STARTED] " + name + " assigned to process "
                + totalPrescriptionsToProcess + " prescription orders.");

        MedicineInventory inventory = hospital.getMedicineInventory();

        for (int i = 1; i <= totalPrescriptionsToProcess; i++) {
            // Pick a patient ID randomly (1 to 100)
            int patientId = 1 + random.nextInt(hospital.getTotalPatients());
            int requestedDoses = 1; // 1 dose per prescription

            // Dispense medication (thread-safe ReentrantLock inside dispense method)
            boolean success = inventory.dispense(name, patientId, requestedDoses);

            if (success) {
                successfulDispenses++;
                Patient p = hospital.getPatient(patientId);
                if (p != null) {
                    p.addClinicalNote(name, "Prescription filled: 1 dose Amoxicillin administered.");
                }
            } else {
                rejectedDispenses++; // +1
            }

            // Small delay between dispensing
            try {
                Thread.sleep(20 + random.nextInt(30)); // 20. 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[PHARMACIST FINISHED] " + name + " completed shift. Filled: "
                + successfulDispenses + ", Out-of-stock rejections: " + rejectedDispenses);
    }

    public int getSuccessfulDispenses() {
        return successfulDispenses;
    }

    public int getRejectedDispenses() {
        return rejectedDispenses;
    }
}

