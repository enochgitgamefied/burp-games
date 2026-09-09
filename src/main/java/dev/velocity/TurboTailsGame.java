package dev.velocity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Native Swing kart racer sharing Sonic's renderer, rig and track-relative movement. */
final class TurboTailsGame extends JPanel {
    static final Color BG=new Color(10,18,33),PANEL=new Color(23,35,54),INK=new Color(233,241,250),MUTED=new Color(151,170,192),GOLD=new Color(255,215,101);
    final Progress progress;final AudioEngine audio;final Consumer<Boolean> muteChanged;
    final KartRace race;final KartScene scene=new KartScene();
    final CardLayout pages=new CardLayout();final RaceCanvas canvas=new RaceCanvas();
    final JPanel garage=new GaragePanel();
    final DriverCard[] driverCards=new DriverCard[4];final JButton[] trackButtons=new JButton[5];
    final JButton modeButton=button("1 player"),secondButton=button("Player 2: Tails"),startButton=button("Start race  →"),soundButton=button("Sound on");
    final JLabel circuitInfo=new JLabel(),selectionInfo=new JLabel();
    final Set<Integer> held=new HashSet<>(),commandsDown=new HashSet<>();
    int first,second,players,level;boolean disposed;long previous;int heardCoins,heardHits,heardPads;
    final Timer timer=new Timer(16,e->tick());
    TurboTailsGame(Progress progress,AudioEngine audio,Consumer<Boolean> muteChanged){
        this.progress=progress;this.audio=audio;this.muteChanged=muteChanged;race=new KartRace(progress);
        first=progress.kartDriver;second=progress.kartSecond;players=progress.kartPlayers;
        setLayout(pages);setBackground(BG);setPreferredSize(new Dimension(1200,760));
        buildGarage();JScrollPane scroll=new JScrollPane(garage);scroll.setBorder(null);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scroll.getVerticalScrollBar().setUnitIncrement(22);scroll.getViewport().setBackground(BG);
        add(scroll,"garage");add(canvas,"race");pages.show(this,"garage");refreshGarage();
        timer.setCoalesce(true);
        addHierarchyListener(e->{if((e.getChangeFlags()&HierarchyEvent.SHOWING_CHANGED)!=0){if(isShowing()&&!disposed){previous=System.nanoTime();timer.start();}else{deactivate();timer.stop();}}});
    }
    private void buildGarage(){
        garage.setBackground(BG);garage.setBorder(BorderFactory.createEmptyBorder(26,30,26,30));
        JPanel heading=new JPanel(new BorderLayout());heading.setOpaque(false);JPanel titles=vertical();
        titles.add(label("TURBO TAILS",32,INK));titles.add(label("3D KART RACING   /   FOUR RIVALS. FIVE CIRCUITS.",12,GOLD));heading.add(titles,BorderLayout.CENTER);
        soundButton.addActionListener(e->toggleMute());heading.add(soundButton,BorderLayout.EAST);garage.add(heading,BorderLayout.NORTH);
        JPanel middle=vertical();JPanel options=new JPanel(new FlowLayout(FlowLayout.LEFT,0,8));options.setOpaque(false);options.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));
        modeButton.addActionListener(e->{players=players==1?2:1;refreshGarage();});secondButton.addActionListener(e->{do{second=(second+1)%4;}while(second==first);refreshGarage();});
        options.add(modeButton);options.add(Box.createHorizontalStrut(12));options.add(secondButton);middle.add(options);
        selectionInfo.setForeground(MUTED);selectionInfo.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));middle.add(selectionInfo);middle.add(Box.createVerticalStrut(12));
        JPanel drivers=new JPanel(new GridLayout(1,4,12,0));drivers.setOpaque(false);drivers.setPreferredSize(new Dimension(920,282));drivers.setMaximumSize(new Dimension(Integer.MAX_VALUE,310));
        for(KartDriver d:KartDriver.values()){DriverCard card=new DriverCard(d);driverCards[d.ordinal()]=card;card.addActionListener(e->{first=d.ordinal();if(second==first)second=(first+1)%4;refreshGarage();});drivers.add(card);}middle.add(drivers);
        middle.add(Box.createVerticalStrut(24));middle.add(label("CHOOSE YOUR CIRCUIT",13,MUTED));middle.add(Box.createVerticalStrut(10));
        JPanel circuits=new JPanel(new GridLayout(1,5,8,0));circuits.setOpaque(false);circuits.setPreferredSize(new Dimension(920,56));circuits.setMaximumSize(new Dimension(Integer.MAX_VALUE,56));
        for(int i=0;i<5;i++){final int track=i;trackButtons[i]=button(KartTrack.THEMES[i].name());trackButtons[i].setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));trackButtons[i].addActionListener(e->{level=track;refreshGarage();});circuits.add(trackButtons[i]);}middle.add(circuits);
        circuitInfo.setForeground(MUTED);circuitInfo.setBorder(BorderFactory.createEmptyBorder(12,0,0,0));middle.add(circuitInfo);garage.add(middle,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout(24,0));footer.setOpaque(false);JPanel controls=vertical();controls.add(label("WASD steer / accelerate / brake    •    Shift boost    •    Space drift",13,INK));
        controls.add(Box.createVerticalStrut(6));controls.add(label("Player 2: arrows + Enter boost + Ctrl drift    •    P / Esc pause    •    M mute",12,MUTED));
        controls.add(Box.createVerticalStrut(6));controls.add(label("Continuous driving. Hold accelerate for full speed. Finish top 2 solo to unlock the next circuit.",12,MUTED));footer.add(controls,BorderLayout.CENTER);
        startButton.setBackground(GOLD);startButton.setForeground(BG);startButton.setPreferredSize(new Dimension(175,58));startButton.addActionListener(e->startRace());footer.add(startButton,BorderLayout.EAST);garage.add(footer,BorderLayout.SOUTH);
    }
    private void refreshGarage(){
        modeButton.setText(players==1?"1 player  ·  change":"2 players  ·  change");secondButton.setVisible(players==2);secondButton.setText("Player 2: "+KartDriver.values()[second].label+"  ›");
        selectionInfo.setText(players==1?"Choose your driver. The other three racers are computer opponents.":"Choose Player 1 below. Change Player 2 with the button above. Two computer opponents join the race.");
        for(DriverCard card:driverCards)card.repaint();
        for(int i=0;i<5;i++){JButton b=trackButtons[i];b.setEnabled(i<=progress.turboUnlocked);b.setText((i>progress.turboUnlocked?"Locked: ":"")+KartTrack.THEMES[i].name());b.setBackground(i==level?new Color(56,85,110):PANEL);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(i==level?GOLD:PANEL,2),BorderFactory.createEmptyBorder(10,6,10,6)));}
        String record=progress.kartBest[level]>0?"  •  Solo best "+time(progress.kartBest[level]):"";
        circuitInfo.setText("2 laps  •  Hills & banked turns  •  Coins recharge boost  •  Cyan pads give a speed burst"+record);
        setMuted(progress.muted);garage.revalidate();garage.repaint();
    }
    void startRace(){
        if(disposed)return;progress.chooseKart(first,second,players);race.start(level,first,second,players);held.clear();heardCoins=heardHits=heardPads=0;
        previous=System.nanoTime();pages.show(this,"race");canvas.requestFocusInWindow();updateAudio();canvas.repaint();
    }
    void showGarage(){race.menu();held.clear();audio.stop(this);pages.show(this,"garage");refreshGarage();}
    void togglePause(){
        if(race.state==KartRace.State.PAUSED){race.resume();previous=System.nanoTime();}else race.pause();held.clear();updateAudio();canvas.repaint();
    }
    void deactivate(){race.pause();held.clear();commandsDown.clear();audio.stop(this);canvas.repaint();}
    void pauseGame(){deactivate();}
    void dispose(){disposed=true;timer.stop();deactivate();}
    void setMuted(boolean value){soundButton.setText(value?"Sound off  ·  M":"Sound on  ·  M");canvas.repaint();}
    private void toggleMute(){boolean value=!progress.muted;progress.setMuted(value);audio.setMuted(value);muteChanged.accept(value);setMuted(value);}
    private void tick(){
        long now=System.nanoTime();double dt=(now-previous)/1e9;previous=now;
        if(disposed||!isShowing())return;
        if(race.state==KartRace.State.COUNTDOWN||race.state==KartRace.State.RACING){applyInputs();race.update(dt);updateAudio();canvas.repaint();}
    }
    void applyInputs(){
        input(race.player1,KeyEvent.VK_A,KeyEvent.VK_D,KeyEvent.VK_W,KeyEvent.VK_S,KeyEvent.VK_SHIFT,KeyEvent.VK_SPACE);
        if(players==1&&race.player1!=null&&!race.player1.finished()){
            if(down(KeyEvent.VK_LEFT)||down(KeyEvent.VK_RIGHT))race.player1.steering=(down(KeyEvent.VK_RIGHT)?1:0)-(down(KeyEvent.VK_LEFT)?1:0);
            race.player1.throttle|=down(KeyEvent.VK_UP);race.player1.brake|=down(KeyEvent.VK_DOWN);
        }
        input(race.player2,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_ENTER,KeyEvent.VK_CONTROL);
    }
    private void input(KartRace.Racer r,int left,int right,int up,int down,int boost,int drift){
        if(r==null||r.finished())return;r.steering=(down(right)?1:0)-(down(left)?1:0);r.throttle=down(up);r.brake=down(down);r.boost=down(boost);r.drift=down(drift);
    }
    private boolean down(int key){return held.contains(key);}
    void key(int code,boolean pressed){
        if(!pressed){held.remove(code);commandsDown.remove(code);return;}
        boolean command=code==KeyEvent.VK_M||code==KeyEvent.VK_P||code==KeyEvent.VK_ESCAPE||code==KeyEvent.VK_R;
        if(command&&!commandsDown.add(code))return;if(!held.add(code))return;
        if(code==KeyEvent.VK_M)toggleMute();
        else if(code==KeyEvent.VK_P||code==KeyEvent.VK_ESCAPE)togglePause();
        else if(code==KeyEvent.VK_R&&race.state!=KartRace.State.MENU)startRace();
        else if(code==KeyEvent.VK_ENTER&&race.state==KartRace.State.PAUSED)togglePause();
        else if(code==KeyEvent.VK_ENTER&&race.state==KartRace.State.FINISHED)startRace();
    }
    private void updateAudio(){
        if(race.state==KartRace.State.COUNTDOWN||race.state==KartRace.State.RACING){
            audio.start(this,AudioEngine.Track.KART);audio.setKartSpeed(race.player1.speed/130);
            if(race.coinEvents>heardCoins)audio.cue(AudioEngine.Cue.COIN);
            if(race.hitEvents>heardHits)audio.cue(AudioEngine.Cue.HIT);
            if(race.padEvents>heardPads)audio.cue(AudioEngine.Cue.PICKUP);
        }else if(race.state==KartRace.State.FINISHED&&audio.isActive(this))audio.finish(this,AudioEngine.Cue.WIN);else audio.stop(this);
        heardCoins=race.coinEvents;heardHits=race.hitEvents;heardPads=race.padEvents;
    }
    static String time(double seconds){if(seconds<0)return "—";int ms=(int)Math.round(seconds*1000);return String.format(java.util.Locale.ROOT,"%d:%02d.%03d",ms/60000,(ms/1000)%60,ms%1000);}
    private static JPanel vertical(){JPanel p=new JPanel(){@Override protected void addImpl(Component c,Object constraints,int index){if(c instanceof JComponent child)child.setAlignmentX(LEFT_ALIGNMENT);super.addImpl(c,constraints,index);}};p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));return p;}
    private static JLabel label(String text,int size,Color color){JLabel l=new JLabel(text);l.setFont(new Font(Font.SANS_SERIF,Font.BOLD,size));l.setForeground(color);return l;}
    private static JButton button(String text){JButton b=new JButton(text);b.setFont(new Font(Font.SANS_SERIF,Font.BOLD,13));b.setBackground(PANEL);b.setForeground(INK);b.setFocusPainted(false);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(55,73,95)),BorderFactory.createEmptyBorder(10,13,10,13)));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;}
    private static final class GaragePanel extends JPanel implements Scrollable {
        GaragePanel(){super(new BorderLayout(0,20));}
        public Dimension getPreferredScrollableViewportSize(){return new Dimension(1200,750);}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 22;}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(22,r.height-30);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return getParent()!=null&&getPreferredSize().height<getParent().getHeight();}
    }
    private final class DriverCard extends JButton {
        final KartDriver driver;final BufferedImage portrait;
        DriverCard(KartDriver driver){this.driver=driver;portrait=KartScene.portrait(driver,360,310);setContentAreaFilled(false);setBorderPainted(false);setFocusable(true);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));setToolTipText("Choose "+driver.label+" · "+driver.description);getAccessibleContext().setAccessibleName("Choose "+driver.label);}
        @Override protected void paintComponent(Graphics graphics){
            Graphics2D g=(Graphics2D)graphics.create();int w=getWidth(),h=getHeight();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(PANEL);g.fillRoundRect(0,0,w,h,18,18);
            Shape clip=g.getClip();g.clip(new java.awt.geom.RoundRectangle2D.Double(0,0,w,h,18,18));g.drawImage(portrait,0,0,w,h-70,null);g.setClip(clip);
            ArcadeGame.label(g,driver.label,15,h-41,22,INK);ArcadeGame.label(g,driver.description,15,h-17,11,MUTED);
            boolean selected=first==driver.ordinal(),p2=players==2&&second==driver.ordinal();if(selected||p2){g.setColor(selected?GOLD:new Color(121,219,246));g.setStroke(new BasicStroke(3));g.drawRoundRect(2,2,w-5,h-5,18,18);g.fillRoundRect(12,12,46,24,8,8);ArcadeGame.label(g,selected?"P1":"P2",23,29,12,BG);}
            if(isFocusOwner()){g.setColor(INK);g.drawRoundRect(5,5,w-11,h-11,14,14);}g.dispose();
        }
    }
    final class RaceCanvas extends JPanel {
        Rectangle pauseButton=new Rectangle(),muteButton=new Rectangle(),resumeButton=new Rectangle(),retryButton=new Rectangle(),garageButton=new Rectangle(),nextButton=new Rectangle();
        RaceCanvas(){
            setBackground(BG);setFocusable(true);
            // Key events are scoped to this focused canvas, so Burp shortcuts remain usable elsewhere.
            addKeyListener(new KeyAdapter(){public void keyPressed(KeyEvent e){key(e.getKeyCode(),true);e.consume();}public void keyReleased(KeyEvent e){key(e.getKeyCode(),false);e.consume();}});
            setFocusTraversalKeysEnabled(false);
            addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){deactivate();}});
            addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){requestFocusInWindow();Point p=e.getPoint();
                if(muteButton.contains(p)){toggleMute();return;}
                if(race.state==KartRace.State.PAUSED||race.state==KartRace.State.FINISHED){
                    if(resumeButton.contains(p)&&race.state==KartRace.State.PAUSED)togglePause();
                    else if(retryButton.contains(p))startRace();else if(garageButton.contains(p))showGarage();
                    else if(nextButton.contains(p)&&race.state==KartRace.State.FINISHED&&players==1&&level<4&&progress.turboUnlocked>level){level++;startRace();}
                }else if(pauseButton.contains(p))togglePause();
            }});
        }
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);if(race.player1==null)return;Graphics2D g=(Graphics2D)graphics.create();int w=getWidth(),h=getHeight();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(players==1)view(g,0,0,w,h,race.player1);else{int half=h/2;view(g,0,0,w,half,race.player1);view(g,0,half,w,h-half,race.player2);g.setColor(GOLD);g.fillRect(0,half-2,w,4);}
            pauseButton=new Rectangle(w-156,16,46,34);muteButton=new Rectangle(w-102,16,86,34);pill(g,pauseButton,"Ⅱ",false);pill(g,muteButton,progress.muted?"Muted":"Sound",false);
            if(race.state==KartRace.State.COUNTDOWN){center(g,Integer.toString(Math.max(1,(int)Math.ceil(race.countdown))),w/2,h/2+32,88,INK);center(g,"GET READY",w/2,h/2+69,14,GOLD);}
            else if(race.state==KartRace.State.RACING&&race.elapsed<.7)center(g,"GO!",w/2,h/2+20,70,GOLD);
            if(race.state==KartRace.State.PAUSED||race.state==KartRace.State.FINISHED)overlay(g,w,h);
            g.dispose();
        }
        private void view(Graphics2D g,int x,int y,int w,int h,KartRace.Racer r){
            Graphics2D v=(Graphics2D)g.create(x,y,w,h);scene.draw(v,w,h,race,r);int pad=16;
            v.setColor(new Color(8,17,31,218));v.fillRoundRect(pad,pad,players==1?280:310,69,12,12);
            ArcadeGame.label(v,(r.human==1?"P1  ":"P2  ")+r.driver.label.toUpperCase()+"   /   "+race.place(r)+" OF 4",pad+14,pad+25,18,INK);
            ArcadeGame.label(v,"LAP "+race.lap(r)+" / 2   ·   "+time(r.finished()?r.finishTime:race.elapsed)+"   ·   "+r.coins+" COINS",pad+14,pad+51,12,GOLD);
            v.setColor(new Color(8,17,31,220));v.fillRoundRect(pad,h-89,215,72,12,12);ArcadeGame.label(v,Math.round(r.speed*2.2)+" km/h",pad+13,h-58,26,INK);
            ArcadeGame.label(v,"BOOST",pad+13,h-34,10,MUTED);v.setColor(new Color(55,73,95));v.fillRoundRect(pad+59,h-43,140,10,6,6);v.setColor(r.boost?GOLD:new Color(101,223,234));v.fillRoundRect(pad+59,h-43,(int)(140*r.energy/100),10,6,6);
            if(players==1){miniMap(v,w-177,h-173,157,142);ArcadeGame.label(v,race.track.theme.name(),w/2-100,33,14,INK);}
            if(r.finished()&&race.state!=KartRace.State.FINISHED){v.setColor(new Color(8,17,31,185));v.fillRoundRect(w/2-165,h/2-38,330,76,12,12);center(v,"FINISHED · "+race.place(r)+" / 4",w/2,h/2-7,23,GOLD);center(v,"Waiting for the other player",w/2,h/2+21,13,INK);}
            v.dispose();
        }
        private void miniMap(Graphics2D g,int x,int y,int w,int h){
            g.setColor(new Color(8,17,31,205));g.fillRoundRect(x,y,w,h,14,14);KartTrack t=race.track;double minX=1e9,maxX=-1e9,minZ=1e9,maxZ=-1e9;
            for(int i=0;i<t.length;i+=10){minX=Math.min(minX,t.x[i]);maxX=Math.max(maxX,t.x[i]);minZ=Math.min(minZ,t.z[i]);maxZ=Math.max(maxZ,t.z[i]);}
            double scale=Math.min((w-30)/(maxX-minX),(h-28)/(maxZ-minZ));double ox=x+(w-(maxX-minX)*scale)/2-minX*scale,oy=y+12-minZ*scale;
            java.awt.geom.Path2D path=new java.awt.geom.Path2D.Double();for(int i=0;i<=t.length;i+=8){double[] p=t.point(0,0,i);if(i==0)path.moveTo(ox+p[0]*scale,oy+p[2]*scale);else path.lineTo(ox+p[0]*scale,oy+p[2]*scale);}path.closePath();g.setStroke(new BasicStroke(5,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(105,130,153));g.draw(path);
            for(KartRace.Racer r:race.racers){double[] p=t.point(0,0,r.distance);int px=(int)(ox+p[0]*scale),py=(int)(oy+p[2]*scale);g.setColor(r.human>0?Color.WHITE:BG);g.fillOval(px-5,py-5,10,10);g.setColor(r.driver.livery);g.fillOval(px-3,py-3,6,6);}
        }
        private void overlay(Graphics2D g,int w,int h){
            boolean finished=race.state==KartRace.State.FINISHED;g.setColor(new Color(4,10,21,205));g.fillRect(0,0,w,h);
            int pw=Math.min(550,w-30),ph=finished?380:256,x=(w-pw)/2,y=Math.max(12,(h-ph)/2);g.setColor(PANEL);g.fillRoundRect(x,y,pw,ph,22,22);
            center(g,finished?"RACE COMPLETE":"PAUSED",w/2,y+48,30,INK);
            center(g,race.track.theme.name()+"  /  2 laps",w/2,y+78,13,MUTED);
            resumeButton.setBounds(0,0,0,0);nextButton.setBounds(0,0,0,0);
            if(finished){int row=0;for(KartRace.Racer r:race.order()){
                int yy=y+107+row*34;g.setColor(r.human>0?new Color(40,59,81):PANEL);g.fillRoundRect(x+24,yy,pw-48,30,7,7);
                ArcadeGame.label(g,(++row)+"   "+r.driver.label+(r.human>0?"  ·  P"+r.human:"  ·  CPU"),x+36,yy+21,14,r.human>0?GOLD:INK);
                ArcadeGame.label(g,time(r.finishTime),x+pw-130,yy+21,14,INK);
            }
            String detail=players==2?"Local multiplayer result":race.place(race.player1)<=2&&level<4?"Next circuit unlocked":"Finish top 2 to unlock the next circuit";
            if(players==1&&level==4)detail="Final circuit complete";
            center(g,detail,w/2,y+266,13,GOLD);
            if(players==1&&level<4&&progress.turboUnlocked>level){nextButton.setBounds(x+24,y+287,pw-48,34);pill(g,nextButton,"Next circuit  →",true);}
            retryButton.setBounds(x+24,y+332,(pw-60)/2,32);garageButton.setBounds(x+36+(pw-60)/2,y+332,(pw-60)/2,32);
            }else{
                resumeButton.setBounds(x+24,y+103,pw-48,44);pill(g,resumeButton,"Resume race  ·  Enter",true);
                retryButton.setBounds(x+24,y+160,(pw-60)/2,36);garageButton.setBounds(x+36+(pw-60)/2,y+160,(pw-60)/2,36);
                center(g,"Focus stays in Burp when you switch tabs.",w/2,y+229,12,MUTED);
            }
            pill(g,retryButton,"Race again  ·  R",false);pill(g,garageButton,"Driver select",false);
        }
        private void pill(Graphics2D g,Rectangle r,String text,boolean primary){g.setColor(primary?GOLD:new Color(45,63,84));g.fillRoundRect(r.x,r.y,r.width,r.height,9,9);center(g,text,r.x+r.width/2,r.y+r.height/2+5,13,primary?BG:INK);}
        private void center(Graphics2D g,String text,int x,int y,int size,Color color){g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,size));g.setColor(color);g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);}
    }
}
