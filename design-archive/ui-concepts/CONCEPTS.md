# UI and HUD concepts for Graves and Glory

Nine mockups at 1920x1080, sized for GUI scale 2 (one authored pixel is two screen pixels; text is Minecraft's own glyphs plus Lato and Marcellus rasterised at 1x as the game does). HTML is the source; `render.py` rebuilds the PNGs; `index.html` is the gallery.

## HUD directions

**A. Marshal's Glass**: *the field through a spyglass; strokes and numerals, no boxes.*
A compass strip on top carries bearings to A/B/C and allies (minimap on demand). One tug line for the score: Dawn grows from the left, Dusk from the right, the centre is 300. One pip per troop per row; a loss snaps a pip white. A rout tilts the pennant glyph and echoes a horn at the screen edge. Gives the centre and right edge back to the battle. Trade-off: least fantasy texture.

**B. Iron Ledger**: *a quartermaster's ledger bolted to the screen.*
Iron bars, vellum slips, wax seals. Scaled numerals on a torn vellum strip; points as seals, hatched when contested. Group cards carry ink health bars; a loss blots ink; a rout is a pre-rotated ROUTED stamp. Vellum map corner, dispatch-ribbon notices, a brass seal under an iron shutter for the ability. Best match to today's charcoal-and-brass camp. Trade-off: most textures, most screen taken.

**C. Banner Line**: *your groups are standards planted in front of you.*
Diegetic. Cloth height is men (one stripe per troop), width is health, the finial is the order, a dot tile is the formation. A loss tears a white stripe; a rout leans the pole and the cloth hangs small and dull, horn ringing beside it. Two great banners fill toward 300 with the point shields between; the ability is the commander's own standard, cloth climbing as the cooldown runs; the minimap is a shield boss. House glyphs are woven into every cloth. Most original. Trade-off: health-as-width must be taught.

## Camp UI directions

**A. War Table**: *the commander's tent from above the table.* Oak, parchment, ink, brass. Home is an inked map of the five fields with lead tokens for Practice, Skirmish and Campaign; friends are letters, contracts wax seals, the chapter a ribbon, the banner cloth on a stand. Recruitment is a muster roll under five House banners; the roll is a sealed letter; a duplicate stamps the second star. Palette change argued by vision section 9: the camp has a war table, so the UI is that table.

**B. Field Ledger**: *the charcoal we have, given a spine.* Keeps DarkUi tokens and Marcellus; adds a spine of brass index tabs, three big fight plates, map seals as cards, the banner as the tallest card (the player's expression), party and room beside it, one primary action. Recruitment: House tabs with colour underlines, portrait cards with counters, stars and evolve cost, a reveal card in House colour.

**C. Hall of Banners**: *heraldry is the interface.* Five House banners hang from a beam as navigation; your banner hangs centre with the 20 slots woven in House cloth; party banners hang beside yours, the room's opposite; the fights are shields on the wall. Recruiting unfurls the chosen House full height while the herald presents the card. Boldest identity; most likely to fight the live camp behind it.

## Recommendation

**HUD: C, Banner Line, with A's compass strip and tug line as the top band.** Readable drama is pillar one: a line that breaks should look like one, and a leaning standard with a horn says it before a word is read. House glyphs on the cloth serve "your banner is yours" all fight long. B's vellum stays for the notice ribbon and the match report.

**UI: B, Field Ledger, dressed with A's objects.** The camp is being rebuilt as a real war camp with recruiter tents, so a flat table drawn over it fights the world. B keeps the existing tokens, ships in weeks, and gives the banner the room it deserves. Borrow from A the letter of enlistment, the stamped star, wax seals and inked map seals. Keep the Hall's hanging-party idea for the room screen.

Colour-blind and scale: team is always shape plus colour (sun, crescent, hatching when contested, a tilt when routing); 1 px grid and 9-slice panels hold at scale 3 on 1440p.

## Assets each would need

- **HUD A** (about 12 small textures): compass tiles, marker shield in 3 states, pip, horn, ring tile, 6 order and 3 group glyphs. Code: compass from yaw, pip rows.
- **HUD B** (about 25): iron and vellum 9-slices with torn caps, seals in 5 colours plus cracked, crests, hourglass plaque, ROUTED stamp, ink blots, brass seal, map rose. Code: 2x numerals.
- **HUD C** (about 30): pole, 6 finials, cloth 9-slice per team and House, 3 lean frames, tear stripe, 5 formation tiles, great banners, point shields, bell, boss rim, commander standard. Code: cloth sizing from count and health, lean animation, standard raise.
- **UI A** (about 30, new layouts everywhere): oak tile, parchment 9-slice, 5 banner tabs, token base, 8 map glyphs, letter, seal, ribbon, candle, brass button.
- **UI B** (about 12, re-layout of existing screens): panel and tab 9-slices, chip, 5 map seals, House underline, portrait frame, card backs, star.
- **UI C** (about 20 large, new screen system): beam, 5 House banners full and furled, your-banner cloth per team, party banner, fight and roll shields, herald card.

## Third pass (v3): War Table and Iron Ledger

Kort picked one direction: the **War Table** for the camp (home and recruit) and the **Iron Ledger** group cards for the battle HUD. v3 polishes only those three screens; the other directions stay at v2.

- `v3-hud-iron-ledger.png`, `v3-ui-war-table-home.png`, `v3-ui-war-table-recruit.png`, with the HTML source beside each.
- `python render.py --all-v3` renders and lints them. Result: 0 lint defects and no text run under 4.5:1 on all three (`v3-check.json`).

### How v3 is made

Every texture is pixel art, painted by script at GUI scale 1 into `v3/` and placed 1:1; `render.py` doubles it.
- **Painting:** `python v3art.py [names]`.
- **Modules:**
  - `v3lib.py`: sprite canvas, noise, outline and shadow.
  - `v3pal.py`: hue-shifted ramps for oak, iron, brass, vellum, ink, wax, teal and violet.
  - `v3a_table.py`: rail, plaques, icons, portrait frames, buttons and key caps.
  - `v3a_paper.py`: parchment, ink drawings, wax seals and the Rivergate map.
  - `v3a_recruit.py`: House banners, muster roll, stars, ornate frame, letters and purse.
  - `v3a_hud.py`: ledger cards, iron corner caps, score bar, bosses, seals, glyphs, the ability slot and its sweep.
- **Light:** always from the top left. Shadows are cool and lights warm, with two or three tones per material and small highlight clusters. There is no pillow shading and no banding.
- **Text:** stays live. Minecraft glyphs are used for numbers, keys and short labels; Marcellus and Lato are set at 11 px and rendered at 1x. The v2 grid, lint and three type sizes still apply (`<html class="v2 v3">`, `v3.css`).

### Portraits are live model renders

Every portrait frame is a window for the game's own model render: `InventoryScreen.drawEntity`, scissored to the window.

| Where | Window | Frame |
|---|---|---|
| Muster roll | 32 x 32 | `frame-rim-40` |
| Party seats | 36 x 36 | `frame-rim-44` |
| Reveal letter | 160 x 160 | `frame-ornate-184` |

- The rim overlaps the window by 1 px, so the scissored edge of the model never shows.
- The slate backdrop behind the model (`frame-back-*`) is a texture, lit behind the head.
- Party seats show each player's own skin.
- **The pixel busts inside the frames are placeholders until the unit identity pass delivers each unit's look.** They were not polished in v3.

### Hunger

Hunger is gone. There is no hunger row in the HUD mock, and nothing moves into that slot yet. Vanilla hearts, XP bar and hotbar are drawn from the game's own sheets, as they are today.

### What changed against the v2 deductions

**HUD**
- **Unlabelled icon pairs:** every order and formation glyph now has its word beside it: Attack and Shieldwall, Hold and Line, Follow and Compact.
- **Cooldown shown as a bare number:** the Rally Horn sits in an iron slot with a vellum window.
  - A dark sweep covers the 48 of 90 seconds still to run. It runs clockwise from twelve, with a bright leading edge, and reads as an angle before the number is read.
  - The seconds sit on an iron tab beside the slot, and the H key is on a brass plate.
- **Contested point read only as a square frame:** B is now a square split teal and violet, with two swords crossed behind it. A stays a round teal seal and C a violet diamond, so shape carries the state without colour.
- **The top bar lost the iron:** the score bar is now a ledger card clamped by two riveted iron bands.
  - It sits between two enamel bosses: Dawn is round with a gold sun, Dusk is a diamond with a silver crescent.
  - The three seals hang from the bar on chains.
  - The notice is a ledger slip with iron corners, clipped to the top of the spine and out of the middle of the view. The HUD now reads as one iron-and-vellum object.
- **The other states:**
  - A loss puts a red rule round the card and turns the lost stretch of the bar red.
  - A rout greys the card, stamps ROUTING, reddens the count and fades the glyphs.
  - The selected card is brass-bound, hangs from a brass hook and carries the order keys.

**War Table, home**
- **Nav tabs floating over a sky band:** the top of the screen is now the table's raised back rail. The camp's sections are brass-framed oak plaques screwed to it, with hand-drawn icons and wax-drop counts.
- **Black Change button:** it is now a vellum button with a quill, in the parchment family.
- **Order sheets:** these and the field card use the same edge-aged parchment as the recruit letter, so the ink drawings sit on a calm middle.

**War Table, recruit**
- **Right column not on one grid:** it is now one centred axis at x 784. From top to bottom: the sealed letters, the Epic counter, Odds, the purse, then Enlist with its cost, with 12 px between each. The three columns share one top (124) and one bottom (492).
- **Wax seal sitting on the star row:** the star seal has moved to the foot of the letter, so the tier reads as two stars of three. The broken House seal lies above the letter.
- **House tabs:** the five Houses are woven banners with embroidered devices, hanging from a brass rod on the rail. The chosen House hangs longer and brighter.

### Rule changes (amends DESIGN-RULES.md)

- Order and formation words are always shown. In v2 they appeared only on select, and the glyph alone did not read.
- The selected group card shows its order keys: G Attack, V Follow, B Formation. Orders go to the selected group, so that is where the keys are useful. All other battle key hints still appear only while Alt is held or in the first minute.
- The ability's H sits on its slot, because keys sit inside what they trigger.

### How it builds in the game

- **Ledger card:** a 9-slice. The four iron corner caps are the fixed corners; the edge-aged vellum tiles the edges and the middle. The loss, selected (brass) and routed (grey) states are three more texture sets of the same slice.
- **Score bar:** a 3-slice, with the iron bands in the end pieces.
- **Point seals:** one sprite per state (Dawn, Dusk, contested, neutral). The bosses are two sprites.
- **Bars:** each is a 3-slice channel, with a fill stretched from a one-pixel column.
- **Cooldown sweep:** a translucent triangle fan drawn each frame (BufferBuilder, position and colour), or 24 pre-baked frames.
- **Fixed-size textures:** banners, letters, the roll, frames and the brass button.
- **9-slices:** the plaques and the vellum buttons.
- **Keys:** the War Table keys are brass caps cut to the glyph width (13 to 48 px). The HUD keys are ink chips.

### Not mocked yet

- The "not enough gold" state on Enlist.
- The battle state with Alt held, where every card shows its keys.
- The point names, which appear when a seal is selected.

## Fourth pass (v4): two to three times the detail

Kort graded v3 "getting better but not quite there yet" and asked for double or triple the detail. v4 keeps the same three screens and the layouts that worked. It repaints every surface at full pixel density, adds the mode subtitles and the battle feed, and fixes the v3 deductions.

- `v4-hud-iron-ledger.png`, `v4-ui-war-table-home.png`, `v4-ui-war-table-recruit.png`, with the HTML source beside each.
- `v4-detail-crops.png`: four crops taken 1:1 from the 1920 x 1080 renders (the map card, a wax seal, an iron corner, a mode drawing), so the detail can be judged at the size the game shows it.
- **Build:**
  - `python v4art.py [names]` paints the sprites into `v4/`.
  - `python render.py --all-v4` renders and lints the three screens.
  - `python v4detail.py` composes the crops.
- **Result:** 0 lint defects and no text run under 4.5:1 on all three (`v4-check.json`). The HUD has 45 words (50 before v4.1), home 48 and recruit 40, with the same type sizes as v3.

### v4.1: two fixes

v4.1 fixes two pieces that still read wrong. It overwrites the v4 files and keeps their names.
- **HUD battle feed:** v4 drew it as a plain dark translucent box, which read as vanilla chat. It is now a leaf of the same ledger as the cards: a narrow vellum slip, bound in riveted iron down its right edge, with one riveted bracket on its free corner. The mock shows five lines, not seven. See the battle feed notes below.
- **Home map:** the map corner under the rail was a large, flat grey rectangle, which read as unfinished. It is now the campaign map rolled and tied with a red ribbon, its far end pushed under the rail and its curl showing at the near end. See the home notes below.
- **Lint:** still 0 on all three screens, with no text run under 4.5:1.

### How v4 is made

The method is the same as v3: pixel art painted by script at GUI scale 1, placed 1:1, and doubled by `render.py`; the text stays live. The ramps are longer, so each material has five or six working tones plus a highlight, with edge wear.

Modules register sprites by name, and a later module overrides an earlier one:
- `v4lib.py`, `v4pal.py`: the canvas and the ramps. Oak and vellum have nine tones each, iron nine with a cold specular, brass eight plus a warm specular, wax seven and ink six.
- `v4art.py`: the oak table. Each plank has cathedral figure, late-wood lines, pores and knots with check cracks. Butt joints carry domed nails bleeding rust. There are scratches and dents, and a stepped vignette.
- `v4a_paper.py`: parchment and wax seals.
  - The parchment has a torn-edge pass (deckle, V tears, loose fibres), two rim tones and mottle. It carries fibres in two tones, foxing clusters with pale halos, and folds drawn as a crease with a lit ridge and a shaded facet.
  - The wax seals are in relief, built up from a lip, a lumpy torus, a beaded ring, an inner wall, a flat field and a raised device. They have bubbles, drips and a glint.
- `v4a_ink.py`: the three mode drawings, as full ink illustrations with hatching.
  - **Practice:** the dummy on its post, with a target, a sword and a kettle hat.
  - **Skirmish:** two crossed longswords behind a quartered heater shield, with a rim, rivets and nicks.
  - **Campaign:** the keep on its hill, with towers, banners, a road and trees.
- `v4a_map.py`: the Rivergate map, zoomed out to the whole field (v4.2). The two flagged forts, the three lanes, the river with its plank bridge, stone bridge and ford, the A, B and C seals in relief and the mill hamlet read first. The shrine sits a notch quieter. Woods, hills, mountains and the farm are pale ground texture. There is no compass or border scale.
- `v4a_hud.py`: the ledger cards, hammered iron and book-corner brackets, the bosses and seals, and the battle feed.
  - **Iron:** six tones, with scratches and their grooves, pits, rust, and a cold specular on the top bevel.
  - **Brackets:** riveted plates with engraved arms and pierced lobes.
  - **Bosses and seals:** enamel turned in fine rings behind beaded iron.
- `v4a_table.py`, overridden by `v4a_furn.py`: the table furniture.
  - **Rail:** quarter-sawn oak with ray flecks and an ogee moulding, inlaid with brass engraved as a twisted cable, with rosette studs.
  - **Plaques:** slotted screws, each turned its own way.
  - **Ride and Enlist plates:** an engraved border that sweeps round each screw, with brushing kept off the text band and tarnish in the corners.
  - **Also here:** key caps, parchment tags, the wax badge, beaded portrait frames with corner bosses, the backdrops, and the props.
- `v4a_recruit.py`, overridden by `v4a_recruit2.py`: the recruit pieces.
  - **House cloths:** woven, with a twill in half steps, a lit weft every fourth row, a sheen on the folds and a fringe of two lengths.
  - **Rod:** six tones with specular glints.
  - **Letters:** the twine is twisted and the envelopes are creased.
- `v4a_portrait.py`: the live-model portraits (below).

### Portraits: the live model, framed by the spec

The unit identity spec (rule 8) fixes the portrait framing:
- the model is shown chest-up, 20 model px from 4 px above the head to 8 px below the shoulders;
- yaw 30 degrees, weapon arm forward, camera 5 degrees below eye level;
- "Tall +N" adds N px at the top;
- linen panels go where the team cloth would.

v4 draws exactly that.
- **Renderer:** a small ray caster in `v4a_portrait.py` stands in for `InventoryScreen.drawEntity`.
  - The boxes follow the vanilla biped layout.
  - The outer layers (hat, armour, EMF shells) are inflated as the game inflates them and drawn without back-face culling.
  - Textures are sampled nearest, and each face is lit as the inventory lights it: tops brightest, then the face toward the camera, then the turned side.
- **Skins:** painted from the spec looks with three or four flat tones per material, as rule 5 sets.
  - The Gilded Order units are Order Squire, Order Archer (with its hood point and the fletching over the shoulder), Templar (Tall +2) and Order Longbow (Tall +2, with the bow rising past the head).
  - Sunweaver (not yet drawn) and Lord Marshal Aldric (locked) are flat silhouettes.
  - The party seats use stand-in player skins: Kort in a white kettle hat with a gilt cross, Ansel in a green hood.
- **Windows:** sized so that one model px lands on whole GUI pixels.

| Where | Window | Frame | GUI px per model px |
|---|---|---|---|
| Muster roll | 40 x 40 | `frame-rim-48` | 2 (1.8 for Tall +2) |
| Party seats | 40 x 40 | `frame-rim-48` | 2 |
| Reveal letter | 160 x 160 | `frame-ornate-184` | 8 (7.3 for Templar, Tall +2) |

- **The consequence, stated plainly:** the 160 px reveal shows the skin at about 8 GUI pixels per texel, as the game will. Its richness comes from the light and the layers of the model. The fine detail sits in the frame and the backdrop around it. More detail inside the window would need higher-resolution unit skins, which is a skin decision for Phase 2, not a UI decision.

### The battle feed (HUD)

The feed sits at the right edge, at mid-height, and replaces the notice slip. In v4.1 it is a leaf of the same ledger as the cards, so it reads as part of the same object: a vellum slip 172 x 74 at x 772, y 232 (`feed-slip` in `v4a_hud.py`).
- **The material:** the same vellum as the cards, with their inked edge rule. It is ruled between its entries, and down a faded red margin beside the glyphs.
- **The iron edge:** the right edge is bound in a strip of hammered iron. It stands a pixel proud, laps two pixels over the top and bottom, and is held by three countersunk rivets. This is the feed’s own spine, mirroring the one the cards hang from.
- **The corner:** the free top corner carries one riveted book-corner bracket, like the cards’.
- **The lines:** the newest line is at the bottom. Five lines are in play, and each fades over the last two of its eight seconds. The mock shows five, with the oldest fading out. The text is Minecraft 8 px at a 12 px pitch, with the glyph at x 8 and the words at x 20.
- **The inks:** on the vellum, each kind is written in a dark ink of its own hue, as the cards write theirs. Every ink holds 5.3:1 or better on the vellum body. The glyph carries the hue a little brighter than its words, and its pierced holes (a skull’s eyes, the cut through a struck seal) are left as bare vellum (`fgv-*`).
- **v4, superseded:** a dark translucent strip, 168 x 100, with light text and seven lines. It read as vanilla chat.

Each kind has its own colour and its own glyph, so colour never works alone:

| Event | Ink | Glyph | Mock line |
|---|---|---|---|
| A point taken by us | teal #1d4f4c | the point seal | Dawn took Mill Hamlet (fading) |
| A point lost to the enemy | violet #563a73 | the seal, struck | Dusk took River Shrine |
| Our troop losses | red #7a2416 | a skull | 3 Dawn Knights lost |
| Enemy troop losses | grey #504b44 | a skull, grey | 4 Dusk Revenants lost |
| A fellow commander down | brass brown #684600 | a fallen banner | Ansel’s banner has fallen |
| A commander back in the field | brass brown #684600 | a raised banner (`fgv-banner-up`) | not shown |
| Routing and regroup | grey #504b44 | the ROUTING stamp (`fgv-rout`) | not shown |
| Reinforcements and notices | ink #3d3124 | the sun (`fgv-sun`) | not shown |

### Mode subtitles

Each order carries a quieter line in Minecraft 8 px sepia under its name:
- Practice: train against bots.
- Skirmish: one battle, your rules.
- Campaign: the story, battle by battle.

The two plain orders widened from 144 to 152 px, so that the longest line (132 px) fits inside the 8 px padding. Their names moved from sepia to ink, so each name stays louder than its subtitle.

### What changed against the v3 deductions

**HUD**
- **Too many always-on words on the cards:** the selected card (Ranged) keeps glyph and word for its order and formation, plus its keys. Infantry and Support show glyphs alone. The state word (Fighting, Steady, ROUTING) stays, because it is the one fact that changes in a fight.
- **The notice slip at the top of the left stack:** it is gone, because the feed carries notices. The spine is shorter (180 px), so the stack starts lower.

**War Table, home**
- **Empty oak:** the content band was already centred: 128 px from the rail to the cards, and 112 px from Ride to the bottom. The oak around it now has a reason. These objects sit at the table edges, kept dim and away from the text:
  - the campaign map, rolled and tied, at the top left (v4.1, `prop-maproll`, replacing the flat map corner). It is traced as a cylinder in the same oblique view as the inkwell and candle. Its far end is pushed under the rail, whose shadow falls across it. At the near end the curl shows, the sheet wound three times round a hollow core, and the edge of the outer sheet runs down its length. A faded red ribbon is tied round it, with the knot at the front and both tails lying on the oak. Its own shadow falls to the lower right;
  - a candle in a brass chamberstick, with its light falling on the oak, at the bottom left;
  - the inkwell and quill at the bottom right.
- **Party seats:** they grew to 40 px windows so the live model lands on whole pixels, and the pitch is now 56 px.

**War Table, recruit**
- **Right column:** it is now one ladder on x 784, with 12 px between rungs. Enlist moves right under the letters, so the action sits next to what it opens.

| Rung | Top to bottom |
|---|---|
| The sealed letters | 124 to 342 |
| Enlist, with its cost | 354 to 394 |
| The purse it spends from | 406 to 432 |
| Epic within 28 letters | 442 |
| Odds | 468 to 492 |

The ladder ends on 492, as the roll and the letter do.

### Rule changes (amends DESIGN-RULES.md)

- Order and formation words appear on the selected group card only. The other cards show the glyphs. v3 showed the words on every card.
- Battle notices appear in the feed only: five lines at most, newest at the bottom, colour always paired with a glyph.
- A portrait window is sized so that a model pixel lands on whole GUI pixels: 40 px for the standard 20 model px.

### How it builds in the game

- **Feed:** each line is an 8 x 8 glyph sprite plus Minecraft text, drawn with an alpha taken from the age of the line. The strip is a 3-slice: the top rule, a tiled middle and the bottom rule.
- **Props:** fixed textures drawn under the screen content. The candle flame can be three or four frames, and the light pool is one translucent texture.
- **Portraits:** `InventoryScreen.drawEntity` with the spec pose, under a scissor. The ray caster exists only for the mock.
- **Everything else:** sliced as in v3. Plaques, tags and ledger cards are 9-slices with fixed corners. Rails, rods and bars are 3-slices. Banners, letters, the roll, the frames and the brass plates are fixed-size textures.

### Not mocked yet

- The raised-banner line (a commander back in the field). Its glyph exists.
- A sixth feed line pushing the oldest out.
- The "not enough gold" state on Enlist, the battle state with Alt held, and the point names on select (carried over from v3).
