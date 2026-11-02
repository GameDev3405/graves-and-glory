// Tool bar, top bar (designs, save status, versions, build) and the right-hand properties panel.
import * as api from './api.js';
import {
  state, commit, endDrag, undo, redo, canUndo, canRedo, select, setTool, notify, findObj, selectedObj,
  onChange, onStatus, onLoad, onNotice, flush, loadDesign, createDesign, freshId, listDesigns, say, getStatus,
} from './app.js';
import { TOOL_ORDER } from './tools.js';

const $ = (id) => document.getElementById(id);

function el(tag, cls, text) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (text !== undefined) e.textContent = text;
  return e;
}

const TOOL_LABEL = { hill: 'Hill', river: 'River', road: 'Road', point: 'Point', spawn: 'Spawn', select: 'Select' };
const FIELDS = {
  hill: [
    { k: 'height', label: 'Height', min: -40, max: 120, step: 1, int: true },
    { k: 'steepness', label: 'Steepness', min: 0, max: 1, step: 0.05 },
  ],
  river: [
    { k: 'width', label: 'Width', min: 1, max: 64, step: 1 },
    { k: 'depth', label: 'Depth', min: 1, max: 12, step: 1 },
  ],
  road: [{ k: 'width', label: 'Width', min: 1, max: 16, step: 1 }],
  point: [{ k: 'radius', label: 'Radius', min: 2, max: 32, step: 1 }],
  spawn: [],
};

// ---------- tool bar ----------

let toolButtons = {};
function buildToolbar() {
  const bar = $('tools');
  TOOL_ORDER.forEach((name, i) => {
    const b = el('button', 'tool', TOOL_LABEL[name]);
    b.appendChild(el('small', '', String(i + 1)));
    b.title = TOOL_LABEL[name] + ' (key ' + (i + 1) + ')';
    b.addEventListener('click', () => { setTool(name); b.blur(); });
    bar.appendChild(b);
    toolButtons[name] = b;
  });
}

// ---------- top bar ----------

let sel, statusEl, undoBtn, redoBtn, verName;

function buildTopbar() {
  const bar = $('topbar');
  bar.appendChild(el('span', 'brand', 'Map engine'));
  sel = el('select');
  sel.title = 'Design';
  sel.addEventListener('change', () => { loadDesign(sel.value); sel.blur(); });
  bar.appendChild(sel);
  const nb = el('button', '', 'New');
  nb.title = 'Start a blank design';
  nb.addEventListener('click', async () => { const id = await freshId(); await createDesign(id, id === 'untitled' ? 'Untitled' : id); fillDesigns(); nb.blur(); });
  bar.appendChild(nb);
  undoBtn = el('button', '', 'Undo');
  undoBtn.addEventListener('click', () => { undo(); undoBtn.blur(); });
  redoBtn = el('button', '', 'Redo');
  redoBtn.addEventListener('click', () => { redo(); redoBtn.blur(); });
  bar.append(undoBtn, redoBtn);
  statusEl = el('span');
  statusEl.id = 'save-status';
  bar.appendChild(statusEl);
  bar.appendChild(el('span', 'sep'));
  verName = el('input');
  verName.id = 'ver-name';
  verName.placeholder = 'version name';
  verName.maxLength = 40;
  const sv = el('button', '', 'Save version');
  sv.addEventListener('click', saveVersion);
  verName.addEventListener('keydown', (ev) => { if (ev.key === 'Enter') saveVersion(); });
  const vb = el('button', '', 'Versions');
  vb.addEventListener('click', () => { toggle('versions-pop', showVersions); vb.blur(); });
  const bb = el('button', '', 'Build');
  bb.addEventListener('click', async () => {
    if (bb.disabled) return;
    bb.disabled = true;
    bb.blur();
    try { await runBuild(); } finally { bb.disabled = false; }
  });
  bar.append(verName, sv, vb, bb);
}

async function fillDesigns() {
  const rows = await listDesigns();
  sel.textContent = '';
  for (const r of rows) {
    const o = el('option', '', r.name === r.id ? r.id : `${r.name} (${r.id})`);
    o.value = r.id;
    sel.appendChild(o);
  }
  if (state.design) sel.value = state.design.id;
}

function renderStatus({ status, errors }) {
  statusEl.className = status;
  statusEl.textContent = { saved: 'Saved', unsaved: 'Unsaved...', saving: 'Saving...', error: 'Save failed' }[status];
  const bar = $('errbar');
  bar.textContent = '';
  if (errors && errors.length) {
    bar.appendChild(el('strong', '', 'The server refused this design. Your local edits are kept; fix these and keep editing:'));
    const ul = el('ul');
    errors.forEach((m) => ul.appendChild(el('li', '', m)));
    bar.appendChild(ul);
    bar.hidden = false;
  } else {
    bar.hidden = true;
  }
}

let noticeTimer = null;
function showNotice(msg) {
  const n = $('notice');
  n.textContent = msg;
  n.hidden = false;
  clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => { n.hidden = true; }, 3500);
}

function toggle(id, fill) {
  const pop = $(id);
  const open = pop.hidden;
  $('issues').hidden = true;
  $('versions-pop').hidden = true;
  if (open) { pop.hidden = false; fill(); }
}

// ---------- versions ----------

async function saveVersion() {
  const name = verName.value.trim();
  if (!/^[A-Za-z0-9 _-]{1,40}$/.test(name)) { say('Version names use 1-40 letters, digits, spaces, _ or -.'); return; }
  await flush();
  const r = await api.saveVersion(state.design.id, name);
  if (!r.ok) { say((r.data && r.data.errors && r.data.errors[0]) || 'could not save the version'); return; }
  verName.value = '';
  say('Saved version "' + name + '"');
  if (!$('versions-pop').hidden) showVersions();
}

async function showVersions() {
  const pop = $('versions-pop');
  pop.textContent = '';
  pop.appendChild(el('h3', '', 'Saved versions'));
  const r = await api.listVersions(state.design.id);
  const rows = r.ok ? r.data : [];
  if (!rows.length) pop.appendChild(el('div', 'dim', 'None yet. Type a name and press Save version.'));
  for (const v of rows) {
    const row = el('div', 'vrow');
    row.appendChild(el('span', '', v.name));
    row.appendChild(el('span', 'dim', new Date(v.mtime * 1000).toLocaleString()));
    const rb = el('button', '', 'Restore');
    rb.addEventListener('click', async () => {
      await flush();
      // Restoring overwrites the design on disk, so keep the current state as a version first.
      if (v.name !== 'before-restore') await api.saveVersion(state.design.id, 'before-restore');
      const rr = await api.restoreVersion(state.design.id, v.name);
      if (!rr.ok) { say((rr.data && rr.data.errors && rr.data.errors[0]) || 'restore failed'); return; }
      await loadDesign(state.design.id);
      say('Restored "' + v.name + '" (previous state kept as "before-restore")');
      $('versions-pop').hidden = true;
    });
    row.appendChild(rb);
    pop.appendChild(row);
  }
}

// ---------- build ----------

async function runBuild() {
  const pop = $('issues');
  $('versions-pop').hidden = true;
  pop.hidden = false;
  pop.textContent = '';
  pop.appendChild(el('h3', '', 'Building...'));
  await flush();
  const r = await api.build(state.design.id);
  pop.textContent = '';
  if (!r.ok) {
    pop.appendChild(el('h3', '', 'Build refused'));
    ((r.data && r.data.errors) || ['build failed (' + r.status + ')']).forEach((m) => pop.appendChild(el('div', 'issue', m)));
    return;
  }
  const issues = r.data.issues || [];
  pop.appendChild(el('h3', '', issues.length ? `Built, ${issues.length} issue${issues.length === 1 ? '' : 's'}` : 'Built, no issues'));
  if (!issues.length) pop.appendChild(el('div', 'dim', 'The checker found nothing to fix.'));
  for (const i of issues) {
    const row = el('div', 'issue');
    const code = el('code', '', i.code);
    row.append(code, document.createTextNode(' ' + i.message));
    if (i.object_id) row.title = 'Select ' + i.object_id;
    row.addEventListener('click', () => {
      if (i.object_id && findObj(state.design, i.object_id)) { setTool('select'); select(i.object_id); }
    });
    pop.appendChild(row);
  }
}

// ---------- right panel ----------

let shownKey = null, rows = {}, panelRoot, fieldsBox, listBox;

function buildPanel() {
  panelRoot = $('panel');
  fieldsBox = el('div');
  listBox = el('div');
  panelRoot.append(fieldsBox, listBox);
}

function numRow(o, f) {
  const row = el('div', 'row');
  row.appendChild(el('label', '', f.label));
  const num = el('input');
  num.type = 'number'; num.min = f.min; num.max = f.max; num.step = f.step;
  const rng = el('input');
  rng.type = 'range'; rng.min = f.min; rng.max = f.max; rng.step = f.step;
  const set = (v, drag) => {
    v = Math.min(f.max, Math.max(f.min, v));
    v = f.int ? Math.round(v) : Math.round(v * 100) / 100;
    commit((d) => { findObj(d, o.id)[f.k] = v; }, { drag });
  };
  rng.addEventListener('input', () => set(+rng.value, true));
  rng.addEventListener('change', () => endDrag());
  num.addEventListener('change', () => {
    if (num.value === '' || Number.isNaN(+num.value)) { refreshFields(); return; }
    set(+num.value, false);
    refreshFields();
  });
  row.append(num, rng);
  rows[f.k] = { num, rng };
  return row;
}

function rebuildFields(o) {
  fieldsBox.textContent = '';
  rows = {};
  if (!o) {
    fieldsBox.appendChild(el('h2', '', 'Nothing selected'));
    fieldsBox.appendChild(el('div', 'meta', 'Pick a tool on the left and click the map. Select, then click a shape to edit it. Drag to move it, or drag a vertex. Delete removes it. Space-drag or middle-drag pans, the wheel zooms.'));
    return;
  }
  fieldsBox.appendChild(el('h2', '', o.kind[0].toUpperCase() + o.kind.slice(1) + ' ' + o.id));
  fieldsBox.appendChild(el('div', 'meta', `by ${o.by}, ${o.state}`));
  if (o.kind === 'point') {
    const row = el('div', 'row two');
    row.appendChild(el('label', '', 'Name'));
    const t = el('input');
    t.type = 'text'; t.maxLength = 20;
    t.addEventListener('change', () => {
      const v = t.value.trim();
      const clash = state.design.objects.some((p) => p.kind === 'point' && p.id !== o.id && p.name === v);
      if (!v || clash) { say(clash ? 'Another point already has that name.' : 'A point needs a name.'); refreshFields(); return; }
      commit((d) => { findObj(d, o.id).name = v; });
    });
    row.appendChild(t);
    rows.name = { text: t };
    fieldsBox.appendChild(row);
  }
  if (o.kind === 'spawn') {
    const row = el('div', 'row two');
    row.appendChild(el('label', '', 'Team'));
    const s = el('select');
    ['dawn', 'dusk'].forEach((t) => { const op = el('option', '', t); op.value = t; s.appendChild(op); });
    s.addEventListener('change', () => { commit((d) => { findObj(d, o.id).team = s.value; }); s.blur(); });
    row.appendChild(s);
    rows.team = { select: s };
    fieldsBox.appendChild(row);
  }
  for (const f of FIELDS[o.kind] || []) fieldsBox.appendChild(numRow(o, f));
  if (o.kind === 'zone') fieldsBox.appendChild(el('div', 'meta', 'palette ' + o.palette + ' (edit the file to change)'));
  const del = el('button', 'danger', 'Delete');
  del.addEventListener('click', () => { commit((d) => { d.objects = d.objects.filter((p) => p.id !== o.id); }); del.blur(); });
  fieldsBox.appendChild(del);
}

function refreshFields() {
  const o = selectedObj();
  const key = o ? o.id + ':' + o.kind : '';
  if (key !== shownKey) { rebuildFields(o); shownKey = key; }
  if (!o) return;
  for (const f of FIELDS[o.kind] || []) {
    const r = rows[f.k];
    if (!r) continue;
    if (document.activeElement !== r.num) r.num.value = o[f.k];
    r.rng.value = o[f.k];
  }
  if (rows.name && document.activeElement !== rows.name.text) rows.name.text.value = o.name || '';
  if (rows.team) rows.team.select.value = o.team;
}

function refreshList() {
  listBox.textContent = '';
  if (!state.design) return;
  listBox.appendChild(el('h3', '', `Objects (${state.design.objects.length})`));
  const ul = el('ul', 'objlist');
  for (const o of state.design.objects) {
    const li = el('li', o.id === state.selectedId ? 'sel' : '');
    li.append(el('span', 'k', o.kind), el('span', '', o.name ? `${o.id} (${o.name})` : o.id));
    li.addEventListener('click', () => { setTool('select'); select(o.id); });
    ul.appendChild(li);
  }
  listBox.appendChild(ul);
}

function refresh() {
  if (!state.design) return;
  for (const [name, b] of Object.entries(toolButtons)) b.classList.toggle('active', name === state.tool);
  undoBtn.disabled = !canUndo();
  redoBtn.disabled = !canRedo();
  refreshFields();
  refreshList();
}

export function initPanel() {
  buildToolbar();
  buildTopbar();
  buildPanel();
  onChange(refresh);
  onStatus(renderStatus);
  onNotice(showNotice);
  onLoad(() => { shownKey = null; fillDesigns(); refresh(); });
  renderStatus(getStatus());
}
