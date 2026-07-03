package io.github.tinusj.swingmcp.server.config;

import io.github.tinusj.swingmcp.server.tools.ApplicationTools;
import io.github.tinusj.swingmcp.server.tools.ClipboardTools;
import io.github.tinusj.swingmcp.server.tools.DialogTools;
import io.github.tinusj.swingmcp.server.tools.InteractionTools;
import io.github.tinusj.swingmcp.server.tools.ScreenshotTools;
import io.github.tinusj.swingmcp.server.tools.SnapshotTools;
import io.github.tinusj.swingmcp.server.tools.UtilityTools;
import io.github.tinusj.swingmcp.server.tools.WindowTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all Swing MCP tool facades with the MCP server via a
 * {@link MethodToolCallbackProvider}.
 */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider swingToolCallbacks(
            ApplicationTools applicationTools,
            SnapshotTools snapshotTools,
            WindowTools windowTools,
            InteractionTools interactionTools,
            DialogTools dialogTools,
            ClipboardTools clipboardTools,
            ScreenshotTools screenshotTools,
            UtilityTools utilityTools) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(
                applicationTools,
                snapshotTools,
                windowTools,
                interactionTools,
                dialogTools,
                clipboardTools,
                screenshotTools,
                utilityTools)
            .build();
    }
}
