package fi.juhpaza.staffactivity.model;

/**
 * Read model for recent staff teleport history.
 */
public record RecentTeleport(
        String createdAt,
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
