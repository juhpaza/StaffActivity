package fi.juhpaza.staffactivity.model;

/**
 * All-time aggregate statistics for one tracked staff member.
 */
public record StaffSummary(
        String uuid,
        String latestName,
        String firstSeen,
        String lastSeen,
        long totalOnlineSeconds,
        long totalActiveSeconds,
        long totalAfkSeconds,
        int totalSessions,
        int totalCommands,
        int totalTeleports,
        int totalGamemodeChanges,
        int totalStaffActions
) {
}
