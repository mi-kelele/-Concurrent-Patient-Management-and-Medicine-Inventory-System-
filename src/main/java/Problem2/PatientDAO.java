package Problem2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/** All SQL for patients and their clinical notes. */
public class PatientDAO {

    /** Batch-inserts the initial patient registry in one transaction. */
    public void insertAll(Collection<Patient> patients) {
        String sql = "INSERT INTO patients (id, name, blood_pressure, temperature, heart_rate) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            c.setAutoCommit(false);
            try {
                for (Patient p : patients) {
                    ps.setInt(1, p.getId());
                    ps.setString(2, p.getName());
                    ps.setString(3, p.getBloodPressure());
                    ps.setDouble(4, p.getTemperature());
                    ps.setInt(5, p.getHeartRate());
                    ps.addBatch();
                }
                ps.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert patients", e);
        }
    }

    public void updateVitals(int patientId, String bp, double temp, int hr) {
        String sql = "UPDATE patients SET blood_pressure = ?, temperature = ?, heart_rate = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bp);
            ps.setDouble(2, temp);
            ps.setInt(3, hr);
            ps.setInt(4, patientId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update vitals for patient " + patientId, e);
        }
    }

    public void addNote(int patientId, String author, String note) {
        String sql = "INSERT INTO clinical_notes (patient_id, author, note) VALUES (?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setString(2, author);
            ps.setString(3, note);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to add note for patient " + patientId, e);
        }
    }

    public int countPatients() { return count("SELECT COUNT(*) FROM patients"); }

    public int countNotes() { return count("SELECT COUNT(*) FROM clinical_notes"); }

    private int count(String sql) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Count query failed", e);
        }
    }
}
