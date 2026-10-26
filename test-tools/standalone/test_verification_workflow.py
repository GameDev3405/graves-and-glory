"""Regression checks for false passes in the verification/navigation infrastructure."""
import pathlib, sys, tempfile, unittest
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[2]/'tools'))
import verify, graph_hook

class EvidenceChecks(unittest.TestCase):
    def test_partial_or_failed_game_never_passes(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=pathlib.Path(tmp)/'checks.txt'
            for text in ['', 'pass mod-loads\n', 'fail combat-hit\npass p1-combat-suite\n']:
                p.write_text(text)
                with self.assertRaises(RuntimeError): verify.require_game_evidence(p)
            p.write_text('pass mod-loads\npass p1-combat-suite\n')
            verify.require_game_evidence(p)

    def test_missing_graph_is_advisory_only_for_graph_query(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertIsNone(graph_hook.advisory({'cwd':tmp,'tool_input':{'command':'rg Campaign src'}}))
            value=graph_hook.advisory({'cwd':tmp,'tool_input':{'command':'graphify query Campaign'}})
            self.assertIn('stale',value['hookSpecificOutput']['additionalContext'])
            self.assertNotIn('permissionDecision',value['hookSpecificOutput'])

if __name__=='__main__': unittest.main()
