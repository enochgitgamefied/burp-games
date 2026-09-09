package dev.velocity;

import java.util.*;

/** Native race simulation built on the Sonic game's track-relative acceleration and steering. */
final class KartRace {
    enum State { MENU, COUNTDOWN, RACING, PAUSED, FINISHED }
    static final int LAPS=2;static final double STEP=1.0/120;
    static final class Racer {
        final KartDriver driver;final int human; // 0 = computer; 1 / 2 = local players.
        double distance,x,speed,steering,energy=100,wheels,invincible,padBoost,finishTime=-1,driftCharge;
        boolean throttle,brake,boost,drift;int coins,lastPad=-1;
        Racer(KartDriver driver,int human){this.driver=driver;this.human=human;}
        boolean finished(){return finishTime>=0;}
        void clear(){throttle=brake=boost=drift=false;steering=0;}
    }
    final Progress progress;KartTrack track;
    State state=State.MENU,resumeState=State.RACING;
    final List<Racer> racers=new ArrayList<>();
    Racer player1,player2;int level,players=1;double countdown=3,elapsed,accumulator;
    boolean[][] collected;int coinEvents,hitEvents,padEvents;
    KartRace(Progress progress){this.progress=progress;track=new KartTrack(0);}
    void start(int level,int first,int second,int players){
        if(level<0||level>=5||level>progress.turboUnlocked||first<0||first>3||second<0||second>3||first==second||(players!=1&&players!=2))throw new IllegalArgumentException("Invalid race selection");
        this.level=level;this.players=players;track=new KartTrack(level);racers.clear();
        player1=new Racer(KartDriver.values()[first],1);player2=players==2?new Racer(KartDriver.values()[second],2):null;
        for(KartDriver d:KartDriver.values())if(d!=player1.driver&&(player2==null||d!=player2.driver))racers.add(new Racer(d,0));
        if(player2!=null)racers.add(player2);racers.add(player1);
        for(int i=0;i<racers.size();i++){Racer r=racers.get(i);r.x=i%2==0?-3:3;r.distance=-i*3.8;}
        elapsed=accumulator=0;countdown=3;coinEvents=hitEvents=padEvents=0;collected=new boolean[LAPS][track.coinCount()];state=State.COUNTDOWN;
    }
    void pause(){if(state==State.RACING||state==State.COUNTDOWN){resumeState=state;state=State.PAUSED;}clear();}
    void resume(){if(state==State.PAUSED)state=resumeState;clear();accumulator=0;}
    void clear(){for(Racer r:racers)r.clear();}
    void menu(){state=State.MENU;clear();}
    void update(double dt){
        if(dt<=0||!Double.isFinite(dt))return;
        if(state==State.COUNTDOWN){countdown-=dt;if(countdown<=0){double left=-countdown;countdown=0;state=State.RACING;advance(Math.min(.25,left));}return;}
        if(state==State.RACING)advance(Math.min(.25,dt));
    }
    private void advance(double dt){
        accumulator+=dt;
        while(accumulator+1e-12>=STEP&&state==State.RACING){step(STEP);accumulator-=STEP;}
        if(player1.finished()&&(player2==null||player2.finished())&&state==State.RACING){
            // Complete the remaining computer laps using the same simulation, so all finish times are real.
            for(int i=0;i<120*180&&racers.stream().anyMatch(r->!r.finished());i++)step(STEP);
            state=State.FINISHED;clear();
            if(players==1)progress.completeKart(level,place(player1),player1.finishTime);
        }
    }
    private void step(double dt){
        for(int i=0;i<racers.size();i++){Racer r=racers.get(i);if(r.finished())continue;
            if(r.human==0)ai(r,i);
            simulate(r,dt);
        }
        for(int i=0;i<racers.size();i++)for(int j=i+1;j<racers.size();j++){
            Racer a=racers.get(i),b=racers.get(j);if(a.finished()||b.finished())continue;
            double dz=relative(a.distance,b.distance),dx=a.x-b.x;
            if(Math.abs(dz)<2.6&&Math.abs(dx)<1.95&&a.invincible<=0&&b.invincible<=0){
                Racer behind=dz<0?a:b;behind.speed*=behind.driver==KartDriver.KNUCKLES?.9:.78;
                double side=dx==0?(i%2==0?-1:1):Math.signum(dx);a.x+=side*.28;b.x-=side*.28;a.invincible=b.invincible=.7;
                if(a.human>0||b.human>0)hitEvents++;
            }
        }
        elapsed+=dt;
    }
    private void ai(Racer r,int index){
        double target=new double[]{-4.8,-1.6,1.6,4.8}[index];
        for(Racer other:racers){double ahead=relative(other.distance,r.distance);if(other!=r&&!other.finished()&&ahead>0&&ahead<24&&Math.abs(other.x-r.x)<2.2){target=r.x<0?3.6:-3.6;break;}}
        r.steering=Math.max(-1,Math.min(1,(target-r.x)*.45));r.throttle=true;r.brake=false;
        r.boost=r.energy>38&&Math.abs(track.curve(r.distance))<.007&&Math.sin(elapsed*.34+index*1.8)>.55;
        r.drift=Math.abs(track.curve(r.distance))>.008&&Math.abs(r.steering)>.3;
    }
    private void simulate(Racer r,double dt){
        double old=r.distance;r.invincible=Math.max(0,r.invincible-dt);r.padBoost=Math.max(0,r.padBoost-dt);
        boolean boost=r.boost&&r.energy>1;
        r.energy=Math.max(0,Math.min(100,r.energy+(boost?-25:r.drift&&Math.abs(r.steering)>.25?(r.driver==KartDriver.AMY?29:24):12)*dt));
        double cruise=r.driver.topSpeed*(r.human==0?.85:.80);
        double target=(r.brake?24:r.throttle?r.driver.topSpeed:cruise)+(boost?32:0)+(r.padBoost>0?20:0);
        if(r.human==0)target*=.91;
        if(Math.abs(r.x)>7.8)target*=r.driver==KartDriver.KNUCKLES?.75:.62;
        r.speed=Math.max(0,r.speed+((target-r.speed)*3.2-track.slope(r.distance)*8)*dt);
        r.x+=r.steering*(11+r.speed*.025)*r.driver.handling*(r.drift?1.22:1)*dt;
        r.x-=track.curve(r.distance)*Math.min(r.speed,85)*.24*dt;
        if(Math.abs(r.x)>9.5){r.x=Math.copySign(9.3,r.x);if(r.invincible<=0){r.speed*=.70;r.invincible=.8;if(r.human>0)hitEvents++;}}
        r.distance+=r.speed*dt;r.wheels+=r.speed*dt/.44;
        for(int lap=Math.max(0,(int)Math.floor(old/track.length));lap<=Math.min(LAPS-1,(int)Math.floor(r.distance/track.length));lap++){
            for(int i=0;i<track.coinCount();i++){
                double at=lap*track.length+track.coin(i);
                if(!collected[lap][i]&&at>=old-.7&&at<=r.distance+.7&&Math.abs(r.x-track.coinLane(i))<1.15){collected[lap][i]=true;r.coins++;r.energy=Math.min(100,r.energy+9);if(r.human>0)coinEvents++;}
            }
            for(int i=0;i<4;i++){double at=lap*track.length+track.pad(i);int id=lap*4+i;
                if(id!=r.lastPad&&r.distance>=at-3&&old<=at+3&&Math.abs(r.x-track.padLane(i))<1.8){r.lastPad=id;r.padBoost=1.3;if(r.human>0)padEvents++;}
            }
        }
        if(r.distance>=LAPS*track.length){r.finishTime=elapsed+(LAPS*track.length-old)/Math.max(r.speed,1);r.distance=LAPS*track.length;r.speed=0;r.clear();}
    }
    double relative(double a,double b){double d=a-b;return d-Math.floor((d+track.length*.5)/track.length)*track.length;}
    List<Racer> order(){ArrayList<Racer> list=new ArrayList<>(racers);list.sort(Comparator.comparingDouble((Racer r)->r.finished()?r.finishTime:1e8-r.distance).thenComparingInt(r->r.driver.ordinal()));return list;}
    int place(Racer r){return order().indexOf(r)+1;}
    int lap(Racer r){return Math.min(LAPS,Math.max(1,(int)Math.floor(r.distance/track.length)+1));}
}
