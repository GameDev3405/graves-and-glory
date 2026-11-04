package gg.gravesandglory;

/**
 * The relic slots' clocks for one commander (pure, gear-combat Task 5): per slot, the battle time its cooldown ends (readyAt), the
 * time its run ends (until, 0 when it is not running) and what a running relic keeps (data: Mark Prey's mark, Bloodrush's tally,
 * Forward Supply's banner). Slots are independent: one cooling never holds another back.
 */
public final class RelicSlots {
    public final long[] readyAt, until;
    /** What each running relic keeps. Object, not double: Mark Prey keeps the marked entity and Forward Supply its banner. */
    public final Object[] data;

    public RelicSlots(int n) { readyAt = new long[n]; until = new long[n]; data = new Object[n]; }

    public int size() { return readyAt.length; }

    /** Whether the slot may start now: its cooldown is over and it is not running. */
    public boolean ready(int slot, long now) { return ok(slot) && now >= readyAt[slot] && !active(slot, now); }

    /** Starts the slot's cooldown and run from now (the relic's start has accepted). */
    public void begin(int slot, long now, int cooldownTicks, int durationTicks) {
        readyAt[slot] = now + Math.max(0, cooldownTicks);
        until[slot] = now + Math.max(0, durationTicks);
    }

    /** ready, then begin: false (and nothing changes) while the slot cools or runs. */
    public boolean tryStart(int slot, long now, int cooldownTicks, int durationTicks) {
        if (!ready(slot, now)) return false;
        begin(slot, now, cooldownTicks, durationTicks);
        return true;
    }

    /** Ticks until the slot may start again; 0 when ready. */
    public int readyIn(int slot, long now) { return ok(slot) ? (int) Math.max(0, readyAt[slot] - now) : 0; }

    /** Ticks the slot still runs; 0 when it is not running. */
    public int activeFor(int slot, long now) { return ok(slot) ? (int) Math.max(0, until[slot] - now) : 0; }

    /** Whether the slot runs now. */
    public boolean active(int slot, long now) { return ok(slot) && now < until[slot]; }

    /** Whether the slot has a run to end (started and not yet stopped), whatever the time. */
    public boolean started(int slot) { return ok(slot) && until[slot] > 0; }

    /** Ends the slot's run and clears its data; the cooldown stands. */
    public void stop(int slot) { if (!ok(slot)) return; until[slot] = 0; data[slot] = null; }

    /** A fresh round: every run ended and every cooldown clear. */
    public void clear() { for (int i = 0; i < size(); i++) { stop(i); readyAt[i] = 0; } }

    boolean ok(int slot) { return slot >= 0 && slot < readyAt.length; }
}
