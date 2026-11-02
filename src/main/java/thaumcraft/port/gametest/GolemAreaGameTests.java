package thaumcraft.port.gametest;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.seal.SealEntity;
import thaumcraft.port.golem.seal.SealHarvest;
import thaumcraft.port.golem.seal.SealPos;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class GolemAreaGameTests {
    @GameTest(template="elemental_tools_empty")
    public static void allSixFacesEnumerateTheWholeAsymmetricAreaExactlyOnce(GameTestHelper helper) {
        var origin=helper.absolutePos(new BlockPos(5,5,5));
        // Bounds are explicit source-oriented offsets for configured X=2, Y=3, Z=4.
        var expected=new AABB[]{
            new AABB(-1,-3,-3,2,0,4),new AABB(-1,1,-3,2,4,4),
            new AABB(-1,-2,-4,2,3,0),new AABB(-1,-2,1,2,3,5),
            new AABB(-2,-2,-3,0,3,4),new AABB(1,-2,-3,3,3,4)};
        for(var face:Direction.values()) {
            var seal=new SealEntity(helper.getLevel(),new SealPos(origin,face),new SealHarvest());
            seal.setArea(new BlockPos(2,3,4));
            var bounds=GolemHelper.getBoundsForArea(seal);var wanted=expected[face.get3DDataValue()].move(Vec3.atLowerCornerOf(origin));
            helper.assertValueEqual(bounds,wanted,"Area bounds point the wrong way for "+face);
            int volume=(int)(wanted.getXsize()*wanted.getYsize()*wanted.getZsize());
            var sampled=new HashSet<BlockPos>();
            for(int index=0;index<volume;index++) {
                var pos=GolemHelper.getPosInArea(seal,index);
                helper.assertTrue(bounds.contains(Vec3.atCenterOf(pos)),"Area scanner left its displayed bounds for "+face);
                helper.assertTrue(sampled.add(pos),"Area scanner repeated a position before finishing "+face);
                helper.assertFalse(pos.equals(origin),"Area scanner included its own supporting block for "+face);
            }
            helper.assertValueEqual(sampled.size(),volume,"Area scanner omitted positions for "+face);
            helper.assertValueEqual(GolemHelper.getPosInArea(seal,volume),GolemHelper.getPosInArea(seal,0),"Area scan failed to wrap for "+face);
        }
        helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void unitAreaMeansExactlyTheAdjacentFaceBlock(GameTestHelper helper) {
        var origin=helper.absolutePos(new BlockPos(5,5,5));
        for(var face:Direction.values()) {
            var seal=new SealEntity(helper.getLevel(),new SealPos(origin,face),new SealHarvest());seal.setArea(new BlockPos(1,1,1));
            helper.assertValueEqual(GolemHelper.getBoundsForArea(seal),new AABB(origin.relative(face)),"Unit seal area is not its adjacent block for "+face);
            for(int index=0;index<10;index++)helper.assertValueEqual(GolemHelper.getPosInArea(seal,index),origin.relative(face),"Unit scanner drifts for "+face);
        }
        helper.succeed();
    }
    private GolemAreaGameTests() { }
}
