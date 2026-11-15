package gg.gravesandglory;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/** The battle plan in the world (battle map spec 5.4): each planned point carries a billboarded flag 6 blocks above it, the battle map's
 *  plan flag (a pole and a parchment cloth, {@link BattleMapScreen#flag}) with the job's glyph and the point's letter. A direct order this
 *  player gave shows a smaller flag at its target, its cloth to the left of the pole so it never covers the point's own flag. Plans come
 *  only from {@link BattleMapClient#state}, which the server sends to the player's own team, so nobody sees the other side's plan.
 *  Flags keep a fixed size on screen past about 18 blocks, so they stay readable from 100 blocks and more. Each is drawn twice: faintly
 *  through terrain, then whole where it is in view. An {@code AFTER_ENTITIES} world-render listener, as the order outlines are. */
public final class PlanMarkers {
    private PlanMarkers() {}

    /** Blocks above the point's ground for a point flag's foot, and for a direct order's flag at a place. */
    static final double POINT_LIFT = 6, PLACE_LIFT = 4;
    /** Blocks per flag pixel: at least NEAR, and DIST per block of distance beyond that (a fixed screen size). Small flags take SMALL of it. */
    static final double NEAR = .08, DIST = .0065, SMALL = .65;
    /** Flag pixels: the pole below the cloth, the cloth's height and its inner padding. */
    static final int POLE = 6, CLOTH = 11, PAD = 2;
    static final int FAINT = 0x99;

    private static int drawn, counting;
    private static boolean[] drawnPoints = new boolean[0], countingPoints = new boolean[0];

    /** How many flags the last frame drew, order flags included (read by the checks). */
    public static int drawn() { return drawn; }
    /** Whether the last frame drew capture point i's own plan flag (read by the checks). */
    public static boolean pointDrawn(int i) { return i >= 0 && i < drawnPoints.length && drawnPoints[i]; }
    /** How many point flags the last frame drew. */
    public static int pointsDrawn() { int n = 0; for (boolean b : drawnPoints) if (b) n++; return n; }

    static void render(WorldRenderContext context) {
        counting = 0;
        countingPoints = new boolean[Spec.POINTS.length];
        try { draw(context); } finally { drawn = counting; drawnPoints = countingPoints; }
    }

    private static void draw(WorldRenderContext context) {
        var s = BattleMapClient.current(); // null outside a running round
        var c = MinecraftClient.getInstance();
        if (s == null || c.player == null || context.consumers() == null) return;
        var pts = Spec.POINTS;
        var cam = context.camera().getPos();
        var matrices = context.matrixStack();
        var consumers = context.consumers();
        boolean[] planned = new boolean[pts.length];
        for (var p : s.points()) {
            if (p.point() < 0 || p.point() >= pts.length || p.job() < 0) continue;
            planned[p.point()] = true;
            var pt = pts[p.point()];
            double x = pt.x() + .5, z = pt.z() + .5;
            flag(c, matrices, consumers, cam, x, ground(pt, x, z) + POINT_LIFT, z, p.job(), pt.id(), false, false);
            countingPoints[p.point()] = true;
        }
        String me = c.player.getGameProfile().getName();
        for (var o : s.orders()) {
            if (!me.equals(o.setter()) || o.job() < 0 || o.job() == BattlePlan.Job.FOLLOW.ordinal()) continue; // Follow's target is the player
            if (o.point() >= 0 && o.point() < pts.length) {
                var pt = pts[o.point()];
                double x = pt.x() + .5, z = pt.z() + .5;
                flag(c, matrices, consumers, cam, x, ground(pt, x, z) + POINT_LIFT, z, o.job(), pt.id(), true, planned[o.point()]);
            } else {
                double y = Arena.footing(net.minecraft.util.math.MathHelper.floor(o.x()), net.minecraft.util.math.MathHelper.floor(o.z())) + PLACE_LIFT;
                flag(c, matrices, consumers, cam, o.x(), y, o.z(), o.job(), "", true, false);
            }
        }
    }

    /** The point's ground: the scanned or engine ground at its centre, never below the point's own elevation. */
    static double ground(Spec.PointsRow p, double x, double z) {
        return Math.max(p.elevation(), Arena.footing(net.minecraft.util.math.MathHelper.floor(x), net.minecraft.util.math.MathHelper.floor(z)));
    }

    /** One flag with its pole's foot at (x, y, z), facing the camera. letter "" draws the glyph alone; left puts the cloth left of the pole. */
    static void flag(MinecraftClient c, MatrixStack matrices, VertexConsumerProvider consumers, net.minecraft.util.math.Vec3d cam,
                     double x, double y, double z, int job, String letter, boolean small, boolean left) {
        double dist = Math.sqrt((x - cam.x) * (x - cam.x) + (y - cam.y) * (y - cam.y) + (z - cam.z) * (z - cam.z));
        float unit = (float) (Math.max(NEAR, dist * DIST) * (small ? SMALL : 1));
        TextRenderer font = c.textRenderer;
        int lw = letter.isEmpty() ? 0 : font.getWidth(letter);
        int w = PAD + 7 + PAD + (lw > 0 ? lw + PAD : 0); // the cloth's inside
        matrices.push();
        matrices.translate(x - cam.x, y - cam.y, z - cam.z);
        matrices.multiply(c.gameRenderer.getCamera().getRotation());
        matrices.scale(unit, -unit, -unit); // as a name tag: x right and y down on screen, in flag pixels (1.21's camera rotation is 1.20.1's turned half about y: this is 1.20.1's (-unit, -unit, unit) exactly)
        Matrix4f m = matrices.peek().getPositionMatrix();
        // the cloth's outer box (ink edge included), left of the pole or right of it
        int top = -POLE - CLOTH - 2, x0 = left ? -w - 2 : 1, x1 = left ? 0 : w + 3;
        for (int pass = 0; pass < 2; pass++) {
            boolean faint = pass == 0;
            int a = faint ? FAINT : 0xff;
            VertexConsumer bg = consumers.getBuffer(faint ? RenderLayer.getTextBackgroundSeeThrough() : RenderLayer.getTextBackground());
            int ink = (a << 24) | (BattleMapScreen.INK & 0xffffff), parch = (a << 24) | (BattleMapScreen.PARCH & 0xffffff);
            rect(bg, m, 0, top, 1, 0, .03f, ink);                       // the pole
            rect(bg, m, x0, top, x1, top + CLOTH + 2, .02f, ink);       // the cloth's ink edge
            rect(bg, m, x0 + 1, top + 1, x1 - 1, top + CLOTH + 1, .01f, parch);
            int gx = x0 + 1 + PAD, gy = top + 1 + (CLOTH - 7) / 2;
            String[] rows = BattleMapScreen.jobGlyph(job);
            for (int j = 0; j < rows.length; j++) for (int i = 0; i < rows[j].length(); i++)
                if (rows[j].charAt(i) == 'x') rect(bg, m, gx + i, gy + j, gx + i + 1, gy + j + 1, 0, ink);
            if (lw > 0) font.draw(letter, gx + 7 + PAD, top + 1 + (CLOTH - 7) / 2f, ink, false, m, consumers,
                    faint ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
        matrices.pop();
        counting++;
    }


    /** A flat box from (x0, y0) to (x1, y1) at depth z (behind the text as z grows), both faces so the mirror of the billboard never culls it. */
    static void rect(VertexConsumer v, Matrix4f m, float x0, float y0, float x1, float y1, float z, int argb) {
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255, light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        v.vertex(m, x0, y0, z).color(r, g, b, a).light(light);
        v.vertex(m, x0, y1, z).color(r, g, b, a).light(light);
        v.vertex(m, x1, y1, z).color(r, g, b, a).light(light);
        v.vertex(m, x1, y0, z).color(r, g, b, a).light(light);
        v.vertex(m, x1, y0, z).color(r, g, b, a).light(light);
        v.vertex(m, x1, y1, z).color(r, g, b, a).light(light);
        v.vertex(m, x0, y1, z).color(r, g, b, a).light(light);
        v.vertex(m, x0, y0, z).color(r, g, b, a).light(light);
    }
}
