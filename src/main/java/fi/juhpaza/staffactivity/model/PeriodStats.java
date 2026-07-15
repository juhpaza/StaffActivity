package fi.juhpaza.staffactivity.model;

/**
 * Aggregate statistics for a date range.
 */
public record PeriodStats(
        long onlineSeconds,
        long activeSeconds,
        long afkSeconds,
        int sessionCount,
        int commandCount,
        int teleportCount,
        int gamemodeChangeCount,
        int staffActionCount
) {
    public boolean empty() {
        return sessionCount == 0
                && onlineSeconds == 0
                && activeSeconds == 0
                && afkSeconds == 0
                && commandCount == 0
                && teleportCount == 0
                && gamemodeChangeCount == 0
                && staffActionCount == 0;
    }
}
