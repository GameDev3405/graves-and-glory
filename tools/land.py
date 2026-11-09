"""Land one finished branch on main, locally. Never pushes, tags a release or publishes.

    python tools/land.py <branch> [--dry-run] [--keep-worktree] [--verify-cmd "..."]

Run it from any checkout of the repository. Steps 1 to 4 stop at the first failure with main untouched:
  1. find the branch's worktree; refuse if it has uncommitted changes (CR-only noise on the generated
     Spec.java / preflight.json is restored, not counted)
  2. rebase the branch on main in its own worktree (a failed rebase is aborted: branch unchanged)
  3. run verify there (default `python tools/verify.py fast`)
  4. fast-forward main to the verified tip in the checkout that has main checked out. Git refuses if
     that would overwrite uncommitted files there (Kort's edits, including CR-only edits to Spec.java),
     and land stops with exit 5
After main has moved, cleanup problems never undo the landing; they are reported with exit 6:
  5. move the whole lane folder to <main checkout>/work/land/<stamp>-<branch>/tree (a folder that is in
     use cannot be moved, so nothing is half-deleted), unregister the worktree, delete the tracked files,
     build/ and .gradle/ from the moved copy and keep every other ignored file (work/, run*/, evidence)
     there; delete the branch only if it still points at the verified tip. Skipped with --keep-worktree
  6. regenerate docs/STATE.md (tools/state.py) and commit only that file on main; skipped with a warning
     if docs/STATE.md has uncommitted edits there

Exit codes: 0 landed, 1 dirty lane, 2 rebase failed, 3 verify failed, 4 main or the lane moved during
verify, 5 merge refused, 6 landed but cleanup incomplete (message says what is left), 7 another land is
running, 64 usage. Run it only for a branch Kort (or the coordinator on his behalf) approved for main.
"""
import argparse, datetime, json, os, pathlib, re, shlex, shutil, stat, subprocess, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'tools'))
import state

GENERATED = ['src/main/java/gg/gravesandglory/Spec.java', 'sheets/preflight.json']
DISPOSABLE = ['build', '.gradle']  # rebuilt by Gradle; not evidence
EXIT_DIRTY, EXIT_REBASE, EXIT_VERIFY, EXIT_MOVED, EXIT_MERGE, EXIT_CLEANUP, EXIT_BUSY, EXIT_USAGE = 1, 2, 3, 4, 5, 6, 7, 64

class Stop(Exception):
    def __init__(self, code, msg): super().__init__(msg); self.code = code

def run(cwd, *args, check=True):
    r = subprocess.run(['git', '-C', str(cwd), *args], capture_output=True, text=True, encoding='utf8', errors='replace')
    if check and r.returncode: raise subprocess.CalledProcessError(r.returncode, r.args, r.stdout, r.stderr)
    return r

def git(cwd, *args): return run(cwd, *args).stdout.strip()

def say(msg): print('land: ' + msg, flush=True)

def clear_generated_noise(wt):
    """Restore generated files whose only difference is CR at end of line. Returns the restored paths."""
    restored = []
    for name in GENERATED:
        if not (pathlib.Path(wt)/name).exists(): continue
        # With eol=lf attributes git can list a CRLF copy as modified while `git diff` is empty, so ask status.
        if not run(wt, 'status', '--porcelain', '--', name, check=False).stdout.strip(): continue
        if run(wt, 'diff', '--quiet', '--ignore-cr-at-eol', '--', name, check=False).returncode == 0:
            git(wt, 'checkout', '--', name); restored.append(name)
    return restored

def dirt(wt): return [l for l in git(wt, 'status', '--porcelain', '-uall').splitlines() if l]

def rm_tree(path):
    def writable(fn, p, _): os.chmod(p, stat.S_IWRITE); fn(p)
    shutil.rmtree(path, onerror=writable)

def retire_worktree(ops, wt, archive, branch, tip):
    """Move the lane out of the way in one rename, then strip what git or Gradle can rebuild. Returns problems."""
    problems = []
    tracked = git(wt, 'ls-files', '-z').split('\0')
    admin = pathlib.Path(git(wt, 'rev-parse', '--absolute-git-dir'))  # <common>/worktrees/<name>
    tree = archive/'tree'
    try: os.rename(wt, tree)
    except OSError as e:
        return [f'worktree kept: could not move {wt} ({e.strerror}); something has a file open there. Close it and remove the worktree by hand']
    if admin.parent.name == 'worktrees': rm_tree(admin)
    run(ops, 'worktree', 'prune', check=False)  # drops only registrations whose folder is gone
    for name in filter(None, tracked):
        try: (tree/name).unlink()
        except FileNotFoundError: pass
        except OSError as e: problems.append(f'could not delete {tree/name}: {e.strerror}')
    for name in ['.git', *DISPOSABLE]:
        p = tree/name
        try: rm_tree(p) if p.is_dir() else p.unlink(missing_ok=True)
        except OSError as e: problems.append(f'could not delete {p}: {e.strerror}')
    for d in sorted((p for p in tree.rglob('*') if p.is_dir()), key=lambda p: -len(p.parts)):
        try: d.rmdir()
        except OSError: pass  # not empty: holds kept evidence
    if tree.exists() and not any(tree.iterdir()): tree.rmdir()
    if run(ops, 'update-ref', '-d', f'refs/heads/{branch}', tip, check=False).returncode:
        problems.append(f'branch {branch} kept: it no longer points at the landed tip {tip[:10]}')
    return problems

def write_state(home, a):
    if run(home, 'status', '--porcelain', '--', 'docs/STATE.md', check=False).stdout.strip():
        return [f'docs/STATE.md has uncommitted edits in {home}; not regenerated']
    out = pathlib.Path(home)/'docs/STATE.md'
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(state.render(state.collect(home, a.status_dir, a.now, a.main)), encoding='utf8', newline='\n')
    git(home, 'add', '--', 'docs/STATE.md')
    if run(home, 'diff', '--cached', '--quiet', '--', 'docs/STATE.md', check=False).returncode:
        r = run(home, 'commit', '-q', '-m', f'docs: regenerate STATE.md after landing {a.branch}', '--', 'docs/STATE.md', check=False)
        if r.returncode: return [f'STATE.md written but not committed: {(r.stderr or r.stdout).strip()}']
    say(f'wrote {out}')
    return []

def land(a):
    repo = pathlib.Path(a.repo)
    trees = state.worktrees(repo)
    lane = next((w for w in trees if w['branch'] == a.branch), None)
    home = next((w for w in trees if w['branch'] == a.main), None)  # the checkout with main checked out, if any
    ops = home['path'] if home else git(repo, 'rev-parse', '--path-format=absolute', '--git-common-dir')  # never the lane itself
    if a.branch == a.main: raise Stop(EXIT_USAGE, f'cannot land {a.main} on itself')
    if not lane: raise Stop(EXIT_USAGE, f'branch {a.branch!r} has no worktree; land rebases and verifies in the branch\'s own worktree')
    wt = pathlib.Path(lane['path']); base = git(ops, 'rev-parse', a.main)
    ahead = int(git(ops, 'rev-list', '--count', f'{a.main}..{a.branch}'))
    if not ahead: raise Stop(EXIT_USAGE, f'{a.branch} has nothing that is not already on {a.main}')

    if a.dry_run:
        say(f'dry run: would rebase {a.branch} ({ahead} commits) in {wt} onto {a.main} {base[:10]}, run {a.verify_cmd!r},'
            f' fast-forward {a.main}' + (f' in {home["path"]}' if home else ' (ref only; no checkout has it)') +
            ('' if a.keep_worktree else f', move {wt} to work/land and delete {a.branch}') +
            (', commit docs/STATE.md' if home else ', no STATE.md (no checkout of main)'))
        return 0

    for p in clear_generated_noise(wt): say(f'restored line-ending noise in {p}')
    if dirty := dirt(wt): raise Stop(EXIT_DIRTY, f'{wt} has uncommitted changes; commit or discard them first:\n  ' + '\n  '.join(dirty[:20]))

    say(f'rebasing {a.branch} onto {a.main} {base[:10]}')
    r = run(wt, 'rebase', base, check=False)
    if r.returncode:
        conflicts = git(wt, 'diff', '--name-only', '--diff-filter=U')
        if any((wt/git(wt, 'rev-parse', '--git-path', d)).exists() for d in ('rebase-merge', 'rebase-apply')):
            run(wt, 'rebase', '--abort', check=False)  # only abort a rebase this run started
        detail = ('conflicting files:\n  ' + conflicts.replace('\n', '\n  ')) if conflicts else (r.stderr or r.stdout).strip()
        raise Stop(EXIT_REBASE, f'rebase failed and was aborted; {a.branch} is unchanged. {detail}')
    tip = git(wt, 'rev-parse', 'HEAD')

    say(f'verifying {tip[:10]}: {a.verify_cmd}')
    code = subprocess.run(a.verify_cmd if os.name == 'nt' else shlex.split(a.verify_cmd), cwd=wt).returncode
    clear_generated_noise(wt)
    if code: raise Stop(EXIT_VERIFY, f'verify failed (exit {code}); {a.main} unchanged. {a.branch} is now rebased on {a.main}.')
    if dirty := dirt(wt): raise Stop(EXIT_VERIFY, 'verify left changes in the worktree (generated output differs from what is committed?):\n  ' + '\n  '.join(dirty[:20]))
    if git(wt, 'rev-parse', 'HEAD') != tip: raise Stop(EXIT_MOVED, f'{a.branch} moved during verify; rerun land')
    if git(ops, 'rev-parse', a.main) != base: raise Stop(EXIT_MOVED, f'{a.main} moved during verify; rerun land')

    if home:
        r = run(home['path'], 'merge', '--ff-only', tip, check=False)
        if r.returncode: raise Stop(EXIT_MERGE, f'fast-forward refused in {home["path"]}; {a.main} unchanged:\n' + (r.stderr or r.stdout).strip())
    else:
        r = run(ops, 'update-ref', f'refs/heads/{a.main}', tip, base, check=False)
        if r.returncode: raise Stop(EXIT_MERGE, f'{a.main} changed under us; unchanged by land: {r.stderr.strip()}')
    say(f'{a.main} is now {tip[:10]}')

    # From here main has moved: report problems, never raise.
    problems = []
    try:
        archive = pathlib.Path(home['path'] if home else ops)/'work/land'/(a.now.strftime('%Y%m%d-%H%M%S') + '-' + re.sub(r'[^\w.-]+', '-', a.branch))
        archive.mkdir(parents=True, exist_ok=True)
        (archive/'land.json').write_text(json.dumps(dict(branch=a.branch, base=base, tip=tip, worktree=str(wt), verify=a.verify_cmd,
                                                         landed=a.now.isoformat()), indent=2), encoding='utf8')
        if not a.keep_worktree:
            problems += retire_worktree(ops, wt, archive, a.branch, tip)
            say(f'kept evidence under {archive}')
    except Exception as e: problems.append(f'cleanup failed: {e}')
    if home:
        try: problems += write_state(home['path'], a)
        except Exception as e: problems.append(f'STATE.md not regenerated: {e}')
    for p in problems: say('warning: ' + p)
    return EXIT_CLEANUP if problems else 0

class Parser(argparse.ArgumentParser):
    def error(self, message): self.print_usage(sys.stderr); print('land: ' + message, file=sys.stderr); raise SystemExit(EXIT_USAGE)

def main(argv=None):
    p = Parser(description='Land one branch on main locally (never pushes).')
    p.add_argument('branch'); p.add_argument('--main', default='main')
    p.add_argument('--repo', default=str(ROOT), help='any checkout of the repository')
    p.add_argument('--verify-cmd', default=f'"{sys.executable}" tools/verify.py fast')
    p.add_argument('--status-dir', default=str(state.STATUS_DIR))
    p.add_argument('--keep-worktree', action='store_true'); p.add_argument('--dry-run', action='store_true')
    p.add_argument('--now', help=argparse.SUPPRESS)
    a = p.parse_args(argv)
    a.now = datetime.datetime.fromisoformat(a.now) if a.now else datetime.datetime.now(datetime.timezone.utc)
    common = pathlib.Path(git(a.repo, 'rev-parse', '--path-format=absolute', '--git-common-dir'))
    lock = common/'land.lock'
    try: fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
    except FileExistsError:
        print(f'land: another land is running ({lock} exists; delete it only if no land is running)', file=sys.stderr); return EXIT_BUSY
    try:
        os.write(fd, f'{os.getpid()} {a.branch}\n'.encode()); os.close(fd)
        return land(a)
    except Stop as e:
        print('land: ' + str(e), file=sys.stderr); return e.code
    finally: lock.unlink(missing_ok=True)

if __name__ == '__main__':
    if hasattr(sys.stdout, 'reconfigure'): sys.stdout.reconfigure(encoding='utf8', errors='replace')
    raise SystemExit(main())
