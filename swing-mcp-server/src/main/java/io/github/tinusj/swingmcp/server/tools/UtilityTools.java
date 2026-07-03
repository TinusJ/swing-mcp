package io.github.tinusj.swingmcp.server.tools;

import io.github.tinusj.swingmcp.server.service.EvaluateService;
import io.github.tinusj.swingmcp.server.service.WaitService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP utility tools: waiting for UI conditions and (optionally) evaluating
 * Java snippets in the target JVM.
 */
@Component
public class UtilityTools {

    private final WaitService waitService;
    private final EvaluateService evaluateService;

    public UtilityTools(WaitService waitService, EvaluateService evaluateService) {
        this.waitService = waitService;
        this.evaluateService = evaluateService;
    }

    @Tool(name = "wait_for", description = """
        Wait until a UI condition is met or a timeout elapses. Condition types: \
        WINDOW_TITLE, COMPONENT_TEXT, COMPONENT_VISIBLE, COMPONENT_ENABLED.""")
    public String waitFor(
            @ToolParam(description = "Condition type: WINDOW_TITLE, COMPONENT_TEXT, COMPONENT_VISIBLE, or COMPONENT_ENABLED") String conditionType,
            @ToolParam(description = "Component UID (required for component conditions)", required = false) String uid,
            @ToolParam(description = "Expected value for the condition", required = false) String expectedValue,
            @ToolParam(description = "Timeout in milliseconds (default 5000)", required = false) Long timeoutMs) {
        return ToolJson.toJson(waitService.waitFor(conditionType, uid, expectedValue, timeoutMs));
    }

    @Tool(name = "evaluate_java", description = """
        Evaluate a Java snippet inside the target JVM via JShell. \
        Disabled unless swing.mcp.evaluate.enabled=true on the server.""")
    public String evaluateJava(
            @ToolParam(description = "Java code to evaluate") String code) {
        return ToolJson.toJson(evaluateService.evaluate(code));
    }
}
