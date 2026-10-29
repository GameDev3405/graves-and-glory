// Small reusable building helpers shared by every zone.
import { hash } from './ctx.js';

export const pick = (w, list) => { // list of [spec, weight]
  let t = 0; for (const [, k] of list) t += k;
  let r = w.rng() * t; for (const [s, k] of list) { if ((r -= k) <= 0) return s; }
  return list[0][0];
};
// paving with a weighted palette; hashed per cell so it is stable
export function pave(pen, x1, z1, x2, z2, y, palette) {
  for (let z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
    const r = hash(pen.mx(x, z), z * 7 + pen.mz(x, z), 31) * palette.reduce((a, p) => a + p[1], 0);
    let acc = 0, sp = palette[0][0];
    for (const [s, k] of palette) { acc += k; if (r <= acc) { sp = s; break; } }
    pen.set(x, y, z, sp);
  }
}
export const STONE_PAVE = [['stone_bricks', 56], ['mossy_stone_bricks', 12], ['cracked_stone_bricks', 10], ['cobblestone', 12], ['andesite', 10]];
export const DARK_PAVE = [['polished_deepslate', 40], ['deepslate_tiles', 30], ['deepslate_bricks', 20], ['polished_blackstone_bricks', 10]];
export const GRAVEL_PATH = [['gravel', 60], ['coarse_dirt', 25], ['cobblestone', 15]];

export function lampPost(pen, x, y, z, h = 4, spec = 'lantern') {
  pen.fill(x, y, z, x, y + h - 1, z, 'dark_oak_fence');
  pen.set(x, y + h, z, spec);
}
export function ironLamp(pen, x, y, z, h = 4, soul = false) {
  pen.fill(x, y, z, x, y + h - 1, z, 'polished_blackstone_wall');
  pen.set(x, y + h, z, 'polished_blackstone_slab');
  for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) pen.setIfAir(x + dx, y + h - 1, z + dz, soul ? 'soul_lantern' : 'lantern');
}
export function brazier(pen, x, y, z, soul = false) {
  // an iron-banded stone bowl with a campfire
  pen.set(x, y, z, 'polished_blackstone_bricks');
  pen.set(x, y + 1, z, soul ? 'soul_campfire' : 'campfire');
}
export function bigBrazier(pen, x, y, z, soul = false) {
  // post, a plus-shaped slab plate, the fire sitting on it, and a ring of four short wall posts as a cage
  pen.fill(x, y, z, x, y + 1, z, 'polished_blackstone_wall');
  pen.set(x, y + 2, z, soul ? 'soul_campfire' : 'campfire');
  for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) pen.set(x + dx, y + 2, z + dz, 'polished_blackstone_bricks_slab');
  for (const [dx, dz] of [[1, 1], [-1, 1], [1, -1], [-1, -1]]) pen.set(x + dx, y + 2, z + dz, 'polished_blackstone_wall');
}
export function pennantString(pen, x1, y1, z1, x2, y2, z2, colors) {
  // bunting: a chain of coloured carpets? Rendered as banners entities by the caller; here only the catenary of chains
  const n = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
  for (let i = 0; i <= n; i++) {
    const t = n === 0 ? 0 : i / n; const sag = Math.round(Math.sin(t * Math.PI) * 1.2);
    pen.set(Math.round(x1 + (x2 - x1) * t), y1 - sag, Math.round(z1 + (z2 - z1) * t), 'chain');
  }
}

/* ---------- trees ---------- */
export function spruce(w, x, y, z, h = 9, maxR = 3) {
  const P = w.pen(0, 0, 0);
  for (let i = 0; i < h; i++) P.set(x, y + i, z, 'spruce_log');
  for (let dy = 2; dy <= h + 1; dy++) {
    let r = Math.round((h + 1 - dy) * maxR / (h - 1) + (dy % 2 ? 0.4 : -0.3));
    r = Math.max(0, Math.min(maxR, r));
    if (dy === h + 1) r = 0; if (dy === h) r = Math.min(r, 1);
    for (let dz = -r; dz <= r; dz++) for (let dx = -r; dx <= r; dx++) {
      if (Math.abs(dx) + Math.abs(dz) > r + 1) continue;
      if (Math.abs(dx) === r && Math.abs(dz) === r && r > 1 && w.rng() < 0.6) continue;
      P.setIfAir(x + dx, y + dy, z + dz, 'spruce_leaves');
    }
  }
}
export function oak(w, x, y, z, h = 6, leaf = 'oak_leaves', log = 'oak_log', R = 3) {
  const P = w.pen(0, 0, 0);
  for (let i = 0; i < h; i++) P.set(x, y + i, z, log);
  const cy = y + h - 1;
  for (const [dx, dy, dz, r] of [[0, 1, 0, R], [R - 1, 0, 0, R - 1], [-(R - 1), 0, 1, R - 1], [0, 0, -(R - 1), R - 1], [1, -1, R - 1, R - 1]]) P.sphere(x + dx, cy + dy, z + dz, r, leaf, 0.12);
  P.set(x, cy + 1, z, log);
}
export function deadTree(w, x, y, z, h = 6) {
  const P = w.pen(0, 0, 0);
  for (let i = 0; i < h; i++) P.set(x, y + i, z, 'dark_oak_log');
  const dirs = [[1, 0], [-1, 0], [0, 1], [0, -1]];
  for (let b = 0; b < 3; b++) {
    const [dx, dz] = dirs[(b * 3 + Math.floor(w.rng() * 4)) % 4]; const by = y + 2 + b * 1 + Math.floor(w.rng() * 2); const len = 2 + Math.floor(w.rng() * 2);
    for (let i = 1; i <= len; i++) P.setIfAir(x + dx * i, by + (i > 1 ? 1 : 0), z + dz * i, 'dark_oak_log');
    P.setIfAir(x + dx * len, by + 2, z + dz * len, 'dark_oak_fence');
    P.setIfAir(x + dx * (len), by + 1, z + dz * len, 'cobweb');
  }
  P.set(x, y + h, z, 'dark_oak_fence'); P.set(x, y + h + 1, z, 'dark_oak_fence');
}
export function cherry(w, x, y, z) { oak(w, x, y, z, 5, 'cherry_leaves', 'birch_log', 3); }
export function azaleaBush(w, x, y, z) { const P = w.pen(0, 0, 0); P.setIfAir(x, y, z, 'azalea_leaves'); P.setIfAir(x, y + 1, z, w.rng() < 0.5 ? 'flowering_azalea_leaves' : 'azalea_leaves'); P.setIfAir(x + 1, y, z, 'azalea_leaves'); }
export function flowers(w, x, y, z) { const P = w.pen(0, 0, 0); const r = w.rng(); P.setIfAir(x, y, z, r < 0.3 ? 'poppy' : r < 0.5 ? 'cornflower' : r < 0.6 ? 'allium' : r < 0.75 ? 'lily_of_the_valley' : r < 0.85 ? 'dandelion' : r < 0.95 ? 'short_grass' : 'fern'); }

/* ---------- house palettes ---------- */
export const HOUSES = {
  gilded:     { name: 'Gilded Order',  color: 0xe9ecec, accent: 0xc9a24a, base: 'quartz_block', trim: 'gold_block', alt: 'polished_andesite', roof: 'white_concrete', cloth: 'white', emblem: 'sun' },
  graveborn:  { name: 'Graveborn',     color: 0xd8d4c2, accent: 0x6fe8ff, base: 'bone_block', trim: 'mossy_stone_bricks', alt: 'deepslate_bricks', roof: 'deepslate_tiles', cloth: 'green', emblem: 'skull' },
  wildmoot:   { name: 'Wildmoot',      color: 0x8e3c2e, accent: 0xd89a30, base: 'spruce_planks', trim: 'stripped_spruce_log', alt: 'orange_terracotta', roof: 'red_terracotta', cloth: 'brown', emblem: 'antler' },
  ironclad:   { name: 'Ironclad',      color: 0x2a2630, accent: 0xf08a3c, base: 'polished_blackstone_bricks', trim: 'cut_brass', alt: 'deepslate_tiles', roof: 'blackstone', cloth: 'black', emblem: 'anvil' },
  tidecallers:{ name: 'Tidecallers',   color: 0x3fa58c, accent: 0x7fffe0, base: 'oxidized_cut_copper', trim: 'prismarine_bricks', alt: 'dark_prismarine', roof: 'oxidized_cut_copper', cloth: 'cyan', emblem: 'trident' },
};
export const HOUSE_BANNER_BG = { gilded: 0xe9ecec, graveborn: 0x2c3a2a, wildmoot: 0x7a3426, ironclad: 0x1c1a20, tidecallers: 0x14506a };

// troop display plinth: size 3x3 base, 1 block tall, house-coloured trim. Registers the troop entity on top.
export function plinth(ctx, pen, lx, lz, lyaw, house, troopId, opts = {}) {
  const H = HOUSES[house], y0 = opts.y ?? 0;
  const big = !!opts.big, r = big ? 2 : 1;
  for (let dz = -r; dz <= r; dz++) for (let dx = -r; dx <= r; dx++) {
    const corner = Math.abs(dx) === r && Math.abs(dz) === r;
    pen.set(lx + dx, y0, lz + dz, corner ? (opts.trim || H.trim) : (opts.base || H.alt));
  }
  if (big) { for (let dz = -1; dz <= 1; dz++) for (let dx = -1; dx <= 1; dx++) pen.set(lx + dx, y0 + 1, lz + dz, opts.top || H.alt); pen.set(lx, y0 + 1, lz, opts.trim || H.trim); }
  else pen.set(lx, y0, lz, opts.top || H.base);
  const p = pen.pos(lx, y0 + 1 + (big ? 1 : 0), lz);
  ctx.troop({ house, id: troopId, x: p.x, y: p.y, z: p.z, yaw: pen.yaw(lyaw), legendary: !!opts.legendary });
  return p;
}
export function signAt(ctx, pen, lx, ly, lz, lyaw, o) { const p = pen.pos(lx, ly, lz); ctx.sign({ x: p.x, y: p.y, z: p.z, yaw: pen.yaw(lyaw), ...o }); }
export function bannerAt(ctx, pen, lx, ly, lz, lyaw, o) { const p = pen.pos(lx, ly, lz); ctx.banner({ x: p.x, y: p.y, z: p.z, yaw: pen.yaw(lyaw), ...o }); }
export function npcAt(ctx, pen, lx, ly, lz, lyaw, o) { const p = pen.pos(lx, ly, lz); ctx.npc({ x: p.x, y: p.y, z: p.z, yaw: pen.yaw(lyaw), ...o }); }
export function propAt(ctx, pen, lx, ly, lz, lyaw, o) { const p = pen.pos(lx, ly, lz); ctx.prop({ x: p.x, y: p.y, z: p.z, yaw: pen.yaw(lyaw), ...o }); }
export function emitAt(ctx, pen, lx, ly, lz, o) { const p = pen.pos(lx, ly, lz); ctx.emit({ x: p.x, y: p.y, z: p.z, ...o }); }
// pointed arch opening: width hw (half), stairs on the arch edge, material
export function pointedArch(pen, cx, y0, z, hw, rise, stairs, solid) {
  for (let i = 0; i <= hw; i++) { /* stepped arch from y0+rise */
    const y = y0 + rise + i, half = hw - i;
    for (let x = -hw; x <= hw; x++) pen.set(cx + x, y, z, Math.abs(x) >= half ? ((x < 0 ? stairs + '@W' : x > 0 ? stairs + '@E' : solid)) : 'air');
    pen.set(cx + half, y, z, half === 0 ? solid : stairs + '@E'); pen.set(cx - half, y, z, half === 0 ? solid : stairs + '@W');
  }
}

export function folkAt(ctx, pen, lx, ly, lz, lyaw, o) { const p = pen.pos(lx, ly, lz); ctx.folk({ x: p.x, y: p.y, z: p.z, yaw: pen.yaw(lyaw), ...o }); }
export function walkPath(ctx, o) { const p = o.path[0]; ctx.folk({ x: p[0], y: o.y, z: p[1], yaw: 0, anim: 'walk', ...o }); }
