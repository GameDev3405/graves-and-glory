from pathlib import Path
r=Path(__file__).resolve().parents[1]
p=r/'src/main/java/gg/gravesandglory/GravesAndGloryClient.java';s=p.read_text()
s=s.replace('key==GLFW.GLFW_KEY_R||key==GLFW.GLFW_KEY_ESCAPE','menu.matchesKey(key,scan)||key==GLFW.GLFW_KEY_ESCAPE').replace('key==GLFW.GLFW_KEY_M','mapKey.matchesKey(key,scan)')
s=s.replace('8,45+i*25,82','8,40+i*21,72').replace('panel(g,4,39,90,158)','panel(g,4,35,80,132)').replace('width/2,height-79,WHITE','width/2,32,WHITE')
s=s.replace('return addDrawableChild(ButtonWidget.builder(Text.literal(label),action::accept).dimensions(x,y,w,20).build());','return addDrawableChild(new NavyButton(x,y,w,20,Text.literal(label),action::accept));')
needle='    abstract static class ArmyScreen'
s=s.replace(needle,'''    static final class NavyButton extends ButtonWidget {
        NavyButton(int x,int y,int w,int h,Text text,PressAction action){super(x,y,w,h,text,action,DEFAULT_NARRATION_SUPPLIER);}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            g.fill(getX(),getY(),getX()+width,getY()+height,active?(isHovered()?0xff354960:0xf01b293a):0xe01b2028);
            g.drawBorder(getX(),getY(),width,height,isHovered()&&active?GOLD:0xff526174);
            g.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,getMessage(),getX()+width/2,getY()+(height-8)/2,active?WHITE:0xff6f7882);
        }
    }
''' +needle)
p.write_text(s)
p=r/'src/main/java/gg/gravesandglory/GravesAndGlory.java';s=p.read_text()
s=s.replace('int rounds=0;','int rounds=0;long roundStarted;')
s=s.replace('if(time%R.scoreInterval()==0){\n                    captureTick();','if(time%20==0)captureTick();\n                if(time%R.scoreInterval()==0){')
s=s.replace('rounds++;running=true;', 'rounds++;roundStarted=time;running=true;')
s=s.replace('points.stream().filter(p->p.owner==s.team).count()*Spec.ECONOMY[0].pointGold()', 'Math.min(Spec.ECONOMY[0].objectiveIncomeCap(),points.stream().filter(p->p.owner==s.team).count()*Spec.ECONOMY[0].pointGold())')
s=s.replace('            if(MeltyTest.active())selfTest();','''            if(running&&time-roundStarted>=R.roundSeconds()*20L){running=false;message=scores[0]==scores[1]?"Time: draw":(scores[0]>scores[1]?"Dawn":"Dusk")+" wins on score";for(var p:server.getPlayerManager().getPlayerList())p.sendMessage(Text.literal(message),false);}
            if(MeltyTest.active())selfTest();''')
# New troop receives current group orders without auto-teleport. Tell owner once.
s=s.replace('spawn(s,t.spec(),slot,base(s.team).add((slot%5-2)*2,0,(slot/5-2)*2));','''spawn(s,t.spec(),slot,base(s.team).add((slot%5-2)*2,0,(slot/5-2)*2));
                var player=server.getPlayerManager().getPlayer(s.owner);if(player!=null)player.sendMessage(Text.literal(t.spec().name()+" trained: deploying from barracks"),true);''')
p.write_text(s)
p=r/'src/main/java/gg/gravesandglory/Arena.java';s=p.read_text()
s=s.replace('return Math.abs(z)<4||Math.abs(z+43)<4||Math.abs(x+44)<3||Math.abs(x-44)<3||Math.abs(z-66)<3;', 'return Math.abs(z-3*Math.sin(x*.038))<3||Math.abs(z+43-4*Math.sin(x*.045))<3||Math.abs(x+44-4*Math.sin(z*.045))<2||Math.abs(x-44-3*Math.sin(z*.05))<2||Math.abs(z-66-3*Math.sin(x*.04))<2;')
s=s.replace('double a=(x-hill.x())/(double)hill.radiusX(),b=(z-hill.z())/(double)hill.radiusZ();', 'double wx=x+Spec.TERRAINSTYLE[0].warpAmplitude()*Math.sin(z*Spec.TERRAINSTYLE[0].warpFrequency()),wz=z+5*Math.sin(x*.043);double a=(wx-hill.x())/(double)hill.radiusX(),b=(wz-hill.z())/(double)hill.radiusZ();')
s=s.replace('        double d=Math.abs(x-riverX(z));','        h+=Spec.TERRAINSTYLE[0].roughness()*(Math.sin(x*.067+z*.031)+.5*Math.sin(z*.13-x*.043));\n        double d=Math.abs(x-riverX(z));',1)
s=s.replace('Math.min(32,border*.75)*(.85+.15*Math.sin(x*.14+z*.09))','Math.min(Spec.TERRAINSTYLE[0].boundaryRise(),border*.4)*(.65+.35*Math.sin(x*.055+z*.043))')
s=s.replace('double k=1.0+MathHelper.clamp((height(x-1,z-1)-h)*.065,-.2,.2);','double k=1.0;')
s=s.replace('case "bridge" -> {box', '''case "mill" -> {
                box(w,x,ground,z,s.width(),1,s.depth(),Blocks.COBBLESTONE);
                for(int dy=0;dy<6;dy++)for(int dx=0;dx<s.width();dx++)for(int dz=0;dz<s.depth();dz++)if(dx==0||dz==0||dx==s.width()-1||dz==s.depth()-1)put(w,x+dx,y+dy,z+dz,dy<2?Blocks.STONE_BRICKS:Blocks.STRIPPED_OAK_LOG);
                box(w,x+3,y,z,3,3,1,Blocks.AIR);for(int a=0;a<5;a++)box(w,x-1+a,y+6+a,z-1,s.width()+2-a*2,1,s.depth()+2,Blocks.DARK_OAK_PLANKS);
                int wheelX=x+s.width(),wheelZ=z+s.depth()/2;for(int dy=-3;dy<=3;dy++)for(int dz=-3;dz<=3;dz++)if(Math.abs(dy*dy+dz*dz-9)<4||dy==0||dz==0)put(w,wheelX,y+2+dy,wheelZ+dz,Blocks.DARK_OAK_SLAB);
                put(w,x+2,y,z-1,Blocks.BARREL);put(w,x+6,y,z-1,Blocks.HAY_BLOCK);
            }
            case "farm" -> {for(int dx=0;dx<s.width();dx++)for(int dz=0;dz<s.depth();dz++){put(w,x+dx,ground,z+dz,dx%5==2?Blocks.WATER:Blocks.FARMLAND);if(dx%5!=2)w.setBlockState(new BlockPos(x+dx,y,z+dz),Blocks.WHEAT.getDefaultState().with(CropBlock.AGE,7),2);}for(int dx=0;dx<s.width();dx+=3){put(w,x+dx,y,z-1,Blocks.OAK_FENCE);put(w,x+dx,y,z+s.depth(),Blocks.OAK_FENCE);}}
            case "breach" -> {for(int dz=0;dz<s.depth();dz++)if(Math.abs(dz-s.depth()/2)>3)box(w,x,y,z+dz,s.width(),Math.max(2,s.height()-Math.abs(dz-s.depth()/2)/2),1,Blocks.CRACKED_STONE_BRICKS);box(w,x,y+9,z,3,2,3,Blocks.CYAN_WOOL);}
            case "bridge" -> {if(s.palette().equals("ford")){box(w,x,ground,z,s.width(),1,s.depth(),Blocks.GRAVEL);for(int dx=0;dx<s.width();dx+=5)for(int dz=1;dz<s.depth();dz+=6)put(w,x+dx,ground,z+dz,Blocks.MOSSY_COBBLESTONE);break;}if(s.palette().equals("wood")){box(w,x,ground,z,s.width(),1,s.depth(),Blocks.SPRUCE_PLANKS);for(int dx=0;dx<s.width();dx+=4){box(w,x+dx,ground-3,z,1,5,1,Blocks.SPRUCE_LOG);box(w,x+dx,ground-3,z+s.depth()-1,1,5,1,Blocks.SPRUCE_LOG);}break;}box''')
p.write_text(s)
