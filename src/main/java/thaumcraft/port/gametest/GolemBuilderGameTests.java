package thaumcraft.port.gametest;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.golem.*;
import thaumcraft.port.golem.part.*;
import thaumcraft.port.golem.builder.*;
import thaumcraft.port.research.ResearchStorage;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class GolemBuilderGameTests {
 @GameTest(template="elemental_tools_empty",timeoutTicks=40)
 public static void allSourcePartCombinationsKeepPackedIndicesAndComponents(GameTestHelper h){
  h.assertValueEqual(GolemMaterial.getMaterials().length,6,"Source material count");h.assertValueEqual(GolemHead.getHeads().length,5,"Source head count");h.assertValueEqual(GolemArm.getArms().length,5,"Source arm count");h.assertValueEqual(GolemLeg.getLegs().length,4,"Source leg count");h.assertValueEqual(GolemAddon.getAddons().length,4,"Source addon count");int count=0;
  for(var material:GolemMaterial.getMaterials())for(var head:GolemHead.getHeads())for(var arms:GolemArm.getArms())for(var legs:GolemLeg.getLegs())for(var addon:GolemAddon.getAddons()){
   var p=new GolemProperties();p.setMaterial(material);p.setHead(head);p.setArms(arms);p.setLegs(legs);p.setAddon(addon);p.setRank(7);long expected=(material.id&255L)|((head.id&255L)<<8)|((arms.id&255L)<<16)|((legs.id&255L)<<24)|((addon.id&255L)<<32)|(7L<<40);
   h.assertTrue(p.toLong()==expected,"Packed byte layout changed");var copy=GolemProperties.fromLong(expected);h.assertTrue(copy.getTraits().equals(p.getTraits()),"Traits changed during save/load");for(var part:copy.generateComponents())h.assertTrue(!part.isEmpty()&&part.getCount()>0,"Unregistered or empty construction component");count++;
  }h.assertValueEqual(count,2400,"Not all source combinations checked");var basic=new GolemProperties();h.assertValueEqual(basic.constructionCost(),8,"Basic wooden golem must require 8 Machina");basic.setAddon(GolemAddon.getAddons()[1]);h.assertFalse(basic.hasTrait(EnumGolemTrait.LIGHT)||basic.hasTrait(EnumGolemTrait.HEAVY),"Opposed traits did not cancel");h.succeed();
 }
 @GameTest(template="elemental_tools_empty",timeoutTicks=40)
 public static void pressPaysOnceWaitsForMachinaAndPersistsBufferedProgress(GameTestHelper h){
  var p=GameTestPlayers.create(h);ResearchStorage.get(p.getServer()).player(p.getUUID()).completeCatalog();var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,GolemContent.BUILDER.get().defaultBlockState());p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);var b=(GolemBuilderEntity)h.getLevel().getBlockEntity(pos);var props=new GolemProperties();int slot=0;for(var item:props.generateComponents())p.getInventory().setItem(slot++,item.copy());
  h.assertTrue(b.startCraft(0,p),"Valid press craft rejected");h.assertFalse(b.startCraft(0,p),"Repeated start replaced active paid craft");h.assertTrue(p.getInventory().isEmpty(),"Parts not consumed exactly once");int total=b.cost;for(int i=0;i<30;i++)b.tick();h.assertValueEqual(b.cost,total,"Press advanced without essentia");
  h.assertValueEqual(b.insert(Aspect.MECHANISM,1,Direction.UP,false),0,"Top must not accept essentia");h.assertValueEqual(b.insert(Aspect.MAGIC,1,Direction.DOWN,false),0,"Wrong aspect accepted");h.assertValueEqual(b.insert(Aspect.MECHANISM,100,Direction.DOWN,true),1,"Simulation rejected Machina");h.assertValueEqual(b.insert(Aspect.MECHANISM,100,Direction.DOWN,false),1,"Buffer must accept only one Machina");
  var saved=b.saveWithoutMetadata(h.getLevel().registryAccess());var loaded=new GolemBuilderEntity(pos,b.getBlockState());loaded.setLevel(h.getLevel());loaded.loadWithComponents(saved,h.getLevel().registryAccess());for(int i=0;i<5;i++)loaded.tick();h.assertValueEqual(loaded.cost,total-1,"Saved buffer lost paid essentia or consumed twice");
  while(loaded.cost>0){if(loaded.cost==1)loaded.setItem(0,new ItemStack(Items.STONE));h.assertValueEqual(loaded.insert(Aspect.MECHANISM,1,Direction.NORTH,false),1,"Machina input stopped");for(int i=0;i<5;i++)loaded.tick();}
  h.assertTrue(loaded.busy()&&loaded.getItem(0).is(Items.STONE),"Blocked output lost paid craft or overwrote another item");loaded.removeItem(0,64);for(int i=0;i<5;i++)loaded.tick();
  var out=loaded.getItem(0);h.assertTrue(out.is(GolemContent.GOLEM_ITEM.get())&&out.getCount()==1,"Press did not produce exactly one golem");h.assertValueEqual(out.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getLong("props"),0L,"Output properties changed");h.assertFalse(loaded.busy(),"Completed press still busy");for(int i=0;i<20;i++)loaded.tick();h.assertValueEqual(out.getCount(),1,"Completed press duplicated output");h.succeed();
 }
 @GameTest(template="elemental_tools_empty",timeoutTicks=40)
 public static void pressUsesAdjacentPartsAndRejectsPartialPlayerFallback(GameTestHelper h){
  var p=GameTestPlayers.create(h);ResearchStorage.get(p.getServer()).player(p.getUUID()).completeCatalog();var pos=h.absolutePos(new BlockPos(4,1,4));h.getLevel().setBlockAndUpdate(pos,GolemContent.BUILDER.get().defaultBlockState());p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);var b=(GolemBuilderEntity)h.getLevel().getBlockEntity(pos);
  h.getLevel().setBlockAndUpdate(pos.west(),Blocks.CHEST.defaultBlockState());h.getLevel().setBlockAndUpdate(pos.above(),Blocks.CHEST.defaultBlockState());var chest=(net.minecraft.world.Container)h.getLevel().getBlockEntity(pos.west());var above=(net.minecraft.world.Container)h.getLevel().getBlockEntity(pos.above());var parts=new GolemProperties().generateComponents();
  chest.setItem(0,parts[0].copyWithCount(2));p.getInventory().setItem(0,parts[0].copyWithCount(1));above.setItem(0,parts[0].copy());for(int i=1;i<parts.length;i++)p.getInventory().setItem(i,parts[i].copy());
  h.assertFalse(b.startCraft(0,p),"Partial adjacent and player ingredient were combined, or top inventory was used");h.assertValueEqual(chest.getItem(0).getCount(),2,"Rejected craft paid adjacent parts");h.assertValueEqual(p.getInventory().getItem(0).getCount(),1,"Rejected craft paid personal parts");
  chest.setItem(0,parts[0].copy());h.assertTrue(b.startCraft(0,p),"Whole adjacent ingredient with personal remaining ingredients rejected");h.assertTrue(chest.getItem(0).isEmpty(),"Adjacent ingredient not consumed");h.assertValueEqual(above.getItem(0).getCount(),parts[0].getCount(),"Top inventory was consumed");h.assertValueEqual(p.getInventory().getItem(0).getCount(),1,"Unused personal fallback was consumed");for(int i=1;i<parts.length;i++)h.assertTrue(p.getInventory().getItem(i).isEmpty(),"Personal ingredient not consumed");h.succeed();
 }
 @GameTest(template="elemental_tools_empty",timeoutTicks=40)
 public static void pressRejectsMissingResearchAndForgedRankWithoutPayment(GameTestHelper h){
  var p=GameTestPlayers.create(h);var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,GolemContent.BUILDER.get().defaultBlockState());p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);var b=(GolemBuilderEntity)h.getLevel().getBlockEntity(pos);var props=new GolemProperties();int slot=0;for(var item:props.generateComponents())p.getInventory().setItem(slot++,item.copy());
  h.assertFalse(b.startCraft(0,p),"Research bypass accepted");h.assertFalse(p.getInventory().isEmpty(),"Rejected craft consumed materials");ResearchStorage.get(p.getServer()).player(p.getUUID()).completeCatalog();props.setRank(10);h.assertFalse(b.startCraft(props.toLong(),p),"Fresh rank10 golem accepted");h.assertFalse(b.startCraft(255,p),"Invalid material id accepted");h.assertFalse(b.busy(),"Rejected request started press");h.succeed();
 }
 @GameTest(template="elemental_tools_empty",timeoutTicks=40)
 public static void pressMenuRetainsSelectionAndShowsActiveCraftOnReopen(GameTestHelper h){
  var p=GameTestPlayers.create(h);ResearchStorage.get(p.getServer()).player(p.getUUID()).completeCatalog();var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,GolemContent.BUILDER.get().defaultBlockState());p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);var b=(GolemBuilderEntity)h.getLevel().getBlockEntity(pos);
  var first=new GolemBuilderMenu(1,p.getInventory(),b);p.containerMenu=first;h.assertTrue(first.clickMenuButton(p,3)&&first.clickMenuButton(p,1),"Researched selectors rejected");long selected=first.selected().toLong();var reopened=new GolemBuilderMenu(2,p.getInventory(),b);h.assertValueEqual(reopened.selected().toLong(),selected,"Reopening forgot part selection");
  var active=new GolemProperties();active.setMaterial(GolemMaterial.getMaterials()[5]);active.setArms(GolemArm.getArms()[4]);b.golem=active.toLong();b.cost=b.maxCost=active.constructionCost();var working=new GolemBuilderMenu(3,p.getInventory(),b);p.containerMenu=working;h.assertValueEqual(working.selected().toLong(),active.toLong(),"Working press displayed another recipe");h.assertFalse(working.clickMenuButton(p,3),"Active craft selection changed");h.succeed();
 }
 @GameTest(template="elemental_tools_empty",timeoutTicks=40)
 public static void pressBlueprintFormsAllRotationsAndRestoresRemainingParts(GameTestHelper h){
  var center=h.absolutePos(new BlockPos(4,1,4));for(Direction face:Direction.Plane.HORIZONTAL){
   for(int x=-2;x<=2;x++)for(int y=0;y<=1;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(center.offset(x,y,z),Blocks.AIR.defaultBlockState());
   h.getLevel().setBlockAndUpdate(center,Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.UP));h.getLevel().setBlockAndUpdate(center.above(),Blocks.IRON_BARS.defaultBlockState());
   var cauldron=GolemBuilderStructure.relative(center,face,-1,0,0);var anvil=GolemBuilderStructure.relative(center,face,-1,0,1);var table=GolemBuilderStructure.relative(center,face,0,0,1);
   h.getLevel().setBlockAndUpdate(cauldron,Blocks.CAULDRON.defaultBlockState());h.getLevel().setBlockAndUpdate(anvil,Blocks.ANVIL.defaultBlockState());h.getLevel().setBlockAndUpdate(table,thaumcraft.port.crafting.content.CraftingContent.TABLE_STONE.get().defaultBlockState());
   h.assertTrue(GolemBuilderStructure.match(h.getLevel(),center,face)!=null,"Source blueprint rotation rejected");h.assertTrue(GolemBuilderStructure.form(h.getLevel(),center,face),"Press formation failed");h.assertTrue(h.getLevel().getBlockState(center).is(GolemContent.BUILDER.get()),"Piston did not become press");h.getLevel().setBlockAndUpdate(anvil,Blocks.AIR.defaultBlockState());
   h.assertTrue(h.getLevel().getBlockState(center).is(Blocks.PISTON),"Core not reverted");h.assertTrue(h.getLevel().getBlockState(cauldron).is(Blocks.CAULDRON)&&h.getLevel().getBlockState(center.above()).is(Blocks.IRON_BARS),"Remaining parts not restored");h.assertTrue(h.getLevel().getBlockState(anvil).isAir(),"Broken shell was duplicated by restoration");
  }h.succeed();
 }
 private GolemBuilderGameTests(){}
}
