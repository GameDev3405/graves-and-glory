package gg.gravesandglory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * attack_v1's cancel (coordinator 2026-11-11: a dropped bow shot lowers the drawn bow), through the pure code the game runs: when the server drops an
 * announced shot (BattleMath.drawUntaken, drawBeat, shotStart) and what the client keeps (MotionInput.admits and cancel, as MotionEvents.Client.attack and
 * cancel call them), at the ticks the server really sends a cancel: on the beat tick (Tactics.dropUntaken) and on the loose tick (Tactics.looseLate).
 */
public final class AttackCancelCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 static final int IMPACT=22,MAX=BattleMath.ATTACK_CLIP_MAX_TICKS;
 /** One troop's client queues, fed as MotionEvents.Client feeds them (its push and KEEP cap aside). */
 static final class Troop{
  final Deque<MotionInput.Attack> kept=new ArrayDeque<>();final Deque<MotionInput.Cancel> done=new ArrayDeque<>();
  boolean announce(int variant,long start){var a=new MotionInput.Attack(variant,start);if(!MotionInput.admits(kept,done,a))return false;kept.addLast(a);return true;}
  boolean cancel(long start,long now){var c=MotionInput.cancel(kept,done,start,now,MAX);if(c==null)return false;done.addLast(c);return true;}
  boolean playing(long start){return MotionInput.kept(kept,start)!=null&&MotionInput.cancelled(done,start)==null;}
 }
 public static void main(String[] args){
  // the server's side: shotStart and drawBeat are inverses for every seed; the beat is when an untaken shot is dropped, not a tick before
  for(int loose=0;loose<=3;loose++)for(int hold=0;hold<=3;hold++){
   long beat=1000,start=BattleMath.shotStart(beat,IMPACT,hold,loose);
   check(BattleMath.drawBeat(start,IMPACT,hold,loose)==beat,"drawBeat inverts shotStart (loose "+loose+", hold "+hold+")");
   check(start+IMPACT+hold==beat+loose,"the clip's loose lands loose ticks after the beat (loose "+loose+", hold "+hold+")");
  }
  check(BattleMath.drawUntaken(500,-1,200,190,1000,1000),"an announce on its beat tick, no late shot, cooldown unspent: dropped on the beat tick itself");
  check(!BattleMath.drawUntaken(500,-1,199,190,999,1000),"a tick before its beat: not dropped");
  check(!BattleMath.drawUntaken(500,203,200,230,1000,1000),"the beat took the shot (a late shot waits, looseAt): not dropped");
  check(!BattleMath.drawUntaken(500,-1,200,230,1000,1000),"the beat took the shot (its cooldown moved past now): not dropped");
  check(!BattleMath.drawUntaken(-1,-1,200,190,1000,1000),"the shot left (loose cleared drawStart): nothing to drop");
  // the client's side at the real send ticks: on the beat tick (heard then or a tick later) and on the loose tick (looseLate), for every loose seed
  for(int loose=0;loose<=3;loose++){
   int hold=Math.min(loose,2);long beat=2000+loose*100,start=BattleMath.shotStart(beat,IMPACT,hold,loose),looseTick=beat+loose;
   for(long heard:new long[]{beat,beat+1,looseTick,looseTick+1,looseTick+2}){
    var t=new Troop();t.announce(1,start);
    check(t.cancel(start,heard),"loose "+loose+": a cancel heard "+(heard-beat)+" ticks after the beat ("+(heard-looseTick)+" after the loose) takes");
    check(!t.playing(start),"loose "+loose+": and the draw is lowered");
   }
  }
  // the earlier cases: a repeat, a late repeat of the cancelled announce, a new announce, an unknown attack, a finished one, the right shot
  var t=new Troop();
  check(t.announce(1,1000),"the announce starts the draw");
  check(!t.announce(1,1000),"its repeat on the next beat is the same clip (kept once)");
  check(t.cancel(1000,1030),"its cancel on its beat tick takes");
  check(MotionInput.cancelled(t.done,1000).at()==1030,"the cancel keeps the tick it was heard on, the fade's start");
  check(!t.cancel(1000,1031),"a second cancel for it does nothing");
  check(!t.announce(1,1000)&&!t.announce(0,1000),"a late repeat of the cancelled announce (either variant) does not restart it");
  check(t.announce(1,1060)&&t.playing(1060),"a new announce (a later start) after the cancel starts and plays");
  var u=new Troop();
  check(!u.cancel(3000,3010)&&u.done.isEmpty(),"a cancel for an attack never heard of does nothing and keeps nothing");
  check(u.announce(0,3000),"an announce of that start afterwards still starts");
  check(!u.cancel(3000,2999),"a cancel heard before its attack began does nothing");
  check(!u.cancel(3000,3000+MAX+1)&&u.playing(3000),"a cancel for an attack whose clip has ended does nothing");
  var k=new Troop();k.announce(1,4000);k.announce(1,4060);
  check(k.cancel(4000,4030)&&!k.playing(4000)&&k.playing(4060),"a cancel names only the shot of its start");
  // wire: the cancel rides attack_v1's variant field; an old client drops it as an invalid variant, and -2 stays invalid for HoldLineChecks' probe
  check(MotionInput.CANCEL==-1&&!BattleMath.validVariant(MotionInput.CANCEL,64)&&!BattleMath.validVariant(-2,64),"CANCEL is no valid variant, so a client without the cancel drops it");
  System.out.println("AttackCancelCheck: "+count+" checks passed");
 }
}
