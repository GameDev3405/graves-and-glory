import json, os, pathlib, re, sys, zipfile
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
# Sides (spec 1.1): factions holds the banner colours. Team 0 and team 1 read their positions from rows 0 and 1;
# colour, name and glyph come from the side each team fields (Battle.side).
GLYPHS=('sun','moon','star','wave')
sides=data['factions']
if len(sides)<2: errors.append('factions: at least two side rows, since teams 0 and 1 read their positions from rows 0 and 1')
for f in sides:
    if f['glyph'] not in GLYPHS: errors.append(f'factions/{f["id"]}: glyph {f["glyph"]} is not one of {", ".join(GLYPHS)}')
if len({f['glyph'] for f in sides})!=len(sides) or len({f['color'] for f in sides})!=len(sides): errors.append('factions: every side needs its own colour and its own glyph')
troops={r['id']:r for r in data['troops']}
if data['rules'][0]['playersPerTeam']*2!=data['rules'][0]['maxPlayers']: errors.append('capacity mismatch: maxPlayers must be playersPerTeam for each of the two teams')
# Battle modes (Phase 4 Task 2): the mode's own numbers live on a modes row, so the frame reads a row instead of Domination's
# hard-coded win score, score interval and round length, and a new mode adds a row rather than another branch in Battle.
modes=data.get('modes',[])
if not modes: errors.append('modes: at least one row; domination is the mode every map hosts and the round falls back to')
if 'domination' not in {m['id'] for m in modes}: errors.append('modes: no domination row, the mode a lobby opens on')
for m in modes:
    where=f'modes/{m["id"]}'
    for part in m['maps'].split(','):
        if not part.strip().isdigit() or int(part) not in range(len(data['maps'])): errors.append(f'{where}: maps entry {part!r} is not a map index 0..{len(data["maps"])-1}')
    if m['timeout'] not in ('score','team0','team1'): errors.append(f'{where}: timeout {m["timeout"]!r} is not score, team0 or team1')
    if m['ruleLine'].count('{n}')>1: errors.append(f'{where}: ruleLine holds more than one {{n}}')
    if m['roundSeconds']<0 or m['winTarget']<0 or m['scoreInterval']<0: errors.append(f'{where}: roundSeconds, winTarget and scoreInterval cannot be negative')
    if m['scoreInterval']>0 and m['winTarget']<=0: errors.append(f'{where}: a mode that scores needs a winTarget above 0')
    if m['scoreInterval']==0 and m['timeout']=='score': errors.append(f'{where}: timeout score needs a scoreInterval above 0')
    for column in ('howTo','howToDefend'):
        parts=[p.strip() for p in m.get(column,'').split('|') if p.strip()]
        if not 1<=len(parts)<=2: errors.append(f'{where}: {column} holds {len(parts)} parts, want 1 or 2')
# Mode copy (plan 2C.2): the feed lines a mode sends, the 2 to 4 numbers of its ledger and its briefing how-to lines are draft copy in
# the sheets, not in the mode classes. A feed line has a kind FeedLines.style knows, the {name} fields its args column lists, and a text
# FeedLines.MAX_TEXT can carry; FeedFitCheck measures how wide each one draws on the feed leaf. A how-to part may name a key as {key:<id>}.
FEED_KINDS=('point','loss','kill','down','up','laststand','deploy','notice')
FEED_ARGS=('team','point','stop','n','site','gold')
HOWTO_KEYS={'menu','attack','follow','formation','map','rotate_left','rotate_right','confirm','cancel','use','hints'}
copy_modes={m['id'] for m in modes}
events=set()
for r in data.get('modeLines',[]):
    where=f'modeLines/{r["id"]}'
    if r['mode'] not in copy_modes: errors.append(f'{where}: mode {r["mode"]} is not a modes id')
    if (r['mode'],r['event']) in events: errors.append(f'{where}: the event {r["event"]} of {r["mode"]} is already a row')
    events.add((r['mode'],r['event']))
    if not re.fullmatch(r'[a-z][a-z0-9_]*',r['event']): errors.append(f'{where}: event {r["event"]!r} is not lower-case words joined by _')
    if r['kind'] not in FEED_KINDS: errors.append(f'{where}: kind {r["kind"]} is not one of '+', '.join(FEED_KINDS))
    if r['status'] not in ('draft','approved'): errors.append(f'{where}: status {r["status"]} is not draft or approved')
    declared=[] if r['args']=='-' else r['args'].split(',')
    for a in declared:
        if a not in FEED_ARGS: errors.append(f'{where}: arg {a} is not one of '+', '.join(FEED_ARGS))
    used=re.findall(r'\{([a-z]+)\}',r['text'])
    if r['text'].count('{')!=len(used) or r['text'].count('}')!=len(used): errors.append(f'{where}: a brace in the text is not a {{name}} field')
    if sorted(set(used))!=sorted(set(declared)): errors.append(f'{where}: the text uses fields {sorted(set(used))}, its args column lists {sorted(set(declared))}')
    if len(r['text'])>120: errors.append(f'{where}: the text is {len(r["text"])} characters, a feed line carries 120')
    if r['text']!=r['text'].strip(): errors.append(f'{where}: the text has spaces at either end')
ledger_slots={}
for r in data.get('modeLedger',[]):
    where=f'modeLedger/{r["id"]}'
    if r['mode'] not in copy_modes: errors.append(f'{where}: mode {r["mode"]} is not a modes id')
    if r['format'] not in ('plain','fraction','percent','time'): errors.append(f'{where}: format {r["format"]} is not plain, fraction, percent or time')
    if r['team'] not in (-1,0,1): errors.append(f'{where}: team {r["team"]} is not -1 (both), 0 or 1')
    if r['status'] not in ('draft','approved'): errors.append(f'{where}: status {r["status"]} is not draft or approved')
    if not 0<=r['slot']<=3: errors.append(f'{where}: slot {r["slot"]} is not 0 to 3')
    if len(r['label'])>16: errors.append(f'{where}: label {r["label"]!r} is over 16 characters')
    ledger_slots.setdefault(r['mode'],[]).append(r['slot'])
for mode_id,slots in ledger_slots.items():
    if not 2<=len(slots)<=4 or sorted(slots)!=list(range(len(slots))): errors.append(f'modeLedger: {mode_id} has slots {sorted(slots)}, it needs 2 to 4 rows in slots 0, 1, 2...')
for mode_id in ('domination','siege','convoy','survival'):
    if mode_id in copy_modes and mode_id not in ledger_slots: errors.append(f'modeLedger: the {mode_id} mode needs its ledger rows')
    if mode_id in copy_modes and not any(r['mode']==mode_id for r in data.get('modeLines',[])): errors.append(f'modeLines: the {mode_id} mode needs its feed lines')
for m in modes:
    for column in ('howTo','howToDefend'):
        for key in re.findall(r'\{key:([a-z_]+)\}',m.get(column,'')):
            if key not in HOWTO_KEYS: errors.append(f'modes/{m["id"]}: {column} names {{key:{key}}}, not one of '+', '.join(sorted(HOWTO_KEYS)))
# Convoy (spec 2026-10-31-convoy-escort-design.md 5.1): the mode's numbers are a modeRules row and its road and bases modeSites
# rows, so ConvoyMode holds no map data. A road's nodes have consecutive stages from 0, at least two named checkpoints before
# the destination (the last node, always named); a base row (stage = team) stands off the road and more than 40 blocks from
# the end its team does not hold (the defence's from the destination, the escort's from the start).
mode_ids={m['id'] for m in modes}
rule_rows={r['id']:r for r in data.get('modeRules',[])}
for r in data.get('modeRules',[]):
    where=f'modeRules/{r["id"]}'
    if r['id'] not in mode_ids: errors.append(f'{where}: no modes row {r["id"]}')
    for c in ('pushRadius','pushHeight','rollbackSpeed','idleSeconds','checkpointSeconds','checkpointGold','overtimeRadius','overtimeGraceSeconds','stoppedAfterSeconds','stoppedEverySeconds','siegeStageBonus','portcullisSeconds','keepEdgeAttackers','keepEdgeDefenders'):
        if r[c]<0: errors.append(f'{where}: {c} cannot be negative')
    if r['id']=='convoy':
        if not 0<r['speedOne']<=r['speedTwo']<=r['speedThree']: errors.append(f'{where}: push speeds must be above 0 and never fall with more pushers')
        if not 0<r['uphill']<=1<=r['downhill']: errors.append(f'{where}: uphill must be in (0,1] and downhill at least 1')
    if r['id']=='siege' and not (r['keepEdgeAttackers']>0 and r['keepEdgeDefenders']>=r['keepEdgeAttackers']): errors.append(f'{where}: the keep edge is attackers of defenders, 0 < keepEdgeAttackers <= keepEdgeDefenders')
if 'convoy' in mode_ids and 'convoy' not in rule_rows: errors.append('modeRules: the convoy mode needs its row')
if 'siege' in mode_ids and 'siege' not in rule_rows: errors.append('modeRules: the siege mode needs its row')
def seg_distance(px,pz,a,b):
    dx,dz=b[0]-a[0],b[1]-a[1];t=max(0,min(1,((px-a[0])*dx+(pz-a[1])*dz)/((dx*dx+dz*dz) or 1)))
    return ((px-a[0]-t*dx)**2+(pz-a[1]-t*dz)**2)**.5
piece_ids={p['id'] for p in data.get('pieces',[])}
roads={}
for s in data.get('modeSites',[]):
    where=f'modeSites/{s["id"]}'
    if s['mode'] not in mode_ids: errors.append(f'{where}: mode {s["mode"]} is not a modes id')
    if s['map'] not in range(len(data['maps'])): errors.append(f'{where}: map {s["map"]} is not a map index')
    if s['kind'] not in ('road','base','stage','gate','ward','shrine','rise'): errors.append(f'{where}: kind {s["kind"]} is not road, base, stage, gate, ward, shrine or rise')
    if s['piece']!='-' and s['piece'] not in piece_ids: errors.append(f'{where}: piece {s["piece"]} is not a pieces id')
    if s['kind'] in ('gate','ward') and s['piece']=='-': errors.append(f'{where}: a {s["kind"]} row needs its piece')
    if s['rotation'] not in (0,90,180,270): errors.append(f'{where}: rotation {s["rotation"]} is not 0, 90, 180 or 270')
    if s['hp']<0 or s['seconds']<0: errors.append(f'{where}: hp and seconds cannot be negative')
    if s['kind']=='road': roads.setdefault((s['mode'],s['map']),[]).append(s)
for (mode,m),nodes in roads.items():
    nodes.sort(key=lambda s:s['stage']);where=f'modeSites {mode} map {m}'
    if [s['stage'] for s in nodes]!=list(range(len(nodes))): errors.append(f'{where}: road stages must run 0..{len(nodes)-1} with no gap')
    if nodes[-1]['name']=='-': errors.append(f'{where}: the last road node is the destination and needs a name')
    if sum(1 for s in nodes[1:-1] if s['name']!='-')<2: errors.append(f'{where}: at least two named checkpoints before the destination')
    pts=[(s['x'],s['z']) for s in nodes]
    for b in data.get('modeSites',[]):
        if b['kind']!='base' or b['mode']!=mode or b['map']!=m: continue
        if b['stage'] not in (0,1): errors.append(f'modeSites/{b["id"]}: a base stage is its team, 0 or 1')
        end=pts[-1] if b['stage']==1 else pts[0]
        if ((b['x']-end[0])**2+(b['z']-end[1])**2)**.5<=40: errors.append(f'modeSites/{b["id"]}: a base within 40 blocks of the road end its enemy pushes for')
        near=min(seg_distance(b['x'],b['z'],pts[i],pts[i+1]) for i in range(len(pts)-1))
        if near<=b['radius']+8: errors.append(f'modeSites/{b["id"]}: base {near:.1f} blocks from the road, it must stand off it')
# Siege: the three stages in order (a ring each for the yard and the keep, the gate between them) and the wards' rows.
for m in {s['map'] for s in data.get('modeSites',[]) if s['mode']=='siege'}:
    st=sorted((s['stage'],s['kind']) for s in data['modeSites'] if s['mode']=='siege' and s['map']==m and s['kind'] in ('stage','gate'))
    if st!=[(0,'stage'),(1,'gate'),(2,'stage')]: errors.append(f'modeSites siege map {m}: the stages must be a stage row 0, the gate row 1 and a stage row 2, found {st}')
    for s in data['modeSites']:
        if s['mode']=='siege' and s['map']==m and s['kind']=='stage' and (s['radius']<=0 or s['seconds']<=0): errors.append(f'modeSites/{s["id"]}: a stage ring needs a radius and seconds above 0')
        if s['mode']=='siege' and s['map']==m and s['kind']=='gate' and s['hp']<=0: errors.append(f'modeSites/{s["id"]}: the gate needs hp above 0')
# Survival (Phase 4 spec 6): the modeRules survival row, a waves set of consecutive waves, waveGroups that rise at a rise site
# (modeSites id <...>_rise_<site>) on every map the mode hosts, one shrine ring and one court (base, team 0) per map, and the
# botCommand tiers' waveScale and waveTraitTier.
if 'survival' in mode_ids:
    sr=rule_rows.get('survival')
    if sr is None: errors.append('modeRules: the survival mode needs its row')
    else:
        if sr['survivalPerBanner']<0 or sr['riseWarning']<0: errors.append('modeRules/survival: survivalPerBanner and riseWarning cannot be negative')
        if sr['maxDeadAlive']<=0: errors.append('modeRules/survival: maxDeadAlive must be above 0')
        wave_set=[w for w in data.get('waves',[]) if w['set']==sr['waveSet']]
        if [w['wave'] for w in sorted(wave_set,key=lambda w:w['wave'])]!=list(range(1,len(wave_set)+1)) or not wave_set: errors.append(f'waves: set {sr["waveSet"]} must hold waves 1..n with no gap')
        for w in wave_set:
            if w['seconds']<=0 or w['gold']<0: errors.append(f'waves/{w["id"]}: seconds must be above 0 and gold not negative')
            if not any(g['wave']==w['id'] for g in data.get('waveGroups',[])): errors.append(f'waves/{w["id"]}: no waveGroups row rises in this wave')
    surv_maps=[int(p) for p in next(m for m in modes if m['id']=='survival')['maps'].split(',') if p.strip()]
    troop_ids={t['id'] for t in data['troops']}
    wave_ids={w['id'] for w in data.get('waves',[])}
    for m in surv_maps:
        rows=[s for s in data['modeSites'] if s['mode']=='survival' and s['map']==m]
        shrines=[s for s in rows if s['kind']=='shrine'];courts=[s for s in rows if s['kind']=='base' and s['stage']==0]
        if len(shrines)!=1 or shrines[0]['radius']<=0 or shrines[0]['seconds']<=0: errors.append(f'modeSites survival map {m}: one shrine row with a radius and seconds above 0')
        if len(courts)!=1: errors.append(f'modeSites survival map {m}: one base row for team 0 (the court)')
        rises={s['id'].rsplit('_rise_',1)[1] for s in rows if s['kind']=='rise' and '_rise_' in s['id']}
        for s in rows:
            if s['kind']=='rise' and ('_rise_' not in s['id'] or s['radius']<=0): errors.append(f'modeSites/{s["id"]}: a rise row is named <...>_rise_<site> and needs a radius above 0')
        for g in data.get('waveGroups',[]):
            if g['site'] not in rises: errors.append(f'waveGroups/{g["id"]}: site {g["site"]} is no rise row on map {m}')
    for g in data.get('waveGroups',[]):
        if g['wave'] not in wave_ids: errors.append(f'waveGroups/{g["id"]}: wave {g["wave"]} is not a waves id')
        if g['troop'] not in troop_ids: errors.append(f'waveGroups/{g["id"]}: troop {g["troop"]} is not a troops id')
        if g['count']<=0: errors.append(f'waveGroups/{g["id"]}: count must be above 0')
    for r in data['botCommand']:
        if r['waveScale']<=0 or r['waveTraitTier'] not in range(0,4): errors.append(f'botCommand/{r["id"]}: waveScale above 0 and waveTraitTier 0..3')
groups={g['id'] for g in data.get('groups',[])}
orders={o['id'] for o in data['orders']}
for t in data['troops']:
    if t.get('group') not in groups: errors.append(t['id']+': group does not resolve')
for row in data.get('commandInterface',[]):
    for group in row['selection'].split(','):
        if group not in groups: errors.append('command selection does not resolve: '+group)
    for order in row['orders'].split(','):
        if order not in orders: errors.append('command order does not resolve: '+order)
# Troop textures. A gravesandglory: path must exist in the mod's assets. A minecraft: path must be one of VANILLA_TEXTURES, the
# vanilla paths the sheet may name, each checked by hand against the Minecraft client jar of build.gradle (1.20.1); when Loom has
# cached that jar (after the first build) the path must also be an entry of it, so a misspelled or renamed vanilla texture fails
# here instead of showing as a missing-texture model in game. The list keeps the result the same on a machine without the jar:
# naming a new vanilla texture in the sheet means adding it here.
VANILLA_TEXTURES=('textures/entity/bear/polarbear.png','textures/entity/illager/evoker.png','textures/entity/illager/pillager.png',
    'textures/entity/illager/ravager.png','textures/entity/illager/vindicator.png','textures/entity/iron_golem/iron_golem.png',
    'textures/entity/piglin/zombified_piglin.png','textures/entity/skeleton/skeleton.png','textures/entity/skeleton/wither_skeleton.png',
    'textures/entity/skeleton/stray.png','textures/entity/spider/spider.png','textures/entity/wolf/wolf.png','textures/entity/zombie/drowned.png',
    'textures/entity/zombie/zombie.png')
mcVersion=re.search(r"minecraft 'com\.mojang:minecraft:([^']+)'",(root/'build.gradle').read_text()) if (root/'build.gradle').is_file() else None
mcVersion=mcVersion.group(1) if mcVersion else None
clientJar=pathlib.Path(os.environ.get('GRADLE_USER_HOME') or pathlib.Path.home()/'.gradle')/'caches/fabric-loom'/(mcVersion or '-')/'minecraft-client.jar'
vanilla=None
if mcVersion and clientJar.is_file():
    with zipfile.ZipFile(clientJar) as jar: vanilla={n for n in jar.namelist() if n.startswith('assets/minecraft/textures/')}
for r in data['troops']:
    space,_,path=r['texture'].partition(':')
    if space=='gravesandglory':
        if not (root/'src/main/resources/assets/gravesandglory'/path).exists(): errors.append(f'{r["id"]}: missing texture')
    elif space=='minecraft':
        if path not in VANILLA_TEXTURES: errors.append(f'troops/{r["id"]}: texture {r["texture"]} is not in VANILLA_TEXTURES (tools/preflight.py), the vanilla paths checked against the {mcVersion} client jar')
        elif vanilla is not None and 'assets/minecraft/'+path not in vanilla: errors.append(f'troops/{r["id"]}: texture {r["texture"]} is not in the Minecraft {mcVersion} client jar ({clientJar})')
    else: errors.append(f'troops/{r["id"]}: texture {r["texture"]} must be a gravesandglory: or a minecraft: path')
# Troop looks (Phase 2 Task 5, unit identity rules 2 and 7): the painted shield is the one vanilla item a troop draws,
# its patterns from troops.shield on the side's factions.dye; each art/troops/<id>.json is a troop's own model source, and
# tools/build_skins.py lists every model it built in models/troop/index.json.
DYES=('white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black')
BANNER_PATTERNS=('b','bl','br','tl','tr','bs','ts','ls','rs','cs','ms','drs','dls','ss','cr','sc','bt','tt','bts','tts','ld','rd','lud','rud',
    'mc','mr','vh','hh','vhr','hhb','bo','cbo','gra','gru','bri','glb','cre','sku','flo','moj','pig')
for f in sides:
    if f['dye'] not in DYES: errors.append(f'factions/{f["id"]}: dye {f["dye"]} is not a vanilla dye ({", ".join(DYES)})')
# A shield's field is its side's factions.dye in battle and GravesAndGlory.Battle.PORTRAIT_FIELD on a portrait (read from the
# source here, so the two cannot drift). A layer in the colour of a field vanishes on that field, and two sides with one
# dye fly the same shields, so every field dye is a side's own and no layer takes one.
battleSource=root/'src/main/java/gg/gravesandglory/GravesAndGlory.java'
portraitField=re.search(r'PORTRAIT_FIELD\s*=\s*"([a-z_]+)"',battleSource.read_text(encoding='utf-8')) if battleSource.is_file() else None
portraitField=portraitField.group(1) if portraitField else None
fields={}
for f in sides:
    if f['dye'] in fields: errors.append(f'factions/{f["id"]}: dye {f["dye"]} is also {fields[f["dye"]]}; every side needs its own shield field')
    fields.setdefault(f['dye'],f'factions/{f["id"]} dye')
if portraitField is None: errors.append('GravesAndGlory.java: no PORTRAIT_FIELD="<dye>", the shield field of a portrait')
elif portraitField not in DYES: errors.append(f'GravesAndGlory.java: PORTRAIT_FIELD {portraitField} is not a vanilla dye')
elif portraitField in fields: errors.append(f'GravesAndGlory.java: PORTRAIT_FIELD {portraitField} is {fields[portraitField]}, so a portrait would fly that side')
else: fields[portraitField]='the portrait field (GravesAndGlory.Battle.PORTRAIT_FIELD)'
for r in data['troops']:
    shielded='shield' in r['tags'].split(',')
    if (r['shield']=='none')==shielded: errors.append(f'troops/{r["id"]}: shield must be none exactly when tags lacks shield, not {r["shield"]}')
    elif r['shield']!='none':
        for layer in r['shield'].split(','):
            dye,_,code=layer.partition(':')
            if dye not in DYES or code not in BANNER_PATTERNS: errors.append(f'troops/{r["id"]}: shield layer {layer} must be <vanilla dye>:<banner pattern code>')
            elif dye in fields: errors.append(f'troops/{r["id"]}: shield layer {layer} is the colour of {fields[dye]}, and vanishes on that field')
artTroops=sorted(p.stem for p in (root/'art/troops').glob('*.json')) if (root/'art/troops').is_dir() else []
for t in artTroops:
    if t not in troops: errors.append(f'art/troops/{t}.json: {t} is not a troops id')
troopIndex=root/'src/main/resources/assets/gravesandglory/models/troop/index.json'
if artTroops or troopIndex.is_file():
    try: listedTroops=json.loads(troopIndex.read_text(encoding='utf-8')).get('troops')
    except (OSError,ValueError): listedTroops=None
    if listedTroops!=artTroops: errors.append(f'models/troop/index.json: troops {listedTroops} must be exactly the art/troops ids {artTroops}; run python tools/build_skins.py')
for r in data['ui']:
    if r['id']=='command':
        formationIds={f['id'] for f in data['formations']}
        for id in r['bottom'].split(','):
            if id not in formationIds: errors.append('UI formation reference missing: '+id)
# Phase 2 Houses (spec 6.1): a troop's House, rarity, ability and evolve line (the levelNames key since the in-match progression design) are its own columns.
# (modes, above, left the rules row its arena, capacity and reinforcement columns and took the round's own numbers with it.)
squad=data['rules'][0]['squadSize']
houses={h['id']:h for h in data['houses']}
rarities={r['id']:r for r in data['rarities']}
RARITY_ORDER=['common','rare','epic','legendary']
if [r['id'] for r in data['rarities']]!=RARITY_ORDER: errors.append('rarities: the rows must be common, rare, epic, legendary, in that order (validBanner checks bannerMax from the last row up, rarest first)')
abilityIds={a['id'] for a in data['abilities']}
evolveTroops={e['troop'] for e in data['levelNames']}
HOUSE_SHAPE={'common':2,'rare':2,'epic':1,'legendary':1}
houseOf,rarityOf={},{}
for t in data['troops']:
    name=f'troops/{t["id"]}'
    if t['house'] not in houses or t['rarity'] not in rarities: errors.append(f'{name}: house {t["house"]} or rarity {t["rarity"]} does not resolve'); continue
    houseOf[t['id']]=t['house'];rarityOf[t['id']]=t['rarity']
    if t['ability'] not in abilityIds: errors.append(f'{name}: ability {t["ability"]} does not resolve')
    if t['rarity'] in ('common','rare'):
        if t['evolveLine'] not in evolveTroops: errors.append(f'{name}: a Common or Rare needs an evolveLine that names a levelNames troop, not {t["evolveLine"]}')
    elif t['evolveLine']!='none': errors.append(f'{name}: an Epic or Legendary has evolveLine none')
    if t['slotWeight']!=rarities[t['rarity']]['slotWeight']: errors.append(f'{name}: slotWeight {t["slotWeight"]} differs from rarities/{t["rarity"]} slotWeight {rarities[t["rarity"]]["slotWeight"]}')
    if t['ranged']!=(t['range']>3): errors.append(f'{name}: ranged must be true exactly when range is above 3')
    for slot in ('weapon','helmet','chest'):
        if not t[slot].startswith('minecraft:'): errors.append(f'{name}: {slot} {t[slot]} must be a minecraft: item id (minecraft:air for none)')
    tags=t['tags'].split(',')
    if 'none' in tags and len(tags)>1: errors.append(f'{name}: tags is none or a comma list of tags, never both')
# A troop is drawn from its custom name (TroopModels.resolve through ArmyMobs.named, where a later troop row wins and a troop
# name beats a level name), and " (Veteran)" on it marks the Veteran skin: so no name may stand for two troops, two levels of
# one line, or a level and its own troop, and none may end in " (Veteran)".
VETERAN=' (Veteran)'
nameOwners={}
for t in data['troops']: nameOwners.setdefault(t['name'],{}).setdefault(t['id'],[]).append(f'troops/{t["id"]}')
for e in data['levelNames']:
    for t in data['troops']:
        if t['evolveLine']==e['troop']: nameOwners.setdefault(e['name'],{}).setdefault(t['id'],[]).append(f'levelNames/{e["id"]}')
for nm,owners in nameOwners.items():
    where=', '.join(w for ws in owners.values() for w in ws)
    if nm.endswith(VETERAN): errors.append(f'{where}: the name "{nm}" ends in "{VETERAN.strip()}", which marks a Veteran skin')
    if len(owners)>1: errors.append(f'{where}: the name "{nm}" stands for {len(owners)} troops ({", ".join(owners)}); a troop is drawn from its name, so one would draw as the other')
    else:
        for troop,ws in owners.items():
            if len(ws)>1: errors.append(f'{where}: the name "{nm}" is two levels of {troop}; its level could not be told from it')
for h in houses.values():
    if h['glyph']!=f'gravesandglory:textures/gui/house/{h["id"]}.png': errors.append(f'houses/{h["id"]}: glyph must be gravesandglory:textures/gui/house/{h["id"]}.png')
    elif not (root/'src/main/resources/assets/gravesandglory'/h['glyph'].partition(':')[2]).is_file(): errors.append(f'houses/{h["id"]}: missing glyph texture {h["glyph"]}')
    mine=[t['id'] for t in data['troops'] if t['house']==h['id']]
    if h['troops'].split(',')!=mine: errors.append(f'houses/{h["id"]}: troops must be {",".join(mine)}, the House troops in sheet order')
    if not h.get('fieldable'): errors.append(f'houses/{h["id"]}: fieldable must be true (all five Houses are fieldable since Phase 2 Task 13; a House that is not vanishes from banners)')
    for r,n in HOUSE_SHAPE.items():
        have=sum(1 for t in mine if rarityOf.get(t)==r)
        if have!=n: errors.append(f'houses/{h["id"]}: {have} {r} troops; every House has exactly {n} (all five are fieldable since Phase 2 Task 13)')
CLASS_ACTIVES=('stand_fast','bloodrush','mark_prey','rally_horn','grave_bloom','forward_supply')
CLASS_PASSIVES=('champion','reaver','warden','marshal','warlock','quartermaster')
for c in data['classes']:
    if c['group'] not in groups: errors.append(f'classes/{c["id"]}: group {c["group"]} does not resolve')
    if c['track'] not in ('hero','general'): errors.append(f'classes/{c["id"]}: track must be hero or general')
    if c['active'] not in CLASS_ACTIVES: errors.append(f'classes/{c["id"]}: active {c["active"]} is not one of {", ".join(CLASS_ACTIVES)} (Commander.ACTIVES keys)')
    if c['passive'] not in CLASS_PASSIVES: errors.append(f'classes/{c["id"]}: passive {c["passive"]} is not one of {", ".join(CLASS_PASSIVES)} (the effect kinds Commander reads)')
    if c['cooldownTicks']<c['durationTicks']: errors.append(f'classes/{c["id"]}: cooldownTicks must be at least durationTicks')
    for col in ('activeText','passiveText'):
        if len(c[col])>90: errors.append(f'classes/{c["id"]}: {col} is {len(c[col])} characters, over 90')
    if c['bonus']!=c['passiveText']: errors.append(f'classes/{c["id"]}: bonus must equal passiveText')
for col in ('active','passive'):
    seen=[c[col] for c in data['classes']]
    if len(set(seen))!=len(seen): errors.append(f'classes: every class needs its own {col}')
thresholds={}
for h in houses:
    tiers=sorted((t for t in data['houseTraits'] if t['house']==h),key=lambda t:t['tier'])
    if [t['tier'] for t in tiers]!=[1,2,3]: errors.append(f'houseTraits: {h} needs exactly tiers 1, 2 and 3')
    if len({t['mechanic'] for t in tiers})>1: errors.append(f'houseTraits: {h} uses one mechanic on every tier')
    if any(a['slots']>=b['slots'] for a,b in zip(tiers,tiers[1:])): errors.append(f'houseTraits: {h} slots must rise with the tier')
    thresholds[h]=[t['slots'] for t in tiers]
base=next(iter(thresholds),None)
for h,s in thresholds.items():
    if s!=thresholds[base]: errors.append(f'houseTraits: {h} tier slots {s} differ from {base} {thresholds[base]}; matchRewards takes one threshold array, so every House needs the same slots per tier')
TRAIT_MECHANICS=('rise_again','bulwark','pack_hunters','entrench','riptide')
for t in data['houseTraits']:
    if t['house'] not in houses: errors.append(f'houseTraits/{t["id"]}: house does not resolve')
    if t['id']!=f'{t["house"]}_{t["tier"]}': errors.append(f'houseTraits/{t["id"]}: id must be <house>_<tier>')
    if t['mechanic'] not in TRAIT_MECHANICS: errors.append(f'houseTraits/{t["id"]}: mechanic {t["mechanic"]} is not one of {", ".join(TRAIT_MECHANICS)} (CounterMath.is reads bulwark, pack_hunters, entrench and riptide; rise_again is the Graveborn death rule)')
MECHANICS={'none','shield','mend','blight','stance','pierce','cleave','onhit_status','pounce','aura','slam','fearless','mist','onkill_heal','raise','rise','mark'}
TRIGGERS={'passive','onhit','cast','hold','timer','charge_contact','rider','onkill'}
for a in data['abilities']:
    if a['mechanic'] not in MECHANICS: errors.append(f'abilities/{a["id"]}: mechanic {a["mechanic"]} is not in the closed list')
    if a['trigger'] not in TRIGGERS: errors.append(f'abilities/{a["id"]}: trigger {a["trigger"]} is not in the closed list')
    if a['target'] not in ('enemies','allies'): errors.append(f'abilities/{a["id"]}: target must be enemies or allies')
    if a['mechanic']=='stance' and a['amount']!=0: errors.append(f'abilities/{a["id"]}: stance multipliers live only in counters, so amount must be 0.0')
    if len(a['text'])>90: errors.append(f'abilities/{a["id"]}: text is {len(a["text"])} characters, over 90')
    if not isinstance(a['anchors'],bool): errors.append(f'abilities/{a["id"]}: anchors must be true or false')
    elif a['anchors'] and (a['mechanic']!='aura' or a['target']!='allies'): errors.append(f'abilities/{a["id"]}: only an aura on allies can anchor (Abilities.anchored reads the Oath Banner)')
    if a['mechanic']=='fearless' and not 0<a['amount2']<1: errors.append(f'abilities/{a["id"]}: a fearless row health share (amount2) must be between 0 and 1')
CONDITIONS={'always','raised_front','lowered_front','front','charge_impact','charge','hold','flank','no_sky','in_mist','target_slowed_or_wounded','target_wounded'}
# A counter tag is a dynamic tag, an ability id, a tag some troop carries in its tags column or a tag an abilities row grants (abilities.tags, | joined).
troopTags={tag for t in data['troops'] for tag in t['tags'].split(',')}-{'none'}
grantedTags={tag for a in data['abilities'] for tag in a['tags'].split('|')}-{'none'}
TAGS={'melee','arrow','shot','magic','nonsiege','ranged','charging','holding','structure','any'}|troopTags|abilityIds|grantedTags
counterIds={c['id'] for c in data['counters']}
for c in data['counters']:
    if c['condition'] not in CONDITIONS: errors.append(f'counters/{c["id"]}: condition {c["condition"]} is not in the closed list')
    for tag in c['attackerTag'].split('+')+c['targetTag'].split('+'):
        if tag not in TAGS: errors.append(f'counters/{c["id"]}: tag {tag} does not resolve')
    if c['replaces']!='none' and any(r not in counterIds or r==c['id'] for r in c['replaces'].split('|')): errors.append(f'counters/{c["id"]}: replaces must be none or a | list of other counter ids')
    if c['multiplier']<=0: errors.append(f'counters/{c["id"]}: multiplier must be above 0')
cd=data['combatDiscipline'][0]
if not 0<cd['multiplierMin']<=1<=cd['multiplierMax']: errors.append('combatDiscipline: the counter clamp must hold 1 (0 < multiplierMin <= 1 <= multiplierMax)')
if cd['chargeImpactTicks']<1: errors.append('combatDiscipline: chargeImpactTicks (the counters row C5 window) must be at least 1')
# The columns that came over from the Phase 1 morale sheet when routing went (Task 8b): the flank arc, the last stand, the command feedback sounds, the wounded share and the arrival radius; and the bleed (damage per beat and ticks between beats, design N3, Statuses: Task 10 moved them from code).
for col,lo,hi in (('flankArcDegrees',0,180),('lastStandShare',0,1),('lastStandRadius',0,None),('hornCooldownSeconds',0,None),('hornVolume',0,None),('woundedShare',0,1),('lastStandFeedSeconds',1,None),('arriveRadius',0,None),('bleedDamage',0,None),('bleedEveryTicks',0,None)):
    if col not in cd: errors.append(f'combatDiscipline: missing {col}')
    elif cd[col]<lo or hi is not None and cd[col]>hi or cd[col]==lo and col not in ('hornCooldownSeconds','lastStandFeedSeconds'): errors.append(f'combatDiscipline: {col} must be above {lo}'+(f' and at most {hi}' if hi is not None else ''))
if 'morale' in data: errors.append('morale: the sheet went with routing (Task 8b); its surviving numbers live in combatDiscipline')
if len(data['recruitment'])!=1: errors.append('recruitment: exactly one row')
if any(data['campaign'][0].get(k,-1)<0 for k in ('completionShards','victoryShards','completionRenown','victoryRenown')): errors.append('campaign: completionShards, victoryShards, completionRenown and victoryRenown must be 0 or more')
if not isinstance(data['campaign'][0].get('disconnectGraceSeconds'),int) or data['campaign'][0]['disconnectGraceSeconds']<0: errors.append('campaign: disconnectGraceSeconds must be a whole number of seconds, 0 or more (MatchRules.takeoverAt)')
cap=data['recruitment'][0]['capPct']
for r in rarities.values():
    if not 0<=r['starPct2']<=r['starPct3']<=cap+1e-9: errors.append(f'rarities/{r["id"]}: star bonuses must rise and stay within recruitment.capPct')
    if r['slotWeight']<1 or r['bannerMax']<1: errors.append(f'rarities/{r["id"]}: slotWeight and bannerMax must be at least 1')
# In-match progression (design sections 3, 4, 8 and 9): levels from kills, gear bought in battle, the level names and the per-level ability rows.
# Nothing here is a stage or an evolution: the between-match evolutions are gone, and no player text may say so (checked below).
levelNameRows={}
for e in data['levelNames']:
    if e['id']!=f'{e["troop"]}_{e["level"]}' or e['level'] not in (2,3): errors.append(f'levelNames/{e["id"]}: id must be <troop>_<level> for level 2 or 3'); continue
    if rarityOf.get(e['troop']) not in ('common','rare'): errors.append(f'levelNames/{e["id"]}: only a House Common or Rare has level names (an Epic or Legendary keeps its own name)'); continue
    levelNameRows.setdefault(e['troop'],{})[e['level']]=e
for t,r in rarityOf.items():
    if r in ('common','rare') and sorted(levelNameRows.get(t,{}))!=[2,3]: errors.append(f'levelNames: {t} needs one level 2 and one level 3 row')
if len(data['progression'])!=1: errors.append('progression: exactly one row')
else:
    pg=data['progression'][0]
    if not isinstance(pg['enabled'],bool): errors.append('progression: enabled must be true or false')
    if pg['assistSeconds']<1 or pg['commanderBounty']<1 or pg['chevronRange']<=0: errors.append('progression: assistSeconds, commanderBounty and chevronRange must be above 0')
    if pg['feedLevel'] not in (2,3): errors.append('progression: feedLevel must be 2 or 3')
levels=data['levels']
if [l['level'] for l in levels]!=[1,2,3]: errors.append('levels: the rows must be levels 1, 2 and 3, in order')
elif levels[0]['kills']!=0: errors.append('levels: level 1 needs 0 kills (a soldier is trained at level 1)')
for a,b in zip(levels,levels[1:]):
    if a['kills']>=b['kills']: errors.append(f'levels: kills must rise strictly with the level ({a["id"]} {a["kills"]}, {b["id"]} {b["kills"]})')
    if b['healthPct']<a['healthPct'] or b['damagePct']<a['damagePct']: errors.append(f'levels: healthPct and damagePct must not fall with the level ({a["id"]} to {b["id"]})')
tiers=data['gearTiers']
if [g['tier'] for g in tiers]!=[1,2,3]: errors.append('gearTiers: the rows must be tiers 1, 2 and 3, in order')
for a,b in zip(tiers,tiers[1:]):
    if b['weaponPct']<a['weaponPct'] or b['armourPct']<a['armourPct']: errors.append(f'gearTiers: weaponPct and armourPct must not fall with the tier ({a["id"]} to {b["id"]})')
for r in rarities.values():
    if r['gearPrice2']<=0 or r['gearPrice3']<=r['gearPrice2']: errors.append(f'rarities/{r["id"]}: gear prices must be positive with tier III (gearPrice3) above tier II (gearPrice2)')
STATUSES=('none','slow','weaken','bleed','knockback','stun')
abilityRows={a['id']:a for a in data['abilities']}
for a in data['abilities']:
    if a['status'] not in STATUSES: errors.append(f'abilities/{a["id"]}: status {a["status"]} is not one of {", ".join(STATUSES)}')
    if a['mechanic']=='onhit_status' and a['status']=='none': errors.append(f'abilities/{a["id"]}: an onhit_status row needs a status')
    if a['taken']<0 or a['taken']>=1: errors.append(f'abilities/{a["id"]}: taken must be at least 0 and below 1')
    if not isinstance(a['cleanse'],bool) or not isinstance(a['rangedPenalty'],bool): errors.append(f'abilities/{a["id"]}: cleanse and rangedPenalty must be true or false')
levelAbility={}
for l in data['levelAbilities']:
    if l['troop'] not in troops or l['level'] not in (2,3) or l['id']!=f'{l["troop"]}_{l["level"]}': errors.append(f'levelAbilities/{l["id"]}: id must be <troop>_<level> for a troops id and level 2 or 3'); continue
    if l['ability'] not in abilityRows: errors.append(f'levelAbilities/{l["id"]}: ability {l["ability"]} does not resolve'); continue
    if (l['troop'],l['level']) in levelAbility: errors.append(f'levelAbilities/{l["id"]}: a second row for the same troop and level')
    levelAbility[(l['troop'],l['level'])]=l
for t in data['troops']:
    for lv in (2,3):
        if (t['id'],lv) not in levelAbility: errors.append(f'levelAbilities: {t["id"]} needs exactly one level-{lv} row')
    two=levelAbility.get((t['id'],2))
    if two and t['ability'] in abilityRows and abilityRows[two['ability']]['mechanic']!=abilityRows[t['ability']]['mechanic']: errors.append(f'levelAbilities/{two["id"]}: a level-2 ability keeps its base ability\'s mechanic ({abilityRows[t["ability"]]["mechanic"]}), not {abilityRows[two["ability"]]["mechanic"]}')
# In-match progression Task 6, ruling I1: a level-3 row that changes what the level-2 row does is no second cast. Trigger rider: it rides on the level-2 row
# (Abilities.second): a mend or the howl that also cleanses, a cloud that also lays a status, or a burst (blight) that also leaves a zone. It keeps the level-2
# row's timing, and a rider of the same mechanic its reach too, so it never casts on its own clock. Trigger onkill: it acts on a kill (onkill_heal, Abilities.onKill;
# raise, on a kill by the troop's blight).
for (troop,lv),l in levelAbility.items():
    a=abilityRows[l['ability']];name=f'abilities/{a["id"]}'
    # Task 6 review M6: Abilities.tick casts only a soldier's first row and onHit adds his level-3 row only on the onhit trigger, so a level-3 row on a clock
    # (cast, timer, hold) or on a charge's contact would never fire.
    if lv==3 and a['trigger'] in ('cast','timer','hold','charge_contact'): errors.append(f'{name}: a level-3 row on trigger {a["trigger"]} never fires (a level-3 row acts as a passive, on a hit, as a rider or on a kill)')
    if a['trigger']=='rider':
        two=levelAbility.get((troop,2));two=abilityRows.get(two['ability']) if two else None
        if lv!=3 or two is None: errors.append(f'{name}: a rider is a level-3 row on a troop with a level-2 row'); continue
        same=a['mechanic']==two['mechanic']
        if not (same and a['mechanic'] in ('mend','mist') or same and a['mechanic']=='aura' and two['trigger']=='timer' or a['mechanic']=='mist' and two['mechanic']=='blight'): errors.append(f'{name}: a rider is a mend on a mend, an aura on a timed aura (the howl), a mist on a mist, or a mist zone on a blight burst; its level-2 row is {two["mechanic"]} on {two["trigger"]}'); continue
        if two['trigger']=='rider': errors.append(f'{name}: the level-2 row a rider rides on has a trigger of its own')
        if any(a[k]!=two[k] for k in ('cooldownTicks','windupTicks','minRange')): errors.append(f'{name}: a rider keeps its level-2 row\'s cooldownTicks, windupTicks and minRange (it never casts on its own clock)')
        if same and any(a[k]!=two[k] for k in ('radius','durationTicks')): errors.append(f'{name}: a rider of its level-2 row\'s mechanic keeps its radius and durationTicks')
        if a['mechanic'] in ('mend','aura') and (not a['cleanse'] or a['amount']!=0 or a['amount2']!=0): errors.append(f'{name}: a mend or aura rider cleanses (cleanse true) and adds no heal or share of its own (amount and amount2 0)')
        if a['mechanic']=='mist' and (a['status'] not in ('weaken','bleed') or a['statusTicks']<=0): errors.append(f'{name}: a mist rider lays weaken or bleed (a Statuses status) for statusTicks above 0')
        if a['mechanic']=='mist' and not same and (a['radius']<=0 or a['durationTicks']<=0): errors.append(f'{name}: a zone a burst leaves needs a radius and a durationTicks above 0')
    elif a['mechanic']=='raise':
        two=levelAbility.get((troop,2));two=abilityRows.get(two['ability']) if two else None
        if lv!=3 or two is None or two['mechanic']!='blight': errors.append(f'{name}: raise is a level-3 row of a troop whose level-2 row is a blight (her blight kills rise)')
        elif a['amount']<1 or a['durationTicks']<=0: errors.append(f'{name}: raise needs amount (the most at a time) 1 or more and durationTicks above 0')
for a in data['abilities']:
    if (a['trigger']=='onkill')!=(a['mechanic'] in ('onkill_heal','raise')): errors.append(f'abilities/{a["id"]}: onkill_heal and raise act on a kill (trigger onkill), and only they do')
    if a['trigger'] in ('rider','onkill') and a['id'] in {t['ability'] for t in data['troops']}: errors.append(f'abilities/{a["id"]}: a rider or onkill row is a level-3 row, never a troop\'s base ability')
    if a['trigger'] in ('rider','onkill') and a['id'] not in {l['ability'] for (_,lv),l in levelAbility.items() if lv==3}: errors.append(f'abilities/{a["id"]}: a rider or onkill row is the ability of a level-3 levelAbilities row')
# Every counter tag an ability grants is read by some counter row, so no granted tag is dead.
counterTags={tag for c in data['counters'] for tag in c['attackerTag'].split('+')+c['targetTag'].split('+')}
for a in data['abilities']:
    if a['tags']!='none':
        for tag in a['tags'].split('|'):
            if tag not in counterTags: errors.append(f'abilities/{a["id"]}: tag {tag} appears in no counter row')
# Player-facing text never says stage, evolve or Writ (it says Pact): every text and name cell, and every troopNotes cell.
BANNED=re.compile(r'stage|evolve|writ',re.I)
for sheet,rows in data.items():
    for row in rows:
        for col,value in row.items():
            if isinstance(value,str) and (col in ('text','name') or sheet=='troopNotes' and col!='id') and BANNED.search(value): errors.append(f'{sheet}/{row.get("id")}/{col}: player text says "{BANNED.search(value).group(0)}" (it says level, tier or Pact): {value}')
for sheet,keys in (('mastery',('kills','matches')),('renown',('points',))):
    rows=data[sheet]
    if [r['level'] for r in rows]!=list(range(1,len(rows)+1)): errors.append(f'{sheet}: levels must run 1, 2, 3 and on, in order')
    for k in keys:
        if any(a[k]>=b[k] for a,b in zip(rows,rows[1:])): errors.append(f'{sheet}: {k} must rise with the level')
for r in data['renown']:
    if r['reward']=='legendary' and r['points']!=data['recruitment'][0]['renownForLegendary']: errors.append(f'renown/{r["id"]}: the legendary level must sit at recruitment.renownForLegendary')
difficulties={b['id'] for b in data['botCommand']}
# Bot gear buying (LevelMath.botPicks): the priority is one of the three orders it knows (any other word silently behaves as banner), the reserve is a count of
# cheapest-troop prices kept back (a negative one would budget more gold than the army holds), and the top tier is one the rarities sheet prices (gearPrice2, gearPrice3).
GEAR_PRIORITIES=('banner','role','open')
PRICED_GEAR_TIERS=3
for b in data['botCommand']:
    name=f'botCommand/{b["id"]}'
    if b['gearPriority'] not in GEAR_PRIORITIES: errors.append(f'{name}/gearPriority: {b["gearPriority"]!r} is not one of {", ".join(GEAR_PRIORITIES)}')
    if isinstance(b['gearReserveTroops'],bool) or not isinstance(b['gearReserveTroops'],int) or b['gearReserveTroops']<0: errors.append(f'{name}/gearReserveTroops: {b["gearReserveTroops"]!r} must be a whole number of troops, 0 or more')
    if isinstance(b['gearMaxTier'],bool) or not isinstance(b['gearMaxTier'],int) or not 1<=b['gearMaxTier']<=PRICED_GEAR_TIERS: errors.append(f'{name}/gearMaxTier: {b["gearMaxTier"]!r} must be a whole tier from 1 to {PRICED_GEAR_TIERS} (the tiers rarities.gearPrice2 and gearPrice3 price)')
for b in data['botBanners']:
    name=f'botBanners/{b["id"]}';parts=b['id'].split('_')
    mixed=parts[0]=='mix' and len(parts)>=4
    allowed=set(parts[1:-1]) if mixed else {b['house']}
    if b['house'] not in houses or b['difficulty'] not in difficulties: errors.append(f'{name}: house or difficulty does not resolve')
    if b['id']!=(f'mix_{b["house"]}_{"_".join(parts[2:-1])}_{b["difficulty"]}' if mixed else f'{b["house"]}_{b["difficulty"]}') or len(allowed)!=len(parts)-2 and mixed or not allowed<=set(houses): errors.append(f'{name}: id must be <house>_<difficulty>, or mix_<house>_<second house>[_<more Houses>]_<difficulty> with house the first House and each House once')
    slots=b['slots'].split(',')
    if len(slots)!=squad: errors.append(f'{name}: {len(slots)} entries, not {squad}')
    pending,weights,units,last=0,0,{},None
    for s in slots:
        if s=='^':
            if pending==0: errors.append(f'{name}: a ^ that follows no Epic or Legendary'); break
            pending-=1;continue
        if pending: errors.append(f'{name}: {last} is missing a ^'); break
        if s not in rarityOf or houseOf[s] not in allowed: errors.append(f'{name}: {s} is not a troop of this banner\'s Houses'); break
        last=s;w=rarities.get(rarityOf[s],{}).get('slotWeight',1);weights+=w;pending=w-1;units[rarityOf[s]]=units.get(rarityOf[s],0)+1
    else:
        if pending: errors.append(f'{name}: {last} is missing a ^')
        if weights!=squad: errors.append(f'{name}: slot weights sum to {weights}, not {squad}')
        for r,n in units.items():
            if n>rarities.get(r,{}).get('bannerMax',squad): errors.append(f'{name}: {n} {r} units, over bannerMax {rarities[r]["bannerMax"]}')
for d in sorted(difficulties):
    if not any(b['difficulty']==d and all(houses.get(houseOf.get(s),{}).get('fieldable') for s in b['slots'].split(',') if s!='^') for b in data['botBanners']): errors.append(f'botBanners: no {d} banner fields only fieldable Houses (Campaign.botBanner picks from those)')
# The Long March (plan 2C.3): six battles in one order, each on a map index, in one of the four 1.0 modes, at a botCommand difficulty, with one
# bonusObjectives row of its own. A cell the plan leaves open is listed in the row's placeholder column ('none' when the row is settled).
MARCH_MODES={'domination','siege','survival','convoy'}
BONUS_KINDS={'hold_all','lose_at_most','stage_by','never_rolled','win_within','kill_unit'}
SIEGE_STAGES=('yard','gate','keep')
bonus_rows={o['id']:o for o in data.get('bonusObjectives',[])}
march=data['campaignBattles']
if sorted(r['order'] for r in march)!=list(range(1,len(march)+1)): errors.append('campaignBattles: orders must run 1, 2, 3 and on, each once (MarchMath reads the order lock from them)')
used_bonus=[r['bonus'] for r in march]
for r in march:
    where=f'campaignBattles/{r["id"]}'
    if r['chapter']<1: errors.append(f'{where}: chapter {r["chapter"]} is under 1')
    if r['map'] not in range(len(data['maps'])): errors.append(f'{where}: map {r["map"]} is not a maps index')
    if r['mode'] not in MARCH_MODES: errors.append(f'{where}: mode {r["mode"]} is not one of {sorted(MARCH_MODES)}')
    if r['difficulty'] not in difficulties: errors.append(f'{where}: difficulty {r["difficulty"]} is not a botCommand id')
    if r['size']<1: errors.append(f'{where}: size {r["size"]} is under 1')
    if r['bonus'] not in bonus_rows: errors.append(f'{where}: bonus {r["bonus"]} is not a bonusObjectives id')
    elif used_bonus.count(r['bonus'])!=1: errors.append(f'{where}: bonus {r["bonus"]} is named by {used_bonus.count(r["bonus"])} battles; each battle has its own')
    if r['house'] not in houses and not (r['house']=='all' and 'house' in r['placeholder'] and bonus_rows.get(r['bonus'],{}).get('kind')=='tbd'): errors.append(f'{where}: house {r["house"]} is not a houses id (all is allowed only as a placeholder whose bonus is still tbd, since the shards need one House)')
    if not r['placeholder'].strip(): errors.append(f'{where}: placeholder is blank; it lists the open cells, or reads none')
    # allies (Kort 2026-11-02, Q76): none, or the botBanners rows the commander's bot allies field, one per allied seat (size - 1); those rows are reserved and never join a tier's pool (Campaign.fieldableBotBanners)
    if r['allies']!='none':
        ally=r['allies'].split(',');banner_ids={b['id'] for b in data['botBanners']}
        if len(ally)!=r['size']-1 or len(set(ally))!=len(ally): errors.append(f'{where}: allies names {len(ally)} rows; it needs {r["size"]-1} distinct rows, one per allied bot seat (size - 1)')
        for a in ally:
            if a not in banner_ids: errors.append(f'{where}: allies {a} is not a botBanners id')
    stage_bonus=bonus_rows.get(r['bonus'],{})
    if stage_bonus.get('kind')=='stage_by' and (r['mode']!='siege' or stage_bonus['target'] not in SIEGE_STAGES): errors.append(f'{where}: a stage_by bonus needs a siege battle and a target of {", ".join(SIEGE_STAGES)} (SiegeMode feeds those stages)')
for o in bonus_rows.values():
    where=f'bonusObjectives/{o["id"]}'
    if o['id'] not in used_bonus: errors.append(f'{where}: no campaignBattles row names it')
    if o['kind'] not in BONUS_KINDS and not (o['kind']=='tbd' and 'kind' in o['placeholder']): errors.append(f'{where}: kind {o["kind"]} is not one of {sorted(BONUS_KINDS)} (tbd only as a listed placeholder)')
    if min(o['value'],o['shards'],o['gold'])<0: errors.append(f'{where}: value, shards and gold must be 0 or more')
    if o['kind']=='kill_unit' and o['target'] not in houseOf: errors.append(f'{where}: a kill_unit target must be a troop id')
    if o['kind'] not in ('kill_unit','stage_by') and o['target']!='none': errors.append(f'{where}: target must be none for kind {o["kind"]}')
    if o['kind'] in ('lose_at_most','stage_by','win_within') and o['value']<1: errors.append(f'{where}: kind {o["kind"]} needs a value of 1 or more')
    if not o['placeholder'].strip(): errors.append(f'{where}: placeholder is blank; it lists the open cells, or reads none')
# Troop card text (Task 12): one troopNotes row per troop, in troops order, and nothing else; the economy model row (houses design 4.9, NOT_SHIPPED) has every input.
if [n['id'] for n in data.get('troopNotes',[])]!=[t['id'] for t in data['troops']]: errors.append('troopNotes: one row per troops row, in the same order, with the same ids')
ECONOMY_INPUTS={'matchesPerHour','winRate','hoursPerPlayDay','playDaysPerWeek','matchGold','winGold','dailyGold','weeklyGold','runs'}
if len(data.get('economyModel',[]))!=1 or not ECONOMY_INPUTS<=set(data['economyModel'][0]): errors.append('economyModel: one row with '+', '.join(sorted(ECONOMY_INPUTS))+' (tools/economy_model.py reads it)')
elif not 0<=data['economyModel'][0]['winRate']<=1 or any(data['economyModel'][0][k]<=0 for k in ECONOMY_INPUTS-{'winRate'}): errors.append('economyModel: winRate within 0 to 1 and every other input above 0')
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
    if r['page'] not in ('home','contracts','chapter','match','recruit','friends','army','train','forge'): errors.append(f'{where}: page {r["page"]} is not a camp page')
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
# Commander gear (gear foundation plan Task 1): the catalogue is data, so every name, number and link between the gear tables is checked here.
GEAR_RARITIES=['common','uncommon','rare','epic','legendary']
GEAR_ARMOUR=['helm','chest','legs','boots']
GEAR_STATS=['health','damage','armour','speed','ranged','healing','command']
GEAR_ID=re.compile(r'[a-z0-9_]{1,24}')
rarity_rows=data.get('gearRarities',[])
if [r['id'] for r in rarity_rows]!=GEAR_RARITIES: errors.append(f'gearRarities: the ids must be exactly {", ".join(GEAR_RARITIES)}, in that order')
for i,r in enumerate(rarity_rows):
    rid=f'gearRarities/{r["id"]}'
    if not (0<r['statMin']<=r['statMax']): errors.append(f'{rid}/statMin: needs 0 < statMin <= statMax, got {r["statMin"]} and {r["statMax"]}')
    if not (0<r['breakdownPct']<=1): errors.append(f'{rid}/breakdownPct: {r["breakdownPct"]} is outside (0, 1]')
    if r['power']<1: errors.append(f'{rid}/power: {r["power"]} is below 1')
    if r['rollWeight']<=0: errors.append(f'{rid}/rollWeight: {r["rollWeight"]} must be above 0')
    if not re.fullmatch(r'#[0-9a-f]{6}',str(r['border'])): errors.append(f'{rid}/border: {r["border"]!r} is not #rrggbb in lower case')
    if i:
        before=rarity_rows[i-1]
        for col in ('statMin','statMax','breakdownPct','power'):
            if r[col]<=before[col]: errors.append(f'{rid}/{col}: {r[col]} does not rise above {before["id"]} ({before[col]})')
set_rows=data.get('gearSets',[])
set_ids={s['id'] for s in set_rows}
item_rows=data.get('gearItems',[])
items_by_id={i['id']:i for i in item_rows}
for it in item_rows:
    iid=f'gearItems/{it["id"]}'
    if not GEAR_ID.fullmatch(str(it['id'])): errors.append(f'{iid}/id: {it["id"]!r} is not 1 to 24 characters of [a-z0-9_]')
    if it['slot'] not in GEAR_ARMOUR+['main','off']: errors.append(f'{iid}/slot: {it["slot"]!r} is not one of {", ".join(GEAR_ARMOUR+["main","off"])}')
    for col in GEAR_STATS:
        if it[col]<0: errors.append(f'{iid}/{col}: {it[col]} is negative')
    if it['slot'] in GEAR_ARMOUR:
        if it['set'] not in set_ids: errors.append(f'{iid}/set: {it["set"]!r} is not a gearSets id')
        if it['hands']!=1: errors.append(f'{iid}/hands: an armour piece has hands 1, got {it["hands"]}')
    else:
        if it['set']!='-': errors.append(f'{iid}/set: a weapon belongs to no set, so set must be -')
        if it['hands'] not in (1,2): errors.append(f'{iid}/hands: {it["hands"]} is not 1 or 2')
        elif it['hands']==2 and it['slot']!='main': errors.append(f'{iid}/hands: only a main-hand item can have hands 2')
for s in set_rows:
    for slot in GEAR_ARMOUR:
        n=sum(1 for it in item_rows if it['set']==s['id'] and it['slot']==slot)
        if n!=1: errors.append(f'gearSets/{s["id"]}: set {s["id"]} has {n} {slot} pieces in gearItems, it needs exactly one')
    sid=f'gearSets/{s["id"]}'
    main=items_by_id.get(s['starterMain'])
    if main is None or main['slot']!='main': errors.append(f'{sid}/starterMain: {s["starterMain"]!r} is not a main-hand gearItems id')
    if s['starterOff']!='-':
        off=items_by_id.get(s['starterOff'])
        if off is None or off['slot']!='off': errors.append(f'{sid}/starterOff: {s["starterOff"]!r} is not - or an off-hand gearItems id')
    if main is not None and main['hands']==2 and s['starterOff']!='-': errors.append(f'{sid}/starterOff: the starter main {s["starterMain"]} is two-handed, so starterOff must be -')
relic_rows=data.get('relics',[])
for r in relic_rows:
    rid=f'relics/{r["id"]}'
    if not GEAR_ID.fullmatch(str(r['id'])): errors.append(f'{rid}/id: {r["id"]!r} is not 1 to 24 characters of [a-z0-9_]')
    if r['cooldownTicks']<=0: errors.append(f'{rid}/cooldownTicks: {r["cooldownTicks"]} must be above 0')
    if r['role'] not in set_ids: errors.append(f'{rid}/role: {r["role"]!r} is not a gearSets id')
if 'classes' in data:
    for c in data['classes']:
        n=sum(1 for s in set_rows if c['id'] in str(s['classes']).split(','))
        if n!=1: errors.append(f'gearSets: class {c["id"]} appears in {n} classes lists, it needs exactly one')
        n=sum(1 for r in relic_rows if r['fromClass']==c['id'])
        if n!=1: errors.append(f'relics: class {c["id"]} is the fromClass of {n} relics, it needs exactly one')
# Gear combat (gear-combat plan Task 1). Draft numbers, for Kort's balance review (part g). The code knows only the effect ids below; the sheet picks and tunes them.
RELIC_EFFECTS=('stand_fast','rally_horn','grave_bloom','forward_supply','mark_prey','bloodrush','dash','shove','heal_pulse','volley','self_heal','war_cry','iron_skin')
SET_EFFECTS=('surrounded_guard','taunt_aura','heal_over_time','cleanse','draw_speed','pierce','command_radius','formation_charge')
PERK_EFFECTS=('second_chance','grudge','quickened_relic','heavy_hand','last_light','hawkeye','oathkeeper','trailblazer')
family_rows=data.get('gearFamilies',[])
family_ids={f['id'] for f in family_rows}
for it in item_rows:
    if it['slot'] in ('main','off') and it['family'] not in family_ids: errors.append(f'gearItems/{it["id"]}/family: {it["family"]!r} has no gearFamilies row')
for slot in GEAR_ARMOUR:
    if slot not in family_ids: errors.append(f'gearFamilies: the armour slot {slot} needs a stand-in row')
for f in family_rows:
    fid=f'gearFamilies/{f["id"]}'
    if f['id'] not in GEAR_ARMOUR and not any(it['family']==f['id'] for it in item_rows): errors.append(f'{fid}: no gearItems row has family {f["id"]}, and it is not an armour slot')
    if not re.fullmatch(r'minecraft:[a-z0-9_]+',str(f['item'])): errors.append(f'{fid}/item: {f["item"]!r} is not a minecraft: item id')
    for col in ('attackSpeed','damageMult','drawMult','auraMult'):
        if f[col]<=0: errors.append(f'{fid}/{col}: {f[col]} must be above 0')
    for col in ('sweepRadius','sweepShare','cooldownTicks','range','healBase'):
        if f[col]<0: errors.append(f'{fid}/{col}: {f[col]} is negative')
    if f['sweepShare']>1: errors.append(f'{fid}/sweepShare: {f["sweepShare"]} is above 1')
for b in data.get('gearSetBonuses',[]):
    bid=f'gearSetBonuses/{b["id"]}'
    if b['set'] not in set_ids: errors.append(f'{bid}/set: {b["set"]!r} is not a gearSets id')
    if b['pieces'] not in (2,4): errors.append(f'{bid}/pieces: {b["pieces"]} is not 2 or 4')
    if b['effect'] not in SET_EFFECTS: errors.append(f'{bid}/effect: {b["effect"]!r} is not one of {", ".join(SET_EFFECTS)}')
    if len(b['text'])>90: errors.append(f'{bid}/text: {len(b["text"])} characters, over 90')
for s in set_rows:
    for n in (2,4):
        if not any(b['set']==s['id'] and b['pieces']==n for b in data.get('gearSetBonuses',[])): errors.append(f'gearSetBonuses: set {s["id"]} has no {n}-piece row')
perk_items=[]
for k in data.get('gearPerks',[]):
    kid=f'gearPerks/{k["id"]}'
    it=items_by_id.get(k['item'])
    if it is None: errors.append(f'{kid}/item: {k["item"]!r} is not a gearItems id')
    elif it['slot'] not in GEAR_ARMOUR+['main','off']: errors.append(f'{kid}/item: {k["item"]} sits in slot {it["slot"]}, not a worn slot')
    if k['effect'] not in PERK_EFFECTS: errors.append(f'{kid}/effect: {k["effect"]!r} is not one of {", ".join(PERK_EFFECTS)}')
    if k['item'] in perk_items: errors.append(f'{kid}/item: {k["item"]} already has a perk; perks are fixed, one per item')
    perk_items.append(k['item'])
    if len(k['text'])>90: errors.append(f'{kid}/text: {len(k["text"])} characters, over 90')
relic_ids={r['id'] for r in relic_rows}
tuning_rows=data.get('relicTuning',[])
for t in tuning_rows:
    tid=f'relicTuning/{t["id"]}'
    if t['id'] not in relic_ids: errors.append(f'{tid}/id: {t["id"]!r} is not a relics id')
    if t['effect'] not in RELIC_EFFECTS: errors.append(f'{tid}/effect: {t["effect"]!r} is not one of {", ".join(RELIC_EFFECTS)}')
    r=next((r for r in relic_rows if r['id']==t['id']),None)
    if r is not None and r['cooldownTicks']<t['durationTicks']: errors.append(f'{tid}/durationTicks: {t["durationTicks"]} is above the relic cooldown {r["cooldownTicks"]}')
for r in relic_rows:
    n=sum(1 for t in tuning_rows if t['id']==r['id'])
    if n!=1: errors.append(f'relicTuning: relic {r["id"]} has {n} tuning rows, it needs exactly one')
if len(data.get('gearCombat',[]))!=1: errors.append(f'gearCombat: needs exactly one row, has {len(data.get("gearCombat",[]))}')
for g in data.get('gearCombat',[]):
    if g['surroundedCount']<1: errors.append(f'gearCombat/{g["id"]}/surroundedCount: {g["surroundedCount"]} is below 1')
    if not (0<=g['tauntShare']<=1): errors.append(f'gearCombat/{g["id"]}/tauntShare: {g["tauntShare"]} is outside 0 to 1')
for f in data.get('forge',[]):
    fid=f'forge/{f["id"]}'
    if f['maxSlots']<f['startSlots'] or f['rowSlots']<1 or (f['maxSlots']-f['startSlots'])%f['rowSlots']!=0: errors.append(f'{fid}/maxSlots: {f["maxSlots"]} is not startSlots {f["startSlots"]} plus a whole number of rows of {f["rowSlots"]}')
    if f['pity']<1: errors.append(f'{fid}/pity: {f["pity"]} is below 1')
    if f['relicSlots']!=3: errors.append(f'{fid}/relicSlots: {f["relicSlots"]} must be 3')
    if not (0<=f['starterQuality']<=100): errors.append(f'{fid}/starterQuality: {f["starterQuality"]} is outside 0 to 100')
    if not (0<f['brokenStatPct']<=1): errors.append(f'{fid}/brokenStatPct: {f["brokenStatPct"]} is outside (0, 1]')
# Blacksmith lines (gear screen): the Forge tab shows one, so the sheet needs at least one, each short enough for its line.
if not data.get('blacksmithLines'): errors.append('blacksmithLines: the Forge tab needs at least one blacksmith line')
for r in data.get('blacksmithLines',[]):
    text=str(r.get('text',''))
    if not text.strip(): errors.append(f'blacksmithLines/{r["id"]}/text: the line is empty')
    elif len(text)>90: errors.append(f'blacksmithLines/{r["id"]}/text: {len(text)} characters, at most 90 fit the Forge tab')
if errors:
    print('\n'.join(errors)); sys.exit(1)
def typ(v): return 'boolean' if isinstance(v,bool) else 'int' if isinstance(v,int) else 'double' if isinstance(v,float) else 'String'
def literal(v): return json.dumps(v,ensure_ascii=True)
# Validated above, but process metadata rather than game data: never shipped as Spec records.
NOT_SHIPPED={'checks','revisionChecks','playtestMilestone','economyModel'}
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
