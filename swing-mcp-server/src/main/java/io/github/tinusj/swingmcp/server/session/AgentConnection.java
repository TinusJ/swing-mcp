package io.github.tinusj.swingmcp.server.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tinusj.swingmcp.common.command.CommandRequest;
import io.github.tinusj.swingmcp.common.command.CommandResponse;
import io.github.tinusj.swingmcp.common.enums.CommandType;
import io.github.tinusj.swingmcp.server.domain.AgentCommandException;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * JSON line-protocol client for the agent's localhost socket server.
 * Requests are serialized as single-line JSON and answered with a single
 * JSON response line. Calls are serialized; one command is in flight at a time.
 */
public class AgentConnection implements Closeable {

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final ObjectMapper mapper = new ObjectMapper();

    private AgentConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
    }

    /**
     * Opens a connection to the agent on localhost.
     *
     * @param port          the agent's listening port
     * @param readTimeoutMs socket read timeout for command round-trips
     */
    public static AgentConnection connect(int port, long readTimeoutMs) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), CONNECT_TIMEOUT_MS);
        socket.setSoTimeout((int) readTimeoutMs);
        return new AgentConnection(socket);
    }

    /**
     * Sends a command and waits for its response.
     *
     * @throws AgentCommandException if the agent reports an error or the transport fails
     */
    public synchronized Object send(CommandType type, Map<String, Object> params) {
        String requestId = UUID.randomUUID().toString();
        try {
            String json = mapper.writeValueAsString(new CommandRequest(requestId, type, params));
            writer.println(json);
            String line = reader.readLine();
            if (line == null) {
                throw new AgentCommandException("Agent connection closed while waiting for response to " + type);
            }
            CommandResponse response = mapper.readValue(line, CommandResponse.class);
            if (!response.success()) {
                throw new AgentCommandException("Agent command " + type + " failed: " + response.error());
            }
            return response.result();
        } catch (IOException e) {
            throw new AgentCommandException("Agent command " + type + " transport failure: " + e.getMessage(), e);
        }
    }

    /** Returns true while the socket is open. */
    public boolean isOpen() {
        return !socket.isClosed() && socket.isConnected();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
