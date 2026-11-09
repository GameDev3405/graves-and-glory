"""Claude hook that enforces Kort's model ban: never a Fable model, in the main session or any subagent.

PreToolUse (matcher "Agent|Task|Workflow"): denies a subagent call whose `model` names Fable, and a workflow
script that sets a Fable model. SessionStart: warns when the session itself runs on a Fable model (a hook
cannot switch the model; the warning tells the session to stop and ask for a restart on Opus or Sonnet).
Install: merge the entries of tools/claude-hooks.json into the project's .claude/settings.json (SETUP.md).
"""
import json, re, sys

REASON = ("Kort's rule (WORKFLOW.md): never use a Fable model, in the main session or any subagent. "
          "Use Sonnet 5.5 high for routine work and reviews, Opus 5.5 medium for complicated work, Opus 5.5 high when stuck.")

def banned(name): return bool(name) and 'fable' in str(name).lower()

def decide(payload):
    event = payload.get('hook_event_name') or ('PreToolUse' if 'tool_name' in payload else '')
    if event == 'SessionStart':
        model = payload.get('model')
        model = model.get('id') or model.get('display_name') if isinstance(model, dict) else model
        if banned(model):
            return {'hookSpecificOutput': {'hookEventName': 'SessionStart', 'additionalContext':
                    f'This session runs on {model}. ' + REASON + ' Do no work; tell the user to restart this session on Opus or Sonnet.'}}
        return None
    if event != 'PreToolUse': return None
    tool, args = payload.get('tool_name', ''), payload.get('tool_input') or {}
    hit = None
    if tool in ('Agent', 'Task') and banned(args.get('model')): hit = args.get('model')
    if tool == 'Workflow':
        m = re.search(r"""model\s*[:=]\s*['"`]([^'"`]*fable[^'"`]*)['"`]""", str(args.get('script', '')), re.I)
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
