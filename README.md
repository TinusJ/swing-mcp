# swing-mcp

An MCP (Model Context Protocol) server for interacting with Java Swing applications — inspired by [chrome-devtools-mcp](https://github.com/ChromeDevTools/chrome-devtools-mcp), but targeting any Swing UI instead of HTML pages.

## Modules

- `swing-mcp-server` — Spring Boot MCP server (stdio transport) exposing Swing automation tools.
- `swing-mcp-agent` — Java agent for attaching to an already-running Swing JVM by PID.
- `swing-mcp-demo` — Demo Swing application used for integration testing.

## Status

Under initial development.
