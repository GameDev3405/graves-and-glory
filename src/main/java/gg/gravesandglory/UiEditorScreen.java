package gg.gravesandglory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.Supplier;

/**
 * The UI editor's home (UI editor design 5.6, phase 1: words), opened by /gg ui edit and from the strip on any mod screen. Three tabs:
 * Screens (the screen browser: open any mod screen in edit mode, with this client's own camp state as its sample data; screens that
 * need a battle say so), Lines (every line drawn since the editor opened, the battle HUD's included, and the edits whose piece no
 * longer exists), and Versions (Publish, Discard, the published history with Roll back). The editor's own controls are not editable.
 */
public final class UiEditorScreen extends Screen {
    final Screen parent;
    String tab = "screens";
    int scroll;

    UiEditorScreen(Screen parent) {
        super(Text.literal("UI editor"));
        this.parent = parent instanceof UiEditorScreen e ? e.parent : parent instanceof UiEditScreen s ? s.parent : parent;
    }

    /** A screen the browser can open: a name, and how to make it (null when it cannot be opened here, with why). */
    record Entry(String name, Supplier<Screen> make, String why) {}

    static List<Entry> screens() {
        boolean inWorld = MinecraftClient.getInstance().world != null;
        String camp = inWorld ? null : "join a world first";
        List<Entry> out = new ArrayList<>();
        out.add(new Entry("Main menu: home", () -> new FrontMenuScreen(), null));
        out.add(new Entry("Main menu: play with friends", () -> new FrontMenuScreen("friends"), null));
        out.add(new Entry("Main menu: credits", () -> new FrontMenuScreen("credits"), null));
        for (String page : List.of("home", "recruit", "lobby", "results", "gear", "train"))
            out.add(new Entry("War Table: " + page, inWorld ? () -> new CampScreen(page) : null, camp));
        for (String page : WarTablePages.NAMES) out.add(new Entry("War Table: " + page, inWorld ? () -> new CampScreen(page) : null, camp));
        out.add(new Entry("Barracks", inWorld ? RecruitmentScreen::new : null, camp));
        out.add(new Entry("Armoury", inWorld ? () -> new RecruitmentScreen(1) : null, camp));
        out.add(new Entry("Visuals and sound", () -> new PresentationScreen(null), null));
        out.add(new Entry("Battlefield map", null, "needs a running battle: open it in a match"));
        out.add(new Entry("Battle report", null, "needs a finished battle"));
        out.add(new Entry("Battle HUD", null, "in a match its lines appear under Lines"));
        return out;
    }

    void refresh() { if (client != null) clearAndInit(); }

    @Override protected void init() {
        int x = 10, y = 30;
        for (String t : new String[] {"screens", "lines", "versions"}) {
            String label = t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1);
            var b = addDrawableChild(ButtonWidget.builder(Text.literal(label), w -> { tab = t; scroll = 0; clearAndInit(); }).dimensions(x, y, 70, 18).build());
            b.active = !tab.equals(t);
            x += 74;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal(UiEditorClient.active ? "Exit editor" : "Editor off"), w -> { UiEditorClient.on(false); close(); }).dimensions(width - 190, 30, 90, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), w -> close()).dimensions(width - 96, 30, 86, 18).build());
        int top = 56, rows = Math.max(1, (height - top - 34) / 20);
        switch (tab) {
            case "screens" -> {
                List<Entry> list = screens();
                for (int i = 0; i < rows && i + scroll < list.size(); i++) {
                    Entry e = list.get(i + scroll);
                    var b = addDrawableChild(ButtonWidget.builder(Text.literal(e.make() == null ? e.name() + "  (" + e.why() + ")" : e.name()), w -> client.setScreen(e.make().get())).dimensions(10, top + i * 20, Math.min(360, width - 20), 18).build());
                    b.active = e.make() != null;
                }
            }
            case "lines" -> {
                List<UiText.Seen> list = lines();
                for (int i = 0; i < rows && i + scroll < list.size(); i++) {
                    UiText.Seen s = list.get(i + scroll);
                    String mark = s.locked ? "[locked] " : UiText.draft().containsKey(s.id) ? "[edited] " : "";
                    String label = textRenderer.trimToWidth(mark + s.id + ": " + s.shown, Math.min(460, width - 20) - 8);
                    addDrawableChild(ButtonWidget.builder(Text.literal(label), w -> client.setScreen(new UiEditScreen(this, s))).dimensions(10, top + i * 20, Math.min(460, width - 20), 18).build());
                }
            }
            default -> {
                addDrawableChild(ButtonWidget.builder(Text.literal("Publish"), w -> UiEditorClient.op("publish")).dimensions(10, top, 90, 18).build()).active = !changes().equals("no change");
                addDrawableChild(ButtonWidget.builder(Text.literal("Discard draft"), w -> UiEditorClient.op("discard")).dimensions(104, top, 90, 18).build());
                List<JsonObject> h = history();
                for (int i = 0; i < rows - 3 && i + scroll < h.size(); i++) {
                    JsonObject en = h.get(i + scroll);
                    int v = en.get("version").getAsInt();
                    addDrawableChild(ButtonWidget.builder(Text.literal("Roll back"), w -> UiEditorClient.rollback(v)).dimensions(10, top + 66 + i * 20, 70, 18).build());
                }
            }
        }
    }

    static List<UiText.Seen> lines() {
        List<UiText.Seen> list = new ArrayList<>(UiText.seen(Long.MAX_VALUE));
        list.sort(Comparator.comparing(s -> s.id));
        return list;
    }

    static List<JsonObject> history() {
        List<JsonObject> out = new ArrayList<>();
        var st = UiEditorClient.state;
        if (st.has("history")) for (JsonElement e : st.getAsJsonArray("history")) out.add(e.getAsJsonObject());
        return out;
    }

    static String changes() { return UiSheet.summary(UiText.server(), UiText.draft()); }

    int total() {
        return switch (tab) { case "screens" -> screens().size(); case "lines" -> lines().size(); default -> history().size(); };
    }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        int next = Math.max(0, Math.min(Math.max(0, total() - 1), scroll - (int) Math.signum(v) * 3));
        if (next != scroll) { scroll = next; clearAndInit(); }
        return true;
    }

    @Override public void render(DrawContext g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        int ink = 0xffe8d8a8, dim = 0xffa89878;
        g.drawText(textRenderer, "UI EDITOR", 10, 10, 0xffffd75a, false);
        g.drawText(textRenderer, textRenderer.trimToWidth("Draft: " + changes() + ".  " + UiEditorClient.message, width - 100), 90, 10, ink, false);
        int top = 56;
        switch (tab) {
            case "screens" -> g.drawText(textRenderer, "Open a screen, then click a line to retype it. Sample data is this game's own camp state.", 10, height - 24, dim, false);
            case "lines" -> {
                if (lines().isEmpty()) g.drawText(textRenderer, "No lines drawn yet: open a screen or play a match with the editor on.", 10, top, dim, false);
                var st = UiEditorClient.state;
                int orphans = st.has("orphans") ? st.getAsJsonArray("orphans").size() : 0;
                g.drawText(textRenderer, orphans == 0 ? "Every edit belongs to a line in this build." : orphans + " edit(s) belong to lines this build no longer has: kept, not shown (" + st.getAsJsonArray("orphans").get(0).getAsString() + (orphans > 1 ? ", ..." : "") + ").", 10, height - 24, dim, false);
            }
            default -> {
                var st = UiEditorClient.state;
                String pub = UiSheet.str(st, "published");
                g.drawText(textRenderer, textRenderer.trimToWidth("Published: " + (pub.isEmpty() ? "version " + (st.has("version") ? st.get("version").getAsInt() : 0) + " (the mod's built-in wording)" : pub), width - 20), 10, top + 26, ink, false);
                String damaged = UiSheet.str(st, "damaged");
                if (!damaged.isEmpty()) g.drawText(textRenderer, textRenderer.trimToWidth("The server's sheet is damaged (" + damaged + "): /gg ui reset starts a fresh one.", width - 20), 10, top + 40, 0xffff8a6a, false);
                g.drawText(textRenderer, "Earlier versions:", 10, top + 54, dim, false);
                List<JsonObject> h = history();
                int rows = Math.max(1, (height - top - 34) / 20);
                for (int i = 0; i < rows - 3 && i + scroll < h.size(); i++) {
                    JsonObject en = h.get(i + scroll);
                    String line = "v" + en.get("version").getAsInt() + "  " + UiSheet.str(en, "time") + "  " + UiSheet.str(en, "who") + "  " + UiSheet.str(en, "summary");
                    g.drawText(textRenderer, textRenderer.trimToWidth(line, width - 100), 86, top + 71 + i * 20, ink, false);
                }
                if (h.isEmpty()) g.drawText(textRenderer, "none yet", 86, top + 71, dim, false);
            }
        }
    }

    @Override public void close() { client.setScreen(parent instanceof UiEditScreen ? null : parent); }
}
