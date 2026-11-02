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

/** BETA26 exchange focus: source settings, effects and return contract. */
public final class FocusEffectExchange extends SourceFocusEffect implements IFocusBlockPicker {
    @Override public String getResearch() { return "FOCUSEXCHANGE"; }
    @Override public String getKey() { return "thaumcraft.EXCHANGE"; }
    @Override public Aspect getAspect() { return Aspect.EXCHANGE; }
    @Override public int getComplexity() { return 5+getSettingValue("silk")*4+getSettingValue("fortune")==0?0:(getSettingValue("fortune")+1)*3; }
    @Override public NodeSetting[] createSettings() { return new NodeSetting[]{fortune(),silk()}; }
    @Override public boolean execute(HitResult target,Trajectory trajectory,float finalPower,int num) {
        if(world().isClientSide)return false;
        if(target instanceof BlockHitResult hit) {
            if(caster() instanceof ServerPlayer player) {
                var stack=player.getItemInHand(getPackage().getCastingHand());
                var picked=FocusWorldEffects.pickedBlock(stack,player);
                if(!picked.isEmpty()) {
                    boolean silk=getSettingValue("silk")>0;int fortune=getSettingValue("fortune");
                    FocusWorldEffects.queueExchange(player,hit.getBlockPos(),world().getBlockState(hit.getBlockPos()),picked,silk,fortune,.25f+(silk?.25f:0)+fortune*.1f,getPackage().getCastingHand());
                }
            }
            return true;
        }
        return false;
    }
    @Override public void onCast(Entity caster) { castSound(caster,"field_190021_aL",.2f,2+(float)caster.level().random.nextGaussian()*.05f); }
}
