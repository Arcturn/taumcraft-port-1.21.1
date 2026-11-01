package thaumcraft.port.gametest;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.content.RevealingEquipment;
import thaumcraft.port.infusion.armor.InfusionEquipmentEvents;
import thaumcraft.port.infusion.bauble.VerdantCharmItem;
import thaumcraft.port.infusion.content.InfusionCraftingContent;
import thaumcraft.port.infusion.recipe.InfusionAugments;
import thaumcraft.port.infusion.structure.ItemCharge;
import thaumcraft.port.world.AuraSystem;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

/** Source ItemVoidArmor/ItemVoidRobeArmor/ItemPrimalCrusher and crafted ItemAmuletVis behavior. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class InfusionCraftingContentGameTests {
    @GameTest(template="empty",timeoutTicks=40)
    public static void voidEquipmentRepairsAtOriginalCadenceAndCrusherKeepsBothToolAbilities(GameTestHelper h){
        var player=GameTestPlayers.create(h);
        var armor=List.of(InfusionCraftingContent.VOID_HELM.get(),InfusionCraftingContent.VOID_CHEST.get(),InfusionCraftingContent.VOID_LEGS.get(),InfusionCraftingContent.VOID_ROBE_HELM.get(),InfusionCraftingContent.VOID_ROBE_CHEST.get(),InfusionCraftingContent.VOID_ROBE_LEGS.get());
        int[] durability={110,160,150,198,288,270},defense={3,8,6,4,9,7};
        var ingot=stack("ingot_void");
        for(int i=0;i<armor.size();i++){
            var item=armor.get(i);var gear=new ItemStack(item);
            h.assertValueEqual(gear.getMaxDamage(),durability[i],"Original void armor durability changed");
            h.assertValueEqual(item.getDefense(),defense[i],"Original void armor defense changed");
            h.assertTrue(item.isValidRepairItem(gear,ingot),"Void armor no longer repairs with void metal");verifyRepair(h,player,gear);
        }
        var pick=new ItemStack(InfusionCraftingContent.VOID_PICK.get());var shovel=new ItemStack(InfusionCraftingContent.VOID_SHOVEL.get());var crusher=new ItemStack(InfusionCraftingContent.PRIMAL_CRUSHER.get());
        h.assertValueEqual(pick.getMaxDamage(),150,"Original void pick durability changed");h.assertValueEqual(shovel.getMaxDamage(),150,"Original void shovel durability changed");h.assertValueEqual(crusher.getMaxDamage(),500,"Original primal crusher durability changed");
        for(var tool:List.of(pick,shovel,crusher)){h.assertTrue(tool.getItem().isValidRepairItem(tool,ingot),"Void tool cannot repair with void metal");verifyRepair(h,player,tool);}
        h.assertTrue(crusher.canPerformAction(ItemAbilities.PICKAXE_DIG)&&crusher.canPerformAction(ItemAbilities.SHOVEL_DIG),"Primal crusher lost its original pickaxe/shovel tool classes");
        h.assertValueEqual(InfusionAugments.getEnchantment(crusher,"destructive"),1,"Crusher lost inherent destructive infusion");h.assertValueEqual(InfusionAugments.getEnchantment(crusher,"refining"),1,"Crusher lost inherent refining infusion");
        var helmet=new ItemStack(InfusionCraftingContent.VOID_ROBE_HELM.get());player.setItemSlot(EquipmentSlot.HEAD,helmet);player.setItemSlot(EquipmentSlot.MAINHAND,crusher);player.setItemSlot(EquipmentSlot.OFFHAND,pick);
        h.assertTrue(RevealingEquipment.hasGoggles(player),"Void robe helmet no longer reveals aspects");h.assertValueEqual(RevealingEquipment.discountedVis(100,player),95,"Void robe helmet must save five percent vis");
        h.assertValueEqual(InfusionEquipmentEvents.gearWarp(player),5,"Void robe and crusher intrinsic warp must be three plus two, excluding offhand");
        InfusionAugments.setWarp(helmet,2);h.assertValueEqual(InfusionEquipmentEvents.gearWarp(player),7,"Added infusion warp must coexist with intrinsic void warp");h.succeed();
    }
    private static void verifyRepair(GameTestHelper h,net.minecraft.server.level.ServerPlayer player,ItemStack gear){
        gear.setDamageValue(17);player.tickCount=19;gear.getItem().inventoryTick(gear,h.getLevel(),player,0,false);
        h.assertValueEqual(gear.getDamageValue(),17,"Void equipment repaired before its twentieth tick");
        player.tickCount=20;gear.getItem().inventoryTick(gear,h.getLevel(),player,0,false);
        h.assertValueEqual(gear.getDamageValue(),16,"Void equipment must repair exactly one durability per second");
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void craftedVisAmuletChargesOneItemEveryFiveTicksInOriginalPriorityOrder(GameTestHelper h){
        var player=GameTestPlayers.create(h);player.getInventory().clearContent();
        var first=VerdantCharmItem.stack(0);var second=VerdantCharmItem.stack(0);var backpack=VerdantCharmItem.stack(0);var worn=VerdantCharmItem.stack(0);var cosmetic=VerdantCharmItem.stack(0);var boots=stack("traveller_boots");
        player.getInventory().setItem(0,first);player.getInventory().setItem(1,second);player.getInventory().setItem(9,backpack);player.setItemSlot(EquipmentSlot.FEET,boots);
        var curios=CuriosApi.getCuriosInventory(player).orElseThrow();var charms=curios.getStacksHandler("charm").orElseThrow();charms.getStacks().setStackInSlot(0,worn);charms.getCosmeticStacks().setStackInSlot(0,cosmetic);
        var aura=AuraSystem.get(h.getLevel(),player.blockPosition());aura.drain(false,Float.MAX_VALUE,false);aura.addVis(20);
        var amulet=InfusionCraftingContent.AMULET_VIS.get();var stack=new ItemStack(amulet);var context=new SlotContext("necklace",player,0,false,true);
        player.tickCount=4;amulet.curioTick(context,stack);h.assertValueEqual(aura.vis(),20f,"Crafted amulet recharged before its fifth tick");
        player.tickCount=5;amulet.curioTick(context,stack);
        h.assertValueEqual(ItemCharge.get(first),1,"Amulet did not prioritize first hotbar item");h.assertValueEqual(ItemCharge.get(second),0,"Amulet charged more than one item per interval");h.assertValueEqual(ItemCharge.get(worn),0,"Amulet skipped hotbar priority");h.assertValueEqual(aura.vis(),19f,"One charge must consume exactly one aura vis");
        ItemCharge.add(first,player,200);ItemCharge.add(second,player,200);player.tickCount=10;amulet.curioTick(context,stack);
        h.assertValueEqual(ItemCharge.get(worn),1,"Amulet did not continue from full hotbar to functional Curios");h.assertValueEqual(ItemCharge.get(boots),0,"Amulet skipped Curios priority");
        curios.setSlotActive("charm",0,false);player.tickCount=15;amulet.curioTick(context,stack);
        h.assertValueEqual(ItemCharge.get(boots),1,"Amulet did not continue to worn armor after disabled Curios");h.assertValueEqual(ItemCharge.get(worn),1,"Disabled Curios received charge");
        player.tickCount=20;amulet.curioTick(new SlotContext("necklace",player,0,true,true),stack);
        h.assertValueEqual(aura.vis(),17f,"Cosmetic amulet activated or a charge spent extra aura");h.assertValueEqual(ItemCharge.get(backpack),0,"Amulet charged outside the original nine hotbar slots");h.assertValueEqual(ItemCharge.get(cosmetic),0,"Amulet charged cosmetic Curios");h.succeed();
    }
    private static ItemStack stack(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:"+id)));}
    private InfusionCraftingContentGameTests(){}
}
