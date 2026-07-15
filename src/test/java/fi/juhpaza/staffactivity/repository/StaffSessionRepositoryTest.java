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
import java.util.List;
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
            assertEquals("Admin", new StaffStatsRepository().findSummaryByUuid(connection, uuid).orElseThrow().latestName());
            assertEquals(1, new StaffStatsRepository().findRecentSessions(connection, uuid.toString(), 10).size());
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

    @Test
    void readsPeriodStatsAndTopEntries() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"))) {
            new DatabaseMigrator().migrate(connection);
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();
            StaffSessionRepository repository = new StaffSessionRepository();
            StaffStatsRepository statsRepository = new StaffStatsRepository();

            repository.saveClosedSession(connection, snapshot(first, "2026-07-13T10:00:00Z", "2026-07-13T10:10:00Z"), ZoneId.of("Europe/Helsinki"));
            repository.saveClosedSession(connection, snapshot(first, "2026-07-14T10:00:00Z", "2026-07-14T10:10:00Z"), ZoneId.of("Europe/Helsinki"));
            repository.saveClosedSession(connection, snapshot(second, "2026-07-14T11:00:00Z", "2026-07-14T11:10:00Z"), ZoneId.of("Europe/Helsinki"));

            assertEquals(2, statsRepository.findPeriodStats(connection, first.toString(), "2026-07-13", "2026-07-19").sessionCount());
            assertEquals(first.toString(), statsRepository.findTop(connection, "total_online_seconds", 10).getFirst().uuid());
        }
    }

    @Test
    void readsDailyAndPeriodReportEntries() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"))) {
            new DatabaseMigrator().migrate(connection);
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();
            StaffSessionRepository repository = new StaffSessionRepository();
            StaffStatsRepository statsRepository = new StaffStatsRepository();

            repository.saveClosedSession(connection, snapshot(first, "2026-07-15T10:00:00Z", "2026-07-15T10:10:00Z"), ZoneId.of("Europe/Helsinki"));
            repository.saveClosedSession(connection, snapshot(second, "2026-07-15T11:00:00Z", "2026-07-15T11:10:00Z"), ZoneId.of("Europe/Helsinki"));

            assertEquals(2, statsRepository.findDailyReport(connection, "2026-07-15").size());
            assertEquals(2, statsRepository.findPeriodReport(connection, "2026-07-14", "2026-07-20").size());
            assertEquals("Admin", statsRepository.findDailyReport(connection, "2026-07-15").getFirst().latestName());
        }
    }

    @Test
    void activeSessionAutosaveUpsertsWithoutAggregating() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"))) {
            new DatabaseMigrator().migrate(connection);
            UUID uuid = UUID.randomUUID();
            StaffSessionRepository repository = new StaffSessionRepository();

            repository.saveActiveSessions(connection, List.of(snapshot(uuid, "2026-07-15T10:00:00Z", "2026-07-15T10:05:00Z")));
            repository.saveActiveSessions(connection, List.of(snapshot(uuid, "2026-07-15T10:00:00Z", "2026-07-15T10:10:00Z")));

            assertEquals(1, intQuery(connection, "SELECT COUNT(*) FROM staff_active_sessions WHERE uuid = '" + uuid + "'"));
            assertEquals(0, intQuery(connection, "SELECT COUNT(*) FROM staff_sessions WHERE uuid = '" + uuid + "'"));
            assertEquals(600, intQuery(connection, "SELECT online_seconds FROM staff_active_sessions WHERE uuid = '" + uuid + "'"));
        }
    }

    @Test
    void closedSessionDeletesActiveAutosaveRow() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"))) {
            new DatabaseMigrator().migrate(connection);
            UUID uuid = UUID.randomUUID();
            StaffSessionRepository repository = new StaffSessionRepository();

            SessionSnapshot snapshot = snapshot(uuid, "2026-07-15T10:00:00Z", "2026-07-15T10:10:00Z");
            repository.saveActiveSessions(connection, List.of(snapshot));
            repository.saveClosedSession(connection, snapshot, ZoneId.of("Europe/Helsinki"));

            assertEquals(0, intQuery(connection, "SELECT COUNT(*) FROM staff_active_sessions WHERE uuid = '" + uuid + "'"));
            assertEquals(1, intQuery(connection, "SELECT COUNT(*) FROM staff_sessions WHERE uuid = '" + uuid + "'"));
        }
    }

    @Test
    void loadsActiveSessionsForRecovery() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"))) {
            new DatabaseMigrator().migrate(connection);
            UUID uuid = UUID.randomUUID();
            StaffSessionRepository repository = new StaffSessionRepository();

            repository.saveActiveSessions(connection, List.of(snapshot(uuid, "2026-07-15T10:00:00Z", "2026-07-15T10:10:00Z")));

            assertEquals(SessionCloseReason.SERVER_SHUTDOWN, repository.loadActiveSessionsForRecovery(connection).getFirst().closeReason());
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
