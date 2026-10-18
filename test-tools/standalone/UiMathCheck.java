package gg.gravesandglory;

/** Pure UI maths: the canvas pixel scale, text trimming, WCAG contrast and key widths. Never shipped. */
public final class UiMathCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        check(UiMath.pixelScale(1920,1080,4)==2 && UiMath.pixelScale(1920,1080,1)==1, "1080p is 2, or the player's smaller GUI Scale");
        check(UiMath.pixelScale(2560,1440,6)==2 && UiMath.pixelScale(3840,2160,9)==4, "1440p and 4K");
        check(UiMath.pixelScale(1366,768,3)==1 && UiMath.pixelScale(854,480,2)==1, "small windows never go below 1");
        java.util.function.ToIntFunction<String> six = s -> 6*s.length();
        check(UiMath.trim("abcdefghijklmnopqrstuvwxyz",60,six).equals("abcdefg...") && UiMath.trim("short",60,six).equals("short"), "trim with an ellipsis");
        check(UiMath.brassKeyWidth(18,3)==27 && UiMath.brassKeyWidth(28,5)==37 && UiMath.brassKeyWidth(6,1)==13 && UiMath.brassKeyWidth(80,12)==48, "brass keys: Esc 27, Enter 37, B 13, cap 48");
        check(UiMath.chipWidth(6)==10 && UiMath.chipWidth(18)==22, "ink chips: G 10, LMB 22");
        check(UiMath.contrast(0xffeee9df,0xff1f2427)>=4.5 && UiMath.contrast(0xffe47a64,0xff1f2427)>=4.5, "ivory and alarm text on the iron plate");
        check(Math.abs(UiMath.contrast(0xff000000,0xffffffff)-21.0)<0.01, "WCAG ratio");
        check(UiMath.sealCentre(0,3,960)==452 && UiMath.sealCentre(2,3,960)==508 && UiMath.sealCentre(0,5,960)==424 && UiMath.sealCentre(4,5,960)==536, "seals on a 28 px pitch, centred");
        check(java.util.Arrays.equals(UiMath.cardTops(540,new int[]{48,62,48}),new int[]{348,404,474}) && java.util.Arrays.equals(UiMath.cardTops(540,new int[]{48,48}),new int[]{418,474}), "cards end 18 above the bottom, 8 px apart");
        check(UiMath.countText(9,12).equals("9/12") && UiMath.countText(0,2).equals("0/2") && UiMath.clock(462).equals("7:42") && UiMath.clock(5).equals("0:05"), "count and clock");
        check(UiMath.trackFill(148,300)==27 && UiMath.trackFill(400,300)==54 && UiMath.trackFill(10,0)==0, "track fill, guarded");
        check(UiMath.barWidth(0.667f)==68 && UiMath.barWidth(1.4f)==102 && UiMath.barWidth(-1f)==0, "strength bar of 102 px");
        check(UiMath.sweep(18,6,24,0.5)==0 && UiMath.sweep(5,18,24,0.5)==1 && UiMath.sweep(12,20,24,0.5)==2 && UiMath.sweep(12,3,24,1.0)==3, "sweep classes");
        int veil=0; for(int y=0;y<24;y++) for(int x=0;x<24;x++) if(UiMath.sweep(x,y,24,48/90.0)==1) veil++;
        check(veil==297, "the painter's sweep at 48 of 90 s veils 297 pixels");
        System.out.println("UiMathCheck: " + checks + " checks passed");
    }
}
