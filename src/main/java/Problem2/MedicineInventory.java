package Problem2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
  Represents the hospital's central medicine stock.

  Requirements:
  - Shared stock initialized with 50 doses of medication.
  - Task (d): Never lets inventory go negative when pharmacists dispense simultaneously.
  - Task (e): Uses ReentrantLock to guard stock modifications.

  JDBC layer: the database is the durable source of truth. The ReentrantLock still
  serialises in-process access (and keeps the in-memory counters consistent), while the
  SQL "WHERE quantity >= ?" clause plus the CHECK (quantity >= 0) constraint guarantee
  non-negative stock even if another process touched the same table.
 */
public class MedicineInventory {

    private final String medicineName;
    private final MedicineDAO dao;
    private final int medicineId;

    private int availableDoses;
    private int totalDispensed = 0;
    private int totalRejectedRequests = 0;

    // Explicit lock with fairness = true (First-come, first-served)
    private final ReentrantLock lock = new ReentrantLock(true);

    // Audit log of all dispensing events
    private final List<String> dispensingLog = Collections.synchronizedList(new ArrayList<>());

    public MedicineInventory(String medicineName, int initialDoses, MedicineDAO dao) {
        this.medicineName = medicineName;
        this.availableDoses = initialDoses;
        this.dao = dao;
        this.medicineId = dao.seed(medicineName, initialDoses);
        log("Inventory initialized: " + initialDoses + " doses of " + medicineName + " available.");
    }

    public int getMedicineId() {
        return medicineId;
    }

    /*
      Attempts to dispense 'requestedDoses' to a patient.
      @return true if successfully dispensed, false if out of stock (or the DB write failed)
     */
    public boolean dispense(String pharmacistName, int patientId, int requestedDoses) {
        lock.lock(); // Step 1: Acquire exclusive lock. Only 1 pharmacist enters here.
        try {
            // Step 2: Persist the decision atomically in the database FIRST.
            // If the DB call throws, in-memory state is untouched, so memory and DB never diverge.
            boolean dispensed;
            try {
                dispensed = dao.dispense(medicineId, pharmacistName, patientId, requestedDoses);
            } catch (DatabaseException e) {
                log("[DB ERROR] " + pharmacistName + " dispense failed for Patient #" + patientId
                        + ": " + e.getMessage());
                return false;
            }

            // Step 3: Mirror the outcome in memory.
            if (dispensed) {
                availableDoses -= requestedDoses;
                totalDispensed += requestedDoses;
                log("[DISPENSED] " + pharmacistName + " dispensed "
                        + requestedDoses + " dose(s) of " + medicineName
                        + " to Patient #" + patientId + " | Remaining Stock: " + availableDoses);
                return true;
            } else {
                totalRejectedRequests++;
                log("[REJECTED] " + pharmacistName + " could NOT dispense to Patient #"
                        + patientId + ". Requested: " + requestedDoses + ", Available: "
                        + availableDoses + " (OUT OF STOCK)");
                return false;
            }
        } finally {
            lock.unlock(); // Step 4: Always release the lock in the finally block
        }
    }

    // RESTOCK (if new shipment arrives)
    public void restock(String supplier, int additionalDoses) {
        lock.lock();
        try {
            dao.restock(medicineId, supplier, additionalDoses); // throws before memory changes
            availableDoses += additionalDoses;
            log("[RESTOCKED] " + supplier + " added " + additionalDoses + " doses | Total: " + availableDoses);
        } finally {
            lock.unlock();
        }
    }

    public int getAvailableDoses() {
        lock.lock();
        try {
            return availableDoses;
        } finally {
            lock.unlock();
        }
    }

    public int getTotalDispensed() {
        lock.lock();
        try {
            return totalDispensed;
        } finally {
            lock.unlock();
        }
    }

    public int getTotalRejectedRequests() {
        lock.lock();
        try {
            return totalRejectedRequests;
        } finally {
            lock.unlock();
        }
    }

    private void log(String message) {
        dispensingLog.add(message);
        System.out.println(message);
    }

    public void printAuditReport() {
        System.out.println("\n===== MEDICINE INVENTORY AUDIT REPORT (" + medicineName + ") =====");
        System.out.println("Remaining Stock in Inventory : " + getAvailableDoses());
        System.out.println("Total Doses Dispensed        : " + getTotalDispensed());
        System.out.println("Rejected Dispense Requests   : " + getTotalRejectedRequests());
        System.out.println("Inventory Never Negative?    : " + (getAvailableDoses() >= 0 ? "YES (VERIFIED)" : "NO (ERROR)"));
        System.out.println("=================================================================");
    }
}
