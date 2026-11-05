package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandMode;
import gg.gravesandglory.CommandNet;
import gg.gravesandglory.CommandSelection;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.OrderIntakeProbe;
import gg.gravesandglory.OrderIntakeProbe.Snap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

/**
 * Command UI plan Task 3: the server's order intake for the command panel. Every order is a real tactical_v1 packet from this client
 * (CommandMode.issue, through OrderIntakeProbe.send); the checks read what the server stored (OrderIntakeProbe.snap) and what command_v1 brought
 * back (CommandNet.Client) or was addressed to whom (CommandNet.sent). Never included in releases.
 */
public final class OrderIntakeSuite implements CommandUiChecks.Suite {
    UUID id;
    volatile double cx, cz, px, pz, py;
    volatile Snap first;
    volatile long readyAt;
    volatile UUID mate, enemy;
    volatile Snap infBefore, rngBefore;
    volatile boolean clientCooldownSeen;
    // Task 3b: multi-group orders as one order
    volatile Vec3d infC, rngC;
    volatile Snap mInf, mRng;
    volatile long mReady;
    volatile int before, chargeSends, followSends, attackSends, cooldownsAfterFirst;
    volatile boolean localRefused, v2open, v1open, customOpen;
    volatile Snap followInf, followRng;

    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) {
            id = c.player.getUuid();
            CommandNet.Client.reset();
            CommandUiChecks.seatAndStart(c);
        }
        if (tick == 40) {
            if (!GravesAndGloryClient.battleActive()) { CommandUiChecks.report("cu-intake-battle", false, "no battle after seatAndStart"); CommandUiChecks.finish(c); return; }
            CommandUiChecks.server(c, b -> {
                var p = b.server.getPlayerManager().getPlayer(id);
                Vec3d centre = OrderIntakeProbe.centre(b, id, "infantry", p.getPos());
                cx = centre.x; cz = centre.z; px = p.getX(); pz = p.getZ(); py = p.getY();
                OrderIntakeProbe.respawning(b, id, false); OrderIntakeProbe.resetCharge(b, id);
            });
        }
        // Attack: hunt, at the group's centre (x/z sent are ignored)
        if (tick == 45) OrderIntakeProbe.send("infantry", "attack", "line", 0, 0, 0);
        if (tick == 55) CommandUiChecks.server(c, b -> {
            Snap a = OrderIntakeProbe.snap(b, id, "infantry");
            boolean near = a != null && Math.hypot(a.tx() - cx, a.tz() - cz) < 12;
            CommandUiChecks.report("cu-intake-attack-hunt", a != null && a.order().equals("attack") && a.hunt() && near,
                "stored " + a + "; group centre at send " + (int) cx + ", " + (int) cz + " (x/z sent were 0, 0)");
        });
        // Charge: a unit direction, then refused for chargeCooldownTicks; the client hears the cooldown
        if (tick == 60) OrderIntakeProbe.send("infantry", "charge", "wedge", cx, cz, -90f);
        if (tick >= 61 && tick <= 80 && CommandNet.Client.chargeReadyIn > 0) clientCooldownSeen = true;
        if (tick == 70) CommandUiChecks.server(c, b -> { first = OrderIntakeProbe.snap(b, id, "infantry"); readyAt = OrderIntakeProbe.chargeReadyAt(b, id); });
        if (tick == 75) OrderIntakeProbe.send("infantry", "charge", "block", cx, cz, 90f);
        if (tick == 85) CommandUiChecks.server(c, b -> {
            Snap again = OrderIntakeProbe.snap(b, id, "infantry");
            double len = first == null || first.chargeDir() == null ? -1 : first.chargeDir().length();
            boolean dirOk = first != null && first.order().equals("charge") && Math.abs(len - 1) <= 0.01 && first.chargeDir().x > 0.99;
            boolean refused = again != null && again.equals(first);
            boolean cooling = readyAt > OrderIntakeProbe.time(b) && readyAt - OrderIntakeProbe.time(b) <= 400;
            CommandUiChecks.report("cu-intake-charge-cooldown", dirOk && refused && cooling && clientCooldownSeen,
                "first " + first + " |dir|=" + len + "; second within cooldown left " + (refused ? "it unchanged" : "it as " + again)
                    + "; chargeReadyAt " + readyAt + " at time " + OrderIntakeProbe.time(b) + "; client chargeReadyIn seen within 20 ticks=" + clientCooldownSeen + " (now " + CommandNet.Client.chargeReadyIn + ")");
        });
        // Charge toward the arena edge: the end walks back inside
        if (tick == 90) CommandUiChecks.server(c, b -> OrderIntakeProbe.resetCharge(b, id));
        if (tick == 95) OrderIntakeProbe.send("infantry", "charge", "line", 335, cz, -90f);
        if (tick == 105) CommandUiChecks.server(c, b -> {
            Snap a = OrderIntakeProbe.snap(b, id, "infantry");
            boolean ok = a != null && a.order().equals("charge") && OrderIntakeProbe.inArena(a.tx(), a.tz()) && !OrderIntakeProbe.wet(a.tx(), a.tz()) && a.tx() >= 335;
            CommandUiChecks.report("cu-intake-charge-edge", ok, "charge from 335, " + (int) cz + " east 32 blocks (past the 340 limit) stored " + a);
        });
        // Retreat: to the commander, then Follow me
        if (tick == 110) OrderIntakeProbe.send("infantry", "retreat", "line", 0, 0, 0);
        if (tick == 120) CommandUiChecks.server(c, b -> {
            Snap a = OrderIntakeProbe.snap(b, id, "infantry");
            var p = b.server.getPlayerManager().getPlayer(id);
            double off = a == null ? -1 : Math.hypot(a.tx() - p.getX(), a.tz() - p.getZ());
            CommandUiChecks.report("cu-intake-retreat", a != null && a.order().equals("retreat") && a.thenFollow() && off >= 0 && off <= 1,
                "stored " + a + "; player at " + String.format("%.1f, %.1f", p.getX(), p.getZ()) + ", off by " + String.format("%.2f", off));
        });
        // Formation order: hold in a wedge at a dry spot, the yaw kept
        if (tick == 125) OrderIntakeProbe.send("ranged", "hold", "wedge", px + 3, pz, 37.5f);
        if (tick == 135) CommandUiChecks.server(c, b -> {
            Snap a = OrderIntakeProbe.snap(b, id, "ranged");
            boolean dry = OrderIntakeProbe.inArena(px + 3, pz) && !OrderIntakeProbe.wet(px + 3, pz);
            boolean ok = a != null && a.order().equals("hold") && a.formation().equals("wedge") && a.yaw() == 37.5f && Math.abs(a.tx() - (px + 3)) < 0.01 && Math.abs(a.tz() - pz) < 0.01;
            CommandUiChecks.report("cu-intake-formation", ok, "stored " + a + " for hold/wedge at " + String.format("%.1f, %.1f", px + 3, pz) + " (dry spot " + dry + ") yaw 37.5");
        });
        // OUTLINE to teammates only; ACK to issuer and teammates; nothing to the enemy
        if (tick == 140) CommandUiChecks.server(c, b -> {
            int team = OrderIntakeProbe.team(b, id);
            mate = OrderIntakeProbe.addSynthetic(b, team); enemy = OrderIntakeProbe.addSynthetic(b, 1 - team);
            CommandNet.sent.clear();
        });
        if (tick == 145) OrderIntakeProbe.send("infantry", "hold", "block", px, pz + 2, 0f);
        if (tick == 155) CommandUiChecks.server(c, b -> {
            int mateOut = CommandNet.sentTo(mate, CommandNet.OUTLINE), enemyOut = CommandNet.sentTo(enemy, CommandNet.OUTLINE), selfOut = CommandNet.sentTo(id, CommandNet.OUTLINE);
            int mateAck = CommandNet.sentTo(mate, CommandNet.ACK), enemyAck = CommandNet.sentTo(enemy, CommandNet.ACK), selfAck = CommandNet.sentTo(id, CommandNet.ACK);
            OrderIntakeProbe.removeSynthetic(b, mate); OrderIntakeProbe.removeSynthetic(b, enemy);
            CommandUiChecks.report("cu-intake-outline-team", mateOut == 1 && enemyOut == 0 && selfOut == 0 && mateAck == 1 && enemyAck == 0 && selfAck == 1,
                "OUTLINE sends: teammate " + mateOut + ", enemy " + enemyOut + ", issuer " + selfOut + "; ACK sends: teammate " + mateAck + ", enemy " + enemyAck + ", issuer " + selfAck);
        });
        // While respawning, every action is ignored
        if (tick == 160) CommandUiChecks.server(c, b -> {
            OrderIntakeProbe.resetCharge(b, id); OrderIntakeProbe.respawning(b, id, true);
            infBefore = OrderIntakeProbe.snap(b, id, "infantry"); rngBefore = OrderIntakeProbe.snap(b, id, "ranged");
        });
        if (tick == 165) OrderIntakeProbe.send("all", "attack", "line", 0, 0, 0);
        if (tick == 172) OrderIntakeProbe.send("all", "charge", "wedge", cx, cz, -90f);
        if (tick == 179) OrderIntakeProbe.send("all", "retreat", "line", 0, 0, 0);
        if (tick == 186) OrderIntakeProbe.send("all", "hold", "wedge", px, pz, 10f);
        if (tick == 193) OrderIntakeProbe.send("all", "follow", "line", 0, 0, 0);
        if (tick == 200) OrderIntakeProbe.send("all", "formation", "loose", 0, 0, 0);
        if (tick == 210) CommandUiChecks.server(c, b -> {
            Snap inf = OrderIntakeProbe.snap(b, id, "infantry"), rng = OrderIntakeProbe.snap(b, id, "ranged");
            long ready = OrderIntakeProbe.chargeReadyAt(b, id);
            OrderIntakeProbe.respawning(b, id, false);
            boolean same = java.util.Objects.equals(inf, infBefore) && java.util.Objects.equals(rng, rngBefore) && ready == 0;
            CommandUiChecks.report("cu-intake-respawning", same, "six orders while in respawns: infantry " + (java.util.Objects.equals(inf, infBefore) ? "unchanged" : infBefore + " -> " + inf)
                + ", ranged " + (java.util.Objects.equals(rng, rngBefore) ? "unchanged" : rngBefore + " -> " + rng) + ", chargeReadyAt " + ready);
        });
        if (tick == 215) {
            boolean ok = OrderIntakeProbe.loadOrder("move").equals("hold") && OrderIntakeProbe.loadOrder("hold").equals("hold") && OrderIntakeProbe.loadOrder("attack").equals("attack")
                && OrderIntakeProbe.loadFormation("block").equals("block") && OrderIntakeProbe.loadFormation("nonsense").equals("line");
            CommandUiChecks.report("cu-intake-compat", ok, "OrderRules.loadOrder/loadFormation in game: move->" + OrderIntakeProbe.loadOrder("move") + ", nonsense->" + OrderIntakeProbe.loadFormation("nonsense"));
        }
        // Task 3b. One Charge (X, then confirm) with infantry and ranged selected: one packet, both groups charge in the same tick, each from its own
        // centre, and the commander gets one cooldown.
        if (tick == 220) {
            CommandNet.Client.reset();
            CommandUiChecks.server(c, b -> {
                var p = b.server.getPlayerManager().getPlayer(id);
                infC = OrderIntakeProbe.centre(b, id, "infantry", p.getPos()); rngC = OrderIntakeProbe.centre(b, id, "ranged", p.getPos());
                OrderIntakeProbe.resetCharge(b, id); CommandNet.sent.clear();
            });
        }
        if (tick == 224) {
            CommandSelection.select("infantry", false); CommandSelection.select("ranged", true);
            before = OrderIntakeProbe.ordersSent();
            CommandSelection.order("charge", false); CommandMode.confirm();
        }
        if (tick == 234) {
            chargeSends = OrderIntakeProbe.ordersSent() - before;
            CommandUiChecks.server(c, b -> {
                mInf = OrderIntakeProbe.snap(b, id, "infantry"); mRng = OrderIntakeProbe.snap(b, id, "ranged");
                mReady = OrderIntakeProbe.chargeReadyAt(b, id); cooldownsAfterFirst = CommandNet.sentTo(id, CommandNet.COOLDOWN);
                boolean both = charged(mInf) && charged(mRng) && mInf.issued() == mRng.issued();
                CommandUiChecks.report("cu-intake-multi-charge", both && chargeSends == 1 && cooldownsAfterFirst == 1 && mReady > OrderIntakeProbe.time(b),
                    "infantry+ranged, X then confirm: " + chargeSends + " packet(s); infantry " + mInf + " (centre " + fmt(infC) + "), ranged " + mRng + " (centre " + fmt(rngC)
                        + "); COOLDOWN records to the commander " + cooldownsAfterFirst + "; chargeReadyAt " + mReady + " at time " + OrderIntakeProbe.time(b));
            });
        }
        // A second Charge inside the cooldown: the keys refuse it on the client, and a hand-written packet is refused by the server for both groups.
        if (tick == 238) {
            before = OrderIntakeProbe.ordersSent();
            CommandSelection.order("charge", false);
            localRefused = OrderIntakeProbe.ordersSent() == before && CommandSelection.aiming().isEmpty();
            v2open = OrderIntakeProbe.sendMany("charge", "block", 90f, List.of(new OrderIntakeProbe.At("infantry", infC.x, infC.z), new OrderIntakeProbe.At("ranged", rngC.x, rngC.z)));
        }
        if (tick == 248) CommandUiChecks.server(c, b -> {
            Snap inf = OrderIntakeProbe.snap(b, id, "infantry"), rng = OrderIntakeProbe.snap(b, id, "ranged");
            long ready = OrderIntakeProbe.chargeReadyAt(b, id); int cooldowns = CommandNet.sentTo(id, CommandNet.COOLDOWN);
            boolean unchanged = inf != null && inf.equals(mInf) && rng != null && rng.equals(mRng) && ready == mReady;
            CommandUiChecks.report("cu-intake-multi-charge-cooldown", unchanged && localRefused && v2open && cooldowns == 2 && CommandNet.Client.chargeTicksLeft() > 0,
                "second charge within " + (mReady - OrderIntakeProbe.time(b)) + " ticks: key refused locally " + localRefused + "; tactical_v2 open " + v2open + "; infantry "
                    + (java.util.Objects.equals(inf, mInf) ? "unchanged" : "now " + inf) + ", ranged " + (java.util.Objects.equals(rng, mRng) ? "unchanged" : "now " + rng)
                    + "; chargeReadyAt " + ready + " (was " + mReady + "); COOLDOWN records " + cooldowns + " (one use, one refusal); client ticks left " + CommandNet.Client.chargeTicksLeft());
        });
        // Follow me then Attack with two groups selected: one packet each, both groups updated in the same tick.
        if (tick == 252) { CommandSelection.select("infantry", false); CommandSelection.select("ranged", true); before = OrderIntakeProbe.ordersSent(); CommandSelection.order("follow", false); }
        if (tick == 262) {
            followSends = OrderIntakeProbe.ordersSent() - before;
            CommandUiChecks.server(c, b -> { followInf = OrderIntakeProbe.snap(b, id, "infantry"); followRng = OrderIntakeProbe.snap(b, id, "ranged"); });
        }
        if (tick == 264) { before = OrderIntakeProbe.ordersSent(); CommandSelection.order("attack", false); }
        if (tick == 274) {
            attackSends = OrderIntakeProbe.ordersSent() - before;
            CommandUiChecks.server(c, b -> {
                Snap inf = OrderIntakeProbe.snap(b, id, "infantry"), rng = OrderIntakeProbe.snap(b, id, "ranged");
                boolean follow = followInf != null && followInf.order().equals("follow") && followRng != null && followRng.order().equals("follow") && followInf.issued() == followRng.issued();
                boolean attack = inf != null && inf.order().equals("attack") && inf.hunt() && rng != null && rng.order().equals("attack") && rng.hunt() && inf.issued() == rng.issued();
                CommandUiChecks.report("cu-intake-multi-one-packet", follow && attack && followSends == 1 && attackSends == 1,
                    "infantry+ranged: C sent " + followSends + " packet(s), stored infantry " + (followInf == null ? null : followInf.order() + "@" + followInf.issued())
                        + ", ranged " + (followRng == null ? null : followRng.order() + "@" + followRng.issued()) + "; Z sent " + attackSends + " packet(s), stored infantry "
                        + (inf == null ? null : inf.order() + "@" + inf.issued() + " hunt " + inf.hunt()) + ", ranged " + (rng == null ? null : rng.order() + "@" + rng.issued() + " hunt " + rng.hunt()));
            });
        }
        // An old client's single-group tactical_v1 still lands.
        if (tick == 276) v1open = OrderIntakeProbe.sendV1("infantry", "hold", "line", px + 2, pz, 15f);
        if (tick == 286) CommandUiChecks.server(c, b -> {
            Snap a = OrderIntakeProbe.snap(b, id, "infantry");
            boolean ok = v1open && a != null && a.order().equals("hold") && a.formation().equals("line") && a.yaw() == 15f && Math.abs(a.tx() - (px + 2)) < 0.01 && Math.abs(a.tz() - pz) < 0.01;
            CommandUiChecks.report("cu-intake-v1-compat", ok, "tactical_v1 open " + v1open + ", infantry hold/line at " + String.format("%.1f, %.1f", px + 2, pz) + " yaw 15 stored " + a);
        });
        // A custom:<n> id in the list is skipped (Task 12's) and the rest of the order still lands.
        if (tick == 288) customOpen = OrderIntakeProbe.sendMany("retreat", "line", 0f, List.of(new OrderIntakeProbe.At("custom:1", 0, 0), new OrderIntakeProbe.At("ranged", 0, 0)));
        if (tick == 298) CommandUiChecks.server(c, b -> {
            Snap rng = OrderIntakeProbe.snap(b, id, "ranged");
            CommandUiChecks.report("cu-intake-multi-custom-ignored", customOpen && rng != null && rng.order().equals("retreat"), "tactical_v2 open " + customOpen + "; [custom:1, ranged] retreat stored ranged " + rng);
        });
        if (tick == 300) { CommandSelection.select("all", false); CommandUiChecks.finish(c); }
    }

    static boolean charged(Snap a) { return a != null && a.order().equals("charge") && a.chargeDir() != null && Math.abs(a.chargeDir().length() - 1) <= 0.01; }
    static String fmt(Vec3d v) { return v == null ? "null" : String.format("%.1f, %.1f", v.x, v.z); }

    @Override public void cleanup(MinecraftClient c) {
        CommandSelection.select("all", false);
        if (id == null) return;
        UUID m = mate, e = enemy;
        CommandUiChecks.server(c, b -> {
            OrderIntakeProbe.respawning(b, id, false);
            if (m != null) OrderIntakeProbe.removeSynthetic(b, m);
            if (e != null) OrderIntakeProbe.removeSynthetic(b, e);
        });
    }
}
