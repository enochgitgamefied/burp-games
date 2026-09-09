package dev.velocity;

import java.awt.*;

final class BreakoutGame extends ArcadeGame {
    final boolean[][] bricks=new boolean[6][10];
    double ballX=480,ballY=515,vx=180,vy=-270,paddle=480;
    boolean launched;
    BreakoutGame(){this(AudioEngine.silent());}
    BreakoutGame(AudioEngine audio){super(audio,AudioEngine.Track.BREAKOUT);reset();}
    String title(){return "Breakout";}
    String controls(){return "MOUSE or A / D or ARROWS  PADDLE     SPACE  LAUNCH     ENTER  PLAY     P  PAUSE     R  RESTART";}
    void reset(){score=0;lives=3;level=1;ended=false;paddle=pointerX=480;fillBricks();serve();}
    void fillBricks(){for(boolean[] row:bricks)java.util.Arrays.fill(row,true);}
    void serve(){launched=false;ballX=paddle;ballY=515;vx=160+level*20;vy=-260-level*25;}
    void step(double dt){
        if(down("LEFT","A")){paddle-=550*dt;pointerX=paddle;}else if(down("RIGHT","D")){paddle+=550*dt;pointerX=paddle;}else paddle=pointerX;
        paddle=Math.max(63,Math.min(897,paddle));
        if(!launched){ballX=paddle;ballY=515;if(down("SPACE")){launched=true;audio.cue(AudioEngine.Cue.BOUNCE);}else return;}
        int steps=(int)Math.ceil(dt/.004);double sub=dt/steps;
        for(int n=0;n<steps;n++){
            double oldX=ballX,oldY=ballY;ballX+=vx*sub;ballY+=vy*sub;
            if(ballX<9){ballX=9;vx=Math.abs(vx);}if(ballX>951){ballX=951;vx=-Math.abs(vx);}if(ballY<66){ballY=66;vy=Math.abs(vy);}
            if(vy>0&&ballY+8>=530&&oldY+8<=530&&Math.abs(ballX-paddle)<69){
                audio.cue(AudioEngine.Cue.BOUNCE);ballY=521;double speed=Math.min(590,Math.hypot(vx,vy)+9),angle=(ballX-paddle)/69*1.05;
                vx=Math.sin(angle)*speed;vy=-Math.cos(angle)*speed;
            }
            boolean hit=false;
            for(int row=0;row<6&&!hit;row++)for(int col=0;col<10;col++)if(bricks[row][col]){
                double left=35+col*89,top=99+row*33;
                if(ballX+8>left&&ballX-8<left+81&&ballY+8>top&&ballY-8<top+25){
                    bricks[row][col]=false;audio.cue(AudioEngine.Cue.BRICK);score+=(6-row)*10;
                    if(oldY+8<=top){ballY=top-8;vy=-Math.abs(vy);}else if(oldY-8>=top+25){ballY=top+33;vy=Math.abs(vy);}else if(oldX<left){ballX=left-8;vx=-Math.abs(vx);}else{ballX=left+89;vx=Math.abs(vx);}hit=true;break;
                }
            }
            if(ballY>570){audio.cue(AudioEngine.Cue.LOSE);lives--;if(lives<=0){ended=true;paused=true;endTitle="Out of lives";}serve();return;}
            boolean any=false;for(boolean[] row:bricks)for(boolean b:row)any|=b;
            if(!any){if(level==3){ended=true;paused=true;endTitle="All levels cleared!";}else{level++;audio.cue(AudioEngine.Cue.WIN);fillBricks();serve();}return;}
        }
    }
    void drawGame(Graphics2D g){
        g.setPaint(new GradientPaint(0,0,new Color(18,9,42),960,600,new Color(13,28,57)));g.fillRect(0,0,960,600);
        g.setColor(new Color(42,42,72));for(int x=0;x<960;x+=40)g.drawLine(x,56,x,570);for(int y=56;y<570;y+=40)g.drawLine(0,y,960,y);
        Color[] colors={new Color(255,102,140),new Color(252,148,105),new Color(255,207,100),new Color(108,221,178),new Color(89,190,255),new Color(165,137,255)};
        for(int row=0;row<6;row++)for(int col=0;col<10;col++)if(bricks[row][col]){g.setColor(colors[row]);g.fillRoundRect(35+col*89,99+row*33,81,25,6,6);g.setColor(new Color(255,255,255,85));g.fillRoundRect(40+col*89,102+row*33,71,3,3,3);}
        g.setColor(new Color(102,230,255,40));g.fillRoundRect((int)paddle-68,524,136,25,16,16);g.setColor(new Color(156,230,255));g.fillRoundRect((int)paddle-60,530,120,12,10,10);
        g.setColor(new Color(255,255,255,50));g.fillOval((int)ballX-15,(int)ballY-15,30,30);g.setColor(Color.WHITE);g.fillOval((int)ballX-8,(int)ballY-8,16,16);
        if(!launched)label(g,"SPACE TO LAUNCH",397,470,16,new Color(199,188,243));
    }
}
