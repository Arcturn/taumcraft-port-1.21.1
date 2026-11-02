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

/** BETA26 fire focus: source settings, effects and return contract. */
public final class FocusEffectFire extends SourceFocusEffect {
    @Override public String getResearch() { return "BASEAUROMANCY"; }
    @Override public String getKey() { return "thaumcraft.FIRE"; }
    @Override public Aspect getAspect() { return Aspect.FIRE; }
    @Override public int getComplexity() { return getSettingValue("duration")+getSettingValue("power")*2; }
    @Override public NodeSetting[] createSettings() { return new NodeSetting[]{power(),range("duration","focus.fire.burn",0,5)}; }
    @Override public float getDamageForDisplay(float finalPower) { return (3+getSettingValue("power"))*finalPower; }
    @Override public boolean execute(HitResult target,Trajectory trajectory,float finalPower,int num) {
        if(world().isClientSide)return false;
        impact(target);
        if(target instanceof EntityHitResult hit) {
            Entity victim=hit.getEntity();if(victim.fireImmune())return false;
            var damage=new DamageSource(world().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.FIREBALL),victim,caster());
            victim.hurt(damage,getDamageForDisplay(finalPower));
            victim.igniteForSeconds(Math.round((1+getSettingValue("duration")*getSettingValue("duration"))*finalPower));return true;
        }
        if(target instanceof BlockHitResult hit&&getSettingValue("duration")>0) {
            BlockPos pos=hit.getBlockPos().relative(hit.getDirection());
            if(world().isEmptyBlock(pos)&&world().random.nextFloat()<finalPower&&FocusWorldEffects.mayModify(world(),caster(),pos)) {
                FocusEffectSounds.play(world(),Vec3.atCenterOf(pos),"field_187649_bu",1,world().random.nextFloat()*.4f+.8f);
                world().setBlock(pos,Blocks.FIRE.defaultBlockState(),11);return true;
            }
        }
        return false;
    }
    @Override public void onCast(Entity caster) { castSound(caster,"field_187616_bj",1,1+(float)caster.level().random.nextGaussian()*.05f); }
}
