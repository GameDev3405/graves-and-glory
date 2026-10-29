// Ironclad: "the hold remembers every siege". A forge-hall cut into the cliff under the Command Terrace.
import { plinth, signAt, bannerAt, npcAt, propAt, emitAt, bigBrazier, pave } from './kit.js';

export function buildIronclad(ctx) {
  const w = ctx.w;
  w.beginZone('ironclad');
  const P = w.pen(0, 6, -37, 0);   // local y 0 = T2 surface; local z 0 = the cliff face; +z = toward the plaza
  const BR = 'polished_blackstone_bricks', DB = 'deepslate_bricks';

  /* ---- hall: 23 wide, 16 deep, 8 tall, cut into the mountain ---- */
  P.clear(-11, 0, -15, 11, 7, -1);
  P.shell(-12, -1, -16, 12, 8, 0, DB);
  P.fill(-11, -1, -15, 11, -1, -1, 'deepslate_tiles');
  P.fill(-11, 8, -15, 11, 8, -1, 'polished_blackstone');
  // floor: brass runner and ember seams
  for (let z = -15; z <= -1; z++) { P.set(0, -1, z, 'cut_brass'); if (z % 4 === 0) for (const x of [-3, 3]) P.set(x, -1, z, 'magma_block'); }
  for (let x = -11; x <= 11; x++) { P.set(x, -1, -1, 'polished_blackstone_bricks'); }
  // ceiling embers + beams
  for (let z = -13; z <= -3; z += 5) for (const x of [-8, -3, 3, 8]) P.set(x, 8, z, 'shroomlight');
  for (let z = -15; z <= -1; z += 4) P.fill(-11, 7, z, 11, 7, z, 'polished_blackstone_bricks_slab^');
  // pillars
  for (const x of [-6, 6]) for (const z of [-3, -7, -11]) {
    P.fill(x, 0, z, x, 6, z, BR);
    P.set(x, 1, z, 'cut_brass'); P.set(x, 4, z, 'cut_brass'); P.set(x, 7, z, 'chiseled_polished_blackstone');
    for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) P.set(x + dx, 0, z + dz, 'polished_blackstone_bricks_stairs@' + (dx === 1 ? 'W' : dx === -1 ? 'E' : dz === 1 ? 'N' : 'S'));
  }
  // hanging lamps between the pillars
  for (const x of [-3, 3]) for (const z of [-5, -9, -13]) { P.set(x, 7, z, 'chain'); P.set(x, 6, z, 'chain'); P.set(x, 5, z, 'lantern~'); }

  /* ---- side alcoves with troop plinths ---- */
  const west = [['ic_hearthguard', -3], ['ic_arbalist', -7], ['ic_breaker', -11]];
  const east = [['ic_ballista', -3], ['ic_mortar', -7]];
  const alcove = (sgn, z, id) => {
    const x0 = sgn * 12;
    P.clear(x0, 0, z - 1, sgn * 15, 5, z + 1);
    P.fill(sgn * 16, -1, z - 2, sgn * 16, 6, z + 2, 'polished_blackstone');
    P.fill(sgn * 12, -1, z - 2, sgn * 15, -1, z + 2, 'deepslate_tiles');
    P.fill(sgn * 12, 6, z - 2, sgn * 15, 6, z + 2, 'polished_blackstone_bricks');
    P.set(sgn * 12, 0, z - 2, 'cut_brass'); P.set(sgn * 12, 0, z + 2, 'cut_brass');
    for (const zz of [z - 2, z + 2]) P.fill(sgn * 12, 1, zz, sgn * 15, 5, zz, BR);
    P.set(sgn * 15, 5, z, 'shroomlight'); // alcove glow
    P.set(sgn * 12, 5, z, 'cut_brass');
    plinth(ctx, P, sgn * 14, z, sgn * 90, 'ironclad', id, { y: 0 });
    P.set(sgn * 12, 1, z - 1, 'iron_bars'); P.set(sgn * 12, 1, z + 1, 'iron_bars'); P.set(sgn * 12, 2, z - 1, 'iron_bars'); P.set(sgn * 12, 2, z + 1, 'iron_bars');
  };
  west.forEach(([id, z]) => alcove(-1, z, id));
  east.forEach(([id, z]) => alcove(1, z, id));
  // east third alcove: the Hold's ledger
  P.clear(12, 0, -12, 15, 5, -10); P.fill(16, -1, -13, 16, 6, -9, 'polished_blackstone'); P.fill(12, -1, -13, 15, -1, -9, 'deepslate_tiles'); P.fill(12, 6, -13, 15, 6, -9, BR);
  for (const zz of [-13, -9]) P.fill(12, 1, zz, 15, 5, zz, BR);
  P.set(14, 0, -11, 'lectern'); P.set(13, 0, -10, 'barrel'); P.set(13, 0, -12, 'crate'); P.set(15, 0, -11, 'cut_brass'); P.set(15, 4, -11, 'lantern~');

  /* ---- the forge wall (north) + Brassjaw's dais ---- */
  for (let x = -10; x <= -5; x++) { P.set(x, 0, -15, 'blast_furnace'); P.set(x, 1, -15, 'blast_furnace'); }
  for (let x = 5; x <= 10; x++) { P.set(x, 0, -15, 'blast_furnace'); P.set(x, 1, -15, 'blast_furnace'); }
  for (const x of [-9, -7, 7, 9]) { P.set(x, 0, -13, 'anvil'); }
  for (const x of [-8, 8]) { P.set(x, 0, -14, 'grindstone'); P.set(x - 1, 0, -12, 'smithing_table'); P.set(x + 1, 0, -12, 'cauldron'); }
  for (let x = -10; x <= 10; x++) { if (Math.abs(x) <= 4) continue; for (let y = 2; y <= 7; y++) P.set(x, y, -16, y % 3 === 1 ? 'cut_brass' : (x % 2 ? 'polished_blackstone_bricks' : 'chiseled_polished_blackstone')); }
  // lava forge channel behind iron bars: x -3..3 ... in front of the dais
  for (const sgn of [-1, 1]) {
    for (let z = -14; z <= -11; z++) for (let x = 2; x <= 4; x++) { P.set(sgn * x, -1, z, 'lava'); P.set(sgn * x, -2, z, 'polished_blackstone'); }
    for (let x = 1; x <= 5; x++) { P.set(sgn * x, 0, -10, 'iron_bars'); P.set(sgn * x, 1, -10, 'iron_bars'); }
    for (let z = -14; z <= -11; z++) { P.set(sgn * 1, 0, z, 'iron_bars'); P.set(sgn * 1, 1, z, 'iron_bars'); P.set(sgn * 5, 0, z, 'iron_bars'); P.set(sgn * 5, 1, z, 'iron_bars'); }
    P.set(sgn * 5, -1, -14, 'polished_blackstone_bricks'); P.set(sgn * 2, 2, -12, 'chain'); P.set(sgn * 2, 3, -12, 'chain');
  }
  // Brassjaw's plinth, raised
  plinth(ctx, P, 0, -13, 0, 'ironclad', 'ic_colossus', { y: 0, big: true, legendary: true, base: 'polished_blackstone_bricks', trim: 'cut_brass', top: 'gold_block' });
  for (const x of [-1, 1]) P.set(x, 0, -10, 'cut_brass_stairs@N');
  P.set(0, 0, -10, 'cut_brass_stairs@N');
  P.fill(-1, 0, -16, 1, 5, -16, 'magma_block'); // the glowing seal behind the Colossus
  P.set(0, 3, -16, 'shroomlight'); P.set(0, 4, -16, 'shroomlight');
  bannerAt(ctx, P, -3.5, 7.5, -15.6, 0, { w: 1.6, h: 4.5, emblem: 'anvil', bg: 0x1c1a20, trim: 0xc9a24a, kind: 'wall' });
  bannerAt(ctx, P, 3.5, 7.5, -15.6, 0, { w: 1.6, h: 4.5, emblem: 'anvil', bg: 0x1c1a20, trim: 0xc9a24a, kind: 'wall' });
  signAt(ctx, P, 0, 7.8, -9.7, 0, { text: 'BRASSJAW', sub: 'the Forge Colossus · Legendary', w: 3.4, h: 0.9, style: 'iron' });
  emitAt(ctx, P, -3.5, 1.5, -13, { type: 'ember', w: 2, h: 5, n: 30 }); emitAt(ctx, P, 3.5, 1.5, -13, { type: 'ember', w: 2, h: 5, n: 30 });

  /* ---- facade: heavy squat arch, brass bands, glowing slits ---- */
  P.fill(-12, 0, 1, 12, 9, 2, BR);
  for (const sgn of [-1, 1]) {
    P.fill(sgn * 9, 0, 3, sgn * 12, 9, 3, BR);
    P.fill(sgn * 8, 0, 3, sgn * 8, 9, 3, 'polished_blackstone');
    for (const y of [0, 4, 9]) P.fill(sgn * 9, y, 3, sgn * 12, y, 3, 'cut_brass');
    // ember slits in the piers
    for (const x of [sgn * 10, sgn * 11]) for (const y of [2, 3]) P.set(x, y, 3, 'shroomlight');
    P.fill(sgn * 10, 6, 3, sgn * 11, 7, 3, 'chiseled_polished_blackstone');
    P.set(sgn * 12, 10, 3, 'polished_blackstone_wall'); P.set(sgn * 10, 10, 3, 'polished_blackstone_wall'); P.set(sgn * 12, 10, 2, 'polished_blackstone_wall'); P.set(sgn * 10, 10, 2, 'polished_blackstone_wall');
  }
  P.fill(-12, 9, 1, 12, 9, 3, 'cut_brass'); P.fill(-12, 10, 1, 12, 10, 1, 'polished_blackstone_bricks_wall');
  // opening + stepped arch
  P.clear(-5, 0, 0, 5, 4, 3);
  for (let y = 5; y <= 7; y++) {
    const open = 9 - y;                      // 4,3,2
    P.clear(-open, y, 0, open, y, 3);
    for (let z = 0; z <= 3; z++) { P.set(-(open + 1), y, z, 'polished_blackstone_bricks_stairs@W'); P.set(open + 1, y, z, 'polished_blackstone_bricks_stairs@E'); }
  }
  for (let z = 0; z <= 3; z++) for (let x = -2; x <= 2; x++) P.set(x, 8, z, x === 0 ? 'cut_brass' : 'chiseled_polished_blackstone');
  // portcullis raised in the arch
  for (let x = -3; x <= 3; x++) for (let y = 5; y <= 7; y++) if (Math.abs(x) <= 8 - y) P.set(x, y, 1, 'iron_bars');
  for (const x of [-3, 3]) P.set(x, 4, 1, 'chain');
  P.fill(-5, -1, 0, 5, -1, 3, 'cut_brass'); for (let z = 0; z <= 3; z++) for (const x of [-5, 5]) P.set(x, -1, z, 'polished_blackstone_bricks');
  // forecourt: ember braziers, steps
  for (const sgn of [-1, 1]) { bigBrazier(P, sgn * 7, 0, 6, false); for (let x = 3; x <= 5; x++) P.set(sgn * x, -1, 4, 'cut_brass'); }
  signAt(ctx, P, 0, 9.6, 3.55, 0, { text: 'THE IRONCLAD HOLD', sub: 'Hearthguard · Arbalists · Shieldbreakers · Engines', w: 7.2, h: 1.1, style: 'iron', size: 0.5 });
  bannerAt(ctx, P, -9.5, 8.4, 3.6, 0, { w: 1.7, h: 4.8, emblem: 'anvil', bg: 0x1c1a20, trim: 0xc9a24a, kind: 'wall' });
  bannerAt(ctx, P, 9.5, 8.4, 3.6, 0, { w: 1.7, h: 4.8, emblem: 'anvil', bg: 0x1c1a20, trim: 0xc9a24a, kind: 'wall' });
  // siege engines on the plaza
  propAt(ctx, P, -17, 0, 11, 25, { type: 'ballista', scale: 1 }); propAt(ctx, P, 17, 0, 11, -25, { type: 'mortar', scale: 1 });
  P.fill(-19, -1, 9, -15, -1, 13, 'cobblestone'); P.fill(15, -1, 9, 19, -1, 13, 'cobblestone');
  // chimneys through the Command Terrace floor with smoke
  for (const x of [-9, 9]) {
    P.fill(x - 1, 9, -8, x + 1, 17, -6, 'polished_blackstone_bricks'); P.clear(x, 10, -7, x, 17, -7); P.fill(x - 1, 18, -8, x + 1, 18, -6, 'polished_blackstone_bricks_slab'); P.clear(x, 18, -7, x, 18, -7);
    for (const [dx, dz] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) P.set(x + dx, 19, -7 + dz, 'polished_blackstone_wall');
    P.set(x, 10, -7, 'magma_block'); P.set(x, 9, -7, 'magma_block');
    emitAt(ctx, P, x, 18.5, -7, { type: 'smoke', w: 1, h: 10, n: 18 });
  }
  ctx.reserve(-14, -54, 14, -29);
  ctx.zone({ id: 'ironclad', name: 'The Ironclad Hold', color: '#f08a3c', rect: [-14, -54, 14, -29], y: [5, 26], tour: { x: 0, y: 7, z: -22, yaw: 0, pitch: 4 },
    house: 'ironclad', what: 'The Ironclad House tent: a squat forge-hall carved into the cliff beneath the Command Terrace. The player recruits Ironclad troops here; each of the six troops stands on a lit plinth (LOCKED or the player\'s star rating) and Brassjaw, the Forge Colossus, dominates the back on a raised dais. Outside stand a ballista and a mortar.',
    builder: ['Palette: polished blackstone bricks, deepslate tiles, brass (cut copper / gold), ember orange (shroomlight, magma, blast furnaces, lava behind iron bars).', 'Hall 23 x 16 x 8 inside the cliff, entrance arch 11 wide x 8 tall (stepped with stair blocks), raised portcullis of iron bars.', 'Troop alcoves: 3x3 plinth recessed 3 deep into the side walls with a shroomlight above.', 'Brassjaw: 5x5 plinth with a raised gold-block top, a magma-block seal behind.', 'Two chimneys rise through the Command Terrace floor and smoke.'] });
}
