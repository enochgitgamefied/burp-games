package dev.velocity;

import java.util.prefs.Preferences;

/** Local preferences and separate records for quiz / uninterrupted runs. */
final class Progress {
    final Preferences store;
    int unlocked,turboUnlocked,kartDriver,kartSecond=1,kartPlayers=1;
    final double[] kartBest=new double[5];
    boolean quizEnabled=true,rememberQuizChoice=true,muted;
    final double[] best=new double[24];
    final int[] medals=new int[24];
    String warning="";
    Progress(boolean persistent){this(preferences(persistent));if(persistent&&store==null)warning="Settings are available for this session only.";}
    Progress(Preferences preferences){
        store=preferences;
        if(store!=null)try{
            unlocked=Math.max(0,Math.min(3,store.getInt("unlocked",0)));
            muted=store.getBoolean("muted",false);
            turboUnlocked=Math.max(0,Math.min(4,store.getInt("turboUnlocked",0)));
            kartDriver=Math.max(0,Math.min(3,store.getInt("kartDriver",0)));
            kartSecond=Math.max(0,Math.min(3,store.getInt("kartSecond",1)));if(kartSecond==kartDriver)kartSecond=(kartDriver+1)%4;
            kartPlayers=store.getInt("kartPlayers",1)==2?2:1;
            for(int i=0;i<5;i++){double t=store.getDouble("v6-kart-time"+i,0);kartBest[i]=Double.isFinite(t)&&t>0?t:0;}
            rememberQuizChoice=store.getBoolean("rememberQuizChoice",true);
            quizEnabled=!rememberQuizChoice||store.getBoolean("quizEnabled",true);
            for(int i=0;i<best.length;i++){
                best[i]=store.getDouble("v3-time"+i,0);if(!Double.isFinite(best[i])||best[i]<0)best[i]=0;
                medals[i]=Math.max(0,Math.min(3,store.getInt("v3-medal"+i,0)));
            }
        }catch(Exception e){warning="Saved settings could not be read.";}
    }
    private static Preferences preferences(boolean enabled){if(!enabled)return null;try{return Preferences.userRoot().node("dev/velocity/burp-games");}catch(Exception e){return null;}}
    void chooseQuiz(boolean enabled,boolean remember){
        quizEnabled=enabled;rememberQuizChoice=remember;
        if(store!=null)try{
            store.putBoolean("rememberQuizChoice",remember);
            if(remember)store.putBoolean("quizEnabled",enabled);else store.remove("quizEnabled");
            store.flush();
        }catch(Exception e){warning="This choice is available for this session only.";}
    }
    void setMuted(boolean value){
        muted=value;
        if(store!=null)try{store.putBoolean("muted",value);store.flush();}
        catch(Exception e){warning="Audio preference is available for this session only.";}
    }
    void saveTurboLevel(int value){
        int next=Math.max(turboUnlocked,Math.max(0,Math.min(4,value)));if(next==turboUnlocked)return;turboUnlocked=next;
        if(store!=null)try{store.putInt("turboUnlocked",next);store.flush();}catch(Exception e){warning="Progress is available for this session only.";}
    }
    void chooseKart(int first,int second,int players){
        kartDriver=first;kartSecond=second;kartPlayers=players;
        if(store!=null)try{store.putInt("kartDriver",first);store.putInt("kartSecond",second);store.putInt("kartPlayers",players);store.flush();}catch(Exception e){warning="Driver choices are available for this session only.";}
    }
    void completeKart(int level,int place,double seconds){
        if(level<0||level>=5||!Double.isFinite(seconds)||seconds<=0)return;
        if(place<=2)saveTurboLevel(Math.min(4,level+1));
        if(kartBest[level]==0||seconds<kartBest[level]){kartBest[level]=seconds;if(store!=null)try{store.putDouble("v6-kart-time"+level,seconds);store.flush();}catch(Exception e){warning="Race record is available for this session only.";}}
    }
    int key(int level,int difficulty,boolean quiz){return level*3+difficulty+(quiz?0:12);}
    void complete(int level,int difficulty,boolean quiz,double seconds,int medal){
        unlocked=Math.max(unlocked,Math.min(3,level+1));int key=key(level,difficulty,quiz);
        if(best[key]==0||seconds<best[key])best[key]=seconds;medals[key]=Math.max(medals[key],medal);
        if(store!=null)try{store.putInt("unlocked",unlocked);store.putDouble("v3-time"+key,best[key]);store.putInt("v3-medal"+key,medals[key]);}
        catch(Exception e){warning="Progress is available for this session only.";}
    }
}
