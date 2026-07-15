package fi.juhpaza.staffactivity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.juhpaza.staffactivity.database.DatabaseMigrator;
import fi.juhpaza.staffactivity.model.RecentTeleport;
import fi.juhpaza.staffactivity.model.TeleportRecord;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class StaffTeleportRepositoryTest {
    @TempDir
    private Path tempDir;

    @Test
    void savesAndReadsRecentTeleportsNewestFirst() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"))) {
            new DatabaseMigrator().migrate(connection);
            UUID uuid = UUID.randomUUID();
            StaffTeleportRepository repository = new StaffTeleportRepository();

            repository.saveTeleport(connection, record(uuid, "2026-07-15T10:00:00Z", "COMMAND", false));
            repository.saveTeleport(connection, record(uuid, "2026-07-15T10:05:00Z", "PLUGIN", true));

            List<RecentTeleport> teleports = repository.findRecentTeleports(connection, uuid.toString(), 10);

            assertEquals(2, teleports.size());
            assertEquals(2, repository.countTeleports(connection, uuid.toString()));
            assertEquals("2026-07-15T10:05:00Z", teleports.getFirst().createdAt());
            assertEquals("PLUGIN", teleports.getFirst().cause());
            assertTrue(teleports.getFirst().vanished());
            assertEquals("world", teleports.getFirst().fromWorld());
            assertEquals("world_nether", teleports.getFirst().toWorld());
        }
    }

    private TeleportRecord record(UUID uuid, String createdAt, String cause, boolean vanished) {
        return new TeleportRecord(
                uuid,
                "Admin",
                Instant.parse(createdAt),
                cause,
                "world",
                10.25,
                64.0,
                -5.75,
                "world_nether",
                100.0,
                70.5,
                -20.25,
                vanished
        );
    }
}
