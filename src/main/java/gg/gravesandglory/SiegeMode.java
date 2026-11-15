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
        return site(1, kind, stage);
    }
    /** The modeSites siege row of a map of this kind and stage; null when the map has none. */
    static Spec.ModeSitesRow find(int map, String kind, int stage) {
        for (var s : Spec.MODESITES) if (s.mode().equals(Modes.SIEGE) && s.map() == map && s.kind().equals(kind) && s.stage() == stage) return s;
        return null;
    }
    static Spec.ModeSitesRow site(int map, String kind, int stage) {
        var s = find(map, kind, stage);
        if (s == null) throw new IllegalStateException("no modeSites siege " + kind + " row for map " + map + " stage " + stage);
        return s;
    }
    /** The ward rows of Ironhold, in sheet order. */
    static List<Spec.ModeSitesRow> wards() {
        return rows(1, "ward");
    }
    /** The siege rows of a map of one kind, in sheet order. */
    static List<Spec.ModeSitesRow> rows(int map, String kind) {
        var rows = new java.util.ArrayList<Spec.ModeSitesRow>();
        for (var s : Spec.MODESITES) if (s.mode().equals(Modes.SIEGE) && s.map() == map && s.kind().equals(kind)) rows.add(s);
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

    /**
     * The numbers of the map a round is on, read from its modeSites siege rows. The old Ironhold's gate is its landmark piece (ironhold/gate, shut by
     * ironhold/gate_intact between the ward walls); a bundled engine map's gate is the gate row's passage, closed with its plug rows (a filled box of blocks per
     * passage: the gate and the postern doors beside it), saved and put back like a piece. Map 1's numbers are the constants above, unchanged.
     */
    static final class Sites {
        final int map;
        final Spec.ModeSitesRow yard, gate, keep;
        final double yardX, yardZ, yardRadius, keepX, keepZ, keepRadius, gateX, gateZ, donjonX, donjonZ, donjonY;
        final int yardSeconds, keepSeconds, floor, pass, passZ, passHeight;
        final float gateHp;
        /** The attacking and the defending team, and the way the attackers go through the gate (a unit step along x or z). Ironhold: Dawn attacks, eastward. */
        final int att, def, ux, uz;
        /** The attackers' forward camp while the yard stands (a base row of their team), or null: their own camp. */
        final Spec.ModeSitesRow camp;
        /** The gate's landmark (map 1), or null on an engine map. */
        final Spec.LandmarksRow landmark;
        final List<Spec.ModeSitesRow> wards, plugs, posts;

        private Sites(int map) {
            this.map = map;
            yard = site(map, "stage", 0);
            gate = site(map, "gate", 1);
            keep = site(map, "stage", 2);
            yardX = yard.x(); yardZ = yard.z(); yardRadius = yard.radius(); yardSeconds = yard.seconds();
            keepX = keep.x(); keepZ = keep.z(); keepRadius = keep.radius(); keepSeconds = keep.seconds();
            gateHp = (float) gate.hp();
            Spec.LandmarksRow lm = null;
            for (var r : Spec.LANDMARKS) if (r.map() == map && r.template().equals(GATE)) lm = r;
            landmark = lm;
            wards = rows(map, "ward");
            plugs = rows(map, "plug");
            posts = rows(map, "post");
            // The defenders are the team of the base row nearest the keep (their keep muster; a base row's stage is its team); the other team's base row, if the
            // map has one, is the attackers' forward camp. The gate row's rotation is the way in: 0 east, 90 south, 180 west, 270 north.
            Spec.ModeSitesRow near = null;
            if (lm == null) for (var r : rows(map, "base")) if (near == null || Math.hypot(r.x() - keepX, r.z() - keepZ) < Math.hypot(near.x() - keepX, near.z() - keepZ)) near = r;
            def = near != null && near.stage() == 0 ? 0 : 1;
            att = 1 - def;
            camp = lm == null ? find(map, "base", att) : null;
            int turn = lm != null ? 0 : Math.floorMod(gate.rotation(), 360);
            ux = turn == 0 ? 1 : turn == 180 ? -1 : 0;
            uz = turn == 90 ? 1 : turn == 270 ? -1 : 0;
            if (lm != null) { // the old Ironhold: the gate piece's passage and the donjon, as constants
                gateX = lm.x(); gateZ = lm.z(); floor = lm.elevation();
                pass = PASSAGE_X; passZ = PASSAGE_Z; passHeight = PASSAGE_HEIGHT;
                donjonX = DONJON_X; donjonZ = DONJON_Z; donjonY = 65;
            } else { // an engine map: the gate row's own plug sets the passage it shuts
                gateX = gate.x(); gateZ = gate.z(); floor = (int) gate.y();
                Spec.ModeSitesRow own = null;
                for (var p : plugs) if (p.x() == gate.x() && p.z() == gate.z()) own = p;
                pass = own == null ? PASSAGE_X : own.seconds() / 2 + 1;
                passZ = own == null ? PASSAGE_Z : (int) own.radius();
                passHeight = own == null ? PASSAGE_HEIGHT : (int) own.hp();
                var base = find(map, "base", def); // the defenders' field base: the old donjon's place
                donjonX = base != null ? base.x() : gate.x() + 40 * ux;
                donjonZ = base != null ? base.z() : gate.z() + 40 * uz;
                donjonY = base != null ? base.y() + 1 : floor + 1;
            }
        }

        private static final java.util.Map<Integer, Sites> CACHE = new java.util.concurrent.ConcurrentHashMap<>();
        /** The sites of a map; a map without siege rows answers Ironhold's. */
        static Sites of(int map) {
            return CACHE.computeIfAbsent(find(map, "gate", 1) != null ? map : 1, Sites::new);
        }
        /** The attackers' base once the gate is breached: two blocks inside the gate's far face (Ironhold's MOUTH constants are this at pass 3). */
        Vec3d mouth() {
            return landmark != null ? at(MOUTH_X, MOUTH_Z) : off(pass + 2.5, .5);
        }
        /** A spot on the gate's line: along blocks past the gate's centre the way the attackers go (negative: on their side), across blocks to the side. */
        Vec3d off(double along, double across) {
            return at(gateX + .5 + ux * (along - .5) + Math.abs(uz) * (across - .5), gateZ + .5 + uz * (along - .5) + Math.abs(ux) * (across - .5)); // mirrored about the gate block's centre, so a west or north gate's spots sit as far off its faces as an east gate's
        }
        /** The yaw of a troop that looks the way the attackers go (with false: back at them). */
        float yaw(boolean attacking) {
            int dx = attacking ? ux : -ux, dz = attacking ? uz : -uz;
            return dx > 0 ? -90 : dx < 0 ? 90 : dz > 0 ? 0 : 180;
        }
        /** The gate's and the yard's names in a line: the rows' own on an engine map. */
        String gateName() { return landmark != null ? "Iron Gate" : gate.name(); }
        String yardName() { return landmark != null ? "the Outer Yard" : yard.name(); }
        /** Whether the round has a gate to shut: the landmark piece, or a plug row. */
        boolean closable() {
            return landmark != null || !plugs.isEmpty();
        }
        /** The gate-sight stand of a defenders' ranged troop: the post row of that side on an engine map, else the old gate tower's top. */
        Vec3d post(int side) {
            for (var p : posts) if (p.stage() == side) return new Vec3d(p.x(), p.y() + 1, p.z());
            return landmark == null ? null : SiegeMode.post(landmark, side);
        }
    }
    /** The sites of the map the round is on, which a round's first call resolves (a mode object lives for one round). */
    Sites sites = Sites.of(1);
    Sites s(GravesAndGlory.Battle b) {
        if (b != null && sites.map != b.campaign.map) sites = Sites.of(b.campaign.map);
        return sites;
    }

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
    /** Whether the felling blow already sounded the breach (GateTarget.structureCue), so breach() does not sound it a second time. */
    boolean breachCued;

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
            return Text.literal((mode != null ? mode.sites.gateName() : "Iron Gate") + " " + (int) Math.ceil(hp())).formatted(Formatting.LIGHT_PURPLE);
        }

        /**
         * A blow on the gate rings as the gate cue, not as a banner's wool, when the blow lands (a melee swing's contact frame, Audio.blowDelay). The blow that
         * fells it sounds the breach instead, at that same moment, and SiegeMode.breach (which runs on the next whole second) then stays quiet.
         */
        @Override
        void structureCue(net.minecraft.server.world.ServerWorld w, boolean felled) {
            var b = GravesAndGlory.battle;
            int wait = Audio.blowDelay;
            Vec3d at = getPos();
            if (felled) {
                if (mode != null) mode.breachCued = true;
                Audio.later(b, wait, () -> Audio.all(b, AudioCues.Cue.GATE_BREACH, "gate"));
            } else Audio.later(b, wait, () -> Audio.at(b, AudioCues.Cue.GATE_HIT, at, "gate"));
        }
    }

    /** Attackers take the yard, the gate and the keep; defenders hold them. */
    @Override
    public String howToSide(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        return s == null || s.team == s(b).att ? "attack" : "defend"; // the lobby's preview has no viewer squad (Campaign.snapshot)
    }

    /** The gate's landmark row on Ironhold, or null when the map has no gate. */
    /** The tag a plug's saved box is filed under; breach() restores every box tagged this way. */
    static final String PLUG_TAG = "siege-plug-";
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
        breachCued = false;
        for (var p : b.points) Arena.unflag(b.world, p.spec);
        var st = s(b);
        var row = st.landmark;
        if (row == null && !st.closable()) return;
        // A gate snapshot an earlier teardown could not restore still holds the open gate: keep it and shut again, never re-save over it.
        // An engine map's plugs save their boxes when they close (drop), so setup has nothing of the gate to save there.
        if (row != null && b.saves.stream().noneMatch(s -> s.tag().equals(GATE_TAG))) {
            var saved = Structures.placeSaving(b.world, GATE, row.x(), row.elevation(), row.z(), row.rotation(), GATE_TAG);
            if (saved == null) return; // no snapshot, no shut gate: a closed gate that cannot be recovered is worse than an open one
            b.saves.add(saved);
        }
        for (var ward : st.wards) {
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

    /** How far from the gate's centre counts as at the breach (the gate row has no ring of its own). */
    static final double GATE_OBJECTIVE_RADIUS = 8;

    /** The keep, or the site of the stage being fought: the yard, the gate's breach, then the keep. Nothing once the keep has fallen. */
    @Override
    public boolean onObjective(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Vec3d pos) {
        if (stage >= 3) return false;
        var st = s(b);
        if (GravesAndGlory.Battle.horizontalDistance(pos, new Vec3d(st.keepX, 65, st.keepZ)) < st.keepRadius * st.keepRadius) return true;
        return stage == 0 ? GravesAndGlory.Battle.horizontalDistance(pos, new Vec3d(st.yardX, 65, st.yardZ)) < st.yardRadius * st.yardRadius
            : stage == 1 && GravesAndGlory.Battle.horizontalDistance(pos, new Vec3d(st.gate.x(), 65, st.gate.z())) < GATE_OBJECTIVE_RADIUS * GATE_OBJECTIVE_RADIUS;
    }

    /** Stages taken, then the current stage's strength in percent. */
    @Override
    public int[] ledger(GravesAndGlory.Battle b) {
        var st = s(b);
        double progress = stage == 0 ? held / (double) st.yardSeconds : stage == 2 ? held / (double) st.keepSeconds : 0;
        float hp = target == null ? (dropAt >= 0 ? st.gateHp : 0) : Math.max(0, target.hp()); // the gate is whole until the portcullis drops
        return new int[]{stage, ModeMath.siegeStrength(stage, progress, hp, st.gateHp)};
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
        var st = s(b);
        boolean ours = team == st.def ? stage == 0 : stage >= 1;
        if (!ours || Morale.enemyWithin(b, team, new Vec3d(st.yardX, 65, st.yardZ), st.yardRadius)) return List.of();
        return List.of(new GravesAndGlory.Anchor(st.yardX, st.yardZ, st.yardName(), team));
    }

    /**
     * Bots go to the current stage (spec 5): both sides to the yard; then attackers to the gate's face and defenders behind the
     * passage (to the breach once the gate is down to BREACH_SHARE); then the keep. While the portcullis is pending the defenders
     * fall back through the gate and the attackers wait clear of the passage, so nobody is shut in on the wrong side.
     */
    @Override
    public Vec3d botTarget(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        var st = s(b);
        if (stage == 0) return at(st.yardX, st.yardZ);
        if (stage == 1 && st.closable()) {
            if (s.team == st.att) return st.off(-st.pass - (dropAt >= 0 ? 10 : 4), 0);
            boolean low = target != null && target.hp() <= st.gateHp * BREACH_SHARE;
            return st.off(st.pass + (low ? .5 : 2.5), .5);
        }
        return at(st.keepX, st.keepZ);
    }

    /** Spec 5: defenders respawn at the donjon; attackers at their base, then the yard once it falls, then the gate's east mouth once it is breached. */
    @Override
    public Vec3d fieldBase(GravesAndGlory.Battle b, int team) {
        var st = s(b);
        if (team == st.def) return at(st.donjonX, st.donjonZ);
        if (stage == 0) return st.camp != null ? at(st.camp.x(), st.camp.z()) : super.fieldBase(b, team);
        return stage == 1 ? at(st.yardX, st.yardZ) : st.mouth();
    }

    /** The defenders' barracks is the donjon. */
    @Override
    public Vec3d fieldBarracks(GravesAndGlory.Battle b, int team) {
        var st = s(b);
        return team == st.def ? new Vec3d(st.donjonX, st.donjonY, st.donjonZ) : super.fieldBarracks(b, team);
    }

    /** The defenders' lineup forms in the yard while it stands; the attackers' at their base. */
    @Override
    public Vec3d lineup(GravesAndGlory.Battle b, int team) {
        var st = s(b);
        return team == st.def && stage == 0 ? at(st.yardX + YARD_LINEUP * st.ux, st.yardZ + YARD_LINEUP * st.uz) : super.lineup(b, team);
    }

    /** The defenders' lineup faces out through the gate, at the attackers; the attackers' faces in through it once the yard is theirs (at their own camp: as on any field). */
    @Override
    public double[] lineupFacing(GravesAndGlory.Battle b, int team) {
        var st = s(b);
        if (st.landmark != null || team == st.att && stage == 0) return super.lineupFacing(b, team);
        return team == st.def ? new double[]{-st.ux, -st.uz} : new double[]{st.ux, st.uz};
    }

    /** A spot on the ground. */
    static Vec3d at(double x, double z) {
        return new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z);
    }

    /** The gate-sight stand on each gate tower, in blocks from the gate piece's centre: TOWER_X west of it, TOWER_Z north or south, feet TOWER_FEET above its elevation. TOWER_Z 8 keeps the perch slots (three across, a block apart) off the stair hole at 9 to 11. */
    static final int TOWER_X = 2, TOWER_Z = 8, TOWER_FEET = 13;

    /**
     * A defenders' ranged post: the top of the north (side 0) or south gate tower. Each tower has an east door and a spiral stair from it to the gate-sight stand
     * (ironhold/gate and gate_intact, 2026-11-01), so the bots walk up; the 2026-11 sims had ladders, which bots never climbed, and posted them on the ground behind the gate.
     */
    static Vec3d post(Spec.LandmarksRow row, int side) {
        return new Vec3d(row.x() - TOWER_X + .5, row.elevation() + TOWER_FEET, row.z() + .5 + (side == 0 ? -TOWER_Z : TOWER_Z));
    }

    /**
     * The stage plan by group (spec 5). Yard and keep: the defenders fight for their ring on Attack (Hold's ten-block leash let the attackers' ranged troops shoot them from outside it in the 2026-11 sims); the attackers keep their plain order. Gate, defenders: ranged troops at a post behind the gate, on Attack so they shoot as far as their reach rather than
     * Hold's ten blocks (Normal and Hard; Easy keeps them with the army). Gate, attackers: infantry and cavalry strike the gate itself, siege troops stand STANDOFF off its face
     * and shoot it (Easy sends them in with the infantry) and ranged troops shoot from RANGED_STANDOFF off, at what they see.
     * While the portcullis is pending the attackers hold where they wait.     */
    @Override
    public void botPlan(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.BotCommandRow tier) {
        var st = s(b);
        if (!st.closable() || stage >= 3) return;
        if (s.team == st.def && stage != 1) {
            s.order = "attack";
            return;
        }
        if (stage != 1) return;
        if (s.team == st.def) {
            Vec3d stand = st.post(Math.floorMod(s.owner.hashCode(), 2));
            if (stand != null && stations(tier)) put(s, "ranged", "attack", stand, -1).perch = true;
            return;
        }
        if (dropAt >= 0 || target == null) {
            s.order = "hold";
            return;
        }
        int gateId = target.getId();
        double face = -st.pass - 1;
        put(s, "infantry", "attack", s.target, gateId);
        put(s, "cavalry", "attack", s.target, gateId);
        if (stations(tier)) put(s, "siege", "hold", st.off(face - STANDOFF, .5), gateId);
        else put(s, "siege", "attack", s.target, gateId);
        put(s, "ranged", "attack", st.off(face - RANGED_STANDOFF, .5), -1);
    }

    /** Once the yard has fallen the defenders' base is the donjon behind the gate they hold: they fight on there rather than fall back and wait to rally (the 2026-11 sims had them sit out the gate and keep). */
    @Override
    public boolean botRetreats(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        return s.team != s(b).def || stage == 0;
    }

    /** One group's order, keeping the formation the army already gave it (a shieldwall stays braced). */
    static GravesAndGlory.Assignment put(GravesAndGlory.Squad s, String group, String order, Vec3d spot, int enemy) {
        var was = s.assignments.get(group);
        var a = new GravesAndGlory.Assignment(order, was != null ? was.formation : s.formation, spot, s.yaw);
        a.enemy = enemy;
        s.assignments.put(group, a);
        return a;
    }

    /** Each second: the current stage's hold or the gate's health, and the win when the keep falls. */
    @Override
    public void tick(GravesAndGlory.Battle b) {
        if (b.time % 20 != 0 || stage >= 3) return;
        var st = s(b);
        if (stage == 0) {
            if (hold(b, st.yardX, st.yardZ, st.yardRadius, st.yardSeconds, false)) fall(b);
        } else if (stage == 1) {
            if (dropAt >= 0) { if (b.time >= dropAt) drop(b); }
            else if (target == null || !target.isAlive() || target.isRemoved()) breach(b);
        } else if (hold(b, st.keepX, st.keepZ, st.keepRadius, st.keepSeconds, true)) {
            stage = 3;
            keepSecond = second(b);
            staged(b, "keep", keepSecond);
            b.scores[st.att] = stage;
            feed(b, "keep_taken", "team", attackers(b));
            b.campaign.finish(st.att); // the victory and defeat stings play from Campaign.finish
        }
    }

    /**
     * One second of a ring: attackers alone add a second, defenders alone take one back, contested holds. With the edge
     * (the Keep), attackers also add a second while contested if they are at least keepEdgeAttackers per keepEdgeDefenders (two thirds) of the defenders number.
     * Returns whether it fell.
     */
    boolean hold(GravesAndGlory.Battle b, double x, double z, double r, int seconds, boolean edge) {
        int[] in = {0, 0};
        int a = s(b).att, d = 1 - a;
        Vec3d c = new Vec3d(x, 65, z);
        for (var u : b.units)
            if (u.mob.isAlive() && GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(), c) < r * r) in[u.squad.team]++;
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) {
            var s = b.squads.get(p.getUuid());
            if (s != null && s.seated && !p.isSpectator() && GravesAndGlory.Battle.horizontalDistance(p.getPos(), c) < r * r) in[s.team]++;
        }
        if (in[a] > 0 && (in[d] == 0 || edge && in[a] * R.keepEdgeDefenders() >= in[d] * R.keepEdgeAttackers())) held++;
        else if (in[d] > 0 && in[a] == 0) held = Math.max(0, held - 1);
        return held >= seconds;
    }

    /** The yard falls: time is added, the portcullis drops and the gate's target goes up on its west face. */
    void fall(GravesAndGlory.Battle b) {
        stage = 1;
        yardSecond = second(b);
        staged(b, "yard", yardSecond);
        held = 0;
        var st = s(b);
        b.scores[st.att] = stage;
        b.roundSeconds += STAGE_BONUS;
        feed(b, "yard_taken", "team", attackers(b));
        Audio.captured(b, "yard", st.att);
        GravesAndGlory.Squad defender = null;
        for (var s : b.squads.values()) if (s.seated && s.team == st.def) { defender = s; break; }
        if (!st.closable() || defender == null || st.landmark != null && b.saves.stream().noneMatch(s -> s.tag().equals(GATE_TAG))) { breach(b); return; } // no saved gate to shut and break
        dropAt = b.time + R.portcullisSeconds() * 20L;
        // The gate's crash is 2.3 s into its sound, so the cue starts that much ahead of the drop and the two land together.
        Audio.later(b, (int) (dropAt - b.time) - AudioCues.GATE_SHUT_LEAD_TICKS, () -> Audio.all(b, AudioCues.Cue.GATE_SHUT, "gate"));
        feed(b, "gate_closing", "n", Integer.toString(R.portcullisSeconds()));
    }

    /** The portcullis drops: units in the passage go to the nearer side, the intact gate replaces the open one and its target goes up on its west face. */
    void drop(GravesAndGlory.Battle b) {
        dropAt = -1;
        var st = s(b);
        var row = st.landmark;
        GravesAndGlory.Squad defender = null;
        for (var s : b.squads.values()) if (s.seated && s.team == st.def) { defender = s; break; }
        if (!st.closable() || defender == null) { breach(b); return; }
        if (row != null) {
            clearPassage(b, st.gateX, st.gateZ, st.floor, st.pass, st.passZ, st.passHeight);
            Structures.place(b.world, GATE_INTACT, row.x(), row.elevation(), row.z(), row.rotation());
        } else if (!plug(b, st)) { breach(b); return; } // a plug that could not be saved shuts nothing: the gate is down at once rather than closed for good
        feed(b, "gate_shut"); // the shut cue itself started GATE_SHUT_LEAD_TICKS ago (fall)
        var e = new GateTarget(b.world);
        e.squad = defender;
        e.mode = this;
        e.hp = st.gateHp;
        e.refreshPositionAndAngles(st.gateX - st.ux * (st.pass + 1) + .5, st.floor + 1, st.gateZ - st.uz * (st.pass + 1) + .5, st.yaw(false), 0);
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
        clearPassage(b, row.x(), row.z(), row.elevation(), PASSAGE_X, PASSAGE_Z, PASSAGE_HEIGHT);
    }

    /** The same for a passage centred at (x, z) on a floor: pass blocks each side of the centre are the shut part, passZ half its width, height its height. */
    static void clearPassage(GravesAndGlory.Battle b, double x, double z, int floor, int pass, int passZ, int height) {
        clearPassage(b, x, z, floor, pass, passZ, height, false);
    }
    /** The same for a passage that runs along z (turned): pass is then its half thickness in z and passZ its half width in x. */
    static void clearPassage(GravesAndGlory.Battle b, double x, double z, int floor, int pass, int passZ, int height, boolean turned) {
        int px = turned ? passZ : pass, pz = turned ? pass : passZ;
        var box = new net.minecraft.util.math.Box(x - px - 1, floor - 1, z - pz - 1, x + px + 2, floor + height + 2, z + pz + 2);
        for (var e : b.world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class, box, v -> true)) {
            if (turned) e.requestTeleport(e.getX(), floor + 1, e.getZ() < z ? z - pass - 3 : z + pass + 3);
            else e.requestTeleport(e.getX() < x ? x - pass - 3 : x + pass + 3, floor + 1, e.getZ());
        }
    }
    /** Whether a plug's passage runs along z (its row's rotation 90 or 270): its thickness is then in z and its width in x. */
    static boolean turned(Spec.ModeSitesRow p) {
        return Math.floorMod(p.rotation(), 180) == 90;
    }
    /** A plug's box: corner and size (thickness seconds along its passage, radius each side of the centre across it, hp high). */
    static int[] plugBox(Spec.ModeSitesRow p) {
        int thick = p.seconds(), half = (int) p.radius(), wide = half * 2 + 1;
        return turned(p) ? new int[]{(int) p.x() - half, (int) p.y() + 1, (int) p.z() - thick / 2, wide, (int) p.hp(), thick}
            : new int[]{(int) p.x() - thick / 2, (int) p.y() + 1, (int) p.z() - half, thick, (int) p.hp(), wide};
    }

    /**
     * Shuts an engine map's passages (its plug rows): everything standing in a plug's box goes to the nearer side, each box is saved (Structures.snapshot, filed
     * under PLUG_TAG and listed in the crash marker) and filled with the row's block. False, with nothing shut, when a box cannot be saved.
     */
    boolean plug(GravesAndGlory.Battle b, Sites st) {
        var states = new java.util.ArrayList<net.minecraft.block.BlockState>();
        for (var p : st.plugs) { // every plug's block first: a bad id shuts nothing (preflight checks the ids too)
            var id = net.minecraft.util.Identifier.tryParse(p.piece());
            if (id == null || !net.minecraft.registry.Registries.BLOCK.containsId(id)) { System.err.println("[Graves and Glory] siege plug " + p.id() + ": " + p.piece() + " is not a block; nothing shut"); return false; }
            states.add(net.minecraft.registry.Registries.BLOCK.get(id).getDefaultState());
        }
        var saved = new java.util.ArrayList<Structures.Saved>();
        for (var p : st.plugs) {
            int[] box = plugBox(p);
            var snap = Structures.snapshot(b.world, new net.minecraft.util.math.BlockPos(box[0], box[1], box[2]), new net.minecraft.util.math.Vec3i(box[3], box[4], box[5]), PLUG_TAG + p.id().substring(p.id().lastIndexOf('_') + 1));
            if (snap == null) { for (var s : saved) Structures.restore(b.world, s); return false; }
            saved.add(snap);
        }
        b.saves.addAll(saved); // saved and in the crash marker before a block changes
        mark(b);
        for (var p : st.plugs) clearPassage(b, p.x() + .5, p.z() + .5, (int) p.y(), p.seconds() / 2 + 1, (int) p.radius(), (int) p.hp(), turned(p));
        for (int i = 0; i < st.plugs.size(); i++) {
            var p = st.plugs.get(i);
            var block = states.get(i);
            var o = saved.get(i).origin();
            int[] box = plugBox(p);
            for (int x = 0; x < box[3]; x++) for (int y = 0; y < box[4]; y++) for (int z = 0; z < box[5]; z++) { // the passage only: a wall or floor block inside the box stays
                var at = o.add(x, y, z);
                if (!b.world.getBlockState(at).isFullCube(b.world, at)) b.world.setBlockState(at, block);
            }
            for (int x = 0; x < box[3]; x++) for (int y = 0; y < box[4]; y++) for (int z = 0; z < box[5]; z++) { // bars and panes join their neighbours
                var at = o.add(x, y, z);
                if (b.world.getBlockState(at).isOf(block.getBlock())) b.world.setBlockState(at, net.minecraft.block.Block.postProcessState(b.world.getBlockState(at), b.world, at));
            }
        }
        return true;
    }

    /** The gate falls: the open gate returns as the breach, time is added and the keep can be taken. */
    void breach(GravesAndGlory.Battle b) {
        for (int i = b.saves.size() - 1; i >= 0; i--)
            if ((b.saves.get(i).tag().equals(GATE_TAG) || b.saves.get(i).tag().startsWith(PLUG_TAG)) && Structures.restore(b.world, b.saves.get(i))) b.saves.remove(i);
        if (target != null && !target.isRemoved()) target.discard();
        target = null;
        stage = 2;
        gateSecond = second(b);
        staged(b, "gate", gateSecond);
        held = 0;
        b.scores[s(b).att] = stage;
        b.roundSeconds += STAGE_BONUS;
        feed(b, "gate_breached");
        if (!breachCued) Audio.all(b, AudioCues.Cue.GATE_BREACH, "gate"); // a felled gate sounded it with the blow (GateTarget.structureCue)
        breachCued = false;
        mark(b); // the marker stops listing the gate's snapshot once it is back
    }

    /** A stage line (the modeLines siege row of this event, draft copy) on every player's feed, in the attackers' colour. */
    static void feed(GravesAndGlory.Battle b, String event, String... fields) {
        FeedEvents.mode(b, attacking(b), Modes.SIEGE, event, fields);
    }

    /** The attackers' short name for a feed line: "Gold" for the Gold Banner. */
    static String attackers(GravesAndGlory.Battle b) {
        return FeedLines.teamShort(b.side(attacking(b)).name());
    }
    /** The attacking team of the round's map. */
    static int attacking(GravesAndGlory.Battle b) {
        return b.mode instanceof SiegeMode m ? m.s(b).att : 0;
    }
    /** The clock running out is the defenders' win. */
    @Override
    public int timeoutWinner(GravesAndGlory.Battle b) {
        return s(b).def;
    }
    /** The way a bot's army faces: at the enemy's side of the gate's line. */
    float yaw(GravesAndGlory.Battle b, int team) {
        var st = s(b);
        return st.yaw(team == st.att);
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
    /** A stage fell: a March stage_by bonus hears it (the commander's side attacks on the campaign's map). */
    static void staged(GravesAndGlory.Battle b, String stage, int second) {
        if (b.campaign.bonusTracker != null) b.campaign.bonusTracker.staged(stage, second);
    }

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
