package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test-only reach into the package-private battle state for cmdui.OrderBehaviourSuite (command UI plan Task 5): empty rounds, synthetic enemy
 * squads, real player orders through OrderRules.playerOrder and the per-troop numbers the behaviour checks read. Never included in releases
 * (its name carries the OrderIntakeProbe guard token).
 */
public final class OrderIntakeProbeBehaviour {
    private OrderIntakeProbeBehaviour() {}

    static boolean saved; static boolean fill, practice; static int size, lastWinner;

    /** A round with only this player's army (DuelSimulation.startEmptyRound): no bots, a clock that never runs out, the intro over. */
    public static void emptyRound(GravesAndGlory.Battle b, UUID id) {
        var c = b.campaign;
        if (!saved) { saved = true; fill = c.fill; practice = c.practice; size = c.size; lastWinner = c.lastWinner; }
        c.fill = false; c.practice = false; c.size = 1; c.lastWinner = -2;
        b.squads.get(id).seated = true;
        b.startRound();
        b.roundSeconds = Integer.MAX_VALUE / 40;
        c.introUntil = b.time;
    }
    /** Puts the campaign's seat settings back as emptyRound found them. */
    public static void restore(GravesAndGlory.Battle b) {
        if (!saved) return;
        var c = b.campaign; c.fill = fill; c.practice = practice; c.size = size; c.lastWinner = lastWinner; saved = false;
    }
    /** Nothing scores, so the round never ends by points; false when the round is not running. */
    public static boolean keep(GravesAndGlory.Battle b) { b.scores[0] = 0; b.scores[1] = 0; return b.running; }

    /** A player order through the real server entry, with the command cooldown and the Charge cooldown cleared first. */
    public static void order(GravesAndGlory.Battle b, UUID id, String group, String action, String formation, double x, double z, float yaw) {
        var s = b.squads.get(id); s.lastOrder = Long.MIN_VALUE / 4; s.chargeReadyAt = 0;
        OrderRules.playerOrder(b, b.server.getPlayerManager().getPlayer(id), group, action, formation, x, z, yaw);
    }

    /** A synthetic squad outside Battle.squads (CommandPlaytest's enemy pattern): its troops tick, nothing seats or reinforces it. */
    public static Object squad(int team, boolean bot, Vec3d at, String order) {
        var s = new GravesAndGlory.Squad(UUID.randomUUID(), team, at); s.bot = bot; s.order = order; s.target = at; return s;
    }
    /** n troops of the id in a column across z, 1.5 apart, centred on at, on the ground. */
    public static List<GravesAndGlory.Unit> spawn(GravesAndGlory.Battle b, Object squad, String troop, Vec3d at, int n) {
        var s = (GravesAndGlory.Squad) squad; var out = new ArrayList<GravesAndGlory.Unit>();
        for (int i = 0; i < n; i++) {
            double x = at.x, z = at.z + (i - (n - 1) / 2.0) * 1.5;
            out.add(b.spawn(s, Campaign.troop(troop), i, new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z)));
        }
        return out;
    }
    /** Troops that never move and never attack: AI off (no navigation, no move control), stunned for good. They still take blows. */
    public static void inert(List<GravesAndGlory.Unit> units) { for (var u : units) { u.mob.setAiDisabled(true); u.stunnedUntil = Long.MAX_VALUE / 4; } }
    /** A bot's order as Campaign.botDecide gives it: Battle.issue, never OrderRules (no hunt). */
    public static void issue(GravesAndGlory.Battle b, Object squad, String group, String action, Vec3d dest, float yaw) { b.issue((GravesAndGlory.Squad) squad, group, action, "line", dest, yaw, -1); }

    public static List<GravesAndGlory.Unit> members(GravesAndGlory.Battle b, UUID id, String group) { return Morale.members(b, b.squads.get(id), group); }
    public static List<GravesAndGlory.Unit> living(List<GravesAndGlory.Unit> units) { var out = new ArrayList<GravesAndGlory.Unit>(); for (var u : units) if (u.mob.isAlive() && !u.mob.isRemoved()) out.add(u); return out; }
    public static Vec3d centre(List<GravesAndGlory.Unit> units) { Vec3d sum = Vec3d.ZERO; int n = 0; for (var u : units) if (u.mob.isAlive()) { sum = sum.add(u.mob.getPos()); n++; } return n == 0 ? null : sum.multiply(1.0 / n); }
    public static MobEntity mob(GravesAndGlory.Unit u) { return u.mob; }
    public static long swungAt(GravesAndGlory.Unit u) { return u.swungAt; }
    public static long nextAttack(GravesAndGlory.Unit u) { return u.nextAttack; }
    public static long looseAt(GravesAndGlory.Unit u) { return u.looseAt; }
    public static LivingEntity target(GravesAndGlory.Unit u) { return u.lockedTarget; }
    public static boolean shieldUp(GravesAndGlory.Unit u) { return u.tags.contains("shield") && u.mob.isUsingItem(); }
    public static boolean walking(GravesAndGlory.Unit u) { return !u.mob.getNavigation().isIdle() || u.mob.getMoveControl().isMoving(); }
    /** The movement speed MoveControl set this tick over the troop's movement speed attribute: the walk's speed multiplier (an order's moveMultiplier). */
    public static double speedShare(GravesAndGlory.Unit u) { double attr = u.mob.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED); return attr <= 0 ? 0 : u.mob.getMovementSpeed() / attr; }
    /** The movement speed MoveControl set this tick over the troop's BASE movement speed: the walk's multiplier with every speed modifier on it (the shield crawl included). */
    public static double baseShare(GravesAndGlory.Unit u) { var i = u.mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED); double base = i == null ? 0 : i.getBaseValue(); return base <= 0 ? 0 : u.mob.getMovementSpeed() / base; }
    /** Whether the shield crawl's speed modifier (Tactics.crawl) is on the troop. */
    public static boolean crawling(GravesAndGlory.Unit u) { var i = u.mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED); return i != null && i.getModifier(Tactics.CRAWL_ID) != null; }
    /** The troop's weapon reach (Abilities.range). */
    public static double reach(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return Abilities.range(b, u); }
    /** The troop's formation slot (Battle.formation). */
    public static Vec3d slot(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return b.formation(u); }
    public static long chargedAt(GravesAndGlory.Battle b, UUID id, String group) { return b.squads.get(id).chargedAt.getOrDefault(group, -1L); }
    /** The group under archer fire now (Squad.shotAt), as an enemy archer's shot would mark it. */
    public static void fire(GravesAndGlory.Battle b, UUID id, String group) { b.squads.get(id).shotAt.put(group, b.time); }
    public static int strikesLanded() { return Tactics.strikesLanded; }
    public static int team(GravesAndGlory.Battle b, UUID id) { return b.squads.get(id).team; }
    public static boolean respawning(GravesAndGlory.Battle b, UUID id) { return b.respawns.containsKey(id); }
    public static double ground(double x, double z) { return Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1; }
    public static boolean inArena(double x, double z) { return GravesAndGlory.Battle.inArena(x, z); }
    public static boolean wet(double x, double z) { return GravesAndGlory.Battle.wet(x, z); }
    public static void teleport(GravesAndGlory.Battle b, UUID id, double x, double z) {
        var p = b.server.getPlayerManager().getPlayer(id); p.networkHandler.requestTeleport(x, ground(x, z), z, p.getYaw(), p.getPitch());
    }
    /** Strikes the commander down by the real damage path; if that path did not take him (no battle hook for the source), the same steps it takes
     *  (Battle's downing blow: full health, to base, spectator, the respawn wait). Returns "damage" or "steps". */
    public static String kill(GravesAndGlory.Battle b, UUID id) {
        var p = b.server.getPlayerManager().getPlayer(id);
        p.damage(b.world.getDamageSources().generic(), 10000f);
        if (b.respawns.containsKey(id)) return "damage";
        p.setHealth(p.getMaxHealth()); b.toBase(p, b.squads.get(id).team); p.changeGameMode(GameMode.SPECTATOR);
        b.respawns.put(id, b.time + GravesAndGlory.R.respawnSeconds() * 20L);
        return "steps";
    }

    // Final fix wave (review I1, M4): the legacy bare-order path, Battle.command("charge"), as an old or modified client reaches it through the ORDER packet.
    /** The legacy command path with the command cooldown cleared first, for a command that should only meet the Charge cooldown. */
    /** Returns the lastOrder it left in place before the command, so a check can see whether a refusal kept it. */
    public static long legacy(GravesAndGlory.Battle b, UUID id, String cmd) { long cleared = Long.MIN_VALUE / 4; b.squads.get(id).lastOrder = cleared; b.command(b.server.getPlayerManager().getPlayer(id), cmd); return cleared; }
    public static long lastOrder(GravesAndGlory.Battle b, UUID id) { return b.squads.get(id).lastOrder; }
    /** Forgets the once-per-kind feedback throttle so a shout that a refused command wrongly played could not hide behind it. */
    public static void clearThrottle() { BattleEffects.lastFeedback.clear(); }
    /** How many charge horns this player has heard since the round started. */
    public static int chargeHeard(UUID id) { int n = 0; for (var h : BattleEffects.heard) if (h.player().equals(id) && h.kind().equals("charge")) n++; return n; }
    public static int chargeCooldownTicks() { return Spec.COMBATDISCIPLINE[0].chargeCooldownTicks(); }
    public static float yaw(GravesAndGlory.Battle b, UUID id) { return b.server.getPlayerManager().getPlayer(id).getYaw(); }
    /** The squad's stored assignments as text, for a check's detail. */
    public static String flags(GravesAndGlory.Battle b, UUID id) {
        StringBuilder out = new StringBuilder();
        for (var e : b.squads.get(id).assignments.entrySet()) out.append(e.getKey()).append('=').append(e.getValue().order).append(e.getValue().chargeDir != null ? " dir" : "").append(e.getValue().thenFollow ? " thenFollow" : "").append("; ");
        return out.toString();
    }
    /** Whether the squad has stored assignments and every one is this order with (or without) a chargeDir and thenFollow. */
    public static boolean allAssigned(GravesAndGlory.Battle b, UUID id, String order, boolean dir, boolean thenFollow) {
        var s = b.squads.get(id); if (s.assignments.isEmpty()) return false;
        for (var a : s.assignments.values()) if (!a.order.equals(order) || (a.chargeDir != null) != dir || a.thenFollow != thenFollow) return false;
        return true;
    }
}
