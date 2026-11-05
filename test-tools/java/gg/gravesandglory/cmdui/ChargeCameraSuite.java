package gg.gravesandglory.cmdui;

import gg.gravesandglory.ChargeCamera;
import gg.gravesandglory.ChargeCameraProbe;
import gg.gravesandglory.CommandNet;
import gg.gravesandglory.CommandSelection;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.OrderIntakeProbe;
import gg.gravesandglory.OrderIntakeProbe.Snap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Command UI plan Task 10: the overhead charge camera. X is held and released as a real binding (KeyBinding.onKeyPressed, then held
 * pressed); the mouse turns the player through changeLookDirection, the call Mouse.updateMouse makes, so ChargeCamera.absorb sees it as a
 * real mouse. The render camera is read from the GameRenderer; the troops' height and the stored Charge from the server. The screenshot
 * charge-camera.png goes to work/cmdui-shots (never committed). Never included in releases.
 */
public final class ChargeCameraSuite implements CommandUiChecks.Suite {
    static final float LOOK_YAW = 30f, LOOK_PITCH = 10f;
    /** A mouse turn of 40 degrees right moves the cursor 8 blocks right of its start 8 blocks ahead: the arrow swings 45 degrees. */
    static final double TURN = 40 / 0.15;

    UUID id;
    Path shots;
    String shotLog = "";
    boolean initialOption;
    volatile double troopsY = Double.NaN;
    volatile float[] serverLook;
    volatile Snap stored;
    float heldPlayerYaw, heldPlayerPitch;
    double heldCamY;
    boolean heldActive, heldThird;
    int arrowBefore;
    // accessibility
    int releaseTick = -1;
    boolean lingerActive; double lingerCamY;
    // disconnect: watched from a tick listener of our own while the suite's ticks pause without a world
    static volatile ChargeCameraSuite live;
    static boolean listening;
    int disconnectStage, offlineTicks, rejoinTick = -1;
    String beforeDisconnect = "", afterDisconnect = "";

    static KeyBinding find(MinecraftClient c, String translationKey) {
        for (KeyBinding k : c.options.allKeys) if (k.getTranslationKey().equals(translationKey)) return k;
        return null;
    }

    static void hold(KeyBinding k) { KeyBinding.onKeyPressed(KeyBindingHelper.getBoundKeyOf(k)); k.setPressed(true); }

    static Vec3d camera(MinecraftClient c) { return c.gameRenderer.getCamera().getPos(); }

    /** The render camera is back at the player's eye, looking where the player looks. */
    static boolean atEye(MinecraftClient c) {
        var cam = c.gameRenderer.getCamera();
        return cam.getPos().distanceTo(c.player.getEyePos()) < 0.35 && Math.abs(MathHelper.wrapDegrees(cam.getYaw() - c.player.getYaw())) < 0.5 && Math.abs(cam.getPitch() - c.player.getPitch()) < 0.5;
    }

    static String cam(MinecraftClient c) {
        var cam = c.gameRenderer.getCamera(); var p = cam.getPos();
        return String.format("camera (%.2f, %.2f, %.2f) yaw %.1f pitch %.1f, eye (%.2f, %.2f, %.2f) yaw %.1f pitch %.1f", p.x, p.y, p.z, cam.getYaw(), cam.getPitch(),
            c.player.getEyePos().x, c.player.getEyePos().y, c.player.getEyePos().z, c.player.getYaw(), c.player.getPitch());
    }

    void freshCharge(MinecraftClient c) {
        CommandUiChecks.server(c, b -> OrderIntakeProbe.resetCharge(b, id));
        CommandNet.Client.reset();
        CommandSelection.reset();
        CommandSelection.select("infantry", false);
        c.player.setYaw(LOOK_YAW); c.player.prevYaw = LOOK_YAW; c.player.setPitch(LOOK_PITCH); c.player.prevPitch = LOOK_PITCH;
    }

    @Override public void tick(MinecraftClient c, int tick) {
        KeyBinding charge = find(c, "key.gravesandglory.charge");
        if (tick == 1) {
            id = c.player.getUuid(); live = this; listen();
            initialOption = ChargeCamera.overheadAlways;
            ChargeCamera.setOverheadAlways(false, false);
            shots = Path.of(System.getProperty("gravesandglory.repoRoot", ".")).resolve("work").resolve("cmdui-shots");
            try { Files.createDirectories(shots); } catch (Exception ignored) { }
            CommandUiChecks.seatAndStart(c);
        }
        if (tick == 40) {
            if (!GravesAndGloryClient.battleActive()) { CommandUiChecks.report("cu-cam-battle", false, "no battle after seatAndStart"); CommandUiChecks.finish(c); return; }
            CommandUiChecks.server(c, b -> OrderIntakeProbe.respawning(b, id, false));
            freshCharge(c);
        }

        // ---- 1. hold X: the camera lifts over the infantry; the mouse swings the arrow; the player's own look never changes ----
        if (tick == 50) { arrowBefore = ChargeCamera.arrowFrames; hold(charge); }
        if (tick == 58) CommandUiChecks.server(c, b -> troopsY = ChargeCameraProbe.troopsY(b, id, "infantry"));
        if (tick == 62) c.player.changeLookDirection(TURN, 0);
        if (tick == 66) CommandUiChecks.server(c, b -> serverLook = ChargeCameraProbe.serverLook(b, id));
        if (tick == 68) {
            heldActive = ChargeCamera.active() && ChargeCamera.held(); heldThird = !c.options.getPerspective().isFirstPerson();
            heldCamY = camera(c).y; heldPlayerYaw = c.player.getYaw(); heldPlayerPitch = c.player.getPitch();
            shot(c, "charge-camera.png");
        }
        if (tick == 69) {
            float[] sl = serverLook;
            boolean high = heldActive && !Double.isNaN(troopsY) && heldCamY >= troopsY + 15;
            boolean lookKept = heldPlayerYaw == LOOK_YAW && heldPlayerPitch == LOOK_PITCH && sl != null && Math.abs(MathHelper.wrapDegrees(sl[0] - LOOK_YAW)) < 0.01 && Math.abs(sl[1] - LOOK_PITCH) < 0.01;
            boolean arrow = ChargeCamera.arrowFrames > arrowBefore;
            CommandUiChecks.report("cu-cam-lift", high && lookKept && arrow && heldThird,
                String.format("X held: active=%s, third-person view (no held items, no crosshair)=" + heldThird + ", camera y %.2f vs infantry feet %.2f (+%.2f); player look while the mouse turned %.2f/%.2f, server %s; arrow frames %d; %s",
                    heldActive, heldCamY, troopsY, heldCamY - troopsY, heldPlayerYaw, heldPlayerPitch, sl == null ? "null" : sl[0] + "/" + sl[1], ChargeCamera.arrowFrames - arrowBefore, shotLog));
        }
        // ---- 2. release: the Charge goes along the arrow and the camera is back at the eye within 2 ticks ----
        if (tick == 70) { charge.setPressed(false); releaseTick = tick; }
        if (tick == 72) {
            boolean back = !ChargeCamera.active() && atEye(c);
            boolean exact = c.player.getYaw() == LOOK_YAW && c.player.getPitch() == LOOK_PITCH && c.options.getPerspective().isFirstPerson();
            CommandUiChecks.report("cu-cam-snap", back && exact, "2 ticks after release: active=" + ChargeCamera.active() + ", " + cam(c) + "; player look and first-person view restored exactly=" + exact);
        }
        if (tick == 80) CommandUiChecks.server(c, b -> stored = OrderIntakeProbe.snap(b, id, "infantry"));
        if (tick == 84) {
            Snap s = stored;
            float arrow = ChargeCamera.direction();
            double storedYaw = s == null || s.chargeDir() == null ? Double.NaN : Math.toDegrees(Math.atan2(-s.chargeDir().x, s.chargeDir().z));
            double off = Math.abs(MathHelper.wrapDegrees(storedYaw - arrow)), want = Math.abs(MathHelper.wrapDegrees(arrow - (LOOK_YAW + 45)));
            CommandUiChecks.report("cu-cam-direction", s != null && s.order().equals("charge") && off <= 5 && want <= 1,
                String.format("arrow yaw %.2f (look %.0f + the 45 degree mouse swing: off %.2f); stored %s, its yaw %.2f, %.2f degrees from the arrow", arrow, LOOK_YAW, want, s, storedYaw, off));
        }

        // ---- 3. accessibility: with the option on, the camera stays overhead after the release until a key is pressed ----
        if (tick == 90) { freshCharge(c); ChargeCamera.setOverheadAlways(true, false); }
        if (tick == 95) hold(charge);
        if (tick == 105) { charge.setPressed(false); releaseTick = tick; }
        if (tick == 108) CommandUiChecks.server(c, b -> troopsY = ChargeCameraProbe.troopsY(b, id, "infantry"));
        if (tick == releaseTick + 61 && tick > 105) {
            lingerActive = ChargeCamera.active() && !ChargeCamera.held(); lingerCamY = camera(c).y;
            CommandUiChecks.press(c, c.options.sneakKey);
        }
        if (tick == 170) {
            boolean stayed = lingerActive && lingerCamY >= troopsY + 15;
            boolean back = !ChargeCamera.active() && atEye(c);
            CommandUiChecks.report("cu-cam-accessibility", stayed && back,
                String.format("option on: 61 ticks after release active=%s, camera y %.2f vs infantry feet %.2f; after a key press: active=%s, %s", lingerActive, lingerCamY, troopsY, ChargeCamera.active(), cam(c)));
        }

        // ---- 4. the option's toggle: a binding in the Graves and Glory controls; the choice is saved in the client options file ----
        if (tick == 172) {
            ChargeCamera.setOverheadAlways(false, false);
            KeyBinding t = find(c, "key.gravesandglory.charge_overhead");
            c.options.setKeyCode(t, InputUtil.fromTranslationKey("key.keyboard.f9")); KeyBinding.updateKeysByCode();
            CommandUiChecks.press(c, t);
        }
        if (tick == 178) {
            KeyBinding t = find(c, "key.gravesandglory.charge_overhead");
            String text = "";
            try { text = Files.readString(GravesAndGloryClient.toolbarFlag()); } catch (Exception e) { text = "unreadable: " + e; }
            boolean on = ChargeCamera.overheadAlways, saved = text.contains("chargeCameraOverheadAlways=true"), kept = text.contains("toolbarKeysFreed=true");
            boolean category = t != null && t.getCategory().equals("category.gravesandglory") && t.getDefaultKey().equals(InputUtil.UNKNOWN_KEY);
            CommandUiChecks.report("cu-cam-toggle", on && saved && kept && category,
                "toggle pressed: option on=" + on + ", saved in " + GravesAndGloryClient.toolbarFlag().getFileName() + "=" + saved + ", other lines kept=" + kept + ", binding in the Graves and Glory category, unbound by default=" + category);
            c.options.setKeyCode(t, InputUtil.UNKNOWN_KEY); KeyBinding.updateKeysByCode();
            ChargeCamera.setOverheadAlways(false, true);
        }

        // ---- 5. disconnect with the camera up; after the rejoin it is inactive ----
        if (tick == 182) freshCharge(c);
        if (tick == 186) hold(charge);
        if (tick == 200) {
            beforeDisconnect = "active=" + ChargeCamera.active() + " held=" + ChargeCamera.held();
            disconnectStage = 1;
            c.world.disconnect();
            c.disconnect(new gg.gravesandglory.FrontMenuScreen());
        }
        if (tick >= 201 && disconnectStage == 3 && rejoinTick < 0) rejoinTick = tick;
        if (rejoinTick > 0 && tick == rejoinTick + 20) {
            boolean ok = beforeDisconnect.equals("active=true held=true") && afterDisconnect.equals("active=false held=false") && !ChargeCamera.active() && !ChargeCamera.held() && atEye(c) && c.options.getPerspective().isFirstPerson();
            CommandUiChecks.report("cu-cam-disconnect", ok, "before: " + beforeDisconnect + "; without a world: " + afterDisconnect + "; after rejoin: active=" + ChargeCamera.active() + ", first person=" + c.options.getPerspective().isFirstPerson() + ", " + cam(c));
            CommandUiChecks.finish(c);
        }
    }

    void shot(MinecraftClient c, String name) {
        try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {
            img.writeTo(shots.resolve(name));
            shotLog = name + " " + img.getWidth() + "x" + img.getHeight();
        } catch (Exception e) { shotLog = name + " FAILED " + e; }
    }

    /** A tick listener of the suite's own: the suite's ticks stop while there is no world, so this one watches the disconnect and rejoins. */
    static void listen() {
        if (listening) return;
        listening = true;
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            ChargeCameraSuite s = live;
            if (s == null || s.disconnectStage == 0 || s.disconnectStage == 3) return;
            if (s.disconnectStage == 1 && c.world == null && c.player == null) {
                if (++s.offlineTicks < 10) return;
                s.afterDisconnect = "active=" + ChargeCamera.active() + " held=" + ChargeCamera.held();
                s.disconnectStage = 2;
                try {
                    Method m = GravesAndGloryClient.class.getDeclaredMethod("startCamp", MinecraftClient.class, boolean.class);
                    m.setAccessible(true);
                    m.invoke(null, c, false);
                } catch (Exception e) { CommandUiChecks.report("cu-cam-disconnect", false, "rejoin failed: " + e); s.disconnectStage = 3; c.scheduleStop(); }
            } else if (s.disconnectStage == 2 && c.player != null && c.world != null) s.disconnectStage = 3;
        });
    }

    @Override public void cleanup(MinecraftClient c) {
        KeyBinding charge = find(c, "key.gravesandglory.charge");
        if (charge != null) charge.setPressed(false);
        KeyBinding t = find(c, "key.gravesandglory.charge_overhead");
        if (t != null && !t.isUnbound()) { c.options.setKeyCode(t, InputUtil.UNKNOWN_KEY); KeyBinding.updateKeysByCode(); }
        ChargeCamera.setOverheadAlways(initialOption, true);
        CommandSelection.reset();
        live = null;
    }
}
