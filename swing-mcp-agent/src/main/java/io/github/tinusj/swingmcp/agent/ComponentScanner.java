package io.github.tinusj.swingmcp.agent;

import io.github.tinusj.swingmcp.common.dto.ComponentDescriptor;
import io.github.tinusj.swingmcp.common.dto.SnapshotNode;
import io.github.tinusj.swingmcp.common.enums.ScrollDirection;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreePath;

/**
 * Scans Swing component trees and performs interactions on behalf of the agent.
 * Maintains a UID registry mapping string UIDs to {@link Component} instances
 * via weak references so that garbage-collected components are automatically
 * removed.
 */
public class ComponentScanner {

    private final AtomicInteger uidCounter = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Component> uidToComponent = new ConcurrentHashMap<>();
    private final Map<Component, String> componentToUid = new WeakHashMap<>();
    private Window activeWindow;

    /**
     * Takes a snapshot of the current active window's component tree.
     *
     * @param params optional params (may contain "windowIndex")
     * @return a {@link SnapshotNode} with full component tree
     */
    public SnapshotNode takeSnapshot(Map<String, Object> params) {
        Window win = getTargetWindow(params);
        if (win == null) {
            return new SnapshotNode("No window", "null", 0, 0, 0, 0, List.of());
        }
        List<ComponentDescriptor> children = scanComponent(win);
        Rectangle bounds = win.getBounds();
        return new SnapshotNode(
            getWindowTitle(win),
            win.getClass().getSimpleName(),
            bounds.x, bounds.y, bounds.width, bounds.height,
            children
        );
    }

    /**
     * Lists all visible windows in the current JVM.
     *
     * @return list of window descriptors as maps
     */
    public List<Map<String, Object>> listWindows() {
        List<Map<String, Object>> result = new ArrayList<>();
        int index = 0;
        for (Window w : Window.getWindows()) {
            if (w.isVisible()) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("index", index++);
                info.put("title", getWindowTitle(w));
                info.put("class", w.getClass().getSimpleName());
                Rectangle b = w.getBounds();
                info.put("x", b.x);
                info.put("y", b.y);
                info.put("width", b.width);
                info.put("height", b.height);
                info.put("focused", w.isFocused());
                result.add(info);
            }
        }
        return result;
    }

    /**
     * Sets the active window by index.
     */
    public String selectWindow(Map<String, Object> params) {
        int index = getInt(params, "index", 0);
        List<Window> visible = getVisibleWindows();
        if (index < 0 || index >= visible.size()) {
            throw new IllegalArgumentException("Window index " + index + " out of range [0, " + (visible.size() - 1) + "]");
        }
        activeWindow = visible.get(index);
        activeWindow.toFront();
        activeWindow.requestFocus();
        return "Window selected: " + getWindowTitle(activeWindow);
    }

    /**
     * Resizes the active (or specified) window.
     */
    public String resizeWindow(Map<String, Object> params) {
        Window win = getTargetWindow(params);
        if (win == null) {
            throw new IllegalStateException("No active window");
        }
        int width = getInt(params, "width", win.getWidth());
        int height = getInt(params, "height", win.getHeight());
        win.setSize(width, height);
        return "Window resized to " + width + "x" + height;
    }

    /**
     * Closes the specified window by dispatching a WINDOW_CLOSING event.
     */
    public String closeWindow(Map<String, Object> params) {
        Window win = getTargetWindow(params);
        if (win == null) {
            throw new IllegalStateException("No active window");
        }
        win.dispatchEvent(new WindowEvent(win, WindowEvent.WINDOW_CLOSING));
        return "Window closing event dispatched";
    }

    /**
     * Returns detailed info for a component identified by UID.
     */
    public Map<String, Object> getComponentDetails(Map<String, Object> params) {
        String uid = getString(params, "uid");
        Component comp = resolveUid(uid);
        return describeComponent(comp, true);
    }

    /**
     * Takes a screenshot of the active window or a specific component.
     */
    public Map<String, Object> takeScreenshot(Map<String, Object> params) throws IOException, AWTException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Cannot take screenshot in headless environment");
        }
        Component target;
        String uid = (String) params.get("uid");
        if (uid != null && !uid.isBlank()) {
            target = resolveUid(uid);
        } else {
            target = getTargetWindow(params);
        }
        if (target == null) {
            throw new IllegalStateException("No target component or window for screenshot");
        }
        Robot robot = new Robot();
        Rectangle bounds = new Rectangle(target.getLocationOnScreen(), target.getSize());
        BufferedImage img = robot.createScreenCapture(bounds);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imageBase64", base64);
        result.put("mimeType", "image/png");
        result.put("width", img.getWidth());
        result.put("height", img.getHeight());
        return result;
    }

    /**
     * Clicks a component identified by UID.
     */
    public String click(Map<String, Object> params) throws AWTException {
        String uid = getString(params, "uid");
        Component comp = resolveUid(uid);
        String buttonStr = getString(params, "button", "LEFT");
        String clickTypeStr = getString(params, "clickType", "SINGLE");

        if (comp instanceof AbstractButton btn) {
            btn.doClick();
            return "Clicked button: " + btn.getText();
        }

        if (!GraphicsEnvironment.isHeadless()) {
            Robot robot = new Robot();
            Point loc = comp.getLocationOnScreen();
            int cx = loc.x + comp.getWidth() / 2;
            int cy = loc.y + comp.getHeight() / 2;
            int mask = getMouseMask(buttonStr);
            robot.mouseMove(cx, cy);
            robot.mousePress(mask);
            robot.mouseRelease(mask);
            if ("DOUBLE".equalsIgnoreCase(clickTypeStr)) {
                robot.mousePress(mask);
                robot.mouseRelease(mask);
            }
        }
        return "Clicked component: " + uid;
    }

    /**
     * Fills a text component, spinner, or editable combo box with the given text.
     */
    public String fill(Map<String, Object> params) {
        String uid = getString(params, "uid");
        String text = getString(params, "text");
        Component comp = resolveUid(uid);

        if (comp instanceof JTextComponent tc) {
            tc.setText(text);
        } else if (comp instanceof JSpinner spinner) {
            spinner.setValue(parseSpinnerValue(spinner, text));
        } else if (comp instanceof JComboBox<?> combo && combo.isEditable()) {
            combo.setSelectedItem(text);
        } else {
            throw new IllegalArgumentException("Component " + uid + " does not support text input: " + comp.getClass().getSimpleName());
        }
        return "Filled: " + uid;
    }

    /**
     * Selects an option in a JList, JComboBox, or JTabbedPane by index or text.
     */
    @SuppressWarnings("unchecked")
    public String selectOption(Map<String, Object> params) {
        String uid = getString(params, "uid");
        Component comp = resolveUid(uid);
        String text = (String) params.get("text");
        Integer index = params.containsKey("index") ? getInt(params, "index", 0) : null;

        if (comp instanceof JComboBox<?> combo) {
            if (index != null) {
                combo.setSelectedIndex(index);
            } else {
                ((JComboBox<Object>) combo).setSelectedItem(text);
            }
        } else if (comp instanceof JList<?> list) {
            if (index != null) {
                list.setSelectedIndex(index);
            } else {
                for (int i = 0; i < list.getModel().getSize(); i++) {
                    if (String.valueOf(list.getModel().getElementAt(i)).equals(text)) {
                        list.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } else if (comp instanceof JTabbedPane tabs) {
            if (index != null) {
                tabs.setSelectedIndex(index);
            } else {
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    if (tabs.getTitleAt(i).equals(text)) {
                        tabs.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Component " + uid + " does not support option selection: " + comp.getClass().getSimpleName());
        }
        return "Option selected on: " + uid;
    }

    /**
     * Selects a tree node by path (e.g. "Root > Folder > Leaf").
     */
    public String selectTreeNode(Map<String, Object> params) {
        String uid = getString(params, "uid");
        String pathStr = getString(params, "path");
        Component comp = resolveUid(uid);

        if (!(comp instanceof JTree tree)) {
            throw new IllegalArgumentException("Component " + uid + " is not a JTree");
        }

        String[] parts = pathStr.split("\\s*>\\s*");
        TreePath treePath = findTreePath(tree, parts);
        if (treePath == null) {
            throw new IllegalArgumentException("Tree path not found: " + pathStr);
        }
        tree.setSelectionPath(treePath);
        tree.scrollPathToVisible(treePath);
        return "Tree node selected: " + pathStr;
    }

    /**
     * Selects a table cell by row and column.
     */
    public String selectTableCell(Map<String, Object> params) {
        String uid = getString(params, "uid");
        Component comp = resolveUid(uid);
        int row = getInt(params, "row", 0);
        int col = getInt(params, "col", 0);

        if (!(comp instanceof JTable table)) {
            throw new IllegalArgumentException("Component " + uid + " is not a JTable");
        }
        table.changeSelection(row, col, false, false);
        return "Table cell selected: [" + row + "," + col + "]";
    }

    /**
     * Selects a menu item by path (e.g. "File > Save").
     */
    public String selectMenuItem(Map<String, Object> params) {
        String pathStr = getString(params, "path");
        Window win = getTargetWindow(params);
        if (win == null) {
            throw new IllegalStateException("No active window");
        }
        String[] parts = pathStr.split("\\s*>\\s*");
        JMenuItem item = findMenuItem(win, parts);
        if (item == null) {
            throw new IllegalArgumentException("Menu item not found: " + pathStr);
        }
        item.doClick();
        return "Menu item clicked: " + pathStr;
    }

    /**
     * Sends a key press (e.g. "CTRL+S", "ENTER") using Robot.
     */
    public String pressKey(Map<String, Object> params) throws AWTException {
        String keys = getString(params, "keys");
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Cannot press keys in headless environment");
        }
        Robot robot = new Robot();
        List<Integer> keyCodes = parseKeyChord(keys);
        for (int code : keyCodes) {
            robot.keyPress(code);
        }
        for (int i = keyCodes.size() - 1; i >= 0; i--) {
            robot.keyRelease(keyCodes.get(i));
        }
        return "Key pressed: " + keys;
    }

    /**
     * Drags from one component to another using Robot.
     */
    public String drag(Map<String, Object> params) throws AWTException {
        String fromUid = getString(params, "fromUid");
        String toUid = getString(params, "toUid");
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Cannot drag in headless environment");
        }
        Component from = resolveUid(fromUid);
        Component to = resolveUid(toUid);
        Robot robot = new Robot();
        Point fromLoc = from.getLocationOnScreen();
        Point toLoc = to.getLocationOnScreen();
        robot.mouseMove(fromLoc.x + from.getWidth() / 2, fromLoc.y + from.getHeight() / 2);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseMove(toLoc.x + to.getWidth() / 2, toLoc.y + to.getHeight() / 2);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return "Dragged from " + fromUid + " to " + toUid;
    }

    /**
     * Scrolls a scrollable component in the specified direction.
     */
    public String scroll(Map<String, Object> params) {
        String uid = getString(params, "uid");
        String directionStr = getString(params, "direction", "DOWN");
        int amount = getInt(params, "amount", 3);
        Component comp = resolveUid(uid);

        ScrollDirection direction = ScrollDirection.valueOf(directionStr.toUpperCase());
        JScrollPane scrollPane = findScrollPane(comp);
        if (scrollPane == null) {
            throw new IllegalArgumentException("No scroll pane found for: " + uid);
        }

        JScrollBar bar = (direction == ScrollDirection.UP || direction == ScrollDirection.DOWN)
            ? scrollPane.getVerticalScrollBar()
            : scrollPane.getHorizontalScrollBar();

        int delta = (direction == ScrollDirection.DOWN || direction == ScrollDirection.RIGHT) ? amount : -amount;
        bar.setValue(bar.getValue() + delta * bar.getUnitIncrement());
        return "Scrolled " + direction + " by " + amount;
    }

    /**
     * Waits for a condition with polling. Params: conditionType, uid, expectedValue, timeoutMs.
     */
    public String waitFor(Map<String, Object> params) {
        String condType = getString(params, "conditionType");
        long timeoutMs = getLong(params, "timeoutMs", 5000L);
        long pollMs = 200L;
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            try {
                boolean met = checkCondition(condType, params);
                if (met) {
                    return "Condition met: " + condType;
                }
                Thread.sleep(pollMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Wait interrupted", e);
            }
        }
        throw new RuntimeException("Timeout waiting for condition: " + condType + " after " + timeoutMs + "ms");
    }

    /**
     * Evaluates a Java snippet using JShell (if supported).
     */
    public String evaluateJava(Map<String, Object> params) {
        String code = getString(params, "code");
        try {
            Class<?> jshellClass = Class.forName("jdk.jshell.JShell");
            Object jshell = jshellClass.getMethod("create").invoke(null);
            Object events = jshellClass.getMethod("eval", String.class).invoke(jshell, code);
            return events.toString();
        } catch (Exception e) {
            throw new RuntimeException("JShell evaluation failed: " + e.getMessage(), e);
        }
    }

    private List<ComponentDescriptor> scanComponent(Component comp) {
        List<ComponentDescriptor> result = new ArrayList<>();
        String uid = assignUid(comp);
        List<ComponentDescriptor> children = new ArrayList<>();
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                children.addAll(scanComponent(child));
            }
        }
        String text = extractText(comp);
        String selectionState = extractSelectionState(comp);
        Rectangle bounds = comp.getBounds();
        ComponentDescriptor desc = new ComponentDescriptor(
            uid,
            comp.getClass().getSimpleName(),
            comp.getName(),
            text,
            comp.isEnabled(),
            comp.isVisible(),
            comp.isFocusable(),
            bounds.x, bounds.y, bounds.width, bounds.height,
            selectionState,
            children.isEmpty() ? null : children
        );
        result.add(desc);
        return result;
    }

    private String assignUid(Component comp) {
        return componentToUid.computeIfAbsent(comp, c -> {
            String uid = "comp-" + uidCounter.incrementAndGet();
            uidToComponent.put(uid, c);
            return uid;
        });
    }

    private Component resolveUid(String uid) {
        Component comp = uidToComponent.get(uid);
        if (comp == null) {
            throw new IllegalArgumentException("Unknown component UID: " + uid + ". Please take a fresh snapshot.");
        }
        return comp;
    }

    private String extractText(Component comp) {
        if (comp instanceof javax.swing.JLabel lbl) {
            return lbl.getText();
        }
        if (comp instanceof AbstractButton btn) {
            return btn.getText();
        }
        if (comp instanceof JTextComponent tc) {
            return tc.getText();
        }
        if (comp instanceof JSpinner sp) {
            return String.valueOf(sp.getValue());
        }
        return null;
    }

    private String extractSelectionState(Component comp) {
        if (comp instanceof JComboBox<?> combo) {
            return String.valueOf(combo.getSelectedIndex());
        }
        if (comp instanceof JList<?> list) {
            return String.valueOf(list.getSelectedIndex());
        }
        if (comp instanceof JTabbedPane tabs) {
            return String.valueOf(tabs.getSelectedIndex());
        }
        if (comp instanceof JTable table) {
            return table.getSelectedRow() + "," + table.getSelectedColumn();
        }
        return null;
    }

    private Map<String, Object> describeComponent(Component comp, boolean detailed) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("uid", assignUid(comp));
        info.put("class", comp.getClass().getName());
        info.put("simpleName", comp.getClass().getSimpleName());
        info.put("name", comp.getName());
        info.put("text", extractText(comp));
        info.put("enabled", comp.isEnabled());
        info.put("visible", comp.isVisible());
        info.put("focusable", comp.isFocusable());
        Rectangle b = comp.getBounds();
        info.put("bounds", Map.of("x", b.x, "y", b.y, "width", b.width, "height", b.height));
        if (detailed) {
            info.put("selectionState", extractSelectionState(comp));
            javax.accessibility.AccessibleContext ac = comp.getAccessibleContext();
            if (ac != null) {
                info.put("accessibleName", ac.getAccessibleName());
                info.put("accessibleDescription", ac.getAccessibleDescription());
                if (ac.getAccessibleRole() != null) {
                    info.put("accessibleRole", ac.getAccessibleRole().toDisplayString());
                }
            }
        }
        return info;
    }

    private Window getTargetWindow(Map<String, Object> params) {
        if (activeWindow != null && activeWindow.isVisible()) {
            return activeWindow;
        }
        List<Window> visible = getVisibleWindows();
        return visible.isEmpty() ? null : visible.get(0);
    }

    private List<Window> getVisibleWindows() {
        List<Window> result = new ArrayList<>();
        for (Window w : Window.getWindows()) {
            if (w.isVisible()) {
                result.add(w);
            }
        }
        return result;
    }

    private String getWindowTitle(Window w) {
        if (w instanceof java.awt.Frame f) {
            return f.getTitle();
        }
        if (w instanceof java.awt.Dialog d) {
            return d.getTitle();
        }
        return w.getClass().getSimpleName();
    }

    private JScrollPane findScrollPane(Component comp) {
        if (comp instanceof JScrollPane sp) {
            return sp;
        }
        Component parent = comp.getParent();
        while (parent != null) {
            if (parent instanceof JScrollPane sp) {
                return sp;
            }
            parent = parent.getParent();
        }
        if (comp instanceof Container c) {
            for (Component child : c.getComponents()) {
                JScrollPane sp = findScrollPane(child);
                if (sp != null) {
                    return sp;
                }
            }
        }
        return null;
    }

    private JMenuItem findMenuItem(Component comp, String[] path) {
        if (path.length == 0) {
            return null;
        }
        if (comp instanceof javax.swing.JMenuBar menuBar) {
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                javax.swing.JMenu menu = menuBar.getMenu(i);
                if (menu != null && menu.getText().equals(path[0])) {
                    if (path.length == 1 && menu instanceof JMenuItem mi) {
                        return mi;
                    }
                    String[] rest = new String[path.length - 1];
                    System.arraycopy(path, 1, rest, 0, rest.length);
                    JMenuItem found = findMenuItemInMenu(menu, rest);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        if (comp instanceof Container c) {
            for (Component child : c.getComponents()) {
                JMenuItem found = findMenuItem(child, path);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private JMenuItem findMenuItemInMenu(javax.swing.JMenu menu, String[] path) {
        menu.setPopupMenuVisible(true);
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item == null) {
                continue;
            }
            if (item.getText().equals(path[0])) {
                if (path.length == 1) {
                    return item;
                }
                if (item instanceof javax.swing.JMenu subMenu) {
                    String[] rest = new String[path.length - 1];
                    System.arraycopy(path, 1, rest, 0, rest.length);
                    return findMenuItemInMenu(subMenu, rest);
                }
            }
        }
        return null;
    }

    private TreePath findTreePath(JTree tree, String[] parts) {
        Object root = tree.getModel().getRoot();
        if (!String.valueOf(root).equals(parts[0])) {
            return null;
        }
        List<Object> pathNodes = new ArrayList<>();
        pathNodes.add(root);
        Object current = root;
        for (int i = 1; i < parts.length; i++) {
            int childCount = tree.getModel().getChildCount(current);
            Object found = null;
            for (int j = 0; j < childCount; j++) {
                Object child = tree.getModel().getChild(current, j);
                if (String.valueOf(child).equals(parts[i])) {
                    found = child;
                    break;
                }
            }
            if (found == null) {
                return null;
            }
            pathNodes.add(found);
            current = found;
        }
        return new TreePath(pathNodes.toArray());
    }

    private boolean checkCondition(String condType, Map<String, Object> params) {
        return switch (condType.toUpperCase()) {
            case "WINDOW_TITLE" -> {
                String expected = getString(params, "expectedValue");
                yield getVisibleWindows().stream().anyMatch(w -> getWindowTitle(w).contains(expected));
            }
            case "COMPONENT_TEXT" -> {
                String uid = getString(params, "uid");
                String expected = getString(params, "expectedValue");
                try {
                    Component comp = resolveUid(uid);
                    String text = extractText(comp);
                    yield text != null && text.contains(expected);
                } catch (Exception e) {
                    yield false;
                }
            }
            case "COMPONENT_VISIBLE" -> {
                String uid = getString(params, "uid");
                try {
                    Component comp = resolveUid(uid);
                    yield comp.isVisible();
                } catch (Exception e) {
                    yield false;
                }
            }
            case "COMPONENT_ENABLED" -> {
                String uid = getString(params, "uid");
                try {
                    Component comp = resolveUid(uid);
                    yield comp.isEnabled();
                } catch (Exception e) {
                    yield false;
                }
            }
            default -> throw new IllegalArgumentException("Unknown condition type: " + condType);
        };
    }

    private int getMouseMask(String button) {
        return switch (button.toUpperCase()) {
            case "RIGHT" -> InputEvent.BUTTON3_DOWN_MASK;
            case "MIDDLE" -> InputEvent.BUTTON2_DOWN_MASK;
            default -> InputEvent.BUTTON1_DOWN_MASK;
        };
    }

    private List<Integer> parseKeyChord(String keys) {
        List<Integer> codes = new ArrayList<>();
        for (String part : keys.split("\\+")) {
            codes.add(switch (part.trim().toUpperCase()) {
                case "CTRL" -> KeyEvent.VK_CONTROL;
                case "ALT" -> KeyEvent.VK_ALT;
                case "SHIFT" -> KeyEvent.VK_SHIFT;
                case "META" -> KeyEvent.VK_META;
                case "ENTER" -> KeyEvent.VK_ENTER;
                case "ESCAPE", "ESC" -> KeyEvent.VK_ESCAPE;
                case "TAB" -> KeyEvent.VK_TAB;
                case "SPACE" -> KeyEvent.VK_SPACE;
                case "BACKSPACE" -> KeyEvent.VK_BACK_SPACE;
                case "DELETE", "DEL" -> KeyEvent.VK_DELETE;
                case "UP" -> KeyEvent.VK_UP;
                case "DOWN" -> KeyEvent.VK_DOWN;
                case "LEFT" -> KeyEvent.VK_LEFT;
                case "RIGHT" -> KeyEvent.VK_RIGHT;
                default -> {
                    String t = part.trim().toUpperCase();
                    int code = KeyEvent.getExtendedKeyCodeForChar(t.charAt(0));
                    yield code != KeyEvent.VK_UNDEFINED ? code : (int) t.charAt(0);
                }
            });
        }
        return codes;
    }

    private Object parseSpinnerValue(JSpinner spinner, String text) {
        javax.swing.SpinnerModel model = spinner.getModel();
        if (model instanceof javax.swing.SpinnerNumberModel) {
            return Double.parseDouble(text);
        }
        return text;
    }

    private String getString(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return val.toString();
    }

    private String getString(Map<String, Object> params, String key, String defaultVal) {
        Object val = params.get(key);
        return val == null ? defaultVal : val.toString();
    }

    private int getInt(Map<String, Object> params, String key, int defaultVal) {
        Object val = params.get(key);
        if (val == null) {
            return defaultVal;
        }
        if (val instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(val.toString());
    }

    private long getLong(Map<String, Object> params, String key, long defaultVal) {
        Object val = params.get(key);
        if (val == null) {
            return defaultVal;
        }
        if (val instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(val.toString());
    }
}
