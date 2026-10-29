// First-person controller with Minecraft-ish movement, shape-aware collision, step-up, swimming and creative flight.
import * as THREE from '../vendor/three.module.min.js';
import { collisionBoxes, WX0, WX1, WZ0, WZ1, WY0 } from './world.js';
import { BLOCKS } from './blocks.js';

const HALF = 0.3, HEIGHT = 1.8, EYE = 1.62, STEP = 0.6;
const GRAV = 30, JUMP = 8.6, WALK = 4.4, SPRINT = 6.6, FLY = 11, FLY_FAST = 24;

export class Player {
  constructor(world, camera) {
    this.w = world; this.camera = camera;
    this.pos = new THREE.Vector3(0, 10, 0);
    this.vel = new THREE.Vector3();
    this.yaw = 0; this.pitch = 0;           // yaw 0 = looking north (-z)
    this.onGround = false; this.flying = false; this.inWater = false;
    this.keys = new Set();
    this.lastSpace = 0; this.bob = 0; this.bobAmt = 0; this.sens = 0.0022;
    this.sprinting = false; this.fovBase = 74; this.enableBob = true;
    this.frozen = false;
  }
  teleport(x, y, z, yaw, pitch = 0) { this.pos.set(x, y, z); this.vel.set(0, 0, 0); if (yaw !== undefined) this.yaw = yaw; this.pitch = pitch; }
  look(dx, dy) {
    this.yaw -= dx * this.sens; this.pitch -= dy * this.sens;
    this.pitch = Math.max(-1.55, Math.min(1.55, this.pitch));
  }
  press(code) {
    if (code === 'Space') {
      const now = performance.now();
      if (!this.keys.has('Space') && now - this.lastSpace < 280) { this.flying = !this.flying; this.vel.y = 0; }
      if (!this.keys.has('Space')) this.lastSpace = now;
    }
    this.keys.add(code);
  }
  release(code) { this.keys.delete(code); }
  forwardVec(out = new THREE.Vector3()) { return out.set(-Math.sin(this.yaw) * Math.cos(this.pitch), Math.sin(this.pitch), -Math.cos(this.yaw) * Math.cos(this.pitch)); }

  // collide the player box (min..max) against world; returns list of overlapping boxes (world units)
  boxesIn(minX, minY, minZ, maxX, maxY, maxZ) {
    const out = [];
    for (let y = Math.floor(minY); y <= Math.floor(maxY); y++) for (let z = Math.floor(minZ); z <= Math.floor(maxZ); z++) for (let x = Math.floor(minX); x <= Math.floor(maxX); x++) {
      const bs = collisionBoxes(this.w, x, y, z); if (!bs) continue;
      for (const b of bs) {
        const bx0 = x + b[0] / 16, by0 = y + b[1] / 16, bz0 = z + b[2] / 16, bx1 = x + b[3] / 16, by1 = y + b[4] / 16, bz1 = z + b[5] / 16;
        if (bx1 > minX && bx0 < maxX && by1 > minY && by0 < maxY && bz1 > minZ && bz0 < maxZ) out.push([bx0, by0, bz0, bx1, by1, bz1]);
      }
    }
    return out;
  }
  collides(px, py, pz) { return this.boxesIn(px - HALF, py + 0.001, pz - HALF, px + HALF, py + HEIGHT, pz + HALF).length > 0; }

  moveAxis(axis, d) {
    if (d === 0) return false;
    const p = this.pos;
    const target = p[axis] + d;
    const old = p[axis];
    p[axis] = target;
    const boxes = this.boxesIn(p.x - HALF, p.y + 0.001, p.z - HALF, p.x + HALF, p.y + HEIGHT, p.z + HALF);
    if (!boxes.length) return false;
    // blocked: attempt step-up for horizontal moves
    if (axis !== 'y' && (this.onGround || this.stepAssist) && !this.flying) {
      let top = -Infinity;
      for (const b of boxes) top = Math.max(top, b[4]);
      const need = top - p.y;
      if (need > 0 && need <= STEP + 0.01) {
        const oy = p.y; p.y = top + 0.002;
        if (!this.collides(p.x, p.y, p.z)) { this.stepped = true; return false; }
        p.y = oy;
      }
    }
    // resolve
    if (d > 0) { let lim = Infinity; for (const b of boxes) lim = Math.min(lim, axis === 'x' ? b[0] - HALF : axis === 'y' ? b[1] - HEIGHT : b[2] - HALF); p[axis] = lim - 0.0005; }
    else { let lim = -Infinity; for (const b of boxes) lim = Math.max(lim, axis === 'x' ? b[3] + HALF : axis === 'y' ? b[4] : b[5] + HALF); p[axis] = lim + 0.0005; }
    if (Math.abs(p[axis] - old) > Math.abs(d)) p[axis] = old;
    return true;
  }

  update(dt) {
    dt = Math.min(dt, 0.05);
    const k = this.keys;
    // intent
    let fx = 0, fz = 0;
    if (!this.frozen) {
      if (k.has('KeyW') || k.has('ArrowUp')) fz += 1;
      if (k.has('KeyS') || k.has('ArrowDown')) fz -= 1;
      if (k.has('KeyD') || k.has('ArrowRight')) fx += 1;
      if (k.has('KeyA') || k.has('ArrowLeft')) fx -= 1;
    }
    const len = Math.hypot(fx, fz) || 1; fx /= len; fz /= len;
    const sin = Math.sin(this.yaw), cos = Math.cos(this.yaw);
    const wishX = (-sin * fz) + (cos * fx), wishZ = (-cos * fz) - (sin * fx);
    const sprint = (k.has('ShiftLeft') || k.has('ShiftRight')) && fz > 0;
    this.sprinting = sprint;
    const p = this.pos;
    // water
    const feetId = this.w.get(Math.floor(p.x), Math.floor(p.y + 0.3), Math.floor(p.z));
    this.inWater = !!(feetId && BLOCKS[feetId].lq === 'water');

    if (this.flying) {
      const sp = sprint ? FLY_FAST : FLY;
      this.vel.x += (wishX * sp - this.vel.x) * Math.min(1, dt * 9);
      this.vel.z += (wishZ * sp - this.vel.z) * Math.min(1, dt * 9);
      let vy = 0; if (k.has('Space') && !this.frozen) vy += sp * 0.7; if ((k.has('KeyC') || k.has('ControlLeft')) && !this.frozen) vy -= sp * 0.7;
      this.vel.y += (vy - this.vel.y) * Math.min(1, dt * 9);
    } else {
      const sp = (sprint ? SPRINT : k.has('KeyC') ? WALK * 0.4 : WALK) * (this.inWater ? 0.55 : 1);
      const acc = this.onGround ? 14 : 3.2;
      this.vel.x += (wishX * sp - this.vel.x) * Math.min(1, dt * acc);
      this.vel.z += (wishZ * sp - this.vel.z) * Math.min(1, dt * acc);
      if (this.inWater) {
        this.vel.y -= 5 * dt; this.vel.y *= Math.pow(0.05, dt);
        if (k.has('Space') && !this.frozen) this.vel.y = Math.min(this.vel.y + 22 * dt, 3.6);
      } else {
        this.vel.y -= GRAV * dt;
        if (k.has('Space') && this.onGround && !this.frozen) { this.vel.y = JUMP; this.onGround = false; }
      }
    }
    // integrate (substeps keep us from tunnelling)
    const steps = Math.max(1, Math.ceil(Math.max(Math.abs(this.vel.x), Math.abs(this.vel.y), Math.abs(this.vel.z)) * dt / 0.25));
    const sd = dt / steps;
    this.stepAssist = false;
    for (let i = 0; i < steps; i++) {
      this.stepped = false;
      const hitX = this.moveAxis('x', this.vel.x * sd); if (hitX) this.vel.x = 0;
      const hitZ = this.moveAxis('z', this.vel.z * sd); if (hitZ) this.vel.z = 0;
      const dy = this.vel.y * sd;
      const hitY = this.moveAxis('y', dy);
      if (hitY) { if (dy < 0) this.onGround = true; this.vel.y = 0; } else if (dy !== 0) this.onGround = false;
      if (this.stepped) this.onGround = true;
    }
    if (this.flying && this.onGround && !k.has('Space')) this.flying = false;
    p.x = Math.max(WX0 + 2, Math.min(WX1 - 2, p.x)); p.z = Math.max(WZ0 + 2, Math.min(WZ1 - 2, p.z));
    if (p.y < WY0 + 1) { p.y = 12; this.vel.set(0, 0, 0); }

    // view bob
    const hs = Math.hypot(this.vel.x, this.vel.z);
    if (this.onGround && hs > 0.5 && !this.flying) { this.bob += dt * hs * 1.9; this.bobAmt += (1 - this.bobAmt) * Math.min(1, dt * 8); } else this.bobAmt *= Math.max(0, 1 - dt * 8);
    const bobY = this.enableBob ? Math.abs(Math.sin(this.bob)) * 0.045 * this.bobAmt : 0;
    const bobX = this.enableBob ? Math.sin(this.bob) * 0.02 * this.bobAmt : 0;
    const eye = k.has('KeyC') && !this.flying ? EYE - 0.25 : EYE;
    this.eyeH = this.eyeH === undefined ? eye : this.eyeH + (eye - this.eyeH) * Math.min(1, dt * 14);
    this.camera.position.set(p.x + Math.cos(this.yaw) * bobX, p.y + this.eyeH + bobY, p.z - Math.sin(this.yaw) * bobX);
    this.camera.rotation.order = 'YXZ';
    this.camera.rotation.set(this.pitch, this.yaw, 0);
    const targetFov = this.fovBase + (this.sprinting && hs > 5 ? 6 : 0);
    if (Math.abs(this.camera.fov - targetFov) > 0.05) { this.camera.fov += (targetFov - this.camera.fov) * Math.min(1, dt * 8); this.camera.updateProjectionMatrix(); }
  }
}
