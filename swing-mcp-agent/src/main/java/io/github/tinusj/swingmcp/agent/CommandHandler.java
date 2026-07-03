package io.github.tinusj.swingmcp.agent;

import io.github.tinusj.swingmcp.common.command.CommandRequest;
import io.github.tinusj.swingmcp.common.command.CommandResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Handles incoming JSON line commands from the MCP server and dispatches them
 * to the appropriate Swing component operations on the EDT.
 */
public class CommandHandler {

    private static final Logger LOG = Logger.getLogger(CommandHandler.class.getName());

    private final JsonCodec codec;
    private final ComponentScanner scanner;

    public CommandHandler(JsonCodec codec) {
        this.codec = codec;
        this.scanner = new ComponentScanner();
    }

    /**
     * Processes a single JSON command line and returns a JSON response line.
     *
     * @param jsonLine the incoming JSON command
     * @return a JSON response string
     */
    public String handle(String jsonLine) {
        try {
            CommandRequest request = codec.decodeRequest(jsonLine);
            Object result = dispatch(request);
            CommandResponse response = new CommandResponse(request.requestId(), true, result, null);
            return codec.encode(response);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error handling command: " + jsonLine, e);
            try {
                CommandRequest req = codec.decodeRequest(jsonLine);
                CommandResponse error = new CommandResponse(req.requestId(), false, null, e.getMessage());
                return codec.encode(error);
            } catch (Exception inner) {
                return "{\"success\":false,\"error\":\"Failed to parse request\"}";
            }
        }
    }

    private Object dispatch(CommandRequest request) throws Exception {
        return switch (request.type()) {
            case PING -> "pong";
            case TAKE_SNAPSHOT -> invokeOnEdt(() -> scanner.takeSnapshot(request.params()));
            case LIST_WINDOWS -> invokeOnEdt(scanner::listWindows);
            case SELECT_WINDOW -> invokeOnEdt(() -> scanner.selectWindow(request.params()));
            case RESIZE_WINDOW -> invokeOnEdt(() -> scanner.resizeWindow(request.params()));
            case CLOSE_WINDOW -> invokeOnEdt(() -> scanner.closeWindow(request.params()));
            case GET_COMPONENT_DETAILS -> invokeOnEdt(() -> scanner.getComponentDetails(request.params()));
            case TAKE_SCREENSHOT -> scanner.takeScreenshot(request.params());
            case CLICK -> invokeOnEdt(() -> scanner.click(request.params()));
            case FILL -> invokeOnEdt(() -> scanner.fill(request.params()));
            case SELECT_OPTION -> invokeOnEdt(() -> scanner.selectOption(request.params()));
            case SELECT_TREE_NODE -> invokeOnEdt(() -> scanner.selectTreeNode(request.params()));
            case SELECT_TABLE_CELL -> invokeOnEdt(() -> scanner.selectTableCell(request.params()));
            case SELECT_MENU_ITEM -> invokeOnEdt(() -> scanner.selectMenuItem(request.params()));
            case PRESS_KEY -> scanner.pressKey(request.params());
            case DRAG -> scanner.drag(request.params());
            case SCROLL -> invokeOnEdt(() -> scanner.scroll(request.params()));
            case WAIT_FOR -> scanner.waitFor(request.params());
            case EVALUATE_JAVA -> scanner.evaluateJava(request.params());
        };
    }

    @FunctionalInterface
    private interface EdtCallable<T> {
        T call() throws Exception;
    }

    private <T> T invokeOnEdt(EdtCallable<T> callable) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return callable.call();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            try {
                result.set(callable.call());
            } catch (Exception e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }
}
