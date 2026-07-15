package fi.juhpaza.staffactivity.repository;

import fi.juhpaza.staffactivity.model.DailyStats;
import fi.juhpaza.staffactivity.model.PeriodStats;
import fi.juhpaza.staffactivity.model.RecentSession;
import fi.juhpaza.staffactivity.model.StaffReportEntry;
import fi.juhpaza.staffactivity.model.StaffSummary;
import fi.juhpaza.staffactivity.model.TopEntry;
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

    public PeriodStats findPeriodStats(Connection connection, String uuid, String startDateInclusive, String endDateInclusive) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT
                    COALESCE(SUM(online_seconds), 0) AS online_seconds,
                    COALESCE(SUM(active_seconds), 0) AS active_seconds,
                    COALESCE(SUM(afk_seconds), 0) AS afk_seconds,
                    COALESCE(SUM(session_count), 0) AS session_count,
                    COALESCE(SUM(command_count), 0) AS command_count,
                    COALESCE(SUM(teleport_count), 0) AS teleport_count,
                    COALESCE(SUM(gamemode_change_count), 0) AS gamemode_change_count,
                    COALESCE(SUM(staff_action_count), 0) AS staff_action_count
                FROM staff_daily_stats
                WHERE uuid = ? AND stat_date BETWEEN ? AND ?
                """)) {
            statement.setString(1, uuid);
            statement.setString(2, startDateInclusive);
            statement.setString(3, endDateInclusive);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new PeriodStats(0, 0, 0, 0, 0, 0, 0, 0);
                }
                return new PeriodStats(
                        resultSet.getLong("online_seconds"),
                        resultSet.getLong("active_seconds"),
                        resultSet.getLong("afk_seconds"),
                        resultSet.getInt("session_count"),
                        resultSet.getInt("command_count"),
                        resultSet.getInt("teleport_count"),
                        resultSet.getInt("gamemode_change_count"),
                        resultSet.getInt("staff_action_count")
                );
            }
        }
    }

    public List<TopEntry> findTop(Connection connection, String metricColumn, int limit) throws SQLException {
        if (!List.of("total_online_seconds", "total_active_seconds", "total_staff_actions").contains(metricColumn)) {
            throw new IllegalArgumentException("Unsupported top metric: " + metricColumn);
        }
        String sql = "SELECT uuid, latest_name, " + metricColumn + " AS value FROM staff_members ORDER BY " + metricColumn + " DESC, latest_name ASC LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TopEntry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    entries.add(new TopEntry(
                            resultSet.getString("uuid"),
                            resultSet.getString("latest_name"),
                            resultSet.getLong("value")
                    ));
                }
                return List.copyOf(entries);
            }
        }
    }

    public List<StaffReportEntry> findDailyReport(Connection connection, String statDate) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT d.uuid, m.latest_name, d.online_seconds, d.active_seconds, d.afk_seconds,
                       d.session_count, d.command_count, d.teleport_count, d.gamemode_change_count, d.staff_action_count
                FROM staff_daily_stats d
                JOIN staff_members m ON m.uuid = d.uuid
                WHERE d.stat_date = ?
                ORDER BY d.active_seconds DESC, m.latest_name ASC
                """)) {
            statement.setString(1, statDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                return reportEntries(resultSet);
            }
        }
    }

    public List<StaffReportEntry> findPeriodReport(Connection connection, String startDateInclusive, String endDateInclusive) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT d.uuid, m.latest_name,
                       COALESCE(SUM(d.online_seconds), 0) AS online_seconds,
                       COALESCE(SUM(d.active_seconds), 0) AS active_seconds,
                       COALESCE(SUM(d.afk_seconds), 0) AS afk_seconds,
                       COALESCE(SUM(d.session_count), 0) AS session_count,
                       COALESCE(SUM(d.command_count), 0) AS command_count,
                       COALESCE(SUM(d.teleport_count), 0) AS teleport_count,
                       COALESCE(SUM(d.gamemode_change_count), 0) AS gamemode_change_count,
                       COALESCE(SUM(d.staff_action_count), 0) AS staff_action_count
                FROM staff_daily_stats d
                JOIN staff_members m ON m.uuid = d.uuid
                WHERE d.stat_date BETWEEN ? AND ?
                GROUP BY d.uuid, m.latest_name
                ORDER BY active_seconds DESC, m.latest_name ASC
                """)) {
            statement.setString(1, startDateInclusive);
            statement.setString(2, endDateInclusive);
            try (ResultSet resultSet = statement.executeQuery()) {
                return reportEntries(resultSet);
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

    private List<StaffReportEntry> reportEntries(ResultSet resultSet) throws SQLException {
        List<StaffReportEntry> entries = new ArrayList<>();
        while (resultSet.next()) {
            entries.add(new StaffReportEntry(
                    resultSet.getString("uuid"),
                    resultSet.getString("latest_name"),
                    resultSet.getLong("online_seconds"),
                    resultSet.getLong("active_seconds"),
                    resultSet.getLong("afk_seconds"),
                    resultSet.getInt("session_count"),
                    resultSet.getInt("command_count"),
                    resultSet.getInt("teleport_count"),
                    resultSet.getInt("gamemode_change_count"),
                    resultSet.getInt("staff_action_count")
            ));
        }
        return List.copyOf(entries);
    }
}
