package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * The drop board on the client (design 2026-11-14, section 5): every coin this client has been told of (its own and those it watches),
 * and for its own roll the cinematic: the camera flies to the altar, the coin is aimed along the rail, falls along the server's path,
 * the slot flares, the result rises, the camera returns and the War Table reopens. All motion is sampled from continuous time
 * (System.nanoTime) each render frame; nothing steps with the game tick. DropRender draws it and DropScreen takes the input.
 */
public final class DropClient {
    private DropClient() {}

    public enum Phase { FLY, AIM, WAIT, DROP, FLARE, REVEAL, RETURN, DONE }
    /** The cinematic's field of view (degrees), and how far the camera stands from the coin's plane to see the whole altar. */
    static final double FOV = 50, WIDE = 16.5;

    /** One coin: a drop this client was told of. */
    static final class View {
        int id; UUID who; String name = "", what = "unit"; boolean mine;
        /** Seconds (now()) the view began, the coin was let go, the path began to play and the view is to be dropped (0: not yet). */
        double began, releasedAt, pathAt, ended;
        /** The coin on the rail: where it is drawn and where it is going. */
        double x = DropSim.WIDTH / 2.0, targetX = x, xVel;
        boolean released, skipped;
        DropSim.Path path; double[] roll;
        JsonObject card = new JsonObject();
        int nextHit;
        /** What rises out of the slot: a LivingEntity or an ItemStack, built at the flare. */
        Object riser; boolean riserBuilt, landed, flared, revealed;
        /** The largest move of the coin between two frames no more than a thirtieth of a second apart (blocks), for the smoothness check: a frame the machine dropped is not the coin's doing. */
        double maxStep; double[] lastCoin;

        double length() { return path == null ? 0 : path.seconds(); }
        int slot() { return path == null ? -1 : path.slot(); }
        Phase phase(double now) {
            var b = Spec.DROPBOARD[0];
            if (path == null) return !released ? (mine && now - began < b.flyTenths() / 10.0 ? Phase.FLY : Phase.AIM) : Phase.WAIT;
            double t = now - pathAt;
            if (t < length()) return Phase.DROP;
            t -= length();
            if (t < b.flareTenths() / 10.0) return Phase.FLARE;
            t -= b.flareTenths() / 10.0;
            if (t < b.revealTenths() / 10.0) return Phase.REVEAL;
            t -= b.revealTenths() / 10.0;
            return mine && t < b.returnTenths() / 10.0 ? Phase.RETURN : Phase.DONE;
        }
        /** Seconds into the phases after the fall: the flare starts at 0. */
        double sinceLanding(double now) { return path == null ? -1 : now - pathAt - length(); }
        /** The coin's centre in the board frame. */
        double[] coin(double now) {
            if (path == null) return new double[]{x, DropSim.START_Y + Math.sin((now - began) * 3) * 0.02};
            return path.at(now - pathAt);
        }
    }

    /** Whether the War Table sends its rolls through the altar (the harness turns it off for the suites that test the plain roll). */
    public static volatile boolean enabled = true;
    static final Map<Integer, View> views = new LinkedHashMap<>();
    /** This client's own drop while its cinematic runs. */
    static View mine;
    /** The War Table page to reopen after the cinematic ("" for none), and the HUD setting to put back. */
    static String returnPage = ""; private static boolean previousHud;

    // the camera: eye and look-at point, each eased toward its target by a critically damped spring
    private static final double[] cam = new double[6], camVel = new double[6];
    private static double fovBlend, fovVel, lastSample, shake;
    private static double[] startEye = new double[3], startLook = new double[3];
    public static double cameraX, cameraY, cameraZ; public static float cameraYaw, cameraPitch;
    /** Counted by the checks: frames the cinematic's camera was sampled, and the last finished drop's largest coin step. */
    public static int frames; public static double lastMaxStep; public static int finished;

    static double now() { return System.nanoTime() / 1e9; }
    public static boolean cinematic() { return mine != null; }
    public static String phase() { return mine == null ? "" : mine.phase(now()).name(); }
    public static int active() { return views.size(); }

    // ---- packets ---------------------------------------------------------------------------------------------------------------------

    static void receive(MinecraftClient c, PacketByteBuf buf) {
        byte kind = buf.readByte();
        int id = buf.readVarInt();
        switch (kind) {
            case DropBoard.BEGIN -> {
                UUID who = buf.readUuid(); String name = buf.readString(64), what = buf.readString(16); int aimTicks = buf.readVarInt(); float x = buf.readFloat();
                c.execute(() -> begin(c, id, who, name, what, x));
            }
            case DropBoard.AIM -> { float x = buf.readFloat(); c.execute(() -> { var v = views.get(id); if (v != null && !v.mine && !v.released) v.targetX = DropSim.clampStart(x); }); }
            case DropBoard.PATH -> {
                UUID who = buf.readUuid(); String name = buf.readString(64), what = buf.readString(16); float x = buf.readFloat();
                byte[] bytes = buf.readByteArray(1 << 16); String card = buf.readString(4096);
                c.execute(() -> path(c, id, who, name, what, x, bytes, card));
            }
            case DropBoard.END -> c.execute(() -> { var v = views.remove(id); if (v != null) { DropRender.forget(v); if (v == mine) finish(c, false); } });
            default -> {}
        }
    }

    private static View view(MinecraftClient c, int id, UUID who, String name, String what) {
        View v = views.get(id);
        if (v != null) return v;
        // one coin a commander: his older one goes
        for (var it = views.values().iterator(); it.hasNext(); ) { View o = it.next(); if (o.who.equals(who) && o != mine) { DropRender.forget(o); it.remove(); } }
        v = new View(); v.id = id; v.who = who; v.name = name; v.what = what; v.began = now();
        v.mine = c.player != null && c.player.getUuid().equals(who);
        views.put(id, v);
        return v;
    }

    private static void begin(MinecraftClient c, int id, UUID who, String name, String what, float x) {
        if (c.player == null || c.world == null) return;
        View v = view(c, id, who, name, what);
        v.x = v.targetX = DropSim.clampStart(x);
        if (!v.mine) return;
        if (mine != null && mine != v) { views.remove(mine.id); DropRender.forget(mine); }
        boolean running = mine != null;
        mine = v;
        if (!running) {
            returnPage = c.currentScreen instanceof CampScreen s ? s.page : "";
            previousHud = c.options.hudHidden;
            Vec3d eye = c.player.getCameraPosVec(1f), look = c.player.getRotationVec(1f);
            startEye = new double[]{eye.x, eye.y, eye.z};
            startLook = new double[]{eye.x + look.x * 10, eye.y + look.y * 10, eye.z + look.z * 10};
            System.arraycopy(startEye, 0, cam, 0, 3); System.arraycopy(startLook, 0, cam, 3, 3);
            Arrays.fill(camVel, 0); fovBlend = 0; fovVel = 0; lastSample = now(); shake = 0;
        }
        c.options.hudHidden = true;
        c.setScreen(new DropScreen());
        DropRender.sound(c, DropRender.Sound.FLY, 1f, 1f);
    }

    private static void path(MinecraftClient c, int id, UUID who, String name, String what, float x, byte[] bytes, String cardText) {
        if (c.player == null || c.world == null) return;
        DropSim.Path p;
        try { p = DropSim.decode(bytes); } catch (IllegalArgumentException e) { return; }
        View v = view(c, id, who, name, what);
        if (v.path != null) return;
        try { v.card = JsonParser.parseString(cardText).getAsJsonObject(); } catch (RuntimeException e) { v.card = new JsonObject(); }
        v.released = true; v.x = v.targetX = p.xs()[0];
        v.path = p; v.pathAt = now(); v.nextHit = 0;
        // the coin's turn: it rolls with its sideways travel
        v.roll = new double[p.xs().length];
        for (int i = 1; i < v.roll.length; i++) v.roll[i] = v.roll[i - 1] + (p.xs()[i] - p.xs()[i - 1]) / DropSim.COIN + (p.ys()[i] - p.ys()[i - 1]) * 0.35;
        DropRender.sound(c, DropRender.Sound.RELEASE, 0.8f, 1f);
        if (v == mine && v.skipped) { v.skipped = false; skip(c); }
    }

    // ---- the roller's input (DropScreen, and the checks) --------------------------------------------------------------------------------

    /** Slides the coin toward a rail x. */
    public static void aim(double x) { if (mine != null && !mine.released) mine.targetX = DropSim.clampStart(x); }

    /** Lets the coin go where it is drawn. */
    public static void release() {
        View v = mine;
        if (v == null || v.released || v.phase(now()) == Phase.FLY && now() - v.began < 0.5) return;
        v.released = true; v.releasedAt = now(); v.targetX = v.x;
        GravesAndGloryClient.send("drop:place:" + String.format(Locale.ROOT, "%.3f", v.x));
    }

    /** A skip press: on the rail it lets the coin go; in the fall it jumps to the end of the reveal; a second press returns at once. */
    public static void skip(MinecraftClient c) {
        View v = mine;
        if (v == null) return;
        if (!v.released) { release(); return; }
        if (v.path == null) { v.skipped = true; return; } // the path is on its way: the skip is taken when it arrives
        if (v.skipped) { finish(c, true); return; }
        v.skipped = true;
        var b = Spec.DROPBOARD[0];
        double end = v.length() + (b.flareTenths() + b.revealTenths()) / 10.0, t = now() - v.pathAt;
        if (t < end - 0.7) { v.pathAt = now() - (end - 0.7); v.nextHit = v.path.hits().size(); v.lastCoin = null; }
        GravesAndGloryClient.send("drop:skip");
    }

    /** Ends the cinematic: the camera goes back to the commander and the War Table reopens on the result. */
    static void finish(MinecraftClient c, boolean tellServer) {
        View v = mine;
        if (v == null) return;
        mine = null;
        lastMaxStep = v.maxStep; finished++;
        if (tellServer) GravesAndGloryClient.send("drop:skip");
        v.ended = now(); views.remove(v.id); DropRender.forget(v);
        c.options.hudHidden = previousHud;
        if (c.currentScreen instanceof DropScreen) c.setScreen(null);
        if (!returnPage.isEmpty() && c.player != null && GravesAndGloryClient.inCamp()) CampScreen.open(returnPage);
        returnPage = "";
    }

    /** Each client tick: the aim is sent on (five times a second at most), the timer lets the coin go, finished coins are forgotten. */
    private static double aimSent, aimSentX = -1;
    static void tick(MinecraftClient c) {
        double now = now();
        if (c.world == null || c.player == null) { clear(c); return; }
        View v = mine;
        if (v != null) {
            var b = Spec.DROPBOARD[0];
            if (!(c.currentScreen instanceof DropScreen)) { finish(c, true); return; } // another screen took over (a match starting, the pause menu)
            Phase p = v.phase(now);
            if (!v.released) {
                if (now - v.began >= b.flyTenths() / 10.0 + b.aimSeconds()) release();
                else if (now - aimSent >= 0.2 && Math.abs(v.x - aimSentX) > 0.02) { aimSent = now; aimSentX = v.x; GravesAndGloryClient.send("drop:aim:" + String.format(Locale.ROOT, "%.2f", v.x)); }
            }
            if (p == Phase.DONE) finish(c, false);
            else if (v.released && v.path == null && now - v.releasedAt > 6) finish(c, true); // the path never came
        }
        for (var it = views.values().iterator(); it.hasNext(); ) {
            View o = it.next();
            if (o == mine) continue;
            boolean stale = o.path == null ? now - o.began > 40 : o.phase(now) == Phase.DONE;
            if (stale) { DropRender.forget(o); it.remove(); }
        }
        DropRender.tick(c);
    }

    static void clear(MinecraftClient c) {
        if (mine != null) { mine = null; c.options.hudHidden = previousHud; }
        for (View v : views.values()) DropRender.forget(v);
        views.clear(); returnPage = "";
    }

    // ---- the camera ---------------------------------------------------------------------------------------------------------------------

    private static double smooth(double t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }

    /** A critically damped spring toward target (exact for any frame time): returns the new value; vel[i] is its speed. */
    static double damp(double cur, double target, double[] vel, int i, double time, double dt) {
        double omega = 2 / time, k = omega * dt, e = 1 / (1 + k + 0.48 * k * k + 0.235 * k * k * k);
        double change = cur - target, temp = (vel[i] + omega * change) * dt;
        vel[i] = (vel[i] - omega * temp) * e;
        return target + (change + temp) * e;
    }

    /** The camera's field of view: the commander's own, eased to the cinematic's and back. */
    public static double fov(double own) { return own + (FOV - own) * smooth(fovBlend); }

    /** Render-frame sampling (the Camera mixin calls it): eases the eye and the look-at point toward the phase's shot. */
    public static void sampleCamera(float partial) {
        View v = mine;
        var c = MinecraftClient.getInstance();
        if (v == null || c.player == null) return;
        double now = now(), dt = Math.max(0, Math.min(0.1, now - lastSample));
        lastSample = now; frames++;
        Phase p = v.phase(now);
        double[] eye, look; double time = 0.5;
        double slotX = v.slot() + 0.5;
        switch (p) {
            case DROP -> {
                double t = now - v.pathAt, prog = t / Math.max(0.1, v.length()), cx = 0, cy = 0;
                for (int i = -3; i <= 3; i++) { double[] q = v.path.at(t + i * 0.1); cx += q[0] / 7; cy += q[1] / 7; }
                double dist = WIDE + (11.5 - WIDE) * smooth(prog), ly = Math.max(4.6, Math.min(8.6, 5.2 + (cy + 1.0 - 5.2) * smooth(prog * 3)));
                look = DropLayout.world(4.5 + (cx - 4.5) * 0.35, ly, 0); eye = DropLayout.world(4.5 + (cx - 4.5) * 0.2, ly - 0.3, dist); time = 0.35;
            }
            case FLARE -> { look = DropLayout.world(slotX, 9.4, 0); eye = DropLayout.world(4.5 + (slotX - 4.5) * 0.7, 8.9, 8.5); time = 0.3; }
            case REVEAL -> { look = DropLayout.world(slotX, 8.7, 0.8); eye = DropLayout.world(4.5 + (slotX - 4.5) * 0.75, 8.6, 6.8); time = 0.45; }
            case RETURN, DONE -> {
                Vec3d e = c.player.getCameraPosVec(partial), l = c.player.getRotationVec(partial);
                eye = new double[]{e.x, e.y, e.z}; look = new double[]{e.x + l.x * 10, e.y + l.y * 10, e.z + l.z * 10}; time = 0.2;
            }
            default -> { look = DropLayout.world(4.5, 5.2, 0); eye = DropLayout.world(4.5 + (v.x - 4.5) * 0.12, 5.0, WIDE); time = p == Phase.FLY ? 0.5 : 0.4; }
        }
        for (int i = 0; i < 3; i++) { cam[i] = damp(cam[i], eye[i], camVel, i, time, dt); cam[i + 3] = damp(cam[i + 3], look[i], camVel, i + 3, time, dt); }
        double[] f = {fovVel};
        fovBlend = damp(fovBlend, p == Phase.RETURN || p == Phase.DONE ? 0 : 1, f, 0, p == Phase.RETURN ? 0.25 : 0.5, dt); fovVel = f[0];
        shake = Math.max(0, shake - dt * 1.6);
        double sx = Math.sin(now * 47) * shake * 0.12, sy = Math.cos(now * 53) * shake * 0.12;
        cameraX = cam[0]; cameraY = cam[1]; cameraZ = cam[2];
        double dx = cam[3] - cam[0], dy = cam[4] + sy - cam[1], dz = cam[5] + sx - cam[2];
        cameraYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        cameraPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    /** The landing's kick on the camera: stronger on the high slots. */
    static void shake(double amount) { shake = Math.max(shake, amount); }
}
