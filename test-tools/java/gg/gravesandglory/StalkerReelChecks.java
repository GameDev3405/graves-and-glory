package gg.gravesandglory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * Capture only, never a check of the animations themselves (GRAVES_PHASE1_CHECKS=stalker-reel, run with GRAVES_GEO_TROOPS=all): the review reel of the
 * stalker. One tier III stalker (own side, frozen, no AI) plays all 35 clips in a fixed order in a practice battle, in clear daylight on flat ground,
 * and every client tick of every segment is saved as p1-reel-&lt;NN&gt;-&lt;segment&gt;-&lt;TTT&gt;.png (NN the segment 01..35, TTT the frame within it from 000), so the
 * frames can be stitched into a video at 20 fps. Every segment starts in the neutral state (idle, no stance, no recent blow) for a lead-in of
 * {@link Seg#lead} frames, then the clip is triggered by the same server events the game sends (MotionEvents stance / attack / hit; movement by the server,
 * as the geo-knight march does). The camera is the player's eye, set by the server every tick (a client-side move would stall with the screenshots): a
 * three-quarter front view from the troop's left (bow) side, {@link #DIST} blocks away, tracking the troop while it moves. One evidence line per segment
 * (p1-stalker-reel-NN-slug) says how many frames were taken and which clips and gait the client really showed.
 */
final class StalkerReelChecks implements Phase1Checks.Suite {
    static final String TROOP = "stalker";
    /** The troop faces north-east (yaw 225), the camera is due north of it looking south: the troop's front and its left side show. */
    static final float YAW = 225f;
    static final double FX = -Math.sin(Math.toRadians(YAW)), FZ = Math.cos(Math.toRadians(YAW));
    /** Camera: blocks north of the troop (deaths: farther, so the body stays in frame), downward pitch (degrees), and the client's field of view (default 70: at 4.5 blocks the troop would fill a third of the frame). */
    static final double DIST = 4.5, DEATH_DIST = 6.0;
    static final float PITCH = 3f;
    static final int FOV = 48;
    /** The client draws a moved troop about this many ticks behind the server's position (3-step interpolation): the moving camera aims that far behind the server's position so the troop stays centred. */
    static final double CAM_LAG = 2.0;
    /** Ticks at the start of every segment that are not shot: the server's begin() (a move, a spawn) reaches the client. */
    static final int PRE = 3, PRE_DEATH = 6;
    /** A bow troop's attack clip is 32 ticks (the loose on tick 22) plus its draw hold (0 to MotionMath.LOOSE_MAX = 3), plus 8 after. */
    static final int ATTACK_FRAMES = 32 + MotionMath.LOOSE_MAX + 8;

    interface Go { void run(StalkerReelChecks r, GravesAndGlory.Battle b); }

    /** One segment: number, slug, lead-in frames, frames after the clip is triggered; the expectations the evidence line checks. */
    static final class Seg {
        final int no, lead, active; final String slug;
        /** Frames before the first shot (not shot), and the segment's camera distance. */
        int pre = PRE; double dist = DIST;
        /** Movement: 0 none, 1 walk forward, 2 run forward, 3 walk backward. */
        int mv;
        /** A death: the fresh victim's index (spawned at the segment's start), else -1. A continuation segment (cont) goes on from the last one with no new neutral start. */
        int victim = -1; boolean cont;
        /** Expected: a clip name some frame showed (prefix; empty: none), and a gait mode shown for at least gaitMin frames (back: while moving backwards). */
        String want = ""; Gait.Mode gait; int gaitMin; boolean back;
        String note = "";
        Go go;
        Seg(int no, String slug, int lead, int active) { this.no = no; this.slug = slug; this.lead = lead; this.active = active; }
        Seg want(String w) { want = w; return this; }
        Seg gait(Gait.Mode m, int min) { gait = m; gaitMin = min; return this; }
        Seg move(int kind) { mv = kind; return this; }
        Seg go(Go g) { go = g; return this; }
        Seg note(String n) { note = n; return this; }
        Seg victim(int v) { victim = v; pre = PRE_DEATH; dist = DEATH_DIST; return this; }
        Seg cont() { cont = true; pre = 0; return this; }
        int total() { return lead + active; }
    }

    static List<Seg> build() {
        List<Seg> l = new ArrayList<>();
        l.add(new Seg(1, "idle", 0, 60).gait(Gait.Mode.IDLE, 55));
        l.add(new Seg(2, "walk", 6, 38).move(1).gait(Gait.Mode.WALK, 32).go((r, b) -> r.startMove(b, "", 1)));
        l.add(new Seg(3, "run", 6, 50).move(2).gait(Gait.Mode.RUN, 28).go((r, b) -> r.startMove(b, "run", 2)));
        l.add(new Seg(4, "charge", 6, 50).move(2).gait(Gait.Mode.RUN, 28).want("charge_run_upper").go((r, b) -> r.startMove(b, "charge", 2)));
        l.add(new Seg(5, "attack-direct", 6, ATTACK_FRAMES).want("attack_direct").go((r, b) -> r.attack(b, "bow_direct")));
        l.add(new Seg(6, "attack-volley", 6, ATTACK_FRAMES).want("attack_volley").go((r, b) -> r.attack(b, "bow_volley")));
        // stances, 44 frames after the order (rally: 30)
        l.add(new Seg(7, "stance-ready", 6, 44).want("stance_ready_upper").go((r, b) -> r.ready(b)));
        l.add(new Seg(8, "stance-rest", 6, 44).want("stance_rest_upper").go((r, b) -> r.order(b, "ranged_rest", 0f)));
        l.add(new Seg(9, "stance-hold", 6, 44).want("stance_hold_upper").go((r, b) -> r.order(b, "hold", 0f)));
        l.add(new Seg(10, "stance-brace", 6, 44).want("stance_brace_upper").go((r, b) -> r.order(b, "brace", 0f)));
        l.add(new Seg(11, "stance-wall", 6, 44).want("stance_wall_upper").go((r, b) -> r.order(b, "wall", 0f)));
        l.add(new Seg(12, "stance-ward", 6, 44).go((r, b) -> r.order(b, "fall_back", 0f))
            .note("stance_ward_upper has no route for a troop that has stance_fallback clips (StanceController.target: the troop's own fall_back clips come first), so this plays fall_back standing"));
        l.add(new Seg(13, "stance-shield-up", 6, 44).want("stance_shield_up_upper").go((r, b) -> r.order(b, "shield_move", 0f)));
        l.add(new Seg(14, "stance-rally", 6, 30).want("stance_rally_upper").go((r, b) -> r.order(b, "rally", 0f)));
        l.add(new Seg(15, "stance-fallback", 6, 44).move(3).want("stance_fallback_upper").gait(Gait.Mode.WALK, 30).go((r, b) -> r.startMove(b, "fall_back", 3)));
        l.add(new Seg(16, "stance-aim", 6, 44).want("stance_aim_upper").go((r, b) -> r.order(b, "ranged_aim", 0f)));
        l.add(new Seg(17, "stance-aim-up", 6, 44).want("stance_aim_up_upper").go((r, b) -> r.order(b, "ranged_aim", -45f)));
        l.add(new Seg(18, "stance-aim-down", 6, 44).want("stance_aim_down_upper").go((r, b) -> r.order(b, "ranged_aim", 30f)));
        // hits: bearing 0 front, 180 back, +90 right, -90 left (HitController.mix); weight 0 light, 1 medium
        l.add(new Seg(19, "hit-block", 6, 24).want("hit_block").go((r, b) -> r.hit(b, 0f, 1, true, false, false)));
        String[] dirs = {"front", "back", "left", "right"};
        float[] bearing = {0f, 180f, -90f, 90f};
        for (int w = 0; w < 2; w++) for (int d = 0; d < 4; d++) {
            final float br = bearing[d]; final int wt = w;
            l.add(new Seg(20 + w * 4 + d, "hit-" + (w == 0 ? "light" : "medium") + "-" + dirs[d], 6, 24).want(w == 0 ? "hit_light" : "hit_medium").go((r, b) -> r.hit(b, br, wt, false, false, false)));
        }
        l.add(new Seg(28, "hit-knockback", 6, 30).want("hit_knockback").go((r, b) -> r.hit(b, 0f, 1, false, true, false)));
        l.add(new Seg(29, "hit-pulled", 6, 24).want("hit_pulled").go((r, b) -> r.hit(b, 0f, 1, false, false, true)));
        l.add(new Seg(30, "stunned", 6, 40).want("stun_upper").go((r, b) -> r.order(b, "stunned", 0f)));
        // the stun ends on the first frame of the next segment: no neutral lead-in there, it continues the stun
        l.add(new Seg(31, "stun-out", 0, 16).cont().go((r, b) -> r.order(b, "", 0f)).note("continues segment 30: no lead-in, the stun is lifted on its first frame"));
        // deaths: DeathController.clipFor(bearing): 0 front blow -> death_back, 180 -> death_forward, +90 -> death_left, -90 -> death_right
        String[] death = {"forward", "back", "left", "right"};
        float[] dbear = {180f, 0f, 90f, -90f};
        for (int d = 0; d < 4; d++) {
            final float br = dbear[d];
            l.add(new Seg(32 + d, "death-" + death[d], 6, 34).victim(d).want("death_" + death[d]).go((r, b) -> r.kill(b, br)));
        }
        return l;
    }

    static final List<Seg> SEGS = build();

    /** The suite instance the server-tick hook drives (set once the stalker is placed, cleared by cleanup). */
    static volatile StalkerReelChecks live;
    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            StalkerReelChecks r = live;
            if (r != null) try { r.serverHold(server); } catch (Exception e) { Phase1Checks.report("p1-stalker-reel-exception", false, "server hold: " + e); e.printStackTrace(); }
        });
    }

    // ---- state ----
    volatile UUID player;
    volatile boolean battleReady, placed;
    volatile String why = "";
    volatile double cx, cz, walkV, runV;
    Spec.TroopsRow spec;
    GravesAndGlory.Squad mine, foe;
    volatile GravesAndGlory.Unit main, subject;
    volatile int subjectId = -1;
    /** Units held far behind the camera (the foe that keeps the battle alive, the own troop while a death victim stands in). Server thread. */
    final Map<GravesAndGlory.Unit, double[]> parked = new LinkedHashMap<>();
    /** What the server holds the subject in this tick: the stance sent, its aim pitch, the move (signed speed along the facing, from (px, pz), starting at moveStart), the camera distance. */
    volatile String stance = "";
    volatile float aim;
    volatile double speed, px, pz, camDist = DIST;
    volatile long moveStart = -1;
    int stage, at, i, fov0 = 70;
    // per-segment evidence (client thread)
    int frames, missing, gaitFrames;
    final Set<String> clips = new LinkedHashSet<>();
    int totalFrames;
    boolean failedAny;

    void next(int tick, int s) { stage = s; at = tick; }

    @Override public void cleanup(MinecraftClient c) {
        live = null;
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
        try { c.options.getFov().setValue(fov0); } catch (Exception e) { /* leave it */ }
        if (c.getServer() != null) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
    }

    // ---- server side ----

    static void stand(GravesAndGlory.Unit u, double x, double z, float yaw) {
        var m = u.mob;
        if (!m.isAlive()) return;
        m.setPosition(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z);
        m.setVelocity(0, 0, 0);
        m.setYaw(yaw); m.setBodyYaw(yaw); m.setHeadYaw(yaw);
    }

    /** Clears the field, then stands the subject (tier III, own side) at the centre, with a foe parked far behind the camera so the round never ends. */
    void place(GravesAndGlory.Battle b) {
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) { why = "no player"; return; }
        spec = Campaign.troop(TROOP);
        if (spec == null) { why = "no " + TROOP + " troop"; return; }
        walkV = .169 * spec.speed() / .28; runV = .264 * spec.speed() / .28; // the knight's walk and charge speeds scaled by the troop's speed (geo-knight)
        for (var u : b.units) { GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); }
        b.units.clear(); b.clearCorpses();
        for (var s : b.squads.values()) { if (s.team == 0 && mine == null) mine = s; else if (s.team == 1 && foe == null) foe = s; }
        var own = b.squads.get(player);
        if (own != null) { mine = own; for (var s : b.squads.values()) if (s.team != own.team) { foe = s; break; } }
        if (mine == null || foe == null) { why = "squads " + b.squads.size(); return; }
        b.world.setTimeOfDay(6000);
        b.world.setWeather(24000, 0, false, false);
        cx = Math.floor(p.getX()) + .5; cz = Math.floor(p.getZ()) + 7.5;
        main = spawn(b, mine, 20, cx, cz);
        foeAnchor(b);
        px = cx; pz = cz; subject = main; subjectId = main.mob.getId();
        stance = ""; aim = 0; speed = 0; moveStart = -1; camDist = DIST;
        stand(main, px, pz, YAW);
        live = this;
        placed = true;
    }

    GravesAndGlory.Unit spawn(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, double x, double z) {
        var u = b.spawn(s, spec, slot, new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z));
        u.level = 3; u.weaponTier = 3; u.armourTier = 3;
        u.mob.setAiDisabled(true); u.mob.setInvulnerable(true);
        RankSync.changed(b, u);
        stand(u, x, z, YAW);
        u.mob.prevYaw = YAW; u.mob.prevBodyYaw = YAW; u.mob.prevHeadYaw = YAW;
        return u;
    }

    void foeAnchor(GravesAndGlory.Battle b) {
        var f = spawn(b, foe, 21, cx + 20, cz - 30);
        parked.put(f, new double[]{cx + 20, cz - 30});
    }

    /** Every server tick (after the battle's own beat): the parked troops held, the subject held in its stance and on its move, the camera set. */
    void serverHold(MinecraftServer server) {
        var b = GravesAndGlory.battle;
        var u = subject;
        if (b == null || !placed || u == null) return;
        for (var en : parked.entrySet()) { stand(en.getKey(), en.getValue()[0], en.getValue()[1], YAW); Phase1Checks.freeze(en.getKey()); }
        var m = u.mob;
        double camX = m.getX(), camZ = m.getZ();
        if (m.isAlive()) {
            double t = moveStart >= 0 ? Math.max(0, b.world.getTime() - moveStart) : 0, tc = Math.max(0, t - CAM_LAG);
            stand(u, px + FX * speed * t, pz + FZ * speed * t, YAW);
            camX = px + FX * speed * tc; camZ = pz + FZ * speed * tc;
            Phase1Checks.freeze(u);
            // a held "stunned" stance is also a real stun (far ahead), so the battle beat's Tactics.stunStance does not drop it; any other hold ends it
            if (stance.equals("stunned")) u.stunnedUntil = Long.MAX_VALUE / 4; else if (u.stunnedUntil == Long.MAX_VALUE / 4) u.stunnedUntil = 0;
            Tactics.set(u, stance, aim); Tactics.shield(m, false);
        }
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        if (p != null) { p.setNoGravity(true); p.networkHandler.requestTeleport(camX, m.getY() - .45, camZ - camDist, 0f, PITCH); }
    }

    /** Segment start (server thread): the neutral state at the start mark (a move starts so that it passes the centre half way), a fresh victim for a death. */
    void begin(GravesAndGlory.Battle b, Seg s) {
        if (s.victim >= 0) {
            parked.put(main, new double[]{cx - 20, cz - 30});
            subject = spawn(b, mine, 22 + s.victim, cx, cz);
        } else { parked.remove(main); subject = main; }
        subjectId = subject.mob.getId();
        double v = s.mv == 1 || s.mv == 3 ? (s.mv == 3 ? -walkV : walkV) : s.mv == 2 ? runV : 0;
        stance = ""; aim = 0; speed = 0; moveStart = -1; camDist = s.dist;
        px = cx - FX * v * s.active / 2; pz = cz - FZ * v * s.active / 2;
        stand(subject, px, pz, YAW);
        subject.mob.prevYaw = YAW; subject.mob.prevBodyYaw = YAW; subject.mob.prevHeadYaw = YAW;
    }

    /** Segment end (server thread): a death's victim and its corpse go, the stalker is the subject again. */
    void end(GravesAndGlory.Battle b, Seg s) {
        if (s.victim < 0) return;
        var u = subject;
        if (u != null && u != main) { GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); b.units.remove(u); b.clearCorpses(); }
        parked.remove(main); subject = main; subjectId = main.mob.getId();
        px = cx; pz = cz; speed = 0; moveStart = -1;
    }

    void startMove(GravesAndGlory.Battle b, String st, int kind) {
        stance = st; aim = 0;
        speed = kind == 3 ? -walkV : kind == 2 ? runV : walkV;
        moveStart = b.world.getTime();
    }

    void order(GravesAndGlory.Battle b, String st, float pitch) { stance = st; aim = pitch; }

    /** The ready guard: a troop with no order stance stands ready while it has swung or been struck within StanceController.FIGHTING ticks. A swing dated far in the future makes it "recent" without ever playing a clip (AttackController plays only swings that have started). */
    void ready(GravesAndGlory.Battle b) { MotionEvents.sendAttack(subject, 0, b.world.getTime() + 100000); }

    void attack(GravesAndGlory.Battle b, String variantId) {
        var rows = Tactics.variants(spec.family());
        for (int v = 0; v < rows.size(); v++) if (rows.get(v).id().equals(variantId)) { MotionEvents.sendAttack(subject, v, b.world.getTime()); return; }
        throw new IllegalStateException("no attack variant " + variantId);
    }

    /** A blow of this weight from this bearing (+ from the troop's right): blocked (hit_block), knocked back (hit_v1 flag 4) or pulled (flag 8). The body is not moved. */
    void hit(GravesAndGlory.Battle b, float bearing, int weight, boolean blocked, boolean knockback, boolean pulled) {
        var m = subject.mob;
        if (pulled) MotionEvents.sendHit(m, "slash", "body", bearing, weight, false, false, false, true, MotionEvents.watchers(m));
        else if (knockback) MotionEvents.sendHit(m, "slash", "body", bearing, weight, false, false, true, MotionEvents.watchers(m));
        else MotionEvents.sendHit(m, "slash", "body", bearing, weight, blocked, false);
    }

    void kill(GravesAndGlory.Battle b, float bearing) {
        var m = subject.mob;
        m.setInvulnerable(false); m.kill();
        MotionEvents.sendHit(m, "slash", "body", bearing, 1, false, true);
    }

    // ---- client side ----

    void sample(MinecraftClient c, Seg s) {
        Entity e = c.world == null ? null : c.world.getEntityById(subjectId);
        if (e == null) { missing++; return; }
        Gait g = Gait.LIVE.get(e);
        if (g != null && s.gait != null && (s.back ? g.mode == s.gait && g.speed < -.05 : g.mode == s.gait)) gaitFrames++;
        StanceController st = StanceController.LIVE.get(e);
        if (st != null) { if (!st.shownUpper.isEmpty()) clips.add(st.shownUpper); if (!st.shownLegs.isEmpty()) clips.add(st.shownLegs); }
        AttackController ac = AttackController.LIVE.get(e);
        if (ac != null && ac.shownWeight > 0 && !ac.shownClip.isEmpty()) clips.add(ac.shownClip);
        HitController hc = HitController.LIVE.get(e);
        if (hc != null && !hc.shownClip.isEmpty()) clips.add(hc.shownClip);
        DeathController dc = DeathController.LIVE.get(e);
        if (dc != null && !dc.shownClip.isEmpty()) clips.add(dc.shownClip);
    }

    void report(Seg s) {
        boolean wantOk = s.want.isEmpty() || clips.stream().anyMatch(cl -> cl.startsWith(s.want));
        boolean gaitOk = s.gait == null || gaitFrames >= s.gaitMin;
        boolean ok = frames == s.total() && missing == 0 && wantOk && gaitOk;
        failedAny |= !ok;
        totalFrames += frames;
        Phase1Checks.report(String.format("p1-stalker-reel-%02d-%s", s.no, s.slug), ok,
            frames + "/" + s.total() + " frames (" + s.lead + " lead-in)" + (missing > 0 ? ", entity missing on " + missing : "")
            + ", clips " + clips + (s.gait != null ? ", gait " + s.gait + (s.back ? " backwards" : "") + " on " + gaitFrames + " frames (need " + s.gaitMin + ")" : "")
            + (wantOk ? "" : ", never showed " + s.want) + (s.note.isEmpty() ? "" : "; " + s.note));
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 5000 && stage != 9 && stage < 99) { Phase1Checks.report("p1-stalker-reel-timeout", false, "stuck at stage " + stage + ", segment " + i); next(tick, 9); }
        int k = tick - at - 1; // next(tick, s) is called on the last tick of a stage: the next stage's first step is k 0
        GeoLayerRest.where = "stage " + stage + (stage >= 99 ? "" : " segment " + i);
        switch (stage) {
            case 0 -> {
                if (tick == 20) {
                    c.setScreen(null); player = c.player.getUuid();
                    c.options.hudHidden = true; c.options.setPerspective(Perspective.FIRST_PERSON);
                    fov0 = c.options.getFov().getValue(); c.options.getFov().setValue(FOV);
                    GeoLayerRest.start();
                    next(tick, 1);
                }
            }
            case 1 -> {
                if (!battleReady) {
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true; });
                    if (k > 600) { Phase1Checks.report("p1-stalker-reel", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                if (k == 80) Phase1Checks.server(c, this::place);
                if (k > 100) {
                    if (!placed) { Phase1Checks.report("p1-stalker-reel-placed", false, "could not place the stalker: " + why); next(tick, 9); return; }
                    Phase1Checks.report("p1-stalker-reel-placed", true, "one tier III " + TROOP + ", camera " + DIST + " blocks, fov " + FOV);
                    i = 0; next(tick, 2);
                }
            }
            case 2 -> {
                Seg s = SEGS.get(i);
                if (k == 0) {
                    frames = 0; missing = 0; gaitFrames = 0; clips.clear();
                    if (!s.cont) {
                        // the previous segment's blows and swings would keep the troop in its ready guard: forget them, so the lead-in is the idle
                        MotionEvents.Client.ATTACKS.remove(subjectId); MotionEvents.Client.HITS.remove(subjectId);
                        Phase1Checks.server(c, b -> begin(b, s));
                    }
                }
                int f = k - (s.cont ? 0 : s.pre);
                if (f >= 0 && f < s.total()) {
                    if (f == s.lead && s.go != null) Phase1Checks.server(c, b -> s.go.run(this, b));
                    sample(c, s);
                    Phase1Checks.shot(c, String.format("reel-%02d-%s-%03d", s.no, s.slug, f));
                    frames++;
                }
                if (f >= s.total()) {
                    report(s);
                    if (s.victim >= 0) Phase1Checks.server(c, b -> end(b, s));
                    if (++i >= SEGS.size()) {
                        Phase1Checks.report("p1-stalker-reel", !failedAny, SEGS.size() + " segments, " + totalFrames + " frames");
                        next(tick, 9);
                    } else next(tick, 2);
                }
            }
            case 9 -> {
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
                // every Geo draw of the run began with the bones left off rest (GeoLayerRest): none may have been built on that
                if (k == 1) {
                    GeoLayerRest.stop(); Phase1Checks.report("p1-stalker-reel-layer-rest", GeoLayerRest.clean(), GeoLayerRest.result());
                    // and with another draw's legs left on the leg bones: a troop that set off must have matched its stride to its own
                    Phase1Checks.report("p1-stalker-reel-start-own-legs", GeoLayerRest.ownLegs(1), GeoLayerRest.startResult());
                }
                if (k > 40) { Phase1Checks.finish(c, "stalker-reel"); stage = 99; }
            }
            default -> {}
        }
    }
}
