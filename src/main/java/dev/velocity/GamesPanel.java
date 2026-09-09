package dev.velocity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/** Four-game library, designed as the extension's single Burp tab. */
public final class GamesPanel extends JPanel {
    private final CardLayout pages=new CardLayout();
    private final JPanel content=new JPanel(pages);
    private final Map<String,JPanel> games=new LinkedHashMap<>();
    private final Progress progress;
    final AudioEngine audio;
    final JCheckBox muteAudio=new JCheckBox("Mute audio");
    private final JButton back=new JButton("←  All games");
    private final JLabel pageTitle=new JLabel("Games");
    private String active="library";
    private boolean disposed;
    public GamesPanel(){this(new Progress(true));}
    GamesPanel(Progress progress){this(progress,GraphicsEnvironment.isHeadless()?AudioEngine.silent():new AudioEngine());}
    GamesPanel(Progress progress,AudioEngine audio){
        this.progress=progress;this.audio=audio;audio.setMuted(progress.muted);setLayout(new BorderLayout());setBackground(Color.BLACK);
        JPanel header=new JPanel(new BorderLayout());header.setBackground(new Color(9,9,11));header.setBorder(BorderFactory.createEmptyBorder(16,28,16,28));
        pageTitle.setForeground(Color.WHITE);pageTitle.setFont(new Font(Font.SANS_SERIF,Font.BOLD,21));header.add(pageTitle,BorderLayout.WEST);
        back.setFocusPainted(false);back.setForeground(Color.WHITE);back.setBackground(new Color(37,37,42));back.addActionListener(e->showLibrary());back.setVisible(false);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,16,0));actions.setOpaque(false);
        muteAudio.setOpaque(false);muteAudio.setForeground(new Color(211,214,221));muteAudio.setSelected(progress.muted);muteAudio.setFocusable(false);muteAudio.setMnemonic('M');
        muteAudio.setToolTipText("Mute music and sound effects · Alt+M · Saved across restarts");
        muteAudio.addActionListener(e->setMuted(muteAudio.isSelected()));
        actions.add(muteAudio);actions.add(back);header.add(actions,BorderLayout.EAST);
        add(header,BorderLayout.NORTH);content.setBackground(Color.BLACK);add(content,BorderLayout.CENTER);content.add(library(),"library");
    }
    private JPanel library(){
        JPanel library=new JPanel(new BorderLayout());library.setBackground(Color.BLACK);
        JLabel subtitle=new JLabel("Choose your next game.");subtitle.setForeground(new Color(159,159,168));subtitle.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));subtitle.setBorder(BorderFactory.createEmptyBorder(14,28,24,28));library.add(subtitle,BorderLayout.NORTH);
        JPanel grid=new GalleryPanel();grid.setBackground(Color.BLACK);grid.setBorder(BorderFactory.createEmptyBorder(0,28,24,28));
        String[] names={"Sonic 3D","Asteroids","Breakout","Turbo Tails"};
        String[] descriptions={"Run & ride  |  Optional quizzes","Space combat  |  Enemy squadrons","Classic arcade  |  Three levels","Native 3D kart racing  |  1–2 players"};
        for(int i=0;i<names.length;i++){String name=names[i];GameCard card=new GameCard(name,descriptions[i],cover(i));card.addActionListener(e->openGame(name));grid.add(card);}
        JScrollPane scroll=new JScrollPane(grid);scroll.setBorder(null);scroll.getViewport().setBackground(Color.BLACK);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scroll.getVerticalScrollBar().setUnitIncrement(20);library.add(scroll,BorderLayout.CENTER);
        JLabel footer=new JLabel("4 games  •  Click a card to play  •  Alt+M mutes audio");footer.setForeground(new Color(105,105,115));footer.setBorder(BorderFactory.createEmptyBorder(14,28,20,28));library.add(footer,BorderLayout.SOUTH);return library;
    }
    private void syncMute(boolean value){
        muteAudio.setSelected(value);audio.setMuted(value);progress.setMuted(value);
    }
    private void setMuted(boolean value){
        syncMute(value);
        for(JPanel panel:games.values())if(panel instanceof TurboTailsGame turbo)turbo.setMuted(value);
    }
    void openGame(String name){
        if(disposed)return;pauseActive();
        JPanel panel=games.get(name);
        if(panel==null){panel=switch(name){case "Sonic 3D"->new GamePanel(progress,audio);case "Asteroids"->new AsteroidsGame(audio);case "Breakout"->new BreakoutGame(audio);case "Turbo Tails"->new TurboTailsGame(progress,audio,this::syncMute);default->throw new IllegalArgumentException(name);};games.put(name,panel);content.add(panel,name);}
        active=name;pages.show(content,name);pageTitle.setText(name);back.setVisible(true);JPanel target=panel;SwingUtilities.invokeLater(target::requestFocusInWindow);
    }
    void showLibrary(){pauseActive();active="library";pages.show(content,"library");pageTitle.setText("Games");back.setVisible(false);}
    void pauseActive(){JPanel p=games.get(active);if(p instanceof GamePanel sonic)sonic.pauseGame();if(p instanceof ArcadeGame arcade)arcade.pauseGame();if(p instanceof TurboTailsGame turbo)turbo.deactivate();}
    public void dispose(){disposed=true;for(JPanel p:games.values()){if(p instanceof GamePanel sonic)sonic.dispose();if(p instanceof ArcadeGame arcade)arcade.dispose();if(p instanceof TurboTailsGame turbo)turbo.dispose();}games.clear();audio.close();}
    JPanel game(String name){return games.get(name);}
    String active(){return active;}
    static BufferedImage cover(int index){
        BufferedImage image=new BufferedImage(960,540,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        if(index==0){
            g.setPaint(new GradientPaint(0,0,new Color(8,47,124),960,540,new Color(37,126,205)));g.fillRect(0,0,960,540);
            g.setColor(new Color(64,157,226));g.fillOval(415,-50,620,620);g.setColor(new Color(90,192,233));for(int i=0;i<8;i++)g.drawLine(-120,540-i*54,590,290-i*20);
            Renderer3D renderer=new Renderer3D();Game game=new Game();renderer.begin(960,540,0,0,true,Math.toRadians(144));new HedgehogRig().draw(renderer,game,true);g.drawImage(renderer.image(),0,0,null);
            ArcadeGame.label(g,"SONIC",42,230,76,Color.WHITE);ArcadeGame.label(g,"3D",43,318,88,new Color(255,214,66));ArcadeGame.label(g,"RUN • RIDE • EXPLORE",46,375,23,new Color(184,229,255));
        }else if(index==1){
            AsteroidsGame game=new AsteroidsGame();game.x=420;game.y=300;game.enemies.add(new AsteroidsGame.Enemy(240,155));game.enemies.add(new AsteroidsGame.Enemy(600,145));game.shots.add(new AsteroidsGame.Shot(505,243,0,0));game.drawGame(g);game.dispose();
            g.setColor(new Color(3,8,25,90));g.fillRect(0,365,960,175);ArcadeGame.label(g,"ASTEROIDS",43,462,69,Color.WHITE);ArcadeGame.label(g,"TAKE ON THE SQUADRON.",47,501,19,new Color(149,222,255));
        }else if(index==2){
            BreakoutGame game=new BreakoutGame();game.ballX=540;game.ballY=380;game.launched=true;game.drawGame(g);game.dispose();
            ArcadeGame.label(g,"BREAKOUT",48,437,75,Color.WHITE);ArcadeGame.label(g,"FIND YOUR ANGLE.",52,481,22,new Color(184,164,255));
        }
        if(index==3)g.drawImage(KartScene.cover(),0,0,null);
        g.dispose();return image;
    }
    private static final class GameCard extends JButton {
        final String title,description;final BufferedImage cover;
        GameCard(String title,String description,BufferedImage cover){
            this.title=title;this.description=description;this.cover=cover;setBorderPainted(false);setContentAreaFilled(false);setFocusPainted(false);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));setToolTipText("Play "+title);getAccessibleContext().setAccessibleName("Play "+title);
        }
        @Override protected void paintComponent(Graphics graphics){
            Graphics2D g=(Graphics2D)graphics.create();int w=getWidth(),h=getHeight(),pad=15,ih=(w-pad*2)*9/16;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(getModel().isRollover()?new Color(31,31,35):new Color(20,20,22));g.fillRoundRect(0,0,w,h,17,17);
            Shape old=g.getClip();g.clip(new java.awt.geom.RoundRectangle2D.Double(pad,pad,w-2*pad,ih,13,13));g.drawImage(cover,pad,pad,w-pad*2,ih,null);g.setClip(old);
            ArcadeGame.label(g,title,pad,ih+pad+34,23,Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));g.setColor(new Color(159,159,166));g.drawString(description,pad,ih+pad+64);
            if(isFocusOwner()){g.setColor(new Color(131,173,246));g.setStroke(new BasicStroke(2));g.drawRoundRect(2,2,w-5,h-5,17,17);}g.dispose();
        }
    }
    private static final class GalleryPanel extends JPanel implements Scrollable {
        GalleryPanel(){super(new GalleryLayout());}
        public Dimension getPreferredScrollableViewportSize(){return new Dimension(1200,600);}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 24;}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(24,r.height-24);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return getParent()!=null&&getPreferredSize().height<getParent().getHeight();}
    }
    private static final class GalleryLayout implements LayoutManager {
        public void addLayoutComponent(String s,Component c){}public void removeLayoutComponent(Component c){}
        private int width(Container p){return p.getParent()!=null&&p.getParent().getWidth()>0?p.getParent().getWidth():p.getWidth()>0?p.getWidth():1200;}
        private int columns(Container p){return width(p)>=1300?4:width(p)>=730?2:1;}
        public Dimension preferredLayoutSize(Container p){int width=width(p);int cols=columns(p);Insets in=p.getInsets();int cw=(width-in.left-in.right-(cols-1)*16)/cols;return new Dimension(width,in.top+in.bottom+(int)Math.ceil(p.getComponentCount()/(double)cols)*((cw-30)*9/16+112));}
        public Dimension minimumLayoutSize(Container p){return new Dimension(340,400);}
        public void layoutContainer(Container p){Insets in=p.getInsets();int cols=columns(p),cw=(p.getWidth()-in.left-in.right-(cols-1)*16)/cols,ch=(cw-30)*9/16+96;for(int i=0;i<p.getComponentCount();i++)p.getComponent(i).setBounds(in.left+(i%cols)*(cw+16),in.top+(i/cols)*(ch+16),cw,ch);}
    }
}
