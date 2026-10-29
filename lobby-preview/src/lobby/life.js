// Camp life: unnamed folk who patrol, spar, shoot, hammer and cook, plus bunting. Purely atmosphere; every one is optional for the builder.
import { folkAt, walkPath } from './kit.js';

export function buildLife(ctx) {
  const w = ctx.w;
  // training yard (pen at -49,0,26 facing east): sparring pairs and archers
  const Y = w.pen(-49, 0, 26, 3);
  folkAt(ctx, Y, 2, 0, -6.5, 0, { id: 'recruit', anim: 'swing' }); folkAt(ctx, Y, 2, 0, -4.5, 180, { id: 'recruit2', anim: 'swing', ph: 1.5 });
  folkAt(ctx, Y, 6, 0, -6.5, 0, { id: 'recruit2', anim: 'swing', ph: 0.7 }); folkAt(ctx, Y, 6, 0, -4.5, 180, { id: 'recruit', anim: 'swing', ph: 2.2 });
  for (const [x, ph] of [[-8, 0], [-2, 0.3], [4, 0.6]]) folkAt(ctx, Y, x + 0.5, 0, 3.6, 180, { id: 'archerRecruit', anim: 'shoot', ph });
  // armory smith at the forge corner (pen at -24,0,24 facing east)
  const A = w.pen(-24, 0, 24, 3);
  folkAt(ctx, A, 7.5, 0, 6.7, 0, { id: 'smith', anim: 'hammer' });
  // avenue and road walkers
  walkPath(ctx, { id: 'courier', y: 0, path: [[0, 50], [1, 21]], speed: 1.7 });
  walkPath(ctx, { id: 'folk1', y: 0, path: [[-10, 45], [-10, 18]], speed: 1.15 });
  walkPath(ctx, { id: 'folk2', y: 0, path: [[10, 19], [10, 44]], speed: 1.2 });
  walkPath(ctx, { id: 'folk3', y: 0, path: [[-17, 38], [-37, 38]], speed: 1.1 });
  walkPath(ctx, { id: 'folk4', y: 0, path: [[17, 40], [44, 40]], speed: 1.25 });
  walkPath(ctx, { id: 'folk1', y: 0, path: [[-6, 9], [6, 9], [18, 9]], speed: 1.0 });
  // gate guards and standard guards
  for (const x of [-19.5, 19.5]) ctx.folk({ x, y: 0, z: 47.6, yaw: 180, id: 'guardG' });
  for (const x of [-3.2, 3.2]) ctx.folk({ x, y: 16, z: -38.6, yaw: 180, id: 'guardH' });
  for (const x of [-7, 7]) ctx.folk({ x, y: 6, z: -29.5, yaw: 180, id: 'guardG' });
  // patrols along the terrace walls
  walkPath(ctx, { id: 'guardG', y: 6, path: [[-50, 1.5], [-12, 1.5]], speed: 1.15 });
  walkPath(ctx, { id: 'guardG', y: 6, path: [[50, 1.5], [12, 1.5]], speed: 1.15 });
  walkPath(ctx, { id: 'guardH', y: 16, path: [[-38, -38.6], [-8, -38.6]], speed: 1.1 });
  walkPath(ctx, { id: 'guardH', y: 16, path: [[38, -38.6], [8, -38.6]], speed: 1.1 });
  // the cook at the Companions' fire
  ctx.folk({ x: -24.9, y: 16, z: -49.4, yaw: 90, id: 'cook', anim: 'stir' });
  // a courier on the House Ring
  walkPath(ctx, { id: 'courier', y: 6, path: [[-14, -6], [14, -6], [14, -24], [-14, -24]], speed: 1.5 });

  // bunting ring over the spawn seal, strung between the four iron lamp posts
  const { x: sx, z: sz } = { x: 0, z: 30 };
  const posts = [];
  for (let i = 0; i < 8; i += 2) { const a = i * Math.PI / 4 + Math.PI / 8; posts.push([Math.round(sx + Math.cos(a) * 14) + 0.5, 5.4, Math.round(sz + Math.sin(a) * 14) + 0.5]); }
  for (let i = 0; i < 4; i++) ctx.bunting.push({ a: posts[i], to: posts[(i + 1) % 4], n: 16, sag: 1.6, len: 0.95 });
  // over the stair foot and the gate wall
  ctx.bunting.push({ a: [-9.5, 5.4, 17.5], to: [9.5, 5.4, 17.5], n: 14, sag: 1.3 });
  ctx.bunting.push({ a: [-8, 12.6, 48.2], to: [8, 12.6, 48.2], n: 14, sag: 1.2 });
}
