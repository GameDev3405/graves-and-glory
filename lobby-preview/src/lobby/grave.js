// Graveborn: "the dead who answered the horn". A walled barrow-yard, open graves for plinths and a skull-faced mausoleum.
import { plinth, signAt, bannerAt, propAt, emitAt, deadTree } from './kit.js';
import { hash } from './ctx.js';

export function buildGrave(ctx) {
  const w = ctx.w;
  w.beginZone('graveborn');
  const P = w.pen(49, 6, -17, 1);                 // front faces west
  const MB = 'mossy_stone_bricks', BO = 'bone_block';

  /* ---- ground ---- */
  for (let x = -10; x <= 10; x++) for (let z = -9; z <= 10; z++) {
    const h = hash(x, z, 12);
    P.set(x, -1, z, h > 0.82 ? 'coarse_dirt' : h > 0.55 ? 'moss_block' : h > 0.3 ? 'podzol' : 'mossy_cobblestone');
  }
  // gravel path gate -> mausoleum
  for (let z = -3; z <= 10; z++) for (const x of [-1, 0, 1]) P.set(x, -1, z, hash(x, z, 5) > 0.7 ? 'cobblestone' : 'gravel');
  for (let z = -3; z <= 10; z++) for (const x of [-2, 2]) P.set(x, -1, z, 'mossy_stone_bricks');

  /* ---- perimeter: mossy wall + iron bars, soul-lit pillars ---- */
  for (let x = -10; x <= 10; x++) {
    for (const z of [10]) { if (Math.abs(x) <= 2) continue; P.set(x, 0, z, 'mossy_stone_bricks_wall'); P.set(x, 1, z, 'iron_bars'); P.set(x, 2, z, 'iron_bars'); }
  }
  for (const sgn of [-1, 1]) for (let z = -3; z <= 10; z++) { P.set(sgn * 10, 0, z, 'mossy_stone_bricks_wall'); P.set(sgn * 10, 1, z, 'iron_bars'); P.set(sgn * 10, 2, z, 'iron_bars'); }
  for (const x of [-10, -6, 6, 10]) for (const z of [10]) { P.fill(x, 0, z, x, 3, z, 'chiseled_stone_bricks'); P.set(x, 4, z, 'soul_lantern'); }
  for (const z of [-3, 2, 6]) for (const x of [-10, 10]) { P.fill(x, 0, z, x, 3, z, 'chiseled_stone_bricks'); P.set(x, 4, z, 'soul_lantern'); }
  // gate arch with skulls
  for (const x of [-3, 3]) { P.fill(x, 0, 10, x, 6, 10, 'deepslate_bricks'); P.set(x, 3, 10, BO); P.set(x, 7, 10, 'wither_skull'); P.set(x, 6, 10, 'chiseled_stone_bricks'); }
  for (let x = -2; x <= 2; x++) { P.set(x, 6, 10, 'deepslate_bricks'); P.set(x, 5, 10, 'iron_bars'); P.set(x, 4, 10, (x % 2) ? 'iron_bars' : 'air'); }
  P.set(0, 7, 10, 'deepslate_bricks'); P.set(0, 8, 10, 'skeleton_skull'); for (const x of [-2, 2]) P.set(x, 7, 10, 'soul_lantern'.replace('soul_lantern', 'iron_bars'));
  P.set(-3, 8, 10, 'soul_lantern'); P.set(3, 8, 10, 'soul_lantern');

  /* ---- mausoleum ---- */
  P.fill(-7, -1, -9, 7, -1, -3, 'deepslate_tiles');
  for (let x = -7; x <= 7; x++) for (let y = 0; y <= 4; y++) { P.set(x, y, -9, (Math.abs(x) % 4 === 3 || Math.abs(x) === 7) ? BO : MB); }
  for (let z = -9; z <= -3; z++) for (const x of [-7, 7]) for (let y = 0; y <= 4; y++) P.set(x, y, z, (z % 3 === 0) ? BO : MB);
  for (let x = -7; x <= 7; x++) for (let y = 0; y <= 4; y++) P.set(x, y, -3, (Math.abs(x) === 7 || Math.abs(x) === 3) ? BO : MB);
  P.clear(-6, 0, -8, 6, 4, -4);
  // pointed doorway
  P.clear(-2, 0, -3, 2, 4, -3);
  for (let i = 0; i < 2; i++) { const y = 5 + i, open = 2 - i; P.clear(-open, y, -3, open, y, -3); P.set(-(open + 1), y, -3, 'stone_bricks_stairs@W'); P.set(open + 1, y, -3, 'stone_bricks_stairs@E'); }
  for (let y = 5; y <= 6; y++) for (const x of [-3, 3]) { /* jambs */ }
  P.set(0, 7, -3, 'chiseled_stone_bricks'); P.fill(-2, 7, -3, 2, 7, -3, 'chiseled_stone_bricks'); P.fill(-1, 6, -3, 1, 6, -3, 'air');
  // soul glow in the doorway
  for (const x of [-3, 3]) { P.set(x, 2, -2, 'soul_lantern'); P.set(x, 0, -2, 'chiseled_stone_bricks'); }
  // roof: deepslate tiles, ridge of bone
  P.gable(-7, -9, 7, -3, 4, 'z', 'deepslate_tiles_stairs', 'deepslate_tiles', BO, 1);
  for (let x = -6; x <= 6; x++) for (let y = 5; y < 11 - Math.abs(x) + 0; y++) for (const z of [-3, -9]) P.set(x, y, z, 'mossy_stone_bricks');
  // glowing skull on the front gable
  for (let y = 7; y <= 10; y++) for (let x = -2; x <= 2; x++) if (Math.hypot(x, y - 8.6) <= 2.3 && y < 11 - Math.abs(x)) P.set(x, y, -3, BO);
  for (const x of [-1, 1]) P.set(x, 9, -3, 'soul_lantern'.replace('soul_lantern', 'sea_lantern')); P.set(0, 8, -3, 'air'); 
  for (const x of [-7, 7]) { for (let y = 5; y <= 7; y++) P.set(x, y, -3, BO); P.set(x, 8, -3, 'wither_skull'); }
  // interior: dais, throne, sarcophagi
  for (let z = -8; z <= -4; z++) P.set(0, -1, z, BO); P.fill(-1, -1, -8, 1, -1, -8, BO);
  plinth(ctx, P, 0, -6, 0, 'graveborn', 'gb_hollowking', { y: 0, big: true, legendary: true, base: 'deepslate_bricks', trim: BO, top: 'bone_block' });
  for (const x of [-4, 4]) for (const z of [-4, -6, -8]) { if (z === -4 && false) continue; P.set(x, 0, z, 'stone_bricks_slab'); P.set(x, 0, z - (z === -8 ? 0 : 0), 'stone_bricks_slab'); }
  for (const x of [-5, 5]) { P.fill(x, 0, -8, x, 0, -4, 'stone_bricks_slab'); P.set(x, 1, -8, 'skeleton_skull'); }
  for (const x of [-4, 4]) for (const z of [-4, -8]) { P.fill(x, 0, z, x, 2, z, 'bone_block'); P.set(x, 3, z, 'soul_lantern'); }
  for (let i = 0; i < 6; i++) P.setIfAir(Math.floor(hash(i, 1, 9) * 10) - 5, 0, -4 - Math.floor(hash(i, 2, 9) * 4), 'cobweb');
  P.set(0, 5, -6, 'chain'); P.set(0, 4, -6, 'chain'); P.set(0, 3, -6, 'soul_lantern~');
  for (const x of [-3, 3]) P.set(x, 5, -5, 'chain'), P.set(x, 4, -5, 'soul_lantern~');
  signAt(ctx, P, 0, 3.4, -3.5 + -2.6, 0, { text: 'THE HOLLOW KING', sub: 'Legendary · the crown that cracked', w: 3.4, h: 0.9, style: 'bone' });

  /* ---- grave rows: open graves are the troop plinths ---- */
  const spots = [[5, 7, 'revenant'], [-5, 7, 'bonebow'], [5, 3, 'reaper'], [-5, 3, 'stalker'], [5, -1, 'hexer']];
  for (const [x, z, id] of spots) {
    plinth(ctx, P, x, z, x > 0 ? -90 : 90, 'graveborn', id, { y: 0, base: 'coarse_dirt', trim: 'mossy_cobblestone', top: 'podzol' });
    // headstone behind the figure, outer side
    const hx = x + (x > 0 ? 2 : -2); P.set(hx, 0, z, 'cracked_stone_bricks'); P.set(hx, 1, z, 'stone_bricks_slab');
    P.set(x > 0 ? 7 : -7, 0, z + 2, 'soul_torch'); 
  }
  // scattered tombstones and crosses
  for (let i = 0; i < 22; i++) {
    const x = Math.floor(hash(i, 3, 17) * 17) - 8, z = Math.floor(hash(i, 4, 17) * 12) - 1;
    if (Math.abs(x) <= 2 || (Math.abs(x) >= 3 && Math.abs(x) <= 7 && z >= -2 && z <= 8 && false)) continue;
    const near = spots.some(([sx, sz]) => Math.abs(sx - x) <= 3 && Math.abs(sz - z) <= 3);
    if (near) continue;
    const kind = hash(i, 5, 17);
    if (kind < 0.4) { P.set(x, 0, z, 'cracked_stone_bricks'); P.set(x, 1, z, 'stone_bricks_slab'); P.set(x, -1, z + 1, 'coarse_dirt'); P.set(x, 0, z + 1, 'coarse_dirt'.replace('coarse_dirt', 'moss_carpet')); }
    else if (kind < 0.7) { P.fill(x, 0, z, x, 2, z, 'mossy_cobblestone_wall'); P.set(x - 1, 1, z, 'mossy_cobblestone_wall'); P.set(x + 1, 1, z, 'mossy_cobblestone_wall'); }
    else P.set(x, 0, z, hash(i, 6, 3) > 0.5 ? 'dead_bush' : 'brown_mushroom');
  }
  // dead trees
  for (const [x, z] of [[-8, 8], [8, -1], [-8, -2]]) deadTree(w, P.mx(x, z), 6, P.mz(x, z), 6 + Math.floor(hash(x, z, 1) * 3));
  bannerAt(ctx, P, -3.2, 7.8, 10.5, 0, { w: 1.2, h: 3.4, emblem: 'skull', bg: 0x2c3a2a, trim: 0x6fe8ff, kind: 'wall' });
  bannerAt(ctx, P, 3.2, 7.8, 10.5, 0, { w: 1.2, h: 3.4, emblem: 'skull', bg: 0x2c3a2a, trim: 0x6fe8ff, kind: 'wall' });
  signAt(ctx, P, 0, 9.4, 11.1, 0, { text: 'THE BARROW SHRINE', sub: 'the Graveborn recruit here · Risen · Archers · Reapers · Stalkers · Gravecallers', w: 7.6, h: 1.1, style: 'bone', size: 0.46 });
  emitAt(ctx, P, 0, 0.3, 3, { type: 'soul', w: 18, h: 5, n: 46 });
  emitAt(ctx, P, 0, 0, 3, { type: 'mist', w: 18, h: 1.2, n: 26, color: 0x9ad8c8 });
  emitAt(ctx, P, 0, 1, -6, { type: 'soul', w: 5, h: 5, n: 16 });
  ctx.reserve(38, -28, 60, -6);
  ctx.zone({ id: 'graveborn', name: 'The Graveborn · Barrow Shrine', color: '#6fe8ff', rect: [38, -28, 60, -6], y: [5, 22], tour: { x: 37, y: 7, z: -17, yaw: 270, pitch: 3 },
    house: 'graveborn', what: 'The Graveborn recruit at this graveyard shrine (not a tent): a walled barrow-yard under cold soul-light. Troops rise from open graves on mossy mounds; the mausoleum at the back has a bone-white skull glowing on its gable and the Hollow King waiting on his bone dais. Dead trees, cobwebs, tombstones and drifting teal wisps fill the yard.',
    builder: ['Palette: bone blocks, mossy stone bricks + mossy cobblestone, deepslate tiles, soul lanterns / soul torches (cyan), iron bars.', 'Yard 21 x 20 inside a mossy brick wall + iron bar fence, with chiselled brick pillars topped by soul lanterns every 4-8 blocks.', 'Gate: two 7-high deepslate pillars with wither skulls, iron-bar arch.', 'Mausoleum 15 x 7, walls 5 high, deepslate-tile gable roof, bone-block skull on the front gable with sea-lantern eyes.', 'Dead trees: dark oak logs with fence branches and cobwebs.'] });
}
