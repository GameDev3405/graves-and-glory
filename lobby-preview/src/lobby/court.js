// The lower court: spawn seal, avenue, Founders' Stair, Chapter Wall, and the House Ring plaza on the second terrace.
import { T1, T2, T3 } from './terrain.js';
import { pave, STONE_PAVE, GRAVEL_PATH, lampPost, ironLamp, bigBrazier, brazier, flowers, azaleaBush } from './kit.js';
import { hash } from './ctx.js';

export const SEAL = { x: 0, z: 30 };
export const MOSAIC = { x: 0, z: -14, r: 11 };

export function buildCourt(ctx) {
  const w = ctx.w, P = w.pen(0, 0, 0);
  w.beginZone('court');
  ctx.spawn = { x: SEAL.x + 0.5, y: 0, z: SEAL.z + 0.5, yaw: 0 };

  /* ---- T1 paving: promenade at the foot of the wall, the avenue, side paths ---- */
  pave(P, -60, 5, 60, 12, -1, STONE_PAVE);
  pave(P, -13, 12, 13, 57, -1, STONE_PAVE);
  // avenue edging in polished andesite
  for (let z = 13; z <= 56; z++) { P.set(-13, -1, z, 'polished_andesite'); P.set(13, -1, z, 'polished_andesite'); }
  for (let x = -60; x <= 60; x++) P.set(x, -1, 12, 'polished_andesite');
  // cross roads (gravel) to the Training Yard and the Archive
  pave(P, -40, 36, -13, 39, -1, GRAVEL_PATH);
  pave(P, 13, 38, 46, 41, -1, GRAVEL_PATH);
  ctx.reserve(-62, 5, 62, 12); ctx.reserve(-13, 12, 13, 57);

  /* ---- spawn seal: the Hollow's mark, sunk into the avenue ---- */
  const { x: sx, z: sz } = SEAL;
  for (let z = -13; z <= 13; z++) for (let x = -13; x <= 13; x++) {
    const d = Math.hypot(x, z), a = Math.atan2(z, x);
    let b = null;
    if (d <= 12.4) b = (d > 11.2) ? 'polished_andesite' : 'stone_bricks';
    if (d <= 11.2) b = 'polished_deepslate';
    if (d <= 10.5 && d > 9.4) b = 'brass_block';
    if (d <= 9.4 && d > 8.4) b = 'deepslate_tiles';
    if (d <= 8.4) b = 'blue_concrete';
    if (d <= 6.8 && d > 5.2) b = 'brass_block';              // the hollow ring
    if (d <= 5.2 && d > 4.5) b = 'blue_terracotta';
    if (d <= 3.2 && d > 2.2) b = 'cut_brass';
    // eight spokes between ring and rim
    const sp = Math.abs(((a / (Math.PI / 4)) % 1 + 1) % 1);
    if (d > 6.8 && d <= 8.4 && (sp < 0.09 || sp > 0.91)) b = 'cut_brass';
    if (b) P.set(sx + x, -1, sz + z, b);
  }
  // sea lanterns under the rim, a ring of light
  for (let i = 0; i < 16; i++) { const a = i * Math.PI / 8; P.set(Math.round(sx + Math.cos(a) * 9.9), -1, Math.round(sz + Math.sin(a) * 9.9), 'shroomlight'); }
  ctx.reserve(sx - 14, sz - 14, sx + 14, sz + 14);
  ctx.markers.push({ x: sx, z: sz, label: 'Spawn seal', kind: 'spawn' });

  // lamp posts + braziers around the seal
  for (let i = 0; i < 8; i++) {
    const a = i * Math.PI / 4 + Math.PI / 8, x = Math.round(sx + Math.cos(a) * 14), z = Math.round(sz + Math.sin(a) * 14);
    if (i % 2) bigBrazier(P, x, 0, z); else ironLamp(P, x, 0, z, 5);
  }
  // flower beds hugging the avenue
  for (let i = 0; i < 14; i++) { const z = 16 + i * 3; if (z > 12 && z < 38) continue; for (const sgn of [-1, 1]) { const x = sgn * 15; P.set(x, -1, z, 'moss_block'); azaleaBush(w, x, 0, z); P.set(sgn * 16, -1, z + 1, 'grass_block'); flowers(w, sgn * 16, 0, z + 1); } }

  /* ---- Founders' Stair: 6 rises, two blocks deep, flanked by walls ---- */
  const zs = 16;
  for (let k = 1; k <= 6; k++) {
    const zf = zs - 2 * (k - 1), zb = zf - 1;
    for (let x = -8; x <= 8; x++) {
      const edge = Math.abs(x) >= 7;
      const fill = edge ? 'stone_bricks' : (Math.abs(x) <= 1 ? 'polished_andesite' : 'stone_bricks');
      P.fill(x, 0, zf, x, k - 2, zf, 'stone_bricks'); P.fill(x, 0, zb, x, k - 1, zb, 'stone_bricks');
      P.set(x, k - 1, zf, (Math.abs(x) <= 1 ? 'polished_andesite_stairs' : 'stone_bricks_stairs') + '@N');
      P.set(x, k - 1, zb, fill);
    }
  }
  // blue banner runner: carpets on every full step
  for (let k = 1; k <= 6; k++) { const zb = zs - 2 * (k - 1) - 1; for (let x = -1; x <= 1; x++) P.set(x, k, zb, 'blue_carpet'); }
  // side walls + lamps
  for (const sgn of [-1, 1]) {
    for (let k = 0; k <= 6; k++) {
      const z = zs - 2 * k;
      for (const zz of [z, z - 1]) { if (zz < 4 || zz > 17) continue; P.fill(sgn * 9, 0, zz, sgn * 9, Math.max(0, k - 1) + (k === 0 ? 0 : 0), zz, 'stone_bricks'); }
    }
    for (let z = 5; z <= 17; z++) {
      const k = Math.min(6, Math.max(0, Math.ceil((zs + 1 - z) / 2)));
      P.fill(sgn * 9, 0, z, sgn * 9, Math.max(0, k - 1), z, 'stone_bricks');
      P.set(sgn * 9, Math.max(0, k), z, 'stone_bricks_wall');
    }
    for (const z of [16, 12, 8]) { const k = Math.min(6, Math.ceil((zs + 1 - z) / 2)); P.set(sgn * 9, k + 1, z, 'lantern'); }
    // corner braziers at the foot
    bigBrazier(P, sgn * 10, 0, 17, true);
  }
  ctx.reserve(-10, 4, 10, 17);

  /* ---- T2 plaza paving ---- */
  pave(P, -61, -35, 61, 4, 5, STONE_PAVE);
  // clear the grass that sat on T2 first (blocks at y=5 were grass)
  for (let z = -35; z <= 4; z++) for (let x = -61; x <= 61; x++) P.set(x, 4, z, 'stone');
  pave(P, -61, -35, 61, 4, 5, STONE_PAVE);
  for (let x = -61; x <= 61; x++) P.set(x, 5, 4, 'polished_andesite');
  ctx.reserve(-62, -36, 62, 4);

  /* ---- Chapter Wall: 30 niches, tiers left to right; first 12 are lit ---- */
  for (const sgn of [-1, 1]) for (let x = 10; x <= 62; x++) {
    const ax = sgn * x;
    for (let y = -1; y <= 4; y++) P.set(ax, y, 4, (hash(ax, y, 5) > 0.82) ? 'mossy_stone_bricks' : 'stone_bricks');
    P.set(ax, 5, 4, 'polished_andesite');
    P.set(ax, -1, 5, 'polished_andesite'); // plinth
    P.set(ax, 0, 5, 'stone_bricks_slab');
    // vines + cracks for age
    if (hash(ax, 3, 8) > 0.9) P.set(ax, 3, 5, 'vine@S'); 
  }
  for (const sgn of [-1, 1]) for (let x = 10; x <= 62; x += 6) { // buttresses
    const ax = sgn * x; P.fill(ax, 0, 5, ax, 5, 5, 'polished_andesite'); P.set(ax, 6, 5, 'chiseled_stone_bricks'); P.set(ax, 6, 6, 'lantern'); 
  }
  const milestone = (t) => t % 5 === 0;
  const tierX = (t) => (t <= 15 ? -40 + 2 * t : 10 + 2 * (t - 16));
  ctx.chapter = { tiers: [] };
  for (let t = 1; t <= 30; t++) {
    const x = tierX(t), lit = t <= 12, ms = milestone(t);
    P.set(x, 1, 4, 'air'); P.set(x, 2, 4, 'air'); P.set(x, 0, 4, 'polished_andesite');
    P.set(x, 3, 4, ms ? 'gold_block' : 'polished_andesite'); 
    if (ms) { P.set(x - 1, 1, 4, 'cut_brass'); P.set(x - 1, 2, 4, 'cut_brass'); P.set(x + 1, 1, 4, 'cut_brass'); P.set(x + 1, 2, 4, 'cut_brass'); P.set(x - 1, 3, 4, 'brass_block'); P.set(x + 1, 3, 4, 'brass_block'); }
    if (lit) { P.set(x, 2, 4, 'lantern~'); P.set(x, 1, 3, 'air'); P.set(x, 1, 3, 'stone_bricks'); } else { P.set(x, 1, 4, 'iron_bars'); P.set(x, 2, 4, 'iron_bars'); }
    ctx.sign({ x: x + 0.5, y: 0.35, z: 5.52, yaw: 180, text: String(t), w: 0.6, h: 0.42, style: ms ? 'gold' : 'iron', size: 0.62 });
    if (ms) {
      ctx.banner({ x: x + 0.5, y: 6.2, z: 5.6, yaw: 180, w: 1.4, h: 2.8, emblem: t === 30 ? 'crown' : ['sun', 'skull', 'antler', 'anvil', 'trident'][(t / 5 - 1) % 5], bg: t === 30 ? 0xc9a24a : 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' });
    }
    ctx.chapter.tiers.push({ t, x, lit });
  }
  // the 30th niche is crowned: braziers and a gold crown above
  { const x = 38; for (const dx of [-3, 3]) bigBrazier(P, x + dx, 0, 7, false); }
  ctx.zone({ id: 'chapter', bom: ['court'], name: "The Founders' Chapter Wall", color: '#e0c060', rect: [-62, 3, 62, 7], y: [-1, 8], tour: { x: 0, y: 1, z: 12, yaw: 0, pitch: 8 },
    what: "30 niches, one per Founders' Chapter tier. Each lights when the player earns that tier (build with redstone lamps / lanterns on a hidden circuit or command blocks). Every 5th niche is a title tier with a gold frame and a banner; the 30th (far east, \"Bannerlord\") is crowned and flanked by braziers.",
    builder: ['Retaining wall between the lower court (y 64) and the House Ring (y 70): 7 blocks tall, stone bricks with mossy and polished andesite buttresses every 6 blocks.', 'Niches: 1 wide x 2 tall x 1 deep at y 65-66, 2 blocks apart. Lit = hanging lantern, unlit = iron bars.', 'Tier plaque (sign) under every niche. Milestone niches: gold block lintel, brass frame.'] });
  ctx.zone({ id: 'stair', bom: ['court'], name: "The Founders' Stair", color: '#b0b0b0', rect: [-10, 4, 10, 17], y: [-1, 8], tour: { x: 0, y: 1, z: 22, yaw: 0, pitch: 12 },
    what: 'Seventeen-wide ceremonial stair from the spawn court up to the House Ring: six rises, each two blocks deep (stair block then full block), a navy carpet runner, lanterns on the wall caps and soul braziers at the foot.',
    builder: ['Stair block facing north, then a full block, repeated 6 times (rise 6).', 'Runner: 3 wide blue carpet on the full-block rows.', 'Walls: stone brick wall caps, lantern every 4 blocks.'] });
  ctx.zone({ id: 'seal', bom: ['court'], name: 'Spawn Court & the Hollow Seal', color: '#c8a56b', rect: [-14, 16, 14, 44], y: [-1, 8], tour: { x: 0, y: 1, z: 30, yaw: 0, pitch: 4 },
    what: 'Players arrive on the Hollow Seal: a navy disc with a hollow brass ring (the company\'s mark), eight brass spokes, a deepslate-tile band and a brass rim lit by shroomlights. Eight posts around it alternate iron lamps and braziers.',
    builder: ['Radius 12 circle sunk into the avenue floor (replace the paving).', 'Navy = blue concrete; ring = gold block / cut copper (waxed); rim = gold block; light = shroomlight under glass or in the floor.'] });

  /* ---- Mosaic of the Five Houses and the Muster Standard ---- */
  const { x: mx, z: mz, r: mr } = MOSAIC;
  const HOUSE_WEDGE = [
    ['polished_blackstone_bricks', 'cut_brass', 'magma_block'],           // Ironclad (north)
    ['oxidized_cut_copper', 'prismarine_bricks', 'sea_lantern'],          // Tidecallers
    ['bone_block', 'mossy_stone_bricks', 'deepslate_tiles'],               // Graveborn
    ['quartz_block', 'gold_block', 'polished_andesite'],                   // Gilded
    ['red_terracotta', 'spruce_planks', 'orange_terracotta'],              // Wildmoot
  ];
  for (let z = -mr - 1; z <= mr + 1; z++) for (let x = -mr - 1; x <= mr + 1; x++) {
    const d = Math.hypot(x, z); if (d > mr + 0.5) continue;
    const ang = (Math.atan2(x, -z) * 180 / Math.PI + 360 + 36) % 360; // clockwise from north, wedge centred on its direction
    const wi = Math.floor(ang / 72) % 5, pal = HOUSE_WEDGE[wi];
    const fa = (ang % 72) / 72;
    let b = pal[0];
    if (d > mr - 1.2) b = 'brass_block';
    else if (d > mr - 3.0) b = pal[1];
    else if (d < 3.2) b = 'gold_block';
    else if (d < 4.4) b = 'brass_block';
    else if (Math.abs(fa - 0.5) < 0.07 && d > 5) b = pal[2];
    else if (fa < 0.03 || fa > 0.97) b = 'deepslate_bricks';
    P.set(mx + x, 5, mz + z, b);
  }
  // standard base + pole
  P.fill(mx - 1, 6, mz - 1, mx + 1, 6, mz + 1, 'polished_andesite'); P.fill(mx, 7, mz, mx, 22, mz, 'stripped_dark_oak_log'); P.set(mx, 23, mz, 'gold_block'); P.set(mx, 24, mz, 'lantern');
  for (let i = 0; i < 5; i++) { const y = 21 - i * 3.2 | 0; P.set(mx, y, mz, 'cut_brass'); }
  const hcol = [0x2a2630, 0x3fa58c, 0x5a7a3a, 0xe9ecec, 0x8e3c2e], emb = ['anvil', 'trident', 'skull', 'sun', 'antler'], ang0 = [0, 72, 144, 216, 288];
  for (let i = 0; i < 5; i++) {
    const a = ang0[i] * Math.PI / 180;
    ctx.banner({ x: mx + 0.5 + Math.sin(a) * 0.6, y: 21 - i * 3.2, z: mz + 0.5 - Math.cos(a) * 0.6, yaw: 180 - ang0[i], w: 3.2, h: 2.0, emblem: emb[i], bg: hcol[i], trim: 0xc9a24a, kind: 'pennant', pole: true });
  }
  ctx.zone({ id: 'ring', bom: ['court'], name: 'House Ring & Muster Standard', color: '#e07a50', rect: [-56, -36, 56, 4], y: [5, 30], tour: { x: 0, y: 7, z: 2, yaw: 0, pitch: 6 },
    what: 'The second terrace. A mosaic of the five Houses (each wedge in that House\'s materials) surrounds the Muster Standard, a 18-block pole carrying five pennants. The five House pavilions arc around the edge: Gilded and Wildmoot to the west, Tidecallers and Graveborn to the east, the Ironclad Hold in the north wall.',
    builder: ['Mosaic radius 11, centred 14 blocks in front of the Ironclad gate. Wedges (clockwise from north): Ironclad blackstone+brass, Tidecallers oxidised copper+prismarine, Graveborn bone+moss, Gilded quartz+gold, Wildmoot terracotta+spruce.', 'Gold block centre with the pole (stripped dark oak log) 18 high.'] });
}
