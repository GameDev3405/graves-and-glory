"""Proposed file ownership for the parallel port lanes: every Java file under src/main/java in exactly one lane, by API use.
Writes work/port/ownership.txt and checks the lanes are disjoint and cover every file."""
import collections, pathlib, re, sys

root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else '.')
src = root / 'src/main/java'
errors = collections.Counter()
census = root / 'work/port/errors-core.txt'
for line in census.read_text(encoding='utf-8').splitlines():
    m = re.match(r'^\s+(\d+)\s+(\S+\.java)$', line)
    if m:
        errors[m.group(2)] = int(m.group(1))

NET = re.compile(r'PlayNetworking|LoginNetworking|ConfigurationNetworking|PacketByteBuf|PacketByteBufs|CustomPayload|PlayChannelEvents')
RENDER = re.compile(r'net\.minecraft\.client\.|DrawContext|VertexConsumer|MatrixStack|RenderSystem|BufferBuilder|RenderLayer|WorldRenderEvents|HudRenderCallback|extends (Screen|ButtonWidget)')
GECKO = re.compile(r'software\.bernie\.geckolib')
CORE = {'GravesAndGlory.java', 'GravesAndGloryClient.java', 'VersionGate.java'}
GENERATED = {'Spec.java'}

lanes = collections.defaultdict(list)
for p in sorted(src.rglob('*.java')):
    rel = p.relative_to(src / 'gg/gravesandglory').as_posix()
    text = p.read_text(encoding='utf-8', errors='replace')
    if rel in GENERATED:
        lane = 'X generated (tools/preflight.py; nobody edits)'
    elif rel in CORE or rel.startswith('net/') or NET.search(text):
        lane = 'A networking'
    elif rel.startswith('mixin/') or GECKO.search(text) or RENDER.search(text):
        lane = 'B client: rendering, mixins, HUD/UI, GeckoLib'
    else:
        lane = 'C items, NBT, entities, registries, world'
    lanes[lane].append(rel)

all_files = [f for fs in lanes.values() for f in fs]
assert len(all_files) == len(set(all_files)), 'a file sits in two lanes'
assert len(all_files) == sum(1 for _ in src.rglob('*.java')), 'a file has no lane'
out = []
for lane in sorted(lanes):
    fs = lanes[lane]
    out.append(f'## {lane}: {len(fs)} files, {sum(errors[f.split("/")[-1]] for f in fs)} current errors')
    out.append('with errors: ' + ', '.join(f'{f} ({errors[f.split("/")[-1]]})' for f in fs if errors[f.split('/')[-1]]))
    out.append('all: ' + ', '.join(fs))
    out.append('')
out.append('## D harness (later): every file under test-tools/java, test-tools/standalone/*.java, test-tools/resources (mixins json), test-tools/qa (ggbot)')
(root / 'work/port/ownership.txt').write_text('\n'.join(out), encoding='utf-8', newline='\n')
for lane in sorted(lanes):
    print(lane, len(lanes[lane]), sum(errors[f.split('/')[-1]] for f in lanes[lane]))
print('disjoint and complete:', len(all_files), 'files')
