package dev.velocity;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class ArcadeTest {
    static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container container)layout(container);}
    static void save(JPanel panel,File file,int width,int height)throws Exception{
        panel.setSize(width,height);for(int i=0;i<3;i++)layout(panel);BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();ImageIO.write(image,"png",file);
    }
    public static void main(String[] args)throws Exception{
        SwingUtilities.invokeAndWait(()->{try{
            AsteroidsGame ast=new AsteroidsGame();ast.rocks.clear();ast.rocks.add(new AsteroidsGame.Rock(600,320,0,0,40));ast.shots.add(new AsteroidsGame.Shot(600,320,0,0));ast.step(.01);
            check(ast.rocks.size()==2&&ast.score>0,"asteroid splits and scores");
            ast.rocks.clear();ast.enemies.clear();ast.enemiesRemaining=0;ast.step(.01);check(ast.level==2&&!ast.rocks.isEmpty(),"new wave");
            ast.shield=0;ast.rocks.clear();ast.rocks.add(new AsteroidsGame.Rock(ast.x,ast.y,0,0,20));ast.step(.01);check(ast.lives==2&&ast.shield>0,"ship collision grants shield");
            ast.started=true;ast.paused=false;ast.pauseGame();check(ast.paused&&ast.keys.isEmpty(),"pause clears input");ast.dispose();check(!ast.timer.isRunning(),"dispose stops timer");
            BreakoutGame b=new BreakoutGame();b.launched=true;b.ballX=75;b.ballY=91;b.vx=0;b.vy=300;b.step(.02);check(!b.bricks[0][0]&&b.vy<0&&b.score>0,"brick collision reflects and scores");
            b.ballX=480;b.ballY=520;b.vy=300;b.step(.02);check(b.vy<0,"paddle bounces");
            b.ballX=10;b.ballY=569;b.vy=400;b.step(.02);check(b.lives==2&&!b.launched,"lost ball serves next life");
            for(boolean[] row:b.bricks)java.util.Arrays.fill(row,false);b.launched=true;b.ballY=400;b.step(.004);check(b.level==2&&!b.launched,"next breakout level");b.dispose();
            GamesPanel hub=new GamesPanel(new Progress(false));hub.openGame("Asteroids");AsteroidsGame first=(AsteroidsGame)hub.game("Asteroids");first.started=true;first.paused=false;hub.showLibrary();check(first.paused,"switching pauses old game");hub.openGame("Asteroids");check(hub.game("Asteroids")==first,"session preserved");hub.showLibrary();
            if(args.length>0){
                File dir=new File(args[0]);save(hub,new File(dir,"games-library.png"),1400,820);save(hub,new File(dir,"games-library-narrow.png"),640,900);
                hub.openGame("Sonic 3D");GamePanel sonic=(GamePanel)hub.game("Sonic 3D");save(sonic,new File(dir,"sonic-level-select.png"),1200,800);
                sonic.game.togglePause();sonic.game.z=65;sonic.game.speed=88;sonic.game.elapsed=3.6;sonic.game.wheelAngle=2;save(sonic,new File(dir,"sonic-gameplay.png"),1200,800);
                sonic.game.state=Game.State.CHALLENGE;save(sonic,new File(dir,"sonic-mission.png"),1200,800);
                hub.openGame("Asteroids");first.reset();first.enemies.add(new AsteroidsGame.Enemy(270,205));first.enemies.add(new AsteroidsGame.Enemy(645,215));first.hostileShots.add(new AsteroidsGame.Shot(405,320,0,180));first.shots.add(new AsteroidsGame.Shot(457,400,0,-650));first.started=true;first.paused=false;save(first,new File(dir,"asteroids.png"),1200,800);
                hub.openGame("Breakout");BreakoutGame bricks=(BreakoutGame)hub.game("Breakout");bricks.started=true;bricks.paused=false;bricks.launched=true;bricks.ballY=410;save(bricks,new File(dir,"breakout.png"),1200,800);
            }
            hub.dispose();check(!first.timer.isRunning(),"hub unload disposes games");
            System.out.println("PASS: Asteroids combat/waves, Breakout collisions/levels, library switching and cleanup, screenshots.");
        }catch(Exception e){throw new RuntimeException(e);}});
    }
}
