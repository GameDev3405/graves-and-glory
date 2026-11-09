"""Claude hook that enforces Kort's model ban: never a Fable model, in the main session or any subagent.

PreToolUse (matcher "Agent|Task|Workflow"): denies a subagent call whose `model` names Fable, a subagent type
whose agent file (.claude/agents/<type>.md, project or user) sets a Fable model, and a workflow script that
sets a Fable model as a literal. SessionStart: warns when the session itself starts on a Fable model (a hook
cannot switch the model). Known gaps: a mid-session `/model` switch fires no SessionStart, and subagents with no
`model` inherit the session's model; a workflow that sets the model through a variable is not seen.
Install: merge the entries of tools/claude-hooks.json into the project's .claude/settings.json (SETUP.md).
"""
import json, os, pathlib, re, sys

REASON = ("Kort's rule (WORKFLOW.md): never use a Fable model, in the main session or any subagent. "
          "Use Sonnet 5.5 high for routine work and reviews, Opus 5.5 medium for complicated work, Opus 5.5 high when stuck.")
WORKFLOW_MODEL = re.compile(r"""['"]?model['"]?\s*[:=]\s*['"`]([^'"`]*fable[^'"`]*)['"`]""", re.I)
FRONT_MODEL = re.compile(r'^model\s*:\s*["\']?([^"\'\r\n]+)', re.I | re.M)

def banned(name): return bool(name) and 'fable' in str(name).lower()

def agent_file_model(kind, payload):
    """The `model:` in the front matter of the agent file a subagent_type names, if any."""
    if not kind or not re.fullmatch(r'[\w.-]+', str(kind)): return None
    roots = [os.environ.get('CLAUDE_PROJECT_DIR'), payload.get('cwd'), str(pathlib.Path.home())]
    for root in filter(None, roots):
        f = pathlib.Path(root)/'.claude'/'agents'/f'{kind}.md'
        try: text = f.read_text(encoding='utf-8', errors='replace')
        except OSError: continue
        front = text.split('---')[1] if text.startswith('---') and text.count('---') >= 2 else ''
        m = FRONT_MODEL.search(front)
        if m: return m.group(1).strip()
    return None

def decide(payload):
    event = payload.get('hook_event_name') or ('PreToolUse' if 'tool_name' in payload else '')
    if event == 'SessionStart':
        model = payload.get('model')
        names = [model.get('id'), model.get('display_name')] if isinstance(model, dict) else [model]
        hit = next((n for n in names if banned(n)), None)
        if hit:
            return {'hookSpecificOutput': {'hookEventName': 'SessionStart', 'additionalContext':
                    f'This session runs on {hit}. ' + REASON + ' Do no work; tell the user to restart this session on Opus or Sonnet.'}}
        return None
    if event != 'PreToolUse': return None
    tool, args = payload.get('tool_name', ''), payload.get('tool_input')
    if not isinstance(args, dict): return None
    hit = None
    if tool in ('Agent', 'Task'):
        if banned(args.get('model')): hit = args.get('model')
        elif not args.get('model'):
            declared = agent_file_model(args.get('subagent_type'), payload)
            if banned(declared): hit = declared
    if tool == 'Workflow':
        m = WORKFLOW_MODEL.search(str(args.get('script', '')))
        if m: hit = m.group(1)
    if not hit: return None
    return {'hookSpecificOutput': {'hookEventName': 'PreToolUse', 'permissionDecision': 'deny',
                                   'permissionDecisionReason': f'Blocked model {hit!r}. ' + REASON}}

if __name__ == '__main__':
    try:
        result = decide(json.load(sys.stdin))
        if result: print(json.dumps(result))
    except Exception as exc:  # a broken guard must not strand normal tools; say so on stderr
        print('model guard unavailable: ' + str(exc), file=sys.stderr)
