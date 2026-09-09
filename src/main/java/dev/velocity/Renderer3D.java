package dev.velocity;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/** Perspective-correct depth buffer and smooth vertex lighting, entirely in Java. */
final class Renderer3D {
    record Point(double x, double y, double z, double light) {}
    record UVPoint(Point p,double u,double v) {}
    static final class Texture {
        final int width,height;final int[] pixels;
        Texture(BufferedImage image){width=image.getWidth();height=image.getHeight();pixels=image.getRGB(0,0,width,height,null,0,width);}
    }
    private Color fogColor=new Color(130,204,199);
    private Track track;
    private double trackOrigin,originX,originY,originZ,headingCos,headingSin;
    private BufferedImage image;
    private int[] pixels;
    private double[] depth;
    private int width, height;
    private double focal, centerX, centerY, focusX, focusZ, cameraY, distance, yaw, pitch;

    void begin(int width, int height, double x, double z, boolean preview, double angle) {
        track=null;
        // Bound software rendering cost on Retina / very large Burp windows.
        double scale = Math.min(1, 1100.0 / Math.max(1, width));
        int rw = Math.max(1, (int)(width*scale)), rh = Math.max(1, (int)(height*scale));
        if (image == null || rw != this.width || rh != this.height) {
            this.width=rw; this.height=rh; image=new BufferedImage(rw,rh,BufferedImage.TYPE_INT_ARGB);
            pixels=((DataBufferInt)image.getRaster().getDataBuffer()).getData(); depth=new double[rw*rh];
        }
        Arrays.fill(pixels,0); Arrays.fill(depth,0);
        focal=Math.min(rw*.88,rh*1.2); centerX=rw*(preview?.71:.5); centerY=rh*(preview?.59:.48);
        focusX=preview?x:x*.38; focusZ=z; cameraY=preview?1.72:4.4;
        distance=preview?6.5:10.5; yaw=preview?angle:0; pitch=preview?.035:.185;
    }
    void follow(Track track,double s){this.track=track;trackOrigin=s;double[] origin=track.point(0,0,s);originX=origin[0];originY=origin[1];originZ=origin[2];headingCos=Math.cos(track.heading(s));headingSin=Math.sin(track.heading(s));pitch=.185-Math.atan(track.slope(s))*.8;}
    void arena(int width,int height,double targetX,double targetZ,double separation){
        begin(width,height,0,0,false,0);
        focusX=targetX;focusZ=targetZ;cameraY=2.55;distance=9.0+Math.max(0,separation-3)*.85;
        yaw=.14;pitch=.14;centerY=this.height*.56;focal=this.height*1.50;
    }
    void portrait(int width,int height,double targetY,double zoom,double angle){
        begin(width,height,0,0,false,0);focusX=focusZ=0;cameraY=targetY;distance=zoom;
        yaw=angle;pitch=0;centerY=this.height*.5;focal=this.height*1.24;
    }
    void atmosphere(Color color){fogColor=color;}
    void pace(double speed){focal*=Math.max(.76,1-Math.max(0,speed-65)*.0025);}
    BufferedImage image() { return image; }
    Point camera(double x,double y,double z,double light) {
        if(track!=null){
            double[] p=track.point(x,y,z);double px=p[0]-originX,pz=p[2]-originZ;
            x=px*headingCos-pz*headingSin;y=p[1]-originY;z=focusZ+px*headingSin+pz*headingCos;
        }
        double dx=x-focusX, dz=z-focusZ;
        double xx=dx*Math.cos(yaw)-dz*Math.sin(yaw);
        double zz=dx*Math.sin(yaw)+dz*Math.cos(yaw)+distance, yy=y-cameraY;
        return new Point(xx,yy*Math.cos(pitch)+zz*Math.sin(pitch),zz*Math.cos(pitch)-yy*Math.sin(pitch),light);
    }
    void polygon(Color color, double... xyz) {
        Point[] points=new Point[xyz.length/3];
        for(int i=0;i<points.length;i++)points[i]=camera(xyz[3*i],xyz[3*i+1],xyz[3*i+2],1);
        polygon(color,points);
    }
    void polygon(Color color, Point... input) {
        Point[] clipped=new Point[input.length+2]; int n=0;
        Point last=input[input.length-1];
        for(Point next:input) {
            if((last.z>=.3)!=(next.z>=.3)) {
                double t=(.3-last.z)/(next.z-last.z);
                clipped[n++]=new Point(last.x+(next.x-last.x)*t,last.y+(next.y-last.y)*t,.3,last.light+(next.light-last.light)*t);
            }
            if(next.z>=.3)clipped[n++]=next; last=next;
        }
        for(int i=1;i<n-1;i++) triangle(color,clipped[0],clipped[i],clipped[i+1]);
    }
    void polygon(Color color,Point a,Point b,Point c){
        if(a.z>=.3&&b.z>=.3&&c.z>=.3)triangle(color,a,b,c);
        else if(a.z>=.3||b.z>=.3||c.z>=.3)polygon(color,new Point[]{a,b,c});
    }
    private void triangle(Color c,Point a,Point b,Point d) {
        double ax=centerX+focal*a.x/a.z, ay=centerY-focal*a.y/a.z;
        double bx=centerX+focal*b.x/b.z, by=centerY-focal*b.y/b.z;
        double cx=centerX+focal*d.x/d.z, cy=centerY-focal*d.y/d.z;
        double area=(bx-ax)*(cy-ay)-(by-ay)*(cx-ax);
        if(Math.abs(area)<1e-7)return;
        int minX=Math.max(0,(int)Math.floor(Math.min(ax,Math.min(bx,cx))));
        int maxX=Math.min(width-1,(int)Math.ceil(Math.max(ax,Math.max(bx,cx))));
        int minY=Math.max(0,(int)Math.floor(Math.min(ay,Math.min(by,cy))));
        int maxY=Math.min(height-1,(int)Math.ceil(Math.max(ay,Math.max(by,cy))));
        double za=1/a.z,zb=1/b.z,zc=1/d.z;
        double dax=(by-cy)/area, dbx=(cy-ay)/area;
        boolean flat=a.light==b.light&&a.light==d.light;
        int flatRgb=0xff000000|((int)Math.min(255,c.getRed()*a.light)<<16)|((int)Math.min(255,c.getGreen()*a.light)<<8)|(int)Math.min(255,c.getBlue()*a.light);
        for(int y=minY;y<=maxY;y++) {
            double wa=((bx-(minX+.5))*(cy-(y+.5))-(by-(y+.5))*(cx-(minX+.5)))/area;
            double wb=((cx-(minX+.5))*(ay-(y+.5))-(cy-(y+.5))*(ax-(minX+.5)))/area;
            for(int x=minX;x<=maxX;x++,wa+=dax,wb+=dbx) {
                double wc=1-wa-wb;
                if(wa<-.00001 || wb<-.00001 || wc<-.00001)continue;
                double iz=wa*za+wb*zb+wc*zc; int index=y*width+x;
                if(iz<=depth[index])continue;
                depth[index]=iz;
                if(flat&&iz>=1.0/65){pixels[index]=flatRgb;continue;}
                double light=(wa*za*a.light+wb*zb*b.light+wc*zc*d.light)/iz;
                double fog=Math.max(0,Math.min(.78,(1/iz-65)/160));
                int r=(int)Math.min(255,c.getRed()*light*(1-fog)+fogColor.getRed()*fog);
                int g=(int)Math.min(255,c.getGreen()*light*(1-fog)+fogColor.getGreen()*fog);
                int bch=(int)Math.min(255,c.getBlue()*light*(1-fog)+fogColor.getBlue()*fog);
                pixels[index]=0xff000000|(r<<16)|(g<<8)|bch;
            }
        }
    }
    void textured(Texture texture,int tint,UVPoint a,UVPoint b,UVPoint c){
        if(a.p.z>=.3&&b.p.z>=.3&&c.p.z>=.3){textureTriangle(texture,tint,a,b,c);return;}
        UVPoint[] in={a,b,c},out=new UVPoint[5];int n=0;UVPoint last=c;
        for(UVPoint next:in){
            if((last.p.z>=.3)!=(next.p.z>=.3)){
                double t=(.3-last.p.z)/(next.p.z-last.p.z);Point p=last.p,q=next.p;
                out[n++]=new UVPoint(new Point(p.x+(q.x-p.x)*t,p.y+(q.y-p.y)*t,.3,p.light+(q.light-p.light)*t),last.u+(next.u-last.u)*t,last.v+(next.v-last.v)*t);
            }
            if(next.p.z>=.3)out[n++]=next;last=next;
        }
        for(int i=1;i<n-1;i++)textureTriangle(texture,tint,out[0],out[i],out[i+1]);
    }
    private void textureTriangle(Texture tex,int tint,UVPoint aa,UVPoint bb,UVPoint cc){
        Point a=aa.p,b=bb.p,c=cc.p;
        double ax=centerX+focal*a.x/a.z,ay=centerY-focal*a.y/a.z,bx=centerX+focal*b.x/b.z,by=centerY-focal*b.y/b.z,cx=centerX+focal*c.x/c.z,cy=centerY-focal*c.y/c.z;
        double area=(bx-ax)*(cy-ay)-(by-ay)*(cx-ax);if(Math.abs(area)<1e-7)return;
        int minX=Math.max(0,(int)Math.floor(Math.min(ax,Math.min(bx,cx)))),maxX=Math.min(width-1,(int)Math.ceil(Math.max(ax,Math.max(bx,cx))));
        int minY=Math.max(0,(int)Math.floor(Math.min(ay,Math.min(by,cy)))),maxY=Math.min(height-1,(int)Math.ceil(Math.max(ay,Math.max(by,cy))));
        double za=1/a.z,zb=1/b.z,zc=1/c.z,dax=(by-cy)/area,dbx=(cy-ay)/area;
        double tr=((tint>>16)&255)/255.,tg=((tint>>8)&255)/255.,tb=(tint&255)/255.;
        for(int y=minY;y<=maxY;y++){
            double wa=((bx-minX-.5)*(cy-y-.5)-(by-y-.5)*(cx-minX-.5))/area,wb=((cx-minX-.5)*(ay-y-.5)-(cy-y-.5)*(ax-minX-.5))/area;
            for(int x=minX;x<=maxX;x++,wa+=dax,wb+=dbx){
                double wc=1-wa-wb;if(wa<-.00001||wb<-.00001||wc<-.00001)continue;
                double iz=wa*za+wb*zb+wc*zc;int at=y*width+x;if(iz<=depth[at])continue;
                double u=(wa*za*aa.u+wb*zb*bb.u+wc*zc*cc.u)/iz,v=(wa*za*aa.v+wb*zb*bb.v+wc*zc*cc.v)/iz;
                int tx=Math.max(0,Math.min(tex.width-1,(int)(u*tex.width))),ty=Math.max(0,Math.min(tex.height-1,(int)((1-v)*tex.height)));
                int pixel=tex.pixels[ty*tex.width+tx];if((pixel>>>24)<100)continue;
                double light=(wa*za*a.light+wb*zb*b.light+wc*zc*c.light)/iz;
                int rr=(int)Math.min(255,((pixel>>16)&255)*light*tr),gg=(int)Math.min(255,((pixel>>8)&255)*light*tg),bl=(int)Math.min(255,(pixel&255)*light*tb);
                depth[at]=iz;pixels[at]=0xff000000|(rr<<16)|(gg<<8)|bl;
            }
        }
    }
}
