"""Minimal stdlib protocol-763 (Minecraft 1.20.1) bot for the Graves and Glory QA rows. Offline-mode servers only.
The jar argument may be a built jar or a project folder (a dev server: build/resources/main holds the identity)."""
import socket, struct, threading, time, zlib, uuid, json, zipfile, re, io, sys

def vi(n):
    out = b""
    n &= 0xFFFFFFFF
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out += bytes([b | 0x80])
        else:
            return out + bytes([b])

def rvi(buf, pos):
    r = 0
    s = 0
    while True:
        b = buf[pos]
        pos += 1
        r |= (b & 0x7F) << s
        s += 7
        if not b & 0x80:
            break
    if r >= 1 << 31:
        r -= 1 << 32
    return r, pos

def mstr(s):
    d = s.encode("utf8")
    return vi(len(d)) + d

def rstr(buf, pos):
    n, pos = rvi(buf, pos)
    return buf[pos:pos + n].decode("utf8", "replace"), pos + n

def jar_identity(jar):
    import os
    if os.path.isdir(jar):
        res = os.path.join(jar, "build", "resources", "main")
        build = json.load(open(os.path.join(res, "gravesandglory-build.json")))["sourceSha256"]
        ver = json.load(open(os.path.join(res, "fabric.mod.json")))["version"]
        return ver, build
    with zipfile.ZipFile(jar) as z:
        build = json.loads(z.read("gravesandglory-build.json"))["sourceSha256"]
        ver = json.loads(z.read("fabric.mod.json"))["version"]
    return ver, build

class Bot:
    def __init__(self, name, jar, host="127.0.0.1", port=25640, protocol=1):
        self.name = name
        self.uuid = uuid.uuid3(uuid.NAMESPACE_OID, "OfflinePlayer:" + name) if False else self._offline(name)
        self.host, self.port = host, port
        self.ver, self.build = jar_identity(jar)
        self.protocol = protocol
        self.sock = None
        self.compress = -1
        self.alive = False
        self.disconnect = None
        self.channels = {}
        self.texts = []      # (time, overlay, text)
        self.last_kick = None
        self.state = "login"
        self.lock = threading.Lock()
        self.play_started = None

    @staticmethod
    def _offline(name):
        import hashlib
        h = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
        h[6] = (h[6] & 0x0F) | 0x30
        h[8] = (h[8] & 0x3F) | 0x80
        return uuid.UUID(bytes=bytes(h))

    def _send_raw(self, payload):
        if self.compress >= 0:
            if len(payload) >= self.compress:
                c = zlib.compress(payload)
                body = vi(len(payload)) + c
            else:
                body = vi(0) + payload
        else:
            body = payload
        self.sock.sendall(vi(len(body)) + body)

    def send(self, pid, data=b""):
        self.sent=getattr(self,'sent',[]);self.sent.append((hex(pid),data[:40]))
        self._send_raw(vi(pid) + data)

    def _read_exact(self, n):
        out = b""
        while len(out) < n:
            c = self.sock.recv(n - len(out))
            if not c:
                raise EOFError("closed")
            out += c
        return out

    def _read_packet(self):
        n = 0
        s = 0
        while True:
            b = self._read_exact(1)[0]
            n |= (b & 0x7F) << s
            s += 7
            if not b & 0x80:
                break
        body = self._read_exact(n)
        if self.compress >= 0:
            dl, p = rvi(body, 0)
            body = body[p:] if dl == 0 else zlib.decompress(body[p:])
        pid, p = rvi(body, 0)
        return pid, body[p:]

    def plugin(self, channel, data=b""):
        self.send(0x0D, mstr(channel) + data)

    def connect(self, timeout=40):
        self.sock = socket.create_connection((self.host, self.port), timeout=15)
        self.sock.settimeout(5)
        hs = vi(763) + mstr(self.host) + struct.pack(">H", self.port) + vi(2)
        self._send_raw(vi(0) + hs)
        self._send_raw(vi(0) + mstr(self.name) + b"\x01" + self.uuid.bytes)
        t0 = time.time()
        while time.time() - t0 < timeout:
            try:
                pid, d = self._read_packet()
            except socket.timeout:
                continue
            if pid == 0x00:  # disconnect
                self.disconnect = rstr(d, 0)[0]
                return False
            if pid == 0x03:
                self.compress, _ = rvi(d, 0)
            elif pid == 0x04:  # login plugin request
                mid, p = rvi(d, 0)
                ch, p = rstr(d, p)
                if ch == "gravesandglory:release_check_v1":
                    data = vi(self.protocol) + mstr(self.ver) + mstr(self.build)
                    self._send_raw(vi(0x02) + vi(mid) + b"\x01" + data)
                else:
                    self._send_raw(vi(0x02) + vi(mid) + b"\x00")
            elif pid == 0x02:  # login success -> play
                self.state = "play"
                self.alive = True
                self.play_started = time.time()
                threading.Thread(target=self._loop, daemon=True).start()
                reg = "\0".join(["gravesandglory:state_v9", "gravesandglory:impact_v1", "gravesandglory:barracks_v1",
                                 "gravesandglory:party_v1", "gravesandglory:stance_v1", "gravesandglory:feed_v1",
                                 "gravesandglory:camp_station"]).encode()
                if not getattr(self,"quiet",False):
                    self.plugin("minecraft:register", reg)
                    self.plugin("minecraft:brand", mstr("vanilla"))
                return True
        return False

    def _loop(self):
        while self.alive:
            try:
                pid, d = self._read_packet()
            except socket.timeout:
                continue
            except Exception as e:
                self.alive = False
                if self.disconnect is None:
                    self.disconnect = "socket: %r" % (e,)
                break
            try:
                if pid == 0x23:
                    self.send(0x12, d[:8])
                elif pid == 0x1A:
                    self.disconnect = d.decode("utf8", "replace")[:300]
                    self.alive = False
                elif pid == 0x17:
                    ch, p = rstr(d, 0)
                    with self.lock:
                        self.channels.setdefault(ch, []).append(d[p:])
                elif pid == 0x64:
                    s, p = rstr(d, 0)
                    ov = d[p] if p < len(d) else 0
                    with self.lock:
                        self.texts.append((time.time(), ov, s))
                elif pid == 0x3C:  # synchronize player position
                    tid, _ = rvi(d, 8 * 3 + 4 * 2 + 1)
                    self.send(0x00, vi(tid))
                elif pid == 0x40:  # resource pack
                    pass
            except Exception:
                pass

    def close(self):
        self.alive = False
        try:
            self.sock.close()
        except Exception:
            pass

    # packets of the mod
    def order(self, s):
        self.plugin("gravesandglory:order_v4", mstr(s))

    def profile(self, s):
        self.plugin("gravesandglory:profile_v1", mstr(s))

    def tactical(self, group, action, formation, x, z, yaw=0.0, target=-1):
        self.plugin("gravesandglory:tactical_v1", mstr(group) + mstr(action) + mstr(formation) + struct.pack(">ddfi", x, z, yaw, target))

    def texts_since(self, t):
        with self.lock:
            return [x for x in self.texts if x[0] >= t]

    def chan_count(self, name):
        with self.lock:
            return len(self.channels.get(name, []))
