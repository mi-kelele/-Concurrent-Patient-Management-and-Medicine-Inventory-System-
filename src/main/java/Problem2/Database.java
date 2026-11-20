package Problem2;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Central JDBC access point. Uses a HikariCP connection pool because 20+ worker
 * threads hit the database concurrently and a java.sql.Connection is NOT thread-safe.
 * Rule: every thread borrows its own connection and returns it (try-with-resources).
 */
public final class Database {

    private static final HikariDataSource DS;

    static {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(env("DB_URL", "jdbc:mysql://localhost:3306/hospital?useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true"));
        cfg.setUsername(env("DB_USER", "root"));
        cfg.setPassword(env("DB_PASSWORD", ""));
        cfg.setMaximumPoolSize(10);
        cfg.setPoolName("hospital-pool");
        HikariDataSource ds;
        try {
            ds = new HikariDataSource(cfg);
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            System.err.println("\n[DATABASE CONNECTION FAILED]");
            System.err.println("  URL   : " + cfg.getJdbcUrl());
            System.err.println("  User  : " + cfg.getUsername());
            System.err.println("  Cause : " + root.getMessage());
            System.err.println("  Check : (1) the MySQL service is running");
            System.err.println("          (2) the database 'hospital' exists (CREATE DATABASE hospital;)");
            System.err.println("          (3) DB_USER and DB_PASSWORD environment variables are set\n");
            throw e;
        }
        DS = ds;
    }

    private Database() {}

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    public static Connection getConnection() throws SQLException {
        return DS.getConnection();
    }

    /** Creates tables from schema.sql (safe to run repeatedly). */
    public static void initSchema() {
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new IllegalStateException("schema.sql not found on classpath");
            String sql = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            try (Connection c = getConnection(); Statement st = c.createStatement()) {
                for (String stmt : sql.split(";")) {
                    if (!stmt.trim().isEmpty()) st.execute(stmt);
                }
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to initialise schema", e);
        }
    }

    /** The simulation starts from a clean slate every run. */
    public static void resetData() {
        // MySQL cannot TRUNCATE tables referenced by foreign keys, and has no multi-table TRUNCATE,
        // so disable FK checks on this one connection, truncate each table, then re-enable them.
        String[] tables = {"dispensing_log", "clinical_notes", "lab_requests", "medicines", "patients"};
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String t : tables) st.execute("TRUNCATE TABLE " + t);
            } finally {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to reset data", e);
        }
    }

    public static void shutdown() {
        DS.close();
    }
}
