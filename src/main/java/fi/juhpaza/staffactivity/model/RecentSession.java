package fi.juhpaza.staffactivity.model;

/**
 * Compact representation of a recent stored staff session.
 */
public record RecentSession(
        String startedAt,
        String endedAt,
        long onlineSeconds,
        long activeSeconds,
        long afkSeconds,
        int commandCount,
        int teleportCount,
        int gamemodeChangeCount,
        int staffActionCount,
        String closeReason
) {
}
