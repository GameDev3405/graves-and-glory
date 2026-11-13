package gg.gravesandglory;

/** The battle map's view transform (pure, no Minecraft): where the field sits in the map area of the screen, at what zoom. The field
 *  spans world x in [-halfX, halfX+1] and z in [-halfZ, halfZ+1] (the block cells of Field), north up. The scale runs from the fit scale
 *  (the whole field inside the area with a margin) to {@link #MAX_SCALE} screen px per block. After every change the field is clamped:
 *  on an axis where it is larger than the area it never leaves a gap at an edge, where it is smaller it stays centred. A view that was
 *  never moved refits when the area changes; a moved one keeps the world point at the area's centre, so a window resize does not jump. */
public final class MapView {
    public static final double MAX_SCALE = 8, MARGIN = 8;
    private final double fw, fd, hx, hz;
    private double rx, ry, rw, rh, scale, ox, oy;
    private boolean moved;

    public MapView(int halfX, int halfZ) {
        hx = halfX; hz = halfZ;
        fw = 2 * halfX + 1; fd = 2 * halfZ + 1;
    }

    /** Sets the screen rect of the map area: refits when never moved, else re-clamps around the same centre point. */
    public void layout(int x, int y, int w, int h) {
        double cwx = worldX(rx + rw / 2), cwz = worldZ(ry + rh / 2);
        rx = x; ry = y; rw = w; rh = h;
        if (!moved) { fit(); return; }
        scale = Math.max(fitScale(), Math.min(MAX_SCALE, scale));
        ox = rx + rw / 2 - (cwx + hx) * scale;
        oy = ry + rh / 2 - (cwz + hz) * scale;
        clamp();
    }

    /** The whole field inside the rect with an 8 px margin, centred. */
    public void fit() {
        scale = fitScale();
        moved = false;
        clamp();
    }

    public double scale() { return scale; }

    /** The smallest zoom: the field fits both axes with the margin (never below a sliver, so a tiny rect cannot divide by zero). */
    public double fitScale() { return Math.max(0.01, Math.min((rw - 2 * MARGIN) / fw, (rh - 2 * MARGIN) / fd)); }

    /** scale *= 1.25^steps within [fit, 8], keeping the world point under (sx, sy) where it is. */
    public void zoomAt(double sx, double sy, double steps) {
        double wx = worldX(sx), wz = worldZ(sy);
        scale = Math.max(fitScale(), Math.min(MAX_SCALE, scale * Math.pow(1.25, steps)));
        ox = sx - (wx + hx) * scale;
        oy = sy - (wz + hz) * scale;
        moved = true;
        clamp();
    }

    public void pan(double dxScreen, double dyScreen) {
        double x0 = ox, y0 = oy;
        ox += dxScreen; oy += dyScreen;
        clamp();
        if (ox != x0 || oy != y0) moved = true;
    }

    private void clamp() {
        ox = axis(ox, rx, rw, fw * scale);
        oy = axis(oy, ry, rh, fd * scale);
    }

    private static double axis(double o, double r0, double len, double content) {
        if (content <= len) return r0 + (len - content) / 2;
        return Math.max(r0 + len - content, Math.min(r0, o));
    }

    public double worldX(double sx) { return (sx - ox) / scale - hx; }
    public double worldZ(double sy) { return (sy - oy) / scale - hz; }
    public double screenX(double wx) { return ox + (wx + hx) * scale; }
    public double screenY(double wz) { return oy + (wz + hz) * scale; }

    /** True once the player has zoomed or panned (so a resize keeps their view instead of refitting). */
    public boolean moved() { return moved; }
}
