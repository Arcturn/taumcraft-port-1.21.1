package thaumcraft.port.warp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.port.alchemy.liquid.AlchemyLiquids;
import thaumcraft.port.infusion.armor.InfusionEquipmentEvents;
import thaumcraft.port.research.ResearchProgress;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.WorldEntities;

/** Server-side BETA26 warp events. Each player's temporary warp decays once per eligible check. */
@EventBusSubscriber(modid = "thaumcraft")
public final class WarpEvents {
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !WarpRules.isCheckDue(player.tickCount, WarpConfig.WUSS_MODE.get(), player.hasEffect(AlchemyLiquids.WARP_WARD))) return;
        check(player, player.getRandom().nextInt(100));
    }

    /** A check with an explicit 0..99 chance roll; retained as a deterministic integration boundary. */
    public static boolean check(ServerPlayer player, int chanceRoll) {
        if (WarpConfig.WUSS_MODE.get() || player.hasEffect(AlchemyLiquids.WARP_WARD)) return false;
        var storage = ResearchStorage.get(player.getServer());
        var state = storage.player(player.getUUID());
        int temporary = state.temporaryWarp();
        state.warp(state.permanentWarp(), state.normalWarp(), Math.max(0, temporary - 1));
        boolean changed = temporary != state.temporaryWarp();
        // Capture before the clarity event: original unlock thresholds include the point it removes.
        int actualWarp = state.permanentWarp() + state.normalWarp();
        int gearWarp = InfusionEquipmentEvents.gearWarp(player);
        int total = actualWarp + state.temporaryWarp() + gearWarp;
        int counter = state.warpCounter();
        boolean occurred = WarpRules.canTrigger(counter, total, chanceRoll);
        if (occurred) {
            int effectiveWarp = WarpRules.severityRange(total, counter);
            state.warpCounter(WarpRules.remainingCounter(counter, gearWarp));
            int severity = InfusionEquipmentEvents.mitigateWarpSeverity(player,
                    player.getRandom().nextInt(effectiveWarp) + gearWarp);
            WarpPayload.send(player, 0);
            applyEvent(player, severity, effectiveWarp);
            changed = true;
            if (actualWarp > 10 && !state.knows("BATHSALTS") && !state.knows("!BATHSALTS")) {
                ResearchProgress.completeResearch(player, "!BATHSALTS");
                message(player, 8);
            }
            if (actualWarp > 25 && !state.knows("ELDRITCHMINOR"))
                ResearchProgress.completeResearch(player, "ELDRITCHMINOR");
            if (actualWarp > 50 && !state.knows("ELDRITCHMAJOR"))
                ResearchProgress.completeResearch(player, "ELDRITCHMAJOR");
        }
        if (changed) {
            storage.setDirty();
            ResearchProgress.sync(player);
        }
        return occurred;
    }

    /** Execute an explicitly selected event, without periodic eligibility, counter decay or research unlocks. */
    public static void dispatch(ServerPlayer player, int severity, int effectiveWarp) {
        if (applyEvent(player, severity, effectiveWarp)) {
            ResearchStorage.get(player.getServer()).setDirty();
            ResearchProgress.sync(player);
        }
    }

    /** Returns whether the event itself changed persisted warp; ordinary checks sync the entire batch. */
    private static boolean applyEvent(ServerPlayer player, int severity, int effectiveWarp) {
        int warp = Math.clamp(effectiveWarp, 1, 100);
        int amplifier = Math.min(3, warp / 15);
        switch (WarpRules.eventForSeverity(severity)) {
            case NONE -> { }
            case CREEPER -> WarpPayload.send(player, 3);
            case EXPLOSION -> WarpPayload.send(player, 4);
            case UNEASE -> message(player, 11);
            case VIS_EXHAUST -> {
                effect(player, WorldEntities.VIS_EXHAUST, 5000, amplifier, true, true);
                message(player, 1);
            }
            case THAUMARHIA -> {
                effect(player, WarpEffects.THAUMARHIA, Math.min(32000, 10 * warp), 0, true, true);
                message(player, 15);
            }
            case HUNGER -> {
                effect(player, WarpEffects.UNNATURAL_HUNGER, 5000, amplifier, true, true);
                message(player, 2);
            }
            case WHISPERS -> message(player, 12);
            case MIST_SINGLE -> spawnMist(player, 1);
            case BLURRED_VISION -> effect(player, WarpEffects.BLURRED_VISION, Math.min(32000, 10 * warp), 0, true, false);
            case SUN_SCORNED -> {
                effect(player, WarpEffects.SUN_SCORNED, 5000, amplifier, true, true);
                message(player, 5);
            }
            case MINING_FATIGUE -> {
                effect(player, MobEffects.DIG_SLOWDOWN, 1200, amplifier, true, false);
                message(player, 9);
            }
            case INFECTIOUS -> {
                // Unlike the other warp potions, BETA26 uses the non-ambient constructor here.
                effect(player, WorldEntities.INFECTIOUS, 6000, amplifier, false, true);
                message(player, 1);
            }
            case NIGHT_VISION -> {
                effect(player, MobEffects.NIGHT_VISION, Math.min(40 * warp, 6000), 0, true, false);
                message(player, 10);
            }
            case DEATH_GAZE -> {
                effect(player, WarpEffects.DEATH_GAZE, 6000, amplifier, true, true);
                message(player, 4);
            }
            case SPIDERS -> WarpSpawns.suddenlySpiders(player, warp, false);
            case STARES -> message(player, 13);
            case MIST_MEDIUM -> spawnMist(player, warp / 30);
            case BLINDNESS -> effect(player, MobEffects.BLINDNESS, Math.min(32000, 5 * warp), 0, true, false);
            case CLARITY -> {
                var state = ResearchStorage.get(player.getServer()).player(player.getUUID());
                boolean changed = state.normalWarp() > 0;
                if (changed) state.warp(state.permanentWarp(), state.normalWarp() - 1, state.temporaryWarp());
                message(player, 14);
                return changed;
            }
            case HUNGER_LONG -> {
                effect(player, WarpEffects.UNNATURAL_HUNGER, 6000, amplifier, true, true);
                message(player, 2);
            }
            case PORTAL -> WarpSpawns.spawnPortal(player);
            case REAL_SPIDERS -> WarpSpawns.suddenlySpiders(player, warp, true);
            case MIST_STRONG -> spawnMist(player, warp / 15);
        }
        return false;
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier,
                               boolean ambient, boolean uncurable) {
        var instance = new MobEffectInstance(effect, duration, amplifier, ambient, true);
        player.addEffect(uncurable ? WarpEffects.uncurable(instance) : instance);
    }

    private static void spawnMist(ServerPlayer player, int guardians) {
        WarpPayload.send(player, 1);
        for (int i = 0; i < Math.min(8, guardians); i++) WarpSpawns.spawnGuardian(player);
        message(player, 6);
    }

    private static void message(ServerPlayer player, int number) {
        player.displayClientMessage(Component.translatable("warp.text." + number)
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
    }

    private WarpEvents() {}
}
