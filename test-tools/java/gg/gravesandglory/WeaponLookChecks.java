package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.*;

/**
 * Weapons stay out of Excalibur's look (Kort, 2026-11-05). GRAVES_PHASE1_CHECKS=weapon-look. In a practice battle the server puts each
 * weapon in the player's hand; first person and third person (front) shots are p1-weapon-<item>-<fp|tp>.png. Also checks that the
 * enabled pack order has our armory and weapons packs above Excalibur. Never shipped.
 */
final class WeaponLookChecks implements Phase1Checks.Suite {
    static final String[] ITEMS = {"iron_sword", "golden_sword", "diamond_sword", "netherite_sword", "iron_axe", "bow", "crossbow", "trident", "shield"};
    volatile UUID player;
    volatile boolean battleReady;
    int stage, at, i;
    String why = "";

    void next(int tick, int s) { stage = s; at = tick; }

    @Override public void cleanup(MinecraftClient c) {
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
        if (c.getServer() != null) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 20000 && stage < 9) { Phase1Checks.report("p1-weapon-look-timeout", false, "stuck at stage " + stage); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); next(tick, 1); } }
            case 1 -> {
                if (!battleReady) {
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true; });
                    if (k > 600) { Phase1Checks.report("p1-weapon-look", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                if (k > 80) {
                    var on = new ArrayList<>(c.getResourcePackManager().getEnabledNames());
                    int e = on.indexOf(LookPacks.EXCALIBUR), a = on.indexOf(LookPacks.ARMORY), w = on.indexOf(LookPacks.WEAPONS);
                    boolean ok = e >= 0 && a > e && w > e;
                    System.out.println("[p1] weapon-look: enabled packs " + on);
                    Phase1Checks.report("p1-weapon-look-pack-order", ok, "enabled " + on);
                    i = 0; next(tick, 2);
                }
            }
            case 2 -> {
                String item = ITEMS[i];
                if (k == 1) Phase1Checks.server(c, b -> {
                    var p = b.server.getPlayerManager().getPlayer(player);
                    if (p == null) return;
                    p.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Registries.ITEM.get(new Identifier("minecraft:" + item))));
                    p.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                });
                if (k == 2) { c.setScreen(null); c.options.hudHidden = false; c.options.setPerspective(Perspective.FIRST_PERSON); }
                if (k == 30) Phase1Checks.shot(c, "weapon-" + item + "-fp");
                if (k == 32) c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                if (k == 60) Phase1Checks.shot(c, "weapon-" + item + "-tp");
                if (k == 62) { if (++i < ITEMS.length) next(tick, 2); else { Phase1Checks.report("p1-weapon-look", true, ITEMS.length + " weapons shot in both views"); next(tick, 9); } }
            }
            case 9 -> {
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
                if (k > 40) { Phase1Checks.finish(c, "weapon-look"); stage = 99; }
            }
            default -> {}
        }
    }
}
