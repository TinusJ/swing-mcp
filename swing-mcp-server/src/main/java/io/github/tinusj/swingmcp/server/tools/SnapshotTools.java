package io.github.tinusj.swingmcp.server.tools;

import io.github.tinusj.swingmcp.server.service.SnapshotService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP tools for inspecting the target application's component tree.
 */
@Component
public class SnapshotTools {

    private final SnapshotService snapshotService;

    public SnapshotTools(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Tool(name = "take_snapshot", description = """
        Take a snapshot of the active window's Swing component tree. \
        Each component gets a stable UID (e.g. "comp-42") used by interaction tools. \
        Always take a fresh snapshot after actions that change the UI.""")
    public String takeSnapshot(
            @ToolParam(description = "Optional index of the window to snapshot (from list_windows)", required = false) Integer windowIndex) {
        return ToolJson.toJson(snapshotService.takeSnapshot(windowIndex));
    }

    @Tool(name = "get_component_details", description = """
        Get detailed information about a single component by UID, including \
        accessibility metadata, bounds, text, and selection state.""")
    public String getComponentDetails(
            @ToolParam(description = "Component UID from a snapshot") String uid) {
        return ToolJson.toJson(snapshotService.componentDetails(uid));
    }
}
