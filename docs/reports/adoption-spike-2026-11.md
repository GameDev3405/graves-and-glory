# Map adoption spike and Ironhold round trip (plan 2A.2, 2A.4, 2A.5), 2026-11-01

Format: `gg-mapforge-runs/1` (tools/mapforge/runs.py, src/main/java/gg/gravesandglory/MapRuns.java), as decided
2026-10-28: the generated map is the base and a small edit overlay is written over it after generation. All
runs used an isolated run root and the Gradle harness (`-PmapForge=SCRIPT`, test-tools/MapForgeHarness.java);
no player save, no dedicated server.

## 2A.4 spike: 64x64 camp region, y 40..120 (331,776 cells)

| | full snapshot | base + overlay |
|---|---|---|
| file size | capture 24,405 B | base 22,637 B (kept by tools only), overlay 4,177 B (2,724 cells, 64 block entities) |
| make | export | export + diff 141 ms |
| apply | 65-89 ms, visits all 331,776 cells | 67 ms: base-digest check plus 2,724 cells |
| verify after apply | 0 differ | 0 differ |
| determinism | two re-exports byte-identical (sha 5aa4acb3) | same; re-adopting on the final world is refused (base digest) |

Whole Ironhold map, y 40..130, 700x500 columns (31.85M cells): capture 1.16 MB in 6.6-6.8 s; a full snapshot
would ship and rewrite every cell. The builder edit below is a 5.6 KB overlay, applied in 138 ms. The
measurement does not call for the snapshot fallback; base + overlay stays.

## 2A.5 Ironhold round trip (runs A3 and B)

1. Agent batch (the offline surface, `adopt batch --area=-240,-250,240,249` with the game's protects): plan
   18.38M cells / 2,456,061 changes 7.1 s; apply 9.1 s, backup zip 2.8 MB in 262 ms, undo file 846 KB; a second
   apply without a new plan is refused. Walk 9/12: the offline surface does not join the generated terrain at
   the area edge (gold spawn cannot reach a point). Undo restored 2,456,057 cells; verify of the base 0 differ;
   walk 12/12; a second undo is refused.
2. Builder hand edits (ground retextured, barrels set in), export final, diff: overlay 2,056 cells, 5,606 B,
   10.9 s. Verify final 0 differ; walk 12/12.
3. `adopt validate` and `adopt bake --target dev`: test-tools/resources/data/gravesandglory/adoption-dev/ironhold.runs.gz,
   5,643 B, stamp bf0e9ef4e2f6, modVersion 0.5.0. A release bake is refused without `--approved-by`.
4. Run B (`-PadoptionDev`): map 1 built and adopted after generation, "adopted 2056 cells in 138 ms"; verify
   against the builder's final export 0 differ; walk 12/12. Refused as expected: another generator stamp, a
   changed body (body hash), and the overlay on an already-adopted world (base digest).

## What the game changes on its own

Random ticks turn grass and dirt into each other, and flowing water spreads into cleared cells, within
seconds. World-comparing digests (plan, apply, undo, base digest) therefore settle soil to dirt and flowing
(non-source) water or lava to air (`settle` in both runs.py and MapRuns). File digests stay exact. Without
this an undo was refused 10 cells into a 2.4M-cell apply.

## Open for Kort

- Approve `gg-mapforge-runs/1` as the shipped map format and a first release bake (`--approved-by`).
- Approve the settle rule (soil and flowing fluid ignored by world comparisons).
- The surface json's spawns (manifest ±314) differ from Spec's (±283, ±108); its full-map batch traps the
  gold spawn. The builder stand-in was the integrated harness world, not the BuildMode server (no eula.txt).
