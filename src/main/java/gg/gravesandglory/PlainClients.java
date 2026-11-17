package gg.gravesandglory;

import gg.gravesandglory.polymer.PolymerBridge;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
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
 * The mod-optional server (port plan P3) and the plain front end: battles (troops, orders, the HUD) from P3, and from P4 (Kort,
 * 2026-11-16 evening) formations, relics and the War Table's pages as chest menus (PlainWarTable).
 *
 * <p>On only with Polymer loaded and -Dgravesandglory.plainClients=true (default off: a modpack-only server refuses clients without
 * the mod, as before). Then a vanilla 1.21.1 client joins, flagged plain in configuration (VersionGate), and gets:
 * <ul>
 * <li>troops as vanilla mobs by role, the wagon and map blocks as stand-ins (PlainLooks, through Polymer);</li>
 * <li>a locked hotbar: slots 1 to 5 are the orders (in battle the slot key is the order, the same server call as the modpack keybind),
 *     slot 6 the tactics page, slot 7 the War Table book, slot 8 the practice compass (right-click in camp). In battle the swap-hand
 *     key (F) or slot 6 turns to the tactics page: slots 1 to 4 the formations, 5 to 7 the relic slots, 8 the Armoury (P4);</li>
 * <li>a boss bar, action bar and per-player sidebar (PlainHud) and the Recruit page as a chest menu (PlainRecruit);</li>
 * <li>the plain pack (PlainPack), required, served by PropPack to plain players only.</li>
 * </ul>
 * A plain client never receives a custom payload (VersionGate.canSend) and reads notices in plain words (PlainText).
 */
public final class PlainClients {
    private PlainClients() {}
    public static final boolean ON = PolymerSupport.LOADED && Boolean.getBoolean("gravesandglory.plainClients");
    /** Hotbar slot of each plain item: 0 stays the kit weapon; 1 to 5 the orders (keys 2 to 6), 6 the tactics page (key 7), 7 the
     *  War Table (key 8), 8 practice (key 9). */
    static final Map<Integer, String> HOTBAR = new LinkedHashMap<>();
    /** The battle's tactics page (plan decision 2): 1 to 4 the formations (keys 2 to 5), 5 to 7 relic slots 1 to 3 (keys 6 to 8), 8 the
     *  Armoury (key 9). The wheel from slot 0 reaches only slot 1 (a formation) or 8 (the Armoury): never a relic. */
    static final Map<Integer, String> TACTICS = new LinkedHashMap<>();
    static {
        for (int i = 0; i < PlainPack.ORDERS.length; i++) HOTBAR.put(i + 1, PlainPack.ORDERS[i]);
        HOTBAR.put(6, "tactics");
        HOTBAR.put(7, "wartable");
        HOTBAR.put(8, "practice");
        for (int i = 0; i < Math.min(4, Spec.FORMATIONS.length); i++) TACTICS.put(i + 1, "formation:" + Spec.FORMATIONS[i].id());
        for (int i = 0; i < 3; i++) TACTICS.put(5 + i, "relic:" + i);
        TACTICS.put(8, "armoury");
    }
    /** Plain players on the tactics page (in a battle only). */
    static final Set<UUID> ON_TACTICS = ConcurrentHashMap.newKeySet();
    /** The shape each plain player last chose: the modpack keeps it on the client and sends it with every order; here the server keeps it. */
    static final Map<UUID, String> SHAPE = new ConcurrentHashMap<>();
    /** Per plain player, each relic slot's readyAt the vanilla cooldown sweep was last set for. */
    static final Map<UUID, long[]> SWEPT = new ConcurrentHashMap<>();

    /** At mod init. Polymer's registrations run whenever Polymer is loaded (its server-only marks must be in place before the
     *  registries freeze); the plain front end only when ON. */
    public static void init() {
        if (PolymerSupport.LOADED) PolymerBridge.init();
        if (!ON) return;
        PlainLooks.init();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (PolymerSupport.LOADED) PolymerBridge.warm(PlainLooks.sentTypes());
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
        ServerPlayConnectionEvents.DISCONNECT.register((h, server) -> { PlainHud.forget(h.player); UUID id = h.player.getUuid(); ON_TACTICS.remove(id); SHAPE.remove(id); SWEPT.remove(id); });
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
                if (kind != null) battleKey(p, kind);
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
            default -> PlainWarTable.station(p, station);
        }
    }

    public static String kindOf(ItemStack st) {
        var data = st.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return null;
        var nbt = data.copyNbt();
        return nbt.contains("gg_plain") ? nbt.getString("gg_plain") : null;
    }

    static String label(String kind) { return kind.substring(0, 1).toUpperCase(Locale.ROOT) + kind.substring(1); }

    static ItemStack item(String kind) {
        if (kind.startsWith("formation:") || kind.startsWith("relic:") || kind.equals("armoury")) return tacticsItem(null, kind);
        ItemStack s = kind.equals("wartable") ? new ItemStack(Items.BOOK) : kind.equals("practice") ? new ItemStack(Items.COMPASS) : kind.equals("tactics") ? new ItemStack(Items.MAP) : PolymerBridge.icon(kind);
        if (s == null) s = new ItemStack(Items.PAPER);
        int key = 0;
        for (var e : HOTBAR.entrySet()) if (e.getValue().equals(kind)) key = e.getKey() + 1;
        String name = switch (kind) { case "wartable" -> "War Table"; case "practice" -> "Practice battle"; case "tactics" -> "Tactics"; default -> label(kind) + " order"; };
        String how = kind.equals("wartable") || kind.equals("practice") ? "use it" : kind.equals("tactics") ? "the swap-hand key or key " + key + " in battle" : "key " + key + " in battle";
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name + " (" + how + ")").styled(st -> st.withItalic(false).withColor(Formatting.GOLD)));
        String what = switch (kind) {
            case "attack" -> "Your army hunts the enemy where you aim."; case "charge" -> "Your army charges along your look."; case "hold" -> "Hold the ground you aim at.";
            case "follow" -> "Your army follows you."; case "retreat" -> "Fall back to you, then follow."; case "wartable" -> "Open the War Table: recruit, banner, gear and more.";
            case "tactics" -> "Formations, relics and the Armoury."; default -> "A battle against bots, in camp.";
        };
        s.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal(what).formatted(Formatting.GRAY))));
        NbtCompound tag = new NbtCompound();
        tag.putString("gg_plain", kind);
        s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        return s;
    }

    /** Keeps the plain items in their hotbar slots (the locked hotbar): the orders page, or in a battle the tactics page, whose items
     *  show the chosen shape and each relic's readiness (rebuilt each refresh; a relic's cooldown also shows as the vanilla sweep). */
    public static void hotbar(ServerPlayerEntity p) {
        var inv = p.getInventory();
        var s = seated(p);
        UUID id = p.getUuid();
        if (s == null) { ON_TACTICS.remove(id); SHAPE.remove(id); SWEPT.remove(id); } // the next battle starts from its own shape
        Map<Integer, String> page = s != null && ON_TACTICS.contains(id) ? TACTICS : HOTBAR;
        if (page == TACTICS) {
            for (var e : TACTICS.entrySet()) {
                ItemStack want = tacticsItem(s, e.getValue()), have = inv.getStack(e.getKey());
                if (!ItemStack.areItemsAndComponentsEqual(want, have) || have.getCount() != want.getCount()) inv.setStack(e.getKey(), want);
            }
            relicSweep(p, s);
        } else for (var e : HOTBAR.entrySet()) if (!e.getValue().equals(kindOf(inv.getStack(e.getKey())))) inv.setStack(e.getKey(), item(e.getValue()));
        // P4 review H1: a plain item anywhere but its own slot (moved by the inventory screen, pick-block, the 2x2 grid) is removed,
        // so a refresh can never hand out a second copy to keep, craft or drop.
        for (int i = 0; i < inv.size(); i++) {
            String kind = kindOf(inv.getStack(i));
            if (kind != null && !kind.equals(page.get(i))) inv.setStack(i, ItemStack.EMPTY);
        }
        var grid = p.playerScreenHandler.getCraftingInput();
        for (int i = 0; i < grid.size(); i++) if (kindOf(grid.getStack(i)) != null) grid.setStack(i, ItemStack.EMPTY);
        if (kindOf(p.playerScreenHandler.getCursorStack()) != null) p.playerScreenHandler.setCursorStack(ItemStack.EMPTY);
    }

    /** P4 review H1 (PlainInventoryMixin): a click in any window (the player's own inventory, a barrel, an anvil) that would pick up,
     *  swap or drag a plain hotbar item is refused and the screen resynced; the items stay locked in their slots. True when refused. */
    public static boolean lockedClick(net.minecraft.screen.ScreenHandler h, int slot, int button, net.minecraft.screen.slot.SlotActionType type, net.minecraft.entity.player.PlayerEntity pl) {
        if (!ON || !(pl instanceof ServerPlayerEntity p) || !VersionGate.plain(p)) return false; // any window: own inventory, barrels, anvils (review round 2)
        boolean plain = kindOf(h.getCursorStack()) != null
            || slot >= 0 && slot < h.slots.size() && kindOf(h.slots.get(slot).getStack()) != null
            || type == net.minecraft.screen.slot.SlotActionType.SWAP && (button == 40 ? kindOf(p.getOffHandStack()) != null
                : button >= 0 && button < 9 && kindOf(p.getInventory().getStack(button)) != null);
        if (!plain) return false;
        if (kindOf(h.getCursorStack()) != null) h.setCursorStack(ItemStack.EMPTY); // a real item held stays held
        h.syncState();
        return true;
    }

    /** The squad of a plain player seated in a running battle, else null. */
    static GravesAndGlory.Squad seated(ServerPlayerEntity p) {
        var b = GravesAndGlory.battle;
        if (b == null || !b.running) return null;
        var s = b.squads.get(p.getUuid());
        return s == null || s.bot || !s.seated ? null : s;
    }

    /** The swap-hand key (PlainSwapMixin): in a battle it turns the hotbar page; true when the vanilla swap is to be cancelled (a page
     *  turn, or a plain item in either hand, which never goes to the offhand). */
    public static boolean swapHands(ServerPlayerEntity p) {
        if (!VersionGate.plain(p)) return false;
        if (seated(p) != null) { turnPage(p); return true; }
        return kindOf(p.getMainHandStack()) != null || kindOf(p.getOffHandStack()) != null;
    }

    static void turnPage(ServerPlayerEntity p) {
        boolean tactics = !ON_TACTICS.remove(p.getUuid());
        if (tactics) ON_TACTICS.add(p.getUuid());
        hotbar(p);
        p.sendMessage(Text.literal(tactics ? "Tactics: keys 2-5 formations, 6-8 relics, 9 Armoury. Swap-hand key: orders." : "Orders: keys 2-6. Swap-hand key: tactics.").formatted(Formatting.GOLD), true);
    }

    /** A key pressed in a running battle (the slot key, START_SERVER_TICK): an order, the page turn, a formation, a relic or the Armoury. */
    static void battleKey(ServerPlayerEntity p, String kind) {
        var b = GravesAndGlory.battle;
        if (b == null || kind.equals("practice") || kind.equals("wartable")) return;
        if (kind.equals("tactics")) { turnPage(p); return; }
        if (kind.startsWith("formation:")) { formation(p, kind.substring(10)); return; }
        if (kind.startsWith("relic:")) { b.command(p, kind); PlainHud.notices(p); hotbar(p); return; }
        if (kind.equals("armoury")) { b.command(p, "barracks"); return; } // review L2: Battle.command's gates; openBarracks routes plain players to the Armoury
        order(p, kind);
    }

    /** A formation key: the modpack's formation order (OrderRules.playerOrder's "formation" action, a shape change only, with every gate
     *  the keybind meets), and the shape kept for this player's next hotbar orders. */
    static void formation(ServerPlayerEntity p, String id) {
        var b = GravesAndGlory.battle;
        var s = seated(p);
        if (b == null || s == null || OrderRules.formationId(id) == null) return;
        long before = s.lastOrder;
        OrderRules.playerOrder(b, p, List.of(new OrderRules.At("all", p.getX(), p.getZ())), "formation", id, p.getYaw(), -1);
        if (s.lastOrder != before) SHAPE.put(p.getUuid(), id); // accepted: a refusal leaves the order clock alone
        hotbar(p);
    }

    /** The shape this plain player's hotbar orders carry: the last one chosen, else the squad's. */
    static String shape(ServerPlayerEntity p, GravesAndGlory.Squad s) {
        String f = SHAPE.get(p.getUuid());
        return f != null ? f : s.formation;
    }

    /** Relic slot 1 to 3's vanilla items: each its own, so the cooldown sweep marks one slot. */
    static final Item[] RELIC_ITEMS = {Items.AMETHYST_SHARD, Items.ECHO_SHARD, Items.PRISMARINE_CRYSTALS};

    /** A tactics page item: a formation, a relic slot or the Armoury (s null: no battle state yet). */
    static ItemStack tacticsItem(GravesAndGlory.Squad s, String kind) {
        var b = GravesAndGlory.battle;
        int key = 0;
        for (var e : TACTICS.entrySet()) if (e.getValue().equals(kind)) key = e.getKey() + 1;
        ItemStack st;
        String name;
        List<Text> lore = new ArrayList<>();
        Formatting colour = Formatting.GOLD;
        if (kind.startsWith("formation:")) {
            String id = kind.substring(10);
            st = new ItemStack(switch (id) { case "block" -> Items.SHIELD; case "banner" -> Items.WHITE_BANNER; case "wedge" -> Items.FLINT; default -> Items.FEATHER; });
            boolean on = s != null && id.equals(SHAPE.getOrDefault(s.owner, s.formation));
            name = OrderRules.formationLabel(id) + (on ? " (chosen)" : "");
            if (on) colour = Formatting.GREEN;
            for (var f : Spec.FORMATIONS) if (f.id().equals(id)) lore.add(Text.literal(f.purpose()).formatted(Formatting.GRAY));
            lore.add(Text.literal("Key " + key + ": your army takes this shape; your next orders keep it.").formatted(Formatting.DARK_GRAY));
        } else if (kind.startsWith("relic:")) {
            int slot = Integer.parseInt(kind.substring(6));
            st = new ItemStack(RELIC_ITEMS[slot]);
            String id = s == null || b == null ? "" : Commander.relicIn(s, slot);
            var r = id == null || id.isEmpty() ? null : Commander.relic(id);
            if (r == null) { name = "Relic slot " + (slot + 1) + ": empty"; colour = Formatting.DARK_GRAY; lore.add(Text.literal("Set relics on the War Table's Gear page.").formatted(Formatting.GRAY)); }
            else {
                int ready = Commander.readyIn(b, s, slot), active = Commander.activeFor(b, s, slot);
                name = r.name() + (active > 0 ? " (active " + (active + 19) / 20 + "s)" : ready > 0 ? " (ready in " + (ready + 19) / 20 + "s)" : " (ready)");
                colour = active > 0 ? Formatting.AQUA : ready > 0 ? Formatting.RED : Formatting.GREEN;
                lore.add(Text.literal("Key " + key + ": use it.").formatted(Formatting.DARK_GRAY));
            }
        } else {
            st = new ItemStack(Items.SMITHING_TABLE);
            name = "Armoury";
            lore.add(Text.literal("Train, disband and arm troops at your barracks.").formatted(Formatting.GRAY));
            lore.add(Text.literal("Key " + key + " in battle.").formatted(Formatting.DARK_GRAY));
        }
        final Formatting c = colour;
        st.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).styled(x -> x.withItalic(false).withColor(c)));
        st.set(DataComponentTypes.LORE, new LoreComponent(lore.stream().map(l -> (Text) l.copy().styled(x -> x.withItalic(false))).toList()));
        NbtCompound tag = new NbtCompound();
        tag.putString("gg_plain", kind);
        st.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        return st;
    }

    /** The vanilla cooldown sweep over a relic slot's item, set once each time the slot's cooldown starts. */
    static void relicSweep(ServerPlayerEntity p, GravesAndGlory.Squad s) {
        var b = GravesAndGlory.battle;
        long[] swept = SWEPT.computeIfAbsent(p.getUuid(), k -> new long[3]);
        for (int i = 0; i < Math.min(3, s.relicState.size()); i++) {
            long at = s.relicState.readyAt[i];
            int left = Commander.readyIn(b, s, i);
            if (left > 0 && swept[i] != at) { p.getItemCooldownManager().set(RELIC_ITEMS[i], left); swept[i] = at; }
        }
    }

    static void use(ServerPlayerEntity p, String kind) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        switch (kind) {
            case "practice" -> b.command(p, "camp:practice");
            case "wartable" -> PlainWarTable.open(p);
            case "tactics" -> { if (seated(p) != null) turnPage(p); }
            default -> battleKey(p, kind);
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
        OrderRules.playerOrder(b, p, List.of(new OrderRules.At("all", at.x, at.z)), action, shape(p, s), p.getYaw(), -1);
    }
}
