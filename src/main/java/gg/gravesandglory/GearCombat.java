package gg.gravesandglory;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Arms a commander from the gear he wears (gear-combat plan Task 3): kit() lays the stand-in stacks (GearLooks) into his hands and armour slots,
 * apply() turns his Loadout into fixed-id attribute modifiers. The stand-ins carry no attributes of their own, so every number is the Loadout's.
 * A profile wearing nothing fights with the starter kit of its role (on a copy; the profile is never changed here).
 */
public final class GearCombat {
    private GearCombat() {}

    /** Fixed ids, so a respawn or a new battle replaces the modifier and never stacks it. HEALTH and ATTACK keep the old class-passive ids. */
    static final Identifier HEALTH = Identifier.of("gravesandglory", "gear_health"), ATTACK = Identifier.of("gravesandglory", "gear_damage"),
        ARMOUR = Identifier.of("gravesandglory", "gear_armour"), SPEED = Identifier.of("gravesandglory", "gear_speed"),
        ATTACK_SPEED = Identifier.of("gravesandglory", "gear_attack_speed");

    /** The vanilla attack speed a player starts from; gearFamilies.attackSpeed is the total, so the modifier is the difference. */
    static final double BASE_ATTACK_SPEED = 4.0;

    /** The profile the kit is built from: the profile itself, or a starter copy of its role when it wears nothing (Review Focus 1). */
    static Profile wearing(Profile p) {
        if (p != null && !p.equipped.isEmpty()) return p;
        Profile copy = new Profile();
        if (p != null) copy.commander = p.commander;
        Gear.grantStarter(copy);
        if (p != null) for (int i = 0; i < p.relicSlots.size() && i < copy.relicSlots.size(); i++) if (!p.relicSlots.get(i).isEmpty()) copy.relicSlots.set(i, p.relicSlots.get(i));
        return copy;
    }

    /** A human seat's Loadout: what its owner wears, with the starter fallback. */
    static GearStats.Loadout loadout(Profile p) { return GearStats.of(wearing(p)); }

    /** Seats a squad's gear: its Loadout and its relic slots, and restarts its commander's heavy_hand swing count (C02-F1). A bot seat fights with no gear until part (e). */
    static void seat(GravesAndGlory.Squad s, Profile p) {
        if (s.owner != null) swings.remove(s.owner);
        s.gear = s.bot || p == null ? GearStats.empty() : loadout(p);
        s.relics = s.gear.relics().toArray(new String[0]);
    }

    static Gear.Piece worn(Profile p, String slot) {
        Integer uid = p.equipped.get(slot);
        return uid == null ? null : Gear.piece(p, uid);
    }

    /** Clears the inventory, then main weapon in hotbar slot 0, off-hand, armour, and arrows for a ranged main: a quiver off-hand is the arrows, else slot 9. */
    static void kit(ServerPlayerEntity pl, Profile p) {
        Profile w = wearing(p);
        var inv = pl.getInventory();
        inv.clear();
        ItemStack main = GearLooks.stack(worn(w, "main"));
        inv.setStack(0, main);
        Gear.Piece offPiece = worn(w, "off");
        ItemStack off = GearLooks.stack(offPiece);
        boolean ranged = main.getItem() instanceof RangedWeaponItem;
        if (ranged && off.isOf(Items.ARROW)) off.setCount(off.getMaxCount());
        else if (off.isOf(Items.ARROW)) off = ItemStack.EMPTY; // a quiver with no bow is only its stats
        pl.equipStack(EquipmentSlot.OFFHAND, off);
        if (ranged && !off.isOf(Items.ARROW)) inv.setStack(9, new ItemStack(Items.ARROW, 64));
        for (int i = 0; i < Gear.ARMOUR.size(); i++) {
            EquipmentSlot slot = switch (i) { case 0 -> EquipmentSlot.HEAD; case 1 -> EquipmentSlot.CHEST; case 2 -> EquipmentSlot.LEGS; default -> EquipmentSlot.FEET; };
            pl.equipStack(slot, GearLooks.stack(worn(w, Gear.ARMOUR.get(i))));
        }
        inv.selectedSlot = 0;
        if (pl.networkHandler != null) pl.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(0));
    }

    static void set(ServerPlayerEntity pl, RegistryEntry<EntityAttribute> a, Identifier id, String name, double value) {
        var inst = pl.getAttributeInstance(a);
        if (inst == null) return;
        inst.removeModifier(id);
        if (value != 0) inst.addTemporaryModifier(new EntityAttributeModifier(id, value, EntityAttributeModifier.Operation.ADD_VALUE));
    }

    /** Removes every gear modifier (the lobby, between battles). Health above the new maximum is cut back to it. */
    static void clear(ServerPlayerEntity pl) { apply(pl, GearStats.empty()); Abilities.keepSpeed(pl, TRAIL, "Graves and Glory trailblazer", 0); }

    /** Replaces the gear modifiers with this Loadout's, then heals to the new maximum. */
    static void apply(ServerPlayerEntity pl, GearStats.Loadout l) {
        set(pl, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH, "Graves and Glory gear health", GearStats.healthBonus(l));
        set(pl, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK, "Graves and Glory gear damage", GearStats.meleeBonus(l));
        set(pl, EntityAttributes.GENERIC_ARMOR, ARMOUR, "Graves and Glory gear armour", GearStats.armourBonus(l));
        set(pl, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED, "Graves and Glory gear speed", GearStats.speedBonus(l));
        var f = GearStats.family(l.mainFamily());
        set(pl, EntityAttributes.GENERIC_ATTACK_SPEED, ATTACK_SPEED, "Graves and Glory gear attack speed", f == null ? 0 : f.attackSpeed() - BASE_ATTACK_SPEED);
        pl.setHealth(pl.getMaxHealth());
    }

    /** In a running battle a seated commander always holds his main weapon: hotbar slot 0 (the keys 1 to 3 are relics). END_SERVER_TICK. */
    static void tick(GravesAndGlory.Battle b) {
        if (!b.running) return;
        if (b.time % 20 == 0) second(b);
        for (var s : b.squads.values()) {
            if (s.bot || !s.seated) continue;
            var pl = b.server.getPlayerManager().getPlayer(s.owner);
            if (pl == null || pl.isSpectator() || pl.getInventory().selectedSlot == 0) continue;
            pl.getInventory().selectedSlot = 0;
            pl.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(0));
        }
    }

    // ---- Weapon behaviours (gear-combat plan Task 4). Each reads the worn family's gearFamilies row; a zero cell turns the behaviour off.

    /** The seated human squad of a player in a running battle, or null (the lobby, a spectator seat, no battle). */
    static GravesAndGlory.Squad seated(PlayerEntity pl) {
        var b = GravesAndGlory.battle;
        if (b == null || !b.running || !(pl instanceof ServerPlayerEntity)) return null;
        var s = b.squads.get(pl.getUuid());
        return s == null || s.bot || !s.seated ? null : s;
    }

    /** Whether e fights on the other side of team: a troop or a seated commander of the other army, alive and not a spectator. */
    static boolean foe(GravesAndGlory.Battle b, Entity e, int team) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e.isSpectator()) return false;
        int t = b.teamOf(e);
        return t >= 0 && t != team;
    }
    static boolean friend(GravesAndGlory.Battle b, Entity e, int team) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e.isSpectator()) return false;
        return b.teamOf(e) == team;
    }

    /** Swings each commander has made with a sweeping family (heavy_hand counts them). */
    static final Map<UUID, Integer> swings = new HashMap<>();
    static boolean sweeping;

    /**
     * A commander's own blow by hand (Battle.allowDamage, before the blow is scaled): with a sweeping main family (sweepRadius and sweepShare above 0),
     * every other foe within sweepRadius of the target (heavy_hand widens every value2-th swing) takes sweepShare of the blow, once per swing (the
     * sweep's own blows never sweep again). Those blows go through allowDamage as the commander's, so they are scaled like any blow of his.
     */
    static void onPlayerHit(GravesAndGlory.Battle b, ServerPlayerEntity pl, GravesAndGlory.Squad s, LivingEntity target, DamageSource src, float amount) {
        if (sweeping || s == null || src.getSource() != pl || src.isIn(DamageTypeTags.IS_PROJECTILE)) return;
        var f = GearStats.family(s.gear.mainFamily());
        if (f == null || f.sweepRadius() <= 0 || f.sweepShare() <= 0) return;
        int swing = swings.merge(pl.getUuid(), 1, Integer::sum);
        double r = f.sweepRadius() * GearEffects.sweepRadiusFactor(s.gear, swing);
        List<LivingEntity> hit = new ArrayList<>();
        for (var e : b.world.getOtherEntities(target, new Box(target.getPos(), target.getPos()).expand(r, 2, r)))
            if (e != pl && e instanceof LivingEntity le && foe(b, le, s.team) && e.squaredDistanceTo(target) <= r * r) hit.add(le);
        sweeping = true;
        try { for (var le : hit) le.damage(src, (float) (amount * f.sweepShare())); } finally { sweeping = false; }
    }

    /** A shield block (ShieldCooldownMixin, PlayerEntity.damageShield): the worn off-hand family's cooldownTicks on the shield, which lowers it. */
    public static void onShieldBlock(PlayerEntity pl) {
        var s = seated(pl);
        if (s == null || !pl.getActiveItem().isOf(Items.SHIELD)) return;
        var f = GearStats.family(s.gear.offFamily());
        if (f == null || f.cooldownTicks() <= 0) return;
        pl.getItemCooldownManager().set(Items.SHIELD, f.cooldownTicks());
        pl.clearActiveItem();
        pl.getWorld().sendEntityStatus(pl, (byte) 30); // the shield-down sound and animation
    }

    /** The bow's use ticks as its draw progress reads them (BowDrawMixin): x GearEffects.drawFactor for a seated commander, else unchanged. */
    public static int drawTicks(LivingEntity user, int ticks) {
        var s = user instanceof PlayerEntity pl ? seated(pl) : null;
        return s == null ? ticks : (int) Math.round(ticks * GearEffects.drawFactor(s.gear));
    }

    /**
     * A staff's use (UseItemCallback): the nearest friendly troop or commander the commander looks at within the family's range is healed
     * healBase x healingFactor, then the staff waits cooldownTicks. Other items and anyone outside battle pass through to vanilla.
     */
    static TypedActionResult<ItemStack> useItem(PlayerEntity pl, World world, Hand hand) {
        ItemStack stack = pl.getStackInHand(hand);
        var s = seated(pl);
        if (world.isClient || s == null || hand != Hand.MAIN_HAND || GearLooks.uid(stack) < 0) return TypedActionResult.pass(stack);
        var f = GearStats.family(s.gear.mainFamily());
        if (f == null || f.healBase() <= 0 || f.range() <= 0) return TypedActionResult.pass(stack);
        if (pl.getItemCooldownManager().isCoolingDown(stack.getItem())) return TypedActionResult.fail(stack);
        LivingEntity target = lookedAt(GravesAndGlory.battle, pl, s.team, f.range());
        if (target == null) return TypedActionResult.fail(stack);
        target.heal((float) (f.healBase() * GearStats.healingFactor(s.gear)));
        pl.getItemCooldownManager().set(stack.getItem(), Math.max(1, f.cooldownTicks()));
        return TypedActionResult.success(stack);
    }

    /** The nearest friend (troop or commander) whose box the look ray from the eyes crosses within range blocks, or null. */
    static LivingEntity lookedAt(GravesAndGlory.Battle b, PlayerEntity pl, int team, double range) {
        Vec3d from = pl.getEyePos(), to = from.add(pl.getRotationVec(1f).multiply(range));
        LivingEntity best = null; double bestD = Double.MAX_VALUE;
        for (var e : b.world.getOtherEntities(pl, new Box(from, to).expand(1))) {
            if (!friend(b, e, team)) continue;
            var hit = e.getBoundingBox().expand(0.3).raycast(from, to);
            if (hit.isEmpty()) continue;
            double d = hit.get().squaredDistanceTo(from);
            if (d < bestD) { bestD = d; best = (LivingEntity) e; }
        }
        return best;
    }

    /**
     * The gear share of a blow the commander strikes himself (Commander.playerFactor): rangedFactor and hawkeye on a projectile, grudge on whoever
     * last hurt one of his troops, last_light while he is low. 1 for a squad with no gear.
     */
    static double blowFactor(GravesAndGlory.Squad s, ServerPlayerEntity pl, LivingEntity target, DamageSource src) {
        if (s == null || pl == null) return 1;
        var l = s.gear;
        double f = 1;
        if (src != null && src.isIn(DamageTypeTags.IS_PROJECTILE)) f *= GearStats.rangedFactor(l) * GearEffects.hawkeyeFactor(l, Math.sqrt(pl.squaredDistanceTo(target)));
        f *= GearEffects.grudgeFactor(l, target != null && target.getUuid().equals(s.grudge));
        f *= GearEffects.lastLightFactor(l, pl.getHealth() / pl.getMaxHealth());
        return f;
    }

    // ---- Set bonuses and perks that act on their own (gear-combat plan Task 6). Each reads its gearSetBonuses or gearPerks row through GearStats.

    /** Fixed ids of the trailblazer speed on the commander and the formation_charge speed on his troops. */
    static final Identifier TRAIL = Identifier.of("gravesandglory", "trailblazer_speed"), FORMATION = Identifier.of("gravesandglory", "formation_charge_speed");

    /** How many foes (troops and seated commanders of the other side) stand within radius blocks of e. */
    static int foesNear(GravesAndGlory.Battle b, Entity e, int team, double radius) {
        int n = 0;
        for (var o : b.world.getOtherEntities(e, e.getBoundingBox().expand(radius, radius, radius))) if (foe(b, o, team) && o.squaredDistanceTo(e) <= radius * radius) n++;
        return n;
    }

    /** The once-a-second bonuses of every seated commander (GearCombat.tick on the battle's whole seconds): heal_over_time, cleanse, trailblazer and formation_charge. */
    static void second(GravesAndGlory.Battle b) {
        for (var s : b.squads.values()) {
            if (s.bot || !s.seated) continue;
            var p = Commander.player(b, s);
            healOverTime(b, s, p);
            if (cleanseDue(b, s)) cleanse(b, s, p);
            trail(b, s, p);
            formation(b, s, p);
        }
    }

    /** heal_over_time: each of his own troops within the bonus radius of him heals value1. */
    static void healOverTime(GravesAndGlory.Battle b, GravesAndGlory.Squad s, ServerPlayerEntity p) {
        if (p == null || !GearStats.has(s.gear, "heal_over_time")) return;
        double r = GearStats.radius(s.gear, "heal_over_time");
        float heal = (float) GearStats.value1(s.gear, "heal_over_time");
        for (var u : b.units) if (u.squad == s && u.mob.isAlive() && u.mob.squaredDistanceTo(p) <= r * r) u.mob.heal(heal);
    }

    /** cleanse is due every value1 seconds of battle time (its value1 is the period). */
    static boolean cleanseDue(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (!GearStats.has(s.gear, "cleanse")) return false;
        long every = Math.max(1, Math.round(GearStats.value1(s.gear, "cleanse")));
        return (b.time / 20) % every == 0;
    }

    /** cleanse: slow, weaken and bleed leave him and his own troops within the bonus radius (Statuses.cleanse). */
    static void cleanse(GravesAndGlory.Battle b, GravesAndGlory.Squad s, ServerPlayerEntity p) {
        if (p == null || !GearStats.has(s.gear, "cleanse")) return;
        double r = GearStats.radius(s.gear, "cleanse");
        Statuses.cleanse(b, null, p);
        for (var u : b.units) if (u.squad == s && u.mob.isAlive() && u.mob.squaredDistanceTo(p) <= r * r) Statuses.cleanse(b, null, u.mob);
    }

    /** trailblazer: x(1 + value1) movement speed on him while no foe stands within value2 blocks; off again once one does. */
    static void trail(GravesAndGlory.Battle b, GravesAndGlory.Squad s, ServerPlayerEntity p) {
        if (p == null) return;
        boolean has = GearStats.has(s.gear, "trailblazer");
        double f = has ? GearEffects.trailFactor(s.gear, foesNear(b, p, s.team, GearStats.value2(s.gear, "trailblazer")) > 0) : 1;
        Abilities.keepSpeed(p, TRAIL, "Graves and Glory trailblazer", f - 1);
    }

    /** formation_charge: his own troops inside his command aura on the Attack order move x(1 + value1) on their ground (BattleMath.speedModifier); the rest lose it. */
    static void formation(GravesAndGlory.Battle b, GravesAndGlory.Squad s, ServerPlayerEntity p) {
        boolean has = p != null && GearStats.has(s.gear, "formation_charge");
        double r = has ? GearEffects.commandRadius(s.gear) : 0;
        for (var u : b.units) {
            if (u.squad != s || !u.mob.isAlive()) continue;
            var a = b.assignment(u);
            boolean on = has && a != null && "attack".equals(a.order) && GravesAndGlory.Battle.horizontalDistance(p.getPos(), u.mob.getPos()) <= r * r;
            Abilities.keepSpeed(u.mob, FORMATION, "Graves and Glory formation charge", on ? BattleMath.speedModifier(1 + GearStats.value1(s.gear, "formation_charge"), u.mob.isTouchingWater()) : 0);
        }
    }

    /**
     * taunt_aura: the enemy commander a troop is drawn to, or null (Tactics). Of the troops of the troop's army that stand within the bonus radius of a commander
     * wearing it, the tauntShare of them (rounded) with the lowest entity ids pick him, so the share is exact and each troop's choice holds still.
     */
    static ServerPlayerEntity taunter(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        for (var s : b.squads.values()) {
            if (s.bot || !s.seated || s.team == u.squad.team || !GearStats.has(s.gear, "taunt_aura")) continue;
            var p = Commander.player(b, s);
            double r = GearStats.radius(s.gear, "taunt_aura");
            if (p == null || u.mob.squaredDistanceTo(p) > r * r) continue;
            List<Integer> ids = new ArrayList<>();
            for (var o : b.units) if (o.squad.team == u.squad.team && o.mob.isAlive() && o.mob.squaredDistanceTo(p) <= r * r) ids.add(o.mob.getId());
            Collections.sort(ids);
            int idx = ids.indexOf(u.mob.getId());
            if (idx >= 0 && idx < Math.round(GearEffects.tauntShare(s.gear) * ids.size())) return p;
        }
        return null;
    }

    /** pierce (ENTITY_LOAD): an arrow a seated commander looses (his bow's or his Volley's) pierces GearEffects.pierceLevel foes, never fewer than it already would. */
    static void onEntityLoad(Entity e) {
        if (!(e instanceof PersistentProjectileEntity a) || !(a.getOwner() instanceof PlayerEntity pl)) return;
        var s = seated(pl);
        if (s == null) return;
        int level = GearEffects.pierceLevel(s.gear);
        if (level > a.getPierceLevel()) {
            // 1.21.1 hides setPierceLevel (private); a round trip through the arrow's own save data sets it without touching any other field.
            var data = new NbtCompound();
            a.writeCustomDataToNbt(data);
            data.putByte("PierceLevel", (byte) Math.min(127, level));
            a.readCustomDataFromNbt(data);
        }
    }

    /**
     * second_chance (Battle.allowDamage, before the downed branch): the blow that would down a seated commander leaves him on value1 health instead, once a
     * match (Squad.secondChanceUsed; each match makes new Squads). True when it caught the blow.
     */
    static boolean secondChance(GravesAndGlory.Squad s, ServerPlayerEntity p) {
        if (s == null || s.bot || !GearEffects.secondChance(s.gear, s.secondChanceUsed)) return false;
        s.secondChanceUsed = true;
        p.setHealth((float) Math.max(1, Math.min(p.getMaxHealth(), GearStats.value1(s.gear, "second_chance"))));
        p.getWorld().playSound(null, p.getBlockPos(), net.minecraft.sound.SoundEvents.ITEM_TOTEM_USE, net.minecraft.sound.SoundCategory.PLAYERS, .6f, 1.2f);
        return true;
    }
}
