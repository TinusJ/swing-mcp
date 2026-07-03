package io.github.tinusj.swingmcp.server.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tinusj.swingmcp.common.command.CommandResponse;
import io.github.tinusj.swingmcp.common.enums.CommandType;
import io.github.tinusj.swingmcp.server.domain.AgentCommandException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentConnectionTest {

    @Test
    void sendReturnsResultOnSuccess() throws Exception {
        try (FakeAgentServer server = FakeAgentServer.echoing();
             AgentConnection connection = AgentConnection.connect(server.port(), 5000)) {
            Object result = connection.send(CommandType.PING, Map.of());
            assertEquals("PING", result);
            assertTrue(connection.isOpen());
        }
    }

    @Test
    void sendThrowsOnAgentError() throws Exception {
        try (FakeAgentServer server = new FakeAgentServer(req ->
                new CommandResponse(req.requestId(), false, null, "boom"));
             AgentConnection connection = AgentConnection.connect(server.port(), 5000)) {
            AgentCommandException ex = assertThrows(AgentCommandException.class,
                () -> connection.send(CommandType.CLICK, Map.of("uid", "comp-1")));
            assertTrue(ex.getMessage().contains("boom"));
        }
    }

    @Test
    void sendPassesParamsThrough() throws Exception {
        try (FakeAgentServer server = new FakeAgentServer(req ->
                new CommandResponse(req.requestId(), true, req.params(), null));
             AgentConnection connection = AgentConnection.connect(server.port(), 5000)) {
            Object result = connection.send(CommandType.FILL, Map.of("uid", "comp-2", "text", "hello"));
            assertEquals(Map.of("uid", "comp-2", "text", "hello"), result);
        }
    }
}
