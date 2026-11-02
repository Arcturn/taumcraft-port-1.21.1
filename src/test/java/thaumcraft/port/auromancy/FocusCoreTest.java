package thaumcraft.port.auromancy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.focus.*;
import static org.junit.jupiter.api.Assertions.*;

/** Independent arithmetic/flow checks for source FocalManipulator, ItemFocus and FocusEngine. */
public class FocusCoreTest {
    @Test void editorCostsPreserveDuplicateOccurrenceOrderAndPerAdditionTruncation() {
        var low = new ProbeEffect("same",3); var high = new ProbeEffect("same",7);
        assertEquals(13,FocusPackage.calculateComplexity(List.of(low,high)));
        assertEquals(11,FocusPackage.calculateComplexity(List.of(high,low)));
        assertEquals(20,FocusPackage.calculateComplexity(List.of(low,low,low,low)));
        assertEquals(Map.of(Aspect.FIRE,4),FocusPackage.crystals(List.of(low,low,low,low)));
        assertEquals(0,FocusPackage.calculateComplexity(List.of(new FocusMediumRoot())));
    }
    @Test void focusCreationAndCastPricesUseOriginalIntegerBoundaries() {
        assertEquals(53,FocusPackage.constructionVis(5,15));
        assertEquals(55,FocusPackage.constructionVis(5,25));
        assertEquals(60,FocusPackage.constructionVis(5,50));
        assertEquals(1,FocusPackage.experienceCost(0));assertEquals(2,FocusPackage.experienceCost(6));
        assertEquals(3,FocusPackage.experienceCost(7));assertEquals(7,FocusPackage.experienceCost(50));
        var pack=new FocusPackage();
        pack.setComplexity(14);assertEquals(2.8f,pack.getVisCost());assertEquals(6,pack.getActivationTime());
        pack.setComplexity(15);assertEquals(9,pack.getActivationTime());
        pack.setComplexity(50);assertEquals(120,pack.getActivationTime());
    }
    @Test void settingChoicesRejectForgedNetworkValuesWithoutChangingCurrentSelection() {
        var list=new NodeSetting("cone","cone",new NodeSetting.NodeSettingIntList(new int[]{10,30,60,90,180,270,360},new String[]{"10","30","60","90","180","270","360"}));
        assertTrue(list.setValueChecked(90));assertFalse(list.setValueChecked(91));assertEquals(90,list.getValue());
        list.decrement();assertEquals(60,list.getValue());list.setValue(360);list.increment();assertEquals(360,list.getValue());
        var range=new NodeSetting("power","power",new NodeSetting.NodeSettingIntRange(1,5));
        assertFalse(range.setValueChecked(0));assertFalse(range.setValueChecked(Integer.MAX_VALUE));assertEquals(1,range.getValue());
        assertTrue(range.setValueChecked(1));range.increment();assertEquals(2,range.getValue());
    }
    @Test void executorStopsAtIntermediaryAndPreservesOffhandContinuationContext() {
        var pack=new FocusPackage();pack.setCastingHand(InteractionHand.OFF_HAND);pack.setUniqueID(UUID.randomUUID());pack.setCasterUUID(UUID.randomUUID());
        var medium=new ProbeMedium();var effect=new ProbeEffect("effect",3);
        var trajectory=new Trajectory(Vec3.ZERO,new Vec3(0,0,1));var target=hit(1);
        pack.addNode(new FocusMediumRoot(new Trajectory[]{trajectory},new HitResult[]{target}));pack.addNode(medium);pack.addNode(effect);
        FocusEngine.runFocusPackage(pack,null,null);
        assertEquals(1,medium.executions);assertTrue(effect.hits.isEmpty());
        var remainder=medium.getRemainingPackage();assertNotNull(remainder);assertEquals(List.of(effect),remainder.nodes);
        assertEquals(pack.getUniqueID(),remainder.getUniqueID());assertEquals(pack.getCasterUUID(),remainder.getCasterUUID());
        assertEquals(InteractionHand.OFF_HAND,remainder.getCastingHand());assertEquals(HumanoidArm.LEFT,remainder.getCastingArm());
        FocusEngine.runFocusPackage(remainder,new Trajectory[]{trajectory},new HitResult[]{target});
        assertEquals(List.of(target),effect.hits);assertEquals(.5f,effect.powers.getFirst());
    }
    @Test void splitBranchesReceiveTheirOwnPowerAndTheSameCasterHandAndCastId() {
        var pack=new FocusPackage();pack.setCastingHand(InteractionHand.OFF_HAND);pack.setCasterUUID(UUID.randomUUID());pack.setUniqueID(UUID.randomUUID());
        var split=new ProbeSplit();var left=new ProbeEffect("left",3);var right=new ProbeEffect("right",3);
        var first=new FocusPackage();first.addNode(left);var second=new FocusPackage();second.addNode(right);
        split.getSplitPackages().add(first);split.getSplitPackages().add(second);
        pack.addNode(new FocusMediumRoot(null,new HitResult[]{hit(3)}));pack.addNode(split);
        FocusEngine.runFocusPackage(pack,null,null);
        for(var branch:List.of(first,second)) {
            assertEquals(pack.getCasterUUID(),branch.getCasterUUID());assertEquals(pack.getUniqueID(),branch.getUniqueID());
            assertEquals(InteractionHand.OFF_HAND,branch.getCastingHand());assertEquals(.75f,branch.getPower());
        }
        assertEquals(List.of(.75f),left.powers);assertEquals(List.of(.75f),right.powers);
    }
    @Test void validatorRejectsBrokenSupplyChainsCyclesAndCapacityButAllowsBorrowedPrograms() {
        var pack=new FocusPackage();pack.addNode(new FocusMediumRoot());pack.addNode(new ProbeEffect("heal",3));pack.setComplexity(3);
        assertTrue(pack.validate(15,null).valid());assertFalse(pack.validate(2,null).valid());
        assertFalse(pack.validate(15,key->false).valid());
        pack.addNode(new ProbeEffect("fire",3));assertFalse(pack.isValid());
        var cycle=new FocusPackage();cycle.addNode(cycle);assertFalse(cycle.isValid());assertThrows(IllegalArgumentException.class,cycle::serialize);
    }
    @Test void allTwentyOneSourceNodesAndOffhandProgramsRoundTripWithoutLosingSettings() {
        assertEquals(21, FocusEngine.elements.size());
        for (var key : FocusEngine.elements.keySet()) {
            var element = FocusEngine.getElement(key);
            assertNotNull(element, key);
            assertEquals(key, element.getKey());
            assertNotNull(FocusEngine.getElementIcon(key), key);
            var pack = new FocusPackage();
            pack.addNode(element);
            pack.setCastingHand(InteractionHand.OFF_HAND);
            pack.setCasterUUID(UUID.randomUUID());
            pack.setUniqueID(UUID.randomUUID());
            pack.setComplexity(15);
            var decoded = new FocusPackage();
            decoded.deserialize(pack.serialize());
            assertEquals("", decoded.getDecodeError(), key);
            assertEquals(pack.serialize(), decoded.serialize(), key);
        }
    }
    @Test void networkCodecRejectsUnknownNodesIllegalChoicesAndExcessiveNesting() {
        var pack = new FocusPackage();
        pack.addNode(FocusEngine.getElement("thaumcraft.FIRE"));
        var badNode = pack.serialize();
        badNode.getList("nodes", Tag.TAG_COMPOUND).getCompound(0).putString("key", "forged.NODE");
        var decoded = new FocusPackage();
        decoded.deserialize(badNode);
        assertFalse(decoded.getDecodeError().isEmpty());
        assertTrue(decoded.nodes.isEmpty());
        var badSetting = pack.serialize();
        badSetting.getList("nodes", Tag.TAG_COMPOUND).getCompound(0).putInt("setting.power", Integer.MAX_VALUE);
        decoded.deserialize(badSetting);
        assertFalse(decoded.getDecodeError().isEmpty());
        assertTrue(decoded.nodes.isEmpty());
        var fractionalSetting = pack.serialize();
        fractionalSetting.getList("nodes", Tag.TAG_COMPOUND).getCompound(0).putFloat("setting.power", 1.9f);
        decoded.deserialize(fractionalSetting);assertFalse(decoded.getDecodeError().isEmpty());
        var badHand = pack.serialize();badHand.putInt("castingHand", 2);
        decoded.deserialize(badHand);assertFalse(decoded.getDecodeError().isEmpty());
        var nested = new FocusPackage();var current = nested;
        for (int i=0;i<=FocusPackage.MAX_DEPTH;i++) {
            var next = new FocusPackage();current.addNode(next);current = next;
        }
        assertThrows(IllegalArgumentException.class, nested::serialize);
    }
    private static BlockHitResult hit(int x) { return new BlockHitResult(new Vec3(x,.5,.5),Direction.UP,new BlockPos(x,0,0),false); }
    private static final class ProbeEffect extends FocusEffect {
        private final String key;private final int complexity;
        final List<HitResult> hits=new ArrayList<>();final List<Float> powers=new ArrayList<>();
        ProbeEffect(String key,int complexity){this.key=key;this.complexity=complexity;}
        public String getKey(){return key;}public String getResearch(){return "BASEAUROMANCY";}public int getComplexity(){return complexity;}public Aspect getAspect(){return Aspect.FIRE;}
        public boolean execute(HitResult target,Trajectory trajectory,float power,int index){hits.add(target);powers.add(power);return true;}
        public void renderParticleFX(Level level,double x,double y,double z,double vx,double vy,double vz){}
    }
    private static final class ProbeMedium extends FocusMedium {
        int executions;public String getKey(){return "intermediary";}public String getResearch(){return "BASEAUROMANCY";}
        public int getComplexity(){return 1;}public Aspect getAspect(){return null;}
        public float getPowerMultiplier(){return .5f;}public boolean hasIntermediary(){return true;}
        public boolean execute(Trajectory trajectory){executions++;return true;}
    }
    private static final class ProbeSplit extends FocusModSplit {
        public String getKey(){return "split";}public String getResearch(){return "FOCUSSPLIT";}public int getComplexity(){return 4;}
        public EnumSupplyType[] mustBeSupplied(){return new EnumSupplyType[]{EnumSupplyType.TARGET};}
        public EnumSupplyType[] willSupply(){return new EnumSupplyType[]{EnumSupplyType.TARGET};}
        public HitResult[] supplyTargets(){return getParent().supplyTargets();}public boolean execute(){return true;}
    }
}
