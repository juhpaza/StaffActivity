package fi.juhpaza.staffactivity.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class DatabaseMigratorTest {
    @TempDir
    private Path tempDir;

    @Test
    void createsInitialSchema() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"))) {
            new DatabaseMigrator().migrate(connection);

            assertEquals(DatabaseMigrator.CURRENT_SCHEMA_VERSION, currentSchemaVersion(connection));
            assertTrue(tableNames(connection).containsAll(Set.of(
                    "schema_version",
                    "staff_members",
                    "staff_sessions",
                    "staff_daily_stats",
                    "staff_actions",
                    "staff_active_sessions",
                    "staff_teleport_events"
            )));
        }
    }

    @Test
    void migrationIsIdempotent() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"))) {
            DatabaseMigrator migrator = new DatabaseMigrator();
            migrator.migrate(connection);
            migrator.migrate(connection);

            assertEquals(DatabaseMigrator.CURRENT_SCHEMA_VERSION, currentSchemaVersion(connection));
        }
    }

    private int currentSchemaVersion(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT MAX(version) FROM schema_version")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private Set<String> tableNames(Connection connection) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            Set<String> names = new java.util.HashSet<>();
            while (resultSet.next()) {
                names.add(resultSet.getString("TABLE_NAME"));
            }
            return names.stream().collect(Collectors.toUnmodifiableSet());
        }
    }
}
