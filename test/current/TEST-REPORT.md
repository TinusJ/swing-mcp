# Swing MCP Demo — UI Test Report

- **Date:** 2026-07-05
- **Application:** `io.github.tinusj.swingmcp.demo.DemoFrame` (window title: "Swing MCP Demo")
- **Session:** attached to running JVM (PID 5104) via swing-mcp agent
- **Result:** ✅ All panels, options, and buttons tested — all functions work as expected (1 known limitation, see Menus)

All feedback is shown in the shared status bar label at the bottom of the window (`statusLabel`).

---

## 1. Buttons Panel (tab 0) — `buttonsPanel`

Screenshots: `01-buttons-initial.png`, `02-buttons-after-tests.png`

| Component | Name | Action | Result | Status |
|---|---|---|---|---|
| JButton "Click Me" | `clickMeButton` | Clicked | Increments a click counter → status: `Clicked N times` (2 → 3) | ✅ |
| JToggleButton "Toggle" | `toggleButton` | Clicked twice | Toggles state → status: `Toggle: ON`, then `Toggle: OFF` | ✅ |
| JCheckBox "Enable feature" | `featureCheckBox` | Checked | Status: `Feature: enabled` | ✅ |
| JRadioButton "Option A" | `optionARadio` | Selected | Status: `Selected: Option A` | ✅ |
| JRadioButton "Option B" | `optionBRadio` | Selected | Status: `Selected: Option B` (mutually exclusive with A) | ✅ |
| JButton "Disabled" | `disabledButton` | Inspected | Confirmed `enabled=false` — not clickable, by design | ✅ |

**Function:** demonstrates basic button types; every interaction reports its state to the status bar.

---

## 2. Forms Panel (tab 1) — `formsPanel`

Screenshots: `03-forms-filled.png`, `04-forms-submitted.png`

| Component | Name | Action | Result | Status |
|---|---|---|---|---|
| JTextField "Name" | `nameField` | Filled `Jane Tester` | Accepts text | ✅ |
| JPasswordField "Password" | `passwordField` | Filled `s3cret!` | Accepts masked input | ✅ |
| JSpinner "Age" | `ageSpinner` | Set to `42` (default 30) | Accepts numeric value | ✅ |
| JTextArea "Notes" | `notesArea` | Filled test note | Accepts multi-line text | ✅ |
| JButton "Submit" | `submitButton` | Clicked | Status: `Submitted: name=Jane Tester, age=42.0` | ✅ |

**Function:** simple data-entry form; Submit gathers field values and echoes name + age to the status bar (password and notes are not echoed).

Note: age displays as `42.0` — spinner value reported as double.

---

## 3. Lists Panel (tab 2) — `listsPanel`

Screenshot: `05-lists-panel.png`

| Component | Name | Action | Result | Status |
|---|---|---|---|---|
| JComboBox "Color" | `colorCombo` | Selected `Green`, then `Blue` (default `Red`) | Status: `Color: Green`, then `Color: Blue` | ✅ |
| JList | `itemList` | Selected `Item 7` | Status: `List selected: Item 7` | ✅ |

**Function:** selection widgets. Combo box holds colors (Red/Green/Blue). List contains 20 entries (`Item 1` … `Item 20`) inside a scroll pane. Each selection is reported to the status bar.

---

## 4. Table Panel (tab 3) — `tablePanel`

Screenshot: `06-table-selection.png`

Table `peopleTable` contents (verified via model extraction):

| ID | Name | Role |
|---|---|---|
| 1 | Alice | Engineer |
| 2 | Bob | Designer |
| 3 | Carol | Manager |
| 4 | Dave | Analyst |
| 5 | Eve | Tester |

| Action | Result | Status |
|---|---|---|
| Selected cell row 2, col 1 (Carol) | Status: `Table selected row: Carol` | ✅ |

**Function:** 5-row read-only people table; row selection reports the Name column of the selected row to the status bar.

---

## 5. Tree Panel (tab 4) — `treePanel`

Screenshot: `07-tree-selection.png`

Tree `projectTree` structure:

```
Projects
├── Alpha
└── Beta
```

| Action | Result | Status |
|---|---|---|
| Selected `Projects > Alpha` | Status: `Tree selected: Alpha` | ✅ |
| Selected `Projects > Beta` | Status: `Tree selected: Beta` | ✅ |

**Function:** small project tree; node selection reports the node name to the status bar.

---

## 6. Menu Bar

Screenshot: `08-final-after-about.png`

| Menu | Action | Result | Status |
|---|---|---|---|
| Help > About | Clicked | Opens a modal About dialog; after dismissal status shows `About dialog closed` | ✅ (works) ⚠️ (agent limitation) |
| File > Exit | **Not tested** | Skipped intentionally — would terminate the app | — |

**⚠️ Known limitation found during testing:** clicking `Help > About` opens a **modal** `JOptionPane` that blocks the agent command thread — subsequent MCP commands timed out until the dialog was dismissed (pressing ENTER got through). This desynchronized the command/response queue; recovery required detaching and re-attaching the agent session. Recommendation: the agent's `select_menu_item` should invoke menu actions asynchronously (e.g. `SwingUtilities.invokeLater` without waiting) so modal dialogs can be handled with `handle_dialog`.

---

## Screenshot Index

| File | Description |
|---|---|
| `01-buttons-initial.png` | Buttons panel, initial state |
| `02-buttons-after-tests.png` | Buttons panel after all interactions (Option B selected, checkbox checked) |
| `03-forms-filled.png` | Forms panel with all fields filled |
| `04-forms-submitted.png` | Forms panel after Submit (status shows submitted values) |
| `05-lists-panel.png` | Lists panel, combo = Blue, Item 7 selected |
| `06-table-selection.png` | Table panel with Carol's row selected |
| `07-tree-selection.png` | Tree panel with Beta node selected |
| `08-final-after-about.png` | Final state after About dialog closed |

## Summary

- **Panels tested:** 5/5 (Buttons, Forms, Lists, Table, Tree) + menu bar
- **Controls tested:** 16 interactive components
- **Failures:** 0 application bugs
- **Issues:** 1 tooling limitation (modal dialog blocks agent commands — see Menus section)

