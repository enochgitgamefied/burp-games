package dev.velocity;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;

public final class GameTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static Game running(){Game g=new Game();g.togglePause();return g;}
    public static void main(String[] args)throws Exception{
        Game g=new Game();g.update(.02);check(g.z==0,"ready freezes");
        g.togglePause();g.throttle=true;for(int i=0;i<50;i++)g.update(.02);check(g.speed>15,"throttle accelerates");
        g.togglePause();double z=g.z;g.update(.02);check(g.z==z,"pause freezes");
        g=running();g.jump();g.update(.02);check(g.y>0,"jump rises");double vy=g.vy;g.jump();check(vy==g.vy,"no double jump");
        for(int i=0;i<70;i++)g.update(.02);check(g.y==0,"lands");
        g=running();g.z=400.2;g.speed=0;g.braking=true;g.update(.02);check(g.state==Game.State.LOST,"missed ramp crashes");
        for(Vehicle vehicle:Vehicle.values()){
            g=running();g.vehicle=vehicle;g.z=395.8;g.speed=160;g.boosting=true;g.update(.02);
            check(g.state==Game.State.LOST&&g.y==0&&g.vy==0,"no automatic launch for "+vehicle);
            g=running();g.vehicle=vehicle;g.items.clear();g.items.add(new Game.Item(Game.Kind.CRATE,0,150));g.z=148;g.speed=140;g.update(.04);
            check(g.hits==1&&g.y==0&&g.vy==0,"obstacle hit does not auto jump: "+vehicle);
            g=running();g.vehicle=vehicle;g.items.clear();g.z=375;g.speed=90;g.jump();
            for(int i=0;i<30;i++)g.update(.02);
            check(g.z>408&&g.state==Game.State.RUNNING,"manual jump clears gap: "+vehicle);
        }
        g=running();g.z=100;g.checkpoint=80;g.crash("test");g.respawn();check(g.z==80&&g.state==Game.State.RUNNING&&g.invincible>0,"checkpoint respawn");
        g=running();g.state=Game.State.CHALLENGE;g.answer(1);check(g.correct==0&&g.answered,"wrong answer produces feedback");g.answer(0);check(g.correct==0,"cannot farm answer points");g.continueMission();check(g.mission==1,"checkpoint advances once");
        Progress progress=new Progress(false);
        for(int level=0;level<4;level++){
            g=new Game(progress);g.selectLevel(level);check(g.level==level,"level unlock");g.difficulty=1;g.togglePause();
            for(int i=0;i<14000&&g.state!=Game.State.WON;i++){
                if(g.state==Game.State.CHALLENGE){g.answer(g.question().correct());g.continueMission();}
                if(g.state==Game.State.LOST)g.respawn();
                double target=0;
                for(Game.Item item:g.items)if(item.kind==Game.Kind.CRATE&&!item.used&&item.z>g.z&&item.z<g.z+35){target=item.x<=0?3:-3;break;}
                double drift=g.track.curve(g.z)*Math.min(g.speed,85)*.32;
                g.steering=Math.max(-1,Math.min(1,((target-g.x)*6+drift)/(11+g.speed*.025)));
                if(Game.gap(g.z+20)&&g.y==0)g.jump();
                g.throttle=true;g.update(.02);
            }
            check(g.state==Game.State.WON,"full level traversable: "+level);
            check(g.correct==2&&g.mission==2,"both missions completed");check(progress.best[level*3+1]>0,"time saved by difficulty");
        }
        check(progress.unlocked==3,"all levels unlocked");
        Track track=new Track(3);check(Math.abs(track.height(50))>3,"hills exist");check(Math.abs(track.heading(60))>.3,"corners exist");check(track.slope(50)*track.slope(190)<0,"climb and descent");
        Map<String,Object> calls=new HashMap<>();new Extension().initialize((MontoyaApi)mock(MontoyaApi.class,calls));
        check("Games".equals(calls.get("tabTitle")),"tab named Games");check(calls.get("registerSuiteTab") instanceof GamesPanel,"library registered");
        ((ExtensionUnloadingHandler)calls.get("registerUnloadingHandler")).extensionUnloaded();SwingUtilities.invokeAndWait(()->{});
        System.out.println("PASS: runner physics, ramps, checkpoints, mission scoring, all 4 levels, progress, Games tab/unload.");
    }
    static Object mock(Class<?> type,Map<String,Object> calls){return Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
        if(m.getName().equals("registerSuiteTab")){calls.put(m.getName(),a[1]);calls.put("tabTitle",a[0]);}
        if(m.getName().equals("registerUnloadingHandler"))calls.put(m.getName(),a[0]);
        if(m.getReturnType().isInterface())return mock(m.getReturnType(),calls);return null;
    });}
}
