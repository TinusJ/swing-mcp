package io.github.tinusj.swingmcp.server.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tinusj.swingmcp.common.command.CommandRequest;
import io.github.tinusj.swingmcp.common.command.CommandResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Test double that speaks the agent's JSON line protocol on a loopback socket.
 */
public class FakeAgentServer implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Function<CommandRequest, CommandResponse> handler;
    private final Thread acceptThread;
    private volatile boolean running = true;

    public FakeAgentServer(Function<CommandRequest, CommandResponse> handler) throws IOException {
        this.handler = handler;
        this.serverSocket = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
        this.acceptThread = new Thread(this::acceptLoop, "fake-agent-server");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    /** Creates a server that echoes success with the request type name as result. */
    public static FakeAgentServer echoing() throws IOException {
        return new FakeAgentServer(req ->
            new CommandResponse(req.requestId(), true, req.type().name(), null));
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (running) {
            try (Socket client = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(
                     new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(client.getOutputStream(), true, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    CommandRequest request = mapper.readValue(line, CommandRequest.class);
                    writer.println(mapper.writeValueAsString(handler.apply(request)));
                }
            } catch (IOException e) {
                if (running) {
                    // Client disconnected; keep accepting.
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        serverSocket.close();
    }
}
