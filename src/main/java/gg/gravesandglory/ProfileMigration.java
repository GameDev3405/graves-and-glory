package gg.gravesandglory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

/**
 * The one-time upgrade of a schema-1 commander profile (Dawn and Dusk, 12 troops) to schema 2 (spec 6.3). Pure: JSON goes
 * through the parse and write functions the caller passes, so the standalone ProfileMigrationCheck needs no Gson.
 */
public final class ProfileMigration {
    private ProfileMigration() {}

    /** A schema-1 profile as Campaign.Profile saved it. Those files carry no schema field, so it reads 0, meaning 1. */
    static final class V1 {
        int schema, gold, matches, wins;
        String commander;
        boolean tutorialDone;
        Set<String> unlocked;
        Map<String, Integer> army;
        Map<String, List<String>> deployment;
        Progression.State progression;
        int campaignCleared;
    }

    /** Spec 6.3: each v1 troop id to its v2 id; oracle merges into mage and wraith into hexer. */
    static final Map<String, String> ID_MAP = ordered("knight", "knight", "archer", "archer", "mage", "mage", "sentinel", "sentinel", "longbow", "longbow", "oracle", "mage",
        "revenant", "revenant", "bonebow", "bonebow", "hexer", "hexer", "reaper", "reaper", "stalker", "stalker", "wraith", "hexer");
    /** Spec 5: v1 commander classes to v2 ones; any other class becomes champion. */
    static final Map<String, String> CLASS_MAP = ordered("vanguard", "champion", "marksman", "warden", "arcanist", "warlock");
    /** v1 ids refunded at economy.refundGold instead of granted (Kort, 2026-10-17): the two casters that became Epics and the two troops merged into them. */
    static final Set<String> REFUNDED = Set.of("mage", "hexer", "oracle", "wraith");
    /** The v1 starters: every v1 profile had them, whether or not its file lists them. */
    static final List<String> V1_STARTERS = List.of("knight", "archer", "mage", "revenant", "bonebow", "hexer");
    /** The troop ids each v1 deployment line could hold. */
    static final Map<String, Set<String>> V1_SIDES = Map.of(
        "dawn", Set.of("knight", "archer", "mage", "sentinel", "longbow", "oracle"),
        "dusk", Set.of("revenant", "bonebow", "hexer", "reaper", "stalker", "wraith"));
    /** Entries in a v1 deployment line. */
    static final int V1_LINE = 20;

    static Map<String, String> ordered(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put(pairs[i], pairs[i + 1]);
        return Collections.unmodifiableMap(map);
    }

    /**
     * The schema-2 profile for a v1 one (spec 6.3). Every Common of every House in the catalog is granted, plus each v1
     * unlock that maps to a troop. mage, hexer, oracle and wraith are refunded at economy.refundGold each instead of being
     * granted (mage and hexer always, as v1 starters). Gold, matches, wins, the tutorial flag, campaignCleared and the
     * Founders progression are copied; the class maps through CLASS_MAP; the banner comes from banner(); shards, renown
     * and pity start at 0 for every House.
     */
    static Profile toV2(V1 v1, RosterMath.Catalog c) {
        Profile p = new Profile();
        p.gold = v1.gold;
        p.matches = v1.matches;
        p.wins = v1.wins;
        p.tutorialDone = v1.tutorialDone;
        p.campaignCleared = v1.campaignCleared;
        p.commander = CLASS_MAP.getOrDefault(v1.commander, "champion");
        if (v1.progression != null) p.progression = v1.progression;
        for (var e : c.rarity().entrySet()) if ("common".equals(e.getValue())) p.owned.put(e.getKey(), new Profile.Owned());
        Set<String> had = new LinkedHashSet<>(V1_STARTERS);
        if (v1.unlocked != null) had.addAll(v1.unlocked);
        for (String id : had) {
            if (id == null) continue;
            if (REFUNDED.contains(id)) p.gold += Spec.ECONOMY[0].refundGold();
            else if (c.known(ID_MAP.get(id))) p.owned.putIfAbsent(ID_MAP.get(id), new Profile.Owned());
        }
        p.banner = banner(v1, c, p.owned.keySet());
        for (var h : Spec.HOUSES) {
            p.shards.put(h.id(), 0);
            p.renown.put(h.id(), 0);
            p.pity.put(h.id(), 0);
        }
        return p;
    }

    /**
     * The v2 banner: the v1 Dawn line if it is a valid v1 line (V1_LINE entries of Dawn ids or empty, with at least one
     * troop), else the Dusk line on the same terms, else starterBanner(). Ids map through ID_MAP, and a line that then
     * breaks a banner rule (an unowned Epic, a missing ^) is repacked.
     */
    static List<String> banner(V1 v1, RosterMath.Catalog c, Set<String> owned) {
        List<String> line = null;
        for (String side : List.of("dawn", "dusk")) {
            List<String> candidate = v1.deployment == null ? null : v1.deployment.get(side);
            if (validLine(candidate, V1_SIDES.get(side))) { line = candidate; break; }
        }
        List<String> mapped = new ArrayList<>();
        if (line == null) mapped = RosterMath.starterBanner();
        else for (String id : line) mapped.add(RosterMath.EMPTY.equals(id) ? RosterMath.EMPTY : ID_MAP.get(id));
        return RosterMath.validBanner(mapped, c, owned) == null ? mapped : RosterMath.repack(mapped, c, owned);
    }
    static boolean validLine(List<String> line, Set<String> side) {
        if (line == null || line.size() != V1_LINE) return false;
        boolean troop = false;
        for (String id : line) {
            if (RosterMath.EMPTY.equals(id)) continue;
            if (id == null || !side.contains(id)) return false;
            troop = true;
        }
        return troop;
    }

    /**
     * Upgrades one profile file in place when it is schema 1, and returns the new profile; returns null when the file is
     * already schema 2 or later. The original bytes are kept once as <uuid>.v1.json (an existing copy is never replaced),
     * then the new file is written to <uuid>.tmp and moved over the original. A parse failure throws
     * IllegalStateException and touches no file.
     */
    static Profile upgradeFile(Path file, Function<String, V1> parse, Function<Profile, String> write, RosterMath.Catalog c) throws IOException {
        byte[] original = Files.readAllBytes(file);
        V1 v1;
        try {
            v1 = parse.apply(new String(original, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Commander profile could not be read; it was left unchanged: " + file.getFileName(), e);
        }
        if (v1 == null) throw new IllegalStateException("Commander profile is empty; it was left unchanged: " + file.getFileName());
        if (v1.schema >= 2) return null;
        Profile p = toV2(v1, c);
        String text = write.apply(p);
        String name = file.getFileName().toString(), stem = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
        Path backup = file.resolveSibling(stem + ".v1.json"), tmp = file.resolveSibling(stem + ".tmp");
        if (!Files.exists(backup)) Files.write(backup, original, StandardOpenOption.CREATE_NEW);
        Files.writeString(tmp, text);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        return p;
    }
}
