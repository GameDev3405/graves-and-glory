package gg.gravesandglory;

import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL14;
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
 * (TroopMotion.settled) no longer changes, so its draw is recorded once, in its own frame (world axes about its feet, as
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
    static volatile boolean off;static volatile int hide=-1,hide2=-1;
    /** Harness: whether the renderer skips the entity (BodyCache.hide or hide2); and noGroups puts every fading body in one group, as before carry D14 (the red side of mo-fade-overlap). */
    static boolean hidden(int id){return id==hide||id==hide2;}static volatile boolean noGroups;
    /** Harness switch (hl-corpse, R42): draw a fading body opaque, its fade ignored (the reference frame its alpha is measured against). */
    static volatile boolean opaque;
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
        Key key;final List<Part> parts=new ArrayList<>();int records;boolean normal;int lastFrame,verified=Integer.MIN_VALUE/2;double x,y,z;float yaw,shade=1f;Entity entity;TroopRenderer<?> renderer;net.minecraft.util.math.Box cull;
        void free(){for(Part p:parts)p.buffer.close();parts.clear();}
    }
    /**
     * A body drawn this frame part way through its corpse fade (R42): a cached body's own buffers, or the buffers one normal
     * draw was caught into (body null), drawn translucent at alpha by fadePass.
     */
    static final class Fade { final Body body;final List<Part> parts;final float alpha;double d2,x,y,z,reach;int group;Fade(Body body,List<Part> parts,float alpha){this.body=body;this.parts=parts;this.alpha=alpha;} }
    record FadeDraw(Fade fade,VertexBuffer buffer){}
    private static final Map<Entity,Body> BODIES=new IdentityHashMap<>();
    private static final List<Body> QUEUE=new ArrayList<>(),SHADOWS=new ArrayList<>();
    private static final Map<RenderLayer,List<Body>> BY_LAYER=new LinkedHashMap<>();
    private static final Map<RenderLayer,List<VertexBuffer>> BUFFERS=new LinkedHashMap<>();
    private static final List<Fade> FADES=new ArrayList<>();private static final Map<RenderLayer,List<FadeDraw>> FADE_BY_LAYER=new LinkedHashMap<>();
    private static final Map<RenderLayer,Integer> KINDS=new IdentityHashMap<>();private static final ArrayDeque<VertexBuffer> SPARE=new ArrayDeque<>();
    private static boolean inWorld;private static int frame;
    /** Fade passes, the fading bodies and draws they drew, and their time (the harness reads them). */
    static long fadePasses,fadeBodies,fadeDraws,fadeNanos,fadeGroups,fadeGroupsMax,fadeLayered,fadeTranslucentMax;
    /** Draws from cached buffers and records since the client started (the harness reads them). */
    static long cachedDraws,records;
    /** Diagnostics for the harness: cached draws on the short check, draws refused outside the world pass, key changes, give-ups, failed records, and the last key change or refusal. */
    static long quick,outside,changes,giveUps,failed,flushNanos,flushes,flushDraws,flushBodies;static String lastChange="";
    static String diag(){return "cached draws "+cachedDraws+" ("+quick+" on the short check), records "+records+", outside the world pass "+outside+", key changes "+changes+", give-ups "+giveUps+", failed records "+failed+", kept "+kept()+", flush "+String.format(java.util.Locale.ROOT,"%.1f us for %.1f bodies and %.1f draws a frame",flushNanos/1e3/Math.max(1,flushes),flushBodies/(double)Math.max(1,flushes),flushDraws/(double)Math.max(1,flushes))
        +String.format(java.util.Locale.ROOT,", fade %d passes, %.1f us for %.1f bodies and %.1f draws a pass, %.2f overlap groups a pass (most %d), %d fading bodies with 2 or more blended layers (most blended layers in one body %d)",fadePasses,fadeNanos/1e3/Math.max(1,fadePasses),fadeBodies/(double)Math.max(1,fadePasses),fadeDraws/(double)Math.max(1,fadePasses),fadeGroups/(double)Math.max(1,fadePasses),fadeGroupsMax,fadeLayered,fadeTranslucentMax)+"; last: "+lastChange;}

    /**
     * How opaque the entity's body draws now (R42): whole until its corpse fade begins (death tick 20 + the sheet's
     * corpseLieTicks), then BattleMath.corpseAlpha's eased fall to 0 on its removal tick, smooth on the partial tick. Its shadow
     * fades with it.
     */
    static float fade(Entity e,float tickDelta){
        if(!(e instanceof net.minecraft.entity.LivingEntity l)||l.deathTime<=0)return 1f;
        var row=Spec.COMBATDISCIPLINE[0];return BattleMath.corpseAlpha(l.deathTime+tickDelta,row.corpseLieTicks(),row.corpseFadeTicks());
    }
    /** The opacity the body draws at: its fade, or whole while the harness takes its opaque reference frame. */
    static float alpha(Entity e,float tickDelta){return opaque?1f:fade(e,tickDelta);}

    /** WorldRenderEvents.START: the world pass begins; bodies may queue. */
    static void begin(){inWorld=ENABLED;frame++;QUEUE.clear();SHADOWS.clear();recycle();}
    /** Queues a cached body at its opacity: whole with the rest, part way to the fade pass, gone at 0. */
    private static void queue(Body b,float a){if(a>=1f)QUEUE.add(b);else if(a>0f){Fade f=new Fade(b,b.parts,a);f.reach=reach(b.entity);FADES.add(f);}}
    /** How far a body can reach from its feet, lying any way: its height laid flat plus its width (the fade pass's overlap test, conservative). */
    private static double reach(Entity e){return e==null?3:e.getHeight()+e.getWidth();}

    /** A normal draw part way through its fade is caught here (null outside the world pass: it draws whole); faded or abandonFading ends it. */
    static VertexConsumerProvider fading(VertexConsumerProvider consumers){
        if(!inWorld)return null;
        LOOSE.outline=consumers instanceof OutlineVertexConsumerProvider o?o:null;LOOSE.rec.open.clear();return LOOSE;
    }
    /** Ends a caught draw: each layer's vertices into a buffer (kept between frames), drawn by fadePass at the alpha. */
    static void faded(Entity e,float alpha){
        Fade f=new Fade(null,new ArrayList<>(),alpha);
        for(var en:LOOSE.rec.open.entrySet()){
            BufferBuilder.BuiltBuffer built=en.getValue().endNullable();if(built==null)continue;
            VertexBuffer vb=SPARE.isEmpty()?new VertexBuffer(VertexBuffer.Usage.DYNAMIC):SPARE.pop();vb.bind();vb.upload(built);f.parts.add(new Part(en.getKey(),vb));
        }
        VertexBuffer.unbind();LOOSE.rec.release();LOOSE.outline=null;BufferRenderer.reset();
        if(!f.parts.isEmpty()){f.d2=e.squaredDistanceTo(MinecraftClient.getInstance().gameRenderer.getCamera().getPos());f.x=e.getX();f.y=e.getY();f.z=e.getZ();f.reach=reach(e);FADES.add(f);}
    }
    static void abandonFading(){LOOSE.rec.abandon();LOOSE.outline=null;}
    /** The caught draws' buffers back to the spare pool. */
    private static void recycle(){for(Fade f:FADES)if(f.body==null)for(Part p:f.parts)SPARE.push(p.buffer());FADES.clear();}
    /** Catches a normal draw; a layer with an outline also feeds the frame's outline, as OutlineVertexConsumerProvider does. */
    private static final class Loose implements VertexConsumerProvider {
        final Recorder rec=new Recorder();OutlineVertexConsumerProvider outline;
        @Override public VertexConsumer getBuffer(RenderLayer layer){
            VertexConsumer vc=rec.getBuffer(layer);
            if(outline!=null&&layer.getAffectedOutline().isPresent())return VertexConsumers.union(outline.getBuffer(layer.getAffectedOutline().get()),vc);
            return vc;
        }
    }
    private static final Loose LOOSE=new Loose();

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
        b.x=key.x;b.y=key.y;b.z=key.z;b.yaw=body;b.lastFrame=frame;queue(b,alpha(e,tickDelta));cachedDraws++;
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
     * The short check's draw, past the dispatcher: queued at its opacity, with the shadow the dispatcher would have drawn, when it
     * lies where it was recorded (turned to its body yaw now) in the light it was recorded in (R45: the light is read every
     * frame, so a torch or a lightning flash reaches the body on the next frame); false sends it to the full check.
     */
    static boolean drawFresh(Body b,double x,double y,double z,float body,int light,float tickDelta){
        Key k=b.key;if(light!=k.light()||Math.abs(k.x()-x)>=1e-5||Math.abs(k.y()-y)>=1e-5||Math.abs(k.z()-z)>=1e-5)return false;
        b.yaw=body;b.lastFrame=frame;queue(b,alpha(b.entity,tickDelta));b.shade=fade(b.entity,tickDelta);SHADOWS.add(b);cachedDraws++;quick++;return true;
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

    private static final Matrix4f VIEW=new Matrix4f(),MV=new Matrix4f(),ENTITY_MV=new Matrix4f(),PROJ=new Matrix4f();private static final Matrix3f IDENTITY3=new Matrix3f(),INV_VIEW=new Matrix3f();
    private static final Vector3f L0=new Vector3f(),L1=new Vector3f(),V0=new Vector3f(),V1=new Vector3f(),B0=new Vector3f(),B1=new Vector3f();private static final Matrix3f TURN=new Matrix3f();
    private static Vec3d cam=Vec3d.ZERO;

    /**
     * WorldRenderEvents.AFTER_ENTITIES: draws the queued bodies, one layer at a time: the layer's state and program once, then
     * per body its buffer with the model-view matrix that places it (the camera's rotation, then where it lies from the
     * camera, then its body yaw). Its vertices' normals are in its recorded axes, so the lights are put into those axes.
     * It keeps the frame's entity matrices and lights for fadePass.
     */
    static void flush(WorldRenderContext ctx){
        inWorld=false;
        cam=ctx.camera().getPos();
        VIEW.set(RenderSystem.getModelViewMatrix()).mul(ctx.matrixStack().peek().getPositionMatrix());
        ENTITY_MV.set(RenderSystem.getModelViewMatrix());PROJ.set(RenderSystem.getProjectionMatrix());INV_VIEW.set(RenderSystem.getInverseViewRotationMatrix());
        Vector3f[] lights=RenderSystemAccessor.gravesandglory$lights();V0.set(lights[0]);V1.set(lights[1]);
        INV_VIEW.transform(V0,L0);INV_VIEW.transform(V1,L1);
        if(QUEUE.isEmpty()&&SHADOWS.isEmpty()){sweep();return;}
        long t0=System.nanoTime();flushes++;flushBodies+=QUEUE.size();
        shadows(ctx,cam);
        for(List<Body> l:BY_LAYER.values())l.clear();
        for(List<VertexBuffer> l:BUFFERS.values())l.clear();
        for(Body b:QUEUE)for(Part p:b.parts){BY_LAYER.computeIfAbsent(p.layer(),k->new ArrayList<>()).add(b);BUFFERS.computeIfAbsent(p.layer(),k->new ArrayList<>()).add(p.buffer());}
        for(var en:BY_LAYER.entrySet()){
            List<Body> bodies=en.getValue();if(bodies.isEmpty())continue;
            RenderLayer layer=en.getKey();List<VertexBuffer> bufs=BUFFERS.get(layer);
            ShaderProgram sh=open(layer,RenderSystem.getShaderColor());
            if(sh!=null){
                for(int i=0;i<bodies.size();i++){place(sh,bodies.get(i));VertexBuffer vb=bufs.get(i);vb.bind();vb.draw();flushDraws++;}
                sh.unbind();
            }
            VertexBuffer.unbind();
            layer.endDrawing();
        }
        BufferRenderer.reset();
        QUEUE.clear();sweep();flushNanos+=System.nanoTime()-t0;
    }
    /** Starts the layer's state and binds its program with the frame's uniforms, the colour modulator given (null when the layer has no program). */
    private static ShaderProgram open(RenderLayer layer,float[] color){
        layer.startDrawing();
        ShaderProgram sh=RenderSystem.getShader();if(sh==null)return null;
        for(int i=0;i<12;i++)sh.addSampler("Sampler"+i,RenderSystem.getShaderTexture(i));
        set(sh.projectionMat,PROJ);
        if(sh.viewRotationMat!=null)sh.viewRotationMat.set(IDENTITY3);
        if(sh.colorModulator!=null)sh.colorModulator.set(color);
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
        return sh;
    }
    /** Uploads the model-view matrix that places a cached body and the lights turned into its recorded axes. */
    private static void place(ShaderProgram sh,Body b){
        float turn=(float)Math.toRadians(180f-b.yaw);
        if(sh.modelViewMat!=null){MV.set(VIEW).translate((float)(b.x-cam.x),(float)(b.y-cam.y),(float)(b.z-cam.z)).rotateY(turn);sh.modelViewMat.set(MV);sh.modelViewMat.upload();}
        TURN.rotationY(-turn); // the lights into the body's recorded axes
        if(sh.light0Direction!=null){sh.light0Direction.set(TURN.transform(L0,B0));sh.light0Direction.upload();}
        if(sh.light1Direction!=null){sh.light1Direction.set(TURN.transform(L1,B1));sh.light1Direction.upload();}
    }
    private static void set(GlUniform u,Matrix4f m){if(u!=null)u.set(m);}

    /** How a layer blends, from its transparency phase: 0 opaque (no blend), 2 adding light (eyes, glint), 1 any other blend. */
    private static int kind(RenderLayer layer){
        return KINDS.computeIfAbsent(layer,k->{String s=k.toString();
            return s.contains("no_transparency")?0:s.contains("additive_transparency")||s.contains("glint_transparency")||s.contains("lightning_transparency")?2:1;});
    }
    /**
     * WorldRenderEvents.BEFORE_DEBUG_RENDER, after every entity layer of the frame is drawn: the bodies part way through their
     * fade (R42), cached and caught alike, translucent. First their opaque layers write only depth, so each body shows its front
     * surface only; then every layer draws in colour against that depth, farthest body first: an opaque layer blended at the
     * body's alpha by a constant blend colour, a blending layer with its colour modulator's alpha scaled, an adding one
     * (eyes, glint) with its whole modulator scaled. Bodies whose screen discs overlap go in successive groups, farthest first
     * (carry D14): each group's depth and colour passes finish before the next group's depth pass, so a nearer fading body blends
     * over a farther one instead of its depth hiding it; bodies that overlap none share group 0 and draw batched by layer.
     */
    static void fadePass(WorldRenderContext ctx){
        if(FADES.isEmpty())return;
        long t0=System.nanoTime();fadePasses++;fadeBodies+=FADES.size();
        for(Fade f:FADES)if(f.body!=null){Body b=f.body;f.x=b.x;f.y=b.y;f.z=b.z;double dx=b.x-cam.x,dy=b.y-cam.y,dz=b.z-cam.z;f.d2=dx*dx+dy*dy+dz*dz;}
        FADES.sort((p,q)->Double.compare(q.d2,p.d2));
        int groups=group(FADES);fadeGroups+=groups;fadeGroupsMax=Math.max(fadeGroupsMax,groups);
        for(Fade f:FADES){int n=0;for(Part p:f.parts)if(kind(p.layer())==1)n++;if(n>=2)fadeLayered++;fadeTranslucentMax=Math.max(fadeTranslucentMax,n);}
        try{
            for(int g=0;g<groups;g++){
                for(List<FadeDraw> l:FADE_BY_LAYER.values())l.clear();
                for(Fade f:FADES)if(f.group==g)for(Part p:f.parts)FADE_BY_LAYER.computeIfAbsent(p.layer(),k->new ArrayList<>()).add(new FadeDraw(f,p.buffer()));
                for(var en:FADE_BY_LAYER.entrySet())if(!en.getValue().isEmpty()&&kind(en.getKey())==0)drawFades(en.getKey(),en.getValue(),-1);
                for(int k=0;k<3;k++)for(var en:FADE_BY_LAYER.entrySet())if(!en.getValue().isEmpty()&&kind(en.getKey())==k)drawFades(en.getKey(),en.getValue(),k);
            }
        }finally{
            RenderSystem.colorMask(true,true,true,true);RenderSystem.depthMask(true);RenderSystem.disableBlend();RenderSystem.defaultBlendFunc();GL14.glBlendColor(0f,0f,0f,0f);
            BufferRenderer.reset();recycle();fadeNanos+=System.nanoTime()-t0;
        }
    }
    /**
     * Puts the fading bodies (sorted farthest first) in groups for the fade pass (carry D14): a body goes one group past the
     * latest group of any farther body whose screen disc overlaps its own (the angle between their centres from the camera under
     * the sum of their angular reaches), so overlapping bodies draw farther first; returns the number of groups.
     */
    static int group(List<Fade> fades){
        if(noGroups){for(Fade f:fades)f.group=0;return fades.isEmpty()?0:1;}
        int n=fades.size(),groups=0;double[] ux=new double[n],uy=new double[n],uz=new double[n],th=new double[n];
        for(int i=0;i<n;i++){
            Fade f=fades.get(i);double dx=f.x-cam.x,dy=f.y-cam.y,dz=f.z-cam.z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(d<1e-6){ux[i]=0;uy[i]=0;uz[i]=1;th[i]=Math.PI;}else{ux[i]=dx/d;uy[i]=dy/d;uz[i]=dz/d;th[i]=f.reach>=d?Math.PI:Math.asin(f.reach/d);}
            int g=0;
            for(int j=0;j<i;j++){
                Fade o=fades.get(j);if(o.group+1<=g)continue;
                double dot=Math.max(-1,Math.min(1,ux[i]*ux[j]+uy[i]*uy[j]+uz[i]*uz[j]));
                if(Math.acos(dot)<th[i]+th[j])g=o.group+1;
            }
            f.group=g;groups=Math.max(groups,g+1);
        }
        return groups;
    }
    private static final float[] COLOR=new float[4];
    /** One layer of the fading bodies: kind -1 the depth pass, else the colour pass for a layer of that kind. */
    private static void drawFades(RenderLayer layer,List<FadeDraw> draws,int kind){
        float[] base=RenderSystem.getShaderColor();
        ShaderProgram sh=open(layer,base);
        if(sh!=null){
            if(kind<0){RenderSystem.colorMask(false,false,false,false);RenderSystem.depthMask(true);}
            else{RenderSystem.colorMask(true,true,true,true);RenderSystem.depthMask(false);
                if(kind==0){RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.CONSTANT_ALPHA,GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA,GlStateManager.SrcFactor.ZERO,GlStateManager.DstFactor.ONE);}}
            for(FadeDraw d:draws){
                Fade f=d.fade();
                if(f.body!=null){place(sh,f.body);if(sh.viewRotationMat!=null){sh.viewRotationMat.set(IDENTITY3);sh.viewRotationMat.upload();}}
                else{ // caught from a normal draw: its vertices are in the camera's axes, as the frame's entity draws
                    if(sh.modelViewMat!=null){sh.modelViewMat.set(ENTITY_MV);sh.modelViewMat.upload();}
                    if(sh.viewRotationMat!=null){sh.viewRotationMat.set(INV_VIEW);sh.viewRotationMat.upload();}
                    if(sh.light0Direction!=null){sh.light0Direction.set(V0);sh.light0Direction.upload();}
                    if(sh.light1Direction!=null){sh.light1Direction.set(V1);sh.light1Direction.upload();}
                }
                if(kind==0)GL14.glBlendColor(0f,0f,0f,f.alpha);
                else if(kind>0&&sh.colorModulator!=null){
                    float a=f.alpha;if(kind==2){COLOR[0]=base[0]*a;COLOR[1]=base[1]*a;COLOR[2]=base[2]*a;}else{COLOR[0]=base[0];COLOR[1]=base[1];COLOR[2]=base[2];}COLOR[3]=base[3]*a;
                    sh.colorModulator.set(COLOR);sh.colorModulator.upload();
                }
                d.buffer().bind();d.buffer().draw();fadeDraws++;
            }
            sh.unbind();
        }
        VertexBuffer.unbind();
        layer.endDrawing();
    }

    /**
     * The shadows of the bodies drawn on the short check, as EntityRenderDispatcher.render draws them (its switch and the
     * option, the renderer's radius and opacity, faded by the squared distance over 256) into the frame's entity consumers,
     * and drawn there and then. The shadow layer has no buffer of its own, so left queued it would draw only when the world
     * pass flushes the shared buffer after the debug renderer, after fadePass: a fading body's depth would then hide its own
     * shadow and those of the bodies behind it, where the dispatcher's shadows show through (mo-fade-overlap's F0 caught it).
     */
    private static void shadows(WorldRenderContext ctx,Vec3d cam){
        MinecraftClient mc=MinecraftClient.getInstance();var d=mc.getEntityRenderDispatcher();
        if(SHADOWS.isEmpty()||mc.world==null||!mc.options.getEntityShadows().getValue()||!((EntityRenderDispatcherAccessor)d).gravesandglory$renderShadows())return;
        float td=ctx.tickDelta();MatrixStack ms=ctx.matrixStack();var vc=mc.getBufferBuilders().getEntityVertexConsumers();
        for(Body b:SHADOWS){
            Entity e=b.entity;float radius=b.renderer.shadowRadius(),opacity=b.renderer.shadowOpacity()*b.shade;if(radius<=0)continue;
            float o=(float)((1.0-cam.squaredDistanceTo(e.getX(),e.getY(),e.getZ())/256.0)*opacity);if(o<=0)continue;
            ms.push();
            ms.translate(MathHelper.lerp(td,e.lastRenderX,e.getX())-cam.x,MathHelper.lerp(td,e.lastRenderY,e.getY())-cam.y,MathHelper.lerp(td,e.lastRenderZ,e.getZ())-cam.z);
            EntityRenderDispatcherAccessor.gravesandglory$renderShadow(ms,vc,e,o,td,mc.world,Math.min(radius,32f));
            ms.pop();
        }
        vc.drawCurrentLayer(); // drawn now, as the dispatcher's shadows are: left in the shared buffer they would draw after fadePass, under the fading bodies' depth
    }

    /** Frees the buffers of bodies gone from the world, or out of view for IDLE_FRAMES. */
    private static void sweep(){
        var world=MinecraftClient.getInstance().world;
        for(Iterator<Map.Entry<Entity,Body>> it=BODIES.entrySet().iterator();it.hasNext();){
            var en=it.next();Entity e=en.getKey();Body b=en.getValue();
            if(e.isRemoved()||e.getWorld()!=world||frame-b.lastFrame>IDLE_FRAMES||!TroopMotion.lies(e)){b.free();it.remove();}
        }
    }
    /** Frees every buffer (a world left, resources reloaded). */
    static void clear(){for(Body b:BODIES.values())b.free();BODIES.clear();QUEUE.clear();recycle();for(VertexBuffer vb:SPARE)vb.close();SPARE.clear();}
    /** Whether this client keeps buffers for the entity's body and draws it from them. */
    static boolean cached(Entity e){Body b=BODIES.get(e);return b!=null&&!b.normal&&!b.parts.isEmpty();}
    /** Harness (Task 13 carry D21, R45): the light the entity's body drew from its buffers with this frame, -1 when it did not draw from them this frame. */
    static int drewLight(Entity e){Body b=BODIES.get(e);return b!=null&&!b.normal&&b.key!=null&&b.lastFrame==frame?b.key.light():-1;}
    /** The bodies this client keeps buffers for (the harness reads it). */
    static int kept(){int n=0;for(Body b:BODIES.values())if(!b.parts.isEmpty())n++;return n;}
}
