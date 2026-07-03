---
name: swing-mcp
description: Uses Swing MCP for efficient debugging, troubleshooting and automation of Java Swing applications. Use when inspecting Swing UIs, automating UI interactions, extracting data from tables/lists/trees, or verifying Swing application behavior.
---

## Core Concepts

**Application lifecycle**: Tools operate on a session that connects the MCP server to the swing-mcp agent inside the target JVM. Start a session with `launch_app` (starts a new JVM with the agent preloaded via `-javaagent`) or `attach_to_app` (loads the agent into an already-running Swing JVM by PID). End it with `stop_app` — a launched application is terminated, an attached one is only disconnected.

**Session selection**: Tools operate on the currently active session. Use `list_sessions` to see open sessions, then `select_session` to switch. Check connectivity with `app_status`.

**Window selection**: Inspection and interaction tools operate on the active window. Use `list_windows` to see visible windows, then `select_window` to switch context (this also brings the window to front).

**Component interaction**: Use `take_snapshot` to get the Swing component tree with component `uid`s (e.g. `comp-42`). Each component has a stable UID used by interaction tools such as `click` and `fill`. If a component isn't found, take a fresh snapshot — UIDs from an older snapshot may be stale after the UI changed.

**Event Dispatch Thread**: Every command is executed on the Swing EDT inside the target JVM, so interactions behave like real user actions and are safe with respect to Swing's threading rules.

## Workflow Patterns

### Before interacting with an application

1. Connect: `launch_app` or `attach_to_app`
2. Snapshot: `take_snapshot` to understand the component tree and collect UIDs
3. Interact: Use component `uid`s from the snapshot for `click`, `fill`, `select_option`, etc.
4. Wait: `wait_for` to synchronise on UI state changes (`COMPONENT_TEXT`, `COMPONENT_VISIBLE`, `WINDOW_TITLE`, `EDT_IDLE`, …) instead of sleeping
5. Re-snapshot: take a fresh `take_snapshot` after any action that changes the UI

### Efficient data retrieval

- Use `find_component` (by `TEXT`, `NAME`, `TOOLTIP`, `CLASS`, or `ANY`) to locate a component without paying for a full snapshot
- Use `take_snapshot` with `filter` (`VISIBLE_ONLY`, `ENABLED_ONLY`, `FOCUSABLE_ONLY`) to reduce output size
- Use `get_table_data` / `get_list_items` with `startRow`/`endRow` (or `startIndex`/`endIndex`) ranges instead of dumping entire models
- Use `get_component_details` for the full state of a single component instead of re-snapshotting the whole tree

### Tool selection

- **Automation/inspection**: `take_snapshot` (text-based component tree, faster, better for automation)
- **Visual inspection**: `take_screenshot` (whole window, or a single component via `uid`); the PNG is saved to disk and the path returned — only set `returnImage: true` when the image must be inline
- **Structured data**: `get_table_data` for `JTable`, `get_list_items` for `JList`/`JTree`
- **Text entry**: `fill` sets a value directly (text components, `JSpinner`, editable `JComboBox`); `type_text` types character-by-character with key events when listeners/validation must fire
- **Selection**: `select_option` for `JList`/`JComboBox`/`JTabbedPane`, `select_tree_node` for `JTree` paths, `select_table_cell` for `JTable`, `select_menu_item` / `select_context_menu_item` for menus
- **Additional details**: `evaluate_java` (JShell in the target JVM) for state not exposed by the other tools — disabled by default, requires `swing.mcp.evaluate.enabled=true`

### Handling dialogs

Modal dialogs block interaction with the active window. Use `list_dialogs` to see open dialogs (type, title, message, buttons), then `handle_dialog` to click a button by text or pick a file in a `JFileChooser`.

### Parallel execution

You can send multiple tool calls in parallel, but maintain correct order: connect → snapshot → interact → wait.

## Troubleshooting

If tool calls fail or the session won't connect, use the [troubleshooting skill](../troubleshooting/SKILL.md) and refer to the documentation:

- [Tool reference](../../docs/tools/README.md)
- [Installation and troubleshooting guide](../../docs/installation.md)
