// The two courts: the Lower Camp (spawn seal, plaza), the Founders' Stair and Chapter Wall, and the House Terrace (mosaic, roads).
import { CAMP, WALL_Z, TERRACE, HILL, SEAL, MOSAIC, inPoly } from './layout.js';
import { pave, STONE_PAVE, GRAVEL_PATH, lampPost, ironLamp, bigBrazier, brazier, flowers, azaleaBush } from './kit.js';
import { hash } from './ctx.js';

// where each House door meets the terrace; a road runs from each one to the hill apron
const DOORS = [[-43, -2], [-45, -42], [-38, -62], [36, -66], [46, -40], [45, -2]];

// thick line of paving (square brush) from (x1,z1) to (x2,z2)
function road(P, x1, z1, x2, z2, half, y, palette) {
  const n = Math.ceil(Math.hypot(x2 - x1, z2 - z1) * 2);
  for (let i = 0; i <= n; i++) {
    const cx = Math.round(x1 + (x2 - x1) * i / n), cz = Math.round(z1 + (z2 - z1) * i / n);
    for (let dz = -half; dz <= half; dz++) for (let dx = -half; dx <= half; dx++) {
      if (Math.hypot(dx, dz) > half + 0.4) continue;
      const x = cx + dx, z = cz + dz; if (!inPoly(x, z)) continue;
      const r = hash(x, z, 31) * palette.reduce((a, p) => a + p[1], 0); let acc = 0, sp = palette[0][0];
      for (const [s, k] of palette) { acc += k; if (r <= acc) { sp = s; break; } }
      P.set(x, y, z, sp);
    }
  }
}

export function buildCourt(ctx) {
  const w = ctx.w, P = w.pen(0, 0, 0), S = w.pen(0, 0, 22);   // S: the stair and wall were drawn for a wall at z 4, the pen moves them to z 26
  w.beginZone('court');
  ctx.spawn = { x: SEAL.x + 0.5, y: 0, z: SEAL.z + 0.5, yaw: 0 };

  /* ---- Lower Camp: one big open plaza, edged in polished andesite, flower beds along its flanks ---- */
  pave(P, -40, 27, 40, 76, -1, STONE_PAVE);
  for (let z = 27; z <= 76; z++) { P.set(-40, -1, z, 'polished_andesite'); P.set(40, -1, z, 'polished_andesite'); }
  for (let x = -40; x <= 40; x++) P.set(x, -1, 76, 'polished_andesite');
  // gravel lanes to the Training Yard and the Archive doors, and to the gate wall
  pave(P, -47, 74, -41, 78, -1, GRAVEL_PATH); pave(P, 41, 74, 46, 78, -1, GRAVEL_PATH);
  ctx.reserve(-40, 27, 40, 82);
  for (let z = 30; z <= 70; z += 4) for (const sgn of [-1, 1]) {
    if (Math.abs(z - 44) < 8) continue;               // keep the doors of the Armory and the Muster clear
    const x = sgn * 38; P.set(x, -1, z, 'moss_block'); azaleaBush(w, x, 0, z); P.set(x - sgn, -1, z + 1, 'grass_block'); flowers(w, x - sgn, 0, z + 1);
  }

  /* ---- spawn seal: the Hollow's mark, sunk into the plaza ---- */
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

  /* ---- Founders' Stair: 6 rises, two blocks deep, flanked by walls ---- */
  const zs = 16;
  for (let k = 1; k <= 6; k++) {
    const zf = zs - 2 * (k - 1), zb = zf - 1;
    for (let x = -8; x <= 8; x++) {
      const edge = Math.abs(x) >= 7;
      const fill = edge ? 'stone_bricks' : (Math.abs(x) <= 1 ? 'polished_andesite' : 'stone_bricks');
      S.fill(x, 0, zf, x, k - 2, zf, 'stone_bricks'); S.fill(x, 0, zb, x, k - 1, zb, 'stone_bricks');
      S.set(x, k - 1, zf, (Math.abs(x) <= 1 ? 'polished_andesite_stairs' : 'stone_bricks_stairs') + '@N');
      S.set(x, k - 1, zb, fill);
    }
  }
  // blue banner runner: carpets on every full step
  for (let k = 1; k <= 6; k++) { const zb = zs - 2 * (k - 1) - 1; for (let x = -1; x <= 1; x++) S.set(x, k, zb, 'blue_carpet'); }
  // side walls + lamps
  for (const sgn of [-1, 1]) {
    for (let k = 0; k <= 6; k++) {
      const z = zs - 2 * k;
      for (const zz of [z, z - 1]) { if (zz < 4 || zz > 17) continue; S.fill(sgn * 9, 0, zz, sgn * 9, Math.max(0, k - 1) + (k === 0 ? 0 : 0), zz, 'stone_bricks'); }
    }
    for (let z = 5; z <= 17; z++) {
      const k = Math.min(6, Math.max(0, Math.ceil((zs + 1 - z) / 2)));
      S.fill(sgn * 9, 0, z, sgn * 9, Math.max(0, k - 1), z, 'stone_bricks');
      S.set(sgn * 9, Math.max(0, k), z, 'stone_bricks_wall');
    }
    for (const z of [16, 12, 8]) { const k = Math.min(6, Math.ceil((zs + 1 - z) / 2)); S.set(sgn * 9, k + 1, z, 'lantern'); }
    // corner braziers at the foot
    bigBrazier(S, sgn * 10, 0, 17, true);
  }
  ctx.reserve(-10, 26, 10, 39);

  /* ---- House Terrace: promenade under the wall, processional road, hill apron and a road from every House door ---- */
  pave(P, -75, 22, 75, 25, 5, STONE_PAVE);
  for (let x = -75; x <= 75; x++) P.set(x, 5, 22, 'polished_andesite');
  road(P, 0, 25, 0, 2, 4, 5, STONE_PAVE);
  for (let z = 2; z <= 25; z++) { P.set(-5, 5, z, 'polished_andesite'); P.set(5, 5, z, 'polished_andesite'); }
  for (let z = -45; z <= 1; z++) for (let x = -30; x <= 30; x++) {                // apron around the hill: a ring of paving 4 wide
    const d = Math.hypot(x - HILL.x, z - HILL.z); if (d > HILL.wall + 0.5 && d < HILL.wall + 5) P.set(x, 5, z, d > HILL.wall + 4 ? 'polished_andesite' : (hash(x, z, 17) > 0.7 ? 'cracked_stone_bricks' : 'stone_bricks'));
  }
  for (const [dx, dz] of DOORS) {
    const vx = HILL.x - dx, vz = HILL.z - dz, L = Math.hypot(vx, vz), ex = HILL.x - vx / L * (HILL.wall + 3), ez = HILL.z - vz / L * (HILL.wall + 3);
    road(P, dx, dz, ex, ez, 1, 5, GRAVEL_PATH);
  }
  ctx.reserve(-76, 22, 76, 26); ctx.reserve(-6, -2, 6, 26);

  /* ---- Chapter Wall: 30 niches, tiers left to right; first 12 are lit ---- */
  for (const sgn of [-1, 1]) for (let x = 10; x <= 75; x++) {
    const ax = sgn * x;
    for (let y = -1; y <= 4; y++) S.set(ax, y, 4, (hash(ax, y, 5) > 0.82) ? 'mossy_stone_bricks' : 'stone_bricks');
    S.set(ax, 5, 4, 'polished_andesite');
    S.set(ax, -1, 5, 'polished_andesite'); // plinth
    S.set(ax, 0, 5, 'stone_bricks_slab');
    // vines + cracks for age
    if (hash(ax, 3, 8) > 0.9) S.set(ax, 3, 5, 'vine@S'); 
  }
  for (const sgn of [-1, 1]) for (let x = 10; x <= 75; x += 6) { // buttresses
    const ax = sgn * x; S.fill(ax, 0, 5, ax, 5, 5, 'polished_andesite'); S.set(ax, 6, 5, 'chiseled_stone_bricks'); S.set(ax, 6, 6, 'lantern'); 
  }
  const milestone = (t) => t % 5 === 0;
  const tierX = (t) => (t <= 15 ? -40 + 2 * t : 10 + 2 * (t - 16));
  ctx.chapter = { tiers: [] };
  for (let t = 1; t <= 30; t++) {
    const x = tierX(t), lit = t <= 12, ms = milestone(t);
    S.set(x, 1, 4, 'air'); S.set(x, 2, 4, 'air'); S.set(x, 0, 4, 'polished_andesite');
    S.set(x, 3, 4, ms ? 'gold_block' : 'polished_andesite'); 
    if (ms) { S.set(x - 1, 1, 4, 'cut_brass'); S.set(x - 1, 2, 4, 'cut_brass'); S.set(x + 1, 1, 4, 'cut_brass'); S.set(x + 1, 2, 4, 'cut_brass'); S.set(x - 1, 3, 4, 'brass_block'); S.set(x + 1, 3, 4, 'brass_block'); }
    if (lit) { S.set(x, 2, 4, 'lantern~'); S.set(x, 1, 3, 'air'); S.set(x, 1, 3, 'stone_bricks'); } else { S.set(x, 1, 4, 'iron_bars'); S.set(x, 2, 4, 'iron_bars'); }
    ctx.sign({ x: x + 0.5, y: 0.35, z: 27.52, yaw: 180, text: String(t), w: 0.6, h: 0.42, style: ms ? 'gold' : 'iron', size: 0.62 });
    if (ms) {
      ctx.banner({ x: x + 0.5, y: 6.2, z: 27.6, yaw: 180, w: 1.4, h: 2.8, emblem: t === 30 ? 'crown' : ['sun', 'skull', 'antler', 'anvil', 'trident'][(t / 5 - 1) % 5], bg: t === 30 ? 0xc9a24a : 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' });
    }
    ctx.chapter.tiers.push({ t, x, lit });
  }
  // the 30th niche is crowned: braziers and a gold crown above
  { const x = 38; for (const dx of [-3, 3]) bigBrazier(S, x + dx, 0, 7, false); }
  ctx.zone({ id: 'chapter', bom: ['court'], name: "The Founders' Chapter Wall", color: '#e0c060', rect: [-76, 25, 76, 29], y: [-1, 8], tour: { x: 0, y: 1, z: 44, yaw: 0, pitch: 8 },
    what: "30 niches, one per Founders' Chapter tier, set into the retaining wall that holds up the House Terrace. Each lights when the player earns that tier (build with redstone lamps / lanterns on a hidden circuit or command blocks). Every 5th niche is a title tier with a gold frame and a banner; the 30th (far east, \"Bannerlord\") is crowned and flanked by braziers. The wall runs the whole width of the terrace so the plaza below always has a backdrop.",
    builder: ['Retaining wall between the Lower Camp (y 64) and the House Terrace (y 70): 7 blocks tall, 152 wide, stone bricks with mossy bricks and polished andesite buttresses every 6 blocks.', 'Niches: 1 wide x 2 tall x 1 deep at y 65-66, 2 blocks apart. Lit = hanging lantern, unlit = iron bars.', 'Tier plaque (sign) under every niche. Milestone niches: gold block lintel, brass frame.'] });
  ctx.zone({ id: 'stair', bom: ['court'], name: "The Founders' Stair", color: '#b0b0b0', rect: [-10, 26, 10, 39], y: [-1, 8], tour: { x: 0, y: 1, z: 44, yaw: 0, pitch: 12 },
    what: 'Seventeen-wide ceremonial stair, the only way from the Lower Camp up to the House Terrace: six rises, each two blocks deep (stair block then full block), a navy carpet runner, lanterns on the wall caps and soul braziers at the foot.',
    builder: ['Stair block facing north, then a full block, repeated 6 times (rise 6).', 'Runner: 3 wide blue carpet on the full-block rows.', 'Walls: stone brick wall caps, lantern every 4 blocks.'] });
  ctx.zone({ id: 'seal', bom: ['court'], name: 'Spawn Plaza & the Hollow Seal', color: '#c8a56b', rect: [-40, 40, 40, 76], y: [-1, 8], tour: { x: 0, y: 1, z: 53, yaw: 0, pitch: 4 },
    what: 'Players arrive on the Hollow Seal in the middle of a wide open plaza: a navy disc with a hollow brass ring (the company\'s mark), eight brass spokes, a deepslate-tile band and a brass rim lit by shroomlights. The four service buildings stand on the plaza edge with their doors facing the seal, the War Gates close the south end, and the Founders\' Stair rises in front of you.',
    builder: ['Radius 12 circle sunk into the plaza floor (replace the paving). Plaza 81 x 50, polished andesite edge.', 'Navy = blue concrete; ring = gold block / cut copper (waxed); rim = gold block; light = shroomlight under glass or in the floor.', 'Eight posts around the seal alternate iron lamps and braziers.'] });

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
  ctx.zone({ id: 'ring', bom: ['court'], name: 'House Terrace & Muster Standard', color: '#e07a50', rect: [-76, -110, 76, 26], y: [5, 30], tour: { x: 0, y: 7, z: 21, yaw: 0, pitch: 6 },
    what: 'The upper terrace (y 70), with the Command Hill rising in the middle. A mosaic of the five Houses surrounds the Muster Standard, a 18-block pole carrying five pennants, right at the head of the stair. Roads run out from the hill like spokes to the door of every House: Gilded and Graveborn flank the entrance, Wildmoot and Ironclad stand on the west, the Tidepost and the Commander\'s Gallery on the east, and the Companions\' Fire hides in a glade behind the hill. Every House is at least 9 blocks from its neighbours.',
    builder: ['Mosaic radius 11, centred 13 blocks north of the Chapter Wall. Wedges (clockwise from north): Ironclad blackstone+brass, Tidecallers oxidised copper+prismarine, Graveborn bone+moss, Gilded quartz+gold, Wildmoot terracotta+spruce.', 'Gold block centre with the pole (stripped dark oak log) 18 high.', 'Roads: 3 wide gravel from each House door to a 4-wide stone apron around the Command Hill.'] });
}
