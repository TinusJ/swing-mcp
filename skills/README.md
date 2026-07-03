# Swing MCP — Agent Skills

Reusable agent skills that teach AI coding agents how to use the Swing MCP
server effectively. Each skill is a folder containing a `SKILL.md` with
frontmatter (`name`, `description`) and workflow instructions, following the
same structure as the
[chrome-devtools-mcp skills](https://github.com/ChromeDevTools/chrome-devtools-mcp/tree/main/skills).

| Skill | Use for |
|---|---|
| [swing-mcp](swing-mcp/SKILL.md) | Core concepts and workflow patterns: sessions, snapshots/UIDs, tool selection, dialogs |
| [swing-ui-testing](swing-ui-testing/SKILL.md) | Testing and verifying Swing apps: forms, tables/trees, menus, dialogs, keyboard navigation |
| [troubleshooting](troubleshooting/SKILL.md) | Diagnosing server, agent, and session failures step by step |

## Installation

Copy the skill folders into your agent's skills directory, for example:

- **Claude Code**: `.claude/skills/` in your project (or `~/.claude/skills/` globally)
- **Other agents**: consult your agent's documentation for its skills location

The agent will automatically load a skill when its `description` matches the
task at hand.
