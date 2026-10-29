import * as THREE from '../vendor/three.module.min.js';
import { buildAtlas } from './blocks.js';
import { buildMeshes } from './world.js';
import { Env, PRESET_ORDER } from './render.js';
import { Player } from './player.js';
import { Ctx } from './lobby/ctx.js';
import { buildLobby } from './lobby/index.js';
import { Entities } from './entities.js';
import { Particles } from './particles.js';
import { UI } from './ui.js';
import { NPCS } from './data.js';

const $ = (s) => document.querySelector(s);
const canvas = $('#c');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, powerPreference: 'high-performance', preserveDrawingBuffer: false });
renderer.outputColorSpace = THREE.LinearSRGBColorSpace;
renderer.setPixelRatio(Math.min(devicePixelRatio, 1.6));
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(74, 1, 0.08, 700);
let particles = null;
function resize() { renderer.setSize(innerWidth, innerHeight, false); camera.aspect = innerWidth / innerHeight; camera.updateProjectionMatrix(); if (particles) particles.setScale(renderer.domElement.height * 0.5 / Math.tan(THREE.MathUtils.degToRad(camera.fov / 2)) * 0.5); }
addEventListener('resize', resize); resize();

const L = window.__lobby = { THREE, scene, camera, renderer, canvas, ready: false, locked: false, NPCS };
const setProgress = (msg, f) => { $('#lmsg').textContent = msg; if (f !== undefined) $('#lbar').style.width = (f * 100) + '%'; };
const tick = () => new Promise((r) => setTimeout(r, 12));

(async function init() {
  const params = new URLSearchParams(location.search);
  setProgress('Painting the blocks', 0.02); await tick();
  const atlas = buildAtlas();
  const tex = new THREE.CanvasTexture(atlas.canvas);
  tex.magFilter = THREE.NearestFilter; tex.minFilter = THREE.NearestFilter; tex.generateMipmaps = false; tex.colorSpace = THREE.NoColorSpace;
  const env = new Env(scene, tex); env.setPreset(params.get('time') || 'dusk');
  const ctx = new Ctx();
  let n = 0; const steps = 14;
  await buildLobby(ctx, (m) => setProgress(m, 0.05 + 0.45 * Math.min(1, n++ / steps)));
  setProgress('Lighting the lanterns', 0.56); await tick();
  ctx.w.computeLight();
  setProgress('Laying the blocks', 0.72); await tick();
  const meshes = buildMeshes(ctx.w, atlas.rows, env.materials); scene.add(meshes);
  setProgress('Raising banners, signs and soldiers', 0.88); await tick();
  const entities = new Entities(ctx, scene, env, ctx.w);
  particles = new Particles(scene, env); particles.scanWorld(ctx.w);
  for (const e of ctx.emitters) particles.emitter(e);
  resize();
  const player = new Player(ctx.w, camera);
  Object.assign(L, { env, ctx, world: ctx.w, player, atlas, tex, entities, particles });
  L.setTime = (k) => { env.setPreset(k); document.querySelectorAll('#introTimes button, #tab-settings .btns button').forEach((b) => { if (b.dataset && b.dataset.t) b.classList.toggle('on', b.dataset.t === k); }); L.ui?.buildSettings && 0; };
  L.setBright = (on) => env.setBright(on ? 1 : 0);
  L.requestLock = () => { try { const p = canvas.requestPointerLock?.(); if (p && p.catch) p.catch(() => {}); } catch (e) { /* headless */ } };
  L.releaseLock = () => { try { document.exitPointerLock?.(); } catch (e) { /* ignore */ } };
  L.tp = (x, y, z, yawDeg = 0, pitchDeg = 0) => { player.teleport(x, y, z, THREE.MathUtils.degToRad(yawDeg), THREE.MathUtils.degToRad(pitchDeg)); player.update(0.001); };
  L.tpZone = (z) => { const t = z.tour; L.tp(t.x, t.y, t.z, t.yaw ?? 0, t.pitch ?? 0); };
  L.overview = (z) => { const [x1, z1, x2, z2] = z.rect; const cx = (x1 + x2) / 2, cz = (z1 + z2) / 2, r = Math.max(x2 - x1, z2 - z1); player.flying = true; const gy = Math.max(0, z.y[0]); L.tp(cx, gy + 10 + r * 0.45, Math.min(84, cz + r * 0.9 + 8), 0, -28); };
  L.shot = (x, y, z, yawDeg, pitchDeg, fov) => { player.flying = true; L.tp(x, y - 1.62, z, yawDeg, pitchDeg); if (fov) { camera.fov = fov; camera.updateProjectionMatrix(); } L.frame(1.0, 0.016); };
  L.frame = (t, dt) => { env.update(t, camera); entities.update(t, dt, camera.position); particles.update(t, dt, camera); renderer.render(scene, camera); };
  L.screenshot = () => { renderer.render(scene, camera); canvas.toBlob((b) => { const a = document.createElement('a'); a.href = URL.createObjectURL(b); a.download = 'banner-camp-' + Date.now() + '.png'; a.click(); L.ui.toast('Screenshot saved'); }); };
  const ui = L.ui = new UI(L);
  const spawn = ctx.spawn || { x: 0, y: 2, z: 28, yaw: 0 };
  L.tp(spawn.x, spawn.y, spawn.z, spawn.yaw, 4);
  $('#loading').style.display = 'none'; $('#intro').classList.remove('hidden');
  if (params.has('clean')) { $('#intro').classList.add('hidden'); $('#hud').style.display = 'none'; ui.setPaused(false); }
  else if (params.has('go')) { $('#intro').classList.add('hidden'); $('#hud').classList.remove('hidden'); ui.setPaused(false); }
  // mouse look
  document.addEventListener('mousemove', (e) => { if (L.locked && !ui.paused && Math.abs(e.movementX) < 300 && Math.abs(e.movementY) < 300) player.look(e.movementX, e.movementY); });
  // animated water / lava
  let lastTile = 0;
  let last = performance.now(), acc = 0;
  L.ready = true;
  (function loop(now) {
    requestAnimationFrame(loop);
    const dt = Math.min(0.1, (now - last) / 1000); last = now;
    const t = now / 1000;
    if (!ui.paused) player.update(dt); else player.update(0);
    if (t - lastTile > 0.16) { lastTile = t; atlas.repaint('water', t * 2.2); atlas.repaint('lava', t * 1.3); tex.needsUpdate = true; }
    ui.updateHUD(dt, t);
    L.frame(t, dt);
  })(last);
})().catch((e) => { console.error(e); setProgress('Error: ' + e.message + (e.stack ? ' @ ' + e.stack.split('\n')[1] : '')); });
