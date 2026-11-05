package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandNet;
import gg.gravesandglory.CommandSelection;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.OrderIntakeProbe;
import gg.gravesandglory.OrderIntakeProbe.Snap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.util.List;
import java.util.UUID;

/**
 * Command UI plan Task 4: the command keys and the selection model. Every key is a real binding pressed through CommandUiChecks.press (the path a
 * physical key takes); the server's stored assignments (OrderIntakeProbe.snap) show what each key ordered. Never included in releases.
 */
public final class KeysSuite implements CommandUiChecks.Suite {
    UUID id;
    volatile Snap infBefore, rngBefore, followed;
    String lastBefore = "";
    String aimedCharge = "", aimedHold = "";
    boolean chargeCooling;
    boolean openedByKey, closedByKey, openedNoScreen, shiftStayedOpen;

    static KeyBinding find(MinecraftClient c, String translationKey) {
        for (KeyBinding k : c.options.allKeys) if (k.getTranslationKey().equals(translationKey)) return k;
        return null;
    }

    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) { id = c.player.getUuid(); CommandUiChecks.seatAndStart(c); }
        if (tick == 40) {
            if (!GravesAndGloryClient.battleActive()) { CommandUiChecks.report("cu-keys-battle", false, "no battle after seatAndStart"); CommandUiChecks.finish(c); return; }
            CommandUiChecks.server(c, b -> { OrderIntakeProbe.respawning(b, id, false); OrderIntakeProbe.resetCharge(b, id); });
            CommandSelection.reset();
        }
        if (tick == 41) defaults(c);

        // R toggles the panel and opens no Screen
        if (tick == 45) CommandUiChecks.press(c, find(c, "key.gravesandglory.menu"));
        if (tick == 50) { openedByKey = CommandSelection.panelOpen(); openedNoScreen = c.currentScreen == null; }
        if (tick == 52) CommandUiChecks.press(c, find(c, "key.gravesandglory.menu"));
        if (tick == 58) {
            closedByKey = !CommandSelection.panelOpen();
            CommandUiChecks.report("cu-keys-panel-toggle", openedByKey && openedNoScreen && closedByKey && c.currentScreen == null,
                "R opened the panel=" + openedByKey + " with no Screen=" + openedNoScreen + "; R again closed it=" + closedByKey);
        }

        // Number keys pick pills only while the panel is open, and never reach the relics then
        if (tick == 60) { CommandSelection.setPanelOpen(true); CommandSelection.select("all", false); lastBefore = GravesAndGloryClient.lastSent; CommandUiChecks.press(c, c.options.hotbarKeys[1]); }
        if (tick == 66) {
            boolean picked = CommandSelection.selected().equals(List.of("ranged"));
            boolean noRelic = GravesAndGloryClient.lastSent.equals(lastBefore);
            CommandSelection.setPanelOpen(false); CommandSelection.select("infantry", false);
            CommandUiChecks.press(c, c.options.hotbarKeys[1]);
            CommandUiChecks.report("cu-keys-number-picks", picked && noRelic, "panel open, key 2: selection " + CommandSelection.selected() + ", lastSent before '" + lastBefore + "' after '" + GravesAndGloryClient.lastSent + "'");
        }
        if (tick == 72) {
            boolean unchanged = CommandSelection.selected().equals(List.of("infantry"));
            CommandUiChecks.report("cu-keys-number-closed", unchanged, "panel closed, key 2: selection " + CommandSelection.selected() + " (unchanged " + unchanged + ")");
        }

        // Attack with the panel closed acts on the last selection only
        if (tick == 76) {
            CommandSelection.select("ranged", false);
            CommandUiChecks.server(c, b -> { infBefore = OrderIntakeProbe.snap(b, id, "infantry"); rngBefore = OrderIntakeProbe.snap(b, id, "ranged"); });
        }
        if (tick == 80) CommandUiChecks.press(c, find(c, "key.gravesandglory.attack"));
        if (tick == 98) CommandUiChecks.server(c, b -> {
            Snap inf = OrderIntakeProbe.snap(b, id, "infantry"), rng = OrderIntakeProbe.snap(b, id, "ranged");
            boolean ok = rng != null && rng.order().equals("attack") && java.util.Objects.equals(inf, infBefore);
            CommandUiChecks.report("cu-keys-attack-closed", ok, "selection ranged, panel closed, Z: ranged was " + rngBefore + " now " + rng + "; infantry was " + infBefore + " now " + inf);
        });

        // Shift keeps the panel open after an order; without it the panel closes
        if (tick == 102) { CommandSelection.select("infantry", false); CommandSelection.setPanelOpen(true); c.options.sneakKey.setPressed(true); CommandUiChecks.press(c, find(c, "key.gravesandglory.attack")); }
        if (tick == 108) { shiftStayedOpen = CommandSelection.panelOpen(); c.options.sneakKey.setPressed(false); }
        if (tick == 110) { CommandSelection.setPanelOpen(true); CommandUiChecks.press(c, find(c, "key.gravesandglory.attack")); }
        if (tick == 116) {
            boolean closed = !CommandSelection.panelOpen();
            CommandUiChecks.report("cu-keys-shift-keeps-open", shiftStayedOpen && closed, "Z with Shift held left the panel open=" + shiftStayedOpen + "; Z without Shift closed it=" + closed);
        }

        // Follow me and Retreat
        if (tick == 120) { CommandSelection.setPanelOpen(false); CommandSelection.select("infantry", false); CommandUiChecks.press(c, find(c, "key.gravesandglory.follow")); }
        if (tick == 130) CommandUiChecks.server(c, b -> followed = OrderIntakeProbe.snap(b, id, "infantry"));
        if (tick == 132) CommandUiChecks.press(c, find(c, "key.gravesandglory.retreat"));
        if (tick == 142) CommandUiChecks.server(c, b -> {
            Snap r = OrderIntakeProbe.snap(b, id, "infantry");
            boolean ok = followed != null && followed.order().equals("follow") && r != null && r.order().equals("retreat") && r.thenFollow();
            CommandUiChecks.report("cu-keys-follow-retreat", ok, "C stored " + followed + "; V stored " + r);
        });

        // First launch only: the vanilla toolbar keys are freed once, then a flag file keeps later launches (and a player's rebinding) alone
        if (tick == 43) toolbarFlag(c);

        // Several selected groups: one tactical_v2 packet, both groups take the order (cu-intake-multi-* count the packets)
        if (tick == 146) { CommandSelection.select("infantry", false); CommandSelection.select("ranged", true); CommandUiChecks.press(c, find(c, "key.gravesandglory.follow")); }
        if (tick == 176) CommandUiChecks.server(c, b -> {
            Snap inf = OrderIntakeProbe.snap(b, id, "infantry"), rng = OrderIntakeProbe.snap(b, id, "ranged");
            CommandUiChecks.report("cu-keys-multi-select", inf != null && inf.order().equals("follow") && rng != null && rng.order().equals("follow"),
                "infantry+ranged selected, C: infantry " + (inf == null ? null : inf.order()) + " at " + (inf == null ? 0 : inf.issued()) + ", ranged " + (rng == null ? null : rng.order()) + " at " + (rng == null ? 0 : rng.issued()));
        });

        // Charge: X starts the aim (no enemy needed), Enter sends it; the server stores a direction. Then X within the cooldown ends any aim.
        if (tick == 180) { CommandSelection.select("infantry", false); CommandSelection.setPanelOpen(false); CommandUiChecks.server(c, b -> OrderIntakeProbe.resetCharge(b, id)); }
        if (tick == 184) CommandUiChecks.press(c, find(c, "key.gravesandglory.charge"));
        if (tick == 188) { aimedCharge = CommandSelection.aiming(); CommandUiChecks.press(c, find(c, "key.gravesandglory.confirm")); }
        if (tick == 200) CommandUiChecks.server(c, b -> {
            Snap a = OrderIntakeProbe.snap(b, id, "infantry");
            CommandUiChecks.report("cu-keys-charge-aim", "charge".equals(aimedCharge) && a != null && a.order().equals("charge") && a.chargeDir() != null,
                "X aimed '" + aimedCharge + "', Enter stored " + a + " (no enemy aimed)");
        });
        if (tick == 206) { chargeCooling = CommandNet.Client.chargeTicksLeft() > 0; CommandSelection.order("hold", false); aimedHold = CommandSelection.aiming(); CommandUiChecks.press(c, find(c, "key.gravesandglory.charge")); }
        if (tick == 212) {
            CommandUiChecks.report("cu-keys-charge-refused-cancels-aim", chargeCooling && "hold".equals(aimedHold) && CommandSelection.aiming().isEmpty(),
                "client charge cooldown running=" + chargeCooling + "; hold aim before X '" + aimedHold + "', after X '" + CommandSelection.aiming() + "'");
        }
        if (tick == 216) CommandUiChecks.finish(c);
    }

    /** freeToolbarKeys: a fresh flag path unbinds a toolbar key on C; with the flag now written a second call leaves a re-bound C alone. */
    void toolbarFlag(MinecraftClient c) {
        var save = c.options.saveToolbarActivatorKey;
        var key = net.minecraft.client.util.InputUtil.fromTranslationKey("key.keyboard.c");
        java.nio.file.Path flag = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "gg-toolbar-flag-" + System.nanoTime() + ".properties");
        boolean real = java.nio.file.Files.exists(GravesAndGloryClient.toolbarFlag());
        try {
            c.options.setKeyCode(save, key); net.minecraft.client.option.KeyBinding.updateKeysByCode();
            boolean first = GravesAndGloryClient.freeToolbarKeys(c, flag);
            boolean freed = save.isUnbound(), written = java.nio.file.Files.exists(flag);
            c.options.setKeyCode(save, key); net.minecraft.client.option.KeyBinding.updateKeysByCode();
            boolean second = GravesAndGloryClient.freeToolbarKeys(c, flag);
            boolean kept = save.getBoundKeyTranslationKey().equals("key.keyboard.c");
            CommandUiChecks.report("cu-keys-first-launch-only", first && freed && written && !second && kept && real,
                "no flag: unbound C=" + (first && freed) + ", flag written=" + written + "; flag present: left a re-bound C alone=" + (!second && kept) + "; this launch's own flag file existed=" + real);
        } finally {
            try { java.nio.file.Files.deleteIfExists(flag); } catch (Exception ignored) { }
            c.options.setKeyCode(save, net.minecraft.client.util.InputUtil.UNKNOWN_KEY); net.minecraft.client.option.KeyBinding.updateKeysByCode();
        }
    }

    /** Every command key is bound to its default: Attack Z, Charge X, Follow me C, Retreat V, Formation B, panel R, Enter, Backspace; Attack is no longer G. */
    void defaults(MinecraftClient c) {
        String[][] want = {
            {"key.gravesandglory.attack", "key.keyboard.z"}, {"key.gravesandglory.charge", "key.keyboard.x"}, {"key.gravesandglory.follow", "key.keyboard.c"},
            {"key.gravesandglory.retreat", "key.keyboard.v"}, {"key.gravesandglory.formation", "key.keyboard.b"}, {"key.gravesandglory.menu", "key.keyboard.r"},
            {"key.gravesandglory.confirm", "key.keyboard.enter"}, {"key.gravesandglory.cancel", "key.keyboard.backspace"}};
        StringBuilder detail = new StringBuilder();
        boolean ok = true;
        for (String[] w : want) {
            KeyBinding k = find(c, w[0]);
            String bound = k == null ? "(missing)" : k.getBoundKeyTranslationKey();
            if (!w[1].equals(bound)) ok = false;
            detail.append(w[0].substring("key.gravesandglory.".length())).append('=').append(bound.replace("key.keyboard.", "")).append(' ');
        }
        KeyBinding attack = find(c, "key.gravesandglory.attack");
        boolean notG = attack != null && !attack.getBoundKeyTranslationKey().equals("key.keyboard.g");
        // no other binding may share one of our keys: only one binding per key receives presses (vanilla's save/load hotbar sat on C and X)
        StringBuilder clash = new StringBuilder();
        for (String[] w : want) for (KeyBinding k : c.options.allKeys) if (k.getBoundKeyTranslationKey().equals(w[1]) && !k.getTranslationKey().equals(w[0])) clash.append(k.getTranslationKey()).append(" on ").append(w[1]).append(' ');
        CommandUiChecks.report("cu-keys-defaults", ok && notG && clash.length() == 0, detail.toString().trim() + "; attack is not G=" + notG + "; shared keys: " + (clash.length() == 0 ? "none" : clash.toString().trim()));
    }

    @Override public void cleanup(MinecraftClient c) {
        c.options.sneakKey.setPressed(false);
        CommandSelection.reset();
    }
}
