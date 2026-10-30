package thaumcraft.port.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.shapes.Shapes;

/** BETA26 FXGeneric: frame 457 alpha core, then additive flames 264–271. */
public final class NitorParticle extends TextureSheetParticle {
    private final boolean core;
    private final float[] scaleFrames;
    public NitorParticle(String kind,ClientLevel level,double x,double y,double z,double vx,double vy,double vz){
        super(level,x,y,z);
        core=kind.equals("nitor_core");
        setSize(.1f,.1f);setPos(x,y,z);
        hasPhysics=true;xd=vx;yd=vy;zd=vz;
        var preset=NitorVisualRules.preset(level.random,core);
        lifetime=preset.lifetime();alpha=preset.alpha();scaleFrames=preset.scaleFrames();quadSize=scaleFrames[0];
        if(!core){
            int colour=DyeColor.byName(kind.substring(6),DyeColor.YELLOW).getMapColor().col;
            setColor(((colour>>16)&255)/255f,((colour>>8)&255)/255f,(colour&255)/255f);
        }
    }
    private int frame(){return NitorVisualRules.frame(core,age);}
    @Override protected float getU0(){return sprite.getU(NitorVisualRules.u0(frame()));}
    @Override protected float getU1(){return sprite.getU(NitorVisualRules.u1(frame()));}
    @Override protected float getV0(){return sprite.getV(NitorVisualRules.v0(frame()));}
    @Override protected float getV1(){return sprite.getV(NitorVisualRules.v1(frame()));}
    // Original position interpolation uses partial ticks; scale and atlas frame use whole particle age.
    @Override public float getQuadSize(float partial){return NitorVisualRules.size(scaleFrames,age);}
    @Override public void tick(){
        xo=x;yo=y;zo=z;
        if(age++>=lifetime)remove();
        oRoll=roll;
        move(xd,yd,zd);
        xd*=NitorVisualRules.SLOWDOWN;yd*=NitorVisualRules.SLOWDOWN;zd*=NitorVisualRules.SLOWDOWN;
        float motion=core?NitorVisualRules.CORE_MOVEMENT:NitorVisualRules.FLAME_MOVEMENT;
        // FXGeneric draws all three Gaussian samples even when the flame's Y multiplier is zero.
        xd+=level.random.nextGaussian()*motion;
        yd+=level.random.nextGaussian()*(core?motion:0);
        zd+=level.random.nextGaussian()*motion;
        if(onGround){xd*=.7f;zd*=.7f;}
    }
    @Override public void move(double mx,double my,double mz){
        // 1.12 Particle clips Y, X, then Z on every call. Modern Particle.move latches
        // stoppedByCollision after vertical contact, freezing flames against ceilings.
        double requestedX=mx,requestedY=my,requestedZ=mz;
        var bounds=getBoundingBox();
        if(hasPhysics){
            var collisions=level.getBlockCollisions(null,bounds.expandTowards(mx,my,mz));
            my=Shapes.collide(Direction.Axis.Y,bounds,collisions,my);bounds=bounds.move(0,my,0);
            mx=Shapes.collide(Direction.Axis.X,bounds,collisions,mx);bounds=bounds.move(mx,0,0);
            mz=Shapes.collide(Direction.Axis.Z,bounds,collisions,mz);bounds=bounds.move(0,0,mz);
        }else bounds=bounds.move(mx,my,mz);
        setBoundingBox(bounds);setLocationFromBoundingbox();
        onGround=requestedY!=my&&requestedY<0;
        if(requestedX!=mx)xd=0;
        if(requestedZ!=mz)zd=0;
    }
    // FXGeneric inherits world light sampling: the placed block supplies its own level-15 light.
    @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(!core);}
    public record Provider(String kind) implements ParticleProvider.Sprite<SimpleParticleType>{
        public TextureSheetParticle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double vx,double vy,double vz){return new NitorParticle(kind,level,x,y,z,vx,vy,vz);}
    }
}
