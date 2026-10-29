// Renderer pieces: block shader (baked light + time of day), sky dome, environment presets.
import * as THREE from '../vendor/three.module.min.js';

THREE.ColorManagement.enabled = false;   // all colour maths is done in plain display space

export const PRESETS = {
  dusk: {
    label: 'Dusk', top: 0x1a2040, mid: 0x4a3c6c, hor: 0xe8885c, sunDir: [-0.9, 0.07, 0.45], sunCol: 0xffa060, moonDir: [0.35, 0.55, -0.75],
    skyCol: [0.7, 0.66, 0.95], skyI: 0.8, blkCol: [1.0, 0.72, 0.42], amb: [0.08, 0.07, 0.11], fog: 0x7a6086, fogD: 0.0048, stars: 0.45, moon: 0.9, cloud: 0.5, mtn: 0x2a2442, mtn2: 0x3c3354, cloudCol: 0xe08a7a,
  },
  golden: {
    label: 'Golden hour', top: 0x4a6aa4, mid: 0xc0987c, hor: 0xffc070, sunDir: [-0.85, 0.2, 0.45], sunCol: 0xffd090, moonDir: [0.4, -0.6, -0.5],
    skyCol: [1.0, 0.86, 0.68], skyI: 0.98, blkCol: [1.0, 0.75, 0.45], amb: [0.1, 0.09, 0.12], fog: 0xd8aa84, fogD: 0.0065, stars: 0, moon: 0, cloud: 0.7, mtn: 0x5a4a62, mtn2: 0x7a6670, cloudCol: 0xffd0a0,
  },
  night: {
    label: 'Night', top: 0x050818, mid: 0x0c1633, hor: 0x1e3558, sunDir: [-0.5, -0.8, 0.3], sunCol: 0x000000, moonDir: [0.3, 0.6, -0.7],
    skyCol: [0.42, 0.52, 0.95], skyI: 0.42, blkCol: [1.0, 0.7, 0.4], amb: [0.045, 0.05, 0.09], fog: 0x0e1a30, fogD: 0.0085, stars: 1, moon: 1, cloud: 0.25, mtn: 0x0a1020, mtn2: 0x121b30, cloudCol: 0x2a3a60,
  },
  dawn: {
    label: 'Dawn', top: 0x3c5c8e, mid: 0xc09ab4, hor: 0xffb8a0, sunDir: [0.9, 0.08, 0.4], sunCol: 0xffc8a0, moonDir: [-0.5, 0.3, -0.7],
    skyCol: [0.95, 0.82, 0.92], skyI: 0.82, blkCol: [1.0, 0.75, 0.5], amb: [0.09, 0.08, 0.12], fog: 0xcfa0b0, fogD: 0.007, stars: 0.1, moon: 0.4, cloud: 0.6, mtn: 0x4a4a6c, mtn2: 0x6e6a88, cloudCol: 0xffc8c0,
  },
  noon: {
    label: 'Noon (blueprint-clear)', top: 0x3f86d8, mid: 0x8cc0ee, hor: 0xcfe6fa, sunDir: [-0.2, 0.9, 0.25], sunCol: 0xfff4d8, moonDir: [0.4, -0.6, -0.5],
    skyCol: [1.0, 0.98, 0.94], skyI: 1.0, blkCol: [1.0, 0.8, 0.55], amb: [0.2, 0.2, 0.24], fog: 0xbcd8f0, fogD: 0.004, stars: 0, moon: 0, cloud: 0.9, mtn: 0x7a96b0, mtn2: 0x9ab4c8, cloudCol: 0xffffff,
  },
};
export const PRESET_ORDER = ['dusk', 'golden', 'night', 'dawn', 'noon'];

const col3 = (h) => new THREE.Color(h);

const VERT = /* glsl */`
attribute vec4 aux;
varying vec2 vUv; varying vec4 vAux; varying float vFogDist; varying vec3 vWorld;
void main(){
  vUv = uv; vAux = aux;
  vec4 mv = modelViewMatrix * vec4(position,1.0);
  vFogDist = length(mv.xyz); vWorld = position;
  gl_Position = projectionMatrix * mv;
}`;
const FRAG = /* glsl */`
precision highp float;
uniform sampler2D uMap; uniform vec3 uSkyCol; uniform vec3 uBlkCol; uniform vec3 uAmb; uniform vec3 uFogCol; uniform float uFogD; uniform float uFlicker; uniform float uBright; uniform float uAlphaMode; uniform float uTime;
varying vec2 vUv; varying vec4 vAux; varying float vFogDist; varying vec3 vWorld;
float curve(float x){ return pow(0.845, (1.0 - x) * 15.0); }
void main(){
  vec4 t = texture2D(uMap, vUv);
  if (uAlphaMode < 0.5) { if (t.a < 0.5) discard; } else { if (t.a < 0.04) discard; }
  float ao = vAux.x;
  vec3 light = uSkyCol * curve(vAux.y) + uBlkCol * (curve(vAux.z) * uFlicker) + uAmb;
  light = mix(light, vec3(1.0), uBright);
  vec3 c = t.rgb * light * mix(ao, 1.0, uBright * 0.6);
  c = max(c, t.rgb * vAux.w * vec3(1.0, 0.96, 0.88));
  c = c / (1.0 + max(c - 0.85, 0.0) * 0.6);
  float f = 1.0 - exp(-pow(vFogDist * uFogD, 2.0));
  c = mix(c, uFogCol, clamp(f, 0.0, 1.0));
  gl_FragColor = vec4(c, uAlphaMode < 0.5 ? 1.0 : t.a);
}`;

const SKY_VERT = /* glsl */`
varying vec3 vDir;
void main(){ vDir = position; vec4 p = projectionMatrix * modelViewMatrix * vec4(position,1.0); gl_Position = p.xyww; }`;
const SKY_FRAG = /* glsl */`
precision highp float;
varying vec3 vDir;
uniform vec3 uTop, uMid, uHor, uSunDir, uSunCol, uMoonDir, uMtn, uMtn2, uCloudCol;
uniform float uStars, uMoon, uCloud, uTime;
float h1(vec3 p){ p = fract(p*0.3183099+.1); p *= 17.0; return fract(p.x*p.y*p.z*(p.x+p.y+p.z)); }
float h2(vec2 p){ return fract(sin(dot(p, vec2(127.1,311.7)))*43758.5453); }
float n2(vec2 p){ vec2 i=floor(p), f=fract(p); f=f*f*(3.0-2.0*f); return mix(mix(h2(i),h2(i+vec2(1,0)),f.x), mix(h2(i+vec2(0,1)),h2(i+vec2(1,1)),f.x), f.y); }
float fbm(vec2 p){ float a=0.5, s=0.0; for(int i=0;i<5;i++){ s+=a*n2(p); p*=2.03; a*=0.5; } return s; }
void main(){
  vec3 d = normalize(vDir);
  float h = d.y;
  vec3 col = mix(uHor, uMid, smoothstep(0.0, 0.22, h));
  col = mix(col, uTop, smoothstep(0.14, 0.75, h));
  col = mix(col, uHor * 0.55, smoothstep(0.0, -0.35, h));
  float az = atan(d.z, d.x);
  // stars
  if (uStars > 0.0 && h > 0.0) {
    vec3 sp = floor(d * 260.0);
    float r = h1(sp);
    float tw = 0.7 + 0.3 * sin(uTime * 2.0 + r * 60.0);
    col += vec3(1.0, 0.95, 0.9) * step(0.9975, r) * tw * uStars * smoothstep(0.0, 0.25, h) * (0.6 + 0.8*h1(sp+3.0));
  }
  // sun + moon
  float sd = max(dot(d, normalize(uSunDir)), 0.0);
  col += uSunCol * (pow(sd, 80.0) * 0.9 + pow(sd, 7.0) * 0.28);
  col = mix(col, uSunCol * 1.5, smoothstep(0.9992, 0.9996, sd));
  float md = dot(d, normalize(uMoonDir));
  col += vec3(0.5, 0.6, 0.9) * pow(max(md, 0.0), 60.0) * 0.35 * uMoon;
  float md_ = smoothstep(0.9984, 0.9988, md);
  col = mix(col, vec3(0.9, 0.93, 1.0) * (0.92 + 0.08*n2(d.xz*400.0)), md_ * uMoon);
  // clouds (projected onto a plane)
  if (h > 0.02) {
    vec2 cp = d.xz / (h + 0.18) * 1.6 + vec2(uTime * 0.012, 0.0);
    float cl = fbm(cp * 1.3);
    float m = smoothstep(0.52, 0.78, cl) * uCloud * smoothstep(0.02, 0.2, h);
    vec3 cc = uCloudCol * (0.55 + 0.6 * smoothstep(0.5, 0.8, cl)) + uHor * 0.15;
    col = mix(col, cc, m * 0.75);
  }
  // distant mountain ranges
  float m2 = 0.035 + 0.075 * fbm(vec2(az * 2.3, 1.7)) + 0.03 * fbm(vec2(az * 9.0, 4.0));
  float m1 = 0.012 + 0.055 * fbm(vec2(az * 3.7 + 9.0, 7.7)) + 0.02 * fbm(vec2(az * 14.0, 2.0));
  vec3 fogMtn = mix(uMtn2, uHor, 0.45);
  if (h < m2) col = mix(col, fogMtn, 0.95);
  if (h < m1) col = mix(uMtn, uHor, 0.18);
  if (h < -0.02) col = mix(col, uMtn * 0.8, smoothstep(-0.02, -0.12, h));
  gl_FragColor = vec4(col, 1.0);
}`;

export class Env {
  constructor(scene, atlasTexture) {
    this.scene = scene;
    this.uniforms = {
      uMap: { value: atlasTexture }, uSkyCol: { value: new THREE.Color() }, uBlkCol: { value: new THREE.Color() }, uAmb: { value: new THREE.Color() },
      uFogCol: { value: new THREE.Color() }, uFogD: { value: 0.01 }, uFlicker: { value: 1 }, uBright: { value: 0 }, uTime: { value: 0 },
    };
    const mk = (mode, transparent) => new THREE.ShaderMaterial({
      vertexShader: VERT, fragmentShader: FRAG,
      uniforms: { ...this.uniforms, uAlphaMode: { value: mode } },
      transparent, depthWrite: !transparent, side: THREE.FrontSide,
    });
    this.matSolid = mk(0, false);
    this.matGlass = mk(1, true);
    this.matGlass.side = THREE.DoubleSide;
    this.skyU = {
      uTop: { value: new THREE.Color() }, uMid: { value: new THREE.Color() }, uHor: { value: new THREE.Color() }, uSunDir: { value: new THREE.Vector3() },
      uSunCol: { value: new THREE.Color() }, uMoonDir: { value: new THREE.Vector3() }, uMtn: { value: new THREE.Color() }, uMtn2: { value: new THREE.Color() },
      uCloudCol: { value: new THREE.Color() }, uStars: { value: 0 }, uMoon: { value: 0 }, uCloud: { value: 0 }, uTime: { value: 0 },
    };
    this.sky = new THREE.Mesh(new THREE.SphereGeometry(500, 48, 24), new THREE.ShaderMaterial({ vertexShader: SKY_VERT, fragmentShader: SKY_FRAG, uniforms: this.skyU, side: THREE.BackSide, depthWrite: false }));
    this.sky.frustumCulled = false; this.sky.renderOrder = -10;
    scene.add(this.sky);
    this.preset = null; this.name = 'dusk';
    this.entityMats = [];       // {mat, base:Color, pos:[sky,blk]} registered by entity code
    this.onChange = [];
  }
  get materials() { return { solid: this.matSolid, glass: this.matGlass }; }
  setPreset(name) {
    const p = PRESETS[name]; if (!p) return;
    this.name = name; this.preset = p;
    const u = this.uniforms;
    u.uSkyCol.value.setRGB(p.skyCol[0] * p.skyI, p.skyCol[1] * p.skyI, p.skyCol[2] * p.skyI);
    u.uBlkCol.value.setRGB(...p.blkCol);
    u.uAmb.value.setRGB(...p.amb);
    u.uFogCol.value.set(p.fog); u.uFogD.value = p.fogD;
    this.matSolid.uniforms.uFogD = u.uFogD; this.matGlass.uniforms.uFogD = u.uFogD;
    const s = this.skyU;
    s.uTop.value.set(p.top); s.uMid.value.set(p.mid); s.uHor.value.set(p.hor);
    s.uSunDir.value.set(...p.sunDir).normalize(); s.uSunCol.value.set(p.sunCol); s.uMoonDir.value.set(...p.moonDir).normalize();
    s.uMtn.value.set(p.mtn); s.uMtn2.value.set(p.mtn2); s.uCloudCol.value.set(p.cloudCol);
    s.uStars.value = p.stars; s.uMoon.value = p.moon; s.uCloud.value = p.cloud;
    for (const f of this.onChange) f(this);
  }
  // colour a lit entity: sky/block light levels 0..15
  entityLight(sky, blk, out = new THREE.Color()) {
    const p = this.preset, curve = (l) => Math.pow(0.845, 15 - l);
    const a = curve(sky) * p.skyI, b = curve(blk);
    out.setRGB(Math.min(1.25, p.skyCol[0] * a + p.blkCol[0] * b + p.amb[0]), Math.min(1.25, p.skyCol[1] * a + p.blkCol[1] * b + p.amb[1]), Math.min(1.25, p.skyCol[2] * a + p.blkCol[2] * b + p.amb[2]));
    const br = this.uniforms.uBright.value;
    if (br > 0) out.lerp(new THREE.Color(1, 1, 1), br);
    return out;
  }
  setBright(v) { this.uniforms.uBright.value = v; this.matSolid.uniforms.uBright = this.uniforms.uBright; this.matGlass.uniforms.uBright = this.uniforms.uBright; for (const f of this.onChange) f(this); }
  update(t, camera) {
    this.uniforms.uTime.value = t; this.skyU.uTime.value = t;
    this.uniforms.uFlicker.value = 1 + 0.05 * Math.sin(t * 9.1) + 0.035 * Math.sin(t * 23.7 + 1.3) + 0.02 * Math.sin(t * 41.0);
    this.sky.position.copy(camera.position);
  }
}
