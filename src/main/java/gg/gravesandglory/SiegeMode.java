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
 * Siege on Ironhold (Phase 4 Task 5). The round takes the map's flags down and saves what stood under the west gate, so
 * teardown and the crash marker put it back; the gate stays open while the yard is fought over and shuts when it falls. Three stages fall in order (spec section 5): the Outer
 * Yard (attackers hold its ring uncontested), the Iron Gate (a structure target with HP; at 0 the open gate returns as
 * the breach) and the Keep (a ring; taking it wins for the attackers). The yard and the gate each add time when they
 * fall; the clock running out wins for the defenders. When the yard falls the gate stays open for portcullisSeconds, then
 * ironhold/gate_intact replaces it (units in the passage are moved to the nearer side first). Two ward walls close the gaps
 * beside the gate from setup to teardown and take no damage. The Keep is small and the attackers hold the capture edge there
 * (Kort, 2026-10-31: hard but takeable, a good push breaks it in one to two minutes). Every number is a sheet row: the
 * modeRules siege row and the modeSites siege rows (yard, gate, keep, wards).
 */
public final class SiegeMode extends BattleMode {
    /** The map piece the round shuts: the open west gate of Ironhold. */
    public static final String GATE = "ironhold/gate";
    /** The tag its saved box is filed under. */
    public static final String GATE_TAG = "siege-gate";
    /** The piece that replaces the open gate once the portcullis drops. */
    public static final String GATE_INTACT = "ironhold/gate_intact";
    /** The passage the portcullis closes: seven blocks thick (x), eleven wide (z) and twelve high over the gate floor. */
    static final int PASSAGE_X = 3, PASSAGE_Z = 5, PASSAGE_HEIGHT = 12;

    /** The mode numbers: the modeRules siege row; preflight requires it. */
    static Spec.ModeRulesRow rules() {
        for (var r : Spec.MODERULES) if (r.id().equals(Modes.SIEGE)) return r;
        throw new IllegalStateException("no modeRules siege row");
    }
    static final Spec.ModeRulesRow R = rules();

    /** The modeSites siege row of Ironhold of this kind and stage; preflight requires the stage and gate rows. */
    static Spec.ModeSitesRow site(String kind, int stage) {
        for (var s : Spec.MODESITES) if (s.mode().equals(Modes.SIEGE) && s.map() == 1 && s.kind().equals(kind) && s.stage() == stage) return s;
        throw new IllegalStateException("no modeSites siege " + kind + " row for stage " + stage);
    }
    /** The ward rows of Ironhold, in sheet order. */
    static List<Spec.ModeSitesRow> wards() {
        var rows = new java.util.ArrayList<Spec.ModeSitesRow>();
        for (var s : Spec.MODESITES) if (s.mode().equals(Modes.SIEGE) && s.map() == 1 && s.kind().equals("ward")) rows.add(s);
        return rows;
    }
    /** The tag a ward saved box is filed under. */
    static String wardTag(Spec.ModeSitesRow ward) {
        return "siege-ward-" + ward.id().substring(ward.id().lastIndexOf('_') + 1);
    }
    static final Spec.ModeSitesRow YARD = site("stage", 0), GATE_SITE = site("gate", 1), KEEP = site("stage", 2);
    /** Stage rings and the gate health, from the rows. */
    static final double YARD_X = YARD.x(), YARD_Z = YARD.z(), YARD_RADIUS = YARD.radius(), KEEP_X = KEEP.x(), KEEP_Z = KEEP.z(), KEEP_RADIUS = KEEP.radius();
    static final int YARD_SECONDS = YARD.seconds(), KEEP_SECONDS = KEEP.seconds(), STAGE_BONUS = R.siegeStageBonus();
    static final float GATE_HP = (float) GATE_SITE.hp();
    /** The donjon, the defenders' field base for respawn, barracks and lineup after the yard (spec 5; the phase 4 plan's donjon cell in the keep's east court), and the gate's east mouth, the attackers' base after the breach. In code until a modeSites base row takes them. */
    static final double DONJON_X = 12, DONJON_Z = 34, MOUTH_X = -31.5, MOUTH_Z = .5;
    /** How far east of the yard's centre the defenders' lineup point stands, so reinforce's rows (5 to 20 forward of it) form across the yard. */
    static final double YARD_LINEUP = 12;

    /** The round's stage (0 yard, 1 gate, 2 keep, 3 taken) and the current ring's hold seconds. A mode object lives for one round. */
    int stage;
    int held;
    /** The gate target while stage 1 runs once the portcullis is down. */
    GateTarget target;
    /** The world time the portcullis drops, or -1 when it is not pending (the gate stays open until then). */
    long dropAt = -1;
    /** What the round did, for the bot sim's report (read-only on the game): blows that hurt the gate and their damage, and the round second each stage fell (-1: never). */
    int gateBlows, yardSecond = -1, gateSecond = -1, keepSecond = -1;
    float gateDamage;

    /**
     * The bot plan's draft values (spec 5's table; in code like Convoy's raidShare until the botCommand sheet takes them): defender infantry
     * move from behind the passage to the breach at this share of the gate's health, attacker siege troops stand this far west of the gate's
     * face, and attacker ranged troops this far.
     */
    static final double BREACH_SHARE = .25, STANDOFF = 25, RANGED_STANDOFF = 12, POST_IN = 8, POST_SIDE = 4;
    /** Whether a tier mans the stations (the gate towers) and stands its siege troops off: Normal and Hard; Easy sends everything in with the infantry. */
    static boolean stations(Spec.BotCommandRow tier) {
        return !tier.id().equals("easy");
    }

    SiegeMode(Spec.ModesRow row) {
        super(row);
    }

    /** The gate's target: a structure of the defenders (a vanilla armour stand like the Forward Supply banner, never saved) that only attackers hurt. */
    public static final class GateTarget extends GravesAndGlory.ArmySupplyBanner {
        /** The round's mode, which counts its blows. */
        SiegeMode mode;

        public GateTarget(World world) {
            super(world);
        }

        @Override
        public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
            float was = hp;
            boolean took = super.damage(source, amount);
            if (took && mode != null) {
                mode.gateBlows++;
                mode.gateDamage += was - Math.max(0, hp);
            }
            return took;
        }

        @Override
        Text label() {
            return Text.literal("Iron Gate " + (int) Math.ceil(hp())).formatted(Formatting.LIGHT_PURPLE);
        }

        /** A blow on the gate rings as the gate cue (its last blow too: the breach cue follows from SiegeMode.breach), not as a banner's wool. */
        @Override
        void structureCue(net.minecraft.server.world.ServerWorld w, boolean felled) {
            Audio.at(GravesAndGlory.battle, AudioCues.Cue.GATE_HIT, getPos(), "gate");
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
     * Saves the open gate (the box the intact gate will replace), places the wards (each with its own saved box) and takes the flags down. Runs after startRound raised the
     * neutral flags, so the round opens with no flags on the field and the gate open until the yard falls.
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
        for (var ward : wards()) {
            String tag = wardTag(ward);
            if (b.saves.stream().anyMatch(s -> s.tag().equals(tag))) { Structures.place(b.world, ward.piece(), (int) ward.x(), (int) ward.y(), (int) ward.z(), ward.rotation()); continue; }
            var saved = Structures.placeSaving(b.world, ward.piece(), (int) ward.x(), (int) ward.y(), (int) ward.z(), ward.rotation(), tag);
            if (saved != null) b.saves.add(saved);
        }
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
        float hp = target == null ? (dropAt >= 0 ? GATE_HP : 0) : Math.max(0, target.hp()); // the gate is whole until the portcullis drops
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

    /**
     * Bots go to the current stage (spec 5): both sides to the yard; then attackers to the gate's face and defenders behind the
     * passage (to the breach once the gate is down to BREACH_SHARE); then the keep. While the portcullis is pending the defenders
     * fall back through the gate and the attackers wait clear of the passage, so nobody is shut in on the wrong side.
     */
    @Override
    public Vec3d botTarget(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        var row = gate();
        if (stage == 0) return at(YARD_X, YARD_Z);
        if (stage == 1 && row != null) {
            if (s.team == 0) return at(row.x() - PASSAGE_X - (dropAt >= 0 ? 10 : 4), row.z());
            boolean low = target != null && target.hp() <= GATE_HP * BREACH_SHARE;
            return at(row.x() + PASSAGE_X + (low ? .5 : 2.5), row.z() + .5);
        }
        return at(KEEP_X, KEEP_Z);
    }

    /** Spec 5: defenders respawn at the donjon; attackers at their base, then the yard once it falls, then the gate's east mouth once it is breached. */
    @Override
    public Vec3d fieldBase(GravesAndGlory.Battle b, int team) {
        if (team == 1) return at(DONJON_X, DONJON_Z);
        if (stage == 0) return super.fieldBase(b, team);
        return stage == 1 ? at(YARD_X, YARD_Z) : at(MOUTH_X, MOUTH_Z);
    }

    /** The defenders' barracks is the donjon. */
    @Override
    public Vec3d fieldBarracks(GravesAndGlory.Battle b, int team) {
        return team == 1 ? new Vec3d(DONJON_X, 65, DONJON_Z) : super.fieldBarracks(b, team);
    }

    /** The defenders' lineup forms in the yard while it stands; the attackers' at their base. */
    @Override
    public Vec3d lineup(GravesAndGlory.Battle b, int team) {
        return team == 1 && stage == 0 ? at(YARD_X + YARD_LINEUP, YARD_Z) : super.lineup(b, team);
    }

    /** A spot on the ground. */
    static Vec3d at(double x, double z) {
        return new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z);
    }

    /** A defenders' ranged post behind the gate: on the ground POST_IN inside the passage, north (side 0) or south of it. The tower tops were tried first, but bots never climbed to them (the 2026-11 sim runs). */
    static Vec3d post(Spec.LandmarksRow row, int side) {
        return at(row.x() + PASSAGE_X + POST_IN, row.z() + .5 + (side == 0 ? -POST_SIDE : POST_SIDE));
    }

    /**
     * The stage plan by group (spec 5). Yard and keep: the defenders fight for their ring on Attack (Hold's ten-block leash let the attackers' ranged troops shoot them from outside it in the 2026-11 sims); the attackers keep their plain order. Gate, defenders: ranged troops at a post behind the gate, on Attack so they shoot as far as their reach rather than
     * Hold's ten blocks (Normal and Hard; Easy keeps them with the army). Gate, attackers: infantry and cavalry strike the gate itself, siege troops stand STANDOFF off its face
     * and shoot it (Easy sends them in with the infantry) and ranged troops shoot from RANGED_STANDOFF off, at what they see.
     * While the portcullis is pending the attackers hold where they wait.     */
    @Override
    public void botPlan(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.BotCommandRow tier) {
        var row = gate();
        if (row == null || stage >= 3) return;
        if (s.team == 1 && stage != 1) {
            s.order = "attack";
            return;
        }
        if (stage != 1) return;
        if (s.team == 1) {
            if (stations(tier)) put(s, "ranged", "attack", post(row, Math.floorMod(s.owner.hashCode(), 2)), -1);
            return;
        }
        if (dropAt >= 0 || target == null) {
            s.order = "hold";
            return;
        }
        int gateId = target.getId();
        double face = row.x() - PASSAGE_X - 1;
        put(s, "infantry", "attack", s.target, gateId);
        put(s, "cavalry", "attack", s.target, gateId);
        if (stations(tier)) put(s, "siege", "hold", at(face - STANDOFF, row.z() + .5), gateId);
        else put(s, "siege", "attack", s.target, gateId);
        put(s, "ranged", "attack", at(face - RANGED_STANDOFF, row.z() + .5), -1);
    }

    /** Once the yard has fallen the defenders' base is the donjon behind the gate they hold: they fight on there rather than fall back and wait to rally (the 2026-11 sims had them sit out the gate and keep). */
    @Override
    public boolean botRetreats(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        return s.team != 1 || stage == 0;
    }

    /** One group's order, keeping the formation the army already gave it (a shieldwall stays braced). */
    static void put(GravesAndGlory.Squad s, String group, String order, Vec3d spot, int enemy) {
        var was = s.assignments.get(group);
        var a = new GravesAndGlory.Assignment(order, was != null ? was.formation : s.formation, spot, s.yaw);
        a.enemy = enemy;
        s.assignments.put(group, a);
    }

    /** Each second: the current stage's hold or the gate's health, and the win when the keep falls. */
    @Override
    public void tick(GravesAndGlory.Battle b) {
        if (b.time % 20 != 0 || stage >= 3) return;
        if (stage == 0) {
            if (hold(b, YARD_X, YARD_Z, YARD_RADIUS, YARD_SECONDS, false)) fall(b);
        } else if (stage == 1) {
            if (dropAt >= 0) { if (b.time >= dropAt) drop(b); }
            else if (target == null || !target.isAlive() || target.isRemoved()) breach(b);
        } else if (hold(b, KEEP_X, KEEP_Z, KEEP_RADIUS, KEEP_SECONDS, true)) {
            stage = 3;
            keepSecond = second(b);
            b.scores[0] = stage;
            feed(b, "keep_taken", "team", attackers());
            b.campaign.finish(0); // the victory and defeat stings play from Campaign.finish
        }
    }

    /**
     * One second of a ring: attackers alone add a second, defenders alone take one back, contested holds. With the edge
     * (the Keep), attackers also add a second while contested if they are at least keepEdgeAttackers per keepEdgeDefenders (two thirds) of the defenders number.
     * Returns whether it fell.
     */
    boolean hold(GravesAndGlory.Battle b, double x, double z, double r, int seconds, boolean edge) {
        int[] in = {0, 0};
        Vec3d c = new Vec3d(x, 65, z);
        for (var u : b.units)
            if (u.mob.isAlive() && GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(), c) < r * r) in[u.squad.team]++;
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) {
            var s = b.squads.get(p.getUuid());
            if (s != null && s.seated && !p.isSpectator() && GravesAndGlory.Battle.horizontalDistance(p.getPos(), c) < r * r) in[s.team]++;
        }
        if (in[0] > 0 && (in[1] == 0 || edge && in[0] * R.keepEdgeDefenders() >= in[1] * R.keepEdgeAttackers())) held++;
        else if (in[1] > 0 && in[0] == 0) held = Math.max(0, held - 1);
        return held >= seconds;
    }

    /** The yard falls: time is added, the portcullis drops and the gate's target goes up on its west face. */
    void fall(GravesAndGlory.Battle b) {
        stage = 1;
        yardSecond = second(b);
        held = 0;
        b.scores[0] = stage;
        b.roundSeconds += STAGE_BONUS;
        feed(b, "yard_taken", "team", attackers());
        Audio.captured(b, "yard", 0);
        var row = gate();
        GravesAndGlory.Squad defender = null;
        for (var s : b.squads.values()) if (s.seated && s.team == 1) { defender = s; break; }
        if (row == null || defender == null || b.saves.stream().noneMatch(s -> s.tag().equals(GATE_TAG))) { breach(b); return; } // no saved gate to shut and break
        dropAt = b.time + R.portcullisSeconds() * 20L;
        feed(b, "gate_closing", "n", Integer.toString(R.portcullisSeconds()));
    }

    /** The portcullis drops: units in the passage go to the nearer side, the intact gate replaces the open one and its target goes up on its west face. */
    void drop(GravesAndGlory.Battle b) {
        dropAt = -1;
        var row = gate();
        GravesAndGlory.Squad defender = null;
        for (var s : b.squads.values()) if (s.seated && s.team == 1) { defender = s; break; }
        if (row == null || defender == null) { breach(b); return; }
        clearPassage(b, row);
        Structures.place(b.world, GATE_INTACT, row.x(), row.elevation(), row.z(), row.rotation());
        feed(b, "gate_shut");
        Audio.all(b, AudioCues.Cue.GATE_SHUT, "gate");
        var e = new GateTarget(b.world);
        e.squad = defender;
        e.mode = this;
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

    /** Moves everything standing in the gate passage to the side of it that is nearer (by x), just clear of the grilles. */
    static void clearPassage(GravesAndGlory.Battle b, Spec.LandmarksRow row) {
        double lo = row.x() - PASSAGE_X - 1, hi = row.x() + PASSAGE_X + 2;
        var box = new net.minecraft.util.math.Box(lo, row.elevation() - 1, row.z() - PASSAGE_Z - 1, hi, row.elevation() + PASSAGE_HEIGHT + 2, row.z() + PASSAGE_Z + 2);
        for (var e : b.world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class, box, x -> true)) {
            double side = e.getX() < row.x() ? row.x() - PASSAGE_X - 3 : row.x() + PASSAGE_X + 3;
            e.requestTeleport(side, row.elevation() + 1, e.getZ());
        }
    }

    /** The gate falls: the open gate returns as the breach, time is added and the keep can be taken. */
    void breach(GravesAndGlory.Battle b) {
        for (int i = b.saves.size() - 1; i >= 0; i--)
            if (b.saves.get(i).tag().equals(GATE_TAG) && Structures.restore(b.world, b.saves.get(i))) b.saves.remove(i);
        if (target != null && !target.isRemoved()) target.discard();
        target = null;
        stage = 2;
        gateSecond = second(b);
        held = 0;
        b.scores[0] = stage;
        b.roundSeconds += STAGE_BONUS;
        feed(b, "gate_breached");
        Audio.all(b, AudioCues.Cue.GATE_BREACH, "gate");
        mark(b); // the marker stops listing the gate's snapshot once it is back
    }

    /** A stage line (the modeLines siege row of this event, draft copy) on every player's feed, in the attackers' colour. */
    static void feed(GravesAndGlory.Battle b, String event, String... fields) {
        FeedEvents.mode(b, 0, Modes.SIEGE, event, fields);
    }

    /** The attackers' short name for a feed line: "Gold" for the Gold Banner. */
    static String attackers() {
        return FeedLines.teamShort(Spec.FACTIONS[0].name());
    }

    /** The objective panel's opening line (the modeLines siege start row): the flags are down, so "Capture the objectives." would mislead. */
    @Override
    public String startLine(GravesAndGlory.Battle b) {
        return FeedLines.modeLine(Modes.SIEGE, "start");
    }

    /** The deciding numbers: stages taken of three, the defence's strength in percent, and the clock. */
    @Override
    public List<ModeLines.Entry> ledgerEntries(GravesAndGlory.Battle b) {
        int[] l = ledger(b), m = max(b);
        return List.of(ModeLines.entry("siege_stage", l[0], m[0]), ModeLines.entry("siege_strength", l[1], m[1]), ModeLines.entry("siege_time", clock(b), b.roundSeconds));
    }

    /** The round second now. */
    static int second(GravesAndGlory.Battle b) {
        return (int) ((b.time - b.roundStarted) / 20);
    }

    /** After teardown put the gate back: removes the gate target and raises the neutral flags; idempotent, as a round may end twice. */
    @Override
    protected void undo(GravesAndGlory.Battle b) {
        if (target != null && !target.isRemoved()) target.discard();
        target = null;
        for (var p : b.points) Arena.flag(b.world, p.spec, -1);
    }
}
