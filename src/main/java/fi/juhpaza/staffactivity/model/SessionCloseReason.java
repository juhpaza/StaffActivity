package fi.juhpaza.staffactivity.model;

/**
 * Reason why an in-memory staff session was closed.
 */
public enum SessionCloseReason {
    NORMAL,
    PERMISSION_REMOVED,
    PLUGIN_SHUTDOWN,
    SERVER_SHUTDOWN
}
