package thaumcraft.port.auromancy;

import java.util.HashMap;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import thaumcraft.port.auromancy.focal.FocalGraph;
import thaumcraft.port.auromancy.focal.FocusElementNode;
import thaumcraft.port.auromancy.focus.FocusEngine;
import thaumcraft.port.auromancy.focus.FocusModSplit;
import thaumcraft.port.auromancy.focus.FocusNode;
import static org.junit.jupiter.api.Assertions.*;

class FocalGraphTest {
    @Test void emptySocketsCanBeEditedButCannotBecomeCraftedPrograms() {
        var graph=new HashMap<Integer,FocusElementNode>();
        graph.put(0,node(0,-1,0,"ROOT",1));graph.put(1,node(1,0,1,null));
        assertDoesNotThrow(()->FocalGraph.validateEdit(graph));
        assertThrows(IllegalArgumentException.class,()->FocalGraph.compile(graph));
        graph.put(1,node(1,0,1,"thaumcraft.HEAL"));
        assertTrue(FocalGraph.compile(graph).isValid());
    }
    @Test void splitCompilationPreservesBranchOrderDuplicateCostsAndIndependentPaidSettings() {
        var graph=split();graph.get(3).node.getSetting("power").setValue(3);
        var compiled=FocalGraph.compile(graph);
        assertEquals(15,compiled.getComplexity()); // split4 + fire2 + repeated fire6*1.5
        assertEquals(2,compiled.nodes.size());
        var split=(FocusModSplit)compiled.nodes.get(1);
        assertEquals(2,split.getSplitPackages().size());
        assertEquals(1,((FocusNode)split.getSplitPackages().get(0).nodes.getFirst()).getSettingValue("power"));
        assertEquals(3,((FocusNode)split.getSplitPackages().get(1).nodes.getFirst()).getSettingValue("power"));
        assertEquals(.75f,graph.get(2).getPower(graph));
        graph.get(3).node.getSetting("power").setValue(5);
        assertEquals(3,((FocusNode)split.getSplitPackages().get(1).nodes.getFirst()).getSettingValue("power"));
    }
    @Test void nodeCodecRetainsOriginalFieldsAndRejectsUnknownOrOutOfRangeSettings() {
        var source=node(12,9,3,"thaumcraft.FIRE");source.x=-4;source.node.getSetting("power").setValue(4);source.complexityMultiplier=1.5f;
        CompoundTag encoded=source.serialize();var decoded=new FocusElementNode();decoded.deserialize(encoded);
        assertEquals(encoded,decoded.serialize());
        var bad=encoded.copy();bad.putInt("setting.power",Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class,()->decoded.deserialize(bad));
        var unknown=encoded.copy();unknown.putString("key","unknown.FOCUS");
        assertThrows(IllegalArgumentException.class,()->decoded.deserialize(unknown));
        var fractional=encoded.copy();fractional.putFloat("setting.power",4.5f);
        assertThrows(IllegalArgumentException.class,()->decoded.deserialize(fractional));
    }
    @Test void editorSnapshotsCannotShareMutableChildArraysWithLaterEdits() {
        var source=node(0,-1,0,"ROOT",1);var snapshot=source.serialize();source.children[0]=20;
        assertArrayEquals(new int[]{1},snapshot.getIntArray("children"));
        var decoded=new FocusElementNode();decoded.deserialize(snapshot);decoded.children[0]=30;
        assertArrayEquals(new int[]{1},snapshot.getIntArray("children"));
    }
    @Test void malformedLinksOrForgedSupplyFlagsCannotReachTheCompiler() {
        var missing=split();missing.remove(3);assertThrows(IllegalArgumentException.class,()->FocalGraph.validateEdit(missing));
        var duplicate=split();duplicate.get(1).children=new int[]{2,2};assertThrows(IllegalArgumentException.class,()->FocalGraph.validateEdit(duplicate));
        var disconnected=split();disconnected.put(99,node(99,0,1,"thaumcraft.HEAL"));assertThrows(IllegalArgumentException.class,()->FocalGraph.validateEdit(disconnected));
        var flags=split();flags.get(1).trajectory=true;assertThrows(IllegalArgumentException.class,()->FocalGraph.validateEdit(flags));
        var wrongRoot=split();wrongRoot.get(0).parent=2;assertThrows(IllegalArgumentException.class,()->FocalGraph.validateEdit(wrongRoot));
    }
    @Test void targetOnlySplitCannotSupplyTrajectoryToAnotherMedium() {
        var graph=split();graph.put(2,node(2,1,2,"thaumcraft.TOUCH",4));graph.put(4,node(4,2,3,"thaumcraft.FIRE"));
        assertThrows(IllegalArgumentException.class,()->FocalGraph.compile(graph));
    }
    @Test void exclusiveScatterCannotBeDuplicatedAlongTheGraph() {
        var graph=new HashMap<Integer,FocusElementNode>();
        graph.put(0,node(0,-1,0,"ROOT",1));graph.put(1,node(1,0,1,"thaumcraft.SCATTER",2));
        graph.put(2,node(2,1,2,"thaumcraft.SCATTER",3));graph.put(3,node(3,2,3,"thaumcraft.TOUCH",4));graph.put(4,node(4,3,4,"thaumcraft.FIRE"));
        assertThrows(IllegalArgumentException.class,()->FocalGraph.validateEdit(graph));
    }
    private static HashMap<Integer,FocusElementNode> split() {
        var graph=new HashMap<Integer,FocusElementNode>();
        graph.put(0,node(0,-1,0,"ROOT",1));graph.put(1,node(1,0,1,"thaumcraft.SPLITTARGET",2,3));
        graph.put(2,node(2,1,2,"thaumcraft.FIRE"));graph.put(3,node(3,1,2,"thaumcraft.FIRE"));graph.get(2).x=-1;graph.get(3).x=1;
        return graph;
    }
    private static FocusElementNode node(int id,int parent,int y,String key,int... children) {
        var node=new FocusElementNode();node.id=id;node.parent=parent;node.y=y;node.children=children;
        if(key!=null){node.node=(FocusNode)FocusEngine.getElement(key);node.target=node.node.canSupply(FocusNode.EnumSupplyType.TARGET);node.trajectory=node.node.canSupply(FocusNode.EnumSupplyType.TRAJECTORY);}
        return node;
    }
}
