package dev.velocity;

import java.util.ArrayList;
import java.util.List;

/** Fast track-relative runner with optional vehicles and optional security quizzes. */
public final class Game {
    public enum State { READY, RUNNING, PAUSED, CHALLENGE, WON, LOST }
    public enum Kind { RING, CRATE, BIKE, CAR, SURFBOARD }
    public static final double GRAVITY=32,JUMP=13;
    public static final class Item {
        public final Kind kind;public final double x,z;public boolean used;
        Item(Kind kind,double x,double z){this.kind=kind;this.x=x;this.z=z;}
        boolean pickup(){return kind==Kind.BIKE||kind==Kind.CAR||kind==Kind.SURFBOARD;}
    }
    final Progress progress;
    public final List<Item> items=new ArrayList<>();
    public State state;
    Track track;
    Vehicle vehicle=Vehicle.RUNNING;
    public double z,x,y,vy,speed,energy,elapsed,invincible,checkpoint,steering,wheelAngle,pickupNotice;
    public int lane,rings,hits,level,difficulty,mission,correct,crashes,medal;
    public boolean boosting,throttle,braking,answered,quizEnabled;
    public String reason="",feedback="";
    public Game(){this(new Progress(false));}
    Game(Progress progress){this.progress=progress;quizEnabled=progress.quizEnabled;reset();}
    double length(){return 3600+level*600;}
    double topSpeed(){return (92+level*4+vehicle.bonus)*(difficulty==0?.95:difficulty==2?1.08:1);}
    double cruiseSpeed(){return topSpeed()*.84;}
    void selectLevel(int value){if(value>=0&&value<4&&value<=progress.unlocked){level=value;reset();}}
    public void reset(){
        z=x=y=vy=elapsed=invincible=checkpoint=steering=wheelAngle=pickupNotice=0;
        lane=rings=hits=mission=correct=crashes=medal=0;speed=0;energy=100;answered=false;vehicle=Vehicle.RUNNING;
        clearInput();state=State.READY;reason=feedback="";track=new Track(level);items.clear();
        for(int p=42;p<length()-24;p+=18)if(!gap(p))items.add(new Item(Kind.RING,((p/180)%3-1)*3,p));
        for(int p=240;p<length()-40;p+=130-level*10)if(!gap(p-25)&&!gap(p+25)&&!nearMission(p))items.add(new Item(Kind.CRATE,((p/70)%3-1)*3,p));
        for(int i=0,p=150;p<length()-100;i++,p+=450)if(!gap(p))items.add(new Item(new Kind[]{Kind.BIKE,Kind.CAR,Kind.SURFBOARD}[i%3],i%2==0?3:-3,p));
    }
    boolean nearMission(double p){return Math.abs(p-length()*.32)<30||Math.abs(p-length()*.68)<30;}
    public static boolean gap(double s){return s>100&&s%600>=396&&s%600<408;}
    void clearInput(){boosting=throttle=braking=false;steering=0;}
    public void togglePause(){if(state==State.READY||state==State.PAUSED)state=State.RUNNING;else if(state==State.RUNNING)state=State.PAUSED;clearInput();}
    public void steer(int direction){if(state==State.RUNNING)steering=direction;}
    public void jump(){if(state==State.RUNNING&&y==0)vy=JUMP;}
    void dismount(){vehicle=Vehicle.RUNNING;pickupNotice=0;}
    Missions.Question question(){return Missions.QUESTIONS[level][Math.min(1,mission)];}
    void answer(int index){
        if(state!=State.CHALLENGE||answered||index<0||index>2)return;
        answered=true;boolean good=index==question().correct();if(good){correct++;rings+=25;}
        feedback=(good?"Correct. ":"Review: ")+question().explanation();
    }
    void passCheckpoint(){checkpoint=z+3;z=checkpoint;mission++;answered=false;energy=100;hits=0;}
    void continueMission(){if(state!=State.CHALLENGE||!answered)return;passCheckpoint();state=State.RUNNING;clearInput();}
    void respawn(){z=checkpoint;x=y=vy=0;speed=35;hits=0;invincible=2;vehicle=Vehicle.RUNNING;state=State.RUNNING;clearInput();}
    void crash(String why){crashes++;state=State.LOST;reason=why;clearInput();}
    public void update(double dt){
        if(state!=State.RUNNING||dt<=0)return;
        // Fixed small substeps keep pickups and collisions reliable at boost speed.
        int steps=(int)Math.ceil(Math.min(dt,.05)/.008);double step=Math.min(dt,.05)/steps;
        for(int i=0;i<steps&&state==State.RUNNING;i++)simulate(step);
    }
    private void simulate(double dt){
        elapsed+=dt;invincible=Math.max(0,invincible-dt);pickupNotice=Math.max(0,pickupNotice-dt);
        boolean dash=boosting&&energy>1;energy=Math.max(0,Math.min(100,energy+(dash?-25:18)*dt));
        double target=braking?25:(throttle?topSpeed():cruiseSpeed())+(dash?45:0);
        speed=Math.max(0,speed+((target-speed)*3.2-track.slope(z)*8)*dt);
        double old=z;z+=speed*dt;wheelAngle+=speed*dt/.49;
        x+=steering*(11+speed*.025)*dt;
        // Road-following assistance caps corner drift so the faster pace stays controllable.
        x-=track.curve(z)*Math.min(speed,85)*(difficulty==0?.18:.32)*dt;
        lane=(int)Math.round(x/3);
        if(Math.abs(x)>5.2){x=Math.copySign(4.8,x);if(invincible==0){hits++;invincible=1.5;speed*=.75;}}
        if(vy!=0||y>0){y+=vy*dt;vy-=GRAVITY*dt;if(y<=0){y=0;vy=0;}}
        if(gap(z)&&y<.10){crash("Fell into a gap. Press Space before the edge to jump.");return;}
        for(Item item:items){
            if(item.used||item.z<old-1||item.z>z+1||Math.abs(x-item.x)>(item.pickup()?1.25:1))continue;
            if(item.kind==Kind.RING&&y<1.8){item.used=true;rings++;energy=Math.min(100,energy+2);}
            else if(item.pickup()&&y<1.8){item.used=true;vehicle=Vehicle.valueOf(item.kind.name());pickupNotice=2.4;energy=Math.min(100,energy+15);}
            else if(item.kind==Kind.CRATE&&y<1.4&&invincible==0){item.used=true;hits++;rings=Math.max(0,rings-5);invincible=1.5;speed*=.70;}
        }
        if(hits>=3){crash("Three collisions. Resume from your last checkpoint.");return;}
        if(mission<2&&z>=length()*(mission==0?.32:.68)){
            if(quizEnabled){state=State.CHALLENGE;answered=false;feedback="";clearInput();return;}
            passCheckpoint();
        }
        if(z>=length()){
            z=length();state=State.WON;clearInput();
            boolean fast=elapsed<length()/70;
            medal=crashes==0&&fast&&(!quizEnabled||correct==2)?3:(!quizEnabled||correct>=1)?2:1;
            progress.complete(level,difficulty,quizEnabled,elapsed,medal);
        }
    }
}
