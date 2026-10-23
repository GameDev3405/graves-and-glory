package gg.gravesandglory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The kill-credit logs of in-match progression design 3.2: per victim, who damaged, slowed, pulled, weakened or zoned it
 * (hit); per unit, who healed or buffed it (support). Units are entity ids and times battle ticks; the caller logs only
 * troops that can earn credit (never a commander or a temporary troop) and converts progression.assistSeconds to ticks.
 * A unit that dies is forgotten: it is never credited again, its own victim log goes, but its blows and the support it
 * received stay as links until they leave the window, so whoever healed a fallen helper or a fallen killer inside the
 * window is still credited. Pure, no Minecraft types.
 */
public final class CreditLog {
    private record Entry(int team, long t) {}

    private final Map<Integer, Map<Integer, Entry>> hits = new HashMap<>();
    private final Map<Integer, Map<Integer, Entry>> supports = new HashMap<>();
    private final Set<Integer> gone = new HashSet<>();

    /** A helper of helperTeam touched the victim at t (a blow, a slow, a pull, a weaken, a zone tick); the latest time is kept. */
    public void hit(int victim, int helper, int helperTeam, long t) {
        put(hits, victim, helper, helperTeam, t);
    }

    /** A supporter of team healed or buffed the target at t (mend, howl, oath banner, an aura); the latest time is kept. */
    public void support(int target, int supporter, int team, long t) {
        put(supports, target, supporter, team, t);
    }

    /**
     * Who gets credit when the victim dies at t: the killer (a troop's entity id; pass a negative id when the blow was not
     * a troop's, such as a commander's or a fall), every helper of killerTeam who touched the victim within window ticks,
     * and every supporter of killerTeam who supported the killer or one of those helpers within the window. Dead units
     * are never in the set; the victim never is.
     */
    public Set<Integer> credited(int victim, int killer, int killerTeam, long t, long window) {
        Set<Integer> links = new LinkedHashSet<>();
        if (killer >= 0) links.add(killer);
        Map<Integer, Entry> onVictim = hits.get(victim);
        if (onVictim != null) for (var e : onVictim.entrySet()) if (e.getValue().team() == killerTeam && inWindow(e.getValue().t(), t, window)) links.add(e.getKey());
        Set<Integer> out = new LinkedHashSet<>(links);
        for (int link : links) {
            Map<Integer, Entry> s = supports.get(link);
            if (s != null) for (var e : s.entrySet()) if (e.getValue().team() == killerTeam && inWindow(e.getValue().t(), t, window)) out.add(e.getKey());
        }
        out.removeAll(gone);
        out.remove(victim);
        return out;
    }

    /** Drops every entry older than window ticks before t, and the forgotten units no entry names any more. */
    public void trim(long t, long window) {
        trim(hits, t, window);
        trim(supports, t, window);
        if (gone.isEmpty()) return;
        Set<Integer> linked = new HashSet<>(supports.keySet());
        for (var m : hits.values()) linked.addAll(m.keySet());
        gone.retainAll(linked);
    }

    /** The unit died or left the battle: it is never credited again and its own victim log goes; its links stay until trimmed. */
    public void forget(int unit) {
        hits.remove(unit);
        for (Iterator<Map<Integer, Entry>> it = supports.values().iterator(); it.hasNext();) {
            Map<Integer, Entry> m = it.next();
            m.remove(unit);
            if (m.isEmpty()) it.remove();
        }
        gone.add(unit);
    }

    /** Empties every log (a round boundary: startRound, returnLobby, switchMap). */
    public void clear() {
        hits.clear();
        supports.clear();
        gone.clear();
    }

    /** Logs unit against key at t, keeping the later time when the unit is already logged for the same team. */
    private static void put(Map<Integer, Map<Integer, Entry>> log, int key, int unit, int team, long t) {
        Map<Integer, Entry> m = log.computeIfAbsent(key, k -> new HashMap<>());
        Entry old = m.get(unit);
        if (old == null || old.team() != team || old.t() < t) m.put(unit, new Entry(team, t));
    }

    private static boolean inWindow(long at, long now, long window) {
        return at <= now && now - at <= window;
    }

    private static void trim(Map<Integer, Map<Integer, Entry>> log, long t, long window) {
        for (Iterator<Map<Integer, Entry>> it = log.values().iterator(); it.hasNext();) {
            Map<Integer, Entry> m = it.next();
            m.values().removeIf(e -> t - e.t() > window);
            if (m.isEmpty()) it.remove();
        }
    }
}
