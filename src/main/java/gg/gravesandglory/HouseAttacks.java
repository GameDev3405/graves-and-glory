package gg.gravesandglory;

import gg.gravesandglory.TroopClips.Clip;
import net.minecraft.client.render.entity.animation.Animation;
import java.util.*;

/**
 * The House attacks (Phase 2 Task 5c): each House's own way of striking, in place of the biped attack for that House's biped
 * troops (TroopTells.clip; the beasts and the colossus keep their rig's attack). A variant is a once clip as long as the attack
 * it replaces, 0.5 s, so TroopAnimator's blend out over the last BLEND_TICKS of the clip works as it does for the rig's; it
 * ends at rest. Angles are degrees, as the biped clips have them (a negative arm pitch raises the arm to the front, a positive
 * body pitch leans forward, a right arm's positive yaw swings it out to the right).
 * Each also turns the wrist (right_hand, as the biped attack does) so the blade lands on the target at the strike. The weapon points up and
 * forward at about 45 degrees with the arm at rest, so a blade level at an arm lift of L degrees takes a wrist of L + 45 (the gilded
 * thrust: 95 + 45 = 140); the blows that cut down take less.
 * STRIKE is the second of each at which the blow lands: the pose a still shot shows, and the moment House effects may take.
 */
public final class HouseAttacks {
    private HouseAttacks() {}
    /** Each House's attack, by House id. */
    public static final Map<String,Animation> VARIANTS;
    /** The second of each variant at which its blow lands. */
    public static final Map<String,Float> STRIKE=Map.of("gilded",.20f,"graveborn",.30f,"wildmoot",.25f,"ironclad",.35f,"tidecallers",.30f);
    static{
        Map<String,Animation> v=new LinkedHashMap<>();
        // From behind the shield: the shield arm held at -40 from the first frame (it was keyed up from 0, which dipped the shield for a tick), the sword arm
        // drawn back to -60, then the thrust level (-95) with the body turned into it.
        v.put("gilded",new Clip("attack").rot("left_arm",0,-40,0,0, .4f,-40,0,0, .5f,0,0,0)
            .rot("right_arm",0,0,0,0, .1f,-60,0,0, .2f,-95,0,0, .5f,0,0,0).rot("body",0,0,0,0, .2f,0,15,0, .5f,0,0,0).rot("right_hand",0,0,0,0, .1f,0,0,0, .2f,140,0,0, .5f,0,0,0).build());
        // The overhead hack: the sword arm from overhead (-170) down to the ground, the body stooping with it.
        v.put("graveborn",new Clip("attack").rot("right_arm",0,0,0,0, .15f,-170,0,0, .3f,0,0,0).rot("body",0,0,0,0, .3f,25,0,0, .5f,0,0,0)
            .rot("right_hand",0,0,0,0, .15f,0,0,0, .3f,75,0,0, .5f,0,0,0).build());
        // The low lunging swipe: the arm out at the right (yaw 70) and swept across to -40, the body down into the lunge.
        v.put("wildmoot",new Clip("attack").rot("right_arm",0,0,0,0, .1f,-60,70,0, .25f,-60,-40,0, .5f,0,0,0).rot("body",0,0,0,0, .25f,30,0,0, .5f,0,0,0)
            .rot("right_hand",0,0,0,0, .1f,0,0,0, .25f,105,0,0, .5f,0,0,0).build());
        // The two-handed shoulder chop: both arms up to -150 and down to level at the waist (-60, the blade level: 105 at the wrist), the shoulders behind it. It stops
        // there; the Graveborn hack goes through to the ground (fix round 1: at -20 the two ended alike, stooped with the blade at the feet).
        v.put("ironclad",new Clip("attack").rot("right_arm",0,0,0,0, .2f,-150,0,0, .35f,-60,0,0, .5f,0,0,0).rot("left_arm",0,0,0,0, .2f,-150,0,0, .35f,-60,0,0, .5f,0,0,0)
            .rot("body",0,0,0,0, .35f,15,0,0, .5f,0,0,0).rot("right_hand",0,0,0,0, .2f,0,0,0, .35f,105,0,0, .5f,0,0,0).build());
        // The wide sweeping lunge: the arm from far out (yaw 90) across to -60, the body turning 25 with it, the left leg stepping out.
        v.put("tidecallers",new Clip("attack").rot("right_arm",0,0,0,0, .1f,-80,90,0, .3f,-80,-60,0, .5f,0,0,0).rot("body",0,0,0,0, .3f,0,25,0, .5f,0,0,0)
            .rot("left_leg",0,0,0,0, .3f,-20,0,0, .5f,0,0,0).rot("right_hand",0,0,0,0, .1f,0,0,0, .3f,125,0,0, .5f,0,0,0).build());
        VARIANTS=Collections.unmodifiableMap(v);
    }
}
