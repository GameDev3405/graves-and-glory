import json,pathlib
r=pathlib.Path(__file__).resolve().parents[1];p=r/'sheets/design.json';d=json.loads(p.read_text())
d['economy'][0].update(passiveGold=8,pointGold=1,killGold=0,objectiveIncomeCap=2)
d['rules'][0].update(scoreInterval=80,roundSeconds=600)
d['terrainStyle']=[{'id':'authored','warpAmplitude':7.0,'warpFrequency':0.035,'roughness':1.4,'boundaryRise':12.0,'roadCurve':4.0}]
d['pings']=[{'id':i,'label':label,'seconds':8,'cooldownSeconds':2} for i,label in [('attack','Attack'),('defend','Defend'),('help','Help'),('moving','Moving here')]]
d['combatDiscipline']=[{'id':'captain','damageScale':0.6,'targetMemoryTicks':40,'rangedObstructionStep':4.0,'shieldFrontalScale':0.55,'shieldAngleCos':0.65,'spellWindupTicks':20,'spellRadius':2.5,'recoveryIncome':'baseline','enemyKnowledge':'visible_only'}]
for s in d['structures']:
    if s['id']=='bridge_north':s['palette']='wood';s['depth']=9
    if s['id']=='bridge_south':s['depth']=16
    if s['kind']=='bridge' and s['z']>40:s['palette']='ford';s['depth']=18
d['structures'].extend([
 {'id':'hamlet_mill','kind':'mill','x':-20,'z':-64,'width':10,'depth':10,'height':8,'palette':'wood','elevation':64},
 {'id':'hamlet_farm','kind':'farm','x':-74,'z':-45,'width':18,'depth':13,'height':1,'palette':'wood','elevation':64},
 {'id':'gatehouse_breach','kind':'breach','x':-18,'z':26,'width':5,'depth':16,'height':7,'palette':'stone','elevation':68}])
p.write_text(json.dumps(d,indent=2)+'\n')
