package gg.gravesandglory;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.RaycastContext;
import java.util.*;

/**
 * Camp NPCs on a modded client (lobby NPC spec 3.1 and 5): GRAVES_PHASE1_CHECKS=camp-npcs. For each of the five staff NPCs (blacksmith,
 * quartermaster, archivist, steward, drillmaster) in turn: the server's villager has the spec's profession and biome skin and an interaction
 * box of its look's size, tagged camp:<page>; the client draws it through its look's GeckoLib rig (per-look draw counters, so two NPCs a few
 * blocks apart are told apart), while a villager the suite spawns with the same name but its AI on, and an unnamed one, draw vanilla;
 * CampWorld.swing starts its interact clip, and a real click on its interaction box opens the camp screen on its station's page and plays the
 * clip again. The camera stands on the first free spot with a clear view of the NPC (front first, then the sides). One shot per NPC:
 * p1-camp-npc-<id>-idle. Madame Fleece (tailor, Kort 2026-11-16) has no screen yet: her click opens none and says the Wardrobe opens soon.
 * Then the placement egg (p1-camp-npc-eggs): her egg used on a block inside her station's zone moves her villager, click box, label and zone
 * there (it does not open the station or spawn a vanilla villager), the camp's file in the world save holds the spot and a reload of it keeps
 * it (a restart), and a reset puts her back on her row's spot. Shot p1-camp-npc-tailor-placed. Never shipped.
 */
final class CampNpcChecks implements Phase1Checks.Suite {
    static final String SPAWNED = "camp-npc-check";
    static final String[] NPCS = {"blacksmith", "quartermaster", "archivist", "steward", "drillmaster", "tailor"};
    volatile UUID player;
    volatile String serverWhy;
    volatile boolean placed;
    /** The entity ids of the two vanilla villagers spawned for the current NPC (the integrated server's ids are the client's). */
    final int[] spawnedIds = new int[2];
    int stage, at, index;
    long draws, played;
    String why = "";
    final List<String> passed = new ArrayList<>();

    static void say(String line) { System.out.println("[p1] camp-npcs: " + line); }
    void next(int tick, int s) { stage = s; at = tick; }

    static Spec.CampStationsRow row(String id) { for (var s : Spec.CAMPSTATIONS) if (s.id().equals(id)) return s; return null; }
    /** Where the row's NPC stands now (CampWorld.at: a placement, else the row); the integrated server shares the client's JVM, so the client reads it too. */
    static Vec3d pos(Spec.CampStationsRow s) { var a = CampWorld.at(s); return new Vec3d(a.x(), a.y(), a.z()); }
    /** The game chat lines the client received (not the action bar), cleared before each click. */
    static final List<String> CHAT = Collections.synchronizedList(new ArrayList<>());
    static boolean hooked;
    volatile String eggWhy;
    volatile BlockPos eggFloor;

    /** The client's villager of a station (its name, AI off, on its spot), or null. */
    static VillagerEntity npc(MinecraftClient c, Spec.CampStationsRow s) {
        String name = CampNpcs.name(s);
        for (Entity e : c.world.getEntities())
            if (e instanceof VillagerEntity v && v.isAiDisabled() && v.getCustomName() != null && v.getCustomName().getString().equals(name) && v.squaredDistanceTo(pos(s)) < 1) return v;
        return null;
    }

    /** Where the camera stands to see an NPC: the first of front, left, right and back, 3.5 down to 2 blocks out (a vanilla server takes an entity click only within about 4 blocks of the eye), whose feet and head cells
     *  are free, whose floor is solid, and from whose eye the NPC's chest is in plain view. Null if none is. */
    static Vec3d stand(ServerWorld w, Spec.CampStationsRow s, double height) {
        Vec3d at = pos(s), chest = at.add(0, height * .6, 0);
        double yaw = Math.toRadians(CampWorld.at(s).yaw());
        Vec3d fwd = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw)), side = new Vec3d(fwd.z, 0, -fwd.x);
        for (Vec3d dir : new Vec3d[]{fwd, side, side.negate(), fwd.negate()})
            for (double d = 3.5; d >= 2; d -= .5) {
                Vec3d feet = at.add(dir.multiply(d));
                BlockPos f = BlockPos.ofFloored(feet);
                if (!free(w, f) || !free(w, f.up()) || free(w, f.down())) continue;
                Vec3d eye = feet.add(0, 1.62, 0);
                var hit = w.raycast(new RaycastContext(eye, chest, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, ShapeContext.absent()));
                if (hit.getType() == HitResult.Type.MISS) return feet;
            }
        return null;
    }
    static boolean free(ServerWorld w, BlockPos p) { return w.getBlockState(p).getCollisionShape(w, p).isEmpty(); }

    static void discardSpawned(GravesAndGlory.Battle b) { var gone = new ArrayList<Entity>(); for (var e : b.world.iterateEntities()) if (e.getCommandTags().contains(SPAWNED)) gone.add(e); gone.forEach(Entity::discard); }

    /** The placement egg on Madame Fleece: used on the floor block one east of her spot (inside her station zone), then the checks, a reload, a reset. */
    void eggs(MinecraftClient c, int k, int tick) {
        var s = row("tailor");
        if (k == 1) {
            eggWhy = null; eggFloor = null;
            Phase1Checks.server(c, b -> {
                var p = b.server.getPlayerManager().getPlayer(player);
                if (p == null) { eggWhy = "no player; "; return; }
                if (!CampNpcEggs.allowed(p)) b.server.getPlayerManager().addToOperators(p.getGameProfile()); // the throwaway test world's player; deopped below
                p.getInventory().setStack(p.getInventory().selectedSlot, CampNpcEggs.egg(s));
                Vec3d at = pos(s);
                eggFloor = BlockPos.ofFloored(at.x + 1, at.y - 1, at.z);
            });
        }
        if (k == 20) {
            if (eggFloor == null) { Phase1Checks.report("p1-camp-npc-eggs", false, eggWhy == null ? "the server never set up the egg" : eggWhy); next(tick, 9); return; }
            CHAT.clear();
            c.interactionManager.interactBlock(c.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(eggFloor).add(0, .5, 0), Direction.UP, eggFloor, false));
        }
        if (k == 50) Phase1Checks.shot(c, "camp-npc-tailor-placed");
        if (k == 60) {
            StringBuilder bad = new StringBuilder();
            if (c.currentScreen != null) bad.append("the egg's click opened ").append(c.currentScreen.getClass().getSimpleName()).append("; ");
            BlockPos floor = eggFloor;
            Phase1Checks.server(c, b -> {
                var w = b.world; var p = b.server.getPlayerManager().getPlayer(player); var camp = Spec.CAMP[0];
                Vec3d want = new Vec3d(floor.getX() + .5, floor.getY() + 1, floor.getZ() + .5);
                String tag = CampWorld.npcTag(s.id());
                java.util.function.BiConsumer<String, Vec3d> near = (what, spot) -> {
                    List<Entity> found = new ArrayList<>();
                    for (var e : w.iterateEntities()) if (e.getCommandTags().contains(tag) && e.getCommandTags().contains(CampWorld.TAG) && (what.equals("label") ? e.getType() == EntityType.TEXT_DISPLAY : what.equals("box") ? e instanceof InteractionEntity : e instanceof VillagerEntity)) found.add(e);
                    if (found.size() != 1) bad.append(found.size()).append(" ").append(what).append("s tagged ").append(tag).append("; ");
                    else if (found.get(0).getPos().distanceTo(spot) > .05) bad.append(what).append(" at ").append(found.get(0).getPos()).append(", want ").append(spot).append("; ");
                };
                near.accept("villager", want); near.accept("box", want); near.accept("label", want.add(0, 3.7, 0));
                if (!CampNpcs.WARDROBE.equals(CampWorld.station(floor.up()))) bad.append("station() at the new spot is ").append(CampWorld.station(floor.up())).append("; ");
                int strays = w.getEntitiesByClass(VillagerEntity.class, new Box(want, want).expand(3), e -> !e.getCommandTags().contains(CampWorld.TAG)).size();
                if (strays > 0) bad.append(strays).append(" vanilla villagers near the spot (the egg spawned one); ");
                var file = DimensionType.getSaveDirectory(w.getRegistryKey(), b.server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)).resolve(CampNpcPlacements.fileName(camp.id()));
                try {
                    var saved = CampNpcPlacements.parse(java.nio.file.Files.readString(file), camp.id()).get(s.id());
                    if (saved == null || Math.abs(saved.dx() - (want.x - camp.originX())) > .01 || Math.abs(saved.dz() - (want.z - camp.originZ())) > .01 || Math.abs(saved.y() - want.y) > .01) bad.append("the file holds ").append(saved).append("; ");
                } catch (Exception e) { bad.append("the file ").append(file).append(" could not be read: ").append(e).append("; "); }
                // a restart: the camp loads the file again before its people spawn
                CampWorld.placedWorld = Map.of(); CampNpcEggs.load(w); CampWorld.respawn(w, s);
                near.accept("villager", want);
                // the reset: back on the row's spot, the file without her
                String why = p == null ? "no player" : CampNpcEggs.set(p, s, null);
                if (!why.isEmpty()) bad.append("reset: ").append(why).append("; ");
                near.accept("villager", new Vec3d(camp.originX() + s.x(), s.y(), camp.originZ() + s.z()));
                try { if (CampNpcPlacements.parse(java.nio.file.Files.readString(file), camp.id()).containsKey(s.id())) bad.append("the reset left her in the file; "); } catch (Exception e) { bad.append("after the reset the file could not be read: ").append(e).append("; "); }
                if (p != null) { p.getInventory().setStack(p.getInventory().selectedSlot, net.minecraft.item.ItemStack.EMPTY); b.server.getPlayerManager().removeFromOperators(p.getGameProfile()); }
                eggWhy = bad.toString();
            });
        }
        if (k == 80) {
            String w = eggWhy == null ? "the server checks never ran" : eggWhy;
            if (!CHAT.isEmpty()) say("egg said: " + CHAT);
            Phase1Checks.report("p1-camp-npc-eggs", w.isEmpty(), w.isEmpty() ? "Madame Fleece's egg on a block in her station zone moved her villager, click box, label and zone there without opening the station or spawning a villager; the world save's file holds the spot, a reload keeps it, and a reset puts her back on her row" : w);
            next(tick, 9);
        }
    }

    @Override public void cleanup(MinecraftClient c) {
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
        if (c.getServer() != null) Phase1Checks.server(c, CampNpcChecks::discardSpawned);
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 9000 && stage < 9) { Phase1Checks.report("p1-camp-npcs-timeout", false, "stuck at stage " + stage + " on " + NPCS[Math.min(index, NPCS.length - 1)]); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); if (!hooked) { hooked = true; ClientReceiveMessageEvents.GAME.register((m, overlay) -> { if (!overlay) CHAT.add(m.getString()); }); } next(tick, 1); } }
            case 1 -> { // one NPC: the server's side, then the camera on its free spot facing it, with the two vanilla villagers beside the camera
                String id = NPCS[index];
                Spec.CampStationsRow s = row(id);
                if (k == 1) {
                    serverWhy = null; placed = false; spawnedIds[0] = spawnedIds[1] = -1;
                    if (s == null) { why += id + ": no campStations row; "; next(tick, 2); return; }
                    String tag = "camp:" + s.page();
                    Phase1Checks.server(c, b -> {
                        StringBuilder bad = new StringBuilder();
                        Vec3d at = pos(s);
                        var v = b.world.getEntitiesByClass(VillagerEntity.class, new Box(at, at).expand(.5), e -> e.getCommandTags().contains(tag) && e.getCommandTags().contains(CampWorld.TAG));
                        if (v.size() != 1) bad.append(v.size()).append(" villagers; ");
                        else {
                            var d = v.get(0).getVillagerData();
                            if (!Registries.VILLAGER_PROFESSION.getId(d.getProfession()).getPath().equals(s.npc())) bad.append("profession ").append(d.getProfession()).append(" not ").append(s.npc()).append("; ");
                            if (!Registries.VILLAGER_TYPE.getId(d.getType()).getPath().equals(s.villagerType())) bad.append("type ").append(d.getType()).append(" not ").append(s.villagerType()).append("; ");
                        }
                        var look = CampNpcs.look(s);
                        if (look == null) bad.append("no look; ");
                        var boxes = b.world.getEntitiesByClass(InteractionEntity.class, new Box(at, at).expand(.5), e -> e.getCommandTags().contains(tag) && e.getCommandTags().contains(CampWorld.TAG));
                        if (look != null && CampNpcs.hitBox(look)) {
                            if (boxes.size() != 1) bad.append(boxes.size()).append(" interaction boxes; ");
                            else { var box = boxes.get(0).getBoundingBox(); double w = box.getLengthX(), h = box.getLengthY();
                                if (Math.abs(w - look.hitWidth()) > .01 || Math.abs(h - look.hitHeight()) > .01) bad.append("interaction box ").append(w).append(" x ").append(h).append(" not ").append(look.hitWidth()).append(" x ").append(look.hitHeight()).append("; "); }
                        } else if (!boxes.isEmpty()) bad.append(boxes.size()).append(" interaction boxes on a look without one; ");
                        Vec3d feet = stand(b.world, s, look == null ? 1.95 : 1.95 * look.scale());
                        if (feet == null) { bad.append("no free spot with a clear view of it; "); serverWhy = bad.toString(); return; }
                        // two plain villagers between the camera and the NPC, to either side: one with the NPC's name and its AI on, one unnamed
                        Vec3d toNpc = at.subtract(feet).normalize(), side = new Vec3d(toNpc.z, 0, -toNpc.x);
                        for (int i = 0; i < 2; i++) {
                            VillagerEntity o = new VillagerEntity(EntityType.VILLAGER, b.world);
                            Vec3d p = feet.add(side.multiply(i == 0 ? 1.2 : -1.2)).add(toNpc.multiply(.8));
                            o.refreshPositionAndAngles(p.x, feet.y, p.z, 0, 0);
                            o.setInvulnerable(true); o.setSilent(true); o.addCommandTag(SPAWNED);
                            if (i == 0) o.setCustomName(Text.literal(CampNpcs.name(s)));
                            b.world.spawnEntity(o); spawnedIds[i] = o.getId();
                        }
                        serverWhy = bad.toString();
                        var p = b.server.getPlayerManager().getPlayer(player);
                        if (p != null) {
                            Vec3d eye = feet.add(0, 1.62, 0), aim = at.add(0, 1.2, 0).subtract(eye);
                            float py = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z)), pitch = (float) -Math.toDegrees(Math.atan2(aim.y, Math.hypot(aim.x, aim.z)));
                            p.networkHandler.requestTeleport(feet.x, feet.y, feet.z, py, pitch); placed = true;
                            say(id + ": camera at " + Math.round(feet.x * 10) / 10.0 + " " + feet.y + " " + Math.round(feet.z * 10) / 10.0);
                        }
                    });
                }
                if (k == 60) {
                    c.setScreen(null); c.options.hudHidden = true; c.options.setPerspective(Perspective.FIRST_PERSON);
                    if (!placed) { why += id + ": " + (serverWhy == null ? "the server never placed the player (no battle?)" : serverWhy) + "; "; next(tick, 2); return; }
                }
                if (k == 120) {
                    if (serverWhy == null) why += id + ": server checks never ran; "; else if (!serverWhy.isEmpty()) why += id + ": " + serverWhy;
                    VillagerEntity v = npc(c, s);
                    if (v == null) why += id + ": no villager named " + CampNpcs.name(s) + " on its spot on the client; ";
                    else if (CampNpcRenderer.look(v) == null || !id.equals(CampNpcRenderer.look(v).id())) why += id + ": the client's villager does not resolve to its look; ";
                    // the two spawned villagers by entity id (they have their AI on and may have wandered off): both draw vanilla
                    int plain = 0, wrong = 0;
                    for (int sid : spawnedIds) if (c.world.getEntityById(sid) instanceof VillagerEntity o && !o.isAiDisabled()) { plain++; if (CampNpcRenderer.look(o) != null) wrong++; }
                    if (plain < 2 || wrong > 0) why += id + ": " + plain + " spawned AI villagers on the client, " + wrong + " of them drawn as a look (want 2 and 0); ";
                    if (v != null && !(c.getEntityRenderDispatcher().getRenderer(v) instanceof CampNpcRenderer)) why += id + ": the villager renderer is not CampNpcRenderer; ";
                    draws = CampNpcRenderer.drawsByLook.getOrDefault(id, 0L);
                    Phase1Checks.server(c, CampNpcChecks::discardSpawned); // they have their AI on and wander
                }
                if (k == 140) {
                    long now = CampNpcRenderer.drawsByLook.getOrDefault(id, 0L);
                    if (now <= draws) why += id + ": no draws through its look in 20 ticks (" + draws + " to " + now + "); ";
                    Phase1Checks.shot(c, "camp-npc-" + id + "-idle");
                    played = CampNpcRenderer.interactsByLook.getOrDefault(id, 0L);
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (p != null) CampWorld.swing(b.world, p, s.page()); });
                }
                if (k == 160) {
                    if (CampNpcRenderer.interactsByLook.getOrDefault(id, 0L) <= played) why += id + ": CampWorld.swing did not start its interact clip; ";
                    played = CampNpcRenderer.interactsByLook.getOrDefault(id, 0L);
                    // a real click on the interaction box (or the villager, for a look without one): the camp screen opens on the page and the clip plays again
                    Entity target = null;
                    for (Entity e : c.world.getEntities()) if (e instanceof InteractionEntity i && i.squaredDistanceTo(pos(s)) < .25) target = i;
                    if (target == null) target = npc(c, s);
                    if (target == null) why += id + ": nothing to click on the client; ";
                    else { CHAT.clear(); c.interactionManager.interactEntity(c.player, target, Hand.MAIN_HAND); }
                }
                if (k == 200 && s.page().equals(CampNpcs.WARDROBE)) { // no Wardrobe screen yet: no screen, her chat line
                    if (c.currentScreen != null) why += id + ": the click opened " + c.currentScreen.getClass().getSimpleName() + ", want none yet; ";
                    if (!CHAT.contains(CampNpcs.wardrobeSoon())) why += id + ": no '" + CampNpcs.wardrobeSoon() + "' chat line (got " + CHAT + "); ";
                    else say(id + ": the click said " + CampNpcs.wardrobeSoon());
                    if (CampNpcRenderer.interactsByLook.getOrDefault(id, 0L) <= played) why += id + ": the click did not play its interact clip; ";
                    c.setScreen(null);
                    next(tick, 2);
                } else if (k == 200) {
                    String want = CampScreen.alias(s.page());
                    if (!(c.currentScreen instanceof CampScreen cs)) why += id + ": the click opened " + (c.currentScreen == null ? "no screen" : c.currentScreen.getClass().getSimpleName()) + ", not the camp screen; ";
                    else if (!want.equals(cs.page)) why += id + ": the click opened page " + cs.page + ", not " + want + "; ";
                    else say(id + ": the click opened the camp screen on " + cs.page);
                    if (CampNpcRenderer.interactsByLook.getOrDefault(id, 0L) <= played) why += id + ": the click did not play its interact clip; ";
                    c.setScreen(null);
                    next(tick, 2);
                }
            }
            case 2 -> { // the next NPC, or the report
                if (k < 10) return;
                if (!why.contains(NPCS[index] + ":")) passed.add(NPCS[index]);
                index++;
                if (index < NPCS.length) { next(tick, 1); return; }
                Phase1Checks.report("p1-camp-npcs", why.isEmpty(), why.isEmpty() ? "all six staff NPCs (" + String.join(", ", passed) + ") draw their looks, a same-named villager with AI on and an unnamed one stay vanilla, the server's villagers and interaction boxes are right, and both the swing and a click play the interact clip and open the station's page (Madame Fleece: no screen yet, her chat line)" : why);
                next(tick, 3);
            }
            case 3 -> eggs(c, k, tick);
            case 9 -> {
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, CampNpcChecks::discardSpawned);
                if (k > 40) { Phase1Checks.finish(c, "camp-npcs"); stage = 99; }
            }
            default -> {}
        }
    }
}
