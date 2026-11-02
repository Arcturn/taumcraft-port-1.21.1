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

/** BETA26 break focus: source settings, effects and return contract. */
public final class FocusEffectBreak extends SourceFocusEffect {
    @Override public String getResearch() { return "FOCUSBREAK"; }
    @Override public String getKey() { return "thaumcraft.BREAK"; }
    @Override public Aspect getAspect() { return Aspect.ENTROPY; }
    @Override public int getComplexity() { return getSettingValue("power")*3+getSettingValue("silk")*4+(getSettingValue("fortune")==0?0:(getSettingValue("fortune")+1)*3); }
    @Override public NodeSetting[] createSettings() { return new NodeSetting[]{range("power","focus.break.power",1,5),fortune(),silk()}; }
    @Override public boolean execute(HitResult target,Trajectory trajectory,float finalPower,int num) {
        if(world().isClientSide)return true;
        if(target instanceof BlockHitResult hit&&caster() instanceof ServerPlayer player) {
            BlockPos pos=hit.getBlockPos();var state=world().getBlockState(pos);FocusEffectVisuals.impact(world(),Vec3.atCenterOf(pos),getKey());
            float strength=getSettingValue("power")*finalPower,hardness=state.getDestroySpeed(world(),pos);
            if(strength>0&&hardness>=0) {
                float durability=(float)Math.sqrt(hardness*100);boolean silk=getSettingValue("silk")>0;int fortune=getSettingValue("fortune");
                FocusWorldEffects.queueBreak(player,pos,state,true,silk,fortune,strength,durability,durability,(int)(durability/strength/3*num),.25f+(silk?.25f:0)+fortune*.1f,getPackage().getCastingHand());
            }
        }
        return true;
    }
    @Override public void onCast(Entity caster) { castSound(caster,"field_187598_bd",.1f,2+(float)caster.level().random.nextGaussian()*.05f); }
}
