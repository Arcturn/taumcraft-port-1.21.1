package thaumcraft.port.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.port.world.*;

/** Original seed plumes/flashes, finite-goo bubbles and falling crust debris. */
@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class TaintParticles {
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.isPaused())return;
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof TaintSeed seed&&seed.isAlive()){
            double x=seed.getX()+Math.sin(seed.tickCount*.05-.5)/5;
            double z=seed.getZ()+Math.sin(seed.tickCount*.06-.5)/5+seed.hurtTime/200f+seed.attackAnim;
            mc.particleEngine.add(new TaintMote(mc.level,x,seed.getY()+seed.getBbHeight()+.25,z,mc.level.random.nextFloat()<.033));
        }
    }
    public static void goo(Level level,BlockPos pos){
        if(!(level instanceof ClientLevel client)||!level.getBlockState(pos).is(WorldContent.block("flux_goo")))return;
        int amount=level.getBlockState(pos).getValue(FluxGooBlock.LEVEL);
        Minecraft.getInstance().particleEngine.add(new GooBubble(client,pos.getX()+level.random.nextFloat(),pos.getY()+amount*.125,pos.getZ()+level.random.nextFloat()));
    }
    public static void landing(FallingTaint entity){
        if(!(entity.level() instanceof ClientLevel level))return;
        for(int i=0;i<10;i++){
            double angle=level.random.nextFloat()*Math.PI*2,radius=level.random.nextFloat()*.5+.5;
            Minecraft.getInstance().particleEngine.add(new Debris(level,entity.getX()+Math.sin(angle)*radius,entity.getY()+entity.getBbHeight()/2,entity.getZ()+Math.cos(angle)*radius));
        }
    }
    private static final class TaintMote extends TextureSheetParticle {
        private final int frame;private final boolean flash;private final float startSize,initialAlpha,rotation;
        TaintMote(ClientLevel level,double x,double y,double z,boolean flash){
            super(level,x,y,z);this.flash=flash;frame=flash?108+random.nextInt(4):57+random.nextInt(3);
            lifetime=flash?5+random.nextInt(5):80+random.nextInt(20);initialAlpha=flash?.5f:.75f;alpha=initialAlpha;
            startSize=quadSize=(flash?1.5f+random.nextFloat():2)*.1f;friction=flash?.98f:.975f;gravity=flash?0:.2f;
            if(flash)setColor(.7f,.1f,.9f);else{setColor(.4f+random.nextFloat()*.2f,.1f+random.nextFloat()*.3f,.5f+random.nextFloat()*.2f);xd=random.nextGaussian()*.05;yd=.1+.01*random.nextFloat();zd=random.nextGaussian()*.05;}
            roll=oRoll=random.nextFloat()*(float)(Math.PI*2);rotation=flash?0:(random.nextBoolean()?-1:1)*(float)(Math.PI/180*Math.PI*2);
        }
        @Override protected float getU0(){return frame%16/16f;}
        @Override protected float getU1(){return getU0()+1/16f;}
        @Override protected float getV0(){return frame/16/16f;}
        @Override protected float getV1(){return getV0()+1/16f;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(flash);}
        @Override public void tick(){super.tick();oRoll=roll;roll+=rotation;float f=age/(float)lifetime;alpha=initialAlpha*(1-f);quadSize=flash?startSize:startSize*(1-f*.75f);}
    }
    private static final class GooBubble extends TextureSheetParticle {
        private final float size;
        GooBubble(ClientLevel level,double x,double y,double z){super(level,x,y,z);lifetime=2+random.nextInt(3);quadSize=size=(random.nextFloat()*.3f+.2f)*.1f;setColor(1,0,.5f);alpha=.25f;gravity=-.01f;}
        private int cell(){return age>lifetime-2?(age>=lifetime?65:66):64;}
        @Override protected float getU0(){return cell()%64/64f;}
        @Override protected float getU1(){return getU0()+1/64f;}
        @Override protected float getV0(){return cell()/64/64f;}
        @Override protected float getV1(){return getV0()+1/64f;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(true);}
        @Override public void tick(){super.tick();xd+=random.nextGaussian()*.001;yd+=random.nextGaussian()*.001;zd+=random.nextGaussian()*.001;quadSize=size;alpha=.25f;}
    }
    private static final ParticleRenderType DEBRIS_LAYER=new ParticleRenderType(){
        @Override public BufferBuilder begin(Tesselator tessellator,TextureManager textures){RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShader(SalisMundusParticle::legacyShader);RenderSystem.setShaderTexture(0,TextureAtlas.LOCATION_BLOCKS);return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);}
        @Override public String toString(){return "THAUMCRAFT_TAINT_LAND";}
    };
    private static final class Debris extends TextureSheetParticle {
        private final float u,v;
        Debris(ClientLevel level,double x,double y,double z){super(level,x,y,z,0,0,0);setSprite(Minecraft.getInstance().getItemRenderer().getModel(new ItemStack(Items.SLIME_BALL),level,null,0).getParticleIcon());u=random.nextFloat()*3;v=random.nextFloat()*3;setColor(.1f,0,.1f);gravity=1;quadSize*=.5f;alpha=.4f;lifetime=(int)(66/(random.nextFloat()*.9f+.1f));}
        @Override protected float getU0(){return sprite.getU(u/4);}
        @Override protected float getU1(){return sprite.getU((u+1)/4);}
        @Override protected float getV0(){return sprite.getV(v/4);}
        @Override protected float getV1(){return sprite.getV((v+1)/4);}
        @Override public ParticleRenderType getRenderType(){return DEBRIS_LAYER;}
        @Override public void render(VertexConsumer out,Camera camera,float partial){alpha=.4f*(1-age/(float)lifetime);super.render(out,camera,partial);}
    }
    private TaintParticles(){}
}
