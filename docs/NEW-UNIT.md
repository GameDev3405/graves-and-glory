# New unit: from nothing to shippable

One checklist for adding a brand-new troop. Policy wins over this page: `docs/WORKFLOW.md`, then the skills and recipes cited below. `<tid>` is the troop id (`[a-z0-9_]`, House prefix such as `wm_`, `ic_`, `tc_`, `gb_`, `gl_`), `<house>` its House, `<lead>` the lead who briefed you. Run every command from the lane worktree in Git Bash unless it says PowerShell. Checked against `main` at 0dc1f9be (2026-11-09).

**Hard rules.** No push, no publish (the booth upload is the one allowed upload). Never hand-edit or commit `src/main/java/gg/gravesandglory/Spec.java` or `sheets/preflight.json`: `tools/preflight.py` writes them. Never start a Minecraft client or server yourself: in-game suites and sims go through `tools/checkq.py` (see stage 5). Never a Fable model. Reach `main` only with `tools/land.py`.

**Ask Kort** by having your lead add one line to `C:/Users/kortt/Melty/status/<lead>.md` (a worker returns the line to its lead; it does not write the status file itself):
`- kort: <one plain sentence> | open: <https link, path or none> | min: <minutes> | prio: <1 now, 2 this week, 3 whenever>`
`python tools/inbox.py` shows it (`--json` to check it parsed). Delete the line once Kort answers.

## Command sequence (one screen)

```
# 0  lane (the lead):  git worktree add ../bb-unit-<tid> -b unit/<tid> main ; cd ../bb-unit-<tid>
# 1  spec      edit sheets/design.json + test-tools/standalone/HousesCheck.java ; python tools/preflight.py
# 2  lore      git show docs/lore-lead:docs/lore/season-framework.md   (section 5, eight rules) -> pitch to lore lead
# 3  model     mkdir -p art/hand/<tid> ; cp art/hand/_template/build_template.py art/hand/<tid>/build_<tid>.py
#              python art/hand/<tid>/build_<tid>.py preview <scratch>/pv crimson
# 4  texture   python art/hand/<tid>/build_<tid>.py gate
#              python tools/art/house_palette.py check --dir <scratch>/pv --house <house>
#              python tools/art_hue_check.py --dir <scratch>/pv --house <house>
#              (review, then)  python art/hand/<tid>/build_<tid>.py install ; python tools/build_skins.py --verify
# 5  anim      python art/anim/common/troop_rig.py <tid>
#              <VAR>=<tid> python <family builder>      (both from pipeline.md section 2, e.g. GG_SPEAR_TROOP, spear)
#              python art/anim/common/export.py <tid> ; python tools/anim/scorecard.py --troop <tid>
#              python tools/checkq.py submit client --root <tid>-r1 --env GRAVES_PHASE1_CHECKS=geo-knight \
#                  --env GRAVES_GEO_CHECK_TROOP=<tid> --env GRAVES_GEO_TROOPS=all ; python tools/checkq.py wait <id>
#              python tools/anim/booth.py build --config art/anim/<family>/booth.<tid>.json --target main
# 6  sound     nothing by default ; python -I test-tools/standalone/test_audio_events.py
# 7  land      python tools/verify.py fast ; python tools/land.py unit/<tid> --dry-run ; python tools/land.py unit/<tid>
```

## Routing (WORKFLOW.md "Kort's model rules"; troop-animation skill "Routing")

| Stage | Author | Review |
|---|---|---|
| 1 Spec, 2 Lore, 6 Sound, 7 Land | Sonnet 5.5 high | one fresh Sonnet 5.5 high review per change |
| 3 Model, 4 Texture | Opus 5.5 medium (art authoring is important work) | fresh Sonnet 5.5 high, grade per TEXTURE-RECIPE A8.8 |
| 5 Animation | Opus 5.5 medium for builder/clip authoring; Sonnet 5.5 high for build, export, scorecard, renders, harness | fresh Sonnet 5.5 high (`.claude/skills/troop-animation/references/reviewer-prompt.md`) |

Stuck after a failed fix round: Opus 5.5 high. Two fix rounds at most, then escalate with evidence. Never Fable.

## 1. Spec sheet row

**Inputs.** Kort's approved design for the unit: House, rarity, role, stats, weapon family, ability, level-2/3 abilities, names. The lore never sets stats (lore rule 8); mechanics go through Kort and the balance process.

**Gap, read first: a seventh troop in an existing House fails today.** `tools/preflight.py` (`HOUSE_SHAPE`) requires every House to have exactly 2 Common, 2 Rare, 1 Epic, 1 Legendary, and `test-tools/standalone/HousesCheck.java` hard-codes the 30-troop `ROSTER` and the `launchRoster()` list. So a new unit is either a replacement in a House slot, a new House (houses row, glyph `textures/gui/house/<house>.png`, `houseTraits`, palette per TEXTURE-RECIPE part B), or needs a rule change. All three are Kort decisions. Ask before editing:
`- kort: Should <tid> replace a <house> slot, start a new House, or should a House allow more than six troops? | open: none | min: 5 | prio: 2`

**Rows to add in `sheets/design.json`** (copy a peer of the same House and family; every troop row must have exactly the keys of `troops[0]`, or preflight crashes generating `Spec.java`):

| Sheet | What | Rule preflight enforces |
|---|---|---|
| `troops` | the row, at its place in the House's run | `house`, `rarity` resolve; `slotWeight` = the rarity's; `ranged` true exactly when `range` > 3; `weapon`/`helmet`/`chest` are `minecraft:` ids; `family` one of `FAMILIES` (sword_shield, axe_shield, sword, axe, greatsword, spear, scythe, bow, crossbow, thrower, caster, crew, hound, spider, bear, ravager, colossus); `shield` is `none` exactly when `tags` lacks `shield`; `texture` an existing `gravesandglory:` asset or a `VANILLA_TEXTURES` path (peers use `gravesandglory:textures/entity/dawn_guard.png` or a vanilla mob); `ability` an `abilities` id; Common/Rare: `evolveLine` names a `levelNames` troop, Epic/Legendary: `none` |
| `houses` | add `<tid>` to that House's `troops` | must equal the House's troop ids in sheet order |
| `troopNotes` | `{id, beats, beatenBy}` | one row per troop, same order as `troops` |
| `levelAbilities` | `<tid>_2`, `<tid>_3` | exactly one each; level 2 keeps the base ability's `mechanic` |
| `levelNames` | `<tid>_2`, `<tid>_3` (Common/Rare only) | no name shared with another troop or level; none ends in ` (Veteran)` |
| `abilities` | new ability rows if the design has them | ids resolve |
| `attackVariants` | only for a new `family` | each used family has rows; melee needs one; `impactTicks` is gameplay (Kort) |

Player text never says stage, evolve or Writ (it says level, tier or Pact). Then update `ROSTER` / `launchRoster()` in `HousesCheck.java` to match.

**Commands.**
```
python tools/preflight.py
python tools/verify.py fast            # background, about 2 min
```
`tools/expand_design.py` is a one-off historical migration: never run it.

**Done when** preflight prints `Preflight clean: <n> filled cells; every row generates a Java record. Runtime verification pending.` (exit 0) and verify fast prints PASS (it runs preflight, the Python tests, `skin-assets` = `build_skins.py --verify`, `clips.py --verify`, every `test-tools/standalone/*Check.java` including HousesCheck, and the build). Commit `sheets/design.json` and the Java check only; `git status` will show the regenerated `Spec.java`/`preflight.json`: leave them uncommitted.

**Common failures.** `houses/<h>: N common troops; every House has exactly 2` (the gap above). `troopNotes: one row per troops row, in the same order`. `levelAbilities: <tid> needs exactly one level-2 row`. `KeyError` in preflight: a missing column. HousesCheck failure: the roster lists were not updated.

## 2. Lore row

**Where lore lives (2026-11-09).** Not on `main` yet: the reboot is on branch `docs/lore-lead` (worktree `bannerbound-lore`), page https://claude.ai/artifact/N5FnQy9GTjfB7s6Rir5LnL. Read:
```
git show docs/lore-lead:docs/lore/season-framework.md      # section 5 "Rules for adding a unit"
git show docs/lore-lead:docs/drafts/lore-bible-2026-11-09.md   # section 5, the House
git show docs/lore-lead:docs/lore/names.md
```
**Not built:** there is no per-troop lore column in `sheets/design.json` and no troop lore file. The only player text a troop carries is `troops.name`, `levelNames`, `troopNotes` and its ability rows (stage 1).

**One lore row needs:** House and species (rule 1); its magic as that House's glimpur, if any (2); one line on its place in the House's grievance, what wrong it answers or commits (3); its look within the House aesthetic, no violet on Wildmoot (4); whether it speaks (5); no giants before Act II (6); a role or title, not a new named character (7). Write it as a short pitch (name, House, the eight rules ticked, one flavour line) and send it to the lore lead for a yes.

**Done when** the lore lead approves the pitch (their reply or status file). Mechanical check: `python tools/preflight.py` still clean after the names land in the sheet.
**Kort:** only if the unit needs a new named character or a reserved look: `- kort: May <tid> be the named character <name>? | open: <pitch path> | min: 3 | prio: 2`.

## 3. Model

**Source of truth.** A troop's geometry and paint are one Python builder, `art/hand/<tid>/build_<tid>.py`, built on `art/hand/common/handlib.py` from the template `art/hand/_template/build_template.py`. The GeckoLib model (`src/main/resources/assets/gravesandglory/geo/troop/<tid>.geo.json`) is generated from it in stage 5; the legacy model (`art/troops/<tid>.json`, `models/troop/<tid>.json`) by `install` in stage 4. Never hand-edit either.

**Inputs.** The stage 1 row; the look and mark from your brief (existing roster: `docs/superpowers/specs/2026-10-17-unit-identity.md`, `docs/ART-STYLE-GUIDE.md` 10.x). No House, look or mark: ask before starting.

**Commands** (TEXTURE-RECIPE A2, A2.4, A2.5):
```
mkdir -p art/hand/<tid>
cp art/hand/_template/build_template.py art/hand/<tid>/build_<tid>.py
# edit: TID, HOUSE, delete the --house loop and other PRESETS, rename TEMPLATE, write NOTE, meta["weapon"]
grep -ho '"weapon": "[a-z_]*"' art/hand/*/build_*.py | sort -u      # existing weapon ids
python art/hand/<tid>/build_<tid>.py preview <scratch>/pv crimson
```
Keep the biped skeleton the animation builders expect: `limb()` joints, the hand bones, `arms_folded`, the `tabard` bone; at most 64 bones; the rig contract (`handlib` `ELBOW`/`KNEE`/`HAND`) is the animation lead's. A new weapon is a separate task (`weapon: TODO` in NOTE). **Gap:** non-biped units (beasts, siege crews) have no template; `wm_hound` has its own rig (`art/anim/wm_hound/build_rig.py`) and wm_stalker, wm_oldhorn, tc_brute, ic_ballista, ic_mortar, ic_colossus, reaper have no GeckoLib set yet. Such a unit needs an animation-lead design first.

**Done when** `preview` prints `preview written to <scratch>/pv` and writes `<tid>_t1..3.png`, `<tid>_t1..3.geo.json`, `skin_s1..3.png`, `cloth_<side>.png`, with no pack overflow.
**Common failures.** Gear moved off its limb with offsets instead of `dil`; pauldron `ROLL` sign (+14 makes wings, use -14); chest gear hidden behind the tabard (recipe A2.5).

## 4. Texture

Follow `docs/art/TEXTURE-RECIPE.md` part A exactly (part B for a new House); read `docs/art/unit-texture-audit.md` sections 2 and 4 first. Approved troops are never repainted.

**Commands** (`<b>` = `art/hand/<tid>/build_<tid>.py`; the renderer is on `main` at `tools/anim/render_geo.py`, so the recipe's `git show anim/geckolib-rebuild:...` copy is not needed):
```
python tools/art/house_palette.py show <house> <scratch>/<house>.png
python <b> preview <scratch>/pv crimson
python <b> gate
python tools/art/texture_audit_render.py --renderer tools/anim/render_geo.py --out <scratch>/r --builder <b> --house <house>
python tools/art/house_palette.py check --dir <scratch>/pv --house <house>
python tools/art_hue_check.py --dir <scratch>/pv --house <house>
```
**The gate.** `python <b> gate` must end `gate PASS` (exit 0): primary share >= 35%, trim+accent <= 20%, clamped <= 12%, top-to-bottom light on tall faces. Palette check prints `OK`; hue check prints `PASS`. Then the 16-block gate (A8.4: role, tier, House at 1x on each `t<N>_far.png`) and a fresh Sonnet 5.5 high grade, >= 8 on every rubric line (A8.8). The tools passing is not the grade.

**Install** (only when the lead says; the troop must already be in `sheets/design.json`):
```
python <b> install
python tools/build_skins.py --verify
python tools/art/house_palette.py check <tid>
```
**Done when** all three exit 0 and `src/main/resources/assets/gravesandglory/models/troop/index.json` lists `<tid>` (preflight checks it equals `art/troops/*.json`).
**Kort:** `- kort: Approve <tid>'s look (tiers I-III, near and 16 blocks)? | open: <scratch>/r/troop_<tid>.png | min: 3 | prio: 2`, only after the review grade.
**Common failures.** A hex literal or another House's ramp (palette check fails); trim as the main mass; tier II without a plate-ramp mass; a reserved colour (`tidecallers.CORAL`): stop and ask (A9).

## 5. Animation

Read `.claude/skills/troop-animation/SKILL.md`, then `references/pipeline.md` and `references/lessons.md`. `art/anim/**` and `tools/anim/**` belong to the animation lead: this stage runs in a lane the animation lead approves.

**Inputs.** Stage 4 installed; the `family` from stage 1; the builder for that family (pipeline.md section 2). Then register the troop where the builder reads it:
- `art/anim/common/troop_rig.py` `TROOPS`: `"<tid>": ("<tid>", "build_<tid>", "<TROOP ATTR>", "<weapon module>", "<weapon id>", <rotation>[, "bow"|"crossbow"|"thrower"])`;
- the family builder's own table (`SPEAR_TROOPS`, `BOW_TROOPS`, `CAST_TROOPS`, `XBOW_TROOPS`, `THROW_TROOPS`), and `art/anim/common/stances.py` if the family has per-troop stances;
- `PREVIEW` in `src/main/java/gg/gravesandglory/GeoTroopRenderer.java` (moves to `APPROVED` only when Kort's booth verdict says so).

**Commands.**
```
python art/anim/common/troop_rig.py <tid>
GG_SPEAR_TROOP=<tid> python art/anim/spear/build_spear_anims.py      # use your family's builder and variable (pipeline.md 2)
git status --short art/anim                                          # only <tid> changed
python art/anim/common/export.py <tid>
python tools/anim/scorecard.py --troop <tid>
python tools/anim/render_geo.py --geo src/main/resources/assets/gravesandglory/geo/troop/<tid>.geo.json \
  --texture src/main/resources/assets/gravesandglory/textures/entity/troop/<tid>_geo/t3_crimson.png \
  --anim src/main/resources/assets/gravesandglory/animations/troop/<tid>.animation.json --clip <clip>_legs \
  --anim src/main/resources/assets/gravesandglory/animations/troop/<tid>.animation.json --clip <clip> \
  --ticks 0:24 --views front,side,three_quarter --out work/render/<tid>-<clip>
```
**Harness, through the queue** (checkq is on branch `workflow/checkq`, being landed; route documented in docs/SETUP.md "Shared in-game check queue"; until it is on `main`, ask the coordinator to run it, never launch a client yourself):
```
python tools/checkq.py submit client --root <tid>-r1 --env GRAVES_PHASE1_CHECKS=geo-knight \
  --env GRAVES_GEO_CHECK_TROOP=<tid> --env GRAVES_GEO_TROOPS=all
python tools/checkq.py wait <id>          # as a background shell command; exit = the job's exit code
grep -c "^\[p1\] p1-" work/ingame-check/<tid>-r1/gradle.log; grep -c "=false" work/ingame-check/<tid>-r1/gradle.log
```
**Booth** (SKILL.md "Review booth"): add `art/anim/<family>/booth.<tid>.json` (model: `art/anim/knight/booth.knight.json`; `impact_tick` from the builder, never invented), commit, then
```
python tools/anim/booth.py build --config art/anim/<family>/booth.<tid>.json --target main
```
upload `work/booth/<batch>/upload.txt` files to the booth with the Artifact tool and write the batch with ArtifactData as SKILL.md step 4 says. The inbox picks up `work/booth/<batch>/db/batch.json` automatically while it has no `verdict.json`: no `- kort:` line needed. After Kort reviews: save his verdict to `work/booth/<batch>/verdict.json` and act on `python tools/anim/booth.py decide --batch-dir work/booth/<batch>`.

**Done when** scorecard exits 0 (a failing clip cannot enter the booth; `booth.py build` refuses it), the harness log has 22 `[p1] p1-` lines and 0 `=false`, the Sonnet review says SHIP, and `booth.py decide` prints `merge`. Then move `<tid>` from `PREVIEW` to `APPROVED`.
**Common failures.** The weapon points the wrong way (Kort rule 1); a clip keys a bone its controller does not own; `impact` marker differs from `attackVariants.impactTicks`; another troop's `out/` changed (the builders share the knight builder); a loaded machine failing a speed check (rerun once, record both).

## 6. Sound

**Default: no sound work.** `Audio.arm(u)` in `src/main/java/gg/gravesandglory/Audio.java` picks shared sounds from the Spec row: axe/mace/hammer heavy hit; other weapons blade swing and clash; spear/trident thrust; bow/crossbow shot; magic cast and impact; no weapon silent. Getting `weapon`, `family` and the magic/ranged flags right in stage 1 is the whole job.

**Own sound** (a beast, siege weapon or signature spell; the audio lead's call), all in one change:
1. `src/main/resources/assets/gravesandglory/sounds/<event>_1.ogg` ... `_4.ogg`: mono Ogg Vorbis 44.1 kHz, 2 to 4 variants.
2. `sounds.json`: event `<event>`, `"subtitle": "subtitles.gravesandglory.<event>"`, one `{"name": "gravesandglory:<event>_N", "stream": false}` per file (copy `tusk_attack`).
3. `src/main/java/gg/gravesandglory/AudioCues.java`: `NAME("<event>", null, volume, pitch, "hostile", Scope.NEARBY, cooldown, "Subtitle text", perTickCap)`, volume .3 to .6, cap 2 or 3; subtitle in `lang/en_us.json`.
4. Play site: `case "<tid>" -> AudioCues.Cue.NAME` in `Audio.shotCue` or `Audio.meleeCue`.
5. Credit in `docs/AUDIO-CREDITS.md`: CC0, CC-BY or similar only; no NC/ND/SA, nothing from a game or film.

**Check:** `python -I test-tools/standalone/test_audio_events.py` prints `OK` (also in verify fast, with `AudioCuesCheck`). **Not on main yet:** `tools/audio/mixfix.py --verify` (branch `audio/lead`). **Not built:** per-troop voice lines and House horns from a data file; do not wire voices by hand.

## 7. Land

```
python tools/verify.py fast                 # must PASS
git status --short                          # clean except regenerated Spec.java / preflight.json
python tools/land.py unit/<tid> --dry-run
python tools/land.py unit/<tid>             # only after Kort (or the coordinator for him) approves the branch for main
```
`land.py` rebases on `main`, runs verify fast in the lane, fast-forwards `main`, retires the worktree and regenerates `docs/STATE.md`; it never pushes. CR-only noise on `Spec.java`/`preflight.json` is restored, other dirt refuses.
**Done when** it exits 0. Exit codes: 1 dirty lane, 2 rebase failed, 3 verify failed, 4 main moved, 5 merge refused, 6 landed but cleanup incomplete, 7 another land running.
**Kort:** `- kort: Land unit/<tid> (<tid>: spec, art, animation approved in booth)? | open: <commit or none> | min: 2 | prio: 2`.
