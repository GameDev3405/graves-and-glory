package gg.gravesandglory;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * Ground outlines for formation orders (Task 9), all client-side drawing on top of what the server sends (command_v1):
 * <ul>
 * <li>While the player aims a Formation (CommandSelection.aiming() is "hold"), each selected troop's slot is drawn on the ground at the cursor, facing
 *     the look yaw: {@link #aimed} holds those slots, laid out by CommandMode.places exactly as the server forms the group.</li>
 * <li>On confirm the outline stays lit outlineLitTicks and, when the acknowledgement arrives, a pulse ring expands from the target.</li>
 * <li>A teammate's outline (OUTLINE, never sent to the issuer or an enemy) is drawn faint, GHOST_ALPHA, until his troops have arrived or outlineShareTicks
 *     have gone. One order may bring several (a Charge's groups each run from their own start); all are drawn, and the owner's next order replaces them
 *     (CommandNet.Client keeps the owner and the arrival time).</li>
 * <li>A Formation change (ACK "formation", no outline of its own) lights a square under each troop it names.</li>
 * </ul>
 * Everything counts client ticks (OrderFeedback.now), so a paused game pauses it.
 */
public final class OrderOutlines {
    private OrderOutlines() {}
    /** A teammate's outline is drawn at this opacity (the brief: 35%). */
    public static final float GHOST_ALPHA = .35f;
    /** How long a pulse ring takes to expand and fade, in ticks, and the radii it spans. */
    static final int PULSE_TICKS = 24;
    static final double PULSE_FROM = .6, PULSE_TO = 5;
    /** A teammate's outline is not retired for arrival before it has been up this long, so it is seen even when his troops are already there. */
    static final int MIN_GHOST_TICKS = 10;
    /** An acknowledgement within this many ms of an outline is the same order's. */
    static final long SAME_ORDER_MS = 400;
    /** Gold, the House-neutral command colour. */
    static final float R = .95f, G = .82f, B = .4f;

    /** The slots under the cursor while aiming a Formation (empty otherwise), on the ground. */
    public static final List<Vec3d> aimed = new ArrayList<>();
    /** The facing (degrees, as CommandMode.facing) and the cursor point the {@link #aimed} slots were laid out for. */
    public static float aimedFacing;
    public static Vec3d aimedAnchor;
    private static List<Vec3d> lastAimed = List.of();
    private static boolean prevAiming;
    private static int prevOrders;
    /** Orders this client sent whose acknowledgement has not arrived yet (so an acknowledgement then is his own, not a teammate's). */
    private static int pendingOwn;
    private static long pendingUntil;

    /** A set of slots lit for a while: the confirmed outline, or the squares under a Formation change's troops. */
    record Lit(List<Vec3d> slots, long until, boolean own) {}
    /** A teammate's outline: its slots, when it was first seen and the troops that went with the order (arrival is judged from them). */
    record Ghost(CommandNet.Client.Outline src, List<Vec3d> slots, long born, int[] units) {}
    record Pulse(double x, double z, long start, boolean own) {}
    private static final List<Lit> lit = new ArrayList<>();
    private static final List<Ghost> ghosts = new ArrayList<>();
    private static final List<Pulse> pulses = new ArrayList<>();

    // What the last frames drew, for the harness.
    /** The opacity the last teammate slot was drawn at (0 until one is). */
    public static volatile float lastTeamAlpha;
    /** The opacity the last own lit or aimed slot was drawn at. */
    public static volatile float lastOwnAlpha;
    /** The alpha of the last square actually handed to the renderer; the team alphas above are read from it, so a test sees what was drawn. */
    static volatile float lastSquareAlpha;
    /** Frames that drew at least one teammate slot / one lit own slot / one aimed slot. */
    public static volatile int ghostFrames, litFrames, aimFrames;

    private static boolean registered;
    /** Hooks the drawing and the tick (GravesAndGloryClient.onInitializeClient, after the main tick handler so it reads the aim as that left it). */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        WorldRenderEvents.AFTER_ENTITIES.register(OrderOutlines::renderWorld);
        ClientTickEvents.END_CLIENT_TICK.register(c -> { tick(c); OrderFeedback.tick(c); });
    }

    /** Own slots lit now. */
    public static int litCount() { return (int) lit.stream().filter(l -> l.own() && OrderFeedback.now < l.until()).mapToInt(l -> l.slots().size()).sum(); }
    /** Teammate slots drawn now (lit squares under a teammate's Formation change included). */
    public static int ghostCount() {
        return ghosts.stream().mapToInt(g -> g.slots().size()).sum() + lit.stream().filter(l -> !l.own() && OrderFeedback.now < l.until()).mapToInt(l -> l.slots().size()).sum();
    }
    /** The teammate outlines (one per OUTLINE record) being drawn. */
    public static int ghostOutlines() { return ghosts.size(); }
    public static int pulseCount() { return pulses.size(); }

    /** A disconnect or a new round. */
    public static void reset() {
        aimed.clear(); lastAimed = List.of(); aimedAnchor = null; prevAiming = false; prevOrders = 0; pendingOwn = 0; pendingUntil = 0;
        lit.clear(); ghosts.clear(); pulses.clear(); lastTeamAlpha = 0; lastOwnAlpha = 0; ghostFrames = 0; litFrames = 0; aimFrames = 0;
    }

    static void tick(MinecraftClient c) {
        if (c.world == null || c.player == null || !GravesAndGloryClient.battleActive()) {
            if (!aimed.isEmpty() || !lit.isEmpty() || !ghosts.isEmpty() || !pulses.isEmpty()) reset();
            prevOrders = CommandMode.ordersSent; prevAiming = false;
            return;
        }
        long now = OrderFeedback.now;
        // Orders this client sent since the last tick (an acknowledgement then is his own).
        int sent = CommandMode.ordersSent;
        if (sent > prevOrders) { pendingOwn += sent - prevOrders; pendingUntil = now + 60; }
        boolean aiming = CommandSelection.aiming().equals("hold") && CommandMode.active();
        if (aiming) layOut();
        else aimed.clear();
        if (prevAiming && !aiming && sent > prevOrders && !lastAimed.isEmpty()) lit.add(new Lit(lastAimed, now + Spec.COMBATDISCIPLINE[0].outlineLitTicks(), true));
        if (!aiming) lastAimed = List.of();
        prevAiming = aiming; prevOrders = sent;
        if (pendingOwn > 0 && now > pendingUntil) pendingOwn = 0;
        lit.removeIf(l -> now >= l.until());
        pulses.removeIf(p -> now - p.start() > PULSE_TICKS);
        syncGhosts(c, now);
    }

    /** The aimed slots: every selected group's places at the cursor, rotated to the facing the way CommandMode's own preview does. */
    private static void layOut() {
        aimed.clear();
        var at = CommandMode.destination;
        if (at == null) { lastAimed = List.of(); return; }
        var f = formation(CommandMode.formation);
        aimedFacing = CommandMode.facing; aimedAnchor = at;
        for (var group : CommandMode.groups) {
            if (group.id().equals("all") || group.count() <= 0 || !CommandSelection.isSelected(group.id())) continue;
            for (double[] place : CommandMode.places(CommandMode.troops(group), group.count(), group.id(), "hold", f)) aimed.add(slot(at.x, at.z, aimedFacing, place));
        }
        lastAimed = List.copyOf(aimed);
    }

    /** A place {side, back} from the point (x, z) turned to the facing (degrees), on the ground (CommandMode.tick's formula). */
    static Vec3d slot(double x, double z, float facing, double[] place) {
        double angle = Math.toRadians(facing), px = x + Math.cos(angle) * place[0] + Math.sin(angle) * place[1], pz = z + Math.sin(angle) * place[0] - Math.cos(angle) * place[1];
        return new Vec3d(px, Arena.footing((int) Math.floor(px), (int) Math.floor(pz)) + 1.05, pz);
    }

    static Spec.FormationsRow formation(String id) {
        return Arrays.stream(Spec.FORMATIONS).filter(v -> v.id().equals(id)).findFirst().orElse(Spec.FORMATIONS[0]);
    }

    /** An acknowledgement arrived (OrderFeedback.onAck): the target pulses, and a Formation change lights a square under each troop it names. */
    static void onAck(CommandNet.Client.Ack ack) {
        long now = OrderFeedback.now;
        boolean own = pendingOwn > 0;
        if (own) pendingOwn--;
        pulses.add(new Pulse(ack.x(), ack.z(), now, own));
        if (ack.order().equals("formation")) {
            var c = MinecraftClient.getInstance();
            List<Vec3d> under = new ArrayList<>();
            if (c.world != null) for (int id : ack.units()) {
                Entity e = c.world.getEntityById(id);
                if (e != null && e.isAlive()) under.add(new Vec3d(e.getX(), Arena.footing((int) Math.floor(e.getX()), (int) Math.floor(e.getZ())) + 1.05, e.getZ()));
            }
            if (!under.isEmpty()) lit.add(new Lit(under, now + Spec.COMBATDISCIPLINE[0].outlineLitTicks(), own));
        }
    }

    // ---- teammates' outlines ----

    private static void syncGhosts(MinecraftClient c, long now) {
        var shared = CommandNet.Client.outlines;
        // new arrivals
        for (var o : shared) {
            boolean known = false;
            for (var g : ghosts) if (g.src() == o) { known = true; break; }
            if (!known) ghosts.add(ghost(c, o, now));
        }
        // retired: replaced by the owner's next order (no longer in the shared list), out of time, or arrived
        int share = Spec.COMBATDISCIPLINE[0].outlineShareTicks();
        for (Iterator<Ghost> it = ghosts.iterator(); it.hasNext(); ) {
            var g = it.next();
            boolean gone = !shared.contains(g.src());
            if (!gone && now - g.born() >= share) { shared.remove(g.src()); gone = true; }
            if (!gone && now - g.born() >= MIN_GHOST_TICKS && now % 5 == 0 && arrived(c, g)) { shared.remove(g.src()); gone = true; }
            if (gone) it.remove();
        }
    }

    private static Ghost ghost(MinecraftClient c, CommandNet.Client.Outline o, long now) {
        // The same order's acknowledgement (it was sent first) says whether this is a Charge and which troops went.
        CommandNet.Client.Ack match = null;
        var acks = CommandNet.Client.acks;
        for (int i = acks.size() - 1; i >= 0; i--) {
            var a = acks.get(i);
            if ((a.order().equals("hold") || a.order().equals("charge")) && Math.abs(a.atMs() - o.atMs()) <= SAME_ORDER_MS) { match = a; break; }
        }
        String order = match != null && match.order().equals("charge") ? "charge" : "hold";
        int[] units = match == null ? new int[0] : match.units();
        var f = formation(o.formation());
        List<Vec3d> slots = new ArrayList<>();
        for (var entry : o.groups()) {
            int members = Math.max(0, entry.getValue());
            for (double[] place : CommandMode.places(troopsOf(c, units, entry.getKey(), members), members, entry.getKey(), order, f)) slots.add(slot(o.x(), o.z(), o.yaw(), place));
        }
        return new Ghost(o, List.copyOf(slots), now, units);
    }

    /** The sheet rows of the troops of this group among the order's troops, as far as the client knows them (a teammate's troops are named like any). */
    private static List<Spec.TroopsRow> troopsOf(MinecraftClient c, int[] units, String group, int members) {
        List<Spec.TroopsRow> out = new ArrayList<>();
        if (c.world == null) return out;
        for (int id : units) {
            Entity e = c.world.getEntityById(id);
            var name = e == null ? null : e.getCustomName();
            var spec = name == null ? null : ArmyMobs.named(name.getString());
            if (spec != null && spec.group().equals(group)) out.add(spec);
        }
        return out.size() > members ? out.subList(0, members) : out;
    }

    /** Whether the order's troops have reached this outline: three quarters of its slots have one of them within a block and a half. */
    private static boolean arrived(MinecraftClient c, Ghost g) {
        if (g.units().length == 0 || g.slots().isEmpty() || c.world == null) return false;
        List<Entity> troops = new ArrayList<>();
        for (int id : g.units()) { Entity e = c.world.getEntityById(id); if (e != null && e.isAlive()) troops.add(e); }
        if (troops.isEmpty()) return false;
        int filled = 0;
        for (var slot : g.slots()) for (var e : troops) {
            double dx = e.getX() - slot.x, dz = e.getZ() - slot.z;
            if (dx * dx + dz * dz <= 2.25) { filled++; break; }
        }
        return filled * 4 >= g.slots().size() * 3;
    }

    // ---- drawing ----

    static void renderWorld(WorldRenderContext context) {
        if (context.consumers() == null) return;
        boolean any = !aimed.isEmpty() || !lit.isEmpty() || !ghosts.isEmpty() || !pulses.isEmpty();
        if (!any) return;
        MatrixStack matrices = context.matrixStack();
        var camera = context.camera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());
        long now = OrderFeedback.now;
        if (!aimed.isEmpty()) {
            for (var p : aimed) square(matrices, lines, p, 1f);
            heading(matrices, lines, aimedAnchor, aimedFacing, 1f);
            lastOwnAlpha = 1f; aimFrames++;
        }
        boolean ownLit = false, teamLit = false;
        for (var l : lit) {
            if (now >= l.until()) continue;
            float a = l.own() ? 1f : GHOST_ALPHA;
            for (var p : l.slots()) square(matrices, lines, p, a);
            if (l.own()) { lastOwnAlpha = a; ownLit = true; } else if (!l.slots().isEmpty()) { lastTeamAlpha = lastSquareAlpha; teamLit = true; }
        }
        if (ownLit) litFrames++;
        boolean teamDrawn = teamLit;
        for (var g : ghosts) {
            for (var p : g.slots()) square(matrices, lines, p, GHOST_ALPHA);
            heading(matrices, lines, new Vec3d(g.src().x(), g.slots().isEmpty() ? 65 : g.slots().get(0).y, g.src().z()), g.src().yaw(), GHOST_ALPHA);
            if (!g.slots().isEmpty()) { lastTeamAlpha = lastSquareAlpha; teamDrawn = true; }
        }
        if (teamDrawn) ghostFrames++;
        for (var pulse : pulses) {
            double age = (now - pulse.start()) + context.tickDelta();
            if (age < 0 || age > PULSE_TICKS) continue;
            double k = age / PULSE_TICKS, radius = PULSE_FROM + (PULSE_TO - PULSE_FROM) * k;
            float a = (float) (1 - k) * (pulse.own() ? 1f : GHOST_ALPHA);
            ring(matrices, lines, pulse.x(), pulse.z(), radius, a);
        }
        matrices.pop();
    }

    private static void square(MatrixStack matrices, VertexConsumer lines, Vec3d p, float alpha) {
        WorldRenderer.drawBox(matrices, lines, new Box(p.x - .32, p.y, p.z - .32, p.x + .32, p.y + .09, p.z + .32), R, G, B, alpha);
        lastSquareAlpha = alpha;
    }

    /** A short line from the point along the facing, so the row's front reads. The facing is the player's yaw in degrees, 0 toward +z. */
    private static void heading(MatrixStack matrices, VertexConsumer lines, Vec3d at, float facing, float alpha) {
        if (at == null) return;
        double angle = Math.toRadians(facing), dx = -Math.sin(angle), dz = Math.cos(angle);
        double y = Arena.footing((int) Math.floor(at.x), (int) Math.floor(at.z)) + 1.07;
        line(matrices, lines, at.x, y, at.z, at.x + dx * 3, y, at.z + dz * 3, alpha);
        line(matrices, lines, at.x + dx * 3, y, at.z + dz * 3, at.x + dx * 2.3 + dz * .5, y, at.z + dz * 2.3 - dx * .5, alpha);
        line(matrices, lines, at.x + dx * 3, y, at.z + dz * 3, at.x + dx * 2.3 - dz * .5, y, at.z + dz * 2.3 + dx * .5, alpha);
    }

    private static void ring(MatrixStack matrices, VertexConsumer lines, double cx, double cz, double radius, float alpha) {
        int segments = 28;
        double y = Arena.footing((int) Math.floor(cx), (int) Math.floor(cz)) + 1.07;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            line(matrices, lines, cx + Math.cos(a0) * radius, y, cz + Math.sin(a0) * radius, cx + Math.cos(a1) * radius, y, cz + Math.sin(a1) * radius, alpha);
        }
    }

    private static void line(MatrixStack matrices, VertexConsumer lines, double x0, double y0, double z0, double x1, double y1, double z1, float alpha) {
        var entry = matrices.peek();
        float nx = (float) (x1 - x0), ny = (float) (y1 - y0), nz = (float) (z1 - z0), len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-4f) return;
        nx /= len; ny /= len; nz /= len;
        lines.vertex(entry.getPositionMatrix(), (float) x0, (float) y0, (float) z0).color(R, G, B, alpha).normal(entry.getNormalMatrix(), nx, ny, nz).next();
        lines.vertex(entry.getPositionMatrix(), (float) x1, (float) y1, (float) z1).color(R, G, B, alpha).normal(entry.getNormalMatrix(), nx, ny, nz).next();
    }
}
