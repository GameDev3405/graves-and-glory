package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

/**
 * The march hold_all (march-1, Domination) and kill_unit (march-6, Siege) bonuses in real rounds (GRAVES_PHASE4_CHECKS=march-bonus).
 * Each battle starts with the campaign command on a profile that has won the earlier battles. hold_all: the commander's side is given
 * every point and the tracker must notice within a second. kill_unit: the Hollow King must stand in the enemy army and the player's own
 * blow strikes him down; the tracker must notice. The round is then won and the bonus must be claimed. Forced steps are named in the report.
 */
final class MarchBonusChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    static final String[] IDS = System.getenv("GRAVES_MARCH_BONUS") != null ? System.getenv("GRAVES_MARCH_BONUS").split(",") : new String[] {"march-1", "march-6"};
    volatile UUID player; volatile boolean done; volatile String shot;
    int which, stage; long at; String savedProfile; boolean spawnedKing, kingForced;

    public int limit() { return 30000; }
    public void cleanup(MinecraftClient c) {
        Phase4Checks.server(c, b -> {
            if (b.running) b.campaign.returnLobby();
            if (savedProfile != null && player != null) { b.campaign.profiles.put(player, Campaign.JSON.fromJson(savedProfile, Profile.class)); b.campaign.save(player); }
        });
    }
    public void step(MinecraftClient c, int tick) {
        if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); }
        String s = shot;
        if (s != null) { c.options.hudHidden = false; if (s.startsWith("!")) { Phase4Checks.shot(c, s.substring(1)); shot = null; } else shot = "!" + s; }
        if (done) Phase4Checks.finish(c, "march-bonus");
    }
    static String banners(GravesAndGlory.Battle b) { var out = new StringBuilder(); for (var sq : b.squads.values()) if (sq.team == 1) out.append("[lane " + sq.lane + " bot=" + sq.bot + " tier=" + sq.tier + " " + sq.banner + "]"); return out.toString(); }
    void end(String why) { if (why != null) Phase4Checks.report("p4-march-bonus-round", false, why); done = true; }

    public void serverTick(GravesAndGlory.Battle b) {
        if (player == null || done) return;
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        var c = b.campaign;
        var row = Spec.CAMPAIGNBATTLES[0];
        for (var r : Spec.CAMPAIGNBATTLES) if (r.id().equals(IDS[which])) row = r;
        String id = row.id();
        switch (stage) {
            case 0 -> {
                if (b.running) { if (b.time - at > 100) end("a round was already running"); return; }
                if (savedProfile == null) savedProfile = Campaign.JSON.toJson(c.profile(player));
                var pr = c.profile(player);
                for (var r : Spec.CAMPAIGNBATTLES) if (r.order() < row.order()) pr.battlesWon.add(r.id());
                pr.battlesWon.remove(id); pr.bonusClaimed.remove(row.bonus()); pr.sanitize();
                c.closeParty(); c.battleMode = Modes.DOMINATION;
                b.command(p, "campaign:" + id);
                stage = 1; at = b.time;
            }
            case 1 -> {
                if (b.time - at > 15000) { end(id + " did not start within 15000 ticks"); return; }
                if (!b.running || c.activeBattle == null || !b.ready) return;
                boolean ok = c.activeBattle.id().equals(id) && b.mode.id().equals(row.mode()) && c.map == row.map() && c.bonusTracker != null;
                Phase4Checks.report("p4-march-bonus-start-" + id, ok, "activeBattle=" + c.activeBattle.id() + " mode=" + b.mode.id() + " map=" + c.map + " tracker=" + (c.bonusTracker == null ? "none" : c.bonusTracker.kind));
                if (!ok) { end(null); return; }
                p.getAbilities().invulnerable = true; p.sendAbilitiesUpdate();
                stage = 2; at = b.time;
            }
            case 2 -> {
                if (b.time < c.introUntil) return;
                if (c.bonusTracker.kind.equals("hold_all")) {
                    if (b.time - at < 60) return; // let the round settle
                    for (var pt : b.points) pt.owner = 0; // forced: the whole map to Dawn
                    if (b.time % 20 == 0 && c.bonusTracker.heldAll || b.time - at > 400) {
                        Phase4Checks.report("p4-march-bonus-hold-" + id, c.bonusTracker.heldAll, "all " + b.points.size() + " points forced to Dawn; tracker heldAll=" + c.bonusTracker.heldAll);
                        shot = "march-hold"; stage = 3; at = b.time;
                    }
                } else if (c.bonusTracker.kind.equals("kill_unit")) {
                    GravesAndGlory.Unit king = null;
                    for (var u : b.units) if (u.spec.id().equals(c.bonusTracker.target) && u.mob.isAlive()) king = u;
                    if (king == null) {
                        if (b.time - at > 1200) { Phase4Checks.report("p4-march-bonus-kill-" + id, false, "no " + c.bonusTracker.target + " in the army; units=" + b.units.size() + "; team 1 banners " + banners(b)); end(null); }
                        return;
                    }
                    king.mob.damage(p.getDamageSources().playerAttack(p), 10000f); // the player's own blow
                    if (!king.mob.isAlive() || b.time - at > 1500) {
                        stage = 21; at = b.time;
                    }
                } else end("unexpected bonus kind " + c.bonusTracker.kind);
            }
            case 21 -> {
                if (b.time - at < 40) return;
                Phase4Checks.report("p4-march-bonus-kill-" + id, c.bonusTracker.killed, "Hollow King fielded by the enemy army; "+"struck down by the player's blow; tracker killed=" + c.bonusTracker.killed);
                shot = "march-kill"; stage = 3; at = b.time;
            }
            case 3 -> { // finish: win for Dawn
                if (b.time - at < 40) return;
                if (b.mode instanceof SiegeMode m) {
                    if (m.stage == 0) { m.fall(b); return; }
                    if (m.stage == 1) { if (m.target != null && b.time % 20 == 0) m.target.damage(p.getDamageSources().playerAttack(p), 400); }
                    if (m.stage == 2 || m.stage == 3) { if (b.running) c.finish(0); }
                } else if (b.running) c.finish(0);
                if (!b.running) stage = 4;
                else if (b.time - at > 6000) end(id + " did not finish");
            }
            case 4 -> {
                var pr = c.profile(player);
                boolean won = c.lastWinner == 0 && pr.battlesWon.contains(id);
                boolean paid = pr.bonusClaimed.contains(row.bonus());
                Phase4Checks.report("p4-march-bonus-won-" + id, won, "lastWinner=" + c.lastWinner);
                Phase4Checks.report("p4-march-bonus-paid-" + id, paid, "bonus " + row.bonus() + " claimed=" + pr.bonusClaimed + " gold=" + pr.gold);
                shot = "march-result-" + id;
                stage = 5; at = b.time;
            }
            case 5 -> {
                if (b.time - at < 80) return;
                c.returnLobby();
                which++; stage = 0; at = b.time;
                if (which >= IDS.length) end(null);
            }
            default -> {}
        }
    }
}
