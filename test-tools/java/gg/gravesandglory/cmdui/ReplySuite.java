package gg.gravesandglory.cmdui;

import gg.gravesandglory.BattleHud;
import gg.gravesandglory.CommandNet;
import gg.gravesandglory.CommandSelection;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGlory;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.ReplyProbe;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Army control polish, slice 1 (spec 2026-11-10 section 5, "every order gets exactly one answer"), on a new engine map (GRAVES_REPLY_MAP, default 6,
 * Rivergate EY, which has a river): the accepted line in words, a key press inside the command cooldown held and sent, the refusal codes down,
 * intro, no_troops (an empty Charge that starts no cooldown), out_of_range and target_gone as the client's lines, the respawn card's second line,
 * and an order into deep water moved to the dry ground the aim preview shows. Screenshots go to work/cmdui-shots. Never included in releases.
 */
public final class ReplySuite implements CommandUiChecks.Suite {
    static volatile String overlay = "";
    static volatile int overlays;
    static boolean hooked;
    final int map = Integer.getInteger("gravesandglory.replyMap", envInt("GRAVES_REPLY_MAP", 6));
    int stage, at, refusalsBefore, heldBefore;
    UUID id;
    String empty;
    long readyBefore;
    int[] water;
    double[] clientDry;
    Vec3d rawHit;
    final List<String> shots = new ArrayList<>();
    Path dir;

    static int envInt(String k, int d) { try { return Integer.parseInt(System.getenv(k)); } catch (Exception e) { return d; } }

    @Override public int limit() { return 6000; }

    @Override public void tick(MinecraftClient c, int tick) {
        var b = GravesAndGlory.battle;
        if (tick == 1) {
            id = c.player.getUuid();
            dir = Path.of(System.getProperty("gravesandglory.repoRoot", ".")).resolve("work").resolve("cmdui-shots");
            try { Files.createDirectories(dir); } catch (Exception ignored) { }
            if (!hooked) { hooked = true; ClientReceiveMessageEvents.GAME.register((m, isOverlay) -> { if (isOverlay) { overlay = m.getString(); overlays++; } }); }
            CommandUiChecks.server(c, x -> ReplyProbe.switchMap(x, map));
            return;
        }
        switch (stage) {
            case 0 -> { if (tick > 20 && ReplyProbe.onMap(b, map)) { CommandUiChecks.seatAndStart(c); stage = 1; at = tick; } else if (tick > 3000) fail(c, "cu-reply-map", "map " + map + " not ready"); }
            case 1 -> { if (tick - at >= 60) { CommandUiChecks.report("cu-reply-battle", GravesAndGloryClient.battleActive(), "battle on map " + map); next(tick); } }
            // A: an accepted order says what it did in words
            case 2 -> { if (tick == at + 1) { clear(c); CommandSelection.select("all", false); CommandSelection.order("follow", false); }
                        if (tick == at + 6) shot(c, "reply-accepted.png");
                        if (tick == at + 10) { CommandUiChecks.report("cu-reply-accepted", "All: follow me".equals(overlay) && CommandNet.Client.refusals == 0, "action bar '" + overlay + "', refusals " + CommandNet.Client.refusals); next(tick); } }
            // B: a second press inside the command cooldown is held and then sent
            case 3 -> { if (tick == at + 1) { clear(c); heldBefore = ReplyProbe.heldSent(); CommandSelection.order("follow", false); }
                        if (tick == at + 2) CommandSelection.order("retreat", false);
                        if (tick == at + 20) { String o = orderOf(b); CommandUiChecks.report("cu-reply-held", ReplyProbe.heldSent() == heldBefore + 1 && "retreat".equals(o) && overlay.endsWith("fall back to me"), "held sends " + (ReplyProbe.heldSent() - heldBefore) + ", stored " + ReplyProbe.stored(b, id) + ", action bar '" + overlay + "'"); next(tick); } }
            // C: the round intro
            case 4 -> { if (tick == at + 1) { clear(c); CommandUiChecks.server(c, x -> ReplyProbe.intro(x, 400)); }
                        if (tick == at + 4) CommandSelection.order("follow", false);
                        if (tick == at + 12) { refusal("cu-reply-intro", "intro", "Wait for the horn."); CommandUiChecks.server(c, x -> ReplyProbe.intro(x, 0)); next(tick); } }
            // D: down, and the respawn card says the troops keep their orders
            case 5 -> { if (tick == at + 1) { clear(c); CommandUiChecks.server(c, x -> ReplyProbe.down(x, id, 600)); }
                        if (tick == at + 4) CommandSelection.order("attack", false);
                        if (tick == at + 12) refusal("cu-reply-down", "down", null);
                        if (tick == at + 40) { shot(c, "reply-down-card.png"); CommandUiChecks.report("cu-reply-down-card", BattleHud.rects().containsKey("respawn") && ReplyProbe.respawnSeconds() > 0, "respawn card drawn " + BattleHud.rects().containsKey("respawn") + " at " + box(BattleHud.rects().get("respawn")) + ", " + ReplyProbe.respawnSeconds() + " s"); }
                        if (tick == at + 42) { CommandUiChecks.server(c, x -> ReplyProbe.down(x, id, 0)); next(tick); } }
            // E: a Charge with an empty group is no_troops and starts no cooldown
            case 6 -> { if (tick == at + 1) { clear(c); empty = null; CommandUiChecks.server(c, x -> { for (String g : new String[]{"siege", "support", "cavalry", "ranged"}) if (empty == null && ReplyProbe.living(x, id, g) == 0) empty = g; readyBefore = ReplyProbe.chargeReadyAt(x, id); }); }
                        if (tick == at + 4) { if (empty == null) { CommandUiChecks.report("cu-reply-no-troops", false, "no empty group to order"); next(tick); return; } CommandSelection.select(empty, false); CommandSelection.order("charge", false); }
                        if (tick == at + 6) ReplyProbe.confirm();
                        if (tick == at + 14) { long ready = ReplyProbe.chargeReadyAt(b, id); refusal("cu-reply-no-troops", "no_troops", null);
                            CommandUiChecks.report("cu-reply-empty-charge-no-cooldown", ready == readyBefore && ready <= ReplyProbe.time(b) && CommandNet.Client.chargeTicksLeft() == 0, "group " + empty + ": chargeReadyAt " + readyBefore + " -> " + ready + " at " + ReplyProbe.time(b) + ", client ticks left " + CommandNet.Client.chargeTicksLeft() + ", line '" + ReplyProbe.lastAnswer() + "'");
                            CommandSelection.select("all", false); next(tick); } }
            // F: out_of_range and target_gone from the server
            case 7 -> { if (tick == at + 1) { clear(c); CommandUiChecks.server(c, x -> ReplyProbe.order(x, id, "all", "hold", 9999, 9999, -1)); }
                        if (tick == at + 8) refusal("cu-reply-out-of-range", "out_of_range", "Too far to order.");
                        if (tick == at + 9) { clear(c); CommandUiChecks.server(c, x -> ReplyProbe.order(x, id, "all", "attack", 0, 0, 1 << 30)); }
                        if (tick == at + 16) { refusal("cu-reply-target-gone", "target_gone", "That enemy is gone."); next(tick); } }
            // G: deep water: the server moves the point to dry ground, the same cell the client's preview uses
            case 8 -> { if (tick == at + 1) { clear(c); water = null; CommandUiChecks.server(c, x -> water = ReplyProbe.deepNear(x, c.player.getX(), c.player.getZ())); }
                        if (tick == at + 4) { if (water == null) { CommandUiChecks.report("cu-reply-water", false, "no deep water in the arena of map " + map); next(tick); return; }
                            double px = c.player.getX(); clientDry = ReplyProbe.clientGround(c.world, water[0] + .5, water[1] + .5, px);
                            CommandUiChecks.server(c, x -> ReplyProbe.order(x, id, "all", "hold", water[0] + .5, water[1] + .5, -1)); }
                        if (tick == at + 12) { Vec3d t = ReplyProbe.target(b, id, "infantry"); if (t == null) t = ReplyProbe.target(b, id, "ranged");
                            boolean ok = CommandNet.Client.refusals == refusalsBefore && t != null && !ReplyProbe.deep(t.x, t.z) && clientDry != null && Math.abs(t.x - clientDry[0]) < .01 && Math.abs(t.z - clientDry[1]) < .01;
                            CommandUiChecks.report("cu-reply-water", ok, "water " + water[0] + "," + water[1] + ": server target " + (t == null ? "-" : String.format("%.2f,%.2f", t.x, t.z)) + " (deep " + (t != null && ReplyProbe.deep(t.x, t.z)) + "), client " + (clientDry == null ? "-" : String.format("%.2f,%.2f", clientDry[0], clientDry[1])) + ", refusals " + (CommandNet.Client.refusals - refusalsBefore) + ", line '" + overlay + "'");
                            next(tick); } }
            // H: the aim preview over water stands on the snapped ground
            case 9 -> { if (tick == at + 1) { clear(c); CommandUiChecks.server(c, x -> ReplyProbe.stand(x, id, clientDry[0], clientDry[1])); }
                        if (tick == at + 20) { Vec3d eye = c.player.getEyePos(), to = new Vec3d(water[0] + .5, ReplyProbe.groundY(water[0], water[1]) + .2, water[1] + .5).subtract(eye);
                            float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90), pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
                            c.player.setYaw(yaw); c.player.setPitch(pitch); c.player.prevYaw = yaw; c.player.prevPitch = pitch; c.player.setHeadYaw(yaw); c.player.prevHeadYaw = yaw;
                            CommandSelection.select("all", false); CommandSelection.order("hold", false); }
                        if (tick == at + 26) { var hit = c.player.raycast(gg.gravesandglory.Spec.COMMANDINTERFACE[0].targetRange(), 1, false); rawHit = hit.getType() == HitResult.Type.MISS ? null : hit.getPos(); shot(c, "reply-water-aim.png"); }
                        if (tick == at + 27) { Vec3d d = ReplyProbe.aimed(); boolean rawDeep = rawHit != null && ReplyProbe.deep(rawHit.x, rawHit.z);
                            double[] want = rawHit == null ? null : ReplyProbe.clientGround(c.world, rawHit.x, rawHit.z, c.player.getX());
                            boolean ok = rawDeep && d != null && !ReplyProbe.deep(d.x, d.z) && want != null && Math.abs(d.x - want[0]) < .01 && Math.abs(d.z - want[1]) < .01;
                            CommandUiChecks.report("cu-reply-water-aim", ok, "raw aim " + (rawHit == null ? "-" : String.format("%.1f,%.1f", rawHit.x, rawHit.z)) + " deep " + rawDeep + ", preview anchor " + (d == null ? "-" : String.format("%.2f,%.2f", d.x, d.z)) + " deep " + (d != null && ReplyProbe.deep(d.x, d.z)));
                            gg.gravesandglory.CommandSelection.reset(); next(tick); } }
            default -> CommandUiChecks.finish(c);
        }
    }

    void next(int tick) { stage++; at = tick; }
    void clear(MinecraftClient c) { overlay = ""; ReplyProbe.clearAnswer(); refusalsBefore = CommandNet.Client.refusals; CommandUiChecks.server(c, x -> ReplyProbe.clearCooldown(x, id)); }
    String orderOf(GravesAndGlory.Battle b) { for (String g : new String[]{"infantry", "ranged", "cavalry", "support", "siege"}) { String o = ReplyProbe.orderOf(b, id, g); if (o != null) return o; } return null; }
    void refusal(String check, String code, String line) {
        String shown = ReplyProbe.lastAnswer(), bar = ReplyProbe.overlayText(); // the client writes a refusal itself (CommandMode.answer), so no message event carries it: read the HUD's own action bar
        boolean ok = CommandNet.Client.refusals == refusalsBefore + 1 && code.equals(CommandNet.Client.lastRefusal) && shown.equals(bar)
            && (line == null ? !shown.isEmpty() && !shown.contains("{") : shown.equals(line));
        CommandUiChecks.report(check, ok, "refusals " + (CommandNet.Client.refusals - refusalsBefore) + ", code " + CommandNet.Client.lastRefusal + ", line '" + shown + "', action bar '" + bar + "'");
    }
    void fail(MinecraftClient c, String check, String why) { CommandUiChecks.report(check, false, why); stage = 99; }
    static String box(int[] r) { return r == null ? "-" : r[0] + "," + r[1] + " " + r[2] + "x" + r[3]; }
    void shot(MinecraftClient c, String name) {
        try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) { img.writeTo(dir.resolve(name)); shots.add(name); System.out.println("[reply] shot " + dir.resolve(name)); }
        catch (Exception e) { System.out.println("[reply] shot " + name + " failed " + e); }
    }
    @Override public void cleanup(MinecraftClient c) { CommandSelection.reset(); System.out.println("[reply] shots " + shots); }
}
