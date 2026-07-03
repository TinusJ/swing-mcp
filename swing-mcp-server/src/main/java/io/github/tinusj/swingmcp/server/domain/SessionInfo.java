package io.github.tinusj.swingmcp.server.domain;

import io.github.tinusj.swingmcp.common.enums.AttachMode;

/**
 * Summary of the currently active application session.
 */
public record SessionInfo(
    AttachMode mode,
    long pid,
    int agentPort,
    String description
) {}
