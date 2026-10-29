package thaumcraft.port.research;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.Aspect;
import static org.junit.jupiter.api.Assertions.*;
import static thaumcraft.port.research.ResearchState.ResearchFlag.*;

/** Original PlayerKnowledge flags, browser acknowledgement and PacketSyncKnowledge lifecycle. */
class ResearchNotificationTest {
    @Test void pageAndResearchAreIndependentAndReadDoesNotConsumePopup(){
        var state=new ResearchState();state.stage("FIRSTSTEPS",1);
        state.setResearchFlag("FIRSTSTEPS",PAGE);
        assertTrue(state.clearResearchFlag("FIRSTSTEPS",RESEARCH));
        assertTrue(ResearchView.newPage(state,"FIRSTSTEPS"));
        assertFalse(ResearchView.newResearch(state,"FIRSTSTEPS"));
        state.setResearchFlag("FIRSTSTEPS",RESEARCH);
        assertTrue(state.clearReadFlags("FIRSTSTEPS"));
        assertFalse(ResearchView.unread(state,"FIRSTSTEPS"));
        assertTrue(state.hasResearchFlag("FIRSTSTEPS",POPUP));
        assertFalse(state.clearReadFlags("FIRSTSTEPS"));
    }

    @Test void readingDoesNotCompleteResearchOrGrantOrConsumeRewards(){
        var state=new ResearchState();state.stage("FIRSTSTEPS",2);
        state.recordScan("item:minecraft:stone",List.of(Aspect.EARTH));
        state.craft("thaumcraft:thaumometer");state.addKnowledge("THEORY","BASICS",64);state.warp(2,4,6);
        var before=ResearchCodec.write(state);state.clearReadFlags("FIRSTSTEPS");var after=ResearchCodec.write(state);
        before.remove("research_flags");after.remove("research_flags");assertEquals(before,after);
        assertEquals(2,state.stage("FIRSTSTEPS"));assertFalse(state.knows("FIRSTSTEPS"));
    }

    @Test void manualPageCompletionSuppressesOnlyItsResearchBadge(){
        var state=new ResearchState();state.stage("FIRSTSTEPS",2,false);
        assertFalse(ResearchView.unread(state,"FIRSTSTEPS"));assertTrue(state.popupResearch().isEmpty());
        state.stage("FIRSTSTEPS",4,false);
        assertTrue(state.hasResearchFlag("FIRSTSTEPS",POPUP));assertFalse(state.hasResearchFlag("FIRSTSTEPS",RESEARCH));
        state.stage("KNOWLEDGETYPES",2);
        assertTrue(state.hasResearchFlag("KNOWLEDGETYPES",POPUP));assertTrue(state.hasResearchFlag("KNOWLEDGETYPES",RESEARCH));
    }

    @Test void newAddendumDoesNotReplaceAnExistingResearchNotification(){
        var state=new ResearchState();complete(state,"INFERNALFURNACE");complete(state,"ESSENTIASMELTER");
        state.clearReadFlags("ESSENTIASMELTER");state.clearPopups();
        complete(state,"BELLOWS");
        assertTrue(state.hasResearchFlag("INFERNALFURNACE",RESEARCH));assertTrue(state.hasResearchFlag("INFERNALFURNACE",PAGE));
        assertFalse(state.hasResearchFlag("ESSENTIASMELTER",RESEARCH));assertTrue(state.hasResearchFlag("ESSENTIASMELTER",PAGE));
        assertEquals(Set.of("BELLOWS"),state.popupResearch());
    }

    @Test void addendaDoNotFlagAnUnfinishedOwner(){
        var state=new ResearchState();state.stage("INFERNALFURNACE",1,false);complete(state,"BELLOWS");
        assertFalse(state.hasResearchFlag("INFERNALFURNACE",PAGE));
    }

    @Test void addendumMessageIsAOneShotCompletionEventIndependentOfPageReadFlag(){
        var state=new ResearchState();complete(state,"INFERNALFURNACE");complete(state,"ESSENTIASMELTER");complete(state,"BELLOWS");
        state.clearReadFlags("INFERNALFURNACE");
        assertEquals(Set.of("INFERNALFURNACE","ESSENTIASMELTER"),Set.copyOf(state.drainAddendumMessages()));
        assertTrue(state.drainAddendumMessages().isEmpty());
        complete(state,"BELLOWS");assertTrue(state.drainAddendumMessages().isEmpty());
        assertTrue(state.hasResearchFlag("ESSENTIASMELTER",PAGE));
    }

    @Test void loadingAnUnreadPageNeverReplaysItsAddendumChat(){
        var state=new ResearchState();complete(state,"INFERNALFURNACE");complete(state,"BELLOWS");
        var copy=ResearchCodec.read(ResearchCodec.write(state));
        assertTrue(copy.hasResearchFlag("INFERNALFURNACE",PAGE));assertTrue(copy.drainAddendumMessages().isEmpty());
        assertEquals(List.of("INFERNALFURNACE"),state.drainAddendumMessages());
    }

    @Test void repeatedCompletionAndReloadDoNotReplayNotifications(){
        var state=new ResearchState();complete(state,"BELLOWS");state.clearReadFlags("BELLOWS");state.clearPopups();
        var copy=ResearchCodec.read(ResearchCodec.write(state));
        complete(copy,"BELLOWS");assertTrue(copy.researchFlags().isEmpty());
        assertEquals(ResearchCodec.write(state),ResearchCodec.write(copy));
    }

    @Test void codecRoundTripsPendingFlagsWithoutInventingOthers(){
        var state=new ResearchState();state.stage("FIRSTSTEPS",2,false);state.setResearchFlag("FIRSTSTEPS",PAGE);
        state.setResearchFlag("FIRSTSTEPS",POPUP);var copy=ResearchCodec.read(ResearchCodec.write(state));
        assertEquals(Set.of(PAGE,POPUP),copy.researchFlags().get("FIRSTSTEPS"));
        assertEquals(ResearchCodec.write(state),ResearchCodec.write(copy));
        assertThrows(UnsupportedOperationException.class,()->copy.researchFlags().get("FIRSTSTEPS").clear());
    }

    @Test void syncSnapshotsPopupOnceAndLeavesBothUnreadFlagsOnServer(){
        var storage=new ResearchStorage();var state=storage.player(UUID.randomUUID());state.stage("FIRSTSTEPS",1);state.setResearchFlag("FIRSTSTEPS",PAGE);
        var first=ResearchCodec.read(ResearchProgress.snapshot(storage,state));
        assertEquals(Set.of(PAGE,RESEARCH,POPUP),first.researchFlags().get("FIRSTSTEPS"));
        assertEquals(Set.of(PAGE,RESEARCH),state.researchFlags().get("FIRSTSTEPS"));assertTrue(storage.isDirty());
        var next=ResearchCodec.read(ResearchProgress.snapshot(storage,state));assertTrue(next.popupResearch().isEmpty());
        assertEquals(Set.of(PAGE,RESEARCH),next.researchFlags().get("FIRSTSTEPS"));
    }

    @Test void playerUuidRetainsReadFlagsAcrossCloneReconnectAndSaveLoad(){
        UUID id=UUID.randomUUID();var storage=new ResearchStorage();var original=storage.player(id);
        original.stage("FIRSTSTEPS",2,false);original.setResearchFlag("FIRSTSTEPS",PAGE);original.addKnowledge("THEORY","BASICS",32);
        assertSame(original,storage.player(UUID.fromString(id.toString())));
        var loaded=ResearchStorage.load(storage.save(new CompoundTag(),null),null);var reconnected=loaded.player(id);
        assertEquals(ResearchCodec.write(original),ResearchCodec.write(reconnected));
        assertTrue(loaded.player(UUID.randomUUID()).researchFlags().isEmpty());
        reconnected.clearReadFlags("FIRSTSTEPS");var second=ResearchStorage.load(loaded.save(new CompoundTag(),null),null);
        assertFalse(ResearchView.unread(second.player(id),"FIRSTSTEPS"));assertEquals(32,second.player(id).knowledge("THEORY","BASICS"));
    }

    @Test void migrationRetainsAnExactReadMarkerWithoutReplayingPopup(){
        var state=new ResearchState();state.stage("FIRSTSTEPS",2,false);state.flag("!gotdream");state.addKnowledge("OBSERVATION","BASICS",27);
        var migrated=ResearchCodec.read(legacy(state,"ui_seen:FIRSTSTEPS:2:0"));
        assertTrue(migrated.researchFlags().isEmpty());assertTrue(migrated.knows("!gotdream"));
        assertEquals(27,migrated.knowledge("OBSERVATION","BASICS"));assertTrue(migrated.flags().stream().noneMatch(k->k.startsWith("ui_seen:")));
    }

    @Test void migrationDistinguishesUnseenStageFromNewAddendum(){
        var state=new ResearchState();state.stage("FIRSTSTEPS",2,false);complete(state,"INFERNALFURNACE");complete(state,"BELLOWS");
        var migrated=ResearchCodec.read(legacy(state,"ui_seen:FIRSTSTEPS:1:0","ui_seen:INFERNALFURNACE:"+state.stage("INFERNALFURNACE")+":0","ui_seen:BELLOWS:"+state.stage("BELLOWS")+":0"));
        assertEquals(Set.of(RESEARCH),migrated.researchFlags().get("FIRSTSTEPS"));
        assertEquals(Set.of(PAGE),migrated.researchFlags().get("INFERNALFURNACE"));assertTrue(migrated.popupResearch().isEmpty());
    }

    @Test void malformedLegacyMarkersDoNotGrantKnowledgeOrSuppressUnread(){
        var state=new ResearchState();state.stage("FIRSTSTEPS",1,false);
        var migrated=ResearchCodec.read(legacy(state,"ui_seen:FIRSTSTEPS:invalid:0","ui_seen:FIRSTSTEPS:-1:0","ui_seen:UNKNOWN:4:0"));
        assertEquals(Set.of(RESEARCH),migrated.researchFlags().get("FIRSTSTEPS"));assertFalse(migrated.knows("FIRSTSTEPS"));assertFalse(migrated.knows("UNKNOWN"));
        assertEquals(1,migrated.stage("FIRSTSTEPS"));assertTrue(migrated.popupResearch().isEmpty());
    }

    @Test void malformedNewFlagsCannotCreateGameplayResearch(){
        var tag=new CompoundTag();tag.putInt("schema",ResearchCodec.SCHEMA);var flags=new CompoundTag();flags.put("FIRSTSTEPS",strings("PAGE","INVALID"));flags.put("UNKNOWN",strings("RESEARCH"));tag.put("research_flags",flags);
        var state=ResearchCodec.read(tag);assertEquals(Set.of(PAGE),state.researchFlags().get("FIRSTSTEPS"));assertFalse(state.isResearchKnown("FIRSTSTEPS"));assertFalse(state.isResearchKnown("UNKNOWN"));
        assertFalse(ResearchView.categoryFlag(state,"BASICS",PAGE));
    }

    @Test void browserCountsStartedResearchAndSearchesOriginalSelectedStage(){
        var state=new ResearchState();state.stage("FIRSTSTEPS",1,false);assertTrue(state.isResearchKnown("FIRSTSTEPS"));assertFalse(state.knows("FIRSTSTEPS"));
        long count=ResearchCatalog.all().stream().filter(e->e.category().equals("BASICS")&&!e.meta().contains("AUTOUNLOCK")).count();
        assertEquals((int)(100/count),ResearchView.completion(state,"BASICS"));
        assertEquals(List.of("thaumcraft:thaumometer","thaumcraft:salismundusfake","thaumcraft:StoneArcane","thaumcraft:BrickArcane"),ResearchView.searchRecipes(state,ResearchCatalog.get("FIRSTSTEPS")));
    }

    private static void complete(ResearchState state,String key){state.stage(key,ResearchCatalog.get(key).stages().size()+1);}
    private static CompoundTag legacy(ResearchState state,String... markers){var tag=ResearchCodec.write(state);tag.putInt("schema",2);tag.remove("research_flags");var flags=tag.getList("flags",8);for(String marker:markers)flags.add(StringTag.valueOf(marker));tag.put("flags",flags);return tag;}
    private static ListTag strings(String... values){var list=new ListTag();for(String value:values)list.add(StringTag.valueOf(value));return list;}
}
