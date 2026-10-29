// The Command Hill: a round, walled hill in the middle of the House Terrace with the war tent on top, the Hollow Standard on its brow,
// plus the Companions' Fire (a quiet glade behind the hill) and the Commander's Gallery (a free-standing arcade on the terrace).
import { pave, STONE_PAVE, bigBrazier, ironLamp, plinth, signAt, bannerAt, npcAt, propAt, emitAt, spruce, flowers, azaleaBush } from './kit.js';
import { PLACE, RECT, HILL } from './layout.js';
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
  const T = w.pen(0, 6, 0, 0);       // local y 0 = House Terrace surface (world 6)
  const H = w.pen(0, 12, 0, 0);      // local y 0 = Command Hill surface (world 12)
  const FLOOR = -1;
  const HX = HILL.x, HZ = HILL.z;

  /* ---- hill: stone-brick retaining wall facing the terrace, paved top, parapet and lamps ---- */
  for (let z = HZ - 20; z <= HZ + 20; z++) for (let x = HX - 20; x <= HX + 20; x++) {
    const d = Math.hypot(x - HX, z - HZ);
    if (d > HILL.wall + 0.6) continue;
    if (d <= HILL.r + 0.4) H.set(x, FLOOR, z, hash(x, z, 6) > 0.8 ? 'polished_andesite' : (hash(x, z, 7) > 0.7 ? 'cracked_stone_bricks' : 'stone_bricks'));
    else {
      for (let y = -7; y <= -2; y++) H.set(x, y, z, hash(x, y, z) > 0.84 ? 'mossy_stone_bricks' : 'stone_bricks');
      H.set(x, FLOOR, z, 'polished_andesite');
      if (d > HILL.r + 0.9 && !(Math.abs(x - HX) <= 9 && z > HZ)) H.set(x, 0, z, 'stone_bricks_wall');
    }
  }
  // buttresses and lamp posts around the brow
  for (let i = 0; i < 12; i++) {
    const a = i * Math.PI / 6 + Math.PI / 12, bx = Math.round(HX + Math.sin(a) * (HILL.wall + 0.2)), bz = Math.round(HZ - Math.cos(a) * (HILL.wall + 0.2));
    if (Math.abs(bx - HX) <= 9 && bz > HZ) continue;             // the stair side
    H.fill(bx, -7, bz, bx, -1, bz, 'polished_andesite'); H.set(bx, 0, bz, 'chiseled_stone_bricks'); H.set(bx, 1, bz, 'lantern');
  }
  /* ---- the hill stair: six rises on the south side, in front of the wall, flanked by cheek walls ---- */
  for (let k = 1; k <= 6; k++) {
    const z = 2 - k;                                              // z of step k: 1 (terrace side) ... -4 (brow)
    for (let x = -8; x <= 8; x++) {
      T.fill(x, -1, z, x, k - 2, z, 'stone_bricks');
      T.set(x, k - 1, z, (Math.abs(x) <= 1 ? 'polished_andesite_stairs' : 'stone_bricks_stairs') + '@N');
    }
  }
  for (const sgn of [-1, 1]) {
    for (let z = 2; z >= -5; z--) {
      const top = Math.min(6, 2 - z) ;
      T.fill(sgn * 9, -1, z, sgn * 9, Math.max(0, top - 1), z, 'stone_bricks'); T.set(sgn * 9, Math.max(0, top), z, 'stone_bricks_wall');
      if (z === 1 || z === -3) T.set(sgn * 9, Math.max(1, top) + 1, z, 'lantern');
    }
    bigBrazier(T, sgn * 11, 0, 3, true);
  }
  ctx.reserve(HX - 20, HZ - 20, HX + 20, HZ + 20);
  // carpet runner from the stair head to the tent
  for (let z = -5; z >= -14; z--) for (const x of [-1, 0, 1]) H.set(x, 0, z, 'blue_carpet');

  /* ---- the great Hollow Standard on the brow, in front of the tent ---- */
  const SZ0 = -7;
  H.fill(-1, FLOOR, SZ0 - 1, 1, FLOOR, SZ0 + 1, 'polished_andesite'); H.fill(-1, 0, SZ0 - 1, 1, 0, SZ0 + 1, 'cut_brass'); H.fill(0, 1, SZ0, 0, 30, SZ0, 'stripped_dark_oak_log');
  for (const y of [4, 10, 16, 22]) H.set(0, y, SZ0, 'cut_brass');
  H.set(0, 31, SZ0, 'gold_block'); H.set(0, 32, SZ0, 'lantern');
  H.fill(-3, 28, SZ0, 3, 28, SZ0, 'dark_oak_fence');
  bannerAt(ctx, H, 0.5, 28, SZ0 + 0.6, 0, { w: 7.2, h: 14, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'standard' });
  signAt(ctx, H, 0, 1.4, SZ0 + 1.6, 0, { text: 'THE HOLLOW STANDARD', sub: 'carried by the Hollow, borne by you', w: 3.6, h: 0.9, style: 'gold' });
  ctx.markers.push({ x: 0, z: SZ0, label: 'Hollow Standard', kind: 'landmark' });

  /* ---- Command Pavilion: a navy-and-brass war tent (23 wide) ---- */
  const TZ = -24;                                       // tent centre z (world)
  for (let x = -10; x <= 10; x++) for (let z = TZ - 8; z <= TZ + 8; z++) {
    const ex = Math.abs(x), ez = Math.abs(z - TZ);
    H.set(x, 0, z, (ex === 10 || ez === 8) ? 'black_carpet' : (ex === 9 || ez === 7) ? 'orange_carpet' : 'blue_carpet');
  }
  for (const sgn of [-1, 1]) for (const z of [TZ - 8, TZ - 3, TZ + 3, TZ + 8]) { H.fill(sgn * 11, 0, z, sgn * 11, 4, z, 'stripped_dark_oak_log'); H.set(sgn * 11, 5, z, 'dark_oak_fence'); }
  for (let k = 0; k <= 5; k++) {
    const y = 4 + k, h = 12 - 2 * k;
    for (let z = TZ - 9; z <= TZ + 9; z++) for (const x of [h, h - 1]) { const cell = (k % 3 === 2) ? 'cut_brass' : 'blue_wool'; H.set(x, y, z, cell); H.set(-x, y, z, cell); }
  }
  for (let z = TZ - 9; z <= TZ + 9; z++) { H.set(0, 9, z, 'blue_wool'); H.set(0, 10, z, 'dark_oak_log'); }
  // back wall and the closed back half of the side walls
  for (const sgn of [-1, 1]) for (let z = TZ - 9; z <= TZ - 3; z++) for (let y = 0; y <= 3; y++) H.set(sgn * 12, y, z, y === 3 ? 'cut_brass' : 'blue_wool');
  for (let y = 0; y <= 9; y++) { const hw = y <= 4 ? 11 : 11 - 2 * (y - 4); for (let x = -hw; x <= hw; x++) H.set(x, y, TZ - 9, y <= 3 && Math.abs(x) >= 11 ? 'cut_brass' : 'blue_wool'); }
  // front gable: open, framed by two stout gate posts, brass crest and swagged curtains
  for (const sgn of [-1, 1]) { H.fill(sgn * 11, 0, TZ + 9, sgn * 11, 7, TZ + 9, 'stripped_dark_oak_log'); H.set(sgn * 11, 8, TZ + 9, 'gold_block'); for (let y = 0; y <= 7; y++) H.set(sgn * 10, y, TZ + 9, y > 4 ? 'blue_wool' : 'air'); }
  for (const sgn of [-1, 1]) for (let i = 0; i < 4; i++) for (let y = 3 + (i > 1 ? 1 : 0); y <= 6; y++) H.set(sgn * (10 - i), y + i, TZ + 9, 'blue_wool');
  bannerAt(ctx, H, 0, 9.0, TZ + 9.3, 0, { w: 3.2, h: 2.6, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'wall' });
  signAt(ctx, H, 0, 0.9, TZ + 9.55, 0, { text: 'COMMAND PAVILION', sub: 'war table · the Long March', w: 4.4, h: 0.9, style: 'gold' });

  /* ---- the war table: relief of the Barrowlands ---- */
  const TX = 0, TTZ = TZ - 1;
  for (let x = -7; x <= 7; x++) for (let z = -4; z <= 4; z++) { H.set(TX + x, 0, TTZ + z, 'air'); }
  for (let x = -7; x <= 7; x++) for (let z = -4; z <= 4; z++) H.set(TX + x, 1, TTZ + z, 'dark_oak_planks');
  for (const x of [-7, 7]) for (const z of [-4, 4]) { H.fill(TX + x, 0, TTZ + z, TX + x, 0, TTZ + z, 'dark_oak_log'); }
  for (let x = -6; x <= 6; x++) for (let z = -3; z <= 3; z++) {
    const c = MAP[z + 3][x + 6];
    const spec = { s: 'snow_block', g: 'moss_block', w: 'blue_concrete', i: 'cobblestone', r: 'gravel', k: 'gold_block', h: 'green_wool' }[c];
    H.set(TX + x, 1, TTZ + z, spec);
    if (c === 's') { if (hash(x, z, 1) > 0.5) H.set(TX + x, 2, TTZ + z, 'snow_layer'); }
    if (c === 'i' && hash(x, z, 2) > 0.5) H.set(TX + x, 2, TTZ + z, 'stone_bricks_slab');
    if (c === 'h' && hash(x, z, 3) > 0.45) H.set(TX + x, 2, TTZ + z, 'spruce_leaves');
    if (c === 'g' && hash(x, z, 4) > 0.8) H.set(TX + x, 2, TTZ + z, 'short_grass');
  }
  for (const [x, z] of [[-5, -3], [4, -3], [-5, 0], [4, 2], [0, 0]]) { H.set(TX + x, 2, TTZ + z, 'candle'); }
  // Ironhold mini keep, Rivergate bridge, Kingsfield mounds
  for (const [x, z] of [[3, -3], [5, -3], [3, -2], [5, -2]]) H.set(TX + x, 2, TTZ + z, 'stone_bricks_wall');
  for (let z = 0; z <= 0; z++) for (const x of [-5, -4, -3]) H.set(TX + x, 2, TTZ + z, 'spruce_planks_slab');
  for (const [x, z] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) H.set(TX + x, 2, TTZ + z, 'coarse_dirt');
  signAt(ctx, H, 0, 1.55, TTZ + 4.55, 0, { text: 'THE BARROWLANDS', sub: 'Rivergate · Ironhold · Frostmarch · Hollowwood · Kingsfield', w: 5.6, h: 0.8, style: 'vellum', size: 0.5 });
  // the storybook on a lectern at the table's front edge
  H.set(TX, 2, TTZ + 4, 'air'); H.set(TX, 1, TTZ + 5, 'dark_oak_planks'); H.set(TX, 0, TTZ + 5, 'lectern');
  propAt(ctx, H, TX + 0.0, 1.15, TTZ + 5, 0, { type: 'book', scale: 1 });
  emitAt(ctx, H, 0, 1.7, TTZ + 5, { type: 'sparkle', w: 1.2, h: 2.2, n: 12, color: 0xffe9a0 });
  ctx.markers.push({ x: 0, z: TTZ + 5, label: 'Storybook', kind: 'book' });
  // maud at the east end of the table
  npcAt(ctx, H, 8.3, 0, TTZ, 90, { id: 'maud' });
  // chairs, lamps, braziers, maps
  for (const x of [-4, 4]) { H.set(x, 0, TTZ - 5, 'spruce_planks_stairs@S'); H.set(x - 1, 0, TTZ - 5, 'spruce_planks_stairs@S'); H.set(x + 1, 0, TTZ - 5, 'spruce_planks_stairs@S'); }
  for (const x of [-9, 9]) for (const z of [TZ - 5, TZ + 5]) bigBrazier(H, x, 0, z, false);
  for (const x of [-3, 3]) for (const z of [TZ - 4, TZ + 3]) { H.set(x, 7, z, 'chain'); H.set(x, 6, z, 'lantern~'); }
  for (const x of [-9, 9]) for (const z of [TZ - 7, TZ + 2]) bannerAt(ctx, H, x, 5, z, x > 0 ? -90 : 90, { w: 1, h: 1.4, emblem: 'map', bg: 0xe2d3a4, trim: 0x5c4a32, kind: 'wall' });
  for (let z = TZ - 8; z <= TZ + 8; z += 3) { H.set(0, 8, z, 'chain'); H.set(0, 7, z, 'lantern~'); }
  for (const sgn of [-1, 1]) for (const z of [TZ - 8, TZ - 3, TZ + 3, TZ + 8]) { H.set(sgn * 10, 3, z, 'lantern'); H.set(sgn * 10, 0, z, 'candle'); }
  for (const x of [-6, 6]) for (const z of [TTZ - 5, TTZ + 5]) H.set(x, 1, z, 'candle');
  H.fill(0, 0, TZ - 8, 0, 8, TZ - 8, 'stripped_dark_oak_log'); H.set(0, 9, TZ - 8, 'gold_block');
  bannerAt(ctx, H, 0.5, 8.5, TZ - 7.4, 0, { w: 2.2, h: 5.6, emblem: 'hollow', bg: 0x1d2a5c, trim: 0xc9a24a, kind: 'standard' });
  for (const [x, z] of [[-10, TZ - 7], [-9, TZ - 7], [-10, TZ - 6], [9, TZ - 7], [10, TZ - 7]]) H.set(x, 0, z, hash(x, z, 4) > 0.5 ? 'barrel' : 'crate');
  ctx.zone({ id: 'command', name: 'Command Hill · Pavilion & War Table', color: '#2a4a9c', rect: RECT.command, y: [5, 44], tour: { x: 0, y: 7, z: 5, yaw: 0, pitch: 16 },
    what: 'The hill in the middle of the House Terrace (y 76), reached by a single six-step stair. On the brow stands the Hollow Standard; behind it the navy-and-brass war tent holds a 13 x 7 relief of the Barrowlands (Frostmarch snow NW, Ironhold NE, Rivergate river SW, Hollowwood SE, Kingsfield gold in the centre) with a candle on each battlefield. The Long March storybook lies open on a lectern at the table; Maud Harrow stands at the table. The company standard stands behind it.',
    builder: ['Hill: a round platform radius 18 and 6 high (y 70 to 76) faced with stone bricks, parapet of stone brick walls and a lantern buttress every 30 degrees. One stair, 17 wide, on the south side.', 'Tent 23 x 19: stripped dark oak posts, A-frame roof stepping 2 blocks in per block up in blue wool with a brass band every third row, dark oak log ridge. Front gable open with swagged curtains.', 'Floor: blue carpet with orange (brass) border, black carpet at the walls.', 'Table: dark oak planks 15 x 9 on log legs; map layer in moss, blue concrete (river), snow, cobblestone, green wool, gravel roads, gold block at Kingsfield.', 'Lectern at the table\'s south edge carries the storybook.'] });

  /* ---- Companions' Fire (west wing) ---- */
  const FX = PLACE.fire.x, FZ = PLACE.fire.z;
  const P = w.pen(0, 6, 0);
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
  for (const [x, z] of [[-9, -6], [-10, 8], [6, 7], [6, -7]]) spruce(w, P.mx(FX + x, FZ + z), 6, P.mz(FX + x, FZ + z), 9, 3);
  P.set(FX, 3, FZ, 'air');
  signAt(ctx, P, FX, 3.2, FZ + 7.8, 0, { text: "THE COMPANIONS' FIRE", sub: 'after every battle the Hollow gather here', w: 4.6, h: 0.9, style: 'wood' });
  ctx.markers.push({ x: FX, z: FZ, label: "Companions' Fire", kind: 'fire' });
  ctx.zone({ id: 'fire', bom: ['command'], name: "The Companions' Fire", color: '#f08a3c', rect: RECT.fire, y: [5, 20], tour: { x: 0, y: 7, z: -76, yaw: 0, pitch: 10 },
    what: 'Story hearth, tucked away behind the Command Hill. After every campaign battle the company talks around this fire: Wren, Dunmar and Ashka sit on log seats (they appear here as they join the march); Maud keeps the war table. Wounded companions rest on the stretchers until the next battle is finished.',
    builder: ['Cobblestone-ringed campfire with a spruce-fence tripod, chain and cauldron; eight spruce-stair log seats facing it.', 'Two small navy field tents with red bedrolls; two stretchers (white carpet on spruce slabs).', 'Ring of spruce trees to enclose the glade.', 'The glade sits behind the Command Hill on purpose: it is a quiet corner, not visible from the House Plaza.'] });

  /* ---- Commander's Gallery (east wing): six class alcoves ---- */
  const GX = PLACE.gallery.x, GZ = PLACE.gallery.z;
  const G = w.pen(0, 6, 0);                              // back wall centre
  G.fill(GX - 12, FLOOR, GZ - 1, GX + 12, FLOOR, GZ + 12, 'dark_oak_planks');
  for (let x = -12; x <= 12; x++) for (let z = -1; z <= 12; z++) if (Math.abs(x) % 4 === 0 && z % 4 === 3) G.set(GX + x, FLOOR, GZ + z, 'polished_andesite');
  const classes = [['champion', 'CHAMPION', 'sword & shield'], ['reaver', 'REAVER', 'two-hander'], ['warden', 'WARDEN', 'spear & bow'], ['marshal', 'MARSHAL', 'horn & banner'], ['warlock', 'WARLOCK', 'staff & lantern'], ['quartermaster', 'QUARTERMASTER', 'supply banner']];
  G.fill(GX - 13, 0, GZ - 2, GX + 13, 8, GZ - 2, 'deepslate_bricks');
  G.fill(GX - 13, 0, GZ - 1, GX + 13, 8, GZ - 1, 'stone_bricks');
  classes.forEach(([id, name, sub], i) => {
    const x = GX - 10 + i * 4;
    G.clear(x - 1, 0, GZ - 2, x + 1, 5, GZ - 1);
    G.fill(x - 1, 0, GZ - 2, x + 1, 0, GZ - 2, 'polished_andesite'); G.fill(x - 1, 0, GZ - 1, x + 1, 0, GZ - 1, 'polished_andesite');
    G.set(x - 2, 0, GZ - 1, 'polished_andesite'); G.set(x + 2, 0, GZ - 1, 'polished_andesite');
    for (const dx of [-2, 2]) G.fill(x + dx, 1, GZ - 1, x + dx, 5, GZ - 1, 'chiseled_stone_bricks');
    G.fill(x - 1, 6, GZ - 1, x + 1, 6, GZ - 1, 'cut_brass'); G.set(x, 5, GZ - 2, 'sea_lantern'.replace('sea_lantern', 'lantern~'));
    G.set(x, 0, GZ - 1, 'polished_andesite'); G.fill(x - 1, 1, GZ - 2, x + 1, 4, GZ - 2, 'blue_wool'); G.set(x, 5, GZ - 1, 'lantern~');
    G.set(x, 0, GZ + 1, 'quartz_block_slab'); G.set(x - 1, 0, GZ + 1, 'quartz_block_slab'); G.set(x + 1, 0, GZ + 1, 'quartz_block_slab');
    npcAt(ctx, G, x + 0.5, 0.5, GZ - 0.2, 0, { id: 'class_' + id, kind: 'classfig', cls: id, label: name });
    signAt(ctx, G, x + 0.5, 6.9, GZ - 0.4, 0, { text: name, sub, w: 3.2, h: 0.85, style: 'gold', size: 0.55 });
  });
  G.fill(GX - 14, -1, GZ + 13, GX + 14, -1, GZ + 16, 'stone_bricks'); pave(G, GX - 14, GZ + 13, GX + 14, GZ + 16, -1, STONE_PAVE);
  // columns + shed roof
  for (let x = -12; x <= 12; x += 4) { G.fill(GX + x, 0, GZ + 11, GX + x, 6, GZ + 11, 'stripped_dark_oak_log'); G.set(GX + x, 7, GZ + 11, 'cut_brass'); }
  G.fill(GX - 13, 7, GZ + 11, GX + 13, 7, GZ + 11, 'dark_oak_planks');
  for (let z = -2; z <= 11; z++) { const y = 8 + Math.floor((11 - z) / 3.5); for (let x = -13; x <= 13; x++) G.set(GX + x, y, GZ + z, 'dark_oak_planks_stairs@N'.replace('@N', '@N')); }
  for (let z = -2; z <= 12; z++) { for (let x = -13; x <= 13; x++) { const y = 8 + Math.floor((11 - z) / 3.5); if (y > 8) G.set(GX + x, y - 1, GZ + z, 'dark_oak_planks'); } }
  for (let x = -12; x <= 12; x += 4) G.set(GX + x, 6, GZ + 10, 'lantern~');
  signAt(ctx, G, GX, 9.4, GZ + 11.55, 0, { text: "THE COMMANDER'S GALLERY", sub: 'choose how you fight', w: 6.4, h: 1.0, style: 'gold' });
  ctx.zone({ id: 'gallery', bom: ['command'], name: "The Commander's Gallery", color: '#c9a24a', rect: RECT.gallery, y: [5, 18], tour: { x: 36, y: 7, z: -58, yaw: 0, pitch: 8 },
    what: 'Where the player picks a commander class: six lit alcoves, one per class, each with a mannequin in that class\'s gear and a banner. Hero track: Champion (sword & shield), Reaver (two-hander), Warden (spear & bow). General track: Marshal (horn & banner), Warlock (staff & lantern), Quartermaster (supply banner).',
    builder: ['Free-standing open arcade 27 x 14 with dark oak columns and a sloped dark oak plank roof, back wall of stone bricks with six 3-wide alcoves (navy wool back, brass lintel, hanging lantern).', 'Each alcove: quartz slab dais, mannequin (armour stand with the class\'s equipment) and a gold sign.'] });

}
