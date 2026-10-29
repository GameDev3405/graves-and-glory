// Training Yard (Drillmaster) and the twin War Gates with the Steward's tower.
import { pave, STONE_PAVE, GRAVEL_PATH, bigBrazier, ironLamp, plinth, signAt, bannerAt, npcAt, propAt, emitAt, oak, spruce, flowers } from './kit.js';
import { hash } from './ctx.js';

/* ===================================================================== TRAINING YARD */
export function buildYard(ctx) {
  const w = ctx.w;
  w.beginZone('yard');
  const P = w.pen(-49, 0, 26, 3);      // front (gate) faces east
  // ground: packed earth with gravel lanes
  for (let x = -13; x <= 13; x++) for (let z = -11; z <= 11; z++) { const h = hash(x, z, 21); P.set(x, -1, z, h > 0.8 ? 'gravel' : h > 0.45 ? 'coarse_dirt' : h > 0.3 ? 'podzol' : 'dirt'); }
  for (let z = -2; z <= 11; z++) for (const x of [-1, 0, 1]) P.set(x, -1, z, 'gravel');
  // palisade
  for (let x = -13; x <= 13; x++) for (const z of [-11]) { const h = 2 + (hash(x, z, 4) > 0.5 ? 1 : 0); P.fill(x, 0, z, x, h, z, 'spruce_log'); }
  for (let z = -11; z <= 11; z++) for (const x of [-13, 13]) { const h = 2 + (hash(x, z, 5) > 0.5 ? 1 : 0); P.fill(x, 0, z, x, h, z, 'spruce_log'); if (hash(x, z, 6) > 0.8) P.set(x, h + 1, z, 'spruce_fence'); }
  for (let x = -13; x <= 13; x++) { if (Math.abs(x) <= 3) continue; P.fill(x, 0, 11, x, 2, 11, 'spruce_log'); }
  // palisade lamps so the yard is usable after dusk
  for (let x = -12; x <= 12; x += 6) { P.fill(x, 0, -10, x, 3, -10, 'spruce_fence'); P.set(x, 4, -10, 'lantern'); }
  for (const sgn of [-1, 1]) for (let z = -8; z <= 8; z += 8) { P.fill(sgn * 12, 0, z, sgn * 12, 3, z, 'spruce_fence'); P.set(sgn * 12, 4, z, 'lantern'); }
  for (const [x, z] of [[-4, -1], [4, 6], [-6, 6], [8, -3]]) { P.fill(x, 0, z, x, 3, z, 'spruce_fence'); P.set(x, 4, z, 'lantern'); }
  // START gate at the front (local +z)
  for (const x of [-3, 3]) { P.fill(x, 0, 11, x, 6, 11, 'stripped_spruce_log'); P.set(x, 7, 11, 'lantern'); P.set(x, 5, 11, 'orange_terracotta'); P.set(x, 2, 11, 'orange_terracotta'); }
  P.fill(-3, 6, 11, 3, 6, 11, 'spruce_planks'); P.fill(-2, 7, 11, 2, 7, 11, 'spruce_planks_slab');
  signAt(ctx, P, 0, 5.4, 11.55, 0, { text: 'START · BATTLE 1', sub: 'The Training Field · Rivergate Valley · the road west must open', w: 5.2, h: 1.1, style: 'wood', size: 0.55 });
  bannerAt(ctx, P, -4.4, 6.6, 11.4, 0, { w: 1.1, h: 3.4, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' });
  bannerAt(ctx, P, 4.4, 6.6, 11.4, 0, { w: 1.1, h: 3.4, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' });
  P.set(0, 5, 11, 'chain'); P.set(0, 4, 11, 'bell'); P.set(0, 6, 11, 'chain');
  // training dummies
  const dummy = (x, z) => { P.set(x, -1, z, 'spruce_planks'); P.fill(x, 0, z, x, 1, z, 'spruce_fence'); P.set(x, 2, z, 'hay_block'); P.set(x - 1, 2, z, 'spruce_fence'); P.set(x + 1, 2, z, 'spruce_fence'); P.set(x, 3, z, 'target'); P.set(x, 1, z - 1, 'spruce_planks_slab'.replace('_slab', '_slab')); };
  for (const [x, z] of [[-9, 4], [-5, 4], [-1, 4], [3, 0], [7, 0], [11, 0]]) dummy(x, z);
  // archery lane
  for (const x of [-8, -2, 4]) { P.fill(x, 0, -9, x, 1, -9, 'spruce_fence'); P.set(x, 2, -9, 'target'); P.fill(x, -1, -2, x, -1, -8, 'white_carpet'.replace('white_carpet', 'gravel')); }
  for (const x of [-8, -2, 4]) P.set(x, 0, 2, 'hay_block');
  // weapon racks
  for (const x of [8, 11]) { P.fill(x, 0, 8, x + 1, 0, 8, 'spruce_fence'); P.fill(x, 1, 8, x + 1, 1, 8, 'spruce_fence'); P.fill(x, 2, 8, x + 1, 2, 8, 'iron_bars'); }
  npcAt(ctx, P, 5.6, 0, 8.6, 0, { id: 'drillmaster' });
  // lesson boards along the north fence (local +x side ... inward facing)
  const lessons = [['LESSON 1 · COMMAND', 'R opens the war council. Pick a group, then an order: Move, Attack, Hold.'], ['LESSON 2 · ORDERS', 'G aims an attack. V recalls your whole army. Use / Backspace cancels.'], ['LESSON 3 · FORMATIONS', 'B cycles Compact, Line, Wedge, Loose. Brackets turn the facing.'], ['LESSON 4 · THE MAP', 'M opens the tactical map. Right-click to send; ping to call for aid.']];
  lessons.forEach(([t, s], i) => { const z = -8 + i * 5; P.fill(12, 0, z - 1, 12, 2, z - 1, 'spruce_fence'); P.fill(12, 0, z + 1, 12, 2, z + 1, 'spruce_fence'); signAt(ctx, P, 11.6, 2.0, z, -90, { text: t, sub: s, w: 3.4, h: 1.7, style: 'vellum', size: 0.4, wrap: true }); });
  // obstacle course on the south side: hurdles, mud, wall
  for (let z = -8; z <= 6; z += 3) { P.fill(-12, 0, z, -12, 1, z, 'spruce_log'); }
  for (const z of [-7, -4, -1, 2]) { P.fill(-11, 0, z, -9, 0, z, 'spruce_log'); }
  for (let x = -11; x <= -9; x++) for (let z = -6; z <= -2; z++) P.set(x, -1, z, 'brown_terracotta');
  P.fill(-12, 0, 6, -9, 3, 6, 'spruce_planks');
  // barracks hut
  for (let x = -12; x <= -8; x++) for (let z = 7; z <= 10; z++) { P.set(x, -1, z, 'spruce_planks'); }
  for (let y = 0; y <= 2; y++) { P.fill(-12, y, 7, -8, y, 7, y === 2 ? 'stripped_spruce_log' : 'spruce_planks'); P.fill(-12, y, 10, -8, y, 10, y === 2 ? 'stripped_spruce_log' : 'spruce_planks'); P.fill(-12, y, 7, -12, y, 10, 'spruce_planks'); P.fill(-8, y, 7, -8, y, 10, 'spruce_planks'); }
  P.gable(-12, 7, -8, 10, 2, 'z', 'spruce_planks_stairs', 'spruce_planks', 'dark_oak_log', 1);
  P.clear(-11, 0, 8, -9, 2, 9); P.clear(-8, 0, 8, -8, 1, 9);
  P.fill(-11, 0, 8, -11, 0, 9, 'red_carpet'); P.fill(-9, 0, 8, -9, 0, 9, 'red_carpet'); P.set(-10, 1, 9, 'lantern~');
  // hay + trees outside
  for (const [x, z] of [[-6, 9], [-5, 9], [-6, 10]]) P.set(x, 0, z, 'hay_block');
  signAt(ctx, P, 0, 2.7, -10.55, 0, { text: 'THE TRAINING YARD', sub: 'Drillmaster · learn command before the march', w: 4.6, h: 0.9, style: 'wood', size: 0.5 });
  emitAt(ctx, P, 0, 0.5, 0, { type: 'dust', w: 20, h: 2, n: 24 });
  ctx.reserve(-62, 12, -36, 42);
  ctx.zone({ id: 'yard', name: 'The Training Yard · Drillmaster', color: '#d89a30', rect: [-62, 12, -36, 42], y: [-1, 10], tour: { x: -36, y: 1, z: 26, yaw: 90, pitch: 6 },
    npc: 'drillmaster', what: 'TUTORIAL. The training yard is where a new commander learns: four lesson boards name the player\'s own keys, six straw dummies and an archery lane let them try orders, and the START arch launches campaign battle 1 (The Training Field). A commander who has not cleared battle 1 spawns here.',
    builder: ['Palisade of spruce logs 2-3 high around a 27 x 23 packed-earth yard (coarse dirt / podzol / gravel).', 'START arch: two 7-high stripped spruce posts with orange terracotta bands, spruce plank beam, hanging bell, navy banners and the battle sign.', 'Six dummies: fence post, hay block body, fence arms, target block head. Archery lane of three target blocks on posts, hay-bale firing marks.', 'Four lesson boards on fence posts along the north palisade; obstacle course (log hurdles, terracotta mud, plank wall) on the south side; small barracks hut.'] });
}

/* ===================================================================== WAR GATES */
export function buildGates(ctx) {
  const w = ctx.w;
  w.beginZone('gates');
  const P = w.pen(0, 0, 52, 0);        // local z grows southward; the court is at negative z
  const BR = 'stone_bricks', PA = 'polished_andesite';
  // apron + road leading down to the valley
  pave(P, -34, 3, 34, 12, -1, STONE_PAVE);
  pave(P, -5, 12, 5, 24, -1, GRAVEL_PATH);
  // main wall: z -3..3 (world 49..55), x -34..34, 11 high with battlements
  for (let x = -34; x <= 34; x++) for (let z = -3; z <= 3; z++) for (let y = -1; y <= 10; y++) { P.set(x, y, z, (y <= 0 && Math.abs(z) <= 3 && hash(x, y + z, 3) > 0.8) ? 'mossy_stone_bricks' : BR); }
  for (let x = -34; x <= 34; x++) for (let y = 0; y <= 10; y++) if (y === 10 && x % 2 === 0) P.set(x, 11, -3, BR);
  for (let x = -34; x <= 34; x++) { P.set(x, 11, -3, x % 2 ? 'air' : BR); P.set(x, 11, 3, x % 2 ? 'air' : BR); P.set(x, 10, -2, 'stone_bricks_slab'); }
  for (let x = -34; x <= 34; x += 8) { P.fill(x, 0, -4, x, 9, -4, PA); P.set(x, 10, -4, 'chiseled_stone_bricks'); P.set(x, 11, -4, 'lantern'); }
  // cut the walkway on top, banners
  P.clear(-33, 11, -2, 33, 12, 2); P.fill(-34, 10, -3, 34, 10, 3, BR);
  for (let x = -34; x <= 34; x++) { P.set(x, 11, -3, x % 2 ? 'stone_bricks_wall' : BR); }
  // the two gates: Dawn (west, teal) and Dusk (east, violet)
  const gate = (cx, glass, team, emblem, bg, trim, label) => {
    P.clear(cx - 3, 0, -3, cx + 3, 6, 3); P.fill(cx - 3, -1, -3, cx + 3, -1, 3, 'polished_deepslate');
    for (let i = 0; i < 3; i++) { const y = 7 + i, open = 3 - i; P.clear(cx - open, y, -3, cx + open, y, 3); for (let z = -3; z <= 3; z++) { P.set(cx - open - 1, y, z, 'stone_bricks_stairs@W'); P.set(cx + open + 1, y, z, 'stone_bricks_stairs@E'); } }
    for (let z = -3; z <= 3; z++) for (let x = cx - 1; x <= cx + 1; x++) P.set(x, 10, z, x === cx ? 'chiseled_stone_bricks' : PA);
    // jambs
    for (const x of [cx - 4, cx + 4]) { P.fill(x, 0, -4, x, 8, -4, PA); P.set(x, 9, -4, 'brass_block'); }
    for (const x of [cx - 4, cx + 4]) P.fill(x, 0, -3, x, 6, -3, 'chiseled_stone_bricks');
    // the gate itself: tinted glass membrane in the middle of the passage
    for (let y = 0; y <= 6; y++) for (let x = cx - 3; x <= cx + 3; x++) P.set(x, y, 0, glass + '_stained_glass');
    for (let y = 7; y <= 9; y++) for (let x = cx - (9 - y) + 0; x <= cx + (9 - y); x++) if (Math.abs(x - cx) <= 9 - y - 0) P.set(x, y, 0, glass + '_stained_glass');
    for (const x of [cx - 2, cx + 2]) { P.set(x, 7, -2, 'chain'); P.set(x, 6, -2, 'lantern~'); }
    bannerAt(ctx, P, cx, 13, -4.6, 180, { w: 2.6, h: 4.6, emblem, bg, trim, kind: 'wall' });
    signAt(ctx, P, cx, 9.4, -4.55, 180, { text: label, sub: 'stand on the pad to ready', w: 4.8, h: 0.95, style: 'gold', size: 0.55 });
    emitAt(ctx, P, cx, 3.5, 0, { type: 'sparkle', w: 5, h: 7, n: 30, color: team });
  };
  gate(-14, 'cyan', 0x6fe8ff, 'sun', 0x1d4f4c, 0xc9a24a, 'DAWN GATE');
  gate(14, 'purple', 0xc89cff, 'moon', 0x563a73, 0xc9a24a, 'DUSK GATE');
  // ready pads in front of each gate
  const pad = (cx, tint, lantern) => {
    P.disc(cx, -1, -9, 5, 'polished_deepslate'); P.disc(cx, -1, -9, 4, tint + '_concrete'); P.ring(cx, -1, -9, 3, 'brass_block'); P.disc(cx, -1, -9, 1, tint + '_concrete'); P.set(cx, -1, -9, 'sea_lantern');
    for (const [dx, dz] of [[4, 0], [-4, 0], [0, 4], [0, -4]]) P.set(cx + dx, -1, -9 + dz, 'sea_lantern');
    for (const [dx, dz] of [[5, 0], [-5, 0]]) { P.fill(cx + dx, 0, -9 + dz, cx + dx, 4, -9 + dz, 'stone_bricks'); P.set(cx + dx, 5, -9 + dz, 'lantern'); }
    P.fill(cx - 5, 5, -9, cx + 5, 5, -9, 'dark_oak_planks_slab');
    for (let x = cx - 4; x <= cx + 4; x++) P.set(x, 6, -9, tint + '_wool');
    for (let x = cx - 3; x <= cx + 3; x++) P.set(x, 7, -9, tint + '_wool'); P.set(cx, 8, -9, 'gold_block');
  };
  pad(-14, 'cyan'); pad(14, 'purple');
  signAt(ctx, P, -14, 0.5, -5.6, 180, { text: 'DAWN · READY PAD', sub: '4 commanders per side · a party stands together', w: 3.2, h: 0.8, style: 'iron', size: 0.45 });
  signAt(ctx, P, 14, 0.5, -5.6, 180, { text: 'DUSK · READY PAD', sub: '4 commanders per side · a party stands together', w: 3.2, h: 0.8, style: 'iron', size: 0.45 });
  // central map board: five vote pedestals with miniature battlefields
  const dioramas = [
    ['RIVERGATE VALLEY', 'blue_concrete', 'moss_block', (x, z) => { if (Math.abs(z) <= 0) { P.set(x, -1, z, 'blue_concrete'); } P.set(x, 0, z, Math.abs(z - 0) < 1 && Math.abs(x - 0) < 1 ? 'spruce_planks_slab' : 'air'); }],
    ['IRONHOLD SIEGE', 'cobblestone', 'cobblestone'],
    ['FROSTMARCH PASS', 'snow_block', 'snow_block'],
    ['HOLLOWWOOD REACH', 'podzol', 'podzol'],
    ['KINGSFIELD CROSSROADS', 'moss_block', 'moss_block'],
  ];
  const xs = [-8, -4, 0, 4, 8];
  dioramas.forEach(([name, base], i) => {
    const x = xs[i], z = -5;
    P.fill(x - 1, -1, z - 1, x + 1, -1, z + 1, 'polished_andesite'); P.fill(x - 1, 0, z - 1, x + 1, 0, z + 1, base);
    // little features
    if (i === 0) { for (let dz = -1; dz <= 1; dz++) P.set(x, 0, z + dz, 'blue_concrete'); P.set(x, 1, z, 'spruce_planks_slab'); P.set(x + 1, 1, z - 1, 'oak_leaves'); }
    if (i === 1) { for (const [dx, dz] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) P.set(x + dx, 1, z + dz, 'stone_bricks_wall'); P.fill(x - 1, 1, z, x + 1, 1, z, 'stone_bricks_slab'); P.set(x, 2, z, 'lantern'); }
    if (i === 2) { P.set(x - 1, 1, z, 'spruce_leaves'); P.set(x + 1, 1, z + 1, 'spruce_leaves'); P.set(x, 1, z - 1, 'snow_layer'); P.set(x + 1, 0, z, 'ice'); }
    if (i === 3) { P.set(x - 1, 1, z, 'dark_oak_fence'); P.set(x - 1, 2, z, 'dark_oak_leaves'); P.set(x + 1, 1, z, 'dark_oak_fence'); P.set(x + 1, 2, z, 'dark_oak_leaves'); P.set(x, 1, z, 'cobweb'); }
    if (i === 4) { for (let d = -1; d <= 1; d++) { P.set(x + d, 0, z, 'gravel'); P.set(x, 0, z + d, 'gravel'); } P.set(x, 0, z, 'gold_block'); for (const [dx, dz] of [[-1, -1], [1, 1]]) P.set(x + dx, 1, z + dz, 'coarse_dirt'); }
    P.set(x, 0, z + 2, 'lectern');
    signAt(ctx, P, x, 2.4, z + 2.4, 180, { text: name, sub: 'vote', w: 2.4, h: 0.7, style: 'wood', size: 0.36 });
  });
  signAt(ctx, P, 0, 3.6, -2.3, 180, { text: 'BATTLE MAP', sub: 'vote for the next battlefield · the countdown starts when both pads are ready', w: 6, h: 1.1, style: 'gold', size: 0.55 });
  // hourglass countdown over the map board
  P.fill(-1, 6, -2, 1, 6, -2, 'brass_block'); P.fill(-1, 13, -2, 1, 13, -2, 'brass_block');
  for (let y = 7; y <= 12; y++) { const hw = Math.abs(y - 9.5) < 0.6 ? 0 : (Math.abs(y - 9.5) < 1.6 ? 1 : 1); for (let x = -hw; x <= hw; x++) P.set(x, y, -2, y >= 9 && y <= 10 ? (x === 0 ? 'sand' : 'glass') : 'glass'); }
  P.set(0, 8, -2, 'sand'); P.set(0, 12, -2, 'sand'); P.set(0, 11, -2, 'sand'); P.set(0, 9, -2, 'sand');
  P.fill(0, 7, -2, 0, 7, -2, 'sand');
  emitAt(ctx, P, 0, 9, -2, { type: 'sparkle', w: 1, h: 2, n: 8, color: 0xffe9a0 });
  // towers
  const tower = (cx, side) => {
    P.disc(cx, -1, 0, 6, 'polished_andesite');
    P.ring(cx, 0, 0, 5, BR, 17, 1);
    for (let a = 0; a < 360; a += 5) { const r = a * Math.PI / 180, x = Math.round(cx + Math.cos(r) * 5), z = Math.round(Math.sin(r) * 5); if (a % 30 === 0) for (let y = 7; y <= 9; y++) P.set(x, y, z, 'glass_pane'); if (hash(a, 2, 1) > 0.9) P.set(x, 4, z, 'mossy_stone_bricks'); }
    P.ring(cx, 18, 0, 6, 'stone_bricks_slab', 18, 1); P.ring(cx, 17, 0, 6, BR, 17, 1);
    for (let a = 0; a < 360; a += 12) { const r = a * Math.PI / 180; P.set(Math.round(cx + Math.cos(r) * 6), 18, Math.round(Math.sin(r) * 6), BR); }
    for (let i = 0; i <= 9; i++) { const rr = 6 - Math.floor(i * 0.65); P.ring(cx, 18 + i - 1 + 1, 0, Math.max(rr, 0), i % 3 === 2 ? 'cut_brass' : (side > 0 ? 'purple_concrete' : 'cyan_concrete'), 18 + i, 2); if (rr <= 1) P.disc(cx, 18 + i, 0, Math.max(rr, 0), 'cut_brass'); }
    P.fill(cx, 28, 0, cx, 32, 0, 'iron_bars'); P.set(cx, 33, 0, 'gold_block');
  };
  tower(-27, -1); tower(27, 1);
  // west tower: the Horn tower (bell chamber open at the top)
  P.clear(-31, 12, -2, -23, 16, 2); P.set(-27, 14, 0, 'bell');
  for (const [dx, dz] of [[-1, 0], [1, 0], [0, -1], [0, 1]]) P.set(-27 + dx, 14, dz, 'air');
  P.set(-27, 15, 0, 'chain'); propAt(ctx, P, -27.0, 11.0, 4.2, 180, { type: 'horn' });
  // east tower: the Steward's office, open to the court
  P.clear(23, 0, -4, 31, 6, 4);
  P.fill(22, -1, -5, 32, -1, 5, 'dark_oak_planks');
  P.fill(23, 0, -4, 23, 6, -4, 'air');
  for (let x = 23; x <= 31; x++) for (let z = -6; z <= -4; z++) for (let y = 0; y <= 6; y++) P.set(x, y, z, 'air');
  P.clear(24, 0, -4, 30, 5, -1);
  P.fill(23, 0, -5, 31, 0, -5, 'air');
  // desk, boards, rooms
  P.fill(24, 0, -3, 30, 0, -3, 'dark_oak_planks_slab'); for (const x of [24, 27, 30]) P.set(x, -0, -2, 'barrel'); P.set(26, 1, -3, 'candle'); P.set(29, 1, -3, 'cartography_table'.replace('cartography_table', 'candle')); P.set(27, 0, -3, 'cartography_table'); P.set(28, 0, -3, 'lectern');
  npcAt(ctx, P, 27.5, 0, -1.8, 180, { id: 'steward' });
  signAt(ctx, P, 24.4, 3.0, 1.3, 180, { text: 'PARTY ROOMS', sub: 'Create a room, share the code, up to 4 per party', w: 2.4, h: 1.5, style: 'vellum', size: 0.4, wrap: true });
  signAt(ctx, P, 27.5, 3.0, 1.3, 180, { text: 'ROOM  K7QX', sub: 'host: Maud · 3 / 4 · 2v2 · Kingsfield', w: 2.4, h: 1.5, style: 'vellum', size: 0.4, wrap: true });
  signAt(ctx, P, 30.5, 3.0, 1.3, 180, { text: 'INVITE A FRIEND', sub: 'friends online · accept, then ready together', w: 2.4, h: 1.5, style: 'vellum', size: 0.4, wrap: true });
  for (const x of [24, 27, 30]) { P.fill(x - 1, 0, 1, x - 1, 3, 1, 'dark_oak_fence'); }
  P.fill(23, 0, 1, 23, 6, 1, 'stone_bricks'); P.fill(31, 0, 1, 31, 6, 1, 'stone_bricks'); P.fill(23, 6, 1, 31, 6, 1, 'stone_bricks');
  for (const x of [25, 29]) { P.set(x, 6, -2, 'chain'); P.set(x, 5, -2, 'lantern~'); }
  signAt(ctx, P, 27.5, 8.2, -4.6, 180, { text: 'THE STEWARD', sub: 'parties · rooms · invites', w: 4.0, h: 0.9, style: 'gold', size: 0.5 });
  bannerAt(ctx, P, 22, 7, -4.6, 180, { w: 1.0, h: 3.0, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' });
  // braziers + lamps along the wall front, flowers
  for (const x of [-22, -6, 6, 22]) bigBrazier(P, x, 0, -6, false);
  for (let x = -33; x <= 33; x += 3) if (Math.abs(x) > 8 && Math.abs(Math.abs(x) - 14) > 5 && Math.abs(x) < 24) { P.set(x, -1, -6, 'moss_block'); flowers(w, x, 0, 46); }
  ctx.reserve(-36, 38, 36, 62);
  ctx.zone({ id: 'gates', name: 'The War Gates', color: '#6fe8ff', rect: [-36, 38, 36, 62], y: [-1, 35], tour: { x: 0, y: 1, z: 36, yaw: 180, pitch: 8 },
    npc: 'steward', what: 'MATCHMAKING. Twin gates face the battlefields: the Dawn gate (teal) in the west, the Dusk gate (violet) in the east, each with a ready pad. A party must stand together on one pad; when both sides are ready the countdown (hourglass) runs and the gates open. Five map pedestals between the gates vote on the battlefield (Rivergate, Ironhold, Frostmarch, Hollowwood, Kingsfield), each a tiny diorama. The east tower is the Steward\'s office for parties and rooms; the west tower is the horn tower.',
    builder: ['Wall 69 x 7 deep x 11 high in stone bricks, battlement walkway, two 7-wide pointed arches (stair blocks) each filled with stained glass: cyan for Dawn, purple for Dusk.', 'Ready pads: radius-5 discs, polished deepslate rim, concrete in team colour, brass ring, sea lanterns; overhead wooden arch with team wool.', 'Map board: five 3x3 polished andesite plinths with miniature landscapes, lectern each with a vote sign.', 'Towers radius 5, 17 high with conical roofs in team concrete and brass bands; Steward\'s office open on the court side with a desk, cartography table and three notice boards.'] });
}
