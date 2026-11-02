// Fetch wrappers for the map engine server routes. Same-origin relative URLs only.
// Every call resolves to {ok, status, data}; network failures come back as ok:false with data.errors.

async function call(method, url, body, extra) {
  const init = { method, headers: {}, cache: 'no-store', ...(extra || {}) };
  if (method !== 'GET') {
    init.headers['Content-Type'] = 'application/json';
    init.body = JSON.stringify(body === undefined ? {} : body);
  }
  let res;
  try {
    res = await fetch(url, init);
  } catch (e) {
    return { ok: false, status: 0, data: { errors: ['cannot reach the server: ' + e.message] } };
  }
  let data = null;
  try { data = await res.json(); } catch (e) { /* non-JSON body */ }
  return { ok: res.ok, status: res.status, data };
}

const dp = (id) => '/api/design/' + encodeURIComponent(id);

export const listDesigns = () => call('GET', '/api/designs');
export const getDesign = (id) => call('GET', dp(id));
export const putDesign = (id, design, extra) => call('PUT', dp(id), design, extra);
export const listVersions = (id) => call('GET', dp(id) + '/versions');
export const saveVersion = (id, name) => call('POST', dp(id) + '/versions', { name });
export const restoreVersion = (id, name) => call('POST', dp(id) + '/restore', { name });
export const build = (id) => call('POST', dp(id) + '/build', {});
export const exportBundle = (id) => call('POST', dp(id) + '/export', {});
export const importDraft = (id, draft) => call('POST', dp(id) + '/import', draft === undefined ? {} : { draft });

function b64(s, Type) {
  const bin = atob(s);
  const u8 = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) u8[i] = bin.charCodeAt(i);
  return new Type(u8.buffer); // server sends little-endian; browsers on x86/ARM are little-endian too
}

// Decoded field: {width, depth, height:Int16Array, water:Int16Array (-32768 = none), road:Uint8Array, zone:Int16Array, zones}
// Arrays are row-major [z * width + x].
export async function getField(id) {
  const r = await call('GET', dp(id) + '/field');
  if (!r.ok) return r;
  const f = r.data;
  r.data = {
    width: f.width, depth: f.depth, zones: f.zones,
    height: b64(f.height, Int16Array), water: b64(f.water, Int16Array),
    road: b64(f.road, Uint8Array), zone: b64(f.zone, Int16Array),
  };
  return r;
}
