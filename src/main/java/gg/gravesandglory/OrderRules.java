package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A player's group order from the command panel (command UI plan Task 3, 3b): the one entry both order packets reach. tactical_v2 carries every
 * selected group in one packet (OrderRules.At: each group with its own point); tactical_v1, kept for old clients, bots and the harness, carries one.
 * Either way it is ONE order: one set of gates (seated, running, the round intro, respawning, commandCooldownTicks), one lastOrder stamp, one
 * ACK and OUTLINE wave, and for a Charge one cooldown for the commander (Squad.chargeReadyAt). The server decides every point:
 * <ul>
 *   <li>attack: each named group's own centre, hunting map-wide (Assignment.hunt); x and z are ignored.</li>
 *   <li>charge: from each group's aimed start (x, z) along the look yaw, chargeRunBlocks out, walked back to dry ground inside the arena; once per
 *       chargeCooldownTicks per commander, however many groups it moves.</li>
 *   <li>follow and retreat: the commander's own position (Retreat then switches to Follow me, Assignment.thenFollow).</li>
 *   <li>hold: the Formation order, at the dry anchor (x, z) in the shape and yaw sent. An old client's move arrives as hold (loadOrder).</li>
 *   <li>formation: a shape change only, as before.</li>
 * </ul>
 * Unknown ids, and custom:&lt;n&gt; until Task 12, are skipped; "all" in the list stands for the whole army alone. A group whose own point fails
 * (outside the arena, no dry end) is left out and the rest still move; when none is left the order is refused. After a stored order the issuer
 * and his teammates get one ACK, and for hold and charge his teammates one OUTLINE per distinct destination (CommandNet). Bots and SiegeMode call
 * Battle.issue directly and never pass here; they share the commander's Charge cooldown through chargeReady and markChargeUsed (or useCharge).
 */
final class OrderRules {
    private OrderRules() {}
    static final Spec.RulesRow R = Spec.RULES[0];
    /** The most groups one order packet may name (five groups and the custom ones of Task 12). */
    static final int MAX_GROUPS = 16;

    /** One group an order names, with its own point: a Charge's start (the group's centre as the client saw it), a Formation's anchor. */
    record At(String group, double x, double z) {}

    /** The command panel's order to one group, as tactical_v1 brings it with no aimed enemy. */
    static void playerOrder(GravesAndGlory.Battle b, ServerPlayerEntity p, String group, String action, String formation, double x, double z, float yaw) {
        playerOrder(b, p, group, action, formation, x, z, yaw, -1);
    }

    /** As above, with tactical_v1's aimed enemy (entity id, -1 none), which an Attack or a Charge may still focus. */
    static void playerOrder(GravesAndGlory.Battle b, ServerPlayerEntity p, String group, String action, String formation, double x, double z, float yaw, int enemyId) {
        playerOrder(b, p, List.of(new At(group, x, z)), action, formation, yaw, enemyId);
    }

    /** One order to every group in the list (tactical_v2): see the class comment. */
    static void playerOrder(GravesAndGlory.Battle b, ServerPlayerEntity p, List<At> groups, String action, String formation, float yaw, int enemyId) {
        var s = b.squads.get(p.getUuid());
        if (s == null || !b.ready || !b.running || !s.seated || b.time < b.campaign.introUntil || b.respawns.containsKey(s.owner) || b.time - s.lastOrder < R.commandCooldownTicks()) return;
        if (groups == null || action == null || formation == null) return;
        if (Arrays.stream(Spec.FORMATIONS).noneMatch(f -> f.id().equals(formation))) return;
        List<At> named = known(groups);
        if (named.isEmpty()) return;
        List<String> ids = named.stream().map(At::group).toList();
        Vec3d at = centre(b, s, ids, p.getPos());
        if (action.equals("formation")) {
            for (var g : Spec.GROUPS) if (!g.id().equals("all") && covers(ids, g.id())) { var a = s.assignments.computeIfAbsent(g.id(), k -> new GravesAndGlory.Assignment(s.order, s.formation, s.target, s.yaw)); a.formation = formation; }
            s.lastOrder = b.time; b.campaign.tutorialDid(p, 1);
            CommandNet.ack(b, s, "formation", unitIds(b, s, ids), at.x, at.z);
            return;
        }
        if (Arrays.stream(Spec.ORDERS).noneMatch(o -> o.id().equals(action)) || !Float.isFinite(yaw)) return;
        String order = loadOrder(action);
        if (!order.equals("attack") && !order.equals("charge")) enemyId = -1;
        Entity enemy = enemyId >= 0 ? b.world.getEntityById(enemyId) : null;
        if (enemyId >= 0 && (enemy == null || !enemy.isAlive() || b.teamOf(enemy) < 0 || b.teamOf(enemy) == s.team || !p.canSee(enemy))) { p.sendMessage(Text.literal("Target lost. Aim again or issue an attack-move."), true); return; }
        if (order.equals("charge") && !chargeReady(b, s)) {
            int left = (int) (s.chargeReadyAt - b.time);
            CommandNet.cooldown(p, left);
            p.sendMessage(Text.literal("Charge ready in " + (left + 19) / 20 + "s"), true);
            return;
        }

        // Each group's destination; a group whose point fails is left out, and with none left the order is refused.
        double r = Math.toRadians(yaw), dx = Math.sin(-r), dz = Math.cos(r);
        Vec3d chargeDir = order.equals("charge") ? new Vec3d(dx, 0, dz).normalize() : null;
        Map<String, Vec3d> dests = new LinkedHashMap<>();
        boolean wetRefusal = false;
        for (At g : named) {
            Vec3d dest = null;
            switch (order) {
                case "attack" -> { Vec3d c = centre(b, s, List.of(g.group()), p.getPos()); dest = GravesAndGlory.Battle.groundAt(c.x, c.z); }
                case "follow", "retreat" -> dest = p.getPos();
                case "charge" -> {
                    if (!GravesAndGlory.Battle.inArena(g.x(), g.z())) break;
                    for (int d = Spec.COMBATDISCIPLINE[0].chargeRunBlocks(); d >= 0 && dest == null; d--) {
                        double ex = g.x() + dx * d, ez = g.z() + dz * d;
                        if (GravesAndGlory.Battle.inArena(ex, ez) && !GravesAndGlory.Battle.wet(ex, ez)) dest = GravesAndGlory.Battle.groundAt(ex, ez);
                    }
                    if (dest == null) wetRefusal = true;
                }
                default -> { // hold: the Formation order's anchor
                    if (!GravesAndGlory.Battle.inArena(g.x(), g.z())) break;
                    if (GravesAndGlory.Battle.wet(g.x(), g.z())) { wetRefusal = true; break; }
                    dest = GravesAndGlory.Battle.groundAt(g.x(), g.z());
                }
            }
            if (dest != null) dests.put(g.group(), dest);
        }
        if (dests.isEmpty()) { if (wetRefusal) p.sendMessage(Text.literal("Choose dry ground or a crossing."), true); return; }

        // One order: one stamp, every group issued in this tick, one charge cooldown, one feedback, one ACK.
        s.lastOrder = b.time;
        List<String> moved = new ArrayList<>(dests.keySet());
        List<String> charging = new ArrayList<>();
        for (var e : dests.entrySet()) {
            String group = e.getKey();
            if (order.equals("charge") && b.startsCharge(s, group)) charging.add(group);
            b.issue(s, group, order, formation, e.getValue(), yaw, enemyId);
        }
        for (var g : Spec.GROUPS) if (!g.id().equals("all") && covers(moved, g.id())) {
            var a = s.assignments.get(g.id());
            if (a == null) continue;
            a.hunt = order.equals("attack");
            a.chargeDir = chargeDir;
            a.thenFollow = order.equals("retreat");
        }
        if (order.equals("charge")) { markChargeUsed(b, s); CommandNet.cooldown(p, Spec.COMBATDISCIPLINE[0].chargeCooldownTicks()); }
        for (String group : charging) b.markCharge(s, group);
        b.campaign.tutorialDid(p, order.equals("attack") || order.equals("charge") ? 2 : 0); b.campaign.progressEvent(p.getUuid(), false);
        // The command's feedback, to this player alone, as before: Retreat's and Charge's cues, every other order the chime (order sounds are Task 9's).
        if (order.equals("retreat") || order.equals("charge")) BattleEffects.feedback(b, p, order); else p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, .6f, 1.1f);
        Vec3d mid = mean(dests.values());
        p.sendMessage(Text.literal(String.join("+", moved) + " / " + order + " / " + (int) mid.x + ", " + (int) mid.z), true);

        CommandNet.ack(b, s, order, unitIds(b, s, moved), mid.x, mid.z);
        if (order.equals("hold") || order.equals("charge")) {
            // One OUTLINE per distinct destination: a Formation order's groups share its anchor, a Charge's each run from their own start.
            Map<Vec3d, List<String>> byDest = new LinkedHashMap<>();
            for (var e : dests.entrySet()) byDest.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            for (var e : byDest.entrySet()) CommandNet.outline(b, s, formation, e.getKey().x, e.getKey().z, yaw, members(b, s, e.getValue()));
        }
    }

    /** The list's known groups in order, each once: "all" alone if it is named; unknown ids (custom:n until Task 12) and nulls are skipped. */
    static List<At> known(List<At> groups) {
        List<At> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (At g : groups) {
            if (g == null || g.group() == null || Arrays.stream(Spec.GROUPS).noneMatch(x -> x.id().equals(g.group()))) continue;
            if (g.group().equals("all")) return List.of(g);
            if (seen.add(g.group())) out.add(g);
            if (out.size() >= MAX_GROUPS) break;
        }
        return out;
    }

    static Vec3d mean(Collection<Vec3d> points) {
        double x = 0, z = 0; for (var v : points) { x += v.x; z += v.z; }
        return new Vec3d(x / points.size(), 0, z / points.size());
    }

    /** Whether this commander's Charge is off cooldown (the cooldown is the squad's, so per commander, not per group). */
    static boolean chargeReady(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { return b.time >= s.chargeReadyAt; }

    /** Starts this squad's Charge cooldown: chargeCooldownTicks from now. One per commander, whichever groups the charge moves. */
    static void markChargeUsed(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { s.chargeReadyAt = b.time + Spec.COMBATDISCIPLINE[0].chargeCooldownTicks(); }

    /** A bot's or AI commander's charge decision, however many groups it moves: true and the cooldown started when the Charge is ready, else
     *  false (the decision must not charge). Call it once per decision, never once per group, so one decision is one charge. */
    static boolean useCharge(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (!chargeReady(b, s)) return false;
        markChargeUsed(b, s);
        return true;
    }

    /** An old order id onto the new set: move and hold are hold (the Formation order); see BattleMath.loadOrder. */
    static String loadOrder(String order) { return BattleMath.loadOrder(order); }

    /** An old or unknown formation id onto the new set (block stays block, labelled Shield wall); see BattleMath.loadFormation. */
    static String loadFormation(String formation) { return BattleMath.loadFormation(formation); }

    /** Whether a group id is covered by the named groups ("all" covers every one). */
    static boolean covers(Collection<String> groups, String id) { return groups.contains("all") || groups.contains(id); }
    static boolean named(Collection<String> groups, GravesAndGlory.Unit u) { return covers(groups, u.spec.group()); }

    /** The average position of the named groups' living troops, else the fallback (the commander). */
    static Vec3d centre(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group, Vec3d fallback) { return centre(b, s, List.of(group), fallback); }
    static Vec3d centre(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Collection<String> groups, Vec3d fallback) {
        double x = 0, z = 0; int n = 0;
        for (var u : b.units) if (u.squad == s && named(groups, u) && u.mob.isAlive()) { x += u.mob.getX(); z += u.mob.getZ(); n++; }
        return n == 0 ? fallback : new Vec3d(x / n, fallback.y, z / n);
    }

    /** The entity ids of the named groups' living troops: the ones that raise their weapons on the ACK. */
    static int[] unitIds(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group) { return unitIds(b, s, List.of(group)); }
    static int[] unitIds(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Collection<String> groups) {
        return b.units.stream().filter(u -> u.squad == s && named(groups, u) && u.mob.isAlive()).mapToInt(u -> u.mob.getId()).toArray();
    }

    /** Each named group with its living troops, for the OUTLINE (for "all", only the groups that have some). */
    static List<Map.Entry<String, Integer>> members(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Collection<String> groups) {
        boolean all = groups.contains("all");
        List<Map.Entry<String, Integer>> out = new ArrayList<>();
        for (var g : Spec.GROUPS) {
            if (g.id().equals("all") || !covers(groups, g.id())) continue;
            int n = (int) b.units.stream().filter(u -> u.squad == s && u.spec.group().equals(g.id()) && u.mob.isAlive()).count();
            if (n > 0 || !all) out.add(Map.entry(g.id(), n));
        }
        return out;
    }
}
