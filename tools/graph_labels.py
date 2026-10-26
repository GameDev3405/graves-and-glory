"""Readable local graph labels, matched to reviewed symbol anchors rather than cluster IDs."""
import collections, json, pathlib, re

ROOT = pathlib.Path(__file__).resolve().parents[1]

def words(value):
    value = value.strip('.').removesuffix('()')
    value = re.sub(r'([a-z0-9])([A-Z])', r'\1 \2', value)
    return re.sub(r'[_-]+', ' ', value).strip()

def names_for(graph, records):
    groups = collections.defaultdict(list)
    for node in graph['nodes']:
        groups[node['community']].append(node)
    names = {}
    used = set()
    for community, nodes in sorted(groups.items(), key=lambda row: -len(row[1])):
        ids = {n['id'] for n in nodes}
        matches = [(len(ids.intersection(row['anchors'])), row) for row in records]
        matches = [(count, row) for count, row in matches
                   if count >= min(2, len(row['anchors'])) and row['name'] not in used]
        if matches:
            _, row = max(matches, key=lambda pair: pair[0])
            name = row['name']
        else:
            files = collections.Counter(pathlib.Path(n.get('source_file', '')).stem
                                        for n in nodes if n.get('source_file'))
            stem = files.most_common(1)[0][0] if files else 'Shared Code'
            name = words(stem)
            if name in used:
                symbol = next((words(n.get('label', '')) for n in nodes
                               if n.get('_callable') and words(n.get('label', '')) != name), '')
                name += ' / ' + (symbol or 'Related Symbols')
        base = name
        suffix = 2
        while name in used:
            name = f'{base} ({suffix})'
            suffix += 1
        used.add(name)
        names[community] = name
    return names

def apply_labels(directory, records_path=None):
    directory = pathlib.Path(directory)
    graph_path = directory / 'graph.json'
    graph = json.loads(graph_path.read_text(encoding='utf8'))
    records = json.loads((records_path or ROOT/'docs/graph-labels.json').read_text(encoding='utf8'))['labels']
    names = names_for(graph, records)
    for node in graph['nodes']:
        node['community_name'] = names[node['community']]
    graph_path.write_text(json.dumps(graph, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    (directory/'.graphify_labels.json').write_text(json.dumps(names, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    report = directory/'GRAPH_REPORT.md'
    if report.exists():
        text = report.read_text(encoding='utf8')
        text = re.sub(r'Community (\d+)', lambda m: names.get(int(m[1]), m[0]), text)
        report.write_text(text, encoding='utf8')
    return names

if __name__ == '__main__':
    names = apply_labels(ROOT/'graphify-out')
    print(f'Named {len(names)} code groups from local symbols; no external model calls.')
