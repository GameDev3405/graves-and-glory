"""QA row C9: on a dedicated server a new commander's profile file exists after the first join, before any collection change.
Needs a development dedicated server (offline mode, fresh save):
  gradlew runServer -I test-tools/isolated-server.gradle
  (GRAVES_ROADMAP_SERVER_ROOT=<fresh scratch root>, server-port and online-mode=false in <root>/server.properties; EULA only in that scratch root).
Usage: profile_on_join.py <repo> <port> <server root> <evidence file>
The world folder is read from level-name in <root>/server.properties (default "world"). The bot name carries the time, so every run is a first join.
Exit 0 only if <world>/gravesandglory-profiles/<uuid>.json exists after the join, holds schema 3 with the 10 starter Commons, and the same
file is left unchanged by a rejoin."""
import sys, os, json, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ggbot

repo, port, root, evid = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]
log = open(evid, "w", encoding="utf8")
fails = []


def say(*a):
    s = " ".join(str(x) for x in a)
    print(s, flush=True)
    log.write(s + "\n")
    log.flush()


def check(name, ok, detail=""):
    say(("PASS " if ok else "FAIL ") + name + (" :: " + str(detail) if detail != "" else ""))
    if not ok:
        fails.append(name)


def level_name():
    try:
        for line in open(os.path.join(root, "server.properties"), encoding="utf8"):
            if line.startswith("level-name="):
                return line.split("=", 1)[1].strip()
    except OSError:
        pass
    return "world"


def wait_for(path, seconds):
    t0 = time.time()
    while time.time() - t0 < seconds:
        if os.path.exists(path):
            return True
        time.sleep(0.25)
    return os.path.exists(path)


name = "C9" + time.strftime("%H%M%S")
bot = ggbot.Bot(name, repo, port=port)
bot.quiet = True
profiles = os.path.join(root, level_name(), "gravesandglory-profiles")
file = os.path.join(profiles, "%s.json" % bot.uuid)
check("no profile file before the first join", not os.path.exists(file), file)
check("bot joins", bot.connect(), bot.disconnect)
# No command, no purchase, no collection change: the join alone must write the file.
# The server seats a joiner on a tick after the arena is built, which can take over a minute after the first start; the bot stays connected meanwhile.
check("profile file exists after the first join", wait_for(file, 150), file)
if os.path.exists(file):
    try:
        data = json.load(open(file, encoding="utf8"))
        check("profile is schema 3", data.get("schema") == 3, data.get("schema"))
        check("profile holds the 10 starter Commons", len(data.get("owned", {})) == 10, len(data.get("owned", {})))
        before = open(file, "rb").read()
    except Exception as e:
        check("profile file reads as JSON", False, e)
        before = None
else:
    before = None
bot.close()
time.sleep(4)
bot2 = ggbot.Bot(name, repo, port=port)
bot2.quiet = True
check("same bot rejoins", bot2.connect(), bot2.disconnect)
time.sleep(4)
after = open(file, "rb").read() if os.path.exists(file) else None
check("a rejoin leaves the profile unchanged", before is not None and before == after)
bot2.close()
say("RESULT", "PASS" if not fails else "FAIL " + ", ".join(fails))
sys.exit(0 if not fails else 1)
