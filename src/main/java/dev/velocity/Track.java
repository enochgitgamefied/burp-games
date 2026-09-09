package dev.velocity;

/** Smooth, sampled route with lateral offsets, climbs, descents and banked corners. */
class Track {
    final int level;
    final double[] x=new double[6001],z=new double[6001];
    Track(int level) {this(level,true);}
    Track(int level,boolean generate) {
        this.level=level;if(!generate)return;
        for(int s=1;s<x.length;s++){double a=heading(s-.5);x[s]=x[s-1]+Math.sin(a);z[s]=z[s-1]+Math.cos(a);}
    }
    double heading(double s) { return (.40+level*.13)*Math.sin(s/150.0)+.22*Math.sin(s/75.0); }
    double height(double s) { return (6+level*2.2)*Math.sin(s/130.0)+3*Math.sin(s/65.0); }
    double slope(double s) { return (height(s+1)-height(s-1))/2; }
    double curve(double s) { return (heading(s+1)-heading(s-1))/2; }
    double bank(double s) { return Math.max(-.20,Math.min(.20,-curve(s)*12)); }
    double sample(double[] v,double s) { int i=Math.max(0,Math.min(v.length-2,(int)s));return v[i]+(v[i+1]-v[i])*(s-i); }
    double[] point(double lateral,double up,double s) {
        double a=heading(s),b=bank(s);
        return new double[]{sample(x,s)+lateral*Math.cos(a),height(s)+up+lateral*Math.sin(b),sample(z,s)-lateral*Math.sin(a)};
    }
    double ramp(double s) {
        double p=s%600;return p>=384 && p<396?(p-384)*.10:0;
    }
}
