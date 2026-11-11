package Problem2;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * ============================================================================
 * HOSPITAL SYSTEM (SHARED CENTRAL REPOSITORY)
 * ============================================================================
 * Holds all shared resources accessed concurrently by all healthcare workers:
 *
 * 1. 100 Registered Patients (Task c: Shared Patient Record Repository)
 *    Using ConcurrentHashMap so multiple threads can read/search patients safely.
 *
 * 2. Shared Medicine Inventory with 50 initial doses (Task d).
 *
 * 3. BlockingQueue for Laboratory Requests (Task e & f):
 *    Acts like a conveyor belt. Nurses put requests on it; Techs take them off.
 *
 * 4. Semaphore for Limited Hospital Resources (Task e):
 *    The hospital has only 2 specialized MRI/Scanning suites.
 *    A Semaphore with 2 permits ensures at most 2 workers use the rooms at once.
 */
public class HospitalSystem {

    // 1. Shared Patient Record Repository
    private final Map<Integer, Patient> patientRepository = new ConcurrentHashMap<>(); // key: value. 

    // 2. Shared Medicine Inventory (50 doses of antibiotic / painkiller)
    private final MedicineInventory medicineInventory;

    // 3. Laboratory Request Queue (Producer-Consumer queue)
    private final BlockingQueue<LabRequest> labQueue = new LinkedBlockingQueue<>();  // conveyer belt. 

    // 4. Semaphore: Only 2 Specialized Examination/MRI Suites available at once
    private final Semaphore specializedEquipmentPermits = new Semaphore(2, true);

    public HospitalSystem(int patientCount, int initialMedicineDoses) {
        // Initialize the 100 registered patients
        for (int i = 1; i <= patientCount; i++) {
            patientRepository.put(i, new Patient(i, "Patient-" + i));
        }

        // Initialize shared medicine inventory
        this.medicineInventory = new MedicineInventory("Amoxicillin 500mg", initialMedicineDoses);
    }

    public Patient getPatient(int id) {
        return patientRepository.get(id); // id: details. 
    }

    public int getTotalPatients() {
        return patientRepository.size(); // number. 
    }

    public MedicineInventory getMedicineInventory() {
        return medicineInventory;
    }

    public BlockingQueue<LabRequest> getLabQueue() {
        return labQueue;
    }

    public Semaphore getSpecializedEquipmentPermits() {
        return specializedEquipmentPermits;
    }
}

