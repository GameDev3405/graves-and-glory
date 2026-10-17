import json,pathlib
r=pathlib.Path(__file__).resolve().parents[1]
p=r/'sheets/design.json';d=json.loads(p.read_text())
d['groups']=[{'id':i,'label':l,'role':role} for i,l,role in [('all','All troops','all'),('infantry','Infantry','frontline'),('ranged','Ranged','ranged'),('support','Support','support')]]
for t in d['troops']: t['group']='support' if t['magic'] else 'ranged' if t['range']>3 else 'infantry'
for o in d['orders']:
    o['label']={'follow':'Follow','move':'Move','attack':'Attack','hold':'Hold','rush':'Charge'}[o['id']]
    if o['id']=='rush': o['id']='charge'
    o['engage']=o['id']!='move'
    o['engageRadius']={'follow':10,'move':0,'attack':24,'hold':10,'charge':32}[o['id']]
    o['meaning']={'follow':'Guard the commander; keep support behind the frontline.','move':'Travel without pursuing unrelated enemies.','attack':'Advance and engage nearby threats, or focus a selected enemy.','hold':'Defend the chosen anchor; pursue only within ten blocks.','charge':'Commit to a selected enemy with reduced cohesion.'}[o['id']]
    o['targeted']=o['id']!='follow'
d['orders'].append({'id':'retreat','label':'Retreat','targeted':True,'engage':False,'engageRadius':0,'moveMultiplier':1.15,'meaning':'Disengage to the destination; ignore enemies and interrupt prior attacks.'})
purposes={'block':('Compact','Narrow frontage; infantry screen the rear.'),'line':('Line','Wide firing frontage; needs open ground.'),'wedge':('Wedge','Concentrated advance; exposed sides.'),'loose':('Loose','Spread out; less concentrated protection.')}
for f in d['formations']: f['label'],f['purpose']=purposes[f['id']]
d['commandInterface']=[{'id':'battlefield','selection':'all,infantry,ranged,support','orders':'move,attack,charge,hold,follow,retreat','targeting':'choose,aim,preview,confirm','facingStep':15,'targetRange':240,'acknowledgementSeconds':3,'lobby':'separate','confirm':'left_mouse','cancel':'right_mouse','preview':'world_slots'}]
d['playtestMilestone']=[{'id':i,'requirement':req,'acceptance':acc,'status':'pending'} for i,req,acc in [
 ('command','Visible battlefield group commands','Independent groups; preview and facing match destination; retreat interrupts combat.'),
 ('formations','Positioning changes engagements','Compare narrow approach, wide frontage, charge, spacing; no common mob pile.'),
 ('map','Three memorable combat spaces','Ground-level review of gatehouse, hamlet and ridge shrine; distinct crossings.'),
 ('information','Readable HUD and tactical map','Groups, ownership, contested state, destinations, teammate pings and feedback.'),
 ('economy','Ten-minute 2v2 playtest','Baseline recovery, limited snowball, little replacement busywork; four real players pending.'),
 ('ai','Trustworthy interactions','Bridge collision, retreat/reinforcements, death, occlusion, ranged rush, order spam, terrain and disconnect.'),
 ('identity','Faction identity and authored engineering','Telegraphed counterable faction mechanic; bounded repair/gate sites.'),
 ('quality','Honest experience evidence','Visual/audio review and measured playtest report; fun/balance not inferred from automated passes.')]]
p.write_text(json.dumps(d,indent=2)+'\n')
meta=r/'melty-project.json';m=json.loads(meta.read_text());m.update(status='private_revision_in_progress',readyToPublish=False,testPassed=False);meta.write_text(json.dumps(m,indent=2)+'\n')
progress=r/'PROGRESS.md';progress.write_text('# CURRENT: private playtest revision, not ready to publish\n\nCreator rejected previous completion standard. Implement the new command/control and experience milestone. Historical 0.1.0 test passes below apply ONLY to 0.1.0. No publication approval. All new milestone acceptance rows are pending until independently verified.\n\n'+progress.read_text(encoding='utf-8'),encoding='utf-8')
