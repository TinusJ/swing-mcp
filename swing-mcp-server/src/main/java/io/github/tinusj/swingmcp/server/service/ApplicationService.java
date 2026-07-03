package io.github.tinusj.swingmcp.server.service;

import io.github.tinusj.swingmcp.server.config.SwingMcpProperties;
import io.github.tinusj.swingmcp.server.domain.AgentCommandException;
import io.github.tinusj.swingmcp.server.domain.SessionInfo;
import io.github.tinusj.swingmcp.server.registry.SessionRegistry;
import io.github.tinusj.swingmcp.server.session.AgentConnection;
import io.github.tinusj.swingmcp.server.session.AppSession;
import io.github.tinusj.swingmcp.server.session.AttachedAppSession;
import io.github.tinusj.swingmcp.server.session.LaunchedAppSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Manages the lifecycle of the target Swing application session:
 * launching a new JVM with the agent preloaded, attaching to a running JVM
 * by PID, and stopping/disconnecting.
 */
@Service
public class ApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationService.class);
    private static final long PORT_FILE_POLL_MS = 200;
    private static final long PORT_FILE_TIMEOUT_MS = 30000;

    private final SessionRegistry registry;
    private final SwingMcpProperties properties;

    public ApplicationService(SessionRegistry registry, SwingMcpProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    /**
     * Launches a Swing application with the agent preloaded via {@code -javaagent}.
     *
     * @param command    full java command line (e.g. "java -jar app.jar")
     * @param workingDir optional working directory
     * @return session info for the launched application
     */
    public SessionInfo launch(String command, String workingDir) {
        Path agentJar = requireAgentJar();
        try {
            Path portFile = Files.createTempFile("swing-mcp-port", ".txt");
            Files.deleteIfExists(portFile);

            List<String> args = new ArrayList<>(List.of(command.trim().split("\\s+")));
            if (args.isEmpty() || args.getFirst().isBlank()) {
                throw new IllegalArgumentException("Launch command must not be empty");
            }
            String agentArg = "-javaagent:" + agentJar.toAbsolutePath()
                + "=portMin=" + properties.getAgentPortMin()
                + ",portMax=" + properties.getAgentPortMax()
                + ",responseFile=" + portFile.toAbsolutePath();
            args.add(1, agentArg);

            ProcessBuilder pb = new ProcessBuilder(args);
            if (workingDir != null && !workingDir.isBlank()) {
                pb.directory(Path.of(workingDir).toFile());
            }
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();

            int port = awaitPortFile(portFile, process);
            AgentConnection connection = AgentConnection.connect(port, properties.getToolTimeoutMs());
            AppSession session = new LaunchedAppSession(process, connection, port, "Launched: " + command);
            registry.set(session);
            LOG.info("Launched application pid={} agentPort={}", process.pid(), port);
            return session.info();
        } catch (IOException e) {
            throw new AgentCommandException("Failed to launch application: " + e.getMessage(), e);
        }
    }

    /**
     * Attaches the agent to an already-running JVM by PID and connects to it.
     *
     * @param pid the target JVM process id
     * @return session info for the attached application
     */
    public SessionInfo attach(long pid) {
        Path agentJar = requireAgentJar();
        try {
            Path portFile = Files.createTempFile("swing-mcp-port", ".txt");
            Files.deleteIfExists(portFile);

            String agentArgs = "portMin=" + properties.getAgentPortMin()
                + ",portMax=" + properties.getAgentPortMax()
                + ",responseFile=" + portFile.toAbsolutePath();
            loadAgent(pid, agentJar, agentArgs);

            int port = awaitPortFile(portFile, null);
            AgentConnection connection = AgentConnection.connect(port, properties.getToolTimeoutMs());
            AppSession session = new AttachedAppSession(pid, connection, port);
            registry.set(session);
            LOG.info("Attached to pid={} agentPort={}", pid, port);
            return session.info();
        } catch (IOException e) {
            throw new AgentCommandException("Failed to attach to PID " + pid + ": " + e.getMessage(), e);
        }
    }

    /**
     * Stops the active session. A launched application is terminated;
     * an attached application is only disconnected.
     */
    public String stop() {
        if (!registry.hasSession()) {
            return "No active session";
        }
        SessionInfo info = registry.require().info();
        registry.clear();
        return "Session closed: " + info.description();
    }

    /** Returns status of the current session. */
    public Map<String, Object> status() {
        if (!registry.hasSession()) {
            return Map.of("connected", false);
        }
        AppSession session = registry.require();
        SessionInfo info = session.info();
        return Map.of(
            "connected", true,
            "alive", session.isAlive(),
            "mode", info.mode().name(),
            "pid", info.pid(),
            "agentPort", info.agentPort(),
            "description", info.description()
        );
    }

    private Path requireAgentJar() {
        String path = properties.getAgentJar();
        if (path == null || path.isBlank()) {
            throw new IllegalStateException(
                "swing.mcp.agent-jar is not configured. Set it to the swing-mcp-agent shaded jar path.");
        }
        Path jar = Path.of(path);
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException("Agent jar not found at: " + jar.toAbsolutePath());
        }
        return jar;
    }

    private void loadAgent(long pid, Path agentJar, String agentArgs) throws IOException {
        try {
            com.sun.tools.attach.VirtualMachine vm = com.sun.tools.attach.VirtualMachine.attach(String.valueOf(pid));
            try {
                vm.loadAgent(agentJar.toAbsolutePath().toString(), agentArgs);
            } finally {
                vm.detach();
            }
        } catch (com.sun.tools.attach.AttachNotSupportedException
                 | com.sun.tools.attach.AgentLoadException
                 | com.sun.tools.attach.AgentInitializationException e) {
            throw new IOException("VM attach failed: " + e.getMessage(), e);
        }
    }

    private int awaitPortFile(Path portFile, Process process) throws IOException {
        long deadline = System.currentTimeMillis() + PORT_FILE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (process != null && !process.isAlive()) {
                throw new IOException("Target process exited before agent started (exit=" + process.exitValue() + ")");
            }
            if (Files.isRegularFile(portFile)) {
                String content = Files.readString(portFile).trim();
                if (!content.isEmpty()) {
                    Files.deleteIfExists(portFile);
                    return Integer.parseInt(content);
                }
            }
            try {
                Thread.sleep(PORT_FILE_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for agent port", e);
            }
        }
        throw new IOException("Timed out waiting for agent port file: " + portFile);
    }
}
