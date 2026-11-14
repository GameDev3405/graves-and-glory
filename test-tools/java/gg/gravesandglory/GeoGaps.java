package gg.gravesandglory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Capture only (GRAVES_GEO_CAPTURE=gaps, GRAVES_GEO_GAP=bash|charge): real gameplay, not the frozen synthetic captures of GeoKnightChecks.
 * bash: a level-3 knight's shield bash through Combat.resolve (Abilities.onHit -> bash: knockback and stun, then hit_v1), on a live victim of the
 *   troop under test, filmed at 000/045/090 from the hit until 10 ticks after the stun ends; the victim's AI is on again from the hit, so the exit is the real one.
 * charge: the player's squad of six of the troop under test gets a real Charge order (OrderRules.playerOrder) against three frozen foes 30 blocks ahead;
 *   filmed from the front-right three-quarter from the order until the squad stops, with a per-tick client state line [gc] and the sole points of the
 *   middle unit's feet (GeoTroopRenderer.PROBE) logged to gaps-foot-<troop>.csv every rendered frame, for the foot-slide measure.
 */
final class GeoGaps {
    static volatile GeoGaps live;
    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> { GeoGaps g = live; if (g != null) g.serverTick(); });
    }

    final GeoKnightChecks o;
    final String mode = System.getenv("GRAVES_GEO_GAP") == null ? "bash" : System.getenv("GRAVES_GEO_GAP");
    final String troop = GeoKnightChecks.TROOP;
    static final int H = 24;              // bash: the run tick of the blow
    static final int CHARGE_ORDER = 12;   // charge: the run tick of the order
    static final int CHARGE_LEN = 260;    // charge: run ticks filmed after the order
    int run;                              // the angle index (bash)
    int runStart;                         // the stage tick the current run began
    boolean armed;

    // server side
    volatile GravesAndGlory.Battle battle;
    volatile GravesAndGlory.Unit victim, attacker;
    final List<GravesAndGlory.Unit> squad = new ArrayList<>();
    final List<GravesAndGlory.Unit> foes = new ArrayList<>();
    volatile int phase;                   // 0 idle, 1 bash set up, 2 bash struck, 3 charge set up, 4 charge ordered
    volatile long hitTime = -1, stunUntil = -1;
    volatile String snap = "";
    volatile double fx, fz;               // facing vector of the bash victim
    volatile float vyaw;
    volatile int probeId = -1;
    final StringBuilder foot = new StringBuilder();
    /** Per rendered frame of the probed unit: the model-space position of each listed bone's pivot, a point 1 block in front of it (-z) and a point 1 block below it (-y). */
    final StringBuilder pose = new StringBuilder();
    static final String[] POSE_BONES = {"torso", "head", "right_arm", "left_arm", "right_hand", "left_hand"};
    static final int WOUND = Integer.parseInt(System.getenv().getOrDefault("GRAVES_GEO_WOUND", "0"));
    static final boolean EVERY_TICK = System.getenv("GRAVES_GEO_EVERYTICK") != null;
    /** GRAVES_GEO_HOLDYAW=1: the bash victim's server yaw is held at its start facing, so the stun exit can be judged without the AI's turn toward its target. */
    static final boolean HOLDYAW = System.getenv("GRAVES_GEO_HOLDYAW") != null;
    long lastProbe = Long.MIN_VALUE; float lastTd = -1;
    String lastStun = "";

    GeoGaps(GeoKnightChecks o) { this.o = o; live = this; }

    // ---------------------------------------------------------------- client

    /** Returns true when the whole capture is done. k counts client ticks since stage 32 began. */
    boolean step(MinecraftClient c, int k) {
        if (k == 1) { c.setScreen(null); c.options.hudHidden = true; }
        if (mode.equals("charge")) return stepCharge(c, k);
        return stepBash(c, k);
    }

    final double[] camHist = {Double.NaN, 0, 0, 0, 0, 0};
    final ArrayDeque<double[]> hist = new ArrayDeque<>();
    /** The bash camera lags the server position by this many ticks (the client draws the victim about 3 ticks behind it) and sits DIST blocks off. */
    static final int LAG = Integer.parseInt(System.getenv().getOrDefault("GRAVES_GEO_BASHLAG", "5"));
    static final double DIST = Double.parseDouble(System.getenv().getOrDefault("GRAVES_GEO_BASHDIST", "6.5"));
    /** The charge camera offset from the followed unit: dx, dz, yaw, dy (GRAVES_GEO_GAPCAM="3.2,4.2,143,0"). */
    static final double[] CAM = camOf(System.getenv("GRAVES_GEO_GAPCAM"));
    static double[] camOf(String s) {
        double[] d = {3.2, 4.2, 143, 0};
        if (s != null) { String[] a = s.split(","); for (int i = 0; i < a.length && i < 4; i++) d[i] = Double.parseDouble(a[i].trim()); }
        return d;
    }

    boolean stepBash(MinecraftClient c, int k) {
        if (run >= 3) { GeoTroopRenderer.PROBE = null; return true; }
        int rk = k - runStart;
        if (rk == 1) { armed = false; installProbe(c); /* its own probe every run, even over GeoDraws' chain from an earlier suite */ foot.setLength(0); pose.setLength(0); Phase1Checks.server(c, b -> setupBash(b, run)); }
        if (rk == H) Phase1Checks.server(c, this::strike);
        String ang = GeoKnightChecks.CAP_ANG[run];
        var e = c.world == null ? null : c.world.getEntityById(victimId);
        if (e != null && rk >= H - 6) {
            Gait g = Gait.LIVE.get(e);
            StanceController st = StanceController.LIVE.get(e);
            AttackController at = AttackController.LIVE.get(e);
            long wt = c.world.getTime();
            if (st != null && g != null) System.out.println(String.format("[gb] %s %s t=%03d wt=%d gait=%s sp=%.3f still=%.2f stance=%s up=%s@%.2f legs=%s@%.2f atk=%s@%.2f cam=%.1f,%.1f ent=%.1f,%.1f yaw=%.0f/%.0f/%.0f snap=%s",
                troop, ang, rk - H, wt, g.mode, g.speed, st.shownStill, st.shownStance, st.shownUpper, st.shownUpperWeight, st.shownLegs, st.shownLegsWeight,
                at == null ? "" : at.shownClip, at == null ? 0 : at.shownWeight, c.player.getX() - o.cx, c.player.getZ() - o.cz, e.getX() - o.cx, e.getZ() - o.cz, e.getYaw(), e instanceof LivingEntity ly ? ly.getBodyYaw() : 0f, e instanceof LivingEntity lh ? lh.getHeadYaw() : 0f, snap));
        }
        if (rk == H + 5 && c.world != null) for (var en : c.world.getEntities()) if (en instanceof LivingEntity le && !(en instanceof net.minecraft.entity.player.PlayerEntity))
            System.out.println(String.format("[gb] ent id=%d vic=%b %s pos=%.2f,%.2f,%.2f yaw=%.0f", en.getId(), en.getId() == victimId, en.getType().getUntranslatedName(), en.getX() - o.cx, en.getY(), en.getZ() - o.cz, en.getYaw()));
        long hit = hitTime, until = stunUntil;
        if (rk >= H - 3 && hit >= 0 && c.world != null) {
            int over = (int) (until - hit) + 12;
            if (rk <= H + over) Phase1Checks.shot(c, String.format("geo-%s-gap-bash-%s-%03d", troop, ang, rk - H + 10));
            if (rk > H + over + 2) { saveFoot(c, "bash-" + ang); run++; runStart = k; hitTime = -1; stunUntil = -1; }
        }
        if (rk > H + 200) { Phase1Checks.report("p1-geo-" + troop + "-gap-bash", false, "no bash landed (" + snap + ")"); run++; runStart = k; hitTime = -1; }
        return false;
    }

    volatile int victimId = -1;

    boolean stepCharge(MinecraftClient c, int k) {
        int rk = k - 2;
        if (rk == 1) { Phase1Checks.server(c, this::setupCharge); installProbe(c); }
        if (rk == CHARGE_ORDER) Phase1Checks.server(c, this::order);
        // a mender casts at the wounded: GRAVES_GEO_WOUND=<ticks> hurts three of her squad-mates that many ticks after the order, so the cast lands mid-run
        if (WOUND > 0 && rk == CHARGE_ORDER + WOUND) Phase1Checks.server(c, b -> { for (int j : new int[]{1, 3, 5}) squad.get(j).mob.setHealth(squad.get(j).mob.getMaxHealth() * 0.4f); });
        if (rk >= CHARGE_ORDER - 2 && rk <= CHARGE_ORDER + CHARGE_LEN) {
            if (EVERY_TICK || rk % 2 == 0) Phase1Checks.shot(c, String.format("geo-%s-gap-charge-%03d", troop, rk - CHARGE_ORDER + 10));
            for (int j : new int[]{2, 5}) {
                if (j >= squadIds.length) continue;
                var e = c.world == null ? null : c.world.getEntityById(squadIds[j]);
                if (e == null) continue;
                Gait g = Gait.LIVE.get(e);
                StanceController st = StanceController.LIVE.get(e);
                AttackController at = AttackController.LIVE.get(e);
                if (st != null && g != null) System.out.println(String.format("[gc] %s u%d t=%03d wt=%d gait=%s sp=%.3f stance=%s up=%s@%.2f legs=%s@%.2f still=%.2f atk=%s@%.2f %s",
                    troop, j, rk - CHARGE_ORDER, c.world.getTime(), g.mode, g.speed, st.shownStance, st.shownUpper, st.shownUpperWeight, st.shownLegs, st.shownLegsWeight, st.shownStill,
                    at == null ? "" : at.shownClip, at == null ? 0 : at.shownWeight, j == 2 ? snap : ""));
            }
        }
        if (rk > CHARGE_ORDER + CHARGE_LEN) {
            GeoTroopRenderer.PROBE = null;
            try { Files.writeString(Path.of(c.runDirectory.getPath(), "gaps-foot-" + troop + ".csv"), foot.toString()); } catch (IOException ex) { System.out.println("[gc] foot csv: " + ex); }
            try { Files.writeString(Path.of(c.runDirectory.getPath(), "gaps-pose-" + troop + ".csv"), pose.toString()); } catch (IOException ex) { System.out.println("[gc] pose csv: " + ex); }
            Phase1Checks.report("p1-geo-" + troop + "-gap-charge", phase == 4, "charge ordered, " + snap);
            return true;
        }
        return false;
    }

    volatile int[] squadIds = new int[0];

    void saveFoot(MinecraftClient c, String tag) {
        try { Files.writeString(Path.of(c.runDirectory.getPath(), "gaps-foot-" + troop + "-" + tag + ".csv"), foot.toString()); } catch (IOException ex) { System.out.println("[gb] foot csv: " + ex); }
        try { Files.writeString(Path.of(c.runDirectory.getPath(), "gaps-pose-" + troop + "-" + tag + ".csv"), pose.toString()); } catch (IOException ex) { System.out.println("[gb] pose csv: " + ex); }
        pose.setLength(0);
    }

    // ---------------------------------------------------------------- foot probe

    void installProbe(MinecraftClient c) {
        GeoTroopRenderer.Probe own = (Entity e, BakedGeoModel model, float td) -> {
            if (e.getId() != probeId || !(e instanceof LivingEntity le)) return;
            long wt = e.getWorld().getTime();
            if (wt == lastProbe && td == lastTd) return;
            lastProbe = wt; lastTd = td;
            double ex = MathHelper.lerp(td, e.lastRenderX, e.getX()), ey = MathHelper.lerp(td, e.lastRenderY, e.getY()), ez = MathHelper.lerp(td, e.lastRenderZ, e.getZ());
            float by = MathHelper.lerpAngleDegrees(td, le.prevBodyYaw, le.bodyYaw);
            double th = Math.toRadians(-by), cs = Math.cos(th), sn = Math.sin(th);
            // sole points relative to the ankle pivot, in blocks: the knight's boot spans z -3..2 and the archer's -2..2 (model px), bottom 2 px under the pivot
            double zToe = troop.equals("knight") ? -3 / 16.0 : -2 / 16.0, zHeel = 2 / 16.0, yy = 0;
            StringBuilder row = new StringBuilder(String.format(Locale.ROOT, "%.4f,%.5f,%.5f,%.5f,%.3f", wt + td, ex, ey, ez, by));
            for (String name : new String[]{"right_foot", "left_foot"}) {
                var bone = model.getBone(name).orElse(null);
                if (bone == null) return;
                var m = bone.getLocalSpaceMatrix();
                for (double zz : new double[]{zHeel, zToe}) {
                    Vector4f v = m.transform(new Vector4f(0f, (float) yy, (float) zz, 1f));
                    double wx = ex + v.x() * cs + v.z() * sn, wz = ez - v.x() * sn + v.z() * cs, wy = ey + v.y();
                    row.append(String.format(Locale.ROOT, ",%.5f,%.5f,%.5f", wx, wy, wz));
                }
            }
            foot.append(row).append('\n');
            StringBuilder pr = new StringBuilder(String.format(Locale.ROOT, "%.4f", wt + td));
            for (String name : POSE_BONES) {
                var bone = model.getBone(name).orElse(null);
                if (bone == null) { pr.append(",0,0,0,0,0,0,0,0,0"); continue; }
                var m = bone.getLocalSpaceMatrix();
                for (float[] pt : new float[][]{{0, 0, 0}, {0, 0, -1}, {0, -1, 0}}) {
                    Vector4f v = m.transform(new Vector4f(pt[0], pt[1], pt[2], 1f));
                    pr.append(String.format(Locale.ROOT, ",%.5f,%.5f,%.5f", v.x(), v.y(), v.z()));
                }
            }
            pose.append(pr).append('\n');
        };
        GeoTroopRenderer.PROBE = (e, model, td) -> { own.seen(e, model, td); GeoLayerRest.tap(model); };   // the layer-rest row reads these draws too
    }

    // ---------------------------------------------------------------- server

    GravesAndGlory.Squad[] sides(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        GravesAndGlory.Squad mine = b.squads.get(o.player), foe = null;
        for (var s : b.squads.values()) if (mine != null && s.team != mine.team) { foe = s; break; }
        return new GravesAndGlory.Squad[]{mine, foe};
    }

    void clear(GravesAndGlory.Battle b) {
        for (var u : b.units) { GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); }
        b.units.clear(); b.clearCorpses();
        squad.clear(); foes.clear(); o.knights.clear();
    }

    GravesAndGlory.Unit spawn(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String troopId, int slot, double x, double z, int level, int tier, float yaw) {
        var spec = Campaign.troop(troopId);
        var u = b.spawn(s, spec, 20 + slot, new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z));
        u.level = level; u.weaponTier = tier; u.armourTier = tier;
        RankSync.changed(b, u);
        var m = u.mob;
        m.setYaw(yaw); m.setBodyYaw(yaw); m.setHeadYaw(yaw); m.prevYaw = yaw; m.prevBodyYaw = yaw; m.prevHeadYaw = yaw;
        return u;
    }

    void bigHealth(GravesAndGlory.Unit u, double hp) {
        var a = u.mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (a != null) a.setBaseValue(hp);
        u.mob.setHealth((float) hp);
    }

    void setupBash(GravesAndGlory.Battle b, int angle) {
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(o.player);
        var sd = sides(b, p);
        if (p == null || sd[0] == null || sd[1] == null) { snap = "no squads"; return; }
        clear(b);
        vyaw = GeoKnightChecks.CAP_YAW[angle];
        double rad = Math.toRadians(vyaw); fx = -Math.sin(rad); fz = Math.cos(rad);
        victim = spawn(b, sd[0], troop, 0, o.cx, o.cz, 1, 2, vyaw);
        bigHealth(victim, 400);
        victim.mob.setAiDisabled(true);
        attacker = spawn(b, sd[1], "knight", 1, o.cx + fx * 1.5, o.cz + fz * 1.5, 3, 3, vyaw + 180);
        attacker.mob.setAiDisabled(true); attacker.mob.setInvulnerable(true);
        victimId = victim.mob.getId(); probeId = victimId;
        battle = b; phase = 1; hitTime = -1; stunUntil = -1;
        p.setNoGravity(true);
        snap = "set";
    }

    void strike(GravesAndGlory.Battle b) {
        if (victim == null || attacker == null || phase != 1) return;
        victim.mob.setAiDisabled(false);
        long before = victim.stunnedUntil;
        float dealt = Combat.resolve(b, attacker, victim.mob, 3f, "melee", false);
        hitTime = b.time; stunUntil = victim.stunnedUntil;
        phase = 2;
        System.out.println(String.format("[gb] strike %s ang=%d dealt=%.2f stun %d -> %d (b.time %d) vel=%.2f,%.2f hp=%.1f", troop, run, dealt, before, stunUntil, b.time,
            victim.mob.getVelocity().x, victim.mob.getVelocity().z, victim.mob.getHealth()));
        if (stunUntil <= b.time) { hitTime = -1; stunUntil = -1; phase = 1; snap = "no stun"; return; }
        // the attacker leaves: the victim's AI then walks 18 blocks ahead to find it (invisible, so its teleport lerp does not cross the camera)
        attacker.mob.setInvisible(true);
        attacker.mob.setPosition(o.cx + fx * 18, Arena.ground((int) Math.floor(o.cx + fx * 18), (int) Math.floor(o.cz + fz * 18)) + 1, o.cz + fz * 18);
    }

    void setupCharge(GravesAndGlory.Battle b) {
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(o.player);
        var sd = sides(b, p);
        if (p == null || sd[0] == null || sd[1] == null) { snap = "no squads"; return; }
        clear(b);
        double z0 = o.cz - 10;
        for (int j = 0; j < 6; j++) {
            var u = spawn(b, sd[0], troop, j, o.cx + (j - 2.5) * 1.8, z0, 1, 2, 0f);
            squad.add(u);
        }
        for (int j = 0; j < 3; j++) {
            var u = spawn(b, sd[1], "knight", 10 + j, o.cx + (j - 1) * 4.0, z0 + 30, 1, 1, 180f);
            u.mob.setAiDisabled(true);
            bigHealth(u, 14);
            foes.add(u);
        }
        squadIds = squad.stream().mapToInt(u -> u.mob.getId()).toArray();
        probeId = squad.get(2).mob.getId();
        battle = b; phase = 3;
        p.setNoGravity(true);
        snap = "set";
    }

    void order(GravesAndGlory.Battle b) {
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(o.player);
        if (p == null || phase != 3) return;
        double z0 = squad.get(2).mob.getZ();
        OrderRules.playerOrder(b, p, "all", "charge", "line", o.cx, z0, 0f);
        phase = 4;
        System.out.println("[gc] order " + troop + " orders=" + squad.stream().map(u -> b.assignment(u).order).distinct().toList() + " b.time=" + b.time);
    }

    void serverTick() {
        var b = battle;
        if (b == null || b.server == null) return;
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(o.player);
        if (p == null) return;
        if (phase == 1 || phase == 2) {
            var v = victim;
            if (v == null || !v.mob.isAlive()) return;
            if (phase == 1) {
                GeoKnightChecks.stand(v, o.cx, o.cz, vyaw);
                GeoKnightChecks.stand(attacker, o.cx + fx * 1.5, o.cz + fz * 1.5, vyaw + 180);
                Tactics.set(v, "", 0);
            } else {
                v.mob.setHealth(v.mob.getMaxHealth());
                if (HOLDYAW) { var m = v.mob; m.setYaw(vyaw); m.setBodyYaw(vyaw); m.setHeadYaw(vyaw); }
                attacker.nextAttack = b.time + 400; attacker.abilityAt = b.time + 400;
                attacker.mob.setVelocity(0, 0, 0);
            }
            snap = String.format(Locale.ROOT, "stun=%d order=%s st=%s pos=%.2f,%.2f", victim.stunnedUntil - b.time, b.assignment(v).order, v.stance, v.mob.getX() - o.cx, v.mob.getZ() - o.cz);
            // the client draws the victim about two ticks behind the server, so the camera follows the server position of two ticks ago
            if (phase == 1) hist.clear();
            hist.addFirst(new double[]{v.mob.getX(), v.mob.getZ()});
            while (hist.size() > 12) hist.removeLast();
            double[] hz = new ArrayList<>(hist).get(Math.min(LAG, hist.size() - 1));
            p.networkHandler.requestTeleport(hz[0], o.cy - .45, hz[1] - DIST, 0, 8);
        } else if (phase >= 3) {
            double zs = 0, xs = 0; int n = 0;
            for (var u : squad) if (u.mob.isAlive()) { zs += u.mob.getZ(); xs += u.mob.getX(); n++; }
            for (var f : foes) {
                if (!f.mob.isAlive()) continue;
                f.nextAttack = b.time + 400; f.abilityAt = b.time + 400;
                GeoKnightChecks.stand(f, f.mob.getX(), f.mob.getZ(), 180);
            }
            // a front-right three-quarter camera that follows the middle unit's client-visible position (the server position of two ticks ago)
            var uc = squad.get(2);
            double[] hz = camHist;
            if (hz[0] != hz[0] || phase == 3) { hz[0] = hz[2] = hz[4] = uc.mob.getX(); hz[1] = hz[3] = hz[5] = uc.mob.getZ(); }
            hz[4] = hz[2]; hz[5] = hz[3]; hz[2] = hz[0]; hz[3] = hz[1]; hz[0] = uc.mob.getX(); hz[1] = uc.mob.getZ();
            p.networkHandler.requestTeleport(hz[4] + CAM[0], o.cy - .45 + CAM[3], hz[5] + CAM[1], (float) CAM[2], 6);
            var u2 = squad.get(2);
            int alive = 0; for (var f : foes) if (f.mob.isAlive()) alive++;
            double d = foes.isEmpty() ? 0 : foes.get(0).mob.getZ() - u2.mob.getZ();
            snap = String.format(Locale.ROOT, "order=%s st=%s dz=%.1f foes=%d spd=%.3f z=%.1f", b.assignment(u2).order, u2.stance, d, alive, u2.mob.getVelocity().horizontalLength(), u2.mob.getZ() - o.cz);
        }
    }
}
