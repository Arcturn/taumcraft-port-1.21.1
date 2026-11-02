package thaumcraft.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.port.auromancy.effect.*;
import thaumcraft.port.content.*;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FocusEffectClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{FocusEffectVisuals.particles=FocusEffectClient::particle;FocusEffectVisuals.receiver=FocusEffectClient::receive;FocusEffectVisuals.holeAmbient=FocusEffectClient::hole;FocusEffectVisuals.sapAmbient=FocusEffectClient::sap;});}
    private static void particle(String key,Level world,Vec3 position,Vec3 velocity){if(world instanceof ClientLevel level){var p=FocusEffectParticle.effect(key,level,position,velocity);if(p!=null)add(p);}}
    private static void receive(FocusEffectVisuals.Payload payload){
        var client=Minecraft.getInstance();var level=client.level;if(level==null||client.player==null||client.player.position().distanceToSqr(payload.position())>80*80)return;
        if(payload.kind()==1){bamf(level,payload);return;}
        int count=Math.max(1,15/payload.keys().size());
        for(String key:payload.keys())for(int i=0;i<count;i++)particle(key,level,payload.position(),new Vec3(level.random.nextGaussian()*.15,level.random.nextGaussian()*.15,level.random.nextGaussian()*.15));
    }
    private static void bamf(ClientLevel level,FocusEffectVisuals.Payload payload){
        var point=payload.position();var r=level.random;float red=((payload.colour()>>16)&255)/255f,green=((payload.colour()>>8)&255)/255f,blue=(payload.colour()&255)/255f;
        if(payload.sound())level.playLocalSound(point.x,point.y,point.z,PortSounds.get("poof"),SoundSource.BLOCKS,.4f,1+(float)r.nextGaussian()*.05f,false);
        for(int i=0;i<8+r.nextInt(3);i++){Vec3 velocity=new Vec3(signed(level,.05,.05),signed(level,.05,.05),signed(level,.05,.05));var p=dust(point.add(velocity.scale(2)),velocity.scale(.5));if(p!=null)p.smoke(Mth.clamp(red*(1+(float)r.nextGaussian()*.1f),0,1),Mth.clamp(green*(1+(float)r.nextGaussian()*.1f),0,1),Mth.clamp(blue*(1+(float)r.nextGaussian()*.1f),0,1));}
        if(payload.flair()){
            for(int i=0;i<2+r.nextInt(3);i++){Vec3 velocity=new Vec3(signed(level,.025,.025),signed(level,.025,.025),signed(level,.025,.025));var p=dust(point.add(velocity.scale(2)),velocity);if(p!=null)p.mote(15+r.nextInt(10));}
            var p=dust(point,Vec3.ZERO);if(p!=null)p.flash();
        }
        for(int i=0;i<(payload.flair()?2:0)+r.nextInt(3);i++){Vec3 velocity=new Vec3(signed(level,.0025,.005),signed(level,.0025,.005),signed(level,.0025,.005));var p=dust(point.add(velocity.scale(5)),velocity);if(p!=null)p.wisp(i,(.9f+r.nextFloat()*.1f+red)/2,(.1f+green)/2,(.5f+r.nextFloat()*.1f+blue)/2);}
    }
    private static double signed(ClientLevel level,double base,double spread){return (base+level.random.nextFloat()*spread)*(level.random.nextBoolean()?-1:1);}
    private static SalisMundusParticle dust(Vec3 point,Vec3 velocity){var client=Minecraft.getInstance();if(client.level==null||!FocusParticleAdmission.allows(client.options.particles().get(),client.getFps(),client.level.random.nextInt(3)))return null;var p=client.particleEngine.createParticle(PortParticles.get("salis_mundus"),point.x,point.y,point.z,velocity.x,velocity.y,velocity.z);return p instanceof SalisMundusParticle dust?dust:null;}
    private static void sap(Level world,BlockPos pos){
        if(!(world instanceof ClientLevel level))return;var random=level.random;float h=random.nextFloat()*.33f;
        Vec3 point=new Vec3(pos.getX()+random.nextFloat(),pos.getY()+.1515+h/2,pos.getZ()+random.nextFloat());
        add(FocusEffectParticle.sapSpark(level,point,3+h*6,.3f-random.nextFloat()*.1f,.5f+random.nextFloat()*.2f));
        if(random.nextInt(50)==0)level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),PortSounds.get("jacobs"),SoundSource.AMBIENT,.25f,1+(random.nextFloat()-random.nextFloat())*.2f,false);
    }
    private static void hole(HoleBlockEntity hole){
        if(!(hole.getLevel() instanceof ClientLevel level))return;BlockPos pos=hole.getBlockPos();
        for(int pass=0;pass<2;pass++)for(Direction a:Direction.values()){
            BlockPos neighbor=pos.relative(a);var first=level.getBlockState(neighbor);if(first.is(FocusEffectContent.HOLE.get())||first.isSolidRender(level,neighbor))continue;
            for(Direction b:Direction.values()){
                BlockPos side=pos.relative(b),diagonal=neighbor.relative(b);
                if(a.getAxis()==b.getAxis()||!level.getBlockState(side).isSolidRender(level,side)&&!level.getBlockState(diagonal).isSolidRender(level,diagonal))continue;
                float x=.5f*(a.getStepX()!=0?a.getStepX():b.getStepX()),y=.5f*(a.getStepY()!=0?a.getStepY():b.getStepY()),z=.5f*(a.getStepZ()!=0?a.getStepZ():b.getStepZ());
                Vec3 point=new Vec3(pos.getX()+(x==0?level.random.nextFloat():x+.5f),pos.getY()+(y==0?level.random.nextFloat():y+.5f),pos.getZ()+(z==0?level.random.nextFloat():z+.5f));
                if(level.random.nextInt(6)<4)add(FocusEffectParticle.holeSparkle(level,point));
            }
        }
    }
    private static void add(FocusEffectParticle particle){
        var client=Minecraft.getInstance();if(client.level!=null&&FocusParticleAdmission.allows(client.options.particles().get(),client.getFps(),client.level.random.nextInt(3)))client.particleEngine.add(particle);
    }
    private FocusEffectClient(){}
}
