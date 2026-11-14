package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import java.util.*;

/**
 * Real-client checks for the drop board (design 2026-11-14). GRAVES_PHASE1_CHECKS=drop-board. The commander stands on the altar's court;
 * a unit roll goes through the whole sequence (fly in, aim, release, the fall, the flare, the reveal, the return) with a shot at each
 * step and the coin's largest frame-to-frame step measured; a gear roll is released at once and skipped in the pegs. Both rolls are paid
 * and saved as they are sent, and their cards reach the War Table as the drop ends. The profile is put back at the end. Never shipped.
 */
final class DropBoardChecks implements Phase1Checks.Suite {
    volatile UUID player;
    volatile String backup;
    volatile boolean ready, restored, court;
    int stage, stageAt, dropTicks, shots;
    long rollBefore;
    Object forgeBefore;
    String last = "";
    final List<String> phases = new ArrayList<>();
    double worstStep;

    void next(int tick, int s) { stage = s; stageAt = tick; last = ""; phases.clear(); dropTicks = 0; }
    static void say(String line) { System.out.println("[p1] drop-board: " + line); }

    @Override public void cleanup(MinecraftClient c) {
        if (restored || player == null || c.getServer() == null) return;
        try { c.getServer().submit(this::restore).join(); } catch (RuntimeException e) { say("restore in cleanup failed: " + e); }
    }

    void restore() {
        var b = GravesAndGlory.battle;
        if (b == null || backup == null) return;
        b.campaign.profiles.put(player, Campaign.JSON.fromJson(backup, Profile.class));
        b.campaign.save(player);
        b.campaign.forgeResults.remove(player);
        restored = true;
    }

    /** Follows the cinematic's phases; a shot as each begins and two in the pegs. */
    void watch(MinecraftClient c, String name) {
        String p = DropClient.phase();
        if (!p.equals(last)) {
            last = p;
            if (!p.isEmpty()) { phases.add(p); if (!p.equals("WAIT") && !p.equals("FLY")) Phase1Checks.shot(c, "drop-board-" + name + "-" + p.toLowerCase(Locale.ROOT)); }
        }
        if (p.equals("FLY") && DropClient.mine != null && DropClient.now() - DropClient.mine.began > 0.8 && shots++ == 0) Phase1Checks.shot(c, "drop-board-" + name + "-fly");
        if (p.equals("DROP")) { dropTicks++; if (dropTicks == 25 || dropTicks == 60) Phase1Checks.shot(c, "drop-board-" + name + "-pegs-" + dropTicks); }
    }

    public void step(MinecraftClient c, int tick) {
        switch (stage) {
            case 0 -> { // the profile copy, the purse, and the commander onto the court
                if (tick == 20) {
                    c.setScreen(null);
                    UUID id = c.player.getUuid();
                    Phase1Checks.server(c, b -> {
                        player = id; backup = Campaign.JSON.toJson(b.campaign.profile(id));
                        var pr = b.campaign.profile(id); pr.troopWrits = 10; pr.gearShards = 400; b.campaign.save(id);
                        var p = b.server.getPlayerManager().getPlayer(id);
                        var at = DropStage.at(0, 2, 1);
                        p.teleport(b.world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, DropLayout.viewYaw(), -20);
                        court = b.world.getBlockState(DropStage.markerPos()).isOf(DropStage.MARKER) && DropStage.built(b.world);
                        ready = true;
                    });
                }
                if (ready && tick - 20 > 60) {
                    boolean ok = court && DropAltar.drawn > 0;
                    Phase1Checks.report("p1-drop-board-court", ok, "court built " + court + ", the altar was drawn " + DropAltar.drawn + " time(s), at " + DropStage.markerPos().toShortString());
                    Phase1Checks.shot(c, "drop-board-court");
                    rollBefore = CampaignClient.lastRoll == null ? -1 : CampaignClient.lastRoll.n();
                    GravesAndGloryClient.send("drop:roll:" + Spec.HOUSES[0].id());
                    next(tick, 1);
                }
            }
            case 1 -> { // the unit roll, start to finish
                int k = tick - stageAt;
                watch(c, "unit");
                if (last.equals("AIM")) {
                    int a = ++dropTicks; // ticks in the aim (dropTicks is free until the fall)
                    if (a == 10) DropClient.aim(2.0);
                    if (a == 30) { Phase1Checks.shot(c, "drop-board-unit-aimed"); DropClient.aim(6.5); }
                    if (a == 50) { dropTicks = 0; DropClient.release(); }
                }
                if (last.equals("WAIT")) dropTicks = 0;
                boolean over = k > 20 && !DropClient.cinematic();
                if (over || k > 600) {
                    var r = CampaignClient.lastRoll;
                    boolean card = r != null && r.n() != rollBefore, paid = CampaignClient.profile.troopWrits == 10 - Campaign.ROLL_WRITS;
                    boolean seq = phases.containsAll(List.of("FLY", "AIM", "DROP", "FLARE", "REVEAL", "RETURN"));
                    worstStep = DropClient.lastMaxStep;
                    Phase1Checks.report("p1-drop-board-unit", over && card && paid && seq, "phases " + phases + ", over after " + k + " ticks, card " + (r == null ? "none" : r.troop()) + (card ? " (new)" : " (not new)") + ", writs " + CampaignClient.profile.troopWrits);
                    Phase1Checks.report("p1-drop-board-smooth", over && DropClient.frames > 100 && worstStep < 0.35, "the coin's largest step between frames " + String.format(Locale.ROOT, "%.3f", worstStep) + " blocks over " + DropClient.frames + " frames (limit 0.35)");
                    forgeBefore = CampaignClient.forgeRoll;
                    next(tick, 2);
                }
            }
            case 2 -> { // a gear roll: let go at once, skipped in the pegs
                int k = tick - stageAt;
                if (k == 30) GravesAndGloryClient.send("drop:forge:helm");
                if (k < 30) return;
                watch(c, "gear");
                if (last.equals("AIM") && DropClient.mine != null && !DropClient.mine.released) DropClient.release();
                if (last.equals("DROP") && dropTicks == 30) { DropClient.skip(c); stageAt = tick - 1000; }
                if (k >= 1000) { // skipped: it must be over at once
                    int since = k - 1000;
                    boolean over = !DropClient.cinematic();
                    if (!over && since < 30) return;
                    var g = CampaignClient.gear;
                    boolean card = CampaignClient.forgeRoll != null && CampaignClient.forgeRoll != forgeBefore;
                    if (!card && since < 40) return;
                    Phase1Checks.report("p1-drop-board-gear-skip", over && card && since <= 30, "phases " + phases + ", the skip ended it after " + since + " tick(s), forge card " + (card ? "arrived" : "missing") + ", shards " + (g == null ? "?" : g.gearShards()));
                    Phase1Checks.shot(c, "drop-board-gear-after-skip");
                    next(tick, 3);
                } else if (k > 500) { Phase1Checks.report("p1-drop-board-gear-skip", false, "the gear drop never reached the pegs: phases " + phases); next(tick, 3); }
            }
            case 3 -> {
                if (tick - stageAt == 10) Phase1Checks.server(c, b -> restore());
                if (tick - stageAt > 30) { stage = 99; Phase1Checks.finish(c, "drop-board"); }
            }
            default -> {}
        }
    }
}
