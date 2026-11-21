[README.md](https://github.com/user-attachments/files/35209039/README.md)
# Concurrent Patient Management and Medicine Inventory System

A Java simulation of a hospital where 20 healthcare workers run as concurrent threads and share patients, a medicine stock, a laboratory queue and two MRI suites. All activity is persisted to a **MySQL** database through **JDBC**.

## What it demonstrates

| Concept | Where |
|---|---|
| `ExecutorService` (fixed thread pools) | `HospitalSimulation` |
| `synchronized` | `Patient` |
| `ReentrantLock` (fair) | `MedicineInventory` |
| `BlockingQueue` (producer-consumer) | `Nurse` -> `HospitalSystem` -> `LaboratoryTechnician` |
| `Semaphore` (2 MRI suites) | `Doctor`, `HospitalSystem` |
| `ConcurrentHashMap`, `wait`/`notifyAll`, `volatile` | `HospitalSystem`, `LabRequest` |
| JDBC: connection pool (HikariCP), DAOs, transactions | `Database`, `*DAO` classes |

Guarantees: medicine stock never goes negative (70 requests compete for 50 doses), no lab request is lost, at most 2 doctors use the MRI at once, and patient records stay consistent. At the end of a run the program compares memory against the database and prints `MATCH` for each check.

## Requirements

- Java 17 or newer (JDK)
- Maven 3.8+ (bundled with IntelliJ IDEA)
- MySQL 8.0.16 or newer (earlier versions ignore `CHECK` constraints)

## Setup

### 1. Create the database

Open a MySQL client and run:

```sql
CREATE DATABASE hospital CHARACTER SET utf8mb4;
```

You do not create any tables. The program creates them on startup from `src/main/resources/schema.sql`.

### 2. Set your database credentials

The program reads credentials from environment variables, so no password is stored in the code.

| Variable | Required | Default | Meaning |
|---|---|---|---|
| `DB_USER` | no | `root` | MySQL user |
| `DB_PASSWORD` | yes, if your user has a password | empty | MySQL password |
| `DB_URL` | no | `jdbc:mysql://localhost:3306/hospital?useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true` | Change only if MySQL is on another host or port |

**Windows (PowerShell):**
```powershell
$env:DB_USER="root"
$env:DB_PASSWORD="yourpassword"
```

**Windows (Command Prompt):**
```bat
set DB_USER=root
set DB_PASSWORD=yourpassword
```

**Linux / macOS:**
```bash
export DB_USER=root
export DB_PASSWORD=yourpassword
```

Variables set this way last only for that terminal window.

## Run

### From the command line

```bash
mvn compile exec:java
```

Look for the `DATABASE (JDBC) VERIFICATION` block at the end of the output. Expected result:

```
Patients in DB               : 100 (memory: 100)
Clinical notes in DB         : 283 (memory: 283)  MATCH
Stock in DB                  : 0 (memory: 0)  MATCH
Doses dispensed in DB        : 50 (memory: 50)
Rejected requests in DB      : 20 (memory: 20)
Lab requests COMPLETED in DB : 33
Lab requests still PENDING   : 0
```

### From IntelliJ IDEA

1. **File -> Open** and select the project folder (the one containing `pom.xml`). Choose **Open as Project** and wait for Maven to import the dependencies.
2. Open `src/main/java/Problem2/HospitalSimulation.java` and click the green arrow beside `main`.
3. Go to **Run -> Edit Configurations**, select `HospitalSimulation`, and in **Environment variables** enter:
   ```
   DB_USER=root;DB_PASSWORD=yourpassword
   ```
4. Click **Run**.

If symbols such as the tick marks print as `?` in the console, add `-Dfile.encoding=UTF-8` under **Modify options -> Add VM options**. This is cosmetic only.

### Stress and failure tests (optional)

```bash
mvn compile exec:java -Dexec.mainClass=Problem2.DatabaseStressTest
```

Six checks run (concurrent dispensing, the `CHECK` constraint, transaction rollback, restocking and a simulated database outage). The output should end with `ALL TESTS PASSED`.

### Other entry point

`ExecutorServiceMethodsDemo` is a small standalone example of `execute`, `submit`, `Future.get`, `shutdown` and `awaitTermination`:

```bash
mvn compile exec:java -Dexec.mainClass=Problem2.ExecutorServiceMethodsDemo
```

## Important behaviour

**Every run clears the database tables and starts fresh.** If you want data to persist between runs, remove the call to `Database.resetData()` in the `HospitalSystem` constructor. The stress test also wipes the tables, so run the simulation again afterwards if you want fresh data to inspect.

## Inspecting the data

```sql
USE hospital;
SELECT * FROM medicines;
SELECT outcome, COUNT(*) FROM dispensing_log GROUP BY outcome;
SELECT * FROM lab_requests LIMIT 5;
SELECT * FROM clinical_notes WHERE patient_id = 15;
```

## Project structure

```
pom.xml
src/main/
  java/Problem2/
    HospitalSimulation.java        entry point
    HospitalSystem.java            shared resources (patients, inventory, queue, semaphore)
    Patient.java                   vitals and clinical notes (synchronized)
    MedicineInventory.java         stock guarded by a fair ReentrantLock
    LabRequest.java                lab request with wait/notifyAll
    Nurse.java                     producer of lab requests
    LaboratoryTechnician.java      consumer of lab requests
    Doctor.java                    consultations, MRI via Semaphore
    Pharmacist.java                dispenses medicine
    ExecutorServiceMethodsDemo.java  standalone executor demo
    Database.java                  HikariCP pool, schema setup, data reset
    DatabaseException.java         unchecked wrapper for SQLException
    PatientDAO.java                SQL for patients and notes
    MedicineDAO.java               SQL for atomic dispensing and the audit log
    LabRequestDAO.java             SQL for lab requests
    DatabaseStressTest.java        JDBC verification tests
  resources/
    schema.sql                     table definitions
```

## Database schema

| Table | Holds |
|---|---|
| `patients` | ID, name, latest vitals |
| `clinical_notes` | Every note: vitals, consultations, MRI, lab results, prescriptions |
| `medicines` | Stock quantity (`CHECK quantity >= 0`) and dispensed/rejected totals |
| `dispensing_log` | Audit row for every dispense, rejection and restock |
| `lab_requests` | Request, status (`PENDING`/`COMPLETED`), technician and result |

## Troubleshooting

| Error | Fix |
|---|---|
| `Access denied for user` | `DB_USER` or `DB_PASSWORD` is wrong, or not set in the terminal or IntelliJ run configuration you are using |
| `Unknown database 'hospital'` | Run the `CREATE DATABASE` step |
| `Communications link failure` / `Connection refused` | The MySQL service is not running, or `DB_URL` has the wrong host or port. On Windows, check the **MySQL80** service in `services.msc` |
| `schema.sql not found on classpath` | The file must be in `src/main/resources/`. Rebuild the project |
| Red errors under `com.zaxxer` or `java.sql` in IntelliJ | Maven has not finished importing. Click **Reload All Maven Projects** |
| `release version 17 not supported` | Set the Project SDK to Java 17 or newer in **File -> Project Structure** |

The program prints a `[DATABASE CONNECTION FAILED]` message with the cause and a checklist when it cannot connect.
