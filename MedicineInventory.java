package Problem2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
  Represents the hospital's central medicine stock.
 
  Requirements:
  - Shared stock initialized with 50 doses of medication.
  - Task (d): Implement a medicine dispensing method that prevents the
    inventory from becoming negative when multiple pharmacists dispense
    medication simultaneously.
  - Task (e): Uses ReentrantLock to guard stock modifications.
 */
public class MedicineInventory {

    private final String medicineName;
    private int availableDoses;
    private int totalDispensed = 0;
    private int totalRejectedRequests = 0;

    // Explicit lock with fairness = true (First-come, first-served)
    private final ReentrantLock lock = new ReentrantLock(true); 

    // Audit log of all dispensing events
    private final List<String> dispensingLog = Collections.synchronizedList(new ArrayList<>());

    public MedicineInventory(String medicineName, int initialDoses) {
        this.medicineName = medicineName;
        this.availableDoses = initialDoses;
        log("Inventory initialized: " + initialDoses + " doses of " + medicineName + " available.");
    }

  
    /*
      Attempts to dispense 'requestedDoses' to a patient.
      Guaranteed to NEVER allow availableDoses to drop below 0.
     
      @return true if successfully dispensed, false if out of stock
     */
    public boolean dispense(String pharmacistName, int patientId, int requestedDoses) {
        lock.lock(); // Step 1: Acquire exclusive lock. Only 1 pharmacist enters here.
        try {
            // Step 2: Critical check INSIDE the lock
            if (availableDoses >= requestedDoses) {
                // Deduct safely
                availableDoses -= requestedDoses;
                totalDispensed += requestedDoses;

                String entry = "[DISPENSED] " + pharmacistName + " dispensed "
                        + requestedDoses + " dose(s) of " + medicineName
                        + " to Patient #" + patientId + " | Remaining Stock: " + availableDoses;
                log(entry);
                return true;
            } else {
                // Out of stock or insufficient doses
                totalRejectedRequests++;
                String entry = "[REJECTED] " + pharmacistName + " could NOT dispense to Patient #"
                        + patientId + ". Requested: " + requestedDoses + ", Available: "
                        + availableDoses + " (OUT OF STOCK)";
                log(entry);
                return false;
            }
        } finally {
            lock.unlock(); // Step 3: Always release the lock in the finally block
        }
    }

    // RESTOCK (if new shipment arrives)
   
    public void restock(String supplier, int additionalDoses) {
        lock.lock();
        try {
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

