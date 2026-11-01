package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.crafting.content.ArcaneLampBlock;
import thaumcraft.port.crafting.content.CraftingContent;
import thaumcraft.port.crafting.content.HungryChestEntity;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CraftingContentGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void hungryChestConservesPartialPickupAndPersistsInventory(GameTestHelper helper) {
        var chest = chest(helper);
        for (int slot = 0; slot < 26; slot++) chest.setItem(slot, new ItemStack(Items.STONE, 64));
        chest.setItem(26, new ItemStack(Items.DIAMOND, 62));
        var pos = chest.getBlockPos();
        var dropped = new ItemEntity(helper.getLevel(), pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5,
                new ItemStack(Items.DIAMOND, 5));
        chest.collect(dropped);
        helper.assertValueEqual(chest.getItem(26).getCount(), 64, "Partial pickup did not fill the last slot");
        helper.assertTrue(dropped.isAlive(), "A full chest deleted the rejected remainder");
        helper.assertValueEqual(dropped.getItem().getCount(), 3, "Partial pickup changed the item total");
        chest.collect(dropped);
        helper.assertValueEqual(dropped.getItem().getCount(), 3, "Full inventory consumed a rejected stack");

        var saved = chest.saveWithoutMetadata(helper.getLevel().registryAccess());
        var restored = new HungryChestEntity(pos, chest.getBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertValueEqual(restored.getContainerSize(), 27, "Hungry Chest must have 27 slots");
        helper.assertValueEqual(restored.getItem(26).getCount(), 64, "Save/reload lost the last inventory slot");
        helper.assertTrue(restored.getItem(0).is(Items.STONE), "Save/reload changed item identities");

        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
        helper.assertTrue(handler != null, "Hoppers cannot access the Hungry Chest inventory");
        handler.extractItem(26, 3, false);
        chest.collect(dropped);
        helper.assertFalse(dropped.isAlive(), "Fully accepted dropped stack was not removed");
        helper.assertValueEqual(chest.getItem(26).getCount(), 64, "Second pickup did not preserve counts");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void hungryChestCollectsItemsLandingOnItsLid(GameTestHelper helper) {
        var chest = chest(helper);
        var pos = chest.getBlockPos();
        var dropped = new ItemEntity(helper.getLevel(), pos.getX() + .5, pos.getY() + 1.2, pos.getZ() + .5,
                new ItemStack(Items.APPLE, 4));
        dropped.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(dropped);
        helper.succeedWhen(() -> {
            helper.assertTrue(chest.getItem(0).is(Items.APPLE), "An item resting on the lid was not collected");
            helper.assertValueEqual(chest.getItem(0).getCount(), 4, "Collision pickup did not conserve the stack");
            helper.assertFalse(dropped.isAlive(), "Collision pickup left a duplicate item entity");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void arcaneLampUsesOriginalRedstoneOffSwitch(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlock(pos, CraftingContent.LAMP_ARCANE.get().defaultBlockState(), 3);
        helper.assertValueEqual(helper.getLevel().getBlockState(pos).getLightEmission(helper.getLevel(), pos), 15,
                "Unpowered arcane lamp must provide full light");
        helper.getLevel().setBlock(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        helper.assertFalse(helper.getLevel().getBlockState(pos).getValue(ArcaneLampBlock.LIT), "Redstone failed to disable the lamp");
        helper.getLevel().removeBlock(pos.east(), false);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(ArcaneLampBlock.LIT), "Lamp did not resume after removing redstone");
        helper.succeed();
    }

    private static HungryChestEntity chest(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlock(pos, CraftingContent.HUNGRY_CHEST.get().defaultBlockState(), 3);
        return (HungryChestEntity) helper.getLevel().getBlockEntity(pos);
    }
}
