package gg.gravesandglory;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.dimension.DimensionType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Placement eggs for the camp NPCs (Kort 2026-11-16). A builder or owner uses an NPC's egg on a block and the NPC stands there, facing them;
 * placing one that already stands moves it. The spot is written at once to the map's file in the world save (CampNpcPlacements), and it
 * ships with the map when adopted (tools/mapforge adopt npcs). Two places take an egg:
 * - the live lobby camp: the NPC, its click box and its label move (CampWorld.respawn), and the file is the camp's (map camp-4, from the camp origin);
 * - a builder canvas (-Dgg.build): there is no camp there, so a marker villager stands in for the NPC, and the file is the canvas's (map = the canvas id, from 0,0).
 * /campnpc egg <id|all>, /campnpc reset <id|all> and /campnpc list are for the same people: op level 2 or the owner (UiEdits.owner).
 */
final class CampNpcEggs {
    static final String KEY = "gg_npc", MARKER_TAG = "gg-npc-marker";
    /** On the villager a dispenser (or anything else that skips the use callbacks) hatches from an egg: discarded the moment it loads. */
    static final String DUD_TAG = "gg-npc-egg-dud";
    /** Each builder canvas's placements as last read or written, keyed by its world: the marker clean-up on entity load compares against it. */
    static final Map<net.minecraft.registry.RegistryKey<net.minecraft.world.World>, Map<String, CampNpcPlacements.Place>> CANVAS = new java.util.concurrent.ConcurrentHashMap<>();
    /** The server tick a player's egg was last used on a block or entity: the item use vanilla sends after it in the same tick is swallowed (as PropMenu does). */
    static final Map<UUID, Integer> USED = new HashMap<>();

    static boolean allowed(ServerPlayerEntity p) { return p.hasPermissionLevel(2) || UiEdits.owner(p); }
    static boolean allowed(ServerCommandSource s) { return s.hasPermissionLevel(2) || s.getPlayer() != null && UiEdits.owner(s.getPlayer()); }

    static Spec.CampStationsRow row(String id) {
        for (var s : Spec.CAMPSTATIONS) if (s.id().equals(id) && !s.npc().equals("-")) return s;
        return null;
    }
    /** The character's name (the campNpcs row's name), else the villager name. */
    static String who(Spec.CampStationsRow s) { var look = CampNpcs.look(s); return look != null ? look.name() : CampNpcs.name(s); }

    /** The NPC id an egg carries, or null for any other item (a plain villager spawn egg too). */
    static String eggId(ItemStack st) {
        if (!st.isOf(Items.VILLAGER_SPAWN_EGG)) return null;
        var data = st.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return null;
        var nbt = data.copyNbt();
        return nbt.contains(KEY) ? nbt.getString(KEY) : null;
    }
    static ItemStack egg(Spec.CampStationsRow s) {
        var st = new ItemStack(Items.VILLAGER_SPAWN_EGG);
        var nbt = new NbtCompound(); nbt.putString(KEY, s.id());
        st.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        // a dispenser hatches spawn eggs without the use callbacks: what it hatches carries DUD_TAG and is discarded on load
        var entity = new NbtCompound(); entity.putString("id", "minecraft:villager");
        var tags = new net.minecraft.nbt.NbtList(); tags.add(net.minecraft.nbt.NbtString.of(DUD_TAG)); entity.put("Tags", tags);
        st.set(DataComponentTypes.ENTITY_DATA, NbtComponent.of(entity));
        st.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Place " + who(s)).formatted(Formatting.GOLD));
        st.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal("Use on a block: " + who(s) + " stands there, facing you.").formatted(Formatting.GRAY),
                Text.literal("Placing again moves them. /campnpc reset " + s.id() + " undoes it.").formatted(Formatting.GRAY))));
        return st;
    }

    // ---- the files ----

    static Path root(ServerWorld w) { return w.getServer().getSavePath(WorldSavePath.ROOT); }
    static Path file(Path dimension, String map) { return dimension.resolve(CampNpcPlacements.fileName(map)); }
    static Map<String, CampNpcPlacements.Place> read(Path file, String map) {
        return SafeFiles.load(file, f -> CampNpcPlacements.parse(Files.readString(f, StandardCharsets.UTF_8), map), Map::of, "NPC placements " + file.getFileName());
    }
    /** A file read for a write: missing is empty, but a damaged file throws, so a save never replaces placements it could not read. */
    static Map<String, CampNpcPlacements.Place> strict(Path file, String map) throws Exception {
        return Files.exists(file) ? CampNpcPlacements.parse(Files.readString(file, StandardCharsets.UTF_8), map) : Map.of();
    }
    /** Writes a placements file with a .bak of the one before (SafeFiles.load falls back to it). "" when saved, else why not. */
    static String save(Path file, String map, Map<String, CampNpcPlacements.Place> places) {
        try { SafeFiles.writeWithBackup(file, CampNpcPlacements.write(map, places)); return ""; }
        catch (Exception e) { SafeFiles.warn.accept("NPC placements " + file.getFileName() + " could not be written: " + e); return "The placement could not be saved; see the server log."; }
    }
    static Map<String, CampNpcPlacements.Place> shipped(String map) {
        try (InputStream in = CampNpcEggs.class.getResourceAsStream(CampNpcPlacements.resource(map))) {
            if (in == null) return Map.of();
            return CampNpcPlacements.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), map);
        } catch (Exception e) { SafeFiles.warn.accept("shipped NPC placements for " + map + " are ignored: " + e.getMessage()); return Map.of(); }
    }
    /** The live camp's placements, before its people spawn (CampWorld.build). */
    static void load(ServerWorld w) {
        String map = Spec.CAMP[0].id();
        CampWorld.placedShipped = shipped(map);
        CampWorld.placedWorld = read(file(DimensionType.getSaveDirectory(w.getRegistryKey(), root(w)), map), map);
    }

    // ---- placing ----

    /** Places (place != null) or resets (null) one NPC where the player is: the live camp or their build canvas. "" when done, else why not. */
    static String set(ServerPlayerEntity p, Spec.CampStationsRow s, CampNpcPlacements.Place place) {
        ServerWorld w = p.getServerWorld();
        // the same limits parse() reads back: a spot it would refuse would make the whole file unreadable on the next load
        if (place != null && (Math.abs(place.dx()) > CampNpcPlacements.REACH || Math.abs(place.dz()) > CampNpcPlacements.REACH || place.y() < CampNpcPlacements.MIN_Y || place.y() > CampNpcPlacements.MAX_Y)) return "That is too far from the map's origin to save.";
        if (BuildMode.active()) {
            var plan = BuildMode.plan(p);
            if (plan == null) return "Stand in a build canvas to place a camp NPC.";
            String map = plan.id();
            if (!CampNpcPlacements.validMap(map)) return "This canvas id cannot name a file: " + map;
            Path f = file(DimensionType.getSaveDirectory(plan.key(), BuildMode.root()), map);
            Map<String, CampNpcPlacements.Place> now;
            try { now = strict(f, map); } catch (Exception e) { return "The canvas's NPC file " + f.getFileName() + " is damaged (" + e.getMessage() + "); fix or remove it first."; }
            var next = CampNpcPlacements.with(now, s.id(), place);
            String why = save(f, map, next);
            if (!why.isEmpty()) return why;
            CANVAS.put(plan.key(), Map.copyOf(next)); // before the marker spawns: its load is checked against this
            marker(w, s, place);
            return "";
        }
        var b = GravesAndGlory.battle;
        if (b == null || w != b.world) return "Camp NPCs are placed in the lobby camp or on a build canvas.";
        var c = Spec.CAMP[0];
        if (place != null && !CampWorld.inCamp(c.originX() + place.dx(), place.y(), c.originZ() + place.dz())) return "That is outside the camp.";
        String map = c.id();
        Path f = file(DimensionType.getSaveDirectory(w.getRegistryKey(), root(w)), map);
        var next = CampNpcPlacements.with(CampWorld.placedWorld, s.id(), place);
        String why = save(f, map, next);
        if (!why.isEmpty()) return why;
        CampWorld.placedWorld = Map.copyOf(next);
        CampWorld.respawn(w, s);
        return "";
    }
    /** A builder canvas's stand-in: the NPC's villager (a modded client draws its look), tagged as a marker; the old marker of that NPC in the canvas goes first. place null only removes it. */
    static void marker(ServerWorld w, Spec.CampStationsRow s, CampNpcPlacements.Place place) {
        var old = new ArrayList<Entity>();
        for (var e : w.iterateEntities()) if (e.getCommandTags().contains(MARKER_TAG) && e.getCommandTags().contains(CampWorld.npcTag(s.id()))) old.add(e);
        old.forEach(Entity::discard);
        if (place == null) return;
        var v = CampWorld.villager(w, s, new CampWorld.At(place.dx(), place.y(), place.dz(), place.yaw()));
        v.addCommandTag(MARKER_TAG); v.addCommandTag(CampWorld.npcTag(s.id()));
        w.spawnEntity(v);
    }
    /** The placement an egg used on a block makes: on top of the block's collision when its top face is clicked, else on the floor of the cell beside the clicked face; centred, facing the player. */
    static CampNpcPlacements.Place placeAt(ServerPlayerEntity p, BlockPos pos, Direction side) {
        ServerWorld w = p.getServerWorld();
        BlockPos cell; double y;
        if (side == Direction.UP) {
            var shape = w.getBlockState(pos).getCollisionShape(w, pos);
            cell = pos; y = pos.getY() + (shape.isEmpty() ? 0 : shape.getMax(Direction.Axis.Y));
        } else { cell = pos.offset(side); y = cell.getY(); }
        double ox = 0, oz = 0;
        if (!BuildMode.active()) { ox = Spec.CAMP[0].originX(); oz = Spec.CAMP[0].originZ(); }
        return new CampNpcPlacements.Place(cell.getX() + 0.5 - ox, y, cell.getZ() + 0.5 - oz, CampNpcPlacements.facing(p.getYaw()));
    }
    /**
     * A camp NPC's entity as it loads (a chunk coming back, or a spawn): a dud hatched from an egg goes, and so does a villager, click box or
     * stand-in left behind in an unloaded chunk when its NPC moved (respawn and marker only reach loaded entities): one whose x/z is not its
     * NPC's current spot. Labels get their camp-npc tag after they spawn, so a new one is never checked here; an old one from disk is.
     */
    static void loaded(Entity e, ServerWorld w) {
        var tags = e.getCommandTags();
        if (tags.contains(DUD_TAG)) { e.discard(); return; }
        String id = null;
        for (String t : tags) if (t.startsWith("camp-npc:")) id = t.substring(9);
        if (id == null) return;
        var s = row(id);
        double x, z;
        if (tags.contains(MARKER_TAG)) {
            if (!BuildMode.active()) return;
            var places = canvas(w);
            if (places == null) return; // unreadable: leave it
            var p = places.get(id);
            if (s == null || p == null) { e.discard(); return; }
            x = p.dx(); z = p.dz();
        } else if (tags.contains(CampWorld.TAG)) {
            var b = GravesAndGlory.battle;
            if (b == null || w != b.world || CampWorld.generation.isEmpty() || !tags.contains("camp-generation:" + CampWorld.generation)) return; // older generations: GravesAndGlory's own clean-up
            if (s == null) return;
            var a = CampWorld.at(s); x = a.x(); z = a.z();
        } else return;
        if (Math.abs(e.getX() - x) > 0.05 || Math.abs(e.getZ() - z) > 0.05) e.discard();
    }
    /** A canvas world's placements (cached), or null when it is no canvas or its file cannot be read. */
    static Map<String, CampNpcPlacements.Place> canvas(ServerWorld w) {
        var cached = CANVAS.get(w.getRegistryKey());
        if (cached != null) return cached;
        for (var plan : BuildMode.PLANS.values()) if (plan.key() == w.getRegistryKey()) {
            try {
                var places = Map.copyOf(strict(file(DimensionType.getSaveDirectory(plan.key(), BuildMode.root()), plan.id()), plan.id()));
                CANVAS.put(plan.key(), places);
                return places;
            } catch (Exception ex) { return null; }
        }
        return null;
    }

    static void tell(ServerPlayerEntity p, String why, String done) {
        p.sendMessage(why.isEmpty() ? Text.literal(done).formatted(Formatting.GREEN) : Text.literal(why).formatted(Formatting.RED), false);
    }

    /** Before the station callbacks (GravesAndGlory registers this first): an egg never opens a station, never spawns a vanilla villager, and does nothing for anyone else. */
    static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            String id = eggId(player.getStackInHand(hand));
            if (id == null || world.isClient || !(player instanceof ServerPlayerEntity p)) return ActionResult.PASS;
            USED.put(p.getUuid(), p.getServer().getTicks());
            if (hand != Hand.MAIN_HAND) return ActionResult.FAIL;
            if (!allowed(p)) { tell(p, "Only builders and owners can place camp NPCs.", ""); return ActionResult.FAIL; }
            var s = row(id);
            if (s == null) { tell(p, "No camp NPC is called " + id + ".", ""); return ActionResult.FAIL; }
            var place = placeAt(p, hit.getBlockPos(), hit.getSide());
            String why = set(p, s, place);
            tell(p, why, String.format(java.util.Locale.ROOT, "%s stands at %.1f, %.1f, %.1f, facing %.0f. Saved.", who(s), place.dx(), place.y(), place.dz(), place.yaw()));
            return why.isEmpty() ? ActionResult.SUCCESS : ActionResult.FAIL;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (entity.getCommandTags().contains(MARKER_TAG)) return ActionResult.FAIL; // a canvas stand-in is not a trader
            if (eggId(player.getStackInHand(hand)) == null) return ActionResult.PASS;
            if (!world.isClient && player instanceof ServerPlayerEntity p) { USED.put(p.getUuid(), p.getServer().getTicks()); if (hand == Hand.MAIN_HAND) tell(p, "Use the egg on a block where the NPC should stand.", ""); }
            return ActionResult.FAIL;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack held = player.getStackInHand(hand);
            if (eggId(held) == null || world.isClient || !(player instanceof ServerPlayerEntity p)) return TypedActionResult.pass(held);
            Integer used = USED.get(p.getUuid());
            if (used == null || p.getServer().getTicks() - used > 1) if (hand == Hand.MAIN_HAND) tell(p, "Use the egg on a block where the NPC should stand.", "");
            return TypedActionResult.fail(held);
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h, server) -> USED.remove(h.player.getUuid()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register(CampNpcEggs::loaded);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> CANVAS.clear());
        CommandRegistrationCallback.EVENT.register((d, access, env) -> d.register(CommandManager.literal("campnpc").requires(CampNpcEggs::allowed)
            .then(CommandManager.literal("egg").then(CommandManager.argument("id", StringArgumentType.word()).suggests((c, b) -> { b.suggest("all"); CampNpcPlacements.ids().forEach(b::suggest); return b.buildFuture(); })
                .executes(c -> {
                    var p = c.getSource().getPlayerOrThrow(); String id = StringArgumentType.getString(c, "id"); int n = 0;
                    for (var s : Spec.CAMPSTATIONS) if (!s.npc().equals("-") && (id.equals("all") || s.id().equals(id))) { p.getInventory().offerOrDrop(egg(s)); n++; }
                    if (n == 0) { tell(p, "No camp NPC is called " + id + ".", ""); return 0; }
                    tell(p, "", n + " placement egg" + (n == 1 ? "" : "s") + " given."); return n;
                })))
            .then(CommandManager.literal("reset").then(CommandManager.argument("id", StringArgumentType.word()).suggests((c, b) -> { b.suggest("all"); CampNpcPlacements.ids().forEach(b::suggest); return b.buildFuture(); })
                .executes(c -> {
                    var p = c.getSource().getPlayerOrThrow(); String id = StringArgumentType.getString(c, "id"); int n = 0;
                    for (var s : Spec.CAMPSTATIONS) if (!s.npc().equals("-") && (id.equals("all") || s.id().equals(id))) {
                        String why = set(p, s, null);
                        if (!why.isEmpty()) { tell(p, why, ""); return n; }
                        n++;
                    }
                    if (n == 0) { tell(p, "No camp NPC is called " + id + ".", ""); return 0; }
                    tell(p, "", n + " NPC" + (n == 1 ? "" : "s") + (BuildMode.active() ? " removed from this canvas." : " back to the shipped or default spot.")); return n;
                })))
            .then(CommandManager.literal("list").executes(c -> {
                var src = c.getSource();
                if (BuildMode.active()) {
                    var pl = src.getPlayer() == null ? null : BuildMode.plan(src.getPlayer());
                    var places = pl == null ? null : canvas(src.getWorld());
                    if (places == null) { src.sendFeedback(() -> Text.literal("Stand in a build canvas whose NPC file can be read."), false); return 0; }
                    if (places.isEmpty()) src.sendFeedback(() -> Text.literal("No camp NPCs placed on " + pl.id() + "."), false);
                    for (var e : new java.util.TreeMap<>(places).entrySet()) src.sendFeedback(() -> Text.literal(String.format(java.util.Locale.ROOT, "%s: %.1f, %.1f, %.1f yaw %.0f [%s]", e.getKey(), e.getValue().dx(), e.getValue().y(), e.getValue().dz(), e.getValue().yaw(), pl.id())), false);
                    return 1;
                }
                for (var s : Spec.CAMPSTATIONS) if (!s.npc().equals("-")) {
                    var a = CampWorld.at(s);
                    String from = CampWorld.placedWorld.containsKey(s.id()) ? "placed" : CampWorld.placedShipped.containsKey(s.id()) ? "shipped" : "sheet";
                    src.sendFeedback(() -> Text.literal(String.format(java.util.Locale.ROOT, "%s (%s): %.1f, %.1f, %.1f yaw %.0f [%s]", s.id(), who(s), a.x(), a.y(), a.z(), a.yaw(), from)), false);
                }
                return 1;
            }))));
    }
}
