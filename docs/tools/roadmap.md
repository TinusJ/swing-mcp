# Tools roadmap

All tools and capabilities previously tracked on this page have been
implemented:

| Capability | Where it landed |
|---|---|
| Snapshot state filtering | `take_snapshot` `filter` parameter — [inspection.md](inspection.md) |
| `find_component` | [inspection.md](inspection.md) |
| `get_table_data` / `get_list_items` | [inspection.md](inspection.md) |
| `hover` | [interaction.md](interaction.md) |
| `focus` | [interaction.md](interaction.md) |
| `type_text` | [interaction.md](interaction.md) |
| Right-click context menu selection | `select_context_menu_item` — [interaction.md](interaction.md) |
| `list_dialogs` / `handle_dialog` | [dialogs.md](dialogs.md) |
| `move_window` | [windows.md](windows.md) |
| `maximize_window` / `minimize_window` / `restore_window` | [windows.md](windows.md) |
| `get_clipboard` / `set_clipboard` | [clipboard.md](clipboard.md) |
| Additional `wait_for` conditions | `COMPONENT_EXISTS`, `COMPONENT_GONE`, `WINDOW_COUNT`, `EDT_IDLE` — [utilities.md](utilities.md) |
| Inline screenshot image content | `take_screenshot` `returnImage` parameter — [screenshots.md](screenshots.md) |
| Multiple concurrent sessions | Named sessions with `list_sessions` / `select_session` — [application.md](application.md) |

Ideas for future tools are welcome — please open an issue describing the
intended behaviour.
