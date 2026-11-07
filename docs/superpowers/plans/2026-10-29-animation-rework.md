> Note (2026-11-07): model and effort routing in this plan is superseded by docs/WORKFLOW.md.

# Animation Rework and Troop Models Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the stiff troop animation with a layered, per-unit, whole-body motion engine on a jointed rig. Make the approved gameplay changes:

- the hold-the-line shield wall
- the archer-fire crawl
- volley arcs
- impact-frame melee
- event-driven hits
- corpse fade

Model all 30 troops to the same standard, finishing the Gilded house from Freebuff's work.

**Architecture:** The server changes are pure `BattleMath` rules plus thin hooks in `Tactics`, `Combat`, `Projectiles` and `GravesAndGlory.Battle`. Each is tested standalone and then in a real client.

The client motion engine is pure Java with no Minecraft imports:

- `MotionMath`
- `Clip`
- `MotionEngine`

It is fed by a thin client adapter (`TroopMotion`) that reads entity state and three server events (`attack_v1`, `hit_v1`, `stance_v2`), and writes a `Pose` onto the jointed rig's `ModelPart`s.

Clips are hand-keyed in Blockbench and converted to JSON by `tools/clips.py`. The troop models are hand-authored in the Gilded house's 1-texel pipeline (`art/hand/<tid>/build_<tid>.py` on `art/hand/common/handlib.py`).

**Tech Stack:**

- Java 17, Fabric 1.20.1 (Loader 0.16.14, API 0.92.2+1.20.1, Yarn build.10, Loom 1.7.4)
- Python 3.12 tools
- Blockbench 5.2.2 through the MCP bridge
- `sheets/design.json` → `tools/preflight.py` → generated `Spec.java`

**Spec:** `docs/superpowers/specs/2026-10-29-animation-rework-design.md`. Read it first. Section numbers below (§) refer to it.

## Global Constraints

- **Policy:** WORKFLOW.md (canonical copy at C:/Users/kortt/Melty/bannerbound/docs/WORKFLOW.md)
  - one coordinator plus at most two helpers
  - one writer per file and per checkout
  - no push, publish or merge
  - local commits only on `work/anim-rework`
- **Who does the work (Kort, 2026-10-29):** Opus 5.5 at high effort does all the work: this session and any worker or reviewer subagent, each spawned with `model: opus`, `effort: high`. Escalate to Opus xhigh only when stuck. The Freebuff session is a reviewer and consultant only; it builds nothing new. Never use Fable models.
- **Kort's game:** never stop it. At most four harness clients in total. No compiling in a checkout with a running client. Use isolated run roots only.
- **`Spec.java`:** never hand-edit it. Change `sheets/design.json`, run `python tools/preflight.py`, and save the before/after hashes.
- **Freebuff's uncommitted files:** until Task 0 lands, don't edit `TroopModel`, `TroopRenderer`, `TroopFeatures`, `TroopModelLoader`, `TroopModels`, `art/hand/**`, `models/troop/{knight,sentinel,gl_marshal,archer,longbow,mage,index}.json`, `textures/entity/troop/<gilded tid>/`, or the Gilded `art/weapons/*`. After Task 0, all of them are this plan's to edit.
- **Art rules (Kort, all troops):**
  - Souls-like, not historical and not Minecraft-mod.
  - 1 texel per model pixel.
  - Each tier visibly grander than the one before.
  - No flags, pennants or standards on any troop's back; a quiver is fine.
  - Veteran trim: alpha 0 or 255 only, only on texels every tier paints, at most 20% of stage-1 opaque texels.
  - Every cube face a whole number of texels, with no coplanar overlapping faces.
  - The loader's `faces` count is 2–8, or the key is left out.
- **Quality bar:**
  - Animation clips pass at 10 on every rubric line (§10).
  - Models pass at 10 against Kort's references, with Kort's final word.
  - Two automatic correction rounds, then report the honest grade to Kort. Never label work 10 when it isn't.
- **Hit zones change only visuals.** Melee damage lands on the clip's impact frame (§11.2), and the swing cooldown counts from the swing's start.
- **Rig contract** (§8, reviewed by Freebuff):
  - `right_forearm`: child of `right_arm`, pivot `[-1, 4, 0]`.
  - `left_forearm`: child of `left_arm`, pivot `[1, 4, 0]`.
  - `right_shin` / `left_shin`: children of the legs, pivot `[0, 6, 0]`.
  - `right_hand` / `left_hand` move under the forearms, pivot `[0, 4, 0]`.
  - Upper limb boxes are 4×6×4 above the joint; lower limb boxes are 4×6×4 below it.
  - The hand stays at arm-frame `[∓1, 8, 0]`, exactly where it is today, so the held-item offset in `TroopFeatures` (about 2 px further down the arm, near arm y10) rides along and no weapon grip changes. Task 9 pins this.
- **Freebuff's Gilded handoff (2026-10-29), to keep:**
  - Sleeves that start below the elbow sit wholly on the forearm (the mage's bell sleeves, 5×5×5 at y4).
  - Robes and skirt flaps that cross the knee stay on the upper leg (mage robe tiers, archer, longbow, gl_marshal flaps) so a knee bend can't tear them; Task 16 checks they clear the shin at full bend.
  - The mage's robe stays rigid on the thigh, so the mage's knee flex is capped at 10° and it glides (Freebuff's rig review option B; my ruling, Kort may overrule).
  - The couter sits on the forearm and the knee cop on the shin, with a lip about 0.5 px proud covering the seam.
  - Weapon frames have at most 128 quads. Each weapon tier gets its own `_e` glow texture, because `glow_source` falls back to the base weapon's. A lit weapon texel counts 4 toward the rarity glow cap.
  - Keep `LEATH[4]` off team-tinted areas; it fails the team-tint check.
  - Bow troops use `draw-*`/`loose-*` shots, not `attack-*`.

## Review Focus

1. **Mixed infantry groups on Hold** (ic_hearthguard with ic_breaker; gl_marshal's large body). Shield units close up into a tight wall while others keep their own cell. Nobody overlaps or gets pushed out of the line. Pinned in Task 2.
2. **A melee strike whose target dies, walks off or is struck by another unit before the impact frame.** The blow misses cleanly: no damage, no kill credit, no double hit, and the attacker's cooldown is unchanged. A dead attacker's pending strike is dropped. Pinned in Task 5.
3. **Round end, map switch or chunk unload while corpses lie or strikes are pending.** Everything is cleared at once. No corpse survives into the next round, and no pending strike fires into a new battle. Pinned in Tasks 5 and 7.
4. **A client that joins mid-battle, or tracks a troop after its stance or attack started.** It gets the current stance and joins a clip in progress at the right time, without a pop. Pinned in Task 6 and Task 13.
5. **Frame-rate and partial-tick extremes** (20 fps vs 240 fps, lag spikes). Motion stays continuous with no pops, and weights and springs don't explode. Pinned in Task 10 (bounded per-frame delta, dt clamp).

---

## Phase 0: Base

### Task 0: Land Freebuff's Gilded work and take it over

Gate: Kort approves committing Freebuff's uncommitted work on `art/knight-1x` in the main checkout. Freebuff commits its own work as its last write; nothing is merged into its branch.

- [ ] **Step 1:** Ask Kort to approve the `art/knight-1x` commit. Freebuff makes it.
- [ ] **Step 2:** Rebase `work/anim-rework` onto that commit:

  ```bash
  git -C C:/Users/kortt/Melty/bannerbound-anim-rework rebase refs/heads/art/knight-1x
  ```

  Expected: the rebase is clean. Phase 1 commits touch none of Freebuff's files.
- [ ] **Step 3:** Confirm the handover with Freebuff by message: every Troop* class, Gilded builder, weapon and texture is now edited only on `work/anim-rework`. Freebuff reviews and advises on request.
- [ ] **Step 4:** Ask Freebuff to review the rig contract in Global Constraints against its Gilded builders before Task 9 starts.

Phase 1 doesn't wait for Task 0. Phases 2–5 do.

**Other worktrees (checked 2026-10-29):**
- `bannerbound-modes` (`mode/frame`) has uncommitted edits to `sheets/design.json`, `tools/preflight.py`, `Campaign.java`, `GravesAndGlory.java` and `DuelSimulation.java`, which Tasks 1, 5 and 7 also edit. Phase 1 starts only after that work is committed; rebase onto it, then regenerate `Spec.java` and `preflight.json` with `tools/preflight.py` instead of merging them.
- `bannerbound-models` (`work/troop-models-house`) is superseded by Phase 4 (Kort, 2026-10-29). Leave it untouched; don't merge or reuse its models or `tools/paint_house_troops.py`.

---

## Phase 1: Gameplay and network (server, starting now from `main`)

New `combatDiscipline` columns, added in Task 1 and used by later tasks. They get preflight bounds at tools/preflight.py:257-260.

| Column | Value | Bounds |
|---|---|---|
| `shieldWallCell` | 0.7 | (0.5, 1.3] |
| `crawlShare` | 0.33 | (0, 1) |
| `fireMemoryTicks` | 60 | ≥ 1 |
| `volleyPeakPerBlock` | 0.25 | > 0 |
| `volleyPeakMax` | 6.0 | > 0 |
| `crossbowArcScale` | 0.4 | (0, 1] |
| `thrownArcScale` | 0.6 | (0, 1] |
| `meleeReachGrace` | 0.5 | ≥ 0 |
| `corpseLieTicks` | 60 | ≥ 0 |
| `corpseFadeTicks` | 40 | ≥ 1 |
| `arcTravelScale` | 1.0 | [1, 2] |
| `mediumHitShare` | 0.10 | — |
| `heavyHitShare` | 0.25 | > mediumHitShare |

New sheet data:

- **`troops[].family`:** one of `sword_shield, axe_shield, sword, axe, greatsword, spear, scythe, bow, crossbow, thrower, caster, crew, hound, spider, bear, ravager, colossus`. Preflight rejects any other value.
- **`attackVariants`** table, one row per family attack from §6.2: `id`, `family`, `zone` (head, body, legs), `kind` (stab, slash, blunt, arrow, bolt, magic), `impactTicks` (an int ≥ 1, the impact frame in ticks from the swing's start), `weight` (light, medium, heavy).
- Preflight rejects:
  - an `attackVariants` row whose family no troop uses;
  - a melee family with no rows.

### Task 1: Sheet columns, families and attack variants; remove Shieldwall

**Files:**

- Modify:
  - `sheets/design.json`: the new columns and tables above. Delete the `shieldwall` formation row (~950), its UI list entry (~1834) and `shieldwallVsArchers` (~2002/2018/2034). Rewrite the lesson (~3006) and map beats (~8067, ~8127) without Shieldwall.
  - `tools/preflight.py`: bounds; family and variant validation; the formation id check at :150.
  - `BattleMath.java`: delete `shieldwallVsArchers`.
  - `Campaign.java:542-596`: delete the Hard bot's Shieldwall tactic.
  - `GravesAndGloryClient.java`: 138, 257, 353-360.
  - `BattleHud.java`: 290-291, 383, 390, 397.
  - `textures/gui/atlas.json`: 26-27.
  - `tools/ui_art/manifest.py`: 50.
  - `tools/ui_art/v4a_hud.py`: 481.
  - Current docs that name Shieldwall: `docs/PHASE-2-PLAYTEST*`, `docs/VISION-AND-ROADMAP*`, the SHOTLIST. Archived docs stay as they are.
  - `Tactics.java:51`: brace = shield or spear tag on Hold.
- Test:
  - `test-tools/standalone/test_preflight_rules.py`
  - `BattleMathCheck.java` (57, 62-64)
  - `LevelMathCheck.java:21`
  - `UiMathCheck.java` (176-177)
  - `Phase1Checks.java` (929-974, 1523)
  - `Phase2Checks.java` (1591-1616, 2578, 3888, 5316)

**Interfaces:**

- Produces:
  - `Spec.COMBATDISCIPLINE[0].shieldWallCell()` … `heavyHitShare()`
  - `Spec.TroopsRow.family()`
  - `Spec.ATTACKVARIANTS` (`AttackVariantsRow(id, family, zone, kind, impactTicks, weight)`)
  - `Spec.FORMATIONS` without `shieldwall`
- Steps:
  - [ ] **Step 1:** Write the failing preflight tests in `test_preflight_rules.py`:
    - `test_family_must_be_known` (`"family": "lance"` → error naming the troop);
    - `test_melee_family_needs_variants`;
    - `test_variant_impact_ticks_positive`;
    - `test_shieldwall_formation_rejected`;
    - `test_combat_discipline_new_bounds` (each new column at its bound → error).
  - [ ] **Step 2:** Run them and confirm they fail.

    ```bash
    python -m unittest discover -s test-tools/standalone -p test_preflight_rules.py
    ```

  - [ ] **Step 3:** Add the preflight validation, the sheet values, the family for every troop (from §6.2) and the variant rows from §6.2. Give each its `impactTicks`. Starting values are swing 8, overhead 10, thrust 7, heavy two-hand 12; Task 16 and later re-key them to the clips. Run preflight and save the hashes.
  - [ ] **Step 4:** Remove Shieldwall at every touchpoint in Files. Rewrite each listed test so it states the new rule:
    - Bots never pick Shieldwall.
    - Hold braces shield and spear troops.
    - The formation cycle has four entries.
    - The HUD has no Shieldwall glyph.
  - [ ] **Step 5:** Search for leftovers. Expect matches only in `docs/archive/**`.

    ```bash
    rg -n -i "shieldwall|shield wall" --glob '!docs/archive/**' --glob '!docs/superpowers/**'
    ```

    The phrase "shield wall" survives only where it describes the Hold behaviour.
  - [ ] **Step 6:** Run `python tools/verify.py fast --offline`. Expect a PASS.
  - [ ] **Step 7:** Commit:

    ```
    Remove the Shieldwall formation and add the animation sheet data
    ```

### Task 2: Shield troops close into a wall on Hold

**Files:**

- Modify:
  - `BattleMath.java`
  - `GravesAndGlory.java` (`Battle.formation` 826-836; the deploy slot that sets `Unit.deployed`)
  - `CommandMode.java:83-113` (the mirror)
  - `Tactics.java` (48, 150, 173: slot centre, goal, bridge; the `StepHop.Probe` at 210 must accept the narrower cell)
  - the `b.formation(u)` placement helpers in `Phase1Checks` and `Phase2Checks` that assume a group-wide cell
- Test:
  - `BattleMathCheck.java:31`
  - new `test-tools/java/gg/gravesandglory/HoldLineChecks.java` (suite `holdline`, registered with `HoldLineChecks.register(SUITES)` beside `LevelChecks.register` at Phase2Checks.java:54)
  - `Phase2Checks` p2-ironclad-spacing (2577-2593)
  - `VisualChecks.java:59`

**Interfaces:**

- Produces:
  - `BattleMath.shieldCell(double width, double wallCell) -> double`: `max(wallCell, width + 0.1)`.
  - `BattleMath.holdSlots(boolean[] shield, double[] width, double formationCell, double wallCell) -> double[] cells`: shield units take the front ranks at their shield cell. The others follow in later ranks at `max(formationCell, spacing(width))`.
  - `Battle.formation(Unit)`: same signature, new layout.
- Steps:
  - [ ] **Step 1:** Add the failing checks to `BattleMathCheck`:

    ```java
    check(BattleMath.shieldCell(0.6, 0.7) == 0.7, "a husk-wide shield troop takes the 0.7 wall cell");
    check(Math.abs(BattleMath.shieldCell(0.9, 0.7) - 1.0) < 1e-9, "a large shield troop takes its width plus 0.1");
    double[] c = BattleMath.holdSlots(new boolean[]{true,false,true}, new double[]{.6,.6,.6}, 1.8, 0.7);
    check(c[0] == 0.7 && c[2] == 0.7 && c[1] == 1.8, "shield units close up; others keep the formation cell");
    ```

  - [ ] **Step 2:** Run `python tools/verify.py fast --offline`. Expect a FAIL on the missing methods.
  - [ ] **Step 3:** Implement the two methods. Change `Battle.formation` and the deploy slot:
    - On Hold, shield-tagged units lay out first, in the front ranks, at their shield cell, keeping the formation's shape (line, block, wedge, loose).
    - Non-shield units of the group lay out in the ranks behind them.
    - Other orders use today's layout.
    - Mirror this exactly in `CommandMode`.
  - [ ] **Step 4:** Write `HoldLineChecks` `hl-wall`: spawn 12 knights and an ironclad group (6 hearthguard and 4 breakers) on Hold, run 200 ticks, then assert:
    - neighbouring knights' centres are 0.7 ± 0.05 apart;
    - no two bodies' boxes overlap by more than 0.02;
    - every breaker stands behind the hearthguard rank;
    - the gl_marshal centre gap is at least its width plus 0.1.

    Capture the screenshot `hl-wall.png`.
  - [ ] **Step 5:** Run the suite in an isolated root. Expect `[hl-wall] PASS`.

    ```powershell
    $env:GRAVES_ROADMAP_TEST_ROOT="work/hl/$(Get-Date -f yyyyMMddHHmm)"
    $env:GRAVES_PHASE2_CHECKS='holdline'
    .\gradlew.bat runClient -PlocalIntegration -I test-tools/isolated-checks.gradle
    ```

  - [ ] **Step 6:** Update `p2-ironclad-spacing` and `VisualChecks:59` to the new rule. Run `verify.py fast`. Expect a PASS.
  - [ ] **Step 7:** Commit:

    ```
    Close shield troops into a wall on Hold
    ```

### Task 3: Shields on Hold and under archer fire, crawl speed

**Files:**

- Modify:
  - `BattleMath.java` (`raiseShield` at 127-134)
  - `GravesAndGlory.java` (`Squad`: add `Map<String,Long> shotAt`)
  - `Projectiles.java` (`fire`: stamp the target's squad and group)
  - `Tactics.java` ranged target pick: stamp the target's squad when an enemy ranged troop in range picks a target in it (§5, "targeting")
  - `Tactics.java:122-123`
- Test:
  - `BattleMathCheck.java`
  - `HoldLineChecks.java`

**Interfaces:**

- Produces:
  - `BattleMath.raiseShield(String order, boolean underFire, double nearestSq, double reachSq, boolean hasEnemy) -> boolean`:
    - false on charge or retreat;
    - false when an enemy is within reach;
    - true on Hold when there is any enemy;
    - true on other orders when under fire.
  - `BattleMath.underFire(long now, long shotAt, int memoryTicks) -> boolean`: `shotAt > 0 && now - shotAt <= memoryTicks`.
  - The crawl uses the existing `BattleMath.speedModifier(crawlShare, swimming)` as a `MULTIPLY_TOTAL` movement-speed modifier with fixed UUID `CRAWL_ID`. It is applied while the shield is up, the squad group is under fire, and the order isn't Hold.
- Steps:
  - [ ] **Step 1:** Add the failing checks:

    ```java
    check(!BattleMath.raiseShield("charge", true, 50, 4, true) && !BattleMath.raiseShield("retreat", true, 50, 4, true), "never on charge or retreat");
    check(BattleMath.raiseShield("hold", false, 50, 4, true) && !BattleMath.raiseShield("hold", false, 3, 4, true), "hold: up, down to strike in reach");
    check(BattleMath.raiseShield("move", true, 400, 4, true) && !BattleMath.raiseShield("move", false, 400, 4, true), "moving: up only under fire");
    check(BattleMath.underFire(160, 100, 60) && !BattleMath.underFire(161, 100, 60) && !BattleMath.underFire(5, 0, 60), "fire memory");
    ```

  - [ ] **Step 2:** Run fast and confirm it FAILs. Implement. Run fast and confirm it PASSes.
  - [ ] **Step 3:** In `Tactics`, add and remove `CRAWL_ID` along with raising and lowering the shield, and on order change. Follow the HOWL pattern (`Abilities.java:689-709`): remove before re-adding, so it never stacks. It combines multiplicatively with HOWL, RALLY (`Commander.java:106`) and `moveMultiplier`.
  - [ ] **Step 4:** Add the checks to `HoldLineChecks`:
    - **`hl-crawl`:** knights ordered to move 30 blocks while 6 enemy archers shoot them.
      - Their mean speed while under fire is within ±15% of `crawlShare` times their unfired speed.
      - With the archers removed, the speed recovers within `fireMemoryTicks` + 20.
      - On Hold, a knight returning to its slot is not slowed.
    - **`hl-raise`:** a Hold wall raises its shields once an enemy is in sight, lowers a shield as an enemy enters reach, and never raises on Charge.
  - [ ] **Step 5:** Run the suite. Expect both PASS. Commit:

    ```
    Raise shields on Hold and under archer fire, crawling while moving
    ```

### Task 4: Volley arcs

**Files:**

- Modify: `BattleMath.java`, `Projectiles.java` (41 launch, 54-70 steering)
- Test: `BattleMathCheck.java`, `HoldLineChecks.java`

**Interfaces:**

- Produces:
  - `BattleMath.arcPeak(double dist, double perBlock, double max, double scale) -> double`: `min(max, dist * perBlock) * scale`.
  - `BattleMath.arcHeight(double t, double peak) -> double`: `4 * peak * t * (1 - t)`, for t in [0, 1].
  - `BattleMath.launchPitch(double dist, double dy, double peak, int ticks) -> double` (degrees): the angle of the path's initial tangent.
  - Projectiles carry `peak` and `ticks`. The steering aim point adds `arcHeight(age / ticks, peak)`. The launch velocity's vertical part adds `4 * peak / ticks`.
  - Travel time is `travelTicks × arcTravelScale` for arced shots; the sheet value starts at 1.0, so nothing changes until Kort asks for longer lobs (§5).
  - Scale by weapon: bows 1, crossbows `crossbowArcScale`, tridents `thrownArcScale`. Bolts and mortar are unchanged.
- Steps:
  - [ ] **Step 1:** Add the failing checks:

    ```java
    check(BattleMath.arcHeight(0, 5) == 0 && BattleMath.arcHeight(1, 5) == 0 && BattleMath.arcHeight(.5, 5) == 5, "arc is a parabola peaking mid-flight");
    check(BattleMath.arcPeak(22, .25, 6, 1) == 5.5 && BattleMath.arcPeak(40, .25, 6, 1) == 6 && Math.abs(BattleMath.arcPeak(22, .25, 6, .4) - 2.2) < 1e-9, "peak grows with range, capped, scaled");
    check(BattleMath.launchPitch(20, 0, 0, 10) == 0 && BattleMath.launchPitch(20, 0, 5, 10) > 30, "flat with no arc; lofted with one");
    ```

  - [ ] **Step 2:** Run fast and confirm it FAILs. Implement. Run fast and confirm it PASSes.
  - [ ] **Step 3:** Add `hl-arc`:
    - At 22 blocks, an arrow's peak height above the straight line is 5.5 ± 0.5.
    - It lands on `s.lands` within 0.3.
    - Its tick of landing equals `travelTicks`.
    - A crossbow bolt's peak is 0.4× a bow's at the same range.
    - Damage per second from 6 archers on a dummy line matches the pre-change baseline within ±3%, measured with the same seed before and after.
  - [ ] **Step 4:** Run, PASS, commit:

    ```
    Arc arrows and bolts by range without changing flight time
    ```

### Task 5: Melee damage on the impact frame

**Files:**

- Modify:
  - `BattleMath.java`
  - `GravesAndGlory.java` (`Battle`: add `List<PendingStrike> strikes`; tick it; clear it on round end, map switch and battle stop)
  - `Tactics.java:133-135`
- Test: `BattleMathCheck.java`, `HoldLineChecks.java`

**Interfaces:**

- Produces:
  - `record PendingStrike(GravesAndGlory.Unit attacker, LivingEntity target, String variant, long landsAt)`, nested in `Battle`.
  - `BattleMath.strikeLands(boolean attackerAlive, boolean stunnedSinceSwing, boolean targetAlive, double distSq, double reach, double grace) -> boolean`.
  - `BattleMath.pickVariant(int count, int last, long roll) -> int`: never repeats `last` when `count > 1`.
- Behaviour:
  - For melee, the swing (`swingHand`, the `attack_v1` send in Task 6, the cooldown) happens now.
  - `BattleEffects.strike` and `Abilities.attack` run at `landsAt = time + impactTicks`, and only if `strikeLands`.
  - Ranged and caster attacks are unchanged.
- Steps:
  - [ ] **Step 1:** Add the failing checks:

    ```java
    check(BattleMath.strikeLands(true, false, true, 9, 2.8, .5) && !BattleMath.strikeLands(true, false, true, 3.4*3.4, 2.8, .5), "in reach plus grace lands; beyond misses");
    check(!BattleMath.strikeLands(false, false, true, 1, 2.8, .5) && !BattleMath.strikeLands(true, true, true, 1, 2.8, .5) && !BattleMath.strikeLands(true, false, false, 1, 2.8, .5), "dead or stunned attacker, dead target: no blow");
    for (int i = 0; i < 50; i++) check(BattleMath.pickVariant(3, 1, i) != 1, "no immediate repeat");
    check(BattleMath.pickVariant(1, 0, 7) == 0, "a single variant always plays");
    ```

  - [ ] **Step 2:** Run fast and confirm it FAILs. Implement. Run fast and confirm it PASSes.
  - [ ] **Step 3:** Add `hl-impact`:
    - A knight's damage lands exactly `impactTicks` after its swing tick.
    - A target teleported 6 blocks away mid-swing takes no damage, and its attacker's next swing comes after the normal cooldown.
    - An attacker killed mid-swing deals nothing.
    - Ending the round with strikes pending leaves the list empty, and nothing lands in the next round.
    - Kill credit and Veterans fire once per kill.
  - [ ] **Step 4:** Re-baseline melee balance with `DuelSimulation`, using its existing standard matchups. Record the before/after win rates and time-to-kill in `docs/reports/2026-10-29-impact-frame-balance.md`. Flag any matchup that shifts more than 5 points to Kort. Don't retune silently.
  - [ ] **Step 5:** Commit:

    ```
    Land melee damage on the impact frame
    ```

### Task 6: `attack_v1`, `hit_v1` and `stance_v2`

**Files:**

- Create: `src/main/java/gg/gravesandglory/MotionEvents.java` (server-side ids, send helpers and the shared codec)
- Modify:
  - `GravesAndGlory.java` (ids at :50, START_TRACKING 86-98)
  - `Tactics.java` (stance send 57-62; the `STANCES` list)
  - `Combat.java` (send the hit after `target.damage`, using `bearing` and `raised`)
  - `GravesAndGloryClient.java:86-88` (receivers fill `MotionEvents.Client` queues, consumed in Task 13)
- Test: `HoldLineChecks.java`

**Interfaces:**

- Produces:
  - Ids `gravesandglory:attack_v1`, `hit_v1`, `stance_v2`. `stance_v1` is retired.
  - `MotionEvents.sendAttack(Unit u, int variantIndex, long startTick)`
  - `MotionEvents.sendHit(LivingEntity victim, String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal)`
  - `MotionEvents.sendStance(Unit u, String stance, float aimPitch)`
  - `MotionEvents.STANCES = List.of("", "hold", "brace", "wall", "shield_move", "fall_back", "run", "ranged_rest", "ranged_aim")`
  - Client side (`Attack` and `Hit` are the pure records in `MotionInput`, Task 12, so the engine stays free of Minecraft imports):
    - `MotionEvents.Client.attacks(int entityId) -> Deque<MotionInput.Attack>`
    - `hits(int entityId) -> Deque<MotionInput.Hit>`
    - `stance(int entityId) -> Stance`
  - Stance and the newest attack are re-sent on START_TRACKING, so a late joiner can start a clip partway.
  - Hits from non-troop sources map their kind as in §5:
    - player weapon → slash or stab by item
    - slam → blunt
    - blight, fire or magic → magic
    - fall → blunt to the legs

    Their direction is the bearing from the damage source's position, or front (0) when there is none.
  - `aimPitch` in `stance_v2` is `BattleMath.launchPitch` for the unit's current target, computed in `Tactics` where the ranged stance is chosen, so the bow angle equals the arrow's launch (§7.4).
- Steps:
  - [ ] **Step 1:** Write `hl-events`. A second, client-side check reads `MotionEvents.Client`. Assert:
    - Each knight swing produces an attack with the variant `Battle` picked and the server's tick.
    - An arrow on a shield wall arrives `blocked=true` with its zone from the impact height (`BattleMath.zone`, below).
    - A lethal blow arrives `lethal=true`.
    - A player who joins after the battle starts receives every tracked unit's stance within 2 ticks of tracking.
  - [ ] **Step 2:** Add the failing pure checks:
    - `BattleMath.zone(double impactY, double feetY, double height)`:
      - fraction < 0.45 → `"legs"`;
      - < 0.8 → `"body"`;
      - otherwise `"head"`.
    - `BattleMath.weight(double amount, double maxHealth, double medium, double heavy) -> int` (0 light, 1 medium, 2 heavy).

    Implement until fast PASSes.
  - [ ] **Step 3:** Implement the sends and receivers. Run `holdline` and expect `[hl-events] PASS`.
  - [ ] **Step 4:** Commit:

    ```
    Send attack, hit and stance events to clients
    ```

### Task 7: Corpses lie, then are removed

**Files:**

- Create: `src/main/java/gg/gravesandglory/mixin/CorpseMixin.java` (registered in the mixins json). It targets `LivingEntity.updatePostDeath` for troop mobs only and removes at `20 + corpseLieTicks + corpseFadeTicks`.
- Modify:
  - `BattleMath.java`
  - `GravesAndGlory.java` (corpse sweep on round end, map switch and stop)
  - every battle tally that iterates mobs: assert they already test `isAlive()`, and fix any that don't
- Test: `BattleMathCheck.java`, `HoldLineChecks.java`

**Interfaces:**

- Produces: `BattleMath.corpseAlpha(int deathTicks, int lie, int fade) -> float`: 1 up to `20 + lie`, then eased (`0.5 + 0.5*cos`) down to 0 at `20 + lie + fade`. The client fade (Task 14) consumes it.
- Steps:
  - [ ] **Step 1:** Add the failing checks: `corpseAlpha(10,60,40)==1`, `corpseAlpha(80,60,40)==1`, `corpseAlpha(120,60,40)==0`, and strictly falling between 80 and 120. Implement until it PASSes.
  - [ ] **Step 2:** Add `hl-corpse`:
    - A killed knight is still in the world at tick 79 and gone at tick 121.
    - It drops nothing, gives no XP and fires AFTER_DEATH once.
    - No troop targets it, and it does not block movement.
    - Squad counts and objectives treat it as dead.
    - Rise Again still works.
    - A round reset removes it at once.
  - [ ] **Step 3:** Add `hl-corpse-load`: a 100-v-100 wave, every unit dying within 10 s, compared with a run where the corpse ticks are 0. It passes if mean server tick time is within +10% and client frame time within +15%. Log the numbers to the report.
  - [ ] **Step 4:** Run, PASS, commit:

    ```
    Leave dead troops lying before they fade
    ```

### Task 8: Phase 1 gate

- [ ] **Step 1:** Run `python tools/verify.py game --offline`. Expect a PASS. If Kort's client is running, defer; never stop it.
- [ ] **Step 2:** Run the `holdline` suite in full and save the evidence under `work/verify/<id>/`.
- [ ] **Step 3:** Run one fresh review of Phase 1: an Opus 5.5 high-effort subagent with the spec §7 touchpoint lists. Fix its findings; at most two rounds.
- [ ] **Step 4:** Update `docs/DECISIONS.md` (the formation removal, impact-frame melee, the crawl, arcs, corpses) and `docs/HANDOFF.md`. Commit.

---

## Phase 2: Rig and motion engine (after Task 0)

### Task 9: The jointed rig

**Files:**

- Modify:
  - `art/rigs/biped.json`:
    - add `right_forearm`, `left_forearm`, `right_shin` and `left_shin` per the contract;
    - reparent the hands;
    - split each limb box into upper and lower 4×6×4 boxes, with per-face UVs that map the halves of the vanilla 64×64 limb layout (top face on the upper box, bottom face on the lower);
    - include the forearms in `arms_folded`.
  - Freebuff's rig review (2026-10-29), in the Gilded builders under `art/hand/**`:
    - **Joint caps:** the split creates an upper-box bottom face and a lower-box top face at the joint. Every arm, sleeve and leg painter (and handlib's `K.arm`/`K.leg`) paints them in the skin, cloth or armour colour of that row, so a bent joint shows a solid cap instead of a hole. Recheck the veteran 20% cap with the new denominator.
    - **Shield:** reparent from `left_arm` at `SHIELD_PIVOT (2.7, 10, -1.5)` to `left_forearm` at `(1.7, 6, -1.5)` for knight, sentinel and gl_marshal. `SHIELD_ROT = Rx(45)*Ry(-60)` stays until Task 16 re-derives it from the new carry.
    - **Knee cops** (knight, sentinel, gl_marshal, longbow s3; z -3..-1): move from y6–8 to start at y5 on the shin so the lip covers the seam. Couters move to the forearm unchanged.
    - **Back flaps:** sentinel skirts (both tiers, y0–8 at z 2.45) shorten the back flap to end at y7; longbow `jack` flaps move from z ±2.2 to ±2.3.
    - **Bare seams** (stages with no couter or cop): the joint caps cover the wedge; no lower-box overlap, so the 4×6×4 contract stands. `mo-rig`'s 90° bend images are the evidence; Task 16 grades them.
  - `tools/build_skins.py` and `tools/bbmodel.py`: carry the new bones and per-face UVs.
  - `TroopModel.java`: build the new parts; the folded-arms swap includes the forearms.
  - `TroopFeatures.java`: items follow the hand bones' full chain.
- Test:
  - `test-tools/standalone/test_build_skins.py`
  - `test_bbmodel.py`
  - new harness check `mo-rig` in `test-tools/java/gg/gravesandglory/MotionChecks.java` (suite `motion`)

**Interfaces:**

- Produces:
  - Rig bone names: `root, body, head, right_arm, right_forearm, right_hand, left_arm, left_forearm, left_hand, arms_folded, right_leg, right_shin, left_leg, left_shin`.
  - `TroopModel.part(String bone) -> ModelPart`
- Steps:
  - [ ] **Step 1:** Write the failing tests:
    - `test_biped_has_joints`: names, parents, pivots exactly as in Global Constraints.
    - `test_split_limb_uv_covers_vanilla_layout`: the union of upper and lower face UVs equals the original box UV rects.
    - `test_hand_world_position_unchanged_at_rest`: the hand's rest world point equals the old `[-1, 8, 0]` in arm space (and `[1, 8, 0]` on the left).
  - In `mo-rig`, a sword and a bow held at rest render with a pixel diff of at most 0.5% against today's, so no grip moved.
  - [ ] **Step 2:** Run `python -m unittest discover -s test-tools/standalone -p test_build_skins.py` and confirm it FAILs. Implement. Confirm it PASSes.
  - [ ] **Step 3:** Rebuild the skins with `python tools/build_skins.py`. `mo-rig`: every modelled troop renders at rest with a pixel diff of at most 0.5% against the pre-change turnaround. Bending the elbow 90° and the knee 90° shows no gap, because the cop covers it. Capture `mo-rig-*.png`.
  - [ ] **Step 4:** Commit:

    ```
    Give the biped rig elbows and knees
    ```

### Task 10: `MotionMath`, the pure motion maths

**Files:**

- Create: `src/main/java/gg/gravesandglory/MotionMath.java` (no Minecraft imports; add it to the `names` list at tools/verify.py:67, as Tasks 11–12 do for `Clip`, `MotionEngine`, `MotionInput` and `Pose`)
- Test: `test-tools/standalone/MotionMathCheck.java`

**Interfaces:**

- Produces:
  - `static double ease(double x)`: `0.5 - 0.5*cos(PI*clamp01(x))`
  - `static double gait(double phase)`: `sin(phase + sin(2*phase)/3)`
  - `static double tri(double phase)`: a triangle wave in [-1, 1], period 2π
  - `static double approach(double current, double target, double ratePerSecond, double dt)`: frame-rate-independent exponential approach, with dt clamped to [0, 0.1]
  - `record Spring(double pos, double vel)`; `static Spring spring(Spring s, double target, double stiffness, double damping, double dt)`: semi-implicit, dt clamped, sub-stepped at ≤ 1/120 s
  - `record Seed(double phase, double tempo, double amp, double breath, int idleLeg, double noveltyOffset, long variantBias)`; `static Seed seed(long uuidMost, long uuidLeast)`: phase in [0, 2π), tempo in [0.9, 1.1], amp in [0.9, 1.1]
- Steps:
  - [ ] **Step 1:** Write the failing checks:
    - `ease(0)==0`, `ease(1)==1`, `ease(.5)==.5`;
    - `gait` stays in [-1, 1] and crests early (reaches 0.95 before π/3, at least 0.25 rad before plain sine; its true peak is π/2), giving a snappy footfall;
    - `approach` reaches the same value at 20 fps and 240 fps after 1 s, within 1e-3;
    - `spring` is stable at dt 0.1 and settles to its target within 2 s;
    - `seed` gives the same values for the same UUID;
    - over 1000 random UUIDs, phases are spread so that no 0.1-rad bin holds more than 3%, and the tempo range is respected.
  - [ ] **Step 2:** Run fast and confirm it FAILs. Implement. Run fast and confirm it PASSes. Commit:

    ```
    Add the motion engine's pure maths
    ```

### Task 11: The clip format, converter and loader

**Files:**

- Create:
  - `tools/clips.py`: converts the Blockbench animations in `art/clips/<rig>/<family>.bbmodel` to `src/main/resources/assets/gravesandglory/clips/<rig>/<family>/<clip>.json`. It has a `--verify` mode, added to `verify.py fast` after `skin-assets`.
  - `src/main/java/gg/gravesandglory/Clip.java`: pure. Holds the keys per bone channel with Blockbench interpolation (`linear`, `catmullrom`, `bezier` with handles, `step`), plus `length`, `loop`, `impact` (ticks) and `markers`.
  - `src/main/java/gg/gravesandglory/ClipLibrary.java`: client; loads by rig and family.
- Test:
  - `test-tools/standalone/test_clips.py`
  - `MotionMathCheck.java` (Clip sampling)

**Interfaces:**

- Produces:
  - `Clip.sample(double seconds, Map<String,double[]> out)` writes 6 channels per bone, in degrees and pixels.
  - `Clip.reach() -> double[]` gives the max extents for culling.
  - `ClipLibrary.get(String rig, String family, String clip) -> Clip`
  - `ClipLibrary.variants(String family) -> List<Clip>`, ordered by the sheet's `attackVariants` order.
- Steps:
  - [ ] **Step 1:** Write the failing `test_clips.py` tests:
    - bezier handles survive the round trip;
    - an unknown bone name fails;
    - a melee clip's `impact` marker must equal its sheet row's `impactTicks` (else `--verify` fails, naming both);
    - a death clip's last frame has no bone point below ground (y < 0) by more than 0.1 px;
    - looping clips' first and last frames match within 0.01.
  - [ ] **Step 2:** In `MotionMathCheck`, check that `Clip` sampling of a bezier key pair matches Blockbench's curve at 5 points to 1e-3 (a reference fixture exported from Blockbench and saved in `test-tools/standalone/fixtures/clip_bezier.json`).
  - [ ] **Step 3:** Implement until both PASS. Commit:

    ```
    Load hand-keyed clips from Blockbench
    ```

### Task 12: `MotionEngine`, the layers

**Files:**

- Create:
  - `src/main/java/gg/gravesandglory/MotionEngine.java` (pure)
  - `MotionInput.java` (record, pure)
  - `Pose.java` (pure; bone → 6 doubles)
- Test: `MotionMathCheck.java`

**Interfaces:**

- Consumes: `MotionMath`, `Clip`
- Produces:
  - `record MotionInput.Attack(int variant, long startTick)` and `record MotionInput.Hit(String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal, long tick)`
  - `record MotionInput(double time, double limbAngle, double limbSpeed, double headYaw, double headPitch, double bodyYawRate, String stance, float aimPitch, boolean shieldUp, boolean enemyNear, boolean dead, double deathSeconds, Attack attack, Hit hit, Seed seed)`
  - `MotionEngine.State`: per entity; holds weights, springs, the active action and the novelty timer.
  - `MotionEngine.pose(MotionInput in, State st, Family fam, Pose out)`: layers in §6.1 order:
    1. locomotion
    2. stance
    3. life
    4. action
    5. look
    6. secondary
  - `Family`: stance clips, carry and guard, attack variants, hit set and death set, resolved from `ClipLibrary`.
- Steps:
  - [ ] **Step 1:** Write the failing checks:
    - **No pops:** stepping any state change (stance swap, attack start, hit, death) at 60 fps never moves any channel more than 12° or 1.5 px in one frame, except inside an authored clip whose own keys do.
    - **No clones:** two inputs that differ only in seed differ, over a 4 s idle, by an RMS of at least 2° on the body and arms.
    - **Feet planted:** standing idle with speed 0, foot points move less than 0.05 px.
    - **Stable:** 10 minutes of simulated input at random dt in [0.004, 0.1] stays finite and bounded.
    - **Hit zones:** a head hit's peak rotation is largest on the head and neck chain, a legs hit's on the shins.
  - [ ] **Step 2:** Run fast and confirm it FAILs. Implement the layers with the §6.1 behaviours. Run fast and confirm it PASSes.
  - [ ] **Step 3:** Commit:

    ```
    Build the layered motion engine
    ```

### Task 13: Wire the engine in; remove the old animator

**Files:**

- Create: `src/main/java/gg/gravesandglory/TroopMotion.java` (client adapter: entity → `MotionInput`; `MotionEvents.Client` → attack and hit; `State` per entity id, forgotten on unload)
- Modify:
  - `TroopModel.java` (setAngles → `TroopMotion.pose`; cullBox from `Clip.reach`)
  - `TroopFeatures.java` (bow pull from draw-clip progress)
  - `TroopRenderer.java` (folds and helmOff from engine state)
  - `TroopTells.java` (read engine state)
  - `GravesAndGloryClient.java`
- Delete: `TroopAnimator`, `TroopClips`, `BipedClips`, `HoundClips`, `SpiderClips`, `ColossusClips`, `HouseAttacks`
- Test:
  - `MotionChecks.java`
  - Remove the old p2-anim-* checks (4946-5419) from `Phase2Checks`.
  - Port the turn suite (5444-5812) to the new hooks: `TroopMotion.force(int id, String clip, double t)` and `TroopMotion.simulate(...)`.
- Steps:
  - [ ] **Step 1:** Write the `MotionChecks` checks:
    - **`mo-late-join`:** a second client joining mid-swing shows the swing at the matching clip time, ±2 ticks.
    - **`mo-crowd`:** 20 knights idle on Hold. Sampled over 4 s, no two are within 1° RMS of each other. Capture `mo-crowd.png` at 30 blocks.
    - **`mo-cull`:** every clip's posed bounds lie inside cullBox.
  - [ ] **Step 2:** Implement and delete the old classes. Expect no matches outside `docs/`:

    ```bash
    rg -n "TroopAnimator|HouseAttacks|BipedClips" src test-tools
    ```

  - [ ] **Step 3:** Run `verify.py fast` and the `motion` suite. Expect both PASS. (`verify.py game` runs only the Phase 1 combat smoke, so removing p2-anim-* changes no `verify` gate.) Commit:

    ```
    Drive troops with the motion engine
    ```

### Task 14: Corpse fade rendering

**Files:**

- Modify: `TroopRenderer.java` (a translucent layer while `corpseAlpha < 1`; the alpha comes from `BattleMath.corpseAlpha`)
- Test: `MotionChecks.java`

**Steps:**

- [ ] **Step 1:** Write `mo-fade`:
  - Mean model alpha sampled at deathTicks 70, 100 and 115 falls strictly.
  - Armour layers, weapon and shield fade together.
  - Nothing renders after removal.
  - Capture a contact sheet.
- [ ] **Step 2:** Implement, PASS, commit:

  ```
  Fade corpses out
  ```

### Task 15: The grading harness

**Files:**

- Modify: `MotionChecks.java` and `tools/` (a contact-sheet script, if it doesn't exist)

**Interfaces:**

- Produces:
  - `GRAVES_PHASE2_CHECKS=motion-grade` with `GRAVES_MOTION_FAMILY=<family>` renders, for every clip and state of that family:
    - a contact sheet at 8 fixed frames, front and side;
    - a turnaround;
    - the 20-unit crowd at 30 blocks;
    - a 6 s capture.

    Output goes to `work/grade/<family>/<timestamp>/`.
  - `docs/reports/grading/<family>.md` template: the rubric lines from §10, with a grade and observable findings per line.
- Steps:
  - [ ] **Step 1:** Implement and run it on the stub family. Expect all files present and a template filled with placeholders.
  - [ ] **Step 2:** Commit:

    ```
    Render grading evidence per family
    ```

---

## Phase 3: The bar (sword and shield)

### Task 16: The sword-and-shield family on the knight, to 10

Deliverable: every sword-and-shield state and clip on the knight model (Freebuff's builder, split at the joints):

- march carry
- ready guard
- wall
- wall-strike for each of the 3 variants
- braced shield shuffle
- walk and run
- hits for 3 zones × 4 directions × 3 weights, with blocked variants
- deaths:
  - stab: knees, then faceplant;
  - slash: 4 directions;
  - blunt;
  - arrow;
  - magic
- novelty idles

**Files:**

- Create: `art/clips/biped/sword_shield.bbmodel` and `art/clips/biped/common.bbmodel` (hits and deaths shared by every biped family; families override where their gear changes them)
- Modify:
  - the `attackVariants` impactTicks to match the keyed clips
  - the knight shield carry angles: `SHIELD_ROT` in `TroopFeatures` and the Gilded shield placement are re-derived here. `Rx(45)*Ry(-60)` exactly undoes the old 45° shoulder carry; if the new carry splits that angle between shoulder and elbow, recompute it so the board doesn't tilt, then re-shoot the knight's shield in hold and idle.

**Steps:**

- [ ] **Step 1:** Assemble the reference board (§10) into `docs/visual-references/animation/`: Kort's four hold references, FA captures and HEMA/shield-wall footage stills.
- [ ] **Step 2:** Key the clips in Blockbench through the MCP bridge. Convert with `python tools/clips.py`. Run `--verify`.
- [ ] **Step 3:** Run `motion-grade` for `sword_shield`. A fresh Opus 5.5 high-effort reviewer grades it with the evidence and the reference board; Freebuff gives a second opinion on the knight's fit and silhouette.
- [ ] **Step 4:** Make up to two correction rounds on the observable findings.
- [ ] **Step 5:** Show Kort the evidence and the honest grades. This family sets the bar for the rest. Don't start Phase 5 families until Kort accepts it or gives direction.
- [ ] **Step 6:** Commit:

  ```
  Animate the sword and shield family
  ```

---

## Phase 4: Troop models (all 30 troops, by house)

Each house task delivers, per troop:

- `art/hand/<tid>/build_<tid>.py` on handlib;
- the model JSON;
- stage skins s1–s3 and veteran;
- faces where the troop has them;
- its weapons in tiers I–III and its shield;
- in-game turnaround and battle-distance shots.

All on the jointed rig, following every art rule in Global Constraints.

ic_hearthguard, wm_tusk and tc_tideguard are modelled from scratch here. Their versions on `work/troop-models-house` are superseded (Kort, 2026-10-29) and are not a starting point.

Each house runs this cycle:

1. Concept silhouettes (front and side, all 3 tiers).
2. Kort picks a direction before modelling. The questions go in one message per house.
3. Build.
4. `python tools/build_skins.py --verify` and `verify.py fast`.
5. A fresh Opus 5.5 high-effort reviewer grades the house against Kort's references, and Freebuff reviews it as a consultant.
6. At most two correction rounds.
7. Kort accepts.

| Task | House | Troops | Notes |
|---|---|---|---|
| 16b | Gilded (finish) | knight, archer, sentinel, longbow, mage, gl_marshal | Freebuff's committed builders are installed and approved in game, except two items Kort stopped mid-flight on 2026-10-29: the sun_staff tiers (finish `art/hand/weapons/build_sun_staff.py` from whatever the helper left, then install it on the mage) and the in-game check of the order_bow and longbow tiers and the Longbowman. This task finishes both. State at the stop (Freebuff, 2026-10-29): `art/hand/weapons/build_sun_staff.py` is untracked, 308 lines, parses, but was killed mid-edit, so treat it as partial and unverified; it was never installed and the sun_staff assets are untouched. In-game evidence in the main checkout's `work/art-units/`: `lb-s1` complete (40 shots and log), `lb-s3` 36 shots but its client exited early, `ar-s1`/`ar-s3` none. Re-shoot `lb-s3`, `ar-s1` and `ar-s3`. This task then splits all six at the joints and re-shoots archer and longbow `draw-0`/`loose-0`, because the forearm bend moves the bow grip. Freebuff reviews against its own intent. |
| 17 | Graveborn | revenant, bonebow, reaper, stalker, hexer, gb_hollowking | skeleton and wither-skeleton proportions: thin limbs, tall |
| 18 | Wildmoot humanoids | wm_hunter, wm_tusk, wm_seer | piglin proportions; tusk spear |
| 19 | Wildmoot beasts | wm_hound, wm_stalker, wm_oldhorn | rebuild the jointed hound and spider rigs (`art/rigs/hound.json`, `spider.json`); a new `art/rigs/bear.json` for wm_oldhorn (antlered alpha). Each new rig follows Task 9's test pattern. |
| 20 | Ironclad humanoids | ic_hearthguard, ic_arbalist, ic_breaker, ic_ballista, ic_mortar | illager proportions; ballista and mortar pieces are crew props |
| 21 | Ironclad colossus | ic_colossus | rebuild the jointed `colossus.json` |
| 22 | Tidecallers humanoids | tc_raider, tc_harpooner, tc_tideguard, tc_mist, tc_admiral | drowned and evoker proportions |
| 23 | Tidecallers brute | tc_brute | a new `art/rigs/ravager.json` |

Each new or rebuilt rig adds an engine adapter: its locomotion layer and family in `MotionEngine`. A new rig's tests copy Task 9 (names, parents, rest pose unchanged, joint coverage) and Task 12 (no pops, no clones, feet planted).

---

## Phase 5: Animation families (each graded to 10 like Task 16)

| Task | Families | Clips beyond the shared biped set |
|---|---|---|
| 24 | `axe_shield`, `sword`, `axe`, `greatsword` | guards and attack variants per §6.2 |
| 25 | `spear`, `scythe` | levelled brace, the reap |
| 26 | `bow`, `crossbow`, `thrower` | lowered rest, volley draw toward the sky following `aimPitch`, direct draw, loose, crossbow reload, throw |
| 27 | `caster`, `crew` | a cast per caster (mage, hexer, wm_seer, tc_mist); mortar load, fire, recoil |
| 28 | `hound`, `spider`, `bear`, `ravager`, `colossus` | rig-specific carry, gait, attacks, hits and deaths (§5) |

Each task runs the cycle from Task 16, Steps 2–6. A family's grading uses its house's final models from Phase 4. The two phases may interleave house by house.

---

## Phase 6: Whole-game acceptance

### Task 29: Battle review and gates

- [ ] **Step 1:** Run a 60-v-60 battle with every house in an isolated root. Capture a crowd video and stills at 10, 30 and 60 blocks. A fresh reviewer grades the whole against the §3 goals and anti-goals.
- [ ] **Step 2:** Run `hl-corpse-load` again with every final model.
- [ ] **Step 3:** Final per-unit review (Kort, 2026-10-29): for each of the 30 troops, a fresh Sonnet 5.5 subagent at high effort (`model: sonnet`, `effort: high`), which has built and graded nothing in this effort, reviews the unit's model and every one of its clips against the §3 goals, its house reference and the motion-grade evidence. A unit is done only when that reviewer passes it; any finding goes back to an Opus 5.5 high implementer and the same unit is reviewed again by a new Sonnet reviewer.
- [ ] **Step 4:** Run `python tools/verify.py game --offline` and `release --offline`. Expect both PASS.
- [ ] **Step 5:** Update DECISIONS, HANDOFF, BACKLOG, ART-STYLE-GUIDE (the jointed-rig rules) and the media-shoot note (the unit models are final). Hand the evidence and grades to Kort. Nothing is pushed or merged.
