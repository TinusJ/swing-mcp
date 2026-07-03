package io.github.tinusj.swingmcp.server.tools;

import io.github.tinusj.swingmcp.server.service.InteractionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP tools for interacting with components of the target application.
 */
@Component
public class InteractionTools {

    private final InteractionService interactionService;

    public InteractionTools(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @Tool(name = "click", description = "Click a component by UID. Buttons are clicked directly; other components via mouse emulation.")
    public String click(
            @ToolParam(description = "Component UID from a snapshot") String uid,
            @ToolParam(description = "Mouse button: LEFT, RIGHT, or MIDDLE (default LEFT)", required = false) String button,
            @ToolParam(description = "Click type: SINGLE or DOUBLE (default SINGLE)", required = false) String clickType) {
        return ToolJson.toJson(interactionService.click(uid, button, clickType));
    }

    @Tool(name = "fill", description = "Set the text of a text field/area, spinner, or editable combo box by UID.")
    public String fill(
            @ToolParam(description = "Component UID from a snapshot") String uid,
            @ToolParam(description = "Text or value to enter") String text) {
        return ToolJson.toJson(interactionService.fill(uid, text));
    }

    @Tool(name = "select_option", description = "Select an option in a JList, JComboBox, or JTabbedPane by index or visible text.")
    public String selectOption(
            @ToolParam(description = "Component UID from a snapshot") String uid,
            @ToolParam(description = "Zero-based option index", required = false) Integer index,
            @ToolParam(description = "Visible text of the option to select", required = false) String text) {
        return ToolJson.toJson(interactionService.selectOption(uid, index, text));
    }

    @Tool(name = "select_tree_node", description = "Select a JTree node by path, e.g. \"Root > Folder > Leaf\".")
    public String selectTreeNode(
            @ToolParam(description = "JTree component UID") String uid,
            @ToolParam(description = "Node path separated by ' > '") String path) {
        return ToolJson.toJson(interactionService.selectTreeNode(uid, path));
    }

    @Tool(name = "select_table_cell", description = "Select a JTable cell by zero-based row and column.")
    public String selectTableCell(
            @ToolParam(description = "JTable component UID") String uid,
            @ToolParam(description = "Zero-based row index") int row,
            @ToolParam(description = "Zero-based column index") int col) {
        return ToolJson.toJson(interactionService.selectTableCell(uid, row, col));
    }

    @Tool(name = "select_menu_item", description = "Click a menu item by path in the active window, e.g. \"File > Save\".")
    public String selectMenuItem(
            @ToolParam(description = "Menu path separated by ' > '") String path) {
        return ToolJson.toJson(interactionService.selectMenuItem(path));
    }

    @Tool(name = "press_key", description = "Press a key or key chord in the target application, e.g. \"ENTER\" or \"CTRL+S\".")
    public String pressKey(
            @ToolParam(description = "Key or chord such as ENTER, TAB, CTRL+S") String keys) {
        return ToolJson.toJson(interactionService.pressKey(keys));
    }

    @Tool(name = "drag", description = "Drag from one component to another using mouse emulation.")
    public String drag(
            @ToolParam(description = "Source component UID") String fromUid,
            @ToolParam(description = "Target component UID") String toUid) {
        return ToolJson.toJson(interactionService.drag(fromUid, toUid));
    }

    @Tool(name = "scroll", description = "Scroll a scrollable component in a direction by a number of units.")
    public String scroll(
            @ToolParam(description = "Component UID inside a scroll pane") String uid,
            @ToolParam(description = "Direction: UP, DOWN, LEFT, or RIGHT (default DOWN)", required = false) String direction,
            @ToolParam(description = "Number of scroll units (default 3)", required = false) Integer amount) {
        return ToolJson.toJson(interactionService.scroll(uid, direction, amount));
    }
}
