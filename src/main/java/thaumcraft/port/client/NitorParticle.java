package thaumcraft.port.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.DyeColor;

/** Original frames 264–271 (additive flame) and 457 (white alpha-blended core). */
public final class NitorParticle extends TextureSheetParticle {
    private static final ParticleRenderType GLOW=new ParticleRenderType(){
        public BufferBuilder begin(Tesselator tessellator,TextureManager textures){
            RenderSystem.depthMask(false);RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(GameRenderer::getParticleShader);RenderSystem.setShaderTexture(0,TextureAtlas.LOCATION_PARTICLES);
            return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
        }
        public String toString(){return "THAUMCRAFT_NITOR_GLOW";}
    };
    private final boolean core;
    private final float initialSize,corePulse;
    public NitorParticle(String kind,ClientLevel level,double x,double y,double z,double vx,double vy,double vz){
        super(level,x,y,z,vx,vy,vz);core=kind.equals("nitor_core");hasPhysics=false;friction=.98f;xd=vx;yd=vy;zd=vz;
        lifetime=core?10:10+random.nextInt(5);initialSize=core?.1f:.3f+random.nextFloat()*.1f;corePulse=(float)random.nextGaussian()*.1f;quadSize=initialSize;alpha=core?1:.66f;
        if(!core){int color=DyeColor.byName(kind.substring(6),DyeColor.YELLOW).getMapColor().col;setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f);}
    }
    private int frame(){return core?457:264+age%8;}
    @Override protected float getU0(){return sprite.getU((frame()%64+.01f)/64);}
    @Override protected float getU1(){return sprite.getU((frame()%64+.99f)/64);}
    @Override protected float getV0(){return sprite.getV((frame()/64+.01f)/64);}
    @Override protected float getV1(){return sprite.getV((frame()/64+.99f)/64);}
    @Override public float getQuadSize(float partial){float t=Math.min(1,(age+partial)/lifetime);return core?initialSize*(1+corePulse*(1-Math.abs(2*t-1))):initialSize+(.005f-initialSize)*t;}
    @Override public void tick(){super.tick();float motion=core?.0002f:.0025f;xd+=(random.nextFloat()-random.nextFloat())*motion;zd+=(random.nextFloat()-random.nextFloat())*motion;if(core)yd+=(random.nextFloat()-random.nextFloat())*motion;}
    @Override protected int getLightColor(float partial){return 0xf000f0;}
    @Override public ParticleRenderType getRenderType(){return core?ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT:GLOW;}
    public record Provider(String kind) implements ParticleProvider.Sprite<SimpleParticleType>{
        public TextureSheetParticle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double vx,double vy,double vz){return new NitorParticle(kind,level,x,y,z,vx,vy,vz);}
    }
}
