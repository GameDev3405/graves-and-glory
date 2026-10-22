# Unit identity: a look and a purpose for every unit

Version 1, 2026-10-17. Proposal for Kort's sign-off; design only. Feeds the
Phase 2 skin batches and the live-model portraits on the War Table screens.
Built from the sheet, the Houses spec and plan, the armory pack, the spawn
and portrait code and the vanilla 1.20.1 renderers.

Covers 32 units: the 30 Phase 2 troops (ten live today under Dawn and Dusk
names) and the two rows the migration removes. "(was ...)" marks a change
from Houses spec 2.7; anything unmentioned stays as 2.7 has it.

## 0. Rules

**Kort, 2026-10-18 late: new characters, not recolours.** Every troop is a new character: its own geometry on the shared biped or beast skeleton (horns, antlers, hoods, pauldrons, capes, quivers, exposed bone, crests, lanterns), its own body proportions by House and troop (broad and low, gaunt and tall, hunched), its own hand-drawn skin per stage with a glow layer where earned, and its own weapon model. Only the invisible skeleton is shared, so one animation set drives everyone. The pipeline builds this from a shared part library the skin tool stitches into any troop's model, plus per-troop parts. A troop that could be mistaken for a recoloured vanilla mob, or for another troop by silhouette alone, fails the lookalike check and goes back; geometry counts, not only colour. Every skin and portrait is graded against `work/ui-concepts/GRADING-RUBRIC.md` at 9 or better before Kort sees a House.

1. **Layout.** Headers give House, group, rarity, evolve line and sheet id.
   Look runs: silhouette; palette; armour and weapon; head and face; **mark**,
   readable at 30 blocks (4 px or more, or a change of outline); **tell**, an
   idle or attack cue.
2. **Team colour is the loudest read, in fixed places:** the back pennant (the
   plan's 4x8 box, 6 px above the head; on beasts, at the shoulders), a chest
   panel at least 4 px wide (tabard, sash, saddle-cloth or jerkin), the shield
   field, impact dust, trails, and one large cloth per Legendary. Panels and
   pennants are team parts of the troop's own model, drawn from one sheet per
   side, `textures/entity/troop/team_cloth_<side>.png`, picked by the troop's
   `gg_<side>` scoreboard team; skins carry no team colour. Spell particles
   belong to the House, never WITCH or SOUL_FIRE_FLAME.
3. **Armour never draws** on a troop (points only; plan Needs Kort 10): the
   skin and parts are the look. Troops with folded arms (rule 6) show weapon
   and shield only while attacking or casting, and folded arms hide arm parts,
   so illager pauldrons hang from the body. Helms and hoods are parts tagged
   `helm` (a scene's `helm_off` hides them): the item keeps its stats, the part
   sets the look.
4. **Gold and diamond never show** (section 8). Golden helmets become
   chainmail (same armour), diamond weapons iron (the item adds no damage),
   Aldric's diamond chestplate netherite (+1 toughness, +0.1 knockback
   resistance).
5. **Art.** Original paintings in the rig's UV layout, flat-shaded, 3 to 4
   tones per material. Commons and Rares get the plan's standard set (three
   stage skins `<id>_s1.png` to `_s3.png`, a Veteran skin `<id>_veteran.png`
   and the model file `art/troops/<id>.json`, whose stage-3 part shows from
   stage 3), Epics and Legendaries one skin `<id>_s1.png` and `<id>.json`;
   drowned and strays add an outer layer `<id>_o.png`. Build lines name the
   skins, outer layers and glow. `<id>_e.png` is glow: at most 6 lit pixels
   (16 on Epics and Legendaries), never on team surfaces. Stage 2 repaints
   trim, stage 3 adds the 2.7 part, Veteran edges the chest panel in gilt.
6. **Rigs and poses.** Four rigs carry every troop: biped (husk, zombie,
   skeleton, stray, wither skeleton, drowned, zombified piglin, vindicator,
   pillager, evoker), hound (wolf, polar bear, ravager), spider and colossus
   (iron golem). A rig fixes its bone names (`body`, `head`, the arms and
   hands, the legs and its beast bones), so one set of 18 clips per rig drives
   every troop on it (plan Task 5b), with a short idle tell per troop and an
   attack flavour per House (Task 5c). Proportion is per-troop scale on named
   bones; boar-kin is the biped with a hunch; Old Horn and the Brute are the
   hound rig on their own boxes. `idleArms` gives the arms-forward stance
   (Risen, Drowned Raider) and folded arms (Hearthguard, Shieldbreaker, Mist
   Caller). No vanilla model or pose code reaches a troop, so knights cannot
   shamble and `BipedAnimationMixin` goes.
7. **Item data.** Weapons are the troop's own models, drawn at the rig's hand
   bone; the sheet's weapon, helmet and chest items stay for armour points and
   ability code and never draw. The one vanilla item drawn is the painted
   shield (`BlockEntityTag` patterns on the side's dye; a light-grey field on
   portraits), from one sheet column, `troops.shield`, read by one line each
   in `Battle.spawn` and `ArmyCards.portrait`.
8. **Portraits** show the troop's own live model (`TroopRenderer`, through
   `ArmyCards.portrait`) chest-up in a square window. Standard:
   20 model px, from 4 px above the head to 8 px below the shoulders; yaw 30
   degrees, weapon arm forward; camera 5 degrees below eye level; fixed gaze.
   "Tall +N" adds N px at the top; "Wide N" (head and shoulders, or crew and
   engine) and "Beast N" (head and forequarters, yaw 35) are N px across. One
   window serves the 36 px roster and 160 px reveal frames. Portrait models
   wear no match side and no pennant: the company's cloth on campaign and
   camp pages (the Hollow's navy), the House's colour on a House page
   (controller, 2026-10-18 late, from Kort's live-portrait ruling; the linen
   #CFC6B0 reading is retired).

## 1. Gilded Order

Upright and pale: crests, plumes, white surcoats, painted shields, faces
shown. All husk.

### Order Squire
Gilded | infantry | Common | Order Squire > Knight > Knight-Captain | `knight` (today Dawn Knight)
- **Purpose.** The cheap shield line that advances into arrow fire and screens the bows. Beats archers and Arbalists; loses to Shieldbreakers, cleave and blight.
- **Look.** Upright, shield forward. Enamel #D9D3C1, steel #7C8591, gilt #96783A. Iron cap, mail, hip-length white surcoat with a team panel, arming sword. Young, clean-shaven. **Mark:** team shield, white pale. **Tell:** shield to the chin in Shieldwall, and on the approach in Compact (it drops to swing; Phase 1 Task 5d).
- **Build.** husk, biped rig, no scale; parts: iron_cap (helm), surcoat_skirt; team: chest_panel, pennant; stage 3 +crest_plume; skins knight_s1/s2/s3.png; weapon model arming_sword; shield `white:cs`; items (stats only): iron_helmet, iron_chestplate, iron_sword.
- **Portrait.** Standard.

### Order Archer
Gilded | ranged | Common | Order Archer > Order Bowman > Sergeant-Archer | `archer` (today Dawn Ranger)
- **Purpose.** The volley line: +20% damage on Hold, behind a wall. Beats slow infantry and massed Risen; loses to Hounds, Web Stalkers and any charge.
- **Look.** Slight, hooded, bow upright. Linen #BFB59A, steel #7C8591, gilt #96783A. Linen hood, gambeson (was green-grey), team jerkin, white-fletched quiver. Face in shadow. **Mark:** hood point and fletching over the shoulder. **Tell:** bow raised to the eye. Stage 3: signal horn (was a feathered cap).
- **Build.** husk, biped rig, slight (scale body and arms [0.9,1,0.9]); parts: linen_hood (helm), white_quiver; team: jerkin, pennant; stage 3 +signal_horn; skins archer_s1/s2/s3.png; weapon model order_bow; items (stats only): chainmail_helmet, leather_chestplate, bow.
- **Portrait.** Standard.

### Templar
Gilded | infantry | Rare | Templar > Templar Knight > Templar Commander | `sentinel` (today Dawn Sentinel)
- **Purpose.** The arrow wall: 46 HP, a shield and a slow walk. Beats every ranged unit and Hounds at a wall; loses to Shieldbreakers, the Mortar and Mist Callers.
- **Look.** A tall white block. Enamel #D9D3C1, steel #7C8591, gilt #96783A. White plate, knee-length team tabard with a white cross, longsword. Closed helm, painted slit. **Mark:** flat-topped great helm over the long tabard. **Tell:** shield up in Shieldwall; a slow plod.
- **Build.** husk, biped rig, tall (scale body [1.05,1.1,1.05], legs [1,1.15,1]); parts: great_helm (helm, closed, painted slit); team: long_tabard (the white cross is fixed paint on the cloth sheet), pennant; stage 3 +cape; skins sentinel_s1/s2/s3.png; weapon model longsword; shield `white:sc`; items (stats only): diamond_helmet, iron_chestplate, iron_sword (was diamond).
- **Portrait.** Tall +2.

### Order Longbow
Gilded | ranged | Rare | Order Longbow > Yeoman Longbow > Master Longbow | `longbow` (today Dawn Longbow)
- **Purpose.** The mobile line-breaker: 30 range and pierce punish packed formations. Beats tight lines and Ballista crews; loses to Web Stalkers, Hounds and Mist.
- **Look.** Narrow, tall quiver. Linen #BFB59A, yeoman brown #6B5136, gilt #96783A. Peaked felt cap (moved from stage 3), gambeson, team jerkin, gilt sash. Weathered, short beard. **Mark:** the longbow rising past the head. **Tell:** aims with the bow canted. Stage 3: a long white feather.
- **Build.** husk, biped rig, narrow (scale body [0.9,1,0.9], legs [1,1.05,1]); parts: felt_cap (helm, peaked), tall_quiver, gilt_sash; team: jerkin, pennant; stage 3 +long_feather; skins longbow_s1/s2/s3.png; weapon model longbow (rising past the head); items (stats only): leather_helmet, leather_chestplate, bow.
- **Portrait.** Tall +2.

### Sunweaver
Gilded | support | Epic | no evolution | `mage` (today Sunweaver)
- **Purpose.** The healer that wins long fights: mends 5 HP every 4 s within 10 and never strikes. Beats attrition fights and Rise Again stalls; loses to Pounce, Harpooner pull and the Mortar.
- **Look.** Robed triangle. Sun-white #F2E6C2, linen #BFB59A, gilt #96783A. White hood and robe, team vestment, sunburst brooch. Old, grey-bearded. **Mark:** a 6x6 sun disc on a staff, 4 px above the head (was a head halo). **Tell:** staff forward, ENCHANT at the hand, hearts on the target.
- **Build.** husk, biped rig, no scale; parts: white_hood (helm), robe_skirt, sun_brooch; team: vestment, pennant; skin mage_s1.png, glow mage_e.png; weapon model sun_staff (the 6x6 disc 4 px above the head); items (stats only): chainmail_helmet (was golden), leather_chestplate, blaze_rod.
- **Portrait.** Tall +6.

### Lord Marshal Aldric
Gilded | infantry | Legendary | no evolution | `gl_marshal`
- **Purpose.** The anchor: on Hold he plants a banner that for 15 s adds 10% damage within 12, and its ground cannot be moved (no knockback, pull or slow). Beats slows, pulls and knockback; loses to slams and focused blight.
- **Look.** Broad, under a standard. Enamel #D9D3C1, blackened steel #2E3236, gilt #96783A. White surcoat over blackened plate (was gilded), shield, longsword. Open helm with a gilt laurel (was a crown-helm); grey beard. **Mark:** his pennant as a standard: 6x10 team cloth, 12 px above the head (was a red cloak). **Tell:** plants it on Hold, lifting the war horn from his belt to his lips; the horn is dress only and never sounds (no-routing design 5.2, round 30).
- **Build.** husk, biped rig, broad (scale body [1.15,1.05,1.1], arms 1.1); parts: laurel_helm (helm, open), surcoat_skirt; team: chest_panel, standard (his pennant: 6x10, 12 px above the head); skin gl_marshal_s1.png; weapon model marshal_sword; shield `white:sc,light_gray:bo`; items (stats only): chainmail_helmet (was golden), netherite_chestplate (was diamond), netherite_sword.
- **Portrait.** Tall +4.

## 2. Graveborn

Hunched or gaunt, ragged hems, bare bone, no shields; the casters carry light.
Glow is soul-light #CFEFFF (was soul-cyan).

### Risen
Graveborn | infantry | Common | Risen > Revenant > Grave Knight | `revenant` (today Dusk Revenant)
- **Purpose.** Cheap, slow, fearless bodies that hold ground by numbers. Beats other Commons on price and in long fights, hardest when nearly dead; loses to cleave, slams, the Mortar and kiting archers.
- **Look.** Stooped, arms forward, hem in strips. Grave-moss #4F5A3C, rust #6B4A33, bone #C9C1A8. Rusted coif and hauberk, shredded team tabard, notched sword. Grey-green face, slack jaw. **Mark:** the stoop under the shredded tabard. **Tell:** the arms-forward shamble. Stage 3: rusted visor and half-cape (was a crown).
- **Build.** zombie (`ArmyZombie`; today wither_skeleton), biped rig, stooped (hunch 15, idleArms forward); parts: rusted_coif (helm), ragged_hem; team: shredded_tabard, pennant; stage 3 +rusted_visor, half_cape; skins revenant_s1/s2/s3.png, glow revenant_e.png; weapon model notched_sword; items (stats only): chainmail_helmet, chainmail_chestplate, iron_sword.
- **Portrait.** Standard, head dipped.

### Bone Archer
Graveborn | ranged | Common | Bone Archer > Barrow Archer > Deadeye | `bonebow` (today Bone Ranger)
- **Purpose.** The cheap, fearless bow line that keeps shooting while chased and hits hardest when wounded. Beats a focused Sunweaver and slow siege crews; loses to Hounds and Gilded shields.
- **Look.** Dark hood over stick legs. Bone #C9C1A8, grave-moss #4F5A3C, grave-iron #2B2826. Moss rag hood, team jerkin, bone quiver. Bare skull. **Mark:** a skull pauldron on the bow shoulder. **Tell:** the native skeleton draw. Stage 3: one lit eye socket (was a lantern).
- **Build.** skeleton, biped rig, bone limbs (arm and leg boxes 2x12x2); parts: rag_hood (helm), bone_quiver, skull_pauldron (bow shoulder); team: jerkin, pennant; stage 3: glow bonebow_e.png from stage 3 (the lit eye socket); skins bonebow_s1/s2/s3.png; weapon model bone_bow; items (stats only): chainmail_helmet, leather_chestplate, bow.
- **Portrait.** Standard.

### Grave Reaper
Graveborn | infantry | Rare | Grave Reaper > Harvester > Lord of Scythes | `reaper` (today Dusk Reaper)
- **Purpose.** The crowd-cutter: each hit also strikes a second enemy for half. Beats Commons in crowds and Risen mirrors; loses to Templar shields and braced Trident Guards.
- **Look.** Tall, black, hooded. Grave-iron #2B2826, bone #C9C1A8, soul-light #CFEFFF. Mantle over the cuirass, team sash, scythe. Skull deep in the hood, two lit eyes. **Mark:** a 10 px curved blade over the hood. **Tell:** overhead swing, sweep on both targets.
- **Build.** wither_skeleton, biped rig, tall bone (arm and leg boxes 2x12x2; scale body [1,1.1,1], legs [1,1.15,1]); parts: deep_hood (helm), mantle; team: sash, pennant; stage 3 +spine_plates; skins reaper_s1/s2/s3.png, glow reaper_e.png (the two eyes); weapon model scythe (the 10 px blade over the hood); items (stats only): netherite_helmet, iron_chestplate, iron_hoe (today iron_axe).
- **Portrait.** Tall +4.

### Barrow Stalker
Graveborn | ranged | Rare | Barrow Stalker > Frost Stalker > Wight Stalker | `stalker` (today Dusk Stalker)
- **Purpose.** The anti-runner: frostshot slows, so Hounds and Web Stalkers die before they arrive. Beats beasts and cavalry; loses to Templars and Mist Callers.
- **Look.** A solid blue-grey wedge. Frost grey #93A7AE, rime #DCE3E2, grave-iron #2B2826. Fur cowl, mantle to the knees, team wrap. Rimed skull. **Mark:** the knee-length mantle. **Tell:** SNOWFLAKE on shot and target. No antler headdress (antlers are Wildmoot's).
- **Build.** stray (`ArmyStray`; today skeleton), biped rig, a wedge (leg boxes 2x12x2; scale body [1.1,1,1.1]); parts: fur_cowl (helm), knee_mantle; team: wrap, pennant; stage 3 +frost_strips; skins stalker_s1/s2/s3.png, outer stalker_o.png; weapon model frost_bow; items (stats only): chainmail_helmet, no chest, bow.
- **Portrait.** Standard.

### Gravecaller
Graveborn | support | Epic | no evolution | `hexer` (today Gravecaller)
- **Purpose.** The shieldwall-breaker: blight drops a delayed burst that ignores shields and slows. Beats shieldwalls and clumped Commons; loses to Hounds, Harpooners and pierce.
- **Look.** Hooded robe to the ground, lantern held out. Grave-iron #2B2826, gilt #96783A, soul-light #CFEFFF. Burial robe, team vestment, candle stubs on the shoulders. Gilt death mask. **Mark:** the lit lantern at arm's length. **Tell:** lantern raised through the windup; SOUL ring at the target (was WITCH).
- **Build.** wither_skeleton, biped rig, robed (arm boxes 2x12x2); parts: burial_hood (helm), death_mask, burial_robe (to the ground), candle_stubs; team: vestment, pennant; skin hexer_s1.png, glow hexer_e.png; weapon model lantern (4x6 px, lit, at arm's length); items (stats only): chainmail_helmet (was golden), leather_chestplate, empty hand (was amethyst_shard).
- **Portrait.** Standard.

### The Hollow King
Graveborn | infantry | Legendary | no evolution | `gb_hollowking`
- **Purpose.** The fear engine: enemies within 10 deal 15% less. Beats damage-heavy packs, above all Wildmoot's; loses to the Lord Marshal's banner, Sunder and Brassjaw.
- **Look.** Tall, broad, cloak dragging. Grave-iron #2B2826, gilt #96783A, soul-light #CFEFFF. Black plate split over a soul-fire ribcage, broken crown, tattered team-cloth cloak, sword. Jaw bound in gilt wire. **Mark:** the lit ribcage under the crown. **Tell:** SOUL motes drifting at the aura's edge.
- **Build.** wither_skeleton, biped rig, tall and broad (scale body [1.2,1.15,1.15], arms 1.1, legs [1.1,1.15,1.1]); parts: broken_crown, gilt_jaw_wire, split_plate, lit_ribcage; team: tattered_cloak (his one large cloth), pennant; skin gb_hollowking_s1.png, glow gb_hollowking_e.png; weapon model grave_sword; items (stats only): chainmail_helmet (was golden) as a coif, netherite_chestplate, netherite_sword.
- **Portrait.** Tall +4.

## 3. Wildmoot

Snouts, ears, low four-legged bodies, antlers and feathers; fur and hide, no
metal. Warpaint is pale clay (was ochre).

### Moot Hound
Wildmoot | cavalry | Common | Moot Hound > Dire Hound > Packleader | `wm_hound`
- **Purpose.** The fastest unit, diving over open ground onto archers and casters. Beats archers, casters and wounded troops; loses to spears, Frost Stalkers and a Templar wall.
- **Look.** Low and long, head down. Russet #7A4127, bark #4A3626, clay #C8B48A. Shaggy coat, bone collar, team saddle-cloth with hide spikes. Dark muzzle. **Mark:** a clay stripe from nose to tail. **Tell:** a growl and a puff of clay dust on the pounce.
- **Build.** wolf (`ArmyWolf`), hound rig, low and long (scale body [1,0.9,1.15], legs [1,0.9,1]); parts: bone_collar, hide_spikes; team: saddle_cloth, pennant (at the shoulders); stage 3 +antler_tufts; skins wm_hound_s1/s2/s3.png; no weapon; no items.
- **Portrait.** Beast 16.

### Hide Hunter
Wildmoot | ranged | Common | Hide Hunter > Thornstalker > Elder Hunter | `wm_hunter`
- **Purpose.** The ambusher: +25% damage while shooting from under canopy or roof. Beats anything walking through woods; loses to open-field archers, Hounds and the Mortar.
- **Look.** Snout, floppy ears, feathers up one side. Russet #7A4127, bark #4A3626, clay #C8B48A. Hide cloak, team jerkin, leather cap, bow. Clay brow stripes. **Mark:** a three-feather fan on the left of the head. **Tell:** the bow draw (rule 6). Stage 3: five feathers (was an antler headdress).
- **Build.** zombified_piglin (`ArmyBoarkin`), biped rig, boar-kin (hunch 20); parts: boar_snout, floppy_ears, leather_cap (helm), feather_fan_3 (stages 1 and 2), hide_cloak; team: jerkin, pennant; stage 3 +feather_fan_5 (in place of the three); skins wm_hunter_s1/s2/s3.png; weapon model hide_bow; items (stats only): leather_helmet, leather_chestplate, bow.
- **Portrait.** Tall +4.

### Web Stalker
Wildmoot | cavalry | Rare | Web Stalker > Broodstalker > Widowmaker | `wm_stalker`
- **Purpose.** The wall-climber: over cover onto backlines, pinning them with Slowness II. Beats ranged backlines and wall defenders; loses to spears, Frost Stalkers and Sunweaver sustain.
- **Look.** Wide, low, eight legs. Chitin #23291F, bone #C9C1A8, clay #C8B48A. Silk sacs, team cloth tied on the back. **Mark:** a white bone mask over the eyes (moved from stage 3; it covers the red vanilla eyes). **Tell:** cobweb particles on the snared target. Stage 3: mandible bones.
- **Build.** spider (`ArmySpider`), spider rig, wide (scale legs [1.15,1,1.15]); parts: bone_mask (over the eyes), silk_sacs; team: back_cloth, pennant; stage 3 +mandibles; skins wm_stalker_s1/s2/s3.png; no weapon; no items.
- **Portrait.** Beast 18, yaw 20.

### Tusk Spear
Wildmoot | infantry (spear) | Rare | Tusk Spear > Tuskguard > Boar-Lord | `wm_tusk`
- **Purpose.** The cheap, fast counter-charge for woods and flanks: braced on Hold it hits chargers 30% harder, beasts always 60%. Beats cavalry and charges; loses to archers, the Mortar and cleave.
- **Look.** Boar-kin, spear upright. Russet #7A4127, bark #4A3626, clay #C8B48A. Hide mantle, team jerkin, leather cap, clay tassel. Big white tusks. **Mark:** a crossbarred boar-spear head above the cap. **Tell:** clay dust at its feet while braced.
- **Build.** zombified_piglin, biped rig, boar-kin (hunch 20); parts: boar_snout, tusks, leather_cap (helm), hide_mantle; team: jerkin, pennant; stage 3 +tusk_crown; skins wm_tusk_s1/s2/s3.png; weapon model boar_spear (crossbar and clay tassel, so it never reads as a trident); items (stats only): leather_helmet, leather_chestplate, trident.
- **Portrait.** Tall +4.

### Bloodhowl Seer
Wildmoot | support | Epic | no evolution | `wm_seer`
- **Purpose.** The war-drum: every 15 s its howl gives troops within 10 +15% damage and speed for 6 s. Beats long melee brawls; loses to pierce, Barrow Stalkers and Harpooners.
- **Look.** Hunched elder, antler crown, totem staff on the back. Grey fur #7D7468, bark #4A3626, dried blood #6E2A22. Hide robe, blood handprints, bone wand. Grey muzzle, clay-glow eyes. **Mark:** the antler crown, two branching tines 6 px tall. **Tell:** a wolf howl and a ring of clay dust at 10 blocks.
- **Build.** zombified_piglin, biped rig, hunched elder (hunch 30); parts: boar_snout, antler_crown (two tines, 6 px), hide_robe, totem_staff (on the back); team: sash, pennant; skin wm_seer_s1.png, glow wm_seer_e.png; weapon model bone_wand; items (stats only): leather_helmet under the crown, leather_chestplate, bone.
- **Portrait.** Tall +6.

### Old Horn, the Antlered Alpha
Wildmoot | cavalry | Legendary | no evolution | `wm_oldhorn`
- **Purpose.** The battering beast: on Charge it tramples everything within 3 for 6 damage and knockback. Beats archer lines and Commons in Line; loses to braced spears, Shieldbreakers and the Mortar.
- **Look.** A huge low bear under a wide rack. Bark #4A3626, grey muzzle #8A8378, clay #C8B48A. Bone-and-feather mantle, clay warpaint, team war-banner strung between the tines, pennant on the hump. Scarred muzzle, clay-glow eyes. **Mark:** the 16 px antler rack. **Tell:** rears (the polar-bear warning pose) before the trample.
- **Build.** polar_bear (`ArmyBear`), hound rig with its own bear boxes on a 128x64 sheet; parts: antler_rack (16 px), bone_feather_mantle; team: war_banner (strung between the tines; his one large cloth), pennant (on the hump); skin wm_oldhorn_s1.png, glow wm_oldhorn_e.png; no weapon; no items.
- **Portrait.** Beast 24.

## 4. Ironclad

Wide and square, arms folded at rest, beards, engines on the back. Every plate
is skin or a part (rule 3). Ember is a light only.

### Hearthguard
Ironclad | infantry | Common | Hearthguard > Ironwall > Hearth-Thane | `ic_hearthguard`
- **Purpose.** The anchor: slow, strongest standing still (Entrench), it holds a point against archers. Beats archers and Hounds at a wall; loses to Shieldbreakers, the Mortar and Mist.
- **Look.** Square: box helm, slab pauldrons, arms folded. Soot iron #34373B, brass #7F6232, copper beard #7A4A2E. Box helm with cheek plates, knee-length scale coat, team tabard. Braided beard to the belt. **Mark:** the slab pauldrons. **Tell:** arms folded at rest; axe and shield appear when it engages.
- **Build.** vindicator (`ArmyVindicator`), biped rig, square (scale body [1.2,0.95,1.15], arms [1.1,0.9,1.1], legs [1.1,0.8,1.1]; idleArms folded); parts: box_helm (helm, cheek plates), braided_beard, slab_pauldrons (on the body), scale_coat_skirt; team: tabard, pennant; stage 3 +helm_horns; skins ic_hearthguard_s1/s2/s3.png; weapon model hearth_axe; shield `black:bo,light_gray:mc`; items (stats only): iron_helmet, iron_chestplate, iron_axe.
- **Portrait.** Standard.

### Arbalist
Ironclad | ranged | Common | Arbalist > Steel Arbalist > Bolt-Master | `ic_arbalist`
- **Purpose.** The heavy shot: a 3 s reload, but shields stop only part of each bolt. Beats Gilded shields and slow infantry; loses to Hounds and Web Stalkers.
- **Look.** Crossbow held level at the chest. Soot iron #34373B, brass #7F6232, scorched leather #4B3A2C. Leather coat, team tabard, bolt case on the back, brass goggles. Short beard. **Mark:** the level crossbow; every other bow stands upright. **Tell:** the crossbow-charge pose while reloading (`setCharging`).
- **Build.** pillager (`ArmyPillager`), biped rig, square (scale body [1.15,0.95,1.1], legs [1.1,0.8,1.1]); parts: brass_goggles, short_beard, leather_coat_skirt, bolt_case; team: tabard, pennant; stage 3 +brass_shoulder_plate; skins ic_arbalist_s1/s2/s3.png; weapon model arbalest (held level); items (stats only): chainmail_helmet, leather_chestplate, crossbow.
- **Portrait.** Standard.

### Shieldbreaker
Ironclad | infantry | Rare | Shieldbreaker > Gatebreaker > Anvilfist | `ic_breaker`
- **Purpose.** The shield answer: it ignores shields and hits shield troops 40% harder. Beats every shield troop and Brassjaw; loses to archers, Hounds and Reaper cleave.
- **Look.** Lopsided: one anvil pauldron, bare arms. Soot iron #34373B, brass #7F6232, ember #E2702A (a few lit pixels). Chain belt, team tabard, tattooed arms and scalp. Shaved head, black beard. **Mark:** a 6x4x6 anvil on the right shoulder. **Tell:** the axe comes up overhead when it engages.
- **Build.** vindicator, biped rig, square, heavy arms (scale body [1.2,0.95,1.15], arms [1.15,0.95,1.15], legs [1.1,0.8,1.1]; idleArms folded); parts: anvil_pauldron (6x4x6, right shoulder, on the body), black_beard, chain_belt; team: tabard, pennant; stage 3 +forge_apron; skins ic_breaker_s1/s2/s3.png, glow ic_breaker_e.png; weapon model great_axe; items (stats only): iron_helmet, iron_chestplate, netherite_axe.
- **Portrait.** Standard.

### Ballista Crew
Ironclad | siege | Rare | Ballista Crew > Siege Crew > Master Engineer | `ic_ballista`
- **Purpose.** The dug-in piercer: 34 range, 10 damage, bolts that carry on to a second target. Beats lines, Legendaries and (Phase 4) structures; loses to Hounds, Web Stalkers and Old Horn.
- **Look.** A crewman behind a two-wheeled ballista. Soot iron #34373B, brass #7F6232, oak #6A4E30. Oak-and-iron frame, 16 px bow arms, loaded bolt, team tabard, iron cap. **Mark:** the wheeled frame. **Tell:** POOF at the frame on each shot.
- **Build.** pillager, biped rig, square (as the Arbalist); parts: iron_cap (helm), ballista_frame and ballista_wheels (root parts: oak frame, 16 px bow arms, loaded bolt); team: tabard, pennant; stage 3 +brass_crest (on the frame); skins ic_ballista_s1/s2/s3.png; no weapon model (the frame is the engine); items (stats only): iron_helmet, leather_chestplate, crossbow (the trigger).
- **Portrait.** Wide 28.

### Ironhold Mortar
Ironclad | siege | Epic | no evolution | `ic_mortar`
- **Purpose.** The shell: a 36-range blast that cannot fire inside 10 blocks. Beats shieldwalls, entrenched lines and clumps; loses to anything that closes.
- **Look.** A crewman under a black barrel slung at 45 degrees. Soot iron #34373B, brass #7F6232, ember #E2702A (glow). Ammo crate, heavy apron, team tabard. Scorched beard. **Mark:** the ember-lit barrel mouth above the shoulder. **Tell:** FLAME at the mouth through the 30-tick windup, then LARGE_SMOKE.
- **Build.** pillager, biped rig, square (as the Arbalist); parts: iron_cap (helm), scorched_beard, heavy_apron, ammo_crate, mortar_barrel (root part at 45 degrees over the shoulder; the mouth is lit); team: tabard, pennant; skin ic_mortar_s1.png, glow ic_mortar_e.png; no weapon model; items (stats only): iron_helmet, iron_chestplate, no item.
- **Portrait.** Tall +6.

### Brassjaw, the Forge Colossus
Ironclad | infantry | Legendary | no evolution | `ic_colossus`
- **Purpose.** The walking forge: every hit also slams all within 2.5 for 60%. Beats Commons and packed melee; loses to Shieldbreakers, Mortar focus fire and kiting archers.
- **Look.** 2.7 blocks, long arms, smokestacks from both shoulders. Brass #7F6232, soot iron #34373B, ember #E2702A (glow). Riveted plates, furnace-grille chest, brass jaw, team banner from belt to knee. Sunken head, ember eye slit. **Mark:** the twin smokestacks. **Tell:** arms swing up on each hit (golem status 4) and the stacks smoke.
- **Build.** iron_golem (`ArmyGolem`), colossus rig (128x128), long arms (scale arms [1,1.1,1]); parts: smokestacks (both shoulders), furnace_grille, brass_jaw; team: belt_banner (belt to knee; his one large cloth), pennant; skin ic_colossus_s1.png, glow ic_colossus_e.png; no weapon; no items.
- **Portrait.** Wide 28.

## 5. Tidecallers

Dripping: kelp trailing from limbs, bleached coral, tridents, hoods. Glow is
pale sea-foam #CFF3E4 (was teal).

### Drowned Raider
Tidecallers | infantry | Common | Drowned Raider > Reef Raider > Tide Reaver | `tc_raider`
- **Purpose.** The catcher: every hit slows, so runners cannot get away. Beats Hounds, runners and retreating troops; loses to Templars, cleave and the Mortar.
- **Look.** Forward lean, kelp streaming from both arms. Verdigris #4E7A6A, barnacle #CFCBBE, navy #1E2C44. Bare torso, torn team sash, cutlass. Bloated face, white eyes. **Mark:** the green turtle-shell helmet. **Tell:** the arms-forward lunge; BUBBLE on the slowed target. No coral (the Trident Guard's); stage 3: barnacles crust the shell (was a crown).
- **Build.** drowned (`ArmyDrowned`), biped rig, forward lean (hunch 10, idleArms forward); parts: turtle_shell (helm), kelp_strands (both arms); team: torn_sash, pennant; stage 3 +shell_barnacles; skins tc_raider_s1/s2/s3.png, outer tc_raider_o.png; weapon model cutlass; items (stats only): turtle_helmet (was chainmail; same armour), no chest, iron_sword.
- **Portrait.** Standard.

### Harpooner
Tidecallers | ranged | Common | Harpooner > Whaler > Deep Harpooner | `tc_harpooner`
- **Purpose.** The puller: 16-range harpoons drag the target a block closer and slow it. Beats casters and Ballista crews; loses to longer-range archers and Gilded shields.
- **Look.** Oilskin hat, rope coil over the shoulder. Navy #1E2C44, barnacle #CFCBBE, verdigris #4E7A6A. Rags, team sash, harpoon. Drowned face under the brim. **Mark:** the sou'wester brim, long at the back. **Tell:** the drowned throw pose, trident overhead.
- **Build.** drowned, biped rig, no scale; parts: souwester (helm, brim long at the back), rope_coil; team: sash, pennant; stage 3 +tooth_necklace; skins tc_harpooner_s1/s2/s3.png, outer tc_harpooner_o.png; weapon model harpoon; items (stats only): leather_helmet, no chest, trident.
- **Portrait.** Tall +2.

### Trident Guard
Tidecallers | infantry (spear) | Rare | Trident Guard > Reef Warden > Tide-Sworn | `tc_tideguard`
- **Purpose.** The heavy river wall: braced on Hold it holds a ford against charges and beasts. Beats cavalry, Old Horn and the Brute; loses to archers, the Mortar and Shieldbreakers.
- **Look.** Upright, coral branching from both shoulders. Barnacle #CFCBBE, verdigris #4E7A6A, navy #1E2C44. Barnacled mail, scallop shell on the back, finned iron cap, team sash, trident. Gill slits. **Mark:** bleached coral rising from each shoulder. **Tell:** trident overhead when engaged.
- **Build.** drowned, biped rig, upright (scale legs [1,1.05,1]); parts: finned_cap (helm), coral_pauldrons (both arms), scallop_shell (on the back); team: sash, pennant; stage 3 +coral_crown; skins tc_tideguard_s1/s2/s3.png, outer tc_tideguard_o.png; weapon model coral_trident (three prongs); items (stats only): iron_helmet, chainmail_chestplate, trident.
- **Portrait.** Standard.

### Mist Caller
Tidecallers | support | Rare | Mist Caller > Fogweaver > Stormcaller | `tc_mist`
- **Purpose.** The anti-archer: a 4-block fog that slows and halves ranged hits into or out of it. Beats archer and ballista lines; loses to Hounds, Web Stalkers and Reapers.
- **Look.** Hooded robe to the ground, arms folded. Navy #1E2C44, verdigris #4E7A6A, sea-foam #CFF3E4 (eyes). Kelp-fringed hood, conch at the belt, team wrap. Grey face. **Mark:** a pale barnacle rune band round the hem. **Tell:** the evoker spell pose through the windup (`setSpell`), CLOUD particles.
- **Build.** evoker (`ArmyEvoker`), biped rig, robed (idleArms folded); parts: kelp_hood (helm), robe_skirt (to the ground; the rune band is skin paint), conch; team: wrap, pennant; stage 3 +mist_strips; skins tc_mist_s1/s2/s3.png, glow tc_mist_e.png; no weapon; no armour or item.
- **Portrait.** Standard.

### Kraken-touched Brute
Tidecallers | cavalry | Epic | no evolution | `tc_brute`
- **Purpose.** The line-sweeper: each hit also sweeps all within 3 for 70%, with knockback. Beats Commons and archer lines; loses to braced spears and Shieldbreaker focus fire.
- **Look.** Long, low, horned. Verdigris #4E7A6A, barnacle #CFCBBE, navy #1E2C44. Barnacled hide, coral spines, team saddle-cloth and pennant. White eyes. **Mark:** four 8 px tentacles hanging from the jaw. **Tell:** the ravager head-butt (status 4), SPLASH across the sweep.
- **Build.** ravager (`ArmyRavager`), hound rig with its own ravager boxes on a 128x128 sheet; parts: horns, jaw_tentacles (four, 8 px), coral_spines; team: saddle_cloth, pennant; skin tc_brute_s1.png; no weapon; no items.
- **Portrait.** Wide 28, from the front left.

### Admiral Vael, the Drowned Tide
Tidecallers | infantry | Legendary | no evolution | `tc_admiral`
- **Purpose.** The undertow: every 10 s she drags enemies within 7 three blocks toward her and slows them. Beats kiting archers and backline casters; loses to the Lord Marshal's banner and Sunder focus fire.
- **Look.** Tricorne, calf-length coat tails, anchor on the back. Navy #1E2C44, barnacle #CFCBBE, gilt #96783A. Officer's coat with team-cloth lining and sash, trident. Pale face, hair adrift. **Mark:** the 12 px tricorne. **Tell:** trident overhead, a SPLASH ring drawn inward from 7 blocks.
- **Build.** drowned, biped rig, no scale; parts: tricorne (helm, 12 px), coat_tails (to the calf), anchor (on the back); team: coat_lining with the sash (her one large cloth), pennant; skin tc_admiral_s1.png, outer tc_admiral_o.png, glow tc_admiral_e.png; weapon model admiral_trident; items (stats only): chainmail_helmet (was golden) under the tricorne, netherite_chestplate under the coat, trident.
- **Portrait.** Tall +4.

## 6. Removed at migration

### Dawn Oracle
Dawn | support | unlock, no evolution | `oracle`; removed in Phase 2, owners refunded 300 gold
- **Purpose.** A second mend caster with the Sunweaver's job and counters. Not worth splitting.
- **Look, build, portrait.** Unchanged: husk, `dawn_guard.png`, armory overlay, amethyst_shard; Standard.

### Dusk Wraith
Dusk | support | unlock, no evolution | `wraith`; removed in Phase 2, owners refunded 300 gold
- **Purpose.** A harder-hitting blight caster with the Gravecaller's job and counters. Not worth splitting.
- **Look, build, portrait.** Unchanged: wither_skeleton, vanilla texture, armory overlay, echo_shard; Standard.

## 7. Shared jobs and lookalikes

**Shared jobs.** Five pairs share a job; their purposes carry the split.
Squire and Hearthguard: advance or hold. Longbow and Ballista Crew (pierce):
mobile or dug in. Moot Hound and Web Stalker: open ground or walls. Grave
Reaper and Shieldbreaker: crowds or shields. Tusk Spear and Trident Guard
(`brace`): the cheaper, faster counter-charge for woods and flanks (38 gold,
0.31) or the heavier ford wall (34 HP, fords from Riptide II); if play still
blurs them, the Tusk Spear braces on Charge instead. The four Common bows
split by place: the Order Archer holds a line, the Bone Archer is cheap, the
Arbalist cracks shields, the Hide Hunter wants trees.

| Reads alike at 30 blocks | Fix |
|---|---|
| Squire / Templar | Iron cap and hip surcoat against great helm and knee tabard. |
| Order Archer / Bone Archer | Pale hood, full legs against dark hood, stick legs. |
| Squire / Hearthguard | Upright and pale against square, folded arms, slab pauldrons. |
| Risen / Drowned Raider | Grey coif and mail against a turtle shell on bare skin. |
| Bone Archer / Barrow Stalker | Sticks against a solid mantle; moss against frost grey. |
| Reaper / Gravecaller / Hollow King | Scythe; lantern and gilt mask; crown and lit ribs. |
| Tusk Spear / Trident Guard | Snout and crossbarred spear against coral and three prongs. |
| Hide Hunter / Tusk Spear / Seer | Feathers and bow; spear above the head; antler crown and hunch. |
| Hearthguard / Shieldbreaker | Two slab pauldrons against one anvil and bare arms. |
| Mist Caller / Ironclad | Same illager body: hood and long robe against helms and beards. |
| Old Horn / Brute / Brassjaw | Bear and rack; horned body and tentacles; golem and smokestacks. |
| Robed casters | Sunweaver white with a sun staff; Gravecaller black with a lantern; Mist Caller navy, arms folded. |
| Shared motifs | Antlers stay in Wildmoot, the lantern is the Gravecaller's, and among Graveborn and Legendaries the crown is the Hollow King's. |

**Today's 12** read as two shapes per side: one Dawn skin, one armory overlay
on every husk, skeleton and wither skeleton, identical Bone Ranger and Dusk
Stalker kits. A sheet-only step works now: the Stalker's Phase 2 chest (`air`)
and the Reaper's `iron_hoe`.

## 8. House palettes and the team tint rule

| House | Colour 1 | Colour 2 | Colour 3 | Light | UI accent |
|---|---|---|---|---|---|
| Gilded Order | enamel #D9D3C1 | steel #7C8591 | gilt #96783A | sun-white #F2E6C2 | #D8B45A |
| Graveborn | bone #C9C1A8 | grave-moss #4F5A3C | grave-iron #2B2826 | soul-light #CFEFFF | #3E5240 |
| Wildmoot | russet #7A4127 | bark #4A3626 | clay #C8B48A | clay #C8B48A | #6E3A1F |
| Ironclad | soot iron #34373B | brass #7F6232 | scorched leather #4B3A2C | ember #E2702A | #4A5257 |
| Tidecallers | verdigris #4E7A6A | barnacle #CFCBBE | navy #1E2C44 | sea-foam #CFF3E4 | #2B4F73 |

A unit may swap one House colour for its own (linen, frost grey, chitin). UI
accents are the War Table tab colours, screens only; the Houses sheet's Gilded
accent (0xDFCE68) should become #D8B45A.

**Team tint rule** (Dawn and Dusk today, the four banner colours in Phase 2):
1. **Team is loud, House is quiet.** Side colours have saturation 0.5 or more
   and value 0.65 or more: Dawn #55FFFF, Dusk #AA00AA; gold #E3B341, violet
   #B07CFF, crimson #E0524A, teal #3FC7C0. House and unit colours have
   saturation 0.45 or less, or value 0.60 or less, or sit 45 degrees of hue
   from every side colour (the turtle shell). Golden armour, diamond, red
   spider eyes, WITCH particles, the amethyst shard and ember fail; ember
   appears only as a few lit pixels, inside the rule 5 caps.
2. **Same-hue pairs hold by value:** gilt (0.59) is a third darker than the
   gold side (0.89), verdigris (0.48) than teal (0.78).
3. **Team colour appears only in the rule 2 places;** the pennant glyph (sun,
   moon, star, wave) carries it for colour-blind players.
4. **Vanilla dyes** for shields and banners: Dawn light_blue, Dusk purple;
   gold yellow, violet purple, crimson red, teal cyan.
