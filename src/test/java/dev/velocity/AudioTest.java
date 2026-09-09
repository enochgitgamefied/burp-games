package dev.velocity;

import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/** Shared audio and Games tab lifecycle checks. */
public final class AudioTest {
    static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
    static void await(BooleanSupplier test,String message)throws Exception{long until=System.nanoTime()+2_000_000_000L;while(!test.getAsBoolean()&&System.nanoTime()<until)Thread.sleep(5);check(test.getAsBoolean(),message);}
    static final class FakeOutput implements AudioEngine.Output {
        final AtomicInteger writes=new AtomicInteger();volatile boolean closed;volatile int silences;volatile boolean nonzero;
        public void write(byte[] bytes){check(!closed,"no writes after close");for(byte b:bytes)if(b!=0)nonzero=true;writes.incrementAndGet();try{Thread.sleep(5);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
        public synchronized void silence(){silences++;}
        public synchronized void close(){closed=true;}
    }
    private static void mixer()throws Exception{
        for(AudioEngine.Track track:AudioEngine.Track.values()){
            double energy=0,peak=0;for(int i=0;i<AudioEngine.RATE*3;i++){double v=AudioEngine.music(track,i);check(Double.isFinite(v),"finite music");energy+=v*v;peak=Math.max(peak,Math.abs(v));}check(energy>1&&peak<.5,"audible music with headroom: "+track);
        }
        for(AudioEngine.Cue cue:AudioEngine.Cue.values()){
            double energy=0;for(int i=0;i<AudioEngine.RATE*AudioEngine.duration(cue);i++){double v=AudioEngine.effect(cue,i);check(Double.isFinite(v)&&Math.abs(v)<.5,"finite bounded effect");energy+=v*v;}check(energy>.5,"audible effect: "+cue);check(AudioEngine.effect(cue,AudioEngine.RATE*2)==0,"effect terminates");
        }
        FakeOutput output=new FakeOutput();AtomicInteger opens=new AtomicInteger();AudioEngine audio=new AudioEngine(()->{opens.incrementAndGet();return output;});Object sonic=new Object(),arena=new Object();
        audio.setMuted(true);audio.start(sonic,AudioEngine.Track.SONIC);Thread.sleep(25);check(opens.get()==0,"muted launch never opens the audio device");
        audio.setMuted(false);await(()->output.writes.get()>2,"unmute starts actual PCM mixing");check(output.nonzero,"mixer sends nonzero audio");
        audio.setMuted(true);int writes=output.writes.get();Thread.sleep(30);check(output.writes.get()==writes&&output.silences>0,"mute flushes and stops all writes");
        audio.cue(AudioEngine.Cue.HIT);audio.setMuted(false);await(()->output.writes.get()>writes,"unmute resumes soundtrack");
        audio.start(arena,AudioEngine.Track.ARENA);audio.stop(sonic);check(audio.isPlaying(arena),"inactive game cannot stop active game's audio");
        audio.stop(arena);int paused=output.writes.get();Thread.sleep(30);check(output.writes.get()==paused,"pause leaves the mixer idle");
        audio.close();await(()->output.closed,"unload closes device");check(audio.isClosed()&&opens.get()==1,"one audio device per library");
        AudioEngine unavailable=new AudioEngine(()->{throw new IllegalStateException("no test device");});unavailable.start(sonic,AudioEngine.Track.SONIC);await(unavailable::isUnavailable,"missing audio device is handled without failing the game");unavailable.close();
    }
    private static void ui(String dir)throws Exception{
        SwingUtilities.invokeAndWait(()->{try{
            Progress progress=new Progress(false);GamesPanel hub=new GamesPanel(progress,AudioEngine.silent());
            hub.openGame("Asteroids");AsteroidsGame game=(AsteroidsGame)hub.game("Asteroids");
            game.getActionMap().get("ENTER").actionPerformed(null);check(game.started&&!game.paused&&hub.audio.isPlaying(game),"Enter starts soundtrack");
            hub.muteAudio.doClick();check(progress.muted&&hub.audio.isMuted(),"global mute is saved");hub.muteAudio.doClick();
            hub.audio.setMuted(true);game.ended=true;game.updateAudio();hub.audio.setMuted(false);
            check(!hub.audio.isActive(game),"ending while muted cannot resume music");
            hub.openGame("Turbo Tails");check(hub.game("Turbo Tails") instanceof TurboTailsGame,"Turbo Tails replaces fighter");
            check(game.paused&&game.keys.isEmpty()&&!hub.audio.isPlaying(game),"switching pauses previous game");
            JPanel turbo=hub.game("Turbo Tails");hub.showLibrary();hub.openGame("Turbo Tails");check(hub.game("Turbo Tails")==turbo,"kart session survives switching");
            hub.showLibrary();if(dir!=null)ArcadeTest.save(hub,new File(dir,"games-library.png"),1400,820);
            hub.dispose();check(hub.audio.isClosed(),"unload closes mixer");
        }catch(Exception ex){throw new RuntimeException(ex);}});
    }
    public static void main(String[] args)throws Exception{mixer();ui(args.length>0?args[0]:null);System.out.println("PASS: audio PCM, mute/resume, device failure, four-game switching and cleanup.");}
}
