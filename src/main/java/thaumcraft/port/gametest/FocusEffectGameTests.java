package thaumcraft.port.gametest;

import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.auromancy.effect.*;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.auromancy.focus.effect.*;
import thaumcraft.port.world.AuraSystem;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class FocusEffectGameTests {
    private static <T extends FocusEffect>T effect(GameTestHelper helper,T effect){effect.setPackage(new FocusPackage(GameTestPlayers.create(helper)));return effect;}
    private static BlockHitResult hit(GameTestHelper helper,BlockPos relative){BlockPos absolute=helper.absolutePos(relative);return new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false);}
    @GameTest(template="elemental_tools_empty")
    public static void fireRetainsSeparateDamageAndQuadraticBurnDuration(GameTestHelper helper){
        var fire=effect(helper,new FocusEffectFire());fire.getSetting("power").setValue(3);fire.getSetting("duration").setValue(2);
        var cow=helper.spawn(EntityType.COW,new BlockPos(4,3,4));
        helper.assertTrue(fire.execute(new EntityHitResult(cow),null,1,0),"Fire entity return changed");
        helper.assertValueEqual(cow.getHealth(),4f,"Fire damage must be 3 + power");
        helper.assertValueEqual(cow.getRemainingFireTicks(),100,"Fire burn must use 1 + duration squared seconds");
        var blaze=helper.spawn(EntityType.BLAZE,new BlockPos(6,3,4));float before=blaze.getHealth();
        helper.assertFalse(fire.execute(new EntityHitResult(blaze),null,1,0),"Fire-immune entity accepted fire focus");helper.assertValueEqual(blaze.getHealth(),before,"Fire-immune entity was damaged");
        cow.discard();blaze.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void frostFreezesOnlySourceWaterAndUsesOriginalSlowness(GameTestHelper helper){
        var frost=effect(helper,new FocusEffectFrost());frost.getSetting("power").setValue(3);frost.getSetting("duration").setValue(7);
        var cow=helper.spawn(EntityType.COW,new BlockPos(4,3,4));
        helper.assertFalse(frost.execute(new EntityHitResult(cow),null,1,0),"Frost return contract changed");
        helper.assertValueEqual(cow.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration(),140,"Frost duration changed");helper.assertValueEqual(cow.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier(),2,"Frost amplifier changed");
        var source=new BlockPos(10,3,10);var flowing=source.east();helper.setBlock(source,Blocks.WATER);helper.setBlock(flowing,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,1));
        frost.execute(hit(helper,source),null,1,0);helper.assertBlockPresent(Blocks.FROSTED_ICE,source);helper.assertBlockPresent(Blocks.WATER,flowing);cow.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void healHurtsUndeadAndCurseAlwaysAppliesPoison(GameTestHelper helper){
        var heal=effect(helper,new FocusEffectHeal());heal.getSetting("power").setValue(4);var cow=helper.spawn(EntityType.COW,new BlockPos(4,3,4));cow.setHealth(2);
        heal.execute(new EntityHitResult(cow),null,1,0);helper.assertValueEqual(cow.getHealth(),6f,"Heal did not restore four points");
        var zombie=helper.spawn(EntityType.ZOMBIE,new BlockPos(6,3,4));float before=zombie.getHealth();heal.execute(new EntityHitResult(zombie),null,1,0);helper.assertValueEqual(zombie.getHealth(),before-6,"Undead healing must deal power * 1.5 magic damage");
        cow.invulnerableTime=0;var curse=effect(helper,new FocusEffectCurse());curse.getSetting("duration").setValue(4);curse.getSetting("power").setValue(1);curse.execute(new EntityHitResult(cow),null,1,0);
        helper.assertTrue(cow.hasEffect(MobEffects.POISON),"Curse lost its guaranteed poison");helper.assertValueEqual(cow.getEffect(MobEffects.POISON).getDuration(),80,"Curse duration changed");helper.assertFalse(cow.hasEffect(MobEffects.LEVITATION),"SRG UNLUCK was incorrectly mapped to levitation");
        cow.discard();zombie.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void airKnockbackUsesTrajectoryAndEarthHonorsSoftBlockThreshold(GameTestHelper helper){
        var air=effect(helper,new FocusEffectAir());var cow=helper.spawn(EntityType.COW,new BlockPos(4,3,4));cow.setDeltaMovement(Vec3.ZERO);
        helper.assertTrue(air.execute(new EntityHitResult(cow),new Trajectory(cow.position(),new Vec3(1,0,0)),1,0),"Air entity return changed");helper.assertTrue(cow.getDeltaMovement().x>0,"Air knockback reversed the outgoing trajectory");
        var earth=effect(helper,new FocusEffectEarth());var grass=new BlockPos(9,3,9);var stone=grass.east();helper.setBlock(grass.below(),Blocks.DIRT);helper.setBlock(grass,Blocks.SHORT_GRASS);helper.setBlock(stone,Blocks.STONE);AuraSystem.add(helper.getLevel(),helper.absolutePos(grass),5,false);
        earth.execute(hit(helper,grass),null,1,0);earth.execute(hit(helper,stone),null,1,0);FocusWorldEffects.tickWorld(helper.getLevel());helper.assertBlockPresent(Blocks.AIR,grass);helper.assertBlockPresent(Blocks.STONE,stone);cow.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void breakerWaitsForDamageAndPaysAuraOnlyForSuccessfulHarvest(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);var level=helper.getLevel();var relative=new BlockPos(8,3,8);var pos=helper.absolutePos(relative);helper.setBlock(relative,Blocks.STONE);AuraSystem.add(level,pos,5,false);float vis=AuraSystem.get(level,pos).vis();
        FocusWorldEffects.queueBreak(player,pos,Blocks.STONE.defaultBlockState(),true,true,0,2,10,10,2,.5f,InteractionHand.OFF_HAND);
        for(int i=0;i<6;i++)FocusWorldEffects.tickWorld(level);
        helper.assertBlockPresent(Blocks.STONE,relative);helper.assertValueEqual(AuraSystem.get(level,pos).vis(),vis,"Incomplete break charged aura");FocusWorldEffects.tickWorld(level);helper.assertBlockPresent(Blocks.AIR,relative);helper.assertTrue(Math.abs(AuraSystem.get(level,pos).vis()-(vis-.5f))<.0001,"Breaker cost was not exactly one half vis");
        helper.assertTrue(!level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2),entity->entity.getItem().is(Items.STONE)).isEmpty(),"Silk-touch break lost the original stone drop");
        helper.setBlock(relative,Blocks.STONE);float protectedVis=AuraSystem.get(level,pos).vis();Consumer<BlockEvent.BreakEvent> cancel=event->{if(event.getPos().equals(pos))event.setCanceled(true);};NeoForge.EVENT_BUS.addListener(cancel);
        try{FocusWorldEffects.queueBreak(player,pos,Blocks.STONE.defaultBlockState(),true,false,0,10,1,1,0,.5f,InteractionHand.MAIN_HAND);FocusWorldEffects.tickWorld(level);helper.assertBlockPresent(Blocks.STONE,relative);helper.assertValueEqual(AuraSystem.get(level,pos).vis(),protectedVis,"Canceled break charged aura");}finally{NeoForge.EVENT_BUS.unregister(cancel);}
        helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void exchangeConsumesOneInventoryBlockPreservesStateAndCollectsOldDrop(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);var level=helper.getLevel();var relative=new BlockPos(8,3,8);var pos=helper.absolutePos(relative);helper.setBlock(relative,Blocks.STONE);AuraSystem.add(level,pos,5,false);float vis=AuraSystem.get(level,pos).vis();player.getInventory().setItem(9,new ItemStack(Items.OAK_LOG,2));
        var selected=new ItemStack(Items.OAK_LOG);selected.set(DataComponents.BLOCK_STATE,new BlockItemStateProperties(java.util.Map.of("axis","x")));
        FocusWorldEffects.queueExchange(player,pos,Blocks.STONE.defaultBlockState(),selected,true,0,.5f,InteractionHand.OFF_HAND);FocusWorldEffects.tickWorld(level);
        helper.assertBlockPresent(Blocks.OAK_LOG,relative);helper.assertValueEqual(level.getBlockState(pos).getValue(BlockStateProperties.AXIS),Direction.Axis.X,"Exchange lost picked block state");helper.assertValueEqual(player.getInventory().getItem(9).getCount(),1,"Exchange did not consume exactly one material");helper.assertTrue(player.getInventory().contains(new ItemStack(Items.STONE)),"Exchange did not collect silk-touched source");helper.assertTrue(Math.abs(AuraSystem.get(level,pos).vis()-(vis-.5f))<.0001,"Exchange cost wrong");
        FocusWorldEffects.queueExchange(player,pos,Blocks.STONE.defaultBlockState(),selected,true,0,.5f,InteractionHand.OFF_HAND);FocusWorldEffects.tickWorld(level);helper.assertValueEqual(player.getInventory().getItem(9).getCount(),1,"Stale queued exchange consumed another item");helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void portableHolePersistsTimerAndExactOriginalBlockState(GameTestHelper helper){
        var relative=new BlockPos(8,3,8);var pos=helper.absolutePos(relative);var level=helper.getLevel();var original=Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.Z);helper.setBlock(relative,original);
        helper.assertTrue(FocusWorldEffects.createHole(level,pos,Direction.NORTH,1,40,null),"Ordinary solid block refused a hole");var hole=(HoleBlockEntity)level.getBlockEntity(pos);for(int i=0;i<13;i++)hole.tick();var data=hole.saveWithFullMetadata(level.registryAccess());var restored=new HoleBlockEntity(pos,hole.getBlockState());restored.loadWithComponents(data,level.registryAccess());level.setBlockEntity(restored);
        helper.assertValueEqual(restored.remainingTicks(),27,"Reload restarted hole lifetime");helper.assertValueEqual(restored.storedState(),original,"Reload lost stored block properties");for(int i=0;i<26;i++)restored.tick();helper.assertBlockPresent(FocusEffectContent.HOLE.get(),relative);restored.tick();helper.assertValueEqual(level.getBlockState(pos),original,"Hole failed to restore exact source state");
        helper.setBlock(relative.east(),Blocks.CHEST);helper.assertFalse(FocusWorldEffects.createHole(level,pos.east(),null,1,20,null),"Hole destroyed a block entity");helper.setBlock(relative.west(),Blocks.BEDROCK);helper.assertFalse(FocusWorldEffects.createHole(level,pos.west(),null,1,20,null),"Hole destroyed bedrock");helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void portableHolePropagatesOriginalThreeByThreeCrossSection(GameTestHelper helper){
        var origin=new BlockPos(8,3,8);var level=helper.getLevel();for(int z=0;z<3;z++)for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)helper.setBlock(origin.offset(x,y,z),Blocks.STONE);
        BlockPos pos=helper.absolutePos(origin);helper.assertTrue(FocusWorldEffects.createHole(level,pos,Direction.NORTH,3,40,null),"Initial tunnel hole failed");((HoleBlockEntity)level.getBlockEntity(pos)).tick();
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)helper.assertBlockPresent(FocusEffectContent.HOLE.get(),origin.offset(x,y,0));helper.assertBlockPresent(FocusEffectContent.HOLE.get(),origin.south());helper.assertBlockPresent(Blocks.STONE,origin.south(2));
        ((HoleBlockEntity)level.getBlockEntity(pos.south())).tick();helper.assertBlockPresent(FocusEffectContent.HOLE.get(),origin.south(2));helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=45)
    public static void portableHoleBlocksWaterWithoutBlockingEntities(GameTestHelper helper){
        var relative=new BlockPos(8,3,8);var pos=helper.absolutePos(relative);var level=helper.getLevel();helper.setBlock(relative,Blocks.STONE);
        helper.assertTrue(FocusWorldEffects.createHole(level,pos,null,1,200,null),"Hole creation failed");
        helper.assertTrue(level.getBlockState(pos).getCollisionShape(level,pos).isEmpty(),"Portable hole blocks walking");
        helper.assertTrue(level.getBlockState(pos).isFaceSturdy(level,pos,Direction.UP),"Source solid-side support was lost");
        helper.setBlock(relative.above(),Blocks.WATER);
        helper.runAtTickTime(25,()->{helper.assertBlockPresent(FocusEffectContent.HOLE.get(),relative);helper.assertTrue(level.getBlockEntity(pos) instanceof HoleBlockEntity,"Water destroyed the stored restoration state");helper.succeed();});
    }
    @GameTest(template="elemental_tools_empty")
    public static void sourceComplexitiesAndSoundIdsRemainExact(GameTestHelper helper){
        var exchange=new FocusEffectExchange();helper.assertValueEqual(exchange.getComplexity(),3,"BETA26 exchange's actual precedence must be preserved");exchange.getSetting("silk").setValue(1);helper.assertValueEqual(exchange.getComplexity(),3,"Exchange source did not price silk due its conditional precedence");exchange.getSetting("fortune").setValue(4);helper.assertValueEqual(exchange.getComplexity(),15,"Exchange fortune IV complexity changed");
        var breaker=new FocusEffectBreak();breaker.getSetting("power").setValue(5);breaker.getSetting("silk").setValue(1);breaker.getSetting("fortune").setValue(4);helper.assertValueEqual(breaker.getComplexity(),34,"Break final complexity changed");
        for(String id:FocusEffectSounds.sourceMappings().values())helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.withDefaultNamespace(id)),"Missing original effect sound "+id);helper.succeed();
    }
    private FocusEffectGameTests(){}
}
