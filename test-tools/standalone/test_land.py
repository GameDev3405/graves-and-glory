"""tools/land.py and tools/state.py: land one branch on main and regenerate docs/STATE.md, without ever pushing."""
import contextlib, datetime, io, os, pathlib, subprocess, sys, tempfile, unittest
TOOLS = pathlib.Path(__file__).resolve().parents[2]/'tools'
sys.path.insert(0, str(TOOLS))
import land, state

SPEC = 'src/main/java/gg/gravesandglory/Spec.java'
PASS = sys.executable + ' -c "pass"'
FAIL = sys.executable + ' -c "import sys; sys.exit(1)"'

def git(cwd, *args, date=None):
    env = dict(os.environ)
    if date: env.update(GIT_AUTHOR_DATE=date, GIT_COMMITTER_DATE=date)
    return subprocess.run(['git', '-C', str(cwd), *args], check=True, capture_output=True, text=True, env=env).stdout.strip()

def commit(cwd, name, text, msg, date='2026-11-01T12:00:00+00:00'):
    p = pathlib.Path(cwd)/name; p.parent.mkdir(parents=True, exist_ok=True)
    p.write_bytes(text.encode()); git(cwd, 'add', name); git(cwd, 'commit', '-q', '-m', msg, date=date)

class Repo:
    """A main checkout M with a lane worktree W on branch `lane`, and a status folder."""
    def __init__(self, tmp):
        t = pathlib.Path(tmp)
        self.m, self.w, self.status = t/'main', t/'lane', t/'status'
        self.m.mkdir(); self.status.mkdir()
        git(self.m, 'init', '-q', '-b', 'main')
        git(self.m, 'config', 'user.email', 't@t'); git(self.m, 'config', 'user.name', 't')
        git(self.m, 'config', 'core.autocrlf', 'false')
        commit(self.m, '.gitignore', 'work/\n', 'ignore runtime evidence like the real repo')
        commit(self.m, 'a.txt', 'a\n', 'base')
        commit(self.m, SPEC, 'class Spec {}\n', 'spec')
        git(self.m, 'worktree', 'add', '-q', '-b', 'lane', str(self.w))
        commit(self.w, 'lane.txt', 'lane\n', 'lane work', date='2026-11-03T12:00:00+00:00')
        commit(self.m, 'b.txt', 'b\n', 'main moved', date='2026-11-04T12:00:00+00:00')
    def land(self, *extra, verify=PASS):
        return land.main(['lane', '--repo', str(self.m), '--verify-cmd', verify, '--status-dir', str(self.status),
                          '--now', '2026-11-09T12:00:00+00:00', *extra])
    def log(self, ref='main'): return git(self.m, 'log', '--format=%s', ref).splitlines()

REAL_REFRESH = land.refresh_graph

class Land(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(); self.r = Repo(self.tmp.name)
        self.refreshes = []  # fake seam: never runs graphify
        def fake(home, timeout): self.refreshes.append((pathlib.Path(home), timeout)); return None
        land.refresh_graph = fake
    def tearDown(self):
        land.refresh_graph = REAL_REFRESH
        subprocess.run(['git', '-C', str(self.r.m), 'worktree', 'prune'], capture_output=True)
        self.tmp.cleanup()

    def test_happy_path_rebases_verifies_merges_removes_and_writes_state(self):
        self.assertEqual(self.r.land(), 0)
        log = self.r.log()
        self.assertEqual(log[:3], ['docs: regenerate STATE.md after landing lane', 'lane work', 'main moved'])
        self.assertEqual(git(self.r.m, 'rev-list', '--merges', '--count', 'main'), '0')  # linear: the verified tip is what lands
        self.assertFalse(self.r.w.exists())
        self.assertNotIn('lane', git(self.r.m, 'branch', '--list', 'lane'))
        text = (self.r.m/'docs/STATE.md').read_text()
        self.assertIn('lane work', text)  # head subject
        self.assertEqual(git(self.r.m, 'status', '--porcelain'), '')

    def land_output(self, *extra, **kw):
        out = io.StringIO()
        with contextlib.redirect_stdout(out): code = self.r.land(*extra, **kw)
        return code, out.getvalue()

    def test_graph_refreshed_in_main_checkout_after_success(self):
        self.assertEqual(self.r.land(), 0)
        self.assertEqual(self.refreshes, [(self.r.m, land.GRAPH_TIMEOUT)])
        self.assertIn('docs: regenerate STATE.md after landing lane', self.r.log()[0])  # refresh comes after STATE.md

    def test_graph_not_refreshed_when_landing_fails(self):
        for kw in (dict(verify=FAIL),): self.assertEqual(self.r.land(**kw), land.EXIT_VERIFY)
        (self.r.m/'a.txt').write_text('kort edit\n')
        commit(self.r.w, 'a.txt', 'lane a\n', 'lane touches a')
        self.assertEqual(self.r.land(), land.EXIT_MERGE)
        self.assertEqual(self.refreshes, [])

    def test_graph_not_refreshed_on_dry_run(self):
        self.assertEqual(self.r.land('--dry-run'), 0)
        self.assertEqual(self.refreshes, [])

    def test_no_graph_flag_skips_refresh(self):
        self.assertEqual(self.r.land('--no-graph'), 0)
        self.assertEqual(self.refreshes, [])

    def test_no_checkout_of_main_skips_refresh(self):
        git(self.r.m, 'checkout', '-q', '-b', 'other')
        self.assertEqual(self.r.land(), 0)
        self.assertEqual(self.refreshes, [])

    def test_refresh_warning_never_changes_exit_code(self):
        land.refresh_graph = lambda home, timeout: 'graph not refreshed: boom'
        code, out = self.land_output()
        self.assertEqual(code, 0)
        self.assertIn('warning: graph not refreshed: boom', out)

    def test_refresh_exception_never_changes_exit_code(self):
        def boom(home, timeout): raise RuntimeError('kaboom')
        land.refresh_graph = boom
        code, out = self.land_output()
        self.assertEqual(code, 0)
        self.assertIn('warning: graph not refreshed: kaboom', out)

    def test_refresh_failure_missing_script_and_timeout_are_warnings(self):
        home = pathlib.Path(self.tmp.name)/'h'; (home/'tools').mkdir(parents=True)
        self.assertIn('does not exist', REAL_REFRESH(home, 5))
        script = home/'tools/graph_state.py'
        script.write_text('import sys; print("graphify: not found"); sys.exit(3)\n')
        w = REAL_REFRESH(home, 30)
        self.assertIn('exit 3', w); self.assertIn('graphify: not found', w)
        script.write_text('import time; time.sleep(60)\n')
        self.assertIn('timed out after 1s', REAL_REFRESH(home, 1))
        script.write_text('print("Graph refreshed locally; 7 nodes")\n')
        self.assertIsNone(REAL_REFRESH(home, 30))

    def test_landing_runs_main_checkouts_own_graph_script_and_timeout_keeps_exit_0(self):
        land.refresh_graph = REAL_REFRESH
        commit(self.r.w, 'tools/graph_state.py', 'import time; time.sleep(60)\n', 'fake slow graph')
        code, out = self.land_output('--graph-timeout', '1')
        self.assertEqual(code, 0)
        self.assertIn('warning: graph not refreshed: timed out after 1s', out)
        self.assertEqual(git(self.r.m, 'rev-parse', 'HEAD'), git(self.r.m, 'rev-parse', 'main'))

    def test_verify_failure_leaves_main_and_worktree(self):
        before = git(self.r.m, 'rev-parse', 'main')
        self.assertEqual(self.r.land(verify=FAIL), land.EXIT_VERIFY)
        self.assertEqual(git(self.r.m, 'rev-parse', 'main'), before)
        self.assertTrue(self.r.w.exists())

    def test_rebase_conflict_aborts_and_restores_lane(self):
        commit(self.r.w, 'b.txt', 'lane b\n', 'conflicting')
        lane_before = git(self.r.m, 'rev-parse', 'lane'); main_before = git(self.r.m, 'rev-parse', 'main')
        self.assertEqual(self.r.land(), land.EXIT_REBASE)
        self.assertEqual(git(self.r.m, 'rev-parse', 'lane'), lane_before)
        self.assertEqual(git(self.r.m, 'rev-parse', 'main'), main_before)
        self.assertEqual(git(self.r.w, 'status', '--porcelain'), '')

    def test_dirty_lane_is_refused(self):
        (self.r.w/'lane.txt').write_text('edited\n')
        self.assertEqual(self.r.land(), land.EXIT_DIRTY)
        self.assertEqual(git(self.r.m, 'rev-parse', 'lane~1'), git(self.r.m, 'merge-base', 'main', 'lane'))  # not rebased

    def test_line_ending_noise_on_generated_files_is_not_dirt(self):
        (self.r.w/SPEC).write_bytes(b'class Spec {}\r\n')
        self.assertEqual(self.r.land(), 0)

    def test_real_change_to_generated_file_is_dirt(self):
        (self.r.w/SPEC).write_bytes(b'class Spec { int x; }\n')
        self.assertEqual(self.r.land(), land.EXIT_DIRTY)

    def test_unrelated_uncommitted_files_in_main_checkout_survive(self):
        (self.r.m/'a.txt').write_text('kort edit\n'); (self.r.m/'b.txt').unlink()
        self.assertEqual(self.r.land(), 0)
        self.assertEqual((self.r.m/'a.txt').read_text(), 'kort edit\n')
        self.assertFalse((self.r.m/'b.txt').exists())
        self.assertEqual(sorted(l.strip() for l in git(self.r.m, 'status', '--porcelain').splitlines()), ['D b.txt', 'M a.txt'])
        self.assertIn('docs/STATE.md', git(self.r.m, 'show', '--name-only', '--format=', 'main'))
        self.assertEqual(git(self.r.m, 'show', '--name-only', '--format=', 'main'), 'docs/STATE.md')

    def test_overlapping_uncommitted_file_in_main_checkout_blocks_merge(self):
        commit(self.r.w, 'a.txt', 'lane a\n', 'lane touches a')
        (self.r.m/'a.txt').write_text('kort edit\n')
        main_before = git(self.r.m, 'rev-parse', 'main')
        self.assertEqual(self.r.land(), land.EXIT_MERGE)
        self.assertEqual(git(self.r.m, 'rev-parse', 'main'), main_before)
        self.assertEqual((self.r.m/'a.txt').read_text(), 'kort edit\n')
        self.assertTrue(self.r.w.exists())

    def test_dry_run_changes_nothing(self):
        refs = git(self.r.m, 'show-ref')
        self.assertEqual(self.r.land('--dry-run'), 0)
        self.assertEqual(git(self.r.m, 'show-ref'), refs)
        self.assertTrue(self.r.w.exists())

    def test_keep_worktree(self):
        self.assertEqual(self.r.land('--keep-worktree'), 0)
        self.assertTrue(self.r.w.exists())
        self.assertIn('lane work', self.r.log())

    def test_landing_main_itself_is_refused(self):
        self.assertEqual(land.main(['main', '--repo', str(self.r.m), '--verify-cmd', PASS]), land.EXIT_USAGE)

    def test_evidence_in_work_is_kept(self):
        (self.r.w/'work/verify/x').mkdir(parents=True); (self.r.w/'work/verify/x/result.json').write_text('{}')
        self.assertEqual(self.r.land(), 0)
        kept = list((self.r.m/'work/land').glob('*lane/tree/work/verify/x/result.json'))
        self.assertEqual(len(kept), 1)

    def test_ignored_evidence_kept_even_when_work_has_tracked_files(self):
        (self.r.w/'work').mkdir(); (self.r.w/'work/HANDOFF.md').write_text('tracked\n')
        git(self.r.w, 'add', '-f', 'work/HANDOFF.md'); git(self.r.w, 'commit', '-q', '-m', 'tracked file under ignored work/')
        (self.r.w/'work/verify/x').mkdir(parents=True); (self.r.w/'work/verify/x/result.json').write_text('{}')
        (self.r.w/'.gitignore').write_text('work/\nrun/\nbuild/\n'); git(self.r.w, 'commit', '-qam', 'ignore run and build')
        (self.r.w/'run').mkdir(); (self.r.w/'run/latest.log').write_text('log')
        (self.r.w/'build').mkdir(); (self.r.w/'build/x.class').write_text('x')
        self.assertEqual(self.r.land(), 0)
        tree = next((self.r.m/'work/land').glob('*lane'))/'tree'
        self.assertTrue((tree/'work/verify/x/result.json').exists())
        self.assertTrue((tree/'run/latest.log').exists())
        self.assertFalse((tree/'build').exists())
        self.assertFalse((tree/'lane.txt').exists())  # tracked files live in git, not in the archive
        self.assertFalse((tree/'work/HANDOFF.md').exists())

    def test_runs_from_the_lane_checkout_itself(self):
        r = land.main(['lane', '--repo', str(self.r.w), '--verify-cmd', PASS, '--status-dir', str(self.r.status)])
        self.assertIn(r, (0, land.EXIT_CLEANUP))  # Windows cannot move a folder a process sits in; POSIX can
        self.assertIn('lane work', self.r.log())
        if r == land.EXIT_CLEANUP: self.assertTrue((self.r.w/'lane.txt').exists())  # kept whole, not half-deleted
        self.assertTrue((self.r.m/'docs/STATE.md').exists())

    def test_runs_from_an_unrelated_checkout(self):
        other = pathlib.Path(self.tmp.name)/'other'
        git(self.r.m, 'worktree', 'add', '-q', '-b', 'other', str(other))
        r = land.main(['lane', '--repo', str(other), '--verify-cmd', PASS, '--status-dir', str(self.r.status)])
        self.assertEqual(r, 0)
        self.assertFalse(self.r.w.exists())
        self.assertEqual(git(self.r.m, 'branch', '--list', 'lane'), '')

    def test_no_checkout_of_main_moves_ref_only(self):
        git(self.r.m, 'checkout', '-q', '--detach')
        self.assertEqual(self.r.land(), 0)
        self.assertIn('lane work', self.r.log())

    def test_failing_commit_hook_after_merge_reports_cleanup_not_crash(self):
        hooks = pathlib.Path(git(self.r.m, 'rev-parse', '--path-format=absolute', '--git-common-dir'))/'hooks'
        (hooks/'pre-commit').write_text('#!/bin/sh\nexit 1\n')
        self.assertEqual(self.r.land(), land.EXIT_CLEANUP)
        self.assertIn('lane work', self.r.log())  # landed
        self.assertFalse(self.r.w.exists())

    def test_main_moving_during_verify(self):
        mover = f'{sys.executable} -c "import subprocess; subprocess.run([\'git\',\'-C\',r\'{self.r.m}\',\'commit\',\'-q\',\'--allow-empty\',\'-m\',\'race\'])"'
        before_lane = git(self.r.m, 'rev-parse', 'lane')
        self.assertEqual(self.r.land(verify=mover), land.EXIT_MOVED)
        self.assertEqual(self.r.log()[0], 'race')
        self.assertTrue(self.r.w.exists())

    def test_uncommitted_state_md_in_main_checkout_is_not_overwritten(self):
        (self.r.m/'docs').mkdir(); (self.r.m/'docs/STATE.md').write_text('kort notes\n')
        self.assertEqual(self.r.land(), land.EXIT_CLEANUP)
        self.assertEqual((self.r.m/'docs/STATE.md').read_text(), 'kort notes\n')

    def test_concurrent_land_is_refused(self):
        lock = pathlib.Path(git(self.r.m, 'rev-parse', '--path-format=absolute', '--git-common-dir'))/'land.lock'
        lock.write_text('1 other\n')
        self.assertEqual(self.r.land(), land.EXIT_BUSY)
        lock.unlink()

    def test_bad_arguments_are_usage_errors(self):
        with self.assertRaises(SystemExit) as e: land.main(['--no-such-flag'])
        self.assertEqual(e.exception.code, land.EXIT_USAGE)

    @unittest.skipUnless(os.name == 'nt', 'Windows file locking')
    def test_open_file_in_lane_keeps_worktree_whole(self):
        (self.r.w/'work').mkdir(); f = open(self.r.w/'work/held.log', 'w')
        try: self.assertEqual(self.r.land(), land.EXIT_CLEANUP)
        finally: f.close()
        self.assertTrue((self.r.w/'lane.txt').exists())
        self.assertIn('lane work', self.r.log())

    def test_real_line_ending_config(self):
        # Kort's machine: core.autocrlf=true and .gitattributes '*.java text eol=lf'; preflight writes CRLF.
        git(self.r.m, 'config', 'core.autocrlf', 'true')
        commit(self.r.w, '.gitattributes', '* text=auto\n*.java text eol=lf\n', 'attributes')
        (self.r.w/SPEC).write_bytes(b'class Spec {}\r\n')
        self.assertEqual(self.r.land(), 0)
        self.assertIn('attributes', self.r.log())

    def test_primary_checkout_is_never_retired(self):
        # Kort's layout: the primary checkout (holding .git) is on a work branch; main lives in a linked worktree.
        prim, home = pathlib.Path(self.tmp.name)/'prim', pathlib.Path(self.tmp.name)/'home'
        prim.mkdir(); git(prim, 'init', '-q', '-b', 'trunk'); git(prim, 'config', 'user.email', 't@t'); git(prim, 'config', 'user.name', 't')
        commit(prim, 'a.txt', 'a\n', 'base'); git(prim, 'branch', 'main'); git(prim, 'worktree', 'add', '-q', str(home), 'main')
        commit(prim, 'b.txt', 'b\n', 'work on the primary branch')
        r = land.main(['trunk', '--repo', str(home), '--verify-cmd', PASS, '--status-dir', str(self.r.status)])
        self.assertEqual(r, land.EXIT_USAGE)
        self.assertTrue((prim/'.git').is_dir())
        self.assertNotIn('work on the primary branch', git(home, 'log', '--format=%s', 'main'))

    def test_locked_worktree_is_refused(self):
        git(self.r.m, 'worktree', 'lock', str(self.r.w))
        self.assertEqual(self.r.land(), land.EXIT_USAGE)
        self.assertTrue(self.r.w.exists())
        git(self.r.m, 'worktree', 'unlock', str(self.r.w))

    def test_lane_mid_merge_is_refused_and_left_alone(self):
        commit(self.r.w, 'b.txt', 'lane b\n', 'conflicting')
        subprocess.run(['git', '-C', str(self.r.w), 'merge', 'main'], capture_output=True)  # leaves a conflicted merge
        self.assertEqual(self.r.land(), land.EXIT_DIRTY)
        self.assertTrue((pathlib.Path(git(self.r.w, 'rev-parse', '--absolute-git-dir'))/'MERGE_HEAD').exists())

    def test_unknown_main_branch_is_a_usage_error(self):
        self.assertEqual(land.main(['lane', '--repo', str(self.r.m), '--main', 'nope', '--verify-cmd', PASS]), land.EXIT_USAGE)

    def test_dry_run_takes_no_lock(self):
        lock = pathlib.Path(git(self.r.m, 'rev-parse', '--path-format=absolute', '--git-common-dir'))/'land.lock'
        lock.write_text('1 other\n')
        try: self.assertEqual(self.r.land('--dry-run'), 0)
        finally: lock.unlink()

    def test_default_status_dir_is_beside_primary_checkout(self):
        nested = self.r.m/'.claude/worktrees/x'
        git(self.r.m, 'worktree', 'add', '-q', '-b', 'x', str(nested))
        self.assertEqual(state.default_status_dir(nested), self.r.m.parent/'status')

    def test_never_pushes(self):
        src = (TOOLS/'land.py').read_text() + (TOOLS/'state.py').read_text()
        self.assertNotIn("'push'", src); self.assertNotIn('"push"', src)

class State(unittest.TestCase):
    def test_report_lists_head_lanes_leads_and_questions_with_age(self):
        with tempfile.TemporaryDirectory() as tmp:
            r = Repo(tmp)
            (r.m/'docs').mkdir()
            commit(r.m, 'docs/QUESTIONS-FOR-KORT.md', '# Q\n\n| ID | Work | Rec | Blocks |\n|---|---|---|---|\n| VIS-01 | UI | Inspect | Visual |\n',
                   'questions', date='2026-11-02T12:00:00+00:00')
            lead = r.status/'anim-lead.md'
            lead.write_text('state: working on clip batch 3\ndone since last: batch 2\nnext: batch 4\nblocked: none\n'
                            'questions for Kort: which units reuse the knight death?\n')
            t = datetime.datetime(2026, 11, 8, 12, tzinfo=datetime.timezone.utc).timestamp(); os.utime(lead, (t, t))
            now = datetime.datetime(2026, 11, 9, 12, tzinfo=datetime.timezone.utc)
            text = state.render(state.collect(r.m, r.status, now))
            self.assertIn(git(r.m, 'rev-parse', '--short=10', 'main') + ' questions', text)  # head sha and subject
            self.assertRegex(text, r'`lane`.*1 ahead, 2 behind.*6d')     # lane age from its last commit
            self.assertRegex(text, r'anim-lead.*1d.*working on clip batch 3')
            self.assertRegex(text, r'anim-lead.*1d.*which units reuse the knight death')
            self.assertRegex(text, r'VIS-01.*7d')
            self.assertLess(len(text.splitlines()), 60)
            subprocess.run(['git', '-C', str(r.m), 'worktree', 'prune'], capture_output=True)

    def test_status_none_answers_are_not_questions_and_bold_keys_parse(self):
        with tempfile.TemporaryDirectory() as tmp:
            r = Repo(tmp)
            (r.status/'props-lead.md').write_text('- **State:** making props\n- **Questions for Kort:** none yet.\n')
            (r.status/'lore-lead.md').write_text('State: reading\nQuestions for Kort: Nothing.\n')
            text = state.render(state.collect(r.m, r.status, datetime.datetime.now(datetime.timezone.utc)))
            self.assertRegex(text, r'props-lead.*making props')
            self.assertNotRegex(text, r'(props|lore)-lead.*(none|Nothing)')
            subprocess.run(['git', '-C', str(r.m), 'worktree', 'prune'], capture_output=True)

    def test_status_format_variants(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = pathlib.Path(tmp)
            cases = {
                'a': ('## State\nbuilding clips\n## Questions for Kort\n- which units reuse the death?\n', 'building clips', 'which units reuse the death?'),
                'b': ('1. State: idle\n2. Questions for Kort: (none)\n', 'idle', None),
                'c': ('> State: x\n> Questions for Kort: N/A.\n', 'x', None),
                'e': ('State: y\nQuestions for Kort: None, but pick a map\n', 'y', 'None, but pick a map'),
                'f': ('State: z\nQuestions for Kort: none\n- Q1: which map?\n', 'z', 'none Q1: which map?'),
                'd': ('State:\n  waiting on Kort\nQuestions for Kort: pick a map → yes or no\n', 'waiting on Kort', 'pick a map → yes or no'),
            }
            for name, (text, st, q) in cases.items():
                (d/f'{name}.md').write_text(text, encoding='utf8')
                f = state.read_status(d/f'{name}.md')
                self.assertEqual(f['state'], st, name)
                got = f.get('questions for kort')
                self.assertEqual(None if state.is_none(got) else got, q, name)
            (d/'DECISIONS-2026-11-09.md').write_text('# Decisions\n- Models: never Fable.\n')
            self.assertNotIn('state', state.read_status(d/'DECISIONS-2026-11-09.md'))

    def test_question_age_survives_rebase(self):
        with tempfile.TemporaryDirectory() as tmp:
            r = Repo(tmp)
            commit(r.w, 'docs/QUESTIONS-FOR-KORT.md', '| ID | W | R | B |\n|---|---|---|---|\n| Q1 | x | old question | y |\n', 'q',
                   date='2026-10-20T12:00:00+00:00')
            git(r.w, 'rebase', '-q', 'main', date='2026-11-09T12:00:00+00:00')
            rows = state.question_rows(r.m, 'lane')
            self.assertEqual(rows[0]['since'].date(), datetime.date(2026, 10, 20))
            subprocess.run(['git', '-C', str(r.m), 'worktree', 'prune'], capture_output=True)

    def test_cli_prints_non_ascii(self):
        with tempfile.TemporaryDirectory() as tmp:
            r = Repo(tmp)
            (r.status/'x-lead.md').write_text('State: a → b — c\n', encoding='utf8')
            out = subprocess.run([sys.executable, str(TOOLS/'state.py'), '--repo', str(r.m), '--status-dir', str(r.status)],
                                 capture_output=True, env=dict(os.environ, PYTHONIOENCODING='cp1252'))
            self.assertEqual(out.returncode, 0, out.stderr)
            self.assertIn('a → b — c', out.stdout.decode('utf8'))
            subprocess.run(['git', '-C', str(r.m), 'worktree', 'prune'], capture_output=True)

    def test_lane_families_fold_unless_full(self):
        now = datetime.datetime(2026, 11, 9, tzinfo=datetime.timezone.utc)
        lanes = [dict(name=f'anim/v2-{n}', ahead=i+1, behind=3, last=now - datetime.timedelta(days=i)) for i, n in enumerate('abcde')]
        lanes.append(dict(name='feat/oldhold-wiring', ahead=2, behind=9, last=now))
        folded = state.lane_lines(lanes, now)
        self.assertEqual(len(folded), 2)
        self.assertIn('`anim/v2-*` 5 lanes, 1 to 5 ahead', folded[0])
        self.assertEqual(len(state.lane_lines(lanes, now, full=True)), 6)

if __name__ == '__main__': unittest.main()
