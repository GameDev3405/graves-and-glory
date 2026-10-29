// Shared build context: the voxel world plus registries that main.js turns into 3D entities.
import { World, WX0, WX1, WZ0, WZ1, SX, SZ } from '../world.js';

export class Ctx {
  constructor() {
    this.w = new World();
    this.zones = [];       // zone descriptions for the tour + blueprint map
    this.npcs = [];        // named characters
    this.troops = [];      // troop display plinths
    this.props = [];       // free-standing voxel models (siege engines, animals, statues ...)
    this.signs = [];       // painted signboards
    this.banners = [];     // cloth that waves
    this.emitters = [];    // particles: smoke, embers, soul wisps, fireflies ...
    this.markers = [];     // points of interest for the blueprint map
    this.reserved = new Uint8Array(SX * SZ);   // columns that trees/props must leave alone
    this.heights = new Int16Array(SX * SZ);    // terrain surface height per column (first air y)
  }
  reserve(x1, z1, x2, z2) {
    for (let z = Math.max(WZ0, Math.min(z1, z2)); z <= Math.min(WZ1, Math.max(z1, z2)); z++)
      for (let x = Math.max(WX0, Math.min(x1, x2)); x <= Math.min(WX1, Math.max(x1, x2)); x++) this.reserved[(z - WZ0) * SX + (x - WX0)] = 1;
  }
  isReserved(x, z) { return this.reserved[(z - WZ0) * SX + (x - WX0)] === 1; }
  surface(x, z) { return this.heights[(z - WZ0) * SX + (x - WX0)]; }
  setSurface(x, z, h) { this.heights[(z - WZ0) * SX + (x - WX0)] = h; }
  zone(def) { this.zones.push(def); return def; }
  sign(s) { this.signs.push(s); }
  banner(b) { this.banners.push(b); }
  emit(e) { this.emitters.push(e); }
  npc(n) { this.npcs.push(n); }
  troop(t) { this.troops.push(t); }
  prop(p) { this.props.push(p); }
}

/* ---------- noise ---------- */
function hash(x, z, s) { let h = (Math.imul(x, 374761393) + Math.imul(z, 668265263) + Math.imul(s, 982451653)) | 0; h = Math.imul(h ^ (h >>> 13), 1274126177); h ^= h >>> 16; return (h >>> 0) / 4294967296; }
export function vnoise(x, z, s = 0) {
  const xi = Math.floor(x), zi = Math.floor(z), xf = x - xi, zf = z - zi;
  const u = xf * xf * (3 - 2 * xf), v = zf * zf * (3 - 2 * zf);
  const a = hash(xi, zi, s), b = hash(xi + 1, zi, s), c = hash(xi, zi + 1, s), d = hash(xi + 1, zi + 1, s);
  return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
}
export function fbm(x, z, s = 0, oct = 4) { let a = 0.5, f = 1, sum = 0, n = 0; for (let i = 0; i < oct; i++) { sum += a * vnoise(x * f, z * f, s + i * 17); n += a; a *= 0.5; f *= 2.03; } return sum / n; }
export const smooth = (a, b, x) => { const t = Math.max(0, Math.min(1, (x - a) / (b - a))); return t * t * (3 - 2 * t); };
export const lerp = (a, b, t) => a + (b - a) * t;
export { hash };
