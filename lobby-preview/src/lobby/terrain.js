// Terrain: a mountain bowl with three carved terraces (court, House Ring, Command Terrace) and a valley to the south.
import { WX0, WX1, WY0, WZ0, WZ1, SX } from '../world.js';
import { BY_NAME } from '../blocks.js';
import { fbm, smooth, lerp, hash } from './ctx.js';

// Terraces: x1,z1,x2,z2,height (height = walkable surface y)
export const T1 = { x1: -62, z1: 5, x2: 62, z2: 62, h: 0 };
export const T2 = { x1: -62, z1: -36, x2: 62, z2: 4, h: 6 };
export const T3 = { x1: -42, z1: -76, x2: 42, z2: -37, h: 16 };
const REGIONS = [T1, T2, T3];

function rectDist(r, x, z) { const dx = Math.max(r.x1 - x, 0, x - r.x2), dz = Math.max(r.z1 - z, 0, z - r.z2); return Math.hypot(dx, dz); }

function rock(x, z) {
  // warped ridged noise gives craggy peaks instead of smooth ramps
  const wx = x + 14 * (fbm(x * 0.02, z * 0.02, 21) - 0.5), wz = z + 14 * (fbm(x * 0.02 + 9, z * 0.02, 22) - 0.5);
  const ridge = 1 - Math.abs(2 * fbm(wx * 0.045, wz * 0.045, 4) - 1);
  const m = 9 + 10 * fbm(wx * 0.03, wz * 0.03, 3) + 15 * ridge * ridge + 0.26 * Math.max(0, Math.abs(x) - 56) + 30 * smooth(-44, -88, z) * (0.7 + 0.6 * ridge) + 3 * fbm(x * 0.15, z * 0.15, 9);
  if (z > 58) {
    const valley = -1.5 - (z - 62) * 0.26 + 3.5 * (fbm(x * 0.06, z * 0.06, 5) - 0.5);
    const hills = 5 + 10 * fbm(x * 0.05, z * 0.05, 7) + 8 * ridge * ridge + 0.3 * Math.max(0, Math.abs(x) - 40);
    return lerp(valley, hills, smooth(34, 62, Math.abs(x)));
  }
  return m;
}

export function groundHeight(x, z) {
  let best = null, bd = 1e9;
  for (const r of REGIONS) { const d = rectDist(r, x, z); if (d < bd || (d === bd && r.h > best.h)) { bd = d; best = r; } }
  if (bd === 0) return best.h;
  const ro = rock(x, z);
  return lerp(best.h, ro, smooth(0, 28, bd));
}

export function buildTerrain(ctx) {
  const w = ctx.w;
  const id = (n) => BY_NAME[n];
  const stone = id('stone'), deep = id('deepslate'), dirt = id('dirt'), grass = id('grass_block'), andesite = id('andesite'), cobble = id('cobblestone'), mossCobble = id('mossy_cobblestone'), snow = id('snow_block'), tuff = id('tuff'), gravel = id('gravel'), moss = id('moss_block'), podzol = id('podzol'), coarse = id('coarse_dirt'), sand = id('sand'), water = id('water'), basalt = id('basalt');
  const H = new Float32Array(176 * 176);
  const hi = (x, z) => (z - WZ0) * SX + (x - WX0);
  for (let z = WZ0; z <= WZ1; z++) for (let x = WX0; x <= WX1; x++) H[hi(x, z)] = groundHeight(x, z);
  // world edge walls so the border is never visible
  for (let z = WZ0; z <= WZ1; z++) for (let x = WX0; x <= WX1; x++) {
    const e = Math.min(x - WX0, WX1 - x, z - WZ0, WZ1 - z);
    if (e < 8) { const south = (WZ1 - z) === e; H[hi(x, z)] = Math.max(H[hi(x, z)], south ? 1 + (8 - e) * 1.2 + 4 * fbm(x * 0.1, z * 0.1, 2) : 10 + (8 - e) * 3 + 6 * fbm(x * 0.1, z * 0.1, 2)); }
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
        if (h > 38) b = snow;
        else if (h > 30 && slope > 1.0) b = n > 0.55 ? snow : andesite;
        else if (slope > 3.0) b = n > 0.6 ? andesite : n > 0.4 ? stone : cobble;
        else if (slope > 1.9) b = n > 0.55 ? mossCobble : n > 0.35 ? stone : coarse;
        else if (h < -5 && z > 58) b = (n > 0.5 ? sand : gravel);
        else b = (n > 0.72 ? podzol : n < 0.18 ? moss : grass);
      }
      w.setRaw(x, y, z, b);
    }
  }
}
