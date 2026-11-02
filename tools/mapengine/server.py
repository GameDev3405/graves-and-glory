"""Local map engine server: designs, autosave, saved versions, field and build. Stdlib only, 127.0.0.1."""
import base64
import json
import pathlib
import re
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlparse

import numpy as np

from mapengine import build as mbuild
from mapengine import check as mcheck
from mapengine import design
from mapengine import handoff
from mapengine.terrain import heightfield

WEB = pathlib.Path(__file__).resolve().parent / 'web'
ID_RE = re.compile(r'[a-z0-9_-]{1,40}')
NAME_RE = re.compile(r'[A-Za-z0-9 _-]{1,40}')
MAX_BODY = 16 * 1024 * 1024
TYPES = {'.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
         '.css': 'text/css; charset=utf-8', '.json': 'application/json', '.svg': 'image/svg+xml',
         '.png': 'image/png'}


def _b64(arr, dtype):
    return base64.b64encode(np.ascontiguousarray(arr, dtype=dtype).tobytes()).decode('ascii')


class BadLength(Exception):
    pass


def make_server(maps_dir, work_dir, port):
    maps_dir, work_dir = pathlib.Path(maps_dir), pathlib.Path(work_dir)
    write_lock = threading.Lock()
    build_lock = threading.Lock()

    class Handler(BaseHTTPRequestHandler):
        protocol_version = 'HTTP/1.0'

        def log_message(self, *a):
            pass

        def send(self, status, body, ctype='application/json'):
            if not isinstance(body, bytes):
                body = json.dumps(body).encode('utf-8')
            self.send_response(status)
            self.send_header('Content-Type', ctype)
            self.send_header('Content-Length', str(len(body)))
            self.send_header('Cache-Control', 'no-store')
            self.end_headers()
            self.wfile.write(body)

        def err(self, status, *msgs):
            self.send(status, {'errors': list(msgs)})

        def body_json(self):
            """Parsed JSON body, or None if unparseable. Raises BadLength / OverflowError on a bad size."""
            raw = self.headers.get('Content-Length')
            try:
                n = 0 if raw is None else int(raw)
            except ValueError:
                raise BadLength()
            if n < 0:
                raise BadLength()
            if n > MAX_BODY:
                raise OverflowError('too large')
            try:
                return json.loads(self.rfile.read(n).decode('utf-8'))
            except (ValueError, UnicodeDecodeError):
                return None

        def host_ok(self):
            port = self.server.server_address[1]
            return self.headers.get('Host') in (f'127.0.0.1:{port}', f'localhost:{port}')

        def route(self, method):
            try:
                if not self.host_ok():
                    return self.err(403, 'forbidden host')
                ctype = (self.headers.get('Content-Type') or '').lower()
                if method != 'GET' and not ctype.startswith('application/json'):
                    return self.err(415, 'Content-Type must be application/json')
                path = urlparse(self.path).path
                if path.startswith('/api/'):
                    self.api(method, [unquote(p) for p in path[5:].split('/')])
                elif method == 'GET':
                    self.static(unquote(path))
                else:
                    self.err(404, 'not found')
            except design.DesignError as e:
                self.err(422, *e.problems)
            except OverflowError:
                self.err(413, 'request body too large')
            except BadLength:
                self.err(400, 'bad Content-Length')
            except Exception:  # never crash the server on a bad request
                self.err(500, 'server error')

        do_GET = lambda self: self.route('GET')  # noqa: E731
        do_PUT = lambda self: self.route('PUT')  # noqa: E731
        do_POST = lambda self: self.route('POST')  # noqa: E731

        def static(self, path):
            rel = path.lstrip('/') or 'index.html'
            if '\\' in rel or '\x00' in rel or any(p in ('..', '') for p in rel.split('/')):
                return self.err(404, 'not found')
            f = (WEB / rel).resolve()
            if WEB.resolve() not in f.parents or not f.is_file():
                return self.err(404, 'not found')
            self.send(200, f.read_bytes(), TYPES.get(f.suffix, 'application/octet-stream'))

        def api(self, method, parts):
            if parts == ['library'] and method == 'GET':
                from mapengine import library
                return self.send(200, library.load())
            if parts == ['designs'] and method == 'GET':
                rows = []
                for p in maps_dir.glob('*/design.json'):
                    if not ID_RE.fullmatch(p.parent.name):
                        continue
                    try:
                        name = json.loads(p.read_text(encoding='utf-8')).get('name', p.parent.name)
                    except (ValueError, OSError):
                        continue
                    rows.append({'id': p.parent.name, 'name': name, 'mtime': p.stat().st_mtime})
                rows.sort(key=lambda r: -r['mtime'])
                return self.send(200, rows)
            if len(parts) < 2 or parts[0] != 'design' or not ID_RE.fullmatch(parts[1]):
                return self.err(404, 'not found')
            did, rest = parts[1], parts[2:]
            ddir = maps_dir / did
            dpath = ddir / 'design.json'
            if rest == [] and method == 'PUT':
                d = self.body_json()
                problems = design.validate(d)
                if not problems and d.get('id') != did:
                    problems = [f'id must be {did}']
                if problems:
                    return self.err(400, *problems)
                with write_lock:
                    design.save(dpath, d)
                return self.send(200, {'ok': True})
            if not dpath.is_file():
                return self.err(404, 'no such design')
            if rest == [] and method == 'GET':
                with write_lock:
                    data = dpath.read_bytes()
                return self.send(200, data)
            if rest == ['versions'] and method == 'GET':
                rows = [{'name': p.stem, 'mtime': p.stat().st_mtime} for p in (ddir / 'versions').glob('*.json')
                        if NAME_RE.fullmatch(p.stem)]
                rows.sort(key=lambda r: -r['mtime'])
                return self.send(200, rows)
            if rest in (['versions'], ['restore']) and method == 'POST':
                body = self.body_json()
                name = body.get('name') if isinstance(body, dict) else None
                if not isinstance(name, str) or not NAME_RE.fullmatch(name):
                    return self.err(400, 'name must be 1-40 letters, digits, spaces, _ or -')
                vpath = ddir / 'versions' / f'{name}.json'
                if rest == ['versions']:
                    with write_lock:
                        design.write_atomic(vpath, dpath.read_bytes())
                else:
                    if not vpath.is_file():
                        return self.err(404, 'no such version')
                    with write_lock:
                        data = vpath.read_bytes()
                        try:
                            problems = design.validate(json.loads(data.decode('utf-8')))
                        except (ValueError, UnicodeDecodeError):
                            problems = ['version is not valid JSON']
                        if problems:
                            return self.err(400, *problems)
                        design.write_atomic(dpath, data)
                return self.send(200, {'ok': True})
            if rest == ['field'] and method == 'GET':
                with write_lock:
                    d = design.load(dpath)
                f = heightfield(d)
                return self.send(200, {'width': d['width'], 'depth': d['depth'],
                                       'height': _b64(f.height, '<i2'), 'water': _b64(f.water, '<i2'),
                                       'road': _b64(f.road, 'u1'), 'zone': _b64(f.zone, '<i2'),
                                       'zones': f.zones})
            if rest == ['export'] and method == 'POST':
                with write_lock:
                    d = design.load(dpath)
                out = handoff.export(d, work_dir / did / 'handoff')
                return self.send(200, {'path': str(out), 'brief': str(out / 'BRIEF.md'), 'draft': str(out / 'draft.json')})
            if rest == ['import'] and method == 'POST':
                body = self.body_json()
                if not isinstance(body, dict):
                    return self.err(400, 'body must be {"draft": {...}} or {}')
                try:
                    draft = body['draft'] if 'draft' in body else handoff.load_draft(work_dir / did / 'handoff' / 'draft.json')
                    with write_lock:
                        merged, rep = handoff.merge(design.load(dpath), draft)
                        design.save(dpath, merged)
                except handoff.DraftError as e:
                    return self.err(422, *e.problems)
                return self.send(200, rep)
            if rest == ['build'] and method == 'POST':
                if not build_lock.acquire(blocking=False):
                    return self.err(409, 'a build is already running')
                try:
                    with write_lock:
                        d = design.load(dpath)
                    out = work_dir / did
                    try:
                        manifest = mbuild.build(d, out)
                    except mbuild.BuildError as e:
                        return self.err(400, *e.problems)
                    issues = [{'code': i.code, 'object_id': i.object_id, 'message': i.message}
                              for i in mcheck.check(d, out)]
                finally:
                    build_lock.release()
                return self.send(200, {'manifest': manifest, 'issues': issues})
            return self.err(404, 'not found')

    return ThreadingHTTPServer(('127.0.0.1', port), Handler)
