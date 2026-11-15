package gg.gravesandglory.net;

import gg.gravesandglory.CommandNet;
import gg.gravesandglory.GravesAndGlory;
import gg.gravesandglory.MotionEvents;
import gg.gravesandglory.RankSync;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Every Graves and Glory play channel as a 1.21.1 CustomPayload (24: 17 server to client, 7 client to server). Channel ids are
 * unchanged and stay defined where 1.20.1 defined them (GravesAndGlory, CommandNet, MotionEvents, RankSync). Each body is the
 * channel's 1.20.1 byte layout (RawBody), so the send and receive sites keep their PacketByteBuf code:
 * <pre>
 *   send:    ServerPlayNetworking.send(p, Payloads.FeedPayload.of(buf));
 *   receive: ServerPlayNetworking.registerGlobalReceiver(Payloads.OrderPayload.ID, (payload, ctx) -> { var buf = payload.buf(); ... });
 * </pre>
 * Fabric runs 1.21 play handlers on the game thread (server or client), not the network thread. Two configuration-phase payloads
 * (release_check_v2, VersionGate) close the file; their handlers run on netty threads. The login query release_check_v1 is not a
 * payload (VersionGate).
 */
public final class Payloads {
    private Payloads() {}

    /** Registers all 26 types (24 play, 2 configuration), once, from the common initializer (GravesAndGlory.onInitialize): both sides need them. */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(CampPayload.ID, CampPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CommandPayload.ID, CommandPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ImpactPayload.ID, ImpactPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MapGridPayload.ID, MapGridPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MapStatePayload.ID, MapStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatePayload.ID, StatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenBarracksPayload.ID, OpenBarracksPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyPayload.ID, PartyPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EndingBeginPayload.ID, EndingBeginPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FeedPayload.ID, FeedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GearPayload.ID, GearPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RelicsPayload.ID, RelicsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AttackPayload.ID, AttackPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HitPayload.ID, HitPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StancePayload.ID, StancePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RisePayload.ID, RisePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RankPayload.ID, RankPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OrderPayload.ID, OrderPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ProfilePayload.ID, ProfilePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CameraPayload.ID, CameraPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PingPayload.ID, PingPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PlanPayload.ID, PlanPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TacticalPayload.ID, TacticalPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TacticalV2Payload.ID, TacticalV2Payload.CODEC);
        PayloadTypeRegistry.configurationS2C().register(ReleaseCheckRequestPayload.ID, ReleaseCheckRequestPayload.CODEC);
        PayloadTypeRegistry.configurationC2S().register(ReleaseCheckReplyPayload.ID, ReleaseCheckReplyPayload.CODEC);
    }

    // Server to client.

    /** S2C GravesAndGlory.CAMP: camp_station. */
    public record CampPayload(byte[] body) implements CustomPayload {
        public static final Id<CampPayload> ID = new Id<>(GravesAndGlory.CAMP);
        public static final PacketCodec<PacketByteBuf, CampPayload> CODEC = RawBody.codec(CampPayload::new, CampPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static CampPayload of(PacketByteBuf buf) { return new CampPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<CampPayload> getId() { return ID; }
    }
    /** S2C CommandNet.COMMAND: command_v1. */
    public record CommandPayload(byte[] body) implements CustomPayload {
        public static final Id<CommandPayload> ID = new Id<>(CommandNet.COMMAND);
        public static final PacketCodec<PacketByteBuf, CommandPayload> CODEC = RawBody.codec(CommandPayload::new, CommandPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static CommandPayload of(PacketByteBuf buf) { return new CommandPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<CommandPayload> getId() { return ID; }
    }
    /** S2C GravesAndGlory.IMPACT: impact_v1. */
    public record ImpactPayload(byte[] body) implements CustomPayload {
        public static final Id<ImpactPayload> ID = new Id<>(GravesAndGlory.IMPACT);
        public static final PacketCodec<PacketByteBuf, ImpactPayload> CODEC = RawBody.codec(ImpactPayload::new, ImpactPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static ImpactPayload of(PacketByteBuf buf) { return new ImpactPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<ImpactPayload> getId() { return ID; }
    }
    /** S2C GravesAndGlory.MAP_GRID: battlemap_grid_v1. */
    public record MapGridPayload(byte[] body) implements CustomPayload {
        public static final Id<MapGridPayload> ID = new Id<>(GravesAndGlory.MAP_GRID);
        public static final PacketCodec<PacketByteBuf, MapGridPayload> CODEC = RawBody.codec(MapGridPayload::new, MapGridPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static MapGridPayload of(PacketByteBuf buf) { return new MapGridPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<MapGridPayload> getId() { return ID; }
    }
    /** S2C GravesAndGlory.MAP_STATE: battlemap_v1. */
    public record MapStatePayload(byte[] body) implements CustomPayload {
        public static final Id<MapStatePayload> ID = new Id<>(GravesAndGlory.MAP_STATE);
        public static final PacketCodec<PacketByteBuf, MapStatePayload> CODEC = RawBody.codec(MapStatePayload::new, MapStatePayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static MapStatePayload of(PacketByteBuf buf) { return new MapStatePayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<MapStatePayload> getId() { return ID; }
    }
    /** S2C GravesAndGlory.STATE: state_v10. */
    public record StatePayload(byte[] body) implements CustomPayload {
        public static final Id<StatePayload> ID = new Id<>(GravesAndGlory.STATE);
        public static final PacketCodec<PacketByteBuf, StatePayload> CODEC = RawBody.codec(StatePayload::new, StatePayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static StatePayload of(PacketByteBuf buf) { return new StatePayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<StatePayload> getId() { return ID; }
    }
    /** S2C GravesAndGlory.OPEN_BARRACKS: barracks_v1. */
    public record OpenBarracksPayload(byte[] body) implements CustomPayload {
        public static final Id<OpenBarracksPayload> ID = new Id<>(GravesAndGlory.OPEN_BARRACKS);
        public static final PacketCodec<PacketByteBuf, OpenBarracksPayload> CODEC = RawBody.codec(OpenBarracksPayload::new, OpenBarracksPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static OpenBarracksPayload of(PacketByteBuf buf) { return new OpenBarracksPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<OpenBarracksPayload> getId() { return ID; }
    }
    /** S2C GravesAndGlory.PARTY: party_v1 (no server sender on main; the client still opens the War Table for it). */
    public record PartyPayload(byte[] body) implements CustomPayload {
        public static final Id<PartyPayload> ID = new Id<>(GravesAndGlory.PARTY);
        public static final PacketCodec<PacketByteBuf, PartyPayload> CODEC = RawBody.codec(PartyPayload::new, PartyPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static PartyPayload of(PacketByteBuf buf) { return new PartyPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<PartyPayload> getId() { return ID; }
    }
    /** S2C GravesAndGlory.ENDING_BEGIN: ending_begin_v1. */
    public record EndingBeginPayload(byte[] body) implements CustomPayload {
        public static final Id<EndingBeginPayload> ID = new Id<>(GravesAndGlory.ENDING_BEGIN);
        public static final PacketCodec<PacketByteBuf, EndingBeginPayload> CODEC = RawBody.codec(EndingBeginPayload::new, EndingBeginPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static EndingBeginPayload of(PacketByteBuf buf) { return new EndingBeginPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<EndingBeginPayload> getId() { return ID; }
    }
    /** S2C GravesAndGlory.FEED: feed_v1. */
    public record FeedPayload(byte[] body) implements CustomPayload {
        public static final Id<FeedPayload> ID = new Id<>(GravesAndGlory.FEED);
        public static final PacketCodec<PacketByteBuf, FeedPayload> CODEC = RawBody.codec(FeedPayload::new, FeedPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static FeedPayload of(PacketByteBuf buf) { return new FeedPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<FeedPayload> getId() { return ID; }
    }
    /** S2C GravesAndGlory.GEAR: gear_v1. */
    public record GearPayload(byte[] body) implements CustomPayload {
        public static final Id<GearPayload> ID = new Id<>(GravesAndGlory.GEAR);
        public static final PacketCodec<PacketByteBuf, GearPayload> CODEC = RawBody.codec(GearPayload::new, GearPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static GearPayload of(PacketByteBuf buf) { return new GearPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<GearPayload> getId() { return ID; }
    }
    /** S2C GravesAndGlory.RELICS: relics_v1. */
    public record RelicsPayload(byte[] body) implements CustomPayload {
        public static final Id<RelicsPayload> ID = new Id<>(GravesAndGlory.RELICS);
        public static final PacketCodec<PacketByteBuf, RelicsPayload> CODEC = RawBody.codec(RelicsPayload::new, RelicsPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static RelicsPayload of(PacketByteBuf buf) { return new RelicsPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<RelicsPayload> getId() { return ID; }
    }
    /** S2C MotionEvents.ATTACK: attack_v1. */
    public record AttackPayload(byte[] body) implements CustomPayload {
        public static final Id<AttackPayload> ID = new Id<>(MotionEvents.ATTACK);
        public static final PacketCodec<PacketByteBuf, AttackPayload> CODEC = RawBody.codec(AttackPayload::new, AttackPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static AttackPayload of(PacketByteBuf buf) { return new AttackPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<AttackPayload> getId() { return ID; }
    }
    /** S2C MotionEvents.HIT: hit_v1. */
    public record HitPayload(byte[] body) implements CustomPayload {
        public static final Id<HitPayload> ID = new Id<>(MotionEvents.HIT);
        public static final PacketCodec<PacketByteBuf, HitPayload> CODEC = RawBody.codec(HitPayload::new, HitPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static HitPayload of(PacketByteBuf buf) { return new HitPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<HitPayload> getId() { return ID; }
    }
    /** S2C MotionEvents.STANCE: stance_v2. */
    public record StancePayload(byte[] body) implements CustomPayload {
        public static final Id<StancePayload> ID = new Id<>(MotionEvents.STANCE);
        public static final PacketCodec<PacketByteBuf, StancePayload> CODEC = RawBody.codec(StancePayload::new, StancePayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static StancePayload of(PacketByteBuf buf) { return new StancePayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<StancePayload> getId() { return ID; }
    }
    /** S2C MotionEvents.RISE: rise_v1. */
    public record RisePayload(byte[] body) implements CustomPayload {
        public static final Id<RisePayload> ID = new Id<>(MotionEvents.RISE);
        public static final PacketCodec<PacketByteBuf, RisePayload> CODEC = RawBody.codec(RisePayload::new, RisePayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static RisePayload of(PacketByteBuf buf) { return new RisePayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<RisePayload> getId() { return ID; }
    }
    /** S2C RankSync.CHANNEL: rank_v1. */
    public record RankPayload(byte[] body) implements CustomPayload {
        public static final Id<RankPayload> ID = new Id<>(RankSync.CHANNEL);
        public static final PacketCodec<PacketByteBuf, RankPayload> CODEC = RawBody.codec(RankPayload::new, RankPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static RankPayload of(PacketByteBuf buf) { return new RankPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<RankPayload> getId() { return ID; }
    }

    // Client to server.

    /** C2S GravesAndGlory.ORDER: order_v4. */
    public record OrderPayload(byte[] body) implements CustomPayload {
        public static final Id<OrderPayload> ID = new Id<>(GravesAndGlory.ORDER);
        public static final PacketCodec<PacketByteBuf, OrderPayload> CODEC = RawBody.codec(OrderPayload::new, OrderPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static OrderPayload of(PacketByteBuf buf) { return new OrderPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<OrderPayload> getId() { return ID; }
    }
    /** C2S GravesAndGlory.PROFILE: profile_v1. */
    public record ProfilePayload(byte[] body) implements CustomPayload {
        public static final Id<ProfilePayload> ID = new Id<>(GravesAndGlory.PROFILE);
        public static final PacketCodec<PacketByteBuf, ProfilePayload> CODEC = RawBody.codec(ProfilePayload::new, ProfilePayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static ProfilePayload of(PacketByteBuf buf) { return new ProfilePayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<ProfilePayload> getId() { return ID; }
    }
    /** C2S GravesAndGlory.CAMERA: camera_v1. */
    public record CameraPayload(byte[] body) implements CustomPayload {
        public static final Id<CameraPayload> ID = new Id<>(GravesAndGlory.CAMERA);
        public static final PacketCodec<PacketByteBuf, CameraPayload> CODEC = RawBody.codec(CameraPayload::new, CameraPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static CameraPayload of(PacketByteBuf buf) { return new CameraPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<CameraPayload> getId() { return ID; }
    }
    /** C2S GravesAndGlory.PING: ping_v1. */
    public record PingPayload(byte[] body) implements CustomPayload {
        public static final Id<PingPayload> ID = new Id<>(GravesAndGlory.PING);
        public static final PacketCodec<PacketByteBuf, PingPayload> CODEC = RawBody.codec(PingPayload::new, PingPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static PingPayload of(PacketByteBuf buf) { return new PingPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<PingPayload> getId() { return ID; }
    }
    /** C2S GravesAndGlory.PLAN: battleplan_v1. */
    public record PlanPayload(byte[] body) implements CustomPayload {
        public static final Id<PlanPayload> ID = new Id<>(GravesAndGlory.PLAN);
        public static final PacketCodec<PacketByteBuf, PlanPayload> CODEC = RawBody.codec(PlanPayload::new, PlanPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static PlanPayload of(PacketByteBuf buf) { return new PlanPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<PlanPayload> getId() { return ID; }
    }
    /** C2S GravesAndGlory.TACTICAL: tactical_v1. */
    public record TacticalPayload(byte[] body) implements CustomPayload {
        public static final Id<TacticalPayload> ID = new Id<>(GravesAndGlory.TACTICAL);
        public static final PacketCodec<PacketByteBuf, TacticalPayload> CODEC = RawBody.codec(TacticalPayload::new, TacticalPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static TacticalPayload of(PacketByteBuf buf) { return new TacticalPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<TacticalPayload> getId() { return ID; }
    }
    /** C2S GravesAndGlory.TACTICAL_V2: tactical_v2. */
    public record TacticalV2Payload(byte[] body) implements CustomPayload {
        public static final Id<TacticalV2Payload> ID = new Id<>(GravesAndGlory.TACTICAL_V2);
        public static final PacketCodec<PacketByteBuf, TacticalV2Payload> CODEC = RawBody.codec(TacticalV2Payload::new, TacticalV2Payload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static TacticalV2Payload of(PacketByteBuf buf) { return new TacticalV2Payload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<TacticalV2Payload> getId() { return ID; }
    }

    // Configuration phase (VersionGate, port P1.3). Same id both ways; the registries are per direction.

    /** release_check_v2. Defined here, not in VersionGate, whose class init needs Fabric Loader (keeps PayloadRoundTripCheck loader-free). */
    public static final Identifier RELEASE_CHECK = Identifier.of("gravesandglory", "release_check_v2");

    /** S2C configuration RELEASE_CHECK (VersionGate.CONFIG_CHANNEL): release_check_v2 request, empty body. */
    public record ReleaseCheckRequestPayload(byte[] body) implements CustomPayload {
        public static final Id<ReleaseCheckRequestPayload> ID = new Id<>(RELEASE_CHECK);
        public static final PacketCodec<PacketByteBuf, ReleaseCheckRequestPayload> CODEC = RawBody.codec(ReleaseCheckRequestPayload::new, ReleaseCheckRequestPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static ReleaseCheckRequestPayload of(PacketByteBuf buf) { return new ReleaseCheckRequestPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<ReleaseCheckRequestPayload> getId() { return ID; }
    }
    /** C2S configuration RELEASE_CHECK (VersionGate.CONFIG_CHANNEL): release_check_v2 reply, body VersionGate.response() (varInt protocol,
     *  string version (64), string build (64)). Raw so a malformed reply reaches VersionGate's refusal text, not a decoder error. */
    public record ReleaseCheckReplyPayload(byte[] body) implements CustomPayload {
        public static final Id<ReleaseCheckReplyPayload> ID = new Id<>(RELEASE_CHECK);
        public static final PacketCodec<PacketByteBuf, ReleaseCheckReplyPayload> CODEC = RawBody.codec(ReleaseCheckReplyPayload::new, ReleaseCheckReplyPayload::body);
        /** Wraps the bytes a send site wrote (copied). */
        public static ReleaseCheckReplyPayload of(PacketByteBuf buf) { return new ReleaseCheckReplyPayload(RawBody.copy(buf)); }
        /** A fresh reader over the body, for a receive site. */
        public PacketByteBuf buf() { return RawBody.reader(body); }
        @Override public Id<ReleaseCheckReplyPayload> getId() { return ID; }
    }
}
