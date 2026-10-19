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
        check(UiMath.trimWords("Capture the objectives. Your army awaits your orders.",150,six).equals("Capture the objectives...") && UiMath.trimWords("Capture the objectives. Your army awaits your orders.",140,six).equals("Capture the...") && UiMath.trimWords("Hold the ford",140,six).equals("Hold the ford") && UiMath.trimWords("Hold, the ford",60,six).equals("Hold...") && UiMath.trimWords("Unbreakable line",60,six).equals("Unbreak..."), "trim at a word, else inside the first");
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
        var page = UiMath.fitKeys(java.util.List.of("Page Up","Page Down","Left Alt"),turn,154,mc); // keypad keys read 4 and 6 in game: these are long
        check(UiMath.hintRowWidth(java.util.List.of("Page Up","Page Down","Left Alt"),turn,mc)==204 && page.equals(java.util.List.of("P...Up","Pag...","L...Alt")) && UiMath.hintRowWidth(page,turn,mc)==147, "the turn keys on Page Up and Page Down fit the slip");
        var orders = java.util.List.of("Attack","Follow","Formation"); var ordersShort = java.util.List.of("Attack","Follow","Form"); // a tall card's key row (UI polish round 2)
        var plain = UiMath.cardRow(java.util.List.of("G","V","B"),orders,ordersShort,mc);
        check(plain.keys().equals(java.util.List.of("G","V","B")) && plain.words().equals(orders) && plain.at().equals(java.util.List.of(10,58,105)), "the default order keys keep the mock's places and words");
        var mmb = UiMath.cardRow(java.util.List.of("MMB","V","B"),orders,ordersShort,mc);
        check(mmb.keys().equals(java.util.List.of("MMB","V","B")) && mmb.words().equals(ordersShort) && mmb.at().equals(java.util.List.of(10,68,113)), "MMB on Attack would reach the Follow chip: the row flows, Formation as Form, MMB whole");
        var caps = UiMath.cardRow(java.util.List.of("Caps Lock","V","B"),orders,ordersShort,mc);
        check(caps.keys().equals(java.util.List.of("C...Lock","V","B")) && caps.at().equals(java.util.List.of(10,85,130)) && UiMath.hintRowWidth(caps.keys(),ordersShort,UiMath.CARD_ROW_GAP,mc)==156, "a long Attack name gives way as on the slip: C...Lock, the row ending at x 166");
        var three = UiMath.cardRow(java.util.List.of("Right Shift","Left Alt","Page Down"),orders,ordersShort,mc);
        check(three.keys().equals(java.util.List.of("Ri...","L...","P...")) && UiMath.hintRowWidth(three.keys(),ordersShort,UiMath.CARD_ROW_GAP,mc)==147, "three long names give way alike and the row still ends by x 166");
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
        check(java.util.stream.Stream.of("menu","map","hints","attack","confirm","hit","cancel","use").allMatch(id -> HintText.hinted(HintText.bindingKey(id))) && !HintText.hinted("key.forward") && !HintText.hinted("key.sneak"), "the keybind texts chipped are the hints' own bindings, no other");
        check(FeedLines.lost(3,"Dawn Knight").equals("3 Dawn Knights lost") && FeedLines.lost(1,"Dawn Knight").equals("Dawn Knight lost") && FeedLines.lost(4,"Dusk Revenant").equals("4 Dusk Revenants lost"), "loss copy");
        check(FeedLines.pointTaken("Dawn","Mill Hamlet").equals("Dawn took Mill Hamlet") && FeedLines.bannerDown("Ansel").equals("Ansel's banner has fallen") && FeedLines.bannerUp("Ansel").equals("Ansel is back in the field"), "point and banner copy");
        check(FeedLines.regrouped("Ranged").equals("Ranged regrouped") && FeedLines.routing("Support").equals("Support routing") && FeedLines.deploying(2,"Dawn Knight").equals("Dawn Knights deploying") && FeedLines.deploying(1,"Dawn Knight").equals("Dawn Knight deploying"), "rout and deploy copy");
        check(FeedLines.plural("Risen").equals("Risen") && FeedLines.plural("Order Bowman").equals("Order Bowmen") && FeedLines.teamShort("Dusk Legion").equals("Dusk"), "plurals and team names");
        check(FeedLines.style("point",0,0).equals(new FeedLines.Style("hud/fgv-seal-dawn",0xff1d4f4c)) && FeedLines.style("point",1,0).equals(new FeedLines.Style("hud/fgv-seal-x-dusk",0xff563a73)) && FeedLines.style("point",1,1).equals(new FeedLines.Style("hud/fgv-seal-dusk",0xff563a73)) && FeedLines.style("point",0,1).equals(new FeedLines.Style("hud/fgv-seal-x-dawn",0xff1d4f4c)), "seal by perspective, ink by team");
        check(FeedLines.style("loss",0,0).argb()==0xff7a2416 && FeedLines.style("kill",1,0).glyph().equals("hud/fgv-skull-grey") && FeedLines.style("down",0,0).glyph().equals("hud/fgv-banner-down") && FeedLines.style("regroup",0,0).argb()==0xff504b44, "loss, kill, banner, regroup");
        check(FeedLines.style("deploy",0,0).glyph().equals("hud/fgv-sun") && FeedLines.style("notice",1,1).glyph().equals("hud/fgv-moon") && FeedLines.style("bogus",0,0).equals(FeedLines.style("notice",0,0)), "own emblem; unknown kinds are notices");
        for(String kind:new String[]{"point","loss","kill","down","rout","deploy"}) for(int t=0;t<2;t++) check(UiMath.contrast(FeedLines.style(kind,t,0).argb(),0xffdccb9f)>=4.5, "feed ink "+kind+" holds 4.5:1 on the slip");
        check(UiMath.feedAlpha(0)==1f && UiMath.feedAlpha(6000)==1f && Math.abs(UiMath.feedAlpha(7000)-0.5f)<1e-6 && UiMath.feedAlpha(8000)==0f && UiMath.feedAlpha(9000)==0f, "8 s life, 2 s fade");
        check(UiMath.portraitSize(40,0)==32 && UiMath.portraitSize(160,0)==128 && UiMath.portraitSize(40,2)==29 && UiMath.portraitSize(160,2)==116, "2 px and 8 px per model pixel; Tall +2");
        check(UiMath.portraitFeet(40,0)==72 && UiMath.portraitFeet(160,0)==288 && UiMath.portraitFeet(40,2)==69, "feet below the window: 36 model px from the top");
        check(UiMath.plateLabelX(126)==57 && UiMath.plateLabelX(150)==57 && UiMath.plateLabelX(100)==70, "plate label clears the Enter key");
        check(UiMath.sheetRise(0,0f)==0f && UiMath.sheetRise(3,0f)==1f && UiMath.sheetRise(40,0.5f)==1f && UiMath.sheetRise(1,0.5f)>0.5f && UiMath.sheetRise(1,0.5f)<UiMath.sheetRise(2,0f) && Math.abs(UiMath.sheetRise(1,0.5f)-0.875f)<1e-6, "the focused sheet rises over 3 ticks (150 ms), easing out");
        check(UiMath.sealStamp(0,0.9f)==0f && UiMath.sealStamp(1,0f)==0f && UiMath.sealStamp(2,0f)==0.25f && UiMath.sealStamp(3,0f)==1f && UiMath.sealStamp(9,0.5f)==1f, "the seal stamps in the rise's last 2 ticks (100 ms), easing in");
        check(UiMath.lettersLine(0).equals("No letters left") && UiMath.lettersLine(1).equals("1 letter left") && UiMath.lettersLine(3).equals("3 letters left"), "letters left");
        check(UiMath.pityLine(28).equals("Epic within 28 letters") && UiMath.pityLine(1).equals("Epic in the next letter"), "pity line (Phase 2)");
        check(UiMath.oddsLine(4).equals("Each new troop: 25.0%") && UiMath.oddsLine(3).equals("Each new troop: 33.3%") && UiMath.oddsLine(0).equals("Nothing left to draw"), "odds line");
        check(UiMath.wheel(2,4,0,-1).equals(new UiMath.Wheel(3,0)) && UiMath.wheel(2,4,0,1).equals(new UiMath.Wheel(1,0)) && UiMath.wheel(0,0,0,-1).equals(new UiMath.Wheel(0,0)), "a notch toward the player moves a row down the list, away a row up; a roll that fits moves not at all");
        check(UiMath.wheel(2,4,0,0).equals(new UiMath.Wheel(2,0)) && UiMath.wheel(2,4,0.4,0).equals(new UiMath.Wheel(2,0.4)) && UiMath.wheel(2,4,0,Double.NaN).equals(new UiMath.Wheel(2,0)), "a turn of 0 (a sideways scroll) moves nothing and keeps the carry");
        UiMath.Wheel w = new UiMath.Wheel(0,0); int firstMove = -1;
        for (int i = 1; i <= 10; i++) { w = UiMath.wheel(w.first(), 4, w.carry(), -0.1); if (w.first() == 1 && firstMove < 0) firstMove = i; }
        check(firstMove == 10 && w.first() == 1 && w.carry() == 0 && UiMath.wheel(1,4,0.5,-0.4).equals(new UiMath.Wheel(1,0.9)) && UiMath.wheel(1,4,0.5,0.75).equals(new UiMath.Wheel(1,-0.25)), "a touchpad's tenth turns add up to one row on the tenth; fractions carry either way");
        check(UiMath.wheel(3,4,0,-5).equals(new UiMath.Wheel(4,0)) && UiMath.wheel(4,4,0,-0.5).equals(new UiMath.Wheel(4,0)) && UiMath.wheel(4,4,0,1).equals(new UiMath.Wheel(3,0)) && UiMath.wheel(1,4,0,3).equals(new UiMath.Wheel(0,0)) && UiMath.wheel(0,4,0,0.5).equals(new UiMath.Wheel(0,0)) && UiMath.wheel(0,4,0,-2.5).equals(new UiMath.Wheel(2,0.5)), "past either end nothing carries, so a turn back moves at once");
        check(UiMath.lines("A unit roll costs 100 gold. Earn gold by completing battles.",200,2,six).equals(java.util.List.of("A unit roll costs 100 gold.","Earn gold by completing battles.")) && UiMath.lines("Every Dawn Covenant troop is already yours.",300,2,six).equals(java.util.List.of("Every Dawn Covenant troop is already yours.")), "a notice too long for a line breaks after a sentence that fits; one that fits stays whole");
        check(UiMath.lines("Hold the ford at the bridge",60,2,six).equals(java.util.List.of("Hold the","ford at...")) && UiMath.lines("Hold the ford",60,3,six).equals(java.util.List.of("Hold the","ford")) && UiMath.lines("Unbreakable line",60,2,six).equals(java.util.List.of("Unbreak...")) && UiMath.lines("  ",60,2,six).isEmpty(), "else at the last space that fits; the last line ends as trimWords ends it");
        System.out.println("UiMathCheck: " + checks + " checks passed");
    }
}
