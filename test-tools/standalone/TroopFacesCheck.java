package gg.gravesandglory;

import java.util.*;

/**
 * The troop faces' pure rules (texel-3 faces design F2, task-5d-faces-design.md): which of a troop's face variants an entity
 * wears (a splitmix64 hash of its UUID, floor-modded by the face count), what a card, a pinned troop and the harness's forced
 * variant show instead, and which face bones a variant shows. javac-only: no Minecraft, never shipped.
 *
 *   javac --release 21 -d build/faces-checks src/main/java/gg/gravesandglory/TroopFaces.java test-tools/standalone/TroopFacesCheck.java
 *   java -cp build/faces-checks gg.gravesandglory.TroopFacesCheck
 */
public final class TroopFacesCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }

    /** (uuid, expected variant for count 2, 3, 4 and 5), worked out with an independent Python splitmix64: a refactor must not reshuffle anyone's face. */
    static final String[][] GOLDEN = {
        {"00000000-0000-0000-0000-000000000000", "1", "2", "3", "4"},
        {"ffffffff-ffff-ffff-ffff-ffffffffffff", "0", "2", "2", "2"},
        {"80000000-0000-0000-8000-000000000000", "0", "1", "0", "4"},
        {"123e4567-e89b-12d3-a456-426614174000", "1", "0", "3", "4"},
        {"f47ac10b-58cc-4372-a567-0e02b2c3d479", "0", "1", "0", "1"},
        {"0d8e1a2b-3c4d-4e5f-8a9b-0c1d2e3f4a5b", "1", "2", "1", "2"},
        {"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11", "1", "1", "1", "4"},
        {"6ba7b810-9dad-11d1-80b4-00c04fd430c8", "1", "2", "1", "1"},
        {"7d444840-9dc0-11d1-b245-5ffdce74fad2", "0", "2", "0", "1"},
        {"c1a5a5a5-0001-4002-8003-000000000004", "0", "0", "2", "4"},
        {"00000000-0000-4000-8000-000000000001", "1", "0", "1", "2"},
        {"00000000-0000-4000-8000-000000000002", "1", "1", "3", "0"},
        {"00000000-0000-4000-8000-000000000003", "0", "0", "2", "3"},
        {"00000000-0000-4000-8000-000000000004", "0", "2", "0", "0"},
    };

    public static void main(String[] args) {
        golden();
        range();
        evenness();
        pick();
        shows();
        System.out.println("TroopFacesCheck: " + checks + " checks pass");
    }

    /** The variant of each fixed UUID is the table's, for counts 2 to 5, however often it is asked and however the UUID object is made. */
    static void golden() {
        for (String[] row : GOLDEN) {
            for (int c = 2; c <= 5; c++) {
                int want = Integer.parseInt(row[c - 1]);
                UUID id = UUID.fromString(row[0]);
                check(TroopFaces.variant(id, c) == want, row[0] + " with " + c + " faces is " + TroopFaces.variant(id, c) + ", not " + want);
                check(TroopFaces.variant(new UUID(id.getMostSignificantBits(), id.getLeastSignificantBits()), c) == want, "a rebuilt UUID " + row[0] + " changed its face");
            }
        }
        UUID one = UUID.fromString(GOLDEN[3][0]);
        int first = TroopFaces.variant(one, 4);
        for (int i = 0; i < 1000; i++) check(TroopFaces.variant(one, 4) == first, "the variant of one UUID changed between calls");
    }

    /** Every variant lies in [0, count) for counts 1 to 8 over 20000 random UUIDs and the extreme words; a count of 1 or less is always 0; each variant of the count is reached. */
    static void range() {
        Random rng = new Random(7);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 20000; i++) ids.add(new UUID(rng.nextLong(), rng.nextLong()));
        long[] words = {0L, -1L, 1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x8000000000000001L};
        for (long a : words) for (long b : words) ids.add(new UUID(a, b));
        for (int count = 1; count <= 8; count++) {
            boolean[] seen = new boolean[count];
            for (UUID id : ids) {
                int v = TroopFaces.variant(id, count);
                check(v >= 0 && v < count, id + " with " + count + " faces is " + v + ", outside 0 to " + (count - 1));
                seen[v] = true;
            }
            for (int k = 0; k < count; k++) check(seen[k], "no UUID of 20000 wears face " + k + " of " + count);
        }
        for (int count = -2; count <= 1; count++) for (UUID id : ids.subList(0, 50)) check(TroopFaces.variant(id, count) == 0, "a count of " + count + " must give 0");
    }

    /** Four faces share 40000 UUIDs evenly (each within 5% of a quarter, about 6 sigma), and sequential UUIDs do not walk the faces in order. */
    static void evenness() {
        Random rng = new Random(11);
        int[] n = new int[4];
        for (int i = 0; i < 40000; i++) n[TroopFaces.variant(new UUID(rng.nextLong(), rng.nextLong()), 4)]++;
        for (int k = 0; k < 4; k++) check(Math.abs(n[k] - 10000) <= 500, "face " + k + " of 4 has " + n[k] + " of 40000 UUIDs");
        int same = 0, ascending = 0;
        int prev = TroopFaces.variant(new UUID(0, 1), 4);
        for (long i = 2; i <= 2000; i++) {
            int v = TroopFaces.variant(new UUID(0, i), 4);
            if (v == prev) same++;
            if (v == (prev + 1) % 4) ascending++;
            prev = v;
        }
        check(same > 300 && same < 700, "sequential UUIDs repeat the previous face " + same + " times of 1999 (about a quarter expected)");
        check(ascending > 300 && ascending < 700, "sequential UUIDs step to the next face " + ascending + " times of 1999 (about a quarter expected)");
    }

    /** What a draw wears: the harness's forced face first, then a card portrait's fixed face, then a pinned troop's, else the UUID's. */
    static void pick() {
        UUID id = UUID.fromString(GOLDEN[3][0]);   // wears 3 of 4 by hash
        check(TroopFaces.pick(id, 4, -1, 0, false, -1) == 3, "a world troop wears its UUID's face");
        check(TroopFaces.pick(id, 4, -1, 0, true, -1) == 0, "a card portrait wears the card face, not its random UUID's");
        check(TroopFaces.pick(id, 4, -1, 2, true, -1) == 2, "a card portrait wears face 2 when the card face is 2");
        check(TroopFaces.pick(id, 4, 1, 1, false, -1) == 1, "a pinned troop keeps its pinned face whatever its UUID");
        check(TroopFaces.pick(UUID.fromString(GOLDEN[0][0]), 4, 1, 1, false, -1) == 1, "a pinned troop keeps its pinned face for another UUID too");
        check(TroopFaces.pick(id, 4, 1, 2, true, -1) == 2, "a card shows the card face, even for a pinned troop");
        check(TroopFaces.pick(id, 4, 1, 1, false, 2) == 2, "the harness's forced face beats a pinned face");
        check(TroopFaces.pick(id, 4, -1, 0, true, 3) == 3, "the harness's forced face beats the card face");
        check(TroopFaces.pick(id, 4, -1, 0, false, 6) == 2, "a forced face past the count wraps");
        check(TroopFaces.pick(id, 1, -1, 0, false, 3) == 0, "a troop with one face (or none) always wears face 0");
        check(TroopFaces.pick(id, 0, 5, 5, true, 9) == 0, "a troop with no faces always wears face 0");
        for (int v = -3; v < 12; v++) {
            int got = TroopFaces.pick(id, 4, v, v, v % 2 == 0, -1);
            check(got >= 0 && got < 4, "pick with fixed and card " + v + " gave " + got);
            got = TroopFaces.pick(id, 4, -1, 0, false, v);
            check(got >= 0 && got < 4, "a forced face " + v + " gave " + got);
        }
    }

    /** Four face bones carry the variant sets {0}, {1}, {2} and {3}: whatever variant a draw asks (even -1, 4, MAX or MIN), exactly one shows; a bone with no variants always shows. */
    static void shows() {
        List<Set<Integer>> bones = List.of(Set.of(0), Set.of(1), Set.of(2), Set.of(3));
        int[] asked = {-1, 0, 1, 2, 3, 4, 5, 99, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int v : asked) {
            int shown = 0, which = -1;
            for (int k = 0; k < 4; k++) if (TroopFaces.shows(bones.get(k), v, 4)) { shown++; which = k; }
            check(shown == 1, "variant " + v + " of 4 shows " + shown + " face bones, not one");
            if (v >= 0 && v < 4) check(which == v, "variant " + v + " shows face bone " + which);
        }
        for (int v : asked) check(TroopFaces.shows(Set.of(), v, 4), "a bone with no variants shows at variant " + v);
        for (int v : asked) check(TroopFaces.shows(Set.of(), v, 0), "a bone with no variants shows for a troop with no faces");
    }
}
