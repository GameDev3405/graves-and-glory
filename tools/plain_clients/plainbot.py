"""Plain clients (port plan P3; from the P2 spike): a ggbot that acts as a vanilla 1.21.1 client (no channels, no gate answer) and audits what the server sends it:
entity spawn types (a raw id at or above vanilla's 130 would crash a vanilla client), sound ids (at or above 1611 likewise; 0 = sent by name),
the HUD packets, menus and the resource pack push. With --script it also drives the P2.3 order checks over RCON and packets.

  python tools/plain_clients/plainbot.py <port> [--name N] [--stay S] [--script] [--out file.json]
Unshipped test harness (port plan P3; from the P2 spike).
"""
import os, sys, struct, time, json, argparse, collections
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "test-tools", "qa"))
sys.path.insert(0, HERE)
import ggbot
from ggbot import vi, rvi, rstr, mstr, read_text
import server as spike

VANILLA_ENTITY_TYPES, VANILLA_SOUNDS = 130, 1611
TYPE_NAMES = {54: "husk", 91: "skeleton", 118: "witch", 124: "zombie", 80: "pillager", 114: "vindicator", 113: "villager", 18: "chest_minecart", 128: "player", 40: "falling_block", 105: "text_display", 3: "armor_stand"}
ADD_ENTITY, SOUND_ENTITY, SOUND, BOSS, OPEN_SCREEN, OBJECTIVE, ACTION_BAR, CONTAINER_CONTENT = 0x01, 0x67, 0x68, 0x0A, 0x33, 0x5E, 0x4C, 0x13
C2S_USE_ITEM, C2S_CARRIED, C2S_CONTAINER_CLICK = 0x39, 0x2F, 0x0E


class PlainBot(ggbot.Bot):
    def __init__(self, *a, **k):
        super().__init__(*a, **k)
        self.audit = collections.Counter()
        self.types = collections.Counter()
        self.bad_types, self.bad_sounds, self.sound_names = [], [], collections.Counter()
        self.boss, self.actionbar, self.screens, self.objectives = [], [], [], []
        self.window, self.state_id, self.look = None, 0, (0.0, 0.0)
        self.seq = 1
        self.block_states, self.bad_blocks = collections.Counter(), []
        self.key_leaks = []

    def _play(self, pid, d):
        try:
            self._audit(pid, d)
        except Exception:
            self.audit["decode-error-0x%02X" % pid] += 1
        super()._play(pid, d)

    def _audit(self, pid, d):
        self.audit["0x%02X" % pid] += 1
        if pid in (0x6C, ACTION_BAR, BOSS, OPEN_SCREEN, OBJECTIVE, 0x61, CONTAINER_CONTENT, 0x15):  # P3.4: no binding id, key token or keybind text in any text sent to a plain client
            for needle in (b"key.gravesandglory", b"{key:", b"keybind"):
                if needle in d:
                    self.key_leaks.append("0x%02X %s %r" % (pid, needle.decode(), d[:160]))
        if pid == ADD_ENTITY:
            eid, p = rvi(d, 0)
            t, p = rvi(d, p + 16)
            self.types[TYPE_NAMES.get(t, str(t))] += 1
            if t >= VANILLA_ENTITY_TYPES:
                self.bad_types.append(t)
        elif pid in (SOUND, SOUND_ENTITY):
            sid, p = rvi(d, 0)
            if sid == 0:
                name, _ = rstr(d, p)
                self.sound_names[name] += 1
            else:
                self.sound_names["#%d" % (sid - 1)] += 1
                if sid - 1 >= VANILLA_SOUNDS:
                    self.bad_sounds.append(sid - 1)
        elif pid == BOSS:
            act, p = rvi(d, 16)
            if act in (0, 3):
                txt, _ = read_text(d, p)
                if not self.boss or self.boss[-1] != txt:
                    self.boss.append(txt)
        elif pid == ACTION_BAR:
            txt, _ = read_text(d, 0)
            if not self.actionbar or self.actionbar[-1] != txt:
                self.actionbar.append(txt)
        elif pid == OPEN_SCREEN:
            w, p = rvi(d, 0)
            kind, p = rvi(d, p)
            title, _ = read_text(d, p)
            self.window = w
            self.screens.append((w, kind, title))
        elif pid == CONTAINER_CONTENT:
            self.state_id, _ = rvi(d, 1)
        elif pid == OBJECTIVE:
            name, p = rstr(d, 0)
            self.objectives.append((name, d[p]))
        elif pid == 0x09:  # block_update: position (long), block state id
            st, _ = rvi(d, 8)
            self.block_states[st] += 1
            if st > 26683:
                self.bad_blocks.append(st)
        elif pid == 0x49:  # section_blocks_update: section (long), count, varlongs (state << 12 | local position)
            n, p = rvi(d, 8)
            for _ in range(n):
                v, sh = 0, 0
                while True:
                    c = d[p]; p += 1
                    v |= (c & 0x7F) << sh; sh += 7
                    if not c & 0x80:
                        break
                st = v >> 12
                self.block_states[st] += 1
                if st > 26683:
                    self.bad_blocks.append(st)
        elif pid == 0x40:  # position: remember the look the server set
            self.look = struct.unpack(">ff", d[24:32])

    def use_slot(self, slot, key_only=True):
        """A hotbar key press (set_carried_item); with key_only False also a right-click (use_item: hand, sequence, yaw, pitch).
        In a running battle the key alone is the order (the server snaps the hotbar back to slot 0); in camp the right-click is."""
        self.send(C2S_CARRIED, struct.pack(">h", slot))
        if key_only:
            return
        time.sleep(0.15)
        self.seq += 1
        self.send(C2S_USE_ITEM, vi(0) + vi(self.seq) + struct.pack(">ff", *self.look))

    def click(self, slot):
        """A left click on a slot of the open menu (1.21.1: byte window id)."""
        self.send(C2S_CONTAINER_CLICK, bytes([self.window & 0xFF]) + vi(self.state_id) + struct.pack(">h", slot) + b"\x00" + vi(0) + vi(0) + vi(0))

    def tactical_v2(self, action, formation, yaw, groups, target=-1):
        """The modded keybind's packet (CommandSelection.issueSelected), sent raw as the keybind would."""
        body = mstr(action) + mstr(formation) + struct.pack(">fi", yaw, target) + struct.pack(">i", len(groups))
        for g, x, z in groups:
            body += mstr(g) + struct.pack(">dd", x, z)
        self.plugin("gravesandglory:tactical_v2", body)

    def summary(self):
        return {"types": dict(self.types), "bad_entity_types": self.bad_types[:20], "bad_sounds": self.bad_sounds[:20], "bad_block_states": self.bad_blocks[:20], "block_updates": dict(self.block_states.most_common(10)),
                "sounds": dict(self.sound_names.most_common(25)), "boss_titles": self.boss[-8:], "action_bars": self.actionbar[-8:],
                "screens": self.screens, "objectives": self.objectives[:6], "pack_pushes": self.audit.get("0x46", 0),
                "decode_errors": {k: v for k, v in self.audit.items() if k.startswith("decode")}, "chat": [t[2][:200] for t in self.texts][-20:],
                "disconnect": self.disconnect, "alive": self.alive, "key_leaks": self.key_leaks[:10],
                "custom_payloads": {ch: len(v) for ch, v in self.channels.items()}, "mod_payloads": self.mod_payloads()}

    def mod_payloads(self):
        """P3.3: the mod's custom payloads (gravesandglory:*, and Fabric's own) a plain client was sent: there must be none. Polymer's
        polymer:hello is Polymer's own handshake; a vanilla client reads an unknown channel as a discarded payload, so it is listed in
        custom_payloads and not failed here."""
        return {ch: len(v) for ch, v in self.channels.items() if ch.startswith("gravesandglory:") or ch.startswith("fabric")}


def rc(port, cmd):
    out = spike.rcon(port, cmd)
    print("  rcon> %s :: %s" % (cmd, out.strip()[:160]), flush=True)
    return out


def wait_state(port, needle, timeout):
    t0 = time.time()
    while time.time() - t0 < timeout:
        try:
            st = spike.rcon(port, "plainprobe state")
        except OSError:
            st = ""
        if needle in st:
            print("  state: " + st[:200], flush=True)
            return st
        time.sleep(2)
    print("  state never had " + needle, flush=True)
    return None


def script(b, port, name):
    """P2.3/P2.4 drive: recruit menu in camp, a practice battle, then orders by item against the keybind packet."""
    wait_state(port, "ready=true", 180)
    rc(port, "plainprobe writs %s 30" % name)  # a new server profile has no writs, so the Enlist click needs some
    time.sleep(1)
    rc(port, "plainprobe recruit " + name)
    time.sleep(2)
    if b.window is not None:
        b.click(6)  # Enlist (roll:<house>) if the profile has writs
        time.sleep(2)
        b.click(1)  # show the second House
        time.sleep(2)
    rc(port, "plainprobe practice " + name)
    st = wait_state(port, "running=true", 180)
    import re
    m = re.search(r"time=(\d+) intro=(\d+)", st or "")
    if m and int(m.group(2)) > int(m.group(1)):
        time.sleep((int(m.group(2)) - int(m.group(1))) / 20 + 2)  # past the intro
    rc(port, "plainprobe state")
    rc(port, "plainprobe aim %s 20" % name)
    time.sleep(1)
    print("  -- item order: hold (slot 3), aimed 20 blocks", flush=True)
    b.use_slot(3)
    time.sleep(0.5)
    print("  -- item order again at once: expect the command cooldown to refuse it", flush=True)
    b.use_slot(1)
    time.sleep(4)
    rc(port, "plainprobe state")
    print("  -- keybind packet (tactical_v2 attack), then an item order at once: one shared cooldown", flush=True)
    b.tactical_v2("attack", "block", b.look[0], [("all", 0.0, 0.0)])
    b.use_slot(1)  # same TCP burst: handled in the same tick, inside the 4-tick command cooldown
    time.sleep(1)
    rc(port, "plainprobe state")
    time.sleep(4)
    print("  -- item charge twice: the second meets the charge cooldown", flush=True)
    b.use_slot(2)
    time.sleep(4)
    b.use_slot(2)
    time.sleep(4)
    print("  -- range: aimed at ground 200 blocks ahead (in range), then 260 (beyond 240: a miss)", flush=True)
    rc(port, "plainprobe aim %s 200" % name)
    time.sleep(1)
    b.use_slot(3)
    time.sleep(4)
    rc(port, "plainprobe aim %s 260" % name)
    time.sleep(1)
    b.use_slot(3)
    time.sleep(2)
    rc(port, "plainprobe aim %s 20" % name)
    time.sleep(1)
    print("  -- keyorder (server-side keybind equivalent) follow, then item retreat", flush=True)
    rc(port, "plainprobe keyorder %s follow" % name)
    time.sleep(4)
    b.use_slot(5)
    time.sleep(3)
    rc(port, "plainprobe state")


def registry_check(b, port, name):
    """P2.2 for the wagon and the two map blocks: spawn a wagon and set both blocks beside the bot, then check what it was sent."""
    wait_state(port, "ready=true", 180)
    time.sleep(3)
    before = dict(b.types)
    rc(port, "plainprobe wagon " + name)
    rc(port, "plainprobe blocks " + name)
    time.sleep(2)
    rc(port, "execute at %s run setblock ~ ~3 ~ minecraft:gold_block" % name)  # a vanilla control block
    time.sleep(1)
    rc(port, "execute at %s run setblock ~4 ~3 ~ gravesandglory:troop_fence" % name)  # one custom block alone in its tick
    time.sleep(3)
    print("  block updates seen: 0x09=%d 0x49=%d; gold(2091)=%d barrier(10365)=%d" % (b.audit.get("0x09", 0), b.audit.get("0x49", 0), b.block_states.get(2091, 0), b.block_states.get(10365, 0)), flush=True)
    print("  new spawns after the wagon: %s" % {k: v - before.get(k, 0) for k, v in b.types.items() if v != before.get(k, 0)}, flush=True)
    print("  block updates: %s  over vanilla: %s" % (dict(b.block_states), b.bad_blocks), flush=True)


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("port", type=int)
    ap.add_argument("--name", default="PlainBot")
    ap.add_argument("--stay", type=float, default=30)
    ap.add_argument("--script", action="store_true")
    ap.add_argument("--out")
    ap.add_argument("--registry", action="store_true", help="the wagon and map-block check")
    a = ap.parse_args(argv)
    b = PlainBot(a.name, None, port=a.port, gate="plain", identity=("0", "0"))
    b.quiet = True
    ok = b.connect(timeout=180)
    print("joined" if ok else "refused: %s" % b.disconnect, flush=True)
    if ok:
        t0 = time.time()
        if a.registry:
            registry_check(b, a.port, a.name)
        if a.script:
            script(b, a.port, a.name)
        while b.alive and time.time() - t0 < a.stay:
            b.move()
            time.sleep(0.5)
    s = b.summary()
    b.close()
    print(json.dumps(s, indent=1, ensure_ascii=True)[:6000])
    if a.out:
        with open(a.out, "w", encoding="utf-8") as f:
            json.dump(s, f, indent=1)
    clean = not s["bad_entity_types"] and not s["bad_sounds"] and not s["bad_block_states"] and not s["key_leaks"] and not s["mod_payloads"] and not s["decode_errors"]
    print("PLAIN AUDIT " + ("PASS" if ok and clean else "FAIL"), flush=True)
    return 0 if ok and clean else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
