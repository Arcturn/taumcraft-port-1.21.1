package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

/** BETA26 FXSmokeSpiral: two turns over a vertical semicircle, clamped above the floor. */
final class ElementalVortexParticle extends TextureSheetParticle {
    private final float radius;
    private final int rotation,minY;
    ElementalVortexParticle(ClientLevel level,double x,double y,double z,float radius,int rotation,int minY) {
        super(level,x,y,z);this.radius=radius;this.rotation=rotation;this.minY=minY;
        xd=yd=zd=0;hasPhysics=false;lifetime=20+level.random.nextInt(10);
        quadSize*=1.5f;setColor(221/255f,221/255f,221/255f);alpha=.66f;setSize(.01f,.01f);
    }
    @Override public void tick() { alpha=.66f*(lifetime-age)/(float)lifetime;if (age++>=lifetime) remove(); }
    @Override public void render(VertexConsumer output,Camera camera,float partial) {
        float t=(age+partial)/lifetime;
        float a=(rotation+720*t)*Mth.DEG_TO_RAD,b=(90-180*t)*Mth.DEG_TO_RAD;
        var eye=camera.getPosition();
        renderRotatedQuad(output,camera.rotation(),(float)(x-Mth.sin(a)*Mth.cos(b)*radius-eye.x),
                (float)(Math.max(y-Mth.sin(b)*radius,minY+.1f)-eye.y),
                (float)(z+Mth.cos(a)*Mth.cos(b)*radius-eye.z),partial);
    }
    private int frame() { return (int)(1+age/(float)lifetime*4); }
    @Override protected float getU0() { return frame()/64f; }
    @Override protected float getU1() { return (frame()+1)/64f; }
    @Override protected float getV0() { return 0; }
    @Override protected float getV1() { return 1/64f; }
    @Override public ParticleRenderType getRenderType() { return SalisMundusParticle.legacyLayer(false); }
}
