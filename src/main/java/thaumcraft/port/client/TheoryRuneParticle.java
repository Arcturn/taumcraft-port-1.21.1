package thaumcraft.port.client;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Research-table glyph row 384..399 in the untouched original 64x64 particle atlas. */
public final class TheoryRuneParticle extends TextureSheetParticle {
    private final int glyph;private final float startSize;
    TheoryRuneParticle(ClientLevel l,double x,double y,double z,double vx,double vy,double vz){
        super(l,x,y,z);glyph=random.nextInt(16);lifetime=40+random.nextInt(20);hasPhysics=false;friction=.98f;gravity=0;xd=vx/lifetime;yd=vy/lifetime;zd=vz/lifetime;alpha=0;
        setColor(.5f+random.nextFloat()*.5f,.5f+random.nextFloat()*.5f,.5f+random.nextFloat()*.5f);startSize=(.8f+random.nextFloat()*.3f)*.1f;quadSize=startSize;
    }
    @Override protected float getU0(){return glyph/64f;}@Override protected float getU1(){return (glyph+1)/64f;}
    @Override protected float getV0(){return 6/64f;}@Override protected float getV1(){return 7/64f;}
    @Override public void tick(){super.tick();float f=age/(float)lifetime;float[] stops={0,.25f,.5f,.75f,0};int i=Math.min(3,(int)(f*4));alpha=net.minecraft.util.Mth.lerp(f*4-i,stops[i],stops[i+1]);quadSize=net.minecraft.util.Mth.lerp(f,startSize,.03f);}
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.scannerGlowLayer();}
    public static final class Provider implements ParticleProvider.Sprite<SimpleParticleType>{public TextureSheetParticle createParticle(SimpleParticleType t,ClientLevel l,double x,double y,double z,double vx,double vy,double vz){return new TheoryRuneParticle(l,x,y,z,vx,vy,vz);}}
}
