// Writes clip_bezier.json, the reference curve for Clip sampling, by running Blockbench's own keyframe code: the
// Keyframe.getBezierLerp and Keyframe.getCatmullromLerp methods of js/animations/keyframe.js, the Math helpers of
// js/util/math_util.js and the bundled three.js, all read from the installed app's dist/bundle.js.map (sourcesContent).
// The choice of lerp follows BoneAnimator.interpolate (js/animations/timeline_animators.js) for samples between two keys.
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
for (const name of ['getLerp', 'lerp', 'clamp']) (0, eval)(mathUtil.match(new RegExp(String.raw`^Math\.` + name + String.raw` = function[\s\S]*?^}`, 'm'))[0]);
const getAxisNumber = (0, eval)('(' + mathUtil.match(/^export function getAxisNumber[\s\S]*?^}/m)[0].replace('export ', '') + ')');

const keyframeJs = src('../js/animations/keyframe.js');
function method(name) {
    const start = keyframeJs.indexOf('\t' + name + '('), open = keyframeJs.indexOf('{', start);
    let depth = 0, end = open;
    for (; end < keyframeJs.length; end++) { if (keyframeJs[end] == '{') depth++; else if (keyframeJs[end] == '}' && --depth == 0) break; }
    const params = keyframeJs.slice(keyframeJs.indexOf('(', start) + 1, keyframeJs.lastIndexOf(')', open));
    return new Function('THREE', 'getAxisNumber', 'return function(' + params + ')' + keyframeJs.slice(open, end + 1))(THREE, getAxisNumber);
}
const getBezierLerp = method('getBezierLerp'), getCatmullromLerp = method('getCatmullromLerp');

// A keyframe as the methods read it; calc reads a plain number the way Blockbench's Molang parser does.
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
function interpolate(keys, time) {   // BoneAnimator.interpolate, between two keys, not looping, no quaternions
    let before = false, after = false;
    for (const k of keys) {
        if (k.time < time) { if (!before || k.time > before.time) before = k; }
        else if (!after || k.time < after.time) after = k;
    }
    const alpha = Math.getLerp(before.time, after.time, time);
    if (before.interpolation === 'catmullrom' || after.interpolation === 'catmullrom') {
        const sorted = keys.slice().sort((a, b) => a.time - b.time), i = sorted.indexOf(before);
        return ['x', 'y', 'z'].map(axis => getCatmullromLerp(sorted[i - 1], before, after, sorted[i + 2], axis, alpha));
    }
    return ['x', 'y', 'z'].map(axis => getBezierLerp(before, after, axis, alpha));
}
const asKey = k => {
    const o = {t: k.time, i: k.interpolation, v: k.data_points[0] && ['x', 'y', 'z'].map(a => k.calc(a, 0))};
    if (k.data_points.length > 1) o.post = ['x', 'y', 'z'].map(a => k.calc(a, 1));
    if (k.interpolation == 'bezier') Object.assign(o, {lt: k.bezier_left_time, lv: k.bezier_left_value, rt: k.bezier_right_time, rv: k.bezier_right_value});
    return o;
};

// The bezier pair: the first key has a second data point (the curve leaves from it), and one handle on each side reaches past
// the gap between the keys, so Blockbench's clamp of the handle times is in the curve.
const bezier = [
    new Keyframe(0.2, 'bezier', [[10, -30, 5], [14, -30, 0]], {bezier_right_time: [0.15, 0.05, 0.9], bezier_right_value: [35, -10, 12]}),
    new Keyframe(0.7, 'bezier', [[80, 20, -40]], {bezier_left_time: [-0.1, -0.35, -0.6], bezier_left_value: [-20, 15, 0]}),
];
const catmullrom = [
    new Keyframe(0, 'catmullrom', [[0, 0, 0]]), new Keyframe(0.25, 'catmullrom', [[2, -1.5, 0.5]]),
    new Keyframe(0.5, 'catmullrom', [[-1, 3, 0]]), new Keyframe(1, 'catmullrom', [[0.5, 0, -2]]),
];
const samples = [];
for (const t of [0.25, 0.35, 0.45, 0.55, 0.65]) samples.push({bone: 'right_arm', channel: 'rotation', t, value: interpolate(bezier, t)});
for (const t of [0.1, 0.3, 0.45, 0.6, 0.9]) samples.push({bone: 'body', channel: 'position', t, value: interpolate(catmullrom, t)});

const fixture = {
    source: 'Blockbench ' + version + ' (resources/app.asar, dist/bundle.js.map sourcesContent): js/animations/keyframe.js '
        + 'Keyframe.getBezierLerp and Keyframe.getCatmullromLerp, with js/util/math_util.js and the bundled three.js '
        + 'r' + THREE.REVISION + ' (CubicBezierCurve, SplineCurve), lerp chosen as js/animations/timeline_animators.js '
        + 'BoneAnimator.interpolate chooses it; run by test-tools/standalone/fixtures/clip_bezier.mjs',
    clip: {format: 1, clip: 'reference', length: 1, loop: false, markers: {},
        bones: {right_arm: {rotation: bezier.map(asKey)}, body: {position: catmullrom.map(asKey)}}},
    samples,
};
fs.writeFileSync(url.fileURLToPath(new URL('clip_bezier.json', import.meta.url)), JSON.stringify(fixture, null, 1) + '\n');
console.log('clip_bezier.json: Blockbench ' + version + ', ' + samples.length + ' samples');
