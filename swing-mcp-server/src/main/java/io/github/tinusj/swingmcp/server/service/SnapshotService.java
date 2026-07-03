package io.github.tinusj.swingmcp.server.service;

import io.github.tinusj.swingmcp.common.enums.CommandType;
import io.github.tinusj.swingmcp.server.registry.SessionRegistry;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Retrieves component tree snapshots and component details from the agent.
 */
@Service
public class SnapshotService {

    private final SessionRegistry registry;

    public SnapshotService(SessionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Takes a snapshot of the active window's component tree.
     *
     * @param windowIndex optional window index; null for the active window
     */
    public Object takeSnapshot(Integer windowIndex) {
        Map<String, Object> params = new HashMap<>();
        if (windowIndex != null) {
            params.put("windowIndex", windowIndex);
        }
        return registry.require().send(CommandType.TAKE_SNAPSHOT, params);
    }

    /**
     * Returns detailed information about a component identified by UID.
     */
    public Object componentDetails(String uid) {
        return registry.require().send(CommandType.GET_COMPONENT_DETAILS, Map.of("uid", uid));
    }
}
