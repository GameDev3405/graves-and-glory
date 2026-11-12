package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Ebb visual capture (GRAVES_PHASE4_CHECKS=ebb-shots, for Kort's keep/fix page): starts oath-4, oath-5 and oath-6, frames one magic caster and
 * shoots it 1.5 s before its flicker and mid-flicker, then a spell bolt fired by a caster mid-flicker and, for comparison, one fired outside a
 * flicker. Each shot reports the caster's client glow at capture. Capture only; the profile is put back afterwards.
 */
final class EbbShotChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    static final String[] IDS = {"oath-4", "oath-5", "oath-6"};
    record Pending(String name, int entity, boolean wantDim) {}
    final ConcurrentLinkedQueue<Pending> queue = new ConcurrentLinkedQueue<>();
    volatile UUID player; volatile boolean done; boolean armed;
    int which, stage, delay; long at, window, cooldown; String savedProfile; GravesAndGlory.Unit caster; boolean boltEbb, boltNormal;

    public int limit() { return 40000; }
    public void cleanup(MinecraftClient c) {
        Phase4Checks.server(c, b -> {
            if (b.running) b.campaign.returnLobby();
            if (savedProfile != null && player != null) { b.campaign.profiles.put(player, Campaign.JSON.fromJson(savedProfile, Profile.class)); b.campaign.save(player); }
        });
    }
    public void step(MinecraftClient c, int tick) {
        if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); }
        if (tick > 20) c.options.hudHidden = true;
        if (delay > 0) { delay--; return; }
        if (!armed && queue.peek() != null) { armed = true; delay = 2; return; } // let the teleport land and render
        Pending s = queue.poll();
        if (s != null) {
            armed = false;
            var e = c.world.getEntityById(s.entity);
            float glow = e == null ? -1 : CampaignClient.ebbGlow(e);
            boolean ok = e != null && (s.wantDim ? glow < 1f : glow == 1f);
            Phase4Checks.report("p4-ebb-" + s.name, ok, "caster " + s.entity + " client glow " + glow + (s.wantDim ? " (want dimmed)" : " (want full)"));
            Phase4Checks.shot(c, "ebb-" + s.name);
            delay = 2;
        }
        if (done && queue.isEmpty()) { c.options.hudHidden = false; Phase4Checks.finish(c, "ebb-shots"); }
    }

    /** Puts the player 5 blocks out and 2.5 up from the point, looking at it. */
    static void frame(ServerPlayerEntity p, Vec3d at, double side) {
        double ox = 5 * Math.cos(side), oz = 5 * Math.sin(side);
        Vec3d eye = at.add(ox, 2.5, oz);
        double dx = at.x - eye.x, dy = at.y + 0.8 - eye.y, dz = at.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        p.networkHandler.requestTeleport(eye.x, eye.y - p.getStandingEyeHeight(), eye.z, yaw, pitch);
    }

    void next(GravesAndGlory.Battle b, String why) {
        if (why != null) Phase4Checks.report("p4-ebb-" + IDS[which] + "-skipped", false, why);
        if (b.running) b.campaign.returnLobby();
        which++; stage = 0; at = b.time;
        if (which >= IDS.length) done = true;
    }

    public void serverTick(GravesAndGlory.Battle b) {
        if (player == null || done) return;
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        var c = b.campaign;
        String id = IDS[which];
        var row = MarchMath.row(id, Spec.CAMPAIGNBATTLES);
        long wt = b.world.getTime();
        switch (stage) {
            case 0 -> {
                if (b.running) { if (b.time - at > 100) { done = true; Phase4Checks.report("p4-ebb-round", false, "a round was already running"); } return; }
                if (savedProfile == null) savedProfile = Campaign.JSON.toJson(c.profile(player));
                var pr = c.profile(player);
                for (var r : Spec.CAMPAIGNBATTLES) if (r.order() < row.order()) pr.battlesWon.add(r.id());
                pr.battlesWon.remove(id); pr.sanitize();
                c.closeParty(); c.battleMode = Modes.DOMINATION;
                b.command(p, "campaign:" + id);
                stage = 1; at = b.time; caster = null; boltEbb = boltNormal = false;
            }
            case 1 -> {
                if (b.time - at > 15000) { next(b, id + " did not start"); return; }
                if (!b.running || !b.ready || b.time < c.introUntil) return;
                p.getAbilities().invulnerable = true; p.sendAbilitiesUpdate();
                b.world.setTimeOfDay(18000); // night, so the glow reads (the ebb keys on world time, not time of day)
                for (var u : b.units) if (u.spec.magic() && u.mob.isAlive()) { caster = u; break; }
                if (caster == null) { if (b.time - at > 15400) next(b, "no magic caster on the field"); return; }
                window = -1;
                for (long t = wt + 60; t < wt + 60 + 2 * MarchMath.Ebb.PERIOD; t++) if (MarchMath.Ebb.starts(true, caster.mob.getId(), t)) { window = t; break; }
                stage = 2; at = b.time;
            }
            case 2 -> { // before (1.5 s ahead of the flicker) and mid-flicker (the LOW plateau)
                if (!b.running) { next(b, "the round ended before the caster shots"); return; }
                if (!caster.mob.isAlive()) { stage = 1; return; }
                if (wt % 2 == 0) frame(p, caster.mob.getPos(), 0.7);
                if (wt == window - 32) queue.add(new Pending(id + "-before", caster.mob.getId(), false));
                if (wt == window + 12) { queue.add(new Pending(id + "-during", caster.mob.getId(), true)); stage = 3; at = b.time; }
            }
            case 3 -> { // a bolt in flight from a caster mid-flicker, and one from a caster outside a flicker
                if (!b.running) { next(b, (boltEbb ? "" : "no bolt from a flickering caster; ") + (boltNormal ? "" : "no bolt outside a flicker; ") + "round ended"); return; }
                if (b.time - at > 6000) { next(b, (boltEbb ? "" : "no bolt from a flickering caster; ") + (boltNormal ? "" : "no bolt outside a flicker") + " within 6000 ticks"); return; }
                if (b.time < cooldown) return;
                for (var s : b.shots) {
                    if (!"bolt".equals(s.kind()) || s.shooter() == null || !s.shooter().spec.magic() || b.time - s.fired() != 1 || s.lands() - s.fired() < 8) continue;
                    boolean dim = BattleEffects.ebbing(b, s.shooter());
                    if (dim ? boltEbb : boltNormal) continue;
                    // side-on to the caster-to-target line, a third of the way along it, so the bolt crosses the frame
                    Vec3d from = s.shooter().mob.getPos(), to = s.target() != null ? s.target().getPos() : from.add(s.shooter().mob.getRotationVector().multiply(8));
                    Vec3d along = to.subtract(from);
                    Vec3d mid = from.add(along.multiply(0.35));
                    frame(p, mid, Math.atan2(along.z, along.x) + Math.PI / 2);
                    cooldown = b.time + 30;
                    queue.add(new Pending(id + (dim ? "-bolt-during" : "-bolt-outside"), s.shooter().mob.getId(), dim));
                    if (dim) boltEbb = true; else boltNormal = true;
                    break;
                }
                if (boltEbb && boltNormal) next(b, null);
            }
            default -> {}
        }
    }
}
