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

/** BETA26 air focus: source settings, effects and return contract. */
public final class FocusEffectAir extends SourceFocusEffect {
    @Override public String getResearch() { return "FOCUSELEMENTAL"; }
    @Override public String getKey() { return "thaumcraft.AIR"; }
    @Override public Aspect getAspect() { return Aspect.AIR; }
    @Override public int getComplexity() { return getSettingValue("power")*2; }
    @Override public NodeSetting[] createSettings() { return new NodeSetting[]{power()}; }
    @Override public float getDamageForDisplay(float finalPower) { return (1+getSettingValue("power"))*finalPower; }
    @Override public boolean execute(HitResult target,Trajectory trajectory,float finalPower,int num) {
        if(world().isClientSide)return false;
        impact(target); FocusEffectSounds.play(world(),target.getLocation(),"field_187524_aN",.5f,.66f);
        if(target instanceof EntityHitResult hit) {
            Entity victim=hit.getEntity();float damage=getDamageForDisplay(finalPower);victim.hurt(thrown(victim),damage);
            if(victim instanceof LivingEntity living) {
                double yaw=Math.toRadians(victim.getYRot());
                living.knockback(damage*.25,trajectory!=null?-trajectory.direction.x:-Math.sin(yaw),trajectory!=null?-trajectory.direction.z:Math.cos(yaw));
            }
            return true;
        }
        return false;
    }
    @Override public void onCast(Entity caster) { caster.level().playSound(null,caster.blockPosition().above(),PortSounds.get("wind"),SoundSource.PLAYERS,.125f,2); }
}
