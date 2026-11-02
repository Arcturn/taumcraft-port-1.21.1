package thaumcraft.port.gametest;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.content.*;
import thaumcraft.port.crafting.content.CraftingContent;
import thaumcraft.port.golem.*;
import thaumcraft.port.golem.builder.*;
import thaumcraft.port.research.ResearchStorage;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class GolemRitualGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=80)
    public static void dustFormsPressFromCauldronAfterResearchAndPersistsFiveParts(GameTestHelper h) {
        var center=blueprint(h);var clicked=GolemBuilderStructure.relative(center,Direction.NORTH,-1,0,0);
        var player=GameTestPlayers.create(h);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PortContent.SALIS_MUNDUS.get(),2));
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(clicked),Direction.UP,clicked,false));
        h.assertFalse(PortContent.SALIS_MUNDUS.get().useOn(context).consumesAction(),"Unknown mindclockwork formed a press");
        h.assertValueEqual(player.getMainHandItem().getCount(),2,"Research rejection consumed dust");
        var knowledge=ResearchStorage.get(player.getServer()).player(player.getUUID());knowledge.stage("MINDCLOCKWORK",1);
        h.assertTrue(PortContent.SALIS_MUNDUS.get().useOn(context).consumesAction(),"Started source research did not enable formation from cauldron");
        h.assertValueEqual(player.getMainHandItem().getCount(),1,"Press did not reserve exactly one dust");
        h.assertFalse(PortContent.SALIS_MUNDUS.get().useOn(context).consumesAction(),"Reserved press accepted duplicate ritual");
        var factory=new SavedData.Factory<>(SalisMundusRituals::new,SalisMundusRituals::load,null);
        var rituals=h.getLevel().getDataStorage().get(factory,"thaumcraft_salis_rituals_v1");
        var registries=h.getLevel().registryAccess();var snapshot=rituals.save(new CompoundTag(),registries);
        h.assertValueEqual(SalisMundusRituals.load(snapshot,registries).save(new CompoundTag(),registries),snapshot,"Five-part pending press was lost on reload");
        h.runAfterDelay(48,()->h.assertTrue(h.getLevel().getBlockState(center).is(Blocks.PISTON),"Press formed before paid ritual deadline"));
        h.runAfterDelay(53,()->{
            h.assertTrue(h.getLevel().getBlockState(center).is(GolemContent.BUILDER.get()),"Press did not form after ritual");
            h.assertTrue(h.getLevel().getBlockState(clicked).is(GolemContent.BUILDER_CAULDRON.get()),"Cauldron incorrectly became a crucible");
            h.assertValueEqual(player.getMainHandItem().getCount(),1,"Completion charged twice");
            h.assertTrue(knowledge.crafted().contains("thaumcraft:golem_builder"),"Press observation was not awarded");h.succeed();
        });
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=80)
    public static void changedPressBlueprintRefundsDustAndLeavesOtherParts(GameTestHelper h) {
        var center=blueprint(h);var player=GameTestPlayers.create(h);
        ResearchStorage.get(player.getServer()).player(player.getUUID()).stage("MINDCLOCKWORK",1);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PortContent.SALIS_MUNDUS.get(),2));
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(center),Direction.UP,center,false));
        h.assertTrue(PortContent.SALIS_MUNDUS.get().useOn(context).consumesAction(),"Fixture did not start press ritual");
        h.getLevel().setBlockAndUpdate(center.above(),Blocks.STONE.defaultBlockState());
        h.runAfterDelay(55,()->{
            h.assertTrue(h.getLevel().getBlockState(center).is(Blocks.PISTON),"Changed blueprint partially formed a press");
            h.assertTrue(h.getLevel().getBlockState(center.above()).is(Blocks.STONE),"Refund reverted an unrelated replacement");
            h.assertValueEqual(player.getInventory().countItem(PortContent.SALIS_MUNDUS.get()),2,"Failed press lost or duplicated dust");h.succeed();
        });
    }
    private static BlockPos blueprint(GameTestHelper h) {
        var center=h.absolutePos(new BlockPos(5,1,5));var level=h.getLevel();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(center.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(center,Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.UP));
        level.setBlockAndUpdate(center.above(),Blocks.IRON_BARS.defaultBlockState());
        level.setBlockAndUpdate(GolemBuilderStructure.relative(center,Direction.NORTH,-1,0,0),Blocks.CAULDRON.defaultBlockState());
        level.setBlockAndUpdate(GolemBuilderStructure.relative(center,Direction.NORTH,-1,0,1),Blocks.ANVIL.defaultBlockState());
        level.setBlockAndUpdate(GolemBuilderStructure.relative(center,Direction.NORTH,0,0,1),CraftingContent.TABLE_STONE.get().defaultBlockState());
        return center;
    }
    private GolemRitualGameTests() {}
}
