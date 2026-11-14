package gg.gravesandglory;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import gg.gravesandglory.mixin.DisplayPoseAccessor;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Alive effects (prop kit plan task S5): every {@link PropEffects#STEP} ticks per world, each live prop with a player of that world within
 * {@link Props#RANGE} blocks plays the vanilla particles and sounds {@link PropEffects#due} gives it. Props with no player near are skipped
 * (gated) before any lookup. Props with {@code motion} also get their part keyframes here ({@link #motion}, docs/props/MOVING-PARTS.md). {@code /prop stats} (op 2) prints live props, gated props and the mean cost of a run over the last 200 runs.
 */
public final class PropTicker {
    private PropTicker(){}
    static final int WINDOW=200;

    static final class Stats{
        final long[] nanos=new long[WINDOW];int runs,next,live,gated,played,keyframes;
        void add(long ns){nanos[next]=ns;next=(next+1)%WINDOW;if(runs<WINDOW)runs++;}
        double meanMicros(){long s=0;for(int i=0;i<runs;i++)s+=nanos[i];return runs==0?0:s/1000.0/runs;}
        double maxMicros(){long m=0;for(int i=0;i<runs;i++)m=Math.max(m,nanos[i]);return m/1000.0;}
    }
    private static final Map<World,Stats> STATS=new HashMap<>();
    /** Resolved effect ids: a DefaultParticleType, a SoundEvent, or Boolean.FALSE for an id that names neither. */
    private static final Map<String,Object> IDS=new HashMap<>();

    public static void init(){
        ServerTickEvents.END_WORLD_TICK.register(PropTicker::tick);
        ServerWorldEvents.UNLOAD.register((server,w)->{STATS.remove(w);MOVING.remove(w);});
        CommandRegistrationCallback.EVENT.register((d,access,env)->d.register(CommandManager.literal("prop").requires(s->s.hasPermissionLevel(2))
            .then(CommandManager.literal("stats").executes(c->{
                ServerWorld w=c.getSource().getWorld();
                Stats s=STATS.get(w);
                String text=s==null?"No prop runs yet in "+w.getRegistryKey().getValue()
                    :String.format(java.util.Locale.ROOT,"Props in %s: live %d, gated %d, played %d, motion keyframes %d; mean %.2f us/run, max %.2f us over the last %d runs; server tick %.2f ms",
                        w.getRegistryKey().getValue(),s.live,s.gated,s.played,s.keyframes,s.meanMicros(),s.maxMicros(),s.runs,w.getServer().getTickTime());
                c.getSource().sendFeedback(()->Text.literal(text),false);
                return 1;
            }))));
    }

    static void tick(ServerWorld w){
        long tick=w.getTime();
        if(tick%PropEffects.STEP!=0)return;
        long t0=System.nanoTime();
        java.util.Collection<Props.Live> all=Props.liveView(w);
        List<ServerPlayerEntity> players=w.getPlayers();
        double r2=Props.RANGE*Props.RANGE;
        int live=0,gated=0,played=0;
        double m2=PropMotion.RANGE*PropMotion.RANGE;
        int moved=0;
        for(Props.Live l:all){
            live++;
            double cx=l.anchor().getX()+.5,cy=l.anchor().getY()+.5,cz=l.anchor().getZ()+.5;
            boolean near=false,inSight=false;
            for(int i=0,n=players.size();i<n;i++){
                ServerPlayerEntity p=players.get(i);
                double dx=p.getX()-cx,dy=p.getY()-cy,dz=p.getZ()-cz,d2=dx*dx+dy*dy+dz*dz;
                if(d2<=m2)inSight=true;
                if(d2<=r2){near=true;break;}
            }
            if(PropMotion.moves(l.def()))moved+=motion(w,l,tick,inSight);
            if(!near){gated++;continue;}
            if(l.def().effects().isEmpty())continue;
            Props.Live now=Props.readAnchor(w,l.anchor()); // an anchor erased by hand stops playing at once
            if(now==null)continue;
            List<PropDef.Effect> due=PropEffects.due(now.def(),tick,PropEffects.seed(l.anchor().getX(),l.anchor().getY(),l.anchor().getZ()));
            for(PropDef.Effect e:due){play(w,now,e);played++;}
        }
        Stats s=STATS.computeIfAbsent(w,k->new Stats());
        s.live=live;s.gated=gated;s.played=played;s.keyframes=moved;
        s.add(System.nanoTime()-t0);
    }

    // ---- moving parts (docs/props/MOVING-PARTS.md) ------------------------------------------------------------------------------------

    /** A moving prop's part entities (by def part index, null for a part that does not move) and whether its last keyframe was the rest pose. */
    static final class Moving{DisplayEntity[] parts;PropDef def;}
    private static final Map<World,Map<BlockPos,Moving>> MOVING=new HashMap<>();

    /** One ticker run for a prop with motion: on its keyframe slot, while a player is within {@link PropMotion#RANGE}, each moving part gets the
     *  pose due {@link PropMotion#KEY} ticks on, tweened by the client over KEY ticks. When the last player leaves range the parts get the rest
     *  pose once (eased over KEY ticks) and then nothing: paused. Returns the keyframes sent (entity updates, one per moving part). */
    static int motion(ServerWorld w,Props.Live l,long tick,boolean inSight){
        Map<BlockPos,Moving> m=MOVING.computeIfAbsent(w,k->new HashMap<>());
        Moving mv=m.get(l.anchor());
        if(!inSight&&mv==null)return 0;   // at rest and nobody near: nothing to do
        long seed=PropEffects.seed(l.anchor().getX(),l.anchor().getY(),l.anchor().getZ());
        if(inSight&&!PropMotion.keyframe(tick,seed))return 0;
        if(mv==null||mv.def!=l.def()||!valid(mv)){
            mv=find(w,l);
            if(mv==null){m.remove(l.anchor());return 0;}
            m.put(l.anchor(),mv);
        }
        int sent=0;
        for(int i=0;i<mv.parts.length;i++){
            DisplayEntity e=mv.parts[i];
            if(e==null)continue;
            float[] p=inSight?PropMotion.pose(l.def(),l.def().parts().get(i),tick+PropMotion.KEY,seed):PropMotion.REST;
            setPose(e,p,PropMotion.KEY);sent++;
        }
        if(!inSight)m.remove(l.anchor());
        return sent;
    }
    static boolean valid(Moving mv){
        for(DisplayEntity e:mv.parts)if(e!=null&&e.isRemoved())return false;
        return true;
    }
    /** The prop's moving part entities, or null when one is missing (the scanner puts it back; the next run looks again). */
    static Moving find(ServerWorld w,Props.Live l){
        List<PropDef.Part> defParts=l.def().parts();
        Moving mv=new Moving();mv.def=l.def();mv.parts=new DisplayEntity[defParts.size()];
        List<net.minecraft.entity.Entity> ents=Props.parts(w,l.anchor());
        for(int i=0;i<defParts.size();i++){
            if(!PropMotion.moves(l.def(),defParts.get(i)))continue;
            for(net.minecraft.entity.Entity e:ents)if(e instanceof DisplayEntity d&&Props.partCmd(e)==defParts.get(i).cmd()&&!e.isRemoved())mv.parts[i]=d;
            if(mv.parts[i]==null)return null;
        }
        return mv;
    }
    /** Sets a part's transformation to {@code pose} ({tx,ty,tz,qx,qy,qz,qw}), tweened by the client over {@code ticks} from now. */
    public static void setPose(DisplayEntity e,float[] pose,int ticks){
        DisplayPoseAccessor a=(DisplayPoseAccessor)e;
        a.gravesandglory$setTransformation(new AffineTransformation(new Vector3f(pose[0],pose[1],pose[2]),new Quaternionf(pose[3],pose[4],pose[5],pose[6]),new Vector3f(1,1,1),new Quaternionf()));
        a.gravesandglory$setInterpolationDuration(ticks);
        a.gravesandglory$setStartInterpolation(0);
    }
    /** A part's current transformation as {tx,ty,tz,qx,qy,qz,qw}; for checks. */
    public static float[] pose(DisplayEntity e){
        AffineTransformation t=DisplayPoseAccessor.gravesandglory$getTransformation(e.getDataTracker());
        Vector3f v=t.getTranslation();Quaternionf q=t.getLeftRotation();
        return new float[]{v.x(),v.y(),v.z(),q.x(),q.y(),q.z(),q.w()};
    }
    /** Whether the ticker holds motion state for this anchor (it is moving, or not yet eased back to rest); for checks. */
    static boolean moving(ServerWorld w,BlockPos anchor){Map<BlockPos,Moving> m=MOVING.get(w);return m!=null&&m.containsKey(anchor);}
    /** Forgets a removed prop's motion state (Props.remove). */
    static void forget(ServerWorld w,BlockPos anchor){Map<BlockPos,Moving> m=MOVING.get(w);if(m!=null)m.remove(anchor);}
    /** Forgets the motion state of the props in an unloaded chunk (their parts were saved at rest, PropRestSaveMixin). */
    static void forgetChunk(ServerWorld w,net.minecraft.util.math.ChunkPos cp){
        Map<BlockPos,Moving> m=MOVING.get(w);
        if(m!=null)m.keySet().removeIf(a->(a.getX()>>4)==cp.x&&(a.getZ()>>4)==cp.z);
    }

    static void play(ServerWorld w,Props.Live l,PropDef.Effect e){
        Object kind=resolve(e);
        if(kind==Boolean.FALSE)return;
        double[] p=PropEffects.at(l.def(),l.facing(),e);
        double x=l.anchor().getX()+p[0],y=l.anchor().getY()+p[1],z=l.anchor().getZ()+p[2];
        if(kind instanceof DefaultParticleType t)w.spawnParticles(t,x,y,z,e.count(),e.spread(),e.spread()*.5,e.spread(),0.01);
        else if(kind instanceof SoundEvent s)w.playSound(null,x,y,z,s,SoundCategory.BLOCKS,e.volume(),e.pitch());
    }

    static Object resolve(PropDef.Effect e){
        String key=e.kind()+" "+e.id();
        Object o=IDS.get(key);
        if(o!=null)return o;
        o=Boolean.FALSE;
        try{
            Identifier id=new Identifier(e.id());
            if(e.kind().equals("particle")){
                ParticleType<?> t=Registries.PARTICLE_TYPE.get(id);
                if(t instanceof DefaultParticleType d&&Registries.PARTICLE_TYPE.getId(t).equals(id))o=d;
            }else if(Registries.SOUND_EVENT.containsId(id))o=Registries.SOUND_EVENT.get(id);
        }catch(InvalidIdentifierException x){/* stays FALSE */}
        if(o==Boolean.FALSE)Props.LOG.warn("[props] effect {} {} is not a plain vanilla particle or sound; skipped",e.kind(),e.id());
        IDS.put(key,o);
        return o;
    }
}
