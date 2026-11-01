package thaumcraft.port.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;

/** FXFireMote at original scale 4, layer 1, atlas frame 7 and alpha .5. */
public final class AlumentumFireParticle extends TextureSheetParticle {
    public AlumentumFireParticle(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,float red,float green,float blue){
        super(level,x,y,z,0,0,0);
        xd=vx;yd=vy;zd=vz;lifetime=16;quadSize=.4f;alpha=.5f;roll=(float)Math.PI*2;
        setColor(red,green,blue);
    }
    @Override public void tick(){
        super.tick();if(random.nextInt(6)==0)age++;if(age>=lifetime)remove();
        float remaining=1-age/(float)lifetime;quadSize=.4f*remaining;alpha=.5f*remaining;oRoll=roll;roll+=1;
    }
    @Override protected float getU0(){return 7/64f;}
    @Override protected float getU1(){return 8/64f;}
    @Override protected float getV0(){return 0;}
    @Override protected float getV1(){return 1/64f;}
    @Override protected int getLightColor(float partialTick){return 255;}
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(false);}
}
