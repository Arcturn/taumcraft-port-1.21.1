package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** The ten BETA26 FXGeneric presets, including their atlas grids and original blend layers. */
public final class FocusEffectParticle extends TextureSheetParticle {
    private int grid=64,firstFrame,frames=1,delay;
    private boolean additive=true,loop,flipped;
    private float[] alphaKeys={1},scaleKeys={1};
    private float rotationStep,startRed=1,startGreen=1,startBlue=1,endRed,endGreen,endBlue;
    private double slowdown=.98f,randomX,randomY,randomZ;
    private FocusEffectParticle(ClientLevel level,Vec3 point,Vec3 velocity){super(level,point.x,point.y,point.z);setSize(.1f,.1f);xd=velocity.x;yd=velocity.y;zd=velocity.z;hasPhysics=true;}
    public static FocusEffectParticle effect(String key,ClientLevel level,Vec3 point,Vec3 velocity) {
        var r=level.random;
        if(key.equals("thaumcraft.HEAL")||key.equals("thaumcraft.FLUX")||key.equals("thaumcraft.EXCHANGE"))velocity=velocity.add(r.nextGaussian()*.01,r.nextGaussian()*.01,r.nextGaussian()*.01);
        var p=new FocusEffectParticle(level,point,velocity);
        switch(key) {
            case "thaumcraft.AIR" -> {p.colour(1,1,1);p.gravity=-.1f;p.lifetime=20+r.nextInt(10);p.alphaKeys=new float[]{.5f,0};p.grid=32;p.firstFrame=337;p.frames=5;p.slowdown=.75;p.rotation(0,(float)r.nextGaussian()/2);float s=(float)(2+r.nextGaussian()*.5);p.scaleKeys=new float[]{s,s*2};}
            case "thaumcraft.EARTH" -> {p.colour(1,1,1);p.gravity=.4f;p.additive=false;p.lifetime=20+r.nextInt(10);p.alphaKeys=new float[]{1,0};p.firstFrame=75+r.nextInt(4);p.slowdown=.9;p.rotation(0,(float)r.nextGaussian());float s=(float)(1+r.nextGaussian()*.2);p.scaleKeys=new float[]{s,s/2};}
            case "thaumcraft.FIRE" -> {p.colour(1,1,1);p.gravity=-.2f;p.lifetime=10;p.alphaKeys=new float[]{.7f};p.firstFrame=640;p.frames=10;p.slowdown=.75;p.scaleKeys=new float[]{(float)(1.5+r.nextGaussian()*.2)};}
            case "thaumcraft.FROST" -> {p.lifetime=40+r.nextInt(40);p.alphaKeys=new float[]{1,0};p.firstFrame=8;p.gravity=.033f;p.slowdown=.8;p.movement(.0025,.0001,.0025);p.scaleKeys=new float[]{(float)(.7+r.nextGaussian()*.3)};p.rotation(r.nextFloat()*3,(float)r.nextGaussian()/4);}
            case "thaumcraft.HEAL" -> {p.lifetime=(int)(10+10*r.nextFloat());p.colour(1,1,1);p.alphaKeys=new float[]{0,.7f,.7f,0};p.scaleKeys=new float[]{r.nextFloat()*2,r.nextFloat()};p.slowdown=.8;p.gravity=(float)(r.nextGaussian()*.1);p.movement(.0125,.0125,.0125);p.rotation(0,(float)r.nextGaussian());p.delay=r.nextInt(4);}
            case "thaumcraft.FLUX" -> {p.lifetime=(int)(15+10*r.nextFloat());p.colour(.25f+r.nextFloat()*.25f,0,.25f+r.nextFloat()*.25f);p.alphaKeys=new float[]{0,1,1,0};p.firstFrame=128;p.frames=14;p.scaleKeys=new float[]{2+r.nextFloat(),.25f+r.nextFloat()*.25f};p.loop=true;p.slowdown=.9;p.gravity=(float)(r.nextGaussian()*.1);p.movement(.0125,.0125,.0125);p.rotation(0,(float)r.nextGaussian());p.delay=r.nextInt(4);}
            case "thaumcraft.CURSE" -> {p.lifetime=8;p.colour(.41f+r.nextFloat()*.2f,0,.019f+r.nextFloat()*.2f);p.alphaKeys=new float[]{0,r.nextFloat(),r.nextFloat(),r.nextFloat(),0};p.grid=16;p.firstFrame=72+r.nextInt(4);p.scaleKeys=new float[]{2+r.nextFloat()*4};p.slowdown=.9;p.rotation(r.nextFloat(),0);p.delay=r.nextInt(4);}
            case "thaumcraft.BREAK" -> {p.lifetime=6+r.nextInt(6);p.firstFrame=704+r.nextInt(4)*3;p.frames=3;p.slowdown=.8;p.scaleKeys=new float[]{(float)(1.7+r.nextGaussian()*.3)};}
            case "thaumcraft.EXCHANGE" -> {p.lifetime=9;p.colour(.25f+r.nextFloat()*.25f,.25f+r.nextFloat()*.25f,.25f+r.nextFloat()*.25f);p.alphaKeys=new float[]{0,.6f,.6f,0};p.firstFrame=448;p.frames=9;p.scaleKeys=new float[]{.5f,.25f};p.gravity=(float)(r.nextGaussian()*.01);p.movement(.0025,.0025,.0025);}
            case "thaumcraft.RIFT" -> {p.lifetime=16+r.nextInt(16);p.firstFrame=384+r.nextInt(16);p.slowdown=.75;p.alphaKeys=new float[]{1,0};p.scaleKeys=new float[]{(float)(.7+r.nextGaussian()*.3)};p.colour(.25f,.25f,1);p.movement(.01,.01,.01);}
            default -> {return null;}
        }
        p.alpha=p.alphaKeys[0];p.quadSize=p.scaleKeys[0]*.1f;return p;
    }
    public static FocusEffectParticle holeSparkle(ClientLevel level,Vec3 point) {
        var p=new FocusEffectParticle(level,point,Vec3.ZERO);p.colour(.25f,.25f,1);p.alphaKeys=new float[]{.9f};p.loop=true;p.firstFrame=320;p.frames=16;p.lifetime=6+level.random.nextInt(4);p.scaleKeys=new float[]{.6f+level.random.nextFloat()*.2f};return p;
    }
    public static FocusEffectParticle sapSpark(ClientLevel level,Vec3 point,float size,float red,float blue) {
        var p=new FocusEffectParticle(level,point,Vec3.ZERO);p.lifetime=5+level.random.nextInt(5);p.colour(red,0,blue);p.grid=16;p.firstFrame=8+level.random.nextInt(3)*16;p.frames=8;p.scaleKeys=new float[]{size};p.flipped=level.random.nextBoolean();return p;
    }
    private void colour(float r,float g,float b){startRed=endRed=r;startGreen=endGreen=g;startBlue=endBlue=b;}
    private void movement(double x,double y,double z){randomX=x;randomY=y;randomZ=z;}
    private void rotation(float start,float speed){roll=oRoll=start*Mth.TWO_PI;rotationStep=Mth.TWO_PI*speed*Mth.DEG_TO_RAD;}
    @Override public void tick(){if(delay>0&&--delay>0)return;xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}oRoll=roll;roll+=rotationStep;yd-=.04*gravity;move(xd,yd,zd);xd*=slowdown;yd*=slowdown;zd*=slowdown;xd+=level.random.nextGaussian()*randomX;yd+=level.random.nextGaussian()*randomY;zd+=level.random.nextGaussian()*randomZ;if(onGround&&slowdown!=1){xd*=.7;zd*=.7;}}
    private int frame(){return firstFrame+(loop?age%frames:Math.min(frames-1,age*frames/Math.max(1,lifetime)));}
    @Override protected float getU0(){return (frame()%grid)/(float)grid;}
    @Override protected float getU1(){return (frame()%grid+1)/(float)grid;}
    @Override protected float getV0(){return (frame()/grid)/(float)grid;}
    @Override protected float getV1(){return (frame()/grid+1)/(float)grid;}
    private float key(float[] keys){float cursor=Mth.clamp(age/(float)lifetime,0,1)*(keys.length-1);int i=Math.min(keys.length-1,(int)cursor);return Mth.lerp(cursor-i,keys[i],keys[Math.min(i+1,keys.length-1)]);}
    @Override public float getQuadSize(float partial){return key(scaleKeys)*.1f;}
    @Override public void render(VertexConsumer buffer,Camera camera,float partial){
        if(delay>0)return;alpha=key(alphaKeys);float progress=Mth.clamp((age+partial)/lifetime,0,1);rCol=Mth.lerp(progress,startRed,endRed);gCol=Mth.lerp(progress,startGreen,endGreen);bCol=Mth.lerp(progress,startBlue,endBlue);
        var position=camera.getPosition();float px=(float)(Mth.lerp(partial,xo,x)-position.x),py=(float)(Mth.lerp(partial,yo,y)-position.y),pz=(float)(Mth.lerp(partial,zo,z)-position.z);
        var rotation=new Quaternionf(camera.rotation()).rotateZ(roll+(roll-oRoll)*partial);
        SalisMundusParticle.renderQuad(buffer,rotation,px,py,pz,getQuadSize(partial),flipped?getU1():getU0(),flipped?getU0():getU1(),getV0(),getV1(),rCol,gCol,bCol,alpha,getLightColor(partial));
    }
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(additive);}
}
