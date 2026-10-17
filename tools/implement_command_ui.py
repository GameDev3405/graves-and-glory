from pathlib import Path
import json
r=Path(__file__).resolve().parents[1];p=r/'src/main/java/gg/gravesandglory/GravesAndGloryClient.java';s=p.read_text()
s=s.replace('cycle,mapKey;', 'cycle,mapKey,rotateLeft,rotateRight,confirmOrder,cancelOrder;')
s=s.replace('String local=System.getenv', '''rotateLeft=key("rotate_left",GLFW.GLFW_KEY_LEFT_BRACKET);rotateRight=key("rotate_right",GLFW.GLFW_KEY_RIGHT_BRACKET);
        confirmOrder=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gravesandglory.confirm",InputUtil.Type.MOUSE,0,"category.gravesandglory"));
        cancelOrder=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gravesandglory.cancel",InputUtil.Type.MOUSE,1,"category.gravesandglory"));
        String local=System.getenv''')
s=s.replace('            client.execute(()->{ready=r;', '''            int groupCount=buf.readInt();if(groupCount!=3)return;var groups=new ArrayList<CommandMode.Group>();
            for(int i=0;i<groupCount;i++)groups.add(new CommandMode.Group(buf.readString(),buf.readInt(),buf.readFloat(),buf.readString(),buf.readString(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readBoolean()));
            client.execute(()->{CommandMode.groups=java.util.List.copyOf(groups);ready=r;''')
s=s.replace('while(menu.wasPressed())client.setScreen(new CommandScreen());while(attack.wasPressed())send("attack");while(follow.wasPressed())send("follow");', '''while(menu.wasPressed()){CommandMode.cancel();client.setScreen(running?new CommandScreen():new LobbyScreen());}
                while(attack.wasPressed())CommandMode.begin("attack");while(follow.wasPressed())CommandMode.issue("all","follow",CommandMode.formation,client.player.getPos(),client.player.getYaw(),-1);
                while(rotateLeft.wasPressed()){CommandMode.facingSet=true;CommandMode.facing-=Spec.COMMANDINTERFACE[0].facingStep();}
                while(rotateRight.wasPressed()){CommandMode.facingSet=true;CommandMode.facing+=Spec.COMMANDINTERFACE[0].facingStep();}
                CommandMode.tick(client);
                while(confirmOrder.wasPressed())if(CommandMode.active())CommandMode.confirm();
                while(cancelOrder.wasPressed())CommandMode.cancel();''')
start=s.index('        panel(g,w/2-155,7,310,37);');end=s.index('        panel(g,w-116',start)
s=s[:start]+'''        panel(g,w/2-175,7,350,37);g.drawCenteredTextWithShadow(font,"DAWN "+dawn+" / 300   -   DUSK "+dusk+" / 300",w/2,14,GOLD);
        for(int i=0;i<3;i++)g.drawTextWithShadow(font,Spec.POINTS[i].id()+": "+(progress[i]>0?"Capturing":owners[i]<0?"Neutral":owners[i]==0?"Dawn":"Dusk"),w/2-160+i*110,29,factionColor(owners[i]));
        CommandMode.hud(g,w,h);
        if(!running)g.drawCenteredTextWithShadow(font,"R: lobby / choose faction and start",w/2,48,WHITE);
''' +s[end:]
start=s.index('    public static final class CommandScreen');end=s.index('    public static final class BarracksScreen',start)
s=s[:start]+'''    public static final class LobbyScreen extends ArmyScreen {
        LobbyScreen(){super("Graves and Glory lobby");}
        @Override protected void init(){int x=width/2-120,y=height/2;button("Join Dawn",x,y,115,b->send("team:dawn"));button("Join Dusk",x+125,y,115,b->send("team:dusk"));button("Start round",x,y+30,240,b->{send("start");close();}).active=ready&&!running;}
        @Override public void render(DrawContext g,int mx,int my,float delta){panel(g,width/2-135,height/2-55,270,125);g.drawCenteredTextWithShadow(textRenderer,"GRAVES AND GLORY / 2v2",width/2,height/2-42,GOLD);g.drawCenteredTextWithShadow(textRenderer,"Hold objectives to score. First to 300 wins.",width/2,height/2-24,WHITE);super.render(g,mx,my,delta);}
    }
    public static final class CommandScreen extends ArmyScreen {
        CommandScreen(){super("Battlefield commands");}
        @Override protected void init(){
            int groupWidth=Math.min(100,(width-24)/4);int i=0;
            for(var group:Spec.GROUPS){String id=group.id();button((CommandMode.selected.equals(id)?"> ":"")+group.label(),width/2-2*groupWidth+i*groupWidth,8,groupWidth-3,b->{CommandMode.selected=id;clearAndInit();});i++;}
            i=0;for(String id:new String[]{"move","attack","charge","hold","follow","retreat"}){var order=Arrays.stream(Spec.ORDERS).filter(o->o.id().equals(id)).findFirst().orElseThrow();var btn=button(order.label(),8,45+i*25,82,b->{if(id.equals("follow")){CommandMode.issue(CommandMode.selected,id,CommandMode.formation,client.player.getPos(),client.player.getYaw(),-1);close();}else CommandMode.begin(id);});btn.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(order.meaning())));i++;}
            button("Map",width-76,45,68,b->client.setScreen(new MapScreen()));
            int cardWidth=Math.min(105,(width-24)/4);i=0;
            for(var f:Spec.FORMATIONS){String id=f.id();var btn=button((id.equals(CommandMode.formation)?"> ":"")+f.label(),width/2-2*cardWidth+i*cardWidth,height-27,cardWidth-3,b->{CommandMode.formation=id;clearAndInit();});btn.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(f.purpose())));i++;}
        }
        @Override public void render(DrawContext g,int mx,int my,float delta){
            panel(g,4,39,90,158);panel(g,width-80,39,76,32);
            int cardWidth=Math.min(105,(width-24)/4),i=0;
            for(var f:Spec.FORMATIONS){int x=width/2-2*cardWidth+i*cardWidth;panel(g,x,height-65,cardWidth-3,37);if(f.id().equals(CommandMode.formation))g.drawBorder(x,height-65,cardWidth-3,37,GOLD);
                for(int n=0;n<12;n++){int col=f.id().equals("line")?6:4;int dx=n%col*7,dy=n/col*6;if(f.wedge()){int rank=(int)Math.sqrt(n);dx=(n-rank*rank-rank)*7+20;dy=rank*6;}if(f.id().equals("loose")){dx*=2;dy*=2;}g.fill(x+12+dx,height-60+dy,x+15+dx,height-57+dy,GOLD);}i++;}
            g.drawCenteredTextWithShadow(textRenderer,"Choose group and order. Aim and confirm in the battlefield.",width/2,height-79,WHITE);super.render(g,mx,my,delta);
        }
    }
''' +s[end:]
# Scaled map: own groups/destination lines over subdued terrain. Own troop dots remain for drilldown.
s=s.replace('        for(int i=0;i<Spec.POINTS.length;i++)', '        g.fill(x,y,x+w,y+h,0x66303c39);\n        for(int i=0;i<Spec.POINTS.length;i++)',1)
needle='        var p=MinecraftClient.getInstance().player;if(p!=null)'
s=s.replace(needle,'''        for(var group:CommandMode.groups){int gx=x+(int)((group.cx()+140)*w/280),gz=y+(int)((group.cz()+100)*h/200);boolean selected=CommandMode.selected.equals("all")||CommandMode.selected.equals(group.id());
            if(selected){int dx=x+(int)((group.x()+140)*w/280),dz=y+(int)((group.z()+100)*h/200);for(int k=0;k<=20;k++){int lx=gx+(dx-gx)*k/20,lz=gz+(dz-gz)*k/20;g.fill(lx,lz,lx+1,lz+1,GOLD);}g.drawBorder(dx-3,dz-3,7,7,GOLD);}
            g.fill(gx-4,gz-4,gx+5,gz+5,0xff111923);g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,group.id().substring(0,1).toUpperCase(),gx-2,gz-3,group.fighting()?0xffff9955:GOLD);
        }
''' +needle)
# Expand map uses left group selection; right-click movement with explicit map position.
s=s.replace('MapScreen(){super("Battlefield map");}', 'MapScreen(){super("Battlefield map");}')
s=s.replace('for(Marker m:markers){double px=', '''if(b==1&&mx>=x&&mx<=x+w&&my>=y&&my<=y+h){double tx=(mx-x)*280/w-140,tz=(my-y)*200/h-100;CommandMode.issue(CommandMode.selected,"move",CommandMode.formation,new net.minecraft.util.math.Vec3d(tx,65,tz),client.player.getYaw(),-1);return true;}
            for(var group:CommandMode.groups){double gx=x+(group.cx()+140)*w/280,gz=y+(group.cz()+100)*h/200;if(b==0&&Math.hypot(mx-gx,my-gz)<8){CommandMode.selected=group.id();return true;}}
            for(Marker m:markers){double px=''')
s=s.replace('Gold: your army   Cyan: allies   Orange: fighting   White: you','Click group to select / Right-click to move / Gold yours, cyan ally')
p.write_text(s)
p=r/'src/main/resources/fabric.mod.json';d=json.loads(p.read_text());d['mixins']=['gravesandglory.mixins.json'];d['version']='0.2.0-dev';p.write_text(json.dumps(d,indent=2)+'\n')
p=r/'src/main/resources/assets/gravesandglory/lang/en_us.json';d=json.loads(p.read_text());d.update({'key.gravesandglory.rotate_left':'Rotate formation left','key.gravesandglory.rotate_right':'Rotate formation right','key.gravesandglory.confirm':'Confirm army order','key.gravesandglory.cancel':'Cancel army order','key.gravesandglory.attack':'Aim attack order'});p.write_text(json.dumps(d,indent=2)+'\n')
p=r/'build.gradle';s=p.read_text().replace("version = '0.1.0'","version = '0.2.0-dev'");p.write_text(s)
