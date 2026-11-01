package thaumcraft.port.gametest;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.RevealingReadout;
import thaumcraft.port.essentia.*;
import thaumcraft.port.machine.CrucibleEntity;
import thaumcraft.port.machine.MachineContent;

/** Checks the original IAspectContainer contract against registered block entities. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class RevealingReadoutGameTests {
    private static final BlockPos POS=new BlockPos(1,3,1);

    @GameTest(template="empty")
    public static void jarsAndAlembicsExposeOnlyStoredEssentia(GameTestHelper helper) {
        for(var block:java.util.List.of(EssentiaContent.JAR,EssentiaContent.VOID_JAR,EssentiaContent.ALEMBIC)) {
            var jar=new JarEntity(POS,block.get().defaultBlockState());
            jar.tank.filter(Aspect.AIR);
            check(helper,jar,Map.of());
            jar.tank.insert(Aspect.AIR,17,false,false);
            check(helper,jar,Map.of(Aspect.AIR,17));
        }
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void filterShowsItsFilterWithoutCountWhileOrdinaryTubesStayHidden(GameTestHelper helper) {
        var filter=new TubeEntity(POS,EssentiaContent.TUBES.get("tube_filter").get().defaultBlockState());
        check(helper,filter,Map.of());
        filter.tank.filter(Aspect.FIRE);
        filter.tank.insert(Aspect.FIRE,1,false,false);
        check(helper,filter,Map.of(Aspect.FIRE,-1));
        for(var name:java.util.List.of("tube","tube_restrict","tube_oneway","tube_valve")) {
            var tube=new TubeEntity(POS,EssentiaContent.TUBES.get(name).get().defaultBlockState());
            tube.tank.insert(Aspect.FIRE,1,false,false);
            check(helper,tube,Map.of());
        }
        var buffer=new BufferEntity(POS,EssentiaContent.TUBES.get("tube_buffer").get().defaultBlockState());
        buffer.contents.insert(Aspect.FIRE,3,false);buffer.contents.insert(Aspect.AIR,2,false);
        check(helper,buffer,Map.of(Aspect.FIRE,3,Aspect.AIR,2));
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void centrifugeRevealsOnlyOutgoingAspectAndCrucibleRevealsAllContents(GameTestHelper helper) {
        var centrifuge=new CentrifugeEntity(POS,EssentiaContent.CENTRIFUGE.get().defaultBlockState());
        centrifuge.incoming=Aspect.ENERGY;check(helper,centrifuge,Map.of());
        centrifuge.outgoing=Aspect.FIRE;check(helper,centrifuge,Map.of(Aspect.FIRE,1));
        var crucible=new CrucibleEntity(POS,MachineContent.CRUCIBLE.get().defaultBlockState());
        crucible.state.restore(1000,151,-100,Map.of(Aspect.FIRE,5,Aspect.AIR,7));
        check(helper,crucible,Map.of(Aspect.FIRE,5,Aspect.AIR,7));
        var smelter=new SmelterEntity(POS,EssentiaContent.SMELTERS.get("smelter_basic").get().defaultBlockState());
        check(helper,smelter,Map.of());check(helper,null,Map.of());
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void bothThaumatoriumHalvesRevealTheSameAccumulatedEssentia(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(POS);
        level.setBlock(pos.below(),MachineContent.CRUCIBLE.get().defaultBlockState(),2);
        level.setBlock(pos,EssentiaContent.THAUMATORIUM.get().defaultBlockState(),2);
        level.setBlock(pos.above(),EssentiaContent.THAUMATORIUM_TOP.get().defaultBlockState(),2);
        var main=(ThaumatoriumEntity)helper.getBlockEntity(POS);
        var top=(ThaumatoriumEntity)helper.getBlockEntity(POS.above());
        main.contents.insert(Aspect.FIRE,9,false);
        check(helper,main,Map.of(Aspect.FIRE,9));check(helper,top,Map.of(Aspect.FIRE,9));
        helper.succeed();
    }

    private static void check(GameTestHelper helper,BlockEntity entity,Map<Aspect,Integer> expected) {
        helper.assertValueEqual(RevealingReadout.aspects(entity),expected,"Incorrect revealing view for "+entity);
    }
}
