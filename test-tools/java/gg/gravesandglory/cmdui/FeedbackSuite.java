package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandNet;
import gg.gravesandglory.CommandSelection;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.FeedbackProbe;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.OrderFeedback;
import gg.gravesandglory.OrderIntakeProbe;
import gg.gravesandglory.OrderOutlines;
import gg.gravesandglory.Phase1Checks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command UI plan Task 9: ground outlines and order feedback. The host client (this suite) aims and confirms a Formation through the real keys,
 * issues the other orders as real packets, and reads what its own client draws, hears and poses; a second, teammate client (FeedbackMate, joined
 * to this host's LAN world by the scratch launcher, env GRAVES_FEEDBACK_MATE = the control directory) logs what a teammate sees, and this suite
 * reads that log at the end. An enemy never gets an OUTLINE: proven by the server's per-recipient send count with a synthetic enemy squad
 * (controller ruling 3). Screenshots go to work/cmdui-shots (never committed). Never included in releases.
 */
public final class FeedbackSuite implements CommandUiChecks.Suite {
    static final String HORN_CHARGE = "gravesandglory:horn_charge", HORN_ROUT = "gravesandglory:horn_rout", HOLD_SHOUT = "gravesandglory:order_hold_shout",
        CHARGE_SHOUT = "gravesandglory:order_charge_shout", CHIME = "minecraft:block.note_block.chime";
    static final String[] CUES = { HORN_CHARGE, HORN_ROUT, HOLD_SHOUT, CHARGE_SHOUT, CHIME };

    /** One order and what it must sound like, how its acknowledgement must pose the troops. */
    static final class Rec {
        final String name; final int[] expect; final String ackOrder; final int at;
        int mark = -1, ackTick = -1, firstAll = -1, onAt10 = -1, onAt30 = -1, known = 0, sentOutlines;
        int[] heard; CommandNet.Client.Ack ack; int baseAcks; int firstLit = -1;
        Rec(String name, String ackOrder, int at, int... expect) { this.name = name; this.ackOrder = ackOrder; this.at = at; this.expect = expect; }
    }

    UUID id, mate, synMate, synEnemy;
    Path ctl, shots;
    boolean wantMate, mateGone, seated, listening;
    int stage, t0 = -1, waitMate;
    final Map<String, Long> events = new HashMap<>();
    volatile double cx, cz, px, pz;
    volatile boolean battleOk;
    // aim
    List<Vec3d> aimedNow = List.of();
    // lit window
    int litTicks, drawnTicks, confirmAt = -1, lastFrames, hold1Ordered;
    int pulseSeen;
    final List<Rec> recs = new ArrayList<>();
    Rec cur;
    volatile int mateOut = -1, synMateOut = -1, synEnemyOut = -1, selfOut = -1, enemyAck = -1, mateAckSent = -1, selfAckSent = -1, mateReal = -1;
    boolean evaluated, rallyShot, formationLit;
    int formationLitTick = -1;
    final List<String> shotLog = new ArrayList<>();

    static KeyBinding find(MinecraftClient c, String translationKey) {
        for (KeyBinding k : c.options.allKeys) if (k.getTranslationKey().equals(translationKey)) return k;
        return null;
    }

    @Override public int limit() { return 8000; }

    @Override public void tick(MinecraftClient c, int tick) {
        if (stage == 0) { setup(c, tick); return; }
        if (stage == 1) { waitForMate(c, tick); return; }
        int r = tick - t0;
        watch(c, r);
        run(c, r);
    }

    // ---- setup: the mate joins the lobby, then both seat on one team ----

    void setup(MinecraftClient c, int tick) {
        id = c.player.getUuid();
        CommandNet.Client.reset(); OrderOutlines.reset(); OrderFeedback.reset();
        Phase1Checks.listenSounds(c);
        String dir = System.getenv("GRAVES_FEEDBACK_MATE");
        shots = Path.of(System.getProperty("gravesandglory.repoRoot", ".")).resolve("work").resolve("cmdui-shots");
        try { Files.createDirectories(shots); } catch (Exception ignored) { }
        wantMate = dir != null && !dir.isBlank();
        if (wantMate) {
            ctl = Path.of(dir);
            CommandUiChecks.server(c, b -> {
                try (var socket = new ServerSocket(0)) {
                    int port = socket.getLocalPort(); socket.close();
                    boolean open = b.server.openToLan(GameMode.ADVENTURE, false, port);
                    Files.createDirectories(ctl);
                    Files.writeString(ctl.resolve("address.txt"), "127.0.0.1:" + port);
                    System.out.println("[fx] LAN open=" + open + " port " + port);
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            stage = 1;
        } else {
            CommandUiChecks.seatAndStart(c);
            t0 = tick; stage = 2;
        }
    }

    void waitForMate(MinecraftClient c, int tick) {
        boolean two = c.getServer() != null && c.getServer().getPlayerManager().getPlayerList().size() == 2;
        boolean joined = ctl != null && Files.exists(ctl.resolve("mate-joined.txt"));
        if (two && joined) waitMate++;
        if (waitMate >= 40) {
            c.setScreen(null);
            CommandUiChecks.server(c, b -> {
                mate = FeedbackProbe.seatMate(b, id);
                FeedbackProbe.start(b);
            });
            t0 = tick; stage = 2; return;
        }
        if (tick > 5000) { // the mate never came: run what a single client can prove, and fail the teammate check below
            CommandUiChecks.report("cu-fx-team-faint", false, "the teammate client never joined the LAN world within 5000 ticks (" + (two ? "2 players" : "1 player") + ", joined file " + joined + ")");
            wantMate = false; CommandUiChecks.seatAndStart(c); t0 = tick; stage = 2;
        }
    }

    // ---- per-tick observation ----

    void watch(MinecraftClient c, int r) {
        if (c.world == null) return;
        if (pendingShot > 0 && --pendingShot == 0) { shot(c, "rally-pose.png"); pendingShot = -1; }
        // lit window of the confirmed Formation
        if (confirmAt >= 0 && r > confirmAt && r <= confirmAt + 45) {
            int frames = OrderOutlines.litFrames;
            boolean lit = OrderOutlines.litCount() > 0;
            if (lit) { litTicks++; if (frames > lastFrames) drawnTicks++; if (hold1First < 0) hold1First = r; }
            lastFrames = frames;
            if (OrderOutlines.pulseCount() > 0) pulseSeen++;
        }
        if (cur == null) return;
        // the acknowledgement of the current order, and the troops' stance after it
        if (cur.ack == null && CommandNet.Client.acks.size() > cur.baseAcks) { cur.ack = CommandNet.Client.acks.get(CommandNet.Client.acks.size() - 1); cur.ackTick = r; }
        if (cur.ack != null) {
            int dt = r - cur.ackTick, known = 0, on = 0;
            for (int eid : cur.ack.units()) { Entity e = c.world.getEntityById(eid); if (e == null) continue; known++; if (FeedbackProbe.stance(eid).equals("rally")) on++; }
            if (dt <= 10 && cur.firstAll < 0 && known > 0 && on == known) { cur.firstAll = dt; cur.known = known; }
            if (dt == 10) cur.onAt10 = on;
            if (dt == 30) cur.onAt30 = on;
            if (cur.name.equals("follow") && dt == 4 && !rallyShot) { rallyShot = true; shotRally(c, cur.ack); }
        }
        if (cur.name.equals("formation") && cur.ack != null && !formationLit && r - cur.at <= 12 && OrderOutlines.litCount() > 0) { formationLit = true; formationLitTick = r - cur.at; }
        if (cur.firstLit < 0 && cur.name.equals("formation") && OrderOutlines.litCount() > 0) cur.firstLit = r - cur.at;
        if (r == cur.at + 16) { // sounds of this order alone: the next order is 40 ticks later
            cur.heard = new int[CUES.length];
            var log = Phase1Checks.clientSounds;
            for (int i = cur.mark; i < log.size(); i++) for (int k = 0; k < CUES.length; k++) if (log.get(i).equals(CUES[k])) cur.heard[k]++;
        }
    }

    int hold1First = -1;

    // ---- the script ----

    void run(MinecraftClient c, int r) {
        if (r == 40) {
            battleOk = GravesAndGloryClient.battleActive();
            if (!battleOk) { CommandUiChecks.report("cu-fx-battle", false, "no battle after seating"); CommandUiChecks.finish(c); return; }
            CommandUiChecks.server(c, b -> {
                var p = b.server.getPlayerManager().getPlayer(id);
                Vec3d centre = OrderIntakeProbe.centre(b, id, "infantry", p.getPos());
                cx = centre.x; cz = centre.z; px = p.getX(); pz = p.getZ();
                OrderIntakeProbe.respawning(b, id, false); OrderIntakeProbe.resetCharge(b, id); FeedbackProbe.clearThrottle();
                int team = OrderIntakeProbe.team(b, id);
                synMate = OrderIntakeProbe.addSynthetic(b, team); synEnemy = OrderIntakeProbe.addSynthetic(b, 1 - team);
                CommandNet.sent.clear();
            });
        }
        // Formation: aim, look at the slots, confirm
        if (r == 42) {
            setLook(c, 35f, 12f);
            CommandSelection.select("infantry", false);
            hold1Ordered = CommandSelection.pills().stream().filter(p -> p.id().equals("infantry")).mapToInt(CommandSelection.Pill::members).findFirst().orElse(-1);
            CommandSelection.order("hold", false);
        }
        if (r == 44 && mate != null) { UUID m = mate; CommandUiChecks.server(c, b -> FeedbackProbe.placeMate(b, m, id, 3.5, -1.5)); }
        if (r == 48) evaluateAim(c);
        if (r == 50) shot(c, "outline-aim.png");
        if (r == 52) {
            cur = new Rec("hold", "hold", 52, 0, 0, 1, 0, 0);
            begin(cur);
            confirmAt = r;
            events.put("hold1", System.currentTimeMillis());
            CommandUiChecks.press(c, find(c, "key.gravesandglory.confirm"));
        }
        if (r == 100) evaluateLit();
        // the other orders, 40 ticks apart, each with a clean throttle
        order(c, r, 110, "charge", "charge", () -> OrderIntakeProbe.send("infantry", "charge", "wedge", cx, cz, -90f), true, 1, 0, 0, 1, 0);
        order(c, r, 150, "retreat", "retreat", () -> OrderIntakeProbe.send("infantry", "retreat", "line", 0, 0, 0), false, 0, 1, 0, 0, 0);
        order(c, r, 190, "attack", "attack", () -> OrderIntakeProbe.send("infantry", "attack", "line", 0, 0, 0), false, 0, 0, 0, 0, 1);
        if (r == 226) CommandUiChecks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(id); double yw = Math.toRadians(p.getYaw());
            FeedbackProbe.bring(b, id, "infantry", p.getX() - Math.sin(yw) * 5 - 2, p.getZ() + Math.cos(yw) * 5, p.getY()); });
        order(c, r, 230, "follow", "follow", () -> OrderIntakeProbe.send("infantry", "follow", "line", 0, 0, 0), false, 0, 0, 0, 0, 1);
        order(c, r, 270, "formation", "formation", () -> OrderIntakeProbe.send("infantry", "formation", "wedge", 0, 0, 0), false, 0, 0, 1, 0, 0);
        order(c, r, 310, "multi-charge", "charge", () -> OrderIntakeProbe.sendMany("charge", "line", 0f,
            List.of(new OrderIntakeProbe.At("infantry", cx, cz), new OrderIntakeProbe.At("ranged", cx + 14, cz))), true, 1, 0, 0, 1, 0);
        order(c, r, 350, "hold2", "hold", () -> OrderIntakeProbe.send("infantry", "hold", "line", px + 6, pz, 20f), false, 0, 0, 1, 0, 0);
        if (r == 392) {
            evaluateOrders(c);
            CommandUiChecks.server(c, b -> {
                mateOut = CommandNet.sentTo(synMate, CommandNet.OUTLINE); synMateOut = mateOut;
                synEnemyOut = CommandNet.sentTo(synEnemy, CommandNet.OUTLINE); enemyAck = CommandNet.sentTo(synEnemy, CommandNet.ACK);
                selfOut = CommandNet.sentTo(id, CommandNet.OUTLINE); selfAckSent = CommandNet.sentTo(id, CommandNet.ACK);
                mateAckSent = CommandNet.sentTo(synMate, CommandNet.ACK);
                mateReal = mate == null ? -1 : CommandNet.sentTo(mate, CommandNet.OUTLINE);
            });
        }
        if (r == 396) evaluateSends();
        // the last Formation's ghost lives at most outlineShareTicks (200) on the mate: give it that and some
        if (r == 600) {
            if (wantMate) evaluateMate();
            CommandUiChecks.finish(c);
        }
    }

    void order(MinecraftClient c, int r, int at, String name, String ackOrder, Runnable send, boolean charge, int hornCharge, int hornRout, int holdShout, int chargeShout, int chime) {
        if (r == at - 3) CommandUiChecks.server(c, b -> { FeedbackProbe.clearThrottle(); OrderIntakeProbe.resetCharge(b, id); });
        if (r == at) {
            cur = new Rec(name, ackOrder, at, hornCharge, hornRout, holdShout, chargeShout, chime);
            begin(cur);
            events.put(name, System.currentTimeMillis());
            send.run();
        }
    }

    void begin(Rec rec) {
        rec.mark = Phase1Checks.clientSounds.size();
        rec.baseAcks = CommandNet.Client.acks.size();
        recs.add(rec);
    }

    // ---- the checks ----

    void evaluateAim(MinecraftClient c) {
        aimedNow = new ArrayList<>(OrderOutlines.aimed);
        Vec3d dest = FeedbackProbe.destination();
        float yaw = c.player.getYaw();
        double maxD = 0;
        if (dest != null) for (Vec3d v : aimedNow) maxD = Math.max(maxD, Math.hypot(v.x - dest.x, v.z - dest.z));
        float facing = OrderOutlines.aimedFacing;
        double diff = Math.abs(((facing - yaw) % 360 + 540) % 360 - 180);
        // the rows' own direction, from the slots alone: slots of one row (same distance along the facing) lie on the line (cos f, sin f)
        double f = Math.toRadians(yaw), fx = -Math.sin(f), fz = Math.cos(f);
        double worst = 0; int pairs = 0;
        for (int i = 0; i < aimedNow.size(); i++) for (int j = i + 1; j < aimedNow.size(); j++) {
            Vec3d a = aimedNow.get(i), b = aimedNow.get(j);
            double dx = b.x - a.x, dz = b.z - a.z, len = Math.hypot(dx, dz);
            if (len < 1.5 || Math.abs(dx * fx + dz * fz) > 0.05) continue; // not the same row
            double ang = Math.toDegrees(Math.atan2(dz, dx));
            double d = Math.abs(((ang - yaw) % 180 + 180) % 180);
            d = Math.min(d, 180 - d); // the side axis of a row is (cos yaw, sin yaw)
            worst = Math.max(worst, d); pairs++;
        }
        boolean ok = dest != null && aimedNow.size() > 0 && aimedNow.size() == hold1Ordered && maxD <= 12 && diff <= 1 && pairs > 0 && worst <= 1;
        CommandUiChecks.report("cu-fx-outline-count", ok, "aiming Formation '" + FeedbackProbe.formation() + "' with infantry of " + hold1Ordered + ": aimed.size()=" + aimedNow.size()
            + " (one slot per troop; this fixture fields " + hold1Ordered + " infantry, not the brief's 8); farthest slot " + String.format("%.1f", maxD) + " blocks from the cursor point " + (dest == null ? "none" : String.format("%.1f, %.1f", dest.x, dest.z))
            + "; facing " + String.format("%.1f", facing) + " vs player yaw " + String.format("%.1f", yaw) + " (off by " + String.format("%.2f", diff) + "); " + pairs
            + " same-row slot pairs, worst row direction error " + String.format("%.2f", worst) + " deg; aim frames drawn " + OrderOutlines.aimFrames);
    }

    void evaluateLit() {
        boolean ok = litTicks >= 18 && litTicks <= 22 && drawnTicks >= litTicks - 3 && hold1First >= 0 && hold1First - confirmAt <= 4;
        CommandUiChecks.report("cu-fx-lit", ok, "after confirm (tick " + confirmAt + ") the outline was lit on " + litTicks + " ticks (first at +" + (hold1First - confirmAt)
            + ", expected 20 +- 2), frames drew it on " + drawnTicks + " of them; pulse ring present on " + pulseSeen + " ticks; own alpha " + OrderOutlines.lastOwnAlpha);
    }

    void evaluateOrders(MinecraftClient c) {
        StringBuilder sound = new StringBuilder(), rally = new StringBuilder();
        boolean soundOk = true, rallyOk = true;
        for (Rec rec : recs) {
            if (rec.heard == null) { soundOk = false; sound.append(rec.name).append(": no sound window; "); continue; }
            boolean same = java.util.Arrays.equals(rec.heard, rec.expect);
            soundOk &= same;
            sound.append(rec.name).append(" [charge ").append(rec.heard[0]).append("/").append(rec.expect[0]).append(", rout ").append(rec.heard[1]).append("/").append(rec.expect[1])
                .append(", holdShout ").append(rec.heard[2]).append("/").append(rec.expect[2]).append(", chargeShout ").append(rec.heard[3]).append("/").append(rec.expect[3])
                .append(", chime ").append(rec.heard[4]).append("/").append(rec.expect[4]).append(same ? "] " : " MISMATCH] ");
            boolean ok = rec.ack != null && rec.ack.order().equals(rec.ackOrder) && rec.firstAll >= 0 && rec.firstAll <= 10 && rec.onAt10 > 0 && rec.onAt30 == 0;
            rallyOk &= ok;
            rally.append(rec.name).append(" [ack ").append(rec.ack == null ? "none" : rec.ack.order() + " x" + rec.ack.units().length).append(", all ").append(rec.known).append(" known troops rally at +")
                .append(rec.firstAll).append(", still raised at +10: ").append(rec.onAt10).append(", at +30: ").append(rec.onAt30).append(ok ? "] " : " FAIL] ");
        }
        CommandUiChecks.report("cu-fx-sound-shout", soundOk, "heard on this client within 16 ticks of each order, heard/expected: " + sound.toString().trim());
        CommandUiChecks.report("cu-fx-rally", rallyOk, rally.toString().trim());
        Rec formation = recs.stream().filter(x -> x.name.equals("formation")).findFirst().orElse(null);
        CommandUiChecks.report("cu-fx-formation-ack", formation != null && formation.ack != null && formation.ack.order().equals("formation") && formationLit,
            "Formation change: ACK '" + (formation == null || formation.ack == null ? "none" : formation.ack.order()) + "', lit squares under its troops " + formationLit
                + " (+" + formationLitTick + " ticks after the order); rally above");
    }

    void evaluateSends() {
        // hold, charge, multi-charge (2 groups, 2 destinations), hold again = 5 OUTLINE records to a teammate; the retreat, attack, follow and shape change send none
        boolean ok = synEnemyOut == 0 && enemyAck == 0 && synMateOut == 5 && selfOut == 0 && selfAckSent == recs.size() && mateAckSent == recs.size();
        CommandUiChecks.report("cu-fx-enemy-none", ok, "server-side sends over " + recs.size() + " orders: OUTLINE to the enemy squad " + synEnemyOut + " (ACK " + enemyAck + "), to a teammate squad "
            + synMateOut + " (expected 5; the real teammate client " + mateReal + "), to the issuer " + selfOut + " (ACK to issuer " + selfAckSent + " and teammate " + mateAckSent + ", expected " + recs.size() + " each)");
    }

    // ---- the teammate's log ----

    static final Pattern STATE = Pattern.compile("state t=(\\d+) wall=(\\d+) outlines=(\\d+) ghosts=(\\d+) slots=(\\d+) alpha=([0-9.]+) frames=(\\d+) sig=(.*)");
    static final Pattern SAMPLE = Pattern.compile("sample t=(\\d+) wall=(\\d+) ghosts=(\\d+) alpha=([0-9.]+) frames=(\\d+)");
    record State(int t, long wall, int outlines, int ghosts, int slots, float alpha, String sig) {}

    void evaluateMate() {
        List<String> lines;
        try { lines = Files.readAllLines(ctl.resolve("mate-log.txt")); }
        catch (Exception e) { CommandUiChecks.report("cu-fx-team-faint", false, "no teammate log: " + e); return; }
        List<State> states = new ArrayList<>();
        float maxAlpha = 0, minAlphaDrawn = 9; int samples = 0; String shotLine = "";
        for (String line : lines) {
            Matcher m = STATE.matcher(line);
            if (m.matches()) { states.add(new State(Integer.parseInt(m.group(1)), Long.parseLong(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Float.parseFloat(m.group(6)), m.group(8))); continue; }
            m = SAMPLE.matcher(line);
            if (m.matches()) { float a = Float.parseFloat(m.group(4)); samples++; maxAlpha = Math.max(maxAlpha, a); if (a > 0) minAlphaDrawn = Math.min(minAlphaDrawn, a); continue; }
            if (line.startsWith("shot ")) shotLine = line;
        }
        // 1. faint: ghosts were drawn at 0.35
        boolean faint = samples > 0 && Math.abs(maxAlpha - OrderOutlines.GHOST_ALPHA) < 0.006 && Math.abs(minAlphaDrawn - OrderOutlines.GHOST_ALPHA) < 0.006;
        // 2. replacement, from the shared list the teammate holds: each later order replaces the owner's earlier outlines
        String replace = replaceDetail(states);
        boolean replaced = replace.startsWith("ok");
        // 3. the last Formation's ghost is gone after the troops arrive or 200 ticks
        long hold2 = events.getOrDefault("hold2", 0L);
        State appear = null, gone = null;
        for (State s : states) if (s.wall >= hold2 - 50 && appear == null && s.ghosts >= 1 && s.wall >= hold2) appear = s;
        if (appear != null) for (State s : states) if (s.wall > appear.wall && s.ghosts == 0 && s.outlines == 0) { gone = s; break; }
        int life = appear != null && gone != null ? gone.t - appear.t : -1;
        boolean goneOk = life >= 10 && life <= 205;
        boolean shotOk = shotLine.contains("outline-team.png");
        CommandUiChecks.report("cu-fx-team-faint", faint && replaced && goneOk && shotOk,
            "teammate client: ghost slots drawn at alpha " + (samples == 0 ? "never" : String.format("%.2f..%.2f", minAlphaDrawn, maxAlpha)) + " over " + samples + " samples (expected 0.35); " + replace
                + "; last Formation's ghost lived " + (life < 0 ? "never gone/never seen" : life + " teammate ticks (gone by " + (life < 199 ? "arrival" : "the 200-tick limit") + ")") + "; screenshot: " + (shotLine.isEmpty() ? "none" : shotLine));
    }

    static State before(List<State> s, long wall) { State r = null; for (State x : s) if (x.wall < wall) r = x; return r; }
    static int maxIn(List<State> s, long from, long to, boolean ghost) {
        int m = 0; State pre = before(s, from); if (pre != null) m = ghost ? pre.ghosts : pre.outlines;
        for (State x : s) if (x.wall >= from && x.wall < to) m = Math.max(m, ghost ? x.ghosts : x.outlines);
        return m;
    }

    String replaceDetail(List<State> s) {
        long charge = events.getOrDefault("charge", 0L), multi = events.getOrDefault("multi-charge", 0L), hold2 = events.getOrDefault("hold2", 0L), hold1 = events.getOrDefault("hold1", 0L);
        State preCharge = before(s, charge), preMulti = before(s, multi), preHold2 = before(s, hold2);
        int afterHold1 = maxIn(s, hold1, charge, false), afterCharge = maxIn(s, charge, multi, false), afterMulti = maxIn(s, multi, hold2, false);
        // after the last order the owner's earlier outlines are gone and exactly one stays (until it runs out)
        int afterHold2 = maxIn(s, hold2 + 100, hold2 + 1500, false);
        int ghostsMulti = maxIn(s, multi, hold2, true);
        boolean pre = preCharge != null && preCharge.outlines >= 1 && preMulti != null && preMulti.outlines >= 1 && preHold2 != null && preHold2.outlines >= 2;
        boolean ok = pre && afterHold1 == 1 && afterCharge == 1 && afterMulti == 2 && ghostsMulti == 2 && afterHold2 == 1;
        return (ok ? "ok" : "FAIL") + " shared outlines seen: after Formation " + afterHold1 + ", after Charge (replacing it) " + afterCharge + ", after 2-group Charge (both drawn, replacing the Charge) "
            + afterMulti + " (ghosts " + ghostsMulti + "), after the next Formation (replacing both) " + afterHold2 + "; earlier ones still live when each order came: "
            + (preCharge == null ? "-" : preCharge.outlines) + "/" + (preMulti == null ? "-" : preMulti.outlines) + "/" + (preHold2 == null ? "-" : preHold2.outlines) + " (expected >=1/>=1/>=2)";
    }

    // ---- helpers ----

    static void setLook(MinecraftClient c, float yaw, float pitch) {
        c.player.setYaw(yaw); c.player.setPitch(pitch); c.player.prevYaw = yaw; c.player.prevPitch = pitch; c.player.setHeadYaw(yaw); c.player.prevHeadYaw = yaw;
    }

    void shotRally(MinecraftClient c, CommandNet.Client.Ack ack) {
        Entity best = null; double bd = 1e9;
        for (int eid : ack.units()) { Entity e = c.world.getEntityById(eid); if (e != null && e.distanceTo(c.player) < bd && e.distanceTo(c.player) > 2) { bd = e.distanceTo(c.player); best = e; } }
        if (best == null) { shotLog.add("rally-pose.png no troop in view"); return; }
        Vec3d eye = c.player.getEyePos(), to = best.getBoundingBox().getCenter().subtract(eye);
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90), pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        setLook(c, yaw, pitch);
        pendingShot = 2;
    }
    int pendingShot = -1;

    void shot(MinecraftClient c, String name) {
        try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {
            img.writeTo(shots.resolve(name));
            shotLog.add(name + " " + img.getWidth() + "x" + img.getHeight());
            System.out.println("[fx] shot " + shots.resolve(name));
        } catch (Exception e) { shotLog.add(name + " FAILED " + e); }
    }

    @Override public void cleanup(MinecraftClient c) {
        CommandSelection.reset();
        if (!shotLog.isEmpty()) System.out.println("[fx] shots: " + String.join(", ", shotLog));
        if (ctl != null) { try { Files.writeString(ctl.resolve("done.txt"), "done"); } catch (Exception ignored) { } }
        if (id == null) return;
        UUID m = synMate, e = synEnemy;
        CommandUiChecks.server(c, b -> {
            OrderIntakeProbe.respawning(b, id, false);
            if (m != null) OrderIntakeProbe.removeSynthetic(b, m);
            if (e != null) OrderIntakeProbe.removeSynthetic(b, e);
        });
    }
}
