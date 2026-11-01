package thaumcraft.port.gametest;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.*;
import thaumcraft.port.world.AuraSystem;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CondenserGameTests {
    private static final BlockPos BASE=new BlockPos(10,2,10);

    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void latticeCostAndCadenceFollowOriginalWeightedGraph(GameTestHelper helper){
        var be=fixture(helper);lattice(helper,be.getBlockPos().above(),false);be.triggerCheck();
        helper.assertTrue(Math.abs(be.latticeEfficiency()-.85f)<.0001,"Single lattice weighting differs from TC6");
        helper.assertValueEqual(be.cycleTicks(),587,"Single-lattice cycle must take 587 ticks");helper.assertValueEqual(be.essentiaCost(),5,"Single-lattice cost must be five essentia");
        lattice(helper,be.getBlockPos().above(2),false);be.triggerCheck();
        // The condenser below is not a lattice neighbour: both counted nodes have one lattice side.
        helper.assertTrue(Math.abs(be.latticeEfficiency()-1.7f)<.0001,"Two-block column did not apply its connectivity penalty");
        helper.assertValueEqual(be.cycleTicks(),575,"Column cycle time differs from original");
        lattice(helper,be.getBlockPos().above(3),false);lattice(helper,be.getBlockPos().above(4),false);be.triggerCheck();
        helper.assertValueEqual(be.latticeBlocks(),4,"Column traversal lost a lattice block");helper.assertValueEqual(be.essentiaCost(),6,"Four blocks did not raise sqrt-based cost to six");
        helper.succeed();
    }

    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void oneCycleConsumesFuelAndExactlyOneAuraFluxToProduceVitium(GameTestHelper helper){
        var be=fixture(helper);lattice(helper,be.getBlockPos().above(),false);be.triggerCheck();
        var aura=AuraSystem.get(helper.getLevel(),be.getBlockPos());AuraSystem.add(helper.getLevel(),be.getBlockPos(),10,true);float before=aura.flux();
        helper.assertValueEqual(be.insert(Aspect.AIR,5,Direction.NORTH,false),5,"Condenser rejected non-Flux essentia");
        int interval=be.cycleTicks();for(int i=1;i<interval;i++)be.tick();
        helper.assertValueEqual(be.storedFlux(),0,"Condenser completed before its cycle deadline");be.tick();
        helper.assertValueEqual(be.storedFlux(),1,"Condenser did not produce one Vitium");helper.assertValueEqual(be.storedEssentia(),0,"Cycle did not spend exact fuel cost");
        helper.assertTrue(Math.abs(aura.flux()-before+1)<.001,"Condenser did not remove exactly one aura flux");
        helper.assertValueEqual(be.extract(Aspect.FLUX,1,Direction.EAST,false),0,"Vitium escaped through a side");
        helper.assertValueEqual(be.extract(Aspect.AIR,1,Direction.DOWN,false),0,"Output claimed an incorrect aspect");
        helper.assertValueEqual(be.extract(Aspect.FLUX,1,Direction.DOWN,true),1,"Bottom output could not be simulated");
        helper.assertValueEqual(be.storedFlux(),1,"Output simulation consumed Vitium");
        helper.assertValueEqual(be.extract(Aspect.FLUX,1,Direction.DOWN,false),1,"Bottom output failed");helper.succeed();
    }

    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void sideSupplyPullsOnFifthTickAndFluxSupplyClogsWithoutBeingConsumed(GameTestHelper helper){
        var be=fixture(helper);var root=be.getBlockPos().above();lattice(helper,root,false);
        var sourcePos=be.getBlockPos().east();helper.getLevel().setBlock(sourcePos,EssentiaContent.ALEMBIC.get().defaultBlockState(),3);
        var source=(JarEntity)helper.getLevel().getBlockEntity(sourcePos);source.fill(Aspect.AIR,10,false);
        for(int i=0;i<4;i++)be.tick();helper.assertValueEqual(be.storedEssentia(),0,"Condenser pulled before tick five");be.tick();
        helper.assertValueEqual(be.storedEssentia(),1,"Condenser did not pull one essentia from the side");helper.assertValueEqual(source.tank.amount(),9,"Side transfer did not conserve essentia");
        source.tank.restore(Aspect.FLUX,10,null);for(int i=0;i<5;i++)be.tick();
        helper.assertTrue(helper.getLevel().getBlockState(root).is(CondenserContent.DIRTY_LATTICE.get()),"Flux input did not clog a lattice");
        helper.assertValueEqual(source.tank.amount(),10,"Bad Flux supply was consumed");helper.assertValueEqual(be.storedEssentia(),1,"Flux was accepted as condenser fuel");
        be.triggerCheck();helper.assertValueEqual(be.cycleTicks(),0,"Clogged root failed to disable the condenser");helper.succeed();
    }

    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void replacingCloggedFilterRestoresLatticeAndConsumesOneFilter(GameTestHelper helper){
        var be=fixture(helper);var pos=be.getBlockPos().above();lattice(helper,pos,true);be.triggerCheck();
        var player=GameTestPlayers.create(helper);player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+2);
        var filter=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:filter"));player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(filter,2));
        player.gameMode.useItemOn(player,helper.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false));
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(CondenserContent.LATTICE.get()),"Fresh filter did not restore clean lattice");
        helper.assertValueEqual(player.getMainHandItem().getCount(),1,"Replacement did not consume exactly one filter");be.triggerCheck();
        helper.assertValueEqual(be.cycleTicks(),587,"Filter replacement did not restore original throughput");helper.succeed();
    }

    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void redstoneStopsProcessingAndSaveReloadPreservesBothStores(GameTestHelper helper){
        var be=fixture(helper);lattice(helper,be.getBlockPos().above(),false);be.insert(Aspect.FIRE,100,Direction.EAST,false);
        AuraSystem.add(helper.getLevel(),be.getBlockPos(),10,true);var aura=AuraSystem.get(helper.getLevel(),be.getBlockPos());float before=aura.flux();
        helper.getLevel().setBlock(be.getBlockPos().west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        helper.assertFalse(be.getBlockState().getValue(CondenserBlock.ENABLED),"Redstone failed to disable the block");for(int i=0;i<600;i++)be.tick();
        helper.assertValueEqual(be.storedEssentia(),100,"Disabled condenser spent its fuel");helper.assertValueEqual(aura.flux(),before,"Disabled condenser removed aura flux");
        helper.getLevel().removeBlock(be.getBlockPos().west(),false);for(int i=0;i<600&&be.storedFlux()==0;i++)be.tick();
        helper.assertValueEqual(be.storedFlux(),1,"Unpowered condenser did not resume");
        var restored=new CondenserEntity(be.getBlockPos(),be.getBlockState());restored.loadWithComponents(be.saveWithoutMetadata(helper.getLevel().registryAccess()),helper.getLevel().registryAccess());
        helper.assertValueEqual(restored.storedEssentia(),95,"Fuel was lost across save/reload");helper.assertValueEqual(restored.storedFlux(),1,"Condensed Vitium was lost across save/reload");helper.succeed();
    }

    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void missingLatticeInsufficientFuelAndDryAuraCannotCreateOutput(GameTestHelper helper){
        var be=fixture(helper);be.insert(Aspect.AIR,100,Direction.NORTH,false);AuraSystem.add(helper.getLevel(),be.getBlockPos(),10,true);
        for(int i=0;i<600;i++)be.tick();helper.assertValueEqual(be.storedFlux(),0,"Condenser worked without lattice");
        lattice(helper,be.getBlockPos().above(),false);be.invalidateLattice();
        AuraSystem.drain(helper.getLevel(),be.getBlockPos(),Float.MAX_VALUE,true,false);for(int i=0;i<600;i++)be.tick();
        helper.assertValueEqual(be.storedFlux(),0,"Condenser created Vitium without aura flux");helper.assertValueEqual(be.storedEssentia(),100,"Dry aura consumed fuel");
        helper.assertValueEqual(be.insert(Aspect.WATER,1,Direction.SOUTH,false),0,"Condenser exceeded 100 fuel cap");
        helper.assertValueEqual(be.insert(Aspect.WATER,1,Direction.UP,false),0,"Condenser accepted essentia from top");
        var saved=be.saveWithoutMetadata(helper.getLevel().registryAccess());saved.putInt("Essentia",4);be.loadWithComponents(saved,helper.getLevel().registryAccess());
        AuraSystem.add(helper.getLevel(),be.getBlockPos(),10,true);for(int i=0;i<600;i++)be.tick();
        helper.assertValueEqual(be.storedFlux(),0,"Insufficient fuel created Vitium");helper.assertValueEqual(be.storedEssentia(),4,"Incomplete recipe consumed its fuel");helper.succeed();
    }

    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void latticeOutsideOriginalRadiusDetachesAndSharedCondensersAreDisabled(GameTestHelper helper){
        var be=fixture(helper);for(int y=1;y<=9;y++)lattice(helper,be.getBlockPos().above(y),false);
        var outside=be.getBlockPos().above(9);
        helper.runAfterDelay(3,()->{
            helper.assertFalse(helper.getLevel().getBlockState(outside).getBlock() instanceof CondenserLatticeBlock,"Out-of-range lattice failed to detach");
            be.triggerCheck();helper.assertValueEqual(be.latticeBlocks(),8,"Original squared-distance limit included the ninth vertical lattice");
            var second=be.getBlockPos().east(2);helper.getLevel().setBlock(second,CondenserContent.CONDENSER.get().defaultBlockState(),3);
            lattice(helper,be.getBlockPos().above().east(),false);lattice(helper,second.above(),false);
            be.triggerCheck();helper.assertValueEqual(be.cycleTicks(),0,"Connected pair of condensers did not invalidate shared lattice");helper.succeed();
        });
    }

    private static CondenserEntity fixture(GameTestHelper helper){var pos=helper.absolutePos(BASE);helper.getLevel().setBlock(pos,CondenserContent.CONDENSER.get().defaultBlockState(),3);return (CondenserEntity)helper.getLevel().getBlockEntity(pos);}
    private static void lattice(GameTestHelper helper,BlockPos pos,boolean dirty){var block=dirty?CondenserContent.DIRTY_LATTICE.get():CondenserContent.LATTICE.get();helper.getLevel().setBlock(pos,CondenserLatticeBlock.connections(block.defaultBlockState(),helper.getLevel(),pos),3);}
    private CondenserGameTests(){}
}
