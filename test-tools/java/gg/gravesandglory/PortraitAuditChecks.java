package gg.gravesandglory;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The unit-portrait audit (Kort, 2026-11-11: units in frames stare off, are cut off, or move in lockstep). Shoots every troop through
 * every way the UI frames one, at 1920x1080, into <run>/command-ui/portraits:
 * <ul>
 * <li>studio sheets: every troop in the 40 px roll frame (tier I and tier III, as the Armoury shows the best tier), every troop in the
 * 160 px letter frame, and every troop on the roll card (DarkUi.model);</li>
 * <li>a group strip: one troop in eight 40 px frames and eight troops side by side, shot six times 3 ticks apart, so lockstep shows;</li>
 * <li>the real pages: the War Table recruit page for every House, the troop letter for the large, mounted and four-legged troops, the
 * banner page, then in a battle the Barracks page (overview and a troop picked) and the Armoury.</li>
 * </ul>
 * The suite is cmdui.PortraitsSuite (GRAVES_COMMAND_UI_CHECKS=Portraits). Every frame Portrait drew is logged to portraits.tsv.
 */
public class PortraitAuditChecks implements CommandUiChecks.Suite {
    static final String[] TALL = {"knight", "gb_hollowking", "wm_hound", "wm_tusk", "ic_colossus", "tc_admiral", "ic_ballista", "wm_oldhorn", "reaper", "tc_brute"};
    Path shots;
    int[] initialSize;
    final List<String> shotLog = new ArrayList<>();
    final List<Runnable> steps = new ArrayList<>();
    final List<Integer> waits = new ArrayList<>();
    int step, due;
    boolean battle;

    @Override public int limit() { return 4000; }

    /** Every troop the UI can frame: a Spec row whose portrait mob exists. */
    static List<Spec.TroopsRow> troops() {
        List<Spec.TroopsRow> out = new ArrayList<>();
        for (var t : Spec.TROOPS) if (ArmyCards.portrait(t) != null) out.add(t);
        return out;
    }

    /** The studio: a plain screen that draws the troops as the pages do, in the pages' own frames. */
    static final class Studio extends Screen {
        String mode = "row"; int page, tier = 1; final List<Spec.TroopsRow> list;
        Studio(List<Spec.TroopsRow> list) { super(Text.literal("Portrait studio")); this.list = list; }
        @Override public boolean shouldPause() { return false; }
        @Override public void render(DrawContext g, int mx, int my, float delta) {
            g.fill(0, 0, width, height, 0xff2a2118);
            var c = MinecraftClient.getInstance(); var f = c.textRenderer;
            if (mode.equals("card")) { // DarkUi.model as the roll card draws it: a 104 x 128 box, the model at size 40 from 10 above its bottom
                int pw = 104, ph = 128, cols = Math.max(1, (width - 8) / (pw + 6));
                for (int i = 0; i < list.size(); i++) {
                    int px = 6 + (i % cols) * (pw + 6), py = 6 + (i / cols) * (ph + 14);
                    g.fill(px, py, px + pw, py + ph, 0xff141a1d); g.drawBorder(px, py, pw, ph, 0xff455156);
                    DarkUi.model(g, list.get(i), px + pw / 2, py + ph - 10, 40);
                    g.getMatrices().push(); g.getMatrices().translate(0, 0, 400);
                    g.drawText(f, f.trimToWidth(list.get(i).id(), pw), px + 2, py + ph + 2, 0xffe8dcc0, false);
                    g.getMatrices().pop();
                }
                return;
            }
            UiCanvas.begin(g);
            int cw = UiCanvas.width();
            if (mode.equals("row")) {
                int cell = 62, cols = Math.max(1, (cw - 8) / cell);
                for (int i = 0; i < list.size(); i++) {
                    int x = 4 + (i % cols) * cell, y = 4 + (i / cols) * 70;
                    frame40(g, ArmyCards.portrait(list.get(i), tier, tier), x, y);
                    g.drawText(f, f.trimToWidth(list.get(i).id(), cell - 2), x, y + 58, 0xffe8dcc0, false);
                }
            } else if (mode.equals("group")) {
                var knight = ArmyCards.spec("knight");
                for (int i = 0; i < 8; i++) frame40(g, ArmyCards.portrait(knight), 4 + i * 58, 4);
                for (int i = 0; i < 8 && i < list.size(); i++) frame40(g, ArmyCards.portrait(list.get(i * Math.max(1, list.size() / 8))), 4 + i * 58, 70);
            } else if (mode.equals("orient")) { // the knight drawn straight with drawEntity at four body yaws, to read which way it faces
                var e = ArmyCards.portrait(ArmyCards.spec("knight"));
                float[] yaws = {180, 210, 270, 0};
                for (int i = 0; i < 4 && e != null; i++) {
                    int x = 4 + i * 170, y = 4;
                    g.fill(x, y, x + 160, y + 160, 0xff141a1d);
                    float by = e.bodyYaw, hy = e.headYaw, yy = e.getYaw();
                    e.bodyYaw = yaws[i]; e.headYaw = yaws[i]; e.setYaw(yaws[i]); e.prevBodyYaw = yaws[i]; e.prevHeadYaw = yaws[i];
                    var cam = new org.joml.Quaternionf();
                    var body = new org.joml.Quaternionf().rotateZ((float) Math.PI);
                    net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(g, x + 80, y + 150, 60, body, cam, e);
                    e.bodyYaw = by; e.headYaw = hy; e.setYaw(yy); e.prevBodyYaw = by; e.prevHeadYaw = hy;
                    g.drawText(f, "yaw " + (int) yaws[i], x + 2, y + 2, 0xffe8dcc0, false);
                }
            } else { // letter: the 160 px window in the ornate frame, as the Barracks and recruit letters draw it
                int cell = 190, cols = Math.max(1, (cw - 4) / cell), per = cols * 2;
                for (int j = 0; j < per; j++) {
                    int i = page * per + j; if (i >= list.size()) break;
                    int x = 4 + (j % cols) * cell, y = 4 + (j / cols) * 200;
                    UiTex.draw(g, "table/frame-back-160", x + 12, y + 12);
                    var e = ArmyCards.portrait(list.get(i));
                    if (e != null) { e.age = c.player == null ? 0 : c.player.age; Portrait.draw(g, e, x + 12, y + 12, 160); log(list.get(i).id(), "letter-" + j); }
                    UiTex.draw(g, "table/frame-ornate-184", x, y);
                    g.drawText(f, list.get(i).id(), x + 4, y + 187, 0xffe8dcc0, false);
                }
            }
            UiCanvas.end(g);
        }
        void frame40(DrawContext g, LivingEntity e, int x, int y) {
            var c = MinecraftClient.getInstance();
            UiTex.draw(g, "table/frame-back-48", x + 4, y + 4);
            if (e != null) { e.age = c.player == null ? 0 : c.player.age; Portrait.draw(g, e, x + 4, y + 4, 40); log(String.valueOf(e.getCustomName() == null ? "?" : e.getCustomName().getString()), mode + "@" + x + "," + y); }
            UiTex.draw(g, "table/frame-rim-48", x, y);
        }
    }

    static final List<String> drawLog = new ArrayList<>();
    static final Set<String> LOGGED = new HashSet<>();
    /** Every troop's head bounds and its fit in a 160 window, for the tuning table. */
    static List<String> boundsTable() {
        List<String> out = new ArrayList<>(List.of("troop|top|faceTop|faceBottom|forward|span|scale"));
        for (var t : troops()) {
            var e = ArmyCards.portrait(t); var b = Portrait.bounds(e); var f = Portrait.fit(e, 160);
            out.add(t.id() + "|" + b.top() + "|" + b.faceTop() + "|" + b.faceBottom() + "|" + b.forward() + "|" + f.span() + "|" + Portrait.modelScale(e));
        }
        return out;
    }
    static void log(String who, String where) {
        var d = Portrait.last();
        if (d != null && LOGGED.add(who + "\t" + where)) drawLog.add(who + "\t" + where + "\t" + d);
    }

    void add(int wait, Runnable r) { waits.add(wait); steps.add(r); }

    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) {
            shots = FabricLoader.getInstance().getGameDir().resolve("command-ui").resolve("portraits");
            try { Files.createDirectories(shots); } catch (Exception ignored) { }
            initialSize = new int[]{c.getWindow().getWidth(), c.getWindow().getHeight()};
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
            plan(c);
            due = 30;
            return;
        }
        if (step < steps.size() && tick >= due) {
            try { steps.get(step).run(); } catch (Throwable e) { shotLog.add("step " + step + " FAILED " + e); e.printStackTrace(); }
            due = tick + waits.get(step); step++;
        }
        if (step >= steps.size() && tick >= due) {
            try { Files.write(shots.resolve("portraits.tsv"), drawLog); Files.write(shots.resolve("shots.txt"), shotLog); Files.write(shots.resolve("bounds.tsv"), boundsTable()); } catch (Exception ignored) { }
            CommandUiChecks.report("cu-portraits-audit", shotLog.stream().noneMatch(s -> s.contains("FAILED")), shotLog.size() + " shots: " + String.join(", ", shotLog));
            c.setScreen(null);
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), initialSize[0], initialSize[1]);
            CommandUiChecks.finish(c);
            step = Integer.MAX_VALUE / 2;
        }
    }

    void plan(MinecraftClient c) {
        var list = troops();
        // the War Table: recruit page per House, troop letters, banner page (out of battle; CampScreen closes in one)
        add(20, () -> c.setScreen(new CampScreen("recruit")));
        for (var h : Spec.HOUSES) {
            add(16, () -> { if (c.currentScreen instanceof CampScreen s && s.recruit != null) s.recruit.chooseHouse(h.id()); });
            add(2, () -> shot(c, "page-recruit-" + h.id()));
        }
        for (String id : TALL) {
            if (ArmyCards.spec(id) == null) continue;
            add(16, () -> { if (c.currentScreen instanceof CampScreen s) s.openTroop(id); else { var s = new CampScreen("recruit"); c.setScreen(s); s.openTroop(id); } });
            add(2, () -> shot(c, "page-troop-" + id));
        }
        add(16, () -> c.setScreen(new CampScreen("banner")));
        add(2, () -> shot(c, "page-banner"));
        // the studio sheets
        Studio studio = new Studio(list);
        add(16, () -> { studio.mode = "row"; studio.tier = 1; c.setScreen(studio); });
        add(2, () -> shot(c, "studio-row40-t1"));
        add(10, () -> studio.tier = 3);
        add(2, () -> shot(c, "studio-row40-t3"));
        int pages = (list.size() + 9) / 10;
        for (int p = 0; p < pages; p++) { int pp = p; add(10, () -> { studio.mode = "letter"; studio.page = pp; }); add(2, () -> shot(c, "studio-letter160-p" + (pp + 1))); }
        add(10, () -> studio.mode = "card");
        add(2, () -> shot(c, "studio-card"));
        add(10, () -> studio.mode = "orient");
        add(2, () -> shot(c, "studio-orient"));
        add(10, () -> { studio.mode = "group"; studio.tier = 1; });
        for (int k = 0; k < 6; k++) { int kk = k; add(3, () -> shot(c, "studio-group-f" + kk)); }
        // the battle pages: Barracks and Armoury
        add(60, () -> { c.setScreen(null); CommandUiChecks.seatAndStart(c); });
        add(30, () -> CommandUiChecks.server(c, b -> toBarracks(c, b)));
        add(30, () -> CommandUiChecks.server(c, b -> toBarracks(c, b)));
        add(16, () -> c.setScreen(new GravesAndGloryClient.BarracksScreen()));
        add(2, () -> shot(c, "page-barracks-overview"));
        add(16, () -> { if (c.currentScreen instanceof RecruitmentScreen r && !r.barracks.listed.isEmpty()) { r.selected = r.barracks.listed.get(r.barracks.listed.size() > 1 ? 1 : 0); } });
        add(2, () -> shot(c, "page-barracks-picked"));
        add(16, () -> c.setScreen(new RecruitmentScreen(1)));
        add(2, () -> shot(c, "page-armoury"));
    }

    void toBarracks(MinecraftClient c, GravesAndGlory.Battle b) {
        var s = b.squads.get(c.player.getUuid()); var p = c.getServer().getPlayerManager().getPlayer(c.player.getUuid());
        if (s == null || p == null) return;
        s.gold = 5000;
        var at = b.fieldBarracks(s.team);
        double x = at.x + 2.5, z = at.z + .5;
        int y = p.getServerWorld().getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        p.teleport(p.getServerWorld(), x, y, z, p.getYaw(), p.getPitch());
    }

    void shot(MinecraftClient c, String name) {
        try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {
            img.writeTo(shots.resolve(name + ".png"));
            shotLog.add(name + " " + img.getWidth() + "x" + img.getHeight() + " on " + (c.currentScreen == null ? "no screen" : c.currentScreen.getClass().getSimpleName()));
        } catch (Exception e) { shotLog.add(name + " FAILED " + e); }
    }

    @Override public void cleanup(MinecraftClient c) { if (c.currentScreen != null) c.setScreen(null); }
}
