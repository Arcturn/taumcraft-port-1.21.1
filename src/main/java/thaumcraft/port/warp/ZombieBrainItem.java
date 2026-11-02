package thaumcraft.port.warp;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.port.research.ResearchProgress;
import thaumcraft.port.research.ResearchStorage;

/** Zombie brains are food, research ingredients, and a source of normal or temporary warp. */
public final class ZombieBrainItem extends Item {
    public ZombieBrainItem(Properties properties) {
        // BETA26 overrides ItemFood.onFoodEaten without calling super: its configured Hunger
        // potion is never actually applied. Retain that observed behavior, not the dead setting.
        super(properties.food(new FoodProperties.Builder().nutrition(4).saturationModifier(.2f).build()));
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        // ItemFood in 1.12 consumes one item even when the player has creative abilities.
        if (entity instanceof Player player && player.hasInfiniteMaterials()) result.shrink(1);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            var storage = ResearchStorage.get(player.getServer());
            var state = storage.player(player.getUUID());
            if (level.random.nextFloat() < .1f) state.warp(state.permanentWarp(), state.normalWarp() + 1, state.temporaryWarp());
            else state.warp(state.permanentWarp(), state.normalWarp(), state.temporaryWarp() + 1 + level.random.nextInt(3));
            storage.setDirty();
            ResearchProgress.sync(player);
        }
        return result;
    }
}
