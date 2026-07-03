package io.github.tinusj.swingmcp.server.tools;

import io.github.tinusj.swingmcp.server.service.ApplicationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP tools for managing the target application lifecycle.
 */
@Component
public class ApplicationTools {

    private final ApplicationService applicationService;

    public ApplicationTools(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Tool(name = "launch_app", description = """
        Launch a Swing application with the swing-mcp agent preloaded. \
        Provide the full java command line, e.g. "java -jar /path/to/app.jar". \
        Returns session info including the target PID.""")
    public String launchApp(
            @ToolParam(description = "Full java command line to launch the application") String command,
            @ToolParam(description = "Optional working directory for the launched process", required = false) String workingDir) {
        return ToolJson.toJson(applicationService.launch(command, workingDir));
    }

    @Tool(name = "attach_to_app", description = """
        Attach the swing-mcp agent to an already-running Swing JVM by process id (PID). \
        The target keeps running when the session is closed.""")
    public String attachToApp(
            @ToolParam(description = "Process id of the target Swing JVM") long pid) {
        return ToolJson.toJson(applicationService.attach(pid));
    }

    @Tool(name = "stop_app", description = """
        Close the current session. A launched application is terminated; \
        an attached application is only disconnected and keeps running.""")
    public String stopApp() {
        return applicationService.stop();
    }

    @Tool(name = "app_status", description = "Get the status of the current application session (mode, PID, liveness).")
    public String appStatus() {
        return ToolJson.toJson(applicationService.status());
    }
}
