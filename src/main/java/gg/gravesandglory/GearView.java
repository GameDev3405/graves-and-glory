package gg.gravesandglory;

import java.util.*;

/**
 * The Arsenal screen's view rules (gear screen plan Task 1): filters, sorts, stat totals, set progress, compare lines, tooltips,
 * relic-slot targeting, grid scrolling and the Forge tab's previews. Pure: it reads a GearWire.View and the Gear and Forge lookups,
 * so the standalone checks run it without Minecraft. Every name comes from the sheets; the client never decides an outcome.
 */
final class GearView {
    private GearView() {}

    static final List<String> FILTERS = List.of("all", "helm", "chest", "legs", "boots", "weapon", "relic");
    static final List<String> SORTS = List.of("rarity", "newest", "slot", "set");
    static final int COLUMNS = 8;
    /** The words the tooltip and the Worn row use for each of Gear.SLOTS. */
    static final Map<String, String> SLOT_WORDS = Map.of("helm", "helm", "chest", "chest", "legs", "legs", "boots", "boots", "main", "main hand", "off", "off hand");
    /** The stat names, capitalised from Gear.STATS. */
    static String statWord(int i) {
        String s = Gear.STATS.get(i);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static boolean resolves(Gear.Piece p) { return p != null && p.item != null && Gear.item(p.item) != null && Gear.rarity(p.rarity) != null; }
    private static boolean matches(Gear.Piece p, String filter) {
        String slot = Gear.item(p.item).slot();
        return switch (filter == null ? "all" : filter) {
            case "all" -> true;
            case "weapon" -> slot.equals("main") || slot.equals("off");
            case "relic" -> false;
            default -> slot.equals(filter);
        };
    }
    private static int setOrder(Gear.Piece p) {
        var it = Gear.item(p.item);
        if (it.slot().equals("main") || it.slot().equals("off")) return Integer.MAX_VALUE;
        for (int i = 0; i < Spec.GEARSETS.length; i++) if (Spec.GEARSETS[i].id().equals(it.set())) return i;
        return Integer.MAX_VALUE - 1;
    }

    /** The pieces the grid shows for this filter and sort; a piece whose item or rarity the sheet no longer has is skipped. */
    static List<Gear.Piece> visible(GearWire.View v, String filter, String sort) {
        List<Gear.Piece> out = new ArrayList<>();
        if (v == null) return out;
        for (Gear.Piece p : v.pieces()) if (resolves(p) && matches(p, filter)) out.add(p);
        Comparator<Gear.Piece> byUid = Comparator.comparingInt(p -> p.uid);
        Comparator<Gear.Piece> rarityDesc = Comparator.comparingInt((Gear.Piece p) -> -Gear.rarityIndex(p.rarity));
        Comparator<Gear.Piece> slotOrder = Comparator.comparingInt(p -> Gear.SLOTS.indexOf(Gear.item(p.item).slot()));
        Comparator<Gear.Piece> c = switch (sort == null ? "rarity" : sort) {
            case "newest" -> Comparator.comparingInt((Gear.Piece p) -> -p.uid);
            case "slot" -> slotOrder.thenComparing(rarityDesc).thenComparing(byUid);
            case "set" -> Comparator.comparingInt(GearView::setOrder).thenComparing(slotOrder).thenComparing(byUid);
            default -> rarityDesc.thenComparing(p -> Gear.item(p.item).name()).thenComparing(byUid);
        };
        out.sort(c);
        return out;
    }

    /** The owned relics in sheet order. */
    static List<String> visibleRelics(GearWire.View v) {
        List<String> out = new ArrayList<>();
        if (v == null) return out;
        for (var r : Spec.RELICS) if (v.relics().contains(r.id())) out.add(r.id());
        return out;
    }

    static Gear.Piece piece(GearWire.View v, int uid) {
        if (v == null) return null;
        for (Gear.Piece p : v.pieces()) if (p.uid == uid) return p;
        return null;
    }
    /** The piece worn in a slot, or null. */
    static Gear.Piece wornIn(GearWire.View v, String slot) {
        Integer uid = v == null ? null : v.equipped().get(slot);
        return uid == null ? null : piece(v, uid);
    }
    static boolean worn(GearWire.View v, int uid) { return v != null && v.equipped().containsValue(uid); }

    /** The summed stats of the worn pieces, in Gear.STATS order. */
    static double[] totals(GearWire.View v) {
        double[] sum = new double[Gear.STATS.size()];
        if (v == null) return sum;
        for (String slot : Gear.SLOTS) {
            Gear.Piece p = wornIn(v, slot);
            if (p == null) continue;
            double[] s = Gear.stats(p);
            for (int i = 0; i < sum.length; i++) sum[i] += s[i];
        }
        return sum;
    }

    /** "—" for 0, else one decimal with a trailing ".0" dropped. */
    static String statText(double value) {
        double r = Math.round(value * 10) / 10.0;
        if (r == 0) return "—";
        String s = String.format(Locale.ROOT, "%.1f", r);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    /** "Set: <name> N of 4" for the set with the most worn armour pieces (sheet order breaks ties), "" with none worn. */
    static String setProgress(GearWire.View v) {
        Spec.GearSetsRow best = null;
        int most = 0;
        for (var set : Spec.GEARSETS) {
            int n = 0;
            for (String slot : Gear.ARMOUR) {
                Gear.Piece p = wornIn(v, slot);
                if (p != null && Gear.item(p.item) != null && Gear.item(p.item).set().equals(set.id())) n++;
            }
            if (n > most) { most = n; best = set; }
        }
        return best == null ? "" : "Set: " + best.name() + " " + most + " of " + Gear.ARMOUR.size();
    }

    static String counter(GearWire.View v) { return v == null ? "" : v.pieces().size() + " of " + v.inventorySize(); }

    /** For each stat that differs from the piece worn in the item's slot, "▲ +X <stat> vs worn" or "▼ -X <stat> vs worn". */
    static List<String> compare(GearWire.View v, Gear.Piece p) {
        List<String> out = new ArrayList<>();
        if (!resolves(p) || worn(v, p.uid)) return out;
        Gear.Piece other = wornIn(v, Gear.item(p.item).slot());
        if (other == null) return List.of("Nothing worn there");
        double[] a = Gear.stats(p), b = Gear.stats(other);
        for (int i = 0; i < a.length; i++) {
            double d = Math.round((a[i] - b[i]) * 10) / 10.0;
            if (d > 0) out.add("▲ +" + statText(d) + " " + statWord(i) + " vs worn");
            else if (d < 0) out.add("▼ -" + statText(-d) + " " + statWord(i) + " vs worn");
        }
        return out;
    }

    /** The hover card's lines, top to bottom; empty for a piece the sheet no longer knows. */
    static List<String> tooltip(GearWire.View v, Gear.Piece p) {
        List<String> out = new ArrayList<>();
        if (!resolves(p)) return out;
        var it = Gear.item(p.item);
        var r = Gear.rarity(p.rarity);
        out.add(it.name());
        boolean weapon = it.slot().equals("main") || it.slot().equals("off");
        var set = Gear.set(it.set());
        out.add(weapon || set == null ? r.name() + " " + it.family() : r.name() + " " + SLOT_WORDS.get(it.slot()) + " · " + set.name() + " set");
        double[] s = Gear.stats(p);
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < s.length; i++) if (!statText(s[i]).equals("—")) parts.add("+" + statText(s[i]) + " " + statWord(i));
        if (!parts.isEmpty()) out.add(String.join(" · ", parts));
        out.addAll(compare(v, p));
        out.add(p.durability <= 0 ? "Broken: half stats until repaired" : "Durability " + p.durability * 100 / Math.max(1, Gear.forge().maxDurability()) + "%");
        if (v != null && Forge.duplicate(asProfile(v), p)) out.add("Duplicate");
        out.add(worn(v, p.uid) ? "Worn" : "Click: equip · Shift-click: break down");
        return out;
    }

    /** A breakdown that asks first: Epic and above. */
    static boolean confirmBreakdown(Gear.Piece p) { return p != null && Gear.rarityIndex(p.rarity) >= 3; }

    /** The relic slot a clicked relic goes to: the selected one, else the first empty, else 0. */
    static int relicTarget(GearWire.View v, int selected) {
        if (selected >= 0) return selected;
        if (v != null) for (int i = 0; i < v.relicSlots().size(); i++) if (v.relicSlots().get(i).isEmpty()) return i;
        return 0;
    }

    static int rows(int count) { return (count + COLUMNS - 1) / COLUMNS; }
    /** The grid's scroll offset in rows, kept so the last row is the lowest one shown. */
    static int scrollClamp(int offsetRows, int count, int visibleRows) {
        return Math.max(0, Math.min(offsetRows, rows(count) - visibleRows));
    }

    /** A throwaway Profile holding the view's gear fields, for Forge previews. Gold is not in the view and stays 0. */
    static Profile asProfile(GearWire.View v) {
        Profile p = new Profile();
        p.forgeMarks = v.forgeMarks();
        p.inventorySize = v.inventorySize();
        p.gearPity = v.pity();
        p.gearPower = v.power();
        p.equipped = new LinkedHashMap<>(v.equipped());
        p.relics = new LinkedHashSet<>(v.relics());
        p.relicSlots = new ArrayList<>(v.relicSlots());
        p.gear = new ArrayList<>();
        int next = 1;
        for (Gear.Piece g : v.pieces()) {
            p.gear.add(new Gear.Piece(g.uid, g.item, g.rarity, g.quality, g.durability));
            next = Math.max(next, g.uid + 1);
        }
        p.nextGearUid = next;
        return p;
    }

    /** Marks "Break down all duplicates" would give. */
    static int duplicatesValue(GearWire.View v) { return v == null ? 0 : Forge.breakdownDuplicates(asProfile(v)); }

    /** Gold "Repair all" would cost. */
    static int repairAllCost(GearWire.View v) {
        int sum = 0;
        if (v != null) for (Gear.Piece g : v.pieces()) if (Gear.rarity(g.rarity) != null) sum += Forge.repairCost(g);
        return sum;
    }

    /** The client's guess at why a roll would be refused, "" when it looks allowed. The server still decides. */
    static String rollRefusal(GearWire.View v, String slot) {
        if (v == null) return "Loading your gear…";
        int cost = Forge.rollCost(slot);
        if (v.forgeMarks() < cost) return "A roll costs " + cost + " Forge marks.";
        if ("relic".equals(slot)) {
            if (Arrays.stream(Spec.RELICS).allMatch(r -> v.relics().contains(r.id()))) return "You own every relic.";
            return "";
        }
        if (v.pieces().size() >= v.inventorySize()) return "Your inventory is full. Break down or expand first.";
        return "";
    }
}
