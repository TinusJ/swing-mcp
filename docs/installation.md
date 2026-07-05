# Installing the Swing MCP server in your MCP client

This guide shows how to register the Swing MCP server with different MCP
clients (IntelliJ IDEA, VS Code, Claude Desktop, Claude Code, Cursor,
Windsurf, …).

## Prerequisites

1. **JDK 21+** on your `PATH` (the server and agent both require Java 21).
2. **Build the jars** (or download them from a release):

   ```bash
   mvn verify
   ```

   This produces:

   - `swing-mcp-server/target/swing-mcp-server-1.0.0.jar` — the MCP server (stdio transport)
   - `swing-mcp-agent/target/swing-mcp-agent-1.0.0.jar` — the Java agent injected into the target Swing JVM

In all examples below, replace `/path/to/…` with the absolute paths to these
two jars on your machine. On Windows, use paths like
`C:\\path\\to\\swing-mcp-server-1.0.0.jar` (escaped backslashes in JSON).

## Common configuration

Every client uses the same underlying command:

| Setting | Value |
|---|---|
| Command | `java` |
| Arguments | `-jar /path/to/swing-mcp-server-1.0.0.jar` |
| Environment | `SWING_MCP_AGENT_JAR=/path/to/swing-mcp-agent-1.0.0.jar` |

The `SWING_MCP_AGENT_JAR` environment variable tells the server where to find
the agent jar so it can preload it (`launch_app`) or attach it dynamically
(`attach_to_app`).

## IntelliJ IDEA (JetBrains AI Assistant / Junie)

In **Settings → Tools → AI Assistant → Model Context Protocol (MCP)** (or the
equivalent Junie settings page), add a new server. You can either fill in the
command/args fields via the UI or paste a JSON configuration ("As JSON"):

```json
{
  "servers": {
    "swing": {
      "command": "java",
      "args": ["-jar", "/path/to/swing-mcp-server-1.0.0.jar"],
      "env": {
        "SWING_MCP_AGENT_JAR": "/path/to/swing-mcp-agent-1.0.0.jar"
      }
    }
  }
}
```

Restart the AI Assistant chat session; the Swing tools (`launch_app`,
`take_snapshot`, `click`, …) should appear in the available tools list.

## VS Code (GitHub Copilot)

Add the server to your workspace's `.vscode/mcp.json` (or via
**Command Palette → MCP: Add Server**):

```json
{
  "servers": {
    "swing": {
      "type": "stdio",
      "command": "java",
      "args": ["-jar", "/path/to/swing-mcp-server-1.0.0.jar"],
      "env": {
        "SWING_MCP_AGENT_JAR": "/path/to/swing-mcp-agent-1.0.0.jar"
      }
    }
  }
}
```

To make the server available in all workspaces, add the same entry to your
user-level MCP configuration instead (**MCP: Open User Configuration**).

## Claude Desktop

Edit the Claude Desktop configuration file:

- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "swing": {
      "command": "java",
      "args": ["-jar", "/path/to/swing-mcp-server-1.0.0.jar"],
      "env": {
        "SWING_MCP_AGENT_JAR": "/path/to/swing-mcp-agent-1.0.0.jar"
      }
    }
  }
}
```

Restart Claude Desktop after saving.

## Claude Code (CLI)

```bash
claude mcp add swing \
  --env SWING_MCP_AGENT_JAR=/path/to/swing-mcp-agent-1.0.0.jar \
  -- java -jar /path/to/swing-mcp-server-1.0.0.jar
```

## Cursor

Add the server to `~/.cursor/mcp.json` (global) or `.cursor/mcp.json` in your
project (or via **Settings → MCP → Add new MCP server**):

```json
{
  "mcpServers": {
    "swing": {
      "command": "java",
      "args": ["-jar", "/path/to/swing-mcp-server-1.0.0.jar"],
      "env": {
        "SWING_MCP_AGENT_JAR": "/path/to/swing-mcp-agent-1.0.0.jar"
      }
    }
  }
}
```

## Windsurf

Add the same `mcpServers` entry as above to `~/.codeium/windsurf/mcp_config.json`.

## Configuration options

The server is a Spring Boot application, so any property can be overridden on
the command line with `-D` system properties placed **before** `-jar`, or via
environment variables. Common options:

| Property | Default | Description |
|---|---|---|
| `swing.mcp.agent-jar` | `$SWING_MCP_AGENT_JAR` | Path to the agent jar |
| `swing.mcp.agent-port-min` / `agent-port-max` | `40000` / `40100` | Loopback port range the agent binds to |
| `swing.mcp.screenshot-dir` | `${java.io.tmpdir}/swing-mcp-screenshots` | Where `take_screenshot` writes images |
| `swing.mcp.tool-timeout-ms` | `30000` | Per-tool command timeout |
| `swing.mcp.evaluate.enabled` | `false` | Enable the `evaluate_java` tool (arbitrary code execution — keep disabled unless needed) |

Example enabling `evaluate_java` and a custom screenshot directory:

```json
{
  "mcpServers": {
    "swing": {
      "command": "java",
      "args": [
        "-Dswing.mcp.evaluate.enabled=true",
        "-Dswing.mcp.screenshot-dir=/tmp/screens",
        "-jar", "/path/to/swing-mcp-server-1.0.0.jar"
      ],
      "env": {
        "SWING_MCP_AGENT_JAR": "/path/to/swing-mcp-agent-1.0.0.jar"
      }
    }
  }
}
```

## Verifying the setup

1. Ask your client to list its MCP tools — you should see `launch_app`,
   `take_snapshot`, `click`, etc.
2. Try the demo app:
   - `launch_app` with command `java -jar /path/to/swing-mcp-demo-1.0.0.jar`
   - `take_snapshot` to discover component UIDs
   - `click` / `fill` to interact

## Troubleshooting

- **Server doesn't start / no tools listed** — make sure `java` on the
  client's `PATH` is JDK 21+ (`java -version`). Some GUI clients don't inherit
  your shell `PATH`; use an absolute path to the `java` binary if needed.
- **`launch_app`/`attach_to_app` fails with a missing agent jar** — check that
  `SWING_MCP_AGENT_JAR` points to an existing file with an absolute path.
- **Logs** — the server logs to `${java.io.tmpdir}/swing-mcp-server.log`
  (e.g. `/tmp/swing-mcp-server.log`). Console logging is disabled because
  stdout is reserved for the MCP stdio transport.
