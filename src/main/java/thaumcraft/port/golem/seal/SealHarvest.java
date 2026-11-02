package thaumcraft.port.golem.seal;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

public class SealHarvest implements ISeal, ISealGui, ISealConfigArea, ISealConfigToggles {
    private int delay=new Random().nextInt(33), count;
    private final Map<Long,ReplantInfo> replantTasks=new HashMap<>();
    private static final Map<Block,Predicate<BlockState>> STANDARD=new HashMap<>(), CLICKABLE=new HashMap<>(), STACKED=new HashMap<>();
    private static final Map<Block,ItemStack> SEEDS=new HashMap<>();
    protected SealToggle[] props={new SealToggle(true,"prep","golem.prop.replant"),new SealToggle(false,"ppro","golem.prop.provision")};
    public static void registerSeed(Block block,ItemStack seed) { SEEDS.put(block,seed.copyWithCount(1)); }
    public static void registerStandardCrop(Block block,Predicate<BlockState> grown) { STANDARD.put(block,grown); }
    public static void registerClickableCrop(Block block,Predicate<BlockState> grown) { CLICKABLE.put(block,grown); }
    public static void registerStackedCrop(Block block,Predicate<BlockState> grown) { STACKED.put(block,grown); }
    private static boolean matches(Map<Block,Predicate<BlockState>> types,BlockState state) {
        var predicate=types.get(state.getBlock()); return predicate!=null && predicate.test(state);
    }
    public static boolean isGrownCrop(Level level,BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var state=level.getBlockState(pos); var block=state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block instanceof CocoaBlock) return state.getValue(CocoaBlock.AGE)==2;
        if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE)==3;
        if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN) || matches(STANDARD,state) || matches(CLICKABLE,state)) return true;
        return (state.is(Blocks.CACTUS) || state.is(Blocks.SUGAR_CANE) || matches(STACKED,state))
                && level.getBlockState(pos.below()).is(block);
    }
    private static ItemStack seed(Level level,BlockPos pos,BlockState state) {
        var custom=SEEDS.get(state.getBlock());
        if (custom!=null) return custom.copy();
        if (state.getBlock() instanceof CropBlock) return state.getBlock().getCloneItemStack(level,pos,state);
        if (state.getBlock() instanceof CocoaBlock) return new ItemStack(Items.COCOA_BEANS);
        if (state.getBlock() instanceof NetherWartBlock) return new ItemStack(Items.NETHER_WART);
        return ItemStack.EMPTY;
    }
    @Override public String getKey() { return "thaumcraft:harvest"; }
    @Override public void tickSeal(Level level,ISealEntity seal) {
        if (level.isClientSide) return;
        if (delay%100==0) {
            var bounds=GolemHelper.getBoundsForArea(seal); var iterator=replantTasks.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry=iterator.next(); if (bounds.contains(Vec3.atCenterOf(entry.getValue().pos))) continue;
                var old=TaskHandler.getTask(level,entry.getValue().taskId);
                if (old!=null) old.setSuspended(true);
                iterator.remove();
                SealHandler.markChunkAsDirty(level,seal.getSealPos().pos);
            }
        }
        if (delay++%5!=0) return;
        var pos=GolemHelper.getPosInArea(seal,count++);
        if (!level.hasChunkAt(pos)) return;
        if (isGrownCrop(level,pos)) {
            var task=new Task(seal.getSealPos(),pos); task.setPriority(seal.getPriority()); TaskHandler.addTask(level,task);
        } else if (props[0].value && level.isEmptyBlock(pos)) {
            var info=replantTasks.get(pos.asLong());
            if (info!=null && TaskHandler.getTask(level,info.taskId)==null) {
                var task=new Task(seal.getSealPos(),info.pos); task.setPriority(seal.getPriority());
                info.taskId=task.getId(); TaskHandler.addTask(level,task);
            }
        }
    }
    @Override public boolean onTaskCompletion(Level level,IGolemAPI golem,Task task) {
        var pos=task.getPos();
        if (isGrownCrop(level,pos)) {
            var original=level.getBlockState(pos); var plantSeed=seed(level,pos,original);
            boolean clickable=matches(CLICKABLE,original);
            GolemInteractionHelper.golemClick(level,golem,pos,task.getSealPos().face,ItemStack.EMPTY,false,true);
            if (!clickable && isGrownCrop(level,pos) && GolemInteractionHelper.harvestBlock(level,golem,pos,true,false)) {
                golem.addRankXp(1); golem.swingArm();
                if (props[0].value && !plantSeed.isEmpty() && level.isEmptyBlock(pos)) {
                    var soil=level.getBlockState(pos.below()); Direction support=null;
                    if (original.getBlock() instanceof CocoaBlock) support=original.getValue(CocoaBlock.FACING);
                    else if (original.canSurvive(level,pos)) support=Direction.DOWN;
                    if (support!=null) {
                        var replant=new Task(task.getSealPos(),pos); replant.setPriority(task.getPriority()); replant.setLifespan((short)300);
                        replantTasks.put(pos.asLong(),new ReplantInfo(pos,support,replant.getId(),plantSeed,soil.getBlock() instanceof FarmBlock));
                        TaskHandler.addTask(level,replant);
                        SealHandler.markChunkAsDirty(level,task.getSealPos().pos);
                    }
                }
            }
        } else if (props[0].value && level.hasChunkAt(pos) && level.isEmptyBlock(pos)) {
            var info=replantTasks.get(pos.asLong());
            if (info!=null && info.taskId==task.getId() && golem.isCarrying(info.stack)) {
                var soil=level.getBlockState(pos.below());
                if (info.farmland && (soil.is(Blocks.DIRT) || soil.is(Blocks.GRASS_BLOCK))) GolemInteractionHelper.tillSoil(level,golem,pos.below());
                var owned=golem.dropItem(info.stack.copyWithCount(1));
                if (!owned.isEmpty()) {
                    var result=GolemInteractionHelper.golemClick(level,golem,pos.relative(info.face),info.face.getOpposite(),owned,false,true,false);
                    if (result.consumesAction() && !level.isEmptyBlock(pos)) level.levelEvent(2001,pos,Block.getId(level.getBlockState(pos)));
                }
            }
        }
        task.setSuspended(true); return true;
    }
    @Override public boolean canGolemPerformTask(IGolemAPI golem,Task task) {
        var info=replantTasks.get(task.getPos().asLong());
        if (info!=null && info.taskId==task.getId()) {
            if (!props[0].value) { task.setSuspended(true); return false; }
            boolean carrying=golem.isCarrying(info.stack);
            if (!carrying && props[1].value) {
                var seal=SealHandler.getSealEntity(golem.getGolemWorld(),task.getSealPos());
                if (seal!=null) GolemHelper.requestProvisioning(golem.getGolemWorld(),seal,info.stack.copy());
            }
            return carrying;
        }
        return isGrownCrop(golem.getGolemWorld(),task.getPos());
    }
    @Override public void readCustomNBT(CompoundTag tag,HolderLookup.Provider registries) {
        replantTasks.clear(); var list=tag.getList("replant",Tag.TAG_COMPOUND);
        for (int i=0;i<list.size();i++) {
            var entry=list.getCompound(i); var face=entry.getByte("taskface");
            if (face<0 || face>=6) continue;
            var stack=ItemStack.parseOptional(registries,entry.getCompound("stack"));
            if (stack.isEmpty()) continue;
            var pos=BlockPos.of(entry.getLong("taskloc"));
            replantTasks.put(pos.asLong(),new ReplantInfo(pos,Direction.from3DDataValue(face),0,stack,entry.getBoolean("farmland")));
        }
    }
    @Override public void writeCustomNBT(CompoundTag tag,HolderLookup.Provider registries) {
        if (!props[0].value) return;
        var list=new ListTag();
        for (var info : replantTasks.values()) {
            var entry=new CompoundTag(); entry.putLong("taskloc",info.pos.asLong()); entry.putByte("taskface",(byte)info.face.get3DDataValue());
            entry.putBoolean("farmland",info.farmland); entry.put("stack",info.stack.save(registries)); list.add(entry);
        }
        tag.put("replant",list);
    }
    @Override public boolean canPlaceAt(Level level,BlockPos pos,Direction face) { return !level.isEmptyBlock(pos); }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_harvest"); }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public int[] getGuiCategories() { return new int[]{2,3,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return new EnumGolemTrait[]{EnumGolemTrait.DEFT,EnumGolemTrait.SMART}; }
    @Override public EnumGolemTrait[] getForbiddenTags() { return null; }
    private static final class ReplantInfo {
        final BlockPos pos; final Direction face; int taskId; final ItemStack stack; final boolean farmland;
        ReplantInfo(BlockPos pos,Direction face,int taskId,ItemStack stack,boolean farmland) {
            this.pos=pos.immutable(); this.face=face; this.taskId=taskId; this.stack=stack.copyWithCount(1); this.farmland=farmland;
        }
    }
}
