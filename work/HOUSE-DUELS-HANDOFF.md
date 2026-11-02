> Superseded 2026-11-02: these steps are done; see work/HOUSE-BALANCE-RETUNE-2026-11-02.md (v2 on Set A).

# House duels handoff (2026-11-02, worktree bannerbound-house-audit, branch work/house-audit)

Task: audit proposals A (four-condition cross-House duels) and F (zombie reinforcements on hard). No gameplay change.

## Method
- Plan work/method/plans/cross12.txt: tools/balance_method.py plan-units output, same-House pairings dropped, 121 pairings x 12 reps = 1452 fights.
- Run via tools/balance_sim.sh (script work/method/runall.sh), -PsimFast=true -PduelSeed=7, SIM_LEVEL_SEED=4242, melee grace 1.0 (sheet value), traits=banner (tier III).
- No code switch needed: -PsimOverride sets houseTraits values. Rise Again off = graveborn_1..3.value=0 (a roll below 0 never succeeds, same effect as Houses.forceRise=false). Bulwark neutralised = gilded_1..3 value=0 AND radius=0 (radius=0 also removes covered flanks, so this condition removes the flank cover as well as the ranged cut).
- Analysis: python work/method/analyze.py <duels.csv> (House win rate over cross-House troop fights, draw = half).
- Run folders: work/method/c1base-*, c2norise-*, c3nobulwark-*, c4both-* (gitignored, throwaway).

## Results so far (House win %, band 40-60)
| condition | gilded | graveborn | ironclad | tidecallers |
|---|---|---|---|---|
| baseline | 35.9 | 71.5 | 35.6 | 55.8 |
| Rise Again off | 36.9 | 67.1 | 38.7 | 56.4 |
| Bulwark off (value+radius 0) | 29.8 | 75.2 | 36.4 | 56.7 |
| both off | c4both still running (1412 of 1452 rows when this was written) | | | |
Each House n = 672 to 780 troop-fights, SE about 1.7 to 1.9.
Baseline differs from the 10-29 reference (graveborn 81.2, gilded 26.8): main has moved on since, not investigated.

## Still to do
1. Wait for c4both (work/method/runall.out prints ALLDONE), run analyze.py on it.
2. F: write test-tools ZombieReinforcementProbe (draft in work/stash/ZombieReinforcementProbe.java, not yet in test-tools or fabric.mod.json "main" entrypoints). Run with JAVA_TOOL_OPTIONS=-Dgravesandglory.zombieProbe=1 and a tiny -PduelSim plan; it sets hard + doMobSpawning, reads ZOMBIE_SPAWN_REINFORCEMENTS on every zombie-family troop, hurts each 400 times, with a forced-1.0 control. Static note: nothing in src calls initialize on troop mobs, so the attribute should stay at its base 0.
3. Append the table and F result as an appendix to docs/reports/house-balance-vanilla-audit-2026-10-31.md.
4. python tools/verify.py fast --offline; revert Spec.java and sheets/preflight.json if they differ only by line endings (preflight rewrites them).
5. Commit with trailer Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>. No push.
