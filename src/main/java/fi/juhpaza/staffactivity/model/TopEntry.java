package fi.juhpaza.staffactivity.model;

/**
 * One row in a StaffActivity top list.
 */
public record TopEntry(
        String uuid,
        String latestName,
        long value
) {
}
