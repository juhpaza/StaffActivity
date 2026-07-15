package fi.juhpaza.staffactivity.discord;

import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.juhpaza.staffactivity.model.StaffReportEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DiscordMessageFormatterTest {
    @Test
    void reportContainsActivityMetrics() {
        String message = new DiscordMessageFormatter().report(
                "Paivaraportti",
                "2026-07-15",
                List.of(new StaffReportEntry("uuid", "Admin", 600, 450, 150, 1, 2, 3, 0, 4))
        );

        assertTrue(message.contains("Admin"));
        assertTrue(message.contains("10m 0s online"));
        assertTrue(message.contains("75% aktiivinen"));
        assertTrue(message.contains("Komennot: 2"));
        assertTrue(message.contains("Teleportit: 3"));
        assertTrue(message.contains("Toimet: 4"));
    }
}
