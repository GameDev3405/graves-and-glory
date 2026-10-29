// Voxel world: storage, a small building DSL ("Pen"), light flood-fill and a chunk mesher.
import * as THREE from '../vendor/three.module.min.js';
import { BLOCKS, BY_NAME, tileUV } from './blocks.js';

export const WX0 = -88, WY0 = -16, WZ0 = -88, SX = 176, SY = 80, SZ = 176;
export const WX1 = WX0 + SX - 1, WY1 = WY0 + SY - 1, WZ1 = WZ0 + SZ - 1;
export const DISPLAY_Y = 64;            // relative y 0 == Minecraft y 64 in the builder's readouts
const SLICE = SX * SZ;
export const DIRS = [[0, -1], [1, 0], [0, 1], [-1, 0]]; // N E S W as (dx,dz)
const LETTER = { N: 0, E: 1, S: 2, W: 3 };

/* ---------- spec parsing: 'stone_bricks_stairs@N^' = block, facing, top half; '~' = hanging ---------- */
const specCache = new Map();
export function parseSpec(spec) {
  let r = specCache.get(spec);
  if (r) return r;
  let s = spec, f = 0, top = false, hang = false;
  const m = /^([a-z0-9_]+)(?:@([NESW]))?(\^)?(~)?$/.exec(s);
  if (!m) throw new Error('bad block spec: ' + spec);
  const id = BY_NAME[m[1]];
  if (id === undefined) {
    if (!parseSpec.warned) parseSpec.warned = new Set();
    if (!parseSpec.warned.has(m[1])) { parseSpec.warned.add(m[1]); console.warn('UNKNOWN BLOCK ' + m[1] + ' (' + spec + ')'); }
    const r0 = { id: 0, f: 0, top: false, hang: false, hasF: false }; specCache.set(spec, r0); return r0;
  }
  if (m[2]) f = LETTER[m[2]];
  top = !!m[3]; hang = !!m[4];
  r = { id, f, top, hang, hasF: !!m[2] };
  specCache.set(spec, r);
  return r;
}
const packMeta = (f, top, hang) => (f & 3) | (top ? 4 : 0) | (hang ? 8 : 0);

/* ---------- lookup tables ---------- */
const NB = BLOCKS.length + 8;
const OPQ = new Uint8Array(NB), LBLK = new Uint8Array(NB), EMIT = new Uint8Array(NB);
for (const b of BLOCKS) { OPQ[b.id] = (b.opaque && b.shape === 'cube') ? 1 : 0; LBLK[b.id] = b.lightBlock; EMIT[b.id] = b.light; }

export class World {
  constructor() {
    const n = SX * SY * SZ;
    this.ids = new Uint16Array(n);
    this.meta = new Uint8Array(n);
    this.zoneOf = new Uint8Array(n);
    this.sky = new Uint8Array(n);
    this.blk = new Uint8Array(n);
    this.zone = 0;
    this.zoneNames = ['(terrain)'];
    this.rng = mulberry(1337);
  }
  idx(x, y, z) { return (y - WY0) * SLICE + (z - WZ0) * SX + (x - WX0); }
  inb(x, y, z) { return x >= WX0 && x <= WX1 && y >= WY0 && y <= WY1 && z >= WZ0 && z <= WZ1; }
  get(x, y, z) { return this.inb(x, y, z) ? this.ids[this.idx(x, y, z)] : 0; }
  getMeta(x, y, z) { return this.inb(x, y, z) ? this.meta[this.idx(x, y, z)] : 0; }
  setRaw(x, y, z, id, meta = 0) {
    if (!this.inb(x, y, z)) return;
    const i = this.idx(x, y, z);
    this.ids[i] = id; this.meta[i] = meta; this.zoneOf[i] = this.zone;
  }
  set(x, y, z, spec) { const s = parseSpec(spec); this.setRaw(x, y, z, s.id, packMeta(s.f, s.top, s.hang)); }
  beginZone(name) { let i = this.zoneNames.indexOf(name); if (i < 0) { i = this.zoneNames.length; this.zoneNames.push(name); } this.zone = i; return i; }
  endZone() { this.zone = 0; }
  isSolidAt(x, y, z) { const id = this.get(x, y, z); return id !== 0 && BLOCKS[id].collide; }
  topY(x, z, from = WY1) { for (let y = from; y >= WY0; y--) { const id = this.get(x, y, z); if (id !== 0 && BLOCKS[id].collide && BLOCKS[id].shape !== 'liquid') return y; } return WY0; }
  pen(ox = 0, oy = 0, oz = 0, rot = 0) { return new Pen(this, ox, oy, oz, rot); }

  /* ---------- lighting ---------- */
  computeLight() {
    const { ids, sky, blk } = this;
    sky.fill(0); blk.fill(0);
    // direct sky
    for (let z = 0; z < SZ; z++) for (let x = 0; x < SX; x++) {
      let s = 15;
      for (let y = SY - 1; y >= 0; y--) {
        const i = y * SLICE + z * SX + x, lb = LBLK[ids[i]];
        if (lb >= 15) s = 0; else if (lb > 0) s = Math.max(0, s - lb);
        sky[i] = s;
      }
    }
    const queue = new Int32Array(SX * SY * SZ);
    const spread = (seeds) => {
      const QN = queue.length;
      let qh = 0, qt = 0;
      for (const s of seeds) { queue[qt] = s; qt = (qt + 1) % QN; }
      const lvl = seeds.arr;
      while (qh !== qt) {
        const i = queue[qh]; qh = (qh + 1) % QN;
        const l = lvl[i];
        if (l <= 1) continue;
        const x = i % SX, z = ((i / SX) | 0) % SZ, y = (i / SLICE) | 0;
        for (let d = 0; d < 6; d++) {
          let n;
          if (d === 0) { if (x === 0) continue; n = i - 1; }
          else if (d === 1) { if (x === SX - 1) continue; n = i + 1; }
          else if (d === 2) { if (z === 0) continue; n = i - SX; }
          else if (d === 3) { if (z === SZ - 1) continue; n = i + SX; }
          else if (d === 4) { if (y === 0) continue; n = i - SLICE; }
          else { if (y === SY - 1) continue; n = i + SLICE; }
          const lb = LBLK[ids[n]];
          if (lb >= 15) continue;
          const nl = l - 1 - lb;
          if (nl > lvl[n]) { lvl[n] = nl; queue[qt] = n; qt = (qt + 1) % QN; }
        }
      }
    };
    // sky seeds: lit cells with a darker non-opaque neighbour
    const seeds = []; seeds.arr = sky;
    for (let y = 0; y < SY; y++) for (let z = 1; z < SZ - 1; z++) for (let x = 1; x < SX - 1; x++) {
      const i = y * SLICE + z * SX + x, l = sky[i];
      if (l <= 1) continue;
      if (sky[i - 1] < l - 1 || sky[i + 1] < l - 1 || sky[i - SX] < l - 1 || sky[i + SX] < l - 1 || (y > 0 && sky[i - SLICE] < l - 1)) seeds.push(i);
    }
    spread(seeds);
    // block seeds
    const bs = []; bs.arr = blk;
    for (let i = 0; i < ids.length; i++) { const e = EMIT[ids[i]]; if (e > 0) { blk[i] = e; bs.push(i); } }
    spread(bs);
  }
  lightAt(x, y, z) {
    x = Math.floor(x); y = Math.floor(y); z = Math.floor(z);
    if (!this.inb(x, y, z)) return [y >= WY1 ? 15 : 12, 0];
    const i = this.idx(x, y, z); return [this.sky[i], this.blk[i]];
  }
}

function mulberry(a) { return function () { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }
export { mulberry };

/* ---------- Pen: local coordinates (x east, z south, y up), rotated about an origin ---------- */
export class Pen {
  constructor(w, ox, oy, oz, rot = 0) { this.w = w; this.ox = ox; this.oy = oy; this.oz = oz; this.rot = ((rot % 4) + 4) % 4; }
  at(ox, oy, oz, rot = this.rot) { return new Pen(this.w, ox, oy, oz, rot); }
  // world-space helpers for entities (block centre; yaw: 0 = north, 90 = west, 180 = south, 270 = east)
  pos(lx, ly, lz) { return { x: this.mx(lx, lz) + 0.5, y: this.oy + ly, z: this.mz(lx, lz) + 0.5 }; }
  yaw(d = 0) { return (((180 - 90 * this.rot + d) % 360) + 360) % 360; }
  // local -> world (x,z)
  mx(lx, lz) { switch (this.rot) { case 0: return this.ox + lx; case 1: return this.ox - lz; case 2: return this.ox - lx; default: return this.ox + lz; } }
  mz(lx, lz) { switch (this.rot) { case 0: return this.oz + lz; case 1: return this.oz + lx; case 2: return this.oz - lz; default: return this.oz - lx; } }
  wx(lx, lz) { return this.mx(lx, lz); }
  wz(lx, lz) { return this.mz(lx, lz); }
  set(lx, ly, lz, spec) {
    const s = parseSpec(spec);
    this.w.setRaw(this.mx(lx, lz), this.oy + ly, this.mz(lx, lz), s.id, packMeta(s.hasF ? (s.f + this.rot) & 3 : 0, s.top, s.hang));
  }
  setIfAir(lx, ly, lz, spec) { if (this.w.get(this.mx(lx, lz), this.oy + ly, this.mz(lx, lz)) === 0) this.set(lx, ly, lz, spec); }
  get(lx, ly, lz) { return this.w.get(this.mx(lx, lz), this.oy + ly, this.mz(lx, lz)); }
  fill(x1, y1, z1, x2, y2, z2, spec) {
    const [a, b] = [Math.min(x1, x2), Math.max(x1, x2)], [c, d] = [Math.min(y1, y2), Math.max(y1, y2)], [e, f] = [Math.min(z1, z2), Math.max(z1, z2)];
    for (let y = c; y <= d; y++) for (let z = e; z <= f; z++) for (let x = a; x <= b; x++) this.set(x, y, z, spec);
  }
  fillIfAir(x1, y1, z1, x2, y2, z2, spec) {
    const [a, b] = [Math.min(x1, x2), Math.max(x1, x2)], [c, d] = [Math.min(y1, y2), Math.max(y1, y2)], [e, f] = [Math.min(z1, z2), Math.max(z1, z2)];
    for (let y = c; y <= d; y++) for (let z = e; z <= f; z++) for (let x = a; x <= b; x++) this.setIfAir(x, y, z, spec);
  }
  clear(x1, y1, z1, x2, y2, z2) { this.fill(x1, y1, z1, x2, y2, z2, 'air'); }
  // four vertical walls (hollow rectangle prism without floor/roof)
  walls(x1, y1, z1, x2, y2, z2, spec) {
    for (let y = y1; y <= y2; y++) {
      for (let x = x1; x <= x2; x++) { this.set(x, y, z1, spec); this.set(x, y, z2, spec); }
      for (let z = z1; z <= z2; z++) { this.set(x1, y, z, spec); this.set(x2, y, z, spec); }
    }
  }
  shell(x1, y1, z1, x2, y2, z2, spec) { this.walls(x1, y1, z1, x2, y2, z2, spec); this.fill(x1, y1, z1, x2, y1, z2, spec); this.fill(x1, y2, z1, x2, y2, z2, spec); }
  // filled disc / cylinder (centre cx,cz; radius r) from y1..y2
  disc(cx, y1, cz, r, spec, y2 = y1) {
    const r2 = (r + 0.5) * (r + 0.5);
    for (let y = y1; y <= y2; y++) for (let z = -r - 1; z <= r + 1; z++) for (let x = -r - 1; x <= r + 1; x++) if (x * x + z * z <= r2) this.set(cx + x, y, cz + z, spec);
  }
  discIfAir(cx, y1, cz, r, spec, y2 = y1) {
    const r2 = (r + 0.5) * (r + 0.5);
    for (let y = y1; y <= y2; y++) for (let z = -r - 1; z <= r + 1; z++) for (let x = -r - 1; x <= r + 1; x++) if (x * x + z * z <= r2) this.setIfAir(cx + x, y, cz + z, spec);
  }
  ring(cx, y1, cz, r, spec, y2 = y1, thick = 1) {
    const ro = (r + 0.5) * (r + 0.5), ri = Math.max(0, (r + 0.5 - thick)) ** 2;
    for (let y = y1; y <= y2; y++) for (let z = -r - 1; z <= r + 1; z++) for (let x = -r - 1; x <= r + 1; x++) { const d = x * x + z * z; if (d <= ro && d > ri) this.set(cx + x, y, cz + z, spec); }
  }
  // dome: solid or hollow half-sphere
  dome(cx, cy, cz, r, spec, hollow = false) {
    const ro = (r + 0.5) ** 2, ri = (r - 0.5) ** 2;
    for (let y = 0; y <= r; y++) for (let z = -r; z <= r; z++) for (let x = -r; x <= r; x++) { const d = x * x + y * y + z * z; if (d <= ro && (!hollow || d > ri)) this.set(cx + x, cy + y, cz + z, spec); }
  }
  sphere(cx, cy, cz, r, spec, skip = 0) {
    const ro = (r + 0.5) ** 2; const rnd = this.w.rng;
    for (let y = -r; y <= r; y++) for (let z = -r; z <= r; z++) for (let x = -r; x <= r; x++) if (x * x + y * y + z * z <= ro && (skip === 0 || rnd() > skip)) this.setIfAir(cx + x, cy + y, cz + z, spec);
  }
  line(x1, y1, z1, x2, y2, z2, spec) {
    const n = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1), Math.abs(z2 - z1));
    for (let i = 0; i <= n; i++) { const t = n === 0 ? 0 : i / n; this.set(Math.round(x1 + (x2 - x1) * t), Math.round(y1 + (y2 - y1) * t), Math.round(z1 + (z2 - z1) * t), spec); }
  }
  // pillar helper
  pillar(x, y1, z, y2, spec, cap = null, base = null) {
    this.fill(x, y1, z, x, y2, z, spec);
    if (cap) this.set(x, y2, z, cap);
    if (base) this.set(x, y1, z, base);
  }
  // Gable roof between local rectangle x1..x2, z1..z2 starting at height y. ridge runs along 'axis' ('x' or 'z').
  gable(x1, z1, x2, z2, y, axis, stairs, fill, ridge = null, overhang = 1) {
    const slab = ridge;
    if (axis === 'x') {
      // ridge along x; slopes run in z
      const zc = (z1 + z2) / 2, half = Math.floor((z2 - z1) / 2);
      for (let k = -overhang; k <= half + 0; k++) {
        const za = z1 + k + 0, zb = z2 - k;
        const yy = y + Math.max(0, k + overhang);
        const st = typeof stairs === 'function' ? stairs(k) : stairs;
        for (let x = x1 - overhang; x <= x2 + overhang; x++) {
          if (za < zb) { this.set(x, yy, za, st + '@S'); this.set(x, yy, zb, st + '@N'); }
          else if (za === zb) this.set(x, yy, za, ridge || fill);
        }
        if (za >= zb) break;
      }
    } else {
      const half = Math.floor((x2 - x1) / 2);
      for (let k = -overhang; k <= half; k++) {
        const xa = x1 + k, xb = x2 - k;
        const yy = y + Math.max(0, k + overhang);
        const st = typeof stairs === 'function' ? stairs(k) : stairs;
        for (let z = z1 - overhang; z <= z2 + overhang; z++) {
          if (xa < xb) { this.set(xa, yy, z, st + '@E'); this.set(xb, yy, z, st + '@W'); }
          else if (xa === xb) this.set(xa, yy, z, ridge || fill);
        }
        if (xa >= xb) break;
      }
    }
  }
  // Hip roof: shrinking rectangles with stairs on the border (corners take the N/S stair)
  hip(x1, z1, x2, z2, y, stairs, fill, overhang = 1, cap = null) {
    let a = x1 - overhang, b = x2 + overhang, c = z1 - overhang, d = z2 + overhang, yy = y;
    while (a <= b && c <= d) {
      for (let x = a; x <= b; x++) for (let z = c; z <= d; z++) {
        if (a === b || c === d) { this.set(x, yy, z, cap || fill); continue; }
        if (z === c) this.set(x, yy, z, stairs + '@S');
        else if (z === d) this.set(x, yy, z, stairs + '@N');
        else if (x === a) this.set(x, yy, z, stairs + '@E');
        else if (x === b) this.set(x, yy, z, stairs + '@W');
        else this.set(x, yy, z, fill);
      }
      a++; b--; c++; d--; yy++;
    }
  }
}

/* ---------- mesher ---------- */
const FACE = [
  // +y, -y, +z, -z, +x, -x
  { n: [0, 1, 0], c: [[0, 1, 0], [0, 1, 1], [1, 1, 1], [1, 1, 0]], key: 'top', shade: 1.0 },
  { n: [0, -1, 0], c: [[0, 0, 1], [0, 0, 0], [1, 0, 0], [1, 0, 1]], key: 'bottom', shade: 0.52 },
  { n: [0, 0, 1], c: [[0, 0, 1], [1, 0, 1], [1, 1, 1], [0, 1, 1]], key: 'side', shade: 0.84 },
  { n: [0, 0, -1], c: [[1, 0, 0], [0, 0, 0], [0, 1, 0], [1, 1, 0]], key: 'side', shade: 0.84 },
  { n: [1, 0, 0], c: [[1, 0, 1], [1, 0, 0], [1, 1, 0], [1, 1, 1]], key: 'side', shade: 0.68 },
  { n: [-1, 0, 0], c: [[0, 0, 0], [0, 0, 1], [0, 1, 1], [0, 1, 0]], key: 'side', shade: 0.68 },
];
// for each face+corner: the two tangent offsets and corner offset used for smooth light / AO
for (const f of FACE) {
  const ax = f.n.findIndex(v => v !== 0);
  const tang = [0, 1, 2].filter(a => a !== ax);
  f.ao = f.c.map(c => {
    const s = [0, 0, 0]; for (const a of tang) s[a] = c[a] === 1 ? 1 : -1;
    const o1 = [0, 0, 0], o2 = [0, 0, 0]; o1[tang[0]] = s[tang[0]]; o2[tang[1]] = s[tang[1]];
    return { s1: o1, s2: o2, co: [o1[0] + o2[0], o1[1] + o2[1], o1[2] + o2[2]] };
  });
}
const AO_TABLE = [0.48, 0.66, 0.83, 1.0];

class Buf {
  constructor() { this.pos = new Float32Array(3 * 4096); this.uv = new Float32Array(2 * 4096); this.aux = new Uint8Array(4 * 4096); this.idx = new Uint32Array(6 * 2048); this.nv = 0; this.ni = 0; }
  grow() { const n = this.pos.length * 2; const p = new Float32Array(n); p.set(this.pos); this.pos = p; const u = new Float32Array(n / 3 * 2); u.set(this.uv); this.uv = u; const a = new Uint8Array(n / 3 * 4); a.set(this.aux); this.aux = a; }
  growIdx() { const i = new Uint32Array(this.idx.length * 2); i.set(this.idx); this.idx = i; }
  vert(x, y, z, u, v, sh, sk, bl, em) {
    if ((this.nv + 1) * 3 > this.pos.length) this.grow();
    const n = this.nv++, p = n * 3;
    this.pos[p] = x; this.pos[p + 1] = y; this.pos[p + 2] = z;
    this.uv[n * 2] = u; this.uv[n * 2 + 1] = v;
    const a = n * 4; this.aux[a] = sh; this.aux[a + 1] = sk; this.aux[a + 2] = bl; this.aux[a + 3] = em;
  }
  quad(a, b, c, d, flip) {
    if (this.ni + 6 > this.idx.length) this.growIdx();
    const i = this.idx;
    if (!flip) { i[this.ni++] = a; i[this.ni++] = b; i[this.ni++] = c; i[this.ni++] = a; i[this.ni++] = c; i[this.ni++] = d; }
    else { i[this.ni++] = b; i[this.ni++] = c; i[this.ni++] = d; i[this.ni++] = b; i[this.ni++] = d; i[this.ni++] = a; }
  }
  geometry() {
    if (this.nv === 0) return null;
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.BufferAttribute(this.pos.slice(0, this.nv * 3), 3));
    g.setAttribute('uv', new THREE.BufferAttribute(this.uv.slice(0, this.nv * 2), 2));
    g.setAttribute('aux', new THREE.BufferAttribute(this.aux.slice(0, this.nv * 4), 4, true));
    g.setIndex(new THREE.BufferAttribute(this.idx.slice(0, this.ni), 1));
    g.computeBoundingSphere();
    return g;
  }
}

// boxes (in 16ths) that make up a block's shape; neighbours are used for fences/panes/walls
function shapeBoxes(w, x, y, z, def, meta) {
  const f = meta & 3, top = !!(meta & 4), hang = !!(meta & 8);
  switch (def.shape) {
    case 'slab': return [top ? [0, 8, 0, 16, 16, 16] : [0, 0, 0, 16, 8, 16]];
    case 'stairs': {
      const lo = top ? [0, 8, 0, 16, 16, 16] : [0, 0, 0, 16, 8, 16];
      const yy = top ? [0, 8] : [8, 16];
      const up = [[0, 0, 16, 8], [8, 0, 16, 16], [0, 8, 16, 16], [0, 0, 8, 16]][f]; // x0,z0,x1,z1 of the tall half
      return [lo, [up[0], yy[0], up[1], up[2], yy[1], up[3]]];
    }
    case 'carpet': return [[0, 0, 0, 16, 1, 16]];
    case 'torch': return [[7, 0, 7, 9, 10, 9]];
    case 'lantern': return hang ? [[5, 1, 5, 11, 8, 11], [6, 8, 6, 10, 10, 10]] : [[5, 0, 5, 11, 7, 11], [6, 7, 6, 10, 9, 10]];
    case 'chain': return [[7, 0, 7, 9, 16, 9]];
    case 'skull': return [[4, 0, 4, 12, 8, 12]];
    case 'lectern': return [[0, 0, 0, 16, 2, 16], [4, 2, 4, 12, 13, 12], [0, 13, 0, 16, 15, 16]];
    case 'anvil': return [[2, 0, 2, 14, 4, 14], [4, 4, 3, 12, 5, 13], [6, 5, 4, 10, 10, 12], [3, 10, 0, 13, 16, 16]];
    case 'bell': return [[5, 4, 5, 11, 12, 11], [7, 12, 7, 9, 16, 9]];
    case 'enchant': return [[0, 0, 0, 16, 12, 16]];
    case 'grind': return [[2, 2, 4, 14, 14, 12]];
    case 'cauldron': return [[1, 0, 1, 15, 14, 15]];
    case 'campfire': return [[0, 0, 2, 16, 4, 6], [0, 0, 10, 16, 4, 14], [2, 4, 0, 6, 8, 16]];
    case 'fence': case 'wall': {
      const isWall = def.shape === 'wall';
      const post = isWall ? [4, 0, 4, 12, 16, 12] : [6, 0, 6, 10, 16, 10];
      const boxes = [post];
      const ns = [[0, -1], [1, 0], [0, 1], [-1, 0]];
      for (let d = 0; d < 4; d++) {
        const nid = w.get(x + ns[d][0], y, z + ns[d][1]);
        const nd = BLOCKS[nid];
        const conn = nid !== 0 && (nd.shape === def.shape || nd.shape === 'fence' || nd.shape === 'wall' || (nd.opaque && nd.shape === 'cube'));
        if (!conn) continue;
        const a = isWall ? 5 : 7, b = isWall ? 11 : 9;
        const rails = isWall ? [[0, 14]] : [[6, 9], [12, 15]];
        for (const [y0, y1] of rails) {
          if (d === 0) boxes.push([a, y0, 0, b, y1, 6 - (isWall ? 2 : 0)]);
          else if (d === 2) boxes.push([a, y0, 10 + (isWall ? 2 : 0), b, y1, 16]);
          else if (d === 1) boxes.push([10 + (isWall ? 2 : 0), y0, a, 16, y1, b]);
          else boxes.push([0, y0, a, 6 - (isWall ? 2 : 0), y1, b]);
        }
      }
      return boxes;
    }
    case 'pane': {
      const boxes = [[7, 0, 7, 9, 16, 9]];
      const ns = [[0, -1], [1, 0], [0, 1], [-1, 0]];
      for (let d = 0; d < 4; d++) {
        const nid = w.get(x + ns[d][0], y, z + ns[d][1]);
        const nd = BLOCKS[nid];
        const conn = nid !== 0 && (nd.shape === 'pane' || nd.shape === 'wall' || nd.shape === 'fence' || (nd.opaque && nd.shape === 'cube'));
        if (!conn) continue;
        if (d === 0) boxes.push([7, 0, 0, 9, 16, 7]); else if (d === 2) boxes.push([7, 0, 9, 9, 16, 16]);
        else if (d === 1) boxes.push([9, 0, 7, 16, 16, 9]); else boxes.push([0, 0, 7, 7, 16, 9]);
      }
      return boxes;
    }
    case 'vine': return [[[0, 0, 0, 16, 16, 1], [15, 0, 0, 16, 16, 16], [0, 0, 15, 16, 16, 16], [0, 0, 0, 1, 16, 16]][f]];
    default: return [[0, 0, 0, 16, 16, 16]];
  }
}
export function collisionBoxes(w, x, y, z) {
  const id = w.get(x, y, z); if (id === 0) return null;
  const def = BLOCKS[id]; if (!def.collide) return null;
  switch (def.shape) {
    case 'cross': case 'torch': case 'lantern': case 'chain': case 'vine': case 'skull': case 'liquid': return null;
    case 'air': return [[0, 0, 0, 16, 16, 16]];
    case 'cube': return [[0, 0, 0, 16, 16, 16]];
    case 'fence': case 'wall': return shapeBoxes(w, x, y, z, def, 0).map(b => [b[0], 0, b[2], b[3], 24, b[5]]);
    default: return shapeBoxes(w, x, y, z, def, w.getMeta(x, y, z));
  }
}

export function buildMeshes(w, atlasRows, materials) {
  const group = new THREE.Group();
  const CH = 16;
  const uvCache = new Map();
  const getUV = (name) => { let r = uvCache.get(name); if (!r) { r = tileUV(name, atlasRows); uvCache.set(name, r); } return r; };
  const { ids, meta, sky, blk } = w;
  const stats = { faces: 0, chunks: 0 };

  for (let cz = WZ0; cz <= WZ1; cz += CH) for (let cx = WX0; cx <= WX1; cx += CH) {
    const solidBuf = new Buf(), glassBuf = new Buf();
    for (let y = WY0; y <= WY1; y++) for (let z = cz; z < cz + CH; z++) for (let x = cx; x < cx + CH; x++) {
      const i = (y - WY0) * SLICE + (z - WZ0) * SX + (x - WX0);
      const id = ids[i];
      if (id === 0) continue;
      const def = BLOCKS[id];
      if (def.shape === 'air') continue;
      const buf = (def.transparent || def.shape === 'liquid') ? glassBuf : solidBuf;
      const m = meta[i];

      // ----- light helpers
      const lightSample = (xx, yy, zz) => {
        if (xx < WX0 || xx > WX1 || zz < WZ0 || zz > WZ1 || yy < WY0) return null;
        if (yy > WY1) return [15, 0, 0];
        const j = (yy - WY0) * SLICE + (zz - WZ0) * SX + (xx - WX0);
        return [sky[j], blk[j], OPQ[ids[j]]];
      };

      if (def.shape === 'cross') {
        const uv = getUV(def.tex.side);
        const ls = lightSample(x, y, z) || [15, 0, 0];
        const s = Math.round(ls[0] / 15 * 255), b = Math.round(ls[1] / 15 * 255), e = Math.round(def.emit * 255);
        const sh = 235;
        // two crossed double-sided quads
        for (const [ax, az, bx, bz] of [[0.05, 0.05, 0.95, 0.95], [0.95, 0.05, 0.05, 0.95]]) {
          for (let side = 0; side < 2; side++) {
            const base = buf.nv;
            const [p0, p1] = side ? [[bx, bz], [ax, az]] : [[ax, az], [bx, bz]];
            buf.vert(x + p0[0], y, z + p0[1], uv[0], uv[1], sh, s, b, e);
            buf.vert(x + p1[0], y, z + p1[1], uv[2], uv[1], sh, s, b, e);
            buf.vert(x + p1[0], y + 1, z + p1[1], uv[2], uv[3], sh, s, b, e);
            buf.vert(x + p0[0], y + 1, z + p0[1], uv[0], uv[3], sh, s, b, e);
            buf.quad(base, base + 1, base + 2, base + 3, false);
          }
        }
        stats.faces += 4;
        continue;
      }

      const isCube = def.shape === 'cube';
      let boxes;
      if (isCube) boxes = [[0, 0, 0, 16, 16, 16]];
      else if (def.shape === 'liquid') {
        const above = ids[i + SLICE];
        const full = y < WY1 && BLOCKS[above] && BLOCKS[above].shape === 'liquid';
        boxes = [[0, 0, 0, 16, full ? 16 : 14, 16]];
      } else boxes = shapeBoxes(w, x, y, z, def, m);

      for (const bx of boxes) {
        const x0 = bx[0] / 16, y0 = bx[1] / 16, z0 = bx[2] / 16, x1 = bx[3] / 16, y1 = bx[4] / 16, z1 = bx[5] / 16;
        for (let fi = 0; fi < 6; fi++) {
          const F = FACE[fi], n = F.n;
          // does the box face lie on the cell boundary?
          const onEdge = (fi === 0 && bx[4] === 16) || (fi === 1 && bx[1] === 0) || (fi === 2 && bx[5] === 16) || (fi === 3 && bx[2] === 0) || (fi === 4 && bx[3] === 16) || (fi === 5 && bx[0] === 0);
          const nx = x + n[0], ny = y + n[1], nz = z + n[2];
          let nid = 0;
          if (nx >= WX0 && nx <= WX1 && ny >= WY0 && ny <= WY1 && nz >= WZ0 && nz <= WZ1) nid = ids[(ny - WY0) * SLICE + (nz - WZ0) * SX + (nx - WX0)];
          else if (ny < WY0) continue;
          if (onEdge) {
            if (OPQ[nid]) continue;
            if (nid === id && def.cutout && isCube) continue;
            if (nid === id && (def.transparent || def.shape === 'liquid') && isCube) continue;
            if (def.shape === 'liquid') { const nd = BLOCKS[nid]; if (nd && nd.shape === 'liquid' && nd.lq === def.lq) continue; }
          }
          const texName = def.tex[F.key];
          const uv = getUV(texName);
          // light: sample from the neighbour layer when on a boundary, else own cell
          const bx0 = onEdge ? nx : x, by0 = onEdge ? ny : y, bz0 = onEdge ? nz : z;
          const lc = lightSample(bx0, by0, bz0) || [15, 0, 0];
          const em = Math.round(def.emit * 255);
          const shadeBase = F.shade;
          const vs = [], aos = [];
          for (let ci = 0; ci < 4; ci++) {
            const c = F.c[ci];
            let sk = lc[0], bl = lc[1], cnt = 1;
            let ao = 3;
            if (onEdge && (isCube || def.shape === 'slab' || def.shape === 'stairs')) {
              const A = F.ao[ci];
              const l1 = lightSample(bx0 + A.s1[0], by0 + A.s1[1], bz0 + A.s1[2]);
              const l2 = lightSample(bx0 + A.s2[0], by0 + A.s2[1], bz0 + A.s2[2]);
              const l3 = lightSample(bx0 + A.co[0], by0 + A.co[1], bz0 + A.co[2]);
              const o1 = l1 ? l1[2] : 0, o2 = l2 ? l2[2] : 0, o3 = l3 ? l3[2] : 0;
              if (!o1 && l1) { sk += l1[0]; bl += l1[1]; cnt++; }
              if (!o2 && l2) { sk += l2[0]; bl += l2[1]; cnt++; }
              if (!(o1 && o2) && !o3 && l3) { sk += l3[0]; bl += l3[1]; cnt++; }
              ao = (o1 && o2) ? 0 : 3 - (o1 + o2 + o3);
            }
            sk /= cnt; bl /= cnt;
            // vertex position and uv
            const px = x + x0 + c[0] * (x1 - x0), py = y + y0 + c[1] * (y1 - y0), pz = z + z0 + c[2] * (z1 - z0);
            const fx = x0 + c[0] * (x1 - x0), fy = y0 + c[1] * (y1 - y0), fz = z0 + c[2] * (z1 - z0);
            let fu, fv;
            switch (fi) {
              case 0: fu = fx; fv = 1 - fz; break;
              case 1: fu = fx; fv = fz; break;
              case 2: fu = fx; fv = fy; break;
              case 3: fu = 1 - fx; fv = fy; break;
              case 4: fu = 1 - fz; fv = fy; break;
              default: fu = fz; fv = fy; break;
            }
            vs.push([px, py, pz, uv[0] + fu * (uv[2] - uv[0]), uv[1] + fv * (uv[3] - uv[1]), Math.round(shadeBase * AO_TABLE[ao] * 255), Math.round(sk / 15 * 255), Math.round(bl / 15 * 255), em]);
            aos.push(ao);
          }
          const base = buf.nv;
          for (const v of vs) buf.vert(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
          buf.quad(base, base + 1, base + 2, base + 3, aos[0] + aos[2] < aos[1] + aos[3]);
          stats.faces++;
        }
      }
    }
    for (const [buf, mat, name] of [[solidBuf, materials.solid, 'solid'], [glassBuf, materials.glass, 'glass']]) {
      const g = buf.geometry();
      if (!g) continue;
      const mesh = new THREE.Mesh(g, mat);
      mesh.matrixAutoUpdate = false;
      mesh.userData.pass = name;
      if (name === 'glass') mesh.renderOrder = 2;
      group.add(mesh); stats.chunks++;
    }
  }
  group.userData.stats = stats;
  return group;
}

/* ---------- block raycast (Amanatides & Woo) ---------- */
export function raycastBlocks(w, ox, oy, oz, dx, dy, dz, maxDist) {
  let x = Math.floor(ox), y = Math.floor(oy), z = Math.floor(oz);
  const sx = Math.sign(dx), sy = Math.sign(dy), sz = Math.sign(dz);
  const tdx = dx !== 0 ? Math.abs(1 / dx) : Infinity, tdy = dy !== 0 ? Math.abs(1 / dy) : Infinity, tdz = dz !== 0 ? Math.abs(1 / dz) : Infinity;
  let tx = dx !== 0 ? ((dx > 0 ? x + 1 - ox : ox - x) * tdx) : Infinity;
  let ty = dy !== 0 ? ((dy > 0 ? y + 1 - oy : oy - y) * tdy) : Infinity;
  let tz = dz !== 0 ? ((dz > 0 ? z + 1 - oz : oz - z) * tdz) : Infinity;
  let t = 0, face = [0, 0, 0];
  for (let i = 0; i < 400 && t <= maxDist; i++) {
    const id = w.get(x, y, z);
    if (id !== 0) {
      const d = BLOCKS[id];
      if (d.shape !== 'liquid' && d.shape !== 'air') return { x, y, z, id, t, face };
    }
    if (tx < ty && tx < tz) { x += sx; t = tx; tx += tdx; face = [-sx, 0, 0]; }
    else if (ty < tz) { y += sy; t = ty; ty += tdy; face = [0, -sy, 0]; }
    else { z += sz; t = tz; tz += tdz; face = [0, 0, -sz]; }
  }
  return null;
}
