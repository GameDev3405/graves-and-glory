# Port to Minecraft 1.21.1 and plain-client support

2026-11-07. Status: approved in principle by Kort on 2026-11-07 (decisions in section 2); not started. Docs only: this plan changes no code. Task routing follows `docs/WORKFLOW.md` (Sonnet 5.5 high for routine work, Opus 5.5 medium for art, animation, UI and systems, never Fable, xhigh only when explicitly needed). Changes to protocol, saves and dependencies were approved by Kort for this work on 2026-11-07; anything beyond what section 2 records still needs him.

Every number below comes from a read of `main` at `cc93e4b4` (worktree `bb-docs-plan-121`) unless marked "unverified". Sizes are labelled estimates.

## 1. Intent and why

Graves and Glory is a Minecraft 1.20.1 Fabric mod that every player must install; the login gate (`VersionGate`) refuses any client without it. Two things Kort wants for 1.0 conflict with that:

1. The official network and hub (Phase 7 of `docs/PLAN-TO-1.0.md`) should be built on a current Minecraft version, not on 1.20.1, so the port comes first.
2. Players who will not install a modpack should still be able to play everything, so the modpack becomes optional. The modpack stays the best way to play (custom troop models and animations, HUD, War Table, keybinds), but a vanilla client must be able to join the same battles and queues.

Reaching both goals means two jobs, in this order: port the mod to 1.21.1 with a feature freeze, then make the server work for clients that do not have the mod and give those clients a vanilla-only front end.

## 2. Decisions

### 2.1 Kort's decisions (2026-11-07)

- Target Minecraft 1.21.1 (Fabric). Port first, with a feature freeze during the port. The 1.0 network and hub are then built on 1.21.1.
- The modpack is optional. Players with the modpack get the full mod (custom troop models and animations, HUD, War Table, keybinds). "Plain players" (vanilla client, no modpack) can do everything: all modes, all 5 Houses and 30 troops, the armoury, the War Table, the hub and the queues. Only visuals and controls are simpler.
- Fairness: same actions, same cooldowns, same reach for both kinds of player. The modpack adds only visuals and convenience. Both kinds are mixed in the same battles and queues.
- Plain troops look like vanilla mobs chosen by role: skeletons for archers, pillagers for crossbows, vindicators for axes, evokers or witches for casters, ravagers or iron golems for big troops. House colour comes from dyed armour and banners.
- Plain controls: locked hotbar items (right-click) plus the swap-hand key. Plain HUD: boss bar, action bar and sidebar scoreboard. Plain menus: chest-style GUIs.
- A server resource pack is required for plain players (declining means they cannot join). The pack carries item and gear icons, HUD font glyphs and chest-menu art. It does not retexture mobs.
- Plain clients on 1.21.1 and newer may join, through ViaVersion on the Velocity proxy. Older clients cannot. Modpack players must be exactly 1.21.1.
- Gentle nudge to the modpack: a hub sign plus one chat line on first join that links it. Nothing during battles.
- The modpack is published on both Modrinth and CurseForge.
- Timing: the beta is modpack-only. Plain support is built during the beta and must be ready for the 1.0 launch.

### 2.2 Claude's calls (Kort can override any of these)

- **Mod detection by a handshake payload.** On join the modded client sends a small "hello" payload (mod version and build); absence after a short grace period means plain. This replaces the login-phase refusal in `VersionGate`. Reason: the existing login query (`release_check_v1`) needs the proxy to forward custom login queries, which is unverified.
- **Polymer as the plain-client layer.** Polymer (Patbox, LGPL-3.0, Fabric) lets a server-side mod show custom content to vanilla clients (virtual items, blocks and entities) and can generate and host a resource pack. A `+1.21.1` build exists (0.9.19+1.21.1, published 2026-01-11, per Modrinth). Beyond that Modrinth summary its feature details are unverified; the spike (P2) is where they get verified. Polymer would handle: per-viewer entity substitution for troops, the custom `supply_wagon` entity type and custom sound events as seen by vanilla clients, the generated resource pack, and a client-detection hook. It would not handle: chest menus (use vanilla inventory GUIs, optionally with a small library), boss bar and scoreboard HUD (vanilla server API), or hotbar orders (our own code).
- **A plain-client spike as an early go/no-go gate (P2).** One battle with vanilla-mob troops, the plain HUD and one chest menu, shown to Kort before the large plain build (P4) starts. If the spike fails, Kort chooses between narrowing plain support and dropping it.

### 2.3 Non-goals

- Clients older than 1.21.1.
- Retextured or re-modelled vanilla mobs for plain players. The server pack does not touch mob textures or models.
- Separate queues for plain and modpack players. Everyone queues together.
- Publishing, pushing or merging anything. Kort publishes.
- Changing gameplay numbers (stats, cooldowns, reach, balance) as part of this work.

## 3. Port inventory (what changes from 1.20.1 to 1.21.1)

Counts are from `main` `cc93e4b4`; "fragile" rankings are expectations, not checked against 1.21.1 sources.

| Area | Facts today | What the port does |
|---|---|---|
| Source size | 178 main Java files, 38,086 lines (24 mixin files) | Everything compiles on 1.21.1 with Yarn or the mapping the toolchain supports |
| Toolchain | Java 17 (`options.release = 17`), Loom 1.7.4, Yarn 1.20.1+build.10, Fabric Loader 0.16.14, Fabric API 0.92.2+1.20.1, Gradle 8.8 wrapper, LWJGL forced to 3.3.1 | Java 21 (1.21.1 requires it; stated from knowledge, not fetched), new Loom if needed (1.7.4 fit is unverified), Fabric API 0.116.17+1.21.1 (newest for 1.21.1 per Modrinth), mapping Yarn 1.21.1+build.3 (flagged not stable on meta.fabricmc.net), drop the LWJGL force if it no longer applies |
| Networking | 12 files use PacketByteBuf channels; 18 play channels plus the `release_check_v1` login query; 13 client and 5 server receivers; about 21 `PacketByteBufs` calls | Rewrite each as a `CustomPayload` record with a `PacketCodec`, registered through `PayloadTypeRegistry`. This is the largest single job |
| Item data | Only 4 item-NBT call sites (`GearLooks` 3, `GravesAndGlory.unbreakable()` 1); gear stand-ins carry `gg_uid`, `gg_item`, `gg_rarity`, Unbreakable, HideFlags, empty AttributeModifiers | Move to data components: `custom_data`, `custom_name`, `unbreakable`, `attribute_modifiers` with tooltip off, and `custom_model_data` for pack icons |
| Identifiers | 74 `new Identifier(` calls in 33 files | `Identifier.of` (the constructor is private from 1.21) |
| NBT file I/O | `EngineMaps`, `Structures`, `MapRuns`, `CampWorld`, `WagonEntity`, `GearLooks` use `NbtCompound` and `NbtIo`; 41 `.nbt` structures under `data/gravesandglory/structures` | Adjust `NbtIo` signatures (size tracker), block-state and block-entity reads and writes, and confirm all 41 structures load |
| Data folders | Only `damage_type`, `structures` and `tags/damage_type` under `data/gravesandglory` | Check the singular folder renames of 1.21 apply; expected small |
| Mixins | 24 files (7 common, 17 client), `required: true`, `compatibilityLevel JAVA_17` | Re-target each. Expected most fragile: `ArmorFeatureMixin`, `HeldItemFeatureMixin`, `BowDrawMixin`, `HitEventMixin`, `ContactFeedbackMixin`, `CinematicHudMixin`, `HungerHudMixin`, `ChatHudMixin`, `RenderSystemAccessor`, `EntityRenderDispatcherAccessor`, `AutosaveMixin`. Medium: `Camera*`, `Mouse`, `CorpseMixin`, `ShieldCooldownMixin`, `MinecraftClient` |
| Rendering | `DrawContext` in 30 files, `VertexConsumer` 13, `ModelPart` 11, `MatrixStack` 16, `RenderLayer` 9, `RenderSystem` 8, shader use 6, `FeatureRenderer` in 5 files, `WorldRenderEvents` 18 mentions, `InGameHud` render signature (RenderTickCounter) | Port the custom troop animation engine (`MotionEngine`, `TroopModel` and related; it is not GeckoLib), the vertex and buffer API changes and the HUD hooks |
| Input and UI | 15 key bindings, canvas UI (`UiCanvas`) over `DrawContext`, only two classes extend `Screen` directly; `WarTablePages` is 2,288 lines | Port the canvas and each page; keep behaviour |
| Troop entities | Server uses 14 vanilla entity types (see section 4); `TroopRenderers.over(...)` replaces renderers on the client; one custom `WagonEntity` | Re-register renderers; port attribute use (`generic.scale` exists from 1.20.5) |
| Registries | Sound events registered for every `AudioCues` id (143 `.ogg`), one custom entity type, one datapack damage type | Re-register; `Registries` use in 21 files |
| Resources | 1,536 png, 286 json, 143 ogg, 41 nbt, 5 jem, 4 mcmeta, 2 ttf under `src/main/resources`; built-in packs `armory` (OptiFine CEM `.jem`), `gg_build`, `weapons` | Pack format, model and item-override changes; keep CEM files working with Entity Model Features for 1.21.1 |
| Persistence | Profiles are Gson JSON (schema 3, backups, parties, layout, match progress), not NBT | Unchanged. Saves must keep loading, which is an acceptance check |
| Fabric config | `fabric.mod.json` pins minecraft `1.20.1`, java `>=17`, fabric-api `>=0.92.2` | Update to 1.21.1, Java 21 |
| Build and test tooling | `tools/verify.py` requires a Java 17 JDK and checks `version "17."`; `tools/package.py` hard-codes fabric-api `0.92.2+1.20.1`; `tools/export_modrinth.py` has `MINECRAFT_VERSION='1.20.1'`; `tools/server-kit` (`KIT_MC=1.20.1`), `tools/builder-server` (Axiom 6.1.5, WorldEdit 7.2.15, both 1.20.1), `tools/mapforge`, `tools/preflight.py`, `make_weapon_textures.py` (reads the vanilla jar as a layout mask), `melty.json`, `dependencies/*.json` | Update every pin; re-check each tool against the 1.21.1 jar |
| Test harness | 94 test-tools Java files (36.9k lines), 7 use networking classes, 6 are mixins; `Phase2Checks.java` is 7,088 lines; 25 standalone checks compiled with `javac --release 17`; `ggbot.py` is a protocol-763 bot whose handshake and login predate the 1.20.2 configuration phase; `builder_leave.py` uses the 1.20.1 chat-command packet | Port the harness, rewrite `ggbot.py` for protocol 767, update `builder_leave.py` and the Gradle check files |
| Modpack dependencies | All pinned to 1.20.1: Iris 1.7.6, Sodium 0.5.13, Lithium 0.11.4, FerriteCore 6.0.1, ImmediatelyFast 1.5.5, ModernFix 5.25.2, Entity Model Features 3.3.12, Entity Texture Features 7.2.6, Presence Footsteps 1.10.1, shaders (Complementary Reimagined and Unbound r5.5.1, Makeup Ultra Fast 9.5g), Excalibur "V1.20" | Modrinth lists 1.21.1 builds for Sodium, Iris, Lithium, FerriteCore, ImmediatelyFast, ModernFix, Entity Model Features, Entity Texture Features and Presence Footsteps (checked 2026-11-07). The shaders already list 1.21.1. CurseForge availability is unverified. Excalibur lists 1.21 to 1.21.10 on Modrinth; the permission Kort has from Maffhew (2026-10-28) was for the V1.20 pack, so re-confirm for the new file |

Headline: about 38k lines, 24 mixins, 18 network channels, 74 identifier sites, 4 item-NBT sites, 94 test-harness files and 10 third-party pins to move.

## 4. Plain-equivalents table

The server already spawns troops as vanilla entity types (`Army*` subclasses built on vanilla `EntityType`s); the modded client overrides their renderers. So plain players already see vanilla mobs, but chosen by the sheet's `entity` column, not by role.

| Today (modpack) | Plain equivalent | Server work |
|---|---|---|
| Custom troop models and animation clips | Vanilla mob chosen by role; House colour from dyed armour, banners and name colour | Per-viewer entity-type substitution (Polymer virtual entity or spawn-packet rewrite) so, for example, Gilded archers (husks on the server) show as skeletons and mages as witches or evokers |
| 15 key bindings (orders, formation, map, menu, armoury, banner, gear, hints) | Locked hotbar items (right-click) plus the swap-hand key | Map each item and the swap-hand key onto the existing `Battle.command` strings and `OrderRules.playerOrder`; the server already enforces cooldown 4 ticks, range 240, charge cooldown 400 |
| Targeted orders by crosshair | Same server raycast from the player's view | Reuse the existing raycast path |
| HUD from the `state_v10` JSON snapshot | Boss bar, action bar and sidebar scoreboard | New renderer from the same battle state, sent through vanilla packets |
| War Table, camp station screens, lectern screens, armoury tab | Chest-style GUIs | New server-built menus over the same profile and `Battle` calls (nine War Table pages, nine camp stations, the armoury) |
| Local collection import (`profile_v1`) | Fresh server profile | No client file; a plain player starts clean |
| Gear drawn from `gg_item` | Vanilla stand-in items with `custom_model_data` icons from the server pack | Same item data, pack supplies icons |
| HUD fonts, UI art | Server-pack font glyphs and chest-menu art | Pack build and hosting through the proxy |
| `Text.keybind("key.gravesandglory.*")` in chat, labels and hints | Literal text naming the hotbar item or key | Per-viewer text for plain players |
| Custom sound events, `supply_wagon` entity type | Vanilla sounds or direct sound events, and a vanilla stand-in for the wagon | Confirm in the spike how vanilla clients handle the custom registry entries |
| Version gate that refuses unmodded clients | Handshake payload; absence means plain | Replace `VersionGate` behaviour; keep mismatch refusal for modpack clients only |

## 5. Phases

Efforts are estimates in elapsed weeks for one agent lane plus Kort's review time; they will change after P1.

### P0: Freeze and setup decision (about 1 week, estimate)

| ID | Task | Acceptance |
|---|---|---|
| P0.1 | Declare the feature freeze. List what merges first (open lanes such as `anim/geckolib-rebuild` knight work and the animation rework, gear branches, Old Hold wiring) and what waits | A list in `docs/BOARD.md` or `HANDOFF.md`, agreed by Kort; bug fixes only on `main` during P1 |
| P0.2 | Create the port branch and record the baseline: `verify.py fast` and `game` pass on 1.20.1 at the branch point | Baseline log with commit hash |
| P0.3 | Spike setup decision: choose where the spike runs (Kort's PC, Velocity optional at this stage) and which Polymer modules are in play | One short note in this plan |

### P1: Port to 1.21.1 (about 6 to 12 weeks, estimate)

| ID | Task | Acceptance |
|---|---|---|
| P1.1 | Toolchain: Java 21, Loom, Fabric API, mappings, `fabric.mod.json`, `verify.py` JDK check, `package.py`, server-kit, builder-server pins | `verify.py fast` runs on the new toolchain, even if mod code does not yet compile |
| P1.2 | Mechanical changes: `Identifier.of`, item NBT to components, `NbtIo` | Main sources compile |
| P1.3 | Networking: 18 channels to `CustomPayload` records with codecs, login-phase check redesigned for the configuration phase | Client and server exchange every payload in a dedicated-server test |
| P1.4 | Mixins and rendering: 24 mixins, custom engine, canvas UI, HUD hooks | Mod builds with `required: true` mixins applying; no mixin warnings |
| P1.5 | Resources and packs: model and item-override formats, `armory`, `weapons`, `gg_build`, 41 structures | Structures load; packs validate |
| P1.6 | Harness: `Phase2Checks`, standalone checks, `ggbot.py` for protocol 767, `builder_leave.py` | `verify.py fast` green |
| P1.7 | Real-client checks: Phase 2 suites (including `p2-turn-suite`) and the game tier on 1.21.1 | `verify.py game` green; Kort sees screenshots of the War Table and a battle |
| P1.8 | Content and saves: all 30 troops render with correct models and clips; a schema 1, 2 and 3 profile set loads unchanged | Contact sheet of 30 troops; migration tests pass; a 1.20.1 camp profile opens |
| P1.9 | Modpack dependencies: re-pin to 1.21.1 builds, re-confirm Excalibur permission and file, update `platform-lock.json`, `manifest.json`, `curseforge-ids.json` | Locked pack launches with shaders and pack on 1.21.1 |

Exit: `main` equivalent on 1.21.1 plays like 1.20.1; Kort signs off; freeze lifts.

### P2: Plain-client spike, go/no-go gate (about 1 to 2 weeks, estimate)

Scope: one battle on a dedicated server with a vanilla 1.21.1 client (no modpack): vanilla-mob troops by role (including husk-to-skeleton substitution), hotbar orders, boss bar, action bar and sidebar HUD, one chest menu (the Recruit page), and a server resource pack with at least one font glyph and one icon.

| ID | Task | Acceptance |
|---|---|---|
| P2.1 | Add Polymer to a spike branch; verify entity substitution, resource-pack hosting and client detection work on 1.21.1 | A vanilla client sees a skeleton where the server has a husk |
| P2.2 | Check custom registry entries (sound events, `supply_wagon`) against a vanilla client | A vanilla client joins without a registry error; sounds either play or fail silently |
| P2.3 | Orders from hotbar items through the server paths | An order issued by item produces the same effect, cooldown and range as the keybind order |
| P2.4 | Plain HUD and one chest menu | Screenshots of both |
| P2.5 | Report to Kort: what worked, what did not, rough cost of P3 to P5 | Kort's go, narrow or stop decision recorded in `DECISIONS.md` |

### P3: Mod-optional server (about 2 to 3 weeks, estimate)

| ID | Task | Acceptance |
|---|---|---|
| P3.1 | Handshake payload on join; per-player flag (`modded` or `plain`) with a short grace period | A modded client is flagged within the grace period; a vanilla client is flagged plain |
| P3.2 | Replace the login refusal: plain players pass; modpack clients with a wrong version or build are still refused with the download link | Three cases tested: plain, matching modpack, mismatched modpack |
| P3.3 | Guard every custom payload send (14 server send sites) and every server-driven client screen (lectern, camp stations) behind the flag | A plain client in a full session logs no errors and receives no custom payload |
| P3.4 | Per-viewer text: replace keybind text for plain players | No raw key identifiers in plain chat |

### P4: Plain front end (about 6 to 10 weeks, estimate)

| ID | Task | Acceptance |
|---|---|---|
| P4.1 | Mob disguises by role for all 30 troops, with dyed armour and banners for House colour; mapping table in the sheet or a plain-only table | A contact sheet of 30 plain troops; every role readable; Kort approves the table |
| P4.2 | Hotbar orders and swap-hand key: follow, move, attack, hold, charge, retreat, formations, groups, pings | Every order available to modpack players is available to plain players through the same server path |
| P4.3 | Plain HUD: boss bar, action bar, sidebar scoreboard from battle state | All information the match needs is present; screenshots |
| P4.4 | Chest menus: War Table pages (home, recruit, banner, contracts, chapter, friends, field, results, gear), camp stations, armoury | Each page reachable, each action available in the modpack is available here |
| P4.5 | Resource-pack build: item and gear icons, font glyphs, chest-menu art; no mob textures; hosted on the proxy with a hash; decline kicks the player | Pack validates; a decline is refused with a message |
| P4.6 | Camp and hub for plain players: station interaction, first-join nudge (hub sign plus one chat line linking the modpack, none in battle) | A new plain player can reach a battle from the hub without the modpack |

### P5: ViaVersion on Velocity and version matrix (about 1 to 2 weeks, estimate; waits on NET-1 proxy)

| ID | Task | Acceptance |
|---|---|---|
| P5.1 | ViaVersion on Velocity with the 1.21.1 backend; FabricProxy-Lite for forwarding | Plain client joins through the proxy |
| P5.2 | Test matrix: 1.21.1, one middle 1.21.x, and the latest release | Each version joins, plays a battle and opens a menu; known gaps listed |
| P5.3 | Older clients (below 1.21.1) get a clear refusal | Message tested on one older version |

### P6: Mixed-client QA (about 2 to 3 weeks, estimate)

| ID | Task | Acceptance |
|---|---|---|
| P6.1 | A battle with modpack and plain players on both sides | No crash, no desync, no console errors |
| P6.2 | Fairness check: cooldowns, order range and attack reach compared between client kinds from server logs | Identical values; any difference is a defect |
| P6.3 | Queues and parties mix both kinds | A mixed party queues and plays |
| P6.4 | Performance: plain players with many disguised entities | Server tick time within the Phase 7.2 target |

### P7: Modrinth and CurseForge modpack packaging (about 1 to 2 weeks, estimate)

| ID | Task | Acceptance |
|---|---|---|
| P7.1 | Build the 1.21.1 `.mrpack` and CurseForge export; listings text says the modpack is optional | Both artifacts install on a clean machine |
| P7.2 | Verify scripts (`verify_modrinth.py`, `verify_curseforge.py`) updated and passing | Green |
| P7.3 | Nothing is published. Kort publishes | Release record notes the pending go |

Total, if sequential: roughly 20 to 35 weeks (estimate). P3 to P5 overlap the beta and Phase 7 network work, so the added calendar time depends on how much runs in parallel.

## 6. Where it slots into `PLAN-TO-1.0.md`

- The port (P0 to P1) runs before the Phase 7 network work and is the dependency of everything on the network side. Phase 7 task 7.1 (prototype) should start on 1.21.1.
- The plain-client spike (P2) follows the port; P3 to P5 are built during the beta, which is modpack-only.
- Plain support is a gate for 1.0: the release checklist requires P6 to pass before launch.
- Phase 6 (release) packaging uses P7 output.

## 7. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Polymer gaps: entity substitution, custom registry entries or pack hosting do not behave on 1.21.1 as hoped (only the Modrinth summary was verified) | Plain support narrows or stops | P2 spike gate before the big build; a fallback of a spawn-packet rewrite without Polymer |
| Vanilla mobs cannot show 30 troops and 5 Houses readably (poses, bows, held items, colour) | Plain players cannot tell troops apart | Roles mapped to mobs with armour dye, banners and name colour; Kort approves the table in P4.1; a display limit is accepted for plain play |
| Mixin breakage: 24 mixins, several in rendering, armour, bow and damage code | Port runs long | Port the most fragile first, keep mixins small, test each with the real client |
| Double maintenance of two front ends | Every feature costs twice | Plain menus and HUD read the same server state as the modpack HUD; new features need both fronts in their acceptance checks |
| Fairness drift: a plain-only path gets different cooldown, reach or targeting | Players feel cheated | Orders go through one server path; P6.2 compares logged values |
| Port overrun with freeze | Content lanes idle | Time-box P1; allow bug fixes; agree the merge order in P0.1 |
| Dependency or licence gaps for 1.21.1 (CurseForge builds, Excalibur permission for the new file) | Pack delayed | Check early in P1.9; Excalibur permission is Kort's to confirm |
| Proxy does not forward login queries or the pack as expected | Detection or pack delivery fails | Use a play-phase handshake; test the proxy path in P5 |
| Registry sync: custom sound events and the wagon entity confuse vanilla clients | Plain client cannot join | Spike item P2.2; stand-in wagon and direct sound events if needed |

## 8. Facts not verified

- Polymer details beyond the Modrinth summary and the existence of 0.9.19+1.21.1.
- Loom version for 1.21.1, the Java 21 requirement (stated from knowledge), and the 1.21.1 signatures of all mixin targets.
- CurseForge builds of the modpack dependencies.
- ViaVersion's behaviour for a newer client joining an older backend (documented behaviour in general; not confirmed on its page).
- Whether Velocity forwards custom login queries.
- How a vanilla client handles our custom sound events and entity type.
- Excalibur permission for the 1.21 file.
