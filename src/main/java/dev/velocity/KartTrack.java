package dev.velocity;

import java.awt.Color;
import java.util.Arrays;

/** Arc-length sampled closed circuits using the Sonic track coordinate system. */
final class KartTrack extends Track {
    record Theme(String name,Color sky,Color horizon,Color ground,Color verge,Color road,Color accent){}
    private static Color c(int value){return new Color(value);}
    static final Theme[] THEMES={
        new Theme("Emerald Circuit",c(0x72c8ea),c(0xe3f3d4),c(0x258db1),c(0x49a65b),c(0x596d7b),c(0xffd867)),
        new Theme("Sunset Mesa",c(0xd9876b),c(0xffd597),c(0xc78158),c(0xdba476),c(0x776164),c(0xffd384)),
        new Theme("Polar Pass",c(0x77aaca),c(0xebf7ff),c(0x9fc6db),c(0xe0eef2),c(0x66849b),c(0x75ebf5)),
        new Theme("Neon Marina",c(0x0d193d),c(0x624575),c(0x152e52),c(0x2c3f60),c(0x354158),c(0x63efd3)),
        new Theme("Cloud Garden",c(0x97afdc),c(0xf7d9ce),c(0xa9b8d7),c(0x82b99b),c(0x647884),c(0xfaa3cb))
    };
    final int length;final Theme theme;
    final double[] heights=new double[6001],cosines=new double[6001],sines=new double[6001],banks=new double[6001];
    KartTrack(int level){
        super(level,false);theme=THEMES[level];int count=2400;double[] px=new double[count+1],pz=new double[count+1],distance=new double[count+1];
        double radius=185+level*13;
        for(int i=0;i<=count;i++){
            double t=i*Math.PI*2/count;
            px[i]=radius*Math.sin(t)+(26+level*3)*Math.sin(2*t);
            pz[i]=radius*1.18*(1-Math.cos(t))+18*Math.sin(3*t);
            if(i>0)distance[i]=distance[i-1]+Math.hypot(px[i]-px[i-1],pz[i]-pz[i-1]);
        }
        length=(int)Math.round(distance[count]);
        for(int s=0;s<length;s++){
            double d=s*distance[count]/length;int i=Arrays.binarySearch(distance,d);if(i<0)i=-i-2;i=Math.max(0,Math.min(count-1,i));
            double u=(d-distance[i])/(distance[i+1]-distance[i]);x[s]=px[i]+(px[i+1]-px[i])*u;z[s]=pz[i]+(pz[i+1]-pz[i])*u;
            double t=s*Math.PI*2/length;heights[s]=(7+level*1.8)*Math.sin(2*t)+3*Math.sin(4*t+.2)-3*Math.sin(.2);
        }
        x[length]=x[0];z[length]=z[0];heights[length]=heights[0];
        for(int i=0;i<length;i++){double dx=value(x,i+1)-value(x,i-1),dz=value(z,i+1)-value(z,i-1),norm=Math.hypot(dx,dz);sines[i]=dx/norm;cosines[i]=dz/norm;}
        sines[length]=sines[0];cosines[length]=cosines[0];
        for(int i=0;i<length;i++)banks[i]=Math.sin(Math.max(-.16,Math.min(.16,-curve(i)*9)));banks[length]=banks[0];
    }
    double wrap(double s){return s-Math.floor(s/length)*length;}
    private double value(double[] values,double s){double q=wrap(s);int i=(int)q;return values[i]+(values[i+1]-values[i])*(q-i);}
    @Override double heading(double s){return Math.atan2(value(sines,s),value(cosines,s));}
    @Override double height(double s){return value(heights,s);}
    @Override double slope(double s){return (height(s+1)-height(s-1))*.5;}
    @Override double curve(double s){double a=heading(s+1)-heading(s-1);return Math.atan2(Math.sin(a),Math.cos(a))*.5;}
    @Override double bank(double s){return Math.asin(value(banks,s));}
    @Override double[] point(double lateral,double up,double s){
        double q=wrap(s);int i=(int)q;double u=q-i;
        return new double[]{x[i]+(x[i+1]-x[i])*u+lateral*(cosines[i]+(cosines[i+1]-cosines[i])*u),
            heights[i]+(heights[i+1]-heights[i])*u+up+lateral*(banks[i]+(banks[i+1]-banks[i])*u),
            z[i]+(z[i+1]-z[i])*u-lateral*(sines[i]+(sines[i+1]-sines[i])*u)};
    }
    @Override double ramp(double s){return 0;}
    double pad(int index){return length*(.16+index*.21);}
    double padLane(int index){return index%2==0?-4.7:4.7;}
    int coinCount(){return length/44;}
    double coin(int index){return (index+.55)*length/coinCount();}
    double coinLane(int index){return new double[]{-5.2,-1.8,1.8,5.2}[(index/3)%4];}
}
