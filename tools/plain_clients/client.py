"""Plain clients (port plan P3; from the P2 spike): one scripted session of a vanilla 1.21.1 client (Mojang's jar from Loom's cache, no Fabric, no mods) against
the spike server. The server drives the scene over RCON (practice, teleports, the Recruit menu); the client gets only posted key
messages (F2 for screenshots, a hotbar number key for one order), so it never needs focus. Screenshots are copied as JPEGs.

  python tools/plain_clients/client.py <server port> <shots dir>
Run it under the one-client lock (PORT_SLOT_ROOT=... PORT_GRADLE_SLOTS=1 gradle_slot.py -- python ...). Unshipped test harness (port plan P3).
"""
import os, sys, json, glob, time, struct, subprocess, ctypes, uuid, hashlib, shutil
from ctypes import wintypes
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import server as spike

LOOM = os.path.expanduser("~/.gradle/caches/fabric-loom")
M2 = os.path.expanduser("~/.gradle/caches/modules-2/files-2.1")
NAME = "PlainPlayer"
user32 = ctypes.WinDLL("user32", use_last_error=True)
WM_KEYDOWN, WM_KEYUP = 0x0100, 0x0101
KEYS = {"F2": (0x71, 0x3C), "ESC": (0x1B, 0x01), "1": (0x31, 0x02), "2": (0x32, 0x03), "3": (0x33, 0x04), "4": (0x34, 0x05), "5": (0x35, 0x06), "6": (0x36, 0x07), "F1": (0x70, 0x3B), "8": (0x38, 0x09), "Q": (0x51, 0x10)}


def classpath():
    info = json.load(open(os.path.join(LOOM, "1.21.1", "mojang_minecraft_info.json")))
    cp = []
    for lib in info["libraries"]:
        a = lib.get("downloads", {}).get("artifact")
        if not a:
            continue
        fn = os.path.basename(a["path"])
        if "natives-" in fn and "natives-windows" not in fn:
            continue
        if any(r.get("os", {}).get("name") == "osx" and r.get("action") == "allow" for r in lib.get("rules", [])):
            continue
        g, n, v = lib["name"].split(":")[:3]
        hits = glob.glob(os.path.join(M2, g, n, v, "*", fn))
        if hits:
            cp.append(hits[0])
    cp.append(os.path.join(LOOM, "1.21.1", "minecraft-client.jar"))
    return os.pathsep.join(cp), info["mainClass"]


def nbt_str(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def servers_dat(path, port):
    """servers.dat: one entry for the spike server with resource packs set to Enabled (acceptTextures 1b), uncompressed NBT."""
    entry = (b"\x08" + nbt_str("ip") + nbt_str("127.0.0.1:%d" % port) + b"\x08" + nbt_str("name") + nbt_str("Plain clients test")
             + b"\x01" + nbt_str("acceptTextures") + b"\x01" + b"\x00")
    data = b"\x0a" + nbt_str("") + b"\x09" + nbt_str("servers") + b"\x0a" + struct.pack(">i", 1) + entry + b"\x00"
    open(path, "wb").write(data)


def options(path):
    lines = ["version:3955", "pauseOnLostFocus:false", "onboardAccessibility:false", "skipMultiplayerWarning:true", "joinedFirstServer:true",
             "tutorialStep:none", "narrator:0", "soundCategory_master:0.0", "renderDistance:10", "guiScale:2", "fullscreen:false",
             "key_key.screenshot:key.keyboard.f2", "chatVisibility:0", "advancedItemTooltips:false", "darkMojangStudiosBackground:false"]
    open(path, "w").write("\n".join(lines) + "\n")


def find_window(pid):
    found = []
    @ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)
    def cb(hwnd, _):
        p = wintypes.DWORD()
        user32.GetWindowThreadProcessId(hwnd, ctypes.byref(p))
        if p.value == pid and user32.IsWindowVisible(hwnd):
            n = ctypes.create_unicode_buffer(200)
            user32.GetWindowTextW(hwnd, n, 200)
            if n.value.startswith("Minecraft"):
                found.append(hwnd)
        return True
    user32.EnumWindows(cb, 0)
    return found[0] if found else None


def press(hwnd, key):
    vk, scan = KEYS[key]
    user32.PostMessageW(hwnd, WM_KEYDOWN, vk, 1 | (scan << 16))
    time.sleep(0.08)
    user32.PostMessageW(hwnd, WM_KEYUP, vk, 1 | (scan << 16) | 0xC0000000)


class Session:
    def __init__(self, port, shots):
        self.port, self.shots = port, shots
        self.root = os.path.join(spike.REPO, "work", "plain", "client-" + time.strftime("%H%M%S"))
        os.makedirs(self.root, exist_ok=True)
        os.makedirs(shots, exist_ok=True)
        options(os.path.join(self.root, "options.txt"))
        servers_dat(os.path.join(self.root, "servers.dat"), port)
        self.n = 0

    def launch(self):
        cp, main = classpath()
        java = os.path.join(spike.JAVA_HOME, "bin", "javaw.exe")
        offline = uuid.UUID(bytes=hashlib.md5(("OfflinePlayer:" + NAME).encode()).digest(), version=3)
        args = [java, "-Xmx2G", "-cp", cp, main, "--username", NAME, "--uuid", offline.hex, "--accessToken", "0", "--userType", "legacy",
                "--version", "1.21.1", "--versionType", "release", "--gameDir", self.root, "--assetsDir", os.path.join(LOOM, "assets"),
                "--assetIndex", "1.21.1-17", "--width", "1280", "--height", "720", "--quickPlayMultiplayer", "127.0.0.1:%d" % self.port]
        self.fg = user32.GetForegroundWindow()
        self.proc = subprocess.Popen(args, cwd=self.root, stdout=open(os.path.join(self.root, "client-out.txt"), "w"), stderr=subprocess.STDOUT)
        t0 = time.time()
        self.hwnd = None
        while time.time() - t0 < 180 and self.proc.poll() is None and not self.hwnd:
            time.sleep(2)
            self.hwnd = find_window(self.proc.pid)
        print("client pid %d window %s" % (self.proc.pid, self.hwnd), flush=True)
        time.sleep(3)
        if self.fg:
            user32.SetForegroundWindow(self.fg)  # hand focus back to whatever Kort had in front (best effort)
        return self.hwnd is not None

    def chunk_resend(self):
        """Out of view distance and back to the same spot and look: the blocks then come in chunk data, not block updates."""
        import re
        pos = re.findall(r"-?[\d.]+(?=d)", self.rc("data get entity %s Pos" % NAME))
        rot = re.findall(r"-?[\d.]+(?=f)", self.rc("data get entity %s Rotation" % NAME))
        self.rc("execute as %s at @s run tp @s ~ ~ ~800" % NAME)
        time.sleep(8)
        self.rc("tp %s %s %s" % (NAME, " ".join(pos), " ".join(rot)))
        time.sleep(8)

    def rc(self, cmd):
        out = spike.rcon(self.port, cmd)
        print("  rcon> %s :: %s" % (cmd, out.strip()[:200]), flush=True)
        return out

    def shot(self, label, settle=2.5):
        time.sleep(settle)
        before = set(glob.glob(os.path.join(self.root, "screenshots", "*.png")))
        press(self.hwnd, "F2")
        t0 = time.time()
        while time.time() - t0 < 15:
            new = set(glob.glob(os.path.join(self.root, "screenshots", "*.png"))) - before
            if new:
                time.sleep(1)
                from PIL import Image
                self.n += 1
                dst = os.path.join(self.shots, "%02d-%s.jpg" % (self.n, label))
                Image.open(sorted(new)[0]).convert("RGB").save(dst, "JPEG", quality=85)
                print("  shot " + dst, flush=True)
                return dst
            time.sleep(0.5)
        print("  shot %s: no screenshot appeared" % label, flush=True)
        return None

    def wait_state(self, needle, timeout):
        t0 = time.time()
        while time.time() - t0 < timeout:
            try:
                st = spike.rcon(self.port, "plainprobe state")
            except OSError:
                st = ""
            if needle in st:
                return st
            time.sleep(2)
        return None

    def wait_join(self, timeout=240):
        t0 = time.time()
        while time.time() - t0 < timeout:
            if NAME in spike.rcon(self.port, "list"):
                return True
            if self.proc.poll() is not None:
                return False
            time.sleep(2)
        return False

    def run(self):
        if not self.launch():
            print("no client window", flush=True)
            return 1
        if not self.wait_join():
            print("client never joined", flush=True)
            return 1
        print("joined", flush=True)
        self.wait_state("ready=true", 240)
        time.sleep(8)  # the pack applied, chunks drawn
        self.shot("camp-hud-hotbar")
        self.rc("plainprobe writs %s 30" % NAME)
        self.rc("plainprobe recruit " + NAME)
        self.shot("recruit-menu", 3)
        press(self.hwnd, "ESC")
        self.rc("execute as %s at @s run tp @s ~ ~ ~ -90 25" % NAME)  # face east, where the wagon spawns (4 blocks); the map blocks are barriers, invisible
        self.rc("plainprobe wagon " + NAME)
        self.rc("plainprobe blocks " + NAME)
        self.shot("wagon-and-map-blocks", 3)
        self.chunk_resend()
        # a barrier draws only for a creative player holding one: show where the stand-ins are
        self.rc("gamemode creative " + NAME)
        self.rc("item replace entity %s weapon.offhand with minecraft:barrier" % NAME)
        self.shot("after-chunk-resend-barrier-markers", 6)
        self.rc("item replace entity %s weapon.offhand with minecraft:air" % NAME)
        self.rc("gamemode survival " + NAME)
        q = "execute if entity @e[type=minecraft:item,nbt={Item:{components:{\"minecraft:custom_data\":{gg_plain:\"recruit\"}}}}]"
        before = "passed" in self.rc(q)
        self.rc("plainprobe slot %s 7" % NAME)  # the Recruit book, selected by the server so the key cannot miss
        time.sleep(1)
        press(self.hwnd, "Q")  # drop it: the server keeps it (PlainDropMixin)
        time.sleep(1.5)
        after = "passed" in self.rc(q)
        print("  drop check: %s" % ("LEAK (the Recruit book is in the world)" if after and not before else "ok (the Recruit book stayed in the hotbar)" if not after else "unknown (items already near)"), flush=True)
        self.rc("execute as %s at @s run tp @s ~ ~ ~ 90 10" % NAME)  # face west, away from the wagon and blocks
        self.rc("plainprobe mage " + NAME)  # a lone mage, AI off, out of any battle
        self.shot("mage-as-witch-alone", 4)
        self.rc("plainprobe practice " + NAME)
        st = self.wait_state("running=true", 240)
        import re
        m = re.search(r"time=(\d+) intro=(\d+)", st or "")
        if m and int(m.group(2)) > int(m.group(1)):
            time.sleep((int(m.group(2)) - int(m.group(1))) / 20 + 2)
        self.rc("plainprobe overview " + NAME)
        self.shot("battle-hud", 4)
        self.rc("plainprobe view %s archer" % NAME)
        self.shot("archer-as-skeleton", 3)
        self.rc("plainprobe view %s knight" % NAME)
        self.shot("knight-husk", 3)
        self.rc("plainprobe view %s mage" % NAME)
        self.shot("mage-as-witch", 3)
        self.rc("plainprobe view %s mage 14" % NAME)  # from above and behind the front rank, out of the spell particles
        self.shot("mage-as-witch-far", 2)
        self.rc("plainprobe overview " + NAME)
        self.rc("plainprobe aim %s 25" % NAME)
        time.sleep(2)
        press(self.hwnd, "4")  # hotbar key 4 = slot 3 = Hold, from the real client
        self.shot("after-hold-key", 3)
        time.sleep(2)
        press(self.hwnd, "3")  # key 3 = slot 2 = Charge
        self.shot("after-charge-key", 4)
        self.rc("plainprobe state")
        return 0

    def stop(self):
        try:
            if self.proc.poll() is None:
                self.proc.terminate()  # only this session's own client process
                self.proc.wait(20)
        except Exception as e:
            print("stop: %r" % e, flush=True)
        log = os.path.join(self.root, "logs", "latest.log")
        if os.path.exists(log):
            bad = [l.rstrip() for l in open(log, encoding="utf-8", errors="replace") if "ERROR" in l or "Exception" in l or "WARN" in l]
            print("client log: %d WARN/ERROR lines" % len(bad), flush=True)
            for l in bad[:40]:
                print("  " + l[:300], flush=True)
            chat = [l.rstrip() for l in open(log, encoding="utf-8", errors="replace") if "[CHAT]" in l]
            leaks = [l for l in chat if "key.gravesandglory" in l or "{key:" in l]
            print("client chat: %d lines, %d with a binding id or key token (P3.4)" % (len(chat), len(leaks)), flush=True)
            for l in chat[-15:]:
                print("  " + l[:240], flush=True)


if __name__ == "__main__":
    s = Session(int(sys.argv[1]), sys.argv[2])
    rc = 1
    try:
        rc = s.run()
    finally:
        s.stop()
    sys.exit(rc)
