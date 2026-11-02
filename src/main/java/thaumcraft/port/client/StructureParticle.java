package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;

/** The original FXVisSparkle, FXBlockRunes and drawLineSparkle presets on TC6's blurred atlas. */
abstract class StructureParticle extends TextureSheetParticle {
    StructureParticle(ClientLevel level,double x,double y,double z) { super(level,x,y,z);hasPhysics=false;xd=yd=zd=0; }
    abstract int frame();
    @Override protected float getU0(){return frame()%64/64f;}
    @Override protected float getU1(){return (frame()%64+1)/64f;}
    @Override protected float getV0(){return frame()/64/64f;}
    @Override protected float getV1(){return (frame()/64+1)/64f;}
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(true);}
    static final class VisSparkle extends StructureParticle {
        final Vec3 target; final float sizeMod; float scale;
        VisSparkle(ClientLevel level,Vec3 point,Vec3 target,int color){
            super(level,point.x,point.y,point.z);this.target=target;lifetime=1000;hasPhysics=true;
            xd=(float)random.nextGaussian()*.01f;yd=(float)random.nextGaussian()*.01f;zd=(float)random.nextGaussian()*.01f;sizeMod=45+random.nextInt(15);alpha=.5f;
            setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f);
        }
        @Override int frame(){return 512+age%16;}
        @Override protected int getLightColor(float partial){return 240;}
        @Override public float getQuadSize(float partial){return .1f*scale*(Mth.sin(age/3f)*.3f+6);}
        @Override public void move(double dx,double dy,double dz){
            var requested=new Vec3(dx,dy,dz);var obstacles=new ArrayList<VoxelShape>();
            level.getBlockCollisions(null,getBoundingBox().expandTowards(requested)).forEach(obstacles::add);
            var resolved=FocusParticleMotion.collide(getBoundingBox(),requested,obstacles);
            setBoundingBox(getBoundingBox().move(resolved));setLocationFromBoundingbox();
            onGround=dy!=resolved.y&&dy<0;
            if(dx!=resolved.x)xd=0;if(dz!=resolved.z)zd=0;
        }
        @Override public void tick(){
            xo=x;yo=y;zo=z;move(xd,yd,zd);xd*=.985;yd*=.985;zd*=.985;
            Vec3 diff=target.subtract(x,y,z);double distance=(float)diff.length();
            if(distance<2)scale*=.95f;
            if(distance<.2){remove();return;}
            if(age<10)scale=age/sizeMod;
            xd=Mth.clamp((float)(xd+diff.x/distance*.1f),-.1f,.1f);yd=Mth.clamp((float)(yd+diff.y/distance*.1f),-.1f,.1f);zd=Mth.clamp((float)(zd+diff.z/distance*.1f),-.1f,.1f);
            if(age++>=lifetime)remove();
        }
    }
    static final class InlaySpark extends StructureParticle {
        final int first;
        InlaySpark(ClientLevel level,BlockPos pos,Direction face){
            super(level,pos.getX()+.5+level.random.nextGaussian()*.08,pos.getY()+.025,pos.getZ()+.5+level.random.nextGaussian()*.08);
            first=random.nextFloat()<.2?320:512;lifetime=64+random.nextInt(16);quadSize=.066f;
            xd=face.getStepX()/70.0*(1-random.nextFloat()*.1);zd=face.getStepZ()/70.0*(1-random.nextFloat()*.1);
            float red=(150+random.nextInt(51))/255f,green=random.nextInt(201)/255f;setColor(red,green,green/2);alpha=0;
        }
        @Override int frame(){return first+age%16;}
        @Override public float getQuadSize(float partial){float progress=age/(float)lifetime;return .033f*(1+2*Math.min(progress,1-progress));}
        @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}move(xd,yd,zd);xd+=random.nextGaussian()*.00005;zd+=random.nextGaussian()*.00005;float progress=age/(float)lifetime;alpha=2*Math.min(progress,1-progress);}
    }
    static final class BlockRune extends StructureParticle {
        final int rune;final Quaternionf orientation;
        BlockRune(ClientLevel level,BlockPos pos){
            super(level,pos.getX()+.5,pos.getY()+.125,pos.getZ()+.5);lifetime=30;quadSize=.15f*(.5f+(float)random.nextGaussian()*.1f);rune=384+random.nextInt(16);setColor(1,0,0);alpha=0;
            float rotation=random.nextInt(4)*Mth.HALF_PI;
            orientation=new Quaternionf().rotationY(rotation).rotateZ(Mth.HALF_PI);
            var offset=new org.joml.Vector3f(0,-.3f+random.nextFloat()*.6f,-.51f).rotate(orientation);
            x+=offset.x;y+=offset.y;z+=offset.z;xo=x;yo=y;zo=z;
        }
        @Override int frame(){return rune;}
        @Override public void tick(){if(age++>=lifetime)remove();float threshold=lifetime/5f;alpha=(age<=threshold?age/threshold:(lifetime-age)/(float)lifetime)/2;}
        @Override public void render(VertexConsumer buffer,Camera camera,float partial){var origin=camera.getPosition();SalisMundusParticle.renderQuad(buffer,orientation,(float)(x-origin.x),(float)(y-origin.y),(float)(z-origin.z),quadSize,getU0(),getU1(),getV0(),getV1(),rCol,gCol,bCol,alpha,240);}
    }
}
