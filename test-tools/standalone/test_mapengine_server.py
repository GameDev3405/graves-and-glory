"""Checks tools/mapengine local server: designs, autosave, versions, field, build. Pure: no Minecraft."""
import base64
import json
import pathlib
import sys
import tempfile
import threading
import unittest
import urllib.error
import urllib.request

import numpy as np

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from mapengine import design, server  # noqa: E402
from mapengine.terrain import heightfield  # noqa: E402


def tiny():
    d = design.new_design('t', 'T', 128, 64)
    for i, o in enumerate([{'kind': 'spawn', 'team': 'dawn', 'x': 5, 'z': 5},
                           {'kind': 'spawn', 'team': 'dusk', 'x': 120, 'z': 5},
                           {'kind': 'point', 'name': 'A', 'x': 64, 'z': 10, 'radius': 8},
                           {'kind': 'hill', 'outline': [[20, 20], [50, 20], [50, 40], [20, 40]],
                            'height': 8, 'steepness': 1.0}]):
        d['objects'].append(dict(o, id=f"{o['kind']}-{i + 1}", by='kort', state='accepted'))
    return d


class ServerTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.maps = pathlib.Path(tmp.name) / 'maps'
        self.work = pathlib.Path(tmp.name) / 'work'
        design.save(self.maps / 't' / 'design.json', tiny())
        self.srv = server.make_server(self.maps, self.work, 0)
        threading.Thread(target=self.srv.serve_forever, daemon=True).start()
        self.addCleanup(self.srv.server_close)
        self.addCleanup(self.srv.shutdown)
        self.base = f'http://127.0.0.1:{self.srv.server_address[1]}'

    def call(self, method, path, body=None, raw=None, headers=None):
        data = raw if raw is not None else (None if body is None else json.dumps(body).encode())
        req = urllib.request.Request(self.base + path, data=data, method=method)
        if method != 'GET':
            req.add_header('Content-Type', 'application/json')
        for k, v in (headers or {}).items():
            req.add_header(k, v)
        try:
            with urllib.request.urlopen(req) as r:
                return r.status, r.read(), r.headers
        except urllib.error.HTTPError as e:
            return e.code, e.read(), e.headers

    def js(self, *a, **kw):
        st, b, _ = self.call(*a, **kw)
        return st, json.loads(b)

    def test_put_then_get_roundtrip(self):
        d = tiny()
        d['name'] = 'Renamed'
        self.assertEqual(self.js('PUT', '/api/design/t', d), (200, {'ok': True}))
        st, got = self.js('GET', '/api/design/t')
        self.assertEqual((st, got['name']), (200, 'Renamed'))
        self.assertEqual(self.js('GET', '/api/designs')[1][0]['id'], 't')
        self.assertEqual(self.call('GET', '/api/design/nope')[0], 404)

    def test_put_invalid_400(self):
        d = tiny()
        d['objects'].append({'id': 'x', 'kind': 'nope'})
        st, r = self.js('PUT', '/api/design/t', d)
        self.assertEqual(st, 400)
        self.assertTrue(r['errors'])
        for bad in (b'{not json', b'[1]', b'"s"', b'null'):
            st, r = self.js('PUT', '/api/design/t', raw=bad)
            self.assertEqual(st, 400, bad)
            self.assertTrue(r['errors'])
        d = tiny()
        d['objects'] = [5]
        self.assertEqual(self.js('PUT', '/api/design/t', d)[0], 400)
        d = tiny()
        d['id'] = 'other'
        self.assertEqual(self.js('PUT', '/api/design/t', d)[0], 400)
        self.assertEqual(self.call('PUT', '/api/design/..%2Fx', raw=b'{}')[0], 404)
        self.assertEqual(self.js('GET', '/api/design/t')[1]['id'], 't')

    def test_field_decodes_to_heightfield(self):
        st, f = self.js('GET', '/api/design/t/field')
        self.assertEqual(st, 200)
        ref = heightfield(tiny())
        h = np.frombuffer(base64.b64decode(f['height']), dtype='<i2').reshape(64, 128)
        self.assertTrue((h == ref.height).all())
        self.assertEqual((f['width'], f['depth']), (128, 64))
        self.assertEqual(len(base64.b64decode(f['road'])), 128 * 64)
        self.assertEqual(len(base64.b64decode(f['water'])), 128 * 64 * 2)
        self.assertEqual(len(base64.b64decode(f['zone'])), 128 * 64 * 2)
        self.assertIsInstance(f['zones'], list)

    def test_versions_save_list_restore(self):
        self.assertEqual(self.js('POST', '/api/design/t/versions', {'name': 'v 1'})[0], 200)
        d = tiny()
        d['name'] = 'Changed'
        self.js('PUT', '/api/design/t', d)
        st, lst = self.js('GET', '/api/design/t/versions')
        self.assertEqual((st, [v['name'] for v in lst]), (200, ['v 1']))
        self.assertEqual(self.js('POST', '/api/design/t/restore', {'name': 'v 1'}), (200, {'ok': True}))
        self.assertEqual(self.js('GET', '/api/design/t')[1]['name'], 'T')
        self.assertEqual(self.js('POST', '/api/design/t/restore', {'name': 'zzz'})[0], 404)

    def test_version_name_rejected(self):
        for n in ('../x', '', 'a' * 41, 'a/b', 5):
            self.assertEqual(self.js('POST', '/api/design/t/versions', {'name': n})[0], 400, n)
            self.assertEqual(self.js('POST', '/api/design/t/restore', {'name': n})[0], 400, n)

    def test_static_traversal_404(self):
        self.assertEqual(self.call('GET', '/../design.py')[0], 404)
        self.assertEqual(self.call('GET', '/%2e%2e/design.py')[0], 404)
        self.assertEqual(self.call('GET', '/..%5cdesign.py')[0], 404)
        st, body, h = self.call('GET', '/')
        self.assertEqual(st, 200)
        self.assertIn('text/html', h['Content-Type'])
        self.assertEqual(h['Cache-Control'], 'no-store')

    def test_build_endpoint_reports_issues(self):
        st, r = self.js('POST', '/api/design/t/build')
        self.assertEqual(st, 200)
        self.assertIn('tiles', r['manifest'])
        self.assertIsInstance(r['issues'], list)
        self.assertTrue((self.work / 't' / 'map.json').exists())

    def test_concurrent_puts_leave_a_loadable_design(self):
        bodies = []
        for i in range(10):
            d = tiny()
            d['name'] = f'N{i}'
            bodies.append(d)
        stop = threading.Event()
        reads = []

        def reader():
            while not stop.is_set():
                st, b, _ = self.call('GET', '/api/design/t')
                reads.append((st, b))

        rt = threading.Thread(target=reader)
        rt.start()
        ts = [threading.Thread(target=self.call, args=('PUT', '/api/design/t', d)) for d in bodies]
        for t in ts:
            t.start()
        for t in ts:
            t.join()
        stop.set()
        rt.join()
        got = design.load(self.maps / 't' / 'design.json')
        self.assertIn(got, bodies)
        for st, b in reads:
            self.assertEqual(st, 200)
            json.loads(b)
        self.assertEqual([p.name for p in (self.maps / 't').iterdir()], ['design.json'])

    def test_bad_host_403(self):
        self.assertEqual(self.call('GET', '/api/designs', headers={'Host': 'evil.example'})[0], 403)
        port = self.srv.server_address[1]
        self.assertEqual(self.call('GET', '/api/designs', headers={'Host': f'localhost:{port}'})[0], 200)

    def test_non_json_content_type_415(self):
        req = urllib.request.Request(self.base + '/api/design/t', data=json.dumps(tiny()).encode(), method='PUT')
        req.add_header('Content-Type', 'text/plain')
        with self.assertRaises(urllib.error.HTTPError) as cm:
            urllib.request.urlopen(req)
        self.assertEqual(cm.exception.code, 415)

    def test_content_length_limits(self):
        import socket
        port = self.srv.server_address[1]

        def raw(cl):
            s = socket.create_connection(('127.0.0.1', port), timeout=5)
            s.sendall((f'PUT /api/design/t HTTP/1.0\r\nHost: 127.0.0.1:{port}\r\n'
                       f'Content-Type: application/json\r\n{cl}\r\n\r\n').encode())
            data = s.recv(200)
            s.close()
            return int(data.split()[1])
        self.assertEqual(raw('Content-Length: -5'), 400)
        self.assertEqual(raw('Content-Length: abc'), 400)
        self.assertEqual(raw('Content-Length: 99999999999'), 413)
        self.assertEqual(raw('X-Nothing: 1'), 400)

    def test_corrupt_design_on_disk_422(self):
        (self.maps / 't' / 'design.json').write_text('{"format": 1, "objects": [5]}', encoding='utf-8')
        for m, p in (('GET', '/field'), ('POST', '/build')):
            st, r = self.js(m, '/api/design/t' + p)
            self.assertEqual(st, 422)
            self.assertTrue(r['errors'])

    def test_build_error_400(self):
        d = design.new_design('t', 'T', 128, 64)
        self.js('PUT', '/api/design/t', d)
        st, r = self.js('POST', '/api/design/t/build')
        self.assertEqual(st, 400)
        self.assertTrue(r['errors'])


if __name__ == '__main__':
    unittest.main()
