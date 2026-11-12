package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        // gap 2: the bolt's origin, as a contract over the sets (not today's membership): a troop with a cast_tip resource takes its tip exactly when it
        // draws through GeckoLib (approved by default; approved or preview with geoTroops=all; never with geoTroops=false), else its eyes.
        List<String> casters = new ArrayList<>();
        try (var files = Files.list(Path.of("src/main/resources/data/gravesandglory/cast_tip"))) {
            files.map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".json")).map(n -> n.substring(0, n.length() - 5)).sorted().forEach(casters::add);
        } catch (Exception e) { check(false, "cast_tip resources: " + e); }
        check(!casters.isEmpty(), "at least one troop has a cast_tip resource");
        for (String t : casters) {
            boolean approved = GeoTroops.APPROVED.contains(t), geo = approved || GeoTroops.PREVIEW.contains(t);
            check(GeoTroops.castTip(t, true, false) == approved, t + ": by default the bolt starts at the " + (approved ? "GeckoLib staff tip (approved)" : "eyes (old model)"));
            check(GeoTroops.castTip(t, true, true) == geo, t + ": with geoTroops=all the bolt starts at the " + (geo ? "staff tip" : "eyes"));
            check(!GeoTroops.castTip(t, false, true), t + ": with geoTroops=false the bolt starts at the eyes");
        }
        for (String t : GeoTroops.APPROVED) check(GeoTroops.castTip(t, true, false), t + ": an approved troop takes its tip (where it has one) by default");

        // gap 4: the sweep, over every troop in the sheet: shown exactly when the troop is on the old model (Kort, 2026-11-08: a GeckoLib swing
        // carries the blow; the old clips do not).
        for (Spec.TroopsRow r : Spec.TROOPS) {
            String t = r.id();
            boolean approved = GeoTroops.APPROVED.contains(t), geo = approved || GeoTroops.PREVIEW.contains(t);
            check(GeoTroops.sweeps(t, true, false) == !approved, t + " by default: sweep " + !approved);
            check(GeoTroops.sweeps(t, true, true) == !geo, t + " with geoTroops=all: sweep " + !geo);
            check(GeoTroops.sweeps(t, false, false), t + " with geoTroops=false (old model) shows the sweep");
            check(GeoTroops.sweeps(t) == !GeoTroops.drawn(t), t + ": the default overload follows this JVM's settings");
        }
        for (String t : GeoTroops.APPROVED) check(Arrays.stream(Spec.TROOPS).anyMatch(r -> r.id().equals(t)), t + " (approved) is a troop in the sheet");
        for (String t : GeoTroops.PREVIEW) check(Arrays.stream(Spec.TROOPS).anyMatch(r -> r.id().equals(t)), t + " (preview) is a troop in the sheet");

        if (failed > 0) {
            System.out.println("GeoTroopsCheck: " + failed + " checks FAILED");
            System.exit(1);
        }
        System.out.println("GeoTroopsCheck: " + checks + " checks passed");
    }
}
