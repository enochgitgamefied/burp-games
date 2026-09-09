package dev.velocity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

/** Shared focus, pause and unload lifecycle for the arcade games. */
abstract class ArcadeGame extends JPanel {
    final AudioEngine audio;
    final AudioEngine.Track musicTrack;
    final Set<String> keys=new HashSet<>();
    boolean started,paused=true,ended,disposed;
    String endTitle="Game over";
    int score,lives=3,level=1;
    double pointerX=480;
    long previous;
    final javax.swing.Timer timer=new javax.swing.Timer(16,e->{
        long now=System.nanoTime();double dt=Math.min(.04,(now-previous)/1e9);previous=now;
        if(started&&!paused&&!ended){step(dt);updateAudio();repaint();}
    });
    ArcadeGame(){this(AudioEngine.silent(),AudioEngine.Track.BREAKOUT);}
    ArcadeGame(AudioEngine audio,AudioEngine.Track track){
        this.audio=audio;this.musicTrack=track;
        setPreferredSize(new Dimension(1200,750));setFocusable(true);setBackground(new Color(5,9,20));
        for(String key:new String[]{"LEFT","RIGHT","UP","DOWN","A","D","W","S","SPACE","ENTER","P","ESCAPE","R","J","K","L"}){
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("pressed "+key),key);
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("released "+key),key+"-up");
            getActionMap().put(key,new AbstractAction(){public void actionPerformed(ActionEvent e){
                if(keys.add(key)){
                    if(key.equals("R")){reset();started=true;paused=false;ended=false;}
                    else if(key.equals("ENTER")){if(ended){reset();paused=true;}started=true;ended=false;paused=!paused;}
                    else if(key.equals("P")){if(started&&!ended)paused=!paused;}
                    else if(key.equals("ESCAPE"))pauseGame();
                    else if(key.equals("SPACE")&&(!started||paused)){started=true;paused=false;}
                    updateAudio();repaint();
                }
            }});
            getActionMap().put(key+"-up",new AbstractAction(){public void actionPerformed(ActionEvent e){keys.remove(key);}});
        }
        addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){requestFocusInWindow();if(!started){started=true;paused=false;}updateAudio();repaint();}});
        addMouseMotionListener(new MouseMotionAdapter(){public void mouseMoved(MouseEvent e){pointerX=e.getX()*960.0/Math.max(1,getWidth());}public void mouseDragged(MouseEvent e){mouseMoved(e);}});
        addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){pauseGame();}});
        addHierarchyListener(e->{if((e.getChangeFlags()&HierarchyEvent.SHOWING_CHANGED)!=0){if(isShowing()&&!disposed){previous=System.nanoTime();timer.start();}else{pauseGame();timer.stop();}}});
    }
    void updateAudio(){
        if(ended){if(audio.isActive(this))audio.finish(this,endCue());}
        else if(started&&!paused&&!disposed)audio.start(this,musicTrack);
        else audio.stop(this);
    }
    AudioEngine.Cue endCue(){return lives>0?AudioEngine.Cue.WIN:AudioEngine.Cue.LOSE;}
    void pauseGame(){paused=true;keys.clear();audio.stop(this);repaint();}
    void dispose(){disposed=true;timer.stop();keys.clear();audio.stop(this);}
    boolean down(String... names){for(String name:names)if(keys.contains(name))return true;return false;}
    abstract void reset();
    abstract void step(double dt);
    abstract void drawGame(Graphics2D g);
    abstract String title();
    abstract String controls();
    @Override protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.scale(getWidth()/960.0,getHeight()/600.0);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            drawGame(g);
            drawHud(g);
            if(!started||paused||ended){
                g.setColor(new Color(0,0,0,150));g.fillRect(0,56,960,514);g.setColor(new Color(17,24,42));g.fillRoundRect(210,190,540,200,22,22);
                label(g,ended?endTitle:started?"Paused":title(),242,247,34,Color.WHITE);
                label(g,ended?endDetail():"Press Enter to "+(started?"resume":"play"),242,292,18,new Color(143,203,255));
                label(g,"P pauses · R restarts · Use All games to switch",242,344,13,new Color(166,182,204));
            }
        }finally{g.dispose();}
    }
    String endDetail(){return "Score: "+score+" · Press R to play again";}
    void drawHud(Graphics2D g){
            g.setColor(new Color(8,13,26,220));g.fillRect(0,0,960,56);g.fillRect(0,570,960,30);
            label(g,title().toUpperCase(),24,35,21,new Color(216,231,255));
            label(g,"SCORE  "+score+"     LIVES  "+lives+"     "+(this instanceof AsteroidsGame?"WAVE  ":"LEVEL  ")+level,570,34,15,Color.WHITE);
            label(g,controls(),24,589,11,new Color(164,185,211));
    }
    static void label(Graphics2D g,String s,int x,int y,int size,Color c){g.setColor(c);g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,size));g.drawString(s,x,y);}
}
