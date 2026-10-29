// Terrain: a mountain bowl holding the Lower Camp (y 64), the House Terrace (y 70) with the Command Hill (y 76), and a valley to the south.
import { WX0, WX1, WY0, WZ0, WZ1, SX, SZ } from '../world.js';
import { BY_NAME } from '../blocks.js';
import { fbm, smooth, lerp, hash } from './ctx.js';
import { CAMP, TERRACE, HILL, polyDist } from './layout.js';

function rectDist(r, x, z) { const dx = Math.max(r.x1 - x, 0, x - r.x2), dz = Math.max(r.z1 - z, 0, z - r.z2); return Math.hypot(dx, dz); }

function rock(x, z) {
  // warped ridged noise gives craggy peaks instead of smooth ramps
  const wx = x + 14 * (fbm(x * 0.02, z * 0.02, 21) - 0.5), wz = z + 14 * (fbm(x * 0.02 + 9, z * 0.02, 22) - 0.5);
  const ridge = 1 - Math.abs(2 * fbm(wx * 0.045, wz * 0.045, 4) - 1);
  const m = 9 + 10 * fbm(wx * 0.03, wz * 0.03, 3) + 15 * ridge * ridge + 0.26 * Math.max(0, Math.abs(x) - 76) + 30 * smooth(-84, -124, z) * (0.7 + 0.6 * ridge) + 3 * fbm(x * 0.15, z * 0.15, 9);
  const zz = z - 32;                       // the valley starts south of the camp
  if (zz > 58) {
    const valley = -1.5 - (zz - 62) * 0.26 + 3.5 * (fbm(x * 0.06, z * 0.06, 5) - 0.5);
    const hills = 5 + 10 * fbm(x * 0.05, z * 0.05, 7) + 8 * ridge * ridge + 0.3 * Math.max(0, Math.abs(x) - 40);
    return lerp(valley, hills, smooth(34, 62, Math.abs(x)));
  }
  return m;
}

// Two flat plateaus (the Lower Camp and the House Terrace), a raised Command Hill inside the terrace, rock everywhere else.
export function groundHeight(x, z) {
  const dT = polyDist(x, z), dC = rectDist(CAMP, x, z);
  if (dT === 0) return Math.hypot(x - HILL.x, z - HILL.z) <= HILL.wall ? HILL.h : TERRACE.h;
  if (dC === 0) return CAMP.h;
  const [h, bd] = dT <= dC ? [TERRACE.h, dT] : [CAMP.h, dC];
  return lerp(h, rock(x, z), smooth(0, 40, bd));
}

export function buildTerrain(ctx) {
  const w = ctx.w;
  const id = (n) => BY_NAME[n];
  const stone = id('stone'), deep = id('deepslate'), dirt = id('dirt'), grass = id('grass_block'), andesite = id('andesite'), cobble = id('cobblestone'), mossCobble = id('mossy_cobblestone'), snow = id('snow_block'), tuff = id('tuff'), gravel = id('gravel'), moss = id('moss_block'), podzol = id('podzol'), coarse = id('coarse_dirt'), sand = id('sand'), water = id('water'), basalt = id('basalt');
  const H = new Float32Array(SX * SZ);
  const hi = (x, z) => (z - WZ0) * SX + (x - WX0);
  for (let z = WZ0; z <= WZ1; z++) for (let x = WX0; x <= WX1; x++) { let g = groundHeight(x, z); if (g > 22) g = 22 + (g - 22) * 0.62; H[hi(x, z)] = g; }
  // world edge walls so the border is never visible
  for (let z = WZ0; z <= WZ1; z++) for (let x = WX0; x <= WX1; x++) {
    const e = Math.min(x - WX0, WX1 - x, z - WZ0, WZ1 - z);
    if (e < 10) { const south = (WZ1 - z) === e; H[hi(x, z)] = Math.max(H[hi(x, z)], south ? 1 + (10 - e) * 1.0 + 4 * fbm(x * 0.1, z * 0.1, 2) : 14 + (10 - e) * 2.2 + 6 * fbm(x * 0.1, z * 0.1, 2)); }
  }
  for (let z = WZ0; z <= WZ1; z++) for (let x = WX0; x <= WX1; x++) {
    const h = Math.round(H[hi(x, z)]);
    ctx.setSurface(x, z, h);
    // slope = steepest drop to the 4 neighbours
    let slope = 0;
    for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) { const nx = x + dx, nz = z + dz; if (nx < WX0 || nx > WX1 || nz < WZ0 || nz > WZ1) continue; slope = Math.max(slope, Math.abs(H[hi(nx, nz)] - H[hi(x, z)])); }
    const n = fbm(x * 0.2, z * 0.2, 11);
    for (let y = WY0; y < h; y++) {
      let b;
      if (y < -9) b = deep;
      else if (y < h - 3) b = (hash(x, y, z) > 0.93 ? tuff : y < -3 ? (hash(x + 3, y, z) > 0.85 ? deep : stone) : stone);
      else if (y < h - 1) b = slope > 1.6 ? stone : dirt;
      else {
        // surface block
        if (h > 36) b = snow;
        else if (h > 30 && slope > 1.0) b = n > 0.55 ? snow : andesite;
        else if (slope > 3.0) b = n > 0.6 ? andesite : n > 0.4 ? stone : cobble;
        else if (slope > 1.9) b = n > 0.55 ? mossCobble : n > 0.35 ? stone : coarse;
        else if (h < -5 && z > 90) b = (n > 0.5 ? sand : gravel);
        else b = (n > 0.72 ? podzol : n < 0.18 ? moss : grass);
      }
      w.setRaw(x, y, z, b);
    }
  }
}
