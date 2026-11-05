package gg.gravesandglory;

/**
 * The troops' idle tells, read from the motion engine (animation rework Task 13). The Phase 2 per-troop tell clips are gone: a
 * troop standing calm now plays the engine's novelty idles (MotionBody: a weight shift, a look round, a shrug), seeded per entity
 * so a line does not move in step, and only while it is calm (not moving, no enemy near, no action playing, alive). This class
 * reports what the engine is playing, for the harness and for anything that wants to sync a sound or particle to a tell.
 */
public final class TroopTells {
    private TroopTells() {}
    /** A novelty idle playing: its kind (MotionBody's index), seconds since it began, its length and side (-1 left, 1 right). */
    public record Tell(int kind,double seconds,double length,double side){}
    /** The tell an entity id is playing now (on its last drawn frame), or null for none or an entity this client has not posed. */
    public static Tell playing(int id){
        MotionEngine.State st=TroopMotion.state(id);
        if(st==null||st.novKind<0)return null;
        return new Tell(st.novKind,st.frame.now-st.novStart,st.novLength,st.novSide);
    }
    /** How many tells an entity id has started (0 for one this client has not posed). */
    public static int started(int id){MotionEngine.State st=TroopMotion.state(id);return st==null?0:st.novelties;}
}
