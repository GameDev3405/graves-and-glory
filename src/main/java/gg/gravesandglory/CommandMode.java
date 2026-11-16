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

/** Selection -> live-world aim/formation preview -> explicit confirmation. While an order is aimed the battle HUD shows its key
 *  hints (BattleHud.altLayer) and the aimed point's name under its seal; a refused confirmation is a notice in the event feed. */
public final class CommandMode {
    /** One group of the player's army as the server last sent it: full is the most of it alive at once this round (0: no card), strength its living health over its full health; level2 and level3 count its living soldiers at those levels (state_v9), and armouryOpen is set while an Armoury buy for a troop of the group is open and not yet bought (the Iron Ledger pips and glint, Task 12). */
    public record Group(String id,int count,float health,String order,String formation,float x,float z,float yaw,float cx,float cz,boolean fighting,boolean lastStand,int full,float strength,int level2,int level3,boolean armouryOpen){}
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
    static int count(String id){return groups.stream().filter(g->id.equals("all")||id.equals(g.id())).mapToInt(Group::count).sum();}
    static void begin(String order){if(!GravesAndGloryClient.battleActive())return;CommandSelection.clearAimKeep();pending=order;facingSet=false;MinecraftClient.getInstance().setScreen(null);}
    public static boolean cancel(){CommandSelection.clearAimKeep();if(!active())return false;pending="";destination=null;return true;}
    /** A disconnect: no order stays aimed, and the cards' groups and loss flashes go, so the next server's first snapshot is compared
     *  with nothing (noteGroups would flash a card for the old battle's losses). The rest of the command state starts as a new
     *  client's: the map's pings, the clock, the whole army selected (a group the next world lacks would leave no card selected),
     *  the default formation and the facing taken from the player again. */
    static void reset(){cancel();held=null;sentAt=Long.MIN_VALUE/2;lastAnswer="";preview.clear();enemy=-1;groups=java.util.List.of();flashUntil.clear();flashFrom.clear();pings=java.util.List.of();seconds=600;selected="all";formation="block";facing=0;facingSet=false;}
    /** Sends the aimed order to the selected groups: a Charge from the selection's centre along the look yaw (the server clamps the run), anything else at the aimed point. */
    public static boolean confirm(){
        if(!active())return false;
        String order=pending;boolean charge=order.equals("charge");
        if(!charge&&destination==null){answer(OrderReply.OUT_OF_RANGE,0,"");return true;}
        pending="";
        if(charge)CommandSelection.issueSelected(order,formation,CommandSelection.selectedCentre(),facing,-1);
        else CommandSelection.issueSelected(order,formation,destination,facing,enemy);
        CommandSelection.afterOrder(CommandSelection.keepOpenOnConfirm());
        return true;
    }
    /** A refused order's one answer (army control spec section 5): the deny sound and the code's line on the action bar, the same place the
     *  server's accepted line takes. The server's refusals (command_v1 REFUSE) and the client's own (no ground under the aim, the Charge not
     *  ready) both come here, so a refusal reads the same whichever side caught it. */
    static void answer(String code,int n,String subject){
        UiSounds.deny();lastAnswer=OrderReply.refusal(code,n,subject);
        var c=MinecraftClient.getInstance();if(c.inGameHud!=null)c.inGameHud.setOverlayMessage(net.minecraft.text.Text.literal(lastAnswer),false);
    }
    /** The last refusal line shown (for the harness). */
    static String lastAnswer="";
    static void ping(String kind,double x,double z){if(ClientPlayNetworking.canSend(GravesAndGlory.PING)){var b=PacketByteBufs.create();b.writeString(kind);b.writeDouble(x);b.writeDouble(z);ClientPlayNetworking.send(gg.gravesandglory.net.Payloads.PingPayload.of(b));}}
    /** Order packets this client has sent (for the harness: a multi-group order must be one packet). */
    static int ordersSent;
    /** One group's order: a one-entry tactical_v2 packet. */
    static void issue(String group,String action,String form,Vec3d dest,float yaw,int target){issue(java.util.List.of(new Aim(group,dest)),action,form,yaw,target);}
    /** One group of an order and its own point (a Charge's start, a Formation's anchor). */
    record Aim(String group,Vec3d at){}
    /** Client ticks counted by tick(), the tick the last order went out, and an order held because it came inside the command cooldown. */
    static long ticks,sentAt=Long.MIN_VALUE/2;static Runnable held;
    /** Orders held back by the cooldown and sent when it ended (for the harness). */
    static int heldSent;
    /** A key's or the panel's order (CommandSelection.issueSelected): given within the server's commandCooldownTicks (plus one tick for the
     *  network's jitter) of the last order sent, it is held and sent when that ends, so a quick second press is never lost to the silent
     *  cooldown (spec section 5); a later press replaces a held one. */
    static void issueOrHold(java.util.List<Aim> groups,String action,String form,float yaw,int target){
        if(groups.isEmpty()||!GravesAndGloryClient.battleActive())return;
        if(ticks-sentAt<=Spec.RULES[0].commandCooldownTicks()){var copy=java.util.List.copyOf(groups);held=()->{heldSent++;issue(copy,action,form,yaw,target);};return;}
        issue(groups,action,form,yaw,target);
    }
    /** One order to every listed group as ONE tactical_v2 packet (the server applies it as one order: one cooldown, one Charge). */
    static void issue(java.util.List<Aim> groups,String action,String form,float yaw,int target){
        if(groups.isEmpty()||!GravesAndGloryClient.battleActive()||!ClientPlayNetworking.canSend(GravesAndGlory.TACTICAL_V2))return;
        sentAt=ticks;held=null;
        var b=PacketByteBufs.create();b.writeString(action);b.writeString(form);b.writeFloat(yaw);b.writeInt(target);
        b.writeInt(groups.size());for(var g:groups){b.writeString(g.group());b.writeDouble(g.at().x);b.writeDouble(g.at().z);}
        ClientPlayNetworking.send(gg.gravesandglory.net.Payloads.TacticalV2Payload.of(b));ordersSent++;
    }
    /** The team a unit fights for, read from its name colour (team 0 aqua, team 1 light purple; see GravesAndGlory.spawn), or -1. */
    static int nameTeam(net.minecraft.entity.Entity e){
        var name=e.getCustomName();var colour=name==null?null:name.getStyle().getColor();if(colour==null)return -1;
        if(colour.getRgb()==net.minecraft.util.Formatting.AQUA.getColorValue())return 0;
        if(colour.getRgb()==net.minecraft.util.Formatting.LIGHT_PURPLE.getColorValue())return 1;
        return -1;
    }
    /** The army's own troops of this group, as the state packet names them (its markers); a troop's sheet row names its mob and tags. */
    static java.util.List<Spec.TroopsRow> troops(Group group){
        java.util.List<Spec.TroopsRow> out=new java.util.ArrayList<>();
        for(var m:GravesAndGloryClient.markers){if(!m.own())continue;var spec=ArmyMobs.named(m.name());if(spec!=null&&spec.group().equals(group.id()))out.add(spec);}
        return out;
    }
    /**
     * The places, {side, back} from the order's point before facing, the server forms a group of `count` in (Battle.formation, through the same
     * BattleMath.holdLayout): on Hold its shield troops close into the wall in the front ranks, the rest form behind them, and any other order
     * forms all of it in one cell, the formation's spacing or the widest body's room. `troops` are the group's soldiers as far as known; any
     * count past them is laid out as a husk-wide soldier with no shield. The places depend only on the counts and cells, so the markers'
     * order need not be the server's slot order.
     */
    static double[][] places(java.util.List<Spec.TroopsRow> troops,int count,String groupId,String order,Spec.FormationsRow f){
        if(f.id().equals("banner"))return bannerPlaces(troops,count);
        boolean[] shield=new boolean[count];double[] width=new double[count];
        for(int i=0;i<count;i++){var t=i<troops.size()?troops.get(i):null;width[i]=t==null?net.minecraft.entity.EntityType.HUSK.getWidth():ArmyMobs.width(t);shield[i]=t!=null&&order.equals("hold")&&ArmyMobs.tags(t).contains("shield");}
        double[][] places=BattleMath.holdLayout(shield,width,f.spacing(),Spec.COMBATDISCIPLINE[0].shieldWallCell(),BattleMath.groupColumns(groupId,f.columns()),f.wedge());
        double role=BattleMath.roleDepth(groupId);for(double[] p:places)p[1]+=role;
        return places;
    }
    /** The Banner shape's places for a group: each of its troops at the war table cell the player's banner gives that troop type (cells taken in order), any past them in a row behind the table (BattleMath.bannerPlaces). */
    static double[][] bannerPlaces(java.util.List<Spec.TroopsRow> troops,int count){
        java.util.Map<String,Integer> need=new java.util.HashMap<>();for(var t:troops)need.merge(t.id(),1,Integer::sum);
        java.util.List<Integer> cells=new java.util.ArrayList<>();
        var profile=CampaignClient.profile;
        if(profile!=null){var line=profile.line();for(int i=0;i<line.size()&&cells.size()<count;i++){Integer left=need.get(line.get(i));if(left!=null&&left>0){need.put(line.get(i),left-1);cells.add(i);}}}
        return BattleMath.bannerPlaces(cells,count);
    }
    static void tick(MinecraftClient c){
        ticks++;if(held!=null&&ticks-sentAt>Spec.RULES[0].commandCooldownTicks()){var send=held;held=null;send.run();}
        preview.clear();if(!GravesAndGloryClient.battleActive()){cancel();held=null;return;}if(!active()||c.player==null||c.world==null||c.currentScreen!=null)return;
        if(!facingSet)facing=c.player.getYaw();
        var hit=c.player.raycast(Spec.COMMANDINTERFACE[0].targetRange(),1,false);destination=hit.getType()==HitResult.Type.MISS?null:hit.getPos();enemy=-1;
        // Entity targeting is an aim cone, not an omniscient enemy lookup.
        Vec3d eye=c.player.getEyePos(),look=c.player.getRotationVec(1);double best=destination==null?64:eye.distanceTo(destination);
        for(var e:c.world.getEntities())if(e instanceof LivingEntity living&&e!=c.player&&e.isAlive()&&e.hasCustomName()&&MapBlockRules.canSee(c.player,e)){
            Vec3d v=e.getPos().add(0,1,0).subtract(eye);double along=v.dotProduct(look);
            // Both teams can field the same House, so the side comes from the name colour the server gives every unit.
            boolean foe=false;int side=nameTeam(e);if(ArmyMobs.named(e.getCustomName().getString())!=null&&GravesAndGloryClient.team>=0&&side>=0&&side!=GravesAndGloryClient.team)foe=true;
            if(foe&&along>0&&along<best&&v.subtract(look.multiply(along)).length()<1.3){best=along;enemy=e.getId();destination=e.getPos();}
        }
        if(destination==null||Math.abs(destination.x)>Field.halfX()-10||Math.abs(destination.z)>Field.halfZ()-10){destination=null;return;}
        if(enemy<0){double[] dry=Arena.orderGround(c.world,destination.x,destination.z,c.player.getX()); // water: the ring stands on the dry ground the server will send the troops to (OrderRules.dryPoint)
            if(dry==null||Math.abs(dry[0])>Field.halfX()-10||Math.abs(dry[1])>Field.halfZ()-10){destination=null;return;}
            if(dry[0]!=destination.x||dry[1]!=destination.z)destination=new Vec3d(dry[0],destination.y,dry[1]);}
        var f=java.util.Arrays.stream(Spec.FORMATIONS).filter(v->v.id().equals(formation)).findFirst().orElse(Spec.FORMATIONS[0]);
        var dust=new DustParticleEffect(new Vector3f(.95f,.82f,.44f),1.15f);
        for(Group group:groups)if(CommandSelection.isSelected(group.id())){ // the full selection, as issueSelected sends it (C04-F2)
            for(double[] place:places(troops(group),group.count(),group.id(),pending,f)){
                double side=place[0],back=place[1];
                double angle=Math.toRadians(facing),x=destination.x+Math.cos(angle)*side+Math.sin(angle)*back,z=destination.z+Math.sin(angle)*side-Math.cos(angle)*back;
                preview.add(new Vec3d(x,Arena.footing(net.minecraft.util.math.MathHelper.floor(x),net.minecraft.util.math.MathHelper.floor(z))+1.05,z));
            }
        }
        double angle=Math.toRadians(facing);for(int i=0;i<6;i++){double x=destination.x-Math.sin(angle)*i,z=destination.z+Math.cos(angle)*i;preview.add(new Vec3d(x,Arena.footing(net.minecraft.util.math.MathHelper.floor(x),net.minecraft.util.math.MathHelper.floor(z))+1.05,z));}
    }
    static void renderWorld(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context){
        if(!active()||context.consumers()==null||CommandSelection.aiming().equals("hold")||ChargeCamera.held())return; /* a Formation aim is drawn by OrderOutlines, an overhead Charge by ChargeCamera */ var matrices=context.matrixStack();var camera=context.camera().getPos();matrices.push();matrices.translate(-camera.x,-camera.y,-camera.z);
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
        if(side!=null){DarkUi.teamMark(g,team,16,y+4);g.drawTextWithShadow(c.textRenderer,c.textRenderer.trimToWidth(side.name().toUpperCase(java.util.Locale.ROOT)+" / HOUSES",190),28,y+4,DarkUi.MUTED);}
        int x=16;for(var house:Spec.HOUSES){int tier=tiers.getOrDefault(house.id(),0);if(tier<=0)continue;DarkUi.houseMark(g,house.id(),tier,x,y+17);x+=DarkUi.houseMarkWidth(tier)+12;}
    }
    /** Where vanilla draws the hearts and the armour row (GUI x width / 2 - 91), in the HUD frame of width w (the GUI scaled by min(1, width / 640)). */
    static int statusBarsLeft(int w){int sw=MinecraftClient.getInstance().getWindow().getScaledWidth();return (int)Math.floor((sw/2-91)*(w/(double)sw));}
    /** The right edge the group panels were last drawn to, in the HUD frame (read by the checks). */
    static int groupsRight;
}

