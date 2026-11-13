package gg.gravesandglory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/** The field as the server scanned it (pure data and codec, no Minecraft): for each block column of the playable field, the id of its top
 *  block (an index into {@link #palette}, -1 for none) and the height of that top in blocks. index = (z - minZ) * width + (x - minX).
 *  It is the only input of both map looks, and it travels once per map build on battlemap_grid_v1.
 *  Wire: a version byte, then one deflate stream of the header, the palette, and four byte planes (look high, look low, height delta
 *  high, low). The planes and the west-neighbour height delta are what makes a 541 x 381 field about a fifth of a megabyte at most. */
public final class BattleMapGrid {
    static final int VERSION = 1, MAX_SIDE = 4096, MAX_CELLS = 1 << 22;
    public final String map, build;
    public final int minX, minZ, width, depth;
    public final String[] palette;
    public final short[] look, height;

    public BattleMapGrid(String map, String build, int minX, int minZ, int width, int depth, String[] palette, short[] look, short[] height) {
        this.map = map; this.build = build; this.minX = minX; this.minZ = minZ; this.width = width; this.depth = depth;
        this.palette = palette; this.look = look; this.height = height;
    }

    /** Deflater, version byte 1. */
    public byte[] encode() {
        var out = new ByteArrayOutputStream();
        out.write(VERSION);
        try (var d = new DataOutputStream(new DeflaterOutputStream(out, new Deflater(Deflater.BEST_COMPRESSION)))) {
            d.writeUTF(map); d.writeUTF(build);
            d.writeInt(minX); d.writeInt(minZ); d.writeInt(width); d.writeInt(depth);
            d.writeShort(palette.length);
            for (String p : palette) d.writeUTF(p);
            int n = width * depth;
            for (int i = 0; i < n; i++) d.writeByte(look[i] >> 8);
            for (int i = 0; i < n; i++) d.writeByte(look[i]);
            short[] delta = new short[n];
            for (int i = 0; i < n; i++) delta[i] = (short) (height[i] - (i % width == 0 ? 0 : height[i - 1]));
            for (int i = 0; i < n; i++) d.writeByte(delta[i] >> 8);
            for (int i = 0; i < n; i++) d.writeByte(delta[i]);
        } catch (IOException e) { throw new IllegalStateException(e); }
        return out.toByteArray();
    }

    /** @throws IllegalArgumentException on a null, empty, truncated or inconsistent payload or a version other than 1 */
    public static BattleMapGrid decode(byte[] data) {
        if (data == null || data.length < 2 || data[0] != VERSION) throw new IllegalArgumentException("battle map grid: bad version");
        try (var d = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data, 1, data.length - 1)))) {
            String map = d.readUTF(), build = d.readUTF();
            int minX = d.readInt(), minZ = d.readInt(), w = d.readInt(), depth = d.readInt();
            if (w < 1 || depth < 1 || w > MAX_SIDE || depth > MAX_SIDE || (long) w * depth > MAX_CELLS) throw new IllegalArgumentException("battle map grid: bad size " + w + "x" + depth);
            String[] pal = new String[d.readUnsignedShort()];
            for (int i = 0; i < pal.length; i++) pal[i] = d.readUTF();
            int n = w * depth;
            byte[] hi = new byte[n], lo = new byte[n];
            d.readFully(hi); d.readFully(lo);
            short[] look = new short[n], height = new short[n];
            for (int i = 0; i < n; i++) {
                look[i] = (short) (hi[i] << 8 | lo[i] & 255);
                if (look[i] < -1 || look[i] >= pal.length) throw new IllegalArgumentException("battle map grid: look index out of the palette");
            }
            d.readFully(hi); d.readFully(lo);
            for (int i = 0; i < n; i++) height[i] = (short) ((hi[i] << 8 | lo[i] & 255) + (i % w == 0 ? 0 : height[i - 1]));
            if (d.read() >= 0) throw new IllegalArgumentException("battle map grid: trailing data");
            return new BattleMapGrid(map, build, minX, minZ, w, depth, pal, look, height);
        } catch (IOException e) { throw new IllegalArgumentException("battle map grid: truncated or corrupt (" + e.getMessage() + ")", e); }
    }
}
