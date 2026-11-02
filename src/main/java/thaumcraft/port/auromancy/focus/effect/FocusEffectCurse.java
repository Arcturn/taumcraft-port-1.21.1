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

/** BETA26 curse focus: source settings, effects and return contract. */
public final class FocusEffectCurse extends SourceFocusEffect {
    @Override public String getResearch() { return "FOCUSCURSE"; }
    @Override public String getKey() { return "thaumcraft.CURSE"; }
    @Override public Aspect getAspect() { return Aspect.DEATH; }
    @Override public int getComplexity() { return getSettingValue("duration")+getSettingValue("power")*3; }
    @Override public NodeSetting[] createSettings() { return new NodeSetting[]{power(),range("duration","focus.common.duration",1,10)}; }
    @Override public float getDamageForDisplay(float finalPower) { return (1+getSettingValue("power"))*finalPower; }
    @Override public boolean execute(HitResult target,Trajectory trajectory,float finalPower,int num) {
        if(world().isClientSide)return false;
        FocusEffectVisuals.bamf(world(),target.getLocation(),6946821,true,true);
        if(target instanceof EntityHitResult hit) {
            hit.getEntity().hurt(magic(hit.getEntity()),getDamageForDisplay(finalPower));
            if(hit.getEntity() instanceof LivingEntity living) {
                int duration=20*getSettingValue("duration"),amplifier=Math.max(0,(int)(getSettingValue("power")*finalPower/2));
                living.addEffect(new MobEffectInstance(MobEffects.POISON,duration,amplifier));float chance=.85f;
                if(world().random.nextFloat()<chance){living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,duration,amplifier));chance-=.15f;}
                if(world().random.nextFloat()<chance){living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,duration,amplifier));chance-=.15f;}
                if(world().random.nextFloat()<chance){living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,duration*2,amplifier));chance-=.15f;}
                if(world().random.nextFloat()<chance){living.addEffect(new MobEffectInstance(MobEffects.HUNGER,duration*3,amplifier));chance-=.15f;}
                if(world().random.nextFloat()<chance)living.addEffect(new MobEffectInstance(MobEffects.UNLUCK,duration*3,amplifier));
            }
        } else if(target instanceof BlockHitResult hit) {
            float radius=Math.min(8,1.5f*getSettingValue("power")*finalPower);
            for(BlockPos cursor:BlockPos.betweenClosed(BlockPos.containing(hit.getBlockPos().getX()-radius,hit.getBlockPos().getY()-radius,hit.getBlockPos().getZ()-radius),BlockPos.containing(hit.getBlockPos().getX()+radius,hit.getBlockPos().getY()+radius,hit.getBlockPos().getZ()+radius))) {
                if(Vec3.atCenterOf(cursor).distanceToSqr(hit.getLocation())<=radius*radius&&world().hasChunkAt(cursor)&&world().isEmptyBlock(cursor.above())&&world().getBlockState(cursor).isCollisionShapeFullBlock(world(),cursor)&&FocusWorldEffects.mayModify(world(),caster(),cursor.above()))
                    world().setBlockAndUpdate(cursor.above(),FocusEffectContent.SAP.get().defaultBlockState());
            }
        }
        return false;
    }
    @Override public void onCast(Entity caster) { castSound(caster,"field_187514_aD",.15f,1+caster.level().random.nextFloat()/2); }
}
