package fi.juhpaza.staffactivity.database;

/**
 * Safe public database lifecycle state for debug output.
 */
public enum DatabaseStatus {
    NOT_STARTED,
    INITIALIZING,
    READY,
    FAILED,
    CLOSED
}
