package thaumcraft.port.auromancy.focal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import thaumcraft.port.auromancy.focus.FocusEngine;
import thaumcraft.port.auromancy.focus.FocusMedium;
import thaumcraft.port.auromancy.focus.FocusMediumRoot;
import thaumcraft.port.auromancy.focus.FocusModSplit;
import thaumcraft.port.auromancy.focus.FocusNode;
import thaumcraft.port.auromancy.focus.FocusPackage;

/** Validates editor trees before compiling the original linear/split package representation. */
public final class FocalGraph {
    /** Blank leaf sockets are valid edits; only a completed graph can be compiled. */
    public static void validateEdit(Map<Integer,FocusElementNode> graph) {
        if(graph==null||graph.size()>FocusPackage.MAX_NODES)fail("Invalid focus graph size");
        if(graph.isEmpty())return;
        var root=graph.get(0);
        if(root==null||!(root.node instanceof FocusMediumRoot)||root.parent!=-1||root.y!=0)fail("Missing focus root");
        int media=0,exclusiveMedia=0;var exclusiveKeys=new HashSet<String>();
        for(var entry:graph.entrySet()) {
            var node=entry.getValue();
            if(node==null||entry.getKey()==null||entry.getKey()!=node.id||node.id<0||node.id>1_000_000)fail("Invalid focus node id");
            if(Math.abs((long)node.x)>FocusPackage.MAX_NODES||node.y<0||node.y>=FocusPackage.MAX_NODES||node.children==null||node.children.length>2)fail("Invalid focus node layout");
            if(node.id!=0&&(node.parent<0||!graph.containsKey(node.parent)||node.node instanceof FocusMediumRoot))fail("Invalid focus parent/root");
            if(node.node==null) {
                if(node.id==0||node.children.length!=0||node.target||node.trajectory)fail("Invalid empty focus socket");
            } else {
                if(FocusEngine.elements.get(node.node.getKey())!=node.node.getClass())fail("Unknown focus node class");
                boolean target=node.node.canSupply(FocusNode.EnumSupplyType.TARGET),trajectory=node.node.canSupply(FocusNode.EnumSupplyType.TRAJECTORY);
                if(node.target!=target||node.trajectory!=trajectory)fail("Invalid focus supply flags");
                int children=target||trajectory?(node.node instanceof FocusModSplit?2:1):0;
                if(node.children.length!=children)fail("Invalid focus branch count");
                for(String key:node.node.getSettingList())if(!node.node.getSetting(key).isValidValue(node.node.getSettingValue(key)))fail("Invalid focus setting "+key);
                if(node.node.getComplexity()<0||!Float.isFinite(node.node.getPowerMultiplier())||node.node.getPowerMultiplier()<0)fail("Invalid focus complexity/power");
                if(node.node.isExclusive()&&!exclusiveKeys.add(node.node.getKey()))fail("Duplicate exclusive focus element");
                if(node.node instanceof FocusMedium&&!(node.node instanceof FocusMediumRoot)){media++;if(node.node.isExclusive())exclusiveMedia++;}
            }
            var seenChildren=new HashSet<Integer>();
            for(int childId:node.children) {
                var child=graph.get(childId);
                if(!seenChildren.add(childId)||child==null||child.parent!=node.id||child.y!=node.y+1)fail("Broken focus child link");
                if(child.node!=null) {
                    var required=child.node.mustBeSupplied();boolean supplied=false;
                    if(node.node!=null&&required!=null)for(var supply:required)if(node.node.canSupply(supply)){supplied=true;break;}
                    if(!supplied)fail("Missing focus supply");
                }
            }
        }
        if(exclusiveMedia>0&&media>1)fail("Exclusive focus medium cannot be combined");
        var visited=new HashSet<Integer>();visit(graph,root,visited,0);
        if(visited.size()!=graph.size())fail("Disconnected focus graph");
        complexity(graph);
    }
    private static void visit(Map<Integer,FocusElementNode> graph,FocusElementNode node,Set<Integer> visited,int splitDepth) {
        if(!visited.add(node.id)||splitDepth>FocusPackage.MAX_DEPTH)fail("Cyclic/deep focus graph");
        int nextDepth=splitDepth+(node.node instanceof FocusModSplit?1:0);
        for(int child:node.children)visit(graph,graph.get(child),visited,nextDepth);
    }
    /** Keep HashMap.values() iteration and truncate after each duplicate-weighted addition, as BETA26 does. */
    public static int complexity(Map<Integer,FocusElementNode> graph) {
        if(graph==null||graph.size()>FocusPackage.MAX_NODES)fail("Invalid focus graph size");
        int total=0;var occurrences=new HashMap<String,Integer>();
        for(var node:graph.values())if(node!=null&&node.node!=null) {
            node.complexityMultiplier=.5f*(occurrences.merge(node.node.getKey(),1,Integer::sum)+1);
            float cost=node.node.getComplexity()*node.complexityMultiplier;
            if(!Float.isFinite(cost)||cost<0||total+cost>1_000_000)fail("Invalid focus complexity");
            total=(int)(total+cost);
        }
        return total;
    }
    public static FocusPackage compile(Map<Integer,FocusElementNode> graph) {
        validateEdit(graph);
        if(graph.isEmpty())throw new IllegalArgumentException("Empty focus graph");
        for(var node:graph.values())if(node.node==null)fail("Incomplete focus graph");
        var result=new FocusPackage();result.setComplexity(complexity(graph));append(graph,graph.get(0),result);
        var validation=result.validate(Integer.MAX_VALUE,null);
        if(!validation.valid())fail(validation.reason());
        return result;
    }
    private static void append(Map<Integer,FocusElementNode> graph,FocusElementNode editor,FocusPackage destination) {
        var copy=(FocusNode)FocusEngine.getElement(editor.node.getKey());
        for(String key:editor.node.getSettingList())copy.getSetting(key).setValue(editor.node.getSettingValue(key));
        destination.addNode(copy);
        if(editor.children.length==1)append(graph,graph.get(editor.children[0]),destination);
        else if(editor.children.length==2) {
            var split=(FocusModSplit)copy;
            for(int child:editor.children){var branch=new FocusPackage();append(graph,graph.get(child),branch);split.getSplitPackages().add(branch);}
        }
    }
    private static void fail(String message){throw new IllegalArgumentException(message);}
    private FocalGraph() { }
}
