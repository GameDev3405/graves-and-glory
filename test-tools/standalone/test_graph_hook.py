"""tools/graph_hook.py: the advisory names the right checkout and stays quiet when that checkout's graph is fresh."""
import hashlib, json, pathlib, subprocess, sys, tempfile, unittest
TOOLS = pathlib.Path(__file__).resolve().parents[2]/'tools'
sys.path.insert(0, str(TOOLS))
import graph_hook, graph_state

def bash(command, cwd): return {'tool_input': {'command': command}, 'cwd': str(cwd)}

class GraphHook(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(); self.addCleanup(self.tmp.cleanup)
        self.root = pathlib.Path(self.tmp.name).resolve()/'lane'
        (self.root/'tools').mkdir(parents=True); (self.root/'tools'/'x.py').write_text('print(1)\n')
        for args in (['init', '-q'], ['add', '.']):
            subprocess.run(['git', *args], cwd=self.root, check=True, capture_output=True)
        self.old_root = graph_state.ROOT

    def tearDown(self): graph_state.ROOT = self.old_root

    def write_graph(self, inputs=None):
        out = self.root/'graphify-out'; out.mkdir(exist_ok=True)
        (out/'graph.json').write_text('{"nodes":[]}')
        graph_state.ROOT = self.root
        (out/'source-state.json').write_text(json.dumps(dict(inputs=graph_state.inputs() if inputs is None else inputs,
            graph_sha256=hashlib.sha256((out/'graph.json').read_bytes()).hexdigest())))

    def test_non_graph_commands_are_ignored(self):
        self.assertIsNone(graph_hook.advisory(bash('rg graphify', self.root)))

    def test_missing_graph_warns_and_names_the_checkout(self):
        r = graph_hook.advisory(bash('graphify query "x"', self.root))
        self.assertIn(str(self.root), r['hookSpecificOutput']['additionalContext'])

    def test_fresh_graph_is_quiet_from_the_root_and_a_subfolder(self):
        self.write_graph()
        self.assertIsNone(graph_hook.advisory(bash('graphify query "x"', self.root)))
        self.assertIsNone(graph_hook.advisory(bash('graphify query "x"', self.root/'tools')))

    def test_edit_makes_it_stale(self):
        self.write_graph()
        (self.root/'tools'/'x.py').write_text('print(2)\n')
        self.assertIsNotNone(graph_hook.advisory(bash('graphify explain "x"', self.root)))

    def test_graph_argument_and_cd_pick_the_checkout_that_is_read(self):
        self.write_graph()
        elsewhere = self.root.parent  # no graph here
        g = str(self.root/'graphify-out'/'graph.json')
        self.assertIsNone(graph_hook.advisory(bash(f'graphify query "x" --graph "{g}"', elsewhere)))
        self.assertIsNone(graph_hook.advisory(bash(f'cd "{self.root}" && graphify path a b', elsewhere)))
        (self.root/'tools'/'x.py').write_text('print(3)\n')
        self.assertIsNotNone(graph_hook.advisory(bash(f'graphify query "x" --graph "{g}"', elsewhere)))

    def test_git_bash_cd_path(self):
        p = self.root.as_posix()
        if len(p) > 2 and p[1] == ':': p = '/' + p[0].lower() + p[2:]
        self.assertEqual(graph_hook.checkout_root(f'cd {p} && graphify query x', self.root.parent), self.root)

if __name__ == '__main__': unittest.main()
