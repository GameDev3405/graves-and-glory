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
        System.out.println("UiMathCheck: " + checks + " checks passed");
    }
}
