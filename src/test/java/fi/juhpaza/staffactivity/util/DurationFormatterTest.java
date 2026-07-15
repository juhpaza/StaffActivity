package fi.juhpaza.staffactivity.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class DurationFormatterTest {
    @Test
    void formatsHoursAndMinutes() {
        assertEquals("2h 5m", DurationFormatter.seconds(7500));
    }

    @Test
    void formatsMinutesAndSeconds() {
        assertEquals("3m 12s", DurationFormatter.seconds(192));
    }

    @Test
    void formatsSeconds() {
        assertEquals("42s", DurationFormatter.seconds(42));
    }
}
