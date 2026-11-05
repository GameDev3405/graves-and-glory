package gg.gravesandglory.cmdui;

import gg.gravesandglory.ChargeCamera;
import gg.gravesandglory.CommandMode;
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

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Command UI plan Task 13: one battle played with the real keys, in the order a player would. R opens the panel, key 2 picks Archers, B opens the
 * formation tray, key 1 chooses Line, a click on the ground confirms it; then Shift-Z on Infantry, X held and released with the overhead camera, C and
 * V. Every key is a real binding (CommandUiChecks.press, or held like ChargeCameraSuite does) and the click is the attack binding the mouse button
 * fires (CommandInputMixin turns it into the confirmation while an order is aimed). The server's stored assignments (OrderIntakeProbe.snap) are read
 * after each step, and must carry the orders in this order with strictly rising issue times. Then the client disconnects and rejoins: the panel, the tray,
 * the aim, the camera and the selection must come back clean. Never included in releases.
 */
public final class EndToEndSuite implements CommandUiChecks.Suite {
    UUID id;
    final Snap[] step = new Snap[6]; // 1 archers hold, 2 infantry attack, 3 charge, 4 follow, 5 retreat
    final String[] ui = new String[6];
    volatile Snap archersBefore;
    boolean chargeCamera;
    String xDiag = "";
    // disconnect: watched from a tick listener of our own while the suite's ticks pause without a world
    static volatile EndToEndSuite live;
    static boolean listening;
    int disconnectStage, offlineTicks, rejoinTick = -1;
    String before = "";

    static KeyBinding find(MinecraftClient c, String translationKey) {
        for (KeyBinding k : c.options.allKeys) if (k.getTranslationKey().equals(translationKey)) return k;
        return null;
    }

    static void hold(KeyBinding k) { KeyBinding.onKeyPressed(KeyBindingHelper.getBoundKeyOf(k)); k.setPressed(true); }

    void snap(MinecraftClient c, int n, String group) { CommandUiChecks.server(c, b -> step[n] = OrderIntakeProbe.snap(b, id, group)); }

    static String ui(MinecraftClient c) {
        return "panel " + CommandSelection.panelOpen() + ", tray " + CommandSelection.trayOpen() + ", selected " + CommandSelection.selected() + ", aiming '" + CommandSelection.aiming() + "'";
    }

    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) { id = c.player.getUuid(); live = this; listen(); CommandUiChecks.seatAndStart(c); }
        if (tick == 40) {
            if (!GravesAndGloryClient.battleActive()) { CommandUiChecks.report("cu-e2e-battle", false, "no battle after seatAndStart"); CommandUiChecks.finish(c); return; }
            CommandUiChecks.server(c, b -> { OrderIntakeProbe.respawning(b, id, false); OrderIntakeProbe.resetCharge(b, id); archersBefore = OrderIntakeProbe.snap(b, id, "ranged"); });
            CommandNet.Client.reset();
            CommandSelection.reset();
            c.player.setYaw(0f); c.player.prevYaw = 0f; c.player.setPitch(45f); c.player.prevPitch = 45f;
        }

        // 1. R, then 2 (Archers), B (the tray), 1 (Line), then a click on the ground
        if (tick == 45) CommandUiChecks.press(c, find(c, "key.gravesandglory.menu"));
        if (tick == 49) ui[0] = "R: " + ui(c);
        if (tick == 50) CommandUiChecks.press(c, c.options.hotbarKeys[1]);
        if (tick == 54) ui[1] = "2: " + ui(c);
        if (tick == 56) CommandUiChecks.press(c, find(c, "key.gravesandglory.formation"));
        if (tick == 60) ui[2] = "B: " + ui(c);
        if (tick == 62) CommandUiChecks.press(c, c.options.hotbarKeys[0]);
        if (tick == 68) ui[3] = "1: " + ui(c);
        if (tick == 70) CommandUiChecks.press(c, c.options.attackKey); // the left mouse button's binding: the click that confirms the aim
        if (tick == 78) { ui[4] = "click: " + ui(c); snap(c, 1, "ranged"); }
        if (tick == 80) {
            boolean ok = CommandSelection.panelOpen() == false; // nothing but the confirmation closes the panel here (Shift was not held)
            CommandUiChecks.report("cu-e2e-panel-flow", ui[0].contains("panel true") && ui[1].contains("selected [ranged]") && ui[2].contains("tray true")
                && ui[3].contains("aiming 'hold'") && ui[3].contains("tray false") && ui[4].contains("aiming ''") && ok,
                ui[0] + " | " + ui[1] + " | " + ui[2] + " | " + ui[3] + " | " + ui[4]);
        }

        // 2. Shift-Z on Infantry: R, key 1 picks Infantry, Z with Shift held leaves the panel open
        if (tick == 84) CommandUiChecks.press(c, find(c, "key.gravesandglory.menu"));
        if (tick == 88) CommandUiChecks.press(c, c.options.hotbarKeys[0]);
        if (tick == 92) { ui[5] = "key 1: " + ui(c); c.options.sneakKey.setPressed(true); CommandUiChecks.press(c, find(c, "key.gravesandglory.attack")); }
        if (tick == 98) { c.options.sneakKey.setPressed(false); snap(c, 2, "infantry"); }
        if (tick == 100) {
            CommandUiChecks.report("cu-e2e-shift-z", ui[5].contains("selected [infantry]") && CommandSelection.panelOpen(),
                ui[5] + "; after Shift-Z the panel is open=" + CommandSelection.panelOpen());
        }

        // 3. X held with the camera, then released
        if (tick == 104) { ui[0] = "before X: " + ui(c) + ", charge ticks left " + CommandNet.Client.chargeTicksLeft(); hold(find(c, "key.gravesandglory.charge")); }
        if (tick > 104 && tick < 114) find(c, "key.gravesandglory.charge").setPressed(true); // a real held key stays down in the OS poll; the harness re-asserts it each tick
        if (tick == 108) xDiag = "4 ticks into X: " + ui(c) + ", camera active=" + ChargeCamera.active() + " held=" + ChargeCamera.held() + ", pressed=" + find(c, "key.gravesandglory.charge").isPressed();
        if (tick == 112) chargeCamera = ChargeCamera.active() && ChargeCamera.held();
        if (tick == 114) find(c, "key.gravesandglory.charge").setPressed(false);
        if (tick == 128) snap(c, 3, "infantry");

        // 4. C and V
        if (tick == 132) CommandUiChecks.press(c, find(c, "key.gravesandglory.follow"));
        if (tick == 142) snap(c, 4, "infantry");
        if (tick == 146) CommandUiChecks.press(c, find(c, "key.gravesandglory.retreat"));
        if (tick == 156) snap(c, 5, "infantry");

        if (tick == 160) {
            Snap a = step[1], atk = step[2], ch = step[3], fo = step[4], re = step[5];
            boolean shapes = a != null && a.order().equals("hold") && a.formation().equals(CommandSelection.SHAPES[0]);
            boolean orders = atk != null && atk.order().equals("attack") && ch != null && ch.order().equals("charge") && ch.chargeDir() != null
                && fo != null && fo.order().equals("follow") && re != null && re.order().equals("retreat") && re.thenFollow();
            boolean rising = a != null && atk != null && ch != null && fo != null && re != null && a.issued() < atk.issued() && atk.issued() < ch.issued() && ch.issued() < fo.issued() && fo.issued() < re.issued();
            CommandUiChecks.report("cu-e2e-orders", shapes && orders && rising && chargeCamera,
                "[" + ui[0] + "; " + xDiag + "] Archers (B, 1, click): " + a + " [was " + archersBefore + "]; Infantry Shift-Z: " + atk + "; X with camera (lifted=" + chargeCamera + "): " + ch + "; C: " + fo + "; V: " + re
                + "; formation Line=" + shapes + ", orders in order=" + orders + ", issue times rising=" + rising);
            before = ui(c);
            disconnectStage = 1;
            c.world.disconnect();
            c.disconnect(new gg.gravesandglory.FrontMenuScreen());
        }
        if (tick >= 161 && disconnectStage == 3 && rejoinTick < 0) rejoinTick = tick;
        if (rejoinTick > 0 && tick == rejoinTick + 20) {
            boolean clean = !CommandSelection.panelOpen() && !CommandSelection.trayOpen() && CommandSelection.selected().equals(List.of("all")) && !CommandMode.active()
                && !ChargeCamera.active() && !ChargeCamera.held() && CommandNet.Client.chargeTicksLeft() == 0 && c.options.getPerspective().isFirstPerson(); // the rejoin lands on the camp's home screen, which is fine
            CommandUiChecks.report("cu-e2e-rejoin", clean, "before the disconnect: " + before + "; after the rejoin: " + ui(c) + ", camera active=" + ChargeCamera.active()
                + ", charge ticks left " + CommandNet.Client.chargeTicksLeft() + ", first person " + c.options.getPerspective().isFirstPerson() + ", screen " + c.currentScreen);
            CommandUiChecks.finish(c);
        }
    }

    /** A tick listener of the suite's own: the suite's ticks stop while there is no world, so this one watches the disconnect and rejoins. */
    static void listen() {
        if (listening) return;
        listening = true;
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            EndToEndSuite s = live;
            if (s == null || s.disconnectStage == 0 || s.disconnectStage == 3) return;
            if (s.disconnectStage == 1 && c.world == null && c.player == null) {
                if (++s.offlineTicks < 10) return;
                s.disconnectStage = 2;
                try {
                    Method m = GravesAndGloryClient.class.getDeclaredMethod("startCamp", MinecraftClient.class, boolean.class);
                    m.setAccessible(true);
                    m.invoke(null, c, false);
                } catch (Exception e) { CommandUiChecks.report("cu-e2e-rejoin", false, "rejoin failed: " + e); s.disconnectStage = 3; c.scheduleStop(); }
            } else if (s.disconnectStage == 2 && c.player != null && c.world != null) s.disconnectStage = 3;
        });
    }

    @Override public void cleanup(MinecraftClient c) {
        c.options.sneakKey.setPressed(false);
        KeyBinding charge = find(c, "key.gravesandglory.charge");
        if (charge != null) charge.setPressed(false);
        CommandSelection.reset();
        live = null;
    }
}
