package gg.gravesandglory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * Unit animation rebuild, step 3 (docs/anim-rebuild/PLAN.md): the GeckoLib knight rig drawn in game at rest. GRAVES_PHASE1_CHECKS=geo-knight.
 * In a practice battle every troop is cleared, then six knights stand frozen in a line facing the camera: tiers I-III of the player's side
 * and of the enemy side. They are turned to front, three-quarter, side and back and shot close and at 16 blocks
 * (p1-geo-knight-<view>-<near|far>.png), then one tier-III close-up per side. Then the six march past at the knight's real walk
 * and charge speeds, seen side on, head on and three-quarter, past a camera that holds still (so a planted foot must hold still
 * on screen too): close for 16 server ticks (two or more stride cycles) and at 16 blocks for 8,
 * p1-geo-knight-<walk|run>-<view>-<near|far>-<tick>.png, named by the march's server tick (a tick the client was busy for is
 * missing, never doubled). The server moves them every server tick, as a troop's own walk does, never from the client, whose
 * ticks stall on each screenshot. Over 8 mid-march ticks the client's stride clock (Gait) is read back for every knight: its
 * mean speed must be within 10% of the true speed and its clip the expected one in every sample. Never shipped.
 */
final class GeoKnightChecks implements Phase1Checks.Suite {
    /** Knight yaw per view; the camera looks south (+z, yaw 0) and the knights face it at 180. */
    static final String[] VIEWS = {"front", "threequarter", "side", "back"};
    static final float[] YAWS = {180, 135, 90, 0};
    /** March gaits and their true speeds, blocks per tick: the knight's walk and charge (Spec troops row). */
    static final String[] GAITS = {"walk", "run"};
    static final double[] SPEEDS = {.169, .264};
    /** The GeckoLib troop under test (-Dgravesandglory.geoCheckTroop, default the knight): any biped on the knight's skeleton. Its
     *  march speeds scale the knight's by its troops-row speed (the knight's is .28). Reports and shots are named p1-geo-<troop>-... */
    static final String TROOP = System.getProperty("gravesandglory.geoCheckTroop", "knight");
    /** The mage: a caster whose spell is a mend (Abilities.mend, announced a beat ahead there, not by Tactics.announcesAhead), so its duel is a ranged one whose release is the mend shot (Projectiles.fire "mend"), not Tactics.shoot. */
    static final boolean CAST = Arrays.stream(Spec.TROOPS).anyMatch(r -> r.id().equals(TROOP) && r.ranged() && r.magic() && "caster".equals(r.family()) && "mend".equals(Abilities.row(r).mechanic()));
    /** The hexer or the mist caller: a caster whose spell is a blight or a mist (Abilities.attack, its cast clip sent at the release and announced a beat ahead by Tactics.tick when its spell cooldown is ready too), so its duel is a ranged one whose release is Tactics.shoot with that cooldown cleared. */
    static final boolean SPELL = Arrays.stream(Spec.TROOPS).anyMatch(r -> r.id().equals(TROOP) && r.ranged() && r.magic() && "caster".equals(r.family()) && ("blight".equals(Abilities.row(r).mechanic()) || "mist".equals(Abilities.row(r).mechanic())));
    /** The Bloodhowl Seer (caster v2): a caster whose attack beat shoots a bolt (Abilities.shoots: its ability, the howl, is no spell), so its duel is a ranged one whose release is Tactics.shoot, announced a beat ahead as Tactics.tick does (Tactics.announcesAhead). */
    static final boolean SHOT = Arrays.stream(Spec.TROOPS).anyMatch(r -> r.id().equals(TROOP) && r.ranged() && r.magic() && "caster".equals(r.family()) && !"mend".equals(Abilities.row(r).mechanic()) && !"blight".equals(Abilities.row(r).mechanic()) && !"mist".equals(Abilities.row(r).mechanic()));
    /** A bow, crossbow or thrower troop (Tactics.announcesAhead): its duel is fought at range, each shot announced a beat ahead (a bow's loosed late) as Tactics does. */
    static final boolean RANGED = Arrays.stream(Spec.TROOPS).anyMatch(r -> r.id().equals(TROOP) && r.ranged() && !r.magic() && ("bow".equals(r.family()) || "crossbow".equals(r.family()) || "thrower".equals(r.family()))) || CAST || SPELL || SHOT;
    /** A thrower, a mage, a hexer or a mist caller: one throw or cast clip (its own length, read like a melee clip's) per shot, with the gap window just after it. */
    static final boolean THROW = RANGED && (CAST || SPELL || SHOT || Arrays.stream(Spec.TROOPS).anyMatch(r -> r.id().equals(TROOP) && "thrower".equals(r.family())));
    /** A crossbow troop: its 50-tick clip shoots, spans and reloads, so its shots are spaced wider (nudgeR) and its gap window sits after the reload. */
    static final boolean XBOW = RANGED && Arrays.stream(Spec.TROOPS).anyMatch(r -> r.id().equals(TROOP) && "crossbow".equals(r.family()));
    /** Ticks without a shot before a ranged duelist is made to shoot again: a crossbow standing waits 60 (its cooldown) so the gap after its 50-tick clip shows; walking, 40 (clips 51 ticks apart) so the 70-tick walk fits two shots each. */
    static int nudgeR(String mode) { return XBOW && !mode.equals("walk") ? 60 : mode.equals("walk") ? 40 : 40 + ablLead; } // standing, an ability cast's clip runs ablLead ticks later, so its gap window does too
    /** The duelists' spacing (blocks); a bow duel stands where neither is too close to shoot. */
    static final double GAP = RANGED ? 10 : 2;
    /** Ticks into the foe's swing (a bow: its draw) it is killed; the swing must show 2 ticks before that. */
    static final int KILL_AT = RANGED ? 12 : 4;
    /** A melee troop's attack clip length in ticks, read from its exported animation file (the longest attack_ clip): the knight's
     *  are 22, a heavier swing's longer, and every tick of it is a frame to catch, with the gap window opening when it ends. */
    /** A caster whose cast is its ability (a mend, a blight, a mist): its cast plays the ability clip, shifted so its impact lands ABL_WINDUP ticks after the cast event's (AttackController). */
    static final boolean ABL_CAST = Arrays.stream(Spec.TROOPS).anyMatch(r -> r.id().equals(TROOP) && "caster".equals(r.family()) && switch (Abilities.row(r).mechanic()) { case "mend", "blight", "mist" -> true; default -> false; });
    static final int ABL_WINDUP = ABL_CAST ? Arrays.stream(Spec.TROOPS).filter(r -> r.id().equals(TROOP)).mapToInt(r -> Math.max(0, Abilities.row(r).windupTicks())).findFirst().orElse(0) : 0;
    static final String ABILITY_CLIP = Arrays.stream(Spec.TROOPS).filter(r -> r.id().equals(TROOP)).map(Spec.TroopsRow::ability).findFirst().orElse("");
    /** Ticks an ability cast's clip starts after its cast event's start tick (caster_cast impactTicks 10 + ABL_WINDUP - the clip's impact marker), set by meleeTicks. */
    static int ablLead;
    static final int MELEE_T = meleeTicks();
    /** Capture only: how many swing (cut or sweep) clips the troop has, at most 2: its attack_ clips that are not a thrust or a jab. */
    static final int SLASH_N = slashClips();
    /** Whether the troop has its own hit_knockback: without it a throw plays the medium stagger (HitController), so a pull from behind does too. */
    static final boolean KNOCKBACK_CLIP = hasClip("hit_knockback");
    static boolean hasClip(String clip) {
        try (var in = GeoKnightChecks.class.getResourceAsStream("/assets/gravesandglory/animations/troop/" + TROOP + ".animation.json")) {
            return in != null && com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations").has(clip);
        } catch (Exception e) { return true; }
    }
    static boolean isSlash(String clip) { return !clip.contains("thrust") && !clip.contains("jab"); }
    static int slashClips() {
        try (var in = GeoKnightChecks.class.getResourceAsStream("/assets/gravesandglory/animations/troop/" + TROOP + ".animation.json")) {
            if (in == null) return 2;
            var anims = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
            int n = 0;
            for (var k : anims.keySet()) if (k.startsWith("attack_") && !k.endsWith("_legs") && isSlash(k)) n++;
            return Math.max(1, Math.min(2, n));
        } catch (Exception e) { return 2; }
    }
    static int meleeTicks() {
        try (var in = GeoKnightChecks.class.getResourceAsStream("/assets/gravesandglory/animations/troop/" + TROOP + ".animation.json")) {
            if (in == null) return 22;
            var anims = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
            // A mend, blight or mist caster's cast plays its ability clip (AttackController.abilityClip), so its frames are that clip's.
            var ab = ABL_CAST ? anims.getAsJsonObject(ABILITY_CLIP) : null;
            if (ab != null && ab.has("animation_length")) {
                if (ab.has("timeline")) for (var m : ab.getAsJsonObject("timeline").entrySet())
                    if ("impact".equals(m.getValue().getAsString())) ablLead = Tactics.variants("caster").get(0).impactTicks() + ABL_WINDUP - (int) Math.round(Double.parseDouble(m.getKey()) * 20);
                return (int) Math.round(ab.get("animation_length").getAsDouble() * 20);
            }
            double len = 0;
            for (var e : anims.entrySet())
                if (e.getKey().startsWith("attack_") && !e.getKey().endsWith("_legs") && e.getValue().getAsJsonObject().has("animation_length"))
                    len = Math.max(len, e.getValue().getAsJsonObject().get("animation_length").getAsDouble());
            return len > 0 ? (int) Math.round(len * 20) : 22;
        } catch (Exception e) { return 22; }
    }
    /** Clip ticks the frame checks expect (a bow clip draws, holds and recovers over 26; a crossbow's shot, kick and span over its first 30; a melee clip its own length), and the gap window between swings. */
    static final int FRAMES = XBOW ? 30 : THROW ? MELEE_T : RANGED ? 26 : MELEE_T, GAP_T = XBOW ? 55 : THROW ? MELEE_T + 1 : RANGED ? 31 : MELEE_T;
    static final Gait.Mode[] MODES = {Gait.Mode.WALK, Gait.Mode.RUN};
    /** March directions per view, seen from the camera north of the line looking south: side on, head on, three-quarter. */
    static final String[] MARCH_VIEWS = {"side", "front", "threequarter"};
    static final double[][] DIRS = {{1, 0}, {0, -1}, {Math.sqrt(.5), -Math.sqrt(.5)}};
    /** Server ticks of one march; the line passes its start point halfway. */
    static final int MARCH = 48;
    /** The march the server is running (set from the client, advanced every server tick by the hook below). */
    static final class March {
        final GeoKnightChecks owner; final GravesAndGlory.Battle b; final String name; final int start; final double v, dx, dz; final boolean column;
        volatile int k = -1;
        March(GeoKnightChecks owner, GravesAndGlory.Battle b, String name, double v, double dx, double dz, boolean column) {
            this.owner = owner; this.b = b; this.name = name; this.start = b.server.getTicks(); this.v = v; this.dx = dx; this.dz = dz; this.column = column;
        }
    }
    static volatile March run;
    /**
     * A duel the server is running between the two tier-III knights (own, slot 2; foe, slot 5), held in place every server tick. Each
     * swing either starts is logged: {entity id, swing world tick, variant, server impact delay, landing world tick (-1 until it lands)}.
     * The battle ticks in its own END_SERVER_TICK hook, registered at mod init before this one, so a strike that left Battle.strikes
     * this tick landed on this world tick. A duelist that has not swung for {@link #NUDGE} ticks (its squad's orders held it) is made
     * to swing the way Tactics does (forced counts these).
     */
    static final class Duel {
        static final int NUDGE = 40;
        final GeoKnightChecks owner; final GravesAndGlory.Battle b; final String mode; final int start;
        final GravesAndGlory.Unit[] us;
        final long[] seen = new long[2], last = new long[2], beatAt = {-1, -1};
        /** The world tick each duelist's nudged shot left (rangedNudge): the swing log reads it a tick later, so it lands here. */
        final long[] shotAt = {-1, -1};
        final Map<GravesAndGlory.Battle.PendingStrike, long[]> open = new HashMap<>();
        final List<long[]> swings = Collections.synchronizedList(new ArrayList<>());
        volatile int k = -1, forced;
        /** Death mode: the foe swing it dies 4 ticks into, and the world tick it was killed (-1 until then). */
        volatile long killSwing = -1, killedAt = -1;
        volatile boolean killedDead;
        Duel(GeoKnightChecks owner, GravesAndGlory.Battle b, String mode) {
            this.owner = owner; this.b = b; this.mode = mode; this.start = b.server.getTicks();
            us = new GravesAndGlory.Unit[]{owner.knights.get(2), owner.knights.get(5)};
            long now = b.world.getTime();
            // A bow duel shoots first 5 ticks in: its shots are spaced wider than a swing (draw, hold, recovery), so the walk fits two each.
            for (int j = 0; j < 2; j++) { seen[j] = us[j].swingTick; last[j] = RANGED ? now - nudgeR(mode) + 5 : now; }
        }
    }
    static volatile Duel duel;
    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            March m = run; if (m != null) m.owner.serverMarch(m, server.getTicks() - m.start);
            Duel d = duel; if (d != null) d.owner.serverDuel(d, server.getTicks() - d.start);
            Hold h = GeoKnightChecks.hold; if (h != null) h.owner.serverHold(h);
            Runnable f = GeoKnightChecks.ablFire; if (f != null) f.run();
        });
    }
    /**
     * Stance and hit stages: every live knight is held on its line mark facing yaw, in this order stance with its shield raised or
     * not, every server tick (the battle's own beat would reset its stance; Tactics.set only resends a change). changedAt is the
     * world tick the held stance last changed, hitAt the world tick of the last test blow.
     */
    static final class Hold {
        final GeoKnightChecks owner; final String stance; final boolean shield; final float yaw;
        /** Capture only: the aim pitch sent with the stance (degrees, + down) and a walk speed (blocks per tick, 0 = stand) backing away from where it faces. */
        final float aim; final double walk;
        Hold(GeoKnightChecks owner, String stance, boolean shield, float yaw) { this(owner, stance, shield, yaw, 0, 0); }
        Hold(GeoKnightChecks owner, String stance, boolean shield, float yaw, float aim, double walk) { this.owner = owner; this.stance = stance; this.shield = shield; this.yaw = yaw; this.aim = aim; this.walk = walk; }
    }
    static volatile Hold hold;
    volatile long changedAt = -1, hitAt = -1;
    /** Capture (charge) only: the camera offset last set by camera(), tracked along with a running squad, the forward offset a stopped run holds (NaN while running), and the world tick a mid-charge volley looses. */
    volatile double camDx, camDist, fixS = Double.NaN;
    volatile float camPitch;
    volatile long shootAt = -1;

    double lineX(int slot) { return cx + (slot < 3 ? -7 + slot * 2 : 3 + (slot - 3) * 2); }

    void serverHold(Hold h) {
        for (int j = 0; j < knights.size(); j++) {
            var u = knights.get(j);
            if (!u.mob.isAlive()) continue;
            if (h.walk < 0 || !Double.isNaN(fixS)) {
                // Capture (charge): run forward along the line at -walk blocks per tick (-1 = the troop's real run speed) from its mark, or stand where the run stopped (fixS);
                // the camera keeps the same offset to the line, so the squad stays framed as it runs.
                double rad = Math.toRadians(h.yaw), dx = Math.sin(rad), dz = -Math.cos(rad), v = h.walk == -1.0 ? SPEEDS[1] : -h.walk;
                double s = !Double.isNaN(fixS) ? fixS : -v * Math.max(0, u.mob.getWorld().getTime() - changedAt);
                stand(u, lineX(j) + dx * s, cz + dz * s, h.yaw);
                if (j == knights.size() - 1) {
                    var p = u.mob.getServer().getPlayerManager().getPlayer(player);
                    if (p != null && !"stunfix".equals(CAPTURE)) p.networkHandler.requestTeleport(cx + camDx + dx * s, cy - .45, cz - camDist + dz * s, 0, camPitch);
                }
            } else if (h.walk > 0) {
                // Capture: retreat along the line backing away from the way the troop faces, from 2 blocks before its mark.
                double rad = Math.toRadians(h.yaw), dx = Math.sin(rad), dz = -Math.cos(rad);
                double s = h.walk * Math.max(0, u.mob.getWorld().getTime() - changedAt) - 2;
                stand(u, lineX(j) + dx * s, cz + dz * s, h.yaw);
            } else stand(u, lineX(j), cz + (CAPTURE != null && stage >= 30 && Math.abs(h.yaw - 180) > 1 && j >= 3 && !videoLine ? -12 : 0), h.yaw); // capture, side views: the far half of the line steps back so no troop faces a neighbour (a neighbour in front starts its attack clip over the stance)
            // capture: a held "stunned" stance is also a real stun (far ahead), so the battle beat's Tactics.stunStance does not drop it; any other hold ends it
            if (h.stance.equals("stunned")) u.stunnedUntil = Long.MAX_VALUE / 4; else if (u.stunnedUntil == Long.MAX_VALUE / 4) u.stunnedUntil = 0;
            Tactics.set(u, h.stance, h.aim); Tactics.shield(u.mob, h.shield);
        }
    }

    /** Holds every knight in a stance (server thread); a new stance or shield stamps changedAt. */
    void hold(GravesAndGlory.Battle b, String stance, boolean shield, float yaw) { hold(b, stance, shield, yaw, 0, 0); }

    void hold(GravesAndGlory.Battle b, String stance, boolean shield, float yaw, float aim, double walk) {
        Hold was = hold;
        if (was == null || !was.stance.equals(stance) || was.shield != shield || was.aim != aim) changedAt = b.world.getTime();
        Hold h = new Hold(this, stance, shield, yaw, aim, walk);
        hold = h; serverHold(h);
    }

    /**
     * Capture only (server thread): on, no troop attacks or casts for the next 400 ticks, so the battle AI never plays its attack_throw or cast
     * clip over the stance being shot (a harpooner in reach of the far half threw during r2 shots); off lifts it when the captures end.
     */
    void quiet(GravesAndGlory.Battle b, boolean on) {
        for (var u : knights) {
            if (on) { u.nextAttack = Math.max(u.nextAttack, b.time + 400); u.abilityAt = Math.max(u.abilityAt, b.time + 400); }
            else { u.nextAttack = Math.min(u.nextAttack, b.time); u.abilityAt = Math.min(u.abilityAt, b.time); }
        }
    }

    /** The same blow on every live knight (server thread), stamped hitAt. */
    void hitAll(GravesAndGlory.Battle b, float bearing, int weight, boolean blocked) {
        hitAt = b.world.getTime();
        for (var u : knights) if (u.mob.isAlive()) MotionEvents.sendHit(u.mob, "slash", "body", bearing, weight, blocked, false);
    }

    /** The same blow with the knockback flag (a bash, a slam): the client plays hit_knockback. Capture only; the body is not moved here (the server moves it in play). */
    void hitAll(GravesAndGlory.Battle b, float bearing, int weight, boolean blocked, boolean knockback) {
        hitAt = b.world.getTime();
        for (var u : knights) if (u.mob.isAlive()) MotionEvents.sendHit(u.mob, "slash", "body", bearing, weight, blocked, false, knockback, MotionEvents.watchers(u.mob));
    }

    /** Stance cases: name, order stance, shield raised. "ready" is no order stance while fighting (a light blow starts it). */
    static final String[][] ALL_STANCE_CASES = {{"ready", "", "0"}, {"brace", "brace", "0"}, {"brace_up", "brace", "1"}, {"wall", "wall", "0"},
        {"wall_up", "wall", "1"}, {"shield_move", "shield_move", "1"}, {"ward", "fall_back", "0"}, {"fall_back_up", "fall_back", "1"}};
    /** Troops that carry a shield; the rest cannot raise one, so their shield-raised stances and the blocked blow are not checked. */
    static final boolean SHIELD = Set.of("knight", "sentinel").contains(TROOP);
    static final String[][] STANCE_CASES = Arrays.stream(ALL_STANCE_CASES).filter(sc -> SHIELD || sc[2].equals("0")).toArray(String[][]::new);
    /** close: the tier III own knight three-quarter at 3.5 blocks; side: the line side on from 8; far: the line from 16. */
    static final String[] STANCE_VIEWS = {"close", "side", "far"};
    /**
     * Capture only (GRAVES_GEO_CAPTURE=1 runs just this, GRAVES_GEO_CAPTURE=with adds it before the checks; never a check): the stance clips shot in game at 0, 45
     * and 90 degrees from the camera, named p1-geo-<troop>-cap-<case>-<angle>-<ticks since the stance began>.png. Cases: name, stance, aim pitch, walk speed,
     * shot ticks, line (the line of six from 8 blocks, one angle). Rally shots are the up beat, the hold, the way down and the tick after it ends.
     */
    static final String CAPTURE = System.getenv("GRAVES_GEO_CAPTURE");
    static final int[] CAP_YAW = {180, 135, 90};
    static final String[] CAP_ANG = {"000", "045", "090"};
    static final Object[][] CAP_CASES = {
        {"hold", "hold", 0f, 0.0, new int[]{30}, false},
        {"rest", "ranged_rest", 0f, 0.0, new int[]{30}, false},
        {"rally", "rally", 0f, 0.0, new int[]{3, 6, 10, 15, 17, 19, 20, 21, 22, 24, 26, 30}, false},
        {"aim-up45", "ranged_aim", -45f, 0.0, new int[]{36}, false},
        {"aim-level", "ranged_aim", 0f, 0.0, new int[]{36}, false},
        {"aim-down30", "ranged_aim", 30f, 0.0, new int[]{36}, false},
        {"fallback", "fall_back", 0f, .169, new int[]{8, 14, 20}, false},
        {"rally-line", "rally", 0f, 0.0, new int[]{10}, true},
    };
    /**
     * Capture only (GRAVES_GEO_CAPTURE=react runs just this, then the knockback-then-stun video segment; never a check): the Batch 2 reaction clips. The 7th element is the kind:
     * stun (hold "stunned"; shots count from its start: the hit-snap entry at 0..4, the dazed loop, the seam at 24 = 4), stunexit (a knockback and a stun, 2 s later the stun
     * ends; shots count from the end), knock (a knockback blow on a troop in the ready guard; shots count from the blow), hitmed (the same blow without the flag, to compare).
     */
    static final Object[][] REACT_CASES = {
        {"stun", "stunned", 0f, 0.0, new int[]{0, 1, 2, 4, 5, 10, 15, 24}, false, "stun"},
        {"stunexit", "", 0f, 0.0, new int[]{2, 4, 6, 8}, false, "stunexit"},
        {"knock", "", 0f, 0.0, new int[]{2, 4, 6, 8, 11, 15}, false, "knock"},
        {"hitmed", "", 0f, 0.0, new int[]{2, 4, 6, 8, 11, 15}, false, "hitmed"},
    };
    /**
     * Capture only (GRAVES_GEO_CAPTURE=charge runs just this; never a check): the Batch 3 charge. The squad runs forward at the troop's real run speed (walk -1) in the
     * charge stance beside the same run in the plain run stance, one shot a tick over a stride (shots 20..28 after the stance began), then the line of six charging.
     * GRAVES_GEO_CHARGE_VIDEO=1 adds the video segments after the sheets.
     */
    static final int[] CHARGE_SHOTS = {20, 21, 22, 23, 24, 25, 26, 27, 28};
    static final Object[][] CHARGE_CASES = {
        {"charge", "charge", 0f, -1.0, CHARGE_SHOTS, false},
        {"run", "run", 0f, -1.0, CHARGE_SHOTS, false},
        {"charge-line", "charge", 0f, -1.0, new int[]{24}, true},
    };
    static Object[][] capCases() { return "react".equals(CAPTURE) ? REACT_CASES : "charge".equals(CAPTURE) ? CHARGE_CASES : CAP_CASES; }

    /**
     * Capture only, never a check (GRAVES_GEO_CAPTURE=video): the review video. A frame is saved every client tick (20 fps) as
     * p1-geo-<troop>-vid-<segment>-<frame>.png. Segments: label, stance, camera yaw, line of six, frames, aim sweep, walk speed.
     */
    /** Video capture of the line of six: all six stay on their marks (the far half does not step back). */
    static volatile boolean videoLine;
    static List<Object[]> videoSegments() {
        List<Object[]> v = new ArrayList<>();
        if ("charge".equals(CAPTURE)) { // the squad charging, the plain run beside it, the line of six, and (ranged troops) the advance that stops to shoot
            v.add(new Object[]{"charge-045", "charge", 135f, false, 56, false, -1.0});
            v.add(new Object[]{"run-045", "run", 135f, false, 40, false, -1.0});
            v.add(new Object[]{"charge-line-150", "charge", 150f, true, 60, false, -1.0});
            if (RANGED) v.add(new Object[]{"charge-shoot-090", "charge", 270f, true, 120, false, -1.0, "chargeshoot"});
            return v;
        }
        if ("stunfix".equals(CAPTURE)) { // a bash: the knockback slide and the stun on the same tick, at the three review angles, then the stun ends
            v.add(new Object[]{"stunfix-000", "", 180f, false, 110, false, 0.0, "stunfix"});
            v.add(new Object[]{"stunfix-045", "", 135f, false, 110, false, 0.0, "stunfix"});
            v.add(new Object[]{"stunfix-090", "", 270f, false, 110, false, 0.0, "stunfix"});
            return v;
        }
        if ("react".equals(CAPTURE)) { // a light blow (the ready guard), a knockback, a 2 s stun, then the stun ends
            v.add(new Object[]{"knockstun-045", "", 135f, false, 92, false, 0.0, "knockstun"});
            return v;
        }
        v.add(new Object[]{"rally-line-030", "rally", 150f, true, 60, false, 0.0});
        v.add(new Object[]{"hold-045", "hold", 135f, false, 60, false, 0.0});
        if (!"knight".equals(TROOP)) v.add(new Object[]{"rest-045", "ranged_rest", 135f, false, 40, false, 0.0});
        if (!"knight".equals(TROOP)) v.add(new Object[]{"aim-sweep-045", "ranged_aim", 135f, false, 90, true, 0.0});
        if (!"knight".equals(TROOP)) v.add(new Object[]{"aim-sweep-000", "ranged_aim", 180f, false, 90, true, 0.0});
        v.add(new Object[]{"fall_back-045", "fall_back", 135f, false, 60, false, .169});
        v.add(new Object[]{"fall_back-090", "fall_back", 90f, false, 60, false, .169});
        return v;
    }
    /** Aim pitch (degrees, + down) at video frame f: held at -45 for the stance blend-in, then -45 up to +30 and back over 80 frames, eased. */
    static float sweepPitch(int f) {
        double u = Math.max(0, Math.min(1, (f - 10) / 80.0)), t = u < .5 ? 2 * u : 2 - 2 * u;
        return (float) (-45 + 75 * (t * t * (3 - 2 * t)));
    }
    final Map<Integer, Long> stanceLag = new TreeMap<>();
    final List<String> stanceBad = new ArrayList<>();

    /** Hit cases: name, bearing (degrees, + from the knight's right), weight; "block" is taken on the raised shield. */
    static final String[] ALL_HIT_CASES = {"light-front", "medium-front", "heavy-front", "medium-right", "medium-left", "medium-back", "medium-frontright", "block"};
    static final int READY_HIT = 14;
    static final String[] HIT_CASES = SHIELD ? ALL_HIT_CASES : Arrays.copyOf(ALL_HIT_CASES, ALL_HIT_CASES.length - 1);
    static final float[] HIT_BEARING = {0, 0, 0, 90, -90, 180, 45, 0};
    static final int[] HIT_WEIGHT = {0, 1, 2, 1, 1, 1, 1, 1};
    static final String[] HIT_VIEWS = {"side", "threequarter", "far"};
    final List<String> hitBad = new ArrayList<>();

    /** Death cases: name and the lethal blow's bearing (degrees, + from the knight's right). */
    static final String[] DEATH_CASES = {"back", "forward", "left", "right"};
    static final float[] DEATH_BEARING = {0, 180, 90, -90};
    final List<String> deathBad = new ArrayList<>();
    volatile int deathVictim = -1;
    int deathFrames;
    String deathShown = "";
    double deathMaxT, deathPrevT;
    double hitMaxScale, hitMaxLegs;
    int hitFrames;

    /**
     * Pulled cases (hit_v1 flag 8): name and view. The tier III own troop is dragged PULL blocks toward a live foe (foe()), PULL_FROM blocks
     * in front of it (front: the client plays hit_pulled, a forward stumble) or behind it (back: dragged backwards, hit_knockback), by
     * the server's own path, in the Undertow's order: Abilities.pulls marks it (markPulled), Combat.resolve lands the blow and sends its
     * hit_v1 (the pulled flag from Abilities.takePulled), then Abilities.pull moves it. Nothing holds it in place meanwhile.
     */
    static final String[][] PULL_CASES = {{"front", "side"}, {"front", "threequarter"}, {"back", "side"}};
    static final double PULL = 1.5, PULL_FROM = 5;
    final List<String> pullBad = new ArrayList<>();
    volatile double pullMoved = Double.NaN;
    volatile int pullSent = -1;
    /** The puller (and the rise's striker): the first live foe, tier III first (the attack death duel killed slot 5). Server thread. */
    int foe() { for (int j : new int[] {5, 4, 3}) if (knights.get(j).mob.isAlive()) return j; return 5; }
    double pullMaxScale, pullMaxLegs;
    int pullFrames;
    String pullClips = "";

    /**
     * The rise (rise_v1): a troop whose level 3 row is the rise (Abilities.riseRow: the Hollow King's Undying King) takes a lethal
     * blow through Combat.resolve with the rise's roll forced (Houses.forceRise), so Houses.allowDeath keeps it alive and sends
     * rise_v1. The server must keep it down (stunned and still) the whole display, Houses.UNDYING_DOWN; its client must play fake_death
     * then rise over every layer (DeathController.riseAt), ending on the span's last tick. Cases: the rise seen three-quarter and side
     * on (the troop's once-only rise is re-armed between them: Unit.rose cleared), then "death": a second lethal blow RISE_KILL ticks
     * into a rise (it rose already, so it dies) must not snap to a fall from the guard (DeathController.COLLAPSE: fake_death).
     * FAKE_T and RISE_T: the clip lengths (gb_hollowking.animation.json); the span is the server's window, or both clips when it is 0.
     */
    static final double FAKE_T = 30, RISE_T = 40;
    static final String[] RISE_CASES = {"threequarter", "side", "death"};
    static final int RISE_KILL = 45;
    final List<String> riseBad = new ArrayList<>();
    volatile long riseTick = -2, riseStun = -1, riseDown = -1, riseKilledAt = -1;
    volatile int riseWindow = -1, riseSent = -1;
    volatile boolean riseRose, riseAlive, riseKilled;
    int riseDeathFrames; String riseDeathClips = "";
    /** Whether the tier III troop has the rise row: -1 until the server has answered. */
    volatile int riseRow = -1;
    volatile int riseVictim = -1;
    int riseFrames, risePhase;
    double riseMaxFakeT = -1, riseMaxT = -1, riseFirstRise = -1, riseMinW = 2;

    /** Places the camera for a stance or hit view (server thread) and returns the knights' yaw for it. */
    float view(GravesAndGlory.Battle b, String view, boolean stances) {
        switch (view) {
            case "close", "threequarter" -> { camera(b, -3, 3.5, 3); return 150; }
            case "side" -> { if (stances) camera(b, 0, 8, 8); else camera(b, -3, 3.5, 3); return 90; }
            default -> { camera(b, 0, 16, 3); return 150; }
        }
    }

    int lastShot;
    boolean reported;
    final List<double[]> samples = new ArrayList<>();
    final Set<String> wrongClip = new LinkedHashSet<>();
    volatile UUID player;
    volatile boolean battleReady, placed;
    volatile String why = "";
    volatile double cx, cy, cz;
    final List<GravesAndGlory.Unit> knights = new ArrayList<>();
    int stage, at, i;

    GeoGaps gaps;
    void next(int tick, int s) { stage = s; at = tick; }

    @Override public void cleanup(MinecraftClient c) {
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
        if (c.getServer() != null) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
    }

    /** Clears the field and stands the six knights in a line 7 blocks south of the player. */
    void place(GravesAndGlory.Battle b) {
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) { why = "no player"; return; }
        var spec = Campaign.troop(TROOP);
        if (spec == null) { why = "no " + TROOP + " troop"; return; }
        for (int g = 0; g < SPEEDS.length; g++) SPEEDS[g] = (g == 0 ? .169 : .264) * spec.speed() / .28;
        for (var u : b.units) { GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); }
        b.units.clear(); b.clearCorpses();
        GravesAndGlory.Squad mine = null, foe = null;
        for (var s : b.squads.values()) { if (s.team == 0 && mine == null) mine = s; else if (s.team == 1 && foe == null) foe = s; }
        var own = b.squads.get(player);
        if (own != null) { mine = own; for (var s : b.squads.values()) if (s.team != own.team) { foe = s; break; } }
        if (mine == null || foe == null) { why = "squads " + b.squads.size(); return; }
        p.getServerWorld().setTimeOfDay(6000);
        cx = Math.floor(p.getX()) + .5; cz = Math.floor(p.getZ()) + 7.5;
        cy = Arena.ground((int) cx, (int) cz) + 1;
        int slot = 0;
        for (var s : List.of(mine, foe)) for (int t = 1; t <= 3; t++) {
            // Own side at cx-7..cx-3, enemy at cx+3..cx+7: six blocks apart so the two sides never come to blows.
            double x = cx + (slot < 3 ? -7 + slot * 2 : 3 + (slot - 3) * 2), z = cz;
            var u = b.spawn(s, spec, 20 + slot, new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z));
            u.level = t; u.weaponTier = t; u.armourTier = t;
            u.mob.setAiDisabled(true); u.mob.setInvulnerable(true);
            RankSync.changed(b, u);
            knights.add(u);
            slot++;
        }
        turn(180);
        placed = true;
    }

    /** Stands the six knights around (x, z) facing (dx, dz): a column along the march for the side view, else a rank across it. */
    void march(double x, double z, double dx, double dz, boolean column) {
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        double ax = column ? dx : -dz, az = column ? dz : dx;
        for (int j = 0; j < knights.size(); j++) {
            var m = knights.get(j).mob;
            // Own side then enemy side, 2.4 apart within a side and 6 between the sides, so they never come to blows.
            double o = j < 3 ? -3 - (2 - j) * 2.4 : 3 + (j - 3) * 2.4, kx = x + ax * o, kz = z + az * o;
            m.setPosition(kx, Arena.ground((int) Math.floor(kx), (int) Math.floor(kz)) + 1, kz);
            m.setYaw(yaw); m.setBodyYaw(yaw); m.setHeadYaw(yaw);
        }
    }

    /** One sample of every knight's client stride clock: its measured speed (mean kept per knight) and chosen clip. */
    void sample(MinecraftClient c, Gait.Mode want, int mk) {
        double[] row = new double[knights.size()];
        for (int j = 0; j < knights.size(); j++) {
            var e = c.world == null ? null : c.world.getEntityById(knights.get(j).mob.getId());
            Gait g = e == null ? null : Gait.LIVE.get(e);
            row[j] = g == null ? Double.NaN : Math.abs(g.speed);
            if (g == null) wrongClip.add(j + ":none"); else if (g.mode != want) wrongClip.add(String.format("%d:%s@%d(s%.3f t%.3f)", j, g.mode, mk, g.speed, g.trend));
        }
        samples.add(row);
    }

    /** Passes when every knight's mean client speed is within 10% of the true speed and its clip was right in every sample. */
    void readBack(String name, double v) {
        StringBuilder seen = new StringBuilder();
        boolean ok = !samples.isEmpty() && wrongClip.isEmpty();
        for (int j = 0; j < knights.size(); j++) {
            // The median, not the mean: a server hitch on a loaded machine delivers a tick's motion late and the smoothed speed dips
            // for a frame or two on every knight at once (the stride clock still integrates the full distance). The clip choice
            // above is the hard gate; the min shows any dip.
            double[] col = new double[samples.size()];
            for (int i = 0; i < col.length; i++) col[i] = samples.get(i)[j];
            Arrays.sort(col);
            double med = col.length == 0 ? 0 : col[col.length / 2], lo = col.length == 0 ? 0 : col[0], hi = col.length == 0 ? 0 : col[col.length - 1];
            ok &= Math.abs(med - v) <= .1 * v;
            seen.append(String.format(" %.3f[%.3f-%.3f]", med, lo, hi));
        }
        Phase1Checks.report("p1-geo-" + TROOP + "-march-" + name, ok, "true " + v + ", " + samples.size() + " samples, client median[min-max]:" + seen
            + (wrongClip.isEmpty() ? "" : ", wrong clip " + wrongClip));
    }

    /**
     * Server tick k of a march: the line at its point, the camera still. Close (k < 30) the camera frames where the line is
     * mid-way through the close shots (k 20); far, where it is mid-way through the far shots (k 36).
     */
    void serverMarch(March m, int k) {
        m.k = k;
        double s = m.v * (Math.min(k, MARCH) - MARCH / 2.0);
        march(cx + m.dx * s, cz + 2 + m.dz * s, m.dx, m.dz, m.column);
        boolean near = k < 30;
        double f = m.v * ((near ? 20 : 36) - MARCH / 2.0), dist = near ? 8 : 16;
        camera(m.b, m.dx * f, -(2 + m.dz * f) + dist, near ? 8 : 3);
    }

    static void stand(GravesAndGlory.Unit u, double x, double z, float yaw) {
        var m = u.mob;
        if (!m.isAlive()) return;
        m.setPosition(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z);
        m.setVelocity(0, 0, 0);
        m.setYaw(yaw); m.setBodyYaw(yaw); m.setHeadYaw(yaw);
    }

    /** Walk mode: the own knight walks +x at the knight's walk speed for this many server ticks, the foe backing up 2 blocks ahead. */
    static final int WALK = 70;
    /** Walk mode: the duelists stand at their start marks this many ticks first (the move there is a 5 block teleport). */
    static final int WALK0 = 20;

    /** Server tick k of a duel: the duelists held (or walked), the other four kept 30 blocks behind the camera, swings logged. */
    void serverDuel(Duel d, int k) {
        d.k = k;
        var b = d.b;
        long now = b.world.getTime();
        for (int j = 0; j < knights.size(); j++) if (j != 2 && j != 5) stand(knights.get(j), cx + (j < 3 ? -20 - j * 3 : 20 + j * 3), cz - 30, 0);
        var f = d.us[1];
        if (d.mode.equals("walk")) {
            double x = cx - 6 + SPEEDS[0] * Math.max(0, Math.min(k - WALK0, WALK));
            stand(d.us[0], x, cz, -90); stand(f, x + GAP, cz, 90);
        } else { stand(d.us[0], cx - 1, cz, -90); stand(f, cx - 1 + GAP, cz, 90); }
        for (int j = 0; j < 2; j++) {
            var u = d.us[j];
            if (u.swingTick != d.seen[j] && u.swingTick >= 0) {
                d.seen[j] = u.swingTick; d.last[j] = now;
                GravesAndGlory.Battle.PendingStrike ps = null;
                for (var st : b.strikes) if (st.attacker() == u && st.swungAt() == u.swungAt) ps = st;
                // A shot (Projectiles.loose) stamps its swing at the loose: its clip's impact plus its hold before, landing now.
                long[] row = RANGED
                    ? new long[]{u.mob.getId(), u.swingTick, u.catchVariant, Tactics.variants(u.spec.family()).get(u.catchVariant).impactTicks(), d.shotAt[j] >= 0 ? d.shotAt[j] : now, Projectiles.hold(u)}
                    : new long[]{u.mob.getId(), u.swingTick, u.lastVariant, ps == null ? 0 : ps.landsAt() - ps.swungAt(), ps == null ? now : -1, 0};
                d.swings.add(row);
                if (ps != null && !RANGED) d.open.put(ps, row);
                if (!RANGED && j == 1 && d.mode.equals("death") && k > 20 && d.killSwing < 0) d.killSwing = u.swingTick;
            }
        }
        for (var it = d.open.entrySet().iterator(); it.hasNext(); ) { var en = it.next(); if (!b.strikes.contains(en.getKey())) { en.getValue()[4] = now; it.remove(); } }
        if (d.killSwing >= 0 && d.killedAt < 0 && now - d.killSwing == KILL_AT) {
            f.mob.setInvulnerable(false); f.mob.kill(); d.killedAt = now; d.killedDead = f.mob.isDead();
        }
        for (int j = 0; j < 2; j++) {
            var u = d.us[j]; LivingEntity enemy = d.us[1 - j].mob;
            if (RANGED) { rangedNudge(d, j, k, now); continue; }
            if (u.mob.isAlive() && enemy.isAlive() && now - d.last[j] > Duel.NUDGE && b.time >= u.stunnedUntil && !Tactics.pending(b, u)) {
                u.nextAttack = b.time + u.spec.cooldown(); u.mob.swingHand(Hand.MAIN_HAND);
                Tactics.shoot(b, u, enemy, b.time, false);
                d.forced++; d.last[j] = now;
            }
        }
    }

    /**
     * A bow duelist's shot, as Tactics' beat makes it: the beat before, its draw is announced (Projectiles.drawAhead, 10 ahead); on its
     * beat its cooldown is taken, and the arrow leaves its loose ticks later (Tactics.looseLate). The battle's own beat is kept off
     * (nextAttack far ahead) so only these shots play. A mage's release is the mend shot Abilities.mend fires (a heal of 0 at the
     * foe duelist), announced the same way (Abilities.mend calls Projectiles.drawAhead with the same arguments). A hexer's is its blight and a mist caller's its mist
     * (Tactics.shoot, its spell cooldown cleared first so every nudged beat casts), announced as Tactics.tick does for it.
     */
    void rangedNudge(Duel d, int j, int k, long now) {
        var b = d.b; var u = d.us[j]; LivingEntity enemy = d.us[1 - j].mob;
        u.nextAttack = b.time + 1000;
        if (d.beatAt[j] >= 0) {
            if (now < d.beatAt[j]) return;
            d.beatAt[j] = -1;
            if (u.mob.isAlive() && enemy.isAlive() && b.time >= u.stunnedUntil) { u.mob.swingHand(Hand.MAIN_HAND); if (CAST) Projectiles.fire(b, u, enemy, "mend", 0f); else { if (SPELL) u.abilityAt = 0; Tactics.shoot(b, u, enemy, b.time, true); } d.shotAt[j] = now; }
            return;
        }
        if (!u.mob.isAlive() || !enemy.isAlive() || now - d.last[j] <= nudgeR(d.mode) || b.time < u.stunnedUntil || Tactics.pending(b, u)) return;
        if (Projectiles.drawAhead(b, u, enemy, 10) <= 0) return;
        d.beatAt[j] = now + 10 + Projectiles.looseTicks(u);
        d.forced++; d.last[j] = now;
        if (j == 1 && d.mode.equals("death") && k > 20 && d.killSwing < 0)
            { int imp = Tactics.variants(u.spec.family()).get(0).impactTicks(); d.killSwing = Tactics.drawsAhead(u) ? Projectiles.shotStart(u.mob.getUuid(), now + 10, imp) : now + 10 - imp; }
    }

    /** Puts the camera (the player's eye) at (x, z) looking along yaw. */
    void eye(GravesAndGlory.Battle b, double x, double z, float yaw, float pitch) {
        var p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        p.setNoGravity(true);
        p.networkHandler.requestTeleport(x, cy - .45, z, yaw, pitch);
    }

    /** Attack views: side on to the duel, three-quarter on the own knight's front, and side on at 16 blocks: {dx, dz, yaw, pitch}. */
    static final String[] ATTACK_VIEWS = {"side", "threequarter", "far"};
    static final double[][] ATTACK_EYES = {{0, -4.5, 0, 8}, {1.5, 3.5, 144.5, 8}, {0, -16, 0, 3}};
    /** Frames taken this view (clip:tick), and the client's view of every swing: id:start to variant, and to its clip's impact tick. */
    final Set<String> taken = new HashSet<>();
    /** Frames a quickened late wind-up stepped past between two ticks (drawn between harness samples, so no shot of their own). */
    final Set<String> stepped = new HashSet<>();
    final Map<String, Integer> clientVariant = new HashMap<>();
    final Map<String, Double> clientImpact = new HashMap<>();
    /** Swings (id:start) whose troop the client drew at some frame of the clip: one behind the camera all along can show nothing. */
    final Set<String> clientDrawn = new HashSet<>();
    /** The client tick each swing was first listed: a frame drawn before that did not know of it, so it cannot count as drawn. */
    final Map<String, Long> clientKnown = new HashMap<>();
    double minImpactLegsW = 2, maxWalkLegsW = 0, maxOnsetLegsW = 0; int walkSettledSamples;
    String minImpactWhy = "";
    double minImpactW = 2, maxGapW = 0, maxDeadW = 0, maxBeforeDeathW = 0, foeBackMin = 9;
    /** The foe's backing speed each swinging sample: a server hitch dips the smoothed speed for a frame (as in readBack), so the median must
     *  read a real backpedal and the minimum only that it never walked forward. */
    final List<Double> foeBack = new ArrayList<>();
    static double median(List<Double> v) { if (v.isEmpty()) return 0; var c = new ArrayList<>(v); java.util.Collections.sort(c); return c.get(c.size() / 2); }
    int impactSamples, gapSamples, walkSwingSamples, deadSamples;
    final Set<String> walkWrong = new LinkedHashSet<>(), deadWrong = new LinkedHashSet<>();

    /** One client tick of a duel: every duelist's announced swings and what its attack layer drew last frame. */
    void watch(MinecraftClient c, Duel d) {
        if (c.world == null) return;
        long now = c.world.getTime();
        for (var u : d.us) {
            int id = u.mob.getId();
            var e = c.world.getEntityById(id);
            for (var a : MotionEvents.Client.attacks(id)) { clientVariant.putIfAbsent(id + ":" + a.startTick(), a.variant()); clientKnown.putIfAbsent(id + ":" + a.startTick(), now); }
            AttackController ac = e == null ? null : AttackController.LIVE.get(e);
            if (ac == null || ac.shownAt < 0) continue;
            // Everything below judges the last frame drawn, at that frame's own time (a frame can trail the client tick by one or more).
            long latest = Long.MIN_VALUE;
            for (var a : MotionEvents.Client.attacks(id)) if (a.startTick() <= ac.shownAt) latest = Math.max(latest, a.startTick());
            if (ac.shownStart != Long.MIN_VALUE && ac.shownImpact >= 0) clientImpact.putIfAbsent(id + ":" + ac.shownStart, ac.shownImpact + ac.shownLead);
            if (ac.shownAt > now - 1) for (var a : MotionEvents.Client.attacks(id)) if (a.startTick() + ablLead <= ac.shownAt && ac.shownAt <= a.startTick() + ablLead + (ABL_CAST ? Math.min(30, MELEE_T - 1) : 30) && ac.shownAt > clientKnown.getOrDefault(id + ":" + a.startTick(), Long.MAX_VALUE)) clientDrawn.add(id + ":" + a.startTick());
            boolean dead = ac.shownDeath >= 0;
            double t = ac.shownAt - latest;
            if (!dead && latest != Long.MIN_VALUE && ac.shownStart == latest && ac.shownImpact >= 0 && Math.abs(ac.shownT - ac.shownImpact) <= 1) {
                if (ac.shownWeight < minImpactW) minImpactWhy = String.format("%d:%d at %.2f t %.2f seen %.2f", id, latest, ac.shownAt, ac.shownT, ac.shownSeenAt);
                minImpactW = Math.min(minImpactW, ac.shownWeight); impactSamples++;
                if (d.mode.equals("stand")) minImpactLegsW = Math.min(minImpactLegsW, ac.shownLegsWeight);
            }
            if (!dead && latest != Long.MIN_VALUE && t >= GAP_T + ablLead && t <= GAP_T + ablLead + 2.5) { maxGapW = Math.max(maxGapW, ac.shownWeight); gapSamples++; }
            Gait g = Gait.LIVE.get(e);
            if (d.mode.equals("walk") && d.k > WALK0 + 6 && d.k < WALK0 + WALK && ac.shownWeight > .5) {
                walkSwingSamples++;
                StanceController st = StanceController.LIVE.get(e);
                // The footwork fades out over the 3 ticks still takes to reach 0 once the gait walks (no pop); after that it must be gone.
                if (st != null && st.shownStill <= .001) { maxWalkLegsW = Math.max(maxWalkLegsW, ac.shownLegsWeight); walkSettledSamples++; }
                else maxOnsetLegsW = Math.max(maxOnsetLegsW, ac.shownLegsWeight);
                if (g == null || g.mode != Gait.Mode.WALK) walkWrong.add((u == d.us[0] ? "own" : "foe") + ":" + (g == null ? "none" : g.mode) + "@" + d.k);
                if (u == d.us[1] && g != null) { foeBackMin = Math.min(foeBackMin, -g.speed); foeBack.add(-g.speed); }
            }
            if (d.mode.equals("death") && u == d.us[1] && e instanceof LivingEntity l) {
                if (!dead && d.killSwing >= 0 && ac.shownAt - d.killSwing >= KILL_AT - 2 && ac.shownAt - d.killSwing <= KILL_AT) maxBeforeDeathW = Math.max(maxBeforeDeathW, ac.shownWeight);
                if (ac.shownDeath >= AttackController.DEATH_OUT) { deadSamples++; maxDeadW = Math.max(maxDeadW, ac.shownWeight); }
                if (l.deathTime >= 1 && g != null && g.mode != Gait.Mode.IDLE) deadWrong.add(g.mode + "@" + l.deathTime);
            }
        }
    }

    /**
     * Every server swing of the duel (but the last 12 ticks') reached the client with the same variant and start, its clip's impact
     * marker equals the server's impact delay, and the strike landed exactly that many ticks after the swing began.
     */
    void syncReport(Duel d) {
        int ok = 0, n = 0, unseen = 0; List<String> bad = new ArrayList<>();
        long cut = d.b.world.getTime() - 12;
        synchronized (d.swings) {
            for (long[] r : d.swings) {
                if (r[1] > cut) continue;
                String key = r[0] + ":" + r[1];
                // A troop behind the camera for its whole clip (a bow duel's foe, 10 blocks off, in the three-quarter view) is never drawn.
                if (!clientImpact.containsKey(key) && !clientDrawn.contains(key)) { unseen++; continue; }
                n++;
                Integer v = clientVariant.get(key); Double imp = clientImpact.get(key);
                boolean good = v != null && v == r[2] && imp != null && imp == r[3] + ABL_WINDUP && r[4] - r[1] == r[3] + r[5]; // an ability cast's clip lands on its windup's end
                if (good) ok++; else if (bad.size() < 6) bad.add(String.format("%s v%d/%s delay%d clip%s landed+%d", key, r[2], v, r[3], imp, r[4] - r[1]));
            }
        }
        Phase1Checks.report("p1-geo-" + TROOP + "-attack-sync-" + d.mode, n >= 4 && ok == n,
            ok + "/" + n + " swings matched (variant, clip impact = server delay = landing)" + (d.forced > 0 ? ", " + d.forced + " nudged" : "") + (unseen > 0 ? ", " + unseen + " off camera" : "") + (bad.isEmpty() ? "" : ", bad " + bad));
    }


    /** Capture only (GRAVES_GEO_CAPTURE=slash, never a check): slash review views around the own knight (it faces +x, its sword arm is the +z side): name, offset x, offset z, height above the usual eye, yaw. */
    static final Object[][] SLASH_VIEWS = {
        {"r090", 0.9, 3.4, 0.0, 180f}, {"r045", 2.6, 2.6, 0.0, 135f}, {"r135", -2.2, 2.2, 0.0, -135f},
        {"close", 1.2, 1.8, 0.0, 146.3f}, {"top", 0.3, 0.6, 4.5, 180f}};
    void slashEye(GravesAndGlory.Battle b, Object[] v) {
        var p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        double ox = (Double) v[1], oz = (Double) v[2], dy = (Double) v[3];
        boolean top = ((String) v[0]).equals("top");
        float pitch = top ? 90f : (float) Math.toDegrees(Math.atan2(.15 + dy, Math.hypot(ox, oz)));
        p.setNoGravity(true);
        p.networkHandler.requestTeleport(cx - 1 + ox, cy - .45 + dy, cz + oz, (Float) v[4], pitch);
    }

    void turn(float yaw) {
        for (var u : knights) { u.mob.setYaw(yaw); u.mob.setBodyYaw(yaw); u.mob.setHeadYaw(yaw); u.mob.prevYaw = yaw; }
    }

    /** Puts the camera (the player's eye) dist blocks north of the line's centre, looking south and slightly down. */
    void camera(GravesAndGlory.Battle b, double dx, double dist, float pitch) {
        var p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        p.setNoGravity(true);
        camDx = dx; camDist = dist; camPitch = pitch;
        p.networkHandler.requestTeleport(cx + dx, cy - .45, cz - dist, 0, pitch);
    }

    /**
     * Capture (GRAVES_GEO_CAPTURE=ability; abilitywith runs the checks after it), plus one check: the troop's ability clip (work/anim-clip-spec
     * C3 mend, C4 blight, C6 mist, C7 howl) cast for real by the server on the own three (Projectiles.fire for a mend, Abilities.attack for a
     * blight or a mist, Abilities.howl for the howl), shot every tick at 0, 45 and 90 degrees from the camera as
     * p1-geo-<troop>-abl-<angle>-<ticks from the clip's start, +10>.png. p1-geo-<troop>-ability-sync: on the own tier III the frame the client's
     * clip crosses its impact marker lands on the server's tick (the mend's send, the blight's or mist's windup end); the howl, which has no
     * cast event, starts within 3 ticks of the server's howl (the attribute sync's delay).
     */
    /** The mend's release, run on the server tick exactly 10 ticks after its announce (as Abilities.mend's next beat), then cleared. */
    static volatile Runnable ablFire;
    /** The client tick the cast was queued on: the howl has no server start tick on the client, so its clip start is judged on the client's clock. */
    double ablClientCast = Double.NaN;
    volatile long ablServer = Long.MIN_VALUE;
    volatile int ablSubject = -1;
    double ablCross = Double.NaN, ablPrevT = -1, ablClipStart = Double.NaN;
    int ablChecked;
    final List<String> ablBad = new ArrayList<>();

    /**
     * Casts the troop's ability on the own three for real (server thread); ablServer is the own tier III's release in world ticks. A mend is
     * announced (phase 0: Projectiles.drawAhead, as Abilities.mend does the beat before) and sent 10 ticks later (phase 1); the rest cast at phase 0.
     */
    /** Ends any howl on the line (a natural one before the stage, or the last view's, which lasts as long as a view): the client sees its modifier off, so the next cast's howl starts on its edge. */
    void unhowl() {
        for (var u : knights) {
            u.howlUntil = 0;
            var speed = u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(Abilities.HOWL);
        }
    }

    void castAbility(GravesAndGlory.Battle b, int phase) {
        long now = b.world.getTime();
        for (int s = 0; s < 3; s++) {
            var u = knights.get(s);
            u.abilityAt = 0; u.stunnedUntil = 0;
            var row = Abilities.row(u);
            long rel = now;
            boolean mend = row.mechanic().equals("mend");
            if (mend != (phase == 1) && !(mend && phase == 0)) continue;
            if (mend && phase == 0) {
                Projectiles.drawAhead(b, u, knights.get(s == 0 ? 1 : 0).mob, 10);
                if (s == 2) { ablServer = now + 10; ablSubject = u.mob.getId(); long at = now + 10; ablFire = () -> { if (b.world.getTime() >= at) { ablFire = null; castAbility(b, 1); } }; }
                continue;
            }
            switch (row.mechanic()) {
                case "mend" -> Projectiles.fire(b, u, knights.get(s == 0 ? 1 : 0).mob, "mend", 0f);
                case "blight", "mist" -> { Abilities.attack(b, u, knights.get(5 - s).mob, b.time); rel = now + (u.windupUntil - b.time); }
                default -> Abilities.howl(b, u, row, b.time);
            }
            if (s == 2) { ablServer = rel; ablSubject = u.mob.getId(); }
        }
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 30000 && stage != 9 && stage < 99) { Phase1Checks.report("p1-geo-" + TROOP + "-timeout", false, "stuck at stage " + stage); next(tick, 9); }
        int k = tick - at;
        GeoLayerRest.where = "stage " + stage + (stage >= 99 ? "" : " case " + i);
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); GeoLayerRest.start(); next(tick, 1); } }
            case 1 -> {
                if (!battleReady) {
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true; });
                    if (k > 600) { Phase1Checks.report("p1-geo-" + TROOP, false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                if (k == 80) Phase1Checks.server(c, this::place);
                if (k > 100) {
                    if (!placed) { Phase1Checks.report("p1-geo-" + TROOP + "-placed", false, "could not place knights: " + why); next(tick, 9); return; }
                    Phase1Checks.report("p1-geo-" + TROOP + "-placed", true, "six knights");
                    i = 0; next(tick, "ability".equals(CAPTURE) || "abilitywith".equals(CAPTURE) ? 40 : "gaps".equals(CAPTURE) ? 32 : "slash".equals(CAPTURE) ? 33 : "video".equals(CAPTURE) || "stunfix".equals(CAPTURE) ? 31 : CAPTURE != null ? 30 : 2);
                }
            }
            case 2 -> {
                String view = VIEWS[i];
                float yaw = YAWS[i];
                // Re-aimed every tick: the battle's own logic turns troops back toward the enemy.
                Phase1Checks.server(c, b -> turn(yaw));
                if (k == 1) Phase1Checks.server(c, b -> camera(b, 0, 8, 8));
                if (k == 2) { c.setScreen(null); c.options.hudHidden = true; c.options.setPerspective(Perspective.FIRST_PERSON); }
                if (k == 40) Phase1Checks.shot(c, "geo-" + TROOP + "-" + view + "-near");
                if (k == 41) Phase1Checks.server(c, b -> camera(b, 0, 16, 3));
                if (k == 80) Phase1Checks.shot(c, "geo-" + TROOP + "-" + view + "-far");
                if (k == 81) { if (++i < VIEWS.length) next(tick, 2); else { i = 0; next(tick, 3); } }
            }
            case 3 -> {
                // Tier III close-ups: the player's (slot 2, x = cx - 3) and the enemy's (slot 5, x = cx + 7), three-quarter.
                double dx = i == 0 ? -3 : 7;
                Phase1Checks.server(c, b -> turn(150));
                if (k == 1) Phase1Checks.server(c, b -> camera(b, dx, 3.5, 3));
                if (k == 40) Phase1Checks.shot(c, "geo-" + TROOP + "-t3-" + (i == 0 ? "own" : "foe") + "-close");
                if (k == 41) { if (++i < 2) next(tick, 3); else {
                    Phase1Checks.report("p1-geo-" + TROOP, true, VIEWS.length + " views near and far, two close-ups");
                    i = 0; next(tick, 4); } }
            }
            case 4 -> {
                int g = i / MARCH_VIEWS.length, vw = i % MARCH_VIEWS.length;
                String name = GAITS[g] + "-" + MARCH_VIEWS[vw];
                if (k == 1) {
                    double v = SPEEDS[g], dx = DIRS[vw][0], dz = DIRS[vw][1];
                    lastShot = -1; reported = false; samples.clear(); wrongClip.clear();
                    Phase1Checks.server(c, b -> run = new March(this, b, name, v, dx, dz, vw == 0));
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                March m = run;
                if (k > 400) { Phase1Checks.report("p1-geo-" + TROOP + "-march-" + name, false, "march never ran"); run = null; next(tick, 9); return; }
                if (m == null || !m.name.equals(name) || m.k == lastShot) return;
                int mk = lastShot = m.k;
                if (mk >= 12 && mk < 28) Phase1Checks.shot(c, "geo-" + TROOP + "-" + name + "-near-" + String.format("%02d", mk));
                if (mk >= 32 && mk < 40) Phase1Checks.shot(c, "geo-" + TROOP + "-" + name + "-far-" + String.format("%02d", mk));
                // The run is chosen on the 4-tick speed trend, so sample from mk 22: the trend is past WALK_TO_RUN by then on every knight.
                if (mk >= 22 && mk < 30) sample(c, MODES[g], mk);
                if (mk >= 30 && !reported) { reported = true; readBack(name, SPEEDS[g]); }
                if (mk >= 44) { run = null; if (++i < GAITS.length * MARCH_VIEWS.length) next(tick, 4); else { i = 0; next(tick, 5); } }
            }
            case 5 -> {
                // Standing duel, one view at a time: every tick of each of the three swings, frame by frame (far: every other tick).
                String view = ATTACK_VIEWS[i];
                double[] eyeAt = ATTACK_EYES[i];
                boolean far = view.equals("far");
                if (k == 1) {
                    taken.clear(); stepped.clear();
                    if (i == 0) {
                        clientVariant.clear(); clientImpact.clear(); clientDrawn.clear(); clientKnown.clear(); minImpactW = 2; minImpactLegsW = 2; maxGapW = 0; impactSamples = gapSamples = 0;
                        Phase1Checks.server(c, b -> duel = new Duel(this, b, "stand"));
                    }
                    Phase1Checks.server(c, b -> eye(b, cx + eyeAt[0], cz + eyeAt[1], (float) eyeAt[2], (float) eyeAt[3]));
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                Duel d = duel;
                if (d == null || !d.mode.equals("stand")) { if (k > 200) { Phase1Checks.report("p1-geo-" + TROOP + "-attack", false, "duel never ran"); next(tick, 9); } return; }
                watch(c, d);
                // Tick 0 is weight 0 by design (the same pose as the frame before it), and the client often hears of a swing a
                // fraction of a tick after it starts, so it is saved when caught but not required: ticks 1 to FRAMES-1 (far: the even
                // ones) of every variant are (the knight: 3 clips, ticks 1-21, far 2-20).
                // A bow duel at range always shoots the same arc, so one clip's frames: ticks 1-25 (far: 2-24).
                int nv = Tactics.variants(d.us[0].spec.family()).size();
                int want = XBOW ? (far ? 14 : 29) : THROW ? (far ? (FRAMES - 1) / 2 : FRAMES - 1) : RANGED ? (far ? 12 : 25) : nv * (far ? (FRAMES - 1) / 2 : FRAMES - 1);
                if (k > 30 && c.world != null) {
                    var e = c.world.getEntityById(d.us[0].mob.getId());
                    AttackController ac = e == null ? null : AttackController.LIVE.get(e);
                    // Frame 0 is the instant the swing begins: its weight is 0 by design (the fade in), so it shows the pose it leaves.
                    // Its window is half a tick (weight there is at most .10) so a slow harness frame rate still lands in it.
                    if (ac != null && ac.shownT >= 0 && (ac.shownWeight > 0 || ac.shownT < .5)) {
                        int tt = (int) Math.round(ac.shownT);
                        // A late bow draw's quickened wind-up runs its clip faster than the clock, so a tick can step past a frame's .3 window.
                        if (Math.abs(ac.shownT - tt) <= (tt == 0 || ac.shownRate > 1.05 ? .5 : .3) && tt < FRAMES && (!far || tt % 2 == 0) && taken.add(ac.shownClip + ":" + tt))
                            Phase1Checks.shot(c, String.format("geo-" + TROOP + "-attack-%s-%s-%02d", view, ac.shownClip.replace("attack_", ""), tt));
                        // At rate r the clip moves r frames a tick, so the frames within r/2 of this sample play between it and its
                        // neighbours (every render frame in play); the harness samples once a tick and counts them as stepped past.
                        if (ac.shownRate > 1.05)
                            for (int f = (int) Math.ceil(ac.shownT - ac.shownRate / 2); f <= Math.floor(ac.shownT + ac.shownRate / 2); f++)
                                if (f > 0 && f < FRAMES && (!far || f % 2 == 0) && !taken.contains(ac.shownClip + ":" + f)) stepped.add(ac.shownClip + ":" + f);
                    }
                }
                stepped.removeAll(taken);
                int shotN = (int) taken.stream().filter(f -> !f.endsWith(":0")).count(), got = shotN + stepped.size();
                if (got >= want || k > 1200) {
                    Phase1Checks.report("p1-geo-" + TROOP + "-attack-frames-" + view, got >= want, got + "/" + want + " clip frames of the own knight (" + stepped.size() + " stepped past in a quickened wind-up; tick 0 optional: "
                        + (taken.size() - shotN) + "/3 caught)");
                    if (++i < ATTACK_VIEWS.length) next(tick, 5);
                    else {
                        syncReport(d);
                        Phase1Checks.report("p1-geo-" + TROOP + "-attack-plays", impactSamples > 10 && minImpactW > .9 && gapSamples > 4 && maxGapW == 0,
                            String.format("weight at impact (+-1 tick) min %.4f (%s) over %d samples; between swings max %.5f over %d", minImpactW, minImpactWhy, impactSamples, maxGapW, gapSamples));
                        Phase1Checks.report("p1-geo-" + TROOP + "-attack-footwork", impactSamples > 10 && minImpactLegsW > .9,
                            String.format("standing: attack legs weight at impact min %.4f over %d samples", minImpactLegsW, impactSamples));
                        duel = null; i = 0; next(tick, 6);
                    }
                }
            }
            case 6 -> {
                // Walking attack: the own knight presses forward at walk speed, the foe backs away, both swinging.
                if (k == 1) {
                    clientVariant.clear(); clientImpact.clear(); clientDrawn.clear(); clientKnown.clear(); minImpactW = 2; impactSamples = 0; walkSwingSamples = 0; maxWalkLegsW = 0; maxOnsetLegsW = 0; walkSettledSamples = 0; walkWrong.clear(); foeBackMin = 9; foeBack.clear(); lastShot = -1;
                    Phase1Checks.server(c, b -> { duel = new Duel(this, b, "walk"); eye(b, cx, cz + 5, 180, 8); });
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                Duel d = duel;
                if (d == null || !d.mode.equals("walk")) { if (k > 200) { Phase1Checks.report("p1-geo-" + TROOP + "-attack-walk", false, "walk duel never ran"); next(tick, 9); } return; }
                watch(c, d);
                int dk = d.k;
                if (dk != lastShot && dk >= WALK0 + 10 && dk < WALK0 + WALK - 4) { lastShot = dk; Phase1Checks.shot(c, String.format("geo-" + TROOP + "-attack-walk-%02d", dk - WALK0)); }
                if (dk >= WALK0 + WALK + 4 || k > 600) {
                    syncReport(d);
                    Phase1Checks.report("p1-geo-" + TROOP + "-attack-walk-gait-legs", walkSettledSamples > 10 && maxWalkLegsW == 0,
                        String.format("walking: attack legs weight max %.4f over %d settled swinging samples (gait keeps the legs); onset fade peak %.3f",
                            maxWalkLegsW, walkSettledSamples, maxOnsetLegsW));
                    Phase1Checks.report("p1-geo-" + TROOP + "-attack-walk", walkSwingSamples > 10 && walkWrong.isEmpty() && impactSamples > 2 && minImpactW > .9 && foeBackMin > 0 && median(foeBack) > .1,
                        String.format("%d swinging samples, gait while swinging %s; weight at impact min %.3f over %d; foe backing speed min %.3f median %.3f",
                            walkSwingSamples, walkWrong.isEmpty() ? "always WALK" : "wrong " + walkWrong, minImpactW, impactSamples, foeBackMin, median(foeBack)));
                    duel = null; next(tick, 7);
                }
            }
            case 7 -> {
                // Death mid-swing: the foe is killed 4 ticks into a swing; its arms must let the swing go and its legs stay still.
                if (k == 1) {
                    maxBeforeDeathW = 0; maxDeadW = 0; deadSamples = 0; deadWrong.clear(); lastShot = -1;
                    Phase1Checks.server(c, b -> { duel = new Duel(this, b, "death"); eye(b, cx, cz - 4.5, 0, 8); });
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                Duel d = duel;
                if (d == null || !d.mode.equals("death")) { if (k > 200) { Phase1Checks.report("p1-geo-" + TROOP + "-attack-death", false, "death duel never ran"); next(tick, 9); } return; }
                watch(c, d);
                long ks = d.killSwing;
                if (ks >= 0 && c.world != null) {
                    int t = (int) (c.world.getTime() - ks);
                    if (t != lastShot && t >= 0 && t <= 16) { lastShot = t; Phase1Checks.shot(c, String.format("geo-" + TROOP + "-attack-death-%02d", t)); }
                }
                if ((d.killedAt >= 0 && c.world != null && c.world.getTime() - d.killedAt > 20) || k > 600) {
                    Phase1Checks.report("p1-geo-" + TROOP + "-attack-death", d.killedAt >= 0 && d.killedDead && maxBeforeDeathW > 0 && deadSamples > 5 && maxDeadW == 0 && deadWrong.isEmpty(),
                        String.format("killed %s (dead %s); weight %d-%d ticks into the swing max %.3f; once faded (dead 3+ ticks) max %.5f over %d samples; gait %s",
                            d.killedAt >= 0 ? "at +" + KILL_AT : "never", d.killedDead, KILL_AT - 2, KILL_AT, maxBeforeDeathW, maxDeadW, deadSamples, deadWrong.isEmpty() ? "IDLE" : "wrong " + deadWrong));
                    duel = null; i = 0; next(tick, 8);
                }
            }
            case 8 -> {
                // Stances: every live knight held in each order stance in turn. On the close view the change is shot tick by tick as it
                // fades in; every knight must show the pose at weight over .5 within 4 ticks of the server's change, and fully by k30.
                int ci = i / STANCE_VIEWS.length, vi = i % STANCE_VIEWS.length;
                String[] sc = STANCE_CASES[ci]; String view = STANCE_VIEWS[vi];
                boolean shield = sc[2].equals("1"), ready = sc[0].equals("ready");
                String[] want = StanceController.target(sc[1], shield, true);
                if (k == 1) {
                    if (i == 0) stanceBad.clear();
                    if (vi == 0) stanceLag.clear();
                    lastShot = -1;
                    Phase1Checks.server(c, b -> hold(b, sc[1], shield, view(b, view, true)));
                }
                // Ready is the guard a fighting knight stands in: the change is the blow that starts the fight, once every knight stands
                // still on its mark (the duel had parked four of them 30 blocks off; the move back is not the stance's lag).
                // k14: k8 was not always enough for the farthest knight to walk back and stand still (a 5-tick "lag" that was the walk).
                if (ready && k == READY_HIT && vi == 0) changedAt = -1;
                if (ready && k == READY_HIT) Phase1Checks.server(c, b -> { hitAll(b, 0, 0, false); if (vi == 0) changedAt = hitAt; });
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                if (vi == 0 && k > (ready ? READY_HIT : 1) && k < 30 && c.world != null && changedAt >= 0) {
                    long t = c.world.getTime() - changedAt;
                    if (t >= 0 && t <= 6 && t != lastShot) { lastShot = (int) t; Phase1Checks.shot(c, String.format("geo-" + TROOP + "-stance-%s-in-%02d", sc[0], t)); }
                    for (int j = 0; j < knights.size(); j++) {
                        var e = c.world.getEntityById(knights.get(j).mob.getId());
                        StanceController st = e == null ? null : StanceController.LIVE.get(e);
                        if (st == null || stanceLag.containsKey(j) || !(e instanceof LivingEntity l) || !l.isAlive()) continue;
                        boolean up = st.shownUpper.equals(want[0]) && st.shownUpperWeight > .5;
                        boolean legs = want[1] == null || (st.shownLegs.equals(want[1]) && st.shownLegsWeight > .5);
                        if (up && legs) stanceLag.put(j, t);
                    }
                }
                if (k == 30) {
                    Phase1Checks.shot(c, "geo-" + TROOP + "-stance-" + sc[0] + "-" + view);
                    if (c.world != null) for (int j = 0; j < knights.size(); j++) {
                        var e = c.world.getEntityById(knights.get(j).mob.getId());
                        if (!(e instanceof LivingEntity l) || !l.isAlive()) continue;
                        StanceController st = StanceController.LIVE.get(e);
                        if (st == null) { stanceBad.add(sc[0] + ":" + j + " no stance layer"); continue; }
                        boolean up = want[0] == null ? st.shownUpperWeight == 0 : st.shownUpper.equals(want[0]) && st.shownUpperWeight > .99;
                        boolean legs = want[1] == null ? st.shownLegsWeight == 0 : st.shownLegs.equals(want[1]) && st.shownLegsWeight > .99;
                        if (!up || !legs) stanceBad.add(String.format("%s/%s:%d shows %s %.2f + %s %.2f (stance %s shield %s still %.2f)", sc[0], view, j,
                            st.shownUpper, st.shownUpperWeight, st.shownLegs, st.shownLegsWeight, st.shownStance, st.shownShield, st.shownStill));
                        if (vi == 0) { Long lag = stanceLag.get(j); if (lag == null || lag > 4) stanceBad.add(sc[0] + ":" + j + " lag " + lag); }
                    }
                    if (vi == 0) System.out.println("[p1] stance " + sc[0] + " lag per knight " + stanceLag);
                }
                if (k == 31) {
                    if (++i < STANCE_CASES.length * STANCE_VIEWS.length) next(tick, 8);
                    else {
                        Phase1Checks.report("p1-geo-" + TROOP + "-stance", stanceBad.isEmpty(), STANCE_CASES.length + " stances x " + STANCE_VIEWS.length
                            + " views, every live knight on its pose at weight 1 by k30 and over .5 within 4 ticks of the change" + (stanceBad.isEmpty() ? "" : "; bad " + stanceBad));
                        i = 0; next(tick, 10);
                    }
                }
            }
            case 10 -> {
                // Hits: every live knight, in no order stance, takes the same blow at k25; the tier III own knight's hit layer is shot
                // every tick of the reaction (far: even ticks) and checked: its clip, direction mix, peak scale, and gone after.
                int hi = i / HIT_VIEWS.length, vi = i % HIT_VIEWS.length;
                String name = HIT_CASES[hi], view = HIT_VIEWS[vi];
                boolean blocked = name.equals("block"), far = view.equals("far"), first = i == 0;
                int w = HIT_WEIGHT[hi];
                float bearing = HIT_BEARING[hi];
                String clip = blocked ? "hit_block" : w == 0 ? "hit_light" : "hit_medium";
                int len = blocked ? 11 : w == 0 ? 9 : 16;
                double scale = blocked ? HitController.BLOCK_SCALE[w] : w >= 2 ? HitController.HEAVY : 1;
                if (k == 1) {
                    if (first) hitBad.clear();
                    taken.clear(); hitMaxScale = 0; hitMaxLegs = 0; hitFrames = 0; hitAt = -1;
                    Phase1Checks.server(c, b -> {
                        hold(b, "", blocked, view(b, view, false));
                        if (first) hitAll(b, 0, 0, false); // starts the ready guard, so the first case is not also its fade in
                    });
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                if (k == 25) { hitAt = -1; Phase1Checks.server(c, b -> hitAll(b, bearing, w, blocked)); }
                if (k > 25 && hitAt >= 0 && c.world != null) {
                    var e = c.world.getEntityById(knights.get(2).mob.getId());
                    HitController hc = e == null ? null : HitController.LIVE.get(e);
                    if (hc != null && hc.shownT >= 0) {
                        int tt = (int) Math.round(hc.shownT);
                        if (Math.abs(hc.shownT - tt) <= .3 && (!far || tt % 2 == 0) && taken.add(name + ":" + tt)) {
                            Phase1Checks.shot(c, String.format("geo-" + TROOP + "-hit-%s-%s-%02d", view, name, tt));
                            hitFrames++;
                        }
                        if (!hc.shownClip.equals(clip) && hitBad.size() < 40) hitBad.add(name + "/" + view + " clip " + hc.shownClip + "@" + String.format("%.1f", hc.shownT));
                        hitMaxScale = Math.max(hitMaxScale, hc.shownScale);
                        hitMaxLegs = Math.max(hitMaxLegs, hc.shownLegsWeight);
                        if (!blocked) {
                            double[] m = new double[4]; HitController.mix(bearing, m);
                            for (int q = 0; q < 4; q++) if (Math.abs(m[q] - hc.shownMix[q]) > .01) { if (hitBad.size() < 40) hitBad.add(name + " mix " + Arrays.toString(hc.shownMix)); break; }
                        }
                    }
                }
                if (k == 25 + len + 12) {
                    var e = c.world == null ? null : c.world.getEntityById(knights.get(2).mob.getId());
                    HitController hc = e == null ? null : HitController.LIVE.get(e);
                    if (hc == null) hitBad.add(name + "/" + view + " no hit layer");
                    else if (!hc.shownClip.isEmpty()) hitBad.add(name + "/" + view + " still playing " + hc.shownClip + " at " + hc.shownT);
                    if (hitMaxScale < .95 * scale || hitMaxScale > 1.001 * scale) hitBad.add(String.format("%s/%s peak scale %.3f want %.3f", name, view, hitMaxScale, scale));
                    // the knights stand, so each reaction's footwork (skid, give, replant) plays at the blow's full scale
                    if (hitMaxLegs < .9 * scale || hitMaxLegs > 1.001 * scale) hitBad.add(String.format("%s/%s footwork %.3f want %.3f", name, view, hitMaxLegs, scale));
                    int need = far ? len / 4 : len / 2;
                    if (hitFrames < need) hitBad.add(name + "/" + view + " frames " + hitFrames + "/" + need);
                    System.out.println(String.format("[p1] hit %s/%s frames %d scale %.3f legs %.3f", name, view, hitFrames, hitMaxScale, hitMaxLegs));
                    if (++i < HIT_CASES.length * HIT_VIEWS.length) next(tick, 10);
                    else {
                        Phase1Checks.report("p1-geo-" + TROOP + "-hit", hitBad.isEmpty(), HIT_CASES.length + " blows x " + HIT_VIEWS.length
                            + " views: clip, bearing mix, peak scale, frames, and gone after the clip" + (hitBad.isEmpty() ? "" : "; bad " + hitBad.subList(0, Math.min(8, hitBad.size()))));
                        Phase1Checks.server(c, b -> hold = null);
                        i = 0; next(tick, 12);
                    }
                }
            }
            case 12 -> {
                // Pulled: the tier III own troop (slot 2) is dragged toward a live foe (foe()) by the server's pull path at k25, the
                // others stood out of the way; its hit layer is shot every tick of the reaction and checked: clip (front hit_pulled,
                // back hit_knockback, or the medium stagger when the troop has none), scale, footwork, gone after; the server must
                // have sent the pulled flag and moved it.
                String name = PULL_CASES[i][0], view = PULL_CASES[i][1];
                boolean back = name.equals("back");
                String want = back ? (KNOCKBACK_CLIP ? "hit_knockback" : "hit_medium") : "hit_pulled";
                if (k == 1) {
                    if (i == 0) pullBad.clear();
                    taken.clear(); pullMaxScale = 0; pullMaxLegs = 0; pullFrames = 0; pullMoved = Double.NaN; pullSent = -1; pullClips = ""; hitAt = -1;
                    Phase1Checks.server(c, b -> {
                        hold = null; quiet(b, true);
                        for (int j = 0; j < knights.size(); j++) if (j != 2 && j != foe()) stand(knights.get(j), cx + (j < 3 ? -20 - j * 3 : 20 + j * 3), cz - 30, 0);
                        var v = knights.get(2); var f = knights.get(foe());
                        float yaw = view.equals("side") ? 90 : 135;
                        // the puller: in front of the victim (side view along its facing, three-quarter 45 degrees off it) or right behind it
                        float at = back ? yaw + 180 : view.equals("side") ? yaw : yaw + 45;
                        double fx = cx - Math.sin(Math.toRadians(at)) * PULL_FROM, fz = cz + Math.cos(Math.toRadians(at)) * PULL_FROM;
                        stand(v, cx, cz, yaw);
                        stand(f, fx, fz, (float) Math.toDegrees(Math.atan2(-(cx - fx), cz - fz)));
                        if (view.equals("side")) eye(b, cx - 2, cz - 6.5, 0, 8); else eye(b, cx - 4, cz - 2.5, -58, 14);
                    });
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                if (k == 25) Phase1Checks.server(c, b -> {
                    var v = knights.get(2); var f = knights.get(foe());
                    if (!v.mob.isAlive() || !f.mob.isAlive()) { pullSent = -2; return; }
                    Vec3d from = f.mob.getPos();
                    double before = Math.hypot(v.mob.getX() - from.x, v.mob.getZ() - from.z);
                    int sends = MotionEvents.pulledSends;
                    hitAt = b.world.getTime();
                    v.mob.setInvulnerable(false);
                    try {
                        if (Abilities.pulls(b, v.mob, from, PULL)) Abilities.markPulled(b, v.mob);
                        Combat.resolve(b, f, v.mob, 1f, "slam", true);
                        Abilities.PULLED.remove(v.mob.getId());
                        Abilities.pull(b, f, v.mob, from, PULL);
                    } finally { v.mob.setInvulnerable(true); v.mob.setHealth(v.mob.getMaxHealth()); }
                    pullSent = MotionEvents.pulledSends - sends;
                    pullMoved = before - Math.hypot(v.mob.getX() - from.x, v.mob.getZ() - from.z);
                });
                if (k > 25 && hitAt >= 0 && c.world != null) {
                    var e = c.world.getEntityById(knights.get(2).mob.getId());
                    HitController hc = e == null ? null : HitController.LIVE.get(e);
                    if (hc != null && hc.shownT >= 0) {
                        int tt = (int) Math.round(hc.shownT);
                        if (Math.abs(hc.shownT - tt) <= .3 && taken.add(name + view + ":" + tt)) {
                            Phase1Checks.shot(c, String.format("geo-" + TROOP + "-pull-%s-%s-%02d", name, view, tt));
                            pullFrames++;
                        }
                        if (!pullClips.contains(hc.shownClip)) pullClips += hc.shownClip + " ";
                        if (!hc.shownClip.equals(want) && pullBad.size() < 40) pullBad.add(name + "/" + view + " clip " + hc.shownClip + "@" + String.format("%.1f", hc.shownT));
                        pullMaxScale = Math.max(pullMaxScale, hc.shownScale);
                        pullMaxLegs = Math.max(pullMaxLegs, hc.shownLegsWeight);
                    }
                }
                if (k == 25 + 40) {
                    var e = c.world == null ? null : c.world.getEntityById(knights.get(2).mob.getId());
                    HitController hc = e == null ? null : HitController.LIVE.get(e);
                    if (hc == null) pullBad.add(name + "/" + view + " no hit layer");
                    else if (!hc.shownClip.isEmpty()) pullBad.add(name + "/" + view + " still playing " + hc.shownClip + " at " + hc.shownT);
                    if (pullSent < 1) pullBad.add(name + "/" + view + " pulled hit_v1 sends " + pullSent);
                    if (!(pullMoved > PULL - .2)) pullBad.add(String.format("%s/%s moved %.2f toward the puller, want %.2f", name, view, pullMoved, PULL));
                    if (pullMaxScale < .95 || pullMaxScale > HitController.HEAVY * 1.001) pullBad.add(String.format("%s/%s peak scale %.3f", name, view, pullMaxScale));
                    if (pullMaxLegs < .5) pullBad.add(String.format("%s/%s footwork %.3f", name, view, pullMaxLegs));
                    if (pullFrames < 6) pullBad.add(name + "/" + view + " frames " + pullFrames);
                    System.out.println(String.format("[p1] pulled %s/%s clips %sframes %d scale %.3f legs %.3f moved %.2f sends %d", name, view, pullClips, pullFrames, pullMaxScale, pullMaxLegs, pullMoved, pullSent));
                    if (++i < PULL_CASES.length) next(tick, 12);
                    else {
                        Phase1Checks.report("p1-geo-" + TROOP + "-hit-pulled", pullBad.isEmpty(), PULL_CASES.length + " pulls by the server path (front: hit_pulled, back: " + (KNOCKBACK_CLIP ? "hit_knockback" : "hit_medium, no hit_knockback") + "): "
                            + "flag sent, body moved, clip, scale, footwork, frames, gone after" + (pullBad.isEmpty() ? "" : "; bad " + pullBad.subList(0, Math.min(8, pullBad.size()))));
                        Phase1Checks.server(c, b -> quiet(b, false));
                        riseRow = -1; i = 0; next(tick, 13);
                    }
                }
            }
            case 13 -> {
                // The rise: only a troop whose tier III has the rise row (the Hollow King). Per case, a lethal blow at k20 through
                // Combat.resolve with the roll forced; every client tick is shot; the death layer must show fake_death then rise at
                // weight 1, the rise ending on the span's last tick, nothing once it is over; the server must hold it down that long.
                // "death": a second lethal blow RISE_KILL ticks in kills it, and its death layer must sink from the rise into fake_death.
                String rc = RISE_CASES[i];
                boolean killCase = rc.equals("death");
                if (riseRow == 0) { next(tick, 11); return; }
                if (k > 200 && riseRow < 0) { Phase1Checks.report("p1-geo-" + TROOP + "-rise", false, "the server never answered"); next(tick, 11); return; }
                if (k == 1) {
                    if (i == 0) riseBad.clear();
                    taken.clear(); riseFrames = 0; risePhase = 0; riseMaxFakeT = -1; riseMaxT = -1; riseFirstRise = -1; riseMinW = 2;
                    riseTick = -2; riseWindow = -1; riseSent = -1; riseVictim = -1; riseStun = -1; riseDown = -1; riseKilledAt = -1; riseKilled = false;
                    riseDeathFrames = 0; riseDeathClips = "";
                    Phase1Checks.server(c, b -> {
                        var v = knights.get(2);
                        if (Abilities.riseRow(v) == null) { riseRow = 0; return; }
                        if (!v.mob.isAlive()) { riseRow = 0; return; }
                        hold = null; quiet(b, true);
                        for (int j = 0; j < knights.size(); j++) if (j != 2 && j != foe()) stand(knights.get(j), cx + (j < 3 ? -20 - j * 3 : 20 + j * 3), cz - 30, 0);
                        v.rose = false; v.mob.setHealth(v.mob.getMaxHealth()); v.stunnedUntil = 0; v.downUntil = 0; // re-armed: the rise is once a battle
                        boolean side = rc.equals("side");
                        stand(v, cx, cz, side ? 90 : 135);
                        stand(knights.get(foe()), side ? cx + 4 : cx + 3, side ? cz + 3 : cz + 3, side ? 90 : -45); // the striker, out of the frame
                        riseVictim = v.mob.getId();
                        if (side) eye(b, cx - 2, cz - 7, 0, 6); else eye(b, cx - 4, cz - 2.5, -58, 14);
                        riseRow = 1;
                    });
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                if (k == 20 && riseRow == 1) Phase1Checks.server(c, b -> {
                    var v = knights.get(2); var f = knights.get(foe());
                    int sends = MotionEvents.riseSends;
                    v.mob.setInvulnerable(false); Houses.forceRise = Boolean.TRUE;
                    try { Combat.resolve(b, f, v.mob, v.mob.getMaxHealth() * 50, "slam", true); }
                    finally { Houses.forceRise = null; v.mob.setInvulnerable(true); }
                    riseRose = v.rose; riseAlive = v.mob.isAlive(); riseSent = MotionEvents.riseSends - sends; riseTick = v.riseTick; riseWindow = v.riseWindow;
                    riseStun = v.stunnedUntil - b.time; riseDown = v.downUntil - b.time;
                });
                if (killCase && k == 20 + RISE_KILL && riseRow == 1) Phase1Checks.server(c, b -> {
                    var v = knights.get(2); var f = knights.get(foe());
                    v.mob.setInvulnerable(false); v.mob.timeUntilRegen = 0;
                    try { Combat.resolve(b, f, v.mob, v.mob.getMaxHealth() * 50, "slam", true); }
                    finally { if (v.mob.isAlive()) v.mob.setInvulnerable(true); }
                    riseKilled = !v.mob.isAlive(); riseKilledAt = b.world.getTime();
                });
                double span = riseWindow > 0 ? riseWindow : FAKE_T + RISE_T;
                if (k >= 18 && k <= 20 + span + 4 && c.world != null && riseRow == 1) { Phase1Checks.shot(c, String.format("geo-" + TROOP + "-rise-%s-%03d", rc, k - 18)); riseFrames++; }
                if (k > 20 && riseTick >= 0 && c.world != null && c.world.getEntityById(riseVictim) instanceof LivingEntity e) {
                    DeathController dc = DeathController.LIVE.get(e);
                    MotionEvents.Rise r = MotionEvents.Client.rise(riseVictim);
                    if (r != null && (r.tick() != riseTick || r.window() != riseWindow) && riseBad.size() < 40) riseBad.add(rc + " client heard " + r + " server tick " + riseTick + " window " + riseWindow);
                    if (dc == null) { if (riseBad.size() < 40) riseBad.add(rc + " no death layer"); }
                    else if (e.isDead() || e.deathTime > 0) {
                        // killed mid-rise: from its first dead frame the layer sinks into fake_death, never a fall from the guard
                        if (!riseDeathClips.contains(dc.shownClip + " ")) riseDeathClips += dc.shownClip + " ";
                        if (!dc.shownClip.equals(DeathController.FAKE) && riseBad.size() < 40) riseBad.add(rc + " dead mid-rise shows " + dc.shownClip + " at death " + e.deathTime);
                        riseDeathFrames++;
                    } else if (dc.shownRiseElapsed >= 0) {
                        double el = dc.shownRiseElapsed;
                        if (Math.abs(dc.shownRiseSpan - span) > .01 && riseBad.size() < 40) riseBad.add(rc + " span " + dc.shownRiseSpan + " want " + span);
                        double[] want = DeathController.riseAt(el, riseWindow, FAKE_T, RISE_T);
                        String wc = want == null ? "" : want[0] == 0 ? DeathController.FAKE : DeathController.RISE;
                        if (!dc.shownClip.equals(wc) && riseBad.size() < 40) riseBad.add(String.format("%s clip %s want %s at %.1f", rc, dc.shownClip, wc, el));
                        if (dc.shownClip.equals(DeathController.FAKE)) { if (risePhase > 0 && riseBad.size() < 40) riseBad.add(rc + " fake_death after rise at " + el); riseMaxFakeT = Math.max(riseMaxFakeT, dc.shownT); }
                        if (dc.shownClip.equals(DeathController.RISE)) { if (risePhase == 0) { risePhase = 1; riseFirstRise = el; } riseMaxT = Math.max(riseMaxT, dc.shownT); }
                        if (el >= DeathController.FADE_IN + .5 && el <= span - DeathController.RISE_OUT - .5) riseMinW = Math.min(riseMinW, dc.shownWeight);
                    } else if (e.isAlive() && !dc.shownClip.isEmpty() && riseBad.size() < 40) riseBad.add(rc + " death layer " + dc.shownClip + " outside the rise");
                }
                if (k == 20 + (int) span + 12) {
                    var e = c.world == null ? null : c.world.getEntityById(riseVictim);
                    DeathController dc = e == null ? null : DeathController.LIVE.get(e);
                    if (riseSent < 1) riseBad.add(rc + " rise_v1 sends " + riseSent);
                    if (!riseRose || !riseAlive) riseBad.add(rc + " server rose " + riseRose + " alive " + riseAlive);
                    if (riseStun < span || riseDown < span || riseWindow != (int) riseStun) riseBad.add(rc + " server down " + riseDown + " stunned " + riseStun + " window " + riseWindow + ", want the span " + span);
                    if (killCase) {
                        if (!riseKilled) riseBad.add(rc + " the second blow did not kill it");
                        if (riseDeathFrames < 10) riseBad.add(rc + " dead frames " + riseDeathFrames);
                        if (riseFirstRise < 0) riseBad.add(rc + " never reached the rise before the kill");
                    } else {
                        if (dc == null || !dc.shownClip.isEmpty()) riseBad.add(rc + " after the span the death layer shows " + (dc == null ? "none" : dc.shownClip));
                        if (!(e instanceof LivingEntity l) || !l.isAlive() || l.deathTime > 0) riseBad.add(rc + " not standing after the rise");
                        if (riseMaxT < RISE_T - 1.5) riseBad.add(String.format("%s rise reached %.2f, its end must meet the span's", rc, riseMaxT));
                    }
                    if (riseMaxFakeT < FAKE_T - 1.5) riseBad.add(String.format("%s fake_death reached %.2f", rc, riseMaxFakeT));
                    if (Math.abs(riseFirstRise - (span - RISE_T)) > 1.5) riseBad.add(String.format("%s rise began at %.2f, want %.2f", rc, riseFirstRise, span - RISE_T));
                    if (riseMinW < .999) riseBad.add(String.format("%s weight dipped to %.3f inside the span", rc, riseMinW));
                    if (riseFrames < span * .6) riseBad.add(rc + " frames " + riseFrames + "/" + (int) span);
                    System.out.println(String.format("[p1] rise %s window %d stun %d down %d span %.0f frames %d fake max %.2f rise from %.2f max %.2f min w %.3f sends %d%s", rc, riseWindow, riseStun, riseDown,
                        span, riseFrames, riseMaxFakeT, riseFirstRise, riseMaxT, riseMinW, riseSent, killCase ? " killed " + riseKilled + " dead frames " + riseDeathFrames + " clips " + riseDeathClips : ""));
                    Phase1Checks.server(c, b -> quiet(b, false));
                    if (++i < RISE_CASES.length) next(tick, 13);
                    else {
                        Phase1Checks.report("p1-geo-" + TROOP + "-rise", riseBad.isEmpty(), "lethal blow through Combat.resolve, rise forced, three-quarter and side: server rose, held it down "
                            + (int) span + " ticks and sent rise_v1, client fake_death then rise over the span, weight 1, the rise ending on it, gone after; killed "
                            + RISE_KILL + " ticks in: fake_death from the first dead frame" + (riseBad.isEmpty() ? "" : "; bad " + riseBad.subList(0, Math.min(8, riseBad.size()))));
                        i = 0; next(tick, 11);
                    }
                }
            }
            case 11 -> {
                // Death falls: a live knight (tier III own first) stands three-quarter on to the camera and takes a lethal blow from
                // each side in turn at k20. Its death layer must play the side's fall, fully in by deathTime 2, on deathTime, holding
                // the last frame while the body lies; every tick of the fall and two of the lie are shot.
                String name = DEATH_CASES[i], want = DeathController.clipFor(DEATH_BEARING[i]);
                if (k == 1) {
                    taken.clear(); deathFrames = 0; deathMaxT = -1; deathShown = ""; deathVictim = -1; deathPrevT = -1;
                    Phase1Checks.server(c, b -> {
                        hold = null;
                        GravesAndGlory.Unit v = null;
                        for (int j : new int[] {2, 1, 0, 5, 4, 3}) if (knights.get(j).mob.isAlive()) { v = knights.get(j); break; }
                        if (v == null) return;
                        stand(v, cx, cz, 135);
                        deathVictim = v.mob.getId();
                        eye(b, cx - 4, cz - 2.5, -58, 14);
                    });
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                if (k == 20) Phase1Checks.server(c, b -> {
                    for (var u : knights) if (u.mob.getId() == deathVictim && u.mob.isAlive()) {
                        u.mob.setInvulnerable(false); u.mob.kill();
                        MotionEvents.sendHit(u.mob, "slash", "body", DEATH_BEARING[i], 1, false, true);
                    }
                });
                if (k > 20 && c.world != null && deathVictim >= 0
                        && c.world.getEntityById(deathVictim) instanceof net.minecraft.entity.LivingEntity e) {
                    DeathController dc = DeathController.LIVE.get(e);
                    if (dc == null) { if (deathBad.size() < 40) deathBad.add(name + " no death layer"); }
                    else if (e.deathTime > 0) {
                        String dropped = want + DeathController.drop(e.getUuid());
                        if (!dc.shownClip.equals(want) && !dc.shownClip.equals(dropped) && deathBad.size() < 40) deathBad.add(name + " clip " + dc.shownClip + " want " + dropped);
                        deathShown = dc.shownClip;
                        if (e.deathTime >= 3 && dc.shownWeight < .999 && deathBad.size() < 40) deathBad.add(String.format("%s weight %.3f at death %d", name, dc.shownWeight, e.deathTime));
                        if (dc.shownT + .01 < deathPrevT && deathBad.size() < 40) deathBad.add(String.format("%s clip time went back %.2f->%.2f", name, deathPrevT, dc.shownT));
                        deathPrevT = dc.shownT; deathMaxT = Math.max(deathMaxT, dc.shownT);
                        int tt = e.deathTime;
                        if ((tt <= BattleMath.DEATH_FALL_TICKS || tt == 30 || tt == 45) && taken.add(name + ":" + tt)) {
                            Phase1Checks.shot(c, String.format("geo-" + TROOP + "-death-%s-%02d", name, tt));
                            deathFrames++;
                        }
                    } else if (!dc.shownClip.isEmpty() && deathBad.size() < 40) deathBad.add(name + " death layer playing while alive");
                }
                if (k == 130) {
                    if (deathVictim < 0) deathBad.add(name + " no live knight");
                    if (deathMaxT < BattleMath.DEATH_FALL_TICKS - .01) deathBad.add(String.format("%s clip time only reached %.2f", name, deathMaxT));
                    if (deathFrames < 18) deathBad.add(name + " frames " + deathFrames);
                    System.out.println(String.format("[p1] death %s frames %d max t %.2f clip %s", name, deathFrames, deathMaxT, deathShown));
                    if (++i < DEATH_CASES.length) next(tick, 11);
                    else {
                        Phase1Checks.report("p1-geo-" + TROOP + "-death", deathBad.isEmpty(), DEATH_CASES.length + " lethal blows: fall clip by bearing, "
                            + "weight, clip time held, frames" + (deathBad.isEmpty() ? "" : "; bad " + deathBad.subList(0, Math.min(8, deathBad.size()))));
                        next(tick, 9);
                    }
                }
            }
            case 30 -> {
                int ci = i / CAP_YAW.length, vi = i % CAP_YAW.length;
                if (ci >= capCases().length) {
                    boolean react = "react".equals(CAPTURE) || "charge".equals(CAPTURE) && System.getenv("GRAVES_GEO_CHARGE_VIDEO") != null;
                    Phase1Checks.server(c, b -> { hold = null; fixS = Double.NaN; quiet(b, react); });
                    i = 0; next(tick, react ? 31 : "only".equals(CAPTURE) || "1".equals(CAPTURE) || "charge".equals(CAPTURE) ? 9 : 2); return;
                }
                Object[] cc = capCases()[ci];
                String kind = cc.length > 6 ? (String) cc[6] : "";
                boolean line = (Boolean) cc[5];
                if (line && vi > 0) { i++; next(tick, 30); return; }
                int[] shots = (int[]) cc[4];
                float yaw = CAP_YAW[vi];
                if (k == 1) {
                    lastShot = -1;
                    // a different quiet stance first so the next view restarts the stance clip from tick 0 (the same stance twice would not; no stance at all lets the AI throw or aim)
                    Phase1Checks.server(c, b -> {
                        quiet(b, true); hold(b, "hold".equals(cc[1]) ? "ranged_rest" : "hold", false, yaw, 0f, 0.0);
                        fixS = Double.NaN;
                        if (line) camera(b, 0, 8, 8); else camera(b, -3, "fallback".equals(cc[0]) || "charge".equals(CAPTURE) ? 5.0 : 3.5, 3);
                    });
                }
                // the reaction kinds: when the shots start (k) and what they count from; the rest hold the stance at k == 14 and count from its change
                boolean knockKind = kind.equals("knock") || kind.equals("hitmed");
                int t0 = kind.equals("stunexit") ? 54 : knockKind ? 40 : 14;
                if (k == 14) Phase1Checks.server(c, b -> {
                    quiet(b, true);
                    hold(b, kind.equals("stunexit") ? "stunned" : (String) cc[1], false, yaw, (Float) cc[2], (Double) cc[3]);
                    if (kind.equals("stunexit")) hitAll(b, 0, 1, false, true); // a bash: the knockback and the stun together
                    if (knockKind) hitAll(b, 0, 0, false); // a light blow first: the ready guard
                });
                if (k == 54 && kind.equals("stunexit")) Phase1Checks.server(c, b -> hold(b, "", false, yaw, 0f, 0.0));
                if (k == 40 && knockKind) Phase1Checks.server(c, b -> hitAll(b, 0, 1, false, kind.equals("knock")));
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                if (k >= t0 && c.world != null && (knockKind ? hitAt >= 0 : changedAt >= 0)) {
                    long t = c.world.getTime() - (knockKind ? hitAt : changedAt);
                    for (int s : shots) if (t == s && s != lastShot) { lastShot = s; Phase1Checks.shot(c, String.format("geo-" + TROOP + "-cap-%s-%s-%02d", cc[0], CAP_ANG[vi], s)); }
                }
                if (k > t0 + shots[shots.length - 1] + 12) { i++; next(tick, 30); }
            }
            case 32 -> {
                // Real-gameplay gap captures (GeoGaps): a real shield bash, or a real Charge order.
                if (gaps == null) gaps = new GeoGaps(this);
                if (gaps.step(c, k)) { GeoGaps.live = null; next(tick, 9); }
            }
            case 31 -> {
                List<Object[]> segs = videoSegments();
                if (i >= segs.size()) { Phase1Checks.server(c, b -> { hold = null; quiet(b, false); }); i = 0; next(tick, 9); return; }
                Object[] sg = segs.get(i);
                String label = (String) sg[0], stance = (String) sg[1];
                float yaw = (Float) sg[2];
                boolean line = (Boolean) sg[3], sweep = (Boolean) sg[5];
                int frames = (Integer) sg[4];
                double walk = (Double) sg[6];
                boolean knockstun = sg.length > 7 && "knockstun".equals(sg[7]);
                boolean chargeShoot = sg.length > 7 && "chargeshoot".equals(sg[7]);
                boolean stunfix = sg.length > 7 && "stunfix".equals(sg[7]);
                if (k == 1) {
                    videoLine = line;
                    Phase1Checks.server(c, b -> {
                        fixS = Double.NaN; shootAt = -1;
                        quiet(b, true); hold(b, "hold".equals(stance) ? "ranged_rest" : "hold", false, yaw, 0f, 0.0);
                        if (line) camera(b, 0, chargeShoot ? 7.5 : 9, chargeShoot ? 5 : 6); else camera(b, -3, walk != 0 || "stunfix".equals(CAPTURE) ? 5.0 : 3.5, 3);
                    });
                }
                if (chargeShoot) {
                    // the squad advances in the charge stance, then (as Tactics does with an enemy in reach) stops in ranged_aim and the first three shoot at the other three
                    if (k == 14) Phase1Checks.server(c, b -> { quiet(b, true); hold(b, "charge", false, yaw, 0f, walk); });
                    if (k == 50) Phase1Checks.server(c, b -> {
                        fixS = -SPEEDS[1] * Math.max(0, b.world.getTime() - changedAt);
                        hold(b, "ranged_aim", false, yaw, 0f, 0.0);
                    });
                    if (k == 60) Phase1Checks.server(c, b -> {
                        for (int j = 0; j < 3; j++) Projectiles.drawAhead(b, knights.get(j), knights.get(j + 3).mob, 10);
                        shootAt = b.world.getTime() + 10 + Projectiles.looseTicks(knights.get(0));
                    });
                    if (k > 60 && shootAt >= 0) Phase1Checks.server(c, b -> {
                        if (shootAt < 0 || b.world.getTime() < shootAt) return;
                        shootAt = -1;
                        for (int j = 0; j < 3; j++) {
                            var u = knights.get(j); LivingEntity foe = knights.get(j + 3).mob;
                            u.nextAttack = b.time + 1000; u.mob.swingHand(Hand.MAIN_HAND);
                            if (CAST) Projectiles.fire(b, u, foe, "mend", 0f); else { if (SPELL) u.abilityAt = 0; Tactics.shoot(b, u, foe, b.time, true); }
                        }
                    });
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                if (stunfix) {
                    // Capture only (GRAVES_GEO_CAPTURE=stunfix): a bash as the server plays it. The knockback hit and the stun land on the same tick (k = 34) and the body slides
                    // backwards (first 0.9 blocks/tick, x0.546 a tick: about 1.9 blocks over 6 ticks); the stun lasts 40 ticks, then ends. The camera tracks the slide.
                    if (k == 14) Phase1Checks.server(c, b -> { quiet(b, true); fixS = 0; hold(b, "", false, yaw, 0f, 0.0); });
                    if (k > 14) Phase1Checks.server(c, b -> camera(b, -3, 5.0, 3)); // the camera pinned every tick (a slide or a hit must not push it)
                    if (k == 34) Phase1Checks.server(c, b -> { hitAll(b, 0, 1, false, true); hold(b, "stunned", false, yaw, 0f, 0.0); });
                    if (k >= 34 && k < 50) {
                        double sl = 0, v = 0.9;
                        for (int q = 0; q <= k - 34; q++) { sl += v; v *= 0.546; }
                        final double slide = sl;
                        Phase1Checks.server(c, b -> fixS = slide);
                    }
                    if (k == 74) Phase1Checks.server(c, b -> hold(b, "", false, yaw, 0f, 0.0));
                    if (k >= 15 && k < 15 + frames && c.world != null) {
                        var e = c.world.getEntityById(knights.get(0).mob.getId());
                        Gait g = e == null ? null : Gait.LIVE.get(e);
                        StanceController st = e == null ? null : StanceController.LIVE.get(e);
                        if (st != null && g != null) System.out.println(String.format("[sf] %s f=%03d gait=%s sp=%.3f still=%.2f up=%s@%.2f legs=%s@%.2f", label, k - 15, g.mode, g.speed, st.shownStill, st.shownUpper, st.shownUpperWeight, st.shownLegs, st.shownLegsWeight));
                    }
                } else if (knockstun) {
                    // a light blow (the ready guard), a knockback, then a 2 s stun (40 ticks), then the stun ends
                    if (k == 14) Phase1Checks.server(c, b -> { quiet(b, true); hold(b, "", false, yaw, 0f, 0.0); hitAll(b, 0, 0, false); });
                    if (k == 30) Phase1Checks.server(c, b -> hitAll(b, 0, 1, false, true));
                    if (k == 50) Phase1Checks.server(c, b -> hold(b, "stunned", false, yaw, 0f, 0.0));
                    if (k == 90) Phase1Checks.server(c, b -> hold(b, "", false, yaw, 0f, 0.0));
                } else if (chargeShoot) {
                    // handled above
                } else if (k == 14 && !sweep) Phase1Checks.server(c, b -> { quiet(b, true); hold(b, stance, false, yaw, 0f, walk); });
                if (k >= 14 && sweep) { float pitch = sweepPitch(Math.max(0, k - 15)); Phase1Checks.server(c, b -> hold(b, stance, false, yaw, pitch, 0.0)); }
                if (k >= 15 && k < 15 + frames) Phase1Checks.shot(c, String.format("geo-" + TROOP + "-vid-%02d-%03d", i, k - 15));
                if (k == 15) System.out.println("[p1] video segment " + i + " " + label);
                if (k >= 15 + frames + 2) { i++; fixS = Double.NaN; next(tick, 31); }
            }
            case 33 -> {
                // Capture only: slash swings (not the thrust) from several views, one shot per client tick as the swing plays.
                Object[] sv = SLASH_VIEWS[i];
                String view = (String) sv[0];
                if (k == 1) {
                    taken.clear();
                    if (i == 0) Phase1Checks.server(c, b -> duel = new Duel(this, b, "stand"));
                    Phase1Checks.server(c, b -> slashEye(b, sv));
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                Duel d = duel;
                if (d == null || !d.mode.equals("stand")) { if (k > 200) next(tick, 9); return; }
                if (k > 30 && c.world != null) {
                    var e = c.world.getEntityById(d.us[0].mob.getId());
                    AttackController ac = e == null ? null : AttackController.LIVE.get(e);
                    if (ac != null && ac.shownT >= 0 && isSlash(ac.shownClip) && (ac.shownWeight > 0 || ac.shownT < .5)) {
                        int tt = (int) Math.round(ac.shownT);
                        if (Math.abs(ac.shownT - tt) <= .3 && tt < FRAMES && taken.add(ac.shownClip + ":" + tt))
                            Phase1Checks.shot(c, String.format("slash-%s-%s-%02d", view, ac.shownClip.replace("attack_", ""), tt));
                    }
                }
                // every slash clip (at most 2: the sword troops' overhead and low_cut, the axes' chop and hew, the spear's low_sweep) shot through
                long got = taken.stream().collect(java.util.stream.Collectors.groupingBy(f -> f.substring(0, f.indexOf(':')), java.util.stream.Collectors.counting()))
                        .values().stream().filter(n -> n >= FRAMES - 1).count() >= SLASH_N ? 1 : 0;
                if (got == 1 || k > 1500) {
                    System.out.println("[p1] slash view " + view + " frames " + taken.size() + " k " + k);
                    if (++i < SLASH_VIEWS.length) next(tick, 33); else { duel = null; i = 0; next(tick, 9); }
                }
            }
            case 40 -> {
                if (i >= CAP_YAW.length) {
                    Phase1Checks.server(c, b -> { hold = null; quiet(b, false); });
                    Phase1Checks.report("p1-geo-" + TROOP + "-ability-sync", ablBad.isEmpty() && ablChecked == CAP_YAW.length,
                        ABILITY_CLIP + ": " + ablChecked + " casts" + (ablBad.isEmpty() ? ("howl".equals(ABILITY_CLIP) ? ", clip starts within 3 ticks of the howl" : ", release on the server tick") : ": " + String.join("; ", ablBad)));
                    i = 0; next(tick, "abilitywith".equals(CAPTURE) ? 2 : 9); return;
                }
                float yaw = CAP_YAW[i];
                if (k == 1) {
                    ablServer = Long.MIN_VALUE; ablSubject = -1; ablCross = Double.NaN; ablPrevT = -1; ablClipStart = Double.NaN; lastShot = Integer.MIN_VALUE;
                    Phase1Checks.server(c, b -> { quiet(b, true); hold(b, "", false, yaw, 0f, 0.0); camera(b, -3, 3.5, 3); unhowl(); });
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                if (k == 40) { Phase1Checks.server(c, b -> castAbility(b, 0)); ablClientCast = c.world == null ? Double.NaN : c.world.getTime(); }
                if (k > 40 && c.world != null && ablSubject >= 0) {
                    var e = c.world.getEntityById(ablSubject);
                    AttackController ac = e == null ? null : AttackController.LIVE.get(e);
                    if (ac != null && "howl".equals(ABILITY_CLIP) && ac.howlShown >= ablServer - 5) ablClipStart = ac.howlShown;
                    else if (ac != null && ABILITY_CLIP.equals(ac.shownClip) && ac.shownStart >= ablServer - 60) {
                        ablClipStart = ac.shownStart + ac.shownLead;
                        if (ablPrevT >= 0 && ablPrevT < ac.shownImpact && ac.shownT >= ac.shownImpact && Double.isNaN(ablCross))
                            ablCross = ac.shownAt - (ac.shownT - ac.shownImpact) / ac.shownRate;
                        ablPrevT = ac.shownT;
                    }
                    if (!Double.isNaN(ablClipStart)) {
                        int t = (int) (c.world.getTime() - Math.round(ablClipStart));
                        if (t != lastShot && t >= -2 && t <= 32) { lastShot = t; Phase1Checks.shot(c, String.format("geo-" + TROOP + "-abl-%s-%02d", CAP_ANG[i], t + 10)); }
                    }
                }
                if (k == 120) {
                    ablChecked++;
                    String v = CAP_ANG[i];
                    if ("howl".equals(ABILITY_CLIP)) {
                        double d = ablClipStart - ablClientCast;
                        if (Double.isNaN(d) || d < 0 || d > 3) ablBad.add(v + " howl started " + d + " client ticks after the cast was queued");
                        else System.out.println("[geo-ability] " + v + " howl clip start - cast (client clock) = " + d);
                    } else {
                        double d = ablCross - ablServer;
                        if (Double.isNaN(d) || Math.abs(d) > 0.6) ablBad.add(v + " release " + d + " ticks off the server tick");
                        else System.out.println("[geo-ability] " + v + " release frame - server tick = " + String.format("%.3f", d));
                    }
                    i++; next(tick, 40);
                }
            }
            case 9 -> {
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
                // every Geo draw of the run began with the bones left off rest (GeoLayerRest): none may have been built on that
                if (k == 1) {
                    GeoLayerRest.stop(); Phase1Checks.report("p1-geo-" + TROOP + "-layer-rest", GeoLayerRest.clean(), GeoLayerRest.result());
                    // and with another draw's legs left on the leg bones: a troop that set off must have matched its stride to its own
                    Phase1Checks.report("p1-geo-" + TROOP + "-start-own-legs", GeoLayerRest.ownLegs(3), GeoLayerRest.startResult());
                }
                if (k > 40) { Phase1Checks.finish(c, "geo-knight"); stage = 99; }
            }
            default -> {}
        }
    }
}
