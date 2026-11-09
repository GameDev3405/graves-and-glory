"""tools/model_guard.py: Kort's Fable ban, enforced as a Claude hook."""
import json, pathlib, subprocess, sys, unittest
TOOLS = pathlib.Path(__file__).resolve().parents[2]/'tools'
sys.path.insert(0, str(TOOLS))
import model_guard

def pre(tool, **args): return {'hook_event_name': 'PreToolUse', 'tool_name': tool, 'tool_input': args}

class ModelGuard(unittest.TestCase):
    def denied(self, payload):
        r = model_guard.decide(payload)
        return bool(r) and r['hookSpecificOutput'].get('permissionDecision') == 'deny'

    def test_fable_subagents_are_denied(self):
        for m in ['fable', 'Fable', 'claude-fable-5-1']:
            self.assertTrue(self.denied(pre('Agent', model=m, prompt='x')), m)
            self.assertTrue(self.denied(pre('Task', model=m)), m)

    def test_allowed_models_and_default_pass(self):
        for args in [dict(model='sonnet'), dict(model='opus'), dict(model='claude-sonnet-5-5'), dict(prompt='no model given')]:
            self.assertIsNone(model_guard.decide(pre('Agent', **args)), args)
        self.assertIsNone(model_guard.decide(pre('Bash', command='echo fable')))

    def test_workflow_scripts_setting_fable_are_denied(self):
        self.assertTrue(self.denied(pre('Workflow', script="agent('x', {model: 'fable', label: 'a'})")))
        self.assertIsNone(model_guard.decide(pre('Workflow', script="agent('write a fable', {model: 'sonnet'})")))

    def test_json_quoted_workflow_key_and_bad_input(self):
        self.assertTrue(self.denied(pre('Workflow', script='agent("x", {"model": "fable"})')))
        self.assertIsNone(model_guard.decide(pre('Workflow', script="const m = pick(); agent('x', {model: m})")))  # known gap, documented
        self.assertIsNone(model_guard.decide({'hook_event_name': 'PreToolUse', 'tool_name': 'Agent', 'tool_input': 'oops'}))

    def test_agent_file_front_matter_is_checked(self):
        import tempfile, os
        with tempfile.TemporaryDirectory() as d:
            agents = pathlib.Path(d)/'.claude'/'agents'; agents.mkdir(parents=True)
            (agents/'teller.md').write_text('---\nname: teller\nmodel: fable\n---\nTells fables.\n')
            (agents/'coder.md').write_text('---\nname: coder\nmodel: sonnet\n---\nWrites a fable parser.\n')
            old = os.environ.pop('CLAUDE_PROJECT_DIR', None)
            try:
                p = lambda **a: {**pre('Agent', **a), 'cwd': d}
                self.assertTrue(self.denied(p(subagent_type='teller', prompt='x')))
                self.assertIsNone(model_guard.decide(p(subagent_type='teller', model='sonnet')))  # explicit model wins
                self.assertIsNone(model_guard.decide(p(subagent_type='coder')))
                self.assertIsNone(model_guard.decide(p(subagent_type='../teller')))
            finally:
                if old is not None: os.environ['CLAUDE_PROJECT_DIR'] = old

    def test_session_on_fable_is_warned(self):
        for model in ['claude-fable-5-1', {'id': 'claude-fable-5-1', 'display_name': 'Fable 5.1'}, {'id': 'x', 'display_name': 'Fable 5.1'}]:
            r = model_guard.decide({'hook_event_name': 'SessionStart', 'model': model})
            self.assertIn('Do no work', r['hookSpecificOutput']['additionalContext'])
        self.assertIsNone(model_guard.decide({'hook_event_name': 'SessionStart', 'model': 'claude-opus-5-5'}))

    def test_hook_process_prints_deny_json_and_survives_bad_input(self):
        run = lambda data: subprocess.run([sys.executable, str(TOOLS/'model_guard.py')], input=data, capture_output=True, text=True)
        out = run(json.dumps(pre('Agent', model='fable')))
        self.assertEqual(out.returncode, 0)
        self.assertEqual(json.loads(out.stdout)['hookSpecificOutput']['permissionDecision'], 'deny')
        bad = run('not json')
        self.assertEqual((bad.returncode, bad.stdout), (0, ''))

    def test_template_command_survives_a_missing_script(self):
        hooks = json.loads((TOOLS/'claude-hooks.json').read_text())['hooks']
        cmds = [h['command'] for e in hooks['PreToolUse'] + hooks['SessionStart'] for h in e['hooks'] if 'model_guard' in h['command']]
        self.assertTrue(cmds and all(c.endswith('|| exit 0') for c in cmds))
        import shutil
        sh = shutil.which('bash') or shutil.which('sh')
        if sh:  # hooks run through a POSIX shell; a missing script must exit 0, not 2 (2 would block the Agent call)
            r = subprocess.run([sh, '-c', f'"{sys.executable}" "/no/such/model_guard.py" || exit 0'], capture_output=True)
            self.assertEqual(r.returncode, 0)

    def test_template_installs_both_hooks(self):
        hooks = json.loads((TOOLS/'claude-hooks.json').read_text())['hooks']
        pre_cmds = [h['command'] for e in hooks['PreToolUse'] if 'Agent' in e['matcher'] for h in e['hooks']]
        self.assertTrue(any('model_guard.py' in c for c in pre_cmds))
        self.assertTrue(any('model_guard.py' in h['command'] for e in hooks['SessionStart'] for h in e['hooks']))

if __name__ == '__main__': unittest.main()
