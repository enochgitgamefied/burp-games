package dev.velocity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Image assertions exercise depth ordering and clipping independently of the model. */
public final class RendererTest {
    private static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Renderer3D renderer=new Renderer3D();
        renderer.begin(600,400,0,0,false,0);
        Renderer3D.Point[] near={new Renderer3D.Point(-1,-1,4,1),new Renderer3D.Point(1,-1,4,1),new Renderer3D.Point(0,1,4,1)};
        Renderer3D.Point[] far={new Renderer3D.Point(-1,-1,7,1),new Renderer3D.Point(1,-1,7,1),new Renderer3D.Point(0,1,7,1)};
        renderer.polygon(Color.RED,near);renderer.polygon(Color.BLUE,far);
        int first=renderer.image().getRGB(300,192);
        check((first&0xffffff)==0xff0000,"near face must hide far face");
        renderer.begin(600,400,0,0,false,0);
        renderer.polygon(Color.BLUE,far);renderer.polygon(Color.RED,near);
        check(first==renderer.image().getRGB(300,192),"depth test must be independent of submission order");
        renderer.begin(600,400,0,0,false,0);
        renderer.polygon(Color.GREEN,new Renderer3D.Point(-1,-1,-1,1),new Renderer3D.Point(1,-1,4,1),new Renderer3D.Point(0,1,4,1));
        int lit=0; for(int y=0;y<400;y++)for(int x=0;x<600;x++)if(renderer.image().getRGB(x,y)!=0)lit++;
        check(lit>0,"near-plane crossing should clip and remain visible");
        renderer.begin(600,400,0,0,false,0);
        renderer.polygon(Color.RED,new Renderer3D.Point(-1,-1,-2,1),new Renderer3D.Point(1,-1,-2,1),new Renderer3D.Point(0,1,-2,1));
        check(renderer.image().getRGB(300,192)==0,"fully behind-camera triangle must not draw");
        SwingUtilities.invokeAndWait(()->{
            try {
                GamePanel panel=new GamePanel(new Progress(false));panel.setSize(1200,800);
                panel.game.togglePause();panel.game.z=145;panel.game.elapsed=5.8;
                BufferedImage canvas=new BufferedImage(1200,800,BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics=canvas.createGraphics();
                for(int i=0;i<8;i++)panel.paint(graphics);
                long started=System.nanoTime();
                for(int i=0;i<30;i++){panel.game.elapsed+=.016;panel.paint(graphics);}
                double frameMs=(System.nanoTime()-started)/1e6/30;
                System.out.printf("Renderer benchmark: %.1f ms/frame at 1200x800 (1100px render buffer)%n",frameMs);
                if(args.length>0) {
                    panel.game.y=1.5;panel.game.elapsed=6.45;panel.paint(graphics);
                    ImageIO.write(canvas,"png",new File(args[0],"jump.png"));
                    panel.game.y=0;panel.game.boosting=true;panel.paint(graphics);
                    ImageIO.write(canvas,"png",new File(args[0],"boost.png"));
                    panel.game.togglePause();panel.inspectCharacter=true;panel.previewAngle=Math.toRadians(90);panel.paint(graphics);
                    ImageIO.write(canvas,"png",new File(args[0],"character-side.png"));
                }
                graphics.dispose();panel.dispose();
            } catch(Exception e){throw new RuntimeException(e);}
        });
        System.out.println("PASS: depth ordering, near-plane clipping, animated rig rendering.");
    }
}
