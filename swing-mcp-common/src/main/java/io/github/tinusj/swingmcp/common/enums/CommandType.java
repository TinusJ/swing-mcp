package io.github.tinusj.swingmcp.common.enums;

/**
 * Types of commands sent from the MCP server to the agent.
 */
public enum CommandType {
    TAKE_SNAPSHOT,
    GET_COMPONENT_DETAILS,
    TAKE_SCREENSHOT,
    LIST_WINDOWS,
    SELECT_WINDOW,
    RESIZE_WINDOW,
    CLOSE_WINDOW,
    CLICK,
    FILL,
    SELECT_OPTION,
    SELECT_TREE_NODE,
    SELECT_TABLE_CELL,
    SELECT_MENU_ITEM,
    PRESS_KEY,
    DRAG,
    SCROLL,
    WAIT_FOR,
    EVALUATE_JAVA,
    PING
}
