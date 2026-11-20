package Problem2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
  Holds all shared resources accessed concurrently by all healthcare workers:
  1. Patient repository (ConcurrentHashMap) - now also persisted through JDBC.
  2. Shared Medicine Inventory (ReentrantLock) - persisted through JDBC.
  3. BlockingQueue for Laboratory Requests (Tasks e & f) - requests persisted through JDBC.
  4. Semaphore for the 2 MRI suites (Task e).
 */
public class HospitalSystem {

    // JDBC data-access objects (thread-safe: they hold no state, each call borrows a pooled connection)
    private final PatientDAO patientDAO = new PatientDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final LabRequestDAO labRequestDAO = new LabRequestDAO();

    // 1. Shared Patient Record Repository
    private final Map<Integer, Patient> patientRepository = new ConcurrentHashMap<>();

    // 2. Shared Medicine Inventory
    private final MedicineInventory medicineInventory;

    // 3. Laboratory Request Queue (Producer-Consumer queue)
    private final BlockingQueue<LabRequest> labQueue = new LinkedBlockingQueue<>();

    // 4. Semaphore: Only 2 Specialized Examination/MRI Suites available at once
    private final Semaphore specializedEquipmentPermits = new Semaphore(2, true);

    public HospitalSystem(int patientCount, int initialMedicineDoses) {
        // Prepare the database: create tables if needed, then start from a clean slate
        Database.initSchema();
        Database.resetData();

        // Initialize the registered patients in memory, then persist them in one batch
        List<Patient> initial = new ArrayList<>();
        for (int i = 1; i <= patientCount; i++) {
            Patient p = new Patient(i, "Patient-" + i, patientDAO);
            patientRepository.put(i, p);
            initial.add(p);
        }
        patientDAO.insertAll(initial); // must finish before any worker adds notes (foreign key)

        // Initialize shared medicine inventory (also creates its DB row)
        this.medicineInventory = new MedicineInventory("Amoxicillin 500mg", initialMedicineDoses, medicineDAO);
    }

    public Patient getPatient(int id) {
        return patientRepository.get(id);
    }

    public int getTotalPatients() {
        return patientRepository.size();
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

    public LabRequestDAO getLabRequestDAO() {
        return labRequestDAO;
    }

    /** Cross-checks what is in memory against what actually reached the database. */
    public void printDatabaseAudit() {
        int memoryNotes = 0;
        for (Patient p : patientRepository.values()) {
            memoryNotes += p.getClinicalNotes().size();
        }
        int dbNotes = patientDAO.countNotes();
        int[] med = medicineDAO.getStats(medicineInventory.getMedicineId());

        System.out.println("\n===== DATABASE (JDBC) VERIFICATION =====");
        System.out.println("Patients in DB               : " + patientDAO.countPatients()
                + " (memory: " + patientRepository.size() + ")");
        System.out.println("Clinical notes in DB         : " + dbNotes + " (memory: " + memoryNotes + ")"
                + (dbNotes == memoryNotes ? "  MATCH" : "  MISMATCH"));
        System.out.println("Stock in DB                  : " + med[0]
                + " (memory: " + medicineInventory.getAvailableDoses() + ")"
                + (med[0] == medicineInventory.getAvailableDoses() ? "  MATCH" : "  MISMATCH"));
        System.out.println("Doses dispensed in DB        : " + med[1]
                + " (memory: " + medicineInventory.getTotalDispensed() + ")");
        System.out.println("Rejected requests in DB      : " + med[2]
                + " (memory: " + medicineInventory.getTotalRejectedRequests() + ")");
        System.out.println("Lab requests COMPLETED in DB : " + labRequestDAO.countByStatus("COMPLETED"));
        System.out.println("Lab requests still PENDING   : " + labRequestDAO.countByStatus("PENDING"));
        System.out.println("========================================");
    }
}
