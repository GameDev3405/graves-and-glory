// Block catalogue + procedural pixel-art texture atlas for the Banner Camp preview.
// Every block carries `mc`, the nearest vanilla Minecraft block, so the builder can read
// the bill of materials as a real shopping list. Textures are painted here (no game assets).

export const TILE = 16, COLS = 16;

/* ---------- colour + noise helpers ---------- */
const hex = h => [(h >> 16) & 255, (h >> 8) & 255, h & 255];
const clamp = v => v < 0 ? 0 : v > 255 ? 255 : v;
const mul = (c, f) => [clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f)];
const mix = (a, b, t) => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];
function rnd(x, y, s = 0) {
  let h = (x * 374761393 + y * 668265263 + s * 982451653) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177); h ^= h >>> 16;
  return (h >>> 0) / 4294967296;
}

/* ---------- tile painters ---------- */
// A painter receives (put, seed) and calls put(x, y, [r,g,b], alpha?) for each pixel.
const painters = new Map();
const tile = (name, fn) => painters.set(name, fn);

const noisy = (base, amp = 0.1, s = 1) => (put) => {
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) put(x, y, mul(base, 1 + (rnd(x, y, s) - 0.5) * amp * 2));
};
const speckled = (base, amp, s, specks = []) => (put) => {
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    let c = mul(base, 1 + (rnd(x, y, s) - 0.5) * amp * 2);
    for (const [col, thr, k] of specks) if (rnd(x, y, s + k) > thr) c = mul(hex(col), 1 + (rnd(x, y, s + 9) - 0.5) * 0.12);
    put(x, y, c);
  }
};
const bricks = (base, mortar, s = 3, rowH = 4, bw = 8, amp = 0.09) => (put) => {
  const m = hex(mortar), b = hex(base);
  for (let y = 0; y < 16; y++) {
    const row = Math.floor(y / rowH), off = (row % 2) * (bw / 2);
    for (let x = 0; x < 16; x++) {
      const col = Math.floor((x + off) / bw);
      const edge = y % rowH === rowH - 1 || (x + off) % bw === bw - 1;
      if (edge) put(x, y, mul(m, 1 + (rnd(x, y, s) - 0.5) * 0.12));
      else {
        const k = 0.9 + rnd(row, col, s + 4) * 0.2;
        const hi = (y % rowH === 0) ? 1.07 : 1;
        put(x, y, mul(b, k * hi * (1 + (rnd(x, y, s) - 0.5) * amp * 2)));
      }
    }
  }
};
const planks = (base, s = 5) => (put) => {
  const b = hex(base);
  for (let y = 0; y < 16; y++) {
    const row = Math.floor(y / 4);
    for (let x = 0; x < 16; x++) {
      const joint = y % 4 === 3;
      const endJoint = ((x + row * 5) % 16) === 0 && !joint;
      let k = 0.94 + rnd(row, 0, s) * 0.14 + (rnd(Math.floor(x / 3) + row * 7, y, s + 2) - 0.5) * 0.07;
      if (joint) k *= 0.72; else if (endJoint) k *= 0.8; else if (y % 4 === 0) k *= 1.05;
      put(x, y, mul(b, k));
    }
  }
};
const logSide = (base, s = 7) => (put) => {
  const b = hex(base);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const streak = rnd(x, 0, s) * 0.3 + rnd(x, Math.floor(y / 5), s + 1) * 0.15;
    put(x, y, mul(b, 0.78 + streak + (rnd(x, y, s + 2) - 0.5) * 0.08));
  }
};
const logTop = (ring, bark, s = 2) => (put) => {
  const r = hex(ring), b = hex(bark);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
    if (d > 6.2) put(x, y, mul(b, 0.8 + rnd(x, y, s) * 0.3));
    else { const ringK = (Math.floor(d) % 2) ? 0.88 : 1.05; put(x, y, mul(r, ringK + (rnd(x, y, s) - 0.5) * 0.08)); }
  }
};
const cutPanels = (base, line, s = 4) => (put) => {
  const b = hex(base), l = hex(line);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const ex = x % 8, ey = y % 8;
    let c = mul(b, 1 + (rnd(x, y, s) - 0.5) * 0.08);
    if (ex === 7 || ey === 7) c = mul(l, 0.95);
    else if (ex === 0 || ey === 0) c = mul(b, 1.12);
    put(x, y, c);
  }
};
const slabTiles = (base, mortar, s = 6) => (put) => {
  const b = hex(base), m = hex(mortar);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const row = y < 8 ? 0 : 1, off = row * 4;
    const ex = (x + off) % 8 === 7, ey = y % 8 === 7;
    if (ex || ey) put(x, y, mul(m, 1 + (rnd(x, y, s) - 0.5) * 0.1));
    else put(x, y, mul(b, 0.94 + rnd(Math.floor((x + off) / 8), row, s + 3) * 0.12 + (rnd(x, y, s) - 0.5) * 0.06));
  }
};
const cobble = (base, dark, s = 8) => (put) => {
  const b = hex(base), d = hex(dark);
  const cells = [];
  for (let i = 0; i < 10; i++) cells.push([rnd(i, 1, s) * 16, rnd(i, 2, s) * 16]);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    let best = 1e9, second = 1e9, bi = 0;
    for (let i = 0; i < cells.length; i++) for (const ox of [-16, 0, 16]) for (const oy of [-16, 0, 16]) {
      const dx = x - cells[i][0] - ox, dy = y - cells[i][1] - oy, dd = dx * dx + dy * dy;
      if (dd < best) { second = best; best = dd; bi = i; } else if (dd < second) second = dd;
    }
    const edge = Math.sqrt(second) - Math.sqrt(best) < 1.1;
    put(x, y, edge ? mul(d, 0.9 + rnd(x, y, s) * 0.2) : mul(b, 0.85 + rnd(bi, 7, s) * 0.3 + (rnd(x, y, s + 1) - 0.5) * 0.06));
  }
};
const leaves = (c1, c2, holes = 0.18, s = 11) => (put) => {
  const a = hex(c1), b = hex(c2);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const hole = rnd(x, y, s + 5) < holes;
    const c = mix(a, b, rnd(x, y, s));
    put(x, y, mul(c, 0.75 + rnd(Math.floor(x / 2), Math.floor(y / 2), s + 1) * 0.5), hole ? 0 : 255);
  }
};
const glass = (tint, alpha = 70, border = true) => (put) => {
  const t = hex(tint);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const e = x === 0 || y === 0 || x === 15 || y === 15;
    const shine = (x + y === 4 || x + y === 5 || x + y === 9) && x > 1 && y > 1;
    if (e && border) put(x, y, mul(t, 1.15), 200);
    else if (shine) put(x, y, [255, 255, 255], 150);
    else put(x, y, t, alpha);
  }
};
const grid = (base, line, step = 4, s = 3) => (put) => {
  const b = hex(base), l = hex(line);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const edge = x % step === 0 || y % step === 0;
    put(x, y, edge ? mul(l, 0.95 + rnd(x, y, s) * 0.1) : mul(b, 0.95 + rnd(x, y, s) * 0.1));
  }
};
const bookshelfSide = (s = 4) => (put) => {
  const pl = hex(0x7a5a3a), bookCols = [0x8c2a24, 0x2c4a7a, 0x3f6b3a, 0x7a3c78, 0xc9a24a, 0x5a3a24, 0x2a6b72, 0x9a9a8a];
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    if (y === 0 || y === 15 || y === 7 || y === 8) { put(x, y, mul(pl, 0.92 + rnd(x, y, s) * 0.12)); continue; }
    const bk = Math.floor(x / 2) + (y < 8 ? 0 : 9);
    const col = hex(bookCols[Math.floor(rnd(bk, 0, s) * bookCols.length)]);
    const tall = 0.7 + rnd(bk, 3, s) * 0.3;
    const yy = y < 8 ? 7 - y : 15 - y;
    put(x, y, yy / 7 > tall && yy > 4 ? mul(pl, 0.5) : mul(col, (x % 2 ? 0.9 : 1.05) * (0.9 + rnd(x, y, s) * 0.2)));
  }
};

/* ---------- tile table (name -> painter) ---------- */
const GRASS = 0x5f8f3a, GRASS2 = 0x4f7a30, DIRT = 0x7a5436, MOSS = 0x4b6b2c;
tile('stone', speckled(hex(0x7d7d7d), 0.07, 1, [[0x6a6a6a, 0.9, 3], [0x8e8e8e, 0.93, 5]]));
tile('cobblestone', cobble(0x7b7b7b, 0x4a4a4a));
tile('mossy_cobblestone', cobble(0x75806b, 0x424a3c, 9));
tile('stone_bricks', bricks(0x7b7b7b, 0x555555, 3));
tile('cracked_stone_bricks', (put) => { bricks(0x777777, 0x4f4f4f, 5)(put); for (let i = 0; i < 9; i++) { const x = Math.floor(rnd(i, 1, 7) * 16), y = Math.floor(rnd(i, 2, 7) * 16); put(x, y, [60, 60, 60]); put((x + 1) % 16, y, [70, 70, 70]); } });
tile('chiseled_stone_bricks', (put) => { bricks(0x7b7b7b, 0x555555, 3)(put); for (let y = 3; y < 13; y++) for (let x = 3; x < 13; x++) { const e = x === 3 || x === 12 || y === 3 || y === 12; put(x, y, e ? [70, 70, 70] : mul(hex(0x8a8a8a), 0.95 + rnd(x, y, 3) * 0.1)); } });
tile('deepslate', speckled(hex(0x4c4c52), 0.06, 2, [[0x3c3c42, 0.88, 3], [0x5a5a62, 0.93, 6]]));
tile('deepslate_bricks', bricks(0x4a4a50, 0x2f2f34, 4));
tile('deepslate_tiles', slabTiles(0x45454b, 0x2a2a2f, 6));
tile('polished_deepslate', cutPanels(0x4a4a50, 0x35353b, 2));
tile('andesite', speckled(hex(0x888a88), 0.07, 4, [[0x777977, 0.88, 3], [0x9c9e9b, 0.92, 4]]));
tile('polished_andesite', noisy(hex(0x8d908e), 0.04, 6));
tile('blackstone', speckled(hex(0x2a262c), 0.08, 5, [[0x3a343c, 0.9, 3], [0x1e1b20, 0.9, 5]]));
tile('polished_blackstone', cutPanels(0x2f2b32, 0x1f1c22, 8));
tile('polished_blackstone_bricks', bricks(0x2f2b32, 0x19171c, 4));
tile('chiseled_polished_blackstone', (put) => { bricks(0x2f2b32, 0x19171c, 4)(put); for (let y = 2; y < 14; y++) for (let x = 2; x < 14; x++) { const e = x === 2 || x === 13 || y === 2 || y === 13; put(x, y, e ? [24, 22, 28] : mul(hex(0x3a3640), 0.95 + rnd(x, y, 3) * 0.1)); } put(7, 7, [90, 80, 100]); put(8, 8, [90, 80, 100]); put(7, 8, [60, 56, 66]); put(8, 7, [60, 56, 66]); });
tile('gilded_blackstone', speckled(hex(0x2a262c), 0.06, 5, [[0xd9a93a, 0.93, 3]]));
tile('basalt_side', logSide(0x4a4a50, 4));
tile('basalt_top', noisy(hex(0x56565c), 0.07, 8));
tile('gravel', speckled(hex(0x8a8685), 0.12, 6, [[0x6e6a69, 0.8, 3], [0xa09c9a, 0.86, 4]]));
tile('dirt', speckled(hex(DIRT), 0.09, 7, [[0x5d3f28, 0.88, 3], [0x8a6240, 0.92, 4]]));
tile('coarse_dirt', speckled(hex(0x6e4b32), 0.1, 8, [[0x88847c, 0.88, 3], [0x573a26, 0.88, 5]]));
tile('podzol_top', speckled(hex(0x5b4125), 0.1, 9, [[0x3e2e1a, 0.82, 3], [0x6e5a2c, 0.9, 5]]));
tile('grass_top', speckled(hex(GRASS), 0.08, 10, [[0x6fa043, 0.85, 3], [0x4a7230, 0.88, 5]]));
tile('grass_side', (put) => {
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const depth = 3 + Math.floor(rnd(x, 0, 12) * 3);
    if (y < depth) put(x, y, mul(hex(GRASS), 0.9 + rnd(x, y, 13) * 0.2));
    else put(x, y, mul(hex(DIRT), 0.85 + rnd(x, y, 14) * 0.3));
  }
});
tile('moss_block', speckled(hex(MOSS), 0.1, 15, [[0x5d8236, 0.84, 3], [0x3b5a22, 0.88, 6]]));
tile('sand', noisy(hex(0xd9cb92), 0.05, 16));
tile('snow', noisy(hex(0xf2f6fb), 0.025, 17));
tile('sandstone', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) put(x, y, mul(hex(0xdccb94), (y % 5 === 4 ? 0.92 : 1) * (0.97 + rnd(x, y, 18) * 0.06))); });
tile('ice', glass(0x9ec4ff, 190, false));
tile('mud_bricks', bricks(0x8a6a50, 0x5c4432, 20));
tile('clay', noisy(hex(0xa4a8b8), 0.04, 21));
tile('terracotta', noisy(hex(0x985e43), 0.04, 22));
tile('orange_terracotta', noisy(hex(0xa15325), 0.04, 23));
tile('red_terracotta', noisy(hex(0x8e3c2e), 0.04, 24));
tile('brown_terracotta', noisy(hex(0x4d3323), 0.04, 25));
tile('white_terracotta', noisy(hex(0xd1b2a1), 0.04, 26));
tile('yellow_terracotta', noisy(hex(0xba8524), 0.04, 27));
tile('blue_terracotta', noisy(hex(0x4a3b5b), 0.04, 28));
tile('cyan_terracotta', noisy(hex(0x575b5b), 0.04, 29));
tile('green_terracotta', noisy(hex(0x4c532a), 0.04, 30));
tile('black_terracotta', noisy(hex(0x251710), 0.04, 31));

// woods
tile('spruce_planks', planks(0x7a5a34, 1));
tile('dark_oak_planks', planks(0x42301a, 2));
tile('oak_planks', planks(0xa68449, 3));
tile('birch_planks', planks(0xc4b27a, 4));
tile('jungle_planks', planks(0xa5774f, 5));
tile('crimson_planks', planks(0x7d3a52, 6));
tile('warped_planks', planks(0x2b6962, 7));
tile('spruce_log', logSide(0x4a3520, 3));
tile('spruce_log_top', logTop(0x7a5a34, 0x3d2a18, 3));
tile('dark_oak_log', logSide(0x2f2415, 4));
tile('dark_oak_log_top', logTop(0x42301a, 0x2a1f12, 4));
tile('oak_log', logSide(0x5e4a2c, 5));
tile('oak_log_top', logTop(0xa68449, 0x4e3c22, 5));
tile('stripped_spruce_log', logSide(0x7a5a34, 6));
tile('stripped_dark_oak_log', logSide(0x4a3820, 7));
tile('stripped_oak_log', logSide(0xa68449, 8));
tile('birch_log', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const mark = rnd(Math.floor(x / 2), Math.floor(y / 3), 33) > 0.82; put(x, y, mark ? [40, 38, 34] : mul(hex(0xd7d3c4), 0.92 + rnd(x, y, 34) * 0.14)); } });
tile('birch_log_top', logTop(0xc4b27a, 0xd7d3c4, 35));
tile('hay_side', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const band = y === 4 || y === 11; put(x, y, band ? mul(hex(0x8a6a1e), 0.95 + rnd(x, y, 36) * 0.1) : mul(hex(0xc9a22a), 0.8 + rnd(x, Math.floor(y / 2), 37) * 0.35)); } });
tile('hay_top', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5)); put(x, y, mul(hex(d > 6 ? 0x8a6a1e : 0xd5ae34), 0.9 + rnd(x, y, 38) * 0.2)); } });
tile('bookshelf', bookshelfSide());
tile('barrel_side', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const hoop = y === 3 || y === 12; const plank = x % 4 === 3; put(x, y, hoop ? mul(hex(0x3a2e24), 1 + rnd(x, y, 39) * 0.15) : mul(hex(0x6b4f2c), (plank ? 0.8 : 1) * (0.92 + rnd(x, y, 40) * 0.16))); } });
tile('barrel_top', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const e = x < 2 || y < 2 || x > 13 || y > 13; const c = Math.hypot(x - 7.5, y - 7.5) < 3; put(x, y, e ? mul(hex(0x3a2e24), 1) : c ? mul(hex(0x4a3a24), 1) : mul(hex(0x7a5a34), 0.92 + rnd(x, y, 41) * 0.16)); } });
tile('crate_side', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const frame = x < 2 || y < 2 || x > 13 || y > 13; const diag = Math.abs(x - y) < 2 || Math.abs(x + y - 15) < 2; put(x, y, frame || diag ? mul(hex(0x5e4326), 0.9 + rnd(x, y, 42) * 0.2) : mul(hex(0x8a6a3c), 0.9 + rnd(x, y, 43) * 0.2)); } });
tile('crafting_top', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const g = x % 5 === 0 || y % 5 === 0; put(x, y, g ? mul(hex(0x4a3320), 1) : mul(hex(0xa68449), 0.9 + rnd(x, y, 44) * 0.15)); } });
tile('crafting_side', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const tool = y > 3 && y < 11 && x > 2 && x < 13; put(x, y, tool ? mul(hex((x + y) % 3 ? 0x6b6b72 : 0x3a2a1a), 1) : mul(hex(0x8a6a3c), 0.9 + rnd(x, y, 45) * 0.2)); } });
tile('smithing_top', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const e = x < 2 || y < 2 || x > 13 || y > 13; put(x, y, e ? mul(hex(0x3a2f2a), 1) : mul(hex(0x4a4a52), 0.9 + rnd(x, y, 46) * 0.2)); } });
tile('smithing_side', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const top = y < 4; put(x, y, top ? mul(hex(0x3a3a42), 1 + rnd(x, y, 47) * 0.15) : mul(hex(0x5e4326), 0.85 + rnd(x, y, 48) * 0.3)); } });
tile('cartography_top', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const e = x < 2 || y < 2 || x > 13 || y > 13; const r = x > 3 && x < 12 && y > 3 && y < 12; const ink = (x + y) % 6 === 0 && r; put(x, y, e ? mul(hex(0x5e4326), 1) : ink ? [90, 60, 40] : r ? mul(hex(0xe2d3a4), 0.95 + rnd(x, y, 49) * 0.1) : mul(hex(0x8a6a3c), 1)); } });
tile('cartography_side', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const band = y > 2 && y < 6; put(x, y, band ? mul(hex(0xe2d3a4), 0.95 + rnd(x, y, 50) * 0.1) : mul(hex(0x6b4f2c), 0.85 + rnd(x, y, 51) * 0.3)); } });
tile('lectern_top', planks(0xa68449, 12));
tile('blast_furnace_front', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const g = x > 3 && x < 12 && y > 8 && y < 14; put(x, y, g ? mul(hex(0xff7a1c), 0.8 + rnd(x, y, 52) * 0.5) : mul(hex(0x55545a), 0.85 + rnd(x, y, 53) * 0.3)); } });
tile('blast_furnace_side', cutPanels(0x4c4b52, 0x2f2e34, 54));
tile('furnace_front', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const g = x > 3 && x < 12 && y > 7 && y < 14; put(x, y, g ? [30, 26, 24] : mul(hex(0x808080), 0.85 + rnd(x, y, 55) * 0.3)); } });
tile('loom_side', planks(0x8a6a3c, 13));
tile('target_side', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5)); put(x, y, d < 1.5 ? hex(0xd83a2a) : d < 3.5 ? hex(0xf0ece0) : d < 5.5 ? hex(0xd83a2a) : d < 7.2 ? hex(0xf0ece0) : hex(0xb06a3c)); } });
tile('bone_block_side', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const band = x === 0 || x === 15 ? 0.86 : (x === 5 || x === 10) ? 0.92 : 1; put(x, y, mul(hex(0xe2dcc6), band * (0.96 + rnd(x, y, 56) * 0.06))); } });
tile('bone_block_top', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5)); put(x, y, mul(hex(d > 6.2 ? 0xe2dcc6 : 0xc7bf9f), (Math.floor(d) % 2 ? 0.94 : 1) * (0.96 + rnd(x, y, 57) * 0.06))); } });

// metals + glowing things
tile('iron_block', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const e = x === 0 || y === 0 || x === 15 || y === 15; put(x, y, mul(hex(0xd8d8d8), (e ? 0.82 : 1) * (0.96 + rnd(x, y, 58) * 0.06))); } });
tile('gold_block', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const e = x === 0 || y === 0 || x === 15 || y === 15; const hi = (x + y) % 8 === 2 && !e; put(x, y, mul(hex(hi ? 0xfff09a : 0xf5d03c), (e ? 0.8 : 1) * (0.96 + rnd(x, y, 59) * 0.08))); } });
tile('copper_block', noisy(hex(0xc06b4a), 0.06, 60));
tile('cut_copper', cutPanels(0xc4744c, 0x8e4a30, 61));
tile('exposed_cut_copper', cutPanels(0xa27a62, 0x76564a, 62));
tile('weathered_cut_copper', cutPanels(0x5f9a7a, 0x42715a, 63));
tile('oxidized_cut_copper', cutPanels(0x3fa58c, 0x2a7a68, 64));
tile('oxidized_copper', noisy(hex(0x45a88f), 0.06, 65));
tile('brass_block', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const e = x === 0 || y === 0 || x === 15 || y === 15; const hi = (x + y) % 7 === 2 && !e; put(x, y, mul(hex(hi ? 0xe8c866 : 0xc0922c), (e ? 0.78 : 1) * (0.92 + rnd(x, y, 66) * 0.12))); } });
tile('cut_brass', cutPanels(0xb98a2c, 0x6e4c16, 67));
tile('lantern_glow', noisy(hex(0xffc864), 0.1, 68));
tile('sea_lantern', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const e = x % 8 === 0 || y % 8 === 0; const d = (x + y) % 5 === 0; put(x, y, e ? mul(hex(0x8dd7c8), 1) : d ? mul(hex(0xf0fffb), 1) : mul(hex(0xbfeee0), 0.95 + rnd(x, y, 69) * 0.1)); } });
tile('glowstone', speckled(hex(0xe8b45a), 0.14, 70, [[0xfff0a8, 0.8, 3], [0xb87a2a, 0.88, 5]]));
tile('shroomlight', speckled(hex(0xf08a3c), 0.16, 71, [[0xffd070, 0.8, 3], [0xb85a22, 0.86, 5]]));
tile('magma_block', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const v = rnd(Math.floor(x / 3), Math.floor(y / 3), 72); put(x, y, v > 0.55 ? mul(hex(0xff8c24), 0.85 + rnd(x, y, 73) * 0.3) : mul(hex(0x3a1a12), 0.8 + rnd(x, y, 74) * 0.5)); } });
tile('lava', (put, i, ph = 0) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const w = Math.sin((x * 0.7 + ph * 0.9) + Math.sin(y * 0.8 + ph * 0.5) * 1.3) * 0.5 + 0.5; const n = rnd(x, (y + Math.floor(ph * 2)) % 16, 75); put(x, y, mul(mix(hex(0xd04a10), hex(0xffd24a), w * 0.7 + n * 0.3), 0.95)); } });
tile('crying_obsidian', speckled(hex(0x1d0f33), 0.15, 76, [[0x9a3cf0, 0.9, 3], [0x6a22b8, 0.86, 5]]));
tile('obsidian', speckled(hex(0x14101e), 0.12, 77, [[0x2a2040, 0.88, 3]]));
tile('amethyst_block', speckled(hex(0x8560c4), 0.1, 78, [[0xb494ec, 0.88, 3], [0x6a48a8, 0.88, 5]]));
tile('amethyst_cluster', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const sp = [[3, 4], [7, 1], [11, 5], [5, 9], [9, 10]]; let on = false, k = 1; for (const [sx, h] of sp) { if (Math.abs(x - sx) < Math.max(0, (y - h) / 3.4) && y > h) { on = true; k = 0.8 + (x - sx) * 0.08; } } put(x, y, on ? mul(hex(y < 5 ? 0xd0b0ff : 0xa07ae0), k) : [0, 0, 0], on ? 255 : 0); } });
tile('soul_glow', noisy(hex(0x6fe8ff), 0.12, 79));
tile('white_wool_glow', noisy(hex(0xf6f2e4), 0.04, 80));

// wool + concrete in house colours
const WOOLS = { white: 0xe9ecec, orange: 0xf07613, magenta: 0xbd44b3, light_blue: 0x3aafd9, yellow: 0xf8c527, lime: 0x70b919, pink: 0xed8dac, gray: 0x3e4447, light_gray: 0x8e8e86, cyan: 0x158991, purple: 0x792aac, blue: 0x2a2f86, brown: 0x724728, green: 0x546d1b, red: 0xa12722, black: 0x141519 };
export const COLOR_NAMES = Object.keys(WOOLS);
for (const [n, c] of Object.entries(WOOLS)) {
  tile(n + '_wool', (put) => { const b = hex(c); for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) put(x, y, mul(b, 0.94 + ((x + y) % 2 ? 0.05 : 0) + (rnd(x, y, 81) - 0.5) * 0.08)); });
  tile(n + '_concrete', noisy(mul(hex(c), 0.97), 0.02, 82));
  tile(n + '_glass', glass(c, 110));
}
tile('stone_bricks_mossy', bricks(0x727b68, 0x4a5144, 90));
tile('dark_prismarine', speckled(hex(0x335a4c), 0.1, 91, [[0x2a4a3e, 0.85, 3], [0x3f6e5c, 0.88, 5]]));
tile('prismarine_bricks', bricks(0x4e9a8a, 0x2f6a5e, 92));
tile('prismarine', speckled(hex(0x5aa896), 0.1, 93, [[0x4a8a7c, 0.85, 3], [0x72c4b0, 0.9, 5]]));
tile('quartz_block', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) put(x, y, mul(hex(0xece6dd), 0.97 + rnd(x, y, 94) * 0.05)); });
tile('quartz_pillar', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const g = x === 3 || x === 12; put(x, y, mul(hex(0xece6dd), (g ? 0.88 : 1) * (0.97 + rnd(x, y, 95) * 0.05))); } });
tile('quartz_pillar_top', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5)); put(x, y, mul(hex(0xece6dd), (d > 6.3 ? 0.88 : 1) * (0.97 + rnd(x, y, 96) * 0.05))); } });
tile('chiseled_quartz', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const e = x < 2 || y < 2 || x > 13 || y > 13; const ring = Math.abs(Math.hypot(x - 7.5, y - 7.5) - 4) < 1; put(x, y, mul(hex(0xece6dd), (e ? 0.86 : ring ? 0.8 : 1) * (0.97 + rnd(x, y, 97) * 0.05))); } });
tile('calcite', speckled(hex(0xdfe0dc), 0.05, 98, [[0xc8c9c4, 0.88, 3]]));
tile('diorite', speckled(hex(0xbdbdbd), 0.08, 99, [[0x8a8a8a, 0.86, 3], [0xe2e2e2, 0.88, 5]]));
tile('tuff', speckled(hex(0x6a6b60), 0.07, 100, [[0x58594f, 0.88, 3], [0x7c7d72, 0.9, 5]]));
tile('dripstone', noisy(hex(0x7a6252), 0.07, 101));
tile('red_sandstone', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) put(x, y, mul(hex(0xb4562a), (y % 5 === 4 ? 0.9 : 1) * (0.96 + rnd(x, y, 102) * 0.08))); });

// plants + cutouts
const crossPainter = (draw) => (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const r = draw(x, y); if (r) put(x, y, r, 255); else put(x, y, [0, 0, 0], 0); } };
tile('short_grass', crossPainter((x, y) => { const blades = [[2, 9], [4, 12], [6, 8], [8, 13], [10, 10], [12, 12], [14, 8]]; for (const [bx, h] of blades) if (Math.abs(x - bx) < 1.1 - (15 - y) * 0.02 && y > 15 - h) return mul(hex(GRASS), 0.8 + (15 - y) * 0.05 + rnd(x, y, 103) * 0.2); return null; }));
tile('tall_grass', crossPainter((x, y) => { const blades = [[2, 14], [4, 11], [6, 15], [8, 12], [10, 14], [12, 10], [14, 13]]; for (const [bx, h] of blades) if (Math.abs(x - bx) < 1.2 && y > 15 - h) return mul(hex(GRASS2), 0.75 + (15 - y) * 0.04 + rnd(x, y, 104) * 0.2); return null; }));
tile('fern', crossPainter((x, y) => { const d = Math.abs(x - 7.5); const fr = y > 3 && d < (y - 3) * 0.55 && ((y + Math.floor(d)) % 3 !== 0); return fr || (d < 0.8 && y > 3) ? mul(hex(0x3f7a38), 0.8 + rnd(x, y, 105) * 0.3) : null; }));
const flower = (petal, center, h) => crossPainter((x, y) => { const stem = Math.abs(x - 7.5) < 0.7 && y > h; const head = Math.hypot(x - 7.5, y - h) < 2.6; if (head) return Math.hypot(x - 7.5, y - h) < 1 ? hex(center) : hex(petal); if (stem) return mul(hex(0x3f7a38), 0.9); const leaf = y > h + 4 && y < 14 && Math.abs(x - 7.5 - (y % 2 ? 2 : -2)) < 1.2; return leaf ? mul(hex(0x3f7a38), 1) : null; });
tile('poppy', flower(0xd0262a, 0x2a1a1a, 5));
tile('cornflower', flower(0x4a6fe0, 0xe8e8ff, 6));
tile('allium', flower(0xb868e0, 0xe8c8ff, 4));
tile('lily_of_the_valley', flower(0xf4f4f0, 0xfff0a0, 7));
tile('dandelion', flower(0xf2d02a, 0xc89a10, 7));
tile('red_mushroom', crossPainter((x, y) => { const cap = y > 5 && y < 10 && Math.abs(x - 7.5) < 5.2 - (y - 5) * 0.2; const spot = cap && (x + y) % 4 === 0; const stem = y >= 10 && Math.abs(x - 7.5) < 1.6; return spot ? hex(0xf2eadc) : cap ? hex(0xc4302a) : stem ? hex(0xd8cfb4) : null; }));
tile('brown_mushroom', crossPainter((x, y) => { const cap = y > 7 && y < 11 && Math.abs(x - 7.5) < 4.6 - (y - 7) * 0.3; const stem = y >= 11 && Math.abs(x - 7.5) < 1.2; return cap ? hex(0x8a6a4a) : stem ? hex(0xd8cfb4) : null; }));
tile('dead_bush', crossPainter((x, y) => { const m = Math.abs(x - 7.5) < 0.7 && y > 8; const br = [[3, 6], [12, 5], [5, 3], [10, 3]].some(([bx, by]) => Math.abs(x - bx - (y - by) * (bx < 8 ? 0.6 : -0.6)) < 0.8 && y >= by && y < 11); return m || br ? mul(hex(0x7a5a34), 0.85 + rnd(x, y, 106) * 0.2) : null; }));
tile('cobweb', crossPainter((x, y) => { const a = Math.abs(x - y) < 0.6 || Math.abs(x + y - 15) < 0.6 || Math.abs(x - 7.5) < 0.5 || Math.abs(y - 7.5) < 0.5; const ring = Math.abs(Math.hypot(x - 7.5, y - 7.5) - 4) < 0.7 || Math.abs(Math.hypot(x - 7.5, y - 7.5) - 7) < 0.6; return a || ring ? [232, 232, 232] : null; }));
tile('vine', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const on = rnd(Math.floor(x / 2), Math.floor(y / 4), 107) > 0.45 && (x % 4 < 3); put(x, y, mul(hex(0x3a6a2a), 0.8 + rnd(x, y, 108) * 0.4), on ? 255 : 0); } });
tile('kelp', crossPainter((x, y) => { const sway = Math.sin(y * 0.6) * 1.5; return Math.abs(x - 7.5 - sway) < 1.6 && ((x + y) % 7 !== 0) ? mul(hex(0x3a8a4a), 0.7 + rnd(x, y, 109) * 0.4) : null; }));
tile('seagrass', crossPainter((x, y) => { const sway = Math.sin(y * 0.5) * 1.1; return Math.abs(x - 7.5 - sway) < 1.1 || Math.abs(x - 4 - sway * 0.7) < 0.9 && y > 5 || Math.abs(x - 11 + sway) < 0.9 && y > 6 ? mul(hex(0x4a8a3a), 0.8 + rnd(x, y, 110) * 0.3) : null; }));
const coral = (c1, c2) => crossPainter((x, y) => { const stalks = [[3, 11], [6, 14], [9, 12], [12, 10]]; for (const [sx, h] of stalks) { if (Math.abs(x - sx) < 1.5 && y > 15 - h) return mul(hex(y < 17 - h ? c2 : c1), 0.85 + rnd(x, y, 111) * 0.3); } return null; });
tile('tube_coral', coral(0x3050e0, 0x6a8aff));
tile('brain_coral', coral(0xc43a9a, 0xe670c0));
tile('fire_coral', coral(0xc4262e, 0xe85a52));
tile('horn_coral', coral(0xd8c43a, 0xf0e070));
tile('torch', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { if (x >= 7 && x <= 8) { if (y >= 4 && y <= 5) put(x, y, hex(0xffe27a), 255); else if (y > 5 && y < 14) put(x, y, mul(hex(0x7a5a34), 0.9 + rnd(x, y, 112) * 0.2), 255); else put(x, y, [0, 0, 0], 0); } else put(x, y, [0, 0, 0], 0); } });
tile('soul_torch', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { if (x >= 7 && x <= 8) { if (y >= 4 && y <= 5) put(x, y, hex(0x7ff0ff), 255); else if (y > 5 && y < 14) put(x, y, mul(hex(0x7a5a34), 0.9 + rnd(x, y, 113) * 0.2), 255); else put(x, y, [0, 0, 0], 0); } else put(x, y, [0, 0, 0], 0); } });
tile('lantern', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const frame = y < 2 || y > 13 || x < 2 || x > 13; put(x, y, frame ? mul(hex(0x2a2a30), 1 + rnd(x, y, 114) * 0.2) : mul(hex(0xffd36e), 0.9 + rnd(x, y, 115) * 0.2)); } });
tile('soul_lantern', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const frame = y < 2 || y > 13 || x < 2 || x > 13; put(x, y, frame ? mul(hex(0x2a2a30), 1 + rnd(x, y, 116) * 0.2) : mul(hex(0x7aeaff), 0.9 + rnd(x, y, 117) * 0.2)); } });
tile('chain', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const on = (x === 7 || x === 8) && ((y % 4) < 3); put(x, y, mul(hex(0x555560), 0.8 + rnd(x, y, 118) * 0.4), on ? 255 : 0); } });
tile('iron_bars', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const on = (x === 7 || x === 8) || y === 0 || y === 15 || (y === 7 || y === 8) && false; put(x, y, mul(hex(0x8a8a92), 0.85 + rnd(x, y, 119) * 0.3), on ? 255 : 0); } });
tile('glass_pane_clear', glass(0xcfe8f0, 90));
tile('candle', crossPainter((x, y) => { if (x >= 7 && x <= 8 && y > 6) return mul(hex(0xf0e8d0), 0.9 + rnd(x, y, 120) * 0.1); if (x >= 7 && x <= 8 && y === 5) return hex(0xffd24a); if (x === 7 && y === 6) return [40, 30, 20]; return null; }));
tile('skeleton_skull', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const eye = (y > 5 && y < 9) && ((x > 2 && x < 6) || (x > 9 && x < 13)); const nose = y > 9 && y < 11 && x > 6 && x < 9; const teeth = y > 11 && x % 2 === 0; put(x, y, eye || nose ? [30, 30, 30] : teeth ? [160, 160, 150] : mul(hex(0xd8d4c2), 0.9 + rnd(x, y, 121) * 0.15)); } });
tile('wither_skull', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const eye = (y > 5 && y < 9) && ((x > 2 && x < 6) || (x > 9 && x < 13)); put(x, y, eye ? [140, 220, 255] : mul(hex(0x3a3a3e), 0.9 + rnd(x, y, 122) * 0.2)); } });
tile('bell', noisy(hex(0xd8b43a), 0.1, 123));
tile('anvil', noisy(hex(0x3a3a40), 0.12, 124));
tile('azalea_leaves', leaves(0x5e8a2c, 0x77a038, 0.12, 125));
tile('flowering_azalea_leaves', (put) => { leaves(0x5e8a2c, 0x77a038, 0.12, 126)(put); for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) if (rnd(x, y, 127) > 0.9) put(x, y, hex(0xe880c8), 255); });
tile('oak_leaves', leaves(0x3d6b2c, 0x558a38, 0.2, 128));
tile('spruce_leaves', leaves(0x2a4a30, 0x3b5f3a, 0.16, 129));
tile('dark_oak_leaves', leaves(0x2d5026, 0x40702e, 0.14, 130));
tile('birch_leaves', leaves(0x6b9a3a, 0x86b24a, 0.2, 131));
tile('cherry_leaves', leaves(0xe8a0c0, 0xf4c0d8, 0.18, 132));
tile('water', (put, i, ph = 0) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const w = Math.sin((x + y * 0.6) * 0.9 + ph) * 0.5 + 0.5, w2 = Math.sin((x * 0.5 - y) * 0.8 - ph * 1.3) * 0.5 + 0.5; put(x, y, mul(mix(hex(0x2a6fa8), hex(0x4aa0c8), w * 0.5 + w2 * 0.3), 0.92), 150); } });
tile('white', noisy([255, 255, 255], 0, 1));
tile('carpet_trim', (put) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) put(x, y, [255, 255, 255]); });

/* ---------- build atlas ---------- */
export const TILE_INDEX = {};
export function buildAtlas() {
  const names = [...painters.keys()];
  const rows = Math.ceil(names.length / COLS);
  const canvas = document.createElement('canvas');
  canvas.width = COLS * TILE; canvas.height = rows * TILE;
  const ctx = canvas.getContext('2d');
  const img = ctx.createImageData(TILE, TILE);
  names.forEach((name, i) => {
    TILE_INDEX[name] = i;
    img.data.fill(0);
    const put = (x, y, c, a = 255) => {
      const o = (y * TILE + x) * 4;
      img.data[o] = clamp(c[0]); img.data[o + 1] = clamp(c[1]); img.data[o + 2] = clamp(c[2]); img.data[o + 3] = a;
    };
    painters.get(name)(put, i, 0);
    ctx.putImageData(img, (i % COLS) * TILE, Math.floor(i / COLS) * TILE);
  });
  // repaint an animated tile (water, lava) with a new phase
  const repaint = (name, phase) => {
    const i = TILE_INDEX[name]; img.data.fill(0);
    const put = (x, y, c, a = 255) => { const o = (y * TILE + x) * 4; img.data[o] = clamp(c[0]); img.data[o + 1] = clamp(c[1]); img.data[o + 2] = clamp(c[2]); img.data[o + 3] = a; };
    painters.get(name)(put, i, phase);
    ctx.putImageData(img, (i % COLS) * TILE, Math.floor(i / COLS) * TILE);
  };
  return { canvas, rows, repaint };
}
// UV rectangle of a tile in 0..1 atlas space (v measured from the bottom, three.js style)
export function tileUV(name, rows) {
  const i = TILE_INDEX[name];
  if (i === undefined) throw new Error('unknown tile ' + name);
  const cx = i % COLS, cy = Math.floor(i / COLS);
  return [cx / COLS, 1 - (cy + 1) / rows, (cx + 1) / COLS, 1 - cy / rows];
}

/* ---------- block registry ---------- */
export const BLOCKS = [];              // id -> def (id 0 = air)
export const BY_NAME = {};

function reg(name, def) {
  const id = BLOCKS.length;
  const d = Object.assign({ id, name, shape: 'cube', opaque: true, light: 0, cutout: false, transparent: false, collide: true, emit: 0, lightBlock: undefined }, def);
  if (d.lightBlock === undefined) d.lightBlock = d.opaque ? 15 : (d.transparent ? 1 : 0);
  d.tex = normTex(d.tex);
  BLOCKS.push(d); BY_NAME[name] = id;
  return id;
}
function normTex(t) {
  if (typeof t === 'string') return { top: t, side: t, bottom: t };
  if (Array.isArray(t)) return { top: t[0], side: t[1], bottom: t[2] ?? t[1] };
  return { top: t.top ?? t.side, side: t.side ?? t.top, bottom: t.bottom ?? t.side ?? t.top };
}
// solid cube; optionally with slab/stairs/wall/fence siblings
function solid(name, tex, mc, opts = {}, family = {}) {
  reg(name, { tex, mc: mc ?? name, ...opts });
  const fam = (mc ?? name).replace(/(brick|tile)s$/, '$1');   // vanilla: stone_brick_stairs, deepslate_tile_wall ...
  if (family.slab) reg(name + '_slab', { tex, mc: fam + '_slab', shape: 'slab', opaque: false, lightBlock: 0, ...opts, emit: opts.emit ?? 0 });
  if (family.stairs) reg(name + '_stairs', { tex, mc: fam + '_stairs', shape: 'stairs', opaque: false, lightBlock: 0, ...opts, emit: opts.emit ?? 0 });
  if (family.wall) reg(name + '_wall', { tex, mc: fam + '_wall', shape: 'wall', opaque: false, lightBlock: 0, ...opts, emit: opts.emit ?? 0 });
}
const SS = { slab: true, stairs: true }, SSW = { slab: true, stairs: true, wall: true };

reg('air', { tex: 'white', opaque: false, collide: false, shape: 'air', lightBlock: 0, mc: 'air' });

solid('stone', 'stone', 'stone', {}, SS);
solid('cobblestone', 'cobblestone', 'cobblestone', {}, SSW);
solid('mossy_cobblestone', 'mossy_cobblestone', 'mossy_cobblestone', {}, SSW);
solid('stone_bricks', 'stone_bricks', 'stone_bricks', {}, SSW);
solid('mossy_stone_bricks', 'stone_bricks_mossy', 'mossy_stone_bricks', {}, SSW);
solid('cracked_stone_bricks', 'cracked_stone_bricks', 'cracked_stone_bricks');
solid('chiseled_stone_bricks', 'chiseled_stone_bricks', 'chiseled_stone_bricks');
solid('deepslate', 'deepslate', 'deepslate');
solid('deepslate_bricks', 'deepslate_bricks', 'deepslate_bricks', {}, SSW);
solid('deepslate_tiles', 'deepslate_tiles', 'deepslate_tiles', {}, SSW);
solid('polished_deepslate', 'polished_deepslate', 'polished_deepslate', {}, SSW);
solid('andesite', 'andesite', 'andesite', {}, SS);
solid('polished_andesite', 'polished_andesite', 'polished_andesite', {}, SS);
solid('blackstone', 'blackstone', 'blackstone', {}, SSW);
solid('polished_blackstone', 'polished_blackstone', 'polished_blackstone', {}, SSW);
solid('polished_blackstone_bricks', 'polished_blackstone_bricks', 'polished_blackstone_bricks', {}, SSW);
solid('chiseled_polished_blackstone', 'chiseled_polished_blackstone', 'chiseled_polished_blackstone');
solid('gilded_blackstone', 'gilded_blackstone', 'gilded_blackstone');
solid('basalt', { top: 'basalt_top', side: 'basalt_side' }, 'polished_basalt');
solid('gravel', 'gravel', 'gravel');
solid('dirt', 'dirt', 'dirt');
solid('coarse_dirt', 'coarse_dirt', 'coarse_dirt');
solid('podzol', { top: 'podzol_top', side: 'dirt', bottom: 'dirt' }, 'podzol');
solid('grass_block', { top: 'grass_top', side: 'grass_side', bottom: 'dirt' }, 'grass_block');
solid('moss_block', 'moss_block', 'moss_block');
solid('sand', 'sand', 'sand');
solid('snow_block', 'snow', 'snow_block');
solid('sandstone', 'sandstone', 'sandstone', {}, SS);
solid('mud_bricks', 'mud_bricks', 'mud_bricks', {}, SS);
solid('clay', 'clay', 'clay');
solid('terracotta', 'terracotta', 'terracotta');
solid('orange_terracotta', 'orange_terracotta', 'orange_terracotta');
solid('red_terracotta', 'red_terracotta', 'red_terracotta');
solid('brown_terracotta', 'brown_terracotta', 'brown_terracotta');
solid('white_terracotta', 'white_terracotta', 'white_terracotta');
solid('yellow_terracotta', 'yellow_terracotta', 'yellow_terracotta');
solid('blue_terracotta', 'blue_terracotta', 'blue_terracotta');
solid('cyan_terracotta', 'cyan_terracotta', 'cyan_terracotta');
solid('green_terracotta', 'green_terracotta', 'green_terracotta');
solid('black_terracotta', 'black_terracotta', 'black_terracotta');
solid('calcite', 'calcite', 'calcite');
solid('diorite', 'diorite', 'diorite', {}, SSW);
solid('tuff', 'tuff', 'tuff');
solid('dripstone_block', 'dripstone', 'dripstone_block');
solid('red_sandstone', 'red_sandstone', 'red_sandstone');
solid('obsidian', 'obsidian', 'obsidian');
solid('crying_obsidian', 'crying_obsidian', 'crying_obsidian', { light: 10, emit: 0.35 });
solid('amethyst_block', 'amethyst_block', 'amethyst_block', { light: 0, emit: 0.1 });

// planks + logs
for (const [n, t] of [['spruce', 'spruce_planks'], ['dark_oak', 'dark_oak_planks'], ['oak', 'oak_planks'], ['birch', 'birch_planks'], ['jungle', 'jungle_planks'], ['crimson', 'crimson_planks'], ['warped', 'warped_planks']]) {
  solid(n + '_planks', t, n + '_planks', {}, SS);
  reg(n + '_fence', { tex: t, mc: n + '_fence', shape: 'fence', opaque: false, lightBlock: 0 });
}
for (const n of ['spruce', 'dark_oak', 'oak', 'birch']) solid(n + '_log', { top: n + '_log_top', side: n + '_log' }, n + '_log');
for (const n of ['spruce', 'dark_oak', 'oak']) solid('stripped_' + n + '_log', { top: n + '_log_top', side: 'stripped_' + n + '_log' }, 'stripped_' + n + '_log');
solid('hay_block', { top: 'hay_top', side: 'hay_side' }, 'hay_block');
solid('bookshelf', { top: 'oak_planks', side: 'bookshelf' }, 'bookshelf');
solid('barrel', { top: 'barrel_top', side: 'barrel_side' }, 'barrel');
solid('crate', 'crate_side', 'chest');
solid('crafting_table', { top: 'crafting_top', side: 'crafting_side', bottom: 'oak_planks' }, 'crafting_table');
solid('smithing_table', { top: 'smithing_top', side: 'smithing_side', bottom: 'oak_planks' }, 'smithing_table');
solid('cartography_table', { top: 'cartography_top', side: 'cartography_side', bottom: 'oak_planks' }, 'cartography_table');
solid('loom', { top: 'loom_side', side: 'loom_side' }, 'loom');
solid('blast_furnace', { top: 'blast_furnace_side', side: 'blast_furnace_front', bottom: 'blast_furnace_side' }, 'blast_furnace', { light: 13, emit: 0.35 });
solid('furnace', { top: 'blast_furnace_side', side: 'stone', bottom: 'stone' }, 'furnace');
solid('target', 'target_side', 'target');
solid('bone_block', { top: 'bone_block_top', side: 'bone_block_side' }, 'bone_block');

// metals + glow
solid('iron_block', 'iron_block', 'iron_block');
solid('gold_block', 'gold_block', 'gold_block');
solid('copper_block', 'copper_block', 'copper_block');
solid('cut_copper', 'cut_copper', 'cut_copper', {}, SS);
solid('exposed_cut_copper', 'exposed_cut_copper', 'exposed_cut_copper', {}, SS);
solid('weathered_cut_copper', 'weathered_cut_copper', 'weathered_cut_copper', {}, SS);
solid('oxidized_cut_copper', 'oxidized_cut_copper', 'oxidized_cut_copper', {}, SS);
solid('oxidized_copper', 'oxidized_copper', 'oxidized_copper');
solid('brass_block', 'brass_block', 'waxed_cut_copper (gold-ish) / gold_block');
solid('cut_brass', 'cut_brass', 'waxed_exposed_cut_copper / gold_block', {}, SS);
solid('sea_lantern', 'sea_lantern', 'sea_lantern', { light: 15, emit: 1 });
solid('glowstone', 'glowstone', 'glowstone', { light: 15, emit: 1 });
solid('shroomlight', 'shroomlight', 'shroomlight', { light: 15, emit: 1 });
solid('magma_block', 'magma_block', 'magma_block', { light: 3, emit: 0.8 });
reg('lava', { tex: 'lava', mc: 'lava', shape: 'liquid', opaque: false, collide: false, light: 15, emit: 1, lightBlock: 1, lq: 'lava' });
solid('dark_prismarine', 'dark_prismarine', 'dark_prismarine', {}, SS);
solid('prismarine_bricks', 'prismarine_bricks', 'prismarine_bricks', {}, SSW);
solid('prismarine', 'prismarine', 'prismarine', {}, SS);
solid('quartz_block', 'quartz_block', 'quartz_block', {}, SS);
solid('quartz_pillar', { top: 'quartz_pillar_top', side: 'quartz_pillar' }, 'quartz_pillar');
solid('chiseled_quartz_block', 'chiseled_quartz', 'chiseled_quartz_block');

// wool + concrete (+ carpet) in every colour
for (const c of COLOR_NAMES) {
  solid(c + '_wool', c + '_wool', c + '_wool');
  solid(c + '_concrete', c + '_concrete', c + '_concrete', {}, { slab: true });
  reg(c + '_carpet', { tex: c + '_wool', mc: c + '_carpet', shape: 'carpet', opaque: false, lightBlock: 0, collide: true });
  reg(c + '_stained_glass', { tex: c + '_glass', mc: c + '_stained_glass', transparent: true, opaque: false, lightBlock: 0 });
  reg(c + '_stained_glass_pane', { tex: c + '_glass', mc: c + '_stained_glass_pane', shape: 'pane', transparent: true, opaque: false, lightBlock: 0 });
}
reg('glass', { tex: 'glass_pane_clear', mc: 'glass', transparent: true, opaque: false, lightBlock: 0 });
reg('glass_pane', { tex: 'glass_pane_clear', mc: 'glass_pane', shape: 'pane', transparent: true, opaque: false, lightBlock: 0 });
reg('ice', { tex: 'ice', mc: 'ice', transparent: true, opaque: false, lightBlock: 1 });
reg('iron_bars', { tex: 'iron_bars', mc: 'iron_bars', shape: 'pane', cutout: true, opaque: false, lightBlock: 0 });
reg('water', { tex: 'water', mc: 'water', shape: 'liquid', lq: 'water', transparent: true, opaque: false, collide: false, lightBlock: 2 });

// leaves
for (const n of ['oak', 'spruce', 'dark_oak', 'birch', 'azalea', 'flowering_azalea', 'cherry']) reg(n + '_leaves', { tex: n + '_leaves', mc: n + '_leaves', cutout: true, opaque: false, lightBlock: 1 });
reg('snow_layer', { tex: 'snow', mc: 'snow', shape: 'carpet', opaque: false, lightBlock: 0 });
reg('moss_carpet', { tex: 'moss_block', mc: 'moss_carpet', shape: 'carpet', opaque: false, lightBlock: 0 });

// cross plants
for (const n of ['short_grass', 'tall_grass', 'fern', 'poppy', 'cornflower', 'allium', 'lily_of_the_valley', 'dandelion', 'red_mushroom', 'brown_mushroom', 'dead_bush', 'cobweb', 'kelp', 'seagrass', 'tube_coral', 'brain_coral', 'fire_coral', 'horn_coral'])
  reg(n, { tex: n, mc: n === 'short_grass' ? 'short_grass' : n, shape: 'cross', cutout: true, opaque: false, collide: false, lightBlock: 0 });
reg('amethyst_cluster', { tex: 'amethyst_cluster', mc: 'amethyst_cluster', shape: 'cross', cutout: true, opaque: false, collide: false, light: 5, emit: 0.5, lightBlock: 0 });
reg('candle', { tex: 'candle', mc: 'candle', shape: 'cross', cutout: true, opaque: false, collide: false, light: 8, emit: 0.7, lightBlock: 0 });
reg('vine', { tex: 'vine', mc: 'vine', shape: 'vine', cutout: true, opaque: false, collide: false, lightBlock: 0 });

// lights + small furniture
reg('torch', { tex: 'torch', mc: 'torch', shape: 'torch', cutout: true, opaque: false, collide: false, light: 14, emit: 0.7, lightBlock: 0 });
reg('soul_torch', { tex: 'soul_torch', mc: 'soul_torch', shape: 'torch', cutout: true, opaque: false, collide: false, light: 10, emit: 0.7, lightBlock: 0, glow: 'soul' });
reg('lantern', { tex: 'lantern', mc: 'lantern', shape: 'lantern', opaque: false, collide: false, light: 15, emit: 1, lightBlock: 0 });
reg('soul_lantern', { tex: 'soul_lantern', mc: 'soul_lantern', shape: 'lantern', opaque: false, collide: false, light: 10, emit: 1, lightBlock: 0, glow: 'soul' });
reg('chain', { tex: 'chain', mc: 'chain', shape: 'chain', cutout: true, opaque: false, collide: false, lightBlock: 0 });
reg('campfire', { tex: { top: 'spruce_log_top', side: 'spruce_log' }, mc: 'campfire', shape: 'campfire', opaque: false, light: 15, emit: 0.4, lightBlock: 0 });
reg('soul_campfire', { tex: { top: 'spruce_log_top', side: 'spruce_log' }, mc: 'soul_campfire', shape: 'campfire', opaque: false, light: 10, emit: 0.4, lightBlock: 0, glow: 'soul' });
reg('skeleton_skull', { tex: 'skeleton_skull', mc: 'skeleton_skull', shape: 'skull', opaque: false, lightBlock: 0 });
reg('wither_skull', { tex: 'wither_skull', mc: 'wither_skeleton_skull', shape: 'skull', opaque: false, lightBlock: 0 });
reg('lectern', { tex: { top: 'lectern_top', side: 'spruce_planks' }, mc: 'lectern', shape: 'lectern', opaque: false, lightBlock: 0 });
reg('anvil', { tex: 'anvil', mc: 'anvil', shape: 'anvil', opaque: false, lightBlock: 0 });
reg('bell', { tex: 'bell', mc: 'bell', shape: 'bell', opaque: false, lightBlock: 0 });
reg('enchanting_table', { tex: { top: 'crying_obsidian', side: 'obsidian' }, mc: 'enchanting_table', shape: 'enchant', opaque: false, light: 7, emit: 0.4, lightBlock: 0 });
reg('grindstone', { tex: 'smithing_top', mc: 'grindstone', shape: 'grind', opaque: false, lightBlock: 0 });
reg('cauldron', { tex: 'smithing_top', mc: 'cauldron', shape: 'cauldron', opaque: false, lightBlock: 0 });
reg('barrier_air', { tex: 'white', mc: '(invisible)', shape: 'air', opaque: false, collide: true, lightBlock: 0 });

export const nameOf = id => BLOCKS[id].name;
export const isAir = id => id === 0;
