package fi.juhpaza.staffactivity.discord;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DiscordWebhookClientTest {
    @Test
    void jsonPayloadEscapesDiscordContent() {
        String payload = DiscordWebhookClient.jsonPayload("Rivi \"yksi\"\npolku \\ test");

        assertTrue(payload.contains("\\\"yksi\\\""));
        assertTrue(payload.contains("\\n"));
        assertTrue(payload.contains("\\\\"));
        assertFalse(payload.contains("\n"));
    }

    @Test
    void jsonPayloadLimitsContentLength() {
        String payload = DiscordWebhookClient.jsonPayload("a".repeat(2_100));

        assertTrue(payload.length() < 2_000);
        assertTrue(payload.contains("..."));
    }
}
