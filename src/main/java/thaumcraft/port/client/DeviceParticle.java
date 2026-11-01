package thaumcraft.port.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

/** The original FXVent2 expansion/fade and FXDispatcher jarSplashFx/spark presets. */
final class DeviceParticle extends TextureSheetParticle {
    private final int kind;
    private float scale,initial;
    private double lift;
    private DeviceParticle(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,int kind){super(level,x,y,z);xd=vx;yd=vy;zd=vz;this.kind=kind;setSize(.02f,.02f);}
    static DeviceParticle vent(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,int color){
        var particle=new DeviceParticle(level,x,y,z,vx,vy,vz,0);particle.scale=(level.random.nextFloat()*.1f+.05f)*4;particle.quadSize=.3f*particle.scale;
        particle.lift=level.random.nextGaussian()*.0075;particle.alpha=.4f*(1-particle.scale/4);particle.hasPhysics=true;
        particle.setColor(Mth.clamp((float)(((color>>16)&255)/255f+level.random.nextGaussian()*.05),0,1),Mth.clamp((float)(((color>>8)&255)/255f+level.random.nextGaussian()*.05),0,1),Mth.clamp((float)((color&255)/255f+level.random.nextGaussian()*.05),0,1));return particle;
    }
    static DeviceParticle splash(ClientLevel level,double x,double y,double z){
        var random=level.random;var particle=new DeviceParticle(level,x+random.nextGaussian()*.075,y,z+random.nextGaussian()*.075,random.nextGaussian()*.015,.075+random.nextFloat()*.05,random.nextGaussian()*.015,1);
        particle.lifetime=20+random.nextInt(10);particle.initial=.04f+random.nextFloat()*.03f;particle.quadSize=particle.initial;particle.alpha=.5f;particle.gravity=.3f;particle.friction=.98f;particle.setColor(40/255f,111/255f,246/255f);return particle;
    }
    static DeviceParticle spark(ClientLevel level,double x,double y,double z,double vx,double vy,double vz){
        var particle=new DeviceParticle(level,x,y,z,vx,vy,vz,2);particle.lifetime=10+level.random.nextInt(4);particle.quadSize=.025f+level.random.nextFloat()*.01f;particle.alpha=.9f;particle.friction=.98f;particle.setColor(1,.7f,.2f);return particle;
    }
    @Override public void tick(){
        if(kind==0){xo=x;yo=y;zo=z;age++;if(scale>=4){remove();return;}yd+=lift;move(xd,yd,zd);xd*=.85;yd*=.85;zd*=.85;scale=Math.min(4,scale*1.2f);quadSize=.3f*scale;alpha=.4f*(1-scale/4);if(onGround){xd*=.7;zd*=.7;}}
        else{super.tick();if(kind==1)quadSize=initial*(1-age/(float)lifetime);}
    }
    private int frame(){return kind==0?(int)(1+scale):kind==1?73:320+age%16;}
    @Override protected float getU0(){return (frame()%64)/64f;}
    @Override protected float getU1(){return (frame()%64+1)/64f;}
    @Override protected float getV0(){return (frame()/64)/64f;}
    @Override protected float getV1(){return (frame()/64+1)/64f;}
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(kind==2);}
}
