# Issue #1: Modal dialogs opened via `select_menu_item` deadlock the agent and desynchronize the command protocol

- **Date reported:** 2026-07-05
- **Severity:** High — agent becomes unusable, recovery requires detach + re-attach
- **Components affected:**
  - `swing-mcp-agent` — `AgentServer`, `CommandHandler`, `ComponentScanner.selectMenuItem`
  - `swing-mcp-server` — `session/AgentConnection`
- **Discovered during:** interactive UI test of `swing-mcp-demo` (attached session, PID 5104), see `TEST-REPORT.md`

---

## Summary

Clicking `Help > About` via the `select_menu_item` tool opened a modal `JOptionPane`. From that moment the agent stopped responding to **all** commands (every call failed with `transport failure: Read timed out`), the agent had **no awareness** that a modal dialog was open, and after the dialog was eventually dismissed the request/response protocol was **off-by-N desynchronized** — every tool call returned the response of a *previous* call. The only recovery was `stop_app` + `attach_to_app` (new session, new port).

Additionally, after the user manually clicked **OK** on the dialog, the **Help menu popup was still left open** on screen — the agent never closed the menu selection it had opened.

## Reproduction

1. Attach to the running demo app (`DemoFrame`).
2. Call `select_menu_item` with path `Help > About` (opens a modal `JOptionPane`).
3. Observe: the call times out (`Agent command SELECT_MENU_ITEM transport failure: Read timed out`).
4. Call anything else (`list_dialogs`, `handle_dialog`, `wait_for`, ...): all time out too.
   - Note the irony: `list_dialogs` / `handle_dialog` — the exact tools meant to deal with dialogs — are unusable in the one situation where they are needed.
5. Dismiss the dialog (user clicks OK, or an ENTER keypress slips through).
6. Observe: subsequent tool calls now return **stale responses of earlier timed-out calls**, shifted by one per timed-out request. Example transcript observed:
   - `press_key(ENTER)` → returned `"Menu item clicked: Help > About"`
   - `get_component_details(comp-71)` → returned `[]` (the earlier `list_dialogs` response)
   - next call → returned the earlier `wait_for` timeout error
   - next call → returned the earlier `handle_dialog` error
   - next call → returned `"Key pressed: ENTER"`
7. Session is permanently out of sync; only detach/re-attach fixes it.

## Root cause analysis (3 separate defects)

### Defect A — agent socket loop is single-threaded and blocks on modal dialogs

`AgentServer` (lines ~105-106) reads a line and calls `handler.handle(line)` **synchronously on the reader thread**:

```java
while ((line = reader.readLine()) != null) {
    String response = handler.handle(line);   // blocks for the whole command
```

`CommandHandler.dispatch` routes `SELECT_MENU_ITEM` through `invokeOnEdt(...)`, which does `latch.await()` **with no timeout** (CommandHandler.java, line 109). When the invoked menu action opens a **modal** dialog, `JMenuItem.doClick()` does not return until the dialog is closed, so:

- the EDT callable never completes → `latch.await()` never returns,
- the reader thread is stuck inside `handle()` → **no further commands are read or answered**.

Important nuance: a modal `JOptionPane` starts a *new event pump*, so the EDT itself is **not** frozen — `invokeLater` tasks would still run. The deadlock is purely in the agent's sequential command loop. If commands were processed concurrently, `list_dialogs`/`handle_dialog` would work fine while the menu action is blocked.

### Defect B — server does not correlate responses to requests (off-by-N desync)

`AgentConnection.send()` (swing-mcp-server) generates a `requestId`, but **never validates it** on the response path:

```java
writer.println(json);
String line = reader.readLine();               // SoTimeout → "Read timed out"
CommandResponse response = mapper.readValue(line, CommandResponse.class);
// response.requestId() is NEVER compared to requestId!
```

When a read times out, the request is abandoned but its response **stays in the TCP stream**. The next `send()` then reads that stale response and attributes it to the new request. Every timeout shifts the stream by one more response — this is exactly the observed off-by-N behavior. There is no drain/resync mechanism.

### Defect C — agent has no modal-dialog awareness and leaves the menu popup open

- `selectMenuItem` gives no indication that the action opened a modal dialog; the caller has no way to know it should switch to `handle_dialog`.
- After the dialog was dismissed by the user, the **JMenu popup remained open** — the menu selection path is never cleared (`MenuSelectionManager.defaultManager().clearSelectedPath()` is not called after/around invoking the item).

## Recommended fixes

### Fix 1 (agent): never block the command loop indefinitely

Pick one (or both):

- **(a) Fire-and-poll for action commands:** for `SELECT_MENU_ITEM` (and other commands that can trigger modal dialogs: `CLICK`, `HANDLE_DIALOG`, `SELECT_CONTEXT_MENU_ITEM`), post the action with `SwingUtilities.invokeLater` **without waiting for completion**, then wait a short bounded time (e.g. 500 ms). If the action hasn't completed, return success immediately with a result like `{"status":"pending","modalDialogOpen":true,"dialogTitle":"About"}` so the client knows to use `handle_dialog` next.
- **(b) Concurrent command handling:** in `AgentServer`, hand each incoming line to an executor and write responses (with `requestId`) as they complete, instead of processing serially on the reader thread. This keeps `list_dialogs` / `handle_dialog` / `take_screenshot` usable while an action command is blocked. (Requires Fix 2 so the server can match out-of-order responses.)
- Additionally add a **hard timeout** to `invokeOnEdt` (`latch.await(N, SECONDS)`) so no command can hang the agent forever; on timeout return an error that explicitly says a modal dialog is probably open.

### Fix 2 (server): correlate responses by `requestId`

In `AgentConnection.send()`:

- loop on `readLine()` until a response with the **matching `requestId`** arrives; **discard** (and log) responses whose `requestId` belongs to an abandoned/timed-out request;
- keep a small set of abandoned request ids so stale responses can be recognized and dropped;
- this alone eliminates the off-by-N desync and makes timeouts recoverable without re-attaching.

### Fix 3 (agent): modal dialog awareness + menu cleanup

- After executing `selectMenuItem`, call `MenuSelectionManager.defaultManager().clearSelectedPath()` (on the EDT) so the menu popup never stays open.
- Include modal dialog info in action responses (e.g. after any action, check `Window.getWindows()` for a new visible modal dialog and report `modalDialogOpen`, `dialogTitle`, `availableButtons`).
- Consider a proactive server-side behavior: when any command times out, automatically probe `list_dialogs` on a fresh/secondary connection and surface "a modal dialog appears to be blocking the agent: <title>" instead of a generic `Read timed out`.

## Expected behavior after fix

1. `select_menu_item("Help > About")` returns promptly with an indication that a modal dialog opened.
2. `list_dialogs` shows the About dialog; `handle_dialog(button="OK")` closes it.
3. No command ever leaves the agent permanently unresponsive; timeouts are recoverable in-session.
4. The menu popup is closed automatically after the item is invoked.
5. Stale/late responses are dropped by requestId matching — no off-by-N desync.

## Test cases to add

- Integration test: menu item that opens a modal `JOptionPane` → assert `select_menu_item` returns within N ms and `handle_dialog` can close the dialog (the demo app already has `Help > About` for this).
- Unit test (`AgentConnection`): inject a delayed response for request 1, then send request 2 → assert request 2 receives *its own* response and the stale response is discarded.
- Regression test: after `select_menu_item`, assert `MenuSelectionManager` selection path is empty.

---

## Resolution (2026-07-05)

**Status: FIXED** — all three defects addressed; all recommended fixes implemented.

- **Defect A** — `AgentServer.handleClient` now dispatches every command line to its own virtual thread (responses written under a lock, correlated by request id), so `list_dialogs`/`handle_dialog` keep working while an action blocks. `CommandHandler` runs `CLICK`, `SELECT_MENU_ITEM`, `SELECT_CONTEXT_MENU_ITEM`, and `HANDLE_DIALOG` fire-and-poll (500 ms bounded wait): if the action opened a modal dialog it returns `{"status":"pending","modalDialogOpen":true,"dialogs":[...]}` immediately. Query commands got a 20 s hard EDT timeout so nothing can hang the agent forever.
- **Defect B** — `AgentConnection.send` now loops until the response with the **matching requestId** arrives, discarding (and logging) stale/late responses of abandoned requests. Timeouts are recoverable in-session and the error message points to `list_dialogs`/`handle_dialog`.
- **Defect C** — `ComponentScanner.selectMenuItem` clears the menu selection path (`MenuSelectionManager.clearSelectedPath()`) before and after `doClick()`; pending results include the open dialogs (title, modality, buttons).

**Tests added**

- `swing-mcp-agent`: `ModalDialogCommandTest` — menu item opens a modal `JOptionPane`; asserts `SELECT_MENU_ITEM` returns promptly with `pending`/`modalDialogOpen`, `LIST_DIALOGS` works while the dialog is open, `HANDLE_DIALOG(OK)` closes it, and the menu selection path is cleared. (Skipped when headless; runs under xvfb in CI.)
- `swing-mcp-server`: `AgentConnectionTest#sendDiscardsStaleResponsesAndReturnsOwnResult` and `#timedOutRequestDoesNotDesyncSubsequentRequests` — verify requestId correlation and off-by-N recovery, using the extended multi-response `FakeAgentServer`.

