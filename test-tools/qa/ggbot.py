"""Minimal stdlib protocol-767 (Minecraft 1.21.1) bot for the Graves and Glory QA rows. Offline-mode servers only.

The jar argument may be a built jar or a project folder (a dev server: build/resources/main holds the identity).

Connection flow (1.21.1): handshake -> login (offline hello, compression, our release_check_v1 login query as the fallback gate)
-> login acknowledged -> configuration (channel registration, our release_check_v2 configuration request, known packs, registry data,
finish configuration) -> play. Packet ids come from the 1.21.1 merged jar (net.minecraft.network.state.*, add() order), not memory.

The compatibility gate (VersionGate) is answered the way the modded client answers it:
  configuration: the server's minecraft:register is answered with our own (gravesandglory:release_check_v2) before Fabric's ping,
                 then the empty release_check_v2 request is answered with vi(protocol) + str(version) + str(build) on the same id;
  login (fallback, while the server keeps -Dgravesandglory.loginGate on and a battle exists): the same bytes on release_check_v1.
Bot(gate=...) chooses the answer: ok (the real identity), build, old, protocol, malformed, none (advertise the channel, never answer:
the server's task waits), plain (never advertise the channel, and answer the login query "not understood", as a vanilla client).
Bot(login_gate=...) overrides the login answer alone (default: the same as gate; plain and none answer "not understood" / ok).

Command line, for a hand test against a running dev server (never starts a server):
  python test-tools/qa/ggbot.py <repo|jar> [--port 25640] [--name QaBot] [--gate ok] [--login-gate ...] [--expect join|refused]
      [--stay 10] [--chat "warp"] [--trace]
  Prints every phase change, the gate exchange and the disconnect reason; exit 0 when the outcome matches --expect.
  python test-tools/qa/ggbot.py --selftest   (framing and phase machine against a scripted fake server; no Minecraft)
"""
import socket, struct, threading, time, zlib, uuid, json, zipfile, io, sys, os

PROTOCOL_767 = 767
RELEASE_CHECK_V1 = "gravesandglory:release_check_v1"   # login query (fallback)
RELEASE_CHECK_V2 = "gravesandglory:release_check_v2"   # configuration task (primary)

# Packet ids, protocol 767, from the yarn 1.21.1 merged jar: the add()/addBundle() order in HandshakeStates, LoginStates,
# ConfigurationStates and PlayStateFactories (one id per call, in order).
HS_INTENTION = 0x00
L_S2C_DISCONNECT, L_S2C_HELLO, L_S2C_SUCCESS, L_S2C_COMPRESSION, L_S2C_QUERY, L_S2C_COOKIE = 0x00, 0x01, 0x02, 0x03, 0x04, 0x05
L_C2S_HELLO, L_C2S_QUERY_ANSWER, L_C2S_ACK, L_C2S_COOKIE = 0x00, 0x02, 0x03, 0x04
C_S2C_COOKIE, C_S2C_PAYLOAD, C_S2C_DISCONNECT, C_S2C_FINISH, C_S2C_KEEPALIVE, C_S2C_PING = 0x00, 0x01, 0x02, 0x03, 0x04, 0x05
C_S2C_REGISTRY, C_S2C_PACK_POP, C_S2C_PACK_PUSH, C_S2C_KNOWN_PACKS = 0x07, 0x08, 0x09, 0x0E
C_C2S_OPTIONS, C_C2S_COOKIE, C_C2S_PAYLOAD, C_C2S_FINISH, C_C2S_KEEPALIVE, C_C2S_PONG, C_C2S_PACK, C_C2S_KNOWN_PACKS = range(8)
P_S2C_CHUNK_BATCH_FINISHED, P_S2C_COOKIE, P_S2C_PAYLOAD, P_S2C_DISCONNECT, P_S2C_KEEPALIVE = 0x0C, 0x16, 0x19, 0x1D, 0x26
P_S2C_LOGIN, P_S2C_PING, P_S2C_POSITION, P_S2C_PACK_PUSH, P_S2C_START_CONFIG, P_S2C_SYSTEM_CHAT = 0x2B, 0x35, 0x40, 0x46, 0x69, 0x6C
P_C2S_ACCEPT_TELEPORT, P_C2S_CHAT_COMMAND, P_C2S_CHUNK_BATCH_RECEIVED, P_C2S_CONFIG_ACK = 0x00, 0x04, 0x08, 0x0C
P_C2S_COOKIE, P_C2S_PAYLOAD, P_C2S_KEEPALIVE, P_C2S_MOVE_POS, P_C2S_PONG, P_C2S_PACK = 0x11, 0x12, 0x18, 0x1A, 0x27, 0x2B
PACK_ACCEPTED, PACK_DOWNLOADED, PACK_LOADED = 3, 4, 0   # ResourcePackStatusC2SPacket.Status ordinals

# Play channels a non-quiet bot registers (S2C ids it "can receive"), as the 1.20.1 bot did; stance_v1 was retired for stance_v2.
PLAY_CHANNELS = ["gravesandglory:state_v10", "gravesandglory:impact_v1", "gravesandglory:barracks_v1", "gravesandglory:party_v1",
                 "gravesandglory:stance_v2", "gravesandglory:feed_v1", "gravesandglory:camp_station"]
GATES = ("ok", "build", "old", "protocol", "malformed", "none", "plain")


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


# ---- network NBT (1.20.2+: the root tag has a type byte and no name) and chat text ----

def read_nbt(buf, pos=0):
    """Reads one nameless network NBT tag; returns (python value, new pos). Compounds become dicts, lists become lists."""
    t = buf[pos]
    return _nbt_payload(buf, pos + 1, t)


def _nbt_payload(buf, pos, t):
    if t == 0: return None, pos
    if t == 1: return struct.unpack_from(">b", buf, pos)[0], pos + 1
    if t == 2: return struct.unpack_from(">h", buf, pos)[0], pos + 2
    if t == 3: return struct.unpack_from(">i", buf, pos)[0], pos + 4
    if t == 4: return struct.unpack_from(">q", buf, pos)[0], pos + 8
    if t == 5: return struct.unpack_from(">f", buf, pos)[0], pos + 4
    if t == 6: return struct.unpack_from(">d", buf, pos)[0], pos + 8
    if t == 7:
        n = struct.unpack_from(">i", buf, pos)[0]
        return bytes(buf[pos + 4:pos + 4 + n]), pos + 4 + n
    if t == 8:
        n = struct.unpack_from(">H", buf, pos)[0]
        return bytes(buf[pos + 2:pos + 2 + n]).decode("utf8", "replace"), pos + 2 + n  # modified UTF-8; fine for our texts
    if t == 9:
        et, n = buf[pos], struct.unpack_from(">i", buf, pos + 1)[0]
        pos += 5
        out = []
        for _ in range(max(0, n)):
            v, pos = _nbt_payload(buf, pos, et)
            out.append(v)
        return out, pos
    if t == 10:
        out = {}
        while True:
            et = buf[pos]
            pos += 1
            if et == 0:
                return out, pos
            n = struct.unpack_from(">H", buf, pos)[0]
            name = bytes(buf[pos + 2:pos + 2 + n]).decode("utf8", "replace")
            out[name], pos = _nbt_payload(buf, pos + 2 + n, et)
    if t == 11:
        n = struct.unpack_from(">i", buf, pos)[0]
        return list(struct.unpack_from(">%di" % n, buf, pos + 4)), pos + 4 + 4 * n
    if t == 12:
        n = struct.unpack_from(">i", buf, pos)[0]
        return list(struct.unpack_from(">%dq" % n, buf, pos + 4)), pos + 4 + 8 * n
    raise ValueError("bad NBT tag type %d" % t)


def text_plain(v):
    """Flattens a Text (as decoded NBT: a string, a compound or a list) to plain text."""
    if v is None:
        return ""
    if isinstance(v, str):
        return v
    if isinstance(v, list):
        return "".join(text_plain(x) for x in v)
    if isinstance(v, dict):
        if set(v) == {""}:  # a mixed-type list element is wrapped in a compound with an empty key
            return text_plain(v[""])
        s = v.get("text", "")
        if not s and "translate" in v:
            args = [text_plain(a) for a in v.get("with", [])]
            s = v.get("fallback", v["translate"]) + ("[" + ", ".join(args) + "]" if args else "")
        return str(s) + "".join(text_plain(x) for x in v.get("extra", []))
    return str(v)


def read_text(buf, pos=0):
    """A 1.21 network Text (TextCodecs.PACKET_CODEC: NBT) as plain text."""
    v, pos = read_nbt(buf, pos)
    return text_plain(v), pos


def nbt_string(s):
    """A Text that is a plain string, as network NBT (for the self-test's fake server)."""
    d = s.encode("utf8")
    return b"\x08" + struct.pack(">H", len(d)) + d


def jar_identity(jar):
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
    def __init__(self, name, jar, host="127.0.0.1", port=25640, protocol=1, gate="ok", login_gate=None, view_distance=10, identity=None):
        """protocol: the mod's CompatibilityPolicy.PROTOCOL to report. identity: (version, build) instead of reading jar."""
        if gate not in GATES:
            raise ValueError("gate must be one of " + ", ".join(GATES))
        self.name = name
        self.uuid = self._offline(name)
        self.host, self.port = host, port
        self.ver, self.build = identity if identity else jar_identity(jar)
        self.protocol = protocol
        self.gate = gate
        self.login_gate = login_gate or {"plain": "plain", "none": "ok"}.get(gate, gate)
        self.view_distance = view_distance
        self.sock = None
        self.compress = -1
        self.alive = False
        self.disconnect = None
        self.channels = {}
        self.server_channels = set()  # what the server registered (configuration and play)
        self.texts = []      # (time, overlay, text)
        self.events = []     # (time, text): phase changes and the gate exchange, for logs and assertions
        self.last_kick = None
        self.state = "login"
        self.lock = threading.Lock()
        self.send_lock = threading.Lock()
        self.play_started = None
        self.quiet = False   # no brand and no play channel registration (the release_check_v2 registration is the gate answer, always sent)
        self.trace = False
        self.sent = []

    @staticmethod
    def _offline(name):
        import hashlib
        h = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
        h[6] = (h[6] & 0x0F) | 0x30
        h[8] = (h[8] & 0x3F) | 0x80
        return uuid.UUID(bytes=bytes(h))

    def log(self, text):
        with self.lock:
            self.events.append((time.time(), text))
        if self.trace or text.startswith("could not handle"):  # handler errors always show: otherwise a decode bug looks like a hang
            print("[ggbot %s] %s" % (self.name, text), flush=True)

    # ---- framing ----

    def _send_raw(self, payload):
        if self.compress >= 0:
            if len(payload) >= self.compress:
                body = vi(len(payload)) + zlib.compress(payload)
            else:
                body = vi(0) + payload
        else:
            body = payload
        with self.send_lock:
            self.sock.sendall(vi(len(body)) + body)

    def send(self, pid, data=b""):
        if len(self.sent) < 400:
            self.sent.append((self.state, hex(pid), data[:40]))
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
        """A custom payload in the current phase (play by default; configuration while configuring)."""
        self.send(C_C2S_PAYLOAD if self.state == "configuration" else P_C2S_PAYLOAD, mstr(channel) + data)

    # ---- the compatibility answers ----

    def gate_body(self, mode):
        """The answer body for a gate mode (both gates carry the same bytes), or None for no answer."""
        if mode == "malformed":
            return b"\x05garbage"
        if mode in ("none", "plain"):
            return None
        protocol = self.protocol - 1 if mode == "protocol" else self.protocol
        ver = "0.0.0-old" if mode == "old" else self.ver
        build = "0" * 64 if mode == "build" else self.build
        return vi(protocol) + mstr(ver) + mstr(build)

    # ---- connection ----

    def connect(self, timeout=40):
        """Logs in, configures and enters play. True once in play; False with self.disconnect set otherwise."""
        self.sock = socket.create_connection((self.host, self.port), timeout=15)
        self.sock.settimeout(5)
        self.state = "login"
        self._send_raw(vi(HS_INTENTION) + vi(PROTOCOL_767) + mstr(self.host) + struct.pack(">H", self.port) + vi(2))
        self.send(L_C2S_HELLO, mstr(self.name) + self.uuid.bytes)
        self.log("handshake 767, login start as %s %s" % (self.name, self.uuid))
        t0 = time.time()
        while time.time() - t0 < timeout:
            try:
                pid, d = self._read_packet()
            except socket.timeout:
                continue
            except Exception as e:
                if self.disconnect is None:
                    self.disconnect = "socket: %r" % (e,)
                self.log("connection lost in %s: %s" % (self.state, self.disconnect))
                return False
            try:
                self._handle(pid, d)
            except Exception as e:  # a malformed packet from the server is reported, not raised
                self.log("could not handle %s packet 0x%02X: %r" % (self.state, pid, e))
            if self.disconnect is not None and self.state != "play":
                return False
            if self.state == "play":
                self.alive = True
                self.play_started = time.time()
                threading.Thread(target=self._loop, daemon=True).start()
                if not self.quiet:
                    self.plugin("minecraft:register", "\0".join(PLAY_CHANNELS).encode())
                return True
        self.disconnect = self.disconnect or "timeout in %s" % self.state
        self.log(self.disconnect)
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
                self.log("connection lost: %s" % self.disconnect)
                break
            try:
                self._handle(pid, d)
            except Exception as e:
                self.log("could not handle %s packet 0x%02X: %r" % (self.state, pid, e))

    def _handle(self, pid, d):
        getattr(self, "_" + self.state)(pid, d)

    def _login(self, pid, d):
        if pid == L_S2C_DISCONNECT:  # login disconnect: a JSON text
            raw = rstr(d, 0)[0]
            try:
                self.disconnect = text_plain(json.loads(raw))
            except ValueError:
                self.disconnect = raw
            self.log("refused in login: " + self.disconnect)
        elif pid == L_S2C_COMPRESSION:
            self.compress, _ = rvi(d, 0)
        elif pid == L_S2C_QUERY:
            mid, p = rvi(d, 0)
            ch, p = rstr(d, p)
            body = self.gate_body(self.login_gate) if ch == RELEASE_CHECK_V1 else None
            if body is None:
                self._send_raw(vi(L_C2S_QUERY_ANSWER) + vi(mid) + b"\x00")
                self.log("login query %s: not understood" % ch)
            else:
                self._send_raw(vi(L_C2S_QUERY_ANSWER) + vi(mid) + b"\x01" + body)
                self.log("login query %s: answered %s" % (ch, self.login_gate))
        elif pid == L_S2C_COOKIE:
            self.send(L_C2S_COOKIE, d[:rstr(d, 0)[1]] + b"\x00")
        elif pid == L_S2C_SUCCESS:
            self.send(L_C2S_ACK)
            self._enter_configuration()
        elif pid == L_S2C_HELLO:
            self.disconnect = "the server is in online mode (encryption request); this bot needs online-mode=false"
            self.log(self.disconnect)

    def _enter_configuration(self):
        self.state = "configuration"
        self.log("configuration")
        if not self.quiet:
            self.plugin("minecraft:brand", mstr("vanilla"))
        # Client information: locale, view distance, chat FULL, colours, all skin parts, main arm RIGHT, no filtering, listed.
        # Without it the server assumes view distance 2 (1.20.2+); 1.20.1 used the server's own distance.
        self.send(C_C2S_OPTIONS, mstr("en_us") + bytes([self.view_distance]) + vi(0) + b"\x01" + b"\x7f" + vi(1) + b"\x00\x01")

    def _configuration(self, pid, d):
        if pid == C_S2C_PAYLOAD:
            ch, p = rstr(d, 0)
            if ch == "minecraft:register":
                names = [x for x in d[p:].decode("utf8", "replace").split("\0") if x]
                self.server_channels.update(names)
                # Sent before Fabric's ping is answered, so the server sees this client's channels before CONFIGURE.
                if self.gate != "plain":
                    self.plugin("minecraft:register", RELEASE_CHECK_V2.encode())
                    self.log("configuration channels registered: " + RELEASE_CHECK_V2)
                else:
                    self.log("configuration channels: none registered (plain)")
            elif ch == RELEASE_CHECK_V2:
                body = self.gate_body(self.gate)
                if body is None:
                    self.log("release_check_v2 request: left unanswered (%s)" % self.gate)
                else:
                    self.plugin(RELEASE_CHECK_V2, body)
                    self.log("release_check_v2 request: answered %s" % self.gate)
            else:
                with self.lock:
                    self.channels.setdefault(ch, []).append(d[p:])
        elif pid == C_S2C_PING:
            self.send(C_C2S_PONG, d[:4])
        elif pid == C_S2C_KEEPALIVE:
            self.send(C_C2S_KEEPALIVE, d[:8])
        elif pid == C_S2C_KNOWN_PACKS:
            self.send(C_C2S_KNOWN_PACKS, vi(0))  # knows none: the server sends full registry data, which is ignored
        elif pid == C_S2C_PACK_PUSH:
            self._pack_status(C_C2S_PACK, d)
        elif pid == C_S2C_COOKIE:
            self.send(C_C2S_COOKIE, d[:rstr(d, 0)[1]] + b"\x00")
        elif pid == C_S2C_DISCONNECT:
            self.disconnect = read_text(d)[0]
            self.alive = False
            self.log("refused in configuration: " + self.disconnect)
        elif pid == C_S2C_FINISH:
            self.send(C_C2S_FINISH)
            self.state = "play"
            self.log("play")

    def _pack_status(self, pid, d):
        pack = d[:16]  # the pack's UUID
        for status in (PACK_ACCEPTED, PACK_DOWNLOADED, PACK_LOADED):
            self.send(pid, pack + vi(status))
        self.log("resource pack %s: reported loaded" % uuid.UUID(bytes=bytes(pack)))

    def _play(self, pid, d):
        if pid == P_S2C_KEEPALIVE:
            self.send(P_C2S_KEEPALIVE, d[:8])
        elif pid == P_S2C_DISCONNECT:
            self.disconnect = read_text(d)[0][:300]
            self.alive = False
            self.log("disconnected in play: " + self.disconnect)
        elif pid == P_S2C_PAYLOAD:
            ch, p = rstr(d, 0)
            if ch == "minecraft:register":
                self.server_channels.update(x for x in d[p:].decode("utf8", "replace").split("\0") if x)
            with self.lock:
                self.channels.setdefault(ch, []).append(d[p:])
        elif pid == P_S2C_SYSTEM_CHAT:
            s, p = read_text(d, 0)
            ov = d[p] if p < len(d) else 0
            with self.lock:
                self.texts.append((time.time(), ov, s))
        elif pid == P_S2C_POSITION:  # x, y, z, yaw, pitch, flags, teleport id
            tid, _ = rvi(d, 8 * 3 + 4 * 2 + 1)
            if d[32] & 0x07 == 0:
                self.pos = struct.unpack(">ddd", d[:24])
            self.send(P_C2S_ACCEPT_TELEPORT, vi(tid))
        elif pid == P_S2C_CHUNK_BATCH_FINISHED:
            self.send(P_C2S_CHUNK_BATCH_RECEIVED, struct.pack(">f", 25.0))
        elif pid == P_S2C_PING:
            self.send(P_C2S_PONG, d[:4])
        elif pid == P_S2C_PACK_PUSH:
            self._pack_status(P_C2S_PACK, d)
        elif pid == P_S2C_COOKIE:
            self.send(P_C2S_COOKIE, d[:rstr(d, 0)[1]] + b"\x00")
        elif pid == P_S2C_START_CONFIG:  # the server reconfigures this client
            self.send(P_C2S_CONFIG_ACK)
            self._enter_configuration()

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

    def chat_command(self, cmd):
        """A slash command without the slash (1.21.1 unsigned command packet: the command string only)."""
        self.send(P_C2S_CHAT_COMMAND, mstr(cmd))

    def move(self, dx=0.0, dz=0.0):
        """Reports standing at the last position the server set (plus an offset), as a client does every tick; the server only
        updates the player's watched chunk section from such move packets."""
        p = getattr(self, "pos", None)
        if p is not None:
            self.send(P_C2S_MOVE_POS, struct.pack(">ddd", p[0] + dx, p[1], p[2] + dz) + b"\x01")  # on ground

    def texts_since(self, t):
        with self.lock:
            return [x for x in self.texts if x[0] >= t]

    def chan_count(self, name):
        with self.lock:
            return len(self.channels.get(name, []))

    def gate_events(self):
        with self.lock:
            return [e for _, e in self.events if "release_check" in e or "login query" in e or "refused" in e or "channels" in e
                    or e.startswith("could not handle")]


# ---- self-test: a scripted fake server (framing and the phase machine only; not the real server's behaviour) ----

def _selftest():
    srv = socket.socket()
    srv.bind(("127.0.0.1", 0))
    srv.listen(4)
    port = srv.getsockname()[1]
    seen = {}

    def frame(pid, data, compress):
        payload = vi(pid) + data
        body = (vi(0) + payload) if compress else payload
        return vi(len(body)) + body

    def serve(case):
        conn, _ = srv.accept()
        conn.settimeout(10)
        fake = Bot.__new__(Bot)
        fake.sock, fake.compress = conn, -1
        got = []

        def read():
            pid, d = Bot._read_packet(fake)
            got.append((pid, d))
            return pid, d
        read()  # handshake
        read()  # hello
        if case == "plain-login-refused":
            conn.sendall(frame(L_S2C_QUERY, vi(7) + mstr(RELEASE_CHECK_V1), False))
            pid, d = read()
            seen[case + ":answer"] = d
            refusal = "This server requires Graves and Glory 9.9.9 (build abc). Install the same modpack release as the server, then reconnect."
            conn.sendall(frame(L_S2C_DISCONNECT, mstr(json.dumps({"text": refusal})), False))
            conn.close()
            return
        conn.sendall(frame(L_S2C_COMPRESSION, vi(256), False))
        fake.compress = 256
        conn.sendall(frame(L_S2C_QUERY, vi(3) + mstr(RELEASE_CHECK_V1), True))
        seen[case + ":login"] = read()[1]
        conn.sendall(frame(L_S2C_SUCCESS, uuid.uuid4().bytes + mstr("x") + vi(0) + b"\x01", True))
        assert read()[0] == L_C2S_ACK
        conn.sendall(frame(C_S2C_PAYLOAD, mstr("minecraft:register") + b"fabric:registry/sync\0" + RELEASE_CHECK_V2.encode(), True))
        conn.sendall(frame(C_S2C_PING, struct.pack(">i", 0xFAB71C), True))
        pids = []  # brand (not quiet), options, our register, then the pong
        while C_C2S_PONG not in pids:
            pid, d = read()
            pids.append(pid)
            if pid == C_C2S_PAYLOAD:
                seen.setdefault(case + ":config_payloads", []).append(rstr(d, 0)[0])
        seen[case + ":order"] = pids
        conn.sendall(frame(C_S2C_PAYLOAD, mstr(RELEASE_CHECK_V2), True))
        if case == "build-config-refused":
            seen[case + ":reply"] = read()[1]
            conn.sendall(frame(C_S2C_DISCONNECT, nbt_string("Your Graves and Glory pack does not match this server."), True))
            conn.close()
            return
        seen[case + ":reply"] = read()[1]
        conn.sendall(frame(C_S2C_KNOWN_PACKS, vi(1) + mstr("minecraft") + mstr("core") + mstr("1.21.1"), True))
        assert read()[0] == C_C2S_KNOWN_PACKS
        conn.sendall(frame(C_S2C_REGISTRY, mstr("minecraft:dimension_type") + vi(0), True))
        conn.sendall(frame(C_S2C_FINISH, b"", True))
        assert read()[0] == C_C2S_FINISH
        big = b"\x00" * 600  # a compressed play packet
        payload = vi(P_S2C_PAYLOAD) + mstr("gravesandglory:state_v10") + big
        body = vi(len(payload)) + zlib.compress(payload)
        conn.sendall(vi(len(body)) + body)
        # system chat {"text": "Warps in lobby: ", "extra": [{"": "center, "}, {"text": "far"}]}, then overlay false
        conn.sendall(frame(P_S2C_SYSTEM_CHAT, b"\x0a" + b"\x08" + struct.pack(">H", 4) + b"text" + struct.pack(">H", 16) + b"Warps in lobby: "
                           + b"\x09" + struct.pack(">H", 5) + b"extra" + b"\x0a" + struct.pack(">i", 2)
                           + b"\x08" + struct.pack(">H", 0) + struct.pack(">H", 8) + b"center, " + b"\x00"
                           + b"\x08" + struct.pack(">H", 4) + b"text" + struct.pack(">H", 3) + b"far" + b"\x00" + b"\x00" + b"\x00", True))
        conn.sendall(frame(P_S2C_KEEPALIVE, struct.pack(">q", 42), True))
        conn.sendall(frame(P_S2C_POSITION, struct.pack(">dddffb", 1.5, 64.0, -2.5, 0.0, 0.0, 0) + vi(9), True))
        want = {P_C2S_KEEPALIVE, P_C2S_ACCEPT_TELEPORT}
        play = []
        while not want.issubset({p for p, _ in play}):
            play.append(read())
        seen[case + ":play"] = play
        conn.sendall(frame(P_S2C_DISCONNECT, nbt_string("bye"), True))
        time.sleep(0.5)
        conn.close()

    fails = []

    def check(name, ok, detail=""):
        print(("PASS " if ok else "FAIL ") + name + ("" if ok else " :: " + str(detail)), flush=True)
        if not ok:
            fails.append(name)

    ident = ("9.9.9", "a" * 64)
    # 1. a matching modded bot joins: login answer, config register before pong, release_check_v2 reply, play
    t = threading.Thread(target=serve, args=("join",), daemon=True); t.start()
    b = Bot("SelfTest", None, port=port, identity=ident)
    ok = b.connect(timeout=10)
    time.sleep(0.8)
    check("join: reaches play", ok, b.disconnect)
    want = vi(1) + mstr("9.9.9") + mstr("a" * 64)
    check("join: login query answered with the identity", seen.get("join:login") == vi(3) + b"\x01" + want, seen.get("join:login"))
    check("join: release_check_v2 registered before the pong", seen.get("join:config_payloads", [])[-1:] == ["minecraft:register"]
          and seen["join:order"][-1] == C_C2S_PONG, seen.get("join:order"))
    check("join: release_check_v2 reply is protocol, version, build", seen.get("join:reply") == mstr(RELEASE_CHECK_V2) + want, seen.get("join:reply"))
    check("join: compressed play payload received", b.chan_count("gravesandglory:state_v10") == 1, b.channels.keys())
    check("join: system chat NBT flattened", [x[2] for x in b.texts] == ["Warps in lobby: center, far"], b.texts)
    check("join: teleport accepted and position kept", getattr(b, "pos", None) == (1.5, 64.0, -2.5), getattr(b, "pos", None))
    check("join: play disconnect text read", b.disconnect == "bye", b.disconnect)
    t.join(5); b.close()
    # 2. a wrong build is refused by the configuration gate; the refusal text is read from NBT
    t = threading.Thread(target=serve, args=("build-config-refused",), daemon=True); t.start()
    b = Bot("SelfTestB", None, port=port, identity=ident, gate="build")
    ok = b.connect(timeout=10)
    check("build: not admitted", not ok)
    check("build: reply carries the wrong build", seen.get("build-config-refused:reply") == mstr(RELEASE_CHECK_V2) + vi(1) + mstr("9.9.9") + mstr("0" * 64))
    check("build: configuration refusal text", b.disconnect == "Your Graves and Glory pack does not match this server.", b.disconnect)
    t.join(5); b.close()
    # 3. a plain bot answers the login query "not understood" and reads a JSON login refusal
    t = threading.Thread(target=serve, args=("plain-login-refused",), daemon=True); t.start()
    b = Bot("SelfTestP", None, port=port, identity=ident, gate="plain")
    ok = b.connect(timeout=10)
    check("plain: not admitted", not ok)
    check("plain: login query not understood", seen.get("plain-login-refused:answer") == vi(7) + b"\x00", seen.get("plain-login-refused:answer"))
    check("plain: login refusal text", b.disconnect.startswith("This server requires Graves and Glory 9.9.9"), b.disconnect)
    t.join(5); b.close()
    srv.close()
    print("SELFTEST", "PASS" if not fails else "FAIL " + ", ".join(fails))
    return 0 if not fails else 1


def main(argv):
    import argparse
    ap = argparse.ArgumentParser(description="Protocol-767 Graves and Glory test bot (offline-mode dev servers only).")
    ap.add_argument("jar", nargs="?", help="the mod jar, or the repo folder of a dev server (build/resources/main)")
    ap.add_argument("--selftest", action="store_true", help="framing and phase machine against a scripted fake server")
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=25640)
    ap.add_argument("--name", default="QaBot" + time.strftime("%H%M%S"))
    ap.add_argument("--gate", choices=GATES, default="ok", help="the release_check_v2 answer (and the login answer unless --login-gate)")
    ap.add_argument("--login-gate", choices=GATES, default=None, help="the release_check_v1 login answer alone")
    ap.add_argument("--protocol", type=int, default=1, help="CompatibilityPolicy.PROTOCOL to report")
    ap.add_argument("--expect", choices=("join", "refused"), default="join")
    ap.add_argument("--stay", type=float, default=10.0, help="seconds to stay in play")
    ap.add_argument("--chat", action="append", default=[], help="a command to run once in play (no slash); repeatable")
    ap.add_argument("--quiet", action="store_true", help="no brand and no play channel registration")
    ap.add_argument("--trace", action="store_true", help="print every phase change and gate exchange as it happens")
    a = ap.parse_args(argv)
    if a.selftest:
        return _selftest()
    if not a.jar:
        ap.error("jar (or repo folder) is required")
    b = Bot(a.name, a.jar, host=a.host, port=a.port, protocol=a.protocol, gate=a.gate, login_gate=a.login_gate)
    b.quiet, b.trace = a.quiet, a.trace
    print("ggbot %s -> %s:%d gate=%s login=%s identity=%s/%s" % (b.name, a.host, a.port, b.gate, b.login_gate, b.ver, b.build[:8]), flush=True)
    joined = b.connect()
    if joined:
        t0 = time.time()
        for c in a.chat:
            b.chat_command(c)
            time.sleep(2)
        while b.alive and time.time() - t0 < a.stay:
            time.sleep(0.25)
        for _, _, s in b.texts:
            print("  chat: " + s[:200])
    for e in b.gate_events():
        print("  " + e)
    stayed = joined and b.alive
    print("  outcome: " + ("in play" if stayed else "joined, then lost: %s" % b.disconnect if joined else "refused: %s" % b.disconnect))
    b.close()
    ok = stayed if a.expect == "join" else not joined
    print("RESULT", "PASS" if ok else "FAIL", "(expect %s)" % a.expect, flush=True)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
