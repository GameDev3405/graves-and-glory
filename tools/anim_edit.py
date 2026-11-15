"""Edit troop clips in Blockbench and bring the result back into the game's clip JSON.

  python tools/anim_edit.py open <family> [--rig R] [--troop T] [--fresh] [--launch]
      Builds work/anim-edit/<family>-<troop>.bbmodel: the troop's real model (skin cubes, textures embedded) with the
      family's animations on the rig bones. Prints the path; --launch hands it to Blockbench. An existing file with
      unapplied edits is kept (--fresh rebuilds it and drops them). A family with no source file yet opens with no
      animations; apply then creates art/clips/<rig>/<family>.bbmodel.
  python tools/anim_edit.py apply [family ...]
      Copies the animations of each saved working file back into art/clips/<rig>/<family>.bbmodel, then runs
      tools/clips.py and tools/clips.py --verify.
  python tools/anim_edit.py preview <family> <clip>
      Prints the harness command that renders the family in the real game (it has no one-clip filter; the clip is
      in the solo sheet). Starts nothing.

Families and their rig and default troop are in FAMILIES; --troop picks another troop of the same rig, --rig overrides
the rig (art/rigs/<rig>.json). bear and ravager have no rig file yet, so they are not listed."""
import base64
import json
import os
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
import clips  # noqa: E402

WORK = ROOT / 'work/anim-edit'
FAMILIES = {  # family: (rig, default troop)
    'sword_shield': ('biped', 'knight'), 'bow': ('biped', 'archer'), 'common': ('biped', 'knight'),
    'caster': ('biped', 'mage'), 'spear': ('biped', 'wm_tusk'), 'sword': ('biped', 'revenant'),
    'crossbow': ('biped', 'ic_arbalist'), 'axe_shield': ('biped', 'ic_hearthguard'), 'axe': ('biped', 'ic_breaker'),
    'greatsword': ('biped', 'gb_hollowking'), 'scythe': ('biped', 'reaper'), 'thrower': ('biped', 'tc_harpooner'),
    'crew': ('biped', 'ic_mortar'),
    'hound': ('hound', 'wm_hound'), 'spider': ('spider', 'wm_stalker'), 'colossus': ('colossus', 'ic_colossus'),
}


def src(family, rig=None):
    return ROOT / 'art/clips' / (rig or FAMILIES[family][0]) / (family + '.bbmodel')


def option(args, name):
    return args[args.index(name) + 1] if name in args and args.index(name) + 1 < len(args) else None


def load(path):
    return json.loads(pathlib.Path(path).read_text(encoding='utf-8'))


def save(project, path):
    path = pathlib.Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(project, indent=1) + '\n', encoding='utf-8')


def by_name(project):
    return {g['name']: g['uuid'] for g in project.get('groups') or []}


def remap(animations, src, dst):
    """The animations with each bone animator re-keyed from src's group uuids to dst's, matched by group name."""
    names = {u: n for n, u in by_name(src).items()}
    to = by_name(dst)
    out = json.loads(json.dumps(animations))
    for a in out:
        fixed = {}
        for uid, an in (a.get('animators') or {}).items():
            if uid == 'effects' or an.get('type') == 'effect':
                fixed[uid] = an
                continue
            name = names.get(uid) or an.get('name')
            if name not in to:
                sys.exit(f"anim_edit: animator {an.get('name')!r} has no group {name!r} in the target project")
            fixed[to[name]] = an
        a['animators'] = fixed
    return out


def mesh(troop):
    """The troop's model project: art/troops/<troop>.bbmodel when it is the geometry source, else built from its json."""
    path = ROOT / 'art/troops' / (troop + '.bbmodel')
    if path.is_file():
        return load(path)
    sys.path.insert(0, str(ROOT / 'tools'))
    import build_skins
    tmp = WORK / (troop + '-mesh.tmp.bbmodel')
    build_skins.troop_project(build_skins.load(ROOT), troop, dest=tmp)
    project = load(tmp)
    tmp.unlink()
    return project


def embed(project):
    """Embeds each texture as a data URL so the copy under work/ does not depend on relative paths."""
    for t in project.get('textures') or []:
        rel = t.pop('relative_path', None)
        if rel and 'source' not in t:
            png = (ROOT / 'art/troops' / rel).resolve().read_bytes()
            t['source'] = 'data:image/png;base64,' + base64.b64encode(png).decode()
    return project


def semantic(project, rig):
    return clips.convert(project, load(ROOT / 'art/rigs' / (rig + '.json')), 'project')


def working(family, troop):
    return WORK / f'{family}-{troop}.bbmodel'


def cmd_open(args):
    fresh, launch = '--fresh' in args, '--launch' in args
    troop, rig = option(args, '--troop'), option(args, '--rig')
    rest = [a for a in args if not a.startswith('--') and a not in (troop, rig)]
    if len(rest) != 1 or rest[0] not in FAMILIES:
        print(__doc__)
        return 2
    family = rest[0]
    rig = rig or FAMILIES[family][0]
    troop = troop or FAMILIES[family][1]
    if not (ROOT / 'art/rigs' / (rig + '.json')).is_file():
        sys.exit(f'anim_edit: no rig art/rigs/{rig}.json')
    src_path = src(family, rig)
    source = load(src_path) if src_path.is_file() else None
    path = working(family, troop)
    if path.is_file() and not fresh:
        try:
            same = source is not None and semantic(load(path), rig) == semantic(source, rig)
        except clips.ClipError:
            same = False
        if not same:
            print(f'anim_edit: {path} holds edits not yet applied; kept (use --fresh to drop them)')
            print(path)
            return launch_it(path, launch)
    project = mesh(troop)
    project['name'] = f'{troop} {family}'
    project['animations'] = remap(source['animations'], source, project) if source else []
    save(embed(project), path)
    print(f'anim_edit: wrote {path.relative_to(ROOT).as_posix()} ({len(project["animations"])} clips)')
    print(path)
    return launch_it(path, launch)


def launch_it(path, launch):
    if launch:
        exe = os.environ.get('BLOCKBENCH', 'Blockbench.exe')
        subprocess.Popen([exe, str(path)])
    return 0


def cmd_apply(args):
    families = args or sorted(FAMILIES)
    done = 0
    for family in families:
        if family not in FAMILIES:
            print(f'anim_edit: unknown family {family!r}')
            return 2
        rig = FAMILIES[family][0]
        for path in sorted(WORK.glob(family + '-*.bbmodel')) if WORK.is_dir() else []:
            src_path = src(family, rig)
            edited = load(path)
            if not src_path.is_file():
                if not edited.get('animations'):
                    continue
                source = dict(edited, textures=[])  # a new family: the rig groups, no embedded skin
                source['animations'] = []
            else:
                source = load(src_path)
            try:
                before, after = semantic(source, rig), semantic(edited, rig)
            except clips.ClipError as e:
                print(f'anim_edit: {path.name}: {e}')
                return 1
            if before == after:
                continue
            source['animations'] = remap(edited['animations'], edited, source)
            save(source, src_path)
            changed = sorted(n for n in after if before.get(n) != after[n])
            print(f'anim_edit: {path.name} -> {src_path.relative_to(ROOT).as_posix()}: changed {", ".join(changed)}')
            done += 1
    if not done:
        print('anim_edit: no working file differs from the committed clips')
    if clips.main([]) != 0:
        return 1
    return clips.main(['--verify'])


def cmd_preview(args):
    if len(args) != 2:
        print(__doc__)
        return 2
    family, clip = args
    troop = FAMILIES[family][1] if family in FAMILIES else family
    print(f"""Renders {family} in the real game through the engine layers (no one-clip filter; find '{clip}' in the solo sheet).
At most 1 client; check first that few Minecraft javas run; stop after 30 minutes. From a git bash in the repo root:

  export JAVA_HOME="C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.11.10-hotspot"
  G=$(ls -d ~/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle)
  export GRAVES_ROADMAP_TEST_ROOT="$PWD/work/grade-run"      # absolute, new and empty
  mkdir -p "$GRAVES_ROADMAP_TEST_ROOT"/{{run,evidence,runtime}}
  printf 'pauseOnLostFocus:false\\nonboardAccessibility:false\\n' > "$GRAVES_ROADMAP_TEST_ROOT/run/options.txt"
  GRAVES_PHASE2_CHECKS=motion-grade GRAVES_MOTION_FAMILY={family} GRAVES_GRADE_TROOP={troop} GRAVES_GRADE_PARTS=solo \\
    "$G" runClient --offline -PcampaignChecks --init-script test-tools/isolated-checks.gradle --args="--gameDir $GRAVES_ROADMAP_TEST_ROOT/run"

Sheets land in test-tools/work/grade/{family}/<timestamp>/ (GRAVES_GRADE_OUT changes it). Run python tools/anim_edit.py apply first:
the game reads the clip JSON, not the working file.""")
    return 0


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    if not argv or argv[0] not in ('open', 'apply', 'preview'):
        print(__doc__)
        return 2
    return {'open': cmd_open, 'apply': cmd_apply, 'preview': cmd_preview}[argv[0]](argv[1:])


if __name__ == '__main__':
    sys.exit(main())
