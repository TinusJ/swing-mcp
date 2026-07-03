package io.github.tinusj.swingmcp.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tinusj.swingmcp.common.command.CommandResponse;
import io.github.tinusj.swingmcp.common.enums.CommandType;
import io.github.tinusj.swingmcp.server.config.SwingMcpProperties;
import io.github.tinusj.swingmcp.server.registry.SessionRegistry;
import io.github.tinusj.swingmcp.server.session.AgentConnection;
import io.github.tinusj.swingmcp.server.session.AttachedAppSession;
import io.github.tinusj.swingmcp.server.session.FakeAgentServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises the command-forwarding services end-to-end against a
 * {@link FakeAgentServer} speaking the real line protocol.
 */
class ServicesTest {

    private final ConcurrentLinkedQueue<CommandType> received = new ConcurrentLinkedQueue<>();
    private FakeAgentServer server;
    private SessionRegistry registry;
    private SwingMcpProperties properties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeAgentServer(req -> {
            received.add(req.type());
            if (req.type() == CommandType.TAKE_SCREENSHOT) {
                return new CommandResponse(req.requestId(), true, screenshotPayload(), null);
            }
            return new CommandResponse(req.requestId(), true, req.type().name(), null);
        });
        AgentConnection connection = AgentConnection.connect(server.port(), 5000);
        registry = new SessionRegistry();
        registry.set(new AttachedAppSession(ProcessHandle.current().pid(), connection, server.port()));
        properties = new SwingMcpProperties();
        properties.setScreenshotDir(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        registry.clear();
        server.close();
    }

    @Test
    void snapshotServiceSendsCommands() {
        SnapshotService service = new SnapshotService(registry);
        assertEquals("TAKE_SNAPSHOT", service.takeSnapshot(null));
        assertEquals("GET_COMPONENT_DETAILS", service.componentDetails("comp-1"));
        assertTrue(received.contains(CommandType.TAKE_SNAPSHOT));
        assertTrue(received.contains(CommandType.GET_COMPONENT_DETAILS));
    }

    @Test
    void windowServiceSendsCommands() {
        WindowService service = new WindowService(registry);
        service.listWindows();
        service.selectWindow(0);
        service.resizeWindow(800, 600);
        service.closeWindow();
        assertTrue(received.containsAll(java.util.List.of(
            CommandType.LIST_WINDOWS, CommandType.SELECT_WINDOW,
            CommandType.RESIZE_WINDOW, CommandType.CLOSE_WINDOW)));
    }

    @Test
    void interactionServiceSendsCommands() {
        InteractionService service = new InteractionService(registry);
        service.click("comp-1", null, null);
        service.fill("comp-2", "hello");
        service.selectOption("comp-3", 1, null);
        service.selectTreeNode("comp-4", "Root > Leaf");
        service.selectTableCell("comp-5", 1, 2);
        service.selectMenuItem("File > Exit");
        service.pressKey("ENTER");
        service.drag("comp-6", "comp-7");
        service.scroll("comp-8", "DOWN", 3);
        assertEquals(9, received.size());
    }

    @Test
    void waitServiceSendsCommand() {
        WaitService service = new WaitService(registry);
        assertEquals("WAIT_FOR", service.waitFor("WINDOW_TITLE", null, "Demo", 1000L));
        assertTrue(received.contains(CommandType.WAIT_FOR));
    }

    @Test
    void evaluateServiceIsDisabledByDefault() {
        EvaluateService service = new EvaluateService(registry, properties);
        assertThrows(IllegalStateException.class, () -> service.evaluate("1+1"));
        assertTrue(received.isEmpty());
    }

    @Test
    void evaluateServiceSendsWhenEnabled() {
        properties.getEvaluate().setEnabled(true);
        EvaluateService service = new EvaluateService(registry, properties);
        assertEquals("EVALUATE_JAVA", service.evaluate("1+1"));
        assertTrue(received.contains(CommandType.EVALUATE_JAVA));
    }

    @Test
    void screenshotServiceWritesPngFile() throws Exception {
        ScreenshotService service = new ScreenshotService(registry, properties);
        Map<String, Object> result = service.screenshot(null);
        Path path = Path.of(String.valueOf(result.get("path")));
        assertTrue(Files.isRegularFile(path));
        assertTrue(Files.size(path) > 0);
        assertEquals("image/png", result.get("mimeType"));
    }

    private static Map<String, Object> screenshotPayload() {
        try {
            BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return Map.of(
                "imageBase64", Base64.getEncoder().encodeToString(baos.toByteArray()),
                "mimeType", "image/png",
                "width", 4,
                "height", 4);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
