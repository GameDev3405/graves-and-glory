// Top-down map canvas: hillshaded relief, grid, object overlays, cursor read-out, zoom and pan.
// It turns pointer positions into map block coordinates and hands them to TOOLS[state.tool].
import { state, onChange, onFieldChange, onLoad, registerView } from './app.js';
import { TOOLS } from './tools.js';

const NO_WATER = -32768;
const view = { ox: 0, oy: 0, scale: 1 }; // screen = block * scale + offset (CSS px)
let canvas, ctx, readout;
let cw = 1, ch = 1, dpr = 1;
let relief = null;            // offscreen canvas holding the shaded field, one pixel per block
let drawQueued = false;
let space = false, pan = null, toolDown = false, cursor = null; // cursor: [x, z] in blocks

// ---------- shading ----------

const lerp = (a, b, t) => a + (b - a) * t;

function groundColor(h) {
  // low = deep green, mid = meadow, high = bare stone
  if (h < 0) { const t = Math.min(1, -h / 20); return [lerp(86, 60, t), lerp(122, 92, t), lerp(70, 58, t)]; }
  const t = Math.min(1, h / 50);
  return [lerp(86, 150, t), lerp(122, 140, t), lerp(70, 118, t)];
}

function buildRelief() {
  const f = state.field;
  if (!f) { relief = null; return; }
  const { width: W, depth: D, height: H, water, road } = f;
  relief = document.createElement('canvas');
  relief.width = W;
  relief.height = D;
  const rctx = relief.getContext('2d');
  const img = rctx.createImageData(W, D);
  const px = img.data;
  // light from the north-west; slopes are exaggerated so low hills still read
  const L = [-0.6, 0.75, -0.6];
  const ln = Math.hypot(...L);
  const flat = L[1] / ln;
  for (let z = 0; z < D; z++) {
    const z0 = Math.max(0, z - 1) * W, z1 = Math.min(D - 1, z + 1) * W;
    for (let x = 0; x < W; x++) {
      const i = z * W + x;
      const gx = (H[z * W + Math.min(W - 1, x + 1)] - H[z * W + Math.max(0, x - 1)]) * 1.4;
      const gz = (H[z1 + x] - H[z0 + x]) * 1.4;
      const nl = Math.hypot(gx, 1, gz);
      let shade = ((-gx * L[0] + L[1] - gz * L[2]) / nl / ln) / flat;
      shade = Math.max(0.35, Math.min(1.5, shade));
      let c;
      if (water[i] !== NO_WATER) {
        const depth = Math.max(0, water[i] - H[i]);
        const k = 1 - Math.min(depth, 8) * 0.06;
        c = [46 * k, 108 * k, 190 * k];
        shade = 0.9 + (shade - 1) * 0.3;
      } else if (road[i]) {
        c = [200, 170, 118];
        shade = 0.85 + (shade - 1) * 0.5;
      } else {
        c = groundColor(H[i]);
      }
      const o = i * 4;
      px[o] = Math.min(255, c[0] * shade);
      px[o + 1] = Math.min(255, c[1] * shade);
      px[o + 2] = Math.min(255, c[2] * shade);
      px[o + 3] = 255;
    }
  }
  rctx.putImageData(img, 0, 0);
}

// ---------- view transform ----------

const toScreen = (x, z) => [x * view.scale + view.ox, z * view.scale + view.oy];
const toWorld = (sx, sy) => [(sx - view.ox) / view.scale, (sy - view.oy) / view.scale];

function applyView() {
  state.pxPerBlock = view.scale;
  queueDraw();
}

function fit() {
  const d = state.design;
  if (!d) return;
  view.scale = Math.max(0.05, Math.min((cw - 40) / d.width, (ch - 40) / d.depth));
  view.ox = (cw - d.width * view.scale) / 2;
  view.oy = (ch - d.depth * view.scale) / 2;
  applyView();
}

function resize() {
  const r = canvas.getBoundingClientRect();
  if (r.width < 2 || r.height < 2) return;
  dpr = window.devicePixelRatio || 1;
  const first = cw === 1 && ch === 1;
  cw = r.width;
  ch = r.height;
  canvas.width = Math.round(cw * dpr);
  canvas.height = Math.round(ch * dpr);
  if (first) fit(); else queueDraw();
}

// ---------- drawing ----------

function queueDraw() {
  if (drawQueued) return;
  drawQueued = true;
  requestAnimationFrame(() => { drawQueued = false; draw(); });
}

function labelStep() {
  let step = 64;
  while (step * view.scale < 48) step *= 2;
  return step;
}

function drawGrid(d) {
  const [x0, y0] = toScreen(0, 0);
  const [x1, y1] = toScreen(d.width, d.depth);
  const minor = 16 * view.scale >= 8;
  ctx.lineWidth = 1;
  for (let b = 0; b <= Math.max(d.width, d.depth); b += 16) {
    const major = b % 64 === 0;
    if (!major && !minor) continue;
    ctx.strokeStyle = major ? 'rgba(255,255,255,0.28)' : 'rgba(255,255,255,0.10)';
    ctx.beginPath();
    if (b <= d.width) { const sx = Math.round(b * view.scale + view.ox) + 0.5; ctx.moveTo(sx, y0); ctx.lineTo(sx, y1); }
    if (b <= d.depth) { const sy = Math.round(b * view.scale + view.oy) + 0.5; ctx.moveTo(x0, sy); ctx.lineTo(x1, sy); }
    ctx.stroke();
  }
  ctx.fillStyle = 'rgba(255,255,255,0.85)';
  ctx.font = '11px system-ui, sans-serif';
  const step = labelStep();
  ctx.textAlign = 'left';
  ctx.textBaseline = 'top';
  for (let b = 0; b <= d.width; b += step) {
    const sx = b * view.scale + view.ox;
    if (sx >= 0 && sx <= cw) ctx.fillText(String(b), sx + 3, Math.max(2, y0 + 2));
  }
  for (let b = step; b <= d.depth; b += step) {
    const sy = b * view.scale + view.oy;
    if (sy >= 0 && sy <= ch) ctx.fillText(String(b), Math.max(2, x0 + 3), sy + 2);
  }
}

const COLOR = { hill: '#f0dc8c', zone: '#9ad0e8', river: '#7cc0ff', road: '#f0d090', point: '#ffffff', dawn: '#ffb347', dusk: '#b894ff' };
const KIND_ORDER = ['zone', 'hill', 'river', 'road', 'point', 'spawn'];

function strokePath(pts, close) {
  ctx.beginPath();
  pts.forEach((p, i) => { const [sx, sy] = toScreen(p[0], p[1]); if (i) ctx.lineTo(sx, sy); else ctx.moveTo(sx, sy); });
  if (close) ctx.closePath();
}

function drawObject(o, selected) {
  ctx.setLineDash(o.state === 'proposed' ? [6, 4] : []);
  ctx.lineWidth = selected ? 3 : 1.5;
  const col = selected ? '#ffffff' : COLOR[o.kind];
  ctx.strokeStyle = col;
  if (o.outline) {
    strokePath(o.outline, true);
    ctx.fillStyle = 'rgba(255,255,255,0.05)';
    ctx.fill();
    ctx.stroke();
  } else if (o.path) {
    strokePath(o.path, false);
    ctx.stroke();
  } else if (o.kind === 'point') {
    const [sx, sy] = toScreen(o.x, o.z);
    ctx.beginPath();
    ctx.arc(sx, sy, Math.max(4, o.radius * view.scale), 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(255,255,255,0.12)';
    ctx.fill();
    ctx.stroke();
    ctx.setLineDash([]);
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 14px system-ui, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(o.name || '?', sx, sy);
  } else if (o.kind === 'spawn') {
    const [sx, sy] = toScreen(o.x, o.z);
    ctx.beginPath();
    ctx.moveTo(sx, sy - 9); ctx.lineTo(sx + 8, sy + 7); ctx.lineTo(sx - 8, sy + 7); ctx.closePath();
    ctx.fillStyle = COLOR[o.team] || '#fff';
    ctx.fill();
    ctx.strokeStyle = selected ? '#fff' : '#111';
    ctx.lineWidth = selected ? 2.5 : 1.5;
    ctx.stroke();
    ctx.setLineDash([]);
    ctx.fillStyle = '#fff';
    ctx.font = '11px system-ui, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    ctx.fillText(o.team, sx, sy + 9);
  }
  ctx.setLineDash([]);
  if (selected && (o.outline || o.path)) {
    ctx.fillStyle = '#5aa0ff';
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 1;
    for (const p of o.outline || o.path) {
      const [sx, sy] = toScreen(p[0], p[1]);
      ctx.fillRect(sx - 4, sy - 4, 8, 8);
      ctx.strokeRect(sx - 4, sy - 4, 8, 8);
    }
  }
}

function drawDraft() {
  const dr = state.draft;
  if (!dr || !dr.pts.length) return;
  ctx.strokeStyle = '#ffffff';
  ctx.lineWidth = 2;
  ctx.setLineDash([5, 4]);
  strokePath(dr.pts, false);
  ctx.stroke();
  if (cursor) {
    const [lx, lz] = dr.pts[dr.pts.length - 1];
    const [ax, ay] = toScreen(lx, lz), [bx, by] = toScreen(cursor[0], cursor[1]);
    ctx.beginPath(); ctx.moveTo(ax, ay); ctx.lineTo(bx, by); ctx.stroke();
  }
  ctx.setLineDash([]);
  ctx.fillStyle = '#fff';
  for (const p of dr.pts) {
    const [sx, sy] = toScreen(p[0], p[1]);
    ctx.beginPath(); ctx.arc(sx, sy, 3.5, 0, Math.PI * 2); ctx.fill();
  }
}

function draw() {
  if (!canvas || state.view !== 'map') return;
  state.mapCenter = toWorld(cw / 2, ch / 2); // the 3D view starts above this spot
  state.mapSpan = ch / view.scale;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.fillStyle = '#101317';
  ctx.fillRect(0, 0, cw, ch);
  const d = state.design;
  if (!d) return;
  const [x0, y0] = toScreen(0, 0);
  if (relief) {
    ctx.imageSmoothingEnabled = view.scale < 1;
    ctx.drawImage(relief, x0, y0, d.width * view.scale, d.depth * view.scale);
  } else {
    ctx.fillStyle = '#4d6a46';
    ctx.fillRect(x0, y0, d.width * view.scale, d.depth * view.scale);
  }
  drawGrid(d);
  ctx.strokeStyle = 'rgba(255,255,255,0.6)';
  ctx.lineWidth = 1;
  ctx.strokeRect(x0 + 0.5, y0 + 0.5, d.width * view.scale, d.depth * view.scale);
  const sel = state.selectedId;
  for (const kind of KIND_ORDER) for (const o of d.objects) if (o.kind === kind && o.id !== sel) drawObject(o, false);
  const so = d.objects.find((o) => o.id === sel);
  if (so) drawObject(so, true);
  drawDraft();
  updateReadout();
}

function updateReadout() {
  if (!readout) return;
  if (!cursor) { readout.textContent = ''; return; }
  const x = Math.floor(cursor[0]), z = Math.floor(cursor[1]);
  const f = state.field;
  let txt = `x ${x}   z ${z}`;
  if (f && x >= 0 && z >= 0 && x < f.width && z < f.depth) {
    const h = f.height[z * f.width + x];
    txt += `   h ${h > 0 ? '+' : ''}${h}`;
    if (f.water[z * f.width + x] !== NO_WATER) txt += '   water';
    else if (f.road[z * f.width + x]) txt += '   road';
  }
  readout.textContent = txt;
}

// ---------- input ----------

function local(ev) {
  const r = canvas.getBoundingClientRect();
  return [ev.clientX - r.left, ev.clientY - r.top];
}

function setCursorStyle() {
  canvas.style.cursor = pan ? 'grabbing' : space ? 'grab' : 'crosshair';
}

function wire() {
  canvas.addEventListener('contextmenu', (ev) => ev.preventDefault());
  canvas.addEventListener('mousedown', (ev) => {
    if (state.view !== 'map' || !state.design) return;
    const [sx, sy] = local(ev);
    if (ev.button === 1 || (ev.button === 0 && space)) {
      ev.preventDefault();
      pan = { sx, sy, ox: view.ox, oy: view.oy };
      setCursorStyle();
      return;
    }
    if (ev.button !== 0) return;
    ev.preventDefault();
    toolDown = true;
    const [x, z] = toWorld(sx, sy);
    TOOLS[state.tool].down(state, x, z, ev);
  });
  window.addEventListener('mousemove', (ev) => {
    if (state.view !== 'map' || !state.design) return;
    const [sx, sy] = local(ev);
    if (pan) {
      view.ox = pan.ox + sx - pan.sx;
      view.oy = pan.oy + sy - pan.sy;
      queueDraw();
      return;
    }
    if (ev.target !== canvas && !toolDown) return;
    const [x, z] = toWorld(sx, sy);
    cursor = [x, z];
    TOOLS[state.tool].move(state, x, z, ev);
    queueDraw();
  });
  window.addEventListener('mouseup', (ev) => {
    if (pan) { pan = null; setCursorStyle(); return; }
    if (!toolDown) return;
    toolDown = false;
    const [sx, sy] = local(ev);
    const [x, z] = toWorld(sx, sy);
    TOOLS[state.tool].up(state, x, z, ev);
  });
  canvas.addEventListener('mouseleave', () => { if (!toolDown) { cursor = null; queueDraw(); } });
  canvas.addEventListener('wheel', (ev) => {
    if (state.view !== 'map') return;
    ev.preventDefault();
    const [sx, sy] = local(ev);
    const [wx, wz] = toWorld(sx, sy);
    view.scale = Math.max(0.2, Math.min(40, view.scale * Math.exp(-ev.deltaY * 0.0015)));
    view.ox = sx - wx * view.scale;
    view.oy = sy - wz * view.scale;
    applyView();
  }, { passive: false });
  const typing = (t) => t && (t.tagName === 'TEXTAREA' || t.tagName === 'SELECT' || (t.tagName === 'INPUT' && t.type !== 'range'));
  window.addEventListener('keydown', (ev) => {
    if (ev.code === 'Space' && !typing(ev.target)) { ev.preventDefault(); if (!space) { space = true; setCursorStyle(); } }
  });
  window.addEventListener('keyup', (ev) => {
    if (ev.code === 'Space') { space = false; setCursorStyle(); }
  });
  window.addEventListener('blur', () => { space = false; pan = null; setCursorStyle(); });
  new ResizeObserver(resize).observe(canvas);
}

export function initMap() {
  canvas = document.getElementById('map');
  ctx = canvas.getContext('2d');
  readout = document.getElementById('readout');
  wire();
  setCursorStyle();
  registerView('map', {
    show() { canvas.hidden = false; readout.hidden = false; state.pxPerBlock = view.scale; resize(); queueDraw(); },
    hide() { canvas.hidden = true; readout.hidden = true; },
  });
  onChange(queueDraw);
  onFieldChange(() => { buildRelief(); queueDraw(); });
  onLoad(() => { relief = null; fit(); });
  resize();
}
