"""tools/checkq.py: the shared in-game check queue. No real Minecraft: jobs are fake python scripts (CHECKQ_FAKE_CMD), the
machine's Java processes come from a JSON file (CHECKQ_FAKE_PROCS) and busy ports from CHECKQ_FAKE_PORTS."""
import contextlib, io, json, os, pathlib, subprocess, sys, tempfile, time, unittest
from unittest import mock
TOOLS = pathlib.Path(__file__).resolve().parents[2]/'tools'
sys.path.insert(0, str(TOOLS))
import checkq

SCRIPT = str(TOOLS/'checkq.py')
FAKE = r'''
import json, os, pathlib, subprocess, sys, time
kind, args = sys.argv[1], sys.argv[2:]
code = int(os.environ.get('FAKE_EXIT', '0'))
pids = {'pid': os.getpid()}
if os.environ.get('FAKE_CHILD'):
    pids['child'] = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(60)']).pid
if os.environ.get('FAKE_PIDFILE'): pathlib.Path(os.environ['FAKE_PIDFILE']).write_text(json.dumps(pids))
print('fake', kind, *args, flush=True)
if kind == 'client':
    root = pathlib.Path('work/ingame-check')/args[0]; root.mkdir(parents=True, exist_ok=True)
    time.sleep(float(os.environ.get('FAKE_SLEEP', '0')))
    if not os.environ.get('FAKE_NOLOG'): (root/'gradle.log').write_text('suites ' + args[1] + '\nexit ' + str(code) + '\n')
    sys.exit(0)  # like ingame_check_run.sh: the script ends 0, the gradle exit is the log's last line
name, port = args[0], args[1]
root = pathlib.Path.cwd()/'work'/'method'/(name + '-123456'); root.mkdir(parents=True, exist_ok=True)
print(root.as_posix(), flush=True)
(root/'args.txt').write_text(' '.join(args[2:]))
time.sleep(float(os.environ.get('FAKE_SLEEP', '0')))
print('gradle exit ' + str(code) + ' root ' + root.as_posix(), flush=True)
'''
KORT_GAME = r'"C:\Program Files\Java\bin\javaw.exe" -Xmx4G net.minecraft.client.main.Main --username Kort --gameDir C:\Users\kortt\AppData\Roaming\.minecraft'
GRADLE = r'"C:\jdk\bin\java.exe" -cp gradle-launcher.jar org.gradle.launcher.daemon.bootstrap.GradleDaemon 8.8'


def git(cwd, *args):
    subprocess.run(['git', '-C', str(cwd), *args], check=True, capture_output=True)


def dead_pid():
    p = subprocess.Popen([sys.executable, '-c', 'pass']); p.wait(); return p.pid


_SHARED = {}


def setUpModule():  # one temp dir and three git lanes for the whole file (git init per class costs seconds)
    tmp = tempfile.TemporaryDirectory(); base = pathlib.Path(tmp.name)
    (base/'fake_job.py').write_text(FAKE)
    wts = {}
    for n in ('lane-a', 'lane-b', 'lane-c'):
        wt = base/n; (wt/'tools').mkdir(parents=True)
        for s in ('ingame_check_run.sh', 'balance_sim.sh'): (wt/'tools'/s).write_text('#!/bin/bash\n')
        git(wt, 'init', '-q', '-b', 'lane/' + n); git(wt, 'add', '.')
        git(wt, '-c', 'user.name=t', '-c', 'user.email=t@t', 'commit', '-qm', 'init')
        wts[n] = wt
    _SHARED.update(tmp=tmp, base=base, wt=wts, n=0)


def tearDownModule():
    for _ in range(20):
        try: _SHARED['tmp'].cleanup(); return
        except OSError: time.sleep(0.2)


class Base(unittest.TestCase):
    @property
    def base(self): return _SHARED['base']
    @property
    def wt(self): return _SHARED['wt']

    def setUp(self):
        _SHARED['n'] += 1; self.n = _SHARED['n']
        self.q = self.base/f'q{self.n}'
        self.procs = self.base/f'procs{self.n}.json'; self.set_procs([])
        self.env = dict(CHECKQ_DIR=str(self.q), CHECKQ_FAKE_CMD=json.dumps([sys.executable, '-S', str(self.base/'fake_job.py')]),
                        CHECKQ_FAKE_PROCS=str(self.procs), CHECKQ_FAKE_PORTS='', CHECKQ_POLL='0.05', CHECKQ_NO_RUNNER='1',
                        CHECKQ_JOB_TIMEOUT='20', CHECKQ_IDLE='1', CHECKQ_TEST='1')
        self.saved = {k: os.environ.get(k) for k in self.env}
        os.environ.update(self.env)
        self.runner = None
        self.extra = []

    def tearDown(self):
        if self.runner:
            for r in self.runner.jobs.values():
                checkq.kill_tree(r['proc']); r['log'].close()
        for p in self.extra: p.kill(); p.wait()
        for k, v in self.saved.items():
            if v is None: os.environ.pop(k, None)
            else: os.environ[k] = v

    def set_procs(self, cmds):
        self.procs.write_text(json.dumps([dict(pid=1000 + i, cmd=c) for i, c in enumerate(cmds)]))

    def submit(self, lane, kind='client', root=None, env=None, **kw):
        if kind == 'client': kw.setdefault('root', f'r{time.time_ns()}' if root is None else root); kw.setdefault('suites', 's1')
        else: kw.setdefault('name', 'sim'); kw.setdefault('gradle_args', ['-PbotSim=1'])
        with contextlib.redirect_stdout(io.StringIO()):
            return checkq.submit_job(self.wt[lane], kind, env=env or {}, **kw)

    def new_runner(self):
        self.runner = checkq.Runner()
        self.assertTrue(self.runner.lock.acquire())
        return self.runner

    def drive(self, until, limit=10):
        end = time.time() + limit
        while time.time() < end:
            self.runner.tick()
            if until(): return
            time.sleep(0.05)
        self.fail('condition not reached')

    def result(self, jid):
        return checkq.read_json(self.q/'done'/jid/'result.json')

    def done(self, *ids):
        return lambda: all(self.result(i) for i in ids)


class Submit(Base):
    def test_job_records_worktree_branch_and_sha(self):
        jid = self.submit('lane-a', root='knight-r1', suites='geo', env={'GRAVES_PHASE1_CHECKS': 'geo-knight'})
        job = checkq.read_json(self.q/'jobs'/(jid + '.json'))
        self.assertEqual(job['kind'], 'client'); self.assertEqual(job['branch'], 'lane/lane-a')
        self.assertEqual(len(job['sha']), 40)
        self.assertTrue(checkq.same_path(job['worktree'], self.wt['lane-a']))
        self.assertEqual(job['args'], {'root': 'knight-r1', 'suites': 'geo'})
        self.assertEqual(job['env'], {'GRAVES_PHASE1_CHECKS': 'geo-knight'})

    def test_ids_sort_in_submit_order(self):
        ids = [self.submit('lane-a') for _ in range(4)]
        self.assertEqual(ids, sorted(ids)); self.assertEqual(len(set(ids)), 4)

    def test_bad_names_refused(self):
        for bad in ('../x', 'a b', ''):
            with self.assertRaises(SystemExit): self.submit('lane-a', root=bad)
        with self.assertRaises(SystemExit): self.submit('lane-a', env={'lower': 'x'})

    def test_round_trip_detached_runner_and_wait(self):
        env = dict(os.environ); env.pop('CHECKQ_NO_RUNNER')
        out = subprocess.run([sys.executable, SCRIPT, 'submit', 'client', '--root', 'rt', '--suites', 'p1', '--env', 'FAKE_EXIT=3'],
                             cwd=self.wt['lane-a'], env=env, capture_output=True, text=True, timeout=20)
        self.assertEqual(out.returncode, 0, out.stderr)
        jid = out.stdout.split()[0]
        self.assertIn(str(self.q/'done'/jid/'result.json'), out.stdout)
        w = subprocess.run([sys.executable, SCRIPT, 'wait', jid, '--timeout', '15'], env=env, capture_output=True, text=True, timeout=20)
        self.assertEqual(w.returncode, 3, w.stdout + w.stderr)
        res = self.result(jid)
        self.assertEqual((res['status'], res['exit'], res['kind']), ('failed', 3, 'client'))
        root = self.wt['lane-a']/'work/ingame-check/rt'
        self.assertTrue(checkq.same_path(res['run_root'], root))
        self.assertEqual(checkq.read_json(root/'checkq-result.json')['id'], jid)
        self.assertIn('fake client rt p1', (root/'checkq.log').read_text())
        self.assertIn('exit 3', (self.q/'done'/jid/'gradle.log').read_text())
        end = time.time() + 8  # the runner leaves after its idle second
        while (self.q/'runner.lock').exists() and time.time() < end: time.sleep(0.1)
        self.assertFalse((self.q/'runner.lock').exists())

    def test_wait_timeout_and_unknown(self):
        jid = self.submit('lane-a')
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(checkq.wait_job(jid, 0.2), 124)
            self.assertEqual(checkq.wait_job('20990101-000000-000-ffff', 5), 3)


class Scheduling(Base):
    def test_passed_client_result(self):
        jid = self.submit('lane-b', root='ok')
        self.new_runner(); self.drive(self.done(jid))
        res = self.result(jid)
        self.assertEqual((res['status'], res['exit']), ('passed', 0))
        for k in ('id', 'kind', 'worktree', 'branch', 'sha', 'args', 'started', 'ended', 'exit', 'status', 'log', 'run_root'): self.assertIn(k, res)
        self.assertTrue(pathlib.Path(res['log']).exists())
        self.assertEqual(list((self.q/'running').glob('*')), [])

    def test_fifo_under_one_free_slot(self):
        self.set_procs([KORT_GAME] * 4)
        ids = [self.submit(lane) for lane in ('lane-b', 'lane-a', 'lane-c')]
        r = self.new_runner(); r.tick()
        self.assertEqual(list(r.jobs), ids[:1])
        self.drive(self.done(*ids))
        starts = [self.result(i)['started'] for i in ids]
        self.assertEqual(starts, sorted(starts))
        for a, b in zip(ids, ids[1:]): self.assertLessEqual(self.result(a)['ended'], self.result(b)['started'])

    def test_one_job_per_worktree(self):
        a1, a2, b = self.submit('lane-a', env={'FAKE_SLEEP': '0.25'}), self.submit('lane-a'), self.submit('lane-b', env={'FAKE_SLEEP': '0.25'})
        r = self.new_runner(); r.tick()
        self.assertEqual(sorted(r.jobs), sorted([a1, b]))
        self.drive(self.done(a1, a2, b))
        self.assertGreaterEqual(self.result(a2)['started'], self.result(a1)['ended'])

    def test_client_outside_queue_blocks_its_worktree(self):
        self.set_procs([f'java.exe net.fabricmc.devlaunchinjector.Main -cp {self.wt["lane-a"]}/build/classes'])
        a, b = self.submit('lane-a'), self.submit('lane-b')
        r = self.new_runner(); r.tick()
        self.assertEqual(list(r.jobs), [b])
        self.assertTrue((self.q/'jobs'/(a + '.json')).exists())

    def test_slot_limit_counts_kort_game_and_other_minecraft(self):
        self.set_procs([KORT_GAME, r'java.exe net.fabricmc.devlaunchinjector.Main C:\Users\kortt\Melty\other\run',
                        'java.exe -Dfabric.dli.env=server net.fabricmc.loader.impl.launch.knot.KnotServer nogui',
                        GRADLE, 'java.exe -jar some-ide.jar'])
        ids = [self.submit(lane, env={'FAKE_SLEEP': '0.3'}) for lane in ('lane-a', 'lane-b', 'lane-c')]
        r = self.new_runner(); r.tick()
        self.assertEqual(list(r.jobs), ids[:2])  # 3 other Minecraft processes + 2 queue jobs = 5
        snap = checkq.snapshot()
        self.assertEqual((snap['slots']['minecraft_other'], snap['slots']['queue_running'], snap['slots']['limit']), (3, 2, 5))
        self.drive(self.done(*ids))

    def test_kort_game_alone_takes_one_slot_and_never_pauses(self):
        self.set_procs([KORT_GAME])
        wts = ['lane-a', 'lane-b', 'lane-c']
        ids = [self.submit(l, env={'FAKE_SLEEP': '0.3'}) for l in wts]
        r = self.new_runner(); r.tick()
        self.assertEqual(len(r.jobs), 3)
        self.drive(self.done(*ids))

    def test_is_minecraft(self):
        for cmd in (KORT_GAME, 'java net.fabricmc.devlaunchinjector.Main', 'java -jar fabric-server-launch.jar nogui', 'java x --gameDir y'):
            self.assertTrue(checkq.is_minecraft(cmd), cmd)
        for cmd in (GRADLE, 'java -jar idea.jar', '', None):
            self.assertFalse(checkq.is_minecraft(cmd), cmd)

    def test_timeout_stops_only_its_own_tree(self):
        os.environ['CHECKQ_JOB_TIMEOUT'] = '0.6'
        pidfile = self.base/f'pids{self.n}.json'
        other = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(60)']); self.extra.append(other)
        jid = self.submit('lane-a', env={'FAKE_SLEEP': '30', 'FAKE_CHILD': '1', 'FAKE_PIDFILE': str(pidfile)})
        self.new_runner(); self.drive(self.done(jid))
        res = self.result(jid)
        self.assertEqual((res['status'], res['exit']), ('timeout', 124))
        pids = json.loads(pidfile.read_text())
        time.sleep(0.2)
        self.assertFalse(checkq.pid_alive(pids['pid'])); self.assertFalse(checkq.pid_alive(pids['child']))
        self.assertTrue(checkq.pid_alive(other.pid)); self.assertIsNone(other.poll())

    def test_cancel_waiting(self):
        jid = self.submit('lane-a')
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(checkq.cancel_job(jid), 0)
            self.assertEqual(checkq.wait_job(jid, 2), 125)
        self.assertEqual(self.result(jid)['status'], 'cancelled')
        self.assertEqual(list((self.q/'jobs').glob('*.json')), [])

    def test_cancel_running_stops_its_tree(self):
        pidfile = self.base/f'pids{self.n}.json'
        jid = self.submit('lane-c', env={'FAKE_SLEEP': '30', 'FAKE_CHILD': '1', 'FAKE_PIDFILE': str(pidfile)})
        self.new_runner(); self.drive(lambda: pidfile.exists())
        with contextlib.redirect_stdout(io.StringIO()): self.assertEqual(checkq.cancel_job(jid), 0)
        self.drive(self.done(jid))
        self.assertEqual((self.result(jid)['status'], self.result(jid)['exit']), ('cancelled', 125))
        time.sleep(0.2)
        self.assertFalse(checkq.pid_alive(json.loads(pidfile.read_text())['child']))

    def test_bad_job_file_is_an_error_result_and_the_runner_goes_on(self):
        (self.q/'jobs').mkdir(parents=True)
        (self.q/'jobs'/'20200101-000000-000-bad0.json').write_text('{not json')
        (self.q/'jobs'/'20200101-000000-001-bad1.json').write_text(json.dumps({'kind': 'rocket', 'worktree': 'x'}))
        good = self.submit('lane-a')
        self.new_runner(); self.drive(self.done('20200101-000000-000-bad0', '20200101-000000-001-bad1', good))
        self.assertEqual((self.result('20200101-000000-000-bad0')['status'], self.result('20200101-000000-000-bad0')['exit']), ('error', 126))
        self.assertEqual(self.result('20200101-000000-001-bad1')['status'], 'error')
        self.assertEqual(self.result(good)['status'], 'passed')

    def test_sim_port_skips_busy_ports_and_result_lands_in_sim_root(self):
        os.environ['CHECKQ_FAKE_PORTS'] = '25680,25681'
        s1 = self.submit('lane-a', 'sim', name='duels', env={'FAKE_SLEEP': '0.25'})
        s2 = self.submit('lane-b', 'sim', name='p1a')
        self.new_runner(); self.drive(self.done(s1, s2))
        self.assertEqual(self.result(s1)['port'], 25682); self.assertEqual(self.result(s2)['port'], 25683)
        root = self.wt['lane-a']/'work/method/duels-123456'
        self.assertTrue(checkq.same_path(self.result(s1)['run_root'], root))
        self.assertEqual(checkq.read_json(root/'checkq-result.json')['status'], 'passed')
        self.assertIn('-PbotSim=1', (root/'args.txt').read_text())

    def test_sim_waits_when_every_port_is_busy(self):
        os.environ['CHECKQ_FAKE_PORTS'] = ','.join(str(p) for p in checkq.PORTS)
        s = self.submit('lane-a', 'sim')
        r = self.new_runner(); r.tick()
        self.assertEqual(r.jobs, {}); self.assertTrue((self.q/'jobs'/(s + '.json')).exists())

    def test_minecraft_process_naming_a_running_job_worktree_is_that_job(self):
        a = self.submit('lane-a', env={'FAKE_SLEEP': '0.6'})
        r = self.new_runner(); r.tick()
        self.assertEqual(list(r.jobs), [a])
        # a's own client now shows up in the process list: 3 others + a's client (= job a) + job a = 4 slots, not 5
        self.set_procs([KORT_GAME] * 3 + [f'java.exe net.fabricmc.devlaunchinjector.Main -cp {self.wt["lane-a"]}/build/classes'])
        b = self.submit('lane-b', env={'FAKE_SLEEP': '0.2'})
        r.tick()
        self.assertEqual(sorted(r.jobs), sorted([a, b]))
        snap = checkq.snapshot()
        self.assertEqual((snap['slots']['minecraft_other'], snap['slots']['queue_running']), (3, 2))
        self.drive(self.done(a, b))

    def test_foreign_runner_jobs_count_only_when_its_owner_holds_the_live_lock(self):
        other = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(60)']); self.extra.append(other)
        (self.q/'running').mkdir(parents=True)
        job = dict(id='20200101-000000-000-fore', kind='client', worktree=str(self.wt['lane-a']), args={'root': 'x'},
                   runner_pid=other.pid, job_pid=other.pid)
        checkq.write_json(self.q/'running'/'20200101-000000-000-fore.json', job)
        r = checkq.Runner()
        self.assertEqual(r.foreign(), [])  # the pid is alive but no lock names it: a reused pid
        checkq.write_json(self.q/'runner.lock', dict(pid=other.pid, token='them', heartbeat=time.time()))
        self.assertEqual([f['id'] for f in r.foreign()], ['20200101-000000-000-fore'])
        checkq.write_json(self.q/'runner.lock', dict(pid=other.pid, token='them', heartbeat=time.time() - 600))
        self.assertEqual(r.foreign(), [])  # stalled past the stale limit

    def test_reused_pid_without_a_lock_does_not_hold_a_job_forever(self):
        other = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(60)']); self.extra.append(other)
        (self.q/'running').mkdir(parents=True)
        checkq.write_json(self.q/'running'/'20200101-000000-000-reus.json',
                          dict(id='20200101-000000-000-reus', kind='client', worktree=str(self.wt['lane-a']), args={'root': 'x'},
                               runner_pid=other.pid, job_pid=other.pid))  # a live pid (reused after a reboot), no runner owns it
        r = self.new_runner(); r.adopt()
        self.assertEqual(self.result('20200101-000000-000-reus')['status'], 'error')
        self.assertEqual(list((self.q/'running').glob('*.json')), [])
        self.assertIsNone(other.poll())

    def test_stalled_old_runner_is_taken_over_and_its_job_recorded(self):
        old = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(60)']); self.extra.append(old)  # alive, but silent
        (self.q/'running').mkdir(parents=True)
        checkq.write_json(self.q/'runner.lock', dict(pid=old.pid, token='old', heartbeat=time.time() - 600))
        checkq.write_json(self.q/'running'/'20200101-000000-000-stal.json',
                          dict(id='20200101-000000-000-stal', kind='client', worktree=str(self.wt['lane-c']), args={'root': 'x'},
                               runner_pid=old.pid, job_pid=old.pid))
        self.assertFalse(checkq.runner_info()[1])
        jid = self.submit('lane-a')
        self.new_runner(); self.runner.adopt(); self.drive(self.done(jid))
        self.assertEqual(self.result('20200101-000000-000-stal')['status'], 'error')
        self.assertEqual(self.result(jid)['status'], 'passed')
        self.assertIsNone(old.poll())  # the stalled runner's processes were left alone

    def test_stale_gradle_log_from_an_earlier_run_is_not_a_pass(self):
        root = self.wt['lane-a']/'work/ingame-check/again'; root.mkdir(parents=True, exist_ok=True)
        old = root/'gradle.log'; old.write_text('exit 0\n')
        os.utime(old, (time.time() - 3600, time.time() - 3600))
        jid = self.submit('lane-a', root='again', env={'FAKE_NOLOG': '1'})
        self.new_runner(); self.drive(self.done(jid))
        res = self.result(jid)
        self.assertEqual((res['status'], res['exit']), ('error', 126)); self.assertIn('older', res['note'])
        self.assertFalse((self.q/'done'/jid/'gradle.log').exists())

    def test_job_is_not_left_untracked_when_its_running_record_cannot_be_written(self):
        real, killed = checkq.write_json, []
        def flaky(path, data):
            if pathlib.Path(path).parent.name == 'running': raise OSError('disk full')
            return real(path, data)
        spy_target = checkq.kill_tree
        def spy(proc): killed.append(proc); return spy_target(proc)
        jid = self.submit('lane-a', env={'FAKE_SLEEP': '30'})
        r = self.new_runner()
        with mock.patch.object(checkq, 'write_json', flaky), mock.patch.object(checkq, 'kill_tree', spy):
            r.tick()
        self.assertEqual(len(killed), 1); self.assertIsNotNone(killed[0].poll())  # the process was stopped, not leaked
        self.assertEqual(r.jobs, {})
        res = self.result(jid)
        self.assertEqual(res['status'], 'error'); self.assertIn('could not start', res['note'])

    def test_transient_netstat_failure_retries_the_sim_next_tick(self):
        s = self.submit('lane-a', 'sim')
        r = self.new_runner()
        with mock.patch.object(checkq, 'busy_ports', side_effect=RuntimeError('netstat failed')): r.tick()
        self.assertEqual(r.jobs, {}); self.assertTrue((self.q/'jobs'/(s + '.json')).exists()); self.assertIsNone(self.result(s))
        self.drive(self.done(s))
        self.assertEqual(self.result(s)['status'], 'passed')

    def test_lost_lock_starts_nothing(self):
        a = self.submit('lane-a')
        r = self.new_runner()
        checkq.write_json(self.q/'runner.lock', dict(pid=os.getpid(), token='someone-else', heartbeat=time.time()))
        r.lock.lost = False  # skip beat's own check: the re-check right before launch must catch it
        r.start_jobs(sorted((self.q/'jobs').glob('*.json')))
        self.assertEqual(r.jobs, {}); self.assertTrue(r.lock.lost); self.assertTrue((self.q/'jobs'/(a + '.json')).exists())

    def test_orphan_of_a_dead_runner_is_recorded_not_killed(self):
        (self.q/'running').mkdir(parents=True)
        checkq.write_json(self.q/'running'/'20200101-000000-000-orph.json',
                          dict(id='20200101-000000-000-orph', kind='client', worktree=str(self.wt['lane-a']), args={'root': 'x'},
                               runner_pid=dead_pid(), job_pid=os.getpid()))
        r = self.new_runner(); r.adopt()
        self.assertEqual(self.result('20200101-000000-000-orph')['status'], 'error')
        self.assertTrue(checkq.pid_alive(os.getpid()))


class Locking(Base):
    def test_stale_lock_takeover(self):
        self.q.mkdir(parents=True)
        lock = self.q/'runner.lock'
        lock.write_text(json.dumps(dict(pid=dead_pid(), heartbeat=time.time(), token='old')))
        a = checkq.Lock(); self.assertTrue(a.acquire()); a.release()
        lock.write_text(json.dumps(dict(pid=os.getpid(), heartbeat=time.time() - 600, token='old')))
        b = checkq.Lock(); self.assertTrue(b.acquire())
        self.assertEqual(checkq.read_json(lock)['token'], b.token)
        b.release(); self.assertFalse(lock.exists())

    def test_unreadable_lock_is_removed_only_when_old(self):
        self.q.mkdir(parents=True)
        lock = self.q/'runner.lock'
        lock.write_text('{half a file')
        self.assertFalse(checkq.Lock().acquire())  # fresh and unreadable: might be a live lock mid-write
        os.utime(lock, (time.time() - 600, time.time() - 600))
        c = checkq.Lock(); self.assertTrue(c.acquire()); c.release()

    def test_beat_retries_one_failed_read_before_declaring_the_lock_lost(self):
        a = checkq.Lock(); self.assertTrue(a.acquire())
        real, calls = checkq.read_json, []
        def flaky(path):
            calls.append(path)
            return None if len(calls) == 1 else real(path)
        with mock.patch.object(checkq, 'read_json', flaky): a.beat()
        self.assertFalse(a.lost)
        checkq.write_json(self.q/'runner.lock', dict(pid=os.getpid(), token='other', heartbeat=time.time()))
        a.beat(); self.assertTrue(a.lost)

    def test_second_runner_refuses(self):
        a = checkq.Lock(); self.assertTrue(a.acquire())
        self.assertFalse(checkq.Lock().acquire())
        out = subprocess.run([sys.executable, SCRIPT, 'runner'], env=dict(os.environ), capture_output=True, text=True, timeout=20)
        self.assertEqual(out.returncode, 3, out.stdout + out.stderr)
        self.assertIn('already', out.stdout)
        self.assertEqual(checkq.read_json(self.q/'runner.lock')['token'], a.token)
        a.release()


class Hooks(Base):
    def test_fake_hooks_ignored_without_checkq_test(self):
        self.assertTrue(checkq.test_hooks()); self.assertEqual(checkq.fake('PORTS'), '')
        del os.environ['CHECKQ_TEST']
        try:
            self.assertEqual(checkq.test_hooks(), []); self.assertIsNone(checkq.fake('CMD'))
            self.assertNotIn('test hooks active', checkq.status_text(checkq.snapshot()))
        finally: os.environ['CHECKQ_TEST'] = '1'
        self.assertIn('test hooks active', checkq.status_text(checkq.snapshot()))


class Wait(Base):
    def test_runner_error_exit_126_and_foreign_exit_codes_pass_through(self):
        (self.q/'jobs').mkdir(parents=True)
        (self.q/'jobs'/'20200101-000000-000-bad0.json').write_text('{not json')
        self.new_runner(); self.drive(self.done('20200101-000000-000-bad0'))
        with contextlib.redirect_stdout(io.StringIO()) as out:
            self.assertEqual(checkq.wait_job('20200101-000000-000-bad0', 5), 126)
        self.assertIn('error', out.getvalue())

    def test_wait_cli_unknown_job_is_3_not_argparses_2(self):
        out = subprocess.run([sys.executable, SCRIPT, 'wait', '20990101-000000-000-ffff', '--timeout', '10'], env=dict(os.environ),
                             capture_output=True, text=True, timeout=30)
        self.assertEqual(out.returncode, 3, out.stdout + out.stderr)
        self.assertIn('3 unknown job', subprocess.run([sys.executable, SCRIPT, 'wait', '--help'], capture_output=True, text=True).stdout)


class Status(Base):
    def test_status_text_and_json(self):
        self.set_procs([KORT_GAME])
        done = self.submit('lane-a', root='finished')
        self.new_runner(); self.drive(self.done(done))
        run = self.submit('lane-b', root='busy', env={'FAKE_SLEEP': '30'})
        self.runner.tick()
        wait = self.submit('lane-b', root='queued')
        out = io.StringIO()
        with contextlib.redirect_stdout(out): self.assertEqual(checkq.main(['status']), 0)
        text = out.getvalue()
        for s in ('Waiting (1)', 'Running (1)', 'Recent', wait, run, done, 'passed', 'lane-b', '2 of 5'): self.assertIn(s, text)
        js = subprocess.run([sys.executable, SCRIPT, 'status', '--json'], env=dict(os.environ), capture_output=True, text=True, timeout=20)
        data = json.loads(js.stdout)
        self.assertEqual([j['id'] for j in data['waiting']], [wait])
        self.assertEqual([j['id'] for j in data['running']], [run])
        self.assertEqual(data['recent'][0]['id'], done)
        self.assertTrue(data['runner']['alive'])
        self.assertEqual(data['slots']['minecraft_other'], 1)


class Helpers(unittest.TestCase):
    def test_git_bash_root_becomes_a_windows_path(self):
        self.assertEqual(checkq.native_path('/c/Users/kortt/Melty/bb/work/method/p1-101010'), 'C:/Users/kortt/Melty/bb/work/method/p1-101010')
        self.assertEqual(checkq.native_path('C:/x'), 'C:/x')

    def test_exit_parsing(self):
        self.assertEqual(checkq.client_exit('a\nexit 1\nb\nexit 0\n'), 0)
        self.assertIsNone(checkq.client_exit('no line'))
        self.assertEqual(checkq.sim_exit('/c/x\ngradle exit 2 root /c/x\n'), 2)
        self.assertEqual(checkq.sim_root('port 1 x\n/c/U/w/work/method/p1-101010\n', 'p1'), 'C:/U/w/work/method/p1-101010')
        self.assertIsNone(checkq.sim_root('port 25680 is in use; pick another\n', 'p1'))

    def test_path_in_needs_a_boundary(self):
        self.assertTrue(checkq.path_in(r'java -cp C:\Users\k\bb\build;x', 'C:/Users/k/bb'))
        self.assertFalse(checkq.path_in(r'java -cp C:\Users\k\bb-other\build', 'C:/Users/k/bb'))


if __name__ == '__main__':
    unittest.main()
