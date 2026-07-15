package fi.juhpaza.staffactivity.model;

/**
 * Day-level aggregate statistics for one tracked staff member.
 */
public record DailyStats(
        String uuid,
        String statDate,
        long onlineSeconds,
        long activeSeconds,
        long afkSeconds,
        int sessionCount,
        int commandCount,
        int teleportCount,
        int gamemodeChangeCount,
        int staffActionCount,
        String firstLoginAt,
        String lastLogoutAt
) {
}
