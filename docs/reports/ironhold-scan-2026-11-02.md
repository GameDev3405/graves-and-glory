# Ironhold scan handoff (plan 4A.3 and 3.1 prep), 2026-11-02

Branch `work/ironhold-scan` in `C:/Users/kortt/Melty/bannerbound-ironhold`, cut from main ef1c1a6. Nothing pushed. No game, sheet or Spec change.

## Findings so far
- There is no scan command in `tools/mapforge/` (it has validate, dry-run, apply, status, sign, render, gaps, adopt). The only scan idea on record is SLICE-1-CHECKPOINT.md line 49: "a scan tool reads the adopted world and proposes new values".
- The D1a spawn move is already in code: `CampaignMaps.spawnX(1, team)` gives Dawn -314 and Dusk 128 (z 0); Dusk barracks at 131. `docs/MAP-STANDARDS.md` section 9 still says every map spawns at +-283 and calls spawns identical, so that record is stale for Ironhold. Not yet fixed.
- Capture points are unchanged in code: A (-29,-130), B (0,0), C (29,130), radius 17, elevation 64. Manifest protect locks match.
- `sheets/design.json` `landmarks` rows for Ironhold still hold the pre-layout places. Differences from the signed layout: Dusk watchtowers `ih-dusk-tower-n/s` at (180,+-85) lie in the gorge (playable ground ends near x 165, parapet x 166..168); the layout wants watchtowers at (150,+-150) for the east. The four x 72 east curtain rows stay until adoption (Kort 2026-10-31).
- `LayoutMarker` is only layout string, map, margin revision and `Structures.hash(map)` of the landmark rows. It has no rows of its own and the hash cannot be computed outside Java, so it changes only when the landmark rows change (adoption).

## World rebuild (needed because work/ is ignored and no built world exists)
- Base: `python docs/maps/ironhold/sections/make_blockout.py base`, then apply every section plan with a manifest copy.
- Plan order matters (erode and noise read neighbours). Applying sections in manifest order made A0, C0, E0, F0 and G0 fail their surface `expectSection` check. A second script applies plans in review-time order. Script: `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound/3774e262-685f-4a49-9dac-390d66d09f46/scratchpad/rebuild.py`, log `.../rebuild.log` in the same folder. It prints the count of sections whose digest differs from the manifest's last passing review.
- Running now: python PID 23384 (the rebuild). It writes `work/mapforge/ironhold/ironhold-rebuilt.json`. No Minecraft client was started by me.

## Left to do
1. Read the rebuild log. If drifted sections remain, the scan is on an approximate world; say so.
2. Write `tools/mapforge/scan.py` (+ `scan` subcommand, one test): per spawn the standing height, headroom and a walk graph (step 1, headroom 2, water depth at most 2) with path lengths to A, B, C, the supply yards and the Siege rings; per capture ring the ground heights and blocks at y 64..67; landmark rows against the scanned ground and the playable box.
3. Commit the generated report as `docs/maps/ironhold/scan-2026-11-02.md`.
4. Fix only records (MAP-STANDARDS section 9 Ironhold spawns). Do not move spawns, captures, routes or landmark rows.
5. Questions for Kort to list: Dusk towers in the gorge at (180,+-85); Dusk spawn-to-B about 130 versus Dawn about 315 (layout D1a, by design, but MAP-STANDARDS says equal lane-head distance); camp fan roads still run to (+-290,0) though the Dusk spawn is at 128.
6. `verify fast --offline` only if no client runs (2 Minecraft javas seen at last count; this checkout is clean except the new files).
