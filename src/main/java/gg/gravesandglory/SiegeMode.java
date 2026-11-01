package gg.gravesandglory;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Siege on Ironhold (Phase 4 Task 5), first slice. The round shuts the west gate and takes the map's flags down,
 * saving what stood under the gate so teardown and the crash marker put it back. The three stages, the gate target,
 * the wards and the bot plans land in the next pass, once Task 4's pieces (gate_intact, ward) exist.
 */
public final class SiegeMode extends BattleMode {
    /** The map piece the round shuts: the open west gate of Ironhold. */
    public static final String GATE = "ironhold/gate";
    /** The tag its saved box is filed under. */
    public static final String GATE_TAG = "siege-gate";
    /** The passage the portcullis closes: seven blocks thick (x), eleven wide (z) and twelve high over the gate floor. */
    static final int PASSAGE_X = 3, PASSAGE_Z = 5, PASSAGE_HEIGHT = 12;

    SiegeMode(Spec.ModesRow row) {
        super(row);
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

    /** After teardown put the gate back, raises the neutral flags; idempotent, as a round may end twice. */
    @Override
    protected void undo(GravesAndGlory.Battle b) {
        for (var p : b.points) Arena.flag(b.world, p.spec, -1);
    }
}
