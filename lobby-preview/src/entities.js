// Turns the build registries (signs, banners, figures, props) into live THREE objects, animates them and answers "what am I looking at".
import * as THREE from '../vendor/three.module.min.js';
import { makeFigure, mannequin } from './catalog.js';
import { quadruped, ballista, mortar, bookModel, hornModel, C as FC } from './figures.js';
import { TROOPS, RARITY, HOUSE_NAMES, NPCS, CLASSES } from './data.js';

const FONT = (px, w = 'bold') => `${w} ${px}px Marcellus, "Trajan Pro", Georgia, "Times New Roman", serif`;
const BODY = (px, w = '') => `${w} ${px}px Lato, "Segoe UI", system-ui, sans-serif`;
const hex = (h) => '#' + h.toString(16).padStart(6, '0');
const deg = THREE.MathUtils.degToRad;

/* ------------------------------------------------------------------ sign textures */
const STYLES = {
  wood: { bg: '#5a3e26', bg2: '#4a3220', border: '#2a1c10', text: '#f4e4c4', sub: '#d8c298', planks: true },
  gold: { bg: '#1c2038', bg2: '#141830', border: '#c8a56b', text: '#f0cd78', sub: '#cfc4a4' },
  iron: { bg: '#2c2c32', bg2: '#222228', border: '#6e6e78', text: '#eee9df', sub: '#b8b8c0' },
  vellum: { bg: '#e6d8aa', bg2: '#d8c890', border: '#8a6a3a', text: '#2a2219', sub: '#5c4a32' },
  bone: { bg: '#d4ccb2', bg2: '#c4bca2', border: '#6a6450', text: '#1c1a14', sub: '#3e3a2e' },
  copper: { bg: '#1e5a52', bg2: '#184a44', border: '#7fffe0', text: '#eafff8', sub: '#b8ece0' },
  stone: { bg: '#72727a', bg2: '#62626a', border: '#3a3a42', text: '#1a1a20', sub: '#2a2a30' },
};
function wrapLines(ctx, text, maxW) {
  const words = text.split(/\s+/), lines = []; let line = '';
  for (const w of words) { const t = line ? line + ' ' + w : w; if (ctx.measureText(t).width > maxW && line) { lines.push(line); line = w; } else line = t; }
  if (line) lines.push(line); return lines;
}
export function signCanvas(s) {
  const st = STYLES[s.style || 'wood'];
  const ppb = 112, W = Math.min(1024, Math.round(s.w * ppb)), H = Math.min(512, Math.round(s.h * ppb));
  const cv = document.createElement('canvas'); cv.width = W; cv.height = H;
  const c = cv.getContext('2d');
  c.fillStyle = st.bg; c.fillRect(0, 0, W, H);
  if (st.planks) { c.fillStyle = st.bg2; for (let y = H / 4; y < H; y += H / 4) c.fillRect(0, y - 2, W, 3); for (let i = 0; i < 90; i++) { c.fillStyle = `rgba(0,0,0,${Math.random() * 0.1})`; c.fillRect(Math.random() * W, Math.random() * H, 30 + Math.random() * 50, 2); } }
  else { const g = c.createLinearGradient(0, 0, 0, H); g.addColorStop(0, st.bg); g.addColorStop(1, st.bg2); c.fillStyle = g; c.fillRect(0, 0, W, H); }
  c.strokeStyle = st.border; c.lineWidth = Math.max(4, H * 0.05); c.strokeRect(c.lineWidth / 2, c.lineWidth / 2, W - c.lineWidth, H - c.lineWidth);
  c.fillStyle = st.border; for (const [x, y] of [[10, 10], [W - 10, 10], [10, H - 10], [W - 10, H - 10]]) { c.beginPath(); c.arc(x, y, 3, 0, 7); c.fill(); }
  c.textAlign = 'center'; c.textBaseline = 'middle';
  const pad = W * 0.06, maxW = W - pad * 2;
  const size = (s.size ?? 0.6) * ppb;
  if (s.wrap) {
    let fs = size * 0.7; let lines;
    do { c.font = BODY(fs, '600'); lines = wrapLines(c, s.text, maxW); fs -= 2; } while (lines.length * fs * 1.25 > H * (s.sub ? 0.66 : 0.9) && fs > 8);
    const lh = fs * 1.25; let y = (s.sub ? H * 0.42 : H / 2) - (lines.length - 1) * lh / 2;
    c.fillStyle = st.text; c.font = BODY(fs + 2, '600'); for (const l of lines) { c.fillText(l, W / 2, y); y += lh; }
    if (s.sub) { c.font = BODY(Math.min(fs * 0.9, 22), ''); c.fillStyle = st.sub; const sl = wrapLines(c, s.sub, maxW); let yy = H * 0.86 - (sl.length - 1) * 11; for (const l of sl) { c.fillText(l, W / 2, yy); yy += 22; } }
  } else {
    let fs = size; c.font = FONT(fs); while (c.measureText(s.text).width > maxW && fs > 10) { fs -= 2; c.font = FONT(fs); }
    const hasSub = !!s.sub; c.fillStyle = st.text; c.shadowColor = 'rgba(0,0,0,.35)'; c.shadowBlur = 3;
    c.fillText(s.text, W / 2, hasSub ? H * 0.38 : H / 2); c.shadowBlur = 0;
    if (hasSub) { let sfs = Math.min(fs * 0.5, 26); c.font = BODY(sfs); const sl = wrapLines(c, s.sub, maxW); while (sl.length > 2 && sfs > 9) { sfs -= 1; c.font = BODY(sfs); sl.splice(0, sl.length, ...wrapLines(c, s.sub, maxW)); } c.fillStyle = st.sub; let yy = H * (sl.length > 1 ? 0.66 : 0.72); for (const l of sl) { c.fillText(l, W / 2, yy); yy += sfs * 1.2; } }
  }
  return cv;
}

/* ------------------------------------------------------------------ emblems + banner textures */
function drawEmblem(c, name, cx, cy, S, col, col2) {
  c.save(); c.translate(cx, cy); c.fillStyle = col; c.strokeStyle = col; c.lineWidth = S * 0.09; c.lineCap = 'round'; c.lineJoin = 'round';
  switch (name) {
    case 'sun': { c.beginPath(); c.arc(0, 0, S * 0.2, 0, 7); c.fill(); for (let i = 0; i < 12; i++) { const a = i / 12 * Math.PI * 2, r1 = S * 0.27, r2 = S * (i % 2 ? 0.38 : 0.46); c.beginPath(); c.moveTo(Math.cos(a - 0.12) * r1, Math.sin(a - 0.12) * r1); c.lineTo(Math.cos(a) * r2, Math.sin(a) * r2); c.lineTo(Math.cos(a + 0.12) * r1, Math.sin(a + 0.12) * r1); c.fill(); } break; }
    case 'moon': { c.beginPath(); c.arc(0, 0, S * 0.34, 0, 7); c.fill(); c.globalCompositeOperation = 'destination-out'; c.beginPath(); c.arc(S * 0.14, -S * 0.06, S * 0.3, 0, 7); c.fill(); c.globalCompositeOperation = 'source-over'; break; }
    case 'skull': { c.beginPath(); c.roundRect(-S * 0.3, -S * 0.36, S * 0.6, S * 0.5, S * 0.2); c.fill(); c.fillRect(-S * 0.2, S * 0.08, S * 0.4, S * 0.2); c.fillStyle = col2; c.beginPath(); c.arc(-S * 0.13, -S * 0.12, S * 0.08, 0, 7); c.arc(S * 0.13, -S * 0.12, S * 0.08, 0, 7); c.fill(); c.fillRect(-S * 0.03, -S * 0.02, S * 0.06, S * 0.1); for (const x of [-0.1, 0, 0.1]) c.fillRect(S * x - S * 0.012, S * 0.1, S * 0.024, S * 0.18); break; }
    case 'antler': { c.beginPath(); for (const sg of [-1, 1]) { c.moveTo(sg * S * 0.04, S * 0.3); c.lineTo(sg * S * 0.14, S * 0.05); c.lineTo(sg * S * 0.3, -S * 0.3); c.moveTo(sg * S * 0.14, S * 0.05); c.lineTo(sg * S * 0.34, -S * 0.04); c.moveTo(sg * S * 0.2, -S * 0.12); c.lineTo(sg * S * 0.08, -S * 0.3); } c.stroke(); break; }
    case 'anvil': { c.beginPath(); c.moveTo(-S * 0.38, -S * 0.2); c.lineTo(S * 0.3, -S * 0.2); c.lineTo(S * 0.42, -S * 0.12); c.lineTo(S * 0.2, -S * 0.06); c.lineTo(S * 0.16, S * 0.08); c.lineTo(S * 0.3, S * 0.18); c.lineTo(S * 0.3, S * 0.28); c.lineTo(-S * 0.3, S * 0.28); c.lineTo(-S * 0.3, S * 0.18); c.lineTo(-S * 0.16, S * 0.08); c.lineTo(-S * 0.16, -S * 0.06); c.lineTo(-S * 0.34, -S * 0.1); c.closePath(); c.fill(); break; }
    case 'trident': { c.beginPath(); c.moveTo(0, S * 0.4); c.lineTo(0, -S * 0.3); c.moveTo(-S * 0.24, -S * 0.1); c.lineTo(-S * 0.24, -S * 0.34); c.moveTo(S * 0.24, -S * 0.1); c.lineTo(S * 0.24, -S * 0.34); c.moveTo(-S * 0.24, -S * 0.08); c.quadraticCurveTo(0, S * 0.12, S * 0.24, -S * 0.08); c.stroke(); break; }
    case 'hollow': { c.beginPath(); c.arc(0, S * 0.04, S * 0.3, 0, 7); c.stroke(); c.lineWidth = S * 0.05; c.beginPath(); c.moveTo(0, -S * 0.46); c.lineTo(0, S * 0.46); c.stroke(); c.beginPath(); c.moveTo(0, -S * 0.44); c.lineTo(S * 0.2, -S * 0.36); c.lineTo(0, -S * 0.28); c.fill(); break; }
    case 'crown': { c.beginPath(); c.moveTo(-S * 0.34, S * 0.2); c.lineTo(-S * 0.38, -S * 0.22); c.lineTo(-S * 0.18, -S * 0.04); c.lineTo(0, -S * 0.3); c.lineTo(S * 0.18, -S * 0.04); c.lineTo(S * 0.38, -S * 0.22); c.lineTo(S * 0.34, S * 0.2); c.closePath(); c.fill(); c.fillStyle = col2; c.fillRect(-S * 0.3, S * 0.1, S * 0.6, S * 0.06); break; }
    case 'map': { c.lineWidth = S * 0.04; c.strokeRect(-S * 0.34, -S * 0.3, S * 0.68, S * 0.6); c.beginPath(); c.moveTo(-S * 0.2, S * 0.1); c.quadraticCurveTo(0, -S * 0.2, S * 0.2, S * 0.05); c.stroke(); c.beginPath(); c.moveTo(S * 0.08, -S * 0.14); c.lineTo(S * 0.2, -S * 0.02); c.moveTo(S * 0.2, -S * 0.14); c.lineTo(S * 0.08, -S * 0.02); c.stroke(); break; }
  }
  c.restore();
}
export function bannerCanvas(b) {
  const W = 64, H = Math.max(32, Math.round(64 * b.h / b.w));
  const cv = document.createElement('canvas'); cv.width = W; cv.height = H; const c = cv.getContext('2d'); c.imageSmoothingEnabled = false;
  const bg = hex(b.bg), trim = hex(b.trim ?? 0xc9a24a);
  c.clearRect(0, 0, W, H);
  c.fillStyle = bg; c.fillRect(0, 0, W, H);
  const swallow = b.kind === 'standard' || b.kind === 'pennant';
  // subtle weave
  c.fillStyle = 'rgba(0,0,0,.08)'; for (let y = 0; y < H; y += 2) c.fillRect(0, y, W, 1);
  c.fillStyle = trim; c.fillRect(0, 0, W, 3); c.fillRect(0, 0, 3, H); c.fillRect(W - 3, 0, 3, H); c.fillRect(0, H - 3, W, 3);
  c.fillStyle = 'rgba(255,255,255,.07)'; c.fillRect(3, 3, W - 6, 2);
  const S = Math.min(W * 0.72, H * 0.62);
  const lum = ((b.bg >> 16) & 255) * 0.3 + ((b.bg >> 8) & 255) * 0.59 + (b.bg & 255) * 0.11;
  drawEmblem(c, b.emblem, W / 2, H * 0.44, S, lum > 150 ? trim : trim, lum > 150 ? '#222' : bg);
  if (swallow) { c.globalCompositeOperation = 'destination-out'; c.beginPath(); c.moveTo(W / 2, H * 0.84); c.lineTo(-2, H + 2); c.lineTo(W + 2, H + 2); c.closePath(); c.fill(); c.globalCompositeOperation = 'source-over'; c.strokeStyle = trim; c.lineWidth = 3; c.beginPath(); c.moveTo(0, H - 1); c.lineTo(W / 2, H * 0.84); c.lineTo(W, H - 1); c.stroke(); }
  return cv;
}

function labelSprite(lines, o = {}) {
  const cv = document.createElement('canvas'); cv.width = 512; cv.height = 128; const c = cv.getContext('2d');
  c.textAlign = 'center'; c.textBaseline = 'middle';
  c.fillStyle = 'rgba(18,16,22,.62)'; c.beginPath(); c.roundRect(6, 10, 500, 108, 14); c.fill(); c.strokeStyle = o.border || 'rgba(200,165,107,.55)'; c.lineWidth = 3; c.stroke();
  let fs = 44; c.font = FONT(fs); while (c.measureText(lines[0].t).width > 470 && fs > 16) { fs -= 2; c.font = FONT(fs); }
  c.fillStyle = lines[0].c || '#f3ecdc'; c.fillText(lines[0].t, 256, lines[1] ? 46 : 64);
  if (lines[1]) { let f2 = 28; c.font = BODY(f2); while (c.measureText(lines[1].t).width > 470 && f2 > 12) { f2 -= 1; c.font = BODY(f2); } c.fillStyle = lines[1].c || '#c8a56b'; c.fillText(lines[1].t, 256, 90); }
  const tex = new THREE.CanvasTexture(cv); tex.minFilter = THREE.LinearFilter; tex.generateMipmaps = false;
  const sp = new THREE.Sprite(new THREE.SpriteMaterial({ map: tex, transparent: true, depthTest: true, depthWrite: false, fog: false }));
  sp.scale.set(2.6, 0.65, 1); sp.renderOrder = 6; return sp;
}

/* ------------------------------------------------------------------ the manager */
export class Entities {
  constructor(ctx, scene, env, world) {
    this.ctx = ctx; this.scene = scene; this.env = env; this.world = world;
    this.group = new THREE.Group(); scene.add(this.group);
    this.lit = [];            // {setLight(color), pos:[x,y,z]}
    this.banners = []; this.figures = []; this.pickables = [];
    this.unlockMode = 'mixed'; this.labelsOn = true;
    this._c = new THREE.Color();
    for (const s of ctx.signs) this.addSign(s);
    for (const b of ctx.banners) this.addBanner(b);
    for (const t of ctx.troops) this.addTroop(t);
    for (const n of ctx.npcs) this.addNpc(n);
    for (const p of ctx.props) this.addProp(p);
    env.onChange.push(() => this.relight());
    this.relight();
  }
  lightAt(x, y, z) { const [s, b] = this.world.lightAt(x, y, z); return this.env.entityLight(s, b, this._c); }
  relight() {
    for (const l of this.lit) {
      const col = this.lightAt(l.pos[0], l.pos[1], l.pos[2]).clone();
      if (l.locked) col.multiply(new THREE.Color(0.52, 0.58, 0.78));
      l.setLight(col);
    }
  }
  addLit(setLight, pos, extra = {}) { const o = { setLight, pos, ...extra }; this.lit.push(o); return o; }

  addSign(s) {
    const cv = signCanvas(s); const tex = new THREE.CanvasTexture(cv); tex.minFilter = THREE.LinearFilter; tex.generateMipmaps = false; tex.anisotropy = 4;
    const g = new THREE.Group();
    const front = new THREE.Mesh(new THREE.PlaneGeometry(s.w, s.h), new THREE.MeshBasicMaterial({ map: tex })); front.position.z = 0.04;
    const back = new THREE.Mesh(new THREE.BoxGeometry(s.w + 0.1, s.h + 0.1, 0.07), new THREE.MeshBasicMaterial({ color: 0x3a2a1a }));
    g.add(back, front); g.position.set(s.x, s.y, s.z); g.rotation.y = deg(s.yaw) + Math.PI; this.group.add(g);
    this.addLit((c) => { front.material.color.copy(c).multiplyScalar(1.12); back.material.color.copy(c).multiplyScalar(0.35); front.material.color.r = Math.min(1.2, front.material.color.r); }, [s.x, s.y, s.z]);
    // sign posts fixed by the builder; text for the tour
    g.userData.sign = s;
  }
  addBanner(b) {
    const cv = bannerCanvas(b); const tex = new THREE.CanvasTexture(cv); tex.magFilter = THREE.NearestFilter; tex.minFilter = THREE.NearestFilter; tex.generateMipmaps = false;
    const segX = 6, segY = Math.max(6, Math.round(b.h * 3));
    const geo = new THREE.PlaneGeometry(b.w, b.h, segX, segY);
    const pennant = b.kind === 'pennant';
    geo.translate(pennant ? b.w / 2 : 0, -b.h / 2, 0);
    const mat = new THREE.MeshBasicMaterial({ map: tex, side: THREE.DoubleSide, alphaTest: 0.5 });
    const m = new THREE.Mesh(geo, mat); m.position.set(b.x, b.y, b.z);
    m.rotation.y = pennant ? deg(b.yaw - 90) : deg(b.yaw) + Math.PI;
    this.group.add(m);
    const base = geo.attributes.position.array.slice();
    this.banners.push({ m, geo, base, kind: b.kind, ph: Math.random() * 10, amp: { wall: 0.05, pole: 0.12, pennant: 0.16, standard: 0.24 }[b.kind] ?? 0.06, h: b.h });
    this.addLit((c) => mat.color.copy(c).multiplyScalar(1.1), [b.x, b.y - b.h * 0.5, b.z]);
  }
  place(fig, o, yawExtra = 0) {
    fig.root.position.set(o.x, o.y, o.z); fig.root.rotation.y = deg(o.yaw) + Math.PI + yawExtra; this.group.add(fig.root);
  }
  addTroop(t) {
    const fig = makeFigure(t.id); this.place(fig, t);
    const d = TROOPS[t.id]; const r = RARITY[d.r];
    const key = t.id; const h = [...key].reduce((a, c) => a + c.charCodeAt(0), 0);
    const locked = d.r === 'L' ? (h % 2 === 0) : d.r === 'E' ? (h % 3 === 0) : d.r === 'R' ? (h % 5 === 0) : false;
    const stars = 1 + (h % 3);
    const ent = { kind: 'troop', id: t.id, fig, data: d, pos: new THREE.Vector3(t.x, t.y + (fig.height || 2) * 0.55, t.z), radius: 1.1, top: (fig.height || 2) + 0.55, locked, stars, ph: Math.random() * 9, yaw: deg(t.yaw) + Math.PI, house: t.house, legendary: t.legendary };
    ent.label = labelSprite([{ t: d.name, c: '#f3ecdc' }, { t: '', c: r[1] }], { border: r[1] }); ent.label.position.set(t.x, t.y + ent.top, t.z); this.group.add(ent.label);
    ent.lit = this.addLit((c) => fig.setLight(c), [t.x, t.y + 1, t.z], { locked }); ent.litObj = ent.lit;
    this.setLabel(ent); this.figures.push(ent); this.pickables.push(ent);
  }
  setLabel(ent) {
    const r = RARITY[ent.data.r];
    const locked = this.unlockMode === 'all' ? false : this.unlockMode === 'locked' ? true : ent.locked;
    ent.lockedNow = locked; ent.litObj.locked = locked;
    const sub = locked ? 'LOCKED · ' + r[0] : '★'.repeat(ent.stars) + '☆'.repeat(3 - ent.stars) + ' · ' + r[0];
    const old = ent.label, p = old.position.clone(); this.group.remove(old); old.material.map.dispose();
    ent.label = labelSprite([{ t: ent.data.name, c: locked ? '#9a9aa8' : '#f3ecdc' }, { t: sub, c: locked ? '#8a8a98' : r[1] }], { border: r[1] }); ent.label.position.copy(p); this.group.add(ent.label);
  }
  setUnlockMode(m) { this.unlockMode = m; for (const e of this.figures) if (e.kind === 'troop') this.setLabel(e); this.relight(); }
  addNpc(n) {
    const isClass = n.kind === 'classfig';
    const fig = makeFigure(n.id); this.place(fig, n);
    const info = isClass ? { name: CLASSES[n.cls][0], role: CLASSES[n.cls][1] + ' · commander class', station: "Commander's Gallery", desc: CLASSES[n.cls][2], lines: [] } : NPCS[n.id];
    const ent = { kind: isClass ? 'class' : 'npc', id: n.id, fig, info, pos: new THREE.Vector3(n.x, n.y + (fig.height || 2) * 0.55, n.z), radius: 1.0, top: (fig.height || 2) + 0.5, ph: Math.random() * 9, yaw: deg(n.yaw) + Math.PI, pose: n.pose, lineIdx: 0 };
    if (n.pose === 'sit') { fig.root.position.y -= 0.52 * (fig.scale || 1); fig.sitting = true; }
    ent.label = labelSprite([{ t: info.name, c: '#f3ecdc' }, { t: info.role, c: '#c8a56b' }]); ent.label.position.set(n.x, n.y + ent.top - (n.pose === 'sit' ? 0.5 : 0), n.z); this.group.add(ent.label);
    this.addLit((c) => fig.setLight(c), [n.x, n.y + 1, n.z]);
    this.figures.push(ent); this.pickables.push(ent);
  }
  addProp(p) {
    let f = null;
    switch (p.type) {
      case 'ballista': f = ballista({ scale: 1.0 }); break;
      case 'mortar': f = mortar({ scale: 1.0 }); break;
      case 'wolf': f = quadruped({ scale: 1.0, bw: 6, bh: 6, bl: 12, lh: 6, col: 0xb8b8b8, col2: 0x8a8a90, tail: true, head: 6, snout: 0xc8c0b8, eye: 0xf0c030, eyeGlow: true, paws: 0xe0dcd0 }); break;
      case 'mannequin': f = mannequin(p.set); break;
      case 'book': f = bookModel(); break;
      case 'horn': f = hornModel(); break;
    }
    if (!f) return;
    this.place(f, p); f.root.scale.setScalar(p.scale ?? 1);
    this.addLit((c) => f.setLight(c), [p.x, p.y + 0.8, p.z]);
    const ent = { kind: 'prop', type: p.type, fig: f, pos: new THREE.Vector3(p.x, p.y + 0.8, p.z), ph: Math.random() * 9, yaw: deg(p.yaw) + Math.PI, baseY: p.y };
    if (p.type === 'book') { ent.pickInfo = { name: 'The Long March', role: 'Campaign storybook', station: 'War Table', desc: 'The book lies open on the war table; the wind turns it to the player\'s page. Each page holds two to four scenes; clicking a scene plays it, then hands straight to its battle. Chapter 1, The Road North, has six battles from Rivergate to Kingsfield.', lines: [] }; ent.radius = 1.1; this.pickables.push(ent); ent.kind = 'prop'; }
    this.figures.push(ent);
  }

  /* ---------------- animation ---------------- */
  update(t, dt, camPos) {
    const cx = camPos.x, cz = camPos.z;
    for (const b of this.banners) {
      const arr = b.geo.attributes.position.array, base = b.base, n = arr.length / 3;
      for (let i = 0; i < n; i++) {
        const x = base[i * 3], y = base[i * 3 + 1];
        const k = Math.min(1, Math.abs(y) / b.h) ;
        const along = b.kind === 'pennant' ? x / 3 : Math.abs(y) / 2;
        arr[i * 3 + 2] = Math.sin(t * 1.9 + b.ph + along * 1.4 + x * 0.5) * b.amp * (b.kind === 'pennant' ? Math.min(1, x / 1.4 + 0.05) : k) * (1 + 0.3 * Math.sin(t * 0.6 + b.ph));
      }
      b.geo.attributes.position.needsUpdate = true;
    }
    for (const e of this.figures) {
      const f = e.fig, dx = cx - e.pos.x, dz = cz - e.pos.z, dist = Math.hypot(dx, dz);
      if (e.label) { const vis = this.labelsOn && dist < (e.kind === 'npc' || e.kind === 'class' ? 14 : 9.5); e.label.visible = vis; e.label.material.opacity = Math.max(0, Math.min(1, (e.kind === 'npc' || e.kind === 'class' ? 14 : 9.5) - dist)); }
      if (dist > 70) continue;
      const P = f.parts;
      if (e.kind === 'prop') {
        if (e.type === 'book') { f.root.position.y = e.baseY + Math.sin(t * 1.3) * 0.03; if (P.flap) P.flap.rotation.z = Math.PI * 0.5 * (1 - Math.cos(t * 0.5)); }
        if (e.type === 'wolf' && P.tail) P.tail.rotation.y = Math.sin(t * 4 + e.ph) * 0.5;
        continue;
      }
      if (f.kind === 'quad') { const s = 1 + Math.sin(t * 1.4 + e.ph) * 0.012; P.body.scale.set(1, s, 1); if (P.tail) P.tail.rotation.y = Math.sin(t * 3.6 + e.ph) * 0.45; P.head.rotation.y = Math.sin(t * 0.5 + e.ph) * 0.25; this.track(e, P.head, camPos, 1.0); continue; }
      if (f.kind === 'spider') { for (const k of Object.keys(P)) if (k.startsWith('leg')) P[k].rotation.z = Math.sin(t * 3 + e.ph + k.length * 1.7) * 0.08; P.body.position.y = Math.sin(t * 1.6 + e.ph) * 0.2; continue; }
      if (f.kind === 'golem') { P.armL.rotation.x = Math.sin(t * 0.8 + e.ph) * 0.05 - 0.05; P.armR.rotation.x = -Math.sin(t * 0.8 + e.ph) * 0.05 - 0.05; P.body.scale.y = 1 + Math.sin(t * 1.1 + e.ph) * 0.01; this.track(e, P.head, camPos, 0.7); continue; }
      const crew = f.crew; const hf = crew || f;
      if (f.kind === 'crew' && crew) { const Pc = crew.parts; Pc.armR.rotation.x = (crew.armPose?.R ?? -0.7) + Math.sin(t * 1.2 + e.ph) * 0.05; Pc.armL.rotation.x = (crew.armPose?.L ?? -0.5) + Math.cos(t * 1.1 + e.ph) * 0.05; this.track(e, Pc.head, camPos, 1, crew.root); continue; }
      if (!P.armR) continue;
      const ap = f.armPose || {};
      P.armR.rotation.x = (ap.R ?? 0) + Math.sin(t * 1.4 + e.ph) * 0.03; P.armL.rotation.x = (ap.L ?? 0) + Math.cos(t * 1.3 + e.ph) * 0.03;
      if (f.zombieArms) { P.armR.rotation.x = -1.45 + Math.sin(t * 1.5 + e.ph) * 0.06; P.armL.rotation.x = -1.45 + Math.cos(t * 1.4 + e.ph) * 0.06; }
      P.body.scale.y = 1 + Math.sin(t * 1.6 + e.ph) * 0.012;
      if (e.pose === 'sit') { P.legR.rotation.x = -1.5; P.legL.rotation.x = -1.5; }
      this.track(e, P.head, camPos, 1);
    }
  }
  track(e, head, camPos, k, root = e.fig.root) {
    const dx = camPos.x - root.position.x, dz = camPos.z - root.position.z, dist = Math.hypot(dx, dz);
    let target = 0;
    if (dist < 10) { const want = Math.atan2(dx, dz) - root.rotation.y; let d = Math.atan2(Math.sin(want), Math.cos(want)); target = Math.max(-1.0, Math.min(1.0, d)) * k; }
    head.rotation.y += (target - head.rotation.y) * 0.08;
  }

  /* ---------------- picking ---------------- */
  pick(camera, maxDist = 7.5) {
    const o = camera.position, d = new THREE.Vector3(); camera.getWorldDirection(d);
    let best = null, bd = 1e9; const v = new THREE.Vector3();
    for (const e of this.pickables) {
      const hgt = (e.fig && e.fig.height) || 2, feet = e.pos.y - hgt * 0.55, r = (e.radius ?? 1) * 0.62;
      let hit = false, dist = 0;
      for (let k = 0; k < 6 && !hit; k++) {
        v.set(e.pos.x - o.x, feet + hgt * (0.08 + k * 0.17) - o.y, e.pos.z - o.z);
        const l = v.length(), along = v.dot(d); if (l > maxDist || along <= 0.2) continue;
        const perp = Math.sqrt(Math.max(0, l * l - along * along)); if (perp < r) { hit = true; dist = l; }
      }
      if (hit && dist < bd) { best = e; bd = dist; }
    }
    return best;
  }
}
