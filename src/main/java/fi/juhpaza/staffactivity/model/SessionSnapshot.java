package fi.juhpaza.staffactivity.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable view of a staff session at a specific point in time.
 */
public record SessionSnapshot(
        UUID uuid,
        String latestName,
        Instant startedAt,
        Instant endedAt,
        Duration onlineTime,
        Duration activeTime,
        Duration afkTime,
        int commandCount,
        int teleportCount,
        int gamemodeChangeCount,
        int staffActionCount,
        SessionCloseReason closeReason
) {
}
