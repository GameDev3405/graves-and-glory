package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure battle arithmetic shared by the server, the HUD and standalone checks. No Minecraft types. */
public final class BattleMath {
    private BattleMath() {}
    /** Ticks a shot spends in the air: distance over speed, rounded, clamped to [minTicks,maxTicks]. */
    public static int travelTicks(double distance, double blocksPerTick, int minTicks, int maxTicks) {
        long ticks = Math.round(distance / blocksPerTick);
        return (int) Math.max(minTicks, Math.min(maxTicks, ticks));
    }
    /**
     * A volley's peak (animation rework section 5): how far over the straight line from bow to target a shot rises mid-flight, in blocks. It grows
     * with range (perBlock a block, combatDiscipline volleyPeakPerBlock), stops growing at max (volleyPeakMax), and is scaled by the weapon (arcScale).
     */
    public static double arcPeak(double dist, double perBlock, double max, double scale) { return Math.min(max, dist * perBlock) * scale; }
    /** How far over its straight line a shot stands at t of its flight (0 launch, 1 landing): a parabola, 0 at both ends and peak at the middle; flat outside the flight. */
    public static double arcHeight(double t, double peak) {
        double f = Math.max(0, Math.min(1, t));
        return 4 * peak * f * (1 - f);
    }
    /**
     * The launch angle, in degrees above the horizontal (Minecraft's own pitch is its negative), of a shot dist out horizontally and dy up whose
     * arc peaks at peak over ticks of flight: the tangent of the path at launch, the straight line's slope plus the parabola's starting climb
     * (arcHeight's slope at 0); the flight time scales both alike, so it never tilts the angle. A flying shot's launch velocity is the first
     * tick's secant instead (Projectiles.fire, R26), a little flatter than this tangent, so the shot sits on its curve every tick.
     */
    public static double launchPitch(double dist, double dy, double peak, int ticks) {
        double n = Math.max(1, ticks), rise = (dy + 4 * peak) / n, run = dist / n;
        return rise == 0 ? 0 : Math.toDegrees(Math.atan2(rise, run));
    }
    /**
     * How a weapon scales the volley's arc: a bow 1, a crossbow crossbowScale (combatDiscipline crossbowArcScale; a flatter, faster bolt; the
     * Arbalist's and the Ballista Crew's alike, R24), a thrown trident thrownScale (thrownArcScale). The Mortar's crew, casters (whose bolts are
     * particles) and every other family keep their straight flight: 0.
     */
    public static double arcScale(String family, double crossbowScale, double thrownScale) {
        return switch (family) {
            case "bow" -> 1;
            case "crossbow" -> crossbowScale;
            case "thrower" -> thrownScale;
            default -> 0;
        };
    }
    /**
     * A volley's peak under overhead cover (M2): clears[i] is how far the first solid block above the straight line stands over it at ts[i] of the
     * flight (infinite in open sky). The arc keeps margin under every one of them: at t it stands arcHeight(t, 1) of its peak, so each caps the
     * peak at its headroom, clear - margin, over 4 t (1 - t). A shot flies flat (0) if a column that lowers its peak leaves it less than minRoom of
     * headroom there (R-M2), so a lowered arc that still flies has at least minRoom of room under every roof that lowers it; cover a small arc never
     * reaches leaves it be.
     */
    public static double arcCoverPeak(double peak, double[] ts, double[] clears, double margin, double minRoom) {
        double capped = peak, room = Double.POSITIVE_INFINITY;
        for (int i = 0; i < ts.length; i++) {
            double shape = arcHeight(ts[i], 1);
            if (shape <= 0) continue;
            double headroom = Math.max(0, clears[i] - margin);
            if (headroom / shape >= peak) continue;
            room = Math.min(room, headroom);
            capped = Math.min(capped, headroom / shape);
        }
        return capped < peak && room < minRoom ? 0 : capped;
    }
    /** The yaw, in degrees, of an arrow flying (vx, vz) nose first, in the convention vanilla gives a flying arrow: atan2(vx, vz); +z is 0, +x 90 (R25). */
    public static double arrowYaw(double vx, double vz) { return Math.toDegrees(Math.atan2(vx, vz)); }
    /** The pitch, in degrees, of an arrow flying (vx, vy, vz) nose first, as vanilla gives a flying arrow: up while it climbs, down while it falls (R25). */
    public static double arrowPitch(double vx, double vy, double vz) { return Math.toDegrees(Math.atan2(vy, Math.hypot(vx, vz))); }
    /** An arced shot's flight in ticks: travelTicks times scale (combatDiscipline arcTravelScale, 1 to 2), rounded; a shot with no arc keeps travelTicks. At 1, no flight time changes. */
    public static int arcTravelTicks(int travel, double peak, double scale) { return peak > 0 ? (int) Math.round(travel * scale) : travel; }
    /** A bleed's next beat after one landed at now that was due at scheduled: every ticks on, but never sooner than every ticks after this beat,
     *  so beats held back (the hurt cooldown) are not caught up in a burst of one a tick. */
    public static long nextBleedBeat(long scheduled, long now, int every) { return Math.max(scheduled + every, now + every); }
    /** Whether a blow struck at lastHitAt can still credit a death at now: no more than window ticks ago, and not in the future. */
    public static boolean creditWindowOpen(long now, long lastHitAt, int window) {
        long age = now - lastHitAt;
        return age >= 0 && age <= window;
    }
    /** A group took a loss when a member died or its average health fell by at least 0.008 (one 4-damage hit on a full 12-knight group). */
    public static boolean tookLoss(float prevHealth, int prevCount, float health, int count) {
        return count < prevCount || prevHealth - health >= 0.008f - 1e-6f;
    }
    /** A group's fighting strength: living health over the health it started the round with, clamped to [0,1]. */
    public static double strength(double livingHealth, double fullHealth) {
        if (fullHealth <= 0) return 1;
        return Math.max(0, Math.min(1, livingHealth / fullHealth));
    }
    /**
     * The signed bearing of a blow's source from its victim's facing (hit_v1's direction, carry 1 of Task 6): relativeBearing's angle with a side,
     * + to the victim's right, - to its left, 0 straight ahead and 180 straight behind (never -180), in (-180, 180]. Minecraft's forward for a yaw is
     * (-sin yaw, cos yaw), so its right hand is (-cos yaw, -sin yaw): at yaw 0 (facing +z) the right is -x. A source on the victim has no direction: 0.
     */
    public static double signedBearing(double facingYawDeg, double dx, double dz) {
        if (Math.hypot(dx, dz) < 1e-9) return 0;
        double yaw = Math.toRadians(facingYawDeg);
        double ahead = -Math.sin(yaw) * dx + Math.cos(yaw) * dz, right = -Math.cos(yaw) * dx - Math.sin(yaw) * dz;
        double deg = Math.toDegrees(Math.atan2(right, ahead));
        return deg <= -180 + 1e-9 ? 180 : deg;
    }
    /** The zone a projectile strikes (hit_v1, spec 6.4): where its impact height sits on the victim's box from its feet, under 0.45 of the height legs, under 0.8 body, else head. A box with no height is struck in the body. */
    public static String zone(double impactY, double feetY, double height) {
        if (height <= 0) return "body";
        double f = (impactY - feetY) / height;
        return f < 0.45 ? "legs" : f < 0.8 ? "body" : "head";
    }
    /** A blow's weight class from its share of the victim's maximum health (combatDiscipline mediumHitShare and heavyHitShare): 0 light, 1 medium, 2 heavy. No max health reads light. */
    public static int weight(double amount, double maxHealth, double medium, double heavy) {
        if (maxHealth <= 0) return 0;
        double share = amount / maxHealth;
        return share >= heavy ? 2 : share >= medium ? 1 : 0;
    }
    /** The weight class index of an attackVariants weight cell (light, medium, heavy); an unknown cell reads light. */
    public static int weightIndex(String weight) { return switch (weight) {case "heavy" -> 2; case "medium" -> 1; default -> 0;}; }
    /** A weight off the wire clamped to the ordinals: 0 light, 1 medium, 2 heavy (MotionEvents.Client). */
    public static int weightOrdinal(int weight) { return Math.max(0, Math.min(2, weight)); }
    /** The health a blow really took: health and absorption before it less after it, never below 0 (a troop that rose again ends with more) and 0 for NaN. */
    public static double lostHealth(double before, double after) { double lost = before - after; return lost > 0 ? lost : 0; }
    /**
     * A blow's hit_v1 weight (fix round 1, M3) from the health it really took (lostHealth) as a share of the victim's maximum (weight): outside the
     * hurt cooldown the larger of that and its swing's weight (swingWeight, an ordinal: 0 for a blow with no swing); inside it (vanilla deals only the
     * excess over the last blow) the health taken alone, so a heavy swing that took little reads light.
     */
    public static int hitWeight(int swingWeight, boolean cooldown, double lost, double maxHealth, double medium, double heavy) {
        int taken = weight(lost, maxHealth, medium, heavy);
        return cooldown ? taken : Math.max(weightOrdinal(swingWeight), taken);
    }
    /** Whether a swing's index lies inside its family's attackVariants rows (count of them): the client drops any other (MotionEvents.Client). */
    public static boolean validVariant(int variant, int count) { return variant >= 0 && variant < count; }
    /**
     * stance_v2's aimPitch, in MotionInput's sign (positive aims down, as Minecraft's own pitch): launchPitch's negative, 0 for a flat shot (never -0).
     * The wire, the Unit and the client all carry this one sign, so the motion engine (Task 13) reads it as it comes.
     */
    public static double aimPitch(double dist, double dy, double peak, int ticks) { double up = launchPitch(dist, dy, peak, ticks); return up == 0 ? 0 : -up; }
    /**
     * The longest an attack clip may run, in ticks (Task 13 carry B8). The server holds no clip lengths (they live in the client's motion assets), so a
     * late joiner is sent a swing for this long and its client, which knows the clip, skips one whose clip has already ended. tools/clips.py fails any
     * attackVariants clip longer than this, so no swing that still shows is withheld.
     */
    public static final int ATTACK_CLIP_MAX_TICKS = 40;
    /** Whether a swing begun on world tick swingTick (-1: none) may still show at world tick now: from that tick through ATTACK_CLIP_MAX_TICKS. */
    public static boolean swingCurrent(long now, long swingTick) { return swingTick >= 0 && now >= swingTick && now - swingTick <= ATTACK_CLIP_MAX_TICKS; }
    /** The death tick vanilla removes a mob on, and the length of the fall a troop's corpse plays before it lies (LivingEntity.updatePostDeath). */
    public static final int DEATH_FALL_TICKS = 20;
    /** The death tick a troop's corpse is removed on: the vanilla fall, then lie ticks lying whole, then fade ticks fading (CorpseMixin). */
    public static int corpseGoneTick(int lie, int fade) { return DEATH_FALL_TICKS + Math.max(0, lie) + Math.max(0, fade); }
    /** corpseAlpha on a whole death tick: the float overload's curve, so the two can never drift apart (the server-side checks use this one). */
    public static float corpseAlpha(int deathTicks, int lie, int fade) { return corpseAlpha((float) deathTicks, lie, fade); }
    /**
     * How opaque a troop's corpse is deathTicks after it fell, on a partial death tick (the death tick plus the frame's tick delta) so the fade is
     * smooth between ticks: 1 through DEATH_FALL_TICKS + lie, then an eased fade (0.5 + 0.5cos) to 0 at DEATH_FALL_TICKS + lie + fade, and 0 after.
     * With no fade it is 1 up to the removal tick and 0 past it. The client fade (BodyCache.fade) consumes it.
     */
    public static float corpseAlpha(float deathTicks, int lie, int fade) {
        int start = DEATH_FALL_TICKS + Math.max(0, lie);
        if (deathTicks <= start) return 1f;
        if (fade <= 0 || deathTicks >= start + fade) return 0f;
        double t = (deathTicks - start) / (double) fade;
        return (float) (0.5 + 0.5 * Math.cos(Math.PI * t));
    }
    /** Whether a troop shows vanilla's red hurt tint: while hurt or through its fall (deathTime 1..DEATH_FALL_TICKS), never once the body lies. */
    public static boolean corpseHurtTint(int deathTime, int hurtTime) { return deathTime <= DEATH_FALL_TICKS && (hurtTime > 0 || deathTime > 0); }
    /**
     * The squared distance a troop must not pass to draw relief (TroopDepth): of the first n values of a, those strictly below nearSq are in
     * range; with fewer than cap of them (or cap below 1) the answer is nearSq, else the cap-th smallest. It may reorder a.
     */
    public static double reliefCutoffSq(double[] a, int n, int cap, double nearSq) {
        int k = 0;
        for (int i = 0; i < n; i++) if (a[i] < nearSq) a[k++] = a[i];
        if (k < cap || cap < 1) return nearSq;
        Arrays.sort(a, 0, k);
        return a[cap - 1];
    }
    /**
     * The relief cutoff for bodies lying past their fall, which rank after every living troop: the living in range (first nl of living,
     * below nearSq) take their slots first, and the bodies share what is left of cap, nearest first. -1 when nothing is left, so no body
     * is near. It may reorder bodies; living is only read.
     */
    public static double bodyCutoffSq(double[] living, int nl, double[] bodies, int nb, int cap, double nearSq) {
        if (cap < 1) return nearSq;
        int k = 0;
        for (int i = 0; i < nl; i++) if (living[i] < nearSq) k++;
        int slots = cap - Math.min(cap, k);
        return slots <= 0 ? -1 : reliefCutoffSq(bodies, nb, slots, nearSq);
    }
    /**
     * The stances a troop shows its clients, in stance_v2's wire order (the byte is the index): none, hold-ready, brace, the formed shield wall,
     * shield-up moving, fall back, run, ranged lowered and ranged aiming (spec 6.4), then the command UI's rally (weapons raised on an order's
     * acknowledgement), charge (a Charge's run, its own pose since the animation rework: run stays the Retreat's) and stunned (while Unit.stunnedUntil
     * runs; it overrides the order's stance). New stances are appended, never inserted: the index is the wire byte.
     */
    public static final List<String> STANCES = List.of("", "hold", "brace", "wall", "shield_move", "fall_back", "run", "ranged_rest", "ranged_aim", "rally", "charge", "stunned");
    /**
     * An order id as the command UI reads it (OrderRules.loadOrder): the old Move and Hold are both the player's Formation order, hold; the
     * other sheet orders stay; anything unknown or missing is the squad default, follow. Bots, Siege and Muster still issue move internally.
     */
    public static String loadOrder(String order) {
        if (order == null) return "follow";
        if (order.equals("move") || order.equals("hold")) return "hold";
        for (var o : Spec.ORDERS) if (o.id().equals(order)) return order;
        return "follow";
    }
    /** A formation id as the command UI reads it (OrderRules.loadFormation): a sheet shape stays (block is now labelled Shield wall); anything else is line. */
    public static String loadFormation(String formation) {
        if (formation != null) for (var f : Spec.FORMATIONS) if (f.id().equals(formation)) return formation;
        return "line";
    }
    /** The plain-hold rule (carry 2 of Task 6): on Hold a shield or spear troop braces, a shield troop formed in its wall shows the wall, and every other troop holds. */
    public static String holdStance(boolean shield, boolean spear, boolean formed) { return shield && formed ? "wall" : shield || spear ? "brace" : "hold"; }
    /**
     * A troop's stance (stance_v2) from the order it fights under (an arrived Move or Retreat group's is Hold): Hold by the plain-hold rule
     * (holdStance), Fall Back fall_back, Charge charge, Retreat run, any other order none. A ranged troop with an enemy in sight and in reach
     * (aiming) aims on any order but a Retreat's run or Fall Back (so on a Charge it stops to shoot, as Tactics lets it), and with none rests
     * (on Hold it holds, on a Charge it charges); a shield troop on another order walking with its shield up (shieldUp) is shield_move.
     * Stunned is not here: Tactics shows it over this while the troop is stunned.
     */
    public static String stance(String order, boolean shield, boolean spear, boolean ranged, boolean formed, boolean aiming, boolean shieldUp) {
        String base = switch (order) {
            case "hold" -> holdStance(shield, spear, formed);
            case "fall_back" -> "fall_back";
            case "charge" -> "charge";
            case "retreat" -> "run";
            default -> "";
        };
        if (ranged && aiming && !base.equals("run") && !base.equals("fall_back")) return "ranged_aim";
        if (ranged && base.isEmpty()) return "ranged_rest";
        if (shield && shieldUp && base.isEmpty()) return "shield_move";
        return base;
    }
    /** The stance a troop shows: "stunned" while battle time is before its stunnedUntil, else the stance its order gave (stance). */
    public static String shownStance(String orderStance, long time, long stunnedUntil) { return time < stunnedUntil ? "stunned" : orderStance; }
    /** hit_v1's kinds in wire order (the byte is the index): the three melee kinds, the two shot kinds and magic (spec 6.4). */
    public static final List<String> KINDS = List.of("stab", "slash", "blunt", "arrow", "bolt", "magic");
    /** hit_v1's zones in wire order (the byte is the index). */
    public static final List<String> ZONES = List.of("legs", "body", "head");
    /**
     * A troop's blow's hit_v1 kind from Combat.resolve's kind, the attacker's family and its swing's attackVariants kind: a slam is blunt, a
     * caster's bolt is magic, a blight is magic but the crew's shell is blunt (M5), a shot keeps its family's projectile (a crossbow's a bolt), and a
     * melee blow keeps its swing's stab, slash or blunt (any other slashes).
     */
    public static String hitKind(String kind, String family, String swingKind) {
        return switch (kind) {
            case "slam" -> "blunt";
            case "bolt" -> "magic";
            case "blight" -> "crew".equals(family) ? "blunt" : "magic";
            case "arrow" -> "bolt".equals(swingKind) ? "bolt" : "arrow";
            default -> swingKind != null && (swingKind.equals("stab") || swingKind.equals("slash") || swingKind.equals("blunt")) ? swingKind : "slash";
        };
    }
    /**
     * A player's weapon's hit_v1 kind from its item id: a sword or axe slashes, a trident or spear stabs, anything else (a fist, a stick) is blunt.
     * A fist or a stick reading blunt is accepted as is (Task 6 review ruling): no finer kind for an improvised weapon.
     */
    public static String weaponKind(String itemId) {
        String path = itemId.substring(itemId.indexOf(':') + 1);
        if (path.endsWith("sword") || path.endsWith("_axe") || path.equals("axe")) return "slash";
        if (path.contains("trident") || path.contains("spear")) return "stab";
        return "blunt";
    }
    /** Degrees (0..180) between Minecraft's forward vector for a yaw, (-sin yaw, cos yaw), and the offset (dx,dz). */
    public static double relativeBearing(double facingYawDeg, double dx, double dz) {
        double length = Math.hypot(dx, dz);
        if (length < 1e-9) return 0;
        double yaw = Math.toRadians(facingYawDeg);
        double cos = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / length;
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
    }
    /**
     * The attribute multiplier that makes a troop travel x travel ("+x% speed" on a sheet: 1 + x): the modifier value for a MULTIPLY_TOTAL on
     * the movement speed. A swimmer's stroke is linear in the attribute, so in water it is travel - 1 as it stands; a land mob's walk is
     * quadratic in it (the attribute sets the forward input, and the ground turns that input into an acceleration by the attribute again:
     * measured, a +15% attribute walks 1.32 as far), so on land it is the square root of travel, less 1. Riptide, Muster and the Howl share it.
     */
    public static double speedModifier(double travel, boolean swimming) {
        return (swimming ? travel : Math.sqrt(Math.max(0, travel))) - 1;
    }
    /** Whether a troop is wounded for a sheet rule that reads "at or below share of its max health" (the fearless stance, Run them down): health at or below share x max, with a hair of tolerance for float health. */
    public static boolean wounded(double health, double maxHealth, double share) {
        return maxHealth > 0 && health <= share * maxHealth + 1e-6;
    }
    /** The room a body of this width takes in a formation and on a bridge: its width and 0.3, at least 1.3. A formation's cell is the widest such room in its group, at least the formation's own spacing; the server (Battle.formation) and the command preview (CommandMode) both read it. */
    public static double spacing(double width) { return Math.max(1.3, width + 0.3); }
    /** A shield troop's cell on Hold (animation rework §7.2): the shield-wall cell (combatDiscipline.shieldWallCell), or its body's width and 0.1 when that is more, so neighbouring collision boxes keep a gap and never push each other apart. */
    public static double shieldCell(double width, double wallCell) { return Math.max(wallCell, width + 0.1); }
    /** The cell of each unit of a group on Hold: a shield unit its shieldCell, any other the formation's cell or its body's room (spacing) when that is more. */
    public static double[] holdSlots(boolean[] shield, double[] width, double formationCell, double wallCell) {
        double[] cells = new double[shield.length];
        for (int i = 0; i < cells.length; i++) cells[i] = shield[i] ? shieldCell(width[i], wallCell) : Math.max(formationCell, spacing(width[i]));
        return cells;
    }
    /** How many ranks this many places take in a formation's shape: rows of `columns`, or the wedge's ranks of 1, 3, 5... */
    public static int ranks(int count, int columns, boolean wedge) {
        if (count <= 0) return 0;
        return wedge ? (int) Math.floor(Math.sqrt(count - 1)) + 1 : (count - 1) / columns + 1;
    }
    /**
     * A group's places (Battle.formation, and the command preview in CommandMode, which must agree): {side, back} for each unit, in the order
     * given (the server gives slot order), before the group's role depth. `shield` marks the units that close into the wall (shield troops on Hold;
     * all false on any other order). The shield units come first, in the front ranks of the formation's shape, at the widest of their cells; the
     * others take the ranks behind them at the widest of theirs. With no shield unit this is the old layout: one cell, the formation's spacing or
     * the widest body's room. The places depend only on how many units of each kind there are and their cells, not on their order.
     * With a wall (some shield unit, not the wedge) a partial rank, of the wall or of the rest, stands centred on the formation's columns, filled
     * from the middle out (centreOrder): so a rank one man short is the rank with its outermost place gone, and a wall that loses men stays
     * centred within half a cell. With no shield unit the partial rank stands at the left, as it always has.
     */
    public static double[][] holdLayout(boolean[] shield, double[] width, double formationCell, double wallCell, int columns, boolean wedge) {
        double[] cells = holdSlots(shield, width, formationCell, wallCell);
        int shields = 0; double front = 0, rear = formationCell;
        for (int i = 0; i < cells.length; i++) if (shield[i]) { shields++; front = Math.max(front, cells[i]); } else rear = Math.max(rear, cells[i]);
        double depth = ranks(shields, columns, wedge) * front;
        double[][] grid = holdGrid(shield, columns, wedge), places = new double[cells.length][];
        for (int i = 0; i < cells.length; i++) {
            double cell = shield[i] ? front : rear, back = (grid[i][1] + 1) * cell;
            places[i] = new double[]{grid[i][0] * cell, shield[i] ? back : depth + back};
        }
        return places;
    }
    /**
     * holdLayout's places in cells, not blocks: {side, rank} for each unit, side in cells from the middle of the formation, rank from 0 at the
     * front of its kind's ranks (the wall's, or the rest's behind it). They do not depend on the cells, so a wall whose cell changes (its widest
     * man fell) keeps every man's file and rank (Battle.heldPlace hands keepPlaces these).
     */
    public static double[][] holdGrid(boolean[] shield, int columns, boolean wedge) {
        int shields = 0;
        for (boolean wall : shield) if (wall) shields++;
        boolean centre = shields > 0 && !wedge;
        double[][] walls = kindGrid(shields, columns, wedge, centre), rest = kindGrid(shield.length - shields, columns, wedge, centre), grid = new double[shield.length][];
        for (int i = 0, s = 0, o = 0; i < shield.length; i++) grid[i] = shield[i] ? walls[s++] : rest[o++];
        return grid;
    }
    /** The places of one kind (the wall's, or the rest's) of holdGrid, for this many units of it: rows of `columns` from rank 0, the partial rank centred when `centre` (a wall, not the wedge), or the wedge's ranks. */
    static double[][] kindGrid(int count, int columns, boolean wedge, boolean centre) {
        double[][] grid = new double[Math.max(count, 0)][]; int[] order = centreOrder(columns);
        for (int index = 0; index < grid.length; index++) {
            int inRank = Math.min(columns, count - index / columns * columns);
            int column = centre && inRank < columns ? order[index % columns] : index % columns;
            double side = column - (columns - 1) / 2.0; int rank = index / columns;
            if (wedge) { rank = (int) Math.floor(Math.sqrt(index)); side = index - rank * rank - rank; }
            grid[index] = new double[]{side, rank};
        }
        return grid;
    }
    /** The columns of a rank in the order a partial rank fills them: the middle first, then out to either side alternately (the right of a tie first). */
    public static int[] centreOrder(int columns) {
        Integer[] c = new Integer[Math.max(columns, 0)];
        for (int i = 0; i < c.length; i++) c[i] = i;
        Arrays.sort(c, (a, b) -> { int d = Integer.compare(Math.abs(2 * a - (columns - 1)), Math.abs(2 * b - (columns - 1))); return d != 0 ? d : Integer.compare(b, a); });
        int[] out = new int[c.length];
        for (int i = 0; i < c.length; i++) out[i] = c[i];
        return out;
    }
    /**
     * Which of a held group's places each unit takes (Battle.heldPlace): an index into holdGrid(shield, columns, wedge), one place per unit, place
     * j of the kind of unit j. prev[i] is unit i's holdGrid place the last time (null: none, a new unit); a change of cell keeps every file and rank.
     * A wall that loses a man closes the gap as a wall closes ranks: every unit whose place is still in the layout keeps it; a place left empty,
     * front first, takes the unit directly behind it in its file (of its kind), whose own place is then empty in turn; with none behind it, the
     * rank slides one place toward the man who has to come in (the unit whose place is the one the smaller layout drops; with none, the nearest
     * of the rearmost rank): the neighbour on that side steps into the gap, and so on until that man steps into the last. So every man moves at
     * most one place sideways or one rank up, nobody passes anybody, and the men beyond the sliding stretch stand (ruling R17).
     * Several places left empty at once (an area blow, or losses while the group was on another order and kept its cached places) are filled one
     * at a time, each a single loss against the result of the last: the layout the men stood in is found (the smallest that holds every kept
     * place), and while it is larger than the group, the empty place nearest the place the next smaller layout drops is the one that goes, and
     * the rest close up as for one loss. A man moves at most one place per loss. New units then take the places left, as for one loss.
     * When no unit stands on a place of the new layout (a new shape: Battle.heldPlace hands no last places) the units take the places in order.
     */
    public static int[] keepPlaces(double[][] prev, boolean[] shield, int columns, boolean wedge) {
        int n = shield.length, shields = 0; double[][] places = holdGrid(shield, columns, wedge); int[] at = new int[n]; boolean any = false;
        for (boolean wall : shield) if (wall) shields++;
        for (int i = 0; i < n && !any; i++) if (prev[i] != null) for (int j = 0; j < n; j++) if (shield[j] == shield[i] && same(places[j], prev[i])) { any = true; break; }
        if (!any) { for (int i = 0; i < n; i++) at[i] = i; return at; }
        for (boolean kind : new boolean[]{true, false}) {
            List<Integer> mine = new ArrayList<>();
            for (int i = 0; i < n; i++) if (shield[i] == kind) mine.add(i);
            double[][] last = new double[mine.size()][];
            for (int k = 0; k < last.length; k++) last[k] = prev[mine.get(k)];
            int[] got = closeUp(last, columns, wedge, shields > 0 && !wedge);
            for (int k = 0; k < got.length; k++) at[mine.get(k)] = mine.get(got[k]);
        }
        return at;
    }
    /** keepPlaces for the units of one kind: last[k] is unit k's place the last time (null: new); an index into kindGrid(last.length) for each. */
    private static int[] closeUp(double[][] last, int columns, boolean wedge, boolean centre) {
        int m = last.length, known = 0;
        for (double[] p : last) if (p != null) known++;
        double[][] now = kindGrid(m, columns, wedge, centre);
        int old = -1;
        for (int c = Math.max(known, 1); known > 0 && c <= known + 64 && old < 0; c++) if (holds(kindGrid(c, columns, wedge, centre), last)) old = c;
        if (old <= m) return slide(last, now);
        // the layout they stood in: its places nobody stands on are the places left empty
        double[][] stood = kindGrid(old, columns, wedge, centre); List<double[]> empty = new ArrayList<>();
        for (double[] place : stood) { boolean held = false; for (double[] p : last) held |= p != null && same(p, place); if (!held) empty.add(place); }
        double[][] here = last.clone();
        for (int c = old; c > m; c--) {
            double[][] smaller = kindGrid(c - 1, columns, wedge, centre); double[] dropped = null;
            for (double[] place : kindGrid(c, columns, wedge, centre)) { boolean kept = false; for (double[] q : smaller) kept |= same(q, place); if (!kept) dropped = place; }
            // the empty place nearest the dropped one goes (front first, then the left, on a tie); the rest close up as for one loss
            int gone = 0;
            for (int e = 1; e < empty.size(); e++) {
                double[] a = empty.get(e), b = empty.get(gone); double da = Math.hypot(a[0] - dropped[0], a[1] - dropped[1]), db = Math.hypot(b[0] - dropped[0], b[1] - dropped[1]);
                if (da < db - EPS || Math.abs(da - db) <= EPS && (a[1] < b[1] - EPS || Math.abs(a[1] - b[1]) <= EPS && a[0] < b[0])) gone = e;
            }
            empty.remove(gone);
            // the men and the places still empty, as one loss: the empty places close up with the men, so they stay where the slide leaves a gap
            List<Integer> men = new ArrayList<>();
            for (int k = 0; k < m; k++) if (here[k] != null) men.add(k);
            double[][] before = new double[men.size() + empty.size()][];
            for (int k = 0; k < men.size(); k++) before[k] = here[men.get(k)];
            for (int e = 0; e < empty.size(); e++) before[men.size() + e] = empty.get(e);
            int[] after = slide(before, smaller);
            for (int k = 0; k < men.size(); k++) here[men.get(k)] = smaller[after[k]];
            for (int e = 0; e < empty.size(); e++) empty.set(e, smaller[after[men.size() + e]]);
        }
        return slide(here, now);
    }
    /** Whether these places hold every kept place (null: none), each on its own. */
    private static boolean holds(double[][] places, double[][] last) {
        boolean[] used = new boolean[places.length];
        for (double[] p : last) {
            if (p == null) continue;
            int found = -1;
            for (int j = 0; j < places.length && found < 0; j++) if (!used[j] && same(places[j], p)) found = j;
            if (found < 0) return false;
            used[found] = true;
        }
        return true;
    }
    /** The single-loss slide over the places of one kind (keepPlaces): last[i] is unit i's place before (null: new), an index into places for each. */
    private static int[] slide(double[][] prev, double[][] places) {
        boolean[] shield = new boolean[places.length]; Arrays.fill(shield, true);
        int n = places.length; int[] at = new int[n]; boolean[] taken = new boolean[n]; boolean any = false;
        Arrays.fill(at, -1);
        for (int i = 0; i < n; i++) {
            if (prev[i] == null) continue;
            for (int j = 0; j < n; j++) if (!taken[j] && shield[j] == shield[i] && same(places[j], prev[i])) { at[i] = j; taken[j] = true; any = true; break; }
        }
        if (!any) { for (int i = 0; i < n; i++) at[i] = i; return at; }
        for (int guard = 0; guard < 4 * n * n + n; guard++) {
            int h = -1;
            for (int j = 0; j < n; j++) if (!taken[j] && (h < 0 || places[j][1] < places[h][1] - EPS || Math.abs(places[j][1] - places[h][1]) < EPS && places[j][0] < places[h][0])) h = j;
            if (h < 0) break;
            boolean kind = shield[h]; double[] hole = places[h]; int pick = -1; double best = Double.MAX_VALUE;
            // directly behind it in its file: the nearest of its kind standing (or, without a place now, last standing) further back at its side
            for (int i = 0; i < n; i++) {
                if (shield[i] != kind) continue;
                double[] p = at[i] >= 0 ? places[at[i]] : prev[i];
                if (p != null && Math.abs(p[0] - hole[0]) < EPS && p[1] > hole[1] + EPS && p[1] < best) { best = p[1]; pick = i; }
            }
            if (pick < 0) {
                // the man who has to come in: a unit without a place, the nearest by where it was (a newcomer, with none, after them)
                int donor = -1;
                for (int i = 0; i < n; i++) {
                    if (shield[i] != kind || at[i] >= 0) continue;
                    double d = prev[i] == null ? Double.MAX_VALUE / 2 : Math.hypot(prev[i][0] - hole[0], prev[i][1] - hole[1]);
                    if (d < best) { best = d; donor = i; }
                }
                // with none, the nearest of the rearmost rank of its kind, unless the empty place is in that rank
                if (donor < 0) {
                    double rearmost = -Double.MAX_VALUE;
                    for (int j = 0; j < n; j++) if (shield[j] == kind) rearmost = Math.max(rearmost, places[j][1]);
                    if (hole[1] < rearmost - EPS) for (int i = 0; i < n; i++) {
                        if (shield[i] != kind || at[i] < 0 || Math.abs(places[at[i]][1] - rearmost) > EPS) continue;
                        double d = Math.abs(places[at[i]][0] - hole[0]);
                        if (d < best) { best = d; donor = i; }
                    }
                }
                if (donor < 0) break; // cannot happen: each kind has a place for each of its units
                // the rank slides toward him: its man nearest the gap on his side, between the gap and him, steps in; with none, he does
                pick = donor; double[] from = at[donor] >= 0 ? places[at[donor]] : prev[donor];
                if (from != null) {
                    // a donor in another rank (the rear man who comes up) brings in the man of the gap's rank in his own file too: that man
                    // slides sideways and the donor steps up behind him, rather than the donor stepping across him on the diagonal
                    double reach = Math.abs(from[1] - hole[1]) > EPS ? -EPS : EPS, nearest = Double.MAX_VALUE;
                    double lo = hole[0] < from[0] ? hole[0] + EPS : from[0] + reach, hi = hole[0] < from[0] ? from[0] - reach : hole[0] - EPS;
                    for (int i = 0; i < n; i++) {
                        if (i == donor || shield[i] != kind || at[i] < 0) continue;
                        double[] q = places[at[i]];
                        if (Math.abs(q[1] - hole[1]) < EPS && q[0] > lo && q[0] < hi && Math.abs(q[0] - hole[0]) < nearest) { nearest = Math.abs(q[0] - hole[0]); pick = i; }
                    }
                }
            }
            if (at[pick] >= 0) taken[at[pick]] = false;
            at[pick] = h; taken[h] = true;
        }
        // a unit still without a place (a guard, never expected): the first free place
        for (int i = 0; i < n; i++) if (at[i] < 0) for (int j = 0; j < n; j++) if (!taken[j]) { at[i] = j; taken[j] = true; break; }
        return at;
    }
    private static final double EPS = 1e-6;
    private static boolean same(double[] a, double[] b) { return Math.abs(a[0] - b[0]) < EPS && Math.abs(a[1] - b[1]) < EPS; }
    /**
     * How far behind the first deploy row each row of five stands (Battle.reinforce): rows 3 apart, as always, but two rows of shield troops
     * (wall[r]: the row has troops and every one of them carries a shield) stand their cell apart (cell[r], the row's widest shield cell), so an
     * uncommanded shield group deploys as one deep wall.
     */
    public static double[] deployDepths(boolean[] wall, double[] cell, double rowGap) {
        double[] depth = new double[wall.length];
        for (int r = 1; r < wall.length; r++) depth[r] = depth[r - 1] + (wall[r - 1] && wall[r] ? Math.max(cell[r - 1], cell[r]) : rowGap);
        return depth;
    }
    /**
     * A held group's places (Battle.heldPlace): its members (slot order), the shape (formation, columns, wedge) and layout they stood in, and
     * the place each took, in cells (holdGrid) and in blocks (the layout).
     */
    public static final class Held<T> {
        public final List<T> members; public final String shape; public final double[][] layout;
        public final Map<T, double[]> grid = new HashMap<>(), at = new HashMap<>();
        Held(List<T> members, String shape, double[][] layout) { this.members = members; this.shape = shape; this.layout = layout; }
    }
    /**
     * The places of a group on Hold, kept through losses (Battle.heldPlace): the same members, shape and layout keep `kept`. Any change (a man
     * falls or joins, the wall's cell changes when its widest man fell, the layout changes) rebuilds them: on the same shape each member's last
     * place in cells goes to keepPlaces, so the wall closes as a wall closes ranks and a new cell only changes the spacing; a new shape takes the
     * places in slot order. A Hold given again after another order, with the same members and shape, keeps the cached places; losses during the
     * other order arrive as several empty places at once, which keepPlaces fills one at a time.
     */
    public static <T> Held<T> held(Held<T> kept, List<T> members, String shape, double[][] layout, boolean[] shield, int columns, boolean wedge) {
        if (kept != null && kept.members.equals(members) && kept.shape.equals(shape) && Arrays.deepEquals(kept.layout, layout)) return kept;
        double[][] prev = new double[members.size()][];
        if (kept != null && kept.shape.equals(shape)) for (int i = 0; i < prev.length; i++) prev[i] = kept.grid.get(members.get(i));
        int[] at = keepPlaces(prev, shield, columns, wedge); double[][] grid = holdGrid(shield, columns, wedge);
        Held<T> next = new Held<>(List.copyOf(members), shape, layout);
        for (int i = 0; i < at.length; i++) { next.grid.put(members.get(i), grid[at[i]]); next.at.put(members.get(i), layout[at[i]]); }
        return next;
    }
    /** A troop's cell in the deploy ring (Battle.deploy): a shield troop's wall cell (shieldCell), any other 2, the old ring's, or its width and 0.1 when that is more. */
    public static double deployCell(boolean shield, double width, double wallCell) { return shield ? shieldCell(width, wallCell) : Math.max(2, width + 0.1); }
    /**
     * Where each slot of a squad stands in its deploy ring (Battle.deploy: trained troops at the barracks or a forward anchor), {x, z} from the
     * ring's centre: rows of five, as the reinforce rows (deployRow, deployDepths). cell[i] is slot i's cell (deployCell; 0 or less: an empty
     * slot), shield[i] whether it carries a shield. In a row neighbours stand half of each one's cell apart; two rows of nothing but shield troops
     * stand their wall cell apart, one deep wall (a wall row's empty places take its cell); any other two rows stand `gap` apart, or half of
     * the two rows' widest cells when that is more. So no two troops of any mix stand closer than their cells allow. At least three rows, centred
     * on the middle of the third (slot 12): with every cell `gap` this is the old ring, (slot % 5 - 2, slot / 5 - 2) x gap.
     */
    public static double[][] deployRing(boolean[] shield, double[] cell, double gap) {
        int size = Math.max(15, (shield.length + 4) / 5 * 5), rows = size / 5;
        boolean[] wall = new boolean[rows], troops = new boolean[rows]; double[] rowCell = new double[rows], widest = new double[rows], cells = new double[size], depth = new double[rows];
        Arrays.fill(wall, true);
        for (int i = 0; i < shield.length; i++) {
            if (cell[i] <= 0) continue;
            troops[i / 5] = true;
            if (shield[i]) rowCell[i / 5] = Math.max(rowCell[i / 5], cell[i]); else wall[i / 5] = false;
        }
        for (int r = 0; r < rows; r++) wall[r] &= troops[r];
        for (int i = 0; i < size; i++) { cells[i] = i < cell.length && cell[i] > 0 ? cell[i] : wall[i / 5] ? rowCell[i / 5] : gap; widest[i / 5] = Math.max(widest[i / 5], cells[i]); }
        for (int r = 1; r < rows; r++) depth[r] = depth[r - 1] + (wall[r - 1] && wall[r] ? Math.max(rowCell[r - 1], rowCell[r]) : Math.max(gap, (widest[r - 1] + widest[r]) / 2));
        double[][] ring = new double[size][];
        for (int r = 0; r < rows; r++) {
            double[] row = deployRow(Arrays.copyOfRange(cells, r * 5, r * 5 + 5));
            for (int i = 0; i < 5; i++) ring[r * 5 + i] = new double[]{row[i], depth[r] - depth[2]};
        }
        return ring;
    }
    /** How far behind its order's point a group forms (Battle.formation and CommandMode): support 9, siege 7 (a mortar cannot fire within its minimum range), ranged 5, the rest 0. */
    public static double roleDepth(String group) {
        return switch (group) { case "support" -> 9; case "siege" -> 7; case "ranged" -> 5; default -> 0; };
    }
    /** How many abreast a group forms in a formation of this many columns: support 2, siege and ranged 6, the rest 12. */
    public static int groupColumns(String group, int formationColumns) {
        return Math.min(formationColumns, switch (group) { case "support" -> 2; case "siege", "ranged" -> 6; default -> 12; });
    }
    /**
     * The sideways offsets of one deploy row (Battle.reinforce) from the cells of its places: neighbours stand half of each one's cell apart, so
     * shield troops form their default Hold at the wall cell, and the row is centred on its middle place (index 2 of a row of 5). With every cell
     * the same c this is (i - 2) x c, the old row.
     */
    public static double[] deployRow(double[] cells) {
        double[] at = new double[cells.length];
        for (int i = 1; i < cells.length; i++) at[i] = at[i - 1] + (cells[i - 1] + cells[i]) / 2;
        int middle = (cells.length - 1) / 2; double shift = cells.length == 0 ? 0 : at[middle];
        for (int i = 0; i < at.length; i++) at[i] -= shift;
        return at;
    }
    /** Index of the anchor nearest the target (tx,tz), or -1 when no anchor is strictly nearer to the target than the base (bx,bz). */
    public static int nearestAnchor(double tx, double tz, double bx, double bz, double[] ax, double[] az) {
        int best = -1;
        double bestDistance = Math.hypot(tx - bx, tz - bz);
        for (int i = 0; i < Math.min(ax.length, az.length); i++) {
            double distance = Math.hypot(tx - ax[i], tz - az[i]);
            if (distance < bestDistance) { bestDistance = distance; best = i; }
        }
        return best;
    }
    /** Index of the point to attack: the one minimising distance + defenders*defenderWeight among points not owned by team (all points when the team owns every one). */
    public static int leastDefended(double cx, double cz, double[] px, double[] pz, int[] owner, int[] defenders, int team, double defenderWeight) {
        int n = Math.min(Math.min(px.length, pz.length), Math.min(owner.length, defenders.length));
        boolean anyFree = false;
        for (int i = 0; i < n; i++) if (owner[i] != team) { anyFree = true; break; }
        int best = -1;
        double bestValue = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (anyFree && owner[i] == team) continue;
            double value = Math.hypot(px[i] - cx, pz[i] - cz) + defenders[i] * defenderWeight;
            if (value < bestValue) { bestValue = value; best = i; }
        }
        return best;
    }
    /** Per army index, the point index that army must hold, or -1 (free to flank). Positions are {x,z}; near means within nearRadius.
     *  Guards first: a point owned by team keeps the nearest army standing near it as its guard (ties: the lower army index), and that army
     *  holds it, so a point is not left the moment it is taken. Then every owned point still unguarded takes the nearest army that is neither
     *  guarding nor holding. In both passes the nearest army-point pair is settled first, so an army takes at most one point and goes to the
     *  nearer of two. assignable (one per army) is false for an army the bots cannot order, a player's: it still guards the point it stands
     *  on, so no bot is pulled back to it, but it is never sent to one and always gets -1. */
    public static int[] garrison(int[] owners, int team, double[][] armyPos, boolean[] assignable, double[][] pointPos, double nearRadius) {
        int[] hold = new int[armyPos.length];
        for (int i = 0; i < hold.length; i++) hold[i] = -1;
        int n = Math.min(owners.length, pointPos.length);
        boolean[] open = new boolean[n], taken = new boolean[armyPos.length];
        for (int j = 0; j < n; j++) open[j] = owners[j] == team;
        int[] pair;
        while ((pair = nearestPair(open, taken, null, armyPos, pointPos, nearRadius)) != null) {
            taken[pair[0]] = true;
            open[pair[1]] = false;
            if (assignable[pair[0]]) hold[pair[0]] = pair[1];
        }
        while ((pair = nearestPair(open, taken, assignable, armyPos, pointPos, Double.POSITIVE_INFINITY)) != null) {
            taken[pair[0]] = true;
            open[pair[1]] = false;
            hold[pair[0]] = pair[1];
        }
        return hold;
    }
    /** The nearest {army, point} pair, at most limit apart, of an open point and an army not yet taken (and assignable, unless that is null),
     *  or null. Points, then armies, are scanned in index order and only a strictly nearer pair replaces the best, so ties go to the lower
     *  point index, then the lower army index. */
    private static int[] nearestPair(boolean[] open, boolean[] taken, boolean[] assignable, double[][] armyPos, double[][] pointPos, double limit) {
        int[] best = null;
        double bestDistance = limit;
        for (int j = 0; j < open.length; j++) {
            if (!open[j]) continue;
            for (int i = 0; i < armyPos.length; i++) {
                if (taken[i] || assignable != null && !assignable[i]) continue;
                double distance = Math.hypot(armyPos[i][0] - pointPos[j][0], armyPos[i][1] - pointPos[j][1]);
                if (best == null ? distance <= limit : distance < bestDistance) { best = new int[]{i, j}; bestDistance = distance; }
            }
        }
        return best;
    }
    /** The point index a bot army's lane sends it to. Dawn's lane i is point i; Dusk's is the mirror, points-1-i, so both teams
     *  play the same plan from their own side (Kingsfield: Dawn A, B, C against Dusk E, D, C; three points: Dusk C, B, A). */
    public static int laneTarget(int lane, int team, int points) {
        int i = Math.floorMod(lane, points);
        return team == 0 ? i : points - 1 - i;
    }
    /** A charge goes in when the army outnumbers the enemies near it by chargeRatio and, when chargeDistance is above zero, the nearest enemy is within it. */
    public static boolean timedCharge(double nearestEnemyDistance, long living, long enemiesNear, double chargeRatio, double chargeDistance) {
        return living >= enemiesNear * chargeRatio && (chargeDistance <= 0 || nearestEnemyDistance <= chargeDistance);
    }
    /** Bot point choice weights (bot-AI plan step 2): a point off the army's lane, a contested point (a fight to join), each other friendly bot
     *  army already sent there (so a team's armies spread over the map, audit D1), the army's current point (no flapping between two near-equal
     *  points), and an owned, uncontested point (a fall-back only: nothing is left to take). */
    public static final double POINT_OFF_LANE = 90, POINT_CONTESTED = 55, POINT_CLAIMED = 120, POINT_KEEP = 25, POINT_OWNED = 400;
    /** One point's cost for a bot army at (cx, cz); the lowest wins (pickPoint). A point this team owns counts as open to take again only while it is contested. */
    public static double pointValue(double cx, double cz, double px, double pz, int owner, boolean contested, int team, boolean onLane, int claims, boolean current) {
        double v = Math.hypot(cx - px, cz - pz) + claims * POINT_CLAIMED - (current ? POINT_KEEP : 0);
        if (owner != team || contested) v += (onLane ? 0 : POINT_OFF_LANE) - (contested ? POINT_CONTESTED : 0);
        else v += POINT_OWNED;
        return v;
    }
    /** The point a bot army goes for (pointValue over every point): claims[i] counts this team's other bot armies already sent to point i, current is the army's point now (-1 none). */
    public static int pickPoint(double cx, double cz, double[] px, double[] pz, int[] owner, boolean[] contested, int team, int lane, int[] claims, int current) {
        int best = -1; double bestValue = Double.MAX_VALUE;
        for (int i = 0; i < px.length; i++) {
            double v = pointValue(cx, cz, px[i], pz[i], owner[i], contested[i], team, i == lane, claims[i], i == current);
            if (v < bestValue) { bestValue = v; best = i; }
        }
        return best;
    }
    /** How far behind a point's centre a bot's ranged group stands to shoot across it: its reach less the point's radius and 2, so its arrows cover
     *  the far edge, kept between 5 (the old fixed role depth) and the radius plus 2 (still on the point's edge, where it counts as on the objective). */
    public static double rangedStandoff(double reach, double radius) { return Math.max(5, Math.min(radius + 2, reach - radius - 2)); }
    /** Minecraft yaw, in degrees, of a body facing along (dx, dz): +z is 0, +x is -90 (Dawn's default facing), -x is 90. */
    public static float yawToward(double dx, double dz) { return (float) Math.toDegrees(Math.atan2(-dx, dz)); }
    /** The smaller angle between two yaws, in degrees (0 to 180). */
    public static double yawGap(double a, double b) { double d = Math.abs(((a - b) % 360 + 540) % 360 - 180); return d; }
    /** The group a bot army leaves to guard a point it took: the smallest of its infantry, ranged and cavalry groups with at least 2 living troops
     *  (infantry first on a tie, then ranged), so the bulk of the army moves on; null when the army has fewer than two groups of 2 or more (it is one group: all of it stays or goes). */
    public static String guardGroup(int infantry, int ranged, int cavalry, int support, int siege) {
        int groups = (infantry >= 2 ? 1 : 0) + (ranged >= 2 ? 1 : 0) + (cavalry >= 2 ? 1 : 0) + (support >= 2 ? 1 : 0) + (siege >= 2 ? 1 : 0);
        if (groups < 2) return null;
        String best = null; int size = Integer.MAX_VALUE;
        String[] ids = {"infantry", "ranged", "cavalry"}; int[] n = {infantry, ranged, cavalry};
        for (int i = 0; i < ids.length; i++) if (n[i] >= 2 && n[i] < size) { size = n[i]; best = ids[i]; }
        return best;
    }
    /** Whether a bot's waiting refills move up together: enough have gathered at the rally spot (need), or the first has waited the timeout. */
    public static boolean musterGo(int gathered, int need, long waitedTicks, long timeoutTicks) { return gathered >= need || waitedTicks >= timeoutTicks; }
    /** Whether a shield troop holds its shield up. Never on a charge or a retreat, and never with an enemy in reach
     *  (nearestSq at or inside reachSq, both squared blocks): the troop lowers it to swing, as a player must. On Hold any
     *  enemy raises it (hasEnemy: one picked or in sight), and so does archer fire from one it cannot see (underFire);
     *  on any other order only archer fire does. On Attack (Kort 2026-11-11: an attack is a run into contact, not a wall
     *  walking forward) only archer fire with no enemy picked yet raises it: once the troop has an enemy to close on it
     *  runs at it with the shield down, as on a charge. */
    public static boolean raiseShield(String order, boolean underFire, double nearestSq, double reachSq, boolean hasEnemy) {
        if (order.equals("charge") || order.equals("retreat")) return false;
        if (order.equals("attack") && hasEnemy) return false;
        if (hasEnemy && nearestSq <= reachSq) return false;
        if (order.equals("hold")) return hasEnemy || underFire;
        return underFire;
    }
    /** The order a shield troop raises its shield by (raiseShield): a Move group that has arrived stands as on Hold. A Retreat
     *  stays a Retreat, arrived or not, so it never raises; walls, formation and settle keep the order itself. */
    public static String shieldOrder(String order, boolean arrived) { return arrived && order.equals("move") ? "hold" : order; }
    /** Whether a squad group is still under fire: it was shot at (shotAt, battle ticks; 0 means never) at most memoryTicks ago. */
    public static boolean underFire(long now, long shotAt, int memoryTicks) { return shotAt > 0 && now - shotAt <= memoryTicks; }
    /**
     * Round gold from held points per income tick: held times (pointGold + bonus), capped at objectiveIncomeCap scaled by the same
     * ratio, so the cap still stands for the same number of points (the Quartermaster's bonus: 3 a point becomes 5, and the cap of 9,
     * three points, becomes 15). With bonus 0 it is the Phase 1 rule min(cap, held x pointGold).
     */
    public static int pointIncome(long held, int pointGold, int bonus, int cap) {
        int per = pointGold + bonus;
        long limit = pointGold > 0 ? (long) cap * per / pointGold : cap;
        return (int) Math.max(0, Math.min(limit, held * per));
    }
    /** A troop's round cost under a class factor (the Warlock's magic discount): the sheet cost times the factor, rounded half up. */
    public static int classCost(int cost, double factor) { return (int) Math.round(cost * factor); }
    /** Ticks a troop trains under a class factor (the Marshal's drill): the sheet seconds times 20 times the factor, rounded. */
    public static long trainingTicks(double seconds, double factor) { return Math.round(seconds * 20 * factor); }
    /**
     * Whether a melee swing's blow lands on its impact frame: the attacker still alive and not stunned since it swung, the target
     * still alive, and the target within the attacker's reach plus the grace (distSq in squared blocks). A blow that fails this misses.
     */
    public static boolean strikeLands(boolean attackerAlive, boolean stunnedSinceSwing, boolean targetAlive, double distSq, double reach, double grace) {
        double far = reach + grace;
        return attackerAlive && !stunnedSinceSwing && targetAlive && distSq <= far * far;
    }
    /**
     * The index of the swing a troop plays out of its family's count variants, from a random roll: never last (the swing it played
     * before) while there is another to play. A first swing (last outside 0..count-1) may be any; one or no variant plays index 0.
     */
    public static int pickVariant(int count, int last, long roll) {
        if (count <= 1) return 0;
        if (last < 0 || last >= count) return (int) Math.floorMod(roll, (long) count);
        int i = (int) Math.floorMod(roll, (long) (count - 1));
        return i >= last ? i + 1 : i;
    }
    /**
     * The seed of a round's variant rolls (Battle.rolls, reseeded by startRound; round 0 before the first): the world's seed and the round's number mixed
     * by SplitMix64's finalizer, so a round's swing sequence depends on those two alone, never on how many swings the rounds before it made, and
     * neighbouring rounds do not start java.util.Random on neighbouring seeds (whose first draws run close together).
     */
    public static long roundSeed(long worldSeed, int round) {
        long z = worldSeed + 0x9E3779B97F4A7C15L * (round + 1L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
    /**
     * Ticks from a melee swing to its blow: the swing's impactTicks, but always at least a tick before the attacker's next swing can
     * start (untilNextSwing ticks off: the cooldown as the swing set it), so no blow is ever still pending when the next swing comes.
     * No attack haste exists; one that shortened a cooldown already running would have to re-clamp the blows then pending
     * (Battle.strikes), since this clamp is taken once, at the swing.
     */
    public static int impactDelay(int impactTicks, long untilNextSwing) {
        return (int) Math.max(0, Math.min(impactTicks, untilNextSwing - 1));
    }
    /**
     * The Charge stamp (Squad.chargedAt of the attacker's group) whose impact window holds tick now, for a blow whose attacker is charging: the
     * stamp when now is at most window ticks after it; null when he is not charging, has no stamp, or the window has passed. A melee blow takes
     * this on its swing tick (Ruling R29): its wind-up never shortens the window.
     */
    public static Long chargeStamp(boolean charging, Long charged, long now, int window) {
        return charging && charged != null && now - charged <= window ? charged : null;
    }
    /** Whether a blow crashes in on its Charge's impact (the counters condition charge_impact): it fell inside a stamp's window, and its attacker has not landed an impact under that stamp already (chargeHitAt, the tick he last did). */
    public static boolean chargeImpact(Long stamp, long chargeHitAt) {
        return stamp != null && chargeHitAt < stamp;
    }
    /**
     * A surrounded Shield wall's square (command UI Task 6): n places as {side, back, facingYaw}, in the same side/back frame as holdLayout (back
     * from the front, side across) and a facing relative to the formation's yaw: the front edge faces 0, the -side edge 90, the back edge 180 and
     * the +side edge 270, so every man faces out. The places go round the four edges in turn (front, +side, back, -side), so the edges hold
     * n / 4 each, the first ones one more; each edge is centred, and the square's half width is (k + 1) / 2 cells for k men on the longest edge,
     * so the corners stay a cell apart.
     */
    public static double[][] squareLayout(int n, double cell) {
        double[][] places = new double[Math.max(n, 0)][];
        int k = (Math.max(n, 0) + 3) / 4; double half = (k + 1) / 2.0 * cell;
        int[] onEdge = new int[4];
        for (int i = 0; i < places.length; i++) onEdge[i % 4]++;
        for (int i = 0; i < places.length; i++) {
            int edge = i % 4, at = i / 4; double along = (at - (onEdge[edge] - 1) / 2.0) * cell;
            places[i] = switch (edge) {
                case 0 -> new double[]{along, 0, 0};
                case 1 -> new double[]{half, half + along, 270};
                case 2 -> new double[]{-along, 2 * half, 180};
                default -> new double[]{-half, half - along, 90};
            };
        }
        return places;
    }
    /** The half angle (degrees) of a Shield wall man's front arc: an arrow from within it of his facing is the one his shield stops (FormationRules.arrowBlock). */
    public static final double FRONT_ARC = 60;
    /** Whether a point dx, dz away lies within halfDeg of a facing yaw (relativeBearing): the Shield wall's front-arc test. */
    public static boolean frontArc(double facingYawDeg, double dx, double dz, double halfDeg) {
        return relativeBearing(facingYawDeg, dx, dz) <= halfDeg;
    }
}
