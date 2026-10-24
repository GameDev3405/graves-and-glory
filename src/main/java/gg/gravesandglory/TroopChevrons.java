package gg.gravesandglory;

import com.google.common.collect.MapMaker;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import java.util.Map;

/**
 * The rank chevrons (in-match progression design 7): one chevron over a level-2 soldier and two over a level-3 one, for both
 * teams, a small billboard above the head in the side's colour. TroopRenderer calls draw after it drew a troop, through its own
 * model or through the vanilla renderer of a troop whose House has no model yet. The level is the rank_v1 one (RankSync.client),
 * so a soldier draws no chevron until his rank arrives. Drawn only within progression.chevronRange of the camera and never for
 * a troop without a side (a portrait has none); the troop-depth Visuals switch (TroopDepth.enabled) does not touch them, since
 * they are information, not depth. The texture is textures/entity/troop/chevron.png (tools/build_skins.py, from
 * art/marks/chevron.png): grey, multiplied by the side colour, full bright, depth-tested as a name tag is.
 */
final class TroopChevrons {
    private TroopChevrons() {}
    static final Identifier TEXTURE=new Identifier(TroopModels.NAMESPACE,"textures/entity/troop/chevron.png");
    /** Presentation constants (the plan's only code constants besides effect kinds): the texture's size in texels, the chevron's width in blocks, its lift over the head in blocks, how many texel rows the second chevron sits above the first, and the camera distance in blocks past which the chevron grows with the distance (so it keeps the size it has there on screen, and still reads at the 24-block range). */
    static final int TEX_W=17,TEX_H=7,STACK=4;
    static final float WIDTH=0.55f,LIFT=0.2f,GROW_FROM=16f;
    static final int FULL_BRIGHT=0xF000F0;
    /** One chevron draw as observed (the harness reads it, through drawn): the chevrons drawn, the colour they were tinted (0xRRGGBB), the camera distance in blocks and the world time of the draw. */
    record Mark(int chevrons,int colour,double distance,long time){}
    /** The last draw of each entity, overwritten in place (no allocation a frame after an entity's first chevron). */
    private static final class Last{int chevrons,colour;double distance;long time;}
    private static final Map<Entity,Last> DRAWN=new MapMaker().weakKeys().makeMap();
    /** Chevron billboards drawn since the client started (one per soldier per draw, whatever its count), and the nanoseconds every call of draw took, drawn or not (the harness reads them: the draw cost). */
    static volatile long draws,nanos;

    /** The chevrons a level shows: none at level 1, one at 2, two at 3. */
    static int count(int level){return level>=3?2:level==2?1:0;}
    /** progression.chevronRange, in blocks. */
    static double range(){return Spec.PROGRESSION[0].chevronRange();}
    /** The last chevron draw of an entity (a copy), or null for one that never drew a chevron. */
    static Mark drawn(Entity e){var k=DRAWN.get(e);return k==null?null:new Mark(k.chevrons,k.colour,k.distance,k.time);}
    /** The colour of a side (a factions id), or -1 for none. */
    static int colour(String side){if(side!=null)for(var f:Spec.FACTIONS)if(f.id().equals(side))return f.color();return -1;}

    /**
     * Draws an entity's chevrons at its feet's matrix (as TroopRenderer.render leaves it), from the look TroopRenderer already
     * resolved (its rank level and side) and its model; a troop drawn by its vanilla renderer (no model, so no look) reads its
     * rank and side here. Nothing below level 2, beyond the range, for a dead, invisible or side-less troop, or a non-troop.
     */
    static void draw(Entity e,TroopModels.Look l,TroopModel<?> model,MatrixStack m,VertexConsumerProvider v,EntityRenderDispatcher d){
        long start=System.nanoTime();
        try{billboard(e,l,model,m,v,d);}finally{nanos+=System.nanoTime()-start;}
    }
    private static void billboard(Entity e,TroopModels.Look l,TroopModel<?> model,MatrixStack m,VertexConsumerProvider v,EntityRenderDispatcher d){
        if(e.isInvisible()||e instanceof LivingEntity le&&(le.isDead()||le.deathTime>0))return;
        int level;String side;
        if(l!=null){level=l.level();side=l.side();}
        else{if(TroopModels.troopOf(e)==null)return;RankSync.Rank r=RankSync.client(e);level=r==null?1:r.level();side=null;if(count(level)>0)side=TroopModels.side(e);}
        int n=count(level);if(n==0)return;
        double dist=Math.sqrt(d.getSquaredDistanceToCamera(e));if(dist>range())return;
        int c=colour(side);if(c<0)return;
        float red=(c>>16&255)/255f,green=(c>>8&255)/255f,blue=(c&255)/255f;
        m.push();
        m.translate(0,top(e,model)+LIFT,0);m.multiply(d.getRotation());float k=WIDTH/TEX_W*(float)Math.max(1,dist/GROW_FROM);m.scale(-k,-k,k); // as a name tag: facing the camera, x to its right, y down, one unit a texel
        Matrix4f mat=m.peek().getPositionMatrix();VertexConsumer vc=v.getBuffer(RenderLayer.getText(TEXTURE));
        float x0=-TEX_W/2f,x1=TEX_W/2f;
        for(int i=0;i<n;i++){float y1=-i*STACK,y0=y1-TEX_H; // the first chevron's bottom edge on the lift point, the second STACK rows above it
            vc.vertex(mat,x0,y0,0).color(red,green,blue,1f).texture(0,0).light(FULL_BRIGHT).next();
            vc.vertex(mat,x0,y1,0).color(red,green,blue,1f).texture(0,1).light(FULL_BRIGHT).next();
            vc.vertex(mat,x1,y1,0).color(red,green,blue,1f).texture(1,1).light(FULL_BRIGHT).next();
            vc.vertex(mat,x1,y0,0).color(red,green,blue,1f).texture(1,0).light(FULL_BRIGHT).next();}
        m.pop();
        Last last=DRAWN.get(e);if(last==null)DRAWN.put(e,last=new Last());
        last.chevrons=n;last.colour=c;last.distance=dist;last.time=e.getWorld()==null?0:e.getWorld().getTime();draws++;
    }
    /** The top of a troop's head over its feet, in blocks: its model's crown (the highest helm or crest of any tier, no flag) when it has a model, else its vanilla box. One height for every tier, so the chevrons never move and no tier's crest reaches them. */
    static double top(Entity e,TroopModel<?> model){
        if(model==null||Double.isNaN(model.crown()))return e.getHeight();
        return 1.5-model.crown()/16.0;
    }
}
