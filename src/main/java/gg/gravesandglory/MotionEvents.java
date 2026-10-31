package gg.gravesandglory;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BooleanSupplier;

/**
 * The animation rework's server-to-client motion events (spec 5 and 6.4): what a troop swings (attack_v1), what strikes it (hit_v1) and how it
 * stands (stance_v2). The server is authoritative; the client's motion engine (Task 13) reads the queues in Client. Every tick is the world's
 * time (getTime), the clock MotionInput reads: a swing's startTick is the world tick it began on (Unit.swingTick; Unit.swungAt is the same swing on the
 * battle clock). Every angle is in MotionInput's convention: a hit's bearing 0 in front, 180 behind, negative from the troop's left; an aim pitch in
 * degrees, positive down as Minecraft's own pitch, so a lofted volley aims at a negative pitch.
 */
public final class MotionEvents {
    private MotionEvents() {}
    /** A troop's swing: a varInt entity id, a varInt index into its family's attackVariants rows (Tactics.variants, Battle's pick) and a varLong world tick it began on. */
    public static final Identifier ATTACK = new Identifier("gravesandglory", "attack_v1");
    /** A blow that struck a troop: a varInt entity id, byte kind (KINDS), byte zone (ZONES), float signed bearing (BattleMath.signedBearing), byte weight (0-2), byte flags (1 blocked, 2 lethal), varLong world tick. */
    public static final Identifier HIT = new Identifier("gravesandglory", "hit_v1");
    /**
     * A troop's stance: a varInt entity id, byte index into STANCES and a float aimPitch (Projectiles.aimPitch: degrees, positive down as MotionInput.aimPitch
     * and Minecraft's pitch, so a lofted volley is negative; 0 unless ranged_aim). The client stores it as sent and MotionInput reads it unchanged. Replaces stance_v1.
     */
    public static final Identifier STANCE = new Identifier("gravesandglory", "stance_v2");
    public static final List<String> STANCES = BattleMath.STANCES, KINDS = BattleMath.KINDS, ZONES = BattleMath.ZONES;
    /** Packets sent since the server started, for the live checks: attacks, hits, stances on a change, stances and attacks on START_TRACKING, and on a late channel registration. */
    static volatile int attackSends, hitSends, stanceSends, trackingSends, registerSends;
    /**
     * The entities a troop's blow is being dealt to now (Combat.resolve), with how deep: the hit Combat sends is that target's own, so HitEventMixin sends
     * none for damage to it meanwhile. Damage to any other entity inside the resolve (thorns on the attacker, a splash) still sends its own. Server thread only.
     */
    static final Map<LivingEntity, Integer> RESOLVING = new IdentityHashMap<>();
    /** Deals a troop's blow on target through damage (Combat.resolve): HitEventMixin sends no hit for target's damage inside it, Combat does. Returns what damage returned. */
    static boolean resolving(LivingEntity target, BooleanSupplier damage) {
        RESOLVING.merge(target, 1, Integer::sum);
        try { return damage.getAsBoolean(); } finally { RESOLVING.computeIfPresent(target, (k, n) -> n > 1 ? n - 1 : null); }
    }
    /** A stance as the client holds it. */
    public record Stance(String name, float aimPitch) { public static final Stance NONE = new Stance("", 0); }

    static Collection<ServerPlayerEntity> watchers(LivingEntity e) { return PlayerLookup.tracking(e); }

    /** Sends a troop's swing to every player tracking it: variantIndex into Tactics.variants(its family), startTick the world tick it began on. Returns how many it sent. */
    public static int sendAttack(GravesAndGlory.Unit u, int variantIndex, long startTick) { return sendAttack(u, variantIndex, startTick, watchers(u.mob)); }
    static int sendAttack(GravesAndGlory.Unit u, int variantIndex, long startTick, Collection<ServerPlayerEntity> players) {
        int sent = 0;
        for (ServerPlayerEntity p : players) {
            if (!ServerPlayNetworking.canSend(p, ATTACK)) continue;
            PacketByteBuf buf = PacketByteBufs.create(); buf.writeVarInt(u.mob.getId()); buf.writeVarInt(variantIndex); buf.writeVarLong(startTick);
            ServerPlayNetworking.send(p, ATTACK, buf); sent++;
        }
        attackSends += sent; return sent;
    }
    /** Sends a blow on a troop to every player tracking it, stamped with the world tick now. Returns how many it sent. */
    public static int sendHit(LivingEntity victim, String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal) {
        return sendHit(victim, kind, zone, bearing, weight, blocked, lethal, watchers(victim));
    }
    static int sendHit(LivingEntity victim, String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal, Collection<ServerPlayerEntity> players) {
        int k = Math.max(0, KINDS.indexOf(kind)), z = ZONES.indexOf(zone), sent = 0;
        if (z < 0) z = 1;
        long tick = victim.getWorld().getTime();
        for (ServerPlayerEntity p : players) {
            if (!ServerPlayNetworking.canSend(p, HIT)) continue;
            PacketByteBuf buf = PacketByteBufs.create(); buf.writeVarInt(victim.getId()); buf.writeByte(k); buf.writeByte(z); buf.writeFloat(bearing);
            buf.writeByte(Math.max(0, Math.min(2, weight))); buf.writeByte((blocked ? 1 : 0) | (lethal ? 2 : 0)); buf.writeVarLong(tick);
            ServerPlayNetworking.send(p, HIT, buf); sent++;
        }
        hitSends += sent; return sent;
    }
    /** Sends a troop's stance (STANCES) and its aim pitch to every player tracking it. Returns how many it sent. */
    public static int sendStance(GravesAndGlory.Unit u, String stance, float aimPitch) { return sendStance(u, stance, aimPitch, watchers(u.mob)); }
    static int sendStance(GravesAndGlory.Unit u, String stance, float aimPitch, Collection<ServerPlayerEntity> players) {
        int index = Math.max(0, STANCES.indexOf(stance)), sent = 0;
        for (ServerPlayerEntity p : players) {
            if (!ServerPlayNetworking.canSend(p, STANCE)) continue;
            PacketByteBuf buf = PacketByteBufs.create(); buf.writeVarInt(u.mob.getId()); buf.writeByte(index); buf.writeFloat(aimPitch);
            ServerPlayNetworking.send(p, STANCE, buf); sent++;
        }
        stanceSends += sent; return sent;
    }
    /**
     * A troop struck by something other than a troop's blow (HitEventMixin, when LivingEntity.damage returns true or vanilla's shield stopped the blow;
     * Combat.resolve sends its own for its target, so that target's damage inside it is left alone): a fall is blunt to the legs, fire, magic or wither
     * magic, an explosion blunt, a projectile an arrow at the zone of its height, a player's blow his weapon's kind (BattleMath.weaponKind), a troop's
     * damage outside resolve (a bleed) a slash, anything else blunt. Its signed bearing is from the source's position (0 for none); its weight is the
     * share of the troop's maximum health it really lost (lost: health and absorption before less after, so a blow inside the hurt cooldown or on a
     * shield reads light); blocked when vanilla's shield took it (a troop's own blow bypasses shields, so only a player's or a stray one can be).
     */
    public static void damaged(LivingEntity self, DamageSource source, double lost, boolean blocked) {
        var b = GravesAndGlory.battle;
        if (RESOLVING.containsKey(self) || b == null || !b.running || self.getWorld().isClient() || self.getWorld() != b.world || !ArmyMobs.isTroop(self)) return;
        String kind = "blunt", zone = "body";
        if (source.isIn(DamageTypeTags.IS_FALL)) zone = "legs";
        else if (source.isIn(DamageTypeTags.IS_FIRE) || source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC) || source.isOf(DamageTypes.WITHER)) kind = "magic";
        else if (source.isIn(DamageTypeTags.IS_EXPLOSION)) kind = "blunt";
        else if (source.isIn(DamageTypeTags.IS_PROJECTILE)) { kind = "arrow"; Entity shot = source.getSource(); if (shot != null) zone = BattleMath.zone(shot.getY(), self.getY(), self.getHeight()); }
        else if (source.getAttacker() instanceof PlayerEntity p) kind = BattleMath.weaponKind(Registries.ITEM.getId(p.getMainHandStack().getItem()).toString());
        else if (source.isOf(Combat.TROOP)) kind = "slash";
        Vec3d from = source.getPosition();
        float bearing = from == null ? 0 : (float) BattleMath.signedBearing(self.getYaw(), from.x - self.getX(), from.z - self.getZ());
        var D = Spec.COMBATDISCIPLINE[0];
        sendHit(self, kind, zone, bearing, BattleMath.weight(lost, self.getMaxHealth(), D.mediumHitShare(), D.heavyHitShare()), blocked, !self.isAlive());
    }
    /**
     * Whether the troop's newest swing may still show (BattleMath.swingCurrent): a valid variant that began no more than ATTACK_CLIP_MAX_TICKS ago on the
     * world clock. The server holds no clip lengths; the client plays the swing at its real clip time and skips one whose clip has ended (carry B8).
     */
    static boolean swingCurrent(GravesAndGlory.Unit u) {
        var rows = Tactics.variants(u.spec.family());
        return BattleMath.validVariant(u.lastVariant, rows.size()) && BattleMath.swingCurrent(u.mob.getWorld().getTime(), u.swingTick);
    }
    /** The troop's stance to a player catching up, when it has one. Returns how many it sent. */
    static int catchUpStance(GravesAndGlory.Unit u, ServerPlayerEntity p) { return u.stance.isEmpty() ? 0 : sendStance(u, u.stance, u.aimPitch, List.of(p)); }
    /** The troop's newest swing to a player catching up, with the world tick it began on, while it still shows (swingCurrent). Returns how many it sent. */
    static int catchUpAttack(GravesAndGlory.Unit u, ServerPlayerEntity p) { return swingCurrent(u) ? sendAttack(u, u.lastVariant, u.swingTick, List.of(p)) : 0; }
    /** What a player who registers the channels late is owed for a troop his client already has: its stance and its current swing. Returns how many it sent. */
    static int catchUp(GravesAndGlory.Unit u, ServerPlayerEntity p) { return catchUpStance(u, p) + catchUpAttack(u, p); }
    /**
     * What a player who starts tracking a troop is owed (START_TRACKING, which Fabric fires before the entity's spawn packet): its stance now (the client
     * keeps a stance for an id it does not have yet) and its current swing on a server task, after the spawn packet has gone (the client drops a swing for
     * an id its world does not have). The task sends it only while the player still tracks the living troop. Returns how many it sent now.
     */
    static int startTracking(GravesAndGlory.Unit u, ServerPlayerEntity p) {
        int sent = catchUpStance(u, p);
        if (swingCurrent(u)) p.server.send(new ServerTask(p.server.getTicks(), () -> {
            if (!u.mob.isAlive() || !PlayerLookup.tracking(u.mob).contains(p)) return;
            int late = catchUpAttack(u, p); trackingSends += late; Tactics.trackingSends += late;
        }));
        return sent;
    }
    /** catchUp for every troop the player tracks: for a client that registered the channels after its trackers started. */
    static int sendTracked(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        int sent = 0;
        for (GravesAndGlory.Unit u : b.units) if (PlayerLookup.tracking(u.mob).contains(p)) sent += catchUp(u, p);
        return sent;
    }

    /**
     * The client's side (GravesAndGloryClient registers the receivers): each troop's recent swings and blows, newest last and the oldest dropped past
     * KEEP, and its stance. A swing or a blow for an id the client world does not have is dropped, so no queue is made for an unloaded entity (the
     * server sends a catch-up swing after the spawn packet); a swing outside its family's attackVariants rows is dropped and a weight is clamped to 0..2.
     * A stance arrives on START_TRACKING before the entity's spawn packet, so it is kept for an id the world does not have yet. An unloaded entity's
     * entries go (forget) and a disconnect clears them all. Concurrent: the integrated server's checks read them.
     */
    public static final class Client {
        private Client() {}
        static final int KEEP = 8;
        static final Map<Integer, Deque<MotionInput.Attack>> ATTACKS = new ConcurrentHashMap<>();
        static final Map<Integer, Deque<MotionInput.Hit>> HITS = new ConcurrentHashMap<>();
        static final Map<Integer, Stance> STANCES = new ConcurrentHashMap<>();
        /** The troop's recent swings, oldest first (empty for none). */
        public static Deque<MotionInput.Attack> attacks(int id) { return ATTACKS.getOrDefault(id, new ArrayDeque<>()); }
        /** The blows that recently struck the troop, oldest first (empty for none). */
        public static Deque<MotionInput.Hit> hits(int id) { return HITS.getOrDefault(id, new ArrayDeque<>()); }
        /** The troop's stance (Stance.NONE for none). */
        public static Stance stance(int id) { return STANCES.getOrDefault(id, Stance.NONE); }
        static <T> void push(Map<Integer, Deque<T>> map, int id, T value) {
            Deque<T> q = map.computeIfAbsent(id, k -> new ConcurrentLinkedDeque<>()); q.addLast(value);
            while (q.size() > KEEP) q.pollFirst();
        }
        /** The most attack variants a family may have; a swing for a troop whose family this client cannot name is kept inside it (carry B7). */
        static final int MAX_VARIANTS = 64;
        /**
         * A swing: rows is the attackVariants count of the troop's family, 0 when the client cannot name the troop (its custom name is not one it knows:
         * TroopMotion validates the variant against the model's family when it plays it), -1 when the client world has no entity of this id. Dropped out
         * of range, and dropped when it repeats one already kept (carry B6: a catch-up and the live send may both bring the same swing).
         */
        static void attack(int id, int variant, long startTick, int rows) {
            if (rows < 0 || !BattleMath.validVariant(variant, rows == 0 ? MAX_VARIANTS : rows)) return;
            MotionInput.Attack a = new MotionInput.Attack(variant, startTick);
            Deque<MotionInput.Attack> q = ATTACKS.get(id);
            if (q != null && q.contains(a)) return;
            push(ATTACKS, id, a);
        }
        /** A blow: known when the client world has an entity of this id (else dropped); the weight clamped to 0..2. */
        static void hit(boolean known, int id, int kind, int zone, float bearing, int weight, int flags, long tick) {
            if (!known) return;
            String k = kind >= 0 && kind < KINDS.size() ? KINDS.get(kind) : "blunt", z = zone >= 0 && zone < ZONES.size() ? ZONES.get(zone) : "body";
            push(HITS, id, new MotionInput.Hit(k, z, bearing, BattleMath.weightOrdinal(weight), (flags & 1) != 0, (flags & 2) != 0, tick));
        }
        static void stance(int id, int index, float aimPitch) {
            String name = index >= 0 && index < MotionEvents.STANCES.size() ? MotionEvents.STANCES.get(index) : "";
            if (name.isEmpty()) STANCES.remove(id); else STANCES.put(id, new Stance(name, aimPitch));
        }
        static void forget(int id) { ATTACKS.remove(id); HITS.remove(id); STANCES.remove(id); }
        static void clear() { ATTACKS.clear(); HITS.clear(); STANCES.clear(); }
    }
}
