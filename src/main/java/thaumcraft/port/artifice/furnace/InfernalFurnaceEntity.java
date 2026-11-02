package thaumcraft.port.artifice.furnace;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import thaumcraft.port.essentia.FurnaceBellows;
import thaumcraft.port.world.AuraSystem;

/** BETA26's fuel-free 32-slot furnace; vis buys twenty completed boosted smelts. */
public final class InfernalFurnaceEntity extends BlockEntity implements WorldlyContainer {
    private static final int[] SLOTS=java.util.stream.IntStream.range(0,32).toArray(),NONE={};
    private final NonNullList<ItemStack> items=NonNullList.withSize(32,ItemStack.EMPTY);
    public int furnaceCookTime,furnaceMaxCookTime,speedyTime;
    public InfernalFurnaceEntity(BlockPos pos,BlockState state){super(InfernalFurnaceContent.ENTITY.get(),pos,state);}
    public Direction output(){return getBlockState().getValue(InfernalFurnaceBlock.FACING).getOpposite();}
    public int bellows(){return level==null?0:FurnaceBellows.infernal(level,worldPosition);}
    public int calcCookTime(){return FurnaceBellows.infernalCookTime(speedyTime>0,bellows());}
    private Optional<RecipeHolder<SmeltingRecipe>> recipe(ItemStack stack){
        return stack.isEmpty()||level==null?Optional.empty():level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(stack),level);
    }
    public boolean canSmelt(ItemStack stack){return recipe(stack).filter(r->!r.value().assemble(new SingleRecipeInput(stack),level.registryAccess()).isEmpty()).isPresent();}
    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        int oldCook=furnaceCookTime,oldSpeed=speedyTime;
        boolean cooked=furnaceCookTime>0;
        if(cooked)furnaceCookTime--;
        if(furnaceMaxCookTime<=0)furnaceMaxCookTime=calcCookTime();
        furnaceCookTime=Math.min(furnaceCookTime,furnaceMaxCookTime);
        if(furnaceCookTime<=0&&cooked){
            for(int slot=0;slot<items.size();slot++){
                var input=items.get(slot);if(input.isEmpty())continue;
                var recipe=recipe(input);
                var result=recipe.map(r->r.value().assemble(new SingleRecipeInput(input),server.registryAccess())).orElse(ItemStack.EMPTY);
                if(!result.isEmpty()){
                    if(speedyTime>0)speedyTime--;
                    eject(result,input,recipe.orElseThrow().value().getExperience());
                    server.blockEvent(worldPosition,getBlockState().getBlock(),3,0);
                    if(server.random.nextInt(20)==0)AuraSystem.pollute(server,worldPosition.relative(output()),1,true);
                    removeItem(slot,1);break;
                }
                setItem(slot,ItemStack.EMPTY);
            }
        }
        // The original also fills this reserve while idle; fractional vis is consumed before truncation.
        if(speedyTime<=0)speedyTime=(int)AuraSystem.drain(server,worldPosition,20,false,false);
        if(furnaceCookTime==0&&!cooked)for(var input:items)if(canSmelt(input)){
            furnaceCookTime=furnaceMaxCookTime=calcCookTime();break;
        }
        if(oldCook!=furnaceCookTime||oldSpeed!=speedyTime)setChanged();
    }
    /** Physical intake destroys unsmeltable stacks, whereas the top automation inventory accepts them. */
    public ItemStack accept(ItemStack stack){
        if(!(level instanceof ServerLevel server)||stack.isEmpty())return stack;
        if(canSmelt(stack))return ItemHandlerHelper.insertItemStacked(new SidedInvWrapper(this,Direction.UP),stack,false);
        server.playSound(null,worldPosition,SoundEvents.LAVA_EXTINGUISH,SoundSource.BLOCKS,.3f,2.6f+(server.random.nextFloat()-server.random.nextFloat())*.8f);
        server.sendParticles(ParticleTypes.LAVA,worldPosition.getX()+server.random.nextFloat(),worldPosition.getY()+1,worldPosition.getZ()+server.random.nextFloat(),1,0,0,0,0);
        return ItemStack.EMPTY;
    }
    private void eject(ItemStack result,ItemStack input,float experience){
        var outputs=new ArrayList<ItemStack>();outputs.add(result.copy());
        outputs.addAll(InfernalFurnaceBonuses.roll(input,bellows(),level.random::nextFloat));
        for(var stack:outputs)ejectStack(stack);
        int count=result.getCount();
        if(experience==0)count=0;
        else if(experience<1){float exact=count*experience;count=Mth.floor(exact);if(count<Mth.ceil(exact)&&level.random.nextFloat()<exact-count)count++;}
        // Source values >= 1 award one XP per output, rather than multiplying by the recipe XP.
        var face=output();
        while(count>0){
            int value=ExperienceOrb.getExperienceValue(count);count-=value;
            var orb=new ExperienceOrb(level,worldPosition.getX()+.5+face.getStepX()*1.2,worldPosition.getY()+.4,worldPosition.getZ()+.5+face.getStepZ()*1.2,value);
            double vx=face.getStepX()==0?(level.random.nextFloat()-level.random.nextFloat())*.025:face.getStepX()*.13;
            double vz=face.getStepZ()==0?(level.random.nextFloat()-level.random.nextFloat())*.025:face.getStepZ()*.13;
            orb.setDeltaMovement(vx,0,vz);level.addFreshEntity(orb);
        }
    }
    private void ejectStack(ItemStack stack){
        var face=output();var mouth=worldPosition.relative(face);ItemStack remainder=stack;
        if(level.hasChunkAt(mouth)){
            var target=level.getCapability(Capabilities.ItemHandler.BLOCK,mouth,face.getOpposite());
            if(target!=null)remainder=ItemHandlerHelper.insertItemStacked(target,remainder,false);
        }
        if(remainder.isEmpty())return;
        var origin=level.getBlockState(mouth).isRedstoneConductor(level,mouth)?worldPosition.relative(face.getOpposite()):worldPosition;
        var dropped=new ItemEntity(level,origin.getX()+.5+face.getStepX(),origin.getY(),origin.getZ()+.5+face.getStepZ(),remainder);
        dropped.setDeltaMovement(face.getStepX()*.3,0,face.getStepZ()*.3);level.addFreshEntity(dropped);
    }
    @Override public boolean triggerEvent(int event,int data){if(event==3){if(level!=null&&level.isClientSide)InfernalFurnaceVisuals.complete(this);return true;}return super.triggerEvent(event,data);}
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.UP?SLOTS:NONE;}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return side==Direction.UP;}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
    @Override public int getContainerSize(){return items.size();}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty())setChanged();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);stack.limitSize(getMaxStackSize(stack));setChanged();}
    @Override public void clearContent(){items.clear();setChanged();}
    @Override public boolean stillValid(Player player){return Container.stillValidBlockEntity(this,player);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);ContainerHelper.saveAllItems(tag,items,lookup);tag.putShort("CookTime",(short)furnaceCookTime);tag.putShort("SpeedyTime",(short)speedyTime);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);items.clear();ContainerHelper.loadAllItems(tag,items,lookup);furnaceCookTime=Math.max(0,tag.getShort("CookTime"));speedyTime=Math.max(0,tag.getShort("SpeedyTime"));furnaceMaxCookTime=0;}
}
