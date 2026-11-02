package thaumcraft.port.golem.client;

import thaumcraft.port.client.SalisMundusParticle;
import thaumcraft.port.client.FocusParticleMotion;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** BETA26 FXGeneric emotion, levitation and orb burst presets. */
final class GolemParticle extends TextureSheetParticle {
    private int grid=64,firstFrame,frames=1,delay;
    private boolean additive=true,loop,flipped;
    private float[] alphaKeys={1},scaleKeys={1};
    private float rotationStep,startRed=1,startGreen=1,startBlue=1,endRed,endGreen,endBlue;
    private double slowdown=.98f,randomX,randomY,randomZ,windX,windZ;
    private GolemParticle(ClientLevel level,Vec3 point,Vec3 velocity){super(level,point.x,point.y,point.z);setSize(.1f,.1f);xd=velocity.x;yd=velocity.y;zd=velocity.z;hasPhysics=true;}
    static GolemParticle emote(ClientLevel level,Vec3 point,Vec3 velocity,int frame,int frames,int lifetime,float size,float red,float green,float blue) {
        var p=new GolemParticle(level,point,velocity);p.firstFrame=frame;p.frames=frames;p.lifetime=lifetime;p.scaleKeys=new float[]{size};p.alphaKeys=new float[]{.5f};p.colour(red,green,blue);p.additive=false;return p;
    }
    static GolemParticle burst(ClientLevel level,Vec3 point){var p=new GolemParticle(level,point,Vec3.ZERO);p.grid=16;p.firstFrame=208;p.frames=31;p.lifetime=31;return p;}
    static GolemParticle flyer(ClientLevel level,Vec3 point,Vec3 velocity){
        var p=new GolemParticle(level,point,velocity);var r=level.random;p.lifetime=20+r.nextInt(5);p.alphaKeys=new float[]{.3f,0};p.grid=16;p.firstFrame=56;p.scaleKeys=new float[]{1.5f,3,8};p.slowdown=1;
        float angle=level.getMoonPhase()*(40+r.nextInt(10))*Mth.DEG_TO_RAD;p.windX=Mth.cos(angle)*.0001;p.windZ=-Mth.sin(angle)*.0001;p.rotation(r.nextFloat(),r.nextBoolean()?-1:1);return p;
    }
    private void colour(float r,float g,float b){startRed=endRed=r;startGreen=endGreen=g;startBlue=endBlue=b;}
    private void movement(double x,double y,double z){randomX=x;randomY=y;randomZ=z;}
    private void rotation(float start,float speed){roll=oRoll=start*Mth.TWO_PI;rotationStep=Mth.TWO_PI*speed*Mth.DEG_TO_RAD;}
    @Override public void tick(){if(delay>0&&--delay>0)return;xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}oRoll=roll;roll+=rotationStep;yd-=.04*gravity;move(xd,yd,zd);xd*=slowdown;yd*=slowdown;zd*=slowdown;xd+=level.random.nextGaussian()*randomX+windX;yd+=level.random.nextGaussian()*randomY;zd+=level.random.nextGaussian()*randomZ+windZ;if(onGround&&slowdown!=1){xd*=.7;zd*=.7;}}
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
    private int frame(){return firstFrame+(loop?age%frames:Math.min(frames-1,age*frames/Math.max(1,lifetime)));}
    @Override protected float getU0(){return (frame()%grid)/(float)grid;}
    @Override protected float getU1(){return (frame()%grid+1)/(float)grid;}
    @Override protected float getV0(){return (frame()/grid)/(float)grid;}
    @Override protected float getV1(){return (frame()/grid+1)/(float)grid;}
    private float key(float[] keys){float cursor=Mth.clamp(age/(float)lifetime,0,1)*(keys.length-1);int i=Math.min(keys.length-1,(int)cursor);return Mth.lerp(cursor-i,keys[i],keys[Math.min(i+1,keys.length-1)]);}
    @Override public float getQuadSize(float partial){return key(scaleKeys)*.1f;}
    @Override public void render(VertexConsumer buffer,Camera camera,float partial){
        renderFrame(buffer,camera.rotation(),camera.getPosition(),partial,getLightColor(partial));
    }
    void renderFrame(VertexConsumer buffer,Quaternionf cameraRotation,Vec3 cameraPosition,float partial,int light){
        // FXGeneric initializes its alpha/scale arrays on its first update. Its
        // constructor frame is invisible; do not flash a full-sized sprite first.
        if(delay>0||age==0)return;alpha=key(alphaKeys);float progress=Mth.clamp((age+partial)/lifetime,0,1);rCol=Mth.lerp(progress,startRed,endRed);gCol=Mth.lerp(progress,startGreen,endGreen);bCol=Mth.lerp(progress,startBlue,endBlue);
        var position=cameraPosition;float px=(float)(Mth.lerp(partial,xo,x)-position.x),py=(float)(Mth.lerp(partial,yo,y)-position.y),pz=(float)(Mth.lerp(partial,zo,z)-position.z);
        var rotation=new Quaternionf(cameraRotation).rotateZ(roll+(roll-oRoll)*partial);
        SalisMundusParticle.renderQuad(buffer,rotation,px,py,pz,getQuadSize(partial),flipped?getU1():getU0(),flipped?getU0():getU1(),getV0(),getV1(),rCol,gCol,bCol,alpha,light);
    }
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(additive);}
}
