// Tidecallers: "the sea wants the land back". A verdigris shell-hall behind a glowing tidal pool, with a pier.
import { plinth, signAt, bannerAt, propAt, emitAt, lampPost } from './kit.js';
import { hash } from './ctx.js';

export function buildTide(ctx) {
  const w = ctx.w;
  w.beginZone('tidecallers');
  const P = w.pen(26, 6, -23, 1);                 // front faces west
  const CU = 'oxidized_cut_copper', PB = 'prismarine_bricks', DP = 'dark_prismarine';

  /* ---- ground + pool ---- */
  for (let x = -10; x <= 10; x++) for (let z = -10; z <= 10; z++) P.set(x, -1, z, (hash(x, z, 6) > 0.7) ? 'prismarine_bricks' : 'dark_prismarine');
  for (let x = -9; x <= 9; x++) for (let z = -2; z <= 9; z++) {
    P.set(x, -4, z, hash(x, z, 2) > 0.55 ? 'sand' : 'prismarine'); P.set(x, -3, z, 'water'); P.set(x, -2, z, 'water'); P.set(x, -1, z, 'water');
  }
  for (let x = -10; x <= 10; x++) { P.set(x, -1, 10, CU); P.set(x, -1, -3, x >= -9 && x <= 9 ? 'oxidized_cut_copper' : CU); }
  for (let z = -3; z <= 10; z++) { P.set(-10, -1, z, CU); P.set(10, -1, z, CU); }
  for (let x = -10; x <= 10; x += 4) { P.set(x, 0, 10, 'oxidized_cut_copper_slab'); }
  // glow
  for (const [x, z] of [[-6, 4], [6, 4], [-7, 8], [7, 8], [0, 8], [-3, 0], [3, 0], [-8, 0], [8, 0]]) P.set(x, -4, z, 'sea_lantern');
  // underwater life
  for (let x = -9; x <= 9; x++) for (let z = -2; z <= 9; z++) { const h = hash(x, z, 8); if (Math.abs(x) <= 2) continue; const c = h; if (c < 0.1) { P.set(x, -3, z, 'kelp'); P.set(x, -2, z, 'kelp'); } else if (c < 0.2) P.set(x, -3, z, 'seagrass'); else if (c < 0.25) P.set(x, -3, z, ['tube_coral', 'brain_coral', 'fire_coral', 'horn_coral'][Math.floor(hash(x, z, 4) * 4)]); }
  // pier on pilings
  for (let z = -3; z <= 9; z++) for (let x = -2; x <= 2; x++) P.set(x, -1, z, (x === 0) ? 'spruce_planks' : 'dark_oak_planks');
  for (let z = -2; z <= 9; z += 4) for (const x of [-2, 2]) { P.fill(x, -4, z, x, -2, z, 'stripped_spruce_log'); P.fill(x, 0, z, x, 2, z, 'spruce_fence'); P.set(x, 3, z, 'lantern'); }
  for (const x of [-2, 2]) for (let z = -2; z <= 9; z++) if ((z + 2) % 4) P.set(x, 0, z, 'spruce_fence');
  // islands for the troops
  const isles = [[-4, 6, 'tc_raider'], [-4, 2, 'tc_harpooner'], [-4, -1, 'tc_tideguard'], [4, 6, 'tc_mist'], [4, 2, 'tc_brute']];
  for (const [x, z, id] of isles) {
    for (let dx = -1; dx <= 1; dx++) for (let dz = -1; dz <= 1; dz++) P.fill(x + dx, -4, z + dz, x + dx, -2, z + dz, 'prismarine_bricks');
    plinth(ctx, P, x, z, x > 0 ? -90 : 90, 'tidecallers', id, { y: -1, base: 'dark_prismarine', trim: 'oxidized_cut_copper', top: 'prismarine' });
  }
  // the east third islet is a tide chart
  P.fill(3, -1, -2, 5, -1, 0, 'dark_prismarine'); P.set(4, 0, -1, 'cartography_table'); P.set(3, 0, -1, 'barrel'); P.set(5, 0, -1, 'crate'); 
  // rowboat moored on the east side
  for (let z = 4; z <= 8; z++) P.set(7, -1, z, 'spruce_planks_slab'); P.fill(6, -1, 5, 6, -1, 7, 'spruce_planks'); P.fill(8, -1, 5, 8, -1, 7, 'spruce_planks'); P.fill(6, 0, 5, 6, 0, 7, 'spruce_fence'); 
  P.fill(7, 0, 4, 7, 0, 4, 'spruce_planks'); P.fill(7, 0, 8, 7, 0, 8, 'spruce_planks'); P.set(7, 0, 6, 'barrel'); P.fill(7, 1, 6, 7, 3, 6, 'spruce_fence'); 
  bannerAt(ctx, P, 7, 4.2, 6.3, 0, { w: 1.2, h: 1.6, emblem: 'trident', bg: 0x14506a, trim: 0xe9ecec, kind: 'wall' });

  /* ---- the shell hall ---- */
  P.fill(-9, -1, -10, 9, -1, -3, DP);
  for (let x = -9; x <= 9; x++) for (let y = 0; y <= 4; y++) { P.set(x, y, -10, y === 4 ? CU : PB); }
  for (let z = -10; z <= -3; z++) for (const x of [-9, 9]) for (let y = 0; y <= 4; y++) P.set(x, y, z, (z % 3 === 0 || y === 4) ? CU : PB);
  for (let x = -9; x <= 9; x++) for (let y = 0; y <= 4; y++) { if (Math.abs(x) <= 3 && y <= 7) continue; P.set(x, y, -3, (Math.abs(x) % 3 === 0 || y === 4) ? CU : PB); }
  P.clear(-8, 0, -9, 8, 4, -4);
  // sea-lantern windows
  for (const x of [-7, -5, 5, 7]) { P.set(x, 2, -3, 'prismarine'); P.set(x, 2, -3, 'sea_lantern'); }
  // roof: verdigris gable, ridge along x
  P.gable(-9, -10, 9, -3, 4, 'x', 'oxidized_cut_copper_stairs', CU, PB, 2);
  for (let z = -9; z <= -4; z++) for (const x of [-9, 9]) for (let y = 5; y <= 5 + Math.min(z + 10, -3 - z) - 1; y++) P.set(x, y, z, CU);
  // wave crest along the ridge
  for (let x = -9; x <= 9; x += 2) P.set(x, 10, -7, 'prismarine_bricks_slab');
  // whale rib arch at the entrance
  for (const sgn of [-1, 1]) { for (let y = 0; y <= 5; y++) P.set(sgn * 4, y, -2, 'bone_block'); P.set(sgn * 3, 6, -2, 'bone_block'); P.set(sgn * 2, 7, -2, 'bone_block'); P.set(sgn * 1, 7, -2, 'bone_block'); P.set(sgn * 4, 6, -2, 'bone_block'); for (let y = 1; y <= 4; y++) P.set(sgn * 5, y, -2, 'bone_block'.replace('bone_block', 'air')); }
  P.set(0, 8, -2, 'bone_block'); P.set(0, 7, -2, 'bone_block'); P.clear(-3, 0, -3, 3, 7, -3);
  // interior
  for (let x = -8; x <= 8; x++) for (let z = -9; z <= -4; z++) P.set(x, -1, z, (Math.abs(x) <= 1) ? 'prismarine_bricks' : (hash(x, z, 3) > 0.5 ? 'dark_prismarine' : 'prismarine_bricks'));
  for (let z = -8; z <= -4; z++) P.set(0, -1, z, 'cut_brass'.replace('cut_brass', 'sea_lantern'));
  plinth(ctx, P, 0, -7, 0, 'tidecallers', 'tc_admiral', { y: 0, big: true, legendary: true, base: 'dark_prismarine', trim: 'oxidized_cut_copper', top: 'prismarine' });
  for (const x of [-5, 5]) for (const z of [-5, -8]) { P.fill(x, 0, z, x, 3, z, 'prismarine_bricks_wall'); P.set(x, 4, z, 'sea_lantern'); }
  for (const x of [-3, 3]) { P.set(x, 4, -6, 'chain'); P.set(x, 3, -6, 'chain'); P.set(x, 2, -6, 'soul_lantern~'); }
  // nets + anchors on the back wall
  for (let x = -8; x <= 8; x++) { if (Math.abs(x) < 3) continue; for (let y = 1; y <= 3; y++) if (hash(x, y, 6) > 0.4) P.set(x, y, -9, 'cobweb'); }
  for (const x of [-7, 7]) { P.fill(x, 0, -9, x, 3, -9, 'iron_bars'); P.set(x, 4, -9, 'chain'); }
  for (const x of [-6, -4, 4, 6]) { P.set(x, 0, -9, 'barrel'); }
  // banners + signs
  for (const x of [-5.5, 5.5]) bannerAt(ctx, P, x, 5.8, -3.6, 0, { w: 1.4, h: 4.4, emblem: 'trident', bg: 0x14506a, trim: 0xe9ecec, kind: 'wall' });
  signAt(ctx, P, 0, 9.4, -1.5, 0, { text: 'THE TIDECALLERS', sub: 'Raiders · Harpooners · Trident Guards · Mist Callers · Brutes', w: 6.6, h: 1.0, style: 'copper', size: 0.5 });
  signAt(ctx, P, 0, 3.5, -4.5, 0, { text: 'ADMIRAL VAEL', sub: 'the Drowned Tide · Legendary', w: 3.4, h: 0.9, style: 'copper' });
  emitAt(ctx, P, 0, 0, 3, { type: 'mist', w: 8, h: 2.5, n: 22 });
  emitAt(ctx, P, 0, -2, 4, { type: 'bubble', w: 8, h: 3, n: 20 });
  emitAt(ctx, P, -5, 5, -7, { type: 'sparkle', w: 5, h: 4, n: 12, color: 0x7fffe0 });
  ctx.reserve(16, -34, 36, -12);
  ctx.zone({ id: 'tidecallers', name: 'The Tidecallers · Tidepost', color: '#3fa58c', rect: [16, -34, 36, -12], y: [-5, 20], tour: { x: 12, y: 7, z: -23, yaw: 270, pitch: 3 },
    house: 'tidecallers', what: 'The Tidecallers House tent: a verdigris shell-hall behind a three-block-deep tidal pool lit by sea lanterns. A timber pier leads over the water to a whale-rib arch; five troops stand on prismarine islets, Admiral Vael waits inside. A rowboat is moored beside the pier, mist drifts off the water.',
    builder: ['Palette: oxidised cut copper (verdigris), prismarine bricks + dark prismarine, bone (whale ribs), deep teal-navy banners, sea lanterns, spruce pier.', 'Pool 19 x 12, 3 deep: sand/prismarine floor, sea lanterns on the floor, kelp, seagrass and coral.', 'Pier 5 wide on stripped spruce pilings with fence rails and lanterns every 4 blocks.', 'Hall 19 x 8, walls 5 high, roof of oxidised copper stairs with a prismarine ridge, entrance framed by a bone-block rib arch.'] });
}
