package gg.gravesandglory;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Siege on Ironhold (Phase 4 Task 5). The round shuts the west gate and takes the map's flags down, saving what stood
 * under the gate so teardown and the crash marker put it back. Three stages fall in order (spec section 5): the Outer
 * Yard (attackers hold its ring uncontested), the Iron Gate (a structure target with HP; at 0 the open gate returns as
 * the breach) and the Keep (a ring; taking it wins for the attackers). The yard and the gate each add time when they
 * fall; the clock running out wins for the defenders. Until Task 4's pieces (gate_intact, ward) exist, a portcullis of
 * iron bars shuts the gate from the round's start and there are no wards. The stage values are the spec's draft values,
 * kept here until the modeSites and modeRules rows land.
 */
public final class SiegeMode extends BattleMode {
    /** The map piece the round shuts: the open west gate of Ironhold. */
    public static final String GATE = "ironhold/gate";
    /** The tag its saved box is filed under. */
    public static final String GATE_TAG = "siege-gate";
    /** The passage the portcullis closes: seven blocks thick (x), eleven wide (z) and twelve high over the gate floor. */
    static final int PASSAGE_X = 3, PASSAGE_Z = 5, PASSAGE_HEIGHT = 12;
    /** Stage rings and the gate's health: the spec's draft values (section 5). */
    static final double YARD_X = -55, YARD_Z = 0, YARD_RADIUS = 15, KEEP_X = -8, KEEP_Z = 34, KEEP_RADIUS = 10;
    static final int YARD_SECONDS = 20, KEEP_SECONDS = 30, STAGE_BONUS = 180;
    static final float GATE_HP = 1600;

    /** The round's stage (0 yard, 1 gate, 2 keep, 3 taken) and the current ring's hold seconds. A mode object lives for one round. */
    int stage;
    int held;
    /** The gate's target while stage 1 runs. */
    GateTarget target;

    SiegeMode(Spec.ModesRow row) {
        super(row);
    }

    /** The gate's target: a structure of the defenders (a vanilla armour stand like the Forward Supply banner, never saved) that only attackers hurt. */
    public static final class GateTarget extends GravesAndGlory.ArmySupplyBanner {
        public GateTarget(World world) {
            super(world);
        }

        @Override
        Text label() {
            return Text.literal("Iron Gate " + (int) Math.ceil(hp())).formatted(Formatting.LIGHT_PURPLE);
        }
    }

    /** Attackers take the yard, the gate and the keep; defenders hold them. */
    @Override
    public String howToSide(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        return s.team == 0 ? "attack" : "defend";
    }

    /** The gate's landmark row on Ironhold, or null when the map has no gate. */
    static Spec.LandmarksRow gate() {
        for (var r : Spec.LANDMARKS) if (r.map() == 1 && r.template().equals(GATE)) return r;
        return null;
    }

    /**
     * Saves the open gate, shuts it with a portcullis and takes the flags down. Runs after startRound raised the
     * neutral flags, so the round opens with no flags on the field.
     */
    @Override
    public void setup(GravesAndGlory.Battle b) {
        for (var p : b.points) Arena.unflag(b.world, p.spec);
        var row = gate();
        if (row == null) return;
        // A gate snapshot an earlier teardown could not restore still holds the open gate: keep it and shut again, never re-save over it.
        if (b.saves.stream().noneMatch(s -> s.tag().equals(GATE_TAG))) {
            var saved = Structures.placeSaving(b.world, GATE, row.x(), row.elevation(), row.z(), row.rotation(), GATE_TAG);
            if (saved == null) return; // no snapshot, no shut gate: a closed gate that cannot be recovered is worse than an open one
            b.saves.add(saved);
        }
        shut(b.world, row);
    }

    /** No Domination points in a siege: the flags are down, so nothing is captured and no point is a forward anchor. */
    @Override
    public boolean capturesPoints(GravesAndGlory.Battle b) {
        return false;
    }

    /** Stages taken, then the current stage's strength in percent. */
    @Override
    public int[] ledger(GravesAndGlory.Battle b) {
        double progress = stage == 0 ? held / (double) YARD_SECONDS : stage == 2 ? held / (double) KEEP_SECONDS : 0;
        float hp = target == null ? 0 : Math.max(0, target.hp());
        return new int[]{stage, ModeMath.siegeStrength(stage, progress, hp, GATE_HP)};
    }

    @Override
    public int[] max(GravesAndGlory.Battle b) {
        return new int[]{row.winTarget(), 100};
    }

    @Override
    public String[] formats() {
        return new String[]{"fraction", "percent"};
    }

    /** Defenders deploy at the yard while it stands; attackers deploy there once it has fallen. Never with an enemy in the ring. */
    @Override
    public List<GravesAndGlory.Anchor> anchors(GravesAndGlory.Battle b, int team) {
        boolean ours = team == 1 ? stage == 0 : stage >= 1;
        if (!ours || Morale.enemyWithin(b, team, new Vec3d(YARD_X, 65, YARD_Z), YARD_RADIUS)) return List.of();
        return List.of(new GravesAndGlory.Anchor(YARD_X, YARD_Z, "the Outer Yard", team));
    }

    /** Bots go to the current stage: both sides to the yard, then attackers to the gate's face and defenders behind it, then the keep. */
    @Override
    public Vec3d botTarget(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        var row = gate();
        double x, z;
        if (stage == 0) {
            x = YARD_X;
            z = YARD_Z;
        } else if (stage == 1 && row != null) {
            x = row.x() + (s.team == 0 ? -PASSAGE_X - 4 : PASSAGE_X + 4);
            z = row.z();
        } else {
            x = KEEP_X;
            z = KEEP_Z;
        }
        return new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z);
    }

    /** Each second: the current stage's hold or the gate's health, and the win when the keep falls. */
    @Override
    public void tick(GravesAndGlory.Battle b) {
        if (b.time % 20 != 0 || stage >= 3) return;
        if (stage == 0) {
            if (hold(b, YARD_X, YARD_Z, YARD_RADIUS, YARD_SECONDS)) fall(b);
        } else if (stage == 1) {
            if (target == null || !target.isAlive() || target.isRemoved()) breach(b);
        } else if (hold(b, KEEP_X, KEEP_Z, KEEP_RADIUS, KEEP_SECONDS)) {
            stage = 3;
            b.scores[0] = stage;
            feed(b, Spec.FACTIONS[0].name() + " took the Keep");
            b.campaign.finish(0);
            b.world.playSound(null, BlockPos.ORIGIN, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 2, 1);
        }
    }

    /** One second of a ring: attackers alone add a second, defenders alone take one back, contested holds. Returns whether it fell. */
    boolean hold(GravesAndGlory.Battle b, double x, double z, double r, int seconds) {
        int[] in = {0, 0};
        Vec3d c = new Vec3d(x, 65, z);
        for (var u : b.units)
            if (u.mob.isAlive() && GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(), c) < r * r) in[u.squad.team]++;
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) {
            var s = b.squads.get(p.getUuid());
            if (s != null && s.seated && !p.isSpectator() && GravesAndGlory.Battle.horizontalDistance(p.getPos(), c) < r * r) in[s.team]++;
        }
        if (in[0] > 0 && in[1] == 0) held++;
        else if (in[1] > 0 && in[0] == 0) held = Math.max(0, held - 1);
        return held >= seconds;
    }

    /** The yard falls: time is added and the gate's target goes up on the portcullis's west face. */
    void fall(GravesAndGlory.Battle b) {
        stage = 1;
        held = 0;
        b.scores[0] = stage;
        b.roundSeconds += STAGE_BONUS;
        feed(b, Spec.FACTIONS[0].name() + " took the Outer Yard");
        var row = gate();
        GravesAndGlory.Squad defender = null;
        for (var s : b.squads.values()) if (s.seated && s.team == 1) { defender = s; break; }
        if (row == null || defender == null || b.saves.stream().noneMatch(s -> s.tag().equals(GATE_TAG))) { breach(b); return; } // no shut gate to break
        var e = new GateTarget(b.world);
        e.squad = defender;
        e.hp = GATE_HP;
        e.refreshPositionAndAngles(row.x() - PASSAGE_X - 1 + .5, row.elevation() + 1, row.z() + .5, 90, 0);
        e.setNoGravity(true);
        e.setInvisible(true);
        e.addCommandTag("gravesandglory");
        e.addCommandTag("structure");
        e.setCustomName(e.label());
        e.setCustomNameVisible(true);
        b.world.spawnEntity(e);
        b.structures.add(e);
        target = e;
    }

    /** The gate falls: the open gate returns as the breach, time is added and the keep can be taken. */
    void breach(GravesAndGlory.Battle b) {
        for (int i = b.saves.size() - 1; i >= 0; i--)
            if (b.saves.get(i).tag().equals(GATE_TAG) && Structures.restore(b.world, b.saves.get(i))) b.saves.remove(i);
        if (target != null && !target.isRemoved()) target.discard();
        target = null;
        stage = 2;
        held = 0;
        b.scores[0] = stage;
        b.roundSeconds += STAGE_BONUS;
        feed(b, "The Iron Gate is breached");
        mark(b); // the marker stops listing the gate's snapshot once it is back
    }

    /** A stage line on every player's feed, in Dawn's colour. */
    static void feed(GravesAndGlory.Battle b, String text) {
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) FeedEvents.send(p, "point", 0, text);
    }

    /** The portcullis: every air cell of the passage becomes iron bars, so the arch keeps its stone. */
    static void shut(ServerWorld w, Spec.LandmarksRow row) {
        for (int x = row.x() - PASSAGE_X; x <= row.x() + PASSAGE_X; x++)
            for (int z = row.z() - PASSAGE_Z; z <= row.z() + PASSAGE_Z; z++)
                for (int y = row.elevation() + 1; y <= row.elevation() + PASSAGE_HEIGHT; y++) {
                    var pos = new BlockPos(x, y, z);
                    if (w.getBlockState(pos).isAir())
                        w.setBlockState(pos, Blocks.IRON_BARS.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
    }

    /** After teardown put the gate back: removes the gate target and raises the neutral flags; idempotent, as a round may end twice. */
    @Override
    protected void undo(GravesAndGlory.Battle b) {
        if (target != null && !target.isRemoved()) target.discard();
        target = null;
        for (var p : b.points) Arena.flag(b.world, p.spec, -1);
    }
}
