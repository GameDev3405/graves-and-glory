"""Checks tools/builder-server/builder_ops.py (plan task 2A.1): backup naming and retention, scheduled-due logic, zip layout,
offsite copy, log rotation, preflight (EULA), version stamp, overload parsing, and the health check against a fake
Minecraft status server. Pure: no Minecraft, no Java, no real server directory."""
import datetime as dt
import json
import os
import pathlib
import shutil
import socket
import struct
import subprocess
import sys
import tempfile
import threading
import time
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools' / 'builder-server'))
import builder_ops as ops  # noqa: E402

NOW = dt.datetime(2026, 11, 1, 12, 0, 0)


def name_at(delta, reason=''):
    return ops.backup_name(NOW - delta, reason)


def make_world(server, world='camp', files=None):
    w = pathlib.Path(server) / f'gg-build-{world}'
    (w / 'region').mkdir(parents=True, exist_ok=True)
    for rel, data in (files or {'level.dat': b'level', 'region/r.0.0.mca': b'chunks', 'gg-build.json': b'{}', 'session.lock': b'lock'}).items():
        (w / rel).write_bytes(data)
    return w


class Retention(unittest.TestCase):
    def test_name_round_trip(self):
        n = ops.backup_name(NOW, 'prestart')
        self.assertEqual(n, '20261101-120000-prestart.zip')
        self.assertEqual(ops.parse_backup_name(n), (NOW, 'prestart'))
        self.assertEqual(ops.parse_backup_name('20261101-120000.zip'), (NOW, ''))
        for bad in ('notes.txt', '20261101-120000.zip.part', '20261340-250000.zip', 'x20261101-120000.zip'):
            self.assertIsNone(ops.parse_backup_name(bad), bad)

    def test_keeps_two_days_then_one_per_day_for_two_weeks(self):
        names = [name_at(dt.timedelta(hours=h)) for h in (0, 5, 30, 47)]            # all inside 48 h: kept
        d3 = [name_at(dt.timedelta(days=3, hours=h)) for h in (0, 2, 4)]            # same day: newest only
        d5 = [name_at(dt.timedelta(days=5)), name_at(dt.timedelta(days=5, hours=1), 'live')]
        old = [name_at(dt.timedelta(days=14, minutes=1)), name_at(dt.timedelta(days=40))]
        foreign = ['readme.txt', 'x.zip']
        drop = ops.select_prune(names + d3 + d5 + old + foreign, NOW)
        self.assertEqual(set(drop), {d3[1], d3[2], d5[1], *old})
        self.assertFalse(set(drop) & set(names))
        self.assertFalse(set(drop) & set(foreign))

    def test_boundaries_match_the_mod_rule(self):
        # BuildMode.prune: under 48 h keep; older keeps the newest per day; 14 days or more drop.
        d = dt.timedelta
        self.assertEqual(ops.select_prune([name_at(d(hours=47, minutes=59))], NOW), [])
        self.assertEqual(ops.select_prune([name_at(d(days=13, hours=23))], NOW), [])
        self.assertEqual(ops.select_prune([name_at(d(days=14))], NOW), [name_at(d(days=14))])

    def test_prune_dir_removes_files_and_stale_parts(self):
        with tempfile.TemporaryDirectory() as t:
            t = pathlib.Path(t)
            keepf, dropf = t / name_at(dt.timedelta(hours=1)), t / name_at(dt.timedelta(days=30))
            stale, fresh = t / 'a.zip.part', t / 'b.zip.part'
            for f in (keepf, dropf, stale, fresh):
                f.write_bytes(b'x')
            os.utime(stale, (time.time() - 7200,) * 2)
            ops.prune_dir(t, NOW)
            self.assertTrue(keepf.exists() and fresh.exists())
            self.assertFalse(dropf.exists() or stale.exists())


class Backups(unittest.TestCase):
    def test_zip_layout_matches_the_mod_and_skips_session_lock(self):
        with tempfile.TemporaryDirectory() as t:
            make_world(t)
            out = ops.make_backup(t, 'camp', 'prestart', NOW)
            self.assertEqual(out.parent, pathlib.Path(t) / 'backups' / 'camp')
            self.assertEqual(out.name, '20261101-120000-prestart.zip')
            with zipfile.ZipFile(out) as z:
                self.assertEqual(sorted(z.namelist()), ['gg-build.json', 'level.dat', 'region/r.0.0.mca'])
                self.assertIsNone(z.testzip())
            self.assertEqual(list(out.parent.glob('*.part')), [])

    def test_missing_world_is_a_skip_not_an_error(self):
        with tempfile.TemporaryDirectory() as t:
            self.assertIsNone(ops.make_backup(t, 'camp', 'manual', NOW))

    def test_scheduled_due(self):
        with tempfile.TemporaryDirectory() as t:
            w = make_world(t)
            bdir = pathlib.Path(t) / 'backups' / 'camp'
            self.assertEqual(ops.scheduled_due(w, bdir, NOW, 60)[0], True)           # no backup yet
            past = NOW - dt.timedelta(hours=3)
            made = ops.make_backup(t, 'camp', '', past, retention=False)
            os.utime(made, (past.timestamp(),) * 2)
            for f in w.rglob('*'):                                                    # world edited after the backup
                if f.is_file():
                    os.utime(f, (NOW.timestamp(),) * 2)
            self.assertEqual(ops.scheduled_due(w, bdir, NOW, 60)[0], True)
            self.assertEqual(ops.scheduled_due(w, bdir, NOW, 600)[0], False)         # backup younger than the interval
            for f in w.rglob('*'):                                                    # world older than the backup
                if f.is_file():
                    os.utime(f, ((past - dt.timedelta(hours=1)).timestamp(),) * 2)
            self.assertEqual(ops.scheduled_due(w, bdir, NOW, 60)[0], False)

    def test_session_lock_does_not_count_as_a_change(self):
        with tempfile.TemporaryDirectory() as t:
            w = make_world(t)
            for f in w.rglob('*'):
                os.utime(f, (1000, 1000))
            os.utime(w / 'session.lock', (NOW.timestamp(),) * 2)
            self.assertEqual(ops.newest_mtime(w), 1000)

    def test_offsite_copies_once_and_prunes_there(self):
        with tempfile.TemporaryDirectory() as t, tempfile.TemporaryDirectory() as off:
            make_world(t)
            ops.make_backup(t, 'camp', '', NOW - dt.timedelta(hours=2), retention=False)
            ops.make_backup(t, 'camp', 'prestart', NOW, retention=False)
            first = ops.offsite_copy(t, off, NOW)
            self.assertEqual(len(first), 2)
            self.assertEqual(ops.offsite_copy(t, off, NOW), [])                      # nothing new
            ancient = pathlib.Path(off) / 'camp' / name_at(dt.timedelta(days=60))
            ancient.write_bytes(b'x')
            ops.offsite_copy(t, off, NOW)
            self.assertFalse(ancient.exists())
            self.assertEqual(len(list((pathlib.Path(t) / 'backups' / 'camp').glob('*.zip'))), 2)


class Logs(unittest.TestCase):
    def test_select_old_logs(self):
        now = 10_000_000
        day = 86400
        entries = [('latest.log', 0), ('supervisor.log', 0), ('2026-10-01-1.log.gz', now - 20 * day),
                   ('2026-10-30-1.log.gz', now - 2 * day), ('crash-report.txt', 0)]
        self.assertEqual(ops.select_old_logs(entries, now, 14), ['2026-10-01-1.log.gz'])

    def test_supervisor_log_rotation(self):
        with tempfile.TemporaryDirectory() as t:
            log = pathlib.Path(t) / 'supervisor.log'
            log.write_bytes(b'a' * 100)
            self.assertFalse(ops.rotate_supervisor_log(log, max_bytes=1000))
            for round_ in range(7):
                log.write_bytes(bytes([65 + round_]) * 2000)
                self.assertTrue(ops.rotate_supervisor_log(log, max_bytes=1000, keep=3))
            self.assertFalse(log.exists())
            self.assertEqual(sorted(p.name for p in pathlib.Path(t).iterdir()), ['supervisor.log.1', 'supervisor.log.2', 'supervisor.log.3'])
            self.assertEqual((pathlib.Path(t) / 'supervisor.log.1').read_bytes()[:1], b'G')

    def test_rotate_logs_end_to_end(self):
        with tempfile.TemporaryDirectory() as t:
            logs = pathlib.Path(t) / 'logs'
            logs.mkdir()
            old, new = logs / 'old.log.gz', logs / 'new.log.gz'
            old.write_bytes(b'x')
            new.write_bytes(b'x')
            os.utime(old, (1, 1))
            self.assertEqual(ops.rotate_logs(t, 14), ['old.log.gz'])
            self.assertTrue(new.exists())


class Preflight(unittest.TestCase):
    def test_eula_is_required_and_never_created(self):
        with tempfile.TemporaryDirectory() as t:
            t = pathlib.Path(t)
            (t / 'fabric-server-launch.jar').write_bytes(b'j')
            probs = ops.preflight(t)
            self.assertEqual(len(probs), 1)
            self.assertIn('EULA', probs[0])
            self.assertFalse((t / 'eula.txt').exists())
            (t / 'eula.txt').write_text('eula=false\n')
            self.assertEqual(len(ops.preflight(t)), 1)
            (t / 'eula.txt').write_text('# x\neula=true\n')
            self.assertEqual(ops.preflight(t), [])

    def test_missing_launcher(self):
        with tempfile.TemporaryDirectory() as t:
            self.assertTrue(any('fabric-server-launch.jar' in p for p in ops.preflight(t)))


class Stamp(unittest.TestCase):
    def test_stamp_lists_mods_and_launcher(self):
        with tempfile.TemporaryDirectory() as t:
            t = pathlib.Path(t)
            (t / 'mods').mkdir()
            (t / 'mods' / 'graves-and-glory-0.5.0.jar').write_bytes(b'mod')
            (t / 'fabric-server-launch.jar').write_bytes(b'launcher')
            s = ops.build_stamp(t, 'camp', NOW)
            self.assertEqual(s['world'], 'camp')
            self.assertEqual(s['startedAt'], '2026-11-01T12:00:00')
            self.assertEqual(s['mods']['graves-and-glory-0.5.0.jar']['bytes'], 3)
            self.assertEqual(len(s['mods']['graves-and-glory-0.5.0.jar']['sha256']), 16)
            json.dumps(s)


class FakeStatusServer:
    """Answers one Minecraft server-list ping per connection."""

    def __init__(self):
        self.sock = socket.socket()
        self.sock.bind(('127.0.0.1', 0))
        self.sock.listen(4)
        self.port = self.sock.getsockname()[1]
        self.thread = threading.Thread(target=self.run, daemon=True)
        self.thread.start()

    def run(self):
        while True:
            try:
                conn, _ = self.sock.accept()
            except OSError:
                return
            with conn:
                try:
                    conn.recv(4096)
                    body = json.dumps({'version': {'name': '1.21.1', 'protocol': 767}, 'players': {'online': 2, 'max': 10}}).encode()
                    payload = ops._varint(0) + ops._varint(len(body)) + body
                    conn.sendall(ops._varint(len(payload)) + payload)
                except OSError:
                    pass

    def close(self):
        self.sock.close()


class Health(unittest.TestCase):
    def test_overload_parser(self):
        lines = ["[12:00:00] [Server thread/WARN]: Can't keep up! Is the server overloaded? Running 2500ms or 50 ticks behind",
                 '[12:01:00] [Server thread/INFO]: fine',
                 "[12:02:00] [Server thread/WARN]: Can't keep up! Is the server overloaded? Running 7000ms or 140 ticks behind"]
        self.assertEqual(ops.overload_stats(lines), (2, 7000))
        self.assertEqual(ops.overload_stats([]), (0, 0))

    def make_server(self, t, port, ip='127.0.0.1'):
        t = pathlib.Path(t)
        (t / 'server.properties').write_text(f'# c\nserver-port={port}\nserver-ip={ip}\n')
        (t / 'current-world.txt').write_text('camp')
        return t

    def test_up_with_backup_and_stamp(self):
        srv = FakeStatusServer()
        try:
            with tempfile.TemporaryDirectory() as t:
                t = self.make_server(t, srv.port)
                make_world(t)
                ops.make_backup(t, 'camp', 'prestart', NOW - dt.timedelta(minutes=30))
                (t / 'version-stamp.json').write_text(json.dumps({'startedAt': 'x', 'mods': {'m.jar': {'sha256': 'abcdef0123456789'}}}))
                r = ops.health(t, NOW)
                self.assertTrue(r['up'])
                self.assertEqual((r['players'], r['maxPlayers'], r['version']), (2, 10, '1.21.1'))
                self.assertEqual(r['lastBackup']['ageMinutes'], 30)
                self.assertEqual(r['status'], 'OK')
                self.assertIsNone(r['tick']['tps'])
                text = ops.format_health(r)
                self.assertIn('status: OK', text)
                self.assertIn('last backup: 20261101-113000-prestart.zip (30 min ago)', text)
        finally:
            srv.close()

    def test_down_and_no_backup(self):
        s = socket.socket()
        s.bind(('127.0.0.1', 0))
        port = s.getsockname()[1]
        s.close()  # nothing listening
        with tempfile.TemporaryDirectory() as t:
            r = ops.health(self.make_server(t, port), NOW)
            self.assertFalse(r['up'])
            self.assertEqual(r['status'], 'DOWN')
            self.assertIsNone(r['lastBackup'])
            self.assertIn('no backup for the current world', r['problems'])

    def test_stale_backup_and_overload_warn(self):
        srv = FakeStatusServer()
        try:
            with tempfile.TemporaryDirectory() as t:
                t = self.make_server(t, srv.port)
                make_world(t)
                ops.make_backup(t, 'camp', '', NOW - dt.timedelta(hours=30), retention=False)
                (t / 'logs').mkdir()
                (t / 'logs' / 'latest.log').write_text(("[1] Can't keep up! Is the server overloaded? Running 3000ms or 60 ticks behind\n") * 6)
                r = ops.health(t, NOW)
                self.assertEqual(r['status'], 'WARN')
                self.assertEqual(len(r['problems']), 2)
        finally:
            srv.close()

    def test_cli_health_exit_codes_and_files(self):
        s = socket.socket()
        s.bind(('127.0.0.1', 0))
        port = s.getsockname()[1]
        s.close()
        with tempfile.TemporaryDirectory() as t:
            self.make_server(t, port)
            self.assertEqual(ops.main(['--server-dir', t, 'health']), 2)
            self.assertTrue((pathlib.Path(t) / 'health.json').exists())


class Cli(unittest.TestCase):
    def test_backup_command_variants(self):
        with tempfile.TemporaryDirectory() as t:
            t = pathlib.Path(t)
            (t / 'current-world.txt').write_text('camp')
            self.assertEqual(ops.main(['--server-dir', str(t), 'backup', '--reason', 'prestart']), 0)   # no world yet: skip
            make_world(t)
            self.assertEqual(ops.main(['--server-dir', str(t), 'backup', '--reason', 'prestart', '--changed-only']), 0)
            first = list((t / 'backups' / 'camp').glob('*.zip'))
            self.assertEqual(len(first), 1)
            self.assertEqual(ops.main(['--server-dir', str(t), 'backup', '--reason', 'prestart', '--changed-only']), 0)
            self.assertEqual(len(list((t / 'backups' / 'camp').glob('*.zip'))), 1)   # unchanged: no second zip
            self.assertEqual(ops.main(['--server-dir', str(t), 'backup', '--world', 'nowhere']), 2)
            self.assertEqual(ops.main(['--server-dir', str(t), 'offsite']), 0)         # unconfigured: skipped
            self.assertEqual(ops.main(['--server-dir', str(t), 'preflight']), 3)


class CrashedRun(unittest.TestCase):
    """Exit 0 after a crash (builder server, 2026-11-03) must still restart; a deliberate stop must still end the supervisor."""
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.t = pathlib.Path(self.tmp.name)
        (self.t / 'logs').mkdir()
        self.since = time.time() - 60

    def tearDown(self):
        self.tmp.cleanup()

    def log(self, text, age=0):
        f = self.t / 'logs' / 'latest.log'
        f.write_text(text)
        os.utime(f, (time.time() - age, time.time() - age))

    def test_deliberate_stop_is_clean(self):
        self.log('[12:00:00] [Server thread/INFO]: Stopping the server\n[12:00:01] [Server thread/INFO]: Saving worlds\n')
        self.assertIsNone(ops.run_crashed(self.t, self.since))
        self.assertEqual(ops.main(['--server-dir', str(self.t), 'run-crashed', '--since', str(self.since)]), 0)

    def test_tick_loop_lines_count_as_a_crash(self):
        for line in ('[16:08:12] [Server thread/ERROR]: Encountered an unexpected exception', '[16:08:12] [Server thread/ERROR]: Exception in server tick loop'):
            self.log('[16:00:00] Done (3.3s)!\n' + line + '\njava.lang.NullPointerException\n[16:08:13] Stopping server\n')
            self.assertIn('latest.log', ops.run_crashed(self.t, self.since))
            self.assertEqual(ops.main(['--server-dir', str(self.t), 'run-crashed', '--since', str(self.since)]), 1)

    def test_new_crash_report_counts_and_an_old_one_does_not(self):
        self.log('clean\n')
        reports = self.t / 'crash-reports'
        reports.mkdir()
        old = reports / 'crash-2026-11-01_10.00.00-server.txt'
        old.write_text('old')
        os.utime(old, (self.since - 3600, self.since - 3600))
        self.assertIsNone(ops.run_crashed(self.t, self.since))
        (reports / 'crash-2026-11-03_16.08.12-server.txt').write_text('new')
        self.assertIn('crash-2026-11-03_16.08.12-server.txt', ops.run_crashed(self.t, self.since))

    def test_a_previous_runs_log_is_ignored(self):
        self.log('Exception in server tick loop' + chr(10), age=3600)   # latest.log not rewritten since the start: not this run's
        self.assertIsNone(ops.run_crashed(self.t, self.since))

    def test_supervisor_checks_before_ending_on_exit_0(self):
        ps = (ROOT / 'tools' / 'builder-server' / 'start-build-server.ps1').read_text()
        stop = ps.index("'clean stop; supervisor ending.'")
        check = ps.index('Test-RunCrashed $PSScriptRoot $startedUtc')
        self.assertLess(check, stop)
        self.assertLess(ps.index('$startedUtc = '), ps.index('Start-Process'))
        self.assertNotIn("Invoke-Ops @('run-crashed'", ps)   # the server PC has no Python; the check must not need it


POWERSHELL = shutil.which('powershell') or shutil.which('pwsh')


@unittest.skipUnless(POWERSHELL, 'needs PowerShell')
class SupervisorCrashCheck(unittest.TestCase):
    """The pure-PowerShell Test-RunCrashed in start-build-server.ps1 (no Python on the server PC), run through its -CheckCrashedSince
    dry run against a temp server folder. Same cases as CrashedRun."""
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.t = pathlib.Path(self.tmp.name)
        (self.t / 'logs').mkdir()
        self.since = int(time.time()) - 60

    def tearDown(self):
        self.tmp.cleanup()

    def log(self, text, age=0):
        f = self.t / 'logs' / 'latest.log'
        f.write_text(text)
        os.utime(f, (time.time() - age, time.time() - age))

    def check(self):
        script = ROOT / 'tools' / 'builder-server' / 'start-build-server.ps1'
        r = subprocess.run([POWERSHELL, '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', str(script),
                            '-CheckCrashedSince', str(self.since), '-CheckDir', str(self.t)], capture_output=True, text=True, timeout=60)
        self.assertIn(r.returncode, (0, 1), r.stdout + r.stderr)
        return r.returncode, r.stdout

    def test_deliberate_stop_is_clean(self):
        self.log('[12:00:00] [Server thread/INFO]: Stopping the server' + chr(10))
        self.assertEqual(self.check(), (0, 'clean' + chr(10)))

    def test_tick_loop_lines_count_as_a_crash(self):
        for line in ('[16:08:12] [Server thread/ERROR]: Encountered an unexpected exception', '[16:08:12] [Server thread/ERROR]: Exception in server tick loop'):
            with self.subTest(line=line):
                self.log('[16:00:00] Done (3.3s)!' + chr(10) + line + chr(10) + 'java.lang.NullPointerException' + chr(10))
                code, out = self.check()
                self.assertEqual(code, 1)
                self.assertIn('latest.log', out)

    def test_new_crash_report_counts_and_an_old_one_does_not(self):
        self.log('clean' + chr(10))
        reports = self.t / 'crash-reports'
        reports.mkdir()
        old = reports / 'crash-2026-11-01_10.00.00-server.txt'
        old.write_text('old')
        os.utime(old, (self.since - 3600, self.since - 3600))
        self.assertEqual(self.check()[0], 0)
        (reports / 'crash-2026-11-03_16.08.12-server.txt').write_text('new')
        code, out = self.check()
        self.assertEqual(code, 1)
        self.assertIn('crash-2026-11-03_16.08.12-server.txt', out)

    def test_a_previous_runs_log_is_ignored(self):
        self.log('Exception in server tick loop' + chr(10), age=3600)
        self.assertEqual(self.check()[0], 0)

    def test_dry_run_leaves_the_server_folder_alone(self):
        before = sorted(p.name for p in (ROOT / 'tools' / 'builder-server').iterdir())
        self.log('clean' + chr(10))
        self.check()
        self.assertEqual(sorted(p.name for p in (ROOT / 'tools' / 'builder-server').iterdir()), before)

if __name__ == '__main__':
    unittest.main()
