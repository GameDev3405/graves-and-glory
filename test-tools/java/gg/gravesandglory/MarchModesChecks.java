package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

/**
 * The oath plays the mode its battle row names (GRAVES_PHASE4_CHECKS=march-modes). Starts oath-2 (Survival on Hollowwood), oath-3
 * (Siege on Ironhold), oath-4 (Convoy on Frostmarch) and oath-5 (Domination on Hollowwood) with the campaign command (GRAVES_MARCH_MODES=<ids> picks others), as the War Table does, on a profile that has won the earlier battles; checks the
 * round runs that mode on that map, that the march bonus tracker is fed by the real round (a fallen troop counts for lose_at_most),
 * that the battle can be finished and won, and that its bonus pays. Each battle's enemy squads field exactly its enemies rows by lane
 * (MarchBonusChecks.enemyMismatch, chapter 1 brief N1; oath-1 and oath-6 are checked there). The profile is put back afterwards.
 */
final class MarchModesChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    static final String[] IDS = System.getenv("GRAVES_MARCH_MODES") != null ? System.getenv("GRAVES_MARCH_MODES").split(",") : new String[] {"oath-2", "oath-3", "oath-4", "oath-5"};
    volatile UUID player; volatile boolean done; volatile String shot;
    int which, stage; long at; String savedProfile; boolean killed;

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
        if (done) Phase4Checks.finish(c, "march-modes");
    }
    void end(String why) { if (why != null) Phase4Checks.report("p4-march-modes-round", false, why); done = true; }

    public void serverTick(GravesAndGlory.Battle b) {
        if (player == null || done) return;
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        var c = b.campaign;
        var row = Spec.CAMPAIGNBATTLES[0];
        for (var r : Spec.CAMPAIGNBATTLES) if (r.id().equals(IDS[which])) row = r;
        String id = row.id();
        switch (stage) {
            case 0 -> { // start the battle from the campaign command on a profile that has cleared the battles before it
                if (b.running) { if (b.time - at > 100) end("a round was already running"); return; }
                if (savedProfile == null) savedProfile = Campaign.JSON.toJson(c.profile(player));
                var pr = c.profile(player);
                for (var r : Spec.CAMPAIGNBATTLES) if (r.order() < row.order()) pr.battlesWon.add(r.id());
                pr.battlesWon.remove(id); pr.bonusClaimed.remove(row.bonus()); pr.sanitize();
                if (c.social.room(player) != null) b.command(p, "social:exit"); // a suite run before (siege-play) can leave the player in a room, and the March is solo
                c.closeParty(); c.battleMode = Modes.DOMINATION;
                b.command(p, "campaign:" + id);
                stage = 1; at = b.time; killed = false;
            }
            case 1 -> {
                if (b.time - at > 15000) { end(id + " did not start within 15000 ticks"); return; }
                if (!b.running || c.activeBattle == null || !b.ready) return;
                boolean ok = c.activeBattle.id().equals(id) && b.mode.id().equals(row.mode()) && c.map == row.map() && c.bonusTracker != null;
                Phase4Checks.report("p4-march-modes-start-" + id, ok, "activeBattle=" + c.activeBattle.id() + " mode=" + b.mode.id() + " (row " + row.mode() + ") map=" + c.map + " (row " + row.map() + ") tracker=" + (c.bonusTracker == null ? "none" : c.bonusTracker.kind));
                String foes = MarchBonusChecks.enemyMismatch(b, row);
                Phase4Checks.report("p4-march-enemies-" + id, foes.isEmpty(), foes.isEmpty() ? "team 1 fields " + row.enemies() + " by lane at " + row.difficulty() + "; no off-chapter Legendary" : foes);
                if (!ok) { end(null); return; }
                p.getAbilities().invulnerable = true; p.sendAbilitiesUpdate();
                stage = 2; at = b.time;
            }
            case 2 -> { // the tracker is fed by the real round: a troop of the commander's banner falls (siege) / the wagon is moved (convoy)
                if (b.time < c.introUntil) return;
                if (b.mode instanceof SiegeMode || b.mode instanceof SurvivalMode || b.mode.id().equals(Modes.DOMINATION)) {
                    if (!killed) {
                        for (var u : b.units) if (u.squad.team == 0 && !u.squad.bot && !u.temporary && u.mob.isAlive()) { u.mob.kill(); killed = true; break; }
                        if (!killed && b.time - at > 1200) { Phase4Checks.report("p4-march-modes-lost-" + id, false, "no troop of the commander's banner to fell"); end(null); }
                        return;
                    }
                    if (c.bonusTracker.lost < 1 && b.time - at < 1400) return;
                    Phase4Checks.report("p4-march-modes-lost-" + id, c.bonusTracker.lost >= 1, "tracker lost=" + c.bonusTracker.lost);
                    if (b.mode instanceof SiegeMode m) { shot = "march-siege"; m.fall(b); } else shot = b.mode instanceof SurvivalMode ? "march-survival" : "march-domination";
                    stage = 3; at = b.time;
                } else if (b.mode instanceof ConvoyMode m) {
                    m.distance = m.total() - 1.5; shot = "march-convoy"; stage = 3; at = b.time;
                } else { end("unexpected mode " + b.mode.id()); }
            }
            case 3 -> { // finish the battle: Siege takes the gate and the keep, Convoy is pushed home, Survival clears its last wave
                if (b.mode instanceof SiegeMode m) {
                    if (m.stage == 1 && m.target != null && b.time % 20 == 0) m.target.damage(p.getDamageSources().playerAttack(p), 400);
                    if (m.stage == 2 || m.stage == 3) { if (b.running) c.finish(0); }
                } else if (b.mode instanceof ConvoyMode m) {
                    FConvoy.beside(b, p, m);
                } else if (b.mode instanceof SurvivalMode m) { // forced: once the first wave has risen, every wave is cleared
                    if (m.current >= 1 && m.current < SurvivalMode.WAVES.size()) { m.current = SurvivalMode.WAVES.size(); SurvivalChecks.killDead(b, m); }
                } else if (b.running) c.finish(0); // Domination (oath-5): forced win for Dawn after one troop fell
                if (!b.running) stage = 4;
                else if (b.time - at > 6000) end(id + " did not finish within 6000 ticks of the push");
            }
            case 4 -> { // won and paid
                Houses.forceRise = null; // killDead forced it for Survival
                var pr = c.profile(player);
                boolean won = c.lastWinner == 0 && pr.battlesWon.contains(id);
                boolean paid = pr.bonusClaimed.contains(row.bonus());
                Phase4Checks.report("p4-march-modes-won-" + id, won, "lastWinner=" + c.lastWinner + " battlesWon=" + pr.battlesWon);
                Phase4Checks.report("p4-march-modes-bonus-" + id, paid, "bonus " + row.bonus() + " claimed=" + pr.bonusClaimed);
                c.returnLobby();
                which++; stage = 0; at = b.time;
                if (which >= IDS.length) end(null);
            }
            default -> {}
        }
    }
    static final class FConvoy { static void beside(GravesAndGlory.Battle b, ServerPlayerEntity p, ConvoyMode m) { ConvoyChecks.beside(b, p, m); } }
}
