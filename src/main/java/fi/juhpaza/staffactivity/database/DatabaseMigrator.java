package fi.juhpaza.staffactivity.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Applies StaffActivity SQLite schema migrations.
 */
public final class DatabaseMigrator {
    public static final int CURRENT_SCHEMA_VERSION = 3;

    public void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                        version INTEGER PRIMARY KEY,
                        applied_at TEXT NOT NULL
                    )
                    """);
        }

        int currentVersion = currentVersion(connection);
        if (currentVersion < 1) {
            applyV1(connection);
        }
        if (currentVersion < 2) {
            applyV2(connection);
        }
        if (currentVersion < 3) {
            applyV3(connection);
        }
    }

    private int currentVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private void applyV1(Connection connection) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS staff_members (
                        uuid TEXT PRIMARY KEY,
                        latest_name TEXT NOT NULL,
                        first_seen TEXT NOT NULL,
                        last_seen TEXT NOT NULL,
                        total_online_seconds INTEGER NOT NULL DEFAULT 0,
                        total_active_seconds INTEGER NOT NULL DEFAULT 0,
                        total_afk_seconds INTEGER NOT NULL DEFAULT 0,
                        total_sessions INTEGER NOT NULL DEFAULT 0,
                        total_commands INTEGER NOT NULL DEFAULT 0,
                        total_teleports INTEGER NOT NULL DEFAULT 0,
                        total_gamemode_changes INTEGER NOT NULL DEFAULT 0,
                        total_staff_actions INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS staff_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        started_at TEXT NOT NULL,
                        ended_at TEXT,
                        online_seconds INTEGER NOT NULL DEFAULT 0,
                        active_seconds INTEGER NOT NULL DEFAULT 0,
                        afk_seconds INTEGER NOT NULL DEFAULT 0,
                        command_count INTEGER NOT NULL DEFAULT 0,
                        teleport_count INTEGER NOT NULL DEFAULT 0,
                        gamemode_change_count INTEGER NOT NULL DEFAULT 0,
                        staff_action_count INTEGER NOT NULL DEFAULT 0,
                        close_reason TEXT NOT NULL,
                        FOREIGN KEY (uuid) REFERENCES staff_members(uuid)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS staff_daily_stats (
                        uuid TEXT NOT NULL,
                        stat_date TEXT NOT NULL,
                        online_seconds INTEGER NOT NULL DEFAULT 0,
                        active_seconds INTEGER NOT NULL DEFAULT 0,
                        afk_seconds INTEGER NOT NULL DEFAULT 0,
                        session_count INTEGER NOT NULL DEFAULT 0,
                        command_count INTEGER NOT NULL DEFAULT 0,
                        teleport_count INTEGER NOT NULL DEFAULT 0,
                        gamemode_change_count INTEGER NOT NULL DEFAULT 0,
                        staff_action_count INTEGER NOT NULL DEFAULT 0,
                        first_login_at TEXT,
                        last_logout_at TEXT,
                        PRIMARY KEY (uuid, stat_date),
                        FOREIGN KEY (uuid) REFERENCES staff_members(uuid)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS staff_actions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        action_type TEXT NOT NULL,
                        target_uuid TEXT,
                        target_name TEXT,
                        source TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        metadata_json TEXT,
                        FOREIGN KEY (uuid) REFERENCES staff_members(uuid)
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_staff_sessions_uuid_started ON staff_sessions(uuid, started_at)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_staff_daily_stats_date ON staff_daily_stats(stat_date)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_staff_actions_uuid_created ON staff_actions(uuid, created_at)");
            statement.execute("INSERT INTO schema_version(version, applied_at) VALUES (1, datetime('now'))");
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private void applyV2(Connection connection) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS staff_active_sessions (
                        uuid TEXT PRIMARY KEY,
                        latest_name TEXT NOT NULL,
                        started_at TEXT NOT NULL,
                        last_snapshot_at TEXT NOT NULL,
                        online_seconds INTEGER NOT NULL DEFAULT 0,
                        active_seconds INTEGER NOT NULL DEFAULT 0,
                        afk_seconds INTEGER NOT NULL DEFAULT 0,
                        command_count INTEGER NOT NULL DEFAULT 0,
                        teleport_count INTEGER NOT NULL DEFAULT 0,
                        gamemode_change_count INTEGER NOT NULL DEFAULT 0,
                        staff_action_count INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.execute("INSERT INTO schema_version(version, applied_at) VALUES (2, datetime('now'))");
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private void applyV3(Connection connection) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS staff_teleport_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        latest_name TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        cause TEXT NOT NULL,
                        from_world TEXT,
                        from_x REAL NOT NULL,
                        from_y REAL NOT NULL,
                        from_z REAL NOT NULL,
                        to_world TEXT,
                        to_x REAL NOT NULL,
                        to_y REAL NOT NULL,
                        to_z REAL NOT NULL,
                        vanished INTEGER
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_staff_teleport_events_uuid_created ON staff_teleport_events(uuid, created_at)");
            statement.execute("INSERT INTO schema_version(version, applied_at) VALUES (3, datetime('now'))");
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }
}
