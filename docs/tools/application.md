# Application lifecycle tools

Tools for starting, attaching to, and stopping Swing application sessions.
Only one session is active at a time; `launch_app` or `attach_to_app` replaces
any existing session.

## `launch_app`

Launch a Swing application with the swing-mcp agent preloaded via `-javaagent`.
The agent binds a loopback-only socket in the configured port range and reports
it back through a response file.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `command` | string | yes | Full java command line, e.g. `java -jar /path/to/app.jar` |
| `workingDir` | string | no | Working directory for the launched process |

**Returns:** session info (mode, PID, agent port).

**Notes:**
- Requires `swing.mcp.agent-jar` (or the `SWING_MCP_AGENT_JAR` environment
  variable) to point at the swing-mcp-agent shaded jar.
- A launched application is terminated when the session is closed with
  `stop_app`.

## `attach_to_app`

Attach the agent to an already-running Swing JVM by PID using dynamic agent
loading.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `pid` | number | yes | Process id of the target Swing JVM |

**Returns:** session info (mode, PID, agent port).

**Notes:**
- The target JVM must allow dynamic attach.
- An attached application keeps running when the session is closed — only the
  connection is dropped.

## `stop_app`

Close the current session.

No parameters.

**Behaviour:**
- Launched application → the process is terminated.
- Attached application → the agent connection is closed; the target keeps
  running.

## `app_status`

Get the status of the current application session.

No parameters.

**Returns:** `connected`, `alive`, `mode` (`LAUNCHED`/`ATTACHED`), `pid`,
`agentPort`, `description`.
