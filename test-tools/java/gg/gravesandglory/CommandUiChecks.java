package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Isolated real-client checks for the command UI. -Dgravesandglory.commandUiChecks=a,b,c (env GRAVES_COMMAND_UI_CHECKS) names suites;
 * each loads as gg.gravesandglory.cmdui.NameSuite, so a suite file needs no registry edit. Results go to
 * run/command-ui/results.txt as "PASS|FAIL check detail". Never included in releases.
 */
public final class CommandUiChecks implements ClientModInitializer {
    public interface Suite {
        /** Called on the client thread once per client tick; tick counts from 1 for this suite. */
        void tick(MinecraftClient c, int tick);
        /** Optional: called on the server thread after every server tick while the suite runs, for exact tick timing. */
        default void serverTick(GravesAndGlory.Battle b) {}
        /** Called once on the client thread when the suite ends by any path (finished, timed out or thrown), before the next suite starts. */
        void cleanup(MinecraftClient c);
        /** Client ticks the suite may take before it is failed as stuck (one tick is 50 ms). */
        default int limit() { return 2400; }
    }

    static final ArrayDeque<String> queue = new ArrayDeque<>();
    static final Map<KeyBinding, Integer> pressed = new HashMap<>();
    static volatile Suite current;
    static volatile String currentName = "";
    static volatile boolean failed, broken, finishing;
    static int tick, front, now, finishWait;
    static boolean started;
    static Path results;
    static final int FINISH_WAIT_TICKS = 200; // 10 s for the lobby return between queued suites

    /** One evidence line, appended to results.txt, plus a "[cu]" log line and the shared MeltyTest evidence line. */
    public static synchronized void report(String check, boolean pass, String detail) {
        if (!pass) failed = true;
        String why = detail == null ? "" : detail.replace('\r', ' ').replace('\n', ' ');
        MeltyTest.report(check, pass, why);
        System.out.println("[cu] " + check + "=" + pass + " " + why);
        if (results == null) return;
        try {
            Files.createDirectories(results.getParent());
            Files.writeString(results, (pass ? "PASS " : "FAIL ") + check + (why.isEmpty() ? "" : " " + why) + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) { System.out.println("[cu] cannot write " + results + ": " + e); }
    }

    /** Runs action on the server thread with the live battle. A throw is reported as cu-suite-exception and ends the suite. */
    public static void server(MinecraftClient c, Consumer<GravesAndGlory.Battle> action) {
        if (c.getServer() == null) return;
        c.getServer().execute(() -> {
            if (GravesAndGlory.battle == null) return;
            try { action.accept(GravesAndGlory.battle); }
            catch (Throwable e) { report("cu-" + key(currentName) + "-exception", false, e.toString()); e.printStackTrace(); broken = true; }
        });
    }

    /** Seats the player, starts the round and ends the intro (during which tactical refuses orders), as CommandPlaytest tick 1 does. */
    public static void seatAndStart(MinecraftClient c) {
        c.setScreen(null);
        UUID id = c.player.getUuid();
        server(c, b -> {
            var s = b.squads.get(id);
            s.seated = true; // a lobby join is unseated (AdmissionPolicy) and startRound fields seated squads only
            b.startRound();
            b.campaign.introUntil = b.time;
        });
    }

    /** Presses a binding for exactly one input pass through KeyBinding.onKeyPressed, the path a real key takes: pressed during client tick n, seen by the input handling at the start of tick n+1, released at the end of tick n+1. */
    public static void press(MinecraftClient c, KeyBinding k) {
        KeyBinding.onKeyPressed(KeyBindingHelper.getBoundKeyOf(k));
        k.setPressed(true);
        pressed.put(k, now);
    }

    /** Ends the running suite: cleanup, then the cu-suite line. Suites call this when their last check is in. */
    public static void finish(MinecraftClient c) { finish(c, currentName); }

    static void finish(MinecraftClient c, String name) {
        var done = current;
        if (done == null) return;
        current = null;
        try { done.cleanup(c); } catch (Throwable e) { report("cu-" + key(name) + "-exception", false, "cleanup: " + e); e.printStackTrace(); }
        for (var k : pressed.keySet()) k.setPressed(false);
        pressed.clear();
        report("cu-" + key(name) + "-suite", !failed, failed ? "an earlier cu check failed" : "all checks passed");
        server(c, b -> { if (b.running) b.campaign.returnLobby(); }); // the next suite starts from the lobby
        finishing = true;
        if (queue.isEmpty()) c.scheduleStop();
    }

    static String key(String name) { return name.toLowerCase(Locale.ROOT); }

    static Suite load(String name) throws ReflectiveOperationException {
        String simple = name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return (Suite) Class.forName("gg.gravesandglory.cmdui." + simple + "Suite").getDeclaredConstructor().newInstance();
    }

    @Override public void onInitializeClient() {
        String list = System.getProperty("gravesandglory.commandUiChecks", "");
        if (list.isBlank()) list = System.getenv().getOrDefault("GRAVES_COMMAND_UI_CHECKS", "");
        if (list.isBlank()) return;
        for (String name : list.split(",")) if (!name.isBlank()) queue.add(name.trim());
        results = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("command-ui").resolve("results.txt");
        try { Files.createDirectories(results.getParent()); Files.deleteIfExists(results); } catch (Exception e) { System.out.println("[cu] cannot reset " + results + ": " + e); }
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            var suite = current;
            if (suite == null || GravesAndGlory.battle == null) return;
            try { suite.serverTick(GravesAndGlory.battle); }
            catch (Throwable e) { report("cu-" + key(currentName) + "-exception", false, e.toString()); e.printStackTrace(); broken = true; }
        });
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            c.options.pauseOnLostFocus = false; // an unfocused test window must not pause the integrated server
            if (c.getOverlay() != null) return;
            if (c.currentScreen instanceof net.minecraft.client.gui.screen.AccessibilityOnboardingScreen s) { s.close(); return; } // a fresh test root has no options.txt
            if (!started) { if (c.currentScreen instanceof FrontMenuScreen && ++front == 40) { started = true; GravesAndGloryClient.startCamp(c, false); } return; }
            if (c.player == null || c.getServer() == null || !GravesAndGloryClient.ready) return;
            now++;
            pressed.entrySet().removeIf(e -> { if (e.getValue() < now) { e.getKey().setPressed(false); return true; } return false; });
            if (current == null) {
                if (queue.isEmpty()) return;
                if (finishing) { // wait for the previous round to end, but not forever
                    if (GravesAndGloryClient.battleActive()) {
                        if (++finishWait <= FINISH_WAIT_TICKS) return;
                        finishWait = 0;
                        String skipped = queue.poll();
                        report("cu-" + key(skipped) + "-suite", false, "the previous round was still running " + FINISH_WAIT_TICKS + " ticks after its suite ended; suite not run");
                        if (queue.isEmpty()) c.scheduleStop();
                        return;
                    }
                    finishing = false; finishWait = 0;
                }
                currentName = queue.poll(); failed = false; broken = false; tick = 0;
                try { current = load(currentName); }
                catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    report("cu-" + key(currentName) + "-suite", false, "unknown suite " + currentName + "Suite: " + e);
                    if (queue.isEmpty()) c.scheduleStop();
                    return;
                }
            }
            if (broken) { broken = false; finish(c, currentName); return; }
            try {
                if (++tick > current.limit()) { report("cu-" + key(currentName) + "-timeout", false, "no result after " + current.limit() + " ticks"); finish(c, currentName); return; }
                current.tick(c, tick);
            } catch (Throwable e) {
                report("cu-" + key(currentName) + "-exception", false, e.toString()); e.printStackTrace();
                finish(c, currentName);
            }
        });
    }
}
