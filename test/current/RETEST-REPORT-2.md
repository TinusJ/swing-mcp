# Retest Report #2 — Fix Verification for Issue #2 (stale popup menu in tree)

- **Date:** 2026-07-05 (~22:07)
- **Build under test:** all modules rebuilt 22:05 (fix commit), fresh demo launched via agent (PID 3948, LAUNCHED mode)
- **Reference:** `ISSUE-002-stale-popup-menu-in-tree.md`
- **Overall result: ✅ FIX VERIFIED — Issue #2 can be closed**

Fix confirmed present in source before testing:
- `ComponentScanner.selectMenuItem` — `popup.setVisible(false)` in both completed and deferred paths
- Layered-pane sweep of non-showing popups
- Snapshot layer uses `isShowing()` semantics for popups
- New test coverage in `ModalDialogCommandTest` (asserts no showing popup)

---

## Verification against the Issue #2 repro steps

| Step | Action | Expected | Actual | Result |
|---|---|---|---|---|
| 1-2 | `select_menu_item("Help > About")` | pending + `modalDialogOpen=true` | as expected | ✅ |
| 3 | `handle_dialog(OK)` | dialog closes | `"Dialog button clicked: OK"`, `list_dialogs` → `[]` | ✅ |
| 4 | `find_component(CLASS, "JPopupMenu")` | `[]` (previously: stale popup, `visible=true`) | **`[]`** | ✅ |
| 5 | `take_snapshot(VISIBLE_ONLY)` | no popup subtree in `JLayeredPane` (previously: leftover `JPanel` → `JPopupMenu` → `JMenuItem`) | **layered pane contains only the content pane** | ✅ |

## Repeatability & regression

- **Second About cycle** (`select_menu_item` → `handle_dialog(OK)` → `find_component`): again `[]` — cleanup is repeatable, no accumulation of leftovers. ✅
- Status label correct after each step (`About dialog closed`). ✅
- Interaction after cleanup unaffected: `click(Click Me)` → `Clicked 1 times`. ✅
- Protocol stayed in sync for the entire session (every response matched its request). ✅

## Expected behaviors from the ticket — all met

1. ✅ No `JPopupMenu` remains in the component tree after `select_menu_item` (modal flow, tested twice).
2. ✅ `find_component(query="JPopupMenu")` returns `[]` once the menu interaction is finished.
3. ✅ `VISIBLE_ONLY` snapshots contain no popup nodes.

## Artifact

- `retest2-01-clean-after-about.png` — final state after two About cycles + button click; clean UI.

## Verdict

**Issue #2 verified fixed and can be closed.** Both open issues (ISSUE-001, ISSUE-002) are now resolved; the swing-mcp agent handles the full menu → modal dialog → dismissal lifecycle cleanly with truthful snapshots throughout.

