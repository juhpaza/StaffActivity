package fi.juhpaza.staffactivity.model;

/**
 * One staff member row in a Discord activity report.
 */
public record StaffReportEntry(
        String uuid,
        String latestName,
        long onlineSeconds,
        long activeSeconds,
        long afkSeconds,
        int sessionCount,
        int commandCount,
        int teleportCount,
        int gamemodeChangeCount,
        int staffActionCount
) {
    public long activityPercent() {
        if (onlineSeconds <= 0) {
            return 0;
        }
        return Math.round((activeSeconds * 100.0) / onlineSeconds);
    }
}
