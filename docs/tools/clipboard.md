# Clipboard tools

Tools for reading and writing the system clipboard of the target JVM,
enabling copy/paste test flows.

## `get_clipboard`

Read the system clipboard of the target JVM as text.

No parameters.

**Returns:** `text` — the clipboard contents, or `null` when the clipboard
holds no text.

## `set_clipboard`

Write text to the system clipboard of the target JVM.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `text` | string | yes | Text to place on the clipboard |
