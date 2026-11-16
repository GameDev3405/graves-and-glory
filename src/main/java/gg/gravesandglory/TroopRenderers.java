package gg.gravesandglory;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import java.util.function.Function;

/** One TroopRenderer per sheet entity type (TroopModels.RIGS), each over that type's vanilla renderer and its vanilla shadow radius. */
public final class TroopRenderers {
    private TroopRenderers() {}
    public static void register(){
        over(EntityType.HUSK,HuskEntityRenderer::new,.5f);
        over(EntityType.ZOMBIE,ZombieEntityRenderer::new,.5f);
        over(EntityType.SKELETON,SkeletonEntityRenderer::new,.5f);
        over(EntityType.STRAY,StrayEntityRenderer::new,.5f);
        over(EntityType.WITHER_SKELETON,WitherSkeletonEntityRenderer::new,.5f);
        over(EntityType.DROWNED,DrownedEntityRenderer::new,.5f);
        over(EntityType.ZOMBIFIED_PIGLIN,c->new PiglinEntityRenderer(c,EntityModelLayers.ZOMBIFIED_PIGLIN,EntityModelLayers.ZOMBIFIED_PIGLIN_INNER_ARMOR,EntityModelLayers.ZOMBIFIED_PIGLIN_OUTER_ARMOR,true),.5f);
        over(EntityType.VINDICATOR,VindicatorEntityRenderer::new,.5f);
        over(EntityType.PILLAGER,PillagerEntityRenderer::new,.5f);
        over(EntityType.EVOKER,c->new EvokerEntityRenderer<>(c),.5f);
        over(EntityType.WOLF,WolfEntityRenderer::new,.5f);
        over(EntityType.SPIDER,c->new SpiderEntityRenderer<>(c),.8f);
        over(EntityType.POLAR_BEAR,PolarBearEntityRenderer::new,.9f);
        over(EntityType.IRON_GOLEM,IronGolemEntityRenderer::new,.7f);
        over(EntityType.RAVAGER,RavagerEntityRenderer::new,1.1f);
        // camp NPCs (lobby NPC spec 3.1): a villager whose name names a look draws its GeckoLib rig, every other villager vanilla
        EntityRendererRegistry.register(EntityType.VILLAGER,CampNpcRenderer::new);
    }
    private static <T extends MobEntity> void over(EntityType<T> type,Function<EntityRendererFactory.Context,? extends EntityRenderer<? super T>> vanilla,float shadow){
        EntityRendererRegistry.register(type,ctx->new TroopRenderer<T>(ctx,vanilla.apply(ctx),shadow));
    }
}
