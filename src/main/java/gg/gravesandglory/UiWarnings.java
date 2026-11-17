package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.*;

/**
 * The UI editor's warnings (UI editor design 5.6, phase 2), shown before Publish: a line drawn cut short ("Long wor...") or clipped by
 * the box it is drawn in, a line partly off the screen, and a line that runs into another line or into a button it is not drawn on.
 * Every frame of a screen in edit mode is checked ({@link #compute}, from the boxes UiEditorClient located); {@link #start} walks a
 * screen through every common window size and GUI scale (UiPreview.checkSizes) and keeps what each size showed, and {@link #tour}
 * does that for every screen the editor's browser can open. Only lines drawn through UiText are checked: panels are not pieces yet.
 */
public final class UiWarnings {
    private UiWarnings() {}

    /** This frame's warnings: a line's id to the first thing wrong with it. */
    static final Map<String, String> NOW = new LinkedHashMap<>();
    /** What the size checks found: screen name, then line id, then what is wrong, then the sizes it showed at. */
    static final Map<String, Map<String, Map<String, Set<String>>>> FOUND = new LinkedHashMap<>();

    static final String CUT = "cut short", CLIPPED = "clipped by its box", OFF = "off screen", BUTTON = "runs into a button";

    // ---- one frame ----

    /** Checks this frame's boxes (real GUI pixels) on a screen of w by h (the strip's band excluded). */
    static void compute(List<UiEditorClient.Box> boxes, List<float[]> widgets, int w, int h) {
        NOW.clear();
        int n = Math.min(boxes.size(), 600);
        for (int i = 0; i < n; i++) {
            UiEditorClient.Box b = boxes.get(i);
            if (hidden(b)) continue;
            String why = problem(b, w, h);
            if (why == null) why = button(b, widgets);
            if (why == null) {
                for (int j = 0; j < n; j++) {
                    UiEditorClient.Box o = boxes.get(j);
                    if (j != i && !o.id().equals(b.id()) && !hidden(o) && overlap(b, o)) { why = "overlaps " + o.id(); break; }
                }
            }
            if (why != null) NOW.putIfAbsent(b.id(), why);
        }
    }

    /** Cut short, clipped sideways by its scissor box (clipped up or down is a list scrolled, not an overflow), or off the screen. */
    static String problem(UiEditorClient.Box b, int w, int h) {
        if (hidden(b)) return null;
        if (b.trimmed()) return CUT;
        if (b.x0() < b.clipL() - 1 || b.x1() > b.clipR() + 1) return CLIPPED;
        if (b.x0() < -1 || b.y0() < -1 || b.x1() > w + 1 || b.y1() > h + 1) return OFF;
        return null;
    }

    /** A line wholly outside its scissor box up or down: a list row scrolled away, not drawn. */
    static boolean hidden(UiEditorClient.Box b) { return b.y1() <= b.clipT() + 1 || b.y0() >= b.clipB() - 1; }

    /** A line that crosses a button's edge (a label inside its own button is fine). */
    static String button(UiEditorClient.Box b, List<float[]> widgets) {
        for (float[] r : widgets) {
            boolean touches = b.x0() + 1 < r[2] && r[0] < b.x1() - 1 && b.y0() + 1 < r[3] && r[1] < b.y1() - 1;
            boolean inside = b.x0() >= r[0] - 1 && b.x1() <= r[2] + 1 && b.y0() >= r[1] - 1 && b.y1() <= r[3] + 1;
            if (touches && !inside) return BUTTON;
        }
        return null;
    }

    /** Two lines' boxes cross by more than the pixel of slack their boxes carry (lines set one font height apart do not). */
    static boolean overlap(UiEditorClient.Box a, UiEditorClient.Box b) {
        return a.x0() + 1 < b.x1() - 1 && b.x0() + 1 < a.x1() - 1 && a.y0() + 1 < b.y1() - 1 && b.y0() + 1 < a.y1() - 1;
    }

    // ---- the size check ----

    static Screen scanning;
    static String scanName = "";
    static List<UiSizes.Size> sizes = List.of();
    static int at, frames;
    /** Runs when a check ends (the tour's next screen), or null. */
    static Runnable then;
    /** The screens still to check on a tour, and the screen to come back to. */
    static final Deque<UiEditorScreen.Entry> TOUR = new ArrayDeque<>();
    static Screen back;
    static int tourTotal;

    public static boolean scanning() { return scanning != null; }

    static String progress() {
        String tour = tourTotal > 0 ? "Screen " + (tourTotal - TOUR.size()) + " of " + tourTotal + ", " : "";
        return tour + "checking " + scanName + " at " + (at < sizes.size() ? sizes.get(at).label() : "") + " (" + Math.min(at + 1, sizes.size()) + " of " + sizes.size() + ")";
    }

    /** A name for a screen in the warnings list. */
    static String name(Screen s) {
        if (s instanceof CampScreen c) return "War Table: " + c.page;
        String t = s.getTitle().getString();
        return t.isBlank() ? s.getClass().getSimpleName() : t;
    }

    /** Walks screen s through every check size; its earlier findings are replaced. */
    static void start(Screen s, String name, Runnable after) {
        scanning = s; scanName = name; then = after;
        sizes = UiPreview.checkSizes(); at = 0; frames = 0;
        FOUND.put(name, new TreeMap<>());
        MinecraftClient.getInstance().send(() -> { if (scanning == s) UiPreview.force(sizes.get(0)); });
    }

    /** After each frame of an editable screen in edit mode: on a check, keeps what this size showed and moves to the next size. */
    static void frame(Screen s) {
        if (scanning == null || s != scanning) return;
        if (UiPreview.size() != sizes.get(at)) return; // the size is not in force yet
        if (++frames < 3) return; // a frame for the layout to settle (pages that build on their first frame)
        Map<String, Map<String, Set<String>>> found = FOUND.computeIfAbsent(scanName, k -> new TreeMap<>());
        for (var e : NOW.entrySet()) found.computeIfAbsent(e.getKey(), k -> new LinkedHashMap<>()).computeIfAbsent(e.getValue(), k -> new LinkedHashSet<>()).add(sizes.get(at).label());
        frames = 0;
        if (++at < sizes.size()) { UiSizes.Size next = sizes.get(at); MinecraftClient.getInstance().send(() -> { if (scanning == s) UiPreview.force(next); }); }
        else finish();
    }

    /** Called every client tick: a check whose screen went away (it closed itself, or the player left it) ends there. */
    static void tick(MinecraftClient c) {
        if (scanning != null && c.currentScreen != scanning) { TOUR.clear(); tourTotal = 0; back = null; then = null; finish(); }
    }

    static void finish() {
        Runnable after = then;
        scanning = null; then = null;
        if (FOUND.containsKey(scanName) && FOUND.get(scanName).isEmpty()) FOUND.remove(scanName);
        MinecraftClient.getInstance().send(() -> { UiPreview.apply(); if (after != null) after.run(); });
    }

    /** Ends a check or a tour where it is (the editor closed, or Check sizes clicked again). */
    static void stop() {
        TOUR.clear(); tourTotal = 0; back = null;
        if (scanning != null) { scanning = null; then = null; MinecraftClient.getInstance().send(UiPreview::apply); }
    }

    /** Checks every screen the browser can open, one after another, then comes back to the editor. */
    static void tour(List<UiEditorScreen.Entry> entries, Screen backTo) {
        stop();
        for (var e : entries) if (e.make() != null) TOUR.add(e);
        tourTotal = TOUR.size(); back = backTo;
        next();
    }

    static void next() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (TOUR.isEmpty()) {
            Screen to = back; tourTotal = 0; back = null;
            UiEditorClient.message = "Size check done: " + count() + " warning(s).";
            c.setScreen(new UiEditorScreen(to, "warnings"));
            return;
        }
        UiEditorScreen.Entry e = TOUR.poll();
        Screen s;
        try { s = e.make().get(); } catch (RuntimeException bad) { next(); return; }
        c.setScreen(s);
        if (c.currentScreen == s && UiEditorClient.editable(s)) start(s, e.name(), UiWarnings::next);
        else c.send(UiWarnings::next);
    }

    /** How many (screen, line, problem) findings the checks hold. */
    static int count() {
        int n = 0;
        for (var screen : FOUND.values()) for (var line : screen.values()) n += line.size();
        return n;
    }

    /** The findings as lines for the editor: "screen  line: problem at sizes". */
    static List<String> lines() {
        List<String> out = new ArrayList<>();
        for (var screen : FOUND.entrySet())
            for (var line : screen.getValue().entrySet())
                for (var why : line.getValue().entrySet())
                    out.add(screen.getKey() + "  " + line.getKey() + ": " + why.getKey() + " at " + String.join(", ", why.getValue()));
        return out;
    }
}
