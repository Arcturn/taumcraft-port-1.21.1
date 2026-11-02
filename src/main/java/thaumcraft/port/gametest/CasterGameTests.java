package thaumcraft.port.gametest;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.auromancy.focus.medium.FocusMediumTouch;
import thaumcraft.port.auromancy.focus.effect.FocusEffectAir;
import thaumcraft.port.auromancy.item.*;
import thaumcraft.port.content.*;
import thaumcraft.port.world.AuraSystem;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CasterGameTests {
    private static ItemStack item(String id) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:" + id))); }
    private static ItemStack focus(int complexity, String name) {
        var focus = item("focus_1"); var pack = new FocusPackage(); pack.addNode(new FocusMediumRoot());
        pack.addNode(new FocusMediumTouch()); pack.addNode(new FocusEffectAir()); pack.setComplexity(complexity);
        FocusItem.setPackage(focus, pack); focus.set(DataComponents.CUSTOM_NAME, Component.literal(name)); return focus;
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void capacitiesCostsAndPouchPersistThroughSave(GameTestHelper helper) {
        for (int tier = 1; tier <= 3; tier++) helper.assertValueEqual(((FocusItem)item("focus_" + tier).getItem()).getMaxComplexity(), new int[]{15,25,50}[tier-1], "Focus capacity differs from BETA26");
        var pouch = item("focus_pouch"); var contents = NonNullList.withSize(18, ItemStack.EMPTY); contents.set(17, focus(15, "Last slot"));
        FocusPouchItem.setInventory(pouch, contents);
        var loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag)pouch.save(helper.getLevel().registryAccess()));
        var saved = FocusPouchItem.getInventory(loaded).get(17); var pack = FocusItem.getPackage(saved);
        helper.assertTrue(pack != null && pack.getComplexity() == 15, "Pouch last slot lost its configured focus during save/load");
        helper.assertValueEqual(pack.getVisCost(), 3f, "Focus vis cost must be complexity/5");
        helper.assertValueEqual(pack.getActivationTime(), 9, "Focus cooldown must retain integer division formula");
        helper.assertValueEqual(saved.getHoverName().getString(), "Last slot", "Named focus lost its components"); helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void offhandSwapReturnsOldFocusWithoutTouchingMainhand(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper); var main = item("caster_basic"); var off = item("caster_basic");
        var old = focus(5, "Old off-hand"); var replacement = focus(10, "Replacement"); var untouched = focus(7, "Main-hand");
        CasterItem.setFocus(main, untouched); CasterItem.setFocus(off, old);
        player.setItemInHand(InteractionHand.MAIN_HAND, main); player.setItemInHand(InteractionHand.OFF_HAND, off);
        var pouch = item("focus_pouch"); var contents = NonNullList.withSize(18, ItemStack.EMPTY); contents.set(17, replacement); FocusPouchItem.setInventory(pouch, contents); player.getInventory().setItem(1, pouch);
        String key = ((FocusItem)replacement.getItem()).getSortingHelper(replacement);
        helper.assertTrue(CasterManager.changeFocus(player, InteractionHand.OFF_HAND, key), "Off-hand focus swap failed");
        helper.assertTrue(ItemStack.isSameItemSameComponents(CasterItem.getFocusStack(main), untouched), "Off-hand swap modified main-hand caster");
        helper.assertTrue(ItemStack.isSameItemSameComponents(CasterItem.getFocusStack(off), replacement), "Selected focus was not equipped in off hand");
        helper.assertTrue(FocusPouchItem.getInventory(pouch).stream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, old)), "Previous focus was lost instead of returned to pouch");
        helper.assertTrue(CasterManager.changeFocus(player, InteractionHand.OFF_HAND, "REMOVE"), "Off-hand focus remove failed");
        helper.assertTrue(CasterItem.getFocusStack(off).isEmpty(), "Removed focus remained equipped"); helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fullInventoryRejectsRemovalAndSwapUsesVacatedSlot(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper); var caster = item("caster_basic"); var old = focus(5, "Old"); CasterItem.setFocus(caster, old);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE,64));
        player.setItemInHand(InteractionHand.OFF_HAND, caster);
        helper.assertFalse(CasterManager.changeFocus(player, InteractionHand.OFF_HAND, "REMOVE"), "Full inventory must reject focus removal");
        helper.assertTrue(ItemStack.isSameItemSameComponents(CasterItem.getFocusStack(caster), old), "Rejected removal lost the equipped focus");
        var next = focus(10, "Next"); player.getInventory().setItem(35, next);
        helper.assertTrue(CasterManager.changeFocus(player, InteractionHand.OFF_HAND, ((FocusItem)next.getItem()).getSortingHelper(next)), "Swap must use vacated focus slot when full");
        helper.assertTrue(ItemStack.isSameItemSameComponents(player.getInventory().getItem(35), old), "Full-inventory swap lost or duplicated old focus"); helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void castingUsesOffhandCostAndSharedCooldownWithoutConsumingFocus(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper); var main = item("caster_basic"); var off = item("caster_basic");
        CasterItem.setFocus(main, focus(15,"Main")); CasterItem.setFocus(off, focus(10,"Off"));
        player.setItemInHand(InteractionHand.MAIN_HAND, main); player.setItemInHand(InteractionHand.OFF_HAND, off);
        var aura = AuraSystem.get(helper.getLevel(),player.blockPosition()); aura.addVis(100); float before = aura.vis();
        var offFocus = CasterItem.getFocusStack(off); var cast = (CasterItem)off.getItem();
        helper.assertTrue(cast.use(helper.getLevel(),player,InteractionHand.OFF_HAND).getResult().consumesAction(), "Off-hand focus failed to cast");
        helper.assertTrue(Math.abs(aura.vis() - (before-2)) < .0001, "Off-hand cast paid main-hand focus cost or paid twice");
        helper.assertTrue(ItemStack.isSameItemSameComponents(CasterItem.getFocusStack(off),offFocus), "Casting consumed or changed its equipped focus");
        ((CasterItem)main.getItem()).use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        helper.assertTrue(Math.abs(aura.vis() - (before-2)) < .0001, "Switching hands bypassed shared cooldown");
        CasterManager.setCooldown(player,0); aura.drain(false,aura.vis(),false);
        helper.assertFalse(cast.consumeVis(off,player,2,false,false), "Insufficient aura was accepted");
        helper.assertValueEqual(aura.vis(),0f,"Rejected payment altered aura"); helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void anatomicalMappingAndOffhandPouchLock(GameTestHelper helper) {
        helper.assertValueEqual(CasterHand.arm(HumanoidArm.LEFT, InteractionHand.MAIN_HAND), HumanoidArm.LEFT, "Left-primary main hand rendered on right");
        helper.assertValueEqual(CasterHand.arm(HumanoidArm.LEFT, InteractionHand.OFF_HAND), HumanoidArm.RIGHT, "Left-primary off hand rendered on left");
        helper.assertValueEqual(CasterHand.arm(HumanoidArm.RIGHT, InteractionHand.OFF_HAND), HumanoidArm.LEFT, "Right-primary off hand rendered on right");
        helper.assertValueEqual(CasterHand.armWidth(true),3f,"Slim gauntlet must use three-pixel arm width");
        helper.assertValueEqual(CasterHand.armCenter(true,HumanoidArm.LEFT),.5f,"Slim left arm center is wrong");
        var player=GameTestPlayers.create(helper); var pouch=item("focus_pouch");player.setItemInHand(InteractionHand.OFF_HAND,pouch);
        var menu=new FocusPouchMenu(1,player.getInventory(),InteractionHand.OFF_HAND);
        helper.assertTrue(menu.stillValid(player),"Off-hand pouch cannot open");
        helper.assertValueEqual(menu.lockedSlot(),40,"Off-hand pouch must lock vanilla inventory slot 40");
        player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        helper.assertFalse(menu.stillValid(player),"Menu remained active after its off-hand pouch was removed");helper.succeed();
    }
}
