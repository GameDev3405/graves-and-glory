"""tools/checkq.py: the shared in-game check queue (design note: workflow lead, 2026-11-09).

Lanes do not launch Minecraft clients or balance-sim servers themselves. From the lane's own worktree they submit a job:
  python tools/checkq.py submit client --root <name> [--suites <list>] [--env NAME=VALUE ...]
  python tools/checkq.py submit sim --name <name> [--env NAME=VALUE ...] -- <gradle -P arguments...>
  python tools/checkq.py wait <id> [--timeout S]   # start as a background shell command; exits with the job's exit code
  python tools/checkq.py status [--json]
  python tools/checkq.py cancel <id>
submit records the worktree, branch and HEAD, prints the job id and its result path, and starts the runner if none is alive.
The runner (one detached process, `runner.lock` holds its pid and heartbeat) starts jobs first come, first served while
  - the Minecraft Java processes on the machine (Kort's own game, clients and servers started outside the queue) plus the
    queue's running jobs stay at or below five; each queue job is one slot, a Minecraft process whose command line names the
    worktree of a running queue job is that job's;
  - at most one job runs per worktree (and none in a worktree whose client or server was started outside the queue);
  - a sim gets a port of 25680-25699 that nothing listens on.
It runs tools/ingame_check_run.sh or tools/balance_sim.sh unchanged (the EULA is written only by balance_sim.sh), stops only
the process tree it started (30 minutes, or cancel), never any other process, copies the result and log into the lane's run
root (checkq-result.json, checkq.log) and writes done/<id>/result.json. It exits after 10 idle minutes.
wait exits with the job's exit code; 124 timeout, 125 cancelled, 126 runner error, 3 unknown job (2 stays argparse's usage error).
Queue folder: C:/Users/kortt/Melty/.checkq (CHECKQ_DIR overrides): jobs/ (waiting), running/, done/<id>/.
Test seams (environment): CHECKQ_FAKE_CMD (JSON argv run instead of bash <script>), CHECKQ_FAKE_PROCS (JSON file of
{pid, cmd} in place of the machine's Java processes), CHECKQ_FAKE_PORTS (busy ports) are honoured only when CHECKQ_TEST=1
(`status` then says "test hooks active"); CHECKQ_NO_RUNNER, CHECKQ_POLL, CHECKQ_JOB_TIMEOUT, CHECKQ_IDLE, CHECKQ_STALE (seconds).
"""
import argparse, datetime, json, os, pathlib, re, secrets, shutil, signal, subprocess, sys, time, traceback

SLOTS = 5
PORTS = range(25680, 25700)
DEFAULT_DIR = 'C:/Users/kortt/Melty/.checkq'
SCRIPTS = {'client': 'tools/ingame_check_run.sh', 'sim': 'tools/balance_sim.sh'}
# How a Java process is recognised as Minecraft: tools/verify.py game_guard's client markers, plus server markers.
MC_MARKERS = ('devlaunchinjector', 'net.minecraft.client', '--gamedir', 'net.minecraft.server', 'net.minecraft.bundler',
              'knotclient', 'knotserver', 'fabric-server-launch', 'minecraft_server')
NAME_RE = re.compile(r'^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$')
ENV_RE = re.compile(r'^[A-Z][A-Z0-9_]*$')
EXIT_TIMEOUT, EXIT_CANCELLED, EXIT_ERROR, EXIT_UNKNOWN = 124, 125, 126, 3
NT = os.name == 'nt'
NO_WINDOW = 0x08000000 if NT else 0


def setting(name, default):
    try: return float(os.environ.get('CHECKQ_' + name) or default)
    except ValueError: return default
def job_timeout(): return setting('JOB_TIMEOUT', 1800.0)
def idle_limit(): return setting('IDLE', 600.0)
def stale_after(): return setting('STALE', 120.0)
def poll(): return setting('POLL', 3.0)


def fake(name):
    """A CHECKQ_FAKE_<name> test hook, honoured only under CHECKQ_TEST=1 so a stray variable cannot fool a real queue."""
    return os.environ.get('CHECKQ_FAKE_' + name) if os.environ.get('CHECKQ_TEST') == '1' else None
def test_hooks():
    return [n for n in ('CMD', 'PROCS', 'PORTS') if fake(n) is not None]


def qdir(): return pathlib.Path(os.environ.get('CHECKQ_DIR') or DEFAULT_DIR)
def sub(name):
    p = qdir()/name; p.mkdir(parents=True, exist_ok=True); return p
def stamp(t=None): return datetime.datetime.fromtimestamp(t or time.time()).isoformat(timespec='seconds')
def new_id():
    t = time.time(); return time.strftime('%Y%m%d-%H%M%S', time.localtime(t)) + f'-{int(t*1000)%1000:03d}-' + secrets.token_hex(2)


def write_json(path, data):
    """Atomic: written beside the queue and moved into place, so a reader never sees half a file."""
    path = pathlib.Path(path); path.parent.mkdir(parents=True, exist_ok=True)
    tmp = sub('tmp')/f'{path.name}.{os.getpid()}.{secrets.token_hex(3)}'
    tmp.write_text(json.dumps(data, indent=1), encoding='utf8')
    for _ in range(40):
        try: os.replace(tmp, path); return
        except PermissionError: time.sleep(0.05)  # Windows: a reader holds the file for a moment
    os.replace(tmp, path)


def read_json(path):
    try: return json.loads(pathlib.Path(path).read_text(encoding='utf8'))
    except (OSError, ValueError): return None


def norm(p): return str(p).replace('\\', '/').lower().rstrip('/')
def same_path(a, b): return norm(a) == norm(b)
def path_in(cmd, root):
    """root appears in cmd as a whole path (followed by an end, a separator, ; " or space): game_guard's rule."""
    cmd, root = norm(cmd or ''), norm(root); i = cmd.find(root)
    while i >= 0:
        if i + len(root) == len(cmd) or cmd[i + len(root)] in '/;" ': return True
        i = cmd.find(root, i + 1)
    return False
def native_path(p):
    """Git Bash prints /c/Users/...; Python wants C:/Users/..."""
    m = re.match(r'^/([a-zA-Z])/(.*)$', p)
    return f'{m.group(1).upper()}:/{m.group(2)}' if m else p


# ---- processes and ports -------------------------------------------------------------------------------------------
def java_processes():
    fake_procs = fake('PROCS')
    if fake_procs: rows = read_json(fake_procs) or []
    elif NT:
        shell = shutil.which('pwsh') or shutil.which('powershell')
        if not shell: raise RuntimeError('the slot count needs PowerShell (pwsh or Windows PowerShell)')
        out = subprocess.run([shell, '-NoProfile', '-Command', "@(Get-CimInstance Win32_Process | Where-Object { $_.Name -match "
                              "'^java(w)?\\.exe$' } | Select-Object ProcessId,CommandLine) | ConvertTo-Json -Compress"],
                             capture_output=True, timeout=120, creationflags=NO_WINDOW, check=True).stdout
        rows = json.loads(out or b'[]'); rows = rows if isinstance(rows, list) else [rows]
        rows = [dict(pid=r.get('ProcessId'), cmd=r.get('CommandLine') or '') for r in rows if isinstance(r, dict)]
    else:
        rows = []
        for line in subprocess.run(['ps', '-eo', 'pid=,args='], capture_output=True, text=True).stdout.splitlines():
            parts = line.split(None, 1)
            if len(parts) == 2 and 'java' in parts[1]: rows.append(dict(pid=int(parts[0]), cmd=parts[1]))
    return [r for r in rows if isinstance(r, dict)]


def is_minecraft(cmd): return bool(cmd) and any(m in cmd.lower() for m in MC_MARKERS)
def minecraft_processes(): return [p for p in java_processes() if is_minecraft(p.get('cmd'))]


def pid_alive(pid):
    try: pid = int(pid)
    except (TypeError, ValueError): return False
    if pid <= 0: return False
    if NT:
        import ctypes
        k = ctypes.windll.kernel32
        h = k.OpenProcess(0x1000, False, pid)  # PROCESS_QUERY_LIMITED_INFORMATION
        if not h: return ctypes.GetLastError() == 5  # access denied: it exists
        code = ctypes.c_ulong()
        try: k.GetExitCodeProcess(h, ctypes.byref(code)); return code.value == 259  # STILL_ACTIVE
        finally: k.CloseHandle(h)
    try: os.kill(pid, 0); return True
    except PermissionError: return True
    except OSError: return False


def kill_tree(proc):
    """Only the tree this runner started for one job (as verify.py does on a timeout)."""
    if NT: subprocess.run(['taskkill', '/PID', str(proc.pid), '/T', '/F'], capture_output=True, creationflags=NO_WINDOW)
    else:
        try: os.killpg(proc.pid, signal.SIGKILL)
        except OSError: proc.kill()
    try: proc.wait(timeout=30)
    except subprocess.TimeoutExpired: pass


def busy_ports():
    fake_ports = fake('PORTS')
    if fake_ports is not None: return {int(p) for p in fake_ports.split(',') if p.strip()}
    done = subprocess.run(['netstat', '-ano'] if NT else ['netstat', '-an'], capture_output=True, text=True, errors='replace',
                          creationflags=NO_WINDOW, timeout=60)
    if done.returncode != 0 or not done.stdout.strip(): raise RuntimeError(f'netstat failed (exit {done.returncode})')
    out = done.stdout
    ports = set()
    for line in out.splitlines():
        parts = line.split()
        if 'LISTEN' not in line.upper() or len(parts) < 4: continue
        m = re.search(r'[:.](\d+)$', parts[1] if NT else parts[3])
        if m: ports.add(int(m.group(1)))
    return ports


def bash():
    git_bash = pathlib.Path('C:/Program Files/Git/bin/bash.exe')
    if NT and git_bash.exists(): return str(git_bash)  # not System32's WSL bash
    found = shutil.which('bash')
    if not found: raise RuntimeError('bash not found')
    return found


# ---- runner lock ---------------------------------------------------------------------------------------------------
def lock_live(info, path):
    if info is None:  # unreadable: only stale once old
        try: return time.time() - path.stat().st_mtime <= stale_after()
        except OSError: return False
    if not isinstance(info, dict): return False
    try: beat = float(info.get('heartbeat', 0))
    except (TypeError, ValueError): beat = 0
    return pid_alive(info.get('pid')) and time.time() - beat <= stale_after()


class Lock:
    def __init__(self):
        self.path = qdir()/'runner.lock'; self.token = secrets.token_hex(4); self.lost = False; self.since = time.time()

    def content(self): return dict(pid=os.getpid(), token=self.token, heartbeat=time.time(), started=stamp(self.since))

    def acquire(self):
        sub('tmp')
        for _ in range(5):
            try:
                fd = os.open(self.path, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
                with os.fdopen(fd, 'w', encoding='utf8') as f: json.dump(self.content(), f)
                return True
            except FileExistsError: pass
            info = read_json(self.path)
            if lock_live(info, self.path): return False
            take = qdir()/'runner.takeover'  # one taker of a stale lock at a time
            try: os.close(os.open(take, os.O_CREAT | os.O_EXCL | os.O_WRONLY))
            except FileExistsError:
                try:
                    if time.time() - take.stat().st_mtime > 30: take.unlink()
                except OSError: pass
                time.sleep(0.1); continue
            try:
                # An unreadable lock (info None) is removed only when it is also old: a failed read of a live lock is not stale.
                if read_json(self.path) == info and (info is not None or (self.path.exists() and not lock_live(None, self.path))):
                    try: self.path.unlink()
                    except FileNotFoundError: pass
            finally:
                try: take.unlink()
                except OSError: pass
        return False

    def beat(self):
        if self.lost: return
        if not self.owned(): self.lost = True; return
        write_json(self.path, self.content())

    def owned(self):
        """The lock file names this runner. A read that fails once (a reader racing the atomic move) is retried before 'no'."""
        for attempt in range(2):
            info = read_json(self.path)
            if isinstance(info, dict) and info.get('token') == self.token: return True
            if attempt == 0: time.sleep(0.05)
        return False

    def release(self):
        info = read_json(self.path)
        if isinstance(info, dict) and info.get('token') == self.token:
            try: self.path.unlink()
            except OSError: pass


def runner_info():
    path = qdir()/'runner.lock'
    if not path.exists(): return None, False
    info = read_json(path)
    return info, lock_live(info, path)


def owner_alive(pid):
    """A running/ job's runner is alive only if it is the live lock holder: a bare pid may be reused after a reboot."""
    info, alive = runner_info()
    try: return alive and isinstance(info, dict) and int(info.get('pid')) == int(pid)
    except (TypeError, ValueError): return False


def ensure_runner():
    """Starts a detached runner when none is alive; a second one refuses through the lock anyway."""
    if os.environ.get('CHECKQ_NO_RUNNER'): return False
    if runner_info()[1]: return False
    cmd = [sys.executable, str(pathlib.Path(__file__).resolve()), 'runner']
    out = open(sub('tmp').parent/'runner.out', 'ab')
    try:
        if NT:
            base = 0x00000008 | 0x00000200  # DETACHED_PROCESS | CREATE_NEW_PROCESS_GROUP: no console window
            try: subprocess.Popen(cmd, cwd=qdir(), stdin=subprocess.DEVNULL, stdout=out, stderr=out, close_fds=True,
                                  creationflags=base | 0x01000000)  # CREATE_BREAKAWAY_FROM_JOB, so a closing shell keeps it
            except OSError:
                subprocess.Popen(cmd, cwd=qdir(), stdin=subprocess.DEVNULL, stdout=out, stderr=out, close_fds=True, creationflags=base)
        else:
            subprocess.Popen(cmd, cwd=qdir(), stdin=subprocess.DEVNULL, stdout=out, stderr=out, close_fds=True, start_new_session=True)
    finally: out.close()
    return True


# ---- results -------------------------------------------------------------------------------------------------------
def client_exit(text):
    found = re.findall(r'^exit (\d+)\s*$', text or '', re.M)
    return int(found[-1]) if found else None
def sim_exit(text):
    found = re.findall(r'^gradle exit (\d+) root ', text or '', re.M)
    return int(found[-1]) if found else None
def sim_root(text, name):
    found = re.findall(r'^(\S.*[/\\]work[/\\]method[/\\]' + re.escape(name) + r'-\d{6})\s*$', text or '', re.M)
    return native_path(found[0].replace('\\', '/')) if found else None


def status_of(code):
    return {EXIT_TIMEOUT: 'timeout', EXIT_CANCELLED: 'cancelled', EXIT_ERROR: 'error'}.get(code, 'passed' if code == 0 else 'failed')


def record(jid, job, code, status, note='', log=None, run_root=None, started=None, port=None):
    """Writes done/<id>/result.json last, after the copies into the lane's run root, so wait wakes on a complete result."""
    job = job if isinstance(job, dict) else {}
    d = sub('done')/jid; d.mkdir(parents=True, exist_ok=True)
    result = dict(id=jid, kind=job.get('kind'), worktree=job.get('worktree'), branch=job.get('branch'), sha=job.get('sha'),
                  args=job.get('args'), env=job.get('env'), started=started, ended=stamp(), exit=code, status=status,
                  log=str(log) if log else None, run_root=str(run_root).replace('\\', '/') if run_root else None, port=port, note=note)
    if run_root:
        try:
            rr = pathlib.Path(run_root); rr.mkdir(parents=True, exist_ok=True)
            if log and pathlib.Path(log).exists(): shutil.copyfile(log, rr/'checkq.log')
            write_json(rr/'checkq-result.json', result)
        except OSError as e: result['note'] = (note + '; ' if note else '') + f'could not copy into the run root: {e}'
    if not (d/'job.json').exists(): write_json(d/'job.json', job)
    write_json(d/'result.json', result)
    return result


def validate(job):
    if not isinstance(job, dict): return 'the job file is not a JSON object'
    if job.get('kind') not in SCRIPTS: return f'unknown kind {job.get("kind")!r}'
    if not isinstance(job.get('worktree'), str): return 'no worktree'
    args = job.get('args')
    if not isinstance(args, dict): return 'no args'
    key = 'root' if job['kind'] == 'client' else 'name'
    if not NAME_RE.match(str(args.get(key, ''))): return f'bad {key} {args.get(key)!r}'
    env = job.get('env') or {}
    if not isinstance(env, dict) or any(not ENV_RE.match(str(k)) or not isinstance(v, str) for k, v in env.items()): return 'bad env'
    if job['kind'] == 'sim' and not all(isinstance(a, str) for a in args.get('gradle_args', [])): return 'bad gradle args'
    return None


# ---- runner --------------------------------------------------------------------------------------------------------
class Runner:
    def __init__(self):
        self.jobs = {}  # id -> dict(job, proc, log, started_ts, port)
        self.lock = Lock()
        self.idle_since = time.time()

    def say(self, msg):
        try:
            with open(sub('tmp').parent/'runner.log', 'a', encoding='utf8') as f: f.write(f'{stamp()} [{os.getpid()}] {msg}\n')
        except OSError: pass

    def foreign(self):
        """running/ jobs of another live runner (one that lost and regained nothing): they hold slots and worktrees too."""
        out = []
        for p in sub('running').glob('*.json'):
            if p.stem in self.jobs: continue
            info = read_json(p)
            if isinstance(info, dict) and info.get('runner_pid') != os.getpid() and owner_alive(info.get('runner_pid')): out.append(info)
        return out

    def adopt(self):
        """Jobs left in running/ by a runner that died: recorded as errors; their processes are left alone."""
        for p in sorted(sub('running').glob('*.json')):
            info = read_json(p)
            if isinstance(info, dict) and info.get('runner_pid') != os.getpid() and owner_alive(info.get('runner_pid')): continue
            if p.stem in self.jobs: continue
            jid = p.stem
            try:
                log = sub('running')/f'{jid}.log'; d = sub('done')/jid; d.mkdir(parents=True, exist_ok=True)
                if log.exists(): os.replace(log, d/'log')
                record(jid, info, EXIT_ERROR, 'error', note='the runner that started this job stopped; its processes were left alone',
                       log=d/'log' if (d/'log').exists() else None, started=(info or {}).get('started') if isinstance(info, dict) else None)
                p.unlink(); (sub('running')/f'{jid}.cancel').unlink(missing_ok=True)
                self.say(f'{jid}: orphaned job recorded as error')
            except Exception: self.say(traceback.format_exc())

    def tick(self):
        self.lock.beat()
        for jid in list(self.jobs):
            r = self.jobs[jid]
            try:
                if (sub('running')/f'{jid}.cancel').exists():
                    kill_tree(r['proc']); self.finish(jid, EXIT_CANCELLED, 'cancel requested; only this job\'s own process tree was stopped')
                elif r['proc'].poll() is not None: self.finish(jid)
                elif time.time() - r['started_ts'] > job_timeout():
                    kill_tree(r['proc']); self.finish(jid, EXIT_TIMEOUT, f'over {job_timeout():.0f} s; only this job\'s own process tree was stopped')
            except Exception:
                self.say(traceback.format_exc())
                if jid in self.jobs and self.jobs[jid]['proc'].poll() is not None:  # never leave a waiter hanging
                    r = self.jobs.pop(jid)
                    try:
                        r['log'].close()
                        record(jid, r['job'], EXIT_ERROR, 'error', note='the runner failed while finishing this job; see runner.log',
                               started=r['job'].get('started'), port=r['port'])
                        (sub('running')/f'{jid}.json').unlink(missing_ok=True)
                    except Exception: self.say(traceback.format_exc())
        waiting = sorted(sub('jobs').glob('*.json'))
        if waiting and not self.lock.lost: self.start_jobs(waiting)
        if self.jobs or waiting: self.idle_since = time.time()

    def start_jobs(self, waiting):
        mc = minecraft_processes()
        foreign = self.foreign()
        held = [r['job']['worktree'] for r in self.jobs.values()] + [f.get('worktree', '') for f in foreign]
        other = [p for p in mc if not any(path_in(p.get('cmd'), wt) for wt in held)]
        used = len(other) + len(self.jobs) + len(foreign)
        ports = {r['port'] for r in self.jobs.values()} | {f.get('port') for f in foreign}
        for path in waiting:
            if used >= SLOTS: break
            jid = path.stem
            try:
                job = read_json(path)
                problem = validate(job)
                if problem:
                    d = sub('done')/jid; d.mkdir(parents=True, exist_ok=True)
                    os.replace(path, d/'job.json')
                    record(jid, job, EXIT_ERROR, 'error', note=f'bad job file: {problem}')
                    self.say(f'{jid}: bad job file: {problem}'); continue
                wt = job['worktree']
                if any(same_path(wt, h) for h in held) or any(path_in(p.get('cmd'), wt) for p in other): continue
                port = None
                if job['kind'] == 'sim':
                    try: busy = busy_ports() | ports
                    except Exception as e:  # a transient netstat failure is not the job's fault: try again next tick
                        self.say(f'{jid}: could not list ports, retrying next tick: {e}'); continue
                    port = next((p for p in PORTS if p not in busy), None)
                    if port is None: continue
                if not self.lock.owned(): self.lock.lost = True; return  # another runner took over: start nothing
                claimed = sub('running')/f'{jid}.json'
                try: os.replace(path, claimed)
                except OSError: continue  # cancelled meanwhile
                try: self.launch(jid, job, port)
                except Exception as e:
                    record(jid, job, EXIT_ERROR, 'error', note=f'could not start: {e}')
                    claimed.unlink(missing_ok=True); self.say(f'{jid}: could not start: {e}'); continue
                used += 1; held.append(wt); ports.add(port)
            except Exception:
                self.say(traceback.format_exc())
                try:  # never let one job file stop the queue
                    if path.exists():
                        d = sub('done')/jid; d.mkdir(parents=True, exist_ok=True); os.replace(path, d/'job.json')
                        record(jid, None, EXIT_ERROR, 'error', note='the runner could not handle this job file')
                except Exception: self.say(traceback.format_exc())

    def launch(self, jid, job, port):
        wt = pathlib.Path(job['worktree'])
        if not wt.is_dir(): raise RuntimeError(f'worktree {wt} is gone')
        args = job['args']
        tail = [args['root'], args.get('suites', '')] if job['kind'] == 'client' else [args['name'], str(port), *args.get('gradle_args', [])]
        fake_cmd = fake('CMD')
        if fake_cmd: cmd = json.loads(fake_cmd) + [job['kind']] + tail
        else:
            if not (wt/SCRIPTS[job['kind']]).exists(): raise RuntimeError(f'{SCRIPTS[job["kind"]]} missing in {wt}')
            cmd = [bash(), SCRIPTS[job['kind']]] + tail
        # The runner's environment came from whichever lane started it: drop its harness settings, use only the job's.
        env = {k: v for k, v in os.environ.items() if not k.startswith(('GRAVES_', 'SIM_'))}
        env.update(job.get('env') or {})
        log = open(sub('running')/f'{jid}.log', 'wb')
        try:
            flags = dict(creationflags=NO_WINDOW | 0x00000200) if NT else dict(start_new_session=True)
            proc = subprocess.Popen(cmd, cwd=wt, env=env, stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, **flags)
        except Exception: log.close(); raise
        started = time.time()
        info = dict(job, id=jid, started=stamp(started), port=port, runner_pid=os.getpid(), job_pid=proc.pid)
        self.jobs[jid] = dict(job=info, proc=proc, log=log, started_ts=started, port=port)  # tracked before anything can fail
        try: write_json(sub('running')/f'{jid}.json', info)
        except Exception:
            self.jobs.pop(jid, None); kill_tree(proc); log.close(); raise
        self.say(f'{jid}: started {job["kind"]} in {wt} (pid {proc.pid}{", port " + str(port) if port else ""})')

    def finish(self, jid, code=None, note=''):
        r = self.jobs.pop(jid); job = r['job']; r['log'].close()
        d = sub('done')/jid; d.mkdir(parents=True, exist_ok=True)
        log = d/'log'
        for _ in range(40):
            try: os.replace(sub('running')/f'{jid}.log', log); break
            except FileNotFoundError: break
            except PermissionError: time.sleep(0.05)
        text = log.read_text(encoding='utf8', errors='replace') if log.exists() else ''
        wt = pathlib.Path(job['worktree'])
        if job['kind'] == 'client':
            run_root = wt/'work'/'ingame-check'/job['args']['root']
            glog = run_root/'gradle.log'
            try: stale_log = glog.exists() and glog.stat().st_mtime < r['started_ts'] - 2
            except OSError: stale_log = False
            if stale_log:  # left by an earlier run with the same --root: its exit line is not this job's
                parsed = None
                if code is None: note = 'gradle.log is older than this job (a stale exit line from an earlier run was ignored)'
            else:
                gtext = glog.read_text(encoding='utf8', errors='replace') if glog.exists() else ''
                if glog.exists(): shutil.copyfile(glog, d/'gradle.log')
                parsed = client_exit(gtext)
        else:
            run_root = sim_root(text, job['args']['name'])
            parsed = sim_exit(text)
        if code is None:
            rc = r['proc'].returncode
            if parsed is not None: code = parsed
            elif rc: code = rc if 0 < rc < 256 else 1
            else: code, note = EXIT_ERROR, note or 'the script ended without an exit line'
        record(jid, job, code, status_of(code), note=note, log=log if log.exists() else None, run_root=run_root,
               started=job.get('started'), port=r['port'])
        (sub('running')/f'{jid}.json').unlink(missing_ok=True); (sub('running')/f'{jid}.cancel').unlink(missing_ok=True)
        self.say(f'{jid}: {status_of(code)} (exit {code})')

    def run(self):
        if not self.lock.acquire():
            print('a check-queue runner is already alive; this one exits'); return 3
        self.say('runner up')
        try:
            self.adopt()
            while True:
                try: self.tick()
                except Exception: self.say(traceback.format_exc())
                if self.lock.lost and not self.jobs: break
                if not self.jobs and time.time() - self.idle_since > idle_limit(): break
                time.sleep(poll())
        finally:
            for jid in list(self.jobs):  # only on a crash: stop what this runner started
                try: kill_tree(self.jobs[jid]['proc']); self.finish(jid, EXIT_ERROR, 'the runner stopped')
                except Exception: pass
            self.lock.release(); self.say('runner down')
        if any(sub('jobs').glob('*.json')): ensure_runner()  # a job arrived as this runner left
        return 0


# ---- commands ------------------------------------------------------------------------------------------------------
def submit_job(cwd, kind, root=None, suites='', name=None, gradle_args=(), env=None):
    env = dict(env or {})
    if kind == 'client' and not NAME_RE.match(root or ''): sys.exit(f'bad --root {root!r}: letters, digits, . _ - only')
    if kind == 'sim' and not NAME_RE.match(name or ''): sys.exit(f'bad --name {name!r}: letters, digits, . _ - only')
    for k in env:
        if not ENV_RE.match(k): sys.exit(f'bad --env name {k!r}: upper-case letters, digits and _ only')
    try:
        out = subprocess.run(['git', 'rev-parse', '--show-toplevel', 'HEAD', '--abbrev-ref', 'HEAD'], cwd=cwd,
                             capture_output=True, text=True, check=True).stdout.split('\n')
    except (subprocess.CalledProcessError, OSError): sys.exit('submit from inside a lane worktree (a git checkout)')
    worktree, sha, branch = out[0].strip(), out[1].strip(), out[2].strip()
    if not (pathlib.Path(worktree)/SCRIPTS[kind]).exists(): sys.exit(f'{SCRIPTS[kind]} is missing in {worktree}')
    jid = new_id()
    args = dict(root=root, suites=suites or '') if kind == 'client' else dict(name=name, gradle_args=list(gradle_args))
    job = dict(id=jid, kind=kind, worktree=worktree, branch=branch, sha=sha, args=args, env=env, submitted=stamp())
    write_json(sub('jobs')/f'{jid}.json', job)
    started = ensure_runner()
    print(jid)
    print(f'result: {sub("done")/jid/"result.json"}')
    print(f'wait:   python tools/checkq.py wait {jid}   (as a background shell command)' + ('; runner started' if started else ''))
    return jid


def wait_job(jid, timeout=None):
    end = time.time() + timeout if timeout else None
    misses, last_check = 0, 0.0
    while True:
        res = read_json(qdir()/'done'/jid/'result.json')
        if isinstance(res, dict):
            print(f'{jid} {res.get("status")} (exit {res.get("exit")}); result {qdir()/"done"/jid/"result.json"}'
                  + (f'; run root {res["run_root"]}' if res.get('run_root') else '') + (f'; {res["note"]}' if res.get('note') else ''))
            try: code = int(res.get('exit'))
            except (TypeError, ValueError): code = EXIT_ERROR
            return code if 0 <= code < 256 else 1
        pending = (qdir()/'jobs'/f'{jid}.json').exists() or (qdir()/'running'/f'{jid}.json').exists()
        misses = 0 if pending or (qdir()/'done'/jid).exists() else misses + 1
        if misses >= 3: print(f'no job {jid} in {qdir()}'); return EXIT_UNKNOWN
        if pending and time.time() - last_check > 30: ensure_runner(); last_check = time.time()
        if end and time.time() >= end: print(f'{jid} still not finished after {timeout:g} s (the job goes on)'); return EXIT_TIMEOUT
        time.sleep(min(poll(), 2.0) if not end else max(0.01, min(poll(), 2.0, end - time.time())))


def cancel_job(jid):
    if not NAME_RE.match(jid): print(f'bad job id {jid!r}'); return 1
    src = qdir()/'jobs'/f'{jid}.json'
    if src.exists():
        d = sub('done')/jid; fresh = not d.exists(); d.mkdir(parents=True, exist_ok=True)
        try: os.replace(src, d/'job.json')
        except FileNotFoundError:
            if fresh:
                try: d.rmdir()
                except OSError: pass
        else:
            record(jid, read_json(d/'job.json'), EXIT_CANCELLED, 'cancelled', note='cancelled before it started')
            print(f'{jid} removed from the queue'); return 0
    if (qdir()/'running'/f'{jid}.json').exists():
        (sub('running')/f'{jid}.cancel').write_text(stamp())
        ensure_runner()
        print(f'{jid} cancel requested; the runner stops only this job\'s own process tree'); return 0
    if (qdir()/'done'/jid/'result.json').exists(): print(f'{jid} has already finished'); return 1
    print(f'no job {jid} in {qdir()}'); return 1


def snapshot():
    def load(p):
        j = read_json(p); return j if isinstance(j, dict) else {'id': p.stem, 'note': 'unreadable job file'}
    waiting = [dict(load(p), id=p.stem) for p in sorted(sub('jobs').glob('*.json'))]
    running = [dict(load(p), id=p.stem) for p in sorted(sub('running').glob('*.json'))]
    recent = []
    for d in sorted((p for p in sub('done').iterdir() if p.is_dir()), reverse=True):
        r = read_json(d/'result.json')
        if isinstance(r, dict): recent.append(r)
        if len(recent) >= 10: break
    info, alive = runner_info()
    try:
        mc = minecraft_processes()
        other = [p for p in mc if not any(path_in(p.get('cmd'), j.get('worktree', '')) for j in running)]
        slots = dict(limit=SLOTS, minecraft_other=len(other), queue_running=len(running), used=len(other) + len(running))
    except Exception as e:
        slots = dict(limit=SLOTS, minecraft_other=None, queue_running=len(running), used=None, error=str(e))
    runner = dict(alive=alive, pid=info.get('pid') if isinstance(info, dict) else None,
                  heartbeat=stamp(info['heartbeat']) if isinstance(info, dict) and isinstance(info.get('heartbeat'), (int, float)) else None)
    return dict(queue=str(qdir()).replace('\\', '/'), runner=runner, slots=slots, waiting=waiting, running=running, recent=recent,
                test_hooks=test_hooks())


def describe(j):
    a = j.get('args') or {}
    what = f'client {a.get("root")} ({a.get("suites") or "no suites"})' if j.get('kind') == 'client' else f'sim {a.get("name")}' if j.get('kind') == 'sim' else 'unreadable job'
    wt = pathlib.Path(j['worktree']).name if isinstance(j.get('worktree'), str) else '?'
    return f'{what} from {wt} ({j.get("branch") or "?"})'


def ago(iso):
    try: s = time.time() - datetime.datetime.fromisoformat(iso).timestamp()
    except (TypeError, ValueError): return '?'
    return f'{s/60:.0f} min' if s >= 90 else f'{s:.0f} s'


def status_text(s):
    lines = [f'Check queue: {s["queue"]}']
    if s.get('test_hooks'): lines.append(f'test hooks active ({", ".join("CHECKQ_FAKE_" + n for n in s["test_hooks"])}); this is not the real machine')
    r = s['runner']
    lines.append(f'Runner: running (pid {r["pid"]}, heartbeat {r["heartbeat"]})' if r['alive'] else 'Runner: not running (submit or wait starts it)')
    sl = s['slots']
    if sl.get('used') is None: lines.append(f'Minecraft processes: could not count ({sl.get("error")})')
    else: lines.append(f'Minecraft slots in use: {sl["used"]} of {sl["limit"]} ({sl["queue_running"]} queue jobs, '
                       f'{sl["minecraft_other"]} other Minecraft processes such as Kort\'s game)')
    lines.append(f'Waiting ({len(s["waiting"])}):')
    lines += [f'  {j["id"]}  {describe(j)}, waiting {ago(j.get("submitted"))}' for j in s['waiting']] or ['  none']
    lines.append(f'Running ({len(s["running"])}):')
    lines += [f'  {j["id"]}  {describe(j)}, running {ago(j.get("started"))}' + (f' on port {j["port"]}' if j.get('port') else '')
              for j in s['running']] or ['  none']
    lines.append(f'Recent ({len(s["recent"])}):')
    lines += [f'  {j["id"]}  {j.get("status")} (exit {j.get("exit")})  {describe(j)}, ended {j.get("ended")}' for j in s['recent']] or ['  none']
    return '\n'.join(lines)


def main(argv=None):
    ap = argparse.ArgumentParser(prog='checkq.py', description='Shared in-game check queue (see the module docstring).')
    cmds = ap.add_subparsers(dest='cmd', required=True)
    s = cmds.add_parser('submit'); kinds = s.add_subparsers(dest='kind', required=True)
    c = kinds.add_parser('client'); c.add_argument('--root', required=True); c.add_argument('--suites', default='')
    c.add_argument('--env', action='append', default=[], metavar='NAME=VALUE')
    m = kinds.add_parser('sim'); m.add_argument('--name', required=True); m.add_argument('--env', action='append', default=[], metavar='NAME=VALUE')
    m.add_argument('gradle_args', nargs=argparse.REMAINDER)
    w = cmds.add_parser('wait', description="Blocks until the job has a result and exits with the job's exit code.",
                        epilog="exit codes: the job's own; 124 timeout (--timeout elapsed; the job goes on), 125 cancelled, "
                               "126 queue error, 3 unknown job id.")
    w.add_argument('id'); w.add_argument('--timeout', type=float, default=0, metavar='S',
                   help='give up after S seconds (exit 124); 0 or omitted waits forever')
    st = cmds.add_parser('status'); st.add_argument('--json', action='store_true')
    x = cmds.add_parser('cancel'); x.add_argument('id')
    cmds.add_parser('runner')
    a = ap.parse_args(argv)
    if a.cmd == 'submit':
        env = {}
        for e in a.env:
            if '=' not in e: ap.error(f'--env {e!r}: expected NAME=VALUE')
            k, v = e.split('=', 1); env[k] = v
        if a.kind == 'client': submit_job(os.getcwd(), 'client', root=a.root, suites=a.suites, env=env)
        else:
            gradle = a.gradle_args[1:] if a.gradle_args[:1] == ['--'] else a.gradle_args
            submit_job(os.getcwd(), 'sim', name=a.name, gradle_args=gradle, env=env)
        return 0
    if a.cmd == 'wait': return wait_job(a.id, a.timeout)
    if a.cmd == 'status':
        snap = snapshot()
        print(json.dumps(snap, indent=1) if a.json else status_text(snap)); return 0
    if a.cmd == 'cancel': return cancel_job(a.id)
    return Runner().run()


if __name__ == '__main__':
    sys.exit(main())
