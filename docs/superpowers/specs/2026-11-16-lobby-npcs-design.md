# Lobby NPCs: design (draft)

Date: 2026-11-16. Status: DRAFT for the grill-me interview with Kort. Nothing here is approved or built.
Brief: `Melty/status/brief-lobby-npcs-goblin.md`. Branch `design/lobby-npcs-goblin` from main 616b2705.
Companion: `2026-11-16-goblin-dealer-design.md` (the goblin's rules; its character is in section 4.11 here).
Open decisions are marked **OPEN Qn**. The numbering is shared with the goblin spec, and the grill asks them in order.
Lines marked **(call)** are my proposals, and Kort can overrule any of them.

## 1. Intent

Kort wants players with the modpack to see a custom character at every lobby NPC. Plain (vanilla) players see a villager. His example: the forge NPC is a big, burly blacksmith. The characters follow the troop look: hand-authored at 1 texel per model pixel, rigged and animated on GeckoLib in the style of the knight reel.

## 2. What exists today

**Stations and NPCs.** NPCs are rows of `campStations` in `sheets/design.json`. `CampWorld.npc` spawns one `VillagerEntity` per row whose `npc` is not `-`. It sets AI off, invulnerable, silent and persistent, a profession at level 5, and a hidden custom name (the label text before ` / `). It adds the tags `gravesandglory-camp…` and `camp:<page>`. Right-clicking the villager within 6 blocks opens its station (`GravesAndGlory` UseEntityCallback). A click on the station's camp piece opens it too (`CampWorld.station`). The code never sets the villager type, so every NPC shows the plains skin.

| Where | Station row | Page | Villager profession today |
|---|---|---|---|
| main (Banner Camp) and Old Hold | quartermaster | army | armorer |
| main and Old Hold | archivist | contracts | librarian |
| main and Old Hold | steward | friends | cartographer |
| main and Old Hold | drillmaster | train | weaponsmith |
| main and Old Hold | blacksmith | forge | weaponsmith (same as drillmaster) |
| `feat/oldhold-wiring` only | house-gilded | recruit:gilded | armorer |
| Old Hold only | house-graveborn | recruit:graveborn | cleric |
| Old Hold only | house-wildmoot | recruit:wildmoot | leatherworker |
| Old Hold only | house-ironclad | recruit:ironclad | toolsmith |
| Old Hold only | house-tidecallers | recruit:tidecallers | fisherman |
| new (goblin spec) | goblin | loot | none yet |

Rows with no NPC: welcome, founders, the gates, war-gates, arsenal (Old Hold). The Old Hold is not merged, and it waits on Kort's walk (Q129).

**How troops get custom looks.** Troops are vanilla mobs on the server (husks, skeletons and so on). On a modded client, `TroopRenderers.over(type, vanilla)` wraps each vanilla renderer in `TroopRenderer`. That renderer resolves the mob by its custom name (`TroopModels.resolve`) and draws it through `GeoTroopRenderer` (a GeckoLib `GeoReplacedEntityRenderer`). A mob it does not recognise falls back to the vanilla renderer. No new entity type is needed, and no new packet is needed for the identity.

**Plain clients** (`status/plain-spike-p2-report.md`, Kort's NARROW decision on 2026-11-16):
- A plain client sees the server's vanilla mob, or a role-based substitute.
- Custom sounds reach plain clients by name through Polymer. The spike client was muted, so they resolved but were not heard.
- A new entity type is the risky case: Polymer marks entries server-only for every client, and a modded client on a Polymer server was never tested.
- Plain support at first covers battles only (troops, orders, HUD, Recruit). Lobby station screens for plain players are not in the first plain release.

## 3. Shared technical approach

### 3.1 Recommended: server villager, client GeckoLib replacement (OPEN Q3)

- **Server: no new entity.** The server keeps spawning a `VillagerEntity` per row. Two new sheet columns:
  - `look`: the character id, or `-` to stay a villager for everyone.
  - `villagerType`: plains, desert, savanna, snow, swamp, taiga or jungle. `CampWorld.npc` then sets the type as well as the profession.
- **Client: identity by name.** The modded client recognises a camp NPC by its custom name, as troops are recognised. The name is synced by vanilla. Command tags are not sent to clients, so they cannot be used.
- **Client: renderer.** `TroopRenderers` gains `over(EntityType.VILLAGER, VillagerEntityRenderer::new)` with a `CampNpcRenderer` wrapper. It draws known NPCs through a `GeoReplacedEntityRenderer<VillagerEntity, GeoCampNpc>` and every other villager (campaign maps, villages) through vanilla.
- **Why this approach:**
  - No registry entry and no new packet.
  - A plain client gets the villager for free.
  - The Polymer mixed-client registry problem is never touched.
  - It is the pattern the troops already prove in game.
- **Idle animation.** It runs on the client:
  - a looping idle, plus a random fidget every 6 to 14 s;
  - the head turns toward the nearest player within 8 blocks, as a client-side bone offset.
- **Interact animation.**
  - When a player opens a station, the server calls `swingHand` on that villager. Vanilla sends the entity-animation packet to every client tracking it.
  - The modded client maps that swing to the NPC's interact clip, so watchers see it too. Again no new packet.
  - On a plain client the swing does nothing visible (risk 6.3).
- **Interact clips per station.** The clicking player's own client also knows which screen opened. That lets the forge pick its roll clip and the goblin its board beckon, without a packet.
- **Click area.** The villager box (0.6 x 1.95) stays the click target, along with the station's piece zone.
  - The blacksmith (taller) and the goblin (smaller) get a vanilla `interaction` entity sized to the model. That entity type is vanilla since 1.19.4, so plain clients accept it.
  - **(call)** Add it only where the model is far off the villager box.
- **Rigs and art pipeline.**
  - Humanoid NPCs start from the knight biped rig (elbows, knees, the GeckoLib bone set). Each one gets its own proportions, outfit bones and props.
  - Art: `art/hand` builders, 1 texel per model pixel, faces at 2. Weapons and props follow the weapon rule of 2.
  - Animation: the knight reel's style and `docs/anim-rebuild/STYLE.md`, with weight shifts, anticipation and overshoot. NPCs need no combat set, only idle, fidget, interact and a talk loop.
  - Review: `render_geo` reels, the review booth, then one Sonnet high review per change, then Kort.

### 3.2 Alternatives considered

| Option | For | Against |
|---|---|---|
| A. Villager plus client GeckoLib replacement (recommended) | No registry or protocol change; free villager fallback; same path as troops | Identity hangs on the custom name; the villager hitbox (mitigated by an `interaction` entity) |
| B. New `gravesandglory:camp_npc` entity type, a Polymer virtual entity shown as a villager to plain clients | Clean identity, own hitbox, own AI | Hits the untested Polymer and modded-client registry problem; a modded client may get the wrong type, as the wagon likely does |
| C. Display entities (item or block displays with resource-pack models) | Works on plain clients with the pack | No skeletal animation; poses would have to be streamed from the server; plain players would also need the pack art |

### 3.3 Voices (OPEN Q14)

- **The rule:** audio is free-only, CC0 or CC-BY, and generated audio counts only if it is good. No free source gives spoken lines that would pass, so voice "lines" are text.
- **Proposed:**
  - Each bark is a short line shown in a speech scroll over the NPC, for the clicking player only. It plays with a non-verbal vocal cue: a grunt, a hum, a bony rattle or a gurgle.
  - Cues come from the kept voices (forge, bone, sea, beast) or new CC0, CC-BY or Stable Audio takes. Kort rates them, as in Batch 1.
  - Lines are drawn at random from the character's set and never repeat twice in a row. **(call)** At most one line every 20 s per player per NPC.
- **Line rules:**
  - Lines never name the giants (Act I never names them). They may hint ("the old stone").
  - Lines never state a troop or banner number limit.

### 3.4 Plain clients (OPEN Q6)

- A plain player always sees the villager, with the profession and skin in section 4. That needs no work beyond the `villagerType` column.
- What the click does is the open part. Under the narrowed plain scope, station screens are not built for plain players at first.
- **(call)** At first, a plain player's click on a staff NPC gives one chat line: the character's name and "this station needs the Graves and Glory modpack", with the download link the version gate already uses.
- The Recruit chest menu is the likely exception: P2 built it on the unmerged `spike/plain-p2` branch, and it is in the narrowed scope. If it lands, the House hall recruiters (Old Hold) can open it for plain players.

## 4. The characters

These follow the House-tied staff proposal (OPEN Q4): each camp station is run by someone from one House, so all five Houses have a face in the hub. Under the neutral alternative, every staff member is a human of the Gilded realms, and the House details drop away.

Sizes are relative to a villager or player (about 1.95 blocks; OPEN Q16).

### 4.1 Blacksmith (forge and repairs): Ironclad smith, "Ingot" Haldra (name a placeholder)
- **Silhouette and size:** big and burly, about 1.3 x villager height (about 2.5 blocks) and nearly twice as wide at the shoulders. A barrel chest, a short neck and huge forearms. He reads at 40 blocks as a block of a man.
- **Outfit and props:**
  - a scorched leather apron over bare, scarred arms;
  - the left forearm is a riveted brass-and-redstone graft, which glows dull red when he strikes (Ironclad: redstone in giant bone);
  - soot, a braided beard tied with an iron ring;
  - a sledge hammer and tongs, and an anvil with a glowing billet beside him (an anvil prop exists in the prop kit's forge art).
- **Idle:** two-beat hammer strikes on the billet with a full-body hip drop, about every 3 s; sparks. He wipes his brow with the graft arm.
  - **Fidgets:** quenches in a bucket (steam), rolls his shoulder, inspects the billet against the light.
- **Interact:** lays the hammer across his shoulder, turns, leans on the anvil toward the player.
  - **Forge roll:** a big overhead strike that lands as the roll is sent.
  - **Repair:** two quick taps.
- **Lines:**
  - "Steel remembers every hammer. So do I."
  - "Broke it again? Course you did. Hand it here."
  - "Bring me shards, not stories."
- **Vanilla:** toolsmith (apron, reads as a smith), taiga skin. This also ends the weaponsmith clash with the drillmaster.

### 4.2 Quartermaster (army and shop, the War Table banner): Gilded sergeant-clerk, Sergeant Wynn
- **Silhouette and size:** player height, stout and square, very upright. Reads as "counter and ledger".
- **Outfit and props:**
  - a Gilded tabard in gold and white over mail, ink-stained cuffs;
  - a quill behind the ear;
  - a ring of keys on the belt;
  - a ledger board in the left hand;
  - stacked crates with stencilled House marks beside him.
- **Idle:** ticks entries in the ledger, counts crates with the quill, taps a crate.
  - **Fidgets:** licks a thumb and turns a page; squints at a crate's stamp.
- **Interact:** snaps the ledger shut, a sharp half-salute with the quill hand, then holds the ledger out.
- **Lines:**
  - "Every soldier counted, every coin accounted."
  - "Few and fierce, or many and cheap. Your purse, your call, Commander."
  - "Sign here. And here. Not there."
- **Vanilla:** armorer, desert skin.

### 4.3 Archivist (daily contracts): Graveborn scribe, "the Clerk of Bones"
- **Silhouette and size:** tall and thin, 1.1 x villager height, stooped forward. A skeleton in a robe. Reads as a long, bent line.
- **Outfit and props:**
  - a moth-eaten archive robe, a skullcap with a candle stub fixed to it;
  - a scroll case on the back;
  - long bony fingers;
  - a lectern of stacked scrolls.
  - A faint violet-white glimpur glint in the eye sockets: the binding that frayed.
- **Idle:** writes with a quill, blows on the ink, the candle flickers.
  - **Fidgets:** the jaw clacks as he reads under his breath; he rolls a scroll; dust falls from a sleeve.
- **Interact:** unrolls a scroll toward the player with both hands, head tilted.
- **Lines:**
  - "I wrote orders once. Contracts are kinder."
  - "Today's work, Commander. Sign in ink, not blood."
  - "I remember everything. That is the problem."
- **Vanilla:** librarian, snow skin (pale).

### 4.4 Steward (parties and rooms): Tidecaller host, Mother Brine
- **Silhouette and size:** player height, round-shouldered, with a wide-hemmed coat that flares at the bottom (a bell shape).
- **Outfit and props:**
  - a long sea-coat with shell buttons;
  - kelp hair tied up with a fishbone pin;
  - a conch on a cord, used to "call" friends;
  - a hanging lantern with a blue glow;
  - a host's list on a slate.
- **Idle:** polishes the conch on her sleeve, holds it to her ear and listens.
  - **Fidgets:** the gills flare; she adjusts the lantern; she waves at a passer-by.
- **Interact:** raises the conch and blows a short note (a kept Tidecaller conch cue), then opens a hand.
- **Lines:**
  - "The tide brings friends. Mostly."
  - "A room for six? I'll call the shoal."
  - "Wipe your feet. You're dripping on my floor, and I'm the fish."
- **Vanilla:** cartographer, swamp skin.

### 4.5 Drillmaster (battle 1 and practice): Wildmoot ram, Sergeant Hornbreak
- **Silhouette and size:** about 1.15 x villager height, with horns curling past the shoulders. Heavy hooves, a forward-leaning stance. The horns read at range.
- **Outfit and props:**
  - an upright bighorn ram;
  - a padded drill jerkin and a scarred hide;
  - a quarterstaff with a brass end-cap;
  - a bone whistle;
  - training dummies in the yard (they exist).
- **Idle:** paces three steps and back, slaps the staff into his palm, glares at the dummies.
  - **Fidgets:** a hoof stamp; a short snort; tests a dummy with the staff (a thwack).
- **Interact:** points the staff at the yard, stamps once and puffs his chest.
- **Lines:**
  - "Again! The dummy is winning."
  - "Hold the line. Then hold it longer."
  - "You'll sweat here or bleed out there. Choose."
- **Vanilla:** weaponsmith, savanna skin.

### 4.6 to 4.10 House hall recruiters (Old Hold only; OPEN Q8)
One per hall door. Each recruiter reuses his House's troop rig and palette, with a recruiter's prop and idle. All five share one interact: presents the House banner and gestures at the hall.

| House | Character | Silhouette and size | Outfit and props | Idle | Lines (2 of 3) | Vanilla |
|---|---|---|---|---|---|---|
| Gilded | A herald | Player height, upright | Tabard, a trumpet with a banner, a scroll of names | Unrolls the roll, reads, nods | "The Charter needs swords." / "Sign, and be remembered." | armorer, plains |
| Graveborn | A bone-sexton | Tall and gaunt | Grave-robe, lantern, shovel | Leans on the shovel and taps the ground | "We were bound. Now we choose." / "Dig in with us." | cleric, snow |
| Wildmoot | A den-mother (a boar) | Low and wide, 0.9 x | Hide wraps, tusk charms, a carved staff | Sniffs the air and paws the ground | "The herd remembers who hunted it." / "Run with us." | leatherworker, savanna |
| Ironclad | A foreman | Square, 1.1 x | Goggles, a redstone graft, a clipboard plate | Taps a gauge and adjusts the goggles | "Good steel, good pay, no questions." / "Mind the sparks." | toolsmith, snow (the blacksmith is toolsmith, taiga) |
| Tidecallers | A purser | Player height, finned | Captain's coat, a coral pistol-shell | Rocks heel to toe and checks the tide on a shell | "The sea is calling. Answer." / "Sign the roll. Mind the salt." | fisherman, swamp |

### 4.11 The goblin dealer (rules in the goblin spec; identity OPEN Q7)
- **Silhouette and size:** small, about 0.6 x villager height (about 1.2 blocks), hunched. Huge ears and a long nose. A pack of junk on the back that is taller than he is. The pack is the silhouette.
- **Outfit and props:**
  - a patched coat with too many pockets, coin pouches and a jeweller's loupe;
  - a shard of violet-white crystal on a string;
  - three chests on a little handcart (small, medium and large, Kort 2026-11-16), which double as the shop display;
  - a lockpick roll and an oversized mallet in the pack, for opening (c).
- **Idle:** counts coins, bites one, peeks into the pack.
  - **Fidgets:** polishes the loupe; tosses a coin and catches it; glances at the altar.
- **Where:** his stand sits right next to the drop altar (Kort 2026-11-16).
- **Interact:** rubs his hands and slides the chosen chest forward.
  - **Staked buy:** the board drop happens first; then he walks to the chest.
  - **Jackpot (x10):** a little dance.
- **Openings (Kort 2026-11-16).** One of three plays at random, and the same performance plays for 1, 3 or 5 chests:
  - **(a) the kick:** a big, satisfying chest kick;
  - **(b) the mimic:** he dives into a mimic chest, it chomps, and he pops out with the loot. The idea comes from Frieren's gag; the art and motion are original;
  - **(c) the lockpick:** the lockpick snaps, then he smashes the chest with a huge mallet.
- A player can skip an opening only after seeing it once. The details are in the goblin spec, 3.1.
- **Clips needed:** idle, 2 fidgets, the interact, the walk, three openings (each with its chest or mimic prop rigged; the mimic is its own small rig), and the dance.
- **Lines:**
  - "Gold for a box. Box for a maybe."
  - "Pay a bit more and the old stone listens."
  - "No refunds. Goblin law."
- **Vanilla:** see goblin spec OPEN Q6 (proposed: a baby swamp-skin villager, which reads small and green).

## 5. Data and code (for the plan, not built)

- **Sheet:**
  - `campStations` gains `look` and `villagerType`;
  - a new `campNpcs` table holds per character: rig, texture, scale, interaction box, lines and the bark cue;
  - `Spec.java` is regenerated by preflight.
- **Server:**
  - `CampWorld.npc` sets the type, adds the optional `interaction` entity and calls `swingHand` on station open;
  - the plain-click chat line (Q6).
- **Client:** `CampNpcRenderer` (wrapper), `GeoCampNpc` (animatable), the bark scroll and cue, and the head-tracking bone.
- **Checks:**
  - a resolve check (every row's name maps to its look, and an unknown villager draws vanilla);
  - a harness shot per NPC (idle and interact);
  - a plain-bot check that no unknown entity or sound id is sent from the lobby.
- **Port:** built on 1.21.1 after the port lands, on whichever GeckoLib 1.21.1 build the port pins (not checked here).

## 6. Risks

1. **Name as identity.** A rename breaks the look (it falls back to the villager, so nothing crashes). Players could rename an NPC with a name tag; the use handler should eat name-tag use on camp NPCs. Localising names later needs a stable id in the name or a sync.
2. **Hitbox against model.** A 2.5-block smith with a 1.95 box causes missed clicks. Mitigated by the `interaction` entity, which is new to the camp and needs a harness check.
3. **The swing as the interact signal.** Verify the villager swing packet reaches trackers on 1.21.1, and that the vanilla villager model ignores it.
4. **Renderer wrap on every villager.** The wrapper costs one name lookup per villager per frame. That is trivial, but it must fall through to vanilla exactly, including baby villagers and campaign map villagers.
5. **Animation capacity.** Kort moved all troop animation to the animation-skill session. Eleven new rigs compete with the troop booth queue (OPEN Q8).
6. **Lobby churn.** The Banner Camp and the Old Hold differ, and the Old Hold waits on Q129. Station rows move with the lobby, and characters do not depend on the layout.
7. **Plain players.** Clicking an NPC that does nothing feels broken; the chat line covers the first release (Q6).
8. **Lore.** The House-tied staff and the goblin need the lore lead's pass. No line names the giants.
9. **Audio.** Vocal cues need Kort's ears; text lines carry the character until cues pass.

## 7. Open decisions in this spec

- **OPEN Q3:** render approach (section 3.1).
- **OPEN Q4:** House-tied staff or neutral humans (section 4).
- **OPEN Q6:** plain-client behaviour for the NPCs and the goblin (section 3.4; shared with the goblin spec).
- **OPEN Q7:** the goblin's identity in the lore (section 4.11; shared).
- **OPEN Q8:** scope and order: which lobby, pilot first, the hall recruiters, who builds.
- **OPEN Q14:** voices (section 3.3).
- **OPEN Q16:** sizes and click boxes (section 4).

Q1, Q2, Q5, Q9, Q10, Q12, Q13 and Q15 are in the goblin spec (Q11 was decided by Kort on 2026-11-16).
