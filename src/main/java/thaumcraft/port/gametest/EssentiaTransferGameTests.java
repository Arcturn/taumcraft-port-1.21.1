package thaumcraft.port.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.*;

/** Real registered capabilities, mixed old/new networks, conservation and TC6 storage rules. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class EssentiaTransferGameTests {
    @GameTest(template="empty",batch="essentia_transfer")
    public static void wirelessSearchUsesOriginalSixDirectionalHalfBoxes(GameTestHelper helper){
        BlockPos origin=new BlockPos(100,80,100);
        for(Direction facing:Direction.values()){
            Direction side=facing.getAxis()==Direction.Axis.X?Direction.UP:Direction.EAST;
            Direction side2=facing.getAxis()==Direction.Axis.Z?Direction.UP:Direction.SOUTH;
            helper.assertTrue(RemoteEssentia.inRange(origin,origin.relative(facing,15).relative(side,16).relative(side2,-16),facing),"Original far corner was excluded: "+facing);
            helper.assertFalse(RemoteEssentia.inRange(origin,origin.relative(facing,16),facing),"16th forward block must be excluded");
            helper.assertFalse(RemoteEssentia.inRange(origin,origin.relative(facing,-1),facing),"Rear source leaked into search");
            helper.assertFalse(RemoteEssentia.inRange(origin,origin.relative(side,17),facing),"Perpendicular range exceeded 16");
            helper.assertFalse(RemoteEssentia.inRange(origin,origin,facing),"A transfer must not target itself");
            var input=new EssentiaTransferEntity(origin,EssentiaTransferContent.INPUT.get().defaultBlockState().setValue(EssentiaTransferBlock.FACING,facing));
            for(Direction port:Direction.values())helper.assertValueEqual(input.connects(port),port==facing.getOpposite(),"Only the rear port should connect");
            helper.assertValueEqual(input.suction(facing.getOpposite()),128,"Input pressure must match BETA26");
            helper.assertValueEqual(input.insert(Aspect.FIRE,1,facing.getOpposite(),false),0,"A storage-free transfer must not silently eat direct insertion");
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="essentia_transfer")
    public static void inputPrefersFilledJarAndPreservesTotalEssentia(GameTestHelper helper){
        var source=jar(helper,new BlockPos(2,3,2));source.fill(Aspect.COLD,10,false);source.braced=true;
        var input=transfer(helper,new BlockPos(2,4,2),true);
        var empty=jar(helper,new BlockPos(3,4,2));
        var filled=jar(helper,new BlockPos(2,5,2));filled.fill(Aspect.COLD,1,false);
        helper.assertTrue(EssentiaEntity.at(helper.getLevel(),input.getBlockPos(),Direction.DOWN)==input,"Wireless port capability missing");
        ticks(input,4);
        helper.assertValueEqual(source.storedAmount(),10,"Transfer ran before the fifth tick");
        ticks(input,1);
        helper.assertValueEqual(source.storedAmount(),9,"Source did not lose exactly one unit");
        helper.assertValueEqual(filled.storedAmount(),2,"Existing matching jar was not preferred over an empty jar");
        helper.assertValueEqual(empty.storedAmount(),0,"Empty jar received essentia ahead of a filled match");
        helper.assertValueEqual(source.storedAmount()+filled.storedAmount()+empty.storedAmount(),11,"Input duplicated or lost essentia");
        helper.succeed();
    }

    @GameTest(template="empty",batch="essentia_transfer")
    public static void everyMountedDirectionTransfersFromRearTubeToFrontJar(GameTestHelper helper){
        BlockPos origin=new BlockPos(4,4,4);
        for(Direction facing:Direction.values()){
            var tube=(TubeEntity)place(helper,origin.relative(facing.getOpposite()),EssentiaContent.TUBES.get("tube").get().defaultBlockState());tube.tank.insert(Aspect.SENSES,1,false,false);
            var input=(EssentiaTransferEntity)place(helper,origin,EssentiaTransferContent.INPUT.get().defaultBlockState().setValue(EssentiaTransferBlock.FACING,facing));
            var target=jar(helper,origin.relative(facing,2));target.fill(Aspect.SENSES,1,false);
            ticks(input,5);
            helper.assertValueEqual(tube.tank.amount(),0,"Rear tube not drained for "+facing);
            helper.assertValueEqual(target.storedAmount(),2,"Forward jar not filled for "+facing);
            helper.getLevel().removeBlock(tube.getBlockPos(),false);helper.getLevel().removeBlock(target.getBlockPos(),false);helper.getLevel().removeBlock(input.getBlockPos(),false);
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="essentia_transfer",timeoutTicks=240)
    public static void removedCachedSourceRetriesAfterTenSecondsInsteadOfWaitingForever(GameTestHelper helper){
        BlockPos origin=helper.absolutePos(new BlockPos(2,3,2));
        var jar=jar(helper,new BlockPos(2,4,2));jar.fill(Aspect.AVERSION,1,false);
        var network=new RemoteEssentia();
        helper.assertTrue(network.providing(helper.getLevel(),origin,Direction.UP,Aspect.AVERSION)!=null,"Initial source was not discovered");
        helper.getLevel().removeBlock(jar.getBlockPos(),false);
        helper.assertTrue(network.providing(helper.getLevel(),origin,Direction.UP,Aspect.AVERSION)==null,"Removed cached source remained valid");
        var replacement=jar(helper,new BlockPos(2,4,2));replacement.fill(Aspect.AVERSION,1,false);
        helper.runAfterDelay(40,()->helper.assertTrue(network.providing(helper.getLevel(),origin,Direction.UP,Aspect.AVERSION)==null,"Retry ignored original ten-second delay"));
        helper.runAfterDelay(205,()->{
            var found=network.providing(helper.getLevel(),origin,Direction.UP,Aspect.AVERSION);
            helper.assertTrue(found!=null&&found.source()==replacement,"Probe during cooldown extended the deadline or replacement source was not rediscovered");
            helper.succeed();
        });
    }

    @GameTest(template="empty",batch="essentia_transfer")
    public static void wirelessFindsPendingSavedJarInAnAlreadyLoadedChunk(GameTestHelper helper){
        BlockPos origin=helper.absolutePos(new BlockPos(2,3,2));
        var jar=jar(helper,new BlockPos(2,4,2));jar.fill(Aspect.TRAP,17,false);
        var saved=jar.saveWithFullMetadata(helper.getLevel().registryAccess());var chunk=helper.getLevel().getChunkAt(jar.getBlockPos());
        chunk.removeBlockEntity(jar.getBlockPos());chunk.setBlockEntityNbt(saved);
        helper.assertFalse(chunk.getBlockEntities().containsKey(jar.getBlockPos()),"Fixture did not leave a pending saved block entity");
        var target=new RemoteEssentia().providing(helper.getLevel(),origin,Direction.UP,Aspect.TRAP);
        helper.assertTrue(target!=null&&target.pos().equals(jar.getBlockPos())&&target.source().storedAmount()==17,"Loaded chunk's pending saved jar was skipped by wireless search");
        helper.succeed();
    }

    @GameTest(template="empty",batch="essentia_transfer")
    public static void outputRequiresTypedSuctionAndSkipsBracedSources(GameTestHelper helper){
        var sink=jar(helper,new BlockPos(2,3,2));
        var output=transfer(helper,new BlockPos(2,4,2),false);
        var blocked=jar(helper,new BlockPos(3,4,2));blocked.fill(Aspect.DESIRE,10,false);blocked.braced=true;
        var source=jar(helper,new BlockPos(2,5,2));source.fill(Aspect.DESIRE,10,false);
        ticks(output,5);
        helper.assertValueEqual(sink.storedAmount(),0,"Untyped suction must not draw from a remote jar");
        sink.tank.filter(Aspect.DESIRE);ticks(output,5);
        helper.assertValueEqual(sink.storedAmount(),1,"Typed receiver did not receive essentia");
        helper.assertValueEqual(blocked.storedAmount(),10,"Brace failed to block wireless extraction");
        helper.assertValueEqual(source.storedAmount(),9,"Wireless source was not debited exactly once");
        helper.assertValueEqual(sink.storedAmount()+blocked.storedAmount()+source.storedAmount(),20,"Output duplicated or lost essentia");
        helper.succeed();
    }

    @GameTest(template="empty",batch="essentia_transfer")
    public static void bufferWirelessPairAndFilteredJarFormWorkingNetwork(GameTestHelper helper){
        var buffer=(BufferEntity)place(helper,new BlockPos(3,3,3),EssentiaContent.TUBES.get("tube_buffer").get().defaultBlockState());
        buffer.contents.insert(Aspect.ELDRITCH,7,false);
        var input=transfer(helper,new BlockPos(3,4,3),true);
        var remote=jar(helper,new BlockPos(4,4,3));remote.fill(Aspect.ELDRITCH,1,false);
        var output=transfer(helper,new BlockPos(5,4,3),false);
        var sink=jar(helper,new BlockPos(5,3,3));sink.tank.filter(Aspect.ELDRITCH);
        for(int i=0;i<4;i++){ticks(input,5);ticks(output,5);}
        helper.assertValueEqual(buffer.contents.total(),3,"Input failed to drain the old mixed buffer");
        helper.assertValueEqual(remote.storedAmount(),1,"Intermediate jar accumulated or lost essentia");
        helper.assertValueEqual(sink.storedAmount(),4,"Output failed to feed the old filtered jar");
        helper.assertValueEqual(buffer.contents.total()+remote.storedAmount()+sink.storedAmount(),8,"Mixed network violated conservation");
        helper.succeed();
    }

    @GameTest(template="empty",batch="essentia_transfer")
    public static void jarVoidAlembicFiltersAndBracesSurviveSave(GameTestHelper helper){
        var pos=new BlockPos(1,3,1);var jar=new JarEntity(pos,EssentiaContent.JAR.get().defaultBlockState());
        jar.tank.filter(Aspect.WATER);jar.fill(Aspect.WATER,240,false);jar.braced=true;
        helper.assertValueEqual(jar.fill(Aspect.FIRE,20,false),0,"Label accepted a different aspect");
        helper.assertValueEqual(jar.fill(Aspect.WATER,20,true),10,"Simulation ignored remaining jar capacity");
        helper.assertValueEqual(jar.storedAmount(),240,"Simulation mutated a jar");
        helper.assertValueEqual(jar.takeRemote(Aspect.WATER,1,false),0,"Brace allowed a remote drain");
        helper.assertValueEqual(jar.extract(Aspect.WATER,1,Direction.UP,false),1,"Brace incorrectly blocked pipe extraction");
        var restored=new JarEntity(pos,jar.getBlockState());restored.loadWithComponents(jar.saveWithoutMetadata(helper.getLevel().registryAccess()),helper.getLevel().registryAccess());
        helper.assertTrue(restored.braced&&restored.tank.filter()==Aspect.WATER&&restored.storedAmount()==239,"Jar contents, filter or brace lost on reload");
        var voidJar=new JarEntity(pos,EssentiaContent.VOID_JAR.get().defaultBlockState());voidJar.tank.filter(Aspect.WATER);
        helper.assertValueEqual(voidJar.suction(Direction.UP),48,"Filtered void suction before filling");
        helper.assertValueEqual(voidJar.fill(Aspect.WATER,300,false),300,"Void jar should accept and destroy overflow");
        helper.assertValueEqual(voidJar.storedAmount(),250,"Void jar exceeded capacity");
        helper.assertValueEqual(voidJar.suction(Direction.UP),32,"Full void jar should retain lower suction");
        var alembic=new JarEntity(pos,EssentiaContent.ALEMBIC.get().defaultBlockState());alembic.tank.filter(Aspect.WATER);alembic.labelFacing=Direction.WEST;
        helper.assertValueEqual(alembic.fill(Aspect.WATER,200,false),128,"Alembic capacity differs from TC6");
        helper.assertFalse(alembic.connects(Direction.DOWN)||alembic.connects(Direction.WEST),"Alembic bottom/label must not connect");
        helper.assertValueEqual(alembic.addRemote(Aspect.WATER,1,false),0,"Alembic must not act as a remote jar");
        helper.succeed();
    }

    @GameTest(template="empty",batch="essentia_transfer")
    public static void jarsAndAlembicsExposeOriginalComparatorFillLevels(GameTestHelper helper){
        for(var block:List.of(EssentiaContent.JAR,EssentiaContent.VOID_JAR,EssentiaContent.ALEMBIC)){
            var jar=(JarEntity)place(helper,new BlockPos(2,3,2),block.get().defaultBlockState());var state=jar.getBlockState();
            helper.assertTrue(state.hasAnalogOutputSignal(),"Container omitted comparator support");
            helper.assertValueEqual(state.getAnalogOutputSignal(helper.getLevel(),jar.getBlockPos()),0,"Empty comparator level");
            jar.fill(Aspect.WATER,jar.tank.capacity/2,false);
            helper.assertValueEqual(state.getAnalogOutputSignal(helper.getLevel(),jar.getBlockPos()),8,"Half-full comparator level");
            jar.fill(Aspect.WATER,jar.tank.capacity,false);
            helper.assertValueEqual(state.getAnalogOutputSignal(helper.getLevel(),jar.getBlockPos()),15,"Full comparator level");
            jar.tank.extract(Aspect.WATER,jar.tank.capacity-1,false);
            helper.assertValueEqual(state.getAnalogOutputSignal(helper.getLevel(),jar.getBlockPos()),1,"Single-unit comparator level");
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="essentia_transfer")
    public static void bufferHonoursStrongerCompetingSuctionAndResonatorListsAllAspects(GameTestHelper helper){
        var buffer=(BufferEntity)place(helper,new BlockPos(3,3,3),EssentiaContent.TUBES.get("tube_buffer").get().defaultBlockState());
        var requester=(TubeEntity)place(helper,new BlockPos(3,2,3),EssentiaContent.TUBES.get("tube").get().defaultBlockState());
        var competitor=(TubeEntity)place(helper,new BlockPos(4,3,3),EssentiaContent.TUBES.get("tube_filter").get().defaultBlockState());
        buffer.contents.insert(Aspect.FIRE,3,false);buffer.contents.insert(Aspect.AIR,2,false);buffer.bellows=1;
        requester.suction=31;requester.suctionType=Aspect.FIRE;competitor.suction=64;competitor.suctionType=Aspect.FIRE;
        helper.assertValueEqual(buffer.extract(Aspect.FIRE,1,Direction.DOWN,false),0,"Buffer ignored a stronger competing branch");
        buffer.open&=~(1<<Direction.EAST.ordinal());
        helper.assertValueEqual(buffer.extract(Aspect.FIRE,1,Direction.DOWN,true),1,"Closed branch still blocked extraction");
        helper.assertValueEqual(buffer.contents.total(),5,"Simulated buffer extraction mutated contents");
        helper.assertValueEqual(buffer.extract(Aspect.FIRE,1,Direction.DOWN,false),1,"Eligible buffered aspect was not extracted");
        helper.assertValueEqual(ResonatorItem.readout(buffer,Direction.DOWN).size(),3,"Resonator omitted a mixed aspect or suction line");
        buffer.chokes[Direction.DOWN.ordinal()]=2;helper.assertValueEqual(buffer.suction(Direction.DOWN),0,"Full choke did not stop suction");
        buffer.chokes[Direction.DOWN.ordinal()]=1;helper.assertValueEqual(buffer.suction(Direction.DOWN),1,"Partial choke did not limit suction");
        helper.succeed();
    }

    @GameTest(template="empty",batch="essentia_transfer")
    public static void closedValveReleasesStoredUnitAndCentrifugePausesOnRedstone(GameTestHelper helper){
        var valve=new TubeEntity(new BlockPos(1,3,1),EssentiaContent.TUBES.get("tube_valve").get().defaultBlockState());
        valve.tank.insert(Aspect.AIR,1,false,false);valve.allowFlow=false;valve.suction=40;
        helper.assertValueEqual(valve.suction(Direction.UP),0,"Closed valve propagated suction");
        helper.assertValueEqual(valve.extract(Aspect.AIR,1,Direction.UP,false),1,"Closed valve should permit draining its retained unit, as in TC6");
        var centrifuge=(CentrifugeEntity)place(helper,new BlockPos(3,3,3),EssentiaContent.CENTRIFUGE.get().defaultBlockState());
        helper.assertValueEqual(centrifuge.insert(Aspect.FIRE,1,Direction.DOWN,false),0,"Centrifuge accepted a primal aspect");
        helper.assertValueEqual(centrifuge.insert(Aspect.ENERGY,1,Direction.DOWN,false),1,"Centrifuge rejected a compound aspect");
        helper.getLevel().setBlock(centrifuge.getBlockPos().east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        ticks(centrifuge,10);helper.assertValueEqual(centrifuge.process,39,"Powered centrifuge kept processing");
        var restored=new CentrifugeEntity(centrifuge.getBlockPos(),centrifuge.getBlockState());restored.loadWithComponents(centrifuge.saveWithoutMetadata(helper.getLevel().registryAccess()),helper.getLevel().registryAccess());
        helper.assertTrue(restored.incoming==Aspect.ENERGY&&restored.process==39,"Centrifuge lost an in-flight unit on reload");
        helper.getLevel().removeBlock(centrifuge.getBlockPos().east(),false);ticks(centrifuge,39);
        helper.assertTrue(centrifuge.incoming==null&&List.of(Aspect.FIRE,Aspect.ORDER).contains(centrifuge.outgoing),"Centrifuge did not split into one original component");
        helper.assertValueEqual(centrifuge.extract(centrifuge.outgoing,20,Direction.UP,false),1,"Centrifuge duplicated output on large extraction request");
        helper.succeed();
    }
    private static net.minecraft.world.level.block.entity.BlockEntity place(GameTestHelper helper,BlockPos relative,BlockState state){var pos=helper.absolutePos(relative);helper.getLevel().setBlock(pos,state,3);return helper.getLevel().getBlockEntity(pos);}
    private static JarEntity jar(GameTestHelper helper,BlockPos pos){return (JarEntity)place(helper,pos,EssentiaContent.JAR.get().defaultBlockState());}
    private static EssentiaTransferEntity transfer(GameTestHelper helper,BlockPos pos,boolean input){return (EssentiaTransferEntity)place(helper,pos,(input?EssentiaTransferContent.INPUT:EssentiaTransferContent.OUTPUT).get().defaultBlockState().setValue(EssentiaTransferBlock.FACING,Direction.UP));}
    private static void ticks(EssentiaEntity entity,int count){for(int i=0;i<count;i++)entity.tick();}
    private EssentiaTransferGameTests(){}
}
