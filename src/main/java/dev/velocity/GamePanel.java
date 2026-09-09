package dev.velocity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.*;

/** Burp-friendly Swing game view with a depth-buffered articulated 3D character. */
public final class GamePanel extends JPanel {
    final Game game;
    private final AudioEngine audio;
    private final java.util.List<java.util.Map.Entry<Rectangle,Runnable>> buttons=new ArrayList<>();
    private final TimerLoop loop = new TimerLoop();
    private final Set<String> held = new HashSet<>();
    private final Renderer3D renderer = new Renderer3D();
    private final HedgehogRig hedgehog = new HedgehogRig();
    boolean inspectCharacter;
    double previewAngle = Math.toRadians(155);
    private int dragX;
    private boolean rotating;
    private final Color ink = new Color(14, 30, 45), mint = new Color(121, 255, 206);
    private boolean disposed;
    final JRadioButton noQuizChoice=new JRadioButton("Play without quiz");
    final JRadioButton quizChoice=new JRadioButton("Play with cybersecurity quiz");
    final JCheckBox rememberChoice=new JCheckBox("Remember for next app launch");
    private int w, h;

    public GamePanel() { this(new Progress(true)); }
    GamePanel(Progress progress) {this(progress,AudioEngine.silent());}
    GamePanel(Progress progress,AudioEngine audio) {
        this.audio=audio;game=new Game(progress);
        setLayout(null);
        for(AbstractButton box:new AbstractButton[]{noQuizChoice,quizChoice,rememberChoice}){box.setBackground(ink);box.setForeground(Color.WHITE);box.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));box.setVisible(false);add(box);}
        ButtonGroup modes=new ButtonGroup();modes.add(noQuizChoice);modes.add(quizChoice);
        noQuizChoice.setSelected(!game.quizEnabled);quizChoice.setSelected(game.quizEnabled);rememberChoice.setSelected(progress.rememberQuizChoice);
        ActionListener choice=e->{game.quizEnabled=quizChoice.isSelected();progress.chooseQuiz(game.quizEnabled,rememberChoice.isSelected());repaint();};
        noQuizChoice.addActionListener(choice);quizChoice.addActionListener(choice);rememberChoice.addActionListener(choice);
        for(AbstractButton box:new AbstractButton[]{noQuizChoice,quizChoice,rememberChoice}){
            box.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"start-run");
            box.getActionMap().put("start-run",new AbstractAction(){public void actionPerformed(ActionEvent e){requestFocusInWindow();toggle();}});
        }
        setPreferredSize(new Dimension(1200, 760)); setMinimumSize(new Dimension(640, 460));
        setFocusable(true); setBackground(ink);
        bind("LEFT", "left", () -> game.steer(-1)); bind("A", "a", () -> game.steer(-1));
        bind("RIGHT", "right", () -> game.steer(1)); bind("D", "d", () -> game.steer(1));
        bind("SPACE", "jump", ()->{double before=game.y;game.jump();if(game.state==Game.State.RUNNING&&before==0&&game.vy>0)audio.cue(AudioEngine.Cue.JUMP);}); bind("UP", "up", () -> {}); bind("W", "w", () -> {});
        bind("DOWN","down",()->{});bind("S","s",()->{});
        for(int i=1;i<=3;i++){final int answer=i-1;bind(""+i,"answer"+i,()->game.answer(answer));}
        bind("V", "viewer", this::toggleViewer);bind("Q","dismount",game::dismount);
        bind("P", "pause", this::toggle); bind("ESCAPE", "escape", this::pause);
        bind("ENTER", "play", this::toggle); bind("R", "restart", this::restart);
        bind("SHIFT", "boost", () -> game.boosting = game.state == Game.State.RUNNING);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                for(var button:buttons)if(button.getKey().contains(e.getPoint())){button.getValue().run();repaint();return;}
                if (preview() && e.getX() > getWidth() * .48) { rotating=true; dragX=e.getX(); }

            }
            @Override public void mouseReleased(MouseEvent e) { rotating=false; }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if(rotating) { previewAngle+=(e.getX()-dragX)*.012;dragX=e.getX();repaint(); }
            }
        });
        addFocusListener(new FocusAdapter() { @Override public void focusLost(FocusEvent e) { pause(); } });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing() && !disposed) loop.start(); else { pause(); loop.stop(); }
            }
        });
    }
    private void bind(String key, String name, Runnable action) {
        // WHEN_FOCUSED keeps game shortcuts out of Burp's other tools and text editors.
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("pressed " + key), name);
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("released " + key), name + "-up");
        getActionMap().put(name, new AbstractAction() { public void actionPerformed(ActionEvent e) {
            if (held.add(name)) action.run(); updateAudio();repaint();
        }});
        getActionMap().put(name + "-up", new AbstractAction() { public void actionPerformed(ActionEvent e) {
            held.remove(name); if (name.equals("boost")) game.boosting = false;
        }});
    }
    @Override public void doLayout(){layoutChoices();}
    private void layoutChoices(){
        int cw=Math.min(495,(int)(getWidth()*.46)),cy=Math.max(90,(getHeight()-520)/2);
        noQuizChoice.setBounds(48,cy+283,cw-48,30);quizChoice.setBounds(48,cy+315,cw-48,30);rememberChoice.setBounds(48,cy+349,cw-48,30);
    }
    private boolean preview() { return game.state == Game.State.READY || inspectCharacter; }
    private void toggleViewer() { pause(); inspectCharacter = !inspectCharacter; repaint(); }
    private void toggle() {
        inspectCharacter=false;
        if(game.state==Game.State.CHALLENGE)game.continueMission();
        else if(game.state==Game.State.LOST)game.respawn();
        else if(game.state==Game.State.WON){game.selectLevel(Math.min(3,game.level+1));}
        else game.togglePause();
        updateAudio();repaint();
    }
    private void restart() { inspectCharacter=false; game.reset(); game.togglePause(); held.clear(); updateAudio();repaint(); }
    private void updateAudio(){
        if(game.state==Game.State.RUNNING&&!disposed)audio.start(this,AudioEngine.Track.SONIC);
        else if(game.state==Game.State.WON||game.state==Game.State.LOST){if(audio.isActive(this))audio.finish(this,game.state==Game.State.WON?AudioEngine.Cue.WIN:AudioEngine.Cue.LOSE);}
        else audio.stop(this);
    }
    void pauseGame() { pause(); }
    private void pause() { if (game.state == Game.State.RUNNING) game.togglePause(); held.clear(); game.clearInput(); audio.stop(this);repaint(); }
    public void dispose() { disposed = true; loop.stop(); held.clear(); game.clearInput();audio.stop(this); }
    private final class TimerLoop {
        long previous;
        final javax.swing.Timer timer = new javax.swing.Timer(16, e -> {
            long now = System.nanoTime(); double dt = (now - previous) / 1e9; previous = now;
            if (game.state == Game.State.RUNNING) {
                game.steering=(held.contains("d")||held.contains("right")?1:0)-(held.contains("a")||held.contains("left")?1:0);
                game.throttle=held.contains("w")||held.contains("up");game.braking=held.contains("s")||held.contains("down");
                game.boosting=held.contains("boost");
                int rings=game.rings;Vehicle vehicle=game.vehicle;
                game.update(dt);
                if(game.rings>rings)audio.cue(AudioEngine.Cue.COIN);
                if(game.rings<rings)audio.cue(AudioEngine.Cue.HIT);
                if(game.vehicle!=vehicle)audio.cue(AudioEngine.Cue.PICKUP);
                updateAudio();
                if(game.state!=Game.State.RUNNING)held.clear();repaint();
            }
        });
        void start() { previous = System.nanoTime(); timer.start(); }
        void stop() { timer.stop(); }
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            w = getWidth(); h = getHeight(); if (w < 1 || h < 1) return;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            buttons.clear();
            boolean choosing=game.state==Game.State.READY;noQuizChoice.setVisible(choosing);quizChoice.setVisible(choosing);rememberChoice.setVisible(choosing);layoutChoices();
            renderer.begin(w,h,game.x,game.z,preview(),previewAngle);
            if(!preview()){renderer.follow(game.track,game.z);renderer.pace(game.speed);renderer.atmosphere(Environment.ALL[game.level].horizon());}
            if(preview()) {
                g.setPaint(new GradientPaint(0,78,new Color(12,28,61),w,h,new Color(36,76,127)));
                g.fillRect(0,0,w,h);
                g.setColor(new Color(47,96,157));g.fillOval((int)(w*.51),(int)(h*.17),(int)(w*.39),(int)(h*.61));
                box(game.x,-.12,game.z,3.5,.12,3.5,new Color(61,104,158));
                hedgehog.draw(renderer,game,true);
            } else {
                Environment env=Environment.ALL[game.level];
                g.setPaint(new GradientPaint(0,0,env.sky(),0,h,env.horizon()));
                g.fillRect(0,0,w,h);
                g.setColor(new Color(255,249,190));g.fillOval((int)(w*.76),(int)(h*.15),76,76);
                mountains(g); world();
            }
            g.drawImage(renderer.image(),0,0,w,h,null);
            if(!preview())speedLines(g);
            hud(g);
            if (game.state != Game.State.RUNNING) overlay(g);
        } finally { g.dispose(); }
    }
    private void mountains(Graphics2D g) {
        for (int row = 0; row < 2; row++) {
            g.setColor(row==0?Environment.ALL[game.level].stone():Environment.ALL[game.level].stone().darker());
            Path2D p = new Path2D.Double(); p.moveTo(0, h * .58);
            for (int i = 0; i <= 12; i++) p.lineTo(i * w / 12.0, h * (.39 + row * .055 - .12 * Math.abs(Math.sin(i * 2.3 + row))));
            p.lineTo(w, h); p.lineTo(0, h); p.closePath(); g.fill(p);
        }
        g.setColor(Environment.ALL[game.level].ground()); g.fillRect(0, (int)(h * .57), w, h);
    }
    private void face(Color color,double... xyz) { renderer.polygon(color,xyz); }
    private void box(double x, double y, double z, double sx, double sy, double sz, Color c) {
        double l=x-sx/2,r=x+sx/2,b=y,t=y+sy,n=z-sz/2,f=z+sz/2;
        face(c.darker(), l,b,n,r,b,n,r,t,n,l,t,n);
        face(c.darker(), r,b,n,r,b,f,r,t,f,r,t,n);
        face(c, l,b,f,l,b,n,l,t,n,l,t,f);
        face(c, r,b,f,l,b,f,l,t,f,r,t,f);
        face(c.brighter(), l,t,n,r,t,n,r,t,f,l,t,f);
    }
    private void gem(double x, double y, double z, double radius, Color c) {
        for (int i=0;i<8;i++) {
            double a=i*Math.PI/4, b=(i+1)*Math.PI/4;
            double ax=x+Math.cos(a)*radius, az=z+Math.sin(a)*radius;
            double bx=x+Math.cos(b)*radius, bz=z+Math.sin(b)*radius;
            Color shade=i%2==0?c:c.darker();
            face(shade, x,y+radius,z,ax,y,az,bx,y,bz);
            face(shade, x,y-radius,z,bx,y,bz,ax,y,az);
        }
    }
    private void ring(double x, double y, double z) {
        double rotation = game.elapsed * 1.7 + z*.05;
        for (int i=0;i<12;i++) {
            double a=i*Math.PI/6,b=(i+1)*Math.PI/6;
            double[] points = new double[12]; int k=0;
            for (double[] ar : new double[][]{{a,.53},{b,.53},{b,.34},{a,.34}}) {
                points[k++]=x+Math.cos(ar[0])*ar[1]*Math.cos(rotation);
                points[k++]=y+Math.sin(ar[0])*ar[1];
                points[k++]=z+Math.cos(ar[0])*ar[1]*Math.sin(rotation);
            }
            face(i%3==0?new Color(255, 249, 162):new Color(255, 191, 46), points);
        }
    }
    private void world() {
        Environment env=Environment.ALL[game.level];
        int start = Math.max(0, (int)((game.z - 18) / 3) * 3);
        for (int z = start; z < Math.min(game.length() + 30, game.z + 260); z += 3) {
            if (Game.gap(z+.1)) continue;
            boolean warning = Game.gap(z + 12) || Game.gap(z + 9);
            Color road=warning?new Color(255,208,86):(z/3%2==0?env.road():env.road().darker());
            face(road,-5,game.track.ramp(z),z,5,game.track.ramp(z),z,5,game.track.ramp(z+3),z+3,-5,game.track.ramp(z+3),z+3);
            for (int side : new int[]{-1,1}) {
                box(side*5.7,-2,z+1.5,1.4,2,3,z/6%2==0?env.edge():env.edge().darker());
                if (z%12==0) box(side*1.5,.015,z+1.5,.06,.02,1.5,new Color(134, 206, 187));
            }
        }
        for(int z=Math.max(0,start/24*24);z<game.z+245;z+=24){
            for(int side:new int[]{-1,1}){
                double x=side*(12+3*Math.sin(z));
                if(game.level==0){
                    box(x,-3,z,6,2.7,9,env.stone());box(x,-.3,z,.30,4.8,.34,new Color(166,120,70));
                    for(int leaf=0;leaf<5;leaf++){double a=leaf*Math.PI*2/5;
                        face(new Color(30,142,99),x,4.7,z,x+Math.cos(a)*3.5,3.6,z+Math.sin(a)*3.5,x+Math.cos(a+.4)*1.8,4.3,z+Math.sin(a+.4)*1.8);
                    }
                    if(z%48==0)box(side*7,-.15,z,2.2,.15,2.2,new Color(253,225,171));
                }else if(game.level==1){
                    double tall=8+Math.abs(Math.sin(z)) * 13;box(x,-4,z,5,tall,7,env.stone());
                    for(int floor=1;floor<tall-2;floor+=3)box(x,floor,z-3.55,3,.25,.08,new Color(64,229,244));
                    box(side*5.6,.2,z,.18,3.5,.18,new Color(206,109,255));
                }else if(game.level==2){
                    box(x,-4,z,5,3,7,env.stone());box(x,-1,z,.3,4,.3,new Color(100,108,106));
                    gem(x,3,z,2.1,new Color(219,242,247));gem(x,4.4,z,1.5,new Color(250,252,255));
                }else{
                    box(x,-3,z,4,6+Math.abs(Math.sin(z))*4,5,env.stone());
                    gem(x,3,z,1.3,new Color(255,124,40));
                    box(side*7.3,-.3,z,1.8,.13,8,new Color(255,180,41));
                }
            }
        }
        if(game.level>=2)for(int z=Math.max(0,start/9*9);z<game.z+260;z+=9)if(z%400>260&&z%400<350){
            box(-6,0,z,.4,7,.45,new Color(31,49,83));box(6,0,z,.4,7,.45,new Color(31,49,83));box(0,7,z,12.4,.4,.45,new Color(93,124,173));
        }
        for(int m=0;m<2;m++){double pos=game.length()*(m==0?.32:.68);if(pos>game.z&&pos<game.z+260){box(-5,0,pos,.4,6,.4,mint);box(5,0,pos,.4,6,.4,mint);box(0,6,pos,10,.5,.4,mint);}}
        for (Game.Item item : game.items) {
            if (item.used || item.z < game.z-4 || item.z > game.z+150) continue;
            if (item.kind == Game.Kind.RING) ring(item.x,1.05,item.z);
            else if(item.pickup()){
                Vehicle kind=Vehicle.valueOf(item.kind.name());
                double bob=.22+Math.sin(game.elapsed*3)*.10;
                hedgehog.drawVehicle(renderer,kind,item.x,item.z,bob,.55,game.wheelAngle,0);
                ring(item.x,3.1,item.z);
            }else {
                box(item.x,0,item.z,1.65,1.4,1.5,new Color(214, 78, 75));
                box(item.x,1.4,item.z,1.1,.09,1,new Color(255, 176, 114));
            }
        }
        if (game.length()-game.z < 170) {
            box(-5,0,game.length(),.6,7,.6,mint); box(5,0,game.length(),.6,7,.6,mint);
            box(0,6.5,game.length(),10,.7,.6,mint);
        }
        if (game.invincible==0 || (int)(game.elapsed*12)%2==0) {
            box(game.x,.012,game.z,1.2,.012,.85,new Color(35,71,76));
            hedgehog.draw(renderer,game,false);
        }
        if(game.vehicle==Vehicle.SURFBOARD)for(int i=1;i<7;i++)
            box(game.x,.07,game.z-i*.8,1.0+i*.15,.02,.16,new Color(163,239,255));
        if(game.boosting && game.energy>1) for(int i=1;i<=8;i++)
            gem(game.x,.65+game.y,game.z-i*.48,.28-i*.022,i%2==0?new Color(90,205,255):new Color(34,105,245));
    }
    private void speedLines(Graphics2D g){
        if(game.state!=Game.State.RUNNING||game.speed<65)return;
        int alpha=game.boosting?100:35;g.setColor(new Color(222,246,255,alpha));g.setStroke(new BasicStroke(game.boosting?2:1));
        for(int i=0;i<18;i++){
            double a=i*Math.PI*2/18,phase=(game.elapsed*2+i*.17)%1,r=.55+phase*.45;
            int x1=(int)(w*.5+Math.cos(a)*w*.53*r),y1=(int)(h*.46+Math.sin(a)*h*.55*r);
            int x2=(int)(w*.5+Math.cos(a)*w*.53*(r+.11)),y2=(int)(h*.46+Math.sin(a)*h*.55*(r+.11));
            g.drawLine(x1,y1,x2,y2);
        }
    }
    private void text(Graphics2D g, String value, int x, int y, int size, Color color, boolean bold) {
        g.setFont(new Font(Font.SANS_SERIF,bold?Font.BOLD:Font.PLAIN,size)); g.setColor(color); g.drawString(value,x,y);
    }
    private void hud(Graphics2D g) {
        g.setColor(ink); g.fillRect(0,0,w,78);
        text(g,"SONIC 3D",24,33,23,mint,true);
        text(g,Environment.ALL[game.level].name()+" / "+(game.quizEnabled?"QUIZ ON":"FREE RUN"),24,56,10,new Color(159, 189, 199),false);
        int right=Math.max(290,w-480);
        text(g,"RINGS  " + String.format("%03d",game.rings),right,33,17,Color.WHITE,true);
        text(g,"HITS  " + game.hits + "/3",right+150,33,17,Color.WHITE,true);
        text(g,String.format("%02d:%02d",(int)game.elapsed/60,(int)game.elapsed%60),right+285,33,17,Color.WHITE,true);
        text(g,"COURSE",right,57,10,new Color(159,189,199),false);
        g.setColor(new Color(44,64,75)); g.fillRoundRect(right+62,49,280,5,5,5);
        g.setColor(mint); g.fillRoundRect(right+62,49,(int)(280*game.z/game.length()),5,5,5);
        g.setColor(ink); g.fillRoundRect(22,h-106,196,66,16,16);
        text(g,String.format("%02.0f",game.speed*3.6),38,h-73,26,Color.WHITE,true);
        text(g,"KM/H",97,h-73,10,mint,false);
        text(g,"BOOST",139,h-74,10,mint,true);
        g.setColor(new Color(47,68,78)); g.fillRoundRect(38,h-59,163,6,6,6);
        g.setColor(mint); g.fillRoundRect(38,h-59,(int)(163*game.energy/100),6,6,6);
        g.setColor(ink); g.fillRect(0,h-28,w,28);
        text(g,"A / D  MOVE    W  SPRINT    S  BRAKE    SPACE  JUMP    SHIFT  BOOST    Q  RUN ON FOOT    P  PAUSE",22,h-10,11,new Color(189,210,216),false);
        text(g,game.vehicle.label,242,h-72,17,mint,true);
        if(game.pickupNotice>0)text(g,game.vehicle.label+" PICKED UP  /  Q TO RUN",w/2-170,118,17,mint,true);
        if (game.state==Game.State.RUNNING && Game.gap(game.z+45)) {
            text(g,"GAP AHEAD — JUMP",w/2-100,116,18,new Color(255,244,151),true);
        }
    }
    private void button(Graphics2D g,String label,int x,int y,int width,int height,Runnable action,boolean enabled){
        g.setColor(enabled?mint:new Color(49,65,80));g.fillRoundRect(x,y,width,height,10,10);
        text(g,label,x+14,y+height/2+5,13,enabled?ink:new Color(160,170,182),true);
        if(enabled)buttons.add(new AbstractMap.SimpleEntry<>(new Rectangle(x,y,width,height),action));
    }
    private int wrap(Graphics2D g,String value,int x,int y,int max,int size,Color color){
        g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,size));String line="";
        for(String word:value.split(" ")){if(g.getFontMetrics().stringWidth(line+word)>max&&!line.isEmpty()){text(g,line,x,y,size,color,false);y+=size+8;line="";}line+=word+" ";}
        text(g,line,x,y,size,color,false);return y+size+8;
    }
    private void overlay(Graphics2D g){
        if(game.state==Game.State.CHALLENGE){
            g.setColor(new Color(3,12,25,210));g.fillRect(0,78,w,h-106);
            int cw=Math.min(760,w-40),cx=(w-cw)/2,cy=Math.max(90,(h-500)/2);
            g.setColor(ink);g.fillRoundRect(cx,cy,cw,490,22,22);
            text(g,"CHECKPOINT "+(game.mission+1)+" / "+Missions.THEMES[game.level],cx+25,cy+35,12,mint,true);
            text(g,game.question().title(),cx+25,cy+75,26,Color.WHITE,true);
            int y=wrap(g,game.question().evidence(),cx+25,cy+109,cw-50,15,new Color(200,215,231));
            y=wrap(g,game.question().prompt(),cx+25,y+7,cw-50,15,Color.WHITE);
            if(game.answered){
                wrap(g,game.feedback,cx+25,y+20,cw-50,17,mint);
                button(g,"CONTINUE  /  ENTER",cx+25,cy+423,cw-50,44,this::toggle,true);
            }else{
                for(int i=0;i<3;i++){final int choice=i;int by=y+10+i*64;
                    g.setColor(new Color(31,48,68));g.fillRoundRect(cx+25,by,cw-50,56,9,9);
                    wrap(g,(i+1)+". "+game.question().answers()[i],cx+38,by+23,cw-80,14,Color.WHITE);
                    buttons.add(new AbstractMap.SimpleEntry<>(new Rectangle(cx+25,by,cw-50,56),()->game.answer(choice)));
                }
                if(game.difficulty==0)wrap(g,"HINT: "+game.question().hint(),cx+25,cy+438,cw-50,12,mint);
            }
            return;
        }
        if(!preview()){g.setColor(new Color(4,16,31,130));g.fillRect(0,78,w,h-106);}
        int cw=preview()?Math.min(495,(int)(w*.46)):Math.min(570,w-40),cx=preview()?24:(w-cw)/2;
        int cy=Math.max(90,(h-520)/2);
        g.setColor(ink);g.fillRoundRect(cx,cy,cw,510,22,22);
        text(g,Environment.ALL[game.level].name()+" / HIGH SPEED RUN",cx+24,cy+33,10,mint,true);
        String title=switch(game.state){case READY->Missions.NAMES[game.level];case PAUSED->"Run paused";case WON->"Course cleared!";default->"One more run?";};
        text(g,title,cx+24,cy+78,29,Color.WHITE,true);
        if(game.state==Game.State.READY){
            wrap(g,"Start on foot. Collect a bike, car or surfboard. Hold Shift to boost.",cx+24,cy+110,cw-48,14,new Color(194,210,224));
            for(int i=0;i<4;i++){final int level=i;button(g,(i+1)+"  "+Missions.NAMES[i]+(game.level==i?"  •":""),cx+24+(i%2)*(cw-44)/2,cy+151+(i/2)*43,(cw-56)/2,35,()->game.selectLevel(level),i<=game.progress.unlocked);}
            button(g,"DIFFICULTY: "+new String[]{"EASY","NORMAL","HARD"}[game.difficulty],cx+24,cy+247,cw-48,34,()->game.difficulty=(game.difficulty+1)%3,true);
            int record=game.progress.key(game.level,game.difficulty,game.quizEnabled);
            double best=game.progress.best[record];
            text(g,best>0?String.format("BEST %.1fs   •   %s",best,new String[]{"—","BRONZE","SILVER","GOLD"}[game.progress.medals[record]]):"Quizzes are optional. Checkpoints work in both modes.",cx+24,cy+396,11,new Color(168,190,210),false);
            button(g,"RUN  /  ENTER",cx+24,cy+415,cw-48,45,this::toggle,true);
        }else{
            String message=game.state==Game.State.WON?String.format("%s medal · %.1f seconds · %d rings",new String[]{"","Bronze","Silver","Gold"}[game.medal],game.elapsed,game.rings)+(game.quizEnabled?" · "+game.correct+"/2 answers":" · Free run"):game.state==Game.State.LOST?game.reason:"Your run is saved in this session. Resume when ready.";
            wrap(g,message,cx+24,cy+123,cw-48,16,new Color(190,213,231));
            button(g,game.state==Game.State.WON?"NEXT LEVEL / SELECT":game.state==Game.State.LOST?"RESPAWN AT CHECKPOINT":"RESUME RUN",cx+24,cy+239,cw-48,44,this::toggle,true);
            button(g,"LEVEL SELECT",cx+24,cy+294,cw-48,38,()->{game.reset();inspectCharacter=false;},true);
            button(g,"RESTART LEVEL",cx+24,cy+343,cw-48,38,this::restart,true);
        }
        text(g,"Drag preview to rotate • All games returns to library",cx+24,cy+488,9,new Color(150,176,197),false);
        if(!game.progress.warning.isEmpty())text(g,game.progress.warning,24,h-115,11,Color.ORANGE,false);
    }
}
