# Tools roadmap — not yet implemented

This page tracks tools and capabilities that are planned or would be natural
additions, but are **not implemented yet**. It is inspired by the feature set
of [chrome-devtools-mcp](https://github.com/ChromeDevTools/chrome-devtools-mcp)
and by gaps observed while automating real Swing applications.

Contributions are welcome — each item lists the intended behaviour so it can be
picked up independently.

## Inspection

### Snapshot state filtering
`take_snapshot` should accept a `filter` parameter to reduce snapshot size for
large UIs. The shared `ComponentStateFilter` enum (`ALL`, `VISIBLE_ONLY`,
`ENABLED_ONLY`, `FOCUSABLE_ONLY`) already exists in `swing-mcp-common` but is
not yet wired into the tool, the command protocol, or the agent.

### `find_component`
Search for components by text, name, tooltip, or class without taking a full
snapshot — returning matching UIDs directly. Useful for very large component
trees where a full snapshot is expensive or noisy.

### `get_table_data` / `get_list_items`
Extract the model contents of a `JTable`, `JList`, or `JTree` (rows/items,
optionally a range) as structured data, rather than parsing them out of a
snapshot.

## Interaction

### `hover`
Move the mouse over a component to trigger hover effects and tooltips
(mouse-emulation move without a click).

### `focus`
Give keyboard focus to a specific component by UID, so that subsequent
`press_key` calls target it deterministically.

### `type_text`
Type text character-by-character into the focused component using key events
(as opposed to `fill`, which sets the value directly). Needed for UIs with
per-keystroke listeners, input masks, or autocompletion.

### Right-click context menu selection
`click` already supports `button=RIGHT`, but there is no first-class way to
select an item from the resulting `JPopupMenu`. A `select_context_menu_item`
tool (open the context menu on a component, then click an item by path) would
cover this.

## Dialogs

### `list_dialogs` / `handle_dialog`
Modal dialogs (`JOptionPane`, file choosers, custom `JDialog`s) block the EDT
flow of many applications. Planned support:
- list currently open dialogs with their type, title, and message;
- respond to a dialog (e.g. click `OK` / `Cancel` / a named button, or
  set a file path in a `JFileChooser`).

Today dialogs can only be handled generically via `take_snapshot` +
`click` on the dialog window.

## Windows

### `move_window`
Move the active window to a given screen position (complementing
`resize_window`).

### `maximize_window` / `minimize_window` / `restore_window`
Change the extended state of the active `JFrame`.

## Clipboard

### `get_clipboard` / `set_clipboard`
Read and write the system clipboard of the target JVM, enabling copy/paste
test flows.

## Utilities

### Additional `wait_for` conditions
- `COMPONENT_EXISTS` / `COMPONENT_GONE` — wait for a component matching a
  text/name query to appear or disappear;
- `WINDOW_COUNT` — wait for a window (e.g. a dialog) to open or close;
- an "EDT idle" condition that waits until the event queue has drained.

## Screenshots

### Inline image content
`take_screenshot` currently saves a PNG and returns its file path. It should
optionally return the image as MCP image content so clients without local
filesystem access can view it.

## Sessions

### Multiple concurrent sessions
The server currently manages a single session; `launch_app`/`attach_to_app`
replace the previous one. Supporting multiple named sessions (and a
`sessionId` parameter on tools) would allow automating interactions between
applications.
