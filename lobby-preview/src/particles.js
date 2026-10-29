// Ambient particles: embers, smoke, mist, soul wisps, fireflies, sparkles, bubbles, flames and lantern halos.
import * as THREE from '../vendor/three.module.min.js';
import { BLOCKS, BY_NAME } from './blocks.js';
import { WX0, WX1, WY0, WY1, WZ0, WZ1, SX, SZ } from './world.js';

const VERT = /* glsl */`
attribute vec4 aColor; attribute float aSize; varying vec4 vColor; uniform float uScale;
void main(){ vColor = aColor; vec4 mv = modelViewMatrix * vec4(position,1.0); gl_Position = projectionMatrix * mv; gl_PointSize = clamp(aSize * uScale / max(0.1,-mv.z), 0.0, 220.0); }`;
const FRAG = /* glsl */`
precision mediump float; varying vec4 vColor; uniform float uSoft;
void main(){ float d = length(gl_PointCoord - 0.5) * 2.0; float a = pow(clamp(1.0 - d, 0.0, 1.0), uSoft) * vColor.a; if (a < 0.003) discard; gl_FragColor = vec4(vColor.rgb, a); }`;

const rand = (a, b) => a + Math.random() * (b - a);
const C = (h) => [((h >> 16) & 255) / 255, ((h >> 8) & 255) / 255, (h & 255) / 255];

class Layer {
  constructor(scene, additive, soft, max) {
    this.max = max; this.n = 0;
    this.pos = new Float32Array(max * 3); this.col = new Float32Array(max * 4); this.size = new Float32Array(max);
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.BufferAttribute(this.pos, 3)); g.setAttribute('aColor', new THREE.BufferAttribute(this.col, 4)); g.setAttribute('aSize', new THREE.BufferAttribute(this.size, 1));
    g.setDrawRange(0, 0);
    this.mat = new THREE.ShaderMaterial({ vertexShader: VERT, fragmentShader: FRAG, uniforms: { uScale: { value: 600 }, uSoft: { value: soft } }, transparent: true, depthWrite: false, blending: additive ? THREE.AdditiveBlending : THREE.NormalBlending });
    this.points = new THREE.Points(g, this.mat); this.points.frustumCulled = false; this.points.renderOrder = 5; scene.add(this.points);
    this.geo = g; this.list = [];
  }
  add(p) { if (this.n >= this.max) return; p.i = this.n++; this.list.push(p); }
  flush() { this.geo.setDrawRange(0, this.n); for (const a of ['position', 'aColor', 'aSize']) this.geo.attributes[a].needsUpdate = true; }
}

export class Particles {
  constructor(scene, env) {
    this.env = env; this.add = new Layer(scene, true, 1.6, 9000); this.nor = new Layer(scene, false, 1.1, 6000);
    this.t = 0; this.halos = [];
    this.layers = [this.add, this.nor];
  }
  setScale(h) { for (const l of this.layers) l.mat.uniforms.uScale.value = h * 0.9; }
  emitter(e) {
    const n = e.n ?? 12, type = e.type;
    const layer = (type === 'smoke' || type === 'mist' || type === 'dust' || type === 'bubble') ? this.nor : this.add;
    for (let i = 0; i < n; i++) { const p = { e, type, life: Math.random() * 6, max: 3, sx: 0, sy: 0, sz: 0, vx: 0, vy: 0, vz: 0, ph: Math.random() * 100, size: 0.2, c: [1, 1, 1], a: 0 }; layer.add(p); this.respawn(p, true); }
  }
  respawn(p, first) {
    const { e, type } = p, w = e.w ?? 1, h = e.h ?? 3;
    const bx = e.x + rand(-w / 2, w / 2), bz = e.z + rand(-w / 2, w / 2);
    p.sx = bx; p.sz = bz; p.sy = e.y; p.vx = 0; p.vy = 0; p.vz = 0; p.life = 0;
    switch (type) {
      case 'ember': p.max = rand(1.4, 3.2); p.vy = rand(0.5, 1.3); p.vx = rand(-0.25, 0.25); p.vz = rand(-0.25, 0.25); p.size = rand(0.06, 0.13); p.c = C([0xffb040, 0xff8a2a, 0xffd070][Math.floor(Math.random() * 3)]); break;
      case 'smoke': p.max = rand(4, 7); p.vy = Math.max(0.5, h / p.max) * rand(0.8, 1.1); p.vx = rand(0.1, 0.35); p.vz = rand(-0.1, 0.1); p.size = rand(0.4, 0.7); p.c = C(0x6a6a72); break;
      case 'mist': p.sy = e.y + rand(0, h); p.max = rand(7, 12); p.vx = rand(-0.18, 0.18); p.vz = rand(-0.18, 0.18); p.size = rand(1.6, 3.2); p.c = C(e.color ?? 0xbfe0e8); break;
      case 'soul': p.max = rand(4, 8); p.vy = rand(0.25, 0.6); p.size = rand(0.12, 0.3); p.c = C(e.color ?? 0x6fe8ff); p.sy = e.y + rand(0, h * 0.4); break;
      case 'firefly': p.sy = e.y + rand(0, h); p.max = 1e9; p.size = rand(0.1, 0.18); p.c = C(0xd8ff7a); p.vx = rand(-1, 1); p.vz = rand(-1, 1); break;
      case 'sparkle': p.sy = e.y + rand(0, h); p.max = rand(1.5, 3.5); p.vy = rand(0.05, 0.25); p.size = rand(0.07, 0.16); p.c = C(e.color ?? 0xffe9a0); break;
      case 'bubble': p.sy = e.y + rand(0, 1); p.max = rand(2, 4); p.vy = rand(0.4, 0.9); p.size = rand(0.07, 0.14); p.c = C(0xcfeeff); break;
      case 'dust': p.sy = e.y + rand(0, h); p.max = rand(4, 8); p.vx = rand(0.1, 0.5); p.vz = rand(-0.1, 0.1); p.vy = rand(-0.02, 0.1); p.size = rand(0.05, 0.1); p.c = C(0xc8b090); break;
      case 'flame': p.max = rand(0.45, 0.9); p.vy = rand(0.9, 1.6); p.vx = rand(-0.08, 0.08); p.vz = rand(-0.08, 0.08); p.size = rand(0.28, 0.46) * (e.sz ?? 1); p.c = C(e.color ?? [0xffa030, 0xffd060, 0xff7a20][Math.floor(Math.random() * 3)]); break;
    }
    if (first) p.life = Math.random() * p.max;
    p.ph = Math.random() * 100;
  }
  halo(x, y, z, col, size, a) { const p = { type: 'halo', x, y, z, size, c: C(col), a, ph: Math.random() * 100 }; this.add.add(p); }
  scanWorld(w) {
    const L = BY_NAME;
    const ids = { lantern: L.lantern, soul_lantern: L.soul_lantern, torch: L.torch, soul_torch: L.soul_torch, campfire: L.campfire, soul_campfire: L.soul_campfire, candle: L.candle, lava: L.lava, amethyst_cluster: L.amethyst_cluster, shroomlight: L.shroomlight, magma_block: L.magma_block };
    const inv = {}; for (const [k, v] of Object.entries(ids)) inv[v] = k;
    let lavaCount = 0, n = 0;
    for (let y = WY0; y <= WY1; y++) for (let z = WZ0; z <= WZ1; z++) {
      const row = ((y - WY0) * SZ + (z - WZ0)) * SX;
      for (let x = WX0; x <= WX1; x++) {
        const id = w.ids[row + (x - WX0)]; if (id === 0) continue;
        const k = inv[id]; if (!k) continue;
        const px = x + 0.5, pz = z + 0.5;
        switch (k) {
          case 'lantern': this.halo(px, y + 0.35, pz, 0xffc060, 2.2, 0.26); break;
          case 'soul_lantern': this.halo(px, y + 0.35, pz, 0x70e8ff, 2.2, 0.26); break;
          case 'torch': this.halo(px, y + 0.8, pz, 0xffb050, 2.4, 0.34); this.emitter({ type: 'flame', x: px, y: y + 0.7, z: pz, w: 0.1, n: 3, sz: 0.9 }); break;
          case 'soul_torch': this.halo(px, y + 0.8, pz, 0x70e8ff, 1.7, 0.28); this.emitter({ type: 'flame', x: px, y: y + 0.7, z: pz, w: 0.1, n: 2, color: 0x6fe8ff }); break;
          case 'campfire': this.halo(px, y + 0.8, pz, 0xff9a3a, 6.5, 0.42); this.emitter({ type: 'flame', x: px, y: y + 0.3, z: pz, w: 0.45, n: 10, sz: 2.1 }); this.emitter({ type: 'ember', x: px, y: y + 0.6, z: pz, w: 0.4, n: 4 }); this.emitter({ type: 'smoke', x: px, y: y + 0.9, z: pz, w: 0.3, h: 5, n: 3 }); break;
          case 'soul_campfire': this.halo(px, y + 0.8, pz, 0x70e8ff, 6.5, 0.42); this.emitter({ type: 'flame', x: px, y: y + 0.3, z: pz, w: 0.45, n: 10, sz: 2.1, color: 0x6fe8ff }); this.emitter({ type: 'soul', x: px, y: y + 0.8, z: pz, w: 0.4, h: 3, n: 3 }); break;
          case 'candle': this.halo(px, y + 0.5, pz, 0xffd070, 1.1, 0.3); break;
          case 'lava': if ((lavaCount++ % 3) === 0) { this.halo(px, y + 1, pz, 0xff8a2a, 3.2, 0.25); this.emitter({ type: 'ember', x: px, y: y + 0.9, z: pz, w: 0.8, n: 1 }); } break;
          case 'amethyst_cluster': this.halo(px, y + 0.5, pz, 0xc89cff, 1.8, 0.22); break;
          case 'shroomlight': if (n++ % 4 === 0) this.halo(px, y + 0.5, pz, 0xff9a3a, 2.4, 0.16); break;
        }
      }
    }
  }
  update(t, dt, camera) {
    this.t = t; dt = Math.min(dt, 0.05);
    const night = this.env.preset ? Math.max(0.55, 1.15 - this.env.preset.skyI * 0.6) : 1;   // halos read stronger when dark
    for (const layer of this.layers) {
      for (const p of layer.list) {
        const i = p.i, i3 = i * 3, i4 = i * 4;
        if (p.type === 'halo') {
          const fl = 0.85 + 0.15 * Math.sin(t * 7 + p.ph) + 0.08 * Math.sin(t * 19 + p.ph * 2);
          layer.pos[i3] = p.x; layer.pos[i3 + 1] = p.y; layer.pos[i3 + 2] = p.z; layer.size[i] = p.size; layer.col[i4] = p.c[0]; layer.col[i4 + 1] = p.c[1]; layer.col[i4 + 2] = p.c[2]; layer.col[i4 + 3] = p.a * fl * night; continue;
        }
        p.life += dt;
        if (p.life >= p.max) this.respawn(p, false);
        const u = p.life / p.max;
        let x = p.sx + p.vx * p.life, y = p.sy + p.vy * p.life, z = p.sz + p.vz * p.life, a = 0, s = p.size;
        switch (p.type) {
          case 'ember': a = (1 - u) * (0.7 + 0.3 * Math.sin(t * 20 + p.ph)); s = p.size * (1 - u * 0.6); x += Math.sin(t * 3 + p.ph) * 0.12 * u; break;
          case 'smoke': a = Math.sin(Math.min(1, u * 1.2) * Math.PI) * 0.34; s = p.size * (1 + u * 3.2); x += Math.sin(t * 0.7 + p.ph) * 0.25 * u; break;
          case 'mist': a = Math.sin(u * Math.PI) * 0.14; x += Math.sin(t * 0.3 + p.ph) * 0.9; z += Math.cos(t * 0.27 + p.ph) * 0.9; break;
          case 'soul': a = Math.sin(u * Math.PI) * 0.8; x += Math.sin(t * 1.3 + p.ph) * 0.4; z += Math.cos(t * 1.1 + p.ph) * 0.4; s = p.size * (0.8 + 0.4 * Math.sin(t * 3 + p.ph)); break;
          case 'firefly': { const w = p.e.w ?? 6; x = p.e.x + Math.sin(t * 0.35 + p.ph) * w * 0.45 + Math.sin(t * 0.9 + p.ph * 3) * 0.6; z = p.e.z + Math.cos(t * 0.29 + p.ph * 1.7) * w * 0.45 + Math.cos(t * 1.1 + p.ph) * 0.6; y = p.sy + Math.sin(t * 0.5 + p.ph) * 0.6; a = Math.pow(Math.max(0, Math.sin(t * 1.6 + p.ph * 5)), 3) * 0.95; p.life = 0; break; }
          case 'sparkle': a = Math.pow(Math.sin(u * Math.PI), 2) * 0.9; x += Math.sin(t * 0.8 + p.ph) * 0.25; z += Math.cos(t * 0.7 + p.ph) * 0.25; break;
          case 'bubble': a = Math.sin(u * Math.PI) * 0.55; x += Math.sin(t * 2 + p.ph) * 0.1; break;
          case 'dust': a = Math.sin(u * Math.PI) * 0.22; break;
          case 'flame': a = Math.sin(Math.min(1, u * 1.4) * Math.PI) * 0.85; s = p.size * (1 - u * 0.7); break;
        }
        layer.pos[i3] = x; layer.pos[i3 + 1] = y; layer.pos[i3 + 2] = z; layer.size[i] = s;
        layer.col[i4] = p.c[0]; layer.col[i4 + 1] = p.c[1]; layer.col[i4 + 2] = p.c[2]; layer.col[i4 + 3] = a;
      }
      layer.flush();
    }
  }
}
