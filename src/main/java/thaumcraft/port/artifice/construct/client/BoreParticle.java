package thaumcraft.port.artifice.construct.client;

import java.util.ArrayList;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.port.artifice.construct.ArcaneBoreEntity;
import thaumcraft.port.client.*;

/** Source FXBoreParticles/FXBoreSparkle: homing acceleration, damping, jitter and shrinking at the mouth. */
final class BoreParticle extends TextureSheetParticle {
    private final ArcaneBoreEntity target;
    private final boolean sparkle;
    private final float fragmentU,fragmentV;
    BoreParticle(ClientLevel level,double x,double y,double z,ArcaneBoreEntity target,BlockState block,boolean sparkle){
        super(level,x,y,z);this.target=target;this.sparkle=sparkle;hasPhysics=true;setSize(.2f,.2f);
        fragmentU=random.nextFloat()*3;fragmentV=random.nextFloat()*3;
        quadSize=(sparkle?random.nextFloat()*.5f+.5f:random.nextFloat()*.3f+.4f)*.1f;
        double distance=new Vec3(x,y,z).distanceTo(sparkle?target.getEyePosition():target.position());int base=Math.max(1,(int)((float)distance*10));lifetime=base/2+random.nextInt(base);
        var source=sparkle?random:level.random;xd=(float)source.nextGaussian()*.01f;yd=(float)source.nextGaussian()*.01f;zd=(float)source.nextGaussian()*.01f;
        if(sparkle){rCol=.2f;gCol=.6f+random.nextFloat()*.3f;bCol=.2f;var camera=Minecraft.getInstance().getCameraEntity();if(camera!=null&&camera.distanceToSqr(x,y,z)>(Minecraft.useFancyGraphics()?64*64:32*32))lifetime=0;}
        else{rCol=gCol=bCol=.6f;setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(block));}
    }
    @Override public void tick(){
        xo=x;yo=y;zo=z;Vec3 destination=target.getEyePosition();
        if(age++>=lifetime||BlockPos.containing(x,y,z).equals(BlockPos.containing(destination))){remove();return;}
        move(xd,yd,zd);xd*=.985;yd*=.95;zd*=.985;
        Vec3 difference=destination.subtract(x,y,z);double distance=difference.length();if(distance<1e-8){remove();return;}
        double limit=Math.min(.25,distance/15);if(distance<2)quadSize*=.9f;Vec3 acceleration=difference.scale(limit/distance);
        double jitter=sparkle?.01:.005;xd=Mth.clamp((float)(xd+acceleration.x),-limit,limit)+random.nextGaussian()*jitter;yd=Mth.clamp((float)(yd+acceleration.y),-limit,limit)+random.nextGaussian()*jitter;zd=Mth.clamp((float)(zd+acceleration.z),-limit,limit)+random.nextGaussian()*jitter;
    }
    @Override public void move(double dx,double dy,double dz){
        var requested=new Vec3(dx,dy,dz);var obstacles=new ArrayList<VoxelShape>();level.getBlockCollisions(null,getBoundingBox().expandTowards(requested)).forEach(obstacles::add);
        var resolved=FocusParticleMotion.collide(getBoundingBox(),requested,obstacles);setBoundingBox(getBoundingBox().move(resolved));setLocationFromBoundingbox();onGround=dy!=resolved.y&&dy<0;if(dx!=resolved.x)xd=0;if(dz!=resolved.z)zd=0;
    }
    @Override public void render(VertexConsumer out,Camera camera,float partial){
        var eye=camera.getPosition();float size=quadSize*(sparkle?Mth.sin(age/3f)*.5f+1:1);
        // Debris UV orientation is opposite the source sparkle's billboard.
        SalisMundusParticle.renderQuad(out,camera.rotation(),(float)(Mth.lerp(partial,xo,x)-eye.x),(float)(Mth.lerp(partial,yo,y)-eye.y),(float)(Mth.lerp(partial,zo,z)-eye.z),size,sparkle?getU0():getU1(),sparkle?getU1():getU0(),getV0(),getV1(),rCol,gCol,bCol,1,getLightColor(partial));
    }
    @Override protected float getU0(){return sparkle?(age%4)/64f:sprite.getU(fragmentU/4);}
    @Override protected float getU1(){return sparkle?(age%4+1)/64f:sprite.getU((fragmentU+1)/4);}
    @Override protected float getV0(){return sparkle?4/64f:sprite.getV(fragmentV/4);}
    @Override protected float getV1(){return sparkle?5/64f:sprite.getV((fragmentV+1)/4);}
    @Override public int getLightColor(float partial){return sparkle?240:super.getLightColor(partial);}
    @Override public ParticleRenderType getRenderType(){return sparkle?SalisMundusParticle.legacyLayer(true):ParticleRenderType.TERRAIN_SHEET;}
}
