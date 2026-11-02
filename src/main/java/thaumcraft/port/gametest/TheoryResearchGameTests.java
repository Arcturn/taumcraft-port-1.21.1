package thaumcraft.port.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.research.ResearchCatalog;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.theory.*;

/** Original shared-table transactions, inventories, tag equivalents and scripted discoveries. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class TheoryResearchGameTests {
    @GameTest(template="empty",timeoutTicks=80)
    public static void anotherPlayerCanResumeDrawAndFinishTheSharedTable(GameTestHelper helper) {
        var table=table(helper);var author=player(helper,table);var visitor=player(helper,table);
        var session=new TheorySession(author.getUUID(),5,List.of());
        restore(table,session);
        var menu=menu(visitor,table,31);
        menu.action(visitor,request(menu,table,1));
        var drawn=session(table);
        helper.assertTrue(drawn.owner.equals(visitor.getUUID()),"Original last-drawer identity did not follow the player");
        helper.assertTrue(!drawn.choices.isEmpty(),"Another player could not continue a shared theory");
        helper.assertValueEqual(table.getItem(1).getCount(),3,"Drawing must consume exactly one paper");
        var pending=new TheorySession(author.getUUID(),1,List.of());
        pending.choices.add(TheoryCards.initialize("Calibrate",41,true,pending,TheoryItems.context(visitor)));
        restore(table,pending);
        menu.action(visitor,request(menu,table,2));
        helper.assertTrue(session(table).complete(),"Another player could not select the shared card");
        helper.assertValueEqual(table.ink(),99,"Selected card must consume one ink");
        menu.action(visitor,request(menu,table,6));
        helper.assertTrue(!table.saveCustomOnly(table.getLevel().registryAccess()).contains("TheoryV1"),"Finished theory remained on the table");
        helper.assertValueEqual(ResearchStorage.get(visitor.getServer()).player(visitor.getUUID()).knowledge("THEORY","ARTIFICE"),5,"Finisher did not receive the original rounded 15% reward");
        helper.assertValueEqual(ResearchStorage.get(author.getServer()).player(author.getUUID()).knowledge("THEORY","ARTIFICE"),0,"Reward incorrectly went to the old owner");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void finishRequiresSpentInspirationButNoSuppliesOrAcknowledgment(GameTestHelper helper) {
        var table=table(helper);var player=player(helper,table);var menu=menu(player,table,35);
        var pending=new TheorySession(player.getUUID(),1,List.of());
        pending.add("ARTIFICE",100);
        pending.choices.add(TheoryCards.initialize("Calibrate",42,true,pending,TheoryItems.context(player)));
        restore(table,pending);
        table.getItem(0).setDamageValue(table.getItem(0).getMaxDamage()-1);
        table.setItem(1,ItemStack.EMPTY);
        var state=ResearchStorage.get(player.getServer()).player(player.getUUID());
        var unfinished=table.saveCustomOnly(table.getLevel().registryAccess());
        menu.action(player,request(menu,table,6));
        helper.assertTrue(unfinished.equals(table.saveCustomOnly(table.getLevel().registryAccess())),"100% category progress finished a theory with inspiration remaining");
        helper.assertValueEqual(state.knowledge("THEORY","ARTIFICE"),0,"Rejected finish awarded knowledge");

        menu.action(player,request(menu,table,2));
        helper.assertValueEqual(session(table).inspiration,0,"The final card did not spend the last inspiration");
        helper.assertValueEqual(table.ink(),0,"The final card did not exhaust the last ink");
        helper.assertTrue(table.getItem(1).isEmpty(),"Selecting Calibrate unexpectedly supplied paper");
        var completed=table.saveCustomOnly(table.getLevel().registryAccess());
        table.loadCustomOnly(completed,table.getLevel().registryAccess());
        helper.assertTrue(session(table).complete()&&session(table).selected==0,"Reload lost completion before the card animation acknowledgment");

        var finish=request(menu,table,6);
        menu.action(player,finish);
        helper.assertFalse(table.saveCustomOnly(table.getLevel().registryAccess()).contains("TheoryV1"),"Zero inspiration could not finish without ink, paper or acknowledgment");
        helper.assertValueEqual(state.knowledge("THEORY","ARTIFICE"),37,"Finish did not award the original rounded 115% reward");
        menu.action(player,finish);
        helper.assertValueEqual(state.knowledge("THEORY","ARTIFICE"),37,"A replayed finish awarded the same theory twice");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void missingInputsAndReplayedCardRequestsCannotChargeOrRewardTwice(GameTestHelper helper) {
        var table=table(helper);var player=player(helper,table);var menu=menu(player,table,32);
        var pending=new TheorySession(player.getUUID(),2,List.of());
        var card=TheoryCards.initialize("Synthesis",17,true,pending,TheoryItems.context(player));
        pending.choices.add(card);restore(table,pending);
        player.getInventory().setItem(9,TheoryItems.stack(card.required().get(0)));
        var original=table.saveCustomOnly(table.getLevel().registryAccess());
        menu.action(player,request(menu,table,2));
        helper.assertTrue(original.equals(table.saveCustomOnly(table.getLevel().registryAccess())),"Missing second ingredient changed the theory or supplies");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(),1,"Failed activation consumed first crystal");
        player.getInventory().setItem(10,TheoryItems.stack(card.required().get(1)));
        var accepted=request(menu,table,2);
        menu.action(player,accepted);
        helper.assertValueEqual(table.ink(),99,"Synthesis did not consume exactly one ink");
        helper.assertTrue(player.getInventory().getItem(9).isEmpty()&&player.getInventory().getItem(10).isEmpty(),"Synthesis did not consume both reserved crystals");
        helper.assertValueEqual(count(player,TheoryCards.Requirement.crystal(Aspect.byId(card.aspect()).orElseThrow(),false)),1,"Synthesis did not produce exactly one compound crystal");
        var after=table.saveCustomOnly(table.getLevel().registryAccess());
        menu.action(player,accepted);
        helper.assertTrue(after.equals(table.saveCustomOnly(table.getLevel().registryAccess())),"A replayed packet changed the selected card");
        helper.assertValueEqual(count(player,TheoryCards.Requirement.crystal(Aspect.byId(card.aspect()).orElseThrow(),false)),1,"A replayed packet duplicated the compound crystal");
        var copy=new ResearchTableEntity(table.getBlockPos(),table.getBlockState());copy.setLevel(table.getLevel());
        copy.loadCustomOnly(after,table.getLevel().registryAccess());
        helper.assertTrue(session(copy).encode().equals(session(table).encode()),"Saving a selected card changed its seeded parameters or selection");
        helper.assertValueEqual(copy.ink(),99,"Reload restored spent ink");
        menu.action(player,request(menu,table,5));
        helper.assertTrue(session(table).choices.isEmpty()&&session(table).lastDraw.equals(card),"Acknowledging did not preserve the accepted card history");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void cardItemsAcceptOriginalOreGroupsAndKeepAspectAndDamageRestrictions(GameTestHelper helper) {
        var wool=TheoryCards.Requirement.item("minecraft:white_wool",true);
        helper.assertTrue(TheoryItems.matches(new ItemStack(Items.RED_WOOL),wool),"Original wool OreDictionary alternatives were lost");
        helper.assertTrue(!TheoryItems.matches(new ItemStack(Items.WHITE_CARPET),wool),"A carpet was accepted as wool");
        var nitor=TheoryCards.Requirement.item("thaumcraft:nitor_yellow",true);
        helper.assertTrue(TheoryItems.matches(new ItemStack(MachineContent.NITOR.get("nitor_blue").get()),nitor),"Another nitor colour did not satisfy the card");
        helper.assertTrue(!TheoryItems.matches(new ItemStack(Items.GOLD_INGOT),TheoryCards.Requirement.item("minecraft:iron_ingot",true)),"Broad ingot membership bypassed the requested metal");
        helper.assertTrue(!TheoryItems.matches(CrystalItem.stack(Aspect.FIRE),TheoryCards.Requirement.crystal(Aspect.WATER,true)),"Tag matching bypassed a crystal aspect");
        var bow=new ItemStack(Items.BOW);bow.setDamageValue(1);
        helper.assertTrue(!TheoryItems.matches(bow,TheoryCards.Requirement.item("minecraft:bow",true)),"Damaged tool bypassed original exact metadata");
        var table=table(helper);var player=player(helper,table);var menu=menu(player,table,33);
        for(int i=0;i<9;i++){
            var slot=menu.getSlot(29+i);
            helper.assertValueEqual(slot.getSlotIndex(),i/3+i%3*3,"Original column-major hotbar slot order changed");
            helper.assertValueEqual(slot.x,20+i/3*18,"Wrong hotbar X");
            helper.assertValueEqual(slot.y,190+i%3*18,"Wrong hotbar Y");
        }
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void crimsonRitesRespectsDiscoveryParentsAndDoesNotSkipAnAlreadyStartedEntry(GameTestHelper helper) {
        var table=table(helper);var blocked=player(helper,table);var state=ResearchStorage.get(blocked.getServer()).player(blocked.getUUID());
        state.warp(0,21,0);useRites(blocked);
        helper.assertValueEqual(state.stage("CrimsonRites"),0,"Using a curio bypassed the missing cultist parent");
        helper.assertValueEqual(blocked.totalExperience,0,"Failed scripted completion rewarded XP");
        helper.assertTrue(blocked.getMainHandItem().isEmpty(),"Successful curio knowledge use did not consume its item");
        var started=player(helper,table);var startedState=ResearchStorage.get(started.getServer()).player(started.getUUID());
        startedState.stage("!CrimsonCultist",2,false);startedState.stage("CrimsonRites",1,false);startedState.warp(0,21,0);useRites(started);
        helper.assertValueEqual(startedState.stage("CrimsonRites"),1,"Curio completed an already-known research entry unlike TC6");
        helper.assertValueEqual(started.totalExperience,0,"Known research was rewarded again");
        var fresh=player(helper,table);var freshState=ResearchStorage.get(fresh.getServer()).player(fresh.getUUID());
        freshState.stage("!CrimsonCultist",2,false);freshState.warp(0,21,0);useRites(fresh);
        helper.assertValueEqual(freshState.stage("CrimsonRites"),ResearchCatalog.get("CrimsonRites").stages().size()+1,"Valid scripted discovery did not complete the original entry");
        helper.assertValueEqual(fresh.totalExperience,10,"Valid scripted completion did not grant the two original transitions");
        useRites(fresh);helper.assertValueEqual(fresh.totalExperience,10,"Repeated curio use duplicated research XP");
        helper.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=80)
    public static void infusionUnlockEnablesTheoryBeforeItsMatrixResearch(GameTestHelper helper) {
        var table=table(helper);var player=player(helper,table);var state=ResearchStorage.get(player.getServer()).player(player.getUUID());
        state.flag("UNLOCKALCHEMY");state.flag("!auram");state.flag("!praecantatio");
        state.stage("UNLOCKINFUSION",1,false);state.addKnowledge("OBSERVATION","INFUSION",16);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PortContent.THAUMONOMICON.get()));
        var session=new TheorySession(player.getUUID(),5,List.of());
        helper.assertFalse(session.availableCategories(state).contains("INFUSION"),"An unfinished category unlock enabled Infusion cards");
        thaumcraft.port.research.ResearchProgress.advance(player,"UNLOCKINFUSION",1);
        helper.assertTrue(state.knows("UNLOCKINFUSION"),"Paying the actual book requirements did not finish the unlock");
        helper.assertTrue(session.availableCategories(state).contains("INFUSION"),"Completed unlock did not enable ordinary Infusion cards");
        helper.assertFalse(state.isResearchKnown("INFUSION"),"Category unlock incorrectly completed matrix research");
        helper.assertFalse(table.nearbyAids().contains("BasicInfusion"),"An unlocked category fabricated a nearby matrix aid");
        helper.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=80)
    public static void matrixAidUsesOriginalHorizontalAndVerticalRangeAndStartsItsCardPool(GameTestHelper helper) {
        var table=table(helper);var player=player(helper,table);var matrix=thaumcraft.port.infusion.matrix.MatrixContent.MATRIX.get().defaultBlockState();
        BlockPos inside=table.getBlockPos().offset(4,1,0),high=table.getBlockPos().offset(4,2,0),far=table.getBlockPos().offset(5,1,0);
        helper.getLevel().setBlock(high,matrix,3);
        helper.assertFalse(table.nearbyAids().contains("BasicInfusion"),"Matrix two blocks above table escaped the source vertical boundary");
        helper.getLevel().removeBlock(high,false);helper.getLevel().setBlock(far,matrix,3);
        helper.assertFalse(table.nearbyAids().contains("BasicInfusion"),"Matrix five blocks away escaped the source horizontal boundary");
        helper.getLevel().removeBlock(far,false);helper.getLevel().setBlock(inside,matrix,3);
        helper.assertTrue(table.nearbyAids().contains("BasicInfusion"),"Matrix on the source scan boundary was not detected");
        var menu=menu(player,table,34);menu.broadcastChanges();
        helper.assertTrue(menu.aids.contains("BasicInfusion"),"Detected matrix aid was not offered by the server menu");
        menu.action(player,new TheoryActionPayload(menu.containerId,menu.token,table.saveCustomOnly(table.getLevel().registryAccess()).getLong("TheoryRevision"),0,"BasicInfusion"));
        var started=session(table);
        helper.assertTrue(started.aids.contains("BasicInfusion"),"Selecting a real nearby matrix aid did not start a theory");
        helper.assertTrue(started.aidCards.equals(List.of("Measure","Channel","Infuse")),"Matrix aid lost its exact original Infusion card pool");
        helper.getLevel().removeBlock(inside,false);helper.succeed();
    }

    private static ResearchTableEntity table(GameTestHelper helper) {
        var pos=helper.absolutePos(new BlockPos(1,2,1));
        helper.getLevel().setBlock(pos,TheoryContent.TABLE.get().defaultBlockState(),3);
        var table=(ResearchTableEntity)helper.getLevel().getBlockEntity(pos);
        table.setItem(0,new ItemStack(PortContent.SCRIBING_TOOLS.get()));table.setItem(1,new ItemStack(Items.PAPER,4));return table;
    }
    private static ServerPlayer player(GameTestHelper helper,ResearchTableEntity table) {
        var player=GameTestPlayers.create(helper);var pos=table.getBlockPos();player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);return player;
    }
    private static TheoryMenu menu(ServerPlayer player,ResearchTableEntity table,int id) {
        var menu=new TheoryMenu(id,player.getInventory(),table,table.getBlockPos());player.containerMenu=menu;return menu;
    }
    private static TheoryActionPayload request(TheoryMenu menu,ResearchTableEntity table,int action) {
        return new TheoryActionPayload(menu.containerId,menu.token,table.saveCustomOnly(table.getLevel().registryAccess()).getLong("TheoryRevision"),action,"");
    }
    private static void restore(ResearchTableEntity table,TheorySession session) {
        var tag=table.saveCustomOnly(table.getLevel().registryAccess());tag.putString("TheoryV1",session.encode());table.loadCustomOnly(tag,table.getLevel().registryAccess());
    }
    private static TheorySession session(ResearchTableEntity table) {
        return TheorySession.decode(table.saveCustomOnly(table.getLevel().registryAccess()).getString("TheoryV1"));
    }
    private static int count(ServerPlayer player,TheoryCards.Requirement item) {
        return player.getInventory().items.stream().filter(s->TheoryItems.matches(s,item)).mapToInt(ItemStack::getCount).sum();
    }
    private static void useRites(ServerPlayer player) {
        var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:curio_rites")));
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);stack.getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
    }
    private TheoryResearchGameTests() {}
}
