package thaumcraft.port.machine;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.*;
import thaumcraft.port.crafting.ArcaneRecipe;
import thaumcraft.port.research.*;
import thaumcraft.port.world.AuraSystem;

/** Result clicks revalidate and pay atomically, including when two players share one table. */
public final class WorkbenchMenu extends AbstractContainerMenu {
    public static final List<Aspect> PRIMALS=List.of(Aspect.AIR,Aspect.EARTH,Aspect.FIRE,Aspect.WATER,Aspect.ORDER,Aspect.ENTROPY);
    private final Container matrix,result=new SimpleContainer(1);
    private final Player player;
    private final BlockPos pos;
    // Keep the existing save-slot order: Aer, Terra, Ignis, Aqua, Ordo, Perditio.
    // The TC6 screen orders these visually by ShardType, which differs from that storage order.
    private static final int[] SHARD_ORDER={0,3,1,2,4,5};
    private static final int[] CRYSTAL_X={64,17,112,17,112,64},CRYSTAL_Y={13,35,35,93,93,115};
    private final ContainerData data=new SimpleContainerData(8);
    public static int shardId(int index){return SHARD_ORDER[index];}
    public static int crystalX(int index){return CRYSTAL_X[shardId(index)];}
    public static int crystalY(int index){return CRYSTAL_Y[shardId(index)];}
    public WorkbenchMenu(int id,Inventory inventory,Container matrix,BlockPos pos){
        super(MachineContent.WORKBENCH_MENU.get(),id);this.matrix=matrix;this.player=inventory.player;this.pos=pos;
        addSlot(new Slot(result,0,160,64){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});
        for(int y=0;y<3;y++)for(int x=0;x<3;x++)addSlot(new Slot(matrix,y*3+x,40+x*24,40+y*24));
        for(int i=0;i<6;i++){final Aspect aspect=PRIMALS.get(i);addSlot(new Slot(matrix,9+i,crystalX(i),crystalY(i)){public boolean mayPlace(ItemStack s){return s.getItem() instanceof CrystalItem&&CrystalItem.aspect(s)==aspect;}});}
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,x+y*9+9,16+x*18,151+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,16+x*18,209));
        addDataSlots(data);
    }
    private record Order(Recipe<CraftingInput> recipe,String research,int vis,Map<String,Integer> crystals,ItemStack result){}
    private CraftingInput.Positioned positioned(){return CraftingInput.ofPositioned(3,3,java.util.stream.IntStream.range(0,9).mapToObj(matrix::getItem).toList());}
    private CraftingInput input(){return positioned().input();}
    private Order recipe(){
        var input=input();var manager=player.level().getRecipeManager();
        var arcane=manager.getAllRecipesFor(MachineContent.ARCANE_RECIPE.get()).stream().filter(h->h.value().matches(input,player.level()))
            .sorted(java.util.Comparator.<RecipeHolder<ArcaneRecipe>>comparingInt(h->hasCrystals(h.value().crystals())?0:1).thenComparing(h->h.id().toString())).findFirst();
        if(arcane.isPresent()){var r=arcane.get().value();return new Order(r,r.research(),r.vis(),r.crystals(),r.assemble(input,player.level().registryAccess()));}
        return manager.getRecipeFor(RecipeType.CRAFTING,input,player.level()).map(h->{var r=h.value();return new Order(r,"",0,Map.of(),r.assemble(input,player.level().registryAccess()));}).orElse(null);
    }
    private boolean hasCrystals(Map<String,Integer> costs){
        for(int i=0;i<6;i++){int n=costs.getOrDefault(PRIMALS.get(i).id(),0);var stack=matrix.getItem(9+i);if(n>0&&(!(stack.getItem() instanceof CrystalItem)||CrystalItem.aspect(stack)!=PRIMALS.get(i)||stack.getCount()<n))return false;}return true;
    }
    public int visCost(){return data.get(0);}
    public int availableVis(){return data.get(1);}
    public int crystalCost(int index){return data.get(2+index);}
    /** Client-only presentation: never added to any inventory or result slot. */
    public ItemStack unavailablePreview(){
        if(visCost()<=0||availableVis()>=visCost())return ItemStack.EMPTY;
        var order=recipe();return order!=null&&order.vis()==visCost()?order.result().copy():ItemStack.EMPTY;
    }
    private boolean canCraft(Order recipe){
        if(!(player instanceof ServerPlayer sp)||recipe==null||recipe.result().isEmpty()||!stillValid(player))return false;
        var state=ResearchStorage.get(sp.getServer()).player(sp.getUUID());if(!state.knows(recipe.research()))return false;
        if(!hasCrystals(recipe.crystals()))return false;
        return recipe.vis()==0||AuraSystem.drain(sp.serverLevel(),pos,recipe.vis(),false,true)>=recipe.vis();
    }
    @Override public void broadcastChanges(){
        if(player instanceof ServerPlayer sp){var recipe=recipe();result.setItem(0,canCraft(recipe)?recipe.result().copy():ItemStack.EMPTY);
            data.set(0,recipe==null?0:recipe.vis());var aura=AuraSystem.get(sp.serverLevel(),pos);data.set(1,aura==null?0:(int)aura.vis());
            for(int i=0;i<6;i++)data.set(2+i,recipe==null?0:recipe.crystals().getOrDefault(PRIMALS.get(i).id(),0));}
        super.broadcastChanges();
    }
    private boolean craft(boolean intoInventory){
        var recipe=recipe();if(!canCraft(recipe))return false;var output=recipe.result().copy();
        if(!intoInventory&&!getCarried().isEmpty()&&(!ItemStack.isSameItemSameComponents(output,getCarried())||getCarried().getCount()+output.getCount()>output.getMaxStackSize()))return false;
        var sp=(ServerPlayer)player;
        // Shift-click must fit the whole result, otherwise no resources are consumed.
        if(intoInventory){int room=0;for(var stack:sp.getInventory().items){if(stack.isEmpty())room+=output.getMaxStackSize();else if(ItemStack.isSameItemSameComponents(stack,output))room+=Math.max(0,stack.getMaxStackSize()-stack.getCount());}if(room<output.getCount())return false;}
        var positioned=positioned();var input=positioned.input();var remaining=recipe.recipe().getRemainingItems(input);
        AuraSystem.drain(sp.serverLevel(),pos,recipe.vis(),false,false);
        for(int i=0;i<6;i++)matrix.removeItem(9+i,recipe.crystals().getOrDefault(PRIMALS.get(i).id(),0));
        for(int y=0;y<input.height();y++)for(int x=0;x<input.width();x++){
            int slot=(x+positioned.left())+(y+positioned.top())*3;var stack=matrix.getItem(slot);if(!stack.isEmpty())matrix.removeItem(slot,1);
            var rest=remaining.get(x+y*input.width()).copy();if(rest.isEmpty())continue;
            var existing=matrix.getItem(slot);
            if(existing.isEmpty())matrix.setItem(slot,rest);
            else if(ItemStack.isSameItemSameComponents(existing,rest)&&existing.getCount()+rest.getCount()<=existing.getMaxStackSize())existing.grow(rest.getCount());
            else if(!sp.getInventory().add(rest))sp.drop(rest,false);
        }
        ResearchProgress.craft(sp,output);output.onCraftedBy(sp.level(),sp,output.getCount());
        if(intoInventory)sp.getInventory().add(output);else if(getCarried().isEmpty())setCarried(output);else getCarried().grow(output.getCount());
        matrix.setChanged();sp.level().playSound(null,pos,PortSounds.get("wand"),net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);broadcastChanges();return true;
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(slot==0){if(!player.level().isClientSide&&(type==ClickType.PICKUP||type==ClickType.QUICK_MOVE))craft(type==ClickType.QUICK_MOVE);return;}
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<=0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index<16){if(!moveItemStackTo(stack,16,52,true))return ItemStack.EMPTY;}
        else if(stack.getItem() instanceof CrystalItem&&PRIMALS.contains(CrystalItem.aspect(stack))){int target=10+PRIMALS.indexOf(CrystalItem.aspect(stack));if(!moveItemStackTo(stack,target,target+1,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,1,10,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
    @Override public boolean stillValid(Player player){return matrix.stillValid(player)&&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.container!=result&&super.canTakeItemForPickAll(stack,slot);}
}
