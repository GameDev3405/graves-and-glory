package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Battle map field grid (Task 3): the wire codec round trips a full-size field under the size budget and refuses bad data, and the kinds
 *  table read from the real battlemap/kinds.json classifies blocks as the preview script does. Checked by javac alone, never shipped. */
public final class BattleMapGridCheck {
    static int checks;
    static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); checks++; }

    /** A deterministic hash of a cell, the same cheap integer mix the preview script uses. */
    static int hash(int x, int z) {
        long h = (x * 374761393L + z * 668265263L) & 0xFFFFFFFFL;
        h = ((h ^ (h >> 13)) * 1274126177L) & 0xFFFFFFFFL;
        return (int) ((h ^ (h >> 16)) & 0xFFFF);
    }

    public static void main(String[] args) throws Exception {
        { // roundTrip: 541x381, 40 palette entries, hashed in 6x5 patches like blocks and rooms, heights wandering over a few levels
            int w = 541, d = 381;
            String[] pal = new String[40];
            for (int i = 0; i < pal.length; i++) pal[i] = "minecraft:block_" + i;
            short[] look = new short[w * d], height = new short[w * d];
            for (int z = 0; z < d; z++) for (int x = 0; x < w; x++) {
                look[z * w + x] = (short) (hash(x / 6, z / 5) % 40);
                height[z * w + x] = (short) (60 + hash(x / 9, z / 9) % 12);
            }
            look[7] = -1;
            var g = new BattleMapGrid("kingsfield_cq", "sha-abc", -270, -190, w, d, pal, look, height);
            byte[] bytes = g.encode();
            check(bytes.length < 200000, "encoded under 200000 bytes, got " + bytes.length);
            var r = BattleMapGrid.decode(bytes);
            check(r.map.equals("kingsfield_cq") && r.build.equals("sha-abc"), "map and build");
            check(r.minX == -270 && r.minZ == -190 && r.width == w && r.depth == d, "bounds");
            check(Arrays.equals(r.palette, pal) && Arrays.equals(r.look, look) && Arrays.equals(r.height, height), "palette, look, height equal");
        }
        { // badVersion, truncated, and nonsense sizes all throw IllegalArgumentException, never anything else
            var g = new BattleMapGrid("m", "b", 0, 0, 2, 2, new String[] {"minecraft:stone"}, new short[4], new short[4]);
            byte[] ok = g.encode();
            byte[] bad = ok.clone();
            bad[0] = 9;
            try { BattleMapGrid.decode(bad); check(false, "bad version accepted"); } catch (IllegalArgumentException e) { check(true, "bad version"); }
            try { BattleMapGrid.decode(Arrays.copyOf(ok, ok.length - 4)); check(false, "truncated accepted"); } catch (IllegalArgumentException e) { check(true, "truncated"); }
            try { BattleMapGrid.decode(new byte[0]); check(false, "empty accepted"); } catch (IllegalArgumentException e) { check(true, "empty"); }
            try { BattleMapGrid.decode(null); check(false, "null accepted"); } catch (IllegalArgumentException e) { check(true, "null"); }
        }
        { // kindsTable: the real file
            var k = MapKinds.parse(Files.readString(Path.of("src/main/resources/assets/gravesandglory/battlemap/kinds.json")));
            String[] kinds = k.kinds();
            check(kinds.length == 8 && kinds[0].equals("ground") && kinds[1].equals("water") && kinds[7].equals("building"), "kinds list");
            check(k.raise() == 3, "raise 3");
            check(kinds[k.kind("minecraft:water", false)].equals("water") && kinds[k.kind("minecraft:water", true)].equals("water"), "water");
            check(kinds[k.kind("minecraft:oak_leaves", false)].equals("foliage"), "leaves are foliage");
            check(kinds[k.kind("minecraft:oak_stairs", true)].equals("building"), "raised stairs are a building");
            check(kinds[k.kind("minecraft:oak_stairs", false)].equals("road"), "stairs at ground level are paving");
            check(kinds[k.kind("minecraft:hay_block", false)].equals("crops") && kinds[k.kind("minecraft:hay_block", true)].equals("building"), "hay flat is crops");
            check(kinds[k.kind("minecraft:stripped_oak_log", true)].equals("building"), "prefix rule");
            check(kinds[k.kind("minecraft:barrel", false)].equals("ground"), "barrel flat is ground");
            check(kinds[k.kind("minecraft:grass_block", false)].equals("ground") && kinds[k.kind("minecraft:stone", false)].equals("rock"), "ground and rock");
            check(kinds[k.kind("minecraft:cobblestone", true)].equals("building"), "first match wins over the later rock rule for cobbled deepslate");
            check(kinds[k.kind("minecraft:cobbled_deepslate", true)].equals("building"), "first match wins");
            check(kinds[k.kind("mod:never_heard_of_it", true)].equals("ground") && kinds[k.kind("", false)].equals("ground"), "unknown id gives the default");
        }
        System.out.println("BattleMapGridCheck: " + checks + " checks passed");
    }
}
