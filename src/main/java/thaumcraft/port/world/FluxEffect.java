package thaumcraft.port.world;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
public final class FluxEffect extends MobEffect {
    private final boolean taint;
    public FluxEffect(boolean taint){super(MobEffectCategory.HARMFUL,taint?0x7c1689:0x604482);this.taint=taint;}
    @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return duration%Math.max(1,taint?40>>Math.min(amplifier,8):40)==0;}
    @Override public boolean applyEffectTick(LivingEntity target,int amplifier){
        if(target.level().isClientSide)return true;
        if(taint){if(target instanceof TaintSeed)target.heal(1);else target.hurt(target.damageSources().magic(),1);}
        else for(var e:target.level().getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(4)))
            if(!e.hasEffect(WorldEntities.INFECTIOUS))e.addEffect(new MobEffectInstance(amplifier>0?WorldEntities.INFECTIOUS:WorldEntities.VIS_EXHAUST,6000,Math.max(0,amplifier-1)));
        return true;
    }
}
