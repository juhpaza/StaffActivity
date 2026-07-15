package fi.juhpaza.staffactivity.service;

import fi.juhpaza.staffactivity.model.SessionCloseReason;
import fi.juhpaza.staffactivity.model.SessionSnapshot;
import fi.juhpaza.staffactivity.model.StaffSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages currently tracked in-memory staff sessions.
 */
public final class SessionService {
    private final Duration afkTimeout;
    private final Map<UUID, StaffSession> sessions = new ConcurrentHashMap<>();

    public SessionService(Duration afkTimeout) {
        this.afkTimeout = afkTimeout;
    }

    public StaffSession startSession(UUID uuid, String latestName, Instant at) {
        return sessions.compute(uuid, (ignored, existing) -> {
            if (existing != null) {
                existing.updateName(latestName);
                existing.markActivity(at);
                return existing;
            }
            return new StaffSession(uuid, latestName, at, afkTimeout);
        });
    }

    public Optional<SessionSnapshot> closeSession(UUID uuid, Instant at, SessionCloseReason closeReason) {
        StaffSession removed = sessions.remove(uuid);
        if (removed == null) {
            return Optional.empty();
        }
        return Optional.of(removed.close(at, closeReason));
    }

    public Optional<StaffSession> session(UUID uuid) {
        return Optional.ofNullable(sessions.get(uuid));
    }

    public Collection<StaffSession> activeSessions() {
        return sessions.values();
    }

    public int activeSessionCount() {
        return sessions.size();
    }
}
