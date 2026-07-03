# Screenshot tools

## `take_screenshot`

Capture a PNG of the active window or a single component.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | no | Component UID; omit to capture the whole active window |

**Returns:** the path of the saved PNG file.

**Notes:**
- Images are saved under `swing.mcp.screenshot-dir`
  (default `${java.io.tmpdir}/swing-mcp-screenshots`).
- Returning image data inline as MCP image content is planned — see the
  [roadmap](roadmap.md).
