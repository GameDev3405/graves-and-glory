package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.EquipmentSlot;
import java.util.*;

/**
 * Real-client checks for the commander gear art (plan 2026-11-03-gear-art, Task 9). GRAVES_PHASE1_CHECKS=gear-art. A practice battle
 * in the full Legendary bruiser set draws all six pieces with their own models and none with the vanilla layer (p1-gear-art-worn,
 * shots from the front and back); with only the helm and weapons worn, only those draw (p1-gear-art-single); the gear screen frame
 * draws the worn loadout at 1920x1080 and 3840x2160 (p1-gear-art-frame). Task 10: back in battle, a greatsword, a mending staff and a
 * war banner each play their clips, shot at the strike's peak: the strike over a swing, the staff's cast in a fresh cooldown, the
 * banner raised while sneaking (p1-gear-art-clips). Kort's 11-04 review: each class set at Legendary with a sword and shield, a
 * staff and a banner, shot from the front, both sides and the back by a camera stand (p1-gear-art-sets). The profile is put back
 * from a JSON copy. Never shipped.
 */
final class GearArtChecks implements Phase1Checks.Suite {
    static final List<String> SET = List.of("bruiser_helm", "bruiser_chest", "bruiser_legs", "bruiser_boots", "arming_sword", "kite_shield");
    static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    volatile UUID player;
    volatile String backup, why = "";
    volatile boolean ready, battleReady, restored, ok;
    static final List<String> CLIP_WEAPONS = List.of("greatsword", "mending_staff", "war_banner");
    static final List<String> CLASSES = List.of("bruiser", "commander", "healer", "ranged");
    static final List<List<String>> HELD = List.of(List.of("arming_sword", "kite_shield"), List.of("mending_staff"), List.of("war_banner"));
    static final String[] VIEWS = {"front", "right", "back", "left"};
    int stage, at, oldW, oldH, oldGui, wi;
    float yaw;
    long frames0;

    static void say(String line) { System.out.println("[p1] gear-art: " + line); }
    void next(int tick, int s) { stage = s; at = tick; ok = false; }

    @Override public void cleanup(MinecraftClient c) {
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false; c.options.sneakKey.setPressed(false); c.setCameraEntity(c.player);
        if (restored || player == null || c.getServer() == null) return;
        try { c.getServer().submit(this::restore).join(); } catch (RuntimeException e) { say("restore in cleanup failed: " + e); }
    }

    void restore() {
        var b = GravesAndGlory.battle;
        if (b == null || backup == null) return;
        if (b.running) b.campaign.returnLobby();
        b.campaign.profiles.put(player, Campaign.JSON.fromJson(backup, Profile.class));
        b.campaign.save(player);
        restored = true;
    }

    /** What the client player wears that the feature did not draw, or the vanilla layer would also draw. */
    static String wornWhy(MinecraftClient c, Collection<String> want) {
        StringBuilder s = new StringBuilder();
        Set<String> worn = new TreeSet<>();
        for (var slot : SLOTS) {
            var st = c.player.getEquippedStack(slot); var a = GearLooks.art(st);
            if (a == null) continue;
            worn.add(a.item());
            if (!CommanderGearFeature.drawsOwn(st)) s.append(a.item()).append(" left to the vanilla layer; ");
        }
        if (!worn.equals(new TreeSet<>(want))) s.append("wears ").append(worn).append(" (want ").append(new TreeSet<>(want)).append("); ");
        if (!CommanderGearFeature.drawn.equals(new HashSet<>(want))) s.append("drew ").append(new TreeSet<>(CommanderGearFeature.drawn)).append("; ");
        return s.toString();
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 16000 && stage < 9) { Phase1Checks.report("p1-gear-art-timeout", false, "stuck at stage " + stage); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { // the full bruiser set at Legendary, worn
                if (tick != 20) return;
                var w = c.getWindow(); oldW = w.getWidth(); oldH = w.getHeight(); oldGui = c.options.getGuiScale().getValue();
                c.setScreen(null);
                UUID id = c.player.getUuid(); player = id;
                Phase1Checks.server(c, b -> {
                    if (b.running) b.campaign.returnLobby();
                    var pr = b.campaign.profile(id);
                    backup = Campaign.JSON.toJson(pr);
                    pr.gear.clear(); pr.equipped.clear();
                    for (String item : SET) {
                        var piece = new Gear.Piece(pr.nextGearUid++, item, "legendary", 90, 100);
                        pr.gear.add(piece);
                        var err = Gear.equip(pr, piece.uid);
                        if (!err.isEmpty()) why += item + ": " + err + "; ";
                    }
                    b.campaign.save(id);
                    ready = true;
                });
                next(tick, 1);
            }
            case 1 -> { // into a practice battle
                if (!ready) return;
                if (!battleReady) {
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true; });
                    if (k > 600) { Phase1Checks.report("p1-gear-art-worn", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                next(tick, 2);
            }
            case 2 -> { // p1-gear-art-worn
                if (k == 80) { c.setScreen(null); c.options.hudHidden = true; c.options.setPerspective(Perspective.THIRD_PERSON_FRONT); }
                if (k == 110) { Phase1Checks.shot(c, "gear-art-worn-front"); why += wornWhy(c, SET); }
                if (k == 120) c.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                if (k == 150) {
                    Phase1Checks.shot(c, "gear-art-worn-back");
                    String back = wornWhy(c, SET);
                    Phase1Checks.report("p1-gear-art-worn", why.isEmpty() && back.isEmpty(), why.isEmpty() && back.isEmpty() ? "six Legendary bruiser pieces drawn by their own models, front and back" : why + back);
                    why = "";
                    Phase1Checks.server(c, b -> {
                        var pr = b.campaign.profile(player);
                        for (String s : List.of("chest", "legs", "boots")) pr.equipped.remove(s);
                        b.equipPlayer(b.server.getPlayerManager().getPlayer(player));
                        ok = true;
                    });
                }
                if (ok) next(tick, 3);
            }
            case 3 -> { // p1-gear-art-single: the helm and the weapons, nothing else
                if (k == 1) c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                if (k == 40) {
                    Phase1Checks.shot(c, "gear-art-single");
                    var want = List.of("bruiser_helm", "arming_sword", "kite_shield");
                    String w = wornWhy(c, want);
                    Phase1Checks.report("p1-gear-art-single", w.isEmpty(), w.isEmpty() ? "helm, sword and shield drawn, no chest, legs or boots" : w);
                    c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                    Phase1Checks.server(c, b -> {
                        if (b.running) b.campaign.returnLobby();
                        var pr = b.campaign.profile(player);
                        for (var p : pr.gear) Gear.equip(pr, p.uid);
                        b.campaign.save(player);
                        ok = true;
                    });
                }
                if (ok) next(tick, 4);
            }
            case 4 -> { // p1-gear-art-frame at 1920x1080, then 3840x2160
                if (k == 60) Phase1Checks.window(c, 1920, 1080, 0, true);
                if (k == 80 || k == 180) { c.setScreen(null); GearScreenChecks.gearKeyPress(); frames0 = CommanderGearFeature.frames; }
                if (k == 120 || k == 220) {
                    boolean big = k == 220;
                    Phase1Checks.shot(c, big ? "gear-art-frame-2160" : "gear-art-frame-1080");
                    String w = GearScreenChecks.screen(c) == null ? "the gear page is not open; " : "";
                    if (CommanderGearFeature.frames == frames0) w += "the feature drew no frame; ";
                    if (!CommanderGearFeature.drawn.equals(new HashSet<>(SET))) w += "frame drew " + new TreeSet<>(CommanderGearFeature.drawn) + "; ";
                    why += w.isEmpty() ? "" : (big ? "2160: " : "1080: ") + w;
                    c.setScreen(null);
                    if (!big) Phase1Checks.window(c, 3840, 2160, 0, true);
                    else {
                        Phase1Checks.report("p1-gear-art-frame", why.isEmpty(), why.isEmpty() ? "the frame draws all six worn pieces at 1920x1080 and 3840x2160" : why);
                        Phase1Checks.window(c, oldW, oldH, oldGui);
                        why = ""; battleReady = false; next(tick, 5);
                    }
                }
            }
            case 5 -> { // back into a practice battle for the clips
                if (!battleReady) {
                    if (k % 20 == 1) Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true; });
                    if (k > 600) { Phase1Checks.report("p1-gear-art-clips", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                wi = 0; next(tick, 6);
            }
            case 6 -> { // p1-gear-art-clips: one weapon at a time
                String item = CLIP_WEAPONS.get(wi);
                if (k == 1) Phase1Checks.server(c, b -> {
                    var pr = b.campaign.profile(player);
                    var piece = new Gear.Piece(pr.nextGearUid++, item, "legendary", 90, 100);
                    pr.gear.add(piece);
                    var err = Gear.equip(pr, piece.uid);
                    if (!err.isEmpty()) why += item + ": " + err + "; ";
                    b.equipPlayer(b.server.getPlayerManager().getPlayer(player));
                });
                if (k == 40) { c.setScreen(null); c.options.hudHidden = true; c.options.setPerspective(Perspective.THIRD_PERSON_FRONT); CommanderClips.seen.clear(); }
                var art = GearLooks.art(c.player.getMainHandStack());
                if (k == 60 && (art == null || !art.item().equals(item))) why += item + " not in the main hand; ";
                if (k == 60) c.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                if (k >= 61 && k <= 66) { // the swing held at the strike key's share for the shot
                    var p = c.player; p.handSwinging = true; p.handSwingTicks = 3; p.handSwingProgress = p.lastHandSwingProgress = 0.55f;
                }
                if (k == 65) Phase1Checks.shot(c, "gear-art-clip-" + item + "-strike");
                String strike = item.equals("greatsword") ? "great_swing" : item.equals("mending_staff") ? "staff_strike" : "banner_strike";
                if (k == 70 && !CommanderClips.seen.contains(strike)) why += item + " swing played " + CommanderClips.seen + " (want " + strike + "); ";
                if (k == 75 && item.equals("mending_staff")) { CommanderClips.seen.clear(); c.player.getItemCooldownManager().set(c.player.getMainHandStack().getItem(), 40); }
                if (k == 80 && item.equals("mending_staff")) {
                    Phase1Checks.shot(c, "gear-art-clip-mending_staff-cast");
                    if (!CommanderClips.seen.contains("staff_cast")) why += "the staff's cooldown played " + CommanderClips.seen + " (want staff_cast); ";
                }
                if (k == 75 && item.equals("war_banner")) { CommanderClips.seen.clear(); c.options.sneakKey.setPressed(true); }
                if (k == 110 && item.equals("war_banner")) {
                    Phase1Checks.shot(c, "gear-art-clip-war_banner-raised");
                    if (!"banner_raise".equals(CommanderClips.playing) || CommanderClips.playingAt < 1) why += "sneaking with the banner plays " + CommanderClips.playing + " at " + CommanderClips.playingAt + " (want banner_raise held at 1); ";
                    c.options.sneakKey.setPressed(false);
                }
                if (k == 120) {
                    if (++wi < CLIP_WEAPONS.size()) { next(tick, 6); return; }
                    c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                    Phase1Checks.report("p1-gear-art-clips", why.isEmpty(), why.isEmpty() ? "greatsword, mending staff and war banner play their strike, cast and raise clips" : why);
                    why = ""; wi = 0; next(tick, 7);
                }
            }
            case 7 -> { // p1-gear-art-sets: a class set and a loadout per pass, four views each
                String cls = CLASSES.get(wi / HELD.size()); var held = HELD.get(wi % HELD.size());
                List<String> want = new ArrayList<>(List.of(cls + "_helm", cls + "_chest", cls + "_legs", cls + "_boots")); want.addAll(held);
                if (k == 1) Phase1Checks.server(c, b -> {
                    var pr = b.campaign.profile(player);
                    pr.gear.clear(); pr.equipped.clear();
                    for (String item : want) {
                        var piece = new Gear.Piece(pr.nextGearUid++, item, "legendary", 90, 100);
                        pr.gear.add(piece);
                        var err = Gear.equip(pr, piece.uid);
                        if (!err.isEmpty()) why += item + ": " + err + "; ";
                    }
                    b.equipPlayer(b.server.getPlayerManager().getPlayer(player));
                });
                if (k == 2) { c.setScreen(null); c.options.hudHidden = true; c.options.setPerspective(Perspective.THIRD_PERSON_FRONT); yaw = c.player.getYaw(); }
                // the front camera follows the look yaw, held still; the model turns by its body and head yaw instead (vanilla never draws
                // the local player through another camera entity)
                int v = (k - 30) / 20; float turn = yaw + 90 * Math.max(0, Math.min(v, VIEWS.length - 1));
                c.player.setYaw(yaw); c.player.setPitch(0); c.player.prevYaw = yaw; c.player.prevPitch = 0;
                c.player.setBodyYaw(turn); c.player.prevBodyYaw = turn; c.player.setHeadYaw(turn); c.player.prevHeadYaw = turn;
                if (k >= 30 && (k - 30) % 20 == 15 && v < VIEWS.length) {
                    Phase1Checks.shot(c, "gear-art-set-" + cls + "-" + String.join("+", held) + "-" + VIEWS[v]);
                    if (v == 0) why += wornWhy(c, want);
                }
                if (k == 30 + 20 * VIEWS.length) {
                    if (++wi < CLASSES.size() * HELD.size()) { next(tick, 7); return; }
                    c.options.hudHidden = false;
                    Phase1Checks.report("p1-gear-art-sets", why.isEmpty(), why.isEmpty() ? "four class sets with sword and shield, staff and banner, each drawn by its own models from four sides" : why);
                    next(tick, 9);
                }
            }
            case 9 -> {
                if (k == 1) Phase1Checks.server(c, b -> restore());
                if (restored || k > 100) { Phase1Checks.finish(c, "gear-art"); stage = 99; }
            }
            default -> {}
        }
    }
}
