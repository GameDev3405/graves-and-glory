package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A player's group order from the command panel (command UI plan Task 3): the one entry tactical_v1 reaches (Battle.tactical), with the gates
 * today's intake kept (seated, running, the round intro, respawning, commandCooldownTicks). The server decides every point:
 * <ul>
 *   <li>attack: the named groups' centre, hunting map-wide (Assignment.hunt); x and z are ignored.</li>
 *   <li>charge: from the aimed start (x, z) along the look yaw, chargeRunBlocks out, walked back to dry ground inside the arena; once per
 *       chargeCooldownTicks (Squad.chargeReadyAt).</li>
 *   <li>follow and retreat: the commander's own position (Retreat then switches to Follow me, Assignment.thenFollow).</li>
 *   <li>hold: the Formation order, at the dry anchor (x, z) in the shape and yaw sent. An old client's move arrives as hold (loadOrder).</li>
 *   <li>formation: a shape change only, as before.</li>
 * </ul>
 * After a stored order the issuer and his teammates get an ACK, and for hold and charge his teammates an OUTLINE (CommandNet). Bots and SiegeMode
 * call Battle.issue directly and never pass here.
 */
final class OrderRules {
    private OrderRules() {}
    static final Spec.RulesRow R = Spec.RULES[0];

    /** The command panel's order, as tactical_v1 brings it with no aimed enemy. */
    static void playerOrder(GravesAndGlory.Battle b, ServerPlayerEntity p, String group, String action, String formation, double x, double z, float yaw) {
        playerOrder(b, p, group, action, formation, x, z, yaw, -1);
    }

    /** As above, with tactical_v1's aimed enemy (entity id, -1 none), which an Attack or a Charge may still focus. */
    static void playerOrder(GravesAndGlory.Battle b, ServerPlayerEntity p, String group, String action, String formation, double x, double z, float yaw, int enemyId) {
        var s = b.squads.get(p.getUuid());
        if (s == null || !b.ready || !b.running || !s.seated || b.time < b.campaign.introUntil || b.respawns.containsKey(s.owner) || b.time - s.lastOrder < R.commandCooldownTicks()) return;
        if (group == null || action == null || formation == null) return;
        // custom:<n> groups are Task 12's; until then (and for any unknown id) the order is ignored, as is an unknown shape
        if (Arrays.stream(Spec.GROUPS).noneMatch(g -> g.id().equals(group)) || Arrays.stream(Spec.FORMATIONS).noneMatch(f -> f.id().equals(formation))) return;
        if (action.equals("formation")) {
            for (var g : Spec.GROUPS) if (!g.id().equals("all") && (group.equals("all") || group.equals(g.id()))) { var a = s.assignments.computeIfAbsent(g.id(), k -> new GravesAndGlory.Assignment(s.order, s.formation, s.target, s.yaw)); a.formation = formation; }
            s.lastOrder = b.time; b.campaign.tutorialDid(p, 1);
            CommandNet.ack(b, s, "formation", unitIds(b, s, group), centre(b, s, group, p.getPos()).x, centre(b, s, group, p.getPos()).z);
            return;
        }
        if (Arrays.stream(Spec.ORDERS).noneMatch(o -> o.id().equals(action)) || !Float.isFinite(yaw)) return;
        String order = loadOrder(action);
        if (!order.equals("attack") && !order.equals("charge")) enemyId = -1;
        Entity enemy = enemyId >= 0 ? b.world.getEntityById(enemyId) : null;
        if (enemyId >= 0 && (enemy == null || !enemy.isAlive() || b.teamOf(enemy) < 0 || b.teamOf(enemy) == s.team || !p.canSee(enemy))) { p.sendMessage(Text.literal("Target lost. Aim again or issue an attack-move."), true); return; }

        Vec3d dest; Vec3d chargeDir = null;
        switch (order) {
            case "attack" -> dest = GravesAndGlory.Battle.groundAt(centre(b, s, group, p.getPos()).x, centre(b, s, group, p.getPos()).z);
            case "follow", "retreat" -> dest = p.getPos();
            case "charge" -> {
                if (!chargeReady(b, s)) {
                    int left = (int) (s.chargeReadyAt - b.time);
                    CommandNet.cooldown(p, left);
                    p.sendMessage(Text.literal("Charge ready in " + (left + 19) / 20 + "s"), true);
                    return;
                }
                if (!GravesAndGlory.Battle.inArena(x, z)) return;
                double r = Math.toRadians(yaw), dx = Math.sin(-r), dz = Math.cos(r);
                Vec3d end = null;
                for (int d = Spec.COMBATDISCIPLINE[0].chargeRunBlocks(); d >= 0; d--) {
                    double ex = x + dx * d, ez = z + dz * d;
                    if (GravesAndGlory.Battle.inArena(ex, ez) && !GravesAndGlory.Battle.wet(ex, ez)) { end = new Vec3d(ex, 0, ez); break; }
                }
                if (end == null) { p.sendMessage(Text.literal("Choose dry ground or a crossing."), true); return; }
                dest = GravesAndGlory.Battle.groundAt(end.x, end.z);
                chargeDir = new Vec3d(dx, 0, dz).normalize();
            }
            default -> { // hold: the Formation order's anchor
                if (!GravesAndGlory.Battle.inArena(x, z)) return;
                if (GravesAndGlory.Battle.wet(x, z)) { p.sendMessage(Text.literal("Choose dry ground or a crossing."), true); return; }
                dest = GravesAndGlory.Battle.groundAt(x, z);
            }
        }

        s.lastOrder = b.time;
        boolean charging = order.equals("charge") && b.startsCharge(s, group);
        b.issue(s, group, order, formation, dest, yaw, enemyId);
        for (var g : Spec.GROUPS) if (!g.id().equals("all") && (group.equals("all") || group.equals(g.id()))) {
            var a = s.assignments.get(g.id());
            if (a == null) continue;
            a.hunt = order.equals("attack");
            a.chargeDir = chargeDir;
            a.thenFollow = order.equals("retreat");
        }
        if (order.equals("charge")) { markChargeUsed(b, s); CommandNet.cooldown(p, Spec.COMBATDISCIPLINE[0].chargeCooldownTicks()); }
        if (charging) b.markCharge(s, group);
        b.campaign.tutorialDid(p, order.equals("attack") || order.equals("charge") ? 2 : 0); b.campaign.progressEvent(p.getUuid(), false);
        // The command's feedback, to this player alone, as before: Retreat's and Charge's cues, every other order the chime (order sounds are Task 9's).
        if (order.equals("retreat") || order.equals("charge")) BattleEffects.feedback(b, p, order); else p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, .6f, 1.1f);
        p.sendMessage(Text.literal(group + " / " + order + " / " + (int) dest.x + ", " + (int) dest.z), true);

        CommandNet.ack(b, s, order, unitIds(b, s, group), dest.x, dest.z);
        if (order.equals("hold") || order.equals("charge")) CommandNet.outline(b, s, formation, dest.x, dest.z, yaw, members(b, s, group));
    }

    /** Whether this squad's Charge is off cooldown. */
    static boolean chargeReady(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { return b.time >= s.chargeReadyAt; }

    /** Starts this squad's Charge cooldown: chargeCooldownTicks from now. */
    static void markChargeUsed(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { s.chargeReadyAt = b.time + Spec.COMBATDISCIPLINE[0].chargeCooldownTicks(); }

    /** An old order id onto the new set: move and hold are hold (the Formation order); see BattleMath.loadOrder. */
    static String loadOrder(String order) { return BattleMath.loadOrder(order); }

    /** An old or unknown formation id onto the new set (block stays block, labelled Shield wall); see BattleMath.loadFormation. */
    static String loadFormation(String formation) { return BattleMath.loadFormation(formation); }

    static boolean named(String group, GravesAndGlory.Unit u) { return group.equals("all") || u.spec.group().equals(group); }

    /** The average position of the named groups' living troops, else the fallback (the commander). */
    static Vec3d centre(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group, Vec3d fallback) {
        double x = 0, z = 0; int n = 0;
        for (var u : b.units) if (u.squad == s && named(group, u) && u.mob.isAlive()) { x += u.mob.getX(); z += u.mob.getZ(); n++; }
        return n == 0 ? fallback : new Vec3d(x / n, fallback.y, z / n);
    }

    /** The entity ids of the named groups' living troops: the ones that raise their weapons on the ACK. */
    static int[] unitIds(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group) {
        return b.units.stream().filter(u -> u.squad == s && named(group, u) && u.mob.isAlive()).mapToInt(u -> u.mob.getId()).toArray();
    }

    /** Each named group with its living troops, for the OUTLINE (for "all", only the groups that have some). */
    static List<Map.Entry<String, Integer>> members(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group) {
        List<Map.Entry<String, Integer>> out = new ArrayList<>();
        for (var g : Spec.GROUPS) {
            if (g.id().equals("all") || !(group.equals("all") || group.equals(g.id()))) continue;
            int n = (int) b.units.stream().filter(u -> u.squad == s && u.spec.group().equals(g.id()) && u.mob.isAlive()).count();
            if (n > 0 || !group.equals("all")) out.add(Map.entry(g.id(), n));
        }
        return out;
    }
}
