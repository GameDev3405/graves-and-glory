package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.*;

/**
 * Real-client shots of the five House armour sets Kort approved on 11-04 (art only: no items, catalogue or drops until his #113).
 * GRAVES_PHASE1_CHECKS=house-art. In a practice battle the server puts stand-in stacks straight on the player, each carrying only
 * gg_item and gg_rarity, the NBT CommanderGearFeature draws a model from (GearLooks.art); no profile piece is made, so nothing is
 * saved. Per set: Legendary from the front, right, back and left, the head turned 45 degrees each way, and Common from the front
 * with the head turned (p1-house-art-<set>). Shots are p1-house-art-<set>-<rarity>-<view>.png. Never shipped.
 */
final class HouseArtChecks implements Phase1Checks.Suite {
    static final List<String> SETS = List.of("oathbound", "barrow_warden", "thornhide", "forgewall", "tidewrought");
    static final String[] PIECES = {"helm", "chest", "legs", "boots"};
    static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    static final String[] STAND_IN = {"minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots"};
    /** (rarity, label, body turn, head turn from the body) per shot. */
    static final Object[][] VIEWS = {
        {5, "front", 0, 0}, {5, "right", 90, 0}, {5, "back", 180, 0}, {5, "left", 270, 0},
        {5, "head-left", 0, 45}, {5, "head-right", 0, -45}, {1, "front", 0, 0}, {1, "head-left", 0, 45}};
    volatile UUID player;
    volatile boolean battleReady, equipped;
    int stage, at, si, vi;
    float yaw;
    String why = "";

    static void say(String line) { System.out.println("[p1] house-art: " + line); }
    void next(int tick, int s) { stage = s; at = tick; }

    static ItemStack stand(int slot, String item, int rarity) {
        ItemStack s = new ItemStack(Registries.ITEM.get(new Identifier(STAND_IN[slot])));
        var nbt = s.getOrCreateNbt();
        nbt.putString(GearLooks.ITEM, item); nbt.putInt(GearLooks.RARITY, rarity); nbt.putBoolean("Unbreakable", true);
        return s;
    }

    @Override public void cleanup(MinecraftClient c) {
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
        if (c.getServer() != null) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 20000 && stage < 9) { Phase1Checks.report("p1-house-art-timeout", false, "stuck at stage " + stage); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); next(tick, 1); } }
            case 1 -> {
                if (!battleReady) {
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true; });
                    if (k > 600) { Phase1Checks.report("p1-house-art", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                if (k > 80) { si = 0; vi = 0; next(tick, 2); }
            }
            case 2 -> { // one shot a pass
                String set = SETS.get(si); Object[] v = VIEWS[vi]; int rarity = (Integer) v[0];
                if (k == 1) {
                    equipped = false;
                    Phase1Checks.server(c, b -> {
                        var p = b.server.getPlayerManager().getPlayer(player);
                        if (p == null) return;
                        for (int i = 0; i < 4; i++) p.equipStack(SLOTS[i], stand(i, set + "_" + PIECES[i], rarity));
                        p.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY); p.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                        equipped = true;
                    });
                }
                if (k == 2) { c.setScreen(null); c.options.hudHidden = true; c.options.setPerspective(Perspective.THIRD_PERSON_FRONT); yaw = c.player.getYaw(); }
                if (k >= 2) {
                    float body = yaw + (Integer) v[2], head = body + (Integer) v[3];
                    c.player.setYaw(yaw); c.player.setPitch(0); c.player.prevYaw = yaw; c.player.prevPitch = 0;
                    c.player.setBodyYaw(body); c.player.prevBodyYaw = body; c.player.setHeadYaw(head); c.player.prevHeadYaw = head;
                }
                if (k == 25) {
                    Set<String> want = new TreeSet<>(), worn = new TreeSet<>();
                    for (String p : PIECES) want.add(set + "_" + p);
                    for (var slot : SLOTS) { var a = GearLooks.art(c.player.getEquippedStack(slot)); if (a != null && a.rarity() == rarity) worn.add(a.item()); }
                    if (!worn.equals(want)) why += set + " r" + rarity + " " + v[1] + ": wears " + worn + "; ";
                    else if (!CommanderGearFeature.drawn.containsAll(want)) why += set + " r" + rarity + " " + v[1] + ": drew " + new TreeSet<>(CommanderGearFeature.drawn) + "; ";
                    Phase1Checks.shot(c, "house-art-" + set + "-r" + rarity + "-" + v[1]);
                }
                if (k == 30) {
                    if (++vi < VIEWS.length) { next(tick, 2); return; }
                    vi = 0;
                    if (++si < SETS.size()) { next(tick, 2); return; }
                    Phase1Checks.report("p1-house-art", why.isEmpty(), why.isEmpty() ? "five House sets drawn by their own models, eight shots each" : why);
                    next(tick, 9);
                }
            }
            case 9 -> {
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
                if (k > 40) { Phase1Checks.finish(c, "house-art"); stage = 99; }
            }
            default -> {}
        }
    }
}
