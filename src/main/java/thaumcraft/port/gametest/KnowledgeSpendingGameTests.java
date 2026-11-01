package thaumcraft.port.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.essentia.EssentiaItem;
import thaumcraft.port.network.JournalPayload;
import thaumcraft.port.network.PortNetwork;
import thaumcraft.port.network.ResearchActionPayload;
import thaumcraft.port.research.ResearchCatalog;
import thaumcraft.port.research.ResearchCodec;
import thaumcraft.port.research.ResearchProgress;
import thaumcraft.port.research.ResearchState;
import thaumcraft.port.research.ResearchStorage;

/** Regression coverage for whole-point costs, fractional balances and the TUBES report. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class KnowledgeSpendingGameTests {
    @GameTest(template="empty",timeoutTicks=40)
    public static void tubesRequireAWholeObservationInEachCategory(GameTestHelper helper) {
        var player=player(helper);var state=state(player);prepareTubes(player,state,1);
        state.addKnowledge("OBSERVATION","ALCHEMY",15);
        state.addKnowledge("OBSERVATION","ARTIFICE",31);
        var before=ResearchCodec.write(state);
        ResearchProgress.advance(player,"TUBES",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"15/16 Alchemy progress paid a whole observation");
        assertPhials(helper,player,1);

        state.addKnowledge("OBSERVATION","ALCHEMY",1);
        state.addKnowledge("OBSERVATION","ARTIFICE",-16);
        before=ResearchCodec.write(state);
        ResearchProgress.advance(player,"TUBES",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Alchemy observations paid the missing Artifice point");
        assertPhials(helper,player,1);
        helper.assertValueEqual(player.totalExperience,0,"Rejected knowledge requirements awarded experience");

        state.addKnowledge("OBSERVATION","ARTIFICE",1);
        ResearchProgress.advance(player,"TUBES",1);
        helper.assertTrue(state.knows("TUBES"),"Two complete observations and all four phials did not complete TUBES");
        helper.assertValueEqual(state.knowledge("OBSERVATION","ALCHEMY"),0,"Exact Alchemy observation was not consumed");
        helper.assertValueEqual(state.knowledge("OBSERVATION","ARTIFICE"),0,"Exact Artifice observation was not consumed");
        assertPhials(helper,player,0);helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void tubesMissingParentDoesNotConsumeAndKnownParentIsNeverSpent(GameTestHelper helper) {
        var player=player(helper);var state=state(player);
        state.stage("TUBES",1,false);givePhials(player,2);
        state.addKnowledge("OBSERVATION","ALCHEMY",23);
        state.addKnowledge("OBSERVATION","ARTIFICE",41);
        var before=ResearchCodec.write(state);
        ResearchProgress.advance(player,"TUBES",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Missing WARDEDJARS consumed knowledge or changed TUBES");
        assertPhials(helper,player,2);

        complete(state,"WARDEDJARS");
        int parentStage=state.stage("WARDEDJARS");
        ResearchProgress.advance(player,"TUBES",1);
        helper.assertTrue(state.knows("TUBES")&&state.knows("WARDEDJARS"),"Completing TUBES consumed a prerequisite research");
        helper.assertValueEqual(state.stage("WARDEDJARS"),parentStage,"Known prerequisite stage was changed by payment");
        helper.assertValueEqual(state.knowledge("OBSERVATION","ALCHEMY"),7,"Alchemy remainder was lost");
        helper.assertValueEqual(state.knowledge("OBSERVATION","ARTIFICE"),25,"Artifice remainder was lost");
        assertPhials(helper,player,1);helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void tubesReplyReopenAndSaveRetainSpentBalancesAndFractions(GameTestHelper helper) {
        var player=player(helper);var state=state(player);prepareTubes(player,state,2);
        state.addKnowledge("OBSERVATION","ALCHEMY",23);
        state.addKnowledge("OBSERVATION","ARTIFICE",41);
        state.addKnowledge("THEORY","ALCHEMY",73);
        state.addKnowledge("OBSERVATION","BASICS",53);
        var connection=new JournalConnection(player);
        PortNetwork.handleResearchAction(player,new ResearchActionPayload("TUBES",1));
        helper.assertValueEqual(connection.journals.size(),1,"Research action did not send exactly one knowledge snapshot");
        helper.assertTrue(!connection.journals.getLast().open(),"Research payment response reopens a closed book");
        assertTubeBalances(helper,ResearchCodec.read(connection.journals.getLast().progress()));
        assertTubeBalances(helper,state);assertPhials(helper,player,1);
        helper.assertValueEqual(player.totalExperience,5,"One paid stage did not grant five experience");

        // A stale click arriving after the request cooldown must also be inert.
        for(int tick=0;tick<4;tick++)player.getCooldowns().tick();
        PortNetwork.handleResearchAction(player,new ResearchActionPayload("TUBES",1));
        helper.assertValueEqual(connection.journals.size(),2,"Stale research click did not return the authoritative snapshot");
        assertTubeBalances(helper,ResearchCodec.read(connection.journals.getLast().progress()));
        assertPhials(helper,player,1);
        helper.assertValueEqual(player.totalExperience,5,"Stale research click repeated the reward");

        ResearchProgress.show(player);
        helper.assertTrue(connection.journals.getLast().open(),"Reopening the book did not send an opening snapshot");
        assertTubeBalances(helper,ResearchCodec.read(connection.journals.getLast().progress()));
        var storage=ResearchStorage.get(player.getServer());
        var saved=storage.save(new CompoundTag(),helper.getLevel().registryAccess());
        var loaded=ResearchStorage.load(saved,helper.getLevel().registryAccess()).player(player.getUUID());
        assertTubeBalances(helper,loaded);
        helper.assertTrue(ResearchCodec.write(state).equals(ResearchCodec.write(loaded)),"Save/reload changed research or currency");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void theoryCostsUseWholePointsAndPreserveUnspentKnowledge(GameTestHelper helper) {
        var player=player(helper);var state=state(player);
        complete(state,"METALLURGY");state.stage("METALPURIFICATION",1,false);
        state.addKnowledge("THEORY","ALCHEMY",63);
        state.addKnowledge("OBSERVATION","ALCHEMY",23);
        state.addKnowledge("THEORY","ARTIFICE",95);
        var before=ResearchCodec.write(state);
        ResearchProgress.advance(player,"METALPURIFICATION",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"1 theory plus 31/32 progress paid a two-theory cost");
        helper.assertValueEqual(player.totalExperience,0,"Insufficient theories awarded experience");

        state.addKnowledge("THEORY","ALCHEMY",8);
        ResearchProgress.advance(player,"METALPURIFICATION",1);
        helper.assertTrue(state.knows("METALPURIFICATION"),"Two full Alchemy theories did not complete purification");
        helper.assertValueEqual(state.knowledge("THEORY","ALCHEMY"),7,"Two-theory payment did not preserve 7/32 progress");
        helper.assertValueEqual(state.knowledge("OBSERVATION","ALCHEMY"),23,"Theory payment spent observations");
        helper.assertValueEqual(state.knowledge("THEORY","ARTIFICE"),95,"Theory payment spent a different category");
        helper.assertTrue(state.knows("METALLURGY"),"Theory payment forgot prerequisite research");
        before=ResearchCodec.write(state);
        ResearchProgress.advance(player,"METALPURIFICATION",1);
        helper.assertTrue(before.equals(ResearchCodec.write(state)),"Duplicate theory payment changed knowledge");
        helper.assertValueEqual(player.totalExperience,5,"Duplicate theory payment awarded experience");
        helper.succeed();
    }

    private static void assertTubeBalances(GameTestHelper helper,ResearchState state) {
        helper.assertTrue(state.knows("TUBES"),"Snapshot lost completed TUBES research");
        helper.assertValueEqual(state.knowledge("OBSERVATION","ALCHEMY"),7,"Spent Alchemy point returned or its 7/16 remainder disappeared");
        helper.assertValueEqual(state.knowledge("OBSERVATION","ARTIFICE"),25,"Spent Artifice point returned or its remaining 1+9/16 changed");
        helper.assertValueEqual(state.knowledge("THEORY","ALCHEMY"),73,"Observation payment changed theories");
        helper.assertValueEqual(state.knowledge("OBSERVATION","BASICS"),53,"Observation payment changed another category");
    }

    private static void prepareTubes(ServerPlayer player,ResearchState state,int count) {
        complete(state,"WARDEDJARS");state.stage("TUBES",1,false);givePhials(player,count);
    }
    private static void givePhials(ServerPlayer player,int count) {
        String[] aspects={"vitium","vinculum","alienis","alkimia"};
        for(int i=0;i<aspects.length;i++){
            var phial=EssentiaItem.phial(Aspect.byId(aspects[i]).orElseThrow());phial.setCount(count);
            player.getInventory().setItem(9+i,phial);
        }
    }
    private static void assertPhials(GameTestHelper helper,ServerPlayer player,int expected) {
        for(int i=9;i<13;i++)helper.assertValueEqual(player.getInventory().getItem(i).getCount(),expected,"TUBES consumed the wrong number of phials in slot "+i);
    }
    private static void complete(ResearchState state,String key) {state.stage(key,ResearchCatalog.get(key).stages().size()+1,false);}
    private static ServerPlayer player(GameTestHelper helper) {
        var player=GameTestPlayers.create(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PortContent.THAUMONOMICON.get()));return player;
    }
    private static ResearchState state(ServerPlayer player) {return ResearchStorage.get(player.getServer()).player(player.getUUID());}

    private static final class JournalConnection extends net.minecraft.server.network.ServerGamePacketListenerImpl {
        final List<JournalPayload> journals=new ArrayList<>();
        JournalConnection(ServerPlayer player) {
            super(player.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false));
        }
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {
            if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom&&custom.payload() instanceof JournalPayload journal)journals.add(journal);
        }
    }
    private KnowledgeSpendingGameTests() {}
}
