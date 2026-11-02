package thaumcraft.port.artifice.mirror.client;

import java.util.ArrayList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.port.client.FocusParticleMotion;
import thaumcraft.port.client.SalisMundusParticle;

/** FXDispatcher.drawWispyMotes preset, including its four alpha keys and legacy collision. */
final class MirrorMote extends TextureSheetParticle {
    private final double windX,windZ;
    MirrorMote(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,int life,float red,float blue,float gravity){
        super(level,x,y,z);setSize(.1f,.1f);hasPhysics=true;xd=vx;yd=vy;zd=vz;this.gravity=gravity;
        lifetime=(int)(life+(life/2)*level.random.nextFloat());setColor(red,0,blue);alpha=0;quadSize=.1f;
        float angle=level.getMoonPhase()*(40+level.random.nextInt(10))/180f*(float)Math.PI;
        windX=Math.cos(angle)*.0001;windZ=-Math.sin(angle)*.0001;
    }
    private int frame(){return 512+age%16;}
    @Override protected float getU0(){return frame()%64/64f;}
    @Override protected float getU1(){return (frame()%64+1)/64f;}
    @Override protected float getV0(){return frame()/64/64f;}
    @Override protected float getV1(){return (frame()/64+1)/64f;}
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(true);}
    @Override public void tick(){
        xo=x;yo=y;zo=z;if(age++>=lifetime)remove();yd-=.04*gravity;move(xd,yd,zd);
        xd*=.9800000190734863;yd*=.9800000190734863;zd*=.9800000190734863;
        xd+=level.random.nextGaussian()*.0025;yd+=level.random.nextGaussian()*0;zd+=level.random.nextGaussian()*.0025;
        xd+=windX;zd+=windZ;if(onGround){xd*=.7f;zd*=.7f;}
        float t=Math.min(1,age/(float)lifetime);alpha=t<1/3f?t*1.8f:t<2/3f?.6f:(1-t)*1.8f;quadSize=.1f*(1-t*.5f);
    }
    @Override public void move(double x,double y,double z){
        var requested=new Vec3(x,y,z);var shapes=new ArrayList<VoxelShape>();level.getBlockCollisions(null,getBoundingBox().expandTowards(requested)).forEach(shapes::add);
        var move=FocusParticleMotion.collide(getBoundingBox(),requested,shapes);setBoundingBox(getBoundingBox().move(move));setLocationFromBoundingbox();onGround=y!=move.y&&y<0;if(x!=move.x)xd=0;if(z!=move.z)zd=0;
    }
}
