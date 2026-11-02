package thaumcraft.port.alchemy.liquid;

import java.util.ArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import thaumcraft.port.aspect.AspectTags;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.warp.WarpEvents;

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
    /** Compatibility entry point; the sole periodic listener now belongs to WarpEvents. */
    public static boolean checkWarp(ServerPlayer player, int roll) {
        return WarpEvents.check(player, roll);
    }
    private AlchemyLiquidEvents() {}
}
