---
name: swing-ui-testing
description: Uses Swing MCP to test and verify Java Swing application behavior. Use when writing or executing UI test scenarios against Swing apps - filling forms, verifying tables and trees, exercising menus and dialogs, testing keyboard navigation, and visually verifying results.
---

## Core Concepts

**Deterministic synchronisation**: Never rely on sleeps. Use `wait_for` with an explicit condition (`COMPONENT_TEXT`, `COMPONENT_VISIBLE`, `COMPONENT_ENABLED`, `COMPONENT_EXISTS`, `COMPONENT_GONE`, `WINDOW_TITLE`, `WINDOW_COUNT`, `EDT_IDLE`) and a `timeoutMs` to make test steps deterministic.

**Fresh snapshots**: Component UIDs come from `take_snapshot` and can go stale after any action that changes the UI. Re-snapshot before asserting on state, and whenever an interaction fails to find a component.

**Assert via the model, not pixels**: Prefer `get_component_details`, `get_table_data`, and `get_list_items` for assertions — they read the actual Swing model state. Use `take_screenshot` only for visual verification.

## Workflow Patterns

### 1. Test session setup and teardown

1. `launch_app` with the application's full java command (use `attach_to_app` only when the app must keep running after the test).
2. `wait_for` with `WINDOW_TITLE` (or `WINDOW_COUNT`) to ensure the main window is up before interacting.
3. Run the scenario steps.
4. `stop_app` to end the session, even on failure, so the launched JVM is cleaned up.

### 2. Form filling and validation

1. `take_snapshot` (optionally `filter: "ENABLED_ONLY"`) to find input fields and buttons.
2. `fill` each text field, spinner, or editable combo box; use `type_text` when the form relies on key listeners or per-keystroke validation.
3. `select_option` for combo boxes and `click` for checkboxes/radio buttons.
4. Submit with `click` on the confirm button, or `press_key` with `ENTER`.
5. Verify: `wait_for` a success condition, then check resulting state with `get_component_details` (e.g. an error label's text, or a button becoming enabled/disabled).

### 3. Tables, lists, and trees

- **Verify data**: `get_table_data` with `startRow`/`endRow` to assert column names and cell values; `get_list_items` for `JList` items or visible `JTree` rows.
- **Select**: `select_table_cell` (row/col), `select_option` (index or visible text), `select_tree_node` with a `Root > Folder > Leaf` path.
- **Row actions**: after selecting, use `click` with `clickType: "DOUBLE"` to open, or `select_context_menu_item` for the row's context menu.
- **Long content**: `scroll` inside the `JScrollPane` to reach off-screen rows before interacting via mouse emulation.

### 4. Menus and dialogs

1. `select_menu_item` with a path like `File > Save` to trigger menu actions.
2. `list_dialogs` to detect and inspect the resulting dialog (type, title, message, buttons).
3. `handle_dialog` to click a button by text (e.g. `OK`, `Cancel`) or to pick a file in a `JFileChooser` via `filePath`.
4. `wait_for` with `WINDOW_COUNT` or `COMPONENT_GONE` to confirm the dialog closed.

### 5. Keyboard navigation and focus

1. `focus` a starting component, then `press_key` with `TAB` / `SHIFT+TAB` to walk the focus traversal order.
2. `take_snapshot` (or `get_component_details`) to verify which component holds focus after each step.
3. Test accelerators and shortcuts with key chords, e.g. `press_key` with `CTRL+S`.
4. For modal dialogs, verify focus moves into the dialog and stays there until it is closed.

### 6. Drag and drop, and clipboard

- `drag` from a source UID to a target UID to test drag-and-drop behavior, then verify the result via snapshots or model data.
- Use `set_clipboard` + `press_key` with `CTRL+V` to test paste handling, and `get_clipboard` to assert what the app copied after `CTRL+C`.

### 7. Multi-window scenarios

1. `list_windows` after actions that may open new frames.
2. `select_window` by index to switch the active window before snapshotting or interacting.
3. Use window tools (`resize_window`, `maximize_window`, `close_window`, …) to test layout and shutdown behavior.

### 8. Visual verification

- `take_screenshot` of the whole window at key checkpoints, or of a single component via `uid` for focused comparisons.
- Screenshots are saved under the server's `screenshot-dir` and the path is returned; reference the file rather than inlining the image unless needed.

## Troubleshooting

- **Component not found / stale UID**: take a fresh `take_snapshot`; the UI likely changed.
- **Interaction has no effect**: check `get_component_details` — the component may be disabled or not visible; `wait_for` with `COMPONENT_ENABLED` before retrying.
- **Test hangs on a step**: a modal dialog is probably blocking the window — call `list_dialogs` and handle it.
- For connection/session failures, use the [troubleshooting skill](../troubleshooting/SKILL.md).
