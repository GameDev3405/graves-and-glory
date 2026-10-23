package gg.gravesandglory;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/** Selection -> live-world aim/formation preview -> explicit confirmation. While an order is aimed the battle HUD shows its key
 *  hints (BattleHud.altLayer) and the aimed point's name under its seal; a refused confirmation is a notice in the event feed. */
public final class CommandMode {
    /** One group of the player's army as the server last sent it: full is the most of it alive at once this round (0: no card), strength its living health over its full health. */
    public record Group(String id,int count,float health,String order,String formation,float x,float z,float yaw,float cx,float cz,boolean fighting,boolean routing,int full,float strength){}
    record Ping(String kind,float x,float z){}
    static java.util.List<Ping> pings=java.util.List.of();static int seconds=600;
    static java.util.List<Group> groups=java.util.List.of();
    static String selected="all",formation="block",pending="";
    static Vec3d destination;
    static float facing;
    static boolean facingSet;
    static final java.util.List<Vec3d> preview=new java.util.ArrayList<>();
    static int enemy=-1;
    /** How long a group card flashes after it takes a loss (presentation only). */
    public static final long FLASH_MS=450;
    static final java.util.Map<String,Long> flashUntil=new java.util.HashMap<>();
    /** Each group's strength before the loss that started its current flash. */
    static final java.util.Map<String,Float> flashFrom=new java.util.HashMap<>();
    /** Compares consecutive server snapshots; a death or a health drop flashes that group's card. A loss inside a flash lengthens it
     *  and keeps the strength from before the first loss, so the card shows everything lost since the flash began. */
    public static void noteGroups(java.util.List<Group> before,java.util.List<Group> after,long nowMs){
        for(Group now:after)for(Group was:before)if(was.id().equals(now.id())&&BattleMath.tookLoss(was.health(),was.count(),now.health(),now.count())){
            if(nowMs>=flashUntil.getOrDefault(now.id(),0L))flashFrom.put(now.id(),was.strength());
            flashUntil.put(now.id(),nowMs+FLASH_MS);
        }
    }
    public static boolean flashing(String groupId){return net.minecraft.util.Util.getMeasuringTimeMs()<flashUntil.getOrDefault(groupId,0L);}
    /** The group's strength before the loss that started its current flash (0 when it never flashed). */
    public static float lostFrom(String group){return flashFrom.getOrDefault(group,0f);}
    /** The point whose radius holds the aimed ground position while an order is aimed, else -1. */
    public static int aimPoint(){
        var at=destination;if(!active()||at==null)return -1;var pts=Spec.POINTS;
        for(int i=0;i<pts.length;i++){double dx=at.x-pts[i].x(),dz=at.z-pts[i].z(),r=pts[i].radius();if(dx*dx+dz*dz<r*r)return i;}
        return -1;
    }
    public static boolean active(){return !pending.isEmpty();}
    static String label(String id){return Character.toUpperCase(id.charAt(0))+id.substring(1);}
    static int count(String id){return groups.stream().filter(g->id.equals("all")||id.equals(g.id())).mapToInt(Group::count).sum();}
    static void changeFormation(String id){formation=id;var c=MinecraftClient.getInstance();if(c.player!=null)issue(selected,"formation",id,c.player.getPos(),c.player.getYaw(),-1);}
    static void begin(String order){if(!GravesAndGloryClient.battleActive())return;pending=order;facingSet=false;MinecraftClient.getInstance().setScreen(null);}
    public static boolean cancel(){if(!active())return false;pending="";destination=null;return true;}
    /** A disconnect: no order stays aimed, and the cards' groups and loss flashes go, so the next server's first snapshot is compared
     *  with nothing (noteGroups would flash a card for the old battle's losses). The rest of the command state starts as a new
     *  client's: the map's pings, the clock, the whole army selected (a group the next world lacks would leave no card selected),
     *  the default formation and the facing taken from the player again. */
    static void reset(){cancel();preview.clear();enemy=-1;groups=java.util.List.of();flashUntil.clear();flashFrom.clear();pings=java.util.List.of();seconds=600;selected="all";formation="block";facing=0;facingSet=false;}
    public static boolean confirm(){
        if(!active())return false;
        if(destination==null){refuse("Aim at reachable terrain");return true;}
        if(pending.equals("charge")&&enemy<0){refuse("Charge: aim at an enemy");return true;}
        issue(selected,pending,formation,destination,facing,enemy);pending="";return true;
    }
    /** A refused confirmation as a feed notice; clicking again while the same notice still shows adds no second line. */
    static void refuse(String why){
        long now=net.minecraft.util.Util.getMeasuringTimeMs();var lines=EventFeed.lines(now);
        if(lines.isEmpty()||!lines.get(lines.size()-1).text().equals(why))EventFeed.push("notice",GravesAndGloryClient.team,why,now);
    }
    static void ping(String kind,double x,double z){if(ClientPlayNetworking.canSend(GravesAndGlory.PING)){var b=PacketByteBufs.create();b.writeString(kind);b.writeDouble(x);b.writeDouble(z);ClientPlayNetworking.send(GravesAndGlory.PING,b);}}
    static void issue(String group,String action,String form,Vec3d dest,float yaw,int target){
        if(!GravesAndGloryClient.battleActive()||!ClientPlayNetworking.canSend(GravesAndGlory.TACTICAL))return;
        var b=PacketByteBufs.create();b.writeString(group);b.writeString(action);b.writeString(form);b.writeDouble(dest.x);b.writeDouble(dest.z);b.writeFloat(yaw);b.writeInt(target);ClientPlayNetworking.send(GravesAndGlory.TACTICAL,b);
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
}

