package gg.gravesandglory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

/**
 * The one-time upgrades of a commander profile: schema 1 (Dawn and Dusk, 12 troops) to schema 2 (spec 6.3), and schema 2 to
 * schema 3 (plan 2C.3: the Long March battles won and bonuses paid). Pure: JSON goes through the parse and write functions the
 * caller passes, so the standalone ProfileMigrationCheck needs no Gson.
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
        p.schema = 2; // the schema-2 image; v2toV3 takes it to 3
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
     * Schema 2 to 3 (Phase 4 spec 12), in place, and returns the same profile: battlesWon gains every row whose order is at most
     * campaignCleared (a schema-2 profile could only clear battles in order); bonusClaimed starts empty, since no bonus existed before.
     * Every schema-2 field is kept as it is, the Pact count (writs) and writChapters included. Sets schema 3.
     */
    static Profile v2toV3(Profile p, Spec.CampaignBattlesRow[] rows) {
        if (p.battlesWon == null) p.battlesWon = new LinkedHashSet<>();
        if (p.bonusClaimed == null) p.bonusClaimed = new LinkedHashSet<>();
        for (var row : MarchMath.byOrder(rows)) if (row.order() <= p.campaignCleared) p.battlesWon.add(row.id());
        p.schema = 3;
        return p;
    }

    /**
     * Upgrades one profile file in place when it is below schema 3, and returns the new profile; returns null when the file is already
     * schema 3 or later. The file's schema is read first (parse: a file with no schema field reads 0, meaning 1). Schema 1 runs toV2 and
     * then v2toV3; schema 2 is read whole (parseV2) and runs v2toV3. The original bytes are kept once as <uuid>.v1.json or <uuid>.v2.json
     * by the schema they hold, and a schema-1 file also keeps its schema-2 image once as <uuid>.v2.json; an existing copy is never
     * replaced. The new file is written to <uuid>.tmp and moved over the original. A parse failure throws IllegalStateException and
     * touches no file. The caller sanitizes the result.
     */
    static Profile upgradeFile(Path file, Function<String, V1> parse, Function<String, Profile> parseV2, Function<Profile, String> write, RosterMath.Catalog c) throws IOException {
        byte[] original = Files.readAllBytes(file);
        String source = new String(original, StandardCharsets.UTF_8);
        V1 v1;
        Profile p;
        try {
            v1 = parse.apply(source);
            if (v1 == null) throw new IllegalStateException("Commander profile is empty; it was left unchanged: " + file.getFileName());
            if (v1.schema >= 3) return null;
            p = v1.schema >= 2 ? parseV2.apply(source) : null;
        } catch (IllegalStateException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Commander profile could not be read; it was left unchanged: " + file.getFileName(), e);
        }
        String name = file.getFileName().toString(), stem = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
        Path v1Backup = file.resolveSibling(stem + ".v1.json"), v2Backup = file.resolveSibling(stem + ".v2.json"), tmp = file.resolveSibling(stem + ".tmp");
        byte[] v2Bytes;
        if (v1.schema < 2) {
            p = toV2(v1, c);
            v2Bytes = write.apply(p).getBytes(StandardCharsets.UTF_8);
        } else {
            if (p == null) throw new IllegalStateException("Commander profile is empty; it was left unchanged: " + file.getFileName());
            v2Bytes = original;
        }
        String text = write.apply(v2toV3(p, Spec.CAMPAIGNBATTLES));
        if (v1.schema < 2 && !Files.exists(v1Backup)) Files.write(v1Backup, original, StandardOpenOption.CREATE_NEW);
        if (!Files.exists(v2Backup)) Files.write(v2Backup, v2Bytes, StandardOpenOption.CREATE_NEW);
        Files.writeString(tmp, text);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        return p;
    }
}
