# Swing MCP — Tool Reference

> Detailed per-category documentation lives under [docs/tools](tools/README.md).
> This page is a single-page quick reference.

The Swing MCP server exposes the following tools over the MCP stdio transport.
Component `uid` values come from `take_snapshot`; take a fresh snapshot after any
action that changes the UI.

## Application lifecycle

### `launch_app`
Launch a Swing application with the swing-mcp agent preloaded via `-javaagent`.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `command` | string | yes | Full java command line, e.g. `java -jar /path/to/app.jar` |
| `workingDir` | string | no | Working directory for the launched process |
| `sessionId` | string | no | Session id; auto-generated when omitted |

Returns session info (session id, mode, PID, agent port). The new session
becomes the active one.

### `attach_to_app`
Attach the agent to an already-running Swing JVM by PID. The target keeps
running when the session is closed.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `pid` | number | yes | Process id of the target Swing JVM |
| `sessionId` | string | no | Session id; auto-generated when omitted |

### `stop_app`
Close a session (the active one by default). A launched application is
terminated; an attached application is only disconnected.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | string | no | Session id to close; the active session when omitted |

### `list_sessions`
List all open sessions with `sessionId`, `active`, `alive`, `mode`, `pid`,
`agentPort`, and `description`.

### `select_session`
Make a session the active one that other tools operate on.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | string | yes | Session id from `list_sessions` |

### `app_status`
Get the status of the active session: `connected`, `sessionId`,
`sessionCount`, `alive`, `mode`, `pid`, `agentPort`, `description`.

## Inspection

### `take_snapshot`
Take a snapshot of the active window's Swing component tree. Every component
gets a stable UID (e.g. `comp-42`) used by the interaction tools.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `windowIndex` | number | no | Window index from `list_windows` |
| `filter` | string | no | `ALL` (default), `VISIBLE_ONLY`, `ENABLED_ONLY`, `FOCUSABLE_ONLY` |

### `get_component_details`
Detailed information about a single component: class, text, bounds, enabled /
visible / focusable flags, selection state, and accessibility metadata.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID from a snapshot |

### `find_component`
Find components by text, name, tooltip, or class without a full snapshot.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `query` | string | yes | Search string (case-insensitive substring match) |
| `by` | string | no | `TEXT`, `NAME`, `TOOLTIP`, `CLASS`, or `ANY` (default) |

### `get_table_data`
Extract the model contents of a `JTable`: column names and row values.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | JTable component UID |
| `startRow` | number | no | Zero-based first row (inclusive) |
| `endRow` | number | no | Zero-based last row (inclusive) |

### `get_list_items`
Extract the items of a `JList` or visible rows of a `JTree`.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | JList or JTree component UID |
| `startIndex` | number | no | Zero-based first item (inclusive) |
| `endIndex` | number | no | Zero-based last item (inclusive) |

## Windows

### `list_windows`
List all visible windows with index, title, class, bounds, and focus state.

### `select_window`
Select the active window by index and bring it to front.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `index` | number | yes | Window index from `list_windows` |

### `resize_window`
Resize the active window.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `width` | number | no | New width in pixels |
| `height` | number | no | New height in pixels |

### `move_window`
Move the active window to a screen position.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `x` | number | no | New x position in pixels |
| `y` | number | no | New y position in pixels |

### `maximize_window` / `minimize_window` / `restore_window`
Change the extended state of the active frame window. No parameters.

### `close_window`
Close the active window by dispatching a window-closing event.

## Interaction

### `click`
Click a component. `AbstractButton`s are clicked directly (`doClick`); other
components via mouse emulation.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID |
| `button` | string | no | `LEFT` (default), `RIGHT`, `MIDDLE` |
| `clickType` | string | no | `SINGLE` (default) or `DOUBLE` |

### `hover`
Move the mouse over a component to trigger hover effects and tooltips.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID |

### `focus`
Give keyboard focus to a component.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID |

### `type_text`
Type text character-by-character using key events (unlike `fill`).

| Parameter | Type | Required | Description |
|---|---|---|---|
| `text` | string | yes | Text to type |
| `uid` | string | no | Component UID to focus before typing |

### `fill`
Set the text/value of a text component, `JSpinner`, or editable `JComboBox`.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID |
| `text` | string | yes | Text or value to enter |

### `select_option`
Select an option in a `JList`, `JComboBox`, or `JTabbedPane`.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID |
| `index` | number | no | Zero-based option index |
| `text` | string | no | Visible text of the option |

### `select_tree_node`
Select a `JTree` node by path.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | JTree component UID |
| `path` | string | yes | Node path, e.g. `Root > Folder > Leaf` |

### `select_table_cell`
Select a `JTable` cell.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | JTable component UID |
| `row` | number | yes | Zero-based row |
| `col` | number | yes | Zero-based column |

### `select_menu_item`
Click a menu item in the active window's menu bar.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `path` | string | yes | Menu path, e.g. `File > Save` |

### `select_context_menu_item`
Open a component's context menu and click an item by path.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID |
| `path` | string | yes | Menu item path, e.g. `Copy` or `Refactor > Rename` |

### `press_key`
Press a key or key chord using AWT `Robot`.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `keys` | string | yes | e.g. `ENTER`, `TAB`, `CTRL+S` |

### `drag`
Drag from one component to another using mouse emulation.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `fromUid` | string | yes | Source component UID |
| `toUid` | string | yes | Target component UID |

### `scroll`
Scroll a component inside a `JScrollPane`.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID |
| `direction` | string | no | `UP`, `DOWN` (default), `LEFT`, `RIGHT` |
| `amount` | number | no | Scroll units (default 3) |

## Dialogs

### `list_dialogs`
List open dialogs with window index, type (`option`, `fileChooser`, `custom`),
title, message, modality, and button texts. No parameters.

### `handle_dialog`
Respond to an open dialog by clicking a button or selecting a file.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `button` | string | no | Text of the dialog button to click |
| `filePath` | string | no | File path to select in a `JFileChooser` |
| `windowIndex` | number | no | Window index of the dialog (from `list_dialogs`) |

## Clipboard

### `get_clipboard`
Read the target JVM's system clipboard as text. No parameters.

### `set_clipboard`
Write text to the target JVM's system clipboard.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `text` | string | yes | Text to place on the clipboard |

## Screenshots

### `take_screenshot`
Capture a PNG of the active window or a single component. The image is saved
under `swing.mcp.screenshot-dir` and the file path is returned.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | no | Component UID; omit for the whole window |
| `returnImage` | boolean | no | When `true`, include the base64-encoded PNG data inline |

## Utilities

### `wait_for`
Wait until a UI condition is met or a timeout elapses.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `conditionType` | string | yes | `WINDOW_TITLE`, `COMPONENT_TEXT`, `COMPONENT_VISIBLE`, `COMPONENT_ENABLED`, `COMPONENT_EXISTS`, `COMPONENT_GONE`, `WINDOW_COUNT`, `EDT_IDLE` |
| `uid` | string | no | Component UID (required for `COMPONENT_TEXT`/`VISIBLE`/`ENABLED`) |
| `expectedValue` | string | no | Expected value, search query, or window count |
| `timeoutMs` | number | no | Timeout in ms (default 5000) |

### `evaluate_java`
Evaluate a Java snippet inside the target JVM via JShell.
**Disabled by default**; enable with `swing.mcp.evaluate.enabled=true`.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `code` | string | yes | Java code to evaluate |

## Server configuration

Properties under the `swing.mcp` prefix (see `application.yml`):

| Property | Default | Description |
|---|---|---|
| `agent-port-min` / `agent-port-max` | 40000 / 40100 | Port range the agent binds within |
| `agent-jar` | `${SWING_MCP_AGENT_JAR:}` | Path to the swing-mcp-agent shaded jar |
| `screenshot-dir` | `${java.io.tmpdir}/swing-mcp-screenshots` | Screenshot output directory |
| `tool-timeout-ms` | 30000 | Agent command round-trip timeout |
| `evaluate.enabled` | `false` | Allow `evaluate_java` |
