package gg.gravesandglory;

import gg.gravesandglory.polymer.PolymerBridge;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

/**
 * The mod-optional server (port plan P3) and the plain front end Kort approved for the first plain release (2026-11-16: battles only,
 * that is troops, orders, the HUD and the Recruit menu; formations, relics and the other War Table pages are not offered yet).
 *
 * <p>On only with Polymer loaded and -Dgravesandglory.plainClients=true (default off: a modpack-only server refuses clients without
 * the mod, as before). Then a vanilla 1.21.1 client joins, flagged plain in configuration (VersionGate), and gets:
 * <ul>
 * <li>troops as vanilla mobs by role, the wagon and map blocks as stand-ins (PlainLooks, through Polymer);</li>
 * <li>a locked hotbar: slots 1 to 5 are the orders (in battle the slot key is the order, the same server call as the modpack keybind),
 *     slot 7 the Recruit book, slot 8 the practice compass (right-click in camp);</li>
 * <li>a boss bar, action bar and per-player sidebar (PlainHud) and the Recruit page as a chest menu (PlainRecruit);</li>
 * <li>the plain pack (PlainPack), required, served by PropPack to plain players only.</li>
 * </ul>
 * A plain client never receives a custom payload (VersionGate.canSend) and reads notices in plain words (PlainText).
 */
public final class PlainClients {
    private PlainClients() {}
    public static final boolean ON = PolymerSupport.LOADED && Boolean.getBoolean("gravesandglory.plainClients");
    /** Hotbar slot of each plain item: 0 stays the kit weapon; 1 to 5 the orders (keys 2 to 6), 7 Recruit (key 8), 8 practice (key 9). */
    static final Map<Integer, String> HOTBAR = new LinkedHashMap<>();
    static {
        for (int i = 0; i < PlainPack.ORDERS.length; i++) HOTBAR.put(i + 1, PlainPack.ORDERS[i]);
        HOTBAR.put(7, "recruit");
        HOTBAR.put(8, "practice");
    }

    /** At mod init. Polymer's registrations run whenever Polymer is loaded (its server-only marks must be in place before the
     *  registries freeze); the plain front end only when ON. */
    public static void init() {
        if (PolymerSupport.LOADED) PolymerBridge.init();
        if (!ON) return;
        PlainLooks.init();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path zip = Path.of("plain-pack.zip").toAbsolutePath();
            boolean ok = PolymerBridge.buildPack(zip);
            long size = -1;
            try { size = Files.size(zip); } catch (java.io.IOException ignored) {}
            System.out.println("[Graves and Glory] Plain clients on; plain pack built " + (ok ? "ok" : "FAILED") + " " + zip + " (" + size + " bytes)");
            // Registered before PropPack.init, so the zip exists when PropPack.start (SERVER_STARTED too) reads it.
        });
        ServerPlayConnectionEvents.JOIN.register((h, sender, server) -> {
            var p = h.player;
            PlainHud.forget(p);
            if (VersionGate.plain(p))
                p.sendMessage(Text.literal("Playing without the modpack: your orders are on the hotbar. The Graves and Glory modpack adds troop models and the full War Table.").formatted(Formatting.GRAY));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h, server) -> PlainHud.forget(h.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 10 != 0) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (!VersionGate.plain(p)) continue;
                try { hotbar(p); PlainHud.tick(p); }
                catch (RuntimeException e) { System.err.println("[Graves and Glory] Plain HUD failed for " + p.getName().getString() + ": " + e); }
            }
        });
        // In a running battle GearCombat snaps a seated commander's hotbar back to slot 0 every tick, so a plain player cannot hold an
        // order item to right-click it. The slot key is the order instead: set_carried_item is handled before START_SERVER_TICK, so a
        // non-zero slot here is this tick's key press. The order goes out and the hotbar snaps back to the weapon.
        // Known gap (P4.2): the mouse wheel also changes the slot, so one wheel notch in battle gives an order.
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            var b = GravesAndGlory.battle;
            if (b == null || !b.running) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                int slot = p.getInventory().selectedSlot;
                if (slot == 0 || !VersionGate.plain(p)) continue;
                var s = b.squads.get(p.getUuid());
                if (s == null || s.bot || !s.seated) continue;
                String kind = kindOf(p.getInventory().getStack(slot));
                if (kind != null && !kind.equals("practice") && !kind.equals("recruit")) order(p, kind);
                p.getInventory().selectedSlot = 0;
                p.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(0));
            }
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack st = player.getStackInHand(hand);
            String kind = kindOf(st);
            if (kind == null || world.isClient() || !(player instanceof ServerPlayerEntity sp) || !VersionGate.plain(sp)) return TypedActionResult.pass(st);
            use(sp, kind);
            return TypedActionResult.success(st);
        });
    }

    /** A camp station or lectern used by a plain player (GravesAndGlory's use callbacks, which send the modpack's screens to modded
     *  players only). The barracks lectern opens Recruit, the drillmaster starts a practice battle; the rest need the modpack. */
    static void station(ServerPlayerEntity p, String station) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        switch (station) {
            case "army" -> PlainRecruit.open(p, null);
            case "train" -> b.command(p, "camp:practice");
            default -> p.sendMessage(Text.literal("This needs the Graves and Glory modpack. Recruit with the book in hotbar slot 8.").formatted(Formatting.GRAY), true);
        }
    }

    static String kindOf(ItemStack st) {
        var data = st.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return null;
        var nbt = data.copyNbt();
        return nbt.contains("gg_plain") ? nbt.getString("gg_plain") : null;
    }

    static String label(String kind) { return kind.substring(0, 1).toUpperCase(Locale.ROOT) + kind.substring(1); }

    static ItemStack item(String kind) {
        ItemStack s = kind.equals("recruit") ? new ItemStack(Items.BOOK) : kind.equals("practice") ? new ItemStack(Items.COMPASS) : PolymerBridge.icon(kind);
        if (s == null) s = new ItemStack(Items.PAPER);
        int key = 0;
        for (var e : HOTBAR.entrySet()) if (e.getValue().equals(kind)) key = e.getKey() + 1;
        String name = switch (kind) { case "recruit" -> "Recruit"; case "practice" -> "Practice battle"; default -> label(kind) + " order"; };
        String how = kind.equals("recruit") || kind.equals("practice") ? "use it" : "key " + key + " in battle";
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name + " (" + how + ")").styled(st -> st.withItalic(false).withColor(Formatting.GOLD)));
        String what = switch (kind) {
            case "attack" -> "Your army hunts the enemy where you aim."; case "charge" -> "Your army charges along your look."; case "hold" -> "Hold the ground you aim at.";
            case "follow" -> "Your army follows you."; case "retreat" -> "Fall back to you, then follow."; case "recruit" -> "Open the Recruit page."; default -> "A battle against bots, in camp.";
        };
        s.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal(what).formatted(Formatting.GRAY))));
        NbtCompound tag = new NbtCompound();
        tag.putString("gg_plain", kind);
        s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        return s;
    }

    /** Keeps the plain items in their hotbar slots (the locked hotbar). */
    static void hotbar(ServerPlayerEntity p) {
        var inv = p.getInventory();
        for (var e : HOTBAR.entrySet()) if (!e.getValue().equals(kindOf(inv.getStack(e.getKey())))) inv.setStack(e.getKey(), item(e.getValue()));
    }

    static void use(ServerPlayerEntity p, String kind) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        switch (kind) {
            case "practice" -> b.command(p, "camp:practice");
            case "recruit" -> PlainRecruit.open(p, null);
            default -> order(p, kind);
        }
    }

    /**
     * One order from the hotbar: the modpack keybind's order, aimed on the server. The keybind aims on the client (CommandMode.tick:
     * a raycast to COMMANDINTERFACE.targetRange, a Charge from the army's centre) and sends tactical_v2, whose receiver calls
     * OrderRules.playerOrder; this aims the same way and calls the same method, so every gate (seat, intro, respawn, the order
     * cooldown, the Charge cooldown) is the one the keybind order meets.
     */
    static void order(ServerPlayerEntity p, String action) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        var s = b.squads.get(p.getUuid());
        if (s == null) return;
        Vec3d at;
        if (action.equals("charge")) {
            double x = 0, z = 0; int n = 0;
            for (var u : b.fielded(s).toList()) { x += u.mob.getX(); z += u.mob.getZ(); n++; }
            at = n == 0 ? p.getPos() : new Vec3d(x / n, p.getY(), z / n);
        } else {
            HitResult hit = p.raycast(Spec.COMMANDINTERFACE[0].targetRange(), 1, false);
            if (hit.getType() == HitResult.Type.MISS) { p.sendMessage(Text.literal("Aim at reachable terrain"), true); return; } // CommandMode.tick refuses a miss the same way
            at = hit.getPos();
        }
        OrderRules.playerOrder(b, p, List.of(new OrderRules.At("all", at.x, at.z)), action, s.formation, p.getYaw(), -1);
    }
}
