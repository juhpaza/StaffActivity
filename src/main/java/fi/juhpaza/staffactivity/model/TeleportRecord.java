package fi.juhpaza.staffactivity.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One staff teleport event captured from Paper's teleport event.
 */
public record TeleportRecord(
        UUID uuid,
        String latestName,
        Instant createdAt,
        String cause,
        String fromWorld,
        double fromX,
        double fromY,
        double fromZ,
        String toWorld,
        double toX,
        double toY,
        double toZ,
        Boolean vanished
) {
}
