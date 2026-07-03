# Interaction tools

Tools for interacting with components in the active window. All UID parameters
come from `take_snapshot` (see [inspection.md](inspection.md)). Actions are
executed on the Swing Event Dispatch Thread.

## `click`

Click a component. `AbstractButton`s (buttons, checkboxes, radio buttons,
toggle buttons) are clicked directly via `doClick()`; other components via
mouse emulation.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID |
| `button` | string | no | `LEFT` (default), `RIGHT`, `MIDDLE` |
| `clickType` | string | no | `SINGLE` (default) or `DOUBLE` |

## `fill`

Set the text/value of a text component (`JTextField`, `JTextArea`, …),
`JSpinner`, or editable `JComboBox`.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID |
| `text` | string | yes | Text or value to enter |

## `select_option`

Select an option in a `JList`, `JComboBox`, or `JTabbedPane` by index or
visible text.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID |
| `index` | number | no | Zero-based option index |
| `text` | string | no | Visible text of the option |

**Notes:** provide either `index` or `text`.

## `select_tree_node`

Select a `JTree` node by path.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | JTree component UID |
| `path` | string | yes | Node path separated by ` > `, e.g. `Root > Folder > Leaf` |

## `select_table_cell`

Select a `JTable` cell.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | JTable component UID |
| `row` | number | yes | Zero-based row index |
| `col` | number | yes | Zero-based column index |

## `select_menu_item`

Click a menu item in the active window's menu bar.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `path` | string | yes | Menu path separated by ` > `, e.g. `File > Save` |

## `press_key`

Press a key or key chord using AWT `Robot`.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `keys` | string | yes | Key or chord such as `ENTER`, `TAB`, `CTRL+S` |

## `drag`

Drag from one component to another using mouse emulation.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `fromUid` | string | yes | Source component UID |
| `toUid` | string | yes | Target component UID |

## `scroll`

Scroll a component inside a `JScrollPane`.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID inside a scroll pane |
| `direction` | string | no | `UP`, `DOWN` (default), `LEFT`, `RIGHT` |
| `amount` | number | no | Scroll units (default 3) |
