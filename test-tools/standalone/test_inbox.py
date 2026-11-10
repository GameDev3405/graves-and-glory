"""tools/inbox.py: Kort's inbox, one page of everything that waits on Kort, in priority order."""
import contextlib, datetime, io, json, pathlib, subprocess, sys, tempfile, unittest
TOOLS = pathlib.Path(__file__).resolve().parents[2]/'tools'
sys.path.insert(0, str(TOOLS))
import inbox

NOW = datetime.datetime(2026, 11, 10, 9, 30)

def git(cwd, *args):
    return subprocess.run(['git', '-C', str(cwd), *args], check=True, capture_output=True, text=True).stdout.strip()

def booth(worktree, batch, clips=3, verdict=False, troop='sentinel', raw=None):
    """A booth batch under <worktree>/work/booth/<batch>/ with `clips` clip files in db/clips."""
    d = pathlib.Path(worktree)/'work'/'booth'/batch
    (d/'db'/'clips').mkdir(parents=True)
    (d/'db'/'batch.json').write_text(raw if raw is not None else json.dumps(dict(batch=batch, troop=troop, clip_order=[f'c{i}' for i in range(clips)])), encoding='utf8')
    for i in range(clips): (d/'db'/'clips'/f'c{i}.json').write_text('{}')
    if verdict: (d/'verdict.json').write_text('{}')
    return d

def item(**kw):
    base = dict(kind='question', lead='anim-lead', asker='Animation lead', ask='x', open=None, min=5, prio=2, old_style=False, batch=None)
    base.update(kw); return base

class Parse(unittest.TestCase):
    def one(self, line):
        got = inbox.parse_kort_lines(line, 'anim-lead')
        self.assertEqual(len(got), 1, line); return got[0]

    def test_huge_minutes_fall_back_and_bold_stays_in_the_ask(self):
        i = self.one('- **kort:** Pick **one** | min: ' + '9' * 5000)
        self.assertEqual((i['ask'], i['min']), ('Pick **one**', 5))
        self.assertEqual(self.one('- **Kort**: x | prio: 1')['prio'], 1)

    def test_full_line(self):
        i = self.one('- kort: Pick a banner colour | open: https://claude.ai/artifact/abc | min: 10 | prio: 1')
        self.assertEqual((i['ask'], i['open'], i['min'], i['prio'], i['lead'], i['old_style']), ('Pick a banner colour', 'https://claude.ai/artifact/abc', 10, 1, 'anim-lead', False))
        self.assertEqual(i['kind'], 'question'); self.assertEqual(i['asker'], 'Animation lead')

    def test_case_bold_spaces_and_bullets(self):
        for line in ('- **kort:** Ask one | open: none | min: 3 | prio: 3', '* Kort:   Ask one   |  open:none |min:3|   prio : 3',
                     '  - KORT: Ask one | open: none | min: 3 | prio: 3', 'kort: Ask one | open: none | min: 3 | prio: 3'):
            i = self.one(line)
            self.assertEqual((i['ask'], i['open'], i['min'], i['prio']), ('Ask one', None, 3, 3), line)

    def test_missing_fields_default(self):
        i = self.one('- kort: Only an ask')
        self.assertEqual((i['open'], i['min'], i['prio']), (None, 5, 2))
        i = self.one('- kort: Ask | min: soon | prio: 9')
        self.assertEqual((i['min'], i['prio']), (5, 2))
        i = self.one('- kort: Ask | min: 0 | prio: 1')
        self.assertEqual(i['min'], 5)  # unknown or zero minutes -> 5

    def test_open_can_be_a_path_and_ask_can_hold_colons(self):
        i = self.one('- kort: Is this OK: yes or no? | open: C:/work/page.html | min: 2 | prio: 2')
        self.assertEqual((i['ask'], i['open']), ('Is this OK: yes or no?', 'C:/work/page.html'))

    def test_many_lines_anywhere_and_non_items_ignored(self):
        text = 'state: busy\n\nnotes `- kort: <ask> | open: ...`\n- kort: One | prio: 1\nrandom\n  - kort: Two | prio: 3\n- kort: \n'
        self.assertEqual([i['ask'] for i in inbox.parse_kort_lines(text, 'x-lead')], ['One', 'Two'])

class Collect(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(); t = pathlib.Path(self.tmp.name)
        self.repo, self.status = t/'main', t/'status'
        self.repo.mkdir(); self.status.mkdir()
        git(self.repo, 'init', '-q', '-b', 'main'); git(self.repo, 'config', 'user.email', 't@t'); git(self.repo, 'config', 'user.name', 't')
        (self.repo/'a.txt').write_text('a'); git(self.repo, 'add', 'a.txt'); git(self.repo, 'commit', '-q', '-m', 'base')
        self.lane = t/'lane'; git(self.repo, 'worktree', 'add', '-q', '-b', 'lane', str(self.lane))
    def tearDown(self):
        subprocess.run(['git', '-C', str(self.repo), 'worktree', 'prune'], capture_output=True); self.tmp.cleanup()
    def lead(self, name, text): (self.status/f'{name}.md').write_text(text, encoding='utf8')
    def collect(self): return inbox.collect(self.repo, self.status)

    def test_new_format_from_status_files_and_lead_names(self):
        self.lead('anim-lead', 'state: x\n- kort: Ask A | prio: 1\n- kort: Ask B | min: 7\n')
        self.lead('audio-lead', '- kort: Ask C\n')
        got = {i['ask']: i for i in self.collect()}
        self.assertEqual(set(got), {'Ask A', 'Ask B', 'Ask C'})
        self.assertEqual(got['Ask A']['asker'], 'Animation lead'); self.assertEqual(got['Ask C']['asker'], 'Audio lead')

    def test_decisions_files_are_skipped(self):
        (self.status/'DECISIONS-2026-11-09.md').write_text('- kort: Not an item | prio: 1\nquestions for Kort: also not\n')
        self.assertEqual(self.collect(), [])

    def test_fallback_old_style_note_and_artifact_link(self):
        self.lead('props-lead', 'State: busy\n- **Questions for Kort:** OK for Siege? See https://claude.ai/artifact/XyZ_1-2). Also merge?\n')
        [i] = self.collect()
        self.assertTrue(i['old_style']); self.assertEqual((i['prio'], i['min']), (2, 5))
        self.assertEqual(i['open'], 'https://claude.ai/artifact/XyZ_1-2'); self.assertIn('OK for Siege?', i['ask'])

    def test_fallback_not_used_when_file_has_kort_lines(self):
        self.lead('props-lead', 'questions for Kort: old text\n- kort: new text | prio: 3\n')
        self.assertEqual([i['ask'] for i in self.collect()], ['new text'])

    def test_none_answers_ignored(self):
        for n, v in enumerate(('none', 'None yet', 'nothing', 'n/a', '')): self.lead(f'l{n}-lead', f'state: x\nquestions for Kort: {v}\n')
        self.assertEqual(self.collect(), [])

    def test_booth_waiting_vs_verdict(self):
        booth(self.lane, 'sentinel-1', clips=4)
        booth(self.lane, 'knight-1', clips=2, verdict=True, troop='knight')
        [i] = self.collect()
        self.assertEqual((i['kind'], i['prio'], i['min'], i['lead'], i['asker'], i['batch']), ('booth', 1, 2, 'anim-lead', 'Animation lead', 'sentinel-1'))
        self.assertEqual(i['open'], inbox.BOOTH_URL)
        self.assertEqual(i['ask'], "Watch Sentinel's animations (4 clips) and approve or reject each")

    def test_booth_minutes_round_up_and_main_checkout_counts(self):
        booth(self.repo, 'b-main', clips=5, troop='knight')
        [i] = self.collect()
        self.assertEqual(i['min'], 3)

    def test_booth_broken_or_incomplete_batches_skipped(self):
        booth(self.lane, 'bad-json', raw='{not json')
        booth(self.lane, 'not-a-dict', raw='[1,2]')
        booth(self.lane, 'no-clips', clips=0)
        (pathlib.Path(self.lane)/'work'/'booth'/'no-db').mkdir(parents=True)
        booth(self.lane, 'good', clips=1)
        self.assertEqual([i['batch'] for i in self.collect()], ['good'])

    def test_batch_already_in_its_target_is_not_waiting(self):
        head = git(self.repo, 'rev-parse', 'HEAD')
        old = booth(self.lane, 'old-1', clips=2)
        data = json.loads((old/'db'/'batch.json').read_text()); data.update(submitted_sha=head, target='main')
        (old/'db'/'batch.json').write_text(json.dumps(data))
        new = booth(self.lane, 'new-1', clips=2)
        data = json.loads((new/'db'/'batch.json').read_text()); data.update(submitted_sha='0'*40, target='main')
        (new/'db'/'batch.json').write_text(json.dumps(data))
        self.assertEqual([i['batch'] for i in self.collect()], ['new-1'])

    def test_faction_prefix_dropped_from_troop_name(self):
        booth(self.lane, 'ic-1', clips=1, troop='ic_hearthguard')
        self.assertEqual(self.collect()[0]['ask'], "Watch Hearthguard's animations (1 clip) and approve or reject each")

    def test_git_trouble_shows_the_batch(self):
        head = git(self.repo, 'rev-parse', 'HEAD')
        for target in ('--output=x', 'no-such-branch'):
            self.assertFalse(inbox.already_in_target(self.repo, dict(submitted_sha=head, target=target)), target)
        real = inbox.subprocess.run
        def no_git(*a, **k): raise FileNotFoundError('git')
        inbox.subprocess.run = no_git
        try: self.assertFalse(inbox.already_in_target(self.repo, dict(submitted_sha=head, target='main')))
        finally: inbox.subprocess.run = real

    def test_same_batch_in_two_worktrees_listed_once(self):
        booth(self.repo, 'dup-1', clips=1); booth(self.lane, 'dup-1', clips=1)
        self.assertEqual([i['batch'] for i in self.collect()], ['dup-1'])

    def test_binary_status_file_is_fine(self):
        (self.status/'odd-lead.md').write_bytes(b'\xff\xfe\x00kort\x00: \x80\x81\n')
        self.assertIsInstance(self.collect(), list)

    def test_missing_status_dir_is_fine(self):
        self.assertEqual(inbox.collect(self.repo, self.status/'nope'), [])

class Order(unittest.TestCase):
    def test_order_prio_then_booth_then_minutes_then_asker(self):
        items = [item(ask='q3', prio=3, min=1), item(ask='q2', prio=2, min=1), item(ask='q1b', prio=1, min=2, asker='B'),
                 item(ask='q1a', prio=1, min=2, asker='A'), item(ask='booth', kind='booth', prio=1, min=30), item(ask='q1short', prio=1, min=1)]
        self.assertEqual([i['ask'] for i in inbox.order(items)], ['booth', 'q1short', 'q1a', 'q1b', 'q2', 'q3'])

    def test_today_is_longest_fitting_prefix_and_at_least_one(self):
        items = [item(ask=str(n), prio=1, min=m) for n, m in enumerate((20, 20, 30, 5))]
        today, later = inbox.split_today(inbox.order(items), 60)  # order: 5, 20, 20, 30
        self.assertEqual([i['ask'] for i in today], ['3', '0', '1']); self.assertEqual([i['ask'] for i in later], ['2'])
        today, later = inbox.split_today(inbox.order([item(ask='big', min=90), item(ask='small', min=1, prio=3)]), 60)
        self.assertEqual([i['ask'] for i in today], ['big']); self.assertEqual(len(later), 1)
        self.assertEqual(inbox.split_today([], 60), ([], []))

    def test_prefix_stops_at_first_misfit(self):
        items = inbox.order([item(ask='a', prio=1, min=40), item(ask='b', prio=2, min=40), item(ask='c', prio=3, min=1)])
        today, later = inbox.split_today(items, 60)
        self.assertEqual([i['ask'] for i in today], ['a']); self.assertEqual([i['ask'] for i in later], ['b', 'c'])

    def test_summary_line(self):
        items = inbox.order([item(min=30, prio=1), item(min=30, prio=1), item(min=30, prio=1)])
        self.assertEqual(inbox.summary(items, *inbox.split_today(items, 60)), '3 items, 90 min; today: 2 items, 60 min')

class Html(unittest.TestCase):
    def page(self, items, budget=60):
        items = inbox.order(items); return inbox.render_html(items, *inbox.split_today(items, budget), now=NOW)

    def test_contract(self):
        h = self.page([item(ask='Pick one')])
        self.assertIn("<title>Kort's inbox</title>", h)
        self.assertIn(':root{', h.replace(' ', '')); self.assertIn('prefers-color-scheme: dark', h)
        self.assertIn(':root:not([data-theme="light"])', h); self.assertIn(':root[data-theme="dark"]', h)
        self.assertIn('name="claude:viewport-fit-sides"', h); self.assertNotIn('<body', h); self.assertNotIn('<!doctype', h.lower())
        self.assertIn('padding-left:max(16px, env(safe-area-inset-left', h); self.assertIn('padding-right:max(16px, env(safe-area-inset-right', h)
        self.assertIn('body{background:var(--bg)', h.replace(' ', '').replace('body{margin:0}', ''))
        self.assertNotIn('<script', h); self.assertNotRegex(h, r'src="http')
        self.assertRegex(h, r'Updated \w+ \d\d \w+, 09:30\.'); self.assertIn('About 5 minutes today.', h)
        self.assertIn('Today', h); self.assertIn('Pick one', h); self.assertIn('Animation lead', h); self.assertIn('~5 min', h)

    def test_priority_words_and_later_section(self):
        h = self.page([item(ask='aaa', prio=1, min=50), item(ask='bbb', prio=2, min=50), item(ask='ccc', prio=3, min=1)])
        for w in ('Now', 'This week', 'Whenever', 'Later'): self.assertIn(w, h)
        self.assertLess(h.index('aaa'), h.index('>Later<')); self.assertGreater(h.index('bbb'), h.index('>Later<'))

    def test_escaping(self):
        h = self.page([item(ask='<script>alert(1)</script> & "q"', asker='<b>x</b>', open='C:/a<b>.html', batch='<i>')])
        self.assertNotIn('<script>alert', h); self.assertIn('&lt;script&gt;alert(1)&lt;/script&gt; &amp; &quot;q&quot;', h)
        self.assertNotIn('<b>x</b>', h); self.assertNotIn('<i>', h); self.assertNotIn('a<b>', h)

    def test_only_http_urls_become_links(self):
        h = self.page([item(ask='ok', open='https://claude.ai/artifact/x')])
        self.assertIn('href="https://claude.ai/artifact/x"', h); self.assertIn('>Open<', h)
        for bad in ('javascript:alert(1)', 'file:///C:/x.html', 'C:/work/p.html', 'http://x" onclick="y'):
            h = self.page([item(ask='bad', open=bad)])
            self.assertNotIn('href="javascript', h); self.assertNotIn('href="file', h); self.assertNotIn('href="C:', h); self.assertNotIn('onclick="y', h)
            self.assertNotIn('>Open<', h)
        self.assertIn('C:/work/p.html', self.page([item(open='C:/work/p.html')]))

    def test_small_notes_for_old_style_and_batch(self):
        h = self.page([item(old_style=True), item(kind='booth', batch='sentinel-1', prio=1)])
        self.assertIn('old-style note', h); self.assertIn('sentinel-1', h)

    def test_empty_state(self):
        h = self.page([]); self.assertIn('Nothing waits on you.', h); self.assertIn('About 0 minutes today.', h)

class Cli(unittest.TestCase):
    def setUp(self): Collect.setUp(self)
    def tearDown(self): Collect.tearDown(self)
    def lead(self, n, t): Collect.lead(self, n, t)
    def run_cli(self, *args):
        out = io.StringIO()
        with contextlib.redirect_stdout(out): code = inbox.main(['--repo', str(self.repo), '--status-dir', str(self.status), *args])
        return code, out.getvalue()

    def test_json(self):
        self.lead('anim-lead', '- kort: A | prio: 1 | min: 10\n- kort: B | prio: 3 | min: 55\n')
        code, out = self.run_cli('--json', '--budget', '30')
        self.assertEqual(code, 0); data = json.loads(out)
        self.assertEqual([(i['ask'], i['today']) for i in data], [('A', True), ('B', False)])
        self.assertFalse((self.repo/'work'/'inbox'/'index.html').exists())

    def test_default_out_is_work_inbox_index_of_repo_and_summary_printed(self):
        self.lead('anim-lead', '- kort: A | prio: 1 | min: 10\n- kort: B | prio: 3 | min: 55\n')
        code, out = self.run_cli()
        page = self.repo/'work'/'inbox'/'index.html'
        self.assertEqual(code, 0); self.assertTrue(page.is_file())
        self.assertIn(str(page), out); self.assertIn('2 items, 65 min; today: 1 item, 10 min', out)

    def test_explicit_out_and_empty(self):
        code, out = self.run_cli('--out', str(self.repo/'x'/'p.html'))
        self.assertIn('Nothing waits on you.', (self.repo/'x'/'p.html').read_text(encoding='utf8')); self.assertIn('0 items, 0 min', out)

if __name__ == '__main__':
    unittest.main()
