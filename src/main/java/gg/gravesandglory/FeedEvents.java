package gg.gravesandglory;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

/** The server half of the battle event feed (UI spec 4.5): who is told what, as feed_v1 lines (kind, the event's team, text).
 *  A point taken goes to every player; a commander falling or returning to the seated players of his team but him; a last stand
 *  and a deploy to the army's owner. Losses, kills and deploys are counted per recipient, kind and troop and go out as one
 *  line once their bucket is BUCKET_TICKS old ("3 Dawn Knights lost"). Bots are told nothing. Server thread only. */
public final class FeedEvents {
    private FeedEvents() {}

    /** A bucket's age when its line goes out: what one troop lost, killed or deployed within 2 s makes one line. */
    static final int BUCKET_TICKS = 40;
    /** The longest text a feed line carries. */
    static final int MAX_TEXT = 120;
    /** Feed lines sent since the server started, for checks. */
    public static int sent;

    private record Key(UUID to, String kind, String troop) {}
    private static final class Bucket {
        final long born; final int team; final String name; int n;
        Bucket(long born, int team, String name) { this.born = born; this.team = team; this.name = name; }
    }
    private static final Map<Key, Bucket> pending = new LinkedHashMap<>();

    /** A point changed hands: "Dawn took Mill Hamlet", to every player. */
    public static void point(GravesAndGlory.Battle b, GravesAndGlory.Capture c, int team) {
        String text = FeedLines.pointTaken(FeedLines.teamShort(Spec.FACTIONS[team].name()), c.spec.name());
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) send(p, "point", team, text);
    }

    /** A troop died: a loss for its owner, and a kill for the killer's owner when the killer fought for the other side. */
    public static void died(GravesAndGlory.Battle b, GravesAndGlory.Unit victim, GravesAndGlory.Squad killer) {
        var s = victim.squad;
        count(b, s, "loss", victim.spec, s.team);
        if (killer != null && killer.team != s.team) count(b, killer, "kill", victim.spec, killer.team);
    }

    /** A commander fell (back false) or is back in the field: his banner, to the seated players of his team but him. */
    public static void commander(GravesAndGlory.Battle b, ServerPlayerEntity p, boolean back) {
        var own = b.squads.get(p.getUuid());
        if (own == null || !own.seated) return;
        String name = p.getName().getString(), text = back ? FeedLines.bannerUp(name) : FeedLines.bannerDown(name);
        for (ServerPlayerEntity other : b.server.getPlayerManager().getPlayerList()) {
            if (other == p) continue;
            var s = b.squads.get(other.getUuid());
            if (s != null && s.seated && s.team == own.team) send(other, back ? "up" : "down", own.team, text);
        }
    }

    /** The lines of Morale.lastStandLines already sent or passed over. The list only grows within a round and Morale.startRound empties
     *  it, so a list shorter than this count means a new round began. */
    private static int lastStandSeen;

    /** A group of an army turned to its last stand: "Infantry make a last stand", to the army's owner. Morale throttles the lines
     *  (one per group per combatDiscipline.lastStandFeedSeconds, a bot's army none), so each one in its list goes out as it is. */
    static void lastStand(GravesAndGlory.Battle b) {
        var lines = Morale.lastStandLines;
        if (lines.size() < lastStandSeen) lastStandSeen = 0;
        for (; lastStandSeen < lines.size(); lastStandSeen++) {
            var line = lines.get(lastStandSeen);
            var s = b.squads.get(line.owner());
            var p = b.server.getPlayerManager().getPlayer(line.owner());
            if (s == null || s.bot || p == null) continue;
            String label = line.group();
            for (var row : Spec.GROUPS) if (row.id().equals(line.group())) label = row.label();
            send(p, "laststand", s.team, FeedLines.lastStand(label));
        }
    }

    /** A trained troop took the field: counted into the owner's "Dawn Knights deploying" line. */
    public static void deployed(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.TroopsRow troop) {
        count(b, s, "deploy", troop, s.team);
    }

    /** Each server tick: every bucket BUCKET_TICKS old goes out as its line, and each new last-stand line of Morale's. Outside a round
     *  nothing waits. */
    public static void tick(GravesAndGlory.Battle b) {
        if (!b.running) { pending.clear(); return; }
        lastStand(b);
        for (var it = pending.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Key k = e.getKey(); Bucket bucket = e.getValue();
            if (b.time - bucket.born < BUCKET_TICKS) continue;
            it.remove();
            var p = b.server.getPlayerManager().getPlayer(k.to());
            if (p != null) send(p, k.kind(), bucket.team, k.kind().equals("deploy") ? FeedLines.deploying(bucket.n, bucket.name) : FeedLines.lost(bucket.n, bucket.name));
        }
    }

    /** A new round or battlefield: nothing counted so far goes out, and no last-stand line recorded so far. */
    public static void clear() { pending.clear(); lastStandSeen = Morale.lastStandLines.size(); }

    static void count(GravesAndGlory.Battle b, GravesAndGlory.Squad to, String kind, Spec.TroopsRow troop, int team) {
        if (to == null || to.bot) return;
        pending.computeIfAbsent(new Key(to.owner, kind, troop.id()), k -> new Bucket(b.time, team, troop.name())).n++;
    }

    static void send(ServerPlayerEntity p, String kind, int team, String text) {
        if (!ServerPlayNetworking.canSend(p, GravesAndGlory.FEED)) return;
        var buf = PacketByteBufs.create();
        buf.writeString(kind);
        buf.writeInt(team);
        buf.writeString(text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text);
        ServerPlayNetworking.send(p, GravesAndGlory.FEED, buf);
        sent++;
    }
}
