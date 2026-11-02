package thaumcraft.port.auromancy.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.client.SalisMundusParticle;

/** Focal manipulator's BETA26 FXGeneric and FXVisSparkle presets. */
abstract class FocalParticle extends TextureSheetParticle {
    FocalParticle(ClientLevel level,Vec3 point){super(level,point.x,point.y,point.z);hasPhysics=false;xd=yd=zd=0;}
    abstract int frame();
    @Override protected float getU0(){return frame()%64/64f;}
    @Override protected float getU1(){return (frame()%64+1)/64f;}
    @Override protected float getV0(){return frame()/64/64f;}
    @Override protected float getV1(){return (frame()/64+1)/64f;}
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(true);}
    static final class Craft extends FocalParticle {
        Craft(ClientLevel level,Vec3 point,float red,float green,float blue,int life,float scale){super(level,point);lifetime=life;quadSize=scale*.1f;alpha=.8f;setColor(red,green,blue);}
        @Override int frame(){return 448+Math.min(8,(int)(age/(float)lifetime*9));}
        @Override public float getQuadSize(float partial){return age==0?0:quadSize;}
        @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();}
    }
    static final class VisSparkle extends FocalParticle {
        private final Vec3 target;
        private final float sizeMod;
        private float scale;
        VisSparkle(ClientLevel level,Vec3 point,Vec3 target,int color) {
            super(level,point);this.target=target;lifetime=1000;xd=random.nextGaussian()*.01;yd=random.nextGaussian()*.01;zd=random.nextGaussian()*.01;sizeMod=45+random.nextInt(15);alpha=.5f;
            setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f);
        }
        @Override int frame(){return 512+age%16;}
        @Override protected int getLightColor(float partial){return 240;}
        @Override public float getQuadSize(float partial){return .1f*scale*(Mth.sin(age/3f)*.3f+6);}
        @Override public void tick() {
            xo=x;yo=y;zo=z;move(xd,yd,zd);xd*=.985;yd*=.985;zd*=.985;
            Vec3 difference=target.subtract(x,y,z);double distance=difference.length();
            if(distance<2)scale*=.95f;
            if(distance<.2){remove();return;}
            if(age<10)scale=age/sizeMod;
            xd=Mth.clamp(xd+difference.x/distance*.1,-.1,.1);yd=Mth.clamp(yd+difference.y/distance*.1,-.1,.1);zd=Mth.clamp(zd+difference.z/distance*.1,-.1,.1);
            if(age++>=lifetime)remove();
        }
    }
}
