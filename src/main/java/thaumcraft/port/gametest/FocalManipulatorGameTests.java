package thaumcraft.port.gametest;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.focal.*;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.auromancy.focus.medium.*;
import thaumcraft.port.auromancy.focus.effect.*;
import thaumcraft.port.auromancy.item.FocusItem;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.crafting.content.CraftingContent;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.AuraSystem;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class FocalManipulatorGameTests {
    private static FocalManipulatorEntity table(GameTestHelper h){h.setBlock(new BlockPos(1,2,1),CraftingContent.WAND_WORKBENCH.get());return (FocalManipulatorEntity)h.getBlockEntity(new BlockPos(1,2,1));}
    private static ItemStack focus(){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:focus_1")));}
    private static Map<Integer,FocusElementNode> graph(){
        var result=new HashMap<Integer,FocusElementNode>();
        FocusNode[] nodes={new FocusMediumRoot(),new FocusMediumTouch(),new FocusEffectAir()};
        for(int i=0;i<nodes.length;i++){
            var node=new FocusElementNode();node.id=i;node.x=0;node.y=i;node.parent=i-1;node.children=i==nodes.length-1?new int[0]:new int[]{i+1};node.node=nodes[i];node.node.initialize();node.target=node.node.canSupply(FocusNode.EnumSupplyType.TARGET);node.trajectory=node.node.canSupply(FocusNode.EnumSupplyType.TRAJECTORY);result.put(i,node);
        }
        return result;
    }
    private static void fillCrystals(net.minecraft.server.level.ServerPlayer player,Map<Integer,FocusElementNode> graph){
        int slot=0;for(var e:FocusPackage.crystals(graph.values().stream().map(n->n.node).toList()).entrySet()){var stack=CrystalItem.stack(e.getKey());stack.setCount(e.getValue());player.getInventory().setItem(slot++,stack);}
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void missingCrystalsResearchAndCapacityNeverSpendExperience(GameTestHelper h){
        var table=table(h);var player=GameTestPlayers.create(h);var pos=table.getBlockPos();player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+1.5);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.experienceLevel=30;
        table.setItem(0,focus());var graph=graph();h.assertTrue(table.edit(graph,"Airstream"),"Valid editor graph rejected");
        fillCrystals(player,graph);h.assertFalse(table.startCraft(player),"Unresearched focus was accepted");h.assertValueEqual(player.experienceLevel,30,"Research rejection spent experience");
        ResearchStorage.get(h.getLevel().getServer()).player(player.getUUID()).completeCatalog();player.getInventory().clearContent();
        h.assertFalse(table.startCraft(player),"Missing crystals were accepted");h.assertValueEqual(player.experienceLevel,30,"Missing crystals spent experience");
        h.assertValueEqual(table.vis,0f,"Rejected craft reserved aura");
        var costly=graph();costly.get(2).node=new FocusEffectFire();costly.get(2).node.getSetting("power").setValue(5);costly.get(2).node.getSetting("duration").setValue(5);
        h.assertTrue(table.edit(costly,"Too complex"),"Over-capacity graph should remain editable");fillCrystals(player,costly);
        int before=player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum();
        h.assertFalse(table.startCraft(player),"Over-capacity focus was accepted");h.assertValueEqual(player.experienceLevel,30,"Capacity rejection spent experience");
        h.assertValueEqual(player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum(),before,"Capacity rejection consumed crystals");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void paidProgramSurvivesReloadAndFinishesOnce(GameTestHelper h){
        var table=table(h);var player=GameTestPlayers.create(h);var pos=table.getBlockPos();player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+1.5);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.experienceLevel=30;
        ResearchStorage.get(h.getLevel().getServer()).player(player.getUUID()).completeCatalog();
        var graph=graph();table.setItem(0,focus());table.edit(graph,"Airstream");fillCrystals(player,graph);
        int complexity=table.getTotalComplexity();h.assertTrue(table.startCraft(player),"Configured focus did not begin construction");
        h.assertValueEqual(player.experienceLevel,30-FocusPackage.experienceCost(complexity),"Construction XP cost differs from source");h.assertTrue(player.getInventory().isEmpty(),"Construction did not consume all reserved crystals");
        h.assertValueEqual(table.vis,(float)FocusPackage.constructionVis(complexity,15),"Construction vis cost differs from source");
        h.assertFalse(table.edit(graph,"Changed"),"Paid graph was mutable during construction");h.assertFalse(table.startCraft(player),"Construction could be paid twice");
        var saved=table.saveWithFullMetadata(h.getLevel().registryAccess());var loaded=new FocalManipulatorEntity(pos,table.getBlockState());loaded.setLevel(h.getLevel());loaded.loadWithComponents(saved,h.getLevel().registryAccess());
        var aura=AuraSystem.get(h.getLevel(),pos);aura.addVis(1000);float before=aura.vis();float cost=loaded.vis;
        for(int i=0;i<2000&&loaded.vis>0;i++)FocalManipulatorEntity.tick(loaded);
        var program=FocusItem.getPackage(loaded.getItem(0));h.assertTrue(program!=null&&program.validate(15,null).valid(),"Reload lost the paid focus program");
        h.assertValueEqual(loaded.getItem(0).getHoverName().getString(),"Airstream","Focus lost its paid name");h.assertTrue(Math.abs(aura.vis()-(before-cost))<.001,"Construction drained the wrong aura amount");
        for(int i=0;i<40;i++)FocalManipulatorEntity.tick(loaded);h.assertTrue(Math.abs(aura.vis()-(before-cost))<.001,"Completed focus kept draining aura");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void staleMenuAndInvalidGraphCannotMutateTable(GameTestHelper h){
        var table=table(h);var player=GameTestPlayers.create(h);var pos=table.getBlockPos();player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+1.5);table.setItem(0,focus());
        var graph=graph();table.edit(graph,"Original");var broken=graph();broken.get(1).children=new int[]{0};h.assertFalse(table.edit(broken,"Cyclic"),"Cyclic graph was accepted");h.assertValueEqual(table.focusName,"Original","Rejected graph changed name");
        var menu=new FocalManipulatorMenu(7,player.getInventory(),table);player.containerMenu=menu;var tag=new CompoundTag();tag.put("nodes",FocalManipulatorEntity.writeGraph(graph));tag.putString("focusName","Forged");
        menu.edit(player,new FocalPayloads.Edit(7,"wrong nonce",table.revision,1,tag));h.assertValueEqual(table.focusName,"Original","Unrelated menu token edited focus");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void editedDuplicateCostsKeepSourceHashMapOrder(GameTestHelper h){
        var table=table(h);table.setItem(0,focus());var graph=graph();graph.remove(2);
        var split=graph.get(1);split.node=new thaumcraft.port.auromancy.focus.mod.FocusModSplitTarget();split.children=new int[]{6,10};split.target=true;split.trajectory=false;
        for(int id:new int[]{6,10}){var node=new FocusElementNode();node.id=id;node.parent=1;node.y=2;node.node=new FocusEffectFire();node.node.getSetting("power").setValue(id==6?1:3);graph.put(id,node);}
        h.assertTrue(table.edit(graph,"Ordered"),"Valid split graph was rejected");
        h.assertValueEqual(table.getTotalComplexity(),15,"Map copy reordered repeated fire settings and changed their source price");h.succeed();
    }
    private FocalManipulatorGameTests(){}
}
