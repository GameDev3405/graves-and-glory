package gg.gravesandglory;

import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import java.util.*;

/**
 * Real-client checks for the gear foundation (plan 2026-11-03-gear-foundation, Task 8): gear_v1 reaches the client, a Forge roll goes through the
 * ORDER channel, saves and syncs, a forged request is refused and changes nothing, 120 pieces stay out of the camp snapshot yet reach the client,
 * and a seated commander in a battle cannot roll. GRAVES_PHASE1_CHECKS=gear. The profile is put back from a JSON copy taken at the start. Never shipped.
 */
final class GearChecks implements Phase1Checks.Suite {
    static final String INVALID = "That gear request is not valid.", LOCKED = "Active battle choices are locked until the match ends.";
    volatile UUID player;
    volatile String backup, serverBefore, snapshotWhy = "", diskWhy = "";
    volatile boolean ready, snapshotDone, snapshotOk, diskDone, diskOk, battleReady, restored;
    int stage, stageAt, piecesBefore, serialBefore;

    void next(int tick, int s) { stage = s; stageAt = tick; }
    static void say(String line) { System.out.println("[p1] gear: " + line); }

    @Override public void cleanup(MinecraftClient c) {
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
        restored = true;
    }

    public void step(MinecraftClient c, int tick) {
        switch (stage) {
            case 0 -> { // take the profile copy, then wait for gear_v1
                if (tick == 20) {
                    c.setScreen(null);
                    UUID id = c.player.getUuid();
                    Phase1Checks.server(c, b -> { player = id; backup = Campaign.JSON.toJson(b.campaign.profile(id)); ready = true; });
                }
                if (ready) next(tick, 1);
            }
            case 1 -> { // p1-gear-sync
                var g = CampaignClient.gear;
                boolean ok = g != null && g.equipped().size() == 6 && !g.relicSlots().isEmpty() && !g.relicSlots().get(0).isEmpty();
                if (ok || tick - stageAt > 100) {
                    Phase1Checks.report("p1-gear-sync", ok, g == null ? "no gear_v1 packet within 100 ticks" : g.equipped().size() + " equipped, relic slot 0 '" + g.relicSlots().get(0) + "', " + g.pieces().size() + " pieces");
                    if (ok) {
                        Phase1Checks.server(c, b -> { var pr = b.campaign.profile(player); pr.gearShards = 200; b.campaign.save(player); });
                        next(tick, 2);
                    } else {
                        Phase1Checks.finish(c, "gear");
                        stage = 99;
                    }
                }
            }
            case 2 -> { // wait for the 200 marks to reach the client, then roll a helm
                var g = CampaignClient.gear;
                if (g != null && g.gearShards() == 200) {
                    piecesBefore = g.pieces().size();
                    GravesAndGloryClient.send("forge:roll:helm");
                    next(tick, 3);
                } else if (tick - stageAt > 100) {
                    Phase1Checks.report("p1-gear-roll", false, "the client never saw 200 gear shards (has " + (g == null ? "no gear" : g.gearShards()) + ")");
                    next(tick, 5);
                }
            }
            case 3 -> { // p1-gear-roll
                var g = CampaignClient.gear;
                boolean grew = g != null && g.pieces().size() == piecesBefore + 1 && g.gearShards() == 125;
                if (grew && !diskDone) {
                    diskDone = true;
                    int want = piecesBefore + 1;
                    Phase1Checks.server(c, b -> {
                        try {
                            var onDisk = Campaign.JSON.fromJson(java.nio.file.Files.readString(b.campaign.directory.resolve(player + ".json")), Profile.class);
                            diskOk = onDisk.gearShards == 125 && onDisk.gear.size() == want;
                            diskWhy = "file has " + onDisk.gearShards + " marks and " + onDisk.gear.size() + " pieces, wanted 125 and " + want;
                        } catch (Exception e) { diskWhy = e.toString(); }
                    });
                }
                if (grew && diskDone && !diskWhy.isEmpty()) {
                    Phase1Checks.report("p1-gear-roll", diskOk, "client pieces " + piecesBefore + " -> " + g.pieces().size() + ", marks " + g.gearShards() + "; " + diskWhy);
                    Phase1Checks.server(c, b -> serverBefore = Campaign.JSON.toJson(b.campaign.profile(player)));
                    next(tick, 4);
                } else if (tick - stageAt > 100) {
                    Phase1Checks.report("p1-gear-roll", false, "within 100 ticks the client saw " + (g == null ? "no gear" : g.pieces().size() + " pieces (wanted " + (piecesBefore + 1) + "), marks " + g.gearShards() + " (wanted 125)"));
                    next(tick, 5);
                }
            }
            case 4 -> { // p1-gear-forged: two forged requests, each answered with the refusal, the profile untouched
                if (serverBefore == null) return;
                int t = tick - stageAt;
                if (t == 1) { serialBefore = CampaignClient.noticeSerial; GravesAndGloryClient.send("gear:equip:999999"); }
                if (t == 40) { serialBefore = CampaignClient.noticeSerial; GravesAndGloryClient.send("forge:breakdown:abc"); }
                if (t == 80) {
                    boolean noticeOk = CampaignClient.notice.equals(INVALID) && CampaignClient.noticeSerial > serialBefore;
                    Phase1Checks.server(c, b -> {
                        snapshotWhy = "profile " + (Campaign.JSON.toJson(b.campaign.profile(player)).equals(serverBefore) ? "unchanged" : "CHANGED");
                        snapshotOk = snapshotWhy.endsWith("unchanged");
                        snapshotDone = true;
                    });
                    stageData = noticeOk;
                    next(tick, 41);
                }
            }
            case 41 -> {
                if (!snapshotDone) return;
                Phase1Checks.report("p1-gear-forged", snapshotOk && stageData, "notice '" + CampaignClient.notice + "' (serial up " + (CampaignClient.noticeSerial > serialBefore) + "), " + snapshotWhy);
                snapshotDone = false;
                next(tick, 5);
            }
            case 5 -> { // p1-gear-snapshot: 120 pieces stay out of the camp snapshot and still reach the client
                Phase1Checks.server(c, b -> {
                    var pr = b.campaign.profile(player);
                    String longest = "";
                    for (var it : Spec.GEARITEMS) if (it.id().length() > longest.length()) longest = it.id();
                    pr.gear.clear();
                    pr.equipped.clear();
                    pr.inventorySize = 120;
                    for (int i = 0; i < 120; i++) pr.gear.add(new Gear.Piece(300000 + i, longest, "common", 50, 100));
                    pr.nextGearUid = 300120;
                    var p = b.server.getPlayerManager().getPlayer(player);
                    String s = b.campaign.snapshot(p);
                    boolean pretty = s.contains("\n");
                    boolean hasGear = s.length() > 2 && JsonParser.parseString(s).getAsJsonObject().getAsJsonObject("profile").has(Profile.GEAR);
                    snapshotOk = pretty && !hasGear && s.length() <= Campaign.SNAPSHOT_LIMIT;
                    snapshotWhy = "snapshot " + s.length() + " of " + Campaign.SNAPSHOT_LIMIT + " characters, pretty=" + pretty + ", gear key present=" + hasGear;
                    snapshotDone = true;
                });
                next(tick, 51);
            }
            case 51 -> {
                var g = CampaignClient.gear;
                boolean synced = g != null && g.pieces().size() == 120;
                if (snapshotDone && (synced || tick - stageAt > 100)) {
                    Phase1Checks.report("p1-gear-snapshot", snapshotOk && synced, snapshotWhy + "; the client holds " + (g == null ? "no gear" : g.pieces().size() + " pieces") + " (wanted 120)");
                    snapshotDone = false;
                    next(tick, 6);
                }
            }
            case 6 -> { // p1-gear-battle-lock: seated in a practice battle, a roll is refused
                if (!battleReady) {
                    Phase1Checks.server(c, b -> {
                        var p = b.server.getPlayerManager().getPlayer(player);
                        if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true;
                    });
                    if (tick - stageAt > 600) { Phase1Checks.report("p1-gear-battle-lock", false, "the practice battle never started"); next(tick, 7); }
                    return;
                }
                next(tick, 61);
            }
            case 61 -> {
                int t = tick - stageAt;
                if (t == 5) { serialBefore = CampaignClient.noticeSerial; GravesAndGloryClient.send("forge:roll:helm"); }
                if (t > 5 && CampaignClient.noticeSerial > serialBefore || t > 120) {
                    Phase1Checks.report("p1-gear-battle-lock", CampaignClient.notice.equals(LOCKED), "notice '" + CampaignClient.notice + "'");
                    next(tick, 7);
                }
            }
            case 7 -> { // put the profile back, leave the battle, finish
                if (tick - stageAt == 1) Phase1Checks.server(c, b -> restore());
                if (restored || tick - stageAt > 100) { Phase1Checks.finish(c, "gear"); stage = 99; }
            }
            default -> { }
        }
    }
    volatile boolean stageData;
}
