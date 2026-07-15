package fi.juhpaza.staffactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.juhpaza.staffactivity.model.SessionCloseReason;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SessionServiceTest {
    @Test
    void startsAndClosesSession() {
        SessionService service = new SessionService(Duration.ofMinutes(5));
        UUID uuid = UUID.randomUUID();
        Instant start = Instant.parse("2026-07-15T10:00:00Z");

        service.startSession(uuid, "Admin", start);

        assertEquals(1, service.activeSessionCount());
        assertTrue(service.closeSession(uuid, start.plus(Duration.ofMinutes(1)), SessionCloseReason.NORMAL).isPresent());
        assertEquals(0, service.activeSessionCount());
    }
}
