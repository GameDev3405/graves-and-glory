// Checks for review-booth.html's saving and syncing. Serve the page from a local web server wrapped in a bare
// document, open it with #test in the address, then run in the page: (await import("./page_checks.js")).run()
// It resolves to a list of {name, ok}. The page shows no real data in #test mode; this script supplies a fake store.
export async function run() {
  const out = [], check = (name, ok) => out.push({ name, ok: !!ok });
  const wait = ms => new Promise(r => setTimeout(r, ms));
  const H = "a".repeat(64), copy = x => JSON.parse(JSON.stringify(x));
  const st = { saves: [], reads: 0, pending: [], remote: {}, v: null, b: null, c: {} };
  const store = {
    watchBatches(cb) { st.b = cb; return () => {}; },
    watchVerdicts(cb) { st.v = cb; return () => {}; },
    watchClips(id, cb) { st.c[id] = cb; return () => {}; },
    saveVerdict(id, doc) { st.saves.push({ id, doc: copy(doc) }); return new Promise((res, rej) => st.pending.push({ res, rej })); },
    readVerdict(id) { st.reads++; return Promise.resolve(st.remote[id] ? copy(st.remote[id]) : null); },
    sheet() { return Promise.resolve({ demo: 2 }); },
  };
  const sections = ["near", "game16"].map(d => ({ viewer: "new", distance: d, y: 0, cell_w: 300, cell_h: 300, cols: 12, frames: 5 }));
  const clip = { name: "a", ticks: 5, impact_tick: 2, frame_hash: H, sheet: "round-1/a.webp", sheet_asset: null, changed: true, round: 1,
    sections, numbers: { foot_slide: null, foot_slide_reference: null }, flags: [], demo: 2 };
  const batch = id => ({ id, doc: { batch: id, round: 1, troop: id, lane: "lane", created_at: new Date().toISOString(), clip_order: ["a"], other_files: [] } });
  const entry = verdict => ({ verdict, tags: [], pins: [], frame_hash: H });
  const S = () => window.Booth.peek();
  const btn = text => [...document.querySelectorAll("button")].find(b => b.textContent.trim() === text);
  const click = async text => { const b = btn(text); if (b && !b.disabled) b.click(); await wait(30); return !!b && !b.disabled; };
  const open = async id => { [...document.querySelectorAll("button")].find(b => b.textContent.includes(id)).click(); await wait(30); st.c[id]([copy(clip)]); await wait(30);
    S().cards.forEach(c => window.Booth.loadSheet(c));      // a pane that is not being painted never reports cards as near
    await wait(60); };

  window.Booth.start(store);
  st.b([batch("b1"), batch("b2")], false);
  st.v({}, true);                                   // first answer comes from the browser's saved copy
  await open("b1");
  await click("Approve");
  check("no decision is saved before the server has answered", st.saves.length === 0);
  st.pending.splice(0).forEach(p => p.res());
  await wait(30);
  st.v({}, false);
  await wait(30);
  await click("Approve");
  check("a decision is saved once the server has answered", st.saves.length >= 1 && st.saves[st.saves.length - 1].doc.clips.a.verdict === "approved");

  // another device rejects the clip while this save is on its way
  st.remote.b1 = { sent_round: 0, clips: { a: entry("rejected") } };
  st.v({ b1: copy(st.remote.b1) }, false);
  st.pending.splice(0).forEach(p => p.res());
  await wait(60);
  check("a change that arrived during a save is shown afterwards", S().verdicts.b1 && S().verdicts.b1.clips.a.verdict === "rejected");

  // a save fails; meanwhile the review was sent from another device; Retry must not overwrite it
  await click("Approve");
  st.pending.splice(0).forEach(p => p.rej(new Error("offline")));
  await wait(30);
  st.remote.b1 = { sent_round: 1, clips: { a: entry("rejected") } };
  st.v({ b1: copy(st.remote.b1) }, false);
  const before = st.saves.length;
  await click("Retry");
  await wait(60);
  check("Retry does not overwrite a review sent elsewhere", st.saves.length === before && S().verdicts.b1.sent_round === 1 && S().verdicts.b1.clips.a.verdict === "rejected");

  // sending fails: the page must not say the review was sent
  await click("←");
  st.v({ b1: copy(st.remote.b1) }, false);
  await open("b2");
  await click("Approve");
  st.pending.splice(0).forEach(p => p.res());
  await wait(60);
  await click("Send review");
  await click("Confirm send");
  st.pending.splice(0).forEach(p => p.rej(new Error("offline")));
  await wait(60);
  const note = [...document.querySelectorAll("p.note")].filter(p => !p.hidden).map(p => p.textContent).join(" ");
  check("a failed send is not reported as sent", !/Review sent/.test(note) && /not sent/i.test(note));
  return out;
}
