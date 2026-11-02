package thaumcraft.port.auromancy.focus.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.effect.*;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.content.PortSounds;

/** BETA26 flux focus: source settings, effects and return contract. */
public final class FocusEffectFlux extends SourceFocusEffect {
    @Override public String getResearch() { return "FOCUSFLUX"; }
    @Override public String getKey() { return "thaumcraft.FLUX"; }
    @Override public Aspect getAspect() { return Aspect.FLUX; }
    @Override public int getComplexity() { return getSettingValue("power")*3; }
    @Override public NodeSetting[] createSettings() { return new NodeSetting[]{power()}; }
    @Override public float getDamageForDisplay(float finalPower) { return (3+getSettingValue("power"))*finalPower; }
    @Override public boolean execute(HitResult target,Trajectory trajectory,float finalPower,int num) {
        if(world().isClientSide)return false;
        impact(target);
        if(target instanceof EntityHitResult hit)hit.getEntity().hurt(magic(hit.getEntity()),getDamageForDisplay(finalPower));
        return false;
    }
    @Override public void onCast(Entity caster) { castSound(caster,"field_187542_ac",2,2+(float)caster.level().random.nextGaussian()*.1f); }
}
