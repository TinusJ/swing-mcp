# Inspection tools

Tools for discovering the component structure of the target application. All
interaction tools rely on component UIDs produced by `take_snapshot`.

## `take_snapshot`

Take a snapshot of the active window's Swing component tree. Every component
gets a stable UID (e.g. `comp-42`) that interaction tools use to address it.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `windowIndex` | number | no | Index of the window to snapshot (from `list_windows`); defaults to the active window |

**Returns:** a tree of components with, per node: UID, component class, name,
text/label, and key state flags.

**Notes:**
- Take a fresh snapshot after any action that changes the UI; UIDs from stale
  snapshots may no longer resolve.
- Snapshot filtering by component state (visible/enabled/focusable only) is
  planned but not yet exposed — see the [roadmap](roadmap.md).

## `get_component_details`

Get detailed information about a single component.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | yes | Component UID from a snapshot |

**Returns:** component class, text, bounds, enabled / visible / focusable
flags, selection state, and accessibility metadata.
