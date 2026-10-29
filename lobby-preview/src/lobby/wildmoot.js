// Wildmoot: "the woods have teeth". A timber longhouse under a steep hide roof, ringed by woods, wolf kennel and totems.
import { plinth, signAt, bannerAt, propAt, emitAt, oak, spruce, flowers } from './kit.js';
import { hash } from './ctx.js';

export function buildWildmoot(ctx) {
  const w = ctx.w;
  w.beginZone('wildmoot');
  const P = w.pen(-21, 6, -23, 3);
  const LOG = 'dark_oak_log', PL = 'spruce_planks';

  /* ---- earthen yard ---- */
  for (let x = -10; x <= 10; x++) for (let z = -9; z <= 10; z++) {
    const h = hash(P.mx(x, z), P.mz(x, z), 4);
    P.set(x, -1, z, h > 0.78 ? 'coarse_dirt' : h > 0.45 ? 'podzol' : h > 0.38 ? 'moss_block' : 'podzol');
  }
  /* ---- longhouse: log posts, plank infill, open gable front ---- */
  P.fill(-6, -1, -8, 6, -1, 8, 'dark_oak_planks');
  for (let z = -8; z <= 8; z++) for (const x of [-6, 6]) for (let y = 0; y <= 3; y++) P.set(x, y, z, (z % 4 === 0) ? LOG : (y === 3 ? 'stripped_spruce_log' : PL));
  for (let x = -6; x <= 6; x++) for (let y = 0; y <= 3; y++) P.set(x, y, -8, (Math.abs(x) % 4 === 2 || Math.abs(x) === 6) ? LOG : (y === 3 ? 'stripped_spruce_log' : PL));
  // side windows (open slits with fence bars) and ochre warpaint band
  for (const x of [-6, 6]) for (let z = -6; z <= 6; z += 4) { P.set(x, 1, z + 2, 'spruce_fence'); P.set(x, 2, z + 2, 'spruce_fence'); }
  for (const x of [-6, 6]) for (let z = -7; z <= 7; z++) if (z % 4 !== 0) P.set(x, 3, z, 'orange_terracotta');
  for (let x = -5; x <= 5; x++) P.set(x, 3, -8, 'orange_terracotta');
  // hide roof: spruce with ochre stripes every third row
  P.gable(-6, -8, 6, 8, 4, 'z', (k) => (k % 3 === 2 ? 'jungle_planks_stairs' : 'spruce_planks_stairs'), PL, 'dark_oak_log', 1);
  for (let x = -5; x <= 5; x++) for (let y = 4; y < 11 - Math.abs(x); y++) for (const z of [8, -8]) P.set(x, y, z, y === 4 && z === 8 ? 'orange_terracotta' : PL);
  // antler emblem on the front gable (bone)
  for (const [dx, dy] of [[0, 5], [0, 6], [0, 7], [-1, 7], [-2, 8], [1, 7], [2, 8], [-2, 6], [-3, 7], [2, 6], [3, 7], [-1, 8], [1, 8]]) if (4 + dy - 4 >= 4 && 4 + dy < 11 - Math.abs(dx)) P.set(dx, 4 + dy - 0, 8, 'bone_block');
  // ridge crest: crossed antler spars + skull
  for (const z of [-9, 9]) { for (let y = 11; y <= 13; y++) P.set(0, y, z, 'spruce_fence'); P.set(0, 14, z, 'skeleton_skull'); P.set(1, 13, z, 'spruce_fence'); P.set(-1, 13, z, 'spruce_fence'); P.set(2, 14, z, 'spruce_fence'); P.set(-2, 14, z, 'spruce_fence'); }
  // open the front: big door gap flanked by carved totems
  P.clear(-4, 0, 8, 4, 3, 8);
  for (const x of [-5, 5]) { P.fill(x, 0, 9, x, 7, 9, 'stripped_spruce_log'); for (const y of [2, 5]) { P.set(x, y, 9, 'orange_terracotta'); } P.set(x, 8, 9, 'skeleton_skull'); P.set(x + (x > 0 ? 1 : -1), 7, 9, 'spruce_fence'); P.set(x + (x > 0 ? 2 : -2), 8, 9, 'spruce_fence'); P.set(x, 9, 9, 'spruce_fence'); P.set(x + (x > 0 ? 1 : -1), 8, 9, 'spruce_fence'); P.set(x + (x > 0 ? 1 : -1), 9, 9, 'spruce_fence'); P.set(x, 7, 9, 'bone_block'); }
  P.fill(-4, 4, 8, 4, 4, 8, 'dark_oak_log'); // lintel
  // torches on the totems
  for (const x of [-4, 4]) P.set(x, 3, 9, 'lantern');

  /* ---- interior ---- */
  for (let x = -5; x <= 5; x++) for (let z = -7; z <= 7; z++) P.set(x, -1, z, (Math.abs(x) <= 1) ? 'dark_oak_planks' : 'spruce_planks');
  for (let z = -7; z <= 7; z++) { P.set(0, 0, z, z % 2 ? 'brown_carpet' : 'red_carpet'); }
  // central fire pit + hanging pot
  for (const [dx, dz] of [[1, 2], [-1, 2], [0, 1], [0, 3], [1, 1], [-1, 1], [1, 3], [-1, 3]]) P.set(dx, -1, dz, 'cobblestone');
  P.set(0, 0, 2, 'campfire'); for (const [dx, dz] of [[1, 2], [-1, 2], [0, 1], [0, 3]]) P.set(dx, 0, dz, 'cobblestone_slab');
  P.set(0, 3, 2, 'air'); for (let y = 6; y <= 10; y++) P.set(0, y, 2, 'air');
  emitAt(ctx, P, 0, 1, 2, { type: 'smoke', w: 0.6, h: 11, n: 16 });
  // hides on the walls + trophies
  for (const x of [-5.6, 5.6]) for (const z of [-6, -2, 2, 6]) bannerAt(ctx, P, x, 3.2, z, x > 0 ? -90 : 90, { w: 1.2, h: 2.2, emblem: 'antler', bg: 0x7a3426, trim: 0xd89a30, kind: 'wall' });
  P.set(0, 3, -7, 'skeleton_skull'); for (const x of [-3, 3]) P.set(x, 2, -7, 'spruce_fence');
  // troop plinths
  const spots = [[4, 5, 'wm_hound'], [-4, 5, 'wm_hunter'], [4, 1, 'wm_stalker'], [-4, 1, 'wm_tusk'], [4, -3, 'wm_seer']];
  for (const [x, z, id] of spots) plinth(ctx, P, x, z, x > 0 ? -90 : 90, 'wildmoot', id, { y: 0 });
  plinth(ctx, P, 0, -5, 0, 'wildmoot', 'wm_oldhorn', { y: 0, big: true, legendary: true, base: 'dark_oak_planks', trim: 'stripped_spruce_log', top: 'bone_block' });
  // long tables + benches
  for (const x of [-5]) { P.fill(x, 0, -6, x, 0, -4, 'spruce_planks_slab'); }
  P.fill(-5, 0, -4, -5, 0, -7, 'spruce_planks_slab'); for (const z of [-7, -4]) P.set(-5, -1, z, 'spruce_planks'); P.set(-4, 0, -6, 'barrel'); P.set(-5, 1, -6, 'candle');

  /* ---- yard dressing ---- */
  // log palisade along the sides (a gap near the plaza keeps the way to the west stair open)
  for (const sgn of [-1, 1]) for (let z = -9; z <= 9; z++) { if (sgn === 1 && z >= 6) continue; const h = 2 + (hash(z, sgn, 3) > 0.5 ? 1 : 0); P.fill(sgn * 10, 0, z, sgn * 10, h, z, 'spruce_log'); if (hash(z, sgn, 9) > 0.7) P.set(sgn * 10, h + 1, z, 'spruce_fence'); }
  for (let x = -10; x <= 10; x++) { const h = 2 + (hash(x, 2, 3) > 0.5 ? 1 : 0); P.fill(x, 0, -9, x, h, -9, 'spruce_log'); }
  // kennel with wolves
  P.fill(8, 0, -2, 9, 0, 2, 'spruce_planks'); P.fill(8, 1, -2, 9, 1, -2, 'spruce_fence'); P.fill(8, 1, 2, 9, 1, 2, 'spruce_fence');
  propAt(ctx, P, 8.5, 0, -0.8, -90, { type: 'wolf' }); propAt(ctx, P, 8.5, 0, 1.2, -110, { type: 'wolf' });
  // drying racks with hides
  for (const z of [-4, 0]) { P.fill(-9, 0, z, -9, 2, z, 'spruce_fence'); P.fill(-9, 2, z, -9, 2, z + 2, 'spruce_fence'); P.fill(-9, 0, z + 2, -9, 1, z + 2, 'spruce_fence'); bannerAt(ctx, P, -8.6, 2.1, z + 1, 90, { w: 1, h: 1.6, emblem: 'antler', bg: 0x7a5230, trim: 0x4a3020, kind: 'wall' }); }
  // hay bales, stumps, mushrooms
  for (const [x, z] of [[-8, 5], [-8, 6], [-9, 6], [8, -5], [9, -6], [8, -6]]) P.set(x, 0, z, 'hay_block');
  for (const [x, z] of [[-7, -7], [7, -7], [3, 8], [-3, 9]]) { P.set(x, 0, z, 'dark_oak_log'); }
  for (let i = 0; i < 26; i++) { const x = Math.floor(hash(i, 1, 55) * 20) - 10, z = Math.floor(hash(i, 2, 55) * 19) - 8; const c = hash(i, 3, 5); if (Math.abs(x) < 7 && Math.abs(z) < 9) continue; P.setIfAir(x, 0, z, c < 0.35 ? 'red_mushroom' : c < 0.6 ? 'brown_mushroom' : c < 0.85 ? 'fern' : 'cobweb'); }
  // the woods brought into camp
  for (const [x, z] of [[-9, -7], [-9, 8], [9, -6]]) { const mx = P.mx(x, z), mz = P.mz(x, z); oak(w, mx, 6, mz, 7, 'dark_oak_leaves', 'dark_oak_log', 3); }
  const sx = P.mx(9, -7), sz = P.mz(9, -7); spruce(w, sx, 6, sz, 10, 3);
  signAt(ctx, P, 0, 4.9, 9.55, 0, { text: 'THE WILDMOOT', sub: 'Hounds · Hunters · Web Stalkers · Tusk Spears · Seers', w: 6.4, h: 1.0, style: 'wood', size: 0.5 });
  signAt(ctx, P, 0, 2.9, -3.4, 0, { text: 'OLD HORN', sub: 'the Antlered Alpha · Legendary', w: 3.4, h: 0.9, style: 'wood' });
  bannerAt(ctx, P, -5, 7.6, 10, 0, { w: 1.3, h: 3.2, emblem: 'antler', bg: 0x7a3426, trim: 0xd89a30, kind: 'wall' });
  bannerAt(ctx, P, 5, 7.6, 10, 0, { w: 1.3, h: 3.2, emblem: 'antler', bg: 0x7a3426, trim: 0xd89a30, kind: 'wall' });
  emitAt(ctx, P, 0, 3, 0, { type: 'firefly', w: 10, h: 4, n: 14 });
  ctx.reserve(-34, -34, -10, -12);
  ctx.zone({ id: 'wildmoot', name: 'The Wildmoot · Moot Lodge', color: '#d89a30', rect: [-34, -34, -10, -12], y: [5, 22], tour: { x: -9, y: 7, z: -23, yaw: 90, pitch: 4 },
    house: 'wildmoot', what: 'The Wildmoot House tent: a timber longhouse under a steep hide roof (spruce with ochre stripes), carved totems with skulls and antlers at the open front, a central fire pit, hides on the walls. Five troop plinths line the hall, Old Horn the Antlered Alpha stands at the back. A wolf kennel, drying racks and a brought-in grove of dark oaks make the yard.',
    builder: ['Palette: dark oak logs + spruce planks, ochre (orange terracotta / jungle planks) warpaint, bone blocks, brown + red wool hides, hay, podzol ground.', 'Hall 13 x 17, walls 4 tall, gable roof ridge 11 high; roof stairs spruce with a jungle-plank stripe every third row; dark oak log ridge.', 'Front: two 8-high stripped spruce totems with ochre rings, skeleton skulls and fence antlers; lintel of dark oak.', 'Interior fire: campfire in a cobblestone ring, floor of planks with a brown/red carpet runner.'] });
}
