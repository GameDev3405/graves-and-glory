"""Operations helper for the builder server (plan task 2A.1): world backups, retention, offsite copy, log rotation,
version stamp and health check. Standard library only. Never starts, stops or signals any process.

Backups live in <server>/backups/<world>/<yyyyMMdd-HHmmss>[-reason].zip with entries relative to the world folder,
the same layout BuildMode writes, so a restore is the same for both. BuildMode's own prune only touches plain
<stamp>.zip names, so suffixed files (-prestart, -live) are pruned here with the same rule.

Commands: backup, prune, offsite, rotate-logs, stamp, health, preflight.
"""
import argparse
import datetime as dt
import hashlib
import json
import os
import pathlib
import re
import shutil
import socket
import struct
import sys
import time
import zipfile

STAMP_FORMAT = '%Y%m%d-%H%M%S'
NAME_RE = re.compile(r'^(\d{8}-\d{6})(?:-([a-z]+))?\.zip$')
# Since 2026-11-03 the builder server runs one save, gg-build, holding every canvas as a dimension (BuildMode). The per-map
# names remain for the legacy gg-build-<world> folders and their backups.
ALL = 'gg-build'
WORLDS = (ALL, 'camp', 'lobby', 'rivergate', 'ironhold', 'frostmarch', 'hollowwood', 'kingsfield')


def world_folder(world):
    """The save folder of a world: gg-build for the all-worlds save, gg-build-<world> for a legacy one."""
    return ALL if world == ALL else f'gg-build-{world}'
KEEP_ALL_HOURS = 48
KEEP_DAILY_DAYS = 14
OVERLOAD_RE = re.compile(r"Can't keep up! Is the server overloaded\? Running (\d+)ms or (\d+) ticks behind")
LOG_TIME_RE = re.compile(r'^\[(\d\d):(\d\d):(\d\d)\]')


# ---------- backup names and retention (pure) ----------

def backup_name(now, reason=''):
    base = now.strftime(STAMP_FORMAT)
    return f'{base}-{reason}.zip' if reason else f'{base}.zip'


def parse_backup_name(name):
    """Return (datetime, reason) for a backup file name, or None if it is not one of ours."""
    m = NAME_RE.match(name)
    if not m:
        return None
    try:
        return dt.datetime.strptime(m.group(1), STAMP_FORMAT), m.group(2) or ''
    except ValueError:
        return None


def select_prune(names, now, keep_all_hours=KEEP_ALL_HOURS, keep_daily_days=KEEP_DAILY_DAYS):
    """Names to delete. Everything newer than keep_all_hours stays; of older ones the newest of each calendar day
    stays until keep_daily_days old; the rest go. Unparseable names are never returned."""
    parsed = []
    for n in names:
        p = parse_backup_name(n)
        if p:
            parsed.append((p[0], n))
    parsed.sort(reverse=True)
    seen_days, drop = set(), []
    for t, n in parsed:
        age = now - t
        if age < dt.timedelta(hours=keep_all_hours):
            continue
        if age >= dt.timedelta(days=keep_daily_days) or t.date() in seen_days:
            drop.append(n)
        else:
            seen_days.add(t.date())
    return drop


def newest_backup(directory):
    """(datetime, path) of the newest valid backup in a directory, or None."""
    best = None
    d = pathlib.Path(directory)
    if d.is_dir():
        for f in d.iterdir():
            p = parse_backup_name(f.name)
            if p and (best is None or p[0] > best[0]):
                best = (p[0], f)
    return best


def newest_mtime(world_dir):
    """Latest modification time (epoch seconds) of any file in a world, ignoring session.lock; 0 if empty."""
    latest = 0.0
    for root, _dirs, files in os.walk(world_dir):
        for f in files:
            if f == 'session.lock':
                continue
            try:
                latest = max(latest, os.path.getmtime(os.path.join(root, f)))
            except OSError:
                pass
    return latest


def scheduled_due(world_dir, backup_dir, now, interval_minutes):
    """A scheduled backup is needed only if the world changed after the newest backup and that backup is older
    than the interval. Returns (due, why)."""
    mtime = newest_mtime(world_dir)
    if mtime == 0:
        return False, 'world folder has no files'
    last = newest_backup(backup_dir)
    if last is None:
        return True, 'no backup yet'
    if (now - last[0]) < dt.timedelta(minutes=interval_minutes):
        return False, 'newest backup is younger than the interval'
    if mtime <= last[1].stat().st_mtime:  # file time, not the name: the name is truncated to the second
        return False, 'world unchanged since the newest backup'
    return True, 'world changed since the newest backup'


# ---------- backup, offsite, logs ----------

def server_running(server_dir, world):
    """True if the world's session.lock is held open for writing by another process (Windows: cannot be opened exclusively).
    Best effort and read-only; on other platforms falls back to the pid file the supervisor writes."""
    lock = pathlib.Path(server_dir) / world_folder(world) / 'session.lock'
    if not lock.exists():
        return False
    try:
        with open(lock, 'r+b'):
            return False
    except PermissionError:
        return True
    except OSError:
        return False


def make_backup(server_dir, world, reason, now=None, retention=True):
    """Zip the world folder (gg-build, or a legacy gg-build-<world>) into backups/<world>. A temp .part file is moved into place, so a half-written zip never
    carries a real name. Returns the path, or None if the world folder does not exist yet."""
    server_dir = pathlib.Path(server_dir)
    now = now or dt.datetime.now()
    src = server_dir / world_folder(world)
    if not src.is_dir():
        return None
    dest_dir = server_dir / 'backups' / world
    dest_dir.mkdir(parents=True, exist_ok=True)
    target = dest_dir / backup_name(now, reason)
    part = target.with_name(target.name + '.part')
    with zipfile.ZipFile(part, 'w', zipfile.ZIP_DEFLATED) as z:
        for root, _dirs, files in os.walk(src):
            for f in files:
                if f == 'session.lock':
                    continue
                full = pathlib.Path(root) / f
                try:
                    z.write(full, full.relative_to(src).as_posix())
                except (PermissionError, FileNotFoundError):
                    pass  # a file held or removed mid-copy (live backups only); the zip stays valid
    part.replace(target)
    if retention:
        prune_dir(dest_dir, now)
    return target


def prune_dir(directory, now=None):
    now = now or dt.datetime.now()
    d = pathlib.Path(directory)
    dropped = select_prune([f.name for f in d.iterdir()], now)
    for n in dropped:
        (d / n).unlink(missing_ok=True)
    for f in d.glob('*.part'):  # leftovers from a crash during a backup
        if time.time() - f.stat().st_mtime > 3600:
            f.unlink(missing_ok=True)
    return dropped


def offsite_copy(server_dir, offsite_dir, now=None):
    """Copy backups not yet present (matched by name and size) to offsite_dir/<world>/ and apply the same retention there.
    The offsite folder is expected to be a synced or external drive; this never deletes anything outside it."""
    server_dir, offsite_dir = pathlib.Path(server_dir), pathlib.Path(offsite_dir)
    now = now or dt.datetime.now()
    copied = []
    for world_dir in sorted((server_dir / 'backups').glob('*')):
        if not world_dir.is_dir():
            continue
        out = offsite_dir / world_dir.name
        out.mkdir(parents=True, exist_ok=True)
        for f in sorted(world_dir.glob('*.zip')):
            dest = out / f.name
            if parse_backup_name(f.name) and not (dest.exists() and dest.stat().st_size == f.stat().st_size):
                tmp = dest.with_name(dest.name + '.part')
                shutil.copy2(f, tmp)
                tmp.replace(dest)
                copied.append(dest)
        prune_dir(out, now)
    return copied


def select_old_logs(entries, now_epoch, max_age_days):
    """entries: [(name, mtime_epoch)]; names of rotated logs (.log.gz or dated .log) older than max_age_days.
    latest.log, supervisor.log and debug.log are never returned."""
    keep = {'latest.log', 'supervisor.log', 'debug.log'}
    out = []
    for name, mtime in entries:
        if name in keep:
            continue
        if name.endswith(('.log.gz', '.log')) and now_epoch - mtime > max_age_days * 86400:
            out.append(name)
    return out


def rotate_supervisor_log(path, max_bytes=2_000_000, keep=5):
    """supervisor.log -> supervisor.log.1 ... .keep once it passes max_bytes. Returns True if it rotated."""
    path = pathlib.Path(path)
    if not path.exists() or path.stat().st_size < max_bytes:
        return False
    for i in range(keep, 0, -1):
        older = path.with_name(f'{path.name}.{i}')
        if i == keep:
            older.unlink(missing_ok=True)
        newer = path if i == 1 else path.with_name(f'{path.name}.{i - 1}')
        if newer.exists():
            newer.replace(older)
    return True


def rotate_logs(server_dir, max_age_days=14, now_epoch=None):
    log_dir = pathlib.Path(server_dir) / 'logs'
    now_epoch = now_epoch or time.time()
    removed = []
    if log_dir.is_dir():
        rotate_supervisor_log(log_dir / 'supervisor.log')
        entries = [(f.name, f.stat().st_mtime) for f in log_dir.iterdir() if f.is_file()]
        for n in select_old_logs(entries, now_epoch, max_age_days):
            (log_dir / n).unlink(missing_ok=True)
            removed.append(n)
    return removed


# ---------- version stamp ----------

def sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def build_stamp(server_dir, world, now=None):
    """What is installed right now: mod jars and the launcher with hashes, plus world and time. Written at each start."""
    server_dir = pathlib.Path(server_dir)
    jars = {}
    for j in sorted((server_dir / 'mods').glob('*.jar')) if (server_dir / 'mods').is_dir() else []:
        jars[j.name] = {'sha256': sha256(j)[:16], 'bytes': j.stat().st_size}
    launcher = server_dir / 'fabric-server-launch.jar'
    return {
        'world': world,
        'startedAt': (now or dt.datetime.now()).isoformat(timespec='seconds'),
        'launcher': {'sha256': sha256(launcher)[:16]} if launcher.exists() else None,
        'mods': jars,
    }


# ---------- health ----------

def read_properties(path):
    props = {}
    try:
        for line in pathlib.Path(path).read_text(encoding='utf-8', errors='replace').splitlines():
            if line and not line.startswith('#') and '=' in line:
                k, v = line.split('=', 1)
                props[k.strip()] = v.strip()
    except OSError:
        pass
    return props


def _varint(n):
    data = bytearray()
    while True:
        b = n & 127
        n >>= 7
        data.append(b | (128 if n else 0))
        if not n:
            return bytes(data)


def _exact(sock, n):
    data = b''
    while len(data) < n:
        chunk = sock.recv(n - len(data))
        if not chunk:
            raise RuntimeError('connection closed')
        data += chunk
    return data


def _read_varint(sock):
    result = 0
    for i in range(5):
        b = _exact(sock, 1)[0]
        result |= (b & 127) << (i * 7)
        if not b & 128:
            return result
    raise ValueError('bad varint')


def ping_status(host, port, timeout=5.0):
    """Minecraft server-list ping. Returns the status JSON dict or raises OSError/ValueError/RuntimeError."""
    with socket.create_connection((host, port), timeout=timeout) as sock:
        sock.settimeout(timeout)
        h = b'localhost'
        hello = _varint(0) + _varint(763) + _varint(len(h)) + h + struct.pack('>H', port) + _varint(1)
        sock.sendall(_varint(len(hello)) + hello + b'\x01\x00')
        length = _read_varint(sock)
        if length > 1 << 20:
            raise ValueError('status too large')
        if _read_varint(sock) != 0:
            raise ValueError('unexpected packet')
        size = _read_varint(sock)
        return json.loads(_exact(sock, size))


def overload_stats(lines):
    """From server log lines: (count of 'Can't keep up' warnings, worst ms behind). This is the only tick-health signal on
    1.20.1: the /tick command and tick-rate API do not exist before 1.20.3 and the server has no RCON."""
    count, worst = 0, 0
    for line in lines:
        m = OVERLOAD_RE.search(line)
        if m:
            count += 1
            worst = max(worst, int(m.group(1)))
    return count, worst


def recent_log_lines(log_path, max_lines=4000):
    try:
        with open(log_path, 'rb') as f:
            f.seek(0, os.SEEK_END)
            size = f.tell()
            f.seek(max(0, size - 400_000))
            text = f.read().decode('utf-8', errors='replace')
        return text.splitlines()[-max_lines:]
    except OSError:
        return []


def health(server_dir, now=None):
    server_dir = pathlib.Path(server_dir)
    now = now or dt.datetime.now()
    props = read_properties(server_dir / 'server.properties')
    world = ALL
    cw = server_dir / 'current-world.txt'
    if cw.exists():
        world = cw.read_text().strip() or ALL
    port = int(props.get('server-port', '25565') or 25565)
    host = props.get('server-ip') or '127.0.0.1'
    report = {'world': world, 'port': port, 'checkedAt': now.isoformat(timespec='seconds')}
    try:
        st = ping_status(host, port)
        report['up'] = True
        report['version'] = st.get('version', {}).get('name')
        report['players'] = st.get('players', {}).get('online')
        report['maxPlayers'] = st.get('players', {}).get('max')
    except (OSError, ValueError, RuntimeError, json.JSONDecodeError) as e:
        report['up'] = False
        report['error'] = f'{type(e).__name__}: {e}'
    count, worst = overload_stats(recent_log_lines(server_dir / 'logs' / 'latest.log'))
    report['tick'] = {'tps': None, 'mspt': None, 'overloadWarningsInLog': count, 'worstBehindMs': worst,
                      'note': 'TPS/MSPT are not exposed by Minecraft 1.20.1 without RCON; overload warnings are the proxy'}
    last = newest_backup(server_dir / 'backups' / world)
    report['lastBackup'] = {'at': last[0].isoformat(timespec='seconds'), 'file': last[1].name,
                            'ageMinutes': round((now - last[0]).total_seconds() / 60)} if last else None
    stamp = server_dir / 'version-stamp.json'
    if stamp.exists():
        try:
            report['stamp'] = json.loads(stamp.read_text())
        except (OSError, ValueError):
            report['stamp'] = None
    problems = []
    if not report['up']:
        problems.append('server not answering')
    if report['lastBackup'] is None:
        problems.append('no backup for the current world')
    elif report['lastBackup']['ageMinutes'] > 24 * 60:
        problems.append('newest backup is over 24 hours old')
    if count >= 5:
        problems.append(f'{count} overload warnings in the recent log')
    report['problems'] = problems
    report['status'] = 'OK' if not problems else ('DOWN' if not report['up'] else 'WARN')
    return report


def format_health(r):
    lines = [f"status: {r['status']}   world: {r['world']}   port: {r['port']}"]
    if r['up']:
        lines.append(f"up: yes   version: {r.get('version')}   players: {r.get('players')}/{r.get('maxPlayers')}")
    else:
        lines.append(f"up: NO ({r.get('error')})")
    t = r['tick']
    lines.append(f"tps/mspt: n/a ({t['overloadWarningsInLog']} overload warnings in recent log, worst {t['worstBehindMs']} ms behind)")
    b = r['lastBackup']
    lines.append(f"last backup: {b['file']} ({b['ageMinutes']} min ago)" if b else 'last backup: none')
    if r.get('stamp'):
        mods = ', '.join(f"{k} {v['sha256'][:8]}" for k, v in r['stamp'].get('mods', {}).items()) or 'none'
        lines.append(f"started: {r['stamp'].get('startedAt')}   mods: {mods}")
    for p in r['problems']:
        lines.append(f'problem: {p}')
    return '\n'.join(lines)


def preflight(server_dir):
    """Problems that stop a start; the supervisor refuses to loop on these. Never creates or edits eula.txt."""
    server_dir = pathlib.Path(server_dir)
    problems = []
    if not (server_dir / 'fabric-server-launch.jar').exists():
        problems.append('fabric-server-launch.jar is missing')
    eula = server_dir / 'eula.txt'
    if not eula.exists() or not re.search(r'^\s*eula\s*=\s*true\s*$', eula.read_text(errors='replace'), re.M | re.I):
        problems.append('eula.txt is missing or not set to eula=true: the owner must read and accept the Minecraft EULA themselves')
    return problems


# ---------- CLI ----------

def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--server-dir', default=str(pathlib.Path(__file__).resolve().parent))
    sub = ap.add_subparsers(dest='cmd', required=True)
    p = sub.add_parser('backup', help='zip the current world into backups/<world>')
    p.add_argument('--world')
    p.add_argument('--reason', default='manual', choices=['manual', 'prestart', 'scheduled', 'live'])
    p.add_argument('--changed-only', action='store_true', help='skip when the world has not changed since the newest backup (keeps crash loops from filling the disk)')
    p.add_argument('--if-due', type=int, metavar='MINUTES', help='scheduled mode: only when the world changed and the newest backup is older than this')
    sub.add_parser('prune', help='apply retention to every world backup folder')
    p = sub.add_parser('offsite', help='copy new backups to a folder and prune there')
    p.add_argument('--to')
    p = sub.add_parser('rotate-logs')
    p.add_argument('--max-age-days', type=int, default=14)
    p = sub.add_parser('stamp', help='write version-stamp.json')
    p.add_argument('--world')
    sub.add_parser('health')
    sub.add_parser('preflight')
    a = ap.parse_args(argv)
    sd = pathlib.Path(a.server_dir)

    def current():
        if getattr(a, 'world', None):
            return a.world
        cw = sd / 'current-world.txt'
        return cw.read_text().strip() if cw.exists() else ALL

    if a.cmd == 'backup':
        world = current()
        if world not in WORLDS:
            print(f'unknown world {world}', file=sys.stderr)
            return 2
        reason = a.reason
        if a.if_due:
            due, why = scheduled_due(sd / world_folder(world), sd / 'backups' / world, dt.datetime.now(), a.if_due)
            if not due:
                print(f'skip: {why}')
                return 0
            reason = 'live' if server_running(sd, world) else 'scheduled'
        else:
            if reason == 'prestart' and server_running(sd, world):
                print('refusing prestart backup: the world is in use', file=sys.stderr)
                return 2
            if a.changed_only:
                due, why = scheduled_due(sd / world_folder(world), sd / 'backups' / world, dt.datetime.now(), 0)
                if not due:
                    print(f'skip: {why}')
                    return 0
        out = make_backup(sd, world, reason)
        print(f'backup: {out}' if out else f'skip: {world_folder(world)} does not exist yet')
        return 0
    if a.cmd == 'prune':
        for d in sorted((sd / 'backups').glob('*')):
            if d.is_dir():
                for n in prune_dir(d):
                    print(f'pruned {d.name}/{n}')
        return 0
    if a.cmd == 'offsite':
        dest = a.to or os.environ.get('GG_OFFSITE')
        f = sd / 'offsite.txt'
        if not dest and f.exists():
            dest = f.read_text().strip()
        if not dest:
            print('offsite: not configured (use --to, GG_OFFSITE or offsite.txt); skipped')
            return 0
        copied = offsite_copy(sd, dest)
        print(f'offsite: copied {len(copied)} file(s) to {dest}')
        return 0
    if a.cmd == 'rotate-logs':
        removed = rotate_logs(sd, a.max_age_days)
        print(f'logs: removed {len(removed)} old file(s)')
        return 0
    if a.cmd == 'stamp':
        s = build_stamp(sd, current())
        (sd / 'version-stamp.json').write_text(json.dumps(s, indent=2))
        print(json.dumps(s))
        return 0
    if a.cmd == 'health':
        r = health(sd)
        print(format_health(r))
        (sd / 'health.json').write_text(json.dumps(r, indent=2))
        return {'OK': 0, 'WARN': 1, 'DOWN': 2}[r['status']]
    if a.cmd == 'preflight':
        probs = preflight(sd)
        for p_ in probs:
            print(p_)
        return 3 if probs else 0
    return 2


if __name__ == '__main__':
    sys.exit(main())
