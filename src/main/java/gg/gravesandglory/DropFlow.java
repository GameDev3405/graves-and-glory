package gg.gravesandglory;

import java.util.*;

/**
 * The drop board's server bookkeeping (design 2026-11-14, sections 2.1 and 8), pure: which commander has a coin on the rail or in the
 * pegs, when it drops by itself and when it is over. A drop holds no value: the roll was paid, drawn and saved before begin, and the
 * drop only carries the result (the card the War Table shows) until its reveal. Ending a drop early, by a skip, a disconnect or a match
 * starting, therefore loses and doubles nothing; the caller publishes the card of every drop this hands back.
 */
final class DropFlow {
    enum Stage { AIM, SEARCH, DROP }

    /** One commander's drop. card and notice are the result to publish at its end; what tells the roll kinds apart ("unit", "gear", "relic"). */
    static final class Drop {
        final int id; final UUID who; final String name, what, card, notice; final int slot; final long began;
        Stage stage = Stage.AIM;
        /** Where the coin is on the rail (DropSim's x), and from the release where it was let go. */
        double x = DropSim.WIDTH / 2.0;
        long aimEnds, ends;
        DropSim.Path path;
        Drop(int id, UUID who, String name, String what, int slot, String card, String notice, long now) { this.id = id; this.who = who; this.name = name; this.what = what; this.slot = slot; this.card = card; this.notice = notice; this.began = now; }
    }

    private final Map<UUID, Drop> drops = new LinkedHashMap<>();
    private int serial;

    /** Ticks (20 a second) from tenths of a second. */
    static long ticks(int tenths) { return tenths * 2L; }
    /** The aim in ticks: the fly-in, the aim timer and half a second of grace for the roller's connection. */
    static long aimTicks() { var b = Spec.DROPBOARD[0]; return ticks(b.flyTenths()) + b.aimSeconds() * 20L + 10; }

    Drop of(UUID who) { return drops.get(who); }
    Collection<Drop> active() { return Collections.unmodifiableCollection(drops.values()); }

    /**
     * A new drop for a commander whose roll has just been paid and saved. A drop he still had is ended and handed back first in the list
     * (its card is due); the new one is last.
     */
    List<Drop> begin(UUID who, String name, String what, int slot, String card, String notice, long now) {
        List<Drop> out = new ArrayList<>();
        Drop old = drops.remove(who);
        if (old != null) out.add(old);
        Drop d = new Drop(++serial, who, name, what, slot, card, notice, now);
        d.aimEnds = now + aimTicks();
        drops.put(who, d);
        out.add(d);
        return out;
    }

    /** The roller slides the coin: true when it moved (he is aiming). */
    boolean aim(UUID who, double x) {
        Drop d = drops.get(who);
        if (d == null || d.stage != Stage.AIM || Double.isNaN(x)) return false;
        d.x = DropSim.clampStart(x);
        return true;
    }

    /** The roller lets the coin go at x: the drop, now waiting for its path, or null when he has no coin on the rail (a second send changes nothing). */
    Drop release(UUID who, double x) {
        Drop d = drops.get(who);
        if (d == null || d.stage != Stage.AIM) return null;
        if (!Double.isNaN(x)) d.x = DropSim.clampStart(x);
        d.stage = Stage.SEARCH;
        return d;
    }

    /** The coins whose aim timer ran out: each is let go where it stands. */
    List<Drop> overdue(long now) {
        List<Drop> out = new ArrayList<>();
        for (Drop d : drops.values()) if (d.stage == Stage.AIM && now >= d.aimEnds) { d.stage = Stage.SEARCH; out.add(d); }
        return out;
    }

    /** The path found for a released coin (DropSim.find from d.x to d.slot): false when that drop is gone or not waiting for one. */
    boolean falling(UUID who, int id, DropSim.Path path, long now) {
        Drop d = drops.get(who);
        if (d == null || d.id != id || d.stage != Stage.SEARCH || path.slot() != d.slot) return false;
        var b = Spec.DROPBOARD[0];
        d.path = path; d.stage = Stage.DROP;
        d.ends = now + (long) Math.ceil(path.seconds() * 20) + ticks(b.flareTenths()) + ticks(b.revealTenths());
        return true;
    }

    /** The drops whose reveal is over, removed: their cards are due. */
    List<Drop> finished(long now) {
        List<Drop> out = new ArrayList<>();
        for (var it = drops.values().iterator(); it.hasNext(); ) { Drop d = it.next(); if (d.stage == Stage.DROP && now >= d.ends) { out.add(d); it.remove(); } }
        return out;
    }

    /** Ends a commander's drop now (a skip, a disconnect): the drop, whose card is due, or null. */
    Drop end(UUID who) { return drops.remove(who); }
    /** Ends every drop (a match starting): their cards are due. */
    List<Drop> endAll() { List<Drop> out = new ArrayList<>(drops.values()); drops.clear(); return out; }
}
