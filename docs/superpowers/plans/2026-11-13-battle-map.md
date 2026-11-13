# Battle Map Redo and Bot Plans Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the M-key `MapScreen` with a full-screen, zoomable battle map. It gets two generated looks (parchment and real), and players can give allied bots point plans and direct orders.

**Architecture:**
- Pure, Minecraft-free logic lives in classes the fast `javac` run checks: `BattlePlan`, `MapView`, `BattleMapGrid` and `MapKinds`.
- The server scans the field once per map build, keeps the plans on the `Battle`, applies them in one hook in `Campaign.botDecide`, and sends three new packets.
- The client builds both textures off-thread from the grid, then draws the new `BattleMapScreen` and the world plan flags.

**Tech Stack:** Java 17, Fabric 1.20.1 (Yarn names as used in the repo), the Fabric networking API, Gson, `NativeImage`, and the plain-`main` fast checks in `test-tools/standalone`.

**Spec:** `docs/superpowers/specs/2026-11-13-battle-map-design.md` (approved by Kort 2026-11-13). The look algorithm reference is `tools/battlemap_preview.py` (its docstring gives the port order), with kinds in `src/main/resources/assets/gravesandglory/battlemap/kinds.json`.

## Global Constraints

- Never hand-edit `Spec.java`; never write `eula.txt`; never kill a running client; at most five clients; don't touch `bb-play` or other worktrees.
- Workers: Sonnet 5.5 (`sonnet`) at medium for routine work and high for debugging and reviews; Opus 5.5 (`opus`) at medium for UI art and layout. Never max; never Fable. Set the model and effort on every worker.
- One Sonnet-high review per milestone, with at most two fix rounds.
- Shapes carry meaning, colour only state (UI spec 6.1). No colour words in the legend.
- The `state_v10` packet is unchanged. The new channels are exactly `gravesandglory:battlemap_grid_v1` (S2C), `gravesandglory:battlemap_v1` (S2C, every 20 ticks) and `gravesandglory:battleplan_v1` (C2S). There is no save change.
- Bot-side contract (agreed with the AI lead): retreat, no retreat while in contact, and self-defence keep priority. The muster refill and release still run. Unplanned bots still spread through `pickPoint` claims. A Defend or Hold plan replaces the guard-only-under-threat rule for that point and never clears other armies' `botGuard` entries. A player taking a bot's seat drops its direct order. `BattlePlan.jobFor` is a pure query. Pathing, `Tactics` and routing do not change.
- Exact values:
  - Zoom runs from fit up to 8 screen px per block.
  - The drag threshold is 4 px.
  - Parchment is 3 texels per block.
  - Defend engages within the point radius + 16.
  - Bot lines are at most one per bot per 100 ticks, and lines that fall due together are spread over 20 to 60 ticks.
  - A player may send at most one plan packet every 10 ticks.
  - Toggle key T, fit key Home.
- Ack copy: `on it, taking {P}` / `defending {P}` / `holding {P}` / `following you` / `pushing there` / `guarding there` / `holding there` / `back to my own plan`, where `{P}` is the point id letter.
- Kort's known-red checks on main are p1-ui-army-selected, p1-ui-chat-respawn, p1-ui-chat-lift, p1-ui-seals and p1-ui-report-open. Don't fix them.

## Review Focus

1. **A map with no grid yet** (a late joiner, or a packet still on its way): the screen opens and draws the fallback texture with markers; nothing throws. Test: `MapViewCheck.fitsWithoutGrid`, plus `BattleMapScreen` drawing with `BattleMapClient.grid==null` in `p1-ui-map-view`.
2. **A plan on a point index that a map switch made invalid, or an army id whose squad has left:** the server refuses or drops it, and `assign` ignores it. Test: `BattlePlanCheck.staleIndexIgnored`, `BattlePlanCheck.unknownArmyIgnored`.
3. **Two teammates planning the same point in the same tick:** the higher tick wins, and on an equal tick the later call wins. Test: `BattlePlanCheck.latestWins`.
4. **Zoomed in at a field corner, then the window resized:** the view stays clamped and the cursor-anchored zoom does not jump. Test: `MapViewCheck.clampAfterResize`.
5. **Teams with zero ally bots (all human) or one bot with four plans:** popups still work, and the one bot takes the newest plan. Test: `BattlePlanCheck.oneBotNewestPlan`, `BattlePlanCheck.noBots`.

---

### Task 1: `BattlePlan`, the pure plan table and assignment

**Files:**
- Create: `src/main/java/gg/gravesandglory/BattlePlan.java`
- Create: `test-tools/standalone/BattlePlanCheck.java`
- Modify: `tools/verify.py:81` (add `BattlePlan` to `names`)

**Interfaces (produces):**
```java
public final class BattlePlan {
  public enum Job { ATTACK, DEFEND, HOLD, FOLLOW }
  public record PointPlan(int point, Job job, String setter, long tick) {}
  public record Order(String army, Job job, int point, double x, double z, String setter, long tick) {} // point -1 = a place
  public record Army(String id, int team, double x, double z, boolean bot, boolean retreating) {}
  public record Assignment(Job job, int point, double x, double z, boolean direct) {}
  public void setPoint(int team, int point, Job job, String setter, long tick);           // job null clears
  public void setOrder(int team, String army, Job job, int point, double x, double z, String setter, long tick); // job null clears
  public void dropArmy(String army);
  public void clear();
  /** Attack plans on a point the team owns uncontested become Defend (same setter, tick kept). Returns the changed points. */
  public java.util.List<Integer> update(int[] owners, boolean[] contested);
  public java.util.List<PointPlan> points(int team);   // newest first
  public java.util.List<Order> orders(int team);
  public java.util.Map<String, Assignment> assign(int team, java.util.List<Army> armies, double[] px, double[] pz); // pure
  public Assignment jobFor(String army, int team, java.util.List<Army> armies, double[] px, double[] pz);          // pure, null = no plan
  public static String ackLine(Assignment a, String[] pointIds); // null assignment -> "back to my own plan"
}
```

- [ ] **Step 1: Write `BattlePlanCheck`** in the `FeedFitCheck` style (`check(boolean,String)`, a `main` that runs every case and prints the count). Cases and assertions:
  - `assignNearestPerPlan`: points at x = 0, 100, 200. A plan of ATTACK on 2 (tick 5) and DEFEND on 0 (tick 3). Bots `a` at x=190 and `b` at x=10. Expect `a`→(ATTACK,2) and `b`→(DEFEND,0).
  - `extrasGoToAttackFirst`: three bots and plans ATTACK 2 and HOLD 0. The third bot gets ATTACK 2.
  - `oneBotNewestPlan`: one bot and four plans with ticks 1 to 4. The bot gets the tick-4 point.
  - `latestWins`: `setPoint(0,1,ATTACK,"p1",10)` then `setPoint(0,1,HOLD,"p2",10)`. `points(0)` has one entry, HOLD by p2.
  - `directBeatsTeam`: an order HOLD at (50,60) for bot `a`. `jobFor("a")` is `(HOLD,-1,50,60,direct=true)`, and `a` is left out of the point split.
  - `retreatingExcluded`: a retreating bot gets no point assignment, and the others cover the plans.
  - `humansIgnored`: an army with `bot=false` is never assigned.
  - `noBots`: `assign` with no bots returns an empty map.
  - `attackBecomesDefend`: an ATTACK on point 1 for team 0, then `update(owners={-1,0},contested={false,false})`. The plan is DEFEND and `update` returns `[1]`. With contested true, it stays ATTACK.
  - `staleIndexIgnored`: a plan on point 7 with only 3 points is skipped by `assign`.
  - `unknownArmyIgnored`: an order for army `zz` that is not in the list does not throw or assign.
  - `dropArmy`: removes that army's order only.
  - `teamsSeparate`: team 1 plans never reach team 0 bots.
  - `jobForIsPure`: calling `jobFor` twice gives equal results, and `points`/`orders` are unchanged.
  - `ackCopy`: each job and target pairs with the exact ack copy from Global Constraints.
- [ ] **Step 2:** Run `python tools/verify.py fast`. Expected: the fast java step FAILs (BattlePlan missing).
- [ ] **Step 3: Implement `BattlePlan`.**
  - **Assignment:** drop orders and plans that fail validation. Take the bots of `team` that are not retreating and have no valid direct order. Go through the plans newest first (tick descending), giving each the nearest unassigned bot. Then give each remaining bot the nearest point among the ATTACK plans, else DEFEND, else HOLD.
  - **Point positions:** an `Assignment` for a point carries `px/pz` of that point.
  - **FOLLOW** is only an order; the caller fills its x/z with the setter's position.
- [ ] **Step 4:** Run `python tools/verify.py fast`. The fast java step PASSes.
- [ ] **Step 5: Commit** `Battle map: BattlePlan pure plan table and assignment (fast checked)`.

### Task 2: `MapView`, the pure view transform

**Files:**
- Create: `src/main/java/gg/gravesandglory/MapView.java`
- Create: `test-tools/standalone/MapViewCheck.java`
- Modify: `tools/verify.py:81` (add `MapView`)

**Interfaces (produces):**
```java
public final class MapView {
  public MapView(int halfX, int halfZ);             // field half sizes (Field.halfX/halfZ)
  public void layout(int x, int y, int w, int h);   // screen rect of the map area; refits when never moved, else re-clamps
  public void fit();                                 // whole field inside the rect, 8 px margin, centred
  public double scale();                             // screen px per block
  public double fitScale();
  public void zoomAt(double sx, double sy, double steps); // scale *= 1.25^steps, clamped to [fitScale, 8], keeping the world point under (sx,sy) fixed
  public void pan(double dxScreen, double dyScreen);
  public double worldX(double sx); public double worldZ(double sy);
  public double screenX(double wx); public double screenY(double wz);
  public boolean moved();
}
```
- [ ] **Step 1: Write `MapViewCheck`.** Cases:
  - `fitsField`: field 270x190 in a 900x500 rect. All four corners map inside the rect, and one axis touches the margin.
  - `fitsWithoutGrid`: `layout` then `fit` with no other calls gives `scale()==fitScale()`.
  - `zoomKeepsCursor`: after `zoomAt(300,200,3)`, `worldX(300)` and `worldZ(200)` are unchanged (within 1e-6).
  - `zoomClamped`: `zoomAt(..,100)` gives `scale()==8`, and `zoomAt(..,-100)` gives `fitScale`.
  - `panClamped`: panning far left at fit scale moves nothing. At scale 8, panning far left stops with the field's west edge at the rect's left.
  - `clampAfterResize`: zoom 4x, pan to the SE corner, then `layout` to a smaller rect. Field corners never leave gaps inside the rect beyond the margin.
  - `roundTrip`: `screenX(worldX(s))==s`.
- [ ] **Step 2:** Run fast; it FAILs.
- [ ] **Step 3: Implement.**
- [ ] **Step 4:** Run fast; it PASSes.
- [ ] **Step 5: Commit.**

### Task 3: The field grid and kinds (pure codec, server scan)

**Files:**
- Create: `src/main/java/gg/gravesandglory/BattleMapGrid.java` (pure: data, encode and decode)
- Create: `src/main/java/gg/gravesandglory/MapKinds.java` (pure: parse `kinds.json` text with Gson, `kind(String id, int raise)` → index, `flat` handling as in the preview script)
- Create: `src/main/java/gg/gravesandglory/BattleMapScan.java` (server: scan the `ServerWorld` field)
- Create: `test-tools/standalone/BattleMapGridCheck.java`
- Modify: `tools/verify.py:81` (add `BattleMapGrid MapKinds`)

**Interfaces (produces):**
```java
public final class BattleMapGrid {
  public final String map; public final String build; public final int minX, minZ, width, depth;
  public final String[] palette; public final short[] look; public final short[] height; // index = (z-minZ)*width + (x-minX)
  public BattleMapGrid(String map, String build, int minX, int minZ, int width, int depth, String[] palette, short[] look, short[] height);
  public byte[] encode();                          // Deflater, version byte 1
  public static BattleMapGrid decode(byte[] data); // throws IllegalArgumentException on bad version/size
}
public final class MapKinds {
  public static MapKinds parse(String json);
  public String[] kinds(); public int raise();
  public int kind(String blockId, boolean raised);  // raised = stands >= raise above local terrain
}
final class BattleMapScan { static BattleMapGrid scan(net.minecraft.server.world.ServerWorld w, String map, String build, int halfX, int halfZ); }
```
- [ ] **Step 1: Write `BattleMapGridCheck`.**
  - `roundTrip` on a 541x381 grid with a 40-entry palette and hashed looks and heights: every field is equal, and the encoded length is under 200000.
  - `badVersion` throws.
  - `kindsTable` parses the real `kinds.json` from disk. `minecraft:water`→water, `minecraft:oak_leaves`→foliage, `minecraft:oak_stairs` raised→building, the same not raised→its flat kind, and an unknown id gives the default.
- [ ] **Step 2:** Run fast; it FAILs.
- [ ] **Step 3: Implement.**
  - **Scan:** do as the preview's step 1. For each column, take the top non-air block whose collision or outline shape covers the full cell top. Skip blocks with an empty or partial shape (plants, torches, flowers); keep water. Store its registry id and its height (y, plus 1 for a full block). The scan runs once after the field is built (find where `EngineMaps.apply` and the old-map build finish; the arena-ready flag that `Battle.join` waits for).
  - **Cache:** cache the scan on `Battle` with the key (map id, build hash: the `EngineMap.sha` for engine maps, `"old-"+CampaignMaps.selected+"-"+Spec hash` for old maps), and rescan only on a different key.
  - **Time:** log the scan time once.
- [ ] **Step 4:** Run fast; it PASSes.
- [ ] **Step 5: Commit.**

### Task 4: Server wiring (packets, plans on the battle, bot hook, lines)

**Files:**
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java`
  - Channel ids next to `PING` (~:130-148).
  - A `battleplan_v1` receiver.
  - `Battle` fields `final BattlePlan plan=new BattlePlan()` and the grid cache.
  - Send the grid on round start and on a late join.
  - Send `battlemap_v1` every 20 ticks from `Battle.tick`.
  - `plan.clear()` where the battle or round resets.
  - `plan.dropArmy` where a player takes a bot seat (~:929, and `Campaign.prepareRound`).
- Create: `src/main/java/gg/gravesandglory/BattleMapNet.java` (server: write the grid and state packets, validate and apply plan packets, queue ack lines through the feed (`FeedEvents`/`EventFeed`, kind `chat`, the team's players only), with rate limit and spread).
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` `botDecide` (~:934-1000). It is the only bot-logic change; rebase onto `army/war-table` before this task if it has landed.

**Wire formats:**
- `battlemap_grid_v1`: `int length`, then the bytes of `BattleMapGrid.encode()`.
- `battlemap_v1`:
  - The tick.
  - Ally armies: `n`, then per army: id string(36), name string(32), bot bool, retreating bool, aimed bool (the mode sets this bot's goal, so the map offers it no order: call 11; added in Task 6), group count `g`, and per group: kind id string(16), cx float, cz float, count int, health float 0..1, order string(16), tx float, tz float, fighting bool, plus an assignment (job byte, -1 for none, point int, x float, z float, direct bool).
  - Plans: `m` × (point int, job byte, setter string(32)).
  - Orders: `k` × (army string(36), job byte, point int, x float, z float, setter string(32)).
  - Ally groups come from `b.units` grouped by squad and group id, team-filtered. Own groups are sent too, so one source serves all.
- `battleplan_v1`:
  - `byte kind` (0 = point plan, 1 = direct order), `int point`, `String army(36)`, `byte job` (-1 clears), `double x`, `double z`.
  - Valid only when the player is seated in a running round, the point index is in range, the army is a bot squad of the player's team, and the 10-tick cooldown has passed.
  - FOLLOW stores the setter's UUID in `setter`, and the hook reads that player's position.
  - A refusal is a feed line to that player only.

**Hook in `botDecide`:**
- Compute `assignment=b.plan.jobFor(s.owner.toString(), s.team, armies, px, pz)` once per decision, after `retreating`.
- In the non-retreating branch, when `goal==null`:
  - ATTACK→`attack` at `spot(point)` or the place.
  - HOLD→`hold` there.
  - DEFEND→`attack` toward the point while a sighting is within radius+16 of it, else `hold` at it.
  - FOLLOW→`hold` at the setter's position.
- Set `choice` for point jobs so the listener logs it.
- With an assignment, skip the guard block for this army, but still `botGuard.remove(s.owner)` its own entry. `planGroups` runs with `guard=-1`.
- Mode objectives (`botTarget`) still win in Siege stages and Convoy, as the spec's mode rule says.
- Call `plan.update(owners, contested)` once a second in `Battle.tick`.
- Queue an ack when an army's assignment differs from its last one (keep `Map<String,Assignment> lastAck` in `BattleMapNet`).

- [ ] **Step 1: Write the failing in-game check `p1-plan-server`.** Add it to `test-tools/java/gg/gravesandglory/Phase1Checks.java` next to the map stages, or as a `BotSimulation` assertion if that is quicker to run (the implementer decides). On an old map with bot fill, send `battleplan_v1` ATTACK on point 1. Within 2 bot decisions, every allied bot that is not retreating has `s.target` on point 1 or, with more bots than plans, on an ATTACK point. The team feed got one `on it, taking ` line per bot. Then a direct HOLD at a place: that bot's target is the place within 1 decision.
- [ ] **Step 2:** Run it through `tools/checkq.py` as SETUP.md says. It FAILs.
- [ ] **Step 3: Implement** as above.
- [ ] **Step 4:** Run it and `python tools/verify.py fast`. Both PASS.
- [ ] **Step 5: Measure.** Run `tools/bot_quality.py` on a 4-match Domination sim (`tools/checkq.py submit sim`) on main's code (baseline: stash nothing, use a run of main) and on this branch with no plans set. Idle, Hold idle, trickle and objective share are unchanged within noise. Save both to `C:\Users\kortt\Melty\status\battle-map-botquality.md`.
- [ ] **Step 6: Commit.**

### Task 5: The client grid textures (`BattleMapArt`)

**Files:**
- Create: `src/main/java/gg/gravesandglory/BattleMapArt.java` (client)
- Create: `src/main/java/gg/gravesandglory/BattleMapClient.java` (client: receivers for both S2C packets, state records and the grid cache, registered from `GravesAndGloryClient`'s init where the other receivers are)

**Interfaces:**
- Consumes: `BattleMapGrid`, `MapKinds`.
- Produces: `BattleMapClient.grid` (nullable), `BattleMapClient.state` (last `battlemap_v1` record), `BattleMapArt.real()`/`parchment()` texture `Identifier`s (null until built), `BattleMapArt.request(grid)` (idempotent per map and build).
- The real look:
  - Per palette id, take the average colour of its top-face sprite (baked model → `Direction.UP` quads → sprite), tinted through `BlockColors.getColor(state,null,null,tint)` when the quad has a tint. The fallback is `state.getMapColor`.
  - Shade as in preview step 2; 1 texel per block.
- The parchment look: port preview steps 3a to 3e exactly (3 texels per block), with the same constants. A 541x381 grid gives 1623x1143, under GL limits.
- Both build on `CompletableFuture.supplyAsync` and register on the client thread, like `prepareMap`. Free the old textures on a map change.

- [ ] **Step 1:** Add the check `p1-ui-map-art`. On New Kingsfield, after round start, both textures are non-null within 200 ticks. The parchment centre texel at The Hand's bowl differs from the base parchment colour. Save screenshots `ui-map-parchment-kf` and `ui-map-real-kf` of the screen at fit (they need Task 6, so this check is completed in Task 6).
- [ ] **Step 2: Implement.** Compare a dumped parchment PNG (dev-only `-Dgravesandglory.dumpMapArt=dir`) against `tools/battlemap_preview.py` output by eye for Kingsfield. They are the same algorithm, so they should look alike.
- [ ] **Step 3: Commit.**

### Task 6: `BattleMapScreen` (the screen itself)

**Files:**
- Create: `src/main/java/gg/gravesandglory/BattleMapScreen.java` (client; extends `GravesAndGloryClient.ArmyScreen`, so make that class package-visible if needed)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGloryClient.java`
  - The M key and the `ArmyScreen.keyPressed` M branch open `BattleMapScreen`.
  - Delete `MapScreen` and keep `map(...)` only if something else draws it (grep `map(`). The war-table preview may draw it; leave that caller alone.
  - The client option for the look: `lookParchment`, persisted where the other client options are.
- Modify: `test-tools/java/gg/gravesandglory/Phase1Checks.java` (stages 17-19 move to the new screen).

**Behaviour (spec sections 2, 4 and 6):**
- **Bars:** the top bar shows the mode, both scores, the clock and held points. The bottom bar shows the ping buttons (`Spec.PINGS`, as before), the Fit button and the glyph legend. The toggle is at the top right.
- **Map area:** the screen minus the bars, laid out by `MapView`, scissored. Draw the parchment or real texture under it, or the fallback (`mapTexture`, or a flat parchment fill).
- **Markers:** the table in spec section 4. Ally and own groups come from `BattleMapClient.state`, falling back to `CommandMode.groups`/`markers`. Sightings, pings, points and the player work as now.
- **Plan flags** sit over the points.
- **Popups:** the point popup has Attack, Defend, Hold, Clear and the point name. The ally-bot popup has Attack (then the next click picks a point or a place), Defend here, Hold here, Follow me and Clear. Esc or a click elsewhere closes it. They send `battleplan_v1` through `BattleMapClient.sendPlan(...)`.
  - In Convoy and Survival, the point popup only offers Ping here (`b.mode.capturesPoints` mirrored to the client; check what the state packet already says about the mode).
- **Tooltips** on hover for every marker, as in spec section 2.
- **Input:**
  - Scroll zooms.
  - Left or middle drag past 4 px pans.
  - A left click without a drag selects, in this order: popup button, point, ally bot plaque, own group, ping mode target.
  - Right click moves the selected own groups (`CommandSelection.issueSelected("hold",...)` as now).
  - T toggles the look; Home fits; M and Esc close.
- **Legend:** `legend()` stays static and public. New copy: `"{hit} select or plan / {use} to move / scroll to zoom / drag to pan"`, with the bindings named as the old `hint()` does. Glyph key: Yours, Ally, Enemy, Fight, Plan.
- **Visual rule:** the War Table direction (dark wood bars, brass edges, parchment map). Check at 960x540 GUI 2 and 1920x1080 GUI 3, and at the smallest supported window.

- [ ] **Step 1: Update `p1-ui-map-glyphs` and add `p1-ui-map-view` and `p1-ui-map-plan`** (failing).
  - `p1-ui-map-glyphs` keeps its asserts, against the new legend string and the new screen class.
  - `p1-ui-map-view`:
    - The screen opens at `scale()==fitScale()`.
    - A synthetic scroll of +3 at the centre raises the scale and keeps the centre's world point.
    - A drag of 50 px moves the view.
    - T switches `lookParchment` and the drawn texture id.
    - Home restores fit.
    - Screenshots: `ui-map-fit`, `ui-map-zoom`, `ui-map-real`.
  - `p1-ui-map-plan`:
    - Click point B's screen position: the popup is open.
    - Click Attack: within 40 ticks `BattleMapClient.state` lists the plan ATTACK on B by this player, and a feed line from a bot starts `on it, taking`.
    - Click an ally bot plaque, then Hold here: within 40 ticks the state lists the direct order.
    - Screenshot: `ui-map-plan`.
  - Run each on an old map and on New Kingsfield (a second round with `CampaignMaps.selected` set to Kingsfield's index, as other engine-map checks do).
- [ ] **Step 2:** Run the checks through checkq. They FAIL.
- [ ] **Step 3: Implement** the screen. Opus medium does the layout and drawing.
- [ ] **Step 4:** Run the checks and `verify.py fast`. They PASS, apart from the known-red list.
- [ ] **Step 5:** Look at every screenshot. Fix clipping, overlap and unreadable labels (at most two rounds).
- [ ] **Step 6: Commit.**

### Task 7: The world plan flags (`PlanMarkers`)

**Files:**
- Create: `src/main/java/gg/gravesandglory/PlanMarkers.java` (client; `WorldRenderEvents.AFTER_ENTITIES`, registered beside `CommandMode::renderWorld` in `GravesAndGloryClient` ~:190)

The flags are billboarded quads 6 blocks above each planned point's elevation:
- the job glyph (sword, shield or banner) and the point letter on a small parchment flag;
- scaled so they stay readable from 100 blocks;
- only from `BattleMapClient.state`, so only for the player's own team.

A direct order the player gave shows a smaller flag at its target.

- [ ] **Step 1:** Extend `p1-ui-map-plan`. After the plan is set and the screen is closed, `PlanMarkers.drawn()` (the count drawn in the last frame) is at least 1. Screenshot `ui-plan-flag` from the player's view toward point B.
- [ ] **Step 2: Implement.**
- [ ] **Step 3:** Run the check; it PASSes. Look at the screenshot.
- [ ] **Step 4: Commit.**

### Task 8: Bot proof and the report

**Files:**
- Modify: `test-tools/java/gg/gravesandglory/BotSimulation.java`
  - `-PsimPlan=TEAM:POINT:JOB` sets a point plan at round start.
  - `-PsimOrder=TEAM:BOTINDEX:JOB:X:Z` sets a direct order.
  - Both go through the same server path as the packet: `BattleMapNet.apply(...)` with a synthetic setter.
- Modify: `tools/bot_quality.py` only if it needs a column for the plan. Add the plan columns to `decisions.csv` through the listener.
- Create: `C:\Users\kortt\Melty\status\battle-map.md` (the report)

- [ ] **Step 1: Run the sims.**
  - Domination on an old map and on Kingsfield, at 1v1, 2v2 and 4v4. Plan: Dawn ATTACK on the point farthest from Dawn's base.
  - **Point plan pass:** in `decisions.csv`, every non-retreating Dawn bot decision after the plan chooses that point (or another ATTACK point), and Dawn owns it at some time in the round.
  - **Direct order pass:** HOLD at a place for bot 0. Its target stays at the place in every non-retreating decision, and it retreats when its share drops (at least one retreat row, if the sim had one).
- [ ] **Step 2: Rebase and verify.** Rebase `ui/battle-map` on main (`git rebase main`) and re-run `verify.py fast` and the map checks.
- [ ] **Step 3: Write the report** at `C:\Users\kortt\Melty\status\battle-map.md`:
  - outcome and commit list;
  - checks with results;
  - the sim numbers;
  - screenshot paths;
  - bot_quality before and after;
  - open defects;
  - implemented, tested, visually inspected and accepted, each stated separately.
- [ ] **Step 4:** Send Kort the in-game screenshots (SendUserFile).
