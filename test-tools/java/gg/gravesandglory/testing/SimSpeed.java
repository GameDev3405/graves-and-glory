package gg.gravesandglory.testing;

/** Unshipped. -Dgravesandglory.simFast=true (gradle -PsimFast=true) lets a headless simulation server start its next tick as soon as the
 *  last one ends instead of waiting for the next 50 ms slot (FastTickMixin). Game logic per tick is unchanged: every rule counts ticks,
 *  so only wall time changes. Off for every other run. */
public final class SimSpeed {
    private SimSpeed() {}
    public static final boolean FAST = Boolean.getBoolean("gravesandglory.simFast");
    /** Set once the mixin has run, so a run can say that fast ticks really applied. */
    public static volatile boolean applied;
}
