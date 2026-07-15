package fi.juhpaza.staffactivity.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Mutable in-memory staff session accumulator.
 */
public final class StaffSession {
    private final UUID uuid;
    private final Instant startedAt;
    private final Duration afkTimeout;

    private String latestName;
    private Instant lastActivityAt;
    private Instant lastEvaluatedAt;
    private Duration activeTime = Duration.ZERO;
    private Duration afkTime = Duration.ZERO;
    private boolean afk;
    private int commandCount;
    private int teleportCount;
    private int gamemodeChangeCount;
    private int staffActionCount;

    public StaffSession(UUID uuid, String latestName, Instant startedAt, Duration afkTimeout) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.latestName = Objects.requireNonNull(latestName, "latestName");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.lastActivityAt = startedAt;
        this.lastEvaluatedAt = startedAt;
        this.afkTimeout = Objects.requireNonNull(afkTimeout, "afkTimeout");
    }

    public UUID uuid() {
        return uuid;
    }

    public void updateName(String latestName) {
        this.latestName = Objects.requireNonNull(latestName, "latestName");
    }

    public void markActivity(Instant at) {
        advanceTo(at);
        this.lastActivityAt = at;
        this.afk = false;
    }

    public void incrementCommands(Instant at) {
        markActivity(at);
        commandCount++;
    }

    public void incrementTeleports(Instant at) {
        markActivity(at);
        teleportCount++;
    }

    public void incrementGamemodeChanges(Instant at) {
        markActivity(at);
        gamemodeChangeCount++;
    }

    public void incrementStaffActions(Instant at) {
        markActivity(at);
        staffActionCount++;
    }

    public SessionSnapshot snapshot(Instant at) {
        advanceTo(at);
        return new SessionSnapshot(
                uuid,
                latestName,
                startedAt,
                at,
                Duration.between(startedAt, at),
                activeTime,
                afkTime,
                commandCount,
                teleportCount,
                gamemodeChangeCount,
                staffActionCount,
                null
        );
    }

    public SessionSnapshot close(Instant at, SessionCloseReason closeReason) {
        advanceTo(at);
        return new SessionSnapshot(
                uuid,
                latestName,
                startedAt,
                at,
                Duration.between(startedAt, at),
                activeTime,
                afkTime,
                commandCount,
                teleportCount,
                gamemodeChangeCount,
                staffActionCount,
                closeReason
        );
    }

    private void advanceTo(Instant at) {
        if (!at.isAfter(lastEvaluatedAt)) {
            return;
        }

        Instant afkStartsAt = lastActivityAt.plus(afkTimeout);
        if (afk || !at.isBefore(afkStartsAt)) {
            Instant activeUntil = min(at, afkStartsAt);
            if (activeUntil.isAfter(lastEvaluatedAt)) {
                activeTime = activeTime.plus(Duration.between(lastEvaluatedAt, activeUntil));
            }

            Instant afkFrom = max(lastEvaluatedAt, afkStartsAt);
            if (at.isAfter(afkFrom)) {
                afkTime = afkTime.plus(Duration.between(afkFrom, at));
                afk = true;
            }
        } else {
            activeTime = activeTime.plus(Duration.between(lastEvaluatedAt, at));
        }

        lastEvaluatedAt = at;
    }

    private Instant min(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }

    private Instant max(Instant left, Instant right) {
        return left.isAfter(right) ? left : right;
    }
}
