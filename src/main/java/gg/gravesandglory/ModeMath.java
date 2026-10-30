package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure battle-mode arithmetic and copy (Phase 4 Task 1): wave counts, rule and size lines, how-to cells, siege strength,
 * Banner roles and ledger text. No Minecraft types, so it is checked by javac alone
 * (test-tools/standalone/ModeMathCheck.java) rather than by a client run.
 */
public final class ModeMath {
    private ModeMath() {}
    /** Troops the n-th wave rises: the base count plus perBanner for every banner past the first, scaled by the tier, rounded up. */
    public static int waveCount(int base,int banners,double perBanner,double tierScale) {
        return (int)Math.ceil(base*(1+perBanner*(Math.max(1,banners)-1))*tierScale-1e-9);
    }
    /** A rule line from its sheet template: {n} becomes n. */
    public static String ruleLine(String template,int n) {
        return template.replace("{n}",Integer.toString(n));
    }
    /** A how-to cell: lines split on |, trimmed, empty parts dropped, at most three. */
    public static List<String> howTo(String cell) {
        List<String> out=new ArrayList<>();
        for(String part:cell.split("\\|")){
            String line=part.trim();
            if(!line.isEmpty())out.add(line);
            if(out.size()==3)break;
        }
        return out;
    }
    /** The field card's size line: the side's size and tier, and for Siege the side it defends (Survival counts banners, not a v b). */
    public static String sizeLine(String battleMode,int size,String tierName,String side) {
        return switch(battleMode){
            case "survival" -> size+(size==1?" banner, ":" banners, ")+tierName+" waves";
            case "siege" -> size+" v "+size+", "+side+", "+tierName+(size==1?" bot":" bots");
            default -> size+" v "+size+", "+tierName+(size==1?" bot":" bots");
        };
    }
    /** The defenders' strength in percent: yard and keep show 100 less capture progress, the gate its health share, a fallen keep 0. */
    public static int siegeStrength(int stage,double progress,double hp,double maxHp) {
        return switch(stage){
            case 0,2 -> (int)Math.round(100*(1-progress));
            case 1 -> maxHp>0?(int)Math.round(100*hp/maxHp):0;
            default -> 0;
        };
    }
    /** One Banner role per banner, humans first. Raiders = max(1, round(banners*raidShare)), and every human banner is a raider; the raid slots the humans leave go to bots, then one guard, then holders. A lone banner with no human splits. */
    public static List<String> roles(int banners,int humans,double raidShare) {
        List<String> out=new ArrayList<>();
        if(banners==1&&humans==0){out.add("split");return out;}
        int raiders=Math.min(banners,Math.max(1,(int)Math.round(banners*raidShare)));
        raiders=Math.min(banners,Math.max(raiders,humans));
        for(int i=0;i<raiders;i++)out.add("raider");
        if(out.size()<banners)out.add("guard");
        while(out.size()<banners)out.add("holder");
        return out;
    }
    /** A ledger number in its mode's format: 1/3, 64%, or 12. */
    public static String ledgerText(String format,int value,int max) {
        return switch(format){
            case "fraction" -> value+"/"+max;
            case "percent" -> value+"%";
            default -> Integer.toString(value);
        };
    }
}
