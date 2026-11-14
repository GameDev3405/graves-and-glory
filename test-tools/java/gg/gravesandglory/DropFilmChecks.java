package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.util.*;

/**
 * Films the drop board from the roller's side (Kort, 2026-11-14; one shoot only). GRAVES_PHASE1_CHECKS=drop-film. The commander stands on
 * the altar's court; a unit roll and then a Forge roll each play start to finish while every rendered frame is written to a video
 * (DropFilm), the HUD as the game shows it in the cinematic. The roll is repeated (profile topped up each time, harness only, no
 * change to odds) until the coin lands in x5 or x10, or the result is Rare or better, up to GRAVES_FILM_TRIES times; the best take is kept.
 * GRAVES_FILM_OUT is the folder (default work/media/dropboard), GRAVES_FILM_W/H the size (2560x1440), GRAVES_FILM_KINDS "unit,gear".
 * The profile is put back at the end. Never shipped.
 */
final class DropFilmChecks implements Phase1Checks.Suite {
    static final int[] MULT = {10, 5, 3, 2, 1, 2, 3, 5, 10};
    static final int W = DropFilm.envInt("GRAVES_FILM_W", 2560), H = DropFilm.envInt("GRAVES_FILM_H", 1440), FPS = 60, TRIES = DropFilm.envInt("GRAVES_FILM_TRIES", 10);
    static final int WANT = DropFilm.envInt("GRAVES_FILM_WANT", 5); // a take scoring this much or more is kept at once

    volatile UUID player; volatile String backup; volatile boolean ready, restored;
    final List<String> kinds = new ArrayList<>(Arrays.asList(System.getenv().getOrDefault("GRAVES_FILM_KINDS", "unit,gear").split(",")));
    final File out = new File(System.getenv().getOrDefault("GRAVES_FILM_OUT", System.getProperty("gravesandglory.repoRoot", ".") + "/work/media/dropboard"));
    int stage, stageAt, attempt, aimTicks, tail, best, bestAttempt, k;
    String kind = "", last = "";
    boolean sent;
    String bestNote = "";
    final List<String> phases = new ArrayList<>();
    final List<String> summary = new ArrayList<>();

    static void say(String line) { System.out.println("[p1] drop-film: " + line); }
    void next(int tick, int s) { stage = s; stageAt = tick; last = ""; phases.clear(); aimTicks = 0; tail = 0; sent = false; }
    File take(String kind, int n) { return new File(out, "roller-" + kind + "-try" + n + ".mp4"); }

    @Override public void cleanup(MinecraftClient c) {
        DropFilm.stop();
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

    /** What the result is worth to a camera: the multiplier of its slot, and a Rare or better result. */
    static int score(DropClient.View v) {
        int s = v.slot() >= 0 && v.slot() < MULT.length ? MULT[v.slot()] : 0;
        String r = DropRender.rarity(v);
        if (r.equals("rare") || r.equals("epic") || r.equals("legendary")) s += 5;
        return s;
    }

    public void step(MinecraftClient c, int tick) {
        if (tick == 2 || tick == 40) DropFilm.prepare(c, W, H, FPS);
        switch (stage) {
            case 0 -> { // the profile copy, the commander onto the court
                if (tick == 20) {
                    c.setScreen(null);
                    UUID id = c.player.getUuid();
                    Phase1Checks.server(c, b -> {
                        player = id; backup = Campaign.JSON.toJson(b.campaign.profile(id));
                        var p = b.server.getPlayerManager().getPlayer(id);
                        var at = DropStage.at(0, 2, 1);
                        p.teleport(b.world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, DropLayout.viewYaw(), -20);
                        ready = true;
                    });
                }
                if (ready && tick - 20 > 60) {
                    boolean size = c.getFramebuffer().textureWidth >= 1920 && c.getFramebuffer().textureHeight >= 1080;
                    Phase1Checks.report("p1-drop-film-size", size, "framebuffer " + c.getFramebuffer().textureWidth + "x" + c.getFramebuffer().textureHeight + " (wanted " + W + "x" + H + ", at least 1920x1080)");
                    out.mkdirs();
                    kind = kinds.isEmpty() ? "" : kinds.remove(0);
                    attempt = 0; best = -1; bestAttempt = 0;
                    next(tick, kind.isEmpty() ? 9 : 1);
                }
            }
            case 1 -> { // top the purse up, start the file
                Phase1Checks.server(c, b -> { var pr = b.campaign.profile(player); pr.troopWrits = 10; pr.gearShards = 400; b.campaign.save(player); });
                attempt++;
                DropFilm.start(c, take(kind, attempt), FPS, 90);
                next(tick, 2);
            }
            case 2 -> { // standing on the court for a moment, then the roll goes
                if (!DropFilm.live) { stageAt = tick; return; }
                k = tick - stageAt;
                if (k == 30) {
                    rollBefore = CampaignClient.lastRoll == null ? -1 : CampaignClient.lastRoll.n();
                    forgeBefore = CampaignClient.forgeRoll;
                    GravesAndGloryClient.send(kind.equals("unit") ? "drop:roll:" + Spec.HOUSES[0].id() : "drop:forge:helm");
                    next(tick, 3);
                }
            }
            case 3 -> { // the drop, to its end and a second past it
                k = tick - stageAt;
                String p = DropClient.phase();
                if (!p.equals(last)) { last = p; if (!p.isEmpty()) phases.add(p); }
                if (p.equals("AIM")) {
                    int a = ++aimTicks;
                    if (a == 12) DropClient.aim(2.0);
                    if (a == 45) DropClient.aim(6.5);
                    if (a == 80) DropClient.release();
                }
                if (DropClient.mine != null && DropClient.mine.path != null) { var m = DropClient.mine; lastView = m; }
                boolean over = k > 20 && !DropClient.cinematic();
                if (over) tail++;
                if (tail > 50 || k > 900) {
                    String st = DropFilm.stop();
                    int sc = lastView == null ? 0 : score(lastView);
                    String note = "attempt " + attempt + " slot " + (lastView == null ? "?" : MULT[lastView.slot()] + " (index " + lastView.slot() + ")") + " rarity " + (lastView == null ? "?" : DropRender.rarity(lastView)) + " score " + sc;
                    say(kind + ": " + note + ", phases " + phases + (over ? "" : " (NEVER ENDED)"));
                    boolean seq = phases.containsAll(List.of("FLY", "AIM", "DROP", "FLARE", "REVEAL", "RETURN"));
                    java.util.regex.Matcher sm = java.util.regex.Pattern.compile("\"slow\":\"([^\"]*)\"").matcher(st);
                    boolean stall = false; // a stall after the first moments is a stutter in the take
                    if (sm.find()) for (String e : sm.group(1).trim().split(" ")) { String[] kv = e.split("="); if (kv.length == 2 && Integer.parseInt(kv[0]) > 45 && Integer.parseInt(kv[1]) > 100) stall = true; }
                    if (stall) { say("take discarded: it stalled (" + sm.group(1) + ")"); sc = Math.min(sc, 0); }
                    if (!seq || !over) { say("take discarded: the sequence is incomplete"); take(kind, attempt).delete(); }
                    else if (stall) take(kind, attempt).delete();
                    else if (sc > best) { if (bestAttempt > 0) take(kind, bestAttempt).delete(); best = sc; bestAttempt = attempt; bestNote = note + "; " + st; }
                    else take(kind, attempt).delete();
                    lastView = null;
                    if ((best >= WANT) || attempt >= TRIES) next(tick, 4); else next(tick, 1);
                    stageAt = tick;
                }
            }
            case 4 -> { // keep the best take under its final name
                if (tick - stageAt < 30) return; // the cinematic's War Table reopen settles
                File f = take(kind, bestAttempt), fin = new File(out, "roller-" + kind + ".mp4");
                boolean ok = bestAttempt > 0 && f.exists();
                if (ok) { fin.delete(); new File(fin.getPath() + ".json").delete(); ok = f.renameTo(fin); new File(f.getPath() + ".json").renameTo(new File(fin.getPath() + ".json")); }
                Phase1Checks.report("p1-drop-film-" + kind, ok, "kept " + (ok ? fin.getName() : "nothing") + " after " + attempt + " attempt(s): " + bestNote + (best >= WANT ? " (picked for a high slot or a Rare+ result)" : " (best of the tries, not a high one)"));
                summary.add(kind + ": " + bestNote);
                kind = kinds.isEmpty() ? "" : kinds.remove(0);
                attempt = 0; best = -1; bestAttempt = 0;
                next(tick, kind.isEmpty() ? 9 : 1);
            }
            case 9 -> {
                if (tick - stageAt == 5) Phase1Checks.server(c, b -> restore());
                if (tick - stageAt > 30) {
                    try { java.nio.file.Files.write(new File(out, "roller-picks.txt").toPath(), summary); } catch (Exception ignored) {}
                    stage = 99; Phase1Checks.finish(c, "drop-film");
                }
            }
            default -> {}
        }
    }

    long rollBefore; Object forgeBefore; DropClient.View lastView;
}
