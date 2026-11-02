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

/** BETA26 frost focus: source settings, effects and return contract. */
public final class FocusEffectFrost extends SourceFocusEffect {
    @Override public String getResearch() { return "FOCUSELEMENTAL"; }
    @Override public String getKey() { return "thaumcraft.FROST"; }
    @Override public Aspect getAspect() { return Aspect.COLD; }
    @Override public int getComplexity() { return getSettingValue("duration")+getSettingValue("power")*2; }
    @Override public NodeSetting[] createSettings() { return new NodeSetting[]{power(),range("duration","focus.common.duration",2,10)}; }
    @Override public float getDamageForDisplay(float finalPower) { return (3+getSettingValue("power"))*finalPower; }
    @Override public boolean execute(HitResult target,Trajectory trajectory,float finalPower,int num) {
        if(world().isClientSide)return false;
        impact(target);
        if(target instanceof EntityHitResult hit) {
            hit.getEntity().hurt(thrown(hit.getEntity()),getDamageForDisplay(finalPower));
            if(hit.getEntity() instanceof LivingEntity living)living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,20*getSettingValue("duration"),(int)(1+getSettingValue("power")*finalPower/3)));
        } else if(target instanceof BlockHitResult hit&&world() instanceof ServerLevel level) {
            float radius=Math.min(16,2*getSettingValue("power")*finalPower);var ice=Blocks.FROSTED_ICE.defaultBlockState();
            for(BlockPos cursor:BlockPos.betweenClosed(BlockPos.containing(hit.getBlockPos().getX()-radius,hit.getBlockPos().getY()-radius,hit.getBlockPos().getZ()-radius),BlockPos.containing(hit.getBlockPos().getX()+radius,hit.getBlockPos().getY()+radius,hit.getBlockPos().getZ()+radius))) {
                if(Vec3.atCenterOf(cursor).distanceToSqr(hit.getLocation())>radius*radius||!level.hasChunkAt(cursor))continue;
                var water=level.getBlockState(cursor);
                if(water.is(Blocks.WATER)&&water.getValue(LiquidBlock.LEVEL)==0&&ice.canSurvive(level,cursor)&&level.isUnobstructed(ice,cursor,net.minecraft.world.phys.shapes.CollisionContext.empty())&&FocusWorldEffects.mayModify(level,caster(),cursor)) {
                    level.setBlockAndUpdate(cursor,ice);level.scheduleTick(cursor,Blocks.FROSTED_ICE,60+level.random.nextInt(61));
                }
            }
        }
        return false;
    }
    @Override public void onCast(Entity caster) { castSound(caster,"field_187942_hp",.2f,1+(float)caster.level().random.nextGaussian()*.05f); }
}
