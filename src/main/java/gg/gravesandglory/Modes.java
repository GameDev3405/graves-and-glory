package gg.gravesandglory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The battle modes this build ships, by id, and the sheet rows behind them (Phase 4 Task 2). A mode is added by a
 * factory here plus its modes row; nothing else in the round needs to know its name.
 */
public final class Modes {
    /** The mode a round opens on and every map hosts. */
    public static final String DOMINATION="domination";
    private static final Map<String,Function<Spec.ModesRow,BattleMode>> BUILDERS=new LinkedHashMap<>();
    static{register(DOMINATION,DominationMode::new);}
    private Modes(){}
    public static void register(String id,Function<Spec.ModesRow,BattleMode> factory){BUILDERS.put(id,factory);}
    public static boolean registered(String id){return BUILDERS.containsKey(id);}
    /** A mode's sheet row; an unknown id is a programming error, so the exception names it. */
    public static Spec.ModesRow row(String id){
        for(Spec.ModesRow row:Spec.MODES)if(row.id().equals(id))return row;
        throw new IllegalArgumentException("No modes row for "+id);
    }
    /** A fresh mode object for a round. */
    public static BattleMode create(String id){
        Function<Spec.ModesRow,BattleMode> factory=BUILDERS.get(id);
        if(factory==null)throw new IllegalArgumentException("No battle mode "+id);
        return factory.apply(row(id));
    }
    /** A mode's rule line as the lobby shows it before any round: its sheet template with the mode's own {n}. */
    public static String ruleLine(String id){Spec.ModesRow row=row(id);return ModeMath.ruleLine(row.ruleLine(),row.winTarget());}
    /** Whether a mode may be played on a map: its maps cell lists the map indexes it hosts. */
    public static boolean hosts(String id,int map){
        for(String part:row(id).maps().split(","))if(!part.isBlank()&&Integer.parseInt(part.trim())==map)return true;
        return false;
    }
}
