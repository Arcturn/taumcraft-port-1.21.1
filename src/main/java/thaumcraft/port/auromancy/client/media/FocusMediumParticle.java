package thaumcraft.port.auromancy.client.media;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.client.SalisMundusParticle;

/** Original 64-grid FXFireMote and 16-grid focus cloud presets on the original atlas. */
public final class FocusMediumParticle extends TextureSheetParticle {
    private final boolean cloud,additive;
    private final int grid,frame;
    private final float startSize,endSize,spin;
    private final double windX,windZ;
    public FocusMediumParticle(ClientLevel level,Vec3 pos,Vec3 velocity,int color,boolean cloud) {
        super(level,pos.x,pos.y,pos.z);this.cloud=cloud;xd=velocity.x;yd=velocity.y;zd=velocity.z;
        setSize(.1f,.1f);setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f);hasPhysics=true;
        if(cloud){lifetime=20+random.nextInt(10);grid=16;frame=56+random.nextInt(4);startSize=(5+random.nextFloat())*.1f;endSize=(10+random.nextFloat())*.1f;spin=(random.nextBoolean()?-.25f:.25f)*Mth.DEG_TO_RAD*Mth.TWO_PI;additive=true;roll=random.nextFloat()*(float)Math.PI*2;}
        else{lifetime=16;grid=64;frame=7;additive=random.nextBoolean();startSize=additive?.7f:.7f/3;endSize=0;spin=1;roll=(float)Math.PI*2;}
        float windAngle=cloud?level.getMoonPhase()*(40+random.nextInt(10))*Mth.DEG_TO_RAD:0;windX=cloud?Mth.cos(windAngle)*.0001:0;windZ=cloud?-Mth.sin(windAngle)*.0001:0;
        oRoll=roll;quadSize=startSize;alpha=cloud?0:.5f;
    }
    @Override public void tick() {
        xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}
        move(xd,yd,zd);xd*=cloud?.99:.98;yd*=cloud?.99:.98;zd*=cloud?.99:.98;
        if(!cloud && random.nextInt(6)==0)age++;
        float progress=Mth.clamp(age/(float)lifetime,0,1);quadSize=Mth.lerp(progress,startSize,endSize);alpha=cloud?(progress<.5f?progress*1.32f:(1-progress)*1.32f):.5f*(1-progress);
        oRoll=roll;roll+=spin;
        xd+=windX;zd+=windZ;if(onGround){xd*=.7;zd*=.7;}
    }
    @Override protected float getU0(){return frame%grid/(float)grid;}
    @Override protected float getU1(){return (frame%grid+1)/(float)grid;}
    @Override protected float getV0(){return frame/grid/(float)grid;}
    @Override protected float getV1(){return (frame/grid+1)/(float)grid;}
    @Override protected int getLightColor(float partial){return cloud?super.getLightColor(partial):15728880;}
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(additive);}
}
