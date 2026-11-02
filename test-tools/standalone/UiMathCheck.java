package gg.gravesandglory;

/** Pure UI maths: the canvas pixel scale, text trimming, WCAG contrast and key widths; key hint text and the event feed's copy,
 *  styles and fade. Never shipped. */
public final class UiMathCheck {
    static int checks, failed;
    /** -Duimath.all=true lists every failing check instead of stopping at the first (for the red runs of a deliberate break). */
    static final boolean ALL = Boolean.getBoolean("uimath.all");
    static void check(boolean pass, String message) {
        if (!pass && !ALL) throw new AssertionError(message);
        if (!pass) { failed++; System.out.println("FAILED: " + message); return; }
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
        check(UiMath.contrast(0xff3a2a12,0xffbda97c)>=4.5 && UiMath.contrast(0xff5c4a32,0xffbda97c)<4.5, "KEY_INK, the key strip's labels and the lesson tag (UI Task 8c fix round 1), holds 4.5:1 on the darkest vellum behind them, #bda97c, where sepia does not");
        check(Math.abs(UiMath.contrast(0xff000000,0xffffffff)-21.0)<0.01, "WCAG ratio");
        check(UiMath.sealCentre(0,3,960)==452 && UiMath.sealCentre(2,3,960)==508 && UiMath.sealCentre(0,5,960)==424 && UiMath.sealCentre(4,5,960)==536, "seals on a 28 px pitch, centred");
        check(java.util.Arrays.equals(UiMath.cardTops(522,new int[]{18,62,18},4),new int[]{416,438,504}) && java.util.Arrays.equals(UiMath.cardTops(502,new int[]{18,18,18},4),new int[]{440,462,484}) && UiMath.cardTops(522,new int[0],4).length==0, "the column's plates stack up from its foot, 4 px apart (UI Task 8c: a chip each, the selected group's card)");
        check(UiMath.countText(9,12).equals("9/12") && UiMath.countText(0,2).equals("0/2") && UiMath.clock(462).equals("7:42") && UiMath.clock(5).equals("0:05"), "count and clock");
        check(UiMath.trackFill(148,300)==27 && UiMath.trackFill(400,300)==54 && UiMath.trackFill(10,0)==0, "track fill, guarded");
        check(UiMath.barWidth(0.667f)==68 && UiMath.barWidth(1.4f)==102 && UiMath.barWidth(-1f)==0, "strength bar of 102 px");
        check(UiMath.sweep(18,6,24,0.5)==0 && UiMath.sweep(5,18,24,0.5)==1 && UiMath.sweep(12,20,24,0.5)==2 && UiMath.sweep(12,3,24,1.0)==3, "sweep classes");
        int veil=0; for(int y=0;y<24;y++) for(int x=0;x<24;x++) if(UiMath.sweep(x,y,24,48/90.0)==1) veil++;
        check(veil==297, "the painter's sweep at 48 of 90 s veils 297 pixels");
        java.util.function.Function<String,String> keys = id -> id.equals("menu") ? "K" : "Not bound";
        check(HintText.expand("{key:menu} opens your war council.",keys).equals("K opens your war council."), "token replaced");
        check(HintText.expand("Map {key:map}",keys).equals("Map Not bound") && HintText.expand("{key:menu",keys).equals("{key:menu"), "unbound and malformed");
        check("LMB".equals(HintText.mouseChip(0)) && "RMB".equals(HintText.mouseChip(1)) && "MMB".equals(HintText.mouseChip(2)) && HintText.mouseChip(3) == null && HintText.mouseChip(-1) == null, "mouse chips by the button's code, in every language; other buttons keep the game's name");
        check(HintText.tokens("a {key:menu} b").equals(java.util.List.of("a ","menu"," b")) && HintText.tokens("{key:map}").equals(java.util.List.of("","map","")) && HintText.tokens("{key:menu").equals(java.util.List.of("{key:menu")), "tokens alternate text and key ids");
        check(HintText.bindingKey("menu").equals("key.gravesandglory.menu") && HintText.bindingKey("hit").equals("key.attack") && HintText.bindingKey("confirm").equals("key.attack") && HintText.bindingKey("cancel").equals("key.use") && HintText.bindingKey("use").equals("key.use"), "binding translation keys");
        check(java.util.stream.Stream.of("menu","map","hints","attack","confirm","hit","cancel","use").allMatch(id -> HintText.hinted(HintText.bindingKey(id))) && !HintText.hinted("key.forward") && !HintText.hinted("key.sneak"), "the keybind texts chipped are the hints' own bindings, no other");
        check(FeedLines.lost(3,"Dawn Knight").equals("3 Dawn Knights lost") && FeedLines.lost(1,"Dawn Knight").equals("Dawn Knight lost") && FeedLines.lost(4,"Dusk Revenant").equals("4 Dusk Revenants lost"), "loss copy");
        check(FeedLines.pointTaken("Dawn","Mill Hamlet").equals("Dawn took Mill Hamlet") && FeedLines.bannerDown("Ansel").equals("Ansel's banner has fallen") && FeedLines.bannerUp("Ansel").equals("Ansel is back in the field"), "point and banner copy");
        check(FeedLines.lastStand("Support").equals("Support make a last stand") && FeedLines.lastStand("Infantry").equals("Infantry make a last stand") && FeedLines.deploying(2,"Dawn Knight").equals("Dawn Knights deploying") && FeedLines.deploying(1,"Dawn Knight").equals("Dawn Knight deploying"), "last stand and deploy copy");
        check(FeedLines.plural("Risen").equals("Risen") && FeedLines.plural("Order Bowman").equals("Order Bowmen") && FeedLines.teamShort("Dusk Legion").equals("Dusk"), "plurals and team names");
        check(FeedLines.style("point",0,0).equals(new FeedLines.Style("hud/fgv-seal-dawn",0xff1b4a47)) && FeedLines.style("point",1,0).equals(new FeedLines.Style("hud/fgv-seal-x-dusk",0xff52376d)) && FeedLines.style("point",1,1).equals(new FeedLines.Style("hud/fgv-seal-dusk",0xff52376d)) && FeedLines.style("point",0,1).equals(new FeedLines.Style("hud/fgv-seal-x-dawn",0xff1b4a47)), "seal by perspective, ink by team");
        check(FeedLines.style("loss",0,0).argb()==0xff7a2416 && FeedLines.style("kill",1,0).glyph().equals("hud/fgv-skull-grey") && FeedLines.style("down",0,0).glyph().equals("hud/fgv-banner-down") && FeedLines.style("laststand",0,0).equals(new FeedLines.Style("hud/fgv-laststand",0xff47433d)) && FeedLines.style("up",0,0).argb()==0xff5c3e00, "loss, kill, banner, last stand");
        check(FeedLines.style("deploy",0,0).glyph().equals("hud/fgv-sun") && FeedLines.style("notice",1,1).glyph().equals("hud/fgv-moon") && FeedLines.style("bogus",0,0).equals(FeedLines.style("notice",0,0)), "own emblem; unknown kinds are notices");
        for(String kind:new String[]{"point","loss","kill","down","up","laststand","deploy","notice"}) for(int t=0;t<2;t++) for(int v=0;v<2;v++) check(UiMath.contrast(FeedLines.style(kind,t,v).argb(),0xffdccb9f)>=5.2, "feed ink "+kind+" (team "+t+", seen by "+v+") holds the spec's 5.2:1 on the slip");
        // UI Task 8c fix round 1: the leaves' darker vellum, #d0bd90, behind 34% of the Compact leaf's text rows (14% of the five-entry
        // slip's), and the next tone down, #c2ae80, where the 90th-percentile worst of that darker vellum falls (measured on the shots of
        // runs ui-8c-base-211317 and ui-8c-green-233227)
        for(String kind:new String[]{"point","loss","kill","down","up","laststand","deploy","notice"}) for(int t=0;t<2;t++) for(int v=0;v<2;v++) check(UiMath.contrast(FeedLines.style(kind,t,v).argb(),0xffd0bd90)>=5.2 && UiMath.contrast(FeedLines.style(kind,t,v).argb(),0xffc2ae80)>=4.5, "feed ink "+kind+" (team "+t+", seen by "+v+") holds 5.2:1 on the leaf's darker vellum #d0bd90 and 4.5:1 on #c2ae80");
        check(UiMath.feedAlpha(0,8000)==1f && UiMath.feedAlpha(6000,8000)==1f && Math.abs(UiMath.feedAlpha(7000,8000)-0.5f)<1e-6 && UiMath.feedAlpha(8000,8000)==0f && UiMath.feedAlpha(9000,8000)==0f, "8 s life, 2 s fade");
        check(UiMath.feedLife(UiMath.HUD_COMPACT)==6000 && UiMath.feedLife(UiMath.HUD_STANDARD)==8000 && UiMath.feedLife(UiMath.HUD_LARGE)==8000 && UiMath.feedLines(UiMath.HUD_COMPACT)==3 && UiMath.feedLines(UiMath.HUD_STANDARD)==5 && UiMath.feedLines(UiMath.HUD_LARGE)==5
            && UiMath.feedAlpha(3999,6000)==1f && Math.abs(UiMath.feedAlpha(5000,6000)-0.5f)<1e-6 && UiMath.feedAlpha(6000,6000)==0f, "Compact's feed (UI Task 8c): three lines that live 6 s, fading over the last 2; Standard and Large keep the spec's five and 8 s");
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
        check(UiMath.thousands(2870).equals("2,870") && UiMath.thousands(7500).equals("7,500") && UiMath.thousands(250).equals("250") && UiMath.thousands(0).equals("0"), "Chapter numbers with thousands separators");
        check(UiMath.trackFirst(11,30,10)==10 && UiMath.trackFirst(12,30,10)==11 && UiMath.trackFirst(1,30,10)==1 && UiMath.trackFirst(2,30,10)==1 && UiMath.trackFirst(31,30,10)==21, "the track starts at the last claim before the first one left, within tiers 1 and 21");
        check(UiMath.trackPage(10,-1,30,10)==1 && UiMath.trackPage(10,1,30,10)==20 && UiMath.trackPage(20,1,30,10)==21 && UiMath.trackPage(1,-1,30,10)==1, "the arrows page the track by ten, within its ends");
        check(UiMath.heroTier(10,10,30,5)==20 && UiMath.heroTier(11,10,30,5)==25 && UiMath.heroTier(1,10,30,5)==15 && UiMath.heroTier(21,10,30,5)==30, "the hero is the first title past the track's right end, else the last tier");
        check(UiMath.xpPoured(2870,250,30,360)==138 && UiMath.xpPoured(0,250,30,360)==0 && UiMath.xpPoured(9000,250,30,360)==360 && UiMath.xpPoured(-5,250,30,360)==0, "the XP rail pours 138 of 360 columns for 2,870 of 7,500 XP, as the approved track");
        check(UiMath.spellSmall("Complete 1 battle").equals("Complete one battle") && UiMath.spellSmall("Issue 10 tactical orders").equals("Issue ten tactical orders") && UiMath.spellSmall("Complete 6 battles this week").equals("Complete six battles this week")
            && UiMath.spellSmall("Hold 3.").equals("Hold three.") && UiMath.spellSmall("Win 25 battles").equals("Win 25 battles") && UiMath.spellSmall("1v1 in room AB12CD34").equals("1v1 in room AB12CD34") && UiMath.spellSmall("2.5 or 1,000 or 05").equals("2.5 or 1,000 or 05"), "a heading spells its counts to twenty (its old-style 1 and 0 read as I and O); codes, 1v1, decimals and larger numbers stay");
        // UI Task 8c: the HUD size setting, the column of chips, the lesson note, the lighter feed and the footprint budget. Task 8a's
        // scale rule (the GUI Scale up to 2 under 1600 px wide) is gone: Kort found too much of the screen blocked.
        int[][] win = {{960,540,1},{854,480,2},{1280,720,3},{1920,1080,2},{1920,1080,4},{1280,720,2},{1366,768,2}}; // the ui-hud suite's windows
        int[][] scales = {{1,1,1},{1,1,1},{1,1,1},{2,3,3},{2,3,3},{1,2,2},{1,2,2}}; // Compact, Standard, Large there
        String picked = ""; boolean pickedOk = true;
        for (int i = 0; i < win.length; i++) {
            int[] s = win[i]; int c = UiMath.hudScale(s[0],s[1],s[2],UiMath.HUD_COMPACT), st = UiMath.hudScale(s[0],s[1],s[2],UiMath.HUD_STANDARD), l = UiMath.hudScale(s[0],s[1],s[2],UiMath.HUD_LARGE);
            pickedOk &= c == scales[i][0] && st == scales[i][1] && l == scales[i][2];
            picked += (picked.isEmpty() ? "" : ", ") + s[0]+"x"+s[1]+" GUI "+s[2]+" "+c+"/"+st+"/"+l;
        }
        check(pickedOk, "Compact at the kit scale on every window; Standard a step up where the HUD fits (1080p 3, 720p and 1366x768 at GUI 2 2), else the kit's; Large the same at these windows: " + picked);
        check(UiMath.hudScale(2560,1440,4,UiMath.HUD_STANDARD)==3 && UiMath.hudScale(2560,1440,4,UiMath.HUD_LARGE)==4 && UiMath.hudScale(3840,2160,4,UiMath.HUD_LARGE)==6 && UiMath.hudScale(3840,2160,4,UiMath.HUD_COMPACT)==4 && UiMath.hudScale(1280,720,2,7)==1, "Large is two steps up where the window has the room (2560x1440, 4K); an unknown size is Compact");
        check(!UiMath.hudFits(960,540,1,2,UiMath.HUD_STANDARD) && !UiMath.hudFits(1920,1080,4,4,UiMath.HUD_LARGE) && !UiMath.hudFits(1280,720,3,2,UiMath.HUD_STANDARD) && UiMath.hudFits(1920,1080,4,3,UiMath.HUD_LARGE), "a step that would not fit is not taken: 960x540 at 2x (the respawn card on the feed), 1080p at 4x (the hint slip in the lesson's shadow), 720p GUI 3 at 2x (the tutorial's chat pushed off the screen)");
        int sizes = 0; String bad = "";
        for (int fw : new int[]{0,1,320,640,854,959,960,1024,1280,1366,1440,1599,1600,1920,2560,3840}) for (int fh : new int[]{0,1,240,480,540,720,768,900,1080,1440,2160}) for (int gui : new int[]{-1,0,1,2,3,4,6}) for (int z = 0; z < 3; z++) {
            int k = UiMath.hudScale(fw,fh,gui,z), g = Math.max(1,gui), kit = UiMath.pixelScale(fw,fh,g);
            if (k < kit || k > kit + z || z == UiMath.HUD_COMPACT && k != kit || k > kit && !UiMath.hudFits(fw,fh,g,k,z) || z == UiMath.HUD_LARGE && k < UiMath.hudScale(fw,fh,gui,UiMath.HUD_STANDARD)) bad += (bad.isEmpty() ? "" : ", ") + "hudScale("+fw+","+fh+","+gui+","+z+") = "+k;
            sizes++;
        }
        check(bad.isEmpty() && sizes == 16*11*7*3 && !UiMath.hudFits(0,0,2,2,0) && !UiMath.hudFits(1280,720,0,1,0) && !UiMath.hudFits(1280,720,2,0,0), "every size, a 0 x 0 window and GUI 0 included: Compact the kit scale (1 or more, so no division by zero), Standard and Large no more than one and two steps above it, above it only where the HUD fits, Large never under Standard" + (bad.isEmpty() ? "" : "; wrong: " + bad.substring(0, Math.min(300, bad.length()))));
        check(UiMath.hintLayerMs(UiMath.HUD_COMPACT)==15000 && UiMath.hintLayerMs(UiMath.HUD_STANDARD)==60000 && UiMath.hintLayerMs(UiMath.HUD_LARGE)==60000, "the hint layer: a round's first 15 s at Compact, the spec's 60 s at Standard and Large");
        // UI Task 8c fix round 1 (the review's Important 2): the size a window shows each choice at, and the Battle HUD button's and
        // the notice's words for it. A row: window, GUI Scale, Compact/Standard/Large's scale there, then the size each shows as.
        // GUI 1 to 4 at 1080p, 720p and 1440p; the suite's windows; the review's probe; and the Auto GUI Scale of the windows players
        // use most, a maximized 1080p or 1440p window (1920x1009, 2560x1369) among them.
        int[][] shownRows = {
            {1920,1080,1, 1,2,3, 0,1,2},{1920,1080,2, 2,3,3, 0,1,1},{1920,1080,3, 2,3,3, 0,1,1},{1920,1080,4, 2,3,3, 0,1,1},
            {1280,720,1, 1,2,2, 0,1,1},{1280,720,2, 1,2,2, 0,1,1},{1280,720,3, 1,1,1, 0,0,0},
            {2560,1440,1, 1,2,3, 0,1,2},{2560,1440,2, 2,3,4, 0,1,2},{2560,1440,3, 2,3,4, 0,1,2},{2560,1440,4, 2,3,4, 0,1,2},{2560,1440,5, 2,3,4, 0,1,2},{2560,1440,6, 2,3,3, 0,1,1},
            {854,480,1, 1,1,1, 0,0,0},{854,480,2, 1,1,1, 0,0,0},{960,540,1, 1,1,1, 0,0,0},{960,540,2, 1,1,1, 0,0,0},{1024,768,3, 1,1,1, 0,0,0},
            {1366,768,2, 1,2,2, 0,1,1},{1366,768,3, 1,2,2, 0,1,1},{1440,900,3, 1,2,2, 0,1,1},{1536,864,3, 1,2,2, 0,1,1},{1600,900,3, 1,2,2, 0,1,1},
            {1680,1050,4, 1,2,2, 0,1,1},{1920,1009,4, 1,2,2, 0,1,1},{2560,1369,5, 2,3,4, 0,1,2},{2560,1600,6, 2,3,4, 0,1,2},{3440,1440,6, 2,3,4, 0,1,2},
            {3840,2160,4, 4,5,6, 0,1,2},{3840,2160,9, 4,5,5, 0,1,1}};
        String shownBad = "";
        for (int[] s : shownRows) for (int z = 0; z < 3; z++) {
            int k = UiMath.hudScale(s[0],s[1],s[2],z), as = UiMath.hudShownAs(s[0],s[1],s[2],z);
            if (k != s[3+z] || as != s[6+z]) shownBad += (shownBad.isEmpty() ? "" : ", ") + s[0]+"x"+s[1]+" GUI "+s[2]+" "+UiMath.HUD_SIZE_NAMES[z]+" at "+k+"x shows as "+UiMath.HUD_SIZE_NAMES[as]+" (want "+s[3+z]+"x, "+UiMath.HUD_SIZE_NAMES[s[6+z]]+")";
        }
        check(shownBad.isEmpty(), "each choice shows as the smallest size hudScale draws at the same scale: Large as Standard at 1080p, 1440p and 4K at their Auto GUI Scale, Standard and Large as Compact at 720p GUI 3, 854x480, 960x540 and 1024x768; Large itself at 1440p GUI 2 to 5, 2560x1600, 3440x1440 and 4K GUI 4" + (shownBad.isEmpty() ? "" : "; wrong: " + shownBad));
        String asBad = ""; int asSeen = 0;
        for (int fw : new int[]{0,640,854,960,1280,1366,1600,1920,2560,3840}) for (int fh : new int[]{0,480,540,720,768,900,1080,1440,2160}) for (int gui : new int[]{0,1,2,3,4,6,9}) for (int z = 0; z < 3; z++) {
            int as = UiMath.hudShownAs(fw,fh,gui,z), k = UiMath.hudScale(fw,fh,gui,z); boolean least = true;
            for (int y = 0; y < as; y++) least &= UiMath.hudScale(fw,fh,gui,y) != k;
            if (as < 0 || as > z || UiMath.hudScale(fw,fh,gui,as) != k || !least || UiMath.hudShownAs(fw,fh,gui,as) != as) asBad += (asBad.isEmpty() ? "" : ", ") + fw+"x"+fh+" GUI "+gui+" size "+z+" as "+as;
            asSeen++;
        }
        check(asBad.isEmpty() && asSeen == 10*9*7*3 && UiMath.hudShownAs(1280,720,2,7) == UiMath.HUD_COMPACT && UiMath.hudShownAs(1280,720,2,-1) == UiMath.HUD_COMPACT && UiMath.hudShownAs(0,0,0,UiMath.HUD_LARGE) == UiMath.HUD_COMPACT, "on every window and GUI Scale a size shows as itself or the smallest size at its scale, never a larger one; an unknown size, or any on a 0 x 0 window, as Compact" + (asBad.isEmpty() ? "" : "; wrong: " + asBad.substring(0, Math.min(300, asBad.length()))));
        check(UiMath.hudSizeLabel(UiMath.HUD_LARGE,UiMath.HUD_STANDARD).equals("Battle HUD: Large (Standard size here)") && UiMath.hudSizeLabel(UiMath.HUD_STANDARD,UiMath.HUD_COMPACT).equals("Battle HUD: Standard (Compact scale here)") && UiMath.hudSizeLabel(UiMath.HUD_LARGE,UiMath.HUD_COMPACT).equals("Battle HUD: Large (Compact scale here)") && UiMath.hudSizeLabel(UiMath.HUD_LARGE,UiMath.HUD_LARGE).equals("Battle HUD: Large") && UiMath.hudSizeLabel(UiMath.HUD_STANDARD,UiMath.HUD_STANDARD).equals("Battle HUD: Standard") && UiMath.hudSizeLabel(UiMath.HUD_COMPACT,UiMath.HUD_COMPACT).equals("Battle HUD: Compact"), "the Battle HUD button names the choice, and where this window draws it smaller what it draws: Standard's size in full, or only Compact's scale (the Task 8c review's Minor A)");
        check(UiMath.hudSizeNotice(UiMath.HUD_LARGE,UiMath.HUD_STANDARD).equals("Battle HUD: Large. Your choice is saved. In this window it draws at Standard size.") && UiMath.hudSizeNotice(UiMath.HUD_STANDARD,UiMath.HUD_COMPACT).equals("Battle HUD: Standard. Your choice is saved. In this window it draws at Compact scale.") && UiMath.hudSizeNotice(UiMath.HUD_LARGE,UiMath.HUD_COMPACT).equals("Battle HUD: Large. Your choice is saved. In this window it draws at Compact scale.") && UiMath.hudSizeNotice(UiMath.HUD_STANDARD,UiMath.HUD_STANDARD).equals("Battle HUD: Standard. Your choice is saved.") && UiMath.hudSizeNotice(UiMath.HUD_COMPACT,UiMath.HUD_COMPACT).equals("Battle HUD: Compact. Your choice is saved."), "the notice says the choice is saved, and where this window draws it smaller, Standard's size or Compact's scale");
        check(UiMath.hudSizeLabel(UiMath.HUD_STANDARD,UiMath.hudShownAs(1280,720,3,UiMath.HUD_STANDARD)).equals("Battle HUD: Standard (Compact scale here)") && UiMath.hudSizeLabel(UiMath.HUD_LARGE,UiMath.hudShownAs(1280,720,3,UiMath.HUD_LARGE)).equals("Battle HUD: Large (Compact scale here)") && UiMath.hudSizeLabel(UiMath.HUD_LARGE,UiMath.hudShownAs(1920,1080,4,UiMath.HUD_LARGE)).equals("Battle HUD: Large (Standard size here)") && UiMath.hudSizeLabel(UiMath.HUD_STANDARD,UiMath.hudShownAs(1920,1080,4,UiMath.HUD_STANDARD)).equals("Battle HUD: Standard") && UiMath.hudSizeLabel(UiMath.HUD_LARGE,UiMath.hudShownAs(2560,1440,4,UiMath.HUD_LARGE)).equals("Battle HUD: Large"), "the button at 720p GUI 3 (Standard and Large at Compact's scale), 1080p GUI 4 (Large at Standard's) and 1440p GUI 4 (Large a step above Standard)");
        boolean roundTrip = UiMath.HUD_SIZE_NAMES.length == 3;
        for (int z = 0; z < UiMath.HUD_SIZE_NAMES.length; z++) roundTrip &= UiMath.hudSizeOf("{\"size\":\"" + UiMath.HUD_SIZE_NAMES[z] + "\"}") == z;
        check(roundTrip && UiMath.HUD_SIZE_NAMES[UiMath.HUD_COMPACT].equals("Compact") && UiMath.HUD_SIZE_NAMES[UiMath.HUD_STANDARD].equals("Standard") && UiMath.HUD_SIZE_NAMES[UiMath.HUD_LARGE].equals("Large") && UiMath.hudSizeOf(" {\n  \"size\" : \"Large\"\n}\n") == UiMath.HUD_LARGE && UiMath.hudSizeOf("{\"seen\":true,\"size\":\"Standard\"}") == UiMath.HUD_STANDARD, "hud.json as HudSize writes it reads back as the size it names, with white space or another key too");
        check(UiMath.hudSizeOf(null) == 0 && UiMath.hudSizeOf("") == 0 && UiMath.hudSizeOf("{") == 0 && UiMath.hudSizeOf("{}") == 0 && UiMath.hudSizeOf("{\"size\":\"Large\"") == 0 && UiMath.hudSizeOf("\"size\":\"Large\"}") == 0 && UiMath.hudSizeOf("{\"size\":\"Large\"}}") == 0 && UiMath.hudSizeOf("{\"size\":Large}") == 0 && UiMath.hudSizeOf("{\"size\":2}") == 0 && UiMath.hudSizeOf("{\"size\":{\"name\":\"Large\"}}") == 0 && UiMath.hudSizeOf("[\"Large\"]") == 0 && UiMath.hudSizeOf("Large") == 0 && UiMath.hudSizeOf("{\"sizes\":\"Large\"}") == 0 && UiMath.hudSizeOf("{\"size\":\"Huge\"}") == 0 && UiMath.hudSizeOf("{\"size\":\"large\"}") == 0 && UiMath.hudSizeOf("{\"size\":\"\"}") == 0, "no hud.json (null), a garbled one or a size it does not know reads as Compact (HudSize.reload)");
        check(UiMath.hudSizeOf(UiMath.BOM + "{\"size\":\"Large\"}") == UiMath.HUD_LARGE && UiMath.hudSizeOf(UiMath.BOM + " {\r\n  \"size\": \"Standard\"\r\n}\r\n") == UiMath.HUD_STANDARD && UiMath.hudSizeOf(String.valueOf(UiMath.BOM)) == UiMath.HUD_COMPACT && UiMath.hudSizeOf("{\"size\":\"" + UiMath.BOM + "Large\"}") == UiMath.HUD_COMPACT, "a hud.json saved with a byte order mark (Notepad's UTF-8) reads as the size it names, as gson read it; a mark inside the name is no name (the Task 8c review's Minor C)");
        check(UiMath.roomOpen(false,false) && UiMath.roomOpen(false,true) && UiMath.roomOpen(true,true) && !UiMath.roomOpen(true,false) && UiMath.roomWhy(false,false).isEmpty() && UiMath.roomWhy(true,true).isEmpty() && UiMath.roomWhy(true,false).equals(UiMath.ROOM_LEADER_ONLY) && UiMath.ROOM_LEADER_ONLY.equals("Your party leader makes or joins rooms."), "a commander alone or leading makes and joins rooms; a party member who does not lead cannot, and the room's note says why in words, not by the dull tags alone");
        // the room gate's truth table, row by row {in a party, leads it, may make or join a room}: the same rows CampRulesCheck pins for
        // the server's rule (SocialHub.lead), so a change to either side fails its own check (8b review Minor 5)
        boolean[][] roomRows = {{false, false, true}, {true, true, true}, {true, false, false}};
        boolean roomRowsOk = true;
        for (boolean[] row : roomRows) roomRowsOk &= UiMath.roomOpen(row[0], row[1]) == row[2] && UiMath.roomWhy(row[0], row[1]).isEmpty() == row[2];
        check(roomRowsOk, "the client's room gate gives the server's rows: alone yes, leading yes, a member who does not lead no");
        check(UiMath.chipBar(0.75f)==48 && UiMath.chipBar(1f)==64 && UiMath.chipBar(1.4f)==64 && UiMath.chipBar(-1f)==0 && UiMath.chipBar(Float.NaN)==0 && UiMath.chipBar(0f)==0, "a chip's strength bar of 64 px, clamped; NaN (a group with nothing to measure) draws none");
        // The column at the mock's 960 x 540 (Compact, k 1): its foot 18 px above the bottom, plates 4 px apart.
        var groups = java.util.List.of("infantry","ranged","support");
        var cards = UiMath.hudLayout(960,540,1,1,UiMath.HUD_COMPACT,new UiMath.HudState(3,groups,"ranged",false,0,false,true));
        check(java.util.Arrays.equals(cards.get("card-infantry"),new int[]{24,416,176,18}) && java.util.Arrays.equals(cards.get("card-ranged"),new int[]{24,438,176,62}) && java.util.Arrays.equals(cards.get("card-support"),new int[]{24,504,176,18})
            && java.util.Arrays.equals(cards.get("spine"),new int[]{12,412,9,114}) && !cards.containsKey("keys") && !cards.containsKey("hints") && java.util.Arrays.equals(cards.get("feed"),new int[]{778,248,166,44}), "ranged selected: its ledger card 62 tall between two chips 18 tall, the spine 4 px past either end, Compact's feed leaf at the right");
        var army = UiMath.hudLayout(960,540,1,1,UiMath.HUD_COMPACT,new UiMath.HudState(3,groups,"all",true,2,true,true));
        check(java.util.Arrays.equals(army.get("card-infantry"),new int[]{24,440,176,18}) && java.util.Arrays.equals(army.get("card-ranged"),new int[]{24,462,176,18}) && java.util.Arrays.equals(army.get("card-support"),new int[]{24,484,176,18}) && java.util.Arrays.equals(army.get("keys"),new int[]{24,506,176,16})
            && java.util.Arrays.equals(army.get("spine"),new int[]{12,436,9,90}) && java.util.Arrays.equals(army.get("hints"),new int[]{24,392,164,40}) && java.util.Arrays.equals(army.get("lesson"),new int[]{12,72,280,33}) && java.util.Arrays.equals(army.get("respawn"),new int[]{380,230,200,28}), "the army selected: three chips over the key strip; the hint slip 48 above the top chip; the lesson note at the top left; the respawn card 40 above the middle");
        var small = UiMath.hudLayout(854,480,2,1,UiMath.HUD_COMPACT,UiMath.DEFAULT_STATE);
        check(UiMath.columnBottom(854,480,2,1)==430 && java.util.Arrays.equals(small.get("keys"),new int[]{24,414,176,16}) && UiMath.columnBottom(960,540,1,1)==522 && UiMath.columnBottom(1920,1080,4,2)==522, "at 854x480 GUI 2 the offhand slot (canvas x 186, y 434) reaches into the column: its foot rises to 430, 4 px above it; elsewhere 18 px above the bottom");
        var none = UiMath.hudLayout(960,540,1,1,UiMath.HUD_COMPACT,new UiMath.HudState(3,java.util.List.of(),"all",true,0,false,false));
        var nobody = UiMath.hudLayout(960,540,1,1,UiMath.HUD_COMPACT,new UiMath.HudState(3,groups,"none",false,0,false,false));
        check(!none.containsKey("spine") && !none.containsKey("keys") && java.util.Arrays.equals(none.get("hints"),new int[]{24,474,164,40}) && nobody.get("card-ranged")[3]==18 && !nobody.containsKey("keys") && UiMath.hudLayout(0,0,0,0,0,UiMath.DEFAULT_STATE).containsKey("score"), "no group fielded: no spine and no key strip, the hint slip above the column's foot; nothing selected: three chips; a 0 x 0 window divides by nothing");
        // The lesson note: 11 px Lato, 264 px a line after its tag, never cut.
        java.util.function.ToIntFunction<String> four = s -> 4*s.length();
        var two = UiMath.lessonLines("Press B to change formation. Shieldwall holds a line; Wedge breaks one.",58,264,four);
        check(two.equals(java.util.List.of("Press B to change formation. Shieldwall holds a","line; Wedge breaks one.")) && String.join(" ",two).equals("Press B to change formation. Shieldwall holds a line; Wedge breaks one."), "the first line is the tag's width narrower; the copy wraps at its spaces and joins back whole");
        check(UiMath.lessonLines("Unbreakableline here",0,40,four).equals(java.util.List.of("Unbreakableline","here")) && UiMath.lessonLines("  ",58,264,four).isEmpty() && UiMath.lessonLines("a  b",0,264,four).equals(java.util.List.of("a b")), "a word too long for a line stands alone, uncut; spaces collapse");
        check(UiMath.lessonHeight(2)==33 && UiMath.lessonHeight(1)==22 && UiMath.lessonHeight(0)==22 && UiMath.lessonHeight(3)==44, "the note: 11 px a line and 11 more");
        check(UiMath.lessonYields(new int[]{12,72,280,33},new int[]{220,110,200,28}) && UiMath.lessonYields(new int[]{12,72,280,33},new int[]{220,114,200,28}) && !UiMath.lessonYields(new int[]{12,72,280,33},new int[]{220,115,200,28}) && !UiMath.lessonYields(new int[]{12,72,280,33},new int[]{292,90,200,28}) && UiMath.lessonYields(new int[]{12,72,280,33},new int[]{100,60,200,28}), "the note gives way to the respawn card where their columns meet and the card's top edge (a row above its box) comes within 4 rows of the note's shadow, which ends at y 110: at y 114, not 115; or over it");
        check(UiMath.crosshairTop(540,1,1)==262 && UiMath.crosshairTop(270,4,2)==254 && UiMath.crosshairTop(240,2,1)==224 && UiMath.crosshairTop(240,3,1)==336, "vanilla's crosshair in canvas px");
        check(UiMath.respawnY(540,262)==230 && UiMath.respawnY(384,184)==152 && UiMath.respawnY(540,254)==222 && UiMath.respawnY(480,224)==192 && UiMath.respawnY(720,336)==304, "the respawn card 40 px above the middle, raised 8 or 16 px where a larger GUI Scale's crosshair comes within 4 px");
        check(UiMath.inksApart("feed",new int[]{200,120,166,44},"lesson",new int[]{12,72,280,33}) && !UiMath.inksApart("feed",new int[]{200,112,166,44},"lesson",new int[]{12,72,280,33}) && UiMath.inksApart("feed",new int[]{292,90,166,44},"lesson",new int[]{12,72,280,33}), "the feed leaf's top edge (3 rows above its box) keeps 4 rows from the note's shadow where they share columns (the 8a review's Important 2)");
        var feedOnLesson = UiMath.hudLayout(960,540,1,1,UiMath.HUD_COMPACT,new UiMath.HudState(3,groups,"all",false,2,false,true)); feedOnLesson.put("feed",new int[]{200,108,166,44});
        check(UiMath.frameFaults(feedOnLesson,960,540,new java.util.HashMap<>()).size()==1 && UiMath.frameFaults(army,960,540,UiMath.namedBars(960,540,1,1)).isEmpty(), "frameFaults finds a feed leaf hung in the note's shadow, and nothing in the fullest Compact frame at 960 x 540");
        check(java.util.Arrays.equals(UiMath.feedBox(960,540,UiMath.HUD_COMPACT),new int[]{778,248,166,44}) && java.util.Arrays.equals(UiMath.feedBox(960,540,UiMath.HUD_STANDARD),new int[]{772,232,172,74}) && UiMath.feedTextW(UiMath.HUD_COMPACT)==144 && UiMath.feedTextW(UiMath.HUD_LARGE)==148 && UiMath.feedRowY(UiMath.HUD_COMPACT,2)==31 && UiMath.feedRowY(UiMath.HUD_STANDARD,4)==57, "the feed: Compact's 166 x 44 leaf of three entries, text 144 px; the five-entry 172 x 74 leaf at Standard and Large, as UI Task 3 hung it");
        // The footprint budget (the brief's item 5): the plates' share of the screen in the default battle state, Compact against the
        // HUD before this task (Task 8a's cards, 48 + 48 + 62 tall, and 172 x 74 feed slip, at its scale: 1, 2, 2, 2, 1), at most half;
        // and the centre band clear. Hand-worked: before, 40536 canvas px; Compact, 3 x 176 x 18 + 176 x 16 + 166 x 44 = 19624.
        int[][] five = {{1280,720,3,1},{1366,768,2,2},{1920,1080,4,2},{1920,1080,2,2},{854,480,2,1}};
        String shares = ""; boolean budget = true;
        for (int[] s : five) {
            double before = UiMath.footprint(hud(s[0]/s[3],s[1]/s[3],-1,-1,false),s[3],s[0],s[1]);
            int k = UiMath.hudScale(s[0],s[1],s[2],UiMath.HUD_COMPACT); var r = UiMath.hudLayout(s[0],s[1],s[2],k,UiMath.HUD_COMPACT,UiMath.DEFAULT_STATE);
            double now = UiMath.footprint(r,k,s[0],s[1]); var band = UiMath.inBand(r,s[0]/k,s[1]/k);
            budget &= now <= before / 2 && band.isEmpty() && Math.abs(before*s[0]*s[1]/(s[3]*s[3])-40536)<0.5 && Math.abs(now*s[0]*s[1]/(k*k)-19624)<0.5;
            shares += (shares.isEmpty() ? "" : ", ") + String.format(java.util.Locale.ROOT,"%dx%d GUI %d %.3f%% before, %.3f%% now (%.0f%%)%s",s[0],s[1],s[2],100*before,100*now,100*now/before,band.isEmpty()?"":" band "+band);
        }
        check(budget, "Compact covers at most half the screen the HUD covered before this task, the centre band clear: " + shares);
        check(java.util.Arrays.equals(UiMath.centreBand(960,540),new int[]{288,162,384,173}) && UiMath.inBand(army,960,540).equals(java.util.List.of()) && UiMath.inBand(java.util.Map.of("feed",new int[]{400,200,10,10},"respawn",new int[]{380,230,200,28},"score",new int[]{300,200,10,10}),960,540).equals(java.util.List.of("feed")), "the centre band (40% of the width, 30% to 62% of the height): a plate in it is named, the respawn card is let in, the ledger is no plate");
        // The lifted chat over the Compact HUD, closed/open with vanilla's widest chat (right edge GUI 332, 9 px lines, 10 and 20 lines),
        // in the suite's four passes: A the lesson and respawn cards with the hint layer down, B the lesson with the hint layer up, C a
        // battle with neither (the default state), D a battle's first seconds (the hint layer up). 1280x720 GUI 3 in A worked by hand:
        // the column's top at 616 would put the chat's bottom at GUI 201, below vanilla's own 200, so it starts there; closed, it
        // moves up past the hearts (201) and the armour row (191) to 187, and 6 lines fit above it and below the crosshair (127, 4 px
        // clear); open, vanilla's bars do not count, but the ability slot beside the hotbar does (GUI 193, UI Task 8b-2): the bottom
        // moves up to 189, and 8 fit below the respawn card's shadow (GUI 111).
        String[][] wantChat = {{"10/20","10/20","10/20","10/20"},{"4/6","1/9","4/14","1/12"},{"6/8","6/16","6/18","6/17"},{"10/20","10/20","10/20","10/20"},{"7/9","5/15","7/19","5/17"},{"10/14","10/20","10/20","10/20"},{"10/15","10/20","10/20","10/20"}};
        String chats = ""; boolean chatsOk = true, clean = true;
        for (int i = 0; i < win.length; i++) for (int p = 0; p < 4; p++) {
            int[] s = win[i]; int k = UiMath.hudScale(s[0],s[1],s[2],UiMath.HUD_COMPACT);
            var r = UiMath.hudLayout(s[0],s[1],s[2],k,UiMath.HUD_COMPACT,new UiMath.HudState(3,groups,"all",p==1||p==3,p<2?2:0,p==0,true));
            String got = chat(r,new int[]{s[0],s[1],s[2],k}); chatsOk &= got.equals(wantChat[i][p]);
            clean &= UiMath.frameFaults(r,s[0]/k,s[1]/k,UiMath.namedBars(-Math.floorDiv(-s[0],s[2]),-Math.floorDiv(-s[1],s[2]),s[2],k)).isEmpty() && r.containsKey("lesson") == (p < 2);
            if (!got.equals(wantChat[i][p])) chats += (chats.isEmpty() ? "" : ", ") + s[0]+"x"+s[1]+" GUI "+s[2]+" pass "+"ABCD".charAt(p)+" "+got+" (want "+wantChat[i][p]+")";
        }
        check(chatsOk, "the lifted chat's lines over the Compact HUD in each pass at each window" + (chats.isEmpty() ? "" : ": " + chats));
        // The open chat's box (GUI x 0 to 332, its lines above its bottom) never meets the ability slot beside the hotbar (UI Task
        // 8b-2: at 1280x720 GUI 3 vanilla's open chat, bottom 200, reached the slot at GUI 193..225 x 317..367), in any pass or size.
        String onAbility = "";
        for (int[] s : win) for (int z = UiMath.HUD_COMPACT; z <= UiMath.HUD_LARGE; z++) for (int p = 0; p < 4; p++) {
            int k = UiMath.hudScale(s[0],s[1],s[2],z), sw = -Math.floorDiv(-s[0],s[2]), sh = -Math.floorDiv(-s[1],s[2]);
            var r = UiMath.hudLayout(s[0],s[1],s[2],k,z,new UiMath.HudState(3,groups,"all",p==1||p==3,p<2?2:0,p==0,true));
            r.put("ability", UiMath.abilityRect(sw,sh,s[2],k));
            for (int open = 0; open < 2; open++) {
                var slot = UiMath.chatSlot(r,k/(double)s[2],sw,sh,UiMath.CHAT_RIGHT,9,open==1?20:10,open==1);
                int[] box = {0,slot.bottom()-9*slot.lines(),(int)UiMath.CHAT_RIGHT,9*slot.lines()};
                if (UiMath.meets(box,new int[]{sw/2+104,sh-47,50,32})) onAbility += (onAbility.isEmpty() ? "" : ", ") + s[0]+"x"+s[1]+" GUI "+s[2]+" size "+z+" pass "+"ABCD".charAt(p)+(open==1?" open":" closed")+" bottom "+slot.bottom();
            }
        }
        check(onAbility.isEmpty(), "the chat, closed and open, keeps off the ability slot at every window, size and pass" + (onAbility.isEmpty() ? "" : ": " + onAbility));
        // The camp's slips (UI Task 8b-2: the bare ivory hint on the courtyard's stone read 3.6:1): each slip's PNG, shadow and all, 12
        // px inside the HUD canvas at every window and size, off vanilla's bars and off the closed chat's ten lines (GUI x 0 to 332, up
        // from vanilla's bottom at sh - 40); the slip's words in SEPIA read 4.5:1 or better on its darkest paper, the chip's in
        // CHIP_TEXT on its ink.
        String campBad = "";
        for (int[] s : win) for (int z = UiMath.HUD_COMPACT; z <= UiMath.HUD_LARGE; z++) {
            int k = UiMath.hudScale(s[0],s[1],s[2],z), cw = s[0]/k, ch = s[1]/k, sw = -Math.floorDiv(-s[0],s[2]), sh = -Math.floorDiv(-s[1],s[2]);
            var bars = UiMath.namedBars(sw,sh,s[2],k); bars.put("the closed chat", UiMath.guiToCanvas(new int[]{0,sh-40-90,332,90},s[2],k));
            // the opt-in items right of the hotbar (the fix round's Minor 3): a left-handed player's offhand slot, the Hotbar-mode attack indicator
            bars.put("the right-hand offhand slot", UiMath.guiToCanvas(new int[]{sw/2+91,sh-23,29,24},s[2],k));
            bars.put("the attack indicator", UiMath.guiToCanvas(new int[]{sw/2+97,sh-20,18,18},s[2],k));
            for (var e : UiMath.campHud(s[0],s[1],s[2],k).entrySet()) {
                int[] b = e.getValue(), ink = {b[0],b[1],169,46};
                if (ink[0] < 12 || ink[1] < 12 || ink[0]+ink[2] > cw-12 || ink[1]+ink[3] > ch-12) campBad += "; " + s[0]+"x"+s[1]+" GUI "+s[2]+" size "+z+" "+e.getKey()+" within 12 px of the edge";
                for (var bar : bars.entrySet()) if (UiMath.meets(ink,bar.getValue())) campBad += "; " + s[0]+"x"+s[1]+" GUI "+s[2]+" size "+z+" "+e.getKey()+" "+java.util.Arrays.toString(ink)+" meets "+bar.getKey();
            }
        }
        check(campBad.isEmpty() && UiMath.contrast(0xff5c4a32,0xffd6c69c) >= 4.5 && UiMath.contrast(0xfff1e7cb,0xff2a2219) >= 4.5, "the camp's slips 12 px inside, off vanilla's bars and the chat, their words 4.5:1 on the paper" + campBad);
        check(clean, "every Compact frame of the four passes at the suite's windows: every plate 12 px inside, none on another or on vanilla's bars, the lesson note beside the respawn card (it never has to give way at Compact)");
        // The closed chat off vanilla's bars (UI Task 8c): at 1280x720 GUI 3 the compact column (top at canvas 616, GUI 205) would
        // leave the chat at vanilla's own bottom, GUI 200, its lowest line on the armour row (191 to 200) under the hearts (201).
        var def720 = UiMath.hudLayout(1280,720,3,1,UiMath.HUD_COMPACT,UiMath.DEFAULT_STATE);
        check(UiMath.chatSlot(def720,1/3.0,427,240,332,9,10,false).bottom()==187 && UiMath.chatSlot(def720,1/3.0,427,240,332,9,20,true).bottom()==200 && UiMath.chatSlot(def720,1/3.0,427,240,90,9,10,false).bottom()==200, "closed, the chat keeps 4 px above vanilla's armour row where its width reaches it (1280x720 GUI 3, the default battle: bottom 187, not vanilla's 200); open, or a chat too narrow to reach the bars (right edge 90 < the offhand slot at 93), it keeps vanilla's 200");
        // Standard and Large (the brief's item 6), as the ui-hud sweep draws them: every frame of the four passes at the suite's
        // windows, each selection, clean, and the chat, closed and open, with a line on the screen. hudFits vets a step above the
        // kit scale; this also covers the windows where no step fits and Standard and Large draw at the kit scale with their leaf.
        String sized = ""; int sizedFrames = 0;
        for (int[] s : win) for (int z = UiMath.HUD_STANDARD; z <= UiMath.HUD_LARGE; z++) {
            int k = UiMath.hudScale(s[0],s[1],s[2],z), sw = -Math.floorDiv(-s[0],s[2]), sh = -Math.floorDiv(-s[1],s[2]);
            for (int p = 0; p < 4; p++) for (String sel : new String[]{"all","infantry","ranged","support"}) {
                var r = UiMath.hudLayout(s[0],s[1],s[2],k,z,new UiMath.HudState(3,groups,sel,p==1||p==3,p<2?2:0,p==0,true));
                var faults = UiMath.frameFaults(r,s[0]/k,s[1]/k,UiMath.namedBars(sw,sh,s[2],k));
                boolean onScreen = UiMath.chatSlot(r,k/(double)s[2],sw,sh,UiMath.CHAT_RIGHT,9,10,false).onScreen() && UiMath.chatSlot(r,k/(double)s[2],sw,sh,UiMath.CHAT_RIGHT,9,20,true).onScreen();
                sizedFrames++;
                if (!faults.isEmpty() || !onScreen) sized += (sized.isEmpty() ? "" : "; ") + s[0]+"x"+s[1]+" GUI "+s[2]+" "+(z == UiMath.HUD_STANDARD ? "Standard" : "Large")+" at "+k+"x, pass "+"ABCD".charAt(p)+", "+sel+" selected: "+(onScreen ? "" : "the chat off the screen ")+faults;
            }
        }
        check(sized.isEmpty() && sizedFrames == win.length*2*4*4, "every Standard and Large frame of the four passes at the suite's windows, each selection: every plate 12 px inside, none on another or on vanilla's bars, the chat closed and open on the screen (UI Task 8c, item 6)" + (sized.isEmpty() ? "" : ": " + sized.substring(0, Math.min(400, sized.length()))));
        check(UiMath.chatSlot(hud(683,384,216,152,false),1,683,384,332,9,10,false).bottom()==83 && UiMath.chatSlot(hud(960,540,280,-1,true),0.5,480,270,332,9,10,false).bottom()==123, "the chat moves up past the respawn and lesson cards (Task 8a's 1366x768 GUI 2: bottom 83) and past the crosshair (1080p GUI 4, hint layer up: bottom 123)");
        var feedOnly = new java.util.LinkedHashMap<String,int[]>(); feedOnly.put("spine",new int[]{12,300,9,100}); feedOnly.put("feed",new int[]{100,250,172,74});
        check(UiMath.chatSlot(feedOnly,1,960,540,332,9,10,true).bottom()==243 && UiMath.chatSlot(hud(960,540,-1,-1,false),1,960,540,332,9,10,false) != null && UiMath.chatSlot(new java.util.HashMap<>(),1,960,540,332,9,10,false) == null && UiMath.columnTop(java.util.Map.of("keys",new int[]{24,506,176,16})) == 506, "the feed slip counts over the chat; the key strip is the column; no column, no lift");
        check(UiMath.chatSlot(10,0,java.util.List.of(),0).lines()==1 && UiMath.chatSlot(10,100,java.util.List.of(new double[]{50,60,60}),0).lines()==10 && !UiMath.chatSlot(10,20,java.util.List.of(new double[]{5,30,30}),9).onScreen() && UiMath.chatSlot(10,20,java.util.List.of(new double[]{5,30,30}),9).bottom()==20, "a line height of 0 divides by nothing; a push off the screen keeps the first place");
        // Short key names for crowded rows (the re-review's 'Pag...'), by the key's code; the long names only where the row is crowded.
        check("MB4".equals(HintText.mouseShort(3)) && "RMB".equals(HintText.mouseShort(1)) && "RShift".equals(HintText.keyShort(344)) && "PgUp".equals(HintText.keyShort(266)) && "PgDn".equals(HintText.keyShort(267)) && "Caps".equals(HintText.keyShort(280)) && "LAlt".equals(HintText.keyShort(342)) && "Num4".equals(HintText.keyShort(324)) && "Num.".equals(HintText.keyShort(330)) && HintText.keyShort(65)==null && HintText.UNBOUND_SHORT.equals("None"), "short names by GLFW code");
        check(HintText.shorter("Keypad 4","Num4").equals("Num4") && HintText.shorter("4","Num4").equals("4") && HintText.shorter("K",null).equals("K") && HintText.shorter("Right Shift","RShift").equals("RShift"), "a short name only where it is shorter");
        var map2 = java.util.List.of("Map","Commands");
        java.util.List<String> row1 = UiMath.fitKeys(java.util.List.of("Not Bound","Right Shift"),java.util.List.of("None","RShift"),map2,154,8,mc), row2 = UiMath.fitKeys(java.util.List.of("Page Up","Page Down","Left Alt"),java.util.List.of("PgUp","PgDn","LAlt"),turn,154,8,mc);
        check(row1.equals(java.util.List.of("None","RShift")) && UiMath.hintRowWidth(row1,map2,mc)==139 && row2.equals(java.util.List.of("PgUp","PgDn","LAlt")) && UiMath.hintRowWidth(row2,turn,mc)==141, "crowded slip rows take the short names: None, RShift; PgUp, PgDn, LAlt");
        check(UiMath.fitKeys(java.util.List.of("Not Bound","K"),java.util.List.of("None","K"),map2,154,8,mc).equals(java.util.List.of("Not Bound","K")), "a row that fits keeps the game's names");
        UiMath.CardRow capsShort = UiMath.cardRow(java.util.List.of("Caps Lock","V","B"),java.util.List.of("Caps","V","B"),orders,ordersShort,mc), threeShort = UiMath.cardRow(java.util.List.of("Right Shift","Left Alt","Page Down"),java.util.List.of("RShift","LAlt","PgDn"),orders,ordersShort,mc);
        check(capsShort.keys().equals(java.util.List.of("Caps","V","B")) && capsShort.words().equals(ordersShort) && capsShort.at().equals(java.util.List.of(10,74,119)) && threeShort.keys().equals(java.util.List.of("R...","L...","P...")) && UiMath.hintRowWidth(threeShort.keys(),ordersShort,UiMath.CARD_ROW_GAP,mc)==145, "a tall card's crowded row: Caps whole; three long keys still give way, ending by x 166");
        check(UiMath.cardRow(java.util.List.of("G","V","B"),java.util.List.of("G","V","B"),orders,ordersShort,mc).at().equals(java.util.List.of(10,58,105)), "the default keys keep the mock's places");
        // text scale (4F.5): every common window at every GUI Scale 1..6 draws the pages at a whole scale the player's GUI Scale allows, and the
        // page scale never leaves the compact page (FILL_W x FILL_H) short of room, nor the HUD below the kit scale or above Large's two steps more
        int[][] windows = {{854,480},{1024,576},{1280,720},{1366,768},{1600,900},{1920,1080},{2560,1440},{3440,1440},{3840,2160}};
        for (int[] sz : windows) for (int gui = 1; gui <= 6; gui++) {
            String at = sz[0] + "x" + sz[1] + " at GUI " + gui;
            int px = UiMath.pixelScale(sz[0], sz[1], gui), pg = UiMath.pageScale(sz[0], sz[1], gui);
            check(px >= 1 && px <= gui, "pixelScale is 1..GUI Scale at " + at);
            check(pg >= px && pg <= gui, "pageScale holds between pixelScale and the GUI Scale at " + at);
            check(-Math.floorDiv(-sz[0], pg) >= UiMath.FILL_W && -Math.floorDiv(-sz[1], pg) >= UiMath.FILL_H, "the page canvas at " + at + " (x" + pg + ") holds the " + UiMath.FILL_W + "x" + UiMath.FILL_H + " compact page");
            for (int size = UiMath.HUD_COMPACT; size <= UiMath.HUD_LARGE; size++) {
                int hk = UiMath.hudScale(sz[0], sz[1], gui, size);
                check(hk >= px && hk <= px + 2, "hudScale of size " + size + " stays within the kit scale and two steps more at " + at);
            }
        }
        check(UiMath.pageScale(2560, 1440, 3) == 3 && UiMath.pageScale(2560, 1440, 2) == 2 && UiMath.pageScale(3840, 2160, 3) == 3, "the 3x War Table scale holds at 1440p and 4K when the GUI Scale is 3");
        if (failed > 0) { System.out.println("UiMathCheck: " + failed + " checks FAILED, " + checks + " passed"); System.exit(1); }
        System.out.println("UiMathCheck: " + checks + " checks passed");
    }

    /** The battle HUD's boxes in canvas px on a w x h canvas with three points as Task 8a laid them out, before UI Task 8c (the
     *  mock's numbers, not UiMath's helpers): the lesson card of two lines at x lessonX (-1: none), the respawn card at y respawnY
     *  (-1: none), the hint layer up (three tall cards and the hint slip) or down (the army selected: the bottom card tall), and the
     *  feed slip. The footprint budget measures the HUD before this task by it. */
    static java.util.Map<String,int[]> hud(int w, int h, int lessonX, int respawnY, boolean hint) {
        var r = new java.util.LinkedHashMap<String,int[]>(); int cx = w / 2;
        r.put("score", new int[]{cx-128,13,256,24}); r.put("dawn-boss", new int[]{cx-140,12,27,28}); r.put("dusk-boss", new int[]{cx+115,12,27,28});
        for (int i = 0; i < 3; i++) r.put("seal-"+i, new int[]{cx-28+28*i-13,37,27,31});
        if (lessonX >= 0) r.put("lesson", new int[]{lessonX,88,400,51});
        if (respawnY >= 0) r.put("respawn", new int[]{cx-100,respawnY,200,28});
        r.put("feed", new int[]{w-188,h/2-38,172,74});
        int[] hs = hint ? new int[]{62,62,62} : new int[]{48,48,62}; int bottom = h - 18, first = 0;
        for (int i = 2; i >= 0; i--) { first = bottom - hs[i]; r.put("card-"+i, new int[]{24,first,176,hs[i]}); bottom = first - 8; }
        r.put("spine", new int[]{12,first-4,9,h-18-first+8});
        if (hint) r.put("hints", new int[]{24,first-48,176,40});
        return r;
    }

    /** The chat's lines over those boxes and the ability slot, "closed/open", in a fbW x fbH window (s[0], s[1]) at GUI s[2] with the
     *  HUD at s[3]x. */
    static String chat(java.util.Map<String,int[]> r, int[] s) {
        int sw = -Math.floorDiv(-s[0],s[2]), sh = -Math.floorDiv(-s[1],s[2]); double f = s[3] / (double) s[2];
        r = new java.util.LinkedHashMap<>(r); r.put("ability", UiMath.abilityRect(sw,sh,s[2],s[3]));
        return UiMath.chatSlot(r,f,sw,sh,332,9,10,false).lines()+"/"+UiMath.chatSlot(r,f,sw,sh,332,9,20,true).lines();
    }
}
