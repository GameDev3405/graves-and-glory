# Troop depth: sprite weapons and the relief layer

Version 1, 2026-10-21. Design for plan Task 5d (`docs/superpowers/plans/2026-10-17-phase-2-houses.md`), an addendum to
the unit identity spec and to the Task 5 pipeline. Kort's wish (2026-10-21 evening): the mod's own troops and their weapons
get 3D depth, made entirely by us. No Skin Layers 3D, no other dependency, no copied code. Every decision below is made;
the three open choices are in section 11, each with the option this design already uses.

## 1. In short

- **Weapons become extruded pixel sprites**, built by `tools/build_skins.py`. Each one has a front face, a back face and a
  side face along every pixel edge that borders transparency, with its own thickness, a grip pixel and an optional glow.
  Bows add three pull frames, picked by the time in the `bow_draw` clip. One mesh per weapon and frame is shared by every
  troop that carries it. Weapons always draw the full mesh (no distance cut), because a weapon is often the troop's mark.
- **A relief layer per troop and stage.** This is a second texture on the troop's own UV layout. Its opaque texels become
  one-texel voxels standing on the face they belong to. The build pastes the relief's paint into the shipped stage skin,
  and builds one mesh per bone with only the exposed faces, merged. The mesh moves with its bone through every clip.
- **Within 24 blocks of the camera**, the 48 nearest troops draw their relief. Every other troop draws flat. Flat means
  the same paint on the same skin, so the only change at the switch is that the 1 px sides appear. It costs nothing.
- **Budget:** at most 1.0 ms more world render per frame with the camera inside the 152-troop melee, and at most 0.3 ms
  at the fixture's commander camera (section 5).

## 2. Sprite weapons

**Source** `art/weapons/<id>.json`. The keys are only these:

| key | value |
|---|---|
| `weapon` | the id (the file name) |
| `thickness` | 1, 2 or 3 model px: how far the sprite is extruded |
| `grip` | `{pixel [x,y], offset [x,y,z]?, rotation [pitch,yaw,roll]?}`. The centre of the grip pixel sits on the hand bone's pivot, through the middle of the fist (Task 5b fix round). `offset` and `rotation` place the sprite in the hand bone's frame, as the old box grip did. |
| `pull` | draw weapons only: the three times, in seconds of `bow_draw`, at which `pull_0`, `pull_1` and `pull_2` begin. They rise strictly and lie between 0 and 1.2. |
| `nocks` | with `pull`: the middle of the string in sprite pixels, one entry per frame (rest, pull_0, pull_1, pull_2) |
| `note` | free text |

The art files sit beside the JSON:
- `<id>.png` is the rest frame.
- `<id>_pull_0.png` to `_pull_2.png` are the pull frames (with `pull`). They are the same size as the rest frame.
- `<id>_e.png` is the glow, for the rest frame only, and never on a weapon with `pull`.

A frame is at most 48x48. One sprite pixel is one model pixel, the same texel density as the skins. The sprite's x
runs right and its y down, as the weapon frame's +x and +y. The thickness runs along z, centred on the sprite plane.
A sword whose flat should face the troop's side is turned there by its grip rotation (yaw 90).

**Generated** `models/troop/weapon/<id>.json`. `FORMAT` becomes 2, because the weapon files change shape and the troop
bones gain `relief`. Its keys are:
- `format`, `weapon`, `size` (the packed sheet: frame width times the frame count, by the frame height), `thickness`;
- `grip {offset, rotation}`, `pull` (`[]` when the weapon has none);
- `frames[]` of `{name, nock, quads}`. `name` is `rest`, then `pull_0` to `pull_2`. `nock` is in model px in the weapon
  frame, or null;
- `texture` and `glow`.

The texture `weapon/<id>.png` packs the frames from left to right in that order. `weapon/<id>_e.png` is the same size
and lit only in the rest column. The generated files hold none of the vanilla block-model keys.

**Mesh.** The build cuts the mesh at build time, the way vanilla's generated item model does:
- The first two quads of a frame are its two big faces. Each covers the whole frame; the cutout layer drops the
  transparent texels.
- Then come the side faces, one along every edge between an opaque pixel and transparency or the frame's border. Faces
  on one line merge into runs. Each run is `thickness` deep.
- A side face samples the edge pixel it stands on, through a sliver of UV half a texel across, at the texel's centre
  (section 6).
- A frame may have at most 128 quads. The build fails above that, naming the weapon and the frame.
- Expected sizes: 20 to 40 quads for a sword, 40 to 70 for a bow frame, 20 to 40 for the sun staff. Today's box weapons
  draw 24 to 60 quads (4 to 10 boxes), so weapons cost about the same as before.

**Pull frames.** `TroopFeatures.Weapon` picks the frame each draw with `Weapon.frameAt(pull, seconds, loose)`:
- `seconds` is the time the live `bow_draw` play shows (`TroopAnimator.drawSeconds`), or -1 when nothing draws.
- `loose` is the rig's loose time (`TroopClips.BOW_TIMES`, biped 0.8 s).
- The rest frame shows below `pull[0]`, from the loose on, and when nothing draws. Otherwise the last pull whose start
  has passed shows.

For the biped clip (nock at 0.15 s, full draw at 0.65 s, held, loose at 0.8 s) the Gilded bows use `pull: [0.15, 0.32, 0.48]`.
So the string comes back with the drawing hand, and it snaps straight on the frame the arrow leaves. A crossbow, a harpoon
or any other draw weapon of a ranged troop that is not magic may use `pull` the same way.

**Bow at rest.** `TroopAnimator.twist` turns the bow 45 degrees at rest (`BOW_REST_YAW = 45`; today 90), with the string
to the back and inward. A flat sprite reads its curve only across its plane. At 45 degrees the curve reads at 71% from the
front and from the side, and nearly in full in the portrait's 30-degree view. At the draw the hand still turns the bow
upright into the plane of the shot. Bows are 2 px thick (deeper than wide, like a real limb), so the two views where the
bow is edge-on still show a lit limb, not a hairline.

The six Gilded weapons and their thickness:

| weapon | thickness |
|---|---|
| arming_sword | 1 |
| longsword | 1 |
| marshal_sword | 1 |
| order_bow | 2, with pull frames |
| longbow | 2, with pull frames |
| sun_staff | 2, with its glow on the disc |

## 3. The relief layer

**Source** `art/troops/<troop>_s<stage>_r.png`, one per own stage skin, plus `<troop>_veteran_r.png` beside a Veteran
skin. It is the troop's size and on the troop's sheet layout, so rig boxes and non-team parts are covered.
- Alpha is 0 or 255 only. An opaque texel is raised, and its colour is the paint of the raised texel.
- Once a troop has one relief file, every own skin needs one. The build fails otherwise.
- A troop with no relief files draws as it does today.

**Generated.**
- The shipped `<troop>_s<stage>.png` is the source skin with the relief's opaque texels pasted over it. The flat skin
  therefore already shows the relief's paint, and the relief source itself is never shipped.
- Each skin bone of the model file gains `relief: {"<key>": [quads]}`. The keys are `"1"`, `"2"`, `"3"` and `"veteran"`,
  and only bones with quads get the key.
- A draw uses the relief of the skin it binds (`TroopModel.reliefKey`: `veteran`, or the stage whose skin file draws).
- The top-level model keys stay exactly as Task 5 lists them.

**Quad format** (relief and weapons alike): 13 numbers per quad, all quads axis-aligned in their bone's (or weapon's)
frame.
- First, the direction index in Minecraft's `Direction` order: 0 down (-y), 1 up (+y), 2 north (-z), 3 south (+z),
  4 west (-x), 5 east (+x), in model space.
- Then the quad's min and max corners in model px (equal on the direction's axis).
- Then the texel coordinates of its corners 0, 1 and 3. Corner 2 is corner 1 + corner 3 - corner 0, so any orientation
  or mirror is exact.
- Corners run counter-clockwise seen from the side the normal points to (`TroopMesh.corners`), since culling is on.
- The loader divides texels by the sheet size and pixels by 16, as `ModelPart.Cuboid` does.
- A relief stage of 200 quads is about 15 KB of JSON.

**Build of one bone's relief mesh:**
1. **Texel to voxel.** Each raised texel lies on one face of one skin box of the bone; the build fails if it lies on
   none. The texel maps to its square on the face by the 1.20.1 `ModelPart.Cuboid` UV rule, mirror included, on the box's
   dilated corners. The voxel stands one texel deep along the face's normal: 1 px times the box's scale on that axis
   (for example 1.15 px on Aldric's body front). A box with no depth (size 0 on an axis) carries no relief.
2. **One grid per box.** Every box gets an occupancy grid of (w+2) x (h+2) x (d+2) cells in texel units. The box fills
   the inside, and the voxels fill the one-cell shell around it. Faces between filled cells, and faces against the box,
   are never drawn. So there are no voxel bottoms and no faces inside a band.
3. **Corners.** An edge cell of the shell fills when the two face cells beside it are both raised. A corner cell fills
   when its three edge cells are filled. A band that wraps a box edge is then one solid with no notch. Edge and corner
   cells take the colour of the face texel they continue.
4. **Merge.** Faces on top of the voxels merge into maximal rectangles of raised texels on each face (greedy: along u,
   then v). Side faces merge into maximal runs along a row or column. Edge cells merge along their edge. A merged quad's
   UV is the straight continuation of its texels, so each texel shows exactly where its voxel stands. Side faces and
   corner faces use the half-texel sliver UV.
5. **Limits.** At most 256 quads per stage across all bones, and at most 20% of the texels of any one face. Expected:
   100 to 200 quads for a Gilded stage (a belt is about 5, a rivet or buckle 5, a trim band that wraps around a box 12
   to 16).

**The build fails**, naming the troop, the file and the texel, when a relief texel:
- lies on no skin box face;
- carries a hue within 45 degrees of a side colour above the tint limits (the tint rule runs on relief art too);
- sits on a lit glow texel (the voxel would hide it);
- sits under an opaque texel of the outer layer (drowned and strays);
- makes a voxel that, at rest, overlaps any box except its own. The one exception is a box of a part that can hide (a
  `helm` part, or a part limited to some stages), because then the relief shows when the part hides. This test catches
  team panels and flags, the inner faces of arms and legs, and every joint face (section 7).

## 4. Draw path and distance

**Sharing.** `TroopModelLoader` builds a `TroopMesh` per weapon frame and per bone and relief key. Every entity shares
them; nothing is built per entity or per frame.

**Relief.**
1. `TroopRenderer.render` asks `TroopDepth.near(e)`. When the answer is yes, it arms the model with the draw's relief key.
2. `TroopModel.render`, the skin pass, draws the parts as before. Then, when armed, it walks the bones in file order with
   the matrix stack, as `ModelPart.render` does (each part's `rotate`, skipping a hidden bone and everything under it).
   Each bone draws its relief mesh in its own frame, into the same vertex consumer as the skin.
3. The arm is spent by that one pass, so the glow pass never draws relief, and `TroopRenderer` clears it in `finally`.

The relief therefore shares the skin's texture, render layer, light, hurt and death overlay, outline and the
helm-off, fold and stage visibility of its bone. It adds no texture bind and no layer.

**Weapons.** `TroopFeatures.Weapon` draws the chosen frame at the hand path and then the grip. It uses
`RenderLayer.getEntityCutout(texture)`, and draws the same mesh again with `RenderLayer.getEyes(glow)` for a glow.

**Distance** (`TroopDepth`):
- `NEAR_BLOCKS = 24`. At 1080p and the default 70-degree view, a model pixel spans about 48/d screen px: 2 px at 24
  blocks. Beyond that a 1 px side face covers a screen pixel or less and reads as shimmer.
- Skin Layers 3D cuts at 12, which suits first-person player skins. Our cameras are the commander on the ground (2 to
  20 blocks) and above the field (the tick-cost fixture's commander camera sits 15 up and 24 back, about 26 blocks from
  the nearest troop).
- `NEAR_CAP = 48`: at most the 48 nearest troops draw relief in one frame.
- `TroopDepth.beginFrame`, on `WorldRenderEvents.START`, works out the frame's cutoff. It takes the squared distances of
  the named mobs to the game camera and keeps the 48th smallest below 24 squared. That is one pass over the world's
  entities, a few microseconds.
- The camera is the game camera, read once at START, so Iris's shadow pass and the main pass draw the same geometry.
- A dressed mob (every `ArmyCards` portrait) is always near, so the 36 px roster and 160 px reveal frames show depth.
- There is no hysteresis: the paint is identical either way, so the switch shows only as the 1 px sides appearing.

**Fallback beyond the cutoff:** flat, at no cost, because the relief paint is already in the skin. Weapons have no
distance step: their mesh is about as cheap as the boxes they replace, and a front-and-back-only sprite vanishes when it
is edge-on.

**Culling.** The cull box keeps its 0.25-block margin, which covers 1 px of relief. Weapon reach comes from the frames'
bounds through the grip (`Weapon.points()`, replacing `TroopModelLoader.gripPoints`).

## 5. Frame budget

Base (Task 5b, 152 troops): animator 0.16 ms a frame, world render 7.83 ms mean at the fixture's commander camera
(`RenderCost`).

| geometry | today | with depth, expected | cap |
|---|---|---|---|
| relief, one near troop | 0 | 100 to 200 quads | 256 quads |
| weapon, every troop | 24 to 60 quads | 20 to 70 quads | 128 quads per frame |
| troops drawing relief | 0 | all those within 24 blocks | 48 |
| added at a ground camera in the melee | 0 | about 5,000 to 9,600 quads (20,000 to 38,000 vertices) | 12,288 relief quads |

Cost estimates:
- A vertex through `VertexConsumer` costs on the order of 10 to 20 ns, so the expected cost is about 0.4 to 0.7 ms.
- At the commander camera no troop is within 24 blocks, so the added cost is only the weapons, about zero.
- Loading adds the mesh parse to each resource reload, a few ms. Memory is about 1.5 MB for 30 troops.

**Budget (`p2-depth-cost`).** The fixture runs twice: depth on, then `TroopDepth.enabled = false` (relief off, weapons
drawn with their two big faces only). The world render mean may differ by:
- at most **1.0 ms** at a ground camera (eye 1.6 above the centre point, inside the melee);
- at most **0.3 ms** at the fixture's commander camera.

A run with Iris, Sodium and Complementary Reimagined is report-only, because the GPU dominates there.

**If the budget fails**, cut in this order and rerun:
1. `NEAR_CAP` 48 to 32;
2. `NEAR_BLOCKS` 24 to 16;
3. the relief cap 256 to 160, with the art trimmed.

Each is a presentation constant.

## 6. Shaders (Complementary Reimagined on Iris and Sodium)

The test root uses the modpack's Iris 1.7.6, Sodium 0.5.13 and `ComplementaryReimagined_r5.5.1.zip`. With Skin Layers
3D, some side faces shade wrongly under shaders. We avoid it with five rules.

1. **True normals.** Every quad carries its own axis normal in its bone's frame, transformed by the entry's normal matrix
   as `ModelPart.Cuboid` does. A side face never borrows the normal of the face below it.
2. **No zero-area UVs.** Iris works out each quad's tangent from its UVs. A side face whose four corners sample one point
   gives a broken tangent and dark or glowing sides. So every side and corner face spans half a texel across the edge
   texel (centre plus or minus 0.25), and the full run along it. The build test asserts that every quad's UV area is
   above zero.
3. **One layer, quads only.** The relief draws into the skin's own `entity_cutout` consumer, four vertices per quad. It
   gets the same Iris program, vertex format, alpha test and shadow pass as the skin. Nothing is translucent, so nothing
   needs sorting.
4. **Winding.** Corners run counter-clockwise from outside, because the cutout layer culls back faces. The build checks
   that each quad's corner cross product points along its normal.
5. **One camera.** The distance comes from the game camera at `WorldRenderEvents.START` (section 4), so shadows match the
   model.

Weapons follow rules 1, 2 and 4 on their own cutout layer, and draw the glow through `getEyes`, which Iris treats as
emissive.

The checks run once bare and once with shaders on. The tester describes:
- `depth-shader-near` (the knight at 4 blocks);
- `depth-shader-lineup`;
- `depth-shader-weapons`.

Side faces must be lit to match their direction: no black or glowing sides, no flicker, and the relief in the shadow.

## 7. Corners and joints

Skin Layers 3D shows zigzags for two reasons. Each face's voxel layer is built on its own and a little larger than the
model, so two layers cross in a stair at every box edge. And voxels on limbs meet the body at the joints. We avoid both:

- **One grid per box, exact size.** Voxels sit on the box's own texel grid at the true texel size, with nothing
  enlarged. The faces of a box share one grid, so a band that crosses an edge is one solid, with the corner filled
  (section 3, steps 2 and 3).
- **No relief where bones meet.** The rest-pose overlap test forbids relief on faces that touch another bone, such as
  the top of an arm under the shoulder, the inner face of an arm against the body, the inner faces of the legs, and the
  underside of the head. Those are the faces that clip and stair when limbs swing. The shoulder and hip pivots sit inside
  the limb boxes, so voxels on the outer faces turn with their limb and never cross the body.
- **The relief template.** `build_skins.py --template <troop>` also writes `work/troops/<troop>_relief_template.png`.
  Faces where relief is allowed show in their hue at 40%. Faces where it is forbidden (joint faces, inner faces, faces
  under team parts or under parts that never hide) are hatched red. The painter sees the rule before painting.

## 8. Art rules

**Relief may raise:**
- rivets and studs, buckles and clasps;
- the edges of belts and straps;
- trim bands on plate and leather;
- rims: the helm brow, the visor slit rim, cuffs, boot tops;
- plate ridges, brooches and badges;
- seams on leather, sparingly;
- on undead, bone knuckles and ribs; on Tidecallers, barnacle and coral crust; on beasts, fur tufts of 2x1 at most.

**Relief never raises:**
- the face itself: eyes, mouths and noses (a helm's brow and a beard's lower edge may be raised);
- team cloth, or anything that touches a team part;
- glow pixels, the soles, joint faces or inner limb faces;
- relief on top of relief. The relief is never more than one texel deep.

**Tones.** Relief paint uses the troop's own 3 to 4 tones per material, with no new tone. The sides show the texel's
colour and only the light darkens them, so the art stays flat-shaded.

**Silhouette.** The outline may grow by 1 px only at isolated points. No raised band runs along a silhouette edge for
more than half that edge's length, unless the troop's Look line names that edge (a crest, a rim).

**Team cloth stays the loudest read.** The tint rule applies to relief paint, relief never touches a team part, and
nothing raised is brighter or more saturated than the team panel beside it.

**Readability at 36 px.** Portraits always draw relief. In the 36 px roster frame the relief must not add noise to the
face or the mark, and the mark (4 px, or a change of outline) stays the first read. This is graded on the cards at 36 px
and in the 160 px reveal frame.

**Sprites.** Weapons are flat-shaded at 3 to 4 tones per material, with their edges in the darkest tone. A bow's string
is one sprite pixel and moves only through the pull frames. Lit pixels count toward the troop's glow cap (6, or 16 on
Epics and Legendaries).

## 9. Risks and mitigations

| risk | mitigation |
|---|---|
| Side faces shade wrongly under Complementary | Section 6 rules; `p2-depth-uv` checks normals, winding and UV area; the shots with shaders on are described by the tester. |
| The relief UV disagrees with the face under it (mirror, dilation) | The mesher copies the Cuboid rule. `p2-depth-uv` compares every top quad's UV centre with the UV the bone's real cuboid face gives at that point, read by reflection (dev only, as Task 4's harness did). |
| Frame cost in a big melee | `NEAR_CAP`, the quad caps, the fallback order (section 5) and `p2-depth-cost`. |
| A pop at 24 blocks | The paint is identical, and only the 2 px sides appear; the tester describes the `depth-lod` row. |
| Relief hides the glow or pokes through the outer layer or team cloth | Build failures (section 3). |
| Relief clips other gear in motion (shield, quiver) | Judged on geared turnaround shots. Animation itself is judged on the bare biped (Kort, 2026-10-21). |
| The painter edits the shipped skin, not the source | `--verify` fails on any shipped skin that is not the source plus its relief. |
| The pull frame lags the hand | Frames follow the clip's own time, not wall time. `p2-depth-bow-nock`: at full draw the drawing hand is within 2 px of pull_2's nock. |
| The jar grows | About 1 MB of mesh JSON for 30 troops (70 relief keys at about 15 KB each). |
| Sodium, ImmediatelyFast or Iris's batched entity rendering reorder draws | The relief is in the skin's own layer and consumer, so reordering cannot separate them; the run with the modpack's mods covers it. |

## 10. Unit identity spec addendum line

To append to section 0, rule 5, as written; nothing else in the spec changes:

> Addendum 2026-10-21 (troop depth, `2026-10-21-troop-depth-design.md`): each own skin may carry a relief layer
> `<troop>_s<stage>_r.png` (and `_veteran_r`) whose opaque texels stand one texel proud of their face within 24 blocks.
> Weapon models are extruded pixel sprites, with pull frames for draw weapons. The Build lines' skin lists imply the
> relief files, and every other rule stands.

## 11. Needs Kort

1. **The bow's rest angle.** The design uses (a): the bow hangs at 45 degrees, string to the back and inward, so the
   curve reads from the front, the side and the portrait. (b) keeps today's string-to-the-back, which is edge-on from
   the front. (c) has the string toward the leg, the full curve from the front and edge-on from the side.
2. **Bow thickness.** The design uses (a): 2 px, deeper than wide, so the edge-on views still read. (b) is 1 px, like
   vanilla items.
3. **A player switch for troop depth** (Visuals screen). The design uses (a): none in Task 5d, and it is parked, because
   the near cap and the budget hold the cost. (b) adds an On/Off option now.
