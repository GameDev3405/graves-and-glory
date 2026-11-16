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
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * Camp NPCs on a modded client (lobby NPC spec 3.1 and 5): GRAVES_PHASE1_CHECKS=camp-npcs. In the camp the forge villager ("Blacksmith", AI off)
 * draws through the blacksmith look's GeckoLib rig (the placeholder until the animation session delivers his), while two villagers the
 * suite spawns beside him draw vanilla: one also named "Blacksmith" but with its AI on, one unnamed. The server side: the villager is a
 * toolsmith with the taiga skin and an interaction box of the look's size stands on him, tagged camp:forge. CampWorld.swing starts the interact
 * clip, and a real click on the interaction box opens the forge and plays it again. Shots: p1-camp-npc-blacksmith-idle and -interact.
 * Never shipped.
 */
final class CampNpcChecks implements Phase1Checks.Suite {
    static final String SPAWNED = "camp-npc-check";
    volatile UUID player;
    volatile String serverWhy;
    volatile boolean placed;
    int stage, at;
    long draws, played;
    String why = "";

    static void say(String line) { System.out.println("[p1] camp-npcs: " + line); }
    void next(int tick, int s) { stage = s; at = tick; }

    static Spec.CampStationsRow row() { for (var s : Spec.CAMPSTATIONS) if (s.id().equals("blacksmith")) return s; return null; }
    static Vec3d pos(Spec.CampStationsRow s) { var c = Spec.CAMP[0]; return new Vec3d(c.originX() + s.x(), s.y(), c.originZ() + s.z()); }

    /** The client's forge villager (named Blacksmith, AI off), or null. */
    static VillagerEntity smith(MinecraftClient c) {
        for (Entity e : c.world.getEntities())
            if (e instanceof VillagerEntity v && v.isAiDisabled() && v.getCustomName() != null && v.getCustomName().getString().equals("Blacksmith")) return v;
        return null;
    }

    @Override public void cleanup(MinecraftClient c) {
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
        if (c.getServer() != null) Phase1Checks.server(c, b -> { var gone = new ArrayList<Entity>(); for (var e : b.world.iterateEntities()) if (e.getCommandTags().contains(SPAWNED)) gone.add(e); gone.forEach(Entity::discard); });
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 6000 && stage < 9) { Phase1Checks.report("p1-camp-npcs-timeout", false, "stuck at stage " + stage); next(tick, 9); }
        int k = tick - at;
        Spec.CampStationsRow s = row();
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); next(tick, 1); } }
            case 1 -> { // the server's side, then put the player 4.5 blocks in front of the smith, facing him, with the two vanilla villagers beside him
                if (k == 1) Phase1Checks.server(c, b -> {
                    StringBuilder bad = new StringBuilder();
                    Vec3d at = pos(s);
                    var v = b.world.getEntitiesByClass(VillagerEntity.class, new net.minecraft.util.math.Box(at, at).expand(.5), e -> e.getCommandTags().contains("camp:forge"));
                    if (v.size() != 1) bad.append(v.size()).append(" forge villagers; ");
                    else {
                        var d = v.get(0).getVillagerData();
                        if (!Registries.VILLAGER_PROFESSION.getId(d.getProfession()).getPath().equals(s.npc())) bad.append("profession ").append(d.getProfession()).append(" not ").append(s.npc()).append("; ");
                        if (!Registries.VILLAGER_TYPE.getId(d.getType()).getPath().equals(s.villagerType())) bad.append("type ").append(d.getType()).append(" not ").append(s.villagerType()).append("; ");
                    }
                    var look = CampNpcs.look(s);
                    var boxes = b.world.getEntitiesByClass(InteractionEntity.class, new net.minecraft.util.math.Box(at, at).expand(.5), e -> e.getCommandTags().contains("camp:forge") && e.getCommandTags().contains(CampWorld.TAG));
                    if (boxes.size() != 1) bad.append(boxes.size()).append(" interaction boxes on the smith; ");
                    else { var box = boxes.get(0).getBoundingBox(); double w = box.getLengthX(), h = box.getLengthY();
                        if (Math.abs(w - look.hitWidth()) > .01 || Math.abs(h - look.hitHeight()) > .01) bad.append("interaction box ").append(w).append(" x ").append(h).append(" not ").append(look.hitWidth()).append(" x ").append(look.hitHeight()).append("; "); }
                    serverWhy = bad.toString();
                    // two plain villagers 2.5 blocks to the smith's left and right: one named Blacksmith with its AI on, one unnamed
                    double yaw = Math.toRadians(s.yaw()); Vec3d fwd = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw)), side = new Vec3d(fwd.z, 0, -fwd.x);
                    for (int i = 0; i < 2; i++) {
                        VillagerEntity o = new VillagerEntity(EntityType.VILLAGER, b.world);
                        Vec3d p = at.add(side.multiply(i == 0 ? 2.5 : -2.5));
                        o.refreshPositionAndAngles(p.x, p.y, p.z, (float) s.yaw(), 0); o.setHeadYaw((float) s.yaw()); o.setBodyYaw((float) s.yaw());
                        o.setInvulnerable(true); o.setSilent(true); o.addCommandTag(SPAWNED);
                        if (i == 0) o.setCustomName(Text.literal("Blacksmith"));
                        b.world.spawnEntity(o);
                    }
                    var p = b.server.getPlayerManager().getPlayer(player);
                    if (p != null) { Vec3d eye = at.add(fwd.multiply(4.5)); float py = (float) s.yaw() + 180f; p.networkHandler.requestTeleport(eye.x, eye.y, eye.z, py, 8f); placed = true; }
                });
                if (k == 60) {
                    if (!placed) { Phase1Checks.report("p1-camp-npcs", false, "the server never placed the player (no battle?)"); next(tick, 9); return; }
                    c.setScreen(null); c.options.hudHidden = true; c.options.setPerspective(Perspective.FIRST_PERSON);
                }
                if (k == 120) {
                    if (serverWhy == null) why += "server checks never ran; "; else why += serverWhy;
                    VillagerEntity v = smith(c);
                    if (v == null) why += "no Blacksmith villager on the client; ";
                    else if (CampNpcRenderer.look(v) == null || !"blacksmith".equals(CampNpcRenderer.look(v).id())) why += "the client's forge villager does not resolve to the blacksmith look; ";
                    int plain = 0, wrong = 0;
                    for (Entity e : c.world.getEntities()) if (e instanceof VillagerEntity o && !o.isAiDisabled() && o.squaredDistanceTo(c.player) < 100) { plain++; if (CampNpcRenderer.look(o) != null) wrong++; }
                    if (plain < 2 || wrong > 0) why += plain + " AI villagers near, " + wrong + " of them drawn as a look (want 2 and 0); ";
                    if (v != null && !(c.getEntityRenderDispatcher().getRenderer(v) instanceof CampNpcRenderer)) why += "the villager renderer is not CampNpcRenderer; ";
                    draws = CampNpcRenderer.npcDraws;
                }
                if (k == 140) {
                    if (CampNpcRenderer.npcDraws <= draws || !"blacksmith".equals(CampNpcRenderer.lastLook)) why += "no blacksmith draws through the look in 20 ticks (" + draws + " to " + CampNpcRenderer.npcDraws + ", last " + CampNpcRenderer.lastLook + "); ";
                    Phase1Checks.shot(c, "camp-npc-blacksmith-idle");
                    played = CampNpcRenderer.interactsPlayed;
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (p != null) CampWorld.swing(b.world, p, "forge"); });
                }
                if (k == 152) Phase1Checks.shot(c, "camp-npc-blacksmith-interact");
                if (k == 160) {
                    if (CampNpcRenderer.interactsPlayed <= played) why += "CampWorld.swing did not start the interact clip; ";
                    played = CampNpcRenderer.interactsPlayed;
                    // a real click on the interaction box: the forge opens and the smith plays his interact again
                    InteractionEntity box = null;
                    for (Entity e : c.world.getEntities()) if (e instanceof InteractionEntity i && i.squaredDistanceTo(pos(s)) < .25) box = i;
                    if (box == null) why += "no interaction box on the client; ";
                    else c.interactionManager.interactEntity(c.player, box, Hand.MAIN_HAND);
                }
                if (k == 200) {
                    if (c.currentScreen == null) why += "a click on the interaction box opened no screen; ";
                    else say("the click opened " + c.currentScreen.getClass().getSimpleName());
                    if (CampNpcRenderer.interactsPlayed <= played) why += "the click on the interaction box did not play the interact clip; ";
                    c.setScreen(null);
                    Phase1Checks.report("p1-camp-npcs", why.isEmpty(), why.isEmpty() ? "the forge villager draws the blacksmith look, two plain villagers stay vanilla, the server's toolsmith/taiga villager and its interaction box are right, and both the swing and a click on the box play the interact clip" : why);
                    next(tick, 9);
                }
            }
            case 9 -> {
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, b -> { var gone = new ArrayList<Entity>(); for (var e : b.world.iterateEntities()) if (e.getCommandTags().contains(SPAWNED)) gone.add(e); gone.forEach(Entity::discard); });
                if (k > 40) { Phase1Checks.finish(c, "camp-npcs"); stage = 99; }
            }
            default -> {}
        }
    }
}
