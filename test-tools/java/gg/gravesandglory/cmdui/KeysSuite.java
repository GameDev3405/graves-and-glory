package gg.gravesandglory.cmdui;

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
            CommandUiChecks.report("cu-keys-number-picks-part-open", picked && noRelic, "panel open, key 2: selection " + CommandSelection.selected() + ", lastSent before '" + lastBefore + "' after '" + GravesAndGloryClient.lastSent + "'");
        }
        if (tick == 72) {
            boolean unchanged = CommandSelection.selected().equals(List.of("infantry"));
            CommandUiChecks.report("cu-keys-number-picks", unchanged, "panel closed, key 2: selection " + CommandSelection.selected() + " (unchanged " + unchanged + "); the open-panel half was reported above");
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
        if (tick == 146) CommandUiChecks.finish(c);
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
