// Service buildings of the lower court: Armory (Quartermaster), Muster Hall (Recruiter), Archive (Pell).
import { pave, STONE_PAVE, bigBrazier, ironLamp, plinth, signAt, bannerAt, npcAt, propAt, emitAt, oak, spruce, flowers, azaleaBush } from './kit.js';
import { hash } from './ctx.js';

/* ===================================================================== ARMORY */
export function buildArmory(ctx) {
  const w = ctx.w;
  w.beginZone('armory');
  const P = w.pen(-24, 0, 24, 3);      // front faces east (toward the avenue)
  const BR = 'stone_bricks', PL = 'dark_oak_planks', LOG = 'dark_oak_log';

  P.fill(-10, -1, -10, 10, -1, 10, 'stone_bricks'); pave(P, -10, -10, 10, 10, -1, STONE_PAVE);
  // rear storehouse: stone base + timber upper, gable roof (ridge along local x)
  P.fill(-9, 0, -10, 9, 0, -10, BR);
  for (let x = -9; x <= 9; x++) for (let y = 0; y <= 5; y++) { P.set(x, y, -10, y <= 1 ? BR : ((Math.abs(x) % 3 === 0) ? LOG : PL)); }
  for (const x of [-9, 9]) for (let z = -10; z <= 0; z++) for (let y = 0; y <= 5; y++) P.set(x, y, z, y <= 1 ? BR : ((z % 3 === 0) ? LOG : PL));
  for (let x = -9; x <= 9; x++) for (let y = 0; y <= 5; y++) { const door = Math.abs(x) <= 2 && y <= 3; if (!door) P.set(x, y, 0, y <= 1 ? BR : ((Math.abs(x) % 3 === 0) ? LOG : PL)); }
  P.clear(-8, 0, -9, 8, 5, -1); P.fill(-8, -1, -9, 8, -1, -1, 'dark_oak_planks');
  for (const x of [-6, 6]) for (const z of [-2]) { P.set(x, 3, 0, 'glass_pane'); P.set(x, 2, 0, 'glass_pane'); P.set(x - 1, 2, 0, 'glass_pane'); P.set(x + 1, 2, 0, 'glass_pane'); }
  P.gable(-9, -10, 9, 0, 5, 'x', 'dark_oak_planks_stairs', PL, LOG, 1);
  for (let z = -9; z <= -1; z++) for (const x of [-9, 9]) for (let y = 6; y < 6 + Math.min(z + 10, -z) - 0; y++) P.set(x, y, z, PL);
  // interior shelves, racks, hanging shields
  for (const x of [-8, 8]) for (let z = -9; z <= -2; z++) { P.set(x === -8 ? -8 : 8, 0, z, z % 2 ? 'barrel' : 'crate'); P.set(x === -8 ? -8 : 8, 1, z, 'crate'); }
  for (let x = -7; x <= 7; x++) { if (Math.abs(x) <= 1) continue; P.set(x, 0, -9, x % 2 ? 'barrel' : 'crate'); P.set(x, 1, -9, 'bookshelf'); P.set(x, 2, -9, 'bookshelf'); }
  for (const x of [-5, 5]) for (const z of [-4, -7]) { P.set(x, 5, z, 'chain'); P.set(x, 4, z, 'lantern~'); }
  for (let x = -7; x <= 7; x += 2) bannerAt(ctx, P, x, 4.8, -8.6, 0, { w: 0.9, h: 1.4, emblem: ['sun', 'skull', 'antler', 'anvil', 'trident', 'hollow'][Math.abs(x / 2 | 0) % 6], bg: [0xe9ecec, 0x2c3a2a, 0x7a3426, 0x1c1a20, 0x14506a, 0x1d2a5c][Math.abs(x / 2 | 0) % 6], trim: 0xc9a24a, kind: 'wall' });
  // shop counter and awning
  for (let x = -8; x <= 8; x++) { P.set(x, 0, 3, 'barrel'); P.set(x, 1, 3, 'spruce_planks_slab'); }
  P.set(0, 0, 3, 'air'); P.set(0, 1, 3, 'air'); P.fill(-1, 0, 3, 1, 0, 3, 'air');
  for (const x of [-8, 8]) P.fill(x, 0, 6, x, 3, 6, 'stripped_dark_oak_log');
  for (let x = -8; x <= 8; x++) for (let z = 3; z <= 6; z++) { const y = 4 - Math.floor((z - 3) / 2); P.set(x, y, z, (Math.floor((x + 8) / 2) % 2) ? 'white_wool' : 'blue_wool'); }
  for (const x of [-8, -4, 0, 4, 8]) P.fill(x, 0, 6, x, 3, 6, 'spruce_fence');
  for (let x = -8; x <= 8; x += 4) { P.set(x, 0, 6, 'dark_oak_fence'); P.set(x, 3, 5, 'lantern~'); }
  npcAt(ctx, P, 0.5, 0, 1.5, 0, { id: 'quartermaster' });
  // goods on the counter
  for (const x of [-6, -3, 3, 6]) P.set(x, 2, 3, ['candle', 'candle', 'candle', 'candle'][0]);
  P.set(-4, 2, 3, 'red_mushroom'.replace('red_mushroom', 'poppy'));
  // forge corner (north side of yard) + armour stands + weapon racks
  P.fill(5, -1, 6, 9, -1, 10, 'polished_blackstone_bricks');
  P.set(6, 0, 8, 'blast_furnace'); P.set(6, 1, 8, 'blast_furnace'); P.set(7, 0, 8, 'anvil'); P.set(8, 0, 8, 'grindstone'); P.set(8, 0, 9, 'smithing_table'); P.set(6, 0, 10, 'cauldron'); P.set(9, 0, 8, 'barrel');
  P.fill(5, 0, 7, 5, 3, 7, 'polished_blackstone_wall'); P.fill(9, 0, 10, 9, 4, 10, 'polished_blackstone_bricks'); P.fill(5, 4, 8, 9, 4, 10, 'polished_blackstone_bricks_slab');
  emitAt(ctx, P, 6, 2.3, 8, { type: 'ember', w: 0.6, h: 3, n: 14 }); emitAt(ctx, P, 7, 4.5, 9, { type: 'smoke', w: 1, h: 8, n: 12 });
  const stands = [['gilded', -7], ['graveborn', -3.5], ['wildmoot', 3.5], ['ironclad', 7]];
  for (const [set, x] of stands) { P.fill(Math.round(x) - 1, -1, 7, Math.round(x) + 1, -1, 9, 'polished_andesite'); propAt(ctx, P, x + 0.5, 0, 8.2, 0, { type: 'mannequin', set }); P.set(Math.round(x), 0, 10, 'spruce_fence'); }
  for (const x of [-9, 9]) { P.fill(x, 0, 8, x, 2, 8, 'spruce_fence'); }
  signAt(ctx, P, 0, 6.4, 6.55, 0, { text: 'THE ARMORY', sub: 'Quartermaster · equip your army, 20 slots', w: 5.2, h: 1.0, style: 'gold', size: 0.55 });
  bannerAt(ctx, P, -9, 5.4, 6.8, 0, { w: 1.2, h: 3.6, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' });
  bannerAt(ctx, P, 9, 5.4, 6.8, 0, { w: 1.2, h: 3.6, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' });
  ironLamp(P, -9, 0, 10, 5); ironLamp(P, 9, 0, -9, 5);
  ctx.reserve(-36, 12, -12, 36);
  ctx.zone({ id: 'armory', name: 'The Armory · Quartermaster', color: '#4a7ac8', rect: [-36, 12, -12, 36], y: [-1, 14], tour: { x: -12, y: 1, z: 24, yaw: 90, pitch: 6 },
    npc: 'quartermaster', what: 'SHOP + ARMY. The Quartermaster sells and equips: the player arranges their twenty deployment slots, buys gear and opens the collection here (this is the station that opens the war council\'s army page). A stone-and-timber storehouse behind a navy-and-white awning counter; four mannequins show each House\'s gear; a forge stands in the corner.',
    builder: ['Storehouse 19 x 11: stone-brick base 2 high, dark oak plank/log upper, gable roof of dark oak stairs, ridge of dark oak log.', 'Counter: barrels with spruce slabs; awning 17 x 4 of alternating blue and white wool stepping down; stripped dark oak posts.', 'Forge corner: blast furnaces, anvil, grindstone, smithing table, cauldron; blackstone brick chimney.', 'Mannequin row of four armour stands, one per House set.'] });
}

/* ===================================================================== MUSTER HALL */
export function buildMuster(ctx) {
  const w = ctx.w;
  w.beginZone('muster');
  const P = w.pen(26, 0, 24, 1);       // front faces west
  const R = 10;
  // floor disc with a five-pointed star
  P.disc(0, -1, 0, R + 1, 'polished_andesite');
  P.disc(0, -1, 0, R - 1, 'polished_deepslate');
  P.ring(0, -1, 0, R - 2, 'purple_concrete');
  const pts = []; for (let i = 0; i < 5; i++) { const a = -Math.PI / 2 + i * 2 * Math.PI / 5; pts.push([Math.round(Math.cos(a) * 7.4), Math.round(Math.sin(a) * 7.4)]); }
  for (let i = 0; i < 5; i++) { const a = pts[i], b = pts[(i + 2) % 5]; P.line(a[0], -1, a[1], b[0], -1, b[1], 'brass_block'); }
  P.disc(0, -1, 0, 3, 'blue_concrete'); P.ring(0, -1, 0, 3, 'brass_block');
  // pillars (8) + low balustrade + glass, entrance gap faces +z
  for (let i = 0; i < 8; i++) {
    const a = i * Math.PI / 4 + Math.PI / 8, x = Math.round(Math.cos(a) * 9), z = Math.round(Math.sin(a) * 9);
    P.fill(x, 0, z, x, 5, z, 'quartz_pillar'); P.set(x, 0, z, 'polished_andesite'); P.set(x, 6, z, 'chiseled_quartz_block'); P.set(x, 1, z, 'amethyst_block'); P.set(x, 5, z, 'brass_block');
    P.set(x, 7, z, 'lantern');
  }
  for (let a = 0; a < 360; a += 3) {
    const r = a * Math.PI / 180, x = Math.round(Math.cos(r) * 9), z = Math.round(Math.sin(r) * 9);
    const ang = ((a + 360) % 360), gap = Math.abs(ang - 90) < 24;   // +z side (south in local) is open
    if (gap) continue;
    if (P.get(x, 0, z) !== 0) continue;
    P.set(x, 0, z, 'deepslate_bricks_wall'); P.set(x, 1, z, 'purple_stained_glass_pane'); P.set(x, 2, z, 'blue_stained_glass_pane'); P.set(x, 3, z, 'purple_stained_glass_pane'); P.set(x, 4, z, 'blue_stained_glass_pane');
  }
  P.ring(0, 6, 0, 9, 'quartz_block', 6, 1);
  P.ring(0, 6, 0, 10, 'brass_block', 6, 1);
  // dome of alternating gores
  for (let y = 0; y <= 9; y++) for (let z = -10; z <= 10; z++) for (let x = -10; x <= 10; x++) {
    const d = Math.sqrt(x * x + y * y * 1.35 + z * z); // slightly flattened dome
    if (d <= 9.6 && d > 8.3 && y >= 0) { const gore = Math.floor(((Math.atan2(z, x) + Math.PI) / (2 * Math.PI)) * 16) % 2; P.set(x, 7 + y, z, gore ? 'blue_concrete' : 'purple_concrete'); }
  }
  for (let i = 0; i < 16; i++) { const a = (i + 0.5) / 16 * 2 * Math.PI; for (let y = 0; y <= 8; y++) { const rr = Math.sqrt(Math.max(0, 81 - y * y * 1.35)); const x = Math.round(Math.cos(a) * rr * 0.97), z = Math.round(Math.sin(a) * rr * 0.97); if (i % 2 === 0 && rr > 2) P.set(x, 7 + y, z, 'brass_block'); } }
  P.set(0, 16, 0, 'brass_block'); P.fill(0, 17, 0, 0, 19, 0, 'lightning_rod'.replace('lightning_rod', 'chain')); P.set(0, 20, 0, 'gold_block'); P.set(0, 21, 0, 'lantern');
  // the Roll: crystal altar
  P.disc(0, 0, 0, 2, 'crying_obsidian', 0); P.set(0, 1, 0, 'amethyst_block'); P.set(0, 2, 0, 'amethyst_block'); P.set(0, 3, 0, 'amethyst_cluster');
  for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) { P.set(dx, 1, dz, 'amethyst_cluster'); }
  for (const [dx, dz] of [[2, 0], [-2, 0], [0, 2], [0, -2]]) P.set(dx, 1, dz, 'candle');
  emitAt(ctx, P, 0, 3.4, 0, { type: 'sparkle', w: 2.5, h: 4, n: 26, color: 0xc89cff });
  // desk: pact table + lectern on the north side
  P.fill(-3, 0, -6, 3, 0, -6, 'dark_oak_planks_slab'); for (const x of [-3, 3]) P.set(x, -1, -6, 'dark_oak_log');
  P.set(-2, 1, -6, 'candle'); P.set(2, 1, -6, 'candle'); P.set(0, 0, -7, 'lectern');
  for (const x of [-5, 5]) P.fill(x, 0, -6, x, 1, -6, 'bookshelf');
  npcAt(ctx, P, 0.5, 0, -4.3, 180, { id: 'recruiter' });
  // house banners round the hall
  [['sun', 0xe9ecec], ['skull', 0x2c3a2a], ['antler', 0x7a3426], ['anvil', 0x1c1a20], ['trident', 0x14506a]].forEach(([emb, bg], i) => {
    const a = (-Math.PI / 2 + (i + 0.5) * 2 * Math.PI / 5) + Math.PI / 5 * 0; const x = Math.cos(a) * 8.4, z = Math.sin(a) * 8.4;
    if (Math.abs(((a * 180 / Math.PI) + 360) % 360 - 90) < 28) return;
    bannerAt(ctx, P, x, 5.6, z, (-(a * 180 / Math.PI) + 90 + 180) % 360, { w: 1.2, h: 3.4, emblem: emb, bg, trim: 0xc9a24a, kind: 'wall' });
  });
  for (const a of [0.3, 1.2, 2.2, 3.0, 4.0, 5.0]) { const x = Math.round(Math.cos(a) * 5), z = Math.round(Math.sin(a) * 5); P.set(x, 8 - 0, z, 'air'); }
  for (const [x, z] of [[4, 4], [-4, 4], [4, -4], [-4, -4]]) { P.set(x, 7, z, 'chain'); P.set(x, 6, z, 'chain'); P.set(x, 5, z, 'lantern~'); }
  // steps + forecourt
  for (let i = 0; i < 2; i++) for (let x = -3 - i; x <= 3 + i; x++) P.set(x, -1 + 0, 11 + i, i === 0 ? 'polished_andesite_stairs@N'.replace('@N', '@N') : 'polished_andesite');
  for (let x = -3; x <= 3; x++) P.set(x, -1, 12, 'polished_andesite');
  for (const x of [-4, 4]) { bigBrazier(P, x, 0, 11, false); }
  signAt(ctx, P, 0, 9.3, 10.6, 0, { text: 'THE MUSTER', sub: 'Recruiter · unlock new troops · the Roll', w: 4.6, h: 1.0, style: 'gold', size: 0.55 });
  ctx.reserve(14, 12, 38, 36);
  ctx.zone({ id: 'muster', name: 'The Muster · Recruiter', color: '#9a4ac8', rect: [14, 12, 38, 36], y: [-1, 24], tour: { x: 11, y: 1, z: 24, yaw: 270, pitch: 8 },
    npc: 'recruiter', what: 'NEW TROOPS. The Recruiter runs the Roll: a rolled troop (100 gold, no duplicates) permanently unlocks one of the 24 troops not yet owned. A domed rotunda with a glowing amethyst-and-crying-obsidian altar, a five-pointed brass star inlaid in the floor and the five House banners round the wall. The Recruiter waits behind the pact table.',
    builder: ['Rotunda radius 10: eight quartz pillars with amethyst bases, glass-pane balustrade (purple/blue), open entrance on the avenue side.', 'Dome of alternating blue / purple concrete gores with brass ribs, topped by a gold block and lantern.', 'Floor: polished deepslate, purple ring, brass five-point star, blue centre disc.', 'Altar: crying obsidian disc, two amethyst blocks, amethyst clusters, candles.'] });
}

/* ===================================================================== ARCHIVE */
export function buildArchive(ctx) {
  const w = ctx.w;
  w.beginZone('archive');
  const P = w.pen(52, 0, 22, 1);       // front faces west
  const R = 8, H = 13;
  P.disc(0, -1, 0, R + 2, 'polished_andesite');
  P.disc(0, -1, 0, R, 'dark_oak_planks');
  P.ring(0, -1, 0, R - 2, 'spruce_planks');
  // tower wall, double height, with windows and a west arched door
  P.ring(0, 0, 0, R, 'stone_bricks', H, 1);
  for (let a = 0; a < 360; a += 4) { const r = a * Math.PI / 180, x = Math.round(Math.cos(r) * R), z = Math.round(Math.sin(r) * R); const ang = (a + 360) % 360; if (Math.abs(ang - 90) < 18) continue; if (hash(a, 1, 3) > 0.9) P.set(x, 3, z, 'mossy_stone_bricks'); if (a % 24 === 0) { for (let y = 3; y <= 5; y++) P.set(x, y, z, 'glass_pane'); for (let y = 8; y <= 10; y++) P.set(x, y, z, 'orange_stained_glass_pane'.replace('orange', 'yellow')); } }
  // door (local +z side = south of the circle in local frame)
  for (let x = -2; x <= 2; x++) for (let z = R - 1; z <= R; z++) for (let y = 0; y <= 4; y++) P.set(x, y, z, 'air');
  for (let x = -1; x <= 1; x++) for (let z = R - 1; z <= R; z++) P.set(x, 5, z, 'air');
  for (const x of [-3, 3]) P.fill(x, 0, R, x, 6, R, 'polished_andesite'); for (let x = -2; x <= 2; x++) P.set(x, 5, R, x === 0 ? 'chiseled_stone_bricks' : 'polished_andesite_stairs@' + (x < 0 ? 'W' : 'E'));
  // upper floor ring (balcony) with bookshelves
  P.ring(0, 6, 0, R - 1, 'dark_oak_planks', 6, 3); P.ring(0, 6, 0, R - 4, 'dark_oak_fence', 7, 1);
  for (let a = 0; a < 360; a += 3) { const r = a * Math.PI / 180, x = Math.round(Math.cos(r) * (R - 1)), z = Math.round(Math.sin(r) * (R - 1)); const ang = (a + 360) % 360; if (Math.abs(ang - 90) < 20) continue; for (let y = 0; y <= 3; y++) P.set(x, y, z, y === 3 ? 'bookshelf' : 'bookshelf'); for (let y = 7; y <= 9; y++) P.set(x, y, z, 'bookshelf'); }
  // central Founders' Stone
  P.disc(0, 0, 0, 2, 'stone_bricks'); P.disc(0, 1, 0, 1, 'polished_andesite');
  for (let y = 1; y <= 6; y++) { const wd = y < 3 ? 1 : (y < 5 ? 1 : 0); for (let x = -wd; x <= wd; x++) for (let z = -wd; z <= wd; z++) P.set(x, y, z, hash(x, y + z * 3, 9) > 0.7 ? 'deepslate' : 'basalt'); }
  P.set(0, 7, 0, 'tuff'); for (const [x, y, z] of [[0, 3, 2], [0, 4, 2], [0, 5, 2]]) P.set(x, y, z, 'air');
  for (let y = 2; y <= 6; y++) { P.set(1, y, 1, 'deepslate'); P.set(-1, y, -1, 'deepslate'); }
  P.set(0, 3, 1, 'soul_lantern'); P.set(1, 5, 0, 'soul_lantern'); P.set(-1, 4, 0, 'soul_lantern');
  signAt(ctx, P, 0, 3.6, 2.02, 0, { text: 'THE CROWN CALLS THEM. THE CROWN SENDS THEM HOME. ONE HAND, ONE CROWN, ONE STANDARD, ON THE FIELD OF KINGS.', sub: 'the Founders\' Stone', w: 2.7, h: 2.2, style: 'stone', size: 0.2, wrap: true });
  emitAt(ctx, P, 0, 6.5, 0, { type: 'soul', w: 1.8, h: 6, n: 18 });
  // Pell's desk
  P.fill(-6, 0, 3, -3, 0, 3, 'dark_oak_planks_slab'); P.set(-4, 1, 3, 'candle'); P.set(-6, 1, 3, 'lectern'); P.set(-3, 1, 3, 'cartography_table'.replace('cartography_table', 'candle'));
  P.set(-5, 0, 4, 'spruce_planks_stairs@N'); P.fill(3, 0, 3, 6, 0, 3, 'dark_oak_planks_slab'); P.set(4, 1, 3, 'candle'); P.set(5, 1, 3, 'lectern');
  npcAt(ctx, P, -4.5, 0, 2.0, 0, { id: 'pell' });
  for (const [x, z] of [[-3, -4], [3, -4], [-3, 4], [3, 4]]) { P.set(x, 6, z, 'chain'); P.set(x, 5, z, 'chain'); P.set(x, 4, z, 'lantern~'); }
  P.set(0, 12, 0, 'chain'); for (let y = 8; y <= 11; y++) P.set(0, y, 0, 'chain'); P.set(0, 7, 0, 'soul_lantern~'.replace('~', '~'));
  // conical roof
  for (let i = 0; i <= 9; i++) { const rr = R + 1 - i; if (rr < 0) break; P.ring(0, H + 1 + i, 0, rr, i % 2 ? 'dark_oak_planks' : 'deepslate_tiles', H + 1 + i, 2); if (rr <= 1) P.disc(0, H + 1 + i, 0, rr, 'deepslate_tiles'); }
  P.disc(0, H + 10, 0, 0, 'oxidized_copper'); P.fill(0, H + 11, 0, 0, H + 14, 0, 'chain'.replace('chain', 'iron_bars')); P.set(0, H + 15, 0, 'gold_block');
  // ceiling disc under the roof (floor of roof space)
  P.disc(0, H, 0, R - 1, 'dark_oak_planks');
  P.set(0, 12, 0, 'air');
  // bell on a bracket by the door
  P.fill(R + 2, 0, 5, R + 2, 4, 5, 'dark_oak_fence'); P.set(R + 2 - 1, 4, 5, 'dark_oak_fence'); P.set(R + 2 - 1, 3, 5, 'bell');
  // contracts board outside
  for (const dx of [-5, -1, 3]) { P.fill(dx, 0, 11, dx + 2, 0, 11, 'dark_oak_planks_slab'.replace('_slab', '')); P.fill(dx, 1, 11, dx, 3, 11, 'dark_oak_fence'); P.fill(dx + 2, 1, 11, dx + 2, 3, 11, 'dark_oak_fence'); }
  P.fill(-5, 3, 11, 5, 3, 11, 'dark_oak_planks'); P.fill(-5, 1, 11, 5, 1, 11, 'air');
  signAt(ctx, P, -3, 2.1, 11.55, 0, { text: 'DAILY CONTRACTS', sub: 'win · hold · lose few — claim gold and XP', w: 2.6, h: 1.4, style: 'vellum', size: 0.42 });
  signAt(ctx, P, 1.2, 2.1, 11.55, 0, { text: 'WEEKLY CONTRACTS', sub: 'bigger work, bigger purses', w: 2.6, h: 1.4, style: 'vellum', size: 0.42 });
  signAt(ctx, P, 4.8, 2.1, 11.55, 0, { text: 'THE ROLL OF THE DEAD', sub: 'Pell writes them as they come', w: 2.0, h: 1.4, style: 'vellum', size: 0.4 });
  signAt(ctx, P, 0, 7.3, 9.45, 0, { text: 'THE ARCHIVE', sub: 'Pell the Archivist · contracts · the Founders\' Stone', w: 5.0, h: 1.0, style: 'gold', size: 0.5 });
  bannerAt(ctx, P, -4.2, 6.9, 9.3, 0, { w: 1.1, h: 3.4, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' });
  bannerAt(ctx, P, 4.2, 6.9, 9.3, 0, { w: 1.1, h: 3.4, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' });
  for (const x of [-7, 7]) ironLamp(P, x, 0, 11, 5, true);
  ctx.reserve(40, 8, 64, 36);
  ctx.zone({ id: 'archive', name: 'The Archive · Pell', color: '#8fb0d8', rect: [40, 8, 64, 36], y: [-1, 30], tour: { x: 38, y: 1, z: 22, yaw: 270, pitch: 6 },
    npc: 'pell', what: 'DAILY + WEEKLY CONTRACTS. Pell the Archivist keeps the contract boards (claimable gold and XP) and the Founders\' Stone, carved with the Horn\'s rule. A round stone tower with a bookshelf-lined reading ring on two floors, a conical dark-oak and deepslate roof, a bell by the door and three notice boards in front.',
    builder: ['Tower radius 8, wall 13 high in stone bricks (mossy streaks), arched west door, glass and yellow-stained windows.', 'Two storeys of bookshelves, upper balcony ring of dark oak planks with fence rail.', 'Founders\' Stone: 1-wide basalt/deepslate pillar 6 high with soul lanterns on its flank, on a stone-brick dais. Carve the three lines of the Horn\'s rule on a sign / item frames.', 'Conical roof of alternating dark oak and deepslate tile rings with an oxidised copper cap and gold finial.', 'Three notice boards (dark oak frame, 3 high) with paper-style signs.'] });
}
