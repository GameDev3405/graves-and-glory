package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

/**
 * Real-client checks for gear combat (plan 2026-11-03-gear-combat). GRAVES_PHASE1_CHECKS=gear-combat. Task 3: the kit in hand and armour is the
 * worn gear, max health is the Loadout's, two downs and respawns leave it unchanged, and a profile wearing nothing fights with the starter kit.
 * Task 4: a greatweapon sweeps foes near its target, a shield block lowers the shield on its cooldown, a bow draws at the family's speed, a staff
 * heals the friend it looks at, and a banner widens the command aura. Task 5: key 2 fires relic slot 1 in battle and keeps the hotbar on slot 0, selects
 * hotbar slot 1 in camp, and the six class relics keep their classes' numbers and clocks. Task 6: each new relic, set bonus and perk
 * moves its sheet number (p1-gear-combat-<effect>), worn alone on a Loadout built for the check. The profile is put back from a JSON copy taken at the start. Never shipped.
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
    /** The six class actives' numbers as the retired class sheet had them: cooldown, duration, radius, range, value1-3. */
    static final java.util.Map<String, double[]> OLD_CLASS = java.util.Map.of(
        "champion", new double[]{1800, 160, 12, 0, 0.25, 0, 0},
        "reaver", new double[]{1500, 200, 0, 0, 0.3, 3, 0},
        "warden", new double[]{1200, 240, 0, 40, 0.3, 0, 0},
        "marshal", new double[]{2400, 300, 0, 0, 0.2, 0, 0},
        "warlock", new double[]{2400, 600, 8, 30, 0.6, 6, 400},
        "quartermaster", new double[]{3000, 1200, 12, 30, 40, 0, 0});

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
            if (tick - stageAt > 600) { Phase1Checks.report(what, false, "the practice battle never started"); next(tick, 10); }
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

    volatile String why = "", shieldWhy = "";

    /** Wears a new top-but-one piece of the first sheet item of the family in the slot and re-arms the commander; the item id, or null. */
    static String wield(GravesAndGlory.Battle b, ServerPlayerEntity p, String family, String slot) {
        var pr = b.campaign.profile(p.getUuid());
        for (var it : Spec.GEARITEMS) if (it.family().equals(family) && it.slot().equals(slot)) {
            var piece = new Gear.Piece(pr.nextGearUid++, it.id(), Spec.GEARRARITIES[Spec.GEARRARITIES.length - 2].id(), 90, 100);
            pr.gear.add(piece);
            if (!Gear.equip(pr, piece.uid).isEmpty()) return null;
            b.equipPlayer(p);
            return it.id();
        }
        return null;
    }
    static String familyWhere(java.util.function.Predicate<Spec.GearFamiliesRow> test) {
        for (var f : Spec.GEARFAMILIES) if (test.test(f)) for (var it : Spec.GEARITEMS) if (it.slot().equals("main") && it.family().equals(f.id())) return f.id();
        return null;
    }
    /** Stands a troop still on the ground at x, z (its next attack and ability held off). */
    static void put(GravesAndGlory.Battle b, GravesAndGlory.Unit u, double x, double z, float yaw) {
        u.mob.getNavigation().stop();
        u.mob.refreshPositionAndAngles(x, Phase1Checks.top(b, x, z), z, yaw, 0);
        u.mob.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
        u.deployed = u.mob.getPos(); u.lastPosition = u.mob.getPos();
        Phase1Checks.freeze(u);
    }
    static float health(LivingEntity e) { return e.getHealth() + e.getAbsorptionAmount(); }

    /** A greatweapon blow on one foe: a second foe inside sweepRadius of it loses sweepShare of what the target lost, a third outside nothing. */
    static void sweep(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s) {
        String fam = familyWhere(f -> f.sweepRadius() > 0 && f.sweepShare() > 0);
        var foes = b.units.stream().filter(u -> u.squad.team != s.team && u.mob.isAlive()).toList();
        if (fam == null || wield(b, p, fam, "main") == null || foes.size() < 3) { Phase1Checks.report("p1-gear-combat-sweep", false, "no sweeping family (" + fam + ") or fewer than 3 foe troops (" + foes.size() + ")"); return; }
        var f = GearStats.family(fam);
        GearCombat.swings.remove(p.getUuid());
        var target = foes.get(0);
        GravesAndGlory.Unit in = null, out = null;
        for (var u : foes.subList(1, foes.size())) if (in == null && u.spec.id().equals(target.spec.id())) in = u;
        for (var u : foes.subList(1, foes.size())) if (u != in && out == null) out = u;
        if (in == null) for (var u : foes.subList(1, foes.size())) if (u != out) { in = u; break; }
        double x = p.getX() + 3, z = p.getZ();
        put(b, target, x, z, 0);
        put(b, in, x, z + f.sweepRadius() * 0.5, 0);
        put(b, out, x, z - f.sweepRadius() - 1.5, 0);
        for (var u : List.of(target, in, out)) u.mob.timeUntilRegen = 0;
        float t0 = health(target.mob), i0 = health(in.mob), o0 = health(out.mob);
        target.mob.damage(b.world.getDamageSources().playerAttack(p), 4f);
        float tl = t0 - health(target.mob), il = i0 - health(in.mob), ol = o0 - health(out.mob);
        boolean same = in.spec.id().equals(target.spec.id());
        boolean ratio = !same || Math.abs(il - tl * f.sweepShare()) < 0.05 * Math.max(1, tl);
        boolean pass = tl > 0 && il > 0 && ol == 0 && ratio;
        Phase1Checks.report("p1-gear-combat-sweep", pass, fam + ": target lost " + tl + ", the foe " + f.sweepRadius() * 0.5 + " blocks off lost " + il + (same ? " (want " + tl * f.sweepShare() + ")" : " (another troop type)") + ", the foe " + (f.sweepRadius() + 1.5) + " blocks off lost " + ol);
    }

    /** C02-F1: seating a squad (round setup) restarts its commander's heavy_hand swing count, so a count from the last round never carries in. */
    static void swingReset(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s) {
        GearCombat.swings.put(p.getUuid(), 2);
        GearCombat.seat(s, b.campaign.profile(p.getUuid()));
        Integer left = GearCombat.swings.get(p.getUuid());
        Phase1Checks.report("p1-gear-combat-swing-reset", left == null, "after two swings the squad was seated again: swing count " + left + " (want none, so the next sweep is swing 1)");
        GearCombat.swings.remove(p.getUuid());
    }

    /** A banner's command aura: a troop just inside commandRadius gains troopFactor, one just outside nothing, and the radius beats the plain one. */
    static void banner(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s) {
        String fam = familyWhere(f -> f.auraMult() > 1);
        var mine = b.units.stream().filter(u -> u.squad == s && u.mob.isAlive()).findFirst().orElse(null);
        var foe = b.units.stream().filter(u -> u.squad.team != s.team && u.mob.isAlive()).findFirst().orElse(null);
        if (fam == null || mine == null || foe == null || wield(b, p, fam, "main") == null) { Phase1Checks.report("p1-gear-combat-banner", false, "no banner family (" + fam + "), own troop or foe"); return; }
        double r = GearEffects.commandRadius(s.gear);
        var a = b.assignment(mine);
        double want = GearEffects.troopFactor(s.gear, true, a != null && "attack".equals(a.order));
        put(b, mine, p.getX() + r - 0.5, p.getZ(), 0);
        double inside = Commander.factor(b, mine, foe.mob);
        put(b, mine, p.getX() + r + 0.5, p.getZ(), 0);
        double outside = Commander.factor(b, mine, foe.mob);
        boolean pass = r > GearStats.combat().commandRadius() && want > 1 && Math.abs(inside / outside - want) < 1e-6;
        Phase1Checks.report("p1-gear-combat-banner", pass, fam + ": aura " + r + " blocks (plain " + GearStats.combat().commandRadius() + "), inside x" + inside + ", outside x" + outside + " (want a x" + want + " step)");
    }

    /** A staff aimed at a hurt troop of the commander heals healBase x healingFactor (or what it misses) and goes on cooldown. */
    static void staff(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s) {
        String fam = familyWhere(f -> f.healBase() > 0 && f.range() > 0);
        var mine = b.units.stream().filter(u -> u.squad == s && u.mob.isAlive()).findFirst().orElse(null);
        if (fam == null || mine == null || wield(b, p, fam, "main") == null) { Phase1Checks.report("p1-gear-combat-staff", false, "no staff family (" + fam + ") or own troop"); return; }
        var f = GearStats.family(fam);
        put(b, mine, p.getX() + 4, p.getZ(), 0);
        mine.mob.setHealth(Math.max(1, mine.mob.getMaxHealth() - 10));
        var eye = p.getEyePos(); var at = mine.mob.getBoundingBox().getCenter().subtract(eye);
        float yaw = (float) (Math.toDegrees(Math.atan2(at.z, at.x)) - 90), pitch = (float) -Math.toDegrees(Math.atan2(at.y, Math.hypot(at.x, at.z)));
        p.setYaw(yaw); p.setHeadYaw(yaw); p.setPitch(pitch);
        float before = mine.mob.getHealth();
        double want = Math.min(f.healBase() * GearStats.healingFactor(s.gear), mine.mob.getMaxHealth() - before);
        var stack = p.getMainHandStack();
        var result = p.interactionManager.interactItem(p, b.world, stack, Hand.MAIN_HAND);
        float got = mine.mob.getHealth() - before;
        boolean cooling = p.getItemCooldownManager().isCoolingDown(stack.getItem());
        boolean pass = Math.abs(got - want) < 1e-3 && cooling && result.isAccepted();
        Phase1Checks.report("p1-gear-combat-staff", pass, fam + ": healed " + got + " (want " + want + "), " + result + ", cooling " + cooling);
    }

    /** A bow loosed after N ticks flies as if drawn N x drawFactor ticks: its arrow's speed is the vanilla pull of those ticks x3. */
    static void bow(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s) {
        String fam = familyWhere(f -> f.item().equals("minecraft:bow") && f.drawMult() != 1);
        if (fam == null || wield(b, p, fam, "main") == null || !(p.getMainHandStack().getItem() instanceof BowItem)) { Phase1Checks.report("p1-gear-combat-bow", false, "no bow family with its own draw (" + fam + ")"); return; }
        int n = 8;
        int drawn = (int) Math.round(n * GearEffects.drawFactor(s.gear));
        double want = BowItem.getPullProgress(drawn) * 3, vanilla = BowItem.getPullProgress(n) * 3;
        p.setPitch(0);
        var stack = p.getMainHandStack();
        stack.onStoppedUsing(b.world, p, stack.getMaxUseTime(p) - n);
        PersistentProjectileEntity arrow = null;
        for (var e : b.world.iterateEntities()) if (e instanceof PersistentProjectileEntity a && a.getOwner() == p && (arrow == null || a.getId() > arrow.getId())) arrow = a;
        double speed = arrow == null ? 0 : arrow.getVelocity().length();
        boolean pass = arrow != null && Math.abs(speed - want) < 0.03 * want && Math.abs(want - vanilla) > 0.06 * want;
        Phase1Checks.report("p1-gear-combat-bow", pass, fam + ": loosed after " + n + " ticks, flew at " + speed + " (want " + want + " for " + drawn + " ticks; vanilla " + vanilla + ")");
        if (arrow != null) arrow.discard();
    }
    volatile boolean ok;
    volatile String keyWhy = "", campScreen = "";
    volatile boolean inBattle, fired, drawn, camp;
    int entranceWait; // ticks p1-gear-combat-relic-keys waited for the battle entrance camera (120 client ticks) to end
    volatile int clientSlot = -1, serverSlot = -1;
    /** Presses hotbar key i+1 as the keyboard would: one press counted on the binding. */
    static void press(MinecraftClient c, int i) {
        net.minecraft.client.option.KeyBinding.onKeyPressed(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(c.options.hotbarKeys[i]));
    }

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
                    if (!b.respawns.containsKey(player)) { p.timeUntilRegen = 0; p.damage(b.world.getDamageSources().generic(), 1000f); } // a worn second_chance catches the first
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
                if (ok) { ok = false; next(tick, 5); }
            }
            case 5 -> { // p1-gear-combat-sweep, -banner, -staff and -bow: one server tick each, nothing moves between placing and measuring
                if (tick - stageAt == 1) Phase1Checks.server(c, b -> {
                    var p = sp(b, player); var s = b.squads.get(player);
                    sweep(b, p, s); banner(b, p, s); staff(b, p, s); bow(b, p, s); swingReset(b, p, s);
                    ok = true;
                });
                if (ok) { ok = false; next(tick, 6); }
            }
            case 6 -> { // p1-gear-combat-shield: the client raises the shield by its own use key, a troop in front strikes it
                int t = tick - stageAt;
                if (t == 1) Phase1Checks.server(c, b -> {
                    var p = sp(b, player);
                    String one = null;
                    for (var it : Spec.GEARITEMS) if (it.slot().equals("main") && it.hands() == 1) { one = it.family(); break; }
                    String off = null;
                    for (var f : Spec.GEARFAMILIES) if (f.cooldownTicks() > 0 && f.healBase() <= 0) for (var it : Spec.GEARITEMS) if (it.slot().equals("off") && it.family().equals(f.id())) off = f.id();
                    shieldWhy = one == null || off == null ? "no one-handed main or cooling off-hand family in the sheet" : (wield(b, p, one, "main") == null || wield(b, p, off, "off") == null ? "could not wear " + one + " and " + off : "");
                });
                if (t == 4 && shieldWhy.isEmpty()) { c.options.useKey.setPressed(true); c.interactionManager.interactItem(c.player, Hand.OFF_HAND); }
                if (t == 14) Phase1Checks.server(c, b -> {
                    var p = sp(b, player); var s = b.squads.get(player);
                    String why = shieldWhy;
                    var f = GearStats.family(s.gear.offFamily());
                    var foe = b.units.stream().filter(u -> u.squad.team != s.team && u.mob.isAlive()).findFirst().orElse(null);
                    if (why.isEmpty() && (foe == null || f == null)) why = "no foe troop or no off-hand family";
                    boolean raised = p.isBlocking();
                    if (why.isEmpty()) {
                        var look = p.getRotationVec(1f);
                        put(b, foe, p.getX() + look.x * 2, p.getZ() + look.z * 2, p.getYaw() + 180);
                        p.timeUntilRegen = 0;
                        p.damage(b.world.getDamageSources().mobAttack(foe.mob), 2f);
                        int cd = Math.round(p.getItemCooldownManager().getCooldownProgress(Items.SHIELD, 0) * f.cooldownTicks());
                        boolean pass = raised && p.getItemCooldownManager().isCoolingDown(Items.SHIELD) && !p.isBlocking();
                        Phase1Checks.report("p1-gear-combat-shield", pass, "raised " + raised + ", after the block cooling " + p.getItemCooldownManager().isCoolingDown(Items.SHIELD) + " (about " + cd + " of " + f.cooldownTicks() + " ticks left), still blocking " + p.isBlocking());
                    } else Phase1Checks.report("p1-gear-combat-shield", false, why);
                    ok = true;
                });
                if (t == 15) c.options.useKey.setPressed(false);
                if (t > 15 && ok) { ok = false; next(tick, 7); }
            }
            case 7 -> { // p1-gear-combat-relic-keys: in battle key 2 fires slot 1 and the hotbar stays on slot 0; in camp key 2 selects slot 1
                int t = tick - stageAt;
                // the entrance camera hides the battle HUD (renderHud), so start only once it has ended (bounded: the report then shows it was still on)
                if (t == 1 && CampaignClient.cinematic() && entranceWait < 200) { entranceWait++; stageAt = tick; return; }
                if (t == 1) Phase1Checks.server(c, b -> {
                    var p = sp(b, player); var pr = b.campaign.profile(player); var s = b.squads.get(player);
                    if (!pr.relics.contains("stand_fast")) pr.relics.add("stand_fast");
                    keyWhy = Gear.setRelic(pr, 1, "stand_fast");
                    Commander.stop(b, s); s.relicState.clear(); GearCombat.seat(s, pr);
                    b.campaign.introUntil = b.time; p.getInventory().selectedSlot = 0;
                    GravesAndGlory.sendRelics(b, s);
                });
                if (t == 12) { inBattle = GravesAndGloryClient.battleActive() && c.currentScreen == null && !CampaignClient.cinematic(); press(c, 1); }
                if (t == 30) {
                    clientSlot = c.player.getInventory().selectedSlot; drawn = BattleHud.rects().containsKey("relic-1");
                    Phase1Checks.server(c, b -> {
                        var p = sp(b, player); var s = b.squads.get(player);
                        fired = Commander.running(b, s, "stand_fast") && s.relicState.readyAt[1] > 0 && s.relicState.readyAt[0] == 0;
                        serverSlot = p.getInventory().selectedSlot;
                        Commander.stop(b, s);
                        b.campaign.returnLobby(); battleReady = false;
                    });
                }
                if (t == 60) { camp = !GravesAndGloryClient.battleActive(); campScreen = c.currentScreen == null ? "none" : c.currentScreen.getClass().getSimpleName(); c.setScreen(null); c.player.getInventory().selectedSlot = 0; }
                if (t == 62) press(c, 1);
                if (t == 75) {
                    int got = c.player.getInventory().selectedSlot;
                    boolean pass = keyWhy.isEmpty() && inBattle && fired && clientSlot == 0 && serverSlot == 0 && drawn && camp && got == 1;
                    Phase1Checks.report("p1-gear-combat-relic-keys", pass, "slot 1 holds stand_fast (" + (keyWhy.isEmpty() ? "set" : keyWhy) + "); in battle=" + inBattle + " key 2 started slot 1 only=" + fired
                        + ", the hotbar stayed on slot " + clientSlot + " (client) and " + serverSlot + " (server), the HUD drew relic-1=" + drawn + " (waited " + entranceWait + " ticks for the entrance camera); in camp=" + camp + " (screen " + campScreen + ", closed) key 2 selected hotbar slot " + got + " (want 1)");
                    c.player.getInventory().selectedSlot = 0;
                    battleAt = -1; next(tick, 8);
                }
            }
            case 8 -> { // p1-gear-combat-ported: each class relic keeps its class's numbers, and seated in slot 0 it starts on those clocks or refuses as before
                if (!battle(c, tick, "p1-gear-combat-ported")) return;
                if (!done) {
                    done = true;
                    Phase1Checks.server(c, b -> {
                        var p = sp(b, player); var pr = b.campaign.profile(player); var s = b.squads.get(player);
                        StringBuilder out = new StringBuilder(); boolean pass = true; int ported = 0, started = 0;
                        b.campaign.introUntil = b.time;
                        for (var r : Spec.RELICS) {
                            if (r.fromClass().equals("-")) continue;
                            ported++;
                            double[] k = OLD_CLASS.get(r.fromClass()); // the numbers the class sheet carried before gear replaced classes
                            var tu = Commander.tuning(r.id());
                            boolean same = k != null && tu != null && r.id().equals(tu.effect()) && (int) k[0] == r.cooldownTicks() && (int) k[1] == tu.durationTicks()
                                && near(k[2], tu.radius()) && near(k[3], tu.range()) && near(k[4], tu.value1()) && near(k[5], tu.value2()) && near(k[6], tu.value3());
                            if (!pr.relics.contains(r.id())) pr.relics.add(r.id());
                            String set = Gear.setRelic(pr, 0, r.id());
                            Commander.stop(b, s); s.relicState.clear(); GearCombat.seat(s, pr);
                            b.campaign.notices.remove(player);
                            Commander.activate(b, p, 0);
                            String notice = b.campaign.notices.getOrDefault(player, "");
                            boolean ran = s.relicState.readyAt[0] > 0, clocks;
                            int cd = Commander.cooldownTicks(s, r);
                            if (ran) { started++; clocks = Commander.readyIn(b, s, 0) == cd && (tu.durationTicks() == 0 || Commander.activeFor(b, s, 0) == tu.durationTicks()); }
                            else clocks = !notice.isEmpty() && Commander.readyIn(b, s, 0) == 0;
                            pass &= same && set.isEmpty() && clocks;
                            out.append(r.id()).append(" (").append(r.fromClass()).append("): sheet same=").append(same).append(ran ? ", started: ready in " + Commander.readyIn(b, s, 0) + " (want " + cd + "), runs " + Commander.activeFor(b, s, 0) + " (want " + tu.durationTicks() + ")" : ", refused \"" + notice + "\"").append("; ");
                            Commander.stop(b, s); s.relicState.clear();
                            p.clearStatusEffects();
                        }
                        pass &= ported == 6 && started >= 3;
                        Phase1Checks.report("p1-gear-combat-ported", pass, ported + " class relics, " + started + " started (stand_fast, rally_horn and bloodrush always can): " + out);
                        ok = true;
                    });
                }
                if (ok) { ok = false; next(tick, 9); }
            }
            case 9 -> { // Task 6: p1-gear-combat-<effect>, one per new relic, set bonus and perk, in the same battle
                if (tick - stageAt == 1) Phase1Checks.server(c, b -> {
                    var p = sp(b, player); var s = b.squads.get(player);
                    b.campaign.introUntil = b.time;
                    try { effects(b, p, s); } catch (RuntimeException e) { Phase1Checks.report("p1-gear-combat-effects", false, "threw " + e); e.printStackTrace(); }
                    ok = true;
                });
                if (ok || tick - stageAt > 200) { ok = false; next(tick, 10); }
            }
            case 10 -> {
                if (tick - stageAt == 1) Phase1Checks.server(c, b -> restore());
                if (restored || tick - stageAt > 100) { Phase1Checks.finish(c, "gear-combat"); stage = 99; }
            }
            default -> { }
        }
    }

    // ---- Task 6: one check per new relic, set-bonus and perk effect. Each wears a Loadout with only that effect (the sheet row by effect id) on the
    // commander's own totals, places troops, then measures the number the sheet value moves. One server task, so nothing moves between placing and measuring.

    /** l with only these effects' set bonuses and perks (the first sheet row of each effect id), its totals, families and relics kept. */
    static GearStats.Loadout only(GearStats.Loadout l, String... effects) {
        List<Spec.GearSetBonusesRow> bs = new ArrayList<>(); List<Spec.GearPerksRow> ks = new ArrayList<>();
        for (String e : effects) {
            boolean found = false;
            for (var r : Spec.GEARSETBONUSES) if (r.effect().equals(e)) { bs.add(r); found = true; break; }
            if (!found) for (var r : Spec.GEARPERKS) if (r.effect().equals(e)) { ks.add(r); break; }
        }
        return new GearStats.Loadout(l.totals(), l.setCounts(), bs, ks, l.role(), l.mainFamily(), l.offFamily(), l.relics());
    }
    static Spec.RelicTuningRow tuningOf(String effect) { for (var t : Spec.RELICTUNING) if (t.effect().equals(effect)) return t; return null; }
    static boolean close(double a, double b, double tol) { return Math.abs(a - b) <= tol; }
    /** A spot d blocks from the player along his flat look turned by deg degrees. */
    static double[] ahead(ServerPlayerEntity p, double d, double deg) {
        var v = net.minecraft.util.math.Vec3d.fromPolar(0, p.getYaw() + (float) deg);
        return new double[]{p.getX() + v.x * d, p.getZ() + v.z * d};
    }
    static void at(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Unit u, double d, double deg) { var xz = ahead(p, d, deg); put(b, u, xz[0], xz[1], p.getYaw() + 180); u.mob.timeUntilRegen = 0; }
    static double flat(net.minecraft.util.math.Vec3d v) { return Math.sqrt(v.x * v.x + v.z * v.z); }

    void effects(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s) {
        var gear = s.gear;
        var mine = b.units.stream().filter(u -> u.squad == s && u.mob.isAlive()).toList();
        var foes = b.units.stream().filter(u -> u.squad.team != s.team && u.mob.isAlive()).toList();
        if (mine.size() < 2 || foes.size() < 3) { Phase1Checks.report("p1-gear-combat-effects", false, mine.size() + " own and " + foes.size() + " foe troops (want 2 and 3)"); return; }
        Commander.stop(b, s); s.relicState.clear();
        p.clearStatusEffects(); p.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
        for (var u : foes) put(b, u, p.getX() - 60, p.getZ() + 60, 0); // every foe out of reach first
        for (var u : mine) put(b, u, p.getX() + 30, p.getZ() - 30, 0);
        var m0 = mine.get(0); var m1 = mine.get(1);
        var f0 = foes.get(0); var f1 = foes.get(1); var f2 = foes.get(2);
        float pm = p.getMaxHealth();

        // pierce and volley: the Volley's arrows, under pierce and without it
        { var t = tuningOf("volley"); var act = RelicEffects.ACTIVES.get("volley");
          s.gear = only(gear, "pierce");
          java.util.function.Supplier<List<net.minecraft.entity.projectile.ArrowEntity>> mineArrows = () -> b.world.getEntitiesByClass(net.minecraft.entity.projectile.ArrowEntity.class, p.getBoundingBox().expand(4), a -> a.getOwner() == p);
          mineArrows.get().forEach(net.minecraft.entity.Entity::discard);
          boolean started = act != null && t != null && act.start(b, p, s, 2, t);
          var shot = mineArrows.get();
          double lo = 999, hi = -999; boolean speeds = true; int pierced = 0;
          for (var a : shot) { var v = a.getVelocity(); double yaw = Math.toDegrees(Math.atan2(-v.x, v.z)); double rel = net.minecraft.util.math.MathHelper.wrapDegrees(yaw - p.getYaw()); lo = Math.min(lo, rel); hi = Math.max(hi, rel); speeds &= close(v.length(), t.value3(), 0.1); if (a.getPierceLevel() == GearEffects.pierceLevel(s.gear)) pierced++; }
          shot.forEach(net.minecraft.entity.Entity::discard);
          int want = t == null ? -1 : (int) Math.round(t.value1());
          Phase1Checks.report("p1-gear-combat-volley", started && shot.size() == want && speeds && close(hi - lo, t.value2(), 1.0),
              "loosed " + shot.size() + " arrows (want " + want + "), speed ~" + t.value3() + " " + speeds + ", fan " + (hi - lo) + " degrees (want " + t.value2() + ")");
          s.gear = only(gear);
          act.start(b, p, s, 2, t);
          var plain = mineArrows.get(); int plainLevel = plain.isEmpty() ? -1 : plain.get(0).getPierceLevel(); plain.forEach(net.minecraft.entity.Entity::discard);
          int lv = (int) Math.round(GearStats.value1(only(gear, "pierce"), "pierce"));
          Phase1Checks.report("p1-gear-combat-pierce", lv > 0 && pierced == shot.size() && !shot.isEmpty() && plainLevel == 0, "with pierce " + pierced + " of " + shot.size() + " arrows pierce " + lv + "; without it the pierce level is " + plainLevel + " (want 0)"); }

        // dash: the velocity it sets
        { var t = tuningOf("dash"); var act = RelicEffects.ACTIVES.get("dash");
          p.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
          boolean started = act != null && t != null && act.start(b, p, s, 2, t);
          var v = p.getVelocity(); var look = net.minecraft.util.math.Vec3d.fromPolar(0, p.getYaw());
          double along = (v.x * look.x + v.z * look.z);
          Phase1Checks.report("p1-gear-combat-dash", started && close(flat(v), t.value1(), 1e-3) && close(along, t.value1(), 1e-3), "flat speed " + flat(v) + " along the look " + along + " (want " + t.value1() + ")");
          p.setVelocity(net.minecraft.util.math.Vec3d.ZERO); p.velocityModified = true; }

        // shove: a foe in front is hit, slowed and thrown back; one behind is not
        { var t = tuningOf("shove"); var act = RelicEffects.ACTIVES.get("shove");
          at(b, p, f0, 2, 0); at(b, p, f1, 2, 180);
          float h0 = health(f0.mob), h1 = health(f1.mob);
          boolean started = act != null && t != null && act.start(b, p, s, 2, t);
          var dir = new net.minecraft.util.math.Vec3d(f0.mob.getX() - p.getX(), 0, f0.mob.getZ() - p.getZ()).normalize();
          var v = f0.mob.getVelocity(); double away = v.x * dir.x + v.z * dir.z;
          double kr = f0.mob.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), wantKb = t.value1() * (1 - kr);
          boolean slowed = f0.mob.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS), behindSlowed = f1.mob.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);
          float lost = h0 - health(f0.mob), lostBehind = h1 - health(f1.mob);
          var slow = f0.mob.getStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);
          Phase1Checks.report("p1-gear-combat-shove", started && lost > 0 && slowed && away >= wantKb * 0.9 && lostBehind == 0 && !behindSlowed && slow != null && close(slow.getDuration(), t.value2(), 1),
              "in front lost " + lost + " (blow " + t.value3() + "), thrown " + away + " away (want at least " + wantKb + "), slowed " + slowed + " for " + (slow == null ? 0 : slow.getDuration()) + " (want " + t.value2() + "); behind lost " + lostBehind + ", slowed " + behindSlowed);
          f0.mob.clearStatusEffects(); f0.mob.setVelocity(net.minecraft.util.math.Vec3d.ZERO); f1.mob.clearStatusEffects();
          for (var u : List.of(f0, f1)) put(b, u, p.getX() - 60, p.getZ() + 60, 0); }

        // heal_pulse: he and an own troop inside the radius heal value1 x healingFactor; one outside does not
        { var t = tuningOf("heal_pulse"); var act = RelicEffects.ACTIVES.get("heal_pulse");
          s.gear = only(gear);
          at(b, p, m0, t.radius() - 1, 90); at(b, p, m1, t.radius() + 2, 90);
          for (var u : List.of(m0, m1)) u.mob.setHealth(Math.max(1, u.mob.getMaxHealth() - 20));
          p.setHealth(Math.max(1, pm - 20));
          float a0 = m0.mob.getHealth(), a1 = m1.mob.getHealth(), ap = p.getHealth();
          boolean started = act != null && act.start(b, p, s, 2, t);
          double want = t.value1() * GearStats.healingFactor(s.gear);
          float g0 = m0.mob.getHealth() - a0, g1 = m1.mob.getHealth() - a1, gp = p.getHealth() - ap;
          Phase1Checks.report("p1-gear-combat-heal_pulse", started && close(g0, want, 0.01) && close(gp, want, 0.01) && g1 == 0, "inside healed " + g0 + ", he healed " + gp + " (want " + want + "), outside " + g1);
          p.setHealth(pm); }

        // self_heal: value1 x healingFactor over durationTicks ticks, refused at full health
        { var t = tuningOf("self_heal"); var act = RelicEffects.ACTIVES.get("self_heal");
          b.campaign.notices.remove(player);
          boolean refused = act != null && !act.start(b, p, s, 2, t) && !b.campaign.notices.getOrDefault(player, "").isEmpty();
          p.setHealth(Math.max(1, pm - 15));
          float a = p.getHealth();
          boolean started = act != null && act.start(b, p, s, 2, t);
          for (int i = 0; i < t.durationTicks(); i++) act.tick(b, s, 2, t);
          double want = t.value1() * GearStats.healingFactor(s.gear); float got = p.getHealth() - a;
          Phase1Checks.report("p1-gear-combat-self_heal", refused && started && close(got, want, 0.05), "healed " + got + " over " + t.durationTicks() + " ticks (want " + want + "); at full health refused " + refused);
          p.setHealth(pm); }

        // war_cry: an own troop inside the radius hits x(1 + value1) while it runs, one outside x1
        { var t = tuningOf("war_cry");
          at(b, p, m0, t.radius() - 1, 90); at(b, p, m1, t.radius() + 2, 90); at(b, p, f0, 25, 0);
          double i0 = Commander.factor(b, m0, f0.mob), o0 = Commander.factor(b, m1, f0.mob);
          String was = s.relics[2]; s.relics[2] = relicWith("war_cry");
          s.relicState.begin(2, b.time, 0, t.durationTicks());
          double i1 = Commander.factor(b, m0, f0.mob), o1 = Commander.factor(b, m1, f0.mob);
          Commander.stop(b, s, 2); s.relicState.clear(); s.relics[2] = was;
          Phase1Checks.report("p1-gear-combat-war_cry", close(i1 / i0, 1 + t.value1(), 1e-6) && close(o1 / o0, 1, 1e-9), "inside x" + (i1 / i0) + " (want " + (1 + t.value1()) + "), outside x" + (o1 / o0));
          put(b, f0, p.getX() - 60, p.getZ() + 60, 0); }

        // surrounded_guard: with surroundedCount foes within surroundedRadius he takes x(1 - value1); with one fewer x1
        { s.gear = only(gear, "surrounded_guard");
          var C = GearStats.combat(); double r = C.surroundedRadius() - 1;
          List<GravesAndGlory.Unit> ring = foes.subList(0, Math.min(foes.size(), C.surroundedCount()));
          for (int i = 0; i < ring.size(); i++) at(b, p, ring.get(i), r, 360.0 * i / ring.size());
          double inside = Commander.takenFactor(b, p);
          put(b, ring.get(0), p.getX() - 60, p.getZ() + 60, 0);
          double fewer = Commander.takenFactor(b, p);
          double want = 1 - GearStats.value1(s.gear, "surrounded_guard");
          Phase1Checks.report("p1-gear-combat-surrounded_guard", ring.size() == C.surroundedCount() && close(inside, want, 1e-9) && close(fewer, 1, 1e-9), ring.size() + " foes within " + r + ": x" + inside + " (want " + want + "), one fewer x" + fewer);
          for (var u : ring) put(b, u, p.getX() - 60, p.getZ() + 60, 0); }

        // taunt_aura: of the foes within its radius, tauntShare of them (rounded) pick him; without it none
        { s.gear = only(gear, "taunt_aura");
          double r = GearStats.radius(s.gear, "taunt_aura");
          List<GravesAndGlory.Unit> near = foes.subList(0, Math.min(foes.size(), 4));
          for (int i = 0; i < near.size(); i++) at(b, p, near.get(i), r - 2, 360.0 * i / near.size());
          int within = 0, drawn = 0;
          for (var u : foes) { if (u.mob.squaredDistanceTo(p) <= r * r) within++; if (GearCombat.taunter(b, u) == p) drawn++; }
          int want = (int) Math.round(GearEffects.tauntShare(s.gear) * within);
          s.gear = only(gear); int plain = 0; for (var u : foes) if (GearCombat.taunter(b, u) == p) plain++;
          Phase1Checks.report("p1-gear-combat-taunt_aura", within == near.size() && want > 0 && drawn == want && plain == 0, drawn + " of the " + within + " foes within " + r + " pick him (want " + want + ", share " + GearStats.combat().tauntShare() + "); without it " + plain);
          for (var u : near) put(b, u, p.getX() - 60, p.getZ() + 60, 0); }

        // oathkeeper: an own troop inside the command aura takes x(1 - value1), one outside x1
        { s.gear = only(gear, "oathkeeper");
          double r = GearEffects.commandRadius(s.gear);
          at(b, p, m0, r - 1, 90); at(b, p, m1, r + 2, 90);
          double in = Commander.takenFactor(b, m0.mob), out = Commander.takenFactor(b, m1.mob), want = 1 - GearStats.value1(s.gear, "oathkeeper");
          Phase1Checks.report("p1-gear-combat-oathkeeper", close(in, want, 1e-9) && close(out, 1, 1e-9), "inside the " + r + "-block aura x" + in + " (want " + want + "), outside x" + out); }

        // command_radius: the aura reaches value1 x auraMult further
        { var f = GearStats.family(gear.mainFamily()); double mult = f == null ? 1 : f.auraMult();
          double plain = GearEffects.commandRadius(only(gear)), wide = GearEffects.commandRadius(only(gear, "command_radius")), want = GearStats.value1(only(gear, "command_radius"), "command_radius") * mult;
          s.gear = only(gear, "command_radius");
          at(b, p, m0, (plain + wide) / 2, 90); at(b, p, f0, 25, 0);
          double in = Commander.factor(b, m0, f0.mob), wantF = GearEffects.troopFactor(s.gear, true, false) * Statuses.markFactor(b, s, f0.mob);
          s.gear = only(gear); double before = Commander.factor(b, m0, f0.mob);
          Phase1Checks.report("p1-gear-combat-command_radius", want > 0 && close(wide - plain, want, 1e-9) && close(in / before, GearEffects.troopFactor(only(gear, "command_radius"), true, false), 1e-6),
              "aura " + plain + " -> " + wide + " blocks (want +" + want + "); a troop between them x" + (in / before) + " (want x" + GearEffects.troopFactor(only(gear, "command_radius"), true, false) + ")");
          put(b, f0, p.getX() - 60, p.getZ() + 60, 0); }

        // formation_charge: an own troop on Attack inside the aura gets the speed and x(1 + value2); one outside neither
        { s.gear = only(gear, "formation_charge");
          double r = GearEffects.commandRadius(s.gear);
          String group = m0.spec.group(); var keep = s.assignments.get(group);
          s.assignments.put(group, new GravesAndGlory.Assignment("attack", "block", m0.mob.getPos(), 0));
          at(b, p, m0, r - 1, 90); at(b, p, m1, r + 3, 90); at(b, p, f0, 25, 0);
          GearCombat.formation(b, s, p);
          var mod = m0.mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).getModifier(GearCombat.FORMATION);
          var outMod = m1.mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).getModifier(GearCombat.FORMATION);
          double wantMod = BattleMath.speedModifier(1 + GearStats.value1(s.gear, "formation_charge"), m0.mob.isTouchingWater());
          double withF = Commander.factor(b, m0, f0.mob);
          s.gear = only(gear); double plainF = Commander.factor(b, m0, f0.mob);
          GearCombat.formation(b, s, p);
          boolean gone = m0.mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).getModifier(GearCombat.FORMATION) == null;
          if (keep == null) s.assignments.remove(group); else s.assignments.put(group, keep);
          boolean sameGroup = m1.spec.group().equals(group);
          double wantStep = 1 + GearStats.value2(only(gear, "formation_charge"), "formation_charge");
          Phase1Checks.report("p1-gear-combat-formation_charge", mod != null && close(mod.value(), wantMod, 1e-9) && (!sameGroup || outMod == null) && gone && close(withF / plainF, wantStep, 1e-6),
              "inside on Attack speed " + (mod == null ? "none" : mod.value()) + " (want " + wantMod + "), outside " + (outMod == null ? "none" : outMod.getValue()) + (sameGroup ? "" : " (another group)") + ", gone without it " + gone + "; blows x" + (withF / plainF) + " (want " + wantStep + ")");
          put(b, f0, p.getX() - 60, p.getZ() + 60, 0); }

        // heal_over_time: each second an own troop inside the radius heals value1, one outside nothing
        { s.gear = only(gear, "heal_over_time");
          double r = GearStats.radius(s.gear, "heal_over_time");
          at(b, p, m0, r - 1, 90); at(b, p, m1, r + 2, 90);
          for (var u : List.of(m0, m1)) u.mob.setHealth(Math.max(1, u.mob.getMaxHealth() - 10));
          float a0 = m0.mob.getHealth(), a1 = m1.mob.getHealth();
          GearCombat.healOverTime(b, s, p);
          float g0 = m0.mob.getHealth() - a0, g1 = m1.mob.getHealth() - a1; double want = GearStats.value1(s.gear, "heal_over_time");
          Phase1Checks.report("p1-gear-combat-heal_over_time", close(g0, want, 1e-3) && g1 == 0, "a second healed " + g0 + " inside " + r + " (want " + want + "), outside " + g1); }

        // cleanse: slow and weaken leave him and an own troop inside the radius; one outside keeps its slow; due every value1 seconds
        { s.gear = only(gear, "cleanse");
          double r = GearStats.radius(s.gear, "cleanse");
          at(b, p, m0, r - 1, 90); at(b, p, m1, r + 2, 90);
          Abilities.slow(b, m0.mob, 1, 200); Abilities.slow(b, m1.mob, 1, 200); Statuses.apply(b, null, m0.mob, "weaken", 200, 0, 0.2);
          p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS, 200, 1));
          boolean set = m0.mob.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS) && Statuses.has(b, m0.mob, "weaken");
          GearCombat.cleanse(b, s, p);
          boolean inClean = !m0.mob.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS) && !Statuses.has(b, m0.mob, "weaken");
          boolean heClean = !p.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS), outKept = m1.mob.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);
          long every = Math.round(GearStats.value1(s.gear, "cleanse")); int due = 0; long t0 = b.time;
          for (long k = 0; k < every * 20 * 3; k++) { b.time = t0 + k; if (k % 20 == 0 && GearCombat.cleanseDue(b, s)) due++; }
          b.time = t0;
          m1.mob.clearStatusEffects();
          Phase1Checks.report("p1-gear-combat-cleanse", set && inClean && heClean && outKept && due == 3, "inside cleansed " + inClean + ", he " + heClean + ", outside kept its slow " + outKept + "; due " + due + " times in " + every * 3 + " s (want 3)"); }

        // trailblazer: x(1 + value1) speed with no foe within value2, gone once one stands there
        { s.gear = only(gear, "trailblazer");
          double r = GearStats.value2(s.gear, "trailblazer");
          int foesBefore = GearCombat.foesNear(b, p, s.team, r);
          Abilities.keepSpeed(p, GearCombat.TRAIL, "", 0); double base = p.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
          GearCombat.trail(b, s, p); double free = p.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
          at(b, p, f0, r - 3, 0);
          GearCombat.trail(b, s, p); double near = p.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
          double want = 1 + GearStats.value1(s.gear, "trailblazer");
          s.gear = only(gear); GearCombat.trail(b, s, p);
          Phase1Checks.report("p1-gear-combat-trailblazer", foesBefore == 0 && close(free / base, want, 1e-6) && close(near, base, 1e-9), "no foe within " + r + ": speed x" + (free / base) + " (want " + want + "); a foe " + (r - 3) + " off: x" + (near / base) + (foesBefore == 0 ? "" : "; " + foesBefore + " foes were near at the start"));
          put(b, f0, p.getX() - 60, p.getZ() + 60, 0); }

        // grudge, last_light, hawkeye, draw_speed and quickened_relic: the factor each moves on the commander's own blows, draws and cooldowns
        { s.gear = only(gear, "grudge"); at(b, p, f0, 3, 0);
          s.grudge = f0.mob.getUuid(); double on = GearCombat.blowFactor(s, p, f0.mob, null); s.grudge = null; double off = GearCombat.blowFactor(s, p, f0.mob, null);
          double want = 1 + GearStats.value1(s.gear, "grudge");
          Phase1Checks.report("p1-gear-combat-grudge", close(on / off, want, 1e-9), "on whoever last hurt his troop x" + (on / off) + " (want " + want + ")");
          s.gear = only(gear, "last_light"); double lowAt = GearStats.value2(s.gear, "last_light");
          p.setHealth((float) (pm * lowAt * 0.5)); double low = GearCombat.blowFactor(s, p, f0.mob, null); p.setHealth(pm); double high = GearCombat.blowFactor(s, p, f0.mob, null);
          want = 1 + GearStats.value1(s.gear, "last_light");
          Phase1Checks.report("p1-gear-combat-last_light", close(low / high, want, 1e-9), "below " + lowAt + " of his health x" + (low / high) + " (want " + want + ")");
          s.gear = only(gear, "hawkeye"); double farAt = GearStats.value2(s.gear, "hawkeye");
          var shot = b.world.getDamageSources().arrow(new net.minecraft.entity.projectile.ArrowEntity(b.world, p, new net.minecraft.item.ItemStack(net.minecraft.item.Items.ARROW), null), p);
          at(b, p, f0, farAt + 2, 0); double far = GearCombat.blowFactor(s, p, f0.mob, shot);
          at(b, p, f0, farAt - 5, 0); double nearF = GearCombat.blowFactor(s, p, f0.mob, shot);
          want = 1 + GearStats.value1(s.gear, "hawkeye");
          Phase1Checks.report("p1-gear-combat-hawkeye", close(far / nearF, want, 1e-9), "a shot from " + (farAt + 2) + " x" + (far / nearF) + " of one from " + (farAt - 5) + " (want " + want + ")");
          s.gear = only(gear, "draw_speed"); int drawn = GearCombat.drawTicks(p, 20); s.gear = only(gear); int plainDraw = GearCombat.drawTicks(p, 20);
          int wantDraw = (int) Math.round(plainDraw * (1 + GearStats.value1(only(gear, "draw_speed"), "draw_speed")));
          Phase1Checks.report("p1-gear-combat-draw_speed", drawn == wantDraw && drawn > plainDraw, "20 ticks of draw read " + drawn + " (want " + wantDraw + "; plain " + plainDraw + ")");
          var r0 = Spec.RELICS[0];
          s.gear = only(gear, "quickened_relic"); int quick = Commander.cooldownTicks(s, r0); s.gear = only(gear); int plainCd = Commander.cooldownTicks(s, r0);
          int wantCd = (int) Math.round(r0.cooldownTicks() * (1 - GearStats.value1(only(gear, "quickened_relic"), "quickened_relic")));
          Phase1Checks.report("p1-gear-combat-quickened_relic", quick == wantCd && plainCd == r0.cooldownTicks(), r0.id() + " cools " + quick + " ticks (want " + wantCd + ", plain " + plainCd + ")");
          put(b, f0, p.getX() - 60, p.getZ() + 60, 0); }

        // second_chance: the first downing blow leaves him on value1, the next downs him
        { s.gear = only(gear, "second_chance"); s.secondChanceUsed = false;
          at(b, p, f0, 3, 0);
          p.setHealth(4); p.timeUntilRegen = 0;
          p.damage(b.world.getDamageSources().mobAttack(f0.mob), 100f);
          float left = p.getHealth(); boolean up = !p.isSpectator() && !b.respawns.containsKey(player); double want = GearStats.value1(s.gear, "second_chance");
          p.timeUntilRegen = 0;
          p.damage(b.world.getDamageSources().mobAttack(f0.mob), 100f);
          boolean downed = b.respawns.containsKey(player);
          if (downed) b.respawns.put(player, b.time + 5);
          Phase1Checks.report("p1-gear-combat-second_chance", up && close(left, want, 1e-3) && downed && s.secondChanceUsed, "the first downing blow left " + left + " (want " + want + ", still up " + up + "); the second downed him " + downed);
          put(b, f0, p.getX() - 60, p.getZ() + 60, 0); }

        // the downing blow is judged after mitigation: a blow the hurt cooldown ignores, or that full Resistance cancels, never downs him
        { s.gear = only(gear); s.secondChanceUsed = true;
          b.respawns.remove(player); p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE); // the block above left him down
          at(b, p, f0, 3, 0);
          p.setHealth(pm); p.timeUntilRegen = 0;
          p.damage(b.world.getDamageSources().mobAttack(f0.mob), 2f); // starts the hurt cooldown, remembering a 2 blow
          p.setHealth(1);
          p.damage(b.world.getDamageSources().mobAttack(f0.mob), 2f); // inside the cooldown and no bigger: vanilla ignores it
          boolean cooldownUp = !p.isSpectator() && !b.respawns.containsKey(player) && p.getHealth() == 1;
          p.timeUntilRegen = 0;
          p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.RESISTANCE, 40, 4));
          p.damage(b.world.getDamageSources().mobAttack(f0.mob), 100f); // Resistance V takes it all
          boolean resistUp = !p.isSpectator() && !b.respawns.containsKey(player);
          p.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.RESISTANCE);
          p.timeUntilRegen = 0;
          p.damage(b.world.getDamageSources().mobAttack(f0.mob), 100f);
          boolean downed = b.respawns.containsKey(player) && p.getHealth() == p.getMaxHealth();
          if (b.respawns.containsKey(player)) b.respawns.put(player, b.time + 5);
          Phase1Checks.report("p1-gear-combat-mitigated", cooldownUp && resistUp && downed, "a blow inside the hurt cooldown left him up " + cooldownUp + "; one Resistance V cancels left him up " + resistUp + "; an unmitigated lethal blow downed him at full health " + downed);
          put(b, f0, p.getX() - 60, p.getZ() + 60, 0); }

        s.gear = gear;
        p.setHealth(pm);
    }

    /** A relic id whose tuning effect is this one (the sheet's relics and relicTuning share ids). */
    static String relicWith(String effect) { for (var t : Spec.RELICTUNING) if (t.effect().equals(effect)) return t.id(); return ""; }
}
