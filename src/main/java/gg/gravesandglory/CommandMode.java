package gg.gravesandglory;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/** Selection -> live-world aim/formation preview -> explicit confirmation. */
public final class CommandMode {
    public record Group(String id,int count,float health,String order,String formation,float x,float z,float yaw,float cx,float cz,boolean fighting,boolean lastStand){}
    record Ping(String kind,float x,float z){}
    static java.util.List<Ping> pings=java.util.List.of();static int seconds=600;
    static java.util.List<Group> groups=java.util.List.of();
    static String selected="all",formation="block",pending="";
    static Vec3d destination;
    static float facing;
    static boolean facingSet;
    static final java.util.List<Vec3d> preview=new java.util.ArrayList<>();
    static int enemy=-1;
    static String feedback="";static long feedbackUntil;
    /** How long a group panel flashes after it takes a loss (presentation only). */
    public static final long FLASH_MS=450;
    static final int DANGER=0xffd9634c;
    static final java.util.Map<String,Long> flashUntil=new java.util.HashMap<>();
    /** Compares consecutive server snapshots; a death or a health drop flashes that group's panel. */
    public static void noteGroups(java.util.List<Group> before,java.util.List<Group> after,long nowMs){
        for(Group now:after)for(Group was:before)if(was.id().equals(now.id())&&BattleMath.tookLoss(was.health(),was.count(),now.health(),now.count()))flashUntil.put(now.id(),nowMs+FLASH_MS);
    }
    public static boolean flashing(String groupId){return net.minecraft.util.Util.getMeasuringTimeMs()<flashUntil.getOrDefault(groupId,0L);}
    public static boolean active(){return !pending.isEmpty();}
    static int count(String id){return groups.stream().filter(g->id.equals("all")||id.equals(g.id())).mapToInt(Group::count).sum();}
    static void changeFormation(String id){formation=id;var c=MinecraftClient.getInstance();if(c.player!=null)issue(selected,"formation",id,c.player.getPos(),c.player.getYaw(),-1);}
    static void begin(String order){if(!GravesAndGloryClient.battleActive())return;pending=order;facingSet=false;MinecraftClient.getInstance().setScreen(null);}
    public static boolean cancel(){if(!active())return false;pending="";destination=null;return true;}
    public static boolean confirm(){
        if(!active())return false;
        if(destination==null){feedback="Aim at reachable terrain";feedbackUntil=System.currentTimeMillis()+2500;return true;}
        if(pending.equals("charge")&&enemy<0){feedback="Charge: aim at an enemy";feedbackUntil=System.currentTimeMillis()+2500;return true;}
        issue(selected,pending,formation,destination,facing,enemy);pending="";return true;
    }
    static void ping(String kind,double x,double z){if(ClientPlayNetworking.canSend(GravesAndGlory.PING)){var b=PacketByteBufs.create();b.writeString(kind);b.writeDouble(x);b.writeDouble(z);ClientPlayNetworking.send(GravesAndGlory.PING,b);}}
    static void issue(String group,String action,String form,Vec3d dest,float yaw,int target){
        if(!GravesAndGloryClient.battleActive()||!ClientPlayNetworking.canSend(GravesAndGlory.TACTICAL))return;
        var b=PacketByteBufs.create();b.writeString(group);b.writeString(action);b.writeString(form);b.writeDouble(dest.x);b.writeDouble(dest.z);b.writeFloat(yaw);b.writeInt(target);ClientPlayNetworking.send(GravesAndGlory.TACTICAL,b);
        feedback=Morale.label(group)+": "+Morale.label(action)+" order sent";feedbackUntil=System.currentTimeMillis()+3000;
    }
    /** The team a unit fights for, read from its name colour (team 0 aqua, team 1 light purple; see GravesAndGlory.spawn), or -1. */
    static int nameTeam(net.minecraft.entity.Entity e){
        var name=e.getCustomName();var colour=name==null?null:name.getStyle().getColor();if(colour==null)return -1;
        if(colour.getRgb()==net.minecraft.util.Formatting.AQUA.getColorValue())return 0;
        if(colour.getRgb()==net.minecraft.util.Formatting.LIGHT_PURPLE.getColorValue())return 1;
        return -1;
    }
    /**
     * The cell the server forms this group in (Battle.formation): the formation's spacing, or the room of the widest body among the army's troops of the group
     * (BattleMath.spacing of the troop's mob width) when that is more. The state packet names each own troop, and a troop's sheet row names its mob.
     */
    static double cell(Group group,double formationSpacing){
        double cell=formationSpacing;
        for(var m:GravesAndGloryClient.markers){
            if(!m.own())continue;
            var spec=ArmyMobs.named(m.name());if(spec==null||!spec.group().equals(group.id()))continue;
            var id=net.minecraft.util.Identifier.tryParse(spec.entity());
            if(id!=null&&net.minecraft.registry.Registries.ENTITY_TYPE.containsId(id))cell=Math.max(cell,BattleMath.spacing(net.minecraft.registry.Registries.ENTITY_TYPE.get(id).getWidth()));
        }
        return cell;
    }
    static void tick(MinecraftClient c){
        preview.clear();if(!GravesAndGloryClient.battleActive()){cancel();return;}if(!active()||c.player==null||c.world==null||c.currentScreen!=null)return;
        if(!facingSet)facing=c.player.getYaw();
        var hit=c.player.raycast(Spec.COMMANDINTERFACE[0].targetRange(),1,false);destination=hit.getType()==HitResult.Type.MISS?null:hit.getPos();enemy=-1;
        // Entity targeting is an aim cone, not an omniscient enemy lookup.
        Vec3d eye=c.player.getEyePos(),look=c.player.getRotationVec(1);double best=destination==null?64:eye.distanceTo(destination);
        for(var e:c.world.getEntities())if(e instanceof LivingEntity living&&e!=c.player&&e.isAlive()&&e.hasCustomName()&&c.player.canSee(e)){
            Vec3d v=e.getPos().add(0,1,0).subtract(eye);double along=v.dotProduct(look);
            // Both teams can field the same House, so the side comes from the name colour the server gives every unit.
            boolean foe=false;int side=nameTeam(e);if(ArmyMobs.named(e.getCustomName().getString())!=null&&GravesAndGloryClient.team>=0&&side>=0&&side!=GravesAndGloryClient.team)foe=true;
            if(foe&&along>0&&along<best&&v.subtract(look.multiply(along)).length()<1.3){best=along;enemy=e.getId();destination=e.getPos();}
        }
        if(destination==null||Math.abs(destination.x)>GravesAndGlory.R.arenaHalfX()-10||Math.abs(destination.z)>GravesAndGlory.R.arenaHalfZ()-10){destination=null;return;}
        var f=java.util.Arrays.stream(Spec.FORMATIONS).filter(v->v.id().equals(formation)).findFirst().orElse(Spec.FORMATIONS[0]);
        var dust=new DustParticleEffect(new Vector3f(.95f,.82f,.44f),1.15f);
        for(Group group:groups)if(selected.equals("all")||selected.equals(group.id())){
            int columns=Math.min(f.columns(),group.id().equals("support")?2:group.id().equals("ranged")?6:12);
            double role=group.id().equals("support")?9:group.id().equals("ranged")?5:0,cell=cell(group,f.spacing());
            for(int i=0;i<group.count();i++){
                double side=(i%columns-(columns-1)/2.0)*cell,back=role+(i/columns+1)*cell;
                if(f.wedge()){int rank=(int)Math.floor(Math.sqrt(i));side=(i-rank*rank-rank)*cell;back=role+(rank+1)*cell;}
                double angle=Math.toRadians(facing),x=destination.x+Math.cos(angle)*side+Math.sin(angle)*back,z=destination.z+Math.sin(angle)*side-Math.cos(angle)*back;
                preview.add(new Vec3d(x,Arena.ground((int)x,(int)z)+1.05,z));
            }
        }
        double angle=Math.toRadians(facing);for(int i=0;i<6;i++){double x=destination.x-Math.sin(angle)*i,z=destination.z+Math.cos(angle)*i;preview.add(new Vec3d(x,Arena.ground((int)x,(int)z)+1.05,z));}
    }
    static void renderWorld(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context){
        if(!active()||context.consumers()==null)return;var matrices=context.matrixStack();var camera=context.camera().getPos();matrices.push();matrices.translate(-camera.x,-camera.y,-camera.z);
        var lines=context.consumers().getBuffer(net.minecraft.client.render.RenderLayer.getLines());
        for(Vec3d p:preview)net.minecraft.client.render.WorldRenderer.drawBox(matrices,lines,new net.minecraft.util.math.Box(p.x-.32,p.y,p.z-.32,p.x+.32,p.y+.09,p.z+.32),1,.82f,.4f,1);
        matrices.pop();
    }
    /**
     * The House strip over the group panels: the side the army flies as its glyph and full name (Gold Banner, never the bare word that also
     * names the currency), then each House the army has a trait tier in as its 16x16 glyph and a Roman numeral. Nothing is drawn without a tier.
     */
    static void houseStrip(DrawContext g,int y){
        var c=MinecraftClient.getInstance();var tiers=GravesAndGloryClient.houses;
        if(tiers.isEmpty())return;
        int team=GravesAndGloryClient.team;var side=DarkUi.side(team); // DarkUi.side reads the live battle state, like every other team mark
        GravesAndGloryClient.panel(g,8,y,210,38);
        if(side!=null){DarkUi.teamMark(g,team,16,y+4);g.drawTextWithShadow(c.textRenderer,c.textRenderer.trimToWidth(side.name().toUpperCase(java.util.Locale.ROOT)+" / HOUSES",190),28,y+4,GravesAndGloryClient.MUTED);}
        int x=16;for(var house:Spec.HOUSES){int tier=tiers.getOrDefault(house.id(),0);if(tier<=0)continue;DarkUi.houseMark(g,house.id(),tier,x,y+17);x+=DarkUi.houseMarkWidth(tier)+12;}
    }
    /** Where vanilla draws the hearts and the armour row (GUI x width / 2 - 91), in the HUD frame of width w (the GUI scaled by min(1, width / 640)). */
    static int statusBarsLeft(int w){int sw=MinecraftClient.getInstance().getWindow().getScaledWidth();return (int)Math.floor((sw/2-91)*(w/(double)sw));}
    /** The right edge the group panels were last drawn to, in the HUD frame (read by the checks). */
    static int groupsRight;
    static void hud(DrawContext g,int w,int h){
        var c=MinecraftClient.getInstance();int y=h-150;
        houseStrip(g,y-42);
        // The group panels end left of vanilla's hearts and armour, which a frame narrower than 640 brings under them: 210 wide when there is room, about 170 in a 640x360 frame.
        int right=Math.min(218,statusBarsLeft(w)-4),width=right-8,bar=width-16;groupsRight=right;
        for(Group group:groups){boolean chosen=selected.equals("all")||selected.equals(group.id());
            boolean hit=flashing(group.id());
            GravesAndGloryClient.panel(g,8,y,width,28);if(hit)g.drawBorder(8,y,width,28,DANGER);if(chosen)g.fill(8,y,10,y+28,GravesAndGloryClient.GOLD);
            g.fill(16,y+24,16+bar,y+27,0xff2a3236);g.fill(16,y+24,16+Math.round(bar*Math.max(0,Math.min(1,group.health()))),y+27,hit?DANGER:GravesAndGloryClient.GOLD);
            g.drawTextWithShadow(c.textRenderer,c.textRenderer.trimToWidth((chosen?"> ":"  ")+Morale.label(group.id())+" / "+group.count()+" / "+Math.round(group.health()*100)+"%",width-12),16,y+4,GravesAndGloryClient.WHITE);
            if(group.lastStand())g.drawTextWithShadow(c.textRenderer,"LAST STAND",16,y+16,DANGER);
            else g.drawTextWithShadow(c.textRenderer,c.textRenderer.trimToWidth(Morale.label(group.order())+(group.fighting()?" / FIGHTING":""),width-12),16,y+16,group.fighting()?0xffffaa66:GravesAndGloryClient.MUTED);y+=30;
        }
        if(active()){
            GravesAndGloryClient.panel(g,w/2-180,52,360,40);
            g.drawCenteredTextWithShadow(c.textRenderer,Morale.label(selected)+" / "+Morale.label(pending)+" / "+Morale.label(formation),w/2,59,GravesAndGloryClient.GOLD);
            g.drawCenteredTextWithShadow(c.textRenderer,"Click: confirm / Right-click: cancel | Facing: "+GravesAndGloryClient.rotateLeft.getBoundKeyLocalizedText().getString()+" / "+GravesAndGloryClient.rotateRight.getBoundKeyLocalizedText().getString(),w/2,74,GravesAndGloryClient.WHITE);
            g.drawCenteredTextWithShadow(c.textRenderer,destination==null?"Choose visible terrain":enemy>=0?"Enemy selected":"Destination: "+(int)destination.x+", "+(int)destination.z,w/2,h/2+20,GravesAndGloryClient.GOLD);
        }
        if(System.currentTimeMillis()<feedbackUntil)g.drawCenteredTextWithShadow(c.textRenderer,feedback,w/2,h-65,GravesAndGloryClient.GOLD);
    }
}

