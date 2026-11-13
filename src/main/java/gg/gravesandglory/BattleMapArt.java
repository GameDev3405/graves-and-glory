package gg.gravesandglory;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** The battle map's two textures (client): {@link #real} at one texel per block and {@link #parchment} at {@link BattleMapPaint#S},
 *  both painted by {@link BattleMapPaint} from {@link BattleMapClient#grid}. The real look's colour per palette block is the alpha-weighted
 *  mean of its top-face sprite (baked model, UP quads), tinted through BlockColors with no world (the default grass and foliage tint),
 *  falling back to the block's map colour. Painting runs off the client thread; the textures are registered on it, and the old pair is
 *  freed when a different map or build is asked for. Dev only: -Dgravesandglory.dumpMapArt=<dir> writes both PNGs there. */
public final class BattleMapArt {
    private BattleMapArt() {}
    static final Logger LOG = LoggerFactory.getLogger("gravesandglory");
    /** The default water colour (the same as tools/mapforge/colours.py); the water block's colour provider needs a world. */
    static final int WATER_TINT = 0x3F76E4;

    private static Identifier real, parchment;
    private static NativeImageBackedTexture realTex, parchmentTex;
    /** The map/build the textures show, and the one being painted (null when none). */
    private static String builtKey, pendingKey;
    private static int generation;
    private static MapKinds kinds;
    private static final Map<Identifier, float[]> spriteMeans = new HashMap<>();

    /** The real look's texture, or null until built. */
    public static Identifier real() { return real; }
    /** The parchment look's texture, or null until built. */
    public static Identifier parchment() { return parchment; }

    /** The map id the built textures show, or null. */
    public static String builtMap() { return builtKey == null ? null : builtKey.substring(0, builtKey.indexOf('/')); }
    /** {width, height} of the built real (true) or parchment texture, {0, 0} when none. */
    public static int[] size(boolean realLook) {
        var t = realLook ? realTex : parchmentTex;
        return t == null || t.getImage() == null ? new int[] {0, 0} : new int[] {t.getImage().getWidth(), t.getImage().getHeight()};
    }

    /** Paints both looks of this grid unless they are built or being built (client thread). A different map or build frees the old pair. */
    public static void request(BattleMapGrid g) {
        if (g == null) return;
        String key = g.map + "/" + g.build;
        if (key.equals(builtKey) || key.equals(pendingKey)) return;
        MinecraftClient c = MinecraftClient.getInstance();
        free(c);
        int gen = ++generation;
        pendingKey = key;
        int[] rgb;
        MapKinds table;
        try { rgb = colours(c, g.palette); table = kinds(c); }
        catch (RuntimeException e) { LOG.warn("battle map art for {} not built: {}", g.map, e.toString()); pendingKey = null; return; }
        String dump = System.getProperty("gravesandglory.dumpMapArt", "");
        CompletableFuture.supplyAsync(() -> {
            long t0 = System.nanoTime();
            int[] r = BattleMapPaint.real(g, rgb);
            long t1 = System.nanoTime();
            int[] p = BattleMapPaint.parchment(g, table);
            long t2 = System.nanoTime();
            NativeImage ri = image(r, g.width, g.depth), pi = image(p, g.width * BattleMapPaint.S, g.depth * BattleMapPaint.S);
            long t3 = System.nanoTime();
            LOG.info("battle map art for {} ({}x{} blocks, parchment {}x{}) painted in {} ms: real {} ms, parchment {} ms, images {} ms", g.map, g.width, g.depth,
                pi.getWidth(), pi.getHeight(), (t3 - t0) / 1_000_000, (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000, (t3 - t2) / 1_000_000);
            if (!dump.isBlank()) {
                try { write(Path.of(dump), g, ri, pi, rgb); }
                catch (Exception e) { LOG.warn("battle map art dump to {} failed: {}", dump, e.toString()); }
            }
            return new NativeImage[] {ri, pi};
        }).thenAccept(images -> c.execute(() -> {
            if (gen != generation) { images[0].close(); images[1].close(); return; }
            realTex = new NativeImageBackedTexture(images[0]);
            parchmentTex = new NativeImageBackedTexture(images[1]);
            real = c.getTextureManager().registerDynamicTexture("gravesandglory_battlemap_real", realTex);
            parchment = c.getTextureManager().registerDynamicTexture("gravesandglory_battlemap_parchment", parchmentTex);
            builtKey = key;
            pendingKey = null;
        })).exceptionally(e -> {
            LOG.warn("battle map art for {} failed: {}", g.map, e.toString());
            c.execute(() -> { if (gen == generation) pendingKey = null; });
            return null;
        });
    }

    /** Frees both textures and forgets the map (client thread). */
    public static void clear() {
        free(MinecraftClient.getInstance());
        generation++;
        pendingKey = null;
    }

    private static void free(MinecraftClient c) {
        if (real != null) c.getTextureManager().destroyTexture(real);
        if (parchment != null) c.getTextureManager().destroyTexture(parchment);
        real = parchment = null;
        realTex = parchmentTex = null;
        builtKey = null;
    }

    /** Writes the built pair as <map>-real.png and <map>-parchment.png in dir (client thread). False when nothing is built yet. */
    public static boolean dump(Path dir) throws java.io.IOException {
        if (realTex == null || parchmentTex == null || realTex.getImage() == null || parchmentTex.getImage() == null) return false;
        String map = builtKey.substring(0, builtKey.indexOf('/'));
        Files.createDirectories(dir);
        realTex.getImage().writeTo(dir.resolve(map + "-real.png"));
        parchmentTex.getImage().writeTo(dir.resolve(map + "-parchment.png"));
        return true;
    }

    private static void write(Path dir, BattleMapGrid g, NativeImage ri, NativeImage pi, int[] rgb) throws java.io.IOException {
        Files.createDirectories(dir);
        ri.writeTo(dir.resolve(g.map + "-real.png"));
        pi.writeTo(dir.resolve(g.map + "-parchment.png"));
        Files.write(dir.resolve(g.map + "-grid.bin"), g.encode());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < g.palette.length; i++) sb.append(g.palette[i]).append(' ').append(rgb[i]).append('\n');
        Files.writeString(dir.resolve(g.map + "-grid.bin.rgb"), sb.toString());
    }

    /** ARGB pixels to a NativeImage (which stores ABGR). */
    private static NativeImage image(int[] argb, int w, int h) {
        NativeImage im = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = argb[y * w + x];
            im.setColor(x, y, (c & 0xff00ff00) | (c >> 16 & 255) | (c & 255) << 16);
        }
        return im;
    }

    private static MapKinds kinds(MinecraftClient c) {
        if (kinds == null) {
            var res = c.getResourceManager().getResource(new Identifier("gravesandglory", "battlemap/kinds.json"))
                .orElseThrow(() -> new IllegalStateException("battlemap/kinds.json missing"));
            try (InputStream in = res.getInputStream()) { kinds = MapKinds.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)); }
            catch (java.io.IOException e) { throw new IllegalStateException(e); }
        }
        return kinds;
    }

    // ---------------------------------------------------------------- real-look colours

    /** The top colour (0xRRGGBB) of every palette block id (client thread: reads baked models and block colours). */
    static int[] colours(MinecraftClient c, String[] palette) {
        int[] out = new int[palette.length];
        Random rnd = Random.create(42L);
        for (int i = 0; i < palette.length; i++) {
            Identifier id = Identifier.tryParse(palette[i]);
            var block = id == null ? null : Registries.BLOCK.getOrEmpty(id).orElse(null);
            if (block == null) { out[i] = 0x808080; continue; }
            BlockState s = block.getDefaultState();
            float[] sum = new float[4];
            if (palette[i].equals("minecraft:water")) add(c, sum, new Identifier("minecraft", "block/water_still"), WATER_TINT);
            else if (palette[i].equals("minecraft:lava")) add(c, sum, new Identifier("minecraft", "block/lava_still"), -1);
            else {
                BakedModel m = c.getBlockRenderManager().getModel(s);
                List<BakedQuad> quads = new ArrayList<>(m.getQuads(s, Direction.UP, rnd));
                for (BakedQuad q : m.getQuads(s, null, rnd)) if (q.getFace() == Direction.UP) quads.add(q);
                for (BakedQuad q : quads) {
                    int tint = -1;
                    if (q.hasColor()) tint = c.getBlockColors().getColor(s, null, null, q.getColorIndex());
                    add(c, sum, q.getSprite().getContents().getId(), tint);
                }
            }
            if (sum[3] > 0) out[i] = Math.round(sum[0] / sum[3]) << 16 | Math.round(sum[1] / sum[3]) << 8 | Math.round(sum[2] / sum[3]);
            else {
                int mc = s.getMapColor(null, null).color;
                out[i] = mc != 0 ? mc : 0x808080;
            }
        }
        return out;
    }

    /** Adds a sprite's alpha-weighted mean, tinted (0xRRGGBB, -1 none), to sum {r*a, g*a, b*a, a}. */
    private static void add(MinecraftClient c, float[] sum, Identifier sprite, int tint) {
        float[] m = spriteMeans.computeIfAbsent(sprite, id -> mean(c, id));
        if (m == null || m[3] <= 0) return;
        float tr = tint == -1 ? 1 : (tint >> 16 & 255) / 255f, tg = tint == -1 ? 1 : (tint >> 8 & 255) / 255f, tb = tint == -1 ? 1 : (tint & 255) / 255f;
        sum[0] += m[0] * tr * m[3]; sum[1] += m[1] * tg * m[3]; sum[2] += m[2] * tb * m[3]; sum[3] += m[3];
    }

    /** {mean r, g, b weighted by alpha, total alpha} of a block sprite's first frame, read from its texture file. */
    private static float[] mean(MinecraftClient c, Identifier sprite) {
        var res = c.getResourceManager().getResource(new Identifier(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png"));
        if (res.isEmpty()) return null;
        try (InputStream in = res.get().getInputStream(); NativeImage im = NativeImage.read(in)) {
            int w = im.getWidth(), h = Math.min(im.getHeight(), w); // an animated strip: its first square frame
            double r = 0, g = 0, b = 0, a = 0;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int p = im.getColor(x, y); // ABGR
                double al = (p >>> 24) / 255.0;
                r += (p & 255) * al; g += (p >> 8 & 255) * al; b += (p >> 16 & 255) * al; a += al;
            }
            return a <= 0 ? null : new float[] {(float) (r / a), (float) (g / a), (float) (b / a), (float) (a / (w * h))};
        } catch (Exception e) { return null; }
    }
}
