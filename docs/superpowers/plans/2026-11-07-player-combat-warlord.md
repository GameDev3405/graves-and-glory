# Player Combat ("Warlord") Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A player who fights well can beat a squad of 8 footmen alone. Formations, elites and enemy warlords stay deadly, gear stays fair, a downed warlord leaves a visibly leaderless army, and bot armies field a warlord of their own.

**Architecture:** One new pure module (`WarlordMath`) holds every rule as testable arithmetic. One new server module (`Warlords`) hooks those rules into the existing damage paths: `Battle.allowDamage` for blows a warlord strikes, and `Combat.resolve` for blows a troop strikes at a warlord. Every number lives in a new one-row `warlord` sheet plus new `botCommand` columns, read through generated `Spec`. Phase 5 adds a player-shaped `WarlordEntity` whose entity UUID is the bot squad's `owner`, so `teamOf`, `allowDamage`, `respawns` and `OrderRules` treat it as a commander without special cases.

**Tech Stack:** Java 17, Fabric 1.20.1 (Loader 0.16.14, API 0.92.2+1.20.1, Yarn build.10), Mixin, `sheets/design.json` with `tools/preflight.py` (which generates `Spec.java`), standalone `*Check.java`, in-game `Phase1Checks.Suite`, `DuelSimulation` / `BotSimulation` through `tools/balance_sim.sh`.

**Spec:** `docs/superpowers/specs/2026-11-07-player-combat-design.md` (approved by Kort 2026-11-07). Base: `main` at `cbbd3641`.

**Kort's decisions (2026-11-07):**
- **G3, port timing:** build Phases 1 to 4 now, before the 1.21.1 port. Phase 5 (the bot warlord body) comes after the port.
- **G0:** approved by that same answer. Phases 1 to 4 may change gameplay as this plan says.
- **Pass thresholds** (Kort: "use them"):
  - the warlord wins at least 14 of 20 fights against 8 footmen;
  - it survives at least 6 s standing still among them;
  - it loses at least 16 of 20 lone charges into braced spears;
  - a newcomer beats a veteran 30 to 50% of the time;
  - warlords make at most 35% of troop kills in bot sims.
- G1 tuning sign-offs and playtests are still Kort's.
- **Orchestrator calls on the open questions:**
  - the gear cap's damage includes the ranged stat;
  - the leaderless action-bar note goes to the downed player only;
  - greatswords and greataxes keep their own sweep instead of also cleaving.

## Global Constraints

- Follow spec sections 5, 7 and 8 exactly. Non-goals (section 4): no parry, dodge, combo, morale, routing, enemy markers or glow, no bonus damage against players, and no relic changes.
- Every number is a `sheets/design.json` row read through `Spec`. Never hand-edit `Spec.java`; regenerate it with `python tools/preflight.py` and save the before and after hashes (WORKFLOW).
- Player-against-player damage stays at gear numbers. The troop damage factor and the troop-hit reduction act only on blows between a warlord and a troop.
- Plain (no-mod) client: everything except hit-stop is server-side (attributes, vanilla particles, sounds, the action bar). Hit-stop is client-only and adds no packet.
- The 12 s respawn (`rules.respawnSeconds`) stays as it is. `FeedEvents.commander` stays as it is, and enemies get no new message.
- Cleave and stagger happen only on a fully charged melee swing (vanilla attack cooldown at least `warlord.fullCharge` = 0.9). Up to `warlord.cleaveTargets` = 2 extra targets. Only the main target triggers on-hit perks, lifesteal and relic triggers.
- Elite means troop rarity `epic` and above (`warlord.eliteRarity`). Formation means the existing `brace` and `wall` stances (`BattleMath.STANCES`).
- Battle-only modifiers use fixed UUIDs, so they never stack, and `GearCombat.clear` removes them all.
- Verification: `python tools/verify.py fast --offline` after every task, and `python tools/verify.py game --offline` at the end of each phase (SETUP.md). There are new standalone checks, so add `WarlordMath` to the pure-source `names` list in `tools/verify.py`.
- Routing (WORKFLOW 2026-11-07): Opus 5.5 at medium effort for systems, AI, UI and animation-feel tasks. Sonnet 5.5 at high effort for routine work: sheet rows, preflight validation, gate scripts, checks, reviews. Never Fable. Use xhigh only when a task explicitly needs it. One writer per checkout.
- No push, no merge, no publishing. Each phase ends on a branch for Kort's review.

## Approval gates

| Gate | When | What Kort decides |
|---|---|---|
| **G0 gameplay (plan)** | before Task 1.1 | Approves this plan: damage, health and AI changes are gameplay changes (spec 6). |
| **G1 numbers** | end of Phase 2, again at end of Phase 3 | Approves the tuned `warlord` row from the sim report plus his playtest. Number changes after that need him. |
| **G2 protocol / registry** | before Task 5.1 | A new entity type (`gravesandglory:warlord`) is a registry-synced addition, so modded clients must match. Bump `CompatibilityPolicy` and the release identity. No other new packet fields in this plan: hit-stop is client-predicted, and the leaderless HUD reads `state`'s existing respawn seconds. |
| **G3 port timing** | when the plan is accepted | Build before or after the 1.21.1 port (see "Port timing"). |
| Already approved | (none) | hit_v1 flag bit 4 and the `stunned` stance (Kort 2026-11-07, `anim/v2-stances` 7b66a8ff). Task 1.6 only uses them. |

No save or profile format changes: every modifier is battle-only.

## Review Focus

1. **Re-entrant sub-hits.** Vanilla sword sweep blows, a family's sweep blows and the new cleave blows all come back through `Battle.allowDamage` as the player's own. A swing into 6 troops must damage at most 1 + 2 cleave targets (or the family's sweep set). No sub-hit may cleave, sweep, stagger, feed Bloodrush, set grudge or fire relic triggers again. Pinned in Task 1.3 (`cleaveDoesNotChain`, `procsMainOnly`).
2. **The charge is read after vanilla resets it.** `PlayerEntity.attack` resets the cooldown before `target.damage`, so `allowDamage` always reads 0. Capture the charge at the head of `attack`. Then a spam click (charge below 0.9) must never cleave or stagger, and a charged click always may. Pinned in Task 1.2 (`spamClickNoCleave`).
3. **Health modifier lifecycle.** Respawn, a round ending, the lobby, a takeover and a reconnect must never stack the warlord health modifier or leave a player at 60 health outside battle. Pinned in Task 2.1 (`healthNeverStacks`, `lobbyClearsWarlordHealth`).
4. **Reduction applied where it must not be.** Fall, lava, a player's blow and an ally's blow must not be reduced. Only a troop's `Combat.resolve` blow on a warlord target is reduced. Pinned in Task 2.2 (`pvpUnreduced`, `environmentUnreduced`).
5. **Seats that change hands.** A human who drops mid-battle becomes a bot seat (`Battle.leave`). In Phase 5 that seat gets a bot body only after `takeoverAt`, and the body goes away when the human reclaims the seat. A body is never left behind, and a reclaimed seat never has two commanders. Pinned in Task 5.3 (`takeoverBodyLifecycle`).

---

## File map

| File | Status | Responsibility |
|---|---|---|
| `src/main/java/gg/gravesandglory/WarlordMath.java` | create (pure) | cleave pick and share, stagger ticks with diminishing returns, troop-hit factor, gear band cap, leaderless line text |
| `src/main/java/gg/gravesandglory/Warlords.java` | create (server) | glue: charge capture, cleave, stagger, feedback, troop-hit reduction context, volley log, band, leaderless action bar, `bodies(b)` |
| `src/main/java/gg/gravesandglory/mixin/PlayerAttackChargeMixin.java` | create | HEAD of `PlayerEntity.attack`: record charge and primary target |
| `src/main/java/gg/gravesandglory/mixin/HitStopMixin.java` | create (client) | brief hit-stop on the local player's fully charged hit |
| `src/main/java/gg/gravesandglory/WarlordEntity.java`, `WarlordRenderer.java`, `WarlordBrain.java` | create (Phase 5) | bot warlord body, client look, target choice per `botCommand` |
| `GravesAndGlory.java` | modify | `allowDamage` player branch delegates to `Warlords.strike`; `allowCommanderDeath` generalised to `LivingEntity`; respawn loop for bot bodies |
| `Combat.java` | modify | `resolve` multiplies `Warlords.troopHitFactor` when the target is a warlord |
| `GearCombat.java`, `GearStats.java` | modify | warlord health modifier; band-capped loadout; `set` takes `LivingEntity` |
| `Tactics.java`, `Abilities.java`, `GearCombat.foesNear`, `FeedEvents.java`, `Veterans.java` | modify (Phase 5) | iterate `Warlords.bodies(b)` instead of only the player list |
| `Campaign.java` | modify (Phase 5) | bot gets a body, band kit; `botDecide` does nothing while its body is down |
| `BattleHud.java`, `FeedLines.java` | modify | leaderless line on the respawn card |
| `sheets/design.json`, `tools/preflight.py`, `test-tools/standalone/test_preflight_rules.py` | modify | `warlord` sheet, `botCommand` columns, validation |
| `test-tools/standalone/WarlordMathCheck.java` | create | pure checks (fast tier) |
| `test-tools/java/gg/gravesandglory/WarlordChecks.java` | create | in-game suite registered in `Phase1Checks.SUITES` (game tier) |
| `test-tools/java/gg/gravesandglory/WarlordPilot.java`, `DuelSimulation.java` | create / modify | scripted warlord for sims |
| `test-tools/resources/duel/warlord-*.txt`, `tools/warlord_gate.py`, `test-tools/standalone/test_warlord_gate.py` | create | sim plans and pass-criteria gate |
| `tools/verify.py` | modify | add `WarlordMath` to the standalone pure sources |

## The `warlord` sheet (one row, id `default`)

All starting values are placeholders for tuning, as section 7 asks. Tasks 2.4 and 3.3 replace them with the measured values.

| Column | Start | Added by | Meaning |
|---|---|---|---|
| fullCharge | 0.9 | 1.1 | attack cooldown share that counts as fully charged |
| troopDamageFactor | 1.0 | 1.1 | × on a warlord's blow to a troop (on top of `combatDiscipline.damageScale` 0.6) |
| cleaveTargets | 2 | 1.1 | extra targets at most |
| cleaveRange | 3.0 | 1.1 | blocks from the warlord |
| cleaveArcDegrees | 100.0 | 1.1 | full arc in front of the warlord's facing |
| cleaveShare | 0.5 | 1.1 | share of the blow each extra target takes |
| staggerTicks | 12 | 1.1 | stun on the first stagger |
| staggerKnockback | 0.6 | 1.1 | knockback strength (`Abilities.knock`), elites and warlords too |
| staggerWindowTicks | 80 | 1.1 | staggers within this window count toward diminishing returns |
| staggerDecay | 0.5 | 1.1 | × per earlier stagger in the window |
| staggerMinTicks | 3 | 1.1 | below this the stagger is 0 (immune until the window clears) |
| eliteRarity | "epic" | 1.1 | lowest troop rarity that counts as elite |
| particlesPerDamage | 0.6 | 1.4 | crit particles per point dealt |
| particlesMax | 24 | 1.4 | cap |
| hitStopMs | 60 | 1.5 | client hit-stop length; 0 turns it off |
| baseHealth | 60.0 | 2.1 | warlord max health before gear (vanilla 20) |
| troopHitFactor | 0.35 | 2.2 | × on a troop blow to a warlord in the open |
| formationHitFactor | 0.9 | 2.2 | the same, from a `brace`/`wall` troop with the warlord in its front arc (`BattleMath.FRONT_ARC`) |
| volleyHits | 4 | 2.2 | ranged troop hits on one warlord... |
| volleyWindowTicks | 30 | 2.2 | ...within this window make a focused volley |
| volleyHitFactor | 0.8 | 2.2 | factor for ranged hits while a volley is on |
| eliteHitFactor | 1.0 | 2.2 | factor for an elite troop's blow |
| bandSpread | 1.25 | 3.1 | a capped stat may reach band floor × this |

When more than one case applies to a blow, the largest factor wins (`WarlordMath.troopHitFactor`).

New `botCommand` columns (Task 5.1): `warlordStance` (easy `back`, normal `escort`, hard `hunt`), `warlordLeash` (12.0 / 20.0 / 60.0 blocks from the army centre), `warlordKitRarity` (`common` / `rare` / `epic`, the band reference kit the bot wears before the cap), `warlordFallBackShare` (0.5 / 0.35 / 0.2 health share at which it falls back to its army), `warlordHuntsPlayers` (false / false / true).

---

## Phase 1: Weighty hits (size L, Opus medium; checks Sonnet high)

Server-side throughout. Only Task 1.6 waits for `anim/v2-stances`.

### Task 1.1: `warlord` sheet and `WarlordMath` core (M)

**Files:** Modify `sheets/design.json` (new `warlord` sheet with the Phase 1 columns), `tools/preflight.py` (exactly one row; `0 < fullCharge <= 1`; `cleaveTargets` 0..4; `0 <= cleaveShare <= 1`; `0 < staggerDecay < 1`; `staggerMinTicks <= staggerTicks`; `eliteRarity` is a `rarities` id), `test-tools/standalone/test_preflight_rules.py`, `tools/verify.py` (`names` += `WarlordMath`). Create `WarlordMath.java` and `test-tools/standalone/WarlordMathCheck.java`.

**Interfaces (produces):**
- `static boolean fullCharge(float progress, double threshold)`
- `static <T> List<T> cleavePick(List<T> foes, ToDoubleFunction<T> distance, ToDoubleFunction<T> bearingDeg, double range, double arcDeg, int max)`: in range, `|bearing| <= arc/2`, nearest first, at most `max`.
- `static int staggerTicks(int earlierInWindow, int base, double decay, int minTicks)`: `round(base × decay^earlier)`, and 0 when that falls below `minTicks`.
- `static boolean elite(String rarity, String eliteRarity)`: by `Spec.RARITIES` order.

- [ ] **Step 1: Write failing checks** in `WarlordMathCheck`:
  - `fullCharge(0.9f,0.9)` is true and `fullCharge(0.89f,0.9)` is false.
  - `cleavePick` of 6 foes at distances 1..6, all at bearing 0, with range 3 and max 2 returns the foes at 1 and 2. A foe at bearing 70 with arc 100 is excluded.
  - `staggerTicks(0,12,.5,3)==12`, `(1,…)==6`, `(2,…)==3`, `(3,…)==0`.
  - `elite("epic","epic")`, `elite("legendary","epic")` and `!elite("rare","epic")`.
- [ ] **Step 2:** Run `python tools/verify.py fast --offline`. Expected: FAIL at `java-compile` (no `WarlordMath`).
- [ ] **Step 3:** Add the sheet and the preflight rules, regenerate `Spec.java` (record hashes), then implement `WarlordMath`.
- [ ] **Step 4:** Run `fast` again. Expected: `WarlordMathCheck: N checks passed`, and the preflight tests pass. A bad row (for example `cleaveTargets: 9`) fails preflight.
- [ ] **Step 5:** Commit `feat(warlord): warlord sheet and pure hit rules`.

### Task 1.2: Charge capture (S)

**Files:** Create `mixin/PlayerAttackChargeMixin.java` (common, add it to `gravesandglory.mixins.json`). Create `Warlords.java` with the capture state.

**Interfaces (produces):** `Warlords.Swing record(Entity primary, float charge, long tick)`, `static Swing swing(ServerPlayerEntity p)` (null when no swing is recorded this tick), `static boolean mainHit(ServerPlayerEntity p, Entity target, DamageSource src)`: the blow is the recorded primary, by hand (not a projectile), and not inside a sub-hit (`Warlords.subHit` flag).

- [ ] **Step 1: Write the failing in-game check** `WarlordChecks.spamClickNoCleave`, a new suite registered as `"warlord"` in `Phase1Checks.SUITES`. A seated FakePlayer attacks a troop with its cooldown at 0.3, then at 1.0. The recorded `charge` is below 0.9, then at least 0.9.
- [ ] **Step 2:** `game` fails (no suite).
- [ ] **Step 3:** Implement. `@Inject(at = HEAD)` on `PlayerEntity.attack(Entity)` stores `getAttackCooldownProgress(0.5f)` and the target before vanilla resets the cooldown.
- [ ] **Step 4:** `game` passes `warlord` and every existing suite.
- [ ] **Step 5:** Commit.

### Task 1.3: Cleave, troop damage factor, procs on the main target only (M)

**Files:** Modify `GravesAndGlory.java` (`allowDamage`'s player branch becomes one call to `Warlords.strike`) and `Warlords.java`. Change `GearCombat.onPlayerHit` so its family sweep runs only on `mainHit` and under the shared `subHit` flag (it replaces `GearCombat.sweeping`). Also change `Commander.feeds`/`lifesteal` and the grudge and relic triggers so they require `mainHit`.

**Interfaces:** `static boolean strike(Battle b, LivingEntity striker, Squad s, LivingEntity target, DamageSource src, float amount)` returns whether `allowDamage` lets the original blow through. Phase 5 calls it with a `WarlordEntity` striker. The scale is today's, with `× warlord.troopDamageFactor` when the target is a troop.

Decision (logged; section 8 leaves it open): a family that already sweeps (greatsword, greataxe: `sweepRadius > 0`) keeps its sweep and does not also cleave. Every other melee family cleaves. A swing never does both, so gear keeps its identity.

- [ ] **Step 1: Failing checks** in `WarlordChecks`:
  - `cleaveHitsTwo`: a charged sword swing into 6 knights in a row damages exactly 3. The two extras each lose `cleaveShare` of the main blow (±0.01).
  - `cleaveDoesNotChain`: across the same swing, `Combat.LAST` plus the damage log shows no blow from a sub-hit. Vanilla sweep blows count as sub-hits.
  - `procsMainOnly`: with Bloodrush running, lifesteal equals `value1 ×` the main target's loss only. The grudge is set once.
  - `pvpAtGearNumbers`: a charged swing at an enemy FakePlayer deals the same as before this task (`troopDamageFactor` not applied, no cleave onto players beyond the arc rule).
- [ ] **Step 2:** `game` fails on these.
- [ ] **Step 3:** Implement `strike`. It picks cleave targets with `WarlordMath.cleavePick` among `GearCombat.foe(...)` troops (never players: cleave hits troops only) and deals them inside `subHit`.
- [ ] **Step 4:** `game` passes. `fast` passes.
- [ ] **Step 5:** Commit.

### Task 1.4: Stagger, knockback, heavier feedback (M)

**Files:** `Warlords.java` (a per-troop stagger log, a `WeakHashMap<Unit, ArrayDeque<Long>>` cleared on `Battle.startRound`) and `Combat.java` (no change unless the shared `Audio` helper needs a key).

**Interfaces:** `static void stagger(Battle b, Unit u, Vec3d from)`: knockback through `Abilities.knock(..., warlord.staggerKnockback)` always, plus `Statuses.apply(b, null, mob, "stun", ticks, 0, 0)` when the troop is not elite and `ticks > 0`. A warlord target takes the knockback only. That covers players, and the `WarlordEntity` too, because `Statuses` skips non-Units. Feedback on every main hit: `AudioCues.Cue.HEAVY_METAL_HIT`, plus a kill cue when the target died. `ParticleTypes.CRIT` with count `min(particlesMax, round(dealt × particlesPerDamage))`. All server-side, so plain clients see and hear them.

- [ ] **Step 1: Failing checks:**
  - `staggerStopsAttacks`: a staggered knight lands no blow for `staggerTicks` (`Combat.landedNow` stays false).
  - `staggerDiminishes`: four charged hits within the window give stun lengths 12, 6, 3 and 0.
  - `eliteNotStaggered`: an epic troop is knocked back (its position moves at least 0.3) and its `stunnedUntil` does not change.
  - `anchoredStays`: a troop under the Oath Banner keeps its position.
  - `spamNoStagger`: an uncharged hit staggers nothing.
- [ ] **Step 2:** `game` fails.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** `game` passes.
- [ ] **Step 5:** Commit.

### Task 1.5: Client hit-stop, mod only (S, Opus medium)

**Files:** Create `mixin/HitStopMixin.java` (client, in the client mixin list). It hooks `ClientPlayerInteractionManager.attackEntity`. When the local cooldown is at least `fullCharge` and the target is a living entity other than a teammate, it freezes the local player's arm swing and camera bob, and the target's client motion, for `hitStopMs`. It sends no packet and slows no game tick.

- [ ] **Step 1:** Add a pure `WarlordMath.hitStopActive(long startedMs, long nowMs, int lengthMs)` with checks: true inside the window, false at `hitStopMs = 0`.
- [ ] **Step 2:** Implement the mixin.
- [ ] **Step 3: Visual evidence.** A recorded clip or a contact sheet of a charged hit against a spam click, compared at normal and small window sizes. Rubric 9 (player-facing).
- [ ] **Step 4:** Commit.

### Task 1.6: Stagger client look **[needs `anim/v2-stances` merged]** (S)

**Files:** `Warlords.stagger` pre-marks the troop (as `Abilities.splash` does on that branch), so `HitEventMixin`'s `MotionEvents.sendHit` sets flag bit 4 (knockback). The `stunned` stance needs no code here: `Tactics` already shows it while `stunnedUntil` runs.

- [ ] **Step 1:** Add `WarlordChecks.staggerSendsKnockFlag`: the hit_v1 captured for a staggered troop has `(flags & 4) != 0`, and its stance is `stunned` during the stun.
- [ ] **Step 2:** Implement, then run `game`.
- [ ] **Step 3: Visual evidence.** The `hit_knockback` and `stun_*` clips play on a staggered knight. The animation style reference applies.
- [ ] **Step 4:** Commit.

**Phase 1 pass:** `fast` and `game` are green, and Kort playtests the feel of charged hits against spam clicks. Hit-stop is the only thing a plain client misses.

## Phase 2: Warlord body (size L, Opus medium; harness and gate Sonnet high)

### Task 2.1: Warlord health (S)

**Files:** `GearCombat.java`: a new fixed-id modifier `WARLORD_HEALTH` (`+ baseHealth − 20`) in `apply` (cleared by `clear`); `set(...)` takes `LivingEntity`. Sheet: `baseHealth`.

- [ ] **Step 1: Failing checks:**
  - `warlordHealth`: a seated player's max health is `baseHealth + healthBonus`.
  - `healthNeverStacks`: `apply` twice, then a respawn, gives the same max health.
  - `lobbyClearsWarlordHealth`: after the round ends the max health is 20 and health is at most 20.
- [ ] **Step 2 to 4:** Run `game` (fail), implement, run `game` (pass).
- [ ] **Step 5:** Commit.

### Task 2.2: Troop-hit reduction with its exceptions (M)

**Files:** `WarlordMath.troopHitFactor(boolean formationFront, boolean volley, boolean elite, double open, double formation, double volleyF, double eliteF)` returns the largest factor that applies, else `open`. In `Warlords`: `isWarlord(Battle, LivingEntity)` (Phase 2: a seated player), `troopHitFactor(Battle, Unit attacker, LivingEntity target, String kind)` (builds the context: the attacker's `stance` is `brace` or `wall` and the target is within `BattleMath.FRONT_ARC` of the attacker's facing; a ranged kind with at least `volleyHits` ranged hits logged on this warlord within `volleyWindowTicks`, counting this one; elite by the attacker's spec rarity). `Combat.resolve` multiplies the amount by it, outside the clamp like `block`, when the target is a warlord. Sheet: the five Task 2.2 columns. `WarlordMathCheck` covers the largest-wins rule and each case alone.

- [ ] **Step 1: Failing checks:**
  - `openReduced`: a knight blow on a warlord deals `troopHitFactor ×` the unreduced amount.
  - `braceFront`: a braced `tc_tideguard` blow from the front uses `formationHitFactor`, and from behind the warlord it uses `troopHitFactor`.
  - `volleyFocus`: the 4th arrow within 30 ticks and later ones use `volleyHitFactor`.
  - `eliteFull`: an epic troop's blow uses `eliteHitFactor`.
  - `pvpUnreduced`: a player blow is not reduced.
  - `environmentUnreduced`: fall damage is not reduced.
- [ ] **Step 2 to 4:** Run `game` (fail), implement, run `game` (pass).
- [ ] **Step 5:** Commit.

### Task 2.3: Warlord duel harness and gate (M, Sonnet high)

**Files:** Create `test-tools/java/gg/gravesandglory/WarlordPilot.java`: a FakePlayer seated in `Battle.squads` for its side, moved by per-tick position steps at its speed attribute on the flat duel cell. Its behaviours:
- `pilot=skirmish`: approach the nearest foe, swing only at charge 1.0, and back off one step when 3 or more foes are within 2 blocks. This is the "plays well" stand-in.
- `pilot=charge`: run straight at the line centre and swing.
- `pilot=idle`: stand still, for the survival-time check.

Modify `DuelSimulation.java`: lineup token `warlord:<kit>`, where kit is `starter`, `band` (the median kit) or `veteran` (all-legendary armour set plus a legendary main hand, as listed in the plan file). Options `pilot=` and `hold=brace|wall` for the troop side. Write `duel/duels.csv` with the new columns `warlordHealthLeft`, `warlordKills` and `seconds`. Create the plans `test-tools/resources/duel/warlord-open.txt`, `warlord-spears.txt`, `warlord-fair.txt` and `warlord-survive.txt`. Create `tools/warlord_gate.py` (reads a run root, prints PASS or FAIL per criterion, exits nonzero on any FAIL) and `test-tools/standalone/test_warlord_gate.py` (synthetic CSVs).

Pass criteria (section 6; the thresholds are mine and Kort can change them at G1):

| Check | Plan | Pass |
|---|---|---|
| Open field | `warlord:band` skirmish vs `knight*8`, 20 fights | warlord wins at least 14 of 20 ("most attempts") |
| Not one second | `warlord:band` idle vs `knight*8` surrounding, 10 fights | median survival at least 6 s ("several seconds, not one") |
| Braced spears | `warlord:band` charge vs `tc_tideguard*8 hold=brace`, 20 fights | warlord loses at least 16 of 20 |
| Shield wall (informational) | the same vs `knight*8 hold=wall` | reported, no threshold |
| Fair share (Phase 3) | `warlord:starter` vs `warlord:veteran`, both skirmish, 20 fights | newcomer wins 6 to 10 of 20 (30 to 50%) |
| Mirror sanity | `warlord:band` vs `warlord:band` | 8 to 12 of 20 |

- [ ] **Step 1:** Write `test_warlord_gate.py`. A CSV with 14 of 20 open wins passes and one with 13 fails. Each criterion gets one passing case and one failing case.
- [ ] **Step 2:** `fast` fails, then implement the gate and the harness. `fast` passes and `compileLocalTestTools` builds.
- [ ] **Step 3:** Smoke run: `tools/balance_sim.sh wl-smoke 25690 -PduelSim=$PWD/test-tools/resources/duel/warlord-open.txt -PsimFast=true` finishes and writes the new columns.
- [ ] **Step 4:** Commit.

### Task 2.4: Tune against the 8-footmen yardstick (M, Opus medium)

Section 7 tuning order: the gear band comes first, but it lands in Phase 3. Tune here with the fixed `band` kit, then re-run in Task 3.3.

- [ ] **Step 1:** Measure today's gear totals (starter, median, veteran) with `GearStats` over the fixture profiles and record them in the report.
- [ ] **Step 2:** Sweep `baseHealth` and `troopHitFactor` (and `troopDamageFactor` if needed) with `-PsimOverride=warlord.default.<col>=<v>` until the open, survive and spears checks all pass in `warlord_gate.py`. Keep `troopHitFactor` a flat factor.
- [ ] **Step 3:** Write the values into the sheet, regenerate `Spec`, and run `fast`.
- [ ] **Step 4:** Report to `docs/superpowers/reports/2026-11-xx-warlord-tuning.md`: the table of runs, the chosen values and the gate output. Commit the sheet and the report. **Gate G1:** Kort approves the numbers and playtests "1 warlord vs a squad" before Phase 3.

## Phase 3: Fair fights, the gear cap (size M, Opus medium)

### Task 3.1: Band cap (M)

Decision (section 7, "prefer the simpler"): **flatten the top.** At round start (`Battle.startRound`, and for a late seat at `GearCombat.seat`), the band floor for each of health, damage, armour and ranged is the larger of the starter kit's total and the lowest seated human's total. Each stat is capped at floor × `bandSpread`. Perks, set bonuses, families and relics are untouched. Bots use the band (Phase 5). With no human seated, the floor is the starter kit.

**Interfaces:** `WarlordMath.bandCap(double[] lowest, double[] starter, double spread, int[] capped)` returns the cap per stat index (other indices `+∞`). `GearStats.capped(Loadout l, double[] cap)` returns a new `Loadout` with the totals clamped. `Battle.band` (a `double[]` set at round start). `GearCombat.seat` applies `capped`.

- [ ] **Step 1: Failing checks.**
  - In `WarlordMathCheck`: lowest `[10,4,6,0]`, starter `[6,2,4,0]`, spread 1.25 gives `[12.5,5,7.5,0]` for the capped stats. Speed, healing and command stay uncapped.
  - In `GearCheck`: `capped` keeps `perks`, `bonuses`, `mainFamily` and `relics` equal.
  - In-game `bandAtRoundStart`: a veteran and a newcomer seated together get the veteran's health capped.
- [ ] **Step 2 to 4:** Run (fail), implement, run `fast` and `game` (pass).
- [ ] **Step 5:** Commit.

### Task 3.2: Band shown to the player (S, Sonnet high)

Plain-client safe: when a cap actually lowers his stats, the player gets one chat line at round start ("Your gear is matched to this battle's band"), through `FeedLines`. There is no new HUD and no packet.

- [ ] **Step 1:** Add a `FeedLines` check for the exact copy, then implement and commit.

### Task 3.3: Re-tune and the newcomer test (S, Opus medium)

- [ ] **Step 1:** Run all of `warlord_gate.py`'s plans with the band on, including `warlord-fair.txt`.
- [ ] **Step 2:** Adjust `bandSpread` first, and only then `baseHealth` or `troopHitFactor`, until every criterion passes.
- [ ] **Step 3:** Update the report and commit. **Gate G1 (again)** with Kort's newcomer-vs-veteran playtest.

## Phase 4: Leaderless army HUD (size S, Opus medium for the HUD)

### Task 4.1: HUD line and action bar (S)

There is no protocol change: `state` already carries the respawn seconds (`GravesAndGloryClient.respawn`), and `OrderRules` already refuses orders while respawning.

**Files:** `FeedLines.leaderless(int seconds)` gives the copy. Default: "Your army is leaderless: 9 s". It is the builder's wording (section 7). Keep it to one line that fits `FeedFitCheck` at Compact. `BattleHud.respawn` draws it as a second line on the respawn card. `Warlords.leaderlessTick(Battle)` runs once a second from the respawn loop and sends `p.sendMessage(Text, true)` (the action bar) to each downed human. That works on plain clients.

- [ ] **Step 1: Failing checks:**
  - `FeedLines`/`FeedFitCheck`: `leaderless(12)` fits at Compact width.
  - `UiMathCheck`: the respawn card's height grows by one line and does not collide with the crosshair (`UiMath.respawnY`).
  - In-game `leaderlessActionBar`: a downed FakePlayer receives the overlay each second with the falling count, and an order sent while it is down is refused.
- [ ] **Step 2 to 4:** Run (fail), implement, run `fast` and `game` (pass).
- [ ] **Step 5: Visual evidence.** The respawn card at normal and small windows against `docs/visual-references`, rubric 9.
- [ ] **Step 6:** Commit.

## Phase 5: Bot warlord body (size XL, Opus medium; checks Sonnet high)

**Gate G2 before starting:** the new entity type.

### Task 5.1: `WarlordEntity`, its registration and its rows (L)

**Files:** Create `WarlordEntity.java` (a `PathAwareEntity` with player dimensions and the gear modifiers through `GearCombat.apply(LivingEntity, Loadout)`). Its entity UUID is the bot squad's `owner`, set before spawn. Register the entity type `gravesandglory:warlord` as `WagonEntity` is registered. Create `WarlordRenderer.java`, a client `BipedEntityRenderer` with the player model, the House colour skin and armour through the existing `ArmorFeatureMixin` and `GearLooks` stacks. Add the `botCommand` columns and preflight rules (stance id set, `0 < warlordFallBackShare < 1`, `warlordKitRarity` a `gearRarities` id). Bump `CompatibilityPolicy`.

- [ ] **Step 1: Failing checks:**
  - Preflight test: a bad `warlordStance` is rejected.
  - In-game `botBodySpawns`: a bot squad seated at round start has a living `WarlordEntity` whose `getUuid()` equals `s.owner`, with `teamOf` equal to its team, max health equal to `baseHealth` plus its band-capped kit, and its kit stacks equipped.
- [ ] **Step 2 to 4:** Run (fail), implement, run `fast` and `game` (pass).
- [ ] **Step 5: Visual evidence** of the body for each House at rubric 9, then commit.

### Task 5.2: Same rules as a player (M)

**Files:**
- `Warlords.bodies(Battle)` returns every live seated commander body: players and `WarlordEntity`. Replace the player-list loops in `Tactics` (the nearest-enemy, taunt and hunt loops), `Abilities.enemies`, `GearCombat.foesNear`, `Battle.updateSight` and the capture presence with it.
- `Warlords.isWarlord` includes the body.
- The body's swing calls `Warlords.strike` with charge 1.0, at the interval of its family's `attackSpeed`, so it cleaves and staggers as a player does.
- `allowCommanderDeath` is generalised to `allowWarlordDeath(LivingEntity, ...)`. For the body: discard it, put `respawns(s.owner)`, pay the bounty through `Veterans.onCommanderDeath`, and send `FeedEvents.commander` (generalised to name + team) to the human allies on that team.
- The respawn loop re-spawns the body at `fieldBase` when its time comes.

- [ ] **Step 1: Failing checks:**
  - `bodyTakesTroopHitRules`: a troop blow on the body uses `troopHitFactor`.
  - `bodyCleaves`: its swing into 6 troops damages 3.
  - `bodyDownedRespawns`: killing it pays the commander bounty to the killer's side, keeps it absent for `respawnSeconds`, then brings it back at full health.
  - `troopsTargetBody`: enemy troops on Attack engage it.
- [ ] **Step 2 to 4:** Run (fail), implement, run `game` (pass).
- [ ] **Step 5:** Commit.

### Task 5.3: Leaderless bot army and seat changes (S)

**Files:** `Campaign.botDecide` returns at once while `respawns` contains `s.owner`, so the army keeps its last orders. `Battle.leave` and the reclaim path spawn the body only once `time >= takeoverAt` and discard it when the human returns.

- [ ] **Step 1: Failing checks:**
  - `botLeaderless`: while the body is down no `Assignment` changes.
  - `takeoverBodyLifecycle`: drop a FakePlayer seat. There is no body before `takeoverAt` and one after. On reclaim the body is gone and the player is seated, so there is exactly one commander.
- [ ] **Step 2 to 4:** Run (fail), implement, run (pass).
- [ ] **Step 5:** Commit.

### Task 5.4: `WarlordBrain`, target choice by difficulty (L, Opus medium)

Behaviour hangs off the bot's `botCommand` row (`Squad.tier`) and adds to `botDecide` without replacing it.

| Stance | Behaviour |
|---|---|
| `back` (easy) | Stay within `warlordLeash` of the army centre, behind it relative to the nearest enemy. Strike only foes that come within reach. |
| `escort` (normal) | Stay within `warlordLeash` of its army's front. Strike the nearest foe engaging its troops. |
| `hunt` (hard) | Pick the weakest enemy flank (the existing `flankLeastDefended` logic, extended to all three rows here) and go there. When `warlordHuntsPlayers` is set, target an enemy human warlord within 30 blocks of its own troops. |

Every stance falls back to its army at `warlordFallBackShare` health. It never walks into a `brace`/`wall` front arc alone: if the target sits in one, it picks the next target or waits for its troops.

**Interfaces:** `WarlordMath.brainPick(...)` holds the pure scoring (distance, flank weight, whether the foe is in a formation front, the leash). `WarlordBrain.tick(Battle, Squad, WarlordEntity)` runs every 10 ticks.

- [ ] **Step 1: Failing checks.** In `WarlordMathCheck`, `brainPick` cases for each stance and the formation-front avoidance. In-game, `easyStaysBack` (distance to its army centre stays at most leash + 2 over 60 s) and `hardHuntsPlayer` (it closes on a human warlord near its troops).
- [ ] **Step 2 to 4:** Run (fail), implement, run (pass).
- [ ] **Step 5:** Commit.

### Task 5.5: Bot-sim balance and the solo playtest (M, Sonnet high runs; Opus medium for tuning)

- [ ] **Step 1:** `-PbotSim=20` bot against bot, before and after Phase 5, on each difficulty. The gate (added to `warlord_gate.py`) requires the share of troop kills made by warlords to stay at most 35% ("troops stop mattering"). It also requires the existing `balance_gate.py` win-rate checks to stay inside their bands.
- [ ] **Step 2:** Re-run every warlord duel plan: the human-side rules must be unchanged.
- [ ] **Step 3: Kort's playtest checklist** (solo on easy, normal and hard):
  - Can he steamroll the bot army alone? He should not on normal or hard.
  - Does the easy bot hang back?
  - Does the hard bot come for him?
  - Is the bot's army visibly leaderless while it is down?
  - Is the bounty paid?
- [ ] **Step 4:** Report and commit. **Gate G1** for the `botCommand` warlord numbers.

---

## Sizes and routing

| Phase | Size | Builder | Checks and reviews | Needs `anim/v2-stances` |
|---|---|---|---|---|
| 1 Weighty hits | L | Opus medium | Sonnet high | Task 1.6 only (client stagger clips) |
| 2 Warlord body | L | Opus medium (2.1, 2.2, 2.4) | Sonnet high (2.3 harness and gate) | no |
| 3 Fair fights | M | Opus medium | Sonnet high (3.2) | no |
| 4 Leaderless HUD | S | Opus medium (UI) | Sonnet high | no |
| 5 Bot warlord | XL | Opus medium | Sonnet high (5.5 runs) | no |

Each phase gets one focused review (Sonnet high) and at most two fix rounds (WORKFLOW). Phases 1 to 4 are independent of the port plan's code areas except `HitStopMixin`, which is a client mixin to re-target in the port.

## Port timing (Kort's call, G3)

The 1.21.1 port has a feature freeze and waits for the animation rebuild (`docs/superpowers/plans/2026-11-07-port-1211-and-plain-clients.md`).

**Building before the port** puts warlord play in front of players sooner. But it adds to the port's re-target list: two new mixins (`PlayerAttackChargeMixin`; `HitStopMixin` is client-side and sits in the fragile tier) and a new entity type with a renderer. In the port, plain clients would need a Polymer stand-in for `WarlordEntity`: a vanilla humanoid mob with dyed armour, chosen in that plan's P2 spike.

**Building after the port** avoids porting this work twice, but delays it behind the whole port.

A middle path: Phases 1 to 4 before the port (server-side apart from hit-stop, small to port), and Phase 5 after it. Kort decides.

## Open design questions

1. **What does "damage" cover in the gear cap?** The plan caps the `ranged` stat with melee `damage` (section 5.3 says "health, damage and armour"). If ranged should stay uncapped, Task 3.1 drops index `ranged`.
2. **Who sees the leaderless note?** Section 5.4 shows it to the downed player. Should his allies see the action bar too? The plan says no, since allies already get the feed line.
3. **Sweeping families and cleave.** Greatsword and greataxe keep their own sweep instead of also cleaving (Task 1.3). Kort may prefer cleave everywhere.
4. **Pass thresholds.** 14 of 20, at least 6 s, 16 of 20, 30 to 50%, and at most 35% are my readings of "most", "several seconds", "loses", "fair share" and "troops stop mattering". Kort confirms them at G1.
