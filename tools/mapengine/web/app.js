// App state, undo/redo, autosave, field refresh and view switching.
//
// Seam for Task 7 (walk view): import { state, commit, undo, redo, onFieldChange } from './app.js';
// register a view with registerView('walk', {show(), hide()}) and Tab will toggle to it. A walk view turns a
// pointer into ground (x, z) and drives TOOLS[state.tool].down/move/up(state, x, z, ev), after setting
// state.pxPerBlock to its approximate pixels per block at that spot (the select tool uses it for pick radii).
import * as api from './api.js';
import { TOOLS, TOOL_ORDER } from './tools.js';
import { initMap } from './map2d.js';
import { initPanel } from './panel.js';

export const state = {
  design: null,      // the design object being edited
  selectedId: null,
  tool: 'select',    // key of TOOLS
  field: null,       // latest decoded heightfield from the server (see api.getField), or null
  view: 'map',       // 'map' | 'walk'
  draft: null,       // in-progress hill/river/road: {kind, pts: [[x, z], ...]}
  pxPerBlock: 1,     // set by whichever view is showing
};

const UNDO_LIMIT = 200;
const SAVE_DELAY_MS = 1000;
const changeCbs = [], fieldCbs = [], statusCbs = [], loadCbs = [], noticeCbs = [];
const views = {};
const undoStack = [], redoStack = [];

export const onChange = (cb) => { changeCbs.push(cb); };      // design, selection, tool or view changed
export const onFieldChange = (cb) => { fieldCbs.push(cb); };  // a fresh heightfield arrived
export const onStatus = (cb) => { statusCbs.push(cb); };      // save status: {status, errors}
export const onLoad = (cb) => { loadCbs.push(cb); };          // a different design was loaded
export const onNotice = (cb) => { noticeCbs.push(cb); };
export const notify = () => changeCbs.forEach((cb) => cb());
export const say = (msg) => noticeCbs.forEach((cb) => cb(msg));

// ---------- design helpers ----------

export const findObj = (d, id) => d.objects.find((o) => o.id === id);
export const selectedObj = () => (state.design && state.selectedId ? findObj(state.design, state.selectedId) || null : null);

export function newId(d, kind) {
  let n = 0;
  for (const o of d.objects) {
    const m = o.id.startsWith(kind + '-') && /^\d+$/.test(o.id.slice(kind.length + 1)) ? +o.id.slice(kind.length + 1) : 0;
    n = Math.max(n, m);
  }
  return kind + '-' + (n + 1);
}

export function blankDesign(id, name) {
  return { format: 1, id, name, width: 700, depth: 500, baseY: 64, objects: [] };
}

// ---------- history ----------

let gesture = false, gestureBefore = null;
const snap = () => JSON.stringify(state.design);

function pushUndo(s) {
  undoStack.push(s);
  if (undoStack.length > UNDO_LIMIT) undoStack.shift();
  redoStack.length = 0;
}

function fixSelection() {
  if (state.selectedId && !findObj(state.design, state.selectedId)) state.selectedId = null;
}

// Apply mutator(design). One call is one undo step. With {drag:true} all calls until endDrag() are one step;
// the autosave and field refresh wait for endDrag().
export function commit(mutator, opts) {
  const drag = !!(opts && opts.drag);
  if (drag) {
    if (!gesture) { gesture = true; gestureBefore = snap(); pushUndo(gestureBefore); }
    mutator(state.design);
    fixSelection();
    notify();
    return;
  }
  endDrag();
  const before = snap();
  mutator(state.design);
  if (snap() === before) return;
  pushUndo(before);
  fixSelection();
  afterEdit();
  notify();
}

export function endDrag() {
  if (!gesture) return;
  gesture = false;
  if (snap() === gestureBefore) undoStack.pop(); else afterEdit();
  gestureBefore = null;
  notify();
}

function restoreSnap(s) {
  state.design = JSON.parse(s);
  fixSelection();
  afterEdit();
  notify();
}

export function undo() {
  if (gesture || !undoStack.length) return;
  redoStack.push(snap());
  restoreSnap(undoStack.pop());
}

export function redo() {
  if (gesture || !redoStack.length) return;
  undoStack.push(snap());
  restoreSnap(redoStack.pop());
}

export const canUndo = () => undoStack.length > 0;
export const canRedo = () => redoStack.length > 0;

export function select(id) {
  state.selectedId = id;
  notify();
}

export function setTool(name) {
  if (!TOOLS[name]) return;
  state.tool = name;
  state.draft = null;
  notify();
}

// ---------- autosave ----------

let dirty = false, timer = null, saving = null;
let saveState = { status: 'saved', errors: [] };

function setStatus(status, errors) {
  saveState = { status, errors: errors || [] };
  statusCbs.forEach((cb) => cb(saveState));
}
export const getStatus = () => saveState;

function afterEdit() {
  dirty = true;
  setStatus('unsaved');
  clearTimeout(timer);
  timer = setTimeout(() => { flush(); }, SAVE_DELAY_MS);
}

async function doSave() {
  dirty = false;
  setStatus('saving');
  const id = state.design.id;
  const res = await api.putDesign(id, state.design);
  if (state.design.id !== id) return; // another design was loaded meanwhile
  if (!res.ok) {
    dirty = true; // keep the local state; the next edit tries again
    setStatus('error', (res.data && res.data.errors) || ['save failed (' + res.status + ')']);
    return;
  }
  setStatus(dirty ? 'unsaved' : 'saved');
  refreshField();
}

// Save any pending edit now and wait for it.
export async function flush() {
  clearTimeout(timer);
  timer = null;
  if (saving) await saving;
  if (dirty && state.design) {
    saving = doSave();
    try { await saving; } finally { saving = null; }
  }
}

function saveOnExit() {
  if (!dirty || !state.design) return;
  dirty = false;
  api.putDesign(state.design.id, state.design, { keepalive: true });
}
window.addEventListener('pagehide', saveOnExit);
window.addEventListener('beforeunload', saveOnExit);

// ---------- field refresh (only the latest request is applied) ----------

let fieldSeq = 0;
export async function refreshField() {
  const mine = ++fieldSeq;
  const id = state.design.id;
  const res = await api.getField(id);
  if (mine !== fieldSeq || !res.ok || state.design.id !== id) return;
  state.field = res.data;
  fieldCbs.forEach((cb) => cb(state.field));
}

// ---------- loading designs ----------

export async function loadDesign(id) {
  await flush();
  const res = await api.getDesign(id);
  if (!res.ok) { say('could not open ' + id); return false; }
  state.design = res.data;
  state.selectedId = null;
  state.draft = null;
  state.field = null;
  undoStack.length = 0;
  redoStack.length = 0;
  gesture = false;
  dirty = false;
  setStatus('saved');
  try { history.replaceState(null, '', '#' + id); } catch (e) { /* ignore */ }
  loadCbs.forEach((cb) => cb());
  notify();
  refreshField();
  return true;
}

export async function createDesign(id, name) {
  await flush();
  const res = await api.putDesign(id, blankDesign(id, name));
  if (!res.ok) { say('could not create ' + id); return false; }
  return loadDesign(id);
}

export async function listDesigns() {
  const res = await api.listDesigns();
  return res.ok ? res.data : [];
}

export async function freshId() {
  const have = new Set((await listDesigns()).map((r) => r.id));
  if (!have.has('untitled')) return 'untitled';
  for (let n = 2; ; n++) if (!have.has('untitled-' + n)) return 'untitled-' + n;
}

// ---------- views (Tab) ----------

export function registerView(name, v) { views[name] = v; }

export function setView(name) {
  if (!views[name] || name === state.view) return;
  if (views[state.view]) views[state.view].hide();
  state.view = name;
  views[name].show();
  notify();
}

function toggleView() {
  const next = state.view === 'map' ? 'walk' : 'map';
  if (!views[next]) { say('The walk view is not installed yet.'); return; }
  setView(next);
}

// ---------- keyboard ----------

window.addEventListener('keydown', (ev) => {
  const t = ev.target;
  const typing = t && (t.tagName === 'TEXTAREA' || t.tagName === 'SELECT' ||
    (t.tagName === 'INPUT' && t.type !== 'range' && t.type !== 'checkbox'));
  if (typing || !state.design) return;
  if (ev.ctrlKey || ev.metaKey) {
    const k = ev.key.toLowerCase();
    if (k === 'z') { ev.preventDefault(); if (ev.shiftKey) redo(); else undo(); }
    else if (k === 'y') { ev.preventDefault(); redo(); }
    return;
  }
  if (ev.key === 'Tab') { ev.preventDefault(); toggleView(); return; }
  const digit = '123456'.indexOf(ev.key);
  if (digit >= 0 && !ev.altKey) { setTool(TOOL_ORDER[digit]); return; }
  if (TOOLS[state.tool].key(state, ev)) ev.preventDefault();
});

// ---------- boot ----------

async function boot() {
  initMap();
  initPanel();
  const rows = await listDesigns();
  const want = decodeURIComponent((location.hash || '').slice(1));
  let id = rows.find((r) => r.id === want) ? want : rows.length ? rows[0].id : null; // rows are newest first
  if (id) {
    await loadDesign(id);
  } else {
    await createDesign('untitled', 'Untitled');
  }
}

boot();
