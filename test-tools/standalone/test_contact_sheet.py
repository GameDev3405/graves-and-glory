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
        'largestJumpPerTransition': {'stance-idle-to-run': {'event': 'stance run', 'largestStepDegPerTick': 30.0, 'largestStepFrame': 12, 'largestBoneThere': 'armR',
                                                            'worstBone': 'armR', 'worstBoneDegPerTick': 21.5, 'worstBoneFrame': 12, 'largestSecondDiff': 9.0,
                                                            'ratioToBaseline': 2.0, 'ratioToAuthored': 0.8, 'flagged': False}},
        'hitSweep': [{'cell': 'hs-head-front-2', 'sumDegPerTick': 40.0, 'worstBone': 'head', 'worstBoneDegPerTick': 33.0, 'worstBoneFrame': 24, 'playedClips': ['hit_head']},
                     {'cell': 'hs-body-back-0', 'sumDegPerTick': 10.0, 'worstBone': 'body', 'worstBoneDegPerTick': 5.0, 'worstBoneFrame': 22, 'playedClips': []}],
        'transitions': [{'name': 'novelty-idle', 'frames': 380, 'noveltyFrames': [{'frame': 120, 'kind': 1}], 'playedClips': ['idle', 'idle_novelty']}],
        'clips': [], 'states': [], 'files': [], 'grading': {},
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

    def test_template_reports_worst_bone_hit_sweep_and_novelty(self):
        _, _, _, run_doc, _ = self.run_main(['solo'])
        self.assertIn('armR 21.5 at f012', run_doc)
        self.assertIn('Worst cell: hs-head-front-2, head 33.0 deg/tick at f024', run_doc)
        self.assertIn('novelty seen on 1 frames (first at f120)', run_doc)
        self.assertIn('guard shuffle', run_doc)


if __name__ == '__main__':
    unittest.main()
