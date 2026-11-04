package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.ScreenshotRecorder;

/** Real menu widgets and transitions; only loaded by the isolated dev harness. */
public final class FrontMenuChecks implements ClientModInitializer {
    int ticks,step,worldTicks,returnTicks;boolean started,returning;
    void report(String id,boolean pass){MeltyTest.report("front-"+id,pass,id);if(!pass)throw new IllegalStateException(id);}
    ButtonWidget find(MinecraftClient c,String prefix){return c.currentScreen.children().stream().filter(e->e instanceof ButtonWidget b&&b.getMessage().getString().startsWith(prefix)).map(e->(ButtonWidget)e).findFirst().orElseThrow();}
    void click(MinecraftClient c,String prefix){find(c,prefix).onPress();}
    void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,"front-"+name+".png",c.getFramebuffer(),t->{});}
    void controls(MinecraftClient c){
        for(var key:c.options.allKeys)if(key.getTranslationKey().equals("iris.keybind.reload")){
            report("iris-default-conflict",!key.getBoundKeyTranslationKey().equals(GravesAndGloryClient.menu.getBoundKeyTranslationKey()));
            var saved=net.minecraft.client.util.InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
            key.setBoundKey(net.minecraft.client.util.InputUtil.Type.KEYSYM.createFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_P));
            PackControls.prepare(c);
            report("iris-custom-preserved",key.getBoundKeyTranslationKey().equals("key.keyboard.p"));
            key.setBoundKey(saved);net.minecraft.client.option.KeyBinding.updateKeysByCode();
        }
    }
    @Override public void onInitializeClient(){
        if(!Boolean.getBoolean("gravesandglory.frontChecks"))return;
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.getOverlay()!=null)return;
            try{
                if(!started){
                    if(!(c.currentScreen instanceof FrontMenuScreen)&&step==0)return;
                    ticks++;
                    if(ticks==80){report("idle-no-world",c.player==null&&c.getServer()==null);report("no-public-placeholder",c.currentScreen.children().stream().noneMatch(e->e instanceof ButtonWidget w&&w.getMessage().getString().startsWith("Public battles")));controls(c);shot(c,"home");}
                    // Key art (board 4F.3): the home page at two window sizes, GUI scale Auto, to check the cover-scale and that the buttons leave the figures clear.
                    if(ticks==81){c.options.getGuiScale().setValue(0);org.lwjgl.glfw.GLFW.glfwSetWindowSize(c.getWindow().getHandle(),1920,1080);}
                    if(ticks==85){MeltyTest.report("front-art-1080-size",true,c.getWindow().getFramebufferWidth()+"x"+c.getWindow().getFramebufferHeight()+" gui "+c.getWindow().getScaledWidth()+"x"+c.getWindow().getScaledHeight());shot(c,"home-1080");org.lwjgl.glfw.GLFW.glfwSetWindowSize(c.getWindow().getHandle(),854,480);}
                    if(ticks==89){MeltyTest.report("front-art-480-size",true,c.getWindow().getFramebufferWidth()+"x"+c.getWindow().getFramebufferHeight()+" gui "+c.getWindow().getScaledWidth()+"x"+c.getWindow().getScaledHeight());shot(c,"home-480p");}
                    if(ticks==90){click(c,"Settings");step=1;}
                    if(ticks==100){report("settings",c.currentScreen instanceof net.minecraft.client.gui.screen.option.OptionsScreen);c.currentScreen.close();}
                    if(ticks==110){click(c,"Credits");}
                    if(ticks==120){report("credits",c.currentScreen instanceof FrontMenuScreen);shot(c,"credits");click(c,"Back");}
                    if(ticks==130){click(c,"Play with friends");}
                    if(ticks==140){report("optional-launcher",!GravesAndGloryClient.launcherContext);shot(c,"friends");click(c,"Join a server");}
                    if(ticks==150){report("server-browser",c.currentScreen instanceof net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen);click(c,"Cancel");}
                    if(ticks==160){click(c,"Back");c.options.getGuiScale().setValue(3);c.onResolutionChanged();}
                    if(ticks==170){report("small-layout",c.currentScreen.children().stream().filter(e->e instanceof ButtonWidget).map(e->(ButtonWidget)e).allMatch(b->b.getY()>=0&&b.getY()+b.getHeight()<=c.getWindow().getScaledHeight()-16));shot(c,"small");}
                    if(ticks==180){c.options.getGuiScale().setValue(2);c.onResolutionChanged();if(Boolean.getBoolean("gravesandglory.frontHost")){click(c,"Play with friends");click(c,"Host a camp");}else click(c,"Enter camp");started=true;}
                }else if(c.player!=null&&GravesAndGloryClient.ready){
                    if(++worldTicks==20){boolean host=Boolean.getBoolean("gravesandglory.frontHost");report(host?"friends-lan":"practice-private",c.getServer()!=null&&c.getServer().isRemote()==host);}
                    if(worldTicks==1040){c.world.disconnect();c.disconnect();c.setScreen(new TitleScreen());returning=true;}
                }else if(returning&&c.currentScreen instanceof FrontMenuScreen){
                    if(++returnTicks==20){report("disconnect-menu",c.player==null&&c.getServer()==null);shot(c,"return");}
                    if(returnTicks==30)c.scheduleStop();
                }
            }catch(Exception e){MeltyTest.report("front-suite",false,e.toString());e.printStackTrace();c.scheduleStop();}
        });
    }
}
