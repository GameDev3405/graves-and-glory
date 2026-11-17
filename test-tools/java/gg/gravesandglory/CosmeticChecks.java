package gg.gravesandglory;

import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * Real-client checks for the cosmetics render (plan 2026-11-16-cosmetics-render). GRAVES_PHASE1_CHECKS=cosmetics. In a practice battle,
 * with the Legendary war banner held and every cosmetic granted, each banner cosmetic is equipped alone: the held stack must carry its
 * vanilla layers (minecraft: ids only), the mesh flag must draw them and CosmeticExtras its extra (cosmetic-&lt;id&gt;.png). A Forward Supply
 * banner planted with a full set must carry the layers on the client's block entity and draw its extras (cosmetic-planted.png). A title
 * must draw over the player (cosmetic-title.png). A knight of a Mastery II troop must draw the Proven trim with skin_proven worn and none
 * without (cosmetic-skin_proven-on/-off.png). The profile is put back from a JSON copy. Never shipped.
 */
final class CosmeticChecks implements Phase1Checks.Suite {
    static final String[] HELD = {"pennant", "pennant_pattern", "trim", "frame", "banner_frame", "frame_brass", "standard"};
    static final String TITLE = "Bannerlord";
    volatile UUID player;
    volatile String backup;
    volatile boolean ready, battleReady, restored;
    volatile BlockPos plantedAt;
    volatile GravesAndGlory.ArmySupplyBanner planted;
    volatile GravesAndGlory.Unit knight;
    volatile float knightYaw;
    volatile String why = "";
    int stage, at, i;
    float yaw;
    final List<String> bad = new ArrayList<>();

    void next(int tick, int s) { stage = s; at = tick; }

    static void shot(MinecraftClient c, String name) {
        ScreenshotRecorder.saveScreenshot(c.runDirectory, "cosmetic-" + name + ".png", c.getFramebuffer(), t -> {});
    }

    /** Every pattern id of a component is minecraft:, and how many layers it has; a reason string when one is not. */
    static String vanillaOnly(BannerPatternsComponent layers) {
        if (layers == null) return "no banner_patterns";
        for (var l : layers.layers()) {
            var key = l.pattern().getKey();
            if (key.isEmpty() || !key.get().getValue().getNamespace().equals("minecraft")) return "a layer with id " + key.map(k -> k.getValue().toString()).orElse("(unregistered)");
        }
        return "";
    }

    /** The profile put back from the copy, on the server thread. */
    void restore() {
        var b = GravesAndGlory.battle;
        if (b == null || backup == null) { restored = true; return; }
        if (planted != null && !planted.isRemoved()) planted.discard();
        if (knight != null && knight.mob != null) { knight.mob.discard(); b.units.remove(knight); }
        if (b.running) b.campaign.returnLobby();
        b.campaign.profiles.put(player, Profile.restored(Campaign.JSON.fromJson(backup, Profile.class)));
        b.campaign.save(player);
        var p = b.server.getPlayerManager().getPlayer(player);
        if (p != null) CosmeticSync.changed(p);
        restored = true;
    }

    @Override public void cleanup(MinecraftClient c) {
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
        if (!restored && c.getServer() != null) Phase1Checks.server(c, b -> restore());
    }

    /** Sets the player's equipped slots (owned: everything), saves, re-dresses his stack and sends the set (server thread). */
    void wear(GravesAndGlory.Battle b, Map<String, String> slots) {
        var pr = b.campaign.profile(player);
        pr.cosmeticSlots = Cosmetics.normalize(pr.cosmetics, new LinkedHashMap<>(slots));
        b.campaign.save(player);
        var p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        CosmeticBanners.dress(b, p, pr);
        CosmeticSync.changed(p);
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 18000 && stage < 9) { Phase1Checks.report("p1-cosmetics-timeout", false, "stuck at stage " + stage); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { // the Legendary war banner, every cosmetic owned, the knight at Mastery II, nothing worn
                if (tick != 20) return;
                c.setScreen(null);
                UUID id = c.player.getUuid(); player = id;
                Phase1Checks.server(c, b -> {
                    if (b.running) b.campaign.returnLobby();
                    var pr = b.campaign.profile(id);
                    backup = Campaign.JSON.toJson(pr);
                    pr.gear.clear(); pr.equipped.clear();
                    var piece = new Gear.Piece(pr.nextGearUid++, "war_banner", "legendary", 90, 100);
                    pr.gear.add(piece);
                    var err = Gear.equip(pr, piece.uid);
                    if (!err.isEmpty()) why += "war_banner: " + err + "; ";
                    for (var item : Cosmetics.CATALOGUE) pr.cosmetics.add(item.id());
                    pr.cosmeticSlots.clear();
                    pr.progression.titles.add(TITLE);
                    pr.progression.title = "Recruit";
                    pr.owned.computeIfAbsent("knight", t -> new Profile.Owned()).mastery = 2;
                    b.campaign.save(id);
                    ready = true;
                });
                next(tick, 1);
            }
            case 1 -> { // into a practice battle
                if (!ready) return;
                if (!battleReady) {
                    if (k % 20 == 1) Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true; });
                    if (k > 900) { Phase1Checks.report("p1-cosmetics-battle", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                if (k > 120) { i = 0; GeoDraws.install(); next(tick, 2); }
            }
            case 2 -> { // one held banner cosmetic at a time, the right side (the flag's face: it hangs along the arm, front to back) to the camera
                String id = HELD[i];
                var item = Cosmetics.item(id);
                if (k == 1) Phase1Checks.server(c, b -> wear(b, Map.of(item.slot(), id)));
                if (k == 2) { c.setScreen(null); c.options.hudHidden = true; c.options.setPerspective(Perspective.THIRD_PERSON_FRONT); yaw = c.player.getYaw(); }
                if (k >= 2) {
                    float turn = yaw - 70; // body yaw - 90 puts the right side toward the front camera; 20 back toward the face
                    c.player.setYaw(yaw); c.player.setPitch(5); c.player.prevYaw = yaw; c.player.prevPitch = 5;
                    c.player.setBodyYaw(turn); c.player.prevBodyYaw = turn; c.player.setHeadYaw(turn); c.player.prevHeadYaw = turn;
                }
                if (k == 50) {
                    var st = c.player.getMainHandStack();
                    var art = GearLooks.art(st);
                    var layers = st.get(DataComponentTypes.BANNER_PATTERNS);
                    var entry = CosmeticSync.client(c.player);
                    if (art == null || !art.item().equals("war_banner")) bad.add(id + ": the main hand holds " + st);
                    else if (layers == null || layers.layers().size() != item.layers().size()) bad.add(id + ": the stack has " + (layers == null ? 0 : layers.layers().size()) + " layers, wanted " + item.layers().size());
                    else if (!vanillaOnly(layers).isEmpty()) bad.add(id + ": " + vanillaOnly(layers));
                    if (!id.equals(entry.slots().get(item.slot()))) bad.add(id + ": cosmetic_v1 entry is " + entry.slots());
                    if (!CosmeticExtras.drawn.contains("held:" + id)) bad.add(id + ": no held extra drawn (drew " + new TreeSet<>(CosmeticExtras.drawn) + ")");
                    if (!CosmeticExtras.drawn.contains("pattern:" + item.layers().size())) bad.add(id + ": the mesh flag drew no " + item.layers().size() + "-layer pattern (drew " + new TreeSet<>(CosmeticExtras.drawn) + ")");
                    shot(c, id);
                }
                if (k == 52) { if (++i < HELD.length) next(tick, 2); else {
                    Phase1Checks.report("p1-cosmetics-held", bad.isEmpty(), bad.isEmpty() ? HELD.length + " held cosmetics carry vanilla layers and draw their extras" : String.join("; ", bad));
                    Phase1Checks.report("p1-cosmetics-sync", CosmeticSync.received > 0 && CosmeticSync.changeSends > 0, "received " + CosmeticSync.received + ", change sends " + CosmeticSync.changeSends + ", tracking " + CosmeticSync.trackingSends + ", register " + CosmeticSync.registerSends + ", skipped (no channel) " + CosmeticSync.skippedPlain);
                    bad.clear(); next(tick, 3); } }
            }
            case 3 -> { // a Forward Supply banner planted with the full set, in front of the player
                if (k == 1) { c.options.hudHidden = true; c.options.setPerspective(Perspective.FIRST_PERSON); }
                if (k == 1) Phase1Checks.server(c, b -> {
                    wear(b, Map.of("pennant", "pennant", "trim", "trim", "frame", "frame_brass", "standard", "standard"));
                    var p = b.server.getPlayerManager().getPlayer(player);
                    var s = b.squads.get(player);
                    if (p == null || s == null) { why += "no player or squad to plant for; "; return; }
                    float py = p.getYaw();
                    double r = Math.toRadians(py);
                    Vec3d feet = p.getPos().add(-Math.sin(r) * 4, 0, Math.cos(r) * 4);
                    planted = GravesAndGlory.ArmySupplyBanner.place(b, s, feet, py + 180, 100);
                    if (planted == null) why += "no spot to plant the supply banner; ";
                    else plantedAt = planted.getBlockPos();
                    p.networkHandler.requestTeleport(p.getX(), p.getY(), p.getZ(), py, 10);
                });
                if (k == 60) {
                    var pos = plantedAt;
                    var be = pos == null || c.world == null ? null : c.world.getBlockEntity(pos);
                    if (!(be instanceof BannerBlockEntity banner)) bad.add("no banner block entity at " + pos);
                    else {
                        var layers = banner.getPatterns();
                        if (layers.layers().size() != 4) bad.add("the planted block has " + layers.layers().size() + " layers, wanted 4");
                        if (!vanillaOnly(layers).isEmpty()) bad.add("planted: " + vanillaOnly(layers));
                    }
                    for (String id : List.of("pennant", "trim", "frame_brass", "standard")) if (!CosmeticExtras.drawn.contains("planted:" + id)) bad.add("planted: no " + id + " extra drawn (drew " + new TreeSet<>(CosmeticExtras.drawn) + ")");
                    shot(c, "planted");
                }
                if (k == 62) {
                    Phase1Checks.report("p1-cosmetics-planted", bad.isEmpty() && why.isEmpty(), bad.isEmpty() && why.isEmpty() ? "the planted supply banner carries 4 vanilla layers and draws its extras" : why + String.join("; ", bad));
                    bad.clear();
                    Phase1Checks.server(c, b -> { if (planted != null) planted.discard(); });
                    next(tick, 4);
                }
            }
            case 4 -> { // the title over the player, HUD on, third person behind
                if (k == 1) Phase1Checks.server(c, b -> {
                    var pr = b.campaign.profile(player);
                    pr.progression.title = TITLE;
                    wear(b, Map.of());
                });
                if (k == 2) { c.setScreen(null); c.options.hudHidden = false; c.options.setPerspective(Perspective.THIRD_PERSON_BACK); }
                if (k == 50) {
                    String drew = CosmeticExtras.drawn.contains("title:" + TITLE) ? "" : "no title drawn (drew " + new TreeSet<>(CosmeticExtras.drawn) + ", entry " + CosmeticSync.client(c.player) + ")";
                    shot(c, "title");
                    Phase1Checks.report("p1-cosmetics-title", drew.isEmpty(), drew.isEmpty() ? "the title " + TITLE + " draws over the player" : drew);
                }
                if (k == 52) { i = 0; next(tick, 5); }
            }
            case 5 -> { // a Proven knight with skin_proven worn (i 0), and without (i 1)
                boolean on = i == 0;
                if (k == 1) { c.options.hudHidden = true; c.options.setPerspective(Perspective.FIRST_PERSON); }
                if (k == 1) Phase1Checks.server(c, b -> {
                    if (knight != null) { knight.mob.discard(); b.units.remove(knight); knight = null; }
                    var pr = b.campaign.profile(player);
                    pr.progression.title = "Recruit";
                    wear(b, on ? Map.of("skin", "skin_proven") : Map.of());
                    var p = b.server.getPlayerManager().getPlayer(player);
                    var s = b.squads.get(player);
                    if (p == null || s == null) { why += "no player or squad for the knight; "; return; }
                    float py = p.getYaw();
                    double r = Math.toRadians(py);
                    double x = p.getX() - Math.sin(r) * 5, z = p.getZ() + Math.cos(r) * 5;
                    var u = b.spawn(s, Campaign.troop("knight"), 40, new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z));
                    u.mob.setAiDisabled(true); u.mob.setInvulnerable(true);
                    float face = py + 90; // side-on: the shield hides the trim from the front
                    u.mob.setYaw(face); u.mob.setBodyYaw(face); u.mob.setHeadYaw(face);
                    knight = u;
                    p.networkHandler.requestTeleport(p.getX(), p.getY(), p.getZ(), py, 12);
                    knightYaw = face;
                });
                if (k > 1 && k < 50) Phase1Checks.server(c, b -> { var u = knight; if (u != null) { float f = knightYaw; u.mob.setYaw(f); u.mob.setBodyYaw(f); u.mob.setHeadYaw(f); u.mob.prevYaw = f; u.mob.prevBodyYaw = f; u.mob.prevHeadYaw = f; } });
                if (k == 50) {
                    var u = knight;
                    var e = u == null || c.world == null ? null : c.world.getEntityById(u.mob.getId());
                    var d = GeoDraws.drawn(e);
                    if (d == null || d.count == 0) bad.add((on ? "on" : "off") + ": the knight never drew through GeckoLib");
                    else if (on == d.trim.equals("-")) bad.add((on ? "on" : "off") + ": the knight drew trim " + d.trim);
                    shot(c, "skin_proven-" + (on ? "on" : "off"));
                }
                if (k == 52) { if (++i < 2) next(tick, 5); else {
                    Phase1Checks.report("p1-cosmetics-skin", bad.isEmpty() && why.isEmpty(), bad.isEmpty() && why.isEmpty() ? "the Proven knight draws its trim with skin_proven worn and none without" : why + String.join("; ", bad));
                    next(tick, 9); } }
            }
            case 9 -> {
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, b -> restore());
                if (restored || k > 100) { Phase1Checks.finish(c, "cosmetics"); stage = 99; }
            }
            default -> {}
        }
    }
}
