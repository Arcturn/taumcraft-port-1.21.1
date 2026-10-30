package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** FXBlockRunes: original fixed outward-facing planes, atlas, drift and alpha. */
public final class ScanRuneParticle extends TextureSheetParticle {
    private final int glyph;
    private final Quaternionf rotation;
    public ScanRuneParticle(ClientLevel level,double x,double y,double z,int duration) {
        super(level,x,y,z);
        rotation=new Quaternionf().rotateY(random.nextInt(4)*(float)Math.PI/2).rotateZ((float)Math.PI/2);
        glyph=random.nextInt(16);
        var offset=new Vector3f(random.nextFloat()*.2f,-.3f+random.nextFloat()*.6f,-.51f).rotate(rotation);
        setPos(x+offset.x,y+offset.y,z+offset.z);xo=this.x;yo=this.y;zo=this.z;
        xd=yd=zd=0;friction=1;gravity=.03f;hasPhysics=false;
        lifetime=Math.max(0,duration*3);
        quadSize=.15f*(1+(float)random.nextGaussian()*.1f);alpha=0;
        setSize(.01f,.01f);
    }
    @Override protected float getU0() {return glyph/64f;}
    @Override protected float getU1() {return (glyph+1)/64f;}
    @Override protected float getV0() {return 6/64f;}
    @Override protected float getV1() {return 7/64f;}
    @Override public void tick() {
        xo=x;yo=y;zo=z;
        alpha=ScannerVisualRules.runeOpacity(age,lifetime);
        if(age++>=lifetime) remove();
        yd+=ScannerVisualRules.RUNE_ACCELERATION;
        setPos(x+xd,y+yd,z+zd);
    }
    @Override public void render(VertexConsumer buffer,Camera camera,float partial) {
        var origin=camera.getPosition();
        emitQuad(buffer,rotation,(float)(Mth.lerp(partial,xo,x)-origin.x),
                (float)(Mth.lerp(partial,yo,y)-origin.y),(float)(Mth.lerp(partial,zo,z)-origin.z),
                quadSize,getU0(),getU1(),getV0(),getV1(),rCol,gCol,bCol,alpha);
    }
    static void emitQuad(VertexConsumer buffer,Quaternionf rotation,float x,float y,float z,float halfSize,
                         float u0,float u1,float v0,float v1,float red,float green,float blue,float opacity) {
        // Vanilla billboards face +Z. These planes sit on the -Z side of a block,
        // so the original -Z winding is essential: otherwise all four face inward.
        float[][] vertices={{-1,1,u1,v1},{1,1,u1,v0},{1,-1,u0,v0},{-1,-1,u0,v1}};
        for(var vertex:vertices) {
            var point=new Vector3f(vertex[0],vertex[1],0).rotate(rotation).mul(halfSize).add(x,y,z);
            buffer.addVertex(point.x,point.y,point.z).setUv(vertex[2],vertex[3])
                    .setColor(red,green,blue,opacity).setLight(0x00F00000);
        }
    }
    @Override public ParticleRenderType getRenderType() {return SalisMundusParticle.scannerGlowLayer();}
    public static final class Provider implements ParticleProvider.Sprite<SimpleParticleType> {
        public TextureSheetParticle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double vx,double duration,double vz) {
            return new ScanRuneParticle(level,x,y,z,(int)duration);
        }
    }
}
