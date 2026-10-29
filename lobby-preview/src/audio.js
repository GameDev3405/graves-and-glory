// Procedural ambience (WebAudio): wind, low drone, fire crackle by proximity, footsteps, a distant war horn. No audio files.
export class Ambience {
  constructor(app) {
    this.app = app; this.on = false; this.ctx = null; this.vol = 0.5; this.nextHorn = 25; this.stepT = 0; this.crackleT = 0; this.fires = [];
  }
  scanFires(particles) { this.fires = particles.add.list.filter((p) => p.type === 'halo' && p.size > 5).map((p) => [p.x, p.y, p.z]); }
  start() {
    if (this.ctx) { this.ctx.resume(); this.on = true; this.master.gain.value = this.vol; return; }
    const AC = window.AudioContext || window.webkitAudioContext; if (!AC) return;
    const c = this.ctx = new AC(); this.on = true;
    this.master = c.createGain(); this.master.gain.value = this.vol; this.master.connect(c.destination);
    const sr = c.sampleRate, nb = c.createBuffer(1, sr * 3, sr), d = nb.getChannelData(0); let b0 = 0, b1 = 0, b2 = 0;
    for (let i = 0; i < d.length; i++) { const w = Math.random() * 2 - 1; b0 = 0.99886 * b0 + w * 0.0555179; b1 = 0.99332 * b1 + w * 0.0750759; b2 = 0.969 * b2 + w * 0.153852; d[i] = (b0 + b1 + b2 + w * 0.5362) * 0.11; }
    this.noise = nb;
    // wind: looped pink noise through a moving band-pass
    const wind = c.createBufferSource(); wind.buffer = nb; wind.loop = true;
    const bp = c.createBiquadFilter(); bp.type = 'bandpass'; bp.frequency.value = 420; bp.Q.value = 0.6;
    const wg = c.createGain(); wg.gain.value = 0.55; wind.connect(bp); bp.connect(wg); wg.connect(this.master); wind.start();
    const lfo = c.createOscillator(); lfo.frequency.value = 0.07; const lg = c.createGain(); lg.gain.value = 220; lfo.connect(lg); lg.connect(bp.frequency); lfo.start();
    const lfo2 = c.createOscillator(); lfo2.frequency.value = 0.11; const lg2 = c.createGain(); lg2.gain.value = 0.25; lfo2.connect(lg2); lg2.connect(wg.gain); lfo2.start();
    // drone
    for (const [f, g] of [[55, 0.05], [82.4, 0.035], [110.5, 0.02]]) { const o = c.createOscillator(); o.type = 'sine'; o.frequency.value = f; const og = c.createGain(); og.gain.value = g; o.connect(og); og.connect(this.master); o.start(); const v = c.createOscillator(); v.frequency.value = 0.05 + f * 0.0007; const vg = c.createGain(); vg.gain.value = f * 0.004; v.connect(vg); vg.connect(o.frequency); v.start(); }
    // reverb-ish echo bus for the horn
    this.echo = c.createDelay(1.5); this.echo.delayTime.value = 0.62; const fb = c.createGain(); fb.gain.value = 0.45; const lp = c.createBiquadFilter(); lp.type = 'lowpass'; lp.frequency.value = 900;
    this.echo.connect(lp); lp.connect(fb); fb.connect(this.echo); lp.connect(this.master);
  }
  stop() { this.on = false; if (this.master) this.master.gain.value = 0; }
  setVolume(v) { this.vol = v; if (this.master && this.on) this.master.gain.value = v; }
  burst(freq, q, dur, gain, type = 'lowpass') {
    const c = this.ctx; const s = c.createBufferSource(); s.buffer = this.noise; const f = c.createBiquadFilter(); f.type = type; f.frequency.value = freq; f.Q.value = q;
    const g = c.createGain(); const t = c.currentTime; g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(gain, t + 0.004); g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    s.connect(f); f.connect(g); g.connect(this.master); s.start(t, Math.random() * 2, dur + 0.05);
  }
  horn() {
    const c = this.ctx, t = c.currentTime; const g = c.createGain(); g.gain.value = 0; const lp = c.createBiquadFilter(); lp.type = 'lowpass'; lp.frequency.value = 520; lp.Q.value = 2;
    for (const [f, m] of [[116.5, 1], [174.6, 0.55], [233, 0.3]]) { const o = c.createOscillator(); o.type = 'sawtooth'; o.frequency.setValueAtTime(f * 0.93, t); o.frequency.linearRampToValueAtTime(f, t + 0.9); const og = c.createGain(); og.gain.value = 0.05 * m; o.connect(og); og.connect(lp); o.start(t); o.stop(t + 5.4); }
    lp.connect(g); g.connect(this.master); g.connect(this.echo);
    g.gain.linearRampToValueAtTime(0.9, t + 0.7); g.gain.setValueAtTime(0.9, t + 2.6); g.gain.exponentialRampToValueAtTime(0.0001, t + 5);
  }
  update(dt, t) {
    if (!this.on || !this.ctx) return; const A = this.app, p = A.player;
    this.nextHorn -= dt; if (this.nextHorn <= 0) { this.horn(); this.nextHorn = 70 + Math.random() * 60; }
    // footsteps
    const hs = Math.hypot(p.vel.x, p.vel.z);
    if (p.onGround && hs > 1 && !p.flying && !A.ui.paused) { this.stepT -= dt; if (this.stepT <= 0) { this.stepT = p.sprinting ? 0.29 : 0.43; const id = A.world.get(Math.floor(p.pos.x), Math.floor(p.pos.y - 0.2), Math.floor(p.pos.z)); const n = id ? A.BLOCKS[id].name : ''; const soft = /grass|dirt|moss|podzol|sand|carpet|wool/.test(n), wood = /planks|log|fence|barrel|crate/.test(n); this.burst(soft ? 380 : wood ? 700 : 1500, 0.8, soft ? 0.09 : 0.07, 0.2 + Math.random() * 0.08); } } else this.stepT = 0;
    // crackle near fires
    let near = 99; for (const f of this.fires) { const d = Math.hypot(f[0] - p.pos.x, f[1] - p.pos.y, f[2] - p.pos.z); if (d < near) near = d; }
    const lvl = Math.max(0, 1 - near / 22);
    this.crackleT -= dt; if (this.crackleT <= 0) { this.crackleT = 0.05 + Math.random() * 0.3; if (Math.random() < lvl * 1.4) this.burst(2500 + Math.random() * 3000, 1.2, 0.03 + Math.random() * 0.03, 0.04 + lvl * 0.14, 'highpass'); }
  }
}
