from pathlib import Path
r=Path(__file__).resolve().parents[1];b=r/'src/main/java/gg/gravesandglory'
p=b/'Campaign.java';s=p.read_text();s=s.replace('Map<String,Integer> army=new LinkedHashMap<>();','Map<String,Integer> army=new LinkedHashMap<>();\n        Map<String,List<String>> deployment=new HashMap<>();')
needle='        void sanitize(){'
code='''        List<String> lineup(int team){
            if(deployment==null)deployment=new HashMap<>();String faction=Spec.FACTIONS[team].id();List<String> line=deployment.get(faction);Map<String,Integer> actual=new HashMap<>();
            if(line!=null)for(String id:line)if(!id.equals("empty"))actual.merge(id,1,Integer::sum);
            boolean valid=line!=null&&line.size()==20;for(var t:Spec.TROOPS)if(t.faction().equals(faction)&&actual.getOrDefault(t.id(),0)!=army.getOrDefault(t.id(),0))valid=false;
            if(!valid){line=new ArrayList<>();for(var t:Spec.TROOPS)if(t.faction().equals(faction))for(int i=0;i<army.getOrDefault(t.id(),0)&&line.size()<20;i++)line.add(t.id());while(line.size()<20)line.add("empty");deployment.put(faction,line);}return line;
        }
'''
s=s.replace(needle,code+needle)
s=s.replace('cmd.startsWith("army:")||cmd.startsWith("class:")','cmd.startsWith("army:")||cmd.startsWith("slot:")||cmd.startsWith("class:")')
needle='        if(cmd.startsWith("army:")){'
code='''        if(cmd.startsWith("slot:")){
            String[] parts=cmd.split(":");if(parts.length!=3)return true;int slot;try{slot=Integer.parseInt(parts[1]);}catch(Exception e){return true;}
            if(slot<0||slot>=20)return true;String id=parts[2];var type=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(id)&&t.faction().equals(Spec.FACTIONS[squad.team].id())).findFirst().orElse(null);
            if(!id.equals("empty")&&(type==null||!unlocked(squad,id)))return true;
            var line=pr.lineup(squad.team);line.set(slot,id);for(var t:Spec.TROOPS)if(t.faction().equals(Spec.FACTIONS[squad.team].id()))pr.army.put(t.id(),(int)line.stream().filter(t.id()::equals).count());save(p.getUuid());return true;
        }
'''
s=s.replace(needle,code+needle)
s=s.replace('pr.army.put(spec.id(),n+delta);save(p.getUuid());','var line=pr.lineup(squad.team);if(delta>0){int slot=line.indexOf("empty");if(slot>=0)line.set(slot,spec.id());}else{int slot=line.lastIndexOf(spec.id());if(slot>=0)line.set(slot,"empty");}pr.army.put(spec.id(),n+delta);save(p.getUuid());')
p.write_text(s)
p=b/'GravesAndGlory.java';s=p.read_text().replace('Vec3d lastPosition,lastGoal;','Vec3d lastPosition,lastGoal,deployed;');s=s.replace('lastPosition=m.getPos();','lastPosition=m.getPos();deployed=m.getPos();')
start=s.index('            for(var spec:Spec.TROOPS)if(spec.faction()',s.index('        void reinforce(Squad s){'));end=s.index('\n        Unit spawn(',start)
s=s[:start]+'''            List<String> line;if(s.bot){line=new ArrayList<>();for(var t:Spec.TROOPS)if(t.faction().equals(Spec.FACTIONS[s.team].id()))for(int n=0;n<t.count();n++)line.add(t.id());}else line=campaign.profile(s.owner).lineup(s.team);
            for(String id:line){final String type=id;int position=slot++;if(position>=RosterMath.LEGACY_BANNER_SIZE||occupied.contains(position)||id.equals("empty")||!campaign.unlocked(s,id))continue;
                var spec=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(type)).findFirst().orElse(null);if(spec==null)continue;
                double x=base(s.team).x+(s.team==0?1:-1)*(20-position/5*3),z=base(s.team).z+(index-1.5)*22+(position%5-2)*2.5;
                spawn(s,spec,position,new Vec3d(x,Arena.ground((int)x,(int)z)+1,z));
            }
        }
''' +s[end:]
s=s.replace('Assignment a=assignment(u);var f=', 'Assignment a=assignment(u);if(!u.squad.bot&&u.squad.order.equals("hold")&&!u.squad.assignments.containsKey(u.spec.group()))return u.deployed;var f=')
p.write_text(s)
p=b/'CampaignLobbyScreen.java';s=p.read_text();s=s.replace('int tab=0;', 'String selectedUnit="knight";final List<ButtonWidget> slots=new ArrayList<>();\n    int tab=0;');s=s.replace('start=null;roll=null;hostControls.clear();','start=null;roll=null;slots.clear();hostControls.clear();')
start=s.index('        }else if(tab==1){');end=s.index('        }else {',start)
s=s[:start]+'''        }else if(tab==1){
            int y=99,row=0,t=Math.max(0,team),left=width/2+8;
            if(Arrays.stream(Spec.TROOPS).noneMatch(u->u.id().equals(selectedUnit)&&u.faction().equals(Spec.FACTIONS[t].id()))&&!selectedUnit.equals("empty"))selectedUnit=t==0?"knight":"revenant";
            for(var unit:Spec.TROOPS)if(unit.faction().equals(Spec.FACTIONS[t].id())){String id=unit.id();int py=y+row*24;
                var choose=button((selectedUnit.equals(id)?"> ":"")+unit.name(),left,py,width-left-105,b->{selectedUnit=id;clearAndInit();});choose.active=CampaignClient.profile.unlocked.contains(id)||CampaignClient.mode.equals("ai");
                button("-",width-96,py,24,b->send("army:"+id+":remove"));button("+",width-43,py,24,b->send("army:"+id+":add"));row++;
            }
            for(int slot=0;slot<20;slot++){final int index=slot;slots.add(button("",20+(slot%5)*55,104+(slot/5)*31,51,b->send("slot:"+index+":"+selectedUnit)));}
            button("Empty slot",20,245,115,b->{selectedUnit="empty";clearAndInit();});
            roll=button("ROLL UNIT / 100 GOLD",16,height-52,Math.min(220,width-32),b->send("roll"));
''' +s[end:]
s=s.replace('        if(running)close();','''        if(tab==1){var line=CampaignClient.profile.lineup(Math.max(0,team));for(int i=0;i<slots.size();i++){String id=line.get(i);var spec=Arrays.stream(Spec.TROOPS).filter(u->u.id().equals(id)).findFirst().orElse(null);slots.get(i).setMessage(Text.literal(spec==null?"-":spec.name().replace("Dawn ","").replace("Dusk ","").substring(0,Math.min(7,spec.name().replace("Dawn ","").replace("Dusk ","").length()))));}}
        if(running)close();''')
start=s.index('        }else if(tab==1){',s.index('@Override public void render'));end=s.index('        }else {',start)
s=s[:start]+'''        }else if(tab==1){
            text(g,(team==1?"DUSK":"DAWN")+" / "+CampaignClient.profile.count(Math.max(0,team))+" OF 20 SELECTED",16,74,width-32,GOLD);
            text(g,"FRONT / toward the enemy",20,91,285,MUTED);text(g,"REAR",20,231,285,MUTED);
            text(g,"Select a unit, then place it in a slot.",20,274,width-32,MUTED);int row=0;
            for(var unit:Spec.TROOPS)if(unit.faction().equals(Spec.FACTIONS[Math.max(0,team)].id())){int y=105+row*24;boolean unlocked=CampaignClient.profile.unlocked.contains(unit.id());
                text(g,unlocked?"":CampaignClient.mode.equals("ai")?"Trial":"Locked",width/2+8,y+14,100,MUTED);g.drawCenteredTextWithShadow(textRenderer,""+CampaignClient.profile.army.getOrDefault(unit.id(),0),width-58,y,GOLD);row++;}
            text(g,"100 gold / equal odds / no duplicates / permanent unlocks",16,height-27,width-32,MUTED);
''' +s[end:]
p.write_text(s)
print('Editable twenty-slot army deployment added')
