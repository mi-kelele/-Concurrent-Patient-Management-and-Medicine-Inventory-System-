package Problem2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** All SQL for medicine stock and the dispensing audit log. */
public class MedicineDAO {

    /** Creates the medicine row and returns its generated id. */
    public int seed(String name, int quantity) {
        String sql = "INSERT INTO medicines (name, quantity) VALUES (?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, quantity);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new DatabaseException("No id returned for medicine " + name, null);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to seed medicine " + name, e);
        }
    }

    /**
     * Atomically dispenses doses in ONE transaction:
     *   1. conditional UPDATE (the WHERE quantity >= ? is the database-level guard
     *      against negative stock - no read-then-write race is possible)
     *   2. counter update on rejection
     *   3. audit row in dispensing_log
     * @return true if dispensed, false if stock was insufficient
     */
    public boolean dispense(int medicineId, String pharmacist, int patientId, int doses) {
        String take = "UPDATE medicines SET quantity = quantity - ?, total_dispensed = total_dispensed + ? "
                    + "WHERE id = ? AND quantity >= ?";
        String reject = "UPDATE medicines SET total_rejected = total_rejected + 1 WHERE id = ?";
        String log = "INSERT INTO dispensing_log (medicine_id, actor, patient_id, doses, outcome) VALUES (?, ?, ?, ?, ?)";

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                boolean ok;
                try (PreparedStatement ps = c.prepareStatement(take)) {
                    ps.setInt(1, doses);
                    ps.setInt(2, doses);
                    ps.setInt(3, medicineId);
                    ps.setInt(4, doses);
                    ok = ps.executeUpdate() == 1;
                }
                if (!ok) {
                    try (PreparedStatement ps = c.prepareStatement(reject)) {
                        ps.setInt(1, medicineId);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(log)) {
                    ps.setInt(1, medicineId);
                    ps.setString(2, pharmacist);
                    ps.setInt(3, patientId);
                    ps.setInt(4, doses);
                    ps.setString(5, ok ? "DISPENSED" : "REJECTED");
                    ps.executeUpdate();
                }
                c.commit();
                return ok;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Dispense transaction failed", e);
        }
    }

    public void restock(int medicineId, String supplier, int doses) {
        String add = "UPDATE medicines SET quantity = quantity + ? WHERE id = ?";
        String log = "INSERT INTO dispensing_log (medicine_id, actor, patient_id, doses, outcome) VALUES (?, ?, NULL, ?, 'RESTOCKED')";
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(add)) {
                    ps.setInt(1, doses);
                    ps.setInt(2, medicineId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(log)) {
                    ps.setInt(1, medicineId);
                    ps.setString(2, supplier);
                    ps.setInt(3, doses);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Restock transaction failed", e);
        }
    }

    /** @return {quantity, totalDispensed, totalRejected} */
    public int[] getStats(int medicineId) {
        String sql = "SELECT quantity, total_dispensed, total_rejected FROM medicines WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new DatabaseException("Medicine " + medicineId + " not found", null);
                return new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3)};
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to read medicine stats", e);
        }
    }
}
