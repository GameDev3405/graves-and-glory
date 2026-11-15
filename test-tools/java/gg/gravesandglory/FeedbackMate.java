package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * The teammate client of cmdui.FeedbackSuite (command UI plan Task 9). Inert unless -Dgravesandglory.feedbackMate=&lt;control dir&gt; is set. It
 * joins the host's LAN world (address.txt in the control dir), and writes what a teammate sees of the host's orders to mate-log.txt there: every
 * change in the shared outline list and in the ghosts OrderOutlines draws, with the opacity of the last ghost slot drawn. It turns to the first
 * ghost it sees and saves outline-team.png (to &lt;repo&gt;/work/cmdui-shots, never committed). It stops when done.txt appears. Never included in releases.
 */
public final class FeedbackMate implements ClientModInitializer {
    int clock, inWorld, offline, lookedAt = -1, shotAt = -1;
    boolean started, shot, joined, disconnected;
    int armed, goneTicks;
    int[] lastAckUnits = new int[0];
    int lastN = -1, lastG = -1, lastS = -1;
    String lastSig = "";
    Path control;

    void log(String line) {
        try { Files.writeString(control.resolve("mate-log.txt"), line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (Exception e) { System.out.println("[mate] cannot write log: " + e); }
    }

    @Override public void onInitializeClient() {
        String dir = System.getProperty("gravesandglory.feedbackMate", "");
        if (dir.isBlank()) return;
        control = Path.of(dir);
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            try { step(c); } catch (Throwable e) { log("exception " + e); e.printStackTrace(); }
        });
    }

    void step(MinecraftClient c) throws Exception {
        c.options.pauseOnLostFocus = false;
        clock++;
        if (Files.exists(control.resolve("done.txt")) || clock > 20 * 60 * 25) { log("stop clock=" + clock); c.scheduleStop(); return; }
        if (c.getOverlay() != null) return;
        if (c.currentScreen instanceof net.minecraft.client.gui.screen.AccessibilityOnboardingScreen s) { s.close(); return; }
        if (!started) {
            if (!(c.currentScreen instanceof FrontMenuScreen) || clock < 80 || !Files.exists(control.resolve("address.txt"))) return;
            started = true;
            String address = Files.readString(control.resolve("address.txt")).trim();
            log("connecting " + address);
            ConnectScreen.connect(new FrontMenuScreen(), c, ServerAddress.parse(address), new ServerInfo("Feedback host", address, ServerInfo.ServerType.OTHER), false, null);
            return;
        }
        if (disconnected) { afterDisconnect(c); return; }
        if (c.player == null || c.world == null || !GravesAndGloryClient.ready) {
            if (joined && ++offline == 100) { log("disconnected"); c.scheduleStop(); }
            return;
        }
        offline = 0;
        if (!joined) { joined = true; log("joined"); try { Files.writeString(control.resolve("mate-joined.txt"), "joined"); } catch (Exception ignored) { } }
        if (c.currentScreen != null && GravesAndGloryClient.battleActive()) c.setScreen(null);
        inWorld++;
        if (Files.exists(control.resolve("disconnect.txt"))) { // armed: leave the server for real once the host's last order shows everywhere (or after 60 ticks)
            armed++;
            int rally = rallyingUnits();
            if ((OrderOutlines.ghostOutlines() >= 1 && OrderOutlines.pulseCount() >= 1 && rally >= 1) || armed >= 60) {
                log("predisc " + state() + " armed=" + armed);
                disconnected = true;
                c.world.disconnect(); c.disconnect();
                return;
            }
        }
        var outlines = CommandNet.Client.outlines;
        int n = outlines.size(), g = OrderOutlines.ghostOutlines(), slots = OrderOutlines.ghostCount();
        StringBuilder sig = new StringBuilder();
        for (var o : outlines) sig.append(String.format("%s@%.0f,%.0f/%d;", o.formation(), o.x(), o.z(), o.groups().size()));
        if (n != lastN || g != lastG || slots != lastS || !sig.toString().equals(lastSig)) {
            lastN = n; lastG = g; lastS = slots; lastSig = sig.toString();
            log(String.format("state t=%d wall=%d outlines=%d ghosts=%d slots=%d alpha=%.2f frames=%d sig=%s", inWorld, System.currentTimeMillis(), n, g, slots,
                OrderOutlines.lastTeamAlpha, OrderOutlines.ghostFrames, sig));
        }
        if (g >= 1 && inWorld % 10 == 0) log(String.format("sample t=%d wall=%d ghosts=%d alpha=%.2f frames=%d", inWorld, System.currentTimeMillis(), g, OrderOutlines.lastTeamAlpha, OrderOutlines.ghostFrames));
        if (!shot && g >= 1 && OrderOutlines.ghostFrames >= 3 && lookedAt < 0 && !outlines.isEmpty()) {
            var o = outlines.get(0);
            var eye = c.player.getEyePos();
            double dx = o.x() - eye.x, dz = o.z() - eye.z, d = Math.max(3, Math.hypot(dx, dz));
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90), pitch = (float) Math.toDegrees(Math.atan2(c.player.getEyeHeight(c.player.getPose()) + 0.5, d));
            c.player.setYaw(yaw); c.player.setPitch(pitch); c.player.prevYaw = yaw; c.player.prevPitch = pitch; c.player.setHeadYaw(yaw); c.player.prevHeadYaw = yaw;
            lookedAt = inWorld;
            log(String.format("look t=%d at %.1f,%.1f from %.1f,%.1f yaw %.1f pitch %.1f", inWorld, o.x(), o.z(), eye.x, eye.z, yaw, pitch));
        }
        if (!shot && lookedAt >= 0 && inWorld >= lookedAt + 4) {
            shot = true;
            Path dir = Path.of(System.getProperty("gravesandglory.repoRoot", ".")).resolve("work").resolve("cmdui-shots");
            Files.createDirectories(dir);
            try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {
                img.writeTo(dir.resolve("outline-team.png"));
                log(String.format("shot t=%d outline-team.png %dx%d alpha=%.2f ghosts=%d", inWorld, img.getWidth(), img.getHeight(), OrderOutlines.lastTeamAlpha, g));
            }
        }
    }

    /** How many of the last acknowledgement's troops this client would still pose with a raised weapon. */
    int rallyingUnits() {
        var acks = CommandNet.Client.acks;
        if (!acks.isEmpty()) lastAckUnits = acks.get(acks.size() - 1).units();
        int n = 0;
        for (int id : lastAckUnits) if (OrderFeedback.rallying(id)) n++;
        return n;
    }

    /** Every piece of command-UI state the client keeps for orders, on one line. */
    String state() {
        return String.format("outlines=%d ghosts=%d slots=%d lit=%d pulses=%d acks=%d rally=%d", CommandNet.Client.outlines.size(), OrderOutlines.ghostOutlines(), OrderOutlines.ghostCount(),
            OrderOutlines.litCount(), OrderOutlines.pulseCount(), CommandNet.Client.acks.size(), rallyingUnits());
    }

    /** After the disconnect: once the world has been gone a few ticks (the HUD reset runs on a tick), log the same state; it must all be empty. */
    void afterDisconnect(MinecraftClient c) {
        if (c.world != null) return;
        goneTicks++;
        if (goneTicks == 4) log("postdisc " + state());
        if (goneTicks >= 100) { log("disconnected"); c.scheduleStop(); }
    }
}
