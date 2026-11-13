package gg.gravesandglory;

/** Battle map view transform (Task 2): fit, cursor-anchored zoom, clamped pan and resize, and the screen/world round trip.
 *  Checked by javac alone, never shipped. */
public final class MapViewCheck {
    static int checks;
    static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); checks++; }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }

    /** No field edge may leave a gap inside the rect when the field is larger than the rect on that axis; a smaller axis stays inside. */
    static void clamped(MapView v, int x, int y, int w, int h, int hx, int hz, String what) {
        double l = v.screenX(-hx), r = v.screenX(hx + 1), t = v.screenY(-hz), b = v.screenY(hz + 1);
        check(r - l <= w + 1e-6 ? l >= x - 1e-6 && r <= x + w + 1e-6 : l <= x + 1e-6 && r >= x + w - 1e-6, what + ": x edges");
        check(b - t <= h + 1e-6 ? t >= y - 1e-6 && b <= y + h + 1e-6 : t <= y + 1e-6 && b >= y + h - 1e-6, what + ": z edges");
    }

    public static void main(String[] args) {
        { // fitsField
            var v = new MapView(270, 190);
            v.layout(0, 0, 900, 500);
            double l = v.screenX(-270), r = v.screenX(271), t = v.screenY(-190), b = v.screenY(191);
            check(l >= 0 && r <= 900 && t >= 0 && b <= 500, "corners inside the rect");
            check(near(l, 8) && near(r, 892) || near(t, 8) && near(b, 492), "one axis touches the margin");
            check(near((l + r) / 2, 450) && near((t + b) / 2, 250), "centred");
        }
        { // fitsWithoutGrid
            var v = new MapView(270, 190);
            v.layout(10, 20, 700, 400);
            check(v.scale() == v.fitScale() && !v.moved(), "fit scale, never moved");
        }
        { // zoomKeepsCursor
            var v = new MapView(270, 190);
            v.layout(0, 0, 900, 500);
            double wx = v.worldX(300), wz = v.worldZ(200);
            v.zoomAt(300, 200, 3);
            check(v.scale() > v.fitScale() && v.moved(), "zoomed in");
            check(near(v.worldX(300), wx) && near(v.worldZ(200), wz), "world point under the cursor fixed");
        }
        { // zoomClamped
            var v = new MapView(270, 190);
            v.layout(0, 0, 900, 500);
            v.zoomAt(450, 250, 100);
            check(v.scale() == 8, "max 8 px per block");
            v.zoomAt(450, 250, -100);
            check(near(v.scale(), v.fitScale()), "min fit scale");
        }
        { // panClamped
            var v = new MapView(270, 190);
            v.layout(0, 0, 900, 500);
            double x0 = v.screenX(0), z0 = v.screenY(0);
            v.pan(-5000, -5000);
            check(near(v.screenX(0), x0) && near(v.screenY(0), z0), "fit scale: nothing moves");
            v.zoomAt(450, 250, 100);
            v.pan(-1e6, -1e6);
            check(near(v.screenX(270 + 1), 900) && near(v.screenY(190 + 1), 500), "far left/up: east and south edges meet the rect");
            v.pan(1e6, 1e6);
            check(near(v.screenX(-270), 0) && near(v.screenY(-190), 0), "far right/down: west and north edges meet the rect");
        }
        { // clampAfterResize
            var v = new MapView(270, 190);
            v.layout(0, 0, 900, 500);
            v.zoomAt(450, 250, 6);
            v.pan(-1e6, -1e6);
            clamped(v, 0, 0, 900, 500, 270, 190, "before");
            double wx = v.worldX(450), wz = v.worldZ(250);
            v.layout(0, 0, 500, 300);
            clamped(v, 0, 0, 500, 300, 270, 190, "after resize");
            check(v.scale() >= v.fitScale() - 1e-9, "scale not below the new fit");
            v.layout(0, 0, 1600, 900);
            clamped(v, 0, 0, 1600, 900, 270, 190, "after growth");
            check(v.scale() >= v.fitScale() - 1e-9 && v.worldX(100) == v.worldX(100) && wx == wx && wz == wz, "scale and anchors finite");
        }
        { // roundTrip
            var v = new MapView(270, 190);
            v.layout(5, 7, 800, 450);
            v.zoomAt(300, 200, 2);
            for (double s : new double[] {5, 123.4, 800}) check(near(v.screenX(v.worldX(s)), s), "x round trip");
            for (double s : new double[] {7, 99.9, 450}) check(near(v.screenY(v.worldZ(s)), s), "y round trip");
        }
        System.out.println("MapViewCheck: " + checks + " checks passed");
    }
}
