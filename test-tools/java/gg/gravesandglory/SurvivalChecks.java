package gg.gravesandglory;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

/**
 * Survival on Hollowwood Reach (GRAVES_PHASE4_CHECKS=survival). Round 1, co-op: the player and a second human banner seated on
 * Dusk both defend on Dawn, team 1 has no banner, and wave 1 rises scaled for two banners. Round 2, solo: the rule, clock, court
 * (base and barracks), night and flags down; the soul-fire warning, then wave 1 at its sites with the director's Graveborn tier; a
 * cleared wave pays its gold; the JSON block; the dead holding the shrine with no defender for its seconds win. Round 3: the last
 * wave cleared wins for the defenders, and teardown puts the time, the flags and the court back with no dead left. Then march-3
 * (The Long Night) starts Survival on map 3 from the campaign command. Screenshots p4-survival-rise and p4-survival-wave.
 */
final class SurvivalChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    static final int MAP = 3;
    volatile UUID player; volatile boolean done; volatile String shot;
    final UUID ally = UUID.nameUUIDFromBytes("gravesandglory:survival-check-ally".getBytes());
    int stage; long at, timeBefore; int gold0; String savedProfile;

    public int limit() { return 30000; }
    public void cleanup(MinecraftClient c) {
        Phase4Checks.server(c, b -> {
            Houses.forceRise = null;
            if (b.running) b.campaign.returnLobby();
            b.squads.remove(ally);
            if (savedProfile != null && player != null) { b.campaign.profiles.put(player, Campaign.JSON.fromJson(savedProfile, Profile.class)); b.campaign.save(player); }
        });
    }
    public void step(MinecraftClient c, int tick) {
        if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); }
        String s = shot;
        if (s != null) { c.options.hudHidden = false; if (s.startsWith("!")) { Phase4Checks.shot(c, s.substring(1)); shot = null; } else shot = "!" + s; }
        if (done) Phase4Checks.finish(c, "survival");
    }
    void end(String why) { if (why != null) Phase4Checks.report("p4-surv-round", false, why); done = true; }

    /** Starts a Survival round on map 3 as the lobby does; with coop a second human banner sits on Dusk first. */
    boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, boolean coop) {
        var c = b.campaign;
        if (b.running) c.returnLobby();
        if (c.map != MAP) { c.switchMap(MAP); return false; }
        if (!b.ready || !b.squads.containsKey(p.getUuid())) return false;
        c.battleMode = Modes.SURVIVAL; c.difficulty = "normal";
        c.closeParty(); c.partyLeader = p.getUuid(); c.party.add(p.getUuid()); c.practice = !coop; c.size = coop ? 2 : 1; c.fill = true;
        b.squads.remove(ally);
        for (var s : b.squads.values()) if (!s.bot) { s.seated = s.owner.equals(p.getUuid()); if (s.seated) s.team = 0; }
        if (coop) { var a = new GravesAndGlory.Squad(ally, 1, b.fieldBase(1)); a.seated = true; b.squads.put(ally, a); }
        timeBefore = b.world.getTimeOfDay();
        b.startRound(); c.introUntil = b.time; return true;
    }
    /** Troops of wave 1 for this many banners at Normal (waveScale 1.0). */
    static int wave1(int banners) {
        int n = 0;
        for (var g : SurvivalMode.groups(SurvivalMode.WAVES.get(0))) n += ModeMath.waveCount(g.count(), banners, SurvivalMode.R.survivalPerBanner(), 1.0);
        return n;
    }
    static void killDead(GravesAndGlory.Battle b, SurvivalMode m) {
        Houses.forceRise = false;
        for (var u : new java.util.ArrayList<>(b.units)) if (u.squad == m.director && u.mob.isAlive()) u.mob.kill();
    }
    static int tagged(GravesAndGlory.Battle b) { return b.world.getEntitiesByType(TypeFilter.instanceOf(MobEntity.class), e -> e.getCommandTags().contains(SurvivalMode.TAG)).size(); }

    public void serverTick(GravesAndGlory.Battle b) {
        if (player == null || done) return;
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        if (stage == 0 || stage == 10 || stage == 20) {
            if (at == 0) at = b.time;
            if (b.time - at > 12000) { end("Hollowwood Reach did not build"); return; }
            if (!start(b, p, stage == 0)) return;
            p.getAbilities().invulnerable = true; p.sendAbilitiesUpdate();
            stage++; at = b.time; return;
        }
        if (stage < 40 && !b.running && stage != 14 && stage != 15 && stage != 22 && stage != 23) { end("round ended early at stage " + stage + ", winner " + b.campaign.lastWinner); return; }
        if (stage < 40 && !(b.mode instanceof SurvivalMode)) { end("mode is " + b.mode.id()); return; }
        var m = stage < 40 ? (SurvivalMode) b.mode : null;
        switch (stage) {
            case 1 -> { // co-op: both humans defend, no Dusk banner, wave 1 scaled for two banners
                boolean seated = true; int team1 = 0, defenders = 0;
                for (var s : b.squads.values()) if (s.seated) { if (s.team == 1) team1++; else defenders++; if (!s.bot && s.team != 0) seated = false; }
                Phase4Checks.report("p4-surv-coop", seated && team1 == 0 && defenders == 2 && m.banners == 2 && b.squads.get(ally).team == 0, "humans on Dawn " + seated + ", Dusk banners " + team1 + ", defenders " + defenders + ", counted " + m.banners);
                stage = 2; at = b.time;
            }
            case 2 -> {
                if (m.current < 1) { if (b.time - at > 400) end("co-op wave 1 never rose"); return; }
                Phase4Checks.report("p4-surv-coop-counts", m.risen == wave1(2) && m.standing(b) + m.pending.size() == wave1(2), "risen " + m.risen + ", standing " + m.standing(b) + " queued " + m.pending.size() + ", want " + wave1(2));
                stage = 10; at = 0;
            }
            case 11 -> { // solo: the frame's numbers
                var court = m.court(b);
                Vec3d base = b.fieldBase(0), barracks = b.fieldBarracks(0);
                Phase4Checks.report("p4-surv-sheet", m.rule().equals("Survival: 7 waves") && m.ruleN() == SurvivalMode.WAVES.size() && b.roundSeconds == 5 + 760 && m.banners == 1,
                        "rule " + m.rule() + ", roundSeconds " + b.roundSeconds + ", banners " + m.banners);
                Phase4Checks.report("p4-surv-sites", Math.abs(base.x - court.x()) < 1e-6 && Math.abs(base.z - court.z()) < 1e-6 && !m.capturesPoints(b) && Math.abs(b.world.getTimeOfDay() % 24000 - SurvivalMode.NIGHT) < 200 && m.timeBefore == timeBefore,
                        "base " + base + ", court " + court.x() + "," + court.z() + ", time " + b.world.getTimeOfDay());
                Phase4Checks.report("p4-surv-barracks", barracks.equals(base) && b.campaign.size == 1 && b.squads.values().stream().filter(s -> s.seated).count() == 1, "barracks " + barracks + ", seated " + b.squads.values().stream().filter(s -> s.seated).count());
                stage = 12; at = b.time;
            }
            case 12 -> { // the warning: soul fire, no dead yet; then wave 1 at its sites with the tier's trait
                if (b.time - at == 40) { Phase4Checks.report("p4-surv-warning", m.riseAt > 0 && m.current == 0 && m.standing(b) == 0 && m.clock(b) <= 5 && m.clock(b) >= 1, "riseAt " + m.riseAt + ", standing " + m.standing(b) + ", clock " + m.clock(b));
                    var s = SurvivalMode.rise(MAP, "chapel"); p.teleport(p.getServerWorld(), s.x() + 8, b.world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) s.x() + 8, (int) s.z()) + 1, s.z() + 8, 135, 20); shot = "survival-rise"; }
                if (m.current < 1) { if (b.time - at > 400) end("solo wave 1 never rose"); return; }
                boolean atSites = true;
                for (var u : b.units) if (u.squad == m.director) {
                    boolean near = false;
                    for (var key : SurvivalMode.sites(SurvivalMode.WAVES.get(0))) { var s = SurvivalMode.rise(MAP, key); if (GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(), new Vec3d(s.x(), 0, s.z())) <= (s.radius() + 3) * (s.radius() + 3)) near = true; }
                    if (!near) atSites = false;
                }
                Phase4Checks.report("p4-surv-rise", m.risen == wave1(1) && m.standing(b) == wave1(1) && atSites && m.director.traits.getOrDefault("graveborn", 0) == 1 && !b.squads.containsKey(SurvivalMode.DIRECTOR),
                        "risen " + m.risen + " standing " + m.standing(b) + " (want " + wave1(1) + "), at sites " + atSites + ", trait " + m.director.traits);
                shot = "survival-wave"; stage = 13; at = b.time;
            }
            case 13 -> { // a cleared wave pays every defending banner
                if (b.time - at < 20) return;
                if (b.time - at == 20) { gold0 = b.squads.get(player).gold; killDead(b, m); return; }
                if (!m.cleared) { if (b.time - at > 200) end("wave 1 never cleared, standing " + m.standing(b)); return; }
                int gold = b.squads.get(player).gold;
                Phase4Checks.report("p4-surv-gold", gold >= gold0 + SurvivalMode.WAVES.get(0).gold() && gold <= gold0 + SurvivalMode.WAVES.get(0).gold() + wave1(1) * Spec.ECONOMY[0].killGold() + 10, "gold " + gold0 + " to " + gold);
                var j = m.snapshot(b, b.squads.get(player));
                Phase4Checks.report("p4-surv-json", j.get("wave").getAsInt() == 1 && j.get("waves").getAsInt() == 7 && j.has("nextIn") && j.get("side").getAsString().equals("defend") && !j.get("points").getAsBoolean()
                        && m.formats()[0].equals("fraction") && m.ledger(b)[0] == 1 && m.max(b)[0] == 7, j.toString());
                long t0 = System.nanoTime(); for (int i = 0; i < 200; i++) { m.shrineCount(b); m.standing(b); } long per = (System.nanoTime() - t0) / 200;
                Phase4Checks.report("p4-surv-tickcost", per < 200_000, per + " ns per shrine and standing count");
                // the next wave, then the dead alone on the shrine
                m.waveEnds = b.time + SurvivalMode.WARNING_TICKS + 20;
                stage = 14; at = b.time;
            }
            case 14 -> { // the dead hold the shrine with no defender: the round is lost after its seconds
                if (m.current < 2) { if (b.time - at > 400) end("wave 2 never rose"); return; }
                Vec3d c = new Vec3d(m.shrine(b).x(), 0, m.shrine(b).z());
                double far = m.shrine(b).radius() + 30;
                p.teleport(p.getServerWorld(), c.x + far, b.world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) (c.x + far), (int) c.z) + 1, c.z, 90, 0);
                boolean one = false;
                for (var u : b.units) if (u.mob.isAlive()) {
                    if (u.squad == m.director && !one) { one = true; u.mob.teleport(c.x + 1, SurvivalMode.surface(b, c.x + 1, c.z).y, c.z); }
                    else if (u.squad != m.director && GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(), c) < far * far) u.mob.teleport(c.x - far, SurvivalMode.surface(b, c.x - far, c.z).y, c.z);
                }
                if (b.running) { if (b.time - at > (m.shrine(b).seconds() + 30) * 20L) end("the shrine was never lost, lost " + m.lost); return; }
                Phase4Checks.report("p4-surv-end-lost", b.campaign.lastWinner == 1, "winner " + b.campaign.lastWinner + ", lost " + m.lost);
                stage = 15; at = b.time;
            }
            case 15 -> { b.campaign.returnLobby(); stage = 20; at = 0; }
            case 21 -> { // the last wave cleared wins
                if (m.current < 1) { if (b.time - at > 400) end("round 3 wave 1 never rose"); return; }
                m.current = SurvivalMode.WAVES.size(); killDead(b, m); stage = 22; at = b.time;
            }
            case 22 -> {
                if (b.running) { if (b.time - at > 200) end("the last wave cleared but the round runs on"); return; }
                Phase4Checks.report("p4-surv-end-won", b.campaign.lastWinner == 0, "winner " + b.campaign.lastWinner);
                stage = 23; at = b.time;
            }
            case 23 -> { // teardown: the time, the flags, no dead
                b.campaign.returnLobby();
                var a = b.points.get(0).spec;
                boolean flag = b.world.getBlockState(new BlockPos(a.x(), a.elevation() + 1, a.z())).isOf(Blocks.OAK_FENCE);
                Phase4Checks.report("p4-surv-teardown", b.world.getTimeOfDay() == timeBefore && tagged(b) == 0 && flag && !m.pole, "time " + b.world.getTimeOfDay() + " of " + timeBefore + ", dead left " + tagged(b) + ", flag A " + flag + ", pole " + m.pole);
                Houses.forceRise = null;
                stage = 40; at = b.time;
            }
            case 40 -> { // The Long Night starts Survival on its map from the campaign command
                var c = b.campaign;
                if (savedProfile == null) savedProfile = Campaign.JSON.toJson(c.profile(player));
                var pr = c.profile(player);
                Spec.CampaignBattlesRow row = null;
                for (var r : Spec.CAMPAIGNBATTLES) if (r.id().equals("march-3")) row = r;
                if (row == null) { end("no march-3 row"); return; }
                for (var r : Spec.CAMPAIGNBATTLES) if (r.order() < row.order()) pr.battlesWon.add(r.id());
                pr.battlesWon.remove(row.id()); pr.sanitize();
                c.closeParty(); c.battleMode = Modes.DOMINATION;
                b.command(p, "campaign:march-3");
                stage = 41; at = b.time;
            }
            case 41 -> {
                if (b.time - at > 15000) { end("march-3 did not start"); return; }
                var c = b.campaign;
                if (!b.running || c.activeBattle == null || !b.ready) return;
                Phase4Checks.report("p4-surv-march", c.activeBattle.id().equals("march-3") && b.mode instanceof SurvivalMode && c.map == MAP, "activeBattle " + c.activeBattle.id() + ", mode " + b.mode.id() + ", map " + c.map);
                c.returnLobby(); stage = 42; end(null);
            }
            default -> {}
        }
    }
}
