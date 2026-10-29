// Dressing for the two courts: lamp posts along the roads, and groves of trees on the outskirts only (never between the buildings).
import { BY_NAME } from '../blocks.js';
import { hash } from './ctx.js';
import { ironLamp, oak, cherry, spruce } from './kit.js';
import { CAMP, HILL, inPoly, edgeDist } from './layout.js';

export function buildGarden(ctx) {
  const w = ctx.w, T = w.pen(0, 6, 0), P = w.pen(0, 0, 0);
  w.beginZone('court');
  // lamps: a ring on the hill apron and posts along the plaza flanks
  for (let i = 0; i < 10; i++) {
    const a = (i + 0.5) * Math.PI / 5, x = Math.round(HILL.x + Math.sin(a) * (HILL.wall + 5.5)), z = Math.round(HILL.z - Math.cos(a) * (HILL.wall + 5.5));
    if (Math.abs(x) < 9 && z > HILL.z) continue;
    if (inPoly(x, z)) ironLamp(T, x, 0, z, 5);
  }
  for (const sgn of [-1, 1]) for (const z of [32, 58, 70]) ironLamp(P, sgn * 39, 0, z, 5);
  for (const sgn of [-1, 1]) for (const x of [14, 28, 44, 60]) ironLamp(T, sgn * x, 0, 20, 4);

  const near = (x, z) => { for (const [dx, dz] of [[4, 0], [-4, 0], [0, 4], [0, -4]]) if (ctx.isReserved(x + dx, z + dz)) return true; return ctx.isReserved(x, z); };
  const grass = BY_NAME.grass_block, podzol = BY_NAME.podzol, moss = BY_NAME.moss_block;
  const plant = (x, z, h, kind) => {
    const r = hash(x, z, 77);
    if (kind === 'terrace' && r < 0.18) cherry(w, x, h, z);
    else if (r < 0.5) oak(w, x, h, z, 5 + Math.floor(r * 8), 'oak_leaves', 'oak_log', 3);
    else if (r < 0.7) oak(w, x, h, z, 6, 'dark_oak_leaves', 'dark_oak_log', 3);
    else if (r < 0.82) oak(w, x, h, z, 5, 'birch_leaves', 'birch_log', 3);
    else spruce(w, x, h, z, 8, 3);
  };
  // terrace outskirts: a loose grove strip 3-12 blocks inside the rim
  for (let z = -110; z <= 25; z += 2) for (let x = -76; x <= 76; x += 2) {
    const jx = x + Math.floor(hash(x, z, 81) * 2), jz = z + Math.floor(hash(x, z, 82) * 2);
    if (!inPoly(jx, jz)) continue;
    const e = edgeDist(jx, jz); if (e < 3 || e > 12) continue;
    if (hash(jx, jz, 83) > 0.34 * (1 - (e - 3) / 12)) continue;
    if (near(jx, jz)) continue;
    const b = w.get(jx, 5, jz); if (b !== grass && b !== podzol && b !== moss) continue;
    if (w.get(jx, 6, jz) !== 0) continue;
    plant(jx, jz, 6, 'terrace');
  }
  // lower camp outskirts (not the south, where the gate wall stands)
  for (let z = CAMP.z1; z <= 80; z += 2) for (let x = CAMP.x1; x <= CAMP.x2; x += 2) {
    const jx = x + Math.floor(hash(x, z, 91) * 2), jz = z + Math.floor(hash(x, z, 92) * 2);
    const e = Math.min(jx - CAMP.x1, CAMP.x2 - jx, jz - CAMP.z1); if (e < 2 || e > 7) continue;
    if (hash(jx, jz, 93) > 0.3) continue;
    if (near(jx, jz)) continue;
    const b = w.get(jx, -1, jz); if (b !== grass && b !== podzol && b !== moss) continue;
    if (w.get(jx, 0, jz) !== 0) continue;
    plant(jx, jz, 0, 'camp');
  }
  w.endZone();
}
