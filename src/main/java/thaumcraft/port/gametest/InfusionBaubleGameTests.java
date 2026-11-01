package thaumcraft.port.gametest;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.*;
import thaumcraft.port.essentia.*;
import thaumcraft.port.infusion.bauble.*;
import thaumcraft.port.infusion.structure.ItemCharge;
import thaumcraft.port.integration.CuriosEvents;
import thaumcraft.port.research.*;
import thaumcraft.port.world.WorldEntities;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class InfusionBaubleGameTests {
    @GameTest(template="empty",timeoutTicks=40)
    public static void curiosSlotsExcludeBackpackCosmeticAndDisabledGear(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);var inventory=CuriosApi.getCuriosInventory(player).orElseThrow();
        var ring=new ItemStack(InfusionBaubles.CLOUD_RING.get());player.getInventory().setItem(9,ring.copy());
        helper.assertFalse(BaubleEvents.worn(player,ring.getItem()),"Backpack ring cannot enable abilities");
        for(var pair:java.util.Map.of("ring",ring,"head",new ItemStack(InfusionBaubles.CURIOSITY_BAND.get()),"charm",VerdantCharmItem.stack(2)).entrySet()){
            var handler=inventory.getStacksHandler(pair.getKey()).orElseThrow();var stack=pair.getValue();
            helper.assertTrue(handler.getStacks().isItemValid(0,stack),"Bauble must fit original "+pair.getKey()+" slot");
            handler.getCosmeticStacks().setStackInSlot(0,stack.copy());helper.assertFalse(BaubleEvents.worn(player,stack.getItem()),"Cosmetic gear grants no effects");
            handler.getStacks().setStackInSlot(0,stack.copy());helper.assertTrue(BaubleEvents.worn(player,stack.getItem()),"Functional gear must activate");
            inventory.setSlotActive(pair.getKey(),0,false);helper.assertFalse(BaubleEvents.worn(player,stack.getItem()),"Disabled slots grant no effects");inventory.setSlotActive(pair.getKey(),0,true);
        }
        equip(player,"ring",new ItemStack(InfusionBaubles.RING_APPRENTICE.get()));
        helper.assertValueEqual(RevealingEquipment.discountedVis(100,player),95,"Apprentice ring retains five percent discount");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void verdantPrioritizesCuresAndSpendsExactCharge(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);var charm=VerdantCharmItem.stack(1);equip(player,"charm",charm);ItemCharge.add(charm,player,25);player.setHealth(10);
        player.addEffect(new MobEffectInstance(MobEffects.WITHER,200));player.addEffect(new MobEffectInstance(MobEffects.POISON,200));player.addEffect(new MobEffectInstance(WorldEntities.TAINT,200));
        VerdantCharmItem.wornSecond(charm,player);
        helper.assertFalse(player.hasEffect(MobEffects.WITHER),"Wither gets first cure");helper.assertTrue(player.hasEffect(MobEffects.POISON),"Only one cure per second");helper.assertValueEqual(ItemCharge.get(charm),5,"Wither costs twenty vis");
        VerdantCharmItem.wornSecond(charm,player);
        helper.assertFalse(player.hasEffect(WorldEntities.TAINT),"An unaffordable poison cure must not prevent affordable taint cure");helper.assertTrue(player.hasEffect(MobEffects.POISON),"Poison requires ten vis");helper.assertValueEqual(ItemCharge.get(charm),0,"Taint costs five vis");
        player.removeAllEffects();VerdantCharmItem.wornSecond(charm,player);helper.assertValueEqual(player.getHealth(),10f,"Empty charm cannot heal");
        ItemCharge.add(charm,player,5);VerdantCharmItem.wornSecond(charm,player);helper.assertValueEqual(player.getHealth(),11f,"Life heart heals one health point");helper.assertValueEqual(ItemCharge.get(charm),0,"Healing costs five vis");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void verdantSustainBreathBeforeFoodAndPersistsVariant(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);var charm=VerdantCharmItem.stack(2);equip(player,"charm",charm);ItemCharge.add(charm,player,3);player.setAirSupply(50);player.getFoodData().setFoodLevel(10);
        VerdantCharmItem.wornSecond(charm,player);helper.assertValueEqual(player.getAirSupply(),300,"Sustain must restore breath");helper.assertValueEqual(player.getFoodData().getFoodLevel(),10,"Breath takes priority over hunger");
        VerdantCharmItem.wornSecond(charm,player);helper.assertValueEqual(player.getFoodData().getFoodLevel(),11,"Sustain restores one food");helper.assertValueEqual(ItemCharge.get(charm),1,"Each action costs one vis");
        var saved=charm.save(helper.getLevel().registryAccess());var copy=ItemStack.parseOptional(helper.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)saved);
        helper.assertValueEqual(VerdantCharmItem.kind(copy),2,"Sustain variant must survive item save");helper.assertValueEqual(ItemCharge.get(copy),1,"Charge must survive item save");
        player.tickCount=20;player.setAirSupply(50);InfusionBaubles.VERDANT_CHARM.get().curioTick(new SlotContext("charm",player,0,true,true),charm);helper.assertValueEqual(ItemCharge.get(charm),1,"Cosmetic charm cannot consume charge or activate");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void curiosityConvertsHalfOrbAndAddsRawKnowledge(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);var orb=new ExperienceOrb(helper.getLevel(),player.getX(),player.getY(),player.getZ(),41);
        BaubleEvents.xp(new PlayerXpEvent.PickupXp(player,orb));helper.assertValueEqual(orb.value,41,"Unequipped band cannot consume XP");
        equip(player,"head",new ItemStack(InfusionBaubles.CURIOSITY_BAND.get()));BaubleEvents.xp(new PlayerXpEvent.PickupXp(player,orb));helper.assertValueEqual(orb.value,21,"Original conversion floors half the XP");
        var state=ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID());int total=0;for(var category:ResearchCatalog.CATEGORIES)total+=state.knowledge("THEORY",category);
        helper.assertValueEqual(total,1,"Twenty converted XP guarantees one raw theory point");
        orb.value=1;BaubleEvents.xp(new PlayerXpEvent.PickupXp(player,orb));helper.assertValueEqual(orb.value,1,"Single XP orb is never converted");
        helper.assertTrue("THEORY".equals(BaubleEvents.knowledgeType(2,.099f)),"Theory probability is five percent per converted XP");helper.assertTrue("OBSERVATION".equals(BaubleEvents.knowledgeType(2,.1f)),"Observation follows theory interval");helper.assertTrue(BaubleEvents.knowledgeType(2,.4f)==null,"No knowledge above the twenty percent threshold");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void cloudJumpValidatesEquipmentAndRejectsRepeatedPackets(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);player.setOnGround(false);helper.assertFalse(BaubleEvents.requestJump(player),"No ring means no jump");equip(player,"ring",new ItemStack(InfusionBaubles.CLOUD_RING.get()));
        player.setDeltaMovement(.1,-.3,.2);player.fallDistance=10;
        helper.assertTrue(BaubleEvents.requestJump(player),"Equipped cloud ring allows an airborne jump");helper.assertTrue(Math.abs(player.getDeltaMovement().y-.75)<.00001,"Original cloud jump height must be .75");helper.assertValueEqual(player.fallDistance,0f,"Jump resets fall distance");
        helper.assertFalse(BaubleEvents.requestJump(player),"Packet replay cannot create infinite air jumps");
        player.setOnGround(true);BaubleEvents.tick(new PlayerTickEvent.Post(player));helper.assertFalse(BaubleEvents.requestJump(player),"Ring cannot trigger while grounded");player.setOnGround(false);player.addEffect(new MobEffectInstance(MobEffects.JUMP,100,1));
        helper.assertTrue(BaubleEvents.requestJump(player),"Landing refreshes ring jump");helper.assertTrue(Math.abs(player.getDeltaMovement().y-.95)<.00001,"Jump boost contributes .1 per level");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void travelAndBarrierRespectPlayersRidersAndRedstone(GameTestHelper helper){
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,1,1));var player=GameTestPlayers.create(helper);
        var travel=InfusionBaubles.PAVING_STONE_TRAVEL.get();travel.stepOn(level,pos,travel.defaultBlockState(),player);
        helper.assertValueEqual(player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier(),1,"Travel paving grants speed two");helper.assertValueEqual(player.getEffect(MobEffects.JUMP).getDuration(),40,"Travel jump lasts two seconds");
        level.setBlockAndUpdate(pos,InfusionBaubles.PAVING_STONE_BARRIER.get().defaultBlockState());var barrier=(BarrierStoneEntity)level.getBlockEntity(pos);for(int i=0;i<201;i++)barrier.tick();
        helper.assertTrue(level.getBlockState(pos.above()).is(InfusionBaubles.BARRIER.get())&&level.getBlockState(pos.above(2)).is(InfusionBaubles.BARRIER.get()),"Paving must maintain two high barrier");
        var mob=helper.spawn(EntityType.PIG,new BlockPos(1,2,1));var field=level.getBlockState(pos.above());
        helper.assertFalse(field.getCollisionShape(level,pos.above(),CollisionContext.of(mob)).isEmpty(),"Field must block mobs");helper.assertTrue(field.getCollisionShape(level,pos.above(),CollisionContext.of(player)).isEmpty(),"Players cross field freely");
        // NeoForge FakePlayer explicitly refuses startRiding; use vanilla's player fixture for a real passenger.
        var rider=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);helper.assertTrue(rider.startRiding(mob,true),"Riding fixture must mount successfully");helper.assertTrue(field.getCollisionShape(level,pos.above(),CollisionContext.of(mob)).isEmpty(),"Player mounts cross field");rider.stopRiding();
        level.setBlockAndUpdate(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());helper.assertTrue(field.getCollisionShape(level,pos.above(),CollisionContext.of(mob)).isEmpty(),"Redstone disables field collision");
        level.removeBlock(pos,false);helper.assertTrue(level.isEmptyBlock(pos.above())&&level.isEmptyBlock(pos.above(2)),"Removing paving clears both invisible fields");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void bannersConsumePhialsSaveAndDropTheirAspect(GameTestHelper helper){
        var level=helper.getLevel();var player=GameTestPlayers.create(helper);var pos=helper.absolutePos(new BlockPos(1,1,1));var block=InfusionBaubles.BANNERS.get(DyeColor.PURPLE).get();level.setBlockAndUpdate(pos,block.defaultBlockState());
        var phial=EssentiaItem.phial(Aspect.MAGIC);phial.setCount(2);ThaumicBannerBlock.apply(level,pos,player,phial);var banner=(ThaumicBannerEntity)level.getBlockEntity(pos);
        helper.assertTrue(banner.aspect()==Aspect.MAGIC,"Phial chooses banner aspect");helper.assertValueEqual(phial.getCount(),1,"Original application consumes one full phial");
        var copy=block.getCloneItemStack(level,pos,block.defaultBlockState());helper.assertTrue(EssentiaItem.aspect(copy)==Aspect.MAGIC,"Pick block preserves aspect");
        var drops=Block.getDrops(block.defaultBlockState(),level,pos,banner);helper.assertValueEqual(drops.size(),1,"Banner drops exactly once");helper.assertTrue(EssentiaItem.aspect(drops.getFirst())==Aspect.MAGIC,"Broken banner keeps aspect");
        var restored=new ThaumicBannerEntity(pos,block.defaultBlockState());restored.loadWithComponents(banner.saveWithFullMetadata(level.registryAccess()),level.registryAccess());helper.assertTrue(restored.aspect()==Aspect.MAGIC,"Banner decoration survives reload");
        player.setShiftKeyDown(true);ThaumicBannerBlock.apply(level,pos,player,ItemStack.EMPTY);helper.assertTrue(banner.aspect()==null,"Sneaking clears decoration");
        helper.assertValueEqual(InfusionBaubles.BANNERS.size(),16,"All sixteen dye banners must exist");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void undyingUsesOnlyActiveCharmAndRetainsOriginalEffects(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);var inventory=CuriosApi.getCuriosInventory(player).orElseThrow();var handler=inventory.getStacksHandler("charm").orElseThrow();
        handler.getStacks().setStackInSlot(0,new ItemStack(PortContent.CHARM_UNDYING.get()));inventory.setSlotActive("charm",0,false);var inactive=new LivingDeathEvent(player,player.damageSources().generic());CuriosEvents.onDeath(inactive);helper.assertFalse(inactive.isCanceled(),"Inactive undying charm must not activate");
        inventory.setSlotActive("charm",0,true);player.addEffect(new MobEffectInstance(MobEffects.POISON,100));var death=new LivingDeathEvent(player,player.damageSources().generic());CuriosEvents.onDeath(death);
        helper.assertTrue(death.isCanceled(),"Worn charm prevents death");helper.assertTrue(handler.getStacks().getStackInSlot(0).isEmpty(),"Charm is consumed");helper.assertValueEqual(player.getHealth(),1f,"Revival starts at one health");helper.assertFalse(player.hasEffect(MobEffects.POISON),"Revival removes previous effects");helper.assertValueEqual(player.getEffect(MobEffects.REGENERATION).getDuration(),900,"Original regeneration lasts 45 seconds");helper.assertValueEqual(player.getEffect(MobEffects.ABSORPTION).getAmplifier(),1,"Original absorption is level two");helper.succeed();
    }
    private static void equip(ServerPlayer player,String slot,ItemStack stack){CuriosApi.getCuriosInventory(player).orElseThrow().getStacksHandler(slot).orElseThrow().getStacks().setStackInSlot(0,stack);}
    private InfusionBaubleGameTests(){}
}
