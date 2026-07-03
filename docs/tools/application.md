# Application lifecycle tools

Tools for starting, attaching to, and stopping Swing application sessions.
Multiple named sessions can be open concurrently; one of them is the *active*
session that all other tools operate on. Use `list_sessions` and
`select_session` to switch between applications.

## `launch_app`

Launch a Swing application with the swing-mcp agent preloaded via `-javaagent`.
The agent binds a loopback-only socket in the configured port range and reports
it back through a response file.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `command` | string | yes | Full java command line, e.g. `java -jar /path/to/app.jar` |
| `workingDir` | string | no | Working directory for the launched process |
| `sessionId` | string | no | Session id; auto-generated when omitted. Reusing an id replaces (and closes) that session |

**Returns:** session info (session id, mode, PID, agent port). The new session
becomes the active one.

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
| `sessionId` | string | no | Session id; auto-generated when omitted. Reusing an id replaces (and closes) that session |

**Returns:** session info (session id, mode, PID, agent port). The new session
becomes the active one.

**Notes:**
- The target JVM must allow dynamic attach.
- An attached application keeps running when the session is closed — only the
  connection is dropped.

## `stop_app`

Close a session — the active one by default.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | string | no | Session id to close; the active session when omitted |

**Behaviour:**
- Launched application → the process is terminated.
- Attached application → the agent connection is closed; the target keeps
  running.
- When the active session is closed, another open session (if any) becomes
  active.

## `list_sessions`

List all open application sessions.

No parameters.

**Returns:** for each session: `sessionId`, `active`, `alive`, `mode`, `pid`,
`agentPort`, `description`.

## `select_session`

Make a session the active one that all other tools operate on.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | string | yes | Session id from `list_sessions` |

## `app_status`

Get the status of the active application session.

No parameters.

**Returns:** `connected`, `sessionId`, `sessionCount`, `alive`, `mode`
(`LAUNCHED`/`ATTACHED`), `pid`, `agentPort`, `description`.
