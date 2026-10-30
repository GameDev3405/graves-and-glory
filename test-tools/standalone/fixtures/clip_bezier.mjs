// Writes clip_bezier.json, the reference curves for Clip sampling, by running Blockbench's own code: BoneAnimator.interpolate
// (js/animations/timeline_animators.js) with the Keyframe.getLerp, getBezierLerp and getCatmullromLerp methods of
// js/animations/keyframe.js, the Math helpers of js/util/math_util.js and the bundled three.js, all read from the installed
// app's dist/bundle.js.map (sourcesContent). The format is 'free': no loop wrapping, no quaternions.
//   node test-tools/standalone/fixtures/clip_bezier.mjs "C:/Users/<you>/AppData/Local/Programs/Blockbench/resources/app.asar"
import fs from 'fs'; import os from 'os'; import path from 'path'; import url from 'url';

const asar = fs.readFileSync(process.argv[2]);
const header = JSON.parse(asar.subarray(16, 16 + asar.readUInt32LE(12)).toString('utf8')), base = 8 + asar.readUInt32LE(4);
const file = p => { let n = header; for (const s of p.split('/')) n = n.files[s]; const o = base + Number(n.offset); return asar.subarray(o, o + n.size).toString('utf8'); };
const version = JSON.parse(file('package.json')).version;
const map = JSON.parse(file('dist/bundle.js.map'));
const src = name => { const i = map.sources.indexOf(name); if (i < 0) throw new Error('no ' + name); return map.sourcesContent[i]; };

const threeFile = path.join(os.tmpdir(), 'blockbench-three-' + process.pid + '.mjs');
fs.writeFileSync(threeFile, src('../node_modules/three/build/three.module.js'));
const THREE = await import(url.pathToFileURL(threeFile)); fs.unlinkSync(threeFile);

const mathUtil = src('../js/util/math_util.js');
for (const name of ['getLerp', 'lerp', 'clamp', 'epsilon']) (0, eval)(mathUtil.match(new RegExp(String.raw`^Math\.` + name + String.raw` = function[\s\S]*?^}`, 'm'))[0]);
const getAxisNumber = (0, eval)('(' + mathUtil.match(/^export function getAxisNumber[\s\S]*?^}/m)[0].replace('export ', '') + ')');

// A class method of a Blockbench source file as a plain function, its free names bound to the stand-ins given.
function method(source, name, scope) {
    const start = source.indexOf('\t' + name + '('), open = source.indexOf('{', start);
    if (start < 0 || source.indexOf('\t' + name + '(', start + 1) >= 0) throw new Error('want one ' + name);
    let depth = 0, end = open;
    for (; end < source.length; end++) { if (source[end] == '{') depth++; else if (source[end] == '}' && --depth == 0) break; }
    const params = source.slice(source.indexOf('(', start) + 1, source.lastIndexOf(')', open));
    return new Function(...Object.keys(scope), 'return function(' + params + ')' + source.slice(open, end + 1))(...Object.values(scope));
}

// A keyframe as Blockbench holds one loaded from a project; calc reads a plain number the way its Molang parser does.
class Keyframe {
    constructor(time, interpolation, points, handles = {}) {
        Object.assign(this, {time, interpolation, data_points: points.map(([x, y, z]) => ({x: String(x), y: String(y), z: String(z)})),
            bezier_left_time: [-0.1, -0.1, -0.1], bezier_left_value: [0, 0, 0], bezier_right_time: [0.1, 0.1, 0.1], bezier_right_value: [0, 0, 0]}, handles);
    }
    calc(axis, data_point = 0) {
        if (data_point) data_point = Math.clamp(data_point, 0, this.data_points.length - 1);
        return parseFloat(this.data_points[data_point][axis]);
    }
}
Keyframe.interpolation = {linear: 'linear', catmullrom: 'catmullrom', bezier: 'bezier', step: 'step'};
const keyframeJs = src('../js/animations/keyframe.js');
for (const name of ['getLerp', 'getBezierLerp', 'getCatmullromLerp']) Keyframe.prototype[name] = method(keyframeJs, name, {THREE, getAxisNumber});
const interpolate = method(src('../js/animations/timeline_animators.js'), 'interpolate', {
    Format: {per_animator_rotation_interpolation: false, quaternion_interpolation: false, animation_loop_wrapping: false},
    Animator: {_last_values: {}}, Blockbench: {hasFlag: () => false, dispatchEvent: () => []}, Keyframe, Reusable: {}, getAxisNumber});

const asKey = k => {
    const o = {t: k.time, i: k.interpolation, v: ['x', 'y', 'z'].map(a => k.calc(a, 0))};
    if (k.data_points.length > 1) o.post = ['x', 'y', 'z'].map(a => k.calc(a, 1));
    if (k.interpolation == 'bezier') Object.assign(o, {lt: k.bezier_left_time, lv: k.bezier_left_value, rt: k.bezier_right_time, rv: k.bezier_right_value});
    return o;
};
const K = (t, i, points, handles) => new Keyframe(t, i, points, handles);

// reference: a split bezier pair whose handles reach past the gap (Blockbench clamps their times), and four catmullrom keys.
// cases: catmullrom through split keys; bezier into linear and into step keys (which keep the default handles); linear into step.
// cycle: a looping clip, so catmullrom's missing neighbours wrap round (with three keys or more), split keys included.
const clips = [
    {clip: 'reference', length: 1, loop: false, bones: {
        right_arm: {rotation: [
            K(0.2, 'bezier', [[10, -30, 5], [14, -30, 0]], {bezier_right_time: [0.15, 0.05, 0.9], bezier_right_value: [35, -10, 12]}),
            K(0.7, 'bezier', [[80, 20, -40]], {bezier_left_time: [-0.1, -0.35, -0.6], bezier_left_value: [-20, 15, 0]})]},
        body: {position: [K(0, 'catmullrom', [[0, 0, 0]]), K(0.25, 'catmullrom', [[2, -1.5, 0.5]]), K(0.5, 'catmullrom', [[-1, 3, 0]]), K(1, 'catmullrom', [[0.5, 0, -2]])]}}},
    {clip: 'cases', length: 1, loop: false, bones: {
        left_arm: {rotation: [K(0, 'catmullrom', [[0, 0, 0]]), K(0.3, 'catmullrom', [[20, -10, 5], [35, -4, 0]]),
            K(0.6, 'catmullrom', [[10, 15, -5]]), K(1, 'catmullrom', [[0, 5, 0], [-6, 2, 1]])]},
        head: {rotation: [K(0, 'bezier', [[0, 0, 0]], {bezier_right_time: [0.2, 0.05, 0.3], bezier_right_value: [15, -5, 8]}),
            K(0.4, 'linear', [[30, 10, -10]]),
            K(0.7, 'bezier', [[-10, 20, 5], [-4, 20, 5]], {bezier_left_time: [-0.1, -0.2, -0.05], bezier_left_value: [6, 0, -3], bezier_right_time: [0.25, 0.1, 0.5], bezier_right_value: [-8, 4, 0]}),
            K(1, 'step', [[5, 0, 0]])]},
        right_leg: {position: [K(0, 'linear', [[0, 0, 0]]), K(0.35, 'step', [[0, 1, -1]]), K(0.6, 'linear', [[0.5, -0.5, 1]]), K(1, 'step', [[0, 0, 0]])]}}},
    {clip: 'cycle', length: 1.2, loop: true, bones: {
        body: {rotation: [K(0, 'catmullrom', [[0, 0, 2]]), K(0.3, 'catmullrom', [[4, -2, 0]]), K(0.6, 'catmullrom', [[0, 3, -2]]),
            K(0.9, 'catmullrom', [[-4, 1, 0]]), K(1.2, 'catmullrom', [[0, 0, 2]])]},
        root: {position: [K(0, 'catmullrom', [[0, 0, 0]]), K(0.6, 'catmullrom', [[0, 1, 0], [0.5, 1.5, 0]]), K(1.2, 'catmullrom', [[0, 0, 0]])]},
        right_arm: {rotation: [K(0, 'catmullrom', [[-10, 0, 0]]), K(1.2, 'catmullrom', [[-10, 0, 0]])]},
        left_arm: {rotation: [K(0, 'linear', [[5, 0, 0]]), K(0.4, 'catmullrom', [[-15, 0, 4]]), K(0.8, 'linear', [[10, 0, -4]]), K(1.2, 'catmullrom', [[5, 0, 0]])]}}},
];

const out = [];
for (const c of clips) {
    const bones = {}, samples = [];
    for (const [bone, chans] of Object.entries(c.bones)) {
        bones[bone] = {};
        for (const [channel, keys] of Object.entries(chans)) {
            bones[bone][channel] = keys.map(asKey);
            const times = new Set();
            for (let n = 0; n < 20; n++) times.add(+(c.length * (n + 0.5) / 20).toFixed(6));
            for (const k of keys) for (const d of [-0.002, 0, 0.002]) times.add(+(k.time + d).toFixed(6));
            for (const t of [...times].sort((a, b) => a - b)) {
                if (t < 0 || t > c.length || (c.loop && t >= c.length)) continue;
                const animator = {animation: {time: t, loop: c.loop ? 'loop' : 'once', length: c.length}, [channel]: keys};
                samples.push({bone, channel, t, value: interpolate.call(animator, channel, false)});
            }
        }
    }
    out.push({clip: {format: 1, clip: c.clip, length: c.length, loop: c.loop, markers: {}, bones}, samples});
}

const fixture = {
    source: 'Blockbench ' + version + ' (resources/app.asar, dist/bundle.js.map sourcesContent): js/animations/timeline_animators.js '
        + 'BoneAnimator.interpolate with js/animations/keyframe.js Keyframe.getLerp, getBezierLerp and getCatmullromLerp, '
        + 'js/util/math_util.js and the bundled three.js r' + THREE.REVISION + ' (CubicBezierCurve, SplineCurve), in the free format; '
        + 'run by test-tools/standalone/fixtures/clip_bezier.mjs',
    clips: out,
};
const text = JSON.stringify(fixture, null, 1).replace(/\[\s+(-?[\d.e+-]+(?:,\s+-?[\d.e+-]+)*)\s+\]/g, (_, xs) => '[' + xs.split(/,\s+/).join(', ') + ']')
    .replace(/\{\s+("bone": [^{}]*?)\s+\}/g, (_, body) => '{' + body.replace(/,\s+/g, ', ') + '}');
fs.writeFileSync(url.fileURLToPath(new URL('clip_bezier.json', import.meta.url)), text + '\n');
console.log('clip_bezier.json: Blockbench ' + version + ', ' + out.map(c => c.clip.clip + ' ' + c.samples.length).join(', ') + ' samples');
