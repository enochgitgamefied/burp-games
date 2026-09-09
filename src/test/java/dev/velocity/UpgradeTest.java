package dev.velocity;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import javax.swing.SwingUtilities;
import java.io.File;

/** Behavioral coverage for speed, vehicle pickups, optional quizzes and enemy combat. */
public final class UpgradeTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static final class MemoryPreferences extends AbstractPreferences {
        final Map<String,String> values=new HashMap<>();
        MemoryPreferences(){super(null,"");}
        protected void putSpi(String k,String v){values.put(k,v);}protected String getSpi(String k){return values.get(k);}
        protected void removeSpi(String k){values.remove(k);}protected void removeNodeSpi(){values.clear();}
        protected String[] keysSpi(){return values.keySet().toArray(String[]::new);}protected String[] childrenNamesSpi(){return new String[0];}
        protected AbstractPreferences childSpi(String name){throw new UnsupportedOperationException();}
        protected void syncSpi(){}protected void flushSpi(){}
    }
    public static void main(String[] args)throws Exception{
        Game g=new Game();check(g.vehicle==Vehicle.RUNNING,"starts on foot");g.togglePause();
        for(int i=0;i<50;i++)g.update(.02);
        check(g.speed>65,"reaches fast automatic running speed within one second");
        double cruise=g.speed;g.boosting=true;for(int i=0;i<30;i++)g.update(.02);check(g.speed>cruise+20,"boost meaningfully increases actual movement speed");
        for(Game.Kind kind:new Game.Kind[]{Game.Kind.BIKE,Game.Kind.CAR,Game.Kind.SURFBOARD}){
            g=new Game();g.togglePause();g.items.clear();g.items.add(new Game.Item(kind,3,150));g.z=148;g.x=-3;g.speed=140;g.update(.04);
            check(g.vehicle==Vehicle.RUNNING,"passing another lane does not mount "+kind);
            g.z=148;g.x=3;g.speed=140;g.update(.04);check(g.vehicle==Vehicle.valueOf(kind.name()),"high speed pickup mounts "+kind);
            g.dismount();check(g.vehicle==Vehicle.RUNNING,"dismount restores running");
            g.reset();check(g.vehicle==Vehicle.RUNNING,"restart is on foot");
        }
        MemoryPreferences preferences=new MemoryPreferences();Progress choice=new Progress(preferences);
        choice.chooseQuiz(false,true);Progress reload=new Progress(preferences);
        check(!reload.quizEnabled&&reload.rememberQuizChoice,"remembered no-quiz choice survives reopening");
        reload.chooseQuiz(false,false);Progress forgotten=new Progress(preferences);
        check(forgotten.quizEnabled&&!forgotten.rememberQuizChoice,"unchecking remember removes saved override");
        choice.complete(0,0,true,42,2);choice.complete(0,0,false,33,3);
        check(choice.best[0]==42&&choice.best[12]==33,"quiz and free-run records stay separate");
        Progress progress=new Progress(false);progress.unlocked=3;
        for(int level=0;level<4;level++){
            g=new Game(progress);g.selectLevel(level);g.quizEnabled=false;g.togglePause();
            for(int i=0;i<8000&&g.state!=Game.State.WON;i++){
                check(g.state!=Game.State.CHALLENGE,"no quiz interrupt in free-run mode");
                if(g.state==Game.State.LOST)g.respawn();
                double target=0;for(Game.Item item:g.items)if(item.kind==Game.Kind.CRATE&&!item.used&&item.z>g.z&&item.z<g.z+45){target=item.x<=0?3:-3;break;}
                double drift=g.track.curve(g.z)*Math.min(g.speed,85)*.18;
                g.steering=Math.max(-1,Math.min(1,((target-g.x)*7+drift)/(11+g.speed*.025)));if(Game.gap(g.z+20)&&g.y==0)g.jump();g.update(.02);
            }
            check(g.state==Game.State.WON&&g.mission==2&&g.correct==0,"free-run checkpoints and finish work at level "+level);
        }
        SwingUtilities.invokeAndWait(()->{try{
            AsteroidsGame ast=new AsteroidsGame();ast.shield=100;double startX=ast.x,startY=ast.y;
            ast.keys.add("D");ast.step(.05);check(ast.x>startX&&ast.y==startY,"right is direct horizontal movement");
            ast.keys.clear();startX=ast.x;ast.step(.05);check(ast.x==startX,"no sideways drift after release");
            ast.keys.add("W");ast.step(.05);check(ast.y<startY&&ast.x==startX,"forward does not change facing or horizontal position");
            ast.keys.clear();ast.keys.add("SPACE");ast.step(.01);check(ast.shots.size()==2&&ast.shots.stream().allMatch(s->s.vx==0&&s.vy<0),"twin guns fire straight forward");
            ast.keys.clear();ast.rocks.clear();ast.shots.clear();ast.enemiesRemaining=0;AsteroidsGame.Enemy enemy=new AsteroidsGame.Enemy(300,220);enemy.hp=1;ast.enemies.add(enemy);ast.shots.add(new AsteroidsGame.Shot(300,220,0,0));ast.step(.005);
            check(!ast.enemies.contains(enemy)&&ast.score>=250,"enemy ships take damage and award score");
            ast.reset();for(int i=0;i<150;i++)ast.step(.01);check(!ast.enemies.isEmpty(),"enemies spawn without user intervention");
            for(int i=0;i<150;i++)ast.step(.01);check(!ast.hostileShots.isEmpty(),"enemy ships return fire");
            ast.enemies.clear();ast.rocks.clear();ast.shield=0;ast.hostileShots.clear();ast.hostileShots.add(new AsteroidsGame.Shot(ast.x,ast.y,0,0));ast.step(.005);check(ast.lives==2,"enemy projectile damages player");ast.dispose();
            GamePanel panel=new GamePanel(new Progress(false));panel.noQuizChoice.doClick();if(!panel.rememberChoice.isSelected())panel.rememberChoice.doClick();
            check(!panel.game.quizEnabled&&panel.game.progress.rememberQuizChoice,"both visible checkbox actions affect settings");
            if(args.length>0){
                File dir=new File(args[0]);ArcadeTest.save(panel,new File(dir,"sonic-no-quiz.png"),1200,800);
                panel.game.progress.unlocked=3;
                for(int i=0;i<4;i++){
                    panel.game.selectLevel(i);panel.game.togglePause();panel.game.vehicle=Vehicle.values()[i];panel.game.z=520;panel.game.elapsed=4.8;panel.game.speed=110;panel.game.wheelAngle=2;
                    ArcadeTest.save(panel,new File(dir,"sonic-"+Vehicle.values()[i].name().toLowerCase()+".png"),1200,800);
                    panel.game.togglePause();panel.inspectCharacter=true;
                    ArcadeTest.save(panel,new File(dir,"model-"+Vehicle.values()[i].name().toLowerCase()+".png"),1200,800);panel.inspectCharacter=false;
                }
            }
            panel.dispose();
        }catch(Exception e){throw new RuntimeException(e);}});
        System.out.println("PASS: fast running, boost, all vehicle pickups, remembered quiz choice, four free-run levels, fixed-facing movement, enemy combat.");
    }
}
