# Window tools

Tools for enumerating and managing the target application's windows. Most
other tools operate on the *active* window, which is chosen with
`select_window`.

## `list_windows`

List all visible windows in the target JVM.

No parameters.

**Returns:** for each window: index, title, class, bounds, and focus state.

## `select_window`

Select the active window by index and bring it to front.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `index` | number | yes | Window index from `list_windows` |

## `resize_window`

Resize the active window.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `width` | number | no | New width in pixels |
| `height` | number | no | New height in pixels |

**Notes:** omitted dimensions keep their current value.

## `move_window`

Move the active window to a screen position.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `x` | number | no | New x position in pixels |
| `y` | number | no | New y position in pixels |

**Notes:** omitted coordinates keep their current value.

## `maximize_window` / `minimize_window` / `restore_window`

Change the extended state of the active frame window: maximize it, minimize
(iconify) it, or restore it to its normal state.

No parameters.

**Notes:** the active window must be a `Frame` (e.g. a `JFrame`); dialogs do
not support extended states.

## `close_window`

Close the active window by dispatching a window-closing event (equivalent to
the user clicking the window's close button).

No parameters.

**Notes:** the application's own window-closing behaviour applies — e.g. a
confirmation dialog may appear, or the whole application may exit.
