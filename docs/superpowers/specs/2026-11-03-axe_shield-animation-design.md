# Axe-and-shield troops in the video style

Kort, 2026-11-03 request (draft for his morning review). Branch `work/anim-rework`. References are saved locally in `C:/Users/kortt/Melty/anim-refs/axe_shield/` (`SOURCES.md`, frames in `frames/<move>/` at 10 fps; never committed).

## Why

Kort wants every unit family to move like good Souls-like game animation, with the weapon always facing the move. The `axe_shield` family has no clips at all today: the Hearthguard runs on the common hits and deaths and the engine's procedural layers, so it has no guard, no wall or brace hold and no authored swing. The knight's sword-and-shield rework (`2026-11-02-knight-video-style-design.md`) is the model: low, shield-forward stance, whole-body swings that start and end in the guard.

## Goals

- The Hearthguard gets an authored stance set and its two attack rows (`axe_shield_overhead_chop`, `axe_shield_hook_hew`) in the video style, with `_wall` versions for formation.
- On every key of both attacks the axe points the way the move needs (see the table; docs/ANIMATION-EDITING.md "Weapon direction"). A pose that is clean but points wrong fails.
- No self-clipping at any angle: axe, shield, arms, beard, pauldrons and crest.
- Each move is approved by Kort in game, one at a time.

## Units

Only one troop uses the `axe_shield` family (design.json, `family`):

| Troop | House | Entity / rig | Held item | Ability | Look |
|---|---|---|---|---|---|
| `ic_hearthguard` (Hearthguard) | Ironclad | `vindicator`, biped rig | `minecraft:iron_axe` shown as the `hearth_axe` weapon model (tiers I to III); shield on the left arm (`black:bo,light_gray:mc`) | `shield` (levels 2 and 3: passive shield, then `stance`) | Broad and short: body scale 1.2 x 0.95 x 1.15, arms 1.1 x 0.9, legs 1.1 x 0.8; slab pauldrons, box helm with crest (horns at stage 3), braided beard to the belt, scale-coat skirt, tabard. `idleArms: folded`. No back flag. |

Stats that matter to animation: melee, range 2.8, cooldown 28 ticks, speed 0.24 (slower than the knight's 0.28). Attack rows are `medium` weight.

## Inventory today

Engine rule (`TroopMotion.build`): clips of `clips/biped/axe_shield/` override `clips/biped/common/` by name. That folder does not exist.

| Clip the engine looks for | State for `axe_shield` | Source today |
|---|---|---|
| Stances `idle`, `carry`, `guard`, `brace`, `wall`, `shield`, `aim`, `rest` | missing | procedural layers only (folded-arms idle, shield arm carry 45 degrees forward) |
| `axe_shield_overhead_chop`, `axe_shield_hook_hew` (index -1) | missing | no authored attack; procedural pulse only |
| `<id>_wall` | missing | falls back to the plain attack (none) |
| `hit_head/body/legs` (+ `_2`, `_3`) | inherited from `common/` | generic |
| `block`, `block_head`, `block_legs`, `block_2` | missing | the sword_shield folder has them; common has none |
| deaths (`death*`, `fall`, `raise`) | inherited from `common/` | fine, no work |

For comparison the `sword_shield` folder holds: `idle`, `carry`, `guard`, `brace`, `wall`, `shield`, three swings and three `_wall` swings, `block*`, and (in progress, uncommitted) new `hit_*` clips.

### What the sword_shield rework gives and what must be axe-specific

Reusable as a start (copy to the new folder, then adjust):
- **Shield side and body:** `guard`, `wall`, `brace`, `shield`, `block*` for `body`, `head`, `left_arm`, `left_forearm`, `left_leg`, `left_shin`, `right_leg`, `right_shin`, `root`. These set the shield in front of the chest and the knee-bent crouch.
- **Structure:** the offset model (swing keys are deltas over the stance, first and last key 0), the end-key check, the `_wall` rule (tight, upright, root travel under 1 px).
- **Hits** (`hit_*`): reusable as they are for a one-handed troop, once the knight's hit rework is committed. The common hits already apply meanwhile.

Must be axe-specific:
- **Right arm, forearm and `right_hand`.** The sword clips key `right_hand` at [180, 169, 0] (blade along the forearm). The `hearth_axe` is gripped differently: grip "turned flat to the side, tilted 45 degrees forward" (tools/paint_house_troops.py), haft along the model's +Y, cutting bit on the -X side of the head (`blade` x -6 to -1.2, `bit` at x -8.2 to -7.5), beard hanging toward the hand (y 8) and a back spike on +X. The hand roll must be solved so the bit edge faces where the move says; do not reuse the sword's hand angle.
- **Reach.** A hand axe is about 26 px long (haft -7 to 15, spike to 18), far shorter than the sword, and the arms are scaled 0.9 in Y. The axe must be brought nearer the target by the body (root and torso lean), not by the arm, so strikes read as close-in chops.
- **The stance's weapon pose.** In `guard` the axe hangs low by the right hip, haft pointing forward-down, bit edge forward (video 0:48 and the other holds on the Hammerhead video).
- **Both attack clips.**

## References

Known tooling gap: `tools/anim_edit.py` line 29 `TROOP` lists only `sword_shield`, `bow` and `common`, so `axe_shield` cannot be opened or built until `'axe_shield': 'ic_hearthguard'` is added; `tools/clips.py` already globs `art/clips/biped/*/*.bbmodel`. Plan Task 0 does this as a tooling change that needs coordinator approval.

Details, URLs and per-timestamp notes: `anim-refs/axe_shield/SOURCES.md`. Both are Unreal mannequin asset previews, the only axe-and-shield footage found in two searches (no Souls-like gameplay capture with a clean single figure); they are 640x360, so the figures are small.

1. **RBStudio, "650+ Axe+Shield Animations"**, https://youtu.be/8jiz-5jf15U, local `rbstudio-axe-shield.mp4`. Main source. Round Viking shield and bearded hand axe, plain stage.
   - Light chain, LA1: 0:30.0 to 0:31.2 (hook hew).
   - Heavy chain, centre figure: 1:49.0 to 1:50.4 and 1:51.2 to 1:52.6 (overhead chop).
2. **Hammerhead Animations, "Axe and Shield Animation Set for UE4"**, https://youtu.be/KB-SWLjRgzs, local `hammerhead-axe-shield.mp4`. Clean fixed camera: the ready stance only. Frame-checked at 0:48, 0:52 and 1:00 (`frames/guard_hold/`): axe arm out to the axe side at hip height, haft about horizontal, head outboard, shield bent in front of the chest, knees bent. Our guard points the haft forward-down instead so the axe stays inside the 0.7-block cell. Few grounded swings.

3. **MythforgeGames axe**, https://www.youtube.com/watch?v=y8Qez-K8VhE, local `mythforge-axe.mkv` (frames `frames/myth_chop`, 14.9 to 16.5 s). Bearded hand axes, 1080p source. Shows how the axe sits in the hand (head up, bit forward) and the overhead path (raised straight above the head about 15.4 to 15.7 s, swept down across the front 15.9 to 16.1 s). Montage cuts, so it supports the chop but is not a clean single move.
4. **Sebastian Graves SHIELD pack**, https://www.youtube.com/watch?v=MJkHBwryr7Q, local `graves-shield.mkv`. One-handed shield idle, damage and walk posture on a labelled grid (times read from a contact sheet, not frame-checked). Shield side only. Frames checked (`frames/graves_shield/`): `ONE_HANDED_IDLE_01` shield held low at the side, about 0:16 (an earlier note said 0:08, which is `ONE_HANDED_LIGHT_ATTACK_01`); `TWO_HANDED_LIGHT_ATTACK_02` shield raised overhead at about 0:14.
5. **Revolt Studios Hunting pack** (Kort's find), https://www.youtube.com/watch?v=wH_w4QR5Xhk: checked, it is bows and hunting; contact sheet of the whole 117 s checked (`sheets2/revolt-hunting-axe_1.png`); no axe move, so not used here.

Key tables (numbers per bone per key, weapon direction in the same row) are in the plan, one table per move.

The video's own timing is quicker than ours in the wind-up and does not show the bit direction at this size; the edge and haft directions in the table come from the engine rule and the weapon model, with the video giving the body mechanics and arm path.

## Stances

Same two-stance ruling as the knight (Kort, 2026-11-02):
- **Formation** (`wall`, `brace`, `*_wall`): tight and upright for the 0.7-block cell. Copy the sword_shield versions, change only the right arm to hold the axe low and forward (haft pointing forward-down, bit forward), and check clipping.
- **Loose melee** (`guard` and swings thrown from it): the video's low pose. Knees bent, weight forward, shield at chest, axe low by the right hip with the haft pointing forward-down and the bit edge forward. The broad, short body means the crouch is a smaller root drop than the knight's (legs scale 0.8), so check the knee and shin against the scale-coat skirt and tassets.
- `idle` (no enemy, still): relaxed and upright, shield arm up, axe hanging at the side, haft pointing down, bit forward. The engine opens the folded arms whenever a clip holds an arm (`folds`), so the idle clip keys both arms and the troop stops looking folded-armed; that is a look change for Kort to confirm (question 3).
- `carry` (march): copy sword_shield's `carry` with the axe over the shoulder or at the side, haft pointing up-back and bit forward, so it never swings into the beard.
- `shield` (shield raised): copy; the axe stays low at the hip.
- `aim` and `rest` are ranged only; not needed.

## Moves

Attack ids and timings come from `Spec.ATTACKVARIANTS` and are fixed. Clip lengths: chop 1.2 s, hew 1.1 s (inside the 40-tick limit). Impact marker on the fixed tick. The key times below are offsets over the stance (the clip is an action layer; first and last keys 0). Angles are the builder's call; the weapon direction is not.

"Video" cites seconds on `rbstudio-axe-shield.mp4`, frames checked in `frames/<move>/`.

### axe_shield_overhead_chop (head, slash, impact tick 10 = 0.50 s)

Video: heavy chain, 1:49.0 to 1:50.4 (frames `frames/overhead_chop_HA_a`) and 1:51.2 to 1:52.6 (`overhead_chop_HA_b`). Start and end stance: `guard` (loose) or `brace`/`wall` (via `_wall`).

| Key (clip time) | Video time | Pose | Weapon direction |
|---|---|---|---|
| Wind-up 0.00 to 0.15 | from the hold | Weight sinks and rocks back, shield tucks to the chest, axe hand begins to rise past the hip | Axe leaves the hip, haft swings from forward-down to vertical-forward, bit forward |
| Wind-up peak 0.28 | 109.0 to 109.2 s (1:49.0 to 1:49.2; 1:51.8 in HA_b) | Right arm straight up, hand above and just behind the head, torso leaned back, hips loaded | Haft near vertical, tilted slightly back; head of the axe above the hand, bit edge forward and up, so it falls edge first. Not behind the neck with the bit toward the back |
| Impact 0.50 | 109.3 to 109.5 s (1:49.3 to 1:49.5) | Torso folds forward, hips turn into the blow, arm extended forward and down at head height of the target, lead foot steps in (root forward 1 to 2 px, legs left to foot planting), shield stays forward | Bit edge leading, haft about 45 degrees below horizontal pointing forward-down at the target's head, bit edge forward and down |
| Follow-through 0.70 | 109.6 to 109.8 s (1:49.6 to 1:49.8) | Deep lunge, axe swept down in front of the lead leg, shield up | Haft pointing forward-down about 60 degrees below horizontal, bit edge down toward the ground in front of the lead knee; never through the shin |
| Recovery 1.20 | 110.0 to 110.4 s (1:50.0 to 1:50.4) | Back to the stance | Axe returns by the hip to the guard pose, haft forward-down, bit forward |

Frames re-checked: in `overhead_chop_HA_a` f001 to f003 (109.0 to 109.2 s) the arm is straight up and the axe is out of frame above the head, so no axe is visible at the peak; the chop descent runs 109.3 to 109.8 s: f005 to f006 (109.4 to 109.5 s, our impact) show the axe small and low at the lead hip; f007 to f010 (109.6 to 109.8 s) show it low in front of the lead leg. The video chop therefore lands at waist height, ours at the target's head (head zone); the video gives body and arm mechanics only, and the edge and haft directions come from the engine rule and the axe model. Retiming: the video's wind-up peak to low point takes about 0.4 s; ours puts the peak at 0.28 s and impact at 0.50 s, so the descent is 0.22 s (a fast drop, which is correct for a chop), and the video's long crouched recovery is kept to 0.5 s. The key at 0.50 stays on the fixed tick.

### axe_shield_hook_hew (body, slash, impact tick 8 = 0.40 s)

Video: light chain LA1, 0:30.0 to 0:31.2 (`frames/hook_hew_LA1`). Start and end stance as above.

| Key (clip time) | Video time | Pose | Weapon direction |
|---|---|---|---|
| Wind-up 0.20 | 0:30.1 to 0:30.2 (f002 to f003: arm swings out to the axe side) | Hips and shoulders turn away, axe drawn back and out on the axe side at hip to chest height, shield forward | Haft roughly horizontal, pointing back and out to the axe side; bit edge facing the shield side (the way it will travel), ready to lead; beard down |
| Impact 0.40 | 0:30.3 (f004: axe level across the front at belly height) | Hips lead, the arm sweeps level across the front at belly to chest height of the target (body zone) | Haft points forward, level and across the body; bit edge faces the travel direction (toward the shield side) |
| Follow-through (hook) 0.58 | 0:30.4 to 0:30.6 (f005 to f007: axe at the chest, hooking in), carried on 30.8 to 31.1 as the axe swings back out to the guard | Axe hand pulls in toward the torso, elbow bends, hips finish turned | Haft roughly horizontal across the chest; the beard (hook) turned inward toward the body, so it would catch a shield rim and pull it toward the troop. The visual hook only (see Fixed limits) |
| Recovery 1.10 | 0:30.7 to 0:31.1 (f008 to f012; follow-through reads 30.8 to 31.1) | Back to the stance | Axe returns to the hip, haft forward-down, bit forward |

Retiming: re-checked frame by frame (the first draft put the impact at 30.4 s, where the axe is raised at the shoulder). In `hook_hew_LA1` f001 (30.0 s) is a raised start pose that is not our wind-up; the swing runs f002 (wind-up) to f004 (level cross, our impact) to f007 (hook). Video wind-up to cross is about 0.2 s, ours 0.2 s to 0.4 s, so the arm path is nearly 1:1 and the extra clip time goes to the pull and the recovery. The axe is small at 640x360: the video gives arm path and body mechanics only; the haft and bit directions in the table come from the engine rule and the axe model.

### Moves from the video not used or missing

- The video has no good block or parry for this troop in frames we checked; `block*` comes from the sword_shield set (copy, adjust the right arm). The hit lineup at about 7:30 to 9:00 on the RBStudio video is unchecked.
- A forward shield bash and the jump slam (Hammerhead 0:59.8 to 1:02.0) are in the footage but not in the attack rows; no use unless Kort wants a new move (that would be a gameplay row change, so Kort first).

## Application to units

Hearthguard (the only unit):
- **Model and held item.** The axe is the `hearth_axe` weapon model (tools/paint_house_troops.py line 1174), with `tier` 1 to 3. The hand roll for the grip must put the bit edge and haft where the table says on all three tiers; tier 3 adds `crown_spike` (y 18 to 19.5, 1.5 px above the spike), `rune_plate` and `butt_spike` (`hearth_axe`), so the axe reaches about 19.5 px above the grip origin: check the chop's wind-up against the horned helm.
- **Scale.** The row (tools/paint_house_troops.py line 533, `ic_hearthguard`) sets bone scales body [1.2, 0.95, 1.15], arms [1.1, 0.9, 1.1], legs [1.1, 0.8, 1.1] and no root scale. `TroopModel.rig` (src/main/java/gg/gravesandglory/TroopModel.java line 314) computes `rootK = (24 - model root pivotY) / (24 - rig root pivotY)`, and the engine uses `Math.max(1, rootK)` for root offsets. The rig root (`art/rigs/biped.json`) has pivot [0,0,0] and the model root is generated from the same rig with no root override, so rootScale = (24 - 0) / (24 - 0) = 1.0 (derived, not read from a log; the first render confirms it). Root offsets in the plan are therefore rig px as written.
- **Clipping risks.** Axe haft/head through the helm and crest at the chop's wind-up; beard braid (hanging to the belt) with the axe hand in the hew's pull; slab pauldrons with the raised arm; shield board with the axe arm in the hew's cross; axe through the lead knee or tassets in the chop's follow-through; the tabard/skirt springs count in the MotionEngineCheck hit-over-attack gate.
- **Formation.** `wall` and `brace` plus the `_wall` versions stay inside the 0.7-block cell (root travel under 1 px; no axe tip out of the cell).
- **Ability visuals.** The `shield` ability is a passive (level 2) and the level-3 `stance`; neither has an attack clip. The shield-up stance (`shield`) is used when the ability raises it.
- **Needs Kort:** a shield-pull effect for the hook hew (gameplay), any timing change, and any new attack row.

## Fixed limits

- Impact ticks stay: chop 10, hew 8 (both `_wall` the same). Clips within `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40 ticks. Any change needs Kort.
- Weapon direction on every key (above).
- `python tools/clips.py` and `--verify` must pass; `Spec.java` and `sheets/preflight.json` only via `tools/preflight.py` (restore after verify with `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`).
- Hit over attack: the repo check's window (ticks 26 to 35) is empty for our clips, so the plan's scratch copy checks the recovery window, ticks 14 to 24, with the hit landing at tick 13; step with the hit minus step without at most 15 degrees per tick.
- No gameplay, save-format or network change. At most 2 Minecraft instances (Kort's counts); never PIDs 29360, 4580, 6048 or `C:/GGMatch`; no eula.txt; no computer-use on Minecraft. No push, publish or merge.

## Questions for Kort

1. **References.** Both videos are low-resolution (640x360) mannequin previews; neither is a Souls-like gameplay capture. OK to use them, or do you have a favourite axe-and-shield video?
2. **Hook that pulls a shield.** The hew's follow-through shows the beard hooking inward. Only a visual, or do you want a gameplay pull (needs your approval, not in this plan)?
3. **Idle look.** The Hearthguard idles with folded arms. An authored `idle` clip opens them (shield arm and axe arm out). Keep folded arms at idle (leave `idle` unauthored) or switch to the open idle like the knight's?
4. **Wall variants.** Add `_wall` versions of both attacks (as the knight has) or only the plain ones?
5. **Hits and blocks.** Copy the knight's new hit and block clips (the hits are not yet committed) once they land?

## Review

v1 6.3 (8/5/6/7/7/5)
Fix round 1: hew haft and bit directions corrected; hew and chop frames re-checked and re-timed (video gives body mechanics only); hit-over-attack window moved to ticks 14-24; TROOP gap and Task 0 extension stated; numeric rows for stances, `_wall` and blocks; Revolt, Hammerhead and Graves frames checked; scale and crown cited.

MotionEngineCheck note: `hitOverAttack` builds its family from `realSwordShield` (common + sword_shield) and plays the sword_shield thrust, so it does not cover axe_shield. Its window (ticks 26 to 35, hit minus no-hit step at most 15 deg/tick) is also empty for our clips: the chop ends at 1.2 s (tick 24) and the hew at 1.1 s (tick 22). The plan's scratch copy therefore lands the hit during the recovery and checks ticks 14 to 24 against the same 15 deg/tick limit.

v2 7.2 (9/7/7/7/7/6)
Fix round 2: Fixed limits now say the recovery window ticks 14-24; plan Tasks 2-4 give the anim_edit open/apply authoring flow with Expected lines and Task 0 copies sword_shield.bbmodel from HEAD; haft direction now includes right_hand terms with a computed (fwd, down, x) check column and a pose_preview note (haft line along --blade 0,-1,-1); Task 6 uses the real sword block values with key times and stances; Task 1 Step 2 has the scratch file path, mec.sh command and "worst <= 15.0"; idle body [1,3,0] and carry from sword -50/-128 with the axe on the shoulder; spec and SOURCES.md agree on chop 109.3-109.8 and hew follow-through 30.8-31.1; rootScale = 1.0 with formula and citation.

Final (after fix round 2): 7.0 (9/7/7/7/6/6). Known gaps for Kort:
- **Hew body and head yaw are inverted.** The shipped low_cut draws back with body y +34. The hew wind-up uses -28 and `hew_wall` matches it. Negate both before authoring.
- **Stub clips are missing.** `clips.py` fails until both attacks exist, so add stubs in Task 0.
- **The MEC claim is wrong.** The "empty window" note is incorrect: ticks 26-35 are attack ticks 6-15, inside both clips. The scratch fixture must keep the repo timing.
- **The fixture is underspecified.** It does not say how the family is built, which attack index is used, or give an Expected line.
- **Chop directions are rule-derived, not seen in the video.** This covers the bit and haft at 0.28, 0.50 and 0.70.
- **Blocks need checks.** Add an 8-angle render check of bit and beard against the shield for each copied block.
