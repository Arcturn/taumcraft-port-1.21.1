package thaumcraft.port.gametest;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.research.*;

/** Gameplay event paths, including one-time starter journal and research-stage gates. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ResearchDiscoveryGameTests {
    @GameTest(template="empty",timeoutTicks=40)
    public static void noSleepOptionGrantsJournalOnFirstCrystalPickupWithoutRepeats(GameTestHelper helper){
        boolean previous=thaumcraft.port.world.WorldConfig.NO_SLEEP.get();
        try{
            thaumcraft.port.world.WorldConfig.NO_SLEEP.set(true);
            var player=new RecordingPlayer(helper);
            pickup(player,CrystalItem.stack(Aspect.AIR));
            helper.assertTrue(state(player).knows("!gotcrystals")&&state(player).knows("!gotdream"),"noSleep did not grant the dream on first crystal pickup");
            helper.assertValueEqual(player.getInventory().countItem(Items.WRITTEN_BOOK),1,"noSleep did not give exactly one journal");
            helper.assertValueEqual(player.count("got.crystals"),1,"noSleep omitted the first-crystal hint");
            helper.assertValueEqual(player.count("got.dream"),1,"noSleep omitted the dream hint");
            pickup(player,CrystalItem.stack(Aspect.FIRE));wake(player);
            helper.assertValueEqual(player.getInventory().countItem(Items.WRITTEN_BOOK),1,"Further pickups or waking repeated the noSleep journal");
            helper.assertValueEqual(player.count("got.dream"),1,"Further pickups or waking repeated the noSleep hint");
            // The original option is checked by the first-crystal discovery, not every pickup.
            var existing=new RecordingPlayer(helper);state(existing).flag("!gotcrystals");pickup(existing,CrystalItem.stack(Aspect.AIR));
            helper.assertTrue(!state(existing).knows("!gotdream"),"Changing noSleep retroactively replayed an old crystal discovery");
        }finally{thaumcraft.port.world.WorldConfig.NO_SLEEP.set(previous);}
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void crystalPickupThenWakeCreatesOriginalJournalExactlyOnce(GameTestHelper helper){
        var player=new RecordingPlayer(helper);var state=state(player);
        wake(player);
        helper.assertTrue(!state.knows("!gotdream"),"Sleeping before finding a crystal granted a dream");
        pickup(player,CrystalItem.stack(Aspect.AIR));
        helper.assertTrue(state.knows("!gotcrystals"),"Picking up a crystal did not start the research introduction");
        helper.assertValueEqual(player.count("got.crystals"),1,"First crystal must show its original hint once");
        pickup(player,CrystalItem.stack(Aspect.FIRE));
        helper.assertValueEqual(player.count("got.crystals"),1,"Second crystal repeated the starter hint");
        helper.assertTrue(!state.knows("!gotdream"),"Crystal pickup skipped the sleep requirement");
        wake(player);
        var books=player.getInventory().items.stream().filter(s->s.is(Items.WRITTEN_BOOK)).toList();
        helper.assertValueEqual(books.size(),1,"Waking did not grant exactly one written journal");
        var content=books.getFirst().get(DataComponents.WRITTEN_BOOK_CONTENT);
        helper.assertTrue(content!=null,"Dream journal has no vanilla written-book content");
        helper.assertValueEqual(content.author(),player.getGameProfile().getName(),"Dream journal author is not the player");
        helper.assertValueEqual(content.generation(),3,"Dream journal must retain original non-copyable generation");
        helper.assertValueEqual(content.pages().size(),3,"Dream journal lost an original page");
        for(int i=0;i<3;i++)helper.assertTrue(content.pages().get(i).raw().getContents() instanceof TranslatableContents t&&t.getKey().equals("book.start."+(i+1)),"Dream pages must use the original client-localized text");
        helper.assertTrue(state.knows("!gotdream"),"Dream completion was not saved");
        wake(player);helper.assertValueEqual(player.getInventory().countItem(Items.WRITTEN_BOOK),1,"Repeated waking duplicated the journal");
        helper.assertValueEqual(player.count("got.dream"),1,"Repeated waking replayed the dream message");
        var restored=ResearchCodec.read(ResearchCodec.write(state));
        helper.assertTrue(restored.knows("!gotdream")&&restored.knows("!gotcrystals"),"Starter discoveries did not survive serialization");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void fullInventoryDropsDreamJournalAndAlreadyStartedPlayersDoNotReceiveAnother(GameTestHelper helper){
        var player=new RecordingPlayer(helper);state(player).flag("!gotcrystals");
        for(int slot=0;slot<player.getInventory().items.size();slot++)player.getInventory().setItem(slot,new ItemStack(Items.STONE,64));
        wake(player);
        var dropped=helper.getLevel().getEntitiesOfClass(ItemEntity.class,player.getBoundingBox().inflate(2),e->e.getItem().is(Items.WRITTEN_BOOK));
        helper.assertValueEqual(dropped.size(),1,"Full inventory lost the dream journal instead of dropping it");
        dropped.forEach(ItemEntity::discard);
        var existing=new RecordingPlayer(helper);state(existing).flag("!gotcrystals");state(existing).flag("!gotdream");wake(existing);
        helper.assertValueEqual(existing.getInventory().countItem(Items.WRITTEN_BOOK),0,"Existing saves received another starter journal");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void bookPickupRecordsDiscoveryWithoutOpeningBook(GameTestHelper helper){
        var player=new RecordingPlayer(helper);var connection=new RecordingConnection(player);
        pickup(player,new ItemStack(PortContent.THAUMONOMICON.get()));
        helper.assertTrue(state(player).knows("!gotthaumonomicon"),"Picking up the Thaumonomicon did not reveal First Steps");
        helper.assertValueEqual(connection.journals.size(),1,"Book pickup did not sync its discovery");
        helper.assertTrue(!connection.journals.getFirst().open(),"Picking up a book unexpectedly opened its screen");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void fireDiscoveryRequiresActualDamageAfterCorrectResearchStage(GameTestHelper helper){
        var player=new RecordingPlayer(helper);var state=state(player);
        hurt(player,player.damageSources().onFire());
        helper.assertTrue(!state.knows("f_onfire"),"Fire damage before auromancy granted future research");
        state.stage("BASEAUROMANCY",1,false);hurt(player,player.damageSources().onFire());
        helper.assertTrue(!state.knows("f_onfire"),"First auromancy stage was treated as the fire-discovery stage");
        state.stage("BASEAUROMANCY",2,false);player.setRemainingFireTicks(80);player.tickCount=200;
        ResearchEvents.tick(new PlayerTickEvent.Post(player));
        helper.assertTrue(!state.knows("f_onfire"),"Being visibly on fire without damage granted research");
        hurt(player,player.damageSources().onFire());
        helper.assertTrue(state.knows("f_onfire"),"Fire damage at the second auromancy stage did not count");
        helper.assertValueEqual(player.count("got.onfire"),1,"Fire discovery lacked its original feedback");
        hurt(player,player.damageSources().onFire());helper.assertValueEqual(player.count("got.onfire"),1,"Repeat fire damage replayed discovery feedback");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void projectileHitsUnlockOnlyTheirMatchingDiscoveryAtStageTwo(GameTestHelper helper){
        var player=new RecordingPlayer(helper);var state=state(player);
        var arrow=EntityType.ARROW.create(helper.getLevel());var fireball=EntityType.SMALL_FIREBALL.create(helper.getLevel());var spit=EntityType.LLAMA_SPIT.create(helper.getLevel());
        hurt(player,player.damageSources().arrow(arrow,null));
        helper.assertTrue(!state.knows("f_arrow"),"An early arrow hit granted future projectile research");
        state.stage("FOCUSPROJECTILE",2,false);hurt(player,player.damageSources().arrow(arrow,null));
        helper.assertTrue(state.knows("f_arrow")&&!state.knows("f_fireball")&&!state.knows("f_spit"),"Arrow hit granted the wrong projectile discoveries");
        hurt(player,player.damageSources().fireball(fireball,null));hurt(player,player.damageSources().spit(spit,null));
        helper.assertTrue(state.knows("f_fireball")&&state.knows("f_spit"),"Fireball or llama-spit damage did not count");
        helper.assertValueEqual(player.count("got.projectile"),3,"Each newly experienced projectile must show one hint");
        hurt(player,player.damageSources().arrow(arrow,null));helper.assertValueEqual(player.count("got.projectile"),3,"Repeated arrow hit repeated its hint");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void travelDiscoveriesUseOriginalCadenceAndSyncWithoutThaumometer(GameTestHelper helper){
        var player=new RecordingPlayer(helper);var state=state(player);var connection=new RecordingConnection(player);
        player.getStats().setValue(player,Stats.CUSTOM.get(Stats.WALK_ONE_CM),160001);
        player.getStats().setValue(player,Stats.CUSTOM.get(Stats.SPRINT_ONE_CM),80001);
        player.getStats().setValue(player,Stats.CUSTOM.get(Stats.JUMP),501);
        player.getStats().setValue(player,Stats.CUSTOM.get(Stats.SWIM_ONE_CM),8001);
        player.tickCount=20;ResearchEvents.tick(new PlayerTickEvent.Post(player));
        helper.assertTrue(!state.knows("m_walker"),"Travel research was checked before original 200-tick cadence");
        player.tickCount=200;ResearchEvents.tick(new PlayerTickEvent.Post(player));
        for(String key:List.of("m_walker","m_runner","m_jumper","m_swimmer"))helper.assertTrue(state.knows(key),"Missing travel discovery: "+key);
        helper.assertValueEqual(connection.journals.size(),1,"Travel discoveries did not sync without a thaumometer");
        player.tickCount=400;ResearchEvents.tick(new PlayerTickEvent.Post(player));helper.assertValueEqual(connection.journals.size(),1,"Repeat travel check resent unchanged discovery state");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void explorationHintIsGatedAndGrantedOnlyOnceAtNewWorldFloor(GameTestHelper helper){
        var player=new RecordingPlayer(helper);var state=state(player);var connection=new RecordingConnection(player);
        player.setPos(player.getX(),helper.getLevel().getMinBuildHeight()+9,player.getZ());player.tickCount=200;
        ResearchEvents.tick(new PlayerTickEvent.Post(player));helper.assertTrue(!state.knows("m_deepdown"),"Depth discovery was granted before auromancy exploration began");
        state.stage("UNLOCKAUROMANCY",1,false);ResearchEvents.tick(new PlayerTickEvent.Post(player));
        helper.assertTrue(state.knows("m_deepdown"),"Depth discovery did not use the new world floor");
        helper.assertValueEqual(player.count("got.deepdown"),1,"Depth discovery lacked original actionbar feedback");
        helper.assertValueEqual(connection.journals.size(),1,"Depth discovery did not sync immediately");
        ResearchEvents.tick(new PlayerTickEvent.Post(player));helper.assertValueEqual(player.count("got.deepdown"),1,"Repeated depth check repeated feedback");
        helper.succeed();
    }

    private static void pickup(ServerPlayer player,ItemStack stack){
        var item=new ItemEntity(player.level(),player.getX(),player.getY(),player.getZ(),stack);item.setNoPickUpDelay();item.playerTouch(player);item.discard();
    }
    private static void wake(ServerPlayer player){NeoForge.EVENT_BUS.post(new PlayerWakeUpEvent(player,false,true));}
    private static void hurt(RecordingPlayer player,DamageSource source){player.setHealth(player.getMaxHealth());player.applyDamage(source);}
    private static ResearchState state(ServerPlayer player){return ResearchStorage.get(player.getServer()).player(player.getUUID());}
    private static final class RecordingPlayer extends FakePlayer {
        final List<String> messages=new ArrayList<>();
        RecordingPlayer(GameTestHelper helper){super(helper.getLevel(),new GameProfile(UUID.randomUUID(),"tc-discovery"));setGameMode(GameType.SURVIVAL);var pos=helper.absolutePos(net.minecraft.core.BlockPos.ZERO);setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);}
        @Override public boolean isInvulnerableTo(DamageSource source){return false;}
        // Exercise vanilla damage/event processing without FakePlayer's login invulnerability timer.
        void applyDamage(DamageSource source){
            damageContainers.push(new net.neoforged.neoforge.common.damagesource.DamageContainer(source,2));
            try{super.actuallyHurt(source,2);}finally{damageContainers.pop();}
        }
        @Override public void displayClientMessage(Component message,boolean actionbar){if(message.getContents() instanceof TranslatableContents t)messages.add(t.getKey());}
        int count(String key){return (int)messages.stream().filter(key::equals).count();}
    }
    private static final class RecordingConnection extends net.minecraft.server.network.ServerGamePacketListenerImpl {
        final List<thaumcraft.port.network.JournalPayload> journals=new ArrayList<>();
        RecordingConnection(ServerPlayer player){super(player.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false));}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet){if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom&&custom.payload() instanceof thaumcraft.port.network.JournalPayload journal)journals.add(journal);}
    }
    private ResearchDiscoveryGameTests(){}
}
