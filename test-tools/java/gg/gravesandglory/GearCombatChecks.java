package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

/**
 * Real-client checks for gear combat (plan 2026-11-03-gear-combat). GRAVES_PHASE1_CHECKS=gear-combat. Task 3: the kit in hand and armour is the
 * worn gear, max health is the Loadout's, two downs and respawns leave it unchanged, and a profile wearing nothing fights with the starter kit.
 * The profile is put back from a JSON copy taken at the start. Never shipped.
 */
final class GearCombatChecks implements Phase1Checks.Suite {
    volatile UUID player;
    volatile String backup;
    volatile boolean ready, battleReady, restored, done;
    volatile double healthBefore;
    volatile int downs;
    int stage, stageAt;

    void next(int tick, int s) { stage = s; stageAt = tick; done = false; }
    static void say(String line) { System.out.println("[p1] gear-combat: " + line); }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-3; } // max health is a float

    @Override public void cleanup(MinecraftClient c) {
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

    static ServerPlayerEntity sp(GravesAndGlory.Battle b, UUID id) { return b.server.getPlayerManager().getPlayer(id); }

    /** Starts a practice battle; true once it runs and the commander stands armed in it. */
    boolean battle(MinecraftClient c, int tick, String what) {
        if (!battleReady) {
            Phase1Checks.server(c, b -> {
                var p = sp(b, player);
                if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true;
            });
            if (tick - stageAt > 600) { Phase1Checks.report(what, false, "the practice battle never started"); next(tick, 9); }
            return false;
        }
        if (battleAt < 0) battleAt = tick;
        return tick - battleAt > 60;
    }
    int battleAt = -1;

    /** The worn kit check: hand and armour carry the equipped uids, and the modifiers are the Loadout's. Server thread. */
    static String kitWhy(GravesAndGlory.Battle b, ServerPlayerEntity p, Profile pr) {
        StringBuilder why = new StringBuilder();
        Integer main = pr.equipped.get("main");
        int inHand = GearLooks.uid(p.getMainHandStack());
        if (main == null || inHand != main) why.append("main hand uid ").append(inHand).append(" (want ").append(main).append("); ");
        if (p.getInventory().selectedSlot != 0) why.append("selected slot ").append(p.getInventory().selectedSlot).append("; ");
        var slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            Integer want = pr.equipped.get(Gear.ARMOUR.get(i));
            int got = GearLooks.uid(p.getEquippedStack(slots[i]));
            if (want == null ? got != -1 : got != want) why.append(Gear.ARMOUR.get(i)).append(" uid ").append(got).append(" (want ").append(want).append("); ");
        }
        Integer off = pr.equipped.get("off");
        int offGot = GearLooks.uid(p.getOffHandStack());
        if (off != null && offGot != off) why.append("off-hand uid ").append(offGot).append(" (want ").append(off).append("); ");
        return why.toString();
    }

    static String statsWhy(ServerPlayerEntity p, GearStats.Loadout l) {
        double base = p.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
        double want = base + GearStats.healthBonus(l);
        double attack = p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE), attackWant = p.getAttributeBaseValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) + GearStats.meleeBonus(l);
        double armour = p.getAttributeValue(EntityAttributes.GENERIC_ARMOR), armourWant = p.getAttributeBaseValue(EntityAttributes.GENERIC_ARMOR) + GearStats.armourBonus(l);
        String why = "";
        if (!near(p.getMaxHealth(), want)) why += "max health " + p.getMaxHealth() + " (want " + want + "); ";
        if (!near(p.getHealth(), p.getMaxHealth())) why += "health " + p.getHealth() + " of " + p.getMaxHealth() + "; ";
        if (!near(attack, attackWant)) why += "attack " + attack + " (want " + attackWant + "); ";
        if (!near(armour, armourWant)) why += "armour " + armour + " (want " + armourWant + "); ";
        return why;
    }

    volatile String why = "";
    volatile boolean ok;

    public void step(MinecraftClient c, int tick) {
        switch (stage) {
            case 0 -> {
                if (tick == 20) {
                    c.setScreen(null);
                    UUID id = c.player.getUuid();
                    Phase1Checks.server(c, b -> {
                        player = id;
                        var pr = b.campaign.profile(id);
                        backup = Campaign.JSON.toJson(pr);
                        // a full role set at Epic with its starter weapons, so the stats differ from the iron kit and perks are on
                        pr.gear.clear(); pr.equipped.clear(); pr.nextGearUid = 1;
                        Gear.grantStarter(pr);
                        String top = Spec.GEARRARITIES[Spec.GEARRARITIES.length - 2].id();
                        for (int i = 0; i < pr.gear.size(); i++) { var g = pr.gear.get(i); pr.gear.set(i, new Gear.Piece(g.uid, g.item, top, 90, 100)); }
                        b.campaign.save(id);
                        ready = true;
                    });
                }
                if (ready) next(tick, 1);
            }
            case 1 -> { // p1-gear-combat-kit and -stats
                if (!battle(c, tick, "p1-gear-combat-kit")) return;
                if (!done) {
                    done = true;
                    Phase1Checks.server(c, b -> {
                        var p = sp(b, player); var pr = b.campaign.profile(player); var s = b.squads.get(player);
                        String k = kitWhy(b, p, pr);
                        Phase1Checks.report("p1-gear-combat-kit", k.isEmpty(), k.isEmpty() ? "main " + p.getMainHandStack().getName().getString() + ", off " + p.getOffHandStack().getName().getString() + ", four armour pieces carry the worn uids" : k);
                        var l = GearStats.of(pr);
                        String st = statsWhy(p, l) + (s == null || s.gear.role() == null || !s.gear.role().equals(l.role()) ? "squad gear not seated; " : "");
                        Phase1Checks.report("p1-gear-combat-stats", st.isEmpty(), st.isEmpty() ? "max health " + p.getMaxHealth() + " = base + " + GearStats.healthBonus(l) + ", attack and armour match, role " + l.role() : st);
                        healthBefore = p.getMaxHealth();
                        ok = true;
                    });
                }
                if (ok) { ok = false; next(tick, 2); }
            }
            case 2 -> { // p1-gear-combat-respawn: two downs, each respawned at once, leave the stats where they were
                int t = tick - stageAt;
                if (t == 1 || t == 60) Phase1Checks.server(c, b -> {
                    var p = sp(b, player);
                    p.damage(b.world.getDamageSources().generic(), 1000f);
                    if (b.respawns.containsKey(player)) { b.respawns.put(player, b.time + 5); downs++; }
                });
                if (t == 120) Phase1Checks.server(c, b -> {
                    var p = sp(b, player); var pr = b.campaign.profile(player);
                    String st = statsWhy(p, GearStats.of(pr)) + kitWhy(b, p, pr);
                    boolean pass = downs == 2 && st.isEmpty() && near(p.getMaxHealth(), healthBefore) && !p.isSpectator();
                    Phase1Checks.report("p1-gear-combat-respawn", pass, downs + " downs (want 2); max health " + p.getMaxHealth() + " (before " + healthBefore + ")" + (p.isSpectator() ? "; still a spectator" : "") + (st.isEmpty() ? "" : "; " + st));
                    ok = true;
                });
                if (ok) { ok = false; next(tick, 3); }
            }
            case 3 -> { // p1-gear-combat-empty: back in camp, nothing worn, a new battle arms him with the starter kit
                if (tick - stageAt == 1) Phase1Checks.server(c, b -> {
                    if (b.running) b.campaign.returnLobby();
                    b.campaign.profile(player).equipped.clear();
                    b.campaign.save(player);
                    battleReady = false;
                    ok = true;
                });
                if (ok) { ok = false; battleAt = -1; next(tick, 4); }
            }
            case 4 -> {
                if (!battle(c, tick, "p1-gear-combat-empty")) return;
                if (!done) {
                    done = true;
                    Phase1Checks.server(c, b -> {
                        var p = sp(b, player); var pr = b.campaign.profile(player);
                        var kit = GearCombat.wearing(pr);
                        String k = kitWhy(b, p, kit) + statsWhy(p, GearStats.of(kit));
                        boolean armed = !p.getMainHandStack().isEmpty() && GearLooks.uid(p.getMainHandStack()) > 0;
                        Phase1Checks.report("p1-gear-combat-empty", armed && k.isEmpty() && pr.equipped.isEmpty(),
                            "main " + p.getMainHandStack().getName().getString() + " (starter " + GearStats.of(kit).role() + " kit), profile still wears " + pr.equipped.size() + " pieces" + (k.isEmpty() ? "" : "; " + k));
                        ok = true;
                    });
                }
                if (ok) { ok = false; next(tick, 9); }
            }
            case 9 -> {
                if (tick - stageAt == 1) Phase1Checks.server(c, b -> restore());
                if (restored || tick - stageAt > 100) { Phase1Checks.finish(c, "gear-combat"); stage = 99; }
            }
            default -> { }
        }
    }
}
