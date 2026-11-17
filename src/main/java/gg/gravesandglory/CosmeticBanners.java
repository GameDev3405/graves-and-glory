package gg.gravesandglory;

import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.Map;

/**
 * The vanilla carriers of the cosmetics (cosmetics render plan 6): every client, plain ones too, sees these, and they carry only minecraft:
 * ids (Cosmetics.layers). A commander's held banner-family gear stack becomes the banner of his side's dye with his layers (dress), and a
 * planted banner block (the Forward Supply banner, the Oath Banner) takes its commander's layers (plant). Bots have no cosmetics. The modded
 * extras ride on cosmetic_v1 (CosmeticSync) and draw in CosmeticExtras.
 */
public final class CosmeticBanners {
    private CosmeticBanners() {}

    /** Harness: patterned stacks dressed and banner blocks planted. */
    static volatile int dressed, planted;

    /** The layers of a slot map on a field of dye, as the banner_patterns component, looked up in the given registries; layers the registry lacks are skipped. */
    static BannerPatternsComponent component(Map<String, String> slots, String fieldDye, RegistryWrapper.WrapperLookup registries) {
        var builder = new BannerPatternsComponent.Builder();
        if (registries == null) return builder.build();
        var lookup = registries.getWrapperOrThrow(RegistryKeys.BANNER_PATTERN);
        for (var layer : Cosmetics.layers(slots, fieldDye)) {
            var key = RegistryKey.of(RegistryKeys.BANNER_PATTERN, Identifier.of(layer.pattern()));
            lookup.getOptional(key).ifPresent(entry -> builder.add(entry, DyeColor.byName(layer.dye(), DyeColor.WHITE)));
        }
        return builder.build();
    }

    /** The equipped slot map of a squad's commander, or null for a bot or no battle. */
    static Map<String, String> slots(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (b == null || s == null || s.bot || s.owner == null) return null;
        Profile p = b.campaign.profile(s.owner);
        return Cosmetics.normalize(p.cosmetics, p.cosmeticSlots);
    }

    /**
     * Dresses the banner-family gear stacks a player holds (GearLooks' stand-in: a white banner carrying gg_item): the banner of the given
     * dye, with the layers of his equipped slots. Every other component stays (gg_item, gg_uid, the name, the stats strip). A team of -1 (no
     * side yet) keeps the field white.
     */
    static void dress(ServerPlayerEntity pl, Profile p, DyeColor field) {
        if (pl == null || p == null) return;
        var slots = Cosmetics.normalize(p.cosmetics, p.cosmeticSlots);
        var inv = pl.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!(s.getItem() instanceof BannerItem) || GearLooks.art(s) == null) continue;
            ItemStack out = s.copyComponentsToNewStack(BannerBlock.getForColor(field), s.getCount());
            var layers = component(slots, field.getName(), pl.getServerWorld().getRegistryManager());
            if (layers.layers().isEmpty()) out.remove(DataComponentTypes.BANNER_PATTERNS); else out.set(DataComponentTypes.BANNER_PATTERNS, layers);
            inv.setStack(i, out);
            dressed++;
        }
    }

    /** dress for a player in a battle: his side's banner dye (the nearest dye to its colour, as the planted banners take), else white. */
    static void dress(GravesAndGlory.Battle b, ServerPlayerEntity pl, Profile p) {
        if (b == null || pl == null) return;
        var s = b.squads.get(pl.getUuid());
        DyeColor field = s != null && b.running ? Abilities.dye(b.side(s.team).color()) : DyeColor.WHITE;
        dress(pl, p, field);
    }

    /** Puts a squad's commander's layers on the banner block at pos (its field stays the block's own colour). Nothing for a bot, an empty set or no banner there. */
    static void plant(GravesAndGlory.Battle b, GravesAndGlory.Squad s, BlockPos pos) {
        var slots = slots(b, s);
        if (slots == null || pos == null || b.world == null) return;
        var state = b.world.getBlockState(pos);
        if (!(state.getBlock() instanceof AbstractBannerBlock banner) || !(b.world.getBlockEntity(pos) instanceof BannerBlockEntity be)) return;
        var layers = component(slots, banner.getColor().getName(), b.world.getRegistryManager());
        if (layers.layers().isEmpty()) return;
        ItemStack carrier = new ItemStack(BannerBlock.getForColor(banner.getColor()));
        carrier.set(DataComponentTypes.BANNER_PATTERNS, layers);
        be.readFrom(carrier, banner.getColor());
        be.markDirty();
        b.world.updateListeners(pos, state, state, net.minecraft.block.Block.NOTIFY_LISTENERS);
        planted++;
    }
}
