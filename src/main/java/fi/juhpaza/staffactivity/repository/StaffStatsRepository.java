package fi.juhpaza.staffactivity.repository;

import fi.juhpaza.staffactivity.model.DailyStats;
import fi.juhpaza.staffactivity.model.RecentSession;
import fi.juhpaza.staffactivity.model.StaffSummary;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads StaffActivity summaries and session history.
 */
public final class StaffStatsRepository {
    public Optional<StaffSummary> findSummaryByUuid(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM staff_members WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(summary(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<StaffSummary> findSummaryByName(Connection connection, String latestName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM staff_members WHERE lower(latest_name) = lower(?) ORDER BY last_seen DESC LIMIT 1")) {
            statement.setString(1, latestName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(summary(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<DailyStats> findDailyStats(Connection connection, String uuid, String statDate) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM staff_daily_stats WHERE uuid = ? AND stat_date = ?")) {
            statement.setString(1, uuid);
            statement.setString(2, statDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(daily(resultSet)) : Optional.empty();
            }
        }
    }

    public List<RecentSession> findRecentSessions(Connection connection, String uuid, int limit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT started_at, ended_at, online_seconds, active_seconds, afk_seconds,
                       command_count, teleport_count, gamemode_change_count, staff_action_count, close_reason
                FROM staff_sessions
                WHERE uuid = ?
                ORDER BY started_at DESC
                LIMIT ?
                """)) {
            statement.setString(1, uuid);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RecentSession> sessions = new ArrayList<>();
                while (resultSet.next()) {
                    sessions.add(new RecentSession(
                            resultSet.getString("started_at"),
                            resultSet.getString("ended_at"),
                            resultSet.getLong("online_seconds"),
                            resultSet.getLong("active_seconds"),
                            resultSet.getLong("afk_seconds"),
                            resultSet.getInt("command_count"),
                            resultSet.getInt("teleport_count"),
                            resultSet.getInt("gamemode_change_count"),
                            resultSet.getInt("staff_action_count"),
                            resultSet.getString("close_reason")
                    ));
                }
                return List.copyOf(sessions);
            }
        }
    }

    private StaffSummary summary(ResultSet resultSet) throws SQLException {
        return new StaffSummary(
                resultSet.getString("uuid"),
                resultSet.getString("latest_name"),
                resultSet.getString("first_seen"),
                resultSet.getString("last_seen"),
                resultSet.getLong("total_online_seconds"),
                resultSet.getLong("total_active_seconds"),
                resultSet.getLong("total_afk_seconds"),
                resultSet.getInt("total_sessions"),
                resultSet.getInt("total_commands"),
                resultSet.getInt("total_teleports"),
                resultSet.getInt("total_gamemode_changes"),
                resultSet.getInt("total_staff_actions")
        );
    }

    private DailyStats daily(ResultSet resultSet) throws SQLException {
        return new DailyStats(
                resultSet.getString("uuid"),
                resultSet.getString("stat_date"),
                resultSet.getLong("online_seconds"),
                resultSet.getLong("active_seconds"),
                resultSet.getLong("afk_seconds"),
                resultSet.getInt("session_count"),
                resultSet.getInt("command_count"),
                resultSet.getInt("teleport_count"),
                resultSet.getInt("gamemode_change_count"),
                resultSet.getInt("staff_action_count"),
                resultSet.getString("first_login_at"),
                resultSet.getString("last_logout_at")
        );
    }
}
