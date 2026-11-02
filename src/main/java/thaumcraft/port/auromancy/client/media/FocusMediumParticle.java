package thaumcraft.port.auromancy.client.media;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.port.client.FocusParticleMotion;
import thaumcraft.port.client.SalisMundusParticle;
import org.joml.Quaternionf;

/** Original 64-grid FXFireMote and 16-grid focus cloud presets on the original atlas. */
public final class FocusMediumParticle extends TextureSheetParticle {
    private final boolean cloud,additive;
    private final int grid,frame;
    private final float startSize,endSize,spin;
    private final double windX,windZ;
    public FocusMediumParticle(ClientLevel level,Vec3 pos,Vec3 velocity,int color,boolean cloud) {
        super(level,pos.x,pos.y,pos.z);this.cloud=cloud;xd=velocity.x;yd=velocity.y;zd=velocity.z;
        setSize(.1f,.1f);setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f);hasPhysics=true;
        var r=level.random;
        if(cloud){
            lifetime=20+r.nextInt(10);grid=16;frame=56+r.nextInt(4);
            startSize=(5+r.nextFloat())*.1f;endSize=(10+r.nextFloat())*.1f;additive=true;
            float windAngle=level.getMoonPhase()*(40+r.nextInt(10))*Mth.DEG_TO_RAD;
            windX=Mth.cos(windAngle)*.0001;windZ=-Mth.sin(windAngle)*.0001;
            roll=(float)(r.nextFloat()*Math.PI*2);spin=(r.nextBoolean()?-.25f:.25f)*Mth.DEG_TO_RAD*Mth.TWO_PI;
        }else{
            lifetime=16;grid=64;frame=7;additive=!r.nextBoolean();
            startSize=additive?.7f:.7f/3;endSize=0;spin=1;roll=(float)Math.PI*2;windX=windZ=0;
        }
        // FXFireMote leaves prevParticleAngle at zero until its first tick.
        quadSize=startSize;alpha=cloud?0:.5f;
    }
    @Override public void tick() {
        xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}
        move(xd,yd,zd);xd*=cloud?.99:.98;yd*=cloud?.99:.98;zd*=cloud?.99:.98;
        if(!cloud && level.random.nextInt(6)==0)age++;
        if(!cloud && age>=lifetime)remove();
        float progress=Mth.clamp(age/(float)lifetime,0,1);quadSize=Mth.lerp(progress,startSize,endSize);alpha=cloud?(progress<.5f?progress*1.32f:(1-progress)*1.32f):.5f*(1-progress);
        oRoll=roll;roll+=spin;
        xd+=windX;zd+=windZ;if(onGround){xd*=.7;zd*=.7;}
    }
    @Override public void render(VertexConsumer buffer,Camera camera,float partial) {
        var cameraPos=camera.getPosition();
        var rotation=new Quaternionf(camera.rotation()).rotateZ(roll+(roll-oRoll)*partial);
        SalisMundusParticle.renderQuad(buffer,rotation,
                (float)(Mth.lerp(partial,xo,x)-cameraPos.x),
                (float)(Mth.lerp(partial,yo,y)-cameraPos.y),
                (float)(Mth.lerp(partial,zo,z)-cameraPos.z),
                quadSize,getU0(),getU1(),getV0(),getV1(),rCol,gCol,bCol,alpha,getLightColor(partial));
    }
    @Override public void move(double dx,double dy,double dz){
        var requested=new Vec3(dx,dy,dz);var resolved=requested;
        if(hasPhysics){
            var obstacles=new ArrayList<VoxelShape>();
            level.getBlockCollisions(null,getBoundingBox().expandTowards(requested)).forEach(obstacles::add);
            resolved=FocusParticleMotion.collide(getBoundingBox(),requested,obstacles);
        }
        setBoundingBox(getBoundingBox().move(resolved));setLocationFromBoundingbox();
        onGround=dy!=resolved.y&&dy<0;
        if(dx!=resolved.x)xd=0;
        if(dz!=resolved.z)zd=0;
    }
    @Override protected float getU0(){return frame%grid/(float)grid;}
    @Override protected float getU1(){return (frame%grid+1)/(float)grid;}
    @Override protected float getV0(){return frame/grid/(float)grid;}
    @Override protected float getV1(){return (frame/grid+1)/(float)grid;}
    @Override protected int getLightColor(float partial){return cloud?super.getLightColor(partial):255;}
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(additive);}
}
