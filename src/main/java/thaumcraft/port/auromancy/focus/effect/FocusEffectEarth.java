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

/** BETA26 earth focus: source settings, effects and return contract. */
public final class FocusEffectEarth extends SourceFocusEffect {
    @Override public String getResearch() { return "FOCUSELEMENTAL"; }
    @Override public String getKey() { return "thaumcraft.EARTH"; }
    @Override public Aspect getAspect() { return Aspect.EARTH; }
    @Override public int getComplexity() { return getSettingValue("power")*3; }
    @Override public NodeSetting[] createSettings() { return new NodeSetting[]{power()}; }
    @Override public float getDamageForDisplay(float finalPower) { return 2*getSettingValue("power")*finalPower; }
    @Override public boolean execute(HitResult target,Trajectory trajectory,float finalPower,int num) {
        if(world().isClientSide)return false;
        impact(target);float damage=getDamageForDisplay(finalPower);
        if(target instanceof EntityHitResult hit){hit.getEntity().hurt(thrown(hit.getEntity()),damage);return true;}
        if(target instanceof BlockHitResult hit&&caster() instanceof ServerPlayer player) {
            var state=world().getBlockState(hit.getBlockPos());
            if(state.getDestroySpeed(world(),hit.getBlockPos())<=damage/25)
                FocusWorldEffects.queueBreak(player,hit.getBlockPos(),state,false,false,0,1,0,1,num,.1f,getPackage().getCastingHand());
        }
        return false;
    }
    @Override public void onCast(Entity caster) { castSound(caster,"field_187523_aM",.25f,1+(float)caster.level().random.nextGaussian()*.05f); }
}
