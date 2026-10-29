// Landscape dressing: valley road, river and bridge, battlefield beacons, forests, flowers.
import { WX0, WX1, WZ0, WZ1, SX } from '../world.js';
import { BY_NAME } from '../blocks.js';
import { hash, fbm, smooth } from './ctx.js';
import { spruce, oak, cherry, flowers, azaleaBush, bigBrazier, ironLamp } from './kit.js';
import { signAt, bannerAt, emitAt } from './kit.js';
import { plateauDist, RIVER_Z } from './layout.js';

export function buildValley(ctx) {
  const w = ctx.w, P = w.pen(0, 0, 0);
  w.beginZone('valley');
  const surf = (x, z) => ctx.surface(x, z);
  // --- road from the gates down the valley (gravel, cobble edges), light posts every 8
  for (let z = 84; z <= 117; z++) {
    const cx = Math.round(Math.sin(z * 0.11) * 3);
    for (let x = cx - 3; x <= cx + 3; x++) {
      const h = surf(x, z); if (h === undefined) continue;
      const edge = Math.abs(x - cx) === 3;
      for (let y = h - 1; y <= h + 3; y++) if (y > h - 1) w.setRaw(x, y, z, 0);
      w.set(x, h - 1, z, edge ? 'cobblestone' : (hash(x, z, 4) > 0.5 ? 'gravel' : 'coarse_dirt'));
      ctx.reserve(x, z, x, z);
    }
    if (z % 8 === 0) { for (const sgn of [-1, 1]) { const x = cx + sgn * 4, h = surf(x, z); w.set(x, h, z, 'dark_oak_fence'); w.set(x, h + 1, z, 'dark_oak_fence'); w.set(x, h + 2, z, 'dark_oak_fence'); w.set(x, h + 3, z, 'lantern'); } }
  }
  // --- river across the valley with a stone bridge on the road
  const RZ = RIVER_Z;
  const level = Math.round(surf(0, RZ)) - 2;
  for (let x = WX0 + 6; x <= WX1 - 6; x++) {
    const zc = RZ + Math.round(Math.sin(x * 0.09) * 3);
    for (let dz = -3; dz <= 3; dz++) {
      const z = zc + dz; const h = surf(x, z); if (h === undefined) continue;
      const half = Math.abs(dz) === 3;
      for (let y = level + 2; y <= h + 6; y++) w.setRaw(x, y, z, 0);
      w.set(x, level + 1, z, half ? 'sand' : 'air'); if (half && h > level + 1) { /* bank */ }
      if (!half) { w.set(x, level, z, 'water'); w.set(x, level - 1, z, 'water'); w.set(x, level - 2, z, hash(x, z, 5) > 0.5 ? 'gravel' : 'sand'); }
      else { w.set(x, level, z, 'sand'); }
      ctx.reserve(x, z, x, z);
      if (!half && hash(x, z, 9) > 0.93) w.set(x, level - 1, z, 'seagrass');
    }
    // banks: fill cells beside the channel up to the level so grass meets the water
    for (const dz of [-4, 4]) { const z = zc + dz; const h = surf(x, z); if (h !== undefined && h < level + 2) for (let y = h; y <= level + 1; y++) w.set(x, y, z, 'grass_block'); }
  }
  // bridge: shallow hump, stone brick walls, lanterns
  const bcx = Math.round(Math.sin(RZ * 0.11) * 3);
  for (let z = RZ - 7; z <= RZ + 7; z++) {
    const dz = Math.abs(z - RZ), hump = dz <= 1 ? 2 : dz <= 3 ? 1 : 0;
    for (let x = bcx - 4; x <= bcx + 4; x++) {
      const deck = level + hump, edge = Math.abs(x - bcx) === 4;
      for (let y = deck + 1; y <= deck + 4; y++) w.setRaw(x, y, z, 0);
      w.set(x, deck, z, edge ? 'stone_bricks' : (hash(x, z, 8) > 0.6 ? 'cobblestone' : 'stone_bricks'));
      if (edge || x === bcx) for (let y = level - 1; y < deck; y++) w.set(x, y, z, 'stone_bricks');
      if (edge) { w.set(x, deck + 1, z, 'stone_bricks_wall'); if ((z - RZ) % 4 === 0) w.set(x, deck + 2, z, 'lantern'); }
    }
  }
  ctx.sign({ x: bcx + 0.5, y: level + 3.2, z: RZ - 7.4, yaw: 0, text: 'RIVERGATE BRIDGE', sub: 'the road to the five battlefields', w: 3.6, h: 0.8, style: 'wood', size: 0.5 });

  /* --- five beacon towers on the far hills: one fire per battlefield --- */
  const B = [['RIVERGATE VALLEY', -66, 0x3a8ac8, 'trident'], ['IRONHOLD SIEGE', -33, 0xf08a3c, 'anvil'], ['FROSTMARCH PASS', 0, 0xcfe8ff, 'moon'], ['HOLLOWWOOD REACH', 33, 0x5a7a3a, 'skull'], ['KINGSFIELD CROSSROADS', 66, 0xe6bd48, 'crown']];
  for (const [name, x, col, emb] of B) {
    const z = 112; const h = Math.max(surf(x, z), surf(x + 2, z), surf(x - 2, z));
    const Q = w.pen(x, h, z, 2);
    Q.fill(-2, -4, -2, 2, -1, 2, 'cobblestone'); Q.fill(-2, 0, -2, 2, 0, 2, 'stone_bricks');
    Q.fill(-1, 1, -1, 1, 8, 1, 'stone_bricks'); for (const [dx, dz] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) Q.fill(dx, 1, dz, dx, 8, dz, 'polished_andesite');
    Q.fill(-2, 9, -2, 2, 9, 2, 'stone_bricks'); for (const [dx, dz] of [[-2, -2], [2, -2], [-2, 2], [2, 2]]) Q.set(dx, 10, dz, 'stone_bricks_wall');
    Q.set(0, 10, 0, 'campfire'); Q.set(0, 9, 0, 'magma_block');
    for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) Q.set(dx, 10, dz, 'stone_bricks_slab');
    bannerAt(ctx, Q, 0, 8.4, 2.6, 0, { w: 1.4, h: 3.4, emblem: emb, bg: col, trim: 0xc9a24a, kind: 'wall' });
    emitAt(ctx, Q, 0, 11, 0, { type: 'ember', w: 1.2, h: 6, n: 26 }); emitAt(ctx, Q, 0, 12, 0, { type: 'smoke', w: 1, h: 14, n: 14 });
    ctx.reserve(x - 3, z - 3, x + 3, z + 3);
    ctx.markers.push({ x, z, label: name, kind: 'beacon' });
  }
  ctx.zone({ id: 'valley', name: 'The Valley & the Five Beacons', color: '#6a8a5a', rect: [-80, 84, 80, 118], y: [-14, 24], tour: { x: 0, y: 1, z: 88, yaw: 180, pitch: 4 },
    what: 'Beyond the War Gates a gravel road runs south across a stone bridge over a river and on toward the battlefields. Five beacon towers on the far hills carry one fire per battlefield: Rivergate, Ironhold, Frostmarch, Hollowwood and Kingsfield. They are purely scenery (the matches are held on separate maps) but they give the gates a horizon.',
    builder: ['Road 7 wide: gravel and coarse dirt with a cobblestone edge, dark oak fence lamp posts every 8 blocks.', 'River 5 wide, 2 deep, banks of sand; bridge 9 wide with a shallow hump, stone brick walls and lanterns.', 'Beacon towers: 3x3 stone brick shafts 9 high with a campfire on a magma block and a House-coloured banner.'] });

  /* --- forests, grass and flowers --- */
  const id = BY_NAME;
  let trees = 0;
  for (let z = WZ0 + 3; z <= WZ1 - 3; z += 2) for (let x = WX0 + 3; x <= WX1 - 3; x += 2) {
    const jx = x + Math.floor(hash(x, z, 31) * 2), jz = z + Math.floor(hash(x, z, 32) * 2);
    if (jx < WX0 + 2 || jx > WX1 - 2 || jz < WZ0 + 2 || jz > WZ1 - 2) continue;
    if (ctx.isReserved(jx, jz) || plateauDist(jx, jz) < 5) continue;   // no trees on the two courts or hugging their edges
    const h = surf(jx, jz); const below = w.get(jx, h - 1, jz);
    if (below !== id.grass_block && below !== id.podzol && below !== id.moss_block) continue;
    if (w.get(jx, h, jz) !== 0) continue;
    const far = smooth(4, 44, plateauDist(jx, jz));
    const inValley = jz > 90;
    let p = 0.015 + far * 0.2 + (inValley ? 0.05 : 0); p *= 0.55 + 0.9 * fbm(jx * 0.07, jz * 0.07, 12);
    if (h > 40) p = 0;
    const roll = hash(jx, jz, 33);
    if (roll < p) {
      // keep clear of reserved neighbours (leaf radius)
      let ok = true; for (const [dx, dz] of [[3, 0], [-3, 0], [0, 3], [0, -3]]) if (ctx.isReserved(jx + dx, jz + dz)) ok = false; if (!ok) continue;
      const k = hash(jx, jz, 34);
      if (h > 14 || far > 0.65) spruce(w, jx, h, jz, 8 + Math.floor(hash(jx, jz, 35) * 6), 3);
      else if (k < 0.4) oak(w, jx, h, jz, 5 + Math.floor(k * 6), 'oak_leaves', 'oak_log', 3);
      else if (k < 0.72) oak(w, jx, h, jz, 6, 'dark_oak_leaves', 'dark_oak_log', 3);
      else if (k < 0.84) oak(w, jx, h, jz, 5, 'birch_leaves', 'birch_log', 3);
      else spruce(w, jx, h, jz, 8, 3);
      trees++;
    }
  }
  // ground cover
  for (let z = WZ0 + 2; z <= WZ1 - 2; z++) for (let x = WX0 + 2; x <= WX1 - 2; x++) {
    if (ctx.isReserved(x, z)) continue;
    const h = surf(x, z); const below = w.get(x, h - 1, z); if (below !== id.grass_block && below !== id.podzol) continue; if (w.get(x, h, z) !== 0) continue;
    const r = hash(x, z, 40), near = plateauDist(x, z) < 6;
    if (r < 0.11) w.set(x, h, z, 'short_grass');
    else if (r < 0.135) w.set(x, h, z, 'tall_grass');
    else if (r < 0.155) w.set(x, h, z, 'fern');
    else if (near && r < 0.19) flowers(w, x, h, z);
    else if (!near && r < 0.17 && below === id.podzol) w.set(x, h, z, hash(x, z, 41) > 0.5 ? 'brown_mushroom' : 'red_mushroom');
    else if (r > 0.995 && h < 30) { // boulder
      w.set(x, h, z, 'mossy_cobblestone'); w.set(x + 1, h, z, 'cobblestone'); w.set(x, h + 1, z, 'cobblestone'); w.setRaw(x, h, z + 1, id.mossy_cobblestone);
    }
  }
  w.endZone();
}
