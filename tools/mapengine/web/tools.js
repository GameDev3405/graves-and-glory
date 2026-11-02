// Editing tools. Every tool is {down(state, x, z, ev), move(state, x, z, ev), up(state, x, z, ev), key(state, ev)}.
// x and z are map block coordinates (floats); nothing here knows about screens, so any view that can turn a
// pointer into ground (x, z) can drive them. key() returns true when it used the key.
// state.pxPerBlock (set by the showing view) converts the 6 px pick distance into blocks.
import { commit, endDrag, select, notify, newId, findObj, selectedObj } from './app.js';

export const TOOL_ORDER = ['hill', 'river', 'road', 'point', 'spawn', 'select'];
const PICK_PX = 6;
const NEW_BY = 'kort';
const NEW_STATE = 'accepted';

const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));
const snapPath = (d, x, z) => [clamp(Math.round(x), 0, d.width), clamp(Math.round(z), 0, d.depth)];
const snapPoint = (d, x, z) => [clamp(Math.round(x), 0, d.width - 1), clamp(Math.round(z), 0, d.depth - 1)];
const base = (d, kind) => ({ id: newId(d, kind), kind, by: NEW_BY, state: NEW_STATE });

// ---------- hill / river / road: click vertices, double-click or Enter to finish ----------

function pathTool(kind, minPts, field, defaults) {
  function finish(state) {
    const dr = state.draft;
    if (!dr || dr.pts.length < minPts) return false;
    state.draft = null;
    let id = null;
    commit((d) => {
      const o = Object.assign(base(d, kind), { [field]: dr.pts }, defaults);
      id = o.id;
      d.objects.push(o);
    });
    select(id);
    return true;
  }
  return {
    down(state, x, z, ev) {
      if (ev && ev.detail >= 2) { finish(state); return; } // second click of a double-click
      const p = snapPath(state.design, x, z);
      if (!state.draft) state.draft = { kind, pts: [] };
      const last = state.draft.pts[state.draft.pts.length - 1];
      if (!last || last[0] !== p[0] || last[1] !== p[1]) state.draft.pts.push(p);
      notify();
    },
    move() {}, // views draw the rubber band to the cursor themselves
    up() {},
    key(state, ev) {
      if (ev.key === 'Enter') return finish(state);
      if (ev.key === 'Escape' && state.draft) { state.draft = null; notify(); return true; }
      if (ev.key === 'Backspace' && state.draft) { state.draft.pts.pop(); if (!state.draft.pts.length) state.draft = null; notify(); return true; }
      return false;
    },
  };
}

// ---------- point / spawn: one click places ----------

function nextPointName(d) {
  const used = new Set(d.objects.filter((o) => o.kind === 'point').map((o) => o.name));
  for (let i = 0; i < 26; i++) { const n = String.fromCharCode(65 + i); if (!used.has(n)) return n; }
  let n = 27;
  while (used.has('P' + n)) n++;
  return 'P' + n;
}

const pointTool = {
  down(state, x, z) {
    let id = null;
    commit((d) => {
      const [px, pz] = snapPoint(d, x, z);
      const o = Object.assign(base(d, 'point'), { x: px, z: pz, name: nextPointName(d), radius: 14 });
      id = o.id;
      d.objects.push(o);
    });
    select(id);
  },
  move() {}, up() {}, key() { return false; },
};

const spawnTool = {
  down(state, x, z) {
    let id = null;
    commit((d) => {
      const [px, pz] = snapPoint(d, x, z);
      const n = (t) => d.objects.filter((o) => o.kind === 'spawn' && o.team === t).length;
      const team = n('dawn') <= n('dusk') ? 'dawn' : 'dusk'; // alternate; the panel can change it
      const o = Object.assign(base(d, 'spawn'), { x: px, z: pz, team });
      id = o.id;
      d.objects.push(o);
    });
    select(id);
  },
  move() {}, up() {}, key() { return false; },
};

// ---------- select: pick, drag, delete ----------

const SHAPE_KEY = { hill: 'outline', zone: 'outline', river: 'path', road: 'path' };
const PICK_ORDER = ['spawn', 'point', 'road', 'river', 'hill', 'zone']; // top first

function vertices(o) {
  const k = SHAPE_KEY[o.kind];
  return k ? o[k] : [[o.x, o.z]];
}

function distSeg(px, pz, a, b) {
  const dx = b[0] - a[0], dz = b[1] - a[1];
  const l2 = dx * dx + dz * dz;
  const t = l2 ? clamp(((px - a[0]) * dx + (pz - a[1]) * dz) / l2, 0, 1) : 0;
  return Math.hypot(px - (a[0] + t * dx), pz - (a[1] + t * dz));
}

function insidePoly(px, pz, pts) {
  let inside = false;
  for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
    const [ax, az] = pts[i], [bx, bz] = pts[j];
    if ((az > pz) !== (bz > pz) && px < ax + ((pz - az) / (bz - az)) * (bx - ax)) inside = !inside;
  }
  return inside;
}

function hits(o, x, z, tol) {
  switch (o.kind) {
    case 'spawn': return Math.hypot(x - o.x, z - o.z) <= Math.max(tol, 2);
    case 'point': return Math.hypot(x - o.x, z - o.z) <= Math.max(o.radius, tol);
    case 'road': case 'river': {
      const p = o.path, r = Math.max(o.width / 2, tol);
      for (let i = 0; i + 1 < p.length; i++) if (distSeg(x, z, p[i], p[i + 1]) <= r) return true;
      return false;
    }
    default: return insidePoly(x, z, o.outline);
  }
}

function pickTop(d, x, z, tol) {
  for (const kind of PICK_ORDER) {
    for (let i = d.objects.length - 1; i >= 0; i--) {
      const o = d.objects[i];
      if (o.kind === kind && hits(o, x, z, tol)) return o;
    }
  }
  return null;
}

function vertexAt(o, x, z, tol) {
  const vs = vertices(o);
  for (let i = 0; i < vs.length; i++) if (Math.hypot(x - vs[i][0], z - vs[i][1]) <= tol) return i;
  return -1;
}

let drag = null; // {id, vertex (-1 = whole object), x0, z0, orig}

function applyDrag(d, x, z) {
  const o = findObj(d, drag.id);
  if (!o) return;
  const orig = drag.orig;
  if (orig.kind === 'point' || orig.kind === 'spawn') {
    [o.x, o.z] = snapPoint(d, x - drag.x0 + orig.x, z - drag.z0 + orig.z);
    return;
  }
  const key = SHAPE_KEY[orig.kind];
  if (drag.vertex >= 0) {
    o[key][drag.vertex] = snapPath(d, x - drag.x0 + orig[key][drag.vertex][0], z - drag.z0 + orig[key][drag.vertex][1]);
    return;
  }
  // Whole shape: one shared integer offset, limited so the shape's bounding box stays on the canvas.
  const xs = orig[key].map((p) => p[0]), zs = orig[key].map((p) => p[1]);
  const dx = Math.round(clamp(x - drag.x0, -Math.min(...xs), d.width - Math.max(...xs)));
  const dz = Math.round(clamp(z - drag.z0, -Math.min(...zs), d.depth - Math.max(...zs)));
  o[key] = orig[key].map((p) => [p[0] + dx, p[1] + dz]);
}

const selectTool = {
  down(state, x, z) {
    const tol = PICK_PX / (state.pxPerBlock || 1);
    let o = selectedObj();
    let vertex = o ? vertexAt(o, x, z, tol) : -1;
    if (vertex < 0) {
      o = pickTop(state.design, x, z, tol);
      vertex = o ? vertexAt(o, x, z, tol) : -1;
    }
    if (!o) { drag = null; select(null); return; }
    const single = o.kind === 'point' || o.kind === 'spawn';
    drag = { id: o.id, vertex: single ? -1 : vertex, x0: x, z0: z, orig: JSON.parse(JSON.stringify(o)) };
    select(o.id);
  },
  move(state, x, z) {
    if (!drag) return;
    if (Math.round(x - drag.x0) === 0 && Math.round(z - drag.z0) === 0 && !drag.moved) return;
    drag.moved = true;
    commit((d) => applyDrag(d, x, z), { drag: true });
  },
  up() {
    if (!drag) return;
    drag = null;
    endDrag(); // the whole drag is one undo step
  },
  key(state, ev) {
    if ((ev.key === 'Delete' || ev.key === 'Backspace') && state.selectedId) {
      const id = state.selectedId;
      commit((d) => { d.objects = d.objects.filter((o) => o.id !== id); });
      return true;
    }
    if (ev.key === 'Escape' && state.selectedId) { select(null); return true; }
    return false;
  },
};

export const TOOLS = {
  hill: pathTool('hill', 3, 'outline', { height: 8, steepness: 0.5 }),
  river: pathTool('river', 2, 'path', { width: 8, depth: 2 }),
  road: pathTool('road', 2, 'path', { width: 5 }),
  point: pointTool,
  spawn: spawnTool,
  select: selectTool,
};
