package thaumcraft.port.auromancy.client.media;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.port.auromancy.entity.*;
import thaumcraft.port.auromancy.focus.FocusEffect;
import thaumcraft.port.auromancy.focus.FocusEngine;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FocusMediaClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {event.enqueueWork(()->{
        FocusMediaVisuals.receive=FocusMediaClient::receive;FocusMediaVisuals.entityTick=FocusMediaClient::tick;FocusMediaVisuals.batTick=FocusMediaClient::bat;
    });}
    private static void receive(FocusMediaPayload payload) {
        var minecraft=Minecraft.getInstance();var level=minecraft.level;if(level==null)return;
        if(payload.kind()==1) {minecraft.particleEngine.add(new FocusBoltParticle(level,payload.from(),payload.to(),payload.color(),payload.width()));return;}
        if(payload.effects().isEmpty())return;
        int amount=Math.max(1,10/payload.effects().size());
        for(var key:payload.effects())if(FocusEngine.getElement(key) instanceof FocusEffect effect)
            for(int i=0;i<amount;i++)effect(effect,level,payload.from(),payload.to().add(gaussian(level,.05)));
    }
    private static void tick(SpellEntity entity) {
        if(!(entity.level() instanceof ClientLevel level))return;
        var effects=entity.getFocusPackage().getFocusEffects();if(effects.length==0)return;
        var random=level.random;
        if(entity instanceof EntityFocusCloud cloud) {
            for(int i=0;i<cloud.getRadius();i++) {
                var effect=effects[random.nextInt(effects.length)];var velocity=gaussian(level,.01);
                var pos=entity.position().add(gaussian(level,cloud.getRadius()*.5*.85));
                Minecraft.getInstance().particleEngine.add(new FocusMediumParticle(level,pos,velocity,FocusEngine.getElementColor(effect.getKey()),true));
                effect(effect,level,entity.position().add(gaussian(level,cloud.getRadius()*.5)),gaussian(level,.01));
            }
        } else if(entity instanceof EntityFocusMine)effect(effects[random.nextInt(effects.length)],level,entity.position().add(gaussian(level,.1)),gaussian(level,.01));
        // The projectile's fire mote is emitted from its interpolated render position.
    }
    private static void bat(EntitySpellBat bat) {
        if(!(bat.level() instanceof ClientLevel level))return;
        var effects=bat.getFocusPackage().getFocusEffects();if(effects.length==0)return;
        effect(effects[level.random.nextInt(effects.length)],level,bat.position().add(gaussian(level,.125)).add(0,bat.getBbHeight()/2,0),Vec3.ZERO);
    }
    public static void projectile(EntityFocusProjectile projectile,float partial,boolean effectThisTick) {
        if(!(projectile.level() instanceof ClientLevel level))return;
        var effects=projectile.getFocusPackage().getFocusEffects();if(effects.length==0)return;
        var random=level.random;var effect=effects[random.nextInt(effects.length)];
        var pos=new Vec3(net.minecraft.util.Mth.lerp(partial,projectile.xo,projectile.getX()),net.minecraft.util.Mth.lerp(partial,projectile.yo,projectile.getY())+projectile.getBbHeight()/2,net.minecraft.util.Mth.lerp(partial,projectile.zo,projectile.getZ()));
        var velocity=new Vec3((random.nextFloat()-.5)*.0125,(random.nextFloat()-.5)*.0125,(random.nextFloat()-.5)*.0125);
        Minecraft.getInstance().particleEngine.add(new FocusMediumParticle(level,pos,velocity,FocusEngine.getElementColor(effect.getKey()),false));
        if(effectThisTick)effect(effect,level,pos.add(gaussian(level,.1)),gaussian(level,.01));
    }
    private static Vec3 gaussian(ClientLevel level,double scale) {return new Vec3(level.random.nextGaussian()*scale,level.random.nextGaussian()*scale,level.random.nextGaussian()*scale);}
    private static void effect(FocusEffect effect,ClientLevel level,Vec3 pos,Vec3 velocity) {effect.renderParticleFX(level,pos.x,pos.y,pos.z,velocity.x,velocity.y,velocity.z);}
    private FocusMediaClient() { }
}
