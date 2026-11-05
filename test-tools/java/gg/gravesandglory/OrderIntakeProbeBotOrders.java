package gg.gravesandglory;

import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Test-only reach into the package-private battle state for cmdui.BotOrdersSuite (command UI plan Task 11): a real bot squad in Battle.squads, so
 * Campaign.botDecide runs on its own cadence, and what that squad stores. Never included in releases (its name carries the OrderIntakeProbe guard token).
 */
public final class OrderIntakeProbeBotOrders {
    private OrderIntakeProbeBotOrders() {}

    /** A bot army of the team in Battle.squads (normal tier, no banner, no gold: it fields only what the suite spawns for it). */
    public static Object addBot(GravesAndGlory.Battle b, int team, Vec3d at) {
        var s = new GravesAndGlory.Squad(UUID.randomUUID(), team, at);
        s.bot = true; s.seated = true; s.tier = "normal"; s.order = "hold"; s.target = at; s.gold = 0;
        b.squads.put(s.owner, s);
        return s;
    }
    /** Takes the bot army out of Battle.squads again. */
    public static void drop(GravesAndGlory.Battle b, Object squad) { b.squads.remove(((GravesAndGlory.Squad) squad).owner); }

    /** Whether the army stores a Charge: its squad order or any group's assignment. */
    public static boolean charging(Object squad) {
        var s = (GravesAndGlory.Squad) squad;
        if (s.order.equals("charge")) return true;
        for (var a : s.assignments.values()) if (a.order.equals("charge")) return true;
        return false;
    }
    /** Whether the army stores a hunting Attack (only a player's Attack may). */
    public static boolean hunting(Object squad) {
        for (var a : ((GravesAndGlory.Squad) squad).assignments.values()) if (a.hunt) return true;
        return false;
    }
    public static String order(Object squad) { return ((GravesAndGlory.Squad) squad).order; }
    public static Vec3d target(Object squad) { return ((GravesAndGlory.Squad) squad).target; }
}
