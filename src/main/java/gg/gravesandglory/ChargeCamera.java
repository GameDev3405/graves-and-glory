package gg.gravesandglory;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * The Charge's overhead camera (command UI plan Task 10). Client only: it moves the RENDER camera (ChargeCameraMixin on Camera.update)
 * and never the player entity or its rotation, and it sends nothing but the existing tactical_v2 Charge with its yaw.
 *
 * Holding the Charge key (X) for HOLD_TICKS while a Charge is aimed lifts the camera LIFT blocks above the selected troops' centre at
 * PITCH degrees, facing the way the player faced. The mouse then moves a ground cursor instead of the player's view (absorb: every
 * frame and tick the player's rotation is put back and the turn moves the cursor), and an arrow on the ground runs from the troops
 * toward it. Releasing the key sends the Charge along the arrow and snaps the camera back to the player's eye, whose view is exactly
 * as it was. A tap of X keeps the older aim (Enter or a click confirms along the player's look).
 *
 * With overheadAlways (an accessibility option, saved in the mod's client options file and toggled by a binding in the Controls
 * screen's Graves and Glory category) the camera stays overhead after the release until any key or mouse button is pressed.
 * A disconnect, a rejoin, death, a Screen or the battle's end puts it back to inactive.
 */
public final class ChargeCamera {
    private ChargeCamera() {}

    /** Blocks above the selected troops' centre, and the camera's downward pitch, while overhead. */
    public static final double LIFT = 18;
    public static final float PITCH = 70f;
    /** Ticks X must be held before the camera lifts (shorter is a tap: the older aim), and ticks the lift eases in over. */
    static final int HOLD_TICKS = 4, LIFT_TICKS = 5;
    /** The ground cursor: its start ahead of the troops, its nearest and farthest reach, and blocks it moves per degree the mouse would turn. */
    static final double CURSOR_START = 8, CURSOR_MIN = 3, CURSOR_MAX = 24, BLOCKS_PER_DEGREE = 0.2;
    static final String OPTION = "chargeCameraOverheadAlways";

    /** Accessibility: keep the camera overhead after the release until a key is pressed. */
    public static boolean overheadAlways;
    static KeyBinding toggleKey;

    private static boolean active, held, lingering;
    /** The perspective to give back when the overhead view ends, or null when it did not change it. */
    private static Perspective forced;
    private static int holdTicks, activeTicks;
    private static float savedYaw, savedPitch, camYaw, arrow;
    private static double right, forward = CURSOR_START;
    private static double cx, cz, prevCx, prevCz, baseY;
    private static final Set<KeyBinding> down = new HashSet<>();
    private static final boolean[] rawDown = new boolean[GLFW.GLFW_KEY_LAST + 1];
    /** Every valid GLFW key code (glfwGetKey reports an error for the gaps between them). */
    private static final int[] KEYS = keys();
    /** Frames the ground arrow was drawn (for the checks). */
    public static int arrowFrames;

    /** Whether the camera is overhead: while X is held, or lingering after the release with overheadAlways. */
    public static boolean active() { return active; }
    /** Whether X is being held with the camera up (the arrow is live). */
    public static boolean held() { return held; }
    /** The arrow's direction as a yaw in degrees (Minecraft's: 0 faces +z, 90 faces -x); the Charge's tactical yaw on release. */
    public static float direction() { return arrow; }

    /** From onInitializeClient: the option, the toggle binding and the arrow's drawing. */
    static void register() {
        load();
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gravesandglory.charge_overhead", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.gravesandglory"));
        WorldRenderEvents.AFTER_ENTITIES.register(ChargeCamera::renderWorld);
    }

    // ---- the option ----

    static Path file() { return GravesAndGloryClient.toolbarFlag(); }

    static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties props = new Properties();
            props.load(new StringReader(Files.readString(f)));
            overheadAlways = Boolean.parseBoolean(props.getProperty(OPTION, "false"));
        } catch (Exception e) { System.err.println("[Graves and Glory] Could not read the client options: " + e.getMessage()); }
    }

    /** Writes the option into the client options file, keeping every other line in it. */
    static void save() {
        try {
            Path f = file();
            Properties props = new Properties();
            if (Files.exists(f)) props.load(new StringReader(Files.readString(f)));
            props.setProperty(OPTION, Boolean.toString(overheadAlways));
            StringWriter out = new StringWriter();
            props.store(out, "Graves and Glory client options");
            Files.createDirectories(f.getParent());
            SafeFiles.write(f, out.toString());
        } catch (Exception e) { System.err.println("[Graves and Glory] Could not save the client options: " + e.getMessage()); }
    }

    /** Sets the option; save writes it to the client options file. Turning it off ends a lingering view. */
    public static void setOverheadAlways(boolean on, boolean save) {
        overheadAlways = on;
        if (save) save();
        if (!on && lingering) end();
    }

    // ---- each client tick ----

    /** Every client tick with a player, after CommandMode.tick: the toggle, the hold, the release and the linger. */
    static void tick(MinecraftClient c, KeyBinding chargeKey) {
        while (toggleKey != null && toggleKey.wasPressed()) {
            setOverheadAlways(!overheadAlways, true);
            if (c.player != null) c.player.sendMessage(Text.literal("Charge camera stays overhead: " + (overheadAlways ? "ON" : "OFF")), true);
        }
        var p = c.player;
        if (p == null || c.world == null || !GravesAndGloryClient.battleActive() || p.isDead() || p.isSpectator()) { reset(); return; }
        if (active) { absorb(); follow(); activeTicks++; if (activeTicks % EYE_TICKS == 1) sendEye(); }
        if (c.currentScreen != null) { if (active) end(); holdTicks = 0; return; }
        if (lingering) { if (keyEdge(c)) end(); return; }
        boolean aimingCharge = CommandSelection.aiming().equals("charge");
        if (!held) {
            if (aimingCharge && chargeKey.isPressed()) { if (++holdTicks >= HOLD_TICKS) lift(p.getYaw(), p.getPitch()); }
            else holdTicks = 0;
            return;
        }
        if (!aimingCharge) { end(); return; } // the aim was cancelled, or sent some other way (Enter, a click)
        CommandMode.facing = arrow; CommandMode.facingSet = true;
        if (!chargeKey.isPressed()) release(c);
    }

    private static void lift(float yaw, float pitch) {
        active = true; held = true; lingering = false; activeTicks = 0; holdTicks = 0;
        savedYaw = yaw; savedPitch = pitch; camYaw = yaw; arrow = yaw;
        right = 0; forward = CURSOR_START;
        Vec3d centre = CommandSelection.selectedCentre();
        cx = prevCx = centre.x; cz = prevCz = centre.z; baseY = groundY(cx, cz);
        CommandMode.facing = arrow; CommandMode.facingSet = true;
        // Overhead the view is a third-person one, as F5 gives: the held items leave the screen, the crosshair (which would point where the
        // commander looks, not at the arrow) goes, and the commander's own body shows. end() gives the player's perspective back.
        var c = MinecraftClient.getInstance();
        if (c.options.getPerspective().isFirstPerson()) { forced = c.options.getPerspective(); c.options.setPerspective(Perspective.THIRD_PERSON_BACK); }
    }

    /** Whether the overhead view put a first-person player into a third-person perspective for its time (end() restores it). */
    public static boolean forcedPerspective() { return forced != null; }

    /** X released: the Charge goes along the arrow, then the camera snaps back (or lingers with overheadAlways). */
    private static void release(MinecraftClient c) {
        held = false; holdTicks = 0;
        CommandMode.facing = arrow; CommandMode.facingSet = true;
        CommandMode.confirm();
        if (overheadAlways) { lingering = true; snapshotKeys(c); }
        else end();
    }

    private static void end() {
        active = false; held = false; lingering = false; holdTicks = 0; activeTicks = 0; down.clear();
        if (forced != null) { var c = MinecraftClient.getInstance(); if (c.options != null) c.options.setPerspective(forced); forced = null; }
    }

    /** A disconnect, a rejoin, death or the battle's end: inactive, nothing held. */
    public static void reset() { end(); arrowFrames = 0; }

    /** The selected troops' centre, eased so the camera glides with them; its ground height under it. */
    private static void follow() {
        prevCx = cx; prevCz = cz;
        Vec3d t = CommandSelection.selectedCentre();
        cx += (t.x - cx) * .35; cz += (t.z - cz) * .35;
        baseY = groundY(cx, cz);
    }

    /** Standing height (feet) on the arena's ground at a point, or the player's when the arena has no ground there. */
    static double groundY(double x, double z) {
        int g = Arena.ground(MathHelper.floor(x), MathHelper.floor(z));
        var p = MinecraftClient.getInstance().player;
        if (g < -64 || g > 320) return p == null ? 65 : p.getY();
        return g + 1;
    }

    /** Whether a key, mouse button or binding went down since the last tick (bindings held when the linger began do not count). */
    private static boolean keyEdge(MinecraftClient c) {
        boolean edge = false;
        for (KeyBinding k : c.options.allKeys) {
            boolean now = k.isPressed();
            if (now && !down.contains(k)) edge = true;
            if (now) down.add(k); else down.remove(k);
        }
        long w = c.getWindow().getHandle();
        for (int key : KEYS) {
            boolean now = InputUtil.isKeyPressed(w, key);
            if (now && !rawDown[key]) edge = true;
            rawDown[key] = now;
        }
        return edge;
    }

    private static void snapshotKeys(MinecraftClient c) {
        down.clear();
        for (KeyBinding k : c.options.allKeys) if (k.isPressed()) down.add(k);
        long w = c.getWindow().getHandle();
        for (int key : KEYS) rawDown[key] = InputUtil.isKeyPressed(w, key);
    }

    private static int[] keys() {
        int[][] ranges = {{32, 32}, {39, 39}, {44, 57}, {59, 59}, {61, 61}, {65, 93}, {96, 96}, {161, 162}, {256, 269}, {280, 284}, {290, 314}, {320, 336}, {340, 348}};
        int n = 0;
        for (int[] r : ranges) n += r[1] - r[0] + 1;
        int[] out = new int[n];
        int i = 0;
        for (int[] r : ranges) for (int k = r[0]; k <= r[1]; k++) out[i++] = k;
        return out;
    }

    // ---- the mouse ----

    /**
     * While overhead, the mouse's turn of the player (Mouse.updateMouse -> changeLookDirection) is taken back and moves the ground cursor
     * instead: right turns move it right on screen, up moves it ahead. Called at the head of every Camera.update (after the frame's mouse
     * input, before anything reads the rotation) and every client tick, so no tick ever sends the server a turned look.
     */
    public static void absorb() {
        if (!active) return;
        var p = MinecraftClient.getInstance().player;
        if (p == null) return;
        float dy = p.getYaw() - savedYaw, dp = p.getPitch() - savedPitch;
        if (dy == 0 && dp == 0) return;
        p.setYaw(savedYaw); p.setPitch(savedPitch);
        p.prevYaw -= dy; p.prevPitch -= dp;
        if (held) steer(dy, dp);
    }

    /** Moves the ground cursor by a mouse turn of dy degrees right and dp degrees down, and points the arrow at it. */
    static void steer(double dy, double dp) {
        right += dy * BLOCKS_PER_DEGREE; forward -= dp * BLOCKS_PER_DEGREE;
        double len = Math.hypot(right, forward);
        if (len < 1e-6) { right = 0; forward = CURSOR_MIN; }
        else if (len < CURSOR_MIN) { right *= CURSOR_MIN / len; forward *= CURSOR_MIN / len; }
        else if (len > CURSOR_MAX) { right *= CURSOR_MAX / len; forward *= CURSOR_MAX / len; }
        arrow = MathHelper.wrapDegrees(camYaw + (float) Math.toDegrees(Math.atan2(right, forward)));
    }

    /** Ticks between the camera messages to the server (Tactics.CAMERA_FRESH is three times this). */
    static final int EYE_TICKS = 10;

    /** Tells the server where the overhead camera is, so that a troop in its view counts as watched (the stall ladder never jumps a troop a player can see). */
    private static void sendEye() {
        if (!net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(GravesAndGlory.CAMERA)) return;
        double back = LIFT / Math.tan(Math.toRadians(PITCH)), r = Math.toRadians(camYaw);
        var b = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        b.writeDouble(cx + Math.sin(r) * back); b.writeDouble(baseY + LIFT); b.writeDouble(cz - Math.cos(r) * back);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(GravesAndGlory.CAMERA, b);
    }

    // ---- the camera (ChargeCameraMixin) ----

    /**
     * The overhead pose for this frame, eased in from the vanilla pose over LIFT_TICKS: {x, y, z, yaw, pitch}. The camera sits LIFT above
     * the troops' centre and far enough back along its yaw that the centre is in the middle of the view.
     */
    public static double[] pose(float delta, Vec3d from, float fromPitch) {
        double x = MathHelper.lerp(delta, prevCx, cx), z = MathHelper.lerp(delta, prevCz, cz), y = baseY + LIFT;
        double back = LIFT / Math.tan(Math.toRadians(PITCH)), r = Math.toRadians(camYaw);
        x += Math.sin(r) * back; z -= Math.cos(r) * back;
        double k = lingering ? 1 : Math.min(1, (activeTicks + delta) / LIFT_TICKS);
        k = k * k * (3 - 2 * k);
        return new double[]{MathHelper.lerp(k, from.x, x), MathHelper.lerp(k, from.y, y), MathHelper.lerp(k, from.z, z), camYaw, MathHelper.lerp(k, fromPitch, PITCH)};
    }

    // ---- the ground arrow ----

    static void renderWorld(WorldRenderContext context) {
        if (!held || context.consumers() == null) return;
        float d = context.tickDelta();
        double ox = MathHelper.lerp(d, prevCx, cx), oz = MathHelper.lerp(d, prevCz, cz);
        double len = Math.hypot(right, forward), a = Math.toRadians(arrow), fx = -Math.sin(a), fz = Math.cos(a);
        var cam = context.camera().getPos();
        var matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        var lines = context.consumers().getBuffer(RenderLayer.getLines());
        for (double s = 1; s <= len; s += 1) mark(matrices, lines, ox + fx * s, oz + fz * s, .36);
        double tx = ox + fx * len, tz = oz + fz * len;
        for (int side : new int[]{-1, 1}) {
            double h = a + side * Math.toRadians(35), hx = Math.sin(h), hz = -Math.cos(h); // back along the shaft, swept out to each side
            for (int i = 1; i <= 3; i++) mark(matrices, lines, tx + hx * i, tz + hz * i, .36);
        }
        mark(matrices, lines, tx, tz, .55);
        matrices.pop();
        arrowFrames++;
    }

    private static void mark(net.minecraft.client.util.math.MatrixStack m, net.minecraft.client.render.VertexConsumer lines, double x, double z, double half) {
        double y = groundY(x, z) + .05;
        WorldRenderer.drawBox(m, lines, new Box(x - half, y, z - half, x + half, y + .12, z + half), 1f, .82f, .4f, 1f);
    }
}
