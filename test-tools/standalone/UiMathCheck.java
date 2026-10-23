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
        check("LMB".equals(HintText.mouseChip(0)) && "RMB".equals(HintText.mouseChip(1)) && "MMB".equals(HintText.mouseChip(2)) && HintText.mouseChip(3) == null && HintText.mouseChip(-1) == null, "mouse chips by the button's code, in every language; other buttons keep the game's name");
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
        for(String kind:new String[]{"point","loss","kill","down","up","rout","regroup","deploy","notice"}) for(int t=0;t<2;t++) for(int v=0;v<2;v++) check(UiMath.contrast(FeedLines.style(kind,t,v).argb(),0xffdccb9f)>=5.2, "feed ink "+kind+" (team "+t+", seen by "+v+") holds the spec's 5.2:1 on the slip");
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
        check(UiMath.thousands(2870).equals("2,870") && UiMath.thousands(7500).equals("7,500") && UiMath.thousands(250).equals("250") && UiMath.thousands(0).equals("0"), "Chapter numbers with thousands separators");
        check(UiMath.trackFirst(11,30,10)==10 && UiMath.trackFirst(12,30,10)==11 && UiMath.trackFirst(1,30,10)==1 && UiMath.trackFirst(2,30,10)==1 && UiMath.trackFirst(31,30,10)==21, "the track starts at the last claim before the first one left, within tiers 1 and 21");
        check(UiMath.trackPage(10,-1,30,10)==1 && UiMath.trackPage(10,1,30,10)==20 && UiMath.trackPage(20,1,30,10)==21 && UiMath.trackPage(1,-1,30,10)==1, "the arrows page the track by ten, within its ends");
        check(UiMath.heroTier(10,10,30,5)==20 && UiMath.heroTier(11,10,30,5)==25 && UiMath.heroTier(1,10,30,5)==15 && UiMath.heroTier(21,10,30,5)==30, "the hero is the first title past the track's right end, else the last tier");
        check(UiMath.xpPoured(2870,250,30,360)==138 && UiMath.xpPoured(0,250,30,360)==0 && UiMath.xpPoured(9000,250,30,360)==360 && UiMath.xpPoured(-5,250,30,360)==0, "the XP rail pours 138 of 360 columns for 2,870 of 7,500 XP, as the approved track");
        check(UiMath.spellSmall("Complete 1 battle").equals("Complete one battle") && UiMath.spellSmall("Issue 10 tactical orders").equals("Issue ten tactical orders") && UiMath.spellSmall("Complete 6 battles this week").equals("Complete six battles this week")
            && UiMath.spellSmall("Hold 3.").equals("Hold three.") && UiMath.spellSmall("Win 25 battles").equals("Win 25 battles") && UiMath.spellSmall("1v1 in room AB12CD34").equals("1v1 in room AB12CD34") && UiMath.spellSmall("2.5 or 1,000 or 05").equals("2.5 or 1,000 or 05"), "a heading spells its counts to twenty (its old-style 1 and 0 read as I and O); codes, 1v1, decimals and larger numbers stay");
        // UI Task 8a: the battle HUD's scale, worked by hand from the boxes (the ledger follows GUI Scale up to 2 under 1600 px where it fits).
        check(UiMath.hudScale(1280,720,2)==2 && UiMath.hudScale(1366,768,2)==2 && UiMath.hudScale(1440,900,2)==2, "720p, 1366x768 and 1440x900 at GUI 2: the ledger at 2x");
        check(UiMath.hudScale(854,480,2)==1 && UiMath.hudScale(1024,768,2)==1 && UiMath.hudScale(960,540,1)==1 && UiMath.hudScale(1280,720,1)==1, "854x480 and 1024x768 at GUI 2 are too narrow for 2x (the lesson card would leave the canvas); GUI 1 stays 1");
        check(UiMath.hudScale(1280,720,3)==1 && !UiMath.hudFits(1280,720,3,2) && UiMath.hudScale(1366,768,3)==1 && UiMath.hudScale(1440,900,3)==1, "at GUI 3 the 2x bottom card would meet the hotbar (720p) or the offhand slot (1366x768, 1440x900): 1x");
        check(UiMath.hudScale(1536,768,3)==1 && UiMath.hudScale(1536,864,3)==2, "1536x768 at GUI 3: at 2x the tutorial's chat would be pushed past the lesson card, the seals and the score bar off the top: 1x; 1536x864 has the room");
        check(UiMath.hudScale(1920,1080,2)==2 && UiMath.hudScale(1920,1080,4)==2 && UiMath.hudScale(1920,1080,1)==1 && UiMath.hudScale(1600,900,2)==1 && UiMath.hudScale(1600,900,4)==1 && UiMath.hudScale(3840,2160,4)==4, "from 1600 px wide the kit scale, as the screens");
        int sizes = 0; String bad = "";
        for (int fw : new int[]{0,1,320,640,854,959,960,1024,1280,1366,1440,1599,1600,1920,2560,3840}) for (int fh : new int[]{0,1,240,480,540,720,768,900,1080,1440,2160}) for (int gui : new int[]{-1,0,1,2,3,4,6}) {
            int k = UiMath.hudScale(fw,fh,gui), g = Math.max(1,gui), kit = UiMath.pixelScale(fw,fh,g);
            if (k < 1 || k > g || fw >= 1600 && k != kit || k > Math.max(kit,2) || k > kit && !UiMath.hudFits(fw,fh,g,k)) bad += (bad.isEmpty() ? "" : ", ") + "hudScale("+fw+","+fh+","+gui+") = "+k;
            sizes++;
        }
        check(bad.isEmpty() && sizes == 16*11*7 && !UiMath.hudFits(0,0,2,2) && !UiMath.hudFits(1280,720,0,1) && !UiMath.hudFits(1280,720,2,0), "every size, a 0 x 0 window and GUI 0 included: a scale of 1 or more, no division by zero, never past the GUI Scale, and a scale above the kit's only where the HUD fits" + (bad.isEmpty() ? "" : "; wrong: " + bad.substring(0, Math.min(300, bad.length()))));
        check(UiMath.lessonX(480)==280 && UiMath.lessonX(320)==216 && UiMath.lessonX(416)==216 && UiMath.lessonX(417)==217 && UiMath.lessonHeight(1)==40 && UiMath.lessonHeight(2)==51, "the lesson card centred (x 280 at 960) unless that reaches the group column, whose edge is x 200");
        check(UiMath.crosshairTop(540,1,1)==262 && UiMath.crosshairTop(270,4,2)==254 && UiMath.crosshairTop(240,2,1)==224 && UiMath.crosshairTop(240,3,1)==336, "vanilla's crosshair in canvas px");
        check(UiMath.respawnY(540,262)==230 && UiMath.respawnY(384,184)==152 && UiMath.respawnY(540,254)==222 && UiMath.respawnY(480,224)==192 && UiMath.respawnY(720,336)==304, "the respawn card 40 px above the middle, raised 8 or 16 px where a larger GUI Scale's crosshair comes within 4 px");
        // A two-line lesson card's shadow ends at y 144 (88 + 51 + 5); the respawn card's PNG starts a row above its box.
        check(UiMath.lessonYields(2,140) && UiMath.lessonYields(2,148) && !UiMath.lessonYields(2,149) && !UiMath.lessonYields(2,152) && !UiMath.lessonYields(1,140) && UiMath.lessonYields(3,152) && !UiMath.lessonYields(2,230), "the lesson card gives way to the respawn card within 4 px of its shadow: 720p GUI 2 (y 140), not 1366x768 (152) or a one-line lesson");
        // The lifted chat, closed/open lines with vanilla's widest chat (right edge GUI 332, 9 px lines, 10 and 20 lines): A, the lesson and respawn
        // cards with the hint layer down (the last number: 0 where the lesson card gives way to the respawn card); B, the lesson with the hint layer
        // up and the commander up; C, neither card, the layer down. Hand-worked.
        int[][] at = {{960,540,1,1,280,230,1},{854,480,2,1,227,192,1},{1280,720,3,1,440,304,1},{1920,1080,2,2,280,230,1},{1920,1080,4,2,280,222,1},{1280,720,2,2,216,140,0},{1366,768,2,2,216,152,1}};
        String[] wantA = {"10/20","1/2","4/6","10/20","2/4","7/7","1/1"}, wantB = {"10/13","3/3","1/10","10/13","5/6","1/1","1/1"};
        for (int i = 0; i < at.length; i++) {
            int[] s = at[i]; int w2 = s[0]/s[3], h2 = s[1]/s[3];
            String a = chat(hud(w2,h2,s[6]==1?s[4]:-1,s[5],false),s), b = chat(hud(w2,h2,s[4],-1,true),s);
            check(UiMath.hudScale(s[0],s[1],s[2])==s[3] && a.equals(wantA[i]) && b.equals(wantB[i]), s[0]+"x"+s[1]+" GUI "+s[2]+" at "+s[3]+"x: the chat shows "+a+" lines with the respawn card"+(s[6]==1?" and the lesson card":", the lesson card giving way")+" (want "+wantA[i]+"), "+b+" with the hint layer up (want "+wantB[i]+")");
        }
        check(chat(hud(640,360,-1,-1,false),at[5]).equals("9/9") && chat(hud(683,384,-1,-1,false),at[6]).equals("10/12"), "in a battle with neither card at 2x: 9 lines at 720p, 10 (12 open) at 1366x768");
        check(UiMath.chatSlot(hud(683,384,216,152,false),1,683,384,332,9,10,false).bottom()==83 && UiMath.chatSlot(hud(960,540,280,-1,true),0.5,480,270,332,9,10,false).bottom()==123, "the chat moves up past the respawn and lesson cards (1366x768 GUI 2: bottom 83) and past the crosshair (1080p GUI 4, hint layer up: bottom 123)");
        var feedOnly = new java.util.LinkedHashMap<String,int[]>(); feedOnly.put("spine",new int[]{12,300,9,100}); feedOnly.put("feed",new int[]{100,250,172,74});
        check(UiMath.chatSlot(feedOnly,1,960,540,332,9,10,true).bottom()==243 && UiMath.chatSlot(hud(960,540,-1,-1,false),1,960,540,332,9,10,false) != null && UiMath.chatSlot(new java.util.HashMap<>(),1,960,540,332,9,10,false) == null, "the feed slip counts over the chat; no column, no lift");
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
        if (failed > 0) { System.out.println("UiMathCheck: " + failed + " checks FAILED, " + checks + " passed"); System.exit(1); }
        System.out.println("UiMathCheck: " + checks + " checks passed");
    }

    /** The battle HUD's boxes in canvas px on a w x h canvas with three points, as BattleHud lays them out (the mock's numbers, not
     *  UiMath's helpers): the lesson card of two lines at x lessonX (-1: none), the respawn card at y respawnY (-1: none), the hint
     *  layer up (three tall cards and the hint slip) or down (the army selected: the bottom card tall), and the feed slip. */
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

    /** The chat's lines over those boxes, "closed/open", in a fbW x fbH window (s[0], s[1]) at GUI s[2] with the HUD at s[3]x. */
    static String chat(java.util.Map<String,int[]> r, int[] s) {
        int sw = -Math.floorDiv(-s[0],s[2]), sh = -Math.floorDiv(-s[1],s[2]); double f = s[3] / (double) s[2];
        return UiMath.chatSlot(r,f,sw,sh,332,9,10,false).lines()+"/"+UiMath.chatSlot(r,f,sw,sh,332,9,20,true).lines();
    }
}
