package dev.velocity;

import javax.swing.*;import java.awt.event.*;import java.util.concurrent.*;import java.util.function.BooleanSupplier;

/** Packaged-JAR native component integration, including a real Swing timer and window lifecycle. */
public final class KartIntegrationTest {
    static JFrame frame;static GamesPanel hub;static TurboTailsGame game;
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static <T>T edt(Callable<T> task)throws Exception{FutureTask<T> t=new FutureTask<>(task);SwingUtilities.invokeAndWait(t);return t.get();}
    static void until(Callable<Boolean> task,String text)throws Exception{long end=System.nanoTime()+6_000_000_000L;while(System.nanoTime()<end){if(edt(task))return;Thread.sleep(25);}throw new AssertionError(text);}
    static void key(int code,boolean pressed){KeyEvent event=new KeyEvent(game.canvas,pressed?KeyEvent.KEY_PRESSED:KeyEvent.KEY_RELEASED,System.currentTimeMillis(),0,code,KeyEvent.CHAR_UNDEFINED);for(KeyListener listener:game.canvas.getKeyListeners()){if(pressed)listener.keyPressed(event);else listener.keyReleased(event);}}
    public static void main(String[] args){
        Thread watchdog=new Thread(()->{try{Thread.sleep(40000);}catch(InterruptedException e){return;}System.err.println("Native integration timed out");System.exit(1);});watchdog.setDaemon(true);watchdog.start();
        try{
            if(args.length>0&&args[0].equals("reduced")){check(ModuleLayer.boot().findModule("jdk.unsupported.desktop").isEmpty(),"desktop interop module excluded");check(ModuleLayer.boot().findModule("jdk.jsobject").isEmpty(),"JavaScript bridge module excluded");}
            edt(()->{hub=new GamesPanel(new Progress(false),AudioEngine.silent());frame=new JFrame("Native kart — component validation");frame.setContentPane(hub);frame.setSize(1280,860);frame.setVisible(true);hub.openGame("Turbo Tails");game=(TurboTailsGame)hub.game("Turbo Tails");return null;});
            // Exercise the actual Swing buttons, canvas event handlers and timer, not a second game implementation.
            edt(()->{((JButton)game.driverCards[3]).doClick();game.startButton.doClick();return null;});
            Thread.sleep(300);
            edt(()->{game.canvas.requestFocusInWindow();if(game.race.state==KartRace.State.PAUSED)game.togglePause();return null;});
            long began=System.nanoTime();until(()->game.race.state==KartRace.State.RACING,"three-second native countdown completes");double wait=(System.nanoTime()-began)/1e9;check(wait<4,"countdown stays near wall time");
            edt(()->{check(game.race.player1.driver==KartDriver.AMY,"driver card chooses Amy");key(KeyEvent.VK_W,true);key(KeyEvent.VK_SHIFT,true);return null;});
            until(()->game.race.player1.speed>112,"native W and Shift accelerate and boost");
            double before=edt(()->game.race.elapsed);Thread.sleep(1000);double advanced=edt(()->game.race.elapsed)-before;check(advanced>.85&&advanced<1.3,"race simulation follows elapsed wall time while rendering");
            edt(()->{key(KeyEvent.VK_W,false);key(KeyEvent.VK_SHIFT,false);key(KeyEvent.VK_P,true);key(KeyEvent.VK_P,false);check(game.race.state==KartRace.State.PAUSED,"native P pauses");check(!hub.audio.isActive(game),"pause silences engine");key(KeyEvent.VK_M,true);key(KeyEvent.VK_M,false);check(hub.muteAudio.isSelected()&&hub.audio.isMuted(),"M synchronizes header mute");hub.muteAudio.doClick();check(!game.progress.muted,"header synchronizes native game");
                for(FocusListener listener:game.canvas.getFocusListeners())listener.focusLost(new FocusEvent(game.canvas,FocusEvent.FOCUS_LOST));check(game.held.isEmpty(),"focus loss releases keys");hub.showLibrary();check(!game.timer.isRunning(),"hidden game timer stops");hub.openGame("Turbo Tails");check(hub.game("Turbo Tails")==game&&game.race.state==KartRace.State.PAUSED,"reopen keeps paused session");hub.dispose();check(game.disposed&&!game.timer.isRunning(),"unload stops timer");frame.dispose();return null;});
            edt(()->{hub=new GamesPanel(new Progress(false),AudioEngine.silent());frame=new JFrame("Native kart — reload validation");frame.setContentPane(hub);frame.setSize(960,740);frame.setVisible(true);hub.openGame("Turbo Tails");check(((TurboTailsGame)hub.game("Turbo Tails")).race.state==KartRace.State.MENU,"reload starts a fresh native garage");hub.dispose();frame.dispose();return null;});
            System.out.printf("PASS: packaged native JAR, driver/start buttons, real timer (countdown wait %.2fs), input, mute sync, focus, tab hide/reopen, unload and reload.%n",wait);System.exit(0);
        }catch(Throwable e){e.printStackTrace();try{edt(()->{if(hub!=null)hub.dispose();if(frame!=null)frame.dispose();return null;});}catch(Exception ignored){}System.exit(1);}
    }
}
