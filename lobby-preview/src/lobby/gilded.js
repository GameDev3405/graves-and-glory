// Gilded Order: "the oath that holds the line". A white-and-gold colonnade chapel with a stained-glass apse.
import { plinth, signAt, bannerAt, propAt, emitAt, cherry, azaleaBush, flowers } from './kit.js';
import { pave } from './kit.js';

export function buildGilded(ctx) {
  const w = ctx.w;
  w.beginZone('gilded');
  const P = w.pen(-43, 6, -17, 3);            // front faces east, toward the House Ring plaza
  const Q = 'quartz_block', G = 'gold_block';

  /* ---- platform + steps ---- */
  P.fill(-9, -1, -10, 9, -1, 10, Q);
  for (let x = -9; x <= 9; x++) for (let z = -10; z <= 10; z++) { if ((Math.abs(x) + Math.abs(z)) % 2 === 0 && Math.abs(x) < 8 && Math.abs(z) < 9) P.set(x, -1, z, 'polished_andesite'); }
  for (const x of [-9, 9]) P.fill(x, -1, -10, x, -1, 10, G);
  P.fill(-9, -1, 10, 9, -1, 10, G); P.fill(-9, -1, -10, 9, -1, -10, G);
  // front steps (toward +z)
  for (let i = 0; i < 2; i++) for (let x = -3 - i; x <= 3 + i; x++) { P.set(x, -1 - i, 11 + i * 0, 'air'); }
  for (let x = -4; x <= 4; x++) { P.set(x, -1, 11, 'quartz_block_stairs@N'.replace('@N', '@S')); }
  for (let x = -4; x <= 4; x++) { P.set(x, -1, 12, 'quartz_block_slab'); }
  pave(P, -4, 13, 4, 15, -1, [['quartz_block', 50], ['polished_andesite', 50]]);

  /* ---- colonnade ---- */
  const zs = [-7, -3, 1, 5];
  for (const x of [-7, 7]) for (const z of [...zs, 9, -9]) {
    P.fill(x, 0, z, x, 5, z, 'quartz_pillar'); P.set(x, 0, z, 'polished_andesite'); P.set(x, 6, z, 'chiseled_quartz_block'); P.set(x, 1, z, 'gold_block'); P.set(x, 5, z, 'gold_block');
  }
  // architrave
  for (const x of [-8, -7, 7, 8]) P.fill(x, 7, -9, x, 7, 9, x === 8 || x === -8 ? 'cut_brass' : Q);
  // back wall + front corner returns
  P.fill(-7, 0, -9, 7, 6, -9, Q); for (let x = -7; x <= 7; x += 2) P.fill(x, 2, -9, x, 2, -9, 'polished_andesite');
  // stained glass apse window (pointed)
  for (let y = 1; y <= 8; y++) { const hw = y <= 5 ? 3 : (y === 6 ? 3 : y === 7 ? 2 : 1); for (let x = -hw; x <= hw; x++) { const edge = Math.abs(x) === hw; P.set(x, y, -9, edge ? 'white_stained_glass_pane' : (y % 3 === 0 ? 'yellow_stained_glass_pane' : (Math.abs(x) === 1 ? 'light_blue_stained_glass_pane' : 'yellow_stained_glass_pane'))); } }
  for (let y = 1; y <= 8; y++) for (const z of [-10, -11]) { P.set(0, y, z, 'air'); }
  for (const y of [2, 5, 7]) for (const x of [-2, 0, 2]) P.set(x, y, -10, 'lantern');
  P.fill(-4, 0, -10, 4, 8, -10, 'air'); for (const y of [2, 5, 7]) for (const x of [-2, 0, 2]) P.set(x, y, -10, 'lantern');
  P.fill(-4, 0, -11, 4, 9, -11, Q);

  /* ---- gable roof, ridge along z ---- */
  P.gable(-8, -9, 8, 9, 7, 'z', 'quartz_block_stairs', Q, G, 1);
  // pediments filled in, front (z=+9) and back (z=-9)
  for (let x = -7; x <= 7; x++) for (let y = 8; y < 16 - Math.abs(x); y++) for (const z of [9, -9]) P.set(x, y, z, Q);
  // sun emblem on the front pediment
  for (let x = -2; x <= 2; x++) for (let y = 9; y <= 13; y++) if (Math.hypot(x, y - 11) <= 2.2) P.set(x, y, 9, 'gold_block');
  for (const [dx, dy] of [[0, 3], [0, -3], [3, 0], [-3, 0], [2, 2], [-2, 2], [2, -2], [-2, -2]]) { if (11 + dy >= 8 && 11 + dy < 16 - Math.abs(dx)) P.set(dx, 11 + dy, 9, 'cut_brass'); }
  P.set(0, 11, 9, 'sea_lantern');
  // back pediment: gold trim
  for (let x = -6; x <= 6; x++) P.set(x, 8, -9, 'cut_brass');

  /* ---- interior ---- */
  // runner
  for (let z = -4; z <= 9; z++) { for (const x of [-1, 0, 1]) P.set(x, 0, z, 'white_carpet'); for (const x of [-2, 2]) P.set(x, 0, z, 'yellow_carpet'); }
  // dais
  for (let x = -5; x <= 5; x++) for (let z = -8; z <= -4; z++) P.set(x, 0, z, 'quartz_block');
  for (let x = -6; x <= 6; x++) P.set(x, 0, -3, 'quartz_block_slab');
  plinth(ctx, P, 0, -6, 0, 'gilded', 'gl_marshal', { y: 1, big: true, legendary: true, base: 'quartz_block', trim: 'gold_block', top: 'gold_block' });
  for (const x of [-5, 5]) { P.fill(x, 1, -8, x, 4, -8, 'gold_block'); P.set(x, 5, -8, 'lantern'); }
  // troop plinths on either side of the carpet
  const spots = [[5, 6, 'knight'], [-5, 6, 'archer'], [5, 2, 'sentinel'], [-5, 2, 'longbow'], [5, -2, 'mage']];
  for (const [x, z, id] of spots) plinth(ctx, P, x, z, x > 0 ? -90 : 90, 'gilded', id, { y: 0 });
  // kite shields on the aisle side of the back columns
  for (const x of [-6, 6]) { P.fill(x, 2, -8, x, 3, -8, 'white_wool'); }
  // chandeliers
  for (const z of [6, 1]) { for (let y = 10; y <= 15 - 0; y++) P.set(0, y, z, y < 11 ? 'air' : 'chain'); for (const [dx, dz] of [[0, 0], [1, 0], [-1, 0], [0, 1], [0, -1]]) P.set(dx, 9 + 1, z + dz, 'lantern~'); P.set(0, 10, z, 'lantern~'); }
  // hanging lanterns between the columns
  for (const x of [-7, 7]) for (let z = -5; z <= 7; z += 4) P.set(x, 6, z, 'lantern~');
  for (const x of [-8, 8]) for (let z of [-9, -3, 3, 9]) { P.set(x, 8, z, 'lantern'); }
  // banners on the columns facing the aisle
  for (const x of [-7, 7]) for (const z of [-5, -1, 3]) bannerAt(ctx, P, x + (x > 0 ? -0.55 : 0.55), 6.5, z + 2, x > 0 ? -90 : 90, { w: 0.9, h: 3, emblem: 'sun', bg: 0xe9ecec, trim: 0xc9a24a, kind: 'wall' });
  bannerAt(ctx, P, 0, 9.5, 10.1, 0, { w: 3, h: 2.6, emblem: 'sun', bg: 0xe9ecec, trim: 0xc9a24a, kind: 'wall' });
  signAt(ctx, P, 0, 7.4, 10.55, 0, { text: 'THE GILDED ORDER', sub: 'Squires · Archers · Templars · Longbows · Sunweavers', w: 7.2, h: 1.1, style: 'gold', size: 0.5 });
  signAt(ctx, P, 0, 3.15, -3.4, 0, { text: 'LORD MARSHAL ALDRIC', sub: 'Legendary · sworn to the last living crown', w: 4.2, h: 0.9, style: 'gold' });

  /* ---- gardens along both flanks ---- */
  for (const sgn of [-1, 1]) {
    const x = sgn * 13;
    P.fill(x - 3, -1, -11, x + 3, -1, 11, 'grass_block'); P.fill(x - 3, -1, -11, x - 3, -1, 11, 'moss_block'); 
    for (let z = -10; z <= 10; z += 2) azaleaBush(w, P.mx(x - sgn * 3, z), 6, P.mz(x - sgn * 3, z));
    cherry(w, P.mx(x, -5), 6, P.mz(x, -5)); cherry(w, P.mx(x, 6), 6, P.mz(x, 6));
    for (let z = -10; z <= 10; z++) if (Math.abs(z) % 3 === 1) { flowers(w, P.mx(x + sgn * 2, z), 6, P.mz(x + sgn * 2, z)); flowers(w, P.mx(x - sgn, z), 6, P.mz(x - sgn, z)); }
    for (const z of [-9, 0, 9]) { P.fill(x + sgn * 2, 0, z, x + sgn * 2, 3, z, 'spruce_fence'); P.set(x + sgn * 2, 4, z, 'lantern'); }
  }
  emitAt(ctx, P, 0, 12, 0, { type: 'sparkle', w: 6, h: 6, n: 24, color: 0xffe9a0 });
  ctx.reserve(-53, -28, -30, -5);
  ctx.zone({ id: 'gilded', name: 'The Gilded Order · Oathhall', color: '#e9ecec', rect: [-54, -28, -30, -5], y: [5, 24], tour: { x: -30, y: 7, z: -17, yaw: 90, pitch: 4 },
    house: 'gilded', what: 'The Gilded Order House tent: a white-and-gold colonnade chapel. A carpeted aisle leads to a dais where Lord Marshal Aldric (the Order\'s Legendary) stands before a stained-glass window. Five troop plinths flank the aisle. Cherry trees and azalea hedges soften the sides.',
    builder: ['Palette: quartz block + pillars, polished andesite (steel), gold blocks, white/yellow carpet, white banners with the gold sun.', 'Footprint 19 x 21, platform 1 block tall with gold border and a 2-step approach from the plaza.', 'Eight quartz pillars (7 tall) with gold bands, gold-trimmed architrave, gable roof of quartz stairs (ridge of gold blocks), pediment with a gold-block sun and a sea lantern core.', 'Back apse: pointed window of yellow / light blue stained-glass panes, lit from behind by lanterns.', 'Aisle: white carpet with yellow edges, dais of 3 steps.'] });
}
