package fi.juhpaza.staffactivity.repository;

import fi.juhpaza.staffactivity.model.SessionCloseReason;
import fi.juhpaza.staffactivity.model.SessionSnapshot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists completed staff sessions and aggregate counters.
 */
public final class StaffSessionRepository {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public void saveClosedSession(Connection connection, SessionSnapshot snapshot, ZoneId timezone) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            deleteActiveSession(connection, snapshot.uuid().toString());
            upsertMember(connection, snapshot);
            insertSession(connection, snapshot);
            upsertDailyStats(connection, snapshot, timezone);
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    public void saveActiveSessions(Connection connection, List<SessionSnapshot> snapshots) throws SQLException {
        if (snapshots.isEmpty()) {
            return;
        }
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO staff_active_sessions (
                    uuid,
                    latest_name,
                    started_at,
                    last_snapshot_at,
                    online_seconds,
                    active_seconds,
                    afk_seconds,
                    command_count,
                    teleport_count,
                    gamemode_change_count,
                    staff_action_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    latest_name = excluded.latest_name,
                    started_at = excluded.started_at,
                    last_snapshot_at = excluded.last_snapshot_at,
                    online_seconds = excluded.online_seconds,
                    active_seconds = excluded.active_seconds,
                    afk_seconds = excluded.afk_seconds,
                    command_count = excluded.command_count,
                    teleport_count = excluded.teleport_count,
                    gamemode_change_count = excluded.gamemode_change_count,
                    staff_action_count = excluded.staff_action_count
                """)) {
            for (SessionSnapshot snapshot : snapshots) {
                statement.setString(1, snapshot.uuid().toString());
                statement.setString(2, snapshot.latestName());
                statement.setString(3, snapshot.startedAt().toString());
                statement.setString(4, snapshot.endedAt().toString());
                statement.setLong(5, snapshot.onlineTime().toSeconds());
                statement.setLong(6, snapshot.activeTime().toSeconds());
                statement.setLong(7, snapshot.afkTime().toSeconds());
                statement.setInt(8, snapshot.commandCount());
                statement.setInt(9, snapshot.teleportCount());
                statement.setInt(10, snapshot.gamemodeChangeCount());
                statement.setInt(11, snapshot.staffActionCount());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    public List<SessionSnapshot> loadActiveSessionsForRecovery(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT uuid, latest_name, started_at, last_snapshot_at, online_seconds, active_seconds, afk_seconds,
                       command_count, teleport_count, gamemode_change_count, staff_action_count
                FROM staff_active_sessions
                """);
             ResultSet resultSet = statement.executeQuery()) {
            List<SessionSnapshot> snapshots = new ArrayList<>();
            while (resultSet.next()) {
                snapshots.add(new SessionSnapshot(
                        UUID.fromString(resultSet.getString("uuid")),
                        resultSet.getString("latest_name"),
                        Instant.parse(resultSet.getString("started_at")),
                        Instant.parse(resultSet.getString("last_snapshot_at")),
                        Duration.ofSeconds(resultSet.getLong("online_seconds")),
                        Duration.ofSeconds(resultSet.getLong("active_seconds")),
                        Duration.ofSeconds(resultSet.getLong("afk_seconds")),
                        resultSet.getInt("command_count"),
                        resultSet.getInt("teleport_count"),
                        resultSet.getInt("gamemode_change_count"),
                        resultSet.getInt("staff_action_count"),
                        SessionCloseReason.SERVER_SHUTDOWN
                ));
            }
            return List.copyOf(snapshots);
        }
    }

    private void upsertMember(Connection connection, SessionSnapshot snapshot) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO staff_members (
                    uuid,
                    latest_name,
                    first_seen,
                    last_seen,
                    total_online_seconds,
                    total_active_seconds,
                    total_afk_seconds,
                    total_sessions,
                    total_commands,
                    total_teleports,
                    total_gamemode_changes,
                    total_staff_actions
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    latest_name = excluded.latest_name,
                    last_seen = excluded.last_seen,
                    total_online_seconds = staff_members.total_online_seconds + excluded.total_online_seconds,
                    total_active_seconds = staff_members.total_active_seconds + excluded.total_active_seconds,
                    total_afk_seconds = staff_members.total_afk_seconds + excluded.total_afk_seconds,
                    total_sessions = staff_members.total_sessions + 1,
                    total_commands = staff_members.total_commands + excluded.total_commands,
                    total_teleports = staff_members.total_teleports + excluded.total_teleports,
                    total_gamemode_changes = staff_members.total_gamemode_changes + excluded.total_gamemode_changes,
                    total_staff_actions = staff_members.total_staff_actions + excluded.total_staff_actions
                """)) {
            statement.setString(1, snapshot.uuid().toString());
            statement.setString(2, snapshot.latestName());
            statement.setString(3, snapshot.startedAt().toString());
            statement.setString(4, snapshot.endedAt().toString());
            statement.setLong(5, snapshot.onlineTime().toSeconds());
            statement.setLong(6, snapshot.activeTime().toSeconds());
            statement.setLong(7, snapshot.afkTime().toSeconds());
            statement.setInt(8, snapshot.commandCount());
            statement.setInt(9, snapshot.teleportCount());
            statement.setInt(10, snapshot.gamemodeChangeCount());
            statement.setInt(11, snapshot.staffActionCount());
            statement.executeUpdate();
        }
    }

    private void deleteActiveSession(Connection connection, String uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM staff_active_sessions WHERE uuid = ?")) {
            statement.setString(1, uuid);
            statement.executeUpdate();
        }
    }

    private void insertSession(Connection connection, SessionSnapshot snapshot) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO staff_sessions (
                    uuid,
                    started_at,
                    ended_at,
                    online_seconds,
                    active_seconds,
                    afk_seconds,
                    command_count,
                    teleport_count,
                    gamemode_change_count,
                    staff_action_count,
                    close_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, snapshot.uuid().toString());
            statement.setString(2, snapshot.startedAt().toString());
            statement.setString(3, snapshot.endedAt().toString());
            statement.setLong(4, snapshot.onlineTime().toSeconds());
            statement.setLong(5, snapshot.activeTime().toSeconds());
            statement.setLong(6, snapshot.afkTime().toSeconds());
            statement.setInt(7, snapshot.commandCount());
            statement.setInt(8, snapshot.teleportCount());
            statement.setInt(9, snapshot.gamemodeChangeCount());
            statement.setInt(10, snapshot.staffActionCount());
            statement.setString(11, snapshot.closeReason().name());
            statement.executeUpdate();
        }
    }

    private void upsertDailyStats(Connection connection, SessionSnapshot snapshot, ZoneId timezone) throws SQLException {
        String statDate = DATE_FORMATTER.format(snapshot.endedAt().atZone(timezone).toLocalDate());
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO staff_daily_stats (
                    uuid,
                    stat_date,
                    online_seconds,
                    active_seconds,
                    afk_seconds,
                    session_count,
                    command_count,
                    teleport_count,
                    gamemode_change_count,
                    staff_action_count,
                    first_login_at,
                    last_logout_at
                ) VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid, stat_date) DO UPDATE SET
                    online_seconds = staff_daily_stats.online_seconds + excluded.online_seconds,
                    active_seconds = staff_daily_stats.active_seconds + excluded.active_seconds,
                    afk_seconds = staff_daily_stats.afk_seconds + excluded.afk_seconds,
                    session_count = staff_daily_stats.session_count + 1,
                    command_count = staff_daily_stats.command_count + excluded.command_count,
                    teleport_count = staff_daily_stats.teleport_count + excluded.teleport_count,
                    gamemode_change_count = staff_daily_stats.gamemode_change_count + excluded.gamemode_change_count,
                    staff_action_count = staff_daily_stats.staff_action_count + excluded.staff_action_count,
                    first_login_at = CASE
                        WHEN staff_daily_stats.first_login_at IS NULL OR excluded.first_login_at < staff_daily_stats.first_login_at
                        THEN excluded.first_login_at
                        ELSE staff_daily_stats.first_login_at
                    END,
                    last_logout_at = CASE
                        WHEN staff_daily_stats.last_logout_at IS NULL OR excluded.last_logout_at > staff_daily_stats.last_logout_at
                        THEN excluded.last_logout_at
                        ELSE staff_daily_stats.last_logout_at
                    END
                """)) {
            statement.setString(1, snapshot.uuid().toString());
            statement.setString(2, statDate);
            statement.setLong(3, snapshot.onlineTime().toSeconds());
            statement.setLong(4, snapshot.activeTime().toSeconds());
            statement.setLong(5, snapshot.afkTime().toSeconds());
            statement.setInt(6, snapshot.commandCount());
            statement.setInt(7, snapshot.teleportCount());
            statement.setInt(8, snapshot.gamemodeChangeCount());
            statement.setInt(9, snapshot.staffActionCount());
            statement.setString(10, snapshot.startedAt().toString());
            statement.setString(11, snapshot.endedAt().toString());
            statement.executeUpdate();
        }
    }
}
