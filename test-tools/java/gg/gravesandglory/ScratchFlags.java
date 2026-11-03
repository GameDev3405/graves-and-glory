package gg.gravesandglory;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.LivingEntity;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * Unshipped scratch measurement flags for the duel harness (audit proposal A), never part of a player JAR (release_config forbids this class name).
 *   -PscratchNoHurtCooldown=true  zero timeUntilRegen before every troop-typed blow lands, so each blow deals its full amount (vanilla otherwise
 *                                 takes only the excess of a blow inside the last 10 ticks of an accepted hit). Fabric's ALLOW_DAMAGE runs at the head
 *                                 of LivingEntity.damage, before the invulnerability check.
 *   -PscratchTroopBypassArmor=true  the troop damage type ignores vanilla armour and toughness (a permanent -armor modifier on the target the first time
 *                                 a troop-typed blow reaches it; the same effect as listing the type in bypasses_armor, without shipping a tag).
 * Always on while a duel sim runs: count every queued damaging shot per shooting troop and every one lost because its target died or was removed
 * before it landed (audit item 6). Written to duel/drops.csv by {@link #write}.
 */
final class ScratchFlags {
    static final boolean NO_HURT_COOLDOWN = Boolean.getBoolean("gravesandglory.scratchNoHurtCooldown");
    static final boolean TROOP_BYPASS_ARMOR = Boolean.getBoolean("gravesandglory.scratchTroopBypassArmor");
    static final UUID ARMOR = UUID.fromString("5c7a0b3e-0000-4000-8000-00000000a001"), TOUGH = UUID.fromString("5c7a0b3e-0000-4000-8000-00000000a002");
    private static final Set<Projectiles.Shot> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    /** troop id -> {fired, dropped}. */
    static final Map<String, long[]> shots = new TreeMap<>();
    private ScratchFlags() {}

    static void install() {
        System.out.println("[duel] scratch flags: noHurtCooldown=" + NO_HURT_COOLDOWN + " troopBypassArmor=" + TROOP_BYPASS_ARMOR);
        if (!NO_HURT_COOLDOWN && !TROOP_BYPASS_ARMOR) return;
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!source.isOf(Combat.TROOP)) return true;
            if (NO_HURT_COOLDOWN) entity.timeUntilRegen = 0;
            if (TROOP_BYPASS_ARMOR) strip(entity);
            return true;
        });
    }
    private static void strip(LivingEntity e) {
        var armor = e.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (armor != null && armor.getModifier(ARMOR) == null)
            armor.addTemporaryModifier(new EntityAttributeModifier(ARMOR, "scratch no armor", -1024, EntityAttributeModifier.Operation.ADDITION));
        var tough = e.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
        if (tough != null && tough.getModifier(TOUGH) == null)
            tough.addTemporaryModifier(new EntityAttributeModifier(TOUGH, "scratch no toughness", -1024, EntityAttributeModifier.Operation.ADDITION));
    }

    /** Once per tick during a fight: a queued shot that has left the queue before its landing tick, with its target dead or removed, was dropped. */
    static void track(GravesAndGlory.Battle b) {
        Set<Projectiles.Shot> now = Collections.newSetFromMap(new IdentityHashMap<>());
        now.addAll(b.shots);
        for (var s : now) if (seen.add(s) && counted(s)) shots.computeIfAbsent(s.shooter().spec.id(), k -> new long[2])[0]++;
        for (var it = seen.iterator(); it.hasNext(); ) {
            var s = it.next();
            if (now.contains(s)) continue;
            it.remove();
            if (counted(s) && b.time < s.lands() && !(s.target().isAlive() && !s.target().isRemoved())) shots.computeIfAbsent(s.shooter().spec.id(), k -> new long[2])[1]++;
        }
    }
    /** Fight over: the queue is cleared next, so forget the shots without counting them. */
    static void reset() { seen.clear(); }
    private static boolean counted(Projectiles.Shot s) { return s.target() != null && s.amount() > 0 && (s.kind().equals("arrow") || s.kind().equals("trident") || s.kind().equals("bolt")); }

    static void write(Path file) {
        StringBuilder sb = new StringBuilder("troop,fired,dropped,dropPct\n");
        long f = 0, d = 0;
        for (var e : shots.entrySet()) {
            sb.append(e.getKey()).append(',').append(e.getValue()[0]).append(',').append(e.getValue()[1]).append(',').append(String.format(Locale.ROOT, "%.2f", 100.0 * e.getValue()[1] / Math.max(1, e.getValue()[0]))).append('\n');
            f += e.getValue()[0]; d += e.getValue()[1];
        }
        sb.append("ALL,").append(f).append(',').append(d).append(',').append(String.format(Locale.ROOT, "%.2f", 100.0 * d / Math.max(1, f))).append('\n');
        try { java.nio.file.Files.writeString(file, sb.toString(), StandardCharsets.UTF_8); } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
    }
}
