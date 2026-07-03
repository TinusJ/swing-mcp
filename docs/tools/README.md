# Swing MCP — Tools Documentation

The Swing MCP server exposes automation tools over the MCP stdio transport.
Each tool call is translated into a JSON line command, forwarded over a
localhost-only socket to the agent inside the target JVM, and executed on the
Swing Event Dispatch Thread (EDT).

Component `uid` values (e.g. `comp-42`) come from `take_snapshot`. Take a fresh
snapshot after any action that changes the UI, as UIDs from an older snapshot
may be stale.

## Tool categories

| Category | Documentation | Tools |
|---|---|---|
| Application lifecycle | [application.md](application.md) | `launch_app`, `attach_to_app`, `stop_app`, `app_status` |
| Inspection | [inspection.md](inspection.md) | `take_snapshot`, `get_component_details` |
| Windows | [windows.md](windows.md) | `list_windows`, `select_window`, `resize_window`, `close_window` |
| Interaction | [interaction.md](interaction.md) | `click`, `fill`, `select_option`, `select_tree_node`, `select_table_cell`, `select_menu_item`, `press_key`, `drag`, `scroll` |
| Screenshots | [screenshots.md](screenshots.md) | `take_screenshot` |
| Utilities | [utilities.md](utilities.md) | `wait_for`, `evaluate_java` |

## Status overview

| Tool | Status |
|---|---|
| `launch_app` | ✅ Implemented |
| `attach_to_app` | ✅ Implemented |
| `stop_app` | ✅ Implemented |
| `app_status` | ✅ Implemented |
| `take_snapshot` | ✅ Implemented |
| `get_component_details` | ✅ Implemented |
| `list_windows` | ✅ Implemented |
| `select_window` | ✅ Implemented |
| `resize_window` | ✅ Implemented |
| `close_window` | ✅ Implemented |
| `click` | ✅ Implemented |
| `fill` | ✅ Implemented |
| `select_option` | ✅ Implemented |
| `select_tree_node` | ✅ Implemented |
| `select_table_cell` | ✅ Implemented |
| `select_menu_item` | ✅ Implemented |
| `press_key` | ✅ Implemented |
| `drag` | ✅ Implemented |
| `scroll` | ✅ Implemented |
| `take_screenshot` | ✅ Implemented |
| `wait_for` | ✅ Implemented |
| `evaluate_java` | ✅ Implemented (disabled by default) |

For tools and capabilities that are planned but **not yet implemented**, see the
[roadmap](roadmap.md).

## Typical workflow

1. `launch_app` (or `attach_to_app` for a running JVM) to start a session.
2. `take_snapshot` to discover component UIDs.
3. Interact: `click`, `fill`, `select_option`, `press_key`, …
4. `wait_for` to synchronise on UI state changes.
5. `take_screenshot` to visually verify results.
6. `stop_app` to end the session.

## Server configuration

Properties under the `swing.mcp` prefix (see the server's `application.yml`):

| Property | Default | Description |
|---|---|---|
| `agent-port-min` / `agent-port-max` | 40000 / 40100 | Port range the agent binds within |
| `agent-jar` | `${SWING_MCP_AGENT_JAR:}` | Path to the swing-mcp-agent shaded jar |
| `screenshot-dir` | `${java.io.tmpdir}/swing-mcp-screenshots` | Screenshot output directory |
| `tool-timeout-ms` | 30000 | Agent command round-trip timeout |
| `evaluate.enabled` | `false` | Allow `evaluate_java` |
