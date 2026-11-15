package gg.gravesandglory.net;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Port P1.3 codec check (lane A): every Graves and Glory payload type round-trips through its codec, byte for byte, without a game.
 * For each of the 26 types: an empty body and random bodies (1 byte to 64 KiB) encode then decode to an equal body (Arrays.equals:
 * a record of a byte[] compares by reference), the encoded bytes are the body itself (D5: the 1.20.1 wire layout), the codec reads
 * every byte, of() copies without moving the caller's reader index, and buf() gives a fresh reader each call. Also: the 26 ids
 * are distinct per direction and phase, and every id is in the gravesandglory namespace. Never shipped; run with
 * work/port/lane-a/run_payload_check.py. Not a network test: the in-game dedicated-server exchange is a separate gate.
 */
public final class PayloadRoundTripCheck {
    static int checks;
    static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); checks++; }

    record Type<T extends CustomPayload>(String name, String side, CustomPayload.Id<T> id, PacketCodec<PacketByteBuf, T> codec,
                                         Function<PacketByteBuf, T> of, Function<T, byte[]> body, Function<T, PacketByteBuf> buf) {}

    static final List<Type<?>> TYPES = List.of(
        new Type<>("camp", "playS2C", Payloads.CampPayload.ID, Payloads.CampPayload.CODEC, Payloads.CampPayload::of, Payloads.CampPayload::body, Payloads.CampPayload::buf),
        new Type<>("command", "playS2C", Payloads.CommandPayload.ID, Payloads.CommandPayload.CODEC, Payloads.CommandPayload::of, Payloads.CommandPayload::body, Payloads.CommandPayload::buf),
        new Type<>("impact", "playS2C", Payloads.ImpactPayload.ID, Payloads.ImpactPayload.CODEC, Payloads.ImpactPayload::of, Payloads.ImpactPayload::body, Payloads.ImpactPayload::buf),
        new Type<>("mapGrid", "playS2C", Payloads.MapGridPayload.ID, Payloads.MapGridPayload.CODEC, Payloads.MapGridPayload::of, Payloads.MapGridPayload::body, Payloads.MapGridPayload::buf),
        new Type<>("mapState", "playS2C", Payloads.MapStatePayload.ID, Payloads.MapStatePayload.CODEC, Payloads.MapStatePayload::of, Payloads.MapStatePayload::body, Payloads.MapStatePayload::buf),
        new Type<>("state", "playS2C", Payloads.StatePayload.ID, Payloads.StatePayload.CODEC, Payloads.StatePayload::of, Payloads.StatePayload::body, Payloads.StatePayload::buf),
        new Type<>("openBarracks", "playS2C", Payloads.OpenBarracksPayload.ID, Payloads.OpenBarracksPayload.CODEC, Payloads.OpenBarracksPayload::of, Payloads.OpenBarracksPayload::body, Payloads.OpenBarracksPayload::buf),
        new Type<>("party", "playS2C", Payloads.PartyPayload.ID, Payloads.PartyPayload.CODEC, Payloads.PartyPayload::of, Payloads.PartyPayload::body, Payloads.PartyPayload::buf),
        new Type<>("endingBegin", "playS2C", Payloads.EndingBeginPayload.ID, Payloads.EndingBeginPayload.CODEC, Payloads.EndingBeginPayload::of, Payloads.EndingBeginPayload::body, Payloads.EndingBeginPayload::buf),
        new Type<>("feed", "playS2C", Payloads.FeedPayload.ID, Payloads.FeedPayload.CODEC, Payloads.FeedPayload::of, Payloads.FeedPayload::body, Payloads.FeedPayload::buf),
        new Type<>("gear", "playS2C", Payloads.GearPayload.ID, Payloads.GearPayload.CODEC, Payloads.GearPayload::of, Payloads.GearPayload::body, Payloads.GearPayload::buf),
        new Type<>("relics", "playS2C", Payloads.RelicsPayload.ID, Payloads.RelicsPayload.CODEC, Payloads.RelicsPayload::of, Payloads.RelicsPayload::body, Payloads.RelicsPayload::buf),
        new Type<>("attack", "playS2C", Payloads.AttackPayload.ID, Payloads.AttackPayload.CODEC, Payloads.AttackPayload::of, Payloads.AttackPayload::body, Payloads.AttackPayload::buf),
        new Type<>("hit", "playS2C", Payloads.HitPayload.ID, Payloads.HitPayload.CODEC, Payloads.HitPayload::of, Payloads.HitPayload::body, Payloads.HitPayload::buf),
        new Type<>("stance", "playS2C", Payloads.StancePayload.ID, Payloads.StancePayload.CODEC, Payloads.StancePayload::of, Payloads.StancePayload::body, Payloads.StancePayload::buf),
        new Type<>("rise", "playS2C", Payloads.RisePayload.ID, Payloads.RisePayload.CODEC, Payloads.RisePayload::of, Payloads.RisePayload::body, Payloads.RisePayload::buf),
        new Type<>("rank", "playS2C", Payloads.RankPayload.ID, Payloads.RankPayload.CODEC, Payloads.RankPayload::of, Payloads.RankPayload::body, Payloads.RankPayload::buf),
        new Type<>("order", "playC2S", Payloads.OrderPayload.ID, Payloads.OrderPayload.CODEC, Payloads.OrderPayload::of, Payloads.OrderPayload::body, Payloads.OrderPayload::buf),
        new Type<>("profile", "playC2S", Payloads.ProfilePayload.ID, Payloads.ProfilePayload.CODEC, Payloads.ProfilePayload::of, Payloads.ProfilePayload::body, Payloads.ProfilePayload::buf),
        new Type<>("camera", "playC2S", Payloads.CameraPayload.ID, Payloads.CameraPayload.CODEC, Payloads.CameraPayload::of, Payloads.CameraPayload::body, Payloads.CameraPayload::buf),
        new Type<>("ping", "playC2S", Payloads.PingPayload.ID, Payloads.PingPayload.CODEC, Payloads.PingPayload::of, Payloads.PingPayload::body, Payloads.PingPayload::buf),
        new Type<>("plan", "playC2S", Payloads.PlanPayload.ID, Payloads.PlanPayload.CODEC, Payloads.PlanPayload::of, Payloads.PlanPayload::body, Payloads.PlanPayload::buf),
        new Type<>("tactical", "playC2S", Payloads.TacticalPayload.ID, Payloads.TacticalPayload.CODEC, Payloads.TacticalPayload::of, Payloads.TacticalPayload::body, Payloads.TacticalPayload::buf),
        new Type<>("tacticalV2", "playC2S", Payloads.TacticalV2Payload.ID, Payloads.TacticalV2Payload.CODEC, Payloads.TacticalV2Payload::of, Payloads.TacticalV2Payload::body, Payloads.TacticalV2Payload::buf),
        new Type<>("releaseCheckRequest", "configS2C", Payloads.ReleaseCheckRequestPayload.ID, Payloads.ReleaseCheckRequestPayload.CODEC, Payloads.ReleaseCheckRequestPayload::of, Payloads.ReleaseCheckRequestPayload::body, Payloads.ReleaseCheckRequestPayload::buf),
        new Type<>("releaseCheckReply", "configC2S", Payloads.ReleaseCheckReplyPayload.ID, Payloads.ReleaseCheckReplyPayload.CODEC, Payloads.ReleaseCheckReplyPayload::of, Payloads.ReleaseCheckReplyPayload::body, Payloads.ReleaseCheckReplyPayload::buf));

    static <T extends CustomPayload> void roundTrip(Type<T> t, byte[] body) {
        // a send site's buffer, with a byte already read to prove of() copies from the reader index and leaves it alone
        PacketByteBuf site = new PacketByteBuf(Unpooled.buffer());
        site.writeByte(0x7F); site.writeBytes(body); site.readByte();
        T payload = t.of().apply(site);
        check(site.readerIndex() == 1 && site.readableBytes() == body.length, t.name() + ": of() moved the caller's reader index");
        check(Arrays.equals(t.body().apply(payload), body), t.name() + ": of() body differs");
        check(payload.getId() == t.id(), t.name() + ": getId is not ID");

        PacketByteBuf wire = new PacketByteBuf(Unpooled.buffer());
        t.codec().encode(wire, payload);
        byte[] bytes = new byte[wire.readableBytes()];
        wire.getBytes(wire.readerIndex(), bytes);
        check(Arrays.equals(bytes, body), t.name() + ": encoded bytes are not the 1.20.1 body (" + body.length + " bytes)");

        T back = t.codec().decode(wire);
        check(!wire.isReadable(), t.name() + ": decode left bytes behind");
        check(Arrays.equals(t.body().apply(back), body), t.name() + ": decode(encode(x)) != x (" + body.length + " bytes)");

        PacketByteBuf r1 = t.buf().apply(back), r2 = t.buf().apply(back);
        if (body.length > 0) r1.readByte();
        check(r2.readerIndex() == 0 && r2.readableBytes() == body.length, t.name() + ": buf() readers are not independent");
    }

    public static void main(String[] args) {
        check(TYPES.size() == 26, "expected 26 payload types, found " + TYPES.size());
        Set<String> seen = new HashSet<>();
        for (var t : TYPES) {
            check(t.id().id().getNamespace().equals("gravesandglory"), t.name() + ": id outside the gravesandglory namespace");
            check(seen.add(t.side() + " " + t.id().id()), t.name() + ": id " + t.id().id() + " registered twice in " + t.side());
        }
        Random random = new Random(1211);
        List<byte[]> bodies = new ArrayList<>(List.of(new byte[0], new byte[] {0}, new byte[] {(byte) 0xFF, 0, 1}));
        for (int n : new int[] {7, 255, 4096, 32767, 65536}) { byte[] b = new byte[n]; random.nextBytes(b); bodies.add(b); }
        for (var t : TYPES) for (byte[] b : bodies) roundTrip(t, b);
        // a real body: the release_check reply layout (varInt protocol, string version, string build) survives the trip and reads back
        PacketByteBuf reply = new PacketByteBuf(Unpooled.buffer());
        reply.writeVarInt(1); reply.writeString("0.5.1", 64); reply.writeString("a".repeat(64), 64);
        var p = Payloads.ReleaseCheckReplyPayload.of(reply);
        PacketByteBuf wire = new PacketByteBuf(Unpooled.buffer());
        Payloads.ReleaseCheckReplyPayload.CODEC.encode(wire, p);
        PacketByteBuf read = Payloads.ReleaseCheckReplyPayload.CODEC.decode(wire).buf();
        check(read.readVarInt() == 1 && read.readString(64).equals("0.5.1") && read.readString(64).equals("a".repeat(64)) && !read.isReadable(),
            "release_check reply fields do not read back");
        System.out.println("PayloadRoundTripCheck: " + TYPES.size() + " types, " + checks + " checks passed");
    }
}
