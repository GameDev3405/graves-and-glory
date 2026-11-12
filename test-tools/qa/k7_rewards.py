"""QA row K7 (rewards): win, loss, rematch, quit, disconnect-and-return, a too-short round, visitors.
Needs a development dedicated server started with  gradlew runServer -PrewardDriver=<CTL> -I test-tools/isolated-server.gradle
(GRAVES_ROADMAP_SERVER_ROOT=<absolute server root>, server-port in <root>/server.properties).
Usage: k7_rewards.py <repo> <CTL> <port> <evidence file>
Four offline protocol-763 bots play; RewardDriver (test-tools) only moves the clock back and calls the real Campaign.finish.
Expected gold is computed here from sheets/design.json, not from the game."""
import sys, os, json, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ggbot

repo, ctl, port, evid = sys.argv[1], sys.argv[2], int(sys.argv[3]), sys.argv[4]
design = json.load(open(os.path.join(repo, "sheets", "design.json"), encoding="utf8"))
camp = []
def find(o):
    if isinstance(o, dict):
        if "completionGold" in o and "victoryGold" in o: camp.append(o); return
        for v in o.values(): find(v)
    elif isinstance(o, list):
        for v in o: find(v)
find(design)
COMP, VICT, MINSEC = camp[0]["completionGold"], camp[0]["victoryGold"], camp[0]["minimumRewardSeconds"]
log = open(evid, "w", encoding="utf8")
fails = []
def say(*a):
    s = " ".join(str(x) for x in a); print(s, flush=True); log.write(s + "\n"); log.flush()
def check(name, ok, detail=""):
    say(("PASS " if ok else "FAIL ") + name + (" :: " + str(detail) if detail != "" else ""))
    if not ok: fails.append(name)

n = [0]
def drv(op, arg=None, wait=20):
    n[0] += 1; i = "c%d" % n[0]
    out = os.path.join(ctl, "out-%s.json" % i)
    tmp = os.path.join(ctl, "cmd.tmp")
    open(tmp, "w").write("%s %s%s\n" % (i, op, "" if arg is None else " %s" % arg))
    os.replace(tmp, os.path.join(ctl, "cmd.txt"))
    t0 = time.time()
    while time.time() - t0 < wait:
        if os.path.exists(out):
            time.sleep(0.05); return json.load(open(out))
        time.sleep(0.1)
    raise RuntimeError("driver timeout " + op)

def disk(st, name):
    for p in st["profiles"]:
        if p["name"] == name:
            f = os.path.join(st["profileDir"], p["id"] + ".json")
            if not os.path.exists(f): return None
            j = json.load(open(f, encoding="utf8"))
            return {"gold": j["gearShards"], "matches": j["matches"], "wins": j["wins"], "xp": j.get("progression", {}).get("xp", 0)}
    return None
def mem(st, name):
    for p in st["profiles"]:
        if p["name"] == name: return {"gold": p["gearShards"], "matches": p["matches"], "wins": p["wins"], "xp": p["xp"]}
    return None
def both(st, name): return mem(st, name), disk(st, name)
def prorate(amount, seated, total):
    return 0 if amount <= 0 or seated <= 0 or total <= 0 else amount * min(seated, total) // total

def newbot(name):
    b = ggbot.Bot(name, repo, port=port); b.quiet = True
    assert b.connect(), b.disconnect
    return b
def wait_for(fn, secs, step=0.5):
    t0 = time.time()
    while time.time() - t0 < secs:
        r = fn()
        if r: return r
        time.sleep(step)
    return None
def running2(s): return s if s["running"] and len(s["participants"]) == 2 and all(p["seated"] for p in s["participants"]) else None
def is_away(name): return lambda: (lambda s: s if any(p["name"] == name and p["away"] for p in s["participants"]) else None)(drv("state"))

A, B, C, D = "K7Host", "K7Mate", "K7Visitor", "K7Late"
a = newbot(A); b = newbot(B); c = newbot(C)
time.sleep(6)
st = drv("state"); say("profileDir", st["profileDir"], "| completion", COMP, "victory", VICT, "minimum seconds", MINSEC)
base = {k: both(st, k) for k in (A, B, C)}
say("baseline", json.dumps(base))
check("players and visitor have profiles before any round", all(base[k][0] is not None for k in (A, B, C)))

# party and room by real packets
a.order("social:create"); time.sleep(1); a.order("social:invite:" + B); time.sleep(1.5); b.order("social:accept"); time.sleep(1.5)
a.order("social:room"); time.sleep(1.5)

def start_round(label, bots):
    for bot in bots: bot.order("social:ready")
    st = wait_for(lambda: running2(drv("state")), 70, 2)
    if st is None: say(label, "did not start", json.dumps(drv("state"))[:700]); return None
    say(label, "started; participants", json.dumps(st["participants"]))
    return st
def team_of(st): return {p["name"]: p["team"] for p in st["participants"]}

# ---------- round 1: win ----------
s1 = start_round("R1 win", (a, b))
check("R1 round started with two participants", s1 is not None)
if s1 is None: sys.exit(1)
t = team_of(s1); check("R1 both on one team", t[A] == t[B], t)
drv("shift", 200); pre = drv("state")
check("R1 each seated about 200 s", all(abs(p["seatedSeconds"] - 200) < 3 for p in pre["participants"]), [p["seatedSeconds"] for p in pre["participants"]])
fin = drv("finish", t[A]); time.sleep(1.5); post = drv("state")
check("R1 finish ran on a live round", fin["wasRunning"] is True)
for k in (A, B):
    m0, d0 = base[k]; m1, d1 = both(post, k)
    check("R1 %s gold +%d (win), memory" % (k, COMP + VICT), m1["gold"] - m0["gold"] == COMP + VICT, (m0["gold"], m1["gold"]))
    check("R1 %s matches +1 and wins +1" % k, m1["matches"] - m0["matches"] == 1 and m1["wins"] - m0["wins"] == 1)
    check("R1 %s saved to disk equal to memory" % k, d1 == m1, (d1, m1))
def unchanged(now, was):  # a visitor profile file first written on join is all zeros: no file before and a zero file after is unchanged
    return now[0] == was[0] and (now[1] == was[1] or (was[1] is None and now[1] is not None and all(v == 0 for v in now[1].values())))
check("R1 visitor unchanged in memory and on disk", unchanged(both(post, C), base[C]), both(post, C))
check("R1 reports saved and eligible for both", all(r["saved"] and r["eligible"] for r in post["results"]) and len(post["results"]) == 2, post["results"])
fin2 = drv("finish", t[A]); time.sleep(1); post2 = drv("state")
check("R1 second finish is a no-op (no round running)", fin2["wasRunning"] is False)
check("R1 gold unchanged after the second finish", all(both(post2, k) == both(post, k) for k in (A, B, C)))
cur = {k: both(post2, k) for k in (A, B, C)}

# ---------- round 2: loss + rematch ----------
time.sleep(2)
s2 = start_round("R2 loss/rematch", (a, b))
check("R2 rematch started from the same room", s2 is not None)
if s2 is None: sys.exit(1)
mid = drv("state")
check("R2 starting the rematch paid nothing", all(both(mid, k) == cur[k] for k in (A, B)))
t = team_of(s2); drv("shift", 200)
fin = drv("finish", 1 - t[A]); time.sleep(1.5); post = drv("state")
for k in (A, B):
    m0, d0 = cur[k]; m1, d1 = both(post, k)
    check("R2 %s gold +%d (loss: completion only), once" % (k, COMP), m1["gold"] - m0["gold"] == COMP, (m0["gold"], m1["gold"]))
    check("R2 %s matches +1, wins +0" % k, m1["matches"] - m0["matches"] == 1 and m1["wins"] - m0["wins"] == 0)
    check("R2 %s saved to disk equal to memory" % k, d1 == m1)
check("R2 visitor unchanged", both(post, C) == cur[C])
cur = {k: both(post, k) for k in (A, B, C)}

# ---------- round 3: B quits for good after 60 s of 200 ----------
time.sleep(2)
s3 = start_round("R3 quit", (a, b))
if s3 is None: sys.exit(1)
t = team_of(s3); drv("shift", 60)
b.close(); gone = wait_for(is_away(B), 20, 1)
check("R3 server sees B as away", gone is not None)
drv("shift", 140); pre = drv("state")
sec = {p["name"]: p["seatedSeconds"] for p in pre["participants"]}
check("R3 seated about 200 s for A and 60 s for B", abs(sec[A] - 200) < 3 and abs(sec[B] - 60) < 3, sec)
fin = drv("finish", t[A]); time.sleep(1.5); post = drv("state")
el = pre["elapsedSeconds"]
lo, hi = prorate(COMP + VICT, int(sec[B]) - 1, int(el) + 1), prorate(COMP + VICT, int(sec[B]) + 2, int(el) - 1)
mA, dA = both(post, A); mB, dB = both(post, B)
check("R3 A paid in full (+%d)" % (COMP + VICT), mA["gold"] - cur[A][0]["gold"] == COMP + VICT)
gB = mB["gold"] - cur[B][0]["gold"]
check("R3 quitter B paid pro rata (%d..%d gold, about 30 percent), once" % (lo, hi), lo <= gB <= hi and gB < COMP + VICT, gB)
check("R3 quitter B matches +1, wins +0 (seated under half)", mB["matches"] - cur[B][0]["matches"] == 1 and mB["wins"] == cur[B][0]["wins"])
check("R3 quitter saved to disk while offline", dB == mB and dA == mA, (dB, mB))
check("R3 visitor unchanged", both(post, C) == cur[C])
cur = {k: both(post, k) for k in (A, B, C)}

# ---------- round 4: B drops and returns, a visitor walks in mid-match ----------
b = newbot(B); time.sleep(4)
s4 = start_round("R4 disconnect/return", (a, b))
check("R4 round started after the quitter came back", s4 is not None)
if s4 is None: sys.exit(1)
t = team_of(s4); drv("shift", 100)
d = newbot(D); time.sleep(2)
b.close(); wait_for(is_away(B), 20, 1)
time.sleep(8)
b = newbot(B); time.sleep(4)
back = drv("state"); pb = [p for p in back["participants"] if p["name"] == B][0]
check("R4 B seated again and not away", pb["seated"] and not pb["away"], pb)
drv("shift", 100); pre = drv("state")
sec = {p["name"]: p["seatedSeconds"] for p in pre["participants"]}
away = {p["name"]: p["awaySeconds"] for p in pre["participants"]}
say("R4 seated", sec, "away", away)
check("R4 B was away about 8 to 16 s", 6 <= away[B] <= 18, away[B])
fin = drv("finish", t[A]); time.sleep(1.5); post = drv("state")
el = pre["elapsedSeconds"]
lo, hi = prorate(COMP + VICT, int(sec[B]) - 1, int(el) + 1), prorate(COMP + VICT, int(sec[B]) + 2, int(el) - 1)
mA, dA = both(post, A); mB, dB = both(post, B)
check("R4 A paid in full", mA["gold"] - cur[A][0]["gold"] == COMP + VICT)
gB = mB["gold"] - cur[B][0]["gold"]
check("R4 returning B paid for his seated time (%d..%d gold), once" % (lo, hi), lo <= gB <= hi, gB)
check("R4 B counted as a win (seated over half)", mB["wins"] - cur[B][0]["wins"] == 1 and mB["matches"] - cur[B][0]["matches"] == 1)
check("R4 saved to disk equal to memory", dA == mA and dB == mB)
mD, dD = both(post, D)
check("R4 idle visitor unchanged", both(post, C) == cur[C])
check("R4 late visitor has no match, no wins and no reward gold in memory or on disk", mD is not None and mD["matches"] == 0 and mD["wins"] == 0 and mD["gold"] == 0 and dD in (None, mD), (mD, dD))
cur = {k: both(post, k) for k in (A, B, C)}; late = both(post, D)

# ---------- round 5: too short ----------
time.sleep(2)
s5 = start_round("R5 short", (a, b))
check("R5 round started", s5 is not None)
if s5:
    t = team_of(s5); time.sleep(5); pre = drv("state")
    fin = drv("finish", t[A]); time.sleep(1.5); post = drv("state")
    for k in (A, B):
        m1, d1 = both(post, k)
        check("R5 %s short round (%d s < %d s) pays no gold" % (k, pre["elapsedSeconds"], MINSEC), m1["gold"] == cur[k][0]["gold"], (cur[k][0]["gold"], m1["gold"]))
        check("R5 %s short round state saved equal on disk" % k, d1 == m1)
    check("R5 reports not eligible", all(r["eligible"] is False for r in post["results"]), post["results"])
    check("R5 visitors unchanged", both(post, C) == cur[C] and both(post, D) == late)
final = drv("state")
say("FINAL", json.dumps({k: both(final, k) for k in (A, B, C, D)}))
for x in (a, b, c, d):
    try: x.close()
    except Exception: pass
say("RESULT", ("FAILS: " + ", ".join(fails)) if fails else "ALL PASS")
sys.exit(1 if fails else 0)
