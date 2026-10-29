// Voxel-box figures (Minecraft-style humanoids, beasts, siege engines) built from vertex-coloured boxes. 1 unit = 1/16 block.
import * as THREE from '../vendor/three.module.min.js';

const SHADE = { top: 1.0, bottom: 0.5, front: 0.88, back: 0.72, right: 0.66, left: 0.8 };
const hexRGB = (h) => [((h >> 16) & 255) / 255, ((h >> 8) & 255) / 255, (h & 255) / 255];
let seed = 7;
const jit = () => { seed = (seed * 16807) % 2147483647; return (seed / 2147483647 - 0.5) * 0.07; };

class Batch {
  constructor() { this.pos = []; this.col = []; this.idx = []; }
  // centre-based box in px
  box(cx, cy, cz, w, h, d, color, o = {}) {
    const base = hexRGB(color), k = (o.k ?? 1) * (1 + jit());
    const hw = w / 2, hh = h / 2, hd = d / 2;
    const faces = [
      [[-hw, -hh, hd], [hw, -hh, hd], [hw, hh, hd], [-hw, hh, hd], SHADE.front],
      [[hw, -hh, -hd], [-hw, -hh, -hd], [-hw, hh, -hd], [hw, hh, -hd], SHADE.back],
      [[hw, -hh, hd], [hw, -hh, -hd], [hw, hh, -hd], [hw, hh, hd], SHADE.right],
      [[-hw, -hh, -hd], [-hw, -hh, hd], [-hw, hh, hd], [-hw, hh, -hd], SHADE.left],
      [[-hw, hh, hd], [hw, hh, hd], [hw, hh, -hd], [-hw, hh, -hd], SHADE.top],
      [[-hw, -hh, -hd], [hw, -hh, -hd], [hw, -hh, hd], [-hw, -hh, hd], SHADE.bottom],
    ];
    const rot = o.rx || o.ry || o.rz ? new THREE.Euler(o.rx || 0, o.ry || 0, o.rz || 0) : null;
    const v = new THREE.Vector3();
    for (const f of faces) {
      const b = this.pos.length / 3;
      for (let i = 0; i < 4; i++) {
        v.set(f[i][0], f[i][1], f[i][2]); if (rot) v.applyEuler(rot);
        this.pos.push(v.x + cx, v.y + cy, v.z + cz);
        const s = f[4] * k * (o.glow ? 1.6 : 1);
        this.col.push(Math.min(1.6, base[0] * s), Math.min(1.6, base[1] * s), Math.min(1.6, base[2] * s));
      }
      this.idx.push(b, b + 1, b + 2, b, b + 2, b + 3);
    }
    return this;
  }
  geometry() {
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.Float32BufferAttribute(this.pos, 3));
    g.setAttribute('color', new THREE.Float32BufferAttribute(this.col, 3));
    g.setIndex(this.idx); return g;
  }
}

export class Figure {
  constructor(scale = 1) {
    this.root = new THREE.Group(); this.inner = new THREE.Group(); this.root.add(this.inner);
    this.inner.scale.setScalar(scale / 16); this.scale = scale;
    this.parts = {}; this.mat = new THREE.MeshBasicMaterial({ vertexColors: true });
    this.mats = [this.mat];
    this.height = 32 * scale / 16; this.pose = 'stand'; this.t0 = Math.random() * 10; this.glowParts = [];
  }
  // add a part pivoting at (px,py,pz); fn(batch) draws boxes relative to the pivot
  part(name, px, py, pz, fn, parent = null) {
    const g = new THREE.Group(); g.position.set(px, py, pz);
    const b = new Batch(); fn(b);
    const m = new THREE.Mesh(b.geometry(), this.mat); m.matrixAutoUpdate = true; g.add(m);
    (parent ? this.parts[parent] : this.inner).add(g);
    this.parts[name] = g; g.userData.base = { x: px, y: py, z: pz };
    return g;
  }
  setLight(color) { for (const m of this.mats) m.color.copy(color); }
}

/* ---------------------------------------------------------------- humanoids */
const C = {
  steel: 0x9aa0a8, steelD: 0x6a7078, steelL: 0xc4cad2, gold: 0xe6bd48, goldD: 0xb08a28, white: 0xeeeae0, navy: 0x1d2a5c, navyD: 0x141c40, brown: 0x5a3e26, brownD: 0x3a2818, leather: 0x6e4a2a,
  bone: 0xd8d4c2, boneD: 0xa8a290, moss: 0x4a6a3a, mossD: 0x30482a, soul: 0x6fe8ff, black: 0x17151a, soot: 0x2a262c, brass: 0xc0922c, ember: 0xff8a2a, russet: 0x8e3c2e, ochre: 0xd89a30,
  skin: 0xd8a878, skinD: 0xb88858, teal: 0x3fa58c, tealD: 0x2a7a68, deep: 0x14506a, zomb: 0x5f8a4a, pig: 0xc98a8a, red: 0xa12722, purple: 0x6a3a9a, cream: 0xe2d3a4, grey: 0x707078,
};
export { C };

function head(b, type, o) {
  const sk = o.skin ?? C.skin;
  switch (type) {
    case 'skull': {
      b.box(0, 4, 0, 8, 8, 8, o.skin ?? C.bone); b.box(-2, 4.5, 4.05, 2.4, 2.4, 0.6, C.black); b.box(2, 4.5, 4.05, 2.4, 2.4, 0.6, C.black);
      if (o.eye) { b.box(-2, 4.5, 4.2, 1.2, 1.2, 0.4, o.eye, { glow: true }); b.box(2, 4.5, 4.2, 1.2, 1.2, 0.4, o.eye, { glow: true }); }
      b.box(0, 2.3, 4.05, 1.2, 1.4, 0.6, C.boneD); b.box(0, 0.9, 4.05, 5, 1, 0.6, C.boneD); break;
    }
    case 'zombie': b.box(0, 4, 0, 8, 8, 8, o.skin ?? C.zomb); b.box(-2, 4.5, 4.05, 2, 1.6, 0.5, 0x1a2a1a); b.box(2, 4.5, 4.05, 2, 1.6, 0.5, 0x1a2a1a); b.box(0, 2, 4.05, 4, 1, 0.5, 0x2a3a2a); break;
    case 'piglin': b.box(0, 4, 0, 8, 8, 8, o.skin ?? C.pig); b.box(0, 2.4, 4.8, 4.4, 3.4, 1.6, 0xb87070); b.box(-1.1, 2.4, 5.7, 0.9, 0.9, 0.4, 0x4a2020); b.box(1.1, 2.4, 5.7, 0.9, 0.9, 0.4, 0x4a2020); b.box(-2.2, 4.8, 4.1, 1.4, 1.4, 0.4, C.black); b.box(2.2, 4.8, 4.1, 1.4, 1.4, 0.4, C.black); b.box(-1.9, 1.1, 4.6, 1, 2, 1, C.bone); b.box(1.9, 1.1, 4.6, 1, 2, 1, C.bone); b.box(-5, 6, 0, 1.2, 4, 3.4, 0xb87070); b.box(5, 6, 0, 1.2, 4, 3.4, 0xb87070); break;
    case 'drowned': b.box(0, 4, 0, 8, 8, 8, o.skin ?? 0x4a9a8a); b.box(-2, 4.5, 4.05, 2, 1.6, 0.5, 0xd8f0e8); b.box(2, 4.5, 4.05, 2, 1.6, 0.5, 0xd8f0e8); b.box(-2, 4.5, 4.2, 0.9, 0.9, 0.4, 0x203a3a); b.box(2, 4.5, 4.2, 0.9, 0.9, 0.4, 0x203a3a); b.box(0, 1.8, 4.05, 4, 1, 0.5, 0x2a6a5a); b.box(0, 8.3, 0, 8.6, 1.2, 8.6, 0x2a6a58); break;
    case 'villager': b.box(0, 4, 0, 8, 8, 8, sk); b.box(0, 3, 5, 2.4, 4.6, 2.4, C.skinD); b.box(-2, 5.6, 4.05, 2, 1.8, 0.5, 0xeeeeee); b.box(2, 5.6, 4.05, 2, 1.8, 0.5, 0xeeeeee); b.box(-2, 5.6, 4.2, 1, 1, 0.4, 0x2a6a3a); b.box(2, 5.6, 4.2, 1, 1, 0.4, 0x2a6a3a); b.box(0, 7, 4.05, 8, 1, 0.5, 0x4a3a28); break;
    case 'golem': b.box(0, 4, 0, 8, 10, 8, o.skin ?? C.steel); b.box(0, 2.5, 5, 2.6, 5, 2, C.steelD); b.box(-3, 6, 4.05, 2, 1.4, 0.5, o.eye ?? C.ember, { glow: true }); b.box(3, 6, 4.05, 2, 1.4, 0.5, o.eye ?? C.ember, { glow: true }); b.box(0, 8.6, 0, 9, 1.4, 9, C.steelD); break;
    case 'hood': b.box(0, 4, 0, 8.8, 8.8, 8.8, o.cloth ?? C.black); b.box(0, 4, 4.1, 6.4, 6.4, 0.8, 0x050505); if (o.eye) { b.box(-1.8, 4.5, 4.6, 1.3, 1, 0.4, o.eye, { glow: true }); b.box(1.8, 4.5, 4.6, 1.3, 1, 0.4, o.eye, { glow: true }); } b.box(0, 9, -0.5, 7, 1.2, 8, o.cloth ?? C.black); break;
    default: { // human
      b.box(0, 4, 0, 8, 8, 8, sk); b.box(-2, 4.5, 4.05, 2, 1.6, 0.5, 0xeeeeee); b.box(2, 4.5, 4.05, 2, 1.6, 0.5, 0xeeeeee); b.box(-2, 4.5, 4.2, 1, 1, 0.4, o.eyeCol ?? 0x2a3a6a); b.box(2, 4.5, 4.2, 1, 1, 0.4, o.eyeCol ?? 0x2a3a6a); b.box(0, 1.6, 4.05, 3, 0.8, 0.5, 0x8a5a4a);
      if (o.scar) { b.box(2.2, 6, 4.2, 0.8, 2.6, 0.4, 0xb86a5a); }
      if (o.paint) { for (const x of [-3, 3]) b.box(x, 3.4, 4.2, 1, 3, 0.4, o.paint); b.box(0, 5.6, 4.2, 8, 0.9, 0.4, o.paint); }
      if (o.hair !== undefined) { b.box(0, 8.3, -0.3, 8.6, 2.4, 8.6, o.hair); b.box(0, 5, -4.2, 8.6, 7, 1, o.hair); b.box(-4.3, 6.8, 0, 0.8, 3, 8, o.hair); b.box(4.3, 6.8, 0, 0.8, 3, 8, o.hair); }
      if (o.beard !== undefined) { b.box(0, 0.8, 4, 7.2, 4.4, 1.8, o.beard); b.box(0, -1.4, 4.2, 5, 3, 1.6, o.beard); }
    }
  }
}

// Build one humanoid. o: {head, skin, eye, torso, trim, legs, boots, arms, sleeve, scale, bulk, robe, cape, tabard, accessories: [fn(fig)]}
export function humanoid(o = {}) {
  const f = new Figure(o.scale ?? 1);
  const bulk = o.bulk ?? 1, W = 8 * bulk;
  const torso = o.torso ?? C.steel, legs = o.legs ?? C.steelD, boots = o.boots ?? C.brownD, sleeve = o.sleeve ?? torso;
  const skeletal = o.skeletal;
  f.part('legR', -2 * bulk, 12, 0, (b) => {
    if (skeletal) { b.box(0, -6, 0, 2.2, 12, 2.2, o.skin ?? C.bone); b.box(0, -11.4, 0.6, 3, 1.4, 4, o.skin ?? C.bone); }
    else { b.box(0, -6, 0, 4 * bulk, 12, 4, legs); b.box(0, -10.5, 0.5, 4.2 * bulk, 3, 4.6, boots); if (o.greaves) b.box(0, -7, 0.1, 4.4 * bulk, 5, 4.4, o.greaves); }
  });
  f.part('legL', 2 * bulk, 12, 0, (b) => {
    if (skeletal) { b.box(0, -6, 0, 2.2, 12, 2.2, o.skin ?? C.bone); b.box(0, -11.4, 0.6, 3, 1.4, 4, o.skin ?? C.bone); }
    else { b.box(0, -6, 0, 4 * bulk, 12, 4, legs); b.box(0, -10.5, 0.5, 4.2 * bulk, 3, 4.6, boots); if (o.greaves) b.box(0, -7, 0.1, 4.4 * bulk, 5, 4.4, o.greaves); }
  });
  f.part('body', 0, 12, 0, (b) => {
    if (skeletal) { b.box(0, 6, 0, 2, 12, 2, o.skin ?? C.bone); for (let i = 0; i < 4; i++) b.box(0, 9 - i * 2.4, 0, 7, 1.2, 3.2, o.skin ?? C.bone); b.box(0, 0.5, 0, 6, 1.5, 3, o.skin ?? C.bone); if (o.tabard) b.box(0, 5, 2.4, 6, 11, 0.8, o.tabard); }
    else {
      b.box(0, 6, 0, W, 12, 4, torso);
      if (o.belt !== false) b.box(0, 1.3, 0, W + 0.6, 1.6, 4.6, o.beltCol ?? C.brownD);
      if (o.chest) b.box(0, 8, 2.1, W - 1.4, 3.2, 0.8, o.chest);
      if (o.tabard) { b.box(0, 5, 2.15, W - 2, 11, 0.9, o.tabard); if (o.emblem) { b.box(0, 7.5, 2.7, 3, 3, 0.5, o.emblem); b.box(0, 7.5, 2.9, 1.2, 1.2, 0.4, o.tabard); } b.box(0, 5, -2.15, W - 2, 11, 0.9, o.tabard); }
      if (o.skirt) { b.box(0, -3, 0, W + 1.6, 10, 5.6, o.skirt); if (o.skirtTrim) b.box(0, -8, 0, W + 2, 1.2, 6, o.skirtTrim); }
    }
  });
  f.part('head', 0, 24, 0, (b) => head(b, o.head ?? 'human', o));
  const armPart = (name, sgn) => f.part(name, sgn * (W / 2 + 2), 22, 0, (b) => {
    if (skeletal) { b.box(0, -6, 0, 1.8, 12, 1.8, o.skin ?? C.bone); b.box(0, -11.5, 0, 2.4, 1.6, 2.4, o.skin ?? C.bone); }
    else {
      b.box(0, -4, 0, 4, 8, 4, sleeve); b.box(0, -10, 0, 3.6, 4, 3.6, o.hands ?? (o.head === 'zombie' ? C.zomb : (o.arms ?? o.skin ?? C.skin)));
      if (o.vambrace) b.box(0, -7, 0, 4.4, 4, 4.4, o.vambrace);
      if (o.pauldrons) { b.box(sgn * 0.6, 1.8, 0, 5.8, 3, 5.8, o.pauldrons); if (o.pauldronTrim) b.box(sgn * 0.6, 3.5, 0, 6, 0.8, 6, o.pauldronTrim); }
    }
  });
  armPart('armR', -1); armPart('armL', 1);
  if (o.cape) f.part('cape', 0, 22, -2.6, (b) => { b.box(0, -10, -0.4, W - 0.6, 20, 1, o.cape); if (o.capeTrim) b.box(0, -19.6, -0.4, W - 0.6, 1.2, 1.2, o.capeTrim); if (o.capeTop) b.box(0, 0.5, 0.4, W + 1, 2.4, 2, o.capeTop); });
  if (o.accessories) for (const a of o.accessories) a(f);
  f.pose = o.pose ?? 'stand'; f.zombieArms = o.zombieArms; f.armPose = o.armPose || {};
  return f;
}

/* ---- accessories: each takes the figure and adds boxes / parts ---- */
export const acc = {
  helm: (col = C.steel, o = {}) => (f) => f.parts.head.add(mesh(f, (b) => { b.box(0, 4.6, 0, 8.8, 5, 8.8, col); b.box(0, 8.8, 0, 8.8, 1.2, 8.8, o.crown ?? col); if (o.nasal !== false) b.box(0, 3.4, 4.5, 1.6, 4.5, 0.8, o.nasalCol ?? C.steelD); if (o.visor) b.box(0, 3.6, 4.5, 7.6, 1.2, 0.8, 0x101010); if (o.crest) { b.box(0, 11, 0, 1.4, 4, 9, o.crest); b.box(0, 13.4, 0, 1.4, 1.6, 4, o.crest); } if (o.plume) { b.box(0, 11, -2.5, 2, 3, 3, o.plume); b.box(0, 12.6, -5, 2, 4, 2.4, o.plume); b.box(0, 10.4, -7, 1.6, 3.4, 1.6, o.plume); } if (o.horns) { b.box(-5.2, 8, 0, 1.4, 4, 1.4, C.bone); b.box(5.2, 8, 0, 1.4, 4, 1.4, C.bone); b.box(-6.2, 10.4, 0, 1.4, 2.4, 1.4, C.bone); b.box(6.2, 10.4, 0, 1.4, 2.4, 1.4, C.bone); } })),
  hat: (col, o = {}) => (f) => f.parts.head.add(mesh(f, (b) => { b.box(0, 8.6, 0, 8.8, 1.6, 8.8, col); b.box(0, 9.8, 0, 11.5, 0.8, 11.5, col); if (o.tall) b.box(0, 11, 0, 7, 3, 7, col); if (o.feather) b.box(2, 12, -2, 1, 5, 1, o.feather); })),
  halo: (col = C.gold) => (f) => f.parts.head.add(mesh(f, (b) => { for (let i = 0; i < 12; i++) { const a = i / 12 * Math.PI * 2; b.box(Math.cos(a) * 6, 13, Math.sin(a) * 6, 2, 1, 2, col, { glow: true }); } })),
  crown: (col = C.gold, broken = false) => (f) => f.parts.head.add(mesh(f, (b) => { b.box(0, 9, 0, 9, 2, 9, col); for (const [x, z] of [[-4, -4], [4, -4], [-4, 4], [4, 4], [0, 4.2], [0, -4.2]]) b.box(x, 11.4, z, 1.8, 3, 1.8, col); if (broken) b.box(1.2, 9.5, 4.8, 0.8, 4, 0.8, 0x101010); b.box(0, 9.4, 4.6, 1.6, 1.6, 0.6, C.soul, { glow: true }); })),
  antlers: (col = C.bone, big = false) => (f) => f.parts.head.add(mesh(f, (b) => { const s = big ? 1.7 : 1; for (const sg of [-1, 1]) { b.box(sg * 3.4, 9.5, 0, 1.2, 4 * s, 1.2, col); b.box(sg * 4.4, 12.2 * s - 0.8, 0, 1.2, 3 * s, 1.2, col, { rz: sg * -0.4 }); b.box(sg * 5.6, 14.6 * s - 1.4, 0, 1.1, 2.4 * s, 1.1, col); b.box(sg * 3.4, 12 * s, -1.4, 1, 3 * s, 1, col); b.box(sg * 6.6, 12.6 * s, 0, 1, 2 * s, 1, col); } })),
  feathers: (cols = [C.red, C.ochre, C.white]) => (f) => f.parts.head.add(mesh(f, (b) => { b.box(0, 8.8, -3.4, 7, 1.4, 1.4, C.leather); cols.forEach((c, i) => b.box(-3 + i * 3, 11.6 + (i === 1 ? 1.2 : 0), -3.6, 1.2, 5, 0.6, c, { rz: (i - 1) * 0.25 })); })),
  kelp: (n = 5) => (f) => { f.parts.head.add(mesh(f, (b) => { for (let i = 0; i < n; i++) { b.box(-4 + i * 2, 9, -4 + (i % 2) * 8, 1.4, 5, 0.8, 0x2e7a3a); b.box(-4 + i * 2, 5.5, -4.8 + (i % 2) * 9.6, 1.4, 4, 0.8, 0x3a9a4a); } })); f.parts.body.add(mesh(f, (b) => { b.box(-3, 1, 2.6, 1.4, 6, 0.8, 0x2e7a3a); b.box(2, 0.5, -2.6, 1.4, 7, 0.8, 0x3a9a4a); b.box(4.2, 3, 0, 0.8, 6, 1.4, 0x2e7a3a); })); },
  coral: (cols = [0xe85a52, 0xe670c0, 0xd8c43a]) => (f) => f.parts.armL.add(mesh(f, (b) => { cols.forEach((c, i) => { b.box(0.6, 3 + i * 1.4, i * 1.3 - 1.4, 2.4, 2.4, 2.4, c); b.box(-0.4 + i, 4.6 + i, i - 1, 1.4, 2, 1.4, c); }); })),
  quiver: (col = C.leather) => (f) => f.parts.body.add(mesh(f, (b) => { b.box(-2.5, 9, -3.4, 3.2, 10, 2.4, col, { rz: 0.18 }); for (let i = 0; i < 3; i++) b.box(-3.2 + i * 1.1, 14.4, -3.4, 0.6, 2.6, 0.6, C.white); })),
  backpack: (col = C.leather) => (f) => f.parts.body.add(mesh(f, (b) => { b.box(0, 6, -4.2, 6, 8, 3.4, col); b.box(0, 11, -4.2, 6.4, 1.6, 3.8, C.brownD); })),
  // held items --------------------------------------------------
  shield: (col, trim = C.gold, o = {}) => (f) => f.parts.armL.add(mesh(f, (b) => { const h = o.h ?? 14, w = o.w ?? 10; b.box(3.2, -5, 1.2, 1.6, h, w, col); b.box(3.9, -5, 1.2, 0.8, h - 1.6, w - 1.6, trim); b.box(4.3, -5, 1.2, 0.6, h - 4, w - 4, col); if (o.boss) b.box(4.7, -5, 1.2, 1.4, 3, 3, o.boss); if (o.kite) b.box(3.2, -5 - h / 2 - 1.4, 1.2, 1.6, 3.2, w - 3, col); })),
  sword: (blade = 0xcfd6dc, hilt = C.gold, len = 15) => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -11, 0.5, 5, 1.2, 1.6, hilt); b.box(0, -11.6, 0.5, 1.2, 2.6, 1.2, C.brownD); b.box(0, -11 + len / 2 + 0.8, 0.5, 1.4, len, 0.8, blade); b.box(0, -11 + len + 1.6, 0.5, 0.8, 1.6, 0.6, blade); })),
  greatsword: (blade = 0xc8d0d8, len = 26) => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -11, 0.5, 7, 1.6, 2, C.gold); b.box(0, -12.6, 0.5, 1.4, 4, 1.4, C.brownD); b.box(0, -11 + len / 2 + 1, 0.5, 2.6, len, 1, blade); b.box(0, -11 + len + 2.4, 0.5, 1.6, 2.4, 0.8, blade); b.box(0, -11 + len / 2 + 1, 0.9, 0.7, len - 2, 0.3, 0xe8eef2); })),
  spear: (len = 36, tip = 0xcfd6dc, shaft = C.leather) => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -10 + len / 2 - 8, 0.5, 1.4, len, 1.4, shaft); b.box(0, -10 + len - 6, 0.5, 2.4, 6, 1, tip); b.box(0, -10 + len - 9, 0.5, 4, 1, 1.2, tip); })),
  trident: (len = 38, col = 0x9ad8c8) => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -10 + len / 2 - 8, 0.5, 1.4, len, 1.4, 0x6a5a40); for (const x of [-2.4, 0, 2.4]) b.box(x, -10 + len - 2, 0.5, 1, x === 0 ? 8 : 6, 1, col); b.box(0, -10 + len - 6, 0.5, 6.4, 1.2, 1, col); })),
  axe: (col = C.steelL, len = 20) => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -10 + len / 2 - 4, 0.5, 1.6, len, 1.6, C.brownD); b.box(0, -10 + len - 2, 3, 1.2, 9, 7, col); b.box(0, -10 + len - 2, 6.6, 0.8, 11, 1.4, col); })),
  hammer: (col = C.soot, len = 22) => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -10 + len / 2 - 4, 0.5, 1.8, len, 1.8, C.brownD); b.box(0, -10 + len - 1, 0.5, 6, 6, 9, col); b.box(0, -10 + len - 1, 0.5, 6.4, 1.4, 9.4, C.brass); })),
  scythe: (len = 40) => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -10 + len / 2 - 8, 0.5, 1.4, len, 1.4, 0x3a2a20); for (let i = 0; i < 6; i++) b.box(0, -10 + len - 8 + (i > 3 ? -(i - 3) * 1.5 : 0), 1.5 + i * 2.4, 0.8, 1.8, 3, 0xd8d4d0); b.box(0, -10 + len - 7, 0.5, 1.2, 1.2, 3, 0xd8d4d0); })),
  bow: (len = 22, col = C.leather) => (f) => f.parts.armL.add(mesh(f, (b) => { const half = len / 2; b.box(0.5, -10 + 0, 4, 1.2, 3, 1.4, col); for (let i = 1; i <= 4; i++) { const t = i / 4, z = 4 + Math.sin(t * 1.4) * 0.5 - t * 3.6; b.box(0.5, -10 + t * half * 0.9, z, 1.2, half / 4 + 0.6, 1.4, col); b.box(0.5, -10 - t * half * 0.9, z, 1.2, half / 4 + 0.6, 1.4, col); } b.box(0.5, -10, 0.6, 0.4, len * 0.92, 0.4, 0xe8e4d8); })),
  crossbow: (col = C.brownD) => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -10, 4, 1.6, 2, 10, col); b.box(0, -10, 8.6, 11, 1.4, 1.4, 0x2a2a30); b.box(-4.6, -10, 7.6, 2, 1.2, 2.4, 0x2a2a30); b.box(4.6, -10, 7.6, 2, 1.2, 2.4, 0x2a2a30); b.box(0, -9.2, 6, 0.5, 0.5, 8, 0xd8d4c8); })),
  staff: (head = 'orb', col = C.soul, len = 40, shaft = 0x5a4028) => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -10 + len / 2 - 8, 0.5, 1.6, len, 1.6, shaft); const y = -10 + len - 6; if (head === 'orb') { b.box(0, y + 3, 0.5, 4, 4, 4, col, { glow: true }); b.box(0, y + 1, 0.5, 5.4, 1, 5.4, C.gold); } else if (head === 'lantern') { b.box(0, y + 1, 0.5, 5, 6, 5, C.black); b.box(0, y + 1, 0.5, 3.4, 4.6, 3.4, col, { glow: true }); b.box(0, y + 4.6, 0.5, 6, 1.2, 6, C.black); } else if (head === 'skull') { b.box(0, y + 3, 0.5, 5, 5, 5, C.bone); b.box(-1.2, y + 3.4, 3.1, 1.2, 1.2, 0.4, col, { glow: true }); b.box(1.2, y + 3.4, 3.1, 1.2, 1.2, 0.4, col, { glow: true }); } else if (head === 'sun') { for (let i = 0; i < 8; i++) { const a = i / 8 * Math.PI * 2; b.box(Math.cos(a) * 3.4, y + 3 + Math.sin(a) * 3.4, 0.5, 1.6, 1.6, 1.6, col, { glow: true }); } b.box(0, y + 3, 0.5, 3, 3, 3, C.white, { glow: true }); } else if (head === 'shell') { b.box(0, y + 3, 0.5, 5, 5, 3, 0xe8e0d0); b.box(0, y + 3, 0.5, 3, 3, 3.6, 0x7fffe0, { glow: true }); } else if (head === 'bone') { b.box(0, y + 2, 0.5, 2, 6, 2, C.bone); b.box(-2, y + 5, 0.5, 1.4, 3, 1.4, C.bone); b.box(2, y + 5, 0.5, 1.4, 3, 1.4, C.bone); b.box(0, y + 6.2, 0.5, 1.4, 2.4, 1.4, col, { glow: true }); } })),
  banner: (cloth = C.navy, trim = C.gold) => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, 6, 0.5, 1.4, 34, 1.4, 0x5a4028); b.box(0, 22, 0.5, 8, 1.2, 1.2, 0x5a4028); b.box(0, 14, 0.9, 7, 13, 0.8, cloth); b.box(0, 14, 1.4, 4, 4, 0.4, trim); b.box(0, 7.4, 0.9, 7, 1.2, 0.8, cloth); })),
  horn: () => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -11, 3, 3, 3, 3, C.bone); b.box(0, -10, 6, 3.6, 3.6, 4, C.bone); b.box(0, -8, 9, 4.4, 4.4, 3, C.boneD); b.box(0, -6, 11.4, 5.4, 5.4, 2, C.bone); b.box(0, -11, 3, 3.4, 3.4, 0.8, C.gold); })),
  crate: () => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -10, 5, 10, 8, 8, 0x8a6a3c); b.box(0, -10, 5, 10.4, 1.4, 8.4, 0x5e4326); b.box(0, -10, 5, 1.4, 8.4, 8.4, 0x5e4326); })),
  scroll: () => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -10, 4, 2.4, 2.4, 9, C.cream); b.box(0, -10, 8.8, 3.2, 3.2, 0.8, C.brownD); b.box(0, -10, -0.4, 3.2, 3.2, 0.8, C.brownD); })),
  book: () => (f) => f.parts.armR.add(mesh(f, (b) => { b.box(0, -10, 4, 8, 1.6, 10, 0x6a2a2a); b.box(0, -9.1, 4, 7, 0.6, 9, C.cream); b.box(0, -10, 4.2, 0.6, 1.8, 10.2, C.gold); })),
  lanternBelt: (col = C.soul) => (f) => f.parts.body.add(mesh(f, (b) => { b.box(-5, 1.5, 0.5, 2.6, 4, 2.6, C.black); b.box(-5, 1.5, 0.5, 1.6, 2.8, 1.6, col, { glow: true }); })),
  gilt: (col = C.gold) => (f) => f.parts.body.add(mesh(f, (b) => { b.box(0, 11.4, 0, 9, 1, 5, col); b.box(0, 0.4, 0, 9, 1, 5, col); })),
  apron: (col = C.black) => (f) => f.parts.body.add(mesh(f, (b) => { b.box(0, 4.5, 2.6, 7.2, 10, 0.8, col); b.box(0, 11.2, 2.3, 6, 1.2, 0.8, col); b.box(0, 1.5, 3.2, 3, 3, 0.6, 0x3a3a40); })),
  goggles: () => (f) => f.parts.head.add(mesh(f, (b) => { b.box(-2, 5.5, 4.4, 3, 3, 1, 0x3a3a40); b.box(2, 5.5, 4.4, 3, 3, 1, 0x3a3a40); b.box(-2, 5.5, 4.9, 2, 2, 0.4, 0x8ad8ff, { glow: true }); b.box(2, 5.5, 4.9, 2, 2, 0.4, 0x8ad8ff, { glow: true }); b.box(0, 5.5, 4.4, 1.2, 0.8, 0.8, 0x3a3a40); b.box(0, 5.5, 0, 9, 0.8, 8.6, 0x3a3a40); })),
  glasses: () => (f) => f.parts.head.add(mesh(f, (b) => { b.box(-2, 5.6, 4.5, 3, 2.6, 0.6, C.gold); b.box(2, 5.6, 4.5, 3, 2.6, 0.6, C.gold); b.box(-2, 5.6, 4.7, 2.2, 1.8, 0.3, 0xcfe8f0); b.box(2, 5.6, 4.7, 2.2, 1.8, 0.3, 0xcfe8f0); b.box(0, 5.8, 4.5, 1.6, 0.6, 0.6, C.gold); })),
  glowEyes: (col = C.soul) => (f) => f.parts.head.add(mesh(f, (b) => { b.box(-2, 4.5, 4.7, 1.4, 1, 0.4, col, { glow: true }); b.box(2, 4.5, 4.7, 1.4, 1, 0.4, col, { glow: true }); })),
  frost: () => (f) => f.parts.body.add(mesh(f, (b) => { for (let i = 0; i < 5; i++) b.box(-3.5 + i * 1.8, 0.5 - (i % 2), 2.6, 1, 3, 0.6, 0xcfe8ff, { glow: false }); b.box(0, 12.5, 0, 9.6, 1.2, 5.4, 0xe0f2ff); })),
  ragged: (col) => (f) => f.parts.body.add(mesh(f, (b) => { for (let i = 0; i < 7; i++) b.box(-3.6 + i * 1.2, -1.5 - (i * 37 % 5) * 0.5, 2.2 + (i % 2) * -4.4, 1.1, 4 + (i % 3), 0.7, col); })),
  vents: (col = C.ember) => (f) => f.parts.body.add(mesh(f, (b) => { for (const x of [-2, 2]) b.box(x, 8, -2.4, 1.4, 5, 0.8, col, { glow: true }); b.box(0, 3, 2.4, 4, 1, 0.5, col, { glow: true }); b.box(-3.6, 12.6, -1, 2, 3, 2, C.soot); b.box(3.6, 12.6, -1, 2, 3, 2, C.soot); })),
  tusks: () => (f) => f.parts.head.add(mesh(f, (b) => { b.box(-2.6, 1.2, 6, 1, 3, 1, C.bone, { rx: -0.4 }); b.box(2.6, 1.2, 6, 1, 3, 1, C.bone, { rx: -0.4 }); })),
  boneArmor: () => (f) => { f.parts.body.add(mesh(f, (b) => { for (let i = 0; i < 4; i++) b.box(0, 9 - i * 2.4, 2.5, 8, 1, 0.8, C.bone); b.box(0, 5.5, 2.3, 1.4, 9, 0.6, C.boneD); })); },
  skullPauldron: () => (f) => f.parts.armL.add(mesh(f, (b) => { b.box(0.6, 3, 0, 5, 4, 5, C.bone); b.box(0.6, 3.4, 2.7, 1, 1, 0.4, C.black); })),
  furCollar: (col = 0xe8e0d0) => (f) => f.parts.body.add(mesh(f, (b) => { b.box(0, 12, 0, 11, 2.6, 6.8, col); })),
  warpaint: (col) => (f) => f.parts.head.add(mesh(f, (b) => { b.box(0, 5.5, 4.15, 8.2, 1, 0.3, col); })),
};
function mesh(f, fn) { const b = new Batch(); fn(b); return new THREE.Mesh(b.geometry(), f.mat); }
export { mesh as extraMesh, Batch };

/* ---------------------------------------------------------------- beasts and engines */
export function quadruped(o) {
  const f = new Figure(o.scale ?? 1);
  const { bw = 8, bh = 8, bl = 14, lh = 8, col = 0x8a7a6a, col2 = 0x6a5a4a } = o;
  const by = lh + bh / 2;
  f.part('body', 0, 0, 0, (b) => { b.box(0, by, 0, bw, bh, bl, col); if (o.stripe) for (let i = 0; i < 3; i++) b.box(0, by + bh / 2 + 0.1, -4 + i * 4, bw + 0.2, 0.6, 1.2, o.stripe); if (o.mane) b.box(0, by + bh / 2 + 1, bl / 4, bw * 0.8, 2, bl / 3, o.mane); if (o.plates) { b.box(0, by + bh / 2 + 0.6, 0, bw - 1.5, 1.2, bl - 4, o.plates); } });
  for (const [n, x, z] of [['legFL', -1, 1], ['legFR', 1, 1], ['legBL', -1, -1], ['legBR', 1, -1]]) f.part(n, x * (bw / 2 - 1.2), lh, z * (bl / 2 - 1.8), (b) => { b.box(0, -lh / 2, 0, 2.8, lh, 2.8, col2); if (o.paws) b.box(0, -lh + 0.5, 0.6, 3.2, 1.2, 3.8, o.paws); });
  f.part('head', 0, by + bh * 0.2, bl / 2 + 0.5, (b) => {
    const hs = o.head ?? 6;
    b.box(0, 0, hs / 2, hs, hs * 0.9, hs, col); b.box(0, -hs * 0.2, hs + 2, hs * 0.55, hs * 0.45, hs * 0.5, o.snout ?? col2); b.box(0, -hs * 0.05, hs + 4.4, 1.4, 1.2, 0.6, 0x101010);
    b.box(-hs * 0.3, hs * 0.45, hs - 0.2, 1.2, 1, 0.5, o.eye ?? 0x101010, { glow: !!o.eyeGlow }); b.box(hs * 0.3, hs * 0.45, hs - 0.2, 1.2, 1, 0.5, o.eye ?? 0x101010, { glow: !!o.eyeGlow });
    b.box(-hs * 0.32, hs * 0.75 + 1, 1.5, 1.8, 2.6, 1.2, col2); b.box(hs * 0.32, hs * 0.75 + 1, 1.5, 1.8, 2.6, 1.2, col2);
    if (o.tusks) for (const sg of [-1, 1]) b.box(sg * 1.6, -2, hs + 2.4, 0.8, 2, 0.8, C.bone);
  });
  if (o.tail) f.part('tail', 0, by + bh * 0.3, -bl / 2, (b) => { b.box(0, 1, -3, 2.6, 2.6, 7, o.tailCol ?? col2); });
  if (o.antlers) f.parts.head.add(mesh(f, (b) => { const s = 2; for (const sg of [-1, 1]) { b.box(sg * 3.4, 4.6, 2, 1.6, 6, 1.6, C.bone); b.box(sg * 4.2, 9.4, 2, 1.4, 5, 1.4, C.bone); b.box(sg * 5.6, 12.4, 2, 1.4, 4, 1.4, C.bone); b.box(sg * 3.4, 8, -0.6, 1.2, 4, 1.2, C.bone); b.box(sg * 6.2, 9, 3, 1.2, 4, 1.2, C.bone); b.box(sg * 7.4, 14, 2, 1.2, 3, 1.2, C.bone); } }));
  if (o.horns) f.parts.head.add(mesh(f, (b) => { for (const sg of [-1, 1]) { b.box(sg * 3.4, 5, 1.6, 1.8, 4, 1.8, o.horns); b.box(sg * 4.4, 8, 2.4, 1.6, 3, 1.6, o.horns, { rz: sg * -0.5 }); } }));
  f.kind = 'quad'; return f;
}
export function spider(o = {}) {
  const f = new Figure(o.scale ?? 1);
  const c = o.col ?? 0x3a2a24, c2 = o.col2 ?? 0x5a3a2a;
  f.part('body', 0, 0, 0, (b) => { b.box(0, 7, -2, 8, 7, 10, c); b.box(0, 6, 5, 6, 6, 6, c2); b.box(-1.6, 7.6, 8.1, 1.6, 1.6, 0.5, 0xd82a2a, { glow: true }); b.box(1.6, 7.6, 8.1, 1.6, 1.6, 0.5, 0xd82a2a, { glow: true }); b.box(-0.8, 8.8, 8.1, 0.8, 0.8, 0.5, 0xd82a2a, { glow: true }); b.box(0.8, 8.8, 8.1, 0.8, 0.8, 0.5, 0xd82a2a, { glow: true }); if (o.stripe) for (let i = 0; i < 3; i++) b.box(0, 10.6, -4 + i * 3, 4, 0.6, 1.2, o.stripe); b.box(-1.5, 3.8, 8.4, 1, 3, 1, C.bone); b.box(1.5, 3.8, 8.4, 1, 3, 1, C.bone); });
  for (let i = 0; i < 4; i++) for (const sg of [-1, 1]) f.part('leg' + i + (sg > 0 ? 'R' : 'L'), sg * 3, 6, 4 - i * 3.2, (b) => { b.box(sg * 5, 2.5, 0, 10, 1.4, 1.4, c2, { rz: sg * 0.4 }); b.box(sg * 10.5, -1.5, 0, 1.4, 9, 1.4, c2, { rz: sg * 0.12 }); });
  f.kind = 'spider'; return f;
}
export function golem(o = {}) {
  const f = new Figure(o.scale ?? 1);
  const col = o.col ?? C.steel, trim = o.trim ?? C.brass, eye = o.eye ?? C.ember;
  f.part('legL', 4, 14, 0, (b) => { b.box(0, -7, 0, 6, 14, 6, col); b.box(0, -13, 1, 7, 3, 8, trim); }); f.part('legR', -4, 14, 0, (b) => { b.box(0, -7, 0, 6, 14, 6, col); b.box(0, -13, 1, 7, 3, 8, trim); });
  f.part('body', 0, 14, 0, (b) => { b.box(0, 8, 0, 20, 16, 11, col); b.box(0, 17, 0, 14, 6, 9, col); b.box(0, 3, 0, 14, 4, 9, trim); b.box(0, 9, 5.8, 10, 8, 0.6, trim); b.box(0, 9, 6.2, 6, 5, 0.5, eye, { glow: true }); for (const x of [-7, 7]) b.box(x, 18, 0, 5, 3, 7, trim); b.box(0, 10, -6, 16, 12, 1.2, C.soot); for (const x of [-4, 0, 4]) b.box(x, 15, -6.8, 1.6, 6, 0.8, eye, { glow: true }); });
  f.part('head', 0, 31, 0, (b) => head(b, 'golem', { skin: col, eye }));
  f.parts.head.add(mesh(f, (b) => { b.box(0, 10.4, 0, 10, 1.6, 10, trim); for (const x of [-3.5, 0, 3.5]) b.box(x, 12.4, 0, 1.6, 3, 1.6, trim); }));
  for (const [n, sg] of [['armL', 1], ['armR', -1]]) f.part(n, sg * 14, 28, 0, (b) => { b.box(0, -10, 0, 6, 22, 6, col); b.box(0, -22, 0.5, 8, 7, 8, trim); b.box(sg * 0.6, 1, 0, 8, 5, 8, trim); });
  f.kind = 'golem'; f.height = 40 * (o.scale ?? 1) / 16; return f;
}
export function ballista(o = {}) {
  const f = new Figure(o.scale ?? 1); const wood = 0x6a4a2a, iron = 0x3a3a42;
  f.part('body', 0, 0, 0, (b) => { b.box(0, 5, 0, 12, 2, 20, wood); b.box(-7.4, 5, 0, 2, 10, 10, 0x4a3320, { rz: 0 }); b.box(7.4, 5, 0, 2, 10, 10, 0x4a3320); b.box(-8, 5, 0, 1.4, 10, 10, wood); b.box(8, 5, 0, 1.4, 10, 10, wood); b.box(0, 8.4, -1, 3, 3, 24, wood); b.box(0, 10.4, -1, 1.2, 1.2, 22, iron); b.box(0, 11, 4, 0.8, 0.8, 18, 0xe8e4d8); b.box(0, 10.8, 11, 1.4, 1.4, 6, 0x9a9aa0); b.box(0, 10.8, 14.6, 2.4, 2.4, 1.6, 0x9a9aa0);
    b.box(-10, 10.4, 6, 18, 1.6, 1.6, 0x5a3a20, { ry: 0.25 }); b.box(10, 10.4, 6, 18, 1.6, 1.6, 0x5a3a20, { ry: -0.25 }); b.box(-19, 10.4, 3, 1.2, 1.2, 8, 0xe8e4d8, { ry: 0 }); b.box(0, 2.5, 8, 14, 2, 2, iron); for (const z of [-7, 8]) { b.box(-8.4, 2.4, z, 1.6, 5, 5, 0x5a3a20); b.box(8.4, 2.4, z, 1.6, 5, 5, 0x5a3a20); b.box(-9.2, 2.4, z, 0.6, 2, 2, iron); } });
  f.kind = 'static'; return f;
}
export function mortar(o = {}) {
  const f = new Figure(o.scale ?? 1); const iron = 0x2e2c32, brass = C.brass;
  f.part('body', 0, 0, 0, (b) => { b.box(0, 2, 0, 18, 4, 18, 0x4a4a52); b.box(0, 5, 0, 12, 2, 12, iron); for (const [x, z] of [[-8, -8], [8, -8], [-8, 8], [8, 8]]) b.box(x, 4.5, z, 3, 3, 3, brass); });
  f.part('barrel', 0, 8, 0, (b) => { b.box(0, 8, 2, 8, 20, 8, iron, { rx: -0.55 }); b.box(0, 13, 6.5, 9.6, 3, 9.6, brass, { rx: -0.55 }); b.box(0, 17.6, 9.4, 7.2, 1.4, 7.2, 0x101010, { rx: -0.55 }); b.box(0, 2.5, -1, 11, 7, 11, brass); b.box(0, 5, -1, 1.4, 12, 10, 0x1a1a1e, { rx: 0 }); });
  for (const [x, z] of [[-9, 11], [-6.8, 12.2], [9, 11]]) f.parts.body.add(mesh(f, (b) => { b.box(x, 6.4, z, 4.6, 4.6, 4.6, 0x3a3a40); b.box(x, 6.4, z + 0.01, 4.8, 1, 4.8, brass); }));
  f.kind = 'static'; return f;
}
export function bookModel() {
  const f = new Figure(1.6);
  f.part('base', 0, 0, 0, (b) => { b.box(0, 0.6, 0, 20, 1.2, 12, 0x5a1e1e); b.box(0, 0.6, 0, 1.6, 1.6, 12.2, C.gold); });
  f.part('pageL', -0.6, 1.2, 0, (b) => { b.box(-5, 0.7, 0, 9.4, 0.8, 11, C.cream); b.box(-5, 1.2, 0, 8.4, 0.4, 10, 0xf2e8c8); for (let i = 0; i < 6; i++) b.box(-5, 1.5, -4 + i * 1.6, 7, 0.3, 0.4, 0x6a5a40); });
  f.part('pageR', 0.6, 1.2, 0, (b) => { b.box(5, 0.7, 0, 9.4, 0.8, 11, C.cream); b.box(5, 1.2, 0, 8.4, 0.4, 10, 0xf2e8c8); for (let i = 0; i < 6; i++) b.box(5, 1.5, -4 + i * 1.6, 7, 0.3, 0.4, 0x6a5a40); });
  f.part('flap', 0, 1.6, 0, (b) => { b.box(5, 1.2, 0, 9, 0.5, 10.4, 0xf8eed0); });
  f.parts.pageL.rotation.z = 0.12; f.parts.pageR.rotation.z = -0.12;
  f.kind = 'book'; return f;
}
export function hornModel() {
  const f = new Figure(1.8);
  f.part('horn', 0, 0, 0, (b) => { for (let i = 0; i < 9; i++) { const t = i / 8, r = 2 + t * 5; b.box(Math.sin(t * 2.2) * 6 - 4, 2 + Math.pow(t, 2.2) * 8, -9 + t * 18, r, r, 4.2, i < 1 ? C.gold : (i === 8 ? C.gold : C.bone)); } b.box(-4, 2, -9.6, 3, 3, 1, C.gold); });
  f.part('stand', 0, 0, 0, (b) => { b.box(0, -2, 0, 18, 2, 12, C.brownD); b.box(-5, 0, -2, 1.4, 6, 1.4, C.brass); b.box(5, 0, 2, 1.4, 6, 1.4, C.brass); });
  f.kind = 'static'; return f;
}
export { Batch as BoxBatch };
