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
 * speed or armour of its own: every number comes from GearCombat's modifiers. It also carries the item id (gg_item) and rarity number (gg_rarity, 1
 * Common to 5 Legendary), which CommanderGearFeature draws the piece's own model from; equipment NBT reaches every client already. No catalogue
 * data here; it all comes from the sheets.
 */
final class GearLooks {
    private GearLooks() {}

    static final String UID = "gg_uid", ITEM = "gg_item", RARITY = "gg_rarity";

    /** What a stand-in stack draws as: the gear item and its rarity number (1 to 5). */
    record Art(String item, int rarity) {}

    /** The gearFamilies row for a weapon family or an armour slot, or null. */
    static Spec.GearFamiliesRow family(String id) { return GearStats.family(id); }

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
        nbt.putString(ITEM, p.item);
        nbt.putInt(RARITY, rarityNumber(p.rarity));
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

    /** The art a stand-in stack names, or null for any other stack. */
    static Art art(ItemStack s) {
        var nbt = s == null || s.isEmpty() ? null : s.getNbt();
        return nbt != null && nbt.contains(ITEM) && nbt.contains(RARITY) ? new Art(nbt.getString(ITEM), nbt.getInt(RARITY)) : null;
    }

    /** A rarity id's place in gearRarities, 1 Common to 5 Legendary; 1 for an unknown id. */
    static int rarityNumber(String id) {
        for (int i = 0; i < Spec.GEARRARITIES.length; i++) if (Spec.GEARRARITIES[i].id().equals(id)) return i + 1;
        return 1;
    }
}
