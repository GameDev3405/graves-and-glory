package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import java.util.*;

/**
 * Unshipped. Per side and troop type, for one match or one fight: what each type fielded, lost, killed and dealt. Read-only: it looks at
 * Battle.units, the troops' health and Combat.LAST, and changes nothing in the game.
 *
 * - fielded: troops of the type that appeared (opening: present at the first tick after reset; bought: later).
 * - deaths: AFTER_DEATH of the troop (any cause). kills: deaths of enemy troops credited to the type (the source's attacker, else the newest
 *   logged blow on the victim); killGold: the sheet cost of those victims.
 * - dealt / taken: health removed (after armour) from a troop, split over the troops whose logged blows hit it in that tick in proportion to
 *   base x factor; health removed with no logged blow that tick is takenOther. A Rise Again counts the health down to 0 as taken and restarts the
 *   count from the risen health. healed: health gained.
 * - hits and factorSum: blows logged (Combat.LAST) by the type and the sum of their clamped factor (HitLog.total), so the mean factor says how
 *   hard the type's blows land on what it meets (a Bone Archer's arrow on a raised Gilded shield lands at about 0.25).
 * - firstKill: ticks from reset to the type's first kill, -1 for none.
 * overflowTicks counts ticks that logged more blows than Combat.LAST keeps (64); their oldest blows were missed for attribution.
 */
final class TypeLedger {
    static final class Tracked { final GravesAndGlory.Unit unit; final int side; final Row row; float health; boolean rose, open = true;
        Tracked(GravesAndGlory.Unit u, int side, Row row) { unit = u; this.side = side; this.row = row; health = u.mob.getHealth(); rose = u.rose; } }
    static final class Row { final String troop; final int cost; int fielded, opening, bought, deaths, rises, kills, killGold, hits;
        double dealt, taken, takenOther, healed, factorSum; long firstKill = -1;
        Row(String troop, int cost) { this.troop = troop; this.cost = cost; } }

    final Map<Integer, Tracked> byId = new HashMap<>();
    final List<Map<String, Row>> sides = List.of(new LinkedHashMap<>(), new LinkedHashMap<>());
    long started; boolean first; int overflowTicks; Combat.HitLog last;
    boolean active;

    void reset(long now) {
        byId.clear(); sides.get(0).clear(); sides.get(1).clear();
        started = now; first = true; overflowTicks = 0; last = Combat.LAST.peekLast(); active = true;
    }
    void stop() { active = false; }

    Row row(int side, GravesAndGlory.Unit u) { return sides.get(side).computeIfAbsent(u.spec.id(), k -> new Row(u.spec.id(), u.spec.cost())); }

    /** Once per server tick while a match or fight runs, after the battle's own tick. */
    void tick(GravesAndGlory.Battle b) {
        if (!active) return;
        for (var u : b.units) {
            if (u.temporary || byId.containsKey(u.mob.getId())) continue;
            int side = u.squad.team;
            if (side < 0 || side > 1) continue;
            Row r = row(side, u);
            r.fielded++;
            if (first) r.opening++; else r.bought++;
            byId.put(u.mob.getId(), new Tracked(u, side, r));
        }
        first = false;
        // The blows logged since the last tick, oldest first.
        List<Combat.HitLog> fresh = new ArrayList<>();
        boolean found = last == null;
        for (var it = Combat.LAST.descendingIterator(); it.hasNext(); ) { var h = it.next(); if (h == last) { found = true; break; } fresh.add(h); }
        if (!found && last != null) overflowTicks++;
        Collections.reverse(fresh);
        if (!Combat.LAST.isEmpty()) last = Combat.LAST.peekLast();
        Map<Integer, List<Combat.HitLog>> onTarget = new HashMap<>();
        for (var h : fresh) {
            var attacker = byId.get(h.attacker());
            if (attacker != null) { attacker.row.hits++; attacker.row.factorSum += h.total(); }
            onTarget.computeIfAbsent(h.target(), k -> new ArrayList<>()).add(h);
        }
        for (var t : byId.values()) {
            if (!t.open) continue;
            var mob = t.unit.mob;
            boolean gone = mob.isRemoved() && !mob.isDead();
            if (gone) { t.open = false; continue; } // discarded (the end of a fight or match): no damage
            float now = mob.isDead() ? 0 : mob.getHealth();
            float lost;
            if (t.unit.rose && !t.rose) { t.rose = true; t.row.rises++; lost = t.health; } // it fell to 0 and stood up at its risen health
            else lost = t.health - now;
            if (lost > 0) {
                t.row.taken += lost;
                var blows = onTarget.get(mob.getId());
                double weight = 0;
                if (blows != null) for (var h : blows) if (byId.containsKey(h.attacker())) weight += h.base() * h.total();
                if (weight > 0) { for (var h : blows) { var a = byId.get(h.attacker()); if (a != null) a.row.dealt += lost * h.base() * h.total() / weight; } }
                else t.row.takenOther += lost;
            } else if (lost < 0) t.row.healed += -lost;
            t.health = now;
            if (mob.isDead()) t.open = false;
        }
    }

    /** ServerLivingEntityEvents.AFTER_DEATH, registered by the simulation that owns the ledger. */
    void onDeath(GravesAndGlory.Battle b, LivingEntity e, DamageSource source) {
        if (!active || b == null) return;
        var victim = byId.get(e.getId());
        if (victim == null) return;
        victim.row.deaths++;
        Tracked killer = null;
        var attacker = source.getAttacker();
        if (attacker != null) killer = byId.get(attacker.getId());
        if (killer == null) {
            Combat.HitLog newest = null;
            for (var it = Combat.LAST.descendingIterator(); it.hasNext(); ) { var h = it.next(); if (h.target() == e.getId()) { newest = h; break; } }
            if (newest != null) killer = byId.get(newest.attacker());
        }
        if (killer == null || killer.side == victim.side) return;
        killer.row.kills++;
        killer.row.killGold += victim.row.cost;
        if (killer.row.firstKill < 0) killer.row.firstKill = b.time - started;
    }

    static final String HEADER = "troop,cost,fielded,opening,bought,deaths,rises,kills,killGold,dealt,taken,takenOther,healed,hits,meanFactor,firstKillSecond,overflowTicks";

    /** The CSV rows of one side, each prefixed with the caller's fields (already joined, ending without a comma). */
    String rows(int side, String prefix) {
        StringBuilder out = new StringBuilder();
        for (var r : sides.get(side).values())
            out.append(prefix).append(',').append(String.join(",", r.troop, String.valueOf(r.cost), String.valueOf(r.fielded), String.valueOf(r.opening), String.valueOf(r.bought),
                String.valueOf(r.deaths), String.valueOf(r.rises), String.valueOf(r.kills), String.valueOf(r.killGold), f1(r.dealt), f1(r.taken), f1(r.takenOther), f1(r.healed),
                String.valueOf(r.hits), r.hits == 0 ? "" : String.format(Locale.ROOT, "%.3f", r.factorSum / r.hits), r.firstKill < 0 ? "" : f1(r.firstKill / 20.0), String.valueOf(overflowTicks))).append('\n');
        return out.toString();
    }
    static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }
    int kills(int side) { int n = 0; for (var r : sides.get(side).values()) n += r.kills; return n; }
    int deaths(int side) { int n = 0; for (var r : sides.get(side).values()) n += r.deaths; return n; }
}
