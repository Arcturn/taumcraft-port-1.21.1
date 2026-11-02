package thaumcraft.port.gametest;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.artifice.mirror.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.*;
import thaumcraft.port.infusion.content.*;
import thaumcraft.port.infusion.matrix.MatrixSources;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class MirrorGameTests {
    private static MirrorEntity mirror(ServerLevel level,BlockPos pos,boolean essentia){
        level.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,(essentia?InfusionCraftingContent.MIRROR_ESSENTIA:InfusionCraftingContent.MIRROR).get().defaultBlockState().setValue(InfusionMirrorBlock.FACING,Direction.UP),3);
        return (MirrorEntity)level.getBlockEntity(pos);
    }
    private static MirrorEntity mirror(GameTestHelper h,int x,int z,boolean essentia){return mirror(h.getLevel(),h.absolutePos(new BlockPos(x,2,z)),essentia);}
    private static ItemStack handMirror(){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:hand_mirror")));}
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void linkedItemTransferAndSimulationHaveSingleOwnership(GameTestHelper h){
        var a=mirror(h,1,1,false);var b=mirror(h,4,1,false);b.bind(a.address());
        h.assertTrue(a.peer()==b&&b.peer()==a,"Placement did not establish a reciprocal pair");
        var handler=Capabilities.ItemHandler.BLOCK.getCapability(h.getLevel(),a.getBlockPos(),null,null,Direction.UP);
        h.assertTrue(handler!=null,"Mirror item capability is absent");var stack=new ItemStack(Items.DIAMOND,7);
        h.assertTrue(handler.insertItem(0,stack,true).isEmpty(),"Valid simulated insertion was rejected");
        h.assertValueEqual(b.queuedCount(),0,"Simulation queued paid items");h.assertValueEqual(a.instability,0,"Simulation charged instability");
        h.assertTrue(handler.insertItem(0,stack,false).isEmpty(),"Actual insertion failed");stack.setCount(0);
        h.assertValueEqual(b.queuedCount(),7,"Insertion failed to transfer custody exactly once");h.assertValueEqual(a.instability,7,"Item input instability differs from source");
        for(int i=0;i<28;i++)b.tick();
        int ejected=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(b.getBlockPos()).inflate(1),e->e.getItem().is(Items.DIAMOND)).stream().mapToInt(e->e.getItem().getCount()).sum();
        h.assertValueEqual(ejected+b.queuedCount(),7,"Ejection lost or duplicated paid items");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(b.getBlockPos()).inflate(1),e->e.getItem().is(Items.DIAMOND)).stream().allMatch(ItemEntity::hasPickUpDelay),"Ejected items can immediately bounce back through the mirror");
        var incoming=new ItemEntity(h.getLevel(),a.getBlockPos().getX()+.5,a.getBlockPos().getY()+.25,a.getBlockPos().getZ()+.5,new ItemStack(Items.IRON_INGOT,3));incoming.setPickUpDelay(20);h.getLevel().addFreshEntity(incoming);
        h.assertFalse(a.transport(incoming),"Pickup delay did not prevent immediate item recapture");incoming.setNoPickUpDelay();int queued=b.queuedCount();
        h.assertTrue(a.transport(incoming),"Dropped item was not transported");h.assertTrue(incoming.isRemoved(),"Accepted item entity retained source ownership");
        h.assertFalse(a.transport(incoming),"Removed item entity was accepted twice");h.assertValueEqual(b.queuedCount(),queued+3,"Dropped item transfer duplicated its contents");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void receiverReloadBreakAndReplacementKeepQueueAndBinding(GameTestHelper h){
        var a=mirror(h,1,1,false);var b=mirror(h,4,1,false);b.bind(a.address());b.enqueue(new ItemStack(Items.EMERALD,9));
        var saved=b.saveCustomOnly(h.getLevel().registryAccess());b.loadCustomOnly(saved,h.getLevel().registryAccess());
        h.assertValueEqual(b.queuedCount(),9,"Reload lost the paid queue");h.assertTrue(b.peer()==a,"Reload lost reciprocal binding");
        var drops=Block.getDrops(b.getBlockState(),h.getLevel(),b.getBlockPos(),b);var bound=drops.stream().filter(s->s.is(InfusionCraftingContent.MIRROR.get().asItem())).findFirst().orElseThrow();
        h.assertTrue(a.address().equals(MirrorLink.get(bound)),"Dropped mirror lost its destination");
        var oldPos=b.getBlockPos();h.getLevel().removeBlock(oldPos,false);
        h.assertFalse(a.linked,"Broken receiver left sender linked");
        int recovered=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(oldPos).inflate(1),e->e.getItem().is(Items.EMERALD)).stream().mapToInt(e->e.getItem().getCount()).sum();
        h.assertValueEqual(recovered,9,"Breaking receiver did not refund every queued item exactly once");
        var replacement=mirror(h,4,4,false);replacement.bind(MirrorLink.get(bound));
        h.assertTrue(a.peer()==replacement&&replacement.peer()==a,"Relocating the bound mirror did not restore the pair");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void unloadedTargetDoesNotLoadChunksConsumeOrEraseLink(GameTestHelper h){
        var a=mirror(h,1,1,false);var far=new BlockPos(28000000,80,28000000);var link=new MirrorLink(h.getLevel().dimension(),far);
        h.assertFalse(h.getLevel().hasChunkAt(far),"Fixture target chunk is unexpectedly loaded");a.bind(link);a.linked=true;
        var stack=new ItemStack(Items.IRON_INGOT,11);h.assertValueEqual(a.items.insertItem(0,stack,false).getCount(),11,"Unloaded receiver consumed items");
        for(int i=0;i<45;i++)a.tick();
        h.assertTrue(link.equals(a.destination),"Unloaded receiver erased a recoverable binding");h.assertFalse(h.getLevel().hasChunkAt(far),"Mirror loaded an absent destination chunk");h.assertValueEqual(a.queuedCount(),0,"Failed transfer created a source queue");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void crossDimensionPairTransfersOnlyToItsReciprocalPartner(GameTestHelper h){
        var a=mirror(h,1,1,false);var nether=h.getLevel().getServer().getLevel(Level.NETHER);h.assertTrue(nether!=null,"Nether dimension missing");
        var pos=new BlockPos(a.getBlockPos().getX(),120,a.getBlockPos().getZ());nether.getChunkAt(pos);var old=nether.getBlockState(pos);var oldSupport=nether.getBlockState(pos.below());
        try{
            var b=mirror(nether,pos,false);b.bind(a.address());h.assertTrue(a.peer()==b&&b.peer()==a,"Cross-dimension pair not reciprocal");
            h.assertTrue(a.items.insertItem(0,new ItemStack(Items.GOLD_INGOT,5),false).isEmpty(),"Cross-dimension insertion failed");h.assertValueEqual(b.queuedCount(),5,"Wrong destination received items");
            var c=mirror(h,4,4,false);c.bind(b.address());h.assertTrue(a.peer()==b&&c.peer()==null,"A third mirror stole an existing live pair");
            b.loadCustomOnly(b.saveCustomOnly(nether.registryAccess()),nether.registryAccess());h.assertTrue(a.peer()==b,"Dimension link changed after NBT round trip");
        }finally{nether.setBlock(pos,old,3);nether.setBlock(pos.below(),oldSupport,3);}
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void essentiaMirrorFeedsMatrixAndNeverRecursesThroughMirrors(GameTestHelper h){
        var a=mirror(h,1,1,true);var b=mirror(h,5,1,true);b.bind(a.address());
        var jarPos=b.getBlockPos().above(2);h.getLevel().setBlock(jarPos,EssentiaContent.JAR.get().defaultBlockState(),3);var jar=(JarEntity)h.getLevel().getBlockEntity(jarPos);jar.addRemote(Aspect.FIRE,3,false);
        var source=EssentiaContent.SOURCE.getCapability(h.getLevel(),a.getBlockPos(),null,null,null);h.assertTrue(source==a,"Mirror is not registered as an infusion source");
        h.assertValueEqual(a.takeRemote(Aspect.FIRE,2,false),0,"Mirror transferred more than one source unit");
        h.assertValueEqual(a.takeRemote(Aspect.FIRE,1,true),1,"Remote jar was not found through pair");h.assertValueEqual(jar.storedAmount(),3,"Simulation drained remote jar");
        h.assertValueEqual(a.takeRemote(Aspect.FIRE,1,false),1,"Remote drain failed");h.assertValueEqual(jar.storedAmount(),2,"Remote drain did not consume exactly one");h.assertValueEqual(a.instability,1,"Essentia transport instability differs from source");
        h.assertValueEqual(a.addRemote(Aspect.FIRE,1,false),1,"Remote insertion failed");h.assertValueEqual(jar.storedAmount(),3,"Remote insertion lost essentia");
        var matrix=new MatrixSources();var matrixSource=matrix.providing(h.getLevel(),a.getBlockPos().below(),Aspect.FIRE);h.assertTrue(matrixSource!=null&&matrixSource.pos().equals(a.getBlockPos()),"Matrix did not select the nearby mirror-backed source");
        h.getLevel().removeBlock(jarPos,false);h.assertValueEqual(a.takeRemote(Aspect.WATER,1,true),0,"Pair recursively discovered itself as an unlimited source");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void handMirrorMenuRetainsRejectedStacksAndCannotSendItself(GameTestHelper h){
        var destination=mirror(h,4,1,false);var player=GameTestPlayers.create(h);var held=handMirror();MirrorLink.set(held,destination.address());player.setItemInHand(InteractionHand.OFF_HAND,held);
        h.assertTrue(HandMirrorMenu.matchesHeldMirror(held,held.copy(),true),"A decoded client inventory snapshot invalidates the held mirror");
        h.assertFalse(HandMirrorMenu.matchesHeldMirror(held,held.copy(),false),"Server accepted a substituted mirror object");
        var menu=new HandMirrorMenu(41,player.getInventory(),InteractionHand.OFF_HAND);player.containerMenu=menu;
        menu.setCarried(new ItemStack(Items.REDSTONE,12));menu.clicked(0,0,ClickType.PICKUP,player);menu.broadcastChanges();menu.broadcastChanges();
        h.assertValueEqual(destination.queuedCount(),12,"Menu transfer duplicated or lost its accepted stack");h.assertTrue(menu.input.isEmpty()&&menu.getCarried().isEmpty(),"Accepted stack kept a second owner");
        h.assertFalse(menu.getSlot(0).mayPlace(held),"Hand mirror can send itself");
        MirrorLink.set(held,new MirrorLink(h.getLevel().dimension(),new BlockPos(28000000,80,28000000)));
        menu.setCarried(new ItemStack(Items.LAPIS_LAZULI,7));menu.clicked(0,0,ClickType.PICKUP,player);menu.broadcastChanges();
        h.assertValueEqual(menu.input.getItem(0).getCount(),7,"Unavailable target ate the pending stack");h.assertValueEqual(destination.queuedCount(),12,"Unavailable transfer affected old receiver");
        menu.removed(player);int returned=player.getInventory().items.stream().filter(s->s.is(Items.LAPIS_LAZULI)).mapToInt(ItemStack::getCount).sum();
        h.assertValueEqual(returned,7,"Closing rejected menu failed to return pending items");h.assertTrue(player.getOffhandItem()==held,"Offhand mirror was moved or consumed");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void losingSupportDropsBoundMirrorAndForwardsTransferEvents(GameTestHelper h){
        var a=mirror(h,1,1,false);var b=mirror(h,4,1,false);b.bind(a.address());b.enqueue(new ItemStack(Items.QUARTZ,3));
        h.assertTrue(b.getBlockState().triggerEvent(h.getLevel(),b.getBlockPos(),1,0),"Block failed to forward the transfer event to its entity");
        var pos=b.getBlockPos();h.getLevel().destroyBlock(pos.below(),false);
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"Mirror remained suspended after losing support");h.assertFalse(a.linked,"Detached mirror left partner linked");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1));
        h.assertValueEqual(drops.stream().filter(e->e.getItem().is(Items.QUARTZ)).mapToInt(e->e.getItem().getCount()).sum(),3,"Support loss did not refund queued items");
        h.assertTrue(drops.stream().filter(e->e.getItem().is(InfusionCraftingContent.MIRROR.get().asItem())).anyMatch(e->a.address().equals(MirrorLink.get(e.getItem()))),"Detached mirror dropped without its binding");h.succeed();
    }
    private MirrorGameTests(){}
}
