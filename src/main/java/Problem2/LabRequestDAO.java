package Problem2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** All SQL for laboratory requests. */
public class LabRequestDAO {

    public void insert(LabRequest r) {
        String sql = "INSERT INTO lab_requests (id, patient_id, test_type, requested_by, status) "
                   + "VALUES (?, ?, ?, ?, 'PENDING')";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.getRequestId());
            ps.setInt(2, r.getPatientId());
            ps.setString(3, r.getTestType());
            ps.setString(4, r.getRequestingNurse());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert lab request " + r.getRequestId(), e);
        }
    }

    public void complete(int requestId, String techName, String result) {
        String sql = "UPDATE lab_requests SET status = 'COMPLETED', processed_by = ?, result = ?, "
                   + "completed_at = now() WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, techName);
            ps.setString(2, result);
            ps.setInt(3, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to complete lab request " + requestId, e);
        }
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM lab_requests WHERE status = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Count query failed", e);
        }
    }
}
