package gg.gravesandglory;

import net.minecraft.item.ItemStack;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

/**
 * The stand-in vanilla items a worn gear piece shows as in battle until the art track (part f) gives each its own model: the family's item
 * (gearFamilies.item) for a weapon, the slot's row for armour. The stack carries the item's name in its rarity colour (gearRarities.border), the
 * piece uid as custom_data gg_uid, the unbreakable component (its tooltip line is shown, as on 1.20.1) and a one-entry zero attribute_modifiers component (tooltip hidden; see NO_VANILLA_STATS), so the vanilla item adds no damage,
 * speed or armour of its own: every number comes from GearCombat's modifiers. It also carries the item id (gg_item) and rarity number (gg_rarity, 1
 * Common to 5 Legendary), which CommanderGearFeature draws the piece's own model from; the custom_data component reaches every client already (equipment sync). No catalogue
 * data here; it all comes from the sheets.
 */
final class GearLooks {
    private GearLooks() {}

    static final String UID = "gg_uid", ITEM = "gg_item", RARITY = "gg_rarity";

    /**
     * The attribute_modifiers that strip the vanilla item's own damage, speed and armour. An empty list is NOT enough in 1.21.1: ItemStack.applyAttributeModifier
     * falls back to Item.getAttributeModifiers() when the component's list is empty (fixed only in 1.21.2), so a stand-in sword or chestplate would add its vanilla
     * numbers on top of GearCombat's. One zero-value entry keeps the list non-empty; tooltip hidden, as HideFlags 2 hid it.
     */
    static final AttributeModifiersComponent NO_VANILLA_STATS = new AttributeModifiersComponent(List.of(new AttributeModifiersComponent.Entry(
        EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(Identifier.of("gravesandglory", "no_vanilla_stats"), 0, EntityAttributeModifier.Operation.ADD_VALUE),
        AttributeModifierSlot.ANY)), false);

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
        ItemStack s = new ItemStack(Registries.ITEM.get(Identifier.of(f.item())));
        if (s.isEmpty()) return ItemStack.EMPTY;
        var r = Gear.rarity(p.rarity);
        int colour = r == null ? 0xffffff : Integer.parseInt(r.border().substring(1), 16);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(it.name()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)).withItalic(false)));
        var nbt = new NbtCompound();
        nbt.putInt(UID, p.uid);
        nbt.putString(ITEM, p.item);
        nbt.putInt(RARITY, rarityNumber(p.rarity));
        s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        s.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));
        s.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, NO_VANILLA_STATS);
        return s;
    }

    /** The piece uid a stand-in stack carries, or -1. */
    static int uid(ItemStack s) {
        var data = data(s);
        return data != null && data.contains(UID) ? data.getNbt().getInt(UID) : -1;
    }

    /** The art a stand-in stack names, or null for any other stack. */
    static Art art(ItemStack s) {
        var data = s == null || s.isEmpty() ? null : data(s);
        return data != null && data.contains(ITEM) && data.contains(RARITY) ? new Art(data.getNbt().getString(ITEM), data.getNbt().getInt(RARITY)) : null;
    }

    /** The custom_data component of a stack, or null. getNbt() is the component's own tag, not a copy (read only here: art() runs every frame per drawn piece). */
    private static NbtComponent data(ItemStack s) { return s == null ? null : s.get(DataComponentTypes.CUSTOM_DATA); }

    /** A rarity id's place in gearRarities, 1 Common to 5 Legendary; 1 for an unknown id. */
    static int rarityNumber(String id) {
        for (int i = 0; i < Spec.GEARRARITIES.length; i++) if (Spec.GEARRARITIES[i].id().equals(id)) return i + 1;
        return 1;
    }
}
