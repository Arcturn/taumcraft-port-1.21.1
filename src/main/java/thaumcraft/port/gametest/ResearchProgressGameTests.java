package thaumcraft.port.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.research.*;
import static thaumcraft.port.research.ResearchState.ResearchFlag.*;

/** Real player inventory, XP and SavedData: failed or repeated requests must be inert. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ResearchProgressGameTests {
    @GameTest(template="empty",timeoutTicks=40)
    public static void missingResearchAndKnowledgeAreAtomicThenSuccessConsumesOnce(GameTestHelper helper){
        var player=player(helper);var state=state(player);prepare(state,"REDSTONERELAY",1);
        player.getInventory().setItem(9,new ItemStack(Items.REDSTONE_TORCH,2));state.addKnowledge("OBSERVATION","ARTIFICE",15);state.flag("!potentia");
        var before=ResearchCodec.write(state);ResearchProgress.advance(player,"REDSTONERELAY",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Missing research consumed knowledge or changed state");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(),2,"Missing research consumed an item");
        state.flag("!machina");before=ResearchCodec.write(state);ResearchProgress.advance(player,"REDSTONERELAY",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Insufficient knowledge changed state");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(),2,"Insufficient knowledge consumed an item");
        helper.assertValueEqual(player.totalExperience,0,"Failed progress rewarded XP");
        state.addKnowledge("OBSERVATION","ARTIFICE",1);ResearchProgress.advance(player,"REDSTONERELAY",1);
        helper.assertTrue(state.knows("REDSTONERELAY"),"Valid research did not complete");
        helper.assertValueEqual(state.knowledge("OBSERVATION","ARTIFICE"),0,"Knowledge was not charged exactly once");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(),1,"Item was not charged exactly once");
        helper.assertValueEqual(player.totalExperience,5,"Successful transition must grant original five XP");
        helper.assertTrue(state.hasResearchFlag("REDSTONERELAY",POPUP)&&!state.hasResearchFlag("REDSTONERELAY",RESEARCH),"Page completion flags differ from original noFlags behavior");
        before=ResearchCodec.write(state);ResearchProgress.advance(player,"REDSTONERELAY",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Duplicate request changed research");
        helper.assertValueEqual(player.totalExperience,5,"Duplicate request rewarded XP twice");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(),1,"Duplicate request charged twice");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void missingItemLeavesKnowledgeAndResearchUntouched(GameTestHelper helper){
        var player=player(helper);var state=state(player);prepare(state,"REDSTONERELAY",1);
        state.flag("!potentia");state.flag("!machina");state.addKnowledge("OBSERVATION","ARTIFICE",16);
        var before=ResearchCodec.write(state);ResearchProgress.advance(player,"REDSTONERELAY",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Missing item consumed knowledge or advanced research");
        helper.assertValueEqual(player.totalExperience,0,"Missing item rewarded XP");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void skippedFinalStageAwardsItsWarpOnceAfterRequirementsPass(GameTestHelper helper){
        var player=player(helper);var state=state(player);prepare(state,"JARBRAIN",1);
        state.flag("f_BRAIN");state.addKnowledge("OBSERVATION","GOLEMANCY",16);state.addKnowledge("THEORY","GOLEMANCY",32);
        state.warp(4,6,8);ResearchProgress.advance(player,"JARBRAIN",1);
        helper.assertValueEqual(state.stage("JARBRAIN"),1,"Missing brain advanced research");
        helper.assertValueEqual(state.permanentWarp(),4,"Failed requirements granted permanent warp");
        helper.assertValueEqual(state.normalWarp(),6,"Failed requirements granted normal warp");
        var brain=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:brain"));
        player.getInventory().setItem(9,new ItemStack(brain,2));ResearchProgress.advance(player,"JARBRAIN",1);
        helper.assertTrue(state.knows("JARBRAIN"),"Valid brain research did not complete");
        helper.assertValueEqual(state.permanentWarp(),6,"Final-stage warp 3 must grant two permanent warp");
        helper.assertValueEqual(state.normalWarp(),7,"Final-stage warp 3 must grant one normal warp");
        helper.assertValueEqual(state.temporaryWarp(),8,"Research changed temporary warp");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(),1,"Brain was not consumed exactly once");
        var before=ResearchCodec.write(state);ResearchProgress.advance(player,"JARBRAIN",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Duplicate completion granted warp or changed progress");
        helper.assertValueEqual(player.totalExperience,5,"Duplicate completion granted XP");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void singleRequiredStageAwardsOriginalOutgoingAndFinalWarpOnce(GameTestHelper helper){
        var player=player(helper);var state=state(player);prepare(state,"BOTTLETAINT",1);
        state.addKnowledge("OBSERVATION","ALCHEMY",16);state.addKnowledge("THEORY","ALCHEMY",32);
        ResearchProgress.advance(player,"BOTTLETAINT",1);
        helper.assertTrue(state.knows("BOTTLETAINT"),"Single required stage did not complete");
        helper.assertValueEqual(state.permanentWarp(),2,"Original single-stage outgoing+final warp must grant two permanent");
        helper.assertValueEqual(state.normalWarp(),2,"Original single-stage outgoing+final warp must grant two normal");
        helper.assertValueEqual(state.knowledge("OBSERVATION","ALCHEMY"),0,"Observation was not consumed exactly once");
        helper.assertValueEqual(state.knowledge("THEORY","ALCHEMY"),0,"Theory was not consumed exactly once");
        var before=ResearchCodec.write(state);ResearchProgress.advance(player,"BOTTLETAINT",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Duplicate single-stage completion granted warp");
        helper.assertValueEqual(player.totalExperience,5,"Duplicate single-stage completion granted XP");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void outgoingWarpIsNotDoubledWhenAnEmptyFinalStageCompletesResearch(GameTestHelper helper){
        var player=player(helper);var state=state(player);prepare(state,"UNLOCKELDRITCH",1);
        state.flag("f_VOIDSEED");state.addKnowledge("OBSERVATION","BASICS",16);state.addKnowledge("OBSERVATION","ELDRITCH",16);
        ResearchProgress.advance(player,"UNLOCKELDRITCH",1);
        helper.assertTrue(state.knows("UNLOCKELDRITCH"),"Eldritch unlock did not complete");
        helper.assertValueEqual(state.permanentWarp(),3,"Outgoing warp 5 plus final warp 0 must grant three permanent");
        helper.assertValueEqual(state.normalWarp(),2,"Outgoing warp 5 plus final warp 0 must grant two normal");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void progressReplyUpdatesKnowledgeWithoutReopeningAClosedBook(GameTestHelper helper){
        var player=player(helper);var state=state(player);prepare(state,"REDSTONERELAY",1);
        state.flag("!potentia");state.flag("!machina");state.addKnowledge("OBSERVATION","ARTIFICE",16);
        player.getInventory().setItem(9,new ItemStack(Items.REDSTONE_TORCH));
        var connection=new JournalConnection(player);
        PortContent.THAUMONOMICON.get().use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        helper.assertValueEqual(connection.journals.size(),1,"Using the book must send one opening payload");
        helper.assertTrue(connection.journals.getFirst().open(),"Using the item no longer opens the book");
        helper.assertTrue(state.knows("!gotthaumonomicon"),"Initial item use did not record book discovery");
        // The client may close its screen while this request is travelling to the server.
        thaumcraft.port.network.PortNetwork.handleResearchAction(player,new thaumcraft.port.network.ResearchActionPayload("REDSTONERELAY",1));
        helper.assertValueEqual(connection.journals.size(),2,"Progress must send one synchronized response");
        var reply=connection.journals.getLast();
        helper.assertTrue(!reply.open(),"Delayed progress response would reopen the closed book");
        var received=ResearchCodec.read(reply.progress());
        helper.assertTrue(received.knows("REDSTONERELAY"),"Non-opening response lost completed research");
        helper.assertTrue(received.hasResearchFlag("REDSTONERELAY",POPUP),"Non-opening response lost completion notification");
        helper.assertTrue(received.knows("!gotthaumonomicon"),"Progress response lost initial book discovery");
        helper.assertValueEqual(received.knowledge("OBSERVATION","ARTIFICE"),0,"Response did not synchronize spent knowledge");helper.succeed();
    }

    private static final class JournalConnection extends net.minecraft.server.network.ServerGamePacketListenerImpl {
        final java.util.List<thaumcraft.port.network.JournalPayload> journals=new java.util.ArrayList<>();
        JournalConnection(ServerPlayer player){
            super(player.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false));
        }
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet){
            if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom&&custom.payload() instanceof thaumcraft.port.network.JournalPayload journal)journals.add(journal);
        }
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void craftedRequirementAndReloadPreserveConsumptionAndNotifications(GameTestHelper helper){
        var player=player(helper);var state=state(player);prepare(state,"METALLURGY",1);state.addKnowledge("OBSERVATION","ALCHEMY",16);
        var before=ResearchCodec.write(state);ResearchProgress.advance(player,"METALLURGY",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Missing craft advanced research");
        state.craft(LegacyRequirement.parse("thaumcraft:ingot;1;2").craftKey());ResearchProgress.advance(player,"METALLURGY",1);
        helper.assertValueEqual(state.stage("METALLURGY"),2,"Recorded brass craft did not advance metallurgy");
        helper.assertValueEqual(player.totalExperience,5,"Craft transition XP differs");
        state.addKnowledge("THEORY","ALCHEMY",32);state.craft(LegacyRequirement.parse("thaumcraft:ingot;1;0").craftKey());ResearchProgress.advance(player,"METALLURGY",2);
        helper.assertTrue(state.knows("METALLURGY"),"Recorded thaumium craft did not complete metallurgy");
        helper.assertValueEqual(player.totalExperience,10,"Second successful transition XP differs");
        var storage=ResearchStorage.get(player.getServer());var saved=storage.save(new CompoundTag(),helper.getLevel().registryAccess());
        var loaded=ResearchStorage.load(saved,helper.getLevel().registryAccess()).player(player.getUUID());
        helper.assertTrue(ResearchCodec.write(state).equals(ResearchCodec.write(loaded)),"Save/reload changed stages, consumed knowledge or pending flags");
        helper.assertValueEqual(loaded.knowledge("THEORY","ALCHEMY"),0,"Reload restored spent knowledge");
        ResearchProgress.advance(player,"METALLURGY",2);helper.assertValueEqual(player.totalExperience,10,"Duplicate after save rewarded XP");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void terminalNodeIntentCannotBypassEarlierStages(GameTestHelper helper){
        var player=player(helper);var state=state(player);prepare(state,"METALLURGY",1);var before=ResearchCodec.write(state);
        ResearchProgress.openTerminal(player,"METALLURGY",1);ResearchProgress.openTerminal(player,"METALLURGY",3);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Terminal-node intent bypassed stage validation");
        helper.assertValueEqual(player.totalExperience,0,"Invalid terminal-node request rewarded XP");
        state.stage("METALLURGY",3,false);ResearchProgress.openTerminal(player,"METALLURGY",3);
        helper.assertTrue(state.knows("METALLURGY"),"Known terminal stage did not complete on node open");
        helper.assertTrue(state.hasResearchFlag("METALLURGY",RESEARCH)&&state.hasResearchFlag("METALLURGY",POPUP),"Terminal-node completion lost original notification");
        helper.assertValueEqual(player.totalExperience,5,"Terminal node XP differs");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void acknowledgementNeedsBookAndNeverGrantsProgressOrRewards(GameTestHelper helper){
        var player=player(helper);var state=state(player);prepare(state,"FIRSTSTEPS",2);state.setResearchFlag("FIRSTSTEPS",RESEARCH);state.setResearchFlag("FIRSTSTEPS",PAGE);
        state.addKnowledge("OBSERVATION","BASICS",32);var before=ResearchCodec.write(state);
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);ResearchProgress.acknowledge(player,"FIRSTSTEPS");
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Acknowledgement without a book changed state");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PortContent.THAUMONOMICON.get()));ResearchProgress.acknowledge(player,"FIRSTSTEPS");
        helper.assertTrue(!ResearchView.unread(state,"FIRSTSTEPS"),"Map-node acknowledgement did not clear both flags");
        helper.assertValueEqual(state.stage("FIRSTSTEPS"),2,"Reading advanced a stage");helper.assertValueEqual(state.knowledge("OBSERVATION","BASICS"),32,"Reading spent knowledge");
        helper.assertValueEqual(player.totalExperience,0,"Reading rewarded XP");ResearchProgress.acknowledge(player,"FIRSTSTEPS");
        helper.assertValueEqual(player.totalExperience,0,"Duplicate reading rewarded XP");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void firstOpenSuppressesFirstCompletionBadgeButPreservesSiblingPopupAndXpOnce(GameTestHelper helper){
        var player=player(helper);var state=state(player);state.flag("!gotthaumonomicon");ResearchProgress.advance(player,"FIRSTSTEPS",0);
        helper.assertValueEqual(state.stage("FIRSTSTEPS"),1,"First open did not start research");helper.assertTrue(!ResearchView.unread(state,"FIRSTSTEPS"),"First open marked the page being read unread");
        helper.assertTrue(state.knows("KNOWLEDGETYPES")&&!state.hasResearchFlag("KNOWLEDGETYPES",RESEARCH)&&state.hasResearchFlag("KNOWLEDGETYPES",POPUP),"First sibling completion did not consume original one-shot RESEARCH suppression");
        helper.assertTrue(state.knows("!gotdream"),"Hidden sibling flag was lost");helper.assertValueEqual(player.totalExperience,10,"Parent and completed sibling must each reward five XP");
        ResearchProgress.advance(player,"FIRSTSTEPS",0);helper.assertValueEqual(player.totalExperience,10,"Duplicate first-open rewarded XP");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void addendumChatDeliveredOnceAndNotRepeatedByReadingOrSync(GameTestHelper helper){
        var player=new MessagePlayer(helper);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PortContent.THAUMONOMICON.get()));var state=state(player);
        state.stage("INFERNALFURNACE",ResearchCatalog.get("INFERNALFURNACE").stages().size()+1,false);
        state.stage("BELLOWS",ResearchCatalog.get("BELLOWS").stages().size()+1,false);ResearchProgress.sync(player);
        helper.assertValueEqual(player.addenda,1,"New addendum did not deliver one original chat event");
        helper.assertTrue(player.correctOwner,"Addendum message did not contain the owning research title");
        ResearchProgress.sync(player);ResearchProgress.acknowledge(player,"INFERNALFURNACE");ResearchProgress.sync(player);
        helper.assertValueEqual(player.addenda,1,"Reading or repeated sync replayed addendum chat");
        var loaded=ResearchCodec.read(ResearchCodec.write(state));helper.assertTrue(loaded.drainAddendumMessages().isEmpty(),"Reload re-created an old chat effect");helper.succeed();
    }

    private static final class MessagePlayer extends net.neoforged.neoforge.common.util.FakePlayer {
        int addenda;boolean correctOwner;
        MessagePlayer(GameTestHelper helper){super(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"tc-chat-test"));var pos=helper.absolutePos(net.minecraft.core.BlockPos.ZERO);setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);}
        @Override public void sendSystemMessage(net.minecraft.network.chat.Component message){
            if(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text&&text.getKey().equals("tc.addaddendum")){
                addenda++;var args=text.getArgs();correctOwner=args.length==1&&args[0] instanceof net.minecraft.network.chat.Component title&&title.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name&&name.getKey().equals("research.INFERNALFURNACE.title");
            }
        }
    }

    private static ServerPlayer player(GameTestHelper helper){var player=GameTestPlayers.create(helper);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PortContent.THAUMONOMICON.get()));return player;}
    private static ResearchState state(ServerPlayer player){return ResearchStorage.get(player.getServer()).player(player.getUUID());}
    private static void prepare(ResearchState state,String key,int stage){var entry=ResearchCatalog.get(key);for(String parent:entry.parents()){String clean=ResearchView.clean(parent);var definition=ResearchCatalog.get(clean);if(definition==null)state.flag(clean);else state.stage(clean,definition.stages().size()+1,false);}state.stage(key,stage,false);state.clearPopups();state.clearReadFlags(key);}
    private ResearchProgressGameTests(){}
}
