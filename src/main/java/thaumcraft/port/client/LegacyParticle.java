package thaumcraft.port.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Original TC6 sheet coordinates and FXDispatcher presets, on the linearly filtered original sheet. */
public final class LegacyParticle extends TextureSheetParticle {
    private final int grid, start, frames;
    private final boolean loop;
    private final float initialSize, finalSize;
    private final String kind;
    public LegacyParticle(String kind, ClientLevel level, double x, double y, double z,
                          double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        this.kind = kind.startsWith("nitor_")?"nitor":kind;
        xd=vx; yd=vy; zd=vz; friction=.98f; hasPhysics=false;
        switch(this.kind) {
            case "alumentum" -> { grid=64; start=448; frames=8; loop=false; lifetime=8; initialSize=.03f; finalSize=.03f; }
            case "taint" -> { grid=16; start=57+random.nextInt(3); frames=1; loop=false;
                lifetime=80+random.nextInt(20); initialSize=.12f; finalSize=.03f;
                gravity=.2f; friction=.975f; setColor(.4f+random.nextFloat()*.2f,.1f+random.nextFloat()*.3f,.5f+random.nextFloat()*.2f); }
            case "scan_spark", "spark" -> { grid=64; start=320; frames=16; loop=true;
                lifetime=6+random.nextInt(4); initialSize=.08f; finalSize=0; }
            case "craft" -> { grid=16; start=208; frames=31; loop=false;
                lifetime=31; initialSize=.8f; finalSize=.8f; }
            case "nitor" -> {grid=64;start=264;frames=8;loop=true;lifetime=10+random.nextInt(5);initialSize=.3f+random.nextFloat()*.1f;finalSize=.005f;}
            case "rune" -> { grid=64; start=512; frames=16; loop=true;
                lifetime=20+random.nextInt(10); initialSize=.1f; finalSize=.05f; }
            default -> { grid=64; start=264; frames=8; loop=true;
                lifetime=10+random.nextInt(5); initialSize=.1f+random.nextFloat()*.025f; finalSize=.005f; }
        }
        quadSize=initialSize;
        if(kind.startsWith("nitor_")) {int color=net.minecraft.world.item.DyeColor.byName(kind.substring(6),net.minecraft.world.item.DyeColor.YELLOW).getTextureDiffuseColor();setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f);}
    }
    private int frame() { return start + (loop ? age%frames : Math.min(frames-1,age*frames/Math.max(1,lifetime))); }
    @Override protected float getU0() { return (frame()%grid+.01f)/grid; }
    @Override protected float getU1() { return (frame()%grid+.99f)/grid; }
    @Override protected float getV0() { return (frame()/grid+.01f)/grid; }
    @Override protected float getV1() { return (frame()/grid+.99f)/grid; }
    @Override public void tick() {
        super.tick(); float t=Math.min(1,age/(float)lifetime);
        quadSize=initialSize+(finalSize-initialSize)*t;
        alpha=switch(kind) {case "taint" -> .75f*(1-t); case "wisp" -> .5f;
            case "rune" -> Math.min(1, Math.min(t*3,(1-t)*3))*.6f; default -> 1-t;};
        if(kind.equals("taint")) { oRoll=roll; roll+=.025f; }
        if(kind.equals("wisp")||kind.equals("rune")) { xd+=(random.nextFloat()-.5)*.005; zd+=(random.nextFloat()-.5)*.005; }
    }
    @Override protected int getLightColor(float tick) { return kind.equals("taint")?super.getLightColor(tick):0xf000f0; }
    @Override public ParticleRenderType getRenderType() { return SalisMundusParticle.legacyLayer(false); }
    public record Provider(String kind) implements ParticleProvider.Sprite<SimpleParticleType> {
        @Override public TextureSheetParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                 double vx, double vy, double vz) {
            return new LegacyParticle(kind,level,x,y,z,vx,vy,vz);
        }
    }
}
