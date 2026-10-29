import { buildTerrain } from './terrain.js';
export async function buildLobby(ctx, progress = () => {}) {
  const steps = [
    ['Shaping the ridge', buildTerrain],
  ];
  const extra = (await import('./steps.js')).STEPS;
  for (const s of [...steps, ...extra]) { progress(s[0]); await new Promise(r => setTimeout(r, 0)); s[1](ctx); }
}
