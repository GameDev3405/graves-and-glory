package gg.gravesandglory;

import java.util.*;

public final class AdmissionPolicyCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static AdmissionPolicy.Seat seat(int team, boolean bot, boolean seated) {
        return new AdmissionPolicy.Seat(UUID.randomUUID(),team,bot,seated);
    }
    public static void main(String[] args) {
        UUID visitor=UUID.randomUUID();
        for (int size=1;size<=4;size++) {
            var seats=new ArrayList<AdmissionPolicy.Seat>();
            for(int team=0;team<2;team++)for(int i=0;i<size;i++)seats.add(seat(team,false,true));
            var full=AdmissionPolicy.decide(true,false,size,visitor,seats,Map.of());
            check(!full.seated()&&full.replacement()==null,"Full human match must keep visitor in camp: "+size);
            var removed=seats.remove(0);
            for(int i=0;i<50;i++)seats.add(seat(0,false,false));
            var open=AdmissionPolicy.decide(true,false,size,visitor,seats,Map.of());
            check(open.seated()&&open.team()==removed.team()&&open.replacement()==null,"Camp visitors must not consume vacant team seat");
            var bot=seat(0,true,true);seats.add(bot);
            var replacement=AdmissionPolicy.decide(true,false,size,visitor,seats,Map.of());
            check(replacement.seated()&&bot.owner().equals(replacement.replacement()),"Bot takeover must not create an extra army");
            var returning=AdmissionPolicy.decide(true,false,size,visitor,seats,Map.of(visitor,bot.owner()));
            check(bot.owner().equals(returning.replacement()),"Returning commander must reclaim held army");
            check(!AdmissionPolicy.decide(false,false,size,visitor,seats,Map.of()).seated(),"No battle seat while idle");
        }
        var own=seat(0,true,true);var free=seat(1,true,true);UUID other=UUID.randomUUID();
        check(AdmissionPolicy.decide(true,false,1,visitor,List.of(own,free),Map.of(other,own.owner())).replacement().equals(free.owner()),"Prefer bot without reconnect reservation");
        check(AdmissionPolicy.decide(true,false,1,visitor,List.of(own,free),Map.of(visitor,own.owner())).replacement().equals(own.owner()),"Own reconnect reservation takes priority over free bot");
        check(!AdmissionPolicy.decide(true,true,1,visitor,List.of(seat(0,false,true),free),Map.of()).seated(),"Practice visitor waits in camp");
        check(!AdmissionPolicy.decide(true,true,1,visitor,List.of(own,free),Map.of(other,own.owner())).seated(),"Practice visitor cannot steal disconnected commander's seat");
        check(AdmissionPolicy.decide(true,true,1,visitor,List.of(own,free),Map.of(visitor,own.owner())).replacement().equals(own.owner()),"Practice commander can reconnect");
        check(!AdmissionPolicy.decide(true,true,1,visitor,List.of(free),Map.of(visitor,UUID.randomUUID())).seated(),"Stale reservation cannot replace practice opponent");
        // Repeated arrivals with bots or empty seats must never exceed either team capacity.
        for(int size=1;size<=4;size++)for(boolean fill:new boolean[]{false,true}) {
            var seats=new ArrayList<AdmissionPolicy.Seat>();
            if(fill)for(int team=0;team<2;team++)for(int i=0;i<size;i++)seats.add(seat(team,true,true));
            for(int i=0;i<30;i++) {
                UUID id=UUID.randomUUID();
                var d=AdmissionPolicy.decide(true,false,size,id,seats,Map.of());
                if(d.replacement()!=null)seats.removeIf(s->s.owner().equals(d.replacement()));
                seats.add(new AdmissionPolicy.Seat(id,d.team(),false,d.seated()));
                for(int t=0;t<2;t++){final int team=t;check(seats.stream().filter(s->s.seated()&&s.team()==team).count()<=size,"Repeated arrivals exceed team capacity");}
                check(d.seated()==(i<size*2),"Arrivals after capacity must wait");
            }
        }
        System.out.println("PASS "+checks+" admission assertions: full matches, 1v1-4v4, bot fill, camp visitors, reconnects and practice isolation");
    }
}
