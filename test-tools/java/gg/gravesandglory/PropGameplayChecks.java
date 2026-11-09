package gg.gravesandglory;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.*;

/**
 * Gameplay props (stake barricade, Kort 2026-11-09). GRAVES_PHASE1_CHECKS=prop-gameplay. In a practice battle on a platform above the map: heavy
 * hits (a slam splash, a battle player's full swing of his weapon at its barrier; a second swing inside the cooldown does not count) wear a
 * barricade down and the third removes it with its barriers, parts and anchor gone and the floor and the anchor's cell put back; siege fire takes
 * two; an explosion counts as siege fire. A ranged troop shot at a target standing behind it deals 75% of the same shot in the open, a target on the
 * shooter's side has no cover, and a siege troop's shot into it is a siege hit. Then a Siege round on Ironhold: a defender places up to 4 within the
 * window, the fifth, an attacker and a spot far from the yard are refused, and the round's teardown puts every box back. Never shipped.
 */
final class PropGameplayChecks implements Phase1Checks.Suite {
    static final String ID = PropGameplay.SIEGE_PROP;
    volatile UUID player;
    volatile boolean ready, built, swung, covered, siegeReady, placedSiege, tornDown;
    volatile int ox, oy, oz;
    int stage, at;
    final Map<BlockPos, BlockState> before = new HashMap<>();
    final List<BlockPos> siegeAnchors = new ArrayList<>();

    static void say(String line) { System.out.println("[p1] prop-gameplay: " + line); }
    void next(int tick, int s) { stage = s; at = tick; }
    static void report(String id, boolean pass, String detail) { Phase1Checks.report("p1-propgame-" + id, pass, detail); }

    static void cmd(GravesAndGlory.Battle b, ServerPlayerEntity p, String line) {
        var src = b.server.getCommandSource().withWorld(p.getServerWorld()).withSilent().withLevel(4);
        b.server.getCommandManager().executeWithPrefix(src, line);
    }
    ServerPlayerEntity me(GravesAndGlory.Battle b) { return b.server.getPlayerManager().getPlayer(player); }
    BlockPos cell(int i) { return new BlockPos(ox + i * 8, oy, oz); }

    /** Whether the prop at anchor cell a is wholly gone: no anchor, no barrier on its cells, no parts, the floor under it grass and the anchor's buried cell dirt again. */
    static String gone(ServerWorld w, BlockPos a, BlockPos base) {
        if (Props.readAnchor(w, a) != null) return "anchor still there";
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) if (w.getBlockState(a.add(dx, dy, 0)).isOf(Blocks.BARRIER)) return "barrier left at " + a.add(dx, dy, 0).toShortString();
        if (!Props.parts(w, a).isEmpty()) return Props.parts(w, a).size() + " parts left";
        if (!w.getBlockState(a.down()).isOf(Blocks.GRASS_BLOCK)) return "floor " + w.getBlockState(a.down()).getBlock();
        if (base != null && !w.getBlockState(base).isOf(Blocks.DIRT)) return "anchor cell " + base.toShortString() + " is " + w.getBlockState(base).getBlock();
        return "";
    }

    void build(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        ox = (int) Math.floor(p.getX()) - 20; oz = (int) Math.floor(p.getZ()); oy = Math.min(300, (int) Math.floor(p.getY()) + 45);
        int x1 = ox - 10, x2 = ox + 60, z1 = oz - 16, z2 = oz + 10;
        cmd(b, p, "gamerule doDaylightCycle false"); cmd(b, p, "time set 6000");
        cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:air", x1, oy, z1, x2, oy + 10, z2));
        cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x1, oy - 1, z1, x2, oy - 1, z2));
        cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:dirt", x1, oy - 4, z1, x2, oy - 2, z2));
        var w = p.getServerWorld();
        String why = Props.place(w, ID, cell(0), 0);
        var g = Props.DEFS.get(ID).gameplay();
        report("def", g != null && g.hp() == 6 && g.heavyHit() == 2 && g.siegeHit() == 3 && g.cover() == .25 && g.siegeCap() == 4, "gameplay " + g);
        report("place", why.isEmpty(), why.isEmpty() ? "placed at " + cell(0).toShortString() : why);
        int n = PropGameplay.slam(w, Vec3d.ofCenter(cell(0)), 2);
        report("slam-hit", n == 1 && PropGameplay.damage(w, cell(0)) == 2 && Props.readAnchor(w, cell(0)) != null, "slam struck " + n + ", damage " + PropGameplay.damage(w, cell(0)) + " of 6, still standing");
        // the swing: the commander's own battle weapon (GearCombat keeps it in slot 0 and sets its attack speed), the player south of it looking at its lower middle barrier
        aim(p);
        built = true;
        say("platform at " + ox + " " + oy + " " + oz);
    }
    void aim(ServerPlayerEntity p) {
        BlockPos a = cell(0);
        double ex = a.getX() + .5, ez = a.getZ() + 2.6, ey = oy + 1.62;
        float pitch = (float) Math.toDegrees(Math.atan2(ey - (oy + .5), ez - (a.getZ() + .5)));
        p.teleport(p.getServerWorld(), ex, oy, ez, 180f, pitch);
    }

    void swing(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        var w = p.getServerWorld(); BlockPos a = cell(0); BlockPos base = Props.anchorBase(w, a);
        aim(p);
        var hr = p.raycast(PropGameplay.SWING_REACH, 1f, false);
        String why = "cooldown " + p.getAttackCooldownProgress(.5f) + ", attack " + p.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE) + ", held " + p.getMainHandStack().getItem()
            + ", mode " + p.interactionManager.getGameMode() + ", at " + p.getPos() + " yaw " + p.getYaw() + " pitch " + p.getPitch() + ", ray " + hr.getType() + (hr instanceof net.minecraft.util.hit.BlockHitResult bh ? " " + bh.getBlockPos().toShortString() + " " + w.getBlockState(bh.getBlockPos()).getBlock() : "");
        boolean heavy = PropGameplay.swing(p);
        report("swing-heavy", heavy && PropGameplay.damage(w, a) == 4, "full swing of the battle weapon at the barrier: hit " + heavy + ", damage " + PropGameplay.damage(w, a) + "; " + why);
        boolean again = PropGameplay.swing(p);
        report("swing-cooldown", !again && PropGameplay.damage(w, a) == 4, "a second swing in the same tick does not count: " + again);
        int n = PropGameplay.slam(w, Vec3d.ofCenter(a), 2);
        String left = gone(w, a, base);
        report("break", n == 1 && left.isEmpty() && PropGameplay.damage(w, a) == 0, left.isEmpty() ? "third heavy hit removed it: barriers, parts, anchor gone, floor and anchor cell restored" : left);
        // siege fire: two shells
        BlockPos s = cell(1); Props.place(w, ID, s, 0); BlockPos sBase = Props.anchorBase(w, s);
        PropGameplay.siegeFire(w, Vec3d.ofCenter(s), 3, "test shell");
        boolean standing = Props.readAnchor(w, s) != null && PropGameplay.damage(w, s) == 3;
        PropGameplay.siegeFire(w, Vec3d.ofCenter(s), 3, "test shell");
        String sLeft = gone(w, s, sBase);
        report("siege-fire", standing && sLeft.isEmpty(), "first shell leaves it at 3 of 6 (" + standing + "), second: " + (sLeft.isEmpty() ? "removed, floor restored" : sLeft));
        // an explosion counts as siege fire (no block damage)
        BlockPos e = cell(2); Props.place(w, ID, e, 0);
        b.running = false; // outside a battle an explosion leaves props alone
        try { w.createExplosion(null, e.getX() + .5, oy + 1, e.getZ() + 2.5, 3f, World.ExplosionSourceType.NONE); } finally { b.running = true; }
        report("explosion-no-battle", PropGameplay.damage(w, e) == 0, "an explosion with no battle running: damage " + PropGameplay.damage(w, e));
        w.createExplosion(null, e.getX() + .5, oy + 1, e.getZ() + 2.5, 3f, World.ExplosionSourceType.NONE);
        report("explosion", PropGameplay.damage(w, e) == 3, "an explosion 2 blocks off: damage " + PropGameplay.damage(w, e));
        Props.remove(w, e);
        swung = true;
    }

    void cover(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        var w = p.getServerWorld(); var own = b.squads.get(p.getUuid());
        GravesAndGlory.Squad foe = null; for (var s : b.squads.values()) if (s.team != own.team) { foe = s; break; }
        if (foe == null) { report("cover", false, "no enemy squad"); covered = true; return; }
        BlockPos a = cell(4); Props.place(w, ID, a, 0); // points north, toward the shooter
        Vec3d shooterAt = new Vec3d(a.getX() + .5, oy, a.getZ() - 9.5);
        var archer = still(b.spawn(own, Campaign.troop("archer"), 70, shooterAt));
        Vec3d behind = new Vec3d(a.getX() + .5, oy, a.getZ() + 1.5), front = new Vec3d(a.getX() + .5, oy, a.getZ() - 1.5);
        var t1 = still(b.spawn(foe, Campaign.troop("knight"), 71, behind));
        var t2 = still(b.spawn(foe, Campaign.troop("knight"), 72, front));
        boolean hasCover = PropGameplay.cover(w, archer.mob.getPos(), t1.mob) != null, frontCover = PropGameplay.cover(w, archer.mob.getPos(), t2.mob) != null;
        float coveredHit = Combat.resolve(b, archer, t1.mob, 10f, "arrow", false);
        report("cover-front", hasCover && !frontCover, "behind it: cover " + hasCover + "; on the shooter's side: cover " + frontCover);
        // a siege troop's shot into it is siege fire
        var ballista = still(b.spawn(own, Campaign.troop("ic_ballista"), 73, shooterAt.add(1, 0, 0)));
        var t3 = still(b.spawn(foe, Campaign.troop("knight"), 74, behind.add(.2, 0, 0)));
        Combat.resolve(b, ballista, t3.mob, 10f, "arrow", false);
        report("cover-siege-hit", PropGameplay.damage(w, a) == 3, "a ballista shot at a covered troop: barricade damage " + PropGameplay.damage(w, a));
        Props.remove(w, a);
        var t4 = still(b.spawn(foe, Campaign.troop("knight"), 75, behind));
        float openHit = Combat.resolve(b, archer, t4.mob, 10f, "arrow", false);
        double ratio = openHit > 0 ? coveredHit / openHit : -1;
        report("cover", openHit > 0 && Math.abs(ratio - .75) < .01, String.format("the same arrow behind the barricade %.2f, in the open %.2f, ratio %.3f (want 0.75)", coveredHit, openHit, ratio));
        for (var u : List.of(archer, ballista, t1, t2, t3, t4)) u.mob.discard();
        covered = true;
    }
    static GravesAndGlory.Unit still(GravesAndGlory.Unit u) {
        Phase1Checks.freeze(u); u.mob.setAiDisabled(true); u.mob.setNoGravity(true);
        float yaw = 0; u.mob.setYaw(yaw); u.mob.setHeadYaw(yaw); u.mob.setBodyYaw(yaw);
        return u;
    }

    /** Siege on Ironhold: a defender (the bot of team 1) places up to the cap near the yard; the teardown puts every box back. */
    void siege(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        var w = b.world; var own = b.squads.get(p.getUuid());
        GravesAndGlory.Squad def = null; for (var s : b.squads.values()) if (s.seated && s.team == 1) { def = s; break; }
        report("siege-start", b.mode instanceof SiegeMode && def != null, "mode " + b.mode.id() + ", defender " + (def != null));
        if (def == null) { placedSiege = true; return; }
        report("siege-attacker", !PropGameplay.siegeRefusal(b, own, ID, null).isEmpty() || own.team == 1, "attacker: " + PropGameplay.siegeRefusal(b, own, ID, null));
        int yx = (int) Math.floor(SiegeMode.YARD_X), yz = (int) Math.floor(SiegeMode.YARD_Z);
        BlockPos far = new BlockPos(yx + 200, Arena.ground(yx + 200, yz) + 1, yz);
        String farWhy = PropGameplay.siegePlace(b, def, ID, far, 0);
        report("siege-far", farWhy.contains("Outer Yard"), "200 blocks from the yard: " + farWhy);
        int placed = 0; String fifth = null; List<String> refusals = new ArrayList<>();
        outer:
        for (int r = 0; r <= 30; r += 3) for (int k = -30; k <= 30; k += 4) {
            int x = yx + k, z = yz + r - 15;
            BlockPos at = new BlockPos(x, Arena.ground(x, z) + 1, z);
            if (!Props.check(w, ID, at, 0).isEmpty()) continue;
            Map<BlockPos, BlockState> box = new HashMap<>();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -Props.MAX_DEPTH; dy <= 1; dy++) box.put(at.add(dx, dy, 0), w.getBlockState(at.add(dx, dy, 0)));
            String why = PropGameplay.siegePlace(b, def, ID, at, 0);
            if (placed < 4) { if (why.isEmpty()) { placed++; siegeAnchors.add(at); before.putAll(box); } else refusals.add(why); }
            else { fifth = why; break outer; }
        }
        report("siege-cap", placed == 4 && fifth != null && fifth.contains("all 4"), "placed " + placed + " (refusals " + refusals + "), fifth: " + fifth);
        report("siege-saved", b.saves.stream().filter(s -> s.tag().startsWith("siege-prop-")).count() == placed, b.saves.size() + " saved pieces");
        placedSiege = true;
    }
    void teardown(GravesAndGlory.Battle b) {
        var w = b.world; int wrong = 0; String first = "";
        for (BlockPos a : siegeAnchors) if (Props.readAnchor(w, a) != null || !Props.parts(w, a).isEmpty()) { wrong++; first = "prop at " + a.toShortString() + " still there"; }
        for (var e : before.entrySet()) if (!w.getBlockState(e.getKey()).equals(e.getValue())) { wrong++; if (first.isEmpty()) first = e.getKey().toShortString() + " is " + w.getBlockState(e.getKey()) + ", was " + e.getValue(); }
        report("siege-teardown", !siegeAnchors.isEmpty() && wrong == 0 && b.saves.stream().noneMatch(s -> s.tag().startsWith("siege-prop-")), wrong == 0 ? siegeAnchors.size() + " barricades gone, " + before.size() + " cells as before" : wrong + " wrong: " + first);
        tornDown = true;
    }

    @Override public void cleanup(MinecraftClient c) {
        if (c.getServer() != null) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); b.campaign.battleMode = Modes.DOMINATION; });
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 40000 && stage < 9) { report("timeout", false, "stuck at stage " + stage); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); next(tick, 1); } }
            case 1 -> {
                if (!ready) {
                    Phase1Checks.server(c, b -> { var p = me(b); if (!ready && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) ready = true; });
                    if (k > 1200) { report("start", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                if (k == 100) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) build(b, p); });
                if (k == 160 && built) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) swing(b, p); });
                if (k == 170 && swung) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) cover(b, p); });
                if (k == 180 && covered) next(tick, 2);
                if (k > 400) { report("field", false, "built " + built + ", swung " + swung + ", covered " + covered); next(tick, 2); }
            }
            case 2 -> {
                if (!siegeReady) {
                    Phase1Checks.server(c, b -> { var p = me(b); if (siegeReady || p == null) return; b.campaign.battleMode = Modes.SIEGE; if (Phase1Checks.skirmish(b, p, 1, 1, "normal")) siegeReady = true; });
                    if (k > 12000) { report("siege-start", false, "the Siege round never started"); next(tick, 9); }
                    return;
                }
                if (k == 200) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) siege(b, p); });
                if (k == 220 && placedSiege) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
                if (k == 260) Phase1Checks.server(c, this::teardown);
                if (k == 280) next(tick, 9);
            }
            case 9 -> {
                if (k == 1) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); b.campaign.battleMode = Modes.DOMINATION; });
                if (k > 40) { Phase1Checks.finish(c, "prop-gameplay"); stage = 99; }
            }
            default -> {}
        }
    }
}
