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
    public record Group(String id,int count,float health,String order,String formation,float x,float z,float yaw,float cx,float cz,boolean fighting,boolean routing){}
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
    static String label(String id){return Character.toUpperCase(id.charAt(0))+id.substring(1);}
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
        feedback=label(group)+": "+label(action)+" order sent";feedbackUntil=System.currentTimeMillis()+3000;
    }
    static void tick(MinecraftClient c){
        preview.clear();if(!GravesAndGloryClient.battleActive()){cancel();return;}if(!active()||c.player==null||c.world==null||c.currentScreen!=null)return;
        if(!facingSet)facing=c.player.getYaw();
        var hit=c.player.raycast(Spec.COMMANDINTERFACE[0].targetRange(),1,false);destination=hit.getType()==HitResult.Type.MISS?null:hit.getPos();enemy=-1;
        // Entity targeting is an aim cone, not an omniscient enemy lookup.
        Vec3d eye=c.player.getEyePos(),look=c.player.getRotationVec(1);double best=destination==null?64:eye.distanceTo(destination);
        for(var e:c.world.getEntities())if(e instanceof LivingEntity living&&e!=c.player&&e.isAlive()&&e.hasCustomName()&&c.player.canSee(e)){
            Vec3d v=e.getPos().add(0,1,0).subtract(eye);double along=v.dotProduct(look);
            boolean foe=false;for(var troop:Spec.TROOPS)if(troop.name().equals(e.getCustomName().getString())&&GravesAndGloryClient.team>=0&&!troop.faction().equals(Spec.FACTIONS[GravesAndGloryClient.team].id()))foe=true;
            if(foe&&along>0&&along<best&&v.subtract(look.multiply(along)).length()<1.3){best=along;enemy=e.getId();destination=e.getPos();}
        }
        if(destination==null||Math.abs(destination.x)>GravesAndGlory.R.arenaHalfX()-10||Math.abs(destination.z)>GravesAndGlory.R.arenaHalfZ()-10){destination=null;return;}
        var f=java.util.Arrays.stream(Spec.FORMATIONS).filter(v->v.id().equals(formation)).findFirst().orElse(Spec.FORMATIONS[0]);
        var dust=new DustParticleEffect(new Vector3f(.95f,.82f,.44f),1.15f);
        for(Group group:groups)if(selected.equals("all")||selected.equals(group.id())){
            int columns=Math.min(f.columns(),group.id().equals("support")?2:group.id().equals("ranged")?6:12);
            double role=group.id().equals("support")?9:group.id().equals("ranged")?5:0;
            for(int i=0;i<group.count();i++){
                double side=(i%columns-(columns-1)/2.0)*f.spacing(),back=role+(i/columns+1)*f.spacing();
                if(f.wedge()){int rank=(int)Math.floor(Math.sqrt(i));side=(i-rank*rank-rank)*f.spacing();back=role+(rank+1)*f.spacing();}
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
    static void hud(DrawContext g,int w,int h){
        var c=MinecraftClient.getInstance();int y=h-150;
        for(Group group:groups){boolean chosen=selected.equals("all")||selected.equals(group.id());
            boolean hit=flashing(group.id());
            GravesAndGloryClient.panel(g,8,y,210,28);if(hit)g.drawBorder(8,y,210,28,DANGER);if(chosen)g.fill(8,y,10,y+28,GravesAndGloryClient.GOLD);
            g.fill(16,y+24,210,y+27,0xff2a3236);g.fill(16,y+24,16+Math.round(194*Math.max(0,Math.min(1,group.health()))),y+27,hit?DANGER:GravesAndGloryClient.GOLD);
            g.drawTextWithShadow(c.textRenderer,(chosen?"> ":"  ")+label(group.id())+" / "+group.count()+" / "+Math.round(group.health()*100)+"%",16,y+4,GravesAndGloryClient.WHITE);
            if(group.routing())g.drawTextWithShadow(c.textRenderer,"ROUTING",16,y+16,DANGER);
            else g.drawTextWithShadow(c.textRenderer,label(group.order())+(group.fighting()?" / FIGHTING":""),16,y+16,group.fighting()?0xffffaa66:GravesAndGloryClient.MUTED);y+=30;
        }
        if(active()){
            GravesAndGloryClient.panel(g,w/2-180,52,360,40);
            g.drawCenteredTextWithShadow(c.textRenderer,label(selected)+" / "+label(pending)+" / "+label(formation),w/2,59,GravesAndGloryClient.GOLD);
            g.drawCenteredTextWithShadow(c.textRenderer,"Click: confirm / Right-click: cancel | Facing: "+GravesAndGloryClient.rotateLeft.getBoundKeyLocalizedText().getString()+" / "+GravesAndGloryClient.rotateRight.getBoundKeyLocalizedText().getString(),w/2,74,GravesAndGloryClient.WHITE);
            g.drawCenteredTextWithShadow(c.textRenderer,destination==null?"Choose visible terrain":enemy>=0?"Enemy selected":"Destination: "+(int)destination.x+", "+(int)destination.z,w/2,h/2+20,GravesAndGloryClient.GOLD);
        }
        if(System.currentTimeMillis()<feedbackUntil)g.drawCenteredTextWithShadow(c.textRenderer,feedback,w/2,h-65,GravesAndGloryClient.GOLD);
    }
}

