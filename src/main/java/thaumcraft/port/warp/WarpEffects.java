package thaumcraft.port.warp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Original warp status effects, selective food treatment, and the involuntary death gaze. */
public final class WarpEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "thaumcraft");
    public static final DeferredHolder<MobEffect, MobEffect> THAUMARHIA = effect("thaumarhia", WarpStatusEffect.Kind.THAUMARHIA, 0x664477, "potion.thaumarhia");
    public static final DeferredHolder<MobEffect, MobEffect> UNNATURAL_HUNGER = effect("unnatural_hunger", WarpStatusEffect.Kind.UNNATURAL_HUNGER, 0x446633, "potion.unhunger");
    public static final DeferredHolder<MobEffect, MobEffect> BLURRED_VISION = effect("blurred_vision", WarpStatusEffect.Kind.BLURRED_VISION, 0x808080, "potion.blurred");
    public static final DeferredHolder<MobEffect, MobEffect> SUN_SCORNED = effect("sun_scorned", WarpStatusEffect.Kind.SUN_SCORNED, 16308330, "potion.sunscorned");
    public static final DeferredHolder<MobEffect, MobEffect> DEATH_GAZE = effect("death_gaze", WarpStatusEffect.Kind.DEATH_GAZE, 0x664433, "potion.deathgaze");

    private static DeferredHolder<MobEffect, MobEffect> effect(String name, WarpStatusEffect.Kind kind, int color, String translation) {
        return EFFECTS.register(name, () -> new WarpStatusEffect(kind, color, translation));
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
        NeoForge.EVENT_BUS.addListener(WarpEffects::finishedUsingItem);
        NeoForge.EVENT_BUS.addListener(WarpEffects::tick);
    }

    /**
     * TC6 clears the particular warp event's curative items, not the potion's defaults.
     * Vanilla 1.12 totems removed all effects independently of those curative items; preserve
     * that behavior with NeoForge's separate totem cure. Secondary infections keep default cures.
     */
    public static MobEffectInstance uncurable(MobEffectInstance instance) {
        instance.getCures().removeIf(cure -> cure != EffectCures.PROTECTED_BY_TOTEM);
        return instance;
    }

    private static void finishedUsingItem(LivingEntityUseItemEvent.Finish event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide) return;
        var hunger = entity.getEffect(UNNATURAL_HUNGER);
        if (hunger == null) return;
        var consumed = event.getItem();
        if (consumed.is(Items.ROTTEN_FLESH) || consumed.getItem() instanceof ZombieBrainItem) {
            int amplifier = hunger.getAmplifier() - 1;
            int duration = hunger.getDuration() - 600;
            entity.removeEffect(UNNATURAL_HUNGER);
            if (duration > 0 && amplifier >= 0)
                entity.addEffect(uncurable(new MobEffectInstance(UNNATURAL_HUNGER, duration, amplifier, true, false)));
            entity.sendSystemMessage(Component.translatable("warp.text.hunger.2").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        } else if (consumed.getFoodProperties(entity) != null)
            entity.sendSystemMessage(Component.translatable("warp.text.hunger.1").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }

    private static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) checkDeathGaze(player);
    }

    /** BETA26 WarpEvents.checkDeathGaze, including the original narrow cone and PvP check. */
    public static void checkDeathGaze(ServerPlayer player) {
        var effect = player.getEffect(DEATH_GAZE);
        if (effect == null) return;
        int range = Math.min(8 + effect.getAmplifier() * 3, 24);
        Vec3 look = player.getViewVector(1);
        Vec3 eye = player.getEyePosition();
        for (var entity : player.level().getEntities(player, player.getBoundingBox().inflate(range))) {
            if (!entity.isPickable() || !(entity instanceof LivingEntity living) || !living.isAlive()
                    || !insideGaze(eye, look, living, range) || !player.hasLineOfSight(living)
                    || living instanceof Player && !player.getServer().isPvpAllowed() || living.hasEffect(MobEffects.WITHER)) continue;
            living.setLastHurtByMob(player);
            living.setLastHurtMob(player);
            if (living instanceof PathfinderMob creature) creature.setTarget(player);
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 80));
        }
    }

    private static boolean insideGaze(Vec3 eye, Vec3 look, LivingEntity target, int range) {
        Vec3 offset = new Vec3(target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2, target.getZ()).subtract(eye);
        // Utils.isLyingInCone uses the target's body centre, aperture .75 radians, and an axial cap.
        double along = offset.dot(look);
        return along / offset.length() > Math.cos(.75f / 2) && along < range;
    }

    private WarpEffects() { }
}
