# Utility tools

## `wait_for`

Wait until a UI condition is met or a timeout elapses. Useful for
synchronising with asynchronous UI updates before taking the next action.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `conditionType` | string | yes | See condition types below |
| `uid` | string | no | Component UID (required for `COMPONENT_TEXT`, `COMPONENT_VISIBLE`, `COMPONENT_ENABLED`) |
| `expectedValue` | string | no | Expected value for the condition |
| `timeoutMs` | number | no | Timeout in milliseconds (default 5000) |

**Condition types:**

| Condition | Description |
|---|---|
| `WINDOW_TITLE` | A visible window's title contains `expectedValue` |
| `COMPONENT_TEXT` | The component's text contains `expectedValue` |
| `COMPONENT_VISIBLE` | The component is visible |
| `COMPONENT_ENABLED` | The component is enabled |
| `COMPONENT_EXISTS` | A component matching the `expectedValue` text/name query exists |
| `COMPONENT_GONE` | No component matches the `expectedValue` text/name query |
| `WINDOW_COUNT` | The number of visible windows equals `expectedValue` |
| `EDT_IDLE` | The event dispatch queue has drained |

## `evaluate_java`

Evaluate a Java snippet inside the target JVM via JShell. This allows
arbitrary code execution in the target process and is therefore
**disabled by default**.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `code` | string | yes | Java code to evaluate |

**Enabling:** set `swing.mcp.evaluate.enabled=true` on the server.

**Security:** only enable this in trusted environments; the snippet runs with
the full privileges of the target JVM.
