package thaumcraft.port.mob.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.particles.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.port.client.SalisMundusParticle;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.mob.*;

/** TC6's persistent animated swarm sprites and tentacle emergence debris. */
@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class TaintedParticles {
    private static ClientLevel world;
    private static final Map<Integer,List<SwarmParticle>> swarms=new HashMap<>();
    private static final List<Long> buzzes=new ArrayList<>();
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.isPaused())return;
        if(world!=mc.level){world=mc.level;swarms.clear();buzzes.clear();}if(world==null)return;
        swarms.entrySet().removeIf(entry->!(world.getEntity(entry.getKey()) instanceof TaintSwarm));
        for(var entity:world.entitiesForRendering()){
            if(entity instanceof TaintSwarm swarm&&swarm.isAlive()){
                var particles=swarms.computeIfAbsent(swarm.getId(),id->new ArrayList<>());particles.removeIf(p->!p.isAlive());
                if(particles.size()<30){var particle=new SwarmParticle(world,swarm);particles.add(particle);mc.particleEngine.add(particle);}
            }
            if(entity instanceof Taintacle tentacle&&tentacle.tickCount<tentacle.getBbHeight()*10&&tentacle.onGround()){
                float height=tentacle.getBbHeight();var floor=world.getBlockState(tentacle.blockPosition().below());
                for(int i=0;i<2*height;i++){
                    float angle=world.random.nextFloat()*Mth.PI*height,radius=(world.random.nextFloat()*.5f+.5f)*height*.25f;
                    mc.particleEngine.add(new TaintDebris(world,tentacle.getX()+Mth.sin(angle)*radius,tentacle.getY(),tentacle.getZ()+Mth.cos(angle)*radius));
                    if(!floor.isAir()){angle=world.random.nextFloat()*Mth.PI*height;radius=(world.random.nextFloat()*.5f+.5f)*height*.25f;world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,floor),tentacle.getX()+Mth.sin(angle)*radius,tentacle.getY(),tentacle.getZ()+Mth.cos(angle)*radius,0,0,0);}
                }
            }
        }
    }
    private static final class SwarmParticle extends TextureSheetParticle {
        private final TaintSwarm target;
        private final float green,blue,size;
        private int deathTicks;
        private float rotationPitch,rotationYaw;
        SwarmParticle(ClientLevel level,TaintSwarm target){
            super(level,target.getX()+(level.random.nextFloat()-level.random.nextFloat())*2,target.getY()+(level.random.nextFloat()-level.random.nextFloat())*2,target.getZ()+(level.random.nextFloat()-level.random.nextFloat())*2);
            this.target=target;rCol=.8f+random.nextFloat()*.2f;gCol=green=random.nextFloat()*.4f;bCol=blue=1-random.nextFloat()*.2f;
            quadSize=size=(1+random.nextFloat()*.5f)*.1f;gravity=.08f;
            xd=(random.nextFloat()-random.nextFloat())*.2;yd=(random.nextFloat()-random.nextFloat())*.2;zd=(random.nextFloat()-random.nextFloat())*.2;
        }
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(false);}
        @Override protected float getU0(){return (7+age%8)/64f;}
        @Override protected float getU1(){return getU0()+1/64f;}
        @Override protected float getV0(){return 4/64f;}
        @Override protected float getV1(){return 5/64f;}
        @Override public int getLightColor(float partial){return 240;}
        @Override public float getQuadSize(float partial){return size*(Mth.sin(age/3f)*.25f+1);}
        @Override public void tick(){
            xo=x;yo=y;zo=z;age++;boolean alive=target.isAlive()&&!target.isRemoved();
            if(!alive){deathTicks++;xd*=.9;zd*=.9;yd-=gravity/2;if(deathTicks>50)remove();}else yd+=gravity;
            move(xd,yd,zd);xd*=.985;yd*=.985;zd*=.985;
            if(alive){
                boolean toward=target.distanceToSqr(x,y,z)>target.getBbWidth()&&target.hurtTime<=0;
                float yawStep=(7.5f+random.nextInt(7))*(toward?1:-1),pitchStep=(7.5f+random.nextInt(7))*(toward?1:-1);
                double dx=target.getX()-x,dz=target.getZ()-z,dy=target.getBoundingBox().getCenter().y-getBoundingBox().getCenter().y;
                rotationYaw=turn(rotationYaw,(float)(Math.atan2(dz,dx)*180/Math.PI)-90,yawStep);
                rotationPitch=turn(rotationPitch,(float)(-Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI),pitchStep);
                xd=(-Mth.sin(rotationYaw*Mth.DEG_TO_RAD)*Mth.cos(rotationPitch*Mth.DEG_TO_RAD)+noise())*.22;
                zd=(Mth.cos(rotationYaw*Mth.DEG_TO_RAD)*Mth.cos(rotationPitch*Mth.DEG_TO_RAD)+noise())*.22;
                yd=(-Mth.sin(rotationPitch*Mth.DEG_TO_RAD)+noise())*.22;
            }
            alpha=(50-deathTicks)/50f;gCol=green/(target.hurtTime>0?2:1);bCol=blue/(target.hurtTime>0?2:1);
            if(buzzes.size()<3&&random.nextInt(50)==0&&level.getNearestPlayer(x,y,z,8,false)!=null){level.playLocalSound(x,y,z,PortSounds.get("fly"),SoundSource.HOSTILE,.03f,.5f+random.nextFloat()*.4f,false);buzzes.add(System.nanoTime()+1500000L);}
            if(buzzes.size()>=3&&buzzes.getFirst()<System.nanoTime())buzzes.removeFirst();
        }
        private double noise(){return random.nextGaussian()*(random.nextBoolean()?-1:1)*.0075*15;}
        private static float turn(float current,float wanted,float speed){float delta=Mth.wrapDegrees(wanted-current);if(delta>speed)delta=speed;if(delta< -speed)delta=-speed;return current+delta;}
    }
    private static final ParticleRenderType DEBRIS_LAYER=new ParticleRenderType(){
        @Override public BufferBuilder begin(Tesselator tessellator,TextureManager textures){RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShader(SalisMundusParticle::legacyShader);RenderSystem.setShaderTexture(0,TextureAtlas.LOCATION_BLOCKS);return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);}
        @Override public String toString(){return "THAUMCRAFT_TAINT_DEBRIS";}
    };
    private static final class TaintDebris extends TextureSheetParticle{
        private final float u,v;
        TaintDebris(ClientLevel level,double x,double y,double z){super(level,x,y,z,0,0,0);setSprite(Minecraft.getInstance().getItemRenderer().getModel(new ItemStack(Items.SLIME_BALL),level,null,0).getParticleIcon());u=random.nextFloat()*3;v=random.nextFloat()*3;setColor(.4f,0,.4f);gravity=1;quadSize*=.5f;alpha=.5f;lifetime=(int)(66/(random.nextFloat()*.9f+.1f));}
        @Override protected float getU0(){return sprite.getU(u/4);}
        @Override protected float getU1(){return sprite.getU((u+1)/4);}
        @Override protected float getV0(){return sprite.getV(v/4);}
        @Override protected float getV1(){return sprite.getV((v+1)/4);}
        @Override public ParticleRenderType getRenderType(){return DEBRIS_LAYER;}
        @Override public void render(VertexConsumer out,Camera camera,float partial){alpha=.5f*(1-age/(float)lifetime);super.render(out,camera,partial);}
    }
    private TaintedParticles(){}
}
