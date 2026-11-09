"""Runs `python -m unittest discover -s DIR -p PATTERN` as several child processes at once (verify's python-tests step).

  python tools/parallel_tests.py DIR [--pattern test_*.py] [--jobs N] [--times FILE]

The parent discovers the tests exactly as `unittest discover` would and lists every test id. Each child discovers one test
module the same way (start dir on sys.path, cwd unchanged) and runs its share of that module's ids in discovery order.
The run fails (exit 1) when any test fails or errors, any module fails to import, any child dies, or the ids the children
ran differ from the ids discovered; it exits 5, as unittest does, when nothing is discovered. Unexpected successes fail too.

Splitting is only a schedule: with --times (per-test seconds from earlier runs, rewritten after each run) a slow module is
cut into chunks of about TARGET seconds so the slowest module no longer sets the wall clock. Without it, one child per module."""
import argparse, json, os, pathlib, subprocess, sys, tempfile, time, unittest
from concurrent.futures import ThreadPoolExecutor

TARGET = 8.0     # seconds of tests per chunk, from the times file
UNKNOWN = 0.5    # a test the times file has not seen yet


def walk(suite):
    for t in suite:
        if isinstance(t, unittest.TestSuite): yield from walk(t)
        else: yield t


def module_of(test_id):
    """the discovered module a test id belongs to; an import failure is unittest.loader._FailedTest.<module>"""
    parts = test_id.split('.')
    return parts[-1] if test_id.startswith('unittest.loader.') else parts[0]


def chunks(ids, times):
    """[(module, [ids])] in discovery order: one chunk per module, unless its known times add up to more than TARGET, when
    it is cut into runs of consecutive ids of about TARGET seconds each."""
    by_mod = {}
    for i in ids: by_mod.setdefault(module_of(i), []).append(i)
    out = []
    for mod, mids in by_mod.items():
        cur, acc = [], 0.0
        for i in mids:
            d = times.get(i, UNKNOWN)
            if cur and acc + d > TARGET: out.append((mod, cur)); cur, acc = [], 0.0
            cur.append(i); acc += d
        out.append((mod, cur))
    return out


def schedule(units, times):
    """longest first, so the slow chunks start at once and the short ones fill in round them"""
    return sorted(units, key=lambda u: -sum(times.get(i, UNKNOWN) for i in u[1]))


class _Timed(unittest.TextTestResult):
    def startTest(self, test):
        self._t0 = time.perf_counter(); super().startTest(test)

    def stopTest(self, test):
        super().stopTest(test)
        self.times[test.id()] = time.perf_counter() - self._t0


def child(start, module, ids_file, out_file):
    """discover one module as `unittest discover` does, run the listed ids in discovery order, write what happened"""
    want = json.loads(pathlib.Path(ids_file).read_text(encoding='utf-8'))
    suite = unittest.defaultTestLoader.discover(start, pattern=module + '.py')
    have = list(walk(suite))
    want = set(want); picked = [t for t in have if t.id() in want]
    _Timed.times = {}
    runner = unittest.TextTestRunner(stream=sys.stderr, resultclass=_Timed, verbosity=1)
    res = runner.run(unittest.TestSuite(picked))
    bad = len(res.failures) + len(res.errors) + len(res.unexpectedSuccesses)
    pathlib.Path(out_file).write_text(json.dumps({
        'ran': [t.id() for t in picked], 'run_count': res.testsRun, 'failed': bad, 'skipped': len(res.skipped),
        'times': _Timed.times}), encoding='utf-8')
    return 1 if bad else 0


def run_unit(start, unit, tmp, n):
    mod, ids = unit
    ids_file, out_file = tmp / ('ids%d.json' % n), tmp / ('out%d.json' % n)
    ids_file.write_text(json.dumps(ids), encoding='utf-8')
    t0 = time.time()
    p = subprocess.run([sys.executable, __file__, '--child', start, mod, str(ids_file), str(out_file)],
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    log = p.stdout.decode('utf-8', 'replace')
    try: res = json.loads(out_file.read_text(encoding='utf-8'))
    except (OSError, ValueError): res = None
    return dict(module=mod, ids=ids, code=p.returncode, log=log, result=res, seconds=time.time() - t0)


def run(start, pattern, jobs, times_file=None, out=sys.stdout):
    t0 = time.time()
    discovered = [t.id() for t in walk(unittest.defaultTestLoader.discover(start, pattern=pattern))]
    times = {}
    if times_file:
        try: times = json.loads(pathlib.Path(times_file).read_text(encoding='utf-8'))
        except (OSError, ValueError): times = {}
    units = schedule(chunks(discovered, times), times)
    print('parallel_tests: %d tests in %d modules, %d chunks, %d jobs' % (
        len(discovered), len({module_of(i) for i in discovered}), len(units), jobs), file=out, flush=True)
    if not discovered:
        print('NO TESTS RAN', file=out); return 5
    with tempfile.TemporaryDirectory() as tmp, ThreadPoolExecutor(jobs) as pool:
        done = list(pool.map(lambda a: run_unit(start, a[1], pathlib.Path(tmp), a[0]), enumerate(units)))
    ran, problems, heads, skipped, new_times = [], [], [], 0, dict(times)
    for d in sorted(done, key=lambda d: d['ids'][0]):
        res = d['result']
        ok = d['code'] == 0 and res is not None and res['failed'] == 0 and res['run_count'] == len(res['ran']) == len(d['ids'])
        if res:
            ran += res['ran']; skipped += res['skipped']; new_times.update(res['times'])
        print('%-8s %6.1fs %4d tests  %s' % ('ok' if ok else 'FAILED', d['seconds'], len(d['ids']), d['module']), file=out)
        if not ok:
            problems.append(d['module'])
            print(d['log'], file=out)
            # repeated at the end, where verify's 18-line tail shows them
            heads += [l for l in d['log'].splitlines() if l.startswith(('FAIL:', 'ERROR:', 'UNEXPECTED SUCCESS:'))] or                      ['%s: child exited %s without a full result' % (d['module'], d['code'])]
    if sorted(ran) != sorted(discovered):
        missing, extra = sorted(set(discovered) - set(ran)), sorted(set(ran) - set(discovered))
        problems.append('id mismatch')
        print('parallel_tests: ran ids differ from discovered: missing %s extra %s, %d run vs %d discovered'
              % (missing[:10], extra[:10], len(ran), len(discovered)), file=out)
    if times_file:
        pathlib.Path(times_file).parent.mkdir(parents=True, exist_ok=True)
        keep = {i: round(new_times[i], 3) for i in discovered if i in new_times}
        pathlib.Path(times_file).write_text(json.dumps(keep, indent=0, sort_keys=True), encoding='utf-8')
    print('-' * 70, file=out)
    print('Ran %d tests in %.1fs (%d chunks)' % (len(ran), time.time() - t0, len(units)), file=out)
    if problems:
        for h in heads: print(h, file=out)
        print('FAILED: ' + ', '.join(problems), file=out); return 1
    print('OK' + (' (skipped=%d)' % skipped if skipped else ''), file=out)
    return 0


if __name__ == '__main__':
    if sys.argv[1:2] == ['--child']:
        sys.exit(child(*sys.argv[2:6]))
    p = argparse.ArgumentParser()
    p.add_argument('start'); p.add_argument('--pattern', default='test_*.py')
    p.add_argument('--jobs', type=int, default=max(1, min(12, (os.cpu_count() or 2) // 2)))
    p.add_argument('--times')
    a = p.parse_args()
    sys.exit(run(a.start, a.pattern, a.jobs, a.times))
