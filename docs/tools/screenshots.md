# Screenshot tools

## `take_screenshot`

Capture a PNG of the active window or a single component.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uid` | string | no | Component UID; omit to capture the whole active window |
| `returnImage` | boolean | no | When `true`, include the base64-encoded PNG data in the response |

**Returns:** the path of the saved PNG file, plus the base64-encoded image
data when `returnImage=true`.

**Notes:**
- Images are saved under `swing.mcp.screenshot-dir`
  (default `${java.io.tmpdir}/swing-mcp-screenshots`).
- Use `returnImage=true` when the MCP client has no access to the server's
  local filesystem.
