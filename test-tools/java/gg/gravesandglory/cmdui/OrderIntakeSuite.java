package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandNet;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.OrderIntakeProbe;
import gg.gravesandglory.OrderIntakeProbe.Snap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

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
        if (tick == 220) CommandUiChecks.finish(c);
    }

    @Override public void cleanup(MinecraftClient c) {
        if (id == null) return;
        UUID m = mate, e = enemy;
        CommandUiChecks.server(c, b -> {
            OrderIntakeProbe.respawning(b, id, false);
            if (m != null) OrderIntakeProbe.removeSynthetic(b, m);
            if (e != null) OrderIntakeProbe.removeSynthetic(b, e);
        });
    }
}
