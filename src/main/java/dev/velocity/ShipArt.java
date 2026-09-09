package dev.velocity;

import java.awt.*;
import java.awt.geom.Path2D;

/** Layered original spacecraft artwork, kept sharp at any display size. */
final class ShipArt {
    private static void polygon(Graphics2D g,Paint fill,double... xy){
        Path2D p=new Path2D.Double();p.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)p.lineTo(xy[i],xy[i+1]);p.closePath();g.setPaint(fill);g.fill(p);g.setColor(new Color(9,18,35));g.setStroke(new BasicStroke(1.7f));g.draw(p);
    }
    static void draw(Graphics2D graphics,double x,double y,double scale,boolean enemy,double time,boolean thrust){
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.scale(scale,scale);if(enemy)g.rotate(Math.PI);
        Color accent=enemy?new Color(255,95,116):new Color(62,220,255);
        Color metal=enemy?new Color(172,80,117):new Color(181,207,228);
        Color dark=enemy?new Color(86,41,76):new Color(42,69,100);
        for(int side:new int[]{-1,1}){
            float glow=thrust?24:12;double pulse=glow+Math.sin(time*24)*4;
            g.setPaint(new RadialGradientPaint(new java.awt.geom.Point2D.Double(side*28,54),40,new float[]{0,1},new Color[]{new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),110),new Color(0,0,0,0)}));g.fillOval(side*28-40,14,80,95);
            polygon(g,accent,side*21,48,side*28,52+pulse,side*35,48);
            polygon(g,new Color(218,248,255),side*25,49,side*28,60+pulse*.35,side*31,49);
            polygon(g,new GradientPaint(side*15,0,metal,side*52,45,dark),side*13,-16,side*52,20,side*59,41,side*29,35,side*19,46);
            polygon(g,dark,side*21,-3,side*35,6,side*38,47,side*20,49);
            g.setColor(metal);g.fillRoundRect(side*28-5,10,10,27,3,3);g.setColor(accent);g.fillRect(side*28-3,15,6,13);
            g.setColor(new Color(32,42,64));g.fillRoundRect(side*49-3,-4,6,37,2,2);g.setColor(accent);g.fillRect(side*49-2,-7,4,8);
            g.setColor(new Color(230,242,250,140));g.drawLine(side*17,-9,side*48,24);
        }
        polygon(g,new GradientPaint(-16,0,dark,13,0,metal),0,-65,-12,-30,-19,7,-16,43,0,51,16,43,19,7,12,-30);
        polygon(g,new GradientPaint(-8,0,new Color(227,245,255),10,0,metal),0,-55,-5,-25,-4,30,0,40,4,30,5,-25);
        polygon(g,new GradientPaint(-7,-20,new Color(137,241,255),9,12,new Color(12,76,129)),0,-28,-9,-12,-7,11,0,18,7,11,9,-12);
        g.setColor(new Color(226,255,255));g.drawLine(-4,-17,-4,5);
        g.setColor(accent);g.fillRect(-2,-45,4,10);
        for(int i=0;i<3;i++){g.setColor(new Color(10,22,39));g.fillRect(-12,22+i*5,6,2);g.fillRect(6,22+i*5,6,2);}
        g.dispose();
    }
}
