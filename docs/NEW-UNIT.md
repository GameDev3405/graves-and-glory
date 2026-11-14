# New unit: from nothing to shippable

One checklist for adding a brand-new troop. Policy wins over this page: `docs/WORKFLOW.md`, then the skills and recipes cited below. `<tid>` is the new troop id (`[a-z0-9_]`, House prefix such as `wm_`, `ic_`, `tc_`, `gb_`, `gl_`), `<old>` the troop whose slot it takes, `<peer>` the shipped troop of the same family you copy from, `<house>` its House, `<lead>` the lead who briefed you. Run every command from the lane worktree in Git Bash. Checked against `main` at bc9afa54 (2026-11-09) and a dry run (placeholder `test_dummy` in the `tc_raider` slot).

**Hard rules.** No push, no publish (the booth upload is the one allowed upload). Never hand-edit `src/main/java/gg/gravesandglory/Spec.java` or `sheets/preflight.json`: `tools/preflight.py` writes them. When your sheet change regenerates them, commit the pair on its own, after the sheet commit: save `sha256sum` of both before and after `python tools/preflight.py` (WORKFLOW.md) and commit with the message `Regenerate Spec.java and preflight.json from <tid>'s sheet rows (tools/preflight.py; no hand edits)` plus the two hash lines (as in commit 10119af7). Never start a Minecraft client or server yourself: in-game suites and sims go through `tools/checkq.py` (docs/SETUP.md "Shared in-game check queue"). Never a Fable model. Reach `main` only with `tools/land.py`. Commands marked **(bg)** run long: start them with `run_in_background` and wait for the notification, never a polling loop.

**Asking Kort.** A worker returns each ask to its lead in its report; the lead adds it as one line to `C:/Users/kortt/Melty/status/<lead>.md` (the folder beside the primary checkout) and deletes it once Kort answers:
`- kort: <one plain sentence> | open: <https link, path or none> | min: <minutes> | prio: <1 now, 2 this week, 3 whenever>`
To test a line without touching that folder: put it in `work/<you>/status/test.md` and run `python tools/inbox.py --status-dir work/<you>/status --json`.

## Command sequence (one screen)

```
# 0  lane (the lead):  git worktree add ../bb-unit-<tid> -b unit/<tid> main ; cd ../bb-unit-<tid>
# 1  spec      (Kort's yes first) edit sheets/design.json ; retire <old> (1b) ; fix every `git grep -l <old>` hit
#              python tools/build_skins.py ; python tools/preflight.py ; python tools/verify.py fast  (bg, ~2 min)
# 2  lore      git show docs/lore-lead:docs/lore/season-framework.md   (section 5) -> pitch, via your lead
# 3  model     cp art/hand/_template/build_template.py art/hand/<tid>/build_<tid>.py ; edit (3)
#              python art/hand/<tid>/build_<tid>.py preview <scratch>/pv crimson
# 4  texture   python art/hand/<tid>/build_<tid>.py gate ; house_palette.py check ; art_hue_check.py ; review
#              python art/hand/<tid>/build_<tid>.py install ; python tools/build_skins.py --verify
# 5  anim      register (5a) ; python art/anim/common/troop_rig.py <tid>
#              GG_ANIM_TROOP=<tid> python art/anim/knight/build_anims.py   (bg, ~6.5 min; builder per pipeline.md 2)
#              python art/anim/common/export.py <tid> ; python tools/anim/scorecard.py --troop <tid>
#              python tools/checkq.py submit client --root <tid>-r1 --env GRAVES_PHASE1_CHECKS=geo-knight \
#                  --env GRAVES_GEO_CHECK_TROOP=<tid> --env GRAVES_GEO_TROOPS=all ; python tools/checkq.py wait <id> (bg)
#              python tools/anim/booth.py build --config art/anim/<family>/booth.<tid>.json --target main
# 6  sound     nothing by default ; python -I test-tools/standalone/test_audio_events.py
# 7  land      python tools/verify.py fast (bg) ; python tools/land.py unit/<tid> --dry-run ; python tools/land.py unit/<tid>
```
Measured wall times: verify fast 1.5 to 2 min; knight builder 6.5 min; troop_rig, export, scorecard about 10 s each; a render 5 s; booth build about 20 s a clip; a harness job up to 30 min (the queue's cap).

## Routing (WORKFLOW.md "Kort's model rules"; troop-animation skill "Routing")

| Stage | Author | Review |
|---|---|---|
| 1 Spec, 2 Lore, 6 Sound, 7 Land | Sonnet 5.5 high | one fresh Sonnet 5.5 high review per change |
| 3 Model, 4 Texture | Opus 5.5 medium (art authoring is important work) | fresh Sonnet 5.5 high, grade per TEXTURE-RECIPE A8.8 |
| 5 Animation | Opus 5.5 medium for builder/clip authoring; Sonnet 5.5 high for build, export, scorecard, renders, harness | fresh Sonnet 5.5 high (`.claude/skills/troop-animation/references/reviewer-prompt.md`) |

Stuck after a failed fix round: Opus 5.5 high. Two fix rounds at most, then escalate with evidence. Never Fable.

## 1. Spec sheet row

**Inputs.** Kort's approved design: House, rarity, role, stats, weapon family, ability, level-2/3 abilities, names, and which slot it takes. The lore never sets stats (lore rule 8).

**1a. The slot (Kort decides first).** `tools/preflight.py` (`HOUSE_SHAPE`) requires every House to have exactly 2 Common, 2 Rare, 1 Epic, 1 Legendary, and `test-tools/standalone/HousesCheck.java` hard-codes the 30-troop `ROSTER` and `launchRoster()`. So a new unit replaces a troop of the same rarity, starts a new House (houses row, glyph `textures/gui/house/<house>.png`, `houseTraits`, palette per TEXTURE-RECIPE part B), or needs a rule change. Replacing changes gameplay and saves: `Profile.sanitize` drops owned ids the catalog no longer knows, so every player who owns `<old>` loses it, and banners holding it are repacked. There is no id-rename save migration (not built). Nothing below starts before Kort answers:
`- kort: May <tid> replace <old> (players who own <old> lose it), or should it start a new House? | open: <design path> | min: 5 | prio: 2`

**1b. Replacing `<old>` in its slot** (delete-and-add at the same sheet position; a true rename keeping the id is a re-skin, not a new unit):
1. Rewrite `<old>`'s rows as `<tid>`'s, in place, in `sheets/design.json`: `troops` (copy `<old>`'s row and change values; every row needs exactly the keys of `troops[0]`), `houses.troops`, `troopNotes`, `levelAbilities` (`<tid>_2`, `<tid>_3`), `levelNames` (Commons/Rares), any `abilities` rows named for it, every `botBanners.slots` list (its House's three bot banners and the `mix_*` banners with that House name it many times) and `waveGroups.troop`. First check no other id contains `<old>`: `grep -o '"[a-z0-9_]*<old>[a-z0-9_]*"' sheets/design.json | sort -u`. Then `sed -i 's/<old>/<tid>/g' sheets/design.json` renames all of them (including `<old>_2`, `<old>_l2`); then read `git diff sheets/design.json` line by line.
2. Retire the old legacy art in the order preflight demands: `git rm art/troops/<old>.json art/troops/<old>_*.png`, then `python tools/build_skins.py` (it deletes `models/troop/<old>.json` and the old textures its previous `index.json` listed, and rewrites `index.json`), then `python tools/preflight.py`. Keep `art/hand/<old>/build_<old>.py`: other builders import its ramps (the tidecallers preset imports `build_tc_raider.NAVY`).
3. Find every other hard-coded use: `git grep -l <old> -- . ':!docs' ':!src/main/java/gg/gravesandglory/Spec.java'`. In the dry run (`tc_raider`) that was, besides the sheet and art: `test-tools/standalone/HousesCheck.java`, `LevelMathCheck.java`, `ProfileMigrationCheck.java` (fast), `GearMigrationCheck.java` (no literal id: it fails `owned troops and banner are unchanged` because the Commons changed, so grep cannot find it; verify fast does), `test-tools/java/.../Phase2Checks.java`, `AbilityLevelChecks.java` (in-game), `test-tools/qa/profile-v2-seed.json`, `test-tools/resources/fixtures/profile-v3-veteran.json`, `tools/balance_method.py`, `art/clips/build_attacks.py`, `art/title/src/key_art.py`, `GeoTroopRenderer.java`, and the animation files of stage 5 (`art/anim/**`, `tools/anim/scorecard.py`, `scorecard_limits.json`). Update the tests and fixtures to the new id. Hits in files another lead owns (`tools/balance_method.py`, `art/title/src/key_art.py`, `GeoTroopRenderer.java`; check `status/*.md` for the owner) go to that lead through your lead, not edited by you; hand hits under `art/anim/**` and `tools/anim/**` to the animation lead; ask your lead before changing a migration test's expected result (that is a save decision).
4. Leave `art/anim/<old>/` and `<old>`'s exported GeckoLib assets for the animation lead to retire.

**1c. A unit with no slot to replace** adds the same rows (troops, houses, troopNotes, levelAbilities, levelNames, abilities, `attackVariants` only for a new `family`) once Kort has changed the House rule. Rules preflight enforces: `house`/`rarity` resolve; `slotWeight` = the rarity's; `ranged` true exactly when `range` > 3; `weapon`/`helmet`/`chest` are `minecraft:` ids; `family` one of preflight's `FAMILIES`; `shield` is `none` exactly when `tags` lacks `shield`; `texture` an existing `gravesandglory:` asset or a `VANILLA_TEXTURES` path; Common/Rare `evolveLine` names a `levelNames` troop, Epic/Legendary `none`; level 2 keeps the base ability's `mechanic`; no name shared with another troop or level, none ending ` (Veteran)`; player text never says stage, evolve or Writ. `impactTicks` is gameplay (Kort). `tools/expand_design.py` is a one-off historical migration: never run it.

**Done when** `python tools/preflight.py` prints `Preflight clean: <n> filled cells; every row generates a Java record. Runtime verification pending.` and `python tools/verify.py fast` **(bg)** exits 0 (it runs every `test-tools/standalone/*Check.java` and `test_*.py`, `build_skins.py --verify`, `clips.py --verify`, the build).
**Commit** `sheets/design.json`, the updated tests and fixtures, the `git rm` of `art/troops/<old>*`, and what build_skins changed under `src/main/resources/assets/gravesandglory/models/troop/` and `textures/entity/troop/`. Then the regenerated `Spec.java` + `sheets/preflight.json` in their own commit (Hard rules).
**Common failures.** `art/troops/<old>.json: <old> is not a troops id` (step 2 not done). `models/troop/index.json: troops [...] must be exactly the art/troops ids` (run build_skins.py). `houses/<h>: N common troops` (slot rule). `LevelMathCheck: no troop row <old>` and other `*Check` failures (step 3). `KeyError` in preflight: a missing column.

## 2. Lore row

**Where lore lives (2026-11-09).** Not on `main`: branch `docs/lore-lead` (worktree `bannerbound-lore`), page https://claude.ai/artifact/N5FnQy9GTjfB7s6Rir5LnL.
```
git show docs/lore-lead:docs/lore/season-framework.md          # section 5 "Rules for adding a unit"
git show docs/lore-lead:docs/drafts/lore-bible-2026-11-09.md    # section 5, the House
git show docs/lore-lead:docs/lore/names.md
```
**Not built:** no per-troop lore column in `sheets/design.json` and no troop lore file. The only player text a troop carries is `troops.name`, `levelNames`, `troopNotes` and its ability rows (stage 1).

**One lore row needs:** House and species (rule 1); its magic as that House's glimpur, if any (2); one line on its place in the House's grievance (3); its look within the House aesthetic (4); whether it speaks (5); no giants before Act II (6); a role or title, not a new named character (7). Write it to `work/<tid>/lore-pitch.md` (name, House, the rules ticked, one flavour line) and put the path in your report; your lead passes it to the lore lead.
**Done when** the lore lead approves (your lead relays it). Mechanical check: `python tools/preflight.py` still clean after the names land.
**Kort:** only for a new named character: `- kort: May <tid> be the named character <name>? | open: work/<tid>/lore-pitch.md | min: 3 | prio: 2`.

## 3. Model

**Source of truth.** Geometry and paint are one builder, `art/hand/<tid>/build_<tid>.py`, on `art/hand/common/handlib.py`, from `art/hand/_template/build_template.py`. The GeckoLib model is generated from it in stage 5, the legacy model by `install` in stage 4; never hand-edit either. Look and mark come from your brief (existing roster: `docs/superpowers/specs/2026-10-17-unit-identity.md`, `docs/ART-STYLE-GUIDE.md` 10.x); missing: ask first.

```
mkdir -p art/hand/<tid>
cp art/hand/_template/build_template.py art/hand/<tid>/build_<tid>.py
python art/hand/<tid>/build_<tid>.py preview <scratch>/pv crimson
```
Edits (TEXTURE-RECIPE A2): `TID = "<tid>"`, `HOUSE = "<house>"`; delete the `--house=` loop, the other `PRESETS` entries and the `if TID == "template"` guard; **rename the variable `TEMPLATE` to `TROOP` everywhere** (stage 5 registers the module attribute `TROOP`, and the comment on `house_stub.main` already says `TROOP`); write `NOTE`; set `meta["weapon"]` to an existing id. Weapon ids and their modules (stage 5 needs both): `grep -ho '"weapon": "[a-z_]*"' art/hand/*/build_*.py | sort -u`, then `grep -ln '"<weapon id>"' art/hand/weapons/*.py` (e.g. `cutlass` is in `build_house_weapons.py`). A new weapon is a separate task (`weapon: TODO` in NOTE). Keep the biped skeleton: `limb()` joints, hand bones, `arms_folded`, the `tabard` bone, at most 64 bones; the rig contract (`handlib` `ELBOW`/`KNEE`/`HAND`) is the animation lead's.

**Gap:** non-biped units (beasts, siege crews) have no template; `wm_hound` has its own rig and wm_stalker, wm_oldhorn, tc_brute, ic_ballista, ic_mortar, ic_colossus, reaper have no GeckoLib set. Such a unit needs an animation-lead design first.
**Done when** `preview` prints `preview written to <scratch>/pv` with `<tid>_t1..3.png`, `<tid>_t1..3.geo.json`, `skin_s1..3.png`, `cloth_<side>.png`, and no pack overflow.
**Common failures.** `NameError: TEMPLATE`/`TROOP` (rename incomplete); gear moved off its limb with offsets instead of `dil`; pauldron `ROLL` +14 (wings; use -14); chest gear hidden behind the tabard (A2.5).

**No coplanar faces (z-fighting).** Two visible faces of different cubes on one plane (within 0.005 px), facing the same way and overlapping by more than 0.25 px2, flicker in game. `python tools/zfight_check.py [--troop <tid>]` checks every shipped troop model, old (`models/troop`) and GeckoLib (`geo/troop`), at rest and with forearms and shins bent 40 and 75 degrees; it is step `zfight` of `tools/verify.py fast`. You normally do nothing: the generators fix it. `Troop.outputs()` (old models) and `troop_rig` (GeckoLib; not yet the knight's own `build_rig`, whose pairs are listed in the exemption file) call `zfight.settle` (`art/hand/common/zfight.py`), which nudges a cube's `dil` by 0.03 px (0.015 or 0.045 when three or more faces share a plane) so every same-way pair ends at least 0.015 px apart, never moving a box more than 0.05 px from its authored shape; jointed shins and forearms get `handlib.LIMB_INSET` (0.04 px) and shield rims come from `handlib.shield_boxes` (`RIM_LIFT` 0.03 px). Do not offset cubes by hand with `origin`, and do not lean on fractional sizes: the pack rounds sizes, the UVs stay put because only `dil` moves. If a pair cannot be separated (the check still fails after a rebuild), add a row `{kind, troop, a, b, reason}` to `tools/zfight_exempt.json` only when the faces are really hidden or intended, with the reason; a row that stops matching fails the check too.

## 4. Texture

Follow `docs/art/TEXTURE-RECIPE.md` part A exactly (part B for a new House); read `docs/art/unit-texture-audit.md` sections 2 and 4 first. Approved troops are never repainted. The renderer is on `main` at `tools/anim/render_geo.py` (no need for the recipe's `git show anim/geckolib-rebuild:` copy). `<b>` = `art/hand/<tid>/build_<tid>.py`.
```
python tools/art/house_palette.py show <house> <scratch>/<house>.png
python <b> gate                                                         # must end: gate PASS
python tools/art/texture_audit_render.py --renderer tools/anim/render_geo.py --out <scratch>/r --builder <b> --house <house>
python tools/art/house_palette.py check --dir <scratch>/pv --house <house>   # OK
python tools/art_hue_check.py --dir <scratch>/pv --house <house>             # <house> PASS
```
Renders: `<scratch>/r/troop_<tid>.png` (near and 16 blocks) and `<scratch>/r/<tid>/t<N>_far.png` (true size). The gate checks primary share >= 35%, trim+accent <= 20%, clamped <= 12%, light on tall faces. Then the 16-block gate (A8.4) and a fresh Sonnet 5.5 high grade, >= 8 on every rubric line (A8.8); the tools passing is not the grade.
**Install** (when the lead says; the troop must be in `sheets/design.json`):
```
python <b> install
python tools/build_skins.py --verify          # build_skins: <n> outputs verified
python tools/art/house_palette.py check <tid>
```
**Done when** all exit 0 and `models/troop/index.json` lists `<tid>`. **Commit** `art/hand/<tid>/`, `art/troops/<tid>.json` and `<tid>_s*.png` (and `_veteran.png` if written), and the build_skins outputs under `src/main/resources/assets/gravesandglory/models/troop/` and `textures/entity/troop/`.
**Kort**, only after the review grade: `- kort: Approve <tid>'s look (tiers I-III, near and 16 blocks)? | open: <scratch>/r/troop_<tid>.png | min: 3 | prio: 2`.
**Common failures.** A hex literal or another House's ramp; trim as the main mass; tier II without a plate-ramp mass; a reserved colour (`tidecallers.CORAL`): stop and ask (A9).

## 5. Animation

Read `.claude/skills/troop-animation/SKILL.md`, `references/pipeline.md` and `references/lessons.md`. `art/anim/**` and `tools/anim/**` belong to the animation lead: this stage runs in a lane the animation lead approves.

**5a. Register the troop** (no generator; copy `<peer>`'s entry everywhere it appears: `git grep -nw <peer> -- art/anim src/main/java`):
- `art/anim/common/troop_rig.py` `TROOPS`: `"<tid>": ("<tid>", "build_<tid>", "TROOP", "<weapon module>", "<weapon id>", <rotation>[, "bow"|"crossbow"|"thrower"])`.
- `art/anim/common/stances.py`: the gear group string of `<peer>` (`"onehand": "revenant tc_raider gb_hollowking"`, `"focus": ...`).
- `src/main/java/gg/gravesandglory/GeoTroopRenderer.java` `PREVIEW` (to `APPROVED` only on the booth verdict).
- **Sword and shield troops** (knight builder, `GG_ANIM_TROOP`) have no family table; pick as `<peer>` a troop with the same weapon and shield setup, run `grep -n <peer> art/anim/knight/build_anims.py` and add `<tid>` beside it at every hit (shield troops also sit in `SHIELD` and further lists); the sword spots are `CU` (cutlass follow-through), `if V2 and not SHIELD and TROOP in (...)`, `SLASH_FIT`, the `("low_cut", "overhead") if V2 and TROOP in (...)` clip list, `EDGE_SIGN`, `D3_TROOPS`.
- **Other families** have a per-troop table in their builder: `BOW_TROOPS` (`art/anim/archer/build_bow_anims.py`), `CAST_TROOPS` (`caster/build_caster_anims.py`), `XBOW_TROOPS` (`crossbow/build_crossbow_anims.py`), `SPEAR_TROOPS` (`spear/build_spear_anims.py`), `THROW_TROOPS` (`thrower/build_thrower_anims.py`); the greataxe builder runs through the spear builder (`SPEAR_TROOPS`); the axe builder (`axe/build_axe_anims.py`) has no table: grep it for `<peer>`. Each also imports the knight builder, so check it for `<peer>` too.

**5b. Build** (the builder, its variable and timing per pipeline.md section 2):
```
python art/anim/common/troop_rig.py <tid>
GG_ANIM_TROOP=<tid> python art/anim/knight/build_anims.py         # (bg) ~6.5 min
git status --short art/anim                                       # only <tid> and your registrations
python art/anim/common/export.py <tid>
python tools/anim/scorecard.py --troop <tid>  --json <scratch>/sc-<tid>.json
python tools/anim/scorecard.py --troop <peer> --json <scratch>/sc-<peer>.json
```
Clip names: gaits and stances are `<name>_legs` + `<name>_upper` (`walk_legs` + `walk_upper`); attacks `attack_<id>` + `attack_<id>_legs`. Render an upper clip with its legs clip:
```
A=src/main/resources/assets/gravesandglory
python tools/anim/render_geo.py --geo $A/geo/troop/<tid>.geo.json --texture $A/textures/entity/troop/<tid>_geo/t3_crimson.png \
  --anim $A/animations/troop/<tid>.animation.json --clip walk_legs --anim $A/animations/troop/<tid>.animation.json --clip walk_upper \
  --ticks 0:24 --views front,side,three_quarter --out work/render/<tid>-walk
```
**Scorecard, realistically.** Exit 0 is the goal, but shipped peers are not all green (on 6cc192ac `tc_raider` fails `pen_limb` on its rally/hold/fallback stances and `death_right*`). Compare the two JSON files: a clip red on `<tid>` and red on `<peer>` with a similar value is baseline; a clip red only on `<tid>` (dry run: `attack_low_cut edge_lead` 22.3 deg, limit 19.1) is yours to fix within the two rounds, usually in the registrations (`SLASH_FIT`, `EDGE_SIGN`). Never re-derive limits. `booth.py build` refuses any red clip, so for each clip still red: record clip, metric, value, limit and the peer's value in `docs/anim-rebuild/<TID>-REVIEW.md`, build the first booth round with `--clips` listing only green clips, and hand the red list to the animation lead in your report.

**5c. Harness, through the queue.** geo-knight is chosen by `GRAVES_PHASE1_CHECKS`; leave `--suites` out (it only sets `GRAVES_PHASE4_CHECKS`), so `status` shows `(no suites)`, which is right.
```
python tools/checkq.py submit client --root <tid>-r1 --env GRAVES_PHASE1_CHECKS=geo-knight \
  --env GRAVES_GEO_CHECK_TROOP=<tid> --env GRAVES_GEO_TROOPS=all
python tools/checkq.py wait <id>          # (bg) exit = the job's: 124 timeout, 125 cancelled, 126 queue error, 3 unknown job
grep -c "^\[p1\] p1-" work/ingame-check/<tid>-r1/gradle.log; grep -c "=false" work/ingame-check/<tid>-r1/gradle.log
```
Pass: 0 `=false` and the last line `p1-geo-knight-suite=true` (the dry run printed 23 `[p1] p1-` lines, the suite line included). The queue's `passed` only means gradle exited 0: a run with failed checks still shows `passed`, so always grep. **0 `[p1]` lines means the suite never ran** (the client crashed or failed to start): it is not a pass. Retry once (new `--root <tid>-r2`) only if the cause is clearly transient (a timeout on a loaded machine, a queue error); otherwise stop and report the job id and the first error lines of `gradle.log`. Known: in a fresh worktree the client can crash with `NoClassDefFoundError: gg/gravesandglory/BuildMode` because the main mod is not loaded (the mods list shows `gravesandglory_local_test_tools` but not `gravesandglory`). Cause: Loom fixes the launch classpath before compiling and drops build dirs that do not exist yet. Fixed (2026-11-09; the rerun after a build loaded the mod and ran every check): `tools/ingame_check_run.sh` and `tools/balance_sim.sh` now run `gradlew classes` first when `build/` is missing; if you still see it, run `./gradlew.bat classes --offline` once and resubmit.

**5d. Booth** (SKILL.md "Review booth"). Config `art/anim/<family>/booth.<tid>.json`: no generator (not built). Copy the closest troop's config, e.g. `git show anim/troop-revenant:art/anim/knight/booth.revenant.json` (others: `anim/troop-ic_hearthguard:art/anim/axe/booth.ic_hearthguard.json`, `anim/fix-bow:art/anim/archer/booth.<bow troop>.json`, `art/anim/knight/booth.knight.json`), change `troop` and the asset paths, and list every clip of `<tid>.animation.json` with `base` (its legs clip), `speed` (gait speed) and `impact_tick` (the builder's `IMPACT` value, never invented). Commit it, then
```
python tools/anim/booth.py build --config art/anim/<family>/booth.<tid>.json --target main [--clips <green list>]
```
Publish per SKILL.md step 4 (Artifact upload and ArtifactData writes). A worker without those tools stops here and reports the batch folder; its lead uploads. The inbox shows the batch to Kort automatically while `work/booth/<batch>/db/batch.json` has no `verdict.json`: no `- kort:` line. After Kort reviews, save the verdict to `work/booth/<batch>/verdict.json` and run `python tools/anim/booth.py decide --batch-dir work/booth/<batch>`: `land` (target main: move `<tid>` from `PREVIEW` to `APPROVED`, then stage 7 with the printed lane), `fix` (the listed fix list, review again, next round with `--batch`), `resubmit`, `wait`, `invalid` (stop, tell your lead).

**Done when** the scorecard is as above, the harness passed, the Sonnet review says SHIP, and `decide` prints `land`. **Commit** the registrations, `art/anim/<tid>/out/`, the exported assets (`geo/troop/`, `animations/troop/`, `troop_rig/`, `textures/entity/troop/<tid>_geo/`), `GeoTroopRenderer.java`, the review md and the booth config.
**Common failures.** A weapon pointing the wrong way (Kort rule 1); a clip keying a bone its controller does not own; `impact` differing from `attackVariants.impactTicks`; another troop's `out/` changed (shared knight builder); a speed check failing on a loaded machine (rerun once, record both).

## 6. Sound

**Default: no sound work.** `Audio.arm(u)` in `src/main/java/gg/gravesandglory/Audio.java` picks shared sounds from the Spec row: axe/mace/hammer heavy hit; other weapons blade swing and clash; spear/trident thrust; bow/crossbow shot; magic cast and impact; no weapon silent. Getting `weapon`, `family` and the magic/ranged flags right in stage 1 is the whole job.

**Own sound** (a beast, siege weapon or signature spell; the audio lead's call), all in one change:
1. `src/main/resources/assets/gravesandglory/sounds/<event>_1.ogg` ... `_4.ogg`: mono Ogg Vorbis 44.1 kHz, 2 to 4 variants.
2. `sounds.json`: event `<event>`, `"subtitle": "subtitles.gravesandglory.<event>"`, one `{"name": "gravesandglory:<event>_N", "stream": false}` per file (copy `tusk_attack`).
3. `src/main/java/gg/gravesandglory/AudioCues.java`: `NAME("<event>", null, volume, pitch, "hostile", Scope.NEARBY, cooldown, "Subtitle text", perTickCap)`, volume .3 to .6, cap 2 or 3; subtitle in `lang/en_us.json`.
4. Play site: `case "<tid>" -> AudioCues.Cue.NAME` in `Audio.shotCue` or `Audio.meleeCue` (and drop `<old>`'s case if it had one).
5. Credit in `docs/AUDIO-CREDITS.md`: CC0, CC-BY or similar only; no NC/ND/SA, nothing from a game or film.

**Check:** `python -I test-tools/standalone/test_audio_events.py` prints `OK` (also in verify fast, with `AudioCuesCheck`). **Not on main yet:** `tools/audio/mixfix.py --verify` (branch `audio/lead`). **Not built:** per-troop voice lines and House horns from a data file; do not wire voices by hand.

## 7. Land

```
python tools/verify.py fast                 # (bg) must exit 0
git status --short                          # empty
python tools/land.py unit/<tid> --dry-run
python tools/land.py unit/<tid>             # only after Kort (or the coordinator for him) approves the branch for main
```
Everything stages 1 to 5 list under **Commit** must be committed first; land refuses a dirty lane. `land.py` rebases on `main`, runs verify fast in the lane, fast-forwards `main`, retires the worktree, regenerates `docs/STATE.md` and refreshes main's graph (`--no-graph` skips; a graph failure only warns); it never pushes. CR-only noise on `Spec.java`/`preflight.json` is restored.
**Done when** it exits 0. Exit codes: 1 dirty lane, 2 rebase failed, 3 verify failed, 4 main moved, 5 merge refused, 6 landed but cleanup incomplete, 7 another land running, 8 a git command failed before main moved, 64 usage.
**Kort:** `- kort: Land unit/<tid> (spec, art, animation approved in booth)? | open: <commit or none> | min: 2 | prio: 2`.
