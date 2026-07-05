# Issue #2: Stale `JPopupMenu` remains in the component tree (visible=true) after `select_menu_item`

- **Date reported:** 2026-07-05
- **Severity:** Low — cosmetic / snapshot-quality; no functional impact on interaction
- **Type:** Follow-up polish from Issue #1 (found during its fix verification, see `RETEST-REPORT.md`)
- **Components affected:**
  - `swing-mcp-agent` — `ComponentScanner.selectMenuItem` (popup cleanup), `ComponentScanner.takeSnapshot` / `findComponent` (snapshot filtering)

---

## Summary

After `select_menu_item` invokes a menu item, the menu is no longer *displayed* (the Issue #1 fix calls `MenuSelectionManager.defaultManager().clearSelectedPath()`), but the menu's lightweight popup **component is left behind in the window's `JLayeredPane`** and still reports `visible=true`. It appears in every subsequent snapshot and `find_component` result as a live component, even though it is not showing on screen.

## Observed behavior (retest session, demo app PID 32456)

After `Help > About` was invoked and its dialog dismissed:

1. `find_component(by=CLASS, query=JPopupMenu)` returns:
   ```json
   [{"uid":"comp-2","class":"javax.swing.JPopupMenu","visible":true,"bounds":{"x":0,"y":0,"width":55,"height":26}}]
   ```
2. `take_snapshot` shows the popup's wrapper panel parked in the layered pane:
   ```
   JLayeredPane
   ├── JPanel (comp-4, x=31, y=21, 55x26, visible=true)   ← leftover popup container
   │   └── JPopupMenu (comp-2, visible=true)
   │       └── JMenuItem "About" (comp-3)
   └── JPanel null.contentPane ...
   ```
3. A component-level screenshot of the wrapper still renders the "About" item.
4. Full-window screenshots confirm the popup is **not painted on screen** (`retest-04-no-popup-visible.png`), and clicks on components underneath work normally — so this is purely a stale-component artifact.
5. Pressing `ESCAPE` does not remove it either (nothing to dismiss — the menu selection is already cleared).

## Why it matters

- **Misleading snapshots:** any client (LLM agent, test script) inspecting the tree sees an "open" popup menu that isn't there. During Issue #1 verification this triggered a false alarm and required extra screenshots to disprove.
- **Wasted tokens/effort:** MCP clients may try to interact with or dismiss a popup that doesn't exist.
- **Inconsistent semantics:** for `JPopupMenu`, `isVisible()==true` conventionally means "showing"; the leftover breaks that assumption downstream.

## Root cause (hypothesis)

`ComponentScanner.selectMenuItem` opens the menu path programmatically (making Swing create a *lightweight* popup hosted in the frame's `JLayeredPane` at `POPUP_LAYER`), then invokes the item and calls `clearSelectedPath()`. Clearing the selection path cancels the menu logic, but the lightweight popup container that was added to the layered pane is not hidden/removed via its owning `Popup`/`PopupFactory` handle, so:

- the wrapper `JPanel` stays as a child of the `JLayeredPane`,
- `JPopupMenu.isVisible()` remains `true` because `setVisible(false)` was never called on it.

This likely only happens in the pending/modal path added for Issue #1: the popup was shown, the item's action blocked in the modal dialog, and the cleanup that would normally run inside `selectMenuItem` either runs too early (before the popup is realized) or misses the popup instance. The non-modal path may have the same leak — needs a check.

## Reproduction

1. Launch the demo (`DemoFrame`) via the agent.
2. `select_menu_item("Help > About")` → returns pending with `modalDialogOpen=true`.
3. `handle_dialog(button="OK")`.
4. `find_component(by=CLASS, query="JPopupMenu")` → **expected:** no visible popup; **actual:** popup with `visible=true`.
5. `take_snapshot` → **expected:** no popup subtree in `JLayeredPane`; **actual:** wrapper `JPanel` + `JPopupMenu` + `JMenuItem` present.

Also verify the simple case (a menu item that does *not* open a modal dialog) for the same leak.

## Recommended fix

### Primary (agent, `ComponentScanner.selectMenuItem`)

After invoking the menu item (in **both** the completed and the pending/modal code paths), on the EDT:

1. Call `MenuSelectionManager.defaultManager().clearSelectedPath()` (already done).
2. Explicitly hide the popup: for each `JMenu` along the opened path, `menu.getPopupMenu().setVisible(false)`.
3. Defensively sweep leftovers: iterate the window's `JLayeredPane` children in `JLayeredPane.POPUP_LAYER`; remove any component whose child is a non-showing `JPopupMenu`, then `revalidate()`/`repaint()` the layered pane.
4. For the pending/modal path: perform this cleanup when the deferred action completes (the same continuation that reports completion), not only at scheduling time.

### Secondary (defense in depth, snapshot layer)

- In `takeSnapshot`/`findComponent`, report popups using `isShowing()` semantics: if a `JPopupMenu` (or its layered-pane wrapper) is `visible=true` but not actually showing/painted, mark it `visible=false` or omit it from `VISIBLE_ONLY` snapshots. This keeps snapshots truthful even if a future leak reappears.

## Expected behavior after fix

1. After any `select_menu_item` call (modal or not), no `JPopupMenu` remains in the component tree, or at minimum none reports `visible=true`.
2. `find_component(query="JPopupMenu")` returns `[]` once the menu interaction is finished.
3. `VISIBLE_ONLY` snapshots never contain non-showing popups.

## Test cases to add

- After `select_menu_item` on a plain item (no dialog): assert no component of class `JPopupMenu` exists in the frame's layered pane, and any popup instance has `isVisible()==false`.
- After the modal-dialog flow (`select_menu_item` → pending → `handle_dialog(OK)`): same assertions, executed after the deferred action completes.
- Snapshot test: `take_snapshot(filter=VISIBLE_ONLY)` after both flows contains no `JPopupMenu` node.

## References

- `RETEST-REPORT.md` — section "Defect C" cosmetic note, screenshot `retest-04-no-popup-visible.png`
- `ISSUE-001-modal-dialog-blocks-agent.md` — parent issue

---

## Resolution (2026-07-05)

**Status: FIXED** — both the primary (agent cleanup) and secondary (snapshot) fixes implemented.

Root cause confirmed: `findMenuItemInMenu` calls `menu.setPopupMenuVisible(true)` to realize a menu's items during lookup, which creates a lightweight popup in the frame's `JLayeredPane`. `clearSelectedPath()` (the Issue #1 fix) cancels the menu selection but does **not** undo `setPopupMenuVisible`, so the popup wrapper stayed parked in the layered pane reporting `visible=true`. The leak existed on both the plain and modal paths (menu-bar menus), and the same pattern applied to context-menu submenus.

**Primary fix (`ComponentScanner`)**

- `findMenuItem` / `findMenuItemInMenu` now record every `JMenu` whose popup they realize into an `openedMenus` collector.
- `selectMenuItem` calls the new `hideMenus(...)` **before** `doClick()` (so nothing leaks even while a modal dialog is blocking) and again in the `finally` (defense if the action re-opened a menu). `hideMenus` calls `setPopupMenuVisible(false)` + `getPopupMenu().setVisible(false)` on each realized menu, deepest-first.
- `sweepStalePopups(win)` defensively removes any layered-pane child that wraps a non-showing `JPopupMenu`, then `revalidate()`/`repaint()`.
- The context-menu path (`selectContextMenuItem` → `findPopupMenuItem`) threads the same collector and hides realized submenu popups + sweeps in its `finally`.

**Secondary fix (snapshot, defense in depth)**

- `matchesFilter(VISIBLE_ONLY)` now routes through `isEffectivelyVisible`, which reports a `JPopupMenu` by `isShowing()` rather than `isVisible()`, so a stale non-painted popup is never included in `VISIBLE_ONLY` snapshots even if a future leak reappears.

**Tests added** (`ModalDialogCommandTest`, agent module, skipped when headless)

- `plainMenuItemLeavesNoStalePopup` — `select_menu_item` on a no-dialog item leaves zero showing `JPopupMenu`.
- `modalMenuFlowLeavesNoStalePopup` — after `select_menu_item` → `handle_dialog(OK)`, zero showing `JPopupMenu` remains.

**Verification** — full `mvn verify` green under xvfb (23 tests). Live MCP-over-stdio run against the demo app: after `Help > About` → `handle_dialog(OK)`, `find_component(query="JPopupMenu")` returns `[]` and a `VISIBLE_ONLY` snapshot contains no `JPopupMenu` node.

