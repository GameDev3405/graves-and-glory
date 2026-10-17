from pathlib import Path
r=Path(__file__).resolve().parents[1];p=r/'src/main/java/gg/gravesandglory/GravesAndGlory.java';s=p.read_text()
s=s.replace('public static final Identifier TACTICAL', 'public static final Identifier PING = new Identifier("gravesandglory", "ping_v1");\n    public static final Identifier TACTICAL')
needle='        ServerPlayNetworking.registerGlobalReceiver(TACTICAL'
s=s.replace(needle,'''        ServerPlayNetworking.registerGlobalReceiver(PING,(server,p,handler,buf,responder)->{String kind=buf.readString(16);double x=buf.readDouble(),z=buf.readDouble();server.execute(()->{if(battle!=null)battle.ping(p,kind,x,z);});});
''' +needle)
s=s.replace('int rounds=0;long roundStarted;', '''int rounds=0;long roundStarted;
        record Ping(UUID owner,int team,String kind,double x,double z,long until){}
        final List<Ping> pings=new ArrayList<>();final Map<UUID,Long> lastPing=new HashMap<>();
        void ping(ServerPlayerEntity p,String kind,double x,double z){
            Squad s=squads.get(p.getUuid());var spec=Arrays.stream(Spec.PINGS).filter(v->v.id().equals(kind)).findFirst().orElse(null);
            if(s==null||!running||spec==null||!Double.isFinite(x)||!Double.isFinite(z)||Math.abs(x)>130||Math.abs(z)>90||time-lastPing.getOrDefault(s.owner,-100L)<spec.cooldownSeconds()*20)return;
            lastPing.put(s.owner,time);pings.removeIf(v->v.owner.equals(s.owner)||v.until<time);pings.add(new Ping(s.owner,s.team,kind,x,z,time+spec.seconds()*20));
        }''')
s=s.replace('                ServerPlayNetworking.send(p,STATE,b);','''                var visiblePings=pings.stream().filter(v->s!=null&&v.team==s.team&&v.until>time).toList();b.writeInt(visiblePings.size());for(var ping:visiblePings){b.writeString(ping.kind);b.writeFloat((float)ping.x);b.writeFloat((float)ping.z);}
                b.writeInt(running?(int)Math.max(0,R.roundSeconds()-(time-roundStarted)/20):R.roundSeconds());
                ServerPlayNetworking.send(p,STATE,b);''')
p.write_text(s)
p=r/'src/main/java/gg/gravesandglory/CommandMode.java';s=p.read_text()
s=s.replace('static java.util.List<Group>', 'record Ping(String kind,float x,float z){}\n    static java.util.List<Ping> pings=java.util.List.of();static int seconds=600;\n    static java.util.List<Group>')
s=s.replace('    static void issue(', '''    static void ping(String kind,double x,double z){if(ClientPlayNetworking.canSend(Graves and Glory.PING)){var b=PacketByteBufs.create();b.writeString(kind);b.writeDouble(x);b.writeDouble(z);ClientPlayNetworking.send(Graves and Glory.PING,b);}}
    static void issue(''')
s=s.replace('"Aim, then confirm | Rotate facing: "','"Click: confirm / Right-click: cancel | Facing: "')
p.write_text(s)
p=r/'src/main/java/gg/gravesandglory/GravesAndGloryClient.java';s=p.read_text()
s=s.replace('            client.execute(()->{CommandMode.groups=', '''            int pingCount=buf.readInt();if(pingCount<0||pingCount>4)return;var pings=new ArrayList<CommandMode.Ping>();for(int i=0;i<pingCount;i++)pings.add(new CommandMode.Ping(buf.readString(),buf.readFloat(),buf.readFloat()));int seconds=buf.readInt();
            client.execute(()->{CommandMode.pings=java.util.List.copyOf(pings);CommandMode.seconds=seconds;CommandMode.groups=''')
s=s.replace('" / 300   -   DUSK "','" / 300  | "+String.format("%d:%02d",CommandMode.seconds/60,CommandMode.seconds%60)+" |  DUSK "')
s=s.replace('        var p=MinecraftClient.getInstance().player;if(p!=null)', '''        for(var ping:CommandMode.pings){int px=x+(int)((ping.x()+140)*w/280),pz=y+(int)((ping.z()+100)*h/200);g.drawBorder(px-6,pz-6,13,13,0xffffcc55);if(labels)g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,ping.kind().toUpperCase(),px+8,pz,0xffffcc55);}
        var p=MinecraftClient.getInstance().player;if(p!=null)''')
s=s.replace('int x,y,w,h;Marker selected;', 'int x,y,w,h;Marker selected;String pingMode="";')
s=s.replace('button("Back",12,8,55,b->client.setScreen(new CommandScreen()));}', '''button("Back",12,8,55,b->client.setScreen(new CommandScreen()));int i=0;for(var ping:Spec.PINGS){String id=ping.id();button(ping.label(),width/2-152+i*77,height-22,74,b->pingMode=id);i++;}}''')
s=s.replace('if(b==1&&mx>=x', 'if(!pingMode.isEmpty()&&b==0&&mx>=x&&mx<=x+w&&my>=y&&my<=y+h){CommandMode.ping(pingMode,(mx-x)*280/w-140,(my-y)*200/h-100);pingMode="";return true;}if(b==1&&mx>=x')
s=s.replace('h=Math.min(height-68,230);','h=Math.min(height-86,230);')
p.write_text(s)
