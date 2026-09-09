package dev.velocity;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Solid track scenery and articulated racers, rendered by the original Sonic depth buffer. */
final class KartScene {
    private final Renderer3D renderer=new Renderer3D();
    private final HedgehogRig[][] rigs=new HedgehogRig[4][2];
    private static final Color WHITE=new Color(239,246,240),DARK=new Color(29,43,59),GOLD=new Color(255,199,52);
    KartScene(){for(KartDriver d:KartDriver.values())for(int q=0;q<2;q++)rigs[d.ordinal()][q]=new HedgehogRig(d,q+1);}
    void draw(Graphics2D g,int w,int h,KartRace race,KartRace.Racer viewer){
        KartTrack t=race.track;KartTrack.Theme theme=t.theme;
        g.setPaint(new GradientPaint(0,0,theme.sky(),0,h*.65f,theme.horizon()));g.fillRect(0,0,w,h);
        // Distant silhouettes move continuously with the camera heading.
        double heading=t.heading(viewer.distance);
        g.setColor(theme.horizon());
        for(int i=-1;i<8;i++){int xx=(int)(i*w/5.-heading*w*.12)%(w+w/5);g.fillOval(xx-w/5,h/4,w/2,h/3);}
        double scale=Math.min(1,(race.players==1?900.0:800.0)/w);
        renderer.begin(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)),viewer.x,viewer.distance,false,0);renderer.follow(t,viewer.distance);renderer.pace(viewer.speed);renderer.atmosphere(theme.horizon());
        double first=Math.floor((viewer.distance-18)/5)*5;
        for(double s=first;s<viewer.distance+225;s+=5){
            strip(theme.ground(),-160,160,-.9,s,s+5);
            strip(theme.verge(),-24,24,-.12,s,s+5);
            strip(theme.road(),-9,9,0,s,s+5);
            Color curb=((long)Math.floor(s/5)&1)==0?WHITE:theme.accent();
            strip(curb,-9.6,-9,.025,s,s+5);strip(curb,9,9.6,.025,s,s+5);
            if(((long)Math.floor(s/5)&1)==0){strip(WHITE,-3.03,-2.97,.018,s,s+3);strip(WHITE,2.97,3.03,.018,s,s+3);}
        }
        // Each object has a fixed location; no random per-frame placement or sprite clipping.
        int spacing=19,count=t.length/spacing;
        for(int i=0;i<count;i++){
            double at=i*t.length/(double)count,rel=race.relative(at,viewer.distance);
            if(rel < -18||rel>205)continue;double z=viewer.distance+rel;
            double side=(i%2==0?-1:1),x=side*(13+(i%3)*3.5);
            prop(t.level,x,z,i);
        }
        for(int i=0;i<4;i++){
            double rel=race.relative(t.pad(i),viewer.distance);if(rel < -10||rel>210)continue;double z=viewer.distance+rel,x=t.padLane(i);
            strip(new Color(27,110,139),x-1.8,x+1.8,.035,z-3,z+3);
            for(int k=-1;k<=1;k++)renderer.polygon(theme.accent(),x-1.35,.055,z+k*1.6-.5,x,.055,z+k*1.6+.5,x+1.35,.055,z+k*1.6-.5,x,.055,z+k*1.6+1.15);
        }
        for(int i=0;i<t.coinCount();i++){
            double rel=race.relative(t.coin(i),viewer.distance),z=viewer.distance+rel;int lap=(int)Math.floor(z/t.length);
            if(rel < -2||rel>180||lap<0||lap>=KartRace.LAPS||race.collected[lap][i])continue;
            coin(t.coinLane(i),1.25,z,race.elapsed*.75+i*.2);
        }
        double finish=viewer.distance+race.relative(0,viewer.distance);
        if(finish-viewer.distance > -12&&finish-viewer.distance<210){
            for(int row=0;row<2;row++)for(int col=0;col<12;col++)strip((row+col)%2==0?WHITE:DARK,-9+col*1.5,-7.5+col*1.5,.04,finish-2+row*1.5,finish-.5+row*1.5);
            box(theme.accent(),-10,3.7,finish,.28,3.7,.3);box(theme.accent(),10,3.7,finish,.28,3.7,.3);
            box(DARK,0,7.5,finish,10.3,.65,.32);
            for(int i=-9;i<=9;i++)box(i%2==0?WHITE:theme.accent(),i,7.5,finish-.34,.38,.34,.03);
        }
        for(KartRace.Racer r:race.racers){
            double rel=race.relative(r.distance,viewer.distance);if(r!=viewer&&(rel < -10||rel>155))continue;
            double z=viewer.distance+rel;
            strip(new Color(47,57,58),r.x-1.03,r.x+1.03,.045,z-1.45,z+1.45);
            rigs[r.driver.ordinal()][r==viewer||Math.abs(rel)<7?0:1].drawKart(renderer,r.x,z,r.steering,r.wheels,r.speed);
            if((r.boost&&r.energy>1||r.padBoost>0)&&r.speed>30){
                cone(new Color(103,230,255),r.x-.53,.44,z-1.8,.19,.38,5);
                cone(new Color(255,208,62),r.x+.53,.44,z-1.8,.19,.38,5);
            }
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(renderer.image(),0,0,w,h,null);
    }
    private void strip(Color c,double l,double r,double y,double a,double b){renderer.polygon(c,l,y,a,r,y,a,r,y,b,l,y,b);}
    private void box(Color c,double x,double y,double z,double w,double h,double d){
        renderer.polygon(c,x-w,y-h,z-d,x+w,y-h,z-d,x+w,y+h,z-d,x-w,y+h,z-d);
        renderer.polygon(c.darker(),x-w,y-h,z+d,x-w,y+h,z+d,x+w,y+h,z+d,x+w,y-h,z+d);
        renderer.polygon(c.darker(),x-w,y-h,z-d,x-w,y+h,z-d,x-w,y+h,z+d,x-w,y-h,z+d);
        renderer.polygon(c,x+w,y-h,z-d,x+w,y-h,z+d,x+w,y+h,z+d,x+w,y+h,z-d);
        renderer.polygon(c.brighter(),x-w,y+h,z-d,x+w,y+h,z-d,x+w,y+h,z+d,x-w,y+h,z+d);
    }
    private void cone(Color c,double x,double y,double z,double radius,double height,int n){
        for(int i=0;i<n;i++){double a=i*Math.PI*2/n,b=(i+1)*Math.PI*2/n;renderer.polygon(i%3==0?c.brighter():i%3==1?c:c.darker(),x+Math.cos(a)*radius,y,z+Math.sin(a)*radius,x,y+height,z,x+Math.cos(b)*radius,y,z+Math.sin(b)*radius);}
    }
    private void prop(int theme,double x,double z,int i){
        double size=1+(i%4)*.12;
        switch(theme){
            case 0->{
                box(new Color(128,92,55),x,2.7*size,z,.26,2.7*size,.26);
                for(int k=0;k<6;k++){double a=k*Math.PI/3;renderer.polygon(k%2==0?new Color(40,137,86):new Color(80,174,85),x,6*size,z,x+Math.cos(a+.35)*1.4*size,5.4*size,z+Math.sin(a+.35)*1.4*size,x+Math.cos(a)*3.4*size,4.5*size,z+Math.sin(a)*3.4*size,x+Math.cos(a-.35)*1.4*size,5.4*size,z+Math.sin(a-.35)*1.4*size);}
                cone(new Color(212,195,139),x*1.5,-.1,z+7,3.5,1.7,7);
            }
            case 1->{
                if(i%3==0){box(new Color(154,82,63),x,3.8*size,z,2,3.8*size,2.4);box(new Color(198,120,78),x,7.6*size,z,2.5,.8,2.7);}
                else{Color green=new Color(69,117,87);box(green,x,2.6*size,z,.45,2.6*size,.45);box(green,x-1,2.5*size,z,1,.3,.35);box(green,x-1.7,3.1*size,z,.3,.8,.35);box(green,x+1,3.5*size,z,1,.3,.35);box(green,x+1.7,4*size,z,.3,.8,.35);}
            }
            case 2->{
                box(new Color(102,94,90),x,1.1,z,.28,1.1,.28);
                for(int k=0;k<3;k++){cone(new Color(58,112,126),x,1+k*1.6*size,z,(2.3-k*.5)*size,3*size,7);cone(new Color(229,242,245),x,2+k*1.6*size,z,(1.75-k*.42)*size,2*size,7);}
            }
            case 3->{
                Color body=new Color(28+(i%3)*9,42,74);double height=(6+i%5*2)*size;box(body,x,height/2,z,2,height/2,2.4);
                for(int floor=1;floor<height;floor+=2)box(i%2==0?new Color(85,220,214):new Color(232,120,198),x,floor,z-2.43,1.5,.18,.04);
                box(new Color(88,214,215),x,height+.1,z,2.1,.1,2.5);
            }
            default->{
                box(new Color(109,86,111),x,2.2*size,z,.28,2.2*size,.28);
                cone(new Color(233,151,188),x,2.5*size,z,2.8*size,3.6*size,9);cone(new Color(255,202,214),x,4*size,z,2*size,2.5*size,9);
                cone(new Color(217,220,241),x*1.6,-.1,z+7,3,1.4,7);
            }
        }
    }
    private void coin(double x,double y,double z,double angle){
        // A thick twelve-sided coin keeps a visible profile at every angle.
        int n=12;double c=Math.cos(angle),s=Math.sin(angle),r=.58,d=.18;
        for(int i=0;i<n;i++){
            double a=i*Math.PI*2/n,b=(i+1)*Math.PI*2/n,ax=Math.cos(a)*r,ay=Math.sin(a)*r,bx=Math.cos(b)*r,by=Math.sin(b)*r;
            renderer.polygon(GOLD,x+c*ax-s*d,y+ay,z+s*ax+c*d,x+c*bx-s*d,y+by,z+s*bx+c*d,x-s*d,y,z+c*d);
            renderer.polygon(GOLD,x+c*bx+s*d,y+by,z+s*bx-c*d,x+c*ax+s*d,y+ay,z+s*ax-c*d,x+s*d,y,z-c*d);
            renderer.polygon(new Color(192,131,32),x+c*ax-s*d,y+ay,z+s*ax+c*d,x+c*ax+s*d,y+ay,z+s*ax-c*d,x+c*bx+s*d,y+by,z+s*bx-c*d,x+c*bx-s*d,y+by,z+s*bx+c*d);
        }
    }
    static BufferedImage portrait(KartDriver driver,int w,int h){
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setPaint(new GradientPaint(0,0,new Color(27,42,64),w,h,driver.livery.darker()));g.fillRect(0,0,w,h);
        g.setColor(new Color(255,255,255,15));g.fillOval(w/6,-h/4,w,w);
        Renderer3D r=new Renderer3D();r.portrait(w,h,1.75,5.5,Math.toRadians(153));
        new HedgehogRig(driver,0).drawKart(r,0,0,0,0,0);g.drawImage(r.image(),0,0,null);g.dispose();return image;
    }
    static BufferedImage cover(){
        Progress p=new Progress(false);KartRace race=new KartRace(p);race.start(0,0,1,1);race.state=KartRace.State.RACING;
        race.player1.distance=155;race.player1.x=1.8;race.player1.speed=98;
        for(int i=0;i<3;i++){race.racers.get(i).distance=164+i*7;race.racers.get(i).x=-5+i*4;}
        BufferedImage image=new BufferedImage(960,540,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();new KartScene().draw(g,960,540,race,race.player1);
        g.setPaint(new GradientPaint(0,350,new Color(6,14,29,0),0,540,new Color(6,14,29,245)));g.fillRect(0,350,960,190);
        ArcadeGame.label(g,"TURBO TAILS",38,456,60,Color.WHITE);ArcadeGame.label(g,"3D KART RACING  /  FOUR RIVALS. FIVE CIRCUITS.",41,499,20,new Color(255,218,109));g.dispose();return image;
    }
}
