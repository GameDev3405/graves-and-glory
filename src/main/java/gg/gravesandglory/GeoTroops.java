package gg.gravesandglory;

import java.util.Set;

/**
 * Which troops draw through GeckoLib (GeoTroopRenderer) and which on the old model and clips (TroopModel, TroopMotion). A plain class with no client
 * imports, so the server may ask too: a few cosmetic things it sends follow the model, a caster's bolt starting at its GeckoLib staff tip
 * (Projectiles.castFrom) and the vanilla sweep on a melee blow (BattleEffects.strike) (docs/anim-rebuild/OLD-MODEL-GAPS.md). The server reads its
 * own JVM's -Dgravesandglory.geoTroops: in single player and the harness that is the client's; a dedicated server on default settings judges by
 * APPROVED alone, which is what a client on default settings draws.
 * <p>
 * -Dgravesandglory.geoTroops=false draws every troop on the old model; =all also draws the PREVIEW troops through GeckoLib.
 */
final class GeoTroops {
    private GeoTroops() {}
    static final boolean ENABLED = !"false".equals(System.getProperty("gravesandglory.geoTroops"));
    static final boolean ALL = "all".equals(System.getProperty("gravesandglory.geoTroops"));
    /** The troops drawn through GeckoLib: those Kort approved through the review booth (docs/anim-rebuild/REVIEW-BOOTH.md). Every other troop
     *  keeps the old model and clips until its batch is approved; then it moves from PREVIEW to here. */
    static final Set<String> APPROVED = Set.of("knight", "sentinel", "gl_marshal", "ic_hearthguard", "revenant");
    /** Rebuilt troops not yet approved: drawn through GeckoLib only with -Dgravesandglory.geoTroops=all (lane testing). */
    static final Set<String> PREVIEW = Set.of("tc_raider", "archer", "longbow", "wm_hunter", "bonebow", "stalker", "ic_arbalist", "wm_tusk", "tc_tideguard", "tc_admiral", "ic_breaker", "tc_harpooner", "mage", "hexer", "tc_mist", "wm_seer", "wm_hound", "gb_hollowking", "wm_stalker");

    /** Whether the troop draws through GeckoLib on this JVM's settings. */
    static boolean drawn(String troop) { return drawn(troop, ENABLED, ALL); }
    /** Whether the troop draws through GeckoLib with GeckoLib on or off (enabled) and the previews in or out (all). */
    static boolean drawn(String troop, boolean enabled, boolean all) { return enabled && troop != null && (APPROVED.contains(troop) || all && PREVIEW.contains(troop)); }

    /** Whether a caster's drawn shot starts at its cast_tip (the GeckoLib prop's tip on its release frame): only for a troop drawn through GeckoLib.
     *  The old model holds no such prop there, so its shot keeps the eyes (Projectiles.castFrom). */
    static boolean castTip(String troop) { return drawn(troop); }
    static boolean castTip(String troop, boolean enabled, boolean all) { return drawn(troop, enabled, all); }

    /** Whether a melee blow shows the vanilla sweep (BattleEffects.strike): only on the old model. A GeckoLib swing carries the blow itself
     *  (Kort, 2026-11-08: the white arc behind the sword fights the swing animation); the old clips do not. */
    static boolean sweeps(String troop) { return !drawn(troop); }
    static boolean sweeps(String troop, boolean enabled, boolean all) { return !drawn(troop, enabled, all); }
}
