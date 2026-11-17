# Cosmetics render: preview and equip (2026-11-16)

Branch `feat/cosmetics-render`, worktree `C:/Users/kortt/Melty/bb-cosmetics-render`, from main `113995d71`.
Direction approved by Kort (status/DECISIONS-2026-11-15-port.md, 2026-11-16 night: "cosmetics rendering (preview and equip only)").
Kort is asleep. The details below were decided by the session and are logged as assumptions (section 9).
Out of scope: shop, gold, Tebex, the Wardrobe screen itself, and Madame Fleece. This branch only makes owned cosmetics draw and equip, so the Wardrobe can open later.

## 1. What the words mean in game

| Term in the task | Object in the game | Who draws it |
|---|---|---|
| Battle banner | The banner blocks a commander's army plants in battle: the supply banner (`ArmySupplyBanner.place`) and the Lord Marshal's Oath Banner (`Abilities.Banner`). | Vanilla banner block entity: patterns for everyone. Modded extras ride on the stand entity. |
| Command banner | The banner-family gear the commander holds (`war_banner`, `long_colours`; family `banner`, stand-in `minecraft:white_banner`). | The vanilla stack carries the patterns, so plain clients see them. `CommanderGearFeature` draws the patterns and extras for modded clients. |
| Title | `Progression.State.title` and `titles`, which exist already, with the `progress:title:` equip. | Modded clients get a line under the player's name tag. Plain clients get nothing, because the scoreboard team suffix is used by sides. |
| `skin:proven` | The Proven (Mastery II) veteran trim on GeckoLib troops: `veteran_t<tier>.png`, built by `tools/build_geo_trim.py`. | `GeoTroopRenderer.Trim`. Only modded clients have GeckoLib. |

The camp's command-tent standard pole and the old `MapDressing` command standards stay parked: they are old-map or camp work.

## 2. Catalogue (code, not sheet)

The catalogue lives in a new pure class, `Cosmetics.java`, which has no Minecraft types and is run by verify fast's standalone checks.
- Leaving it out of `sheets/design.json` keeps `Spec.java` from churning under three sibling branches.
- The reward strings stay in the sheet as they are. `Cosmetics` maps each one to an id.

| id | slot | name | source | vanilla layers (contrast dye = white, or black on a white field) | modded extra |
|---|---|---|---|---|---|
| `pennant` | pennant | Renown Pennant | Renown I (`cosmetic:pennant`) | `triangle_top` | swallow-tail streamer above the flag |
| `pennant_pattern` | pennant | Founders' Pennant | Founders tier 21 | `stripe_top`, `triangles_top` | two-tail streamer, gold |
| `trim` | trim | Renown Trim | Renown II (`cosmetic:trim`) | `border` | gold fringe along the flag's foot |
| `frame` | frame | Renown Frame | Renown III (`cosmetic:frame`) | `curly_border` | dark iron rails on the flag's sides |
| `banner_frame` | frame | Founders' Frame | Founders tier 29 | `curly_border` (gold, orange dye) | gilded rails |
| `frame_brass` | frame | Brass Frame | Mastery III (`frame:brass`) | `border` (orange dye) | brass rails |
| `standard` | standard | Renown Standard | Renown IV (`cosmetic:standard`) | `circle` | gilded crossbar and finial on the pole |
| `skin_proven` | skin | Proven | Mastery II (`skin:proven`) | none | the veteran trim on Proven troops |

- One item per slot. The slots are `pennant`, `trim`, `frame`, `standard` and `skin`. Titles keep their own existing slot.
- Vanilla layers are drawn in this order: field (the side dye), then standard, frame, trim and pennant. At most 6 layers, so vanilla never drops a layer.
- Ids are global, not per House. A Renown level is earned the first time any House reaches it.

## 3. Grants (idempotent, retroactive)

`Cosmetics.earned(maxRenown, maxMastery, foundersIds)` returns the earned ids. `CosmeticGrants.apply(Profile)`:
- adds them to `Profile.cosmetics`;
- runs in `Profile.sanitize` (every load, restore and migration) and again in `Campaign.finish` after the pay and before the save (Mastery moves there too), so profiles already past a threshold get theirs;
- returns the ids that are new, for a notice: "New cosmetic: Renown Pennant".

When `skin_proven` is first granted, it is auto-equipped. That keeps today's behaviour, where every Mastery II troop wears the trim.

## 4. Equipped state and save

- `Profile.cosmeticSlots`: a `Map<String,String>` of slot to id. The change is additive. (Built as `cosmeticSlots` because `Profile.equipped` is already the gear's slot-to-uid map.)
- `Cosmetics.normalize`, run in `Profile.sanitize`, drops unknown slots, unknown ids and ids that are not owned.
- Old saves load with an empty map, then `skin_proven` is auto-equipped if it is owned.
- Equip goes through the existing progress command path: `progress:cosmetic:equip:<slot>:<id|none>`, handled in `Campaign.progressCommand` beside `title:`. A locked id is refused. A saved title or equip change sends `cosmetic_v1` and re-dresses the held banner.
- The equipped state is also in the owner's snapshot, as `profile.cosmeticSlots` (the profile JSON), for the future Wardrobe.

## 5. Sync: `cosmetic_v1`

`CosmeticSync` copies the `RankSync` pattern: a raw channel that is sent only to clients that registered it, so a vanilla client never gets it.
- One packet per entity: VarInt entity id, then a short string list of `slot=id` pairs, then the title (an empty string means none).
- The entities it covers are players, and the stand entity of a planted banner, which takes its owner's set.
- It is sent:
  - on start-tracking;
  - on late channel registration;
  - to every tracker and to the player itself when the player equips;
  - when a cosmetic or title changes, also for each of the player's planted supply banners.
- It is also resent to the player himself after a respawn or a world change (his new client entity lost its entry).
- A client keeps the entries in `CosmeticSync.ENTRIES` (receiver in `CosmeticSync.Client`), drops one on entity unload and clears them on disconnect.
- The title `Recruit` (everyone's default) is sent as no title; `/gg cosmetics title <player> none` sets Recruit.
- It is a real `Payloads.CosmeticPayload` record registered in `Payloads.register()` (1.21.1 needs a payload type per channel), wrapping the raw bytes like `RankPayload`.
- `Cosmetics.previewOverride` lets the future Wardrobe screen draw the local player in an unequipped set, as `CommanderGearFeature.preview` does.

## 6. Drawing

- **Vanilla carriers (all clients):**
  - `Cosmetics.layers(slots, fieldDye)` returns pattern ids that are all `minecraft:` (order standard, frame, trim, pennant; at most six); `CosmeticBanners.component` looks them up in the `BANNER_PATTERN` registry.
  - **Banner block:** the planted banner (supply banner and Oath Banner) sets `BannerBlockEntity` patterns (`readFrom`), marks the block dirty and sends the block update. A change of set re-plants the commander's live banners (`CosmeticBanners.replant`).
  - **Held banner stack:** a 1.21.1 banner's colour is its item, not a component, so the server swaps the banner-gear stack to the banner item of the side dye (white outside a running battle), keeping every component, and sets `BANNER_PATTERNS`.
  - Bots have no cosmetics.
- **Modded extras:**
  - `CosmeticExtras` draws the streamer, fringe, rails and finial as small textured boxes. It uses one new 16x16 texture, `textures/entity/cosmetic/extras.png`, which is generated by `tools/build_cosmetic_extras.py`.
  - It is called from `CommanderGearFeature`'s banner draw. That call also draws the flag's patterns on the commander's own mesh flag, using the vanilla canvas.
  - It also draws on the planted Forward Supply banner, from a `WorldRenderEvents.AFTER_ENTITIES` pass over the stands that carry a `cosmetic_v1` entry. The Oath Banner has no stand entity, so it shows the vanilla patterns only.
- **Title:** a billboard line over the player drawn in the same `AFTER_ENTITIES` pass (no mixin), small and grey-gold. It is never drawn on your own player in first person, nor with the HUD hidden.
- **Proven:**
  - `Campaign.veteran` also needs the owner's `skin` slot to be `skin_proven`. Default is on, as above.
  - The trim follows the troop's name, set at spawn and level-up: turning Proven on or off mid-battle reaches the troops that spawn afterwards.
  - Add `art/geo_trim/<troop>.json` recipes and built overlays for more Geo troops through `build_geo_trim.py`, at least two beyond the knight (`--check` must pass). Built: sentinel, ic_hearthguard, gl_marshal.
  - The troops without a recipe are listed in PARKED.md.

## 7. Command `/gg cosmetics` (owners, existing `/gg` gate)

`CosmeticCommands` is registered from `AdminCommands` with a one-line hook:
- `list [player]`: owned and equipped.
- `grant <player> <id|all>`
- `revoke <player> <id>`: debug only.
- `equip <player> <slot> <id|none>`
- `title <player> <title|none>`: equip an owned title.

## 8. Files and collisions

- New files: `Cosmetics`, `CosmeticGrants`, `CosmeticSync`, `CosmeticBanners`, `CosmeticExtras`, `CosmeticCommands`, the harness suite `CosmeticChecks`, `test-tools/standalone/CosmeticsCheck.java`, and the extras texture and tool.
- One-line hooks in:
  - `Profile`
  - `Campaign`
  - `GravesAndGlory`: the supply banner place, the gear stack and registration
  - `Abilities`
  - `GeoTroopRenderer` or `Campaign.veteran`
  - `CommanderGearFeature`
  - `AdminCommands`
  - `GravesAndGloryClient`
  - `net/Payloads`: the `CosmeticPayload` record and its registration, as `RankPayload`
  - `GearModels` (forgets the cached flags on reload)
  - the `tools/verify.py` names list: `+ Cosmetics CosmeticGrants`
  - `Phase1Checks.SUITES`: `cosmetics`
  - `tools/release_config.py`: the jar leak guard refuses `CosmeticChecks` and `CosmeticsCheck`
  - `tools/where.py` and `tools/where_extract.py`: an `art/geo_trim/` recipe is labelled and ranked below a troop's models, so the new gl_marshal recipe does not push its geo model out of `where`'s art rows (`test_where`)
- Files that are not touched, because siblings own them: `sheets/design.json`, `Spec.java`, `PlainClients.java`, `CampScreen.java`, `WarTablePages.java` and `gravesandglory.mixins.json`. No new mixin.

## 9. Assumptions (Kort may overturn them)

1. The save gains `cosmeticSlots`, and the network gains `cosmetic_v1`. Both are additive and inside the approved cosmetics direction.
2. Cosmetic ids are global, not per House.
3. Proven stays on by default, so nothing changes for current players until they turn it off.
4. Plain clients see the vanilla patterns only: no titles, no extras and no trim.
5. The vanilla pattern choices and the extras' colours are placeholders until Kort's art review.
6. Client harness runs go through `gradle_slot.py` with the client slot root, as the coordinator asked, rather than `checkq`.

## 10. Checks

- `python tools/preflight.py` must show Spec.java unchanged. Save the hashes.
- `tools/verify.py fast`, through `gradle_slot.py`. Fast includes `CosmeticsCheck`, which covers:
  - grants: idempotent and retroactive;
  - normalization;
  - the equip rules;
  - layer order and cap;
  - only `minecraft:` ids in the vanilla layers.
- Client harness suite `cosmetics`. It asserts the draw probes and saves one screenshot per cosmetic:
  - the held banner with each slot;
  - a planted banner;
  - a title line;
  - a Proven Geo troop on and off.
  
  Shots go to `work/ingame-check/cosmetics/run/screenshots/cosmetic-*.png`.
- Plain safety:
  - every send passes `VersionGate.canSend`; skipped sends are counted (`CosmeticSync.skippedPlain`). A one-client harness cannot show a plain client's view, so this rests on the gate and the counter, not on a plain-client run;
  - the item stack and block components carry only vanilla ids.
- One Opus high review, with at most two fix rounds.
