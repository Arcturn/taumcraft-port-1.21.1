package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.content.RevealingEquipment;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.machine.WorkbenchEntity;
import thaumcraft.port.machine.WorkbenchMenu;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.AuraSystem;
import thaumcraft.port.world.WorldEntities;
import top.theillusivec4.curios.api.CuriosApi;

/** Real registry, worn slots, datapack recipe and workbench payment paths. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class GogglesGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void originalHelmetStatsRepairAndEnchantments(GameTestHelper helper) {
        var goggles = new ItemStack(PortContent.GOGGLES.get());
        var item = PortContent.GOGGLES.get();
        helper.assertValueEqual(goggles.getMaxDamage(), 350, "Goggles durability differs from TC6");
        helper.assertValueEqual(item.getDefense(), 1, "Goggles must grant one armor point");
        helper.assertValueEqual(item.getToughness(), 1.0f, "Goggles must grant original toughness");
        helper.assertValueEqual(item.getEnchantmentValue(), 25, "Goggles enchantability differs from SPECIAL");
        helper.assertTrue(item.isValidRepairItem(goggles, brass()), "Brass must repair goggles");
        helper.assertFalse(item.isValidRepairItem(goggles, new ItemStack(Items.IRON_INGOT)), "Iron must not repair goggles");
        helper.assertTrue(goggles.is(ItemTags.HEAD_ARMOR), "Goggles need the head armor tags for modern enchantments");
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var key : java.util.List.of(Enchantments.PROTECTION, Enchantments.AQUA_AFFINITY,
                Enchantments.RESPIRATION, Enchantments.UNBREAKING, Enchantments.MENDING)) {
            helper.assertTrue(enchantments.getOrThrow(key).value().canEnchant(goggles), "Goggles cannot receive " + key.location());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void functionalEquipmentAndOriginalHandRules(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper);
        player.getInventory().setItem(9, goggles());
        helper.assertFalse(RevealingEquipment.hasGoggles(player), "Stored goggles must not enable popups");
        player.setItemSlot(EquipmentSlot.OFFHAND, goggles());
        helper.assertFalse(RevealingEquipment.hasGoggles(player), "TC6 popups ignore offhand goggles");
        helper.assertTrue(RevealingEquipment.hasRevealer(player), "The original revealer API includes offhand");
        helper.assertValueEqual(RevealingEquipment.discountedVis(50, player), 50, "Held goggles must not discount vis");
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.MAINHAND, goggles());
        helper.assertTrue(RevealingEquipment.hasGoggles(player), "Mainhand goggles must enable popups");
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.HEAD, goggles());
        helper.assertTrue(RevealingEquipment.hasGoggles(player), "Helmet goggles must enable popups");
        helper.assertValueEqual(RevealingEquipment.discountedVis(50, player), 47, "Original discount truncates 47.5 to 47");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        var head = CuriosApi.getCuriosInventory(player).orElseThrow().getStacksHandler("head").orElseThrow();
        helper.assertTrue(head.getStacks().isItemValid(0, goggles()), "The Curios head slot must accept goggles");
        head.getCosmeticStacks().setStackInSlot(0, goggles());
        helper.assertFalse(RevealingEquipment.hasGoggles(player), "Cosmetic goggles must not enable gameplay");
        helper.assertValueEqual(RevealingEquipment.discountedVis(50, player), 50, "Cosmetic goggles must not discount vis");
        head.getStacks().setStackInSlot(0, goggles());
        helper.assertTrue(RevealingEquipment.hasGoggles(player), "Functional Curios goggles must enable popups");
        helper.assertValueEqual(RevealingEquipment.discountedVis(50, player), 47, "Functional Curios discount is missing");
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        inventory.setSlotActive("head", 0, false);
        helper.assertFalse(RevealingEquipment.hasGoggles(player), "Inactive Curios slots must not enable gameplay");
        helper.assertValueEqual(RevealingEquipment.discountedVis(50, player), 50, "Inactive Curios slots must not discount vis");
        inventory.setSlotActive("head", 0, true);
        player.setItemSlot(EquipmentSlot.HEAD, goggles());
        helper.assertValueEqual(RevealingEquipment.discountedVis(50, player), 45, "Original worn bonuses must add");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void exhaustionUsesTheStrongerEffectWithoutDoubleCharging(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper);
        player.setItemSlot(EquipmentSlot.HEAD, goggles());
        player.addEffect(new MobEffectInstance(WorldEntities.VIS_EXHAUST, 200, 0));
        helper.assertValueEqual(RevealingEquipment.discountedVis(50, player), 52, "First exhaustion tier subtracts ten percent");
        player.addEffect(new MobEffectInstance(WorldEntities.INFECTIOUS, 200, 2));
        helper.assertValueEqual(RevealingEquipment.discountedVis(100, player), 125, "Use strongest exhaustion, do not add both effects");
        player.removeAllEffects();
        helper.assertValueEqual(RevealingEquipment.discountedVis(50, player), 47, "Expired exhaustion must restore discount");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void originalRecipeRequiresResearchAndFiftyVisWithoutCrystals(GameTestHelper helper) {
        var fixture = fixture(helper);
        var player = fixture.player();
        var menu = fixture.menu();
        setVis(helper, fixture.pos(), 100);
        menu.broadcastChanges();
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "Locked research must hide craftable result");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertValueEqual(fixture.table().getItem(0).getCount(), 2, "Locked recipe consumed leather");
        ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID()).flag("UNLOCKARTIFICE");
        menu.broadcastChanges();
        helper.assertValueEqual(menu.visCost(), 50, "Original goggles recipe costs fifty vis");
        helper.assertTrue(menu.getSlot(0).getItem().is(PortContent.GOGGLES.get()), "Original recipe did not resolve");
        for (int i = 0; i < 6; i++) helper.assertValueEqual(menu.crystalCost(i), 0, "Original goggles recipe needs no crystals");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(PortContent.GOGGLES.get()), "Successful craft did not grant goggles");
        helper.assertValueEqual(AuraSystem.get(helper.getLevel(), fixture.pos()).vis(), 50.0f, "Craft must charge exactly fifty vis");
        for (int i : new int[]{0, 1, 2, 3, 5, 7}) helper.assertValueEqual(fixture.table().getItem(i).getCount(), 1, "Ingredient not consumed exactly once: " + i);
        for (int i : new int[]{6, 8}) helper.assertTrue(fixture.table().getItem(i).isEmpty(), "Thaumometer not consumed exactly once: " + i);
        helper.assertTrue(fixture.table().getItem(4).isEmpty(), "Center of goggles recipe must remain empty");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void changingEquipmentAfterPreviewCannotUnderpay(GameTestHelper helper) {
        var fixture = fixture(helper);
        var player = fixture.player();
        var menu = fixture.menu();
        ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID()).flag("UNLOCKARTIFICE");
        player.setItemSlot(EquipmentSlot.HEAD, goggles());
        setVis(helper, fixture.pos(), 47);
        menu.broadcastChanges();
        helper.assertValueEqual(menu.visCost(), 47, "HUD must show the same cost paid by crafting");
        helper.assertTrue(menu.getSlot(0).getItem().is(PortContent.GOGGLES.get()), "Discount should make recipe affordable");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty(), "Removing goggles after preview bypassed vis payment");
        helper.assertValueEqual(AuraSystem.get(helper.getLevel(), fixture.pos()).vis(), 47.0f, "Failed craft consumed vis");
        helper.assertValueEqual(fixture.table().getItem(0).getCount(), 2, "Failed craft consumed items");
        player.setItemSlot(EquipmentSlot.HEAD, goggles());
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(PortContent.GOGGLES.get()), "Reequipping goggles should allow craft");
        helper.assertValueEqual(AuraSystem.get(helper.getLevel(), fixture.pos()).vis(), 0.0f, "Discounted craft did not charge exactly 47 vis");
        helper.succeed();
    }

    private record Fixture(ServerPlayer player, WorkbenchEntity table, WorkbenchMenu menu, BlockPos pos) {}
    private static Fixture fixture(GameTestHelper helper) {
        var relative = new BlockPos(1, 2, 1);
        helper.setBlock(relative, MachineContent.WORKBENCH.get());
        var table = (WorkbenchEntity)helper.getBlockEntity(relative);
        var player = GameTestPlayers.create(helper);
        var pos = helper.absolutePos(relative);
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + 1.5);
        for (int slot : new int[]{0, 2, 3, 5}) table.setItem(slot, new ItemStack(Items.LEATHER, 2));
        for (int slot : new int[]{1, 7}) table.setItem(slot, brass().copyWithCount(2));
        for (int slot : new int[]{6, 8}) table.setItem(slot, new ItemStack(PortContent.THAUMOMETER.get()));
        return new Fixture(player, table, new WorkbenchMenu(1, player.getInventory(), table, pos), pos);
    }
    private static ItemStack goggles() { return new ItemStack(PortContent.GOGGLES.get()); }
    private static ItemStack brass() { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:ingot_brass"))); }
    private static void setVis(GameTestHelper helper, BlockPos pos, int amount) {
        var aura = AuraSystem.get(helper.getLevel(), pos);
        aura.drain(false, Float.MAX_VALUE, false);
        aura.addVis(amount);
    }
}
