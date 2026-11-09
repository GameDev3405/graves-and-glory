"""tools/parallel_tests.py, verify's python-tests step: the same tests as `unittest discover`, and any failure still fails."""
import importlib.util, json, pathlib, subprocess, sys, tempfile, textwrap, unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
TOOL = ROOT / 'tools' / 'parallel_tests.py'
spec = importlib.util.spec_from_file_location('parallel_tests', TOOL)
PT = importlib.util.module_from_spec(spec); spec.loader.exec_module(PT)

PASSING = '''
import unittest
class A(unittest.TestCase):
    def test_one(self): pass
    def test_two(self): pass
class B(unittest.TestCase):
    def test_three(self): self.skipTest('skipped on purpose')
'''


class ParallelTests(unittest.TestCase):
    def tree(self, files):
        tmp = tempfile.TemporaryDirectory(); self.addCleanup(tmp.cleanup)
        d = pathlib.Path(tmp.name)
        for name, body in files.items(): (d / name).write_text(textwrap.dedent(body), encoding='utf-8')
        return d

    def run_tool(self, d, *extra):
        p = subprocess.run([sys.executable, str(TOOL), str(d), '--jobs', '3', *extra], capture_output=True, text=True, cwd=d)
        return p.returncode, p.stdout + p.stderr

    def test_chunks_keep_discovery_order_and_cut_slow_modules(self):
        ids = ['m1.A.test_a', 'm1.A.test_b', 'm1.B.test_c', 'm2.C.test_d', 'unittest.loader._FailedTest.m3']
        self.assertEqual(PT.chunks(ids, {}), [('m1', ids[:3]), ('m2', ids[3:4]), ('m3', ids[4:])])
        cut = PT.chunks(ids, {'m1.A.test_a': 6, 'm1.A.test_b': 6, 'm1.B.test_c': 1})
        self.assertEqual(cut[:2], [('m1', ['m1.A.test_a']), ('m1', ['m1.A.test_b', 'm1.B.test_c'])])
        self.assertEqual(sorted(i for _, c in cut for i in c), sorted(ids))   # every id in exactly one chunk
        self.assertEqual(PT.schedule(cut, {'m1.A.test_b': 6, 'm1.B.test_c': 1, 'm1.A.test_a': 6})[0][1], ['m1.A.test_b', 'm1.B.test_c'])

    def test_green_tree_passes_and_runs_every_id(self):
        d = self.tree({'test_p.py': PASSING, 'test_q.py': PASSING.replace('test_one', 'test_four')})
        times = d / 'times.json'
        code, out = self.run_tool(d, '--times', str(times))
        self.assertEqual(code, 0, out)
        self.assertIn('Ran 6 tests', out); self.assertIn('OK (skipped=2)', out)
        self.assertEqual(set(json.loads(times.read_text(encoding='utf-8'))),
                         {'test_p.A.test_one', 'test_p.A.test_two', 'test_p.B.test_three',
                          'test_q.A.test_four', 'test_q.A.test_two', 'test_q.B.test_three'})
        code, out = self.run_tool(d, '--times', str(times))   # with times known, still every id once
        self.assertEqual(code, 0, out); self.assertIn('Ran 6 tests', out)

    def test_a_failing_test_fails_the_run(self):
        d = self.tree({'test_p.py': PASSING, 'test_bad.py': '''
            import unittest
            class C(unittest.TestCase):
                def test_wrong(self): self.assertEqual(1, 2)
            '''})
        code, out = self.run_tool(d)
        self.assertEqual(code, 1, out); self.assertIn('FAILED: test_bad', out); self.assertIn('test_wrong', out)

    def test_an_error_or_unexpected_success_fails_the_run(self):
        d = self.tree({'test_err.py': '''
            import unittest
            class C(unittest.TestCase):
                def test_boom(self): raise KeyError('boom')
                @unittest.expectedFailure
                def test_lucky(self): pass
            '''})
        code, out = self.run_tool(d)
        self.assertEqual(code, 1, out); self.assertIn('boom', out)

    def test_an_import_error_fails_the_run(self):
        d = self.tree({'test_p.py': PASSING, 'test_broken.py': 'import no_such_module_anywhere\n'})
        code, out = self.run_tool(d)
        self.assertEqual(code, 1, out); self.assertIn('no_such_module_anywhere', out)

    def test_a_crashing_child_fails_the_run(self):
        d = self.tree({'test_exit.py': '''
            import os, unittest
            class C(unittest.TestCase):
                def test_exit(self): os._exit(0)
            '''})
        code, out = self.run_tool(d)
        self.assertEqual(code, 1, out)

    def test_the_working_directory_comes_first_on_sys_path_as_with_python_m_unittest(self):
        d = self.tree({'rootonly_helper.py': 'VALUE = 7\n'})
        (d / 'tests').mkdir()
        (d / 'tests' / 'test_path.py').write_text(textwrap.dedent('''
            import os, sys, unittest
            class P(unittest.TestCase):
                def test_cwd_first(self):
                    import rootonly_helper
                    self.assertEqual(rootonly_helper.VALUE, 7)
                    self.assertNotIn(os.path.join(os.getcwd(), 'tools'), sys.path[:2])
                    self.assertEqual(os.path.normcase(sys.path[1]), os.path.normcase(os.getcwd()))   # [0] is the start dir
            '''), encoding='utf-8')
        p = subprocess.run([sys.executable, str(TOOL), 'tests', '--jobs', '1'], capture_output=True, text=True, cwd=d)
        self.assertEqual(p.returncode, 0, p.stdout + p.stderr)

    def test_nothing_discovered_exits_five_like_unittest(self):
        d = self.tree({'test_empty.py': 'x = 1\n'})
        code, out = self.run_tool(d)
        self.assertEqual(code, 5, out)


if __name__ == '__main__': unittest.main()
