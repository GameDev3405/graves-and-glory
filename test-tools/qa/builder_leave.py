"""Regression check for the builder-server leave crash (2026-11-03): a builder who first joined (and was moved from the overworld into the lobby canvas)
moves to another chunk section, then disconnects. Before the fix the server died in ChunkTicketManager.handleChunkLeave with a NullPointerException
and kicked everyone. Needs a development dedicated server in build mode, offline mode:
  gradlew runServer -I test-tools/isolated-server.gradle -I <init script adding -Dgg.build=all to the server run>
  (GRAVES_ROADMAP_SERVER_ROOT=<fresh scratch root>, server-port and online-mode=false in <root>/server.properties).
Usage: builder_leave.py <repo> <port> <server latest.log> <evidence file>
Every bot name is new on each run (first join), so the run must start from a fresh save or use fresh names; the names carry the time.
Exit 0 only if the server survives every leave and the watcher stays connected."""
import sys, os, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ggbot

CRASH_MARKERS = ("Exception in server tick loop", "Encountered an unexpected exception", "handleChunkLeave", "method_14051")


def bot(name, repo, port):
    b = ggbot.Bot(name, repo, port=port)
    b.quiet = True  # no register/brand payloads, as in k7_rewards.py
    return b


def chat_command(bot, cmd):
    # 1.21.1 serverbound unsigned command (CommandExecutionC2SPacket, play 0x04): the command string only. 1.20.1 also sent a
    # timestamp, salt and an empty signature set; the signed form is now its own packet and unsigned commands do not need it.
    bot.chat_command(cmd)


def main():
    repo, port, log_path, evid = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]
    out = open(evid, "w", encoding="utf8")
    fails = []

    def say(*a):
        s = " ".join(str(x) for x in a)
        print(s, flush=True)
        out.write(s + "\n")
        out.flush()

    def check(name, ok, detail=""):
        say(("PASS " if ok else "FAIL ") + name + (" :: " + str(detail) if detail != "" else ""))
        if not ok:
            fails.append(name)

    def log_size():
        try:
            return os.path.getsize(log_path)
        except OSError:
            return 0

    def log_since(pos):
        try:
            with open(log_path, encoding="utf8", errors="replace") as f:
                f.seek(pos)
                return f.read()
        except OSError:
            return ""

    def run(bot, cmd, wait=3.0):
        t = time.time()
        chat_command(bot, cmd)
        time.sleep(wait)
        return [x[2] for x in bot.texts_since(t)]

    tag = time.strftime("%H%M%S")
    watcher = bot("W" + tag, repo, port)
    check("watcher joins", watcher.connect(), watcher.disconnect)
    time.sleep(4)

    # Each case: a first-join builder runs these commands, then leaves. "@far" is the last warp of the canvas the builder stands in.
    cases = [
        ("warp", ["warp @far"]),
        ("map-and-back", ["map kingsfield", "warp @far", "map lobby", "warp @far"]),
        ("map-away", ["map camp", "warp @far"]),
    ]
    for i, (name, cmds) in enumerate(cases):
        start = log_size()
        b = bot("L%d%s" % (i, tag), repo, port)
        ok = b.connect()
        check(name + ": builder joins", ok, b.disconnect)
        if not ok:
            continue
        time.sleep(4)
        for c in cmds:
            if "@far" in c:
                listed = " ".join(run(b, "warp", 2.0))
                warps = listed.split(":", 1)[1] if "Warps in" in listed and ":" in listed else ""
                names = [w.strip(' ."}') for w in warps.split(",") if w.strip(' ."}')]
                c = c.replace("@far", names[-1] if names else "center")
            texts = run(b, c)
            b.move()
            time.sleep(1)
            say("  /" + c + " -> " + (" | ".join(texts)[:200] or "(no reply)"))
        b.close()
        time.sleep(6)
        tail = log_since(start)
        crashed = [m for m in CRASH_MARKERS if m in tail]
        check(name + ": server survives the leave", not crashed and watcher.alive, (crashed, watcher.disconnect))
        if crashed or not watcher.alive:
            break

    probe = bot("P" + tag, repo, port)
    check("server still accepts a join", probe.connect(), probe.disconnect)
    probe.close()
    watcher.close()
    say("RESULT", "PASS" if not fails else "FAIL " + ", ".join(fails))
    sys.exit(0 if not fails else 1)


if __name__ == "__main__":
    main()
