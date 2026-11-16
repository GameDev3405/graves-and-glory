"""Plain clients (port plan P3) test helpers, from the P2 spike: a dedicated battle server with RCON, a vanilla 1.21.1 client launched straight from
Loom's cache (no Fabric, no mods), and key presses posted to its window. Unshipped test harness (port plan P3; from the P2 spike).

  python tools/plain_clients/server.py server <name> <port> [gradle -P args...]   start a server (foreground; RCON on port+1)
  python tools/plain_clients/server.py rcon <port> <command...>                    run one console command on it
"""
import os, socket, struct, subprocess, sys, time, json, glob

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
JAVA_HOME = r"C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
RCON_PASS = "plainprobe"
GRADLE_SLOT = r"C:/Users/kortt/Melty/status/port-tools/gradle_slot.py"


def server_root(name):
    root = os.path.join(REPO, "work", "plain", name + "-" + time.strftime("%H%M%S"))
    os.makedirs(root, exist_ok=True)
    return root


def write_server_files(root, port):
    with open(os.path.join(root, "eula.txt"), "w") as f:  # throwaway run folder only (work/ is gitignored)
        f.write("eula=true\n")
    props = {
        "level-name": "Graves and Glory", "motd": "Graves and Glory plain clients test", "server-port": port, "online-mode": "false",
        "max-tick-time": 60000, "view-distance": 10, "simulation-distance": 10, "spawn-protection": 0, "difficulty": "easy",
        "enable-rcon": "true", "rcon.port": port + 1, "rcon.password": RCON_PASS, "broadcast-rcon-to-ops": "false",
    }
    with open(os.path.join(root, "server.properties"), "w") as f:
        for k, v in props.items():
            f.write(f"{k}={v}\n")


def seed_polymer_client_jar(root):
    """Polymer's pack builder reads vanilla assets from Mojang's client jar and downloads it into polymer/cached_client_jars when a
    run folder has none. Copy the one an earlier run already holds, so a test run downloads nothing."""
    for old in sorted(glob.glob(os.path.join(REPO, "work", "plain", "*", "polymer", "cached_client_jars", "*.jar"))):
        dst = os.path.join(root, "polymer", "cached_client_jars")
        os.makedirs(dst, exist_ok=True)
        if not os.path.exists(os.path.join(dst, os.path.basename(old))):
            import shutil
            shutil.copyfile(old, os.path.join(dst, os.path.basename(old)))
        return True
    return False


def start_server(name, port, args, root=None):
    root = root or server_root(name)
    write_server_files(root, port)
    seed_polymer_client_jar(root)
    env = dict(os.environ, JAVA_HOME=JAVA_HOME, GRAVES_ROADMAP_SERVER_ROOT=root)
    log = open(os.path.join(root, "gradle-run.txt"), "w")
    # Every Gradle call holds a shared slot (status/port-tools/gradle_slot.py) while it runs.
    cmd = [sys.executable, GRADLE_SLOT, "--", os.path.join(REPO, "gradlew.bat"), "runServer", "--offline", *args, "--init-script", "test-tools/isolated-server.gradle"]
    proc = subprocess.Popen(cmd, cwd=REPO, env=env, stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
    return proc, root


def wait_for(root, needle, timeout=900, proc=None):
    path = os.path.join(root, "gradle-run.txt")
    t0 = time.time()
    while time.time() - t0 < timeout:
        try:
            with open(path, encoding="utf-8", errors="replace") as f:
                if needle in f.read():
                    return True
        except OSError:
            pass
        if proc is not None and proc.poll() is not None:
            return False
        time.sleep(2)
    return False


def rcon(port, command, timeout=10):
    s = socket.create_connection(("127.0.0.1", port + 1), timeout=timeout)
    def pkt(rid, kind, body):
        data = struct.pack("<ii", rid, kind) + body.encode("utf-8") + b"\x00\x00"
        s.sendall(struct.pack("<i", len(data)) + data)
    def read():
        n = struct.unpack("<i", s.recv(4))[0]
        buf = b""
        while len(buf) < n:
            buf += s.recv(n - len(buf))
        rid, kind = struct.unpack("<ii", buf[:8])
        return rid, buf[8:-2].decode("utf-8", "replace")
    pkt(1, 3, RCON_PASS)
    rid, _ = read()
    if rid == -1:
        raise RuntimeError("rcon auth failed")
    pkt(2, 2, command)
    _, out = read()
    s.close()
    return out


if __name__ == "__main__":
    if sys.argv[1] == "rcon":
        print(rcon(int(sys.argv[2]), " ".join(sys.argv[3:])))
    elif sys.argv[1] == "server":
        proc, root = start_server(sys.argv[2], int(sys.argv[3]), sys.argv[4:])
        print(root, flush=True)
        sys.exit(proc.wait())
