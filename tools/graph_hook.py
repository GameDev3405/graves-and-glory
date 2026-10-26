"""Advisory Claude hook. Check this checkout only when a graph query is attempted."""
import hashlib,json,pathlib,re,sys
def advisory(payload):
    command=payload.get('tool_input',{}).get('command','')
    if not re.search(r'\bgraphify\s+(query|path|explain|affected)\b',command): return None
    root=pathlib.Path(payload.get('cwd') or pathlib.Path.cwd()).resolve()
    out=root/'graphify-out'; state=out/'source-state.json'; graph=out/'graph.json'
    try:
        saved=json.loads(state.read_text())
        # Import the canonical fingerprint code, but resolve all inputs in the caller's checkout.
        import graph_state
        graph_state.ROOT=root
        fresh=saved['inputs']==graph_state.inputs() and saved['graph_sha256']==hashlib.sha256(graph.read_bytes()).hexdigest()
    except (OSError,ValueError,KeyError): fresh=False
    if fresh: return None
    return {'hookSpecificOutput':{'hookEventName':'PreToolUse','additionalContext':
        'This checkout graph is missing or stale. Use rg/direct source now or refresh the checkout-local AST graph. Do not loop graph queries or substitute another branch graph.'}}
if __name__=='__main__':
    try:
        result=advisory(json.load(sys.stdin))
        if result: print(json.dumps(result))
    except Exception as exc:
        # Never strand normal Claude tools; failures are visible, not silent query loops.
        print('Graph freshness hook unavailable: '+str(exc)+'; use direct source.',file=sys.stderr)
