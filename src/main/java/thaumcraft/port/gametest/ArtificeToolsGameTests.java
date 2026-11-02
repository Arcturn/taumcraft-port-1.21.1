package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.artifice.tool.*;
import thaumcraft.port.infusion.brain.*;
import thaumcraft.port.infusion.content.InfusionCraftingContent;
import thaumcraft.port.infusion.structure.ItemCharge;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ArtificeToolsGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void brainJarCollectsMergedExperienceAndPausesAfterRelease(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(2,2,2));var level=h.getLevel();level.setBlockAndUpdate(pos,InfusionCraftingContent.JAR_BRAIN.get().defaultBlockState());var jar=(BrainJarEntity)level.getBlockEntity(pos);
        var orb=new ExperienceOrb(level,pos.getX()+.5,pos.getY()+.4,pos.getZ()+.5,17);var merged=new CompoundTag();orb.addAdditionalSaveData(merged);merged.putInt("Count",3);orb.readAdditionalSaveData(merged);level.addFreshEntity(orb);
        jar.serverTick();h.assertValueEqual(jar.experience(),51,"Merged XP orb lost its additional denominations");h.assertTrue(orb.isRemoved(),"Jar duplicated the collected XP orb");h.assertValueEqual(jar.comparator(),1,"Nonempty jar comparator is missing");
        int released=jar.releaseExperience();h.assertTrue(released>=0&&released<=51,"Release exceeded stored XP");h.assertValueEqual(jar.experience()+released,51,"XP was lost during release");h.assertValueEqual(jar.delay(),40,"Jar must wait forty ticks after release");
        var waiting=new ExperienceOrb(level,pos.getX()+.5,pos.getY()+.4,pos.getZ()+.5,7);level.addFreshEntity(waiting);
        int before=jar.experience();for(int i=0;i<40;i++)jar.serverTick();h.assertValueEqual(jar.experience(),before,"Jar recaptured XP during cooldown");h.assertTrue(waiting.isAlive(),"Cooldown swallowed the waiting orb");jar.serverTick();h.assertTrue(waiting.isRemoved(),"Jar did not resume collecting after cooldown");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void brainJarLootPlacementAndSavePreserveExperience(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,2,2));var state=InfusionCraftingContent.JAR_BRAIN.get().defaultBlockState();level.setBlockAndUpdate(pos,state);var jar=(BrainJarEntity)level.getBlockEntity(pos);jar.restoreExperience(1437);
        var drops=Block.getDrops(state,level,pos,jar);var drop=drops.stream().filter(s->s.getItem() instanceof BrainJarItem).findFirst().orElseThrow();h.assertValueEqual(BrainJarItem.experience(drop),1437,"Breaking jar lost its stored XP");
        var clone=state.getBlock().getCloneItemStack(level,pos,state);h.assertValueEqual(BrainJarItem.experience(clone),1437,"Pick block lost XP");
        var player=GameTestPlayers.create(h);var destination=pos.offset(2,0,0);level.setBlockAndUpdate(destination.below(),Blocks.STONE.defaultBlockState());player.setItemInHand(InteractionHand.MAIN_HAND,drop.copy());
        var context=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,player.getMainHandItem(),new BlockHitResult(Vec3.atCenterOf(destination.below()).add(0,.5,0),Direction.UP,destination.below(),false));
        ((BrainJarItem)drop.getItem()).place(context);h.assertTrue(level.getBlockEntity(destination) instanceof BrainJarEntity,"Portable jar failed placement");var placed=(BrainJarEntity)level.getBlockEntity(destination);h.assertValueEqual(placed.experience(),1437,"Placement did not restore portable XP");
        var saved=placed.saveWithoutMetadata(level.registryAccess());var restored=new BrainJarEntity(destination,state);restored.loadWithComponents(saved,level.registryAccess());h.assertValueEqual(restored.experience(),1437,"World save lost XP");
        jar.restoreExperience(2000);h.assertValueEqual(jar.comparator(),15,"Full jar must produce comparator strength fifteen");h.assertValueEqual(state.getEnchantPowerBonus(level,pos),5f,"Brain jar lost source enchantment power");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void brainJarKeepsOverflowOfMergedOrbsOutsideItsCapacity(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,2,2));level.setBlockAndUpdate(pos,InfusionCraftingContent.JAR_BRAIN.get().defaultBlockState());var jar=(BrainJarEntity)level.getBlockEntity(pos);jar.restoreExperience(1999);
        var orb=new ExperienceOrb(level,pos.getX()+.5,pos.getY()+.4,pos.getZ()+.5,17);var data=new CompoundTag();orb.addAdditionalSaveData(data);data.putInt("Count",3);orb.readAdditionalSaveData(data);level.addFreshEntity(orb);jar.serverTick();
        h.assertValueEqual(jar.experience(),2000,"Merged orb overflow exceeded jar capacity");long remaining=0;int entities=0;
        for(var found:level.getEntitiesOfClass(ExperienceOrb.class,new net.minecraft.world.phys.AABB(pos).inflate(.1),net.minecraft.world.entity.Entity::isAlive)){var tag=new CompoundTag();found.addAdditionalSaveData(tag);remaining+=(long)found.getValue()*Math.max(1,tag.getInt("Count"));entities++;}
        h.assertTrue(remaining==50,"Near-full jar destroyed overflow XP");h.assertTrue(entities<=2,"Merged remainder expanded into an unbounded number of entities");jar.serverTick();h.assertValueEqual(jar.experience(),2000,"Full jar continued accepting XP");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void grappleRequiresChargeReplacesPreviousHookAndClearsLoadedState(GameTestHelper h){
        var player=GameTestPlayers.create(h);var gun=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:grapple_gun")));var item=(GrappleGunItem)gun.getItem();player.setItemInHand(InteractionHand.OFF_HAND,gun);
        item.use(h.getLevel(),player,InteractionHand.OFF_HAND);h.assertTrue(GrappleEntity.active(player)==null,"Empty gun fired a hook");
        ItemCharge.add(gun,player,2);item.use(h.getLevel(),player,InteractionHand.OFF_HAND);var first=GrappleEntity.active(player);
        h.assertTrue(first!=null&&first.hand()==InteractionHand.OFF_HAND,"Offhand hook missing or uses wrong hand");h.assertValueEqual(ItemCharge.get(gun),1,"Shot must cost one charge");h.assertTrue(GrappleGunItem.loaded(gun),"Gun did not switch to fired model");
        item.use(h.getLevel(),player,InteractionHand.OFF_HAND);var second=GrappleEntity.active(player);h.assertTrue(second!=null&&second!=first&&first.isRemoved(),"Second shot left two active ropes");h.assertValueEqual(ItemCharge.get(gun),0,"Second shot charge incorrect");
        second.discard();item.inventoryTick(gun,h.getLevel(),player,40,false);h.assertTrue(!GrappleGunItem.loaded(gun),"Gun retained fired appearance after rope removal");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void grappleAnchorsPullsAndSneakingDetachesWithoutFallDamage(GameTestHelper h){
        var level=h.getLevel();var player=GameTestPlayers.create(h);var anchor=h.absolutePos(new BlockPos(3,3,3));level.setBlockAndUpdate(anchor,Blocks.STONE.defaultBlockState());
        player.setPos(anchor.getX()-4,anchor.getY()-1,anchor.getZ()+.5);player.setDeltaMovement(Vec3.ZERO);player.fallDistance=9;
        var hook=new GrappleEntity(level,player,InteractionHand.MAIN_HAND);hook.setPos(anchor.getX()-.2,anchor.getY()+.5,anchor.getZ()+.5);hook.setDeltaMovement(.5,0,0);level.addFreshEntity(hook);hook.activate();hook.tick();
        h.assertTrue(hook.pulling(),"Hook failed to anchor to collision");h.assertTrue(player.getDeltaMovement().x>0&&player.getDeltaMovement().y>.4,"Anchored rope did not pull and boost its owner");h.assertValueEqual(player.fallDistance,0f,"Rope did not reset fall damage");
        var saved=new CompoundTag();hook.saveWithoutId(saved);var restored=new GrappleEntity(ArtificeTools.GRAPPLE.get(),level);restored.load(saved);h.assertTrue(restored.pulling(),"Saved rope lost attachment");h.assertTrue(restored.hand()==InteractionHand.MAIN_HAND,"Saved rope changed hand");
        player.setShiftKeyDown(true);hook.tick();h.assertTrue(hook.isRemoved()&&GrappleEntity.active(player)==null,"Sneaking did not release rope");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void flyingGrappleExpiresAfterThirtyTicks(GameTestHelper h){
        var player=GameTestPlayers.create(h);var hook=new GrappleEntity(h.getLevel(),player,InteractionHand.MAIN_HAND);hook.setPos(player.position().add(0,30,0));hook.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(hook);hook.activate();
        // ServerLevel advances tickCount before calling tick(); direct entity.tick() does not.
        h.runAfterDelay(28,()->h.assertTrue(hook.isAlive(),"Missed grapple expired before its source flight window"));
        h.runAfterDelay(33,()->{h.assertTrue(hook.isRemoved()&&GrappleEntity.active(player)==null,"Missed grapple remained active beyond source thirty-tick flight");h.succeed();});
    }
    private ArtificeToolsGameTests(){}
}
