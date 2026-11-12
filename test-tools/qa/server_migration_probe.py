"""QA rows E9, D16 to D18, D22, D23 on a dedicated dev server (plain runServer: production profile folder names, no test tools).
Usage: python test-tools/qa/server_migration_probe.py e|d     (JAVA_HOME set; port 25690 free; it stops only the server it started.)
e: a legacy bannerbound-profiles/ file and bannerbound-layout.json in the world; checks the copies, then a join upgrades the profile.
d: v1 (fresh, full, partial, cache, invalid), v2, v3 and a corrupt file in gravesandglory-profiles/; two server starts, a bot join each; run 2 must change nothing.
Scratch root: work/qa1104/srv (eula.txt is written there only and is never committed). Evidence lines go to work/qa1104/server-migration-evidence.txt."""
import sys, os, json, time, subprocess, threading, shutil, hashlib, glob

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
import ggbot

PORT = 25690
ROOT = os.path.join(REPO, "work", "qa1104", "srv")
WORLD = os.path.join(ROOT, "Graves and Glory")
EV = os.path.join(REPO, "work", "qa1104", "server-migration-evidence.txt")
FIX = os.path.join(REPO, "test-tools", "resources", "fixtures")


def say(*a):
    s = " ".join(str(x) for x in a)
    print(s.encode("ascii", "replace").decode(), flush=True)
    with open(EV, "a", encoding="utf8") as f:
        f.write(s + "\n")


def uuid_of(n):
    return str(ggbot.Bot._offline(n))


def sha(p):
    with open(p, "rb") as f:
        return hashlib.sha256(f.read()).hexdigest()[:12]


def start_server():
    env = dict(os.environ, GRAVES_ROADMAP_SERVER_ROOT=ROOT)
    p = subprocess.Popen([os.path.join(REPO, "gradlew.bat"), "runServer", "-I", "test-tools/isolated-server.gradle", "--offline"], cwd=REPO, env=env,
                         stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf8", errors="replace")
    lines = []

    def pump():
        for line in p.stdout:
            lines.append(line)
    threading.Thread(target=pump, daemon=True).start()
    t0 = time.time()
    while time.time() - t0 < 400 and p.poll() is None and not any("Done (" in line for line in lines):
        time.sleep(1)
    return p, lines, any("Done (" in line for line in lines)


def stop_server(p):
    try:
        p.stdin.write("stop\n")
        p.stdin.flush()
        p.wait(120)
    except Exception:
        say("server did not stop cleanly; stop the java process listening on", PORT, "by hand")


def props():
    os.makedirs(ROOT, exist_ok=True)
    with open(os.path.join(ROOT, "eula.txt"), "w") as f:
        f.write("eula=true\n")
    with open(os.path.join(ROOT, "server.properties"), "w") as f:
        f.write("server-port=%d\nonline-mode=false\nlevel-name=Graves and Glory\nwhite-list=false\nenable-rcon=false\nquery.port=%d\n" % (PORT, PORT))


def join(name, wait_file=None, secs=150):
    b = ggbot.Bot(name, REPO, port=PORT)
    b.quiet = True
    ok = b.connect()
    if ok and wait_file:
        t0 = time.time()
        while time.time() - t0 < secs and not os.path.exists(wait_file):
            time.sleep(0.5)
    return b, ok


def phase_e():
    say("=== E9: bannerbound-profiles/ and bannerbound-layout.json in the world ===")
    shutil.rmtree(ROOT, ignore_errors=True)
    props()
    legacy = os.path.join(WORLD, "bannerbound-profiles")
    os.makedirs(legacy)
    u = uuid_of("QaLegacy")
    shutil.copy(os.path.join(FIX, "profile-v1-full.json"), os.path.join(legacy, u + ".json"))
    with open(os.path.join(WORLD, "bannerbound-layout.json"), "w") as f:
        f.write('{"layout":"legacy-probe","map":0}')
    orig = {f: sha(f) for f in glob.glob(os.path.join(legacy, "*")) + [os.path.join(WORLD, "bannerbound-layout.json")]}
    p, lines, ok = start_server()
    try:
        say("E9 server Done:", ok)
        newp = os.path.join(WORLD, "gravesandglory-profiles", u + ".json")
        lay = os.path.join(WORLD, "gravesandglory-layout.json")
        say("E9 profile copied", os.path.exists(newp), "identical", os.path.exists(newp) and sha(newp) == sha(os.path.join(legacy, u + ".json")))
        say("E9 layout marker copied", os.path.exists(lay), "identical", os.path.exists(lay) and sha(lay) == sha(os.path.join(WORLD, "bannerbound-layout.json")))
        say("E9 originals kept unchanged", all(os.path.exists(f) and sha(f) == h for f, h in orig.items()))
        say("E9 errors in log", [l.strip()[:160] for l in lines if "ERROR" in l or "Exception" in l][:5])
        b, jok = join("QaLegacy", os.path.join(WORLD, "gravesandglory-profiles", u + ".v1.json"))
        time.sleep(3)
        d = json.load(open(newp, encoding="utf8"))
        say("E9 join", jok, "| after join: schema", d.get("schema"), "troopWrits", d.get("troopWrits"), "gearShards", d.get("gearShards"), "banners", len(d.get("banners", [])), "matches", d.get("matches"), "owned", len(d.get("owned", {})),
            "files", sorted(os.listdir(os.path.dirname(newp))))
        b.close()
    finally:
        stop_server(p)


def phase_d():
    say("=== D16 D17 D18 D22 D23: restart with old profile files ===")
    shutil.rmtree(ROOT, ignore_errors=True)
    props()
    pdir = os.path.join(WORLD, "gravesandglory-profiles")
    os.makedirs(pdir)
    cases = {"QaV1Fresh": "profile-v1-fresh.json", "QaV1Full": "profile-v1-full.json", "QaV1Partial": "profile-v1-partial.json", "QaV1Cache": "profile-v1-cache.json",
             "QaV1Invalid": "profile-v1-invalid.json", "QaV3Vet": "profile-v3-veteran.json"}
    src = {}
    for n, f in cases.items():
        shutil.copy(os.path.join(FIX, f), os.path.join(pdir, uuid_of(n) + ".json"))
        src[n] = open(os.path.join(FIX, f), "rb").read()
    # A real schema 2 image (campaignCleared 3), reduced to the schema 2 fields.
    src["QaV2"] = open(os.path.join(HERE, "profile-v2-seed.json"), "rb").read()
    open(os.path.join(pdir, uuid_of("QaV2") + ".json"), "wb").write(src["QaV2"])
    src["QaBad"] = b"this is not json {"
    open(os.path.join(pdir, uuid_of("QaBad") + ".json"), "wb").write(src["QaBad"])
    for run in (1, 2):
        say("--- server start", run, "---")
        p, lines, ok = start_server()
        try:
            say("server Done:", ok)
            before = {n: {f: sha(f) for f in glob.glob(os.path.join(pdir, uuid_of(n) + "*"))} for n in src}
            for n in src:
                u = uuid_of(n)
                main = os.path.join(pdir, u + ".json")
                step = 1 if n.startswith("QaV1") else (2 if n == "QaV2" else 3)
                want = os.path.join(pdir, "%s.v%d.json" % (u, step)) if run == 1 and n != "QaBad" else None
                b, jok = join(n, want)
                time.sleep(4 if run == 2 else 1)
                b.close()
                time.sleep(1)
                files = sorted(os.path.basename(f).replace(u, "<uuid>") for f in glob.glob(os.path.join(pdir, u + "*")))
                try:
                    d = json.load(open(main, encoding="utf8"))
                    info = "battlesWon %s schema %s troopWrits %s gearShards %s banners %d matches %s wins %s owned %d commander %s gear %d worn %d relic0 %s" % (
                        d.get("battlesWon"), d.get("schema"), d.get("troopWrits"), d.get("gearShards"), len(d.get("banners", [])), d.get("matches"), d.get("wins"), len(d.get("owned", {})), d.get("commander"),
                        len(d.get("gear", [])), len(d.get("equipped", {})), (d.get("relicSlots") or [""])[0])
                except Exception as e:
                    info = "unreadable: %s" % e
                same = None
                if run == 2:
                    same = all(sha(f) == h for f, h in before[n].items()) and sorted(before[n]) == sorted(glob.glob(os.path.join(pdir, u + "*")))
                bak = None
                if run == 1 and n != "QaBad":
                    bak = open(os.path.join(pdir, "%s.v%d.json" % (u, step)), "rb").read() == src[n]
                say("run", run, n, "joined", jok, "|", info, "| files", files, "| backup byte-copy", bak, "| unchanged-since-run1", same)
            say("run", run, "errors:", [l.strip()[:200] for l in lines if ("[STDERR]" in l or "ERROR" in l)][:8])
        finally:
            stop_server(p)


if __name__ == "__main__":
    {"e": phase_e, "d": phase_d}[sys.argv[1]]()
