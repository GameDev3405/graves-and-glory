package gg.gravesandglory;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gameplay props (stake barricade, Kort 2026-11-09): the rules of a def's {@link PropDef.Gameplay} block.
 * <ul>
 * <li>Destructible: a prop with hp takes hits and at 0 is removed through {@link Props#remove} (barriers, parts, anchor, floor back) with a wood
 *     break. A hit is a heavy melee hit (a battle player's full swing at one of its barriers with a weapon of at least heavyDamage, or a troop's
 *     slam or trample splash reaching a cell) worth heavyHit, or siege fire (a siege troop's shell burst reaching a cell, a siege troop's shot at a
 *     target it covers, a vanilla explosion within its power) worth siegeHit. Damage is held in memory, not saved: a restart mends it.</li>
 * <li>Cover: a ranged troop shot (arrow, trident, magic bolt) at a target standing within coverRange blocks behind its solid cells, the cells
 *     lying between the target and the shooter, deals 1 - cover of its damage (Combat.resolve).</li>
 * <li>Siege placement: in a Siege round a seated defender may place up to siegeCap in the first siegeSeconds after the intro, within siegeRadius
 *     of the Outer Yard, with {@code /barricade} and the prop ghost (PropMenu). Each placement saves its box first as a mode saved piece, so the
 *     round's teardown (and crash recovery) puts the ground back.</li>
 * </ul>
 */
public final class PropGameplay {
    private PropGameplay(){}
    /** Damage taken by each destructible prop, by anchor cell. */
    private static final Map<World,Map<BlockPos,Integer>> DAMAGE=new HashMap<>();
    /** Siege placements this round, by commander; reset when the battle's round number changes. */
    private static final Map<UUID,Integer> PLACED=new HashMap<>();
    private static int placedRound=-1;
    /** The battle placedRound counts in: a new Battle in the same JVM restarts its round numbers. */
    private static GravesAndGlory.Battle placedBattle;
    /** The swing a player's full-swing counts from: the server never resets a player's attack cooldown on a block swing, so this does. */
    static final float FULL_SWING=.9f;
    static final double SWING_REACH=4.5;
    public static final String SIEGE_PROP="stake_barricade";

    public static void init(){
        ServerWorldEvents.UNLOAD.register((server,w)->DAMAGE.remove(w));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->{DAMAGE.clear();PLACED.clear();placedBattle=null;placedRound=-1;});
        CommandRegistrationCallback.EVENT.register((d,access,env)->d.register(CommandManager.literal("barricade")
            .executes(c->startSiege(c.getSource().getPlayerOrThrow(),SIEGE_PROP))
            .then(CommandManager.argument("id",StringArgumentType.word()).suggests((c,b)->CommandSource.suggestMatching(siegeIds(),b))
                .executes(c->startSiege(c.getSource().getPlayerOrThrow(),StringArgumentType.getString(c,"id"))))));
    }

    static int startSiege(ServerPlayerEntity p,String id){
        var b=GravesAndGlory.battle;
        String why=b==null?"No battle is running.":siegeRefusal(b,b.squads.get(p.getUuid()),id,null);
        if(!why.isEmpty()){p.sendMessage(Text.literal(why).formatted(Formatting.RED),false);return 0;}
        PropMenu.startSiege(p,id);
        return 1;
    }
    static List<String> siegeIds(){
        List<String> out=new ArrayList<>();
        for(PropDef d:Props.DEFS.values())if(d.gameplay()!=null&&d.gameplay().siegeCap()>0)out.add(d.id());
        return out;
    }

    static PropDef.Gameplay rules(Props.Live l){return l==null?null:l.def().gameplay();}

    // ---- destructible -----------------------------------------------------------------------------------------------------------------

    /** The damage the prop at this anchor cell has taken (0 when none or not destructible). */
    public static int damage(ServerWorld w,BlockPos anchor){Map<BlockPos,Integer> m=DAMAGE.get(w);return m==null?0:m.getOrDefault(anchor,0);}
    /** Forgets every prop's damage in w: a round starts or ends (Battle.startRound, BattleMode.teardown). */
    public static void clear(ServerWorld w){DAMAGE.remove(w);}
    /** Forgets a prop's damage at this anchor cell (Props.remove, and Props.place for a fresh prop where one went another way: a restore, a scan). */
    static void forget(ServerWorld w,BlockPos anchor){Map<BlockPos,Integer> m=DAMAGE.get(w);if(m!=null)m.remove(anchor);}

    /** One hit of {@code amount} on the destructible prop at anchor cell {@code anchor}; at its hp it is removed. Returns whether it took the hit. */
    public static boolean hit(ServerWorld w,BlockPos anchor,int amount,String why){
        Props.Live l=Props.readAnchor(w,anchor);
        PropDef.Gameplay g=rules(l);
        if(g==null||g.hp()<=0||amount<=0)return false;
        int taken=DAMAGE.computeIfAbsent(w,k->new HashMap<>()).merge(anchor.toImmutable(),amount,Integer::sum);
        List<BlockPos> cells=Props.solidCells(l.def(),anchor,l.facing());
        var wood=new BlockStateParticleEffect(ParticleTypes.BLOCK,Blocks.OAK_PLANKS.getDefaultState());
        Vec3d mid=middle(cells);
        if(taken>=g.hp()){
            Props.remove(w,anchor);
            for(BlockPos c:cells)w.spawnParticles(wood,c.getX()+.5,c.getY()+.5,c.getZ()+.5,14,.3,.3,.3,.05);
            w.playSound(null,mid.x,mid.y,mid.z,SoundEvents.BLOCK_WOOD_BREAK,SoundCategory.BLOCKS,1.2f,.8f);
            Props.LOG.info("[props] {} at {} broken by {}",l.def().id(),anchor.toShortString(),why);
        }else{
            w.spawnParticles(wood,mid.x,mid.y,mid.z,8,.6,.4,.3,.05);
            w.playSound(null,mid.x,mid.y,mid.z,SoundEvents.BLOCK_WOOD_HIT,SoundCategory.BLOCKS,1f,.8f);
        }
        return true;
    }
    static Vec3d middle(List<BlockPos> cells){
        double x=0,y=0,z=0;for(BlockPos c:cells){x+=c.getX()+.5;y+=c.getY()+.5;z+=c.getZ()+.5;}
        int n=Math.max(1,cells.size());return new Vec3d(x/n,y/n,z/n);
    }

    /** The live destructible props with a solid cell centre within {@code radius} of {@code at}. */
    static List<Props.Live> near(ServerWorld w,Vec3d at,double radius){
        List<Props.Live> out=new ArrayList<>();
        for(Props.Live l:Props.live(w)){
            PropDef.Gameplay g=rules(l);
            if(g==null||g.hp()<=0)continue;
            for(BlockPos c:Props.solidCells(l.def(),l.anchor(),l.facing()))
                if(at.squaredDistanceTo(c.getX()+.5,c.getY()+.5,c.getZ()+.5)<=radius*radius){out.add(l);break;}
        }
        return out;
    }
    /** A troop's slam or trample splash at {@code at}: a heavy hit on every destructible prop it reaches. */
    public static int slam(ServerWorld w,Vec3d at,double radius){
        int n=0;for(Props.Live l:near(w,at,radius))if(hit(w,l.anchor(),l.def().gameplay().heavyHit(),"a slam"))n++;return n;
    }
    /** Siege fire (a siege troop's shell burst, an explosion) at {@code at}: a siege hit on every destructible prop it reaches. */
    public static int siegeFire(ServerWorld w,Vec3d at,double radius,String why){
        int n=0;for(Props.Live l:near(w,at,radius))if(hit(w,l.anchor(),l.def().gameplay().siegeHit(),why))n++;return n;
    }
    /** A vanilla explosion (ExplosionMixin): siege fire within its power, at least 2 blocks. */
    public static void explosion(ServerWorld w,Vec3d at,float power){
        var b=GravesAndGlory.battle;
        if(b==null||!b.running||b.world!=w)return; // a battle's explosions only: TNT or a creeper in the lobby or a build world leaves props alone
        siegeFire(w,at,Math.max(2,power),"an explosion");
    }

    /**
     * A battle player's swing (HandSwingMixin): a heavy hit on the destructible prop whose barrier he is looking at within reach, when the swing is
     * full (his attack cooldown at least FULL_SWING) and his weapon deals at least the prop's heavyDamage. A counted swing restarts his cooldown.
     * Returns whether it hit.
     */
    public static boolean swing(ServerPlayerEntity p){
        var b=GravesAndGlory.battle;
        if(b==null||!b.running||p.isSpectator()||!(p.getWorld() instanceof ServerWorld w)||w!=b.world)return false;
        var s=b.squads.get(p.getUuid());
        if(s==null||!s.seated)return false; // a seated commander of either side; a waiting player, an op or a spectator does not
        if(p.getAttackCooldownProgress(.5f)<FULL_SWING)return false;
        HitResult hr=p.raycast(SWING_REACH,1f,false);
        if(hr.getType()!=HitResult.Type.BLOCK)return false;
        BlockPos pos=((BlockHitResult)hr).getBlockPos();
        if(!w.getBlockState(pos).isOf(Blocks.BARRIER))return false;
        BlockPos anchor=Props.owner(w,pos);
        PropDef.Gameplay g=anchor==null?null:rules(Props.readAnchor(w,anchor));
        if(g==null||g.hp()<=0||p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)<g.heavyDamage())return false;
        p.resetLastAttackedTicks();
        return hit(w,anchor,g.heavyHit(),p.getName().getString());
    }

    // ---- cover -------------------------------------------------------------------------------------------------------------------------

    /** A target's cover from a shooter: the prop and its def's cover share; null when none. */
    public record Cover(Props.Live prop,double share){}

    /**
     * The cover {@code target} has from a ranged shot fired from {@code from}: the first prop with cover > 0 one of whose solid cells lies on the
     * level line from the target toward the shooter, within its coverRange of the target (and nearer the target than the shooter is), at the
     * target's feet or the block over them. Null when none.
     */
    public static Cover cover(ServerWorld w,Vec3d from,LivingEntity target){
        Vec3d t=target.getPos();
        double dx=from.x-t.x,dz=from.z-t.z,len=Math.sqrt(dx*dx+dz*dz);
        if(len<1e-3)return null;
        dx/=len;dz/=len;
        for(Props.Live l:Props.live(w)){
            PropDef.Gameplay g=rules(l);
            if(g==null||g.cover()<=0||g.coverRange()<=0)continue;
            double reach=g.coverRange()+Math.max(l.def().sx(),Math.max(l.def().sy(),l.def().sz()))+1;
            if(l.anchor().getSquaredDistance(t)>reach*reach)continue;
            Set<BlockPos> cells=new HashSet<>(Props.solidCells(l.def(),l.anchor(),l.facing()));
            int feet=MathHelper.floor(t.y+.1);
            for(double d=.25;d<=g.coverRange()+1e-9&&d<len;d+=.25){
                int x=MathHelper.floor(t.x+dx*d),z=MathHelper.floor(t.z+dz*d);
                if(cells.contains(new BlockPos(x,feet,z))||cells.contains(new BlockPos(x,feet+1,z)))return new Cover(l,g.cover());
            }
        }
        return null;
    }

    /**
     * The factor a ranged troop shot of this kind deals to {@code target} for its cover (Combat.resolve): 1 - cover's share, else 1. A siege
     * troop's shot that a prop covers strikes the prop as siege fire as well.
     */
    static double coverFactor(GravesAndGlory.Battle b,GravesAndGlory.Unit attacker,LivingEntity target,String kind){
        if(!kind.equals("arrow")&&!kind.equals("bolt"))return 1;
        Cover c=cover(b.world,attacker.mob.getPos(),target);
        if(c==null)return 1;
        PropDef.Gameplay g=c.prop().def().gameplay();
        if(attacker.tags.contains("siege")&&g.hp()>0)hit(b.world,c.prop().anchor(),g.siegeHit(),"siege fire");
        return 1-c.share();
    }

    // ---- Siege placement ---------------------------------------------------------------------------------------------------------------

    /** How many props this commander placed in the battle's current round. */
    static int placed(GravesAndGlory.Battle b,UUID owner){
        if(placedBattle!=b||placedRound!=b.rounds){PLACED.clear();placedBattle=b;placedRound=b.rounds;}
        return PLACED.getOrDefault(owner,0);
    }

    /**
     * Why commander squad {@code s} may not place prop {@code id} for Siege now (at {@code at}, or anywhere when null): "" when he may. A seated
     * defender in a running Siege round, within siegeSeconds after the intro, under his siegeCap, within siegeRadius of the Outer Yard.
     */
    public static String siegeRefusal(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String id,BlockPos at){
        PropDef d=Props.DEFS.get(id);
        PropDef.Gameplay g=d==null?null:d.gameplay();
        if(g==null||g.siegeCap()<=0)return "Defenders cannot place "+(d==null?id:d.name())+".";
        if(b==null||!b.running||!(b.mode instanceof SiegeMode))return "Barricades are placed only in a Siege.";
        if(s==null||!s.seated||s.team!=1)return "Only the defenders place barricades.";
        long until=b.campaign.introUntil+g.siegeSeconds()*20L;
        if(b.time>=until)return "Too late: barricades go up in the first "+g.siegeSeconds()+" seconds.";
        if(placed(b,s.owner)>=g.siegeCap())return "You have placed all "+g.siegeCap()+" barricades.";
        if(at!=null){
            double x=at.getX()+.5-SiegeMode.YARD_X,z=at.getZ()+.5-SiegeMode.YARD_Z;
            if(x*x+z*z>g.siegeRadius()*g.siegeRadius())return "Too far from the Outer Yard.";
        }
        return "";
    }

    /** As Props.check, with the Siege rules first. */
    public static String siegeCheck(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String id,BlockPos at,int facing){
        String why=siegeRefusal(b,s,id,at);
        return why.isEmpty()?Props.check(b.world,id,at,facing):why;
    }

    /**
     * Places a Siege defender's prop: the box it can change (its cells and the column under them down to the anchor's deepest cell) is saved as one of
     * the round's saved pieces first, so teardown and crash recovery put the ground back. "" on success, else the refusal and nothing changed.
     */
    public static String siegePlace(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String id,BlockPos at,int facing){
        String why=siegeCheck(b,s,id,at,facing);
        if(!why.isEmpty())return why;
        PropDef d=Props.DEFS.get(id);
        List<BlockPos> cells=Props.cells(d,at,facing);
        int x0=Integer.MAX_VALUE,y0=at.getY()-Props.MAX_DEPTH,z0=Integer.MAX_VALUE,x1=Integer.MIN_VALUE,y1=Integer.MIN_VALUE,z1=Integer.MIN_VALUE;
        for(BlockPos c:cells){x0=Math.min(x0,c.getX());z0=Math.min(z0,c.getZ());x1=Math.max(x1,c.getX());y1=Math.max(y1,c.getY());z1=Math.max(z1,c.getZ());}
        y0=Math.max(y0,b.world.getBottomY());
        int n=placed(b,s.owner);
        String tag="siege-prop-"+b.rounds+"-"+s.owner.toString().substring(0,8)+"-"+n;
        var saved=Structures.snapshot(b.world,new BlockPos(x0,y0,z0),new Vec3i(x1-x0+1,y1-y0+1,z1-z0+1),tag);
        if(saved==null)return "The ground could not be saved; nothing placed.";
        why=Props.place(b.world,id,at,facing);
        if(!why.isEmpty()){try{java.nio.file.Files.deleteIfExists(saved.file());}catch(Exception ignored){}return why;}
        b.saves.add(saved);
        b.mode.mark(b);
        PLACED.put(s.owner,n+1);
        return "";
    }
}
