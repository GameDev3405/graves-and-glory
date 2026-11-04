package gg.gravesandglory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

/**
 * The stand-in vanilla items a worn gear piece shows as in battle until the art track (part f) gives each its own model: the family's item
 * (gearFamilies.item) for a weapon, the slot's row for armour. The stack carries the item's name in its rarity colour (gearRarities.border), the
 * piece uid as NBT gg_uid, Unbreakable, HideFlags 2 (no attribute lines) and an empty AttributeModifiers list, so the vanilla item adds no damage,
 * speed or armour of its own: every number comes from GearCombat's modifiers. No catalogue data here; it all comes from the sheets.
 */
final class GearLooks {
    private GearLooks() {}

    static final String UID = "gg_uid";

    /** The gearFamilies row for a weapon family or an armour slot, or null. */
    static Spec.GearFamiliesRow family(String id) {
        for (var f : Spec.GEARFAMILIES) if (f.id().equals(id)) return f;
        return null;
    }

    /** The row a piece's look comes from: its weapon family, or its armour slot. */
    static Spec.GearFamiliesRow familyOf(Gear.Piece p) {
        var it = p == null ? null : Gear.item(p.item);
        if (it == null) return null;
        return family(Gear.ARMOUR.contains(it.slot()) ? it.slot() : it.family());
    }

    /** The stand-in stack for a worn piece; EMPTY for null or an unknown item. */
    static ItemStack stack(Gear.Piece p) {
        var it = p == null ? null : Gear.item(p.item);
        var f = familyOf(p);
        if (it == null || f == null) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(Registries.ITEM.get(new Identifier(f.item())));
        if (s.isEmpty()) return ItemStack.EMPTY;
        var r = Gear.rarity(p.rarity);
        int colour = r == null ? 0xffffff : Integer.parseInt(r.border().substring(1), 16);
        s.setCustomName(Text.literal(it.name()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)).withItalic(false)));
        var nbt = s.getOrCreateNbt();
        nbt.putInt(UID, p.uid);
        nbt.putBoolean("Unbreakable", true);
        nbt.putInt("HideFlags", 2);
        nbt.put("AttributeModifiers", new NbtList());
        return s;
    }

    /** The piece uid a stand-in stack carries, or -1. */
    static int uid(ItemStack s) {
        var nbt = s == null ? null : s.getNbt();
        return nbt != null && nbt.contains(UID) ? nbt.getInt(UID) : -1;
    }
}
