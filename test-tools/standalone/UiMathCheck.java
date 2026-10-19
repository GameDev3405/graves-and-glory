package gg.gravesandglory;

/** Pure UI maths: the canvas pixel scale, text trimming, WCAG contrast and key widths; key hint text and the event feed's copy,
 *  styles and fade. Never shipped. */
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
        java.util.function.ToIntFunction<String> mc = s -> s.chars().map(ch -> ch==' '||ch=='t'||ch=='['||ch==']' ? 4 : ch=='.'||ch=='i' ? 2 : ch=='l' ? 3 : ch=='f'||ch=='k' ? 5 : 6).sum(); // Minecraft's font, near enough
        check(UiMath.trimKey("Left Alt",40,mc).equals("Left Alt") && UiMath.trimKey("Right Shift",36,mc).equals("R...Shift") && UiMath.trimKey("Keypad 4",30,mc).equals("Key...4") && UiMath.trimKey("Backspace",30,mc).equals("Back...") && UiMath.trimKey("Right Shift",32,mc).equals("Right..."), "key names keep their last word after the dots, else their start");
        java.util.List<String> turn = java.util.Arrays.asList(null,"Turn","Hints"), map = java.util.List.of("Map","Commands");
        check(UiMath.fitKeys(java.util.List.of("[","]","Left Alt"),turn,154,mc).equals(java.util.List.of("[","]","Left Alt")) && UiMath.fitKeys(java.util.List.of("Not bound","K"),map,154,mc).equals(java.util.List.of("Not bound","K")) && UiMath.hintRowWidth(java.util.List.of("Not bound","K"),map,mc)==142, "hint rows that fit are left alone");
        var keypad = UiMath.fitKeys(java.util.List.of("Keypad 4","Keypad 6","Left Alt"),turn,154,mc);
        check(UiMath.hintRowWidth(java.util.List.of("Keypad 4","Keypad 6","Left Alt"),turn,mc)==204 && keypad.equals(java.util.List.of("Ke...4","Ke...6","L...Alt")) && UiMath.hintRowWidth(keypad,turn,mc)<=154, "the keypad's turn keys fit the slip and stay apart");
        var shift = UiMath.fitKeys(java.util.List.of("Not bound","Right Shift"),map,154,mc);
        check(shift.equals(java.util.List.of("Not b...","R...Shift")) && UiMath.hintRowWidth(shift,map,mc)==153, "two long names on row one fit the slip");
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
        java.util.function.Function<String,String> keys = id -> id.equals("menu") ? "K" : "Not bound";
        check(HintText.expand("{key:menu} opens your war council.",keys).equals("K opens your war council."), "token replaced");
        check(HintText.expand("Map {key:map}",keys).equals("Map Not bound") && HintText.expand("{key:menu",keys).equals("{key:menu"), "unbound and malformed");
        check(HintText.chip("Left Button").equals("LMB") && HintText.chip("Right Button").equals("RMB") && HintText.chip("Middle Button").equals("MMB") && HintText.chip("K").equals("K"), "mouse chips");
        check(HintText.tokens("a {key:menu} b").equals(java.util.List.of("a ","menu"," b")) && HintText.tokens("{key:map}").equals(java.util.List.of("","map","")) && HintText.tokens("{key:menu").equals(java.util.List.of("{key:menu")), "tokens alternate text and key ids");
        check(HintText.bindingKey("menu").equals("key.gravesandglory.menu") && HintText.bindingKey("hit").equals("key.attack") && HintText.bindingKey("confirm").equals("key.attack") && HintText.bindingKey("cancel").equals("key.use") && HintText.bindingKey("use").equals("key.use"), "binding translation keys");
        check(FeedLines.lost(3,"Dawn Knight").equals("3 Dawn Knights lost") && FeedLines.lost(1,"Dawn Knight").equals("Dawn Knight lost") && FeedLines.lost(4,"Dusk Revenant").equals("4 Dusk Revenants lost"), "loss copy");
        check(FeedLines.pointTaken("Dawn","Mill Hamlet").equals("Dawn took Mill Hamlet") && FeedLines.bannerDown("Ansel").equals("Ansel's banner has fallen") && FeedLines.bannerUp("Ansel").equals("Ansel is back in the field"), "point and banner copy");
        check(FeedLines.regrouped("Ranged").equals("Ranged regrouped") && FeedLines.routing("Support").equals("Support routing") && FeedLines.deploying(2,"Dawn Knight").equals("Dawn Knights deploying") && FeedLines.deploying(1,"Dawn Knight").equals("Dawn Knight deploying"), "rout and deploy copy");
        check(FeedLines.plural("Risen").equals("Risen") && FeedLines.plural("Order Bowman").equals("Order Bowmen") && FeedLines.teamShort("Dusk Legion").equals("Dusk"), "plurals and team names");
        check(FeedLines.style("point",0,0).equals(new FeedLines.Style("hud/fgv-seal-dawn",0xff1d4f4c)) && FeedLines.style("point",1,0).equals(new FeedLines.Style("hud/fgv-seal-x-dusk",0xff563a73)) && FeedLines.style("point",1,1).equals(new FeedLines.Style("hud/fgv-seal-dusk",0xff563a73)) && FeedLines.style("point",0,1).equals(new FeedLines.Style("hud/fgv-seal-x-dawn",0xff1d4f4c)), "seal by perspective, ink by team");
        check(FeedLines.style("loss",0,0).argb()==0xff7a2416 && FeedLines.style("kill",1,0).glyph().equals("hud/fgv-skull-grey") && FeedLines.style("down",0,0).glyph().equals("hud/fgv-banner-down") && FeedLines.style("regroup",0,0).argb()==0xff504b44, "loss, kill, banner, regroup");
        check(FeedLines.style("deploy",0,0).glyph().equals("hud/fgv-sun") && FeedLines.style("notice",1,1).glyph().equals("hud/fgv-moon") && FeedLines.style("bogus",0,0).equals(FeedLines.style("notice",0,0)), "own emblem; unknown kinds are notices");
        for(String kind:new String[]{"point","loss","kill","down","rout","deploy"}) for(int t=0;t<2;t++) check(UiMath.contrast(FeedLines.style(kind,t,0).argb(),0xffdccb9f)>=4.5, "feed ink "+kind+" holds 4.5:1 on the slip");
        check(UiMath.feedAlpha(0)==1f && UiMath.feedAlpha(6000)==1f && Math.abs(UiMath.feedAlpha(7000)-0.5f)<1e-6 && UiMath.feedAlpha(8000)==0f && UiMath.feedAlpha(9000)==0f, "8 s life, 2 s fade");
        System.out.println("UiMathCheck: " + checks + " checks passed");
    }
}
