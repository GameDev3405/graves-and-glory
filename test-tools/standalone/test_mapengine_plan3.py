"""Checks map engine Plan 3, the AI handoff: notes and brief, export bundle, draft import with locking."""
import copy
import json
import pathlib
import sys
import tempfile
import threading
import unittest
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from mapengine import build as mb  # noqa: E402
from mapengine import cli, design, handoff, server  # noqa: E402


def obj(kind, i, by='kort', state='accepted', **kw):
    return dict(kw, id=f'{kind}-{i}', kind=kind, by=by, state=state)


def base():
    d = design.new_design('t', 'T', 128, 64)
    d['brief'] = 'Grey ruined march, overgrown.'
    d['objects'] = [
        obj('spawn', 1, team='dawn', x=5, z=5),
        obj('spawn', 2, team='dusk', x=120, z=5),
        obj('point', 1, name='A', x=64, z=10, radius=8),
        obj('hill', 1, outline=[[20, 20], [50, 20], [50, 40], [20, 40]], height=8, steepness=1.0),
        obj('note', 1, x=40, z=30, radius=12, text='ruined chapel here, overgrown'),
    ]
    return d


def ai_hill(i, **kw):
    o = obj('hill', i, by='ai', state='proposed', outline=[[70, 30], [90, 30], [90, 50], [70, 50]], height=5, steepness=0.5)
    o.update(kw)
    return o


class SchemaTest(unittest.TestCase):
    def test_note_schema(self):
        self.assertEqual(design.validate(base()), [])
        d = base()
        d['objects'][4]['text'] = ''
        self.assertTrue(any('note-1' in p and 'text' in p for p in design.validate(d)))
        d = base()
        d['objects'][4]['x'] = 500
        self.assertTrue(any('note-1' in p for p in design.validate(d)))
        d = base()
        d['brief'] = 7
        self.assertTrue(any('brief' in p for p in design.validate(d)))
        d = base()
        d['review'] = ['ok', 3]
        self.assertTrue(any('review' in p for p in design.validate(d)))

    def test_locked(self):
        self.assertTrue(design.locked(obj('hill', 1)))
        self.assertTrue(design.locked(obj('hill', 1, by='ai', state='accepted')))
        self.assertFalse(design.locked(obj('hill', 1, by='ai', state='proposed')))

    def test_build_refuses_proposals(self):
        d = base()
        d['objects'].append(ai_hill(2))
        self.assertTrue(any('not reviewed' in p for p in design.build_problems(d)))
        with tempfile.TemporaryDirectory() as tmp, self.assertRaises(mb.BuildError):
            mb.build(d, tmp)
        d['objects'][-1]['state'] = 'accepted'
        self.assertEqual(design.build_problems(d), [])


class ExportTest(unittest.TestCase):
    def test_export_bundle(self):
        d = base()
        d['objects'].append(ai_hill(2))
        with tempfile.TemporaryDirectory() as tmp:
            out = pathlib.Path(tmp) / 'handoff'
            handoff.export(d, out)
            self.assertEqual(json.loads((out / 'design.json').read_text(encoding='utf-8')), d)
            brief = (out / 'BRIEF.md').read_text(encoding='utf-8')
            self.assertIn('Grey ruined march', brief)
            self.assertIn('ruined chapel here, overgrown', brief)
            self.assertIn('draft.json', brief)
            self.assertIn('locked', brief)
            for png in ('top.png', 'notes/note-1.png'):
                data = (out / png).read_bytes()
                self.assertEqual(data[:8], b'\x89PNG\r\n\x1a\n', png)
            w, h = handoff.png_size((out / 'top.png').read_bytes())
            self.assertEqual((w, h), (128 * handoff.SCALE, 64 * handoff.SCALE))

    def test_export_deterministic(self):
        with tempfile.TemporaryDirectory() as tmp:
            a, b = pathlib.Path(tmp) / 'a', pathlib.Path(tmp) / 'b'
            handoff.export(base(), a)
            handoff.export(base(), b)
            for p in sorted(a.rglob('*')):
                if p.is_file():
                    self.assertEqual(p.read_bytes(), (b / p.relative_to(a)).read_bytes(), p.name)

    def test_cli_export_and_import(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            design.save(root / 'maps' / 't' / 'design.json', base())
            self.assertEqual(cli.main(['export', 't'], root=root), 0)
            hand = root / 'work' / 'mapengine' / 't' / 'handoff'
            draft = json.loads((hand / 'design.json').read_text(encoding='utf-8'))
            draft['objects'].append(ai_hill(2, by='kort', state='accepted'))
            (hand / 'draft.json').write_text(json.dumps(draft), encoding='utf-8')
            self.assertEqual(cli.main(['import', 't'], root=root), 0)
            got = design.load(root / 'maps' / 't' / 'design.json')
            h = [o for o in got['objects'] if o['id'] == 'hill-2'][0]
            self.assertEqual((h['by'], h['state']), ('ai', 'proposed'))


class MergeTest(unittest.TestCase):
    def test_import_tags_ai(self):
        draft = base()
        draft['objects'].append(ai_hill(2, by='kort', state='accepted'))
        merged, rep = handoff.merge(base(), draft)
        h = merged['objects'][-1]
        self.assertEqual((h['id'], h['by'], h['state']), ('hill-2', 'ai', 'proposed'))
        self.assertEqual(rep['added'], ['hill-2'])
        self.assertEqual(rep['refused'], [])
        self.assertEqual(design.validate(merged), [])

    def test_locked_change_refused(self):
        draft = base()
        draft['objects'][3]['height'] = 40           # Kort's hill
        draft['objects'][3]['by'] = 'ai'
        draft['objects'][2]['x'] = 70                # Kort's point
        merged, rep = handoff.merge(base(), draft)
        self.assertEqual(merged['objects'], base()['objects'])
        self.assertTrue(any(m.startswith('hill-1: locked') for m in rep['refused']), rep)
        self.assertTrue(any(m.startswith('point-1: locked') for m in rep['refused']), rep)
        self.assertEqual(merged['review'], rep['refused'])

    def test_accepted_ai_object_locked(self):
        cur = base()
        cur['objects'].append(ai_hill(2, state='accepted'))
        draft = copy.deepcopy(cur)
        draft['objects'][-1]['height'] = 30
        merged, rep = handoff.merge(cur, draft)
        self.assertEqual(merged['objects'][-1]['height'], 5)
        self.assertTrue(any(m.startswith('hill-2: locked') for m in rep['refused']))

    def test_locked_delete_refused(self):
        draft = base()
        draft['objects'] = [o for o in draft['objects'] if o['id'] != 'note-1']
        merged, rep = handoff.merge(base(), draft)
        self.assertIn('note-1', [o['id'] for o in merged['objects']])
        self.assertTrue(any(m.startswith('note-1: locked') and 'delete' in m for m in rep['refused']))

    def test_proposal_replaced_and_withdrawn(self):
        cur = base()
        cur['objects'] += [ai_hill(2), ai_hill(3)]
        draft = base()
        draft['objects'].append(ai_hill(2, height=9))
        merged, rep = handoff.merge(cur, draft)
        ids = [o['id'] for o in merged['objects']]
        self.assertNotIn('hill-3', ids)
        self.assertEqual([o for o in merged['objects'] if o['id'] == 'hill-2'][0]['height'], 9)
        self.assertEqual(rep['updated'], ['hill-2'])
        self.assertEqual(rep['withdrawn'], ['hill-3'])

    def test_bad_schema_refused_whole(self):
        draft = base()
        draft['objects'].append(ai_hill(2, height='tall'))
        with self.assertRaises(handoff.DraftError) as e:
            handoff.merge(base(), draft)
        self.assertTrue(any('hill-2' in p for p in e.exception.problems))
        draft = base()
        draft['id'] = 'other'
        with self.assertRaises(handoff.DraftError):
            handoff.merge(base(), draft)
        with self.assertRaises(handoff.DraftError):
            handoff.merge(base(), ['not', 'a', 'design'])

    def test_clash_dropped(self):
        draft = base()  # exported before Kort added point C
        draft['objects'].append(obj('point', 3, by='ai', state='proposed', name='B', x=90, z=40, radius=8))
        draft['objects'].append(obj('point', 4, by='ai', state='proposed', name='C', x=100, z=40, radius=8))
        cur = base()
        cur['objects'].append(obj('point', 5, name='C', x=30, z=50, radius=8))
        merged, rep = handoff.merge(cur, draft)
        ids = [o['id'] for o in merged['objects']]
        self.assertIn('point-3', ids)
        self.assertIn('point-5', ids)
        self.assertNotIn('point-4', ids)
        self.assertEqual(rep['added'], ['point-3'])
        self.assertTrue(any(m.startswith('point-4:') for m in rep['refused']), rep)
        self.assertEqual(design.validate(merged), [])

    def test_top_level_kept(self):
        draft = base()
        draft['brief'] = 'Sunny meadow.'
        draft['name'] = 'Other'
        merged, rep = handoff.merge(base(), draft)
        self.assertEqual((merged['brief'], merged['name']), (base()['brief'], 'T'))
        self.assertTrue(any(m.startswith('brief:') for m in rep['refused']))
        self.assertTrue(any(m.startswith('name:') for m in rep['refused']))


class ServerTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.maps = pathlib.Path(tmp.name) / 'maps'
        self.work = pathlib.Path(tmp.name) / 'work'
        design.save(self.maps / 't' / 'design.json', base())
        self.srv = server.make_server(self.maps, self.work, 0)
        threading.Thread(target=self.srv.serve_forever, daemon=True).start()
        self.addCleanup(self.srv.server_close)
        self.addCleanup(self.srv.shutdown)
        self.base = f'http://127.0.0.1:{self.srv.server_address[1]}'

    def post(self, path, body):
        req = urllib.request.Request(self.base + path, data=json.dumps(body).encode(), method='POST')
        req.add_header('Content-Type', 'application/json')
        try:
            with urllib.request.urlopen(req) as r:
                return r.status, json.loads(r.read())
        except urllib.error.HTTPError as e:
            return e.code, json.loads(e.read())

    def test_server_import_export(self):
        st, body = self.post('/api/design/t/export', {})
        self.assertEqual(st, 200)
        self.assertTrue((self.work / 't' / 'handoff' / 'BRIEF.md').is_file())
        draft = base()
        draft['objects'].append(ai_hill(2))
        draft['objects'][2]['x'] = 70
        st, body = self.post('/api/design/t/import', {'draft': draft})
        self.assertEqual(st, 200, body)
        self.assertEqual(body['added'], ['hill-2'])
        self.assertTrue(body['refused'])
        saved = design.load(self.maps / 't' / 'design.json')
        self.assertEqual(saved['objects'][2]['x'], 64)
        bad = base()
        bad['objects'][0]['team'] = 'noon'
        st, body = self.post('/api/design/t/import', {'draft': bad})
        self.assertEqual(st, 422)
        self.assertTrue(body['errors'])
        # default path: the handoff folder's draft.json
        (self.work / 't' / 'handoff' / 'draft.json').write_text(json.dumps(base()), encoding='utf-8')
        st, body = self.post('/api/design/t/import', {})
        self.assertEqual(st, 200, body)
        self.assertEqual(body['withdrawn'], ['hill-2'])


if __name__ == '__main__':
    unittest.main()
