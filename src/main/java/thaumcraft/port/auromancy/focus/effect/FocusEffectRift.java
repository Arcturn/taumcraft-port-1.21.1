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

/** BETA26 rift focus: source settings, effects and return contract. */
public final class FocusEffectRift extends SourceFocusEffect {
    @Override public String getResearch() { return "FOCUSRIFT"; }
    @Override public String getKey() { return "thaumcraft.RIFT"; }
    @Override public Aspect getAspect() { return Aspect.ELDRITCH; }
    @Override public int getComplexity() { return 3+getSettingValue("duration")/2+getSettingValue("depth")/4; }
    @Override public NodeSetting[] createSettings() { return new NodeSetting[]{new NodeSetting("depth","focus.rift.depth",new NodeSetting.NodeSettingIntList(new int[]{8,16,24,32},new String[]{"8","16","24","32"})),range("duration","focus.common.duration",2,10)}; }
    @Override public boolean execute(HitResult target,Trajectory trajectory,float finalPower,int num) {
        if(!(world() instanceof ServerLevel level)||!(target instanceof BlockHitResult hit))return false;
        if(level.dimension().location().equals(net.minecraft.resources.ResourceLocation.parse("thaumcraft:outer"))) {
            level.playSound(null,hit.getBlockPos(),PortSounds.get("wandfail"),SoundSource.PLAYERS,1,1);return false;
        }
        BlockPos pos=hit.getBlockPos();int distance=0;float maximum=getSettingValue("depth")*finalPower;
        while(distance<maximum&&level.hasChunkAt(pos)) {
            var state=level.getBlockState(pos);
            if(FocusWorldEffects.holeBlacklisted(state)||state.isAir()||state.is(FocusEffectContent.HOLE.get())||state.getDestroySpeed(level,pos)<0)break;
            distance++;pos=pos.relative(hit.getDirection().getOpposite());
        }
        FocusWorldEffects.createHole(level,hit.getBlockPos(),hit.getDirection(),Math.round(distance+1),20*getSettingValue("duration"),caster());
        return true;
    }
    @Override public void onCast(Entity caster) { castSound(caster,"field_190021_aL",.2f,.7f); }
}
