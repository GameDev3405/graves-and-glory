"""Checks tools/contact_sheet.py (animation rework Task 15): R82, only a complete motion-grade run (solo, states, loco and transitions)
writes docs/reports/grading/<family>.md; a partial run writes its own grading.md and logs the skip. Pure: no Minecraft, no Pillow."""
import contextlib
import io
import json
import os
import pathlib
import sys
import tempfile
import unittest
from unittest import mock

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / 'tools'))
import contact_sheet


def manifest(parts):
    return {
        'family': 'fixture', 'engineFamily': 'fixture', 'troop': 't', 'rig': 'r', 'stub': True, 'parts': parts,
        'pipeline': 'p', 'camera': {'solo': 's', 'loco': 'l', 'spacing': 'sp', 'high': 'h', 'level': 'lv'},
        'timeline': {'captureFrames': 120, 'fps': 20, 'event1AfterFrame': 10, 'event2AfterFrame': 70, 'stripEvery': 4},
        'checks': {'captureTimingRule': 'at most 2 world steps per capture not exactly 1 tick',
                   'captureTiming': [{'capture': 'stance-idle', 'frames': 120, 'staleFrames': 0, 'offStepCount': 1,
                                      'offSteps': [{'frame': 44, 'worldTicks': 2, 'inDenseWindow': False}]}]},
        'loco': [{'name': 'backpedal', 'speedBlocksPerTick': 0.1, 'plantCount': [1, 0], 'plantedSlideBlocks': [0.01, 0],
                  'largestSinglePlantSlideBlocks': [0.01, 0], 'lowestSoleHeight': [0, 0], 'bodyMovedBlocks': 1.0,
                  'plants': [{'foot': 'right', 'startFrame': 3, 'startServerTick': 3, 'frames': 9, 'slideBlocks': 0.01, 'enginePlantWeightMean': 1.0}]}],
        'largestJumpPerTransition': {}, 'clips': [], 'states': [], 'files': [], 'grading': {},
    }


class RepoDocGate(unittest.TestCase):
    def run_main(self, parts, existing=None):
        with tempfile.TemporaryDirectory() as tmp:
            repo = pathlib.Path(tmp); run = repo / 'test-tools' / 'work' / 'grade' / 'run'; run.mkdir(parents=True)
            (run / 'manifest.json').write_text(json.dumps(manifest(parts)), encoding='utf-8')
            doc = repo / 'docs' / 'reports' / 'grading' / 'fixture.md'
            if existing is not None:
                doc.parent.mkdir(parents=True); doc.write_text(existing, encoding='utf-8')
            out = io.StringIO()
            with mock.patch.dict(os.environ, {'GRAVES_GRADE_NO_PILLOW': '1'}), mock.patch.object(sys, 'argv', ['contact_sheet.py', str(run), '--repo', str(repo)]), contextlib.redirect_stdout(out):
                code = contact_sheet.main()
            m = json.loads((run / 'manifest.json').read_text(encoding='utf-8'))
            return code, out.getvalue(), doc.read_text(encoding='utf-8') if doc.exists() else None, (run / 'grading.md').read_text(encoding='utf-8'), m

    def test_partial_run_leaves_repo_doc_alone(self):
        code, log, doc, run_doc, m = self.run_main(['solo', 'states'], existing='kept as it was\n')
        self.assertEqual(code, 3)
        self.assertEqual(doc, 'kept as it was\n')
        self.assertIn('skipped: partial run', log)
        self.assertTrue(m['repoTemplate']['written'].startswith('skipped'))
        self.assertIn('# Motion grading: fixture', run_doc)

    def test_partial_run_creates_no_repo_doc(self):
        _, _, doc, _, _ = self.run_main(['loco'])
        self.assertIsNone(doc)

    def test_complete_run_writes_repo_doc(self):
        _, log, doc, run_doc, m = self.run_main(['solo', 'loco', 'states', 'transitions'])
        self.assertEqual(doc, run_doc)
        self.assertEqual(m['repoTemplate']['written'], 'written whole')
        self.assertIn('template written whole', log)

    def test_template_states_bounds_and_plants(self):
        _, _, _, run_doc, _ = self.run_main(['solo'])
        self.assertIn('at most 2 world steps per capture not exactly 1 tick', run_doc)
        self.assertIn('stance-idle f044 2 ticks', run_doc)
        self.assertIn('0.100 (assumed)', run_doc)
        self.assertIn('f003 (t3) 9 fr 0.010', run_doc)


if __name__ == '__main__':
    unittest.main()
