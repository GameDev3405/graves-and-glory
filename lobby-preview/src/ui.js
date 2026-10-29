// HUD, tour menu, builder notes (bill of materials), blueprint map, interaction cards.
import * as THREE from '../vendor/three.module.min.js';
import { BLOCKS, TILE_INDEX, TILE, COLS } from './blocks.js';
import { WX0, WX1, WZ0, WZ1, WY0, WY1, SX, SZ, DISPLAY_Y, raycastBlocks } from './world.js';
import { PRESETS, PRESET_ORDER } from './render.js';
import { RARITY, HOUSE_NAMES } from './data.js';

const $ = (s, r = document) => r.querySelector(s);
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html !== undefined) e.innerHTML = html; return e; };
const dispY = (y) => y + DISPLAY_Y;

export class UI {
  constructor(app) {
    this.app = app; this.hud = $('#hud'); this.paused = true; this.bpOpen = false; this.menuOpen = false; this.hidden = false;
    this.zone = null; this.zoneTimer = 0; this.dialogEnt = null; this.cardEnt = null; this.selected = null;
    this.order = ['seal', 'stair', 'chapter', 'ring', 'gilded', 'wildmoot', 'ironclad', 'tidecallers', 'graveborn', 'command', 'fire', 'gallery', 'armory', 'muster', 'archive', 'yard', 'gates'];
    this.zones = this.order.map((id) => app.ctx.zones.find((z) => z.id === id)).filter(Boolean);
    this.colorCache = new Map();
    this.buildBOM();
    this.buildMenu(); this.buildSettings(); this.buildHelp(); this.buildIntro();
    this.bindKeys();
  }

  /* ------------------------------------------------------------ bill of materials */
  blockColor(def) {
    let c = this.colorCache.get(def.id); if (c) return c;
    const name = def.tex.side, i = TILE_INDEX[name]; let col = '#888';
    if (i !== undefined) {
      const ctx2 = this.app.atlas.canvas.getContext('2d'); const d = ctx2.getImageData((i % COLS) * TILE, Math.floor(i / COLS) * TILE, TILE, TILE).data;
      let r = 0, g = 0, b = 0, n = 0; for (let k = 0; k < d.length; k += 4) if (d[k + 3] > 100) { r += d[k]; g += d[k + 1]; b += d[k + 2]; n++; }
      if (n) col = `rgb(${Math.round(r / n)},${Math.round(g / n)},${Math.round(b / n)})`;
    }
    this.colorCache.set(def.id, col); return col;
  }
  buildBOM() {
    const w = this.app.world, zn = w.zoneNames;
    for (const z of this.app.ctx.zones) {
      const idxs = (z.bom || [z.id]).map((n) => zn.indexOf(n)).filter((i) => i > 0);
      const [x1, z1, x2, z2] = z.rect, [y1, y2] = z.y;
      const counts = new Map(); let total = 0;
      for (let y = Math.max(WY0, y1); y <= Math.min(WY1, y2 + 6); y++) for (let zz = Math.max(WZ0, z1); zz <= Math.min(WZ1, z2); zz++) for (let x = Math.max(WX0, x1); x <= Math.min(WX1, x2); x++) {
        const i = (y - WY0) * SX * SZ + (zz - WZ0) * SX + (x - WX0); const id = w.ids[i]; if (!id || !idxs.includes(w.zoneOf[i])) continue;
        const d = BLOCKS[id]; if (d.shape === 'air') continue; counts.set(id, (counts.get(id) || 0) + 1); total++;
      }
      // merge by vanilla name
      const merged = new Map();
      for (const [id, n] of counts) { const d = BLOCKS[id]; const k = d.mc; const m = merged.get(k) || { mc: k, n: 0, id }; m.n += n; if (counts.get(m.id) < n) m.id = id; merged.set(k, m); }
      z.bomList = [...merged.values()].sort((a, b) => b.n - a.n); z.bomTotal = total;
    }
  }

  /* ------------------------------------------------------------ intro + settings */
  buildIntro() {
    const t = $('#introTimes'); t.innerHTML = '';
    for (const k of PRESET_ORDER) { const b = el('button', k === this.app.env.name ? 'on' : '', PRESETS[k].label); b.dataset.t = k; b.onclick = () => { this.app.setTime(k); [...t.children].forEach((c) => c.classList.toggle('on', c === b)); }; t.appendChild(b); }
    $('#enter').onclick = () => this.enter();
  }
  enter() { $('#intro').classList.add('hidden'); $('#hud').classList.remove('hidden'); this.app.requestLock(); this.setPaused(false); try { this.app.audio.start(); this.soundBtn && (this.soundBtn.classList.add('on'), this.soundBtn.textContent = 'On'); } catch (e) { /* no audio */ } }
  buildSettings() {
    const A = this.app, root = $('#tab-settings'); root.innerHTML = ''; const box = el('div', 'set'); root.appendChild(box);
    const row = (label, ctrl) => { const r = el('div', 'row'); r.appendChild(el('label', '', label)); r.appendChild(ctrl); box.appendChild(r); return r; };
    const tbox = el('div', 'btns'); for (const k of PRESET_ORDER) { const b = el('button', k === A.env.name ? 'on' : '', PRESETS[k].label); b.onclick = () => { A.setTime(k); [...tbox.children].forEach((c) => c.classList.toggle('on', c === b)); }; tbox.appendChild(b); } this.timeBtns = tbox;
    row('Time of day', tbox);
    const slider = (min, max, step, val, f) => { const s = el('input'); s.type = 'range'; s.min = min; s.max = max; s.step = step; s.value = val; s.oninput = () => f(+s.value); return s; };
    row('Field of view', slider(55, 100, 1, 74, (v) => { A.player.fovBase = v; }));
    row('Mouse sensitivity', slider(0.0008, 0.005, 0.0001, 0.0022, (v) => { A.player.sens = v; }));
    row('Render distance fog', slider(0, 0.012, 0.0005, 0.0048, (v) => { A.env.uniforms.uFogD.value = v; }));
    const tog = (label, init, f) => { const b = el('button', init ? 'on' : '', init ? 'On' : 'Off'); b.onclick = () => { const on = !b.classList.contains('on'); b.classList.toggle('on', on); b.textContent = on ? 'On' : 'Off'; f(on); }; row(label, b); return b; };
    this.soundBtn = tog('Ambient sound (K)', false, (v) => { v ? A.audio.start() : A.audio.stop(); });
    row('Sound volume', slider(0, 1, 0.05, 0.5, (v) => A.audio.setVolume(v)));
    tog('Name labels', true, (v) => { A.entities.labelsOn = v; });
    tog('Head bob', true, (v) => { A.player.enableBob = v; });
    tog('Bright (builder lighting)', false, (v) => A.setBright(v));
    const u = el('div', 'btns'); for (const [k, l] of [['mixed', 'Mixed (shows both)'], ['all', 'All unlocked'], ['locked', 'All locked']]) { const b = el('button', k === 'mixed' ? 'on' : '', l); b.onclick = () => { A.entities.setUnlockMode(k); [...u.children].forEach((c) => c.classList.toggle('on', c === b)); }; u.appendChild(b); }
    row('Troop displays', u);
    const b1 = el('button', '', 'Save screenshot (P)'); b1.onclick = () => A.screenshot(); row('Screenshot', b1);
  }
  buildHelp() {
    const g = el('div', 'helpgrid');
    const items = [['W A S D / arrows', 'Walk'], ['Shift', 'Sprint'], ['Space', 'Jump · double-tap to toggle flight'], ['C / Ctrl', 'Crouch · fly down'], ['E', 'Talk to the NPC or inspect the troop you are looking at'], ['Click', 'Copy the coordinates of the block under the crosshair'], ['Tab / M', 'Blueprint map (top-down, with grid and zones)'], ['Esc', 'Tour and builder\'s notes'], ['1–9, 0', 'Jump to a tour stop (Seal, Stair, Ring, Gilded, Wildmoot, Ironclad, Tide, Graveborn, Command, Gates)'], ['T', 'Cycle time of day (dusk, golden hour, night, dawn, noon)'], ['B', 'Bright mode — flat lighting, handy for counting blocks'], ['U', 'Cycle troop display state: mixed / all unlocked / all locked'], ['L', 'Toggle name labels'], ['K', 'Ambient sound on / off (wind, fires, footsteps, a distant war horn)'], ['F', 'Toggle flight'], ['H', 'Hide / show the HUD'], ['P', 'Save a screenshot']];
    for (const [k, v] of items) g.appendChild(el('div', '', `<kbd>${k}</kbd> ${v}`));
    $('#tab-help').appendChild(g);
  }

  /* ------------------------------------------------------------ tour menu */
  buildMenu() {
    const list = $('#zoneList'); list.innerHTML = '';
    for (const z of this.zones) {
      const d = el('div', 'zi'); d.dataset.id = z.id;
      d.innerHTML = `<div class="sw" style="background:${z.color}"></div><div><div class="zn">${z.name}</div><div class="zk">${z.house ? HOUSE_NAMES[z.house] + ' · House tent' : (z.npc ? 'NPC · ' + this.app.NPCS[z.npc].name : (z.what.split('.')[0].slice(0, 46)))}</div></div>`;
      d.onclick = () => this.select(z.id); list.appendChild(d);
    }
    document.querySelectorAll('.menu-head .tabs button').forEach((b) => { b.onclick = () => { document.querySelectorAll('.menu-head .tabs button').forEach((x) => x.classList.toggle('on', x === b)); for (const t of ['tour', 'settings', 'help']) $('#tab-' + t).classList.toggle('hidden', t !== b.dataset.tab); }; });
    $('#resume').onclick = () => this.closeMenu(true);
    this.select(this.zones[0].id);
  }
  select(id) {
    const z = this.zones.find((q) => q.id === id) || this.app.ctx.zones.find((q) => q.id === id); if (!z) return; this.selected = z;
    document.querySelectorAll('#zoneList .zi').forEach((d) => d.classList.toggle('on', d.dataset.id === id));
    const [x1, z1, x2, z2] = z.rect; const D = $('#zoneDetail');
    const tags = [`${x2 - x1 + 1} × ${z2 - z1 + 1} blocks`, `x ${x1}…${x2}`, `z ${z1}…${z2}`, `y ${dispY(z.y[0])}…${dispY(z.y[1])}`];
    if (z.house) tags.unshift(HOUSE_NAMES[z.house] + ' tent'); if (z.npc) tags.unshift('NPC: ' + this.app.NPCS[z.npc].name);
    const bom = (z.bomList || []).slice(0, 22);
    D.innerHTML = `<h2>${z.name}</h2><div>${tags.map((t) => `<span class="tag">${t}</span>`).join('')}</div>
      <div class="actions"><button id="tp">Teleport here</button><button id="fly">Overview (fly)</button></div>
      <h4>What it is in the game</h4><p>${z.what}</p>
      <h4>Builder's notes</h4><ul>${(z.builder || []).map((b) => `<li>${b}</li>`).join('')}</ul>
      <h4>Bill of materials · ${z.bomTotal || 0} blocks placed here · top ${bom.length}</h4>
      <div class="bom">${bom.map((b) => `<div><i style="background:${this.blockColor(BLOCKS[b.id])}"></i><span>${b.mc}</span><b>${b.n}</b></div>`).join('')}</div>
      <p style="font-size:12px;color:#8a8a94;margin-top:16px">Coordinates are Minecraft-style: the Lower Camp floor is y 64, the House Terrace is y 70 and the Command Hill is y 76. Block names are the nearest vanilla block; counts come from this model.</p>`;
    $('#tp').onclick = () => { this.app.tpZone(z); this.closeMenu(true); };
    $('#fly').onclick = () => { this.app.overview(z); this.closeMenu(true); };
  }

  /* ------------------------------------------------------------ pause / lock handling */
  setPaused(p) { this.paused = p; this.app.player.frozen = p; if (p) this.app.player.keys.clear(); }
  openMenu() { if (this.menuOpen) return; this.menuOpen = true; $('#menu').classList.remove('hidden'); this.setPaused(true); this.app.releaseLock(); }
  closeMenu(lock) { this.menuOpen = false; $('#menu').classList.add('hidden'); this.setPaused(false); if (lock) this.app.requestLock(); }
  toggleBlueprint() {
    this.bpOpen = !this.bpOpen; $('#bp').classList.toggle('hidden', !this.bpOpen);
    if (this.bpOpen) { this.setPaused(true); this.app.releaseLock(); this.drawBlueprint(); } else { this.setPaused(false); this.app.requestLock(); }
  }
  toast(msg) { const t = $('#toast'); t.textContent = msg; t.classList.add('show'); clearTimeout(this._tt); this._tt = setTimeout(() => t.classList.remove('show'), 2200); }

  bindKeys() {
    const A = this.app;
    addEventListener('keydown', (e) => {
      if (['Tab', 'Space', 'ArrowUp', 'ArrowDown'].includes(e.code) || (e.code === 'KeyF' && false)) e.preventDefault();
      if (!$('#intro').classList.contains('hidden')) { if (e.code === 'Enter') this.enter(); return; }
      if (e.code === 'Tab' || e.code === 'KeyM') { e.preventDefault(); if (!this.menuOpen) this.toggleBlueprint(); return; }
      if (e.code === 'Escape') { if (this.bpOpen) this.toggleBlueprint(); else if (this.menuOpen) this.closeMenu(true); else this.openMenu(); return; }
      if (this.paused) return;
      A.player.press(e.code);
      switch (e.code) {
        case 'KeyE': this.interact(); break;
        case 'KeyT': A.setTime(PRESET_ORDER[(PRESET_ORDER.indexOf(A.env.name) + 1) % PRESET_ORDER.length]); this.toast('Time of day: ' + PRESETS[A.env.name].label); break;
        case 'KeyB': A.setBright(!(A.env.uniforms.uBright.value > 0)); this.toast(A.env.uniforms.uBright.value > 0 ? 'Bright mode on' : 'Bright mode off'); break;
        case 'KeyU': { const m = ['mixed', 'all', 'locked']; A.entities.setUnlockMode(m[(m.indexOf(A.entities.unlockMode) + 1) % 3]); this.toast('Troop displays: ' + A.entities.unlockMode); break; }
        case 'KeyL': A.entities.labelsOn = !A.entities.labelsOn; break;
        case 'KeyK': { const on = !A.audio.on; on ? A.audio.start() : A.audio.stop(); this.soundBtn && (this.soundBtn.classList.toggle('on', on), this.soundBtn.textContent = on ? 'On' : 'Off'); this.toast('Ambient sound ' + (on ? 'on' : 'off')); break; }
        case 'KeyF': A.player.flying = !A.player.flying; A.player.vel.y = 0; this.toast(A.player.flying ? 'Flying' : 'Walking'); break;
        case 'KeyH': this.hidden = !this.hidden; $('#hud').style.opacity = this.hidden ? 0 : 1; break;
        case 'KeyP': A.screenshot(); break;
        default: if (/^Digit[0-9]$/.test(e.code)) { const n = (+e.code.slice(5) + 9) % 10; const tour = ['seal', 'stair', 'ring', 'gilded', 'wildmoot', 'ironclad', 'tidecallers', 'graveborn', 'command', 'gates']; const z = this.zones.find((q) => q.id === tour[n]); if (z) { A.tpZone(z); this.toast('Teleported: ' + z.name); } }
      }
    });
    addEventListener('keyup', (e) => A.player.release(e.code));
    addEventListener('blur', () => A.player.keys.clear());
    A.canvas.addEventListener('click', () => {
      if (this.paused) { if (!this.menuOpen && !this.bpOpen && $('#intro').classList.contains('hidden')) { this.setPaused(false); A.requestLock(); } return; }
      if (!A.locked) { A.requestLock(); return; }
      if (this.blockHit) { const h = this.blockHit; const txt = `${h.x} ${dispY(h.y)} ${h.z}`; navigator.clipboard?.writeText(txt).catch(() => {}); this.toast(`Copied ${txt} · ${BLOCKS[h.id].mc}`); }
    });
    document.addEventListener('pointerlockchange', () => { A.locked = document.pointerLockElement === A.canvas; if (!A.locked && !this.paused && $('#intro').classList.contains('hidden')) this.openMenu(); });
    $('#bpc').addEventListener('click', (e) => this.bpClick(e, false)); $('#bpc').addEventListener('dblclick', (e) => this.bpClick(e, true));
  }

  /* ------------------------------------------------------------ interaction + per-frame HUD */
  interact() {
    const e = this.cardEnt; if (!e) return;
    const info = e.info || e.pickInfo;
    if (e.kind === 'npc' || e.kind === 'class' || (e.kind === 'prop' && info)) {
      if (this.dialogEnt === e) { e.lineIdx = (e.lineIdx + 1) % Math.max(1, (info.lines || []).length || 1); } else { this.dialogEnt = e; e.lineIdx = 0; }
      this.renderDialog(e, info);
    } else if (e.kind === 'troop') { this.toast(`${e.data.name}: ${e.data.ability}`); }
  }
  renderDialog(e, info) {
    const d = $('#dialog'); d.classList.remove('hidden');
    const lines = info.lines || [];
    d.innerHTML = `<div class="who">${info.name} <span style="opacity:.6;letter-spacing:.06em;text-transform:none;font:12px Lato">· ${info.role}</span></div>${lines.length ? `<div class="say">“${lines[e.lineIdx % lines.length]}”</div>` : ''}<div class="fn"><b>In the game:</b> ${info.desc}</div><div class="nx">${lines.length > 1 ? 'E · next line · ' : ''}walk away to close</div>`;
  }
  updateHUD(dt, t) {
    const A = this.app, p = A.player;
    // coords + block target
    const fwd = p.forwardVec(); const cam = A.camera.position;
    const hit = raycastBlocks(A.world, cam.x, cam.y, cam.z, fwd.x, fwd.y, fwd.z, 9);
    this.blockHit = hit;
    const yaw = ((-THREE.MathUtils.radToDeg(p.yaw) % 360) + 360) % 360; const dir = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'][Math.round(yaw / 45) % 8];
    $('#coords').textContent = `XYZ ${p.pos.x.toFixed(1)} / ${dispY(p.pos.y).toFixed(1)} / ${p.pos.z.toFixed(1)}   facing ${dir}${p.flying ? '   · flying' : ''}`;
    $('#blockinfo').textContent = hit ? `${BLOCKS[hit.id].mc}  at  ${hit.x} ${dispY(hit.y)} ${hit.z}` : '';
    // zone banner
    let best = null, ba = 1e9;
    for (const z of this.app.ctx.zones) { const [x1, z1, x2, z2] = z.rect; if (p.pos.x >= x1 && p.pos.x <= x2 && p.pos.z >= z1 && p.pos.z <= z2 && p.pos.y >= z.y[0] - 3 && p.pos.y <= z.y[1] + 4) { const a = (x2 - x1) * (z2 - z1); if (a < ba) { ba = a; best = z; } } }
    if (best !== this.zone) {
      this.zone = best;
      if (best) { const b = $('#zonebanner'); $('.zt', b).textContent = best.name; $('.zs', b).textContent = best.house ? HOUSE_NAMES[best.house] + ' recruit tent' : best.npc ? this.app.NPCS[best.npc].role : best.what.split('.')[0].slice(0, 80); b.classList.add('show'); clearTimeout(this._zt); this._zt = setTimeout(() => b.classList.remove('show'), 3800); }
    }
    // aimed entity card
    const e = A.entities.pick(A.camera);
    if (e !== this.cardEnt) { this.cardEnt = e; this.renderCard(e); if (this.dialogEnt && this.dialogEnt !== e) { const dd = A.camera.position.distanceTo(this.dialogEnt.pos); if (dd > 6) { this.dialogEnt = null; $('#dialog').classList.add('hidden'); } } }
    if (this.dialogEnt && A.camera.position.distanceTo(this.dialogEnt.pos) > 8) { this.dialogEnt = null; $('#dialog').classList.add('hidden'); }
  }
  renderCard(e) {
    const c = $('#card'); if (!e) { c.classList.add('hidden'); return; } c.classList.remove('hidden');
    if (e.kind === 'troop') {
      const d = e.data, r = RARITY[d.r]; const st = e.lockedNow ? 'LOCKED · unlock with the Roll' : '★'.repeat(e.stars) + '☆'.repeat(3 - e.stars) + ' · owned';
      c.style.borderColor = r[1];
      c.innerHTML = `<h3>${d.name}</h3><div class="sub" style="color:${r[1]}">${HOUSE_NAMES[d.house]} · ${r[0]} · ${d.role}</div>
      <table><tr><td>Base mob</td><td>${d.mob}</td></tr><tr><td>Health</td><td>${d.hp}</td></tr><tr><td>Damage</td><td>${d.dmg || '—'}</td></tr><tr><td>Range</td><td>${d.range} blocks</td></tr><tr><td>Attack cooldown</td><td>${d.cd} ticks</td></tr><tr><td>Barracks cost / training</td><td>${d.cost} gold · ${(d.train / 20).toFixed(1)} s</td></tr></table>
      <div class="ab">${d.ability}</div><div class="prompt">${st}${e.legendary ? ' · Legendary, spent from a Pact' : ''}</div>`;
    } else if (e.kind === 'npc' || e.kind === 'class' || e.pickInfo) {
      const i = e.info || e.pickInfo; c.style.borderColor = '#6a5a3a';
      c.innerHTML = `<h3>${i.name}</h3><div class="sub" style="color:#c8a56b">${i.role}</div><div class="desc">${i.desc}</div><div class="prompt"><kbd>E</kbd> ${i.lines && i.lines.length ? 'talk' : 'read more'}</div>`;
    }
  }

  /* ------------------------------------------------------------ blueprint */
  drawBlueprint() {
    const A = this.app, w = A.world; const S = 4, cv = $('#bpc'); const W = SX * S, H = SZ * S;
    if (!this.bpBase) {
      const off = document.createElement('canvas'); off.width = SX; off.height = SZ; const oc = off.getContext('2d'); const img = oc.createImageData(SX, SZ);
      const tops = new Int16Array(SX * SZ), cols = new Map();
      for (let z = 0; z < SZ; z++) for (let x = 0; x < SX; x++) {
        let y = WY1; for (; y >= WY0; y--) { const id = w.ids[(y - WY0) * SX * SZ + z * SX + x]; if (id && BLOCKS[id].shape !== 'cross' && BLOCKS[id].shape !== 'air' && !BLOCKS[id].cutout) break; }
        const id = w.ids[(y - WY0) * SX * SZ + z * SX + x]; tops[z * SX + x] = y;
        let c = cols.get(id); if (!c) { const t = this.blockColor(BLOCKS[id] || BLOCKS[1]); const m = /(\d+),(\d+),(\d+)/.exec(t); c = m ? [+m[1], +m[2], +m[3]] : [120, 120, 120]; cols.set(id, c); }
        const nw = z > 0 && x > 0 ? tops[(z - 1) * SX + x - 1] : y; const sh = Math.max(0.55, Math.min(1.35, 1 + (y - nw) * 0.09)); const hf = 0.8 + Math.min(1, Math.max(0, (y + 16) / 70)) * 0.35;
        const o = (z * SX + x) * 4; img.data[o] = Math.min(255, c[0] * sh * hf); img.data[o + 1] = Math.min(255, c[1] * sh * hf); img.data[o + 2] = Math.min(255, c[2] * sh * hf); img.data[o + 3] = 255;
      }
      oc.putImageData(img, 0, 0); this.bpBase = off;
    }
    cv.width = W; cv.height = H; const c = cv.getContext('2d'); c.imageSmoothingEnabled = false; c.drawImage(this.bpBase, 0, 0, W, H);
    const X = (x) => (x - WX0) * S, Z = (z) => (z - WZ0) * S;
    // grid
    c.lineWidth = 1; c.font = '10px Lato'; c.fillStyle = 'rgba(255,255,255,.75)';
    for (let x = Math.ceil(WX0 / 8) * 8; x <= WX1; x += 8) { c.strokeStyle = x % 32 === 0 ? 'rgba(255,255,255,.28)' : 'rgba(255,255,255,.1)'; c.beginPath(); c.moveTo(X(x), 0); c.lineTo(X(x), H); c.stroke(); if (x % 16 === 0) c.fillText(x, X(x) + 2, 10); }
    for (let z = Math.ceil(WZ0 / 8) * 8; z <= WZ1; z += 8) { c.strokeStyle = z % 32 === 0 ? 'rgba(255,255,255,.28)' : 'rgba(255,255,255,.1)'; c.beginPath(); c.moveTo(0, Z(z)); c.lineTo(W, Z(z)); c.stroke(); if (z % 16 === 0) c.fillText(z, 2, Z(z) - 2); }
    // zones
    this.bpZones = [];
    c.font = '700 11px Lato'; c.lineWidth = 2;
    for (const z of this.zones) {
      if (['chapter', 'stair', 'seal', 'ring'].includes(z.id)) continue;
      const [x1, z1, x2, z2] = z.rect; c.strokeStyle = z.color; c.fillStyle = z.color + '33'; c.fillRect(X(x1), Z(z1), (x2 - x1 + 1) * S, (z2 - z1 + 1) * S); c.strokeRect(X(x1), Z(z1), (x2 - x1 + 1) * S, (z2 - z1 + 1) * S);
      c.fillStyle = '#fff'; c.shadowColor = '#000'; c.shadowBlur = 4; c.textAlign = 'center'; c.fillText(z.name.split('·')[0].trim().replace('The ', ''), (X(x1) + X(x2 + 1)) / 2, (Z(z1) + Z(z2 + 1)) / 2); c.shadowBlur = 0; c.textAlign = 'left';
      this.bpZones.push({ z, x: X(x1), y: Z(z1), w: (x2 - x1 + 1) * S, h: (z2 - z1 + 1) * S });
    }
    // markers
    for (const m of A.ctx.markers) { c.fillStyle = '#ffd24a'; c.beginPath(); c.arc(X(m.x), Z(m.z), 4, 0, 7); c.fill(); c.strokeStyle = '#000'; c.lineWidth = 1; c.stroke(); }
    for (const n of A.ctx.npcs) { if (n.kind === 'classfig') continue; c.fillStyle = '#7fffe0'; c.beginPath(); c.arc(X(n.x), Z(n.z), 3, 0, 7); c.fill(); }
    // terrace legend
    c.fillStyle = 'rgba(0,0,0,.6)'; c.fillRect(8, H - 74, 280, 66); c.fillStyle = '#eee'; c.font = '12px Lato'; c.fillText('Levels (Minecraft y):  Lower Camp 64 · House Terrace 70 · Command Hill 76', 14, H - 54); c.fillText('● yellow = landmark   ● teal = NPC   brightness = height', 14, H - 36); c.fillText('Spawn seal at x 0 / z ' + Math.round(A.ctx.spawn.z) + ' · north is up', 14, H - 18);
    // player
    const p = A.player.pos; c.save(); c.translate(X(p.x), Z(p.z)); c.rotate(-A.player.yaw); c.fillStyle = '#ff4a4a'; c.strokeStyle = '#fff'; c.lineWidth = 1.5; c.beginPath(); c.moveTo(0, -9); c.lineTo(6, 6); c.lineTo(0, 3); c.lineTo(-6, 6); c.closePath(); c.fill(); c.stroke(); c.restore();
    this.bpScale = S;
  }
  bpClick(e, dbl) {
    const cv = $('#bpc'), r = cv.getBoundingClientRect(); const px = (e.clientX - r.left) * cv.width / r.width, py = (e.clientY - r.top) * cv.height / r.height;
    const wx = px / this.bpScale + WX0, wz = py / this.bpScale + WZ0;
    const hit = this.bpZones.filter((q) => px >= q.x && px <= q.x + q.w && py >= q.y && py <= q.y + q.h).sort((a, b) => a.w * a.h - b.w * b.h)[0];
    const info = $('#bpinfo');
    const surf = this.app.world.topY(Math.floor(wx), Math.floor(wz));
    let html = `<h3>${hit ? hit.z.name : 'Open ground'}</h3><div>x ${Math.floor(wx)} · z ${Math.floor(wz)} · ground y ${dispY(surf) + 1}</div>`;
    if (hit) { html += `<p>${hit.z.what}</p><ul>${(hit.z.builder || []).map((b) => `<li>${b}</li>`).join('')}</ul>`; this.select(hit.z.id); }
    info.innerHTML = html;
    if (dbl) { this.toggleBlueprint(); const y = surf + 1; this.app.tp(Math.floor(wx) + 0.5, y, Math.floor(wz) + 0.5, 0, 0); this.toast(`Teleported to ${Math.floor(wx)}, ${dispY(y)}, ${Math.floor(wz)}`); }
  }
}
