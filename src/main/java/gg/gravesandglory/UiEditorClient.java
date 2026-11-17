package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.gravesandglory.net.Payloads;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * The client half of the UI editor (UI editor design 5.4 and 5.6, phase 1: words). Every client: takes the server's published sheet
 * into UiText (and drops it on leaving, so the main menu shows the built-in edits only). An owner, after /gg ui edit: the editor is on.
 * While it is on, his own draft is drawn in place of the published sheet, every mod screen gets an outline round each line it draws
 * through UiText and a strip at the top, a click on a line opens UiEditScreen to retype it, and the screen's own buttons and keys do
 * nothing (Esc still closes). The battle HUD's lines are reached from the editor's piece list (UiEditorScreen).
 *
 * <p>Phase 2: the strip's Size and GUI buttons preview the screen at another window size and GUI scale (UiPreview); every frame the
 * located lines are checked for text cut short or clipped, lines off the screen and lines running into each other or into a button
 * (UiWarnings), shown by a warm outline on the line and a count in the strip; Check sizes walks the screen through every common size.
 */
public final class UiEditorClient {
    private UiEditorClient() {}

    /** The editor is on (the server said open, and the player has not left it). */
    static boolean active;
    public static boolean active() { return active; }
    /** The last state the server sent: draft, history and messages. */
    static JsonObject state = new JsonObject();
    static String message = "";
    /** Undo and redo within the draft: {id, before, after}, with null for "no edit". */
    static final Deque<String[]> UNDO = new ArrayDeque<>(), REDO = new ArrayDeque<>();

    /** A line located on screen this frame: its piece, its box in the real window's GUI pixels, whether it was drawn cut short
     *  ("Long wor..."), and the left and right edges of the scissor box it was drawn in (none: infinite). */
    record Box(String id, float x0, float y0, float x1, float y1, boolean trimmed, float clipL, float clipR, float clipT, float clipB) {
        boolean contains(double x, double y) { return x >= x0 - 1 && x <= x1 + 1 && y >= y0 - 1 && y <= y1 + 1; }
    }
    static final List<Box> BOXES = new ArrayList<>();
    static final List<Box> LAST = new ArrayList<>();
    /** This frame's button boxes {x0, y0, x1, y1} in real GUI pixels, and the scissor boxes open now (left and right edges). */
    static final List<float[]> WIDGETS = new ArrayList<>();
    static final Deque<float[]> CLIP = new ArrayDeque<>();
    /** Whether this frame's render pushed the preview's stretch (and opened a preview scope). */
    static boolean stretching, ticking;

    static final int BAR_H = 14;
    static final String CHECK = "Check sizes";

    /** The strip's buttons, left to right (the Size and GUI buttons name what they show). */
    static List<String> bar() { return List.of("Editor", "Undo", "Redo", UiPreview.resLabel(), UiPreview.guiLabel(), CHECK, "Exit editor"); }

    public static void register() {
        UiText.setBuiltin(UiEdits.builtin());
        ClientPlayNetworking.registerGlobalReceiver(Payloads.UiSheetPayload.ID, (payload, context) -> {
            try {
                PacketByteBuf buf = payload.buf();
                byte kind = buf.readByte();
                String json = new String(buf.readByteArray(UiEdits.SEND_CAP), StandardCharsets.UTF_8);
                context.client().execute(() -> receive(context.client(), kind, json));
            } catch (RuntimeException bad) {
                // a malformed or oversized body from the server: ignored, the wording stays as it was
                SafeFiles.warn.accept("UI editor message from the server could not be read (" + bad.getMessage() + "); ignored.");
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(UiEditorClient::leave));
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!editable(screen)) return;
            // laid out at the real size (opened, or the window resized) while a size is previewed: again at the preview's, before the next frame
            if (UiPreview.previewing(screen) && (w != UiPreview.w() || h != UiPreview.h())) client.send(() -> UiPreview.relayout(screen));
            ScreenEvents.beforeRender(screen).register((s, g, mx, my, d) -> {
                UiPreview.frameStart();
                stretching = false;
                if (!active) return;
                UiText.nextFrame(); BOXES.clear(); WIDGETS.clear(); CLIP.clear();
                if (UiPreview.previewing(s) && UiPreview.enter()) {
                    stretching = true;
                    g.getMatrices().push();
                    g.getMatrices().scale(UiPreview.sx(), UiPreview.sy(), 1);
                }
            });
            ScreenEvents.afterRender(screen).register((s, g, mx, my, d) -> {
                if (stretching) { g.getMatrices().pop(); UiPreview.exit(); stretching = false; }
                if (active) { overlay(s, g); UiWarnings.frame(s); }
            });
            ScreenEvents.beforeTick(screen).register(s -> { ticking = UiPreview.previewing(s) && UiPreview.enter(); });
            ScreenEvents.afterTick(screen).register(s -> { if (ticking) UiPreview.exit(); ticking = false; });
            ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, button) -> !active || click(s, mx, my, button));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, mx, my, button) -> !active);
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scan, mods) -> !active || key == GLFW.GLFW_KEY_ESCAPE);
        });
        ClientTickEvents.END_CLIENT_TICK.register(UiWarnings::tick);
        HudRenderCallback.EVENT.register((g, tick) -> {
            if (!active || MinecraftClient.getInstance().currentScreen != null) return;
            UiText.nextFrame();
            BOXES.clear(); WIDGETS.clear(); CLIP.clear();
            var tr = MinecraftClient.getInstance().textRenderer;
            g.fill(0, 0, tr.getWidth(HUD_NOTE) + 8, 12, 0xc0201810);
            g.drawText(tr, HUD_NOTE, 4, 2, 0xffe8d8a8, false);
        });
    }
    static final String HUD_NOTE = "UI editor on: /gg ui edit lists the HUD's lines";

    /** Mod screens only, and never the editor's own. */
    static boolean editable(Screen s) {
        return s != null && s.getClass().getName().startsWith("gg.gravesandglory.") && !(s instanceof UiEditorScreen) && !(s instanceof UiEditScreen);
    }

    // ---- network ----

    static void receive(MinecraftClient client, byte kind, String json) {
        try {
            if (kind == UiEdits.KIND_SHEET) {
                UiText.setServer(UiSheet.renamed(UiSheet.parse(json).edits, UiEdits.renames()));
            } else if (kind == UiEdits.KIND_EDITOR) {
                JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                state = o;
                message = UiSheet.str(o, "message");
                JsonObject draft = new JsonObject();
                if (o.has("draft")) draft.add("edits", o.get("draft")); // the owner's draft: id to {text}, as in a sheet's edits
                UiText.setDraft(UiSheet.editsOf(draft, "draft: "));
                if (o.has("seq")) { long acked = o.get("seq").getAsLong(); PENDING.removeIf(p -> p.seq() <= acked); } // these are in the draft now (or were refused)
                else PENDING.clear(); // a server that echoes no seq, or a request dropped as too fast: the draft sent is the truth
                if (o.has("open") && o.get("open").getAsBoolean()) {
                    on(true);
                    client.setScreen(new UiEditorScreen(client.currentScreen instanceof UiEditorScreen e ? e.parent : client.currentScreen));
                }
                if (client.currentScreen instanceof UiEditorScreen e) e.refresh();
            }
        } catch (RuntimeException bad) {
            // an untrusted sheet that does not parse: keep what we had (the built-in edits at worst)
            SafeFiles.warn.accept("UI sheet from the server could not be read (" + bad.getMessage() + "); ignored.");
        }
    }

    /** Requests are numbered; the server echoes the number with the draft that includes it. */
    static long seq;
    /** A set or clear sent but not yet in a draft the server sent back (text null: clear). */
    record Pending(long seq, String id, String text) {}
    static final List<Pending> PENDING = new ArrayList<>();

    /** The draft's edit for id as it will be once the server has every request sent so far: the last draft it sent, with the sets
     *  and clears still on their way on top. Undo records this as the step's "before", so two quick edits of one line, or an undo
     *  sent before the last edit's reply, never record a stale value. */
    static String viewed(String id) {
        String v = UiText.draft().get(id);
        for (Pending p : PENDING) if (p.id().equals(id)) v = p.text();
        return v;
    }

    static boolean request(JsonObject o) {
        if (!ClientPlayNetworking.canSend(Payloads.UiEditPayload.ID)) { message = "This server has no UI editor."; return false; }
        o.addProperty("seq", ++seq);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBytes(o.toString().getBytes(StandardCharsets.UTF_8));
        ClientPlayNetworking.send(Payloads.UiEditPayload.of(buf));
        return true;
    }

    static void op(String op) { JsonObject o = new JsonObject(); o.addProperty("op", op); request(o); }
    static void rollback(int version) { JsonObject o = new JsonObject(); o.addProperty("op", "rollback"); o.addProperty("version", version); request(o); }

    /** Sets (text not null) or clears a piece in the draft, recording the step for undo. */
    static void set(String id, String text, boolean record) {
        String before = viewed(id);
        JsonObject o = new JsonObject();
        o.addProperty("op", text == null ? "clear" : "set");
        o.addProperty("id", id);
        if (text != null) o.addProperty("text", text);
        if (!request(o)) return;
        if (record) { UNDO.push(new String[] {id, before, text}); REDO.clear(); }
        if (PENDING.size() > 500) PENDING.remove(0); // requests that never get a reply (a lost owner gate) do not pile up
        PENDING.add(new Pending(seq, id, text));
    }

    static void undo() { if (UNDO.isEmpty()) { message = "Nothing to undo."; return; } String[] s = UNDO.pop(); REDO.push(s); set(s[0], s[1], false); }
    static void redo() { if (REDO.isEmpty()) { message = "Nothing to redo."; return; } String[] s = REDO.pop(); UNDO.push(s); set(s[0], s[2], false); }

    static void on(boolean on) {
        active = on;
        UiText.previewDraft(on);
        UiText.recording(on);
        if (!on) { UNDO.clear(); REDO.clear(); PENDING.clear(); BOXES.clear(); LAST.clear(); UiWarnings.stop(); UiPreview.reset(); }
    }

    static void leave() { on(false); UiText.setServer(Map.of()); UiText.setDraft(Map.of()); state = new JsonObject(); message = ""; }

    // ---- locating lines (called from UiTextLocateMixin while the editor is on) ----

    /** A string drawn at (x, y) in the context's current matrix: if it is a line UiText gave out, its box is kept for clicks. */
    public static void drawn(DrawContext g, TextRenderer tr, String text, int x, int y) {
        if (!active || text == null || text.isBlank()) return;
        String id = idFor(text);
        if (id == null) return;
        Matrix4f m = g.getMatrices().peek().getPositionMatrix();
        Vector3f a = m.transformPosition(new Vector3f(x, y - 1, 0)), b = m.transformPosition(new Vector3f(x + tr.getWidth(text), y + tr.fontHeight, 0));
        float[] clip = CLIP.peekLast();
        if (BOXES.size() < 4000) BOXES.add(new Box(id, Math.min(a.x, b.x), Math.min(a.y, b.y), Math.max(a.x, b.x), Math.max(a.y, b.y), lastRank == 1,
            clip == null ? Float.NEGATIVE_INFINITY : clip[0], clip == null ? Float.POSITIVE_INFINITY : clip[1],
            clip == null ? Float.NEGATIVE_INFINITY : clip[2], clip == null ? Float.POSITIVE_INFINITY : clip[3]));
    }

    /** How the last idFor matched: 0 exactly, 1 cut short, 2 a wrapped line inside the piece. */
    static int lastRank = 9;

    /** The piece that gave out this text: an exact match first, then a trimmed one ("Long wor..."), then a wrapped line inside it. */
    static String idFor(String text) {
        String trimmed = text.endsWith("...") ? text.substring(0, text.length() - 3) : text.endsWith("…") ? text.substring(0, text.length() - 1) : null;
        String best = null; long bestFrame = -1; int rank = 9;
        for (UiText.Seen s : UiText.seen(Long.MAX_VALUE)) {
            int r = s.shown.equals(text) ? 0 : trimmed != null && !trimmed.isEmpty() && s.shown.startsWith(trimmed) ? 1 : text.length() >= 6 && s.shown.contains(text) ? 2 : 9;
            if (r < rank || r == rank && r < 9 && s.frame > bestFrame) { rank = r; best = s.id; bestFrame = s.frame; }
        }
        lastRank = rank;
        return rank < 9 ? best : null;
    }

    /** A scissor box opened (DrawContext.enableScissor, in the screen's GUI pixels, which the scissor does not run through the
     *  matrix): kept in real GUI pixels while it is open. */
    public static void clipPush(int x0, int y0, int x1, int y1) {
        float k = UiPreview.scoped() ? UiPreview.sx() : 1, ky = UiPreview.scoped() ? UiPreview.sy() : 1;
        if (CLIP.size() < 64) CLIP.addLast(new float[] {x0 * k, x1 * k, y0 * ky, y1 * ky}); else CLIP.addLast(CLIP.peekLast());
    }

    public static void clipPop() { CLIP.pollLast(); }

    /** A button drawn at (x, y, w, h) in the context's current matrix: its box is kept for the overlap warning. */
    public static void widget(DrawContext g, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0 || WIDGETS.size() >= 400) return;
        Matrix4f m = g.getMatrices().peek().getPositionMatrix();
        Vector3f a = m.transformPosition(new Vector3f(x, y, 0)), b = m.transformPosition(new Vector3f(x + w, y + h, 0));
        float y0 = Math.min(a.y, b.y), y1 = Math.max(a.y, b.y);
        float[] clip = CLIP.peekLast();
        if (clip != null && (y1 <= clip[2] || y0 >= clip[3])) return; // scrolled out of its list: not on screen
        WIDGETS.add(new float[] {Math.min(a.x, b.x), y0, Math.max(a.x, b.x), y1});
    }

    /** The real window's GUI size and pointer (outside any preview scope). */
    static int realW() { return MinecraftClient.getInstance().getWindow().getScaledWidth(); }
    static int realH() { return MinecraftClient.getInstance().getWindow().getScaledHeight(); }
    static double realMx() { var c = MinecraftClient.getInstance(); return c.mouse.getX() * realW() / Math.max(1, c.getWindow().getWidth()); }
    static double realMy() { var c = MinecraftClient.getInstance(); return c.mouse.getY() * realH() / Math.max(1, c.getWindow().getHeight()); }

    // ---- the overlay ----

    /** Line outlines: plain, edited in the draft, warned about (a warm rust, no louder than the others), under the pointer. */
    static final int PLAIN = 0xd0e8d8a8, EDITED = 0xc06fd16f, WARNED = 0xd0e0905a, HOVER = 0xffffd75a;

    /** Drawn after the screen, in the real window's GUI pixels (a previewed screen's stretch is already undone). */
    static void overlay(Screen s, DrawContext g) {
        LAST.clear(); LAST.addAll(BOXES);
        UiWarnings.compute(LAST, WIDGETS, realW(), realH());
        var tr = MinecraftClient.getInstance().textRenderer;
        double mx = realMx(), my = realMy();
        g.getMatrices().push();
        g.getMatrices().translate(0, 0, 400);
        Box hover = hit(mx, my);
        for (Box b : LAST) {
            boolean edited = UiText.draft().containsKey(b.id());
            int c = hover != null && hover.id().equals(b.id()) ? HOVER : UiWarnings.NOW.containsKey(b.id()) ? WARNED : edited ? EDITED : PLAIN;
            outline(g, b, c);
        }
        int w = realW(), top = barTop();
        g.fill(0, top, w, top + BAR_H, 0xe0201810);
        int x = 4;
        g.drawText(tr, "UI EDITOR", x, top + 3, 0xffffd75a, false);
        x += tr.getWidth("UI EDITOR") + 10;
        for (String label : bar()) {
            int bw = tr.getWidth(label) + 8;
            boolean over = my >= top && my < top + BAR_H && mx >= x && mx < x + bw;
            boolean lit = label.equals(UiPreview.resLabel()) && UiPreview.on() || label.equals(UiPreview.guiLabel()) && UiPreview.on() || label.equals(CHECK) && UiWarnings.scanning();
            g.fill(x, top + 1, x + bw, top + BAR_H - 1, over ? 0xff6b5530 : lit ? 0xff4e3d22 : 0xff3a2e1c);
            g.drawText(tr, label, x + 4, top + 3, lit ? 0xffffd75a : 0xffe8d8a8, false);
            x += bw + 4;
        }
        String tip;
        if (hover != null) {
            String why = UiWarnings.NOW.get(hover.id());
            tip = hover.id() + "  (" + UiText.source(hover.id()) + ")" + (why == null ? "" : "  " + why);
        } else if (UiWarnings.scanning()) tip = UiWarnings.progress();
        else {
            int n = UiWarnings.NOW.size();
            String size = UiPreview.on() ? UiPreview.describe(UiPreview.size()) + (UiPreview.stretched() ? ", stretched to fit" : "") + ". " : "";
            tip = size + (n == 0 ? "" : n + (n == 1 ? " line warned about" : " lines warned about") + " (rust outline). ") + message;
        }
        if (!tip.isEmpty()) g.drawText(tr, tr.trimToWidth(tip, Math.max(0, w - x - 8)), x + 6, top + 3, hover != null && UiWarnings.NOW.containsKey(hover.id()) ? 0xffe0a070 : 0xffb8c8a0, false);
        g.getMatrices().pop();
    }

    /** The strip sits on the real window's bottom edge: the War Table's tab bar and page titles are on the top one. */
    static int barTop() { return realH() - BAR_H; }
    static int barTop(Screen s) { return barTop(); }

    /** A dark ring round a light one, so the box shows on dark wood and on light parchment or vellum alike. */
    static void outline(DrawContext g, Box b, int c) {
        int x0 = (int) Math.floor(b.x0()) - 1, y0 = (int) Math.floor(b.y0()) - 1, x1 = (int) Math.ceil(b.x1()) + 1, y1 = (int) Math.ceil(b.y1()) + 1;
        ring(g, x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xc0201810);
        ring(g, x0, y0, x1, y1, c);
    }

    static void ring(DrawContext g, int x0, int y0, int x1, int y1, int c) {
        g.fill(x0, y0, x1, y0 + 1, c); g.fill(x0, y1 - 1, x1, y1, c); g.fill(x0, y0, x0 + 1, y1, c); g.fill(x1 - 1, y0, x1, y1, c);
    }

    /** The smallest located line under the pointer. */
    static Box hit(double mx, double my) {
        Box best = null; float area = Float.MAX_VALUE;
        for (Box b : LAST) if (b.contains(mx, my)) { float a = (b.x1() - b.x0()) * (b.y1() - b.y0()); if (a < area) { area = a; best = b; } }
        return best;
    }

    /** A click while the editor is on: the strip's buttons, or a line. Always swallowed (returns false). The click arrives in the
     *  screen's own GUI pixels (a previewed screen's); the strip and the outlines are in the real window's, so the pointer is read
     *  again there. Acts on the next frame, outside the screen's mouse handling. */
    static boolean click(Screen s, double sx, double sy, int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> act(s, button));
        return false;
    }

    static void act(Screen s, int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != s || !active) return;
        double mx = realMx(), my = realMy();
        if (my >= barTop()) {
            var tr = client.textRenderer;
            int x = 4 + tr.getWidth("UI EDITOR") + 10;
            for (String label : bar()) {
                int bw = tr.getWidth(label) + 8;
                if (mx >= x && mx < x + bw) {
                    if (UiWarnings.scanning() && !label.equals(CHECK)) { message = "Checking sizes: wait for it to finish, or click Check sizes to stop."; return; }
                    switch (label) {
                        case "Editor" -> client.setScreen(new UiEditorScreen(s));
                        case "Undo" -> undo();
                        case "Redo" -> redo();
                        case CHECK -> { if (UiWarnings.scanning()) UiWarnings.stop(); else UiWarnings.start(s, UiWarnings.name(s), null); }
                        case "Exit editor" -> on(false);
                        default -> { if (label.startsWith("Size")) UiPreview.nextRes(); else UiPreview.nextGui(); }
                    }
                    return;
                }
                x += bw + 4;
            }
            return;
        }
        if (UiWarnings.scanning()) return;
        Box b = hit(mx, my);
        if (b != null && button == 0) {
            UiText.Seen seen = UiText.seen(b.id());
            if (seen != null) client.setScreen(new UiEditScreen(s, seen));
        }
    }
}
