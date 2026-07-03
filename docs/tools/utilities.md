# Utility tools

## `wait_for`

Wait until a UI condition is met or a timeout elapses. Useful for
synchronising with asynchronous UI updates before taking the next action.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `conditionType` | string | yes | `WINDOW_TITLE`, `COMPONENT_TEXT`, `COMPONENT_VISIBLE`, or `COMPONENT_ENABLED` |
| `uid` | string | no | Component UID (required for the component conditions) |
| `expectedValue` | string | no | Expected value for the condition |
| `timeoutMs` | number | no | Timeout in milliseconds (default 5000) |

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
