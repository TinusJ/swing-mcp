# Publishing to the MCP Registry

This project is published to the [MCP Registry](https://registry.modelcontextprotocol.io) as
**`io.github.tinusj/swing-mcp`**, following the
[registry quickstart](https://github.com/modelcontextprotocol/registry/blob/main/docs/modelcontextprotocol-io/quickstart.mdx).

Because this is a Java/Maven project (the registry does not host Maven Central packages), the
server is distributed as an **[MCPB](https://github.com/anthropics/mcpb) bundle** attached to
GitHub releases — see the
[supported package types](https://github.com/modelcontextprotocol/registry/blob/main/docs/modelcontextprotocol-io/package-types.mdx).

## Files involved

| File | Purpose |
| --- | --- |
| [`server.json`](../server.json) | MCP Registry server metadata. `name` uses the `io.github.tinusj/` namespace required for GitHub-based authentication. |
| [`mcpb/manifest.json`](../mcpb/manifest.json) | MCPB bundle manifest. Tells MCP clients to run `java -jar server/swing-mcp-server.jar` with `SWING_MCP_AGENT_JAR` pointing at the bundled agent jar. |
| [`.github/workflows/publish-mcp.yml`](../.github/workflows/publish-mcp.yml) | Release workflow that builds the bundle and publishes to the registry. |

## Automated publishing (recommended)

Publishing is automated by the `Publish to MCP Registry` workflow. To release a new version:

1. Bump the Maven project version (e.g. `mvn versions:set -DnewVersion=1.1.0`).
2. Create and publish a GitHub release with a matching tag, e.g. `v1.1.0`.

The workflow then:

1. Builds the jars with Maven (JDK 21).
2. Assembles `swing-mcp.mcpb` (a zip of `manifest.json` plus the server and agent jars, with the
   manifest version set from the tag).
3. Uploads `swing-mcp.mcpb` as a release asset. MCPB verification requires the artifact URL to
   contain the string `mcp`, which the `.mcpb` extension satisfies.
4. Rewrites `server.json` in the workspace with the release version, download URL, and the
   artifact's SHA-256 (`fileSha256`), which MCP clients use to verify file integrity.
5. Authenticates with the registry via GitHub OIDC (`mcp-publisher login github-oidc`) — no
   secrets needed, and OIDC from this repo grants publish rights to the
   `io.github.tinusj/*` namespace.
6. Runs `mcp-publisher publish`.

## Manual publishing

If you need to publish by hand:

```bash
# Build
JAVA_HOME=/path/to/jdk-21 mvn -B -DskipTests package

# Assemble the bundle (version must match the release tag)
mkdir -p bundle/server
jq --arg v "1.0.0" '.version = $v' mcpb/manifest.json > bundle/manifest.json
cp swing-mcp-server/target/swing-mcp-server-*.jar bundle/server/swing-mcp-server.jar
cp swing-mcp-agent/target/swing-mcp-agent-*.jar bundle/server/swing-mcp-agent.jar
(cd bundle && zip -r ../swing-mcp.mcpb .)

# Attach swing-mcp.mcpb to the GitHub release for tag v1.0.0, then
# update server.json: version, packages[0].identifier (download URL),
# and packages[0].fileSha256:
sha256sum swing-mcp.mcpb

# Install mcp-publisher
curl -L "https://github.com/modelcontextprotocol/registry/releases/latest/download/mcp-publisher_$(uname -s | tr '[:upper:]' '[:lower:]')_$(uname -m | sed 's/x86_64/amd64/;s/aarch64/arm64/').tar.gz" | tar xz mcp-publisher

# Authenticate interactively with GitHub (device flow) and publish
./mcp-publisher login github
./mcp-publisher publish
```

## Verifying the published server

```bash
curl "https://registry.modelcontextprotocol.io/v0.1/servers?search=io.github.tinusj/swing-mcp"
```
