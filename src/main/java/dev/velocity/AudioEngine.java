package dev.velocity;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.function.Supplier;

/** Original, locally synthesized music and effects. One bounded mixer per Games tab. */
final class AudioEngine implements AutoCloseable {
    enum Track { SONIC, SPACE, BREAKOUT, ARENA, KART }
    enum Cue { JUMP, COIN, PICKUP, SHOT, HIT, BOUNCE, BRICK, PUNCH, KICK, BLOCK, WIN, LOSE }
    static final int RATE=22050, FRAMES=512;
    static final AudioFormat FORMAT=new AudioFormat(RATE,16,1,true,false);
    interface Output extends AutoCloseable {
        void write(byte[] pcm);
        void silence();
        void close();
    }
    private static final class Device implements Output {
        private final SourceDataLine line;
        Device() throws LineUnavailableException {
            line=AudioSystem.getSourceDataLine(FORMAT);line.open(FORMAT,FRAMES*8);line.start();
        }
        public synchronized void write(byte[] pcm){if(line.isOpen()){if(!line.isRunning())line.start();line.write(pcm,0,pcm.length);}}
        public synchronized void silence(){if(line.isOpen()){line.stop();line.flush();}}
        public synchronized void close(){if(line.isOpen()){line.stop();line.flush();line.close();}}
    }
    private static final class Voice {final Cue cue;int frame;Voice(Cue cue){this.cue=cue;}}
    private final Object lock=new Object();
    private final Supplier<Output> factory;
    private final ArrayList<Voice> voices=new ArrayList<>();
    private final long[] lastCue=new long[Cue.values().length];
    private volatile Output output;
    private volatile boolean closed,muted,unavailable;
    private Thread worker;
    private Object owner;
    private Track track;
    private long musicFrame,revision;
    private double kartSpeed,kartPhase;
    void setKartSpeed(double value){synchronized(lock){kartSpeed=Double.isFinite(value)?Math.max(0,Math.min(1,value)):0;}}
    AudioEngine(){this(()->{try{return new Device();}catch(Exception e){throw new IllegalStateException(e);}});}
    AudioEngine(Supplier<Output> factory){this.factory=factory;}
    static AudioEngine silent(){return new AudioEngine(null);}
    void start(Object source,Track next){
        synchronized(lock){
            if(closed)return;
            if(owner!=source||track!=next){voices.clear();musicFrame=0;revision++;}
            owner=source;track=next;wake();
        }
    }
    void stop(Object source){
        boolean changed=false;
        synchronized(lock){if(owner==source){owner=null;track=null;voices.clear();revision++;changed=true;lock.notifyAll();}}
        if(changed)silence();
    }
    void finish(Object source,Cue cue){
        synchronized(lock){if(owner!=source||closed)return;track=null;voices.clear();if(!muted)voices.add(new Voice(cue));revision++;wake();}
    }
    void cue(Cue cue){
        synchronized(lock){
            if(closed||muted||owner==null)return;
            long now=System.nanoTime();if(now-lastCue[cue.ordinal()]<55_000_000L)return;
            lastCue[cue.ordinal()]=now;if(voices.size()>=8)voices.remove(0);voices.add(new Voice(cue));wake();
        }
    }
    void setMuted(boolean value){synchronized(lock){muted=value;voices.clear();revision++;wake();}if(value)silence();}
    boolean isMuted(){return muted;}
    boolean isClosed(){return closed;}
    boolean isUnavailable(){return unavailable;}
    boolean isActive(Object source){synchronized(lock){return owner==source&&track!=null&&!closed;}}
    boolean isPlaying(Object source){return isActive(source)&&!muted;}
    private void wake(){
        if(factory!=null&&!closed&&!unavailable&&!muted&&(track!=null||!voices.isEmpty())){
            if(worker==null){worker=new Thread(this::run,"Burp Games audio");worker.setDaemon(true);worker.start();}
        }
        lock.notifyAll();
    }
    private void silence(){Output current=output;if(current!=null)current.silence();}
    private void run(){
        try{
            Output opened=factory.get();output=opened;
            if(closed){opened.close();return;}
            while(true){
                byte[] pcm=new byte[FRAMES*2];long version;
                synchronized(lock){
                    while(!closed&&(muted||(track==null&&voices.isEmpty())))lock.wait();
                    if(closed)return;version=revision;
                    for(int i=0;i<FRAMES;i++){
                        double sample=track==null?0:track==Track.KART?nextKartSample():music(track,musicFrame++);
                        for(int j=voices.size()-1;j>=0;j--){Voice v=voices.get(j);sample+=effect(v.cue,v.frame++);if(v.frame>=duration(v.cue)*RATE)voices.remove(j);}
                        encode(pcm,i,sample);
                    }
                }
                // A pause/mute invalidates a buffer that was mixed just before the click.
                synchronized(opened){synchronized(lock){if(version!=revision||muted||closed)continue;}opened.write(pcm);}
            }
        }catch(InterruptedException e){Thread.currentThread().interrupt();}
        catch(Exception e){unavailable=true;}
        finally{Output current=output;if(current!=null)current.close();}
    }
    public void close(){
        synchronized(lock){closed=true;owner=null;track=null;voices.clear();revision++;lock.notifyAll();}
        Output current=output;if(current!=null)current.close();
    }
    static double duration(Cue cue){return switch(cue){case WIN->1.1;case LOSE->.7;case PICKUP->.38;case JUMP->.22;default->.16;};}
    private static double tone(double frequency,double t){return Math.sin(2*Math.PI*frequency*t);}
    private static double note(int midi){return 440*Math.pow(2,(midi-69)/12.0);}
    /** Deterministic noise avoids global random state or allocations in the audio thread. */
    private static double noise(long frame){long n=frame*1103515245L+12345;n^=n>>>13;return ((n&65535)/32767.5)-1;}
    static double effect(Cue cue,long frame){
        double t=frame/(double)RATE,d=duration(cue);if(t>=d)return 0;
        double envelope=Math.min(1,t/.004)*Math.pow(1-t/d,2);
        double sound=switch(cue){
            case JUMP->tone(330+1700*t,t);
            case COIN->tone(t<.07?1200:1800,t);
            case PICKUP->tone(note(72+(int)(t/.075)*3),t);
            case SHOT->tone(1250-6200*t,t)*.5+noise(frame)*.2;
            case HIT->noise(frame)*.8+tone(65,t)*.5;
            case BOUNCE->tone(470-700*t,t);
            case BRICK->tone(780,t)+tone(1170,t)*.25;
            case PUNCH->noise(frame)*Math.exp(-18*t)+tone(88,t)*.6;
            case KICK->noise(frame)*.6+tone(55,t)*.9;
            case BLOCK->tone(260,t)*.5+tone(1620,t)*.3+noise(frame)*.2;
            case WIN->tone(note(new int[]{60,64,67,72,76}[Math.min(4,(int)(t/.17))]),t);
            case LOSE->tone(280-230*t,t);
        };
        return sound*envelope*.20;
    }
    private double nextKartSample(){kartPhase=(kartPhase+2*Math.PI*(55+kartSpeed*140)/RATE)%(2*Math.PI);return (Math.sin(kartPhase)+Math.sin(kartPhase*2)*.25+Math.sin(kartPhase*3)*.1)*(.055+kartSpeed*.035);}
    static double kartEngine(long frame,double speed){double t=frame/(double)RATE;double f=55+speed*140;return (tone(f,t)+tone(f*2,t)*.25+tone(f*3,t)*.1)*(.055+speed*.035);}
    static double music(Track track,long frame){
        double t=frame/(double)RATE,bpm=switch(track){case SONIC->168;case SPACE->124;case BREAKOUT->132;case ARENA->138;case KART->150;};
        double beat=t*bpm/60,phase=beat-Math.floor(beat);int step=(int)(beat*2)%16,bar=(int)(beat/4)%4;
        int[] roots={45,41,48,43};int root=roots[bar]+(track==Track.BREAKOUT?12:0);
        int[] melody={12,19,24,19,16,19,14,19,12,19,24,26,24,19,16,14};
        double eighth=(beat*2)%1;
        double lead=(tone(note(root+melody[step]),t)+.22*tone(note(root+melody[step])*2,t))*.036*Math.pow(1-eighth,.5);
        double bass=tone(note(root),t)*.055*Math.exp(-phase*4);
        double kick=tone(48+75*Math.exp(-phase*28),t)*Math.exp(-phase*17)*.09;
        double hat=noise(frame)*Math.exp(-eighth*22)*.019;
        double snare=((int)beat%2==1)?noise(frame)*Math.exp(-phase*23)*.034:0;
        if(track==Track.ARENA)lead*=.5;
        return lead+bass+kick+hat+snare;
    }
    static void encode(byte[] buffer,int frame,double sample){int v=(int)(Math.max(-.92,Math.min(.92,sample))*32767);buffer[frame*2]=(byte)v;buffer[frame*2+1]=(byte)(v>>8);}
}
