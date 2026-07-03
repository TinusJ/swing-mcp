package io.github.tinusj.swingmcp.server.registry;

import io.github.tinusj.swingmcp.server.domain.NoActiveSessionException;
import io.github.tinusj.swingmcp.server.session.AppSession;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Holds the single active {@link AppSession}. The server manages one target
 * Swing application at a time; starting a new session replaces (and closes)
 * any previous one.
 */
@Component
public class SessionRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SessionRegistry.class);

    private final AtomicReference<AppSession> current = new AtomicReference<>();

    /**
     * Returns the active session.
     *
     * @throws NoActiveSessionException if no session is active
     */
    public AppSession require() {
        AppSession session = current.get();
        if (session == null) {
            throw new NoActiveSessionException();
        }
        return session;
    }

    /** Returns true if a session is currently active. */
    public boolean hasSession() {
        return current.get() != null;
    }

    /**
     * Activates a new session, closing any previously active one.
     */
    public void set(AppSession session) {
        AppSession previous = current.getAndSet(session);
        closeQuietly(previous);
    }

    /**
     * Clears and closes the active session, if any.
     */
    public void clear() {
        closeQuietly(current.getAndSet(null));
    }

    private void closeQuietly(AppSession session) {
        if (session != null) {
            try {
                session.close();
            } catch (IOException e) {
                LOG.warn("Failed to close previous session", e);
            }
        }
    }
}
