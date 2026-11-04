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
        // R2-2: a rejoiner past the 2 min hold has no reservation left; the roster team decides the seat, not the first bot in the list.
        for(int size=2;size<=4;size++)for(int roster=0;roster<2;roster++) {
            var seats=new ArrayList<AdmissionPolicy.Seat>();
            seats.add(seat(0,false,true)); // the other human
            for(int i=1;i<size;i++)seats.add(seat(0,true,true));
            for(int i=0;i<size;i++)seats.add(seat(1,true,true));
            var d=AdmissionPolicy.decide(true,false,size,visitor,seats,Map.of(),roster);
            var taken=seats.stream().filter(s->s.owner().equals(d.replacement())).findFirst().orElse(null);
            check(d.seated()&&taken!=null&&taken.bot()&&taken.team()==roster&&d.team()==roster,"Released rejoiner takes a bot seat on his roster team "+roster+" (size "+size+")");
            check(AdmissionPolicy.decide(true,false,size,visitor,seats,Map.of()).replacement()!=null,"No roster team keeps the old pick");
        }
        {
            // Roster team has an empty seat and no bot: a new seat there. Roster team full of humans: the other side, as before.
            var seats=new ArrayList<>(List.of(seat(0,false,true),seat(1,true,true),seat(1,true,true)));
            var d=AdmissionPolicy.decide(true,false,2,visitor,seats,Map.of(),0);
            check(d.seated()&&d.team()==0&&d.replacement()==null,"Empty seat on the roster team is used before a bot of the other team");
            seats=new ArrayList<>(List.of(seat(0,false,true),seat(0,false,true),seat(1,true,true),seat(1,true,true)));
            d=AdmissionPolicy.decide(true,false,2,visitor,seats,Map.of(),0);
            check(d.seated()&&d.team()==1,"A roster team of humans only sends him to the other side");
            // A bot seat held for someone else is the last choice on the roster team, after a free seat.
            var held=seat(0,true,true);UUID holder=UUID.randomUUID();
            d=AdmissionPolicy.decide(true,false,2,visitor,List.of(seat(0,false,true),held,seat(1,true,true),seat(1,true,true)),Map.of(holder,held.owner()),0);
            check(d.seated()&&held.owner().equals(d.replacement()),"Roster team's held seat is used when nothing else is on it");
        }
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
