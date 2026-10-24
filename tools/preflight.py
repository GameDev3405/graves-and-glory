import json, pathlib, sys
root = pathlib.Path(__file__).resolve().parents[1]
data = json.loads((root/'sheets/design.json').read_text())
errors=[]
cells=0
# Structure files (Phase 3): gzip NBT with DataVersion 3465, odd width and depth up to 48, height up to 32, a studio script of the same id, ids <group>/<name> in [a-z0-9_/].
import gzip, re, struct
STRUCTURE_DATA_VERSION=3465
def nbt_skip(buf, pos, tag):
    """Position after the payload of one tag of the given type, without reading its value."""
    if tag==1: return pos+1
    if tag==2: return pos+2
    if tag in (3,5): return pos+4
    if tag in (4,6): return pos+8
    if tag==7: return pos+4+struct.unpack_from('>i',buf,pos)[0]
    if tag==8: return pos+2+struct.unpack_from('>H',buf,pos)[0]
    if tag==9:
        item=buf[pos]; count=struct.unpack_from('>i',buf,pos+1)[0]; pos+=5
        for _ in range(count): pos=nbt_skip(buf,pos,item)
        return pos
    if tag==10:
        while buf[pos]!=0:
            name=struct.unpack_from('>H',buf,pos+1)[0]; pos=nbt_skip(buf,pos+3+name,buf[pos])
        return pos+1
    if tag==11: return pos+4+4*struct.unpack_from('>i',buf,pos)[0]
    if tag==12: return pos+4+8*struct.unpack_from('>i',buf,pos)[0]
    raise ValueError(f'unknown NBT tag {tag}')
def structure_header(path):
    """(DataVersion, size) of a gzip structure file: walks the root compound and skips every other tag. A value that is missing or of another type is None."""
    buf=gzip.decompress(path.read_bytes())
    if buf[0]!=10: raise ValueError('the root tag is not a compound')
    pos=3+struct.unpack_from('>H',buf,1)[0]; version=None; size=None
    while buf[pos]!=0:
        tag=buf[pos]; length=struct.unpack_from('>H',buf,pos+1)[0]; name=buf[pos+3:pos+3+length].decode('utf-8'); pos+=3+length
        if name=='DataVersion' and tag==3: version=struct.unpack_from('>i',buf,pos)[0]
        elif name=='size' and tag==9 and buf[pos]==3 and struct.unpack_from('>i',buf,pos+1)[0]==3: size=struct.unpack_from('>3i',buf,pos+5)
        pos=nbt_skip(buf,pos,tag)
    return version,size
studio_scripts='test-tools/resources/data/gravesandglory_studio/functions'
for folder in ('src/main/resources/data/gravesandglory/structures','test-tools/resources/data/gravesandglory/structures'):
    if not (root/folder).is_dir(): continue
    for nbt in sorted((root/folder).rglob('*.nbt')):
        sid=nbt.relative_to(root/folder).with_suffix('').as_posix()
        if not re.fullmatch(r'[a-z0-9_]+(/[a-z0-9_]+)+',sid): errors.append(f'structure {sid}: the id must be <group>/<name> in [a-z0-9_/]')
        if not (root/studio_scripts/(sid+'.mcfunction')).is_file(): errors.append(f'structure {sid}: no script {studio_scripts}/{sid}.mcfunction')
        try: version,size=structure_header(nbt)
        except Exception as e: errors.append(f'structure {sid}: cannot read the NBT ({e})'); continue
        if version!=STRUCTURE_DATA_VERSION: errors.append(f'structure {sid}: DataVersion {version}, expected {STRUCTURE_DATA_VERSION}')
        if size is None: errors.append(f'structure {sid}: size is not a list of three ints'); continue
        width,height,depth=size
        if width%2==0: errors.append(f'structure {sid}: width {width} is even, it must be odd')
        if depth%2==0: errors.append(f'structure {sid}: depth {depth} is even, it must be odd')
        if width>48: errors.append(f'structure {sid}: width {width} is more than 48')
        if depth>48: errors.append(f'structure {sid}: depth {depth} is more than 48')
        if height>32: errors.append(f'structure {sid}: height {height} is more than 32')
for sheet, rows in data.items():
    if not rows: errors.append(f'{sheet}: empty sheet'); continue
    columns=set(rows[0])
    ids=set()
    for row in rows:
        if set(row)!=columns: errors.append(f'{sheet}/{row.get("id")}: inconsistent columns')
        if row.get('id') in ids: errors.append(f'{sheet}: duplicate id')
        ids.add(row.get('id'))
        for col in columns:
            cells+=1
            if col not in row or row[col] is None or row[col]=='': errors.append(f'{sheet}/{row.get("id")}/{col}: unfilled')
factions={r['id'] for r in data['factions']}
troops={r['id']:r for r in data['troops']}
for r in troops.values():
    if r['faction'] not in factions: errors.append(f'{r["id"]}: faction does not resolve')
for f in data['factions']:
    refs=f['troops'].split(',')
    for t in refs:
        if t not in troops or troops[t]['faction']!=f['id']: errors.append(f'{f["id"]}: troop {t} does not resolve')
    if sum(troops[t]['count'] for t in refs if t in troops)!=data['rules'][0]['squadSize']: errors.append('squad count mismatch')
if data['rules'][0]['playersPerTeam']*len(factions)!=data['rules'][0]['maxPlayers']: errors.append('capacity mismatch')
groups={g['id'] for g in data.get('groups',[])}
orders={o['id'] for o in data['orders']}
for t in data['troops']:
    if t.get('group') not in groups: errors.append(t['id']+': group does not resolve')
for row in data.get('commandInterface',[]):
    for group in row['selection'].split(','):
        if group not in groups: errors.append('command selection does not resolve: '+group)
    for order in row['orders'].split(','):
        if order not in orders: errors.append('command order does not resolve: '+order)
for r in data['troops']:
    if r['texture'].startswith('gravesandglory:'):
        texture=root/'src/main/resources/assets/gravesandglory'/r['texture'].split(':',1)[1]
        if not texture.exists(): errors.append(f'{r["id"]}: missing texture')
for r in data['ui']:
    if r['id']=='command':
        formationIds={f['id'] for f in data['formations']}
        for id in r['bottom'].split(','):
            if id not in formationIds: errors.append('UI formation reference missing: '+id)
# Built world (Phase 3): the pieces sheet describes the structure files, campPieces places them, campStations names the people and boards of the camp.
piece_rows={r['id']:r for r in data.get('pieces',[])}
for r in piece_rows.values():
    path=root/'src/main/resources/data/gravesandglory/structures'/(r['id']+'.nbt')
    if not path.is_file(): errors.append(f'pieces/{r["id"]}: no structure file {path.relative_to(root).as_posix()}'); continue
    try: version,size=structure_header(path)
    except Exception as e: errors.append(f'pieces/{r["id"]}: cannot read the NBT ({e})'); continue
    if size is None: errors.append(f'pieces/{r["id"]}: the file has no size'); continue
    if tuple(size)!=(r['width'],r['height'],r['depth']): errors.append(f'pieces/{r["id"]}: the file is {size[0]} wide, {size[1]} high, {size[2]} deep but the row says {r["width"]}, {r["height"]}, {r["depth"]} (width, height, depth)')
if len(data.get('camp',[]))!=1: errors.append('camp: the sheet needs exactly one row')
for r in data.get('campPieces',[]):
    where=f'campPieces/{r["id"]}'
    template=piece_rows.get(r['template'])
    if template is None: errors.append(f'{where}: template {r["template"]} is not a pieces id'); continue
    if r['rotation'] not in (0,90,180,270): errors.append(f'{where}: rotation {r["rotation"]} is not 0, 90, 180 or 270')
    if r['station']!='-' and not re.fullmatch(r'[a-z]+(:[a-z]+)?',r['station']): errors.append(f'{where}: station {r["station"]} is neither - nor [a-z]+(:[a-z]+)?')
    slots=(r['slotX'],r['slotY'],r['slotZ'])
    if slots!=(-1,-1,-1) and not (0<=slots[0]<template['width'] and 0<=slots[1]<template['height'] and 0<=slots[2]<template['depth']): errors.append(f'{where}: slot {slots} is neither -1,-1,-1 nor a cell inside the template ({template["width"]} wide, {template["height"]} high, {template["depth"]} deep)')
for r in data.get('campStations',[]):
    where=f'campStations/{r["id"]}'
    if r['npc'] not in ('-','armorer','cleric','librarian','cartographer','weaponsmith'): errors.append(f'{where}: npc {r["npc"]} is not - or a villager profession of the camp')
    if r['page'] not in ('home','contracts','chapter','match','recruit','friends','army','train'): errors.append(f'{where}: page {r["page"]} is not a camp page')
# Lesson boards (Phase 3): ids unique (the generic check above), and every {key:<id>} marker of a text names a key the HUD lessons and the camp labels know.
LESSON_KEYS={'menu','attack','follow','formation','map','rotate_left','rotate_right','confirm','cancel','use'}
for r in data.get('campLessons',[]):
    where=f'campLessons/{r["id"]}'
    text=r['text']
    if text.count('{key:')!=len(re.findall(r'\{key:[a-z_]+\}',text)): errors.append(f'{where}: a {{key: marker is not of the form {{key:<id>}} in [a-z_]')
    for key in re.findall(r'\{key:([a-z_]+)\}',text):
        if key not in LESSON_KEYS: errors.append(f'{where}: {{key:{key}}} is not one of '+', '.join(sorted(LESSON_KEYS)))
# Landmarks (Phase 3): authored pieces that replace rows of CampaignMaps on one map. Ids unique (the generic check above); the footprints of one map
# (the centre block plus and minus (w-1)/2 and (d-1)/2, w and d swapped at 90 and 270) never share a cell.
spans={}
for r in data.get('landmarks',[]):
    where=f'landmarks/{r["id"]}'
    if r['map'] not in range(5): errors.append(f'{where}: map {r["map"]} is not 0..4')
    template=piece_rows.get(r['template'])
    if template is None: errors.append(f'{where}: template {r["template"]} is not a pieces id')
    if r['rotation'] not in (0,90,180,270): errors.append(f'{where}: rotation {r["rotation"]} is not 0, 90, 180 or 270')
    # 64 and up: Arena.height widens the shoulder of a landmark by 2 a block of rise above 64 (10+2*(elevation-64)), so below 64 it would shrink to a cliff
    if not 64<=r['elevation']<=90: errors.append(f'{where}: elevation {r["elevation"]} is not 64..90')
    if r['replaces']!='-' and not re.fullmatch(r'[a-z0-9_-]+',r['replaces']): errors.append(f'{where}: replaces {r["replaces"]} is neither - nor a row id [a-z0-9_-]+')
    # the points of a map (CampaignMaps.select): A, B and C on maps 0 to 3, A to E on Kingsfield (map 4, the two raised outposts are B and D)
    points=('A','B','C','D','E') if r['map']==4 else ('A','B','C')
    if r['encloses']!='-' and r['encloses'] not in points: errors.append(f'{where}: encloses {r["encloses"]} is neither - nor a point of map {r["map"]} ('+', '.join(points)+')')
    if template is None or r['rotation'] not in (0,90,180,270): continue
    w,d=(template['depth'],template['width']) if r['rotation'] in (90,270) else (template['width'],template['depth'])
    box=(r['x']-(w-1)//2,r['z']-(d-1)//2,r['x']+w-1-(w-1)//2,r['z']+d-1-(d-1)//2)
    for other,obox in spans.get(r['map'],[]):
        if box[0]<=obox[2] and obox[0]<=box[2] and box[1]<=obox[3] and obox[1]<=box[3]: errors.append(f'{where}: its footprint x {box[0]}..{box[2]}, z {box[1]}..{box[3]} overlaps landmarks/{other} on map {r["map"]}')
    spans.setdefault(r['map'],[]).append((r['id'],box))
for r in data.get('landmarkLines',[]):
    where=f'landmarkLines/{r["id"]}'
    if r['template'] not in piece_rows: errors.append(f'{where}: template {r["template"]} is not a pieces id')
    if r['kind'] not in ('cover','sight'): errors.append(f'{where}: kind {r["kind"]} is neither cover nor sight')
# Supply houses (Phase 3 Task 8b): five pieces of one size (supply/<map>), eighteen landmark rows, one supplyHouses row for each. A house's footprint is its landmark row's;
# the yard centre is the piece's local (15,24), 6 blocks from the piece centre toward the front gate (south at rotation 0, north at 180).
SUPPLY_PREFIX={0:'rg',1:'ih',2:'fm',3:'hw',4:'kf'}
SUPPLY_COUNT={0:4,1:4,2:4,3:4,4:2}
def land_box(r):
    template=piece_rows.get(r['template'])
    if template is None or r['rotation'] not in (0,90,180,270): return None
    w,d=(template['depth'],template['width']) if r['rotation'] in (90,270) else (template['width'],template['depth'])
    return (r['x']-(w-1)//2,r['z']-(d-1)//2,r['x']+w-1-(w-1)//2,r['z']+d-1-(d-1)//2)
def box_gap(a,b):
    """Blocks between two footprints: the larger of the x gap and the z gap (0 where the boxes touch or overlap in both)."""
    return max(0,a[0]-b[2]-1,b[0]-a[2]-1,a[1]-b[3]-1,b[1]-a[3]-1)
land_rows={r['id']:r for r in data.get('landmarks',[])}
house_pieces=[r for r in data.get('pieces',[]) if r['id'].startswith('supply/')]
if len({(r['width'],r['depth'],r['height']) for r in house_pieces})>1: errors.append('pieces: the supply/ pieces must share one width, depth and height ('+', '.join(f'{r["id"]} {r["width"]}x{r["depth"]}x{r["height"]}' for r in house_pieces)+')')
house_land=[r for r in data.get('landmarks',[]) if r['template'].startswith('supply/')]
land_order={r['id']:i for i,r in enumerate(data.get('landmarks',[]))}
for h in house_land:
    hbox=land_box(h)
    if hbox is None: continue
    for o in data.get('landmarks',[]):
        obox=land_box(o)
        if o['id']==h['id'] or o['map']!=h['map'] or obox is None: continue
        gap=box_gap(hbox,obox)
        # the other row's shoulder (10+2*(e-64)) plus its flat ring (2): it must not reach into the house
        if gap<12+2*(o['elevation']-64): errors.append(f'landmarks/{h["id"]}: landmarks/{o["id"]} (elevation {o["elevation"]}) is {gap} blocks from this house\'s footprint, under its shoulder of {12+2*(o["elevation"]-64)}')
        # Arena.height blends the rows in sheet order: a row whose flat or shoulder reaches the house must come after it, so the house keeps its own flat
        if gap<12+2*(h['elevation']-64) and land_order[o['id']]<land_order[h['id']]: errors.append(f'landmarks/{h["id"]}: landmarks/{o["id"]} is {gap} blocks from this house\'s footprint (the shoulder is {12+2*(h["elevation"]-64)}) but comes before it in the sheet; the house rows go before every row they could touch')
supply_rows=data.get('supplyHouses',[])
by_map={}
for r in supply_rows:
    where=f'supplyHouses/{r["id"]}'
    by_map.setdefault(r['map'],[]).append(r)
    m=re.fullmatch(r'([a-z]+)-supply-(nw|sw|ne|se)',r['id'])
    if r['map'] not in range(5): errors.append(f'{where}: map {r["map"]} is not 0..4')
    elif not m or m.group(1)!=SUPPLY_PREFIX[r['map']]: errors.append(f'{where}: the id must be {SUPPLY_PREFIX[r["map"]]}-supply-<nw|sw|ne|se> on map {r["map"]}')
    land=land_rows.get(r['landmark'])
    if land is None: errors.append(f'{where}: landmark {r["landmark"]} is not a landmarks id'); continue
    if land['map']!=r['map']: errors.append(f'{where}: landmark {land["id"]} is on map {land["map"]}, the row is on map {r["map"]}')
    if not land['template'].startswith('supply/'): errors.append(f'{where}: landmark {land["id"]} has template {land["template"]}, which is not a supply/ piece')
    if land['elevation']!=r['elevation']: errors.append(f'{where}: elevation {r["elevation"]} is not that of landmark {land["id"]} ({land["elevation"]})')
    if land['rotation'] not in (0,180): errors.append(f'{where}: landmark {land["id"]} has rotation {land["rotation"]}, a house stands at 0 or 180'); continue
    yard=(land['x'],land['z']+6 if land['rotation']==0 else land['z']-6)
    if (r['yardX'],r['yardZ'])!=yard: errors.append(f'{where}: the yard centre is ({r["yardX"]},{r["yardZ"]}), the local (15,24) of landmark {land["id"]} is ({yard[0]},{yard[1]})')
    box=land_box(land)
    if box is not None and box[0]<=r['spurX']<=box[2] and box[1]<=r['spurZ']<=box[3]: errors.append(f'{where}: the spur end ({r["spurX"]},{r["spurZ"]}) lies inside the footprint x {box[0]}..{box[2]}, z {box[1]}..{box[3]}')
    if abs(r['spurX'])>350 or abs(r['spurZ'])>250: errors.append(f'{where}: the spur end ({r["spurX"]},{r["spurZ"]}) is outside |x|<=350, |z|<=250')
    if len(r['name'])>20: errors.append(f'{where}: the name {r["name"]!r} is {len(r["name"])} characters, at most 20')
for m,rows in by_map.items():
    seen={}
    for r in rows:
        if r['name'] in seen: errors.append(f'supplyHouses/{r["id"]}: the name {r["name"]!r} is also that of {seen[r["name"]]} on map {m}')
        seen[r['name']]=r['id']
        partner=[o for o in rows if (o['yardX'],o['yardZ'])==(-r['yardX'],-r['yardZ'])]
        if len(partner)!=1: errors.append(f'supplyHouses/{r["id"]}: no partner on map {m} at the yard centre ({-r["yardX"]},{-r["yardZ"]})'); continue
        o=partner[0]
        if o['elevation']!=r['elevation']: errors.append(f'supplyHouses/{r["id"]}: its partner {o["id"]} has elevation {o["elevation"]}, this row {r["elevation"]}')
        if (o['spurX'],o['spurZ'])!=(-r['spurX'],-r['spurZ']): errors.append(f'supplyHouses/{r["id"]}: the spur end of its partner {o["id"]} is ({o["spurX"]},{o["spurZ"]}), not ({-r["spurX"]},{-r["spurZ"]})')
    if m in SUPPLY_COUNT and len(rows)!=SUPPLY_COUNT[m]: errors.append(f'supplyHouses: map {m} has {len(rows)} rows, it needs {SUPPLY_COUNT[m]}')
for m in SUPPLY_COUNT:
    if m not in by_map: errors.append(f'supplyHouses: map {m} has no rows, it needs {SUPPLY_COUNT[m]}')
used=[r['landmark'] for r in supply_rows]
for h in house_land:
    # a house stands on open ground: it takes the place of no other landmark and encloses no point (the plan's tables have - in both columns)
    if h['replaces']!='-': errors.append(f'landmarks/{h["id"]}: replaces is {h["replaces"]!r}, a house replaces nothing (-)')
    if h['encloses']!='-': errors.append(f'landmarks/{h["id"]}: encloses is {h["encloses"]!r}, a house encloses nothing (-)')
    if used.count(h['id'])!=1: errors.append(f'landmarks/{h["id"]}: {used.count(h["id"])} supplyHouses rows name this house, there must be one')
if errors:
    print('\n'.join(errors)); sys.exit(1)
def typ(v): return 'boolean' if isinstance(v,bool) else 'int' if isinstance(v,int) else 'double' if isinstance(v,float) else 'String'
def literal(v): return json.dumps(v,ensure_ascii=True)
# Validated above, but process metadata rather than game data: never shipped as Spec records.
NOT_SHIPPED={'checks','revisionChecks','playtestMilestone'}
out=['package gg.gravesandglory;','public final class Spec {']
for sheet,rows in data.items():
    if sheet in NOT_SHIPPED: continue
    name=sheet[0].upper()+sheet[1:]+'Row'
    cols=list(rows[0])
    out.append(f'public record {name}('+','.join(typ(rows[0][c])+' '+c for c in cols)+') {}')
    out.append(f'public static {name}[] {sheet.upper()} = new {name}[] {{'+','.join('new '+name+'('+','.join(literal(r[c]) for c in cols)+')' for r in rows)+'};')
out.append('}')
dest=root/'src/main/java/gg/gravesandglory/Spec.java'
dest.parent.mkdir(parents=True,exist_ok=True)
dest.write_text('\n'.join(out))
report={'filledCells':cells,'unfilledCells':[],'unresolvedReferences':[],'runtimeVerification':'pending; tracked separately in verification.json'}
(root/'sheets/preflight.json').write_text(json.dumps(report,indent=2))
print(f'Preflight clean: {cells} filled cells; every row generates a Java record. Runtime verification pending.')
