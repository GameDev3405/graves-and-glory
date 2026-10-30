package gg.gravesandglory;

import java.util.*;

/**
 * Pure counter-table maths (spec section 3) and the House trait multipliers (spec 1.3). No Minecraft types, so the
 * standalone HousesCheck compiles it. Every multiplier comes from the counters and houseTraits sheets.
 */
public final class CounterMath {
    private CounterMath() {}
    /** A hit is frontal within this bearing of the target's facing: the half-plane a vanilla shield covers. */
    static final double FRONT_DEGREES = 90;

    /**
     * One troop hit as the counter table sees it. attacker and target hold troop tags, ability ids and the dynamic tags
     * (melee, arrow, shot, magic, nonsiege, ranged, charging, holding, structure); a row's "any" matches every hit.
     * bearing is BattleMath.relativeBearing(targetYaw, attacker - target): 0 straight in front of the target, 180 behind.
     * raised is the target's shield. chargeImpact (the first hit of a fresh Charge), charging and holding are the
     * attacker's group order, noSky is whether the attacker cannot see the sky, inMist is the shooter or the target
     * standing in a Mist cloud, covered is the target's flanks covered (Gilded Bulwark: Houses.covered) and area is a
     * blow with no direction (blight, a shell, mist, the splash of a slam or a cleave on the others around the target).
     * targetSlowed is the target under Slowness and targetWounded the target at or below combatDiscipline.woundedShare of its max health (in-match
     * progression N9: the conditions target_slowed_or_wounded and target_wounded).
     */
    public record Hit(Set<String> attacker, Set<String> target, double bearing, boolean raised, boolean chargeImpact, boolean charging, boolean holding, boolean noSky, boolean inMist, boolean covered, boolean area, boolean targetSlowed, boolean targetWounded) {
        /** A hit on a target that is neither slowed nor wounded (the Phase 2 shape). */
        public Hit(Set<String> attacker, Set<String> target, double bearing, boolean raised, boolean chargeImpact, boolean charging, boolean holding, boolean noSky, boolean inMist, boolean covered, boolean area) { this(attacker, target, bearing, raised, chargeImpact, charging, holding, noSky, inMist, covered, area, false, false); }
        /** A hit with uncovered flanks and a direction (the Phase 1 shape). */
        public Hit(Set<String> attacker, Set<String> target, double bearing, boolean raised, boolean chargeImpact, boolean charging, boolean holding, boolean noSky, boolean inMist) { this(attacker, target, bearing, raised, chargeImpact, charging, holding, noSky, inMist, false, false); }
    }

    /** True when a counters row applies to a hit: its attacker tag, its target tag (a+b needs both) and its condition. */
    public static boolean matches(Spec.CountersRow r, Hit h) {
        return tagged(r.attackerTag(), h.attacker()) && tagged(r.targetTag(), h.target()) && condition(r.condition(), h);
    }
    static boolean tagged(String rule, Set<String> tags) {
        for (String part : rule.split("\\+")) if (!part.equals("any") && !tags.contains(part)) return false;
        return true;
    }
    /**
     * The closed condition list (spec 6.1). Front is a bearing of FRONT_DEGREES or less; flank is more than combatDiscipline.flankArcDegrees, and never for a covered
     * target or a blow with no direction (area). target_slowed_or_wounded (Run them down, C20) holds for a slowed or a wounded target, once for both; target_wounded
     * (Unbroken, C21) for a wounded one only.
     */
    static boolean condition(String condition, Hit h) {
        boolean front = h.bearing() <= FRONT_DEGREES;
        return switch (condition) {
            case "always" -> true;
            case "raised_front" -> h.raised() && front;
            case "lowered_front" -> !h.raised() && front;
            case "front" -> front;
            case "flank" -> h.bearing() > flankDegrees() && !h.covered() && !h.area();
            case "charge_impact" -> h.chargeImpact();
            case "charge" -> h.charging();
            case "hold" -> h.holding();
            case "no_sky" -> h.noSky();
            case "in_mist" -> h.inMist();
            case "target_slowed_or_wounded" -> h.targetSlowed() || h.targetWounded();
            case "target_wounded" -> h.targetWounded();
            default -> false;
        };
    }
    /** The flank arc (combatDiscipline.flankArcDegrees, 110): counter rows C16 and C16s, Pack Hunters and the pierce cone read one number. */
    static double flankDegrees() { return Spec.COMBATDISCIPLINE[0].flankArcDegrees(); }

    /** The counter multiplier for a hit: the product of the applied rows' multipliers, 1 when no row matches. */
    public static double counters(Hit h, Spec.CountersRow[] rows) {
        double product = 1;
        for (var r : winners(h, rows)) product *= r.multiplier();
        return product;
    }
    /** The ids of the rows counters(h, rows) multiplies, in sheet order: the largest of each group of matched rows. */
    public static List<String> applied(Hit h, Spec.CountersRow[] rows) {
        List<String> ids = new ArrayList<>();
        for (var r : winners(h, rows)) ids.add(r.id());
        return ids;
    }
    /**
     * The matched rows that contribute. A matched row and the matched rows its replaces column lists form one group (groups
     * that share a row merge), and each group contributes only its largest multiplier; a tie keeps the earlier row.
     * A level row (one that names a tag only a level ability grants: levelOnly) takes the place of the matched rows it replaces, whatever their size
     * (in-match progression Task 6): those rows drop out of their group's contest, so Better guard's C1g (x0.30) stands in place of C1 (x0.35) and C3g
     * (x0.5) in place of C3 (x0.6). The group still holds them, so a row that shares one with a level row (C15 and C1g, both over C1) still meets it there.
     */
    static List<Spec.CountersRow> winners(Hit h, Spec.CountersRow[] rows) {
        int n = rows.length;
        int[] group = new int[n]; // the group label of a matched row, -1 for a row that does not match
        boolean[] dropped = new boolean[n]; // a matched row a matched level row replaces
        Set<String> granted = grantedTags();
        for (int i = 0; i < n; i++) group[i] = matches(rows[i], h) ? i : -1;
        for (int i = 0; i < n; i++) {
            if (group[i] < 0 || rows[i].replaces().equals("none")) continue;
            boolean level = levelOnly(rows[i], granted);
            for (String id : rows[i].replaces().split("\\|"))
                for (int j = 0; j < n; j++)
                    if (group[j] >= 0 && rows[j].id().equals(id)) { relabel(group, group[j], group[i]); if (level) dropped[j] = true; }
        }
        List<Spec.CountersRow> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (group[i] < 0 || dropped[i]) continue;
            int best = -1;
            for (int j = 0; j < n; j++) if (group[j] == group[i] && !dropped[j] && (best < 0 || rows[j].multiplier() > rows[best].multiplier())) best = j;
            if (best == i) out.add(rows[i]);
        }
        return out;
    }
    /** One counter line of a troop card: the row, whether it hits the troop (incoming) or the troop deals it (outgoing), its multiplier and its text. */
    public record CardRow(String id, boolean incoming, double multiplier, String text) {}
    /**
     * The counter rows a troop card shows (Task 12, review S1 of fix round 1), in sheet order: every row whose own tag side names one of the
     * troop's tags and that applies on the card's canonical hit, through applied. The troop's tags are troops.tags, its ability and its group
     * (ArmyMobs.tags); on the outgoing side they also hold its attack kind's tags (kindTags of attackKind: melee, arrow and shot, or bolt, shot
     * and magic), as Combat.resolve adds them at hit time.
     * Incoming: the row's attacker tags on the troop from the front, its shield raised if it has one, braced (holding). Outgoing: the troop on
     * the row's target tags from the front, a shield there raised, the troop on Hold and out of the sky. A row a charge keys (charge,
     * charge_impact) is read on a canonical charge instead, the first blow of a fresh Charge, against every row, so a row that replaces it
     * (C5 over C6) wins as it does in a fight. Rows of multiplier 1 and an "any" tag side are left out.
     */
    public static List<CardRow> cardRows(Set<String> own, String kind, Spec.CountersRow[] rows) {
        List<CardRow> out = new ArrayList<>();
        Set<String> striking = new HashSet<>(own);
        striking.addAll(kindTags(kind));
        Set<String> braced = new HashSet<>(own);
        braced.add("holding");
        Set<String> granted = grantedTags();
        for (var r : rows) {
            if (r.multiplier() == 1.0) continue;
            if (levelOnly(r, granted)) continue; // a row only a level ability can trigger: the card shows that level's ability line instead
            boolean charge = r.condition().equals("charge") || r.condition().equals("charge_impact");
            var against = charge ? rows : new Spec.CountersRow[]{r};
            if (!r.attackerTag().equals("any") && tagged(r.attackerTag(), striking)) {
                Set<String> to = parts(r.targetTag());
                var hit = charge ? new Hit(striking, to, 0, to.contains("shield"), true, true, false, false, false) : new Hit(striking, to, 0, to.contains("shield"), false, false, true, true, false);
                if (applied(hit, against).contains(r.id())) out.add(new CardRow(r.id(), false, r.multiplier(), r.text()));
            }
            if (!r.targetTag().equals("any") && tagged(r.targetTag(), braced) && !tagged(r.targetTag(), Set.of("holding"))) {
                var hit = new Hit(parts(r.attackerTag()), braced, 0, own.contains("shield"), charge, charge, false, false, false);
                if (applied(hit, against).contains(r.id())) out.add(new CardRow(r.id(), true, r.multiplier(), r.text()));
            }
        }
        return out;
    }
    private static Spec.AbilitiesRow[] grantedFrom;
    private static Set<String> granted = Set.of();
    /** The counter tags the abilities rows grant (abilities.tags, | joined): the tags only a level 2 or level 3 ability brings to a soldier. Read once per abilities sheet (winners asks on every blow). */
    static synchronized Set<String> grantedTags() {
        if (grantedFrom != Spec.ABILITIES) {
            Set<String> out = new HashSet<>();
            for (var a : Spec.ABILITIES) if (!a.tags().equals("none")) out.addAll(Arrays.asList(a.tags().split("\\|")));
            granted = Collections.unmodifiableSet(out);
            grantedFrom = Spec.ABILITIES;
        }
        return granted;
    }
    /** Whether a counter row names a granted tag on either side: it applies to a soldier only once a level ability grants the tag. */
    static boolean levelOnly(Spec.CountersRow r, Set<String> granted) {
        for (String part : (r.attackerTag() + "+" + r.targetTag()).split("\\+")) if (granted.contains(part)) return true;
        return false;
    }
    /** The tags a hit of an attack kind adds to its attacker (Combat.resolve reads these): melee; arrow and shot; bolt, shot and magic; blight and magic; slam and melee. */
    public static Set<String> kindTags(String kind) {
        return switch (kind) {
            case "melee" -> Set.of("melee");
            case "arrow" -> Set.of("arrow", "shot");
            case "bolt" -> Set.of("bolt", "shot", "magic"); // a caster's magic bolt. Not the attackVariants kind "bolt", which is a crossbow's animation kind (crossbows fire "arrow" here)
            case "blight" -> Set.of("blight", "magic");
            case "slam" -> Set.of("slam", "melee");
            default -> Set.of();
        };
    }
    /** A troop's main attack kind, as Abilities fires it: melee for a troop that is not ranged, a bolt for a magic one (Abilities.visual), else an arrow (a trident lands as an arrow). */
    public static String attackKind(Spec.TroopsRow t) { return !t.ranged() ? "melee" : t.magic() ? "bolt" : "arrow"; }
    static Set<String> parts(String rule) {
        Set<String> out = new HashSet<>();
        for (String part : rule.split("\\+")) if (!part.equals("any")) out.add(part);
        return out;
    }
    static void relabel(int[] group, int from, int to) {
        for (int k = 0; k < group.length; k++) if (group[k] == from) group[k] = to;
    }

    /** The full multiplier for a hit: counters x traits x auras x commander, clamped to [min, max] (combatDiscipline.multiplierMin and multiplierMax). */
    public static double total(double counters, double traits, double auras, double commander, double min, double max) {
        return Math.max(min, Math.min(max, counters * traits * auras * commander));
    }

    /** A House's trait row at a tier, or null at tier 0 (or for a tier the sheet lacks). */
    public static Spec.HouseTraitsRow trait(Spec.HouseTraitsRow[] rows, String house, int tier) {
        if (tier <= 0) return null;
        for (var r : rows) if (r.house().equals(house) && r.tier() == tier) return r;
        return null;
    }

    /** Gilded Bulwark: a shield-tag troop takes 1 - value from an arrow, bolt or blight hit, raised shield or not; anything else, or no Bulwark row, is 1. */
    public static double bulwark(Spec.HouseTraitsRow t, boolean shieldTag, String kind) {
        boolean ranged = "arrow".equals(kind) || "bolt".equals(kind) || "blight".equals(kind);
        return is(t, "bulwark") && shieldTag && ranged ? 1 - t.value() : 1;
    }

    /**
     * Wildmoot Pack Hunters: an attack deals 1 + value when at least value2 own Wildmoot troops target the same enemy, or
     * when the attacker strikes from the flank (a bearing beyond combatDiscipline.flankArcDegrees; a covered target does not
     * take it off, which keeps Wildmoot the best flankers). Both at once still give it once.
     */
    public static double packHunters(Spec.HouseTraitsRow t, int sameTarget, double bearing) {
        return is(t, "pack_hunters") && (sameTarget >= t.value2() || bearing > flankDegrees()) ? 1 + t.value() : 1;
    }

    /** Ironclad Entrench: a troop that has stood still takes 1 - value, with the reduction doubled inside an owned objective's capture radius; a moving troop takes full damage. */
    public static double entrench(Spec.HouseTraitsRow t, boolean still, boolean onOwnedPoint) {
        return is(t, "entrench") && still ? 1 - t.value() * (onOwnedPoint ? 2 : 1) : 1;
    }

    /** Tidecallers Riptide: the movement-speed multiplier, 1 + value in water or while it rains, else 1. */
    public static double riptideSpeed(Spec.HouseTraitsRow t, boolean wetOrRain) {
        return is(t, "riptide") && wetOrRain ? 1 + t.value() : 1;
    }
    static boolean is(Spec.HouseTraitsRow t, String mechanic) { return t != null && t.mechanic().equals(mechanic); }
}
