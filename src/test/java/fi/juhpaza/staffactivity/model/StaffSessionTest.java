package fi.juhpaza.staffactivity.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class StaffSessionTest {
    private static final Duration AFK_TIMEOUT = Duration.ofMinutes(5);

    @Test
    void countsActiveTimeBeforeAfkTimeout() {
        Instant start = Instant.parse("2026-07-15T10:00:00Z");
        StaffSession session = new StaffSession(UUID.randomUUID(), "Admin", start, AFK_TIMEOUT);

        SessionSnapshot snapshot = session.snapshot(start.plus(Duration.ofMinutes(3)));

        assertEquals(Duration.ofMinutes(3), snapshot.activeTime());
        assertEquals(Duration.ZERO, snapshot.afkTime());
        assertEquals(Duration.ofMinutes(3), snapshot.onlineTime());
    }

    @Test
    void splitsActiveAndAfkTimeAfterTimeout() {
        Instant start = Instant.parse("2026-07-15T10:00:00Z");
        StaffSession session = new StaffSession(UUID.randomUUID(), "Admin", start, AFK_TIMEOUT);

        SessionSnapshot snapshot = session.snapshot(start.plus(Duration.ofMinutes(8)));

        assertEquals(Duration.ofMinutes(5), snapshot.activeTime());
        assertEquals(Duration.ofMinutes(3), snapshot.afkTime());
        assertEquals(Duration.ofMinutes(8), snapshot.onlineTime());
    }

    @Test
    void activityAfterAfkReturnsToActiveTracking() {
        Instant start = Instant.parse("2026-07-15T10:00:00Z");
        StaffSession session = new StaffSession(UUID.randomUUID(), "Admin", start, AFK_TIMEOUT);

        session.snapshot(start.plus(Duration.ofMinutes(8)));
        session.markActivity(start.plus(Duration.ofMinutes(8)));
        SessionSnapshot snapshot = session.snapshot(start.plus(Duration.ofMinutes(10)));

        assertEquals(Duration.ofMinutes(7), snapshot.activeTime());
        assertEquals(Duration.ofMinutes(3), snapshot.afkTime());
        assertEquals(Duration.ofMinutes(10), snapshot.onlineTime());
    }

    @Test
    void closeAddsCloseReasonAndCounters() {
        Instant start = Instant.parse("2026-07-15T10:00:00Z");
        StaffSession session = new StaffSession(UUID.randomUUID(), "Admin", start, AFK_TIMEOUT);

        session.incrementCommands(start.plusSeconds(10));
        session.incrementTeleports(start.plusSeconds(20));
        session.incrementGamemodeChanges(start.plusSeconds(30));
        session.incrementStaffActions(start.plusSeconds(40));
        SessionSnapshot snapshot = session.close(start.plus(Duration.ofMinutes(1)), SessionCloseReason.NORMAL);

        assertEquals(SessionCloseReason.NORMAL, snapshot.closeReason());
        assertEquals(1, snapshot.commandCount());
        assertEquals(1, snapshot.teleportCount());
        assertEquals(1, snapshot.gamemodeChangeCount());
        assertEquals(1, snapshot.staffActionCount());
    }
}
