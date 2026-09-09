package dev.velocity;

import java.awt.*;import java.awt.event.KeyEvent;import java.awt.image.BufferedImage;import java.io.File;import java.util.Arrays;import javax.imageio.ImageIO;import javax.swing.*;

/** Full native views and stable-frame checks, using the same UI paint path as Burp. */
public final class KartRenderTest {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static BufferedImage paint(TurboTailsGame ui,int w,int h){ui.setSize(w,h);for(int n=0;n<3;n++)ArcadeTest.layout(ui);BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();ui.paint(g);g.dispose();return im;}
    public static void main(String[] args)throws Exception{
        File dir=new File(args.length>0?args[0]:"screenshots");dir.mkdirs();
        SwingUtilities.invokeAndWait(()->{try{
            Progress p=new Progress(false);p.turboUnlocked=4;AudioEngine audio=AudioEngine.silent();TurboTailsGame ui=new TurboTailsGame(p,audio,p::setMuted);
            ImageIO.write(paint(ui,1280,750),"png",new File(dir,"kart-garage.png"));
            BufferedImage roster=new BufferedImage(1400,430,BufferedImage.TYPE_INT_RGB);Graphics2D rg=roster.createGraphics();for(KartDriver d:KartDriver.values()){rg.drawImage(KartScene.portrait(d,350,360),d.ordinal()*350,0,null);ArcadeGame.label(rg,d.label,d.ordinal()*350+25,406,28,Color.WHITE);}rg.dispose();ImageIO.write(roster,"png",new File(dir,"kart-roster.png"));
            for(int level=0;level<5;level++){
                ui.level=level;ui.startRace();ui.race.update(3);ui.race.player1.distance=155;ui.race.player1.x=1.8;ui.race.player1.speed=98;
                for(int i=0;i<3;i++){ui.race.racers.get(i).distance=164+i*7;ui.race.racers.get(i).x=-5+i*4;}ui.race.elapsed=6;
                BufferedImage a=paint(ui,1280,750),b=paint(ui,1280,750);
                check(Arrays.equals(a.getRGB(0,0,1280,750,null,0,1280),b.getRGB(0,0,1280,750,null,0,1280)),"identical state renders identical scenery and coins: "+level);
                ImageIO.write(a,"png",new File(dir,"kart-circuit-"+(level+1)+".png"));
            }
            ui.level=0;ui.startRace();ui.key(KeyEvent.VK_W,true);ui.key(KeyEvent.VK_D,true);ui.applyInputs();check(ui.race.player1.throttle&&ui.race.player1.steering==1,"native keyboard acceleration and steering");
            ui.key(KeyEvent.VK_P,true);ui.key(KeyEvent.VK_P,true);check(ui.race.state==KartRace.State.PAUSED,"held pause key cannot repeat-toggle");ui.key(KeyEvent.VK_P,false);
            ImageIO.write(paint(ui,1280,750),"png",new File(dir,"kart-pause.png"));ui.key(KeyEvent.VK_ENTER,true);ui.key(KeyEvent.VK_ENTER,false);check(ui.race.state==KartRace.State.COUNTDOWN,"Enter resumes countdown");
            ui.key(KeyEvent.VK_M,true);check(audio.isMuted()&&p.muted,"native M mute");ui.key(KeyEvent.VK_M,false);
            ui.players=2;ui.startRace();ui.key(KeyEvent.VK_A,true);ui.key(KeyEvent.VK_RIGHT,true);ui.key(KeyEvent.VK_ENTER,true);ui.applyInputs();check(ui.race.player1.steering==-1&&ui.race.player2.steering==1&&ui.race.player2.boost&&!ui.race.player1.boost,"split controls isolated");
            ui.race.update(3);ui.race.clear();ui.held.clear();ui.race.elapsed=4;ImageIO.write(paint(ui,1280,850),"png",new File(dir,"kart-split-screen.png"));
            ui.deactivate();check(ui.race.state==KartRace.State.PAUSED&&!audio.isActive(ui)&&ui.held.isEmpty(),"deactivation pauses and stops audio");
            ui.players=1;ui.startRace();ui.race.update(3);for(int i=0;i<60*70&&ui.race.state==KartRace.State.RACING;i++){ui.race.player1.throttle=true;ui.race.update(1./60);}check(ui.race.state==KartRace.State.FINISHED,"results available");ImageIO.write(paint(ui,1280,750),"png",new File(dir,"kart-results.png"));
            benchmark(ui,1,1280,750);benchmark(ui,2,1280,850);
            ui.dispose();audio.close();check(!ui.timer.isRunning()&&ui.disposed,"unload releases native timer");
        }catch(Exception e){throw new RuntimeException(e);}});
        System.out.println("PASS: native portraits, all themes, deterministic frames, key controls, pause, mute, split-screen, results and lifecycle.");
    }
    static void benchmark(TurboTailsGame ui,int players,int w,int h){
        ui.players=players;ui.level=0;ui.startRace();ui.race.update(3);ui.setSize(w,h);ArcadeTest.layout(ui);
        BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();long[] frames=new long[90];
        for(int i=-15;i<90;i++){ui.race.player1.throttle=true;if(ui.race.player2!=null)ui.race.player2.throttle=true;long began=System.nanoTime();ui.race.update(1./60);ui.paint(g);if(i>=0)frames[i]=System.nanoTime()-began;}
        g.dispose();Arrays.sort(frames);double median=frames[45]/1e6,p95=frames[85]/1e6;System.out.printf("Native kart %dP at %dx%d: median %.2f ms, p95 %.2f ms (%d frames)%n",players,w,h,median,p95,frames.length);
        check(median<45,"native race frame budget (median <45ms)");
    }
}
