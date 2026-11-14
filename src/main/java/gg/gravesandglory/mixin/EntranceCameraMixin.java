package gg.gravesandglory.mixin;
import gg.gravesandglory.CampaignClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class EntranceCameraMixin {
    @Shadow protected abstract void setPos(double x,double y,double z);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="update",at=@At("TAIL"))
    void gravesandgloryEntrance(BlockView area,Entity entity,boolean third,boolean inverse,float delta,CallbackInfo ci){
        if(CampaignClient.cinematic()){CampaignClient.sampleCamera(delta);setPos(CampaignClient.cameraX,CampaignClient.cameraY,CampaignClient.cameraZ);setRotation(CampaignClient.cameraYaw,CampaignClient.cameraPitch);}
        else if(gg.gravesandglory.EndingClient.cinematic()){gg.gravesandglory.EndingClient.sampleCamera(delta);setPos(gg.gravesandglory.EndingClient.cameraX,gg.gravesandglory.EndingClient.cameraY,gg.gravesandglory.EndingClient.cameraZ);setRotation(gg.gravesandglory.EndingClient.cameraYaw,gg.gravesandglory.EndingClient.cameraPitch);}
        else if(gg.gravesandglory.DropClient.cinematic()){gg.gravesandglory.DropClient.sampleCamera(delta);setPos(gg.gravesandglory.DropClient.cameraX,gg.gravesandglory.DropClient.cameraY,gg.gravesandglory.DropClient.cameraZ);setRotation(gg.gravesandglory.DropClient.cameraYaw,gg.gravesandglory.DropClient.cameraPitch);}
        else {float kick=gg.gravesandglory.Presentation.kick();if(kick!=0){Camera self=(Camera)(Object)this;setRotation(self.getYaw(),self.getPitch()-kick);}}
    }
}
