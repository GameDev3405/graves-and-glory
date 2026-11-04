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
 * heals the friend it looks at, and a banner widens the command aura. The profile is put back from a JSON copy taken at the start. Never shipped.
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
        stack.onStoppedUsing(b.world, p, stack.getMaxUseTime() - n);
        PersistentProjectileEntity arrow = null;
        for (var e : b.world.iterateEntities()) if (e instanceof PersistentProjectileEntity a && a.getOwner() == p && (arrow == null || a.getId() > arrow.getId())) arrow = a;
        double speed = arrow == null ? 0 : arrow.getVelocity().length();
        boolean pass = arrow != null && Math.abs(speed - want) < 0.03 * want && Math.abs(want - vanilla) > 0.06 * want;
        Phase1Checks.report("p1-gear-combat-bow", pass, fam + ": loosed after " + n + " ticks, flew at " + speed + " (want " + want + " for " + drawn + " ticks; vanilla " + vanilla + ")");
        if (arrow != null) arrow.discard();
    }
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
                if (ok) { ok = false; next(tick, 5); }
            }
            case 5 -> { // p1-gear-combat-sweep, -banner, -staff and -bow: one server tick each, nothing moves between placing and measuring
                if (tick - stageAt == 1) Phase1Checks.server(c, b -> {
                    var p = sp(b, player); var s = b.squads.get(player);
                    sweep(b, p, s); banner(b, p, s); staff(b, p, s); bow(b, p, s);
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
                if (t > 15 && ok) { ok = false; next(tick, 9); }
            }
            case 9 -> {
                if (tick - stageAt == 1) Phase1Checks.server(c, b -> restore());
                if (restored || tick - stageAt > 100) { Phase1Checks.finish(c, "gear-combat"); stage = 99; }
            }
            default -> { }
        }
    }
}
