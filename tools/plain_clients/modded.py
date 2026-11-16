"""Plain clients (port plan P3): one scripted session of the modded dev client (runClient without -PplainClients, so no Polymer: the
release modpack) against the plain-clients server. It joins through ConnectOnStart; the server drives the scene over RCON (a wagon
and the two map blocks beside the player, a practice battle) and screenshots are taken with F2 posted to the window.

  python tools/plain_clients/modded.py <server port> <shots dir>
Run it under the one-client lock (PORT_SLOT_ROOT=... PORT_GRADLE_SLOTS=1 gradle_slot.py -- python ...). Unshipped test harness.
"""
import os, sys, glob, time, subprocess, re
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import server as srv
import client as vc

NAME = "ModPlayer"


def find_minecraft_window():
    found = []
    @vc.ctypes.WINFUNCTYPE(vc.wintypes.BOOL, vc.wintypes.HWND, vc.wintypes.LPARAM)
    def cb(hwnd, _):
        if vc.user32.IsWindowVisible(hwnd):
            n = vc.ctypes.create_unicode_buffer(200)
            vc.user32.GetWindowTextW(hwnd, n, 200)
            if n.value.startswith("Minecraft"):
                found.append(hwnd)
        return True
    vc.user32.EnumWindows(cb, 0)
    return found[0] if found else None


class ModdedSession(vc.Session):
    def __init__(self, port, shots):
        self.port, self.shots, self.n = port, shots, 0
        self.root = os.path.join(srv.REPO, "work", "plain", "modded-" + time.strftime("%H%M%S"))
        os.makedirs(os.path.join(self.root, "run"), exist_ok=True)
        os.makedirs(shots, exist_ok=True)
        self.shot_root = os.path.join(self.root, "run")

    def launch(self):
        env = dict(os.environ, JAVA_HOME=srv.JAVA_HOME, GRAVES_ROADMAP_TEST_ROOT=self.root, GRAVES_ROADMAP_CONNECT="127.0.0.1:%d" % self.port,
                   GRAVES_ROADMAP_USER=NAME)
        # Every Gradle call holds a shared slot; the caller already holds the one-client lock.
        cmd = [sys.executable, srv.GRADLE_SLOT, "--", os.path.join(srv.REPO, "gradlew.bat"), "runClient", "--offline", "-PlocalIntegration",
               "--init-script", "test-tools/isolated-checks.gradle"]
        genv = dict(env)
        genv.pop("PORT_SLOT_ROOT", None)
        genv.pop("PORT_GRADLE_SLOTS", None)
        self.proc = subprocess.Popen(cmd, cwd=srv.REPO, env=genv, stdout=open(os.path.join(self.root, "gradle-run.txt"), "w"), stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
        t0 = time.time()
        self.hwnd = None
        while time.time() - t0 < 3000 and self.proc.poll() is None and not self.hwnd:
            time.sleep(5)
            self.hwnd = find_minecraft_window()
        print("modded client window %s" % self.hwnd, flush=True)
        return self.hwnd is not None

    def shot(self, label, settle=2.5):
        time.sleep(settle)
        pattern = os.path.join(self.shot_root, "screenshots", "*.png")
        before = set(glob.glob(pattern))
        vc.press(self.hwnd, "F2")
        t0 = time.time()
        while time.time() - t0 < 15:
            new = set(glob.glob(pattern)) - before
            if new:
                time.sleep(1)
                from PIL import Image
                self.n += 1
                dst = os.path.join(self.shots, "modded-%02d-%s.jpg" % (self.n, label))
                Image.open(sorted(new)[0]).convert("RGB").save(dst, "JPEG", quality=85)
                print("  shot " + dst, flush=True)
                return dst
            time.sleep(0.5)
        print("  shot %s: no screenshot appeared" % label, flush=True)
        return None

    def wait_join(self, timeout=600):
        t0 = time.time()
        while time.time() - t0 < timeout:
            try:
                if NAME in srv.rcon(self.port, "list"):
                    return True
            except OSError:
                pass  # the server still busy (camp build): ask again
            if self.proc.poll() is not None:
                return False
            time.sleep(3)
        return False

    def chunk_resend(self):
        """Out of view distance and back: the blocks then come in chunk data, not block updates."""
        pos = re.findall(r"-?[\d.]+(?=d)", srv.rcon(self.port, "data get entity %s Pos" % NAME))
        rot = re.findall(r"-?[\d.]+(?=f)", srv.rcon(self.port, "data get entity %s Rotation" % NAME))
        self.rc("execute as %s at @s run tp @s ~ ~ ~800" % NAME)
        time.sleep(8)
        self.rc("tp %s %s %s" % (NAME, " ".join(pos), " ".join(rot)))  # the same spot and look as before
        time.sleep(8)

    def run(self):
        if not self.launch():
            print("no client window", flush=True)
            return 1
        if not self.wait_join():
            print("client never joined", flush=True)
            return 1
        print("joined", flush=True)
        self.rc("plainprobe state")
        time.sleep(15)  # the camp drawn
        self.rc("execute as %s at @s run tp @s ~ ~ ~ -90 25" % NAME)  # face east, where the wagon spawns; the blocks at eye height +3, east and west
        self.rc("plainprobe wagon " + NAME)
        self.rc("plainprobe blocks " + NAME)
        self.shot("wagon-and-map-blocks", 4)
        self.chunk_resend()
        self.shot("after-chunk-resend", 4)
        self.rc("plainprobe practice " + NAME)
        st = self.wait_state("running=true", 240)
        m = re.search(r"time=(\d+) intro=(\d+)", st or "")
        if m and int(m.group(2)) > int(m.group(1)):
            time.sleep((int(m.group(2)) - int(m.group(1))) / 20 + 2)
        self.rc("plainprobe overview " + NAME)
        self.shot("battle", 5)
        self.rc("plainprobe state")
        return 0

    def stop(self):
        try:
            self.rc("kick %s test over" % NAME)
        except Exception:
            pass
        time.sleep(5)
        # Close only this session's own client: the window it opened (WM_CLOSE), then its own Gradle process tree.
        if self.hwnd:
            vc.user32.PostMessageW(self.hwnd, 0x0010, 0, 0)
        try:
            self.proc.wait(60)
        except Exception:
            subprocess.run(["taskkill", "/PID", str(self.proc.pid), "/T", "/F"], capture_output=True)
        log = os.path.join(self.root, "run", "logs", "latest.log")
        if os.path.exists(log):
            lines = open(log, encoding="utf-8", errors="replace").read().splitlines()
            bad = [l for l in lines if "ERROR" in l or "Exception" in l]
            print("modded client log: %d ERROR/Exception lines" % len(bad), flush=True)
            for l in bad[:30]:
                print("  " + l[:300], flush=True)
            for l in lines:
                if "registry" in l.lower() and ("sync" in l.lower() or "mismatch" in l.lower()):
                    print("  " + l[:300], flush=True)


if __name__ == "__main__":
    s = ModdedSession(int(sys.argv[1]), sys.argv[2])
    rc = 1
    try:
        rc = s.run()
    finally:
        s.stop()
    sys.exit(rc)
