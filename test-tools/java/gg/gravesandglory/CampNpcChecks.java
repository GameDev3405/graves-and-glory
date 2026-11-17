package gg.gravesandglory;

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
import net.minecraft.world.RaycastContext;
import java.util.*;

/**
 * Camp NPCs on a modded client (lobby NPC spec 3.1 and 5): GRAVES_PHASE1_CHECKS=camp-npcs. For each of the five staff NPCs (blacksmith,
 * quartermaster, archivist, steward, drillmaster) in turn: the server's villager has the spec's profession and biome skin and an interaction
 * box of its look's size, tagged camp:<page>; the client draws it through its look's GeckoLib rig (per-look draw counters, so two NPCs a few
 * blocks apart are told apart), while a villager the suite spawns with the same name but its AI on, and an unnamed one, draw vanilla;
 * CampWorld.swing starts its interact clip, and a real click on its interaction box opens the camp screen on its station's page and plays the
 * clip again. The camera stands on the first free spot with a clear view of the NPC (front first, then the sides). One shot per NPC:
 * p1-camp-npc-<id>-idle. Never shipped.
 */
final class CampNpcChecks implements Phase1Checks.Suite {
    static final String SPAWNED = "camp-npc-check";
    static final String[] NPCS = {"blacksmith", "quartermaster", "archivist", "steward", "drillmaster"};
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
    static Vec3d pos(Spec.CampStationsRow s) { var c = Spec.CAMP[0]; return new Vec3d(c.originX() + s.x(), s.y(), c.originZ() + s.z()); }

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
        double yaw = Math.toRadians(s.yaw());
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

    @Override public void cleanup(MinecraftClient c) {
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
        if (c.getServer() != null) Phase1Checks.server(c, CampNpcChecks::discardSpawned);
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 9000 && stage < 9) { Phase1Checks.report("p1-camp-npcs-timeout", false, "stuck at stage " + stage + " on " + NPCS[Math.min(index, NPCS.length - 1)]); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); next(tick, 1); } }
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
                    Phase1Checks.server(c, b -> { for (var e : b.world.iterateEntities()) if (e.getCommandTags().contains(SPAWNED)) say(id + ": spawned villager on the server at " + e.getBlockPos().toShortString() + " removed " + e.isRemoved()); discardSpawned(b); }); // they have their AI on and wander
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
                    else c.interactionManager.interactEntity(c.player, target, Hand.MAIN_HAND);
                }
                if (k == 200) {
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
                Phase1Checks.report("p1-camp-npcs", why.isEmpty(), why.isEmpty() ? "all five staff NPCs (" + String.join(", ", passed) + ") draw their looks, a same-named villager with AI on and an unnamed one stay vanilla, the server's villagers and interaction boxes are right, and both the swing and a click play the interact clip and open the station's page" : why);
                next(tick, 9);
            }
            case 9 -> {
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, CampNpcChecks::discardSpawned);
                if (k > 40) { Phase1Checks.finish(c, "camp-npcs"); stage = 99; }
            }
            default -> {}
        }
    }
}
