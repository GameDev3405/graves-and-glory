package gg.gravesandglory;

import java.util.Set;

/**
 * The server-side choices that follow which model a troop is drawn on (GeoTroops; docs/anim-rebuild/OLD-MODEL-GAPS.md gaps 2 and 4): a caster's
 * bolt starts at its GeckoLib staff tip only when the troop draws through GeckoLib, else at its eyes (the old origin), and a melee blow shows the
 * vanilla sweep only on the old model. Run on the default settings (no -Dgravesandglory.geoTroops), as verify runs it. Never shipped.
 */
public final class GeoTroopsCheck {
    static int checks, failed;

    static void check(boolean ok, String message) {
        checks++;
        if (!ok) { failed++; System.out.println("FAIL " + message); }
    }

    public static void main(String[] args) {
        // the sets: the knight approved, every PREVIEW troop drawn only with geoTroops=all, nothing in both
        check(GeoTroops.APPROVED.contains("knight"), "the knight is approved");
        for (String t : GeoTroops.PREVIEW) check(!GeoTroops.APPROVED.contains(t), t + " is in both APPROVED and PREVIEW");
        check(GeoTroops.ENABLED && !GeoTroops.ALL, "default settings: GeckoLib on, previews off (run without -Dgravesandglory.geoTroops)");
        for (String t : GeoTroops.APPROVED) {
            check(GeoTroops.drawn(t), t + " (approved) draws through GeckoLib by default");
            check(GeoTroops.drawn(t, true, true), t + " draws through GeckoLib with geoTroops=all");
            check(!GeoTroops.drawn(t, false, false), t + " draws on the old model with geoTroops=false");
        }
        for (String t : GeoTroops.PREVIEW) {
            check(!GeoTroops.drawn(t), t + " (preview) draws on the old model by default");
            check(GeoTroops.drawn(t, true, true), t + " draws through GeckoLib with geoTroops=all");
        }
        check(!GeoTroops.drawn("revenant_not_a_troop") && !GeoTroops.drawn(null), "an unknown troop or none draws on the old model");

        // gap 2: the bolt's origin. Every caster with a cast_tip resource is a preview troop today: by default its tip is not used (the eyes), with
        // geoTroops=all it is; an approved caster would use it by default.
        for (String t : Set.of("mage", "hexer", "tc_mist", "wm_seer")) {
            check(!GeoTroops.castTip(t, true, false), t + ": by default the bolt starts at the eyes (old model), not the GeckoLib staff tip");
            check(GeoTroops.castTip(t, true, true), t + ": with geoTroops=all the bolt starts at the GeckoLib staff tip");
            check(!GeoTroops.castTip(t, false, true), t + ": with geoTroops=false the bolt starts at the eyes");
        }
        check(GeoTroops.castTip("knight", true, false), "an approved troop takes its tip (where it has one) by default");

        // gap 4: the sweep. Old model: shown; GeckoLib: not (Kort, 2026-11-08: the swing carries the blow).
        check(!GeoTroops.sweeps("knight", true, false), "the knight (GeckoLib by default) shows no sweep");
        check(GeoTroops.sweeps("knight", false, false), "the knight on the old model (geoTroops=false) shows the sweep");
        for (String t : Set.of("revenant", "sentinel", "reaper", "gb_hollowking", "tc_raider")) {
            check(GeoTroops.sweeps(t, true, false), t + " (old model by default) shows the sweep");
        }
        check(!GeoTroops.sweeps("sentinel", true, true), "sentinel with geoTroops=all (GeckoLib) shows no sweep");
        check(GeoTroops.sweeps("revenant") == !GeoTroops.drawn("revenant") && GeoTroops.sweeps("knight") == !GeoTroops.drawn("knight"), "the default overload follows this JVM's settings");

        if (failed > 0) {
            System.out.println("GeoTroopsCheck: " + failed + " checks FAILED");
            System.exit(1);
        }
        System.out.println("GeoTroopsCheck: " + checks + " checks passed");
    }
}
