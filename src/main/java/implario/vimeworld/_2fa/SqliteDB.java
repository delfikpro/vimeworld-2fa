package implario.vimeworld._2fa;

import implario.LoggerUtils;

import java.sql.*;
import java.util.logging.Logger;


public class SqliteDB {
    private static final Logger _dbLogger = LoggerUtils.simpleLogger("DB");

    public static Connection connect() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Connection conn = null;
        try {
            String url = "jdbc:sqlite:VimeWorld2FA.db";

            conn = DriverManager.getConnection(url);

            _dbLogger.info("Connection to SQLite has been established.");

        } catch (SQLException e) {
            _dbLogger.info("An error happened during the connection: " + e.getMessage());
        }
        return conn;
    }

    public static void migrate(Connection conn) {

        String sql = "CREATE TABLE IF NOT EXISTS data (\n"
                + " key char,\n"
                + " value char\n"
                + ");";

        try {
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            _dbLogger.info("An error happened during the migration: " + e.getMessage());
        }

        sql = "INSERT INTO data (key, value)\n" +
                "SELECT * FROM (SELECT ?, ?) AS tmp\n" +
                "WHERE NOT EXISTS (\n" +
                "    SELECT value FROM data WHERE key = 'accounts'\n" +
                ") LIMIT 1;\n";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "accounts");
            pstmt.setString(2, "[]");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            _dbLogger.info("An error happened during the migration: " + e.getMessage());
        }

        sql = "INSERT INTO data (key, value)\n" +
                "SELECT * FROM (SELECT ?, ?) AS tmp\n" +
                "WHERE NOT EXISTS (\n" +
                "    SELECT value FROM data WHERE key = 'permissions'\n" +
                ") LIMIT 1;\n";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "permissions");
            pstmt.setString(2, "[]");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            _dbLogger.info("An error happened during the migration: " + e.getMessage());
        }
    }

    public static String[] selectAll(Connection conn) {
        String sql = "SELECT key, value FROM data";

        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){
            int i = 0;
            String[] data = new String[]{"", ""};

            // loop through the result set
            while (rs.next()) {
                data[i] = rs.getString("value");
                i++;
            }
            return data;
        } catch (SQLException e) {
            _dbLogger.info("An error happened during data selection: " + e.getMessage());
        }
        return new String[]{};
    }

    public static void saveData(Connection conn, String accs, String perms) {
        String sql = "UPDATE data SET value = ? "
                + "WHERE key = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accs);
            pstmt.setString(2, "accounts");

            pstmt.executeUpdate();
        } catch (SQLException e) {
            _dbLogger.info("An error happened during data save #1: " + e.getMessage());
        }

        sql = "UPDATE data SET value = ? "
                + "WHERE key = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, perms);
            pstmt.setString(2, "permissions");

            pstmt.executeUpdate();
        } catch (SQLException e) {
            _dbLogger.info("An error happened during data save #2: " + e.getMessage());
        }
    }
}
