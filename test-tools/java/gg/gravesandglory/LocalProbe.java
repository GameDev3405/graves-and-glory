package gg.gravesandglory;
public final class LocalProbe implements Diagnostics.Probe {
    public void start(){MeltyTest.start();}
    public boolean active(){return MeltyTest.active();}
    public void report(String name,boolean passed,String why){MeltyTest.report(name,passed,why);}
    public void tick(net.minecraft.client.MinecraftClient c){CampaignDevControl.tick(c);}
    public boolean hideHud(){return VisualChecks.hideHud();}
    public boolean selfTest(){return Boolean.getBoolean("gravesandglory.selfTest");}
}
