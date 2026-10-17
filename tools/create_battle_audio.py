"""Original synthesized cues; no borrowed recordings or paid services."""
import math,random,wave,struct,subprocess,pathlib,json
r=pathlib.Path(__file__).resolve().parents[1];assets=r/'src/main/resources/assets/gravesandglory';out=assets/'sounds';out.mkdir(parents=True,exist_ok=True)
scratch=r/'build/audio';scratch.mkdir(parents=True,exist_ok=True)
random.seed(521)
def cue(name,length,events):
 rate=44100;data=[]
 for i in range(int(length*rate)):
  t=i/rate;value=0
  for start,duration,freq,amp,kind in events:
   u=t-start
   if 0<=u<duration:
    env=min(1,u/.025)*min(1,(duration-u)/.22)*math.exp(-u/(duration*1.3))
    if kind=='horn':v=sum(math.sin(2*math.pi*freq*k*u)/(k**1.7) for k in range(1,6))
    elif kind=='chime':v=math.sin(2*math.pi*freq*u)+.3*math.sin(2*math.pi*freq*2.76*u)
    else:v=math.sin(2*math.pi*(freq*u+25*(1-math.exp(-u*18))))*.65+(random.random()*2-1)*.25*math.exp(-u*15)
    value+=v*env*amp
  data.append(struct.pack('<h',int(math.tanh(value)*26000)))
 wav=scratch/(name+'.wav')
 with wave.open(str(wav),'wb') as w:w.setparams((1,2,rate,0,'NONE','not compressed'));w.writeframes(b''.join(data))
 subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(out/(name+'.ogg'))],check=True)
cue('battle_intro',6,[(0,.8,60,.6,'drum'),(.7,1.8,146.83,.23,'horn'),(.7,1.8,220,.15,'horn'),(2.1,1,73,.4,'drum'),(2.6,2.2,196,.23,'horn'),(2.6,2.2,293.66,.16,'horn'),(4.5,1.3,392,.18,'chime'),(4.65,1.2,587.33,.12,'chime')])
cue('unit_reveal',2.1,[(0,.8,293.66,.2,'chime'),(.16,.8,392,.2,'chime'),(.32,1.3,587.33,.24,'chime'),(.5,1.4,783.99,.12,'chime')])
cue('victory',3.2,[(0,.9,196,.22,'horn'),(.4,.9,246.94,.22,'horn'),(.8,1.8,293.66,.25,'horn'),(.8,1.8,392,.15,'horn'),(.8,.7,55,.4,'drum')])
(assets/'sounds.json').write_text(json.dumps({n:{'subtitle':'subtitles.gravesandglory.'+n,'sounds':[{'name':'gravesandglory:'+n,'stream':False}]} for n in ['battle_intro','unit_reveal','victory']},indent=2))
p=assets/'lang/en_us.json';d=json.loads(p.read_text());d.update({'subtitles.gravesandglory.battle_intro':'Battle horns sound','subtitles.gravesandglory.unit_reveal':'A new unit joins your banner','subtitles.gravesandglory.victory':'Battle ends'});p.write_text(json.dumps(d,indent=2))
print('Created three original Ogg sound cues')
