package thaumcraft.port.alchemy.liquid;

import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.port.aspect.AspectTags;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.research.ResearchProgress;
import thaumcraft.port.research.ResearchStorage;

@EventBusSubscriber(modid = "thaumcraft")
public final class AlchemyLiquidEvents {
    @SubscribeEvent public static void expire(ItemExpireEvent event) {
        var entity = event.getEntity();
        if (!entity.getItem().is(AlchemyLiquids.BATH_SALTS.get())) return;
        var pos = entity.blockPosition();
        var state = entity.level().getBlockState(pos);
        // Original salts dissolve at their ten-second expiry, only in an actual water source.
        // Waterlogged blocks and flowing water are intentionally not converted.
        if (state.is(Blocks.WATER) && state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource())
            entity.level().setBlockAndUpdate(pos, AlchemyLiquids.PURIFYING_BATH_BLOCK.get().defaultBlockState());
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        if (!event.getSource().is(AlchemyLiquids.DISSOLVE)) return;
        var entity = event.getEntity();
        var aspects = AspectTags.forEntity(entity);
        if (aspects.isEmpty()) return;
        var choices = new ArrayList<>(aspects.keySet());
        int amount = 1 + entity.getRandom().nextInt(1 + aspects.values().stream().mapToInt(Integer::intValue).sum() / 10);
        for (int i = 0; i < amount; i++)
            event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getEyeY(), entity.getZ(),
                    CrystalItem.stack(choices.get(entity.getRandom().nextInt(choices.size())))));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount <= 0
                || player.tickCount % 2000 != 0 || player.hasEffect(AlchemyLiquids.WARP_WARD)) return;
        checkWarp(player, player.getRandom().nextInt(100));
    }
    /** The warp-event eligibility needed by hidden alchemy; unrelated eldritch event effects live elsewhere. */
    public static boolean checkWarp(ServerPlayer player, int roll) {
        if (player.hasEffect(AlchemyLiquids.WARP_WARD)) return false;
        var storage = ResearchStorage.get(player.getServer());
        var state = storage.player(player.getUUID());
        int temporary = state.temporaryWarp();
        state.warp(state.permanentWarp(), state.normalWarp(), Math.max(0, temporary - 1));
        boolean changed = temporary != state.temporaryWarp();
        int total = state.permanentWarp() + state.normalWarp() + state.temporaryWarp();
        boolean occurred = AlchemyRules.warpEvent(state.warpCounter(), total, roll);
        if (occurred) {
            state.warpCounter(AlchemyRules.remainingCounter(state.warpCounter(), 0));
            changed = true;
            if (state.permanentWarp() + state.normalWarp() > 10 && !state.knows("BATHSALTS") && !state.knows("!BATHSALTS")) {
                state.flag("!BATHSALTS");
                player.displayClientMessage(Component.translatable("warp.text.8")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
            }
        }
        if (changed) { storage.setDirty(); ResearchProgress.sync(player); }
        return occurred;
    }
    private AlchemyLiquidEvents() {}
}
