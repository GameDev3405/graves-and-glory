// 3D view of the map: one InstancedMesh with a box per column, translucent water, pillars for points and spawns.
// Tab flips to it (registered as the 'walk' view). Fly mode by default; F toggles walking with gravity.
// Pointer events go through groundAt() into the same TOOLS handlers the map view uses.
import * as THREE from './vendor/three.module.min.js';
import { PointerLockControls } from './vendor/PointerLockControls.js';
import { state, onChange, onFieldChange, onLoad, registerView, say } from './app.js';
import { TOOLS } from './tools.js';

const NO_WATER = -32768;
const FOV = 60;
const EYE = 1.62;          // eye height above the ground top
const STEP_UP = 1;         // walk mode climbs one block
const GRAVITY = 28;
const JUMP = 8.5;
const WALK_SPEED = 5.5;
const WATER_DROP = 0.12;   // water surface sits a little under the top of its block
const PILLAR_H = 40;

// sRGB colours per palette, from the surface / bed / road blocks in tools/mapengine/palettes.json
const PALETTE = {
  default: { surface: 0x5f9444, bed: 0x84807c, road: 0x9a7f4a },
  ironhold: { surface: 0x7d7d7d, bed: 0x84807c, road: 0x6c6c6c },
  frostmarch: { surface: 0xf1f5f7, bed: 0x84807c, road: 0x8fb3e6 },
};
const lin = (hex) => [((hex >> 16) & 255) / 255, ((hex >> 8) & 255) / 255, (hex & 255) / 255].map((v) => Math.pow(v, 2.2));
const PAL = Object.fromEntries(Object.entries(PALETTE).map(([k, p]) => [k, { surface: lin(p.surface), bed: lin(p.bed), road: lin(p.road) }]));
const TEAM_COLOR = { dawn: 0xffb347, dusk: 0xb894ff };

// ---------- instance data (pure, so it can be timed without a GL context) ----------

// Fills matrices (16 floats per column) and colours (3 per column) in place; returns the tallest top face.
export function fillInstances(f, mat, col) {
  const { width: W, depth: D, height: H, water, road, zone, zones } = f;
  const zc = [PAL.default, ...(zones || []).map((n) => PAL[n] || PAL.default)];
  let maxTop = 0;
  for (let z = 0; z < D; z++) {
    for (let x = 0; x < W; x++) {
      const i = z * W + x;
      const h = H[i];
      let mn = h - 2; // beyond the canvas edge: drop two blocks so the edge wall shows
      if (x > 0) mn = H[i - 1];
      if (x + 1 < W) mn = Math.min(mn, H[i + 1]); else mn = Math.min(mn, h - 2);
      if (z > 0) mn = Math.min(mn, H[i - W]); else mn = Math.min(mn, h - 2);
      if (z + 1 < D) mn = Math.min(mn, H[i + W]); else mn = Math.min(mn, h - 2);
      if (x === 0) mn = Math.min(mn, h - 2);
      const bottom = mn < h ? mn + 1 : h;
      const top = h + 1;
      if (top > maxTop) maxTop = top;
      const o = i * 16;
      mat[o] = 1; mat[o + 1] = 0; mat[o + 2] = 0; mat[o + 3] = 0;
      mat[o + 4] = 0; mat[o + 5] = top - bottom; mat[o + 6] = 0; mat[o + 7] = 0;
      mat[o + 8] = 0; mat[o + 9] = 0; mat[o + 10] = 1; mat[o + 11] = 0;
      mat[o + 12] = x; mat[o + 13] = bottom; mat[o + 14] = z; mat[o + 15] = 1;
      const pal = zc[zone[i] + 1] || PAL.default;
      const c = water[i] !== NO_WATER ? pal.bed : road[i] ? pal.road : pal.surface;
      const v = 0.94 + ((Math.imul(i, 2654435761) >>> 0) / 4294967296) * 0.12; // small per-column variation
      const k = i * 3;
      col[k] = c[0] * v; col[k + 1] = c[1] * v; col[k + 2] = c[2] * v;
    }
  }
  return maxTop;
}

// Groups of 4-connected water columns with the same level. Returns [{level, runs: [x, z, len, ...]}].
export function waterGroups(f) {
  const { width: W, depth: D, water } = f;
  const n = W * D;
  const label = new Int32Array(n);
  const groups = [];
  const stack = new Int32Array(n);
  for (let s = 0; s < n; s++) {
    if (water[s] === NO_WATER || label[s]) continue;
    const level = water[s];
    const id = groups.length + 1;
    groups.push({ level, runs: [] });
    let sp = 0;
    stack[sp++] = s;
    label[s] = id;
    while (sp) {
      const i = stack[--sp];
      const x = i % W;
      if (x > 0 && !label[i - 1] && water[i - 1] === level) { label[i - 1] = id; stack[sp++] = i - 1; }
      if (x + 1 < W && !label[i + 1] && water[i + 1] === level) { label[i + 1] = id; stack[sp++] = i + 1; }
      if (i >= W && !label[i - W] && water[i - W] === level) { label[i - W] = id; stack[sp++] = i - W; }
      if (i + W < n && !label[i + W] && water[i + W] === level) { label[i + W] = id; stack[sp++] = i + W; }
    }
  }
  for (let z = 0; z < D; z++) {
    let x = 0;
    while (x < W) {
      const id = label[z * W + x];
      if (!id) { x++; continue; }
      let e = x + 1;
      while (e < W && label[z * W + e] === id) e++;
      groups[id - 1].runs.push(x, z, e - x);
      x = e;
    }
  }
  return groups;
}

// ---------- the view ----------

let el, hud, renderer, scene, camera, controls, dom;
let mesh = null, waterGroup = null, objGroup = null;
let field = null, design = null, maxTop = 0;
let shown = false, built = false, stale = true, needsRender = true, loopOn = false;
let mode = 'fly';           // 'fly' | 'walk'
let flySpeed = 60;
let feet = 0, vy = 0;
const keys = {};
let looking = false, toolDown = false, dragY = null, last = 0;
const labelTex = new Map();
const matCache = new Map();
const raycaster = new THREE.Raycaster();
const tmpV = new THREE.Vector3();
const pillarGeo = new THREE.BoxGeometry(1, 1, 1).translate(0, 0.5, 0);
let hover = null;           // last terrain pick, for the read-out

const clamp = (v, a, b) => Math.min(b, Math.max(a, v));

function topAt(x, z) {
  if (!field) return 0;
  const cx = clamp(Math.floor(x), 0, field.width - 1), cz = clamp(Math.floor(z), 0, field.depth - 1);
  return field.height[cz * field.width + cx] + 1;
}

function mount(container) {
  el = container;
  renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
  dom = renderer.domElement;
  dom.style.display = 'block';
  el.appendChild(dom);
  scene = new THREE.Scene();
  scene.background = new THREE.Color(0x9cc7e8);
  camera = new THREE.PerspectiveCamera(FOV, 1, 0.1, 6000);
  camera.rotation.order = 'YXZ';
  scene.add(new THREE.HemisphereLight(0xffffff, 0x556070, 1.6));
  const sun = new THREE.DirectionalLight(0xffffff, 2.2);
  sun.position.set(-0.6, 1.0, -0.5);
  scene.add(sun);
  objGroup = new THREE.Group();
  scene.add(objGroup);
  controls = new PointerLockControls(camera, dom);
  controls.addEventListener('change', () => { needsRender = true; });
  hud = document.createElement('div');
  hud.className = 'hud3d';
  el.appendChild(hud);
  wire();
  new ResizeObserver(resize).observe(el);
}

function resize() {
  if (!renderer) return;
  const r = el.getBoundingClientRect();
  if (r.width < 2 || r.height < 2) return;
  renderer.setSize(r.width, r.height, false);
  dom.style.width = '100%';
  dom.style.height = '100%';
  camera.aspect = r.width / r.height;
  camera.updateProjectionMatrix();
  needsRender = true;
}

// ---------- terrain ----------

function setField(f, d) {
  const t0 = performance.now();
  field = f;
  design = d || design;
  stale = false;
  if (!f) return;
  const n = f.width * f.depth;
  if (!mesh || mesh.userData.n !== n) {
    if (mesh) { scene.remove(mesh); mesh.geometry.dispose(); mesh.material.dispose(); mesh.dispose(); }
    const geo = new THREE.BoxGeometry(1, 1, 1).translate(0.5, 0, 0.5); // base at y = 0, so scale y = height
    geo.translate(0, 0.5, 0);
    mesh = new THREE.InstancedMesh(geo, new THREE.MeshLambertMaterial({ color: 0xffffff }), n);
    mesh.instanceColor = new THREE.InstancedBufferAttribute(new Float32Array(n * 3), 3);
    mesh.frustumCulled = false;
    mesh.userData.n = n;
    scene.add(mesh);
  }
  maxTop = fillInstances(f, mesh.instanceMatrix.array, mesh.instanceColor.array);
  mesh.instanceMatrix.needsUpdate = true;
  mesh.instanceColor.needsUpdate = true;
  rebuildWater();
  rebuildObjects();
  needsRender = true;
  View3D.lastRebuildMs = performance.now() - t0;
}

function rebuildWater() {
  if (waterGroup) {
    scene.remove(waterGroup);
    waterGroup.traverse((o) => { if (o.geometry) o.geometry.dispose(); });
  }
  waterGroup = new THREE.Group();
  const mat = new THREE.MeshLambertMaterial({ color: 0x2f6fd0, transparent: true, opacity: 0.6, side: THREE.DoubleSide, depthWrite: false });
  for (const g of waterGroups(field)) {
    const nq = g.runs.length / 3;
    const pos = new Float32Array(nq * 12);
    const idx = new Uint32Array(nq * 6);
    const y = g.level + 1 - WATER_DROP;
    for (let q = 0; q < nq; q++) {
      const x = g.runs[q * 3], z = g.runs[q * 3 + 1], len = g.runs[q * 3 + 2];
      pos.set([x, y, z, x + len, y, z, x + len, y, z + 1, x, y, z + 1], q * 12);
      idx.set([q * 4, q * 4 + 2, q * 4 + 1, q * 4, q * 4 + 3, q * 4 + 2], q * 6);
    }
    const geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
    geo.setIndex(new THREE.BufferAttribute(idx, 1));
    geo.computeVertexNormals();
    const m = new THREE.Mesh(geo, mat);
    m.renderOrder = 1;
    m.frustumCulled = false;
    waterGroup.add(m);
  }
  scene.add(waterGroup);
}

// ---------- points, spawns and the shape being edited ----------

function material(hex) {
  if (!matCache.has(hex)) matCache.set(hex, new THREE.MeshLambertMaterial({ color: hex, emissive: hex, emissiveIntensity: 0.35 }));
  return matCache.get(hex);
}

function label(text, color) {
  const key = text + color;
  if (!labelTex.has(key)) {
    const c = document.createElement('canvas');
    c.width = 128; c.height = 64;
    const g = c.getContext('2d');
    g.font = 'bold 40px system-ui, sans-serif';
    g.textAlign = 'center'; g.textBaseline = 'middle';
    g.lineWidth = 6; g.strokeStyle = '#111'; g.strokeText(text, 64, 32);
    g.fillStyle = color; g.fillText(text, 64, 32);
    const t = new THREE.CanvasTexture(c);
    t.colorSpace = THREE.SRGBColorSpace;
    labelTex.set(key, t);
  }
  return labelTex.get(key);
}

function line(pts, hex, closed) {
  const g = new THREE.BufferGeometry().setFromPoints(pts);
  const l = closed ? new THREE.LineLoop(g, new THREE.LineBasicMaterial({ color: hex })) : new THREE.Line(g, new THREE.LineBasicMaterial({ color: hex }));
  l.frustumCulled = false;
  return l;
}

// terrain-following polyline through the given [x, z] vertices
function drape(vs, closed, lift) {
  const out = [];
  const n = closed ? vs.length : vs.length - 1;
  for (let i = 0; i < n; i++) {
    const a = vs[i], b = vs[(i + 1) % vs.length];
    const steps = Math.max(1, Math.ceil(Math.hypot(b[0] - a[0], b[1] - a[1]) / 3));
    for (let s = 0; s < steps; s++) {
      const x = a[0] + ((b[0] - a[0]) * s) / steps, z = a[1] + ((b[1] - a[1]) * s) / steps;
      out.push(new THREE.Vector3(x, topAt(x, z) + lift, z));
    }
  }
  if (!closed && vs.length) { const e = vs[vs.length - 1]; out.push(new THREE.Vector3(e[0], topAt(e[0], e[1]) + lift, e[1])); }
  return out;
}

function pillar(x, z, hex, w, h) {
  const m = new THREE.Mesh(pillarGeo, material(hex));
  m.scale.set(w, h, w);
  m.position.set(x, topAt(x, z), z);
  m.userData.pillar = true;
  return m;
}

function rebuildObjects() {
  if (!objGroup) return;
  for (const c of [...objGroup.children]) {
    objGroup.remove(c);
    if (c.isLine) { c.geometry.dispose(); c.material.dispose(); }
  }
  const d = design;
  if (!d) return;
  const sel = state.selectedId;
  for (const o of d.objects) {
    const selected = o.id === sel;
    if (o.kind === 'point' || o.kind === 'spawn') {
      const hex = selected ? 0xffffff : o.kind === 'point' ? 0xf5d142 : TEAM_COLOR[o.team] || 0xffffff;
      const p = pillar(o.x, o.z, hex, o.kind === 'point' ? 3 : 2.4, PILLAR_H);
      objGroup.add(p);
      const text = o.kind === 'point' ? o.name || '?' : o.team;
      const sp = new THREE.Sprite(new THREE.SpriteMaterial({ map: label(text, '#ffffff'), depthTest: false, sizeAttenuation: false, transparent: true }));
      sp.scale.set(0.16, 0.08, 1);
      sp.position.set(o.x, p.position.y + PILLAR_H + 2, o.z);
      sp.renderOrder = 10;
      objGroup.add(sp);
      if (o.kind === 'point') {
        const ring = [];
        for (let i = 0; i < 48; i++) {
          const a = (i / 48) * Math.PI * 2, x = o.x + Math.cos(a) * o.radius, z = o.z + Math.sin(a) * o.radius;
          ring.push(new THREE.Vector3(x, topAt(x, z) + 0.2, z));
        }
        objGroup.add(line(ring, selected ? 0xffffff : 0xf5d142, true));
      }
    } else if (selected) {
      const vs = o.outline || o.path;
      if (vs) {
        objGroup.add(line(drape(vs, !!o.outline, 0.3), 0xffffff, false));
        for (const v of vs) objGroup.add(pillar(v[0], v[1], 0x5aa0ff, 1.4, 6));
      }
    }
  }
  const dr = state.draft;
  if (dr && dr.pts.length) {
    objGroup.add(line(drape(dr.pts, false, 0.3), 0xffffff, false));
    for (const v of dr.pts) objGroup.add(pillar(v[0], v[1], 0xffffff, 1.4, 6));
  }
  objGroup.updateMatrixWorld(true);
  needsRender = true;
}

// ---------- picking ----------

function rayFrom(clientX, clientY) {
  const r = dom.getBoundingClientRect();
  const ndc = new THREE.Vector2(((clientX - r.left) / r.width) * 2 - 1, -((clientY - r.top) / r.height) * 2 + 1);
  camera.updateMatrixWorld();
  raycaster.setFromCamera(ndc, camera);
  return raycaster.ray;
}

function marchTerrain(ray) {
  const o = ray.origin, dir = ray.direction;
  if (!field) {
    if (dir.y >= 0) return null;
    const t = -o.y / dir.y;
    return { t, x: o.x + dir.x * t, z: o.z + dir.z * t };
  }
  if (dir.y >= 0 && o.y > maxTop) return null;
  let prev = 0, t = 0;
  while (t < 6000) {
    const y = o.y + dir.y * t;
    if (y < topAt(o.x + dir.x * t, o.z + dir.z * t)) {
      let lo = prev, hi = t;
      for (let i = 0; i < 12; i++) {
        const mid = (lo + hi) / 2;
        if (o.y + dir.y * mid < topAt(o.x + dir.x * mid, o.z + dir.z * mid)) hi = mid; else lo = mid;
      }
      return { t: hi, x: o.x + dir.x * hi, z: o.z + dir.z * hi };
    }
    if (dir.y >= 0 && y > maxTop) return null;
    prev = t;
    t += Math.max(0.5, t * 0.005);
  }
  return null;
}

// {x, z, t, y}: nearest of terrain and pillars. pillar is true when a pillar was hit.
function pick(clientX, clientY, terrainOnly) {
  const ray = rayFrom(clientX, clientY);
  const ter = marchTerrain(ray);
  if (!terrainOnly && objGroup) {
    const hits = raycaster.intersectObjects(objGroup.children.filter((c) => c.userData.pillar), false);
    if (hits.length && (!ter || hits[0].distance < ter.t)) {
      const p = hits[0].point;
      return { x: p.x, z: p.z, t: hits[0].distance, y: p.y, pillar: true };
    }
  }
  return ter ? { x: ter.x, z: ter.z, t: ter.t, y: 0, pillar: false } : null;
}

function pickOnPlane(clientX, clientY, y) {
  const ray = rayFrom(clientX, clientY);
  if (Math.abs(ray.direction.y) < 1e-6) return null;
  const t = (y - ray.origin.y) / ray.direction.y;
  if (t <= 0) return null;
  return { x: ray.origin.x + ray.direction.x * t, z: ray.origin.z + ray.direction.z * t, t };
}

// ---------- input ----------

const typing = (t) => t && (t.tagName === 'TEXTAREA' || t.tagName === 'SELECT' || (t.tagName === 'INPUT' && t.type !== 'range' && t.type !== 'checkbox'));

function setPx(dist) {
  const h = dom.getBoundingClientRect().height;
  state.pxPerBlock = h / (2 * Math.max(dist, 0.5) * Math.tan((FOV * Math.PI) / 360));
}

function updateHud(extra) {
  if (!hud) return;
  const parts = [mode === 'walk' ? 'WALK' : 'FLY'];
  if (hover) {
    const x = Math.floor(hover.x), z = Math.floor(hover.z);
    parts.push(`x ${x}  z ${z}`);
    if (field && x >= 0 && z >= 0 && x < field.width && z < field.depth) parts.push('h ' + field.height[z * field.width + x]);
  }
  hud.innerHTML = `<b>${parts.join('   ')}</b><br>` +
    (mode === 'walk'
      ? 'WASD walk, Shift run, Space jump, mouse look (click or right-drag), F fly, Tab map'
      : 'WASD move, Space up, Shift down, right-drag look, wheel speed, F walk, Tab map') + (extra ? '<br>' + extra : '');
}

function wire() {
  dom.addEventListener('contextmenu', (e) => e.preventDefault());
  dom.addEventListener('mousedown', (ev) => {
    if (!shown || !state.design) return;
    if (ev.button === 2) { ev.preventDefault(); looking = true; return; }
    if (ev.button !== 0) return;
    ev.preventDefault();
    if (controls.isLocked) return; // mouse belongs to looking while walking
    const p = pick(ev.clientX, ev.clientY, false);
    if (!p) return;
    toolDown = true;
    dragY = p.pillar ? p.y : null;
    setPx(p.t);
    TOOLS[state.tool].down(state, p.x, p.z, ev);
  });
  window.addEventListener('mousemove', (ev) => {
    if (!shown || !state.design) return;
    if (looking) {
      camera.rotation.y -= ev.movementX * 0.0025;
      camera.rotation.x = clamp(camera.rotation.x - ev.movementY * 0.0025, -1.55, 1.55);
      needsRender = true;
      return;
    }
    if (controls.isLocked || (ev.target !== dom && !toolDown)) return;
    const p = (toolDown && dragY !== null ? pickOnPlane(ev.clientX, ev.clientY, dragY) : null) || pick(ev.clientX, ev.clientY, true);
    if (!p) return;
    hover = p;
    setPx(p.t);
    TOOLS[state.tool].move(state, p.x, p.z, ev);
    updateHud();
  });
  window.addEventListener('mouseup', (ev) => {
    if (ev.button === 2) { looking = false; return; }
    if (!toolDown) return;
    toolDown = false;
    const p = (dragY !== null ? pickOnPlane(ev.clientX, ev.clientY, dragY) : null) || pick(ev.clientX, ev.clientY, true);
    dragY = null;
    if (p) TOOLS[state.tool].up(state, p.x, p.z, ev);
  });
  dom.addEventListener('wheel', (ev) => {
    if (!shown) return;
    ev.preventDefault();
    flySpeed = clamp(flySpeed * Math.exp(-ev.deltaY * 0.001), 5, 400);
  }, { passive: false });
  window.addEventListener('keydown', (ev) => {
    if (!shown || typing(ev.target) || ev.ctrlKey || ev.metaKey || ev.altKey) return;
    if (['KeyW', 'KeyA', 'KeyS', 'KeyD', 'Space', 'ShiftLeft', 'ShiftRight'].includes(ev.code)) {
      keys[ev.code] = true;
      if (ev.code === 'Space') ev.preventDefault();
      return;
    }
    if (ev.code === 'KeyF' && !ev.repeat) { toggleWalk(); }
  });
  window.addEventListener('keyup', (ev) => { delete keys[ev.code]; });
  window.addEventListener('blur', () => { for (const k of Object.keys(keys)) delete keys[k]; looking = false; });
}

function lockPointer() {
  try {
    const p = dom.requestPointerLock();
    if (p && p.catch) p.catch(() => {});
  } catch (e) { /* right-drag still looks around */ }
}

function releasePointer() {
  if (document.pointerLockElement) document.exitPointerLock();
}

function toggleWalk() {
  if (mode === 'fly') {
    mode = 'walk';
    feet = topAt(camera.position.x, camera.position.z);
    vy = 0;
    lockPointer();
  } else {
    mode = 'fly';
    releasePointer();
  }
  updateHud();
  needsRender = true;
}

// ---------- movement and the frame loop ----------

function update(dt) {
  const fwdKey = (keys.KeyW ? 1 : 0) - (keys.KeyS ? 1 : 0);
  const sideKey = (keys.KeyD ? 1 : 0) - (keys.KeyA ? 1 : 0);
  const shift = keys.ShiftLeft || keys.ShiftRight;
  let moved = false;
  if (mode === 'fly') {
    const up = (keys.Space ? 1 : 0) - (shift ? 1 : 0);
    if (fwdKey || sideKey || up) {
      camera.getWorldDirection(tmpV);
      const fx = tmpV.x, fy = tmpV.y, fz = tmpV.z;
      const sp = flySpeed * dt;
      camera.position.x += (fx * fwdKey + -fz * sideKey) * sp;
      camera.position.z += (fz * fwdKey + fx * sideKey) * sp;
      camera.position.y += (fy * fwdKey + up) * sp;
      moved = true;
    }
  } else {
    const yaw = camera.rotation.y;
    const fx = -Math.sin(yaw), fz = -Math.cos(yaw);
    let dx = fx * fwdKey - fz * sideKey, dz = fz * fwdKey + fx * sideKey;
    const len = Math.hypot(dx, dz);
    if (len > 0) {
      const sp = (WALK_SPEED * (shift ? 1.8 : 1) * dt) / len;
      dx *= sp; dz *= sp;
      const px = camera.position.x, pz = camera.position.z;
      const maxX = field ? field.width - 0.3 : 1e9, maxZ = field ? field.depth - 0.3 : 1e9;
      const nx = clamp(px + dx, 0.3, maxX);
      if (topAt(nx, pz) <= feet + STEP_UP) camera.position.x = nx;
      const nz = clamp(pz + dz, 0.3, maxZ);
      if (topAt(camera.position.x, nz) <= feet + STEP_UP) camera.position.z = nz;
      moved = true;
    }
    const ground = topAt(camera.position.x, camera.position.z);
    if (keys.Space && feet <= ground + 1e-3) vy = JUMP;
    vy -= GRAVITY * dt;
    feet += vy * dt;
    if (feet <= ground) { feet = ground; vy = 0; } else moved = true;
    const ey = feet + EYE;
    if (Math.abs(camera.position.y - ey) > 1e-4) { camera.position.y = ey; moved = true; }
  }
  if (moved) needsRender = true;
}

function frame(t) {
  if (!shown) { loopOn = false; return; }
  const dt = Math.min(0.1, (t - last) / 1000 || 0);
  last = t;
  update(dt);
  if (needsRender) {
    needsRender = false;
    renderer.render(scene, camera);
  }
  requestAnimationFrame(frame);
}

function placeCamera() {
  // above the map view's centre, looking at the spot it showed
  const c = state.mapCenter || [(design ? design.width : 700) / 2, (design ? design.depth : 500) / 2];
  const span = clamp(state.mapSpan || (design ? design.depth : 500), 20, 1500);
  const dist = (span / 2) / Math.tan((FOV * Math.PI) / 360);
  const pitch = (55 * Math.PI) / 180;
  const ty = topAt(c[0], c[1]);
  camera.position.set(c[0], ty + Math.sin(pitch) * dist, c[1] + Math.cos(pitch) * dist);
  camera.lookAt(c[0], ty, c[1]);
  camera.rotation.order = 'YXZ';
}

function show() {
  shown = true;
  mode = 'fly';
  for (const k of Object.keys(keys)) delete keys[k];
  resize();
  if (stale && state.field) setField(state.field, state.design);
  design = state.design;
  rebuildObjects();
  placeCamera();
  hover = null;
  updateHud();
  needsRender = true;
  if (!loopOn) { loopOn = true; last = performance.now(); requestAnimationFrame(frame); }
}

function hide() {
  shown = false;
  looking = false;
  toolDown = false;
  releasePointer();
}

export const View3D = {
  mount,
  setField,
  show,
  hide,
  // pointer position -> ground [x, z] in map block coordinates, or null (raycast against the terrain)
  groundAt(clientX, clientY) {
    const p = pick(clientX, clientY, true);
    return p ? [p.x, p.z] : null;
  },
  lastRebuildMs: 0,
  get instanceCount() { return mesh ? mesh.count : 0; },
  get mode() { return mode; },
  get camera() { return camera; },
};

// Called once by app.js at boot: builds the renderer inside #walk and registers the 'walk' view for Tab.
export function initWalk() {
  const host = document.getElementById('walk');
  try {
    View3D.mount(host);
  } catch (e) {
    say('The 3D view needs WebGL: ' + e.message);
    return;
  }
  registerView('walk', {
    show() { host.hidden = false; View3D.show(); },
    hide() { View3D.hide(); host.hidden = true; },
  });
  onFieldChange((f) => { if (shown) View3D.setField(f, state.design); else { stale = true; } });
  onChange(() => { if (shown) { design = state.design; rebuildObjects(); } });
  onLoad(() => { field = null; stale = true; });
  controls.addEventListener('lock', () => updateHud());
  controls.addEventListener('unlock', () => updateHud());
  window.mapengine = { state, View3D, TOOLS }; // handy for checks from the browser console
}
