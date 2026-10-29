// Single source of truth for where everything stands. x east, z south, y 0 = Minecraft y64.
// Lower Camp (y 0) in the south, the House Terrace (y 6) behind the Chapter Wall, the Command Hill (y 12) in the middle of the terrace.
export const CAMP = { x1: -72, z1: 27, x2: 72, z2: 94, h: 0 };
export const WALL_Z = 26;                       // the Founders' Chapter Wall (last row of the terrace)
export const TERRACE = { h: 6, poly: [[-76, 26.5], [76, 26.5], [76, -34], [74, -72], [60, -100], [30, -110], [-30, -110], [-60, -100], [-74, -72], [-76, -34]] };
export const HILL = { x: 0, z: -22, h: 12, r: 17, wall: 18.5 };   // flat top radius, outer wall radius
export const SEAL = { x: 0, z: 53 };
export const MOSAIC = { x: 0, z: 13, r: 11 };
export const GATES_Z = 80;
export const RIVER_Z = 104;
// pen origins and rotations (0 front south, 1 front west, 2 front north, 3 front east)
export const PLACE = {
  gilded:   { x: -58, y: 6, z: -2,  rot: 3 },
  wildmoot: { x: -58, y: 6, z: -42, rot: 3 },
  ironclad: { x: -38, y: 6, z: -77, rot: 0 },
  tide:     { x: 58,  y: 6, z: -40, rot: 1 },
  grave:    { x: 57,  y: 6, z: -2,  rot: 1 },
  armory:   { x: -52, y: 0, z: 44,  rot: 3 },
  yard:     { x: -58, y: 0, z: 76,  rot: 3 },
  muster:   { x: 52,  y: 0, z: 44,  rot: 1 },
  archive:  { x: 58,  y: 0, z: 76,  rot: 1 },
  gates:    { x: 0,   y: 0, z: GATES_Z, rot: 0 },
  gallery:  { x: 36,  y: 6, z: -80 },     // back wall centre (GX, GZ)
  fire:     { x: 0,   y: 6, z: -92 },
};
// zone rectangles [x1, z1, x2, z2] (what the tour, blueprint and bill of materials use)
export const RECT = {
  gilded: [-70, -20, -45, 16], wildmoot: [-70, -56, -45, -28], ironclad: [-55, -94, -21, -63],
  tide: [47, -51, 71, -28], grave: [46, -14, 68, 10], gallery: [22, -83, 50, -67], fire: [-14, -103, 13, -81],
  armory: [-63, 33, -40, 55], yard: [-70, 62, -46, 90], muster: [39, 32, 64, 56], archive: [45, 65, 69, 87],
  gates: [-36, 66, 36, 90], command: [-17, -40, 17, -4],
};
export function inPoly(x, z, pts = TERRACE.poly) {
  let c = false;
  for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
    const [xi, zi] = pts[i], [xj, zj] = pts[j];
    if ((zi > z) !== (zj > z) && x < (xj - xi) * (z - zi) / (zj - zi) + xi) c = !c;
  }
  return c;
}
export function polyDist(x, z, pts = TERRACE.poly) {
  if (inPoly(x, z, pts)) return 0;
  let best = 1e9;
  for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
    const [ax, az] = pts[j], [bx, bz] = pts[i]; const dx = bx - ax, dz = bz - az; const L = dx * dx + dz * dz;
    const t = L === 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / L));
    best = Math.min(best, Math.hypot(x - (ax + t * dx), z - (az + t * dz)));
  }
  return best;
}
export const onTerrace = (x, z) => inPoly(x, z);
export const onCamp = (x, z) => x >= CAMP.x1 && x <= CAMP.x2 && z >= CAMP.z1 && z <= CAMP.z2;
export function campDist(x, z) { const dx = Math.max(CAMP.x1 - x, 0, x - CAMP.x2), dz = Math.max(CAMP.z1 - z, 0, z - CAMP.z2); return Math.hypot(dx, dz); }
export const plateauDist = (x, z) => Math.min(polyDist(x, z), campDist(x, z));
// distance to the nearest polygon edge, whether inside or outside
export function edgeDist(x, z, pts = TERRACE.poly) {
  let best = 1e9;
  for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
    const [ax, az] = pts[j], [bx, bz] = pts[i]; const dx = bx - ax, dz = bz - az; const L = dx * dx + dz * dz;
    const t = L === 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / L));
    best = Math.min(best, Math.hypot(x - (ax + t * dx), z - (az + t * dz)));
  }
  return best;
}
