package gg.gravesandglory;

import java.util.UUID;
import net.minecraft.entity.EquipmentSlot;
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
final class GearCombat {
    private GearCombat() {}

    /** Fixed ids, so a respawn or a new battle replaces the modifier and never stacks it. HEALTH and ATTACK keep the old class-passive ids. */
    static final UUID HEALTH = UUID.fromString("e01516bb-07f8-49b1-86c4-873d30106609"), ATTACK = UUID.fromString("8352fccf-a081-4a35-a122-2f5e5de6e1de"),
        ARMOUR = UUID.fromString("5b0f2b64-5a8e-4d1c-9a43-1f7c2e6a0b11"), SPEED = UUID.fromString("9e3c7a21-4b6f-4f0e-8d52-6a1b3c9e7f22"),
        ATTACK_SPEED = UUID.fromString("c47d1e90-2a3b-4c5d-9e6f-7a8b9c0d1e33");

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

    /** Seats a squad's gear: its Loadout and its relic slots. A bot seat fights with no gear until part (e). */
    static void seat(GravesAndGlory.Squad s, Profile p) {
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

    static void set(ServerPlayerEntity pl, EntityAttribute a, UUID id, String name, double value) {
        var inst = pl.getAttributeInstance(a);
        if (inst == null) return;
        inst.removeModifier(id);
        if (value != 0) inst.addTemporaryModifier(new EntityAttributeModifier(id, name, value, EntityAttributeModifier.Operation.ADDITION));
    }

    /** Removes every gear modifier (the lobby, between battles). Health above the new maximum is cut back to it. */
    static void clear(ServerPlayerEntity pl) { apply(pl, GearStats.empty()); }

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
        for (var s : b.squads.values()) {
            if (s.bot || !s.seated) continue;
            var pl = b.server.getPlayerManager().getPlayer(s.owner);
            if (pl == null || pl.isSpectator() || pl.getInventory().selectedSlot == 0) continue;
            pl.getInventory().selectedSlot = 0;
            pl.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(0));
        }
    }
}
