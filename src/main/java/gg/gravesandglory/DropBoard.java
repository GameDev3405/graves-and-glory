package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * The drop board on the server (design 2026-11-14): takes a roll through the altar. The commands are drop:roll:<house>,
 * drop:forge:<slot>, drop:aim:<x>, drop:place:<x> and drop:skip. A roll draws its slot, then is paid, drawn and saved by the same code
 * as a roll with no board (Campaign.roll, Campaign.gear); only the showing of its result waits for the coin. Where there is no altar
 * to see (no stage, an old client, a commander outside the camp) the roll is the plain one.
 *
 * <p>drop_v1, server to client, a byte kind then: BEGIN id, roller uuid, name, what, aim ticks, x; AIM id, x; PATH id, roller uuid,
 * name, what, x, the DropSim path bytes, the result card; END id.
 */
final class DropBoard {
    static final byte BEGIN = 0, AIM = 1, PATH = 2, END = 3;
    final Campaign c;
    final DropFlow flow = new DropFlow();
    /** Drops in the pegs whose landing may be told to the whole camp: drop id to the tick it lands. */
    private final Map<Integer, Long> lands = new HashMap<>();

    DropBoard(Campaign c) { this.c = c; }

    static boolean handles(String cmd) { return cmd.startsWith("drop:"); }

    /** Whether this commander's roll goes through the altar: it stands, his client knows the packets and he is in the camp. */
    boolean available(ServerPlayerEntity p) {
        return p.getWorld() == c.b.world && DropStage.built(c.b.world) && ServerPlayNetworking.canSend(p, GravesAndGlory.DROP) && CampWorld.inCamp(p.getX(), p.getY(), p.getZ());
    }

    void command(ServerPlayerEntity p, Profile pr, String cmd) {
        UUID id = p.getUuid();
        if (cmd.startsWith("drop:roll:")) { c.roll(p, pr, cmd.substring(10), available(p) ? DropMath.drawSlot(c.rollRng) : -1); return; }
        if (cmd.startsWith("drop:forge:")) { c.gear(p, pr, "forge:roll:" + cmd.substring(11), available(p) ? DropMath.drawSlot(c.rollRng) : -1); return; }
        if (cmd.startsWith("drop:aim:")) {
            var d = flow.of(id);
            if (d != null && flow.aim(id, number(cmd.substring(9)))) { var buf = PacketByteBufs.create(); buf.writeByte(AIM); buf.writeVarInt(d.id); buf.writeFloat((float) d.x); send(d, buf, false); }
            return;
        }
        if (cmd.startsWith("drop:place:")) { var d = flow.release(id, number(cmd.substring(11))); if (d != null) search(d); return; }
        if (cmd.equals("drop:skip")) { end(flow.end(id)); }
    }

    private static double number(String s) { try { return Double.parseDouble(s); } catch (NumberFormatException e) { return Double.NaN; } }

    /** A paid and saved roll starts its drop: card and notice are shown when it ends. */
    void begin(ServerPlayerEntity p, String what, int slot, JsonObject card, String notice) {
        var list = flow.begin(p.getUuid(), p.getGameProfile().getName(), what, slot, card.toString(), notice, c.b.time);
        for (int i = 0; i < list.size() - 1; i++) end(list.get(i));
        var d = list.get(list.size() - 1);
        var buf = PacketByteBufs.create();
        buf.writeByte(BEGIN); buf.writeVarInt(d.id); buf.writeUuid(d.who); buf.writeString(d.name, 64); buf.writeString(d.what, 16);
        buf.writeVarInt((int) (DropFlow.aimTicks() - 10)); buf.writeFloat((float) d.x);
        send(d, buf, true);
    }

    void tick() {
        long now = c.b.time;
        if (flow.active().isEmpty()) return;
        for (var d : flow.overdue(now)) search(d);
        for (var d : flow.finished(now)) publish(d);
        // a match that seats the roller cuts his drop; a roller who has gone is ended by left
        for (var d : new ArrayList<>(flow.active())) {
            var sq = c.b.squads.get(d.who);
            if (c.b.running && sq != null && sq.seated) end(flow.end(d.who));
        }
        for (var d : flow.active()) {
            Long at = lands.get(d.id);
            if (at != null && now >= at) { lands.remove(d.id); tell(d); }
        }
    }

    /** The roller left: his drop ends for the watchers; the result was saved when he paid. */
    void left(UUID who) { end(flow.end(who)); }

    /** Finds the coin's bounces off the server thread (the search can take some tens of milliseconds) and starts the fall when they are found. */
    private void search(DropFlow.Drop d) {
        long seed = c.rollRng.nextLong();
        double x = d.x; int slot = d.slot;
        CompletableFuture.supplyAsync(() -> DropSim.find(x, slot, seed)).whenComplete((path, error) -> c.b.server.execute(() -> {
            if (error != null || path == null) { System.out.println("[Graves and Glory] drop board: no path, the drop is ended: " + error); end(flow.end(d.who)); return; }
            if (!flow.falling(d.who, d.id, path, c.b.time)) return;
            lands.put(d.id, c.b.time + (long) Math.ceil(path.seconds() * 20));
            var buf = PacketByteBufs.create();
            buf.writeByte(PATH); buf.writeVarInt(d.id); buf.writeUuid(d.who); buf.writeString(d.name, 64); buf.writeString(d.what, 16); buf.writeFloat((float) d.x);
            buf.writeByteArray(DropSim.encode(path)); buf.writeString(d.card, 4096);
            send(d, buf, true);
        }));
    }

    /** Ends a drop before its time: the watchers and the roller are told, and its card is shown. */
    private void end(DropFlow.Drop d) {
        if (d == null) return;
        var buf = PacketByteBufs.create(); buf.writeByte(END); buf.writeVarInt(d.id);
        send(d, buf, true);
        publish(d);
    }

    /** The drop is over: the War Table gets its result card and the notice, as after a roll with no board. */
    private void publish(DropFlow.Drop d) {
        lands.remove(d.id);
        JsonObject card = JsonParser.parseString(d.card).getAsJsonObject();
        if (d.what.equals("unit")) c.lastRolls.put(d.who, card); else c.forgeCard(d.who, card);
        if (!d.notice.isEmpty()) c.notice(d.who, d.notice);
    }

    /** An x10, or a Legendary piece, is told to everyone in the camp as the coin lands. */
    private void tell(DropFlow.Drop d) {
        JsonObject card = JsonParser.parseString(d.card).getAsJsonObject();
        boolean legendary = card.has("rarity") && card.get("rarity").getAsString().equals("legendary");
        int m = DropMath.multiplier(d.slot);
        if (m < DropMath.multiplier(0) && !legendary) return;
        String line = legendary ? d.name + " drew a Legendary " + Gear.item(card.get("item").getAsString()).name() + " on the altar" : d.name + " struck x" + m + " on the altar";
        for (var p : c.b.server.getPlayerManager().getPlayerList())
            if (p.getWorld() == c.b.world && CampWorld.inCamp(p.getX(), p.getY(), p.getZ())) p.sendMessage(Text.literal(line).formatted(Formatting.GOLD), false);
    }

    /** To the roller (when asked) and everyone near the altar who can read it. */
    private void send(DropFlow.Drop d, net.minecraft.network.PacketByteBuf buf, boolean roller) {
        for (var p : c.b.server.getPlayerManager().getPlayerList()) {
            boolean his = p.getUuid().equals(d.who);
            if (his ? !roller : p.getWorld() != c.b.world || !DropLayout.watches(p.getX(), p.getZ())) continue;
            if (!ServerPlayNetworking.canSend(p, GravesAndGlory.DROP)) continue;
            ServerPlayNetworking.send(p, GravesAndGlory.DROP, new net.minecraft.network.PacketByteBuf(buf.copy()));
        }
        buf.release();
    }
}
