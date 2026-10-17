from pathlib import Path
r=Path(__file__).resolve().parents[1];p=r/'src/main/java/gg/gravesandglory/GravesAndGlory.java';s=p.read_text()
s=s.replace('"order_v2"','"order_v3"').replace('"state_v2"','"state_v3"')
s=s.replace('public static final Identifier STATE', 'public static final Identifier TACTICAL = new Identifier("gravesandglory", "tactical_v1");\n    public static final Identifier STATE')
s=s.replace('ServerPlayConnectionEvents.DISCONNECT.register', '''ServerPlayNetworking.registerGlobalReceiver(TACTICAL, (server, player, handler, buf, responder) -> {
            String group=buf.readString(16), action=buf.readString(16), formation=buf.readString(16);
            double x=buf.readDouble(),z=buf.readDouble();float yaw=buf.readFloat();int target=buf.readInt();
            server.execute(()->{if(battle!=null)battle.tactical(player,group,action,formation,x,z,yaw,target);});
        });
        ServerPlayConnectionEvents.DISCONNECT.register''')
s=s.replace('final ArrayDeque<Training> training', 'final Map<String,Assignment> assignments=new HashMap<>();\n        final ArrayDeque<Training> training')
s=s.replace('public record Training', '''public static final class Assignment {
        String order,formation;Vec3d target;float yaw;int enemy=-1;long issued;
        Assignment(String o,String f,Vec3d t,float y){order=o;formation=f;target=t;yaw=y;}
    }
    public record Training''')
s=s.replace('int stuckTicks;', 'int stuckTicks;\n        LivingEntity lockedTarget;long lockUntil;')
pos=s.index('        public boolean atBarracks')
s=s[:pos]+'''        Assignment assignment(Unit u){return u.squad.assignments.getOrDefault(u.spec.group(),new Assignment(u.squad.order,u.squad.formation,u.squad.target,u.squad.yaw));}
        public void tactical(ServerPlayerEntity p,String group,String action,String formation,double x,double z,float yaw,int enemyId){
            Squad s=squads.get(p.getUuid());
            if(s==null||!ready||!running||respawns.containsKey(s.owner)||time-s.lastOrder<R.commandCooldownTicks())return;
            if(Arrays.stream(Spec.GROUPS).noneMatch(g->g.id().equals(group))||Arrays.stream(Spec.FORMATIONS).noneMatch(f->f.id().equals(formation)))return;
            var order=Arrays.stream(Spec.ORDERS).filter(o->o.id().equals(action)).findFirst().orElse(null);
            if(order==null||!Double.isFinite(x)||!Double.isFinite(z)||!Float.isFinite(yaw)||Math.abs(x)>130||Math.abs(z)>90)return;
            Entity enemy=world.getEntityById(enemyId);
            if(enemyId>=0&&(enemy==null||!enemy.isAlive()||teamOf(enemy)<0||teamOf(enemy)==s.team||!p.canSee(enemy))){p.sendMessage(Text.literal("Target lost. Aim again or issue an attack-move."),true);return;}
            if(action.equals("charge")&&enemy==null){p.sendMessage(Text.literal("Charge needs a visible enemy. Use Attack to advance to terrain."),true);return;}
            if(Arena.river((int)x,(int)z)&&Arena.ground((int)x,(int)z)<64){p.sendMessage(Text.literal("Choose dry ground or a crossing."),true);return;}
            s.lastOrder=time;Vec3d dest=new Vec3d(x,Arena.ground((int)x,(int)z)+1,z);
            if(action.equals("follow"))dest=p.getPos();
            for(var g:Spec.GROUPS)if(!g.id().equals("all")&&(group.equals("all")||group.equals(g.id()))){
                Assignment a=new Assignment(action,formation,dest,yaw);a.enemy=enemyId;a.issued=time;s.assignments.put(g.id(),a);
            }
            for(Unit u:units)if(u.squad==s&&(group.equals("all")||u.spec.group().equals(group))){u.lockedTarget=null;u.mob.setTarget(null);u.mob.getNavigation().stop();u.nextPath=0;u.stuckTicks=0;}
            p.playSound(action.equals("retreat")?SoundEvents.BLOCK_NOTE_BLOCK_BELL:SoundEvents.BLOCK_NOTE_BLOCK_CHIME,.6f,action.equals("charge")?.7f:1.1f);
            p.sendMessage(Text.literal(group+" / "+action+" / "+(int)x+", "+(int)z),true);
        }
        Vec3d formation(Unit u){
            Assignment a=assignment(u);var f=Arrays.stream(Spec.FORMATIONS).filter(v->v.id().equals(a.formation)).findFirst().orElse(Spec.FORMATIONS[0]);
            int slot=0;for(Unit other:units)if(other.squad==u.squad&&other.spec.group().equals(u.spec.group())&&other.mob.isAlive()&&other.slot<u.slot)slot++;
            double role=u.spec.group().equals("support")?9:u.spec.group().equals("ranged")?5:0;
            int columns=Math.min(f.columns(),u.spec.group().equals("support")?2:u.spec.group().equals("ranged")?6:12);
            double side=(slot%columns-(columns-1)/2.0)*f.spacing(),back=role+(slot/columns+1)*f.spacing();
            if(f.wedge()){int rank=(int)Math.floor(Math.sqrt(slot));side=(slot-rank*rank-rank)*f.spacing();back=role+(rank+1)*f.spacing();}
            double angle=Math.toRadians(a.yaw);return a.target.add(Math.cos(angle)*side+Math.sin(angle)*back,0,Math.sin(angle)*side-Math.cos(angle)*back);
        }
''' + s[pos:]
s=s.replace('s.order=order.id();s.yaw=p.getYaw();','s.assignments.clear();s.order=order.id();s.yaw=p.getYaw();')
s=s.replace('s.formation=f.id();s.yaw=p.getYaw();','s.formation=f.id();s.yaw=p.getYaw();for(Assignment a:s.assignments.values())a.formation=f.id();')
s=s.replace('s.order="follow";s.target=base(s.team);','s.assignments.clear();s.order="follow";s.target=base(s.team);')
needle='                ServerPlayNetworking.send(p,STATE,b);'
s=s.replace(needle,'''                b.writeInt(3);
                for(var g:Spec.GROUPS)if(!g.id().equals("all")){
                    var members=units.stream().filter(u->u.squad==s&&u.spec.group().equals(g.id())&&u.mob.isAlive()).toList();
                    Assignment a=s==null?new Assignment("follow","block",Vec3d.ZERO,0):s.assignments.getOrDefault(g.id(),new Assignment(s.order,s.formation,s.target,s.yaw));
                    b.writeString(g.id());b.writeInt(members.size());b.writeFloat((float)members.stream().mapToDouble(u->u.mob.getHealth()/u.mob.getMaxHealth()).average().orElse(0));
                    b.writeString(a.order);b.writeString(a.formation);b.writeFloat((float)a.target.x);b.writeFloat((float)a.target.z);b.writeFloat(a.yaw);
                    b.writeFloat((float)members.stream().mapToDouble(u->u.mob.getX()).average().orElse(a.target.x));b.writeFloat((float)members.stream().mapToDouble(u->u.mob.getZ()).average().orElse(a.target.z));
                    b.writeBoolean(members.stream().anyMatch(u->u.mob.getTarget()!=null));
                }
''' +needle)
p.write_text(s)
p=r/'src/main/java/gg/gravesandglory/Tactics.java';s=p.read_text();s=s.replace('var mob=u.mob;var s=u.squad;', 'var mob=u.mob;var s=u.squad;var a=battle.assignment(u);')
s=s.replace('s.order','a.order').replace('s.target','a.target').replace('s.yaw','a.yaw')
s=s.replace('LivingEntity enemy=null;double nearest=order.engageRadius()*order.engageRadius();','''LivingEntity enemy=null;double nearest=order.engageRadius()*order.engageRadius();
        if(order.engage()&&u.lockedTarget!=null&&u.lockedTarget.isAlive()&&time<u.lockUntil&&mob.squaredDistanceTo(u.lockedTarget)<nearest*1.4&&(!a.order.equals("hold")||Graves and Glory.Battle.horizontalDistance(u.lockedTarget.getPos(),a.target)<nearest))enemy=u.lockedTarget;
        if(order.engage()&&a.enemy>=0&&battle.world.getEntityById(a.enemy) instanceof LivingEntity explicit&&explicit.isAlive())enemy=explicit;
        if(enemy!=null)nearest=mob.squaredDistanceTo(enemy);
        boolean locked=enemy!=null;''')
s=s.replace('if(other.squad.team!=s.team&&other.mob.isAlive())','if(order.engage()&&!locked&&other.squad.team!=s.team&&other.mob.isAlive())')
s=s.replace('if(other!=null&&other.team!=s.team&&!p.isSpectator()&&p.isAlive())','if(order.engage()&&!locked&&other!=null&&other.team!=s.team&&!p.isSpectator()&&p.isAlive()&&(!a.order.equals("hold")||Graves and Glory.Battle.horizontalDistance(p.getPos(),a.target)<order.engageRadius()*order.engageRadius()))')
s=s.replace('mob.setTarget(enemy);','if(enemy!=u.lockedTarget){u.lockedTarget=enemy;u.lockUntil=time+40;}\n        mob.setTarget(enemy);')
s=s.replace('}else if(!a.order.equals("hold")){','}else if(!a.order.equals("hold")||Graves and Glory.Battle.horizontalDistance(enemy.getPos(),a.target)<order.engageRadius()*order.engageRadius()){')
s=s.replace('Vec3d goal=enemy.getPos();','Vec3d goal=enemy.getPos();\n                if(u.spec.range()<=3){double angle=u.slot*2.399963;goal=goal.add(Math.cos(angle)*1.6,0,Math.sin(angle)*1.6);}')
s=s.replace('Vec3d goal=a.order.equals("rush")?a.target.add((u.slot%5-2)*1.2,0,(u.slot/5-2)*1.2):battle.formation(s,u.slot);','Vec3d goal=battle.formation(u);')
p.write_text(s)
