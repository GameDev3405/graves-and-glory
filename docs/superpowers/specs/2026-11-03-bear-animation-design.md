# Bear (Old Horn) animation

Kort, 2026-11-03. Branch `work/anim-rework`. References: Runebear no-hit footage from Elden Ring (youtu.be/yE3Nf2XMuNU, local `C:/Users/kortt/Melty/anim-refs/bear/runebear.webm`) and the Rageclaw Grizzly pack trailer (youtu.be/938Px9NRK5Q). Frames in `anim-refs/bear/frames/<move>/` at 10 fps, notes in `anim-refs/bear/SOURCES.md`. Not committed.

## Why

Kort wants the units animated like Souls-like game art, with the weapon always facing the move. The bear's "weapon" is its claws and jaws. Today the bear has no animation work at all, because it has no model: the troop never reaches the clip engine (see Inventory). Before any clip can be authored a rig, troop art and a quadruped gait must exist. This spec records what the two attacks must look like and what has to be built first.

## Goals

- `bear_maul` (head, slash, impact tick 10, heavy): the bear rears back on its haunches and rakes one forepaw down onto the target's head, claws leading.
- `bear_bite` (body, stab, impact tick 8, medium): a low pounce, head and jaws leading into the target's torso.
- Heavy, weighty bear: slow telegraph, a violent short strike, a long recovery. Souls-like, not cartoon.
- Claws and jaws never point away from the target at any key (every key below states the direction).
- The trample's rear (`ArmyBear.rear`) keeps looking like a rear-up once the bear has a model.

## Units

| Troop | House | Entity / rig | Stats | Loadout | Ability |
|---|---|---|---|---|---|
| `wm_oldhorn` Old Horn, the Antlered Alpha | wildmoot, legendary | `polar_bear`, rig `bear` (new, see plan Task 0; `TroopModels.RIGS` maps it to `hound` today) | health 90, damage 11, range 3.0, speed 0.30, cooldown 30 ticks (1.5 s), cost 130, slot weight 4, tags beast,cavalry,large | weapon / helmet / chest `minecraft:air`, shield none, no held item, no hand items (the `bear` rig maps `hands` to `front_right_paw` / `front_left_paw`, unused) | trample: slam on charge contact, bear rears (Abilities.java:435 `bear.rear()`) |

It is the only troop of family `bear` (Spec.TROOPS last field). No other unit shares these clips. Texture `minecraft:textures/entity/bear/polarbear.png` (vanilla).

## Inventory today

Checked 2026-11-03.

- Render path: `TroopRenderer.render` line 96 draws any entity with no `TroopModel` through the vanilla renderer. `models/troop/index.json` lists only archer, gl_marshal, knight, longbow, mage, sentinel; there is no `wm_oldhorn` model, no `art/troops/wm_oldhorn.*`, no entry in `art/rigs/` for a bear. So Old Horn draws as the vanilla polar bear (`PolarBearEntityRenderer`) with vanilla poses. `TroopMotion.pose` is never called for it.
- Rig: `art/rigs/hound.json` is the wolf rig (bones root, body, chest, head, jaw, front_left_leg, front_right_leg, tail, back_left_leg, back_right_leg). Its note says the bear and ravager are "on their own boxes" but no such boxes exist in the file. Each front leg is a single bone: no elbow, no paw.
- Clip folders: `src/main/resources/assets/gravesandglory/clips/` holds `biped/{common,bow,caster,sword_shield}` only. `ClipLibrary.folder("bear","bear")` (the new rig and family folder) does not exist, and the hound folders it might fall back to are empty too, so even with a model the engine would find no stances, no `bear_maul`, no `bear_bite`, no hits, no deaths (`TroopMotion.build` lines 97-124: missing attack clip gives index -1 and the attack is dropped).
- Procedural layers are biped only: `MotionBody` gait writes `right_leg` / `left_leg` (MotionBody.java:96); `MotionFeet` uses right_leg/left_leg/right_shin/left_shin; arm layers use right_arm etc. A bear model would get no walk, no idle breath on its legs. A quadruped gait layer does not exist.
- Only motion today: vanilla's. The attack has no swing; the trample sets `setWarning(true)` for 20 ticks (`ArmyBear`, GravesAndGlory.java:487-497) and vanilla stands the bear up (6 ticks up, 6 down).
- `tools/clips.py` reads `art/clips/<rig>/<family>.bbmodel`, which does not exist for the bear (the new project is `art/clips/bear/bear.bbmodel`, clips in `clips/bear/bear/`); `tools/anim_edit.py` is hard-wired to RIG `biped` and TROOP `{sword_shield, bow, common}` (open `bear` exits 2).
- Family-owned vs inherited vs missing for every clip the engine would play:

| Clip group | Source today |
|---|---|
| stances idle, carry, guard, brace, wall, shield, aim, rest | missing (no folder); engine falls back to procedural, which is biped-only |
| `bear_maul`, `bear_bite` (+ `_wall`) | missing; `_wall` not wanted (a beast has no wall hold) |
| `hit_head/body/legs[_n]`, `block*` | missing (common folder is biped); a hound-rig `common` is also missing |
| deaths (`death*`) | missing; must end on the ground (clips.py check) |
| scenes, carries (bow, shield) | not applicable: no hands, no shield |
| trample rear | vanilla `isWarning` stand-up; lost the moment a TroopModel draws the bear (no vanilla pose code reaches a troop) |

## References

1. Elden Ring Runebear, Alejandro Leonor: the heavy rear-up claw slam (27.0-30.5 s) and the pounce (43.0-46.0 s). Dark gameplay footage but the silhouettes read. Souls-like, so these are the primary.
2. Rageclaw Grizzly trailer (Golderov art.): 28-clip pack; idle snarl close-up 0:00-0:04 only. Its attack list is unnamed, so it is not a move source.
3. Listings of Fab polar bear pack (bite, swipe left/right, double swipe, hit), Unity Wild Bear (Claw, Ram, Slam). Not watched. Candidates for the jaw close and a hit set if Kort wants a purchased reference.

Moves the video lacks: jaw closing on a target (not clearly in frame), idle breathing, walk gait, hits, death. Proposed source: author from the quadruped weight rules of the Runebear footage plus the vanilla polar bear's own walk; check a second video for the bite before Task 3.

**Supersedes the committed hound spec.** `docs/superpowers/specs/2026-11-03-hound-animation-design.md` line 33 says `TroopModels.RIGS` maps wolf, polar_bear and ravager to the `hound` rig. Plan Task 0b changes the `polar_bear` entry to `bear` (wolf and ravager stay on `hound`), so that line is superseded for `polar_bear` once 0b lands. The hound docs are not edited here; the coordinator lists the line for Kort.

## Stances

Needed once the `bear` rig exists (all absolute; bone chain root > body > chest > head > jaw; front_*_arm > forearm > paw under chest):

- `idle` / `rest`: four planted paws, head low (head x +10), slow breath on chest (x +-1.5), tail still.
- `carry` (marching): needs a gait loop. No gait layer exists for a quadruped, so a loop clip `walk`/`carry` keyed on the four legs is part of Task 0 (needs coordinator; the one shared quadruped Task 0 of the hound, ravager, spider and bear plans, built once).
- `guard` (fights, enemy near): front paws wide, root y +1.2 (crouch, y down, as shipped guard.json), chest x +4, head x +10, jaw +2. Every attack starts and ends here.
- `brace`, `wall`, `shield`, `aim`: not used by a bear; `STANCES` falls back to `guard`/`idle` when absent.

## Moves (breakdown)

Sign convention (copied from shipped clips, to verify on the first render): positive body/chest/head x leans or pitches the nose DOWN (sword_shield_thrust body x +12 at impact leans into the target), negative x pitches nose UP; a leg hanging down with x -90 points its tip horizontally forward and -180 straight up; right limb outward = z negative (shipped low_cut right_arm z -40); +y turns right; root y positive = down (guard crouch +1.2), root z negative = toward the target (shipped overhead -1.8). The bear faces -z. "World pitch of the claw direction" = body + chest + arm (+ forearm) summed, with 0 = straight down, -90 = forward level, -180 = straight up.

**Rig decision (fix round 1): a new `bear` rig, not the hound rig.** Paths everywhere: rig `art/rigs/bear.json`, project `art/clips/bear/bear.bbmodel`, generated clips `src/main/resources/assets/gravesandglory/clips/bear/bear/`. Bones: root, body, chest, head, jaw, front_left_arm, front_left_forearm, front_left_paw, front_right_arm, front_right_forearm, front_right_paw, tail, back_left_leg, back_right_leg (arm = shoulder pivot under the chest, forearm = elbow, paw = claws). Reason: the hound rig has a single 8 px front leg with no elbow or paw, so it cannot show level claws at the bite or a paw raised and swung down at the maul, and its wolf boxes are far too small for a bear. Size note: on the hound rig the head sits about 13 px above the ground and the 8 px front leg is shorter than the head height; the bear's arm + forearm + paw must total at least the head height or a level claw cannot reach a biped's head. The quadruped gait layer (plan Task 0) is built once for every four-legged rig and must read the leg bone names per rig.

**Target dimensions (px, 16 px = 1 block; the Task 0a pass/fail check, measured on the finished `art/rigs/bear.json` at idle, root y 0):** shoulder pivot (front_*_arm) 17 to 19 px above the ground; head centre 19 to 21 px above the ground and never lower than the shoulder; arm + forearm + paw length 16.5 to 19 px and within 1 px of the shoulder height, so planted paws touch the ground (guard's crouch of +1.2 px root y is absorbed by the paw, as with the biped's feet layer); head box 8 px or more tall so the head reads against the antlers; jaw pivot on the muzzle underside. Why these values: a claw raised to -180 reaches shoulder height + arm length, about 36 px (2.25 blocks), above a 1.8-block biped's head, and a level claw at -90 sits at shoulder height (about 1.1 blocks), the biped's chest. The hound rig for comparison: shoulder about 10 px, leg 8 px, head about 13 px, so it fails every one of these. If the rig owner proposes other values, they replace these here and in plan Task 0a before art starts.

### `bear_maul` (head, slash, impact 10 = 0.50 s, heavy, video 27.0-30.5 s)

Start/end stance: `guard`. Clip length 1.50 s (30 ticks, equal to the cooldown; under the 40 cap). Video's 3.5 s is retimed: the crouch (27.0-28.6) is squeezed into 0-0.15 s, the rise and paw lift (28.7-29.3) into 0.15-0.35 s, the apex hold (29.4-29.9) into 0.35-0.40 s, the throw and slam (30.0-30.3) into 0.40-0.50 s, the settle (30.4-31.5) into 0.50-1.50 s. Every key sits on the 0.05 s (one tick) grid. The telegraph is therefore much shorter than the video's (0.35 s to the apex against 2.3 s); see Questions. In v1 the clip was 1.60 s (32 ticks), longer than the 30-tick cooldown; it is trimmed to 1.50 s so a second maul cannot start while the first still plays.

Key table (deltas on `guard`; absolute = guard + delta; guard: body 0, chest +4, head +10, arms 0, root y +1.2). Pitch rule: world pitch of the right claws = body x + chest abs x + arm x + forearm x (0 = down, -90 = level forward, -180 = up). Yaw rule: net claw yaw = body y + chest y + arm y (negative = left).

| Key | t (s) | body xyz | chest x, y | head x / jaw x | right arm / forearm / paw (x, y, z) | left arm x | root (x,y,z px) | Claw pitch sum, yaw, direction |
|---|---|---|---|---|---|---|---|---|
| Load (video 28.0) | 0.15 | -8, +6, 0 | -6, 0 | -4 / +10 | +20, +6, -8 / 0, 0, 0 / -6 | +6 | 0, +1.8, +1.0 | body -8 + chest abs -2 + arm +20 = +10: claws point down, 10 deg BEHIND vertical; the paw is drawn back and out to the bear's right (z -8, y +6) |
| Rear apex (video 29.5) | 0.35 | -50, +10, 0 | -12, 0 | -12 / +30 | -88, +14, -22 / -4, 0, 0 / -10 | -50 | 0, -1.0, +0.8 | -50 - 8 - 88 - 4 = -150: claws UP and forward, 60 deg above level, splayed, above and just right of the head (yaw +24 right, away from the target, part of the wind-up); torso nose-up, jaw open (roar) |
| Apex hold (video 29.9) | 0.40 | -52, +10, 0 | -12, 0 | -12 / +30 | -90, +14, -22 / -4, 0, 0 / -10 | -52 | 0, -1.0, +0.8 | -52 - 8 - 90 - 4 = -154: the same pose drifted 4 deg so the hold does not freeze; claws up and forward |
| Impact (video 30.2) | 0.50 | +6, -8, 0 | +8, +6 | +12 / 0 | -70, -4, +4 / -10, 0, 0 / -2 | -20 | 0, +0.5, -2.0 | 6 + 12 - 70 - 10 = -62: claws forward-down, 28 deg below level. Yaw -8 + 6 - 4 = -6: the slash crosses from the bear's right and the claws end aimed at the target's centre line (the right paw sits about 4 px off it, 7 deg at range 2), tips on the target's head; head and jaw aimed at the same point |
| Follow-through (video 30.4) | 0.70 | +12, -8, 0 | +14, 0 | +14 / 0 | -20, -14, +8 / -6, 0, 0 / 0 | -6 | 0, +1.8, -2.4 | 12 + 18 - 20 - 6 = +4: claws straight down, planted past the target's centre line (yaw -22 left: the slash carries across after the hit) |
| Recovery (video 31.0) | 1.10 | +3, -2, 0 | +4, 0 | +6 / +2 | -6, 0, 0 / 0 / 0 | -2 | 0, +1.4, -0.8 | claws near down; paws planted |
| End | 1.50 | 0 | 0 | 0 / 0 | 0 | 0 | 0 | back in `guard` (first and last key of every keyed channel is 0) |

Body mechanics: the haunches (body bone) pivot at the hips, so the rear is a hinge: body x -50 lifts the front half; the back legs get no key and stay on the ground (foot planting waits for a quadruped feet layer); the step is the root z only (+1.0 weight back at load, -2.0 at impact). Hips drop (root y +1.8) at the load and rise slightly at the apex; they do not rise through the slam. Head: chin up and jaw open at the apex (roar), eyes onto the target at impact. Easing catmullrom. Speeds per tick (0.05 s), claw pitch sum: Load to apex +10 to -150 = 160 deg over 4 ticks = 40 deg/tick; apex to hold 4 deg in 1 tick; **strike -154 to -62 = 92 deg over the 2 ticks 0.40-0.50 = 46 deg/tick** (the body alone swings -52 to +6 = **58 deg in 2 ticks, 29 deg/tick: this is the load the hit-over-attack check must survive**, since the check window, attack ticks 6-15 = 0.30-0.80 s, contains the whole strike); follow-through -62 to +4 = 66 deg over 4 ticks = 16.5 deg/tick.

### `bear_bite` (body, stab, impact 8 = 0.40 s, medium, video 43.0-46.0 s)

Start/end stance: `guard`. Clip length 1.40 s (28 ticks). Retime: the video's rear and swipe (43.0-43.9) is dropped (that is the maul); the pounce (44.0-45.0) and head thrust (45.4-46.0) become the bite: coil 0-0.15 s, lunge 0.15-0.30 s, jaw snaps shut by the impact at 0.40, jaw held closed at 0.45, shake 0.55-0.75, release and recovery 0.75-1.40 s. All keys are on the 0.05 s grid.

Sign rules as the maul. Snout pitch = body + chest abs + head abs (0 = level, + = nose down). Claw pitch = body + chest abs + arm + forearm (0 = down, -90 = level forward). Arms listed are both arms (z mirrored: right -8, left +8, so the paws land either side of the target, not on its centre line). Jaw is a delta on guard's +2: delta 0 is closed.

| Key | t (s) | body xyz | chest x | head x, y / jaw x | arm / forearm x (both) | root (x,y,z px) | Snout pitch, claw pitch, direction |
|---|---|---|---|---|---|---|---|
| Coil (video 44.0) | 0.15 | -4, 0, 0 | -4 | -14, 0 / +30 | +4 / 0 | 0, +2.0, +1.2 | snout -4 + 0 - 4 = -8: up and level, jaw open, aimed at the target's chest; claws -4 + 0 + 4 = 0: straight down, paws planted; haunches sunk |
| Lunge (video 44.8) | 0.30 | +6, 0, 0 | +8 | +4, 0 / +34 | -100 / -14 | 0, +1.0, -2.4 | snout 6 + 12 + 14 = +32: 32 deg below level, jaws wide open and leading; claws 6 + 12 - 100 - 14 = -96: forward, 6 deg above level, forepaws reaching either side of the head |
| Impact (video 45.6) | 0.40 | +8, 0, 0 | +10 | +2, 0 / 0 | -104 / -18 | 0, +0.8, -3.4 | snout 8 + 14 + 12 = +34: forward-down into the target's torso; jaw delta 0, CLOSED on the target at the impact tick (+34 to 0 across 0.30-0.40); claws 8 + 14 - 104 - 18 = -100: forward and level (10 deg above), either side of the target |
| Jaw held (video 45.7) | 0.45 | +8, 0, 0 | +10 | +2, 0 / -1 | -104 / -18 | 0, +0.8, -3.4 | pose of the impact, jaw squeezed 1 deg tighter: closed on the target |
| Shake A | 0.55 | +8, +4, 0 | +10 | +2, +12 / 0 | -104 / -18 | 0, +1.0, -3.2 | head yaws 12 deg right, snout and jaw still on the target; claws level forward |
| Shake B | 0.60 | +8, -4, 0 | +10 | +2, -12 / 0 | -104 / -18 | 0, +1.0, -3.2 | head yaws 12 deg left (a 24 deg swing from A) |
| Counter-key | 0.75 | +8, +1, 0 | +10 | +2, +4 / 0 | -104 / -18 | 0, +1.0, -3.2 | the shake settles, snout on the target |
| Release | 0.90 | +3, 0, 0 | +4 | -12, 0 / +14 | -60 / -8 | 0, +1.4, -1.0 | snout 3 + 8 - 2 = +9: lifting out, jaw opens to let go; claws 3 + 8 - 68 = -57: forward-down, 33 deg below level |
| Recovery | 1.10 | +1.5, 0, 0 | +2 | -4, 0 / +4 | -8 / 0 | 0, +1.4, -0.4 | snout 1.5 + 6 + 6 = +13.5: still 13.5 deg below level, easing up (the head key -4 lands at abs +6); claws 1.5 + 6 - 8 = -0.5: down, paws planted |
| End | 1.40 | 0 | 0 | 0 / 0 | 0 | 0 | `guard` |

Weapon direction rule for the bite: the snout and open jaw point at the target at every key from the Lunge on; at Coil they point at the target's chest, jaw open, ready to drive in. The jaw is closed on the target at the impact tick and stays closed through the shake. The forepaws reach forward at claw pitch -96 to -100 (level) from Lunge to Shake B, never behind the body, and drop to forward-down at Release.

### Other clips (key tables)

All values are deltas over `guard`; first and last keys 0; sign conventions as the maul. Hits must pass the hit-over-attack check (at most 15 deg/tick extra, plan Task 3).

**`rear`** (the trample warning; 20 ticks = 1.00 s: 6-tick rise, 8-tick hold, 6-tick drop; driven by entity `isWarning`; both paws up, claws up and forward):

| t (s) | body x | chest x | head x / jaw x | both arms x / forearm x (z: right -22, left +22) | root (y, z px) | Claw pitch, direction |
|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 / 0 | 0 / 0 | 0, 0 | `guard` |
| 0.30 | -50 | -12 | -12 / +30 | -88 / -4 | -1.0, +0.8 | -50 - 8 - 88 - 4 = -150: claws up and forward, jaw open (roar) |
| 0.50 | -52 | -12 | -14 / +30 | -90 / -4 | -1.0, +0.8 | -154: the hold drifts 4 deg, claws still up and forward |
| 0.70 | -50 | -12 | -12 / +30 | -88 / -4 | -1.0, +0.8 | -150: end of the hold |
| 1.00 | 0 | 0 | 0 / 0 | 0 / 0 | 0, 0 | `guard` |

Back legs unkeyed (planted).

**`death`** (does not loop; must end on the ground; weapon rule: jaw open and head lolling forward, forepaws fall with the body, nothing points at the sky):

| t (s) | body x | chest x | head x, y / jaw x | arms x (both) / forearm | root roll z, y px | Direction |
|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0, 0 / 0 | 0 / 0 | 0, 0 | `guard` |
| 0.20 | -6 | -4 | -10, 0 / +20 | -14 / 0 | 0, +0.5 | staggers back, head up, jaw open, claws forward-down |
| 0.60 | +8 | +6 | +14, +8 / +10 | -30 / -20 | 20, +4 | forelegs buckle, the nose drops; claws sum 8 + 10 - 30 - 20 = -32: forward-down, 58 deg below level |
| 1.10 | +10 | +8 | +18, +12 / +10 | -40 / -30 | 80, +8 | topples onto its side; claws sum 10 + 12 - 40 - 30 = -48 in the body frame: forward-down, trailing along the ground once the body has rolled |
| 1.60 | +10 | +8 | +18, +12 / +6 | -40 / -30 | 90, tuned | lies on its side, head down, claws forward-down along the ground (sum -48) |

The roll sign and the final root x / y offsets are not guessed: they are solved against the on-the-ground check of `tools/clips.py --verify` (every pivot and box corner within 0.1 px of the ground), because a root roll swings the body about the root pivot.

**`hit_head`, `hit_body`, `hit_legs`** (one of each, 0.50 s; a heavy bear flinches at about 0.4 of a biped's size; the sword_shield family has three variants per zone and the bear gets one, since a beast has no style variants; ask Kort if more are wanted):

| Clip | 0.00 | 0.10 | 0.50 |
|---|---|---|---|
| `hit_head` | 0 | head x -10, head y +6, jaw +10, chest -2, root z +0.8 | 0 |
| `hit_body` | 0 | chest x -4, body x -3, root z +1.0 | 0 |
| `hit_legs` | 0 | body x +3, both arms x -4, root y +0.6 | 0 |

The peak is under 10 deg in 2 ticks (about 5 deg/tick), inside the 15 deg/tick limit.

**`rest`**: root y +4 (belly down), chest x +6, head x +26, jaw 0, arms x +4; claws down, tucked. **`block`, `block_hold`, `shield`, `aim`, `wall`, `brace`**: not authored. Old Horn has shield none and no ranged weapon, so none can trigger, and the engine falls back to `guard` for a missing stance. **Carry**: the marching loop is the quadruped `carry` (plan Task 0 gait); there are no hand carries (bow, shield).

## Application to units

- Old Horn only. Held item: none (claws and jaws are bones, not a weapon model). The bear rig's `hands` map to `front_right_paw` / `front_left_paw`; the carries (bow, shield) never trigger because `shield` is false and `ranged` is false.
- Scale and model: no `TroopModel` exists, so there is no `rootScale`. `rootScale = (24 - bear root pivot y) / 24`; a bear rig that keeps root pivot 0 has scale 1.0. A bear is larger than a wolf, so its boxes (not the root scale) carry the size; the engine's pixel offsets (root y +1.8, z -3.4) are in model pixels and read as small on a big bear: re-tune after the first render.
- Antlers (Old Horn is the "Antlered Alpha"): the model needs antlers on the `head` bone; the maul's head x -12 / +14 swings them, so check they do not pass through the raised paw (right paw is outward, z -22) in the front, 3/4 and side renders.
- Clipping risks: the raised right paw crossing the head and antlers at the apex (kept outward by arm z -22); the jaw box against the chest at the bite's head x +14; the tail against the back legs under body x -50.
- Ability visuals: trample is a server-side slam plus the rear. The visual is the `rear` clip tied to `isWarning` (client reads the synced entity flag; no network change). Needs the coordinator to confirm that reading `isWarning` in TroopMotion is acceptable (it is a client-only change).
- Needs Kort: (a) building the bear rig, skin and model; (b) the shared quadruped gait layer; (c) the shorter-than-video telegraph (0.35 s); (d) any change to attack ticks (none proposed; the maul clip is 30 ticks to match the cooldown).

## Fixed limits

- Impact markers stay on the sheet's ticks: `bear_maul` 10, `bear_bite` 8; clip lengths maul 30 ticks, bite 28 ticks; every attack clip is at most `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40 ticks (2.0 s). Cooldown 30 ticks.
- `tools/clips.py --verify` must pass (impact on a tick, death on the ground, loops close). Spec.java only through `tools/preflight.py`; reset `sheets/preflight.json` and `Spec.java` with `git checkout --` after any verify or gradle run.
- Weapon faces the move (docs/ANIMATION-EDITING.md "Weapon direction"): the claws or jaws point at the target at wind-up, impact and follow-through. A key without a direction is a defect.
- No pennants or standards on the bear's back (Kort 2026-10-29). Antlers stay.
- Max 2 Minecraft instances; never touch PIDs 29360, 4580, 6048 or C:/GGMatch; no eula.txt; no computer-use on Minecraft; no push, publish or merge.

## Questions for Kort

1. The bear has no model today (vanilla render). Build a bear rig and `wm_oldhorn` troop art now (art work before animation), or keep the vanilla bear and skip clips? Recommended: build, with forearm and paw bones.
2. The maul telegraph shrinks from 2.3 s (video) to 0.35 s because the impact is fixed at tick 10. Accept, or allow the server to start the clip earlier (gameplay timing, needs Kort)?
3. A quadruped gait layer does not exist (procedural gait is biped only). Authoring the loop as clips for the bear only, or building the layer for all hound-rig troops?
4. Reference quality: the Elden Ring footage is dark. Is a purchased pack (Fab polar bear, Unity Wild Bear) acceptable as a clearer reference for the bite's jaw?

## Review

v1 6.5 (7/7/5/8/7/5). Fix round 1: the bite's claws are now keyed level (sum -96 to -100) instead of the false claim at -8/-12; the maul Load is a real draw-back (arm +20, sum +10); the maul is trimmed from 32 to 30 ticks and its impact yaw cut from -22 to -6 with a chest y key; added the maul 0.44 apex hold, the bite 0.46 jaw-closed key and a shake with its counter-keys, and the jaw now closes by the impact tick; one rig chosen (new `bear`, paths `art/clips/bear/bear.bbmodel` and `clips/bear/bear/`) with the 8 px hound leg noted; key tables added for rear, death, hits and rest, with block, shield, aim, wall and brace skipped for a stated reason; the plan now authors hits before the attacks, fixes the Task 5 invocation, states Task 0 as new rig, skin, model and engine work with an owner each, and specifies the anim_edit refactor.

v2 = 6.7 (7/7/8/6/7/5). Fix round 2: BearHitCheck loads clips with `Files.list` + `Clip.parse` and hard-codes the attack list (no ClipLibrary or Tactics, which import Fabric/MC); the circular order in Task 0 is broken (the clip-dependent load checks move after Task 2, Task 0 keeps a model-only render); a `solo_oldhorn` line for `run.sh` is specified with an Expected line; Task 1 drops the invented `new` mode for the hound/ravager recipe (`git show HEAD:...sword_shield.bbmodel`, rebuild the bones, `open bear --troop wm_oldhorn`, coordinator work); the maul keys are all on the 0.05 s grid with the strike recomputed as 92 deg over 2 ticks (46 deg/tick) and the body +58 swing named as the check's load; the death words now match the -32 and -48 sums; the bite recovery label says the nose is 13.5 deg down; target dimensions in px are the Task 0a check; the exact `TroopModels.RIGS` / `build_skins.py` change is named and the hound spec line 33 is marked superseded.

(Kort's own review notes go below.)

Final (after fix round 2): 7.5 (8/8/8/7/8/6). Open minor items (not fixed, two-round limit): run.sh needs `CLIPS=... bash run.sh oldhorn solo_oldhorn` (a tag first, or it runs nothing); the polar_bear RIGS entry is TroopModels.java line 84, not 83; build_skins.py LEGS/JOINTS need the bear paw and arm-chain bones; the maul paw sits about 7 deg off the centre line, which no check asserts. Known gaps for Kort: approve building the bear rig, skin, antlers and wm_oldhorn model; approve the shared quadruped gait and the `rear` warning hook; accept the 0.35 s maul telegraph and the authored bite jaw timing; clips, hit-check numbers and renders need a model and a game run.
