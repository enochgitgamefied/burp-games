package dev.velocity;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;

/** Uses independent JVMs and the platform preference backend, under an isolated test node. */
public final class SettingsRestartTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        if(args.length>0){
            Preferences store=Preferences.userRoot().node(args[1]);
            if(args[0].equals("cleanup")){store.removeNode();Preferences.userRoot().flush();return;}
            SwingUtilities.invokeAndWait(()->{
                Progress progress=new Progress(store);GamesPanel hub=new GamesPanel(progress);hub.openGame("Sonic 3D");GamePanel sonic=(GamePanel)hub.game("Sonic 3D");
                try{
                    switch(args[0]){
                        case "write" -> {
                            check(sonic.rememberChoice.isSelected(),"remember should default on for a fresh install");
                            sonic.noQuizChoice.doClick();hub.muteAudio.doClick();progress.saveTurboLevel(3);progress.chooseKart(3,2,2);progress.completeKart(1,1,32.125);
                            check(hub.audio.isMuted(),"mute button silences audio");
                            check(!sonic.game.quizEnabled,"without-quiz option must set game mode");
                        }
                        case "read" -> {
                            check(progress.turboUnlocked==3,"kart circuit unlock survives JVM restart");
                            check(progress.kartDriver==3&&progress.kartSecond==2&&progress.kartPlayers==2,"kart roster and player mode survive JVM restart");
                            check(progress.kartBest[1]==32.125,"native circuit time survives JVM restart");
                            check(!sonic.game.quizEnabled,"no-quiz mode must survive an actual JVM restart");
                            check(hub.muteAudio.isSelected()&&hub.audio.isMuted(),"mute must survive an actual JVM restart");
                            check(sonic.noQuizChoice.isSelected()&&!sonic.quizChoice.isSelected()&&sonic.rememberChoice.isSelected(),"entry options must reflect the remembered mode");
                            sonic.game.togglePause();sonic.game.z=sonic.game.length()*.32-.2;sonic.game.speed=90;sonic.game.update(.02);
                            check(sonic.game.state==Game.State.RUNNING&&sonic.game.mission==1,"restored mode must skip quiz prompts");
                        }
                        case "forget" -> {sonic.rememberChoice.doClick();hub.muteAudio.doClick();}
                        case "default" -> {check(sonic.game.quizEnabled&&!sonic.rememberChoice.isSelected(),"explicitly forgetting must remove the saved mode");check(!hub.audio.isMuted()&&!hub.muteAudio.isSelected(),"unmute is also remembered");}
                        default -> throw new AssertionError("Unknown test mode");
                    }
                }finally{hub.dispose();}
            });
            return;
        }
        String node="dev/velocity/burp-games-preference-test/"+UUID.randomUUID();
        try{for(String mode:new String[]{"write","read","forget","default"})launch(mode,node);}
        finally{launch("cleanup",node);}
        System.out.println("PASS: real process restart restores no-quiz, mute, kart roster, mode, circuit unlocks and race records; forgetting clears it; isolated test settings removed.");
    }
    private static void launch(String mode,String node)throws Exception{
        Process child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Djava.awt.headless=true","-cp",System.getProperty("java.class.path"),SettingsRestartTest.class.getName(),mode,node).inheritIO().start();
        if(!child.waitFor(20,TimeUnit.SECONDS)){child.destroyForcibly();throw new AssertionError("Preference process timed out: "+mode);}
        check(child.exitValue()==0,"Preference process failed: "+mode);
    }
}
