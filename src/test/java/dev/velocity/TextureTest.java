package dev.velocity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public final class TextureTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static Renderer3D.UVPoint p(double x,double y,double z,double u,double v){return new Renderer3D.UVPoint(new Renderer3D.Point(x,y,z,1),u,v);}
    static void textureChecks(){
        BufferedImage colors=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
        colors.setRGB(0,0,0xffff0000);colors.setRGB(1,0,0xff00ff00);colors.setRGB(0,1,0xff0000ff);colors.setRGB(1,1,0xffffffff);
        Renderer3D.Texture tex=new Renderer3D.Texture(colors);Renderer3D r=new Renderer3D();r.begin(120,120,0,0,false,0);
        var a=p(-.4,.4,1,0,1);var b=p(.4,.4,1,1,1);var c=p(.4,-.4,1,1,0);var d=p(-.4,-.4,1,0,0);
        r.textured(tex,0xffffff,a,b,c);r.textured(tex,0xffffff,a,c,d);
        check(r.image().getRGB(35,35)==0xffff0000&&r.image().getRGB(85,35)==0xff00ff00,"texture orientation and horizontal coordinates");
        check(r.image().getRGB(35,85)==0xff0000ff&&r.image().getRGB(85,85)==0xffffffff,"texture orientation and vertical coordinates");
        colors.setRGB(0,0,0);tex=new Renderer3D.Texture(colors);r.begin(120,120,0,0,false,0);
        r.polygon(Color.MAGENTA,new Renderer3D.Point(-1,1,2,1),new Renderer3D.Point(1,1,2,1),new Renderer3D.Point(1,-1,2,1),new Renderer3D.Point(-1,-1,2,1));
        r.textured(tex,0xffffff,a,b,c);r.textured(tex,0xffffff,a,c,d);
        check(r.image().getRGB(35,35)==0xffff00ff,"transparent texels do not occlude geometry behind them");
        check(r.image().getRGB(85,35)==0xff00ff00,"opaque texels update depth");
        r.begin(120,120,0,0,false,0);r.textured(tex,0xffffff,p(-.2,.2,.1,0,1),b,c);
        int visible=0;for(int y=0;y<120;y++)for(int x=0;x<120;x++)if(r.image().getRGB(x,y)!=0)visible++;
        check(visible>100,"textured triangles clip at the near plane without disappearing");
    }
    public static void main(String[] args){textureChecks();System.out.println("PASS: texture orientation, alpha and clipping.");}
}
