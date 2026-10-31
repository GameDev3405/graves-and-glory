package gg.gravesandglory;

import com.mojang.blaze3d.systems.RenderSystem;
import gg.gravesandglory.mixin.EntityFlagsAccessor;
import gg.gravesandglory.mixin.EntityRenderDispatcherAccessor;
import gg.gravesandglory.mixin.RenderSystemAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The settled-body cache (Task 8 fix round 1, R39). A troop's body that lies past its fall with its pose kept
 * (TroopAnimator.settled) no longer changes, so its draw is recorded once, in its own frame (world axes about its feet, as
 * the renderer draws it from an identity matrix stack), into one GPU vertex buffer per render layer; every later frame draws
 * those buffers, layer by layer for all the bodies at once, after the entities (WorldRenderEvents.AFTER_ENTITIES), instead of
 * walking the model again. What the draw would have computed per frame is checked as a key (the Look, the model, the far
 * twins and relief the camera picks, the face, the light, the overlay, the shield, the bow draw and where it lies); a change
 * records it again, and a body that keeps changing goes back to the normal draw. A body with an outline, a label, an
 * invisible one, one drawn outside the world pass, and every body under Iris (whose passes draw entities with their own
 * programs) is drawn the normal way. The vertices are those the normal draw makes; only the matrices move to the GPU, so the
 * settled body looks the same (the fog's cylinder takes the camera's height over the body, a difference far below a pixel at
 * any distance an entity draws).
 */
public final class BodyCache {
    private BodyCache(){}
    /** Off with -Dgravesandglory.bodyCache=false, and always under Iris. */
    static final boolean ENABLED=!"false".equals(System.getProperty("gravesandglory.bodyCache"))&&!FabricLoader.getInstance().isModLoaded("iris");
    /** Harness switches (HoldLineChecks, hl-corpse): draw every body the normal way; leave the entity with this id undrawn. */
    static volatile boolean off;static volatile int hide=-1;
    /** Whether settled bodies draw from their buffers now. */
    static boolean on(){return ENABLED&&!off;}
    /** Records past which a body that keeps changing is drawn the normal way for good. */
    static final int MAX_RECORDS=6;
    /** Frames a body may go undrawn (out of view) before its buffers are freed. */
    static final int IDLE_FRAMES=600;
    /**
     * Frames a body draws from its buffers on the short check (TroopRenderer.shouldRender: where it was recorded, not on fire,
     * glowing, invisible or labelled) before the renderer checks its whole key again: what only the full check sees (its light,
     * Look, model, far twin, relief, face, overlay, shield, bow, a scene pose) is never more than this many frames late.
     */
    static final int VERIFY_FRAMES=24;

    /** What the draw depends on: a recorded body is drawn from its buffers while this stays equal. */
    record Key(Object model,TroopModels.Look look,boolean far,String relief,int variant,int light,int overlay,boolean shieldOff,boolean helmOff,boolean folds,float bow,double x,double y,double z){
        boolean same(Key k){
            return k.model==model&&Objects.equals(k.look,look)&&k.far==far&&Objects.equals(k.relief,relief)&&k.variant==variant&&k.light==light&&k.overlay==overlay
                &&k.shieldOff==shieldOff&&k.helmOff==helmOff&&k.folds==folds&&Float.compare(k.bow,bow)==0&&Math.abs(k.x-x)<1e-5&&Math.abs(k.y-y)<1e-5&&Math.abs(k.z-z)<1e-5;
        }
    }
    record Part(RenderLayer layer,VertexBuffer buffer){}
    static final class Body {
        Key key;final List<Part> parts=new ArrayList<>();int records;boolean normal;int lastFrame,verified=Integer.MIN_VALUE/2;double x,y,z;float yaw;Entity entity;TroopRenderer<?> renderer;net.minecraft.util.math.Box cull;
        void free(){for(Part p:parts)p.buffer.close();parts.clear();}
    }
    private static final Map<Entity,Body> BODIES=new IdentityHashMap<>();
    private static final List<Body> QUEUE=new ArrayList<>(),SHADOWS=new ArrayList<>();
    private static final Map<RenderLayer,List<Body>> BY_LAYER=new LinkedHashMap<>();
    private static final Map<RenderLayer,List<VertexBuffer>> BUFFERS=new LinkedHashMap<>();
    private static boolean inWorld;private static int frame;
    /** Draws from cached buffers and records since the client started (the harness reads them). */
    static long cachedDraws,records;
    /** Diagnostics for the harness: cached draws on the short check, draws refused outside the world pass, key changes, give-ups, failed records, and the last key change or refusal. */
    static long quick,outside,changes,giveUps,failed,flushNanos,flushes,flushDraws,flushBodies;static String lastChange="";
    static String diag(){return "cached draws "+cachedDraws+" ("+quick+" on the short check), records "+records+", outside the world pass "+outside+", key changes "+changes+", give-ups "+giveUps+", failed records "+failed+", kept "+kept()+", flush "+String.format(java.util.Locale.ROOT,"%.1f us for %.1f bodies and %.1f draws a frame",flushNanos/1e3/Math.max(1,flushes),flushBodies/(double)Math.max(1,flushes),flushDraws/(double)Math.max(1,flushes))+"; last: "+lastChange;}

    /** WorldRenderEvents.START: the world pass begins; bodies may queue. */
    static void begin(){inWorld=ENABLED;frame++;QUEUE.clear();SHADOWS.clear();}

    /**
     * Draws a settled body from its buffers (queued for this frame's flush), recording it first when its key changed; false
     * when it must be drawn the normal way. The renderer has resolved the Look and model and passes what its draw would use.
     */
    static <T extends net.minecraft.entity.mob.MobEntity> boolean draw(TroopRenderer<T> r,T e,Key key,float yaw,float body,float tickDelta){
        if(!inWorld){outside++;return false;}
        Body b=BODIES.get(e);
        if(b!=null&&b.normal)return false;
        if(b==null){b=new Body();b.entity=e;b.renderer=r;BODIES.put(e,b);}
        if(b.key==null||!b.key.same(key)){
            if(b.key!=null){changes++;lastChange=b.key+" -> "+key;}
            if(++b.records>MAX_RECORDS){b.free();b.normal=true;giveUps++;return false;}
            b.free();
            if(!record(r,e,key,yaw,tickDelta,b)){b.free();b.normal=true;failed++;return false;}
            b.key=key;records++;
        }
        b.x=key.x;b.y=key.y;b.z=key.z;b.yaw=body;b.lastFrame=frame;QUEUE.add(b);cachedDraws++;
        return true;
    }

    /** The entity's body when it drew from its buffers on a full check within the last VERIFY_FRAMES frames, else null. */
    static Body fresh(Entity e){
        if(!inWorld)return null;Body b=BODIES.get(e);
        return b!=null&&!b.normal&&!b.parts.isEmpty()&&b.cull!=null&&frame-b.verified<VERIFY_FRAMES?b:null;
    }
    /** A body BodyCache drew on a full check within the last VERIFY_FRAMES frames, at any point of the frame (TroopDepth.beginFrame). */
    static boolean recent(Entity e){Body b=BODIES.get(e);return b!=null&&!b.normal&&!b.parts.isEmpty()&&frame-b.verified<=VERIFY_FRAMES;}
    /** Not on fire, invisible or glowing (the shared flags, one tracker read): the dispatcher would draw nothing more, nor an outline. */
    static boolean plain(Entity e){return (e.getDataTracker().get(EntityFlagsAccessor.gravesandglory$flags())&(1|1<<5|1<<6))==0;}
    /**
     * The short check's draw, past the dispatcher: queued, with the shadow the dispatcher would have drawn, when it lies where it
     * was recorded (turned to its body yaw now); false sends it to the full check.
     */
    static boolean drawFresh(Body b,double x,double y,double z,float body){
        Key k=b.key;if(Math.abs(k.x()-x)>=1e-5||Math.abs(k.y()-y)>=1e-5||Math.abs(k.z()-z)>=1e-5)return false;
        b.yaw=body;b.lastFrame=frame;QUEUE.add(b);SHADOWS.add(b);cachedDraws++;quick++;return true;
    }
    /** After a full check drew the entity's body from its buffers: its cull box (it does not move) and the frame of the check. */
    static void verified(Entity e,net.minecraft.util.math.Box cull){Body b=BODIES.get(e);if(b!=null){b.cull=cull;b.verified=frame;}}

    /**
     * Records the body's draw into one buffer a layer, through an identity matrix stack, turned to body yaw 180 (where
     * setupTransforms turns it by nothing), its head yaw moved with it so the pose sees the same net head yaw: a lying body's
     * yaw keeps easing toward its target for a while after it settles, so flush turns it to its yaw of the frame instead.
     */
    private static <T extends net.minecraft.entity.mob.MobEntity> boolean record(TroopRenderer<T> r,T e,Key key,float yaw,float tickDelta,Body b){
        RECORDER.open.clear();
        float by=e.bodyYaw,pby=e.prevBodyYaw,hy=e.headYaw,phy=e.prevHeadYaw,turn=180f-net.minecraft.util.math.MathHelper.lerpAngleDegrees(tickDelta,pby,by);
        e.bodyYaw=e.prevBodyYaw=180f;e.headYaw=net.minecraft.util.math.MathHelper.lerpAngleDegrees(tickDelta,phy,hy)+turn;e.prevHeadYaw=e.headYaw;
        try{r.drawModel(e,yaw,tickDelta,new MatrixStack(),RECORDER,key.light());}
        catch(RuntimeException ex){RECORDER.abandon();throw ex;}
        finally{e.bodyYaw=by;e.prevBodyYaw=pby;e.headYaw=hy;e.prevHeadYaw=phy;}
        boolean ok=true;
        for(var en:RECORDER.open.entrySet()){
            RenderLayer layer=en.getKey();BufferBuilder bb=en.getValue();
            BufferBuilder.BuiltBuffer built=bb.endNullable();
            if(built==null)continue;
            // a glint scrolls with time and an outline needs its own pass: such a body is not still
            String name=layerName(layer);if(name.contains("glint")||name.contains("outline")){built.release();ok=false;lastChange="refused layer "+name;continue;}
            VertexBuffer vb=new VertexBuffer(VertexBuffer.Usage.STATIC);vb.bind();vb.upload(built);VertexBuffer.unbind();
            b.parts.add(new Part(layer,vb));
        }
        RECORDER.release();BufferRenderer.reset();
        return ok;
    }

    /** A layer's own name: RenderLayer.toString is "RenderType[name:phases]", and its phases name the outline mode of every entity layer. */
    static String layerName(RenderLayer layer){String s=layer.toString();int i=s.indexOf('['),j=s.indexOf(':');return i>=0&&j>i?s.substring(i+1,j):s;}
    /** One BufferBuilder a layer, kept between records. */
    private static final class Recorder implements VertexConsumerProvider {
        final Map<RenderLayer,BufferBuilder> open=new LinkedHashMap<>();final ArrayDeque<BufferBuilder> pool=new ArrayDeque<>();
        @Override public VertexConsumer getBuffer(RenderLayer layer){
            BufferBuilder bb=open.get(layer);
            if(bb==null){bb=pool.isEmpty()?new BufferBuilder(Math.max(layer.getExpectedBufferSize(),1<<16)):pool.pop();bb.begin(layer.getDrawMode(),layer.getVertexFormat());open.put(layer,bb);}
            return bb;
        }
        void release(){pool.addAll(open.values());open.clear();}
        void abandon(){for(BufferBuilder bb:open.values()){if(bb.isBuilding()){var built=bb.endNullable();if(built!=null)built.release();}}release();}
    }
    private static final Recorder RECORDER=new Recorder();

    private static final Matrix4f VIEW=new Matrix4f(),MV=new Matrix4f();private static final Matrix3f IDENTITY3=new Matrix3f();
    private static final Vector3f L0=new Vector3f(),L1=new Vector3f(),B0=new Vector3f(),B1=new Vector3f();private static final Matrix3f TURN=new Matrix3f();

    /**
     * WorldRenderEvents.AFTER_ENTITIES: draws the queued bodies, one layer at a time: the layer's state and program once, then
     * per body its buffer with the model-view matrix that places it (the camera's rotation, then where it lies from the
     * camera, then its body yaw). Its vertices' normals are in its recorded axes, so the lights are put into those axes.
     */
    static void flush(WorldRenderContext ctx){
        inWorld=false;
        if(QUEUE.isEmpty()){sweep();return;}
        long t0=System.nanoTime();flushes++;flushBodies+=QUEUE.size();
        Vec3d cam=ctx.camera().getPos();
        shadows(ctx,cam);
        VIEW.set(RenderSystem.getModelViewMatrix()).mul(ctx.matrixStack().peek().getPositionMatrix());
        Matrix3f inv=RenderSystem.getInverseViewRotationMatrix();Vector3f[] lights=RenderSystemAccessor.gravesandglory$lights();
        inv.transform(lights[0],L0);inv.transform(lights[1],L1);
        for(List<Body> l:BY_LAYER.values())l.clear();
        for(List<VertexBuffer> l:BUFFERS.values())l.clear();
        for(Body b:QUEUE)for(Part p:b.parts){BY_LAYER.computeIfAbsent(p.layer(),k->new ArrayList<>()).add(b);BUFFERS.computeIfAbsent(p.layer(),k->new ArrayList<>()).add(p.buffer());}
        for(var en:BY_LAYER.entrySet()){
            List<Body> bodies=en.getValue();if(bodies.isEmpty())continue;
            RenderLayer layer=en.getKey();List<VertexBuffer> bufs=BUFFERS.get(layer);
            layer.startDrawing();
            ShaderProgram sh=RenderSystem.getShader();
            if(sh!=null){
                for(int i=0;i<12;i++)sh.addSampler("Sampler"+i,RenderSystem.getShaderTexture(i));
                set(sh.projectionMat,RenderSystem.getProjectionMatrix());
                if(sh.viewRotationMat!=null)sh.viewRotationMat.set(IDENTITY3);
                if(sh.colorModulator!=null)sh.colorModulator.set(RenderSystem.getShaderColor());
                if(sh.glintAlpha!=null)sh.glintAlpha.set(RenderSystem.getShaderGlintAlpha());
                if(sh.fogStart!=null)sh.fogStart.set(RenderSystem.getShaderFogStart());
                if(sh.fogEnd!=null)sh.fogEnd.set(RenderSystem.getShaderFogEnd());
                if(sh.fogColor!=null)sh.fogColor.set(RenderSystem.getShaderFogColor());
                if(sh.fogShape!=null)sh.fogShape.set(RenderSystem.getShaderFogShape().getId());
                set(sh.textureMat,RenderSystem.getTextureMatrix());
                if(sh.gameTime!=null)sh.gameTime.set(RenderSystem.getShaderGameTime());
                var w=MinecraftClient.getInstance().getWindow();if(sh.screenSize!=null)sh.screenSize.set((float)w.getFramebufferWidth(),(float)w.getFramebufferHeight());
                RenderSystem.setupShaderLights(sh);
                if(sh.light0Direction!=null)sh.light0Direction.set(L0);
                if(sh.light1Direction!=null)sh.light1Direction.set(L1);
                set(sh.modelViewMat,VIEW);
                sh.bind();
                for(int i=0;i<bodies.size();i++){
                    Body b=bodies.get(i);VertexBuffer vb=bufs.get(i);
                    float turn=(float)Math.toRadians(180f-b.yaw);
                    if(sh.modelViewMat!=null){MV.set(VIEW).translate((float)(b.x-cam.x),(float)(b.y-cam.y),(float)(b.z-cam.z)).rotateY(turn);sh.modelViewMat.set(MV);sh.modelViewMat.upload();}
                    TURN.rotationY(-turn); // the lights into the body's recorded axes
                    if(sh.light0Direction!=null){sh.light0Direction.set(TURN.transform(L0,B0));sh.light0Direction.upload();}
                    if(sh.light1Direction!=null){sh.light1Direction.set(TURN.transform(L1,B1));sh.light1Direction.upload();}
                    vb.bind();vb.draw();flushDraws++;
                }
                sh.unbind();
            }
            VertexBuffer.unbind();
            layer.endDrawing();
        }
        BufferRenderer.reset();
        QUEUE.clear();sweep();flushNanos+=System.nanoTime()-t0;
    }
    private static void set(GlUniform u,Matrix4f m){if(u!=null)u.set(m);}

    /**
     * The shadows of the bodies drawn on the short check, as EntityRenderDispatcher.render draws them (its switch and the
     * option, the renderer's radius and opacity, faded by the squared distance over 256) into the frame's entity consumers,
     * which draw them with the rest of the frame's shadows.
     */
    private static void shadows(WorldRenderContext ctx,Vec3d cam){
        MinecraftClient mc=MinecraftClient.getInstance();var d=mc.getEntityRenderDispatcher();
        if(SHADOWS.isEmpty()||mc.world==null||!mc.options.getEntityShadows().getValue()||!((EntityRenderDispatcherAccessor)d).gravesandglory$renderShadows())return;
        float td=ctx.tickDelta();MatrixStack ms=ctx.matrixStack();VertexConsumerProvider vc=mc.getBufferBuilders().getEntityVertexConsumers();
        for(Body b:SHADOWS){
            Entity e=b.entity;float radius=b.renderer.shadowRadius(),opacity=b.renderer.shadowOpacity();if(radius<=0)continue;
            float o=(float)((1.0-cam.squaredDistanceTo(e.getX(),e.getY(),e.getZ())/256.0)*opacity);if(o<=0)continue;
            ms.push();
            ms.translate(MathHelper.lerp(td,e.lastRenderX,e.getX())-cam.x,MathHelper.lerp(td,e.lastRenderY,e.getY())-cam.y,MathHelper.lerp(td,e.lastRenderZ,e.getZ())-cam.z);
            EntityRenderDispatcherAccessor.gravesandglory$renderShadow(ms,vc,e,o,td,mc.world,Math.min(radius,32f));
            ms.pop();
        }
    }

    /** Frees the buffers of bodies gone from the world, or out of view for IDLE_FRAMES. */
    private static void sweep(){
        var world=MinecraftClient.getInstance().world;
        for(Iterator<Map.Entry<Entity,Body>> it=BODIES.entrySet().iterator();it.hasNext();){
            var en=it.next();Entity e=en.getKey();Body b=en.getValue();
            if(e.isRemoved()||e.getWorld()!=world||frame-b.lastFrame>IDLE_FRAMES||!TroopAnimator.lies(e)){b.free();it.remove();}
        }
    }
    /** Frees every buffer (a world left, resources reloaded). */
    static void clear(){for(Body b:BODIES.values())b.free();BODIES.clear();QUEUE.clear();}
    /** Whether this client keeps buffers for the entity's body and draws it from them. */
    static boolean cached(Entity e){Body b=BODIES.get(e);return b!=null&&!b.normal&&!b.parts.isEmpty();}
    /** The bodies this client keeps buffers for (the harness reads it). */
    static int kept(){int n=0;for(Body b:BODIES.values())if(!b.parts.isEmpty())n++;return n;}
}
