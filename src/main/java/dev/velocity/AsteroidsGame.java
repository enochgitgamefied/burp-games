package dev.velocity;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Random;

/** Fixed-facing space shooter: direct horizontal/forward movement and hostile ships. */
final class AsteroidsGame extends ArcadeGame {
    static final class Rock {double x,y,vx,vy,r,spin;Rock(double x,double y,double vx,double vy,double r){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.r=r;}}
    static final class Shot {double x,y,vx,vy,life=2.2;Shot(double x,double y,double vx,double vy){this.x=x;this.y=y;this.vx=vx;this.vy=vy;}}
    static final class Enemy {double x,y,origin,age,cooldown=.8;int hp=3;Enemy(double x,double y){this.x=origin=x;this.y=y;}}
    static final class Burst {double x,y,life=.45;Burst(double x,double y){this.x=x;this.y=y;}}
    final ArrayList<Rock> rocks=new ArrayList<>();final ArrayList<Shot> shots=new ArrayList<>(),hostileShots=new ArrayList<>();
    final ArrayList<Enemy> enemies=new ArrayList<>();final ArrayList<Burst> bursts=new ArrayList<>();
    final Random random=new Random(27);
    double x=480,y=475,vx,vy,cooldown,shield=3,time,spawnClock;
    int enemiesRemaining;
    AsteroidsGame(){this(AudioEngine.silent());}
    AsteroidsGame(AudioEngine audio){super(audio,AudioEngine.Track.SPACE);reset();}
    String title(){return "Asteroids";}
    String controls(){return "A / D or ARROWS  LEFT / RIGHT     W / UP  FORWARD     SPACE  FIRE     ENTER  PLAY     P  PAUSE     R  RESTART";}
    void reset(){score=0;lives=3;level=1;ended=false;shots.clear();hostileShots.clear();enemies.clear();bursts.clear();x=480;y=475;vx=vy=time=0;shield=3;cooldown=0;spawnWave();}
    void spawnWave(){
        rocks.clear();for(int i=0;i<Math.min(12,3+level);i++)rocks.add(new Rock(70+i*820.0/(3+level),95+random.nextDouble()*150,(random.nextDouble()-.5)*45,34+random.nextDouble()*20+Math.min(level,15)*3,38));
        enemiesRemaining=Math.min(8,2+level);spawnClock=.6;
    }
    static double wrap(double p,double min,double span){return min+((p-min)%span+span)%span;}
    void step(double dt){if(dt<=0)return;int n=(int)Math.ceil(Math.min(dt,.05)/.006);for(int i=0;i<n&&!ended;i++)simulate(Math.min(dt,.05)/n);}
    private void simulate(double dt){
        time+=dt;shield=Math.max(0,shield-dt);cooldown-=dt;
        // Facing never changes. Releasing the keys stops direct movement immediately.
        vx=((down("RIGHT","D")?1:0)-(down("LEFT","A")?1:0))*350;
        vy=down("UP","W")?-250:0;
        x=Math.max(48,Math.min(912,x+vx*dt));y=Math.max(115,Math.min(506,y+vy*dt));
        if(down("SPACE")&&cooldown<=0){for(int side:new int[]{-1,1})shots.add(new Shot(x+side*23,y-29,0,-650));cooldown=.14;audio.cue(AudioEngine.Cue.SHOT);}
        for(Rock r:rocks){r.x=wrap(r.x+r.vx*dt,-45,1050);r.y+=r.vy*dt;if(r.y>620)r.y=45;r.spin+=dt*.3;}
        spawnClock-=dt;if(enemiesRemaining>0&&enemies.size()<4&&spawnClock<=0){enemies.add(new Enemy(95+random.nextDouble()*770,72));enemiesRemaining--;spawnClock=Math.max(1.2,3-level*.1);}
        for(int i=enemies.size()-1;i>=0;i--){
            Enemy e=enemies.get(i);e.age+=dt;e.x=e.origin+Math.sin(e.age*1.6)*55;e.y+=(42+Math.min(15,level)*2)*dt;e.cooldown-=dt;
            if(e.cooldown<=0){double dx=x-e.x,dy=y-e.y,dist=Math.max(1,Math.hypot(dx,dy));hostileShots.add(new Shot(e.x,e.y+30,dx/dist*220,dy/dist*220));e.cooldown=1.6;}
            if(e.y>625)enemies.remove(i);
        }
        for(int i=shots.size()-1;i>=0;i--){Shot s=shots.get(i);s.x+=s.vx*dt;s.y+=s.vy*dt;s.life-=dt;boolean hit=false;
            for(int k=enemies.size()-1;k>=0;k--){Enemy e=enemies.get(k);if(Math.hypot(s.x-e.x,s.y-e.y)<27){e.hp--;hit=true;if(e.hp==0){enemies.remove(k);score+=250;bursts.add(new Burst(e.x,e.y));}break;}}
            if(!hit)for(int k=rocks.size()-1;k>=0;k--){Rock r=rocks.get(k);if(distance(s.x,s.y,r.x,r.y)<r.r){
                rocks.remove(k);score+=(int)(120-r.r);bursts.add(new Burst(r.x,r.y));if(r.r>17)for(int a=0;a<2;a++)rocks.add(new Rock(r.x,r.y,r.vx+(a==0?-50:50),r.vy+15,r.r*.55));hit=true;break;
            }}
            if(hit)audio.cue(AudioEngine.Cue.HIT);
            if(hit||s.life<=0||s.y<48)shots.remove(i);
        }
        for(int i=hostileShots.size()-1;i>=0;i--){Shot s=hostileShots.get(i);s.x+=s.vx*dt;s.y+=s.vy*dt;s.life-=dt;
            if(shield==0&&distance(x,y,s.x,s.y)<20){hitPlayer();hostileShots.remove(i);}else if(s.life<=0||s.y>590||s.y<40||s.x<0||s.x>960)hostileShots.remove(i);
        }
        if(shield==0)for(Rock r:rocks)if(distance(x,y,r.x,r.y)<r.r+17){hitPlayer();break;}
        if(shield==0)for(Enemy e:enemies)if(distance(x,y,e.x,e.y)<42){hitPlayer();break;}
        for(int i=bursts.size()-1;i>=0;i--){bursts.get(i).life-=dt;if(bursts.get(i).life<=0)bursts.remove(i);}
        if(rocks.isEmpty()&&enemies.isEmpty()&&enemiesRemaining==0){level++;audio.cue(AudioEngine.Cue.WIN);shield=3;hostileShots.clear();spawnWave();}
    }
    private void hitPlayer(){audio.cue(AudioEngine.Cue.HIT);lives--;bursts.add(new Burst(x,y));shield=3;x=480;y=475;vx=vy=0;if(lives==0){ended=true;paused=true;endTitle="Mission ended";}}
    static double distance(double ax,double ay,double bx,double by){return Math.hypot(ax-bx,ay-by);}
    void drawGame(Graphics2D g){
        g.setPaint(new GradientPaint(0,0,new Color(5,10,31),960,600,new Color(24,18,54)));g.fillRect(0,0,960,600);
        Random stars=new Random(93);for(int i=0;i<155;i++){int sx=stars.nextInt(960);double sy=(stars.nextInt(600)+time*(i%3==0?65:25))%600;g.setColor(new Color(144,177,232,60+stars.nextInt(170)));g.fillOval(sx,(int)sy,i%6==0?3:1,i%6==0?3:1);}
        g.setColor(new Color(42,58,100));g.fillOval(670,80,210,210);g.setColor(new Color(14,26,53));g.fillOval(710,76,185,211);
        for(Rock r:rocks){Path2D p=new Path2D.Double();for(int i=0;i<11;i++){double a=i*2*Math.PI/11+r.spin,rad=r.r*(.8+.2*Math.sin(i*5.7+3));double px=r.x+Math.cos(a)*rad,py=r.y+Math.sin(a)*rad;if(i==0)p.moveTo(px,py);else p.lineTo(px,py);}p.closePath();g.setPaint(new GradientPaint((float)(r.x-r.r),(float)r.y,new Color(111,116,139),(float)(r.x+r.r),(float)(r.y+r.r),new Color(45,49,68)));g.fill(p);g.setStroke(new BasicStroke(1.5f));g.setColor(new Color(155,157,178));g.draw(p);g.setColor(new Color(30,36,57,130));g.fillOval((int)(r.x-r.r*.35),(int)(r.y-r.r*.35),(int)(r.r*.48),(int)(r.r*.35));}
        for(Enemy e:enemies){ShipArt.draw(g,e.x,e.y,.66,true,time,true);g.setColor(new Color(255,104,128));g.fillRect((int)e.x-16,(int)e.y-48,e.hp*11,3);}
        g.setColor(new Color(128,249,255));for(Shot s:shots)g.fillRoundRect((int)s.x-2,(int)s.y-9,4,18,3,3);
        g.setColor(new Color(255,103,118));for(Shot s:hostileShots){g.fillOval((int)s.x-4,(int)s.y-4,8,8);}
        ShipArt.draw(g,x,y,.78,false,time,down("UP","W"));
        if(shield>0){g.setColor(new Color(94,203,255,100));g.setStroke(new BasicStroke(1.5f));g.drawOval((int)x-54,(int)y-57,108,114);}
        for(Burst b:bursts){double radius=(.45-b.life)*100;g.setColor(new Color(255,172,77,(int)(b.life/.45*220)));g.setStroke(new BasicStroke(3));g.drawOval((int)(b.x-radius),(int)(b.y-radius),(int)(radius*2),(int)(radius*2));}
    }
}
