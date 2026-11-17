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
C2S_USE_ITEM, C2S_CARRIED, C2S_CONTAINER_CLICK, C2S_CONTAINER_CLOSE, C2S_PLAYER_ACTION = 0x39, 0x2F, 0x0E, 0x0F, 0x24
SET_SLOT = 0x15
MOD_NEEDLE = b"gravesandglory"
# P4: the plain War Table pages the menu checks open (camp); the Armoury is checked in battle.
PAGES = ["wartable", "recruit", "banner", "contracts", "chapter", "friends", "field", "results", "gear"]


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
        self.odd = []
        self.item_leaks = []

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
        if pid in (CONTAINER_CONTENT, SET_SLOT) and MOD_NEEDLE in d.replace(b"gravesandglory:plain", b"").replace(b"gravesandglory:title", b""):  # the required pack's fonts  # P4: no mod id in any menu or inventory stack (item_model, components)
            self.item_leaks.append("0x%02X %r" % (pid, d[:200]))
        if pid == ADD_ENTITY:
            eid, p = rvi(d, 0)
            t, _ = rvi(d, p + 16)
            self.types[TYPE_NAMES.get(t, str(t))] += 1
            if t not in TYPE_NAMES and len(self.odd) < 40:
                u = d[p:p + 16].hex()
                self.odd.append((t, eid, "%s-%s-%s-%s-%s" % (u[:8], u[8:12], u[12:16], u[16:20], u[20:])))
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

    def click(self, slot, button=0, mode=0, changed=(), carried=None):
        """A click on a slot of the open menu (1.21.1: byte window id, state id, slot, button, mode, the slots the client claims changed,
        the item it claims to carry). changed: (slot, item raw id, count); carried: (item raw id, count) or None for empty. The claims
        are what a cheating client would send; the server must ignore them."""
        def item(it):
            if it is None or it[1] <= 0:
                return vi(0)
            return vi(it[1]) + vi(it[0]) + vi(0) + vi(0)
        body = bytes([self.window & 0xFF]) + vi(self.state_id) + struct.pack(">h", slot) + bytes([button & 0xFF]) + vi(mode)
        body += vi(len(changed)) + b"".join(struct.pack(">h", sl) + item((iid, n)) for sl, iid, n in changed)
        body += item(carried)
        self.send(C2S_CONTAINER_CLICK, body)

    def close_window(self):
        self.send(C2S_CONTAINER_CLOSE, bytes([self.window & 0xFF]))

    def player_action(self, status):
        """player_action: 3 drop stack (ctrl-Q), 4 drop item (Q), 6 swap hands (F); position 0, face 0."""
        self.seq += 1
        self.send(C2S_PLAYER_ACTION, vi(status) + struct.pack(">q", 0) + b"\x00" + vi(self.seq))

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
                "disconnect": self.disconnect, "alive": self.alive, "key_leaks": self.key_leaks[:10], "item_leaks": self.item_leaks[:10],
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
    rc(port, "plainprobe view %s mage" % name)  # near the enemy mage: its spawn should come as a witch (type 118)
    time.sleep(3)
    rc(port, "plainprobe view %s knight" % name)
    rc(port, "plainprobe view %s archer" % name)
    rc(port, "plainprobe overview " + name)
    time.sleep(2)
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


def inv(port, name):
    """The server's view of the player (PlainProbe.inv) as a dict: items (the inventory part), cursor, screen, moditems, drops, pane, button."""
    out = spike.rcon(port, "plainprobe inv " + name).strip()
    d = {"raw": out}
    parts = out.split(" cursor=")
    d["items"] = parts[0]
    for kv in out.split():
        if "=" in kv:
            k, v = kv.split("=", 1)
            d[k] = v
    return d


# Each exploit: (name, the clicks it sends as (slot-key, button, mode, changed, carried)), slot-key one of "menu" (a button slot),
# "pane" (a background slot), "player" (the player's hotbar slot 1 in the window), "outside" (-999). Changed and carried are lies
# (stone, raw id 1, 64): the server must ignore them.
LIE = (1, 64)
EXPLOITS = (
    [("shift-menu", [("menu", 0, 1, (), None)]), ("shift-player", [("player", 0, 1, (), None)])]
    + [("swap%d-menu" % k, [("menu", k, 2, (), None)]) for k in range(9)]
    + [("swap%d-player" % k, [("player", k, 2, (), None)]) for k in (0, 1, 7)]
    + [("offhand-menu", [("menu", 40, 2, (), None)]), ("offhand-player", [("player", 40, 2, (), None)]),
       ("clone-menu", [("menu", 2, 3, (), None)]),
       ("q-menu", [("menu", 0, 4, (), None)]), ("ctrlq-menu", [("menu", 1, 4, (), None)]),
       ("q-player", [("player", 0, 4, (), None)]), ("ctrlq-player", [("player", 1, 4, (), None)]),
       ("drag-menu", [("outside", 0, 5, (), None), ("menu", 1, 5, (), None), ("pane", 1, 5, (), None), ("outside", 2, 5, (), None)]),
       ("drag-player", [("outside", 4, 5, (), None), ("player", 5, 5, (), None), ("outside", 6, 5, (), None)]),
       ("collect-menu", [("menu", 0, 6, (), None)]), ("collect-player", [("player", 0, 6, (), None)]),
       ("outside-left", [("outside", 0, 0, (), None)]), ("outside-right", [("outside", 1, 0, (), None)]),
       ("lie-take", [("pane", 0, 0, [("player", LIE[0], LIE[1])], LIE)]),
       ("lie-carry-close", [("pane", 0, 0, (), LIE), ("close", 0, 0, (), None)]),
       ("pickup-close", [("pane", 0, 0, (), None), ("close", 0, 0, (), None)]),
       ("double-click", [("pane", 0, 0, (), None), ("pane", 0, 6, (), None)])])


def menu_checks(b, port, name, pages, hotbar=True):
    """P4 dupe checks: for each page, every exploit against a button slot, a background slot and the player's own slots; after each the
    server's inventory, cursor, drops and the menu's vanilla-only stacks must be as before. Returns the failures."""
    fails = []
    for page in pages:
        rc(port, "plainprobe open %s %s" % (name, page))
        time.sleep(1.5)
        base = inv(port, name)
        if not base.get("screen", "").startswith("PlainMenu"):
            fails.append("%s: did not open (%s)" % (page, base["raw"][:160]))
            continue
        if base.get("moditems") != "0":
            fails.append("%s: menu holds mod items (%s)" % (page, base["moditems"]))
        size = int(base.get("menu", "54"))
        slots = {"menu": int(base.get("button", "-1")), "pane": int(base.get("pane", "-1")), "player": size + 27 + 1, "outside": -999}
        if slots["menu"] < 0:
            slots["menu"] = 0
        if slots["pane"] < 0:
            slots["pane"] = slots["menu"]
        for ex, clicks in EXPLOITS:
            reopened = False
            for key, button, mode, changed, carried in clicks:
                if key == "close":
                    b.close_window()
                    reopened = True
                    continue
                ch = [(slots[c[0]] if isinstance(c[0], str) else c[0], c[1], c[2]) for c in changed]
                b.click(slots[key], button, mode, ch, carried)
                time.sleep(0.12)
            time.sleep(0.5)
            now = inv(port, name)
            bad = []
            if now["items"] != base["items"]:
                bad.append("inventory changed: %s -> %s" % (base["items"][-160:], now["items"][-160:]))
            if now.get("cursor") != "empty":
                bad.append("cursor " + now.get("cursor", "?"))
            if now.get("drops") != base.get("drops"):
                bad.append("drops %s -> %s" % (base.get("drops"), now.get("drops")))
            if not reopened and now.get("moditems") not in (None, "0"):
                bad.append("mod items in menu")
            print("  menu %-9s %-16s %s" % (page, ex, "ok" if not bad else "FAIL " + "; ".join(bad)), flush=True)
            fails += ["%s %s: %s" % (page, ex, x) for x in bad]
            if reopened or not now.get("screen", "").startswith("PlainMenu"):
                rc(port, "plainprobe open %s %s" % (name, page))
                time.sleep(1.2)
                base = inv(port, name)
        b.close_window()
        time.sleep(0.5)
    if not hotbar:
        return fails
    # the hotbar with no menu: F, Q and ctrl-Q on a plain item never leave a copy, an offhand item or a drop
    b.use_slot(1)
    time.sleep(0.5)
    base = inv(port, name)
    for ex, status in (("F-hotbar", 6), ("Q-hotbar", 4), ("ctrlQ-hotbar", 3)):
        b.player_action(status)
        time.sleep(1.2)  # the locked hotbar refills every 10 ticks
        now = inv(port, name)
        bad = []
        if now["items"] != base["items"]:
            bad.append("inventory changed: %s -> %s" % (base["items"][-160:], now["items"][-160:]))
        if now.get("drops") != base.get("drops"):
            bad.append("drops %s -> %s" % (base.get("drops"), now.get("drops")))
        print("  hotbar %-14s %s" % (ex, "ok" if not bad else "FAIL " + "; ".join(bad)), flush=True)
        fails += ["%s: %s" % (ex, x) for x in bad]
    b.use_slot(0)
    return fails


WINDOW0 = [  # review H1: the player's own inventory screen; hotbar slot 1 (an order item) is window-0 slot 37, the 2x2 grid 1-4
    ("w0 pickup", [(37, 0, 0)]), ("w0 shift", [(37, 0, 1)]), ("w0 key2->inv", [(10, 1, 2)]), ("w0 offhand", [(37, 40, 2)]),
    ("w0 throw", [(37, 0, 4)]), ("w0 ctrl-throw", [(37, 1, 4)]), ("w0 grid", [(37, 0, 0), (1, 0, 0)]),
    ("w0 drag", [(37, 0, 0), (-999, 0, 5), (1, 1, 5), (2, 1, 5), (-999, 2, 5)]), ("w0 collect", [(37, 0, 6)]),
]


def window0_checks(b, port, name):
    """P4 review H1: no click in a plain player's own inventory screen moves, throws or crafts a plain hotbar item (server truth)."""
    fails = []
    if b.window:
        b.close_window()
    time.sleep(1)
    base = inv(port, name)
    for ex, clicks in WINDOW0:
        b.window = 0
        for slot, button, mode in clicks:
            b.click(slot, button, mode, (), None)
            time.sleep(0.12)
        time.sleep(0.8)
        now = inv(port, name)
        bad = []
        if now["items"] != base["items"]:
            bad.append("inventory changed: %s -> %s" % (base["items"][-160:], now["items"][-160:]))
        if now.get("cursor") != "empty":
            bad.append("cursor " + now.get("cursor", "?"))
        if now.get("drops") != base.get("drops"):
            bad.append("drops %s -> %s" % (base.get("drops"), now.get("drops")))
        print("  window0 %-14s %s" % (ex, "ok" if not bad else "FAIL " + "; ".join(bad)), flush=True)
        fails += ["window0 %s: %s" % (ex, "; ".join(bad))] if bad else []
    return fails


def tactics_script(b, port, name):
    """P4 formations and relics in a practice battle: F to the tactics page, the Wedge key, F back, a Hold that keeps the wedge, then the
    relic in slot 1, then the Armoury's dupe checks. Returns the failures."""
    fails = []
    rc(port, "plainprobe relic " + name)
    time.sleep(1)
    rc(port, "plainprobe practice " + name)
    st = wait_state(port, "running=true", 180)
    import re
    m = re.search(r"time=(\d+) intro=(\d+)", st or "")
    if m and int(m.group(2)) > int(m.group(1)):
        time.sleep((int(m.group(2)) - int(m.group(1))) / 20 + 2)
    rc(port, "plainprobe aim %s 20" % name)
    b.player_action(6)  # F: the tactics page
    time.sleep(1.2)
    t = inv(port, name)["raw"]
    if "formation:wedge" not in t or "relic:0" not in t:
        fails.append("tactics page not shown: " + t[:200])
    wedge = next((int(k.split(":")[0]) for k in t.split() if k.endswith(":formation:wedge")), 3)
    b.use_slot(wedge)
    time.sleep(1.5)
    sq = spike.rcon(port, "plainprobe squad " + name)
    print("  " + sq.strip()[:240], flush=True)
    if "shape=wedge" not in sq or "/wedge" not in sq:
        fails.append("wedge not taken: " + sq.strip()[:200])
    b.player_action(6)  # F: back to orders
    time.sleep(1.2)
    b.use_slot(3)  # Hold
    time.sleep(1.5)
    sq = spike.rcon(port, "plainprobe squad " + name)
    print("  " + sq.strip()[:240], flush=True)
    if "/hold/wedge" not in sq:
        fails.append("Hold lost the wedge: " + sq.strip()[:200])
    b.player_action(6)
    time.sleep(1.2)
    relic = next((int(k.split(":")[0]) for k in inv(port, name)["raw"].split() if k.endswith(":relic:0")), 5)
    b.use_slot(relic)
    time.sleep(1.5)
    sq = spike.rcon(port, "plainprobe squad " + name)
    print("  " + sq.strip()[:240], flush=True)
    import re as _re
    mm = _re.search(r"0:[a-z_]+:ready(\d+)", sq)
    if not mm or int(mm.group(1)) <= 0:
        fails.append("relic slot 1 did not fire: " + sq.strip()[:200])
    t = inv(port, name)["raw"]
    print("  tactics hotbar after the relic: " + t[:300], flush=True)
    fails += menu_checks(b, port, name, ["armoury"], hotbar=False)
    b.player_action(6)
    return fails


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
    ap.add_argument("--menus", action="store_true", help="P4: every War Table page's dupe checks, then formations, relics and the Armoury in a practice battle")
    a = ap.parse_args(argv)
    b = PlainBot(a.name, None, port=a.port, gate="plain", identity=("0", "0"))
    b.quiet = True
    ok = b.connect(timeout=180)
    print("joined" if ok else "refused: %s" % b.disconnect, flush=True)
    menu_fails = None
    if ok:
        t0 = time.time()
        if a.registry:
            registry_check(b, a.port, a.name)
        if a.script:
            script(b, a.port, a.name)
        if a.menus:
            wait_state(a.port, "ready=true", 180)
            time.sleep(2)
            menu_fails = menu_checks(b, a.port, a.name, PAGES)
            menu_fails += window0_checks(b, a.port, a.name)
            menu_fails += tactics_script(b, a.port, a.name)
        while b.alive and time.time() - t0 < a.stay:
            b.move()
            time.sleep(0.5)
    for t, eid, u in b.odd[:6]:  # what the server holds behind a type id the audit has no name for
        for q in ("execute as %s if entity @s[type=minecraft:husk]" % u, "data get entity %s CustomName" % u):
            try:
                rc(a.port, q)
            except OSError:
                pass
    s = b.summary()
    s["odd_types"] = b.odd[:20]
    if a.menus:
        s["menu_fails"] = menu_fails
    b.close()
    print(json.dumps(s, indent=1, ensure_ascii=True)[:6000])
    if a.out:
        with open(a.out, "w", encoding="utf-8") as f:
            json.dump(s, f, indent=1)
    clean = not s["bad_entity_types"] and not s["bad_sounds"] and not s["bad_block_states"] and not s["key_leaks"] and not s["mod_payloads"] and not s["decode_errors"] and not s["item_leaks"] and not s.get("menu_fails")
    print("PLAIN AUDIT " + ("PASS" if ok and clean else "FAIL"), flush=True)
    return 0 if ok and clean else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
