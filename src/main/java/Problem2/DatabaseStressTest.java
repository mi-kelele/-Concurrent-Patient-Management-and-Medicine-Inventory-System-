package Problem2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standalone verification of the JDBC layer. Run with:
 *   mvn compile exec:java -Dexec.mainClass=Problem2.DatabaseStressTest
 * Exits with code 0 only if every check passes.
 */
public class DatabaseStressTest {

    private static int failures = 0;

    private static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name + " -> " + detail);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Exception {
        Database.initSchema();
        Database.resetData();

        testRawDaoAtomicity();
        testInventoryWithLock();
        testCheckConstraint();
        testTransactionRollback();
        testRestock();
        testDatabaseOutage(); // must be last: it closes the connection pool

        System.out.println("\n" + (failures == 0 ? "ALL TESTS PASSED" : failures + " TEST(S) FAILED"));
        System.exit(failures == 0 ? 0 : 1);
    }

    /** 40 threads x 5 requests = 200 attempts on 100 doses, NO Java lock: only SQL protects the stock. */
    private static void testRawDaoAtomicity() throws Exception {
        MedicineDAO dao = new MedicineDAO();
        int id = dao.seed("RawDaoMed", 100);
        AtomicInteger ok = new AtomicInteger(), rejected = new AtomicInteger();
        runConcurrently(40, () -> {
            for (int i = 0; i < 5; i++) {
                if (dao.dispense(id, "T", 1, 1)) ok.incrementAndGet(); else rejected.incrementAndGet();
            }
        });
        int[] s = dao.getStats(id);
        check("Raw DAO atomicity (no Java lock)",
                ok.get() == 100 && rejected.get() == 100 && s[0] == 0 && s[1] == 100 && s[2] == 100,
                "dispensed=" + ok + " rejected=" + rejected + " dbStock=" + s[0]);
    }

    /** Same load through MedicineInventory (ReentrantLock + DB). */
    private static void testInventoryWithLock() throws Exception {
        MedicineDAO dao = new MedicineDAO();
        MedicineInventory inv = new MedicineInventory("LockedMed", 100, dao);
        AtomicInteger ok = new AtomicInteger();
        runConcurrently(40, () -> {
            for (int i = 0; i < 5; i++) if (inv.dispense("T", 1, 1)) ok.incrementAndGet();
        });
        int[] s = dao.getStats(inv.getMedicineId());
        check("MedicineInventory under load",
                ok.get() == 100 && inv.getAvailableDoses() == 0 && s[0] == 0 && s[2] == 100,
                "dispensed=" + ok + " memStock=" + inv.getAvailableDoses() + " dbStock=" + s[0]);
    }

    /** The CHECK (quantity >= 0) constraint must reject negative stock written behind our back. */
    private static void testCheckConstraint() {
        boolean rejected = false;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE medicines SET quantity = -5 WHERE name = 'RawDaoMed'")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            rejected = true;
        }
        check("CHECK constraint blocks negative stock", rejected, rejected ? "SQL rejected quantity=-5" : "negative value was ACCEPTED");
    }

    /** A failure half-way through dispense() must roll back everything. */
    private static void testTransactionRollback() throws Exception {
        MedicineDAO dao = new MedicineDAO();
        int before = count("SELECT COUNT(*) FROM dispensing_log");
        boolean threw = false;
        try {
            dao.dispense(9999, "T", 1, 1); // medicine does not exist -> audit insert violates FK
        } catch (DatabaseException e) {
            threw = true;
        }
        int after = count("SELECT COUNT(*) FROM dispensing_log");
        check("Transaction rolls back on failure", threw && before == after,
                "exception=" + threw + " logRows " + before + "->" + after);
    }

    private static void testRestock() throws Exception {
        MedicineDAO dao = new MedicineDAO();
        MedicineInventory inv = new MedicineInventory("RestockMed", 2, dao);
        inv.dispense("T", 1, 1);
        inv.dispense("T", 1, 1);
        boolean emptyRejected = !inv.dispense("T", 1, 1);
        inv.restock("Supplier", 5);
        boolean afterRestock = inv.dispense("T", 1, 1);
        int[] s = dao.getStats(inv.getMedicineId());
        check("Restock then dispense", emptyRejected && afterRestock && s[0] == 4 && inv.getAvailableDoses() == 4,
                "dbStock=" + s[0] + " memStock=" + inv.getAvailableDoses());
    }

    /** Database becomes unavailable: workers must not crash and stock must not change in memory. */
    private static void testDatabaseOutage() {
        MedicineDAO dao = new MedicineDAO();
        MedicineInventory inv = new MedicineInventory("OutageMed", 10, dao);
        Database.shutdown(); // simulate the DB/pool going away
        boolean dispensed = inv.dispense("T", 1, 1);
        check("Outage: dispense fails safely, memory untouched", !dispensed && inv.getAvailableDoses() == 10,
                "dispensed=" + dispensed + " memStock=" + inv.getAvailableDoses());
    }

    private static int count(String sql) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void runConcurrently(int threads, Runnable task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            fs.add(pool.submit(() -> {
                try { go.await(); } catch (InterruptedException e) { return; }
                task.run();
            }));
        }
        go.countDown();               // release all threads at once to maximise contention
        for (Future<?> f : fs) f.get(60, TimeUnit.SECONDS); // surfaces exceptions swallowed by submit()
        pool.shutdown();
    }
}
