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

/** Shared source plumbing only; damage and world behavior remain per effect. */
public abstract class SourceFocusEffect extends FocusEffect {
    protected final Level world() { return getPackage().world; }
    protected final LivingEntity caster() { return getPackage().getCaster(); }
    protected final void impact(HitResult target) { FocusEffectVisuals.impact(world(),target.getLocation(),getKey()); }
    protected final DamageSource thrown(Entity target) { return new DamageSource(world().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.THROWN),target,caster()); }
    protected final DamageSource magic(Entity target) { return new DamageSource(world().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.INDIRECT_MAGIC),target,caster()); }
    @Override public final void renderParticleFX(Level level,double x,double y,double z,double vx,double vy,double vz) {
        FocusEffectVisuals.particle(getKey(),level,new Vec3(x,y,z),new Vec3(vx,vy,vz));
    }
    protected static NodeSetting range(String key,String label,int min,int max) { return new NodeSetting(key,label,new NodeSetting.NodeSettingIntRange(min,max)); }
    protected static NodeSetting power() { return range("power","focus.common.power",1,5); }
    protected static NodeSetting fortune() { return new NodeSetting("fortune","focus.common.fortune",new NodeSetting.NodeSettingIntList(new int[]{0,1,2,3,4},new String[]{"focus.common.no","I","II","III","IV"})); }
    protected static NodeSetting silk() { return new NodeSetting("silk","focus.common.silk",new NodeSetting.NodeSettingIntList(new int[]{0,1},new String[]{"focus.common.no","focus.common.yes"})); }
    protected static void castSound(Entity entity,String sound,float volume,float pitch) { FocusEffectSounds.play(entity,sound,volume,pitch); }
}
