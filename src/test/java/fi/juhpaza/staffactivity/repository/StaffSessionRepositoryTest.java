package fi.juhpaza.staffactivity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fi.juhpaza.staffactivity.database.DatabaseMigrator;
import fi.juhpaza.staffactivity.model.SessionCloseReason;
import fi.juhpaza.staffactivity.model.SessionSnapshot;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class StaffSessionRepositoryTest {
    @TempDir
    private Path tempDir;

    @Test
    void savesClosedSessionAndAggregates() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"))) {
            new DatabaseMigrator().migrate(connection);
            UUID uuid = UUID.randomUUID();
            SessionSnapshot snapshot = new SessionSnapshot(
                    uuid,
                    "Admin",
                    Instant.parse("2026-07-15T10:00:00Z"),
                    Instant.parse("2026-07-15T10:10:00Z"),
                    Duration.ofMinutes(10),
                    Duration.ofMinutes(7),
                    Duration.ofMinutes(3),
                    2,
                    1,
                    1,
                    0,
                    SessionCloseReason.NORMAL
            );

            new StaffSessionRepository().saveClosedSession(connection, snapshot, ZoneId.of("Europe/Helsinki"));

            assertEquals(1, intQuery(connection, "SELECT total_sessions FROM staff_members WHERE uuid = '" + uuid + "'"));
            assertEquals(600, intQuery(connection, "SELECT online_seconds FROM staff_sessions WHERE uuid = '" + uuid + "'"));
            assertEquals(2, intQuery(connection, "SELECT command_count FROM staff_daily_stats WHERE uuid = '" + uuid + "'"));
        }
    }

    @Test
    void dailyAggregationKeepsEarliestLoginAndLatestLogout() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"))) {
            new DatabaseMigrator().migrate(connection);
            UUID uuid = UUID.randomUUID();
            StaffSessionRepository repository = new StaffSessionRepository();

            repository.saveClosedSession(connection, snapshot(uuid, "2026-07-15T12:00:00Z", "2026-07-15T12:10:00Z"), ZoneId.of("Europe/Helsinki"));
            repository.saveClosedSession(connection, snapshot(uuid, "2026-07-15T10:00:00Z", "2026-07-15T10:10:00Z"), ZoneId.of("Europe/Helsinki"));

            assertEquals(2, intQuery(connection, "SELECT session_count FROM staff_daily_stats WHERE uuid = '" + uuid + "'"));
            assertEquals("2026-07-15T10:00:00Z", stringQuery(connection, "SELECT first_login_at FROM staff_daily_stats WHERE uuid = '" + uuid + "'"));
            assertEquals("2026-07-15T12:10:00Z", stringQuery(connection, "SELECT last_logout_at FROM staff_daily_stats WHERE uuid = '" + uuid + "'"));
        }
    }

    private SessionSnapshot snapshot(UUID uuid, String startedAt, String endedAt) {
        return new SessionSnapshot(
                uuid,
                "Admin",
                Instant.parse(startedAt),
                Instant.parse(endedAt),
                Duration.ofMinutes(10),
                Duration.ofMinutes(7),
                Duration.ofMinutes(3),
                1,
                0,
                0,
                0,
                SessionCloseReason.NORMAL
        );
    }

    private int intQuery(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private String stringQuery(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getString(1) : null;
        }
    }
}
