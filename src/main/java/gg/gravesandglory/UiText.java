package gg.gravesandglory;

import java.util.*;

/**
 * The one gate every editable line of mod UI goes through (UI editor design 5.1, phase 1: words). A call site names its piece and
 * gives the default wording inline, with its live values as slots:
 * <pre>  UiText.t("hud.wave.counter", "Wave {wave} of {total}", "wave", w, "total", n)</pre>
 * The id and the default must be string literals and the slot names too: tools/ui_pieces.py reads them from the source into
 * ui/pieces.json, which the server validates edits against, and fails the build on a duplicate id or a slot the call does not supply.
 * A row drawn in a loop is one piece (one call site): editing it edits every row.
 *
 * <p>Wording comes from, last one wins: the default at the call site, the mod's built-in edits, the server's published sheet (or,
 * for an owner previewing, his draft in its place). Every layer is treated as untrusted: an edit that names a slot the call does not
 * supply, or breaks any other rule (UiSheet.problem), is ignored and the default drawn.
 *
 * <p>{@link #locked} marks text that is drawn as a piece but is not edited here (design-sheet names, server text), so the editor can
 * list it with where it comes from. Pure: no Minecraft classes.
 */
public final class UiText {
    private UiText() {}

    private static volatile Map<String, String> builtin = Map.of(), server = Map.of(), draft = Map.of();
    private static volatile boolean previewDraft;
    /** Bumped on every layer change, so cached checks are redone. */
    private static volatile int generation;

    /** What the last check said about an id's edit: the layer generation it was made in, the slot names the call supplied (every
     *  other array entry of the call's slots, names only), and the template to use (null: the default). Every layer change bumps the
     *  generation, so the layers' strings need no key of their own; the hit path below allocates nothing. */
    private record Checked(int generation, Object[] slots, String use) {
        boolean sameSlots(Object[] other) {
            int n = other == null ? 0 : other.length, m = slots.length;
            if (n / 2 != m) return false;
            for (int i = 0; i < m; i++) if (!slots[i].equals(other[i * 2])) return false;
            return true;
        }
    }
    private static final Map<String, Checked> CHECKED = new HashMap<>();

    /** A piece drawn recently, for the editor's list and its click targets. */
    public static final class Seen {
        public final String id;
        public String def = "", shown = "", source = "";
        public Set<String> slots = Set.of();
        /** The live value each slot had when the piece was last drawn, as drawn (the editor's value chips). */
        public Map<String, String> values = Map.of();
        public boolean keys, locked;
        public long frame;
        Seen(String id) { this.id = id; }
    }
    /** Pieces drawn, by id, while recording (the editor is on). Read and written on the render thread only. */
    private static final Map<String, Seen> SEEN = new LinkedHashMap<>();
    private static volatile boolean recording;
    private static long frame;

    // ---- the gate ----

    /** The wording to draw for piece id: its current template with the slots filled. */
    public static String t(String id, String def, Object... slots) {
        String template = template(id, def, slots);
        String out = UiTemplate.expand(template, slots);
        if (recording) record(id, def, out, slots, false, null);
        return out;
    }

    /** Text drawn as a piece but not editable here (a design-sheet name, server text): returned as it is, listed as locked with its source. */
    public static String locked(String id, String text, String source) {
        if (recording) record(id, "", text, null, true, source);
        return text;
    }

    /** The template that wins for this id: the draft or published edit if it passes the client's check, else the built-in edit if
     *  it does, else the default. */
    public static String template(String id, String def, Object... slots) {
        int gen = generation; // read before the layers, so a layer change in between is never cached under the new generation
        String top = previewDraft ? draft.get(id) : server.get(id), base = builtin.get(id);
        if (top == null && base == null) return def;
        Checked c;
        synchronized (CHECKED) { c = CHECKED.get(id); }
        if (c != null && c.generation == gen && c.sameSlots(slots)) return c.use == null ? def : c.use;
        UiSheet.Piece piece = new UiSheet.Piece(id, def, UiTemplate.supplied(slots), UiTemplate.hasKeyHint(def), "");
        String use = top == null ? null : UiSheet.usable(piece, top);
        if (use == null && base != null) use = UiSheet.usable(piece, base);
        synchronized (CHECKED) { CHECKED.put(id, new Checked(gen, names(slots), use)); }
        return use == null ? def : use;
    }

    /** The slot names of a call's slots (name, value, name, value...). */
    static Object[] names(Object[] slots) {
        if (slots == null) return new Object[0];
        Object[] out = new Object[slots.length / 2];
        for (int i = 0; i < out.length; i++) out[i] = String.valueOf(slots[i * 2]);
        return out;
    }

    /** The edited template for an id from the layers (draft or server, then built-in), or null for none. */
    public static String edit(String id) {
        String e = previewDraft ? draft.get(id) : server.get(id);
        return e != null ? e : builtin.get(id);
    }

    /** Where the wording an id shows comes from: "draft", "published", "built-in" or "code". */
    public static String source(String id) {
        if (previewDraft && draft.containsKey(id)) return "draft";
        if (!previewDraft && server.containsKey(id)) return "published";
        return builtin.containsKey(id) ? "built-in" : "code";
    }

    // ---- layers ----

    public static void setBuiltin(Map<String, String> edits) { builtin = Map.copyOf(edits); generation++; }
    public static void setServer(Map<String, String> edits) { server = Map.copyOf(edits); generation++; }
    public static void setDraft(Map<String, String> edits) { draft = Map.copyOf(edits); generation++; }
    public static void previewDraft(boolean on) { previewDraft = on; generation++; }
    public static boolean previewingDraft() { return previewDraft; }
    public static Map<String, String> draft() { return draft; }
    public static Map<String, String> server() { return server; }
    public static Map<String, String> builtin() { return builtin; }

    // ---- what the editor sees ----

    public static void recording(boolean on) { recording = on; if (!on) SEEN.clear(); }
    public static boolean recording() { return recording; }
    /** Starts a frame: pieces drawn from now on are this frame's. */
    public static void nextFrame() { frame++; }
    public static long frame() { return frame; }

    static void record(String id, String def, String shown, Object[] slots, boolean locked, String source) {
        Seen s = SEEN.get(id);
        if (s == null) {
            if (SEEN.size() > 2000) SEEN.clear();
            s = new Seen(id);
            SEEN.put(id, s);
        }
        s.def = def; s.shown = shown; s.frame = frame; s.locked = locked;
        s.source = locked ? source : "";
        if (slots != null) { s.slots = UiTemplate.supplied(slots); s.keys = UiTemplate.hasKeyHint(def); s.values = values(slots); }
    }

    /** A call's slots as name to value, in the call's order; a value is shown as the template would draw it. */
    static Map<String, String> values(Object[] slots) {
        if (slots.length < 2) return Map.of();
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < slots.length; i += 2) out.put(String.valueOf(slots[i]), String.valueOf(slots[i + 1]));
        return out;
    }

    /** The pieces drawn within the last few frames, newest last. */
    public static List<Seen> seen(long frames) {
        List<Seen> out = new ArrayList<>();
        for (Seen s : SEEN.values()) if (frame - s.frame <= frames) out.add(s);
        return out;
    }

    public static Seen seen(String id) { return SEEN.get(id); }
}
