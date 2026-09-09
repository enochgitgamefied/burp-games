package dev.velocity;

/** Simulation checks exercise full races, independent input and frame-rate invariance. */
public final class KartRaceTest {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static void close(double a,double b,double tolerance,String text){check(Math.abs(a-b)<tolerance,text+": "+a+" / "+b);}
    static KartRace fresh(){KartRace r=new KartRace(new Progress(false));r.start(0,0,1,1);r.update(3);return r;}
    public static void main(String[] args){
        for(int level=0;level<5;level++){
            KartTrack t=new KartTrack(level);double low=1e9,high=-1e9;boolean bends=false;
            for(int i=0;i<t.length;i++){low=Math.min(low,t.height(i));high=Math.max(high,t.height(i));bends|=Math.abs(t.curve(i))>.005;}
            check(high-low>12&&bends,"circuit has real hills and corners");
            double[] p=t.point(3,1,-.2),q=t.point(3,1,t.length-.2);for(int i=0;i<3;i++)close(p[i],q[i],1e-8,"closed seam position");
            close(t.heading(-.001),t.heading(t.length+.001),.0001,"closed seam heading");
            close(t.height(-.001),t.height(.001),.001,"closed seam height");
            for(int i=0;i<t.length;i+=7){double[] a=t.point(0,0,i),b=t.point(0,0,i+1);close(Math.hypot(a[0]-b[0],a[2]-b[2]),1,.025,"arc length sampling");}
        }
        KartRace r=new KartRace(new Progress(false));r.start(0,0,1,2);check(r.racers.size()==4&&r.player2!=null,"four unique racers with local two-player");
        r.update(1.25);close(r.countdown,1.75,1e-9,"wall-time countdown");r.pause();r.update(20);close(r.countdown,1.75,1e-9,"pause freezes countdown");r.resume();r.update(1.75);check(r.state==KartRace.State.RACING,"countdown resumes exactly");
        double x=r.player2.x;r.player1.steering=-1;r.player1.throttle=true;r.update(.1);check(r.player1.x<2&&Math.abs(r.player2.x-x)<.01,"independent steering");
        r.pause();double distance=r.player1.distance;r.update(3);close(distance,r.player1.distance,1e-9,"pause freezes race");check(r.player1.steering==0&&!r.player1.throttle,"pause releases inputs");
        KartRace a=fresh(),b=fresh();a.player1.throttle=b.player1.throttle=true;
        for(int i=0;i<180;i++)a.update(1./30);for(int i=0;i<720;i++)b.update(1./120);
        close(a.player1.distance,b.player1.distance,1e-7,"movement independent of frame rate");close(a.elapsed,b.elapsed,1e-7,"time independent of frame rate");
        a=fresh();b=fresh();a.player1.throttle=b.player1.throttle=true;a.player1.boost=true;
        for(int i=0;i<120;i++){a.update(1./60);b.update(1./60);}check(a.player1.distance>b.player1.distance+35&&a.player1.energy<b.player1.energy,"boost produces speed and consumes energy");
        r=fresh();r.player1.distance=90;r.player1.x=0;r.player1.energy=35;r.player1.drift=true;r.player1.steering=.3;r.update(.25);check(r.player1.energy>40,"drift recharges boost");
        r=fresh();double at=r.track.coin(3);r.player1.distance=at-1;r.player1.x=r.track.coinLane(3);r.player1.speed=130;r.player1.boost=true;r.update(1./60);check(r.collected[0][3]&&r.player1.coins==1,"swept pickup catches coin at high speed");
        r=fresh();r.player1.distance=r.track.pad(0)-4;r.player1.x=r.track.padLane(0);r.player1.speed=100;r.update(.1);check(r.player1.padBoost>0&&r.padEvents==1,"pad activates only once per pass");
        r=fresh();r.player1.distance=100;r.player1.x=0;r.player1.speed=110;KartRace.Racer rival=r.racers.get(0);rival.distance=101.8;rival.x=0;rival.speed=85;r.update(1./120);check(r.player1.invincible>0&&r.hitEvents==1,"physical kart contact");
        r=fresh();r.player1.x=12;r.player1.speed=110;r.update(1./120);check(r.player1.x<=9.5&&r.player1.speed<90,"wall response bounded");
        // Finish full races on each environment, without injecting finish states or times.
        for(int level=0;level<5;level++){
            Progress progress=new Progress(false);progress.turboUnlocked=4;r=new KartRace(progress);r.start(level,level%4,(level+1)%4,1);r.update(3);
            for(int frame=0;frame<60*120&&r.state==KartRace.State.RACING;frame++){r.player1.throttle=true;r.player1.boost=r.player1.energy>5;r.player1.steering=Math.max(-1,Math.min(1,-r.player1.x*.45));r.update(1./60);}
            check(r.state==KartRace.State.FINISHED,"race reaches results: "+level);double previous=0;
            for(KartRace.Racer finisher:r.order()){check(finisher.finished()&&finisher.finishTime>=previous,"results use complete ordered race times");previous=finisher.finishTime;}
            check(progress.kartBest[level]==r.player1.finishTime,"solo record saved");
        }
        Progress p=new Progress(false);p.completeKart(0,3,41);check(p.turboUnlocked==0,"third does not unlock");p.completeKart(0,2,40);check(p.turboUnlocked==1,"top two unlock next");p.completeKart(0,1,42);check(p.kartBest[0]==40,"slower time preserves record");
        r=new KartRace(new Progress(false));r.start(0,0,1,2);r.update(3);for(int frame=0;frame<60*100&&r.state==KartRace.State.RACING;frame++){r.player1.throttle=r.player2.throttle=true;r.update(1./60);}
        check(r.state==KartRace.State.FINISHED&&r.player1.finished()&&r.player2.finished(),"two players both finish");check(r.progress.kartBest[0]==0,"multiplayer does not overwrite solo records");
        System.out.println("PASS: five closed circuits, fixed-step timing, pause, independent players, boost, drift, swept coins, pads, collisions, full races and progression.");
    }
}
