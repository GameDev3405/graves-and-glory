package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import java.util.*;

/**
 * The client half of an order's feedback (Task 9): when an acknowledgement arrives (command_v1 ACK, to the issuer and his teammates) the troops it
 * lists raise their weapons for RALLY_TICKS (the "rally" stance, TroopMotion.stance), and OrderOutlines is told so it can pulse the target.
 * The horns and shouts are not here: they are world sounds the server plays at the commander (BattleEffects.feedback, from OrderRules), so a
 * second client-side cue would only double them.
 */
public final class OrderFeedback {
    private OrderFeedback() {}
    /** How long a troop holds its weapon up after an acknowledgement (the brief: 20 ticks). */
    public static final int RALLY_TICKS = 20;
    /** Client ticks run since the world loaded (paused with the game); every duration here counts them. */
    static long now;
    private static final Map<Integer, Long> rallyUntil = new HashMap<>();
    /** The acknowledgements already handled (by identity: the list shifts as it fills). */
    private static final Set<CommandNet.Client.Ack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Acknowledgements handled since the world loaded (for the harness). */
    public static int handled;

    /** Whether this troop's weapon is up now. */
    public static boolean rallying(int entityId) {
        Long until = rallyUntil.get(entityId);
        return until != null && now < until;
    }

    /** An acknowledgement arrived: every troop it lists raises its weapon, and the target pulses. */
    public static void onAck(CommandNet.Client.Ack ack) {
        handled++;
        for (int id : ack.units()) rallyUntil.put(id, now + RALLY_TICKS);
        OrderOutlines.onAck(ack);
    }

    /** Every client tick: the clock, then the acknowledgements that have arrived since the last one. */
    static void tick(MinecraftClient c) {
        if (c.world == null) { reset(); return; }
        now++;
        for (var ack : CommandNet.Client.acks) if (seen.add(ack)) onAck(ack);
        seen.removeIf(a -> !CommandNet.Client.acks.contains(a));
        rallyUntil.values().removeIf(until -> now >= until);
    }

    /** A disconnect or a new round: nothing of the last battle's orders stays. */
    public static void reset() { rallyUntil.clear(); seen.clear(); now = 0; handled = 0; }
}
