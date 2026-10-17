import json
from pathlib import Path
r=Path(__file__).resolve().parents[1];p=r/'sheets/design.json';d=json.loads(p.read_text())
d['recruitmentView']=[{'id':'barracks','columns':'available,current_army,training_queue','refund':'cancel_full_disband_none','deployment':'barracks_then_current_group_order','disabledReason':'distance_or_gold_or_capacity','jobs':'frontline,ranged,support'}]
p.write_text(json.dumps(d,indent=2)+'\n')
p=r/'src/main/java/gg/gravesandglory/GravesAndGloryClient.java';s=p.read_text();a=s.index('        @Override protected void init(){',s.index('public static final class BarracksScreen'));b=s.index('    public static final class MapScreen',a)
s=s[:a]+'''        @Override protected void init(){
            controls.clear();x=width/2-200;y=height/2-108;int row=0;
            if(team>=0)for(int i=0;i<Spec.TROOPS.length;i++){var t=Spec.TROOPS[i];if(!t.faction().equals(Spec.FACTIONS[team].id()))continue;String id=t.id();int py=y+69+row*42;
                var train=button("Train / "+t.cost()+"g",x+10,py,118,b->send("train:"+id));var disband=button("Disband",x+142,py,110,b->send("disband:"+id));controls.add(new Controls(i,train,disband));row++;}
            cancel=button("Cancel / full refund",x+267,y+151,122,b->send("cancel-training"));button("Close",x+10,y+187,70,b->close());
        }
        @Override public void render(DrawContext g,int mx,int my,float delta){
            g.fill(0,0,width,height,0xa0081119);panel(g,x,y,400,216);g.drawTextWithShadow(textRenderer,"BARRACKS / "+gold+" GOLD / "+count+" OF 20",x+10,y+10,GOLD);
            g.drawTextWithShadow(textRenderer,nearBarracks?"Replacements deploy here and follow their group's order.":"Return to your barracks to change your army.",x+10,y+25,MUTED);
            String[] headings={"AVAILABLE","YOUR ARMY","TRAINING"};for(int i=0;i<3;i++){g.fill(x+7+i*132,y+42,x+131+i*132,y+181,0xff1c2a3a);g.drawTextWithShadow(textRenderer,headings[i],x+12+i*132,y+46,GOLD);}
            int row=0;for(Controls c:controls){var t=Spec.TROOPS[c.index()];int py=y+60+row*42;String job=t.group().equals("infantry")?"Frontline":t.group().equals("ranged")?"Ranged":"Support";
                g.drawTextWithShadow(textRenderer,t.name(),x+12,py,WHITE);g.drawTextWithShadow(textRenderer,job+" / "+roster[c.index()]+" alive",x+144,py,WHITE);
                c.train().active=nearBarracks&&gold>=t.cost()&&count+queued<20;c.disband().active=nearBarracks&&roster[c.index()]>0;
                String why=!nearBarracks?"Return to barracks":count+queued>=20?"Army + queue full":gold<t.cost()?"Need "+(t.cost()-gold)+" more gold":t.trainingSeconds()+"s training";
                c.train().setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(why)));c.disband().setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Disband one "+t.name()+". No gold refund.")));row++;
            }
            g.drawTextWithShadow(textRenderer,queued+" queued",x+272,y+67,WHITE);g.drawTextWithShadow(textRenderer,queued>0?"Next: "+trainingSeconds+"s":"Queue empty",x+272,y+82,MUTED);
            int qy=y+99;for(var c:controls)if(pending[c.index()]>0){var t=Spec.TROOPS[c.index()];g.drawTextWithShadow(textRenderer,pending[c.index()]+" "+CommandMode.label(t.group()),x+272,qy,WHITE);qy+=12;}
            g.drawTextWithShadow(textRenderer,"At your barracks",x+272,y+136,MUTED);
            g.drawTextWithShadow(textRenderer,"Disband: no refund / Cancel: all queued gold returned",x+90,y+194,MUTED);
            cancel.active=nearBarracks&&queued>0;super.render(g,mx,my,delta);
        }
    }
''' +s[b:];p.write_text(s)
