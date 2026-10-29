// The Command Terrace: wall + twin stairs, Command Pavilion with the war table, Hollow Standard, Companions' Fire, Commander's Gallery.
import { pave, STONE_PAVE, bigBrazier, ironLamp, lampPost, plinth, signAt, bannerAt, npcAt, propAt, emitAt, spruce, oak, flowers, azaleaBush } from './kit.js';
import { hash } from './ctx.js';

const MAP = [
  's s s s g g r g i i i i i',
  's s s g g g r g g i i i i',
  'g g g g g r r r g g g i g',
  'w w w g r r k r r g g g g',
  'g w w g g r r r g g h h h',
  'g g w w g g r g g h h h h',
  'g g g w w g r g g h h h h',
].map((r) => r.split(' '));

export function buildCommand(ctx) {
  const w = ctx.w;
  w.beginZone('command');
  const P = w.pen(0, 16, 0, 0);     // local y 0 = T3 walking surface (world 16)
  const FLOOR = -1;

  /* ---- floor ---- */
  pave(P, -41, -75, 41, -38, FLOOR, STONE_PAVE);
  for (let x = -41; x <= 41; x++) { P.set(x, FLOOR, -38, 'polished_andesite'); }
  ctx.reserve(-42, -76, 42, -37);

  /* ---- front wall of the terrace (x outside the Ironclad facade) ---- */
  for (let x = -42; x <= 42; x++) {
    if (Math.abs(x) <= 12) continue;
    for (let y = -10; y <= -1; y++) P.set(x, y, -37, hash(x, y, 7) > 0.84 ? 'mossy_stone_bricks' : 'stone_bricks');
    P.set(x, -1, -37, 'polished_andesite');
    P.set(x, 0, -37, 'stone_bricks_wall');
  }
  for (let x = -42; x <= 42; x += 6) {
    if (Math.abs(x) <= 12) continue;
    P.fill(x, -10, -36, x, -2, -36, 'polished_andesite'); P.set(x, -1, -36, 'chiseled_stone_bricks'); P.set(x, 0, -37, 'polished_andesite'); P.set(x, 1, -37, 'lantern');
  }
  // banners down the wall between the buttresses
  for (let x = -39; x <= 39; x += 6) { if (Math.abs(x) <= 14) continue; bannerAt(ctx, P, x, -1.8, -35.9, 0, { w: 1.5, h: 5.2, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' }); }

  /* ---- twin switchback stairs ---- */
  for (const sgn of [1, -1]) {
    const F = sgn > 0 ? 'E' : 'W';
    for (let k = 1; k <= 10; k++) {
      const yk = k - 11;                      // local y of this step's block
      const xf = 12 + 2 * (k - 1), xb = xf + 1;
      for (let z = -36; z <= -34; z++) {
        P.fill(sgn * xf, -10, z, sgn * xf, yk - 1, z, 'stone_bricks'); P.fill(sgn * xb, -10, z, sgn * xb, yk, z, 'stone_bricks');
        P.set(sgn * xf, yk, z, ((z === -35) ? 'polished_andesite_stairs' : 'stone_bricks_stairs') + '@' + F);
        if (z === -35) P.set(sgn * xb, yk, z, 'polished_andesite');
      }
      // rail on the plaza side
      P.set(sgn * xb, yk + 1, -33, 'stone_bricks_wall'); P.set(sgn * xf, yk + 1 - 0, -33, hash(k, 1, 1) > 2 ? 'air' : 'stone_bricks_wall');
      if (k % 2 === 0) P.set(sgn * xb, yk + 2, -33, 'lantern');
    }
    // landing
    for (let x = 32; x <= 36; x++) for (let z = -36; z <= -34; z++) { P.fill(sgn * x, -10, z, sgn * x, -1, z, 'stone_bricks'); }
    for (let x = 32; x <= 36; x++) P.set(sgn * x, 0, -33, 'stone_bricks_wall');
    for (let z = -36; z <= -34; z++) P.set(sgn * 37, -1, z, 'stone_bricks');
    bigBrazier(P, sgn * 34, 0, -35, false);
  }

  /* ---- the great Hollow Standard at the front edge ---- */
  P.fill(-1, -1, -40, 1, -1, -38, 'polished_andesite'); P.fill(-1, 0, -40, 1, 0, -38, 'cut_brass'); P.fill(0, 1, -39, 0, 30, -39, 'stripped_dark_oak_log');
  for (const y of [4, 10, 16, 22]) P.set(0, y, -39, 'cut_brass');
  P.set(0, 31, -39, 'gold_block'); P.set(0, 32, -39, 'lantern'); for (let i = 0; i < 4; i++) { P.set(i % 2 ? 1 : -1, 29 - i, -39, 'air'); }
  // cross arm
  P.fill(-3, 28, -39, 3, 28, -39, 'dark_oak_fence');
  bannerAt(ctx, P, 0.5, 28, -38.4, 0, { w: 5.2, h: 11, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'standard' });
  signAt(ctx, P, 0, 1.4, -37.4, 0, { text: 'THE HOLLOW STANDARD', sub: 'carried by the Hollow, borne by you', w: 3.6, h: 0.9, style: 'gold' });
  ctx.markers.push({ x: 0, z: -39, label: 'Hollow Standard', kind: 'landmark' });

  /* ---- Command Pavilion: a navy-and-brass war tent ---- */
  const TZ = -58;                                       // tent centre z
  // floor carpets
  for (let x = -13; x <= 13; x++) for (let z = TZ - 9; z <= TZ + 9; z++) P.set(x, 0, z, (Math.abs(x) >= 12 || Math.abs(z - TZ) >= 9) ? 'black_carpet' : ((Math.abs(x) + Math.abs(z - TZ)) % 6 === 0 ? 'blue_carpet' : 'blue_carpet'));
  for (let x = -13; x <= 13; x++) for (let z = TZ - 9; z <= TZ + 9; z++) { const e = Math.abs(x) === 12 || Math.abs(z - TZ) === 8; if (e) P.set(x, 0, z, 'yellow_carpet'.replace('yellow', 'orange')); }
  // tent posts and canvas (A-frame, ridge along z)
  for (const sgn of [-1, 1]) for (const z of [TZ - 9, TZ - 3, TZ + 3, TZ + 9]) { P.fill(sgn * 14, 0, z, sgn * 14, 5, z, 'stripped_dark_oak_log'); P.set(sgn * 14, 6, z, 'dark_oak_fence'); }
  for (let k = 0; k <= 7; k++) {
    const y = 4 + k, h = 15 - 2 * k;
    for (let z = TZ - 10; z <= TZ + 10; z++) {
      for (const x of [h, h - 1]) { if (x < 0) continue; const cell = (k % 3 === 2) ? 'cut_brass' : 'blue_wool'; P.set(x, y, z, cell); P.set(-x, y, z, cell); }
    }
  }
  for (let z = TZ - 10; z <= TZ + 10; z++) { P.set(0, 12, z, 'dark_oak_log'); P.fill(-1, 11, z, 1, 11, z, 'blue_wool'); }
  // side walls (canvas drops to y 0-3, raised flaps at the front half)
  for (const sgn of [-1, 1]) for (let z = TZ - 10; z <= TZ - 4; z++) for (let y = 0; y <= 3; y++) P.set(sgn * 15, y, z, y === 3 ? 'cut_brass' : 'blue_wool');
  for (let x = -14; x <= 14; x++) for (let y = 0; y <= 11; y++) { if (Math.abs(x) <= (15 - Math.max(0, y - 4) * 2) - 1) P.set(x, y, TZ - 10, (y <= 3 || Math.abs(x) <= 15 - (y - 4) * 2 - 1) ? 'blue_wool' : 'air'); }
  // front gable: leave open, frame with two thick gate posts and brass crest
  for (const sgn of [-1, 1]) { P.fill(sgn * 14, 0, TZ + 10, sgn * 14, 7, TZ + 10, 'stripped_dark_oak_log'); P.set(sgn * 14, 8, TZ + 10, 'gold_block'); for (let y = 0; y <= 7; y++) P.set(sgn * 13, y, TZ + 10, y > 4 ? 'blue_wool' : 'air'); }
  // clear any wool in the open front plane
  for (let x = -14; x <= 14; x++) for (let y = 0; y <= 11; y++) { if (x === 14 || x === -14) continue; if (Math.abs(x) <= 15 - Math.max(0, y - 4) * 2 - 1 && !(Math.abs(x) >= 13 && y > 4)) P.set(x, y, TZ + 10, 'air'); }
  // swagged curtains
  for (const sgn of [-1, 1]) for (let i = 0; i < 5; i++) { for (let y = 3 + (i > 2 ? 1 : 0); y <= 7; y++) P.set(sgn * (13 - i), y + i, TZ + 10, 'blue_wool'); }
  bannerAt(ctx, P, 0, 11.4, TZ + 10.3, 0, { w: 4, h: 3.2, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' });
  signAt(ctx, P, 0, 0.9, TZ + 10.55, 0, { text: 'COMMAND PAVILION', sub: 'war table · the Long March', w: 4.4, h: 0.9, style: 'gold' });

  /* ---- the war table: relief of the Barrowlands ---- */
  const TX = 0, TTZ = TZ - 1;
  for (let x = -7; x <= 7; x++) for (let z = -4; z <= 4; z++) { P.set(TX + x, 0, TTZ + z, 'air'); }
  for (let x = -7; x <= 7; x++) for (let z = -4; z <= 4; z++) P.set(TX + x, 1, TTZ + z, 'dark_oak_planks');
  for (const x of [-7, 7]) for (const z of [-4, 4]) { P.fill(TX + x, 0, TTZ + z, TX + x, 0, TTZ + z, 'dark_oak_log'); }
  for (let x = -6; x <= 6; x++) for (let z = -3; z <= 3; z++) {
    const c = MAP[z + 3][x + 6];
    const spec = { s: 'snow_block', g: 'moss_block', w: 'blue_concrete', i: 'cobblestone', r: 'gravel', k: 'gold_block', h: 'green_wool' }[c];
    P.set(TX + x, 1, TTZ + z, spec);
    if (c === 's') { if (hash(x, z, 1) > 0.5) P.set(TX + x, 2, TTZ + z, 'snow_layer'); }
    if (c === 'i' && hash(x, z, 2) > 0.5) P.set(TX + x, 2, TTZ + z, 'stone_bricks_slab');
    if (c === 'h' && hash(x, z, 3) > 0.45) P.set(TX + x, 2, TTZ + z, 'spruce_leaves');
    if (c === 'g' && hash(x, z, 4) > 0.8) P.set(TX + x, 2, TTZ + z, 'short_grass');
  }
  for (const [x, z] of [[-5, -3], [4, -3], [-5, 0], [4, 2], [0, 0]]) { P.set(TX + x, 2, TTZ + z, 'candle'); }
  // Ironhold mini keep, Rivergate bridge, Kingsfield mounds
  for (const [x, z] of [[3, -3], [5, -3], [3, -2], [5, -2]]) P.set(TX + x, 2, TTZ + z, 'stone_bricks_wall');
  for (let z = 0; z <= 0; z++) for (const x of [-5, -4, -3]) P.set(TX + x, 2, TTZ + z, 'spruce_planks_slab');
  for (const [x, z] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) P.set(TX + x, 2, TTZ + z, 'coarse_dirt');
  signAt(ctx, P, 0, 1.55, TTZ + 4.55, 0, { text: 'THE BARROWLANDS', sub: 'Rivergate · Ironhold · Frostmarch · Hollowwood · Kingsfield', w: 5.6, h: 0.8, style: 'vellum', size: 0.5 });
  // the storybook on a lectern at the table's front edge
  P.set(TX, 2, TTZ + 4, 'air'); P.set(TX, 1, TTZ + 5, 'dark_oak_planks'); P.set(TX, 0, TTZ + 5, 'lectern');
  propAt(ctx, P, TX + 0.0, 1.15, TTZ + 5, 0, { type: 'book', scale: 1 });
  emitAt(ctx, P, 0, 1.7, TTZ + 5, { type: 'sparkle', w: 1.2, h: 2.2, n: 12, color: 0xffe9a0 });
  ctx.markers.push({ x: 0, z: TTZ + 5, label: 'Storybook', kind: 'book' });
  // maud at the east end of the table
  npcAt(ctx, P, 8.3, 0, TTZ, 90, { id: 'maud' });
  // chairs, lamps, braziers, maps
  for (const x of [-4, 4]) { P.set(x, 0, TTZ - 5, 'spruce_planks_stairs@S'); P.set(x - 1, 0, TTZ - 5, 'spruce_planks_stairs@S'); P.set(x + 1, 0, TTZ - 5, 'spruce_planks_stairs@S'); }
  for (const x of [-10, 10]) for (const z of [TZ - 5, TZ + 5]) { bigBrazier(P, x, 0, z, false); }
  for (const x of [-5, 5]) for (const z of [TZ - 4, TZ + 3]) { P.set(x, 11, z, 'chain'); P.set(x, 10, z, 'chain'); P.set(x, 9, z, 'lantern~'); }
  for (const x of [-9, 9]) for (const z of [TZ - 7, TZ + 2]) bannerAt(ctx, P, x, 5, z, x > 0 ? -90 : 90, { w: 1, h: 1.4, emblem: 'map', bg: 0xe2d3a4, trim: 0x5c4a32, kind: 'wall' });
  // the company standard behind the table
  P.fill(0, 0, TZ - 8, 0, 8, TZ - 8, 'stripped_dark_oak_log'); P.set(0, 9, TZ - 8, 'gold_block');
  bannerAt(ctx, P, 0.5, 8.5, TZ - 7.4, 0, { w: 2.2, h: 5.6, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'standard' });
  // crates + weapon stand
  for (const [x, z] of [[-12, TZ - 8], [-11, TZ - 8], [-12, TZ - 7], [11, TZ - 8], [12, TZ - 8]]) P.set(x, 0, z, hash(x, z, 4) > 0.5 ? 'barrel' : 'crate');
  ctx.zone({ id: 'command', name: 'Command Pavilion · War Table', color: '#2a4a9c', rect: [-16, -70, 16, -46], y: [15, 30], tour: { x: 0, y: 17, z: -46.5, yaw: 0, pitch: 12 },
    what: 'Campaign hub. The Hollow\'s navy-and-brass war tent holds a 13 x 7 relief of the Barrowlands (Frostmarch snow NW, Ironhold NE, Rivergate river SW, Hollowwood SE, Kingsfield gold in the centre) with a candle on each battlefield. The Long March storybook lies open on a lectern at the table; Maud Harrow stands at the table. The company standard stands behind it.',
    builder: ['Tent 29 x 21: stripped dark oak posts, A-frame roof stepping 2 blocks in per block up in blue wool with a brass band every third row, dark oak log ridge. Front gable open with swagged curtains.', 'Floor: blue carpet with orange (brass) border, black carpet at the walls.', 'Table: dark oak planks 15 x 9 on log legs; map layer in moss, blue concrete (river), snow, cobblestone, green wool, gravel roads, gold block at Kingsfield.', 'Lectern at the table\'s south edge carries the storybook.'] });

  /* ---- Companions' Fire (west wing) ---- */
  const FX = -27, FZ = -50;
  for (let x = -7; x <= 7; x++) for (let z = -7; z <= 7; z++) { const d = Math.hypot(x, z); if (d <= 7.3) P.set(FX + x, FLOOR, FZ + z, d <= 3.2 ? 'cobblestone' : (hash(x, z, 5) > 0.55 ? 'coarse_dirt' : 'podzol')); }
  for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1], [1, 1], [-1, -1], [1, -1], [-1, 1]]) P.set(FX + dx, 0, FZ + dz, 'cobblestone_slab');
  P.set(FX, 0, FZ, 'campfire');
  // tripod + pot
  P.fill(FX + 2, 0, FZ, FX + 2, 2, FZ, 'spruce_fence'); P.set(FX + 1, 2, FZ, 'spruce_fence'); P.set(FX, 2, FZ, 'chain'); P.set(FX, 1, FZ, 'cauldron'); P.set(FX, 0, FZ, 'campfire');
  emitAt(ctx, P, FX, 1.2, FZ, { type: 'ember', w: 1, h: 4, n: 18 }); emitAt(ctx, P, FX, 2, FZ, { type: 'smoke', w: 0.6, h: 9, n: 12 });
  // log seats facing the fire
  const seat = (dx, dz, f) => P.set(FX + dx, 0, FZ + dz, 'spruce_planks_stairs@' + f);
  for (const [dx, dz, f] of [[0, -4, 'S'], [4, 0, 'W'], [0, 4, 'N'], [-4, 0, 'E'], [-3, -3, 'S'], [3, 3, 'N'], [-3, 3, 'N'], [3, -3, 'S']]) seat(dx, dz, f);
  npcAt(ctx, P, FX - 3.6, 0.5, FZ + 0.5, 270, { id: 'wren', pose: 'sit' });
  npcAt(ctx, P, FX + 0.5, 0.5, FZ - 3.6, 180, { id: 'dunmar', pose: 'sit' });
  npcAt(ctx, P, FX + 0.5, 0.5, FZ + 4.6, 0, { id: 'ashka', pose: 'sit' });
  // small tents, bedrolls, stretchers
  for (const [tx, tz, rot] of [[FX - 9, FZ - 3, 0], [FX - 9, FZ + 4, 0]]) {
    for (let k = 0; k <= 2; k++) for (let z = tz - 2; z <= tz + 2; z++) for (const x of [tx - 2 + k, tx + 2 - k]) P.set(x, k, z, k === 1 ? 'light_gray_wool' : 'blue_wool');
    for (let z = tz - 2; z <= tz + 2; z++) P.set(tx, 3, z, 'blue_wool'); P.fill(tx - 1, 0, tz - 2, tx + 1, 0, tz + 2, 'red_carpet'); P.set(tx, 0, tz - 2, 'air');
  }
  for (const z of [FZ - 6, FZ + 6]) { P.fill(FX + 7, 0, z, FX + 8, 0, z + 2, 'white_carpet'); P.fill(FX + 6, 0, z, FX + 6, 0, z + 2, 'spruce_planks_slab'); }
  signAt(ctx, P, FX + 7.5, 1.2, FZ + 7.3, 0, { text: 'STRETCHERS', sub: 'the wounded rest here until the next battle', w: 3.4, h: 0.8, style: 'wood', size: 0.5 });
  for (const [x, z] of [[-9, -6], [-10, 8], [6, 7], [6, -7]]) spruce(w, P.mx(FX + x, FZ + z), 16, P.mz(FX + x, FZ + z), 9, 3);
  P.set(FX, 3, FZ, 'air');
  signAt(ctx, P, FX, 3.2, FZ + 7.8, 0, { text: "THE COMPANIONS' FIRE", sub: 'after every battle the Hollow gather here', w: 4.6, h: 0.9, style: 'wood' });
  ctx.markers.push({ x: FX, z: FZ, label: "Companions' Fire", kind: 'fire' });
  ctx.zone({ id: 'fire', bom: ['command'], name: "The Companions' Fire", color: '#f08a3c', rect: [-40, -60, -15, -40], y: [15, 28], tour: { x: -27, y: 17, z: -40, yaw: 0, pitch: 12 },
    what: 'Story hearth. After every campaign battle the company talks around this fire: Wren, Dunmar and Ashka sit on log seats (they appear here as they join the march); Maud keeps the war table. Wounded companions rest on the stretchers until the next battle is finished.',
    builder: ['Cobblestone-ringed campfire with a spruce-fence tripod, chain and cauldron; eight spruce-stair log seats facing it.', 'Two small navy field tents with red bedrolls; two stretchers (white carpet on spruce slabs).', 'Ring of spruce trees to enclose the glade.'] });

  /* ---- Commander's Gallery (east wing): six class alcoves ---- */
  const GX = 28, GZ = -62;                              // back wall centre
  P.fill(GX - 12, FLOOR, GZ - 1, GX + 12, FLOOR, GZ + 12, 'dark_oak_planks');
  for (let x = -12; x <= 12; x++) for (let z = -1; z <= 12; z++) if (Math.abs(x) % 4 === 0 && z % 4 === 3) P.set(GX + x, FLOOR, GZ + z, 'polished_andesite');
  const classes = [['champion', 'CHAMPION', 'sword & shield'], ['reaver', 'REAVER', 'two-hander'], ['warden', 'WARDEN', 'spear & bow'], ['marshal', 'MARSHAL', 'horn & banner'], ['warlock', 'WARLOCK', 'staff & lantern'], ['quartermaster', 'QUARTERMASTER', 'supply banner']];
  P.fill(GX - 13, 0, GZ - 2, GX + 13, 8, GZ - 2, 'deepslate_bricks');
  P.fill(GX - 13, 0, GZ - 1, GX + 13, 8, GZ - 1, 'stone_bricks');
  classes.forEach(([id, name, sub], i) => {
    const x = GX - 10 + i * 4;
    P.clear(x - 1, 0, GZ - 2, x + 1, 5, GZ - 1);
    P.fill(x - 1, 0, GZ - 2, x + 1, 0, GZ - 2, 'polished_andesite'); P.fill(x - 1, 0, GZ - 1, x + 1, 0, GZ - 1, 'polished_andesite');
    P.set(x - 2, 0, GZ - 1, 'polished_andesite'); P.set(x + 2, 0, GZ - 1, 'polished_andesite');
    for (const dx of [-2, 2]) P.fill(x + dx, 1, GZ - 1, x + dx, 5, GZ - 1, 'chiseled_stone_bricks');
    P.fill(x - 1, 6, GZ - 1, x + 1, 6, GZ - 1, 'cut_brass'); P.set(x, 5, GZ - 2, 'sea_lantern'.replace('sea_lantern', 'lantern~'));
    P.set(x, 0, GZ - 1, 'polished_andesite'); P.fill(x - 1, 1, GZ - 2, x + 1, 4, GZ - 2, 'blue_wool'); P.set(x, 5, GZ - 1, 'lantern~');
    P.set(x, 0, GZ + 1, 'quartz_block_slab'); P.set(x - 1, 0, GZ + 1, 'quartz_block_slab'); P.set(x + 1, 0, GZ + 1, 'quartz_block_slab');
    npcAt(ctx, P, x + 0.5, 0.5, GZ - 0.2, 0, { id: 'class_' + id, kind: 'classfig', cls: id, label: name });
    signAt(ctx, P, x + 0.5, 6.9, GZ - 0.4, 0, { text: name, sub, w: 3.2, h: 0.85, style: 'gold', size: 0.55 });
  });
  // columns + shed roof
  for (let x = -12; x <= 12; x += 4) { P.fill(GX + x, 0, GZ + 11, GX + x, 6, GZ + 11, 'stripped_dark_oak_log'); P.set(GX + x, 7, GZ + 11, 'cut_brass'); }
  P.fill(GX - 13, 7, GZ + 11, GX + 13, 7, GZ + 11, 'dark_oak_planks');
  for (let z = -2; z <= 11; z++) { const y = 8 + Math.floor((11 - z) / 3.5); for (let x = -13; x <= 13; x++) P.set(GX + x, y, GZ + z, 'dark_oak_planks_stairs@N'.replace('@N', '@N')); }
  for (let z = -2; z <= 12; z++) { for (let x = -13; x <= 13; x++) { const y = 8 + Math.floor((11 - z) / 3.5); if (y > 8) P.set(GX + x, y - 1, GZ + z, 'dark_oak_planks'); } }
  for (let x = -12; x <= 12; x += 4) P.set(GX + x, 6, GZ + 10, 'lantern~');
  signAt(ctx, P, GX, 9.4, GZ + 11.55, 0, { text: "THE COMMANDER'S GALLERY", sub: 'choose how you fight', w: 6.4, h: 1.0, style: 'gold' });
  ctx.zone({ id: 'gallery', bom: ['command'], name: "The Commander's Gallery", color: '#c9a24a', rect: [14, -66, 42, -48], y: [15, 28], tour: { x: 28, y: 17, z: -46, yaw: 0, pitch: 8 },
    what: 'Where the player picks a commander class: six lit alcoves, one per class, each with a mannequin in that class\'s gear and a banner. Hero track: Champion (sword & shield), Reaver (two-hander), Warden (spear & bow). General track: Marshal (horn & banner), Warlock (staff & lantern), Quartermaster (supply banner).',
    builder: ['Open arcade 27 x 14 with dark oak columns and a sloped dark oak plank roof, back wall of stone bricks with six 3-wide alcoves (navy wool back, brass lintel, hanging lantern).', 'Each alcove: quartz slab dais, mannequin (armour stand with the class\'s equipment) and a gold sign.'] });

  // balcony braziers and trees to frame the terrace
  for (const sgn of [-1, 1]) for (const z of [-44, -72]) ironLamp(P, sgn * 38, 0, z, 5);
  for (let i = 0; i < 18; i++) { const x = Math.floor(hash(i, 1, 90) * 80) - 40, z = -(Math.floor(hash(i, 2, 90) * 36) + 39); if (ctx.isReserved && false) continue; if (Math.abs(x) < 17 && z > -72) continue; if (Math.abs(x) > 20 && Math.abs(x) < 36 && z > -64 && z < -42) continue; if (hash(i, 3, 5) > 0.5) azaleaBush(w, x, 16, z); }
}
