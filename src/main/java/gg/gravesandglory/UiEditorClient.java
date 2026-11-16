package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.gravesandglory.net.Payloads;
import io.netty.buffer.Unpooled;
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

    /** A line located on screen this frame: its piece and its box in GUI pixels. */
    record Box(String id, float x0, float y0, float x1, float y1) {
        boolean contains(double x, double y) { return x >= x0 - 1 && x <= x1 + 1 && y >= y0 - 1 && y <= y1 + 1; }
    }
    static final List<Box> BOXES = new ArrayList<>();
    static final List<Box> LAST = new ArrayList<>();

    static final int BAR_H = 14;
    static final String[] BAR = {"Editor", "Undo", "Redo", "Exit editor"};

    public static void register() {
        UiText.setBuiltin(UiEdits.builtin());
        ClientPlayNetworking.registerGlobalReceiver(Payloads.UiSheetPayload.ID, (payload, context) -> {
            PacketByteBuf buf = payload.buf();
            byte kind = buf.readByte();
            String json = new String(buf.readByteArray(UiEdits.SEND_CAP), StandardCharsets.UTF_8);
            context.client().execute(() -> receive(context.client(), kind, json));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(UiEditorClient::leave));
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!editable(screen)) return;
            ScreenEvents.beforeRender(screen).register((s, g, mx, my, d) -> { if (active) { UiText.nextFrame(); BOXES.clear(); } });
            ScreenEvents.afterRender(screen).register((s, g, mx, my, d) -> { if (active) overlay(s, g, mx, my); });
            ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, button) -> !active || click(s, mx, my, button));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, mx, my, button) -> !active);
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scan, mods) -> !active || key == GLFW.GLFW_KEY_ESCAPE);
        });
        HudRenderCallback.EVENT.register((g, tick) -> {
            if (!active || MinecraftClient.getInstance().currentScreen != null) return;
            UiText.nextFrame();
            BOXES.clear();
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

    static void request(JsonObject o) {
        if (!ClientPlayNetworking.canSend(Payloads.UiEditPayload.ID)) { message = "This server has no UI editor."; return; }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBytes(o.toString().getBytes(StandardCharsets.UTF_8));
        ClientPlayNetworking.send(Payloads.UiEditPayload.of(buf));
    }

    static void op(String op) { JsonObject o = new JsonObject(); o.addProperty("op", op); request(o); }
    static void rollback(int version) { JsonObject o = new JsonObject(); o.addProperty("op", "rollback"); o.addProperty("version", version); request(o); }

    /** Sets (text not null) or clears a piece in the draft, recording the step for undo. */
    static void set(String id, String text, boolean record) {
        if (record) { UNDO.push(new String[] {id, UiText.draft().get(id), text}); REDO.clear(); }
        JsonObject o = new JsonObject();
        o.addProperty("op", text == null ? "clear" : "set");
        o.addProperty("id", id);
        if (text != null) o.addProperty("text", text);
        request(o);
    }

    static void undo() { if (UNDO.isEmpty()) { message = "Nothing to undo."; return; } String[] s = UNDO.pop(); REDO.push(s); set(s[0], s[1], false); }
    static void redo() { if (REDO.isEmpty()) { message = "Nothing to redo."; return; } String[] s = REDO.pop(); UNDO.push(s); set(s[0], s[2], false); }

    static void on(boolean on) {
        active = on;
        UiText.previewDraft(on);
        UiText.recording(on);
        if (!on) { UNDO.clear(); REDO.clear(); BOXES.clear(); LAST.clear(); }
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
        if (BOXES.size() < 4000) BOXES.add(new Box(id, Math.min(a.x, b.x), Math.min(a.y, b.y), Math.max(a.x, b.x), Math.max(a.y, b.y)));
    }

    /** The piece that gave out this text: an exact match first, then a trimmed one ("Long wor..."), then a wrapped line inside it. */
    static String idFor(String text) {
        String trimmed = text.endsWith("...") ? text.substring(0, text.length() - 3) : text.endsWith("…") ? text.substring(0, text.length() - 1) : null;
        String best = null; long bestFrame = -1; int rank = 9;
        for (UiText.Seen s : UiText.seen(Long.MAX_VALUE)) {
            int r = s.shown.equals(text) ? 0 : trimmed != null && !trimmed.isEmpty() && s.shown.startsWith(trimmed) ? 1 : text.length() >= 6 && s.shown.contains(text) ? 2 : 9;
            if (r < rank || r == rank && r < 9 && s.frame > bestFrame) { rank = r; best = s.id; bestFrame = s.frame; }
        }
        return rank < 9 ? best : null;
    }

    // ---- the overlay ----

    static void overlay(Screen s, DrawContext g, int mx, int my) {
        LAST.clear(); LAST.addAll(BOXES);
        var tr = MinecraftClient.getInstance().textRenderer;
        g.getMatrices().push();
        g.getMatrices().translate(0, 0, 400);
        Box hover = hit(mx, my);
        for (Box b : LAST) {
            boolean edited = UiText.draft().containsKey(b.id());
            int c = hover != null && hover.id().equals(b.id()) ? 0xffffd75a : edited ? 0xc06fd16f : 0x80e8d8a8;
            outline(g, b, c);
        }
        int w = s.width;
        g.fill(0, 0, w, BAR_H, 0xe0201810);
        int x = 4;
        g.drawText(tr, "UI EDITOR", x, 3, 0xffffd75a, false);
        x += tr.getWidth("UI EDITOR") + 10;
        for (String label : BAR) {
            int bw = tr.getWidth(label) + 8;
            boolean over = my >= 0 && my < BAR_H && mx >= x && mx < x + bw;
            g.fill(x, 1, x + bw, BAR_H - 1, over ? 0xff6b5530 : 0xff3a2e1c);
            g.drawText(tr, label, x + 4, 3, 0xffe8d8a8, false);
            x += bw + 4;
        }
        String tip = hover != null ? hover.id() + "  (" + UiText.source(hover.id()) + ")" : message;
        if (tip != null && !tip.isEmpty()) g.drawText(tr, tr.trimToWidth(tip, Math.max(0, w - x - 8)), x + 6, 3, 0xffb8c8a0, false);
        g.getMatrices().pop();
    }

    static void outline(DrawContext g, Box b, int c) {
        int x0 = (int) Math.floor(b.x0()) - 1, y0 = (int) Math.floor(b.y0()) - 1, x1 = (int) Math.ceil(b.x1()) + 1, y1 = (int) Math.ceil(b.y1()) + 1;
        g.fill(x0, y0, x1, y0 + 1, c); g.fill(x0, y1 - 1, x1, y1, c); g.fill(x0, y0, x0 + 1, y1, c); g.fill(x1 - 1, y0, x1, y1, c);
    }

    /** The smallest located line under the pointer. */
    static Box hit(double mx, double my) {
        Box best = null; float area = Float.MAX_VALUE;
        for (Box b : LAST) if (b.contains(mx, my)) { float a = (b.x1() - b.x0()) * (b.y1() - b.y0()); if (a < area) { area = a; best = b; } }
        return best;
    }

    /** A click while the editor is on: the strip's buttons, or a line. Always swallowed (returns false). */
    static boolean click(Screen s, double mx, double my, int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (my < BAR_H) {
            var tr = client.textRenderer;
            int x = 4 + tr.getWidth("UI EDITOR") + 10;
            for (String label : BAR) {
                int bw = tr.getWidth(label) + 8;
                if (mx >= x && mx < x + bw) {
                    switch (label) {
                        case "Editor" -> client.setScreen(new UiEditorScreen(s));
                        case "Undo" -> undo();
                        case "Redo" -> redo();
                        default -> on(false);
                    }
                    return false;
                }
                x += bw + 4;
            }
            return false;
        }
        Box b = hit(mx, my);
        if (b != null && button == 0) {
            UiText.Seen seen = UiText.seen(b.id());
            if (seen != null) client.setScreen(new UiEditScreen(s, seen));
        }
        return false;
    }
}
