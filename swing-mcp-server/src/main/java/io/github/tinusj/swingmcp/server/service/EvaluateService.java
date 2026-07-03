package io.github.tinusj.swingmcp.server.service;

import io.github.tinusj.swingmcp.common.enums.CommandType;
import io.github.tinusj.swingmcp.server.config.SwingMcpProperties;
import io.github.tinusj.swingmcp.server.registry.SessionRegistry;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Evaluates Java snippets inside the target JVM via JShell.
 * Disabled by default; enable with {@code swing.mcp.evaluate.enabled=true}.
 */
@Service
public class EvaluateService {

    private final SessionRegistry registry;
    private final SwingMcpProperties properties;

    public EvaluateService(SessionRegistry registry, SwingMcpProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    /**
     * Evaluates a Java snippet in the target JVM.
     *
     * @throws IllegalStateException if evaluation is disabled by configuration
     */
    public Object evaluate(String code) {
        if (!properties.getEvaluate().isEnabled()) {
            throw new IllegalStateException(
                "evaluate_java is disabled. Enable it with swing.mcp.evaluate.enabled=true.");
        }
        return registry.require().send(CommandType.EVALUATE_JAVA, Map.of("code", code));
    }
}
