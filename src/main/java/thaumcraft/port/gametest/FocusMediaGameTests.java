package thaumcraft.port.gametest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.auromancy.entity.*;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.auromancy.focus.medium.*;
import thaumcraft.port.content.PortContent;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class FocusMediaGameTests {
    @GameTest(template="empty",timeoutTicks=60)
    public static void cloudKeepsBeta26FiveTickCadenceAndCuboidRange(GameTestHelper helper) {
        registerCounter();var caster=sheep(helper,new Vec3(8,4,8));var target=sheep(helper,new Vec3(2.9,4,2.9));
        var origin=helper.absoluteVec(new Vec3(2,4,2));var cloud=new EntityFocusCloud(pack(caster),new Trajectory(origin,new Vec3(1,0,0)),1,5);helper.getLevel().addFreshEntity(cloud);
        helper.runAfterDelay(21,()-> {helper.assertValueEqual(CounterEffect.hits.getOrDefault(target.getUUID(),0),4,"Cloud must hit every five ticks, including the corners of its original cuboid area");cloud.discard();caster.discard();target.discard();helper.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void mineArmsForFortyTicksAndOnlySelectsConfiguredFriends(GameTestHelper helper) {
        registerCounter();var caster=sheep(helper,new Vec3(2,4,2));var hostile=sheep(helper,new Vec3(2.9,4,2.9));
        for(int x=1;x<=3;x++)for(int z=1;z<=3;z++)helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(x,3,z)),Blocks.STONE.defaultBlockState());
        var mine=new EntityFocusMine(pack(caster),new Trajectory(caster.position(),new Vec3(1,0,0)),true);mine.setIsArmed(true);helper.getLevel().addFreshEntity(mine);
        helper.runAfterDelay(35,()->helper.assertTrue(CounterEffect.hits.getOrDefault(caster.getUUID(),0)==0,"Mine fired before its 40-tick arming delay"));
        helper.runAfterDelay(46,()-> {helper.assertValueEqual(CounterEffect.hits.getOrDefault(caster.getUUID(),0),1,"Friendly mine did not select its caster");helper.assertValueEqual(CounterEffect.hits.getOrDefault(hostile.getUUID(),0),0,"Friendly mine affected an enemy");helper.assertTrue(mine.isRemoved(),"Triggered mine survived");caster.discard();hostile.discard();helper.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void intermediariesKeepProgramHandAgeAndArmingAcrossSave(GameTestHelper helper) {
        registerCounter();var caster=sheep(helper,new Vec3(8,4,8));var pack=pack(caster);pack.setCastingHand(InteractionHand.OFF_HAND);
        var mine=new EntityFocusMine(pack,new Trajectory(caster.position(),new Vec3(1,0,0)),true);mine.setIsArmed(true);mine.tickCount=731;
        var tag=new CompoundTag();mine.saveWithoutId(tag);tag.putInt("Arming",17);var loaded=new EntityFocusMine(AuromancyEntities.MINE.get(),helper.getLevel());loaded.load(tag);
        helper.assertValueEqual(loaded.tickCount,731,"Mine age reset after reload");helper.assertValueEqual(loaded.getArmingTicks(),17,"Mine arming delay reset after reload");helper.assertTrue(loaded.isFriendly()&&loaded.getIsArmed(),"Mine target/arming mode was lost");helper.assertTrue(loaded.getFocusPackage().getCastingHand()==InteractionHand.OFF_HAND,"Offhand cast context was lost");helper.assertTrue(loaded.getCaster()==caster,"Saved program did not resolve its caster");
        var cloud=new EntityFocusCloud(pack,new Trajectory(caster.position(),new Vec3(1,0,0)),3,30);cloud.tickCount=415;tag=new CompoundTag();cloud.saveWithoutId(tag);var savedCloud=new EntityFocusCloud(AuromancyEntities.CLOUD.get(),helper.getLevel());savedCloud.load(tag);
        helper.assertTrue(savedCloud.getRadius()==3&&savedCloud.getDuration()==30&&savedCloud.tickCount==415,"Cloud dimensions/lifetime were not persisted");caster.discard();helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void planSurfaceIsConnectedAndVolumeExtendsBehindClickedFace(GameTestHelper helper) {
        var stack=new ItemStack(PortContent.CASTER_BASIC.get());var data=new CompoundTag();data.putInt("areax",1);data.putInt("areay",1);data.putInt("areaz",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        var origin=helper.absolutePos(new BlockPos(3,4,3));var level=helper.getLevel();
        for(int x=-2;x<=2;x++)for(int y=-1;y<=1;y++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(origin,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(origin.east(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(origin.west().south(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(origin.below(),Blocks.DIRT.defaultBlockState());level.setBlockAndUpdate(origin.above(),Blocks.STONE.defaultBlockState());
        var plan=new FocusMediumPlan();plan.initialize();plan.getSetting("method").setValue(1);var surface=plan.getArchitectBlocks(stack,level,origin,Direction.UP,null);
        helper.assertTrue(surface.contains(origin)&&surface.contains(origin.east()),"Surface plan lost connected equal-state blocks");helper.assertFalse(surface.contains(origin.west().south()),"Surface plan crossed an air gap");helper.assertFalse(surface.contains(origin.below()),"Surface plan crossed to a different plane/material");
        plan.getSetting("method").setValue(0);var volume=plan.getArchitectBlocks(stack,level,origin,Direction.UP,null);
        helper.assertTrue(volume.contains(origin.below()),"Volume did not extend behind the clicked face");helper.assertFalse(volume.contains(origin.above()),"Volume extended in front of the clicked face");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void projectileBounceRetainsOriginalAxisDamping(GameTestHelper helper) {
        var input=new Vec3(1,2,3);var floor=EntityFocusProjectile.reflected(input,Direction.Axis.Y);var wall=EntityFocusProjectile.reflected(input,Direction.Axis.X);
        helper.assertTrue(floor.distanceTo(new Vec3(.9,-1.62,2.7))<1e-8,"Floor bounce must apply .9 reflection plus .9 global damping");helper.assertTrue(wall.distanceTo(new Vec3(-.9,1.8,2.7))<1e-8,"Wall bounce must apply .9 global damping");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void mineRenderAnglesFollowFlightAndWrapWithoutSnapping(GameTestHelper helper) {
        registerCounter();var caster=sheep(helper,new Vec3(8,8,8));
        var mine=new EntityFocusMine(pack(caster),new Trajectory(helper.absoluteVec(new Vec3(2,8,2)),new Vec3(1,0,0)),false);
        mine.setDeltaMovement(1,-1,0);mine.tick();
        helper.assertTrue(Math.abs(mine.getYRot()-18)<.01,"Mine yaw did not inherit the original throwable's 20% smoothing");
        helper.assertTrue(Math.abs(mine.getXRot()+9)<.01,"Mine pitch stayed flat instead of following its fall");
        mine.setYRot(179);mine.setXRot(0);double angle=Math.toRadians(-179);mine.setDeltaMovement(Math.sin(angle),0,Math.cos(angle));mine.tick();
        helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(mine.getYRot()-179.4f))<.01,"Mine made a full turn at the +/-180 degree boundary");
        caster.discard();helper.succeed();
    }
    private static Sheep sheep(GameTestHelper helper,Vec3 relative) {var sheep=EntityType.SHEEP.create(helper.getLevel());sheep.setPos(helper.absoluteVec(relative));sheep.setNoAi(true);sheep.setNoGravity(true);helper.getLevel().addFreshEntity(sheep);return sheep;}
    private static FocusPackage pack(Sheep caster) {var pack=new FocusPackage(caster);var effect=new CounterEffect();effect.initialize();pack.addNode(effect);pack.setUniqueID(UUID.randomUUID());return pack;}
    private static void registerCounter() {if(!FocusEngine.elements.containsKey("thaumcraft.TEST_MEDIA_COUNTER"))FocusEngine.registerElement(CounterEffect.class,ResourceLocation.parse("thaumcraft:textures/foci/root.png"),0xFFFFFF);}
    public static final class CounterEffect extends FocusEffect {
        static final Map<UUID,Integer> hits=new HashMap<>();
        @Override public String getKey(){return "thaumcraft.TEST_MEDIA_COUNTER";}
        @Override public String getResearch(){return null;}
        @Override public int getComplexity(){return 1;}
        @Override public thaumcraft.port.aspect.Aspect getAspect(){return null;}
        @Override public void renderParticleFX(net.minecraft.world.level.Level level,double x,double y,double z,double vx,double vy,double vz){}
        @Override public boolean execute(HitResult target,Trajectory trajectory,float power,int num){if(target instanceof EntityHitResult hit)hits.merge(hit.getEntity().getUUID(),1,Integer::sum);return true;}
    }
}
