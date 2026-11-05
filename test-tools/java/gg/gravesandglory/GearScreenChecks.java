package gg.gravesandglory;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import java.util.*;

import static gg.gravesandglory.Phase1Checks.*;

/**
 * Real-client checks for the Arsenal (gear screen plan Task 4), GRAVES_PHASE1_CHECKS=ui-gear: the gear key opens and closes the page
 * in camp and not in a battle, the blacksmith station opens the Forge tab, every drawn box fits at 1920x1080, 2560x1440 and
 * 3840x2160, grid clicks equip and break down (Epic and above asking first), Roll reveals the piece and a double click rolls once,
 * 120 pieces scroll by whole rows and the offset clamps after a breakdown, and the corner tooltip stays inside the canvas.
 * Shots: run/screenshots/p1-gear-*.png. The profile is put back from a JSON copy taken at the start. Never shipped.
 */
final class GearScreenChecks implements Phase1Checks.Suite {
    static final int HELM = 900001, EPIC = 900002, CHEST = 900003;
    static final int[][] SIZES = {{1920, 1080}, {2560, 1440}, {3840, 2160}};
    volatile UUID player;
    volatile String backup;
    volatile boolean ready, restored, roundOn;
    int stage, at, size, revealK, count0, marks0, rollCost, oldW, oldH, oldGui;
    String note = "";
    boolean done;

    static void say(String line) { System.out.println("[p1] gear-ui: " + line); }
    void next(int tick, int s) { stage = s; at = tick + 1; note = ""; }

    static CampScreen screen(MinecraftClient c) { return c.currentScreen instanceof CampScreen s && "gear".equals(s.page) ? s : null; }
    static WarTableGear arsenal(MinecraftClient c) { var s = screen(c); return s == null ? null : s.arsenal; }
    static void press(MinecraftClient c, String button) {
        var a = arsenal(c);
        var b = a == null ? null : a.buttons.get(button);
        if (b != null) UiHomeChecks.click(c, b.canvasBox());
    }
    static void clickBox(MinecraftClient c, int[] box) { if (box != null) UiHomeChecks.click(c, box); }
    static String str(int[] b) { return b == null ? "none" : b[0] + "," + b[1] + " " + b[2] + "x" + b[3]; }
    static String rarity(int i) { return Spec.GEARRARITIES[i].id(); }
    static String firstItem(String slot) { for (var it : Spec.GEARITEMS) if (it.slot().equals(slot)) return it.id(); throw new IllegalStateException("no " + slot); }

    /** The grid cell index a piece is drawn at for the page's filter, sort and scroll, or -1 when it is not on screen. */
    static int cellOf(WarTableGear a, int uid) {
        var list = GearView.visible(CampaignClient.gear, a.filter, a.sort);
        for (int i = 0; i < list.size(); i++) if (list.get(i).uid == uid) {
            int cell = i - a.scroll * GearView.COLUMNS;
            return cell >= 0 && cell < WarTableGear.gridRows() * GearView.COLUMNS ? cell : -1;
        }
        return -1;
    }

    /** The bound gear key, pressed the way GLFW's key callback would count it. */
    static void gearKeyPress() { KeyBinding.onKeyPressed(KeyBindingHelper.getBoundKeyOf(GravesAndGloryClient.gearKey)); }
    static int gearKeyCode() { return KeyBindingHelper.getBoundKeyOf(GravesAndGloryClient.gearKey).getCode(); }

    @Override public void cleanup(MinecraftClient c) {
        WarTableGear.harnessShift = false;
        WarTableGear.revealHeldAt = -1;
        if (restored || player == null || c.getServer() == null) return;
        try { c.getServer().submit(this::restore).join(); } catch (RuntimeException e) { say("restore in cleanup failed: " + e); }
    }

    /** Puts the profile back from the copy and leaves any battle. Runs on the server thread. */
    void restore() {
        var b = GravesAndGlory.battle;
        if (b == null || backup == null) return;
        if (b.running) b.campaign.returnLobby();
        b.campaign.profiles.put(player, Campaign.JSON.fromJson(backup, Profile.class));
        b.campaign.save(player);
        b.campaign.forgeResults.remove(player);
        var p = b.server.getPlayerManager().getPlayer(player);
        if (p != null) b.campaign.syncGear(p);
        restored = true;
    }

    /** The commander the screen checks run on: 500 marks, a spare Common helm, an Epic helm and a Common chest. Server thread. */
    void grant(GravesAndGlory.Battle b) {
        var pr = b.campaign.profile(player);
        pr.forgeMarks = 500;
        pr.gear.removeIf(g -> g.uid >= 900000);
        pr.gear.add(new Gear.Piece(HELM, firstItem("helm"), rarity(0), 50, 100));
        pr.gear.add(new Gear.Piece(EPIC, firstItem("helm"), rarity(3), 50, 100));
        pr.gear.add(new Gear.Piece(CHEST, firstItem("chest"), rarity(0), 50, 100));
        pr.nextGearUid = Math.max(pr.nextGearUid, 900010);
        pr.inventorySize = Math.max(pr.inventorySize, pr.gear.size() + 10);
        b.campaign.save(player);
        var p = b.server.getPlayerManager().getPlayer(player);
        if (p != null) b.campaign.syncGear(p);
    }

    /** n unworn Commons and nothing else, at the largest inventory. Server thread. */
    void hundred(GravesAndGlory.Battle b, int n) {
        var pr = b.campaign.profile(player);
        pr.gear.clear();
        pr.equipped.clear();
        pr.inventorySize = Gear.forge().maxSlots();
        for (int i = 0; i < n; i++) pr.gear.add(new Gear.Piece(910000 + i, firstItem("helm"), rarity(0), 50, 100));
        pr.nextGearUid = 910000 + n;
        b.campaign.save(player);
        var p = b.server.getPlayerManager().getPlayer(player);
        if (p != null) b.campaign.syncGear(p);
    }

    /** Every drawn box and visible widget 12 px inside the canvas and clear of the rail, and no two of the commander's boxes or the
     *  grid overlapping. Empty when the page fits. */
    static List<String> fit(MinecraftClient c) {
        var a = arsenal(c);
        Map<String, int[]> widgets = new LinkedHashMap<>();
        if (a != null) for (var e : a.buttons.entrySet()) if (e.getValue().visible) widgets.put(e.getKey(), e.getValue().canvasBox());
        List<String> bad = new ArrayList<>(UiHomeChecks.fits(WarTableGear.rects(), widgets));
        List<String> keys = new ArrayList<>();
        for (String k : WarTableGear.rects().keySet()) if (k.equals("stats") || k.equals("frame") || k.equals("grid") || k.startsWith("worn-") || k.startsWith("relic-")) keys.add(k);
        for (int i = 0; i < keys.size(); i++) for (int j = i + 1; j < keys.size(); j++)
            if (UiHomeChecks.overlap(WarTableGear.rects().get(keys.get(i)), WarTableGear.rects().get(keys.get(j)))) bad.add(keys.get(i) + "/" + keys.get(j) + " overlap");
        List<String> wk = new ArrayList<>(widgets.keySet());
        for (int i = 0; i < wk.size(); i++) for (int j = i + 1; j < wk.size(); j++)
            if (UiHomeChecks.overlap(widgets.get(wk.get(i)), widgets.get(wk.get(j)))) bad.add("widgets " + wk.get(i) + "/" + wk.get(j) + " overlap");
        return bad;
    }

    public void step(MinecraftClient c, int tick) {
        if (done) return;
        if (tick > 20000) { report("p1-gear-ui-timeout", false, "stuck at stage " + stage); end(c); return; }
        int k = tick - at;
        var g = CampaignClient.gear;
        switch (stage) {
            case 0 -> { // the copy, the grant, then wait for the gear view to carry it
                if (tick < 20) return;
                if (player == null) {
                    var w = c.getWindow(); oldW = w.getWidth(); oldH = w.getHeight(); oldGui = c.options.getGuiScale().getValue();
                    UUID id = c.player.getUuid(); player = id; c.setScreen(null);
                    server(c, b -> { if (b.running) b.campaign.returnLobby(); backup = Campaign.JSON.toJson(b.campaign.profile(id)); grant(b); ready = true; });
                    window(c, 1920, 1080, 0, true);
                    at = tick; return;
                }
                if (ready && g != null && g.forgeMarks() == 500 && GearView.piece(g, EPIC) != null) next(tick, 1);
                else if (k > 200) { report("p1-gear-ui-open", false, "the grant never reached the client: " + (g == null ? "no gear view" : g.forgeMarks() + " marks")); end(c); }
            }
            case 1 -> { // p1-gear-ui-open: the key opens the page in camp, the key again closes it
                if (k == 0) { c.setScreen(null); gearKeyPress(); }
                if (k == 10) note = "opened " + (screen(c) != null) + (arsenal(c) == null ? "" : " on tab " + arsenal(c).tab);
                if (k == 12 && screen(c) != null) { int code = gearKeyCode(); c.currentScreen.keyPressed(code, 0, 0); }
                if (k == 20) {
                    boolean closed = c.currentScreen == null;
                    report("p1-gear-ui-open", note.startsWith("opened true") && closed, note + ", the key again closed it " + closed + " (in battle: checked last)");
                    next(tick, 2);
                }
            }
            case 2 -> { // p1-gear-ui-station: the blacksmith opens the Forge tab; he stands 6 blocks south of the quartermaster
                if (k == 0) server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (p != null) GravesAndGlory.openCamp(p, "forge"); });
                if (k == 20) {
                    var a = arsenal(c);
                    Spec.CampStationsRow smith = null, quarter = null;
                    for (var s : Spec.CAMPSTATIONS) { if (s.id().equals("blacksmith")) smith = s; if (s.id().equals("quartermaster")) quarter = s; }
                    boolean placed = smith != null && quarter != null && smith.x() == quarter.x() && smith.z() - quarter.z() == 6 && smith.page().equals("forge");
                    report("p1-gear-ui-station", a != null && a.tab.equals("forge") && placed, "station forge opened " + (a == null ? "nothing (screen " + c.currentScreen + ")" : "page gear, tab " + a.tab) + "; blacksmith " + (smith == null ? "missing" : smith.x() + "," + smith.z()) + " vs quartermaster " + (quarter == null ? "missing" : quarter.x() + "," + quarter.z()));
                    c.setScreen(null); size = 0; next(tick, 3);
                }
            }
            case 3 -> { // p1-gear-ui-rects-<w>: Arsenal then Forge at each size
                int w = SIZES[size][0], h = SIZES[size][1];
                if (k == 0) { c.setScreen(null); window(c, w, h, 0, true); }
                if (k == 8) CampScreen.open("gear");
                if (k == 20) UiRecruitChecks.park(c);
                if (k == 24) {
                    var bad = fit(c); note = "armoury: canvas " + UiCanvas.width() + "x" + UiCanvas.height() + " k " + UiCanvas.k() + " window " + c.getWindow().getFramebufferWidth() + "x" + c.getWindow().getFramebufferHeight() + ", page " + (arsenal(c) != null) + ", " + WarTableGear.rects().size() + " rects, bad " + bad;
                    if (!bad.isEmpty() || arsenal(c) == null) note += " WRONG";
                    shot(c, "gear-" + w + "-armoury");
                }
                if (k == 26) press(c, "page-forge");
                if (k == 34) UiRecruitChecks.park(c);
                if (k == 38) {
                    var a = arsenal(c); var bad = fit(c);
                    boolean forge = a != null && a.tab.equals("forge");
                    for (String key : List.of("roll", "pity", "dupes", "repair-all", "expand", "forge-slot-helm", "forge-slot-relic")) if (!WarTableGear.rects().containsKey(key)) bad.add(key + " not drawn");
                    shot(c, "gear-" + w + "-forge");
                    report("p1-gear-ui-rects-" + w, !note.endsWith("WRONG") && forge && bad.isEmpty(), note + "; forge: tab " + (a == null ? "none" : a.tab) + ", bad " + bad);
                    c.setScreen(null);
                    if (++size < SIZES.length) next(tick, 3);
                    else { window(c, 1920, 1080, 0, true); next(tick, 4); }
                }
            }
            case 4 -> { // p1-gear-ui-equip: a click on the spare helm wears it
                if (k == 6) CampScreen.open("gear");
                if (k == 16) press(c, "tab-helm");
                if (k == 24) { var a = arsenal(c); int cell = a == null ? -1 : cellOf(a, HELM); note = "helm in cell " + cell; if (cell >= 0) clickBox(c, WarTableGear.cell(cell)); }
                boolean worn = g != null && Integer.valueOf(HELM).equals(g.equipped().get("helm"));
                if (k > 24 && worn || k > 130) { report("p1-gear-ui-equip", worn, note + ", worn helm " + (g == null ? "?" : g.equipped().get("helm")) + " after " + (k - 24) + " ticks"); next(tick, 5); }
            }
            case 5 -> { // p1-gear-ui-confirm: an Epic asks first and sends nothing; Break down sends; a Common goes at once
                var a = arsenal(c);
                if (a == null) { report("p1-gear-ui-confirm", false, "the page is not open"); next(tick, 6); return; }
                if (k == 0) { WarTableGear.harnessShift = true; int cell = cellOf(a, EPIC); note = "epic in cell " + cell; if (cell >= 0) clickBox(c, WarTableGear.cell(cell)); }
                if (k == 30) {
                    boolean asks = a.confirmUid == EPIC && WarTableGear.rects().containsKey("confirm") && GearView.piece(g, EPIC) != null;
                    note += ", asks " + asks + " (confirm " + a.confirmUid + ", strip " + str(WarTableGear.rects().get("confirm")) + ", still owned " + (GearView.piece(g, EPIC) != null) + ")";
                    if (!asks) note += " WRONG";
                    press(c, "confirm-yes");
                }
                if (k == 30 + 60) { note += ", after Break down owned " + (GearView.piece(g, EPIC) != null); if (GearView.piece(g, EPIC) != null) note += " WRONG"; press(c, "tab-chest"); }
                if (k == 100) { int cell = cellOf(a, CHEST); note += ", chest in cell " + cell; if (cell >= 0) clickBox(c, WarTableGear.cell(cell)); }
                if (k == 104) note += ", confirm on the Common " + a.confirmUid;
                if (k > 104 && GearView.piece(g, CHEST) == null || k > 200) {
                    boolean gone = GearView.piece(g, CHEST) == null;
                    WarTableGear.harnessShift = false;
                    report("p1-gear-ui-confirm", !note.contains("WRONG") && !note.endsWith("Common " + CHEST) && gone, note + ", the Common gone " + gone + " in " + (k - 100) + " ticks");
                    next(tick, 6);
                }
            }
            case 6 -> { // p1-gear-ui-roll: Roll twice 100 ms apart on helm: one roll, one reveal
                var a = arsenal(c);
                if (a == null) { report("p1-gear-ui-roll", false, "the page is not open"); next(tick, 7); return; }
                if (k == 0) { server(c, b -> { b.campaign.profile(player).forgeMarks = 200; b.campaign.save(player); var p = b.server.getPlayerManager().getPlayer(player); if (p != null) b.campaign.syncGear(p); }); press(c, "page-forge"); }
                if (k == 30) {
                    count0 = g == null ? -1 : g.pieces().size(); marks0 = g == null ? -1 : g.forgeMarks(); rollCost = Forge.rollCost("helm");
                    a = arsenal(c); if (a != null) a.forgeSlot = "helm";
                    note = "marks " + marks0 + ", pieces " + count0;
                    press(c, "roll");
                }
                if (k == 32) press(c, "roll");
                if (k > 32 && k < 140 && WarTableGear.rects().containsKey("reveal") && !note.contains("reveal at")) {
                    note += ", reveal at " + (k - 30) + " ticks";
                    WarTableGear.revealHeldAt = 1000;
                    revealK = k;
                }
                if (revealK > 0 && k == revealK + 3) shot(c, "gear-1080-reveal");
                if (k == 150) {
                    WarTableGear.revealHeldAt = -1;
                    int n = g == null ? -1 : g.pieces().size(), m = g == null ? -1 : g.forgeMarks();
                    boolean once = n == count0 + 1 && m == marks0 - rollCost;
                    var f = CampaignClient.forgeRoll;
                    report("p1-gear-ui-roll", note.contains("reveal at") && once, note + "; after 150 ticks pieces " + n + " (wanted " + (count0 + 1) + "), marks " + m + " (wanted " + (marks0 - rollCost) + "), forge roll " + f);
                }
                if (k == 156) { press(c, "page-armoury"); next(tick, 7); }
            }
            case 7 -> { // p1-gear-ui-120: 120 pieces, scrolled to the end; the last row shows
                if (k == 0) server(c, b -> hundred(b, 120));
                var a = arsenal(c);
                if (k == 10 && a != null) { a.setFilter("all"); }
                if (k > 10 && k < 300 && g != null && g.pieces().size() == 120 && !note.startsWith("synced")) {
                    note = "synced at " + k;
                    var s = c.currentScreen;
                    int[] grid = WarTableGear.rects().get("grid");
                    if (s != null && grid != null) for (int n = 0; n < 40; n++) s.mouseScrolled(UiHomeChecks.gui(c, grid[0] + grid[2] / 2.0), UiHomeChecks.gui(c, grid[1] + grid[3] / 2.0), -1); // one row per wheel notch
                    at = tick - 300; // the next tick is k 301
                }
                if (k == 306) {
                    UiRecruitChecks.park(c);
                }
                if (k == 310) {
                    int want = Math.max(0, GearView.rows(120) - WarTableGear.gridRows());
                    int lastCell = 119 - (a == null ? 0 : a.scroll) * GearView.COLUMNS;
                    boolean last = a != null && a.scroll == want && WarTableGear.rects().containsKey("cell-" + lastCell);
                    note += ", scroll " + (a == null ? "?" : a.scroll) + " (wanted " + want + "), last piece in cell " + lastCell + " drawn " + WarTableGear.rects().containsKey("cell-" + lastCell);
                    if (!last) note += " WRONG";
                    shot(c, "gear-1080-120");
                    server(c, b -> hundred(b, 113)); // 15 rows; one breakdown leaves 112, 14 rows
                }
                if (k > 312 && k < 600 && g != null && g.pieces().size() == 113 && !note.contains("113")) {
                    note += ", 113 synced";
                    var s = c.currentScreen;
                    int[] grid = WarTableGear.rects().get("grid");
                    if (s != null && grid != null) for (int n = 0; n < 40; n++) s.mouseScrolled(UiHomeChecks.gui(c, grid[0] + grid[2] / 2.0), UiHomeChecks.gui(c, grid[1] + grid[3] / 2.0), -1); // one row per wheel notch
                    at = tick - 600;
                }
                if (k == 606 && a != null) {
                    var list = GearView.visible(g, a.filter, a.sort);
                    int cell = cellOf(a, list.get(list.size() - 1).uid);
                    note += ", scroll before " + a.scroll + ", breaking down cell " + cell;
                    WarTableGear.harnessShift = true;
                    if (cell >= 0) clickBox(c, WarTableGear.cell(cell));
                    WarTableGear.harnessShift = false;
                }
                if (k > 606 && g != null && g.pieces().size() == 112 || k > 760) {
                    if (a != null) a.tick();
                    int want = Math.max(0, GearView.rows(112) - WarTableGear.gridRows());
                    boolean clamped = a != null && g != null && g.pieces().size() == 112 && a.scroll == want;
                    report("p1-gear-ui-120", !note.contains("WRONG") && clamped, note + ", after the breakdown " + (g == null ? "?" : g.pieces().size()) + " pieces, scroll " + (a == null ? "?" : a.scroll) + " (wanted " + want + ")");
                    next(tick, 8);
                }
            }
            case 8 -> { // p1-gear-ui-tooltip-corner: the bottom-right cell's tooltip stays 12 px inside
                var a = arsenal(c);
                int cell = WarTableGear.gridRows() * GearView.COLUMNS - 1;
                if (k == 2) { int[] b = WarTableGear.cell(cell); UiRecruitChecks.pointer(c, b[0] + b[2] / 2.0, b[1] + b[3] / 2.0); }
                if (k == 8) {
                    int[] t = WarTableGear.rects().get("tooltip");
                    int w = UiCanvas.width(), h = UiCanvas.height();
                    boolean inside = t != null && t[0] >= 12 && t[1] >= 12 && t[0] + t[2] <= w - 12 && t[1] + t[3] <= h - 12;
                    shot(c, "gear-1080-tooltip-corner");
                    report("p1-gear-ui-tooltip-corner", a != null && inside, "cell " + cell + " " + str(WarTableGear.cell(cell)) + ", tooltip " + str(t) + " on a " + w + "x" + h + " canvas");
                    UiRecruitChecks.park(c);
                    c.setScreen(null);
                    next(tick, 9);
                }
            }
            case 9 -> { // the gear key in a practice battle does nothing
                if (k < 1200 && !GravesAndGloryClient.battleActive()) {
                    if (k % 20 == 0) server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (p != null && !b.running) skirmish(b, p, 0, 1, "normal"); });
                    return;
                }
                if (!roundOn) { roundOn = true; at = tick; c.setScreen(null); return; }
                if (k == 40) gearKeyPress();
                if (k == 60) {
                    boolean active = GravesAndGloryClient.battleActive();
                    boolean shut = screen(c) == null;
                    report("p1-gear-ui-open-battle", active && shut, "battle active " + active + ", screen after the key " + c.currentScreen);
                    server(c, b -> { if (b.running) b.campaign.returnLobby(); });
                    next(tick, 10);
                }
            }
            case 10 -> {
                if (k > 40 && !GravesAndGloryClient.battleActive() || k > 600) end(c);
            }
            default -> {}
        }
    }

    void end(MinecraftClient c) {
        if (done) return;
        done = true;
        c.setScreen(null);
        window(c, oldW > 0 ? oldW : 854, oldH > 0 ? oldH : 480, oldGui);
        finish(c, "gear-ui");
    }
}
