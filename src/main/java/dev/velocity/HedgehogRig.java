package dev.velocity;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Original procedural fan model with a hierarchy of animated joints and smooth meshes. */
final class HedgehogRig {
    private final Color BLUE,BLUE_DARK,TAN,RED,GREEN;
    private static final Color WHITE=new Color(244,247,255),BLACK=new Color(18,25,39);
    private final KartDriver driver;private final int quality;private final boolean kartMode;
    private record Vertex(double x,double y,double z,double nx,double ny,double nz) {}
    private record Mesh(Vertex[] vertices,int[] triangles,Color color) {}
    private static final class Joint {
        double x,y,z,rx,ry,rz;
        boolean visible=true;
        final List<Mesh> meshes=new ArrayList<>();
        final List<Joint> children=new ArrayList<>();
        Joint(double x,double y,double z) { this.x=x;this.y=y;this.z=z; }
        Joint child(double x,double y,double z) { Joint j=new Joint(x,y,z);children.add(j);return j; }
    }
    private final Joint root=new Joint(0,0,0), head=root.child(0,2.48,0);
    private final Joint vehicle=new Joint(0,0,0);
    private final Joint[] wheels=new Joint[2],carWheels=new Joint[4];
    private final Joint car=new Joint(0,0,0),board=new Joint(0,0,0);
    private final Joint[] arms=new Joint[2], elbows=new Joint[2], legs=new Joint[2], knees=new Joint[2];
    HedgehogRig(){this(KartDriver.SONIC,0,false);}
    HedgehogRig(KartDriver driver,int quality){this(driver,quality,true);}
    private HedgehogRig(KartDriver driver,int quality,boolean kartMode) {
        this.driver=driver;this.quality=quality;this.kartMode=kartMode;
        BLUE=kartMode?driver.body:new Color(24,77,219);BLUE_DARK=kartMode?BLUE.darker():new Color(20,59,184);
        TAN=kartMode&&driver==KartDriver.TAILS?WHITE:new Color(245,192,140);
        RED=new Color(223,36,57);GREEN=kartMode&&driver==KartDriver.TAILS?new Color(63,150,214):kartMode&&driver==KartDriver.KNUCKLES?new Color(157,93,202):new Color(47,174,92);

        ellipsoid(vehicle,0,.90,0,.31,.25,.92,new Color(26,180,217));
        ellipsoid(vehicle,0,1.14,-.38,.29,.10,.48,BLACK);
        ellipsoid(vehicle,0,1.10,.48,.33,.34,.42,BLUE);
        ellipsoid(vehicle,0,1.27,.76,.20,.13,.045,WHITE);
        for(int i=0;i<2;i++) {
            wheels[i]=vehicle.child(0,.49,i==0?-1.02:1.02);
            ellipsoid(wheels[i],0,0,0,.18,.49,.49,BLACK);
            ellipsoid(wheels[i],0,0,0,.19,.25,.25,new Color(124,148,172));
            for(int k=0;k<6;k++) {
                double a=k*Math.PI/3;
                tube(wheels[i],new double[]{.20,0,0},new double[]{.20,.34*Math.cos(a),.34*Math.sin(a)},.028,WHITE);
                tube(wheels[i],new double[]{-.20,0,0},new double[]{-.20,.34*Math.cos(a),.34*Math.sin(a)},.028,WHITE);
            }
        }
        for(int side:new int[]{-1,1}) {
            tube(vehicle,new double[]{side*.15,.49,1.02},new double[]{side*.15,1.4,.64},.055,new Color(162,182,197));
            tube(vehicle,new double[]{side*.12,.49,-1.02},new double[]{side*.14,.95,0},.06,BLUE);
        }
        tube(vehicle,new double[]{-.48,1.42,.55},new double[]{.48,1.42,.55},.05,BLACK);
        ellipsoid(car,0,.55,0,1.04,.35,1.72,(kartMode?driver.livery:new Color(236,80,43)));
        ellipsoid(car,0,.78,.85,.88,.22,.75,(kartMode?driver.livery.brighter():new Color(255,151,49)));
        ellipsoid(car,0,.83,-.75,.87,.24,.65,(kartMode?driver.livery.darker():new Color(210,43,52)));
        ellipsoid(car,0,.94,-.24,.55,.08,.48,BLACK);
        ellipsoid(car,0,1.13,.48,.82,.32,.08,new Color(107,205,232));
        for(int side:new int[]{-1,1}){
            ellipsoid(car,side*.69,.75,1.51,.19,.10,.075,WHITE);
            ellipsoid(car,side*.70,.65,-1.55,.20,.085,.055,RED);
            for(int end=0;end<2;end++){
                Joint wheel=car.child(side*.95,.44,end==0?-1.04:1.04);carWheels[(side+1)+end]=wheel;
                ellipsoid(wheel,0,0,0,.23,.44,.44,BLACK);
                ellipsoid(wheel,side*.23,0,0,.025,.26,.26,new Color(190,204,217));
                tube(wheel,new double[]{side*.26,-.23,0},new double[]{side*.26,.23,0},.035,BLACK);
            }
        }
        ellipsoid(board,0,.16,0,.57,.13,1.56,new Color(255,178,63));
        ellipsoid(board,0,.286,0,.12,.018,1.36,new Color(245,70,104));
        spike(board,0,.06,-.75,0,-.25,-1.3,.15,new Color(15,150,209));
        ellipsoid(root,0,1.65,0,.43,.58,.35,kartMode&&driver==KartDriver.AMY?RED:BLUE);
        ellipsoid(root,0,1.68,.31,.275,.405,.085,TAN);
        if(kartMode&&driver==KartDriver.TAILS){
            for(int side:new int[]{-1,1}){
                spike(root,side*.17,1.32,-.2,side*.77,1.72,-1.3,.34,BLUE);
                spike(root,side*.62,1.65,-1.08,side*.91,1.98,-1.72,.23,WHITE);
            }
        }else spike(root,0,1.37,-.25,0,1.2,-.78,.15,BLUE);
        if(kartMode&&driver==KartDriver.KNUCKLES){
            tube(root,new double[]{-.28,1.94,.34},new double[]{0,1.76,.42},.065,WHITE);
            tube(root,new double[]{0,1.76,.42},new double[]{.28,1.94,.34},.065,WHITE);
        }
        ellipsoid(head,0,.04,0,.69,.67,.62,BLUE);
        if(!kartMode||driver==KartDriver.SONIC){
        // Six swept quills, sculpted as curved tapering tubes instead of flat triangles.
        spike(head,0,.43,-.27,0,-.12,-1.4,.36,BLUE);
        for(int side:new int[]{-1,1}) {
            spike(head,side*.43,.24,-.28,side*1.02,-.37,-1.09,.34,BLUE);
            spike(head,side*.40,-.16,-.28,side*.77,-.99,-1.02,.32,BLUE);
        }
        spike(head,0,-.32,-.38,0,-1.05,-1.23,.31,BLUE_DARK);
        }else if(driver==KartDriver.KNUCKLES){
            for(int side:new int[]{-1,1})for(int i=0;i<3;i++)
                spike(head,side*(.39+i*.08),.25,-.08-i*.24,side*(.68+i*.10),-.92-i*.05,-.24-i*.27,.23,BLUE);
            spike(head,0,.38,-.25,0,-.68,-.9,.34,BLUE_DARK);
        }else if(driver==KartDriver.AMY){
            for(int side:new int[]{-1,1}){
                spike(head,side*.42,.08,-.2,side*.75,-.64,-.46,.31,BLUE);
                spike(head,side*.24,-.15,-.35,side*.42,-.75,-.64,.27,BLUE);
            }
            for(int i=0;i<12;i++){
                double a=Math.PI*i/12,b=Math.PI*(i+1)/12;
                tube(head,new double[]{Math.cos(a)*.68,Math.sin(a)*.66+.05,-.015},new double[]{Math.cos(b)*.68,Math.sin(b)*.66+.05,-.015},.065,RED);
            }
        }else{
            for(int side:new int[]{-1,1}){
                spike(head,side*.30,-.26,.4,side*.70,-.25,.52,.24,WHITE);
                spike(head,side*.25,-.30,.43,side*.64,-.48,.5,.19,WHITE);
            }
        }
        for(int side:new int[]{-1,1}) {
            spike(head,side*.43,.48,.035,side*.52,kartMode&&driver==KartDriver.TAILS?1.30:1.02,.055,.235,BLUE);
            // The inset is in front of the blue outer ear.
            earInset(head,side);
            eye(head,side);
            if(kartMode&&driver==KartDriver.AMY)for(int lash=0;lash<2;lash++)tube(head,new double[]{side*.43,.22+lash*.08,.64},new double[]{side*.59,.30+lash*.09,.66},.018,BLACK);
            ellipsoid(head,side*.205,.085,.687,.091,.203,.034,GREEN);
            ellipsoid(head,side*.205,.085,.717,.042,.160,.014,BLACK);
            ellipsoid(head,side*.189,.175,.732,.022,.050,.008,WHITE);
        }
        if(!kartMode||driver!=KartDriver.TAILS)spike(head,0,.48,.52,0,.235,.693,.115,BLUE);
        ellipsoid(head,0,-.222,.599,.493,.246,.222,TAN);
        ellipsoid(head,0,-.119,.845,.132,.102,.135,BLACK);
        ellipsoid(head,-.036,-.085,.944,.036,.019,.023,WHITE);
        // Smirk at one corner of the muzzle.
        tube(head,new double[]{.18,-.295,.808},new double[]{.33,-.255,.758},.015,BLACK);
        for(int i=0;i<2;i++) {
            int side=i==0?-1:1;
            legs[i]=root.child(side*.20,1.21,0);
            ellipsoid(legs[i],0,-.265,0,.105,.29,.105,BLUE);
            knees[i]=legs[i].child(0,-.51,0);
            ellipsoid(knees[i],0,-.215,0,.09,.24,.095,BLUE);
            Joint foot=knees[i].child(0,-.44,.03);
            ellipsoid(foot,0,.06,0,.19,.125,.20,WHITE);
            ellipsoid(foot,0,-.10,.19,.265,.17,.48,RED);
            ellipsoid(foot,0,-.225,.19,.276,.065,.486,WHITE);
            ellipsoid(foot,0,-.05,.21,.277,.215,.13,WHITE);
            ellipsoid(foot,side*.245,-.02,.15,.032,.093,.10,new Color(247,196,59));
            arms[i]=root.child(side*.40,1.99,.015);
            ellipsoid(arms[i],0,-.24,0,.10,.28,.10,TAN);
            elbows[i]=arms[i].child(0,-.46,0);
            ellipsoid(elbows[i],0,-.19,0,.09,.23,.09,TAN);
            Joint glove=elbows[i].child(0,-.40,0);
            ellipsoid(glove,0,.04,0,.17,.12,.16,WHITE);
            ellipsoid(glove,0,-.16,.025,.186,.22,.17,WHITE);
            ellipsoid(glove,-side*.17,-.13,.08,.085,.13,.095,WHITE);
            if(kartMode&&driver==KartDriver.KNUCKLES)for(int k=-1;k<=1;k+=2)spike(glove,k*.095,-.12,.13,k*.11,-.12,.34,.07,WHITE);
            for(int finger=0;finger<3;finger++) ellipsoid(glove,(finger-1)*.09,-.27,.064,.058,.103,.116,WHITE);
        }
    }
    void drawVehicle(Renderer3D renderer,Vehicle kind,double x,double z,double lift,double rotation,double wheelAngle,double lean){
        Joint model=switch(kind){case BIKE->vehicle;case CAR->car;case SURFBOARD->board;default->null;};
        if(model==null)return;
        model.x=x;model.y=lift;model.z=z;model.ry=rotation;model.rz=lean;
        for(Joint wheel:wheels)wheel.rx=wheelAngle;
        for(Joint wheel:carWheels)wheel.rx=wheelAngle;
        drawJoint(renderer,model,identity());
    }
    void drawKart(Renderer3D renderer,double x,double z,double steering,double wheelsAngle,double speed){
        double lean=-steering*.09;
        for(Joint wheel:carWheels)wheel.ry=wheel.z>0?steering*.28:0;
        drawVehicle(renderer,Vehicle.CAR,x,z,0,0,wheelsAngle,lean);
        root.x=x;root.y=.04;root.z=z-.25;root.rx=.23;root.ry=0;root.rz=lean;
        head.y=2.24;head.rx=-.20;head.ry=steering*.10;
        for(int i=0;i<2;i++){arms[i].rz=(i==0?-1:1)*.18;arms[i].rx=-.95;elbows[i].rx=-.35;legs[i].visible=false;}
        drawJoint(renderer,root,identity());
    }
    void draw(Renderer3D renderer,Game game,boolean preview){
        boolean moving=game.state==Game.State.RUNNING&&!preview;
        double lean=moving?Math.max(-.30,Math.min(.30,-game.steering*.15+game.track.curve(game.z)*12)):0;
        double lift=preview?0:game.y+(game.y==0?game.track.ramp(game.z):0);
        drawVehicle(renderer,game.vehicle,game.x,game.z,lift,0,game.wheelAngle,lean);
        root.x=game.x;root.y=lift+.04;root.z=game.z;root.rx=0;root.ry=0;root.rz=lean;
        head.y=2.48;head.rx=0;head.ry=preview?-.12:0;
        for(int i=0;i<2;i++){arms[i].rz=(i==0?-1:1)*.18;legs[i].visible=game.vehicle!=Vehicle.CAR;}
        if(game.vehicle==Vehicle.RUNNING){
            double stride=moving?Math.sin(game.elapsed*(game.boosting?30:24)):0;
            root.rx=moving?(game.boosting?.24:.14):0;head.rx=-root.rx*.6;
            root.y+=moving?Math.abs(stride)*.055:0;
            for(int i=0;i<2;i++){
                int side=i==0?-1:1;arms[i].rx=-side*stride*.95;elbows[i].rx=-.55-Math.max(0,side*stride)*.5;
                legs[i].rx=side*stride*.95;knees[i].rx=-.10-Math.max(0,-side*stride)*1.1;
            }
            if(game.y>0&&!preview){
                root.rx=game.elapsed*19;root.y+=1.45-Math.cos(root.rx)*1.45;root.z-=Math.sin(root.rx)*1.45;head.y=2.05;head.rx=.30;
                for(int i=0;i<2;i++){legs[i].rx=1.4;knees[i].rx=-1.9;arms[i].rx=1.4;elbows[i].rx=-1.7;}
            }
        }else if(game.vehicle==Vehicle.SURFBOARD){
            root.y+=.23;root.ry=.58;head.ry=-.58;root.rx=.05;
            for(int i=0;i<2;i++){arms[i].rx=-.35;arms[i].rz=(i==0?-1:1)*1.05;elbows[i].rx=-.15;legs[i].rx=(i==0?-.30:.30);knees[i].rx=-.28;}
        }else{
            root.rx=.23;root.z-=.25;head.y=2.24;head.rx=-.20;
            for(int i=0;i<2;i++){arms[i].rx=-.95;elbows[i].rx=-.35;legs[i].rx=game.vehicle==Vehicle.CAR?-1.8:-1.25;knees[i].rx=game.vehicle==Vehicle.CAR?1.2:1.75;}
        }
        drawJoint(renderer,root,identity());
    }
    private static void drawJoint(Renderer3D renderer,Joint joint,double[] parent) {
        if(!joint.visible)return;
        double[] matrix=multiply(parent,transform(joint));
        for(Mesh mesh:joint.meshes) {
            Renderer3D.Point[] points=new Renderer3D.Point[mesh.vertices.length];
            for(int i=0;i<points.length;i++) {
                Vertex v=mesh.vertices[i];
                double nx=matrix[0]*v.nx+matrix[1]*v.ny+matrix[2]*v.nz;
                double ny=matrix[4]*v.nx+matrix[5]*v.ny+matrix[6]*v.nz;
                double nz=matrix[8]*v.nx+matrix[9]*v.ny+matrix[10]*v.nz;
                double diffuse=Math.max(0,-.44*nx+.78*ny+.44*nz);
                double light=.57+.48*diffuse;
                points[i]=renderer.camera(matrix[0]*v.x+matrix[1]*v.y+matrix[2]*v.z+matrix[3],
                    matrix[4]*v.x+matrix[5]*v.y+matrix[6]*v.z+matrix[7],
                    matrix[8]*v.x+matrix[9]*v.y+matrix[10]*v.z+matrix[11],light);
            }
            for(int i=0;i<mesh.triangles.length;i+=3)
                renderer.polygon(mesh.color,points[mesh.triangles[i]],points[mesh.triangles[i+1]],points[mesh.triangles[i+2]]);
        }
        for(Joint child:joint.children)drawJoint(renderer,child,matrix);
    }
    private static double[] identity() { return new double[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1}; }
    private static double[] multiply(double[] a,double[] b) {
        double[] c=new double[16];
        for(int r=0;r<4;r++)for(int col=0;col<4;col++)for(int k=0;k<4;k++)c[r*4+col]+=a[r*4+k]*b[k*4+col];
        return c;
    }
    private static double[] transform(Joint j) {
        double cx=Math.cos(j.rx),sx=Math.sin(j.rx),cy=Math.cos(j.ry),sy=Math.sin(j.ry),cz=Math.cos(j.rz),sz=Math.sin(j.rz);
        double[] m=multiply(new double[]{cz,-sz,0,0,sz,cz,0,0,0,0,1,0,0,0,0,1},
            multiply(new double[]{cy,0,sy,0,0,1,0,0,-sy,0,cy,0,0,0,0,1},new double[]{1,0,0,0,0,cx,-sx,0,0,sx,cx,0,0,0,0,1}));
        m[3]=j.x;m[7]=j.y;m[11]=j.z;return m;
    }
    private void ellipsoid(Joint joint,double x,double y,double z,double rx,double ry,double rz,Color color) {
        int rows=quality==0?12:quality==1?8:6,cols=quality==0?20:quality==1?12:10;Vertex[] vertices=new Vertex[(rows+1)*(cols+1)];
        for(int i=0;i<=rows;i++)for(int k=0;k<=cols;k++) {
            double a=Math.PI*i/rows,b=2*Math.PI*k/cols;
            double xx=Math.sin(a)*Math.cos(b),yy=Math.cos(a),zz=Math.sin(a)*Math.sin(b);
            double nx=xx/rx,ny=yy/ry,nz=zz/rz,length=Math.sqrt(nx*nx+ny*ny+nz*nz);
            vertices[i*(cols+1)+k]=new Vertex(x+xx*rx,y+yy*ry,z+zz*rz,nx/length,ny/length,nz/length);
        }
        joint.meshes.add(new Mesh(vertices,grid(rows,cols),color));
    }
    private void eye(Joint joint,int side) {
        ellipsoid(joint,side*.255,.10,.545,.285,.395,.145,WHITE);
        Mesh mesh=joint.meshes.remove(joint.meshes.size()-1);
        // Blue upper eyelid shares the eye surface; no disconnected eyebrow geometry.
        int rows=quality==0?12:quality==1?8:6,cols=quality==0?20:quality==1?12:10;int boundary=(rows/4)*cols*6;
        joint.meshes.add(new Mesh(mesh.vertices,java.util.Arrays.copyOfRange(mesh.triangles,0,boundary),BLUE));
        joint.meshes.add(new Mesh(mesh.vertices,java.util.Arrays.copyOfRange(mesh.triangles,boundary,mesh.triangles.length),WHITE));
    }
    private static int[] grid(int rows,int cols) {
        int[] indices=new int[rows*cols*6];int n=0;
        for(int i=0;i<rows;i++)for(int k=0;k<cols;k++) {
            int a=i*(cols+1)+k,b=a+cols+1;
            indices[n++]=a;indices[n++]=b;indices[n++]=a+1;
            indices[n++]=a+1;indices[n++]=b;indices[n++]=b+1;
        }
        return indices;
    }
    private void spike(Joint joint,double x,double y,double z,double tx,double ty,double tz,double radius,Color color) {
        int rows=quality==0?9:quality==1?6:4,cols=quality==0?16:quality==1?12:8;Vertex[] vertices=new Vertex[(rows+1)*(cols+1)];
        double dx=tx-x,dy=ty-y,dz=tz-z,length=Math.sqrt(dx*dx+dy*dy+dz*dz);
        double ux=dx/length,uy=dy/length,uz=dz/length;
        double ax=uz,ay=0,az=-ux,al=Math.sqrt(ax*ax+az*az);
        if(al<.001){ax=1;az=0;al=1;} ax/=al;az/=al;
        double bx=uy*az,by=uz*ax-ux*az,bz=-uy*ax;
        for(int i=0;i<=rows;i++)for(int k=0;k<=cols;k++) {
            double t=(double)i/rows,a=k*Math.PI*2/cols,r=radius*Math.pow(1-t,.85);
            double nx=ax*Math.cos(a)+bx*Math.sin(a),ny=ay*Math.cos(a)+by*Math.sin(a),nz=az*Math.cos(a)+bz*Math.sin(a);
            // A raised middle section creates a swept rather than straight conical silhouette.
            double bend=Math.sin(t*Math.PI)*radius*.48;
            vertices[i*(cols+1)+k]=new Vertex(x+dx*t+nx*r,y+dy*t+ny*r+bend,z+dz*t+nz*r,nx,ny,nz);
        }
        joint.meshes.add(new Mesh(vertices,grid(rows,cols),color));
    }
    private void earInset(Joint joint,int side) {
        Vertex[] v={new Vertex(side*.31,.50,.284,0,0,1),new Vertex(side*.55,.50,.284,0,0,1),new Vertex(side*.51,.86,.17,0,0,1)};
        joint.meshes.add(new Mesh(v,new int[]{0,1,2},TAN));
    }
    private void tube(Joint joint,double[] a,double[] b,double radius,Color color) {
        int cols=quality==2?6:10;Vertex[] vertices=new Vertex[2*(cols+1)];
        double dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2],length=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(length<.00001)return;dx/=length;dy/=length;dz/=length;
        double ax=dz,ay=0,az=-dx,al=Math.hypot(ax,az);if(al<.001){ax=1;az=0;al=1;}ax/=al;az/=al;
        double bx=dy*az,by=dz*ax-dx*az,bz=-dy*ax;
        for(int row=0;row<2;row++)for(int k=0;k<=cols;k++){
            double t=k*Math.PI*2/cols,nx=ax*Math.cos(t)+bx*Math.sin(t),ny=ay*Math.cos(t)+by*Math.sin(t),nz=az*Math.cos(t)+bz*Math.sin(t);
            double[] center=row==0?a:b;vertices[row*(cols+1)+k]=new Vertex(center[0]+radius*nx,center[1]+radius*ny,center[2]+radius*nz,nx,ny,nz);
        }
        joint.meshes.add(new Mesh(vertices,grid(1,cols),color));
    }
}
