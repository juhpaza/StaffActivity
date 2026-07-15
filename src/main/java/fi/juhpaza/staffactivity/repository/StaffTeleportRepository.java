package fi.juhpaza.staffactivity.repository;

import fi.juhpaza.staffactivity.model.RecentTeleport;
import fi.juhpaza.staffactivity.model.TeleportRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists and reads detailed staff teleport events.
 */
public final class StaffTeleportRepository {
    public void saveTeleport(Connection connection, TeleportRecord record) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO staff_teleport_events (
                    uuid, latest_name, created_at, cause,
                    from_world, from_x, from_y, from_z,
                    to_world, to_x, to_y, to_z, vanished
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, record.uuid().toString());
            statement.setString(2, record.latestName());
            statement.setString(3, record.createdAt().toString());
            statement.setString(4, record.cause());
            statement.setString(5, record.fromWorld());
            statement.setDouble(6, record.fromX());
            statement.setDouble(7, record.fromY());
            statement.setDouble(8, record.fromZ());
            statement.setString(9, record.toWorld());
            statement.setDouble(10, record.toX());
            statement.setDouble(11, record.toY());
            statement.setDouble(12, record.toZ());
            if (record.vanished() == null) {
                statement.setObject(13, null);
            } else {
                statement.setInt(13, record.vanished() ? 1 : 0);
            }
            statement.executeUpdate();
        }
    }

    public List<RecentTeleport> findRecentTeleports(Connection connection, String uuid, int limit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT created_at, cause, from_world, from_x, from_y, from_z,
                       to_world, to_x, to_y, to_z, vanished
                FROM staff_teleport_events
                WHERE uuid = ?
                ORDER BY created_at DESC
                LIMIT ?
                """)) {
            statement.setString(1, uuid);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RecentTeleport> teleports = new ArrayList<>();
                while (resultSet.next()) {
                    teleports.add(new RecentTeleport(
                            resultSet.getString("created_at"),
                            resultSet.getString("cause"),
                            resultSet.getString("from_world"),
                            resultSet.getDouble("from_x"),
                            resultSet.getDouble("from_y"),
                            resultSet.getDouble("from_z"),
                            resultSet.getString("to_world"),
                            resultSet.getDouble("to_x"),
                            resultSet.getDouble("to_y"),
                            resultSet.getDouble("to_z"),
                            nullableBoolean(resultSet, "vanished")
                    ));
                }
                return List.copyOf(teleports);
            }
        }
    }

    private Boolean nullableBoolean(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        if (resultSet.wasNull()) {
            return null;
        }
        return value == 1;
    }
}
