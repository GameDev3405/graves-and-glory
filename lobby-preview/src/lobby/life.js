// Camp life: unnamed folk who patrol, spar, shoot, hammer and cook, plus bunting. Purely atmosphere; every one is optional for the builder.
import { folkAt, walkPath } from './kit.js';
import { PLACE, SEAL } from './layout.js';

export function buildLife(ctx) {
  const w = ctx.w;
  // training yard: sparring pairs and archers
  const Yp = PLACE.yard, Y = w.pen(Yp.x, Yp.y, Yp.z, Yp.rot);
  folkAt(ctx, Y, 2, 0, -6.5, 0, { id: 'recruit', anim: 'swing' }); folkAt(ctx, Y, 2, 0, -4.5, 180, { id: 'recruit2', anim: 'swing', ph: 1.5 });
  folkAt(ctx, Y, 6, 0, -6.5, 0, { id: 'recruit2', anim: 'swing', ph: 0.7 }); folkAt(ctx, Y, 6, 0, -4.5, 180, { id: 'recruit', anim: 'swing', ph: 2.2 });
  for (const [x, ph] of [[-8, 0], [-2, 0.3], [4, 0.6]]) folkAt(ctx, Y, x + 0.5, 0, 3.6, 180, { id: 'archerRecruit', anim: 'shoot', ph });
  // armory smith at the forge corner
  const Ap = PLACE.armory, A = w.pen(Ap.x, Ap.y, Ap.z, Ap.rot);
  folkAt(ctx, A, 7.5, 0, 6.7, 0, { id: 'smith', anim: 'hammer' });
  // plaza walkers
  walkPath(ctx, { id: 'courier', y: 0, path: [[0, 72], [1, 41]], speed: 1.7 });
  walkPath(ctx, { id: 'folk1', y: 0, path: [[-16, 72], [-16, 32]], speed: 1.15 });
  walkPath(ctx, { id: 'folk2', y: 0, path: [[16, 32], [16, 72]], speed: 1.2 });
  walkPath(ctx, { id: 'folk3', y: 0, path: [[-22, 44], [-38, 44]], speed: 1.1 });
  walkPath(ctx, { id: 'folk4', y: 0, path: [[22, 44], [37, 44]], speed: 1.25 });
  walkPath(ctx, { id: 'folk1', y: 0, path: [[-30, 36], [0, 36], [30, 36]], speed: 1.0 });
  // gate guards, stair guards, standard guards
  for (const x of [-19.5, 19.5]) ctx.folk({ x, y: 0, z: 75.6, yaw: 180, id: 'guardG' });
  for (const x of [-3.2, 3.2]) ctx.folk({ x, y: 12, z: -4.2, yaw: 180, id: 'guardH' });
  for (const x of [-7, 7]) ctx.folk({ x, y: 6, z: 24.4, yaw: 180, id: 'guardG' });
  // patrols along the Chapter Wall promenade and around the brow of the hill
  walkPath(ctx, { id: 'guardG', y: 6, path: [[-62, 24], [-12, 24]], speed: 1.15 });
  walkPath(ctx, { id: 'guardG', y: 6, path: [[62, 24], [12, 24]], speed: 1.15 });
  walkPath(ctx, { id: 'guardH', y: 12, path: [[-12, -10], [-14.5, -22], [-9, -34], [0, -36.5], [9, -34], [14.5, -22], [12, -10]], speed: 1.1 });
  // the cook at the Companions' fire
  ctx.folk({ x: PLACE.fire.x + 2.1, y: 6, z: PLACE.fire.z + 0.6, yaw: 90, id: 'cook', anim: 'stir' });
  // a courier on the House Terrace, on the loop around the hill
  walkPath(ctx, { id: 'courier', y: 6, path: [[-30, 6], [30, 6], [30, -50], [-30, -50]], speed: 1.5 });

  // bunting ring over the spawn seal, strung between the four iron lamp posts
  const { x: sx, z: sz } = SEAL;
  const posts = [];
  for (let i = 0; i < 8; i += 2) { const a = i * Math.PI / 4 + Math.PI / 8; posts.push([Math.round(sx + Math.cos(a) * 14) + 0.5, 5.4, Math.round(sz + Math.sin(a) * 14) + 0.5]); }
  for (let i = 0; i < 4; i++) ctx.bunting.push({ a: posts[i], to: posts[(i + 1) % 4], n: 16, sag: 1.6, len: 0.95 });
  // over the stair foot and the gate wall
  ctx.bunting.push({ a: [-9.5, 5.4, 39.5], to: [9.5, 5.4, 39.5], n: 14, sag: 1.3 });
  ctx.bunting.push({ a: [-8, 12.6, 76.2], to: [8, 12.6, 76.2], n: 14, sag: 1.2 });
}
