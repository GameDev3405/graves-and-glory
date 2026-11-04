import json,random,itertools,sys
d=json.load(open('sheets/design.json'))
G={'leather':(1,3,0,0),'chainmail':(2,5,0,0),'iron':(2,6,0,0),'golden':(2,5,0,0),'diamond':(3,8,2,0),'netherite':(3,8,3,.1),'air':(0,0,0,0)}
def gear(n):
    k=n.replace('minecraft:','').split('_')[0];return G[k]
T={}
for t in d['troops']:
    h=gear(t['helmet'])[0];c=gear(t['chest'])[1]
    hh=gear(t['helmet']);cc=gear(t['chest'])
    A=hh[0]+cc[1];Tg=hh[2]+cc[2]
    T[t['id']]=dict(t,A=A,Tg=Tg,kbr=hh[3]+cc[3])
def red(dmg,A,Tg):
    if A==0:return dmg
    return dmg*(1-min(20,max(A/5,A-dmg/(2+Tg/4)))/25)
if sys.argv[1]=='table':
    print('troop|house|hp|armour|tough|taken@4|@6|@9|effHP@5|cost|effHP/gold')
    for i,t in T.items():
        f=lambda x:red(x,t['A'],t['Tg'])/x
        e=t['health']/f(5)
        print(f"{i}|{t['house']}|{t['health']}|{t['A']}|{t['Tg']}|{f(4):.2f}|{f(6):.2f}|{f(9):.2f}|{e:.0f}|{t['cost']}|{e/t['cost']:.2f}")
    sys.exit()
def lineup(house,gold,mode):
    ids=[i for i,t in T.items() if t['house']==house and t['group'] in('infantry','ranged') and t['cost']<=60 and t['damage']>0]
    if not ids:return []
    out=[];g=0;k=0
    while True:
        i=ids[k%len(ids)];k+=1
        if g+T[i]['cost']>gold:
            if all(g+T[j]['cost']>gold for j in ids):break
            continue
        out.append(i);g+=T[i]['cost']
    return out
LOST={};ATT={}
def fight(la,lb,arm,ifr,rng):
    U=[]
    for team,l in((0,la),(1,lb)):
        for i in l:
            t=T[i];U.append(dict(t=t,team=team,hp=t['health'],cd=rng.randrange(t['cooldown']),reg=0,last=0,alive=True))
    for tick in range(6000):
        for u in U:
            if u['reg']>0:u['reg']-=1
        for u in U:
            if not u['alive']:continue
            t=u['t']
            if u['cd']>0:u['cd']-=1;continue
            en=[x for x in U if x['alive'] and x['team']!=u['team']]
            if not en:break
            v=rng.choice(en);u['cd']=t['cooldown']
            amt=t['damage']*rng.uniform(.9,1.1)
            if arm:amt=red(amt,v['t']['A'],v['t']['Tg'])
            h=t['house'];ATT[h]=ATT.get(h,0)+amt
            if ifr and v['reg']>10:
                if amt<=v['last']:LOST[h]=LOST.get(h,0)+amt;continue
                LOST[h]=LOST.get(h,0)+v['last'];amt,v['last']=amt-v['last'],amt
            else:
                v['reg']=20;v['last']=amt
            v['hp']-=amt
            if v['hp']<=0:v['alive']=False
        a=any(x['alive'] for x in U if x['team']==0);b=any(x['alive'] for x in U if x['team']==1)
        if not a or not b:return 0 if a else 1 if b else .5
    return .5
Hs=['gilded','graveborn','ironclad','tidecallers']
gold=int(sys.argv[2]);N=int(sys.argv[3])
for arm,ifr in((1,1),(1,0),(0,1),(0,0)):
    LOST.clear();ATT.clear();rng=random.Random(5);W={h:[0,0] for h in Hs}
    for a,b in itertools.combinations(Hs,2):
        la,lb=lineup(a,gold,0),lineup(b,gold,0)
        for n in range(N):
            if n%2:r=1-fight(lb,la,arm,ifr,rng)
            else:r=fight(la,lb,arm,ifr,rng)
            W[a][0]+=1-r;W[a][1]+=1;W[b][0]+=r;W[b][1]+=1
    if arm and ifr:print('  dmg lost to i-frames by attacker House: '+' '.join(f"{h} {100*LOST.get(h,0)/ATT[h]:.0f}%" for h in Hs))
    print(f"armour={arm} iframes={ifr}: "+" ".join(f"{h} {100*W[h][0]/W[h][1]:.1f}" for h in Hs))
