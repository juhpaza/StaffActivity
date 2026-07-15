package fi.juhpaza.staffactivity.discord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal Discord webhook client with bounded HTTP timeouts.
 */
public final class DiscordWebhookClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;

    public DiscordWebhookClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build());
    }

    DiscordWebhookClient(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    public CompletableFuture<Void> send(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Discord webhook URL is not configured"));
        }
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload(content)))
                    .build();
        } catch (IllegalArgumentException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Discord webhook URL is invalid"));
        }

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(response -> {
                    int status = response.statusCode();
                    if (status < 200 || status >= 300) {
                        throw new IllegalStateException("Discord webhook returned HTTP " + status);
                    }
                    return null;
                });
    }

    static String jsonPayload(String content) {
        return "{\"content\":\"" + escapeJson(limitContent(content)) + "\"}";
    }

    private static String limitContent(String content) {
        String value = Objects.requireNonNullElse(content, "");
        if (value.length() <= 1900) {
            return value;
        }
        return value.substring(0, 1897) + "...";
    }

    private static String escapeJson(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }
        return builder.toString();
    }
}
