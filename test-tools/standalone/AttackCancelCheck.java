package gg.gravesandglory;

import java.util.ArrayDeque;
import java.util.Deque;

/** attack_v1's cancel (coordinator 2026-11-11: a dropped bow shot lowers the drawn bow), through MotionInput's pure rules as MotionEvents.Client applies them: what a cancel takes, and what a later announce does after one. */
public final class AttackCancelCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 /** One troop's client queues: its kept attacks and its cancels, as MotionEvents.Client keeps them. */
 static final class Troop{
  final Deque<MotionInput.Attack> kept=new ArrayDeque<>();final Deque<MotionInput.Cancel> done=new ArrayDeque<>();
  /** attack_v1 with a variant: kept unless a repeat or cancelled (MotionEvents.Client.attack). */
  boolean announce(int variant,long start){var a=new MotionInput.Attack(variant,start);if(!MotionInput.admits(kept,done,a))return false;kept.addLast(a);return true;}
  /** attack_v1 with CANCEL heard at now, the shot's loose at loose (MotionEvents.Client.cancel). */
  boolean cancel(long start,long now,long loose){if(!MotionInput.cancels(kept,done,start,now,loose))return false;done.addLast(new MotionInput.Cancel(start,now));return true;}
  boolean playing(long start){var a=MotionInput.kept(kept,start);return a!=null&&MotionInput.cancelled(done,start)==null;}
 }
 public static void main(String[] args){
  // a bow's draw announced 30 ticks ahead (start 1000), its loose at 1024; the beat's target is lost and the server drops it at 1018
  var t=new Troop();
  check(t.announce(1,1000),"the announce starts the draw");
  check(!t.announce(1,1000),"its repeat on the next beat is the same clip (kept once)");
  check(t.cancel(1000,1018,1024),"cancel removes the pending attack: a cancel before the loose takes");
  check(!t.playing(1000),"the cancelled draw no longer plays (AttackController fades it out from where it was)");
  check(MotionInput.cancelled(t.done,1000).at()==1018,"the cancel keeps the tick it was heard on, the fade's start");
  check(!t.cancel(1000,1019,1024),"a second cancel for it does nothing (sent once; a repeat is harmless)");
  // a late repeat of the cancelled announce (same start, the same or another variant) never restarts the draw
  check(!t.announce(1,1000),"a late repeat of the cancelled announce does not restart it");
  check(!t.announce(0,1000),"nor one with another variant at the same start");
  check(!t.playing(1000),"still cancelled after the repeats");
  // a NEW announce after a cancel starts
  check(t.announce(1,1060),"a new announce (a later start) after the cancel starts");
  check(t.playing(1060),"and plays");
  // a cancel at or after the loose tick does nothing: the shot left, its follow-through plays out
  var l=new Troop();l.announce(0,2000);
  check(!l.cancel(2000,2024,2024),"a cancel heard on the loose tick does nothing");
  check(!l.cancel(2000,2030,2024),"a cancel after the loose does nothing");
  check(l.playing(2000),"the shot plays on");
  // a cancel for an unknown attack does nothing, and leaves no mark on a later announce of that start
  var u=new Troop();
  check(!u.cancel(3000,2990,3024),"a cancel for an attack never heard of does nothing");
  check(u.done.isEmpty(),"and keeps nothing");
  check(u.announce(0,3000),"an announce of that start afterwards still starts");
  u.announce(0,3100);
  check(!u.cancel(3050,3060,3074),"a cancel whose start matches no kept attack does nothing");
  check(u.playing(3000)&&u.playing(3100),"the kept attacks play on");
  // the cancel cannot kill a later shot: it names its start tick, and a troop's shots never share one
  var k=new Troop();k.announce(1,4000);k.announce(1,4060);
  check(k.cancel(4000,4010,4024)&&!k.playing(4000)&&k.playing(4060),"a cancel names only the shot of its start");
  // wire: the cancel rides attack_v1's variant field; an old client drops it as an invalid variant
  check(MotionInput.CANCEL<0&&!BattleMath.validVariant(MotionInput.CANCEL,64),"CANCEL is no valid variant, so a client without the cancel drops it");
  System.out.println("AttackCancelCheck: "+count+" checks passed");
 }
}
