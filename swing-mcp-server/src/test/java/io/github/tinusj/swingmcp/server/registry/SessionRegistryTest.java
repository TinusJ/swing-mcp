package io.github.tinusj.swingmcp.server.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tinusj.swingmcp.common.enums.AttachMode;
import io.github.tinusj.swingmcp.server.domain.NoActiveSessionException;
import io.github.tinusj.swingmcp.server.session.AgentConnection;
import io.github.tinusj.swingmcp.server.session.AttachedAppSession;
import io.github.tinusj.swingmcp.server.session.FakeAgentServer;
import org.junit.jupiter.api.Test;

class SessionRegistryTest {

    @Test
    void requireThrowsWhenEmpty() {
        SessionRegistry registry = new SessionRegistry();
        assertFalse(registry.hasSession());
        assertThrows(NoActiveSessionException.class, registry::require);
    }

    @Test
    void setAndClearManageSessionLifecycle() throws Exception {
        try (FakeAgentServer server = FakeAgentServer.echoing()) {
            AgentConnection connection = AgentConnection.connect(server.port(), 5000);
            AttachedAppSession session = new AttachedAppSession(ProcessHandle.current().pid(), connection, server.port());

            SessionRegistry registry = new SessionRegistry();
            registry.set(session);
            assertTrue(registry.hasSession());
            assertEquals(AttachMode.ATTACHED, registry.require().info().mode());

            registry.clear();
            assertFalse(registry.hasSession());
            assertFalse(session.isAlive());
        }
    }

    @Test
    void setClosesPreviousSession() throws Exception {
        try (FakeAgentServer server = FakeAgentServer.echoing()) {
            AgentConnection first = AgentConnection.connect(server.port(), 5000);
            AttachedAppSession firstSession = new AttachedAppSession(ProcessHandle.current().pid(), first, server.port());
            AgentConnection second = AgentConnection.connect(server.port(), 5000);
            AttachedAppSession secondSession = new AttachedAppSession(ProcessHandle.current().pid(), second, server.port());

            SessionRegistry registry = new SessionRegistry();
            registry.set(firstSession);
            registry.set(secondSession);

            assertFalse(firstSession.isAlive());
            assertTrue(secondSession.isAlive());
            registry.clear();
        }
    }
}
