package thaumcraft.port.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;

/** Original FXVent's expanding, naturally lit smoke on the TC6 particle atlas. */
public final class SmelterVentParticle extends TextureSheetParticle {
    private float scale;
    public SmelterVentParticle(ClientLevel level,double x,double y,double z,double vx,double vy,double vz){
        super(level,x,y,z,vx,vy,vz);
        setSize(.02f,.02f);scale=random.nextFloat()*.1f+.05f;
        double length=Math.sqrt(vx*vx+vy*vy+vz*vz);
        xd=(vx/length+spread())*.125;yd=(vy/length+spread())*.125;zd=(vz/length+spread())*.125;
        setColor(170/255f,170/255f,170/255f);quadSize=.3f*scale;alpha=.4f*(1-scale);
    }
    private double spread(){return random.nextGaussian()*(random.nextBoolean()?-1:1)*.0075*5;}
    @Override public void tick(){
        xo=x;yo=y;zo=z;age++;
        if(scale>=1){remove();return;}
        yd+=.0025;move(xd,yd,zd);xd*=.8500000190734863;yd*=.8500000190734863;zd*=.8500000190734863;
        scale=Math.min(1,scale*1.15f);quadSize=.3f*scale;alpha=.4f*(1-scale);
        if(onGround){xd*=.7;zd*=.7;}
    }
    private int frame(){return (int)(1+scale*4);}
    @Override protected float getU0(){return frame()/64f;}
    @Override protected float getU1(){return (frame()+1)/64f;}
    @Override protected float getV0(){return 0;}
    @Override protected float getV1(){return 1/64f;}
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(false);}
}
