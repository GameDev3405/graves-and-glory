"""The commander weapon clips (docs/superpowers/plans/2026-11-03-gear-art.md, Task 10), played on the player model by
CommanderClips: a greatweapon's great_swing, a staff's staff_strike and staff_cast, a banner's banner_strike and banner_raise.

A clip is keys of the right arm and the right hand (the wrist: the weapon turns about the fist), in degrees, on the
mannequin frame (y down, -z forward; an arm pitch of -90 points the arm forward, as on the vanilla biped). Each key names
the way the weapon's tip must face there (`face`, mannequin frame): a wind-up away from the target, the strike toward it,
the follow-through past it. test_clip_faces holds every key within 45 degrees of it (Kort 11-03: the blade always points
the logical way for the move). A two-hander's left arm is found per key by weaponlib.reach, so the left fist closes on its
second grip. A strike clip plays over the vanilla swing; a use clip after the right click; a hold clip while sneaking.

  python clips.py render <dir>    every clip's keys on the mannequin, one strip a clip and weapon (clips-<clip>.png)
  python clips.py export          src/main/resources/assets/gravesandglory/gear_clips.json
"""
import json, math, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "weapons"))
import weaponlib as WL
from PIL import Image, ImageDraw
import render, mannequin, gearlib

UP, DOWN, FORWARD, BACK = (0, -1, 0), (0, 1, 0), (0, 0, -1), (0, 0, 1)
def mix(*vs): s = [sum(v[i] for v in vs) for i in range(3)]; n = math.sqrt(sum(x * x for x in s)); return tuple(x / n for x in s)

def key(t, arm, hand, face, note): return {"t": t, "right_arm": arm, "right_hand": hand, "face": face, "note": note}

# t is a share of the clip (0..1). Rest keys repeat the held pose so the clip starts and ends where the hold does.
CLIPS = {
    "great_swing": {"kind": "once", "length": 0.6, "keys": [   # an overhead two-handed chop
        key(0.0, (-45, -22, 0), (0, 0, 0), mix(FORWARD, UP), "ready"),
        key(0.3, (-165, -10, 0), (30, 0, 0), mix(BACK, UP), "wind-up: raised behind the head"),
        key(0.55, (-80, -15, 0), (145, 0, 0), mix(FORWARD, DOWN, DOWN), "strike: down on the target"),
        key(0.8, (-25, -20, 0), (90, 0, 0), mix(DOWN, DOWN, FORWARD), "follow-through: past it, to the ground"),
        key(1.0, (-45, -22, 0), (0, 0, 0), mix(FORWARD, UP), "ready")]},
    "staff_strike": {"kind": "once", "length": 0.5, "keys": [   # the head brought down on the target
        key(0.0, (0, 0, 0), (0, 0, 0), UP, "rest: upright"),
        key(0.3, (-150, 0, 0), (105, 0, 0), mix(UP, BACK), "wind-up: raised, head back"),
        key(0.55, (-70, 0, 0), (205, 0, 0), mix(FORWARD, DOWN), "strike: the head forward and down"),
        key(0.8, (-30, 0, 0), (165, 0, 0), mix(DOWN, FORWARD), "follow-through: low"),
        key(1.0, (0, 0, 0), (0, 0, 0), UP, "rest: upright")]},
    "staff_cast": {"kind": "once", "length": 0.6, "keys": [   # the head raised toward the friend it heals
        key(0.0, (0, 0, 0), (0, 0, 0), UP, "rest: upright"),
        key(0.35, (-100, 0, 0), (125, 0, 0), mix(FORWARD, UP, UP), "cast: held out, the head high"),
        key(0.7, (-100, 0, 0), (125, 0, 0), mix(FORWARD, UP, UP), "held"),
        key(1.0, (0, 0, 0), (0, 0, 0), UP, "rest: upright")]},
    "banner_strike": {"kind": "once", "length": 0.5, "keys": [   # a thrust of the spear point
        key(0.0, (0, 0, 0), (0, 0, 0), UP, "rest: upright"),
        key(0.3, (-40, 0, 0), (65, 0, 0), mix(FORWARD, UP, UP), "wind-up: drawn back, the point up"),
        key(0.55, (-75, 0, 0), (165, 0, 0), FORWARD, "strike: the point thrust forward"),
        key(0.8, (-60, 0, 0), (195, 0, 0), mix(FORWARD, DOWN), "follow-through: past it, low"),
        key(1.0, (0, 0, 0), (0, 0, 0), UP, "rest: upright")]},
    "banner_raise": {"kind": "hold", "length": 0.5, "keys": [   # the colours lifted high, held while sneaking
        key(0.0, (0, 0, 0), (0, 0, 0), UP, "rest: upright"),
        key(1.0, (-170, 0, 0), (170, 0, 0), UP, "raised: high over the head")]},
}
# the clips each weapon plays, by trigger
ITEMS = {
    "greatsword": {"strike": "great_swing"}, "great_axe": {"strike": "great_swing"},
    "mending_staff": {"strike": "staff_strike", "use": "staff_cast"}, "sunwell_crook": {"strike": "staff_strike", "use": "staff_cast"},
    "war_banner": {"strike": "banner_strike", "sneak": "banner_raise"}, "long_colours": {"strike": "banner_strike", "sneak": "banner_raise"},
}

def weapons(): return {w.item_id: w for w in WL.items() if w.item_id in ITEMS}

def pose_of(w, k):
    """The bone angles at a key; a two-hander's left arm reaches for its second grip."""
    pose = {"right_arm": tuple(k["right_arm"]), "right_hand": tuple(k["right_hand"])}
    if w.hands == 2: pose["left_arm"] = WL.reach(w, pose)
    return pose

def tip(w, pose):
    """The weapon's tip direction (grip to tip, unit) on the mannequin frame."""
    mats, _ = WL.frames(mannequin.bones(), pose)
    parent = WL.HOLDS[w.hold][0]
    a, b = WL.hold_point(w.hold, (0, 0, 0)), WL.hold_point(w.hold, (0, 1, 0))
    v = render.app(mats[parent], [b[i] - a[i] for i in range(3)])
    n = math.sqrt(sum(x * x for x in v)); return [x / n for x in v]

def angle(a, b): return math.degrees(math.acos(max(-1, min(1, sum(a[i] * b[i] for i in range(3))))))

def picture(w, pose, yaw=-70, pitch=8, px=4):
    bones = mannequin.bones()
    for bn in bones:
        if bn.name in pose: bn.rot = list(pose[bn.name])
    mats, _ = WL.frames(bones, {})
    sheets = {"skin": mannequin.skin(), "cloth": mannequin.skin()}
    return render.render(bones, lambda b: True, sheets, yaw, pitch, px=px, extra=WL.weapon_quads(w, 5, "crimson", mats, WL.HOLDS[w.hold][0]))

def render_all(out):
    os.makedirs(out, exist_ok=True); ws = weapons()
    for name, clip in CLIPS.items():
        rows = []
        for item in [i for i, t in ITEMS.items() if name in t.values()]:
            w = ws[item]; tiles = []
            for k in clip["keys"]:
                img = picture(w, pose_of(w, k)); t = tip(w, pose_of(w, k))
                blk = Image.new("RGBA", (img.width, img.height + 30), gearlib.BG); blk.paste(img, (0, 0))
                d = ImageDraw.Draw(blk)
                d.text((2, img.height + 2), "t %.2f  %s" % (k["t"], k["note"].split(":")[0]), fill=(220, 214, 200, 255))
                d.text((2, img.height + 15), "off %.0f deg" % angle(t, k["face"]), fill=(180, 176, 166, 255))
                tiles.append(blk)
            rows.append(gearlib.label(render.sheet_of(tiles), "%s / %s" % (name, item), gearlib.BORDERS[5]))
        img = gearlib.stack(rows); path = os.path.join(out, "clips-%s.png" % name); img.save(path); print("clip sheet", path, img.size)

def exported():
    ws = weapons(); out = {"format": 1, "items": {}}
    for item, trig in ITEMS.items():
        w = ws[item]; row = {}
        for t, name in trig.items():
            c = CLIPS[name]; keys = []
            for k in c["keys"]:
                p = pose_of(w, k)
                keys.append({"t": k["t"], **{b: [round(x, 2) for x in v] for b, v in p.items()}})
            row[t] = {"clip": name, "kind": c["kind"], "length": c["length"], "keys": keys}
        out["items"][item] = row
    return out

def export():
    path = os.path.join(HERE, "..", "..", "..", "src", "main", "resources", "assets", "gravesandglory", "gear_clips.json")
    with open(path, "w", newline="\n") as f: json.dump(exported(), f, indent=1); f.write("\n")
    print("exported", os.path.normpath(path))

if __name__ == "__main__":
    if sys.argv[1:2] == ["render"]: render_all(sys.argv[2])
    elif sys.argv[1:2] == ["export"]: export()
    else: print(__doc__)
