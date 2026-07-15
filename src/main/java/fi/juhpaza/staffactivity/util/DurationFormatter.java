package fi.juhpaza.staffactivity.util;

import java.time.Duration;

/**
 * Formats durations for compact command output.
 */
public final class DurationFormatter {
    private DurationFormatter() {
    }

    public static String seconds(long seconds) {
        Duration duration = Duration.ofSeconds(Math.max(0, seconds));
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long secondPart = duration.toSecondsPart();
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + secondPart + "s";
        }
        return secondPart + "s";
    }
}
